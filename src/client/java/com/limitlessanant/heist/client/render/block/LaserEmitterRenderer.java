package com.limitlessanant.heist.client.render.block;

import com.limitlessanant.heist.block.LaserEmitterBlock;
import com.limitlessanant.heist.blockentity.LaserEmitterBlockEntity;
import com.limitlessanant.heist.client.render.Fx;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

/** The red security beam: hot core + two additive glow shells, with a subtle shimmer. */
public class LaserEmitterRenderer implements BlockEntityRenderer<LaserEmitterBlockEntity> {
    public LaserEmitterRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(LaserEmitterBlockEntity be, float tickDelta, MatrixStack ms, VertexConsumerProvider vcp, int light, int overlay) {
        if (!be.isActive() || be.getLength() <= 0) return;
        Direction d = be.getCachedState().get(LaserEmitterBlock.FACING);
        float t = be.getWorld().getTime() + tickDelta;
        float shimmer = 0.85f + 0.15f * MathHelper.sin(t * 0.9f + be.getPos().hashCode());
        ms.push();
        ms.translate(0.5, 0.5, 0.5);
        ms.multiply(d.getRotationQuaternion()); // +Y -> facing
        float len = be.getLength();
        Fx.beamY(ms, vcp, len, 0.018f, 1f, 0.75f, 0.7f, 0.95f);                 // white-hot core
        Fx.beamY(ms, vcp, len, 0.045f, 1f, 0.08f, 0.05f, 0.75f * shimmer);      // red beam
        Fx.beamY(ms, vcp, len, 0.12f, 1f, 0.02f, 0.02f, 0.18f * shimmer);       // glow
        ms.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(LaserEmitterBlockEntity be) {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }
}
