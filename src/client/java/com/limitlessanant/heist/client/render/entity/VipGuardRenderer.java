package com.limitlessanant.heist.client.render.entity;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.entity.VipGuardEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * VIP bodyguard = the vanilla iron golem model (so all vanilla animations work) with an original black-suit skin
 * (textures/entity/vip_guard.png, painted on the golem UV layout) + 3D sunglasses / tie / earpiece feature.
 */
public class VipGuardRenderer extends MobEntityRenderer<VipGuardEntity, IronGolemEntityModel<VipGuardEntity>> {
    private static final Identifier TEXTURE = LimitlessHeist.id("textures/entity/vip_guard.png");

    public VipGuardRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new IronGolemEntityModel<>(ctx.getPart(EntityModelLayers.IRON_GOLEM)), 0.7f);
        addFeature(new VipAccessoriesFeatureRenderer(this, ctx.getModelLoader()));
    }

    @Override
    public Identifier getTexture(VipGuardEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void setupTransforms(VipGuardEntity e, MatrixStack ms, float animationProgress, float bodyYaw, float tickDelta) {
        super.setupTransforms(e, ms, animationProgress, bodyYaw, tickDelta);
        if (e.limbAnimator.getSpeed() >= 0.01) { // same heavy waddle as the vanilla golem
            float j = e.limbAnimator.getPos(tickDelta) + 6f;
            float k = (Math.abs(j % 13f - 6.5f) - 3.25f) / 3.25f;
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6.5f * k));
        }
    }
}
