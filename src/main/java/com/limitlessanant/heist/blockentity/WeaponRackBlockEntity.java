package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** Three display slots, rendered across the rack (left, middle, right). */
public class WeaponRackBlockEntity extends SyncedBlockEntity {
    public static final int SLOTS = 3;
    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(SLOTS, ItemStack.EMPTY);

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPON_RACK, pos, state);
    }

    public DefaultedList<ItemStack> getItems() {
        return items;
    }

    public boolean insert(ItemStack stack) {
        for (int i = 0; i < SLOTS; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack);
                sync();
                return true;
            }
        }
        return false;
    }

    public ItemStack removeLast() {
        for (int i = SLOTS - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack s = items.get(i);
                items.set(i, ItemStack.EMPTY);
                sync();
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, items, true);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        for (int i = 0; i < SLOTS; i++) items.set(i, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items);
    }
}
