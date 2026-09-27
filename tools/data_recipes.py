"""Crafting, cooking, stonecutting, transcription and ritual recipes."""
from jsonout import *


def I(x):
    return {"item": rl(x)} if not x.startswith("#") else {"tag": x[1:] if ":" in x[1:] else f"{NS}:{x[1:]}"}


def shaped(name, pattern, key, result, count=1, group=None):
    obj = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
           "key": {k: I(v) for k, v in key.items()}, "result": {"item": rl(result), "count": count}}
    if group:
        obj["group"] = group
    data(f"recipes/{name}.json", obj)


def shapeless(name, ingredients, result, count=1):
    data(f"recipes/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                  "ingredients": [I(x) for x in ingredients], "result": {"item": rl(result), "count": count}})


def cook(name, ingredient, result, xp=0.1, kinds=("smelting",), time=200):
    for k in kinds:
        t = time if k in ("smelting", "campfire_cooking") else time // 2
        if k == "campfire_cooking":
            t = 600
        data(f"recipes/{name}_from_{k}.json", {"type": f"minecraft:{k}", "category": "misc", "ingredient": I(ingredient),
                                              "result": rl(result), "experience": xp, "cookingtime": t})


def stonecut(name, ingredient, result, count=1):
    data(f"recipes/{name}_stonecutting.json", {"type": "minecraft:stonecutting", "ingredient": I(ingredient), "result": rl(result), "count": count})


def transcription(name, result, surface=None, ink=None, subject=None, time=200, keep_nbt=False, result_nbt=None):
    obj = {"type": f"{NS}:transcription", "result": {"item": rl(result)}, "time": time}
    if surface:
        obj["surface"] = I(surface)
    if ink:
        obj["ink"] = I(ink)
    if subject:
        obj["subject"] = I(subject)
    if keep_nbt:
        obj["keep_nbt"] = True
    if result_nbt:
        obj["result"]["nbt"] = result_nbt
    data(f"recipes/transcription/{name}.json", obj)


def ritual(name, offerings, catalyst, result=None, count=1, result_nbt=None, **kw):
    obj = {"type": f"{NS}:ritual", "offerings": [I(o) for o in offerings], "catalyst": I(catalyst)}
    if result:
        obj["result"] = {"item": rl(result), "count": count}
        if result_nbt:
            obj["result"]["nbt"] = result_nbt
    obj.update(kw)
    data(f"recipes/rites/{name}.json", obj)


def gen():
    # ------------------------------------------------ early investigation
    shapeless("commonplace_book", ["minecraft:book", "oak_gall", "minecraft:feather"], "commonplace_book")
    shaped("iron_rasorium", [" I", "SF"], {"I": "minecraft:iron_ingot", "S": "minecraft:stick", "F": "minecraft:flint"}, "iron_rasorium")
    shapeless("iron_gall_ink", ["oak_gall", "oak_gall", "minecraft:iron_nugget", "minecraft:glass_bottle"], "iron_gall_ink", 2)
    shapeless("iron_gall_ink_from_lampblack", ["lampblack", "lampblack", "minecraft:iron_nugget", "minecraft:glass_bottle"], "iron_gall_ink", 2)
    shapeless("blank_vellum_from_leather", ["minecraft:leather", "minecraft:bone_meal"], "blank_vellum", 2)
    shaped("blank_vellum_from_scraps", ["SS", "SS"], {"S": "vellum_scrap"}, "blank_vellum")
    shapeless("paper_from_vellum", ["blank_vellum"], "minecraft:paper", 3)
    shapeless("vermilion", ["cinnabar"], "vermilion", 2)
    shapeless("red_dye_from_vermilion", ["vermilion"], "minecraft:red_dye")
    shaped("rubric_chalk", ["V", "C", "B"], {"V": "vermilion", "C": "minecraft:clay_ball", "B": "minecraft:bone_meal"}, "rubric_chalk")
    shapeless("sealing_wax", ["minecraft:honeycomb", "vermilion"], "sealing_wax", 2)
    shapeless("candlewort_tea", ["candlewort", "minecraft:sugar", "minecraft:potion"], "candlewort_tea")
    shapeless("yellow_dye_from_candlewort", ["candlewort"], "minecraft:yellow_dye")
    shapeless("black_dye_from_blotberries", ["blotberries"], "minecraft:black_dye")
    shapeless("torch_from_spent", ["spent_torch", "spent_torch", "spent_torch", "spent_torch", "minecraft:coal"], "minecraft:torch", 4)
    shaped("reading_lens", [" G ", "GFG", " S "], {"G": "minecraft:gold_ingot", "F": "undertext_fragment", "S": "minecraft:stick"}, "reading_lens")
    shaped("ink_bomb", [" G ", "IBI"], {"G": "minecraft:gunpowder", "I": "iron_gall_ink", "B": "blot_residue"}, "ink_bomb", 3)
    shaped("ink_bomb_from_lampblack", [" G ", "LBL"], {"G": "minecraft:gunpowder", "L": "lampblack", "B": "minecraft:glass_bottle"}, "ink_bomb", 2)

    # ------------------------------------------------ stations
    shaped("scriptorium_desk", ["QIB", "PPP", "P P"], {"Q": "minecraft:feather", "I": "iron_gall_ink", "B": "minecraft:book",
                                                        "P": "#minecraft:planks"}, "scriptorium_desk")
    shaped("reading_stand", ["SSS", " P ", " P "], {"S": "#minecraft:wooden_slabs", "P": "minecraft:stick"}, "reading_stand", 2)
    shaped("rubric_altar", ["VCV", "BSB", "BBB"], {"V": "vermilion", "C": "minecraft:candle", "B": "minecraft:stone_bricks",
                                                   "S": "scraped_stone"}, "rubric_altar")

    # ------------------------------------------------ building
    shaped("vellum_bricks", ["SS", "SS"], {"S": "scraped_stone"}, "vellum_bricks", 4)
    stonecut("vellum_bricks", "scraped_stone", "vellum_bricks")
    shaped("rubricated_vellum_bricks", ["BBB", "BVB", "BBB"], {"B": "vellum_bricks", "V": "vermilion"}, "rubricated_vellum_bricks", 8)
    shaped("vellum_brick_stairs", ["B  ", "BB ", "BBB"], {"B": "vellum_bricks"}, "vellum_brick_stairs", 4)
    shaped("vellum_brick_slab", ["BBB"], {"B": "vellum_bricks"}, "vellum_brick_slab", 6)
    shaped("vellum_brick_wall", ["BBB", "BBB"], {"B": "vellum_bricks"}, "vellum_brick_wall", 6)
    stonecut("vellum_brick_stairs", "vellum_bricks", "vellum_brick_stairs")
    stonecut("vellum_brick_slab", "vellum_bricks", "vellum_brick_slab", 2)
    stonecut("vellum_brick_wall", "vellum_bricks", "vellum_brick_wall")
    cook("inkstone", "cobbled_inkstone", "inkstone", 0.1)
    shaped("polished_inkstone", ["SS", "SS"], {"S": "inkstone"}, "polished_inkstone", 4)
    shaped("inkstone_bricks", ["SS", "SS"], {"S": "polished_inkstone"}, "inkstone_bricks", 4)
    for n, c in (("polished_inkstone", 1), ("inkstone_bricks", 1), ("inkstone_brick_stairs", 1), ("inkstone_brick_slab", 2), ("inkstone_brick_wall", 1)):
        stonecut(n + "_from_inkstone", "inkstone", n, c)
    shaped("inkstone_brick_stairs", ["B  ", "BB ", "BBB"], {"B": "inkstone_bricks"}, "inkstone_brick_stairs", 4)
    shaped("inkstone_brick_slab", ["BBB"], {"B": "inkstone_bricks"}, "inkstone_brick_slab", 6)
    shaped("inkstone_brick_wall", ["BBB", "BBB"], {"B": "inkstone_bricks"}, "inkstone_brick_wall", 6)
    shapeless("blotwood_planks", ["#palimpsest:blotwood_logs"], "blotwood_planks", 4)
    shaped("blotwood_stairs", ["B  ", "BB ", "BBB"], {"B": "blotwood_planks"}, "blotwood_stairs", 4)
    shaped("blotwood_slab", ["BBB"], {"B": "blotwood_planks"}, "blotwood_slab", 6)
    shaped("blotwood_fence", ["BSB", "BSB"], {"B": "blotwood_planks", "S": "minecraft:stick"}, "blotwood_fence", 3)
    shaped("blotwood_door", ["BB", "BB", "BB"], {"B": "blotwood_planks"}, "blotwood_door", 3)
    shaped("blotwood_trapdoor", ["BBB", "BBB"], {"B": "blotwood_planks"}, "blotwood_trapdoor", 2)
    cook("lampblack", "#palimpsest:blotwood_logs", "lampblack", 0.15)
    shapeless("string_from_thread", ["binding_thread"], "minecraft:string", 2)
    shapeless("bone_meal_from_antler", ["pale_antler"], "minecraft:bone_meal", 4)

    # ------------------------------------------------ metals
    shapeless("gall_steeped_iron", ["minecraft:iron_ingot", "iron_gall_ink"], "gall_steeped_iron")
    cook("gall_iron_ingot", "gall_steeped_iron", "gall_iron_ingot", 0.7, ("blasting",))
    shaped("gall_iron_block", ["III", "III", "III"], {"I": "gall_iron_ingot"}, "gall_iron_block")
    shapeless("gall_iron_ingot_from_block", ["gall_iron_block"], "gall_iron_ingot", 9)
    shaped("gall_iron_sword", ["I", "I", "S"], {"I": "gall_iron_ingot", "S": "minecraft:stick"}, "gall_iron_sword")
    shaped("gall_iron_pickaxe", ["III", " S ", " S "], {"I": "gall_iron_ingot", "S": "minecraft:stick"}, "gall_iron_pickaxe")
    shaped("gall_iron_axe", ["II", "IS", " S"], {"I": "gall_iron_ingot", "S": "minecraft:stick"}, "gall_iron_axe")
    cook("illumine_leaf", "raw_illumine", "illumine_leaf", 1.0, ("smelting", "blasting"))
    cook("illumine_leaf_from_ore", "illumine_ore", "illumine_leaf", 1.0, ("smelting", "blasting"))
    cook("cinnabar_from_ore", "cinnabar_ore", "cinnabar", 0.7, ("smelting", "blasting"))
    shaped("illumine_block", ["III", "III", "III"], {"I": "illumine_leaf"}, "illumine_block")
    shapeless("illumine_leaf_from_block", ["illumine_block"], "illumine_leaf", 9)
    shaped("illumined_lantern", ["NNN", "NLN", "NNN"], {"N": "minecraft:iron_nugget", "L": "illumine_leaf"}, "illumined_lantern")
    shaped("rasure_edge", ["  S", " S ", "R  "], {"S": "rasure_shard", "R": "iron_rasorium"}, "rasure_edge")

    # ------------------------------------------------ food
    cook("cooked_pale_venison", "pale_venison", "cooked_pale_venison", 0.35, ("smelting", "smoking", "campfire_cooking"))
    shapeless("blotberry_tart", ["blotberries", "blotberries", "blotberries", "minecraft:wheat", "minecraft:sugar", "minecraft:egg"], "blotberry_tart")
    shapeless("scribes_ration", ["minecraft:bread", "cooked_pale_venison", "blank_vellum"], "scribes_ration", 2)

    # ------------------------------------------------ transcription (Scriptorium Desk)
    transcription("margin_compass", "margin_compass", surface="blank_vellum", ink="iron_gall_ink", subject="minecraft:compass", time=240)
    transcription("dowsing_quill", "dowsing_quill", surface="minecraft:string", ink="iron_gall_ink", subject="black_quill", time=200)
    for piece, leather in (("hood", "minecraft:leather_helmet"), ("robe", "minecraft:leather_chestplate"),
                           ("leggings", "minecraft:leather_leggings"), ("boots", "minecraft:leather_boots")):
        transcription(f"rubric_{piece}", f"rubric_{piece}", surface="blank_vellum", ink="vermilion", subject=leather, time=300, keep_nbt=True)
    transcription("lower_writing", "music_disc_lower_writing", surface="faded_page", ink="iron_gall_ink", subject="minecraft:music_disc_13", time=600)
    transcription("book_from_vellum", "minecraft:writable_book", surface="blank_vellum", ink="iron_gall_ink", subject="minecraft:feather", time=100)

    # ------------------------------------------------ rites (Rubric Altar)
    ritual("warding", ["vermilion", "vermilion", "minecraft:glowstone_dust", "blank_vellum"], "minecraft:lantern", "rubric_ward",
           time="any", duration=100, bleed=4)
    ritual("the_open_page", ["undertext_fragment", "undertext_fragment", "undertext_fragment", "undertext_fragment",
                             "blank_vellum", "iron_gall_ink", "black_quill", "candlewort"], "minecraft:book", "folio_of_descent",
           time="night", dimension="minecraft:overworld", duration=160, bleed=40, backlash_chance=0.15, backlash="knocker")
    ritual("recall", ["minecraft:ender_pearl", "blank_vellum", "vermilion"], "minecraft:paper", "bookmark", count=2,
           duration=100, bleed=6, backlash_chance=0.05, backlash="bleed")
    ritual("closing", ["minecraft:honeycomb", "vermilion", "vermilion", "candlewort"], "sealing_wax", "seal_of_closing", count=2,
           duration=100, bleed=3)
    ritual("unmaking", ["sealing_wax", "vermilion", "candlewort", "candlewort"], "minecraft:water_bucket", effect="purify",
           consume_catalyst=False, duration=140, bleed=5)
    ritual("quieting", ["candlewort", "candlewort", "candlewort", "sealing_wax", "illumine_leaf"], "candlewort_tea", effect="quiet",
           time="night", duration=200, bleed=0, backlash_chance=1.0, backlash="longhand_watch")
    ritual("illumination", ["illumine_leaf", "illumine_leaf", "minecraft:blaze_powder", "candlewort", "gall_iron_ingot"],
           "minecraft:chain", "illumined_censer", duration=160, bleed=8)
    ritual("binding", ["bookbinder_needle", "binding_thread", "binding_thread", "illumine_leaf", "black_quill", "iron_gall_ink"],
           "margin_compass", "margin_compass", result_nbt={"Attuned": True}, dimension="palimpsest:undertext", duration=200, bleed=15)
    ritual("the_last_reading", ["redaction_strip", "redaction_strip", "bookbinder_needle", "illumine_leaf", "vermilion",
                                "vermilion", "undertext_fragment", "faded_page"], "spine_key", effect="summon_rasure",
           consume_catalyst=False, dimension="palimpsest:undertext", duration=200, bleed=30)
    ritual("invitation", ["faded_page", "faded_page", "faded_page", "candlewort", "minecraft:bread"], "vermilion",
           effect="summon_rubricator", time="night", dimension="minecraft:overworld", duration=160, bleed=10,
           backlash_chance=0.25, backlash="lights_out")
    ritual("the_fair_copy", ["blank_vellum", "blank_vellum", "blank_vellum", "blank_vellum", "minecraft:glass_pane",
                             "minecraft:glass_pane", "illumine_leaf", "illumine_leaf"], "minecraft:enchanted_book",
           effect="copy_catalyst", consume_catalyst=True, moon_phase=4, time="night", duration=240, bleed=35,
           backlash_chance=0.4, backlash="fair_copy")
    ritual("vermilion_dawn", ["cinnabar", "cinnabar", "minecraft:coal"], "minecraft:flint_and_steel", "vermilion", count=6,
           consume_catalyst=False, time="day", duration=80, bleed=1)
