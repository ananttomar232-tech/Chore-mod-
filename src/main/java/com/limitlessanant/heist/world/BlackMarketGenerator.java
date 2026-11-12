package com.limitlessanant.heist.world;

import com.limitlessanant.heist.registry.ModBlocks;
import com.limitlessanant.heist.registry.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.WallMountLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

/**
 * THE 8K VIP UNDERGROUND BLACK MARKET
 * <pre>
 *   surface  ── scaffold shaft (the secret entrance, where you ran the command)
 *                │
 *   ┌────────────┴─────────── colossal cavern 120 x 120 x 44, rough deepslate walls ─────────────┐
 *   │  neon chandeliers + giant hanging signs from the ceiling                                   │
 *   │   [shop][shop][shop]          ┌──────────────┐          [shop][shop][shop]                 │
 *   │                              │ VIP TOWER    │  roof:  giant BLACK MARKET sign            │
 *   │   [shop]                     │ 4 Penthouse  │                               [shop]       │
 *   │   [shop]   neon curb ring    │ 3 Hacker Den │   glass facades, gold trim    [shop]       │
 *   │   [shop]                     │ 2 Weapon Cartel                             [shop]       │
 *   │                              │ 1 Dealer's Lounge (briefcase table)                       │
 *   │   [shop][shop][shop]          └────║door║────┘  red carpet, VIP golem bouncers            │
 *   └────────────────────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 * Origin = plaza centre at floor level. Everything is plain vanilla blocks + the mod's props, so it can be edited freely.
 */
public final class BlackMarketGenerator {
    public static final int RX = 60, RZ = 60, H = 44;
    private static final int T_HALF = 15;       // tower footprint -15..15
    private static final int FLOOR_H = 9;       // floor-to-floor height
    private static final int FLOORS = 4;

    public static Canvas build(ServerWorld world, BlockPos origin, int surfaceY, int shaftX, int shaftZ) {
        Canvas c = new Canvas(world, origin);
        Random rnd = Random.create(origin.asLong());
        carveCavern(c);
        plaza(c);
        tower(c);
        shops(c);
        ceiling(c, rnd);
        entranceShaft(c, shaftX, shaftZ, surfaceY - origin.getY());
        return c;
    }

    /** Height of the cavern ceiling above the floor at (x,z) (superellipse dome). */
    public static double ceilingAt(double x, double z) {
        double h = 1 - Math.pow(Math.abs(x) / RX, 4) - Math.pow(Math.abs(z) / RZ, 4);
        return h <= 0 ? -1 : H * Math.sqrt(h);
    }

    // ------------------------------------------------------------------------------------------------ cavern
    private static double noise(int x, int y, int z) {
        long h = x * 73856093L ^ y * 19349663L ^ z * 83492791L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        return ((h ^ (h >>> 15)) & 0xffff) / 65535.0;
    }

    private static double smoothNoise(int x, int y, int z) { // cheap 4-block value noise
        int gx = Math.floorDiv(x, 4), gy = Math.floorDiv(y, 4), gz = Math.floorDiv(z, 4);
        double fx = (x - gx * 4) / 4.0, fy = (y - gy * 4) / 4.0, fz = (z - gz * 4) / 4.0;
        double v = 0;
        for (int i = 0; i < 8; i++) {
            int dx = i & 1, dy = (i >> 1) & 1, dz = (i >> 2) & 1;
            double w = (dx == 1 ? fx : 1 - fx) * (dy == 1 ? fy : 1 - fy) * (dz == 1 ? fz : 1 - fz);
            v += w * noise(gx + dx, gy + dy, gz + dz);
        }
        return v;
    }

    /** f < 1 inside the cavern. */
    private static double shape(int x, int y, int z, double grow) {
        double n = 1 + (smoothNoise(x, y, z) - 0.5) * 0.10;
        double rx = RX * grow * n, rz = RZ * grow * n, h = H * grow * n;
        return Math.pow(Math.abs(x) / rx, 4) + Math.pow(Math.abs(z) / rz, 4) + Math.pow(Math.max(0, y) / h, 2);
    }

