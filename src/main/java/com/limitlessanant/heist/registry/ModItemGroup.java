package com.limitlessanant.heist.registry;

import com.limitlessanant.heist.LimitlessHeist;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroup {
    public static final ItemGroup HEIST = Registry.register(Registries.ITEM_GROUP, LimitlessHeist.id("heist"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.limitlessheist.heist"))
                    .icon(() -> new ItemStack(ModItems.MYSTERY_BOX))
                    .entries((ctx, entries) -> {
                        for (Item i : ModItems.TAB_ORDER) entries.add(i);
                    })
                    .build());

    public static void init() {
    }

    private ModItemGroup() {
    }
}
