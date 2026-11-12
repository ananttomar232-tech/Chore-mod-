package com.limitlessanant.heist.world;

import net.minecraft.block.BlockState;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;

/**
 * A rotated sub-frame on a {@link Canvas}: "local" coordinates where the front faces -Z (north), mapped so the
 * front faces {@link #facing}. Block states are rotated too (stairs, signs, props ...).
 */
public final class Local {
    public final Canvas c;
    public final int ox, oy, oz;
    public final Direction facing;
    public final BlockRotation rotation;

    public Local(Canvas c, int ox, int oy, int oz, Direction facing) {
        this.c = c;
        this.ox = ox;
        this.oy = oy;
        this.oz = oz;
        this.facing = facing;
        this.rotation = switch (facing) {
            case SOUTH -> BlockRotation.CLOCKWISE_180;
            case EAST -> BlockRotation.CLOCKWISE_90;
            case WEST -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    public int x(int lx, int lz) {
        return ox + switch (facing) {
            case SOUTH -> -lx;
            case EAST -> -lz;
            case WEST -> lz;
            default -> lx;
        };
    }

    public int z(int lx, int lz) {
        return oz + switch (facing) {
            case SOUTH -> -lz;
            case EAST -> lx;
            case WEST -> -lx;
            default -> lz;
        };
    }

    public Direction dir(Direction localDir) {
        return rotation.rotate(localDir);
    }

    public void set(int lx, int ly, int lz, BlockState s) {
        c.set(x(lx, lz), oy + ly, z(lx, lz), s.rotate(rotation));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        BlockState r = s.rotate(rotation);
        c.fill(x(x1, z1), oy + y1, z(x1, z1), x(x2, z2), oy + y2, z(x2, z2), r);
    }

    public void neonWall(int lx, int ly, int lz, Direction localFacing, String design, float scale) {
        c.neonWall(x(lx, lz), oy + ly, z(lx, lz), dir(localFacing), design, scale);
    }
}
