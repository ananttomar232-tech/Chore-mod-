package com.limitlessanant.heist.effect;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

/** Marker effect: VIP golem bodyguards target players that have it. Given by {@link com.limitlessanant.heist.heist.HeistAlarm}. */
public class WantedEffect extends StatusEffect {
    public WantedEffect() {
        super(StatusEffectCategory.HARMFUL, 0xff1a1a);
    }
}
