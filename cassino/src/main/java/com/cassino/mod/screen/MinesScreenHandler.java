package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.Mines;
import com.cassino.mod.machine.SlotMachine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * O Campo Minado, do lado do servidor (é ele quem decide o que tem embaixo de cada bloco). As regras estão em
 * {@link Mines}; os créditos, o espaço das fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>JOGAR! tira a aposta dos créditos e cobre a grade; cada clique num bloco cava ele; SACAR! paga o prêmio do
 * momento. Enquanto a rodada está aberta, a casa reserva o que pagaria se o jogador sacasse (como numa mão de
 * blackjack), e só deixa cavar se o prêmio depois da próxima pedra ainda couber no limite de -640: perto do limite, a
 * tela avisa que é hora de sacar. Fechar a tela no meio da rodada saca sozinho.
 */
public class MinesScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_PLAY = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;
    public static final int BUTTON_MINES_UP = 5;
    public static final int BUTTON_MINES_DOWN = 6;
    /** Cavar um bloco: {@code BUTTON_DIG + o número do bloco (0 a 24, linha por linha)}. */
    public static final int BUTTON_DIG = 7;

    /** A rodada: esperando o JOGAR!, cavando, explodiu ou sacou. */
    public static final int WAITING = 0;
    public static final int PLAYING = 1;
    public static final int BUSTED = 2;
    public static final int CASHED = 3;

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
    private static final int P_MINES = 1;
    private static final int P_PHASE = 2;
    private static final int P_FOUND = 3;
    private static final int P_STAKE = 4;
    private static final int P_WIN_LOW = 5;
    private static final int P_WIN_HIGH = 6;
    private static final int P_CAN_DIG = 7;
    private static final int P_TILES = 8;
    /** O contador de jogadas vai por último: quando a tela o vê mudar, o resultado novo já chegou. */
    private static final int P_EVENTS = P_TILES + Mines.CELLS;
    private static final int P_COUNT = P_EVENTS + 1;

    private int betIndex = 2;
    private int mines = Mines.DEFAULT_MINES;
    private int phase = WAITING;
    private int found;
    /** A aposta da rodada aberta (ou da última). */
    private int stake;
    private int lastWin;
    private int canDig;
    private final int[] tiles = new int[Mines.CELLS];
    private int events;
    /** O que a casa reservou para esta rodada (só no servidor). */
    private long reserved;
    /** Se a sorte do jogador ainda pode salvar ele de uma mina nesta rodada (só no servidor). */
    private boolean rescue;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public MinesScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public MinesScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.MINES_HANDLER, syncId, playerInventory, context, Cassino.MINES, COIN_SLOT_X, COIN_SLOT_Y,
                INVENTORY_X, INVENTORY_Y, HOTBAR_Y);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_MINES -> mines;
                    case P_PHASE -> phase;
                    case P_FOUND -> found;
                    case P_STAKE -> stake;
                    case P_WIN_LOW -> low(lastWin);
                    case P_WIN_HIGH -> high(lastWin);
                    case P_CAN_DIG -> state == null ? canDig : (fitsNextDig() ? 1 : 0);
                    case P_EVENTS -> events & 0x7FFF;
                    default -> tiles[index - P_TILES];
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_MINES -> mines = value;
                    case P_PHASE -> phase = value;
                    case P_FOUND -> found = value;
                    case P_STAKE -> stake = value;
                    case P_WIN_LOW -> lastWin = withLow(lastWin, value);
                    case P_WIN_HIGH -> lastWin = withHigh(lastWin, value);
                    case P_CAN_DIG -> canDig = value;
                    case P_EVENTS -> events = value;
                    default -> tiles[index - P_TILES] = value;
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

    public int mines() {
        return Math.max(Mines.MIN_MINES, Math.min(Mines.MAX_MINES, mines));
    }

    public int phase() {
        return phase;
    }

    /** Quantas pedras preciosas o jogador achou nesta rodada. */
    public int found() {
        return found;
    }

    /** A aposta da rodada aberta (ou da última). */
    public int stake() {
        return stake;
    }

    /** Quanto a última rodada pagou. */
    public int lastWin() {
        return lastWin;
    }

    /** Se a casa deixa cavar mais um bloco (o prêmio depois dele cabe no limite). */
    public boolean canDig() {
        return canDig != 0;
    }

    /** O que o bloco mostra (veja {@link Mines#HIDDEN} e companhia). */
    public int tile(int index) {
        return tiles[index];
    }

    public int events() {
        return events;
    }

    // ---------------------------------------------------------------- limite da casa (só no servidor)

    /** O quanto a casa perderia se o jogador sacasse depois de achar {@code gems} pedras. */
    private long houseLoss(int gems) {
        return Math.max(0L, Mines.payout(stake, mines(), gems) - stake);
    }

    private boolean fitsNextDig() {
        if (phase != PLAYING || found >= Mines.CELLS - mines()) {
            return false;
        }
        return state.available() + reserved - houseLoss(found + 1) >= -CassinoState.MAX_HOUSE_DEBT;
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= BUTTON_DIG && id < BUTTON_DIG + Mines.CELLS) {
            dig(id - BUTTON_DIG);
            return true;
        }
        boolean idle = phase != PLAYING;
        switch (id) {
            case BUTTON_PLAY -> {
                if (idle) {
                    start();
                } else {
                    cashOut();
                }
            }
            case BUTTON_BET_UP -> betIndex = idle ? Math.min(betIndex + 1, SlotMachine.BETS.length - 1) : betIndex;
            case BUTTON_BET_DOWN -> betIndex = idle ? Math.max(betIndex - 1, 0) : betIndex;
            case BUTTON_MINES_UP -> mines = idle ? Math.min(mines() + 1, Mines.MAX_MINES) : mines;
            case BUTTON_MINES_DOWN -> mines = idle ? Math.max(mines() - 1, Mines.MIN_MINES) : mines;
            case BUTTON_DEPOSIT -> deposit();
            case BUTTON_WITHDRAW -> withdraw(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** JOGAR!: tira a aposta dos créditos e cobre a grade. */
    private void start() {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou jogar: deposita antes
        }
        if (credits < bet) {
            return;
        }
        credits -= bet;
        saveCredits();
        stake = bet;
        found = 0;
        lastWin = 0;
        rescue = true;
        Arrays.fill(tiles, Mines.HIDDEN);
        phase = PLAYING;
        events++;
    }

    private void dig(int index) {
        if (phase != PLAYING || tiles[index] != Mines.HIDDEN || !fitsNextDig()) {
            return;
        }
        Random random = player.getRandom();
        int hidden = Mines.CELLS - found;
        Mines.Dig dig = Mines.dig(random, hidden, mines(), state.house(), rescue);
        if (dig.rescued()) {
            rescue = false;
        }
        if (dig.mine()) {
            tiles[index] = mineLook(random) | Mines.EXPLODED;
            revealRest(random, mines() - 1);
            phase = BUSTED;
            state.release(reserved);
            reserved = 0;
            state.settle(stake, 0);
            context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE,
                    SoundCategory.BLOCKS, 0.5F, 1.2F));
        } else {
            tiles[index] = random.nextBoolean() ? Mines.EMERALD : Mines.DIAMOND;
            found++;
            long loss = houseLoss(found);
            state.reserve(loss - reserved);
            reserved = loss;
            if (found >= Mines.CELLS - mines()) {
                cashOut(); // achou todas as pedras: saca sozinho
                return;
            }
        }
        events++;
    }

    /** SACAR!: paga o prêmio do momento (antes da primeira pedra, devolve a aposta) e mostra a grade toda. */
    private void cashOut() {
        if (phase != PLAYING) {
            return;
        }
        long payout = Mines.payout(stake, mines(), found);
        lastWin = (int) Math.min(payout, CassinoState.MAX_BALANCE - (long) credits);
        credits += lastWin;
        saveCredits();
        state.release(reserved);
        reserved = 0;
        if (found > 0) {
            state.settle(stake, lastWin);
        } else {
            state.settle(0, 0); // desistiu antes de cavar: a aposta só voltou
        }
        revealRest(player.getRandom(), mines());
        phase = CASHED;
        events++;
        if (found > 0) {
            context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.BLOCKS, 0.6F, 1.0F));
        }
    }

    /** No fim da rodada: espalha as minas que sobraram nos blocos não cavados, e o resto vira pedra preciosa. */
    private void revealRest(Random random, int minesLeft) {
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < Mines.CELLS; i++) {
            if (tiles[i] == Mines.HIDDEN) {
                hidden.add(i);
            }
        }
        for (int i = hidden.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Integer swap = hidden.get(i);
            hidden.set(i, hidden.get(j));
            hidden.set(j, swap);
        }
        for (int i = 0; i < hidden.size(); i++) {
            int look = i < minesLeft ? mineLook(random) : (random.nextBoolean() ? Mines.EMERALD : Mines.DIAMOND);
            tiles[hidden.get(i)] = look | Mines.SHOWN;
        }
    }

    private static int mineLook(Random random) {
        return random.nextBoolean() ? Mines.TNT : Mines.CREEPER;
    }

    /** Fechar a tela no meio da rodada saca sozinho (as fichas não se perdem). */
    @Override
    public void onClosed(PlayerEntity player) {
        if (state != null && phase == PLAYING) {
            cashOut();
        }
        super.onClosed(player);
    }
}
