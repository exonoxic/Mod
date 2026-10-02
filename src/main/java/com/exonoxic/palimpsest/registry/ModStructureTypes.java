package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import com.exonoxic.palimpsest.world.structure.DryLandStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructureTypes {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Palimpsest.MODID);

    public static final RegistryObject<StructureType<DryLandStructure>> DRY_LAND = STRUCTURE_TYPES.register("dry_land",
            () -> () -> DryLandStructure.CODEC);

    private ModStructureTypes() {}
}
