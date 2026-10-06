"""Block textures (16x16)."""
import math

from glyphs import draw_small, GLYPHS
from pixel import *


def base_noise(r, color, amount=10, scale=4):
    c = Canvas(16, 16)
    n = value_noise(16, 16, scale, r)
    for y in range(16):
        for x in range(16):
            c.set(x, y, jitter(shade(color, 0.85 + 0.3 * n[y, x]), amount, r))
    return c


def stone(r, color, cracks=3, crack_color=None):
    c = base_noise(r, color, 8, 4)
    cc = crack_color or shade(color, 0.7)
    for _ in range(cracks):
        x, y = r.randrange(16), r.randrange(16)
        for _ in range(r.randint(3, 6)):
            c.set(x, y, cc)
            x += r.choice((-1, 0, 1))
            y += r.choice((0, 1))
    return c


def bricks(r, brick, mortar, rows=4, glyph=None, glyph_color=None, scraped=False):
    c = Canvas(16, 16, mortar)
    h = 16 // rows
    for row in range(rows):
        off = (row % 2) * 4
        for bx in range(-1, 3):
            x0 = bx * 8 + off
            col = jitter(brick, 10, r)
            for y in range(row * h, row * h + h - 1):
                for x in range(x0, x0 + 7):
                    if 0 <= x < 16:
                        c.set(x, y, jitter(col, 6, r))
            if scraped and r.random() < 0.5:
                for x in range(x0 + 1, x0 + 6):
                    if 0 <= x < 16:
                        c.set(x, row * h + r.randint(0, h - 2), mix(col, SCRAPED, 0.7))
    if glyph is not None:
        draw_small(c, glyph, 6, 6, glyph_color)
        for x, y in ((5, 5), (9, 5), (5, 9), (9, 9)):
            c.set(x, y, glyph_color)
    return c


def planks(r, color):
    c = Canvas(16, 16)
    for row in range(4):
        col = jitter(color, 8, r)
        for y in range(row * 4, row * 4 + 4):
            for x in range(16):
                v = col if y % 4 != 3 else shade(col, 0.7)
                c.set(x, y, jitter(v, 5, r))
        seam = r.randrange(3, 13)
        c.set(seam, row * 4 + 1, shade(col, 0.75))
        c.set(seam, row * 4 + 2, shade(col, 0.75))
    return c


def log_side(r, bark, drip=None):
    c = Canvas(16, 16)
    for x in range(16):
        col = jitter(bark, 10, r)
        for y in range(16):
            v = col if (x + (y // 5)) % 5 else shade(col, 0.7)
            c.set(x, y, jitter(v, 5, r))
    if drip:
        for _ in range(3):
            x = r.randrange(16)
            for y in range(r.randint(2, 7)):
                c.set(x, y, drip)
    return c


def log_top(r, bark, core, ring):
    c = Canvas(16, 16, bark)
    for y in range(1, 15):
        for x in range(1, 15):
            d = math.hypot(x - 7.5, y - 7.5)
            v = core if int(d) % 3 else ring
            c.set(x, y, jitter(v, 6, r))
    return c


def ore(r, base, colors, clusters=4):
    c = base
    for _ in range(clusters):
        cx, cy = r.randint(2, 13), r.randint(2, 13)
        for _ in range(r.randint(3, 5)):
            x, y = cx + r.randint(-1, 1), cy + r.randint(-1, 1)
            c.set(x, y, r.choice(colors))
            c.set(x + 1, y, colors[-1])
    return c


def cross_plant(r, stem, bloom=None, bloom_center=None):
    c = Canvas(16, 16)
    for y in range(5, 16):
        c.set(7 + (1 if y < 9 and r.random() < 0.3 else 0), y, jitter(stem, 8, r))
    for (x0, y0, x1, y1) in ((7, 12, 4, 9), (7, 11, 10, 8)):
        c.line(x0, y0, x1, y1, shade(stem, 1.1))
    if bloom:
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1), (-1, -1), (1, -1)):
            c.set(7 + dx, 4 + dy, jitter(bloom, 8, r))
        c.set(7, 4, bloom_center or bloom)
        c.set(7, 2, (255, 214, 120))
        c.set(7, 1, (255, 240, 180), 180)
    return c


