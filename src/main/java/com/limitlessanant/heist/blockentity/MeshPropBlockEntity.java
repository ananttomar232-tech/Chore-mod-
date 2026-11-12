package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/** Data-less block entity: exists so the client can draw the mesh (briefcase, Mona Lisa). */
public class MeshPropBlockEntity extends BlockEntity {
    public MeshPropBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MESH_PROP, pos, state);
    }
}
