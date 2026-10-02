package com.exonoxic.palimpsest.world.feature;

import com.exonoxic.palimpsest.block.ScrawlBlock;
import com.exonoxic.palimpsest.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Handwriting on cave walls in the Undertext. It clusters, as if someone stood there a while. */
public class ScrawlPatchFeature extends Feature<NoneFeatureConfiguration> {
    public ScrawlPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int placed = 0;
        int want = 3 + random.nextInt(5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int attempt = 0; attempt < 48 && placed < want; attempt++) {
            pos.setWithOffset(origin, random.nextInt(9) - 4, random.nextInt(7) - 3, random.nextInt(9) - 4);
            if (!level.isEmptyBlock(pos)) continue;
            for (Direction facing : Direction.Plane.HORIZONTAL.shuffledCopy(random)) {
                BlockState scrawl = ModBlocks.SCRAWL.get().defaultBlockState()
                        .setValue(HorizontalDirectionalBlock.FACING, facing)
                        .setValue(ScrawlBlock.VARIANT, random.nextInt(4));
                if (scrawl.canSurvive(level, pos)) {
                    level.setBlock(pos, scrawl, 2);
                    placed++;
                    break;
                }
            }
        }
        return placed > 0;
    }
}
