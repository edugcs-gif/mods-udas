package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.CoinFlip;
import com.cassino.mod.machine.SlotMachine;
import java.util.Arrays;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * O cara ou coroa, do lado do servidor (é ele quem lança). As regras estão em {@link CoinFlip}; os créditos, o espaço
 * das fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O jogador escolhe o lado clicando em CARA ou COROA e aperta LANÇAR!. A aposta e o prêmio entram no saldo da casa,
 * o mesmo das outras máquinas; um lançamento só é aceito se o pior caso (o lado escolhido sair) couber no limite de
 * -5000.
 */
public class CoinFlipScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_FLIP = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;
    /** Escolher o lado: {@code BUTTON_CHOOSE + CoinFlip.HEADS/TAILS}. */
    public static final int BUTTON_CHOOSE = 5;

    /** Quantos resultados o quadro "ÚLTIMOS" mostra. */
    public static final int HISTORY = 4;

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
    private static final int P_RESULT = 2;
    private static final int P_WIN_LOW = 3;
    private static final int P_WIN_HIGH = 4;
    private static final int P_MAX_BET = 5;
    private static final int P_HISTORY = 6;
    /** O contador de lançamentos vai por último: quando a tela o vê mudar, o resultado novo já chegou. */
    private static final int P_FLIPS = P_HISTORY + HISTORY;
    private static final int P_COUNT = P_FLIPS + 1;

    private int betIndex = 3;
    private int choice = CoinFlip.HEADS;
    private int result = CoinFlip.HEADS;
    private int lastWin;
    private int maxBet;
    /** Os últimos resultados, o mais novo primeiro ({@code -1} = nenhum ainda). */
    private final int[] history = new int[HISTORY];
    private int flips;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public CoinFlipScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public CoinFlipScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.COIN_FLIP_HANDLER, syncId, playerInventory, context, Cassino.COIN_FLIP, COIN_SLOT_X, COIN_SLOT_Y,
                INVENTORY_X, INVENTORY_Y, HOTBAR_Y);
        Arrays.fill(history, -1);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_CHOICE -> choice;
                    case P_RESULT -> result;
                    case P_WIN_LOW -> low(lastWin);
                    case P_WIN_HIGH -> high(lastWin);
                    case P_MAX_BET -> state == null ? maxBet : tableMaxBet();
                    case P_FLIPS -> flips & 0x7FFF;
                    default -> history[index - P_HISTORY];
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_CHOICE -> choice = value;
                    case P_RESULT -> result = value;
                    case P_WIN_LOW -> lastWin = withLow(lastWin, value);
                    case P_WIN_HIGH -> lastWin = withHigh(lastWin, value);
                    case P_MAX_BET -> maxBet = value;
                    case P_FLIPS -> flips = value;
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

    /** O lado escolhido ({@link CoinFlip#HEADS} ou {@link CoinFlip#TAILS}). */
    public int choice() {
        return choice;
    }

    /**
     * Como a moeda caiu no último lançamento: {@link CoinFlip#HEADS}, {@link CoinFlip#TAILS} ou {@link CoinFlip#EDGE}.
     */
    public int result() {
        return result;
    }

    /** Quanto o último lançamento devolveu (0 se o lado não saiu). */
    public int lastWin() {
        return lastWin;
    }

    /** A maior aposta que a mesa aceita agora (0 = nenhuma), pelo limite da casa. */
    public int maxBet() {
        return maxBet;
    }

    public int[] history() {
        return history;
    }

    public int flips() {
        return flips;
    }

    // ---------------------------------------------------------------- limite da casa (só no servidor)

    private boolean fits(int bet) {
        return state.available() - CoinFlip.worstLoss(bet) >= -CassinoState.MAX_HOUSE_DEBT;
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
        if (id >= BUTTON_CHOOSE && id < BUTTON_CHOOSE + CoinFlip.SIDES) {
            choice = id - BUTTON_CHOOSE;
            return true;
        }
        switch (id) {
            case BUTTON_FLIP -> flip();
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

    private void flip() {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou lançar: deposita antes
        }
        if (credits < bet || !fits(bet)) {
            return;
        }
        result = CoinFlip.flip(player.getRandom(), choice, state.house());
        credits -= bet;
        lastWin = result == choice ? Math.min(CoinFlip.MULTIPLIER * bet, CassinoState.MAX_BALANCE - credits) : 0;
        credits += lastWin;
        state.settle(bet, lastWin);
        saveCredits();
        System.arraycopy(history, 0, history, 1, HISTORY - 1);
        history[0] = result;
        flips++;
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
                SoundCategory.BLOCKS, 0.8F, 1.4F));
    }
}
