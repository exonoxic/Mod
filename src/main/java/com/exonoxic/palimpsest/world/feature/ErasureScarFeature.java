package com.exonoxic.palimpsest.world.feature;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A place in the Overworld where something was rubbed out: a shallow, oval scrape of bare
 * stone fringed with grass that has lost its colour. Nothing grows in the middle.
 */
public class ErasureScarFeature extends Feature<NoneFeatureConfiguration> {
    public ErasureScarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        float rx = 3.5F + random.nextFloat() * 4.0F;
        float rz = 2.5F + random.nextFloat() * 3.0F;
        double angle = random.nextDouble() * Math.PI;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        int reach = (int) Math.ceil(rx) + 2;
        boolean placed = false;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                double u = (dx * cos + dz * sin) / rx;
                double v = (-dx * sin + dz * cos) / rz;
                double d = u * u + v * v + (random.nextFloat() - 0.5F) * 0.25F;
                if (d > 1.45) continue;
                int x = origin.getX() + dx, z = origin.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, top, z);
                // Clear the vegetation sitting on the surface first.
                BlockState at = level.getBlockState(pos);
                if (!at.isAir() && at.canBeReplaced() && !at.liquid()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    pos.move(0, -1, 0);
                    at = level.getBlockState(pos);
                }
                if (!isScarrable(at)) continue;
                if (d <= 1.0) {
                    level.setBlock(pos, ModBlocks.SCRAPED_STONE.get().defaultBlockState(), 2);
                    if (d < 0.45 && top > level.getMinBuildHeight() + 4) {
                        // The middle is pressed down, as if by a thumb.
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        BlockPos below = pos.below();
                        if (isScarrable(level.getBlockState(below)))
                            level.setBlock(below, ModBlocks.SCRAPED_STONE.get().defaultBlockState(), 2);
                    }
                } else if (at.is(BlockTags.DIRT) || at.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(pos, ModBlocks.FADED_GRASS_BLOCK.get().defaultBlockState(), 2);
                    if (random.nextInt(3) == 0 && level.isEmptyBlock(pos.above()))
                        level.setBlock(pos.above(), ModBlocks.ERASED_GRASS.get().defaultBlockState(), 2);
                }
                placed = true;
            }
        }
        return placed;
    }

    private static boolean isScarrable(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.GRASS_BLOCK);
    }
}
