package com.exonoxic.palimpsest.world;

import com.exonoxic.palimpsest.config.CommonConfig;
import com.exonoxic.palimpsest.registry.ModBiomeModifiers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Like forge:add_features, but gated by a named config toggle so server owners can switch
 * Overworld generation off without writing a datapack.
 */
public record ConfigurableFeatureModifier(HolderSet<Biome> biomes, HolderSet<PlacedFeature> features,
                                          GenerationStep.Decoration step, String toggle) implements BiomeModifier {

    public static final Codec<ConfigurableFeatureModifier> CODEC = RecordCodecBuilder.create(b -> b.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfigurableFeatureModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(ConfigurableFeatureModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(ConfigurableFeatureModifier::step),
            Codec.STRING.fieldOf("toggle").forGetter(ConfigurableFeatureModifier::toggle)
    ).apply(b, ConfigurableFeatureModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !biomes.contains(biome) || !enabled()) return;
        for (Holder<PlacedFeature> feature : features) {
            builder.getGenerationSettings().addFeature(step, feature);
        }
    }

    private boolean enabled() {
        return switch (toggle) {
            case "ores" -> CommonConfig.GENERATE_ORES.get();
            case "scars" -> CommonConfig.GENERATE_SCARS.get();
            case "plants" -> CommonConfig.GENERATE_PLANTS.get();
            default -> true;
        };
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return ModBiomeModifiers.CONFIGURABLE_FEATURES.get();
    }
}
