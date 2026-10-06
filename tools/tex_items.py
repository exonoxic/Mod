"""Item sprites (16x16). Every sprite gets a dark ink outline so the set reads as one family."""
import math

from glyphs import draw_small
from pixel import *

OUTLINE = (24, 20, 26)


def sprite():
    return Canvas(16, 16)


def finish(c):
    c.outline(OUTLINE)
    return c


def page(c, r, x0, y0, x1, y1, paper, ink, torn=False, density=0.5):
    for y in range(y0, y1):
        for x in range(x0, x1):
            if torn and (y == y0 or x == x1 - 1) and r.random() < 0.4:
                continue
            c.set(x, y, jitter(paper, 6, r))
    scribble(c, x0 + 1, y0 + 1, x1 - 1, y1 - 1, ink, r, density=density, line_gap=2)


def bottle(c, r, liquid, glass=(180, 200, 210)):
    c.rect(6, 2, 10, 4, (140, 100, 60))
    c.rect(7, 4, 9, 6, glass)
    for y in range(6, 15):
        w = 4 if y > 7 else 3
        for x in range(8 - w, 8 + w):
            c.set(x, y, jitter(liquid if y > 8 else glass, 5, r))
    c.set(5, 10, (230, 240, 245))
    c.set(5, 11, (230, 240, 245))


def blade(c, r, x0, y0, x1, y1, width, color, edge):
    steps = max(abs(x1 - x0), abs(y1 - y0))
    for i in range(steps + 1):
        t = i / steps
        x = round(x0 + (x1 - x0) * t)
        y = round(y0 + (y1 - y0) * t)
        w = max(1, round(width * (1 - t * 0.7)))
        for k in range(w):
            c.set(x + k, y + k, jitter(color, 6, r))
        c.set(x, y, edge)


def handle(c, x0, y0, n, color):
    for i in range(n):
        c.set(x0 - i, y0 + i, color)
        c.set(x0 - i + 1, y0 + i, shade(color, 0.7))


def tool(c, r, head_pts, head_color, edge, handle_color=(80, 56, 36)):
    for i in range(10):
        c.set(3 + i, 12 - i, handle_color)
        c.set(4 + i, 12 - i, shade(handle_color, 0.75))
    for (x, y) in head_pts:
        c.set(x, y, jitter(head_color, 6, r))
    for (x, y) in head_pts[:3]:
        c.set(x, y, edge)


SWORD_HEAD = [(x, 15 - x - 3) for x in range(4, 14)] + [(x + 1, 15 - x - 3) for x in range(4, 13)]
PICK_HEAD = [(5, 3), (6, 2), (7, 2), (8, 2), (9, 2), (10, 3), (11, 4), (12, 5), (13, 6), (13, 7), (4, 4), (4, 5)]
AXE_HEAD = [(8, 2), (9, 2), (10, 2), (11, 3), (12, 4), (12, 5), (11, 6), (10, 6), (9, 5), (8, 4), (9, 3), (10, 3), (11, 4), (10, 4), (10, 5), (9, 4)]


