package com.jbleskmic.mod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Quem está falando por uma caixa agora.
 *
 * <p>O Simple Voice Chat entrega o áudio do microfone numa thread de rede, onde não dá para mexer no mundo
 * com segurança. Então, a cada meio segundo, a thread principal confere quem está segurando um microfone
 * conectado a uma caixa que ainda existe e guarda o resultado aqui; a thread de rede só consulta este mapa.
 */
public final class MicLinks {
    /** A caixa (bloco) a que um microfone está conectado. */
    public record Link(RegistryKey<World> dim, BlockPos pos) {
    }

    private static final Map<UUID, Link> ACTIVE = new ConcurrentHashMap<>();
    private static volatile MinecraftServer server;

    private MicLinks() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            ACTIVE.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(MicLinks::tick);
    }

    /** A caixa por onde o jogador está falando agora, ou {@code null} (pode ser chamado de qualquer thread). */
    public static Link get(UUID player) {
        return ACTIVE.get(player);
    }

    public static MinecraftServer server() {
        return server;
    }

    /** O bloco jbleskão do mod jblesk, ou {@code null} se o mod não estiver carregado. */
    public static Block speakerBlock() {
        Identifier id = new Identifier("caixademusica", "music_box");
        return Registries.BLOCK.containsId(id) ? Registries.BLOCK.get(id) : null;
    }

    private static void tick(MinecraftServer s) {
        if (s.getTicks() % 10 != 0) {
            return;
        }
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : s.getPlayerManager().getPlayerList()) {
            online.add(player.getUuid());
            Link link = activeLink(s, player);
            if (link != null) {
                ACTIVE.put(player.getUuid(), link);
            } else {
                ACTIVE.remove(player.getUuid());
            }
        }
        ACTIVE.keySet().retainAll(online);
    }

    /** A caixa do primeiro microfone conectado em uma das mãos, se ela existir e estiver carregada. */
    private static Link activeLink(MinecraftServer s, ServerPlayerEntity player) {
        Block speaker = speakerBlock();
        if (speaker == null) {
            return null;
        }
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(JbleskMic.MICROPHONE)) {
                continue;
            }
            Link link = MicrophoneItem.readLink(stack);
            if (link == null) {
                continue;
            }
            ServerWorld world = s.getWorld(link.dim());
            if (world == null) {
                continue;
            }
            BlockPos pos = link.pos();
            if (!world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                continue;
            }
            if (world.getBlockState(pos).isOf(speaker)) {
                return link;
            }
        }
        return null;
    }
}
