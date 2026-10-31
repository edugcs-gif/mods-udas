package com.edugcs.batraquizar.mixin;

import com.edugcs.batraquizar.Batraquizar;
import com.edugcs.batraquizar.BencaoDoPrincipe;
import com.edugcs.batraquizar.Retorsao;
import com.edugcs.batraquizar.BatraquizadoEffect;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Os ganchos em qualquer criatura: o corpo de sapo (tamanho, pulo e queda) e as regras da Bênção do Príncipe e da
 * Retorsão nos efeitos.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** A bênção guardada enquanto o totem da imortalidade limpa os efeitos. */
    @Unique
    private StatusEffectInstance batraquizar$savedBlessing;

    /** Sapo é pequeno (a altura dos olhos vem do tamanho, menos no jogador: veja {@link PlayerEntityMixin}). */
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void batraquizar$frogSize(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (Batraquizar.isFrog((LivingEntity) (Object) this)) {
            cir.setReturnValue(cir.getReturnValue().scaled(BatraquizadoEffect.SCALE));
        }
    }

    /** Sapo pula mais alto. */
    @Inject(method = "getJumpVelocity", at = @At("RETURN"), cancellable = true)
    private void batraquizar$frogJump(CallbackInfoReturnable<Float> cir) {
        if (Batraquizar.isFrog((LivingEntity) (Object) this)) {
            cir.setReturnValue(cir.getReturnValue() + BatraquizadoEffect.EXTRA_JUMP);
        }
    }

    /** Sapo não toma dano de queda. */
    @Inject(method = "computeFallDamage", at = @At("HEAD"), cancellable = true)
    private void batraquizar$frogFall(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Integer> cir) {
        if (Batraquizar.isFrog((LivingEntity) (Object) this)) {
            cir.setReturnValue(0);
        }
    }

    /** Retorsão: o efeito ruim volta para quem o aplicou (se ele pegaria nesta criatura). */
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void batraquizar$retorsao(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        LivingEntity attacker = Retorsao.reflectTarget(self, source, effect.getEffectType());
        if (attacker != null && self.canHaveStatusEffect(effect)) {
            StatusEffectInstance copy = new StatusEffectInstance(effect);
            Retorsao.reflect(self, attacker, () -> attacker.addStatusEffect(copy, self));
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canHaveStatusEffect", at = @At("HEAD"), cancellable = true)
    private void batraquizar$blessingProtects(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (BencaoDoPrincipe.blocks((LivingEntity) (Object) this, effect)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onStatusEffectRemoved", at = @At("TAIL"))
    private void batraquizar$turnedBack(StatusEffectInstance effect, CallbackInfo ci) {
        BencaoDoPrincipe.onEffectRemoved((LivingEntity) (Object) this, effect);
    }

    @Inject(method = "tryUseTotem", at = @At("HEAD"))
    private void batraquizar$saveBlessing(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        StatusEffectInstance blessing = ((LivingEntity) (Object) this).getStatusEffect(Batraquizar.BENCAO_DO_PRINCIPE);
        batraquizar$savedBlessing = blessing == null ? null : new StatusEffectInstance(blessing);
    }

    @Inject(method = "tryUseTotem", at = @At("RETURN"))
    private void batraquizar$restoreBlessing(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        StatusEffectInstance saved = batraquizar$savedBlessing;
        batraquizar$savedBlessing = null;
        LivingEntity self = (LivingEntity) (Object) this;
        if (saved != null && Boolean.TRUE.equals(cir.getReturnValue())
                && !self.hasStatusEffect(Batraquizar.BENCAO_DO_PRINCIPE)) {
            self.addStatusEffect(saved);
        }
    }
}
