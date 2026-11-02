package com.jbleskmic.mod.voicechat;

import com.jbleskmic.mod.MicLinks;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * A parte do Simple Voice Chat: o áudio de quem segura um microfone conectado vai para um canal de áudio
 * posicional na caixa de som, e quem estiver perto dela ouve a voz saindo dali.
 *
 * <p>Regras:
 * <ul>
 *   <li>Falando em grupo, ou sussurrando, o microfone não é usado (a voz segue normal);</li>
 *   <li>Falando pelo microfone, a voz normal (proximidade) é cortada: só sai pela caixa, como num
 *       sistema de som (veja {@link #CANCEL_ORIGINAL});</li>
 *   <li>Quem fala não escuta a própria voz saindo da caixa, para não dar eco.</li>
 * </ul>
 */
public class MicVoicechatPlugin implements VoicechatPlugin {
    /** {@code true}: a voz só sai pela caixa. {@code false}: sai pela caixa e também normalmente (proximidade). */
    private static final boolean CANCEL_ORIGINAL = true;

    /** O canal de cada jogador, já apontado para a caixa dele. */
    private record Channel(MicLinks.Link link, LocationalAudioChannel channel) {
    }

    private final Map<UUID, Channel> channels = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "jbleskmic";
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        VoicechatConnection sender = event.getSenderConnection();
        if (sender == null || sender.isInGroup()) {
            return;
        }
        MicrophonePacket packet = event.getPacket();
        if (packet.isWhispering()) {
            return;
        }
        UUID id = sender.getPlayer().getUuid();
        MicLinks.Link link = MicLinks.get(id);
        if (link == null) {
            return;
        }
        LocationalAudioChannel channel = channelFor(event.getVoicechat(), id, link);
        if (channel == null) {
            return;
        }
        channel.send(packet.getOpusEncodedData());
        if (CANCEL_ORIGINAL) {
            event.cancel();
        }
    }

    /** O canal do jogador na caixa; cria de novo se ainda não existe, fechou ou a caixa mudou. */
    private LocationalAudioChannel channelFor(VoicechatServerApi api, UUID player, MicLinks.Link link) {
        Channel current = channels.get(player);
        if (current != null && current.link().equals(link) && !current.channel().isClosed()) {
            return current.channel();
        }
        MinecraftServer server = MicLinks.server();
        ServerWorld world = server == null ? null : server.getWorld(link.dim());
        if (world == null) {
            return null;
        }
        Position position = api.createPosition(link.pos().getX() + 0.5, link.pos().getY() + 0.5,
                link.pos().getZ() + 0.5);
        // Um id só para este canal (não pode ser o do jogador, senão se mistura com a voz normal dele).
        UUID channelId = UUID.nameUUIDFromBytes(("jbleskmic:" + player).getBytes(StandardCharsets.UTF_8));
        LocationalAudioChannel channel = api.createLocationalAudioChannel(channelId, api.fromServerLevel(world),
                position);
        if (channel == null) {
            return null;
        }
        channel.setDistance((float) api.getVoiceChatDistance());
        channel.setFilter(listener -> !listener.getUuid().equals(player));
        channels.put(player, new Channel(link, channel));
        return channel;
    }
}
