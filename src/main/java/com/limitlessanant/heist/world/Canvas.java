package com.limitlessanant.heist.world;

import com.limitlessanant.heist.block.LaserEmitterBlock;
import com.limitlessanant.heist.block.MeshPropBlock;
import com.limitlessanant.heist.block.NeonDesigns;
import com.limitlessanant.heist.block.NeonSignBlock;
import com.limitlessanant.heist.blockentity.NeonSignBlockEntity;
import com.limitlessanant.heist.blockentity.WeaponRackBlockEntity;
import com.limitlessanant.heist.entity.VipGuardEntity;
import com.limitlessanant.heist.registry.ModBlocks;
import com.limitlessanant.heist.registry.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tiny building DSL used by the generators. Coordinates are relative to {@link #origin}. Nothing touches the world until
 * {@link #submit} hands the recorded ops to {@link StructureBuilder}, which places them over several ticks in order
 * (blocks first as recorded, so block entity / entity callbacks recorded later see their blocks).
 */
public class Canvas {
    /** Per-position block chooser; return null to leave the block alone. */
    public interface Pattern {
        BlockState at(int x, int y, int z);
    }

    public static final int FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;
    public final ServerWorld world;
    public final BlockPos origin;
    private final List<StructureBuilder.Op> ops = new ArrayList<>();

    public Canvas(ServerWorld world, BlockPos origin) {
        this.world = world;
        this.origin = origin.toImmutable();
    }

    public BlockPos abs(int x, int y, int z) {
        return origin.add(x, y, z);
    }

    // ------------------------------------------------------------------ blocks
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        fill(x1, y1, z1, x2, y2, z2, (x, y, z) -> s);
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Pattern p) {
        int ax = Math.min(x1, x2), ay = Math.min(y1, y2), az = Math.min(z1, z2);
        int bx = Math.max(x1, x2), by = Math.max(y1, y2), bz = Math.max(z1, z2);
        ops.add(new FillOp(origin, ax, ay, az, bx, by, bz, p));
    }

    public void set(int x, int y, int z, BlockState s) {
        fill(x, y, z, x, y, z, s);
    }

    /** Walls only (no floor / ceiling). */
    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, Pattern p) {
        fill(x1, y1, z1, x2, y2, z1, p);
        fill(x1, y1, z2, x2, y2, z2, p);
        fill(x1, y1, z1 + 1, x1, y2, z2 - 1, p);
        fill(x2, y1, z1 + 1, x2, y2, z2 - 1, p);
    }

    public void walls(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        walls(x1, y1, z1, x2, y2, z2, (x, y, z) -> s);
    }

    public void custom(StructureBuilder.Op op) {
        ops.add(op);
    }

    /** Run arbitrary code once the blocks before it are placed. */
    public void then(Consumer<ServerWorld> action) {
        ops.add((w, budget) -> {
            action.accept(w);
            return true;
        });
    }

    // ------------------------------------------------------------------ props & special blocks
    public void neon(int x, int y, int z, Direction facing, WallMountLocation face, String design, float scale, boolean flicker) {
        set(x, y, z, ModBlocks.NEON_SIGN.getDefaultState().with(NeonSignBlock.FACING, facing).with(NeonSignBlock.FACE, face));
        BlockPos p = abs(x, y, z);
        then(w -> {
            if (w.getBlockEntity(p) instanceof NeonSignBlockEntity be) {
                be.setDesign(NeonDesigns.indexOf(design));
                be.setScale(scale);
                be.setFlicker(flicker);
            }
        });
    }

    public void neonWall(int x, int y, int z, Direction facing, String design, float scale) {
        neon(x, y, z, facing, WallMountLocation.WALL, design, scale, false);
    }

    public void prop(int x, int y, int z, Block block, Direction facing) {
        set(x, y, z, block.getDefaultState().with(MeshPropBlock.FACING, facing));
    }

    public void weaponRack(int x, int y, int z, Direction facing, ItemStack... items) {
        prop(x, y, z, ModBlocks.WEAPON_RACK, facing);
        BlockPos p = abs(x, y, z);
        then(w -> {
            if (w.getBlockEntity(p) instanceof WeaponRackBlockEntity be) for (ItemStack s : items) be.insert(s.copy());
        });
    }

    public void laser(int x, int y, int z, Direction beam, boolean pulse) {
        set(x, y, z, ModBlocks.LASER_EMITTER.getDefaultState().with(LaserEmitterBlock.FACING, beam).with(LaserEmitterBlock.PULSE, pulse));
    }

    public void neonStrip(int x1, int y1, int z1, int x2, int y2, int z2, DyeColor c) {
        Direction.Axis axis = x1 != x2 ? Direction.Axis.X : (z1 != z2 ? Direction.Axis.Z : Direction.Axis.Y);
        fill(x1, y1, z1, x2, y2, z2, ModBlocks.NEON_STRIPS.get(c).getDefaultState().with(net.minecraft.block.PillarBlock.AXIS, axis));
    }

    public BlockState neonBlock(DyeColor c) {
        return ModBlocks.NEON_BLOCKS.get(c).getDefaultState();
    }

    public void chest(int x, int y, int z, Direction facing, ItemStack... items) {
        set(x, y, z, Blocks.CHEST.getDefaultState().with(net.minecraft.block.ChestBlock.FACING, facing));
        BlockPos p = abs(x, y, z);
        then(w -> {
            BlockEntity be = w.getBlockEntity(p);
            if (be instanceof ChestBlockEntity c) for (int i = 0; i < items.length && i < c.size(); i++) c.setStack(i, items[i].copy());
        });
    }

    // ------------------------------------------------------------------ entities
    public <E extends Entity> void entity(double x, double y, double z, float yaw, Function<ServerWorld, E> factory, Consumer<E> setup) {
        double ax = origin.getX() + x, ay = origin.getY() + y, az = origin.getZ() + z;
        then(w -> {
            E e = factory.apply(w);
            if (e == null) return;
            e.refreshPositionAndAngles(ax, ay, az, yaw, 0);
            e.setHeadYaw(yaw);
            if (e instanceof MobEntity m) {
                m.initialize(w, w.getLocalDifficulty(e.getBlockPos()), SpawnReason.STRUCTURE, null, null);
                m.setBodyYaw(yaw);
            }
            if (setup != null) setup.accept(e);
            w.spawnEntity(e);
        });
    }

    public void guard(int x, int y, int z, float yaw) {
        entity(x + 0.5, y, z + 0.5, yaw, w -> ModEntities.VIP_GUARD.create(w), (VipGuardEntity g) -> g.setPost(abs(x, y, z)));
    }

    public void submit(ServerCommandSource src, String name) {
        StructureBuilder.enqueue(world, ops, src, name);
    }

    // ------------------------------------------------------------------ ops
    static final class FillOp implements StructureBuilder.Op {
        private final BlockPos o;
        private final int ax, ay, az, sx, sy, sz;
        private final Pattern p;
        private long i;
        private final BlockPos.Mutable m = new BlockPos.Mutable();

        FillOp(BlockPos o, int ax, int ay, int az, int bx, int by, int bz, Pattern p) {
            this.o = o;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.sx = bx - ax + 1;
            this.sy = by - ay + 1;
            this.sz = bz - az + 1;
            this.p = p;
        }

        @Override
        public long size() {
            return (long) sx * sy * sz;
        }

        @Override
        public boolean work(ServerWorld w, int[] budget) {
            long n = size();
            while (i < n && budget[0] > 0) {
                int y = (int) (i / ((long) sx * sz)), r = (int) (i % ((long) sx * sz));
                int x = r % sx, z = r / sx;
                int wx = ax + x, wy = ay + y, wz = az + z;
                BlockState s = p.at(wx, wy, wz);
                if (s != null) {
                    m.set(o.getX() + wx, o.getY() + wy, o.getZ() + wz);
                    if (!w.isOutOfHeightLimit(m)) w.setBlockState(m, s, FLAGS);
                }
                i++;
                budget[0]--;
            }
            return i >= n;
        }
    }
}
