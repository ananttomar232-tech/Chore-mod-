package com.limitlessanant.heist.mixin;

import com.limitlessanant.heist.item.LavaSuitArmorItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lava Proof Suit: replaces the sluggish vanilla lava physics (velocity x0.5 every tick) with fast, dolphin-like
 * swimming that follows where you look. Runs on the side that simulates the player (the client for the local player).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void limitlessheist$lavaSwim(Vec3d input, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof PlayerEntity p) || !self.isInLava() || p.getAbilities().flying) return;
        if (!self.isLogicalSideForUpdatingMovement() || !LavaSuitArmorItem.hasFullSuit(self)) return;
        float accel = p.isSprinting() ? 0.10f : 0.065f;
        self.updateVelocity(accel, input);
        Vec3d v = self.getVelocity();
        if (input.z > 0) { // swim where you look (up / down)
            double ly = self.getRotationVector().y;
            v = new Vec3d(v.x, v.y + (ly * (p.isSprinting() ? 0.55 : 0.35) - v.y) * 0.25, v.z);
        } else if (!self.hasNoGravity()) {
            v = v.add(0, -0.012, 0);
        }
        self.setVelocity(v);
        self.move(MovementType.SELF, self.getVelocity());
        self.setVelocity(self.getVelocity().multiply(0.86, 0.82, 0.86));
        self.updateLimbs(false);
        ci.cancel();
    }
}
