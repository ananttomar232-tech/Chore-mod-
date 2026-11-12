package com.limitlessanant.heist.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** High-end display piece for the Weapon Cartel (provided "Sci-fi assault rifle PBR" model). Hang it on a weapon rack. */
public class AssaultRifleItem extends Item {
    public AssaultRifleItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Weapon Cartel exclusive - display only").formatted(Formatting.DARK_RED));
        tooltip.add(Text.literal("Price: 250,000 $").formatted(Formatting.GOLD));
    }
}
