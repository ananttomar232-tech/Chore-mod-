package com.limitlessanant.heist.heist;

import com.limitlessanant.heist.blockentity.LaserEmitterBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

/** Knows where every loaded laser emitter is, so hacking can shut a whole grid down without scanning blocks. */
public final class LaserRegistry {
    private static final WeakHashMap<World, Set<BlockPos>> LASERS = new WeakHashMap<>();

    public static void add(World w, BlockPos p) {
        LASERS.computeIfAbsent(w, k -> new HashSet<>()).add(p.toImmutable());
    }

    public static void remove(World w, BlockPos p) {
        Set<BlockPos> s = LASERS.get(w);
        if (s != null) s.remove(p);
    }

    /** @return number of lasers disabled */
    public static int disableAround(ServerWorld w, BlockPos center, double radius, int ticks) {
        Set<BlockPos> s = LASERS.get(w);
        if (s == null) return 0;
        int n = 0;
        double r2 = radius * radius;
        for (BlockPos p : s.toArray(new BlockPos[0])) {
            if (p.getSquaredDistance(center) <= r2 && w.getBlockEntity(p) instanceof LaserEmitterBlockEntity le) {
                le.disableFor(ticks);
                n++;
            }
        }
        return n;
    }

    private LaserRegistry() {
    }
}
