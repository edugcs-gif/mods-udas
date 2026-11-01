package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.Blackjack;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.SlotMachine;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * A mesa de blackjack, do lado do servidor (é ele quem dá as cartas). As regras estão em {@link Blackjack}; os
 * créditos, o espaço das fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O botão verde dá as cartas (gastando a aposta) e, durante a mão, pede mais uma. A aposta e o resultado entram no
 * saldo da casa, o mesmo do caça-níquel. Para o limite de -640 valer mesmo com a mão aberta, a mesa reserva o pior
 * caso para a casa ao dar as cartas e ao dobrar, e só aceita a aposta se ele couber. Se o jogador fechar a tela (ou
 * sair do jogo) no meio da mão, ele para e a mão termina normalmente.
 */
public class BlackjackScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_DEAL_OR_HIT = 0;
    public static final int BUTTON_STAND = 1;
    public static final int BUTTON_DOUBLE = 2;
    public static final int BUTTON_BET_UP = 3;
    public static final int BUTTON_BET_DOWN = 4;
    public static final int BUTTON_DEPOSIT = 5;
    public static final int BUTTON_WITHDRAW = 6;

    public static final int RESULT_NONE = 0;
    public static final int RESULT_WIN = 1;
    public static final int RESULT_DEALER_BUST = 2;
    public static final int RESULT_BLACKJACK = 3;
    public static final int RESULT_PUSH = 4;
    public static final int RESULT_LOSE = 5;
    public static final int RESULT_BUST = 6;
    public static final int RESULT_DEALER_BLACKJACK = 7;

    // ---------------------------------------------------------------- desenho (em pixels, a partir do canto da tela)

    public static final int WIDTH = 276;
    public static final int HEIGHT = 238;
    public static final int COIN_SLOT_X = 154;
    public static final int COIN_SLOT_Y = 106;
    public static final int INVENTORY_X = 57;
    public static final int INVENTORY_Y = 158;
    public static final int HOTBAR_Y = 216;

    // ---------------------------------------------------------------- propriedades enviadas para a tela

    private static final int P_BET = 0;
    private static final int P_PLAYING = 1;
    private static final int P_RESULT = 2;
    private static final int P_PAYOUT_LOW = 3;
    private static final int P_PAYOUT_HIGH = 4;
    private static final int P_HAND_BET = 5;
    private static final int P_MAX_BET = 6;
    private static final int P_CAN_DOUBLE = 7;
    private static final int P_DEALER_COUNT = 8;
    private static final int P_PLAYER_COUNT = 9;
    private static final int P_DEALER = 10;
    private static final int P_PLAYER = P_DEALER + Blackjack.MAX_CARDS;
    /** O contador de mãos vai por último: quando a tela o vê mudar, as cartas novas já chegaram. */
    private static final int P_HANDS = P_PLAYER + Blackjack.MAX_CARDS;
    private static final int P_COUNT = P_HANDS + 1;

    private int betIndex = 3;
    private boolean playing;
    private int result;
    private int payout;
    /** A aposta da mão em andamento (dobra se o jogador dobrar). */
    private int handBet;
    private int maxBet;
    private boolean canDouble;
    private final int[] dealer = new int[Blackjack.MAX_CARDS];
    private final int[] hand = new int[Blackjack.MAX_CARDS];
    private int dealerCount;
    private int handCount;
    private int hands;
    /** Quanto esta mão reservou no cofre (o pior caso para a casa). */
    private int reserved;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public BlackjackScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public BlackjackScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.BLACKJACK_HANDLER, syncId, playerInventory, context, Cassino.BLACKJACK_TABLE, COIN_SLOT_X,
                COIN_SLOT_Y, INVENTORY_X, INVENTORY_Y, HOTBAR_Y);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_PLAYING -> playing ? 1 : 0;
                    case P_RESULT -> result;
                    case P_PAYOUT_LOW -> low(payout);
                    case P_PAYOUT_HIGH -> high(payout);
                    case P_HAND_BET -> handBet;
                    case P_MAX_BET -> state == null ? maxBet : tableMaxBet();
                    case P_CAN_DOUBLE -> (state == null ? canDouble : doubleAllowed()) ? 1 : 0;
                    case P_DEALER_COUNT -> dealerCount;
                    case P_PLAYER_COUNT -> handCount;
                    case P_HANDS -> hands & 0x7FFF;
                    default -> {
                        if (index < P_PLAYER) {
                            int i = index - P_DEALER;
                            // A segunda carta do dealer fica virada enquanto o jogador decide (o cliente nem a recebe).
                            yield i == 1 && playing && state != null ? Blackjack.HIDDEN : dealer[i];
                        }
                        yield hand[index - P_PLAYER];
                    }
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_PLAYING -> playing = value != 0;
                    case P_RESULT -> result = value;
                    case P_PAYOUT_LOW -> payout = withLow(payout, value);
                    case P_PAYOUT_HIGH -> payout = withHigh(payout, value);
                    case P_HAND_BET -> handBet = value;
                    case P_MAX_BET -> maxBet = value;
                    case P_CAN_DOUBLE -> canDouble = value != 0;
                    case P_DEALER_COUNT -> dealerCount = value;
                    case P_PLAYER_COUNT -> handCount = value;
                    case P_HANDS -> hands = value;
                    default -> {
                        if (index < P_PLAYER) {
                            dealer[index - P_DEALER] = value;
                        } else {
                            hand[index - P_PLAYER] = value;
                        }
                    }
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

    public boolean playing() {
        return playing;
    }

    public int result() {
        return result;
    }

    /** Quanto a última mão devolveu (a aposta junto com o prêmio). */
    public int payout() {
        return payout;
    }

    public int handBet() {
        return handBet;
    }

    /** A maior aposta que a mesa aceita agora (0 = nenhuma), pelo limite da casa. */
    public int maxBet() {
        return maxBet;
    }

    public boolean canDouble() {
        return canDouble;
    }

    public int[] dealerCards() {
        return dealer;
    }

    public int dealerCount() {
        return dealerCount;
    }

    public int[] playerCards() {
        return hand;
    }

    public int playerCount() {
        return handCount;
    }

    public int hands() {
        return hands;
    }

    // ---------------------------------------------------------------- limite da casa (só no servidor)

    /** Se a casa aguenta o pior caso de mais {@code extra} fichas sem passar do limite. */
    private boolean fits(long extra) {
        return state.available() - extra >= -CassinoState.MAX_HOUSE_DEBT;
    }

    private int tableMaxBet() {
        int best = 0;
        for (int bet : SlotMachine.BETS) {
            if (fits(Blackjack.worstLossOnDeal(bet))) {
                best = bet;
            }
        }
        return best;
    }

    /** Dobrar: o pior caso passa a ser perder a aposta dobrada (2x a aposta inicial) em vez do blackjack. */
    private int extraReserveForDouble() {
        return Math.max(0, 2 * handBet - reserved);
    }

    private boolean doubleAllowed() {
        return playing && handCount == 2 && credits + coinsWaiting() >= handBet && fits(extraReserveForDouble());
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        switch (id) {
            case BUTTON_DEAL_OR_HIT -> {
                if (playing) {
                    hit();
                } else {
                    deal();
                }
            }
            case BUTTON_STAND -> {
                if (playing) {
                    stand();
                }
            }
            case BUTTON_DOUBLE -> doubleDown();
            case BUTTON_BET_UP -> {
                if (!playing) {
                    betIndex = Math.min(betIndex + 1, SlotMachine.BETS.length - 1);
                }
            }
            case BUTTON_BET_DOWN -> {
                if (!playing) {
                    betIndex = Math.max(betIndex - 1, 0);
                }
            }
            case BUTTON_DEPOSIT -> deposit();
            case BUTTON_WITHDRAW -> withdraw(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private int draw(int[] cards, int count, boolean dealerHand) {
        return Blackjack.draw(player.getRandom(), cards, count, dealerHand, state.house());
    }

    private void deal() {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou dar as cartas: deposita antes
        }
        int worst = Blackjack.worstLossOnDeal(bet);
        if (credits < bet || !fits(worst)) {
            return;
        }
        credits -= bet;
        saveCredits();
        handBet = bet;
        reserved = worst;
        state.reserve(reserved);

        handCount = 0;
        dealerCount = 0;
        hand[handCount] = draw(hand, handCount, false);
        handCount++;
        dealer[dealerCount] = draw(dealer, dealerCount, true);
        dealerCount++;
        hand[handCount] = draw(hand, handCount, false);
        handCount++;
        dealer[dealerCount] = draw(dealer, dealerCount, true);
        dealerCount++;
        playing = true;
        result = RESULT_NONE;
        payout = 0;
        hands++;
        playSound(1.0F);

        if (Blackjack.isBlackjack(hand, handCount) || Blackjack.isBlackjack(dealer, dealerCount)) {
            finish();
        }
    }

    private void hit() {
        hand[handCount] = draw(hand, handCount, false);
        handCount++;
        playSound(1.1F);
        int value = Blackjack.value(hand, handCount);
        if (value > 21) {
            finish();
        } else if (value == 21 || handCount == Blackjack.MAX_CARDS) {
            stand();
        }
    }

    /** O jogador parou: o dealer compra até ter 17 ou mais. */
    private void stand() {
        while (Blackjack.value(dealer, dealerCount) < 17 && dealerCount < Blackjack.MAX_CARDS) {
            dealer[dealerCount] = draw(dealer, dealerCount, true);
            dealerCount++;
        }
        finish();
    }

    private void doubleDown() {
        if (!playing || handCount != 2) {
            return;
        }
        if (credits < handBet) {
            deposit();
        }
        int extra = extraReserveForDouble();
        if (credits < handBet || !fits(extra)) {
            return;
        }
        credits -= handBet;
        saveCredits();
        state.reserve(extra);
        reserved += extra;
        handBet *= 2;
        hand[handCount] = draw(hand, handCount, false);
        handCount++;
        playSound(1.1F);
        if (Blackjack.value(hand, handCount) > 21) {
            finish();
        } else {
            stand();
        }
    }

    /** Fim da mão: decide o resultado, paga e acerta o saldo da casa. */
    private void finish() {
        int mine = Blackjack.value(hand, handCount);
        int theirs = Blackjack.value(dealer, dealerCount);
        boolean myBlackjack = Blackjack.isBlackjack(hand, handCount);
        boolean theirBlackjack = Blackjack.isBlackjack(dealer, dealerCount);
        if (mine > 21) {
            result = RESULT_BUST;
            payout = 0;
        } else if (myBlackjack && theirBlackjack) {
            result = RESULT_PUSH;
            payout = handBet;
        } else if (myBlackjack) {
            result = RESULT_BLACKJACK;
            payout = Blackjack.blackjackPayout(handBet);
        } else if (theirBlackjack) {
            result = RESULT_DEALER_BLACKJACK;
            payout = 0;
        } else if (theirs > 21) {
            result = RESULT_DEALER_BUST;
            payout = 2 * handBet;
        } else if (mine > theirs) {
            result = RESULT_WIN;
            payout = 2 * handBet;
        } else if (mine == theirs) {
            result = RESULT_PUSH;
            payout = handBet;
        } else {
            result = RESULT_LOSE;
            payout = 0;
        }
        payout = Math.min(payout, CassinoState.MAX_BALANCE - credits);
        credits += payout;
        saveCredits();
        state.release(reserved);
        reserved = 0;
        state.settle(handBet, payout);
        playing = false;
        if (payout > handBet) {
            context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    SoundCategory.BLOCKS, 0.8F, result == RESULT_BLACKJACK ? 1.4F : 1.0F));
        }
    }

    private void playSound(float pitch) {
        context.run((world, pos) -> world.playSound(null, pos, SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.BLOCKS,
                1.0F, pitch));
    }

    // ---------------------------------------------------------------- tela

    @Override
    public void onClosed(PlayerEntity player) {
        // Fechou (ou saiu do jogo) no meio da mão: para, e a mão termina normalmente.
        if (playing && state != null) {
            stand();
        }
        super.onClosed(player);
    }
}
