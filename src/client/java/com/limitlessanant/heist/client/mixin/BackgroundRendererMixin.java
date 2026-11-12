package com.limitlessanant.heist.client.mixin;

import com.limitlessanant.heist.item.LavaSuitArmorItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Red glass helmet of the Lava Proof Suit: you can actually see ~24 blocks inside lava (vanilla: ~1-3 blocks). */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void limitlessheist$lavaVision(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog,
                                                  float tickDelta, CallbackInfo ci) {
        if (camera.getSubmersionType() == CameraSubmersionType.LAVA && camera.getFocusedEntity() instanceof LivingEntity e
                && LavaSuitArmorItem.hasHelmet(e)) {
            RenderSystem.setShaderFogStart(-4f);
            RenderSystem.setShaderFogEnd(Math.min(viewDistance, 24f));
        }
    }
}
