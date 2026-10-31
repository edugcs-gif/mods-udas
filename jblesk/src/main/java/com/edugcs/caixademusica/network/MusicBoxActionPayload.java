package com.edugcs.caixademusica.network;

import com.edugcs.caixademusica.CaixaDeMusica;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Pacote do cliente para o servidor: o que o jogador apertou na tela da caixa.
 *
 * <p>{@code index} e {@code extra} dependem da ação: {@link #JUMP} usa o número da música,
 * {@link #SEEK} usa quantos ms avançar (ou voltar, se negativo) e {@link #RANGE} usa
 * {@code index} = perto e {@code extra} = longe, em blocos.
 */
public record MusicBoxActionPayload(BlockPos pos, int action, int index, int extra, String url)
        implements CustomPayload {
    public static final int PLAY = 0;
    public static final int STOP = 1;
    public static final int JUMP = 2;
    public static final int PAUSE = 3;
    public static final int RESUME = 4;
    public static final int SEEK = 5;
    public static final int RANGE = 6;

    public static final CustomPayload.Id<MusicBoxActionPayload> ID = new CustomPayload.Id<>(CaixaDeMusica.id("action"));

    public static final PacketCodec<RegistryByteBuf, MusicBoxActionPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, MusicBoxActionPayload::pos,
            PacketCodecs.VAR_INT, MusicBoxActionPayload::action,
            PacketCodecs.VAR_INT, MusicBoxActionPayload::index,
            PacketCodecs.VAR_INT, MusicBoxActionPayload::extra,
            PacketCodecs.string(CaixaDeMusica.MAX_URL_LENGTH), MusicBoxActionPayload::url,
            MusicBoxActionPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
