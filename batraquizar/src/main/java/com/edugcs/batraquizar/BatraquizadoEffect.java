package com.edugcs.batraquizar;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * O efeito "Batraquizado". Quando acaba (ou a pessoa bebe leite ou morre), o jogador ganha a
 * {@link BencaoDoPrincipe Bênção do Príncipe}. Enquanto dura:
 * <ul>
 *   <li>tamanho 30% do normal: o jogador fica com ~0,5 bloco de altura, como um sapo ({@link #SCALE});</li>
 *   <li>40% mais rápido (como a poção de Velocidade II);</li>
 *   <li>pulo mais forte (uns 2 blocos, {@link #EXTRA_JUMP});</li>
 *   <li>não toma dano de queda.</li>
 * </ul>
 * A velocidade é um modificador de atributo, que o próprio jogo coloca e tira. O resto (o Minecraft 1.20.1 não
 * tem atributos de tamanho, pulo e queda) fica nos mixins, que perguntam {@link Batraquizar#isFrog}.
 */
public class BatraquizadoEffect extends StatusEffect {
    /** O cliente reconhece quem é sapo por este modificador de velocidade (veja {@link Batraquizar#isFrog}). */
    public static final UUID SPEED_ID = UUID.fromString("6f3c9a2e-5b1d-4c8e-9a7f-2d4b8e1c0a53");
    /** Quanto do tamanho normal sobra. */
    public static final float SCALE = 0.3F;
    /** Quanto a mais de impulso no pulo (o normal é 0,42). */
    public static final float EXTRA_JUMP = 0.15F;

    private static final int COLOR = 0x5BA03A;

    public BatraquizadoEffect() {
        super(StatusEffectCategory.HARMFUL, COLOR);
        addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID.toString(), 0.4,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /** Na hora em que vira sapo: encolhe, solta uma nuvem de gosma verde e um "croac" para todo mundo perto. */
    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        double width = entity.getWidth();
        double height = entity.getHeight();
        super.onApplied(entity, attributes, amplifier);
        entity.calculateDimensions();
        if (entity.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ITEM_SLIME, entity.getX(), entity.getY() + height / 2, entity.getZ(),
                    30, width / 2, height / 2, width / 2, 0.05);
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_FROG_AMBIENT,
                    entity.getSoundCategory(), 1.0F, 1.0F);
        }
    }

    /** Voltou ao normal: volta ao tamanho de antes. */
    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(entity, attributes, amplifier);
        entity.calculateDimensions();
    }
}
