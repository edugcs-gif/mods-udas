package com.edugcs.batraquizar;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** O efeito "Bênção do Príncipe". Ele mesmo não faz nada: as regras estão em {@link BencaoDoPrincipe}. */
public class BencaoDoPrincipeEffect extends StatusEffect {
    private static final int COLOR = 0xF5C542;

    public BencaoDoPrincipeEffect() {
        super(StatusEffectCategory.BENEFICIAL, COLOR);
    }
}
