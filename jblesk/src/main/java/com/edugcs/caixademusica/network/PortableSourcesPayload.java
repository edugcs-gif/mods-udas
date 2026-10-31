package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.edugcs.caixademusica.PlaybackState;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;

/**
 * Pacote do servidor para o cliente: quais caixas portáteis estão tocando por perto.
 *
 * <p>A caixa portátil toca de onde estiver guardada (inventário do jogador, baú de burro/mula/lhama,
 * barco ou carrinho com baú), então os outros jogadores não "enxergam" o item. O servidor varre os
 * inventários e manda esta lista: para cada dono (jogador, animal ou veículo), o estado da caixa dele.
 * Só é enviado quando a lista muda.
 */
public record PortableSourcesPayload(List<Entry> entries) implements FabricPacket {
    public static final int MAX_ENTRIES = 32;

    /** Uma caixa tocando: {@code entityId} é a entidade que a carrega (o som sai dela). */
    public record Entry(int entityId, PlaybackState state) {
    }

    public static final PacketType<PortableSourcesPayload> TYPE =
            PacketType.create(CaixaDeMusica.id("portable_sources"), PortableSourcesPayload::read);

    private static PortableSourcesPayload read(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new DecoderException("Caixas demais na lista: " + count);
        }
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(buf.readVarInt(), PlaybackState.read(buf)));
        }
        return new PortableSourcesPayload(entries);
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeVarInt(entry.entityId());
            entry.state().write(buf);
        }
    }

    @Override
    public PacketType<?> getType() {
        return TYPE;
    }
}
