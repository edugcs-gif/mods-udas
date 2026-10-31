package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.SlotMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A tela do caça-níquel, do lado do servidor (é ele quem sorteia e guarda os créditos).
 *
 * <p>DEPÓSITO transforma as fichas do espaço ao lado em créditos (1 ficha = 1 crédito); cada girada gasta a aposta
 * em créditos e os prêmios voltam como créditos; RETIRADA saca todos os créditos em fichas. Os créditos são do
 * jogador, guardados no {@link CassinoState}: valem em qualquer máquina, continuam lá depois de fechar a tela, e
 * ninguém mais mexe neles.
 *
 * <p>Os botões chegam pelo {@link #onButtonClick} (o mesmo caminho da mesa de encantamento), e o estado vai para
 * a tela pelas propriedades da tela (números de 16 bits; os créditos vão em duas partes).
 */
public class SlotMachineScreenHandler extends ScreenHandler {
    public static final int BUTTON_SPIN = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;
    /** Quanto tempo a tela leva para parar os rolos (o aviso do jackpot espera isso para não estragar a surpresa). */
    public static final int SPIN_TICKS = 40;

    // ---------------------------------------------------------------- desenho (em pixels, a partir do canto da tela)

    public static final int WIDTH = 276;
    public static final int HEIGHT = 214;
    /** O espaço das fichas, antes do botão DEPÓSITO (o fundo do espaço; o item fica 1 pixel para dentro). */
    public static final int COIN_SLOT_X = 154;
    public static final int COIN_SLOT_Y = 83;
    public static final int INVENTORY_X = 57;
    public static final int INVENTORY_Y = 131;
    public static final int HOTBAR_Y = 189;

    // ---------------------------------------------------------------- propriedades enviadas para a tela

    private static final int P_CREDITS_LOW = 0;
    private static final int P_CREDITS_HIGH = 1;
    private static final int P_BET = 2;
    private static final int P_WIN_LOW = 3;
    private static final int P_WIN_HIGH = 4;
    private static final int P_POOL_LOW = 5;
    private static final int P_POOL_HIGH = 6;
    private static final int P_CELLS = 7;
    /** O contador de giradas vai por último: quando a tela o vê mudar, os símbolos novos já chegaram. */
    private static final int P_SPINS = P_CELLS + SlotMachine.CELLS;
    private static final int P_COUNT = P_SPINS + 1;

    private final ScreenHandlerContext context;
    private final PlayerEntity player;
    /** O cofre do servidor ({@code null} no cliente). */
    private final CassinoState state;
    private final Inventory coins = new SimpleInventory(1);

    private int credits;
    private int betIndex = SlotMachine.DEFAULT_BET;
    private int lastWin;
    /** O acumulado do jackpot (o saldo positivo da casa), só para a tela mostrar. */
    private int pool;
    private final int[] cells;
    private int spins;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.SLOT_MACHINE_HANDLER, syncId);
        this.context = context;
        this.player = playerInventory.player;
        // Já abre com símbolos nos rolos (só enfeite; não paga nada).
        this.cells = SlotMachine.idle(player.getRandom());
        if (player instanceof ServerPlayerEntity serverPlayer) {
            state = CassinoState.get(serverPlayer.getServer());
            credits = state.balance(player.getUuid());
        } else {
            state = null;
        }

        addSlot(new Slot(coins, 0, COIN_SLOT_X + 1, COIN_SLOT_Y + 1) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.isOf(Cassino.CHIP);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, INVENTORY_X + 1 + column * 18,
                        INVENTORY_Y + 1 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, INVENTORY_X + 1 + column * 18, HOTBAR_Y + 1));
        }

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index == P_CREDITS_LOW) {
                    return credits & 0xFFFF;
                }
                if (index == P_CREDITS_HIGH) {
                    return (credits >>> 16) & 0xFFFF;
                }
                if (index == P_BET) {
                    return betIndex;
                }
                if (index == P_WIN_LOW) {
                    return lastWin & 0xFFFF;
                }
                if (index == P_WIN_HIGH) {
                    return (lastWin >>> 16) & 0xFFFF;
                }
                if (index == P_POOL_LOW || index == P_POOL_HIGH) {
                    int value = state == null ? pool : (int) Math.min(Integer.MAX_VALUE, Math.max(0L, state.house()));
                    return index == P_POOL_LOW ? value & 0xFFFF : (value >>> 16) & 0xFFFF;
                }
                if (index == P_SPINS) {
                    return spins & 0x7FFF;
                }
                return cells[index - P_CELLS];
            }

            @Override
            public void set(int index, int value) {
                // Os números chegam com 16 bits (e sinal): o & 0xFFFF desfaz o sinal.
                if (index == P_CREDITS_LOW) {
                    credits = (credits & 0xFFFF0000) | (value & 0xFFFF);
                } else if (index == P_CREDITS_HIGH) {
                    credits = (credits & 0xFFFF) | ((value & 0xFFFF) << 16);
                } else if (index == P_BET) {
                    betIndex = value;
                } else if (index == P_WIN_LOW) {
                    lastWin = (lastWin & 0xFFFF0000) | (value & 0xFFFF);
                } else if (index == P_WIN_HIGH) {
                    lastWin = (lastWin & 0xFFFF) | ((value & 0xFFFF) << 16);
                } else if (index == P_POOL_LOW) {
                    pool = (pool & 0xFFFF0000) | (value & 0xFFFF);
                } else if (index == P_POOL_HIGH) {
                    pool = (pool & 0xFFFF) | ((value & 0xFFFF) << 16);
                } else if (index == P_SPINS) {
                    spins = value;
                } else {
                    cells[index - P_CELLS] = value;
                }
            }

            @Override
            public int size() {
                return P_COUNT;
            }
        });
    }

    // ---------------------------------------------------------------- o que a tela mostra

    public int credits() {
        return credits;
    }

    public int bet() {
        return SlotMachine.BETS[Math.floorMod(betIndex, SlotMachine.BETS.length)];
    }

    /** Quanto a última girada pagou (em créditos). */
    public int lastWin() {
        return lastWin;
    }

    /** Os 15 símbolos à mostra (veja {@link SlotMachine#cell}). */
    public int[] cells() {
        return cells;
    }

    /** O acumulado do jackpot: o saldo positivo da casa. */
    public int pool() {
        return pool;
    }

    /** Muda a cada girada (a tela começa a animação quando vê este número mudar). */
    public int spins() {
        return spins;
    }

    /** Quantas fichas estão no espaço ao lado de DEPÓSITO. */
    public int coinsWaiting() {
        return coins.getStack(0).getCount();
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        switch (id) {
            case BUTTON_SPIN -> spin(player);
            case BUTTON_BET_UP -> betIndex = Math.min(betIndex + 1, SlotMachine.BETS.length - 1);
            case BUTTON_BET_DOWN -> betIndex = Math.max(betIndex - 1, 0);
            case BUTTON_DEPOSIT -> deposit();
            case BUTTON_WITHDRAW -> withdraw(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Guarda o saldo deste jogador no cofre (é ele que vale; o campo aqui é só o que a tela mostra). */
    private void saveCredits() {
        state.setBalance(player.getUuid(), credits);
    }

    /** As fichas do espaço viram créditos. */
    private void deposit() {
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

    private void spin(PlayerEntity player) {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou girar: deposita antes
        }
        if (credits < bet) {
            return;
        }
        int[] result = SlotMachine.spin(player.getRandom(), bet, state.house());
        System.arraycopy(result, 0, cells, 0, cells.length);
        int base = bet * SlotMachine.multiplier(result);
        boolean jackpot = SlotMachine.isJackpot(result);
        long prize = base;
        if (jackpot) {
            prize += SlotMachine.jackpotBonus(bet, state.house() + bet - base);
        }
        credits -= bet;
        lastWin = (int) Math.min(prize, CassinoState.MAX_BALANCE - credits);
        credits += lastWin;
        state.settle(bet, lastWin);
        saveCredits();
        spins++;

        int paid = lastWin;
        context.run((world, pos) -> {
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.8F, 1.0F);
            if (jackpot && world instanceof ServerWorld serverWorld) {
                // Avisa quem está perto só quando os rolos já pararam na tela de quem jogou.
                Cassino.later(serverWorld.getServer(), SPIN_TICKS, () -> Cassino.broadcastNearby(world, player,
                        Text.translatable("message.cassino.slot.jackpot_broadcast", player.getDisplayName(), paid)
                                .formatted(Formatting.GOLD), 32.0));
            }
        });
    }

    /** Todos os créditos voltam em fichas. */
    private void withdraw(PlayerEntity player) {
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
        return canUse(context, player, Cassino.SLOT_MACHINE);
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
