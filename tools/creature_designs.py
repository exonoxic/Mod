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


def gloss(base, sheen, streak_col=None):
    """Wet ink: near-black with a thin highlight streak down one edge."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = jitter(base, 3, r)
                canvas.set(u + x, v + y, c)
        col = streak_col if streak_col is not None else max(0, w - 2)
        for y in range(h):
            if _hash(y, u) % 5 != 0:
                canvas.blend(u + col, v + y, sheen, 0.55)
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


def knocker():
    """
    Taller than a door, and it has to stoop to listen at one. A burial shroud hangs off its
    shoulders, torn open down one side; an iron door-knocker ring is threaded through its lower
    lip. The knuckles of its knocking hand are worn raw.
    """
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
        "top": [("ascii", ["......", ".h....", "...h..", "......", "h...h.", "......"], {"h": (66, 62, 64)})],
        "back": [("ascii", [".h..h.", "......", "..h...", "......"], {"h": (66, 62, 64)})],
        "bottom": [("ascii", ["mmmmmm", "mmmmmm", "mmmmmm", "mmmmmm", "mmmmmm", "tTtTtT"],
                    {"m": K_MOUTH, "t": K_TOOTH, "T": K_TOOTH_D})],
    })
    jaw = _k_skin({
        "front": [("ascii", ["LLoLL", "cllc."], {"L": K_LIP, "o": K_IRON, "c": K_SKIN_D, "l": K_SKIN_L})],
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
    iron = dict(base=K_IRON, noise=5, shade=False, features={"front": [("px", [(0, 0, K_IRON_L)])],
                                                             "top": [("px", [(0, 0, K_IRON_L), (-1, 0, (70, 60, 56))])]})
    foot = _k_skin({"top": [("ascii", ["..", "d.", ".d", "d.", "..", "NN"], {"d": K_SKIN_D, "N": K_NAIL})],
                             "front": [("ascii", ["NN"], {"N": K_NAIL})]})

    def arm(side, sx, knocking):
        s = side
        m = sx < 0
        fingers = []
        for i, fx in enumerate((-1.0, 0.0, 1.0)):
            fingers.append(P(f"{s}_finger_{i}", (fx, 3, -0.3), rot=(0, 0, (i - 1) * 0.06 * -sx),
                             boxes=[B(-0.5, 0, -0.5, 1, 6, 1, mirror=m)], paint=finger))
        fingers.append(P(f"{s}_thumb", (1.8 * -sx, 1, -0.6), rot=(-0.3, 0, 0.5 * sx),
                         boxes=[B(-0.5, 0, -0.5, 1, 3, 1, mirror=m)], paint=finger))
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
        P("wisp_0", (-1.5, -8.8, 2.9), rot=(0.25, 0, 0.1), boxes=[B(-0.5, 0, 0, 1, 7, 0)],
          paint=dict(base=(62, 58, 58), noise=6, shape=ragged(2, seed=11))),
        P("wisp_1", (1.8, -8.6, 2.8), rot=(0.18, 0, -0.14), boxes=[B(-0.5, 0, 0, 1, 9, 0)],
          paint=dict(base=(70, 64, 62), noise=6, shape=ragged(3, seed=12))),
    ])
    shroud_back = P("shroud_back", (0, -8, 2.2), rot=(-0.32, 0, 0), boxes=[B(-4.5, 0, 0, 9, 24, 0)],
                    paint=_k_linen(ragged(4, holes=4, sides=True, seed=3), stains=5))
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
    anim = f"""        float breath = Mth.sin(ageInTicks * 0.07F);
        float a = Math.min(1.0F, limbSwingAmount);
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
        // Long, slow strides; knees fold on the back swing; the whole body bobs.
        leftThigh.xRot += Mth.cos(w) * 0.85F * a;
        rightThigh.xRot += Mth.cos(w + Mth.PI) * 0.85F * a;
        leftShin.xRot += Math.max(0.0F, Mth.sin(w)) * 1.1F * a;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(w)) * 1.1F * a;
        hips.y -= Mth.abs(Mth.cos(w)) * 0.9F * a;
        leftUpperArm.xRot += Mth.cos(w + Mth.PI) * 0.45F * a;
        rightUpperArm.xRot += Mth.cos(w) * 0.45F * a;
        leftForearm.xRot -= Math.max(0.0F, Mth.cos(w)) * 0.35F * a;
        rightForearm.xRot -= Math.max(0.0F, -Mth.cos(w)) * 0.35F * a;
        // Cloth and iron move a beat behind the body.
        shroudBack.xRot += -Mth.abs(Mth.sin(w)) * 0.22F * a + breath * 0.02F;
        shroudFlap.xRot += -Mth.abs(Mth.sin(w + 0.6F)) * 0.18F * a;
        shroudStrip.xRot += -Mth.abs(Mth.sin(w + 1.1F)) * 0.25F * a;
        shroudStrip.zRot += Mth.sin(ageInTicks * 0.05F) * 0.05F;
        ring.xRot += -Mth.sin(w * 2.0F) * 0.3F * a + Mth.sin(ageInTicks * 0.06F) * 0.1F;
        float flex = Mth.sin(ageInTicks * 0.09F);
        leftFinger0.xRot += flex * 0.12F;
        leftFinger1.xRot += Mth.sin(ageInTicks * 0.09F + 0.8F) * 0.12F;
        leftFinger2.xRot += Mth.sin(ageInTicks * 0.09F + 1.6F) * 0.12F;
        rightFinger0.xRot -= flex * 0.1F;
        rightFinger2.xRot -= Mth.sin(ageInTicks * 0.09F + 1.2F) * 0.1F;

        int state = entity.getState();
        int knock = entity.getKnockAnim();
        if (knock > 0) {{
            // One knock: the arm is up at the door, the forearm swings in and the head presses close.
            float k = knock / 10.0F;
            float strike = Mth.sin(k * Mth.PI);
            rightUpperArm.xRot += -1.45F;
            rightUpperArm.zRot += 0.2F;
            rightForearm.xRot += -0.45F - strike * 0.65F;
            rightHand.xRot += 0.5F;
            head.zRot += 0.38F;
            neck.xRot += 0.18F;
            chest.xRot += 0.08F + strike * 0.05F;
        }} else if (state == {K}.KNOCKING) {{
            // Between knocks the hand rests on the door and it listens.
            rightUpperArm.xRot += -1.4F;
            rightUpperArm.zRot += 0.2F;
            rightForearm.xRot += -0.75F;
            head.zRot += 0.42F;
            neck.xRot += 0.22F;
            jaw.xRot += 0.08F;
        }}
        if (state == {K}.LUNGE) {{
            float shake = Mth.sin(ageInTicks * 1.7F) * 0.05F;
            jaw.xRot += 0.95F + shake;
            ring.xRot += 0.6F;
            chest.xRot += 0.18F;
            neck.xRot -= 0.25F;
            head.xRot -= 0.25F;
            leftUpperArm.xRot += -1.35F + shake;
            rightUpperArm.xRot += -1.35F - shake;
            leftUpperArm.zRot -= 0.18F;
            rightUpperArm.zRot += 0.18F;
            leftForearm.xRot += 0.15F;
            rightForearm.xRot += 0.15F;
            leftFinger0.zRot += 0.35F;
            leftFinger2.zRot -= 0.35F;
            rightFinger0.zRot -= 0.35F;
            rightFinger2.zRot += 0.35F;
        }} else if (state == {K}.LEAVING) {{
            neck.xRot += 0.4F;
            head.xRot += 0.25F;
            chest.xRot += 0.1F;
        }} else {{
            jaw.xRot += 0.05F + Math.max(0.0F, Mth.sin(ageInTicks * 0.031F)) * 0.12F;
        }}
        if (attackTime > 0.0F) {{
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightUpperArm.xRot -= 1.1F * s;
            rightForearm.xRot -= 0.6F * s;
            jaw.xRot += 0.4F * s;
        }}
"""
    return Model("knocker", "KnockerModel", E + "KnockerEntity", (128, 64), parts, anim)
