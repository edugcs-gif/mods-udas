package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * O que as telas do cassino (caça-níquel e mesa de blackjack) têm em comum: os créditos do jogador, o espaço das
 * fichas com DEPÓSITO e RETIRADA, e o inventário embaixo.
 *
 * <p>A tela também mostra o saldo da casa ({@link #house}), o mesmo em todos os jogos.
 *
 * <p>Os créditos são do jogador, guardados no {@link CassinoState}: valem em qualquer máquina, continuam lá depois
 * de fechar a tela, e ninguém mais mexe neles. DEPÓSITO transforma as fichas do espaço em créditos (1 ficha = 1
 * crédito); RETIRADA saca todos em fichas.
 *
 * <p>Os botões chegam pelo {@link #onButtonClick} (o mesmo caminho da mesa de encantamento), e o estado vai para a
 * tela pelas propriedades da tela, que levam números de 16 bits: os maiores vão em duas partes ({@link #low} e
 * {@link #high}).
 */
public abstract class CassinoScreenHandler extends ScreenHandler {
    protected final ScreenHandlerContext context;
    protected final PlayerEntity player;
    /** O cofre do servidor ({@code null} no cliente). */
    protected final CassinoState state;
    private final Inventory coins = new SimpleInventory(1);
    private final Block block;

    protected int credits;
    /** O saldo da casa, só para a tela mostrar (no servidor ele vem do cofre). */
    private int house;

    /**
     * @param coinX, coinY      o fundo do espaço das fichas (o item fica 1 pixel para dentro)
     * @param inventoryX, inventoryY, hotbarY o canto do inventário e da barra de baixo
     */
    protected CassinoScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory,
                                   ScreenHandlerContext context, Block block, int coinX, int coinY, int inventoryX,
                                   int inventoryY, int hotbarY) {
        super(type, syncId);
        this.context = context;
        this.player = playerInventory.player;
        this.block = block;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            state = CassinoState.get(serverPlayer.getServer());
            credits = state.balance(player.getUuid());
        } else {
            state = null;
        }

        addSlot(new Slot(coins, 0, coinX + 1, coinY + 1) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Cassino.CHIP);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, inventoryX + 1 + column * 18,
                        inventoryY + 1 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, inventoryX + 1 + column * 18, hotbarY + 1));
        }

        // 0 e 1: os créditos do jogador; 2 e 3: o saldo da casa (pode ser negativo: vai inteiro, em duas partes).
        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                int value = index < 2 ? credits : houseValue();
                return index % 2 == 0 ? low(value) : high(value);
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    credits = withLow(credits, value);
                } else if (index == 1) {
                    credits = withHigh(credits, value);
                } else if (index == 2) {
                    house = withLow(house, value);
                } else {
                    house = withHigh(house, value);
                }
            }

            @Override
            public int size() {
                return 4;
            }
        });
    }

    // ---------------------------------------------------------------- números grandes nas propriedades

    protected static int low(int value) {
        return value & 0xFFFF;
    }

    protected static int high(int value) {
        return (value >>> 16) & 0xFFFF;
    }

    /** Os números chegam com 16 bits (e sinal): o & 0xFFFF desfaz o sinal. */
    protected static int withLow(int old, int value) {
        return (old & 0xFFFF0000) | (value & 0xFFFF);
    }

    protected static int withHigh(int old, int value) {
        return (old & 0xFFFF) | ((value & 0xFFFF) << 16);
    }

    // ---------------------------------------------------------------- créditos

    public int credits() {
        return credits;
    }

    /** O saldo da casa: o que o cassino já ganhou (positivo) ou pagou a mais (negativo), somando todos os jogos. */
    public int house() {
        return house;
    }

    private int houseValue() {
        return state == null ? house : (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, state.house()));
    }

    /** Quantas fichas estão no espaço ao lado de DEPÓSITO. */
    public int coinsWaiting() {
        return coins.getStack(0).getCount();
    }

    /** Guarda o saldo deste jogador no cofre (é ele que vale; o campo aqui é só o que a tela mostra). */
    protected void saveCredits() {
        state.setBalance(player.getUuid(), credits);
    }

    /** As fichas do espaço viram créditos. */
    protected void deposit() {
        ItemStack stack = coins.getStack(0);
        if (stack.isEmpty() || !stack.isOf(Cassino.CHIP)) {
            return;
        }
        int accepted = Math.min(stack.getCount(), CassinoState.MAX_BALANCE - credits);
        credits += accepted;
        stack.decrement(accepted);
        coins.markDirty();
        saveCredits();
    }

    /** Todos os créditos voltam em fichas. */
    protected void withdraw(PlayerEntity player) {
        if (credits > 0) {
            int amount = credits;
            credits = 0;
            saveCredits();
            giveChips(player, amount);
        }
    }

    /** Entrega fichas; se o jogador saiu do jogo ou morreu, elas caem no chão (como os itens de uma mesa de trabalho). */
    private void giveChips(PlayerEntity player, int amount) {
        SimpleInventory chips = new SimpleInventory((amount + 63) / 64);
        for (int left = amount, slot = 0; left > 0; slot++) {
            int count = Math.min(left, Cassino.CHIP.getMaxCount());
            chips.setStack(slot, new ItemStack(Cassino.CHIP, count));
            left -= count;
        }
        dropInventory(player, chips);
    }

    // ---------------------------------------------------------------- tela

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player.getWorld().isClient) {
            return;
        }
        // As fichas que ficaram no espaço (sem depositar) voltam; os créditos continuam guardados no cofre.
        dropInventory(player, coins);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, block);
    }

    /** Shift + clique: fichas do inventário vão para o espaço de DEPÓSITO, e as do espaço voltam para o inventário. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!insertItem(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!stack.isOf(Cassino.CHIP) || !insertItem(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }
}
