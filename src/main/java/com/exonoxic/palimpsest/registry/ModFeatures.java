package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.world.feature.ErasureScarFeature;
import com.exonoxic.palimpsest.world.feature.PagePillarFeature;
import com.exonoxic.palimpsest.world.feature.ScrawlPatchFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Palimpsest.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ERASURE_SCAR = FEATURES.register("erasure_scar",
            () -> new ErasureScarFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SCRAWL_PATCH = FEATURES.register("scrawl_patch",
            () -> new ScrawlPatchFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> PAGE_PILLAR = FEATURES.register("page_pillar",
            () -> new PagePillarFeature(NoneFeatureConfiguration.CODEC));

    private ModFeatures() {}
}
