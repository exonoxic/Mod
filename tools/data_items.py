"""Item models (non-block)."""
from jsonout import *

GENERATED = [
    "vellum_scrap", "blank_vellum", "oak_gall", "iron_gall_ink", "cinnabar", "vermilion", "undertext_fragment", "lampblack",
    "raw_illumine", "illumine_leaf", "gall_steeped_iron", "gall_iron_ingot", "foxed_dust", "black_quill", "pale_antler",
    "blot_residue", "redaction_strip", "rasure_shard", "spine_key", "torn_folio", "faded_page", "last_folio",
    "commonplace_book", "misprint", "reading_lens", "bookmark", "folio_of_descent", "seal_of_closing", "ink_bomb",
    "illumined_censer", "scriveners_quill", "rubric_chalk", "rubric_hood", "rubric_robe", "rubric_leggings", "rubric_boots",
    "circlet_of_the_first_draft", "blotberries", "blotberry_tart", "candlewort_tea", "sealing_wax", "pale_venison",
    "cooked_pale_venison", "scribes_ration", "music_disc_lower_writing",
]
HANDHELD = ["iron_rasorium", "rasure_edge", "gall_iron_sword", "gall_iron_pickaxe", "gall_iron_axe", "bookbinder_needle"]
EGGS = ["foxing_moth", "blotling", "smudge", "margin_crawler", "quillcrow", "pale_stag", "inkhound", "rubricator", "knocker",
        "copyist", "longhand", "redacted", "fair_copy", "erratum"]


def gen():
    for n in GENERATED:
        asset(f"models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{n}"}})
    for n in HANDHELD:
        asset(f"models/item/{n}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/{n}"}})
    for n in EGGS:
        asset(f"models/item/{n}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

    # Margin compass: needle frames selected by the "palimpsest:angle" property (same scheme as vanilla).
    overrides = []
    for i in range(32):
        frame = (i + 16) % 32
        overrides.append({"predicate": {f"{NS}:angle": i / 32.0}, "model": f"{NS}:item/margin_compass_{frame:02d}"})
    asset("models/item/margin_compass.json", {"parent": "minecraft:item/generated",
                                              "textures": {"layer0": f"{NS}:item/margin_compass_16"}, "overrides": overrides})
    for f in range(32):
        asset(f"models/item/margin_compass_{f:02d}.json", {"parent": "minecraft:item/generated",
                                                             "textures": {"layer0": f"{NS}:item/margin_compass_{f:02d}"}})

    asset("models/item/dowsing_quill.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/dowsing_quill_0"},
                                             "overrides": [{"predicate": {f"{NS}:strength": 0.3}, "model": f"{NS}:item/dowsing_quill_1"},
                                                           {"predicate": {f"{NS}:strength": 0.6}, "model": f"{NS}:item/dowsing_quill_2"},
                                                           {"predicate": {f"{NS}:strength": 0.95}, "model": f"{NS}:item/dowsing_quill_3"}]})
    for f in range(1, 4):
        asset(f"models/item/dowsing_quill_{f}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/dowsing_quill_{f}"}})
