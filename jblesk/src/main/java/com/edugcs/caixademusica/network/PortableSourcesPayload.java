package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.edugcs.caixademusica.PlaybackState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Pacote do servidor para o cliente: quais caixas portáteis estão tocando por perto.
 *
 * <p>A caixa portátil toca de onde estiver guardada (inventário do jogador, baú de burro/mula/lhama,
 * barco ou carrinho com baú), então os outros jogadores não "enxergam" o item. O servidor varre os
 * inventários e manda esta lista: para cada dono (jogador, animal ou veículo), o estado da caixa dele.
 * Só é enviado quando a lista muda.
 */
public record PortableSourcesPayload(List<Entry> entries) implements CustomPayload {
    public static final int MAX_ENTRIES = 32;

    /** Uma caixa tocando: {@code entityId} é a entidade que a carrega (o som sai dela). */
    public record Entry(int entityId, PlaybackState state) {
        public static final PacketCodec<RegistryByteBuf, Entry> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, Entry::entityId,
                PlaybackState.PACKET_CODEC, Entry::state,
                Entry::new);
    }

    public static final CustomPayload.Id<PortableSourcesPayload> ID =
            new CustomPayload.Id<>(CaixaDeMusica.id("portable_sources"));

    public static final PacketCodec<RegistryByteBuf, PortableSourcesPayload> CODEC =
            PacketCodecs.collection(ArrayList<Entry>::new, Entry.CODEC, MAX_ENTRIES)
                    .xmap(PortableSourcesPayload::new, payload -> new ArrayList<>(payload.entries()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
