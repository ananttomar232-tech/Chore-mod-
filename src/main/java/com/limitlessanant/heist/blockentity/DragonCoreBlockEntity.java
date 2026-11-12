package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class DragonCoreBlockEntity extends SyncedBlockEntity {
    private boolean beam = true;

    public DragonCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAGON_CORE, pos, state);
    }

    public boolean hasBeam() {
        return beam;
    }

    public void toggleBeam() {
        beam = !beam;
        sync();
    }

    public static void clientTick(World world, BlockPos pos, BlockState state, DragonCoreBlockEntity be) {
        Random r = world.random;
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.75, cz = pos.getZ() + 0.5;
        if (r.nextInt(2) == 0) {
            double a = r.nextDouble() * Math.PI * 2, d = 0.9 + r.nextDouble() * 0.6;
            // particles get sucked into the core
            world.addParticle(ParticleTypes.PORTAL, cx + Math.cos(a) * d, cy + (r.nextDouble() - 0.5), cz + Math.sin(a) * d,
                    -Math.cos(a) * d, 0, -Math.sin(a) * d);
        }
        if (r.nextInt(4) == 0) {
            world.addParticle(new DustColorTransitionParticleEffect(new Vector3f(0.75f, 0.2f, 1f), new Vector3f(1f, 0.9f, 0.3f), 1.2f),
                    cx + (r.nextDouble() - 0.5) * 1.2, cy + (r.nextDouble() - 0.5), cz + (r.nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
        }
        if (r.nextInt(10) == 0) world.addParticle(ParticleTypes.END_ROD, cx, cy, cz, (r.nextDouble() - 0.5) * 0.1, 0.05, (r.nextDouble() - 0.5) * 0.1);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("Beam", beam);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        beam = !nbt.contains("Beam") || nbt.getBoolean("Beam");
    }
}
