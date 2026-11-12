package com.limitlessanant.heist.entity;

import com.limitlessanant.heist.effect.FrozenEffect;
import com.limitlessanant.heist.registry.ModEffects;
import com.limitlessanant.heist.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Freeze Ray projectile (Mission 3). Hit entity: Slowness X + FROZEN for 10 s -> ice statue.
 * Hit block: water around turns into ice, lava into obsidian.
 */
public class FreezeBoltEntity extends ThrownEntity {
    public static final int FREEZE_TICKS = 200;

    public FreezeBoltEntity(EntityType<? extends ThrownEntity> type, World world) {
        super(type, world);
    }

    public FreezeBoltEntity(World world, LivingEntity owner) {
        super(ModEntities.FREEZE_BOLT, owner, world);
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected float getGravity() {
        return 0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) {
            for (int i = 0; i < 2; i++) {
                getWorld().addParticle(ParticleTypes.SNOWFLAKE, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + (random.nextDouble() - 0.5) * 0.3,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
            if (random.nextInt(3) == 0) getWorld().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        } else if (age > 60) {
            discard();
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        super.onEntityHit(hit);
        if (hit.getEntity() instanceof LivingEntity e && !getWorld().isClient) freeze(e);
    }

    public static void freeze(LivingEntity e) {
        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FREEZE_TICKS, 9)); // Slowness X
        e.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, FREEZE_TICKS, 4));
        e.addStatusEffect(new StatusEffectInstance(ModEffects.FROZEN, FREEZE_TICKS, 0));
        e.setFrozenTicks(FrozenEffect.FROZEN_TICKS);
        e.damage(e.getWorld().getDamageSources().freeze(), 2f);
        if (e.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.ICE.getDefaultState()),
                    e.getX(), e.getBodyY(0.5), e.getZ(), 60, e.getWidth() * 0.5, e.getHeight() * 0.4, e.getWidth() * 0.5, 0.1);
            sw.spawnParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getBodyY(0.5), e.getZ(), 40, 0.4, 0.6, 0.4, 0.05);
            sw.playSound(null, e.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1f, 1.4f);
            sw.playSound(null, e.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1f, 0.8f);
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if (getWorld().isClient) return;
        BlockPos c = hit.getBlockPos().offset(hit.getSide());
        for (BlockPos p : BlockPos.iterate(c.add(-2, -2, -2), c.add(2, 2, 2))) {
            if (p.getSquaredDistance(c) > 6.5) continue;
            FluidState f = getWorld().getFluidState(p);
            BlockState s = getWorld().getBlockState(p);
            if (f.isOf(Fluids.WATER) && s.isOf(Blocks.WATER)) getWorld().setBlockState(p, Blocks.ICE.getDefaultState());
            else if (f.isIn(FluidTags.LAVA) && s.isOf(Blocks.LAVA)) getWorld().setBlockState(p, f.isStill() ? Blocks.OBSIDIAN.getDefaultState() : Blocks.COBBLESTONE.getDefaultState());
        }
        ((ServerWorld) getWorld()).spawnParticles(ParticleTypes.SNOWFLAKE, getX(), getY(), getZ(), 30, 0.3, 0.3, 0.3, 0.08);
        getWorld().playSound(null, getBlockPos(), SoundEvents.BLOCK_POWDER_SNOW_PLACE, SoundCategory.PLAYERS, 1f, 1.2f);
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!getWorld().isClient) discard();
    }
}
