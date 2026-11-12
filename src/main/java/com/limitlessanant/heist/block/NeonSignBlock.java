package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.NeonSignBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
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
 * Neon sign. 34 designs: the 18 signs of the provided Japanese neon set translated to English + 16 Black Market designs.
 * <ul>
 *     <li>right-click: next design (sneak = previous)</li>
 *     <li>right-click with glowstone dust: toggle flicker</li>
 *     <li>right-click with an amethyst shard: change size (0.5x, 1x, 2x, 3x, 4x)</li>
 * </ul>
 * Can hang on walls, stand on the floor or hang from ceilings.
 */
public class NeonSignBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<WallMountLocation> FACE = Properties.WALL_MOUNT_LOCATION;
    private static final VoxelShape WALL_N = Block.createCuboidShape(2, 2, 14, 14, 14, 16);
    private static final VoxelShape FLOOR = Block.createCuboidShape(2, 0, 2, 14, 4, 14);
    private static final VoxelShape CEIL = Block.createCuboidShape(2, 12, 2, 14, 16, 14);

    public NeonSignBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(FACE, WallMountLocation.WALL));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        if (side.getAxis().isHorizontal()) return getDefaultState().with(FACE, WallMountLocation.WALL).with(FACING, side);
        Direction f = ctx.getHorizontalPlayerFacing().getOpposite();
        return getDefaultState().with(FACE, side == Direction.UP ? WallMountLocation.FLOOR : WallMountLocation.CEILING).with(FACING, f);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEIL;
            default -> Shapes.rotate(WALL_N, state.get(FACING));
        };
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof NeonSignBlockEntity sign)) return ActionResult.PASS;
        ItemStack held = player.getStackInHand(hand);
        if (!world.isClient) {
            if (held.isOf(Items.GLOWSTONE_DUST)) {
                sign.setFlicker(!sign.isFlicker());
                player.sendMessage(Text.literal("Flicker: " + (sign.isFlicker() ? "ON" : "OFF")).formatted(Formatting.LIGHT_PURPLE), true);
            } else if (held.isOf(Items.AMETHYST_SHARD)) {
                sign.cycleScale();
                player.sendMessage(Text.literal("Size: " + sign.getScale() + "x").formatted(Formatting.LIGHT_PURPLE), true);
            } else if (held.isEmpty() || held.isOf(asItem())) {
                sign.cycleDesign(player.isSneaking() ? -1 : 1);
                NeonDesigns.Design d = NeonDesigns.get(sign.getDesign());
                String from = d.translatedFrom() == null ? "" : "  (from " + d.translatedFrom() + ")";
                player.sendMessage(Text.literal("Neon: " + d.id().toUpperCase().replace('_', ' ') + from).formatted(Formatting.AQUA), true);
            } else {
                return ActionResult.PASS;
            }
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.8f, 1.6f);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new NeonSignBlockEntity(pos, state);
    }
}
