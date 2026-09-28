"""
Software preview of the generated entity models, for iterating on creature designs without
launching the game.

It uses the same specs, UV packing and painting as gen_models.py and reproduces vanilla's
ModelPart maths: part pose = translate(pivot) then rotation Z*Y*X, cube faces laid out exactly
as net.minecraft.client.model.geom.ModelPart.Cube does (including mirrored boxes). The view
matches what a player standing in front of the creature sees: model +X is on the right of the
image, -Y is up, and -Z (the creature's face) points at the camera.

    python3 tools/preview_models.py knocker longhand          # rest pose, four angles
    python3 tools/preview_models.py knocker --pose lunge      # a named pose from POSES below
    python3 tools/preview_models.py --all                     # every model and pose

Images go to tools/preview/ (git-ignored).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

import gen_models as gm

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "preview")


# ---------------------------------------------------------------- maths
def rot_zyx(xr, yr, zr):
    cx, sx = math.cos(xr), math.sin(xr)
    cy, sy = math.cos(yr), math.sin(yr)
    cz, sz = math.cos(zr), math.sin(zr)
    rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return rz @ ry @ rx


def cube_polys(b):
    """The six textured quads of a box, as vanilla builds them: [(4 vertices, 4 uvs)]."""
    x0, y0, z0 = b.x - b.inflate, b.y - b.inflate, b.z - b.inflate
    x1, y1, z1 = b.x + b.w + b.inflate, b.y + b.h + b.inflate, b.z + b.d + b.inflate
    if b.mirror:
        x0, x1 = x1, x0
    v = [np.array(p, dtype=float) for p in (
        (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
        (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1))]
    u, t = b.u, b.v
    dx, dy, dz = b.w, b.h, b.d
    f4, f5, f6 = u, u + dz, u + dz + dx
    f7, f8, f9 = u + dz + dx + dx, u + dz + dx + dz, u + dz + dx + dz + dx
    f10, f11, f12 = t, t + dz, t + dz + dy
    specs = [
        ((v[5], v[4], v[0], v[1]), f5, f10, f6, f11),   # "down" in model space: visually the top
        ((v[2], v[3], v[7], v[6]), f6, f11, f7, f10),   # "up": visually the bottom
        ((v[0], v[4], v[7], v[3]), f4, f11, f5, f12),   # west (-x)
        ((v[1], v[0], v[3], v[2]), f5, f11, f6, f12),   # north (-z): the front
        ((v[5], v[1], v[2], v[6]), f6, f11, f8, f12),   # east (+x)
        ((v[4], v[5], v[6], v[7]), f8, f11, f9, f12),   # south (+z): the back
    ]
    polys = []
    for verts, u1, v1, u2, v2 in specs:
        uvs = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        verts = list(verts)
        if b.mirror:
            verts.reverse()
            uvs.reverse()
        polys.append((verts, uvs))
    return polys


def collect(model, pose):
    """World-space (model units) quads with uvs, honouring part hierarchy and pose overrides."""
    quads = []

    def visit(part, m, t):
        dx, dy, dz = pose.get(part.name, (0, 0, 0))[:3]
        off = pose.get(part.name, (0, 0, 0, 0, 0, 0))
        ox, oy, oz = (off[3:6] if len(off) >= 6 else (0, 0, 0))
        px, py, pz = part.pivot
        local_t = np.array([px + ox, py + oy, pz + oz], dtype=float)
        r = rot_zyx(part.rot[0] + dx, part.rot[1] + dy, part.rot[2] + dz)
        m2 = m @ r
        t2 = t + m @ local_t
        for b in part.boxes:
            for verts, uvs in cube_polys(b):
                quads.append(([t2 + m2 @ vv for vv in verts], uvs))
        for c in part.children:
            visit(c, m2, t2)

    for p in model.parts:
        visit(p, np.eye(3), np.zeros(3))
    return quads


# ---------------------------------------------------------------- raster
LIGHT = np.array([0.25, 1.0, -0.65])
LIGHT = LIGHT / np.linalg.norm(LIGHT)


def render(model, tex, glow, pose, yaw_deg, size=360, scale=None, pitch_deg=8.0, bg=(58, 58, 64), extra=()):
    """extra: further (model, tex, glow, pose) sets drawn into the same frame (e.g. the Erratum's block)."""
    quads = [(q, tex, glow) for q in collect(model, pose)]
    for m2, t2, g2, p2 in extra:
        quads += [(q, t2, g2) for q in collect(m2, p2)]
    yaw, pitch = math.radians(yaw_deg), math.radians(pitch_deg)
    ry = np.array([[math.cos(yaw), 0, -math.sin(yaw)], [0, 1, 0], [math.sin(yaw), 0, math.cos(yaw)]])
    rx = np.array([[1, 0, 0], [0, math.cos(pitch), -math.sin(pitch)], [0, math.sin(pitch), math.cos(pitch)]])
    view = rx @ ry
    pts = []
    for (verts, uvs), qt, qg in quads:
        pts.append(([view @ v for v in verts], uvs, qt, qg))
    allv = np.array([v for q in pts for v in q[0]])
    lo, hi = allv.min(axis=0), allv.max(axis=0)
    height = max(hi[1] - lo[1], 1.0)
    width = max(hi[0] - lo[0], 1.0)
    if scale is None:
        scale = min((size * 0.86) / height, (size * 0.86) / width)
    cx = (lo[0] + hi[0]) / 2
    cy = (lo[1] + hi[1]) / 2

    img = np.zeros((size, size, 3), dtype=float)
    for yy in range(size):
        t = yy / size
        img[yy, :] = np.array(bg) * (1.1 - 0.35 * t)
    depth = np.full((size, size), np.inf)

    for verts, uvs, tex, glow in pts:
        texw, texh = tex.shape[1], tex.shape[0]
        v = np.array(verts)
        n = np.cross(v[1] - v[0], v[2] - v[0])
        nn = np.linalg.norm(n)
        if nn < 1e-9:
            continue
        n = n / nn
        # Model space has y down; light comes from above and slightly in front.
        light_n = np.array([n[0], -n[1], n[2]])
        shade = 0.55 + 0.45 * abs(float(light_n @ LIGHT))
        sx = (v[:, 0] - cx) * scale + size / 2
        sy = (v[:, 1] - cy) * scale + size / 2
        sz = v[:, 2]
        for tri in ((0, 1, 2), (0, 2, 3)):
            ax, ay = sx[list(tri)], sy[list(tri)]
            x0, x1 = int(max(0, math.floor(ax.min()))), int(min(size - 1, math.ceil(ax.max())))
            y0, y1 = int(max(0, math.floor(ay.min()))), int(min(size - 1, math.ceil(ay.max())))
            if x1 < x0 or y1 < y0:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            (xa, xb, xc), (ya, yb, yc) = ax, ay
            den = (yb - yc) * (xa - xc) + (xc - xb) * (ya - yc)
            if abs(den) < 1e-9:
                continue
            w0 = ((yb - yc) * (gx - xc) + (xc - xb) * (gy - yc)) / den
            w1 = ((yc - ya) * (gx - xc) + (xa - xc) * (gy - yc)) / den
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            zs = sz[list(tri)]
            z = w0 * zs[0] + w1 * zs[1] + w2 * zs[2]
            uv = np.array([uvs[i] for i in tri], dtype=float)
            uu = w0 * uv[0, 0] + w1 * uv[1, 0] + w2 * uv[2, 0]
            vv = w0 * uv[0, 1] + w1 * uv[1, 1] + w2 * uv[2, 1]
            ti = np.clip(np.floor(uu).astype(int), 0, texw - 1)
            tj = np.clip(np.floor(vv).astype(int), 0, texh - 1)
            texel = tex[tj, ti]
            gl = glow[tj, ti]
            ok = inside & (texel[..., 3] > 16)
            sub = depth[y0:y1 + 1, x0:x1 + 1]
            ok &= z < sub - 1e-5
            if not ok.any():
                continue
            col = texel[..., :3].astype(float) * shade
            g = gl[..., 3:4].astype(float) / 255.0
            col = col * (1 - g) + gl[..., :3].astype(float) * g
            a = texel[..., 3:4].astype(float) / 255.0
            region = img[y0:y1 + 1, x0:x1 + 1]
            blended = region * (1 - a) + col * a
            region[ok] = blended[ok]
            sub[ok] = z[ok]
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)), scale


