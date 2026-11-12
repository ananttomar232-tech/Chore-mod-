package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.MeshPropBlockEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

import java.util.EnumMap;
import java.util.Map;

/**
 * A decorative block whose visuals come from a converted 3D mesh (see client/mesh/HeistMesh) rendered by a block
 * entity renderer. The block model JSON only provides break particles. FACING = the direction the prop's front points.
 */
public class MeshPropBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
    private final boolean wallMounted;

    public MeshPropBlock(Settings settings, VoxelShape northShape) {
        this(settings, northShape, false);
    }

    public MeshPropBlock(Settings settings, VoxelShape northShape, boolean wallMounted) {
        super(settings);
        this.wallMounted = wallMounted;
        for (Direction d : Direction.Type.HORIZONTAL) shapes.put(d, Shapes.rotate(northShape, d));
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if (wallMounted && ctx.getSide().getAxis().isHorizontal()) {
            return getDefaultState().with(FACING, ctx.getSide());
        }
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    public boolean isWallMounted() {
        return wallMounted;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return shapes.get(state.get(FACING));
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE; // drawn by the block entity renderer
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MeshPropBlockEntity(pos, state);
    }
}
