"""
Detailed designs for the rest of the bestiary (everything but the four "rule" creatures in
creature_designs.py). Same conventions as gen_models.py: pixel units, y down, feet at y = 24,
-z is the front. Iterate on them with tools/preview_models.py.
"""
import math

from modelkit import B, P, Model, E, HEAD_LOOK
from pixel import jitter, mix, shade
from creature_designs import _hash, gloss, ragged, skin, weave


# ================================================================ painting helpers
def crackle(base, crack, sheen=None, density=6, seed=0):
    """Dried ink: near-black, crazed with fine pale cracks like old varnish, a little sheen."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = base
                if sheen is not None and _hash(u + x, v + y, seed) % 11 == 0:
                    c = mix(c, sheen, 0.3)
                canvas.set(u + x, v + y, jitter(c, 3, r))
        for _ in range(max(1, (w * h) // max(1, 60 // density))):
            x, y = r.randrange(w), r.randrange(h)
            for _ in range(r.randint(2, 5)):
                if 0 <= x < w and 0 <= y < h:
                    canvas.blend(u + x, v + y, crack, 0.55)
                if r.random() < 0.5:
                    x += r.choice((-1, 1))
                else:
                    y += r.choice((-1, 1))
    return fn


def ribs(rib, gap, start=1, period=2, fade=1):
    """Ribs showing through a thin hide: pale bars with dark hollows between, fading top and bottom."""
    def fn(canvas, glow, u, v, w, h, r):
        for x in range(start, w, period):
            for y in range(fade, h - fade):
                canvas.blend(u + x, v + y, rib, 0.45)
                if x + 1 < w:
                    canvas.blend(u + x + 1, v + y, gap, 0.55)
    return fn


def speckle(c, chance, seed=0, alpha=0.8):
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                if _hash(u + x, v + y, seed) % 1000 < chance * 1000:
                    canvas.blend(u + x, v + y, c, alpha)
    return fn


def drip_shape(seed=0):
    """A hanging drop: tapers to a point at the bottom of its side faces."""
    def shape(name, x, y, w, h):
        if name in ("top", "bottom"):
            return 255
        if y >= h - 1 and _hash(x, seed) % 2 == 0:
            return 0
        return 255
    return shape


def teeth_row(tooth, gum, gap=None, glow_gum=False):
    """A row of thin pale teeth with gum showing between them."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = tooth if x % 2 == 0 else (gap or gum)
                canvas.set(u + x, v + y, jitter(c, 4, r))
                if glow_gum and x % 2 == 1 and glow is not None:
                    glow.set(u + x, v + y, gum)
    return fn


def fill(c, noise=3):
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                canvas.set(u + x, v + y, jitter(c, noise, r))
    return fn


def fill_glow(c):
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                canvas.set(u + x, v + y, c)
                if glow is not None:
                    glow.set(u + x, v + y, c)
    return fn


# ================================================================ the Inkhound
H_INK = (19, 18, 25)
H_INK_2 = (27, 26, 35)
H_CRACK = (66, 70, 92)
H_SHEEN = (72, 80, 116)
H_RIB = (58, 60, 78)
H_GAP = (8, 8, 11)
H_GUM = (110, 20, 26)
H_GUM_D = (58, 10, 14)
H_TOOTH = (200, 192, 170)
H_TOOTH_D = (140, 130, 112)
H_CLAW = (150, 144, 128)
H_EAR = (70, 22, 30)


def _h_ink(extra=None, density=6, seed=0):
    feats = {"all": [("draw", crackle(H_INK, H_CRACK, H_SHEEN, density, seed))]}
    for k, lst in (extra or {}).items():
        feats.setdefault(k, []).extend(lst)
    return dict(base=H_INK, noise=3, rim=(H_GAP, 0.4), features=feats)


def inkhound():
    """
    A hound of dried ink, starved to the frame, with no eyes: the skull is smooth where they
    should be. It hunts by sound, so the ears are enormous and never still. The jaw hangs; when it
    has heard you it drops open on rows of little pale nibs, and the wet red inside is the only
    colour it has.
    """
    ribbed = _h_ink({"right": [("draw", ribs(H_RIB, H_GAP))], "left": [("draw", ribs(H_RIB, H_GAP))],
                     "bottom": [("draw", ribs(H_RIB, H_GAP, 0, 2, 0))]}, density=8, seed=1)
    tucked = _h_ink({"sides": [("draw", speckle(H_CRACK, 0.05, 3))]}, seed=2)
    # Where the eyes should be the skull is smooth; only a faint pucker of healed-over lids.
    skull = _h_ink({"front": [("ascii", [
        "....",
        "d..d",
        "Dd.D",
        "....",
    ], {"d": (30, 29, 38), "D": (40, 40, 52)})], "top": [("draw", speckle(H_CRACK, 0.12, 4))]}, seed=4)
    snout = _h_ink({
        "front": [("ascii", ["N.N", "...", "ttt"], {"N": H_GAP, "t": H_TOOTH})],
        "bottom": [("draw", teeth_row(H_TOOTH, H_GUM, glow_gum=True))],
        "left": [("ascii", ["", "", "tTtT"], {"t": H_TOOTH, "T": H_TOOTH_D})],
        "right": [("ascii", ["", "", "TtTt"], {"t": H_TOOTH, "T": H_TOOTH_D})],
    }, seed=5)
    jaw = _h_ink({
        "top": [("draw", teeth_row(H_TOOTH, H_GUM, glow_gum=True))],
        "front": [("ascii", ["t.t"], {"t": H_TOOTH})],
    }, seed=6)
    ear = dict(base=H_INK_2, noise=3, features={
        "all": [("draw", crackle(H_INK_2, H_CRACK, None, 4, 7))],
        # The inside of the ear: thin, red-veined membrane.
        "right": [("draw", fill(H_EAR, 5)), ("vstripes", 2, H_GUM_D)],
        "left": [("draw", fill(H_EAR, 5)), ("vstripes", 2, H_GUM_D)],
    })
    limb = _h_ink(seed=8)
    paw = _h_ink({"front": [("ascii", ["c.c"], {"c": H_CLAW})], "top": [("ascii", ["c.c"], {"c": H_CLAW})]}, seed=9)
    bone = dict(base=H_INK_2, noise=3, features={"top": [("draw", fill(H_RIB, 4))]})
    drip = dict(base=(10, 10, 14), noise=2, shade=False, shape=drip_shape(3),
                features={"all": [("draw", gloss((10, 10, 14), H_SHEEN))]})

    def ear_part(side, sx):
        return P(f"{side}_ear", (1.4 * sx, -2.2, -1.2), rot=(-0.25, -0.25 * sx, 0.3 * sx),
                 boxes=[B(-0.5, -7, -1.5, 1, 7, 3, mirror=sx < 0)], paint=ear)

    def front_leg(side, sx):
        m = sx < 0
        return P(f"front_{side}_leg", (1.9 * sx, 2.2, -1.5), rot=(0.05, 0, 0),
                 boxes=[B(-1, 0, -1, 2, 5, 2, mirror=m)], paint=limb, children=[
                     P(f"front_{side}_shin", (0, 5, 0.2), rot=(-0.05, 0, 0),
                       boxes=[B(-0.5, 0, -0.5, 1, 4, 1, mirror=m)], paint=limb, children=[
                           P(f"front_{side}_paw", (0, 4, 0), boxes=[B(-1, 0, -2.5, 2, 1, 3, mirror=m)], paint=paw)])])

    def hind_leg(side, sx):
        m = sx < 0
        # Hind legs bend like a dog's: thigh forward, the long hock back, the foot forward again.
        return P(f"hind_{side}_leg", (1.8 * sx, 0.5, 1.8), rot=(-0.45, 0, 0),
                 boxes=[B(-1, -0.5, -1.5, 2, 6, 3, mirror=m)], paint=limb, children=[
                     P(f"hind_{side}_hock", (0, 5, 0), rot=(1.0, 0, 0),
                       boxes=[B(-0.5, 0, -0.5, 1, 5, 1, mirror=m)], paint=limb, children=[
                           P(f"hind_{side}_foot", (0, 5, 0), rot=(-0.55, 0, 0),
                             boxes=[B(-0.5, 0, -0.5, 1, 3, 1, mirror=m)], paint=limb, children=[
                                 P(f"hind_{side}_paw", (0, 3, 0), boxes=[B(-1, 0, -2.5, 2, 1, 3, mirror=m)], paint=paw)])])])

    spine = [P(f"vertebra_{i}", (0, -3.2, z), boxes=[B(-0.5, -1, -0.5, 1, 1, 1)], paint=bone) for i, z in enumerate((-4, -2, 0))]
    loin_spine = [P(f"loin_vertebra_{i}", (0, -1.2, z), boxes=[B(-0.5, -1, -0.5, 1, 1, 1)], paint=bone) for i, z in enumerate((1, 3, 5))]
    head = P("head", (0, -0.5, -4.5), rot=(0.15, 0, 0), boxes=[
        B(-2, -2.5, -4, 4, 4, 4, paint=skull),
        B(-1.5, -1.0, -8.5, 3, 2, 5, paint=snout),
    ], children=[
        P("jaw", (0, 1.0, -3.8), rot=(0.18, 0, 0), boxes=[B(-1.5, 0, -4.6, 3, 1, 5)], paint=jaw, children=[
            P("jaw_drip", (0.6, 1, -3.5), boxes=[B(0, 0, -0.5, 0, 3, 1)], paint=drip)]),
        ear_part("left", 1), ear_part("right", -1),
    ])
    parts = [
        P("chest", (0, 11.5, -3), boxes=[B(-2.5, -3, -5, 5, 6, 6)], paint=ribbed, children=spine + [
            P("neck", (0, -1.5, -4.5), rot=(0.42, 0, 0), boxes=[B(-1.5, -1.5, -5, 3, 3, 5)], paint=_h_ink(seed=10), children=[head]),
            front_leg("left", 1), front_leg("right", -1),
            P("chest_drip", (-1.2, 3, -2), boxes=[B(0, 0, -0.5, 0, 4, 1)], paint=drip),
            # The belly is tucked up so tight the body is almost two pieces.
            P("loins", (0, -1.2, 1), rot=(-0.08, 0, 0), boxes=[B(-1.5, -1.3, 0, 3, 3, 6)], paint=tucked, children=loin_spine + [
                P("haunch", (0, 0, 6), rot=(0.1, 0, 0), boxes=[B(-2, -1.8, -0.5, 4, 5, 4)], paint=_h_ink(seed=11), children=[
                    hind_leg("left", 1), hind_leg("right", -1),
                    P("tail", (0, -1.3, 3.2), rot=(-0.55, 0, 0), boxes=[B(-0.5, -0.5, 0, 1, 1, 5)], paint=limb, children=[
                        P("tail_tip", (0, 0, 5), rot=(0.7, 0, 0), boxes=[B(-0.5, -0.5, 0, 1, 1, 5)], paint=limb, children=[
                            P("tail_drip", (0, 0.5, 4.5), boxes=[B(0, 0, -0.5, 0, 3, 1)], paint=drip)])]),
                ]),
            ]),
        ]),
    ]
    anim = """        float a = Math.min(1.0F, limbSwingAmount);
        float w = limbSwing * 0.62F;
        boolean hunting = entity.isAggressive();
        // Always listening: each ear turns on its own, the head sweeps and tilts.
        neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.6F + Mth.sin(ageInTicks * 0.05F) * 0.18F * (1.0F - a);
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;
        head.zRot += Mth.sin(ageInTicks * 0.033F) * 0.2F * (1.0F - a);
        leftEar.yRot += Mth.sin(ageInTicks * 0.11F) * 0.35F;
        rightEar.yRot += Mth.sin(ageInTicks * 0.09F + 2.1F) * 0.35F;
        leftEar.xRot += Mth.sin(ageInTicks * 0.23F) > 0.9F ? -0.3F : 0.0F;
        rightEar.xRot += Mth.sin(ageInTicks * 0.19F + 1.0F) > 0.9F ? -0.3F : 0.0F;
        // The jaw never quite closes; it trembles.
        jaw.xRot += 0.08F + Mth.sin(ageInTicks * 0.6F) * 0.025F;
        // A long, low lope: the spine flexes, the chest dips.
        frontLeftLeg.xRot += Mth.cos(w) * 1.0F * a;
        frontRightLeg.xRot += Mth.cos(w + 0.4F) * 1.0F * a;
        frontLeftShin.xRot -= Math.max(0.0F, Mth.sin(w)) * 0.9F * a;
        frontRightShin.xRot -= Math.max(0.0F, Mth.sin(w + 0.4F)) * 0.9F * a;
        hindLeftLeg.xRot += Mth.cos(w + Mth.PI) * 0.9F * a;
        hindRightLeg.xRot += Mth.cos(w + Mth.PI + 0.4F) * 0.9F * a;
        hindLeftHock.xRot += Math.max(0.0F, Mth.sin(w + Mth.PI)) * 0.6F * a;
        hindRightHock.xRot += Math.max(0.0F, Mth.sin(w + Mth.PI + 0.4F)) * 0.6F * a;
        loins.xRot += Mth.sin(w) * 0.12F * a;
        haunch.xRot -= Mth.sin(w) * 0.1F * a;
        chest.y += Mth.abs(Mth.sin(w)) * 0.8F * a;
        neck.xRot -= Mth.sin(w) * 0.1F * a;
        tail.yRot += Mth.sin(ageInTicks * 0.2F) * 0.2F;
        tailTip.yRot += Mth.sin(ageInTicks * 0.2F - 0.8F) * 0.3F;
        tail.xRot -= a * 0.35F;
        jawDrip.zRot += Mth.sin(ageInTicks * 0.1F) * 0.2F;
        chestDrip.zRot += Mth.sin(ageInTicks * 0.08F + 1.0F) * 0.15F;
        if (hunting) {
            // It has heard you: head low, ears flat, the mouth wide on its little pale teeth.
            jaw.xRot += 0.75F + Mth.sin(ageInTicks * 1.3F) * 0.05F;
            head.xRot -= 0.2F;
            neck.xRot += 0.25F;
            leftEar.xRot += 0.9F;
            rightEar.xRot += 0.9F;
            tail.xRot += 0.4F;
        }
        if (attackTime > 0.0F) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            jaw.xRot += 0.6F * s;
            neck.xRot -= 0.35F * s;
            head.xRot -= 0.3F * s;
        }
"""
    return Model("inkhound", "InkhoundModel", E + "InkhoundEntity", (64, 64), parts, anim)


