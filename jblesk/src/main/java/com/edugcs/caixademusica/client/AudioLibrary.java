package com.edugcs.caixademusica.client;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.format.Pcm16AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Gerencia o Lavaplayer (só no cliente) e guarda as playlists já carregadas por um tempo. */
final class AudioLibrary {
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(20);

    private static AudioPlayerManager manager;
    private static final Map<String, CachedTracks> CACHE = new HashMap<>();
    /** Uma thread só, que cria o player de áudio e começa a carregar as músicas fora da thread do jogo. */
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "CaixaDeMusica loader");
        thread.setDaemon(true);
        return thread;
    });

    private AudioLibrary() {
    }

    /** Roda fora da thread do jogo: criar o player de áudio na primeira vez leva quase 1 segundo. */
    static void runInBackground(Runnable task) {
        WORKER.execute(task);
    }

    static synchronized AudioPlayerManager manager() {
        if (manager == null) {
            DefaultAudioPlayerManager m = new DefaultAudioPlayerManager();
            // PCM 48 kHz estéreo, little-endian; a sessão converte para mono.
            m.getConfiguration().setOutputFormat(new Pcm16AudioDataFormat(2, MusicBoxAudioStream.SAMPLE_RATE, 960, false));
            // Não deixa o Lavaplayer encerrar a música se o jogo ficar pausado por um tempo.
            m.setPlayerCleanupThreshold(TimeUnit.HOURS.toMillis(12));
            // Fonte do YouTube mantida pela comunidade (a embutida no Lavaplayer não funciona mais).
            // A lista de clientes é explícita porque o conjunto padrão não inclui o IOS, que toca
            // muitos clipes de gravadora que os outros recusam ("This video requires login").
            // A ordem é a de tentativa; o Music também atende as buscas por nome.
            m.registerSourceManager(new dev.lavalink.youtube.YoutubeAudioSourceManager(true,
                    new dev.lavalink.youtube.clients.Music(),
                    new dev.lavalink.youtube.clients.AndroidVr(),
                    new dev.lavalink.youtube.clients.Ios(),
                    new dev.lavalink.youtube.clients.Web(),
                    new dev.lavalink.youtube.clients.WebEmbedded()));
            AudioSourceManagers.registerRemoteSources(m, MediaContainerRegistry.DEFAULT_REGISTRY,
                    com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
            manager = m;
        }
        return manager;
    }

    /**
     * Transforma o que o jogador digitou em algo que o Lavaplayer entende: um link
     * ou uma busca no YouTube.
     */
    static String toIdentifier(String input) {
        String text = input.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return normalizeYoutubeShortLink(text);
        }
        if (lower.startsWith("www.") || lower.startsWith("youtube.com/") || lower.startsWith("youtu.be/")
                || lower.startsWith("m.youtube.com/") || lower.startsWith("music.youtube.com/")
                || lower.startsWith("soundcloud.com/")) {
            return normalizeYoutubeShortLink("https://" + text);
        }
        if (lower.startsWith("ytsearch:") || lower.startsWith("ytmsearch:") || lower.startsWith("scsearch:")) {
            return text;
        }
        return "ytsearch:" + text;
    }

    /**
     * O link curto do YouTube ({@code youtu.be/ID?list=...}, o que o botão "Compartilhar" gera) é carregado pela
     * biblioteca como UM vídeo só, sem a playlist ou o Mix; aí não existe "próxima música". Troca pelo link
     * completo ({@code youtube.com/watch?v=ID&list=...}), que carrega a playlist inteira.
     */
    static String normalizeYoutubeShortLink(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            if (host == null || path == null || query == null || path.length() < 2
                    || !(host.equalsIgnoreCase("youtu.be") || host.equalsIgnoreCase("www.youtu.be"))) {
                return url;
            }
            String videoId = path.substring(1);
            for (String param : query.split("&")) {
                if (param.startsWith("list=") && param.length() > "list=".length()) {
                    return "https://www.youtube.com/watch?v=" + videoId + "&" + param;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // endereço malformado: deixa como está e a biblioteca avisa
        }
        return url;
    }

    static boolean isSpotify(String input) {
        String lower = input.trim().toLowerCase(Locale.ROOT);
        return lower.contains("spotify.com") || lower.startsWith("spotify:");
    }

    static synchronized List<AudioTrack> cached(String identifier) {
        CachedTracks entry = CACHE.get(identifier);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.time > CACHE_TTL_MS) {
            CACHE.remove(identifier);
            return null;
        }
        return entry.tracks;
    }

    static synchronized void cache(String identifier, List<AudioTrack> tracks) {
        if (CACHE.size() > 32) {
            CACHE.clear();
        }
        CACHE.put(identifier, new CachedTracks(List.copyOf(tracks), System.currentTimeMillis()));
    }

    private record CachedTracks(List<AudioTrack> tracks, long time) {
    }
}
