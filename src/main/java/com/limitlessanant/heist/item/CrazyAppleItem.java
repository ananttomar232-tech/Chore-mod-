package com.limitlessanant.heist.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Dark enchanted golden apple. Tames the legendary unicorn; eating it gives Regen III, Strength II, Absorption IV, Night Vision. */
public class CrazyAppleItem extends Item {
    public CrazyAppleItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("The only thing the legendary unicorn trusts").formatted(Formatting.LIGHT_PURPLE));
    }
}
