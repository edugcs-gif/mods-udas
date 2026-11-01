package com.cassino.mod.machine;

import java.util.HashMap;
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
 *       servidor, e nunca passa de {@link #MAX_HOUSE_DEBT} negativo (veja {@link SlotMachine}).</li>
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

    private long house;
    private long reserved;
    private final Map<UUID, Integer> balances = new HashMap<>();

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

    /** A casa recebeu {@code bet} e pagou {@code prize}. */
    public void settle(int bet, int prize) {
        house += bet - prize;
        markDirty();
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
        return state;
    }
}
