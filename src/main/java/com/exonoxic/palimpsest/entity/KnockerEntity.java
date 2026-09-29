package com.exonoxic.palimpsest.entity;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.entity.ai.Pursuit;
import com.exonoxic.palimpsest.entity.ai.Squeeze;
import com.exonoxic.palimpsest.entity.ai.SqueezeNavigation;
import com.exonoxic.palimpsest.entity.ai.Squeezer;
import com.exonoxic.palimpsest.entity.apparition.Apparition;
import com.exonoxic.palimpsest.entity.apparition.ApparitionState;
import com.exonoxic.palimpsest.entity.apparition.Apparitions;
import com.exonoxic.palimpsest.horror.Spots;
import com.exonoxic.palimpsest.horror.WorldAlterations;
import com.exonoxic.palimpsest.registry.ModEntities;
import com.exonoxic.palimpsest.registry.ModSounds;
import com.exonoxic.palimpsest.util.Advancements;
import com.exonoxic.palimpsest.util.Sounds;
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
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * It comes to doors at night and knocks. Three times, then a wait, then three again. It will
 * not open a door. It does not need to: sooner or later, somebody inside does.
 *
 * <p>If nobody answers, it stops knocking. That is worse. It goes round the house, tapping on the
 * wall nearest wherever you are standing, dragging its nails along the outside, bending down at
 * the windows to look in. And every so often it tries the house again: any gap a block high will
 * do. If it finds one it comes through without a sound but the dragging, putting out the torches
 * as it passes, and only lets you see it when it is close. If it finds nothing, it goes back to
 * the door and knocks again, harder. Lanterns and the like, which it cannot put out, keep it
 * off, as do wards and the dawn.</p>
 *
 * <p>States: KNOCKING at a door; SEARCHING for another way in; LUNGE after the door opens (or
 * someone steps outside, or it gets in); LEAVING at dawn, when warded off, or when it gives up.</p>
 */
public class KnockerEntity extends Monster implements Apparition, Squeezer {
    public static final int KNOCKING = 1;
    public static final int LUNGE = 2;
    public static final int LEAVING = 3;
    public static final int SEARCHING = 4;
    /** Rounds of three knocks before it goes quiet and looks for another way in. */
    private static final int KNOCK_ROUNDS = 3;
    /** How long it searches before going back to knock again. */
    private static final int SEARCH_BEFORE_RETURN = 1200;
    private static final double PROWL_SPEED = 0.55D;
    private static final double CREEP_SPEED = 0.8D;

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
    @Nullable
    private BlockPos home;
    private int rounds;
    private boolean angry;
    private int searchTicks;
    private boolean creeping;
    @Nullable
    private BlockPos patrol;
    private boolean patrolIsWindow;
    private boolean returning;
    private int pause;
    private int taps;
    private int watching;
    private int lookedAt;
    private int brightTicks;
    private int heartbeatIn;
    private int silence;
    /** Bends double under a doorway, crawls through anything a block high. */
    public final Squeeze squeeze = Squeeze.of(this, 0.7F, 2.7F);

    public KnockerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 12;
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
            e.home = standAt.immutable();
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

    /** Whether it has found a way in and is coming through it. */
    public boolean isCreeping() {
        return creeping;
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
        goalSelector.addGoal(0, new Pursuit.Surface(this, () -> getState() == LUNGE));
        goalSelector.addGoal(0, new Pursuit.Chase(this, () -> getState() == LUNGE));
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
        creeping = false;
        watching = 0;
        squeeze.holdAtLeast(Pose.STANDING);
        entityData.set(STATE, LUNGE);
        entityData.set(VIEWER, Optional.empty());
        lungeTicks = 900;
        setTarget(target);
        playSound(ModSounds.KNOCKER_LUNGE.get(), 2.0F, 0.9F + random.nextFloat() * 0.15F);
    }

    private void leave() {
        if (getState() == LEAVING) return;
        squeeze.holdAtLeast(Pose.STANDING);
        entityData.set(STATE, LEAVING);
        setTarget(null);
        leaveTicks = 0;
    }

