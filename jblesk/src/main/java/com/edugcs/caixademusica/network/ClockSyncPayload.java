package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

/**
 * Pacote do servidor para o cliente, enviado ao entrar: o relógio do servidor (ms).
 *
 * <p>A caixa portátil guarda o horário de início no item, em tempo do servidor. Com isto o
 * cliente descobre a diferença entre o relógio dele e o do servidor e entra na música no
 * ponto certo, mesmo com os relógios dos PCs desencontrados.
 */
public record ClockSyncPayload(long serverMillis) implements FabricPacket {
    public static final PacketType<ClockSyncPayload> TYPE =
            PacketType.create(CaixaDeMusica.id("clock_sync"), buf -> new ClockSyncPayload(buf.readVarLong()));

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarLong(serverMillis);
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
