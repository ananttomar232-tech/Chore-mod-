package com.limitlessanant.heist.client.render.entity;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.client.render.Fx;
import com.limitlessanant.heist.entity.GrappleHookEntity;
import com.limitlessanant.heist.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** The flying hook head (item model "grapple_hook_head") and the steel cable back to the shooter's gun. */
public class GrappleHookRenderer extends EntityRenderer<GrappleHookEntity> {
    private static final ItemStack HEAD = new ItemStack(ModItems.GRAPPLING_HOOK_GUN);

    public GrappleHookRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GrappleHookEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        Entity owner = e.getOwner();
        // hook head
        ms.push();
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, e.prevYaw, e.getYaw()) - 90f));
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch())));
        ItemStack head = HEAD.copy();
        head.getOrCreateNbt().putBoolean("HookOnly", true);
        MinecraftClient.getInstance().getItemRenderer().renderItem(head, ModelTransformationMode.GROUND, light,
                net.minecraft.client.render.OverlayTexture.DEFAULT_UV, ms, vcp, e.getWorld(), e.getId());
        ms.pop();
        if (owner instanceof PlayerEntity p) renderCable(e, p, tickDelta, ms, vcp, light);
        super.render(e, yaw, tickDelta, ms, vcp, light);
    }

    private void renderCable(GrappleHookEntity e, PlayerEntity p, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        Vec3d hand = handPos(p, tickDelta);
        Vec3d hook = e.getLerpedPos(tickDelta);
        Vec3d d = hand.subtract(hook);
        float sag = e.isHooked() ? 0f : 0.08f;
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getLeash());
        Matrix4f m = ms.peek().getPositionMatrix();
        int segs = 24;
        float w = 0.025f;
        // two crossed ribbons so the cable has volume from every angle
        for (int ribbon = 0; ribbon < 2; ribbon++) {
            for (int k = 0; k <= segs; k++) {
                int i = ribbon == 0 ? k : segs - k; // strip goes there and back, like the vanilla leash
                float t = i / (float) segs;
                float x = (float) (d.x * t), y = (float) (d.y * t - Math.sin(t * Math.PI) * sag * d.length()), z = (float) (d.z * t);
                float shade = (i % 2 == 0) ? 0.22f : 0.3f;
                float ox = ribbon == 0 ? w : 0, oy = ribbon == 0 ? 0 : w;
                vc.vertex(m, x - ox, y - oy, z).color(shade, shade, shade + 0.03f, 1f).light(light).next();
                vc.vertex(m, x + ox, y + oy, z).color(shade, shade, shade + 0.03f, 1f).light(light).next();
            }
        }
    }

    private static Vec3d handPos(PlayerEntity p, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int side = p.getMainArm() == Arm.RIGHT ? 1 : -1;
        float yaw = MathHelper.lerp(tickDelta, p.prevBodyYaw, p.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        if (p == mc.player && mc.options.getPerspective().isFirstPerson()) {
            Vec3d eye = p.getCameraPosVec(tickDelta);
            Vec3d look = p.getRotationVec(tickDelta);
            Vec3d right = new Vec3d(-Math.cos(p.getYaw(tickDelta) * MathHelper.RADIANS_PER_DEGREE), 0, -Math.sin(p.getYaw(tickDelta) * MathHelper.RADIANS_PER_DEGREE));
            return eye.add(look.multiply(0.6)).add(right.multiply(-0.32 * side)).add(0, -0.28, 0);
        }
        Vec3d base = p.getLerpedPos(tickDelta);
        double sx = -Math.cos(yaw) * 0.38 * side, sz = -Math.sin(yaw) * 0.38 * side;
        Vec3d fwd = Vec3d.fromPolar(0, p.getYaw(tickDelta)).multiply(0.45);
        return base.add(sx + fwd.x, p.getHeight() * 0.62, sz + fwd.z);
    }

    @Override
    public Identifier getTexture(GrappleHookEntity entity) {
        return LimitlessHeist.id("textures/item/grappling_hook_gun.png");
    }
}
