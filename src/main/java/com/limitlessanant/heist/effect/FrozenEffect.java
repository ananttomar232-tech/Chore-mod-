package com.limitlessanant.heist.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

/**
 * "Ice statue": the entity cannot move or jump and its freeze counter is kept maxed so every client renders the
 * ice block around it (see FrozenIceFeatureRenderer) and the vanilla freezing shiver.
 * The freeze ray also applies vanilla Slowness X on top of this.
 */
public class FrozenEffect extends StatusEffect {
    public static final int FROZEN_TICKS = 300;

    public FrozenEffect() {
        super(StatusEffectCategory.HARMFUL, 0x9fe8ff);
        addAttributeModifier(EntityAttributes.GENERIC_MOVEMENT_SPEED, "6b4f7c5e-3f3c-4b8a-9a52-7f0b2a1a9e01", -1.0,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(EntityAttributes.GENERIC_ATTACK_SPEED, "6b4f7c5e-3f3c-4b8a-9a52-7f0b2a1a9e02", -1.0,
                EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean canApplyUpdateEffect(int duration, int amplifier) {
        return true;
    }

    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        entity.setFrozenTicks(Math.max(entity.getFrozenTicks(), FROZEN_TICKS));
        Vec3d v = entity.getVelocity();
        entity.setVelocity(0, Math.min(v.y, 0), 0);
        if (entity instanceof MobEntity mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(entity, attributes, amplifier);
        entity.setFrozenTicks(0);
    }
}
