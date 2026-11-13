package com.limitlessanant.heist.client.render;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.client.model.LavaSuitModel;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Draws {@link LavaSuitModel} instead of the vanilla armour layers; the helmet's dome is red translucent glass. */
public class LavaSuitArmorRenderer implements ArmorRenderer {
    public static final Identifier TEXTURE = LimitlessHeist.id("textures/models/armor/lava_suit.png");
    private LavaSuitModel model;

    @Override
    public void render(MatrixStack ms, VertexConsumerProvider vcp, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light,
                       BipedEntityModel<LivingEntity> ctx) {
        if (model == null) model = new LavaSuitModel(MinecraftClient.getInstance().getEntityModelLoader().getModelPart(LavaSuitModel.LAYER));
        ctx.copyBipedStateTo(model);
        model.setVisible(false);
        model.helmetShell.visible = model.collar.visible = slot == EquipmentSlot.HEAD;
        model.visor.visible = false;
        model.chest.visible = model.backpack.visible = model.rightSleeve.visible = model.leftSleeve.visible = slot == EquipmentSlot.CHEST;
        model.rightPants.visible = model.leftPants.visible = slot == EquipmentSlot.LEGS;
        model.rightBoot.visible = model.leftBoot.visible = slot == EquipmentSlot.FEET;
        switch (slot) {
            case HEAD -> model.head.visible = true;
            case CHEST -> model.body.visible = model.rightArm.visible = model.leftArm.visible = true;
            case LEGS, FEET -> model.rightLeg.visible = model.leftLeg.visible = true;
            default -> {
            }
        }
        ArmorRenderer.renderPart(ms, vcp, light, stack, model, TEXTURE);
        if (slot == EquipmentSlot.HEAD) { // the red glass dome
            model.helmetShell.visible = model.collar.visible = false;
            model.visor.visible = true;
            model.head.render(ms, vcp.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE)), light, OverlayTexture.DEFAULT_UV, 1f, 0.6f, 0.55f, 0.85f);
        }
    }
}
