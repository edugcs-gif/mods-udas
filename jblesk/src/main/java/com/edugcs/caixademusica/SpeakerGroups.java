package com.edugcs.caixademusica;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Conexão entre caixas: todas as caixas (jbleskão, jblesk portátil, baú de burro/barco) com o mesmo
 * <b>grupo</b> (1 a {@link PlaybackState#MAX_GROUP}) tocam a mesma música, no mesmo ponto, cada uma
 * saindo do seu lugar. Grupo 0 = desconectada.
 *
 * <p>Como funciona: o servidor guarda, só na memória, o último estado de cada grupo (o "mestre"). Quando
 * alguém aperta um botão numa caixa conectada, o novo estado dela vira o mestre do grupo, e as outras
 * caixas do grupo copiam o mestre (menos o alcance perto/longe, que é de cada caixa):
 * <ul>
 *   <li>os blocos conferem a cada 4 ticks (veja {@code MusicBoxBlockEntity#serverTick});</li>
 *   <li>as portáteis e as dos baús de animais/barcos, junto da varredura do {@link PortableTracker}
 *       (meio segundo, ou na hora quando alguém aperta um botão).</li>
 * </ul>
 * Cada caixa guarda o próprio grupo e o último estado dela, então tudo sobrevive a reiniciar o servidor.
 * Só o mestre fica na memória: ele vale por {@link #MASTER_TTL_MS} sem nenhuma caixa do grupo conferindo
 * (por exemplo, todas descarregadas); depois disso a próxima caixa que mexer no grupo cria um novo.
 */
public final class SpeakerGroups {
    /** Quanto tempo o mestre de um grupo vale sem nenhuma caixa do grupo conferir. */
    private static final long MASTER_TTL_MS = 5_000;

    private static final class Master {
        final PlaybackState state;
        long touchedAt;

        Master(PlaybackState state, long touchedAt) {
            this.state = state;
            this.touchedAt = touchedAt;
        }
    }

    private static final Map<Integer, Master> MASTERS = new HashMap<>();

    private SpeakerGroups() {
    }

    public static void init() {
        // Mundo (ou servidor) novo na mesma sessão do jogo: nada do anterior vale.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MASTERS.clear());
    }

    /** O mestre do grupo, se ainda vale; {@code null} se não há ou se expirou. */
    private static Master freshMaster(int group, long now) {
        Master master = MASTERS.get(group);
        if (master == null) {
            return null;
        }
        if (now - master.touchedAt > MASTER_TTL_MS) {
            MASTERS.remove(group);
            return null;
        }
        return master;
    }

    /**
     * Confere uma caixa com o mestre do grupo dela: devolve o estado que ela deve ter agora (o mesmo
     * objeto, se já está igual ao grupo ou se não está conectada). O alcance da caixa é mantido.
     */
    public static PlaybackState adopt(PlaybackState current) {
        int group = current.group();
        if (group == 0) {
            return current;
        }
        long now = System.currentTimeMillis();
        Master master = freshMaster(group, now);
        if (master == null) {
            return current;
        }
        master.touchedAt = now; // tem caixa do grupo por aí conferindo: o mestre continua valendo
        if (current.samePlayback(master.state)) {
            return current;
        }
        return master.state.range(current.near(), current.far());
    }

    /**
     * A caixa {@code state} foi para o grupo {@code group} (0 = sai da conexão e continua tocando o que
     * estava). Se o grupo já está tocando, a caixa passa a tocar o mesmo que ele; senão, ela é quem
     * decide o que o grupo toca (o estado dela vira o mestre quando {@link #publish} for chamado).
     */
    public static PlaybackState join(PlaybackState state, int group) {
        int target = Math.max(0, Math.min(PlaybackState.MAX_GROUP, group));
        if (target == 0) {
            return state.withGroup(0);
        }
        Master master = freshMaster(target, System.currentTimeMillis());
        if (master != null && master.state.playing()) {
            return master.state.range(state.near(), state.far());
        }
        return state.withGroup(target);
    }

    /**
     * O estado novo de uma caixa conectada passa a valer para o grupo todo. Chamar depois de guardar o
     * estado na caixa que mudou. Não faz nada se a caixa não está conectada.
     */
    public static void publish(PlaybackState state, MinecraftServer server) {
        if (state.group() == 0) {
            return;
        }
        MASTERS.put(state.group(), new Master(state, System.currentTimeMillis()));
        if (server != null) {
            // As portáteis e as dos animais/barcos copiam já, sem esperar a próxima varredura.
            for (ServerWorld world : server.getWorlds()) {
                PortableTracker.update(world);
            }
        }
    }
}
