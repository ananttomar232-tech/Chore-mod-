package com.limitlessanant.heist.client.render.block;

import com.limitlessanant.heist.blockentity.WeaponRackBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/** Rack mesh + up to three items hanging horizontally on it, side view facing the room. */
public class WeaponRackRenderer extends MeshPropRenderer<WeaponRackBlockEntity> {
    public WeaponRackRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    protected void afterMesh(WeaponRackBlockEntity be, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay, float tickDelta) {
        for (int i = 0; i < WeaponRackBlockEntity.SLOTS; i++) {
            ItemStack s = be.getItems().get(i);
            if (s.isEmpty()) continue;
            ms.push();
            ms.translate(0, 1.42 - i * 0.48, -0.32);         // three rows: top, middle, bottom
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180)); // face out of the wall
            ms.scale(1.6f, 1.6f, 1.6f);
            MinecraftClient.getInstance().getItemRenderer().renderItem(s, ModelTransformationMode.FIXED, light, overlay, ms, vcp,
                    be.getWorld(), (int) be.getPos().asLong() + i);
            ms.pop();
        }
    }
}
