package com.edugcs.caixademusica.block;

import com.edugcs.caixademusica.CaixaDeMusica;
import com.edugcs.caixademusica.PlaybackState;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Guarda o que a caixa está tocando (veja {@link PlaybackState}).
 *
 * <p>O servidor só guarda o link, a música inicial da playlist e o horário em que começou.
 * Cada jogador baixa e toca o áudio no próprio PC, pulando para o ponto certo, assim
 * todo mundo escuta (quase) a mesma parte da música ao mesmo tempo.
 */
public class MusicBoxBlockEntity extends BlockEntity {
    private PlaybackState state = PlaybackState.BLOCK_DEFAULT;

    /** Só no cliente: o mesmo instante de início, convertido para o relógio deste PC. */
    private long clientStartMs;

    public MusicBoxBlockEntity(BlockPos pos, BlockState blockState) {
        super(CaixaDeMusica.MUSIC_BOX_BLOCK_ENTITY, pos, blockState);
    }

    // ---------------------------------------------------------------- servidor

    /** Troca o estado e avisa os jogadores por perto (se mudou alguma coisa). */
    public void setState(PlaybackState newState) {
        if (newState.equals(state)) {
            return;
        }
        this.state = newState;
        markDirty();
        if (world != null && !world.isClient) {
            BlockState blockState = getCachedState();
            world.updateListeners(pos, blockState, blockState, Block.NOTIFY_LISTENERS);
        }
    }

    // ---------------------------------------------------------------- NBT / sincronização

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("Url", state.url());
        nbt.putBoolean("Playing", state.playing());
        nbt.putBoolean("Paused", state.paused());
        nbt.putInt("StartIndex", state.startIndex());
        nbt.putLong("StartedAt", state.startedAt());
        nbt.putLong("PausedElapsed", state.pausedElapsed());
        nbt.putInt("Near", state.near());
        nbt.putInt("Far", state.far());
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        // Caixas salvas por versões antigas não têm "Paused", "Near" etc.: ficam com os valores padrão.
        PlaybackState defaults = PlaybackState.BLOCK_DEFAULT;
        state = new PlaybackState(
                nbt.getString("Url"),
                nbt.getBoolean("Playing"),
                nbt.getBoolean("Paused"),
                nbt.getInt("StartIndex"),
                nbt.getLong("StartedAt"),
                nbt.getLong("PausedElapsed"),
                nbt.contains("Near") ? nbt.getInt("Near") : defaults.near(),
                nbt.contains("Far") ? nbt.getInt("Far") : defaults.far());
        if (nbt.contains("Elapsed")) {
            // Só vem nos pacotes para o cliente: quanto tempo já passou desde o início.
            clientStartMs = System.currentTimeMillis() - nbt.getLong("Elapsed");
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        NbtCompound nbt = createNbt(registries);
        nbt.putLong("Elapsed", Math.max(0L, System.currentTimeMillis() - state.startedAt()));
        return nbt;
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (world != null && world.isClient) {
            CaixaDeMusica.bridge.removed(pos);
        }
    }

    public static void clientTick(World world, BlockPos pos, BlockState blockState, MusicBoxBlockEntity box) {
        CaixaDeMusica.bridge.clientTick(box);
    }

    // ---------------------------------------------------------------- getters

    public PlaybackState getState() {
        return state;
    }

    public long getClientStartMs() {
        return clientStartMs;
    }
}
