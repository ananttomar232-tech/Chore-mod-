package com.limitlessanant.heist.item;

import com.limitlessanant.heist.entity.FreezeBoltEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Mission 3 weapon (provided "Freeze ray gun" model). Shoots an ice bolt: Slowness X + frozen into an ice statue. */
public class FreezeRayItem extends Item {
    public FreezeRayItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.7f, 2f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 0.6f, 1.6f);
        if (!world.isClient) {
            FreezeBoltEntity bolt = new FreezeBoltEntity(world, user);
            bolt.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 3.0f, 0.1f);
            world.spawnEntity(bolt);
            stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
        }
        user.getItemCooldownManager().set(this, 12);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click: fire an ice bolt").formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Slowness X + frozen into an ice statue (10s)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Freezes water and lava it hits").formatted(Formatting.GRAY));
    }
}
