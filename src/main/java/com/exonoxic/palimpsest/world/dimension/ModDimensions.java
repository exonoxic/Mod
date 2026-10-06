package com.exonoxic.palimpsest.world.dimension;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/** The Undertext is fully data-driven (see data/palimpsest/dimension*); these are its keys. */
public final class ModDimensions {
    public static final ResourceKey<Level> UNDERTEXT = ResourceKey.create(Registries.DIMENSION, Palimpsest.id("undertext"));
    public static final ResourceKey<DimensionType> UNDERTEXT_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, Palimpsest.id("undertext"));
    public static final ResourceLocation UNDERTEXT_EFFECTS = Palimpsest.id("undertext");

    public static boolean isUndertext(Level level) {
        return level.dimension() == UNDERTEXT;
    }

    private ModDimensions() {}
}
