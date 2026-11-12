package com.limitlessanant.heist.client.model;

import com.limitlessanant.heist.LimitlessHeist;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.entity.LivingEntity;

/**
 * Lava Proof "Shark" Suit - a bulky orange/red sci-fi space suit. Built from cuboids on the standard biped skeleton so it
 * follows every player animation (also in blockbench/lava_suit.bbmodel). Texture 128x64: textures/models/armor/lava_suit.png
 * <ul>
 *   <li>head: armoured collar ring + red glass dome (the "visor" child is drawn in a second, translucent pass)</li>
 *   <li>body: chest plate, shoulder pads, oxygen backpack with two tanks + shark fin</li>
 *   <li>arms / legs: thick plated sleeves; legs split into "pants" (leggings) and "boot" (boots) children</li>
 * </ul>
 * Pixel layout of every cuboid is mirrored in tools/gen_textures_entities.py (LAVA_SUIT_BOXES).
 */
public class LavaSuitModel extends BipedEntityModel<LivingEntity> {
    public static final EntityModelLayer LAYER = new EntityModelLayer(LimitlessHeist.id("lava_suit"), "main");
    public final ModelPart visor, helmetShell, collar, chest, backpack, rightPants, leftPants, rightBoot, leftBoot, rightSleeve, leftSleeve;

    public LavaSuitModel(ModelPart root) {
        super(root);
        this.helmetShell = head.getChild("shell");
        this.visor = head.getChild("visor");
        this.collar = head.getChild("collar");
        this.chest = body.getChild("chest");
        this.backpack = body.getChild("backpack");
        this.rightSleeve = rightArm.getChild("sleeve");
        this.leftSleeve = leftArm.getChild("sleeve");
        this.rightPants = rightLeg.getChild("pants");
        this.leftPants = leftLeg.getChild("pants");
        this.rightBoot = rightLeg.getChild("boot");
        this.leftBoot = leftLeg.getChild("boot");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = BipedEntityModel.getModelData(Dilation.NONE, 0f);
        ModelPartData root = data.getRoot();
        // replace the vanilla cuboids with empty parts and hang our pieces under them
        ModelPartData head = root.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create(), ModelTransform.pivot(0, 0, 0));
        root.addChild(EntityModelPartNames.HAT, ModelPartBuilder.create(), ModelTransform.pivot(0, 0, 0));
        head.addChild("shell", ModelPartBuilder.create()
                .uv(0, 0).cuboid(-5.5f, -9.5f, -5.5f, 11, 2, 11)                      // crown plate
                .uv(44, 0).cuboid(-5.5f, -7.5f, 3.0f, 11, 7, 2.5f)                    // back of helmet
                .uv(0, 13).cuboid(-6.2f, -6.0f, -2.0f, 1.2f, 4, 4)                   // left ear module
                .uv(0, 13).cuboid(5.0f, -6.0f, -2.0f, 1.2f, 4, 4)                    // right ear module
                .uv(10, 13).cuboid(4.6f, -13.0f, 1.0f, 0.8f, 4, 0.8f), ModelTransform.NONE); // antenna
        head.addChild("visor", ModelPartBuilder.create()
                .uv(64, 0).cuboid(-5.0f, -7.6f, -5.6f, 10, 7.4f, 8.8f), ModelTransform.NONE); // red glass dome
        head.addChild("collar", ModelPartBuilder.create()
                .uv(0, 22).cuboid(-6.0f, -0.6f, -6.0f, 12, 2, 12), ModelTransform.NONE);       // neck ring
        ModelPartData body = root.addChild(EntityModelPartNames.BODY, ModelPartBuilder.create(), ModelTransform.pivot(0, 0, 0));
        body.addChild("chest", ModelPartBuilder.create()
                .uv(16, 36).cuboid(-5.0f, 0.0f, -3.2f, 10, 12.5f, 6.4f)                // torso armour
                .uv(48, 36).cuboid(-3.0f, 2.0f, -4.0f, 6, 5, 1)                        // chest console
                .uv(62, 36).cuboid(-5.2f, 10.0f, -3.4f, 10.4f, 2, 6.8f), ModelTransform.NONE); // belt
        body.addChild("backpack", ModelPartBuilder.create()
                .uv(84, 0).cuboid(-4.0f, 0.5f, 3.2f, 8, 10, 3)                         // pack
                .uv(84, 14).cuboid(-3.6f, -1.0f, 6.2f, 3, 11, 3)                       // tank L
                .uv(84, 14).cuboid(0.6f, -1.0f, 6.2f, 3, 11, 3)                        // tank R
                .uv(98, 14).cuboid(-0.5f, -3.5f, 5.5f, 1, 6, 4), ModelTransform.NONE); // shark fin
        ModelPartData ra = root.addChild(EntityModelPartNames.RIGHT_ARM, ModelPartBuilder.create(), ModelTransform.pivot(-5, 2, 0));
        ra.addChild("sleeve", ModelPartBuilder.create()
                .uv(0, 36).cuboid(-4.0f, -2.6f, -2.7f, 5.4f, 12.5f, 5.4f)
                .uv(0, 54).cuboid(-4.6f, -3.4f, -3.2f, 6.4f, 4, 6.4f), ModelTransform.NONE); // shoulder pad
        ModelPartData la = root.addChild(EntityModelPartNames.LEFT_ARM, ModelPartBuilder.create(), ModelTransform.pivot(5, 2, 0));
        la.addChild("sleeve", ModelPartBuilder.create().mirrored()
                .uv(0, 36).cuboid(-1.4f, -2.6f, -2.7f, 5.4f, 12.5f, 5.4f)
                .uv(0, 54).cuboid(-1.8f, -3.4f, -3.2f, 6.4f, 4, 6.4f), ModelTransform.NONE);
        ModelPartData rl = root.addChild(EntityModelPartNames.RIGHT_LEG, ModelPartBuilder.create(), ModelTransform.pivot(-1.9f, 12, 0));
        rl.addChild("pants", ModelPartBuilder.create().uv(26, 54).cuboid(-2.7f, -0.2f, -2.7f, 5.4f, 8.5f, 5.4f), ModelTransform.NONE);
        rl.addChild("boot", ModelPartBuilder.create().uv(48, 50).cuboid(-3.0f, 7.5f, -3.6f, 6, 4.8f, 6.6f), ModelTransform.NONE);
        ModelPartData ll = root.addChild(EntityModelPartNames.LEFT_LEG, ModelPartBuilder.create(), ModelTransform.pivot(1.9f, 12, 0));
        ll.addChild("pants", ModelPartBuilder.create().mirrored().uv(26, 54).cuboid(-2.7f, -0.2f, -2.7f, 5.4f, 8.5f, 5.4f), ModelTransform.NONE);
        ll.addChild("boot", ModelPartBuilder.create().mirrored().uv(48, 50).cuboid(-3.0f, 7.5f, -3.6f, 6, 4.8f, 6.6f), ModelTransform.NONE);
        return TexturedModelData.of(data, 128, 64);
    }
}
