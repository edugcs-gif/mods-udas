package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.Plinko;
import com.cassino.mod.machine.SlotMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * O Plinko de Slime, do lado do servidor (é ele quem sorteia o caminho da bola). As regras estão em {@link Plinko}; os
 * créditos, o espaço das fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O jogador escolhe a aposta e aperta LANÇAR!. O caminho da bola vai para a tela, que mostra ela quicando nos pinos
 * até o buraco. A aposta e o prêmio entram no saldo da casa, o mesmo das outras máquinas; uma jogada só é aceita se o
 * pior caso (a bola cair numa ponta, 10x) couber no limite de -2000.
 */
public class PlinkoScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_DROP = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;

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
    private static final int P_PATH = 1;
    private static final int P_WIN_LOW = 2;
    private static final int P_WIN_HIGH = 3;
    private static final int P_MAX_BET = 4;
    /** O contador de jogadas vai por último: quando a tela o vê mudar, o resultado novo já chegou. */
    private static final int P_DROPS = 5;
    private static final int P_COUNT = 6;

    private int betIndex = 2;
    /** O caminho da última bola ({@link Plinko#ROWS} bits, 1 = direita); começa no meio. */
    private int path = 0b000111;
    private int lastWin;
    private int maxBet;
    private int drops;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public PlinkoScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public PlinkoScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.PLINKO_HANDLER, syncId, playerInventory, context, Cassino.PLINKO, COIN_SLOT_X, COIN_SLOT_Y,
                INVENTORY_X, INVENTORY_Y, HOTBAR_Y);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_PATH -> path;
                    case P_WIN_LOW -> low(lastWin);
                    case P_WIN_HIGH -> high(lastWin);
                    case P_MAX_BET -> state == null ? maxBet : tableMaxBet();
                    default -> drops & 0x7FFF;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_PATH -> path = value;
                    case P_WIN_LOW -> lastWin = withLow(lastWin, value);
                    case P_WIN_HIGH -> lastWin = withHigh(lastWin, value);
                    case P_MAX_BET -> maxBet = value;
                    default -> drops = value;
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

    /** O caminho da última bola ({@link Plinko#ROWS} bits, 1 = direita). */
    public int path() {
        return path;
    }

    /** Quanto a última bola devolveu. */
    public int lastWin() {
        return lastWin;
    }

    /** A maior aposta que o Plinko aceita agora (0 = nenhuma), pelo limite da casa. */
    public int maxBet() {
        return maxBet;
    }

    public int drops() {
        return drops;
    }

    // ---------------------------------------------------------------- limite da casa (só no servidor)

    private boolean fits(int bet) {
        return state.available() - Plinko.worstLoss(bet) >= -CassinoState.MAX_HOUSE_DEBT;
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
        switch (id) {
            case BUTTON_DROP -> drop();
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

    private void drop() {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou lançar: deposita antes
        }
        if (credits < bet || !fits(bet)) {
            return;
        }
        path = Plinko.drop(player.getRandom(), state.house());
        credits -= bet;
        lastWin = Math.min(Plinko.prize(player.getRandom(), bet, Plinko.bucket(path)),
                CassinoState.MAX_BALANCE - credits);
        credits += lastWin;
        state.settle(bet, lastWin);
        saveCredits();
        drops++;
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ENTITY_SLIME_SQUISH_SMALL,
                SoundCategory.BLOCKS, 0.8F, 1.0F));
    }
}
