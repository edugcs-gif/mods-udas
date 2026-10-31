package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.SoundInstance;

/**
 * Uma caixa tocando no PC deste jogador: carrega a música/playlist, decodifica o áudio
 * numa thread separada e entrega para o Minecraft tocar na posição do bloco (ou
 * acompanhando o jogador, no caso da caixa portátil).
 */
final class MusicBoxSession {
    enum State { LOADING, BUFFERING, PLAYING, ERROR }

    /** Cria o som que toca o áudio desta sessão (parado no bloco, ou seguindo um jogador). */
    @FunctionalInterface
    interface SoundFactory {
        SoundInstance create(MusicBoxSession session, MusicBoxAudioStream stream);
    }

    /**
     * O que está saindo no alto-falante agora: título, ponto da música ({@code durationMs} = 0 se for
     * transmissão ao vivo ou a duração não for conhecida), endereço da capa e posição na playlist.
     */
    record Now(String title, long positionMs, long durationMs, String artworkUrl, int index, int count) {
    }

    /** Quanto áudio juntar antes de começar a tocar (o Minecraft pede 4 s de uma vez no início). */
    private static final int START_BUFFER_MS = 4500;
    private static final int BUFFER_CAPACITY_MS = 15_000;
    /** Desiste de esperar o buffer depois disso e começa assim mesmo. */
    private static final long MAX_WAIT_MS = 20_000;
    /** Se o som sumir (ex.: troca de dispositivo de áudio), reinicia depois disso. */
    private static final int RESTART_AFTER_TICKS = 40;

    /** Quem é o dono desta sessão: a posição do bloco ou o UUID do jogador com a caixa portátil. */
    final Object key;
    private final String label;
    private final SoundFactory soundFactory;
    final String url;
    final long playId;
    final int startIndex;
    private final long clientStartMs;
    /** Quantas vezes esta reprodução já foi reiniciada automaticamente. */
    final int restarts;
    /** O som sai "colado" na cabeça de quem ouve (a caixa que o jogador carrega, ou o veículo em que está). */
    boolean ownedByListener;

    private final PcmRingBuffer buffer = new PcmRingBuffer(MusicBoxAudioStream.BYTES_PER_MS * BUFFER_CAPACITY_MS);
    private final ConcurrentLinkedDeque<TrackMarker> markers = new ConcurrentLinkedDeque<>();

    private volatile State state = State.LOADING;
    private volatile String error = "";
    private volatile boolean stopped;
    private volatile boolean finished;
    private volatile List<AudioTrack> tracks = List.of();
    private volatile int decodingIndex = -1;
    private volatile long seekIssuedAt;
    private volatile int consecutiveFailures;
    private volatile boolean wantsRestart;
    /** Alcance da caixa (blocos), que o dono pode mudar enquanto toca: o som lê estes valores a cada tick. */
    private volatile int near = 8;
    private volatile int far = 64;

    private volatile AudioPlayer player;
    private Thread pump;
    private SoundInstance sound;
    private int notPlayingTicks;

    MusicBoxSession(Object key, String label, SoundFactory soundFactory, String url, long playId, int startIndex,
                    long clientStartMs, int restarts) {
        this.key = key;
        this.label = label;
        this.soundFactory = soundFactory;
        this.url = url;
        this.playId = playId;
        this.startIndex = startIndex;
        this.clientStartMs = clientStartMs;
        this.restarts = restarts;
    }

    // ------------------------------------------------------------------ carregamento

    /** Começa a carregar, fora da thread do jogo (criar o player de áudio na primeira vez leva quase 1 segundo). */
    void start() {
        AudioLibrary.runInBackground(() -> {
            try {
                load();
            } catch (Throwable t) {
                CaixaDeMusica.LOGGER.error("Falha ao iniciar a caixa de musica {}", label, t);
                fail("o player de áudio não iniciou (veja o log)");
            }
        });
    }