    private static void carveCavern(Canvas c) {
        BlockState air = Blocks.AIR.getDefaultState();
        BlockState lining1 = Blocks.POLISHED_DEEPSLATE.getDefaultState();
        BlockState lining2 = Blocks.BLACKSTONE.getDefaultState();
        BlockState base = Blocks.DEEPSLATE_TILES.getDefaultState();
        ServerWorld w = c.world;
        BlockPos o = c.origin;
        BlockPos.Mutable m = new BlockPos.Mutable();
        c.fill(-RX - 4, -3, -RZ - 4, RX + 4, H + 4, RZ + 4, (x, y, z) -> {
            if (y < 0) { // foundation under the whole footprint
                return shape(x, 0, z, 1.06) < 1 ? (y == -1 ? null : base) : null;
            }
            double f = shape(x, y, z, 1.0);
            if (f < 1) return air;
            if (shape(x, y, z, 1.07) < 1) { // the shell: make it solid and stable (no lava / water / gravel leaking in)
                BlockState cur = w.getBlockState(m.set(o.getX() + x, o.getY() + y, o.getZ() + z));
                if (cur.isAir() || !cur.getFluidState().isEmpty() || cur.getBlock() instanceof FallingBlock || !cur.isOpaqueFullCube(w, m)) {
                    return noise(x, y, z) < 0.5 ? lining1 : lining2;
                }
            }
            return null;
        });
    }

    // ------------------------------------------------------------------------------------------------ plaza
    private static void plaza(Canvas c) {
        BlockState black = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState tile = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState light = Blocks.SEA_LANTERN.getDefaultState();
        c.fill(-RX - 2, -1, -RZ - 2, RX + 2, -1, RZ + 2, (x, y, z) -> {
            if (shape(x, 0, z, 1.03) >= 1) return null;
            if (Math.abs(x) <= T_HALF + 1 && Math.abs(z) <= T_HALF + 1) return null; // tower lobby floor
            boolean lineX = Math.floorMod(x, 8) == 0, lineZ = Math.floorMod(z, 8) == 0;
            if (lineX && lineZ) return light;              // floor lights at every grid crossing
            if (lineX || lineZ) return gold;               // gold grid
            return ((x + z) & 1) == 0 ? black : tile;      // fine black checker
        });
        // red carpet from the tower door to the shaft side
        c.fill(-2, 0, T_HALF + 1, 2, 0, T_HALF + 14, Blocks.RED_CARPET.getDefaultState());
        // neon curb ring around the tower plaza + lamp posts
        int r = 22;
        c.neonStrip(-r, 0, -r, r, 0, -r, DyeColor.MAGENTA);
        c.neonStrip(-r, 0, r, -3, 0, r, DyeColor.MAGENTA);
        c.neonStrip(3, 0, r, r, 0, r, DyeColor.MAGENTA);
        c.neonStrip(-r, 0, -r + 1, -r, 0, r - 1, DyeColor.CYAN);
        c.neonStrip(r, 0, -r + 1, r, 0, r - 1, DyeColor.CYAN);
        for (int i = -r; i <= r; i += 11) {
            lampPost(c, i, -r - 2);
            lampPost(c, i, r + 2);
            lampPost(c, -r - 2, i);
            lampPost(c, r + 2, i);
        }
        // mystery boxes in the plaza corners
        for (int[] p : new int[][]{{-19, -19}, {19, -19}, {-19, 19}, {19, 19}}) {
            c.set(p[0], 0, p[1], ModBlocks.MYSTERY_BOX.getDefaultState());
        }
    }