# ================================================================ the Pale Stag
S_PAPER = (228, 225, 216)
S_PAPER_D = (196, 192, 182)
S_PAPER_DD = (160, 156, 148)
S_RULE = (170, 186, 206)
S_FOX = (186, 150, 108)
S_INK = (16, 15, 21)
S_SHEEN = (78, 84, 120)


def ruled(base, rule, period=3, offset=1, foxing=(), fox_count=0):
    """Ruled writing paper: faint blue lines, a few brown foxing freckles."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = base
                if (v + y + offset) % period == 0:
                    c = mix(c, rule, 0.35)
                canvas.set(u + x, v + y, jitter(c, 3, r))
        for _ in range(fox_count):
            if foxing:
                canvas.blend(u + r.randrange(w), v + r.randrange(h), r.choice(foxing), 0.45)
    return fn


def wicking(ink, height_frac=0.5):
    """Ink soaking up into paper from below, ragged at its upper edge."""
    def fn(canvas, glow, u, v, w, h, r):
        for x in range(w):
            top = int(h * (1 - height_frac)) + (_hash(u + x, 17) % 3) - 1
            for y in range(max(0, top), h):
                t = min(1.0, (y - top + 1) / 3.0)
                canvas.blend(u + x, v + y, ink, 0.5 + 0.45 * t)
    return fn


def _s_paper(extra=None, rules=True, fox=2):
    feats = {"all": [("draw", ruled(S_PAPER, S_RULE, 3, 1, (S_FOX,), fox) if rules else fill(S_PAPER, 3))]}
    for k, lst in (extra or {}).items():
        feats.setdefault(k, []).extend(lst)
    return dict(base=S_PAPER, noise=3, rim=(S_PAPER_DD, 0.3), features=feats)


def pale_stag():
    """
    A deer the colour of blank writing paper, faint ruled lines and all, drawn too tall: legs like
    stilts, the ribs showing through, the neck too long. It has no face. Where the face should be
    the paper is smooth, with a single fold down the middle. Its antlers are ink, a great black
    crown of it, still wet enough to drip, and its legs have stood in ink so long it has soaked up
    into them.
    """
    ribbed = _s_paper({"right": [("draw", ribs(S_PAPER_D, S_PAPER_DD, 1, 2, 1))],
                       "left": [("draw", ribs(S_PAPER_D, S_PAPER_DD, 1, 2, 1))]}, fox=3)
    blank = dict(base=S_PAPER, noise=2, rim=(S_PAPER_D, 0.25), features={
        "all": [("draw", fill(S_PAPER, 2))],
        # Nothing: a smooth page, folded once down the middle.
        "front": [("vstripes", 99, S_PAPER_D), ("ascii", [".f..", ".F..", ".f..", ".F.."], {"f": S_PAPER_D, "F": (206, 202, 192)}, (0, 0))],
        "top": [("ascii", ["..f..", "..F..", "..f..", "..F..", "..f.."], {"f": S_PAPER_D, "F": (206, 202, 192)})],
    })
    muzzle = dict(base=S_PAPER, noise=2, rim=(S_PAPER_D, 0.25), features={
        "all": [("draw", fill(S_PAPER, 2))],
        "front": [("ascii", [".f.", ".F.", ".f."], {"f": S_PAPER_D, "F": (206, 202, 192)})],
        "top": [("vstripes", 99, S_PAPER_D), ("ascii", [".f.", ".F.", ".f.", ".F."], {"f": S_PAPER_D, "F": (206, 202, 192)})],
    })
    leg = _s_paper({"sides": [("draw", wicking(S_INK, 0.35))]}, fox=1)
    cannon = _s_paper({"sides": [("draw", wicking(S_INK, 0.9))], "bottom": [("draw", fill(S_INK, 2))]}, rules=False, fox=0)
    hoof = dict(base=S_INK, noise=2, features={"all": [("draw", gloss(S_INK, S_SHEEN))]})
    antler = dict(base=S_INK, noise=2, shade=True, features={"all": [("draw", gloss(S_INK, S_SHEEN))]})
    drip = dict(base=S_INK, noise=2, shade=False, shape=drip_shape(5), features={"all": [("draw", gloss(S_INK, S_SHEEN))]})

    def tine(name, pivot, rot, length, children=()):
        return P(name, pivot, rot=rot, boxes=[B(-0.5, -length, -0.5, 1, length, 1)], paint=antler, children=list(children))

    def antler_side(side, sx, lean, spread, twist):
        n = f"{side}_antler"
        # A main beam that sweeps back and out, forking twice, with tines off its front.
        return tine(n, (1.3 * sx, -3, -0.5), (-0.35 + lean, 0.25 * sx * twist, spread * sx), 7, [
            tine(n + "_brow", (0, -1.5, 0), (-1.1, 0, -0.35 * sx), 4),
            tine(n + "_tine_a", (0, -4.5, 0), (-0.9, 0, 0.3 * sx), 4, [
                P(n + "_drip_a", (0, -3.2, -0.5), rot=(0.9, 0, 0), boxes=[B(0, 0, -0.5, 0, 3, 1)], paint=drip)]),
            tine(n + "_beam", (0, -7, 0), (-0.35, 0, 0.35 * sx), 6, [
                tine(n + "_tine_b", (0, -3, 0), (-0.8, 0, -0.45 * sx), 4),
                tine(n + "_tine_c", (0, -6, 0), (0.25, 0, 0.5 * sx), 4),
                tine(n + "_tine_d", (0, -6, 0), (-0.6, 0, -0.3 * sx), 5, [
                    tine(n + "_tine_e", (0, -5, 0), (0.5, 0, 0.4 * sx), 3)]),
            ]),
        ])

    def front_leg(side, sx):
        m = sx < 0
        return P(f"front_{side}_leg", (2.1 * sx, 2, -5.5), boxes=[B(-1, -0.5, -1.2, 2, 6, 2.4, mirror=m)], paint=leg, children=[
            P(f"front_{side}_cannon", (0, 5.5, 0), rot=(0.02, 0, 0), boxes=[B(-0.5, 0, -0.5, 1, 9, 1, mirror=m)], paint=cannon, children=[
                P(f"front_{side}_hoof", (0, 9, 0), boxes=[B(-0.5, 0, -1.2, 1, 1, 2, mirror=m)], paint=hoof)])])

    def hind_leg(side, sx):
        m = sx < 0
        return P(f"hind_{side}_leg", (2.0 * sx, 1, 4.5), rot=(-0.3, 0, 0), boxes=[B(-1, -1, -1.5, 2, 7, 3, mirror=m)], paint=leg, children=[
            P(f"hind_{side}_hock", (0, 6, 0.3), rot=(0.85, 0, 0), boxes=[B(-0.5, 0, -0.5, 1, 6, 1, mirror=m)], paint=cannon, children=[
                P(f"hind_{side}_cannon", (0, 6, 0), rot=(-0.55, 0, 0), boxes=[B(-0.5, 0, -0.5, 1, 6, 1, mirror=m)], paint=cannon, children=[
                    P(f"hind_{side}_hoof", (0, 6, 0), boxes=[B(-0.5, 0, -1.2, 1, 1, 2, mirror=m)], paint=hoof)])])])

    head = P("head", (0, -10, 0), rot=(-0.3, 0, 0), boxes=[B(-2, -3, -3.5, 4, 4, 5, paint=blank), B(-1.5, -2, -7.5, 3, 3, 4, paint=muzzle)],
             children=[antler_side("left", 1, 0.0, 0.45, 1.0), antler_side("right", -1, 0.12, 0.38, 0.7)])
    parts = [
        P("torso", (0, 5.5, 0), boxes=[B(-3, -3, -8, 6, 7, 8)], paint=ribbed, children=[
            P("neck", (0, -1.5, -7), rot=(0.42, 0, 0), boxes=[B(-1.5, -10, -1.5, 3, 11, 3)], paint=_s_paper(fox=1), children=[head]),
            front_leg("left", 1), front_leg("right", -1),
            P("haunch", (0, -1, 0), rot=(-0.06, 0, 0), boxes=[B(-2.5, -1.5, 0, 5, 5, 7)], paint=_s_paper(fox=2), children=[
                hind_leg("left", 1), hind_leg("right", -1),
                P("tail", (0, -1, 7), rot=(0.5, 0, 0), boxes=[B(-1, 0, 0, 2, 3, 0)], paint=_s_paper(rules=False, fox=0))]),
        ]),
    ]
    anim = """        float a = Math.min(1.0F, limbSwingAmount);
        float w = limbSwing * 0.5F;
        // It holds itself dead still and turns only its blank face to follow you: the neck does
        // most of the turning, then the head, further than a neck should allow.
        neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.55F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.45F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.7F;
        // Now and then the head tips over sideways, and stays there a moment.
        float tip = Mth.sin(ageInTicks * 0.017F);
        head.zRot += tip > 0.8F ? (tip - 0.8F) * 4.0F : 0.0F;
        torso.xRot += Mth.sin(ageInTicks * 0.04F) * 0.01F;
        frontLeftLeg.xRot += Mth.cos(w) * 0.9F * a;
        frontRightLeg.xRot += Mth.cos(w + Mth.PI) * 0.9F * a;
        frontLeftCannon.xRot -= Math.max(0.0F, Mth.sin(w)) * 0.9F * a;
        frontRightCannon.xRot -= Math.max(0.0F, -Mth.sin(w)) * 0.9F * a;
        hindLeftLeg.xRot += Mth.cos(w + Mth.PI) * 0.8F * a;
        hindRightLeg.xRot += Mth.cos(w) * 0.8F * a;
        hindLeftHock.xRot += Math.max(0.0F, Mth.sin(w)) * 0.5F * a;
        hindRightHock.xRot += Math.max(0.0F, -Mth.sin(w)) * 0.5F * a;
        torso.y -= Mth.abs(Mth.cos(w)) * 1.2F * a;
        neck.xRot -= Mth.cos(w * 2.0F) * 0.06F * a;
        tail.xRot += a * 0.6F;
        leftAntlerDripA.zRot += Mth.sin(ageInTicks * 0.09F) * 0.15F;
        rightAntlerDripA.zRot += Mth.sin(ageInTicks * 0.08F + 1.3F) * 0.15F;
"""
    return Model("pale_stag", "PaleStagModel", E + "PaleStagEntity", (64, 64), parts, anim)


# ================================================================ the Smudge
G_PAPER = (178, 174, 166)
G_LEAD = (74, 72, 76)
G_LEAD_L = (120, 118, 120)
G_RUB = (206, 202, 194)
G_MOUTH = (34, 30, 34)


def hatching(base, lead, period=3, slope=1, alpha=0.5, seed=0):
    """Pencil shading: sparse diagonal strokes over a pale ground, thicker towards the shadowed
    lower part of each face, uneven like a hand did it."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            dark = y / max(1, h - 1)
            for x in range(w):
                c = base
                stroke = (x * slope + y) % period == 0
                if stroke and _hash(u + x, v + y, seed) % 100 < 25 + 65 * dark:
                    c = mix(c, lead, alpha * (0.5 + 0.5 * dark))
                canvas.set(u + x, v + y, jitter(c, 3, r))
    return fn


def rubbed(c, count=2, rad=2.5):
    """Eraser marks: pale, streaky blotches where the drawing was rubbed away."""
    def fn(canvas, glow, u, v, w, h, r):
        for _ in range(count):
            cx, cy = r.uniform(0, w), r.uniform(0, h)
            for y in range(h):
                for x in range(w):
                    d = math.hypot((x + 0.5 - cx) * 0.8, (y + 0.5 - cy) * 1.3)
                    if d < rad:
                        canvas.blend(u + x, v + y, c, 0.75 * (1 - d / rad) + 0.2)
    return fn


def erased(holes=30, seed=0, faces=("front", "back", "right", "left")):
    """Cut-out: bits of the drawing rubbed clean away."""
    def shape(name, x, y, w, h):
        if name in faces and _hash(x, y, seed, len(name)) % 100 < holes:
            return 0
        return 255
    return shape


