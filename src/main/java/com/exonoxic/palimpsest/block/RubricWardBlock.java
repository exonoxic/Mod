package com.exonoxic.palimpsest.block;

import com.exonoxic.palimpsest.registry.ModParticles;
import com.exonoxic.palimpsest.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A lamp glazed in vermilion. Inkborn will not approach within sixteen blocks, Knockers will
 * not come to a warded door, Tears will not open near it, and sleeping beside one lets the
 * page settle. Registered as a point of interest so lookups are cheap.
 */
public class RubricWardBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 2, 12), Block.box(5, 2, 5, 11, 11, 11), Block.box(6, 11, 6, 10, 14, 10));

    public RubricWardBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.RUBRIC_SPARK.get(), pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3,
                    pos.getY() + 0.7, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.015, 0);
        }
        if (random.nextInt(160) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.WARD_HUM.get(), SoundSource.BLOCKS, 0.4F, 1.0F, false);
        }
    }
}
