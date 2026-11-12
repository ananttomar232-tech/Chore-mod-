package com.limitlessanant.heist.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Thin glowing neon tube (like an end rod but square). Placed along the axis of the clicked face - draw neon lines on buildings. */
public class NeonStripBlock extends PillarBlock {
    private static final VoxelShape Y = Block.createCuboidShape(6.5, 0, 6.5, 9.5, 16, 9.5);
    private static final VoxelShape X = Block.createCuboidShape(0, 6.5, 6.5, 16, 9.5, 9.5);
    private static final VoxelShape Z = Block.createCuboidShape(6.5, 6.5, 0, 9.5, 9.5, 16);

    public NeonStripBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(AXIS)) {
            case X -> X;
            case Z -> Z;
            default -> Y;
        };
    }
}
