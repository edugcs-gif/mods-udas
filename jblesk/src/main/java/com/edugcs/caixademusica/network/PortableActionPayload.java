package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

/**
 * Pacote do cliente para o servidor, para a caixa portátil: as mesmas ações da caixa de música
 * (veja {@link MusicBoxActionPayload}), mas no item que está no espaço {@code slot} do inventário do
 * jogador (0 a 35 = barra e mochila, 40 = mão secundária).
 */
public record PortableActionPayload(int slot, int action, int index, int extra, String url) implements FabricPacket {
    public static final PacketType<PortableActionPayload> TYPE = PacketType.create(CaixaDeMusica.id("portable_action"),
            buf -> new PortableActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readString(CaixaDeMusica.MAX_URL_LENGTH)));

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeVarInt(action);
        buf.writeVarInt(index);
        buf.writeVarInt(extra);
        buf.writeString(url, CaixaDeMusica.MAX_URL_LENGTH);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
