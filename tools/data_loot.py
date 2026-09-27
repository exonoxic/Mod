"""Entity and chest loot tables."""
import json

from jsonout import *


def e(item, weight=1, lo=1, hi=1, nbt=None, looting=False, smelt=False, chance=None):
    entry = {"type": "minecraft:item", "name": rl(item), "weight": weight, "functions": []}
    if lo != 1 or hi != 1:
        entry["functions"].append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if nbt is not None:
        entry["functions"].append({"function": "minecraft:set_nbt", "tag": nbt})
    if looting:
        entry["functions"].append({"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    if smelt:
        entry["functions"].append({"function": "minecraft:furnace_smelt", "conditions": [
            {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"flags": {"is_on_fire": True}}}]})
    if not entry["functions"]:
        del entry["functions"]
    if chance is not None:
        entry["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return entry


def pool(entries, rolls=1, bonus=0, conditions=None):
    p = {"rolls": rolls if isinstance(rolls, (int, float)) else {"type": "minecraft:uniform", "min": rolls[0], "max": rolls[1]},
         "entries": entries}
    if bonus:
        p["bonus_rolls"] = bonus
    if conditions:
        p["conditions"] = conditions
    return p


def entity(name, pools):
    data(f"loot_tables/entities/{name}.json", {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{NS}:entities/{name}"})


def chest(name, pools):
    data(f"loot_tables/chests/{name}.json", {"type": "minecraft:chest", "pools": pools, "random_sequence": f"{NS}:chests/{name}"})


def folio(n, weight=1):
    return e("torn_folio", weight, nbt="{Folio:%d}" % n)


def book(title, author, pages):
    """A vanilla written book: used for the Survey's logs and other found writing."""
    snbt = "{title:%s,author:%s,resolved:1b,pages:[%s]}" % (json.dumps(title), json.dumps(author),
                                                            ",".join(json.dumps(json.dumps({"text": p})) for p in pages))
    return {"type": "minecraft:item", "name": "minecraft:written_book", "functions": [{"function": "minecraft:set_nbt", "tag": snbt}]}


SURVEY_LOG = [
    "ANOMALOUS TERRAIN SURVEY\nStation Four\nDaily log\n\nDay 1. Arrived. Wray's cabin located 2km north. Contents consistent with the rumours. Folios recovered: 3.",
    "Day 6. Livestock count: 14. Should be 13. Recounted: 13. Harlan insists he counted 14. The extra one never grazed.",
    "Day 9. Knocking at the hatch, 0300. Three, then three. Protocol: do not open. Nobody opened. By dawn it had gone.\n\nThere was a streak down the hatch cover. Pale. Long.",
    "Day 14. The tall one again, at the edge of the lamplight. It is only ever where we are not looking. Reyes says in the dark we are not looking anywhere. She has started carrying two lamps.",
    "Day 17. The frame in the east room is complete. Vermilion bricks per Wray, VII-VIII. We do not have a Folio. We are not going to write one.\n\nWe have bricked it up from this side.",
    "Day 18. Something is knocking on the bricks.",
]
STATION_NOTE = [
    "To whoever relieves us:\n\nDo not unbrick the east room.\nDo not count the livestock at night.\nIf you hear footsteps behind you, they are behind you.\n\nWe have gone to find Wray. We took the red chalk.\n\n- A.T.S.",
]
CHAPEL_HYMNAL = [
    "THE BLANK HYMNAL\n\n(Every verse has been scraped away. Only the red headings are left.)\n\nFOR THE TURNING OF THE PAGE\n\nFOR THOSE WHO WERE WRITTEN BEFORE\n\nFOR THE KNIFE, THAT IT REST",
    "FOR THE RED STEP AT EVERY DOOR\n\nFOR THE ONES WHO COUNT THEIR ANIMALS TWICE\n\nAT THE ALTAR: THE CANDLE, THE RED, THE STONE WITH WORDS UNDER IT, READ TOGETHER.",
]


def gen():
    # ---------------------------------------------------------------- entities
    entity("foxing_moth", [pool([e("foxed_dust", lo=0, hi=1, looting=True)])])
    entity("blotling", [pool([e("blot_residue", lo=0, hi=2, looting=True)])])
    entity("smudge", [pool([e("faded_page", chance=0.6)])])
    entity("margin_crawler", [pool([e("vellum_scrap", lo=0, hi=2, looting=True)]), pool([e("minecraft:string", lo=0, hi=1)])])
    entity("quillcrow", [pool([e("black_quill", lo=1, hi=2, looting=True)])])
    entity("pale_stag", [pool([e("pale_venison", lo=1, hi=3, looting=True, smelt=True)]), pool([e("pale_antler", chance=0.35)])])
    entity("inkhound", [pool([e("minecraft:bone", lo=0, hi=2, looting=True)]), pool([e("blot_residue", chance=0.4)])])
    entity("rubricator", [pool([e("vermilion", lo=1, hi=3)]), pool([e("faded_page")])])
    entity("knocker", [pool([e("minecraft:bone", lo=1, hi=3, looting=True)]), pool([e("undertext_fragment", chance=0.3)])])
    entity("copyist", [pool([e("minecraft:leather", lo=1, hi=3, looting=True)]), pool([e("undertext_fragment", chance=0.2)])])
    entity("longhand", [pool([e("lampblack", lo=2, hi=4, looting=True)]), pool([e("undertext_fragment", lo=1, hi=2)])])
    entity("redacted", [pool([e("redaction_strip", lo=0, hi=2, looting=True)]), pool([e("minecraft:paper", lo=1, hi=3)])])
    entity("erratum", [pool([e("misprint")])])
    entity("bookbinder", [pool([e("spine_key")]), pool([e("bookbinder_needle", lo=2, hi=2)]), pool([folio(12)]),
                          pool([e("illumine_leaf", lo=3, hi=6)]), pool([e("binding_thread", lo=2, hi=4)])])
    entity("rasure", [pool([e("rasure_shard", lo=3, hi=4)]), pool([e("last_folio")]), pool([e("illumine_leaf", lo=4, hi=8)])])

    # ---------------------------------------------------------------- chests
    chest("wray_cabin", [
        pool([folio(1, 4), folio(2, 3), folio(3, 3)]),
        pool([e("oak_gall", 10, 2, 5), e("minecraft:paper", 10, 2, 6), e("iron_gall_ink", 6), e("minecraft:compass", 3),
              e("minecraft:map", 4), e("minecraft:bread", 8, 1, 3), e("minecraft:candle", 6, 1, 3), e("minecraft:feather", 6, 1, 3),
              e("commonplace_book", 2), e("iron_rasorium", 2)], rolls=(3, 6))])
    chest("wray_cellar", [
        pool([folio(4, 1), folio(5, 1)]),
        pool([e("palimpsest_stone", 8, 1, 3), e("cinnabar", 6, 1, 3), e("vermilion", 4, 1, 2), e("rubric_chalk", 3),
              e("iron_rasorium", 3), e("minecraft:candle", 6, 1, 2), e("undertext_fragment", 2)], rolls=(2, 5))])
    chest("scraped_obelisk", [pool([e("undertext_fragment", 3), e("cinnabar", 4, 1, 2), e("oak_gall", 4, 1, 3), e("minecraft:flint", 5, 1, 3)], rolls=(1, 3))])
    chest("hollow_chapel", [
        pool([folio(6, 3), folio(5, 1)]),
        pool([book("The Blank Hymnal", "Unknown", CHAPEL_HYMNAL)]),
        pool([e("vermilion", 8, 1, 3), e("minecraft:candle", 10, 2, 5), e("blank_vellum", 8, 1, 4), e("sealing_wax", 5, 1, 2),
              e("rubric_ward", 1), e("rubric_chalk", 4), e("minecraft:gold_nugget", 6, 2, 6)], rolls=(3, 6))])
    chest("copying_house", [
        pool([folio(3, 2), folio(5, 2), folio(6, 1)]),
        pool([e("blank_vellum", 10, 2, 6), e("iron_gall_ink", 8, 1, 3), e("minecraft:feather", 8, 2, 4), e("minecraft:book", 6, 1, 3),
              e("black_quill", 4, 1, 2), e("oak_gall", 6, 2, 5), e("minecraft:ink_sac", 4, 1, 3), e("reading_lens", 1)], rolls=(4, 7))])
    chest("survey_station", [
        pool([book("Station Four Log", "A.T.S.", SURVEY_LOG)]),
        pool([book("A Note", "A.T.S.", STATION_NOTE)], conditions=[{"condition": "minecraft:random_chance", "chance": 0.5}]),
        pool([folio(8, 2), folio(7, 1)]),
        pool([e("minecraft:iron_ingot", 8, 2, 6), e("minecraft:bread", 8, 2, 5), e("minecraft:torch", 8, 4, 12), e("rubric_chalk", 5),
              e("vermilion", 5, 2, 4), e("minecraft:lantern", 4), e("undertext_fragment", 4, 1, 3), e("iron_gall_ink", 4, 1, 2)], rolls=(4, 8))])
    chest("crow_roost", [pool([e("black_quill", 10, 2, 5), e("minecraft:bone", 6, 1, 3), e("faded_page", 2), e("minecraft:gold_nugget", 3, 1, 4)], rolls=(2, 4))])
    chest("doubled_house", [pool([e("misprint", 1), e("faded_page", 6), folio(7, 2), e("minecraft:bread", 6, 1, 2), e("minecraft:glass_pane", 4, 1, 4)], rolls=(2, 4))])
    chest("broken_gate", [
        pool([folio(8, 3), folio(9, 1)]),
        pool([e("rubricated_vellum_bricks", 8, 4, 8), e("undertext_fragment", 8, 2, 3), e("folio_of_descent", 1), e("vermilion", 6, 2, 4),
              e("minecraft:candle", 5, 1, 3), e("blank_vellum", 5, 1, 3)], rolls=(3, 6))])
    chest("faded_village", [pool([e("faded_page", 10, 1, 3), e("blotberries", 8, 2, 6), e("vellum_scrap", 8, 2, 5), e("sealing_wax", 4, 1, 2),
                                  e("bookmark", 2), e("candlewort_tea", 3), e("scribes_ration", 4, 1, 2), folio(9, 1)], rolls=(3, 6))])
    chest("marginalia_spire", [pool([e("raw_illumine", 8, 1, 4), e("illumine_leaf", 5, 1, 2), e("faded_page", 6, 1, 3), e("reading_lens", 1),
                                     e("music_disc_lower_writing", 1), folio(10, 2), e("black_quill", 5, 1, 3)], rolls=(3, 6))])
    chest("ink_well", [pool([e("raw_illumine", 10, 2, 5), e("gall_iron_ingot", 5, 1, 3), e("ink_bomb", 6, 1, 3), e("lampblack", 6, 2, 5),
                             e("illumined_lantern", 2), folio(10, 1)], rolls=(3, 6))])
    chest("scrap_shrine", [pool([e("candlewort", 6, 1, 3), e("sealing_wax", 5, 1, 2), e("faded_page", 6), e("vermilion", 4, 1, 2),
                                 e("bookmark", 1)], rolls=(1, 3))])
    chest("bindery", [pool([folio(11, 3), folio(10, 1)]),
                      pool([e("binding_thread", 8, 2, 5), e("illumine_leaf", 6, 1, 3), e("redaction_strip", 5, 1, 2), e("faded_page", 6, 1, 3),
                            e("seal_of_closing", 3, 1, 2), e("sealing_wax", 4, 1, 3), e("ink_bomb", 4, 1, 3)], rolls=(3, 6))])
    chest("last_folio", [pool([e("vermilion", lo=8, hi=12)]), pool([e("rubric_chalk")]), pool([e("sealing_wax", lo=2, hi=4)]),
                         pool([e("candlewort_tea", lo=1, hi=2)]), pool([e("ink_bomb", lo=2, hi=4)]), pool([e("redaction_strip", lo=2, hi=2)])])