def outline_oval(name, x, y, w, h):
    """A construction circle: only a thin ring of pencil, the middle left empty."""
    if name not in ("front", "back"):
        return 0
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    d = ((x - cx) / (w / 2.0)) ** 2 + ((y - cy) / (h / 2.0)) ** 2
    return 255 if 0.62 < d < 1.0 else 0


def dashed(name, x, y, w, h):
    if name not in ("front", "back"):
        return 0
    return 255 if y % 4 != 3 else 0


def _g_lead(extra=None, shape=None, seed=0, period=3):
    feats = {"all": [("draw", hatching(G_PAPER, G_LEAD, period, 1, 0.55, seed))]}
    for k, lst in (extra or {}).items():
        feats.setdefault(k, []).extend(lst)
    d = dict(base=G_PAPER, noise=4, edge=G_LEAD, features=feats)
    if shape:
        d["shape"] = shape
    return d


def smudge():
    """
    A person from the first draft: a pencil sketch, hatched in graphite, with the face rubbed
    out. Only the mouth was missed, and it never stops moving. Whoever erased it gave up part way
    through a forearm and a shin, and the artist's guide lines are still there: a centre line
    through the whole figure, a circle for the head.
    """
    head = _g_lead({"front": [("draw", fill(G_RUB, 5)), ("draw", rubbed((226, 222, 214), 3, 3.0))],
                    "sides": [("draw", rubbed(G_RUB, 1, 2.5))]}, seed=1, period=4)
    torso = _g_lead({"front": [("draw", rubbed(G_RUB, 2, 3.0))]}, shape=erased(6, 2), seed=2)
    limb = _g_lead(seed=3)
    worn = _g_lead({"sides": [("draw", rubbed(G_RUB, 2, 2.0))]}, shape=erased(28, 4), seed=4)
    hand = _g_lead({"front": [("vstripes", 1, G_LEAD_L)]}, seed=5)
    # A careful, realistic mouth, lips and teeth, left behind in a face rubbed blank.
    mouth = dict(base=G_MOUTH, noise=2, shade=False, features={"front": [("ascii", [
        ".LLLL.",
        "LtTtTL",
        "MMMMMM",
    ], {"L": (96, 86, 88), "t": (214, 208, 196), "T": (180, 174, 164), "M": G_MOUTH})]})
    lower_lip = dict(base=(112, 102, 104), noise=2, shade=False, features={"front": [("ascii", [
        "tTtT",
        "LLLL",
    ], {"t": (196, 190, 180), "T": (160, 154, 146), "L": (112, 102, 104)})]})
    guide = dict(base=G_LEAD_L, noise=6, shade=False, shape=dashed)
    ring = dict(base=(140, 138, 140), noise=10, shade=False, shape=outline_oval)

    def arm(side, sx, broken):
        m = sx < 0
        fore_paint = worn if broken else limb
        # The broken arm's forearm starts a little below the elbow: the join was erased.
        gap = 1.5 if broken else 0.0
        return P(f"{side}_arm", (4.2 * sx, 1, 0), rot=(0.05, 0, -0.05 * sx), boxes=[B(-1, -1, -1, 2, 10, 2, mirror=m)], paint=limb, children=[
            P(f"{side}_forearm", (0, 9 + gap, 0), rot=(-0.1, 0, 0), boxes=[B(-1, 0, -1, 2, 9, 2, mirror=m)], paint=fore_paint, children=[
                P(f"{side}_hand", (0, 9 + gap * 0.6, 0), boxes=[B(-1, 0, -1.5, 2, 4, 3, mirror=m)], paint=hand)])])

    def leg(side, sx, worn_shin):
        m = sx < 0
        return P(f"{side}_leg", (1.6 * sx, 0, 0), boxes=[B(-1.25, 0, -1.25, 2.5, 7, 2.5, mirror=m)], paint=limb, children=[
            P(f"{side}_shin", (0, 7, 0), boxes=[B(-1, 0, -1, 2, 6, 2, mirror=m)], paint=worn if worn_shin else limb, children=[
                P(f"{side}_foot", (0, 6, 0), boxes=[B(-1.25, 0, -3, 2.5, 1, 4, mirror=m)], paint=limb)])])

    parts = [
        P("hips", (0, 10.5, 0), boxes=[B(-3, -1, -1.5, 6, 2, 3)], paint=limb, children=[
            leg("left", 1, False), leg("right", -1, True),
            P("body", (0, -1, 0), rot=(0.08, 0, 0), boxes=[B(-3, -10, -1.5, 6, 10, 3)], paint=torso, children=[
                arm("left", 1, True), arm("right", -1, False),
                P("neck", (0, -10, 0), boxes=[B(-1, -2, -1, 2, 2, 2)], paint=limb, children=[
                    P("head", (0, -2, 0), boxes=[B(-3, -7, -3, 6, 7, 6)], paint=head, children=[
                        # The mouth is all that is left of the face.
                        P("mouth", (0.4, -2.6, -3.05), boxes=[B(-3, 0, 0, 6, 3, 0)], paint=mouth),
                        P("lower_lip", (0.4, -1.2, -3.1), boxes=[B(-2, 0, 0, 4, 2, 0)], paint=lower_lip),
                        P("head_guide", (1.2, -4.6, -3.4), rot=(0, 0, 0.12), boxes=[B(-5, -5.5, 0, 10, 11, 0)], paint=ring),
                    ]),
                ]),
            ]),
            P("centre_line", (0, 0, -1.8), boxes=[B(-0.5, -24, 0, 1, 38, 0)], paint=guide),
        ]),
    ]
    anim = HEAD_LOOK.format(head="head") + """        float a = Math.min(1.0F, limbSwingAmount);
        float w = limbSwing * 0.6662F;
        boolean fixated = entity.isFixated();
        leftLeg.xRot += Mth.cos(w) * 1.1F * a;
        rightLeg.xRot += Mth.cos(w + Mth.PI) * 1.1F * a;
        leftShin.xRot += Math.max(0.0F, Mth.sin(w)) * 0.8F * a;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(w)) * 0.8F * a;
        leftArm.xRot += Mth.cos(w + Mth.PI) * 0.5F * a;
        rightArm.xRot += Mth.cos(w) * 0.5F * a;
        // It droops, the head too heavy on one side.
        body.xRot += 0.06F;
        head.zRot += fixated ? 0.0F : 0.28F + Mth.sin(ageInTicks * 0.03F) * 0.06F;
        head.xRot += fixated ? 0.0F : 0.15F;
        leftForearm.zRot += Mth.sin(ageInTicks * 0.05F) * 0.05F;
        // Murmuring: the mouth never stops.
        float talk = fixated ? 1.1F : 0.55F;
        lowerLip.y += Mth.abs(Mth.sin(ageInTicks * talk)) * 1.1F + Mth.abs(Mth.sin(ageInTicks * talk * 2.7F)) * 0.4F;
        // Every so often the drawing is redrawn a hair out of place.
        float redraw = Mth.sin(ageInTicks * 0.7F) > 0.93F ? 0.35F : 0.0F;
        body.x += redraw;
        head.x -= redraw * 1.5F;
        headGuide.x += redraw * 2.0F;
        centreLine.x -= redraw;
        headGuide.zRot += Mth.sin(ageInTicks * 0.02F) * 0.05F;
        if (fixated) {
            // Following you: arms lifting a little towards you, as if to ask something.
            leftArm.xRot -= 0.5F + Mth.sin(ageInTicks * 0.05F) * 0.1F;
            rightArm.xRot -= 0.35F;
            leftForearm.xRot -= 0.3F;
        }
"""
    return Model("smudge", "SmudgeModel", E + "SmudgeEntity", (64, 64), parts, anim, translucent=True, alpha=0.7)


# ================================================================ the Redacted
R_PAPER = (220, 214, 198)
R_PAPER_D = (184, 178, 162)
R_TYPE = (84, 80, 86)
R_BAR = (8, 8, 10)
R_BAR_L = (40, 40, 46)
R_STAMP = (170, 40, 34)
R_EYE = (232, 226, 210)
R_IRIS = (40, 26, 22)
R_VEIN = (170, 40, 40)
R_STEEL = (150, 156, 168)
R_STEEL_L = (214, 218, 226)


def typed(base, type_c, line_gap=2, seed=0, margin=1):
    """A typed page: rows of short grey word-marks with ragged line ends."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                canvas.set(u + x, v + y, jitter(base, 3, r))
        for y in range(1, h - 1, line_gap):
            end = w - margin - (_hash(y, seed, u) % 3)
            x = margin
            while x < end:
                ln = 1 + _hash(x, y, seed) % 3
                for i in range(ln):
                    if x + i < end:
                        canvas.blend(u + x + i, v + y, type_c, 0.75)
                x += ln + 1
    return fn


def bar_paint():
    return dict(base=R_BAR, noise=2, shade=False, features={"all": [("draw", gloss(R_BAR, R_BAR_L))]})


def redacted():
    """
    A person-shaped file. It is made of typed pages, and whoever had it last went over it with a
    black marker: bars across the eyes, the mouth, the chest, one whole arm. The bars do not sit
    still. When the one over the eyes slides, there are more eyes under it than there should be,
    and they are looking at you. In its hand, a letter-opener, red along the edge.
    """
    page = dict(base=R_PAPER, noise=3, rim=(R_PAPER_D, 0.3), features={"all": [("draw", typed(R_PAPER, R_TYPE, 2, 1))]})
    chest = dict(base=R_PAPER, noise=3, rim=(R_PAPER_D, 0.3), features={
        "all": [("draw", typed(R_PAPER, R_TYPE, 2, 2))],
        # A rubber stamp, half-inked.
        "front": [("ascii", ["", "", "", "", "", ".ss.s", "s...s", "s.s.s", ".sss."], {"s": R_STAMP})]})
    # Under the bar across the eyes: too many of them, bloodshot, awake.
    face = dict(base=R_PAPER, noise=3, rim=(R_PAPER_D, 0.3), features={
        "all": [("draw", typed(R_PAPER, R_TYPE, 2, 3))],
        "front": [("ascii", [
            "......",
            "EIvEIE",
            "vEIEvI",
            "IEvIEv",
        ], {"E": (R_EYE, "glow"), "I": (R_IRIS, "glow"), "v": (R_VEIN, "glow")})]})
    sheet = dict(base=R_PAPER_D, noise=3, shade=False, shape=ragged(2, seed=31),
                 features={"all": [("draw", typed(R_PAPER_D, R_TYPE, 2, 4))]})
    limb = dict(base=R_PAPER, noise=3, rim=(R_PAPER_D, 0.3), features={"all": [("draw", typed(R_PAPER, R_TYPE, 3, 5))]})
    blacked = bar_paint()
    blade = dict(base=R_STEEL, noise=3, shade=False, features={
        "all": [("vstripes", 99, R_STEEL)],
        "front": [("ascii", ["L", "L", "L", "L", "r", "L", "r", "r"], {"L": R_STEEL_L, "r": (150, 30, 30)})],
        "right": [("ascii", ["L.", "L.", "Lr", "rr", "L.", "r.", "rr", "r."], {"L": R_STEEL_L, "r": (150, 30, 30)})]})
    handle = dict(base=(60, 44, 36), noise=4)

    def bar(name, pivot, box, rot=(0, 0, 0)):
        return P(name, pivot, rot=rot, boxes=[box], paint=blacked)

    def leg(side, sx):
        m = sx < 0
        return P(f"{side}_leg", (1.6 * sx, 0, 0), boxes=[B(-1, 0, -1, 2, 6, 2, mirror=m)], paint=limb, children=[
            P(f"{side}_shin", (0, 6, 0), boxes=[B(-1, 0, -1, 2, 6, 2, mirror=m)], paint=limb, children=[
                P(f"{side}_foot", (0, 6, 0), boxes=[B(-1, 0, -2.5, 2, 0.5, 3.5, mirror=m)], paint=page)] + (
                [bar("knee_bar", (0, -0.5, 0), B(-1.5, -1, -1.5, 3, 2, 3))] if sx > 0 else []))])

    parts = [
        P("hips", (0, 12, 0), boxes=[B(-3, -1, -1.5, 6, 2, 3)], paint=page, children=[
            leg("left", 1), leg("right", -1),
            P("body", (0, -1, 0), boxes=[B(-3.5, -11, -1.5, 7, 11, 3)], paint=chest, children=[
                P("loose_sheet", (0.4, -10.5, -1.65), rot=(0, 0, -0.06), boxes=[B(-3.5, 0, 0, 7, 8, 0)], paint=sheet),
                bar("bar_chest", (0, -7, -2.2), B(-5, -1, -0.5, 10, 2, 1), rot=(0, 0, 0.12)),
                bar("bar_waist", (0, -2.5, -2.1), B(-4.5, -1, -0.5, 8, 2, 1), rot=(0, 0, -0.05)),
                # The whole left arm is struck out.
                P("left_arm", (4.5, -10, 0), rot=(0.05, 0, -0.08), boxes=[B(-1, -1, -1, 2, 11, 2)], paint=blacked, children=[
                    P("left_forearm", (0, 10, 0), boxes=[B(-1, 0, -1, 2, 10, 2)], paint=blacked)]),
                P("right_arm", (-4.5, -10, 0), rot=(0.05, 0, 0.08), boxes=[B(-1, -1, -1, 2, 11, 2, mirror=True)], paint=limb, children=[
                    P("right_forearm", (0, 10, 0), boxes=[B(-1, 0, -1, 2, 9, 2, mirror=True)], paint=limb, children=[
                        P("knife", (0, 9, -0.5), rot=(1.35, 0, 0), boxes=[B(-0.5, -1, -0.5, 1, 3, 1, paint=handle),
                                                                             B(-0.5, 2, -0.5, 1, 8, 0.5, paint=blade)])])]),
                P("neck", (0, -11, 0), boxes=[B(-1, -2, -1, 2, 2, 2)], paint=limb, children=[
                    P("head", (0, -2, 0), boxes=[B(-3, -8, -1.5, 6, 8, 3)], paint=face, children=[
                        bar("bar_eyes", (0, -5.5, -1.6), B(-4.5, -2, -1, 9, 4, 1)),
                        bar("bar_mouth", (0, -1.5, -1.8), B(-3.5, -1, -0.5, 7, 2, 1), rot=(0, 0, -0.08)),
                    ]),
                ]),
            ]),
        ]),
    ]
    anim = HEAD_LOOK.format(head="head") + """        float a = Math.min(1.0F, limbSwingAmount);
        float w = limbSwing * 0.6662F;
        boolean hunting = entity.isAggressive();
        leftLeg.xRot += Mth.cos(w) * 1.0F * a;
        rightLeg.xRot += Mth.cos(w + Mth.PI) * 1.0F * a;
        leftShin.xRot += Math.max(0.0F, Mth.sin(w)) * 0.7F * a;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(w)) * 0.7F * a;
        leftArm.xRot += Mth.cos(w + Mth.PI) * 0.4F * a;
        rightArm.xRot += Mth.cos(w) * 0.4F * a;
        // It moves in jerks, like a page being turned.
        float jerk = (float) Math.floor(ageInTicks * 0.25F);
        head.zRot += Mth.sin(jerk * 1.7F) * 0.12F;
        // The bars will not keep still: they twitch, and the one over the eyes slides off them
        // for a moment every so often (for longer when it is coming for you).
        barChest.x += Mth.sin(ageInTicks * 0.9F) * 0.25F;
        barWaist.x -= Mth.sin(ageInTicks * 0.7F + 1.0F) * 0.3F;
        barMouth.x += Mth.sin(jerk * 2.3F) * 0.3F;
        kneeBar.x += Mth.sin(ageInTicks * 0.6F) * 0.2F;
        float reveal = Mth.sin(ageInTicks * 0.045F);
        float slide = reveal > (hunting ? 0.3F : 0.85F) ? (reveal - (hunting ? 0.3F : 0.85F)) * (hunting ? 9.0F : 30.0F) : 0.0F;
        barEyes.x += Math.min(4.5F, slide) + Mth.sin(ageInTicks * 1.1F) * 0.15F;
        looseSheet.zRot += Mth.sin(ageInTicks * 0.08F) * 0.04F + Mth.sin(w) * 0.05F * a;
        if (hunting) {
            rightArm.xRot -= 0.9F;
            rightForearm.xRot -= 0.4F;
            head.xRot -= 0.15F;
        }
        if (attackTime > 0.0F) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightArm.xRot -= 1.4F * s;
            rightArm.yRot += 0.4F * s;
            body.yRot += 0.3F * s;
        }
