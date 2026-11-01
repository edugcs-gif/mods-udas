package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do blackjack (21) da mesa do cassino.
 *
 * <p>Cada carta é um número de 0 a 51: o naipe é {@code carta / 13} ({@link #COPAS}, {@link #OUROS},
 * {@link #ESPADAS}, {@link #PAUS}) e o valor é {@code carta % 13} (0 = Ás, 1 a 9 = 2 a 10, 10 = Valete, 11 = Dama,
 * 12 = Rei). O baralho é "infinito": cada carta é sorteada do zero, como numa mesa com muitos baralhos.
 *
 * <ul>
 *   <li>Ás vale 11 ou 1; figuras valem 10. Quem passa de 21 estoura e perde.</li>
 *   <li>Blackjack (Ás + carta de 10 nas duas primeiras) paga 3:2; vitória paga 1:1; empate devolve a aposta.</li>
 *   <li>O dealer compra até ter 17 ou mais (e para no 17, mesmo com Ás valendo 11).</li>
 *   <li>Com as duas primeiras cartas o jogador pode dobrar: dobra a aposta, recebe só mais uma carta e para.</li>
 * </ul>
 *
 * <p>A "sorte da mesa" segue o saldo da casa, como em todos os jogos ({@link Luck}). Com a casa no negativo, numa parte
 * das cartas ({@link Luck#houseLuck}) a mesa escolhe a dedo a melhor carta para o dealer e a pior para o jogador (em
 * todas, perto de -5000: aí o dealer sempre ganha); de 0 a +639 o jogo é limpo (uns 99%);
 * de +640 para cima, a melhor vai para o jogador ({@link #playerLuck}: 1,5% a cada 64, até 15% em +1216, quando a
 * mesa devolve uns 110% para quem joga perfeito).
 */
public final class Blackjack {
    public static final int COPAS = 0;
    public static final int OUROS = 1;
    public static final int ESPADAS = 2;
    public static final int PAUS = 3;
    public static final int DECK = 52;
    /** Uma carta virada para baixo (a segunda do dealer, enquanto o jogador decide). Só a tela usa. */
    public static final int HIDDEN = DECK;
    /** Quantas cartas cabem numa mão (passar disso sem estourar é impossível: 11 cartas já passam de 21). */
    public static final int MAX_CARDS = 11;

    private Blackjack() {
    }

    public static int suit(int card) {
        return card / 13;
    }

    /** "A", "2" a "10", "J", "Q" ou "K". */
    public static String rankLabel(int card) {
        int rank = card % 13;
        return switch (rank) {
            case 0 -> "A";
            case 10 -> "J";
            case 11 -> "Q";
            case 12 -> "K";
            default -> Integer.toString(rank + 1);
        };
    }

    private static int points(int card) {
        int rank = card % 13;
        return rank == 0 ? 11 : Math.min(10, rank + 1);
    }

    /** O valor da mão (os ases valem 11 enquanto não estouram; senão, 1). */
    public static int value(int[] cards, int count) {
        int total = 0;
        int aces = 0;
        for (int i = 0; i < count; i++) {
            total += points(cards[i]);
            if (cards[i] % 13 == 0) {
                aces++;
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    /** Ás + carta de 10, nas duas primeiras cartas. */
    public static boolean isBlackjack(int[] cards, int count) {
        return count == 2 && value(cards, count) == 21;
    }

    /** O que a mão recebe de volta com blackjack: a aposta mais 3/2 dela (arredondado para baixo). */
    public static int blackjackPayout(int bet) {
        return bet + bet * 3 / 2;
    }

    /** O pior caso para a casa numa mão que começa com esta aposta: o jogador tirar blackjack. */
    public static int worstLossOnDeal(int bet) {
        return blackjackPayout(bet) - bet;
    }

    // ---------------------------------------------------------------- sorte da mesa

    /** Quanto a sorte do jogador sobe a cada degrau do lucro da casa ({@link Luck#profitStep}). */
    public static final float PLAYER_LUCK_PER_STEP = 0.015F;

    /** Com a casa em +640 ou mais: a chance de uma carta sair a melhor para o jogador entre duas. */
    public static float playerLuck(long house) {
        return PLAYER_LUCK_PER_STEP * Luck.profitStep(house);
    }

    /**
     * Tira uma carta para a mão {@code hand} (com {@code count} cartas). {@code dealerHand} diz de quem é a mão. Com a
     * sorte da casa (casa no negativo) a mão do dealer fica com a melhor de duas cartas, e a do jogador com a pior; com
     * a sorte do jogador (casa em +640 ou mais), o contrário.
     */
    public static int draw(Random random, int[] hand, int count, boolean dealerHand, long house) {
        int card = random.nextInt(DECK);
        float houseLuck = Luck.houseLuck(house);
        // Uma mão tem várias cartas: a chance por carta é menor (1 - raiz de 1 - sorte), para a mesa acompanhar os
        // outros jogos; no último degrau ela também é 100%.
        if (houseLuck > 0 && random.nextFloat() < 1.0F - (float) Math.sqrt(1.0F - houseLuck)) {
            // Sorte da casa: a melhor carta possível para o dealer, a pior possível para o jogador.
            return rigged(random, hand, count, dealerHand);
        }
        if (random.nextFloat() >= playerLuck(house)) {
            return card;
        }
        // Sorte do jogador: tira duas e dá a melhor para ele e a pior para o dealer.
        int other = random.nextInt(DECK);
        int scoreCard = score(hand, count, card);
        int scoreOther = score(hand, count, other);
        if (!dealerHand) {
            return scoreOther > scoreCard ? other : card;
        }
        return scoreOther < scoreCard ? other : card;
    }

    /**
     * A carta escolhida a dedo entre os 13 valores (de um naipe qualquer): a que deixa a mão do dealer melhor, ou a do
     * jogador pior (estourando, se der). Com a casa no último degrau toda carta é assim, e o dealer sempre ganha.
     */
    private static int rigged(Random random, int[] hand, int count, boolean dealerHand) {
        int chosen = -1;
        int chosenScore = 0;
        int ties = 0;
        for (int rank = 0; rank < 13; rank++) {
            int card = rank + 13 * random.nextInt(4);
            int score = score(hand, count, card);
            boolean better = chosen < 0 || (dealerHand ? score > chosenScore : score < chosenScore);
            if (better) {
                chosen = card;
                chosenScore = score;
                ties = 1;
            } else if (score == chosenScore && random.nextInt(++ties) == 0) {
                chosen = card; // empate (10, J, Q, K): um deles ao acaso
            }
        }
        return chosen;
    }

    /** Quão boa fica a mão com esta carta: o valor dela, ou -1 se estourar. */
    private static int score(int[] hand, int count, int card) {
        int[] next = new int[count + 1];
        System.arraycopy(hand, 0, next, 0, count);
        next[count] = card;
        int value = value(next, count + 1);
        return value > 21 ? -1 : value;
    }
}