def ruler(im, scale, model_height_px):
    """Marks one block (16 units) on the left edge, so sizes can be compared."""
    d = ImageDraw.Draw(im)
    h = 16 * scale
    x = 8
    y1 = im.height - 10
    d.line([(x, y1), (x, y1 - h)], fill=(230, 200, 90), width=2)
    d.text((x + 4, y1 - h), "1 block", fill=(230, 200, 90))


# ---------------------------------------------------------------- poses
# Named poses approximate states from the Java animations: part -> (dxRot, dyRot, dzRot[, dx, dy, dz]).
POSES = {
    "knocker": {
        "knocking": {"right_upper_arm": (-1.45, 0, 0.2), "right_forearm": (-1.1, 0, 0), "right_hand": (0.5, 0, 0),
                     "head": (0, 0, 0.38), "neck": (0.18, 0, 0), "chest": (0.1, 0, 0)},
        "lunge": {"jaw": (0.95, 0, 0), "ring": (0.6, 0, 0), "chest": (0.18, 0, 0), "neck": (-0.25, 0, 0), "head": (-0.25, 0, 0),
                  "left_upper_arm": (-1.35, 0, -0.18), "right_upper_arm": (-1.35, 0, 0.18), "left_forearm": (0.15, 0, 0),
                  "right_forearm": (0.15, 0, 0), "left_finger_0": (0, 0, 0.35), "left_finger_2": (0, 0, -0.35),
                  "right_finger_0": (0, 0, -0.35), "right_finger_2": (0, 0, 0.35)},
        "walk": {"left_thigh": (0.8, 0, 0), "right_thigh": (-0.8, 0, 0), "right_shin": (1.0, 0, 0),
                 "left_upper_arm": (-0.4, 0, 0), "right_upper_arm": (0.4, 0, 0), "shroud_back": (-0.2, 0, 0)},
    },
    "erratum": {
        "walking": dict({p: (0, 0, 0, 0, -5, 0) for p in ["maw"] + [f"{s}_leg_{i}" for s in ("left", "right") for i in range(3)]}),
    },
    "longhand": {
        "freeze0": {"left_upper_arm": (-1.35, 0, 0), "right_upper_arm": (-1.2, 0, 0), "left_forearm": (-0.25, 0, 0),
                    "right_forearm": (-0.35, 0, 0), "left_finger_0": (0, 0, 0.5), "left_finger_3": (0, 0, -0.5),
                    "right_finger_0": (0, 0, -0.5), "right_finger_3": (0, 0, 0.5), "head": (0, 0, 0.45), "neck": (-0.2, 0, 0)},
        "freeze1": {"left_thigh": (-0.75, 0, 0), "left_shin": (0.9, 0, 0), "right_thigh": (0.35, 0, 0), "right_shin": (0.25, 0, 0),
                    "ribcage": (0.28, 0, 0), "left_upper_arm": (0.55, 0, 0), "right_upper_arm": (-0.85, 0, 0),
                    "right_forearm": (-0.4, 0, 0), "head": (0, 0.45, 0)},
        "freeze2": {"neck": (0, 0, 0.55), "head": (-0.15, 0, 1.3), "right_forearm": (-0.25, 0, 0)},
        "freeze3": {"pelvis": (0, 0, 0, 0, 9, 0), "left_thigh": (-1.25, 0, 0), "right_thigh": (-1.1, 0, 0), "left_shin": (2.1, 0, 0),
                    "right_shin": (1.95, 0, 0), "left_point": (-0.9, 0, 0), "right_point": (-0.85, 0, 0), "abdomen": (0.45, 0, 0),
                    "ribcage": (0.5, 0, 0), "neck": (-0.6, 0, 0), "head": (-0.55, 0, 0), "left_upper_arm": (-0.75, 0, 0),
                    "right_upper_arm": (-0.7, 0, 0), "left_forearm": (0.3, 0, 0), "right_forearm": (0.25, 0, 0)},
        "freeze4": {"left_upper_arm": (-2.85, 0, -0.25), "right_upper_arm": (-2.75, 0, 0.25), "left_forearm": (-0.7, 0, 0),
                    "right_forearm": (-0.75, 0, 0), "left_finger_1": (0.9, 0, 0), "left_finger_2": (0.8, 0, 0),
                    "right_finger_1": (0.85, 0, 0), "right_finger_2": (0.9, 0, 0), "head": (0.55, 0, 0)},
    },
}


