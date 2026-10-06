"""
Worldgen data: the Undertext dimension (type, noise settings, density functions, biomes),
placed/configured features, structures, structure sets, template pools, processors and the
Forge biome modifiers that weave Palimpsest into the Overworld.
"""
from data_tags import UNDERTEXT_BIOMES, UNDERTEXT_LAND
from jsonout import *


def wg(rel, obj):
    data(f"worldgen/{rel}.json", obj)


def st(name, props=None):
    s = {"Name": rl(name)}
    if props:
        s["Properties"] = props
    return s


# ======================================================================= dimension
def dimension():
    data("dimension_type/undertext.json", {
        "ultrawarm": False, "natural": False, "coordinate_scale": 1.0, "has_skylight": True, "has_ceiling": False,
        "ambient_light": 0.08, "fixed_time": 13200, "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
        "monster_spawn_block_light_limit": 0, "piglin_safe": False, "bed_works": True, "respawn_anchor_works": False,
        "has_raids": False, "logical_height": 256, "min_y": 0, "height": 256, "infiniburn": "#minecraft:infiniburn_overworld",
        "effects": f"{NS}:undertext"})

    # Noises
    wg("noise/undertext/continents", {"firstOctave": -9, "amplitudes": [1.0, 1.0, 2.0, 1.0, 0.5]})
    wg("noise/undertext/gutter", {"firstOctave": -8, "amplitudes": [1.0, 0.5]})
    wg("noise/undertext/detail", {"firstOctave": -5, "amplitudes": [1.0, 1.0, 0.5]})
    wg("noise/undertext/islands", {"firstOctave": -6, "amplitudes": [1.0, 0.5, 0.25]})

    # Density functions
    wg("density_function/undertext/continents", {"type": "minecraft:flat_cache", "argument": {
        "type": "minecraft:noise", "noise": f"{NS}:undertext/continents", "xz_scale": 0.5, "y_scale": 0.0}})
    wg("density_function/undertext/gutter", {"type": "minecraft:flat_cache", "argument": {
        "type": "minecraft:noise", "noise": f"{NS}:undertext/gutter", "xz_scale": 1.0, "y_scale": 0.0}})

    def pt(loc, val):
        return {"location": loc, "value": val, "derivative": 0.0}

    # Terraces: long flat "pages" with steep cliffs between them. Offset o puts the surface at y = 64 * (2 + o).
    height = {"type": "minecraft:spline", "spline": {"coordinate": f"{NS}:undertext/continents", "points": [
        pt(-1.2, -1.40), pt(-0.45, -1.22), pt(-0.30, -0.96), pt(-0.12, -0.88), pt(0.15, -0.86), pt(0.22, -0.62),
        pt(0.45, -0.60), pt(0.52, -0.38), pt(1.2, -0.33)]}}
    gradient = {"type": "minecraft:y_clamped_gradient", "from_y": 0, "to_y": 256, "from_value": 2.0, "to_value": -2.0}
    detail = {"type": "minecraft:mul", "argument1": 0.035,
              "argument2": {"type": "minecraft:noise", "noise": f"{NS}:undertext/detail", "xz_scale": 1.5, "y_scale": 1.0}}
    # Gutters: narrow, very deep channels where the gutter noise crosses zero. Below sea level they fill with ink.
    gutter = {"type": "minecraft:spline", "spline": {"coordinate": f"{NS}:undertext/gutter", "points": [
        pt(-0.09, 0.0), pt(-0.045, -2.2), pt(0.0, -2.6), pt(0.045, -2.2), pt(0.09, 0.0)]}}
    terrain = {"type": "minecraft:add", "argument1": gradient, "argument2": {"type": "minecraft:add", "argument1": height,
                                                                                   "argument2": {"type": "minecraft:add", "argument1": detail, "argument2": gutter}}}
    band = {"type": "minecraft:abs", "argument": {"type": "minecraft:y_clamped_gradient", "from_y": 136, "to_y": 176, "from_value": -1.0, "to_value": 1.0}}
    islands = {"type": "minecraft:add", "argument1": {"type": "minecraft:noise", "noise": f"{NS}:undertext/islands", "xz_scale": 1.0, "y_scale": 4.0},
               "argument2": {"type": "minecraft:add", "argument1": -0.62, "argument2": {"type": "minecraft:mul", "argument1": -1.4, "argument2": band}}}
    final = {"type": "minecraft:interpolated", "argument": {"type": "minecraft:max", "argument1": terrain, "argument2": islands}}
    initial = {"type": "minecraft:add", "argument1": gradient, "argument2": height}

    shifted = lambda n: {"type": "minecraft:shifted_noise", "noise": n, "xz_scale": 0.25, "y_scale": 0.0,
                         "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z"}
    router = {
        "barrier": 0.0, "fluid_level_floodedness": 0.0, "fluid_level_spread": 0.0, "lava": 0.0,
        "temperature": shifted("minecraft:temperature"), "vegetation": shifted("minecraft:vegetation"),
        "continents": f"{NS}:undertext/continents", "erosion": shifted("minecraft:erosion"), "depth": 0.0,
        "ridges": f"{NS}:undertext/gutter", "initial_density_without_jaggedness": initial, "final_density": final,
        "vein_toggle": 0.0, "vein_ridged": 0.0, "vein_gap": 0.0,
    }

    def floor_cond(then):
        return {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False,
                                                           "secondary_depth_range": 0, "surface_type": "floor"}, "then_run": then}

    def under_floor(then):
        return {"type": "minecraft:condition", "if_true": {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True,
                                                           "secondary_depth_range": 0, "surface_type": "floor"}, "then_run": then}

    def block(name):
        return {"type": "minecraft:block", "result_state": st(name)}

    def biome(names, then):
        return {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": [rl(n) for n in names]}, "then_run": then}

    above_water = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
    surface = {"type": "minecraft:sequence", "sequence": [
        {"type": "minecraft:condition", "if_true": {"type": "minecraft:vertical_gradient", "random_name": "minecraft:bedrock_floor",
                                                    "true_at_and_below": {"above_bottom": 0}, "false_at_and_above": {"above_bottom": 5}},
         "then_run": block("minecraft:bedrock")},
        biome(["inkwell_sea"], under_floor(block("vellum_soil"))),
        biome(["the_gutter"], floor_cond(block("inkstone"))),
        biome(["rubric_wastes"], {"type": "minecraft:sequence", "sequence": [
            floor_cond({"type": "minecraft:condition", "if_true": {"type": "minecraft:noise_threshold", "noise": "minecraft:surface",
                                                                   "min_threshold": 0.1, "max_threshold": 10.0},
                        "then_run": block("cobbled_inkstone")}),
            floor_cond(block("scraped_stone")), under_floor(block("scraped_stone"))]}),
        floor_cond({"type": "minecraft:condition", "if_true": above_water, "then_run": block("ruled_vellum")}),
        under_floor(block("vellum_soil")),
    ]}
    wg("noise_settings/undertext", {
        "sea_level": 63, "disable_mob_generation": False, "aquifers_enabled": False, "ore_veins_enabled": False,
        "legacy_random_source": False, "default_block": st("inkstone"),
        "default_fluid": {"Name": "minecraft:water", "Properties": {"level": "0"}},
        "noise": {"min_y": 0, "height": 256, "size_horizontal": 1, "size_vertical": 2},
        "noise_router": router, "spawn_target": [], "surface_rule": surface})

    # Biome placement
    full = [-1.0, 1.0]
    w_lo, w_hi = [-1.0, -0.05], [0.05, 1.0]
    entries = []

    def add(biome, t=full, h=full, c=full, w=None):
        for wr in (w or [w_lo, w_hi]):
            entries.append({"biome": rl(biome), "parameters": {"temperature": t, "humidity": h, "continentalness": c,
                                                               "erosion": full, "weirdness": wr, "depth": 0.0, "offset": 0.0}})
    sea, low, mid, high = [-1.2, -0.3], [-0.3, 0.2], [0.2, 0.45], [0.45, 1.2]
    dry, wet, cold, warm = [-1.0, 0.1], [0.1, 1.0], [-1.0, 0.2], [0.2, 1.0]
    add("inkwell_sea", c=sea, w=[full])
    add("the_gutter", c=[-0.3, 1.2], w=[[-0.05, 0.05]])
    add("scraped_expanse", h=dry, c=low)
    add("blotwood", h=wet, c=low)
    add("scraped_expanse", t=cold, h=dry, c=mid)
    add("rubric_wastes", t=warm, h=dry, c=mid)
    add("blotwood", h=wet, c=mid)
    add("marginalia", t=cold, c=high)
    add("marginalia", t=warm, h=wet, c=high)
    add("rubric_wastes", t=warm, h=dry, c=high)
    data("dimension/undertext.json", {"type": f"{NS}:undertext", "generator": {
        "type": "minecraft:noise", "settings": f"{NS}:undertext",
        "biome_source": {"type": "minecraft:multi_noise", "biomes": entries}}})


# ======================================================================= biomes
def spawns(**cats):
    out = {k: [] for k in ("monster", "creature", "ambient", "water_creature", "underground_water_creature", "water_ambient", "misc", "axolotls")}
    for cat, lst in cats.items():
        out[cat] = [{"type": rl(e), "weight": w, "minCount": a, "maxCount": b} for (e, w, a, b) in lst]
    return out


def biomes():
    base_effects = {"fog_color": 0x9C9282, "water_color": 0x141019, "water_fog_color": 0x08060B,
                    "ambient_sound": f"{NS}:ambient.undertext.loop",
                    "mood_sound": {"sound": f"{NS}:ambient.undertext.mood", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
                    "additions_sound": {"sound": f"{NS}:ambient.undertext.additions", "tick_chance": 0.008},
                    "music": {"sound": f"{NS}:music.undertext", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}}
    common_monsters = [("blotling", 40, 1, 3), ("margin_crawler", 30, 2, 4), ("inkhound", 25, 2, 3), ("redacted", 6, 1, 1)]
    specs = {
        "scraped_expanse": dict(sky=0xC9BFA6, grass=0xD6CCB0, foliage=0x2A2632, particle=("ash_fleck", 0.0025),
                                monster=common_monsters, creature=[("smudge", 8, 1, 3), ("pale_stag", 6, 2, 4), ("quillcrow", 4, 1, 2)],
                                ambient=[("foxing_moth", 10, 2, 4)],
                                features={9: ["undertext_erased_grass", "undertext_candlewort", "undertext_dead_blotwood", "undertext_page_pillar"],
                                          6: ["undertext_illumine_ore"]}),
        "blotwood": dict(sky=0xA89F8C, grass=0x9C947E, foliage=0x16141C, particle=("ink_drip", 0.004),
                         monster=[("inkhound", 40, 2, 4), ("blotling", 30, 1, 3), ("margin_crawler", 15, 1, 3)],
                         creature=[("pale_stag", 10, 2, 5), ("smudge", 4, 1, 2)], ambient=[("foxing_moth", 14, 2, 5)],
                         features={9: ["undertext_blotwood_trees", "undertext_blotberries", "undertext_erased_grass", "undertext_candlewort"],
                                   6: ["undertext_illumine_ore"]}),
        "the_gutter": dict(sky=0x8E8676, grass=0x8E8676, foliage=0x16141C, particle=("ash_fleck", 0.004),
                           monster=[("margin_crawler", 40, 2, 5), ("blotling", 20, 1, 2)], creature=[], ambient=[("foxing_moth", 6, 1, 3)],
                           features={9: ["undertext_scrawl"], 6: ["undertext_illumine_ore_rich"]}),
        "inkwell_sea": dict(sky=0xB8AE98, grass=0xB8AE98, foliage=0x16141C, particle=("ash_fleck", 0.001),
                            monster=[("blotling", 30, 1, 2)], creature=[], ambient=[],
                            features={9: ["undertext_page_pillar"]}),
        "rubric_wastes": dict(sky=0xD2A898, grass=0xC49A8A, foliage=0x301418, particle=("rubric_spark", 0.0015),
                              monster=[("redacted", 30, 1, 2), ("inkhound", 15, 2, 3), ("margin_crawler", 10, 2, 3)],
                              creature=[("smudge", 3, 1, 1)], ambient=[],
                              features={9: ["undertext_scrawl", "undertext_dead_blotwood"], 6: ["undertext_illumine_ore"]}),
        "marginalia": dict(sky=0xDDD3BA, grass=0xE0D7BF, foliage=0x2A2632, particle=("glyph", 0.0012),
                           monster=[("margin_crawler", 50, 2, 5), ("blotling", 15, 1, 2), ("redacted", 4, 1, 1)],
                           creature=[("quillcrow", 8, 2, 4), ("smudge", 5, 1, 2)], ambient=[("foxing_moth", 8, 2, 4)],
                           features={9: ["undertext_erased_grass", "undertext_scrawl", "undertext_page_pillar"], 6: ["undertext_illumine_ore"]}),
    }
    for name, s in specs.items():
        eff = dict(base_effects)
        eff["sky_color"] = s["sky"]
        eff["grass_color_modifier"] = "none"
        eff["grass_color"] = s["grass"]
        eff["foliage_color"] = s["foliage"]
        eff["particle"] = {"options": {"type": rl(s["particle"][0])}, "probability": s["particle"][1]}
        feats = [[] for _ in range(11)]
        for step, lst in s["features"].items():
            feats[step] = [rl(f) for f in lst]
        wg(f"biome/{name}", {"has_precipitation": False, "temperature": 0.5, "downfall": 0.0, "effects": eff,
                             "spawners": spawns(monster=s["monster"], creature=s["creature"], ambient=s["ambient"]),
                             "spawn_costs": {}, "carvers": {}, "features": feats})


# ======================================================================= features
def features():
    def cf(name, obj):
        wg(f"configured_feature/{name}", obj)

    def pf(name, feature, placement):
        wg(f"placed_feature/{name}", {"feature": rl(feature), "placement": placement})

    count = lambda n: {"type": "minecraft:count", "count": n}
    rarity = lambda n: {"type": "minecraft:rarity_filter", "chance": n}
    square = {"type": "minecraft:in_square"}
    biome_f = {"type": "minecraft:biome"}
    heightmap = lambda h="MOTION_BLOCKING": {"type": "minecraft:heightmap", "heightmap": h}
    uniform_h = lambda lo, hi: {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                                             "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}
    trap_h = lambda lo, hi: {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid",
                                                                          "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}}

    # --- Overworld ores
    cf("palimpsest_stone_ore", {"type": "minecraft:ore", "config": {"size": 7, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": st("palimpsest_stone")},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": st("deepslate_palimpsest_stone")}]}})
    pf("palimpsest_stone_ore", "palimpsest_stone_ore", [count(5), square, trap_h(-48, 56), biome_f])
    cf("cinnabar_ore", {"type": "minecraft:ore", "config": {"size": 5, "discard_chance_on_air_exposure": 0.3, "targets": [
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}, "state": st("cinnabar_ore")},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"}, "state": st("deepslate_cinnabar_ore")}]}})
    pf("cinnabar_ore", "cinnabar_ore", [count(4), square, uniform_h(-64, 8), biome_f])

    # --- Overworld plants and scars
    def patch(name, block, tries=32, xz=6, y=2, props=None, on=None):
        feature = {"type": "minecraft:simple_block", "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": st(block, props)}}}
        placement = [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}]
        if on:
            placement.append({"type": "minecraft:block_predicate_filter", "predicate": {
                "type": "minecraft:matching_block_tag", "offset": [0, -1, 0], "tag": on}})
        cf(name, {"type": "minecraft:random_patch", "config": {"tries": tries, "xz_spread": xz, "y_spread": y,
                                                               "feature": {"feature": feature, "placement": placement}}})
    patch("candlewort_patch", "candlewort", 20, 5, 2, on="minecraft:dirt")
    pf("candlewort_patch", "candlewort_patch", [rarity(12), square, heightmap(), biome_f])
    cf("erasure_scar", {"type": f"{NS}:erasure_scar", "config": {}})
    pf("erasure_scar", "erasure_scar", [rarity(260), square, heightmap("WORLD_SURFACE_WG"), biome_f])

    # --- Undertext
    cf("undertext_illumine_ore", {"type": "minecraft:ore", "config": {"size": 5, "discard_chance_on_air_exposure": 0.0, "targets": [
        {"target": {"predicate_type": "minecraft:block_match", "block": rl("inkstone")}, "state": st("illumine_ore")}]}})
    pf("undertext_illumine_ore", "undertext_illumine_ore", [count(7), square, uniform_h(4, 120), biome_f])
    pf("undertext_illumine_ore_rich", "undertext_illumine_ore", [count(14), square, uniform_h(4, 90), biome_f])
    patch("undertext_erased_grass", "erased_grass", 48, 7, 3, on=f"{NS}:undertext_soil")
    pf("undertext_erased_grass", "undertext_erased_grass", [count(3), square, heightmap(), biome_f])
    pf("undertext_candlewort", "candlewort_patch", [rarity(6), square, heightmap(), biome_f])
    patch("undertext_blotberries", "blotberry_bush", 24, 5, 2, props={"age": "3"}, on=f"{NS}:undertext_soil")
    pf("undertext_blotberries", "undertext_blotberries", [rarity(5), square, heightmap(), biome_f])

    leaves = st("blotwood_leaves", {"distance": "7", "persistent": "false", "waterlogged": "false"})
    log = st("blotwood_log", {"axis": "y"})
    cf("blotwood_tree", {"type": "minecraft:tree", "config": {
        "ignore_vines": True, "force_dirt": False,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": st("vellum_soil")},
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": log},
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": leaves},
        "trunk_placer": {"type": "minecraft:bending_trunk_placer", "base_height": 6, "height_rand_a": 3, "height_rand_b": 1,
                         "min_height_for_leaves": 4, "bend_length": {"type": "minecraft:uniform", "value": {"min_inclusive": 1, "max_inclusive": 2}}},
        "foliage_placer": {"type": "minecraft:random_spread_foliage_placer", "radius": 3, "offset": 0,
                           "foliage_height": 2, "leaf_placement_attempts": 50},
        "decorators": []}})
    cf("blotwood_dead_tree", {"type": "minecraft:tree", "config": {
        "ignore_vines": True, "force_dirt": False,
        "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
        "dirt_provider": {"type": "minecraft:simple_state_provider", "state": st("vellum_soil")},
        "trunk_provider": {"type": "minecraft:simple_state_provider", "state": log},
        "foliage_provider": {"type": "minecraft:simple_state_provider", "state": leaves},
        "trunk_placer": {"type": "minecraft:forking_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 2},
        "foliage_placer": {"type": "minecraft:blob_foliage_placer", "radius": 0, "offset": 0, "height": 1},
        "decorators": []}})
    sapling_filter = {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": st("blotwood_sapling", {"stage": "0"})}}
    pf("undertext_blotwood_trees", "blotwood_tree", [count(6), square, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                                                      heightmap("OCEAN_FLOOR"), biome_f, sapling_filter])
    pf("undertext_dead_blotwood", "blotwood_dead_tree", [rarity(3), square, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
                                                         heightmap("OCEAN_FLOOR"), biome_f, sapling_filter])
    cf("page_pillar", {"type": f"{NS}:page_pillar", "config": {}})
    pf("undertext_page_pillar", "page_pillar", [rarity(4), square, heightmap("WORLD_SURFACE_WG"), biome_f])
    cf("scrawl_patch", {"type": f"{NS}:scrawl_patch", "config": {}})
    pf("undertext_scrawl", "scrawl_patch", [count(4), square, uniform_h(20, 140), biome_f])


# ======================================================================= structures
STRUCTURES = {
    # name: (step, adaptation, start_height, project, spacing, separation, salt, pool size)
    "wray_cabin": ("surface_structures", "beard_thin", {"absolute": -5}, "WORLD_SURFACE_WG", 42, 14, 71117001),
    "scraped_obelisk": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 30, 9, 71117002),
    "hollow_chapel": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 56, 20, 71117003),
    "copying_house": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 58, 20, 71117004),
    "survey_station": ("underground_structures", "none", {"type": "minecraft:uniform", "min_inclusive": {"absolute": -24}, "max_inclusive": {"absolute": 16}}, None, 64, 24, 71117005),
    "crow_roost": ("surface_structures", "beard_thin", {"absolute": 0}, "WORLD_SURFACE_WG", 36, 12, 71117006),
    "doubled_house": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 84, 32, 71117007),
    "broken_gate": ("underground_structures", "none", {"type": "minecraft:uniform", "min_inclusive": {"absolute": -40}, "max_inclusive": {"absolute": 0}}, None, 48, 16, 71117008),
    "faded_village": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 26, 8, 71117009),
    "marginalia_spire": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 22, 7, 71117010),
    "ink_well": ("surface_structures", "beard_thin", {"absolute": -19}, "WORLD_SURFACE_WG", 26, 9, 71117011),
    "scrap_shrine": ("surface_structures", "beard_thin", {"absolute": -1}, "WORLD_SURFACE_WG", 12, 4, 71117012),
    "bindery": ("surface_structures", "beard_box", {"absolute": -1}, "WORLD_SURFACE_WG", 44, 18, 71117013),
    "last_folio": ("surface_structures", "beard_box", {"absolute": -9}, "WORLD_SURFACE_WG", 56, 26, 71117014),
}
# Surface structures that must not start on water (see DryLandStructure).
DRY_LAND = {"wray_cabin", "scraped_obelisk", "hollow_chapel", "copying_house", "crow_roost", "doubled_house",
            "faded_village", "marginalia_spire", "scrap_shrine", "bindery", "last_folio"}
