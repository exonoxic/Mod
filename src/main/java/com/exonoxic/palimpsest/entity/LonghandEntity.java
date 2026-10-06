package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.entity.ai.Pursuit;
import com.exonoxic.palimpsest.entity.ai.Squeeze;
import com.exonoxic.palimpsest.entity.ai.SqueezeNavigation;
import com.exonoxic.palimpsest.entity.ai.Squeezer;
import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.entity.projectile.InkBombEntity;
import com.exonoxic.palimpsest.horror.Spots;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.util.Sounds;
import com.exonoxic.palimpsest.world.WardHelper;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * A figure like one long pen-stroke, taller than a door. It only moves when nobody is
 * looking at it — and in the dark, you are not really looking at anything. Ordinary weapons
 * pass through it; only inkbane cuts. Light, wards and chalk keep it off.
 *
 * <p>Watchers stand far away and are gone when approached (or stared at for too long).
 * Hunters come for one player and are the reason to carry a lantern.</p>
 */
public class LonghandEntity extends Monster implements Apparition, Squeezer {
    public static final int WATCHER = 0;
    public static final int HUNTER = 1;
    public static final int POSES = 5;

    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(LonghandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(LonghandEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> POSE_INDEX = SynchedEntityData.defineId(LonghandEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(LonghandEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    private int stared;
    private int heartbeatIn;

    /** Folds in half under anything two blocks high, and lies flat to get through a one-block hole. */
    public final Squeeze squeeze = Squeeze.of(this, 0.8F, 3.6F);

    public LonghandEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 20;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new SqueezeNavigation(this, level);
    }

    @Override
    public Squeeze squeeze() {
        return squeeze;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return squeeze == null ? super.getDimensions(pose) : squeeze.dimensions(pose);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return squeeze == null ? super.getBoundingBoxForCulling() : squeeze.cullingBox(super.getBoundingBoxForCulling());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0D).add(Attributes.MOVEMENT_SPEED, 0.42D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D).add(Attributes.FOLLOW_RANGE, 64.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static boolean spawnWatcher(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return Apparitions.spawn(ModEntities.LONGHAND.get(), level, pos, player, l -> {
            l.entityData.set(MODE, WATCHER);
            l.apparition.begin(player, 700, 24);
            l.entityData.set(VIEWER, Apparitions.viewerFor(player));
            l.setPersistenceRequired();
        }) != null;
    }

    public static boolean spawnHunter(ServerLevel level, BlockPos pos, ServerPlayer player) {
        LonghandEntity l = Apparitions.spawn(ModEntities.LONGHAND.get(), level, pos, player, e -> {
            e.entityData.set(MODE, HUNTER);
            e.apparition.begin(player, 3600, 0);
            e.setPersistenceRequired();
        });
        if (l == null) return false;
        l.setTarget(player);
        return true;
    }

    public boolean isFrozen() {
        return entityData.get(FROZEN);
    }

    public int getPoseIndex() {
        return entityData.get(POSE_INDEX);
    }

    public int getMode() {
        return entityData.get(MODE);
    }

    @Override
    public ApparitionState apparition() {
        return apparition;
    }

    @Override
    public Optional<UUID> exclusiveViewer() {
        return entityData.get(VIEWER);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(MODE, HUNTER);
        entityData.define(FROZEN, false);
        entityData.define(POSE_INDEX, 0);
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new HoldStillGoal());
        Pursuit.install(this, goalSelector, 1, () -> getMode() == HUNTER && !isFrozen(), 40, 100);
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return getMode() == HUNTER && super.canUse();
            }
        });
    }

