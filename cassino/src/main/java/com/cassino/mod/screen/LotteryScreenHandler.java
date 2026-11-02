package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.Lottery;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * A loteria de mobs, do lado do servidor. As regras e o sorteio estão em {@link Lottery}; os créditos, o espaço das
 * fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O jogador escolhe um mob na tabela e aperta APOSTAR!: saem {@value Lottery#BET} fichas dos créditos, que vão para
 * o prêmio da loteria (separado do saldo da casa dos outros jogos). Uma aposta por dia.
 */
public class LotteryScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_DEPOSIT = 0;
    public static final int BUTTON_WITHDRAW = 1;
    /** Apostar num mob: {@code BUTTON_BET + o número do mob (0 a 24)}. */
    public static final int BUTTON_BET = 2;

    // ---------------------------------------------------------------- desenho (em pixels, a partir do canto da tela)

    public static final int WIDTH = 316;
    public static final int HEIGHT = 246;
    public static final int COIN_SLOT_X = 214;
    public static final int COIN_SLOT_Y = 143;
    public static final int INVENTORY_X = 77;
    public static final int INVENTORY_Y = 166;
    public static final int HOTBAR_Y = 224;

    // ---------------------------------------------------------------- propriedades enviadas para a tela

    private static final int P_MY_BET = 0;
    private static final int P_POT_LOW = 1;
    private static final int P_POT_HIGH = 2;
    private static final int P_LAST_MOB = 3;
    private static final int P_SECONDS = 4;
    private static final int P_BETS_TODAY = 5;
    private static final int P_COUNT = 6;

    private int myBet = -1;
    private int pot;
    private int lastMob = -1;
    private int seconds;
    private int betsToday;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public LotteryScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public LotteryScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.LOTTERY_HANDLER, syncId, playerInventory, context, Cassino.LOTTERY, COIN_SLOT_X, COIN_SLOT_Y,
                INVENTORY_X, INVENTORY_Y, HOTBAR_Y);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (state != null) {
                    refresh();
                }
                return switch (index) {
                    case P_MY_BET -> myBet;
                    case P_POT_LOW -> low(pot);
                    case P_POT_HIGH -> high(pot);
                    case P_LAST_MOB -> lastMob;
                    case P_SECONDS -> seconds;
                    default -> betsToday;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_MY_BET -> myBet = (short) value; // -1 chega como 0xFFFF
                    case P_POT_LOW -> pot = withLow(pot, value);
                    case P_POT_HIGH -> pot = withHigh(pot, value);
                    case P_LAST_MOB -> lastMob = (short) value;
                    case P_SECONDS -> seconds = value & 0xFFFF;
                    default -> betsToday = value & 0xFFFF;
                }
            }

            @Override
            public int size() {
                return P_COUNT;
            }
        });
    }

    /** Copia do cofre o que a tela mostra (só no servidor). */
    private void refresh() {
        myBet = state.lotteryBet(player.getUuid());
        pot = (int) Math.min(Integer.MAX_VALUE, state.lotteryPrize());
        lastMob = state.lastLotteryMob();
        seconds = Math.min(0xFFFF, Lottery.secondsLeft(player.getServer()));
        betsToday = Math.min(0xFFFF, state.lotteryBets().size());
    }

    // ---------------------------------------------------------------- o que a tela mostra

    /** O mob em que o jogador apostou hoje, ou -1. */
    public int myBet() {
        return myBet;
    }

    /** O prêmio acumulado da loteria. */
    public int pot() {
        return pot;
    }

    /** O último mob sorteado, ou -1. */
    public int lastMob() {
        return lastMob;
    }

    /** Quantos segundos faltam para o sorteio. */
    public int seconds() {
        return seconds;
    }

    /** Quantas apostas (de todos os jogadores) entraram hoje. */
    public int betsToday() {
        return betsToday;
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= BUTTON_BET && id < BUTTON_BET + Lottery.COUNT) {
            bet(id - BUTTON_BET);
            return true;
        }
        switch (id) {
            case BUTTON_DEPOSIT -> deposit();
            case BUTTON_WITHDRAW -> withdraw(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void bet(int mob) {
        if (!(player instanceof ServerPlayerEntity serverPlayer) || state.lotteryBet(player.getUuid()) >= 0) {
            return;
        }
        if (credits < Lottery.BET) {
            deposit(); // colocou fichas e já mandou apostar: deposita antes
        }
        if (credits < Lottery.BET) {
            return;
        }
        if (!Lottery.placeBet(serverPlayer.getServer(), serverPlayer, mob)) {
            return;
        }
        credits -= Lottery.BET;
        saveCredits();
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                SoundCategory.BLOCKS, 0.8F, 1.2F));
    }
}
