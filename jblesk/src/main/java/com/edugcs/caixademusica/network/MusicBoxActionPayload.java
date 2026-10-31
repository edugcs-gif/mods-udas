package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Pacote do cliente para o servidor: o que o jogador apertou na tela da caixa.
 *
 * <p>{@code index} e {@code extra} dependem da ação: {@link #JUMP} usa o número da música,
 * {@link #SEEK} usa quantos ms avançar (ou voltar, se negativo) e {@link #RANGE} usa
 * {@code index} = perto e {@code extra} = longe, em blocos.
 */
public record MusicBoxActionPayload(BlockPos pos, int action, int index, int extra, String url) implements FabricPacket {
    public static final int PLAY = 0;
    public static final int STOP = 1;
    public static final int JUMP = 2;
    public static final int PAUSE = 3;
    public static final int RESUME = 4;
    public static final int SEEK = 5;
    public static final int RANGE = 6;

    public static final PacketType<MusicBoxActionPayload> TYPE = PacketType.create(CaixaDeMusica.id("action"),
            buf -> new MusicBoxActionPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readString(CaixaDeMusica.MAX_URL_LENGTH)));

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
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