    private static void lampPost(Canvas c, int x, int z) {
        c.fill(x, 0, z, x, 4, z, Blocks.POLISHED_BLACKSTONE_WALL.getDefaultState());
        c.set(x, 5, z, Blocks.GOLD_BLOCK.getDefaultState());
        c.set(x, 6, z, Blocks.SEA_LANTERN.getDefaultState());
        c.set(x, 7, z, Blocks.END_ROD.getDefaultState());
    }

    // ------------------------------------------------------------------------------------------------ tower
    private static void tower(Canvas c) {
        int a = T_HALF, top = FLOORS * FLOOR_H;
        BlockState frame = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState slab = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState glass = Blocks.GRAY_STAINED_GLASS.getDefaultState(); // smoky "pristine" glass, no white frame grid
        BlockState band = Blocks.BLACK_STAINED_GLASS.getDefaultState();
        BlockState gold = Blocks.GOLD_BLOCK.getDefaultState();
        // lobby floor: white marble with gold inlay
        c.fill(-a, -1, -a, a, -1, a, (x, y, z) -> (x % 6 == 0 || z % 6 == 0) ? gold
                : ((x + z) & 1) == 0 ? Blocks.QUARTZ_BLOCK.getDefaultState() : Blocks.SMOOTH_QUARTZ.getDefaultState());
        // glass facade + black band at the top of every floor
        c.walls(-a, 0, -a, a, top - 1, a, (x, y, z) -> (y % FLOOR_H == FLOOR_H - 1) ? band : glass);
        // vertical frame every 6 blocks
        for (int i = -a; i <= a; i += 6) {
            c.fill(i, 0, -a, i, top, -a, frame);
            c.fill(i, 0, a, i, top, a, frame);
            c.fill(-a, 0, i, -a, top, i, frame);
            c.fill(a, 0, i, a, top, i, frame);
        }
        // floor slabs with gold edge + neon strip outside
        for (int f = 1; f <= FLOORS; f++) {
            int y = f * FLOOR_H - 1;
            c.fill(-a, y, -a, a, y, a, (x, yy, z) -> (Math.abs(x) == a || Math.abs(z) == a) ? gold
                    : (Math.floorMod(x, 4) == 2 && Math.floorMod(z, 4) == 2) ? Blocks.SEA_LANTERN.getDefaultState() : slab); // lit ceilings
            DyeColor col = f % 2 == 0 ? DyeColor.CYAN : DyeColor.MAGENTA;
            if (f < FLOORS) {
                c.neonStrip(-a, y, -a - 1, a, y, -a - 1, col);
                c.neonStrip(-a, y, a + 1, a, y, a + 1, col);
                c.neonStrip(-a - 1, y, -a, -a - 1, y, a, col);
                c.neonStrip(a + 1, y, -a, a + 1, y, a, col);
            }
        }
        // roof parapet
        c.walls(-a, top, -a, a, top, a, gold);
        c.walls(-a, top + 1, -a, a, top + 1, a, Blocks.BLACK_STAINED_GLASS.getDefaultState());
        // staircase along the west wall, every floor (north -> south)
        for (int f = 0; f < FLOORS - 1; f++) {
            int y0 = f * FLOOR_H;
            c.fill(-a + 1, y0 + FLOOR_H - 1, -a + 2, -a + 3, y0 + FLOOR_H - 1, -a + 2 + FLOOR_H, Blocks.AIR.getDefaultState());
            for (int k = 0; k < FLOOR_H; k++) {
                c.fill(-a + 1, y0 + k, -a + 2 + k, -a + 3, y0 + k, -a + 2 + k,
                        Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
            }
        }
        // entrance (south)
        c.fill(-2, 0, a, 2, 4, a, Blocks.AIR.getDefaultState());
        c.fill(-3, 5, a, 3, 5, a, gold);
        c.fill(-3, 0, a, -3, 5, a, gold);
        c.fill(3, 0, a, 3, 5, a, gold);
        c.neon(0, 6, a + 1, Direction.SOUTH, WallMountLocation.WALL, "vip_only", 1f, false);
        c.guard(-4, 0, a + 3, 0);
        c.guard(4, 0, a + 3, 0);

        dealersLounge(c, 0);
        weaponCartel(c, FLOOR_H);
        hackersDen(c, FLOOR_H * 2);
        penthouse(c, FLOOR_H * 3);

        // facade signs
        c.neonWall(0, FLOOR_H * 3 + 4, -a - 1, Direction.NORTH, "black_market", 4f);
        c.neonWall(a + 1, FLOOR_H + 4, 0, Direction.EAST, "weapon_cartel", 2f);
        c.neonWall(-a - 1, FLOOR_H * 2 + 4, 0, Direction.WEST, "hackers_den", 2f);
        c.neonWall(0, 2 + FLOOR_H, a + 1, Direction.SOUTH, "dealers_lounge", 2f);
        c.neonWall(a + 1, FLOOR_H * 3 + 4, 0, Direction.EAST, "limitless", 2f);
        c.neon(0, top + 2, 0, Direction.SOUTH, WallMountLocation.FLOOR, "black_market", 4f, false);
        c.neon(0, top + 2, -2, Direction.NORTH, WallMountLocation.FLOOR, "limitless", 3f, false);
    }

    private static void chandelier(Canvas c, int x, int ceilY, int z, int len, BlockState light) {
        c.fill(x, ceilY - len, z, x, ceilY - 1, z, Blocks.CHAIN.getDefaultState());
        c.set(x, ceilY - len - 1, z, light);
    }

    private static void dealersLounge(Canvas c, int y0) {
        int ceil = y0 + FLOOR_H - 1;
        // main dealer table with the briefcase
        c.fill(-3, y0, -7, 3, y0, -6, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        c.fill(-3, y0 + 1, -7, 3, y0 + 1, -6, Blocks.GREEN_CARPET.getDefaultState());
        c.set(0, y0 + 1, -6, Blocks.AIR.getDefaultState());
        c.prop(0, y0 + 1, -6, ModBlocks.BRIEFCASE, Direction.SOUTH);
        c.set(-2, y0 + 1, -7, Blocks.GOLD_BLOCK.getDefaultState()); // stacks of "gold bars"
        c.set(2, y0 + 1, -7, Blocks.EMERALD_BLOCK.getDefaultState());
        for (int x = -2; x <= 2; x += 2) {
            c.set(x, y0, -4, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH));
        }
        c.set(0, y0, -9, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH)); // the dealer's seat
        // card tables
        for (int[] t : new int[][]{{-9, 3}, {9, 3}, {-9, 9}, {9, 9}}) {
            c.fill(t[0] - 1, y0, t[1], t[0] + 1, y0, t[1] + 1, Blocks.POLISHED_DEEPSLATE.getDefaultState());
            c.fill(t[0] - 1, y0 + 1, t[1], t[0] + 1, y0 + 1, t[1] + 1, Blocks.GREEN_CARPET.getDefaultState());
            c.set(t[0] - 2, y0, t[1], Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
            c.set(t[0] + 2, y0, t[1] + 1, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        }
        // bar along the east wall
        c.fill(11, y0, -10, 11, y0, 6, Blocks.QUARTZ_BRICKS.getDefaultState());
        c.fill(11, y0 + 1, -10, 11, y0 + 1, 6, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        c.neonWall(14, y0 + 4, -2, Direction.WEST, "beer", 0.5f);
        c.neonWall(14, y0 + 4, 3, Direction.WEST, "cash_only", 1f);
        // gold chandeliers
        for (int[] p : new int[][]{{0, -6}, {-8, 6}, {8, 6}, {0, 8}}) {
            chandelier(c, p[0], ceil, p[1], 2, Blocks.SHROOMLIGHT.getDefaultState());
        }
        c.neonWall(0, y0 + 4, -14, Direction.SOUTH, "dealers_lounge", 1f);
    }

    private static void weaponCartel(Canvas c, int y0) {
        int ceil = y0 + FLOOR_H - 1;
        c.fill(-14, y0, -14, 14, y0, 14, (x, y, z) -> (x + z) % 2 == 0 ? Blocks.RED_CARPET.getDefaultState() : Blocks.BLACK_CARPET.getDefaultState());
        // three wall racks on the north wall with the cartel's best pieces
        ItemStack rifle = new ItemStack(ModItems.SCI_FI_ASSAULT_RIFLE);
        c.weaponRack(-7, y0 + 2, -14, Direction.SOUTH, rifle, rifle, rifle);
        c.weaponRack(0, y0 + 2, -14, Direction.SOUTH, new ItemStack(ModItems.FREEZE_RAY_GUN), rifle, new ItemStack(ModItems.GRAPPLING_HOOK_GUN));
        c.weaponRack(7, y0 + 2, -14, Direction.SOUTH, rifle, rifle, rifle);
        // display pedestals
        for (int x = -6; x <= 6; x += 6) {
            c.set(x, y0, 0, Blocks.QUARTZ_PILLAR.getDefaultState());
            c.set(x, y0 + 1, 0, Blocks.RED_STAINED_GLASS.getDefaultState());
            c.set(x, y0 + 2, 0, ModBlocks.MYSTERY_BOX.getDefaultState());
        }
        c.neonWall(0, y0 + 6, -14, Direction.SOUTH, "weapon_cartel", 1f);
        c.neonWall(14, y0 + 3, 6, Direction.WEST, "skull", 1f);
        c.neonWall(-14, y0 + 3, 6, Direction.EAST, "no_cops", 1f);
        for (int x = -10; x <= 10; x += 10) chandelier(c, x, ceil, 6, 1, Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true));
        c.guard(8, y0, 8, 225);
    }

    private static void hackersDen(Canvas c, int y0) {
        int ceil = y0 + FLOOR_H - 1;
        c.fill(-14, y0, -14, 14, y0, 14, Blocks.GREEN_CARPET.getDefaultState());
        c.fill(-14, y0, -14, 14, y0, 14, (x, y, z) -> (x % 4 == 0 && z % 4 == 0) ? Blocks.LIME_CARPET.getDefaultState() : null);
        // the provided hacker terminal setup, in the north-east corner, facing the room
        c.fill(9, y0, -14, 14, y0 + 3, -10, Blocks.AIR.getDefaultState());
        c.prop(12, y0, -12, ModBlocks.HACKER_TERMINAL, Direction.SOUTH);
        // server racks
        for (int x = -10; x <= 4; x += 2) {
            for (int z : new int[]{2, 8}) {
                c.fill(x, y0, z, x, y0 + 4, z + 2, (xx, y, zz) -> (y + zz) % 2 == 0
                        ? Blocks.BLACK_CONCRETE.getDefaultState()
                        : (y == y0 + 2 ? Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true) : Blocks.LIME_STAINED_GLASS.getDefaultState()));
            }
        }
        c.neonWall(0, y0 + 6, -14, Direction.SOUTH, "hackers_den", 1f);
        c.neonWall(-14, y0 + 3, -4, Direction.EAST, "watching", 1f);
        c.neonWall(14, y0 + 3, 4, Direction.WEST, "skull", 1f);
        for (int x = -8; x <= 8; x += 8) chandelier(c, x, ceil, -4, 1, c.neonBlock(DyeColor.LIME));
    }

    private static void penthouse(Canvas c, int y0) {
        int ceil = y0 + FLOOR_H - 1;
        c.fill(-14, y0, -14, 14, y0, 14, Blocks.PURPLE_CARPET.getDefaultState());
        // throne
        c.fill(-1, y0, -13, 1, y0, -13, Blocks.GOLD_BLOCK.getDefaultState());
        c.set(0, y0 + 1, -13, Blocks.GOLD_BLOCK.getDefaultState());
        c.set(0, y0, -12, Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH));
        c.neonWall(0, y0 + 5, -14, Direction.SOUTH, "vip_only", 2f);
        // loot room
        for (int x = -8; x <= 8; x += 4) {
            c.set(x, y0, 8, Blocks.GOLD_BLOCK.getDefaultState());
            c.set(x, y0 + 1, 8, ModBlocks.MYSTERY_BOX.getDefaultState());
        }
        c.neonWall(14, y0 + 3, 0, Direction.WEST, "diamond", 1f);
        c.neonWall(-14, y0 + 3, 0, Direction.EAST, "dollar", 1f);
        for (int[] p : new int[][]{{-7, -5}, {7, -5}, {-7, 5}, {7, 5}}) {
            chandelier(c, p[0], ceil, p[1], 2, c.neonBlock(DyeColor.PURPLE));
        }
        c.guard(-6, y0, -10, 180);
    }

