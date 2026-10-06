package com.exonoxic.palimpsest.world.feature;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

/** Saplings grow into the data-driven palimpsest:blotwood_tree (or, rarely, a dead one). */
public class BlotwoodTreeGrower extends AbstractTreeGrower {
    public static final ResourceKey<ConfiguredFeature<?, ?>> TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Palimpsest.id("blotwood_tree"));
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEAD_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, Palimpsest.id("blotwood_dead_tree"));

    @Override
    protected @Nullable ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        return random.nextInt(8) == 0 ? DEAD_TREE : TREE;
    }
}
