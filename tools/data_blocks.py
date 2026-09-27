"""Blockstates, block models, block item models and block loot tables."""
from jsonout import *

CUTOUT = "minecraft:cutout"
CUTOUT_MIPPED = "minecraft:cutout_mipped"
TRANSLUCENT = "minecraft:translucent"

BLOCK_LOOT_SELF = []


def model(name, obj):
    asset(f"models/block/{name}.json", obj)


def item_model(name, obj):
    asset(f"models/item/{name}.json", obj)


def state(name, obj):
    asset(f"blockstates/{name}.json", obj)


def simple_state(name, model_name=None):
    state(name, {"variants": {"": {"model": f"{NS}:block/{model_name or name}"}}})


def cube_all(name, tex=None, render_type=None):
    m = {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{tex or name}"}}
    if render_type:
        m["render_type"] = render_type
    model(name, m)
    simple_state(name)
    item_model(name, {"parent": f"{NS}:block/{name}"})


def self_drop(name):
    data(f"loot_tables/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl(name)}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{name}"})


SILK = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}
SHEARS = {"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}


def silk_or(name, other_item, count=None, fortune=False):
    entry = {"type": "minecraft:item", "name": rl(other_item), "functions": []}
    if count:
        entry["functions"].append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
    if fortune:
        entry["functions"].append({"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"})
    entry["functions"].append({"function": "minecraft:explosion_decay"})
    data(f"loot_tables/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": rl(name), "conditions": [SILK]}, entry]}]}],
        "random_sequence": f"{NS}:blocks/{name}"})


def stairs(name, full_tex):
    t = f"{NS}:block/{full_tex}"
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        model(name + suffix, {"parent": f"minecraft:block/{parent}", "textures": {"bottom": t, "top": t, "side": t}})
    variants = {}
    for facing, y in (("east", 0), ("north", 270), ("south", 90), ("west", 180)):
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                m = name + ("" if shape == "straight" else ("_inner" if shape.startswith("inner") else "_outer"))
                rot_y = y
                if shape in ("inner_left", "outer_left"):
                    rot_y = (y + 270) % 360
                if half == "top" and shape in ("inner_right", "outer_right", "inner_left", "outer_left"):
                    rot_y = (rot_y + 90) % 360
                v = {"model": f"{NS}:block/{m}"}
                if half == "top":
                    v["x"] = 180
                if rot_y:
                    v["y"] = rot_y
                if half == "top" or rot_y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    state(name, {"variants": variants})
    item_model(name, {"parent": f"{NS}:block/{name}"})
    self_drop(name)


def slab(name, full_tex, full_model):
    t = f"{NS}:block/{full_tex}"
    model(name, {"parent": "minecraft:block/slab", "textures": {"bottom": t, "top": t, "side": t}})
    model(name + "_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t, "top": t, "side": t}})
    state(name, {"variants": {"type=bottom": {"model": f"{NS}:block/{name}"}, "type=top": {"model": f"{NS}:block/{name}_top"},
                              "type=double": {"model": f"{NS}:block/{full_model}"}}})
    item_model(name, {"parent": f"{NS}:block/{name}"})
    data(f"loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": rl(name), "functions": [
            {"function": "minecraft:set_count", "count": 2, "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": rl(name), "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"}]}]}], "random_sequence": f"{NS}:blocks/{name}"})


def wall(name, tex):
    t = f"{NS}:block/{tex}"
    model(name + "_post", {"parent": "minecraft:block/template_wall_post", "textures": {"wall": t}})
    model(name + "_side", {"parent": "minecraft:block/template_wall_side", "textures": {"wall": t}})
    model(name + "_side_tall", {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": t}})
    model(name + "_inventory", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t}})
    mp = [{"when": {"up": "true"}, "apply": {"model": f"{NS}:block/{name}_post"}}]
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for h, m in (("low", "_side"), ("tall", "_side_tall")):
            a = {"model": f"{NS}:block/{name}{m}", "uvlock": True}
            if y:
                a["y"] = y
            mp.append({"when": {d: h}, "apply": a})
    state(name, {"multipart": mp})
    item_model(name, {"parent": f"{NS}:block/{name}_inventory"})
    self_drop(name)


