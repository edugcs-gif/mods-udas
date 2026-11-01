package com.cassino.mod.machine;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/**
 * O "cofre" do cassino, salvo junto com o mundo ({@code data/cassino.dat}):
 * <ul>
 *   <li>o <b>saldo de cada jogador</b> (as fichas que ele depositou nas máquinas, mais os prêmios, menos as
 *       apostas): é só dele, vale em qualquer caça-níquel e continua lá depois de fechar a máquina;</li>
 *   <li>o <b>saldo da casa</b>, o mesmo para o caça-níquel e o blackjack: começa em 0, sobe com cada aposta e
 *       desce com cada prêmio. Ele mede quantas fichas o cassino já criou (negativo) ou destruiu (positivo) no
 *       servidor, e nunca passa de {@link #MAX_HOUSE_DEBT} negativo (veja {@link SlotMachine}). Também nunca passa de
 *       {@link #LOTTERY_HOUSE_CAP}: o que passar vai para o prêmio da loteria;</li>
 *   <li>a <b>loteria de mobs</b> ({@link Lottery}), com o saldo separado: o prêmio acumulado, as apostas do dia, o
 *       último mob sorteado e os avisos de prêmio para quem ganhou sem estar no jogo.</li>
 * </ul>
 *
 * <p>Uma mão de blackjack dura vários cliques: enquanto ela não termina, a mesa <b>reserva</b> o pior caso para a
 * casa (o quanto ela perderia se o jogador ganhasse tudo). O limite é conferido sobre o saldo menos o reservado
 * ({@link #available}), para várias mesas e máquinas ao mesmo tempo nunca passarem dele juntas. A reserva não é
 * salva: some se o servidor fechar (as mãos em andamento também).
 */
public class CassinoState extends PersistentState {
    /** O máximo de fichas que o cassino pode ter dado a mais do que recebeu, no servidor todo. */
    public static final int MAX_HOUSE_DEBT = 640;
    /** Máximo de créditos de um jogador. */
    public static final int MAX_BALANCE = 1_000_000;
    /** O saldo da casa não passa disso: o que passar vai para o prêmio da loteria. */
    public static final int LOTTERY_HOUSE_CAP = 800;

    private long house;
    private long reserved;
    private final Map<UUID, Integer> balances = new HashMap<>();

    /** Uma aposta na loteria: o mob (índice em {@link Lottery#MOBS}) e o nome do jogador (para o anúncio). */
    public record LotteryBet(int mob, String name) {
    }

    private long lotteryPot;
    /** O dia do Minecraft das apostas de agora (-1 = nenhuma aposta). */
    private long lotteryDay = -1;
    private final Map<UUID, LotteryBet> lotteryBets = new LinkedHashMap<>();
    private int lastLotteryMob = -1;
    /** Prêmios de quem ganhou sem estar no jogo: o aviso aparece quando ele entrar. */
    private final Map<UUID, Long> lotteryNotices = new HashMap<>();

