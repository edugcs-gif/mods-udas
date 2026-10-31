package com.edugcs.caixademusica;

import com.edugcs.caixademusica.network.PortableSourcesPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Procura as caixas que tocam "carregadas" por uma entidade e avisa os jogadores por perto.
 *
 * <p>São dois tipos, os dois tocando do lugar de quem as carrega:
 * <ul>
 *   <li>a <b>caixa de som portátil</b> (item) guardada no inventário de um jogador (mão, barra ou
 *       mochila): só uma por jogador, a primeira que estiver tocando;</li>
 *   <li>a <b>caixa embutida</b> no baú de um burro, mula, lhama ou barco com baú: ela fica guardada na
 *       própria entidade ({@link CaixaDeMusica#ENTITY_BOX}), sem item nenhum.</li>
 * </ul>
 *
 * <p>A varredura roda a cada meio segundo e logo depois de qualquer botão apertado na tela. Cada
 * jogador só recebe a lista quando ela muda para ele. As caixas embutidas entram na lista mesmo
 * paradas (a tela precisa do link e do alcance guardados); as portáteis só quando estão tocando.
 */
public final class PortableTracker {
    private static final int SCAN_INTERVAL_TICKS = 10;
    /** Mais que o maior alcance da caixa (128 blocos) mais a margem em que o cliente começa a baixar o áudio. */
    private static final double SEND_RANGE = 176.0;

    /** O que cada jogador já recebeu (e em que dimensão), para só mandar quando mudar. */
    private record Sent(RegistryKey<World> world, List<PortableSourcesPayload.Entry> entries) {
    }

    private record Source(Entity owner, PlaybackState state) {
    }

    private static final Map<UUID, Sent> SENT = new HashMap<>();

    private PortableTracker() {
    }

    /** Burro, mula e lhama com baú, e barco com baú: os que têm uma caixa embutida no baú. */
    public static boolean isSpeakerCarrier(Entity entity) {
        return entity instanceof ChestBoatEntity
                || (entity instanceof AbstractDonkeyEntity donkey && donkey.hasChest());
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % SCAN_INTERVAL_TICKS == 0) {
                update(world);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.getPlayer().getUuid()));
    }

    /** Varre este mundo e manda a lista aos jogadores para quem ela mudou. */
    public static void update(ServerWorld world) {
        List<ServerPlayerEntity> players = world.getPlayers();
        if (players.isEmpty()) {
            return; // ninguém aqui para ouvir (o Nether e o End quase sempre): nem varre o mundo
        }
        List<Source> sources = collect(world);
        for (ServerPlayerEntity player : players) {
            List<PortableSourcesPayload.Entry> entries = sources.isEmpty() ? List.of() : entriesFor(player, sources);

            Sent last = SENT.get(player.getUuid());
            boolean sameWorld = last != null && last.world.equals(world.getRegistryKey());
            if (sameWorld && last.entries.equals(entries)) {
                continue;
            }
            if (last == null && entries.isEmpty()) {
                continue; // nada para contar a quem ainda não recebeu nada (e nada a guardar)
            }
            SENT.put(player.getUuid(), new Sent(world.getRegistryKey(), entries));
            ServerPlayNetworking.send(player, new PortableSourcesPayload(entries));
        }
    }

    /** As caixas que este jogador deve ouvir (as que estão ao alcance), em ordem fixa para comparar com o que já recebeu. */
    private static List<PortableSourcesPayload.Entry> entriesFor(ServerPlayerEntity player, List<Source> sources) {
        List<PortableSourcesPayload.Entry> entries = new ArrayList<>();
        for (Source source : sources) {
            if (player.squaredDistanceTo(source.owner) <= SEND_RANGE * SEND_RANGE) {
                entries.add(new PortableSourcesPayload.Entry(source.owner.getId(), source.state));
            }
        }
        if (entries.size() > 1) {
            entries.sort(Comparator.comparingInt(PortableSourcesPayload.Entry::entityId));
        }
        if (entries.size() > PortableSourcesPayload.MAX_ENTRIES) {
            entries = new ArrayList<>(entries.subList(0, PortableSourcesPayload.MAX_ENTRIES));
        }
        return entries;
    }

    private static List<Source> collect(ServerWorld world) {
        List<Source> sources = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            PlaybackState state = findCarried(player.getInventory());
            if (state != null) {
                sources.add(new Source(player, state));
            }
        }
        for (Entity entity : world.iterateEntities()) {
            if (entity instanceof PlayerEntity || entity.isRemoved() || !isSpeakerCarrier(entity)) {
                continue;
            }
            PlaybackState state = entity.getAttached(CaixaDeMusica.ENTITY_BOX);
            if (state != null) {
                sources.add(new Source(entity, state));
            }
        }
        return sources;
    }

    /** A caixa portátil que o jogador carrega: a que está tocando, ou uma pausada se não houver. */
    private static PlaybackState findCarried(Inventory inventory) {
        PlaybackState best = null;
        for (int i = 0; i < inventory.size(); i++) {
            PlaybackState candidate = stateOf(inventory.getStack(i));
            if (candidate == null) {
                continue;
            }
            if (best == null || (best.paused() && !candidate.paused())) {
                best = candidate;
            }
        }
        return best;
    }

    /** O estado da caixa se este item for uma caixa portátil tocando (ou pausada). */
    private static PlaybackState stateOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.isOf(CaixaDeMusica.PORTABLE_ITEM)) {
            return null;
        }
        PlaybackState state = stack.get(CaixaDeMusica.PORTABLE_STATE);
        return state != null && state.playing() && !state.url().isBlank() ? state : null;
    }
}
