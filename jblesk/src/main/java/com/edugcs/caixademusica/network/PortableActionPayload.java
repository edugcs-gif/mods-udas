package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Pacote do cliente para o servidor, para a caixa portátil: as mesmas ações da caixa de música
 * (veja {@link MusicBoxActionPayload}), mas no item que está no espaço {@code slot} do inventário do
 * jogador (0 a 35 = barra e mochila, 40 = mão secundária).
 */
public record PortableActionPayload(int slot, int action, int index, int extra, String url)
        implements CustomPayload {
    public static final CustomPayload.Id<PortableActionPayload> ID =
            new CustomPayload.Id<>(CaixaDeMusica.id("portable_action"));

    public static final PacketCodec<RegistryByteBuf, PortableActionPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, PortableActionPayload::slot,
            PacketCodecs.VAR_INT, PortableActionPayload::action,
            PacketCodecs.VAR_INT, PortableActionPayload::index,
            PacketCodecs.VAR_INT, PortableActionPayload::extra,
            PacketCodecs.string(CaixaDeMusica.MAX_URL_LENGTH), PortableActionPayload::url,
            PortableActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