"""
    return Model("redacted", "RedactedModel", E + "RedactedEntity", (64, 64), parts, anim)


# ================================================================ the Rubricator
U_ROBE = (128, 26, 20)
U_ROBE_D = (86, 16, 12)
U_ROBE_L = (158, 44, 30)
U_SCRAPED = (232, 228, 216)
U_SCRAPE_D = (204, 198, 184)
U_RED = (178, 34, 24)
U_GILT = (196, 154, 66)
U_ROPE = (150, 128, 90)
U_HAND = (184, 150, 130)
U_HAND_RED = (150, 40, 30)


def folds(base, dark, light, period=3, seed=0):
    """Heavy cloth hanging in vertical folds, lit on one side of each."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                k = (x + _hash(y // 4, seed) % 2) % period
                c = dark if k == 0 else (light if k == 1 else base)
                canvas.set(u + x, v + y, jitter(mix(base, c, 0.4), 3, r))
    return fn


def scraped(base, dark, seed=0):
    """Parchment scraped back with a knife: long horizontal strokes, lighter and darker."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            k = _hash(y, seed) % 5
            row = mix(base, dark, 0.35) if k == 0 else (mix(base, (250, 248, 240), 0.4) if k == 1 else base)
            for x in range(w):
                canvas.set(u + x, v + y, jitter(row, 3, r))
    return fn


def rubricator():
    """
    The last scribe of the first draft. His name was scraped off the page and his face went with
    it: under the hood there is only blank, knife-scraped parchment, and across it, in the red
    that does not scrape away, the one letter he had been writing when it happened. Two points of
    that red still burn where his eyes were. His hands are red to the wrist, and one of them never
    stops writing.
    """
    robe = dict(base=U_ROBE, noise=3, features={"all": [("draw", folds(U_ROBE, U_ROBE_D, U_ROBE_L, 4, 1))],
                                                 "front": [("script", U_ROBE_L, 0.25)]})
    skirt = dict(base=U_ROBE, noise=4, shape=ragged(2, holes=1, seed=41), features={
        "all": [("draw", folds(U_ROBE, U_ROBE_D, U_ROBE_L, 4, 2))],
        "sides": [("ascii", [""] * 11 + ["gGgGgGgGg"], {"g": U_GILT, "G": (150, 112, 44)})]})
    hood = dict(base=U_ROBE_D, noise=4, features={"all": [("draw", folds(U_ROBE_D, (60, 10, 8), U_ROBE, 3, 3))]})
    face = dict(base=U_SCRAPED, noise=2, features={
        "all": [("draw", scraped(U_SCRAPED, U_SCRAPE_D, 4))],
        # A red initial where the face was, and two embers of it where the eyes were.
        "front": [("ascii", [
            ".......",
            ".......",
            ".e...e.",
            ".r...R.",
            ".R...r.",
            "RRRRRRR",
            ".r...R.",
        ], {"R": U_RED, "r": (150, 30, 22), "e": ((255, 110, 60), "glow")})]})
    sleeve = dict(base=U_ROBE, noise=4, features={"all": [("draw", folds(U_ROBE, U_ROBE_D, U_ROBE_L, 2, 5))],
                                                   "bottom": [("draw", fill((40, 8, 6), 2))]})
    hand = dict(base=U_HAND, noise=4, features={"all": [("draw", wicking(U_HAND_RED, 0.7))]})
    rope = dict(base=U_ROPE, noise=6, features={"all": [("vstripes", 2, (116, 96, 64))]})
    quill = dict(base=(236, 232, 220), noise=4, shade=False, features={
        "all": [("ascii", ["r", "r", "", "", "", "", "", "", ""], {"r": ((230, 60, 40), "glow")})]})
    vane = dict(base=(230, 226, 214), noise=5, shade=False, shape=ragged(1, seed=43))
    book = dict(base=(70, 40, 28), noise=5, features={"front": [("rect", 1, 1, -1, -1, U_GILT)], "top": [("draw", fill((226, 214, 184), 3))]})
    pot = dict(base=(40, 38, 44), noise=3, features={"top": [("draw", fill((120, 20, 16), 2))]})
    foot = dict(base=(58, 40, 30), noise=4)

    parts = [
        P("skirt", (0, 12, 0), boxes=[B(-4.5, 0, -3, 9, 12, 6)], paint=skirt, children=[
            P("left_foot", (1.8, 11.5, -2.8), boxes=[B(-1, 0, -1, 2, 0.5, 2)], paint=foot),
            P("right_foot", (-1.8, 11.5, -2.8), boxes=[B(-1, 0, -1, 2, 0.5, 2)], paint=foot),
        ]),
        P("body", (0, 12, 0), rot=(0.18, 0, 0), boxes=[B(-4, -12, -2.5, 8, 12, 5)], paint=robe, children=[
            P("belt", (0, -1, 0), boxes=[B(-4, 0, -2.5, 8, 1, 5, inflate=0.25)], paint=rope, children=[
                P("rope_end", (2.5, 1, -2.7), rot=(0, 0, 0.1), boxes=[B(-0.5, 0, -0.5, 1, 6, 1)], paint=rope),
                P("inkpot", (-3, 1, -2.4), boxes=[B(-1, 0, -1, 2, 2, 2)], paint=pot),
                P("chained_book", (4.3, 1, 0), rot=(0, 0, -0.12), boxes=[B(0, 0, -2, 1, 4, 3)], paint=book)]),
            P("head", (0, -12, -0.5), rot=(0.2, 0, 0), boxes=[B(-3.5, -7, -3.5, 7, 7, 7)], paint=face, children=[
                P("hood_top", (0, 0, 0), boxes=[B(-4.5, -8.5, -4.5, 9, 1.5, 9)], paint=hood),
                P("hood_left", (0, 0, 0), boxes=[B(3.5, -7.5, -4.5, 1, 8.5, 9)], paint=hood),
                P("hood_right", (0, 0, 0), boxes=[B(-4.5, -7.5, -4.5, 1, 8.5, 9, mirror=True)], paint=hood),
                P("hood_back", (0, 0, 0), boxes=[B(-3.5, -7.5, 3.5, 7, 8.5, 1)], paint=hood),
                P("hood_tail", (0, -5, 4.4), rot=(-0.25, 0, 0), boxes=[B(-1.5, 0, 0, 3, 12, 1)], paint=hood),
            ]),
            P("left_arm", (4.8, -11, 0), rot=(-0.5, 0, -0.1), boxes=[B(-1.5, -1, -2, 3, 9, 4)], paint=sleeve, children=[
                P("left_hand", (0, 8, -0.5), boxes=[B(-1, 0, -1, 2, 2, 2)], paint=hand, children=[
                    P("held_book", (0, 1.5, -1), rot=(0.9, 0, 0), boxes=[B(-2.5, 0, -1, 5, 4, 1)], paint=book)])]),
            P("right_arm", (-4.8, -11, 0), rot=(-0.95, 0, 0.15), boxes=[B(-1.5, -1, -2, 3, 9, 4, mirror=True)], paint=sleeve, children=[
                P("right_hand", (0, 8, -0.5), boxes=[B(-1, 0, -1, 2, 2, 2, mirror=True)], paint=hand, children=[
                    P("quill", (0, 1, -0.5), rot=(0.4, 0, 0.3), boxes=[B(-0.5, -8, -0.5, 1, 9, 1)], paint=quill, children=[
                        P("quill_vane", (0, -8, 0), boxes=[B(0, 0, -0.5, 0, 6, 2)], paint=vane)])])]),
        ]),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.8F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.6F;
        float a = Math.min(1.0F, limbSwingAmount);
        float sway = Mth.sin(ageInTicks * 0.05F) * 0.03F;
        body.zRot += sway;
        skirt.zRot += sway * 0.4F;
        skirt.xRot += Mth.cos(limbSwing * 0.6662F) * 0.12F * a;
        leftFoot.xRot += Mth.cos(limbSwing * 0.6662F) * 0.6F * a;
        rightFoot.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.6F * a;
        hoodTail.xRot += Mth.sin(ageInTicks * 0.06F) * 0.05F + a * 0.2F;
        ropeEnd.xRot += Mth.cos(limbSwing * 0.6662F) * 0.2F * a;
        chainedBook.zRot += Mth.cos(limbSwing * 0.6662F) * 0.1F * a;
        // Always writing: the quill hand traces letters in the air, pauses, starts again.
        float write = Mth.sin(ageInTicks * 0.021F) > -0.3F ? 1.0F : 0.0F;
        rightArm.xRot += Mth.sin(ageInTicks * 0.45F) * 0.08F * write;
        rightArm.yRot += Mth.cos(ageInTicks * 0.3F) * 0.1F * write;
        rightHand.zRot += Mth.sin(ageInTicks * 0.6F) * 0.2F * write;
        head.xRot += 0.15F * write;
"""
    return Model("rubricator", "RubricatorModel", E + "RubricatorEntity", (128, 64), parts, anim)


# ================================================================ the Quillcrow
Q_BLACK = (22, 21, 28)
Q_SHEEN = (58, 62, 96)
Q_SHAFT = (226, 220, 204)
Q_STEEL = (126, 132, 146)
Q_STEEL_L = (206, 212, 222)
Q_STEEL_D = (60, 62, 72)
Q_EYE = (214, 222, 214)


