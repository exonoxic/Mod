"""Building blocks shared by gen_models.py and creature_designs.py."""

E = "com.exonoxic.palimpsest.entity."
HEAD_LOOK = """        {head}.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        {head}.xRot += headPitch * Mth.DEG_TO_RAD;
"""


class B:
    def __init__(self, x, y, z, w, h, d, paint=None, mirror=False, inflate=0.0):
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, w, h, d
        self.paint = paint
        self.mirror = mirror
        self.inflate = inflate
        self.u = self.v = 0


class P:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=(), children=(), paint=None):
        self.name = name
        self.pivot = pivot
        self.rot = rot
        self.boxes = list(boxes)
        self.children = list(children)
        self.paint = paint

    def walk(self):
        yield self
        for c in self.children:
            yield from c.walk()


class Model:
    def __init__(self, name, cls, entity, tex, parts, anim, translucent=False, alpha=1.0, extra_imports=(), variants=None):
        self.name, self.cls, self.entity = name, cls, entity
        self.tw, self.th = tex
        self.parts = parts
        self.anim = anim
        self.translucent = translucent
        self.alpha = alpha
        self.extra_imports = extra_imports
        # Extra skins sharing this geometry: {suffix: {part name: paint}} -> <name>_<suffix>.png
        self.variants = variants or {}

    def all_parts(self):
        for p in self.parts:
            yield from p.walk()