def fence(name, tex):
    t = f"{NS}:block/{tex}"
    model(name + "_post", {"parent": "minecraft:block/fence_post", "textures": {"texture": t}})
    model(name + "_side", {"parent": "minecraft:block/fence_side", "textures": {"texture": t}})
    model(name + "_inventory", {"parent": "minecraft:block/fence_inventory", "textures": {"texture": t}})
    mp = [{"apply": {"model": f"{NS}:block/{name}_post"}}]
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        a = {"model": f"{NS}:block/{name}_side", "uvlock": True}
        if y:
            a["y"] = y
        mp.append({"when": {d: "true"}, "apply": a})
    state(name, {"multipart": mp})
    item_model(name, {"parent": f"{NS}:block/{name}_inventory"})
    self_drop(name)


def log(name, side, top):
    model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": f"{NS}:block/{top}", "side": f"{NS}:block/{side}"}})
    model(name + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal",
                                 "textures": {"end": f"{NS}:block/{top}", "side": f"{NS}:block/{side}"}})
    state(name, {"variants": {"axis=y": {"model": f"{NS}:block/{name}"},
                              "axis=z": {"model": f"{NS}:block/{name}_horizontal", "x": 90},
                              "axis=x": {"model": f"{NS}:block/{name}_horizontal", "x": 90, "y": 90}}})
    item_model(name, {"parent": f"{NS}:block/{name}"})
    self_drop(name)


def cross(name, tex=None, render=CUTOUT, item_tex=None, loot=True):
    model(name, {"parent": "minecraft:block/cross", "textures": {"cross": f"{NS}:block/{tex or name}"}, "render_type": render})
    simple_state(name)
    item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{item_tex or tex or name}"}})
    if loot:
        self_drop(name)


def door(name):
    base = f"{NS}:block/{name}"
    for part in ("bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open", "top_left", "top_left_open", "top_right", "top_right_open"):
        model(f"{name}_{part}", {"parent": f"minecraft:block/door_{part}", "render_type": CUTOUT,
                                 "textures": {"bottom": f"{base}_bottom", "top": f"{base}_top"}})
    variants = {}
    rot = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, y in rot.items():
        for half in ("lower", "upper"):
            for hinge in ("left", "right"):
                for opened in ("false", "true"):
                    part = ("bottom" if half == "lower" else "top") + "_" + hinge + ("_open" if opened == "true" else "")
                    yy = y
                    if opened == "true":
                        yy = (y + (90 if hinge == "left" else 270)) % 360
                    v = {"model": f"{NS}:block/{name}_{part}"}
                    if yy:
                        v["y"] = yy
                    variants[f"facing={facing},half={half},hinge={hinge},open={opened}"] = v
    state(name, {"variants": variants})
    item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    data(f"loot_tables/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": rl(name), "conditions": [
            {"condition": "minecraft:block_state_property", "block": rl(name), "properties": {"half": "lower"}}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}], "random_sequence": f"{NS}:blocks/{name}"})


def trapdoor(name):
    t = f"{NS}:block/{name}"
    for part in ("bottom", "top", "open"):
        model(f"{name}_{part}", {"parent": f"minecraft:block/template_orientable_trapdoor_{part}", "render_type": CUTOUT, "textures": {"texture": t}})
    variants = {}
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for facing, y in rot.items():
        for half in ("bottom", "top"):
            for opened in ("false", "true"):
                if opened == "true":
                    m = f"{name}_open"
                    v = {"model": f"{NS}:block/{m}"}
                    if half == "top":
                        v["x"] = 180
                        yy = (y + 180) % 360
                    else:
                        yy = y
                else:
                    v = {"model": f"{NS}:block/{name}_{half}"}
                    yy = y
                if yy:
                    v["y"] = yy
                variants[f"facing={facing},half={half},open={opened}"] = v
    state(name, {"variants": variants})
    item_model(name, {"parent": f"{NS}:block/{name}_bottom"})
    self_drop(name)


