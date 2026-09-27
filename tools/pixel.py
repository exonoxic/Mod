"""
Small pixel-art toolkit shared by the Palimpsest asset generators.

Everything is deterministic: each generator seeds its own RNG from the asset name, so
re-running the tools reproduces byte-identical PNGs.
"""
import hashlib
import math
import os
import random

import numpy as np
from PIL import Image

# ---------------------------------------------------------------- palette
# The art direction: vellum, iron-gall ink, rubric red, scraped white, faded blue-grey, gilt.
VELLUM = (216, 203, 168)
VELLUM_DARK = (176, 160, 124)
VELLUM_LIGHT = (234, 226, 204)
SEPIA = (107, 74, 46)
INK = (42, 39, 51)
INK_DEEP = (21, 19, 26)
INK_BROWN = (59, 47, 42)
RUBRIC = (140, 28, 19)
RUBRIC_BRIGHT = (190, 44, 30)
SCRAPED = (237, 234, 224)
FADED_BLUE = (91, 100, 112)
FADED_GREY = (169, 162, 148)
GILT = (212, 170, 72)
GILT_LIGHT = (246, 214, 120)
BONE = (228, 224, 214)
FLESH_PALE = (201, 176, 158)
STEEL = (122, 128, 140)
STEEL_DARK = (70, 74, 84)
LEATHER = (104, 66, 40)
LEATHER_DARK = (66, 40, 24)
WOOD_DARK = (58, 44, 34)
BLOT = (14, 13, 18)


def rng_for(name):
    h = hashlib.sha256(name.encode()).digest()
    return random.Random(int.from_bytes(h[:8], "big"))


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(round(v))))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(clamp(x * f) for x in c)


def jitter(c, amount, r):
    d = r.uniform(-amount, amount)
    return tuple(clamp(x + d + r.uniform(-amount * 0.3, amount * 0.3)) for x in c)


class Canvas:
    """RGBA canvas with a handful of drawing primitives."""

    def __init__(self, w, h, fill=None):
        self.w, self.h = w, h
        self.px = np.zeros((h, w, 4), dtype=np.uint8)
        if fill is not None:
            self.fill(fill)

    def fill(self, c, alpha=255):
        self.px[:, :, 0] = c[0]
        self.px[:, :, 1] = c[1]
        self.px[:, :, 2] = c[2]
        self.px[:, :, 3] = alpha

    def set(self, x, y, c, alpha=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y, x] = (c[0], c[1], c[2], alpha)

    def get(self, x, y):
        return tuple(int(v) for v in self.px[y, x])

    def blend(self, x, y, c, a):
        """Alpha-blend colour c over the existing pixel with opacity a (0..1)."""
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        old = self.px[y, x].astype(float)
        if old[3] == 0:
            self.px[y, x] = (c[0], c[1], c[2], clamp(255 * a))
            return
        for i in range(3):
            old[i] = old[i] + (c[i] - old[i]) * a
        old[3] = max(old[3], 255 * a)
        self.px[y, x] = old.astype(np.uint8)

    def rect(self, x0, y0, x1, y1, c, alpha=255):
        for y in range(max(0, y0), min(self.h, y1)):
            for x in range(max(0, x0), min(self.w, x1)):
                self.set(x, y, c, alpha)

    def line(self, x0, y0, x1, y1, c, alpha=255):
        dx, dy = abs(x1 - x0), -abs(y1 - y0)
        sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
        err = dx + dy
        while True:
            self.set(x0, y0, c, alpha)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def circle(self, cx, cy, r, c, alpha=255, soft=False):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= r:
                    if soft:
                        self.blend(x, y, c, (1 - d / r) * alpha / 255)
                    else:
                        self.set(x, y, c, alpha)

    def noise(self, amount, r, mask_alpha=True):
        for y in range(self.h):
            for x in range(self.w):
                p = self.px[y, x]
                if mask_alpha and p[3] == 0:
                    continue
                d = r.uniform(-amount, amount)
                self.px[y, x, :3] = [clamp(int(p[i]) + d) for i in range(3)]

    def outline(self, c, alpha=255):
        """Darken transparent pixels that border opaque ones (item sprite outline)."""
        src = self.px.copy()
        for y in range(self.h):
            for x in range(self.w):
                if src[y, x, 3] != 0:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.w and 0 <= ny < self.h and src[ny, nx, 3] > 0:
                        self.px[y, x] = (c[0], c[1], c[2], alpha)
                        break

    def paste(self, other, ox, oy):
        for y in range(other.h):
            for x in range(other.w):
                p = other.px[y, x]
                if p[3] > 0:
                    self.blend(ox + x, oy + y, tuple(int(v) for v in p[:3]), p[3] / 255)

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        Image.fromarray(self.px, "RGBA").save(path, optimize=True)


def scribble(canvas, x0, y0, x1, y1, c, r, alpha=255, density=0.5, line_gap=3):
    """Lines of pseudo-handwriting inside a rectangle: the recurring motif of the mod."""
    y = y0 + 1
    while y < y1 - 1:
        x = x0 + r.randint(0, 2)
        end = x1 - r.randint(0, max(1, (x1 - x0) // 4))
        while x < end:
            word = r.randint(2, 5)
            for i in range(word):
                if x >= end:
                    break
                if r.random() < density:
                    h = r.choice((0, 0, 1, -1))
                    canvas.set(x, y + h, c, alpha)
                    if r.random() < 0.3:
                        canvas.set(x, y + h - 1, c, alpha)
                x += 1
            x += r.randint(1, 2)
        y += line_gap


def value_noise(w, h, scale, r):
    """Smooth 2D noise in [0,1], tileable-ish, for organic textures."""
    gw, gh = max(2, w // scale + 2), max(2, h // scale + 2)
    grid = [[r.random() for _ in range(gw)] for _ in range(gh)]
    out = np.zeros((h, w))
    for y in range(h):
        for x in range(w):
            gx, gy = x / scale, y / scale
            ix, iy = int(gx), int(gy)
            fx, fy = gx - ix, gy - iy
            fx = fx * fx * (3 - 2 * fx)
            fy = fy * fy * (3 - 2 * fy)
            a = grid[iy % gh][ix % gw]
            b = grid[iy % gh][(ix + 1) % gw]
            c = grid[(iy + 1) % gh][ix % gw]
            d = grid[(iy + 1) % gh][(ix + 1) % gw]
            out[y, x] = (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy
    return out