    // ------------------------------------------------------------------------------------------------ shops
    /** design names, one shop each (signs from the translated Japanese set + originals). */
    private static final String[][] SHOPS = {
            {"sushi", "tokyo"}, {"karaoke", "open"}, {"pachinko", "parking"},
            {"top_cash_paid", "cash_only"}, {"drugstore", "no_smoking"}, {"hotel", "vacant"},
            {"beer", "tavern"}, {"mahjong", "dumplings"}, {"bbq", "tea_shop"},
            {"made_in_japan", "open_24_7"}, {"heist", "this_way"}, {"underground", "no_cops"},
    };
    private static final DyeColor[] SHOP_COLORS = {DyeColor.MAGENTA, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.LIME,
            DyeColor.ORANGE, DyeColor.PINK, DyeColor.LIGHT_BLUE, DyeColor.YELLOW, DyeColor.RED, DyeColor.WHITE, DyeColor.MAGENTA, DyeColor.CYAN};

    private static void shops(Canvas c) {
        int d = 38, i = 0;
        for (Direction side : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            for (int off = -22; off <= 22; off += 22) {
                if (side == Direction.SOUTH && off == 0) continue; // keep the path to the shaft clear
                int x = side.getOffsetX() * d + (side.getAxis() == Direction.Axis.Z ? off : 0);
                int z = side.getOffsetZ() * d + (side.getAxis() == Direction.Axis.X ? off : 0);
                shop(c, new Local(c, x, 0, z, side.getOpposite()), SHOPS[i % SHOPS.length], SHOP_COLORS[i % SHOP_COLORS.length]);
                i++;
            }
        }
    }

