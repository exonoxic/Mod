package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grass that has been partly scraped away. It never spreads; left alone in daylight it slowly
 * grows its colour back.
 */
public class FadedGrassBlock extends Block {
    public FadedGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (ModDimensions.isUndertext(level)) return;
        if (random.nextInt(40) == 0 && level.canSeeSky(pos.above()) && level.isDay()) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(30) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ModParticles.ASH_FLECK.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
    }
}
