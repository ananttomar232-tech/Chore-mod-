package com.limitlessanant.heist.block;

import com.limitlessanant.heist.heist.HeistAlarm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Mission 1 target. A 1.8 x 2.6 block framed painting that hangs on a wall (provided "mona_lisa" model).
 * Sneak + right-click (or break it) to steal it - which sets off the museum alarm.
 */
public class MonaLisaBlock extends MeshPropBlock {
    public MonaLisaBlock(Settings settings) {
        super(settings, Block.createCuboidShape(0, 0, 14.5, 16, 16, 16), true);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.isSneaking()) return ActionResult.PASS;
        if (world instanceof ServerWorld sw) {
            world.removeBlock(pos, false);
            ItemStack stolen = new ItemStack(this);
            if (!player.giveItemStack(stolen)) {
                world.spawnEntity(new ItemEntity(world, player.getX(), player.getY(), player.getZ(), stolen));
            }
            world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.BLOCKS, 1f, 0.7f);
            HeistAlarm.trigger(sw, player, "The Mona Lisa has been stolen!");
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        if (world instanceof ServerWorld sw) HeistAlarm.trigger(sw, player, "The Mona Lisa has been stolen!");
    }
}
