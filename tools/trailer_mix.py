#!/usr/bin/env python3
"""Mixes the trailer's soundtrack and finishes the video.

The game films the trailer with no sound card (client/dev/TrailerRecorder), so it writes down
every sound it starts instead: the frame, the .ogg, its loudness, pitch and pan after distance,
plus cues for the music. This script plays that log back into a stereo mix, lays the mod's own
music under it, grades the picture (letterbox, a little grain and vignette) and writes the result.

  python3 tools/trailer_mix.py run/trailer docs/trailer

Needs numpy and ffmpeg. Vanilla sounds are looked up in the asset index ForgeGradle downloads.
"""
import glob
import json
import os
import subprocess
import sys

import numpy as np

SR = 48000
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets")


def decode(path):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-ac", "2", "-ar", str(SR), "-f", "f32le", "-"],
                         check=True, capture_output=True).stdout
    return np.frombuffer(raw, dtype=np.float32).reshape(-1, 2).copy()


def vanilla_index():
    """Maps 'minecraft/sounds/...ogg' to the object file, from the newest asset index found."""
    homes = [os.path.expanduser("~/.gradle/caches/forge_gradle"), os.path.expanduser("~/.minecraft")]
    indexes = [p for h in homes for p in glob.glob(os.path.join(h, "**", "indexes", "*.json"), recursive=True)]
    if not indexes:
        return {}, None
    index = max(indexes, key=os.path.getmtime)
    objects = os.path.join(os.path.dirname(os.path.dirname(index)), "objects")
    with open(index) as f:
        entries = json.load(f)["objects"]
    return {k: os.path.join(objects, v["hash"][:2], v["hash"]) for k, v in entries.items()}, objects


def locate(resource, vanilla):
    ns, path = resource.split(":", 1)
    if ns == "minecraft":
        return vanilla.get("minecraft/" + path)
    p = os.path.join(MOD_ASSETS, ns, path)
    return p if os.path.exists(p) else None


def repitch(x, pitch):
    if abs(pitch - 1.0) < 1e-3:
        return x
    n = int(len(x) / pitch)
    src = np.arange(n) * pitch
    return np.stack([np.interp(src, np.arange(len(x)), x[:, c]) for c in (0, 1)], axis=1).astype(np.float32)


def add(mix, x, start, gain=1.0, pan=0.0):
    if start >= len(mix) or len(x) == 0:
        return
    x = x[: len(mix) - start]
    angle = (np.clip(pan, -1, 1) + 1) * np.pi / 4
    mix[start:start + len(x), 0] += x[:, 0] * gain * np.cos(angle) * 1.4142
    mix[start:start + len(x), 1] += x[:, 1] * gain * np.sin(angle) * 1.4142


def fades(x, fade_in, fade_out):
    x = x.copy()
    a, b = min(fade_in, len(x)), min(fade_out, len(x))
    if a:
        x[:a] *= np.linspace(0, 1, a)[:, None]
    if b:
        x[-b:] *= np.linspace(1, 0, b)[:, None]
    return x


def main(src, out):
    os.makedirs(out, exist_ok=True)
    fps, frames, sounds, cues = 40, None, [], []
    with open(os.path.join(src, "sounds.tsv")) as f:
        for line in f:
            p = line.rstrip("\n").split("\t")
            if p[0] == "meta" and p[1] == "fps":
                fps = int(p[2])
            elif p[0] == "meta" and p[1] == "frames":
                frames = int(p[2])
            elif p[0] == "snd":
                sounds.append((int(p[1]), p[2], float(p[3]), float(p[4]), float(p[5]), p[6] == "true"))
            elif p[0] == "cue":
                cues.append((int(p[1]), p[2]))
    if frames is None:
        frames = max([s[0] for s in sounds] + [c[0] for c in cues] + [0]) + fps * 2
    length = int(frames / fps * SR)
    mix = np.zeros((length + SR, 2), dtype=np.float32)
    vanilla, _ = vanilla_index()
    cache, missing = {}, set()

    for frame, res, gain, pitch, pan, loop in sounds:
        path = locate(res, vanilla)
        if path is None:
            missing.add(res)
            continue
        if path not in cache:
            cache[path] = decode(path)
        x = repitch(cache[path], pitch)
        if loop:
            x = fades(np.tile(x, (int(6 * SR / max(len(x), 1)) + 1, 1))[: 6 * SR], 0, SR)
        add(mix, x, int(frame / fps * SR), gain, pan)

    # Music: "music <name> <gain>" starts a track (fading out whatever was playing), "music_off" ends it.
    playing = None
    for frame, what in cues + [(frames, "music_off")]:
        at = int(frame / fps * SR)
        parts = what.split()
        if playing is not None:
            start, track, gain = playing
            add(mix, fades(track[: max(0, at - start)], SR * 2, int(SR * 1.2)), start, gain)
            playing = None
        if parts[0] == "music":
            track = decode(os.path.join(MOD_ASSETS, "palimpsest", "sounds", "music", parts[1] + ".ogg"))
            reps = int(np.ceil((length - at) / max(len(track), 1))) + 1
            playing = (at, np.tile(track, (reps, 1)), float(parts[2]))

    mix = mix[:length]
    peak = float(np.max(np.abs(mix))) or 1.0
    mix = np.tanh(mix / peak * 1.6) / np.tanh(1.6) * 0.89
    wav = os.path.join(src, "mix.wav")
    pcm = (mix * 32767).astype("<i2").tobytes()
    with open(wav, "wb") as f:
        f.write(b"RIFF" + (36 + len(pcm)).to_bytes(4, "little") + b"WAVEfmt " + (16).to_bytes(4, "little")
                + (1).to_bytes(2, "little") + (2).to_bytes(2, "little") + SR.to_bytes(4, "little")
                + (SR * 4).to_bytes(4, "little") + (4).to_bytes(2, "little") + (16).to_bytes(2, "little")
                + b"data" + len(pcm).to_bytes(4, "little") + pcm)

    video = os.path.join(out, "palimpsest_trailer.mp4")
    grade = ("crop=iw:iw/2.35:0:(ih-iw/2.35)/2,pad=iw:iw*9/16:0:(oh-ih)/2:black,"
             "eq=saturation=0.82:contrast=1.06:gamma=0.97,vignette=PI/5,noise=alls=3:allf=t,format=yuv420p")
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", os.path.join(src, "frames.mp4"), "-i", wav, "-vf", grade,
                    "-r", "30", "-c:v", "libx264", "-preset", "slow", "-crf", "25", "-c:a", "aac", "-b:a", "192k",
                    "-movflags", "+faststart", "-shortest", video], check=True)
    subprocess.run(["ffmpeg", "-y", "-v", "error", "-i", video, "-vf", "fps=1,scale=320:-2,tile=6x16:padding=2",
                    "-frames:v", "1", "-q:v", "4", os.path.join(out, "contact_sheet.jpg")], check=True)
    print(f"{len(sounds)} sounds, {len(cues)} cues, {frames} frames at {fps} fps ({frames / fps:.1f} s) -> {video}")
    if missing:
        print("missing sounds:", ", ".join(sorted(missing)))


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
