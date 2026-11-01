package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.SlotMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A tela do caça-níquel, do lado do servidor (é ele quem sorteia). Os créditos, o espaço das fichas, DEPÓSITO e
 * RETIRADA estão em {@link CassinoScreenHandler}.
 */
public class SlotMachineScreenHandler extends CassinoScreenHandler {
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

    private static final int P_BET = 0;
    private static final int P_WIN_LOW = 1;
    private static final int P_WIN_HIGH = 2;
    private static final int P_POOL_LOW = 3;
    private static final int P_POOL_HIGH = 4;
    private static final int P_CELLS = 5;
    /** O contador de giradas vai por último: quando a tela o vê mudar, os símbolos novos já chegaram. */
    private static final int P_SPINS = P_CELLS + SlotMachine.CELLS;
    private static final int P_COUNT = P_SPINS + 1;

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
        super(Cassino.SLOT_MACHINE_HANDLER, syncId, playerInventory, context, Cassino.SLOT_MACHINE, COIN_SLOT_X,
                COIN_SLOT_Y, INVENTORY_X, INVENTORY_Y, HOTBAR_Y);
        // Já abre com símbolos nos rolos (só enfeite; não paga nada).
        this.cells = SlotMachine.idle(player.getRandom());

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index == P_BET) {
                    return betIndex;
                }
                if (index == P_WIN_LOW) {
                    return low(lastWin);
                }
                if (index == P_WIN_HIGH) {
                    return high(lastWin);
                }
                if (index == P_POOL_LOW || index == P_POOL_HIGH) {
                    int value = state == null ? pool : (int) Math.min(Integer.MAX_VALUE, Math.max(0L, state.house()));
                    return index == P_POOL_LOW ? low(value) : high(value);
                }
                if (index == P_SPINS) {
                    return spins & 0x7FFF;
                }
                return cells[index - P_CELLS];
            }

            @Override
            public void set(int index, int value) {
                if (index == P_BET) {
                    betIndex = value;
                } else if (index == P_WIN_LOW) {
                    lastWin = withLow(lastWin, value);
                } else if (index == P_WIN_HIGH) {
                    lastWin = withHigh(lastWin, value);
                } else if (index == P_POOL_LOW) {
                    pool = withLow(pool, value);
                } else if (index == P_POOL_HIGH) {
                    pool = withHigh(pool, value);
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

    private void spin(PlayerEntity player) {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou girar: deposita antes
        }
        if (credits < bet) {
            return;
        }
        int[] result = SlotMachine.spin(player.getRandom(), bet, state.house(), state.available());
        System.arraycopy(result, 0, cells, 0, cells.length);
        int base = bet * SlotMachine.multiplier(result);
        boolean jackpot = SlotMachine.isJackpot(result);
        long prize = base;
        if (jackpot) {
            prize += SlotMachine.jackpotBonus(bet, state.available() + bet - base);
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
}
