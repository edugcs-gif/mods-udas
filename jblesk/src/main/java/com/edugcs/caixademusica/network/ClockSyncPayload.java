package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Pacote do servidor para o cliente, enviado ao entrar: o relógio do servidor (ms).
 *
 * <p>A caixa portátil guarda o horário de início no item, em tempo do servidor. Com isto o
 * cliente descobre a diferença entre o relógio dele e o do servidor e entra na música no
 * ponto certo, mesmo com os relógios dos PCs desencontrados.
 */
public record ClockSyncPayload(long serverMillis) implements CustomPayload {
    public static final CustomPayload.Id<ClockSyncPayload> ID = new CustomPayload.Id<>(CaixaDeMusica.id("clock_sync"));

    public static final PacketCodec<RegistryByteBuf, ClockSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_LONG, ClockSyncPayload::serverMillis,
            ClockSyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
