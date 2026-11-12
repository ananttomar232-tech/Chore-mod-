package com.limitlessanant.heist.item;

import com.limitlessanant.heist.entity.GrappleHookEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
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

/**
 * Crossbow turned grappling gun (3D Blockbench model: assets/limitlessheist/models/item/grappling_hook_gun.json).
 * Right-click: fire the hook (48 blocks). Right-click again / sneak: let go.
 * NBT "Fired" switches the item model to the version without the hook on the front (model predicate "limitlessheist:fired").
 */
public class GrapplingGunItem extends Item {
    public GrapplingGunItem(Settings settings) {
        super(settings);
    }

    @Nullable
    public static GrappleHookEntity findHook(ServerWorld world, PlayerEntity p) {
        for (Entity e : world.getEntitiesByClass(GrappleHookEntity.class, p.getBoundingBox().expand(GrappleHookEntity.MAX_RANGE + 8), h -> h.getOwner() == p)) {
            return (GrappleHookEntity) e;
        }
        return null;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world instanceof ServerWorld sw) {
            GrappleHookEntity existing = findHook(sw, user);
            if (existing != null) {
                existing.discard();
                stack.getOrCreateNbt().putBoolean("Fired", false);
                world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_CROSSBOW_LOADING_END, SoundCategory.PLAYERS, 1f, 1.4f);
                return TypedActionResult.success(stack);
            }
            GrappleHookEntity hook = new GrappleHookEntity(world, user);
            hook.setVelocity(user, user.getPitch(), user.getYaw(), 0f, 3.2f, 0f);
            world.spawnEntity(hook);
            stack.getOrCreateNbt().putBoolean("Fired", true);
            stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.7f);
            world.playSound(null, user.getBlockPos(), SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 1.3f);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        // hook vanished (arrived / let go) -> show the hook on the gun again
        if (world instanceof ServerWorld sw && world.getTime() % 10 == 0 && entity instanceof PlayerEntity p
                && stack.hasNbt() && stack.getNbt().getBoolean("Fired") && findHook(sw, p) == null) {
            stack.getNbt().putBoolean("Fired", false);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click: shoot hook, pulls you to the block").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("Right-click again or sneak: release").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Fly over laser grids!").formatted(Formatting.RED));
    }
}