def box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down"), uv=None):
    f = {}
    for face in faces:
        entry = {"texture": tex}
        if uv:
            entry["uv"] = uv
        f[face] = entry
    return {"from": frm, "to": to, "faces": f}


def horizontal_state(name, model_name=None, extra=""):
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        v = {"model": f"{NS}:block/{model_name or name}"}
        if y:
            v["y"] = y
        variants[f"facing={facing}" + extra] = v
    return variants


def gen():
    for n in ("palimpsest_stone", "deepslate_palimpsest_stone", "scraped_stone", "vellum_bricks", "rubricated_vellum_bricks",
              "scraped_vellum_bricks", "gall_iron_block", "illumine_block", "vellum_soil", "inkstone", "cobbled_inkstone",
              "polished_inkstone", "inkstone_bricks", "blotwood_planks"):
        cube_all(n)
        self_drop(n)
    for n in ("cinnabar_ore", "deepslate_cinnabar_ore"):
        cube_all(n)
        silk_or(n, "cinnabar", (1, 2), fortune=True)
    cube_all("illumine_ore")
    silk_or("illumine_ore", "raw_illumine", fortune=True)
    cube_all("sealed_door")

    # Grass-like blocks
    model("faded_grass_block", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{NS}:block/faded_grass_block_top", "side": f"{NS}:block/faded_grass_block_side", "bottom": "minecraft:block/dirt"}})
    simple_state("faded_grass_block")
    item_model("faded_grass_block", {"parent": f"{NS}:block/faded_grass_block"})
    silk_or("faded_grass_block", "minecraft:dirt")
    model("ruled_vellum", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{NS}:block/ruled_vellum_top", "side": f"{NS}:block/ruled_vellum_side", "bottom": f"{NS}:block/vellum_soil"}})
    simple_state("ruled_vellum")
    item_model("ruled_vellum", {"parent": f"{NS}:block/ruled_vellum"})
    silk_or("ruled_vellum", "vellum_soil")

    stairs("vellum_brick_stairs", "vellum_bricks")
    slab("vellum_brick_slab", "vellum_bricks", "vellum_bricks")
    wall("vellum_brick_wall", "vellum_bricks")
    stairs("inkstone_brick_stairs", "inkstone_bricks")
    slab("inkstone_brick_slab", "inkstone_bricks", "inkstone_bricks")
    wall("inkstone_brick_wall", "inkstone_bricks")
    stairs("blotwood_stairs", "blotwood_planks")
    slab("blotwood_slab", "blotwood_planks", "blotwood_planks")
    fence("blotwood_fence", "blotwood_planks")
    door("blotwood_door")
    trapdoor("blotwood_trapdoor")
    log("blotwood_log", "blotwood_log", "blotwood_log_top")
    log("stripped_blotwood_log", "stripped_blotwood_log", "stripped_blotwood_log_top")

    model("blotwood_leaves", {"parent": "minecraft:block/leaves", "textures": {"all": f"{NS}:block/blotwood_leaves"}, "render_type": CUTOUT_MIPPED})
    simple_state("blotwood_leaves")
    item_model("blotwood_leaves", {"parent": f"{NS}:block/blotwood_leaves"})
    data("loot_tables/blocks/blotwood_leaves.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": rl("blotwood_leaves"), "conditions": [{"condition": "minecraft:any_of", "terms": [SHEARS, SILK]}]},
            {"type": "minecraft:item", "name": rl("blotwood_sapling"), "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.05, 0.0625, 0.083333336, 0.1]}]}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}],
         "conditions": [{"condition": "minecraft:inverted", "term": {"condition": "minecraft:any_of", "terms": [SHEARS, SILK]}},
                        {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}]}],
        "random_sequence": f"{NS}:blocks/blotwood_leaves"})

    cross("candlewort")
    cross("blotwood_sapling")
    cross("erased_grass", loot=False)
    data("loot_tables/blocks/erased_grass.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rl("erased_grass"), "conditions": [SHEARS]}]}]})
    cross("binding_thread", loot=False)
    data("loot_tables/blocks/binding_thread.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": rl("binding_thread"), "conditions": [{"condition": "minecraft:any_of", "terms": [SHEARS, SILK]}]},
        {"type": "minecraft:item", "name": "minecraft:string", "conditions": [{"condition": "minecraft:survives_explosion"}]}]}]}]})

    # Blotberry bush: four ages, berries only when grown.
    variants = {}
    for age in range(4):
        model(f"blotberry_bush_stage{age}", {"parent": "minecraft:block/cross", "render_type": CUTOUT,
                                              "textures": {"cross": f"{NS}:block/blotberry_bush_stage{age}"}})
        variants[f"age={age}"] = {"model": f"{NS}:block/blotberry_bush_stage{age}"}
    state("blotberry_bush", {"variants": variants})
    data("loot_tables/blocks/blotberry_bush.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl("blotberries"), "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 3}}]}],
         "conditions": [{"condition": "minecraft:block_state_property", "block": rl("blotberry_bush"), "properties": {"age": "3"}}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": rl("blotberries"), "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}],
         "conditions": [{"condition": "minecraft:block_state_property", "block": rl("blotberry_bush"), "properties": {"age": "2"}}]}]})

    # Spent torches share vanilla torch geometry.
    model("spent_torch", {"parent": "minecraft:block/template_torch", "textures": {"torch": f"{NS}:block/spent_torch"}, "render_type": CUTOUT})
    model("spent_wall_torch", {"parent": "minecraft:block/template_torch_wall", "textures": {"torch": f"{NS}:block/spent_torch"}, "render_type": CUTOUT})
    simple_state("spent_torch")
    state("spent_wall_torch", {"variants": {
        "facing=east": {"model": f"{NS}:block/spent_wall_torch"},
        "facing=north": {"model": f"{NS}:block/spent_wall_torch", "y": 270},
        "facing=south": {"model": f"{NS}:block/spent_wall_torch", "y": 90},
        "facing=west": {"model": f"{NS}:block/spent_wall_torch", "y": 180}}})
    item_model("spent_torch", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/spent_torch"}})
    self_drop("spent_torch")
    data("loot_tables/blocks/spent_wall_torch.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rl("spent_torch")}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    # Lantern
    model("illumined_lantern", {"parent": "minecraft:block/template_lantern", "textures": {"lantern": f"{NS}:block/illumined_lantern"}, "render_type": CUTOUT})
    model("illumined_lantern_hanging", {"parent": "minecraft:block/template_hanging_lantern", "textures": {"lantern": f"{NS}:block/illumined_lantern"}, "render_type": CUTOUT})
    state("illumined_lantern", {"variants": {"hanging=false": {"model": f"{NS}:block/illumined_lantern"},
                                             "hanging=true": {"model": f"{NS}:block/illumined_lantern_hanging"}}})
    item_model("illumined_lantern", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/illumined_lantern"}})
    self_drop("illumined_lantern")

    # Rubric chalk: a flat mark, four variants.
    variants = {}
    for v in range(4):
        model(f"rubric_chalk_{v}", {"parent": "minecraft:block/block", "render_type": CUTOUT, "ambientocclusion": False,
                                    "textures": {"particle": f"{NS}:block/rubric_chalk_{v}", "mark": f"{NS}:block/rubric_chalk_{v}"},
                                    "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16], "shade": False, "faces": {
                                        "up": {"uv": [0, 0, 16, 16], "texture": "#mark"}, "down": {"uv": [0, 16, 16, 0], "texture": "#mark"}}}]})
        variants[f"variant={v}"] = {"model": f"{NS}:block/rubric_chalk_{v}"}
    state("rubric_chalk", {"variants": variants})

    # Scrawl: writing on a wall.
    variants = {}
    for v in range(4):
        model(f"scrawl_{v}", {"parent": "minecraft:block/block", "render_type": CUTOUT, "ambientocclusion": False,
                              "textures": {"particle": f"{NS}:block/scrawl_{v}", "s": f"{NS}:block/scrawl_{v}"},
                              "elements": [{"from": [0, 0, 15.8], "to": [16, 16, 15.8], "shade": False,
                                            "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#s"}, "south": {"uv": [16, 0, 0, 16], "texture": "#s"}}}]})
        for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            e = {"model": f"{NS}:block/scrawl_{v}"}
            if y:
                e["y"] = y
            variants[f"facing={facing},variant={v}"] = e
    state("scrawl", {"variants": variants})
    item_model("scrawl", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/scrawl_0"}})
    data("loot_tables/blocks/scrawl.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": rl("scrawl"), "conditions": [SILK]}]}]})

    # Rubric Ward
    wt = f"{NS}:block/rubric_ward"
    wtt = f"{NS}:block/rubric_ward_top"
    model("rubric_ward", {"parent": "minecraft:block/block", "render_type": CUTOUT, "textures": {"particle": wt, "side": wt, "top": wtt},
                          "elements": [box([4, 0, 4], [12, 2, 12], "#top"), box([5, 2, 5], [11, 11, 11], "#side"),
                                       box([6, 11, 6], [10, 14, 10], "#top")]})
    simple_state("rubric_ward")
    item_model("rubric_ward", {"parent": f"{NS}:block/rubric_ward"})
    self_drop("rubric_ward")

    # Scriptorium desk
    dt = f"{NS}:block/scriptorium_desk_top"
    ds = f"{NS}:block/scriptorium_desk_side"
    legs = [box([1, 0, 1], [4, 10, 4], "#side"), box([12, 0, 1], [15, 10, 4], "#side"),
            box([1, 0, 12], [4, 10, 15], "#side"), box([12, 0, 12], [15, 10, 15], "#side")]
    top = {"from": [0, 10, 0], "to": [16, 14, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                                                            "north": {"texture": "#side"}, "south": {"texture": "#side"},
                                                            "east": {"texture": "#side"}, "west": {"texture": "#side"}}}
    model("scriptorium_desk", {"parent": "minecraft:block/block", "textures": {"particle": ds, "top": dt, "side": ds}, "elements": legs + [top]})
    state("scriptorium_desk", {"variants": horizontal_state("scriptorium_desk")})
    item_model("scriptorium_desk", {"parent": f"{NS}:block/scriptorium_desk"})
    self_drop("scriptorium_desk")

    # Rubric altar (active and inactive share geometry)
    at = f"{NS}:block/rubric_altar_top"
    aside = f"{NS}:block/rubric_altar_side"
    altar_el = [box([1, 0, 1], [15, 3, 15], "#side"), box([3, 3, 3], [13, 11, 13], "#side"),
                {"from": [0, 11, 0], "to": [16, 14, 16], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                                                                  "north": {"texture": "#side"}, "south": {"texture": "#side"},
                                                                  "east": {"texture": "#side"}, "west": {"texture": "#side"}}}]
    model("rubric_altar", {"parent": "minecraft:block/block", "textures": {"particle": aside, "top": at, "side": aside}, "elements": altar_el})
    state("rubric_altar", {"variants": {"active=false": {"model": f"{NS}:block/rubric_altar"}, "active=true": {"model": f"{NS}:block/rubric_altar"}}})
    item_model("rubric_altar", {"parent": f"{NS}:block/rubric_altar"})
    self_drop("rubric_altar")

    rt = f"{NS}:block/reading_stand"
    model("reading_stand", {"parent": "minecraft:block/block", "textures": {"particle": rt, "all": rt},
                            "elements": [box([3, 0, 3], [13, 2, 13], "#all"), box([6, 2, 6], [10, 11, 10], "#all"),
                                         box([2, 11, 2], [14, 13, 14], "#all")]})
    simple_state("reading_stand")
    item_model("reading_stand", {"parent": f"{NS}:block/reading_stand"})
    self_drop("reading_stand")

    ft = f"{NS}:block/folio_stand_top"
    fs = f"{NS}:block/folio_stand_side"
    model("folio_stand", {"parent": "minecraft:block/block", "textures": {"particle": fs, "top": ft, "side": fs},
                          "elements": [box([3, 0, 3], [13, 2, 13], "#side"), box([6, 2, 6], [10, 12, 10], "#side"),
                                       {"from": [1, 12, 1], "to": [15, 15, 15], "faces": {"up": {"texture": "#top"}, "down": {"texture": "#side"},
                                                                                        "north": {"texture": "#side"}, "south": {"texture": "#side"},
                                                                                        "east": {"texture": "#side"}, "west": {"texture": "#side"}}}]})
    state("folio_stand", {"variants": horizontal_state("folio_stand")})
    item_model("folio_stand", {"parent": f"{NS}:block/folio_stand"})

    for lit in ("false", "true"):
        suffix = "_lit" if lit == "true" else ""
        model("rubric_pillar" + suffix, {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/rubric_pillar{suffix}"}})
    state("rubric_pillar", {"variants": {"lit=false": {"model": f"{NS}:block/rubric_pillar"}, "lit=true": {"model": f"{NS}:block/rubric_pillar_lit"}}})
    item_model("rubric_pillar", {"parent": f"{NS}:block/rubric_pillar_lit"})

    tt = f"{NS}:block/stitched_tome"
    model("stitched_tome", {"parent": "minecraft:block/block", "textures": {"particle": tt, "all": tt}, "elements": [box([2, 0, 2], [14, 10, 14], "#all")]})
    simple_state("stitched_tome")
    item_model("stitched_tome", {"parent": f"{NS}:block/stitched_tome"})

    model("blank", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/blank"}, "render_type": TRANSLUCENT})
    state("blank", {"variants": {"hollow=false": {"model": f"{NS}:block/blank"}, "hollow=true": {"model": f"{NS}:block/blank"}}})

    vt = f"{NS}:block/undertext_veil"
    model("undertext_veil_ns", {"parent": "minecraft:block/block", "render_type": TRANSLUCENT, "textures": {"particle": vt, "veil": vt},
                                "elements": [{"from": [0, 0, 7.5], "to": [16, 16, 8.5], "shade": False, "faces": {
                                    "north": {"texture": "#veil"}, "south": {"texture": "#veil"}}}]})
    model("undertext_veil_ew", {"parent": "minecraft:block/block", "render_type": TRANSLUCENT, "textures": {"particle": vt, "veil": vt},
                                "elements": [{"from": [7.5, 0, 0], "to": [8.5, 16, 16], "shade": False, "faces": {
                                    "east": {"texture": "#veil"}, "west": {"texture": "#veil"}}}]})
    state("undertext_veil", {"variants": {"axis=x": {"model": f"{NS}:block/undertext_veil_ns"}, "axis=z": {"model": f"{NS}:block/undertext_veil_ew"}}})

    tr = f"{NS}:block/tear"
    model("tear", {"parent": "minecraft:block/block", "render_type": TRANSLUCENT, "textures": {"particle": tr, "t": tr},
                   "elements": [{"from": [0.8, 0, 8], "to": [15.2, 28, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": False},
                                 "shade": False, "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#t"}, "south": {"uv": [0, 0, 16, 16], "texture": "#t"}}},
                                {"from": [8, 0, 0.8], "to": [8, 28, 15.2], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": False},
                                 "shade": False, "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#t"}, "east": {"uv": [0, 0, 16, 16], "texture": "#t"}}}]})
    simple_state("tear")

    # Rubric chalk is placed from its own item; its block has no loot.
    data("loot_tables/blocks/rubric_chalk.json", {"type": "minecraft:block", "pools": []})