    @Override
    public void tick() {
        squeeze.tick(true);
        super.tick();
        if (level().isClientSide) return;
        int anim = entityData.get(KNOCK_ANIM);
        if (anim > 0) entityData.set(KNOCK_ANIM, anim - 1);
        switch (getState()) {
            case KNOCKING -> tickKnocking();
            case LUNGE -> tickLunge();
            case LEAVING -> tickLeaving();
            case SEARCHING -> tickSearching();
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
            // Back again after searching, it pounds rather than knocks.
            level().playSound(null, door, ModSounds.EVENT_KNOCK.get(), SoundSource.HOSTILE, angry ? 1.7F : 1.0F,
                    angry ? 0.72F + random.nextFloat() * 0.08F : 0.95F + random.nextFloat() * 0.1F);
            entityData.set(KNOCK_ANIM, 10);
            knocksLeft--;
            knockDelay = angry ? 8 : 12;
        } else if (knocksLeft == 0 && --knockCooldown <= 0) {
            // Three, and three, and three. Then nothing, which is worse.
            if (++rounds > KNOCK_ROUNDS) {
                startSearching();
                return;
            }
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
        // Torches it puts out as it comes; only light it cannot put out drives it off.
        snuffAround();
        if (tooBright(12)) {
            leave();
            return;
        }
        if (target instanceof Player p && WardHelper.isPlayerWarded(p) && distanceToSqr(p) < 36) leave();
    }

    // ------------------------------------------------------------------ looking for another way in

    private void startSearching() {
        entityData.set(STATE, SEARCHING);
        searchTicks = 0;
        creeping = false;
        returning = false;
        patrol = null;
        watching = 0;
        // It stops. For a while there is nothing to hear at all.
        silence = 60 + random.nextInt(60);
        pause = 0;
        taps = 0;
        getNavigation().stop();
    }

    private void tickSearching() {
        searchTicks++;
        squeeze.holdAtLeast(watching > 0 ? Squeeze.STOOP : Pose.STANDING);
        Player p = quarry();
        boolean dawn = level().isDay() && level().canSeeSky(blockPosition().above(2));
        if (p == null || --visit <= 0 || dawn || WardHelper.isWarded(level(), blockPosition(), 16)) {
            if (!opened && p instanceof ServerPlayer sp && sp.distanceToSqr(this) < 48 * 48) {
                Advancements.grant(sp, "three_knocks");
                BleedManager.unlock(sp, "knocker");
            }
            squeeze.holdAtLeast(Pose.STANDING);
            leave();
            return;
        }
        if (exposed(p)) {
            squeeze.holdAtLeast(Pose.STANDING);
            lunge(p);
            return;
        }
        if (creeping) {
            tickCreeping(p);
            return;
        }
        if (searchTicks <= silence) {
            getNavigation().stop();
            return;
        }
        // Every second it tries the house again: is there any way in at all?
        if (searchTicks % 20 == 0) {
            Path way = wayIn(p.blockPosition());
            if (way != null && way.canReach()) {
                creeping = true;
                watching = 0;
                patrol = null;
                getNavigation().moveTo(way, CREEP_SPEED);
                return;
            }
        }
        if (watching > 0) {
            tickWatching(p);
            return;
        }
        if (pause > 0) {
            pause--;
            getNavigation().stop();
            getLookControl().setLookAt(p, 10.0F, 10.0F);
            if (taps > 0 && pause % 15 == 0) {
                testWall(p);
                taps--;
            }
            return;
        }
        if (patrol == null) {
            if (searchTicks > SEARCH_BEFORE_RETURN && home != null && door != null) {
                returning = true;
                patrol = home;
                getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, PROWL_SPEED);
            } else {
                choosePatrol(p);
            }
            if (patrol == null) {
                pause = 40;
                return;
            }
        }
        boolean arrived = distanceToSqr(patrol.getX() + 0.5D, patrol.getY(), patrol.getZ() + 0.5D) < 2.0D;
        if (arrived || getNavigation().isDone()) {
            if (returning) {
                // Back at the door, it pounds on it: two rounds, harder and faster, then it goes looking again.
                returning = false;
                angry = true;
                rounds = KNOCK_ROUNDS - 2;
                knocksLeft = 0;
                knockCooldown = 10;
                entityData.set(STATE, KNOCKING);
            } else if (patrolIsWindow && arrived) {
                watching = 140 + random.nextInt(100);
                lookedAt = 0;
            } else {
                pause = 50 + random.nextInt(60);
                taps = 1 + random.nextInt(3);
            }
            patrol = null;
            return;
        }
        if (tickCount % 40 == 0) getNavigation().moveTo(patrol.getX() + 0.5D, patrol.getY(), patrol.getZ() + 0.5D, PROWL_SPEED);
    }

