package com.exonoxic.palimpsest.registry;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    /** Weapons that can wound things made of ink (Longhand, Redacted, the Rasure's guard). */
    public static final TagKey<Item> INKBANE = item("inkbane");
    public static final TagKey<Item> INKS = item("inks");
    public static final TagKey<Item> SURFACES = item("surfaces");

    public static final TagKey<Block> SCRAPABLE = block("scrapable");
    public static final TagKey<Block> UNDERTEXT_SOIL = block("undertext_soil");

    public static final TagKey<EntityType<?>> INKBORN = TagKey.create(Registries.ENTITY_TYPE, Palimpsest.id("inkborn"));

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, Palimpsest.id(name));
    }

    private static TagKey<Block> block(String name) {
        return TagKey.create(Registries.BLOCK, Palimpsest.id(name));
    }

    private ModTags() {}
}
