package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.heist.LaserRegistry;
import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

public class HackerTerminalBlockEntity extends SyncedBlockEntity {
    public static final int HACK_TIME = 100;      // 5 s
    public static final int LASERS_OFF = 20 * 60;  // 60 s
    public static final double RADIUS = 48;
    private int hackTicks = -1;
    private UUID hacker;

    public HackerTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HACKER_TERMINAL, pos, state);
    }

    /** -1 when idle, else 0..HACK_TIME. Synced: the screens glitch while hacking. */
    public int getHackTicks() {
        return hackTicks;
    }

    public void startHack(PlayerEntity p) {
        if (hackTicks >= 0) return;
        hackTicks = 0;
        hacker = p.getUuid();
        sync();
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, HackerTerminalBlockEntity be) {
        if (be.hackTicks < 0) return;
        be.hackTicks++;
        PlayerEntity p = be.hacker == null ? null : world.getPlayerByUuid(be.hacker);
        if (be.hackTicks % 3 == 0) {
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.BLOCKS, 0.5f, 1.5f + world.random.nextFloat() * 0.5f);
        }
        if (p != null && be.hackTicks % 5 == 0) {
            int pct = be.hackTicks * 100 / HACK_TIME;
            int bars = pct / 5;
            p.sendMessage(Text.literal("HACKING IN PROGRESS [").formatted(Formatting.GREEN)
                    .append(Text.literal("|".repeat(bars)).formatted(Formatting.GREEN))
                    .append(Text.literal("|".repeat(20 - bars)).formatted(Formatting.DARK_GRAY))
                    .append(Text.literal("] " + pct + "%").formatted(Formatting.GREEN)), true);
        }
        if (be.hackTicks >= HACK_TIME) {
            int n = LaserRegistry.disableAround((ServerWorld) world, pos, RADIUS, LASERS_OFF);
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.5f, 1.2f);
            if (p instanceof ServerPlayerEntity sp) {
                sp.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("ACCESS GRANTED").formatted(Formatting.GREEN, Formatting.BOLD)));
                sp.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(n + " laser grids offline for 60s").formatted(Formatting.GRAY)));
            }
            be.hackTicks = -1;
            be.hacker = null;
            be.sync();
        } else if (be.hackTicks % 20 == 0) {
            be.sync();
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Hack", hackTicks);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hackTicks = nbt.contains("Hack") ? nbt.getInt("Hack") : -1;
    }
}
