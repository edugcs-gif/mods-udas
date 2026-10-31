package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.CaixaDeMusica;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Baixa as capas/miniaturas das músicas e as transforma em texturas do jogo. O download e a
 * leitura da imagem rodam numa thread separada; a textura só é criada na thread do jogo.
 * {@link #get} é chamado da thread do jogo.
 */
final class ArtworkCache {
    private static final int MAX_TEXTURES = 12;
    private static final int MAX_BYTES = 4_000_000;

    /** Uma miniatura pronta para desenhar. */
    record Art(Identifier id, int width, int height) {
    }

    // Ordem de acesso: o primeiro da lista é o menos usado, que sai quando passa do limite.
    private static final Map<String, Art> READY = new LinkedHashMap<>(16, 0.75F, true);
    /** Endereços já pedidos (baixando ou que falharam): não tenta de novo. */
    private static final Set<String> REQUESTED = new HashSet<>();
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CaixaDeMusica artwork");
        thread.setDaemon(true);
        return thread;
    });
    private static int counter;

    private ArtworkCache() {
    }

    /** A miniatura deste endereço, ou {@code null} se ainda está baixando (ou não deu certo). */
    static Art get(String url) {
        if (url == null || !url.startsWith("https://")) {
            return null;
        }
        Art art = READY.get(url);
        if (art == null && REQUESTED.add(url)) {
            POOL.execute(() -> download(url));
        }
        return art;
    }

    private static void download(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("User-Agent", "CaixaDeMusica");
            byte[] bytes;
            try (InputStream in = connection.getInputStream()) {
                bytes = in.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) {
                throw new IllegalStateException("imagem grande demais");
            }
            NativeImage image = decode(bytes);
            MinecraftClient.getInstance().execute(() -> register(url, image));
        } catch (Throwable t) {
            CaixaDeMusica.LOGGER.warn("Nao consegui carregar a miniatura {}: {}", url, t.toString());
        }
    }

    /** Lê a imagem direto com o leitor do jogo (que entende JPEG); se não der, converte para PNG antes. */
    private static NativeImage decode(byte[] bytes) throws Exception {
        try {
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (Throwable direct) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalStateException("formato de imagem desconhecido (" + direct + ")");
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            return NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
        }
    }

    private static void register(String url, NativeImage image) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            int width = image.getWidth();
            int height = image.getHeight();
            Identifier id = CaixaDeMusica.id("artwork/" + (counter++));
            client.getTextureManager().registerTexture(id, new NativeImageBackedTexture(image));
            READY.put(url, new Art(id, width, height));

            Iterator<Map.Entry<String, Art>> it = READY.entrySet().iterator();
            while (READY.size() > MAX_TEXTURES && it.hasNext()) {
                Map.Entry<String, Art> eldest = it.next();
                client.getTextureManager().destroyTexture(eldest.getValue().id());
                REQUESTED.remove(eldest.getKey());
                it.remove();
            }
        } catch (Throwable t) {
            CaixaDeMusica.LOGGER.warn("Nao consegui criar a textura da miniatura {}: {}", url, t.toString());
        }
    }
}
