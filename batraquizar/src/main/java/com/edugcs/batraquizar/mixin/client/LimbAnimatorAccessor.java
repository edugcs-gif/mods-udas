package com.edugcs.batraquizar.mixin.client;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** O balanço das pernas, para o sapo de mentira andar no mesmo ritmo de quem ele representa. */
@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {
    @Accessor("prevSpeed")
    float batraquizar$getPrevSpeed();

    @Accessor("prevSpeed")
    void batraquizar$setPrevSpeed(float value);

    @Accessor("speed")
    float batraquizar$getSpeed();

    @Accessor("speed")
    void batraquizar$setSpeed(float value);

    @Accessor("pos")
    float batraquizar$getPos();

    @Accessor("pos")
    void batraquizar$setPos(float value);
}