def feathers(base, sheen, dark, seed=0):
    """Overlapping rows of black feathers, each edged with a little blue-violet sheen."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                k = (x + (y // 2) % 2) % 3
                c = sheen if (y % 2 == 0 and k == 0) else (dark if k == 2 else base)
                canvas.set(u + x, v + y, jitter(mix(base, c, 0.45), 3, r))
    return fn


def quill_vane(name, x, y, w, h):
    """A quill feather seen flat: a thin shaft with a vane that narrows to the tip."""
    if name in ("top", "bottom"):
        width = w if y < h - 2 else max(1, w - 1)
        return 255 if x < width else 0
    return 255


def quillcrow():
    """
    A crow whose flight feathers are quill pens, shafts white and tips black with the ink they were
    dipped in, and whose beak is a steel nib, split down the middle and wet. Its eyes are milky and
    it does not blink. It watches from high places, and its head moves the way a crow's does,
    in snaps, always ending up pointed at you.
    """
    body = dict(base=Q_BLACK, noise=3, features={"all": [("draw", feathers(Q_BLACK, Q_SHEEN, (10, 10, 14), 1))]})
    head = dict(base=Q_BLACK, noise=3, features={
        "all": [("draw", feathers(Q_BLACK, Q_SHEEN, (10, 10, 14), 2))],
        "right": [("ascii", ["", ".E.", ""], {"E": (Q_EYE, "glow")})],
        "left": [("ascii", ["", ".E.", ""], {"E": (Q_EYE, "glow")})],
        "front": [("ascii", ["", "e.e", ""], {"e": (Q_EYE, "glow")})]})
    nib = dict(base=Q_STEEL, noise=3, shade=True, features={
        "top": [("ascii", ["L.L", "LoL", "L.L"], {"L": Q_STEEL_L, "o": Q_STEEL_D, ".": Q_STEEL_D})],
        "front": [("ascii", [".d."], {"d": Q_STEEL_D})]})
    nib_tip = dict(base=Q_STEEL, noise=3, features={"top": [("ascii", ["d", "d", "d"], {"d": Q_STEEL_D})],
                                                    "front": [("draw", fill((16, 16, 22), 2))]})
    wing = dict(base=Q_BLACK, noise=3, features={"all": [("draw", feathers(Q_BLACK, Q_SHEEN, (10, 10, 14), 3))]})
    quill = dict(base=Q_SHAFT, noise=3, shade=False, shape=quill_vane, features={
        "top": [("draw", fill((30, 29, 36), 3)), ("ascii", ["s", "s", "s", "s", "s", "s"], {"s": Q_SHAFT}),
                ("ascii", ["", "", "", "", "kk", "kk"], {"k": (8, 8, 12)})],
        "bottom": [("draw", fill((30, 29, 36), 3)), ("ascii", ["s", "s", "s", "s", "s", "s"], {"s": Q_SHAFT})]})
    leg = dict(base=(46, 42, 40), noise=3)
    drip = dict(base=(10, 10, 14), noise=2, shade=False, shape=drip_shape(9))

    def wing_part(side, sx):
        m = sx < 0
        return P(f"{side}_wing", (2 * sx, -1.8, -2), rot=(-0.05, 0, 0), boxes=[B(0 if sx > 0 else -1, 0, 0, 1, 4, 7, mirror=m)], paint=wing, children=[
            P(f"{side}_quill_{i}", (0.5 * sx, 1 + i, 6.5), rot=(-0.3 - i * 0.12, 0.08 * sx * i, 0), boxes=[B(-1 if sx < 0 else 0, 0, 0, 2, 0, 6)], paint=quill)
            for i in range(3)])

    parts = [
        P("body", (0, 19.5, 0), rot=(0.3, 0, 0), boxes=[B(-2, -2, -3, 4, 4, 7)], paint=body, children=[
            P("head", (0, -2, -3), rot=(-0.3, 0, 0), boxes=[B(-1.5, -3, -2.5, 3, 3, 3)], paint=head, children=[
                P("ruff", (0, 0, 0.5), boxes=[B(-2, -1.5, -1.5, 4, 2, 3)], paint=body),
                P("beak", (0, -1.5, -2.5), boxes=[B(-1, -0.5, -2, 2, 1, 2)], paint=nib, children=[
                    P("beak_tip", (0, 0, -2), boxes=[B(-0.5, -0.5, -2, 1, 1, 2)], paint=nib_tip, children=[
                        P("beak_drip", (0, 0.5, -1.6), boxes=[B(0, 0, -0.5, 0, 2, 1)], paint=drip)])])]),
            wing_part("left", 1), wing_part("right", -1),
            P("tail", (0, -1, 4), rot=(-0.55, 0, 0), boxes=[B(-1.5, 0, 0, 3, 1, 2)], paint=body, children=[
                P(f"tail_quill_{i}", (i - 1, 0.5, 1.5), rot=(0, (i - 1) * 0.25, 0), boxes=[B(-1, 0, 0, 2, 0, 6)], paint=quill)
                for i in range(3)]),
        ]),
        P("left_leg", (1, 21, 0.5), boxes=[B(-0.5, 0, -0.5, 1, 3, 1)], paint=leg, children=[
            P("left_talons", (0, 2.5, 0), boxes=[B(-1, 0, -1.5, 2, 0.5, 2)], paint=leg)]),
        P("right_leg", (-1, 21, 0.5), boxes=[B(-0.5, 0, -0.5, 1, 3, 1)], paint=leg, children=[
            P("right_talons", (0, 2.5, 0), boxes=[B(-1, 0, -1.5, 2, 0.5, 2)], paint=leg)]),
    ]
    anim = """        // A crow's head: it holds still, then snaps to a new angle, always coming back to you.
        float snap = (float) Math.floor(ageInTicks / 9.0F);
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD + Mth.sin(snap * 2.7F) * 0.35F;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        head.zRot += Mth.sin(snap * 1.3F) > 0.6F ? 0.7F : 0.0F;
        beakDrip.zRot += Mth.sin(ageInTicks * 0.2F) * 0.2F;
        if (entity.isFlying()) {
            float flap = Mth.cos(ageInTicks * 1.6F) * 1.1F;
            leftWing.zRot -= 0.3F + flap;
            rightWing.zRot += 0.3F + flap;
            leftQuill2.xRot -= flap * 0.2F;
            rightQuill2.xRot -= flap * 0.2F;
            leftLeg.xRot += 1.2F;
            rightLeg.xRot += 1.2F;
            tail.xRot += 0.2F;
        } else {
            leftLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
            rightLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * limbSwingAmount;
            // Now and then it rouses its feathers.
            float rouse = Mth.sin(ageInTicks * 0.05F) > 0.97F ? Mth.sin(ageInTicks * 2.0F) * 0.15F : 0.0F;
            leftWing.zRot -= rouse;
            rightWing.zRot += rouse;
            ruff.y -= Math.abs(rouse) * 2.0F;
        }
"""
    return Model("quillcrow", "QuillcrowModel", E + "QuillcrowEntity", (64, 32), parts, anim)


# ================================================================ the Foxing Moth
M_WING = (206, 194, 164)
M_WING_D = (170, 150, 116)
M_FOX = (150, 104, 62)
M_BODY = (136, 108, 78)


def moth_wing_shape(face, x, y, w, h):
    if face not in ("top", "bottom"):
        return 0
    cx, cy = (0 if face == "top" else w - 1), h * 0.45
    return 255 if ((x - cx) / (w * 0.92)) ** 2 + ((y - cy) / (h * 0.56)) ** 2 < 1.0 else 0


def foxing_moth():
    """
    A pale moth freckled brown like old paper. On each forewing, where other moths have a false
    eye, it has a real-looking one: a human eye, lid and lashes, the iris the brown of foxing.
    When it settles with its wings spread, two eyes look at you from the wall.
    """
    eye = {"W": (228, 222, 206), "I": (104, 66, 36), "P": (14, 12, 12), "l": (120, 92, 70), "L": (72, 50, 38)}

    def wing(side_x):
        # The eye sits towards the body end of the wing.
        rows = [
            ".......",
            "..lLLl.",
            ".lWIIWl",
            ".LWIPWL",
            ".lWIIWl",
            "..lLLl.",
            ".......",
        ]
        if side_x < 0:
            rows = [r[::-1] for r in rows]
        return dict(base=M_WING, noise=6, shade=False, shape=moth_wing_shape, features={
            "all": [("spots", M_FOX, 4, 1.4), ("spots", M_WING_D, 3, 1.0)],
            "top": [("ascii", rows, eye)], "bottom": [("ascii", rows, eye)]})

    fur = dict(base=M_BODY, noise=10, features={"all": [("hstripes", 2, (110, 84, 58))]})
    antenna = dict(base=(96, 74, 52), shade=False, shape=lambda face, x, y, w, h: 255 if face in ("front", "back") and (x in (0, w - 1) or y == 0) else 0)
    leg = dict(base=(90, 70, 50), noise=3)
    parts = [
        P("body", (0, 20, 0), boxes=[B(-1, -1, -3, 2, 2, 5)], paint=fur, children=[
            P("head", (0, 0, -3), boxes=[B(-1, -1, -2, 2, 2, 2)], paint=dict(base=(120, 94, 66), noise=8,
              features={"front": [("ascii", ["e.e"], {"e": (20, 16, 16)})]}), children=[
                P("antennae", (0, -1, -2), rot=(-0.5, 0, 0), boxes=[B(-2, -3, 0, 4, 3, 0)], paint=antenna)]),
            P("left_wing", (1, -1, -1), boxes=[B(0, 0, -3, 7, 0, 7)], paint=wing(1)),
            P("right_wing", (-1, -1, -1), boxes=[B(-7, 0, -3, 7, 0, 7, mirror=True)], paint=wing(-1)),
            P("legs", (0, 1, -1), boxes=[B(-1.5, 0, -1, 3, 1, 0), B(-1.5, 0, 0.5, 3, 1, 0), B(-1.5, 0, 2, 3, 1, 0)], paint=leg)]),
    ]
    anim = """        float flap = Mth.cos(ageInTicks * 1.3F) * 0.9F;
        leftWing.zRot += -0.2F + flap;
        rightWing.zRot -= -0.2F + flap;
        body.y += Mth.sin(ageInTicks * 0.2F) * 0.6F;
        antennae.xRot += Mth.sin(ageInTicks * 0.3F) * 0.1F;
"""
    return Model("foxing_moth", "FoxingMothModel", E + "FoxingMothEntity", (32, 32), parts, anim)


# ================================================================ the Blotling
B_INK = (12, 11, 17)
B_SHEEN = (70, 76, 118)
B_HI = (170, 176, 210)
B_EYE = (222, 218, 204)
B_VEIN = (150, 36, 36)
B_TOOTH = (196, 188, 168)


def wet(base, sheen, hi, seed=0):
    """Wet ink: black with soft sheen and a few bright reflections, as if lit from above."""
    def fn(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = base
                n = _hash(u + x, v + y, seed) % 100
                if n < 12:
                    c = mix(base, sheen, 0.45)
                canvas.set(u + x, v + y, jitter(c, 2, r))
        for _ in range(max(1, (w * h) // 30)):
            x, y = r.randrange(w), r.randrange(max(1, h // 2))
            canvas.blend(u + x, v + y, hi, 0.6)
    return fn


def splat(name, x, y, w, h):
    """A puddle: a lumpy disc on the top and bottom faces."""
    if name not in ("top", "bottom"):
        return 0
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    ang = math.atan2(y - cy, x - cx)
    rad = 0.78 + 0.18 * math.sin(ang * 5 + 1.0) + 0.08 * math.sin(ang * 11)
    return 255 if math.hypot((x - cx) / (w / 2.0), (y - cy) / (h / 2.0)) < rad else 0


def blotling():
    """
    A drop of ink that decided to move: a wet, lumpy heap of it with a puddle spreading round its
    base. Eyes have surfaced in it here and there, no two the same size, each looking somewhere
    else, and a mouth has opened across the front, lined with little teeth. When it splits, each
    half keeps some of them.
    """
    ink = dict(base=B_INK, noise=2, features={"all": [("draw", wet(B_INK, B_SHEEN, B_HI, 1))]})
    front = dict(base=B_INK, noise=2, features={
        "all": [("draw", wet(B_INK, B_SHEEN, B_HI, 2))],
        "front": [("ascii", ["", "", "tTtTtTt.", "MMMMMMMM", ".tTtTtTt"], {"t": B_TOOTH, "T": (150, 142, 124), "M": (60, 10, 14)}, (0, 0))]})
    film = dict(base=(40, 42, 70), noise=4, alpha=90, shade=False)

    def eye(name, pivot, size):
        n = size
        rows = {1: ["P"], 2: ["WP", "vW"], 3: ["vWv", "WPW", "vWv"]}[n]
        return P(name, pivot, boxes=[B(-n / 2.0, -n / 2.0, -0.5, n, n, 1)], paint=dict(
            base=B_EYE, noise=3, shade=False, features={"all": [("draw", fill_glow(B_EYE))],
                                                          "front": [("ascii", rows, {"W": (B_EYE, "glow"), "P": ((12, 10, 12), "glow"), "v": (B_VEIN, "glow")})]}), children=[
            # The lid rests above the eye (black on black, unseen) and drops over it to blink.
            P(name + "_lid", (0, -n / 2.0 - n, -0.55), boxes=[B(-n / 2.0, 0, 0, n, n, 0)], paint=dict(base=B_INK, noise=2, shade=False))])

    parts = [
        P("puddle", (0, 24, 0), boxes=[B(-7, -0.05, -7, 14, 0, 14)], paint=dict(base=B_INK, noise=2, shape=splat,
          features={"all": [("draw", wet(B_INK, B_SHEEN, B_HI, 3))]})),
        P("core", (0, 24, 0), boxes=[B(-5, -2, -5, 10, 2, 10, paint=ink), B(-4, -6.5, -4, 8, 5, 8, paint=front),
                                     B(-3.5, -9, -2.5, 5, 3, 5, paint=ink), B(0.5, -10, -0.5, 3, 2, 3, paint=ink),
                                     B(3.5, -4, -2, 2, 3, 4, paint=ink), B(-5.5, -3.5, 0, 2, 2, 3, paint=ink)], children=[
            eye("eye_0", (-2, -4.2, -4.3), 2),
            eye("eye_1", (1.6, -5, -4.2), 1),
            eye("eye_2", (-0.5, -7.5, -3.3), 3),
            eye("eye_3", (3.8, -2.6, -2.2), 1),
            P("maw", (0, -2.3, -4), boxes=[B(-3.5, 0, -0.2, 7, 1.5, 1)], paint=dict(base=(60, 10, 14), noise=2, features={
                "top": [("draw", teeth_row(B_TOOTH, (60, 10, 14)))], "front": [("draw", wet(B_INK, B_SHEEN, B_HI, 4))]})),
        ]),
        P("shell", (0, 24, 0), boxes=[B(-5.5, -7, -5.5, 11, 7, 11)], paint=film),
    ]
    anim = """        core.y += Mth.sin(ageInTicks * 0.2F) * 0.3F;
        // Each eye wanders on its own; now and then one blinks.
        eye0.x += Mth.sin(ageInTicks * 0.07F) * 0.3F;
        eye1.y += Mth.sin(ageInTicks * 0.09F + 1.0F) * 0.3F;
        eye2.x += Mth.sin(ageInTicks * 0.05F + 2.0F) * 0.4F;
        eye3.y += Mth.sin(ageInTicks * 0.06F + 3.0F) * 0.2F;
        eye0Lid.y += Mth.sin(ageInTicks * 0.13F) > 0.93F ? 2.0F : 0.0F;
        eye1Lid.y += Mth.sin(ageInTicks * 0.11F + 2.0F) > 0.93F ? 1.0F : 0.0F;
        eye2Lid.y += Mth.sin(ageInTicks * 0.09F + 4.0F) > 0.9F ? 3.0F : 0.0F;
        eye3Lid.y += Mth.sin(ageInTicks * 0.15F + 1.0F) > 0.93F ? 1.0F : 0.0F;
        // The mouth works, slowly, all the time.
        maw.y += Math.max(0.0F, Mth.sin(ageInTicks * 0.08F)) * 1.2F;
        shell.y += Mth.sin(ageInTicks * 0.2F + 0.5F) * 0.3F;
