package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.CaixaDeMusica;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Configurações só deste jogador (não vão para o servidor): o volume pessoal, de 0 a 150%,
 * e o "mudo". Ficam salvas em {@code config/caixademusica-client.properties}.
 */
final class ClientSettings {
    static final int MAX_VOLUME = 150;

    private static int volume = 100;
    private static boolean muted;
    private static boolean loaded;

    private ClientSettings() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("caixademusica-client.properties");
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = file();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            volume = Math.max(0, Math.min(MAX_VOLUME, Integer.parseInt(props.getProperty("volume", "100").trim())));
            muted = Boolean.parseBoolean(props.getProperty("muted", "false").trim());
        } catch (IOException | NumberFormatException e) {
            CaixaDeMusica.LOGGER.warn("Nao consegui ler {}: {}", file, e.getMessage());
        }
    }

    private static void save() {
        Path file = file();
        try (OutputStream out = Files.newOutputStream(file)) {
            Properties props = new Properties();
            props.setProperty("volume", Integer.toString(volume));
            props.setProperty("muted", Boolean.toString(muted));
            props.store(out, "Caixa de Musica (so deste jogador)");
        } catch (IOException e) {
            CaixaDeMusica.LOGGER.warn("Nao consegui salvar {}: {}", file, e.getMessage());
        }
    }

    /** Volume em porcentagem (0 a 150), sem considerar o "mudo". */
    static synchronized int volume() {
        ensureLoaded();
        return volume;
    }

    static synchronized boolean muted() {
        ensureLoaded();
        return muted;
    }

    /** Multiplicador do volume: 0 se estiver mudo, 1.0 = 100%. Passa de 1.0 só acima de 100%. */
    static synchronized float gain() {
        ensureLoaded();
        return muted ? 0.0F : volume / 100.0F;
    }

    /** O quanto amplificar o áudio além do volume normal (só vale acima de 100%). */
    static synchronized float boost() {
        return Math.max(1.0F, gain());
    }

    static synchronized void changeVolume(int delta) {
        ensureLoaded();
        volume = Math.max(0, Math.min(MAX_VOLUME, volume + delta));
        if (delta > 0) {
            muted = false;
        }
        save();
    }

    static synchronized void toggleMute() {
        ensureLoaded();
        muted = !muted;
        save();
    }
}
