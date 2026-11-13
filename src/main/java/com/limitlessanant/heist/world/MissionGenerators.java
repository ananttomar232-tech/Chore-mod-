package com.limitlessanant.heist.world;

import com.limitlessanant.heist.entity.GotiyaEntity;
import com.limitlessanant.heist.entity.UnicornEntity;
import com.limitlessanant.heist.registry.ModBlocks;
import com.limitlessanant.heist.registry.ModEntities;
import com.limitlessanant.heist.registry.ModItems;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallSkullBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Mission 1-3 locations and the thief's bunker. Each is built in front of (north of) the player who runs the command. */
public final class MissionGenerators {

    private static void clearBox(Canvas c, int x1, int y1, int z1, int x2, int y2, int z2, BlockState ground) {
        // clear up to 30 blocks above the roof too, so a hillside never buries the build and daylight gets in
        c.fill(x1, y1, z1, x2, y2 + 30, z2, Blocks.AIR.getDefaultState());
        c.fill(x1, y1 - 3, z1, x2, y1 - 2, z2, Blocks.STONE.getDefaultState());
        c.fill(x1, y1 - 1, z1, x2, y1 - 1, z2, ground);
    }

    // ============================================================================================ MISSION 1
    /**
     * THE 8K HIGH-SECURITY MUSEUM. White marble hall with a glass roof. The Mona Lisa hangs on the north wall behind a
     * laser corridor (static + pulsing beams). Two ways in: hack the terminal in the security room (east), or
     * grapple onto the gallery above the painting and drop down behind the lasers.
     */
    public static Canvas museum(ServerWorld w, BlockPos o) {
        Canvas c = new Canvas(w, o);
        int X = 20, Z1 = -36, Z2 = 0, top = 14;
        BlockState marble = Blocks.SMOOTH_QUARTZ.getDefaultState();
        BlockState pillar = Blocks.QUARTZ_PILLAR.getDefaultState();
        clearBox(c, -X - 2, 0, Z1 - 2, X + 2, top + 2, Z2 + 6, Blocks.POLISHED_DIORITE.getDefaultState());
        // floor: checker marble
        c.fill(-X, -1, Z1, X, -1, Z2, (x, y, z) -> ((x + z) & 1) == 0 ? Blocks.QUARTZ_BLOCK.getDefaultState() : Blocks.POLISHED_DIORITE.getDefaultState());
        c.walls(-X, 0, Z1, X, top, Z2, marble);
        for (int x = -X; x <= X; x += 4) { // pillars on the facade
            c.fill(x, 0, Z2, x, top, Z2, pillar);
            c.fill(x, 0, Z1, x, top, Z1, pillar);
        }
        // glass roof with a marble grid + ceiling lights
        c.fill(-X, top, Z1, X, top, Z2, (x, y, z) -> (x % 5 == 0 || z % 6 == 0) ? Blocks.QUARTZ_BRICKS.getDefaultState() : Blocks.GLASS.getDefaultState());
        c.fill(-X, top - 1, Z1, X, top - 1, Z2, (x, y, z) -> (Math.floorMod(x, 5) == 2 && Math.floorMod(z, 6) == 3) ? Blocks.SEA_LANTERN.getDefaultState() : null);
        for (int z = Z1 + 3; z <= Z2 - 3; z += 6) { // gallery wall lights
            c.set(-X + 1, 5, z, Blocks.SEA_LANTERN.getDefaultState());
            c.set(X - 1, 5, z, Blocks.SEA_LANTERN.getDefaultState());
        }
        // entrance
        c.fill(-2, 0, Z2, 2, 5, Z2, Blocks.AIR.getDefaultState());
        c.fill(-3, 6, Z2, 3, 6, Z2, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
        c.fill(-2, -1, Z2 + 1, 2, -1, Z2 + 5, Blocks.RED_CONCRETE.getDefaultState());
        c.guard(-4, 0, Z2 + 3, 0);
        c.guard(4, 0, Z2 + 3, 0);
        // exhibits
        BlockState[] treasures = {Blocks.DIAMOND_BLOCK.getDefaultState(), Blocks.EMERALD_BLOCK.getDefaultState(), Blocks.GOLD_BLOCK.getDefaultState(),
                Blocks.AMETHYST_BLOCK.getDefaultState(), Blocks.LAPIS_BLOCK.getDefaultState(), Blocks.NETHERITE_BLOCK.getDefaultState()};
        int i = 0;
        for (int x = -14; x <= 14; x += 28) {
            for (int z = -6; z >= -20; z -= 7) {
                c.set(x, 0, z, Blocks.CHISELED_QUARTZ_BLOCK.getDefaultState());
                c.set(x, 1, z, pillar);
                c.set(x, 2, z, treasures[i++ % treasures.length]);
                c.set(x, 3, z, Blocks.GLASS.getDefaultState());
                final int px = x, pz = z;
                c.fill(x - 1, 0, z - 1, x + 1, 0, z + 1, (xx, yy, zz) -> (xx == px && zz == pz) ? null : Blocks.RED_CARPET.getDefaultState());
            }
        }
        // ---- THE MONA LISA + laser corridor
        int wallZ = Z1 + 1;                               // block in front of the north wall
        c.prop(0, 2, wallZ, ModBlocks.MONA_LISA, Direction.SOUTH);
        c.fill(-7, 0, wallZ, -7, 7, wallZ + 12, marble);  // corridor walls
        c.fill(7, 0, wallZ, 7, 7, wallZ + 12, marble);
        c.fill(-7, 8, wallZ, 7, 8, wallZ + 3, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState()); // gallery above the painting
        c.fill(-7, 9, wallZ + 3, 7, 9, wallZ + 3, Blocks.IRON_BARS.getDefaultState());
        c.fill(-2, 8, wallZ, 2, 8, wallZ + 1, Blocks.AIR.getDefaultState());             // drop hole behind the beams
        for (int z = wallZ + 3; z <= wallZ + 12; z += 2) {   // horizontal beams across the corridor
            for (int y = 1; y <= 5; y += 2) {
                int yy = y + ((z / 2) & 1);
                c.laser(-6, yy, z, Direction.EAST, (z + y) % 4 == 0);
            }
        }
        for (int x = -5; x <= 5; x += 5) c.laser(x, 0, wallZ + 6, Direction.UP, false); // vertical beams
        c.fill(-6, 7, wallZ + 2, 6, 7, wallZ + 12, Blocks.SMOOTH_QUARTZ.getDefaultState()); // corridor ceiling stops vertical beams
        c.guard(0, 0, wallZ + 15, 180);
        c.guard(-12, 0, -27, 90);
        c.guard(12, 0, -27, 270);
        // ---- security room (east): the hacker terminal shuts the grid down
        c.fill(11, 0, -12, 19, 5, -2, Blocks.AIR.getDefaultState());
        c.walls(11, 0, -12, 19, 5, -2, Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState());
        c.fill(11, 0, -8, 11, 2, -7, Blocks.AIR.getDefaultState());
        c.prop(16, 0, -8, ModBlocks.HACKER_TERMINAL, Direction.WEST);
        c.neonWall(11, 4, -10, Direction.WEST, "watching", 0.5f);
        for (int[] p : new int[][]{{-X + 1, Z1 + 1}, {X - 1, Z1 + 1}, {-X + 1, Z2 - 1}, {X - 1, Z2 - 1}}) { // "cameras"
            c.set(p[0], top - 2, p[1], Blocks.OBSERVER.getDefaultState());
        }
        return c;
    }

    // ============================================================================================ MISSION 2
    /** Gotiya's ranch: he sleeps in the barn, his legendary unicorn waits in the dark paddock. Crazy apple under his "pillow". */
    public static Canvas stable(ServerWorld w, BlockPos o) {
        Canvas c = new Canvas(w, o);
        clearBox(c, -14, 0, -32, 14, 12, 2, Blocks.GRASS_BLOCK.getDefaultState());
        // paddock (front)
        c.fill(-12, -1, -14, 12, -1, 0, (x, y, z) -> (x * 7 + z * 13) % 5 == 0 ? Blocks.MOSS_BLOCK.getDefaultState() : Blocks.GRASS_BLOCK.getDefaultState());
        c.walls(-12, 0, -14, 12, 0, 0, Blocks.DARK_OAK_FENCE.getDefaultState());
        c.set(0, 0, 0, Blocks.DARK_OAK_FENCE_GATE.getDefaultState().with(FenceGateBlock.FACING, Direction.SOUTH));
        for (int[] p : new int[][]{{-8, -4}, {7, -10}, {-5, -11}, {9, -3}}) {
            c.set(p[0], -1, p[1], Blocks.AMETHYST_BLOCK.getDefaultState());
            c.set(p[0], 0, p[1], Blocks.AMETHYST_CLUSTER.getDefaultState());
        }
        for (int x = -12; x <= 12; x += 6) {
            c.set(x, 1, 0, Blocks.SOUL_LANTERN.getDefaultState());
            c.set(x, 1, -14, Blocks.SOUL_LANTERN.getDefaultState());
        }
        c.entity(0.5, 0, -7.5, 180, sw -> ModEntities.UNICORN.create(sw), (UnicornEntity u) -> u.setPersistent());
        // barn (back)
        int z1 = -30, z2 = -17;
        BlockState plank = Blocks.DARK_OAK_PLANKS.getDefaultState();
        c.fill(-9, -1, z1, 9, -1, z2, Blocks.SPRUCE_PLANKS.getDefaultState());
        c.walls(-9, 0, z1, 9, 5, z2, (x, y, z) -> (Math.abs(x) == 9 && (z == z1 || z == z2)) ? Blocks.DARK_OAK_LOG.getDefaultState() : plank);
        for (int k = 0; k <= 5; k++) { // gable roof
            int y = 6 + k;
            c.fill(-10 + k, y, z1 - 1, -10 + k, y, z2 + 1, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
            c.fill(10 - k, y, z1 - 1, 10 - k, y, z2 + 1, Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
            if (k < 5) {
                c.fill(-9 + k, y, z1, 9 - k, y, z1, plank);
                c.fill(-9 + k, y, z2, 9 - k, y, z2, plank);
            }
        }
        c.fill(-5, 6, z1 - 1, 5, 6, z2 + 1, Blocks.AIR.getDefaultState());
        c.fill(-4, 11, z1 - 1, 4, 11, z2 + 1, Blocks.SPRUCE_SLAB.getDefaultState());
        c.fill(-1, 0, z2, 1, 3, z2, Blocks.AIR.getDefaultState()); // door
        // Gotiya's bed + "pillow" chest with the crazy apple
        BlockPos foot = new BlockPos(-6, 0, z1 + 3);
        c.set(foot.getX(), 0, foot.getZ(), Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT));
        c.set(foot.getX(), 0, foot.getZ() - 1, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD));
        c.chest(-7, 0, z1 + 1, Direction.EAST, new ItemStack(ModItems.CRAZY_APPLE), new ItemStack(Items.BREAD, 3));
        c.entity(-5.5, 0, z1 + 2.5, 180, sw -> ModEntities.GOTIYA.create(sw), (GotiyaEntity g) -> {
        });
        // hay + decor
        c.fill(4, 0, z1 + 1, 8, 1, z1 + 3, Blocks.HAY_BLOCK.getDefaultState());
        c.set(8, 2, z1 + 1, Blocks.HAY_BLOCK.getDefaultState());
        c.set(0, 0, z1 + 1, Blocks.CRAFTING_TABLE.getDefaultState());
        for (int x = -6; x <= 6; x += 6) {
            for (int z = z1 + 3; z <= z2 - 3; z += 4) c.set(x, 5, z, Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true));
        }
        c.neonWall(0, 4, z2 + 1, Direction.SOUTH, "hotel", 0.5f); // Gotiya's "hotel" (he never wakes up)
        c.set(10, 0, -16, ModBlocks.MYSTERY_BOX.getDefaultState());
        return c;
    }

