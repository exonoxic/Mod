package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Leaves like blots of ink. They drip. */
public class BlotwoodLeavesBlock extends LeavesBlock {
    public BlotwoodLeavesBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(12) != 0) return;
        BlockPos below = pos.below();
        BlockState b = level.getBlockState(below);
        if (b.isFaceSturdy(level, below, Direction.UP)) return;
        level.addParticle(ModParticles.INK_DRIP.get(), pos.getX() + random.nextDouble(), pos.getY() - 0.05, pos.getZ() + random.nextDouble(), 0, 0, 0);
    }
}
