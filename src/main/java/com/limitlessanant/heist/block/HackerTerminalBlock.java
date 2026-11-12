package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.HackerTerminalBlockEntity;
import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Hacker's Den terminal (provided "Hacker computer terminal setup" model, ~3.6 x 2.7 blocks, best placed in a room corner).
 * Right-click: "HACKING IN PROGRESS" for 5 s, then every laser grid within 48 blocks shuts down for 60 s.
 */
public class HackerTerminalBlock extends MeshPropBlock {
    public HackerTerminalBlock(Settings settings) {
        super(settings, Block.createCuboidShape(0, 0, 0, 16, 16, 16));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof HackerTerminalBlockEntity be) {
            if (!world.isClient) be.startHack(player);
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new HackerTerminalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : checkType(type, ModBlockEntities.HACKER_TERMINAL, HackerTerminalBlockEntity::serverTick);
    }
}
