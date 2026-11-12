package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.DragonCoreBlockEntity;
import com.limitlessanant.heist.heist.HeistAlarm;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Mission 3: The Infinity Dragon Core. Floats, spins and glows (rendered by DragonCoreRenderer).
 * Right-click toggles its sky beam. Taking it from the vault (breaking it in survival) triggers the alarm.
 */
public class DragonCoreBlock extends BlockWithEntity {
    private static final VoxelShape SHAPE = Block.createCuboidShape(3, 2, 3, 13, 16, 13);

    public DragonCoreBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof DragonCoreBlockEntity core) {
            if (!world.isClient) core.toggleBeam();
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        if (world instanceof ServerWorld sw) HeistAlarm.trigger(sw, player, "The Infinity Dragon Core is gone!");
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DragonCoreBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? checkType(type, ModBlockEntities.DRAGON_CORE, DragonCoreBlockEntity::clientTick) : null;
    }
}
