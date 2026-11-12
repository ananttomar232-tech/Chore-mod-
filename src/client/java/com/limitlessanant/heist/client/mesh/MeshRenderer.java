package com.limitlessanant.heist.client.mesh;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Immediate-mode renderer for {@link HeistMesh}. Uses vanilla entity render layers so it works with Iris / shaders:
 * <ul>
 *   <li>normal groups: entity cutout (no cull) with world light</li>
 *   <li>EMISSIVE: full-bright, plus an additive "eyes" pass when GLOW is set (bloom-ish halo)</li>
 *   <li>RAINBOW: vertex colour = animated dark rainbow (unicorn horn)</li>
 *   <li>bones: per-vertex blend between the root and one bone matrix (simple linear skinning)</li>
 * </ul>
 * Triangles are emitted as degenerate quads (entity layers draw quads).
 */
public final class MeshRenderer {
    public static final int FULL_BRIGHT = LightmapTextureManager.MAX_LIGHT_COORDINATE;

    public static final class Options {
        public Matrix4f[] pose;        // model-space bone matrices (index = bone id) or null
        public float time;             // ticks, for animated effects
        public float alpha = 1f;
        public float glow = 1f;        // strength of the additive glow pass
        public float emissiveFlicker = 1f;
        public float glowInflate = 0f; // push the glow shell outward along normals (blocks)
        public float r = 1f, g = 1f, b = 1f;
    }

    private static final Options DEFAULT = new Options();

    public static void render(HeistMesh mesh, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        render(mesh, ms, vcp, light, overlay, DEFAULT);
    }

    public static void render(HeistMesh mesh, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay, Options o) {
        if (mesh == null) return;
        MatrixStack.Entry e = ms.peek();
        Matrix4f pm = e.getPositionMatrix();
        Matrix3f nm = e.getNormalMatrix();
        Matrix4f[] comb = null;
        Matrix3f[] ncomb = null;
        if (o.pose != null) {
            comb = new Matrix4f[o.pose.length];
            ncomb = new Matrix3f[o.pose.length];
            for (int i = 0; i < o.pose.length; i++) {
                comb[i] = new Matrix4f(pm).mul(o.pose[i]);
                ncomb[i] = new Matrix3f(nm).mul(new Matrix3f(o.pose[i]));
            }
        }
        for (HeistMesh.Group g : mesh.groups) {
            boolean emissive = g.has(HeistMesh.FLAG_EMISSIVE);
            int l = emissive ? FULL_BRIGHT : light;
            float a = g.a * o.alpha;
            RenderLayer layer;
            if (g.has(HeistMesh.FLAG_TRANSLUCENT) || a < 0.999f) layer = RenderLayer.getEntityTranslucent(g.texture);
            else layer = RenderLayer.getEntityCutoutNoCull(g.texture);
            float fl = emissive ? o.emissiveFlicker : 1f;
            if (!(g.has(HeistMesh.FLAG_GLOW) && g.has(HeistMesh.FLAG_TRANSLUCENT))) {
                emit(g, vcp.getBuffer(layer), pm, nm, comb, ncomb, o, g.r * o.r * fl, g.g * o.g * fl, g.b * o.b * fl, a, l, overlay, 0f);
            }
            if (g.has(HeistMesh.FLAG_GLOW)) {
                float gs = o.glow * fl;
                emit(g, vcp.getBuffer(RenderLayer.getEyes(g.texture)), pm, nm, comb, ncomb, o,
                        g.r * gs, g.g * gs, g.b * gs, 1f, FULL_BRIGHT, overlay, o.glowInflate);
            }
        }
    }

    private static void emit(HeistMesh.Group g, VertexConsumer vc, Matrix4f pm, Matrix3f nm, Matrix4f[] comb, Matrix3f[] ncomb,
                             Options o, float r, float gg, float b, float a, int light, int overlay, float inflate) {
        boolean rainbow = g.has(HeistMesh.FLAG_RAINBOW);
        Vector3f p = new Vector3f(), q = new Vector3f(), n = new Vector3f(), m = new Vector3f();
        float[] pos = g.pos, uv = g.uv, nrm = g.nrm;
        float cr = r, cg = gg, cb = b;
        for (int t = 0; t < g.count; t += 3) {
            for (int k = 0; k < 4; k++) {
                int i = t + Math.min(k, 2);
                float x = pos[i * 3], y = pos[i * 3 + 1], z = pos[i * 3 + 2];
                float nx = nrm[i * 3], ny = nrm[i * 3 + 1], nz = nrm[i * 3 + 2];
                if (inflate != 0) {
                    x += nx * inflate;
                    y += ny * inflate;
                    z += nz * inflate;
                }
                if (comb != null) {
                    int bi = g.bone[i];
                    float w = g.weight[i];
                    comb[0].transformPosition(x, y, z, p);
                    ncomb[0].transform(nx, ny, nz, n);
                    if (bi > 0 && bi < comb.length && w > 0) {
                        comb[bi].transformPosition(x, y, z, q);
                        ncomb[bi].transform(nx, ny, nz, m);
                        p.lerp(q, w);
                        n.lerp(m, w);
                    }
                } else {
                    pm.transformPosition(x, y, z, p);
                    nm.transform(nx, ny, nz, n);
                }
                if (rainbow) {
                    // dark rainbow: deep saturated hues running up the horn
                    float hue = (o.time * 0.012f + y * 0.9f) % 1f;
                    int rgb = MathHelper.hsvToRgb(hue < 0 ? hue + 1 : hue, 1f, 0.85f);
                    cr = ((rgb >> 16) & 255) / 255f * r;
                    cg = ((rgb >> 8) & 255) / 255f * gg;
                    cb = (rgb & 255) / 255f * b;
                }
                vc.vertex(p.x, p.y, p.z, cr, cg, cb, a, uv[i * 2], uv[i * 2 + 1], overlay, light, n.x, n.y, n.z);
            }
        }
    }

    private MeshRenderer() {
    }
}