    /** One upscale pavilion. Local frame: front (open, with counter) at lz=0 facing -Z, back wall at lz=7. */
    private static void shop(Canvas c, Local l, String[] signs, DyeColor color) {
        BlockState wall = Blocks.DEEPSLATE_TILES.getDefaultState();
        BlockState trim = Blocks.BLACK_CONCRETE.getDefaultState();
        l.fill(-5, 0, 0, 5, 6, 7, Blocks.AIR.getDefaultState());
        l.fill(-5, -1, 0, 5, -1, 7, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState());
        l.fill(-5, 0, 7, 5, 6, 7, wall);               // back
        l.fill(-5, 0, 0, -5, 6, 7, wall);              // sides
        l.fill(5, 0, 0, 5, 6, 7, wall);
        l.fill(-5, 7, -1, 5, 7, 7, trim);              // roof with overhang
        l.fill(-5, 6, -1, 5, 6, -1, c.neonBlock(color).getBlock().getDefaultState()); // glowing awning edge
        l.fill(-4, 0, 2, 4, 0, 2, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState()); // counter
        l.fill(-4, 1, 2, 4, 1, 2, Blocks.SMOOTH_QUARTZ_SLAB.getDefaultState());
        l.set(0, 2, 2, ModBlocks.MYSTERY_BOX.getDefaultState());
        l.fill(-4, 5, 6, 4, 5, 6, Blocks.SEA_LANTERN.getDefaultState());
        l.fill(-4, 0, 5, -4, 4, 6, Blocks.BOOKSHELF.getDefaultState()); // merch shelves
        l.fill(4, 0, 5, 4, 4, 6, Blocks.BARREL.getDefaultState());
        l.neonWall(0, 8, -1, Direction.NORTH, signs[0], 1f);           // big sign on the roof edge
        l.neonWall(-5, 3, -1, Direction.NORTH, signs[1], 0.5f);        // small vertical sign on the corner
        l.neonWall(0, 4, 6, Direction.NORTH, signs[0], 0.5f);          // inside
    }

