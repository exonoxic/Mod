"""
Builds every structure template (.nbt) procedurally. Each builder is a small piece of
architecture described in code, so layouts stay editable and variants are cheap.
"""
import json
import math
import os
import random

import nbt
from data_loot import SURVEY_LOG
from nbt import Byte, DoubleList, IntList, Short
from jsonout import RES

OUT = os.path.join(RES, "data/palimpsest/structures")
DATA_VERSION = 3465  # 1.20.1


def rl(n):
    return n if ":" in n else "palimpsest:" + n


class S:
    def __init__(self, sx, sy, sz, seed=0):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.entities = []
        self.r = random.Random(seed)

    def inside(self, x, y, z):
        return 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]

    def b(self, x, y, z, name, nbt_data=None, **props):
        if not self.inside(x, y, z):
            return
        key = (rl(name), tuple(sorted((k, str(v).lower()) for k, v in props.items())))
        self.blocks[(x, y, z)] = (key, nbt_data)

    def get(self, x, y, z):
        v = self.blocks.get((x, y, z))
        return v[0][0] if v else None

    def fill(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.b(x, y, z, name, **props)

    def air(self, x0, y0, z0, x1, y1, z1):
        self.fill(x0, y0, z0, x1, y1, z1, "minecraft:air")

    def walls(self, x0, y0, z0, x1, y1, z1, name, **props):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    if x in (x0, x1) or z in (z0, z1):
                        self.b(x, y, z, name, **props)

    def chest(self, x, y, z, loot, facing="north"):
        self.b(x, y, z, "minecraft:chest", {"id": "minecraft:chest", "LootTable": rl("chests/" + loot)}, facing=facing, type="single", waterlogged="false")

    def barrel(self, x, y, z, loot, facing="up"):
        self.b(x, y, z, "minecraft:barrel", {"id": "minecraft:barrel", "LootTable": rl("chests/" + loot)}, facing=facing, open="false")

    def lectern_book(self, x, y, z, title, author, pages, facing="north"):
        book = {"id": "minecraft:written_book", "Count": Byte(1),
                "tag": {"title": title, "author": author, "resolved": Byte(1), "pages": [json.dumps({"text": p}) for p in pages]}}
        self.b(x, y, z, "minecraft:lectern", {"id": "minecraft:lectern", "Book": book, "Page": 0}, facing=facing, has_book="true", powered="false")

    def stand(self, x, y, z, item=None):
        tag = {"id": "palimpsest:reading_stand"}
        if item:
            tag["Item"] = {"id": rl(item), "Count": Byte(1)}
        self.b(x, y, z, "reading_stand", tag)

    def sign(self, x, y, z, lines, facing="north", wood="oak"):
        msgs = [json.dumps({"text": l}) for l in (lines + ["", "", "", ""])[:4]]
        blank = [json.dumps({"text": ""})] * 4
        tag = {"id": "minecraft:sign", "is_waxed": Byte(1),
               "front_text": {"messages": msgs, "color": "black", "has_glowing_text": Byte(0)},
               "back_text": {"messages": blank, "color": "black", "has_glowing_text": Byte(0)}}
        self.b(x, y, z, f"minecraft:{wood}_wall_sign", tag, facing=facing, waterlogged="false")

    def spawner(self, x, y, z, entity):
        tag = {"id": "minecraft:mob_spawner", "SpawnData": {"entity": {"id": rl(entity)}}, "SpawnPotentials": [],
               "Delay": Short(20), "MinSpawnDelay": Short(200), "MaxSpawnDelay": Short(600), "SpawnCount": Short(2),
               "MaxNearbyEntities": Short(5), "RequiredPlayerRange": Short(16), "SpawnRange": Short(4)}
        self.b(x, y, z, "minecraft:spawner", tag)

    def entity(self, x, y, z, eid, extra=None):
        tag = {"id": rl(eid), "PersistenceRequired": Byte(1)}
        if extra:
            tag.update(extra)
        self.entities.append({"pos": DoubleList([x + 0.5, float(y), z + 0.5]), "blockPos": IntList([x, y, z]), "nbt": tag})

    def bed(self, x, y, z, facing="south", color="white"):
        dx, dz = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}[facing]
        self.b(x, y, z, f"minecraft:{color}_bed", {"id": "minecraft:bed"}, facing=facing, part="foot", occupied="false")
        self.b(x + dx, y, z + dz, f"minecraft:{color}_bed", {"id": "minecraft:bed"}, facing=facing, part="head", occupied="false")

    def door(self, x, y, z, name, facing="north", hinge="left"):
        self.b(x, y, z, name, facing=facing, half="lower", hinge=hinge, open="false", powered="false")
        self.b(x, y + 1, z, name, facing=facing, half="upper", hinge=hinge, open="false", powered="false")

    def save(self, name):
        palette, index = [], {}
        blocks = []
        for (x, y, z), (key, tag) in sorted(self.blocks.items()):
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": key[0]}
                if key[1]:
                    entry["Properties"] = {k: v for k, v in key[1]}
                palette.append(entry)
            b = {"pos": IntList([x, y, z]), "state": index[key]}
            if tag:
                b["nbt"] = tag
            blocks.append(b)
        root = {"DataVersion": DATA_VERSION, "size": IntList(list(self.size)), "palette": palette, "blocks": blocks,
                "entities": self.entities}
        path = os.path.join(OUT, name + ".nbt")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        nbt.write(path, root)