    // ============================================================================================ MISSION 3
    /** Obsidian vault: the Infinity Dragon Core floats on a pedestal inside a ring of pulsing lasers, four VIP golems guard it. */
    public static Canvas dragonVault(ServerWorld w, BlockPos o) {
        Canvas c = new Canvas(w, o);
        int R = 12, top = 13, cz = -R - 3;
        c.fill(-R - 1, -2, cz - R - 1, R + 1, top + 1, cz + R + 1, Blocks.OBSIDIAN.getDefaultState());
        c.fill(-R, 0, cz - R, R, top - 1, cz + R, (x, y, z) -> {
            int dx = Math.abs(x), dz = Math.abs(z - cz);
            return dx + dz > R + 5 ? null : Blocks.AIR.getDefaultState(); // octagonal room
        });
        c.fill(-R, -1, cz - R, R, -1, cz + R, (x, y, z) -> {
            int dx = Math.abs(x), dz = Math.abs(z - cz);
            if (dx + dz > R + 5) return null;
            return ((dx + dz) % 4 == 0) ? Blocks.PURPUR_BLOCK.getDefaultState() : Blocks.END_STONE_BRICKS.getDefaultState();
        });
        c.fill(-R, top - 1, cz - R, R, top - 1, cz + R, (x, y, z) -> (Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) ? Blocks.CRYING_OBSIDIAN.getDefaultState() : null);
        // pedestal + core
        c.fill(-1, 0, cz - 1, 1, 0, cz + 1, Blocks.PURPUR_PILLAR.getDefaultState());
        c.set(0, 1, cz, Blocks.PURPUR_PILLAR.getDefaultState());
        c.set(0, 2, cz, ModBlocks.INFINITY_DRAGON_CORE.getDefaultState());
        for (int k = 0; k < 8; k++) { // pulsing laser ring
            double a = k * Math.PI / 4;
            c.laser((int) Math.round(Math.cos(a) * 4), 0, cz + (int) Math.round(Math.sin(a) * 4), Direction.UP, k % 2 == 0);
        }
        for (int[] p : new int[][]{{-7, cz - 7}, {7, cz - 7}, {-7, cz + 7}, {7, cz + 7}}) c.guard(p[0], 0, p[1], 0);
        // dragon heads + neon
        c.set(0, 6, cz - R + 1, Blocks.DRAGON_WALL_HEAD.getDefaultState().with(WallSkullBlock.FACING, Direction.SOUTH));
        c.set(-R + 5, 6, cz, Blocks.DRAGON_WALL_HEAD.getDefaultState().with(WallSkullBlock.FACING, Direction.EAST));
        c.set(R - 5, 6, cz, Blocks.DRAGON_WALL_HEAD.getDefaultState().with(WallSkullBlock.FACING, Direction.WEST));
        c.neonWall(0, 9, cz - R + 1, Direction.SOUTH, "skull", 1f);
        // entrance + freeze ray rack outside
        c.fill(-1, 0, cz + R, 1, 3, cz + R + 1, Blocks.AIR.getDefaultState());
        c.fill(-2, 4, cz + R + 1, 2, 4, cz + R + 1, Blocks.CRYING_OBSIDIAN.getDefaultState());
        c.weaponRack(-4, 1, cz + R + 2, Direction.SOUTH, new ItemStack(ModItems.FREEZE_RAY_GUN), new ItemStack(ModItems.SMOKE_BOMB, 4));
        c.neonWall(3, 3, cz + R + 2, Direction.SOUTH, "heist", 0.5f);
        return c;
    }

