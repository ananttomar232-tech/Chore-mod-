package com.limitlessanant.heist.entity;

import com.limitlessanant.heist.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Grappling hook. Flies straight (very little drop); when it bites into a block it reels its owner in at high speed -
 * fly over the museum laser grid! Sneak, use the gun again, or arrive to let go. Hitting a mob yanks the mob to you.
 * Physics run on the server; the player's velocity is pushed to the client every tick.
 */
public class GrappleHookEntity extends ProjectileEntity {
    private static final TrackedData<Boolean> HOOKED = DataTracker.registerData(GrappleHookEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    public static final double MAX_RANGE = 48;
    public static final double PULL_SPEED = 1.5;

    public GrappleHookEntity(EntityType<? extends ProjectileEntity> type, World world) {
        super(type, world);
    }

    public GrappleHookEntity(World world, PlayerEntity owner) {
        this(ModEntities.GRAPPLE_HOOK, world);
        setOwner(owner);
        setPosition(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(HOOKED, false);
    }

    public boolean isHooked() {
        return dataTracker.get(HOOKED);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        Entity owner = getOwner();
        if (!(owner instanceof PlayerEntity p) || !p.isAlive() || p.getWorld() != getWorld()) {
            discard();
            return;
        }
        if (!isHooked()) {
            if (age > 40 || squaredDistanceTo(p) > MAX_RANGE * MAX_RANGE) {
                discard();
                return;
            }
            HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
            if (hit.getType() != HitResult.Type.MISS) {
                onCollision(hit);
                if (isRemoved()) return;
            } else {
                Vec3d v = getVelocity();
                setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
                setVelocity(v.multiply(0.99).add(0, -0.02, 0));
            }
            return;
        }
        // ---- reel the player in
        Vec3d target = getPos();
        Vec3d from = p.getPos().add(0, p.getHeight() * 0.5, 0);
        Vec3d d = target.subtract(from);
        double dist = d.length();
        if (dist < 1.6 || p.isSneaking() || age > 20 * 8) {
            if (dist < 1.6) p.setVelocity(p.getVelocity().multiply(0.2).add(0, 0.35, 0)); // pop up onto the ledge
            p.velocityModified = true;
            discard();
            return;
        }
        Vec3d pull = d.normalize().multiply(Math.min(PULL_SPEED, 0.45 + dist * 0.12));
        p.setVelocity(pull);
        p.velocityModified = true;
        p.fallDistance = 0;
        if (age % 4 == 0) getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 0.5f, 1.6f);
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        Vec3d pos = hit.getPos();
        setPosition(pos.x, pos.y, pos.z);
        setVelocity(Vec3d.ZERO);
        dataTracker.set(HOOKED, true);
        getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1.2f, 0.8f);
        getWorld().playSound(null, getBlockPos(), SoundEvents.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 1f, 1.2f);
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (hit.getEntity() instanceof LivingEntity e && getOwner() != null) {
            Vec3d yank = getOwner().getPos().subtract(e.getPos()).normalize().multiply(1.4).add(0, 0.4, 0);
            e.setVelocity(yank);
            e.velocityModified = true;
            getWorld().playSound(null, e.getBlockPos(), SoundEvents.ITEM_CROSSBOW_HIT, SoundCategory.PLAYERS, 1f, 1f);
        }
        discard();
    }

    @Override
    protected boolean canHit(Entity entity) {
        return super.canHit(entity) && entity != getOwner();
    }

    @Override
    public boolean shouldRender(double distance) {
        return distance < 128 * 128;
    }
}
