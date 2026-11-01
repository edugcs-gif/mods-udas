package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras da roleta do cassino: só as cores, sem os números.
 *
 * <p>A roda tem 37 casas, como a roleta europeia: a casa 0 é verde e as outras 36 se alternam entre vermelho (as
 * ímpares) e preto (as pares). O jogador aposta numa cor:
 * <ul>
 *   <li>vermelho ou preto devolve 2x a aposta (18 casas em 37);</li>
 *   <li>verde (o 0) devolve 36x a aposta (1 casa em 37).</li>
 * </ul>
 * Nos dois casos a roleta devolve em média 36/37 ≈ 97% do que é apostado: a casa ganha com o 0.
 *
 * <p>A sorte segue o saldo da casa, como em todos os jogos ({@link Luck}). Com a casa no negativo, numa parte das giradas
 * ({@link Luck#houseLuck}) a roda gira duas vezes e fica com a pior para o jogador. De 0 a +639 a roleta é limpa. De
 * +640 para cima, a chance da cor escolhida sobe {@link #PLAYER_BOOST_PER_STEP} dela a cada degrau (numa parte das
 * derrotas a roda gira de novo): no último degrau, em +1216, vermelho e preto saem em 55% das vezes, o verde em 3,1%,
 * e a roleta devolve uns 110%.
 */
public final class Roulette {
    public static final int POCKETS = 37;

    public static final int GREEN = 0;
    public static final int RED = 1;
    public static final int BLACK = 2;
    public static final int COLORS = 3;

    private Roulette() {
    }

    /** A cor de uma casa da roda: 0 é verde, as ímpares vermelhas e as pares pretas. */
    public static int color(int pocket) {
        return pocket == 0 ? GREEN : pocket % 2 == 1 ? RED : BLACK;
    }

    /** Quantas vezes a aposta volta quando a cor escolhida sai. */
    public static int multiplier(int color) {
        return color == GREEN ? 36 : 2;
    }

    /** O pior caso para a casa numa girada: a cor escolhida sair. */
    public static int worstLoss(int color, int bet) {
        return (multiplier(color) - 1) * bet;
    }

    /** Quanto a chance da cor escolhida sobe (proporcionalmente) a cada degrau do lucro da casa. */
    public static final double PLAYER_BOOST_PER_STEP = 0.013;

    /** Quantas casas da roda têm esta cor. */
    public static int pockets(int color) {
        return color == GREEN ? 1 : (POCKETS - 1) / 2;
    }

    /** Gira a roda para quem apostou na cor {@code choice}: devolve a casa onde a bolinha parou (0 a 36). */
    public static int spin(Random random, int choice, long house) {
        int pocket = random.nextInt(POCKETS);
        if (random.nextFloat() < Luck.houseLuck(house)) {
            // Sorte da casa: gira de novo e fica com a pior para o jogador.
            int again = random.nextInt(POCKETS);
            if (color(pocket) == choice && color(again) != choice) {
                pocket = again;
            }
        } else if (color(pocket) != choice && random.nextFloat()
                < Luck.retryChance(house, PLAYER_BOOST_PER_STEP, pockets(choice) / (double) POCKETS)) {
            // Sorte do jogador: perdeu, mas a roda gira de novo.
            pocket = random.nextInt(POCKETS);
        }
        return pocket;
    }
}