    // ============================================================================================ BUNKER
    public enum Tier {
        GOLD(Blocks.GOLD_BLOCK, Blocks.YELLOW_STAINED_GLASS, 11, 8, DyeColor.YELLOW),
        DIAMOND(Blocks.DIAMOND_BLOCK, Blocks.LIGHT_BLUE_STAINED_GLASS, 15, 11, DyeColor.CYAN),
        NETHERITE(Blocks.NETHERITE_BLOCK, Blocks.BLACK_STAINED_GLASS, 21, 15, DyeColor.MAGENTA);

        final net.minecraft.block.Block wall, glass;
        final int width, depth;
        final DyeColor neon;

        Tier(net.minecraft.block.Block wall, net.minecraft.block.Block glass, int width, int depth, DyeColor neon) {
            this.wall = wall;
            this.glass = glass;
            this.width = width;
            this.depth = depth;
            this.neon = neon;
        }
    }

    /** The thief's bunker: rebuild it over the old one to upgrade Gold -> Diamond -> Netherite. Trophy spots for the 3 missions. */
    public static Canvas bunker(ServerWorld w, BlockPos o, Tier t) {
        Canvas c = new Canvas(w, o);
        int hx = t.width / 2, d = t.depth, h = t == Tier.NETHERITE ? 7 : 5;
        c.fill(-hx - 1, -1, -d - 2, hx + 1, h + 1, 0, t.wall.getDefaultState());
        c.fill(-hx, 0, -d - 1, hx, h, -1, Blocks.AIR.getDefaultState());
        c.fill(-hx, -1, -d - 1, hx, -1, -1, (x, y, z) -> ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE.getDefaultState() : t.wall.getDefaultState());
        c.fill(-hx, h, -d - 1, hx, h, -1, (x, y, z) -> (x % 3 == 0 && z % 3 == 0) ? (t == Tier.GOLD ? Blocks.GLOWSTONE.getDefaultState() : Blocks.SEA_LANTERN.getDefaultState()) : null);
        c.fill(-1, 0, 0, 1, 2, 0, Blocks.AIR.getDefaultState()); // door
        c.fill(-hx, 2, -d - 2, hx, 3, -d - 2, (x, y, z) -> Math.abs(x) % 4 == 1 ? t.glass.getDefaultState() : null);
        // trophy wall: Mona Lisa spot / unicorn corner / core pedestal
        c.set(0, -1, -d, Blocks.GOLD_BLOCK.getDefaultState());
        c.set(0, 0, -d, Blocks.QUARTZ_PILLAR.getDefaultState());       // put the Dragon Core here
        c.set(-hx + 2, 0, -d, Blocks.QUARTZ_PILLAR.getDefaultState());  // Mona Lisa goes on the wall above
        c.set(hx - 2, 0, -d, Blocks.HAY_BLOCK.getDefaultState());       // unicorn snack
        c.set(-hx, 0, -2, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.FOOT));
        c.set(-hx, 0, -3, Blocks.RED_BED.getDefaultState().with(BedBlock.FACING, Direction.NORTH).with(BedBlock.PART, BedPart.HEAD));
        c.chest(hx, 0, -2, Direction.WEST);
        c.chest(hx, 0, -3, Direction.WEST);
        if (t != Tier.GOLD) {
            c.weaponRack(hx, 1, -d / 2, Direction.WEST);
            c.neonStrip(-hx, h - 1, -d - 1, hx, h - 1, -d - 1, t.neon);
            c.neonStrip(-hx, h - 1, -1, hx, h - 1, -1, t.neon);
        }
        if (t == Tier.NETHERITE) {
            c.prop(-hx + 3, 0, -4, ModBlocks.HACKER_TERMINAL, Direction.EAST);
            c.neonWall(0, h - 2, -d - 1, Direction.SOUTH, "limitless", 1f);
            c.set(3, 0, -d + 2, ModBlocks.MYSTERY_BOX.getDefaultState());
        } else {
            c.neonWall(0, h - 2, -d - 1, Direction.SOUTH, t == Tier.GOLD ? "dollar" : "diamond", 0.5f);
        }
        return c;
    }

    private MissionGenerators() {
    }
}
