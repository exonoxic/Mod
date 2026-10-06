"""
Builds the Knocker's door/window sounds from real recordings instead of synthesis.

The source clips live in tools/recordings/ (Freesound HQ previews, all CC0; see SOUND_CREDITS.md).
Each recipe cuts single knocks/taps out of a take, re-sequences them slowly, pitches them down a
little, strips the hiss, adds a small dead room and loudness-matches the result to the rest of the
mod. gen_sounds.py leaves these events' files alone (it still lists them in sounds.json).

Needs numpy, scipy and ffmpeg (with libvorbis) on PATH.  Run: python3 tools/gen_recorded.py
"""
import os
import subprocess

import numpy as np
from scipy import signal

from synth import lowpass, highpass, reverb, fade

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SND = os.path.join(ROOT, "src/main/resources/assets/palimpsest/sounds")
REC = os.path.join(ROOT, "tools/recordings")
SR = 44100

# id -> (author, title); files are tools/recordings/<id>.mp3, pages are https://freesound.org/s/<id>/
SOURCES = {
    494569: ("775noise", "Wooden door knock.wav"),
    193873: ("Ligidium", "Door Knock - 42.wav"),
    119833: ("kbnevel", "DoorKnock_heavy.aif"),
    658607: ("bpwebster", "Tapping on window - 3 types.WAV"),
    483499: ("Robson220pl", "Knocking on glass pane"),
    333788: ("ceich93", "Window_tapping.wav"),
    482226: ("vero.marengere", "Scratching Window with Nails"),
    320912: ("mickdow", "Squeak Finger on Glass.wav"),
}


def load(sid):
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", os.path.join(REC, f"{sid}.mp3"), "-ac", "1",
                          "-ar", str(SR), "-f", "f32le", "-"], capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.float32).astype(np.float64)