    // ------------------------------------------------------------------------------------------------ ceiling
    private static void ceiling(Canvas c, Random rnd) {
        DyeColor[] cols = {DyeColor.MAGENTA, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.PINK, DyeColor.LIGHT_BLUE};
        for (int i = 0; i < 70; i++) {
            int x = rnd.nextBetween(-RX + 8, RX - 8), z = rnd.nextBetween(-RZ + 8, RZ - 8);
            if (Math.abs(x) <= T_HALF + 3 && Math.abs(z) <= T_HALF + 3) continue;
            double ch = ceilingAt(x, z) * 0.95;
            if (ch < 14) continue;
            int top = (int) ch, len = rnd.nextBetween(2, 9);
            c.fill(x, top - len, z, x, top + 3, z, Blocks.CHAIN.getDefaultState());
            c.set(x, top - len - 1, z, rnd.nextInt(3) == 0 ? Blocks.SHROOMLIGHT.getDefaultState() : c.neonBlock(cols[rnd.nextInt(cols.length)]));
        }
        // giant signs hanging from the ceiling over the street
        String[] big = {"black_market", "limitless", "heist", "open_24_7"};
        int[][] pos = {{0, -30}, {0, 30}, {-30, 0}, {30, 0}};
        Direction[] facing = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        for (int i = 0; i < 4; i++) {
            int top = (int) (ceilingAt(pos[i][0], pos[i][1]) * 0.93);
            c.fill(pos[i][0], top - 3, pos[i][1], pos[i][0], top + 3, pos[i][1], Blocks.CHAIN.getDefaultState());
            c.neon(pos[i][0], top - 4, pos[i][1], facing[i], WallMountLocation.CEILING, big[i], 3f, i == 2);
        }
    }

