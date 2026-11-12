package com.limitlessanant.heist.client.model;

import com.limitlessanant.heist.LimitlessHeist;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;

/**
 * VIP golem accessories, built "Blockbench style" from cuboids (also exported as blockbench/vip_accessories.bbmodel).
 * Coordinates are in pixels relative to the vanilla IronGolemEntityModel head / body pivots:
 * <pre>
 * golem head cuboid  (-4, -12, -5.5)  8x10x8   (front face at z = -5.5, nose sticks out to -7.5 at y -5..-1)
 * golem body cuboid  (-9,  -2, -6)   18x12x11  (front face at z = -6)
 * </pre>
 * Texture: textures/entity/vip_accessories.png (32x32).
 */
public class VipAccessoriesModel {
    public static final EntityModelLayer LAYER = new EntityModelLayer(LimitlessHeist.id("vip_accessories"), "main");
    public final ModelPart glasses, lensGlint, earpiece, tie;

    public VipAccessoriesModel(ModelPart root) {
        this.glasses = root.getChild("glasses");
        this.lensGlint = root.getChild("lens_glint");
        this.earpiece = root.getChild("earpiece");
        this.tie = root.getChild("tie");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData r = data.getRoot();
        // ---- 3D black sunglasses (wrap-around "agent" shades)
        r.addChild("glasses", ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-4.5f, -9.0f, -6.4f, 4.0f, 2.5f, 0.8f)    // left lens
                        .uv(0, 4).cuboid(0.5f, -9.0f, -6.4f, 4.0f, 2.5f, 0.8f)     // right lens
                        .uv(10, 0).cuboid(-0.5f, -8.8f, -6.3f, 1.0f, 0.8f, 0.6f)   // bridge
                        .uv(0, 8).cuboid(-4.8f, -9.2f, -6.5f, 9.6f, 0.6f, 1.0f)    // top bar
                        .uv(0, 10).cuboid(-4.7f, -8.9f, -5.9f, 0.5f, 0.6f, 5.0f)   // temple arms
                        .uv(0, 10).cuboid(4.2f, -8.9f, -5.9f, 0.5f, 0.6f, 5.0f),
                ModelTransform.NONE);
        r.addChild("lens_glint", ModelPartBuilder.create()
                        .uv(20, 0).cuboid(-4.3f, -8.9f, -6.55f, 3.6f, 2.2f, 0.1f)
                        .uv(20, 0).cuboid(0.7f, -8.9f, -6.55f, 3.6f, 2.2f, 0.1f),
                ModelTransform.NONE);
        // ---- security earpiece: coil from the right ear down to the collar
        r.addChild("earpiece", ModelPartBuilder.create()
                        .uv(12, 10).cuboid(-4.6f, -7.5f, -2.5f, 0.8f, 1.0f, 1.0f)
                        .uv(16, 10).cuboid(-4.4f, -6.5f, -2.2f, 0.4f, 5.0f, 0.4f),
                ModelTransform.NONE);
        // ---- tie: knot + blade, sitting on the shirt front
        r.addChild("tie", ModelPartBuilder.create()
                        .uv(0, 16).cuboid(-1.0f, -2.0f, -6.8f, 2.0f, 1.6f, 0.9f, new Dilation(0.05f))
                        .uv(6, 16).cuboid(-1.25f, -0.4f, -6.6f, 2.5f, 5.0f, 0.6f)
                        .uv(12, 16).cuboid(-0.85f, 4.6f, -6.6f, 1.7f, 2.4f, 0.6f)
                        .uv(16, 16).cuboid(-1.4f, -2.2f, -6.75f, 2.8f, 0.4f, 0.3f), // collar bar
                ModelTransform.NONE);
        return TexturedModelData.of(data, 32, 32);
    }
}
