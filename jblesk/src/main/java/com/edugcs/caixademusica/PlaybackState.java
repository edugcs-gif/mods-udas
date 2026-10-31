package com.edugcs.caixademusica;

import java.util.concurrent.TimeUnit;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

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
 * @param near  até esta distância (blocos) o volume é total
 * @param far   a partir desta distância (blocos) não se ouve mais; entre as duas o volume cai
 * @param group grupo de conexão (0 = desconectada): caixas com o mesmo grupo tocam a mesma música
 *              (veja {@link SpeakerGroups})
 */
public record PlaybackState(String url, boolean playing, boolean paused, int startIndex, long startedAt,
                            long pausedElapsed, int near, int far, int group) {
    public static final int MAX_INDEX = 1_000_000;
    public static final int MIN_FAR = 4;
    public static final int MAX_FAR = 128;
    /** Maior número de grupo de conexão (0 = sem conexão). */
    public static final int MAX_GROUP = 99;
    private static final long MAX_SEEK_MS = TimeUnit.HOURS.toMillis(12);

    public static final PlaybackState BLOCK_DEFAULT = new PlaybackState("", false, false, 0, 0L, 0L, 8, 64, 0);
    public static final PlaybackState PORTABLE_DEFAULT = new PlaybackState("", false, false, 0, 0L, 0L, 4, 32, 0);
    /** A caixa embutida no baú de um burro, mula, lhama ou barco: alcance entre o do portátil e o do bloco. */
    public static final PlaybackState ENTITY_DEFAULT = new PlaybackState("", false, false, 0, 0L, 0L, 6, 48, 0);

    /** Onde o estado da caixa portátil fica guardado no item (NBT). */
    private static final String ITEM_KEY = "CaixaDeMusica";

    public PlaybackState {
        url = url == null ? "" : url;
        startIndex = Math.max(-MAX_INDEX, Math.min(MAX_INDEX, startIndex));
        pausedElapsed = Math.max(0L, pausedElapsed);
        far = Math.max(MIN_FAR, Math.min(MAX_FAR, far));
        near = Math.max(0, Math.min(far, near));
        group = Math.max(0, Math.min(MAX_GROUP, group));
    }

    // ---------------------------------------------------------------- NBT (bloco, item e entidade) e rede

    public void writeNbt(NbtCompound nbt) {
        nbt.putString("Url", url);
        nbt.putBoolean("Playing", playing);
        nbt.putBoolean("Paused", paused);
        nbt.putInt("StartIndex", startIndex);
        nbt.putLong("StartedAt", startedAt);
        nbt.putLong("PausedElapsed", pausedElapsed);
        nbt.putInt("Near", near);
        nbt.putInt("Far", far);
        nbt.putInt("Group", group);
    }

    /** Lê o estado; o que faltar (salvo por versões antigas, por exemplo) fica com o de {@code defaults}. */
    public static PlaybackState fromNbt(NbtCompound nbt, PlaybackState defaults) {
        return new PlaybackState(
                nbt.getString("Url"),
                nbt.getBoolean("Playing"),
                nbt.getBoolean("Paused"),
                nbt.getInt("StartIndex"),
                nbt.getLong("StartedAt"),
                nbt.getLong("PausedElapsed"),
                nbt.contains("Near") ? nbt.getInt("Near") : defaults.near(),
                nbt.contains("Far") ? nbt.getInt("Far") : defaults.far(),
                nbt.getInt("Group")); // caixas salvas antes da conexão não têm "Group": 0 = desconectada
    }

    /** O estado guardado na caixa portátil, ou {@code null} se ela nunca tocou nada. */
    public static PlaybackState ofStack(ItemStack stack) {
        NbtCompound nbt = stack.getSubNbt(ITEM_KEY);
        return nbt == null ? null : fromNbt(nbt, PORTABLE_DEFAULT);
    }

    public void writeTo(ItemStack stack) {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt);
        stack.setSubNbt(ITEM_KEY, nbt);
    }

    /** Vai pela rede em 6 campos: os booleanos, o alcance e o grupo viajam juntos. */
    public void write(PacketByteBuf buf) {
        buf.writeString(url, CaixaDeMusica.MAX_URL_LENGTH);
        buf.writeVarInt(flags());
        buf.writeVarInt(startIndex);
        buf.writeVarLong(startedAt);
        buf.writeVarLong(pausedElapsed);
        buf.writeVarInt(rangeBits());
    }

    public static PlaybackState read(PacketByteBuf buf) {
        return fromPacket(buf.readString(CaixaDeMusica.MAX_URL_LENGTH), buf.readVarInt(), buf.readVarInt(),
                buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
    }

    private int flags() {
        return (playing ? 1 : 0) | (paused ? 2 : 0);
    }

    /** Empacota o alcance e o grupo: far nos 8 bits de baixo, near nos 8 seguintes e o grupo no resto. */
    private int rangeBits() {
        return (group << 16) | ((near & 0xFF) << 8) | (far & 0xFF);
    }

    private static PlaybackState fromPacket(String url, int flags, int startIndex, long startedAt,
                                            long pausedElapsed, int range) {
        return new PlaybackState(url, (flags & 1) != 0, (flags & 2) != 0, startIndex, startedAt, pausedElapsed,
                (range >>> 8) & 0xFF, range & 0xFF, range >>> 16);
    }

    /** Tem música para sair do alto-falante agora (tocando, não pausada e com link). */
    public boolean audible() {
        return playing && !paused && !url.isBlank();
    }

    // ---------------------------------------------------------------- ações (devolvem o novo estado)

    public PlaybackState play(String newUrl, long now) {
        return new PlaybackState(newUrl, true, false, 0, now, 0L, near, far, group);
    }

    public PlaybackState stop(long now) {
        return new PlaybackState(url, false, false, 0, now, 0L, near, far, group);
    }

    /** Pula para a música {@code index} da playlist (o cliente já manda o número certo). */
    public PlaybackState jump(int index, long now) {
        if (!playing || url.isEmpty()) {
            return this;
        }
        return new PlaybackState(url, true, false, index, now, 0L, near, far, group);
    }

    public PlaybackState pause(long now) {
        if (!playing || paused) {
            return this;
        }
        return new PlaybackState(url, true, true, startIndex, startedAt, Math.max(0L, now - startedAt), near, far, group);
    }

    public PlaybackState resume(long now) {
        if (!playing || !paused) {
            return this;
        }
        return new PlaybackState(url, true, false, startIndex, now - pausedElapsed, 0L, near, far, group);
    }

    /** Avança ({@code deltaMs} &gt; 0) ou volta ({@code deltaMs} &lt; 0) dentro da música atual. */
    public PlaybackState seek(long deltaMs, long now) {
        if (!playing || url.isEmpty()) {
            return this;
        }
        long delta = Math.max(-MAX_SEEK_MS, Math.min(MAX_SEEK_MS, deltaMs));
        if (paused) {
            return new PlaybackState(url, true, true, startIndex, startedAt, pausedElapsed + delta, near, far, group);
        }
        return new PlaybackState(url, true, false, startIndex, Math.min(now, startedAt - delta), 0L, near, far, group);
    }

    public PlaybackState range(int newNear, int newFar) {
        return new PlaybackState(url, playing, paused, startIndex, startedAt, pausedElapsed, newNear, newFar, group);
    }

    /** Muda o grupo de conexão (0 = desconectada); o resto fica igual. */
    public PlaybackState withGroup(int newGroup) {
        return new PlaybackState(url, playing, paused, startIndex, startedAt, pausedElapsed, near, far, newGroup);
    }

    /** Toca exatamente a mesma coisa, no mesmo ponto (ignora o alcance e o grupo, que são de cada caixa). */
    public boolean samePlayback(PlaybackState other) {
        return playing == other.playing && paused == other.paused && startIndex == other.startIndex
                && startedAt == other.startedAt && pausedElapsed == other.pausedElapsed && url.equals(other.url);
    }
}
