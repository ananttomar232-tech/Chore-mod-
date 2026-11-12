package com.limitlessanant.heist.block;

import com.limitlessanant.heist.blockentity.MysteryBoxBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Thief Mystery Box: heavy black loot crate with glowing neon "?" panels and a spinning hologram "?" on top.
 * Break it: a random heist gadget drops (data/limitlessheist/loot_tables/blocks/mystery_box.json - edit the weights there)
 * and an epic burst of particles + firework sounds plays.
 */
public class MysteryBoxBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.createCuboidShape(1, 0, 1, 15, 14, 15);

    public MysteryBoxBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL; // crate is a normal (Blockbench) JSON model, the hologram is a BER
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
        if (world instanceof ServerWorld sw) burst(sw, Vec3d.ofCenter(pos));
    }

    /** The "epic particles". */
    public static void burst(ServerWorld w, Vec3d c) {
        w.spawnParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + 0.3, c.z, 140, 0.25, 0.4, 0.25, 0.75);
        w.spawnParticles(ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, 60, 0.1, 0.1, 0.1, 0.25);
        w.spawnParticles(ParticleTypes.FIREWORK, c.x, c.y + 0.5, c.z, 80, 0.2, 0.2, 0.2, 0.35);
        DustParticleEffect pink = new DustParticleEffect(new Vector3f(1f, 0.17f, 0.84f), 2.0f);
        DustParticleEffect cyan = new DustParticleEffect(new Vector3f(0.22f, 0.91f, 1f), 2.0f);
        for (int i = 0; i < 48; i++) { // double helix shooting up
            double a = i * 0.45, y = i * 0.08;
            w.spawnParticles(pink, c.x + Math.cos(a) * 0.8, c.y + y, c.z + Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
            w.spawnParticles(cyan, c.x - Math.cos(a) * 0.8, c.y + y, c.z - Math.sin(a) * 0.8, 1, 0, 0, 0, 0);
        }
        BlockPos p = BlockPos.ofFloored(c);
        w.playSound(null, p, SoundEvents.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.BLOCKS, 2f, 1f);
        w.playSound(null, p, SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.BLOCKS, 2f, 1f);
        w.playSound(null, p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.BLOCKS, 0.7f, 1.2f);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MysteryBoxBlockEntity(pos, state);
    }
}
