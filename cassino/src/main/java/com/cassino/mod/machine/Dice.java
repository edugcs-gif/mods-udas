package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do jogo de dados do cassino: dois dados de 6 faces, e as apostas são sobre a soma (de 2 a 12).
 *
 * <ul>
 *   <li>{@link #EVEN PARES} e {@link #ODD ÍMPARES}: a soma par ou ímpar (18 jeitos em 36 cada); pagam x1,9.</li>
 *   <li>{@link #NUMBER NÚMERO}: a soma exata, escolhida de 2 a 12; paga conforme a chance (x5,7 no 7, que sai em 6
 *       jeitos de 36, até x34,2 no 2 e no 12, que saem em 1).</li>
 *   <li>{@link #LOW MENOR} (2 a 6) e {@link #HIGH MAIOR} (8 a 12): 15 jeitos em 36 cada; pagam x2,2. O 7 não é nem
 *       menor nem maior.</li>
 * </ul>
 *
 * <p>Dá para marcar várias apostas na mesma jogada: cada uma custa a aposta, e cada uma que acertar paga o seu prêmio.
 * O prêmio é a aposta vezes o multiplicador, arredondado; cada aposta devolve uns 92 a 95% no longo prazo (nunca mais
 * de 100%), e a casa ganha a diferença.
 *
 * <p>A sorte segue o saldo da casa, como em todos os jogos ({@link Luck}). Com a casa no negativo, numa parte das
 * jogadas ({@link Luck#houseLuck}) os dados rolam duas vezes e fica o pior para o jogador. De 0 a +639 os dados são
 * limpos. De +640 para cima, numa parte das jogadas perdidas os dados rolam de novo ({@link #PLAYER_BOOST_PER_STEP}),
 * e no último degrau, em +1216, o jogo devolve uns 110%.
 */
public final class Dice {
    public static final int EVEN = 0;
    public static final int ODD = 1;
    public static final int NUMBER = 2;
    public static final int LOW = 3;
    public static final int HIGH = 4;
    /** Quantos tipos de aposta existem. */
    public static final int TYPES = 5;

    public static final int MIN_TOTAL = 2;
    public static final int MAX_TOTAL = 12;
    /** As combinações de dois dados. */
    public static final int OUTCOMES = 36;

    /** Quanto paga cada aposta, em décimos (19 = x1,9); a do NÚMERO está em {@link #NUMBER_ODDS_10}. */
    private static final int[] ODDS_10 = {19, 19, 0, 22, 22};
    /** Quanto paga o NÚMERO, em décimos, para cada soma de 2 a 12: uns 95% de 36 ÷ os jeitos de a soma sair. */
    private static final int[] NUMBER_ODDS_10 = {342, 171, 114, 85, 68, 57, 68, 85, 114, 171, 342};

    /** Quanto a chance de ganhar sobe (proporcionalmente) a cada degrau do lucro da casa. */
    public static final double PLAYER_BOOST_PER_STEP = 0.016;

    private Dice() {
    }

    /** De quantos jeitos (em 36) dois dados somam {@code total}: 1 para o 2 e o 12, até 6 para o 7. */
    public static int ways(int total) {
        return total < MIN_TOTAL || total > MAX_TOTAL ? 0 : 6 - Math.abs(total - 7);
    }

    /** Quanto a aposta {@code type} paga, em décimos ({@code number} é o número escolhido para o NÚMERO). */
    public static int odds10(int type, int number) {
        return type == NUMBER ? NUMBER_ODDS_10[number - MIN_TOTAL] : ODDS_10[type];
    }

    /** Se a aposta {@code type} ganha com esta soma. */
    public static boolean wins(int type, int number, int total) {
        return switch (type) {
            case EVEN -> total % 2 == 0;
            case ODD -> total % 2 == 1;
            case NUMBER -> total == number;
            case LOW -> total <= 6;
            case HIGH -> total >= 8;
            default -> false;
        };
    }

    /** O prêmio de uma aposta que acertou: a aposta vezes o multiplicador, arredondado. */
    public static int prize(int stake, int odds10) {
        return (stake * odds10 + 5) / 10;
    }

    /** Quanto custa a jogada: a aposta vezes quantas apostas estão marcadas em {@code mask} (um bit por tipo). */
    public static int cost(int mask, int stake) {
        return Integer.bitCount(mask & ((1 << TYPES) - 1)) * stake;
    }

    /** O que as apostas marcadas devolvem, juntas, com esta soma. */
    public static int payout(int mask, int number, int stake, int total) {
        int paid = 0;
        for (int type = 0; type < TYPES; type++) {
            if ((mask & (1 << type)) != 0 && wins(type, number, total)) {
                paid += prize(stake, odds10(type, number));
            }
        }
        return paid;
    }

    /** O pior caso para a casa: a soma que mais paga, menos o que a jogada custou. */
    public static int worstLoss(int mask, int number, int stake) {
        int worst = 0;
        for (int total = MIN_TOTAL; total <= MAX_TOTAL; total++) {
            worst = Math.max(worst, payout(mask, number, stake, total));
        }
        return worst - cost(mask, stake);
    }

    /** A chance de a jogada sair no lucro (devolver mais do que custou), com dados limpos. */
    public static double winChance(int mask, int number, int stake) {
        int cost = cost(mask, stake);
        int ways = 0;
        for (int total = MIN_TOTAL; total <= MAX_TOTAL; total++) {
            if (payout(mask, number, stake, total) > cost) {
                ways += ways(total);
            }
        }
        return ways / (double) OUTCOMES;
    }

    /** Rola os dois dados para esta jogada: devolve {dado 1, dado 2}, cada um de 1 a 6. */
    public static int[] roll(Random random, int mask, int number, int stake, long house) {
        int[] dice = throwDice(random);
        if (random.nextFloat() < Luck.houseLuck(house)) {
            // Sorte da casa: rola de novo e fica com o pior para o jogador.
            int[] again = throwDice(random);
            if (payout(mask, number, stake, sum(again)) < payout(mask, number, stake, sum(dice))) {
                dice = again;
            }
        } else if (payout(mask, number, stake, sum(dice)) <= cost(mask, stake)
                && random.nextFloat() < Luck.retryChance(house, PLAYER_BOOST_PER_STEP,
                winChance(mask, number, stake))) {
            // Sorte do jogador: perdeu, mas os dados rolam de novo.
            dice = throwDice(random);
        }
        return dice;
    }

    private static int[] throwDice(Random random) {
        return new int[] {1 + random.nextInt(6), 1 + random.nextInt(6)};
    }

    private static int sum(int[] dice) {
        return dice[0] + dice[1];
    }
}
