package com.limitlessanant.heist.registry;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.entity.FreezeBoltEntity;
import com.limitlessanant.heist.entity.GotiyaEntity;
import com.limitlessanant.heist.entity.GrappleHookEntity;
import com.limitlessanant.heist.entity.UnicornEntity;
import com.limitlessanant.heist.entity.VipGuardEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
    public static final EntityType<VipGuardEntity> VIP_GUARD = register("vip_guard",
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, VipGuardEntity::new)
                    .dimensions(EntityDimensions.fixed(1.4f, 2.7f)).trackRangeBlocks(10 * 16).build());
    public static final EntityType<UnicornEntity> UNICORN = register("unicorn",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, UnicornEntity::new)
                    .dimensions(EntityDimensions.fixed(1.6f, 2.4f)).trackRangeBlocks(10 * 16).build());
    public static final EntityType<GotiyaEntity> GOTIYA = register("gotiya",
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, GotiyaEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f)).trackRangeBlocks(10 * 16).build());
    public static final EntityType<FreezeBoltEntity> FREEZE_BOLT = register("freeze_bolt",
            FabricEntityTypeBuilder.<FreezeBoltEntity>create(SpawnGroup.MISC, FreezeBoltEntity::new)
                    .dimensions(EntityDimensions.fixed(0.35f, 0.35f)).trackRangeBlocks(128).trackedUpdateRate(1).build());
    public static final EntityType<GrappleHookEntity> GRAPPLE_HOOK = register("grapple_hook",
            FabricEntityTypeBuilder.<GrappleHookEntity>create(SpawnGroup.MISC, GrappleHookEntity::new)
                    .dimensions(EntityDimensions.fixed(0.3f, 0.3f)).trackRangeBlocks(128).trackedUpdateRate(1)
                    .forceTrackedVelocityUpdates(true).build());

    private static <T extends Entity> EntityType<T> register(String name, EntityType<T> type) {
        return Registry.register(Registries.ENTITY_TYPE, LimitlessHeist.id(name), type);
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(VIP_GUARD, VipGuardEntity.createVipAttributes());
        FabricDefaultAttributeRegistry.register(UNICORN, UnicornEntity.createUnicornAttributes());
        FabricDefaultAttributeRegistry.register(GOTIYA, VillagerEntity.createVillagerAttributes());
        ModItems.registerSpawnEggs();
    }

    private ModEntities() {
    }
}
