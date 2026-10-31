package com.edugcs.caixademusica.client;

import com.edugcs.caixademusica.CaixaDeMusica;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * O som "da caixa": toca numa posição (parado no bloco, ou acompanhando o jogador que segura a
 * caixa portátil) e usa a categoria de Jukebox/Blocos de Nota, então o volume geral é controlado
 * por esse controle nas opções de som.
 *
 * <p>A queda do volume com a distância é calculada aqui, a cada tick: volume total até o alcance
 * "perto" da caixa, diminuindo até zero no alcance "longe". Por cima disso entra o volume pessoal
 * do jogador ({@link ClientSettings}).
 */
final class MusicBoxSoundInstance extends AbstractSoundInstance implements TickableSoundInstance, FabricSoundInstance {
    private final MusicBoxSession session;
    private final Supplier<Vec3d> position;
    private final MusicBoxAudioStream stream;
    private boolean done;

    /**
     * @param ownedByListener o som é da caixa portátil que o próprio jogador segura: toca "colado" na
     *                        cabeça dele, sem direção. Uma fonte posicionada em cima do ouvinte e atualizada
     *                        só a cada tick (20x por segundo) balança de lado a cada passo ou giro de câmera.
     */
    MusicBoxSoundInstance(MusicBoxSession session, Supplier<Vec3d> position, boolean ownedByListener,
                          MusicBoxAudioStream stream) {
        super(CaixaDeMusica.id("music_box_stream"), SoundCategory.RECORDS, SoundInstance.createRandom());
        this.session = session;
        this.position = position;
        this.stream = stream;
        this.pitch = 1.0F;
        this.repeat = false;
        this.repeatDelay = 0;
        this.relative = ownedByListener;
        // Sem atenuação do Minecraft: a distância é tratada em update().
        this.attenuationType = SoundInstance.AttenuationType.NONE;
        update();
    }

    private void update() {
        Vec3d source = position.get();
        if (source == null) {
            done = true;
            return;
        }
        double falloff;
        if (relative) {
            // Posição relativa ao ouvinte: (0, 0, 0) é a própria cabeça, e quem segura ouve sempre no volume total.
            this.x = 0.0;
            this.y = 0.0;
            this.z = 0.0;
            falloff = 1.0;
        } else {
            this.x = source.x;
            this.y = source.y;
            this.z = source.z;
            Vec3d listener = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
            double distance = listener.distanceTo(source);
            int near = session.near();
            int far = session.far();
            falloff = distance <= near ? 1.0 : distance >= far ? 0.0 : 1.0 - (distance - near) / (far - near);
        }
        // O Minecraft limita o volume a 100%; o que passar disso é amplificação no áudio (MusicBoxAudioStream).
        this.volume = (float) (falloff * Math.min(1.0F, ClientSettings.gain()));
    }

    @Override
    public void tick() {
        update();
    }

    @Override
    public boolean isDone() {
        return done;
    }

    /** Toca mesmo com volume zero (longe da caixa ou mudo), senão o Minecraft nem começaria o som. */
    @Override
    public boolean shouldAlwaysPlay() {
        return true;
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader, Identifier id, boolean repeatInstantly) {
        return CompletableFuture.completedFuture(stream);
    }
}
