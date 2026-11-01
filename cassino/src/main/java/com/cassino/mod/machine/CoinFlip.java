package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do cara ou coroa do cassino.
 *
 * <p>O jogador aposta em {@link #HEADS CARA} (o rosto) ou {@link #TAILS COROA}, e acertar devolve
 * {@value #MULTIPLIER}x a aposta. A casa ganha porque, uma vez em 25 ({@link #EDGE_CHANCE}), a moeda cai
 * {@link #EDGE em pé}, e aí ninguém acerta: cada lado sai em 48% das vezes, e o jogo devolve uns 96%.
 *
 * <p>A sorte segue o saldo da casa, como em todos os jogos ({@link Luck}). Com a casa no negativo, numa parte dos
 * lançamentos ({@link Luck#houseLuck}) a moeda é lançada duas vezes e fica o pior para o jogador. De 0 a +639 a moeda
 * é limpa. De +640 para cima, numa parte dos lançamentos perdidos ela é lançada de novo
 * ({@link #PLAYER_BOOST_PER_STEP}), e no último degrau, em +1216, o jogo devolve uns 110%.
 */
public final class CoinFlip {
    public static final int HEADS = 0;
    public static final int TAILS = 1;
    /** A moeda caiu em pé: a casa ganha. */
    public static final int EDGE = 2;
    /** Os lados em que dá para apostar. */
    public static final int SIDES = 2;

    public static final int MULTIPLIER = 2;
    /** A chance de a moeda cair em pé (1 em 25). */
    public static final float EDGE_CHANCE = 0.04F;
    /** Quanto a chance de acertar sobe (proporcionalmente) a cada degrau do lucro da casa. */
    public static final double PLAYER_BOOST_PER_STEP = 0.015;

    private CoinFlip() {
    }

    /** O pior caso para a casa: o lado escolhido sair. */
    public static int worstLoss(int bet) {
        return (MULTIPLIER - 1) * bet;
    }

    /** Lança a moeda para quem apostou em {@code choice}: devolve {@link #HEADS}, {@link #TAILS} ou {@link #EDGE}. */
    public static int flip(Random random, int choice, long house) {
        int result = toss(random);
        if (random.nextFloat() < Luck.houseLuck(house)) {
            // Sorte da casa: lança de novo e fica com o pior para o jogador.
            int again = toss(random);
            if (result == choice && again != choice) {
                result = again;
            }
        } else if (result != choice && random.nextFloat()
                < Luck.retryChance(house, PLAYER_BOOST_PER_STEP, (1.0 - EDGE_CHANCE) / SIDES)) {
            // Sorte do jogador: errou, mas a moeda é lançada de novo.
            result = toss(random);
        }
        return result;
    }

    private static int toss(Random random) {
        return random.nextFloat() < EDGE_CHANCE ? EDGE : random.nextInt(SIDES);
    }
}
