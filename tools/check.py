"""Static consistency check between the Java sources and the generated resources.

Catches the mistakes a compiler cannot: a registered block with no blockstate, an item
with no model, a model pointing at a missing texture, a translation key used in code but
absent from en_us.json, an advancement granted from Java that does not exist, and so on.
Exit status is the number of problems found (0 = clean).
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(__file__), "..")
JAVA = os.path.join(ROOT, "src/main/java/com/exonoxic/palimpsest")
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/palimpsest")
DATA = os.path.join(RES, "data/palimpsest")
NS = "palimpsest"

problems = []


def problem(msg):
    problems.append(msg)


def read(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def java_sources():
    for path in glob.glob(os.path.join(JAVA, "**/*.java"), recursive=True):
        yield path, read(path)


# ---------------------------------------------------------------- registries
blocks_src = read(os.path.join(JAVA, "registry/ModBlocks.java"))
items_src = read(os.path.join(JAVA, "registry/ModItems.java"))
ents_src = read(os.path.join(JAVA, "registry/ModEntities.java"))
sounds_src = read(os.path.join(JAVA, "registry/ModSounds.java"))
effects_src = read(os.path.join(JAVA, "registry/ModEffects.java"))
particles_src = read(os.path.join(JAVA, "registry/ModParticles.java"))

BLOCKS = re.findall(r'(?:withItem|BLOCKS\.register)\("([a-z0-9_]+)"', blocks_src)
BLOCK_ITEMS = re.findall(r'withItem\("([a-z0-9_]+)"', blocks_src)
NO_LOOT = set()
for m in re.finditer(r'(?:withItem|BLOCKS\.register)\("([a-z0-9_]+)"(.*?)\)\);', blocks_src, re.S):
    if "noLootTable" in m.group(2):
        NO_LOOT.add(m.group(1))
ITEMS = re.findall(r'(?:lore|ITEMS\.register)\("([a-z0-9_]+)"', items_src)
ITEMS += [n + "_spawn_egg" for n in re.findall(r'egg\("([a-z0-9_]+)"', items_src)]
# Block items registered by hand in ModBlocks (doors etc.) take the block's name.
SPECIAL_BLOCK_ITEMS = re.findall(r'(?:ModItems\.ITEMS|ITEMS)\.register\("([a-z0-9_]+)"', blocks_src)
ITEMS = sorted(set(ITEMS))
BLOCK_ITEMS += SPECIAL_BLOCK_ITEMS
ALL_ITEMS = set(ITEMS) | set(BLOCK_ITEMS)
ENTITIES = re.findall(r'ENTITIES\.register\("([a-z0-9_]+)"', ents_src)
SOUNDS = re.findall(r'reg\("([a-z0-9_.]+)"\)', sounds_src)
EFFECTS = re.findall(r'EFFECTS\.register\("([a-z0-9_]+)"', effects_src)
PARTICLES = re.findall(r'register\("([a-z0-9_]+)"', particles_src)
LORE_ITEMS = re.findall(r'lore\("([a-z0-9_]+)"', items_src)

print(f"blocks {len(BLOCKS)}, block items {len(BLOCK_ITEMS)}, items {len(ITEMS)}, entities {len(ENTITIES)}, "
      f"sounds {len(SOUNDS)}, effects {len(EFFECTS)}, particles {len(PARTICLES)}")

lang = load(os.path.join(ASSETS, "lang/en_us.json"))

# ---------------------------------------------------------------- blocks
for b in BLOCKS:
    if not os.path.exists(os.path.join(ASSETS, f"blockstates/{b}.json")):
        problem(f"block {b}: no blockstate")
    if f"block.{NS}.{b}" not in lang:
        problem(f"block {b}: no lang")
    if b not in NO_LOOT and not os.path.exists(os.path.join(DATA, f"loot_tables/blocks/{b}.json")):
        problem(f"block {b}: no loot table")

for b in BLOCK_ITEMS:
    if not os.path.exists(os.path.join(ASSETS, f"models/item/{b}.json")):
        problem(f"block item {b}: no item model")

# ---------------------------------------------------------------- items
for i in ITEMS:
    if not os.path.exists(os.path.join(ASSETS, f"models/item/{i}.json")):
        problem(f"item {i}: no item model")
    if f"item.{NS}.{i}" not in lang and not (i in BLOCKS and f"block.{NS}.{i}" in lang):
        problem(f"item {i}: no lang")
for i in LORE_ITEMS:
    if f"item.{NS}.{i}.desc" not in lang:
        problem(f"lore item {i}: no .desc")

# ---------------------------------------------------------------- entities / effects / sounds
for e in ENTITIES:
    if f"entity.{NS}.{e}" not in lang:
        problem(f"entity {e}: no lang")
for e in ENTITIES:
    if e in ("ink_bomb", "binding_needle", "palehand", "fair_copy"):  # projectiles and drop-less apparitions
        continue
    if not os.path.exists(os.path.join(DATA, f"loot_tables/entities/{e}.json")):
        problem(f"entity {e}: no loot table")
for e in EFFECTS:
    if f"effect.{NS}.{e}" not in lang:
        problem(f"effect {e}: no lang")
    if not os.path.exists(os.path.join(ASSETS, f"textures/mob_effect/{e}.png")):
        problem(f"effect {e}: no icon texture")

sounds_json = load(os.path.join(ASSETS, "sounds.json"))
for s in SOUNDS:
    if s not in sounds_json:
        problem(f"sound {s}: missing from sounds.json")
for key, ev in sounds_json.items():
    if key not in SOUNDS:
        problem(f"sounds.json {key}: not registered in ModSounds")
    for snd in ev["sounds"]:
        name = snd if isinstance(snd, str) else snd["name"]
        ns, path = name.split(":", 1) if ":" in name else ("minecraft", name)
        if ns == NS and not os.path.exists(os.path.join(ASSETS, f"sounds/{path}.ogg")):
            problem(f"sounds.json {key}: missing file {path}.ogg")
    if "subtitle" in ev and ev["subtitle"] not in lang:
        problem(f"sounds.json {key}: subtitle key {ev['subtitle']} not in lang")

for p in PARTICLES:
    if not os.path.exists(os.path.join(ASSETS, f"particles/{p}.json")):
        problem(f"particle {p}: no particle json")

# ---------------------------------------------------------------- models -> textures
def tex_exists(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns != NS:
        return True
    return os.path.exists(os.path.join(ASSETS, f"textures/{path}.png"))


def model_exists(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns != NS:
        return True
    return os.path.exists(os.path.join(ASSETS, f"models/{path}.json"))


for path in glob.glob(os.path.join(ASSETS, "models/**/*.json"), recursive=True):
    m = load(path)
    rel = os.path.relpath(path, ASSETS)
    for k, v in m.get("textures", {}).items():
        if not v.startswith("#") and not tex_exists(v):
            problem(f"{rel}: texture {k} -> {v} missing")
    if "parent" in m and not m["parent"].startswith("builtin/") and not model_exists(m["parent"]):
        problem(f"{rel}: parent {m['parent']} missing")
    for o in m.get("overrides", []):
        if not model_exists(o["model"]):
            problem(f"{rel}: override model {o['model']} missing")


def walk_models(node, out):
    if isinstance(node, dict):
        if "model" in node and isinstance(node["model"], str):
            out.append(node["model"])
        for v in node.values():
            walk_models(v, out)
    elif isinstance(node, list):
        for v in node:
            walk_models(v, out)


for path in glob.glob(os.path.join(ASSETS, "blockstates/*.json")):
    refs = []
    walk_models(load(path), refs)
    for r in refs:
        if not model_exists(r):
            problem(f"blockstates/{os.path.basename(path)}: model {r} missing")

# ---------------------------------------------------------------- lang keys used in Java
LANG_PREFIXES = ("palimpsest.", "item.palimpsest.", "block.palimpsest.", "entity.palimpsest.", "gui.palimpsest.",
                 "message.palimpsest.", "codex.palimpsest.", "key.palimpsest", "container.palimpsest.", "subtitles.palimpsest.",
                 "advancements.palimpsest.", "effect.palimpsest.", "death.attack.palimpsest")
used_keys = {}
for path, src in java_sources():
    for m in re.finditer(r'(?:translatable|translatableWithFallback|Component\.translatable)\(\s*"([^"]+)"', src):
        used_keys.setdefault(m.group(1), os.path.relpath(path, JAVA))
    for m in re.finditer(r'"((?:key|gui|message|container|palimpsest)\.[a-z0-9_.]+)"', src):
        k = m.group(1)
        if k.startswith(LANG_PREFIXES) and not k.endswith("."):
            used_keys.setdefault(k, os.path.relpath(path, JAVA))
for k, where in sorted(used_keys.items()):
    if k.endswith(".") or k.endswith("_") or "palimpsest" not in k:
        continue
    if k not in lang:
        problem(f"lang key {k} (used in {where}) missing")

# Keys built by concatenation: report the prefixes so the author can eyeball them.
dyn = set()
for path, src in java_sources():
    for m in re.finditer(r'translatable\(\s*"([a-z0-9_.]+\.)"\s*\+', src):
        dyn.add((m.group(1), os.path.relpath(path, JAVA)))

# ---------------------------------------------------------------- tooltip keys (.desc / .use)
for path, src in java_sources():
    cls = os.path.basename(path)[:-5]
    kinds = [k for k, tag in (("desc", "Tooltips.lore(this"), ("use", "Tooltips.use(this")) if tag in src]
    if not kinds:
        continue
    prefix = "block" if "extends BlockItem" in src and "getOrCreateDescriptionId" not in src else "item"
    ids = re.findall(r'ITEMS\.register\("([a-z0-9_]+)",\s*\(\)\s*->\s*new ' + cls + r'\(', items_src)
    if not ids:
        problem(f"tooltip class {cls}: no registrations found")
    for i in ids:
        for k in kinds:
            if f"{prefix}.{NS}.{i}.{k}" not in lang:
                problem(f"tooltip {prefix}.{NS}.{i}.{k} missing")

# ---------------------------------------------------------------- advancements granted from Java
advs = {os.path.relpath(p, os.path.join(DATA, "advancements"))[:-5] for p in glob.glob(os.path.join(DATA, "advancements/**/*.json"), recursive=True)}
for path, src in java_sources():
    for m in re.finditer(r'Advancements\.(?:grant|has)\([^,]+,\s*"([a-z0-9_/]+)"', src):
        if m.group(1) not in advs:
            problem(f"advancement {m.group(1)} (used in {os.path.relpath(path, JAVA)}) missing")
for path in glob.glob(os.path.join(DATA, "advancements/**/*.json"), recursive=True):
    a = load(path)
    rel = os.path.relpath(path, os.path.join(DATA, "advancements"))[:-5]
    par = a.get("parent")
    if par and par.startswith(NS + ":") and par.split(":", 1)[1] not in advs:
        problem(f"advancement {rel}: parent {par} missing")
    disp = a.get("display", {})
    for f in ("title", "description"):
        t = disp.get(f, {})
        if isinstance(t, dict) and "translate" in t and t["translate"] not in lang:
            problem(f"advancement {rel}: {f} key {t['translate']} missing")
    icon = disp.get("icon", {}).get("item", "")
    if icon.startswith(NS + ":") and icon.split(":", 1)[1] not in ALL_ITEMS:
        problem(f"advancement {rel}: icon {icon} not an item")

# ---------------------------------------------------------------- codex ids
codex_src = read(os.path.join(JAVA, "codex/CodexEntries.java"))
codex_ids = set(re.findall(r'(?:entry|add|e)\(\s*"([a-z0-9_:]+)"', codex_src))
print(f"codex entries parsed: {len(codex_ids)}")
for path, src in java_sources():
    if path.endswith("CodexEntries.java"):
        continue
    for m in re.finditer(r'(?:unlock|unlockCodex|codex)\([^;]*?"([a-z0-9_]+)"\)', src):
        cid = m.group(1)
        if cid not in codex_ids:
            problem(f"codex id {cid} (used in {os.path.relpath(path, JAVA)}) not defined")
codex_ids = {c for c in codex_ids if not c.endswith("_")}
for cid in codex_ids:
    for suffix in ("title", "text"):
        if f"codex.{NS}.{cid}.{suffix}" not in lang:
            problem(f"codex {cid}: lang codex.{NS}.{cid}.{suffix} missing")

# ---------------------------------------------------------------- data files referencing items
def check_item_ref(ref, where):
    if ref.startswith("#"):
        return
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    if ns == NS and path not in ALL_ITEMS:
        problem(f"{where}: item {ref} does not exist")


def walk_items(node, where, key=None):
    if isinstance(node, dict):
        for k, v in node.items():
            if k in ("item", "name", "result") and isinstance(v, str) and ":" in v and not v.startswith(NS + ":chests"):
                if k == "name" and node.get("type") not in ("minecraft:item",):
                    continue
                check_item_ref(v, where)
            walk_items(v, where, k)
    elif isinstance(node, list):
        for v in node:
            walk_items(v, where, key)


for path in glob.glob(os.path.join(DATA, "recipes/*.json")) + glob.glob(os.path.join(DATA, "loot_tables/**/*.json"), recursive=True):
    walk_items(load(path), os.path.relpath(path, DATA))

# ---------------------------------------------------------------- tags
for path in glob.glob(os.path.join(RES, "data/*/tags/**/*.json"), recursive=True):
    rel = os.path.relpath(path, os.path.join(RES, "data"))
    kind = rel.split(os.sep)[2]
    for v in load(path)["values"]:
        ref = v if isinstance(v, str) else v["id"]
        if ref.startswith("#") or not ref.startswith(NS + ":"):
            continue
        name = ref.split(":", 1)[1]
        pool = {"items": ALL_ITEMS, "blocks": set(BLOCKS), "entity_types": set(ENTITIES)}.get(kind)
        if pool is not None and name not in pool:
            problem(f"tag {rel}: {ref} does not exist")

# ---------------------------------------------------------------- structures
for path in glob.glob(os.path.join(DATA, "worldgen/template_pool/**/*.json"), recursive=True):
    for el in load(path)["elements"]:
        loc = el["element"].get("location")
        if loc and loc.startswith(NS + ":"):
            if not os.path.exists(os.path.join(DATA, f"structures/{loc.split(':', 1)[1]}.nbt")):
                problem(f"template pool {os.path.basename(path)}: structure {loc} missing")

for p in problems:
    print("PROBLEM:", p)
if dyn:
    print("dynamic lang prefixes (check manually):")
    for d, w in sorted(dyn):
        print("   ", d, "in", w)
print(f"{len(problems)} problem(s)")
sys.exit(min(len(problems), 100))
