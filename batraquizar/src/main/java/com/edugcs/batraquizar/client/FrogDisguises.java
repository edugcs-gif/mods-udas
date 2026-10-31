package com.edugcs.batraquizar.client;

import com.edugcs.batraquizar.Batraquizar;
import com.edugcs.batraquizar.mixin.client.LimbAnimatorAccessor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.FrogEntity;
import net.minecraft.entity.passive.FrogVariant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

/**
 * Desenha quem está transformado em sapo como um sapo de verdade.
 *
 * <p>Cada criatura transformada ganha um sapo "de mentira", que nunca entra no mundo: só serve para
 * o desenho do sapo do jogo. A cada frame ele copia a posição, o giro do corpo e da cabeça, o
 * balanço das pernas, o piscar vermelho de dano e a animação de morte de quem ele representa.
 */
public final class FrogDisguises {
    private static final List<RegistryKey<FrogVariant>> VARIANTS =
            List.of(FrogVariant.TEMPERATE, FrogVariant.WARM, FrogVariant.COLD);
    /** Some o sapo de mentira de quem não foi desenhado por este tanto de ticks (saiu da tela ou de perto). */
    private static final int FORGET_AFTER_TICKS = 100;
    /** Duração da animação de coaxar do sapo (3 segundos). */
    private static final int CROAK_TICKS = 60;
    /** Em média, um "croac" a cada 10 segundos. */
    private static final int CROAK_CHANCE = 200;

    private static final Map<Integer, Disguise> DISGUISES = new HashMap<>();
    private static final Random RANDOM = Random.create();
    private static ClientWorld lastWorld;

    private FrogDisguises() {
    }

    /**
     * Chamado no lugar do desenho normal de qualquer criatura. Devolve {@code true} se desenhou um sapo
     * (aí o desenho normal é cancelado).
     */
    public static boolean render(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, int light) {
        if (entity instanceof FrogEntity || !Batraquizar.isFrog(entity)) {
            return false;
        }
        Disguise disguise = DISGUISES.get(entity.getId());
        if (disguise == null || disguise.source != entity) {
            FrogEntity frog = EntityType.FROG.create(entity.getWorld());
            if (frog == null) {
                return false;
            }
            frog.setVariant(Registries.FROG_VARIANT.entryOf(
                    VARIANTS.get(Math.floorMod(entity.getUuid().hashCode(), VARIANTS.size()))));
            disguise = new Disguise(entity, frog);
            DISGUISES.put(entity.getId(), disguise);
            updateName(disguise);
        }
        disguise.idleTicks = 0;
        copyPose(entity, disguise.frog);
        MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(disguise.frog)
                .render(disguise.frog, yaw, tickDelta, matrices, vertexConsumers, light);
        return true;
    }

    private static void copyPose(LivingEntity source, FrogEntity frog) {
        frog.setPos(source.getX(), source.getY(), source.getZ());
        frog.age = source.age;
        frog.bodyYaw = source.bodyYaw;
        frog.prevBodyYaw = source.prevBodyYaw;
        frog.headYaw = source.headYaw;
        frog.prevHeadYaw = source.prevHeadYaw;
        frog.setPitch(source.getPitch());
        frog.prevPitch = source.prevPitch;
        frog.hurtTime = source.hurtTime;
        frog.deathTime = source.deathTime;
        frog.setInvisible(source.isInvisible());
        frog.setOnGround(source.isOnGround());

        LimbAnimatorAccessor from = (LimbAnimatorAccessor) source.limbAnimator;
        LimbAnimatorAccessor to = (LimbAnimatorAccessor) frog.limbAnimator;
        to.batraquizar$setPrevSpeed(from.batraquizar$getPrevSpeed());
        to.batraquizar$setSpeed(from.batraquizar$getSpeed());
        to.batraquizar$setPos(from.batraquizar$getPos());
    }

    /** O nome em cima do sapo: o do jogador (menos o seu, como no jogo normal), ou o nome dado à criatura. */
    private static void updateName(Disguise disguise) {
        LivingEntity source = disguise.source;
        boolean player = source instanceof PlayerEntity;
        boolean visible = source != MinecraftClient.getInstance().getCameraEntity()
                && (player || source.isCustomNameVisible());
        disguise.frog.setCustomName(player || source.hasCustomName() ? source.getDisplayName() : null);
        disguise.frog.setCustomNameVisible(visible && !source.isSneaky());
    }

    /** A cada tick do cliente: coaxa de vez em quando, e esquece (com uma nuvem de fumaça) quem desvirou. */
    static void tick(MinecraftClient client) {
        if (client.world != lastWorld) {
            DISGUISES.clear();
            lastWorld = client.world;
        }
        if (DISGUISES.isEmpty() || client.world == null) {
            return;
        }
        Iterator<Disguise> it = DISGUISES.values().iterator();
        while (it.hasNext()) {
            Disguise disguise = it.next();
            LivingEntity source = disguise.source;
            if (source.isRemoved() || ++disguise.idleTicks > FORGET_AFTER_TICKS) {
                it.remove();
                continue;
            }
            if (!Batraquizar.isFrog(source)) {
                turnBack(client.world, source);
                it.remove();
                continue;
            }
            updateName(disguise);
            croak(client.world, disguise);
        }
    }

    private static void croak(ClientWorld world, Disguise disguise) {
        FrogEntity frog = disguise.frog;
        if (disguise.croakTicks > 0) {
            if (--disguise.croakTicks == 0) {
                frog.croakingAnimationState.stop();
            }
            return;
        }
        LivingEntity source = disguise.source;
        if (source.isAlive() && RANDOM.nextInt(CROAK_CHANCE) == 0) {
            disguise.croakTicks = CROAK_TICKS;
            frog.croakingAnimationState.start(source.age);
            world.playSound(source.getX(), source.getY(), source.getZ(), SoundEvents.ENTITY_FROG_AMBIENT,
                    source.getSoundCategory(), 1.0F, 0.9F + RANDOM.nextFloat() * 0.2F, false);
        }
    }

    /** Voltou ao normal: uma nuvem de fumaça no lugar do sapo. */
    private static void turnBack(ClientWorld world, LivingEntity source) {
        for (int i = 0; i < 20; i++) {
            world.addParticle(ParticleTypes.POOF,
                    source.getParticleX(0.6), source.getRandomBodyY(), source.getParticleZ(0.6),
                    RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02, RANDOM.nextGaussian() * 0.02);
        }
    }

    private static final class Disguise {
        final LivingEntity source;
        final FrogEntity frog;
        int idleTicks;
        int croakTicks;

        Disguise(LivingEntity source, FrogEntity frog) {
            this.source = source;
            this.frog = frog;
        }
    }
}
