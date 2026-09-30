"""
Detailed designs for the four "rule" creatures: the Knocker, the Longhand, the Copyist and the
Erratum. Same conventions as gen_models.py (pixel units, y down, feet at y = 24, -z is the
front); iterate on them with tools/preview_models.py.
"""
import math

from modelkit import B, P, Model, E
from pixel import jitter, mix, shade


# ================================================================ painting helpers
def _hash(*xs):
    h = 2166136261
    for x in xs:
        h = ((h ^ (int(x) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    return h


def weave(base, light, dark, stains=(), stain_count=0):
    """Coarse cloth: alternating threads, a few darker runs, optional stains."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = base
                if (x + y) % 2 == 0:
                    c = mix(c, light, 0.35)
                if _hash(u + x, 7) % 5 == 0:
                    c = mix(c, dark, 0.45)
                if _hash(v + y, 13) % 7 == 0:
                    c = mix(c, dark, 0.25)
                canvas.set(u + x, v + y, jitter(c, 5, r))
        for _ in range(stain_count):
            sc = r.choice(stains)
            cx, cy = r.uniform(0, w), r.uniform(0, h)
            rad = r.uniform(0.8, 2.2)
            for y in range(h):
                for x in range(w):
                    d = math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 0.7)
                    if d < rad:
                        canvas.blend(u + x, v + y, sc, 0.55 * (1 - d / rad) + 0.25)
    return fn


def skin(base, dark, light, veins=None, mottle=0.25):
    """Pallid skin: soft mottling and a few thin veins."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = base
                n = (_hash(u + x, v + y) % 100) / 100.0
                if n < mottle * 0.5:
                    c = mix(c, dark, 0.35)
                elif n > 1 - mottle * 0.4:
                    c = mix(c, light, 0.35)
                canvas.set(u + x, v + y, jitter(c, 4, r))
        if veins:
            for _ in range(max(1, (w * h) // 40)):
                x = r.randrange(w)
                y = r.randrange(h)
                for _ in range(r.randint(2, 4)):
                    if 0 <= x < w and 0 <= y < h:
                        canvas.blend(u + x, v + y, veins, 0.5)
                    y += 1
                    x += r.choice((-1, 0, 0, 1))
    return fn


def ragged(bottom=3, holes=0, sides=False, seed=0):
    """A cut-out function for cloth: a torn hem, a few moth holes, optionally frayed sides."""
    def shape(name, x, y, w, h):
        if name not in ("front", "back"):
            return 255
        k = _hash(x, seed)
        if y >= h - 1 - (k % (bottom + 1)):
            return 0
        if sides and (x == 0 or x == w - 1) and _hash(y, seed, x) % 3 == 0:
            return 0
        if holes and _hash(x, y, seed) % 97 < holes:
            return 0
        return 255
    return shape


def gloss(base, sheen, streak_col=None, script=0.0):
    """Wet ink: near-black with a soft highlight running down one edge, optionally with faint
    handwriting showing in the surface."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                canvas.set(u + x, v + y, jitter(base, 3, r))
        if w >= 2:
            col = streak_col if streak_col is not None else w - 2
            for y in range(h):
                canvas.blend(u + col, v + y, sheen, 0.4 if _hash(y, u) % 4 else 0.22)
        else:
            for y in range(1, h, 4):
                canvas.blend(u, v + y, sheen, 0.22)
        if script and w >= 3 and h >= 3:
            for y in range(1, h - 1, 2):
                x = 0
                while x < w:
                    if _hash(u + x, v + y, 3) % 100 < script * 100:
                        canvas.blend(u + x, v + y, sheen, 0.3)
                    x += 1
    return fn


# ================================================================ the Knocker
K_SKIN = (136, 134, 132)
K_SKIN_D = (102, 100, 104)
K_SKIN_DD = (70, 68, 74)
K_SKIN_L = (164, 162, 156)
K_VEIN = (96, 88, 112)
K_SOCKET = (24, 20, 24)
K_PUPIL = (212, 206, 186)
K_TEAR = (36, 30, 40)
K_LIP = (88, 58, 58)
K_MOUTH = (56, 18, 22)
K_TOOTH = (208, 198, 174)
K_TOOTH_D = (150, 138, 116)
K_LINEN = (182, 170, 140)
K_LINEN_D = (148, 136, 110)
K_LINEN_L = (202, 192, 164)
K_STAIN = (120, 96, 66)
K_GRIME = (88, 78, 64)
K_IRON = (44, 43, 48)
K_IRON_L = (98, 96, 106)
K_RUST = (128, 68, 40)
K_CLAW = (92, 80, 62)
K_CLAW_L = (156, 140, 104)
K_CLAW_TIP = (214, 204, 170)
K_KNUCKLE = (160, 78, 70)
K_NAIL = (64, 58, 56)


def _k_skin(extra=None, veins=True):
    features = {"all": [("draw", skin(K_SKIN, K_SKIN_D, K_SKIN_L, veins=K_VEIN if veins else None))]}
    for k, lst in (extra or {}).items():
        features.setdefault(k, []).extend(lst)
    return dict(base=K_SKIN, noise=4, rim=(K_SKIN_DD, 0.35), features=features)


def _k_linen(shape=None, stains=3):
    d = dict(base=K_LINEN, noise=0, shade=True,
             features={"all": [("draw", weave(K_LINEN, K_LINEN_L, K_LINEN_D, (K_STAIN, K_GRIME), stains))]})
    if shape:
        d["shape"] = shape
    return d


# ================================================================ squeezing through gaps
# Postures for fitting through holes (tuned with tools/preview_models.py --fit): offsets from the
# rest pose, part -> (dxRot, dyRot, dzRot[, dx, dy, dz]), blended in by the entity's Squeeze.
# Crawls fit under one block (and within one block's width), stoops under two.
SQUEEZE_VARS = ("crawl", "entity.squeeze.crawl(ageInTicks - entity.tickCount)"), \
               ("stoop", "entity.squeeze.stoop(ageInTicks - entity.tickCount)")


def squeeze_blends(crawl, stoop):
    return [(SQUEEZE_VARS[0][0], SQUEEZE_VARS[0][1], crawl), (SQUEEZE_VARS[1][0], SQUEEZE_VARS[1][1], stoop)]


KNOCKER_CRAWL = {
    "hips": (1.17, 0.0, 0.0, 0.0, 18.5, 12.0),
    "left_thigh": (0.36, 0.0, -0.25),
    "right_thigh": (0.36, 0.0, 0.25),
    "left_shin": (0.6, 0.0, 0.0),
    "right_shin": (0.6, 0.0, 0.0),
    "left_foot": (-0.9, 0.0, 0.0),
    "right_foot": (-0.9, 0.0, 0.0),
    "neck": (-1.03, 0.0, 0.0),
    "head": (-0.12, 0.0, 0.0),
    "left_upper_arm": (-2.45, 0.0, -0.12),
    "right_upper_arm": (-2.68, 0.0, 0.2),
    "left_forearm": (-0.35, 0.0, 0.0),
    "right_forearm": (0.06, 0.0, 0.0),
    "left_hand": (0.2, 0.0, 0.0),
    "right_hand": (-0.1, 0.0, 0.0),
    "shroud_back": (0.0, 0.0, 0.0),
    "shroud_back_lower": (0.39, 0.0, 0.0),
    "shroud_flap": (0.34, 0.0, 0.0),
    "shroud_strip": (0.3, 0.0, 0.0),
}
KNOCKER_STOOP = {
    "hips": (0.8, 0.0, 0.0, 0.0, 7.0, 9.0),
    "left_thigh": (-1.62, 0.0, -0.08),
    "right_thigh": (-1.62, 0.0, 0.08),
    "left_shin": (1.3, 0.0, 0.0),
    "right_shin": (1.3, 0.0, 0.0),
    "left_foot": (-0.62, 0.0, 0.0),
    "right_foot": (-0.62, 0.0, 0.0),
    "neck": (-0.4, 0.0, 0.0),
    "head": (-0.25, 0.0, 0.0),
    "left_upper_arm": (-1.5, 0.0, -0.1),
    "right_upper_arm": (-1.5, 0.0, 0.1),
    "left_forearm": (1.45, 0.0, 0.0),
    "right_forearm": (1.45, 0.0, 0.0),
    "left_hand": (1.1, 0.0, 0.0),
    "right_hand": (1.1, 0.0, 0.0),
    "shroud_back_lower": (-0.51, 0.0, 0.0),
    "shroud_flap": (-0.35, 0.0, 0.0),
    "shroud_strip": (-0.6, 0.0, 0.0),
}
LONGHAND_CRAWL = {
    "pelvis": (1.47, 0.0, 0.0, 0.0, 22.5, 2.0),
    "left_thigh": (0.0, 0.0, -0.12),
    "right_thigh": (0.0, 0.0, 0.12),
    "left_shin": (0.1, 0.0, 0.0),
    "right_shin": (0.1, 0.0, 0.0),
    "neck": (-0.34, 0.0, 0.0),
    "head": (-0.52, 0.0, 0.0),
    "left_upper_arm": (-3.64, 0.0, -0.25),
    "right_upper_arm": (-3.5, 0.0, 0.25),
    "left_forearm": (3.1, 0.0, 0.0),
    "right_forearm": (3.01, 0.0, 0.0),
    "left_hand": (-3.0, 0.0, 0.0),
    "right_hand": (-3.0, 0.0, 0.0),
    "tail_0": (-0.3, 0.0, 0.0),
    "tail_1": (0.15, 0.0, 0.0),
    "tail_2": (0.1, 0.0, 0.0),
}
LONGHAND_STOOP = {
    "pelvis": (2.35, 0.0, 0.0, 0.0, 0.0, 12.0),
    "left_thigh": (-2.35, 0.0, 0.0),
    "right_thigh": (-2.35, 0.0, 0.0),
    "neck": (0.23, 0.0, 0.0),
    "head": (-2.42, 0.0, 0.0),
    "left_upper_arm": (-2.47, 0.0, -0.15),
    "right_upper_arm": (-2.47, 0.0, 0.15),
    "left_forearm": (-1.49, 0.0, 0.0),
    "right_forearm": (-1.49, 0.0, 0.0),
    "left_hand": (-0.2, 0.0, 0.0),
    "right_hand": (-0.2, 0.0, 0.0),
    "tail_0": (-2.59, 0.0, 0.0),
    "tail_1": (-1.6, 0.0, 0.0),
    "tail_2": (0.3, 0.0, 0.0),
}
COPYIST_CRAWL = {
    "pelvis": (0.77, 0.0, 0.0, 0.0, 11.0, 11.0),
    "left_thigh": (0.8, 0.0, -0.3),
    "right_thigh": (0.8, 0.0, 0.3),
    "left_shin": (0.3, 0.0, 0.0),
    "right_shin": (0.3, 0.0, 0.0),
    "left_hoof": (0.1, 0.0, 0.0),
    "right_hoof": (0.1, 0.0, 0.0),
    "hide_rump": (0.3, 0.0, 0.0),
    "neck": (0.28, 0.0, 0.0),
    "head": (-1.2, 0.0, 0.0),
    "left_upper_arm": (-1.92, 0.0, -0.2),
    "right_upper_arm": (-1.92, 0.0, 0.2),
    "left_forearm": (-0.42, 0.0, 0.0),
    "right_forearm": (-0.42, 0.0, 0.0),
    "left_hand": (0.37, 0.0, 0.0),
    "right_hand": (0.37, 0.0, 0.0),
    "hide_foreleg_l": (0.65, 0.0, 0.0),
    "hide_foreleg_r": (0.65, 0.0, 0.0),
}
COPYIST_STOOP = {
    "pelvis": (0.35, 0.0, 0.0, 0.0, 2.0, 5.0),
    "left_thigh": (-0.65, 0.0, 0.0),
    "right_thigh": (-0.65, 0.0, 0.0),
    "left_shin": (0.7, 0.0, 0.0),
    "right_shin": (0.7, 0.0, 0.0),
    "left_hoof": (-0.35, 0.0, 0.0),
    "right_hoof": (-0.35, 0.0, 0.0),
    "neck": (0.5, 0.0, 0.0),
    "head": (-0.6, 0.0, 0.0),
    "left_upper_arm": (-0.3, 0.0, 0.0),
    "right_upper_arm": (-0.3, 0.0, 0.0),
    "left_forearm": (0.6, 0.0, 0.0),
    "right_forearm": (0.6, 0.0, 0.0),
    "left_hand": (1.2, 0.0, 0.0),
    "right_hand": (1.2, 0.0, 0.0),
}


def knocker():
    """
    Taller than a door, and it has to stoop to listen at one. A burial shroud hangs off its
    shoulders, torn open down one side; an iron door-knocker ring is threaded through its lower
    lip. The knuckles of its knocking hand are worn raw. The band of linen that was tied over its
    head to keep its mouth shut has torn through on one side, and the jaw hangs.
    """
    band = {"b": K_LINEN, "B": K_LINEN_D, "s": K_STAIN}
    face = _k_skin({
        "front": [("ascii", [
            "dddddd",
            "lDllDl",
            "lSllSl",
            "lPllSl",
            "lTllPl",
            "cTnnTc",
            "dTllcd",
        ], {"d": K_SKIN_D, "l": K_SKIN_L, "D": K_SKIN_DD, "S": K_SOCKET, "P": (K_PUPIL, "glow"),
            "T": K_TEAR, "n": K_SKIN_DD, "c": K_SKIN_D})],
        "top": [("ascii", ["......", "......", "bbBbbb", "BbsbBb", "......", "......"], band)],
        # Tied over the head and under the chin on one side; torn and frayed on the other.
        "left": [("ascii", ["..bb..", "..bB..", "..sb..", "..bb..", "..Bb..", "..bb..", "..bB.."], band)],
        "right": [("ascii", ["..bb..", "..Bb..", "..b...", "...B..", "......", "......", "......"], band)],
        "back": [("ascii", ["......", "....s.", "......", "......"], band)],
        "bottom": [("ascii", ["mmmmmm", "mmmmmm", "mmmmmm", "mmmmmm", "mmmmmm", "tTtTtT"],
                    {"m": K_MOUTH, "t": K_TOOTH, "T": K_TOOTH_D})],
    })
    jaw = _k_skin({
        "front": [("ascii", ["LLoLL", "cllc."], {"L": K_LIP, "o": K_IRON, "c": K_SKIN_D, "l": K_SKIN_L})],
        "left": [("ascii", [".bb..", ".bB.."], band)],
        "bottom": [("ascii", ["", "", "Bbbbb", "bbbbB", ""], band)],
        "top": [("ascii", ["mmmmm", "mmmmm", "mmmmm", "mmmmm", "tTtTt"], {"m": K_MOUTH, "t": K_TOOTH, "T": K_TOOTH_D})],
    })
    ear = _k_skin({"right": [("ascii", ["..", ".D", ".."], {"D": K_SKIN_DD})],
                   "left": [("ascii", ["..", "D.", ".."], {"D": K_SKIN_DD})]}, veins=False)
    chest = _k_skin({
        "front": [("ascii", [
            "SkkkkkS",
            "s.sss.s",
            "rrs.srr",
            "..s.s..",
            "rrs.srr",
            ".ss.ss.",
            "rrs.srr",
            "ssbbbss",
        ], {"S": K_SKIN_D, "k": K_SKIN_L, "s": K_SKIN, ".": (92, 90, 96), "r": (176, 174, 168), "b": K_SKIN_D})],
        "back": [("ascii", [".s.", ".S.", ".s.", ".S.", ".s.", ".S.", ".s.", ".S."],
                  {"s": K_SKIN_L, "S": K_SKIN_D}, (2, 0))],
    })
    waist = _k_skin({"front": [("ascii", ["s.ss.", ".sss.", "s.s.s", "ss.ss"],
                                         {"s": K_SKIN, ".": (104, 96, 92)}, (0, 1))]})
    wrap = dict(base=K_LINEN, noise=0, features={"all": [
        ("draw", weave(K_LINEN, K_LINEN_L, K_LINEN_D, (K_STAIN, K_GRIME), 1)),
        ("hstripes", 3, K_LINEN_D)]})
    hand = _k_skin(veins=False)
    knock_hand = _k_skin({"front": [("ascii", ["KKK", "k.k"], {"K": K_KNUCKLE, "k": (130, 70, 62), ".": K_SKIN_D})],
                                   "bottom": [("ascii", ["KKK", "KKK"], {"K": K_KNUCKLE})]})
    finger = _k_skin({"all": [("ascii", ["", "", "", "", "", "N"], {"N": K_NAIL})],
                               "bottom": [("ascii", ["N"], {"N": K_NAIL})]})
    # Horn-dark at the root, yellowing to a pale point.
    claw = dict(base=K_CLAW, noise=4, features={"all": [("ascii", ["", "c", "c", "C", "C"], {"c": K_CLAW_L, "C": K_CLAW_TIP})]})
    iron = dict(base=K_IRON, noise=5, shade=False, features={"front": [("px", [(0, 0, K_IRON_L)])],
                                                             "top": [("px", [(0, 0, K_IRON_L), (-1, 0, (70, 60, 56))])]})
    foot = _k_skin({"top": [("ascii", ["..", "d.", ".d", "d.", "..", "NN"], {"d": K_SKIN_D, "N": K_NAIL})],
                             "front": [("ascii", ["NN"], {"N": K_NAIL})]})

    def arm(side, sx, knocking):
        s = side
        m = sx < 0
        fingers = []
        # Every finger ends in a long, hooked, yellowed claw.
        for i, fx in enumerate((-1.0, 0.0, 1.0)):
            fingers.append(P(f"{s}_finger_{i}", (fx, 3, -0.3), rot=(0, 0, (i - 1) * 0.06 * -sx),
                             boxes=[B(-0.5, 0, -0.5, 1, 6, 1, mirror=m)], paint=finger, children=[
                                 P(f"{s}_claw_{i}", (0, 5.8, -0.1), rot=(-0.75, 0, 0),
                                   boxes=[B(-0.5, 0, -0.5, 1, 5, 1, mirror=m, inflate=-0.1)], paint=claw)]))
        fingers.append(P(f"{s}_thumb", (1.8 * -sx, 1, -0.6), rot=(-0.3, 0, 0.5 * sx),
                         boxes=[B(-0.5, 0, -0.5, 1, 3, 1, mirror=m)], paint=finger, children=[
                             P(f"{s}_thumb_claw", (0, 3, -0.1), rot=(-0.5, 0, 0),
                               boxes=[B(-0.5, 0, -0.5, 1, 3, 1, mirror=m, inflate=-0.1)], paint=claw)]))
        return P(f"{s}_upper_arm", (4.3 * sx, -7.2, 0), rot=(-0.34, 0, -0.07 * sx),
                 boxes=[B(-1, -1, -1, 2, 13, 2, mirror=m)], paint=wrap, children=[
                     P(f"{s}_forearm", (0, 12, 0), rot=(-0.28, 0, 0),
                       boxes=[B(-1, 0, -1, 2, 12, 2, mirror=m)], paint=_k_skin(), children=[
                           P(f"{s}_hand", (0, 12, 0), rot=(0.1, 0, 0),
                             boxes=[B(-1.5, 0, -1, 3, 3, 2, mirror=m)], paint=knock_hand if knocking else hand,
                             children=fingers)])])

    def leg(side, sx):
        s = side
        m = sx < 0
        return P(f"{s}_thigh", (1.6 * sx, 1, 0), rot=(-0.16, 0, 0.02 * sx),
                 boxes=[B(-1, 0, -1, 2, 11, 2, mirror=m)], paint=_k_skin(), children=[
                     P(f"{s}_shin", (0, 11, 0), rot=(0.34, 0, 0),
                       boxes=[B(-1, 0, -1, 2, 9, 2, mirror=m)], paint=_k_skin(), children=[
                           P(f"{s}_foot", (0, 9, 0), rot=(-0.18, 0, 0),
                             boxes=[B(-1.2, 0, -4.2, 2.4, 1, 5, mirror=m)], paint=foot)])])

    ring = P("ring", (0, 2, -4.5), boxes=[
        B(-1, 0, -0.5, 2, 1, 1), B(-2, 1, -0.5, 1, 2, 1), B(1, 1, -0.5, 1, 2, 1), B(-1, 3, -0.5, 2, 1, 1)], paint=iron)
    head = P("head", (0, -4, 0), rot=(-0.52, 0, 0.26), boxes=[B(-3, -9, -3, 6, 7, 6)], paint=face, children=[
        P("jaw", (0, -2, 2), rot=(0.06, 0, 0), boxes=[B(-2.5, 0, -5, 5, 2, 5)], paint=jaw, children=[ring]),
        P("left_ear", (3, -5.5, 0.5), rot=(0, -0.35, 0.1), boxes=[B(0, -1.5, -1, 1, 3, 2)], paint=ear),
        P("right_ear", (-3, -5.5, 0.5), rot=(0, 0.35, -0.1), boxes=[B(-1, -1.5, -1, 1, 3, 2, mirror=True)], paint=ear),
        P("binding_knot", (2.2, -9, 0), rot=(0, 0.3, 0.2), boxes=[B(-1, -1, -1, 2, 1, 2)], paint=_k_linen(stains=1)),
        P("binding_end", (-3.02, -6, 0.2), rot=(0.15, 0, 0.1), boxes=[B(0, 0, -1, 0, 7, 2)],
          paint=_k_linen(ragged(3, seed=21), stains=1)),
        P("wisp_0", (-1.5, -8.8, 2.9), rot=(0.25, 0, 0.1), boxes=[B(-0.5, 0, 0, 1, 7, 0)],
          paint=dict(base=(62, 58, 58), noise=6, shape=ragged(2, seed=11))),
        P("wisp_1", (1.8, -8.6, 2.8), rot=(0.18, 0, -0.14), boxes=[B(-0.5, 0, 0, 1, 9, 0)],
          paint=dict(base=(70, 64, 62), noise=6, shape=ragged(3, seed=12))),
    ])
    # The shroud lies along its stooped back, then hangs straight down from its hips.
    shroud_back = P("shroud_back", (0, -8, 2.25), rot=(-0.03, 0, 0), boxes=[B(-4.5, 0, 0, 9, 9, 0)],
                    paint=_k_linen(stains=3), children=[
                        P("shroud_back_lower", (0, 9, 0), rot=(-0.36, 0, 0), boxes=[B(-4.5, 0, 0, 9, 16, 0)],
                          paint=_k_linen(ragged(4, holes=4, sides=True, seed=3), stains=5))])
    shroud_flap = P("shroud_flap", (-0.5, -8, -2.3), rot=(-0.34, 0, 0.04), boxes=[B(-4, 0, 0, 4, 22, 0)],
                    paint=_k_linen(ragged(5, holes=3, sides=True, seed=5), stains=3))
    shroud_strip = P("shroud_strip", (3.2, -7.5, -2.25), rot=(-0.3, 0, -0.05), boxes=[B(-1, 0, 0, 2, 13, 0)],
                     paint=_k_linen(ragged(4, holes=2, seed=9), stains=1))
    parts = [
        P("hips", (0, 2, 0), boxes=[B(-2.5, -1, -1.5, 5, 3, 3)], paint=_k_skin(), children=[
            leg("left", 1), leg("right", -1),
            P("waist", (0, -1, 0), rot=(0.16, 0, 0), boxes=[B(-2, -6, -1.5, 4, 6, 3)], paint=waist, children=[
                P("chest", (0, -6, 0), rot=(0.24, 0, 0), boxes=[B(-3.5, -8, -2, 7, 8, 4)], paint=chest, children=[
                    P("neck", (0, -8, -0.6), rot=(0.66, 0, 0), boxes=[B(-1, -4, -1, 2, 4, 2)], paint=_k_skin(), children=[head]),
                    arm("left", 1, False), arm("right", -1, True),
                    shroud_back, shroud_flap, shroud_strip,
                ]),
            ]),
        ]),
    ]
    K = "com.exonoxic.palimpsest.entity.KnockerEntity"
    anim = """        float breath = Mth.sin(ageInTicks * 0.07F);
        float pt = ageInTicks - entity.tickCount;
        int state = entity.getState();
        // Every state eases in and out rather than snapping.
        float knocking = Anim.ease(entity, 0, state == $K.KNOCKING, ageInTicks, 0.18F);
        float lunging = Anim.ease(entity, 1, state == $K.LUNGE, ageInTicks, 0.3F);
        float searching = Anim.ease(entity, 2, state == $K.SEARCHING, ageInTicks, 0.1F);
        float leaving = Anim.ease(entity, 3, state == $K.LEAVING, ageInTicks, 0.1F);
        float idle = Math.max(0.0F, 1.0F - knocking - lunging - searching - leaving);
        // Bent double or crawling, its body keeps that shape; only the jaw and hands still act.
        float upright = 1.0F - Math.max(crawl, stoop);

        // ---- gestures: each starts a few ticks before its sound (KnockerEntity.GESTURE_LEAD), so the
        // arm is drawn back in time for the blows to land on the hits in the recording
        final float[] COCK = {0.264F, -0.106F, 0.242F, -2.120F, -2.469F};
        final float[] HIT = {0.084F, -0.182F, 0.301F, -1.578F, -1.242F};
        final float[] SCRATCH_TOP = {-0.736F, 1.014F, 0.637F, -0.830F, -2.270F};
        final float[] SCRATCH_END = {-0.032F, -0.204F, 0.766F, -0.452F, -2.464F};
        final float[] S_COCK = {0.126F, 0.705F, -0.306F, -3.751F, -0.609F};
        final float[] S_HIT = {0.207F, 0.689F, 0.164F, -3.154F, -0.686F};
        final float[] S_SCRATCH_TOP = {-0.370F, 0.719F, -0.230F, -3.584F, 0.002F};
        final float[] S_SCRATCH_END = {0.516F, 0.475F, 0.494F, -3.699F, 0.623F};
        int kind = entity.gestureKind();
        float gt = entity.gestureTime(pt);
        float reach = 0.0F;   // how far the right arm is taken over by a gesture (0..1)
        float hit = 0.0F;     // 0 cocked back, 1 knuckles on the wood (or 0 top, 1 bottom of a scratch)
        boolean scratching = false;
        if (kind == $K.GESTURE_KNOCK || kind == $K.GESTURE_POUND) {
            // event.knock holds three knocks 17 ticks apart (further apart at a lower pitch).
            float gap = kind == $K.GESTURE_POUND ? 17.0F / $K.POUND_PITCH : 17.0F;
            float t = gt - $K.GESTURE_LEAD;
            reach = Anim.keys(gt, 0.0F, 0.0F, 3.0F, 1.0F, gap * 2.0F + 12.0F, 1.0F, gap * 2.0F + 22.0F, 0.0F);
            hit = 0.3F + Anim.knock(t) + Anim.knock(t - gap) + Anim.knock(t - gap * 2.0F);
        } else if (kind == $K.GESTURE_TAP) {
            // Quick raps of the knuckles, 6 and 11 ticks after the first.
            float t = gt - $K.GESTURE_LEAD;
            reach = Anim.keys(gt, 0.0F, 0.0F, 3.0F, 1.0F, 18.0F, 1.0F, 28.0F, 0.0F);
            hit = 0.3F + 0.6F * (Anim.knock(t) + Anim.knock(t - 6.0F) + Anim.knock(t - 11.0F));
        } else if (kind == $K.GESTURE_GLASS_TAP) {
            // Slow taps on the glass, 18 and 38 ticks after the first.
            float t = gt - $K.GESTURE_LEAD;
            reach = Anim.keys(gt, 0.0F, 0.0F, 3.0F, 1.0F, 48.0F, 1.0F, 60.0F, 0.0F);
            hit = 0.35F + 0.55F * (Anim.knock(t) + Anim.knock(t - 18.0F) + Anim.knock(t - 38.0F));
        } else if (kind == $K.GESTURE_SCRATCH) {
            // Up to the glass or the boards, then the nails dragged all the way down, slowly.
            scratching = true;
            reach = Anim.keys(gt, 0.0F, 0.0F, 4.0F, 1.0F, 86.0F, 1.0F, 98.0F, 0.0F);
            hit = Anim.keys(gt, 4.0F, 0.0F, 84.0F, 1.0F) + Mth.sin(gt * 2.1F) * 0.012F * reach;
        }
        // While it knocks the hand stays up at the door between rounds.
        if (!scratching && knocking > reach) {
            if (reach <= 0.0F) hit = 0.3F + Mth.sin(ageInTicks * 0.05F) * 0.04F;
            reach = knocking;
        }
        reach *= 1.0F - crawl;
        float armFree = 1.0F - reach;

        // ---- walking, crawling
        float a = Math.min(1.0F, limbSwingAmount) * (1.0F - 0.45F * crawl);
        float w = limbSwing * 0.42F;
        chest.xRot += breath * 0.03F;
        neck.xRot += breath * 0.025F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.65F;
        neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.6F;
        // It is always listening: the head drifts, tilting an ear towards whatever it heard.
        head.zRot += Mth.sin(ageInTicks * 0.021F) * 0.14F;
        leftEar.yRot += Mth.sin(ageInTicks * 0.13F) * 0.08F;
        rightEar.yRot -= Mth.sin(ageInTicks * 0.13F + 1.3F) * 0.08F;
        // Long, slow strides; knees fold on the back swing; the whole body bobs and rolls.
        leftThigh.xRot += Mth.cos(w) * 0.85F * a;
        rightThigh.xRot += Mth.cos(w + Mth.PI) * 0.85F * a;
        leftShin.xRot += Math.max(0.0F, Mth.sin(w)) * 1.1F * a;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(w)) * 1.1F * a;
        hips.y -= Mth.abs(Mth.cos(w)) * 0.9F * a;
        hips.zRot += Mth.cos(w) * 0.05F * a;
        chest.zRot -= Mth.cos(w) * 0.04F * a;
        leftUpperArm.xRot += Mth.cos(w + Mth.PI) * 0.45F * a;
        rightUpperArm.xRot += Mth.cos(w) * 0.45F * a * armFree;
        leftForearm.xRot -= Math.max(0.0F, Mth.cos(w)) * 0.35F * a;
        rightForearm.xRot -= Math.max(0.0F, -Mth.cos(w)) * 0.35F * a * armFree;

        // ---- the right arm, given over to a gesture
        if (reach > 0.0F) {
            float[] from = scratching ? SCRATCH_TOP : COCK;
            float[] to = scratching ? SCRATCH_END : HIT;
            float[] sFrom = scratching ? S_SCRATCH_TOP : S_COCK;
            float[] sTo = scratching ? S_SCRATCH_END : S_HIT;
            float s = stoop;
            rightUpperArm.xRot += reach * Mth.lerp(s, Mth.lerp(hit, from[0], to[0]), Mth.lerp(hit, sFrom[0], sTo[0]));
            rightUpperArm.yRot += reach * Mth.lerp(s, Mth.lerp(hit, from[1], to[1]), Mth.lerp(hit, sFrom[1], sTo[1]));
            rightUpperArm.zRot += reach * Mth.lerp(s, Mth.lerp(hit, from[2], to[2]), Mth.lerp(hit, sFrom[2], sTo[2]));
            rightForearm.xRot += reach * Mth.lerp(s, Mth.lerp(hit, from[3], to[3]), Mth.lerp(hit, sFrom[3], sTo[3]));
            rightHand.xRot += reach * Mth.lerp(s, Mth.lerp(hit, from[4], to[4]), Mth.lerp(hit, sFrom[4], sTo[4]));
            // A fist to knock with, fingers crooked like hooks to scratch with.
            float curl = scratching ? 0.35F : 1.4F;
            rightFinger0.xRot += reach * curl;
            rightFinger1.xRot += reach * curl;
            rightFinger2.xRot += reach * curl;
            rightThumb.xRot += reach * (scratching ? 0.2F : 0.6F);
            // Its head goes close to the door, an ear to the wood; each blow runs through its body.
            float jolt = scratching ? 0.0F : Math.max(0.0F, hit - 0.6F) * (kind == $K.GESTURE_POUND ? 0.5F : 0.25F);
            head.zRot += reach * 0.38F * upright;
            neck.xRot += reach * (0.18F + jolt * 0.3F) * upright;
            chest.xRot += reach * jolt * 0.3F;
            head.xRot -= reach * jolt * 0.2F;
            jaw.xRot += reach * (scratching ? 0.25F + hit * 0.15F : 0.06F);
        }
        if (knocking > 0.0F && reach < knocking) jaw.xRot += 0.08F * knocking;

        // ---- cloth and iron move a beat behind the body
        shroudBack.xRot += -Mth.abs(Mth.sin(w)) * 0.1F * a + breath * 0.02F;
        shroudBackLower.xRot += -Mth.abs(Mth.sin(w + 0.4F)) * 0.25F * a + breath * 0.02F;
        shroudFlap.xRot += -Mth.abs(Mth.sin(w + 0.6F)) * 0.18F * a;
        shroudStrip.xRot += -Mth.abs(Mth.sin(w + 1.1F)) * 0.25F * a;
        shroudStrip.zRot += Mth.sin(ageInTicks * 0.05F) * 0.05F;
        ring.xRot += -Mth.sin(w * 2.0F) * 0.3F * a + Mth.sin(ageInTicks * 0.06F) * 0.1F;
        float flex = Mth.sin(ageInTicks * 0.09F) * (1.0F - reach);
        leftFinger0.xRot += Mth.sin(ageInTicks * 0.09F) * 0.12F;
        leftFinger1.xRot += Mth.sin(ageInTicks * 0.09F + 0.8F) * 0.12F;
        leftFinger2.xRot += Mth.sin(ageInTicks * 0.09F + 1.6F) * 0.12F;
        rightFinger0.xRot -= flex * 0.1F;
        rightFinger2.xRot -= Mth.sin(ageInTicks * 0.09F + 1.2F) * 0.1F * (1.0F - reach);

        // ---- states
        if (lunging > 0.0F) {
            float shake = Mth.sin(ageInTicks * 1.7F) * 0.05F;
            jaw.xRot += (0.95F + shake) * lunging;
            ring.xRot += 0.6F * lunging;
            float u = lunging * upright;
            chest.xRot += 0.18F * u;
            neck.xRot -= 0.25F * u;
            head.xRot -= 0.25F * u;
            leftUpperArm.xRot += (-1.35F + shake) * u;
            rightUpperArm.xRot += (-1.35F - shake) * u * armFree;
            leftUpperArm.zRot -= 0.18F * u;
            rightUpperArm.zRot += 0.18F * u * armFree;
            leftForearm.xRot += 0.15F * u;
            rightForearm.xRot += 0.15F * u * armFree;
            leftFinger0.zRot += 0.35F * lunging;
            leftFinger2.zRot -= 0.35F * lunging;
            rightFinger0.zRot -= 0.35F * lunging;
            rightFinger2.zRot += 0.35F * lunging;
        }
        // Going round the house: hunched, head cocked, listening at the walls.
        neck.xRot += 0.28F * searching * upright;
        head.zRot += (0.4F + Mth.sin(ageInTicks * 0.05F) * 0.08F) * searching * (1.0F - reach);
        jaw.xRot += 0.12F * searching;
        neck.xRot += 0.4F * leaving * upright;
        head.xRot += 0.25F * leaving * upright;
        chest.xRot += 0.1F * leaving * upright;
        jaw.xRot += (0.05F + Math.max(0.0F, Mth.sin(ageInTicks * 0.031F)) * 0.12F) * idle;
        if (attackTime > 0.0F) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightUpperArm.xRot -= 1.1F * s;
            rightForearm.xRot -= 0.6F * s;
            jaw.xRot += 0.4F * s;
        }
""".replace("$K", K)
    return Model("knocker", "KnockerModel", E + "KnockerEntity", (128, 64), parts, anim,
                 blends=squeeze_blends(KNOCKER_CRAWL, KNOCKER_STOOP))


# ================================================================ the Longhand
L_INK = (14, 13, 18)
L_INK_2 = (24, 22, 30)
L_SHEEN = (84, 90, 132)
L_STEEL = (96, 100, 112)
L_STEEL_L = (164, 170, 184)
L_STEEL_D = (46, 46, 56)
L_EYE = (236, 234, 226)
L_SLIT = (120, 118, 116)


def _l_ink(ragged_edges=True, sheen_col=None, seed=0, script=0.0):
    d = dict(base=L_INK, noise=3, shade=True, features={"all": [("draw", gloss(L_INK, L_SHEEN, sheen_col, script))]})
    if ragged_edges:
        # Dry-brush edges: the stroke's sides break up into bristle marks.
        def shape(name, x, y, w, h):
            if name in ("front", "back") and w >= 3 and (x == 0 or x == w - 1) and _hash(y, x, seed) % 5 == 0:
                return 0
            return 255
        d["shape"] = shape
    return d


def _l_steel(extra=None):
    feats = {"all": [("draw", lambda cv, gl, u, v, w, h, r: [
        cv.set(u + x, v + y, jitter(mix(L_STEEL, L_STEEL_L, 0.35) if (x + y) % 5 == 0 else L_STEEL, 4, r))
        for y in range(h) for x in range(w)])]}
    for k, lst in (extra or {}).items():
        feats.setdefault(k, []).extend(lst)
    return dict(base=L_STEEL, noise=4, rim=(L_STEEL_D, 0.5), features=feats)


def tail(seed, stage):
    """A tapering run of ink: ragged, dripping edges that thin out further down."""
    def shape(name, x, y, w, h):
        if name not in ("front", "back"):
            return 255
        k = _hash(y, seed)
        if (x == 0 or x == w - 1) and k % (4 - stage) == 0:
            return 0
        if stage == 2 and y >= h - 1 - (_hash(x, seed) % 4):
            return 0
        return 255
    return dict(base=L_INK, noise=2, shape=shape,
                features={"all": [("draw", gloss(L_INK, L_SHEEN, 1, script=0.35 if stage == 0 else 0.0))]})


def longhand():
    """
    One long pen-stroke of wet ink, taller than a door. Its head is a steel nib, point up;
    the breather hole is its only eye. It moves only when nobody is looking, and it is always
    in a slightly different pose when you look back.
    """
    body = _l_ink(seed=1)
    limb = _l_ink(ragged_edges=False)
    # A fountain-pen nib: engraved shoulders, a blade with the breather hole for an eye and the
    # slit running up from it, the point still wet from the ink, and the ribbed feed behind.
    steel = {"k": L_STEEL_L, ".": L_STEEL, "~": L_STEEL_D, "i": L_INK, "s": L_SHEEN}
    nib_base = _l_steel({"front": [("ascii", ["k.k.k", ".~k~.", "iisii"], steel)],
                         "back": [("ascii", ["k...k", ".~.~.", "iiiii"], steel)],
                         "top": [("ascii", ["", "", ".k~k."], steel)]})
    nib_mid = _l_steel({"front": [("ascii", ["~E~", "kSk", ".S."], dict(steel, E=(L_EYE, "glow"), S=(L_SLIT, "glow")))],
                        "back": [("ascii", ["k.k", "...", ".k."], steel)]})
    nib_upper = _l_steel({"front": [("ascii", ["isi", ".S.", "kSk"], dict(steel, S=((86, 84, 86), "glow")))],
                          "back": [("ascii", ["iii", "...", "k.k"], steel)],
                          "left": [("ascii", ["i", "."], steel)], "right": [("ascii", ["i", "."], steel)]})
    nib_tip = dict(base=L_INK, noise=2, features={"all": [("draw", gloss(L_INK, L_SHEEN, 0))]})
    feed = dict(base=L_INK_2, noise=2, features={"all": [("ascii", ["~~", "..", "~~", "..", "~~"], {"~": L_INK, ".": L_INK_2})]})
    drip = dict(base=L_INK, noise=2, features={"all": [("ascii", [".", ".", "s"], {"s": L_SHEEN})]})
    finger = _l_ink(ragged_edges=False)

    def arm(side, sx):
        s = side
        m = sx < 0
        fingers = []
        for i, (fx, spread) in enumerate(((-1.0, -0.22), (-0.35, -0.07), (0.35, 0.07), (1.0, 0.22))):
            ln = 8 if i in (1, 2) else 7
            fingers.append(P(f"{s}_finger_{i}", (fx * 0.8, 2, 0), rot=(0.18, 0, spread * -sx),
                             boxes=[B(-0.5, 0, -0.5, 1, ln, 1, mirror=m)], paint=finger,
                             children=[P(f"{s}_drip_{i}", (0, ln, 0), boxes=[B(-0.5, 0, -0.5, 1, 2, 0)], paint=drip)] if i == 2 else []))
        return P(f"{s}_upper_arm", (2.8 * sx, -7.6, 0), rot=(0.02, 0, -0.06 * sx),
                 boxes=[B(-0.5, -0.5, -0.5, 1, 13, 1, mirror=m)], paint=limb, children=[
                     P(f"{s}_forearm", (0, 12.5, 0), rot=(-0.08, 0, 0.03 * sx),
                       boxes=[B(-0.5, 0, -0.5, 1, 13, 1, mirror=m)], paint=limb, children=[
                           P(f"{s}_hand", (0, 13, 0), boxes=[B(-1, 0, -0.5, 2, 2, 1, mirror=m)], paint=limb,
                             children=fingers)])])

    legs = []
    for side, sx in (("left", 1), ("right", -1)):
        m = sx < 0
        legs.append(P(f"{side}_thigh", (1.1 * sx, 0.5, 0), rot=(-0.04, 0, 0.03 * sx),
                      boxes=[B(-1, 0, -1, 2, 13, 2, mirror=m)], paint=_l_ink(seed=2 + (sx > 0)), children=[
                          P(f"{side}_shin", (0, 13, 0), rot=(0.1, 0, 0), boxes=[B(-0.5, 0, -0.5, 1, 10, 1, mirror=m)], paint=limb,
                            children=[P(f"{side}_point", (0, 10, 0), rot=(-0.06, 0, 0),
                                        boxes=[B(-0.5, 0, -1, 1, 2, 2, mirror=m)], paint=_l_steel())])]))
    head = P("head", (0, -5, 0), rot=(-0.08, 0, 0.18), boxes=[B(-2.5, -3, -1.5, 5, 3, 3)], paint=nib_base, children=[
        P("nib_mid", (0, -3, 0), boxes=[B(-1.5, -3, -1, 3, 3, 2)], paint=nib_mid),
        P("nib_upper", (0, -6, 0), boxes=[B(-1.5, -3, -0.5, 3, 3, 1)], paint=nib_upper),
        P("nib_tip", (0, -9, 0), rot=(0.05, 0, 0), boxes=[B(-0.5, -3, -0.5, 1, 3, 1)], paint=nib_tip),
        P("feed", (0, -2, 1), rot=(-0.06, 0, 0), boxes=[B(-1, -5, 0, 2, 5, 1)], paint=feed),
        P("chin_drip", (0.8, 0, -1.2), boxes=[B(-0.5, 0, 0, 1, 3, 0)], paint=drip),
    ])
    parts = [
        P("pelvis", (0, -1.5, 0), boxes=[B(-1.5, -1, -1, 3, 2, 2)], paint=body, children=legs + [
            P("abdomen", (0, -1, 0), rot=(0.02, 0, -0.07), boxes=[B(-1, -7, -1, 2, 7, 2)], paint=body, children=[
                P("ribcage", (0, -7, 0), rot=(0.08, 0, 0.1), boxes=[B(-2.5, -8, -1.5, 5, 8, 3)], paint=_l_ink(seed=4, script=0.45), children=[
                    # The tail of the stroke: ink running off its shoulders almost to the floor.
                    P("tail_0", (0, -7.8, 1.6), rot=(0.14, 0, 0), boxes=[B(-4, 0, 0, 8, 12, 0)], paint=tail(5, 0), children=[
                        P("tail_1", (0, 12, 0), rot=(0.02, 0, 0), boxes=[B(-2.5, 0, 0, 5, 12, 0)], paint=tail(6, 1), children=[
                            P("tail_2", (0, 12, 0), rot=(-0.04, 0, 0), boxes=[B(-1, 0, 0, 2, 11, 0)], paint=tail(7, 2))])]),
                    P("neck", (0, -8, 0), rot=(0.12, 0, -0.04), boxes=[B(-0.5, -5, -0.5, 1, 5, 1)], paint=limb, children=[head]),
                    arm("left", 1), arm("right", -1),
                    P("rib_drip", (-1.5, 0, -1.6), boxes=[B(-0.5, 0, 0, 1, 3, 0)], paint=drip),
                ]),
            ]),
        ]),
    ]
    anim = """        if (entity.isFrozen()) {
            // Caught. Every time you look back it is holding a different, wrong pose (or, caught
            // squeezing through something, exactly the shape it was squeezing in).
            if (crawl < 0.05F && stoop < 0.05F) switch (entity.getPoseIndex()) {
                case 0 -> {
                    leftUpperArm.xRot -= 1.35F; rightUpperArm.xRot -= 1.2F;
                    leftForearm.xRot -= 0.25F; rightForearm.xRot -= 0.35F;
                    leftFinger0.zRot += 0.5F; leftFinger3.zRot -= 0.5F; rightFinger0.zRot -= 0.5F; rightFinger3.zRot += 0.5F;
                    head.zRot += 0.45F; neck.xRot -= 0.2F;
                }
                case 1 -> {
                    leftThigh.xRot -= 0.75F; leftShin.xRot += 0.9F; rightThigh.xRot += 0.35F; rightShin.xRot += 0.25F;
                    ribcage.xRot += 0.28F; leftUpperArm.xRot += 0.55F; rightUpperArm.xRot -= 0.85F; rightForearm.xRot -= 0.4F;
                    head.yRot += 0.45F;
                }
                case 2 -> {
                    // The neck snapped sideways; the eye still on you.
                    neck.zRot += 0.55F; head.zRot += 1.3F; head.xRot -= 0.15F;
                    leftUpperArm.zRot -= 0.05F; rightUpperArm.zRot += 0.05F;
                    rightForearm.xRot -= 0.25F;
                }
                case 3 -> {
                    pelvis.y += 9.0F;
                    leftThigh.xRot -= 1.25F; rightThigh.xRot -= 1.1F; leftShin.xRot += 2.1F; rightShin.xRot += 1.95F;
                    leftPoint.xRot -= 0.9F; rightPoint.xRot -= 0.85F;
                    abdomen.xRot += 0.45F; ribcage.xRot += 0.5F; neck.xRot -= 0.6F; head.xRot -= 0.55F;
                    leftUpperArm.xRot -= 0.75F; rightUpperArm.xRot -= 0.7F; leftForearm.xRot += 0.3F; rightForearm.xRot += 0.25F;
                }
                default -> {
                    leftUpperArm.xRot -= 2.85F; rightUpperArm.xRot -= 2.75F;
                    leftUpperArm.zRot -= 0.25F; rightUpperArm.zRot += 0.25F;
                    leftForearm.xRot -= 0.7F; rightForearm.xRot -= 0.75F;
                    leftFinger1.xRot += 0.9F; leftFinger2.xRot += 0.8F; rightFinger1.xRot += 0.85F; rightFinger2.xRot += 0.9F;
                    head.xRot += 0.55F;
                }
            }
        } else {
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        limbSwingAmount *= 1.0F - 0.45F * crawl;
        // Unwatched, it moves in fast, broken strokes.
        float jerk = Mth.sin(ageInTicks * 2.3F) * 0.06F;
        float w = limbSwing * 0.5F;
        leftThigh.xRot += Mth.cos(w) * 1.0F * limbSwingAmount;
        rightThigh.xRot += Mth.cos(w + Mth.PI) * 1.0F * limbSwingAmount;
        leftShin.xRot += Math.max(0.0F, Mth.sin(w)) * 1.2F * limbSwingAmount;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(w)) * 1.2F * limbSwingAmount;
        leftUpperArm.xRot += Mth.cos(w + Mth.PI) * 0.7F * limbSwingAmount + jerk;
        rightUpperArm.xRot += Mth.cos(w) * 0.7F * limbSwingAmount - jerk;
        leftForearm.xRot -= Math.max(0.0F, Mth.cos(w)) * 0.5F * limbSwingAmount;
        rightForearm.xRot -= Math.max(0.0F, -Mth.cos(w)) * 0.5F * limbSwingAmount;
        ribcage.zRot += jerk * 0.5F;
        head.zRot += Mth.sin(ageInTicks * 0.9F) * 0.05F;
        tail0.xRot += -Mth.abs(Mth.sin(w)) * 0.35F * limbSwingAmount;
        tail1.xRot += Mth.abs(Mth.sin(w + 0.5F)) * 0.25F * limbSwingAmount;
        tail2.xRot += Mth.sin(ageInTicks * 0.2F) * 0.08F;
        for (ModelPart f : new ModelPart[]{leftFinger0, leftFinger1, leftFinger2, leftFinger3}) f.xRot += Mth.sin(ageInTicks * 0.3F) * 0.2F;
        for (ModelPart f : new ModelPart[]{rightFinger0, rightFinger1, rightFinger2, rightFinger3}) f.xRot += Mth.cos(ageInTicks * 0.3F) * 0.2F;
        }
"""
    return Model("longhand", "LonghandModel", E + "LonghandEntity", (64, 64), parts, anim,
                 blends=squeeze_blends(LONGHAND_CRAWL, LONGHAND_STOOP))


# ================================================================ the Copyist
C_SKIN = (188, 166, 158)
C_SKIN_D = (148, 124, 120)
C_SKIN_DD = (112, 90, 92)
C_SKIN_L = (210, 194, 186)
C_VEIN = (140, 108, 128)
C_TOOTH = (228, 218, 196)
C_GUM = (150, 78, 82)
C_HOOF = (56, 48, 44)
C_EYE = (214, 192, 118)
C_RAW = (166, 92, 90)
C_FAT = (214, 196, 170)


def _c_skin(extra=None, veins=True):
    features = {"all": [("draw", skin(C_SKIN, C_SKIN_D, C_SKIN_L, veins=C_VEIN if veins else None, mottle=0.3))]}
    for k, lst in (extra or {}).items():
        features.setdefault(k, []).extend(lst)
    return dict(base=C_SKIN, noise=4, rim=(C_SKIN_DD, 0.3), features=features)


def patches(base, spot, count, rmin, rmax, seed):
    """Large irregular coat patches (cow), deterministic per face."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                canvas.set(u + x, v + y, jitter(base, 5, r))
        rr = __import__("random").Random(seed * 7919 + u * 31 + v)
        for _ in range(count):
            cx, cy = rr.uniform(0, w), rr.uniform(0, h)
            rad = rr.uniform(rmin, rmax)
            for y in range(h):
                for x in range(w):
                    d = math.hypot((x + 0.5 - cx) * 0.9, y + 0.5 - cy) + (_hash(x, y, seed) % 3) * 0.35
                    if d < rad:
                        canvas.set(u + x, v + y, jitter(spot, 4, r))
    return fn


def raw_inside(seed):
    """The inside of a flayed skin: raw red with pale streaks of fat."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = C_RAW if _hash(x, y, seed) % 5 else C_FAT
                canvas.set(u + x, v + y, jitter(mix(c, C_RAW, 0.3), 6, r))
    return fn


def hide_paint(outer, seed, hem=3):
    """A skin worn as a cloak, as two layers a fraction of a pixel apart (coplanar faces with
    different textures would z-fight): [coat, raw inside]. Both share the same torn edges."""
    shape = ragged(hem, holes=2, sides=True, seed=seed)
    return [dict(base=(200, 190, 180), noise=0, shape=shape, features={"all": [("draw", outer)]}),
            dict(base=C_RAW, noise=0, shape=shape, features={"all": [("draw", raw_inside(seed))]})]


def drape_paint(outer, seed):
    """hide_paint for a piece lying flat over the shoulders: the torn hem is its front edge."""
    tear = ragged(2, holes=1, sides=True, seed=seed)
    shape = lambda name, x, y, w, h: tear("front", x, y, w, h) if name in ("top", "bottom") else 255
    return [dict(base=(200, 190, 180), noise=0, shape=shape, features={"all": [("draw", outer)]}),
            dict(base=C_RAW, noise=0, shape=shape, features={"all": [("draw", raw_inside(seed))]})]


def limb_paint(outer, tip, tip_rows=2):
    """An empty leg of skin, hanging limp, with the hoof (or wingtip) still on the end."""
    return dict(base=(200, 190, 180), noise=0, features={"all": [
        ("draw", outer), ("ascii", ["tt"] * tip_rows, {"t": tip}, (0, -tip_rows))]})


def skin_layers(x, y, w, h, outward):
    """Two boxes for a hide piece: the coat layer sits 0.15 px further out than the raw layer."""
    dz = 0.15 if outward > 0 else -0.15
    return [B(x, y, dz, w, h, 0), B(x, y, 0, w, h, 0)]


def _gone():
    """A piece this animal doesn't have: fully cut out."""
    return dict(base=(0, 0, 0), noise=0, shape=lambda *a: 0)


# Per-animal skins. Each entry: coat, mask face, snout, ears/horns, hanging legs, tail.
def _cow():
    coat = patches((226, 220, 210), (46, 38, 34), 4, 1.5, 3.2, 1)
    return {
        "hide": hide_paint(coat, 21),
        "hide_rump": hide_paint(patches((226, 220, 210), (46, 38, 34), 3, 1.5, 2.8, 7), 24, 2),
        "hide_neck": hide_paint(patches((226, 220, 210), (46, 38, 34), 1, 1.2, 2.0, 8), 25, 2),
        "hide_leg_l": hide_paint(patches((226, 220, 210), (46, 38, 34), 1, 1.0, 2.0, 2), 22, 1),
        "hide_leg_r": hide_paint(patches((226, 220, 210), (46, 38, 34), 1, 1.0, 2.0, 3), 23, 1),
        "hide_tail": dict(base=(46, 38, 34), noise=5, features={"all": [("ascii", ["", "", "", "", "t", "t"], {"t": (30, 26, 24)})]}),
        "hide_shoulder": drape_paint(patches((226, 220, 210), (46, 38, 34), 2, 1.5, 2.6, 9), 26),
        "hide_foreleg_l": limb_paint(patches((226, 220, 210), (46, 38, 34), 1, 1.0, 1.8, 10), (58, 52, 50)),
        "hide_foreleg_r": limb_paint(patches((226, 220, 210), (46, 38, 34), 1, 1.0, 1.8, 11), (58, 52, 50)),
        "mask": dict(base=(226, 220, 210), noise=5, features={
            "all": [("draw", patches((226, 220, 210), (46, 38, 34), 2, 1.5, 2.5, 4))],
            "front": [("ascii", [".......", ".HE.EH.", ".......", ".......", "......."],
                       {"H": (20, 16, 18), "E": (C_EYE, "glow")})]}),
        "snout": dict(base=(186, 150, 140), noise=5, features={"front": [("ascii", ["....", ".nn.", "...."], {"n": (70, 50, 50)})]}),
        "mask_ear_l": dict(base=(214, 206, 184), noise=4, features={"all": [("ascii", ["...d", "...d"], {"d": (150, 140, 118)})]}),
        "mask_ear_r": dict(base=(214, 206, 184), noise=4, features={"all": [("ascii", ["d...", "d..."], {"d": (150, 140, 118)})]}),
    }


def _pig():
    coat = patches((230, 162, 160), (202, 128, 128), 5, 1.0, 2.0, 5)
    return {
        "hide": hide_paint(coat, 31),
        "hide_rump": hide_paint(coat, 34, 2),
        "hide_neck": hide_paint(coat, 35, 2),
        "hide_leg_l": hide_paint(coat, 32, 1),
        "hide_leg_r": hide_paint(coat, 33, 1),
        "hide_tail": dict(base=(222, 150, 150), noise=4),
        "hide_shoulder": drape_paint(coat, 36),
        "hide_foreleg_l": limb_paint(coat, (150, 96, 96)),
        "hide_foreleg_r": limb_paint(coat, (150, 96, 96)),
        "mask": dict(base=(232, 164, 162), noise=5, features={
            "all": [("draw", patches((232, 164, 162), (212, 140, 140), 3, 1.0, 1.8, 6))],
            "front": [("ascii", [".......", ".HE.EH.", ".......", ".......", "......."],
                       {"H": (40, 20, 24), "E": (C_EYE, "glow")})]}),
        "snout": dict(base=(242, 176, 174), noise=4, features={"front": [("ascii", ["....", ".n.n", "...."], {"n": (120, 60, 66)})]}),
        "mask_ear_l": dict(base=(220, 146, 146), noise=4),
        "mask_ear_r": dict(base=(220, 146, 146), noise=4),
    }


def _sheep():
    def wool(seed):
        def fn(canvas, glow, u, v, w, h, r):
            for y in range(h):
                for x in range(w):
                    k = _hash(x // 2, y // 2, seed) % 4
                    c = [(228, 222, 204), (214, 206, 186), (236, 232, 218), (200, 192, 172)][k]
                    canvas.set(u + x, v + y, jitter(c, 4, r))
        return fn
    return {
        "hide": hide_paint(wool(41), 41, 2),
        "hide_rump": hide_paint(wool(45), 45, 2),
        "hide_neck": hide_paint(wool(46), 46, 2),
        "hide_leg_l": hide_paint(wool(42), 42, 1),
        "hide_leg_r": hide_paint(wool(43), 43, 1),
        "hide_tail": dict(base=(220, 214, 196), noise=5),
        "hide_shoulder": drape_paint(wool(47), 47),
        "hide_foreleg_l": limb_paint(wool(48), (96, 88, 82), 4),
        "hide_foreleg_r": limb_paint(wool(49), (96, 88, 82), 4),
        "mask": dict(base=(224, 218, 200), noise=5, features={
            "all": [("draw", wool(44))],
            "front": [("ascii", [".......", ".HE.EH.", ".fffff.", ".fffff.", "......."],
                       {"H": (30, 26, 26), "E": (C_EYE, "glow"), "f": (96, 88, 82)})]}),
        "snout": dict(base=(92, 84, 78), noise=4, features={"front": [("ascii", ["....", ".nn.", "...."], {"n": (50, 44, 42)})]}),
        "mask_ear_l": dict(base=(96, 88, 82), noise=4),
        "mask_ear_r": dict(base=(96, 88, 82), noise=4),
    }


def _chicken():
    def feathers(seed):
        def fn(canvas, glow, u, v, w, h, r):
            for y in range(h):
                for x in range(w):
                    c = (240, 238, 232) if (y + (x // 2)) % 3 else (206, 204, 200)
                    canvas.set(u + x, v + y, jitter(c, 3, r))
        return fn
    return {
        "hide": hide_paint(feathers(51), 51, 2),
        "hide_rump": hide_paint(feathers(53), 53, 2),
        "hide_neck": [dict(base=(190, 40, 36), noise=4, shape=ragged(2, seed=54)), dict(base=(150, 30, 30), noise=4, shape=ragged(2, seed=54))],
        "hide_leg_l": dict(base=(222, 170, 64), noise=4),
        "hide_leg_r": dict(base=(222, 170, 64), noise=4),
        "hide_tail": dict(base=(236, 234, 228), noise=4),
        "hide_shoulder": drape_paint(feathers(55), 55),
        # A chicken skin has wings where the others have forelegs.
        "hide_foreleg_l": limb_paint(feathers(56), (196, 194, 188), 3),
        "hide_foreleg_r": limb_paint(feathers(57), (196, 194, 188), 3),
        "mask": dict(base=(240, 238, 232), noise=4, features={
            "all": [("draw", feathers(52))],
            "front": [("ascii", [".......", ".HE.EH.", ".......", ".......", "......."],
                       {"H": (26, 24, 24), "E": (C_EYE, "glow")})],
            "top": [("ascii", ["..rrr..", "..rrr..", "...r...", ".......", ".......", ".......", "......."], {"r": (190, 40, 36)})]}),
        "snout": dict(base=(226, 164, 60), noise=4, features={"bottom": [("ascii", ["rrrr", "rrrr"], {"r": (190, 40, 36)})]}),
        "mask_ear_l": _gone(),
        "mask_ear_r": _gone(),
    }


def copyist():
    """
    Something that learned what an animal is by watching them and got it almost right. Its own
    body is pale, too long and hairless; it walks on crude copies of hooves; and it wears the
    skin of whatever it was pretending to be, the animal's head pulled down over its own like a
    mask. Under the snout is a human mouth full of flat teeth.
    """
    face = _c_skin({"front": [("ascii", [".....", ".....", ".....", "ddddd", "lllll"],
                               {"d": C_SKIN_D, "l": C_SKIN_L})]}, veins=False)
    jaw = _c_skin({"front": [("ascii", ["TTTT", "gggg"], {"T": C_TOOTH, "g": C_GUM})],
                   "top": [("ascii", ["gggg", "gggg", "gggg", "TTTT"], {"g": (90, 40, 44), "T": C_TOOTH})]}, veins=False)
    chest = _c_skin({"front": [("ascii", [
        "s.sss.s",
        "rr.s.rr",
        "..s.s..",
        "rr.s.rr",
        "..s.s..",
        "rr.s.rr",
        "s.sss.s",
    ], {"s": C_SKIN, ".": (132, 108, 106), "r": C_SKIN_L})]})
    hoof = dict(base=C_HOOF, noise=5, rim=((30, 26, 24), 0.5), features={"front": [("ascii", [".", "d"], {"d": (30, 26, 24)}, (1, 0))]})
    finger = _c_skin({"all": [("ascii", ["", "", "n"], {"n": (96, 80, 76)})]}, veins=False)
    cow = _cow()

    def arm(side, sx):
        m = sx < 0
        fingers = [P(f"{side}_finger_{i}", (fx, 2, -1), rot=(0.3, 0, (i - 1) * 0.12 * -sx),
                     boxes=[B(-0.5, 0, -0.5, 1, 3, 1, mirror=m)], paint=finger) for i, fx in enumerate((-1, 0, 1))]
        return P(f"{side}_upper_arm", (4.2 * sx, -6, -1), rot=(-0.95, 0, -0.1 * sx),
                 boxes=[B(-1, -1, -1, 2, 12, 2, mirror=m)], paint=_c_skin(), children=[
                     P(f"{side}_forearm", (0, 11, 0), rot=(0.15, 0, 0), boxes=[B(-1, 0, -1, 2, 11, 2, mirror=m)], paint=_c_skin(), children=[
                         P(f"{side}_hand", (0, 11, 0), rot=(-0.2, 0, 0), boxes=[B(-1.5, 0, -1.5, 3, 2, 3, mirror=m)],
                           paint=_c_skin(veins=False), children=fingers)])])

    def leg(side, sx):
        m = sx < 0
        return P(f"{side}_thigh", (2 * sx, 1.5, 0), rot=(-0.3, 0, 0), boxes=[B(-1.5, 0, -1.5, 3, 8, 3, mirror=m)], paint=_c_skin(), children=[
            P(f"{side}_shin", (0, 8, 0), rot=(0.6, 0, 0), boxes=[B(-1, 0, -1, 2, 5, 2, mirror=m)], paint=_c_skin(), children=[
                P(f"{side}_hoof", (0, 5, 0), rot=(-0.3, 0, 0), boxes=[B(-1.5, 0, -2, 3, 2, 3, mirror=m)], paint=hoof)])])

    mask = P("mask", (0, -4.5, -0.3), boxes=[B(-3.5, -2, -4, 7, 5, 7)], paint=cow["mask"], children=[
        P("snout", (0, 1.2, -4), boxes=[B(-2, -1.5, -2, 4, 3, 2)], paint=cow["snout"]),
        P("mask_ear_l", (3.5, -1.2, -1), rot=(0, 0, -0.35), boxes=[B(0, -1, -0.5, 3, 2, 1)], paint=cow["mask_ear_l"]),
        P("mask_ear_r", (-3.5, -1.2, -1), rot=(0, 0, 0.35), boxes=[B(-3, -1, -0.5, 3, 2, 1, mirror=True)], paint=cow["mask_ear_r"]),
    ])
    head = P("head", (0, -6, 0), rot=(0.25, 0, 0), boxes=[B(-2.5, -5, -3, 5, 5, 5)], paint=face, children=[
        mask,
        P("jaw", (0, 0, 1.5), rot=(0.15, 0, 0), boxes=[B(-2, 0, -4.5, 4, 2, 4)], paint=jaw),
        # The skin of the animal's throat, hanging off the mask down its front.
        P("hide_neck", (0, 1.6, -2.6), rot=(0.12, 0, 0), boxes=skin_layers(-2.5, 0, 5, 5, -1), paint=None),
    ])
    # The skin over its back, from the shoulders down past the ribs.
    hide = P("hide", (0, -7.2, 2.6), rot=(0.05, 0, 0), boxes=skin_layers(-5, 0, 10, 13, 1), paint=None)
    # Draped over its shoulders like a cape, tied on by the animal's own empty forelegs, which
    # hang straight down its front (the chest leans forward, so they are turned back to vertical).
    shoulder = P("hide_shoulder", (0, -7.05, 0), boxes=[B(-4.5, -0.15, -3.4, 9, 0, 6), B(-4.5, 0, -3.4, 9, 0, 6)], paint=None,
                 children=[
                     P("hide_foreleg_l", (2.2, 0, -3.3), rot=(-0.72, 0, -0.08), boxes=[B(-1, 0, -0.5, 2, 9, 1)],
                       paint=cow["hide_foreleg_l"]),
                     P("hide_foreleg_r", (-2.2, 0, -3.3), rot=(-0.72, 0, 0.08), boxes=[B(-1, 0, -0.5, 2, 9, 1, mirror=True)],
                       paint=cow["hide_foreleg_r"]),
                 ])
    # The hindquarters of the skin hang over its rump, the animal's empty legs and tail swinging from them.
    rump = P("hide_rump", (0, -1.5, 2.3), rot=(0.08, 0, 0), boxes=skin_layers(-4.5, 0, 9, 8, 1), paint=None, children=[
        P("hide_leg_l", (3.3, 7.5, 0.1), rot=(0.05, 0, -0.06), boxes=skin_layers(-1, 0, 2, 7, 1), paint=None),
        P("hide_leg_r", (-3.3, 7.5, 0.1), rot=(0.05, 0, 0.06), boxes=skin_layers(-1, 0, 2, 7, 1), paint=None),
        P("hide_tail", (0, 1, 0.2), rot=(0.35, 0, 0), boxes=[B(-0.5, 0, 0, 1, 9, 0)], paint=cow["hide_tail"]),
    ])
    parts = [
        P("pelvis", (0, 8, 1), boxes=[B(-3, -1, -2, 6, 3, 4)], paint=_c_skin(), children=[
            leg("left", 1), leg("right", -1), rump,
            P("belly", (0, -1, 0), rot=(0.45, 0, 0), boxes=[B(-3, -7, -2.5, 6, 7, 4.5)], paint=_c_skin(), children=[
                P("chest", (0, -7, 0), rot=(0.35, 0, 0), boxes=[B(-3.5, -7, -3, 7, 7, 5)], paint=chest, children=[
                    P("neck", (0, -6.5, -1.5), rot=(-0.55, 0, 0), boxes=[B(-1.5, -6, -1.5, 3, 6, 3)], paint=_c_skin(), children=[head]),
                    arm("left", 1), arm("right", -1),
                    hide, shoulder,
                ]),
            ]),
        ]),
    ]
    for part in (hide, shoulder, rump, head):
        for p in part.walk():
            if p.name in cow and isinstance(cow[p.name], list):
                for b, paint in zip(p.boxes, cow[p.name]):
                    b.paint = paint
    variants = {}
    for name, fn in (("pig", _pig), ("sheep", _sheep), ("chicken", _chicken)):
        variants[name] = fn()
        for k, v in variants[name].items():
            # single-box parts that became two-layer pieces take the same paint on both layers
            if k.startswith("hide_leg") and not isinstance(v, list):
                variants[name][k] = [v, v]
    anim = """        float w = limbSwing * 0.55F;
        // Crawling, it claws itself along rather than striding.
        float a = Math.min(1.0F, limbSwingAmount) * (1.0F - 0.45F * crawl);
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.35F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;
        // It copies animals badly: slow curious tilts broken by sudden snaps of the head.
        float snap = Anim.pulse(Mth.sin(ageInTicks * 0.23F), 0.93F) * 0.45F;
        head.zRot += Mth.sin(ageInTicks * 0.04F) * 0.22F + snap;
        head.yRot -= Anim.pulse(Mth.sin(ageInTicks * 0.11F), 0.95F) * 0.5F;
        // Its jaw works in bursts, chewing nothing.
        jaw.xRot += Anim.pulse(Mth.sin(ageInTicks * 0.05F), 0.55F) * Math.max(0.0F, Mth.sin(ageInTicks * 0.9F)) * 0.3F;
        // A four-limbed walk: arms as forelegs, knuckles down.
        leftThigh.xRot += Mth.cos(w) * 0.8F * a;
        rightThigh.xRot += Mth.cos(w + Mth.PI) * 0.8F * a;
        leftShin.xRot += Math.max(0.0F, Mth.sin(w)) * 0.6F * a;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(w)) * 0.6F * a;
        leftUpperArm.xRot += Mth.cos(w + Mth.PI) * 0.7F * a;
        rightUpperArm.xRot += Mth.cos(w) * 0.7F * a;
        leftForearm.xRot -= Math.max(0.0F, Mth.cos(w)) * 0.4F * a;
        rightForearm.xRot -= Math.max(0.0F, -Mth.cos(w)) * 0.4F * a;
        chest.xRot += Mth.sin(ageInTicks * 0.08F) * 0.03F;
        pelvis.y -= Mth.abs(Mth.cos(w)) * 0.6F * a;
        hide.xRot += -Mth.abs(Mth.sin(w)) * 0.12F * a + Mth.sin(ageInTicks * 0.06F) * 0.03F;
        hideRump.xRot += -Mth.abs(Mth.sin(w + 0.4F)) * 0.25F * a;
        hideNeck.xRot += -Mth.abs(Mth.sin(w + 1.0F)) * 0.2F * a + Mth.sin(ageInTicks * 0.07F) * 0.04F;
        hideLegL.xRot += Mth.sin(w + 0.7F) * 0.35F * a + Mth.sin(ageInTicks * 0.1F) * 0.05F;
        hideLegR.xRot += Mth.sin(w + 2.2F) * 0.35F * a - Mth.sin(ageInTicks * 0.1F) * 0.05F;
        hideTail.zRot += Mth.sin(ageInTicks * 0.13F) * 0.2F;
        hideForelegL.xRot += Mth.sin(w + 1.3F) * 0.3F * a + Mth.sin(ageInTicks * 0.09F) * 0.05F;
        hideForelegR.xRot += Mth.sin(w + 2.9F) * 0.3F * a - Mth.sin(ageInTicks * 0.09F + 1.0F) * 0.05F;
        hideForelegL.zRot += Mth.sin(ageInTicks * 0.05F) * 0.04F;
        hideForelegR.zRot -= Mth.sin(ageInTicks * 0.05F + 0.8F) * 0.04F;
        if (attackTime > 0.0F) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            leftUpperArm.xRot -= 1.3F * s;
            rightUpperArm.xRot -= 1.1F * s;
            jaw.xRot += 0.7F * s;
            head.xRot -= 0.4F * s;
        }
"""
    return Model("copyist", "CopyistModel", E + "CopyistEntity", (128, 64), parts, anim, variants=variants,
                 blends=squeeze_blends(COPYIST_CRAWL, COPYIST_STOOP))


# ================================================================ the Erratum
R_CHITIN = (22, 20, 26)
R_SHEEN = (74, 66, 96)
R_JOINT = (208, 198, 172)
R_TIP = (124, 128, 140)
R_GUM = (112, 30, 36)
R_GUM_D = (70, 16, 22)
R_TOOTH = (226, 216, 190)
R_THROAT = (8, 6, 8)
R_FLESH = (128, 70, 70)
R_SCLERA = (226, 214, 178)
R_VEIN = (170, 60, 56)
R_IRIS = (40, 30, 52)
R_PUPIL = (8, 6, 10)

# Leg angles (radians, for the +x side; mirrored on the other): unfolded and folded.
ERR_FEMUR_OUT, ERR_FEMUR_IN = -2.52, -0.35
ERR_TIBIA_OUT, ERR_TIBIA_IN = 2.28, 2.95


def maw():
    """A round mouth: a ring of gum, a ring of teeth, and the throat."""
    def fn(canvas, glow, u, v, w, h, r):
        cx, cy = w / 2, h / 2
        for y in range(h):
            for x in range(w):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                ang = math.atan2(y + 0.5 - cy, x + 0.5 - cx)
                if d > 6.6:
                    canvas.set(u + x, v + y, (0, 0, 0), 0)
                elif d > 5.2:
                    canvas.set(u + x, v + y, jitter(R_GUM, 6, r))
                elif d > 3.9:
                    tooth = int((ang + math.pi) / (2 * math.pi) * 14) % 2 == 0
                    canvas.set(u + x, v + y, jitter(R_TOOTH if tooth else R_GUM_D, 5, r))
                elif d > 2.4:
                    canvas.set(u + x, v + y, jitter(R_GUM_D, 5, r))
                else:
                    canvas.set(u + x, v + y, jitter(R_THROAT, 3, r))
    return fn


def erratum():
    """Legs and underside of the Erratum. The block itself is drawn by the renderer."""
    chitin = dict(base=R_CHITIN, noise=3, features={"all": [("draw", gloss(R_CHITIN, R_SHEEN)),
                                                            ("ascii", ["j"], {"j": R_JOINT})]})
    tip = dict(base=R_TIP, noise=4, features={"all": [("ascii", ["", "d"], {"d": (80, 84, 96)})]})
    tooth = dict(base=R_TOOTH, noise=5, features={"all": [("ascii", ["", "d"], {"d": (180, 168, 140)})]})
    parts = []
    for side, sx in (("left", 1), ("right", -1)):
        m = sx < 0
        for i, z in enumerate((-5, 0, 5)):
            splay = (0.45, 0.0, -0.45)[i] * sx
            parts.append(P(f"{side}_leg_{i}", (7 * sx, 23.5, z), rot=(0, splay, 0),
                           boxes=[B(0 if sx > 0 else -2, -0.5, -0.5, 2, 1, 1, mirror=m)], paint=chitin, children=[
                               P(f"{side}_femur_{i}", (2 * sx, 0, 0), rot=(0, 0, ERR_FEMUR_OUT * sx),
                                 boxes=[B(-0.5, 0, -0.5, 1, 9, 1, mirror=m)], paint=chitin, children=[
                                     P(f"{side}_tibia_{i}", (0, 9, 0), rot=(0, 0, ERR_TIBIA_OUT * sx),
                                       boxes=[B(-0.5, 0, -0.5, 1, 12, 1, mirror=m)], paint=chitin, children=[
                                           P(f"{side}_tip_{i}", (0, 12, 0), boxes=[B(-0.5, 0, -0.5, 1, 2, 1, mirror=m)], paint=tip)])])]))
    teeth = []
    for k in range(8):
        a = k / 8 * 2 * math.pi
        teeth.append(P(f"tooth_{k}", (round(math.cos(a) * 4.6, 2), 0, round(math.sin(a) * 4.6, 2)),
                       rot=(round(-math.sin(a) * 0.5, 3), 0, round(math.cos(a) * 0.5, 3)),
                       boxes=[B(-0.5, 0, -0.5, 1, 2, 1)], paint=tooth))
    parts.append(P("maw", (0, 24.05, 0), boxes=[B(-7, 0, -7, 14, 0, 14)],
                   paint=dict(base=R_GUM, noise=0, features={"bottom": [("draw", maw())]}), children=teeth))
    anim = """        float ext = entity.legExtension();
        float w = limbSwing * 1.3F;
        // Folded flat under the block, the legs swing out and up as it rises.
        float femurOut = Mth.lerp(ext, %sF, %sF);
        float tibiaOut = Mth.lerp(ext, %sF, %sF);
        ModelPart[][] legs = {
            {leftLeg0, leftFemur0, leftTibia0}, {leftLeg1, leftFemur1, leftTibia1}, {leftLeg2, leftFemur2, leftTibia2},
            {rightLeg0, rightFemur0, rightTibia0}, {rightLeg1, rightFemur1, rightTibia1}, {rightLeg2, rightFemur2, rightTibia2}};
        for (int i = 0; i < 6; i++) {
            float side = i < 3 ? 1.0F : -1.0F;
            // Tripod gait: legs 0 and 2 on one side step with leg 1 on the other.
            boolean groupA = (i %% 2 == 0) == (i < 3);
            float phase = groupA ? 0.0F : Mth.PI;
            float step = Mth.sin(w + phase) * limbSwingAmount * ext;
            legs[i][0].yRot += step * 0.35F * side;
            legs[i][1].zRot = femurOut * side - Math.max(0.0F, Mth.cos(w + phase)) * 0.35F * limbSwingAmount * ext * side;
            legs[i][2].zRot = tibiaOut * side;
            // Twitch while still, like something trying not to move.
            legs[i][1].zRot += Mth.sin(ageInTicks * 0.8F + i * 1.9F) * 0.03F * ext;
        }
        float gnash = Mth.sin(ageInTicks * 0.6F) * 0.2F * ext;
        for (ModelPart t : new ModelPart[]{tooth0, tooth1, tooth2, tooth3, tooth4, tooth5, tooth6, tooth7}) t.xRot += gnash;
""" % (ERR_FEMUR_IN, ERR_FEMUR_OUT, ERR_TIBIA_IN, ERR_TIBIA_OUT)
    return Model("erratum", "ErratumModel", E + "ErratumEntity", (64, 32), parts, anim)


def erratum_eye():
    """The eye that opens in the side of the block. The renderer scales it open and shut."""
    def rows_shape(spans):
        # spans[row] = (first, last) visible column on the front/back faces.
        def shape(name, x, y, w, h):
            if name not in ("front", "back"):
                return 255
            a, b = spans[y]
            return 255 if a <= x <= b else 0
        return shape
    lid = (170, 110, 104)
    wet = (56, 16, 20)
    rim = dict(base=R_FLESH, noise=6, shape=rows_shape([(3, 9), (1, 11), (0, 12), (1, 11), (3, 9)]), features={
        "all": [("ascii", ["...lllllll...",
                           ".ll.......ll.",
                           "w...........w",
                           ".ww.......ww.",
                           "...wwwwwww..."], {"l": lid, "w": wet})]})
    sclera = dict(base=R_SCLERA, noise=5, shape=rows_shape([(2, 8), (0, 10), (2, 8)]), features={
        "front": [("ascii", ["..v........",
                             "p.........p",
                             "........v.."], {"v": R_VEIN, "p": (214, 168, 150)})]})
    ring = (26, 18, 34)
    iris = dict(base=R_IRIS, noise=4, features={"front": [("ascii", ["oio", "iPi", "oio"],
                                                           {"o": ring, "i": (72, 52, 98), "P": R_PUPIL})]})
    glint = dict(base=(250, 250, 244), noise=0)
    parts = [P("rim", (0, 0, 0), boxes=[B(-6.5, -2.5, 0, 13, 5, 0)], paint=rim, children=[
        P("sclera", (0, 0, -0.04), boxes=[B(-5.5, -1.5, 0, 11, 3, 0)], paint=sclera, children=[
            P("iris", (0, 0, -0.04), boxes=[B(-1.5, -1.5, 0, 3, 3, 0)], paint=iris, children=[
                P("glint", (-1.5, -1.5, -0.03), boxes=[B(0, 0, 0, 1, 1, 0)], paint=glint)])])])]
    anim = """        iris.x = entity.irisX();
        iris.y = entity.irisY();
"""
    return Model("erratum_eye", "ErratumEyeModel", E + "ErratumEntity", (32, 16), parts, anim)
