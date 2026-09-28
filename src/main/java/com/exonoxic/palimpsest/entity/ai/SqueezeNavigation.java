package com.exonoxic.palimpsest.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Ground navigation for creatures with a {@link Squeeze}: paths may run through any gap one
 * block high, whatever the creature's standing height, because it will crawl through. While a
 * route is worked out the creature reports its crawling height, so the pathfinder's own checks
 * (such as the headroom it wants before climbing up a step) are made for a crawling body too.
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

    @Override
    @Nullable
    protected Path createPath(Set<BlockPos> targets, int regionOffset, boolean offsetUpward, int accuracy, float followRange) {
        Squeeze squeeze = mob instanceof Squeezer s ? s.squeeze() : null;
        if (squeeze != null) squeeze.planning(true);
        try {
            return super.createPath(targets, regionOffset, offsetUpward, accuracy, followRange);
        } finally {
            if (squeeze != null) squeeze.planning(false);
        }
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