def model_by_name(name):
    for m in gm.MODELS:
        if m.name == name:
            return m
    raise SystemExit(f"no model named {name}")


def closeup(name, pose_name=None, angles=(0, 30, 160), frac=0.42):
    """Double-resolution crops of the top of the model (faces, heads)."""
    m = model_by_name(name)
    gm.pack(m)
    canvas, glow = gm.paint_canvases(m)
    pose = POSES.get(name, {}).get(pose_name, {}) if pose_name else {}
    tiles = []
    for a in angles:
        im, _ = render(m, canvas.px, glow.px, pose, a, size=900, bg=BG)
        box = (225, 20, 675, int(20 + 900 * frac))
        tiles.append(im.crop(box))
    sheet = Image.new("RGB", (sum(t.width for t in tiles), tiles[0].height))
    x = 0
    for t in tiles:
        sheet.paste(t, (x, 0))
        x += t.width
    path = os.path.join(OUT, f"{name}{'_' + pose_name if pose_name else ''}_closeup.png")
    os.makedirs(OUT, exist_ok=True)
    sheet.save(path)
    return path


BG = (58, 58, 64)


def block_stub(lift_px):
    """A stand-in cobblestone block for previewing the Erratum."""
    from modelkit import B as _B, P as _P, Model as _M
    from pixel import Canvas, rng_for, jitter
    m = _M("block", "Block", "x", (64, 32), [_P("block", (0, 24 - lift_px, 0), boxes=[_B(-8, -16, -8, 16, 16, 16)])], "")
    gm.pack(m)
    c = Canvas(64, 32)
    r = rng_for("stub")
    for y in range(32):
        for x in range(64):
            g = 118 + ((x * 7 + y * 13) % 5) * 9
            c.set(x, y, jitter((g, g, g + 4), 10, r))
    return (m, c.px, Canvas(64, 32).px, {})


