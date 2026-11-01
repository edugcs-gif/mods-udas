package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras do Campo Minado (estilo "Mines") do cassino.
 *
 * <p>Uma grade de {@value #SIZE}×{@value #SIZE} blocos de pedra esconde algumas minas (TNT ou um Creeper); o resto
 * são pedras preciosas (diamante ou esmeralda). O jogador escolhe quantas minas quer ({@value #MIN_MINES} a
 * {@value #MAX_MINES}), aposta e vai cavando: cada pedra preciosa achada aumenta o multiplicador
 * ({@link #multiplier}); achar uma mina explode tudo e a aposta é perdida. Ele pode sacar a qualquer momento depois de
 * achar a primeira pedra (ou desistir antes, recebendo a aposta de volta).
 *
 * <p>O multiplicador é o justo (quanto mais minas e mais pedras achadas, mais difícil chegar ali) vezes
 * {@link #RETURN}: seja qual for a estratégia (poucas minas e muitas pedras, ou muitas minas e uma pedra só), o jogo
 * devolve uns 96% no longo prazo. O prêmio é arredondado para baixo.
 *
 * <p>As minas não são sorteadas no começo: cada bloco cavado vira mina com a chance certa (minas que faltam ÷ blocos
 * que faltam), e no fim as minas que sobraram são espalhadas nos blocos que não foram cavados, para mostrar. Isso deixa
 * a sorte da casa ({@link Luck}) funcionar como nos outros jogos: com a casa no negativo, numa parte das cavadas o
 * bloco é sorteado duas vezes e fica o pior para o jogador; de +640 para cima, a primeira mina de cada rodada pode
 * virar pedra preciosa ({@link #PLAYER_RESCUE_PER_STEP}).
 */
public final class Mines {
    public static final int SIZE = 5;
    public static final int CELLS = SIZE * SIZE;
    public static final int MIN_MINES = 1;
    public static final int MAX_MINES = CELLS - 1;
    public static final int DEFAULT_MINES = 3;
    /** Quanto o jogo devolve, seja qual for a estratégia. */
    public static final double RETURN = 0.96;
    /** A chance (por degrau do lucro da casa) de a primeira mina da rodada virar pedra preciosa. */
    public static final float PLAYER_RESCUE_PER_STEP = 0.03F;

    // O que cada bloco da grade mostra.
    public static final int HIDDEN = 0;
    public static final int EMERALD = 1;
    public static final int DIAMOND = 2;
    public static final int TNT = 3;
    public static final int CREEPER = 4;
    /** Somado ao bloco: ele não foi cavado, só aparece no fim da rodada. */
    public static final int SHOWN = 8;
    /** Somado ao bloco: a mina que explodiu. */
    public static final int EXPLODED = 16;

    /** O resultado de uma cavada: se achou uma mina, e se a sorte do jogador salvou ele de uma. */
    public record Dig(boolean mine, boolean rescued) {
    }

    private Mines() {
    }

    /** O multiplicador depois de achar {@code found} pedras com {@code mines} minas na grade (0 = ainda nenhuma). */
    public static double multiplier(int mines, int found) {
        if (found <= 0) {
            return 0.0;
        }
        double value = RETURN;
        for (int i = 0; i < found; i++) {
            value *= (CELLS - i) / (double) (CELLS - mines - i);
        }
        return value;
    }

    /** Quanto o jogador recebe se sacar agora (antes da primeira pedra, desistir devolve a aposta). */
    public static long payout(int bet, int mines, int found) {
        if (found <= 0) {
            return bet;
        }
        return (long) Math.floor(bet * multiplier(mines, found) + 1e-9);
    }

    /** A chance de o próximo bloco ser uma pedra preciosa (com a grade limpa). */
    public static double safeChance(int mines, int found) {
        int hidden = CELLS - found;
        return hidden <= 0 ? 0.0 : (hidden - mines) / (double) hidden;
    }

    /**
     * Cava um bloco, com {@code hidden} blocos ainda cobertos e {@code mines} minas entre eles. {@code rescue} diz se a
     * sorte do jogador ainda pode salvá-lo de uma mina nesta rodada.
     */
    public static Dig dig(Random random, int hidden, int mines, long house, boolean rescue) {
        boolean mine = random.nextInt(hidden) < mines;
        if (random.nextFloat() < Luck.houseLuck(house)) {
            // Sorte da casa: sorteia de novo e fica com o pior para o jogador.
            mine = mine || random.nextInt(hidden) < mines;
            return new Dig(mine, false);
        }
        if (mine && rescue && random.nextFloat() < PLAYER_RESCUE_PER_STEP * Luck.profitStep(house)) {
            // Sorte do jogador: era mina, mas o bloco é sorteado de novo.
            mine = random.nextInt(hidden) < mines;
            return new Dig(mine, true);
        }
        return new Dig(mine, false);
    }
}
