package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.PlaybackState;
import com.edugcs.caixademusica.block.MusicBoxBlockEntity;
import com.edugcs.caixademusica.network.PortableSourcesPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Decide, a cada tick, quais caixas devem estar tocando neste PC: só as que estão
 * tocando no servidor e que estão perto do jogador.
 *
 * <p>Uma "caixa" é um bloco (chave: {@link BlockPos}) ou uma caixa portátil carregada por uma
 * entidade (chave: o id da entidade, um {@link Integer}): o jogador, um burro, um barco...
 */
public final class MusicBoxClientManager {
    /** Começa a baixar o áudio quando o jogador chega a esta distância além do alcance "longe" da caixa. */
    private static final double START_MARGIN = 8;
    /** Para de tocar quando o jogador se afasta mais que isto além do alcance "longe". */
    private static final double STOP_MARGIN = 32;
    /** Se a caixa não "tickar" por esse tanto de ticks, consideramos que ela sumiu. */
    private static final long STALE_TICKS = 40;
    /** Quantas vezes seguidas uma caixa pode reiniciar sozinha antes de desistir. */
    private static final int MAX_RESTARTS = 3;

    private static final Map<Object, BoxInfo> SEEN = new HashMap<>();
    private static final Map<Object, MusicBoxSession> SESSIONS = new HashMap<>();
    /** O último "agora tocando" de cada caixa, para a tela mostrar quando está pausada. */
    private static final Map<Object, MusicBoxSession.Now> LAST_NOW = new HashMap<>();
    private static ClientWorld lastWorld;
    /** Chaves das caixas carregadas por entidade (o id da entidade); as de bloco usam a posição. */
    private static final Predicate<Object> INTEGER_KEY = key -> key instanceof Integer;

    /** As caixas portáteis que o servidor avisou que estão tocando: id da entidade que a carrega -> estado. */
    private static final Map<Integer, PlaybackState> PORTABLE_SOURCES = new HashMap<>();
    /** O mundo em que {@link #PORTABLE_SOURCES} foi recebida (se o mundo mudar, a lista antiga não vale mais). */
    private static ClientWorld portableWorld;

    /** Relógio do servidor menos o deste PC (ms); vem do pacote enviado ao entrar no servidor. */
    private static long serverClockOffset;

    private MusicBoxClientManager() {
    }

    /** Chamado quando o servidor manda o relógio dele (thread do jogo). */
    static void setServerClock(long serverMillis) {
        serverClockOffset = serverMillis - System.currentTimeMillis();
    }

    /** Chamado quando o servidor manda a lista de caixas portáteis tocando por perto (thread do jogo). */
    static void setPortableSources(List<PortableSourcesPayload.Entry> entries) {
        PORTABLE_SOURCES.clear();
        for (PortableSourcesPayload.Entry entry : entries) {
            PORTABLE_SOURCES.put(entry.entityId(), entry.state());
        }
        portableWorld = MinecraftClient.getInstance().world;
    }

    /**
     * O estado que o servidor informou para a caixa carregada por esta entidade (a embutida no baú de um
     * animal ou barco), ou {@code null} se ainda não informou nada.
     */
    static PlaybackState sourceState(int entityId) {
        return PORTABLE_SOURCES.get(entityId);
    }

    /** Chamado pelo ticker da caixa (thread do jogo). */
    static void report(MusicBoxBlockEntity box) {
        if (box.getWorld() == null) {
            return;
        }
        BlockPos pos = box.getPos().toImmutable();
        PlaybackState state = box.getState();
        if (!state.playing()) {
            // Caixa parada: não guarda nada (quem estava tocando é parado no próximo tick). Evita alocar
            // objetos a cada tick por caixa que não está tocando.
            if (!SEEN.isEmpty()) {
                SEEN.remove(pos);
            }
            return;
        }
        SEEN.put(pos, new BoxInfo(state, box.getClientStartMs(), box.getWorld().getTime(), Vec3d.ofCenter(pos), false));
    }