VARIANTS = {"wray_cabin": 3, "scraped_obelisk": 3, "faded_village": 2, "scrap_shrine": 3}
PROCESSORS = {"wray_cabin": "decay_light", "hollow_chapel": "decay_heavy", "copying_house": "decay_heavy", "crow_roost": None,
              "doubled_house": None, "broken_gate": "decay_heavy", "scraped_obelisk": "decay_light", "survey_station": "decay_light",
              "faded_village": "undertext_fade", "marginalia_spire": "undertext_fade", "ink_well": None, "scrap_shrine": None,
              "bindery": None, "last_folio": None}


def structures():
    wg("processor_list/decay_light", {"processors": [
        {"processor_type": "minecraft:rule", "rules": [
            {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:cobblestone", "probability": 0.25},
             "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": {"Name": "minecraft:mossy_cobblestone"}},
            {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:stone_bricks", "probability": 0.2},
             "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": {"Name": "minecraft:cracked_stone_bricks"}}]}]})
    wg("processor_list/decay_heavy", {"processors": [
        {"processor_type": "minecraft:rule", "rules": [
            {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:cobblestone", "probability": 0.4},
             "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": {"Name": "minecraft:mossy_cobblestone"}},
            {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:stone_bricks", "probability": 0.35},
             "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": {"Name": "minecraft:cracked_stone_bricks"}},
            {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:stone_bricks", "probability": 0.15},
             "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": {"Name": "minecraft:mossy_stone_bricks"}}]},
        {"processor_type": "minecraft:block_rot", "integrity": 0.88, "rottable_blocks": f"#{NS}:rottable"}]})
    wg("processor_list/undertext_fade", {"processors": [
        {"processor_type": "minecraft:rule", "rules": [
            {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": rl("vellum_bricks"), "probability": 0.3},
             "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": st("scraped_vellum_bricks")}]},
        {"processor_type": "minecraft:block_rot", "integrity": 0.9, "rottable_blocks": f"#{NS}:rottable"}]})

    for name, (step, adapt, start_h, project, spacing, sep, salt) in STRUCTURES.items():
        obj = {"type": "minecraft:jigsaw", "biomes": f"#{NS}:has_structure/{name}", "step": step, "spawn_overrides": {},
               "terrain_adaptation": adapt, "start_pool": f"{NS}:{name}/start", "size": 1, "start_height": start_h,
               "max_distance_from_center": 80, "use_expansion_hack": False}
        if project:
            obj["project_start_to_heightmap"] = project
        if name in DRY_LAND:
            # The jigsaw itself is registered as <name>_inner (in no structure set); the real
            # structure wraps it and refuses to start on water.
            wg(f"structure/{name}_inner", obj)
            wg(f"structure/{name}", {"type": f"{NS}:dry_land", "biomes": f"#{NS}:has_structure/{name}", "step": step,
                                     "spawn_overrides": {}, "terrain_adaptation": adapt, "structure": f"{NS}:{name}_inner"})
        else:
            wg(f"structure/{name}", obj)
        n = VARIANTS.get(name, 1)
        proc = PROCESSORS.get(name)
        elements = []
        for v in range(n):
            loc = f"{NS}:{name}/{name}_{v}" if n > 1 else f"{NS}:{name}/{name}"
            el = {"element_type": "minecraft:single_pool_element", "location": loc, "projection": "rigid",
                  "processors": f"{NS}:{proc}" if proc else "minecraft:empty"}
            elements.append({"weight": 1, "element": el})
        wg(f"template_pool/{name}/start", {"name": f"{NS}:{name}/start", "fallback": "minecraft:empty", "elements": elements})
        wg(f"structure_set/{name}", {"structures": [{"structure": f"{NS}:{name}", "weight": 1}],
                                     "placement": {"type": "minecraft:random_spread", "salt": salt, "spacing": spacing, "separation": sep}})


