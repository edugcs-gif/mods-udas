package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

/**
 * Pacote do cliente para o servidor, para a caixa embutida no baú de um burro, mula, lhama ou barco:
 * as mesmas ações da caixa de música (veja {@link MusicBoxActionPayload}), na entidade {@code entityId}.
 */
public record EntityActionPayload(int entityId, int action, int index, int extra, String url) implements FabricPacket {
    public static final PacketType<EntityActionPayload> TYPE = PacketType.create(CaixaDeMusica.id("entity_action"),
            buf -> new EntityActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readString(CaixaDeMusica.MAX_URL_LENGTH)));

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(entityId);
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
