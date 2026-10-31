package com.edugcs.batraquizar.mixin.client;

import com.edugcs.batraquizar.Batraquizar;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sapo não tem mão: em primeira pessoa, nem o braço nem o item aparecem. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"), cancellable = true)
    private void batraquizar$hideHands(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
                                ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (Batraquizar.isFrog(player)) {
            ci.cancel();
        }
    }
}
