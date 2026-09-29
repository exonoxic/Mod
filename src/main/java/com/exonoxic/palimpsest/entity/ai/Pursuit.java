package com.exonoxic.palimpsest.entity.ai;

import java.util.EnumSet;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

/**
 * How the hunters keep coming: water is no refuge and neither is a pillar.
 * <p>
 * They swim strongly, plan routes straight through water, never drown and dive after a player who
 * goes under. On land they take a full block in their stride, and when the one they are after is
 * above them they drag themselves up whatever wall is in the way, the way a spider does.
 * <p>
 * And they are in no hurry. When one first sets eyes on you it stops dead and {@link Stare}s for a
 * few seconds, turned full towards you, before it comes. Hurting it or walking up to it cuts
 * that short.
 * <p>
 * Hunters get {@link #attributes} for their stride and stroke and {@link #install} for their
 * goals: {@link Surface} in place of {@link FloatGoal}, {@link Chase} (which takes flags from
 * nobody and only acts while the creature is actually hunting) and, for all but the bosses, Stare.
 */
public final class Pursuit {
    /** Water-stroke multiplier (1 is a zombie flailing; drowned swim at about 2). */
    private static final double SWIM_SPEED = 2.2D;
    /** On top of the vanilla 0.6: a full block without a hop. */
    private static final double STEP_UP = 0.5D;
    private static final double CLIMB_SPEED = 0.22D;
    private static final double DIVE_PULL = 0.035D;
    /** How far off (horizontally) it will go straight for a target it has no path to. */
    private static final double DIRECT_RANGE = 20.0D;
    /** Within this (horizontally) of a target above it, it stops pathing and climbs. */
    private static final double CLOSE_RANGE = 3.0D;

    /** Closer than this and it stops staring and comes. */
    private static final double STARE_BREAK_RANGE = 3.5D;
    /** Lose sight of it for this long and the next sighting gets a fresh stare. */
    private static final int FORGET_TICKS = 400;

    /**
     * Adds the hunting goals. Surface and Chase go in at {@code priority} (where FloatGoal was);
     * Stare, when {@code stareMax > 0}, at 1, ahead of the melee goals so it holds them off.
     */
    public static void install(PathfinderMob mob, GoalSelector goals, int priority, BooleanSupplier hunting, int stareMin, int stareMax) {
        Stare stare = stareMax > 0 ? new Stare(mob, hunting, stareMin, stareMax) : null;
        goals.addGoal(priority, new Surface(mob, hunting));
        goals.addGoal(priority, new Chase(mob, stare == null ? hunting : () -> hunting.getAsBoolean() && !stare.isStaring()));
        if (stare != null) goals.addGoal(1, stare);
    }

    public static AttributeSupplier.Builder attributes(AttributeSupplier.Builder builder) {
        return builder.add(ForgeMod.SWIM_SPEED.get(), SWIM_SPEED).add(ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_UP);
    }

    /** The target it is hunting, or null while it is doing anything else. */
    private static LivingEntity quarry(Mob mob, BooleanSupplier hunting) {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive() && hunting.getAsBoolean() ? target : null;
    }

    /** Going under after someone below it. */
    private static boolean diving(Mob mob, LivingEntity quarry) {
        return quarry != null && mob.isInWater() && quarry.getY() < mob.getY() - 0.8D;
    }

    /** Keeps it afloat like {@link FloatGoal}, except when it means to go under. */
    public static class Surface extends FloatGoal {
        private final Mob mob;
        private final BooleanSupplier hunting;

        public Surface(Mob mob, BooleanSupplier hunting) {
            super(mob);
            this.mob = mob;
            this.hunting = hunting;
            // Water is a way through, not a wall.
            mob.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
            mob.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        }

        @Override
        public boolean canUse() {
            return super.canUse() && !diving(mob, quarry(mob, hunting));
        }
    }

    /** Swimming, diving, climbing and breath. Needs no flags, so it never gets in another goal's way. */
    public static class Chase extends Goal {
        private final Mob mob;
        private final BooleanSupplier hunting;
        /** Ticks it keeps going straight for the target after last touching the wall (to get over the lip). */
        private int climbing;

