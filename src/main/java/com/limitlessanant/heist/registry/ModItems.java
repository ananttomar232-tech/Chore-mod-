package com.limitlessanant.heist.registry;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.item.AssaultRifleItem;
import com.limitlessanant.heist.item.CrazyAppleItem;
import com.limitlessanant.heist.item.FreezeRayItem;
import com.limitlessanant.heist.item.GrapplingGunItem;
import com.limitlessanant.heist.item.HackerUsbItem;
import com.limitlessanant.heist.item.LavaSuitArmorItem;
import com.limitlessanant.heist.item.LavaSuitMaterial;
import com.limitlessanant.heist.item.SmokeBombItem;
import net.minecraft.block.Block;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    /** Everything shown in the creative tab, in order. */
    public static final List<Item> TAB_ORDER = new ArrayList<>();

    // ---- block items
    public static final Item HACKER_TERMINAL = block(ModBlocks.HACKER_TERMINAL, Rarity.EPIC);
    public static final Item BRIEFCASE = block(ModBlocks.BRIEFCASE, Rarity.RARE);
    public static final Item MONA_LISA = block(ModBlocks.MONA_LISA, Rarity.EPIC);
    public static final Item WEAPON_RACK = block(ModBlocks.WEAPON_RACK, Rarity.RARE);
    public static final Item NEON_SIGN = block(ModBlocks.NEON_SIGN, Rarity.UNCOMMON);
    public static final Item MYSTERY_BOX = block(ModBlocks.MYSTERY_BOX, Rarity.EPIC);
    public static final Item INFINITY_DRAGON_CORE = block(ModBlocks.INFINITY_DRAGON_CORE, Rarity.EPIC);
    public static final Item LASER_EMITTER = block(ModBlocks.LASER_EMITTER, Rarity.UNCOMMON);

    // ---- weapons & gadgets
    public static final Item SCI_FI_ASSAULT_RIFLE = register("sci_fi_assault_rifle",
            new AssaultRifleItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));
    public static final Item FREEZE_RAY_GUN = register("freeze_ray_gun",
            new FreezeRayItem(new Item.Settings().maxCount(1).maxDamage(256).rarity(Rarity.EPIC)));
    public static final Item GRAPPLING_HOOK_GUN = register("grappling_hook_gun",
            new GrapplingGunItem(new Item.Settings().maxCount(1).maxDamage(512).rarity(Rarity.RARE)));
    public static final Item HACKER_USB = register("hacker_usb",
            new HackerUsbItem(new Item.Settings().maxCount(16).rarity(Rarity.RARE)));
    public static final Item SMOKE_BOMB = register("smoke_bomb",
            new SmokeBombItem(new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)));
    public static final Item CRAZY_APPLE = register("crazy_apple", new CrazyAppleItem(new Item.Settings()
            .rarity(Rarity.EPIC)
            .food(new FoodComponent.Builder().hunger(6).saturationModifier(1.4f).alwaysEdible()
                    .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 300, 2), 1f)
                    .statusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 1), 1f)
                    .statusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 2400, 3), 1f)
                    .statusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0), 1f)
                    .build())));

    // ---- Lava Proof "Shark" Suit
    public static final Item LAVA_SUIT_HELMET = register("lava_suit_helmet",
            new LavaSuitArmorItem(LavaSuitMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Settings().rarity(Rarity.EPIC)));
    public static final Item LAVA_SUIT_CHESTPLATE = register("lava_suit_chestplate",
            new LavaSuitArmorItem(LavaSuitMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Settings().rarity(Rarity.EPIC)));
    public static final Item LAVA_SUIT_LEGGINGS = register("lava_suit_leggings",
            new LavaSuitArmorItem(LavaSuitMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Settings().rarity(Rarity.EPIC)));
    public static final Item LAVA_SUIT_BOOTS = register("lava_suit_boots",
            new LavaSuitArmorItem(LavaSuitMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Settings().rarity(Rarity.EPIC)));

    // ---- spawn eggs (registered in ModEntities.init once the entity types exist)
    public static Item VIP_GUARD_SPAWN_EGG;
    public static Item UNICORN_SPAWN_EGG;
    public static Item GOTIYA_SPAWN_EGG;

    static {
        for (Block b : ModBlocks.NEON_BLOCKS.values()) block(b, Rarity.COMMON);
        for (Block b : ModBlocks.NEON_STRIPS.values()) block(b, Rarity.COMMON);
    }

    public static void registerSpawnEggs() {
        VIP_GUARD_SPAWN_EGG = register("vip_guard_spawn_egg",
                new SpawnEggItem(ModEntities.VIP_GUARD, 0x111111, 0xd4af37, new Item.Settings()));
        UNICORN_SPAWN_EGG = register("unicorn_spawn_egg",
                new SpawnEggItem(ModEntities.UNICORN, 0x14101c, 0xff2bd6, new Item.Settings()));
        GOTIYA_SPAWN_EGG = register("gotiya_spawn_egg",
                new SpawnEggItem(ModEntities.GOTIYA, 0x8a5a3a, 0x3b2a5a, new Item.Settings()));
    }

    private static Item block(Block block, Rarity rarity) {
        Item item = new BlockItem(block, new Item.Settings().rarity(rarity));
        return register(Registries.BLOCK.getId(block).getPath(), item);
    }

    private static <T extends Item> T register(String name, T item) {
        Registry.register(Registries.ITEM, LimitlessHeist.id(name), item);
        TAB_ORDER.add(item);
        return item;
    }

    public static void init() {
    }

    private ModItems() {
    }
}
