package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import java.util.UUID;
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
 * <p>A tela também mostra o ranking do cassino: o <b>magnata</b> (quem tem mais créditos no servidor, com o nome e
 * quanto tem) e a posição do jogador ({@link #rank}).
 *
 * <p>A tela também mostra o saldo da casa ({@link #house}), o mesmo em todos os jogos. No negativo ela mostra 0: os
 * jogadores não ficam sabendo quando a casa está devendo (e jogando com mais sorte).
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
    /** O saldo da casa, só para a tela mostrar (no servidor ele vem do cofre); nunca menor que 0. */
    private int house;

    /** Quantas propriedades levam o nome do magnata: 2 letras em cada (os nomes do Minecraft têm até 16). */
    private static final int NAME_PROPERTIES = 8;
    private static final int P_RANK = 4;
    private static final int P_RANKED = 5;
    private static final int P_TYCOON_LOW = 6;
    private static final int P_TYCOON_HIGH = 7;
    private static final int P_TYCOON_NAME = 8;
    private static final int BASE_PROPERTIES = P_TYCOON_NAME + NAME_PROPERTIES;

    // O ranking, só para a tela (no servidor ele vem do cofre).
    private int rank;
    private int ranked;
    private int tycoonCredits;
    private final int[] tycoonName = new int[NAME_PROPERTIES];

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
            state.rememberName(player.getUuid(), serverPlayer.getGameProfile().getName());
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

        // 0 e 1: os créditos do jogador; 2 e 3: o saldo da casa (em duas partes, porque passam de 16 bits);
        // 4 a 15: o ranking (a posição do jogador, quantos têm créditos, quanto o magnata tem e o nome dele).
        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index < P_RANK) {
                    int value = index < 2 ? credits : houseValue();
                    return index % 2 == 0 ? low(value) : high(value);
                }
                if (state == null) {
                    return switch (index) {
                        case P_RANK -> rank;
                        case P_RANKED -> ranked;
                        case P_TYCOON_LOW -> low(tycoonCredits);
                        case P_TYCOON_HIGH -> high(tycoonCredits);
                        default -> tycoonName[index - P_TYCOON_NAME];
                    };
                }
                UUID tycoon = state.tycoon();
                return switch (index) {
                    case P_RANK -> Math.min(0x7FFF, state.rank(player.getUuid()));
                    case P_RANKED -> Math.min(0x7FFF, state.rankedPlayers());
                    case P_TYCOON_LOW -> low(tycoon == null ? 0 : state.balance(tycoon));
                    case P_TYCOON_HIGH -> high(tycoon == null ? 0 : state.balance(tycoon));
                    default -> nameChars(tycoon == null ? "" : state.name(tycoon), index - P_TYCOON_NAME);
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> credits = withLow(credits, value);
                    case 1 -> credits = withHigh(credits, value);
                    case 2 -> house = withLow(house, value);
                    case 3 -> house = withHigh(house, value);
                    case P_RANK -> rank = value & 0xFFFF;
                    case P_RANKED -> ranked = value & 0xFFFF;
                    case P_TYCOON_LOW -> tycoonCredits = withLow(tycoonCredits, value);
                    case P_TYCOON_HIGH -> tycoonCredits = withHigh(tycoonCredits, value);
                    default -> tycoonName[index - P_TYCOON_NAME] = value & 0xFFFF;
                }
            }

            @Override
            public int size() {
                return BASE_PROPERTIES;
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

    /** O saldo da casa, somando todos os jogos: o que o cassino já ganhou, ou 0 se ele estiver no negativo. */
    public int house() {
        return house;
    }

    /** Duas letras do nome (a {@code 2k}-ésima e a seguinte) num número de 16 bits; 0 depois do fim. */
    private static int nameChars(String name, int k) {
        int first = 2 * k < name.length() ? name.charAt(2 * k) & 0xFF : 0;
        int second = 2 * k + 1 < name.length() ? name.charAt(2 * k + 1) & 0xFF : 0;
        return first << 8 | second;
    }

    /** A posição do jogador no ranking de créditos do cassino (1 = o magnata), ou 0 se ele não tem créditos. */
    public int rank() {
        return rank;
    }

    /** Quantos jogadores têm créditos no cassino. */
    public int rankedPlayers() {
        return ranked;
    }

    /** Os créditos do magnata. */
    public int tycoonCredits() {
        return tycoonCredits;
    }

    /** O nome do magnata, ou "" se ninguém tem créditos. */
    public String tycoonName() {
        StringBuilder name = new StringBuilder();
        for (int pair : tycoonName) {
            for (int c : new int[] {pair >> 8 & 0xFF, pair & 0xFF}) {
                if (c == 0) {
                    return name.toString();
                }
                name.append((char) c);
            }
        }
        return name.toString();
    }

    private int houseValue() {
        // No negativo a tela mostra 0: o valor de verdade nem sai do servidor.
        return state == null ? house : (int) Math.max(0, Math.min(Integer.MAX_VALUE, state.house()));
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
