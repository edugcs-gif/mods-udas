package com.edugcs.caixademusica.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sound.AudioStream;
import org.lwjgl.BufferUtils;

/**
 * Entrega o áudio da sessão para o motor de som do Minecraft (OpenAL).
 *
 * <p>Nunca bloqueia: se a internet atrasar, devolve um pedacinho de silêncio para o
 * Minecraft não achar que a música acabou.
 */
final class MusicBoxAudioStream implements AudioStream {
    static final int SAMPLE_RATE = 48_000;
    /** Mono, 16 bits, little-endian: formato que o OpenAL posiciona no espaço 3D. */
    static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    static final int BYTES_PER_MS = SAMPLE_RATE * 2 / 1000;

    private static final int MIN_CHUNK = BYTES_PER_MS * 250;

    private final MusicBoxSession session;
    private final PcmRingBuffer buffer;

    MusicBoxAudioStream(MusicBoxSession session, PcmRingBuffer buffer) {
        this.session = session;
        this.buffer = buffer;
    }

    @Override
    public AudioFormat getFormat() {
        return FORMAT;
    }

    @Override
    public ByteBuffer read(int size) {
        if (session.isFinished() && buffer.available() == 0) {
            return null;
        }
        int capacity = Math.max(size, MIN_CHUNK) & ~1;
        // Buffer novo já vem zerado (silêncio); a ordem é a do formato, para amplify() ler amostras inteiras.
        ByteBuffer out = BufferUtils.createByteBuffer(capacity).order(ByteOrder.LITTLE_ENDIAN);
        buffer.read(out, capacity);
        float boost = ClientSettings.boost();
        if (boost > 1.001F) {
            amplify(out, boost);
        }
        if (out.position() < MIN_CHUNK && !session.isFinished()) {
            out.position(MIN_CHUNK); // o resto já é silêncio
        }
        out.flip();
        return out;
    }

    /** Volume pessoal acima de 100%: o Minecraft não passa de 100%, então amplificamos o próprio áudio. */
    private static void amplify(ByteBuffer out, float boost) {
        int end = out.position() & ~1;
        for (int i = 0; i < end; i += 2) {
            int amplified = Math.round(out.getShort(i) * boost);
            out.putShort(i, (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, amplified)));
        }
    }

    @Override
    public void close() {
        // Quem cuida do ciclo de vida é a sessão.
    }
}
