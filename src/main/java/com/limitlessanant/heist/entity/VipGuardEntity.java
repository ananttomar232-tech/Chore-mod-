package com.limitlessanant.heist.entity;

import com.limitlessanant.heist.heist.HeistAlarm;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.goal.WanderNearTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * VIP Iron Golem Bodyguard. A vanilla iron golem (same model + animations) in a black suit, tie, earpiece and 3D black
 * sunglasses (see client VipGuardRenderer / VipAccessoriesFeatureRenderer).
 * <p>
 * Stands guard at the spot it spawned ("post", 12 block leash), ignores players unless they are WANTED
 * (tripped a laser, stole the Mona Lisa / Dragon Core, or hit a guard) - then it hunts them.
 */
public class VipGuardEntity extends IronGolemEntity {
    private BlockPos post;

    public VipGuardEntity(EntityType<? extends IronGolemEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createVipAttributes() {
        return IronGolemEntity.createIronGolemAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 150)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 18)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.add(2, new WanderNearTargetGoal(this, 0.9, 32f));
        goalSelector.add(3, new GoToWalkTargetGoal(this, 0.8));
        goalSelector.add(6, new WanderAroundGoal(this, 0.45, 240));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 10f));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 5, true, false,
                e -> e instanceof PlayerEntity p && HeistAlarm.isWanted(p)));
        targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, false, false,
                e -> e instanceof Monster && !(e instanceof CreeperEntity)));
    }

    /** Called by the alarm. */
    public void alert(PlayerEntity thief) {
        setTarget(thief);
        playSound(SoundEvents.ENTITY_IRON_GOLEM_HURT, 1.5f, 0.6f);
    }

    public void setPost(BlockPos p) {
        post = p.toImmutable();
        setPositionTarget(post, 12);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
                                 @Nullable EntityData data, @Nullable NbtCompound nbt) {
        setPost(getBlockPos());
        setCustomName(Text.literal("VIP Security").formatted(Formatting.GOLD));
        setCustomNameVisible(false);
        return super.initialize(world, difficulty, reason, data, nbt);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.getAttacker() instanceof PlayerEntity p && getWorld() instanceof ServerWorld sw) {
            HeistAlarm.trigger(sw, p, "You attacked VIP security!");
        }
        return super.damage(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && getTarget() instanceof PlayerEntity p && !HeistAlarm.isWanted(p) && getAttacker() != p) {
            setTarget(null); // wanted level expired -> back to post
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (post != null) nbt.put("Post", NbtHelper.fromBlockPos(post));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Post")) setPost(NbtHelper.toBlockPos(nbt.getCompound("Post")));
    }
}
