package com.limitlessanant.heist.registry;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.blockentity.DragonCoreBlockEntity;
import com.limitlessanant.heist.blockentity.HackerTerminalBlockEntity;
import com.limitlessanant.heist.blockentity.LaserEmitterBlockEntity;
import com.limitlessanant.heist.blockentity.MeshPropBlockEntity;
import com.limitlessanant.heist.blockentity.MysteryBoxBlockEntity;
import com.limitlessanant.heist.blockentity.NeonSignBlockEntity;
import com.limitlessanant.heist.blockentity.WeaponRackBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {
    public static final BlockEntityType<MeshPropBlockEntity> MESH_PROP = register("mesh_prop",
            FabricBlockEntityTypeBuilder.create(MeshPropBlockEntity::new, ModBlocks.BRIEFCASE, ModBlocks.MONA_LISA).build());
    public static final BlockEntityType<HackerTerminalBlockEntity> HACKER_TERMINAL = register("hacker_terminal",
            FabricBlockEntityTypeBuilder.create(HackerTerminalBlockEntity::new, ModBlocks.HACKER_TERMINAL).build());
    public static final BlockEntityType<WeaponRackBlockEntity> WEAPON_RACK = register("weapon_rack",
            FabricBlockEntityTypeBuilder.create(WeaponRackBlockEntity::new, ModBlocks.WEAPON_RACK).build());
    public static final BlockEntityType<NeonSignBlockEntity> NEON_SIGN = register("neon_sign",
            FabricBlockEntityTypeBuilder.create(NeonSignBlockEntity::new, ModBlocks.NEON_SIGN).build());
    public static final BlockEntityType<MysteryBoxBlockEntity> MYSTERY_BOX = register("mystery_box",
            FabricBlockEntityTypeBuilder.create(MysteryBoxBlockEntity::new, ModBlocks.MYSTERY_BOX).build());
    public static final BlockEntityType<DragonCoreBlockEntity> DRAGON_CORE = register("infinity_dragon_core",
            FabricBlockEntityTypeBuilder.create(DragonCoreBlockEntity::new, ModBlocks.INFINITY_DRAGON_CORE).build());
    public static final BlockEntityType<LaserEmitterBlockEntity> LASER_EMITTER = register("laser_emitter",
            FabricBlockEntityTypeBuilder.create(LaserEmitterBlockEntity::new, ModBlocks.LASER_EMITTER).build());

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, LimitlessHeist.id(name), type);
    }

    public static void init() {
    }

    private ModBlockEntities() {
    }
}
