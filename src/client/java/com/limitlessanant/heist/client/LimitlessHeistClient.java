package com.limitlessanant.heist.client;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.client.mesh.MeshManager;
import com.limitlessanant.heist.client.model.LavaSuitModel;
import com.limitlessanant.heist.client.model.VipAccessoriesModel;
import com.limitlessanant.heist.client.render.FrozenIceFeatureRenderer;
import com.limitlessanant.heist.client.render.LavaSuitArmorRenderer;
import com.limitlessanant.heist.client.render.block.DragonCoreRenderer;
import com.limitlessanant.heist.client.render.block.LaserEmitterRenderer;
import com.limitlessanant.heist.client.render.block.MeshPropRenderer;
import com.limitlessanant.heist.client.render.block.MysteryBoxRenderer;
import com.limitlessanant.heist.client.render.block.NeonSignRenderer;
import com.limitlessanant.heist.client.render.block.WeaponRackRenderer;
import com.limitlessanant.heist.client.render.entity.FreezeBoltRenderer;
import com.limitlessanant.heist.client.render.entity.GrappleHookRenderer;
import com.limitlessanant.heist.client.render.entity.UnicornRenderer;
import com.limitlessanant.heist.client.render.entity.VipGuardRenderer;
import com.limitlessanant.heist.client.render.item.MeshItemRenderer;
import com.limitlessanant.heist.registry.ModBlockEntities;
import com.limitlessanant.heist.registry.ModBlocks;
import com.limitlessanant.heist.registry.ModEntities;
import com.limitlessanant.heist.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.resource.ResourceType;

public class LimitlessHeistClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new MeshManager());
        com.limitlessanant.heist.client.dev.ShowcaseDirector.init(); // dev only, needs -Dlimitlessheist.showcase=true

        // ---- block entity renderers
        BlockEntityRendererFactories.register(ModBlockEntities.MESH_PROP, MeshPropRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.HACKER_TERMINAL, MeshPropRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.WEAPON_RACK, WeaponRackRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.NEON_SIGN, NeonSignRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.MYSTERY_BOX, MysteryBoxRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.DRAGON_CORE, DragonCoreRenderer::new);
        BlockEntityRendererFactories.register(ModBlockEntities.LASER_EMITTER, LaserEmitterRenderer::new);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), ModBlocks.MYSTERY_BOX, ModBlocks.LASER_EMITTER);
        for (Block b : ModBlocks.NEON_STRIPS.values()) BlockRenderLayerMap.INSTANCE.putBlock(b, RenderLayer.getTranslucent());

        // ---- entities
        EntityModelLayerRegistry.registerModelLayer(VipAccessoriesModel.LAYER, VipAccessoriesModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(LavaSuitModel.LAYER, LavaSuitModel::getTexturedModelData);
        EntityRendererRegistry.register(ModEntities.VIP_GUARD, VipGuardRenderer::new);
        EntityRendererRegistry.register(ModEntities.UNICORN, UnicornRenderer::new);
        EntityRendererRegistry.register(ModEntities.GOTIYA, VillagerEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.FREEZE_BOLT, FreezeBoltRenderer::new);
        EntityRendererRegistry.register(ModEntities.GRAPPLE_HOOK, GrappleHookRenderer::new);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) ->
                helper.register(new FrozenIceFeatureRenderer<>(renderer)));

        // ---- items drawn as 3D meshes
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.FREEZE_RAY_GUN, new MeshItemRenderer("freeze_ray", 1.0f, false));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.SCI_FI_ASSAULT_RIFLE, new MeshItemRenderer("assault_rifle", 1.0f, false));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.BRIEFCASE, new MeshItemRenderer("briefcase", 0.95f, true));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.MONA_LISA, new MeshItemRenderer("mona_lisa", 1.0f, true));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.HACKER_TERMINAL, new MeshItemRenderer("hacker_terminal", 1.0f, true));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.WEAPON_RACK, new MeshItemRenderer("weapon_rack", 1.0f, true));
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.INFINITY_DRAGON_CORE, (stack, mode, ms, vcp, light, overlay) -> {
            ms.push();
            ms.translate(0.5, 0.5, 0.5);
            ms.scale(0.8f, 0.8f, 0.8f);
            float t = MinecraftClient.getInstance().world == null ? 0 : MinecraftClient.getInstance().world.getTime()
                    + MinecraftClient.getInstance().getTickDelta();
            DragonCoreRenderer.renderCore(ms, vcp, overlay, t, false);
            ms.pop();
        });

        // ---- Lava Proof Suit
        ArmorRenderer.register(new LavaSuitArmorRenderer(), ModItems.LAVA_SUIT_HELMET, ModItems.LAVA_SUIT_CHESTPLATE,
                ModItems.LAVA_SUIT_LEGGINGS, ModItems.LAVA_SUIT_BOOTS);

        // ---- grappling gun: 0 = loaded, 0.5 = hook fired (model without hook), 1 = hook head only (flying hook entity)
        FabricModelPredicateProviderRegistry.register(ModItems.GRAPPLING_HOOK_GUN, LimitlessHeist.id("state"), (stack, world, entity, seed) -> {
            if (!stack.hasNbt()) return 0f;
            if (stack.getNbt().getBoolean("HookOnly")) return 1f;
            return stack.getNbt().getBoolean("Fired") ? 0.5f : 0f;
        });
    }
}
