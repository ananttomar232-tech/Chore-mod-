package com.limitlessanant.heist.blockentity;

import com.limitlessanant.heist.block.NeonDesigns;
import com.limitlessanant.heist.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;

public class NeonSignBlockEntity extends SyncedBlockEntity {
    public static final float[] SCALES = {1f, 2f, 3f, 4f, 0.5f};
    private int design = NeonDesigns.indexOf("black_market");
    private int scaleIndex = 0;
    private boolean flicker = false;

    public NeonSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NEON_SIGN, pos, state);
    }

    public int getDesign() {
        return design;
    }

    public void setDesign(int d) {
        design = Math.floorMod(d, NeonDesigns.count());
        sync();
    }

    public void cycleDesign(int dir) {
        setDesign(design + dir);
    }

    public float getScale() {
        return SCALES[scaleIndex % SCALES.length];
    }

    public void setScale(float s) {
        for (int i = 0; i < SCALES.length; i++) if (SCALES[i] == s) scaleIndex = i;
        sync();
    }

    public void cycleScale() {
        scaleIndex = (scaleIndex + 1) % SCALES.length;
        sync();
    }

    public boolean isFlicker() {
        return flicker;
    }

    public void setFlicker(boolean f) {
        flicker = f;
        sync();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Design", NeonDesigns.get(design).id());
        nbt.putInt("Scale", scaleIndex);
        nbt.putBoolean("Flicker", flicker);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("Design")) design = NeonDesigns.indexOf(nbt.getString("Design"));
        scaleIndex = nbt.getInt("Scale");
        flicker = nbt.getBoolean("Flicker");
    }
}
