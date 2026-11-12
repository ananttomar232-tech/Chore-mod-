package com.limitlessanant.heist.client.render.entity;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.client.mesh.HeistMesh;
import com.limitlessanant.heist.client.mesh.MeshManager;
import com.limitlessanant.heist.client.mesh.MeshRenderer;
import com.limitlessanant.heist.client.render.FrozenIceFeatureRenderer;
import com.limitlessanant.heist.entity.UnicornEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import java.util.Map;
import java.util.WeakHashMap;

/** Renders the rigged unicorn mesh with {@link UnicornPose}: rearing when idle, galloping when moving, flapping in the air. */
public class UnicornRenderer extends EntityRenderer<UnicornEntity> {
    private static final Identifier TEX = LimitlessHeist.id("textures/mesh/unicorn/body.png");
    private final Map<UnicornEntity, float[]> state = new WeakHashMap<>(); // lean, fly, lastTime

    public UnicornRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.1f;
    }

    @Override
    public void render(UnicornEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        HeistMesh mesh = MeshManager.get("unicorn");
        if (mesh != null) {
            float t = e.age + tickDelta;
            float speed = e.limbAnimator.getSpeed(tickDelta);
            boolean airborne = !e.isOnGround() && !e.isTouchingWater();
            boolean moving = speed > 0.08f || e.hasPassengers() || airborne;
            float[] s = state.computeIfAbsent(e, k -> new float[]{moving ? 1 : 0, 0, t});
            float dt = MathHelper.clamp(t - s[2], 0, 5);
            s[2] = t;
            s[0] += ((moving ? 1 : 0) - s[0]) * Math.min(1, 0.12f * dt);
            s[1] += ((airborne ? 1 : 0) - s[1]) * Math.min(1, 0.15f * dt);
            if (e.isAngry() && !e.hasPassengers()) s[0] += (0 - s[0]) * Math.min(1, 0.3f * dt); // rear up when angry
            float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, e.prevBodyYaw, e.bodyYaw);
            float headYaw = MathHelper.wrapDegrees(MathHelper.lerpAngleDegrees(tickDelta, e.prevHeadYaw, e.headYaw) - bodyYaw);
            float pitch = MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch());

            MeshRenderer.Options o = new MeshRenderer.Options();
            o.time = t;
            o.glow = 0.9f;
            o.glowInflate = 0.015f;
            o.pose = UnicornPose.compute(mesh, s[0], Math.min(1, speed * 1.6f) * (1 - s[1] * 0.8f),
                    e.limbAnimator.getPos(tickDelta), s[1], t, headYaw, pitch);
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));
            if (e.deathTime > 0) {
                float f = Math.min(1, MathHelper.sqrt((e.deathTime + tickDelta - 1) / 20f * 1.6f));
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 90f));
            }
            int overlay = LivingEntityRenderer.getOverlay(e, 0f);
            MeshRenderer.render(mesh, ms, vcp, light, overlay, o);
            if (e.getFrozenTicks() >= 140) FrozenIceFeatureRenderer.renderIceBox(ms, vcp, light, -1f, 0f, -1.3f, 1f, 3.2f, 1.3f);
            ms.pop();
        }
        super.render(e, yaw, tickDelta, ms, vcp, light);
    }

    @Override
    public Identifier getTexture(UnicornEntity entity) {
        return TEX;
    }
}
