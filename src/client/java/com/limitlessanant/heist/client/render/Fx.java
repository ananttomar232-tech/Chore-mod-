package com.limitlessanant.heist.client.render;

import com.limitlessanant.heist.client.mesh.MeshRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Small immediate-mode helpers: glowing beams (additive), textured quads, billboards. */
public final class Fx {
    /** Additive, untextured box from (0,0,0) to (0,len,0) of the given half-width (beams, ropes, halos). */
    public static void beamY(MatrixStack ms, VertexConsumerProvider vcp, float len, float hw, float r, float g, float b, float a) {
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
        Matrix4f m = ms.peek().getPositionMatrix();
        float[][] c = {{-hw, -hw}, {hw, -hw}, {hw, hw}, {-hw, hw}};
        for (int i = 0; i < 4; i++) {
            float[] p = c[i], q = c[(i + 1) % 4];
            vc.vertex(m, p[0], 0, p[1]).color(r, g, b, a).next();
            vc.vertex(m, q[0], 0, q[1]).color(r, g, b, a).next();
            vc.vertex(m, q[0], len, q[1]).color(r, g, b, a).next();
            vc.vertex(m, p[0], len, p[1]).color(r, g, b, a).next();
        }
    }

    /** Textured quad in the XY plane centred at origin, facing -Z (u runs from +X to -X so text reads correctly from -Z). */
    public static void quad(MatrixStack ms, VertexConsumer vc, float w, float h, float x0, float y0, float r, float g, float b, float a,
                            int light, boolean flipU) {
        Matrix4f m = ms.peek().getPositionMatrix();
        Matrix3f n = ms.peek().getNormalMatrix();
        float xl = x0 - w / 2, xr = x0 + w / 2, yb = y0 - h / 2, yt = y0 + h / 2;
        float u0 = flipU ? 1 : 0, u1 = flipU ? 0 : 1;
        vc.vertex(m, xr, yt, 0).color(r, g, b, a).texture(u0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n, 0, 0, -1).next();
        vc.vertex(m, xl, yt, 0).color(r, g, b, a).texture(u1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n, 0, 0, -1).next();
        vc.vertex(m, xl, yb, 0).color(r, g, b, a).texture(u1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n, 0, 0, -1).next();
        vc.vertex(m, xr, yb, 0).color(r, g, b, a).texture(u0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n, 0, 0, -1).next();
    }

    /** Glowing sprite: translucent core + additive halo. */
    public static void glowQuad(MatrixStack ms, VertexConsumerProvider vcp, Identifier tex, float w, float h, float x0, float y0,
                                float r, float g, float b, float a, float glow, boolean flipU) {
        quad(ms, vcp.getBuffer(RenderLayer.getEntityTranslucent(tex)), w, h, x0, y0, r, g, b, a, MeshRenderer.FULL_BRIGHT, flipU);
        if (glow > 0) quad(ms, vcp.getBuffer(RenderLayer.getEyes(tex)), w, h, x0, y0, r * glow, g * glow, b * glow, 1, MeshRenderer.FULL_BRIGHT, flipU);
    }

    private Fx() {
    }
}
