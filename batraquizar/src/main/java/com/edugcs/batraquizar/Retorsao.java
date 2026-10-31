package com.edugcs.batraquizar;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * A Retorsão: enquanto o efeito durar, todo efeito ruim que outra criatura tentar aplicar (poção arremessável,
 * nuvem, flecha, dano instantâneo...) volta para quem aplicou. Cada efeito devolvido custa 1 minuto da
 * Retorsão, então a normal (1 minuto) devolve um e a prolongada (3 minutos) devolve no máximo três.
 *
 * <p>Se quem aplicou também estiver com Retorsão, o efeito volta de novo, e assim vai (ping-pong) até a
 * Retorsão de um dos dois acabar: aí o efeito pega nele.
 *
 * <p>Cada devolução toca um som e manda um raio de partículas azuis do alvo até quem atacou. No ping-pong, os
 * raios saem um depois do outro, para dar para ver o efeito indo e voltando.
 */
public final class Retorsao {
    /** Cada efeito devolvido custa 1 minuto (em ticks). A poção normal dura isso; a prolongada, o triplo. */
    public static final int COST = 60 * 20;
    /** Limite de idas e voltas, para o caso de alguém ter Retorsão infinita (comando /effect). */
    private static final int MAX_BOUNCES = 16;
    /** Velocidade do raio de partículas, em blocos por tick. */
    private static final double BOLT_SPEED = 1.0;
    private static final int BOLT_MAX_TICKS = 100;
    private static final DustParticleEffect BLUE = new DustParticleEffect(new Vector3f(0.2F, 0.55F, 1.0F), 1.3F);

    private static final List<Bolt> BOLTS = new ArrayList<>();
    /** Quantas devoluções estão acontecendo uma dentro da outra agora (o ping-pong). */
    private static int depth;

    private Retorsao() {
    }

    static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tickBolts());
    }

    /**
     * Para quem devolver o efeito que {@code source} está tentando aplicar em {@code target}: quem aplicou, se for um
     * efeito ruim e o alvo estiver com Retorsão; senão {@code null} (o efeito pega normalmente).
     */
    public static LivingEntity reflectTarget(LivingEntity target, Entity source, StatusEffect effect) {
        if (effect.getCategory() != StatusEffectCategory.HARMFUL || target.getWorld().isClient() || depth >= MAX_BOUNCES
                || !(source instanceof LivingEntity attacker) || attacker == target || !attacker.isAlive()
                || !target.hasStatusEffect(Batraquizar.RETORSAO)) {
            return null;
        }
        return attacker;
    }

    /** Devolve o efeito: gasta 1 minuto da Retorsão do alvo, solta o raio e aplica em quem atacou ({@code apply}). */
    public static void reflect(LivingEntity target, LivingEntity attacker, Runnable apply) {
        spend(target);
        launch(target, attacker, depth);
        depth++;
        try {
            apply.run();
        } finally {
            depth--;
        }
    }

    private static void spend(LivingEntity entity) {
        StatusEffectInstance current = entity.getStatusEffect(Batraquizar.RETORSAO);
        if (current == null || current.isInfinite()) {
            return;
        }
        int left = current.getDuration() - COST;
        entity.removeStatusEffect(Batraquizar.RETORSAO);
        if (left > 0) {
            entity.addStatusEffect(new StatusEffectInstance(Batraquizar.RETORSAO, left, current.getAmplifier(),
                    current.isAmbient(), current.shouldShowParticles(), current.shouldShowIcon()));
        }
    }

    // ------------------------------------------------------------------ som e partículas

    private static void launch(LivingEntity from, LivingEntity to, int order) {
        if (!(from.getWorld() instanceof ServerWorld world) || to.getWorld() != world) {
            return;
        }
        Vec3d start = center(from);
        int travelTicks = Math.max(1, MathHelper.ceil(start.distanceTo(center(to)) / BOLT_SPEED));
        BOLTS.add(new Bolt(world, start, to, order * travelTicks));
    }

    private static void tickBolts() {
        if (BOLTS.isEmpty()) {
            return;
        }
        Iterator<Bolt> it = BOLTS.iterator();
        while (it.hasNext()) {
            Bolt bolt = it.next();
            if (bolt.delay > 0) {
                bolt.delay--;
                continue;
            }
            if (bolt.age == 0) {
                bolt.world.playSound(null, bolt.pos.x, bolt.pos.y, bolt.pos.z, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                        SoundCategory.PLAYERS, 1.0F, 1.2F);
                burst(bolt.world, bolt.pos);
            }
            if (bolt.to.isRemoved() || bolt.to.getWorld() != bolt.world || ++bolt.age > BOLT_MAX_TICKS) {
                it.remove();
                continue;
            }
            Vec3d target = center(bolt.to);
            Vec3d toTarget = target.subtract(bolt.pos);
            double distance = toTarget.length();
            Vec3d next = distance <= BOLT_SPEED ? target : bolt.pos.add(toTarget.multiply(BOLT_SPEED / distance));
            trail(bolt.world, bolt.pos, next);
            bolt.pos = next;
            if (distance <= BOLT_SPEED) {
                burst(bolt.world, target);
                it.remove();
            }
        }
    }

    private static Vec3d center(LivingEntity entity) {
        return new Vec3d(entity.getX(), entity.getBodyY(0.5), entity.getZ());
    }

    /** O rastro do raio: um pontinho azul a cada 0,2 bloco do trecho percorrido neste tick. */
    private static void trail(ServerWorld world, Vec3d from, Vec3d to) {
        Vec3d delta = to.subtract(from);
        int points = Math.max(1, MathHelper.ceil(delta.length() / 0.2));
        for (int i = 1; i <= points; i++) {
            Vec3d p = from.add(delta.multiply(i / (double) points));
            world.spawnParticles(BLUE, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private static void burst(ServerWorld world, Vec3d at) {
        world.spawnParticles(BLUE, at.x, at.y, at.z, 12, 0.25, 0.25, 0.25, 0.0);
    }

    /** Um raio voando até {@code to}; espera {@code delay} ticks antes de sair (para o ping-pong ir em ordem). */
    private static final class Bolt {
        final ServerWorld world;
        final LivingEntity to;
        Vec3d pos;
        int delay;
        int age;

        Bolt(ServerWorld world, Vec3d pos, LivingEntity to, int delay) {
            this.world = world;
            this.pos = pos;
            this.to = to;
            this.delay = delay;
        }
    }
}
