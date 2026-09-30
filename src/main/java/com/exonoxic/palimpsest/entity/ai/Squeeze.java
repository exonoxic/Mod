package com.exonoxic.palimpsest.entity.ai;

import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Lets a tall creature fold itself down to get through gaps it has no business fitting through:
 * bent double under anything two blocks high, flat on its belly through a one-block hole.
 * <p>
 * The server picks the tallest posture that fits both where the creature is and the next steps
 * of its path, so it drops before it reaches the gap rather than hitting its head on it. It
 * shrinks at once but only straightens up again once there has been room for a moment. The pose
 * is synced like any other (stooping is {@link Pose#CROUCHING}, crawling {@link Pose#SWIMMING});
 * the client eases the model between postures.
 * <p>
 * It is heard before it is seen: joints crack as it folds itself down, and while it crawls the
 * floor carries the sound of something dragging itself along on its elbows.
 * <p>
 * Routes are planned for its crawling body ({@link SqueezeNavigation}), so any gap a block high
 * counts as a way through, including one it has to climb up into.
 */
public final class Squeeze {
    public static final Pose STOOP = Pose.CROUCHING;
    public static final Pose CRAWL = Pose.SWIMMING;
    /** Ticks of headroom before it straightens up again. */
    private static final int ROOMY_TICKS = 10;
    /** Model ease per tick between postures. */
    private static final float EASE = 0.16F;
    /** Folded down it goes faster, not slower: bent double it lopes, flat out it scuttles. */
    private static final double STOOP_SPEED = 0.35D;
    private static final double CRAWL_SPEED = 0.8D;
    private static final UUID SCUTTLE = UUID.fromString("5c0e7f1a-3a8e-4d51-9f0b-7a1e2c9d4b63");

    private final Mob mob;
    private final EntityDimensions stand;
    private final EntityDimensions stoop;
    private final EntityDimensions crawl;
    private int roomy;
    private int nextSound;
    private Pose held = Pose.STANDING;
    private float crawlO;
    private float crawlNow;
    private float stoopO;
    private float stoopNow;

    public Squeeze(Mob mob, EntityDimensions stand, EntityDimensions stoop, EntityDimensions crawl) {
        this.mob = mob;
        this.stand = stand;
        this.stoop = stoop;
        this.crawl = crawl;
    }

    /** Standing is the creature's usual size; stooping fits under 2 blocks, crawling under 1. */
    public static Squeeze of(Mob mob, float width, float height) {
        return new Squeeze(mob, EntityDimensions.scalable(width, height), EntityDimensions.scalable(width, 1.85F),
                EntityDimensions.scalable(width, 0.9F));
    }

    public EntityDimensions dimensions(Pose pose) {
        if (pose == CRAWL) return crawl;
        if (pose == STOOP) return stoop;
        return stand;
    }

    public boolean isSqueezed() {
        Pose pose = mob.getPose();
        return pose == CRAWL || pose == STOOP;
    }

    /**
     * Call at the start of the creature's tick, before {@code super.tick()} (so the new hitbox is
     * in place before the game checks whether its head is in a wall). {@code free} is false while
     * it must hold its posture (a Longhand being watched).
     */
    public void tick(boolean free) {
        if (mob.level().isClientSide) {
            crawlO = crawlNow;
            stoopO = stoopNow;
            Pose pose = mob.getPose();
            crawlNow = approach(crawlNow, pose == CRAWL ? 1.0F : 0.0F);
            stoopNow = approach(stoopNow, pose == STOOP ? 1.0F : 0.0F);
            return;
        }
        Pose current = mob.getPose();
        // Dying, sleeping and the like are not ours to change.
        if (free && rank(current) >= 0) {
            Pose fits = tallestFitting();
            if (rank(held) > rank(fits)) fits = held;
            if (rank(fits) > rank(current)) {
                roomy = 0;
                mob.setPose(fits);
                mob.playSound(ModSounds.SQUEEZE_CRACK.get(), fits == CRAWL ? 1.0F : 0.7F, 0.85F + mob.getRandom().nextFloat() * 0.3F);
            } else if (rank(fits) < rank(current)) {
                if (++roomy >= ROOMY_TICKS) {
                    roomy = 0;
                    mob.setPose(fits);
                }
            } else {
                roomy = 0;
            }
            sounds();
        }
        scuttle(mob.getPose());
    }

    private void scuttle(Pose pose) {
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double want = pose == CRAWL ? CRAWL_SPEED : pose == STOOP ? STOOP_SPEED : 0.0D;
        AttributeModifier had = speed.getModifier(SCUTTLE);
        if (had != null && had.getAmount() == want) return;
        if (had != null) speed.removeModifier(SCUTTLE);
        if (want > 0.0D) speed.addTransientModifier(new AttributeModifier(SCUTTLE, "Squeezed scuttle", want, AttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    private void sounds() {
        Pose pose = mob.getPose();
        Vec3 motion = mob.getDeltaMovement();
        boolean moving = motion.x * motion.x + motion.z * motion.z > 4.0E-4D;
        if (--nextSound > 0 || !moving || (pose != CRAWL && pose != STOOP)) return;
        float pitch = 0.85F + mob.getRandom().nextFloat() * 0.25F;
        if (pose == CRAWL) {
            mob.playSound(ModSounds.SQUEEZE_DRAG.get(), 0.9F, pitch);
            nextSound = 18 + mob.getRandom().nextInt(10);
        } else {
            // Bent double, its joints complain at every other step.
            mob.playSound(ModSounds.SQUEEZE_CRACK.get(), 0.3F, pitch * 0.9F);
            nextSound = 30 + mob.getRandom().nextInt(25);
        }
    }

    /** Keeps it at least this low whether or not it needs to be (a Knocker bending to look in at a window). */
    public void holdAtLeast(Pose pose) {
        held = rank(pose) < 0 ? Pose.STANDING : pose;
    }

    /** How tall it is lying flat. */
    public float crawlHeight() {
        return crawl.height;
    }

    /** 0..1 how far into the crawl the model is (client). */
    public float crawl(float partialTick) {
        return smooth(Mth.lerp(partialTick, crawlO, crawlNow));
    }

    /** 0..1 how far into the stoop the model is (client). */
    public float stoop(float partialTick) {
        return smooth(Mth.lerp(partialTick, stoopO, stoopNow));
    }

    /** Culling box: lying down, the model reaches well beyond the hitbox. */
    public AABB cullingBox(AABB box) {
        return isSqueezed() ? box.inflate(2.0D, 0.5D, 2.0D) : box;
    }

    private Pose tallestFitting() {
        List<Vec3> spots = lookahead();
        // Where not even its crawling body fits is a wall it is brushing past, not a gap to fold
        // itself into (unless it is where it already is).
        for (int i = spots.size() - 1; i > 0; i--) {
            if (!fits(crawl, spots.get(i))) spots.remove(i);
        }
        if (fitsAll(stand, spots)) return Pose.STANDING;
        if (fitsAll(stoop, spots)) return STOOP;
        return CRAWL;
    }

    /** Where it is, the next two steps of its path, and just ahead in the direction it is moving. */
    private List<Vec3> lookahead() {
        List<Vec3> spots = new ArrayList<>(4);
        Vec3 here = mob.position();
        spots.add(here);
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            int end = Math.min(path.getNodeCount(), path.getNextNodeIndex() + 2);
            for (int i = path.getNextNodeIndex(); i < end; i++) {
                BlockPos node = path.getNodePos(i);
                spots.add(new Vec3(node.getX() + 0.5D, Math.max(node.getY(), here.y), node.getZ() + 0.5D));
            }
        }
        Vec3 motion = mob.getDeltaMovement();
        Vec3 flat = new Vec3(motion.x, 0.0D, motion.z);
        if (flat.lengthSqr() > 1.0E-4D) spots.add(here.add(flat.normalize().scale(0.6D)));
        return spots;
    }

    private boolean fitsAll(EntityDimensions dims, List<Vec3> spots) {
        for (Vec3 spot : spots) {
            if (!fits(dims, spot)) return false;
        }
        return true;
    }

    private boolean fits(EntityDimensions dims, Vec3 spot) {
        return mob.level().noCollision(mob, dims.makeBoundingBox(spot).deflate(1.0E-7D));
    }

    /** Standing 0, stooping 1, crawling 2; any other pose -1. */
    private static int rank(Pose pose) {
        if (pose == Pose.STANDING) return 0;
        if (pose == STOOP) return 1;
        if (pose == CRAWL) return 2;
        return -1;
    }

    private static float approach(float from, float to) {
        return from < to ? Math.min(to, from + EASE) : Math.max(to, from - EASE);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
