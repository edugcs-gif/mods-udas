package com.edugcs.caixademusica;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

/**
 * O que uma caixa (bloco ou portátil) está tocando. É o estado inteiro, igual nos dois casos.
 *
 * <p>O servidor só guarda o link, a música inicial da playlist e o horário (do servidor, em ms)
 * em que começou; cada jogador baixa e toca o áudio no próprio PC. Pausar, continuar e pular
 * para um ponto da música são só mudanças nesses números:
 * <ul>
 *   <li>pausar guarda quanto tempo já tinha passado ({@code pausedElapsed});</li>
 *   <li>continuar recalcula o horário de início para que esse tempo seja retomado;</li>
 *   <li>pular desloca o horário de início para frente ou para trás.</li>
 * </ul>
 *
 * @param near até esta distância (blocos) o volume é total
 * @param far  a partir desta distância (blocos) não se ouve mais; entre as duas o volume cai
 */
public record PlaybackState(String url, boolean playing, boolean paused, int startIndex, long startedAt,
                            long pausedElapsed, int near, int far) {
    public static final int MAX_INDEX = 1_000_000;
    public static final int MIN_FAR = 4;
    public static final int MAX_FAR = 128;
    private static final long MAX_SEEK_MS = TimeUnit.HOURS.toMillis(12);

    public static final PlaybackState BLOCK_DEFAULT = new PlaybackState("", false, false, 0, 0L, 0L, 8, 64);
    public static final PlaybackState PORTABLE_DEFAULT = new PlaybackState("", false, false, 0, 0L, 0L, 4, 32);
    /** A caixa embutida no baú de um burro, mula, lhama ou barco: alcance entre o do portátil e o do bloco. */
    public static final PlaybackState ENTITY_DEFAULT = new PlaybackState("", false, false, 0, 0L, 0L, 6, 48);

    public static final Codec<PlaybackState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("url", "").forGetter(PlaybackState::url),
            Codec.BOOL.optionalFieldOf("playing", false).forGetter(PlaybackState::playing),
            Codec.BOOL.optionalFieldOf("paused", false).forGetter(PlaybackState::paused),
            Codec.INT.optionalFieldOf("start_index", 0).forGetter(PlaybackState::startIndex),
            Codec.LONG.optionalFieldOf("started_at", 0L).forGetter(PlaybackState::startedAt),
            Codec.LONG.optionalFieldOf("paused_elapsed", 0L).forGetter(PlaybackState::pausedElapsed),
            Codec.INT.optionalFieldOf("near", PORTABLE_DEFAULT.near()).forGetter(PlaybackState::near),
            Codec.INT.optionalFieldOf("far", PORTABLE_DEFAULT.far()).forGetter(PlaybackState::far)
    ).apply(instance, PlaybackState::new));

    /** Vai pela rede em 6 campos (o limite do PacketCodec.tuple): os booleanos e o alcance viajam juntos. */
    public static final PacketCodec<RegistryByteBuf, PlaybackState> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.string(CaixaDeMusica.MAX_URL_LENGTH), PlaybackState::url,
            PacketCodecs.VAR_INT, PlaybackState::flags,
            PacketCodecs.VAR_INT, PlaybackState::startIndex,
            PacketCodecs.VAR_LONG, PlaybackState::startedAt,
            PacketCodecs.VAR_LONG, PlaybackState::pausedElapsed,
            PacketCodecs.VAR_INT, PlaybackState::rangeBits,
            PlaybackState::fromPacket);

    public PlaybackState {
        url = url == null ? "" : url;
        startIndex = Math.max(-MAX_INDEX, Math.min(MAX_INDEX, startIndex));
        pausedElapsed = Math.max(0L, pausedElapsed);
        far = Math.max(MIN_FAR, Math.min(MAX_FAR, far));
        near = Math.max(0, Math.min(far, near));
    }

    private int flags() {
        return (playing ? 1 : 0) | (paused ? 2 : 0);
    }

    private int rangeBits() {
        return (near << 16) | far;
    }

    private static PlaybackState fromPacket(String url, int flags, int startIndex, long startedAt,
                                            long pausedElapsed, int range) {
        return new PlaybackState(url, (flags & 1) != 0, (flags & 2) != 0, startIndex, startedAt, pausedElapsed,
                range >>> 16, range & 0xFFFF);
    }

    /** Tem música para sair do alto-falante agora (tocando, não pausada e com link). */
    public boolean audible() {
        return playing && !paused && !url.isBlank();
    }

    // ---------------------------------------------------------------- ações (devolvem o novo estado)

    public PlaybackState play(String newUrl, long now) {
        return new PlaybackState(newUrl, true, false, 0, now, 0L, near, far);
    }

    public PlaybackState stop(long now) {
        return new PlaybackState(url, false, false, 0, now, 0L, near, far);
    }

    /** Pula para a música {@code index} da playlist (o cliente já manda o número certo). */
    public PlaybackState jump(int index, long now) {
        if (!playing || url.isEmpty()) {
            return this;
        }
        return new PlaybackState(url, true, false, index, now, 0L, near, far);
    }

    public PlaybackState pause(long now) {
        if (!playing || paused) {
            return this;
        }
        return new PlaybackState(url, true, true, startIndex, startedAt, Math.max(0L, now - startedAt), near, far);
    }

    public PlaybackState resume(long now) {
        if (!playing || !paused) {
            return this;
        }
        return new PlaybackState(url, true, false, startIndex, now - pausedElapsed, 0L, near, far);
    }

    /** Avança ({@code deltaMs} &gt; 0) ou volta ({@code deltaMs} &lt; 0) dentro da música atual. */
    public PlaybackState seek(long deltaMs, long now) {
        if (!playing || url.isEmpty()) {
            return this;
        }
        long delta = Math.max(-MAX_SEEK_MS, Math.min(MAX_SEEK_MS, deltaMs));
        if (paused) {
            return new PlaybackState(url, true, true, startIndex, startedAt, pausedElapsed + delta, near, far);
        }
        return new PlaybackState(url, true, false, startIndex, Math.min(now, startedAt - delta), 0L, near, far);
    }

    public PlaybackState range(int newNear, int newFar) {
        return new PlaybackState(url, playing, paused, startIndex, startedAt, pausedElapsed, newNear, newFar);
    }
}
