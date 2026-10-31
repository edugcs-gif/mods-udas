package com.edugcs.batraquizar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * O efeito "Sapo". Tudo o que muda no corpo são modificadores de atributo, que o próprio jogo coloca
 * quando o efeito começa e tira quando acaba (ou quando a pessoa bebe leite ou morre):
 * <ul>
 *   <li>tamanho 30% do normal: o jogador fica com ~0,5 bloco de altura, como um sapo;</li>
 *   <li>pulo mais forte (uns 2 blocos);</li>
 *   <li>cai de até 3 blocos a mais sem se machucar.</li>
 * </ul>
 */
public class FrogEffect extends StatusEffect {
    /** O cliente reconhece quem é sapo por este modificador de tamanho (veja {@link Batraquizar#isFrog}). */
    public static final Identifier SCALE_ID = Batraquizar.id("frog_scale");

    private static final int COLOR = 0x5BA03A;

    public FrogEffect() {
        super(StatusEffectCategory.HARMFUL, COLOR);
        addAttributeModifier(EntityAttributes.GENERIC_SCALE, SCALE_ID, -0.7,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(EntityAttributes.GENERIC_JUMP_STRENGTH, Batraquizar.id("frog_jump"), 0.15,
                EntityAttributeModifier.Operation.ADD_VALUE);
        addAttributeModifier(EntityAttributes.GENERIC_SAFE_FALL_DISTANCE, Batraquizar.id("frog_fall"), 3.0,
                EntityAttributeModifier.Operation.ADD_VALUE);
    }

    /** Na hora em que vira sapo: uma nuvem de gosma verde e um "croac" para todo mundo perto. */
    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        if (entity.getWorld() instanceof ServerWorld world) {
            double width = entity.getWidth();
            double height = entity.getHeight();
            world.spawnParticles(ParticleTypes.ITEM_SLIME, entity.getX(), entity.getY() + height / 2, entity.getZ(),
                    30, width / 2, height / 2, width / 2, 0.05);
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_FROG_AMBIENT,
                    entity.getSoundCategory(), 1.0F, 1.0F);
        }
    }
}