def gable(s, x0, x1, z0, z1, y, stairs, full):
    """A roof ridged along x, overhanging by one block."""
    k = 0
    while z0 + k <= z1 - k:
        for x in range(x0 - 1, x1 + 2):
            if z0 + k == z1 - k:
                s.b(x, y + k, z0 + k, full)
            else:
                s.b(x, y + k, z0 + k, stairs, facing="south", half="bottom", shape="straight", waterlogged="false")
                s.b(x, y + k, z1 - k, stairs, facing="north", half="bottom", shape="straight", waterlogged="false")
        k += 1
    return k


# =================================================================== Overworld
def wray_cabin(v):
    wood = ["spruce", "oak", "dark_oak"][v]
    s = S(11, 15, 11, 100 + v)
    planks, log = f"minecraft:{wood}_planks", f"minecraft:{wood}_log"
    # Cellar (y 0..4)
    s.fill(2, 0, 2, 8, 4, 8, "minecraft:cobblestone")
    s.air(3, 1, 3, 7, 4, 7)
    for (x, y, z) in ((2, 2, 5), (8, 1, 4), (5, 3, 2), (8, 3, 6)):
        s.b(x, y, z, "palimpsest_stone")
    s.b(7, 2, 5, "scrawl", facing="west", variant=1)
    s.b(3, 2, 4, "scrawl", facing="east", variant=3)
    s.b(5, 3, 3, "scrawl", facing="south", variant=0)
    s.chest(3, 1, 3, "wray_cellar", "south")
    s.b(6, 1, 3, "minecraft:candle", candles=3, lit="false", waterlogged="false")
    s.b(4, 1, 6, "rubric_chalk", variant=2)
    for y in range(1, 5):
        s.b(5, y, 7, "minecraft:ladder", facing="north", waterlogged="false")
    # Cabin floor and walls (y 5..9)
    s.fill(1, 5, 1, 9, 5, 9, planks)
    s.b(5, 5, 7, f"minecraft:{wood}_trapdoor", facing="north", half="top", open="false", powered="false", waterlogged="false")
    s.walls(1, 6, 1, 9, 8, 9, planks)
    s.air(2, 6, 2, 8, 9, 8)
    for x, z in ((1, 1), (9, 1), (1, 9), (9, 9)):
        s.fill(x, 5, z, x, 9, z, log, axis="y")
    for x, z in ((3, 1), (7, 1), (3, 9), (7, 9), (1, 5), (9, 5)):
        s.b(x, 7, z, "minecraft:glass_pane", north="false", south="false", east="false", west="false", waterlogged="false")
    s.air(5, 6, 1, 5, 7, 1)
    s.door(5, 6, 1, f"minecraft:{wood}_door", facing="south")
    s.fill(1, 9, 1, 9, 9, 9, planks)
    s.air(2, 9, 2, 8, 9, 8)
    k = gable(s, 1, 9, 0, 10, 10, f"minecraft:{wood}_stairs", planks)
    for dz in range(1, 5):
        for y in range(10, 10 + dz):
            s.b(1, y, dz + 0, planks)
            s.b(9, y, dz + 0, planks)
            s.b(1, y, 10 - dz, planks)
            s.b(9, y, 10 - dz, planks)
    # Furniture
    s.bed(7, 6, 6, "south", ["red", "gray", "brown"][v])
    s.lectern_book(3, 6, 3, "Survey of the Northern Reach", "I. Wray", [
        "Sheet 14. Northern reach.\n\nHill with single oak, NE corner. Surveyed Tuesday.",
        "(The hill has been drawn, then scratched out so hard the paper has torn.)\n\nNot there Thursday.\n\nNot there.",
        "I have started keeping notes in a commonplace book. Book, oak gall, feather. Everything I notice.\n\nThe notes are in the chest. The rest are below."], "south")
    s.chest(8, 6, 2, "wray_cabin", "west")
    s.b(2, 6, 7, "minecraft:crafting_table")
    s.barrel(2, 6, 6, "wray_cabin")
    s.b(3, 6, 8, "minecraft:candle", candles=2, lit="false", waterlogged="false")
    s.sign(5, 7, 8, ["I DREW IT", "ON TUESDAY"], "north", wood)
    s.b(8, 6, 8, "minecraft:flower_pot")
    s.b(2, 8, 2, "spent_wall_torch", facing="south")
    s.save(f"wray_cabin/wray_cabin_{v}")


