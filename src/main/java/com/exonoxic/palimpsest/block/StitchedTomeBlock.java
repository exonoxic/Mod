package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A book sewn shut with red thread. The Bookbinder draws itself back together through these. */
public class StitchedTomeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public StitchedTomeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) {
            level.addParticle(ModParticles.RUBRIC_SPARK.get(), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.05, 0.05, (random.nextDouble() - 0.5) * 0.05);
        }
    }
}
