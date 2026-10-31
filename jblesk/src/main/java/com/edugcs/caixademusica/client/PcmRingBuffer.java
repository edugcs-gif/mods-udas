package com.edugcs.caixademusica.client;

import java.nio.ByteBuffer;
import java.util.function.BooleanSupplier;

/**
 * Buffer circular de áudio PCM (16 bits, mono). Uma thread escreve (o decodificador)
 * e outra lê (o sistema de som do Minecraft).
 */
final class PcmRingBuffer {
    private final byte[] data;
    private int readPos;
    private int writePos;
    private int size;
    private long totalWritten;
    private long totalRead;

    PcmRingBuffer(int capacity) {
        this.data = new byte[capacity & ~1];
    }

    /** Escreve tudo, esperando se o buffer estiver cheio. Retorna false se foi cancelado. */
    synchronized boolean write(byte[] src, int off, int len, BooleanSupplier cancelled) throws InterruptedException {
        while (len > 0) {
            while (size == data.length) {
                if (cancelled.getAsBoolean()) {
                    return false;
                }
                wait(100);
            }
            if (cancelled.getAsBoolean()) {
                return false;
            }
            int chunk = Math.min(len, data.length - size);
            int first = Math.min(chunk, data.length - writePos);
            System.arraycopy(src, off, data, writePos, first);
            if (chunk > first) {
                System.arraycopy(src, off + first, data, 0, chunk - first);
            }
            writePos = (writePos + chunk) % data.length;
            size += chunk;
            totalWritten += chunk;
            off += chunk;
            len -= chunk;
        }
        return true;
    }

    /** Lê até {@code max} bytes para dentro de {@code dst}, sem esperar. */
    synchronized int read(ByteBuffer dst, int max) {
        int n = Math.min(Math.min(max, size), dst.remaining()) & ~1;
        if (n <= 0) {
            return 0;
        }
        int first = Math.min(n, data.length - readPos);
        dst.put(data, readPos, first);
        if (n > first) {
            dst.put(data, 0, n - first);
        }
        readPos = (readPos + n) % data.length;
        size -= n;
        totalRead += n;
        notifyAll();
        return n;
    }

    /** Descarta até {@code n} bytes do começo (usado para alcançar o tempo certo da música). */
    synchronized int skip(int n) {
        n = Math.min(n, size) & ~1;
        if (n <= 0) {
            return 0;
        }
        readPos = (readPos + n) % data.length;
        size -= n;
        totalRead += n;
        notifyAll();
        return n;
    }

    synchronized int available() {
        return size;
    }

    synchronized long totalWritten() {
        return totalWritten;
    }

    synchronized long totalRead() {
        return totalRead;
    }

    synchronized void wakeUp() {
        notifyAll();
    }
}