def scraped_obelisk(v):
    s = S(5, 9, 5, 200 + v)
    r = s.r
    for x in range(5):
        for z in range(5):
            s.b(x, 0, z, r.choice(["minecraft:cobblestone", "scraped_stone", "minecraft:mossy_cobblestone"]))
    s.fill(1, 1, 1, 3, 1, 3, "palimpsest_stone")
    if v == 0:
        for y in range(2, 8):
            s.b(2, y, 2, "palimpsest_stone" if y == 4 else "scraped_stone")
        s.b(2, 8, 2, "minecraft:stone_brick_slab", type="bottom", waterlogged="false")
        s.b(2, 5, 1, "scrawl", facing="north", variant=2)
        s.chest(2, 0, 2, "scraped_obelisk")
    elif v == 1:
        s.fill(1, 2, 1, 2, 6, 2, "scraped_stone")
        s.b(1, 7, 1, "scraped_stone")
        s.b(2, 3, 0, "scrawl", facing="north", variant=0)
    else:
        for x in range(0, 5):
            s.b(x, 2, 2, "scraped_stone")
        s.b(2, 2, 2, "palimpsest_stone")
        s.b(0, 3, 2, "scraped_stone")
        s.chest(4, 1, 4, "scraped_obelisk", "west")
    s.save(f"scraped_obelisk/scraped_obelisk_{v}")


def hollow_chapel():
    s = S(11, 15, 17, 300)
    s.fill(0, 0, 0, 10, 0, 16, "minecraft:stone_bricks")
    s.walls(0, 1, 0, 10, 7, 16, "minecraft:stone_bricks")
    s.air(1, 1, 1, 9, 9, 15)
    for z in (3, 6, 9, 12):
        for x in (0, 10):
            s.fill(x, 3, z, x, 5, z, "minecraft:glass_pane", north="true", south="true", east="false", west="false", waterlogged="false")
    s.air(4, 1, 0, 6, 3, 0)
    gable_x(s)
    for z in (3, 5, 7, 9):
        for x in (1, 2, 3, 7, 8, 9):
            s.b(x, 1, z, "minecraft:spruce_stairs", facing="north", half="bottom", shape="straight", waterlogged="false")
    s.b(5, 1, 14, "rubric_altar", active="false")
    for (x, z) in ((3, 13), (7, 13), (4, 15), (6, 15)):
        s.stand(x, 1, z)
    s.b(3, 1, 14, "minecraft:candle", candles=4, lit="false", waterlogged="false")
    s.b(7, 1, 14, "minecraft:candle", candles=4, lit="false", waterlogged="false")
    s.lectern_book(2, 1, 12, "The Blank Hymnal", "Unknown", [
        "FOR THE TURNING OF THE PAGE\n\nFOR THOSE WHO WERE WRITTEN BEFORE\n\nFOR THE KNIFE, THAT IT REST",
        "AT THE ALTAR: THE CANDLE, THE RED, THE STONE WITH WORDS UNDER IT.\n\nONE THING TO EACH STAND. THE LAST WORD IN YOUR OWN HAND."], "east")
    s.chest(9, 1, 15, "hollow_chapel", "west")
    s.fill(4, 0, 12, 6, 0, 15, "scraped_stone")
    s.b(5, 6, 16, "rubricated_vellum_bricks")
    s.save("hollow_chapel/hollow_chapel")


def gable_x(s):
    """Stone roof ridged along z for the chapel."""
    for k in range(6):
        for z in range(-1, 18):
            if k < 5:
                s.b(k, 8 + k, z, "minecraft:stone_brick_stairs", facing="east", half="bottom", shape="straight", waterlogged="false")
                s.b(10 - k, 8 + k, z, "minecraft:stone_brick_stairs", facing="west", half="bottom", shape="straight", waterlogged="false")
            else:
                s.b(5, 8 + k, z, "minecraft:stone_bricks")
    for k in range(1, 5):
        for x in range(k, 11 - k):
            s.b(x, 7 + k, 0, "minecraft:stone_bricks")
            s.b(x, 7 + k, 16, "minecraft:stone_bricks")


