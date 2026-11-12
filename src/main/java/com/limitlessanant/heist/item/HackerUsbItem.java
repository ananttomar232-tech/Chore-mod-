package com.limitlessanant.heist.item;

import com.limitlessanant.heist.heist.LaserRegistry;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

/** Mystery box gadget: single-use EMP that shuts down lasers within 16 blocks for 20 s. */
public class HackerUsbItem extends Item {
    public HackerUsbItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld sw) {
            int n = LaserRegistry.disableAround(sw, user.getBlockPos(), 16, 400);
            user.sendMessage(Text.literal("EMP: " + n + " lasers offline for 20s").formatted(Formatting.GREEN), true);
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1f, 1.8f);
            if (!user.getAbilities().creativeMode) stack.decrement(1);
        }
        user.getItemCooldownManager().set(this, 40);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click: lasers within 16 blocks OFF for 20s").formatted(Formatting.GREEN));
    }
}
