"""
Entity models for Palimpsest.

Each creature is described once, below, as a tree of parts and boxes (pixel units, the same
coordinate system as vanilla Java models: y grows downward, feet at y = 24). From that single
description this script writes:

  * src/main/java/.../client/model/<Name>Model.java  (LayerDefinition + animation)
  * src/main/resources/assets/palimpsest/textures/entity/<name>.png  (UV-exact painted skin)
  * ..._glow.png for emissive details, where a spec asks for them
  * ModModelLayers.java registering every layer

UV space is packed automatically, so boxes can be edited freely without hand-placing texOffs.
"""
import math
import os

from pixel import *

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA_DIR = os.path.join(ROOT, "src/main/java/com/exonoxic/palimpsest/client/model")
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/palimpsest/textures/entity")
PKG = "com.exonoxic.palimpsest.client.model"


from modelkit import B, P, Model, E, HEAD_LOOK  # noqa: E402  (shared with creature_designs.py)


def camel(s):
    parts = s.split("_")
    return parts[0] + "".join(x.capitalize() for x in parts[1:])


def f(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    if "." not in s:
        s += ".0"
    return s + "F"


# ---------------------------------------------------------------- UV packing
def pack(model):
    boxes = []
    for p in model.all_parts():
        for b in p.boxes:
            boxes.append((p, b))
    # Tallest first packs far tighter than declaration order.
    boxes.sort(key=lambda pb: (-(pb[1].d + pb[1].h), -(pb[1].d + pb[1].w)))
    x = y = row = 0
    for p, b in boxes:
        bw = int(math.ceil(2 * (b.d + b.w)))
        bh = int(math.ceil(b.d + b.h))
        if bw > model.tw:
            raise ValueError(f"{model.name}: box too wide for texture")
        if x + bw > model.tw:
            x = 0
            y += row
            row = 0
        if y + bh > model.th:
            raise ValueError(f"{model.name}: texture {model.tw}x{model.th} too small")
        b.u, b.v = x, y
        x += bw
        row = max(row, bh)


# ---------------------------------------------------------------- painting
def faces(b):
    """Face rectangles (name -> (u, v, w, h)) in the vanilla cube layout."""
    u, v, w, h, d = b.u, b.v, int(b.w), int(b.h), int(b.d)
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


FACE_GROUPS = {
    "all": ("top", "bottom", "right", "front", "left", "back"),
    "sides": ("right", "front", "left", "back"),
}


def resolve(x, n):
    return x + n if x < 0 else x


def paint_box(canvas, glow, b, paint, r):
    paint = dict(paint or {})
    base = paint.get("base", (128, 128, 128))
    noise_amt = paint.get("noise", 8)
    alpha = paint.get("alpha", 255)
    shape = paint.get("shape")
    for name, (u, v, w, h) in faces(b).items():
        if w <= 0 or h <= 0:
            continue
        col = paint.get(name, base)
        for yy in range(h):
            for xx in range(w):
                c = col
                if paint.get("shade", True):
                    if name == "top":
                        c = shade(c, 1.12)
                    elif name == "bottom":
                        c = shade(c, 0.72)
                    else:
                        t = yy / max(1, h - 1)
                        c = shade(c, 1.06 - 0.22 * t)
                c = jitter(c, noise_amt, r)
                a = alpha
                if shape is not None:
                    a = shape(name, xx, yy, w, h)
                canvas.set(u + xx, v + yy, c, a)
        if paint.get("edge"):
            ec = paint["edge"]
            for xx in range(w):
                canvas.blend(u + xx, v, ec, 0.5)
                canvas.blend(u + xx, v + h - 1, ec, 0.5)
            for yy in range(h):
                canvas.blend(u, v + yy, ec, 0.5)
                canvas.blend(u + w - 1, v + yy, ec, 0.5)
        for target, feats in paint.get("features", {}).items():
            names = FACE_GROUPS.get(target, (target,))
            if name not in names:
                continue
            for feat in feats:
                apply_feature(canvas, glow, feat, u, v, w, h, r)
        if paint.get("rim"):
            # Darken the lower edge (and a little of the sides) so joints read as separate pieces.
            rc, ra = paint["rim"]
            for xx in range(w):
                canvas.blend(u + xx, v + h - 1, rc, ra)
            if name not in ("top", "bottom"):
                for yy in range(h):
                    canvas.blend(u, v + yy, rc, ra * 0.6)
                    canvas.blend(u + w - 1, v + yy, rc, ra * 0.6)
        if shape is not None:
            # Cut-outs must survive the features painted on top.
            for yy in range(h):
                for xx in range(w):
                    if shape(name, xx, yy, w, h) == 0:
                        canvas.set(u + xx, v + yy, (0, 0, 0), 0)


def apply_feature(canvas, glow, feat, u, v, w, h, r):
    kind = feat[0]
    if kind == "px" or kind == "glow":
        for (x, y, c) in feat[1]:
            px, py = u + resolve(x, w), v + resolve(y, h)
            if 0 <= px - u < w and 0 <= py - v < h:
                canvas.set(px, py, c)
                if kind == "glow" and glow is not None:
                    glow.set(px, py, c)
    elif kind in ("rect", "glowrect"):
        _, x0, y0, x1, y1, c = feat
        x0, x1 = resolve(x0, w), resolve(x1, w) if x1 <= 0 else x1
        y0, y1 = resolve(y0, h), resolve(y1, h) if y1 <= 0 else y1
        for y in range(y0, min(y1, h)):
            for x in range(x0, min(x1, w)):
                canvas.set(u + x, v + y, c)
                if kind == "glowrect" and glow is not None:
                    glow.set(u + x, v + y, c)
    elif kind == "script":
        _, c, density = feat
        tmp = Canvas(w, h)
        scribble(tmp, 0, 0, w, h, c, r, density=density)
        for y in range(h):
            for x in range(w):
                if tmp.px[y, x, 3]:
                    canvas.blend(u + x, v + y, c, 0.85)
    elif kind == "spots":
        _, c, count, rad = feat
        for _ in range(count):
            cx, cy = r.uniform(0, w), r.uniform(0, h)
            rr = r.uniform(rad * 0.5, rad)
            for y in range(h):
                for x in range(w):
                    if math.hypot(x + 0.5 - cx, y + 0.5 - cy) < rr:
                        canvas.blend(u + x, v + y, c, 0.8)
    elif kind == "vstripes":
        _, period, c = feat
        for x in range(0, w, period):
            for y in range(h):
                canvas.blend(u + x, v + y, c, 0.7)
    elif kind == "hstripes":
        _, period, c = feat
        for y in range(0, h, period):
            for x in range(w):
                canvas.blend(u + x, v + y, c, 0.7)
    elif kind == "grad":
        _, c0, c1 = feat
        for y in range(h):
            t = y / max(1, h - 1)
            c = mix(c0, c1, t)
            for x in range(w):
                canvas.blend(u + x, v + y, c, 0.6)
    elif kind == "drips":
        _, c, count = feat
        for _ in range(count):
            x = r.randrange(w)
            ln = r.randint(1, max(1, h // 2))
            for y in range(ln):
                canvas.set(u + x, v + y, c)
    elif kind == "ascii":
        # Pixel art: rows of characters looked up in a palette. A palette value is a colour,
        # (colour, "glow") for an emissive pixel, or None to cut the pixel out entirely.
        _, rows, pal = feat[:3]
        ox, oy = feat[3] if len(feat) > 3 else (0, 0)
        ox, oy = resolve(ox, w), resolve(oy, h)
        for j, row in enumerate(rows):
            for i, ch in enumerate(row):
                if ch in " .":
                    continue
                x, y = ox + i, oy + j
                if not (0 <= x < w and 0 <= y < h):
                    continue
                spec = pal[ch]
                if spec is None:
                    canvas.set(u + x, v + y, (0, 0, 0), 0)
                    continue
                glowing = isinstance(spec, tuple) and len(spec) == 2 and spec[1] == "glow"
                c = spec[0] if glowing else spec
                a = c[3] if len(c) == 4 else 255
                canvas.set(u + x, v + y, c[:3], a)
                if glowing and glow is not None:
                    glow.set(u + x, v + y, c[:3], a)
    elif kind == "draw":
        # Free-form: fn(canvas, glow, u, v, w, h, r) paints the face however it likes.
        feat[1](canvas, glow, u, v, w, h, r)
    elif kind == "stitches":
        _, c = feat
        for x in range(1, w - 1, 2):
            canvas.set(u + x, v + h // 2, c)
            if x % 4 == 1:
                canvas.set(u + x, v + h // 2 - 1, c)
                canvas.set(u + x, v + h // 2 + 1, c)
    else:
        raise ValueError(kind)


def paint_canvases(model, variant=None):
    """Paints the model's skin and emissive layer in memory (also used by preview_models.py)."""
    overrides = model.variants.get(variant, {}) if variant else {}
    r = rng_for("model:" + model.name + (":" + variant if variant else ""))
    canvas = Canvas(model.tw, model.th)
    glow = Canvas(model.tw, model.th)
    for p in model.all_parts():
        for b in p.boxes:
            paint = overrides.get(p.name) or b.paint or p.paint
            paint_box(canvas, glow, b, paint, r)
    return canvas, glow


def paint_model(model):
    for variant in [None] + list(model.variants):
        canvas, glow = paint_canvases(model, variant)
        name = model.name + ("_" + variant if variant else "")
        canvas.save(os.path.join(TEX_DIR, name + ".png"))
        if glow.px[:, :, 3].any():
            glow.save(os.path.join(TEX_DIR, name + "_glow.png"))


# ---------------------------------------------------------------- Java
def java_part(p, parent_var, lines):
    var = camel(p.name) + "Def"
    builder = "CubeListBuilder.create()"
    mirrored = False
    for b in p.boxes:
        builder += f".texOffs({b.u}, {b.v})"
        if b.mirror != mirrored:
            builder += ".mirror()" if b.mirror else ".mirror(false)"
            mirrored = b.mirror
        builder += (f".addBox({f(b.x)}, {f(b.y)}, {f(b.z)}, {f(b.w)}, {f(b.h)}, {f(b.d)}, "
                    f"new CubeDeformation({f(b.inflate)}))")
    px, py, pz = p.pivot
    rx, ry, rz = p.rot
    pose = f"PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})"
    lines.append(f'        PartDefinition {var} = {parent_var}.addOrReplaceChild("{p.name}", {builder}, {pose});')
    for c in p.children:
        java_part(c, var, lines)


def java_fields(p, parent_expr, decls, inits):
    field = camel(p.name)
    decls.append(f"    protected final ModelPart {field};")
    inits.append(f'        this.{field} = {parent_expr}.getChild("{p.name}");')
    for c in p.children:
        java_fields(c, "this." + field, decls, inits)


def write_java(model):
    decls, inits, body = [], [], []
    for p in model.parts:
        java_fields(p, "root", decls, inits)
        java_part(p, "root", body)
    render_type = "RenderType::entityTranslucent" if model.translucent else "RenderType::entityCutoutNoCull"
    alpha_override = ""
    if model.alpha < 1.0:
        alpha_override = f"""
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {{
        super.renderToBuffer(poseStack, buffer, light, overlay, r, g, b, a * {f(model.alpha)});
    }}
"""
    extra = "".join(f"import {i};\n" for i in model.extra_imports)
    src = f"""package {PKG};

import com.exonoxic.palimpsest.Palimpsest;
import {model.entity};
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
{extra}
/**
 * Generated by tools/gen_models.py from the "{model.name}" spec. Edit the spec, not this file.
 */
@SuppressWarnings("unused")
public class {model.cls}<T extends {model.entity.split('.')[-1]}> extends HierarchicalModel<T> {{
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Palimpsest.id("{model.name}"), "main");
    private final ModelPart root;
{chr(10).join(decls)}

    public {model.cls}(ModelPart root) {{
        super({render_type});
        this.root = root;
{chr(10).join(inits)}
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(body)}
        return LayerDefinition.create(mesh, {model.tw}, {model.th});
    }}

    @Override
    public ModelPart root() {{
        return root;
    }}

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {{
        root().getAllParts().forEach(ModelPart::resetPose);
{model.anim.rstrip()}
    }}
{alpha_override}}}
"""
    os.makedirs(JAVA_DIR, exist_ok=True)
    with open(os.path.join(JAVA_DIR, model.cls + ".java"), "w") as fh:
        fh.write(src)


def write_layers(models):
    regs = "\n".join(f"        event.registerLayerDefinition({m.cls}.LAYER, {m.cls}::createBodyLayer);" for m in models)
    src = f"""package {PKG};

import net.minecraftforge.client.event.EntityRenderersEvent;

/** Generated by tools/gen_models.py. */
public final class ModModelLayers {{
    public static void register(EntityRenderersEvent.RegisterLayerDefinitions event) {{
{regs}
    }}

    private ModModelLayers() {{}}
}}
"""
    with open(os.path.join(JAVA_DIR, "ModModelLayers.java"), "w") as fh:
        fh.write(src)


# ================================================================ creature specs


def walk(parts, speed="0.6662F", amount="1.2F"):
    out = ""
    for i, p in enumerate(parts):
        phase = "" if i % 2 == 0 else " + Mth.PI"
        out += f"        {p}.xRot += Mth.cos(limbSwing * {speed}{phase}) * {amount} * limbSwingAmount;\n"
    return out


def moth_wing(face, x, y, w, h):
    # Rounded wing shape on the top/bottom faces; everything else transparent.
    if face not in ("top", "bottom"):
        return 0
    cx, cy = (0 if face == "top" else w - 1), h * 0.45
    return 255 if ((x - cx) / (w * 0.95)) ** 2 + ((y - cy) / (h * 0.6)) ** 2 < 1.0 else 0


MOTH_WING = dict(base=(200, 186, 154), noise=6, shade=False, shape=moth_wing,
                 features={"all": [("spots", SEPIA, 4, 1.4), ("spots", (150, 110, 70), 3, 1.0)]})

SKIN_ASH = (62, 58, 54)


def knocker():
    skin = dict(base=SKIN_ASH, noise=7, features={"sides": [("vstripes", 3, (48, 44, 42))]})
    head = dict(base=(206, 196, 178), noise=6, features={
        "front": [("grad", (214, 204, 186), (150, 140, 126)),
                  ("rect", 1, 2, 2, 6, INK_DEEP), ("rect", 4, 2, 5, 6, INK_DEEP),
                  ("px", [(1, 6, (70, 60, 60)), (4, 6, (70, 60, 60)), (1, 7, (100, 90, 86))]),
                  ("rect", 1, 7, 6, 8, (60, 26, 24)), ("px", [(0, 7, (60, 26, 24))]),
                  ("glow", [(1, 3, (190, 184, 168)), (4, 3, (190, 184, 168))])],
        "back": [("script", (120, 110, 96), 0.3)]})
    hand = dict(base=(84, 70, 64), noise=8, features={"all": [("rect", 0, 0, 3, 1, (150, 90, 80))]})
    parts = [
        P("left_leg", (2, 4, 0), boxes=[B(-1, 0, -1, 2, 20, 2)], paint=skin),
        P("right_leg", (-2, 4, 0), boxes=[B(-1, 0, -1, 2, 20, 2, mirror=True)], paint=skin),
        P("torso", (0, 4, 0), boxes=[B(-3.5, -14, -2, 7, 14, 4)], paint=dict(base=(52, 48, 46), noise=6, features={
            "front": [("hstripes", 2, (36, 32, 30))], "back": [("vstripes", 2, (40, 36, 34))]}), children=[
            P("head", (0, -14, 0), boxes=[B(-3, -8, -3, 6, 8, 6)], paint=head),
            P("left_arm", (4.5, -13, 0), boxes=[B(-1, 0, -1, 2, 22, 2)], paint=skin, children=[
                P("left_hand", (0, 22, 0), boxes=[B(-1.5, 0, -1.5, 3, 4, 3)], paint=hand)]),
            P("right_arm", (-4.5, -13, 0), boxes=[B(-1, 0, -1, 2, 22, 2, mirror=True)], paint=skin, children=[
                P("right_hand", (0, 22, 0), boxes=[B(-1.5, 0, -1.5, 3, 4, 3, mirror=True)], paint=hand)]),
        ]),
    ]
    anim = HEAD_LOOK.format(head="head") + walk(["leftLeg", "rightLeg"], amount="0.9F") + """        head.zRot += 0.32F;
        torso.xRot += 0.12F;
        int knock = entity.getKnockAnim();
        if (knock > 0) {
            rightArm.xRot += -2.1F + Mth.sin(knock * 0.62F) * 0.4F;
            rightArm.zRot += 0.15F;
        } else if (entity.getState() == com.exonoxic.palimpsest.entity.KnockerEntity.LUNGE) {
            rightArm.xRot += -1.35F + Mth.cos(ageInTicks * 0.9F) * 0.1F;
            leftArm.xRot += -1.35F - Mth.cos(ageInTicks * 0.9F) * 0.1F;
        } else {
            rightArm.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.5F * limbSwingAmount;
            leftArm.xRot += Mth.cos(limbSwing * 0.6662F) * 0.5F * limbSwingAmount;
            rightArm.zRot += 0.06F + Mth.sin(ageInTicks * 0.04F) * 0.03F;
            leftArm.zRot -= 0.06F + Mth.sin(ageInTicks * 0.04F) * 0.03F;
        }
"""
    return Model("knocker", "KnockerModel", E + "KnockerEntity", (64, 64), parts, anim)


def longhand():
    body = dict(base=(16, 15, 20), noise=5, features={"sides": [("vstripes", 4, (26, 24, 32))]})
    face = dict(base=(20, 19, 24), noise=4, features={
        "front": [("glowrect", 1, 2, 3, 9, (240, 236, 224)), ("px", [(1, 1, (180, 176, 168))])]})
    parts = [
        P("left_leg", (1.5, -6, 0), boxes=[B(-1, 0, -1, 2, 30, 2)], paint=body),
        P("right_leg", (-1.5, -6, 0), boxes=[B(-1, 0, -1, 2, 30, 2, mirror=True)], paint=body),
        P("torso", (0, -6, 0), boxes=[B(-2.5, -16, -1.5, 5, 16, 3)], paint=body, children=[
            P("head", (0, -16, 0), boxes=[B(-2, -10, -2, 4, 10, 4)], paint=face),
            P("left_arm", (3, -15, 0), boxes=[B(-0.5, 0, -0.5, 1, 32, 1)], paint=body, children=[
                P("left_fingers", (0, 32, 0), boxes=[B(-1, 0, -0.5, 2, 7, 1)], paint=dict(base=(34, 32, 40)))]),
            P("right_arm", (-3, -15, 0), boxes=[B(-0.5, 0, -0.5, 1, 32, 1, mirror=True)], paint=body, children=[
                P("right_fingers", (0, 32, 0), boxes=[B(-1, 0, -0.5, 2, 7, 1, mirror=True)], paint=dict(base=(34, 32, 40)))]),
        ]),
    ]
    anim = """        if (entity.isFrozen()) {
            switch (entity.getPoseIndex()) {
                case 0 -> { leftArm.xRot = -1.45F; rightArm.xRot = -1.35F; head.zRot = 0.5F; }
                case 1 -> { head.xRot = -0.8F; leftArm.zRot = -0.9F; rightArm.zRot = 0.9F; }
                case 2 -> { rightArm.xRot = -1.6F; rightArm.yRot = 0.2F; head.yRot = 0.35F; head.zRot = -0.25F; }
                case 3 -> { torso.xRot = 0.65F; head.xRot = -0.9F; leftArm.xRot = -0.5F; rightArm.xRot = -0.45F; }
                default -> { head.yRot = 1.45F; leftArm.zRot = -0.12F; rightArm.zRot = 0.12F; }
            }
            return;
        }
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        float jerk = Mth.sin(ageInTicks * 2.3F) * 0.06F;
        leftLeg.xRot += Mth.cos(limbSwing * 0.5F) * 1.1F * limbSwingAmount;
        rightLeg.xRot += Mth.cos(limbSwing * 0.5F + Mth.PI) * 1.1F * limbSwingAmount;
        leftArm.xRot += Mth.cos(limbSwing * 0.5F + Mth.PI) * 0.7F * limbSwingAmount + jerk;
        rightArm.xRot += Mth.cos(limbSwing * 0.5F) * 0.7F * limbSwingAmount - jerk;
        torso.zRot += jerk * 0.5F;
        leftFingers.xRot += Mth.sin(ageInTicks * 0.3F) * 0.2F;
        rightFingers.xRot += Mth.cos(ageInTicks * 0.3F) * 0.2F;
"""
    return Model("longhand", "LonghandModel", E + "LonghandEntity", (64, 64), parts, anim)


def humanoid_parts(skin_paint, head_paint, body_paint, arm_paint, leg_paint, head_children=(), body_children=(), right_arm_children=()):
    return [
        P("head", (0, 0, 0), boxes=[B(-4, -8, -4, 8, 8, 8)], paint=head_paint, children=list(head_children)),
        P("body", (0, 0, 0), boxes=[B(-4, 0, -2, 8, 12, 4)], paint=body_paint, children=list(body_children)),
        P("right_arm", (-5, 2, 0), boxes=[B(-3, -2, -2, 4, 12, 4)], paint=arm_paint, children=list(right_arm_children)),
        P("left_arm", (5, 2, 0), boxes=[B(-1, -2, -2, 4, 12, 4, mirror=True)], paint=arm_paint),
        P("right_leg", (-1.9, 12, 0), boxes=[B(-2, 0, -2, 4, 12, 4)], paint=leg_paint),
        P("left_leg", (1.9, 12, 0), boxes=[B(-2, 0, -2, 4, 12, 4, mirror=True)], paint=leg_paint),
    ]


HUMANOID_WALK = HEAD_LOOK.format(head="head") + """        rightArm.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * limbSwingAmount * 0.5F;
        leftArm.xRot += Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount * 0.5F;
        rightLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
"""


def smudge():
    cloth = (120, 112, 102)
    head = dict(base=(176, 168, 154), noise=10, features={
        "front": [("grad", (176, 168, 154), (120, 114, 104)), ("rect", 1, 2, 7, 6, (150, 142, 130)),
                  ("px", [(2, 3, (110, 104, 96)), (5, 4, (110, 104, 96)), (3, 5, (120, 112, 104))])]})
    body = dict(base=cloth, noise=10, features={"front": [("hstripes", 3, (100, 94, 86))], "all": [("spots", (150, 144, 134), 3, 2)]})
    arm = dict(base=(158, 150, 138), noise=10)
    leg = dict(base=(96, 90, 84), noise=10)
    parts = humanoid_parts(None, head, body, arm, leg)
    anim = HUMANOID_WALK + """        body.xRot += 0.18F;
        head.zRot += 0.22F + Mth.sin(ageInTicks * 0.03F) * 0.06F;
        head.z -= 1.0F;
        rightArm.zRot += 0.05F;
        leftArm.zRot -= 0.05F;
        if (entity.isFixated()) {
            head.zRot = 0.0F;
            rightArm.xRot -= 0.4F;
        }
"""
    return Model("smudge", "SmudgeModel", E + "SmudgeEntity", (64, 64), parts, anim, translucent=True, alpha=0.62)


def redacted():
    robe = dict(base=(196, 190, 176), noise=6, features={"sides": [("script", (60, 56, 60), 0.45)]})
    head = dict(base=(206, 200, 188), noise=5, features={"front": [("script", (80, 76, 80), 0.3)]})
    bar = dict(base=(8, 8, 10), noise=2, shade=False)
    parts = humanoid_parts(None, head, robe, robe, dict(base=(170, 164, 150), noise=6),
                           head_children=[P("bar_eyes", (0, 0, 0), boxes=[B(-5, -6, -4.6, 10, 2, 1)], paint=bar),
                                          P("bar_mouth", (0, 0, 0), boxes=[B(-4, -3, -4.6, 8, 2, 1)], paint=bar)],
                           body_children=[P("bar_chest", (0, 0, 0), boxes=[B(-5, 3, -2.6, 10, 2, 1)], paint=bar)],
                           right_arm_children=[P("blade", (-1, 9, 0), boxes=[B(-0.5, 0, -1, 1, 10, 2)],
                                                 paint=dict(base=STEEL, noise=4, features={"front": [("rect", 0, 0, 1, 10, (200, 204, 212))]}))])
    anim = HUMANOID_WALK + """        if (attackTime > 0) {
            float swing = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightArm.xRot -= 1.6F * swing;
            body.yRot += 0.3F * swing;
        }
        barEyes.x += Mth.sin(ageInTicks * 0.7F) * 0.15F;
        barChest.x -= Mth.sin(ageInTicks * 0.5F) * 0.15F;
"""
    return Model("redacted", "RedactedModel", E + "RedactedEntity", (64, 64), parts, anim)


def rubricator():
    robe = dict(base=(110, 26, 20), noise=8, features={"front": [("vstripes", 4, (84, 18, 14))], "back": [("vstripes", 4, (84, 18, 14))]})
    face = dict(base=(30, 24, 22), noise=4, features={"front": [("glow", [(2, 4, (250, 150, 60)), (5, 4, (250, 150, 60))])]})
    hood = dict(base=(96, 22, 18), noise=7)
    parts = [
        P("head", (0, 0, 0), boxes=[B(-4, -8, -4, 8, 8, 8)], paint=face, children=[
            P("hood_top", (0, 0, 0), boxes=[B(-5, -9, -5, 10, 1, 10)], paint=hood),
            P("hood_left", (0, 0, 0), boxes=[B(4, -8, -5, 1, 9, 10)], paint=hood),
            P("hood_right", (0, 0, 0), boxes=[B(-5, -8, -5, 1, 9, 10, mirror=True)], paint=hood),
            P("hood_back", (0, 0, 0), boxes=[B(-4, -8, 4, 8, 9, 1)], paint=hood)]),
        P("body", (0, 0, 0), boxes=[B(-4, 0, -3, 8, 12, 6)], paint=robe, children=[
            P("folded_arms", (0, 4, -3), boxes=[B(-4, -2, -3, 8, 4, 3)], paint=dict(base=(100, 24, 18), noise=6), children=[
                P("quill", (2, -1, -2), rot=(0.3, 0, -0.35), boxes=[B(-0.5, -8, -0.5, 1, 9, 1)], paint=dict(base=(230, 226, 214), noise=4,
                  features={"all": [("glowrect", 0, -2, 1, 0, (230, 60, 40))]}))])]),
        P("skirt", (0, 12, 0), boxes=[B(-4.5, 0, -3.5, 9, 12, 7)], paint=dict(base=(100, 24, 18), noise=8, features={
            "sides": [("vstripes", 3, (76, 16, 12))], "front": [("rect", 3, 9, 6, 12, GILT)]})),
    ]
    anim = HEAD_LOOK.format(head="head") + """        float sway = Mth.sin(ageInTicks * 0.05F) * 0.03F;
        body.zRot += sway;
        skirt.zRot += sway * 0.5F;
        skirt.xRot += Mth.cos(limbSwing * 0.6662F) * 0.15F * limbSwingAmount;
        quill.xRot += Mth.sin(ageInTicks * 0.1F) * 0.08F;
"""
    return Model("rubricator", "RubricatorModel", E + "RubricatorEntity", (128, 64), parts, anim)


def copyist():
    hide = dict(base=(150, 118, 96), noise=12, features={"all": [("spots", (60, 44, 36), 5, 3), ("spots", (226, 206, 190), 3, 2)]})
    skull = dict(base=(214, 204, 186), noise=8, features={
        "front": [("rect", 1, 2, 5, 4, (40, 20, 18)), ("px", [(1, 2, (240, 236, 220)), (2, 2, (240, 236, 220)), (3, 2, (240, 236, 220)), (4, 2, (240, 236, 220))]),
                  ("px", [(1, 0, INK_DEEP), (4, 0, INK_DEEP)])],
        "top": [("spots", (120, 90, 70), 2, 2)]})
    limb = dict(base=(128, 98, 80), noise=10, features={"all": [("rect", 0, -3, 3, 0, (60, 50, 44))]})
    parts = [
        P("left_leg", (2, 6, 1), boxes=[B(-1.5, 0, -1.5, 3, 18, 3)], paint=limb),
        P("right_leg", (-2, 6, 1), boxes=[B(-1.5, 0, -1.5, 3, 18, 3, mirror=True)], paint=limb),
        P("torso", (0, 6, 0), rot=(0.5, 0, 0), boxes=[B(-4, -14, -3, 8, 14, 6)], paint=hide, children=[
            P("neck", (0, -14, -1), rot=(-0.6, 0, 0), boxes=[B(-1.5, -6, -1.5, 3, 6, 3)], paint=hide, children=[
                P("head", (0, -6, 0), boxes=[B(-3, -4, -7, 6, 5, 9)], paint=skull, children=[
                    P("left_horn", (3, -3, -2), rot=(0, 0, -0.5), boxes=[B(0, -3, -0.5, 1, 3, 1)], paint=dict(base=(200, 190, 170))),
                    P("right_horn", (-3, -3, -2), rot=(0, 0, 0.5), boxes=[B(-1, -3, -0.5, 1, 3, 1)], paint=dict(base=(200, 190, 170)))])]),
            P("left_arm", (4, -13, -1), rot=(-0.5, 0, 0), boxes=[B(-1, 0, -1, 2, 21, 2)], paint=limb),
            P("right_arm", (-4, -13, -1), rot=(-0.5, 0, 0), boxes=[B(-1, 0, -1, 2, 21, 2, mirror=True)], paint=limb)]),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.7F;
        neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        head.zRot += Mth.sin(ageInTicks * 0.9F) * 0.08F;
        leftLeg.xRot += Mth.cos(limbSwing * 0.5F) * 1.3F * limbSwingAmount;
        rightLeg.xRot += Mth.cos(limbSwing * 0.5F + Mth.PI) * 1.3F * limbSwingAmount;
        leftArm.xRot += Mth.cos(limbSwing * 0.5F + Mth.PI) * 1.1F * limbSwingAmount;
        rightArm.xRot += Mth.cos(limbSwing * 0.5F) * 1.1F * limbSwingAmount;
        if (attackTime > 0) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            leftArm.xRot -= 1.2F * s;
            rightArm.xRot -= 1.2F * s;
            head.xRot -= 0.5F * s;
        }
"""
    return Model("copyist", "CopyistModel", E + "CopyistEntity", (64, 64), parts, anim)


def inkhound():
    ink = dict(base=(22, 20, 28), noise=6, features={"sides": [("drips", (8, 8, 10), 4)]})
    head = dict(base=(24, 22, 30), noise=5, features={"front": [("glowrect", 1, 3, 4, 4, (160, 20, 20))]})
    snout = dict(base=(26, 24, 32), noise=5, features={"front": [("glowrect", 0, 2, 3, 3, (190, 30, 24))], "bottom": [("rect", 0, 0, 3, 3, (60, 10, 10))]})
    leg = dict(base=(18, 16, 22), noise=5)
    parts = [
        P("body", (0, 13, 0), boxes=[B(-3, -3, -7, 6, 6, 13)], paint=ink),
        P("head", (0, 11, -7), boxes=[B(-2.5, -3, -5, 5, 5, 5)], paint=head, children=[
            P("snout", (0, 0, -5), boxes=[B(-1.5, -1, -3, 3, 3, 3)], paint=snout),
            P("left_ear", (1.5, -3, -2), rot=(-0.3, 0, 0.3), boxes=[B(-0.5, -3, -0.5, 1, 3, 1)], paint=leg),
            P("right_ear", (-1.5, -3, -2), rot=(-0.3, 0, -0.3), boxes=[B(-0.5, -3, -0.5, 1, 3, 1)], paint=leg)]),
        P("front_left_leg", (2, 15, -5), boxes=[B(-1, 0, -1, 2, 9, 2)], paint=leg),
        P("front_right_leg", (-2, 15, -5), boxes=[B(-1, 0, -1, 2, 9, 2, mirror=True)], paint=leg),
        P("hind_left_leg", (2, 15, 5), boxes=[B(-1, 0, -1, 2, 9, 2)], paint=leg),
        P("hind_right_leg", (-2, 15, 5), boxes=[B(-1, 0, -1, 2, 9, 2, mirror=True)], paint=leg),
        P("tail", (0, 11, 6), rot=(-0.9, 0, 0), boxes=[B(-0.5, 0, 0, 1, 1, 7)], paint=leg),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += 0.25F + Mth.sin(ageInTicks * 0.15F) * 0.08F;
        frontLeftLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        hindRightLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        frontRightLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        hindLeftLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount;
        tail.yRot += Mth.cos(ageInTicks * 0.3F) * 0.25F;
        leftEar.zRot += Mth.sin(ageInTicks * 0.2F) * 0.1F;
"""
    return Model("inkhound", "InkhoundModel", E + "InkhoundEntity", (64, 32), parts, anim)


def pale_stag():
    coat = dict(base=(226, 222, 212), noise=6, features={"all": [("spots", (206, 200, 188), 4, 2)]})
    face = dict(base=(234, 231, 222), noise=3)
    antler = dict(base=(40, 36, 44), noise=6)
    leg = dict(base=(214, 208, 196), noise=6, features={"sides": [("rect", 0, -2, 2, 0, (30, 28, 34))]})
    antler_left = P("left_antler", (1.5, -3, -1), rot=(0, 0, 0.35), boxes=[B(-0.5, -7, -0.5, 1, 7, 1)], paint=antler, children=[
        P("left_tine_a", (0, -4, 0), rot=(-0.4, 0, 0.8), boxes=[B(-0.5, -4, -0.5, 1, 4, 1)], paint=antler),
        P("left_tine_b", (0, -7, 0), rot=(0.3, 0, -0.6), boxes=[B(-0.5, -4, -0.5, 1, 4, 1)], paint=antler),
        P("left_tine_c", (0, -2, 0), rot=(-0.7, 0, 0.2), boxes=[B(-0.5, -3, -0.5, 1, 3, 1)], paint=antler)])
    antler_right = P("right_antler", (-1.5, -3, -1), rot=(0, 0, -0.35), boxes=[B(-0.5, -7, -0.5, 1, 7, 1)], paint=antler, children=[
        P("right_tine_a", (0, -4, 0), rot=(-0.4, 0, -0.8), boxes=[B(-0.5, -4, -0.5, 1, 4, 1)], paint=antler),
        P("right_tine_b", (0, -7, 0), rot=(0.3, 0, 0.6), boxes=[B(-0.5, -4, -0.5, 1, 4, 1)], paint=antler),
        P("right_tine_c", (0, -2, 0), rot=(-0.7, 0, -0.2), boxes=[B(-0.5, -3, -0.5, 1, 3, 1)], paint=antler)])
    parts = [
        P("body", (0, 9, 0), boxes=[B(-3.5, -3.5, -7, 7, 7, 14)], paint=coat),
        P("neck", (0, 7, -6), rot=(0.55, 0, 0), boxes=[B(-1.5, -8, -1.5, 3, 9, 3)], paint=coat, children=[
            P("head", (0, -8, 0), rot=(-0.55, 0, 0), boxes=[B(-2, -3, -5, 4, 4, 6)], paint=face, children=[
                antler_left, antler_right,
                P("left_ear", (2, -2.5, 0), rot=(0, 0, -0.6), boxes=[B(0, -0.5, -0.5, 3, 1, 2)], paint=face),
                P("right_ear", (-2, -2.5, 0), rot=(0, 0, 0.6), boxes=[B(-3, -0.5, -0.5, 3, 1, 2)], paint=face)])]),
        P("front_left_leg", (2.2, 12, -5.5), boxes=[B(-1, 0, -1, 2, 12, 2)], paint=leg),
        P("front_right_leg", (-2.2, 12, -5.5), boxes=[B(-1, 0, -1, 2, 12, 2, mirror=True)], paint=leg),
        P("hind_left_leg", (2.2, 12, 5.5), boxes=[B(-1, 0, -1, 2, 12, 2)], paint=leg),
        P("hind_right_leg", (-2.2, 12, 5.5), boxes=[B(-1, 0, -1, 2, 12, 2, mirror=True)], paint=leg),
        P("tail", (0, 6.5, 7), rot=(0.6, 0, 0), boxes=[B(-1, 0, 0, 2, 3, 1)], paint=face),
    ]
    anim = """        neck.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        frontLeftLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.3F * limbSwingAmount;
        hindRightLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.3F * limbSwingAmount;
        frontRightLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.3F * limbSwingAmount;
        hindLeftLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.3F * limbSwingAmount;
        leftEar.zRot += Mth.sin(ageInTicks * 0.12F) * 0.15F;
        rightEar.zRot -= Mth.sin(ageInTicks * 0.12F + 1.0F) * 0.15F;
"""
    return Model("pale_stag", "PaleStagModel", E + "PaleStagEntity", (64, 64), parts, anim)


def quillcrow():
    feather = dict(base=(27, 26, 34), noise=5, features={"top": [("spots", (60, 66, 90), 2, 1.5)], "sides": [("hstripes", 2, (18, 17, 24))]})
    nib = dict(base=STEEL, noise=3, features={"all": [("px", [(0, 0, (200, 204, 214))])]})
    parts = [
        P("body", (0, 19, 0), rot=(0.35, 0, 0), boxes=[B(-2, -2, -3, 4, 4, 7)], paint=feather, children=[
            P("head", (0, -2, -3), rot=(-0.35, 0, 0), boxes=[B(-1.5, -3, -2.5, 3, 3, 3)], paint=dict(base=(24, 23, 30), noise=4,
              features={"front": [("glow", [(0, 1, (200, 190, 150)), (2, 1, (200, 190, 150))])]}), children=[
                P("beak", (0, -1.5, -2.5), boxes=[B(-0.5, -0.5, -3, 1, 1, 3)], paint=nib)]),
            P("left_wing", (2, -2, -2), boxes=[B(0, 0, 0, 1, 3, 6)], paint=feather),
            P("right_wing", (-2, -2, -2), boxes=[B(-1, 0, 0, 1, 3, 6, mirror=True)], paint=feather),
            P("tail", (0, -1, 4), rot=(-0.3, 0, 0), boxes=[B(-1.5, 0, 0, 3, 1, 4)], paint=feather)]),
        P("left_leg", (1, 21, 0.5), boxes=[B(-0.5, 0, -0.5, 1, 3, 1)], paint=dict(base=(60, 56, 50))),
        P("right_leg", (-1, 21, 0.5), boxes=[B(-0.5, 0, -0.5, 1, 3, 1)], paint=dict(base=(60, 56, 50))),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        if (entity.isFlying()) {
            float flap = Mth.cos(ageInTicks * 1.6F) * 1.1F;
            leftWing.zRot -= 0.3F + flap;
            rightWing.zRot += 0.3F + flap;
            leftLeg.xRot += 1.2F;
            rightLeg.xRot += 1.2F;
        } else {
            head.zRot += Mth.sin(ageInTicks * 0.07F) > 0.92F ? 0.4F : 0.0F;
            leftLeg.xRot += Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount;
            rightLeg.xRot += Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * limbSwingAmount;
        }
"""
    return Model("quillcrow", "QuillcrowModel", E + "QuillcrowEntity", (64, 32), parts, anim)


def foxing_moth():
    body = dict(base=(150, 120, 86), noise=6)
    parts = [
        P("body", (0, 20, 0), boxes=[B(-1, -1, -3, 2, 2, 5)], paint=body, children=[
            P("head", (0, 0, -3), boxes=[B(-1, -1, -2, 2, 2, 2)], paint=dict(base=(120, 94, 66)), children=[
                P("antennae", (0, -1, -2), rot=(-0.5, 0, 0), boxes=[B(-1.5, -2, 0, 3, 2, 0)],
                  paint=dict(base=(90, 70, 50), shade=False, shape=lambda face, x, y, w, h: 255 if face in ("front", "back") and (x in (0, w - 1)) else 0))]),
            P("left_wing", (1, -1, -1), boxes=[B(0, 0, -3, 7, 0, 7)], paint=MOTH_WING),
            P("right_wing", (-1, -1, -1), boxes=[B(-7, 0, -3, 7, 0, 7, mirror=True)], paint=MOTH_WING)]),
    ]
    anim = """        float flap = Mth.cos(ageInTicks * 1.3F) * 0.9F;
        leftWing.zRot += -0.2F + flap;
        rightWing.zRot -= -0.2F + flap;
        body.y += Mth.sin(ageInTicks * 0.2F) * 0.6F;
"""
    return Model("foxing_moth", "FoxingMothModel", E + "FoxingMothEntity", (32, 32), parts, anim)


def blotling():
    core = dict(base=(10, 9, 14), noise=3, features={"front": [("glow", [(1, 2, (214, 208, 190)), (4, 2, (214, 208, 190))])]})
    shell = dict(base=(30, 28, 44), noise=6, alpha=150, features={"top": [("spots", (60, 56, 90), 2, 2)]})
    parts = [
        P("core", (0, 0, 0), boxes=[B(-3, 17, -3, 6, 6, 6)], paint=core),
        P("shell", (0, 0, 0), boxes=[B(-4, 16, -4, 8, 8, 8)], paint=shell),
    ]
    anim = """        core.y += Mth.sin(ageInTicks * 0.2F) * 0.3F;
"""
    return Model("blotling", "BlotlingModel", E + "BlotlingEntity", (64, 32), parts, anim, translucent=True)


def margin_crawler():
    shell = dict(base=VELLUM, noise=8, features={"sides": [("script", (70, 50, 40), 0.5)], "top": [("spots", SEPIA, 2, 1.5)]})
    foot = dict(base=(196, 170, 150), noise=8, features={"sides": [("hstripes", 2, (160, 130, 110))]})
    helmet = dict(base=STEEL, noise=5, features={"front": [("rect", 0, 1, 4, 2, INK_DEEP)]})
    lance = dict(base=(170, 150, 120), noise=5, features={"all": [("vstripes", 3, RUBRIC)]})
    parts = [
        P("foot", (0, 24, 0), boxes=[B(-2, -2, -5, 4, 2, 10)], paint=foot),
        P("shell", (0, 22, 1), boxes=[B(-3, -6, -3, 6, 6, 6)], paint=shell),
        P("head", (0, 22, -5), boxes=[B(-1.5, -3, -2, 3, 3, 3)], paint=foot, children=[
            P("helmet", (0, 0, 0), boxes=[B(-2, -4, -2.5, 4, 3, 4)], paint=helmet),
            P("lance", (1.5, -1, -1), rot=(0.15, -0.1, 0), boxes=[B(0, -0.5, -10, 1, 1, 10)], paint=lance)]),
    ]
    anim = """        float crawl = Mth.sin(limbSwing * 0.9F) * limbSwingAmount;
        shell.y += crawl * 0.6F;
        head.xRot += headPitch * Mth.DEG_TO_RAD * 0.5F;
        head.yRot += netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        lance.xRot += Mth.cos(limbSwing * 0.9F) * 0.2F * limbSwingAmount;
        if (attackTime > 0) lance.z -= Mth.sin(Mth.sqrt(attackTime) * Mth.PI) * 3.0F;
"""
    return Model("margin_crawler", "MarginCrawlerModel", E + "MarginCrawlerEntity", (64, 32), parts, anim)


def erratum_legs():
    leg = dict(base=(30, 28, 34), noise=4)
    parts = []
    idx = 0
    for side, sx in (("left", 5), ("right", -5)):
        for zi, z in enumerate((-4, 0, 4)):
            parts.append(P(f"{side}_leg_{zi}", (sx, 20, z), rot=(0, 0, 0.6 if sx > 0 else -0.6),
                           boxes=[B(-0.5, 0, -0.5, 1, 6, 1)], paint=leg))
            idx += 1
    anim = ""
    for side in ("left", "right"):
        for zi in range(3):
            phase = "" if (zi + (0 if side == "left" else 1)) % 2 == 0 else " + Mth.PI"
            anim += f"        {camel(side + '_leg_' + str(zi))}.xRot += Mth.cos(limbSwing * 1.4F{phase}) * 0.6F;\n"
    return Model("erratum_legs", "ErratumLegsModel", E + "ErratumEntity", (32, 32), parts, anim)


def palehand():
    skin = dict(base=(232, 228, 218), noise=5, features={"sides": [("hstripes", 5, (214, 208, 196))]})
    tip = dict(base=(226, 222, 212), noise=5, features={"all": [("rect", 0, 0, 2, 2, (40, 36, 48))]})
    fingers = []
    for i, (x, length) in enumerate(((-3, 5), (-1, 6), (1, 5), (3, 4))):
        fingers.append(P(f"finger_{i}", (x, -8, 0), boxes=[B(-1, -length, -1, 2, length, 2)], paint=skin, children=[
            P(f"finger_{i}_tip", (0, -length, 0), boxes=[B(-1, -4, -1, 2, 4, 2)], paint=tip)]))
    parts = [
        P("wrist", (0, 24, 0), boxes=[B(-3, -10, -2, 6, 10, 4)], paint=skin, children=[
            P("palm", (0, -10, 0), boxes=[B(-4, -8, -2, 8, 8, 4)], paint=skin, children=fingers + [
                P("thumb", (-4, -2, 0), rot=(0, 0, 0.6), boxes=[B(-2, -6, -1, 2, 6, 2)], paint=skin, children=[
                    P("thumb_tip", (-1, -6, 0), boxes=[B(-1, -3, -1, 2, 3, 2)], paint=tip)])])]),
    ]
    anim = ""
    for i in range(4):
        anim += f"        finger{i}.xRot += 0.15F + Mth.sin(ageInTicks * 0.045F + {i}.0F) * 0.35F;\n"
        anim += f"        finger{i}Tip.xRot += 0.25F + Mth.sin(ageInTicks * 0.045F + {i}.7F) * 0.4F;\n"
    anim += "        thumb.zRot += Mth.sin(ageInTicks * 0.03F) * 0.15F;\n        wrist.zRot += Mth.sin(ageInTicks * 0.02F) * 0.06F;\n"
    return Model("palehand", "PalehandModel", E + "PalehandEntity", (64, 64), parts, anim)


def bookbinder():
    books = dict(base=(96, 60, 36), noise=10, features={"sides": [("vstripes", 3, (200, 186, 150)), ("stitches", RUBRIC)],
                                                        "top": [("spots", (140, 100, 60), 4, 2), ("script", INK_BROWN, 0.4)]})
    coat = dict(base=(70, 56, 44), noise=8, features={"front": [("rect", 3, 0, 5, 12, (50, 38, 30))]})
    face = dict(base=(188, 164, 146), noise=7, features={
        "front": [("rect", 1, 3, 7, 4, (40, 34, 30)), ("glowrect", 1, 3, 3, 5, (230, 210, 140)), ("glowrect", 5, 3, 7, 5, (230, 210, 140)),
                  ("stitches", RUBRIC)],
        "top": [("spots", (120, 116, 110), 3, 2)]})
    needle = dict(base=STEEL, noise=4, features={"all": [("rect", 0, 0, 1, 2, (220, 224, 230))]})
    upper = dict(base=(80, 52, 34), noise=8, features={"sides": [("stitches", RUBRIC)]})
    legs = []
    for side, sx in (("left", 1), ("right", -1)):
        for zi, z in enumerate((-5, 0, 5)):
            name = f"{side}_leg_{zi}"
            if sx > 0:
                up = B(0, -1, -1, 12, 2, 2)
                low = B(0, -0.5, -0.5, 26, 1, 1)
                rot_up, rot_low = -0.7, 2.0
            else:
                up = B(-12, -1, -1, 12, 2, 2, mirror=True)
                low = B(-26, -0.5, -0.5, 26, 1, 1, mirror=True)
                rot_up, rot_low = 0.7, -2.0
            yaw = (zi - 1) * 0.35 * sx
            legs.append(P(name, (7 * sx, -4, z), rot=(0, yaw, rot_up), boxes=[up], paint=upper, children=[
                P(name + "_needle", (12 * sx, 0, 0), rot=(0, 0, rot_low), boxes=[low], paint=needle)]))
    parts = [
        P("heap", (0, 10, 0), boxes=[B(-7, -8, -7, 14, 10, 14)], paint=books, children=legs + [
            P("torso", (0, -8, -2), rot=(0.3, 0, 0), boxes=[B(-4, -12, -2, 8, 12, 4)], paint=coat, children=[
                P("head", (0, -12, 0), boxes=[B(-4, -8, -4, 8, 8, 8)], paint=face),
                P("left_arm", (5, -11, 0), rot=(-0.4, 0, -0.2), boxes=[B(-1, 0, -1, 2, 12, 2)], paint=coat),
                P("right_arm", (-5, -11, 0), rot=(-0.6, 0, 0.2), boxes=[B(-1, 0, -1, 2, 12, 2, mirror=True)], paint=coat, children=[
                    P("held_needle", (0, 11, 0), rot=(0.6, 0, 0), boxes=[B(-0.5, 0, -0.5, 1, 12, 1)], paint=needle)])])]),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        float step = limbSwing * 0.8F;
"""
    for side in ("left", "right"):
        for zi in range(3):
            n = camel(f"{side}_leg_{zi}")
            phase = "" if (zi + (0 if side == "left" else 1)) % 2 == 0 else " + Mth.PI"
            anim += f"        {n}.zRot += Mth.sin(step{phase}) * 0.22F * limbSwingAmount;\n"
            anim += f"        {n}.yRot += Mth.cos(step{phase}) * 0.18F * limbSwingAmount;\n"
    anim += """        if (entity.isRebinding()) {
            torso.xRot += 0.5F;
            leftArm.xRot -= 1.6F + Mth.sin(ageInTicks * 0.4F) * 0.2F;
            rightArm.xRot -= 1.6F + Mth.cos(ageInTicks * 0.4F) * 0.2F;
        } else if (entity.isStunned()) {
            torso.xRot += 0.9F;
            head.xRot += 0.6F;
            heap.y += 1.0F;
        } else if (attackTime > 0) {
            float s = Mth.sin(Mth.sqrt(attackTime) * Mth.PI);
            rightArm.xRot -= 1.8F * s;
            torso.xRot -= 0.3F * s;
        }
        heap.y += Mth.sin(ageInTicks * 0.08F) * 0.4F;
"""
    return Model("bookbinder", "BookbinderModel", "com.exonoxic.palimpsest.entity.boss.BookbinderEntity", (128, 128), parts, anim)


def rasure():
    robe = dict(base=(226, 222, 210), noise=6, features={"sides": [("script", (160, 150, 136), 0.25), ("grad", (230, 226, 214), (150, 142, 128))]})
    face = dict(base=(238, 235, 226), noise=4, features={
        "front": [("hstripes", 2, (220, 216, 206)), ("glowrect", 3, 1, 5, 3, (200, 40, 30))], "all": [("spots", (214, 210, 200), 3, 2)]})
    blade = dict(base=(150, 156, 168), noise=4, features={"all": [("rect", 0, 0, 1, 0, (230, 234, 240)), ("glowrect", -1, 0, 0, 0, (240, 240, 250))]})
    arm = dict(base=(210, 206, 194), noise=6)
    page = dict(base=VELLUM, noise=6, shade=False, features={"all": [("script", INK, 0.6)]})
    pages = []
    for i in range(4):
        a = i * math.pi / 2
        pages.append(P(f"page_{i}", (0, -22, 0), rot=(0, a, 0.2), boxes=[B(9, -3, 0, 4, 5, 0)], paint=page))
    parts = [
        P("robe", (0, 24, 0), boxes=[B(-6, -14, -4, 12, 14, 8)], paint=robe),
        P("body", (0, 10, 0), boxes=[B(-5, -14, -3, 10, 14, 6)], paint=robe, children=[
            P("head", (0, -14, 0), boxes=[B(-4, -12, -4, 8, 12, 8)], paint=face),
            P("blade_arm", (-6, -12, 0), boxes=[B(-1.5, 0, -1.5, 3, 14, 3, mirror=True)], paint=arm, children=[
                P("blade", (0, 14, 0), rot=(0.2, 0, 0), boxes=[B(-0.5, 0, -4, 1, 20, 5)], paint=blade, children=[
                    P("blade_tip", (0, 20, 0), rot=(-0.5, 0, 0), boxes=[B(-0.5, 0, -3, 1, 10, 3)], paint=blade)])]),
            P("left_arm", (6, -12, 0), boxes=[B(-1.5, 0, -1.5, 3, 16, 3)], paint=arm, children=[
                P("fingers", (0, 16, 0), boxes=[B(-2, 0, -1, 4, 8, 2)], paint=dict(base=(216, 212, 200), noise=5,
                  features={"sides": [("vstripes", 2, (170, 164, 150))]}))])] + pages),
    ]
    anim = """        head.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot += headPitch * Mth.DEG_TO_RAD;
        float bob = Mth.sin(ageInTicks * 0.1F) * 1.5F;
        body.y += bob;
        robe.y += bob * 0.6F;
        robe.xRot += Mth.cos(limbSwing * 0.6F) * 0.12F * limbSwingAmount;
        float spin = ageInTicks * 0.05F;
        page0.yRot += spin;
        page1.yRot += spin;
        page2.yRot += spin;
        page3.yRot += spin;
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
        } else {
            bladeArm.xRot += -0.25F + Mth.sin(ageInTicks * 0.06F) * 0.08F;
            leftArm.xRot += -0.15F + Mth.cos(ageInTicks * 0.06F) * 0.08F;
            fingers.xRot += Mth.sin(ageInTicks * 0.2F) * 0.2F;
        }
        if (phase == com.exonoxic.palimpsest.entity.boss.RasureEntity.BLANK_PAGE) head.zRot += 0.4F;
"""
    return Model("rasure", "RasureModel", "com.exonoxic.palimpsest.entity.boss.RasureEntity", (128, 64), parts, anim)


import creature_designs  # noqa: E402

MODELS = [creature_designs.knocker(), longhand(), smudge(), redacted(), rubricator(), copyist(), inkhound(), pale_stag(), quillcrow(),
          foxing_moth(), blotling(), margin_crawler(), erratum_legs(), palehand(), bookbinder(), rasure()]


def main():
    for m in MODELS:
        pack(m)
        paint_model(m)
        write_java(m)
    write_layers(MODELS)
    # The Erratum's legs are a model, but its texture is named after the legs, not the entity.
    print(f"generated {len(MODELS)} models")


if __name__ == "__main__":
    main()
