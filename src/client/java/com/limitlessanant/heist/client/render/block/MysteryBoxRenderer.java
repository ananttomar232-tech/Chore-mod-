package com.limitlessanant.heist.client.render.block;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.blockentity.MysteryBoxBlockEntity;
import com.limitlessanant.heist.client.render.Fx;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Spinning, bobbing neon "?" hologram above the crate. */
public class MysteryBoxRenderer implements BlockEntityRenderer<MysteryBoxBlockEntity> {
    private static final Identifier Q = LimitlessHeist.id("textures/block/neon/question.png");

    public MysteryBoxRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(MysteryBoxBlockEntity be, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        float t = (be.getWorld() == null ? 0 : be.getWorld().getTime()) + tickDelta + (be.getPos().hashCode() & 63);
        ms.push();
        ms.translate(0.5, 1.35 + MathHelper.sin(t * 0.08f) * 0.08, 0.5);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 3f));
        float hue = (t * 0.004f) % 1f;
        int rgb = MathHelper.hsvToRgb(0.78f + 0.12f * MathHelper.sin(t * 0.03f), 0.7f, 1f);
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        Fx.glowQuad(ms, vcp, Q, 0.62f, 0.62f, 0, 0, r, g, b, 1f, 1f, false);
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        Fx.glowQuad(ms, vcp, Q, 0.62f, 0.62f, 0, 0, r, g, b, 1f, 1f, false);
        ms.pop();
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }
}
