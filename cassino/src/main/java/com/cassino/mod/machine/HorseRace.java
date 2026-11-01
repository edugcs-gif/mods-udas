package com.cassino.mod.machine;

import net.minecraft.util.math.random.Random;

/**
 * As regras da corrida de cavalos do cassino.
 *
 * <p>Correm {@link #HORSES} cavalos. A cada dia do Minecraft (cada 24.000 ticks do horário do mundo) muda tudo: quais
 * nomes correm e a força de cada cavalo, sorteadas a partir da semente do mundo e do número do dia, então todos os
 * jogadores do servidor veem a mesma escalação, e ela muda quando vira o dia (também com {@code /time set}).
 *
 * <ul>
 *   <li>A força decide a chance de vencer ({@link Lineup#chance}); o mais forte tem no máximo 50%.</li>
 *   <li>Quanto paga cada cavalo ({@link Lineup#odds10}, em décimos: 20 = x2,0) sai da chance: uns 90% do que é
 *       apostado nele volta no longo prazo ({@link #RETURN}), arredondando para baixo de 0,5 em 0,5; do x1,5 ao x30.</li>
 *   <li>Na corrida, o vencedor é sorteado pelas chances do dia, e os outros chegam em ordem sorteada pelas forças.</li>
 * </ul>
 *
 * <p>A sorte segue o saldo da casa, como em todos os jogos ({@link Luck}). Com a casa no negativo, numa parte das
 * corridas ({@link Luck#houseLuck}) o vencedor é sorteado duas vezes e fica o pior para o jogador. De 0 a +639 a
 * corrida é limpa. De +640 para cima, a chance do cavalo escolhido sobe {@link #PLAYER_BOOST_PER_STEP} dela a cada
 * degrau (numa parte das derrotas o vencedor é sorteado de novo): no último degrau, em +1216, ela é 1,25 vez a do dia
 * (20% vira 25%), e cada cavalo devolve uns 110%.
 */
public final class HorseRace {
    public static final int HORSES = 7;
    /** Quanto do que é apostado num cavalo volta, em média (antes do arredondamento, que só diminui). */
    public static final double RETURN = 0.90;
    public static final int MIN_ODDS_10 = 15;
    public static final int MAX_ODDS_10 = 300;
    private static final int TICKS_PER_DAY = 24_000;
    /** Quanto a chance do cavalo escolhido sobe (proporcionalmente) a cada degrau do lucro da casa. */
    public static final double PLAYER_BOOST_PER_STEP = 0.025;

    /** Os nomes que podem correr (7 por dia). */
    public static final String[] NAMES = {
            "Rapidinha", "Cruz Credo", "Mimimi", "Arrogante", "Pinga Fogo", "Velho Ranzinza", "Pé de Valsa",
            "Ventania", "Trovão Manco", "Bigodudo", "Pangaré Veloz", "Sossegado", "Furacão de Botas", "Tô Atrasado",
            "Pé Quente", "Chá de Cadeira"
    };

    /**
     * A escalação de um dia: para cada um dos {@link #HORSES} cavalos, o nome (índice em {@link #NAMES}), quanto paga
     * (em décimos) e a chance de vencer.
     */
    public record Lineup(long day, int[] names, int[] odds10, double[] chance) {
        /** A chance em milésimos (para a tela mostrar). */
        public int chancePermille(int horse) {
            return (int) Math.round(chance[horse] * 1000);
        }
    }

    private HorseRace() {
    }

    /** O dia do Minecraft deste horário do mundo. */
    public static long day(long timeOfDay) {
        return Math.floorDiv(timeOfDay, TICKS_PER_DAY);
    }

    /** A escalação do dia {@code day} num mundo com esta semente. */
    public static Lineup lineup(long worldSeed, long day) {
        java.util.Random random = new java.util.Random(worldSeed ^ (day * 0x9E3779B97F4A7C15L) ^ 0x5DEECE66DL);

        int[] pool = new int[NAMES.length];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = i;
        }
        for (int i = pool.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int swap = pool[i];
            pool[i] = pool[j];
            pool[j] = swap;
        }
        int[] names = new int[HORSES];
        System.arraycopy(pool, 0, names, 0, HORSES);

        // Força de 0,5 a 3,0, puxada para baixo: poucos favoritos e vários azarões.
        double[] strength = new double[HORSES];
        double total = 0;
        for (int i = 0; i < HORSES; i++) {
            double u = random.nextDouble();
            strength[i] = 0.5 + 2.5 * u * u;
            total += strength[i];
        }
        double[] chance = new double[HORSES];
        int[] odds10 = new int[HORSES];
        for (int i = 0; i < HORSES; i++) {
            chance[i] = strength[i] / total;
            int odds = (int) Math.floor(RETURN / chance[i] * 2) * 5;
            odds10[i] = Math.max(MIN_ODDS_10, Math.min(MAX_ODDS_10, odds));
        }
        return new Lineup(day, names, odds10, chance);
    }

    /** O que a aposta devolve se o cavalo vencer. */
    public static int payout(int bet, int odds10) {
        return bet * odds10 / 10;
    }

    /** O pior caso para a casa: o cavalo escolhido vencer. */
    public static int worstLoss(int bet, int odds10) {
        return payout(bet, odds10) - bet;
    }

    /**
     * Corre: devolve a ordem de chegada (o índice de cada cavalo, do 1º ao último) para quem apostou no cavalo
     * {@code choice}.
     */
    public static int[] race(Random random, Lineup lineup, int choice, long house) {
        double[] chance = lineup.chance();
        int winner = pick(random, chance, new boolean[HORSES]);
        if (random.nextFloat() < Luck.houseLuck(house)) {
            // Sorte da casa: sorteia de novo e fica com o pior para o jogador.
            int again = pick(random, chance, new boolean[HORSES]);
            if (winner == choice && again != choice) {
                winner = again;
            }
        } else if (winner != choice
                && random.nextFloat() < Luck.retryChance(house, PLAYER_BOOST_PER_STEP, chance[choice])) {
            // Sorte do jogador: o cavalo dele perdeu, mas o vencedor é sorteado de novo.
            winner = pick(random, chance, new boolean[HORSES]);
        }
        int[] order = new int[HORSES];
        boolean[] done = new boolean[HORSES];
        order[0] = winner;
        done[winner] = true;
        for (int place = 1; place < HORSES; place++) {
            order[place] = pick(random, chance, done);
            done[order[place]] = true;
        }
        return order;
    }

    /** Sorteia um cavalo pelas chances, entre os que ainda não chegaram. */
    private static int pick(Random random, double[] chance, boolean[] done) {
        double total = 0;
        for (int i = 0; i < HORSES; i++) {
            if (!done[i]) {
                total += chance[i];
            }
        }
        double roll = random.nextDouble() * total;
        int last = -1;
        for (int i = 0; i < HORSES; i++) {
            if (done[i]) {
                continue;
            }
            last = i;
            roll -= chance[i];
            if (roll < 0) {
                return i;
            }
        }
        return last;
    }
}
