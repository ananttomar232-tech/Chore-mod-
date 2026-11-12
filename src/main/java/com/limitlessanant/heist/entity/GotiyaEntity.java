package com.limitlessanant.heist.entity;

import net.minecraft.block.BedBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

/**
 * Gotiya - the funny villager who owns the legendary unicorn (Mission 2). He is ALWAYS asleep in his bed
 * (snoring, sleep-talking when you poke him). When somebody tames his unicorn he jumps out of bed screaming,
 * then... goes back to sleep.
 */
public class GotiyaEntity extends VillagerEntity {
    private static final String[] SLEEP_TALK = {
            "zzz... five more minutes... zzz",
            "zzz... nobody touch my unicorn... zzz",
            "zzz... the crazy apple is under my pillow... wait no... zzz",
            "zzz... mmm... diamonds... zzz",
            "*snores like a dragon*",
            "zzz... LimitlessAnant... I see you... zzz",
    };
    private int furiousTicks;

    public GotiyaEntity(EntityType<? extends VillagerEntity> type, World world) {
        super(type, world);
        setVillagerData(getVillagerData().withProfession(VillagerProfession.NITWIT));
        setCustomName(Text.literal("Gotiya").formatted(Formatting.GOLD));
        setCustomNameVisible(true);
        setAiDisabled(true);
        setPersistent();
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;
        if (furiousTicks > 0) {
            if (--furiousTicks == 0) say("...whatever. Good night. zzz", Formatting.GRAY);
            return;
        }
        if (!isSleeping()) {
            BlockPos bed = findBed();
            if (bed != null) sleep(bed);
        }
        if (isSleeping() && age % 70 == 0) playSound(SoundEvents.ENTITY_FOX_SLEEP, 1.2f, 0.55f);
    }

    private BlockPos findBed() {
        for (BlockPos p : BlockPos.iterate(getBlockPos().add(-2, -1, -2), getBlockPos().add(2, 1, 2))) {
            if (getWorld().getBlockState(p).getBlock() instanceof BedBlock) return p.toImmutable();
        }
        return null;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            if (furiousTicks > 0) player.sendMessage(Text.literal("Gotiya: GIVE ME BACK MY UNICORN!!!").formatted(Formatting.RED), false);
            else player.sendMessage(Text.literal("Gotiya: " + SLEEP_TALK[random.nextInt(SLEEP_TALK.length)]).formatted(Formatting.YELLOW), false);
        }
        return ActionResult.success(getWorld().isClient);
    }

    /** The unicorn was stolen! */
    public void wakeUpFurious(PlayerEntity thief) {
        if (isSleeping()) wakeUp();
        furiousTicks = 20 * 10;
        say("WHAT?! MY UNICORN!!! " + thief.getName().getString().toUpperCase() + " YOU THIEF!!!", Formatting.RED);
        playSound(SoundEvents.ENTITY_VILLAGER_NO, 2f, 0.8f);
        playSound(SoundEvents.ENTITY_VILLAGER_HURT, 2f, 0.6f);
    }

    private void say(String msg, Formatting f) {
        if (!(getWorld() instanceof ServerWorld sw)) return;
        for (ServerPlayerEntity p : sw.getPlayers(p -> p.squaredDistanceTo(this) < 96 * 96)) {
            p.sendMessage(Text.literal("Gotiya: ").formatted(Formatting.GOLD).append(Text.literal(msg).formatted(f)), false);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity p && !getWorld().isClient) {
            p.sendMessage(Text.literal("Gotiya: zzz... stop tickling me... zzz").formatted(Formatting.YELLOW), true);
            return false; // he sleeps through everything
        }
        return super.damage(source, amount);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Furious", furiousTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        furiousTicks = nbt.getInt("Furious");
    }
}
