package com.edugcs.batraquizar.mixin;

import com.edugcs.batraquizar.Batraquizar;
import com.edugcs.batraquizar.BatraquizadoEffect;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** O jogador tem tamanho e altura dos olhos próprios (não usa os das outras criaturas): encolhe os dois. */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void batraquizar$frogSize(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (Batraquizar.isFrog((PlayerEntity) (Object) this)) {
            cir.setReturnValue(cir.getReturnValue().scaled(BatraquizadoEffect.SCALE));
        }
    }

    @Inject(method = "getActiveEyeHeight", at = @At("RETURN"), cancellable = true)
    private void batraquizar$frogEyes(EntityPose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        if (Batraquizar.isFrog((PlayerEntity) (Object) this)) {
            cir.setReturnValue(cir.getReturnValue() * BatraquizadoEffect.SCALE);
        }
    }
}
