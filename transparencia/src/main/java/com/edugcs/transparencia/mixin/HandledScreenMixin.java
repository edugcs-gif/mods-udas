package com.edugcs.transparencia.mixin;

import com.edugcs.transparencia.Transparencia;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Só o fundo da tela (o drawBackground) sai transparente; itens, textos e botões vêm depois, normais. */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V"))
    private void transparencia$beforeBackground(DrawContext context, int mouseX, int mouseY, float delta,
                                                CallbackInfo ci) {
        Transparencia.beginBackground(context);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V",
            shift = At.Shift.AFTER))
    private void transparencia$afterBackground(DrawContext context, int mouseX, int mouseY, float delta,
                                               CallbackInfo ci) {
        Transparencia.endBackground(context);
    }
}
