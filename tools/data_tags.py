"""Block, item, entity, biome and structure tags."""
from jsonout import *


def tag(kind, name, values, ns=NS, replace=False):
    data(f"tags/{kind}/{name}.json", {"replace": replace, "values": [rl(v) if not v.startswith("#") else v for v in values]}, ns=ns)


PICKAXE = ["palimpsest_stone", "deepslate_palimpsest_stone", "scraped_stone", "cinnabar_ore", "deepslate_cinnabar_ore", "rubric_ward",
           "rubric_altar", "vellum_bricks", "vellum_brick_stairs", "vellum_brick_slab", "vellum_brick_wall", "rubricated_vellum_bricks",
           "scraped_vellum_bricks", "gall_iron_block", "illumine_block", "inkstone", "cobbled_inkstone", "polished_inkstone",
           "inkstone_bricks", "inkstone_brick_stairs", "inkstone_brick_slab", "inkstone_brick_wall", "illumine_ore", "illumined_lantern"]
AXE = ["scriptorium_desk", "reading_stand", "blotwood_log", "stripped_blotwood_log", "blotwood_planks", "blotwood_stairs",
       "blotwood_slab", "blotwood_fence", "blotwood_door", "blotwood_trapdoor", "stitched_tome"]
SHOVEL = ["faded_grass_block", "ruled_vellum", "vellum_soil"]
HOE = ["blotwood_leaves"]

UNDERTEXT_BIOMES = ["scraped_expanse", "blotwood", "the_gutter", "inkwell_sea", "rubric_wastes", "marginalia"]
UNDERTEXT_LAND = ["scraped_expanse", "blotwood", "rubric_wastes", "marginalia"]


