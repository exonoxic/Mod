package com.exonoxic.palimpsest.horror;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/** Finding believable places for things to stand. */
public final class Spots {

    /**
     * Picks a ground position at a distance from the player, around a direction relative to where
     * they are looking ({@code yawOffset} 0 = in front, 180 = behind).
     *
     * @param clearance how many air blocks the thing needs above the ground
     */
    @Nullable
    public static BlockPos ground(ServerLevel level, ServerPlayer player, double minDist, double maxDist,
                                  double yawOffset, double spread, int clearance, int attempts, Predicate<BlockPos> extra) {
        RandomSource r = player.getRandom();
        boolean useSurface = level.canSeeSky(player.blockPosition().above()) || player.getY() >= level.getSeaLevel();
        for (int i = 0; i < attempts; i++) {
            double yaw = player.getYRot() + yawOffset + (r.nextDouble() - 0.5D) * 2D * spread;
            double dist = minDist + r.nextDouble() * (maxDist - minDist);
            int x = Mth.floor(player.getX() - Math.sin(Math.toRadians(yaw)) * dist);
            int z = Mth.floor(player.getZ() + Math.cos(Math.toRadians(yaw)) * dist);
            if (!level.hasChunkAt(new BlockPos(x, (int) player.getY(), z))) continue;
            BlockPos found = useSurface ? surfaceAt(level, x, z) : localFloor(level, x, (int) Math.floor(player.getY()), z, 8);
            if (found == null) continue;
            if (!standable(level, found, clearance)) continue;
            if (!extra.test(found)) continue;
            return found;
        }
        return null;
    }

    @Nullable
    public static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) return null;
        BlockPos pos = new BlockPos(x, y, z);
        if (!level.getFluidState(pos.below()).isEmpty()) return null;
        return pos;
    }

    @Nullable
    public static BlockPos localFloor(ServerLevel level, int x, int y, int z, int range) {
        for (int dy = 0; dy <= range; dy++) {
            for (int sign : new int[]{1, -1}) {
                BlockPos p = new BlockPos(x, y + dy * sign, z);
                if (standable(level, p, 2)) return p;
            }
        }
        return null;
    }

    public static boolean standable(ServerLevel level, BlockPos feet, int clearance) {
        BlockPos below = feet.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        for (int i = 0; i < clearance; i++) {
            BlockPos p = feet.above(i);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return false;
            if (!level.getFluidState(p).isEmpty()) return false;
        }
        return true;
    }

    /** Whether a point is inside the player's rough field of view. */
    public static boolean inView(ServerPlayer player, Vec3 target, double cosThreshold) {
        Vec3 to = target.subtract(player.getEyePosition());
        if (to.lengthSqr() < 1.0E-4) return true;
        return to.normalize().dot(player.getLookAngle()) > cosThreshold;
    }

    public static boolean inView(ServerPlayer player, BlockPos pos) {
        return inView(player, Vec3.atCenterOf(pos), 0.45D);
    }

    private Spots() {}
}
