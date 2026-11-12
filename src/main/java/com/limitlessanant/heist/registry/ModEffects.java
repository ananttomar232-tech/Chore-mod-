package com.limitlessanant.heist.registry;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.effect.FrozenEffect;
import com.limitlessanant.heist.effect.WantedEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEffects {
    /** Freeze-ray hit: cannot move, rendered as an ice statue. */
    public static final StatusEffect FROZEN = Registry.register(Registries.STATUS_EFFECT, LimitlessHeist.id("frozen"), new FrozenEffect());
    /** Set off an alarm: every VIP golem bodyguard nearby hunts you. */
    public static final StatusEffect WANTED = Registry.register(Registries.STATUS_EFFECT, LimitlessHeist.id("wanted"), new WantedEffect());

    public static void init() {
    }

    private ModEffects() {
    }
}
