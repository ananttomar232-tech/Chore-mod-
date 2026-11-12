package com.limitlessanant.heist.heist;

import com.limitlessanant.heist.entity.VipGuardEntity;
import com.limitlessanant.heist.registry.ModEffects;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

/**
 * The heist alarm. Triggered by laser grids, stealing the Mona Lisa, breaking museum glass, ...
 * The thief gets the WANTED effect and every VIP golem in range immediately attacks.
 */
public final class HeistAlarm {
    public static final int WANTED_TICKS = 20 * 45;
    public static final double GUARD_RANGE = 64;

    public static boolean isWanted(PlayerEntity p) {
        return p.hasStatusEffect(ModEffects.WANTED) && !p.isCreative() && !p.isSpectator();
    }

    public static void trigger(ServerWorld world, PlayerEntity thief, String reason) {
        if (thief.isCreative() || thief.isSpectator()) return;
        boolean fresh = !thief.hasStatusEffect(ModEffects.WANTED);
        thief.addStatusEffect(new StatusEffectInstance(ModEffects.WANTED, WANTED_TICKS, 0, false, true, true));
        if (fresh && thief instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 40, 10));
            sp.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("ALARM!").formatted(Formatting.DARK_RED, Formatting.BOLD)));
            sp.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(reason).formatted(Formatting.RED)));
            world.playSound(null, thief.getBlockPos(), SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 3f, 0.6f);
            world.playSound(null, thief.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 0.6f, 1.6f);
            HeistEvents.startSiren(world, thief.getBlockPos());
        }
        Box area = thief.getBoundingBox().expand(GUARD_RANGE);
        for (VipGuardEntity g : world.getEntitiesByClass(VipGuardEntity.class, area, e -> e.isAlive())) {
            g.alert(thief);
        }
    }

    private HeistAlarm() {
    }
}
