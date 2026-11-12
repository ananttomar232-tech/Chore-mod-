package com.limitlessanant.heist.client.render.entity;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.client.model.VipAccessoriesModel;
import com.limitlessanant.heist.client.mesh.MeshRenderer;
import com.limitlessanant.heist.entity.VipGuardEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.IronGolemEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/** 3D black sunglasses (with a moving glint), a silk tie and a security earpiece on top of the golem model. */
public class VipAccessoriesFeatureRenderer extends FeatureRenderer<VipGuardEntity, IronGolemEntityModel<VipGuardEntity>> {
    public static final Identifier TEXTURE = LimitlessHeist.id("textures/entity/vip_accessories.png");
    private final VipAccessoriesModel model;

    public VipAccessoriesFeatureRenderer(FeatureRendererContext<VipGuardEntity, IronGolemEntityModel<VipGuardEntity>> ctx, EntityModelLoader loader) {
        super(ctx);
        this.model = new VipAccessoriesModel(loader.getModelPart(VipAccessoriesModel.LAYER));
    }

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, int light, VipGuardEntity e, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        int overlay = LivingEntityRenderer.getOverlay(e, 0f);
        var root = getContextModel().getPart();
        // head-attached: sunglasses + earpiece
        ms.push();
        root.getChild("head").rotate(ms);
        model.glasses.render(ms, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, overlay);
        model.earpiece.render(ms, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, overlay);
        // the glint sweeping across the lenses
        float g = (float) Math.max(0, Math.sin((animationProgress + e.getId() * 7) * 0.05));
        g = g * g * g;
        model.lensGlint.render(ms, vcp.getBuffer(RenderLayer.getEyes(TEXTURE)), MeshRenderer.FULL_BRIGHT, OverlayTexture.DEFAULT_UV, g, g, g, 1f);
        ms.pop();
        // body-attached: tie
        ms.push();
        root.getChild("body").rotate(ms);
        model.tie.render(ms, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE)), light, overlay);
        ms.pop();
    }
}
