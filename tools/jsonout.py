import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/palimpsest")
DATA = os.path.join(RES, "data")
NS = "palimpsest"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2, ensure_ascii=False)
        fh.write("\n")


def asset(rel, obj):
    write(os.path.join(ASSETS, rel), obj)


def data(rel, obj, ns=NS):
    write(os.path.join(DATA, ns, rel), obj)


def rl(path):
    return path if ":" in path else f"{NS}:{path}"
