package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Only exists for the spinning hologram "?" renderer. */
public class MysteryBoxBlockEntity extends BlockEntity {
    public MysteryBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MYSTERY_BOX, pos, state);
    }
}