def copying_house():
    s = S(13, 8, 11, 400)
    s.fill(0, 0, 0, 12, 0, 10, "minecraft:dark_oak_planks")
    s.walls(0, 1, 0, 12, 2, 10, "minecraft:cobblestone")
    s.walls(0, 3, 0, 12, 5, 10, "minecraft:dark_oak_planks")
    for x in (0, 4, 8, 12):
        for z in (0, 10):
            s.fill(x, 1, z, x, 5, z, "minecraft:dark_oak_log", axis="y")
    s.air(1, 1, 1, 11, 6, 9)
    s.air(6, 1, 0, 6, 2, 0)
    s.fill(0, 6, 0, 12, 6, 10, "minecraft:dark_oak_slab", type="bottom", waterlogged="false")
    # The collapsed corner.
    s.air(9, 3, 0, 12, 7, 3)
    s.air(10, 1, 0, 12, 2, 1)
    for (x, y, z) in ((10, 1, 2), (11, 1, 3), (9, 1, 1)):
        s.b(x, y, z, "minecraft:cobblestone")
    s.fill(1, 1, 9, 11, 3, 9, "minecraft:bookshelf")
    s.b(4, 1, 5, "scriptorium_desk", facing="north")
    s.b(8, 1, 5, "scriptorium_desk", facing="north")
    s.chest(1, 1, 1, "copying_house", "south")
    s.chest(11, 1, 8, "copying_house", "west")
    s.b(3, 5, 3, "minecraft:lantern", hanging="true", waterlogged="false")
    s.b(8, 5, 6, "minecraft:lantern", hanging="true", waterlogged="false")
    s.b(1, 2, 5, "scrawl", facing="east", variant=0)
    s.b(11, 2, 6, "scrawl", facing="west", variant=2)
    s.b(6, 4, 9, "scrawl", facing="north", variant=1)
    s.b(4, 2, 5, "minecraft:candle", candles=1, lit="false", waterlogged="false")
    s.save("copying_house/copying_house")


def survey_station():
    s = S(17, 7, 17, 500)
    s.fill(0, 0, 0, 16, 6, 16, "minecraft:stone_bricks")
    s.fill(0, 0, 0, 16, 0, 16, "minecraft:smooth_stone")
    s.fill(0, 6, 0, 16, 6, 16, "minecraft:smooth_stone")
    s.air(7, 1, 0, 9, 3, 16)
    s.air(1, 1, 1, 6, 4, 7)
    s.air(1, 1, 9, 6, 4, 15)
    s.air(10, 1, 1, 15, 4, 7)
    s.air(10, 1, 9, 15, 4, 15)
    for z in (4, 12):
        s.air(6, 1, z, 6, 2, z)
        s.air(10, 1, z, 10, 2, z)
    # Lab
    s.b(2, 1, 2, "minecraft:brewing_stand", has_bottle_0="false", has_bottle_1="false", has_bottle_2="false")
    s.b(3, 1, 2, "minecraft:cauldron")
    s.lectern_book(4, 1, 2, "Station Four Log", "A.T.S.", SURVEY_LOG, "south")
    s.b(1, 1, 5, "minecraft:crafting_table")
    s.b(5, 1, 6, "palimpsest_stone")
    s.b(2, 3, 1, "minecraft:lantern", hanging="false", waterlogged="false")
    # Bunks
    s.bed(2, 1, 10, "south", "white")
    s.bed(5, 1, 10, "south", "white")
    s.bed(2, 1, 13, "south", "white")
    s.b(5, 1, 14, "minecraft:barrel", facing="up", open="false")
    s.b(1, 3, 12, "spent_wall_torch", facing="east")
    # Storage
    s.chest(11, 1, 2, "survey_station", "south")
    s.chest(14, 1, 6, "survey_station", "west")
    s.fill(12, 1, 1, 13, 2, 1, "minecraft:barrel", facing="south", open="false")
    s.b(15, 3, 4, "spent_wall_torch", facing="west")
    # The east room: a Folio Gate frame bricked up from this side.
    s.fill(15, 1, 10, 15, 5, 14, "minecraft:stone_bricks")
    for y in range(0, 5):
        for z in range(10, 14):
            edge = y in (0, 4) or z in (10, 13)
            s.b(15, 1 + y, z, "rubricated_vellum_bricks" if edge else "minecraft:bricks")
    s.fill(10, 1, 12, 10, 2, 12, "minecraft:cobblestone")
    s.sign(14, 2, 12, ["DO NOT", "UNBRICK"], "west", "spruce")
    s.b(12, 1, 11, "rubric_chalk", variant=0)
    s.b(12, 1, 12, "rubric_chalk", variant=1)
    s.b(12, 1, 13, "rubric_chalk", variant=2)
    s.b(14, 3, 10, "scrawl", facing="west", variant=3)
    s.b(8, 3, 8, "minecraft:lantern", hanging="false", waterlogged="false")
    s.save("survey_station/survey_station")


