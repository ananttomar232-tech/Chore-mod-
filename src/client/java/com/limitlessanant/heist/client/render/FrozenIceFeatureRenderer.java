package com.limitlessanant.heist.client.render;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Freeze Ray "ice statue": a translucent ice block around any living entity whose freeze counter is maxed
 * (the FROZEN effect keeps it maxed; the value is synced to every client by vanilla).
 */
public class FrozenIceFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>> extends FeatureRenderer<T, M> {
    private static final Identifier ICE = new Identifier("minecraft", "textures/block/ice.png");

    public FrozenIceFeatureRenderer(FeatureRendererContext<T, M> ctx) {
        super(ctx);
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, T e, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (e.getFrozenTicks() < 140 || !e.isAlive()) return;
        // model space here: y points DOWN, feet at y = 1.501
        float hw = e.getWidth() / 2f + 0.18f, h = e.getHeight() + 0.2f;
        renderIceBox(ms, vcp, light, -hw, 1.52f - h, -hw, hw, 1.52f, hw);
    }

    public static void renderIceBox(MatrixStack ms, VertexConsumerProvider vcp, int light, float x1, float y1, float z1, float x2, float y2, float z2) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucent(ICE));
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();
        float a = 0.62f;
        float[][] faces = {
                {x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, 0, 0, -1},
                {x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2, 0, 0, 1},
                {x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2, -1, 0, 0},
                {x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, 1, 0, 0},
                {x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1, 0, -1, 0},
                {x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, 0, 1, 0},
        };
        for (float[] f : faces) {
            float du = Math.max(Math.abs(f[3] - f[0]) + Math.abs(f[5] - f[2]), 0.01f), dv = Math.abs(f[7] - f[1]) + 0.01f;
            float[][] uv = {{0, 0}, {du, 0}, {du, dv}, {0, dv}};
            for (int k = 0; k < 4; k++) {
                vc.vertex(m, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).color(0.85f, 0.95f, 1f, a).texture(uv[k][0], uv[k][1])
                        .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n, f[12], f[13], f[14]).next();
            }
        }
    }
}
