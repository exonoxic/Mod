package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stone with older writing just under its surface. Scrape it with a Rasorium to lift the
 * lower text out as an Undertext Fragment; it becomes Scraped Stone.
 */
public class PalimpsestStoneBlock extends Block {
    public PalimpsestStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Very occasionally a glyph surfaces on an exposed face, then sinks back.
        if (random.nextInt(60) != 0) return;
        Direction d = Direction.getRandom(random);
        BlockPos side = pos.relative(d);
        if (level.getBlockState(side).isSolidRender(level, side)) return;
        double x = pos.getX() + 0.5 + d.getStepX() * 0.52 + (d.getStepX() == 0 ? (random.nextDouble() - 0.5) * 0.8 : 0);
        double y = pos.getY() + 0.5 + d.getStepY() * 0.52 + (d.getStepY() == 0 ? (random.nextDouble() - 0.5) * 0.8 : 0);
        double z = pos.getZ() + 0.5 + d.getStepZ() * 0.52 + (d.getStepZ() == 0 ? (random.nextDouble() - 0.5) * 0.8 : 0);
        level.addParticle(ModParticles.GLYPH.get(), x, y, z, 0, 0.005, 0);
    }
}
