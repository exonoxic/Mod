package com.exonoxic.palimpsest.world;

import com.exonoxic.palimpsest.bleed.BleedManager;
import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;

/** Natural spawns outside the Undertext follow the nearest player's Bleed. */
public final class SpawnRules {
    public static boolean undertextOr(ServerLevelAccessor accessor, BlockPos pos, int minStage) {
        ServerLevel level = accessor.getLevel();
        if (level.dimension() == ModDimensions.UNDERTEXT) return true;
        if (minStage > 6) return false;
        double mult = CommonConfig.OVERWORLD_SPAWN_MULTIPLIER.get();
        if (mult <= 0 || level.random.nextDouble() > mult) return false;
        Player p = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64, false);
        return p != null && BleedManager.stage(p).index >= minStage;
    }

    private SpawnRules() {}
}