"""
    return Model("blotling", "BlotlingModel", E + "BlotlingEntity", (128, 64), parts, anim, translucent=True)


# ================================================================ the Margin Crawler
C_FLESH = (218, 196, 180)
C_FLESH_D = (184, 156, 140)
C_SHELL = (214, 200, 164)
C_SHELL_D = (170, 150, 110)
C_INK = (58, 42, 34)
C_STEEL = (140, 146, 158)
C_STEEL_L = (206, 210, 220)
C_STEEL_D = (70, 72, 82)
C_RED = (160, 34, 26)


def spiral(ink, turns=2.2):
    """The shell's whorl, drawn in ink on each side face."""
    def fn(canvas, glow, u, v, w, h, r):
        cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
        steps = 120
        for i in range(steps):
            t = i / steps
            ang = t * turns * 2 * math.pi
            rad = t * min(w, h) / 2.0
            x, y = int(round(cx + math.cos(ang) * rad)), int(round(cy + math.sin(ang) * rad))
            if 0 <= x < w and 0 <= y < h:
                canvas.blend(u + x, v + y, ink, 0.85)
    return fn


def margin_crawler():
    """
    A doodle from the margin of an old book: a snail in a knight's helmet, with a lance too long for
    it. The monk who drew it gave it a man's face under the visor, grinning, and a man's eyes on
    the ends of its stalks, and it goes along on a fringe of little pale hands.
    """
    flesh = dict(base=C_FLESH, noise=4, rim=(C_FLESH_D, 0.3), features={"all": [("draw", skin(C_FLESH, C_FLESH_D, (232, 214, 200)))],
                                                                         "top": [("draw", gloss(C_FLESH, (240, 232, 226)))]})
    shell = dict(base=C_SHELL, noise=5, rim=(C_SHELL_D, 0.3), features={
        "all": [("script", C_INK, 0.25)], "right": [("draw", spiral(C_INK))], "left": [("draw", spiral(C_INK))]})
    helmet = dict(base=C_STEEL, noise=4, features={
        "all": [("vstripes", 3, C_STEEL_D)], "top": [("ascii", [".o..o.", "", "", ""], {"o": (20, 20, 24)})]})
    # Under the visor: a grinning face, drawn in brown ink.
    face = dict(base=C_FLESH, noise=3, features={"front": [("ascii", [
        ".e..e.",
        "......",
        "mTTTTm",
        ".mmmm.",
    ], {"e": (40, 30, 26), "m": (110, 40, 36), "T": (236, 228, 212)})]})
    visor = dict(base=C_STEEL, noise=3, features={"front": [("ascii", ["", "dddddd", ".d..d."], {"d": (18, 18, 22)})],
                                                   "top": [("ascii", ["LLLLLL"], {"L": C_STEEL_L})]})
    stalk = dict(base=C_FLESH, noise=3)
    eyeball = dict(base=(236, 232, 222), noise=2, features={"all": [("draw", fill_glow((230, 226, 214))),
                                                                    ("ascii", ["", ".v"], {"v": ((170, 50, 50), "glow")})],
                                                            "front": [("ascii", ["iP", "ii"], {"i": ((60, 110, 90), "glow"), "P": ((10, 10, 12), "glow")})]})
    hand = dict(base=C_FLESH, noise=3, features={"top": [("ascii", ["f.f", "ffff"[:3]], {"f": C_FLESH_D})]})
    lance = dict(base=(176, 150, 112), noise=4, features={"all": [("hstripes", 3, C_RED)]})
    tip = dict(base=C_STEEL, noise=3, features={"all": [("px", [(0, 0, C_STEEL_L)])]})
    pennant = dict(base=C_RED, noise=4, shade=False, shape=lambda n, x, y, w, h: 255 if n in ("right", "left") and y <= (h - 1) * (1 - x / max(1, w - 1)) + 0.5 else 0,
                   features={"all": [("vstripes", 2, (230, 220, 200))]})
    slime = dict(base=(150, 160, 170), noise=6, alpha=110, shade=False)

    hands = []
    for side, sx in (("left", 1), ("right", -1)):
        for i, z in enumerate((-3.5, -1, 1.5, 4)):
            hands.append(P(f"{side}_hand_{i}", (2 * sx, 0, z), rot=(0, 0.25 * sx * (i - 1.5), 0),
                           boxes=[B(0 if sx > 0 else -2, -0.5, -0.5, 2, 0.5, 1.5, mirror=sx < 0)], paint=hand))
    parts = [
        P("slime", (0, 24, 0), boxes=[B(-1.5, -0.03, 4, 3, 0, 7)], paint=slime),
        P("foot", (0, 24, 0), boxes=[B(-2, -2, -5, 4, 2, 10)], paint=flesh, children=hands + [
            P("shell", (0, -2, 1.5), rot=(-0.1, 0, 0), boxes=[B(-3, -7, -3.5, 6, 7, 7), B(-2, -9, -2, 4, 2, 4)], paint=shell),
            P("neck", (0, -1.5, -4), rot=(0.25, 0, 0), boxes=[B(-1.5, -5, -1.5, 3, 5, 3)], paint=flesh, children=[
                P("head", (0, -5, -0.5), boxes=[B(-2, -4, -2.5, 4, 4, 4, paint=face), B(-2.5, -4.5, -2, 5, 4.5, 4.5, paint=helmet)], children=[
                    P("visor", (0, -4.4, -2.6), boxes=[B(-2.5, 0, -0.5, 5, 3, 1)], paint=visor),
                    P("left_stalk", (1, -4.3, -0.5), rot=(-0.25, 0, 0.3), boxes=[B(-0.5, -4, -0.5, 1, 4, 1)], paint=stalk, children=[
                        P("left_eyeball", (0, -4, 0), boxes=[B(-1, -2, -1, 2, 2, 2)], paint=eyeball)]),
                    P("right_stalk", (-1, -4.3, -0.5), rot=(-0.3, 0, -0.35), boxes=[B(-0.5, -4, -0.5, 1, 4, 1)], paint=stalk, children=[
                        P("right_eyeball", (0, -4, 0), boxes=[B(-1, -2, -1, 2, 2, 2)], paint=eyeball)]),
                    P("lance", (2.6, -1, -1), rot=(0.12, -0.08, 0), boxes=[B(-0.5, -0.5, -13, 1, 1, 13)], paint=lance, children=[
                        P("lance_tip", (0, 0, -13), boxes=[B(-0.5, -0.5, -2, 1, 1, 2)], paint=tip),
                        P("pennant", (0, 0, -10.5), boxes=[B(0, -3, 0, 0, 3, 3)], paint=pennant)]),
                ]),
            ]),
        ]),
    ]
    anim = """        float a = Math.min(1.0F, limbSwingAmount);
        float w = limbSwing * 1.4F;
        neck.xRot += Mth.sin(w) * 0.1F * a;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;
        // It goes along on its little hands, a ripple running down each side.
        leftHand0.yRot += Mth.sin(w) * 0.6F * a;
        leftHand1.yRot += Mth.sin(w - 0.8F) * 0.6F * a;
        leftHand2.yRot += Mth.sin(w - 1.6F) * 0.6F * a;
        leftHand3.yRot += Mth.sin(w - 2.4F) * 0.6F * a;
        rightHand0.yRot -= Mth.sin(w + 0.5F) * 0.6F * a;
        rightHand1.yRot -= Mth.sin(w - 0.3F) * 0.6F * a;
        rightHand2.yRot -= Mth.sin(w - 1.1F) * 0.6F * a;
        rightHand3.yRot -= Mth.sin(w - 1.9F) * 0.6F * a;
        shell.zRot += Mth.sin(w) * 0.05F * a;
        leftStalk.zRot += Mth.sin(ageInTicks * 0.1F) * 0.15F;
        rightStalk.zRot += Mth.sin(ageInTicks * 0.08F + 1.5F) * 0.15F;
        leftStalk.xRot += Mth.sin(ageInTicks * 0.07F) * 0.1F;
        pennant.yRot += Mth.sin(ageInTicks * 0.3F) * 0.3F;
        // The visor lifts when it means to fight, and the face grins out.
        boolean fighting = entity.isAggressive();
        visor.xRot -= fighting ? 1.3F : Math.max(0.0F, Mth.sin(ageInTicks * 0.03F) - 0.8F) * 5.0F;
        lance.xRot += Mth.cos(w) * 0.12F * a + (fighting ? 0.1F : 0.0F);
        if (attackTime > 0.0F) lance.z -= Mth.sin(Mth.sqrt(attackTime) * Mth.PI) * 3.0F;
"""
    return Model("margin_crawler", "MarginCrawlerModel", E + "MarginCrawlerEntity", (64, 64), parts, anim)


# ================================================================ the Palehand
PH_SKIN = (230, 226, 216)
PH_SKIN_D = (196, 192, 184)
PH_SKIN_L = (242, 240, 234)
PH_VEIN = (150, 160, 190)
PH_NAIL = (206, 196, 150)
PH_NAIL_D = (150, 140, 100)
PH_SOIL = (70, 56, 42)


def _ph_skin(extra=None, veins=True):
    feats = {"all": [("draw", skin(PH_SKIN, PH_SKIN_D, PH_SKIN_L, veins=PH_VEIN if veins else None, mottle=0.2))]}
    for k, lst in (extra or {}).items():
        feats.setdefault(k, []).extend(lst)
    return dict(base=PH_SKIN, noise=3, rim=(PH_SKIN_D, 0.45), features=feats)