    /**
     * Is anyone actually seeing it? They must be facing it, have a clear line of sight, and
     * there must be enough light to see by (or they must be very close, or have night vision).
     */
    private boolean isObserved() {
        Vec3 center = position().add(0, getBbHeight() * 0.6, 0);
        boolean lit = level().getMaxLocalRawBrightness(blockPosition().above()) >= 3;
        for (Player p : level().players()) {
            if (p.isSpectator() || p.distanceToSqr(this) > 64 * 64) continue;
            Vec3 to = center.subtract(p.getEyePosition());
            double dist = to.length();
            if (dist < 1.0E-3 || to.scale(1.0 / dist).dot(p.getLookAngle()) < 0.6) continue;
            if (!p.hasLineOfSight(this)) continue;
            if (lit || dist < 4.0 || p.hasEffect(MobEffects.NIGHT_VISION)) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        // Watched, it holds whatever shape it was caught in.
        squeeze.tick(!isFrozen());
        super.tick();
        if (level().isClientSide) {
            // Unwatched, it leaves ink behind it: drips from the fingertips and the nib.
            if (!isFrozen() && random.nextInt(4) == 0) {
                double yawRad = Math.toRadians(yBodyRot);
                double side = (random.nextBoolean() ? 1 : -1) * 0.2D;
                level().addParticle(ModParticles.INK_DRIP.get(),
                        getX() + Math.cos(yawRad) * side + (random.nextDouble() - 0.5D) * 0.1D,
                        getY() + 0.35D + random.nextDouble() * 0.3D,
                        getZ() + Math.sin(yawRad) * side + (random.nextDouble() - 0.5D) * 0.1D, 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        boolean observed = isObserved();
        if (observed != isFrozen()) {
            entityData.set(FROZEN, observed);
            if (observed) entityData.set(POSE_INDEX, random.nextInt(POSES));
        }
        if (observed) setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);

        if (getMode() == WATCHER) {
            Player p = apparition.target(level());
            if (p != null && !observed) getLookControl().setLookAt(p, 10F, 10F);
            stared = observed ? stared + 1 : Math.max(0, stared - 1);
            if (stared > 100 || apparition.tick(this)) {
                if (p instanceof ServerPlayer sp) {
                    BleedManager.unlock(sp, "longhand");
                    Advancements.grant(sp, "dont_blink");
                }
                ApparitionState.vanish(this);
            }
            return;
        }

        LivingEntity target = getTarget();
        boolean overworldDay = level().dimension() != ModDimensions.UNDERTEXT && level().isDay();
        if (overworldDay || WardHelper.isWarded(level(), blockPosition(), 16) || apparition.tick(this)) {
            if (target instanceof ServerPlayer sp && sp.isAlive()) {
                Advancements.grant(sp, "dont_blink");
                BleedManager.unlock(sp, "longhand");
            }
            ApparitionState.vanish(this);
            return;
        }
        if (target instanceof Player p && WardHelper.isPlayerWarded(p) && distanceToSqr(p) < 25) {
            getNavigation().stop();
            return;
        }
        if (target == null) {
            Player p = apparition.target(level());
            if (p == null) p = level().getNearestPlayer(this, 32);
            if (p != null && !p.isCreative() && !p.isSpectator()) setTarget(p);
        }
        // Close and unwatched, the one it hunts hears their own heart, faster the nearer it is: turn round.
        if (!observed && target instanceof ServerPlayer sp && distanceToSqr(sp) < 16 * 16 && --heartbeatIn <= 0) {
            float near = (float) Mth.clamp(1.0D - Math.sqrt(distanceToSqr(sp)) / 16.0D, 0.0D, 1.0D);
            float pitch = 0.9F + 0.45F * near;
            Sounds.playTo(sp, ModSounds.EVENT_HEARTBEAT.get(), SoundSource.AMBIENT, sp.getEyePosition(), 0.3F + 0.5F * near, pitch);
            heartbeatIn = (int) (76 / pitch);
        }
        // While nobody watches, it sometimes simply isn't where it was.
        if (!observed && target != null && distanceToSqr(target) < 24 * 24 && distanceToSqr(target) > 16 && random.nextInt(30) == 0) {
            blinkToward(target);
        }
    }

    private void blinkToward(LivingEntity target) {
        Vec3 dir = target.position().subtract(position());
        Vec3 flat = new Vec3(dir.x, 0, dir.z).normalize().scale(2.5D);
        BlockPos dest = BlockPos.containing(position().add(flat));
        BlockPos floor = Spots.localFloor((ServerLevel) level(), dest.getX(), dest.getY(), dest.getZ(), 3);
        if (floor == null || !Spots.standable((ServerLevel) level(), floor, 4) || WardHelper.isWarded(level(), floor, 16)) return;
        teleportTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5);
        level().playSound(null, floor, ModSounds.LONGHAND_MOVE.get(), SoundSource.HOSTILE, 0.6F, 0.8F + random.nextFloat() * 0.3F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
        return hit;
    }

    /** Only inkbane, illumine and ink bombs touch it. Everything else passes through. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        boolean inkbane = attacker instanceof LivingEntity le && direct == attacker && CombatEvents.isInkbane(le);
        boolean bomb = direct instanceof InkBombEntity;
        if (inkbane || bomb) return super.hurt(source, amount);
        if (!level().isClientSide && attacker instanceof Player) {
            level().playSound(null, blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.HOSTILE, 0.8F, 1.6F);
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 96 * 96;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isFrozen() ? null : ModSounds.LONGHAND_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LONGHAND_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LONGHAND_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
        tag.putInt("Mode", getMode());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
        entityData.set(MODE, tag.getInt("Mode"));
    }

    /** While observed, nothing else runs: no steps, no turning, no reaching. */
    private class HoldStillGoal extends Goal {
        HoldStillGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isFrozen();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getNavigation().stop();
            xxa = 0;
            zza = 0;
        }
    }
}
