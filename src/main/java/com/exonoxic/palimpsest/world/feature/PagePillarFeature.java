package com.exonoxic.palimpsest.world.feature;

import com.exonoxic.palimpsest.registry.ModBlocks;
import com.exonoxic.palimpsest.registry.ModTags;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Stacks of ruled vellum standing in the Undertext like unread manuscripts. Some have
 * toppled and lie in a line on the ground, pointing somewhere.
 */
public class PagePillarFeature extends Feature<NoneFeatureConfiguration> {
    public PagePillarFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        BlockState ground = level.getBlockState(origin.below());
        if (!ground.is(ModTags.UNDERTEXT_SOIL) && !ground.is(ModBlocks.INKSTONE.get())) return false;
        if (!level.isEmptyBlock(origin)) return false;

        int height = 3 + random.nextInt(7);
        boolean toppled = random.nextInt(4) == 0;
        if (toppled) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos.MutableBlockPos pos = origin.mutable();
            for (int i = 0; i < height; i++) {
                if (!level.isEmptyBlock(pos)) break;
                BlockPos below = pos.below();
                if (level.isEmptyBlock(below)) pos.move(Direction.DOWN);
                level.setBlock(pos, page(random, i, height), 2);
                pos.move(dir);
            }
            return true;
        }
        for (int i = 0; i < height; i++) {
            BlockPos p = origin.above(i);
            if (!level.isEmptyBlock(p)) return i > 0;
            level.setBlock(p, page(random, i, height), 2);
        }
        // Loose pages drift from the top of the tallest stacks.
        if (height >= 7) {
            for (int i = 0; i < 3; i++) {
                BlockPos p = origin.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2);
                if (level.isEmptyBlock(p) && level.getBlockState(p.below()).is(ModTags.UNDERTEXT_SOIL))
                    level.setBlock(p, ModBlocks.ERASED_GRASS.get().defaultBlockState(), 2);
            }
        }
        return true;
    }

    private static BlockState page(RandomSource random, int index, int height) {
        if (index == 0 && random.nextBoolean()) return ModBlocks.VELLUM_BRICKS.get().defaultBlockState();
        if (index == height - 1 && random.nextInt(5) == 0) return ModBlocks.RUBRICATED_VELLUM_BRICKS.get().defaultBlockState();
        return random.nextInt(6) == 0 ? ModBlocks.SCRAPED_VELLUM_BRICKS.get().defaultBlockState() : ModBlocks.RULED_VELLUM.get().defaultBlockState();
    }
}
