package com.limitlessanant.heist.client.mixin;

import com.limitlessanant.heist.item.LavaSuitArmorItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No flames covering the screen while wearing the full Lava Proof Suit (great for lava cinematics). */
@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Inject(method = "renderFireOverlay", at = @At("HEAD"), cancellable = true)
    private static void limitlessheist$noFireOverlay(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (client.player != null && LavaSuitArmorItem.hasFullSuit(client.player)) ci.cancel();
    }
}
