"""
Generates every non-entity texture: blocks, items, particles, GUI, sky, overlays, armour and the
mod logo. Run from anywhere; output goes to src/main/resources.
"""
import json
import math
import os

import tex_blocks
import tex_items
from glyphs import draw, draw_small
from pixel import *

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
TEX = os.path.join(RES, "assets/palimpsest/textures")


def saver(folder):
    def out(name, canvas, mcmeta=None):
        path = os.path.join(TEX, folder, name + ".png")
        canvas.save(path)
        if mcmeta:
            with open(path + ".mcmeta", "w") as fh:
                json.dump(mcmeta, fh, indent=2)
    return out


def particles():
    out = saver("particle")
    r = rng_for("particles")
    for i in range(3):
        c = Canvas(8, 8)
        c.circle(3.5, 4 + i * 0.3, 1.6 + i * 0.3, (18, 16, 24))
        c.set(3, 1 + i, (18, 16, 24))
        out(f"ink_drip_{i}", c)
    for i in range(4):
        c = Canvas(8, 8)
        for _ in range(3 + i):
            c.set(r.randint(2, 5), r.randint(2, 5), jitter((222, 214, 196), 10, r))
        out(f"ash_fleck_{i}", c)
    for i in range(4):
        c = Canvas(8, 8)
        c.circle(3.5, 3.5, 2.5 - i * 0.4, (230, 70, 40), soft=True)
        c.set(3, 3, (255, 200, 150))
        out(f"rubric_spark_{i}", c)
    for i in range(4):
        c = Canvas(8, 8)
        c.rect(3 - i // 2, 3 - i // 2, 5 + i // 2, 5 + i // 2, (246, 244, 236))
        out(f"erasure_mote_{i}", c)
    for i in range(8):
        c = Canvas(8, 8)
        draw(c, i, 1, 1, (70, 62, 110))
        out(f"glyph_{i}", c)
    for i in range(3):
        c = Canvas(8, 8)
        for _ in range(2 + i):
            c.set(r.randint(2, 5), r.randint(2, 5), jitter((170, 130, 90), 10, r))
        out(f"moth_dust_{i}", c)
    pdir = os.path.join(RES, "assets/palimpsest/particles")
    os.makedirs(pdir, exist_ok=True)
    counts = {"ink_drip": 3, "ash_fleck": 4, "rubric_spark": 4, "erasure_mote": 4, "glyph": 8, "moth_dust": 3}
    for name, n in counts.items():
        with open(os.path.join(pdir, name + ".json"), "w") as fh:
            json.dump({"textures": [f"palimpsest:{name}_{i}" for i in range(n)]}, fh, indent=2)


def paper(c, r, x0, y0, x1, y1, color, edge, stains=6):
    n = value_noise(x1 - x0, y1 - y0, 12, r)
    for y in range(y0, y1):
        for x in range(x0, x1):
            v = n[y - y0, x - x0]
            c.set(x, y, jitter(shade(color, 0.92 + 0.12 * v), 4, r))
    for _ in range(stains):
        cx, cy, rad = r.randint(x0, x1), r.randint(y0, y1), r.uniform(3, 9)
        for y in range(int(cy - rad), int(cy + rad)):
            for x in range(int(cx - rad), int(cx + rad)):
                if x0 <= x < x1 and y0 <= y < y1 and math.hypot(x - cx, y - cy) < rad:
                    c.blend(x, y, (170, 130, 90), 0.08)
    for x in range(x0, x1):
        c.set(x, y0, edge)
        c.set(x, y1 - 1, edge)
    for y in range(y0, y1):
        c.set(x0, y, edge)
        c.set(x1 - 1, y, edge)


def gui():
    out = saver("gui")
    r = rng_for("gui")
    # Codex: an open book, 256x180, plus two ribbon tabs underneath.
    c = Canvas(256, 256)
    c.rect(0, 0, 256, 180, LEATHER_DARK)
    paper(c, r, 6, 6, 128, 174, VELLUM_LIGHT, VELLUM_DARK)
    paper(c, r, 128, 6, 250, 174, VELLUM_LIGHT, VELLUM_DARK)
    for y in range(6, 174):
        c.blend(127, y, (90, 70, 50), 0.5)
        c.blend(128, y, (90, 70, 50), 0.35)
        c.blend(126, y, (90, 70, 50), 0.2)
    for x in (14, 134):
        for y in range(22, 170):
            c.blend(x, y, (190, 110, 100), 0.25)
    c.rect(0, 184, 24, 208, RUBRIC)
    c.rect(1, 185, 23, 207, shade(RUBRIC, 0.85))
    c.rect(24, 184, 48, 208, RUBRIC_BRIGHT)
    c.rect(25, 185, 47, 207, (210, 70, 50))
    out("codex", c)

    for name, color, ink in (("lore_sheet", VELLUM_LIGHT, VELLUM_DARK), ("faded_sheet", (214, 210, 198), (180, 172, 158))):
        s = Canvas(256, 256)
        paper(s, rng_for(name), 0, 0, 192, 220, color, ink, stains=10)
        rr = rng_for(name + "_tear")
        for x in range(0, 192):
            for y in range(rr.randint(0, 3)):
                s.set(x, 219 - y, (0, 0, 0), 0)
        out(name, s)

    d = Canvas(256, 256)
    d.rect(0, 0, 176, 166, (198, 186, 160))
    paper(d, r, 0, 0, 176, 166, (206, 194, 168), (120, 100, 76), stains=4)

    def slot(x, y):
        d.rect(x - 1, y - 1, x + 17, y + 17, (120, 104, 84))
        d.rect(x, y, x + 17, y + 17, (236, 228, 208))
        d.rect(x, y, x + 16, y + 16, (150, 134, 110))
    for (x, y) in ((38, 17), (38, 53), (62, 35)):
        slot(x, y)
    d.rect(118, 29, 148, 59, (120, 104, 84))
    slot(124, 35)
    for row in range(3):
        for col in range(9):
            slot(8 + col * 18, 84 + row * 18)
    for col in range(9):
        slot(8 + col * 18, 142)
    for i in range(24):
        h = 8 - abs(i - 20) if i > 16 else 3
        d.rect(89 + i, 43 - h // 2, 90 + i, 43 + (h + 1) // 2, (150, 134, 110))
        d.rect(176 + i, 8 - h // 2, 177 + i, 8 + (h + 1) // 2, RUBRIC_BRIGHT)
    draw(d, 1, 42, 38, (120, 100, 76))
    draw(d, 6, 41, 56, (120, 100, 76))
    out("scriptorium_desk", d)


def environment():
    out = saver("environment")
    r = rng_for("sky")
    ink = Canvas(64, 64)
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 31.5, y - 31.5)
            wobble = 2.5 * math.sin(math.atan2(y - 31.5, x - 31.5) * 7) + r.uniform(-1, 1)
            if d < 20 + wobble:
                ink.set(x, y, (8, 7, 10), 255)
            elif d < 28 + wobble:
                ink.set(x, y, (20, 16, 20), int(255 * (1 - (d - 20 - wobble) / 8)))
    for _ in range(12):
        a = r.uniform(0, math.tau)
        for i in range(r.randint(4, 10)):
            x, y = int(31.5 + math.cos(a) * (20 + i)), int(31.5 + math.sin(a) * (20 + i))
            ink.set(x, y + i // 2, (12, 10, 14), 200)
    out("inkwell", ink)

    writing = Canvas(64, 64)
    scribble(writing, 0, 0, 64, 64, (40, 30, 26), r, alpha=180, density=0.45, line_gap=6)
    for _ in range(10):
        draw(writing, r.randrange(12), r.randint(0, 58), r.randint(0, 58), (90, 20, 14), 200)
    out("sky_writing", writing)


def misc():
    out = saver("misc")
    r = rng_for("vignette")
    v = Canvas(256, 256)
    n = value_noise(256, 256, 24, r)
    for y in range(256):
        for x in range(256):
            dx, dy = (x - 127.5) / 127.5, (y - 127.5) / 127.5
            d = math.sqrt(dx * dx + dy * dy)
            a = max(0.0, min(1.0, (d - 0.62 + (n[y, x] - 0.5) * 0.35) * 2.4))
            if a > 0:
                v.set(x, y, (10, 8, 12), int(255 * a))
    out("ink_vignette", v)

    f = Canvas(128, 128)
    rf = rng_for("face")
    for y in range(128):
        for x in range(128):
            dx, dy = (x - 64) / 40, (y - 60) / 52
            if dx * dx + dy * dy < 1:
                f.set(x, y, (12, 10, 14), int(70 * (1 - dx * dx - dy * dy)))
    for ex in (46, 82):
        f.circle(ex, 52, 7, (0, 0, 0), 230)
        f.circle(ex, 52, 2, (220, 214, 190), 200)
    f.rect(48, 84, 80, 88, (0, 0, 0), 200)
    for _ in range(40):
        x = rf.randint(30, 98)
        for y in range(rf.randint(60, 90), rf.randint(95, 128)):
            f.set(x, y, (10, 8, 12), 90)
    out("vignette_face", f)


def armor():
    out = saver("models/armor")
    r = rng_for("armor")
    l1 = Canvas(64, 32)
    # Head (8x8x8 at 0,0), body (8x12x4 at 16,16), arms (4x12x4 at 40,16), legs (4x12x4 at 0,16)
    for y in range(0, 16):
        for x in range(0, 32):
            l1.set(x, y, jitter(RUBRIC, 8, r))
    l1.rect(8, 8, 16, 16, (0, 0, 0), 0)
    l1.rect(9, 12, 15, 16, (30, 24, 22))
    for y in range(16, 32):
        for x in range(16, 56):
            l1.set(x, y, jitter(RUBRIC, 8, r))
    for y in range(20, 32):
        l1.set(23, y, GILT)
        l1.set(24, y, GILT)
    for y in range(16, 32):
        for x in range(0, 16):
            l1.set(x, y, jitter(shade(RUBRIC, 0.75), 8, r))
        for x in range(0, 16):
            if y > 27:
                l1.set(x, y, (60, 40, 30))
    out("rubric_layer_1", l1)
    l2 = Canvas(64, 32)
    for y in range(16, 32):
        for x in range(0, 16):
            l2.set(x, y, jitter(shade(RUBRIC, 0.7), 8, r))
        for x in range(16, 40):
            if y < 22:
                l2.set(x, y, jitter(shade(RUBRIC, 0.7), 8, r))
    l2.rect(16, 16, 40, 17, GILT)
    out("rubric_layer_2", l2)
    fd = Canvas(64, 32)
    for x in range(0, 32):
        fd.set(x, 14, GILT)
        fd.set(x, 15, shade(GILT, 0.8))
    fd.set(11, 13, (170, 60, 220))
    fd.set(12, 13, (170, 60, 220))
    out("first_draft_layer_1", fd)
    out("first_draft_layer_2", Canvas(64, 32))


def logo():
    r = rng_for("logo")
    w, h = 400, 160
    c = Canvas(w, h)
    paper(c, r, 0, 0, w, h, VELLUM, VELLUM_DARK, stains=14)
    # The lower writing: large faded rubric glyphs, reversed, showing through.
    for i in range(10):
        draw(c, (11 - i) % 12, 18 + i * 37, 34, (200, 150, 140), 255, scale=6)
    # The upper writing: the name, in iron-gall ink, built from the glyph pen.
    letters = {
        "P": ["####.", "#...#", "####.", "#....", "#...."], "A": [".###.", "#...#", "#####", "#...#", "#...#"],
        "L": ["#....", "#....", "#....", "#....", "#####"], "I": ["#####", "..#..", "..#..", "..#..", "#####"],
        "M": ["#...#", "##.##", "#.#.#", "#...#", "#...#"], "S": [".####", "#....", ".###.", "....#", "####."],
        "E": ["#####", "#....", "####.", "#....", "#####"], "T": ["#####", "..#..", "..#..", "..#..", "..#.."],
    }
    x = 24
    for ch in "PALIMPSEST":
        g = letters[ch]
        for gy, row in enumerate(g):
            for gx, px in enumerate(row):
                if px == "#":
                    for sy in range(7):
                        for sx in range(6):
                            c.set(x + gx * 6 + sx, 60 + gy * 7 + sy, jitter(INK, 10, r))
        x += 36
    c.rect(24, 108, 376, 111, RUBRIC)
    for i in range(0, 352, 9):
        c.set(24 + i, 106, RUBRIC)
    c.save(os.path.join(RES, "palimpsest_logo.png"))
    icon = Canvas(64, 64)
    paper(icon, r, 0, 0, 64, 64, VELLUM, VELLUM_DARK, stains=3)
    draw(icon, 4, 8, 8, (200, 150, 140), 255, scale=9)
    g = letters["P"]
    for gy, row in enumerate(g):
        for gx, px in enumerate(row):
            if px == "#":
                icon.rect(14 + gx * 7, 12 + gy * 8, 21 + gx * 7, 20 + gy * 8, INK)
    icon.save(os.path.join(RES, "pack.png"))


def effects():
    """18x18 status effect icons."""
    out = saver("mob_effect")
    os.makedirs(os.path.join(TEX, "mob_effect"), exist_ok=True)
    r = rng_for("effects")

    # Erasure: a pale scraped stroke across a sliver of written line.
    e = Canvas(18, 18)
    for x in range(2, 16):
        if x % 3 != 0:
            e.set(x, 5, INK, 220)
            e.set(x, 12, INK, 220)
    for i in range(14):
        x, y = 2 + i, 14 - i * 11 // 13
        for w in range(-1, 2):
            e.set(x, y + w, SCRAPED, 255 - abs(w) * 60)
    e.noise(10, r)
    e.outline(INK_DEEP, 160)
    out("erasure", e)

    # Warded: a red chalk ring with the ward stroke through it.
    w = Canvas(18, 18)
    w.circle(9, 9, 7, RUBRIC)
    w.circle(9, 9, 5, (0, 0, 0), 0)
    for y in range(18):
        for x in range(18):
            d = ((x - 8.5) ** 2 + (y - 8.5) ** 2) ** 0.5
            if 5.2 < d < 7.4:
                w.set(x, y, jitter(RUBRIC_BRIGHT, 16, r))
    w.line(9, 4, 9, 13, RUBRIC_BRIGHT)
    w.line(8, 4, 8, 13, RUBRIC)
    w.line(6, 7, 11, 7, RUBRIC_BRIGHT)
    w.line(6, 11, 11, 11, RUBRIC_BRIGHT)
    w.outline(INK_DEEP, 150)
    out("warded", w)

    # Inkblind: an eye under a spreading blot.
    b = Canvas(18, 18)
    for x in range(2, 16):
        h = int(3.2 * (1 - ((x - 8.5) / 7.0) ** 2))
        for y in range(9 - h, 10 + h):
            b.set(x, y, BONE)
    b.circle(9, 9, 2, INK_DEEP)
    for _ in range(9):
        x, y = r.randint(4, 13), r.randint(1, 4)
        b.circle(x, y, 1.2, BLOT, 235)
    for x in (5, 10, 13):
        for y in range(3, 9 + r.randint(1, 5)):
            b.set(x, y, BLOT, 230)
    b.outline(INK_DEEP, 150)
    out("inkblind", b)


def main():
    tex_blocks.gen(saver("block"), lambda n: rng_for("block:" + n))
    tex_items.gen(saver("item"), rng_for)
    particles()
    gui()
    environment()
    misc()
    armor()
    effects()
    logo()
    print("textures generated")


if __name__ == "__main__":
    main()