    private void load() {
        if (stopped) {
            return;
        }
        String identifier = AudioLibrary.toIdentifier(url);
        List<AudioTrack> cached = AudioLibrary.cached(identifier);
        if (cached != null) {
            begin(cached);
            return;
        }
        try {
            AudioLibrary.manager().loadItem(identifier, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    List<AudioTrack> list = List.of(track);
                    AudioLibrary.cache(identifier, list);
                    begin(list);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    List<AudioTrack> all = playlist.getTracks();
                    if (all.isEmpty()) {
                        fail("a playlist está vazia");
                        return;
                    }
                    List<AudioTrack> list;
                    if (playlist.isSearchResult()) {
                        // Busca por nome: toca só o primeiro resultado.
                        list = List.of(all.get(0));
                    } else {
                        list = new ArrayList<>(all);
                        AudioTrack selected = playlist.getSelectedTrack();
                        int selectedIndex = selected == null ? -1 : list.indexOf(selected);
                        if (selectedIndex > 0) {
                            // Link de vídeo dentro de playlist: começa pelo vídeo do link.
                            List<AudioTrack> rotated = new ArrayList<>(list.subList(selectedIndex, list.size()));
                            rotated.addAll(list.subList(0, selectedIndex));
                            list = rotated;
                        }
                    }
                    AudioLibrary.cache(identifier, list);
                    begin(list);
                }

                @Override
                public void noMatches() {
                    fail("nada encontrado para esse link/busca");
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    fail(String.valueOf(exception.getMessage()));
                }
            });
        } catch (Throwable t) {
            CaixaDeMusica.LOGGER.error("Falha ao iniciar o player de audio", t);
            fail("o player de áudio não iniciou (veja o log)");
        }
    }

    private synchronized void begin(List<AudioTrack> loaded) {
        if (stopped) {
            return;
        }
        tracks = loaded;
        int n = loaded.size();

        // Descobre em que música e em que ponto dela a caixa está agora.
        long elapsed = Math.max(0L, System.currentTimeMillis() - clientStartMs);
        int index = Math.floorMod(startIndex, n);
        long total = 0;
        boolean allKnown = true;
        for (AudioTrack t : loaded) {
            long d = duration(t);
            if (d <= 0) {
                allKnown = false;
                break;
            }
            total += d;
        }
        long offset = elapsed;
        if (allKnown && total > 0) {
            offset %= total; // a playlist fica em loop
            while (offset >= duration(loaded.get(index))) {
                offset -= duration(loaded.get(index));
                index = (index + 1) % n;
            }
        } else {
            for (int guard = 0; guard < n; guard++) {
                long d = duration(loaded.get(index));
                if (d <= 0) {
                    offset = 0; // transmissão ao vivo: entra no ponto atual dela
                    break;
                }
                if (offset < d) {
                    break;
                }
                offset -= d;
                index = (index + 1) % n;
            }
        }

        seekIssuedAt = System.currentTimeMillis();
        player = AudioLibrary.manager().createPlayer();
        player.addListener(new AudioEventAdapter() {
            @Override
            public void onTrackEnd(AudioPlayer p, AudioTrack track, AudioTrackEndReason endReason) {
                if (stopped) {
                    return;
                }
                if (endReason == AudioTrackEndReason.FINISHED) {
                    consecutiveFailures = 0;
                    playNext();
                } else if (endReason == AudioTrackEndReason.LOAD_FAILED) {
                    consecutiveFailures++;
                    playNext();
                } else if (endReason == AudioTrackEndReason.CLEANUP) {
                    wantsRestart = true;
                }
            }

            @Override
            public void onTrackException(AudioPlayer p, AudioTrack track, FriendlyException exception) {
                error = String.valueOf(exception.getMessage());
                CaixaDeMusica.LOGGER.warn("Erro ao tocar \"{}\": {}", track.getInfo().title, exception.getMessage());
            }

            @Override
            public void onTrackStuck(AudioPlayer p, AudioTrack track, long thresholdMs) {
                if (stopped) {
                    return;
                }
                CaixaDeMusica.LOGGER.warn("Musica travou: \"{}\", pulando", track.getInfo().title);
                consecutiveFailures++;
                playNext();
            }
        });

        state = State.BUFFERING;
        playIndex(index, offset);

        pump = new Thread(this::pumpLoop, "CaixaDeMusica " + label);
        pump.setDaemon(true);
        pump.start();
    }

    private static long duration(AudioTrack track) {
        if (track.getInfo().isStream) {
            return 0;
        }
        long d = track.getDuration();
        return d == Long.MAX_VALUE ? 0 : Math.max(0, d);
    }

    private synchronized void playNext() {
        if (stopped) {
            return;
        }
        List<AudioTrack> list = tracks;
        if (list.isEmpty()) {
            return;
        }
        if (consecutiveFailures >= Math.min(list.size(), 5)) {
            fail(error.isEmpty() ? "não consegui tocar as músicas" : error);
            return;
        }
        playIndex((decodingIndex + 1) % list.size(), 0);
    }

    private synchronized void playIndex(int index, long offsetMs) {
        AudioTrack track = tracks.get(index).makeClone();
        long startMs = 0;
        if (offsetMs > 0 && track.isSeekable()) {
            track.setPosition(offsetMs);
            startMs = offsetMs;
        }
        decodingIndex = index;
        markers.addLast(new TrackMarker(buffer.totalWritten(), index, title(track), startMs, duration(track),
                artworkUrl(track)));
        player.playTrack(track);
    }

    /**
     * O endereço da capa: o que a fonte informar (SoundCloud, por exemplo). A fonte do YouTube não
     * informa nenhum, então usa a miniatura padrão do vídeo (16:9), que existe para todo vídeo.
     */
    private static String artworkUrl(AudioTrack track) {
        String url = track.getInfo().artworkUrl;
        if (url != null && !url.isBlank()) {
            return url;
        }
        String id = track.getIdentifier();
        if (track.getSourceManager() != null && "youtube".equals(track.getSourceManager().getSourceName())
                && id != null && id.matches("[A-Za-z0-9_-]{11}")) {
            return "https://i.ytimg.com/vi/" + id + "/mqdefault.jpg";
        }
        return null;
    }

    private static String title(AudioTrack track) {
        String title = track.getInfo().title;
        String author = track.getInfo().author;
        if (title == null || title.isBlank()) {
            title = track.getInfo().uri == null ? "?" : track.getInfo().uri;
        }
        if (author != null && !author.isBlank() && !title.contains(author)
                && !"Unknown artist".equalsIgnoreCase(author)) {
            return author + " - " + title;
        }
        return title;
    }

    private void fail(String message) {
        error = message;
        state = State.ERROR;
        finished = true;
        stopped = true; // a thread de decodificação encerra; o resto é liberado em stop()
        buffer.wakeUp();
        CaixaDeMusica.LOGGER.warn("Caixa de musica {} ({}): {}", label, url, message);
    }

    /** Desiste depois de reiniciar várias vezes seguidas (ex.: PC sem dispositivo de áudio). */
    void giveUp(MinecraftClient client) {
        stop(client);
        error = "o som parou várias vezes seguidas; afaste-se e volte para tentar de novo";
        state = State.ERROR;
    }

    // ------------------------------------------------------------------ decodificação

    /** Puxa o áudio do Lavaplayer, converte para mono e guarda no buffer. */
    private void pumpLoop() {
        byte[] mono = new byte[0];
        while (!stopped) {
            try {
                AudioFrame frame = player.provide(100, TimeUnit.MILLISECONDS);
                if (frame == null) {
                    Thread.sleep(10);
                    continue;
                }
                byte[] stereo = frame.getData();
                int stereoLength = frame.getDataLength() & ~3;
                int monoLength = stereoLength / 2;
                if (mono.length < monoLength) {
                    mono = new byte[monoLength];
                }
                for (int i = 0, o = 0; i < stereoLength; i += 4, o += 2) {
                    int left = (short) ((stereo[i] & 0xFF) | (stereo[i + 1] << 8));
                    int right = (short) ((stereo[i + 2] & 0xFF) | (stereo[i + 3] << 8));
                    int mixed = (left + right) / 2;
                    mono[o] = (byte) mixed;
                    mono[o + 1] = (byte) (mixed >> 8);
                }
                if (!buffer.write(mono, 0, monoLength, () -> stopped)) {
                    break;
                }
            } catch (TimeoutException ignored) {
                // sem áudio ainda, tenta de novo
            } catch (InterruptedException e) {
                break;
            } catch (Throwable t) {
                if (!stopped) {
                    CaixaDeMusica.LOGGER.error("Erro decodificando audio da caixa {}", label, t);
                    fail("erro no áudio (veja o log)");
                }
                break;
            }
        }
    }

    // ------------------------------------------------------------------ thread do jogo

    /** Chamado a cada tick do cliente. */
    void tick(MinecraftClient client) {
        if (stopped) {
            return;
        }
        if (state == State.BUFFERING && sound == null) {
            long lagMs = System.currentTimeMillis() - seekIssuedAt;
            int availableMs = buffer.available() / MusicBoxAudioStream.BYTES_PER_MS;
            boolean enough = availableMs >= lagMs + START_BUFFER_MS
                    || availableMs >= BUFFER_CAPACITY_MS - 1000
                    || (lagMs > MAX_WAIT_MS && availableMs > 0);
            if (enough || (finished && availableMs > 0)) {
                // Joga fora o áudio do tempo que levou para carregar, para ficar em sincronia.
                int keep = Math.min(availableMs, START_BUFFER_MS);
                long dropMs = Math.max(0, Math.min(lagMs, availableMs - keep));
                buffer.skip((int) (dropMs * MusicBoxAudioStream.BYTES_PER_MS));
                startSound(client);
            }
            return;
        }
        if (state == State.PLAYING && sound != null) {
            if (client.getSoundManager().isPlaying(sound)) {
                notPlayingTicks = 0;
            } else if (++notPlayingTicks > RESTART_AFTER_TICKS) {
                if (finished) {
                    state = State.ERROR;
                } else {
                    wantsRestart = true;
                }
            }
        }
    }

    private void startSound(MinecraftClient client) {
        sound = soundFactory.create(this, new MusicBoxAudioStream(this, buffer));
        client.getSoundManager().play(sound);
        // Para a música de fundo do jogo para não misturar.
        client.getMusicTracker().stop();
        state = State.PLAYING;
        notPlayingTicks = 0;
    }

    void stop(MinecraftClient client) {
        stopped = true;
        finished = true;
        buffer.wakeUp();
        if (sound != null) {
            client.getSoundManager().stop(sound);
            sound = null;
        }
        AudioPlayer p = player;
        if (p != null) {
            try {
                p.destroy();
            } catch (Throwable ignored) {
                // nada a fazer
            }
        }
    }

    // ------------------------------------------------------------------ informações para a tela

    boolean isFinished() {
        return finished;
    }

    boolean wantsRestart() {
        return wantsRestart && !stopped;
    }

    State state() {
        return state;
    }

    String error() {
        return error;
    }

    int trackCount() {
        return tracks.size();
    }

    /** Música que está saindo no alto-falante agora (não a que está sendo baixada). */
    int audibleIndex() {
        TrackMarker marker = audibleMarker();
        return marker == null ? Math.max(0, decodingIndex) : marker.index;
    }

    String audibleTitle() {
        TrackMarker marker = audibleMarker();
        return marker == null ? "" : marker.title;
    }

    private TrackMarker audibleMarker() {
        long read = buffer.totalRead();
        while (markers.size() >= 2) {
            Iterator<TrackMarker> it = markers.iterator();
            it.next();
            TrackMarker second = it.next();
            if (second.offset <= read) {
                markers.pollFirst();
            } else {
                break;
            }
        }
        return markers.peekFirst();
    }

    /** O que está tocando agora, ou {@code null} se ainda não começou nenhuma música. */
    Now now() {
        TrackMarker marker = audibleMarker();
        if (marker == null) {
            return null;
        }
        long playedMs = Math.max(0L, buffer.totalRead() - marker.offset) / MusicBoxAudioStream.BYTES_PER_MS;
        long position = marker.startMs + playedMs;
        if (marker.durationMs > 0) {
            position = Math.min(position, marker.durationMs);
        }
        return new Now(marker.title, position, marker.durationMs, marker.artworkUrl, marker.index, tracks.size());
    }

    void setRange(int near, int far) {
        this.near = near;
        this.far = far;
    }

    int near() {
        return near;
    }

    int far() {
        return far;
    }

    /**
     * Um trecho do buffer que pertence a uma música: {@code offset} é quantos bytes já tinham sido escritos
     * quando ela começou, {@code startMs} o ponto da música onde começou (maior que zero se entrou no meio).
     */
    private record TrackMarker(long offset, int index, String title, long startMs, long durationMs,
                               String artworkUrl) {
    }
}
