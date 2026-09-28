package com.exonoxic.palimpsest.entity.ai;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/**
 * Ground navigation for creatures with a {@link Squeeze}: paths may run through any gap one
 * block high, whatever the creature's standing height, because it will crawl through.
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
    }
}