    static void removed(BlockPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!client.isOnThread()) {
            BlockPos immutable = pos.toImmutable();
            client.execute(() -> removed(immutable));
            return;
        }
        SEEN.remove(pos);
        MusicBoxSession session = SESSIONS.remove(pos);
        if (session != null) {
            session.stop(client);
        }
    }

    /** A sessão de uma caixa: passe a posição do bloco ou o id da entidade que carrega a caixa portátil. */
    static MusicBoxSession session(Object key) {
        return SESSIONS.get(key);
    }

    /**
     * O que está tocando agora nesta caixa: da sessão, se ela está tocando; senão o último que
     * se soube (por exemplo, enquanto está pausada). {@code null} se nunca se soube.
     */
    static MusicBoxSession.Now now(Object key) {
        MusicBoxSession session = SESSIONS.get(key);
        if (session != null && session.state() == MusicBoxSession.State.PLAYING) {
            MusicBoxSession.Now now = session.now();
            if (now != null) {
                return now;
            }
        }
        return LAST_NOW.get(key);
    }

    static boolean isMuted(MinecraftClient client) {
        return client.options.getSoundVolume(SoundCategory.MASTER) <= 0.0F
                || client.options.getSoundVolume(SoundCategory.RECORDS) <= 0.0F;
    }

    /** De onde sai o som de quem carrega a caixa: a cabeça, se for um jogador; o centro, se for um animal ou barco. */
    private static Vec3d positionOf(Entity entity) {
        return entity instanceof PlayerEntity ? entity.getEyePos() : entity.getBoundingBox().getCenter();
    }

    /** Onde está agora a entidade que carrega a caixa ({@code null} se ela sumiu deste mundo). */
    private static Vec3d positionOf(int entityId) {
        ClientWorld world = MinecraftClient.getInstance().world;
        Entity entity = world == null ? null : world.getEntityById(entityId);
        return entity == null ? null : positionOf(entity);
    }

    /** Transforma a lista do servidor em caixas "vistas", usando a posição das entidades que as carregam. */
    private static void scanPortables(MinecraftClient client, ClientWorld world) {
        if (portableWorld != world) {
            PORTABLE_SOURCES.clear(); // a lista é de outro mundo (mudou de dimensão ou reconectou)
            portableWorld = world;
        }
        if (PORTABLE_SOURCES.isEmpty()) {
            // Nenhuma caixa carregada por entidade: só limpa o que sobrou de antes (sem alocar nada).
            if (!SEEN.isEmpty()) {
                SEEN.keySet().removeIf(INTEGER_KEY);
            }
            return;
        }
        long now = world.getTime();
        Entity vehicle = client.player.getVehicle();
        Set<Integer> active = new HashSet<>();
        for (Map.Entry<Integer, PlaybackState> entry : PORTABLE_SOURCES.entrySet()) {
            PlaybackState state = entry.getValue();
            if (!state.playing()) {
                continue; // caixa embutida parada: só a tela precisa saber dela
            }
            Entity carrier = world.getEntityById(entry.getKey());
            if (carrier == null) {
                continue; // longe demais para este cliente enxergar: ainda não dá para saber onde está
            }
            // Quem carrega a caixa (ou anda no veículo que a leva) ouve "colada" na cabeça: uma fonte posicionada
            // em cima do ouvinte e atualizada só a cada tick balança de lado a cada passo ou giro de câmera.
            boolean own = carrier == client.player || carrier == vehicle;
            active.add(entry.getKey());
            SEEN.put(entry.getKey(), new BoxInfo(state, state.startedAt() - serverClockOffset, now, positionOf(carrier),
                    own));
        }
        // A caixa saiu da lista (guardada em outro lugar, parada...) ou o dono sumiu: some na hora.
        SEEN.keySet().removeIf(key -> key instanceof Integer && !active.contains(key));
    }

    static void tick(MinecraftClient client) {
        if (client.world != lastWorld) {
            stopAll(client);
            SEEN.clear();
            LAST_NOW.clear();
            lastWorld = client.world;
        }
        if (client.world == null || client.player == null) {
            return;
        }
        if (SEEN.isEmpty() && SESSIONS.isEmpty() && PORTABLE_SOURCES.isEmpty() && LAST_NOW.isEmpty()) {
            return; // nada tocando nem para tocar: não gasta nada por tick
        }

        scanPortables(client, client.world);

        long now = client.world.getTime();
        SEEN.values().removeIf(info -> now - info.seenAt > STALE_TICKS || info.seenAt > now + STALE_TICKS);
        LAST_NOW.keySet().removeIf(key -> {
            BoxInfo info = SEEN.get(key);
            return info == null || !info.state.playing();
        });

        boolean muted = isMuted(client);
        Vec3d playerPos = client.player.getPos();

        for (Map.Entry<Object, BoxInfo> entry : SEEN.entrySet()) {
            Object key = entry.getKey();
            BoxInfo info = entry.getValue();
            PlaybackState state = info.state;
            MusicBoxSession session = SESSIONS.get(key);

            double distance = Math.sqrt(playerPos.squaredDistanceTo(info.position));
            double limit = state.far() + (session == null ? START_MARGIN : STOP_MARGIN);
            boolean shouldPlay = state.audible() && !muted && distance <= limit;

            if (session != null) {
                // Mudar quem ouve "colado" (subiu ou desceu do burro/barco) refaz o som, que é criado de um jeito ou do outro.
                boolean changed = session.playId != state.startedAt()
                        || session.startIndex != state.startIndex()
                        || !session.url.equals(state.url())
                        || session.ownedByListener != info.ownedByListener;
                if (!shouldPlay || changed) {
                    rememberNow(key, session);
                    session.stop(client);
                    SESSIONS.remove(key);
                    session = null;
                }
            }
            if (shouldPlay && session == null) {
                session = newSession(key, info, 0);
                SESSIONS.put(key, session);
                session.start();
            }
            if (session != null) {
                session.setRange(state.near(), state.far());
            }
        }

        List<MusicBoxSession> restart = null; // criada só se alguma sessão precisar recomeçar
        Iterator<Map.Entry<Object, MusicBoxSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Object, MusicBoxSession> entry = it.next();
            MusicBoxSession session = entry.getValue();
            if (!SEEN.containsKey(entry.getKey())) {
                session.stop(client);
                it.remove();
                continue;
            }
            session.tick(client);
            if (session.state() == MusicBoxSession.State.PLAYING) {
                rememberNow(entry.getKey(), session);
            }
            if (session.wantsRestart()) {
                if (session.restarts >= MAX_RESTARTS) {
                    // Fica no mapa em estado de erro, para não tentar de novo sem parar.
                    session.giveUp(client);
                } else {
                    session.stop(client);
                    it.remove();
                    if (restart == null) {
                        restart = new ArrayList<>();
                    }
                    restart.add(session);
                }
            }
        }
        // Recomeça do ponto certo (ex.: depois de trocar o fone/caixa de som do PC).
        if (restart != null) {
            for (MusicBoxSession old : restart) {
                BoxInfo info = SEEN.get(old.key);
                if (info != null) {
                    MusicBoxSession fresh = newSession(old.key, info, old.restarts + 1);
                    SESSIONS.put(old.key, fresh);
                    fresh.start();
                }
            }
        }
    }

    private static void rememberNow(Object key, MusicBoxSession session) {
        if (session.state() != MusicBoxSession.State.PLAYING) {
            return;
        }
        MusicBoxSession.Now now = session.now();
        if (now != null) {
            LAST_NOW.put(key, now);
        }
    }

    /** Um nome para os logs e para o nome da thread: as coordenadas do bloco ou o nome de quem carrega a caixa. */
    private static String labelFor(Object key) {
        if (key instanceof BlockPos pos) {
            return pos.toShortString();
        }
        ClientWorld world = MinecraftClient.getInstance().world;
        Entity entity = world != null && key instanceof Integer id ? world.getEntityById(id) : null;
        return entity != null ? entity.getName().getString() : String.valueOf(key);
    }

    private static MusicBoxSession newSession(Object key, BoxInfo info, int restarts) {
        PlaybackState state = info.state;
        Vec3d position = info.position;
        boolean own = info.ownedByListener;
        // O bloco soa de onde está; a caixa portátil, de quem a carrega (e acompanha quando ele anda).
        MusicBoxSession.SoundFactory sound = key instanceof Integer id
                ? (session, stream) -> new MusicBoxSoundInstance(session, () -> positionOf(id), own, stream)
                : (session, stream) -> new MusicBoxSoundInstance(session, () -> position, false, stream);
        MusicBoxSession session = new MusicBoxSession(key, labelFor(key), sound, state.url(), state.startedAt(),
                state.startIndex(), info.clientStartMs, restarts);
        session.ownedByListener = own;
        session.setRange(state.near(), state.far());
        return session;
    }

    private static void stopAll(MinecraftClient client) {
        for (MusicBoxSession session : SESSIONS.values()) {
            session.stop(client);
        }
        SESSIONS.clear();
    }

    /** {@code ownedByListener}: a caixa é de quem ouve (soa colada na cabeça dele). */
    private record BoxInfo(PlaybackState state, long clientStartMs, long seenAt, Vec3d position,
                           boolean ownedByListener) {
    }
}