def palehand():
    """
    Far off, at night, behind the hills: a hand the size of a mountain, coming up out of the
    earth to the forearm. Bone-pale, blue-veined, the nails long and yellow. Each finger has one
    joint more than a finger should, so when it curls, writing in the air, it curls too far.
    """
    forearm = _ph_skin({"sides": [("draw", wicking(PH_SOIL, 0.25))], "bottom": [("draw", fill(PH_SOIL, 4))]})
    palm = _ph_skin({"front": [("hstripes", 3, PH_SKIN_D)], "back": [("vstripes", 2, (214, 212, 208))]})
    knuckle = _ph_skin({"all": [("hstripes", 2, PH_SKIN_D)]}, veins=False)
    tip = _ph_skin({"back": [("draw", fill(PH_NAIL, 4)), ("ascii", ["", "", "d.", ".d"], {"d": PH_NAIL_D})],
                    "top": [("draw", fill(PH_NAIL, 3))]}, veins=False)

    def finger(i, x, lengths, spread):
        name = f"finger_{i}"
        segs = []
        for j, ln in reversed(list(enumerate(lengths))):
            last = j == len(lengths) - 1
            kids = segs if segs else []
            seg = P(f"{name}_{j}" if j else name, (x, -9, 0) if j == 0 else (0, -lengths[j - 1], 0),
                    rot=(0.12 + 0.06 * j, 0, spread if j == 0 else 0),
                    boxes=[B(-1, -ln, -1, 2, ln, 2)], paint=tip if last else knuckle, children=kids)
            segs = [seg]
        return segs[0]

    fingers = [finger(i, x, ls, sp) for i, (x, ls, sp) in enumerate((
        (-4, (5, 4, 4, 3), -0.3), (-2, (6, 5, 4, 3), -0.12), (0, (6, 5, 5, 4), 0.02), (2, (6, 5, 4, 3), 0.14), (4, (5, 4, 3, 3), 0.32)))]
    thumb = P("thumb", (-5, -2, 0), rot=(-0.3, 0, -0.95), boxes=[B(-1, -6, -1, 2, 6, 2)], paint=knuckle, children=[
        P("thumb_1", (0, -6, 0), rot=(0, 0, 0.25), boxes=[B(-1, -4, -1, 2, 4, 2)], paint=knuckle, children=[
            P("thumb_2", (0, -4, 0), rot=(0, 0, 0.2), boxes=[B(-1, -3, -1, 2, 3, 2)], paint=tip)])])
    parts = [
        P("wrist", (0, 24, 0), boxes=[B(-3.5, -12, -2.5, 7, 16, 5)], paint=forearm, children=[
            P("palm", (0, -12, 0), boxes=[B(-5, -9, -2, 10, 9, 4)], paint=palm, children=fingers + [thumb])]),
    ]
    anim = "        float t = ageInTicks * 0.045F;\n"
    for i in range(5):
        anim += f"        float c{i} = Mth.sin(t + {i * 0.7:.1f}F);\n"
        anim += f"        finger{i}.xRot += 0.12F + c{i} * 0.3F;\n"
        for j in range(1, 4):
            anim += f"        finger{i}{j}.xRot += 0.2F + c{i} * {0.28 + 0.06 * j:.2f}F;\n"
    anim += """        thumb.zRot += Mth.sin(t * 0.7F) * 0.15F;
        thumb1.zRot += Mth.sin(t * 0.7F + 0.5F) * 0.2F;
        palm.xRot += Mth.sin(t * 0.5F) * 0.08F;
        wrist.zRot += Mth.sin(ageInTicks * 0.02F) * 0.06F;
"""
    return Model("palehand", "PalehandModel", E + "PalehandEntity", (64, 64), parts, anim)


# ================================================================ the Bookbinder
K_LEATHER = (92, 56, 34)
K_LEATHER_D = (62, 36, 22)
K_LEATHER_2 = (58, 66, 50)
K_LEATHER_3 = (96, 36, 30)
K_PAGES = (222, 210, 176)
K_PAGES_D = (184, 170, 134)
K_GILT = (206, 164, 70)
K_THREAD = (170, 30, 26)
K_FLESH = (186, 164, 146)
K_FLESH_D = (150, 126, 110)
K_MAP = (70, 56, 46)
K_STEEL = (140, 146, 158)
K_STEEL_L = (214, 218, 228)
K_EYE = (240, 214, 120)


def book(cover, edge=K_PAGES, gilt=K_GILT, spine_face="left", seed=0):
    """A bound book as one box: leather boards top and bottom, page edges round three sides,
    and a spine with raised bands and a gilt title on the fourth."""
    def pages(canvas, glow, u, v, w, h, r):
        for y in range(h):
            for x in range(w):
                c = edge if y % 2 == 0 else K_PAGES_D
                if y == 0 or y == h - 1:
                    c = cover
                canvas.set(u + x, v + y, jitter(c, 4, r))
    feats = {"top": [("draw", fill(cover, 5)), ("draw", speckle(K_LEATHER_D, 0.12, seed))],
             "bottom": [("draw", fill(cover, 5))],
             "front": [("draw", pages)], "back": [("draw", pages)], "right": [("draw", pages)], "left": [("draw", pages)]}
    feats[spine_face] = [("draw", fill(cover, 5)), ("vstripes", 4, K_LEATHER_D), ("hstripes", 99, gilt),
                         ("ascii", ["", "gg.g.gg", ""], {"g": gilt}, (1, 0))]
    return dict(base=cover, noise=5, features=feats)


def seam(thread, col=None):
    """A sewn seam down a face: a line with short stitches across it."""
    def fn(canvas, glow, u, v, w, h, r):
        x0 = w // 2 if col is None else col
        for y in range(h):
            if w < 4:
                # Too narrow for cross-stitches: a dotted line of thread.
                if y % 2 == 0:
                    canvas.blend(u + min(x0, w - 1), v + y, thread, 0.75)
                continue
            canvas.blend(u + x0, v + y, thread, 0.8)
            if y % 2 == 0:
                if x0 - 1 >= 0:
                    canvas.blend(u + x0 - 1, v + y, thread, 0.7)
                if x0 + 1 < w:
                    canvas.blend(u + x0 + 1, v + y, thread, 0.7)
    return fn


def bookbinder():
    """
    Idris Wray, sewn into the Bindery: a starved man grown up out of a heap of books, bound into
    it at the waist. His skin is stitched in red thread, his mouth sewn shut, one eye sewn closed;
    the other stares through a jeweller's loupe. He was a cartographer, and the map is still on
    his scalp. He walks on six great needles, and arms of stitched paper claw out of the pile.
    """
    ribs_thread = dict(base=K_FLESH, noise=4, rim=(K_FLESH_D, 0.35), features={
        "all": [("draw", skin(K_FLESH, K_FLESH_D, (204, 184, 168)))],
        "front": [("draw", ribs((200, 180, 164), K_FLESH_D, 1, 2, 2)), ("stitches", K_THREAD)],
        # Laced up the back like a book's spine.
        "back": [("ascii", ["t.....t", ".t...t.", "..t.t..", "...t...", "..t.t..", ".t...t.", "t.....t", ".t...t.", "..t.t..", "...t...", "..t.t.."], {"t": K_THREAD})]})
    head = dict(base=K_FLESH, noise=4, rim=(K_FLESH_D, 0.3), features={
        "all": [("draw", skin(K_FLESH, K_FLESH_D, (204, 184, 168)))],
        # The map: coastlines and a compass rose in brown ink over the scalp.
        "top": [("ascii", ["..mm....", ".m..mm..", "m.....m.", ".m.x..m.", "..m...mm", "...mm..m", ".m...mm.", "........"], {"m": K_MAP, "x": K_THREAD})],
        "right": [("ascii", ["mm......", "..m.....", "...mm...", "......m."], {"m": K_MAP})],
        "left": [("ascii", ["......mm", ".....m..", "...mm...", ".m......"], {"m": K_MAP})],
        "back": [("ascii", ["m..m..m.", ".mm..mm.", "........"], {"m": K_MAP})],
        "front": [("ascii", [
            "........",
            "........",
            ".x.x....",
            "..x..LL.",
            ".x.x.EL.",
            "........",
            "tttttttt",
            "t.t.t.t.",
        ], {"x": K_THREAD, "t": K_THREAD, "L": K_GILT, "E": (K_EYE, "glow")})]})
    arm = dict(base=K_FLESH, noise=4, rim=(K_FLESH_D, 0.35), features={"all": [("draw", skin(K_FLESH, K_FLESH_D, (204, 184, 168)))],
                                                                         "front": [("draw", seam(K_THREAD))]})
    hand = dict(base=K_FLESH_D, noise=4, features={"all": [("draw", wicking((40, 30, 30), 0.5))]})
    upper = dict(base=K_LEATHER, noise=6, features={"all": [("draw", fill(K_LEATHER, 6)), ("vstripes", 4, K_LEATHER_D)],
                                                    "top": [("vstripes", 3, K_THREAD)], "front": [("vstripes", 3, K_THREAD)]})
    needle = dict(base=K_STEEL, noise=3, features={"all": [("draw", fill(K_STEEL, 3)), ("ascii", ["", "LLLLLLLLLLLLLLLLLLLLLLLLLL"], {"L": K_STEEL_L})],
                                                   "top": [("ascii", ["", "", "od"], {"o": (20, 20, 24), "d": K_THREAD})]})
    paper = dict(base=K_PAGES, noise=5, shade=False, shape=ragged(2, seed=51), features={"all": [("script", K_MAP, 0.35)]})
    paper_arm = dict(base=K_PAGES, noise=5, features={"all": [("script", K_MAP, 0.3), ("hstripes", 3, K_THREAD)]})
    # A brass ring: the open eye stares through the middle of it.
    loupe = dict(base=K_GILT, noise=4, features={"front": [("ascii", ["", ".o.", ""], {"o": None})],
                                                 "back": [("ascii", ["", ".o.", ""], {"o": None})]})
    thread = dict(base=K_THREAD, noise=4, shade=False)

    legs = []
    for side, sx in (("left", 1), ("right", -1)):
        for zi, z in enumerate((-5, 0, 5)):
            name = f"{side}_leg_{zi}"
            m = sx < 0
            up = B(0, -1, -1, 12, 2, 2) if sx > 0 else B(-12, -1, -1, 12, 2, 2, mirror=True)
            low = B(0, -0.5, -0.5, 26, 1, 1) if sx > 0 else B(-26, -0.5, -0.5, 26, 1, 1, mirror=True)
            yaw = (zi - 1) * 0.35 * sx
            legs.append(P(name, (7 * sx, -4, z), rot=(0, yaw, -0.7 * sx), boxes=[up], paint=upper, children=[
                P(name + "_needle", (12 * sx, 0, 0), rot=(0, 0, 2.0 * sx), boxes=[low], paint=needle, children=[
                    P(name + "_thread", (1.5 * sx, 0, 0), rot=(0, 0, -0.6 * sx), boxes=[B(-0.5, 0, 0, 1, 5, 0)], paint=thread)])]))

    def paper_arm_part(side, sx):
        m = sx < 0
        return P(f"{side}_paper_arm", (6 * sx, -6, -5), rot=(-0.6, 0.4 * sx, 0.5 * sx), boxes=[B(-1, 0, -1, 2, 8, 2, mirror=m)], paint=paper_arm, children=[
            P(f"{side}_paper_forearm", (0, 8, 0), rot=(-0.8, 0, 0), boxes=[B(-1, 0, -1, 2, 7, 2, mirror=m)], paint=paper_arm, children=[
                P(f"{side}_paper_hand", (0, 7, 0), boxes=[B(-1.5, 0, -0.5, 3, 4, 0)], paint=paper)])])

    def arm_part(side, sx, holding):
        m = sx < 0
        kids = [P(f"{side}_hand", (0, 9, 0), boxes=[B(-1.5, 0, -1.5, 3, 3, 3, mirror=m)], paint=hand)]
        if holding:
            kids.append(P("held_needle", (0, 10, 0), rot=(0.6, 0, 0), boxes=[B(-0.5, 0, -0.5, 1, 16, 1)], paint=needle, children=[
                P("held_thread", (0, 1, 0), rot=(-0.5, 0, 0.3), boxes=[B(-0.5, 0, 0, 1, 10, 0)], paint=thread)]))
        return P(f"{side}_arm", (5 * sx, -10, 0), rot=((-0.6 if holding else -0.4), 0, -0.2 * sx), boxes=[B(-1, -1, -1, 2, 10, 2, mirror=m)], paint=arm, children=[
            P(f"{side}_forearm", (0, 9, 0), rot=(-0.3, 0, 0), boxes=[B(-1, 0, -1, 2, 10, 2, mirror=m)], paint=arm, children=kids)])

    pages = [P(f"loose_page_{i}", (x, -8 - i, z), rot=(0.2 * i - 0.3, ry, 0), boxes=[B(-2.5, 0, 0, 5, 0, 6)], paint=paper)
             for i, (x, z, ry) in enumerate(((-5, -6, 0.5), (4, -7, -0.4), (6, 3, 1.4), (-6, 4, 2.2)))]
    parts = [
        P("heap", (0, 12, 0), boxes=[B(-8, -4, -7, 16, 4, 14, paint=book(K_LEATHER, spine_face="front", seed=1))], children=legs + pages + [
            P("tome_1", (0, -4, 0), rot=(0, 0.28, 0), boxes=[B(-6.5, -4, -6, 13, 4, 11)], paint=book(K_LEATHER_2, spine_face="right", seed=2)),
            P("tome_2", (0.5, -8, 1), rot=(0, -0.2, 0.05), boxes=[B(-5.5, -3, -5, 11, 3, 9)], paint=book(K_LEATHER_3, spine_face="left", seed=3)),
            P("tome_3", (-2, -9, 5), rot=(-0.35, 0.3, 0), boxes=[B(-4, -10, -1.5, 8, 10, 3)], paint=book(K_LEATHER_D, spine_face="back", seed=4)),
            paper_arm_part("left", 1), paper_arm_part("right", -1),
            P("torso", (0, -10, -1.5), rot=(0.35, 0, 0), boxes=[B(-4, -11, -2, 8, 11, 4)], paint=ribs_thread, children=[
                P("head", (0, -11, -0.5), boxes=[B(-4, -8, -4, 8, 8, 8)], paint=head, children=[
                    P("loupe", (2, -4, -4.2), boxes=[B(-1.5, -1.5, -1, 3, 3, 1)], paint=loupe)]),
                arm_part("left", 1, False), arm_part("right", -1, True),
            ]),
        ]),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        // Now and then his head jerks to one side, against the stitches.
        head.zRot += Mth.sin(ageInTicks * 0.05F) > 0.9F ? 0.25F : 0.0F;
        float step = limbSwing * 0.8F;
"""
    for side in ("left", "right"):
        for zi in range(3):
            n = f"{side}Leg{zi}"
            phase = "" if (zi + (0 if side == "left" else 1)) % 2 == 0 else " + Mth.PI"
            anim += f"        {n}.zRot += Mth.sin(step{phase}) * 0.22F * limbSwingAmount;\n"
            anim += f"        {n}.yRot += Mth.cos(step{phase}) * 0.18F * limbSwingAmount;\n"
            anim += f"        {n}Thread.zRot += Mth.sin(ageInTicks * 0.1F + {zi}.0F) * 0.2F;\n"
    anim += """        loosePage0.xRot += Mth.sin(ageInTicks * 0.3F) * 0.15F;
        loosePage1.xRot += Mth.sin(ageInTicks * 0.27F + 1.0F) * 0.15F;
        loosePage2.xRot += Mth.sin(ageInTicks * 0.33F + 2.0F) * 0.15F;
        loosePage3.xRot += Mth.sin(ageInTicks * 0.25F + 3.0F) * 0.15F;
        // The paper arms grope forward, one after the other.
        leftPaperArm.xRot += Mth.sin(ageInTicks * 0.09F) * 0.3F;
        rightPaperArm.xRot += Mth.sin(ageInTicks * 0.09F + Mth.PI) * 0.3F;
        leftPaperForearm.xRot += Mth.sin(ageInTicks * 0.09F + 0.8F) * 0.25F;
        rightPaperForearm.xRot += Mth.sin(ageInTicks * 0.09F + Mth.PI + 0.8F) * 0.25F;
        heldThread.zRot += Mth.sin(ageInTicks * 0.12F) * 0.2F;
        if (entity.isRebinding()) {
            torso.xRot += 0.5F;
            leftArm.xRot -= 1.6F + Mth.sin(ageInTicks * 0.4F) * 0.2F;
            rightArm.xRot -= 1.6F + Mth.cos(ageInTicks * 0.4F) * 0.2F;
            leftPaperArm.xRot -= 0.8F;
            rightPaperArm.xRot -= 0.8F;
        } else if (entity.isStunned()) {
            torso.xRot += 0.9F;
            head.xRot += 0.6F;
            heap.y += 1.0F;
        } else if (attackTime > 0) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightArm.xRot -= 1.8F * s;
            torso.xRot -= 0.3F * s;
            leftPaperArm.xRot -= 1.0F * s;
            rightPaperArm.xRot -= 1.0F * s;
        }
        heap.y += Mth.sin(ageInTicks * 0.08F) * 0.4F;
