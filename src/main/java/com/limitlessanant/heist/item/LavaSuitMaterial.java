package com.limitlessanant.heist.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/** Lava Proof "Shark" Suit - netherite-grade protection. Rendered by the custom LavaSuitArmorRenderer, not the vanilla layers. */
public final class LavaSuitMaterial implements ArmorMaterial {
    public static final LavaSuitMaterial INSTANCE = new LavaSuitMaterial();
    private static final int[] DURABILITY = {481, 555, 592, 407}; // boots, legs, chest, helmet (netherite)
    private static final int[] PROTECTION = {3, 6, 8, 3};

    @Override
    public int getDurability(ArmorItem.Type type) {
        return DURABILITY[type.getEquipmentSlot().getEntitySlotId()];
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION[type.getEquipmentSlot().getEntitySlotId()];
    }

    @Override
    public int getEnchantability() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(Items.MAGMA_CREAM);
    }

    @Override
    public String getName() {
        return "lava_suit";
    }

    @Override
    public float getToughness() {
        return 3f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1f;
    }

    private LavaSuitMaterial() {
    }
}
