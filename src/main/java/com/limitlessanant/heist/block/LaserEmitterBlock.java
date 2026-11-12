package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.LaserEmitterBlockEntity;
import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Laser Grid Security. Place on a wall / floor / ceiling: it shoots a red laser (up to 24 blocks) in the direction it faces
 * until a solid block stops it. Touching the beam instantly hurts (3 hearts + fire) and triggers the heist alarm.
 * <ul>
 *     <li>redstone power: OFF</li>
 *     <li>right-click: toggle PULSE mode (on 2 s / off 1 s - time your run!)</li>
 *     <li>Hacker terminal / Hacker USB: shuts every laser nearby down for a while</li>
 * </ul>
 * Build grids by stacking emitters on two facing walls (each emitter = one beam), or use /heist laser_grid.
 */
public class LaserEmitterBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;
    public static final BooleanProperty PULSE = BooleanProperty.of("pulse");
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction d : Direction.values()) {
            // emitter body sits against the block it is attached to (opposite of FACING)
            double a = 5, b = 11, t = 4;
            SHAPES[d.getId()] = switch (d) {
                case UP -> Block.createCuboidShape(a, 0, a, b, t, b);
                case DOWN -> Block.createCuboidShape(a, 16 - t, a, b, 16, b);
                case NORTH -> Block.createCuboidShape(a, a, 16 - t, b, b, 16);
                case SOUTH -> Block.createCuboidShape(a, a, 0, b, b, t);
                case WEST -> Block.createCuboidShape(16 - t, a, a, 16, b, b);
                case EAST -> Block.createCuboidShape(0, a, a, t, b, b);
            };
        }
    }

    public LaserEmitterBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.UP).with(POWERED, false).with(PULSE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, PULSE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getSide())
                .with(POWERED, ctx.getWorld().isReceivingRedstonePower(ctx.getBlockPos()));
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        boolean p = world.isReceivingRedstonePower(pos);
        if (p != state.get(POWERED)) world.setBlockState(pos, state.with(POWERED, p), Block.NOTIFY_ALL);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
        if (!world.isClient) {
            BlockState n = state.cycle(PULSE);
            world.setBlockState(pos, n, Block.NOTIFY_ALL);
            world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6f, 1.8f);
            player.sendMessage(Text.literal("Laser mode: " + (n.get(PULSE) ? "PULSE" : "STATIC")).formatted(Formatting.RED), true);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPES[state.get(FACING).getId()];
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
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
        return new LaserEmitterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.LASER_EMITTER, LaserEmitterBlockEntity::tick);
    }
}