def gen(out, r_for):
    def item(name, fn):
        r = r_for("item:" + name)
        c = sprite()
        fn(c, r)
        out(name, finish(c))

    item("vellum_scrap", lambda c, r: [c.set(x, y, jitter(VELLUM, 8, r)) for y in range(4, 13) for x in range(3, 13)
                                       if (x + y) % 7 != 0 and not (y > 10 and x > 9)])
    item("blank_vellum", lambda c, r: page(c, r, 3, 2, 13, 14, VELLUM_LIGHT, VELLUM_DARK, density=0.0))
    item("oak_gall", lambda c, r: (c.circle(7.5, 8.5, 4.5, (150, 108, 64)), c.circle(6.5, 7.5, 1.5, (190, 150, 100)),
                                   c.set(9, 10, (60, 40, 24)), c.set(8, 3, (70, 90, 40))))
    item("iron_gall_ink", lambda c, r: bottle(c, r, (20, 18, 30)))
    item("cinnabar", lambda c, r: [c.set(x, y, jitter((180, 40, 30) if (x + y) % 3 else (230, 90, 70), 10, r))
                                   for y in range(3, 14) for x in range(4, 13) if abs(x - 8) + abs(y - 8) < 6])

    def vermilion(c, r):
        for x in range(3, 13):
            c.set(x, 12, (140, 110, 80))
            c.set(x, 13, (110, 84, 60))
        for y in range(6, 12):
            w = (y - 5)
            for x in range(8 - w, 8 + w):
                c.set(x, y, jitter(RUBRIC_BRIGHT, 14, r))
    item("vermilion", vermilion)

    def fragment(c, r):
        for y in range(3, 14):
            for x in range(3, 14):
                if x + y > 7 and x + y < 24 and abs(x - y) < 7:
                    c.set(x, y, jitter((140, 136, 128), 10, r))
        draw_small(c, 4, 6, 6, (80, 60, 140))
        c.set(7, 7, (170, 150, 240))
    item("undertext_fragment", fragment)
    item("lampblack", lambda c, r: [c.set(x, y, jitter((22, 20, 24), 6, r)) for y in range(8, 14) for x in range(3, 13)
                                    if abs(x - 7.5) < (y - 6)])
    item("raw_illumine", lambda c, r: (c.circle(7.5, 8.5, 4.5, (170, 130, 56)), c.circle(6, 7, 2, GILT_LIGHT), c.set(10, 10, (255, 250, 210))))

    def leaf(c, r):
        for y in range(3, 13):
            for x in range(3, 13):
                c.set(x, y, jitter(GILT, 8, r))
        c.line(3, 12, 12, 3, GILT_LIGHT)
        c.line(4, 12, 12, 4, (255, 246, 200))
        c.set(12, 12, (140, 100, 40))
    item("illumine_leaf", leaf)

    def ingot(color, streak=None):
        def fn(c, r):
            for y in range(6, 12):
                for x in range(2 + (11 - y), 14 - (y - 6) // 2):
                    c.set(x, y, jitter(color, 6, r))
            c.line(7, 6, 12, 6, shade(color, 1.3))
            if streak:
                for x in range(4, 12, 3):
                    c.set(x, 8 + (x % 2), streak)
        return fn
    item("gall_steeped_iron", ingot((196, 196, 200), (30, 28, 40)))
    item("gall_iron_ingot", ingot((70, 78, 100)))
    item("foxed_dust", lambda c, r: [c.set(x, y, jitter((150, 110, 70) if r.random() < 0.6 else (200, 170, 120), 10, r))
                                     for y in range(8, 14) for x in range(3, 13) if abs(x - 7.5) < (y - 6) and r.random() < 0.8])

    def quill(color, tip):
        def fn(c, r):
            for i in range(11):
                x, y = 12 - i, 2 + i
                c.set(x, y, shade(color, 0.6))
                for k in range(1, 3 if i < 8 else 1):
                    c.set(x - k, y - k + 1, jitter(color, 8, r))
                    c.set(x + k, y + k - 1, jitter(color, 8, r))
            c.set(2, 13, tip)
            c.set(1, 14, tip)
        return fn
    item("black_quill", quill((34, 32, 44), STEEL))
    item("scriveners_quill", quill((244, 238, 220), GILT))

    def antler(c, r):
        c.line(4, 14, 8, 6, BONE)
        c.line(8, 6, 11, 2, BONE)
        c.line(8, 6, 13, 6, BONE)
        c.line(6, 10, 3, 6, BONE)
        c.line(10, 4, 12, 1, shade(BONE, 0.85))
    item("pale_antler", antler)
    item("blot_residue", lambda c, r: (c.circle(7.5, 8.5, 5, (16, 14, 22)), c.circle(6, 7, 1.5, (60, 56, 80)), c.set(8, 14, (16, 14, 22))))
    item("redaction_strip", lambda c, r: (c.rect(1, 6, 15, 10, (8, 8, 10)), c.rect(1, 6, 15, 7, (40, 40, 46))))

    def needle(c, r):
        c.line(2, 13, 13, 2, STEEL)
        c.set(13, 2, (230, 234, 240))
        c.set(3, 12, (40, 40, 44))
        for i in range(8):
            c.set(3 + i, 13 - (i // 2) + 1, RUBRIC)
    item("bookbinder_needle", needle)

    def shard(c, r):
        for y in range(1, 15):
            w = int(3 * math.sin(y / 15 * math.pi)) + 1
            for x in range(8 - w + (y // 4), 8 + (y // 4)):
                c.set(x, y, jitter((232, 230, 224), 6, r))
        c.line(8, 1, 11, 14, (255, 255, 255))
        c.set(9, 7, (200, 40, 30))
    item("rasure_shard", shard)

    def key(c, r):
        c.circle(5, 5, 3.3, (214, 206, 190))
        c.circle(5, 5, 1.3, (0, 0, 0), 0)
        c.set(5, 5, (0, 0, 0), 0)
        c.line(7, 7, 13, 13, (214, 206, 190))
        c.set(12, 13, (214, 206, 190))
        c.set(13, 11, (214, 206, 190))
        c.set(11, 12, (214, 206, 190))
        c.line(3, 8, 8, 3, RUBRIC)
    item("spine_key", key)
    item("torn_folio", lambda c, r: page(c, r, 3, 2, 13, 14, VELLUM, INK_BROWN, torn=True, density=0.6))
    item("faded_page", lambda c, r: page(c, r, 3, 2, 13, 14, (214, 208, 190), (170, 160, 146), torn=True, density=0.3))

    def last_folio(c, r):
        page(c, r, 3, 2, 13, 14, VELLUM_LIGHT, INK, density=0.4)
        c.rect(4, 7, 12, 9, RUBRIC_BRIGHT)
        c.set(12, 2, GILT)
    item("last_folio", last_folio)

    def book(cover, ribbon):
        def fn(c, r):
            for y in range(2, 14):
                for x in range(3, 13):
                    c.set(x, y, jitter(cover, 8, r))
            c.rect(11, 3, 13, 13, VELLUM_LIGHT)
            c.rect(3, 2, 4, 14, shade(cover, 0.7))
            c.rect(8, 11, 9, 15, ribbon)
        return fn
    item("commonplace_book", book(LEATHER, RUBRIC))

    def folio_descent(c, r):
        book((40, 30, 46), GILT)(c, r)
        draw_small(c, 0, 6, 6, RUBRIC_BRIGHT)
    item("folio_of_descent", folio_descent)

    def misprint(c, r):
        for y in range(3, 13):
            for x in range(3, 13):
                c.set(x, y, (200, 0, 200) if ((x // 2) + (y // 2)) % 2 else (10, 10, 10))
    item("misprint", misprint)

    def rasorium(c, r):
        for i in range(6):
            c.set(3 + i, 12 - i, (84, 60, 40))
            c.set(4 + i, 12 - i, (60, 42, 28))
        for i in range(6):
            c.set(9 + i // 2 + (1 if i > 3 else 0), 6 - i, jitter((200, 204, 212), 6, r))
            c.set(10 + i // 2, 7 - i, jitter((150, 156, 166), 6, r))
        c.set(13, 1, (240, 242, 246))
    item("iron_rasorium", rasorium)

    def rasure_edge(c, r):
        for i in range(12):
            x, y = 2 + i, 13 - i
            for k in range(3 if i > 3 else 1):
                c.set(x + k, y, jitter((230, 228, 222), 6, r))
            c.set(x, y, (255, 255, 255))
        c.rect(2, 12, 5, 15, (160, 30, 24))
        c.set(8, 7, (200, 40, 30))
    item("rasure_edge", rasure_edge)

    def lens(c, r):
        c.circle(9, 7, 4.8, GILT)
        c.circle(9, 7, 3.6, (170, 200, 210))
        c.set(8, 5, (240, 250, 255))
        c.set(7, 6, (240, 250, 255))
        c.line(5, 10, 2, 13, (90, 60, 40))
        draw_small(c, 7, 8, 6, (80, 60, 140), 140)
    item("reading_lens", lens)

    def dowsing(frame):
        def fn(c, r):
            c.line(8, 0, 8 + frame, 5, (200, 190, 160))
            quill((34, 32, 44), STEEL)(tmp := sprite(), r)
            for y in range(16):
                for x in range(16):
                    p = tmp.px[y, x]
                    if p[3]:
                        nx = int(x * 0.6 + 3 + frame * 1.5)
                        ny = int(y * 0.6 + 5)
                        c.set(nx, ny, tuple(int(v) for v in p[:3]))
        return fn
    for f in range(4):
        item(f"dowsing_quill_{f}", dowsing(f))

    def bookmark(c, r):
        for y in range(1, 14):
            c.set(7, y, jitter(RUBRIC_BRIGHT, 8, r))
            c.set(8, y, jitter(RUBRIC, 8, r))
        c.set(7, 14, RUBRIC)
        c.set(8, 15, RUBRIC)
        c.set(6, 14, RUBRIC)
        c.rect(6, 1, 10, 3, GILT)
    item("bookmark", bookmark)

    def seal(c, r):
        c.circle(7.5, 8, 5.5, RUBRIC)
        c.circle(7.5, 8, 4, RUBRIC_BRIGHT)
        draw_small(c, 11, 6, 7, (100, 18, 12))
        c.set(12, 4, RUBRIC)
        c.set(13, 3, RUBRIC)
    item("seal_of_closing", seal)

    def ink_bomb(c, r):
        c.circle(7.5, 9.5, 4.8, (26, 24, 34))
        c.circle(6, 8, 1.2, (80, 76, 100))
        c.rect(6, 3, 10, 5, (120, 90, 60))
        c.line(9, 3, 12, 0, (200, 180, 120))
    item("ink_bomb", ink_bomb)

    def censer(c, r):
        c.line(8, 0, 8, 5, (90, 80, 70))
        c.circle(8, 10, 4.5, GILT)
        c.rect(4, 9, 13, 11, (140, 100, 40))
        c.set(7, 6, (255, 250, 220))
        c.set(9, 5, (255, 250, 220), 160)
    item("illumined_censer", censer)

    def chalk(c, r):
        for i in range(9):
            c.set(3 + i, 12 - i, jitter(RUBRIC_BRIGHT, 10, r))
            c.set(4 + i, 12 - i, jitter(RUBRIC, 10, r))
            c.set(4 + i, 13 - i, jitter(RUBRIC, 10, r))
        c.set(12, 3, (240, 200, 190))
    item("rubric_chalk", chalk)

    for name in ("sword", "pickaxe", "axe"):
        pts = {"sword": SWORD_HEAD, "pickaxe": PICK_HEAD, "axe": AXE_HEAD}[name]
        def fn(c, r, pts=pts, name=name):
            if name == "sword":
                for i in range(4):
                    c.set(2 + i, 13 - i, (80, 56, 36))
                c.rect(4, 10, 7, 11, (40, 44, 56))
                for (x, y) in pts:
                    if x >= 5:
                        c.set(x, y, jitter((80, 88, 112), 6, r))
                c.line(6, 9, 13, 2, (150, 160, 190))
                c.set(13, 2, (20, 18, 26))
            else:
                tool(c, r, pts, (80, 88, 112), (150, 160, 190))
        item(f"gall_iron_{name}", fn)

    def armor(kind):
        def fn(c, r):
            red, dark = RUBRIC, shade(RUBRIC, 0.7)
            if kind == "hood":
                for y in range(3, 12):
                    for x in range(3, 13):
                        if abs(x - 7.5) < 5 - max(0, 5 - y) * 0.6:
                            c.set(x, y, jitter(red, 8, r))
                c.rect(5, 7, 11, 11, (30, 24, 22))
            elif kind == "robe":
                for y in range(2, 15):
                    for x in range(2, 14):
                        if not (y < 5 and 6 <= x <= 9):
                            c.set(x, y, jitter(red, 8, r))
                c.rect(7, 5, 9, 15, GILT)
            elif kind == "leggings":
                for y in range(3, 15):
                    for x in range(4, 12):
                        if not (y > 7 and 7 <= x <= 8):
                            c.set(x, y, jitter(dark, 8, r))
                c.rect(4, 3, 12, 4, GILT)
            else:
                c.rect(3, 8, 7, 14, dark)
                c.rect(9, 8, 13, 14, dark)
                c.rect(2, 12, 7, 14, (60, 40, 30))
                c.rect(9, 12, 14, 14, (60, 40, 30))
        return fn
    for k in ("hood", "robe", "leggings", "boots"):
        item(f"rubric_{k}", armor(k))

    def circlet(c, r):
        for a in range(40):
            t = a / 40 * math.tau
            x, y = 7.5 + math.cos(t) * 5.5, 8 + math.sin(t) * 3
            c.set(int(x), int(y), GILT if math.sin(t) < 0.3 else shade(GILT, 0.7))
        c.set(7, 5, (160, 40, 200))
        c.set(8, 5, (200, 90, 240))
    item("circlet_of_the_first_draft", circlet)

    def berries(c, r):
        for (x, y) in ((5, 8), (9, 7), (7, 11), (10, 11)):
            c.circle(x, y, 2.2, (26, 22, 50))
            c.set(x - 1, y - 1, (90, 80, 140))
        c.line(7, 3, 9, 6, (60, 70, 50))
    item("blotberries", berries)

    def tart(c, r):
        for y in range(8, 13):
            for x in range(2, 14):
                c.set(x, y, jitter((200, 160, 100), 8, r))
        for x in range(3, 13):
            c.set(x, 8, jitter((30, 24, 56), 8, r))
            c.set(x, 9, jitter((50, 40, 80), 8, r))
        c.rect(2, 12, 14, 13, (160, 120, 70))
    item("blotberry_tart", tart)
    item("candlewort_tea", lambda c, r: bottle(c, r, (214, 170, 80)))

    def wax(c, r):
        for i in range(8):
            c.set(4 + i, 11 - i, jitter(RUBRIC, 8, r))
            c.set(5 + i, 11 - i, jitter(RUBRIC_BRIGHT, 8, r))
            c.set(5 + i, 12 - i, jitter(RUBRIC, 8, r))
        c.set(12, 3, (60, 50, 40))
    item("sealing_wax", wax)

    def meat(raw):
        def fn(c, r):
            col = (220, 200, 196) if raw else (170, 120, 90)
            for y in range(4, 13):
                for x in range(3, 13):
                    if (x - 8) ** 2 / 25 + (y - 8) ** 2 / 16 < 1:
                        c.set(x, y, jitter(col, 8, r))
            c.rect(10, 11, 14, 13, BONE)
        return fn
    item("pale_venison", meat(True))
    item("cooked_pale_venison", meat(False))

    def ration(c, r):
        for y in range(5, 13):
            for x in range(3, 13):
                c.set(x, y, jitter(VELLUM_DARK, 8, r))
        c.rect(7, 5, 9, 13, (120, 90, 60))
        c.rect(3, 8, 13, 10, (120, 90, 60))
        c.set(8, 9, RUBRIC)
    item("scribes_ration", ration)

    def disc(c, r):
        c.circle(7.5, 7.5, 6.8, (26, 24, 30))
        c.circle(7.5, 7.5, 2.6, RUBRIC)
        c.set(7, 7, (10, 10, 10))
        c.set(4, 4, (70, 68, 80))
        c.set(5, 3, (70, 68, 80))
    item("music_disc_lower_writing", disc)

    def door_item(c, r):
        for y in range(1, 15):
            for x in range(4, 12):
                c.set(x, y, jitter((54, 46, 48), 6, r))
        c.rect(5, 2, 11, 6, (140, 120, 90))
        c.rect(6, 3, 10, 5, (30, 28, 34))
        c.set(10, 9, GILT)
    item("blotwood_door", door_item)

    # The Margin Compass: 32 needle angles over one face.
    for frame in range(32):
        r = r_for("compass")
        c = sprite()
        c.circle(7.5, 7.5, 6.5, (120, 90, 56))
        c.circle(7.5, 7.5, 5.5, VELLUM_LIGHT)
        for a in range(8):
            t = a / 8 * math.tau
            c.set(int(7.5 + math.cos(t) * 4.5), int(7.5 + math.sin(t) * 4.5), SEPIA)
        t = frame / 32 * math.tau
        dx, dy = math.sin(t), -math.cos(t)
        for i in range(5):
            c.set(int(round(7.5 + dx * i)), int(round(7.5 + dy * i)), RUBRIC_BRIGHT if i > 1 else INK)
        for i in range(1, 3):
            c.set(int(round(7.5 - dx * i)), int(round(7.5 - dy * i)), INK)
        out(f"margin_compass_{frame:02d}", finish(c))
