package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.Roulette;
import com.cassino.mod.machine.SlotMachine;
import java.util.Arrays;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * A roleta, do lado do servidor (é ele quem gira). As regras estão em {@link Roulette}; os créditos, o espaço das
 * fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O jogador escolhe a cor clicando numa das caixas da mesa (0, VERMELHO ou PRETO) e aperta GIRAR!. A aposta e o
 * prêmio entram no saldo da casa, o mesmo do caça-níquel e do blackjack; uma girada só é aceita se o pior caso (a cor
 * escolhida sair) couber no limite de -5000.
 */
public class RouletteScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_SPIN = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;
    /** Escolher a cor: {@code BUTTON_CHOOSE + Roulette.GREEN/RED/BLACK}. */
    public static final int BUTTON_CHOOSE = 5;

    /** Quantas cores saídas a faixa do lado da roda mostra. */
    public static final int HISTORY = 11;

    // ---------------------------------------------------------------- desenho (em pixels, a partir do canto da tela)

    public static final int WIDTH = 316;
    public static final int HEIGHT = 230;
    public static final int COIN_SLOT_X = 214;
    public static final int COIN_SLOT_Y = 96;
    public static final int INVENTORY_X = 77;
    public static final int INVENTORY_Y = 148;
    public static final int HOTBAR_Y = 206;

    // ---------------------------------------------------------------- propriedades enviadas para a tela

    private static final int P_BET = 0;
    private static final int P_CHOICE = 1;
    private static final int P_POCKET = 2;
    private static final int P_WIN_LOW = 3;
    private static final int P_WIN_HIGH = 4;
    private static final int P_MAX_BET = 5;
    private static final int P_HISTORY = 6;
    /** O contador de giradas vai por último: quando a tela o vê mudar, o resultado novo já chegou. */
    private static final int P_SPINS = P_HISTORY + HISTORY;
    private static final int P_COUNT = P_SPINS + 1;

    private int betIndex = 3;
    private int choice = Roulette.RED;
    private int pocket;
    private int lastWin;
    private int maxBet;
    /** As últimas cores que saíram, a mais nova primeiro ({@code -1} = nenhuma ainda). */
    private final int[] history = new int[HISTORY];
    private int spins;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public RouletteScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public RouletteScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.ROULETTE_HANDLER, syncId, playerInventory, context, Cassino.ROULETTE, COIN_SLOT_X, COIN_SLOT_Y,
                INVENTORY_X, INVENTORY_Y, HOTBAR_Y);
        Arrays.fill(history, -1);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_CHOICE -> choice;
                    case P_POCKET -> pocket;
                    case P_WIN_LOW -> low(lastWin);
                    case P_WIN_HIGH -> high(lastWin);
                    case P_MAX_BET -> state == null ? maxBet : tableMaxBet();
                    case P_SPINS -> spins & 0x7FFF;
                    default -> history[index - P_HISTORY];
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_CHOICE -> choice = value;
                    case P_POCKET -> pocket = value;
                    case P_WIN_LOW -> lastWin = withLow(lastWin, value);
                    case P_WIN_HIGH -> lastWin = withHigh(lastWin, value);
                    case P_MAX_BET -> maxBet = value;
                    case P_SPINS -> spins = value;
                    default -> history[index - P_HISTORY] = (short) value; // -1 chega como 0xFFFF
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

    /** A cor escolhida ({@link Roulette#GREEN}, {@link Roulette#RED} ou {@link Roulette#BLACK}). */
    public int choice() {
        return choice;
    }

    /** A casa onde a bolinha parou na última girada (0 a 36). */
    public int pocket() {
        return pocket;
    }

    /** Quanto a última girada devolveu (0 se a cor não saiu). */
    public int lastWin() {
        return lastWin;
    }

    /** A maior aposta que a roleta aceita agora na cor escolhida (0 = nenhuma), pelo limite da casa. */
    public int maxBet() {
        return maxBet;
    }

    public int[] history() {
        return history;
    }

    public int spins() {
        return spins;
    }

    // ---------------------------------------------------------------- limite da casa (só no servidor)

    private boolean fits(int bet) {
        return state.available() - Roulette.worstLoss(choice, bet) >= -CassinoState.MAX_HOUSE_DEBT;
    }

    private int tableMaxBet() {
        int best = 0;
        for (int bet : SlotMachine.BETS) {
            if (fits(bet)) {
                best = bet;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= BUTTON_CHOOSE && id < BUTTON_CHOOSE + Roulette.COLORS) {
            choice = id - BUTTON_CHOOSE;
            return true;
        }
        switch (id) {
            case BUTTON_SPIN -> spin();
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

    private void spin() {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou girar: deposita antes
        }
        if (credits < bet || !fits(bet)) {
            return;
        }
        pocket = Roulette.spin(player.getRandom(), choice, state.house());
        int color = Roulette.color(pocket);
        credits -= bet;
        lastWin = color == choice ? Math.min(Roulette.multiplier(choice) * bet, CassinoState.MAX_BALANCE - credits) : 0;
        credits += lastWin;
        state.settle(bet, lastWin);
        saveCredits();
        System.arraycopy(history, 0, history, 1, HISTORY - 1);
        history[0] = color;
        spins++;
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS,
                0.8F, 0.8F));
    }
}
