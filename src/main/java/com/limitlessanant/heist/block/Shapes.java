package com.limitlessanant.heist.block;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

/** Rotate voxel shapes authored for a block facing NORTH. */
public final class Shapes {
    public static VoxelShape rotate(VoxelShape north, Direction facing) {
        if (facing == Direction.NORTH) return north;
        VoxelShape[] out = {VoxelShapes.empty()};
        for (Box b : north.getBoundingBoxes()) {
            double x1 = b.minX, z1 = b.minZ, x2 = b.maxX, z2 = b.maxZ;
            Box r = switch (facing) {
                case SOUTH -> new Box(1 - x2, b.minY, 1 - z2, 1 - x1, b.maxY, 1 - z1);
                case EAST -> new Box(1 - z2, b.minY, x1, 1 - z1, b.maxY, x2);
                case WEST -> new Box(z1, b.minY, 1 - x2, z2, b.maxY, 1 - x1);
                default -> b;
            };
            out[0] = VoxelShapes.union(out[0], VoxelShapes.cuboid(r));
        }
        return out[0];
    }

    /** Model rotation (degrees around +Y) for a mesh whose front faces -Z, to make it face {@code facing}. */
    public static float yRotation(Direction facing) {
        return 180f - facing.asRotation();
    }

    private Shapes() {
    }
}