def gen():
    tag("blocks", "mineable/pickaxe", PICKAXE, ns="minecraft")
    tag("blocks", "mineable/axe", AXE, ns="minecraft")
    tag("blocks", "mineable/shovel", SHOVEL, ns="minecraft")
    tag("blocks", "mineable/hoe", HOE, ns="minecraft")
    tag("blocks", "needs_iron_tool", ["cinnabar_ore", "deepslate_cinnabar_ore", "gall_iron_block", "illumine_ore", "illumine_block"], ns="minecraft")
    tag("blocks", "needs_stone_tool", ["palimpsest_stone", "deepslate_palimpsest_stone", "rubric_altar"], ns="minecraft")
    tag("blocks", "logs", ["#palimpsest:blotwood_logs"], ns="minecraft")
    tag("blocks", "logs_that_burn", ["#palimpsest:blotwood_logs"], ns="minecraft")
    tag("blocks", "blotwood_logs", ["blotwood_log", "stripped_blotwood_log"])
    tag("blocks", "planks", ["blotwood_planks"], ns="minecraft")
    tag("blocks", "wooden_stairs", ["blotwood_stairs"], ns="minecraft")
    tag("blocks", "wooden_slabs", ["blotwood_slab"], ns="minecraft")
    tag("blocks", "wooden_fences", ["blotwood_fence"], ns="minecraft")
    tag("blocks", "wooden_doors", ["blotwood_door"], ns="minecraft")
    tag("blocks", "wooden_trapdoors", ["blotwood_trapdoor"], ns="minecraft")
    tag("blocks", "leaves", ["blotwood_leaves"], ns="minecraft")
    tag("blocks", "saplings", ["blotwood_sapling"], ns="minecraft")
    tag("blocks", "walls", ["vellum_brick_wall", "inkstone_brick_wall"], ns="minecraft")
    tag("blocks", "stairs", ["vellum_brick_stairs", "inkstone_brick_stairs"], ns="minecraft")
    tag("blocks", "slabs", ["vellum_brick_slab", "inkstone_brick_slab"], ns="minecraft")
    tag("blocks", "dirt", ["ruled_vellum", "vellum_soil", "faded_grass_block"], ns="minecraft")
    tag("blocks", "small_flowers", ["candlewort"], ns="minecraft")
    tag("blocks", "replaceable", ["erased_grass", "scrawl", "rubric_chalk"], ns="minecraft")
    tag("blocks", "gate_frame", ["rubricated_vellum_bricks"])
    tag("blocks", "undertext_soil", ["ruled_vellum", "vellum_soil"])
    tag("blocks", "scrapable", ["palimpsest_stone", "deepslate_palimpsest_stone", "ruled_vellum", "vellum_soil"])
    tag("blocks", "undertext_carver_replaceables", ["inkstone", "cobbled_inkstone", "vellum_soil", "ruled_vellum"])
    tag("blocks", "rottable", ["minecraft:oak_planks", "minecraft:spruce_planks", "minecraft:dark_oak_planks", "minecraft:glass_pane",
                               "minecraft:stone_bricks", "minecraft:cobblestone", "vellum_bricks", "blotwood_planks", "minecraft:bookshelf"])
    tag("blocks", "ores", ["cinnabar_ore", "deepslate_cinnabar_ore", "illumine_ore"], ns="forge")
    tag("blocks", "ores/cinnabar", ["cinnabar_ore", "deepslate_cinnabar_ore"], ns="forge")

    tag("items", "blotwood_logs", ["blotwood_log", "stripped_blotwood_log"])
    tag("items", "logs", ["blotwood_log", "stripped_blotwood_log"], ns="minecraft")
    tag("items", "logs_that_burn", ["blotwood_log", "stripped_blotwood_log"], ns="minecraft")
    tag("items", "planks", ["blotwood_planks"], ns="minecraft")
    tag("items", "wooden_stairs", ["blotwood_stairs"], ns="minecraft")
    tag("items", "wooden_slabs", ["blotwood_slab"], ns="minecraft")
    tag("items", "wooden_fences", ["blotwood_fence"], ns="minecraft")
    tag("items", "wooden_doors", ["blotwood_door"], ns="minecraft")
    tag("items", "wooden_trapdoors", ["blotwood_trapdoor"], ns="minecraft")
    tag("items", "leaves", ["blotwood_leaves"], ns="minecraft")
    tag("items", "saplings", ["blotwood_sapling"], ns="minecraft")
    tag("items", "walls", ["vellum_brick_wall", "inkstone_brick_wall"], ns="minecraft")
    tag("items", "stairs", ["vellum_brick_stairs", "inkstone_brick_stairs"], ns="minecraft")
    tag("items", "slabs", ["vellum_brick_slab", "inkstone_brick_slab"], ns="minecraft")
    tag("items", "small_flowers", ["candlewort"], ns="minecraft")
    tag("items", "music_discs", ["music_disc_lower_writing"], ns="minecraft")
    tag("items", "swords", ["gall_iron_sword", "iron_rasorium", "rasure_edge"], ns="minecraft")
    tag("items", "pickaxes", ["gall_iron_pickaxe"], ns="minecraft")
    tag("items", "axes", ["gall_iron_axe"], ns="minecraft")
    tag("items", "inkbane", ["iron_rasorium", "rasure_edge", "gall_iron_sword", "gall_iron_axe", "gall_iron_pickaxe", "illumined_censer"])
    tag("items", "inks", ["iron_gall_ink", "vermilion", "lampblack"])
    tag("items", "surfaces", ["blank_vellum", "faded_page", "minecraft:paper", "minecraft:string"])
    tag("items", "ingots/gall_iron", ["gall_iron_ingot"], ns="forge")
    tag("items", "ingots", ["gall_iron_ingot"], ns="forge")
    tag("items", "ores", ["cinnabar_ore", "deepslate_cinnabar_ore", "illumine_ore"], ns="forge")
    tag("items", "dusts", ["vermilion", "lampblack", "foxed_dust"], ns="forge")

    tag("entity_types", "inkborn", ["blotling", "margin_crawler", "inkhound", "knocker", "copyist", "longhand", "redacted", "erratum"])

    tag("worldgen/biome", "is_undertext", UNDERTEXT_BIOMES)
    tag("worldgen/biome", "undertext_land", UNDERTEXT_LAND)
    ow_land = ["#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
               "minecraft:snowy_plains", "minecraft:savanna", "minecraft:dark_forest", "minecraft:old_growth_birch_forest",
               "minecraft:birch_forest", "minecraft:windswept_forest", "minecraft:grove"]
    has = {
        "wray_cabin": ["#minecraft:is_forest", "#minecraft:is_taiga", "minecraft:plains", "minecraft:meadow"],
        "scraped_obelisk": ow_land + ["minecraft:desert", "minecraft:badlands"],
        "hollow_chapel": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:snowy_plains", "minecraft:flower_forest"],
        "copying_house": ["minecraft:dark_forest", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:taiga",
                          "minecraft:old_growth_birch_forest"],
        "survey_station": ["#minecraft:is_overworld"],
        "crow_roost": ["minecraft:plains", "minecraft:savanna", "minecraft:forest", "minecraft:birch_forest", "minecraft:windswept_savanna"],
        "doubled_house": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"],
        "broken_gate": ["#minecraft:is_overworld"],
        "faded_village": ["scraped_expanse", "marginalia", "blotwood"],
        "marginalia_spire": ["marginalia", "scraped_expanse"],
        "ink_well": ["blotwood", "scraped_expanse"],
        "scrap_shrine": UNDERTEXT_LAND,
        "bindery": UNDERTEXT_LAND,
        "last_folio": UNDERTEXT_LAND,
    }
    for k, v in has.items():
        tag("worldgen/biome", f"has_structure/{k}", v)

    tag("worldgen/structure", "margin_compass/overworld", ["wray_cabin", "hollow_chapel", "copying_house", "survey_station", "broken_gate"])
    tag("worldgen/structure", "margin_compass/undertext", ["bindery"])
    tag("worldgen/structure", "margin_compass/attuned", ["last_folio"])
