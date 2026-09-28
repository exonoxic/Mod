package com.exonoxic.palimpsest.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * Ground navigation for creatures with a {@link Squeeze}: paths may run through any gap one
 * block high, whatever the creature's standing height, because it will crawl through, and may
 * climb up into such a gap even from under a low ceiling.
 */
public class SqueezeNavigation extends GroundPathNavigation {
    public SqueezeNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new SqueezeNodeEvaluator();
        this.nodeEvaluator.setCanPassDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
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