    /** Found a way in: it comes through quietly and only shows itself when it is close. */
    private void tickCreeping(Player p) {
        if (distanceToSqr(p) < 8 * 8 && hasLineOfSight(p)) {
            if (p instanceof ServerPlayer sp) Sounds.playTo(sp, ModSounds.EVENT_STINGER.get(), SoundSource.HOSTILE, sp.getEyePosition(), 0.9F, 1.0F);
            lunge(p);
            return;
        }
        // Only the one it came for hears it: their own heart, faster the closer it gets.
        if (p instanceof ServerPlayer sp && --heartbeatIn <= 0) {
            float near = (float) Mth.clamp(1.0D - Math.sqrt(distanceToSqr(sp)) / 24.0D, 0.0D, 1.0D);
            float pitch = 0.85F + 0.5F * near;
            Sounds.playTo(sp, ModSounds.EVENT_HEARTBEAT.get(), SoundSource.AMBIENT, sp.getEyePosition(), 0.3F + 0.5F * near, pitch);
            heartbeatIn = (int) (76 / pitch);
        }
        snuffAround();
        if (tooBright(13)) {
            leave();
            return;
        }
        if (getNavigation().isDone() || tickCount % 30 == 0) {
            Path way = wayIn(p.blockPosition());
            if (way == null || !way.canReach()) {
                // The way has been shut. It goes back to looking.
                creeping = false;
                getNavigation().stop();
                pause = 40;
                return;
            }
            getNavigation().moveTo(way, CREEP_SPEED);
        }
    }

    /** At a window, bent down to look in. Look back at it for long enough and it draws away. */
    private void tickWatching(Player p) {
        watching--;
        getNavigation().stop();
        getLookControl().setLookAt(p, 30.0F, 30.0F);
        if (seenThroughGlassBy(p)) {
            if (++lookedAt > 30) {
                watching = 0;
                lookedAt = 0;
                if (random.nextBoolean()) playSound(ModSounds.KNOCKER_AMBIENT.get(), 0.9F, 0.8F);
                Vec3 away = new Vec3(getX() - p.getX(), 0.0D, getZ() - p.getZ());
                away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
                BlockPos back = Spots.localFloor((ServerLevel) level(), Mth.floor(getX() + away.x * 4), Mth.floor(getY()), Mth.floor(getZ() + away.z * 4), 3);
                if (back != null) {
                    patrol = back;
                    patrolIsWindow = false;
                    getNavigation().moveTo(back.getX() + 0.5D, back.getY(), back.getZ() + 0.5D, 0.35D);
                }
            }
        } else if (watching % 50 == 0 && random.nextInt(2) == 0) {
            // A few taps on the glass.
            testWall(p);
        }
    }

    /**
     * A route from where it stands to {@code target}, worked out afresh ({@code canReach()} on the
     * result says whether it really gets there): how it looks for a way into a house.
     */
    @Nullable
    public Path wayIn(BlockPos target) {
        return getNavigation() instanceof SqueezeNavigation nav ? nav.freshPath(target, 0) : getNavigation().createPath(target, 0);
    }

    /** The player it came for, or failing that whoever is nearby and not in creative. */
    @Nullable
    private Player quarry() {
        Player p = apparition.target(level());
        if (p != null && p.isAlive() && !p.isSpectator() && p.level() == level()) return p;
        Player near = level().getNearestPlayer(this, 32);
        return near != null && !near.isCreative() && !near.isSpectator() ? near : null;
    }

    /** Out under the sky and in plain view: it simply comes for them. */
    private boolean exposed(Player p) {
        return !p.isCreative() && !p.isSpectator() && distanceToSqr(p) < 14 * 14 && hasLineOfSight(p)
                && level().canSeeSky(p.blockPosition().above()) && !WardHelper.isPlayerWarded(p);
    }

