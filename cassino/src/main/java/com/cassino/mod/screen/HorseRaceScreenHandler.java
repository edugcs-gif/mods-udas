package com.cassino.mod.screen;

import com.cassino.mod.Cassino;
import com.cassino.mod.machine.CassinoState;
import com.cassino.mod.machine.HorseRace;
import com.cassino.mod.machine.SlotMachine;
import java.util.Arrays;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A corrida de cavalos, do lado do servidor (é ele quem corre). As regras estão em {@link HorseRace}; os créditos, o
 * espaço das fichas, DEPÓSITO e RETIRADA, em {@link CassinoScreenHandler}.
 *
 * <p>O jogador escolhe o cavalo clicando na fileira dele e aperta CORRER!. A escalação (nomes, quanto paga cada um e as
 * chances) é a do dia do Minecraft. A aposta e o prêmio entram no saldo da casa, o mesmo das outras máquinas; a
 * corrida só é aceita se o pior caso (o cavalo escolhido vencer) couber no limite de -5000.
 */
public class HorseRaceScreenHandler extends CassinoScreenHandler {
    public static final int BUTTON_RUN = 0;
    public static final int BUTTON_BET_UP = 1;
    public static final int BUTTON_BET_DOWN = 2;
    public static final int BUTTON_DEPOSIT = 3;
    public static final int BUTTON_WITHDRAW = 4;
    /** Escolher o cavalo: {@code BUTTON_CHOOSE + número do cavalo (0 a 6)}. */
    public static final int BUTTON_CHOOSE = 5;

    /** Quanto tempo a tela leva para mostrar a corrida (o aviso de azarão espera isso). */
    public static final int RACE_TICKS = 100;
    /** A partir de quanto um azarão vencedor é avisado no chat para quem está perto (em décimos: 100 = x10). */
    private static final int ANNOUNCE_ODDS_10 = 100;

    // ---------------------------------------------------------------- desenho (em pixels, a partir do canto da tela)

    public static final int WIDTH = 316;
    public static final int HEIGHT = 240;
    public static final int COIN_SLOT_X = 214;
    public static final int COIN_SLOT_Y = 96;
    public static final int INVENTORY_X = 77;
    public static final int INVENTORY_Y = 160;
    public static final int HOTBAR_Y = 218;

    // ---------------------------------------------------------------- propriedades enviadas para a tela

    private static final int P_BET = 0;
    private static final int P_CHOICE = 1;
    private static final int P_DAY_LOW = 2;
    private static final int P_DAY_HIGH = 3;
    private static final int P_WIN_LOW = 4;
    private static final int P_WIN_HIGH = 5;
    private static final int P_MAX_BET = 6;
    private static final int P_RACE_CHOICE = 7;
    private static final int P_NAMES = 8;
    private static final int P_ODDS = P_NAMES + HorseRace.HORSES;
    private static final int P_CHANCE = P_ODDS + HorseRace.HORSES;
    private static final int P_ORDER = P_CHANCE + HorseRace.HORSES;
    /** O contador de corridas vai por último: quando a tela o vê mudar, o resultado novo já chegou. */
    private static final int P_RACES = P_ORDER + HorseRace.HORSES;
    private static final int P_COUNT = P_RACES + 1;

    private int betIndex = 2;
    private int choice;
    private int day;
    private int lastWin;
    private int maxBet;
    /** O cavalo em que o jogador apostou na última corrida. */
    private int raceChoice;
    private final int[] names = new int[HorseRace.HORSES];
    private final int[] odds = new int[HorseRace.HORSES];
    private final int[] chance = new int[HorseRace.HORSES];
    /** A ordem de chegada da última corrida (o índice de cada cavalo, do 1º ao último; -1 = nenhuma ainda). */
    private final int[] order = new int[HorseRace.HORSES];
    private int races;
    /** A escalação de hoje (só no servidor; refeita quando vira o dia). */
    private HorseRace.Lineup lineup;

