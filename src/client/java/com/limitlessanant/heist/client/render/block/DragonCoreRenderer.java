package com.limitlessanant.heist.client.render.block;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.blockentity.DragonCoreBlockEntity;
import com.limitlessanant.heist.client.mesh.HeistMesh;
import com.limitlessanant.heist.client.mesh.MeshManager;
import com.limitlessanant.heist.client.mesh.MeshRenderer;
import com.limitlessanant.heist.client.render.Fx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/**
 * The Infinity Dragon Core: floating + bobbing, crystal spinning one way, the two gyro rings the other ways, orbiting
 * shards, a pulsing additive bloom halo and an optional sky beam. Also used for the item (DragonCoreRenderer.renderCore).
 */
public class DragonCoreRenderer implements BlockEntityRenderer<DragonCoreBlockEntity> {
    private static final Identifier HALO = LimitlessHeist.id("textures/entity/core_halo.png");

    public DragonCoreRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    public static Matrix4f[] pose(HeistMesh mesh, float t) {
        Matrix4f[] p = new Matrix4f[mesh.bones.length];
        for (int i = 0; i < p.length; i++) {
            HeistMesh.Bone b = mesh.bones[i];
            Matrix4f m = new Matrix4f().translate(b.px(), b.py(), b.pz());
            switch (b.name()) {
                case "crystal" -> m.rotateY(t * 0.05f);
                case "ring_a" -> m.rotateX(t * 0.07f).rotateZ(0.4f);
                case "ring_b" -> m.rotateZ(t * 0.06f).rotateX(-0.5f);
                case "shards" -> m.rotateY(-t * 0.09f);
                default -> {
                }
            }
            p[i] = m.translate(-b.px(), -b.py(), -b.pz());
        }
        return p;
    }

    /** Core centred on the current matrix origin. */
    public static void renderCore(MatrixStack ms, VertexConsumerProvider vcp, int overlay, float t, boolean halo) {
        HeistMesh mesh = MeshManager.get("dragon_core");
        if (mesh == null) return;
        MeshRenderer.Options o = new MeshRenderer.Options();
        o.time = t;
        o.pose = pose(mesh, t);
        float pulse = 0.8f + 0.2f * MathHelper.sin(t * 0.15f);
        o.glow = 1.1f * pulse;
        o.glowInflate = 0.02f;
        MeshRenderer.render(mesh, ms, vcp, MeshRenderer.FULL_BRIGHT, overlay, o);
        if (halo) {
            ms.push();
            ms.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
            Fx.quad(ms, vcp.getBuffer(net.minecraft.client.render.RenderLayer.getEyes(HALO)), 2.6f * pulse, 2.6f * pulse, 0, 0,
                    0.85f * pulse, 0.35f * pulse, 1f * pulse, 1f, MeshRenderer.FULL_BRIGHT, false);
            ms.pop();
        }
    }

    @Override
    public void render(DragonCoreBlockEntity be, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        float t = (be.getWorld() == null ? 0 : be.getWorld().getTime()) + tickDelta;
        ms.push();
        ms.translate(0.5, 0.75 + MathHelper.sin(t * 0.06f) * 0.1, 0.5);
        renderCore(ms, vcp, overlay, t, true);
        ms.pop();
        if (be.hasBeam() && be.getWorld() != null) {
            BeaconBlockEntityRenderer.renderBeam(ms, vcp, BeaconBlockEntityRenderer.BEAM_TEXTURE, tickDelta, 1f, be.getWorld().getTime(),
                    1, 1024, new float[]{0.75f, 0.25f, 1f}, 0.12f, 0.2f);
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(DragonCoreBlockEntity be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 256;
    }
}