def crow_roost():
    s = S(9, 14, 9, 600)
    log = "minecraft:dark_oak_log"
    s.fill(4, 0, 4, 4, 9, 4, log, axis="y")
    for (x, z) in ((3, 4), (5, 4), (4, 3), (4, 5)):
        s.b(x, 0, z, log, axis="x" if z == 4 else "z")
    s.fill(5, 7, 4, 7, 7, 4, log, axis="x")
    s.fill(1, 9, 4, 3, 9, 4, log, axis="x")
    s.fill(4, 10, 5, 4, 10, 7, log, axis="z")
    s.fill(4, 8, 1, 4, 8, 3, log, axis="z")
    for (x, y, z) in ((7, 8, 4), (1, 10, 4), (4, 11, 7), (4, 9, 1)):
        s.b(x, y, z, "minecraft:black_wool")
    for (x, y, z) in ((6, 6, 4), (2, 8, 4)):
        s.b(x, y, z, "minecraft:dark_oak_fence", north="false", south="false", east="false", west="false", waterlogged="false")
    s.chest(5, 0, 5, "crow_roost", "north")
    s.entity(7, 9, 4, "quillcrow")
    s.entity(4, 12, 7, "quillcrow")
    s.save("crow_roost/crow_roost")


def doubled_house():
    s = S(12, 10, 12, 700)

    def house(ox, oz, rotate):
        s.fill(ox, 0, oz, ox + 6, 0, oz + 6, "minecraft:cobblestone")
        s.walls(ox, 1, oz, ox + 6, 4, oz + 6, "minecraft:oak_planks")
        for (x, z) in ((ox, oz), (ox + 6, oz), (ox, oz + 6), (ox + 6, oz + 6)):
            s.fill(x, 1, z, x, 4, z, "minecraft:oak_log", axis="y")
        s.fill(ox, 5, oz, ox + 6, 5, oz + 6, "minecraft:oak_slab", type="bottom", waterlogged="false")
    house(0, 0, False)
    s.air(1, 1, 1, 5, 4, 5)
    house(4, 4, True)
    s.air(5, 1, 5, 9, 4, 9)
    # The first house's walls run straight through the second.
    s.fill(4, 1, 5, 4, 4, 6, "minecraft:oak_planks")
    s.fill(5, 1, 4, 6, 4, 4, "minecraft:oak_planks")
    # Doors that open onto walls; stairs into the ceiling; a window onto stone.
    s.air(3, 1, 0, 3, 2, 0)
    s.door(3, 1, 0, "minecraft:oak_door", "south")
    s.door(4, 1, 7, "minecraft:oak_door", "east")
    for i in range(4):
        s.b(8 - i, 1 + i, 8, "minecraft:oak_stairs", facing="west", half="bottom", shape="straight", waterlogged="false")
    s.b(10, 3, 7, "minecraft:glass_pane", north="true", south="true", east="false", west="false", waterlogged="false")
    s.b(11, 3, 7, "minecraft:stone")
    s.b(1, 3, 3, "minecraft:glass_pane", north="true", south="true", east="false", west="false", waterlogged="false")
    s.bed(2, 1, 2, "east", "red")
    s.bed(7, 1, 6, "north", "red")
    s.chest(8, 1, 5, "doubled_house", "west")
    s.b(4, 2, 4, "minecraft:chest", {"id": "minecraft:chest", "LootTable": "palimpsest:chests/doubled_house"}, facing="north", type="single", waterlogged="false")
    s.b(2, 3, 5, "minecraft:wall_torch", facing="north")
    s.b(7, 3, 9, "minecraft:wall_torch", facing="north")
    s.sign(5, 2, 1, ["HOME"], "south", "oak")
    s.sign(9, 2, 5, ["HOME"], "east", "oak")
    s.save("doubled_house/doubled_house")


