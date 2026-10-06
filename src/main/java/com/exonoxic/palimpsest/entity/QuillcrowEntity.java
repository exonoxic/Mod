package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Crows with a steel nib where the beak should be. They perch high up and watch you, and
 * leave the moment you come near. Killing one is noticed.
 */
public class QuillcrowEntity extends PathfinderMob implements Apparition {
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(QuillcrowEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(QuillcrowEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    private int flightTicks;
    @Nullable
    private Vec3 fleeDir;

    public QuillcrowEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    /** Perches on the highest block at the chosen column (usually a treetop) to watch. */
    public static boolean spawnWatcher(ServerLevel level, BlockPos ground, ServerPlayer player) {
        BlockPos perch = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, ground);
        if (perch.getY() - ground.getY() > 24) perch = ground;
        BlockPos finalPerch = perch;
        return Apparitions.spawn(ModEntities.QUILLCROW.get(), level, finalPerch, player, c -> {
            c.apparition.begin(player, 1200, 0);
            c.entityData.set(VIEWER, Apparitions.viewerFor(player));
            c.setPersistenceRequired();
        }) != null;
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
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
        entityData.define(FLYING, false);
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 48.0F, 1.0F));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (isFlying()) {
            flightTicks++;
            if (fleeDir == null) fleeDir = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize();
            setDeltaMovement(fleeDir.x * 0.5, 0.35, fleeDir.z * 0.5);
            setYRot((float) (Math.toDegrees(Math.atan2(fleeDir.z, fleeDir.x)) - 90.0D));
            yBodyRot = getYRot();
            if (flightTicks % 6 == 0) playSound(ModSounds.QUILLCROW_FLAP.get(), 0.6F, 1.0F);
            if (flightTicks > 80) discard();
            return;
        }
        Player near = level().getNearestPlayer(this, 7.0D);
        if (near != null && !near.isSpectator()) takeOff(near.position());
        if (apparition.tick(this)) takeOff(position().add(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5));
    }

    private void takeOff(Vec3 awayFrom) {
        Vec3 d = position().subtract(awayFrom);
        fleeDir = new Vec3(d.x, 0, d.z).lengthSqr() < 1.0E-3 ? null : new Vec3(d.x, 0, d.z).normalize();
        entityData.set(FLYING, true);
        setNoGravity(true);
        playSound(ModSounds.QUILLCROW_AMBIENT.get(), 1.0F, 0.9F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && !level().isClientSide && !isFlying() && source.getEntity() != null) takeOff(source.getEntity().position());
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? ModSounds.QUILLCROW_AMBIENT.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.QUILLCROW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.QUILLCROW_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
    }
}
