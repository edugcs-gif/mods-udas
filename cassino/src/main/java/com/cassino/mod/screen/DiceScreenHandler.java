package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.Dice;
import com.cassino.mod.machine.SlotMachine;
import java.util.Arrays;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * O jogo de dados, do lado do servidor (é ele quem rola). As regras estão em {@link Dice}; os créditos, o espaço das
 * fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O jogador escolhe uma aposta clicando numa fileira da mesa, escolhe o número do NÚMERO e aperta ROLAR!. A aposta e
 * o prêmio entram no saldo da casa, o mesmo das outras máquinas; a jogada só é aceita se o pior caso (a aposta
 * acertar) couber no limite de -640.
 */
public class DiceScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_ROLL = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;
    public static final int BUTTON_NUMBER_UP = 5;
    public static final int BUTTON_NUMBER_DOWN = 6;
    /** Escolher a aposta: {@code BUTTON_CHOOSE + Dice.EVEN/ODD/NUMBER/LOW/HIGH}. */
    public static final int BUTTON_CHOOSE = 7;

    /** Quantas somas o quadro dos últimos resultados mostra. */
    public static final int HISTORY = 5;

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
    private static final int P_NUMBER = 2;
    private static final int P_DIE_1 = 3;
    private static final int P_DIE_2 = 4;
    private static final int P_WIN_LOW = 5;
    private static final int P_WIN_HIGH = 6;
    private static final int P_MAX_BET = 7;
    private static final int P_HISTORY = 8;
    /** O contador de jogadas vai por último: quando a tela o vê mudar, o resultado novo já chegou. */
    private static final int P_ROLLS = P_HISTORY + HISTORY;
    private static final int P_COUNT = P_ROLLS + 1;

    private int betIndex = 2;
    /** A aposta escolhida ({@link Dice#EVEN} etc.). Começa com PARES. */
    private int choice = Dice.EVEN;
    private int number = 7;
    private int die1 = 3;
    private int die2 = 4;
    private int lastWin;
    private int maxBet;
    /** As últimas somas, a mais nova primeiro ({@code -1} = nenhuma ainda). */
    private final int[] history = new int[HISTORY];
    private int rolls;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public DiceScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public DiceScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.DICE_HANDLER, syncId, playerInventory, context, Cassino.DICE_TABLE, COIN_SLOT_X, COIN_SLOT_Y,
                INVENTORY_X, INVENTORY_Y, HOTBAR_Y);
        Arrays.fill(history, -1);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_CHOICE -> choice;
                    case P_NUMBER -> number;
                    case P_DIE_1 -> die1;
                    case P_DIE_2 -> die2;
                    case P_WIN_LOW -> low(lastWin);
                    case P_WIN_HIGH -> high(lastWin);
                    case P_MAX_BET -> state == null ? maxBet : tableMaxBet();
                    case P_ROLLS -> rolls & 0x7FFF;
                    default -> history[index - P_HISTORY];
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_CHOICE -> choice = value;
                    case P_NUMBER -> number = value;
                    case P_DIE_1 -> die1 = value;
                    case P_DIE_2 -> die2 = value;
                    case P_WIN_LOW -> lastWin = withLow(lastWin, value);
                    case P_WIN_HIGH -> lastWin = withHigh(lastWin, value);
                    case P_MAX_BET -> maxBet = value;
                    case P_ROLLS -> rolls = value;
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

    /** A aposta escolhida ({@link Dice#EVEN}, {@link Dice#ODD}, {@link Dice#NUMBER}, LOW ou HIGH). */
    public int choice() {
        return Math.floorMod(choice, Dice.TYPES);
    }

    /** O número escolhido para a aposta NÚMERO (2 a 12). */
    public int number() {
        return Math.max(Dice.MIN_TOTAL, Math.min(Dice.MAX_TOTAL, number));
    }

    public int die1() {
        return die1;
    }

    public int die2() {
        return die2;
    }

    /** Quanto a última jogada devolveu (0 se a aposta errou). */
    public int lastWin() {
        return lastWin;
    }

    /** A maior aposta que a mesa aceita agora na aposta escolhida (0 = nenhuma), pelo limite da casa. */
    public int maxBet() {
        return maxBet;
    }

    public int[] history() {
        return history;
    }

    public int rolls() {
        return rolls;
    }

    // ---------------------------------------------------------------- limite da casa (só no servidor)

    private boolean fits(int stake) {
        return state.available() - Dice.worstLoss(choice(), number(), stake) >= -CassinoState.MAX_HOUSE_DEBT;
    }

    private int tableMaxBet() {
        int best = 0;
        for (int stake : SlotMachine.BETS) {
            if (fits(stake)) {
                best = stake;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= BUTTON_CHOOSE && id < BUTTON_CHOOSE + Dice.TYPES) {
            choice = id - BUTTON_CHOOSE;
            return true;
        }
        switch (id) {
            case BUTTON_ROLL -> roll();
            case BUTTON_BET_UP -> betIndex = Math.min(betIndex + 1, SlotMachine.BETS.length - 1);
            case BUTTON_BET_DOWN -> betIndex = Math.max(betIndex - 1, 0);
            case BUTTON_DEPOSIT -> deposit();
            case BUTTON_WITHDRAW -> withdraw(player);
            case BUTTON_NUMBER_UP -> number = number() == Dice.MAX_TOTAL ? Dice.MIN_TOTAL : number() + 1;
            case BUTTON_NUMBER_DOWN -> number = number() == Dice.MIN_TOTAL ? Dice.MAX_TOTAL : number() - 1;
            default -> {
                return false;
            }
        }
        return true;
    }

    private void roll() {
        int stake = bet();
        if (credits < stake) {
            deposit(); // colocou fichas e já mandou rolar: deposita antes
        }
        if (credits < stake || !fits(stake)) {
            return;
        }
        int[] dice = Dice.roll(player.getRandom(), choice(), number(), state.house());
        die1 = dice[0];
        die2 = dice[1];
        int total = die1 + die2;
        credits -= stake;
        lastWin = Math.min(Dice.payout(choice(), number(), stake, total), CassinoState.MAX_BALANCE - credits);
        credits += lastWin;
        state.settle(stake, lastWin);
        saveCredits();
        System.arraycopy(history, 0, history, 1, HISTORY - 1);
        history[0] = total;
        rolls++;
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS,
                0.8F, 1.2F));
    }
}
