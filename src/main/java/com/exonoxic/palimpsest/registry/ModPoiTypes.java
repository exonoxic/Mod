package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Points of interest let us ask "is there a ward/gate near here?" through the chunk-indexed
 * PoiManager instead of scanning blocks.
 */
public final class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(ForgeRegistries.POI_TYPES, Palimpsest.MODID);

    public static final RegistryObject<PoiType> RUBRIC_WARD = POI_TYPES.register("rubric_ward",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.RUBRIC_WARD.get().getStateDefinition().getPossibleStates()), 0, 1));
    public static final RegistryObject<PoiType> UNDERTEXT_VEIL = POI_TYPES.register("undertext_veil",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.UNDERTEXT_VEIL.get().getStateDefinition().getPossibleStates()), 0, 1));
    public static final RegistryObject<PoiType> TEAR = POI_TYPES.register("tear",
            () -> new PoiType(ImmutableSet.copyOf(ModBlocks.TEAR.get().getStateDefinition().getPossibleStates()), 0, 1));

    private ModPoiTypes() {}
}