def broken_gate():
    s = S(13, 9, 11, 800)
    s.fill(0, 0, 0, 12, 8, 10, "minecraft:stone")
    for x in range(1, 12):
        for z in range(1, 10):
            for y in range(1, 8):
                d = ((x - 6) / 5.5) ** 2 + ((y - 3) / 4.2) ** 2 + ((z - 5) / 4.6) ** 2
                if d < 1.0:
                    s.air(x, y, z, x, y, z)
    s.fill(2, 0, 2, 10, 0, 8, "minecraft:cobblestone")
    # The frame (opening 2x3) against the back wall, cracked through.
    fx, fz = 5, 8
    frame = [(fx - 1, y) for y in range(1, 5)] + [(fx + 2, y) for y in range(1, 5)] + [(x, 1) for x in range(fx - 1, fx + 3)] + [(x, 5) for x in range(fx - 1, fx + 3)]
    for (x, y) in frame:
        s.b(x, y, fz, "rubricated_vellum_bricks")
    s.b(fx + 2, 3, fz, "scraped_vellum_bricks")
    s.b(fx, 5, fz, "minecraft:air")
    s.b(fx - 1, 4, fz, "scraped_vellum_bricks")
    s.air(fx, 2, fz, fx + 1, 4, fz)
    for (x, z) in ((5, 6), (7, 5), (4, 4)):
        s.b(x, 1, z, "minecraft:cobblestone")
    s.b(8, 1, 6, "minecraft:bone_block", axis="x")
    s.b(3, 1, 6, "minecraft:skeleton_skull", rotation=6)
    s.chest(9, 1, 3, "broken_gate", "west")
    s.b(6, 1, 3, "minecraft:candle", candles=3, lit="false", waterlogged="false")
    for (x, y, z, f, v) in ((1, 3, 5, "east", 0), (11, 3, 4, "west", 1), (6, 4, 1, "south", 3)):
        if s.get(x, y, z) == "minecraft:air":
            continue
        s.b(x + (1 if f == "east" else -1 if f == "west" else 0), y, z + (1 if f == "south" else 0), "scrawl", facing=f, variant=v)
    for (x, y, z) in ((0, 2, 5), (12, 3, 5), (6, 0, 9)):
        s.b(x, y, z, "palimpsest_stone")
    s.save("broken_gate/broken_gate")


