package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do caça-níquel: 5 rolos com 3 símbolos à mostra cada, e só a linha do meio paga. Ganha quem tirar
 * 3, 4 ou 5 símbolos iguais seguidos nessa linha, a partir do primeiro rolo (da esquerda); o prêmio é a aposta
 * vezes o multiplicador de {@link #PAYOUT}.
 *
 * <p>Como nas máquinas de verdade, primeiro se sorteia o resultado e depois se montam os rolos para mostrá-lo:
 * <ul>
 *   <li>com a casa no positivo (ou até 63 fichas no negativo), 40% das giradas ganham e 60% perdem;</li>
 *   <li>numa vitória, o prêmio sai de {@link #WINS}: quase sempre 3 pás ou 3 esmeraldas (o dobro da aposta), e os
 *       prêmios maiores são cada vez mais raros; o jackpot (cinco 7, 100x) sai uma vez a cada ~25.000 giradas.</li>
 * </ul>
 * Toda vitória paga pelo menos o dobro da aposta, mas no total a máquina devolve uns 88% do que é apostado: quem
 * joga perde mais do que ganha, e a casa lucra no longo prazo.
 *
 * <p>O saldo da casa ({@link CassinoState}) segura quantas fichas o cassino cria no servidor:
 * <ul>
 *   <li>a cada 64 fichas que a casa fica no negativo, a chance de ganhar cai um pouco ({@link #WIN_CHANCE_BY_DEBT}:
 *       40%, 37%, 34%... até 10%), e a máquina passa a devolver menos, até a casa se recuperar;</li>
 *   <li>e nenhum prêmio pode deixar a casa abaixo de -{@value CassinoState#MAX_HOUSE_DEBT}: se o prêmio sorteado
 *       passar disso, sai um menor que caiba; se nem o menor couber, a girada perde.</li>
 * </ul>
 */
public final class SlotMachine {
    public static final int REELS = 5;
    public static final int ROWS = 3;
    public static final int CELLS = REELS * ROWS;
    /** A linha que paga (a do meio). */
    public static final int PAYLINE_ROW = 1;

    public static final int SEVEN = 0;
    public static final int STAR = 1;
    public static final int DIAMOND = 2;
    public static final int HEART = 3;
    public static final int EMERALD = 4;
    public static final int SHOVEL = 5;
    public static final int SYMBOLS = 6;

    /** {@code PAYOUT[símbolo][k - 3]}: quantas vezes a aposta paga k símbolos iguais seguidos (k = 3, 4 ou 5). */
    public static final int[][] PAYOUT = {
            {10, 30, 100}, // 7
            {5, 15, 50},   // estrela
            {4, 8, 25},    // diamante
            {3, 6, 15},    // coração
            {2, 4, 10},    // esmeralda
            {2, 3, 6},     // pá
    };

    /** Fichas no negativo da casa para cada degrau de {@link #WIN_CHANCE_BY_DEBT}. */
    public static final int DEBT_STEP = 64;
    /**
     * Chance de uma girada ganhar alguma coisa, conforme o quanto a casa está no negativo: até -63, 40%; de -64 a
     * -127, 37%; de -128 a -191, 34%; e assim por diante, até 10% de -640 para baixo.
     */
    public static final float[] WIN_CHANCE_BY_DEBT = {0.40F, 0.37F, 0.34F, 0.31F, 0.28F, 0.25F, 0.22F, 0.19F, 0.16F,
            0.13F, 0.10F};
    /**
     * Os prêmios possíveis numa vitória: {símbolo, quantos seguidos, peso}. Os pesos somam 100.000; a chance de
     * cada um por girada é {@code 40% × peso / 100.000} (anotada ao lado).
     */
    private static final int[][] WINS = {
            {SHOVEL, 3, 50_767},  // 2x, 1 em 5
            {EMERALD, 3, 39_800}, // 2x, 1 em 6
            {HEART, 3, 3_000},    // 3x, 1 em 83
            {SHOVEL, 4, 2_500},   // 3x, 1 em 100
            {DIAMOND, 3, 1_250},  // 4x, 1 em 200
            {EMERALD, 4, 1_000},  // 4x, 1 em 250
            {HEART, 4, 400},      // 6x, 1 em 625
            {STAR, 3, 400},       // 5x, 1 em 625
            {SHOVEL, 5, 300},     // 6x, 1 em 833
            {SEVEN, 3, 170},      // 10x, 1 em 1.471
            {DIAMOND, 4, 150},    // 8x, 1 em 1.667
            {EMERALD, 5, 100},    // 10x, 1 em 2.500
            {STAR, 4, 60},        // 15x, 1 em 4.167
            {HEART, 5, 50},       // 15x, 1 em 5.000
            {SEVEN, 4, 23},       // 30x, 1 em 10.870
            {DIAMOND, 5, 20},     // 25x, 1 em 12.500
            {SEVEN, 5, 10},       // 100x, 1 em 25.000 (jackpot)
    };
    private static final int WINS_TOTAL = 100_000;

    /** Quantas "bolinhas" cada símbolo tem nos rolos que só enfeitam (e nas derrotas): o 7 aparece pouco. */
    private static final int[] WEIGHTS = {1, 2, 3, 4, 5, 6};
    private static final int WEIGHTS_TOTAL = 21;

    /** As apostas possíveis, em fichas por girada. */
    public static final int[] BETS = {1, 2, 5, 10, 20, 50};
    public static final int DEFAULT_BET = 2;

    private SlotMachine() {
    }

    /** A célula do rolo {@code reel} (0 a 4) na linha {@code row} (0 = cima, 1 = meio, 2 = baixo). */
    public static int cell(int reel, int row) {
        return reel * ROWS + row;
    }

    private static int draw(Random random) {
        int roll = random.nextInt(WEIGHTS_TOTAL);
        for (int symbol = 0; symbol < WEIGHTS.length; symbol++) {
            if (roll < WEIGHTS[symbol]) {
                return symbol;
            }
            roll -= WEIGHTS[symbol];
        }
        return SHOVEL;
    }

    /** Um símbolo qualquer, menos {@code not}. */
    private static int drawExcept(Random random, int not) {
        int symbol;
        do {
            symbol = draw(random);
        } while (symbol == not);
        return symbol;
    }

    /** A chance de ganhar com a casa neste saldo. */
    public static float winChance(long house) {
        int step = house >= 0 ? 0 : (int) Math.min(WIN_CHANCE_BY_DEBT.length - 1, -house / DEBT_STEP);
        return WIN_CHANCE_BY_DEBT[step];
    }

    /**
     * Gira os rolos: os 15 símbolos à mostra (veja {@link #cell}). Só a linha do meio vale; as outras enfeitam.
     *
     * @param bet   a aposta desta girada
     * @param house o saldo da casa antes dela (veja {@link CassinoState})
     */
    public static int[] spin(Random random, int bet, long house) {
        // O maior prêmio (em vezes a aposta) que não deixa a casa abaixo do limite.
        long maxMultiplier = (house + CassinoState.MAX_HOUSE_DEBT + bet) / bet;
        int[] prize = random.nextFloat() < winChance(house) ? pickWin(random, maxMultiplier) : null;
        int[] payline = new int[REELS];
        if (prize != null) {
            int symbol = prize[0];
            int length = prize[1];
            for (int reel = 0; reel < REELS; reel++) {
                // Os primeiros "length" rolos com o símbolo do prêmio; o seguinte, outro qualquer (para não pagar mais).
                payline[reel] = reel < length ? symbol : reel == length ? drawExcept(random, symbol) : draw(random);
            }
        } else {
            // Derrota: nada de três iguais seguidos desde o primeiro rolo (dois iguais pode: é o "quase").
            payline[0] = draw(random);
            payline[1] = draw(random);
            payline[2] = payline[0] == payline[1] ? drawExcept(random, payline[0]) : draw(random);
            payline[3] = draw(random);
            payline[4] = draw(random);
        }
        int[] cells = new int[CELLS];
        for (int reel = 0; reel < REELS; reel++) {
            cells[cell(reel, 0)] = draw(random);
            cells[cell(reel, PAYLINE_ROW)] = payline[reel];
            cells[cell(reel, 2)] = draw(random);
        }
        return cells;
    }

    /** Sorteia um prêmio de {@link #WINS} entre os que pagam até {@code maxMultiplier}; {@code null} se nenhum couber. */
    private static int[] pickWin(Random random, long maxMultiplier) {
        int total = 0;
        for (int[] win : WINS) {
            if (payout(win) <= maxMultiplier) {
                total += win[2];
            }
        }
        if (total == 0) {
            return null;
        }
        int roll = random.nextInt(total);
        for (int[] win : WINS) {
            if (payout(win) <= maxMultiplier) {
                if (roll < win[2]) {
                    return win;
                }
                roll -= win[2];
            }
        }
        return null;
    }

    private static int payout(int[] win) {
        return PAYOUT[win[0]][win[1] - 3];
    }

    /** Símbolos sem valer nada, para os rolos não abrirem vazios. */
    public static int[] idle(Random random) {
        int[] cells = new int[CELLS];
        for (int i = 0; i < CELLS; i++) {
            cells[i] = draw(random);
        }
        return cells;
    }

    /** Quantos símbolos iguais ao do primeiro rolo aparecem seguidos na linha do meio (1 a 5). */
    public static int runLength(int[] cells) {
        int first = cells[cell(0, PAYLINE_ROW)];
        int length = 1;
        while (length < REELS && cells[cell(length, PAYLINE_ROW)] == first) {
            length++;
        }
        return length;
    }

    /** Quantas vezes a aposta esta girada paga (0 = não ganhou). */
    public static int multiplier(int[] cells) {
        int length = runLength(cells);
        return length >= 3 ? PAYOUT[cells[cell(0, PAYLINE_ROW)]][length - 3] : 0;
    }

    /** Cinco 7 na linha do meio. */
    public static boolean isJackpot(int[] cells) {
        return cells[cell(0, PAYLINE_ROW)] == SEVEN && runLength(cells) == REELS;
    }
}
