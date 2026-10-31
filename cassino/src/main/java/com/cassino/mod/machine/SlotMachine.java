package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do caça-níquel: 5 rolos com 3 símbolos à mostra cada, e só a linha do meio paga. Ganha quem tirar
 * 3, 4 ou 5 símbolos iguais seguidos nessa linha, a partir do primeiro rolo (da esquerda); o prêmio é a aposta
 * vezes o multiplicador de {@link #PAYOUT}.
 *
 * <p>Como nas máquinas de verdade, primeiro se sorteia o resultado e depois se montam os rolos para mostrá-lo:
 * <ul>
 *   <li>{@link #WIN_CHANCE} (40%) das giradas ganham e 60% perdem;</li>
 *   <li>numa vitória, o prêmio sai de {@link #WINS}: quase sempre 3 pás ou 3 esmeraldas (o dobro da aposta), e os
 *       prêmios maiores são cada vez mais raros; o jackpot (cinco 7, 100x) sai uma vez a cada ~25.000 giradas.</li>
 * </ul>
 * Toda vitória paga pelo menos o dobro da aposta, mas no total a máquina devolve uns 88% do que é apostado: quem
 * joga perde mais do que ganha, e a casa lucra no longo prazo.
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

    /** Chance de uma girada ganhar alguma coisa. */
    public static final float WIN_CHANCE = 0.40F;
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

    /** Gira os rolos: os 15 símbolos à mostra (veja {@link #cell}). Só a linha do meio vale; as outras enfeitam. */
    public static int[] spin(Random random) {
        int[] payline = new int[REELS];
        if (random.nextFloat() < WIN_CHANCE) {
            int[] prize = pickWin(random);
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

    private static int[] pickWin(Random random) {
        int roll = random.nextInt(WINS_TOTAL);
        for (int[] win : WINS) {
            if (roll < win[2]) {
                return win;
            }
            roll -= win[2];
        }
        return WINS[0];
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