    /** Do lado do cliente (a tela): o resto chega pelas propriedades. */
    public HorseRaceScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY);
    }

    public HorseRaceScreenHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context) {
        super(Cassino.HORSE_RACE_HANDLER, syncId, playerInventory, context, Cassino.HORSE_RACE, COIN_SLOT_X,
                COIN_SLOT_Y, INVENTORY_X, INVENTORY_Y, HOTBAR_Y);
        Arrays.fill(order, -1);

        addProperties(new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (state != null) {
                    today();
                }
                return switch (index) {
                    case P_BET -> betIndex;
                    case P_CHOICE -> choice;
                    case P_DAY_LOW -> low(day);
                    case P_DAY_HIGH -> high(day);
                    case P_WIN_LOW -> low(lastWin);
                    case P_WIN_HIGH -> high(lastWin);
                    case P_MAX_BET -> state == null ? maxBet : tableMaxBet();
                    case P_RACE_CHOICE -> raceChoice;
                    case P_RACES -> races & 0x7FFF;
                    default -> {
                        if (index < P_ODDS) {
                            yield names[index - P_NAMES];
                        }
                        if (index < P_CHANCE) {
                            yield odds[index - P_ODDS];
                        }
                        if (index < P_ORDER) {
                            yield chance[index - P_CHANCE];
                        }
                        yield order[index - P_ORDER];
                    }
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case P_BET -> betIndex = value;
                    case P_CHOICE -> choice = value;
                    case P_DAY_LOW -> day = withLow(day, value);
                    case P_DAY_HIGH -> day = withHigh(day, value);
                    case P_WIN_LOW -> lastWin = withLow(lastWin, value);
                    case P_WIN_HIGH -> lastWin = withHigh(lastWin, value);
                    case P_MAX_BET -> maxBet = value;
                    case P_RACE_CHOICE -> raceChoice = value;
                    case P_RACES -> races = value;
                    default -> {
                        if (index < P_ODDS) {
                            names[index - P_NAMES] = value;
                        } else if (index < P_CHANCE) {
                            odds[index - P_ODDS] = value;
                        } else if (index < P_ORDER) {
                            chance[index - P_CHANCE] = value;
                        } else {
                            order[index - P_ORDER] = (short) value; // -1 chega como 0xFFFF
                        }
                    }
                }
            }

            @Override
            public int size() {
                return P_COUNT;
            }
        });
    }

    // ---------------------------------------------------------------- o que a tela mostra

    public int bet() {
        return SlotMachine.BETS[Math.floorMod(betIndex, SlotMachine.BETS.length)];
    }

    public int choice() {
        return choice;
    }

    /** O dia do Minecraft (as chances mudam a cada dia). */
    public int day() {
        return day;
    }

    /** Quanto a última corrida devolveu (0 se o cavalo não venceu). */
    public int lastWin() {
        return lastWin;
    }

    /** A maior aposta que a corrida aceita agora no cavalo escolhido (0 = nenhuma), pelo limite da casa. */
    public int maxBet() {
        return maxBet;
    }

    public int raceChoice() {
        return raceChoice;
    }

    /** O nome do cavalo (índice em {@link HorseRace#NAMES}). */
    public int name(int horse) {
        return names[horse];
    }

    /** Quanto o cavalo paga, em décimos (20 = x2,0). */
    public int odds(int horse) {
        return odds[horse];
    }

    /** A chance de o cavalo vencer hoje, em milésimos. */
    public int chance(int horse) {
        return chance[horse];
    }

    public int[] order() {
        return order;
    }

    public int races() {
        return races;
    }

    // ---------------------------------------------------------------- escalação e limite da casa (só no servidor)

    /** A escalação de hoje: refeita (e copiada para a tela) quando vira o dia. */
    private HorseRace.Lineup today() {
        ServerWorld overworld = player.getServer().getOverworld();
        long today = HorseRace.day(overworld.getTimeOfDay());
        if (lineup == null || lineup.day() != today) {
            lineup = HorseRace.lineup(overworld.getSeed(), today);
            day = (int) (today + 1);
            for (int i = 0; i < HorseRace.HORSES; i++) {
                names[i] = lineup.names()[i];
                odds[i] = lineup.odds10()[i];
                chance[i] = lineup.chancePermille(i);
            }
        }
        return lineup;
    }

    private boolean fits(int bet) {
        return state.available() - HorseRace.worstLoss(bet, today().odds10()[choice])
                >= -CassinoState.MAX_HOUSE_DEBT;
    }

    private int tableMaxBet() {
        int best = 0;
        for (int bet : SlotMachine.BETS) {
            if (fits(bet)) {
                best = bet;
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- botões (só no servidor)

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= BUTTON_CHOOSE && id < BUTTON_CHOOSE + HorseRace.HORSES) {
            choice = id - BUTTON_CHOOSE;
            return true;
        }
        switch (id) {
            case BUTTON_RUN -> run();
            case BUTTON_BET_UP -> betIndex = Math.min(betIndex + 1, SlotMachine.BETS.length - 1);
            case BUTTON_BET_DOWN -> betIndex = Math.max(betIndex - 1, 0);
            case BUTTON_DEPOSIT -> deposit();
            case BUTTON_WITHDRAW -> withdraw(player);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void run() {
        int bet = bet();
        if (credits < bet) {
            deposit(); // colocou fichas e já mandou correr: deposita antes
        }
        if (credits < bet || !fits(bet)) {
            return;
        }
        HorseRace.Lineup today = today();
        int odds10 = today.odds10()[choice];
        int[] result = HorseRace.race(player.getRandom(), today, choice, state.house());
        System.arraycopy(result, 0, order, 0, order.length);
        raceChoice = choice;
        credits -= bet;
        boolean won = result[0] == choice;
        lastWin = won ? Math.min(HorseRace.payout(bet, odds10), CassinoState.MAX_BALANCE - credits) : 0;
        credits += lastWin;
        state.settle(bet, lastWin);
        saveCredits();
        races++;

        int paid = lastWin;
        String horse = HorseRace.NAMES[today.names()[choice]];
        context.run((world, pos) -> {
            world.playSound(null, pos, SoundEvents.ENTITY_HORSE_GALLOP, SoundCategory.BLOCKS, 0.6F, 1.0F);
            if (won && odds10 >= ANNOUNCE_ODDS_10 && world instanceof ServerWorld serverWorld) {
                // Azarão! Avisa quem está perto quando a corrida já terminou na tela de quem apostou.
                Cassino.later(serverWorld.getServer(), RACE_TICKS, () -> Cassino.broadcastNearby(world, player,
                        Text.translatable("message.cassino.race.longshot", player.getDisplayName(), paid, horse,
                                odds10 / 10.0).formatted(Formatting.GOLD), 32.0));
            }
        });
    }
}