    // ------------------------------------------------------------------------------------------------ entrance
    private static void entranceShaft(Canvas c, int sx, int sz, int surfaceDy) {
        int bottom = 0;
        int top = Math.max(surfaceDy, 4);
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        c.fill(sx - 2, bottom, sz - 2, sx + 2, top, sz + 2, (x, y, z) ->
                (Math.abs(x - sx) == 2 || Math.abs(z - sz) == 2) ? (y >= (int) ceilingAt(x, z) - 1 || y > H ? wall : null) : Blocks.AIR.getDefaultState());
        c.fill(sx, bottom, sz, sx, top, sz, Blocks.SCAFFOLDING.getDefaultState());
        c.fill(sx - 1, bottom, sz - 1, sx - 1, top, sz - 1, ModBlocks.NEON_STRIPS.get(DyeColor.MAGENTA).getDefaultState());
        c.fill(sx + 1, bottom, sz + 1, sx + 1, top, sz + 1, ModBlocks.NEON_STRIPS.get(DyeColor.CYAN).getDefaultState());
        // welcome arch at the bottom
        c.neonWall(sx, 5, sz - 3, Direction.NORTH, "open_24_7", 1f);
        c.guard(sx - 3, 0, sz - 4, 0);
        c.guard(sx + 3, 0, sz - 4, 0);
    }

    private BlackMarketGenerator() {
    }
}
