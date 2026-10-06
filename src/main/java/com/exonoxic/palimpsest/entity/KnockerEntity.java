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
 * <p>And when it does have you in view (you stepped outside, or it got in), it does not come at
 * once. It follows, keeping its distance, stopping dead whenever you look at it and closing in
 * when you look away, for a quarter of a minute or so. Then it comes. Walking up to it, or
 * hurting it, ends the waiting early.</p>
 *
 * <p>States: KNOCKING at a door; SEARCHING for another way in; STALKING someone it can see;
 * LUNGE when it comes for them (or the door is opened in its face); LEAVING at dawn, when warded
 * off, or when it gives up.</p>
 */
public class KnockerEntity extends Monster implements Apparition, Squeezer {
    public static final int KNOCKING = 1;
    public static final int LUNGE = 2;
    public static final int LEAVING = 3;
    public static final int SEARCHING = 4;
    public static final int STALKING = 5;
    /**
     * Gestures, each timed to the hits in its sound: three knocks on a door (event.knock holds all
     * three), the same pounded slower and harder, knuckles rapped on a wall, slow taps on a window,
     * and nails dragged down either.
     */
    public static final int GESTURE_KNOCK = 1;
    public static final int GESTURE_TAP = 2;
    public static final int GESTURE_SCRATCH = 3;
    public static final int GESTURE_POUND = 4;
    public static final int GESTURE_GLASS_TAP = 5;
    /** Ticks a gesture starts before its sound: the arm draws back first, and the blow lands on the sound. */
    public static final int GESTURE_LEAD = 4;
    /**
     * How far from a wall it stands to knock or scratch on it, in blocks: its hunched head hangs
     * about a block out in front of its feet, so standing in the next block over would put the head
     * through the door. The model's gesture poses are solved for these distances.
     */
    public static final double REACH = 0.9D;
    /** The same when bent down at a window (its head hangs a little further forward then). */
    public static final double REACH_STOOPED = 1.1D;
    /** Pitch of the pounding (a lower pitch plays the knocks further apart). */
    public static final float POUND_PITCH = 0.76F;
    /** Rounds of three knocks before it goes quiet and looks for another way in. */
    private static final int KNOCK_ROUNDS = 3;
    /** How long it searches before going back to knock again. */
    private static final int SEARCH_BEFORE_RETURN = 1200;
    private static final double PROWL_SPEED = 0.55D;
    private static final double CREEP_SPEED = 0.8D;
    private static final double STALK_SPEED = 0.5D;
    /** How long it shadows someone before it comes for them, in ticks: out in the open, and once it is in the house with them. */
    private static final int STALK_MIN = 160;
    private static final int STALK_MAX = 320;
    private static final int STALK_INSIDE_MIN = 60;
    private static final int STALK_INSIDE_MAX = 140;
    /** Stalking, it comes no nearer than this; and anyone who comes this near to it has stopped being stalked. */
    private static final double STALK_KEEP = 6.0D;
    private static final double STALK_BREAK = 3.5D;
    /** With nobody to haunt (a spawn egg, a command), it waits this long for someone before it goes. */
    private static final int UNBOUND_WAIT = 600;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(KnockerEntity.class, EntityDataSerializers.INT);
    /** The latest gesture: a count in the high bits, so the same gesture twice still reads as new, and its kind in the low three. */
    private static final EntityDataAccessor<Integer> GESTURE = SynchedEntityData.defineId(KnockerEntity.class, EntityDataSerializers.INT);
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
    /** The glass (or bars) of the window it is going to. */
    @Nullable
    private BlockPos windowGlass;
    private boolean returning;
    private int pause;
    private int taps;
    private int watching;
    private int lookedAt;
    private int brightTicks;
    private int heartbeatIn;
    private int silence;
    private int stalkTicks;
    /** Ticks since it last had whoever it is after in view (stalking or lunging). */
    private int lostSight;
    private int unbound;
    private int gestures;
    /** A gesture's sound, waiting out {@link #GESTURE_LEAD}. */
    @Nullable
    private SoundEvent gestureSound;
    private BlockPos gestureSoundAt = BlockPos.ZERO;
    private float gestureVolume;
    private float gesturePitch;
    private int gestureSoundIn;
    /** Client: the gesture being played and the tick it started on. */
    private int gestureKind;
    private int gestureStart = -1000;
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
        k.standOff(k.towards(door), REACH);
        Vec3 to = Vec3.atCenterOf(door).subtract(k.position());
        float yaw = (float) (Math.toDegrees(Math.atan2(to.z, to.x)) - 90.0D);
        k.setYRot(yaw);
        k.setYHeadRot(yaw);
        k.setYBodyRot(yaw);
        BleedManager.unlock(player, "knocking");
        return true;
    }

    /** Arrives already hunting (a gate read in a storm, a rite gone wrong): a short look, then it comes. */
    public static void comeThrough(ServerLevel level, BlockPos pos, ServerPlayer player) {
        KnockerEntity k = Apparitions.spawn(ModEntities.KNOCKER.get(), level, pos, player, e -> {
            e.apparition.begin(player, -1, 0);
            e.setPersistenceRequired();
        });
        if (k != null) k.stalk(player, STALK_INSIDE_MIN, STALK_INSIDE_MAX);
    }

    /** Whether it has found a way in and is coming through it. */
    public boolean isCreeping() {
        return creeping;
    }

    public int getState() {
        return entityData.get(STATE);
    }

    /** Starts a gesture on every watching client, and its sound {@link #GESTURE_LEAD} ticks later. */
    private void gesture(int kind, SoundEvent sound, BlockPos at, float volume, float pitch) {
        entityData.set(GESTURE, (++gestures << 3) | kind);
        gestureSound = sound;
        gestureSoundAt = at.immutable();
        gestureVolume = volume;
        gesturePitch = pitch;
        gestureSoundIn = GESTURE_LEAD;
    }

    /** Plays a gesture on this client (the server's arrive through {@link #GESTURE}). */
    public void playGesture(int kind) {
        gestureKind = kind;
        gestureStart = tickCount;
    }

    /** Client: the gesture being played, or last played. */
    public int gestureKind() {
        return gestureKind;
    }

    /** Client: ticks since that gesture began. */
    public float gestureTime(float partialTick) {
        return tickCount - gestureStart + partialTick;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GESTURE.equals(key) && level().isClientSide && entityData.get(GESTURE) != 0) playGesture(entityData.get(GESTURE) & 7);
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
        // Nobody's in particular until it is given a door or a quarry (see tickStalking).
        entityData.define(STATE, STALKING);
        entityData.define(GESTURE, 0);
        entityData.define(VIEWER, Optional.empty());
    }

    @Override
    protected void registerGoals() {
        Pursuit.install(this, goalSelector, 0, () -> getState() == LUNGE, 25, 60);
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

    private void lunge(LivingEntity target) {
        creeping = false;
        watching = 0;
        lostSight = 0;
        squeeze.holdAtLeast(Pose.STANDING);
        entityData.set(STATE, LUNGE);
        entityData.set(VIEWER, Optional.empty());
        lungeTicks = 900;
        setTarget(target);
        playSound(ModSounds.KNOCKER_LUNGE.get(), 2.0F, 0.9F + random.nextFloat() * 0.15F);
    }

    /** Has someone in view and starts to shadow them: {@link #STALK_MIN} to {@link #STALK_MAX} ticks of it, then it comes. */
    public void stalk(LivingEntity target) {
        stalk(target, STALK_MIN, STALK_MAX);
    }

    private void stalk(LivingEntity target, int min, int max) {
        creeping = false;
        watching = 0;
        lostSight = 0;
        squeeze.holdAtLeast(Pose.STANDING);
        entityData.set(STATE, STALKING);
        stalkTicks = min + random.nextInt(max - min + 1);
        setTarget(target);
        getNavigation().stop();
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
        if (gestureSound != null && --gestureSoundIn <= 0) {
            level().playSound(null, gestureSoundAt, gestureSound, SoundSource.HOSTILE, gestureVolume, gesturePitch);
            gestureSound = null;
        }
        switch (getState()) {
            case KNOCKING -> tickKnocking();
            case LUNGE -> tickLunge();
            case LEAVING -> tickLeaving();
            case SEARCHING -> tickSearching();
            case STALKING -> tickStalking();
            default -> leave();
        }
    }

    private void tickKnocking() {
        getNavigation().stop();
        if (door == null || !(level().getBlockState(door).getBlock() instanceof DoorBlock)) {
            // The door has gone (broken down, burnt). That is not a reason to leave: it is a way in.
            door = null;
            startSearching();
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
            // One sound holds the three knocks. Back again after searching, it pounds rather than knocks.
            gesture(angry ? GESTURE_POUND : GESTURE_KNOCK, ModSounds.EVENT_KNOCK.get(), door, angry ? 1.7F : 1.0F,
                    angry ? POUND_PITCH : 0.97F + random.nextFloat() * 0.06F);
            knocksLeft--;
        } else if (knocksLeft == 0 && --knockCooldown <= 0) {
            // Three, and three, and three. Then nothing, which is worse.
            if (++rounds > KNOCK_ROUNDS) {
                startSearching();
                return;
            }
            knocksLeft = 1;
            knockDelay = 0;
            knockCooldown = 160 + random.nextInt(220);
        }
        Player outside = level().getNearestPlayer(getX(), getY(), getZ(), 14, true);
        if (outside != null && exposed(outside)) {
            stalk(outside);
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
        if (gone(target) || --lungeTicks <= 0) {
            leave();
            return;
        }
        // Torches it puts out as it comes; only light it cannot put out drives it off.
        snuffAround();
        if (tooBright(12)) {
            leave();
            return;
        }
        if (target instanceof Player p && WardHelper.isPlayerWarded(p) && distanceToSqr(p) < 36) {
            leave();
            return;
        }
        // They got indoors and shut it out. It does not stand at the wall until it gives up: it goes
        // back to trying the house.
        if (shutOut(target)) besiege();
    }

    // ------------------------------------------------------------------ following, before it comes

    private void tickStalking() {
        LivingEntity target = getTarget();
        if (target == null) {
            // Nobody's yet (a spawn egg, a command, a reload): whoever comes near will do.
            Player near = level().getNearestPlayer(getX(), getY(), getZ(), 24, true);
            if (near != null) stalk(near);
            else if (++unbound > UNBOUND_WAIT) leave();
            return;
        }
        if (gone(target)) {
            leave();
            return;
        }
        snuffAround();
        if (tooBright(13) || (target instanceof Player w && WardHelper.isPlayerWarded(w) && distanceToSqr(w) < 36)) {
            leave();
            return;
        }
        double distSq = distanceToSqr(target);
        boolean sees = hasLineOfSight(target);
        // Walk up to it and it stops waiting.
        if (sees && distSq < STALK_BREAK * STALK_BREAK) {
            lunge(target);
            return;
        }
        if (shutOut(target)) {
            besiege();
            return;
        }
        if (sees && --stalkTicks <= 0) {
            lunge(target);
            return;
        }
        if (target instanceof ServerPlayer sp) heartbeat(sp);
        getLookControl().setLookAt(target, 30.0F, 30.0F);
        // It stops dead while they look at it, and closes in while they do not; never quite into reach.
        if ((sees && lookedAtBy(target)) || (sees && distSq < STALK_KEEP * STALK_KEEP)) {
            getNavigation().stop();
        } else if (getNavigation().isDone() || tickCount % 10 == 0) {
            getNavigation().moveTo(target, STALK_SPEED);
        }
    }

    private static boolean gone(@Nullable LivingEntity target) {
        return target == null || !target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** Out of sight for four seconds and more, with no way through to them: they are behind a wall it cannot pass. */
    private boolean shutOut(LivingEntity target) {
        if (hasLineOfSight(target)) {
            lostSight = 0;
            return false;
        }
        if (++lostSight < 80 || lostSight % 20 != 0) return false;
        Path way = wayIn(target.blockPosition());
        return way == null || !way.canReach();
    }

    /** Back to going round the house, for a while at least. */
    private void besiege() {
        setTarget(null);
        visit = Math.max(visit, 1200);
        startSearching();
    }

    /** Is this one looking more or less straight at it? */
    private boolean lookedAtBy(LivingEntity viewer) {
        Vec3 to = getEyePosition().subtract(viewer.getEyePosition());
        double len = to.length();
        return len > 1.0E-3D && viewer.getViewVector(1.0F).dot(to.scale(1.0D / len)) > 0.9D;
    }

    /** Only the one it came for hears it: their own heart, faster the closer it gets. */
    private void heartbeat(ServerPlayer sp) {
        if (--heartbeatIn > 0) return;
        float near = (float) Mth.clamp(1.0D - Math.sqrt(distanceToSqr(sp)) / 24.0D, 0.0D, 1.0D);
        float pitch = 0.85F + 0.5F * near;
        Sounds.playTo(sp, ModSounds.EVENT_HEARTBEAT.get(), SoundSource.AMBIENT, sp.getEyePosition(), 0.3F + 0.5F * near, pitch);
        heartbeatIn = (int) (76 / pitch);
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
            stalk(p);
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
                if (door != null) standOff(towards(door), REACH);
            } else if (patrolIsWindow && arrived) {
                watching = 140 + random.nextInt(100);
                lookedAt = 0;
                if (windowGlass != null) standOff(towards(windowGlass), REACH_STOOPED);
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
            // In the room with them, and seen. It still does not come at once.
            if (p instanceof ServerPlayer sp) Sounds.playTo(sp, ModSounds.EVENT_STINGER.get(), SoundSource.HOSTILE, sp.getEyePosition(), 0.9F, 1.0F);
            stalk(p, STALK_INSIDE_MIN, STALK_INSIDE_MAX);
            return;
        }
        if (p instanceof ServerPlayer sp) heartbeat(sp);
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
        BlockPos bestGlass = null;
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
                    bestGlass = pos.immutable();
                }
            }
        }
        if (best == null) return null;
        Path path = wayIn(best);
        if (path == null || !path.canReach()) return null;
        windowGlass = bestGlass;
        return best;
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

    /** The way (north, east...) from where it stands to the middle of a block, or null if it is right on it. */
    @Nullable
    private Direction towards(BlockPos target) {
        double dx = target.getX() + 0.5D - getX();
        double dz = target.getZ() + 0.5D - getZ();
        return dx * dx + dz * dz < 1.0E-4D ? null : Direction.getNearest(dx, 0.0D, dz);
    }

    /**
     * Stands it {@code reach} blocks from whatever wall (a door panel, glass, bars) is in front of it
     * in direction {@code toward}: a ray at head height finds the wall wherever it is (a door's panel
     * is on one edge of its block, glass on another), and it steps forward or back to the distance
     * its face and fist need, as far as there is room for.
     */
    private void standOff(@Nullable Direction toward, double reach) {
        if (toward == null || level().isClientSide) return;
        double sx = toward.getStepX();
        double sz = toward.getStepZ();
        Vec3 from = new Vec3(getX() - sx * 0.5D, getY() + 1.5D, getZ() - sz * 0.5D);
        BlockHitResult hit = level().clip(new ClipContext(from, from.add(sx * 3.0D, 0.0D, sz * 3.0D),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.BLOCK) return;
        // How far ahead of its feet the wall is, and how far it has to go to be `reach` from it.
        double move = Mth.clamp(from.distanceTo(hit.getLocation()) - 0.5D - (reach + 0.03D), -0.6D, 0.8D);
        for (int i = 0; i <= 6; i++) {
            double m = move * (1.0D - i / 6.0D);
            if (level().noCollision(this, getBoundingBox().move(sx * m, 0.0D, sz * m))) {
                setPos(getX() + sx * m, getY(), getZ() + sz * m);
                return;
            }
        }
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
        float pitch = 0.95F + random.nextFloat() * 0.1F;
        // Right up at the wall it raps or drags on it; from across the yard it only makes the sound.
        if (hit.getType() == HitResult.Type.BLOCK && getEyePosition().distanceTo(hit.getLocation()) < 2.4D) {
            standOff(towards(at), isCrouching() ? REACH_STOOPED : REACH);
            gesture(scratch ? GESTURE_SCRATCH : glass ? GESTURE_GLASS_TAP : GESTURE_TAP, sound, at, 1.0F, pitch);
        } else {
            level().playSound(null, at, sound, SoundSource.HOSTILE, 1.0F, pitch);
        }
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
        // Hurting it ends any waiting.
        if (hurt && !level().isClientSide && getState() != LUNGE && getState() != LEAVING && source.getEntity() instanceof Player p
                && !p.isCreative()) lunge(p);
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
        // No state at all is one made by /summon or an egg: it keeps the one it was made with. One
        // saved mid-charge has lost whoever it was after, and looks for them again.
        if (tag.contains("State")) entityData.set(STATE, tag.getInt("State") == LUNGE ? STALKING : tag.getInt("State"));
        visit = tag.getInt("Visit");
        opened = tag.getBoolean("Opened");
        door = tag.contains("Door") ? NbtUtils.readBlockPos(tag.getCompound("Door")) : null;
        home = tag.contains("Home") ? NbtUtils.readBlockPos(tag.getCompound("Home")) : null;
        rounds = tag.getInt("Rounds");
        angry = tag.getBoolean("Angry");
    }
}
