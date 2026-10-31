package com.edugcs.batraquizar.mixin;

import com.edugcs.batraquizar.Retorsao;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A poção de Dano instantâneo (arremessável ou persistente) não vira um efeito na criatura: ela age na hora,
 * por aqui. A Retorsão também a devolve.
 */
@Mixin(StatusEffect.class)
public abstract class StatusEffectMixin {
    @Inject(method = "applyInstantEffect", at = @At("HEAD"), cancellable = true)
    private void batraquizar$retorsao(Entity source, Entity attacker, LivingEntity target, int amplifier,
                                      double proximity, CallbackInfo ci) {
        StatusEffect self = (StatusEffect) (Object) this;
        LivingEntity reflectTo = Retorsao.reflectTarget(target, attacker, self);
        if (reflectTo != null) {
            Retorsao.reflect(target, reflectTo, () -> self.applyInstantEffect(source, target, reflectTo, amplifier,
                    proximity));
            ci.cancel();
        }
    }
}
