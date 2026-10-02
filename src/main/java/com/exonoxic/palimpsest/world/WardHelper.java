package com.exonoxic.palimpsest.world;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModEffects;
import com.exonoxic.palimpsest.registry.ModPoiTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rubric Wards: red ink does not scrape away, so the inkborn will not go near it.
 * Lookups go through the POI index rather than scanning blocks.
 */
public final class WardHelper {
    public static final int WARD_RADIUS = 16;

    public static boolean isWarded(Level level, BlockPos pos, int radius) {
        if (!(level instanceof ServerLevel server)) return false;
        return server.getPoiManager().findClosest(h -> h.is(ModPoiTypes.RUBRIC_WARD.getKey()), pos, radius, PoiManager.Occupancy.ANY).isPresent();
    }

    public static boolean isWarded(Level level, BlockPos pos) {
        return isWarded(level, pos, WARD_RADIUS);
    }

    /** A player carrying a lit ward in either hand, or wearing red ink on their skin. */
    public static boolean isPlayerWarded(Player player) {
        if (player.hasEffect(ModEffects.WARDED.get())) return true;
        return holds(player.getMainHandItem()) || holds(player.getOffhandItem());
    }

    private static boolean holds(ItemStack stack) {
        return stack.is(ModBlocks.RUBRIC_WARD.get().asItem());
    }

    private WardHelper() {}
}
