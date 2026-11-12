package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.WeaponRackBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Weapon Cartel wall rack (provided "weapon_rack" model, 3 x 1.85 blocks). Holds 3 display items (rifles, freeze ray, ...):
 * right-click with an item to hang it, empty hand to take the last one back.
 */
public class WeaponRackBlock extends MeshPropBlock {
    public WeaponRackBlock(Settings settings) {
        super(settings, Block.createCuboidShape(0, 0, 13, 16, 16, 16), true);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack)) return ActionResult.PASS;
        ItemStack held = player.getStackInHand(hand);
        if (!held.isEmpty()) {
            if (rack.insert(held.copyWithCount(1))) {
                if (!player.getAbilities().creativeMode) held.decrement(1);
                return ActionResult.success(world.isClient);
            }
            return ActionResult.CONSUME;
        }
        ItemStack out = rack.removeLast();
        if (!out.isEmpty()) {
            if (!world.isClient) player.giveItemStack(out);
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) {
            ItemScatterer.spawn(world, pos, rack.getItems());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponRackBlockEntity(pos, state);
    }
}
