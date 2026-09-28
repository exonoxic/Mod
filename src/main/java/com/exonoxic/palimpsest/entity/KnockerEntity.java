package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.horror.Spots;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.world.WardHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * It comes to doors at night and knocks. Three times, then a wait, then three again. It will
 * not open a door. It does not need to: sooner or later, somebody inside does.
 *
 * <p>States: KNOCKING at a door; LUNGE after the door opens (or someone steps outside);
 * LEAVING at dawn, when warded off, or when it gives up.</p>
 */
public class KnockerEntity extends Monster implements Apparition {
    public static final int KNOCKING = 1;
    public static final int LUNGE = 2;
    public static final int LEAVING = 3;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(KnockerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KNOCK_ANIM = SynchedEntityData.defineId(KnockerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<UUID>> VIEWER = SynchedEntityData.defineId(KnockerEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    private final ApparitionState apparition = new ApparitionState();
    @Nullable
    private BlockPos door;
    private int visit;
    private int knockCooldown = 20;
    private int knocksLeft;
    private int knockDelay;
    private int lungeTicks;
    private int leaveTicks;
    private boolean opened;

    public KnockerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0D).add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D).add(Attributes.ARMOR, 4.0D).add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D);
    }

    /**
     * Stands it outside the door, on whichever side is open to the sky: right up against the door
     * if there is room (close enough to knock on it), otherwise a step back.
     */
    public static boolean spawnAtDoor(ServerLevel level, BlockPos door, ServerPlayer player) {
        BlockState ds = level.getBlockState(door);
        if (!(ds.getBlock() instanceof DoorBlock)) return false;
        Direction facing = ds.getValue(DoorBlock.FACING);
        BlockPos best = null;
        double bestScore = -1;
        for (Direction d : new Direction[]{facing, facing.getOpposite()}) {
            for (int dist = 1; dist <= 2; dist++) {
                BlockPos p = door.relative(d, dist);
                if (!Spots.standable(level, p, 3)) continue;
                double score = (level.canSeeSky(p.above(2)) ? 1000 : 0) + (dist == 1 ? 500 : 0) + p.distSqr(player.blockPosition());
                if (score > bestScore) {
                    bestScore = score;
                    best = p;
                }
            }
        }
        if (best == null) return false;
        BlockPos standAt = best;
        KnockerEntity k = Apparitions.spawn(ModEntities.KNOCKER.get(), level, standAt, player, e -> {
            e.door = door.immutable();
            e.entityData.set(STATE, KNOCKING);
            e.visit = 2400 + level.random.nextInt(2400);
            e.apparition.begin(player, -1, 0);
            e.entityData.set(VIEWER, Apparitions.viewerFor(player));
            e.setPersistenceRequired();
        });
        if (k == null) return false;
        Vec3 to = Vec3.atCenterOf(door).subtract(k.position());
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        k.setYRot(yaw);
        k.setYHeadRot(yaw);
        k.setYBodyRot(yaw);
        BleedManager.unlock(player, "knocking");
        return true;
    }

    /** Arrives already hunting (a gate read in a storm, a rite gone wrong). */
    public static void comeThrough(ServerLevel level, BlockPos pos, ServerPlayer player) {
        KnockerEntity k = Apparitions.spawn(ModEntities.KNOCKER.get(), level, pos, player, e -> {
            e.apparition.begin(player, -1, 0);
            e.setPersistenceRequired();
        });
        if (k != null) k.lunge(player);
    }

    public int getState() {
        return entityData.get(STATE);
    }

    public int getKnockAnim() {
        return entityData.get(KNOCK_ANIM);
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
        entityData.define(STATE, LUNGE);
        entityData.define(KNOCK_ANIM, 0);
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35D, true) {
            @Override
            public boolean canUse() {
                return getState() == LUNGE && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return getState() == LUNGE && super.canContinueToUse();
            }
        });
    }

    private void lunge(Player target) {
        entityData.set(STATE, LUNGE);
        entityData.set(VIEWER, Optional.empty());
        lungeTicks = 900;
        setTarget(target);
        playSound(ModSounds.KNOCKER_LUNGE.get(), 2.0F, 0.9F + random.nextFloat() * 0.15F);
    }

    private void leave() {
        if (getState() == LEAVING) return;
        entityData.set(STATE, LEAVING);
        setTarget(null);
        leaveTicks = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        int anim = entityData.get(KNOCK_ANIM);
        if (anim > 0) entityData.set(KNOCK_ANIM, anim - 1);
        switch (getState()) {
            case KNOCKING -> tickKnocking();
            case LUNGE -> tickLunge();
            case LEAVING -> tickLeaving();
            default -> leave();
        }
    }

    private void tickKnocking() {
        getNavigation().stop();
        if (door == null || !(level().getBlockState(door).getBlock() instanceof DoorBlock)) {
            leave();
            return;
        }
        getLookControl().setLookAt(door.getX() + 0.5, door.getY() + 1.2, door.getZ() + 0.5);
        BlockState ds = level().getBlockState(door);
        if (ds.getValue(DoorBlock.OPEN)) {
            Player opener = level().getNearestPlayer(door.getX() + 0.5, door.getY(), door.getZ() + 0.5, 8, false);
            if (opener != null && !opener.isCreative() && !opener.isSpectator()) {
                opened = true;
                for (Player p : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(16))) {
                    if (p instanceof ServerPlayer sp) {
                        Advancements.grant(sp, "opened_it");
                        BleedManager.unlock(sp, "knocker");
                    }
                }
                lunge(opener);
                return;
            }
        }
        if (knocksLeft > 0 && --knockDelay <= 0) {
            level().playSound(null, door, ModSounds.EVENT_KNOCK.get(), SoundSource.HOSTILE, 1.0F, 0.95F + random.nextFloat() * 0.1F);
            entityData.set(KNOCK_ANIM, 10);
            knocksLeft--;
            knockDelay = 12;
        } else if (knocksLeft == 0 && --knockCooldown <= 0) {
            knocksLeft = 3;
            knockDelay = 0;
            knockCooldown = 160 + random.nextInt(220);
        }
        Player outside = level().getNearestPlayer(this, 14);
        if (outside != null && !outside.isCreative() && !outside.isSpectator() && hasLineOfSight(outside)
                && level().canSeeSky(outside.blockPosition().above()) && !WardHelper.isPlayerWarded(outside)) {
            lunge(outside);
            return;
        }
        if (--visit <= 0 || WardHelper.isWarded(level(), door, 16) || (level().isDay() && level().canSeeSky(blockPosition().above(2)))) {
            Player haunted = apparition.target(level());
            if (!opened && haunted instanceof ServerPlayer sp && sp.distanceToSqr(this) < 48 * 48) {
                Advancements.grant(sp, "three_knocks");
                BleedManager.unlock(sp, "knocker");
            }
            leave();
        }
    }

    private void tickLunge() {
        LivingEntity target = getTarget();
        if (target == null && lungeTicks == 0) {
            // Spawned without a door (egg, command): pick up whoever is nearest, briefly.
            Player nearest = level().getNearestPlayer(this, 24);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator()) {
                lunge(nearest);
                return;
            }
        }
        boolean lost = target == null || !target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator()));
        if (lost || --lungeTicks <= 0) {
            leave();
            return;
        }
        if (level().getBrightness(LightLayer.BLOCK, blockPosition().above()) >= 12) {
            leave();
            return;
        }
        if (target instanceof Player p && WardHelper.isPlayerWarded(p) && distanceToSqr(p) < 36) leave();
    }

    private void tickLeaving() {
        leaveTicks++;
        if (leaveTicks == 1) {
            Vec3 away = door != null ? position().subtract(Vec3.atCenterOf(door)) : new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
            Vec3 dir = new Vec3(away.x, 0, away.z).lengthSqr() < 1.0E-3 ? new Vec3(1, 0, 0) : new Vec3(away.x, 0, away.z).normalize();
            Vec3 goal = position().add(dir.scale(20));
            getNavigation().moveTo(goal.x, goal.y, goal.z, 0.9D);
        }
        boolean watched = level().getNearestPlayer(this, 24) != null;
        if (leaveTicks > 240 || (!watched && leaveTicks > 40)) ApparitionState.vanish(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && getState() == KNOCKING && source.getEntity() instanceof Player p) lunge(p);
        return hurt;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return distance > 128 * 128;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return getState() == LUNGE ? ModSounds.KNOCKER_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.KNOCKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KNOCKER_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        apparition.save(tag);
        tag.putInt("State", getState());
        tag.putInt("Visit", visit);
        tag.putBoolean("Opened", opened);
        if (door != null) tag.put("Door", NbtUtils.writeBlockPos(door));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
        entityData.set(STATE, tag.contains("State") ? tag.getInt("State") : LEAVING);
        visit = tag.getInt("Visit");
        opened = tag.getBoolean("Opened");
        door = tag.contains("Door") ? NbtUtils.readBlockPos(tag.getCompound("Door")) : null;
    }
}