    public static CassinoState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(CassinoState::fromNbt, CassinoState::new, "cassino");
    }

    public long house() {
        return house;
    }

    /** O saldo da casa menos o que está reservado para mãos de blackjack em andamento. */
    public long available() {
        return house - reserved;
    }

    /** Reserva o pior caso de uma mão de blackjack que começou (ou dobrou). */
    public void reserve(long amount) {
        reserved += amount;
    }

    /** Libera a reserva de uma mão que terminou (o resultado de verdade entra pelo {@link #settle}). */
    public void release(long amount) {
        reserved = Math.max(0L, reserved - amount);
    }

    /** A casa recebeu {@code bet} e pagou {@code prize}. O excesso acima do teto vai para a loteria. */
    public void settle(int bet, int prize) {
        house += bet - prize;
        capHouse();
        markDirty();
    }

    private void capHouse() {
        if (house > LOTTERY_HOUSE_CAP) {
            lotteryPot += house - LOTTERY_HOUSE_CAP;
            house = LOTTERY_HOUSE_CAP;
        }
    }

    // ---------------------------------------------------------------- loteria de mobs

    public long lotteryPot() {
        return lotteryPot;
    }

    public void addToLotteryPot(long amount) {
        lotteryPot = Math.max(0L, lotteryPot + amount);
        markDirty();
    }

    public long lotteryDay() {
        return lotteryDay;
    }

    public void setLotteryDay(long day) {
        lotteryDay = day;
        markDirty();
    }

    public Map<UUID, LotteryBet> lotteryBets() {
        return Collections.unmodifiableMap(lotteryBets);
    }

    /** O mob em que o jogador apostou hoje, ou -1. */
    public int lotteryBet(UUID player) {
        LotteryBet bet = lotteryBets.get(player);
        return bet == null ? -1 : bet.mob();
    }

    public void placeLotteryBet(UUID player, int mob, String name) {
        lotteryBets.put(player, new LotteryBet(mob, name));
        markDirty();
    }

    public void clearLotteryBets() {
        lotteryBets.clear();
        markDirty();
    }

    public int lastLotteryMob() {
        return lastLotteryMob;
    }

    public void setLastLotteryMob(int mob) {
        lastLotteryMob = mob;
        markDirty();
    }

    public void addLotteryNotice(UUID player, long amount) {
        lotteryNotices.merge(player, amount, Long::sum);
        markDirty();
    }

    /** O prêmio que o jogador ganhou sem estar no jogo (e que ainda não foi avisado), ou {@code null}. */
    public Long takeLotteryNotice(UUID player) {
        Long amount = lotteryNotices.remove(player);
        if (amount != null) {
            markDirty();
        }
        return amount;
    }

    public int balance(UUID player) {
        return balances.getOrDefault(player, 0);
    }

    public void setBalance(UUID player, int credits) {
        int value = Math.max(0, Math.min(MAX_BALANCE, credits));
        if (value == 0) {
            balances.remove(player);
        } else {
            balances.put(player, value);
        }
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("House", house);
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Integer> entry : balances.entrySet()) {
            NbtCompound player = new NbtCompound();
            player.putUuid("Player", entry.getKey());
            player.putInt("Credits", entry.getValue());
            list.add(player);
        }
        nbt.put("Balances", list);

        NbtCompound lottery = new NbtCompound();
        lottery.putLong("Pot", lotteryPot);
        lottery.putLong("Day", lotteryDay);
        lottery.putInt("LastMob", lastLotteryMob);
        NbtList bets = new NbtList();
        for (Map.Entry<UUID, LotteryBet> entry : lotteryBets.entrySet()) {
            NbtCompound bet = new NbtCompound();
            bet.putUuid("Player", entry.getKey());
            bet.putInt("Mob", entry.getValue().mob());
            bet.putString("Name", entry.getValue().name());
            bets.add(bet);
        }
        lottery.put("Bets", bets);
        NbtList notices = new NbtList();
        for (Map.Entry<UUID, Long> entry : lotteryNotices.entrySet()) {
            NbtCompound notice = new NbtCompound();
            notice.putUuid("Player", entry.getKey());
            notice.putLong("Amount", entry.getValue());
            notices.add(notice);
        }
        lottery.put("Notices", notices);
        nbt.put("Lottery", lottery);
        return nbt;
    }

    private static CassinoState fromNbt(NbtCompound nbt) {
        CassinoState state = new CassinoState();
        state.house = nbt.getLong("House");
        NbtList list = nbt.getList("Balances", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound player = list.getCompound(i);
            if (player.containsUuid("Player")) {
                state.balances.put(player.getUuid("Player"), player.getInt("Credits"));
            }
        }

        NbtCompound lottery = nbt.getCompound("Lottery");
        state.lotteryPot = lottery.getLong("Pot");
        state.lotteryDay = lottery.contains("Day") ? lottery.getLong("Day") : -1;
        state.lastLotteryMob = lottery.contains("LastMob") ? lottery.getInt("LastMob") : -1;
        NbtList bets = lottery.getList("Bets", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < bets.size(); i++) {
            NbtCompound bet = bets.getCompound(i);
            if (bet.containsUuid("Player")) {
                state.lotteryBets.put(bet.getUuid("Player"), new LotteryBet(bet.getInt("Mob"), bet.getString("Name")));
            }
        }
        NbtList notices = lottery.getList("Notices", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < notices.size(); i++) {
            NbtCompound notice = notices.getCompound(i);
            if (notice.containsUuid("Player")) {
                state.lotteryNotices.put(notice.getUuid("Player"), notice.getLong("Amount"));
            }
        }
        state.capHouse();
        return state;
    }
}
