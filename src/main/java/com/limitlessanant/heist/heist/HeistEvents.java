package com.limitlessanant.heist.heist;

import com.limitlessanant.heist.item.LavaSuitArmorItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Server tick logic that is not tied to a block / entity: alarm sirens and the Lava Suit's fire immunity. */
public final class HeistEvents {
    private static final List<Siren> SIRENS = new ArrayList<>();

    private record Siren(ServerWorld world, BlockPos pos, int[] ticks) {
    }

    public static void startSiren(ServerWorld world, BlockPos pos) {
        SIRENS.add(new Siren(world, pos.toImmutable(), new int[]{20 * 12}));
    }

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            // ---- wee-woo alarm
            for (Iterator<Siren> it = SIRENS.iterator(); it.hasNext(); ) {
                Siren s = it.next();
                if (s.world != world) continue;
                int t = --s.ticks[0];
                if (t % 10 == 0) {
                    float pitch = (t / 10) % 2 == 0 ? 1.4f : 0.9f;
                    world.playSound(null, s.pos, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.BLOCKS, 4f, pitch);
                }
                if (t <= 0) it.remove();
            }
            // ---- Lava Proof Suit: permanent fire resistance + lava bubbles
            if (world.getTime() % 10 == 0) {
                for (ServerPlayerEntity p : world.getPlayers()) {
                    if (LavaSuitArmorItem.hasFullSuit(p)) {
                        StatusEffectInstance cur = p.getStatusEffect(StatusEffects.FIRE_RESISTANCE);
                        if (cur == null || cur.getDuration() < 100) {
                            p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 260, 0, true, false, true));
                        }
                        p.extinguish();
                        if (p.isInLava()) {
                            world.spawnParticles(ParticleTypes.LAVA, p.getX(), p.getY() + 1, p.getZ(), 2, 0.3, 0.4, 0.3, 0);
                        }
                    }
                }
            }
        });
    }

    private HeistEvents() {
    }
}
