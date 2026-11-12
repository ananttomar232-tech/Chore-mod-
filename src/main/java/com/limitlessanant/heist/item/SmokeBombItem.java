package com.limitlessanant.heist.item;

import com.limitlessanant.heist.entity.VipGuardEntity;
import com.limitlessanant.heist.registry.ModEffects;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Mystery box gadget: escape tool. Big smoke cloud, 10 s invisibility + speed, guards lose you, WANTED is cleared. */
public class SmokeBombItem extends Item {
    public SmokeBombItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, user.getX(), user.getY() + 1, user.getZ(), 120, 1.5, 1, 1.5, 0.02);
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, user.getX(), user.getY() + 1, user.getZ(), 200, 2, 1.2, 2, 0.05);
            sw.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.6f, 1.8f);
            sw.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 1.2f, 0.6f);
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 200, 0, false, false, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 2, false, false, true));
            user.removeStatusEffect(ModEffects.WANTED);
            for (VipGuardEntity g : sw.getEntitiesByClass(VipGuardEntity.class, user.getBoundingBox().expand(48), g -> g.getTarget() == user)) {
                g.setTarget(null);
            }
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click: vanish (clears WANTED)").formatted(Formatting.GRAY));
    }
}