# =================================================================== Undertext
def faded_house(s, ox, oz, w, d, r, chest=False, rubricator=False, facing_door="north"):
    s.fill(ox, 0, oz, ox + w - 1, 0, oz + d - 1, "blotwood_planks")
    for x in range(ox, ox + w):
        for z in range(oz, oz + d):
            edge = x in (ox, ox + w - 1) or z in (oz, oz + d - 1)
            if not edge:
                s.air(x, 1, z, x, 4, z)
                continue
            for y in range(1, 4):
                if r.random() < 0.18 + 0.1 * y:
                    continue
                corner = x in (ox, ox + w - 1) and z in (oz, oz + d - 1)
                s.b(x, y, z, "blotwood_log" if corner else "vellum_bricks", **({"axis": "y"} if corner else {}))
    for x in range(ox, ox + w):
        for z in range(oz, oz + d):
            if r.random() < 0.6:
                s.b(x, 4, z, "blotwood_slab", type="bottom", waterlogged="false")
    dx = ox + w // 2
    s.air(dx, 1, oz, dx, 2, oz)
    s.b(dx, 0, oz - 1, "rubricated_vellum_bricks")
    if r.random() < 0.5:
        s.b(dx + 1, 3, oz - 1, "illumined_lantern", hanging="false", waterlogged="false")
    if chest:
        s.chest(ox + 1, 1, oz + d - 2, "faded_village", "north")
    if rubricator:
        s.b(ox + w - 2, 1, oz + d - 2, "scriptorium_desk", facing="north")
        s.stand(ox + 1, 1, oz + 1, "faded_page")
        s.entity(ox + w // 2, 1, oz + d // 2, "rubricator")


def faded_village(v):
    s = S(41, 8, 41, 900 + v)
    r = s.r
    c = 20
    s.fill(c - 2, 0, c - 2, c + 2, 0, c + 2, "inkstone_bricks")
    s.walls(c - 2, 1, c - 2, c + 2, 1, c + 2, "inkstone_brick_wall", up="true", north="none", south="none", east="none", west="none", waterlogged="false")
    s.fill(c - 1, 0, c - 1, c + 1, 0, c + 1, "minecraft:water", level="0")
    s.air(c - 1, 1, c - 1, c + 1, 1, c + 1)
    s.fill(c - 2, 2, c - 2, c - 2, 3, c - 2, "blotwood_fence", north="false", south="false", east="false", west="false", waterlogged="false")
    s.fill(c + 2, 2, c + 2, c + 2, 3, c + 2, "blotwood_fence", north="false", south="false", east="false", west="false", waterlogged="false")
    spots = [(4, 4), (26, 3), (5, 27), (27, 26), (15, 31), (30, 14)]
    r.shuffle(spots)
    for i, (x, z) in enumerate(spots[:5 + v]):
        faded_house(s, x, z, r.randint(6, 8), r.randint(6, 8), r, chest=(i in (1, 3)), rubricator=(i == 0))
        # Path to the well.
        px, pz = x + 3, z - 1
        while abs(px - c) + abs(pz - c) > 3:
            s.b(px, 0, pz, "vellum_soil")
            if abs(px - c) > abs(pz - c):
                px += 1 if px < c else -1
            else:
                pz += 1 if pz < c else -1
    for (x, z) in ((c + 4, c), (c - 4, c + 1), (c, c - 5)):
        s.entity(x, 1, z, "smudge")
    for (x, z) in ((c + 6, c + 6), (c - 7, c - 5)):
        s.b(x, 1, z, "illumined_lantern", hanging="false", waterlogged="false")
    s.save(f"faded_village/faded_village_{v}")


def marginalia_spire():
    s = S(9, 34, 9, 1000)
    s.fill(0, 0, 0, 8, 0, 8, "polished_inkstone")
    for y in range(1, 31):
        mat = "vellum_bricks" if y % 6 == 0 else "inkstone_bricks"
        s.walls(2, y, 2, 6, y, 6, mat)
    s.air(3, 1, 3, 5, 29, 5)
    for y in range(1, 30):
        s.b(5, y, 4, "minecraft:ladder", facing="west", waterlogged="false")
    s.air(4, 1, 2, 4, 2, 2)
    s.spawner(3, 1, 5, "margin_crawler")
    for y in (8, 14, 20):
        s.b(4, y, 2, "minecraft:glass_pane", east="true", west="true", north="false", south="false", waterlogged="false")
    s.fill(3, 26, 3, 5, 26, 5, "inkstone_brick_slab", type="top", waterlogged="false")
    s.air(5, 26, 4, 5, 26, 4)
    s.chest(3, 27, 3, "marginalia_spire", "south")
    s.b(4, 28, 5, "illumined_lantern", hanging="false", waterlogged="false")
    for x in range(2, 7):
        for z in range(2, 7):
            if (x in (2, 6) or z in (2, 6)) and (x + z) % 2 == 0:
                s.b(x, 31, z, "inkstone_bricks")
    r = s.r
    for _ in range(18):
        side = r.choice(["north", "south", "east", "west"])
        y = r.randint(2, 29)
        pos = {"north": (r.randint(2, 6), 1), "south": (r.randint(2, 6), 7), "east": (7, r.randint(2, 6)), "west": (1, r.randint(2, 6))}[side]
        s.b(pos[0], y, pos[1], "scrawl", facing=side, variant=r.randint(0, 3))
    s.save("marginalia_spire/marginalia_spire")


def ink_well():
    s = S(11, 22, 11, 1100)
    c = 5
    for y in range(0, 21):
        for x in range(11):
            for z in range(11):
                d = math.hypot(x - c, z - c)
                if d <= 5.4:
                    if d > 3.6:
                        s.b(x, y, z, "inkstone_bricks" if y % 5 else "polished_inkstone")
                    elif y > 0:
                        s.b(x, y, z, "minecraft:air")
                    else:
                        s.b(x, y, z, "inkstone")
    s.fill(3, 1, 3, 7, 2, 7, "minecraft:water", level="0")
    for (x, z) in ((2, 5), (8, 5), (5, 2), (5, 8)):
        s.b(x, 1, z, "polished_inkstone")
        s.b(x, 2, z, "polished_inkstone")
    s.chest(2, 3, 5, "ink_well", "east")
    s.chest(8, 3, 5, "ink_well", "west")
    for y in range(3, 21):
        s.b(5, y, 2, "minecraft:ladder", facing="south", waterlogged="false")
    r = s.r
    for _ in range(10):
        a = r.uniform(0, math.tau)
        x, z, y = int(round(c + math.cos(a) * 4.4)), int(round(c + math.sin(a) * 4.4)), r.randint(2, 18)
        s.b(x, y, z, "illumine_ore")
    for y in (8, 14):
        s.b(8, y, 5, "illumined_lantern", hanging="false", waterlogged="false")
    for x in range(11):
        for z in range(11):
            d = math.hypot(x - c, z - c)
            if 3.6 < d <= 5.4 and (x + z) % 3 == 0:
                s.b(x, 21, z, "inkstone_brick_wall", up="true", north="none", south="none", east="none", west="none", waterlogged="false")
    s.save("ink_well/ink_well")


def scrap_shrine(v):
    s = S(5, 4, 5, 1200 + v)
    s.fill(0, 0, 0, 4, 0, 4, "polished_inkstone")
    for (x, z) in ((0, 0), (4, 0), (0, 4), (4, 4)):
        s.fill(x, 1, z, x, 2, z, "vellum_bricks")
        s.b(x, 3, z, "minecraft:candle", candles=1 + v, lit="false", waterlogged="false")
    s.stand(2, 1, 2, "faded_page" if v != 1 else None)
    if v == 1:
        s.chest(2, 1, 4, "scrap_shrine", "north")
        s.b(1, 1, 1, "rubric_chalk", variant=0)
        s.b(3, 1, 1, "rubric_chalk", variant=1)
    if v == 2:
        s.barrel(2, 1, 4, "scrap_shrine")
        s.b(1, 1, 3, "erased_grass")
    s.save(f"scrap_shrine/scrap_shrine_{v}")


def bindery():
    s = S(29, 16, 29, 1300)
    r = s.r
    s.fill(0, 0, 0, 28, 0, 28, "blotwood_planks")
    for y in range(1, 13):
        for x in range(29):
            for z in range(29):
                if x in (0, 28) or z in (0, 28):
                    s.b(x, y, z, "minecraft:bookshelf" if (y % 4) and (x + z) % 5 else "blotwood_log", **({} if (y % 4) and (x + z) % 5 else {"axis": "y"}))
    s.air(1, 1, 1, 27, 12, 27)
    s.fill(0, 13, 0, 28, 13, 28, "blotwood_planks")
    for x in range(2, 27, 5):
        for z in range(2, 27, 5):
            if r.random() < 0.3:
                s.air(x, 13, z, x + 1, 13, z + 1)
    s.air(13, 1, 0, 15, 4, 0)
    # Pillars of stitched books
    for (x, z) in ((7, 7), (21, 7), (7, 21), (21, 21)):
        s.fill(x, 1, z, x + 1, 12, z + 1, "minecraft:bookshelf")
        s.b(x, 4, z - 1, "scrawl", facing="north", variant=r.randint(0, 3))
    for _ in range(40):
        x, y, z = r.randint(1, 27), r.randint(1, 11), r.randint(1, 27)
        if s.get(x, y, z) == "minecraft:air":
            s.b(x, y, z, "binding_thread")
    for (x, z) in ((1, 1), (27, 1), (1, 27), (27, 27)):
        s.chest(x, 1, z, "bindery", "north" if z > 14 else "south")
    for (x, z) in ((10, 14), (18, 14), (14, 10), (14, 18)):
        s.b(x, 12, z, "illumined_lantern", hanging="true", waterlogged="false")
    for (x, z) in ((12, 20), (16, 20), (14, 23)):
        s.b(x, 1, z, "stitched_tome")
    s.entity(14, 1, 17, "bookbinder")
    s.save("bindery/bindery")


def last_folio():
    s = S(45, 25, 45, 1400)
    c = 22
    s.fill(0, 0, 0, 44, 0, 44, "inkstone")
    for y in range(1, 25):
        s.walls(0, y, 0, 44, y, 44, "polished_inkstone" if y % 6 else "inkstone_bricks")
    s.fill(0, 24, 0, 44, 24, 44, "inkstone_bricks")
    s.fill(1, 1, 1, 43, 1, 43, "minecraft:water", level="0")
    s.air(1, 2, 1, 43, 7, 43)
    s.fill(2, 8, 2, 42, 8, 42, "inkstone_bricks")
    s.air(1, 9, 1, 43, 23, 43)
    for (x, z, f) in ((1, c, "east"), (43, c, "west"), (c, 1, "south")):
        for y in range(2, 10):
            s.b(x, y, z, "minecraft:ladder", facing=f, waterlogged="false")
    # Dais and the reading ring
    s.fill(c - 3, 8, c - 3, c + 3, 8, c + 3, "polished_inkstone")
    s.b(c, 9, c, "rubric_altar", active="false")
    for (dx, dz) in ((-2, 0), (2, 0), (0, -2), (0, 2), (-2, -2), (2, 2), (-2, 2), (2, -2)):
        s.stand(c + dx, 9, c + dz)
    # Pillars
    for (x, z) in ((10, 10), (34, 10), (10, 34), (34, 34)):
        s.fill(x, 9, z, x, 10, z, "polished_inkstone")
        s.b(x, 11, z, "rubric_pillar", lit="true")
    for (x, z) in ((c, 6), (c, 38), (6, c), (38, c), (12, 22), (32, 22)):
        s.b(x, 23, z, "illumined_lantern", hanging="true", waterlogged="false")
    # The stitched door in the south wall
    s.fill(c - 1, 9, 44, c + 1, 12, 44, "sealed_door")
    s.chest(c + 3, 9, 42, "last_folio", "north")
    r = s.r
    for _ in range(24):
        f = r.choice(["south", "north", "east", "west"])
        y = r.randint(10, 20)
        pos = {"south": (r.randint(2, 42), 1), "north": (r.randint(2, 42), 43), "east": (1, r.randint(2, 42)), "west": (43, r.randint(2, 42))}[f]
        s.b(pos[0], y, pos[1], "scrawl", facing=f, variant=r.randint(0, 3))
    s.save("last_folio/last_folio")


def main():
    for v in range(3):
        wray_cabin(v)
        scraped_obelisk(v)
        scrap_shrine(v)
    for v in range(2):
        faded_village(v)
    hollow_chapel()
    copying_house()
    survey_station()
    crow_roost()
    doubled_house()
    broken_gate()
    marginalia_spire()
    ink_well()
    bindery()
    last_folio()
    print("structures written")


if __name__ == "__main__":
    main()