    /** Somewhere outside to stand next: at a window some of the time, otherwise anywhere round the house. */
    private void choosePatrol(Player p) {
        ServerLevel level = (ServerLevel) level();
        patrolIsWindow = false;
        if (random.nextInt(5) < 2) {
            BlockPos window = windowSpot(level, p);
            if (window != null) {
                patrol = window;
                patrolIsWindow = true;
                getNavigation().moveTo(window.getX() + 0.5D, window.getY(), window.getZ() + 0.5D, PROWL_SPEED);
                return;
            }
        }
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double r = 4.0D + random.nextDouble() * 4.0D;
            BlockPos floor = Spots.localFloor(level, Mth.floor(p.getX() + Math.cos(angle) * r), Mth.floor(getY()),
                    Mth.floor(p.getZ() + Math.sin(angle) * r), 4);
            // Outside, and out of the brightest light.
            if (floor == null || !level.canSeeSky(floor.above(2)) || level.getBrightness(LightLayer.BLOCK, floor.above()) >= 13) continue;
            Path path = wayIn(floor);
            if (path == null || !path.canReach()) continue;
            patrol = floor;
            getNavigation().moveTo(path, PROWL_SPEED);
            return;
        }
    }

    /** Outside a window near the player, on the far side of the glass from them. */
    @Nullable
    private BlockPos windowSpot(ServerLevel level, Player p) {
        BlockPos center = p.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -1, -8), center.offset(8, 3, 8))) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AbstractGlassBlock) && !(state.getBlock() instanceof IronBarsBlock)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos outside = pos.relative(d);
                if (outside.distSqr(center) <= pos.distSqr(center)) continue;
                BlockPos feet = Spots.localFloor(level, outside.getX(), outside.getY() - 1, outside.getZ(), 2);
                if (feet == null || !level.canSeeSky(feet.above(2))) continue;
                double dist = feet.distSqr(blockPosition());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = feet.immutable();
                }
            }
        }
        if (best == null) return null;
        Path path = wayIn(best);
        return path != null && path.canReach() ? best : null;
    }

    /** Is the player looking straight at it, with nothing but glass (or bars, or leaves) between? */
    private boolean seenThroughGlassBy(Player p) {
        Vec3 from = p.getEyePosition();
        Vec3 to = getEyePosition();
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 1.0E-3D || len > 24.0D || p.getViewVector(1.0F).dot(d.scale(1.0D / len)) < 0.95D) return false;
        int steps = Mth.ceil(len / 0.2D);
        for (int i = 1; i < steps; i++) {
            if (level().getBlockState(BlockPos.containing(from.add(d.scale(i / (double) steps)))).canOcclude()) return false;
        }
        return true;
    }

    /**
     * Knuckles on the wall between it and the player (or, now and then, its nails along it). On a
     * window it is a fingernail on the glass instead.
     */
    private void testWall(Player p) {
        BlockHitResult hit = level().clip(new ClipContext(getEyePosition().subtract(0.0D, 0.6D, 0.0D), p.getEyePosition(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        BlockPos at = hit.getType() == HitResult.Type.BLOCK ? hit.getBlockPos() : blockPosition().above();
        boolean glass = level().getBlockState(at).getSoundType() == SoundType.GLASS;
        boolean scratch = random.nextInt(4) == 0;
        SoundEvent sound = glass
                ? (scratch ? ModSounds.KNOCKER_GLASS_SCRATCH.get() : ModSounds.KNOCKER_GLASS_TAP.get())
                : (scratch ? ModSounds.KNOCKER_SCRATCH.get() : ModSounds.KNOCKER_TAP.get());
        level().playSound(null, at, sound, SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        if (!scratch) entityData.set(KNOCK_ANIM, 6);
    }

    /** Puts out torches within reach (when the world may be altered). */
    private void snuffAround() {
        if (tickCount % 4 != 0 || !CommonConfig.ALLOW_WORLD_ALTERATION.get() || !(level() instanceof ServerLevel level)) return;
        for (BlockPos pos : WorldAlterations.scan(level, blockPosition().above(), 2, 2, WorldAlterations::isTorch)) {
            WorldAlterations.snuff(level, pos);
        }
    }

    /** Standing in light it could not put out, for half a second. */
    private boolean tooBright(int level) {
        brightTicks = level().getBrightness(LightLayer.BLOCK, blockPosition().above()) >= level ? brightTicks + 1 : 0;
        return brightTicks > 10;
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
        if (hurt && !level().isClientSide && (getState() == KNOCKING || getState() == SEARCHING) && source.getEntity() instanceof Player p) lunge(p);
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
        tag.putInt("Rounds", rounds);
        tag.putBoolean("Angry", angry);
        if (door != null) tag.put("Door", NbtUtils.writeBlockPos(door));
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        apparition.load(tag);
        entityData.set(STATE, tag.contains("State") ? tag.getInt("State") : LEAVING);
        visit = tag.getInt("Visit");
        opened = tag.getBoolean("Opened");
        door = tag.contains("Door") ? NbtUtils.readBlockPos(tag.getCompound("Door")) : null;
        home = tag.contains("Home") ? NbtUtils.readBlockPos(tag.getCompound("Home")) : null;
        rounds = tag.getInt("Rounds");
        angry = tag.getBoolean("Angry");
    }
}
