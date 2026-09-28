package com.exonoxic.palimpsest.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Ground navigation for creatures with a {@link Squeeze}: paths may run through any gap one
 * block high, whatever the creature's standing height, because it will crawl through, and may
 * climb up into such a gap even from under a low ceiling.
 */
public class SqueezeNavigation extends GroundPathNavigation {
    /** Set by createPathFinder, which the superclass constructor calls (so no initialiser here). */
    private PathFinder finder;

    public SqueezeNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new SqueezeNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        this.finder = new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        return this.finder;
    }

    /**
     * A route to {@code target} worked out from scratch, without touching the route being followed.
     * ({@link #createPath} hands back the current path whenever it thinks it is aimed at the same
     * place, which is no use for asking "is there any way in from here, now?".)
     */
    @Nullable
    public Path freshPath(BlockPos target, int accuracy) {
        if (!canUpdatePath()) return null;
        BlockPos goal = target;
        // Someone standing on a slab, on soul sand, or a hair below the top of a block is "in" the
        // floor block: aim at the space above it, which is where a route can end.
        if (!level.getBlockState(goal).isPathfindable(level, goal, PathComputationType.LAND)) goal = goal.above();
        // And at the ground under the target, as GroundPathNavigation does.
        for (int i = 0; i < 4 && level.getBlockState(goal.below()).isAir(); i++) goal = goal.below();
        // The path finder charges each node with the length of whichever route last looked at it,
        // not the shortest, so the walk all the way round a house to its one gap is counted as far
        // longer than it is: allow twice the usual distance, and four times the usual budget of nodes.
        float reach = 2.0F * (float) mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        int r = (int) reach + 8;
        BlockPos from = mob.blockPosition();
        PathNavigationRegion region = new PathNavigationRegion(level, from.offset(-r, -r, -r), from.offset(r, r, r));
        return finder.findPath(region, mob, Set.of(goal), reach, accuracy, 4.0F);
    }

    private static class SqueezeNodeEvaluator extends WalkNodeEvaluator {
        @Override
        public void prepare(PathNavigationRegion region, Mob mob) {
            super.prepare(region, mob);
            // Only the block it crawls through needs to be clear, not its standing height.
            this.entityHeight = 1;
        }

        /**
         * The vanilla evaluator only lets a mob step up onto a block if there is room for its full
         * height above where it stands; something that can crawl needs far less. When a step up onto
         * a solid block is refused, it is tried again with room for a crawling body.
         */
        @Override
        @Nullable
        protected Node findAcceptedNode(int x, int y, int z, int verticalDeltaLimit, double nodeFloorLevel, Direction direction,
                                        BlockPathTypes pathType) {
            Node node = super.findAcceptedNode(x, y, z, verticalDeltaLimit, nodeFloorLevel, direction, pathType);
            if (node != null || verticalDeltaLimit <= 0 || !(mob instanceof Squeezer squeezer)) return node;
            if (getBlockPathTypeStatic(level, new BlockPos.MutableBlockPos(x, y, z)) != BlockPathTypes.BLOCKED) return null;
            Node up = super.findAcceptedNode(x, y + 1, z, verticalDeltaLimit - 1, nodeFloorLevel, direction, pathType);
            if (up == null || (up.type != BlockPathTypes.OPEN && up.type != BlockPathTypes.WALKABLE)) return null;
            double half = mob.getBbWidth() / 2.0D;
            double ox = x - direction.getStepX() + 0.5D;
            double oz = z - direction.getStepZ() + 0.5D;
            AABB room = new AABB(ox - half, y + 1.001D, oz - half, ox + half, y + 1.0D + squeezer.squeeze().crawlHeight() - 0.002D, oz + half);
            return level.noCollision(mob, room) ? up : null;
        }
    }
}
