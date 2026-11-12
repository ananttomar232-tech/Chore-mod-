package com.limitlessanant.heist.entity;

import com.limitlessanant.heist.registry.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Mission 2: the legendary dark unicorn (provided "unicorn_horse_3d" model, restyled + rigged by tools/convert_models.py).
 * <ul>
 *     <li>Only the CRAZY APPLE tames it (anything else and it rears up angrily). Taming wakes up Gotiya, its owner.</li>
 *     <li>Tamed: ride it without a saddle. Look up + forward to take off and FLY; let go of W to glide down.</li>
 *     <li>No fall damage, 80 HP, fast.</li>
 * </ul>
 */
public class UnicornEntity extends AbstractHorseEntity {
    public static final double FLY_SPEED = 1.15;
    private int angryTicks;

    public UnicornEntity(EntityType<? extends AbstractHorseEntity> type, World world) {
        super(type, world);
        setStepHeight(1.5f);
    }

    public static DefaultAttributeContainer.Builder createUnicornAttributes() {
        return AbstractHorseEntity.createBaseHorseAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 80)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.HORSE_JUMP_STRENGTH, 1.1)
                .add(EntityAttributes.GENERIC_ARMOR, 8);
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!isTame()) {
            if (stack.isOf(ModItems.CRAZY_APPLE)) {
                if (!getWorld().isClient) {
                    if (!player.getAbilities().creativeMode) stack.decrement(1);
                    bondWithPlayer(player);
                    saddle(SoundCategory.NEUTRAL);
                    getWorld().sendEntityStatus(this, (byte) 7); // hearts
                    playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1f, 0.6f);
                    playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.6f);
                    if (getWorld() instanceof ServerWorld sw) {
                        sw.spawnParticles(ParticleTypes.WITCH, getX(), getY() + 2.5, getZ(), 80, 0.8, 0.8, 0.8, 0.1);
                        sw.spawnParticles(ParticleTypes.END_ROD, getX(), getY() + 2.5, getZ(), 40, 0.6, 0.8, 0.6, 0.15);
                        for (GotiyaEntity g : sw.getEntitiesByClass(GotiyaEntity.class, getBoundingBox().expand(96), e -> true)) {
                            g.wakeUpFurious(player);
                        }
                    }
                    player.sendMessage(Text.literal("The unicorn trusts you now. Look up and press W to FLY!").formatted(Formatting.LIGHT_PURPLE), true);
                }
                return ActionResult.success(getWorld().isClient);
            }
            if (!getWorld().isClient) {
                angryTicks = 20;
                playAngrySound();
                player.sendMessage(Text.literal("The legendary unicorn only trusts the CRAZY APPLE...").formatted(Formatting.DARK_PURPLE), true);
            }
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public boolean isAngry() {
        return angryTicks > 0 || super.isAngry();
    }

    @Override
    public void tick() {
        super.tick();
        if (angryTicks > 0) angryTicks--;
        if (getWorld().isClient) {
            // dark rainbow sparks from the horn
            if (random.nextInt(3) == 0) {
                float hue = (age % 120) / 120f;
                int rgb = MathHelper.hsvToRgb(hue, 1f, 0.8f);
                Vec3d horn = hornTip();
                getWorld().addParticle(new DustParticleEffect(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), 0.9f),
                        horn.x + (random.nextDouble() - 0.5) * 0.3, horn.y + (random.nextDouble() - 0.5) * 0.3, horn.z + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
            if (!isOnGround() && hasPassengers() && random.nextInt(2) == 0) {
                getWorld().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 2, getY() + 1.5, getZ() + (random.nextDouble() - 0.5) * 2, 0, 0, 0);
            }
        }
    }

    private Vec3d hornTip() {
        Vec3d fwd = Vec3d.fromPolar(0, getBodyYaw());
        boolean moving = hasPassengers() || !isOnGround() || getVelocity().horizontalLengthSquared() > 0.002;
        return moving ? getPos().add(fwd.multiply(1.9)).add(0, 2.5, 0) : getPos().add(fwd.multiply(0.3)).add(0, 3.3, 0);
    }

    // ---------------------------------------------------------------- flight
    @Override
    public void travel(Vec3d input) {
        if (isAlive() && isTame() && getControllingPassenger() instanceof PlayerEntity rider) {
            boolean airborne = !isOnGround() && !isTouchingWater() && !isInLava();
            double forward = input.z;
            float pitch = rider.getPitch();
            if (!airborne && forward > 0 && pitch < -25f) {
                setVelocity(getVelocity().add(0, 0.6, 0));
                airborne = true;
                playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.2f);
            }
            if (airborne) {
                Vec3d v = getVelocity();
                if (forward > 0) {
                    Vec3d look = Vec3d.fromPolar(pitch, rider.getYaw());
                    v = v.lerp(look.multiply(FLY_SPEED), 0.12);
                } else {
                    v = v.multiply(0.92, 1, 0.92).add(0, -0.035, 0);
                    if (v.y < -0.4) v = new Vec3d(v.x, -0.4, v.z);
                }
                setVelocity(v);
                move(MovementType.SELF, v);
                fallDistance = 0;
                updateLimbs(false);
                if (age % 14 == 0) playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.3f);
                return;
            }
        }
        super.travel(input);
    }

    /** Tameable#getWorld (unmapped in yarn 1.20.1+build.10). */
    @Override
    public net.minecraft.world.EntityView method_48926() {
        return getWorld();
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    public double getMountedHeightOffset() {
        return 1.62;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
    }

    @Override
    @Nullable
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_HORSE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_HORSE_DEATH;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.ENTITY_HORSE_ANGRY;
    }

    @Override
    public float getSoundPitch() {
        return 0.65f; // deeper, more dangerous
    }
}
