package com.exonoxic.palimpsest.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;

public final class ModTiers {
    /** Iron that has drunk iron-gall ink. Holds an edge against things made of ink. */
    public static final Tier GALL_IRON = new ForgeTier(2, 520, 6.5F, 2.5F, 16, BlockTags.NEEDS_IRON_TOOL,
            () -> Ingredient.of(ModItems.GALL_IRON_INGOT.get()));
    /** A shard of the knife that scraped the First Draft. */
    public static final Tier RASURE = new ForgeTier(4, 2400, 9.0F, 4.0F, 20, BlockTags.NEEDS_DIAMOND_TOOL,
            () -> Ingredient.of(ModItems.RASURE_SHARD.get()));

    private ModTiers() {}
}
