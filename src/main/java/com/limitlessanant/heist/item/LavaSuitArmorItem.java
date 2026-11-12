package com.limitlessanant.heist.item;

import com.limitlessanant.heist.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lava Proof Suit piece. Full set: permanent Fire Resistance (HeistEvents), super fast lava swimming
 * (mixin/LivingEntityMixin) and clear vision inside lava through the red glass helmet (client BackgroundRendererMixin).
 */
public class LavaSuitArmorItem extends ArmorItem {
    public LavaSuitArmorItem(ArmorMaterial material, Type type, Settings settings) {
        super(material, type, settings.fireproof());
    }

    public static boolean hasFullSuit(LivingEntity e) {
        return e.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.LAVA_SUIT_HELMET)
                && e.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.LAVA_SUIT_CHESTPLATE)
                && e.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.LAVA_SUIT_LEGGINGS)
                && e.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.LAVA_SUIT_BOOTS);
    }

    public static boolean hasHelmet(LivingEntity e) {
        return e.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.LAVA_SUIT_HELMET);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Full set bonus:").formatted(Formatting.GOLD));
        tooltip.add(Text.literal(" Fire Resistance").formatted(Formatting.RED));
        tooltip.add(Text.literal(" Swim incredibly fast in lava").formatted(Formatting.RED));
        tooltip.add(Text.literal(" See clearly inside lava").formatted(Formatting.RED));
    }
}
