"""Prints CI screenshots as base64 JPEG between markers, so they can be reviewed from the job
log when artifacts can't be downloaded. Decode with tools/ci_screens.py --decode <log> <outdir>."""
import base64
import io
import os
import re
import sys


def encode(folder):
    from PIL import Image
    if not os.path.isdir(folder):
        print("no screenshots")
        return
    for name in sorted(os.listdir(folder)):
        if not name.endswith(".png"):
            continue
        im = Image.open(os.path.join(folder, name)).convert("RGB")
        if im.width > 640:
            im = im.resize((640, round(im.height * 640 / im.width)), Image.LANCZOS)
        buf = io.BytesIO()
        im.save(buf, "JPEG", quality=72)
        data = base64.b64encode(buf.getvalue()).decode()
        print(f"BEGIN_SHOT {name}")
        for i in range(0, len(data), 4000):
            print("SHOT|" + data[i:i + 4000])
        print(f"END_SHOT {name}")


def decode(log, out):
    os.makedirs(out, exist_ok=True)
    name, chunks = None, []
    with open(log, encoding="utf-8", errors="replace") as fh:
        for line in fh:
            m = re.search(r"BEGIN_SHOT (\S+)", line)
            if m:
                name, chunks = m.group(1), []
                continue
            m = re.search(r"SHOT\|(\S+)", line)
            if m and name:
                chunks.append(m.group(1))
                continue
            if "END_SHOT" in line and name:
                with open(os.path.join(out, name.replace(".png", ".jpg")), "wb") as f:
                    f.write(base64.b64decode("".join(chunks)))
                print("wrote", name)
                name = None


if __name__ == "__main__":
    if sys.argv[1] == "--decode":
        decode(sys.argv[2], sys.argv[3])
    else:
        encode(sys.argv[1])
