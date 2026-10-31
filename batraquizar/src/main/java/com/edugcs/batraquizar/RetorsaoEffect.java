package com.edugcs.batraquizar;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** O efeito "Retorsão". Ele mesmo não faz nada: as regras estão em {@link Retorsao}. */
public class RetorsaoEffect extends StatusEffect {
    private static final int COLOR = 0x3FA9F5;

    public RetorsaoEffect() {
        super(StatusEffectCategory.BENEFICIAL, COLOR);
    }
}