def grass_blades(r, color, count=9):
    c = Canvas(16, 16)
    for _ in range(count):
        x = r.randint(1, 14)
        h = r.randint(5, 13)
        lean = r.choice((-1, 0, 0, 1))
        for i in range(h):
            xx = x + (lean if i > h // 2 else 0)
            c.set(xx, 15 - i, jitter(color, 12, r), 200 if i > h - 3 else 255)
    return c


def gen(out, r_for):
    """out(name, canvas, mcmeta=None) saves a texture under textures/block."""
    r = r_for("stone")
    ps = stone(r_for("palimpsest_stone"), (125, 123, 120), 2)
    rr = r_for("ps_glyphs")
    for i in range(3):
        draw_small(ps, rr.randrange(12), rr.randint(1, 12), rr.randint(1, 12), (70, 64, 72), 200)
    out("palimpsest_stone", ps)
    dps = stone(r_for("deepslate_palimpsest_stone"), (74, 74, 80), 2, (54, 54, 60))
    for i in range(3):
        draw_small(dps, rr.randrange(12), rr.randint(1, 12), rr.randint(1, 12), (130, 122, 150), 200)
    out("deepslate_palimpsest_stone", dps)

    sc = base_noise(r_for("scraped_stone"), (218, 212, 198), 5, 5)
    rs = r_for("scrape_marks")
    for _ in range(6):
        y = rs.randrange(16)
        x = rs.randrange(4)
        for i in range(rs.randint(6, 12)):
            sc.set(x + i, y + (i // 5), (236, 232, 222))
    for _ in range(2):
        draw_small(sc, rs.randrange(12), rs.randint(1, 12), rs.randint(1, 12), (196, 186, 170), 140)
    out("scraped_stone", sc)

    out("cinnabar_ore", ore(r_for("cin"), stone(r_for("cin_s"), (125, 123, 120), 2), [(150, 30, 24), (196, 52, 36), (226, 90, 70)]))
    out("deepslate_cinnabar_ore", ore(r_for("dcin"), stone(r_for("dcin_s"), (74, 74, 80), 2, (54, 54, 60)),
                                      [(140, 26, 22), (180, 44, 32), (214, 80, 64)]))
    out("illumine_ore", ore(r_for("ill"), stone(r_for("ink_ore"), (38, 36, 44), 3, (24, 22, 28)),
                            [(170, 130, 50), (226, 184, 80), (250, 226, 150)], 5))

    out("candlewort", cross_plant(r_for("candlewort"), (104, 112, 80), (232, 226, 196), (255, 230, 150)))

    fg_top = base_noise(r_for("faded_top"), (150, 158, 138), 10, 3)
    out("faded_grass_block_top", fg_top)
    fg_side = Canvas(16, 16)
    dirt = base_noise(r_for("dirt"), (134, 96, 67), 10, 3)
    fg_side.paste(dirt, 0, 0)
    rs = r_for("faded_side")
    for x in range(16):
        for y in range(rs.randint(3, 5)):
            fg_side.set(x, y, jitter((150, 158, 138), 10, rs))
    out("faded_grass_block_side", fg_side)

    torch = Canvas(16, 16)
    for y in range(8, 16):
        torch.set(7, y, (104, 78, 50))
        torch.set(8, y, (84, 62, 40))
    for y in (6, 7):
        torch.set(7, y, (40, 36, 34))
        torch.set(8, y, (58, 50, 46))
    out("spent_torch", torch)

    tear = Canvas(16, 64)
    rt = r_for("tear")
    for frame in range(4):
        for y in range(16):
            w = 2 + int(2.5 * math.sin((y + frame * 3) * 0.6) ** 2)
            for x in range(8 - w, 8 + w):
                edge = x in (8 - w, 8 + w - 1)
                c = (120, 20, 20) if edge and rt.random() < 0.5 else jitter(BLOT, 4, rt)
                tear.set(x, frame * 16 + y, c)
            if rt.random() < 0.2:
                draw_small(tear, rt.randrange(12), 6, frame * 16 + y, (90, 84, 110), 180)
    out("tear", tear, {"animation": {"frametime": 4, "interpolate": True}})

    rc = r_for("chalk")
    for v in range(4):
        c = Canvas(16, 16)
        for i in range(16):
            y = 7 + int(round(math.sin(i * 0.5 + v) * 1.2))
            c.set(i, y, jitter(RUBRIC_BRIGHT, 12, rc))
            if rc.random() < 0.4:
                c.set(i, y + 1, jitter(RUBRIC, 12, rc), 180)
        draw_small(c, v * 3, 2 + v * 2, 2, RUBRIC_BRIGHT)
        draw_small(c, v * 3 + 1, 10 - v, 11, RUBRIC_BRIGHT)
        out(f"rubric_chalk_{v}", c)

    ward = Canvas(16, 16)
    rw = r_for("ward")
    ward.rect(0, 0, 16, 16, (74, 54, 34))
    ward.rect(1, 1, 15, 15, (160, 40, 28))
    for y in range(2, 14):
        for x in range(2, 14):
            d = math.hypot(x - 7.5, y - 7.5)
            ward.set(x, y, mix((255, 170, 90), (160, 40, 28), min(1, d / 7)))
    draw_small(ward, 4, 6, 6, (110, 20, 14))
    out("rubric_ward", ward)
    wtop = Canvas(16, 16, (74, 54, 34))
    wtop.rect(2, 2, 14, 14, GILT)
    wtop.rect(4, 4, 12, 12, (120, 90, 40))
    out("rubric_ward_top", wtop)

    desk_top = planks(r_for("desk"), (92, 64, 44))
    desk_top.rect(3, 3, 11, 13, VELLUM_LIGHT)
    scribble(desk_top, 3, 3, 11, 13, INK, r_for("desk_script"), density=0.6, line_gap=2)
    desk_top.rect(12, 3, 15, 6, (24, 22, 28))
    desk_top.set(13, 2, (230, 226, 214))
    desk_top.set(14, 1, (230, 226, 214))
    out("scriptorium_desk_top", desk_top)
    out("scriptorium_desk_side", planks(r_for("desk_side"), (76, 52, 36)))

    altar = stone(r_for("altar"), (150, 142, 128), 1)
    for a in range(32):
        t = a / 32 * math.tau
        altar.set(int(7.5 + math.cos(t) * 6), int(7.5 + math.sin(t) * 6), RUBRIC)
    draw_small(altar, 8, 6, 6, RUBRIC_BRIGHT)
    out("rubric_altar_top", altar)
    aside = bricks(r_for("altar_side"), (150, 142, 128), (96, 90, 82), 4)
    aside.rect(0, 0, 16, 2, RUBRIC)
    out("rubric_altar_side", aside)
    out("reading_stand", planks(r_for("stand"), (104, 72, 46)))

    out("vellum_bricks", bricks(r_for("vb"), VELLUM, (150, 136, 108)))
    out("scraped_vellum_bricks", bricks(r_for("svb"), (226, 220, 204), (160, 150, 130), scraped=True))
    out("rubricated_vellum_bricks", bricks(r_for("rvb"), VELLUM, (150, 136, 108), glyph=4, glyph_color=RUBRIC))

    veil = Canvas(16, 128)
    rv = r_for("veil")
    for frame in range(8):
        for y in range(16):
            for x in range(16):
                veil.set(x, frame * 16 + y, jitter((34, 30, 42), 5, rv), 190)
        for gy in range(0, 16, 6):
            for gx in range(0, 16, 5):
                draw_small(veil, (gx + gy + frame) % 12, gx, frame * 16 + (gy + frame * 2) % 16, (206, 196, 170), 230)
    out("undertext_veil", veil, {"animation": {"frametime": 3, "interpolate": True}})

    gi = base_noise(r_for("gib"), (70, 76, 92), 6, 4)
    for i in range(0, 16, 4):
        gi.rect(0, i, 16, i + 1, (48, 52, 64))
    out("gall_iron_block", gi)
    il = base_noise(r_for("ilb"), GILT, 8, 3)
    for i in range(0, 16, 5):
        il.line(0, i, 15, i + 3, GILT_LIGHT)
    out("illumine_block", il)

    rv_top = base_noise(r_for("ruled"), VELLUM, 5, 5)
    for y in (3, 7, 11, 15):
        for x in range(16):
            rv_top.blend(x, y, (140, 150, 170), 0.35)
    for x in range(16):
        rv_top.blend(3, x, (180, 90, 80), 0.3)
    out("ruled_vellum_top", rv_top)
    soil = base_noise(r_for("soil"), (196, 184, 156), 9, 3)
    for _ in range(8):
        soil.set(r.randrange(16), r.randrange(16), (160, 146, 116))
    out("vellum_soil", soil)
    rside = Canvas(16, 16)
    rside.paste(soil, 0, 0)
    rsd = r_for("ruled_side")
    for x in range(16):
        for y in range(rsd.randint(2, 4)):
            rside.set(x, y, jitter(VELLUM, 5, rsd))
    out("ruled_vellum_side", rside)

    out("inkstone", stone(r_for("inkstone"), (40, 38, 46), 3, (26, 24, 30)))
    cob = Canvas(16, 16, (22, 20, 26))
    rcb = r_for("cob")
    for _ in range(14):
        cx, cy, rad = rcb.randint(0, 15), rcb.randint(0, 15), rcb.randint(2, 4)
        col = jitter((46, 44, 54), 8, rcb)
        for y in range(cy - rad, cy + rad):
            for x in range(cx - rad, cx + rad):
                if math.hypot(x - cx, y - cy) < rad - 0.5:
                    cob.set(x % 16, y % 16, jitter(col, 4, rcb))
    out("cobbled_inkstone", cob)
    pol = base_noise(r_for("pol"), (50, 48, 58), 4, 6)
    pol.rect(0, 0, 16, 1, (70, 68, 80))
    pol.rect(0, 15, 16, 16, (30, 28, 36))
    out("polished_inkstone", pol)
    out("inkstone_bricks", bricks(r_for("ib"), (48, 46, 56), (26, 24, 30)))

    out("blotwood_log", log_side(r_for("bl"), (30, 28, 34), (8, 8, 10)))
    out("blotwood_log_top", log_top(r_for("blt"), (30, 28, 34), (70, 60, 56), (40, 34, 34)))
    out("stripped_blotwood_log", log_side(r_for("sbl"), (74, 62, 58)))
    out("stripped_blotwood_log_top", log_top(r_for("sblt"), (74, 62, 58), (86, 74, 68), (60, 50, 46)))
    out("blotwood_planks", planks(r_for("bp"), (58, 50, 52)))

    door_t = planks(r_for("bdt"), (54, 46, 48))
    door_t.rect(4, 3, 12, 7, (140, 120, 90))
    door_t.rect(5, 4, 11, 6, (30, 28, 34))
    out("blotwood_door_top", door_t)
    door_b = planks(r_for("bdb"), (54, 46, 48))
    door_b.set(12, 2, GILT)
    door_b.set(12, 3, GILT)
    out("blotwood_door_bottom", door_b)
    trap = planks(r_for("btr"), (54, 46, 48))
    trap.rect(3, 3, 13, 13, (40, 34, 36))
    trap.rect(4, 4, 12, 12, (60, 52, 54))
    out("blotwood_trapdoor", trap)

    leaves = Canvas(16, 16)
    rl = r_for("leaves")
    for _ in range(40):
        cx, cy = rl.randint(0, 15), rl.randint(0, 15)
        leaves.circle(cx, cy, rl.uniform(1, 2.2), jitter((22, 20, 28), 6, rl))
    for _ in range(3):
        x = rl.randrange(16)
        for y in range(rl.randint(12, 15), 16):
            leaves.set(x, y, (10, 10, 12))
    out("blotwood_leaves", leaves)

    sap = Canvas(16, 16)
    for y in range(8, 16):
        sap.set(7, y, (40, 34, 36))
    sap.circle(7.5, 6, 3.2, (20, 18, 26))
    sap.circle(6, 5, 1.5, (34, 32, 42))
    out("blotwood_sapling", sap)

    rb = r_for("berry")
    for stage in range(4):
        c = Canvas(16, 16)
        for _ in range(6 + stage * 3):
            x, y = rb.randint(2, 13), rb.randint(4 + (3 - stage) * 2, 15)
            c.line(x, 15, x + rb.choice((-2, -1, 1, 2)), y, jitter((40, 46, 40), 8, rb))
        if stage >= 2:
            for _ in range(3 + (stage - 2) * 4):
                x, y = rb.randint(3, 12), rb.randint(5, 12)
                c.set(x, y, (20, 18, 40) if stage == 3 else (70, 50, 80))
                c.set(x + 1, y, (40, 36, 70) if stage == 3 else (90, 70, 100))
        out(f"blotberry_bush_stage{stage}", c)

    out("erased_grass", grass_blades(r_for("erased"), (224, 222, 214), 10))

    rsc = r_for("scrawl")
    for v in range(4):
        c = Canvas(16, 16)
        scribble(c, 1, 1, 15, 15, (20, 18, 24) if v != 2 else RUBRIC, rsc, alpha=230, density=0.55, line_gap=3)
        if v == 3:
            for i in range(16):
                c.set(i, i, (20, 18, 24))
                c.set(15 - i, i, (20, 18, 24))
        out(f"scrawl_{v}", c)

    thread = Canvas(16, 16)
    for i in range(16):
        thread.set(i, i, RUBRIC)
        thread.set(15 - i, i, RUBRIC)
        thread.set(i, 8, RUBRIC_BRIGHT)
        thread.set(8, i, RUBRIC_BRIGHT)
    for a in range(24):
        t = a / 24 * math.tau
        thread.set(int(7.5 + math.cos(t) * 5), int(7.5 + math.sin(t) * 5), RUBRIC)
    out("binding_thread", thread)

    lant = Canvas(16, 16, (120, 90, 36))
    rln = r_for("lantern")
    for y in range(16):
        for x in range(16):
            lant.set(x, y, jitter(GILT, 10, rln))
    for y in range(3, 8):
        for x in range(1, 5):
            lant.set(x, y, (255, 238, 170))
    lant.rect(1, 10, 5, 14, (150, 110, 40))
    lant.rect(11, 1, 14, 3, (60, 54, 50))
    lant.rect(11, 6, 14, 8, (60, 54, 50))
    lant.rect(11, 10, 14, 12, (60, 54, 50))
    out("illumined_lantern", lant)

    door = Canvas(16, 16)
    rd = r_for("sealed")
    for y in range(16):
        for x in range(16):
            door.set(x, y, jitter(LEATHER_DARK, 8, rd))
    for x in range(1, 15, 2):
        door.set(x, 7, RUBRIC_BRIGHT)
        door.set(x + 1, 8, RUBRIC_BRIGHT)
    for y in range(1, 15, 2):
        door.set(7, y, RUBRIC)
    out("sealed_door", door)

    stand = base_noise(r_for("fstand"), (220, 214, 198), 5, 4)
    stand.rect(2, 2, 14, 14, VELLUM_LIGHT)
    scribble(stand, 3, 3, 13, 13, INK, r_for("fstand_s"), density=0.5, line_gap=2)
    stand.rect(3, 12, 13, 13, RUBRIC)
    out("folio_stand_top", stand)
    out("folio_stand_side", base_noise(r_for("fstand_side"), (210, 204, 188), 6, 4))

    for lit in (False, True):
        p = base_noise(r_for("pillar"), (222, 218, 206), 5, 5)
        for y in range(0, 16, 4):
            p.rect(0, y, 16, y + 1, (196, 190, 176))
        if lit:
            draw_small(p, 6, 6, 6, RUBRIC_BRIGHT)
            for x in range(16):
                p.set(x, 0, RUBRIC)
                p.set(x, 15, RUBRIC)
        else:
            draw_small(p, 6, 6, 6, (206, 200, 188))
        out("rubric_pillar" + ("_lit" if lit else ""), p)

    tome = Canvas(16, 16)
    rtm = r_for("tome")
    tome.rect(0, 0, 16, 16, LEATHER)
    for y in range(16):
        for x in range(16):
            tome.set(x, y, jitter(LEATHER, 8, rtm))
    tome.rect(0, 5, 16, 11, VELLUM_DARK)
    for x in range(0, 16, 2):
        tome.set(x, 5, RUBRIC)
        tome.set(x + 1, 10, RUBRIC)
    for y in range(16):
        tome.set(7, y, RUBRIC_BRIGHT)
    out("stitched_tome", tome)

    blank = Canvas(16, 64)
    rbk = r_for("blank")
    for f in range(4):
        for y in range(16):
            for x in range(16):
                v = rbk.randint(214, 250)
                blank.set(x, f * 16 + y, (v, v, v - 4), 235)
    out("blank", blank, {"animation": {"frametime": 2}})
