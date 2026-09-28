package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModItems;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
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
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

/**
 * What is left of a person from the First Draft: an outline with the face rubbed out. It walks
 * the same few steps over and over, like a sentence being re-read. Stare at it long enough and
 * it notices you, and follows at a polite distance. It never hurts anyone. Striking it makes it
 * come apart.
 */
public class SmudgeEntity extends PathfinderMob implements Apparition {
    private static final EntityDataAccessor<Integer> FIXATED = SynchedEntityData.defineId(SmudgeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(SmudgeEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    @Nullable
    private BlockPos anchor;
    private int stareTicks;
    @Nullable
    private UUID fixatedOn;

    public SmudgeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.MOVEMENT_SPEED, 0.2D).add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    public static boolean spawnApparition(ServerLevel level, BlockPos pos, ServerPlayer player) {
        return Apparitions.spawn(ModEntities.SMUDGE.get(), level, pos, player, s -> {
            s.apparition.begin(player, 400, 14);
            s.entityData.set(VIEWER, Apparitions.viewerFor(player));
            s.setPersistenceRequired();
        }) != null;
    }

    /** Every Smudge nearby stops what it is doing and turns to look. */
    public static void fixateAll(ServerLevel level, Player player, double radius) {
        for (SmudgeEntity s : level.getEntitiesOfClass(SmudgeEntity.class, player.getBoundingBox().inflate(radius))) {
            s.fixate(player, 200);
        }
    }

    public void fixate(Player player, int ticks) {
        fixatedOn = player.getUUID();
        entityData.set(FIXATED, ticks);
    }

    public boolean isFixated() {
        return entityData.get(FIXATED) > 0;
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
        entityData.define(FIXATED, 0);
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        goalSelector.addGoal(2, new FollowFixationGoal());
        goalSelector.addGoal(3, new RoutineGoal());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (apparition.active()) {
            Player p = apparition.target(level());
            if (p != null) getLookControl().setLookAt(p, 30F, 30F);
            if (apparition.tick(this)) ApparitionState.vanish(this);
            return;
        }
        int f = entityData.get(FIXATED);
        if (f > 0) entityData.set(FIXATED, f - 1);
        if (tickCount % 10 == 0) checkStare();
    }

    /** Being looked at for three seconds is enough to be noticed back. */
    private void checkStare() {
        Player p = level().getNearestPlayer(this, 16);
        if (p == null || isFixated()) {
            stareTicks = 0;
            return;
        }
        Vec3 to = position().add(0, getBbHeight() * 0.7, 0).subtract(p.getEyePosition()).normalize();
        boolean looking = to.dot(p.getLookAngle()) > 0.93 && p.hasLineOfSight(this);
        stareTicks = looking ? stareTicks + 10 : Math.max(0, stareTicks - 5);
        if (stareTicks >= 60) {
            fixate(p, 400);
            playSound(ModSounds.SMUDGE_AMBIENT.get(), 0.8F, 0.8F);
            stareTicks = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!level().isClientSide && source.getEntity() instanceof Player && random.nextBoolean()) {
            // It does not fight. It comes apart.
            playSound(ModSounds.SMUDGE_DEATH.get(), 1.0F, 1.0F);
            if (random.nextFloat() < 0.6F) spawnAtLocation(new ItemStack(ModItems.FADED_PAGE.get()));
            ApparitionState.vanish(this);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return apparition.active() || distance > 16384.0D;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return isFixated() || random.nextInt(4) == 0 ? ModSounds.SMUDGE_AMBIENT.get() : null;
    }

    @Override
    public int getAmbientSoundInterval() {
        // Once it is following you, the mouth that was left behind hardly stops.
        return isFixated() ? 70 : 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SMUDGE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SMUDGE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
        if (anchor != null) tag.put("Anchor", NbtUtils.writeBlockPos(anchor));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
        if (tag.contains("Anchor")) anchor = NbtUtils.readBlockPos(tag.getCompound("Anchor"));
    }

    /** Follows the one who noticed it, keeping three blocks away, murmuring. */
    private class FollowFixationGoal extends Goal {
        FollowFixationGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !apparition.active() && isFixated() && fixatedOn != null && level().getPlayerByUUID(fixatedOn) != null;
        }

        @Override
        public void tick() {
            Player p = fixatedOn == null ? null : level().getPlayerByUUID(fixatedOn);
            if (p == null) return;
            getLookControl().setLookAt(p, 30F, 30F);
            if (distanceToSqr(p) > 9.0D) getNavigation().moveTo(p, 0.9D);
            else getNavigation().stop();
        }
    }

    /**
     * Walks a small, fixed loop of three points around where it first stood, pausing at each
     * as if to knock on a door that is no longer there.
     */
    private class RoutineGoal extends Goal {
        private int step;
        private int pause;

        RoutineGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !apparition.active() && !isFixated();
        }

        @Override
        public void tick() {
            if (anchor == null) anchor = blockPosition();
            if (pause > 0) {
                pause--;
                return;
            }
            BlockPos target = waypoint(step);
            if (getNavigation().isDone()) {
                if (target.closerToCenterThan(position(), 1.6D)) {
                    step = (step + 1) % 3;
                    pause = 60 + (int) ((anchor.asLong() >>> 3) % 60);
                } else {
                    getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.7D);
                }
            }
        }

        private BlockPos waypoint(int i) {
            long seed = anchor.asLong() * 31L + i;
            int dx = (int) Math.floorMod(seed, 9L) - 4;
            int dz = (int) Math.floorMod(seed >>> 8, 9L) - 4;
            BlockPos p = anchor.offset(dx, 0, dz);
            return level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p).getY() - p.getY() < 3
                    ? level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p) : anchor;
        }
    }
}