        public Chase(Mob mob, BooleanSupplier hunting) {
            this.mob = mob;
            this.hunting = hunting;
        }

        @Override
        public boolean canUse() {
            return mob.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (mob.isInWater()) mob.setAirSupply(mob.getMaxAirSupply());
            LivingEntity quarry = quarry(mob, hunting);
            if (quarry == null) return;

            double dx = quarry.getX() - mob.getX();
            double dz = quarry.getZ() - mob.getZ();
            double above = quarry.getY() - mob.getY();
            boolean near = dx * dx + dz * dz < DIRECT_RANGE * DIRECT_RANGE;

            if (mob.isInWater()) {
                if (diving(mob, quarry)) mob.setDeltaMovement(mob.getDeltaMovement().add(0.0D, -DIVE_PULL, 0.0D));
                // No route through open water (or it gave up on one): just swim at them.
                if (near && mob.getNavigation().isDone()) mob.getMoveControl().setWantedPosition(quarry.getX(), quarry.getY(), quarry.getZ(), 1.0D);
                climbing = 0;
                return;
            }

            // A pillar, a ledge, a wall between: once it is at the foot of it, or already on the
            // wall, it stops pathing (paths lead away along the foot) and goes straight up and over.
            boolean close = dx * dx + dz * dz < CLOSE_RANGE * CLOSE_RANGE;
            if (above > 0.1D && (climbing > 0 || (above > 1.0D && (close || mob.getNavigation().isDone()) && near))) {
                mob.getNavigation().stop();
                mob.getMoveControl().setWantedPosition(quarry.getX(), quarry.getY(), quarry.getZ(), 1.0D);
            }
            boolean onWall = above > 0.1D && mob.horizontalCollision;
            climbing = onWall ? 15 : Math.max(0, climbing - 1);
            if (onWall) {
                Vec3 v = mob.getDeltaMovement();
                mob.setDeltaMovement(v.x, Math.max(v.y, CLIMB_SPEED), v.z);
                mob.resetFallDistance();
            }
        }

        @Override
        public void stop() {
            climbing = 0;
        }
    }

    /**
     * The first sighting: it stops, turns to face you and watches, for a few seconds, before it
     * comes. Holds the movement and look flags, so the attack goals below it wait.
     */
    public static class Stare extends Goal {
        private final PathfinderMob mob;
        private final BooleanSupplier hunting;
        private final int min;
        private final int max;
        private UUID staredAt;
        private int unseen;
        private int left;
        private float healthAtStart;

        public Stare(PathfinderMob mob, BooleanSupplier hunting, int min, int max) {
            this.mob = mob;
            this.hunting = hunting;
            this.min = min;
            this.max = max;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean isStaring() {
            return left > 0;
        }

        @Override
        public boolean canUse() {
            LivingEntity quarry = quarry(mob, hunting);
            if (quarry == null || !mob.hasLineOfSight(quarry)) {
                if (staredAt != null && ++unseen > FORGET_TICKS) staredAt = null;
                return false;
            }
            unseen = 0;
            if (quarry.getUUID().equals(staredAt)) return false;
            staredAt = quarry.getUUID();
            // Already hurt by it, or it walked right up: no time for looking.
            return mob.getLastHurtByMob() != quarry && mob.distanceToSqr(quarry) > STARE_BREAK_RANGE * STARE_BREAK_RANGE;
        }

        @Override
        public void start() {
            left = min + mob.getRandom().nextInt(Math.max(1, max - min + 1));
            healthAtStart = mob.getHealth();
            mob.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity quarry = quarry(mob, hunting);
            return left > 0 && quarry != null && mob.getHealth() >= healthAtStart
                    && mob.distanceToSqr(quarry) > STARE_BREAK_RANGE * STARE_BREAK_RANGE;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            left--;
            LivingEntity quarry = mob.getTarget();
            mob.getNavigation().stop();
            if (quarry != null) {
                mob.getLookControl().setLookAt(quarry, 10.0F, mob.getMaxHeadXRot());
                // Turn the whole body to face it, slowly.
                mob.setYBodyRot(mob.yHeadRot);
            }
        }

        @Override
        public void stop() {
            left = 0;
        }
    }

    private Pursuit() {}
}
