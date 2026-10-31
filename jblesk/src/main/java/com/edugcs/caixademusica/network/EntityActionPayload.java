package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Pacote do cliente para o servidor, para a caixa embutida no baú de um burro, mula, lhama ou barco:
 * as mesmas ações da caixa de música (veja {@link MusicBoxActionPayload}), na entidade {@code entityId}.
 */
public record EntityActionPayload(int entityId, int action, int index, int extra, String url)
        implements CustomPayload {
    public static final CustomPayload.Id<EntityActionPayload> ID =
            new CustomPayload.Id<>(CaixaDeMusica.id("entity_action"));

    public static final PacketCodec<RegistryByteBuf, EntityActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, EntityActionPayload::entityId,
            PacketCodecs.VAR_INT, EntityActionPayload::action,
            PacketCodecs.VAR_INT, EntityActionPayload::index,
            PacketCodecs.VAR_INT, EntityActionPayload::extra,
            PacketCodecs.string(CaixaDeMusica.MAX_URL_LENGTH), EntityActionPayload::url,
            EntityActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
