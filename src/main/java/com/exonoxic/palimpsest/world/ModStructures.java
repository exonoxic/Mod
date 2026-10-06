package com.exonoxic.palimpsest.world;

import com.exonoxic.palimpsest.Palimpsest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.List;

/** Keys for the data-driven structures in data/palimpsest/worldgen/structure. */
public final class ModStructures {
    public static final ResourceKey<Structure> WRAY_CABIN = key("wray_cabin");
    public static final ResourceKey<Structure> SCRAPED_OBELISK = key("scraped_obelisk");
    public static final ResourceKey<Structure> HOLLOW_CHAPEL = key("hollow_chapel");
    public static final ResourceKey<Structure> COPYING_HOUSE = key("copying_house");
    public static final ResourceKey<Structure> SURVEY_STATION = key("survey_station");
    public static final ResourceKey<Structure> CROW_ROOST = key("crow_roost");
    public static final ResourceKey<Structure> DOUBLED_HOUSE = key("doubled_house");
    public static final ResourceKey<Structure> BROKEN_GATE = key("broken_gate");
    public static final ResourceKey<Structure> FADED_VILLAGE = key("faded_village");
    public static final ResourceKey<Structure> MARGINALIA_SPIRE = key("marginalia_spire");
    public static final ResourceKey<Structure> INK_WELL = key("ink_well");
    public static final ResourceKey<Structure> SCRAP_SHRINE = key("scrap_shrine");
    public static final ResourceKey<Structure> BINDERY = key("bindery");
    public static final ResourceKey<Structure> LAST_FOLIO = key("last_folio");

    /** Discovery order doesn't matter; each first visit adds a little Bleed and a codex entry. */
    public static final List<ResourceKey<Structure>> ALL = List.of(WRAY_CABIN, SCRAPED_OBELISK, HOLLOW_CHAPEL, COPYING_HOUSE,
            SURVEY_STATION, CROW_ROOST, DOUBLED_HOUSE, BROKEN_GATE, FADED_VILLAGE, MARGINALIA_SPIRE, INK_WELL, SCRAP_SHRINE,
            BINDERY, LAST_FOLIO);

    public static final TagKey<Structure> COMPASS_OVERWORLD = TagKey.create(Registries.STRUCTURE, Palimpsest.id("margin_compass/overworld"));
    public static final TagKey<Structure> COMPASS_UNDERTEXT = TagKey.create(Registries.STRUCTURE, Palimpsest.id("margin_compass/undertext"));
    public static final TagKey<Structure> COMPASS_ATTUNED = TagKey.create(Registries.STRUCTURE, Palimpsest.id("margin_compass/attuned"));

    private static ResourceKey<Structure> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE, Palimpsest.id(name));
    }

    private ModStructures() {}
}