def denoise(x, amount=1.5, floor=0.08):
    """Spectral subtraction against the quietest tenth of the take (room hiss, mic hum)."""
    f, t_, Z = signal.stft(x, SR, nperseg=2048)
    mag = np.abs(Z)
    quiet = np.argsort(mag.sum(axis=0))[: max(1, mag.shape[1] // 10)]
    noise = mag[:, quiet].mean(axis=1, keepdims=True)
    gain = np.maximum(1 - amount * noise / (mag + 1e-12), floor)
    _, y = signal.istft(Z * gain, SR, nperseg=2048)
    return highpass(y[: len(x)], 45, sr=SR)


def hits(x, max_len=0.5, rise_db=8, within_db=30):
    """Cuts a take into its separate knocks: [(peak_db, clip), ...] in order."""
    hop = SR // 100
    env = np.sqrt(np.convolve(x ** 2, np.ones(hop) / hop, "same")[::hop] + 1e-12)
    db = 20 * np.log10(env)
    top = db.max()
    onsets, last = [], -99
    for i in range(3, len(db)):
        if db[i] - db[i - 3] > rise_db and db[i] > top - within_db and i - last > 12:
            onsets.append(i)
            last = i
    out = []
    for k, i in enumerate(onsets):
        a = max(0, (i - 4) * hop)
        b = min(len(x), a + int(max_len * SR))
        if k + 1 < len(onsets):
            b = min(b, (onsets[k + 1] - 2) * hop)
        out.append((float(db[i:i + 5].max()), fade(x[a:b].copy(), 0.002, 0.04, sr=SR)))
    return out


def repitch(x, factor):
    """Tape-style pitch shift: < 1 is lower and slower."""
    n = int(len(x) / factor)
    return np.interp(np.arange(n) * factor, np.arange(len(x)), x)


def sequence(clips, gaps, gains, tail=1.0):
    total = sum(gaps) + max(len(c) for c in clips) / SR + tail
    out = np.zeros(int(total * SR))
    t0 = 0.0
    for k, c in enumerate(clips):
        i = int(t0 * SR)
        out[i:i + len(c)] += c * gains[k] / (np.abs(c).max() + 1e-9)
        if k < len(gaps):
            t0 += gaps[k]
    return out


def trim(x, thresh_db=-60):
    a = np.abs(x)
    keep = np.where(a > a.max() * 10 ** (thresh_db / 20))[0]
    return fade(x[max(0, keep[0] - 200): keep[-1] + 1], 0.002, 0.2, sr=SR)


def master(x, target_db):
    """Loudest 400 ms at target_db RMS, peak kept under -1 dBFS."""
    x = trim(x)
    n = int(0.4 * SR)
    loud = max(np.sqrt(np.mean(x[i:i + n] ** 2)) for i in range(0, max(1, len(x) - n), n // 4))
    x = x * 10 ** (target_db / 20) / (loud + 1e-12)
    peak = np.abs(x).max()
    return x * min(1.0, 10 ** (-1 / 20) / peak)


def room(x, seed, seconds, wet, damp=3500):
    return reverb(x, np.random.default_rng(seed), seconds, wet, damp, sr=SR)


def take(sid, **kw):
    return [c for _, c in hits(denoise(load(sid)), **kw)]


# ------------------------------------------------------------------------------------ recipes
def door_knock(i):
    """Three or four slow, heavy blows on a wooden door, pitched down into something bigger."""
    rng = np.random.default_rng(100 + i)
    pool = {0: take(119833, max_len=0.45), 1: take(494569, max_len=0.45), 2: take(193873, max_len=0.6)[:1]}[i]
    count = (3, 4, 3)[i]
    picks = [pool[int(rng.integers(len(pool)))] for _ in range(count)]
    picks = [repitch(c, rng.uniform(0.74, 0.8)) for c in picks]
    gaps = [rng.uniform(0.7, 0.95) for _ in range(count - 1)]
    if i == 1:
        gaps[-1] += 0.6  # a held breath before the last one
    gains = [1.0] * (count - 1) + [0.8]
    x = lowpass(sequence(picks, gaps, gains, 1.4), 3200, sr=SR)
    return master(room(x, 10 + i, 1.3, 0.32, 2500), -12.5)


def glass_tap(i):
    """At the window: two to four slow taps on the pane, the last one softer."""
    rng = np.random.default_rng(200 + i)
    sid = (658607, 483499, 333788)[i]
    pool = take(sid, max_len=0.5)
    if sid == 658607:
        pool = pool[:4]  # the first of the three tapping styles: nail and knuckle
    count = (3, 2, 4)[i]
    picks = [repitch(pool[k % len(pool)], rng.uniform(0.86, 0.92)) for k in range(count)]
    gaps = [rng.uniform(0.75, 1.25) for _ in range(count - 1)]
    gains = [1.0] * (count - 1) + [0.55]
    x = sequence(picks, gaps, gains, 0.9)
    return master(room(x, 20 + i, 0.8, 0.22, 5000), -15.5)


def glass_scratch(i):
    """Nails dragged slowly down the pane; the second ends on a fingertip squeal."""
    x = denoise(load(482226))
    a, b = ((7.0, 10.6), (13.4, 17.4))[i]
    x = repitch(highpass(x[int(a * SR): int(b * SR)], 250, sr=SR), 0.9)
    x = fade(x, 0.25, 0.4, sr=SR)
    if i == 1:
        sq = denoise(load(320912))[int(12.25 * SR): int(13.3 * SR)]
        sq = fade(repitch(sq, 0.85), 0.05, 0.3, sr=SR)
        sq *= np.abs(x).max() / (np.abs(sq).max() + 1e-9) * 0.9
        j = len(x) - int(0.7 * SR)
        x = np.concatenate([x, np.zeros(len(sq))])
        x[j:j + len(sq)] += sq
    return master(room(x, 30 + i, 0.7, 0.2, 6000), -15.0)


def knocker_tap(i):
    """Knuckles testing a wall, lightly, somewhere else each time: quick taps, muffled by distance."""
    rng = np.random.default_rng(300 + i)
    pool = take(494569, max_len=0.25)
    count = int(rng.integers(2, 4))
    picks = [repitch(pool[int(rng.integers(len(pool)))], rng.uniform(0.95, 1.05)) for _ in range(count)]
    gaps = [rng.uniform(0.18, 0.32) for _ in range(count - 1)]
    x = lowpass(sequence(picks, gaps, [rng.uniform(0.7, 1.0) for _ in picks], 1.0), 2200, sr=SR)
    return master(room(x, 40 + i, 1.6, 0.45, 2000), -11.0)


# event -> (folder, recipe, variants); gen_sounds.py skips writing these
RECORDED = {
    "event.knock": ("event", door_knock, 3),
    "entity.knocker.glass_tap": ("entity", glass_tap, 3),
    "entity.knocker.glass_scratch": ("entity", glass_scratch, 2),
    "entity.knocker.tap": ("entity", knocker_tap, 3),
}


def save(path, x):
    pcm = np.clip(x, -1, 1).astype(np.float32).tobytes()
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", "-",
                    "-c:a", "libvorbis", "-q:a", "5", "-map_metadata", "-1", path], input=pcm, check=True)


def main():
    for event, (folder, fn, count) in RECORDED.items():
        base = event.replace(".", "_")
        for i in range(count):
            path = os.path.join(SND, folder, f"{base}_{i}.ogg")
            x = fn(i)
            save(path, x)
            print(f"{os.path.relpath(path, ROOT)}  {len(x) / SR:.2f}s")


if __name__ == "__main__":
    main()
