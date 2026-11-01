package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do Plinko de Slime do cassino.
 *
 * <p>A bola de slime cai do funil e quica em {@link #ROWS} fileiras de pinos; em cada uma ela vai para a esquerda ou
 * para a direita, meio a meio. O caminho é um número de {@link #ROWS} bits (1 = direita), e o buraco onde ela cai é
 * quantas vezes ela foi para a direita (de 0 a {@value #ROWS}). Os buracos do meio são os mais fáceis e os das pontas
 * os mais difíceis (1 em 64 cada):
 *
 * <pre>
 *   buraco:     0     1     2     3     4     5     6
 *   paga:      10x    2x  0,5x  0,2x  0,5x    2x   10x
 *   chance:    1,6%  9,4% 23,4% 31,2% 23,4%  9,4%  1,6%
 * </pre>
 *
 * <p>No total o Plinko devolve uns 98,4% do que é apostado. O prêmio é a aposta vezes o multiplicador; quando não dá
 * um número inteiro de fichas, a parte quebrada vira a chance de 1 ficha a mais (0,5x de 1 ficha: metade das vezes
 * volta 1), então a conta vale igual para qualquer aposta.
 *
 * <p>A sorte segue o saldo da casa, como em todos os jogos ({@link Luck}). Com a casa no negativo, numa parte das
 * jogadas ({@link Luck#houseLuck}) a bola cai duas vezes e fica o pior buraco para o jogador. De 0 a +639 o Plinko é
 * limpo. De +640 para cima, numa parte das quedas nos buracos que pagam menos que a aposta a bola cai de novo
 * ({@link #PLAYER_BOOST_PER_STEP}), e no último degrau, em +1216, o jogo devolve uns 110%.
 */
public final class Plinko {
    public static final int ROWS = 6;
    public static final int BUCKETS = ROWS + 1;

    /** Quanto cada buraco paga, em décimos (100 = x10). */
    private static final int[] MULTIPLIER_10 = {100, 20, 5, 2, 5, 20, 100};
    /** A chance de cair num buraco que paga mais que a aposta (os de 2x e 10x): 14 caminhos em 64. */
    private static final double WIN_CHANCE = 14.0 / 64.0;
    /** Quanto a chance de cair num buraco bom sobe (proporcionalmente) a cada degrau do lucro da casa. */
    public static final double PLAYER_BOOST_PER_STEP = 0.019;

    private Plinko() {
    }

    /** Quanto o buraco paga, em décimos. */
    public static int multiplier10(int bucket) {
        return MULTIPLIER_10[Math.max(0, Math.min(BUCKETS - 1, bucket))];
    }

    /** O buraco onde a bola cai com este caminho: quantas vezes ela foi para a direita. */
    public static int bucket(int path) {
        return Integer.bitCount(path & ((1 << ROWS) - 1));
    }

    /** O pior caso para a casa: a bola cair numa ponta (10x). */
    public static int worstLoss(int bet) {
        return bet * MULTIPLIER_10[0] / 10 - bet;
    }

    /** Deixa a bola cair: devolve o caminho ({@value #ROWS} bits, 1 = direita). */
    public static int drop(Random random, long house) {
        int path = randomPath(random);
        if (random.nextFloat() < Luck.houseLuck(house)) {
            // Sorte da casa: a bola cai de novo e fica o pior buraco para o jogador.
            int again = randomPath(random);
            if (multiplier10(bucket(again)) < multiplier10(bucket(path))) {
                path = again;
            }
        } else if (multiplier10(bucket(path)) < 10
                && random.nextFloat() < Luck.retryChance(house, PLAYER_BOOST_PER_STEP, WIN_CHANCE)) {
            // Sorte do jogador: caiu num buraco que paga menos que a aposta, mas a bola cai de novo.
            path = randomPath(random);
        }
        return path;
    }

    private static int randomPath(Random random) {
        return random.nextInt(1 << ROWS);
    }

    /** O prêmio: a aposta vezes o multiplicador; a parte quebrada vira a chance de 1 ficha a mais. */
    public static int prize(Random random, int bet, int bucket) {
        int tenths = bet * multiplier10(bucket);
        int prize = tenths / 10;
        if (random.nextInt(10) < tenths % 10) {
            prize++;
        }
        return prize;
    }
}