EXTRAS = {"erratum": lambda pose: [block_stub(5 if pose else 0)]}


def preview(name, pose_name=None, angles=(0, 35, 90, 160)):
    name, _, variant = name.partition(":")
    m = model_by_name(name)
    gm.pack(m)
    canvas, glow = gm.paint_canvases(m, variant or None)
    if variant:
        name = name + "_" + variant
    tex, gtex = canvas.px, glow.px
    pose = POSES.get(name, {}).get(pose_name, {}) if pose_name else {}
    tiles = []
    extra = EXTRAS.get(name.split("_")[0] if name.startswith("erratum") and not name.startswith("erratum_eye") else "", lambda p: [])(pose_name)
    for a in angles:
        im, scale = render(m, tex, gtex, pose, a, bg=BG, extra=extra)
        ruler(im, scale, None)
        ImageDraw.Draw(im).text((8, 6), f"{name} {pose_name or 'rest'} {a}deg", fill=(255, 255, 255))
        tiles.append(im)
    sheet = Image.new("RGB", (sum(t.width for t in tiles), tiles[0].height))
    x = 0
    for t in tiles:
        sheet.paste(t, (x, 0))
        x += t.width
    os.makedirs(OUT, exist_ok=True)
    path = os.path.join(OUT, f"{name}{'_' + pose_name if pose_name else ''}.png")
    sheet.save(path)
    # A 4x zoom of the texture sheet, for pixel-level review.
    Image.fromarray(tex).resize((m.tw * 4, m.th * 4), Image.NEAREST).save(os.path.join(OUT, f"{name}_texture.png"))
    return path


def main(argv):
    names, pose, close = [], None, False
    i = 0
    while i < len(argv):
        if argv[i] == "--light":
            global BG
            BG = (176, 178, 186)
            i += 1
            continue
        if argv[i] == "--close":
            close = True
            i += 1
            continue
        if argv[i] == "--pose":
            pose = argv[i + 1]
            i += 2
            continue
        if argv[i] == "--all":
            names = [m.name for m in gm.MODELS]
        else:
            names.append(argv[i])
        i += 1
    for n in names:
        if close:
            print(closeup(n, pose))
            continue
        print(preview(n, pose))
        if pose is None:
            for p in POSES.get(n, {}):
                print(preview(n, p))


if __name__ == "__main__":
    main(sys.argv[1:])
