package com.edugcs.transparencia.mixin;

import com.edugcs.transparencia.Transparencia;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** O escurecimento atrás das telas com inventário fica mais leve (as outras telas, como o menu de pausa, não mudam). */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @ModifyArg(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fillGradient(IIIIII)V"), index = 4)
    private int transparencia$dimTop(int color) {
        return Transparencia.dimBackground((Screen) (Object) this, color);
    }

    @ModifyArg(method = "renderBackground(Lnet/minecraft/client/gui/DrawContext;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fillGradient(IIIIII)V"), index = 5)
    private int transparencia$dimBottom(int color) {
        return Transparencia.dimBackground((Screen) (Object) this, color);
    }
}
