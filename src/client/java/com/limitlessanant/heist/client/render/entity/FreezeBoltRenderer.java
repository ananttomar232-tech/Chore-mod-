package com.limitlessanant.heist.client.render.entity;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.client.render.Fx;
import com.limitlessanant.heist.entity.FreezeBoltEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** Ice bolt: a spinning glowing snowflake orb, always facing the camera. */
public class FreezeBoltRenderer extends EntityRenderer<FreezeBoltEntity> {
    private static final Identifier TEX = LimitlessHeist.id("textures/entity/freeze_bolt.png");

    public FreezeBoltRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FreezeBoltEntity e, float yaw, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light) {
        ms.push();
        ms.translate(0, 0.15, 0);
        ms.multiply(dispatcher.getRotation());
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((e.age + tickDelta) * 25f));
        Fx.glowQuad(ms, vcp, TEX, 0.9f, 0.9f, 0, 0, 0.75f, 0.95f, 1f, 1f, 1.2f, false);
        ms.pop();
        super.render(e, yaw, tickDelta, ms, vcp, light);
    }

    @Override
    public Identifier getTexture(FreezeBoltEntity entity) {
        return TEX;
    }
}