# ======================================================================= biome modifiers (Overworld integration)
def biome_modifiers():
    def mod(name, obj):
        data(f"forge/biome_modifier/{name}.json", obj)

    mod("ores", {"type": f"{NS}:configurable_features", "biomes": "#minecraft:is_overworld", "toggle": "ores",
                 "features": [f"{NS}:palimpsest_stone_ore", f"{NS}:cinnabar_ore"], "step": "underground_ores"})
    mod("plants", {"type": f"{NS}:configurable_features", "biomes": ["minecraft:dark_forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga",
                                                                      "minecraft:old_growth_spruce_taiga", "minecraft:swamp", "minecraft:birch_forest"],
                   "toggle": "plants", "features": [f"{NS}:candlewort_patch"], "step": "vegetal_decoration"})
    mod("scars", {"type": f"{NS}:configurable_features", "biomes": "#minecraft:is_overworld", "toggle": "scars",
                  "features": [f"{NS}:erasure_scar"], "step": "top_layer_modification"})
    mod("overworld_moths", {"type": "forge:add_spawns", "biomes": ["minecraft:dark_forest", "minecraft:old_growth_spruce_taiga", "minecraft:swamp"],
                            "spawners": [{"type": rl("foxing_moth"), "weight": 6, "minCount": 1, "maxCount": 3}]})
    mod("overworld_copyists", {"type": "forge:add_spawns", "biomes": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna"],
                               "spawners": [{"type": rl("copyist"), "weight": 3, "minCount": 1, "maxCount": 1}]})
    mod("overworld_blotlings", {"type": "forge:add_spawns", "biomes": "#minecraft:is_overworld",
                                "spawners": [{"type": rl("blotling"), "weight": 4, "minCount": 1, "maxCount": 2}]})


def gen():
    dimension()
    biomes()
    features()
    structures()
    biome_modifiers()