"""
    return Model("bookbinder", "BookbinderModel", "com.exonoxic.palimpsest.entity.boss.BookbinderEntity", (128, 128), parts, anim)


# ================================================================ the Rasure
X_VELLUM = (228, 224, 212)
X_VELLUM_D = (196, 190, 176)
X_RAW = (214, 168, 158)
X_RAW_D = (176, 120, 112)
X_SLIT = (226, 40, 30)
X_STEEL = (170, 176, 188)
X_STEEL_L = (236, 240, 246)
X_STEEL_D = (96, 100, 112)


def raw_patches(raw, dark, count=2, seed=0):
    """Places scraped too deep: raw, pinkish, like skin under the page."""
    def fn(canvas, glow, u, v, w, h, r):
        for _ in range(count):
            cx, cy = r.uniform(0, w), r.uniform(0, h)
            rw, rh = r.uniform(1.0, 2.5), r.uniform(1.5, 3.5)
            for y in range(h):
                for x in range(w):
                    d = ((x + 0.5 - cx) / rw) ** 2 + ((y + 0.5 - cy) / rh) ** 2
                    if d < 1.0:
                        canvas.blend(u + x, v + y, raw, 0.75)
                        if d > 0.7:
                            canvas.blend(u + x, v + y, dark, 0.4)
    return fn


def rasure():
    """
    The knife that scraped the first draft, still scraping: a figure much too tall, of vellum
    scraped so thin that in places it has gone through to something raw and pink. It has no face,
    only a long red cut down the front of its head, which opens. One arm ends in a lunellum, the
    crescent knife parchment-makers scraped skins with, as long as a man. Blank pages circle it,
    and little pen-knives with them.
    """
    vellum = dict(base=X_VELLUM, noise=3, rim=(X_VELLUM_D, 0.35), features={
        "all": [("draw", scraped(X_VELLUM, X_VELLUM_D, 1)), ("draw", raw_patches(X_RAW, X_RAW_D, 2, 1))]})
    robe = dict(base=X_VELLUM, noise=3, shape=ragged(4, holes=3, sides=True, seed=61), features={
        "all": [("draw", scraped(X_VELLUM, X_VELLUM_D, 2)), ("draw", raw_patches(X_RAW, X_RAW_D, 3, 2))],
        "sides": [("script", (170, 160, 146), 0.15)]})
    def wound(cv, gl, u, v, w, h, r):
        # Under the two halves of the face: raw red, brightest down the middle.
        for y in range(h):
            for x in range(w):
                d = abs(x - (w - 1) / 2.0) / (w / 2.0)
                c = mix(X_SLIT, (70, 8, 8), d)
                cv.set(u + x, v + y, jitter(c, 4, r))
                if d < 0.45 and 1 <= y < h - 1:
                    gl.set(u + x, v + y, mix(X_SLIT, (120, 12, 10), d * 2))

    head = dict(base=X_VELLUM, noise=2, rim=(X_VELLUM_D, 0.3), features={
        "all": [("draw", scraped(X_VELLUM, X_VELLUM_D, 3))], "front": [("draw", wound)]})
    plate_l = dict(base=X_VELLUM, noise=2, shade=False, features={"all": [("draw", scraped(X_VELLUM, X_VELLUM_D, 4)), ("vstripes", 99, X_RAW_D)]})
    plate_r = dict(base=X_VELLUM, noise=2, shade=False, features={"all": [("draw", scraped(X_VELLUM, X_VELLUM_D, 6)),
                                                                          ("ascii", ["..r"] * 12, {"r": X_RAW_D})]})
    arm = dict(base=X_VELLUM, noise=3, rim=(X_VELLUM_D, 0.35), features={"all": [("draw", scraped(X_VELLUM, X_VELLUM_D, 5)),
                                                                                  ("draw", raw_patches(X_RAW, X_RAW_D, 1, 5))]})
    finger = dict(base=X_VELLUM_D, noise=3, features={"all": [("hstripes", 3, (170, 162, 148))]})
    steel = dict(base=X_STEEL, noise=3, features={"all": [("draw", fill(X_STEEL, 3))],
                                                  "front": [("ascii", ["L", "L", "L", "L"], {"L": (X_STEEL_L, "glow")})],
                                                  "top": [("ascii", ["", "LLLL"], {"L": X_STEEL_L})]})
    handle = dict(base=(70, 54, 40), noise=4, features={"all": [("hstripes", 2, (50, 38, 28))]})
    page = dict(base=(236, 232, 222), noise=3, shade=False, features={"all": [("draw", fill((236, 232, 222), 2))]})
    knife = dict(base=X_STEEL, noise=3, shade=False, features={"all": [("ascii", ["L", "L", "L"], {"L": X_STEEL_L})]})

    # The lunellum: a crescent of steel on a short handle, its cutting edge on the outside.
    crescent = []
    for i in range(7):
        ang = -1.2 + i * 0.4
        crescent.append(P(f"blade_{i}", (0, -1 + math.cos(ang) * 7.5, math.sin(ang) * 7.5), rot=(ang, 0, 0),
                          boxes=[B(-0.5, -1, -2, 1, 2.5, 4)], paint=steel))
    pages = []
    for i in range(6):
        a = i * math.pi / 3
        pages.append(P(f"page_{i}", (0, -24 + (i % 2) * 5, 0), rot=(0, a, 0.25 - (i % 3) * 0.15), boxes=[B(10, -3, 0, 4, 5, 0)], paint=page))
    for i in range(3):
        a = i * 2 * math.pi / 3 + 0.5
        pages.append(P(f"knife_{i}", (0, -19, 0), rot=(0, a, 1.2), boxes=[B(12, -2, 0, 1, 4, 0)], paint=knife))
    parts = [
        P("robe", (0, 23, 0), boxes=[B(-6.5, -16, -4.5, 13, 16, 9)], paint=robe, children=[
            P("robe_strip_0", (-3, 0, -4.6), boxes=[B(-1, 0, 0, 2, 5, 0)], paint=dict(robe, shape=ragged(3, seed=62))),
            P("robe_strip_1", (3.5, 0, -4.6), boxes=[B(-1, 0, 0, 2, 4, 0)], paint=dict(robe, shape=ragged(2, seed=63))),
        ]),
        P("body", (0, 8, 0), boxes=[B(-5, -16, -3, 10, 16, 6)], paint=vellum, children=[
            P("neck", (0, -16, 0), boxes=[B(-1.5, -4, -1.5, 3, 4, 3)], paint=arm, children=[
                P("head", (0, -4, 0), boxes=[B(-3.5, -14, -3.5, 7, 14, 7)], paint=head, children=[
                    P("face_left", (0, -7, -3.55), boxes=[B(0, -7, 0, 3.5, 14, 0)], paint=plate_l),
                    P("face_right", (0, -7, -3.55), boxes=[B(-3.5, -7, 0, 3.5, 14, 0)], paint=plate_r),
                ]),
            ]),
            P("blade_arm", (-6.5, -15, 0), rot=(0, 0, 0.12), boxes=[B(-1.5, 0, -1.5, 3, 14, 3, mirror=True)], paint=arm, children=[
                P("blade_forearm", (0, 14, 0), rot=(-0.3, 0, 0), boxes=[B(-1.5, 0, -1.5, 3, 12, 3, mirror=True)], paint=arm, children=[
                    P("blade", (0, 12, 0), boxes=[B(-1, 0, -1, 2, 5, 2)], paint=handle, children=crescent)])]),
            P("left_arm", (6.5, -15, 0), rot=(0, 0, -0.1), boxes=[B(-1.5, 0, -1.5, 3, 16, 3)], paint=arm, children=[
                P("left_forearm", (0, 16, 0), rot=(-0.25, 0, 0), boxes=[B(-1, 0, -1, 2, 12, 2)], paint=arm, children=[
                    P(f"finger_{i}", (x, 12, 0), rot=(-0.2, 0, zr), boxes=[B(-0.5, 0, -0.5, 1, 10, 1)], paint=finger)
                    for i, (x, zr) in enumerate(((-0.8, 0.12), (0.2, 0.04), (0.9, -0.08), (-0.2, -0.16)))])]),
        ] + pages),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        float bob = Mth.sin(ageInTicks * 0.1F) * 1.5F;
        body.y += bob;
        robe.y += bob * 0.6F;
        robe.xRot += Mth.cos(limbSwing * 0.6F) * 0.12F * limbSwingAmount;
        robeStrip0.xRot += Mth.sin(ageInTicks * 0.12F) * 0.15F;
        robeStrip1.xRot += Mth.sin(ageInTicks * 0.1F + 1.0F) * 0.15F;
        float spin = ageInTicks * 0.05F;
        page0.yRot += spin;
        page1.yRot += spin;
        page2.yRot += spin;
        page3.yRot += spin;
        page4.yRot += spin;
        page5.yRot += spin;
        knife0.yRot -= spin * 2.0F;
        knife1.yRot -= spin * 2.0F;
        knife2.yRot -= spin * 2.0F;
        // The cut down its face: a crack of red that widens when it strikes.
        float open = 0.25F + Math.max(0.0F, Mth.sin(ageInTicks * 0.04F) - 0.7F) * 3.0F;
        int phase = entity.getPhase();
        if (entity.isShielded()) {
            bladeArm.xRot -= 2.6F;
            leftArm.xRot -= 2.6F;
            head.xRot -= 0.5F;
        } else if (attackTime > 0) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            bladeArm.xRot -= 2.2F * s;
            bladeArm.yRot += 0.8F * s;
            body.yRot -= 0.4F * s;
            open += 1.5F * s;
        } else {
            bladeArm.xRot += -0.25F + Mth.sin(ageInTicks * 0.06F) * 0.08F;
            leftArm.xRot += -0.15F + Mth.cos(ageInTicks * 0.06F) * 0.08F;
            finger0.xRot += Mth.sin(ageInTicks * 0.2F) * 0.2F;
            finger1.xRot += Mth.sin(ageInTicks * 0.2F + 0.7F) * 0.2F;
            finger2.xRot += Mth.sin(ageInTicks * 0.2F + 1.4F) * 0.2F;
            finger3.xRot += Mth.sin(ageInTicks * 0.2F + 2.1F) * 0.2F;
        }
        if (phase == com.exonoxic.palimpsest.entity.boss.RasureEntity.BLANK_PAGE) {
            head.zRot += 0.4F;
            open += 1.0F;
        }
        faceLeft.x += open;
        faceRight.x -= open;
"""
    return Model("rasure", "RasureModel", "com.exonoxic.palimpsest.entity.boss.RasureEntity", (128, 128), parts, anim)
