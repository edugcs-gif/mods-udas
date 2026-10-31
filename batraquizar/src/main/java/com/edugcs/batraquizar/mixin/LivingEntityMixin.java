package com.edugcs.batraquizar.mixin;

import com.edugcs.batraquizar.Batraquizar;
import com.edugcs.batraquizar.BencaoDoPrincipe;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** As regras da Bênção do Príncipe que precisam de ganchos nos efeitos de qualquer criatura. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /** A bênção guardada enquanto o totem da imortalidade limpa os efeitos. */
    @Unique
    private StatusEffectInstance batraquizar$savedBlessing;

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
