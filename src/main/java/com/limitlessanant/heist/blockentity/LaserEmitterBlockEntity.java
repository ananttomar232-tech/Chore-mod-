package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.block.LaserEmitterBlock;
import com.limitlessanant.heist.entity.VipGuardEntity;
import com.limitlessanant.heist.heist.HeistAlarm;
import com.limitlessanant.heist.heist.LaserRegistry;
import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.AbstractGlassBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class LaserEmitterBlockEntity extends SyncedBlockEntity {
    public static final int MAX_LENGTH = 24;
    public static final float DAMAGE = 6f;
    private static final DustParticleEffect RED = new DustParticleEffect(new Vector3f(1f, 0.05f, 0.05f), 0.6f);
    private long disabledUntil = 0;
    private float length = 0;
    private boolean registered;

    public LaserEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_EMITTER, pos, state);
    }

    public void disableFor(int ticks) {
        if (world == null) return;
        disabledUntil = world.getTime() + ticks;
        sync();
    }

    public float getLength() {
        return length;
    }

    public boolean isActive() {
        if (world == null) return false;
        BlockState s = getCachedState();
        if (!(s.getBlock() instanceof LaserEmitterBlock) || s.get(LaserEmitterBlock.POWERED)) return false;
        long t = world.getTime();
        if (t < disabledUntil) return false;
        return !s.get(LaserEmitterBlock.PULSE) || (t % 60) < 40;
    }

    /** Distance from the emitter's centre to the first block that stops the beam (glass lets it through). */
    private float computeLength(World w, Direction d) {
        BlockPos.Mutable m = pos.mutableCopy();
        for (int i = 1; i <= MAX_LENGTH; i++) {
            m.move(d);
            BlockState s = w.getBlockState(m);
            if (s.isAir() || s.getBlock() instanceof AbstractGlassBlock || s.getBlock() instanceof PaneBlock) continue;
            if (!s.getCollisionShape(w, m).isEmpty()) return i - 0.5f;
        }
        return MAX_LENGTH;
    }

    public static void tick(World world, BlockPos pos, BlockState state, LaserEmitterBlockEntity be) {
        Direction d = state.get(LaserEmitterBlock.FACING);
        if (!be.registered && !world.isClient) {
            LaserRegistry.add(world, pos);
            be.registered = true;
        }
        if (world.getTime() % 4 == 0 || be.length == 0) be.length = be.computeLength(world, d);
        if (!be.isActive()) return;
        Vec3d start = Vec3d.ofCenter(pos);
        Vec3d end = start.add(Vec3d.of(d.getVector()).multiply(be.length));
        if (world.isClient) {
            if (world.random.nextInt(3) == 0) {
                double f = world.random.nextDouble();
                Vec3d p = start.lerp(end, f);
                world.addParticle(RED, p.x, p.y, p.z, 0, 0, 0);
            }
            return;
        }
        Box beam = new Box(start, end).expand(0.12);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, beam, LaserEmitterBlockEntity::canBurn)) {
            if (e.hurtTime > 0) continue;
            e.damage(world.getDamageSources().magic(), DAMAGE);
            e.setOnFireFor(3);
            world.playSound(null, e.getBlockPos(), SoundEvents.ENTITY_GENERIC_BURN, SoundCategory.BLOCKS, 0.8f, 1.6f);
            if (e instanceof PlayerEntity p && world instanceof ServerWorld sw) HeistAlarm.trigger(sw, p, "Laser grid tripped!");
        }
    }

    private static boolean canBurn(LivingEntity e) {
        if (!e.isAlive() || e.isSpectator() || e instanceof VipGuardEntity) return false;
        return !(e instanceof PlayerEntity p) || !p.getAbilities().creativeMode;
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (world != null && !world.isClient) LaserRegistry.remove(world, pos);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putLong("DisabledUntil", disabledUntil);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        disabledUntil = nbt.getLong("DisabledUntil");
    }
}
