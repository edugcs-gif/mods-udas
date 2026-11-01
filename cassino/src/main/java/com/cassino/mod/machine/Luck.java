package com.cassino.mod.machine;

/**
 * A sorte que segue o saldo da casa ({@link CassinoState}), igual em todos os jogos do cassino.
 *
 * <ul>
 *   <li><b>Casa no negativo:</b> a cada 64 fichas no negativo, a casa ganha mais sorte ({@link #houseLuck}: 5% a cada
 *       64, até 50% em -640). Os jogos sorteiam duas vezes e ficam com o pior para o jogador.</li>
 *   <li><b>Casa de 0 a +639:</b> jogo normal.</li>
 *   <li><b>Casa em +640 ou mais:</b> a cada 64 fichas sobe um degrau ({@link #profitStep}: 1 de +640 a +703, 2 de
 *       +704 a +767... até {@link #MAX_STEPS} de +1216 para cima), e a chance de o jogador ganhar sobe em todos os
 *       jogos. No último degrau cada jogo devolve uns 110% do que é apostado: os jogadores ganham mais do que perdem,
 *       e a casa volta para perto de +640 em vez de acumular fichas.</li>
 * </ul>
 */
public final class Luck {
    /** Fichas de saldo da casa por degrau, no negativo e no positivo. */
    public static final int STEP = 64;
    /** Saldo da casa a partir do qual a chance dos jogadores começa a subir. */
    public static final int PROFIT_START = 640;
    /** Quantos degraus há de cada lado. */
    public static final int MAX_STEPS = 10;

    private Luck() {
    }

    /** Com a casa no negativo: a chance de um sorteio sair duas vezes e ficar o pior para o jogador. */
    public static float houseLuck(long house) {
        return house >= 0 ? 0.0F : 0.05F * Math.min(MAX_STEPS, -house / STEP);
    }

    /** O degrau do lucro da casa: 0 abaixo de +640, 1 de +640 a +703, 2 de +704 a +767... até 10 de +1216 para cima. */
    public static int profitStep(long house) {
        return house < PROFIT_START ? 0 : (int) Math.min(MAX_STEPS, 1 + (house - PROFIT_START) / STEP);
    }

    /**
     * Com a casa no positivo: a chance de uma derrota ser sorteada de novo, para que a chance de ganhar
     * {@code chance} suba {@code boostPerStep} dela a cada degrau (0,025 = um quadragésimo: 40% vira 41%, 42%...).
     *
     * <p>Sorteando de novo uma parte {@code r} das derrotas, a chance vira {@code p + r(1-p)p = p(1 + r(1-p))}; para
     * ela ser {@code p(1 + b)}, {@code r = b / (1-p)}.
     */
    public static float retryChance(long house, double boostPerStep, double chance) {
        int step = profitStep(house);
        return step == 0 ? 0.0F : (float) Math.min(1.0, step * boostPerStep / (1.0 - chance));
    }
}
