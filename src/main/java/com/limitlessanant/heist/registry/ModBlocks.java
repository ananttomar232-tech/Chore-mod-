package com.limitlessanant.heist.registry;

import com.limitlessanant.heist.LimitlessHeist;
import com.limitlessanant.heist.block.DragonCoreBlock;
import com.limitlessanant.heist.block.HackerTerminalBlock;
import com.limitlessanant.heist.block.LaserEmitterBlock;
import com.limitlessanant.heist.block.MeshPropBlock;
import com.limitlessanant.heist.block.MonaLisaBlock;
import com.limitlessanant.heist.block.MysteryBoxBlock;
import com.limitlessanant.heist.block.NeonSignBlock;
import com.limitlessanant.heist.block.WeaponRackBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.DyeColor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    // ---- props built from the provided 3D files (rendered as meshes by block entity renderers)
    public static final Block HACKER_TERMINAL = register("hacker_terminal", new HackerTerminalBlock(
            FabricBlockSettings.create().mapColor(MapColor.BLACK).strength(2f, 6f).nonOpaque().luminance(9).sounds(BlockSoundGroup.METAL)));
    public static final Block BRIEFCASE = register("briefcase", new MeshPropBlock(
            FabricBlockSettings.create().mapColor(MapColor.BLACK).strength(0.6f).nonOpaque().sounds(BlockSoundGroup.WOOL),
            Block.createCuboidShape(1, 0, 5.5, 15, 13, 10.5)));
    public static final Block MONA_LISA = register("mona_lisa", new MonaLisaBlock(
            FabricBlockSettings.create().mapColor(MapColor.BROWN).strength(0.8f).nonOpaque().sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block WEAPON_RACK = register("weapon_rack", new WeaponRackBlock(
            FabricBlockSettings.create().mapColor(MapColor.BLACK).strength(3f, 8f).nonOpaque().luminance(6).sounds(BlockSoundGroup.METAL)));

    // ---- neon
    public static final Block NEON_SIGN = register("neon_sign", new NeonSignBlock(
            FabricBlockSettings.create().mapColor(MapColor.PINK).strength(0.5f).nonOpaque().noCollision().luminance(15).sounds(BlockSoundGroup.GLASS)));
    public static final Map<DyeColor, Block> NEON_BLOCKS = new LinkedHashMap<>();
    public static final Map<DyeColor, Block> NEON_STRIPS = new LinkedHashMap<>();
    public static final DyeColor[] NEON_COLORS = {DyeColor.MAGENTA, DyeColor.PINK, DyeColor.PURPLE, DyeColor.LIGHT_BLUE,
            DyeColor.CYAN, DyeColor.LIME, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.RED, DyeColor.WHITE};

    // ---- custom mechanics
    public static final Block MYSTERY_BOX = register("mystery_box", new MysteryBoxBlock(
            FabricBlockSettings.create().mapColor(MapColor.BLACK).strength(1.2f).nonOpaque().luminance(10).sounds(BlockSoundGroup.NETHERITE)));
    public static final Block INFINITY_DRAGON_CORE = register("infinity_dragon_core", new DragonCoreBlock(
            FabricBlockSettings.create().mapColor(MapColor.PURPLE).strength(5f, 1200f).nonOpaque().luminance(15).sounds(BlockSoundGroup.AMETHYST_BLOCK)));
    public static final Block LASER_EMITTER = register("laser_emitter", new LaserEmitterBlock(
            FabricBlockSettings.create().mapColor(MapColor.RED).strength(3f, 10f).nonOpaque().luminance(7).sounds(BlockSoundGroup.METAL)));

    static {
        for (DyeColor c : NEON_COLORS) {
            NEON_BLOCKS.put(c, register("neon_block_" + c.getName(), new Block(
                    FabricBlockSettings.create().mapColor(c).strength(0.4f).luminance(15).sounds(BlockSoundGroup.GLASS))));
            NEON_STRIPS.put(c, register("neon_strip_" + c.getName(), new com.limitlessanant.heist.block.NeonStripBlock(
                    FabricBlockSettings.create().mapColor(c).strength(0.2f).nonOpaque().noCollision().luminance(15).sounds(BlockSoundGroup.GLASS))));
        }
    }

    private static <T extends Block> T register(String name, T block) {
        return Registry.register(Registries.BLOCK, LimitlessHeist.id(name), block);
    }

    public static void init() {
    }

    private ModBlocks() {
    }
}
