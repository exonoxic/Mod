"""
Synthesises every Palimpsest sound from scratch and writes sounds.json.

No samples, no recordings: everything below is oscillators, filtered noise and convolution
reverb, so the audio is original and freely licensable with the mod. Positional sounds are
mono (Minecraft only attenuates mono sources); the two score pieces are stereo.
"""
import json
import os

import numpy as np
import soundfile as sf

from synth import *

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SND = os.path.join(ROOT, "src/main/resources/assets/palimpsest/sounds")


def R(name, v=0):
    return np.random.default_rng(abs(hash_str(name)) + v * 7919)


def hash_str(s):
    h = 1469598103934665603
    for ch in s.encode():
        h = (h ^ ch) * 1099511628211 % (1 << 61)
    return h


# ============================================================ building blocks
def knock(rng, strength=1.0):
    n = int(0.35 * SR)
    burst = noise(0.35, rng) * env_exp(n, 0.012)
    body = resonator(burst, 140 + rng.uniform(-15, 15), 8) * 3 + resonator(burst, 380 + rng.uniform(-30, 30), 12) * 1.5
    thump = sine(90, 0.35) * env_exp(n, 0.05) * 0.8
    return norm(lowpass(body + thump, 2500)) * strength


def footstep(rng, surface="grass"):
    dur = 0.18
    n = int(dur * SR)
    x = noise(dur, rng) * env_adsr(n, 0.005, 0.05, 0.2, 0.1)
    if surface == "grass":
        x = bandpass(x, 400, 3500)
    else:
        x = bandpass(x, 200, 1800) + resonator(noise(dur, rng) * env_exp(n, 0.01), 180, 10) * 0.6
    return norm(x)


def syllable(rng, dur, lo=700, hi=2600):
    n = int(dur * SR)
    x = noise(dur, rng)
    f1 = rng.uniform(lo, hi)
    x = bandpass(x, f1 * 0.7, f1 * 1.3, order=2) + bandpass(x, 2500, 5500) * 0.4
    return x * env_adsr(n, dur * 0.2, dur * 0.3, 0.6, dur * 0.35)


def whisper(rng, length=1.4):
    parts, off = [], 0.0
    while off < length - 0.2:
        d = rng.uniform(0.08, 0.22)
        parts.append((off, syllable(rng, d)))
        off += d + rng.uniform(0.01, 0.12)
    return norm(place(length, parts)) * 0.8


def vocal(rng, f0_curve, dur, formants, breath=0.2, rough=0.0):
    src = saw(f0_curve, dur) + noise(dur, rng) * breath
    if rough:
        src = src * (1 + rough * noise(dur, rng))
    return norm(formant(src, formants))


VOWEL_A = [(750, 1.0, 6), (1200, 0.6, 8), (2500, 0.3, 10)]
VOWEL_O = [(500, 1.0, 6), (900, 0.5, 8), (2400, 0.2, 10)]
VOWEL_U = [(320, 1.0, 6), (800, 0.4, 8), (2300, 0.15, 10)]


def drone(rng, dur, base=55.0, voices=4, sr=SR):
    ts = t(dur, sr)
    x = np.zeros_like(ts)
    for i in range(voices):
        f = base * (1 + i * 0.5) * (1 + rng.uniform(-0.006, 0.006))
        x += np.sin(2 * np.pi * f * ts + rng.uniform(0, 6)) * (0.8 / (i + 1)) * (0.7 + 0.3 * np.sin(2 * np.pi * rng.uniform(0.03, 0.1) * ts))
    return x


def bell(freq, dur, rng, inharm=(1.0, 2.76, 5.4, 8.93)):
    n = int(dur * SR)
    x = np.zeros(n)
    for i, h in enumerate(inharm):
        x += sine(freq * h, dur) * env_exp(n, dur / (1.5 + i * 1.3)) / (i + 1)
    return x * env_adsr(n, 0.002, 0.0, 1.0, 0.02)


def scrape(rng, dur, lo=1500, hi=6000):
    n = int(dur * SR)
    x = noise(dur, rng)
    grit = np.abs(noise(dur, rng)) ** 3
    x = bandpass(x * (0.4 + grit), lo, hi) * env_adsr(n, 0.05, 0.1, 0.8, 0.2)
    return norm(am(x, rng.uniform(6, 14), 0.5))


def creak(rng, dur, f_lo=40, f_hi=120):
    n = int(dur * SR)
    rate = f_lo + (f_hi - f_lo) * (0.5 + 0.5 * np.sin(np.linspace(0, rng.uniform(2, 5), n)))
    phase = np.cumsum(rate) / SR
    pulses = (np.diff(np.floor(phase), prepend=0) > 0).astype(float)
    x = signal_conv(pulses, rng)
    x = resonator(x, rng.uniform(500, 900), 6) + resonator(x, rng.uniform(1300, 2000), 10) * 0.5
    return norm(x * env_adsr(n, 0.05, 0.1, 0.9, 0.1))


def signal_conv(p, rng):
    k = np.exp(-np.arange(200) / 30.0) * rng.uniform(-1, 1, 200)
    return np.convolve(p, k)[: len(p)]


def whoosh(rng, dur, up=True):
    n = int(dur * SR)
    x = noise(dur, rng)
    cut = np.linspace(300, 3000, n) if up else np.linspace(3000, 300, n)
    y = np.zeros(n)
    step = 512
    for i in range(0, n, step):
        seg = x[max(0, i - 64): i + step]
        y[i:i + step] = lowpass(seg, cut[i])[-len(y[i:i + step]):]
    return norm(y * np.sin(np.linspace(0, np.pi, n)) ** 2)


def page_flip(rng, dur=0.35):
    n = int(dur * SR)
    x = noise(dur, rng) * env_adsr(n, 0.02, 0.1, 0.5, 0.15)
    x = am(highpass(x, 900), rng.uniform(25, 45), 0.8)
    return norm(bandpass(x, 900, 7000))


def growl(rng, dur, base=70):
    f = lambda ts: base + 15 * np.sin(2 * np.pi * 3 * ts) + rng.uniform(-5, 5)
    x = vocal(rng, f, dur, VOWEL_U, breath=0.6, rough=0.6)
    return norm(am(lowpass(x, 1400), rng.uniform(18, 30), 0.6) * env_adsr(len(x), 0.1, 0.1, 0.8, 0.2))


def stinger(rng, dur=3.0, cluster=(220, 233, 311, 330, 466)):
    n = int(dur * SR)
    x = np.zeros(n)
    for f in cluster:
        x += saw(f * (1 + rng.uniform(-0.01, 0.01)), dur) * 0.3
    x = lowpass(x, 2500) * env_adsr(n, 0.005, 0.3, 0.35, 1.8)
    hit = noise(dur, rng) * env_exp(n, 0.04)
    return norm(reverb(x + lowpass(hit, 3000) * 0.8, rng, 2.2, 0.5))


def save(path, x, sr=SR, stereo=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    x = np.nan_to_num(x)
    if stereo and x.ndim == 1:
        x = np.stack([x, x], axis=1)
    x = np.clip(x, -1, 1).astype(np.float32)
    # libsndfile's Vorbis encoder can crash on very large single writes; stream in blocks.
    channels = 2 if x.ndim == 2 else 1
    with sf.SoundFile(path, "w", samplerate=sr, channels=channels, format="OGG", subtype="VORBIS") as fh:
        for i in range(0, len(x), 4096):
            fh.write(x[i:i + 4096])


# ============================================================ recipes (name -> list of clips)
def r_knock(i):
    return knock(R("knock", i))


def r_footsteps(i):
    rng = R("steps", i)
    surface = "grass" if i != 1 else "stone"
    parts, off = [], 0.0
    for k in range(rng.integers(4, 7)):
        parts.append((off, footstep(rng, surface) * (0.6 + 0.1 * k)))
        off += rng.uniform(0.38, 0.55)
    return lowpass(place(off + 0.3, parts), 3500)


def r_whisper(i):
    rng = R("whisper", i)
    return reverb(whisper(rng, rng.uniform(1.0, 1.8)), rng, 0.6, 0.2)


def r_scream(i):
    rng = R("scream", i)
    dur = 1.8
    f = lambda ts: 500 + 450 * np.sin(np.pi * ts / dur) ** 0.6 + 30 * np.sin(2 * np.pi * 6 * ts)
    x = vocal(rng, f, dur, VOWEL_A, breath=0.3, rough=0.3) * env_adsr(int(dur * SR), 0.15, 0.3, 0.7, 0.6)
    return norm(distance(x, rng, 1.0))


def r_stinger(i):
    rng = R("stinger", i)
    clusters = [(220, 233, 311, 330, 466), (110, 117, 164, 175, 247), (293, 311, 415, 440)]
    return stinger(rng, 3.0, clusters[i % 3])


def r_heartbeat(i):
    rng = R("heart", i)
    beat = lambda: lowpass(sine(55, 0.25) * env_exp(int(0.25 * SR), 0.06) + noise(0.25, rng) * env_exp(int(0.25 * SR), 0.01) * 0.2, 200)
    parts = []
    for k in range(4):
        parts += [(k * 0.9, beat()), (k * 0.9 + 0.22, beat() * 0.7)]
    return norm(place(3.8, parts))


def r_tinnitus(i):
    n = int(4.0 * SR)
    return sine(lambda ts: 6800 + 30 * np.sin(ts * 2), 4.0) * env_adsr(n, 1.0, 0.5, 0.6, 2.0) * 0.25


def r_breath(i):
    rng = R("breath", i)
    parts = []
    for k in range(2):
        d = 1.1
        n = int(d * SR)
        inh = bandpass(noise(d, rng), 300, 2500) * env_adsr(n, 0.4, 0.2, 0.5, 0.4)
        parts.append((k * 2.3, inh))
        parts.append((k * 2.3 + 1.1, lowpass(noise(d, rng), 900) * env_adsr(n, 0.2, 0.3, 0.4, 0.5) * 1.3))
    return norm(place(4.6, parts))


def r_door_creak(i):
    rng = R("door", i)
    return reverb(creak(rng, 1.6, 30, 90), rng, 0.8, 0.25)


def r_page_turn_distant(i):
    rng = R("pagedist", i)
    return norm(distance(page_flip(rng, 0.6), rng, 0.8))


def r_scrape_distant(i):
    rng = R("scrapedist", i)
    return norm(distance(scrape(rng, 2.6, 700, 4000), rng, 0.9))


def r_silence_break(i):
    rng = R("snap", i)
    n = int(2.0 * SR)
    crack = highpass(noise(2.0, rng), 1500) * env_exp(n, 0.008)
    boom = sine(lambda ts: 60 * np.exp(-ts * 2), 2.0) * env_exp(n, 0.5)
    return norm(reverb(crack + boom, rng, 2.0, 0.45))


def r_rasorium(i):
    rng = R("rasorium", i)
    return scrape(rng, 0.5 + 0.1 * i, 2000, 7500) * 0.8


def r_page_turn(i):
    return page_flip(R("page", i))


def r_quill(i):
    rng = R("quill", i)
    parts, off = [], 0.0
    for k in range(rng.integers(6, 11)):
        d = rng.uniform(0.02, 0.07)
        parts.append((off, bandpass(noise(d, rng), 3000, 9000) * env_adsr(int(d * SR), 0.003, 0.01, 0.6, 0.01)))
        off += d + rng.uniform(0.02, 0.08)
    return norm(place(off + 0.05, parts)) * 0.7


def r_bookmark(i):
    rng = R("bookmark", i)
    return norm(place(1.4, [(0, page_flip(rng, 0.3) * 0.6), (0.2, bell(880, 1.2, rng) * 0.5)]))


def r_lens(i):
    rng = R("lens", i)
    return norm(bell(1760, 1.2, rng, (1.0, 2.1, 3.9)) + fit(bell(2637, 1.0, rng, (1.0, 1.5)), int(1.2 * SR)) * 0.4)


def r_censer(i):
    rng = R("censer", i)
    chain = sum(bell(rng.uniform(3000, 5000), 0.3, rng, (1.0, 1.7)) for _ in range(3))
    return norm(place(0.9, [(0, whoosh(rng, 0.6) * 0.8), (0.1, chain * 0.3), (0.35, chain * 0.2)]))


def r_tear_ambient(i):
    rng = R("tear", i)
    dur = 2.5
    n = int(dur * SR)
    crackle = (rng.random(n) < 0.004) * rng.uniform(-1, 1, n)
    crackle = bandpass(signal_conv(crackle, rng), 600, 5000)
    low = drone(rng, dur, 40, 3) * 0.4
    return norm((crackle + low) * env_adsr(n, 0.4, 0.2, 0.8, 0.8))


def r_tear_seal(i):
    rng = R("seal", i)
    n = int(1.2 * SR)
    hiss = highpass(noise(1.2, rng), 2000) * env_exp(n, 0.3) * 0.5
    thud = lowpass(noise(1.2, rng), 300) * env_exp(n, 0.05) + sine(80, 1.2) * env_exp(n, 0.15)
    return norm(hiss + thud)


def r_veil_ambient(i):
    rng = R("veil", i)
    return norm(reverb(fit(whisper(rng, 2.0), int(2.0 * SR)) * 0.6 + drone(rng, 2.0, 220, 3) * 0.08, rng, 1.2, 0.5))


def r_veil_travel(i):
    rng = R("travel", i)
    n = int(3.0 * SR)
    tone = sine(lambda ts: 440 * np.exp(-ts * 0.7), 3.0) * env_adsr(n, 0.2, 0.5, 0.6, 1.0) * 0.4
    flutter = am(page_flip(rng, 3.0), 12, 0.6) * 0.5
    return norm(reverb(fit(whoosh(rng, 3.0, False), n) + fit(tone, n) + fit(flutter, n), rng, 1.5, 0.4))


def r_gate_open(i):
    rng = R("gate", i)
    dur = 5.0
    n = int(dur * SR)
    boom = lowpass(noise(dur, rng), 120) * env_exp(n, 0.4) * 2 + sine(45, dur) * env_exp(n, 0.8)
    choir = sum(vocal(rng, lambda ts, f=f: f + 2 * np.sin(ts * 5), dur, VOWEL_O, 0.1) for f in (110, 131, 165, 196))
    choir = choir * env_adsr(n, 1.5, 0.5, 0.7, 1.5) * 0.3
    return norm(reverb(fit(boom, n) + fit(choir, n), rng, 3.0, 0.5))


def r_ritual_begin(i):
    rng = R("ritual", i)
    dur = 4.0
    n = int(dur * SR)
    choir = sum(vocal(rng, lambda ts, f=f: f * (1 + 0.004 * np.sin(ts * 4)), dur, VOWEL_A if k % 2 else VOWEL_U, 0.15)
                for k, f in enumerate((98, 147, 185, 220)))
    return norm(reverb(choir * env_adsr(n, 1.2, 0.5, 0.8, 1.2), rng, 2.5, 0.5))


def r_ritual_complete(i):
    rng = R("complete", i)
    b = bell(523, 3.0, rng) + bell(784, 3.0, rng) * 0.6 + fit(bell(1046, 2.5, rng), int(3.0 * SR)) * 0.3
    return norm(reverb(b, rng, 2.5, 0.45))


def r_ritual_fail(i):
    rng = R("fail", i)
    n = int(1.2 * SR)
    thud = lowpass(noise(1.2, rng), 250) * env_exp(n, 0.08) + sine(70, 1.2) * env_exp(n, 0.1)
    fizz = highpass(noise(1.2, rng), 3000) * env_exp(n, 0.25) * 0.3
    return norm(thud + fizz)


def r_ward_hum(i):
    rng = R("ward", i)
    n = int(3.0 * SR)
    x = sum(sine(110 * h, 3.0) / h for h in (1, 2, 3, 5)) * env_adsr(n, 0.8, 0.2, 0.8, 1.0)
    return norm(am(x, 0.8, 0.3)) * 0.6


def r_blank_erase(i):
    rng = R("erase", i)
    n = int(0.9 * SR)
    x = bandpass(noise(0.9, rng), 800, 4000)
    x = am(x, 18, 0.9) * env_adsr(n, 0.02, 0.1, 0.8, 0.3)
    return norm(x)


def r_sealed_door(i):
    rng = R("sealdoor", i)
    parts = []
    for k in range(9):
        p = bandpass(noise(0.1, rng), 1500, 5000) * env_exp(int(0.1 * SR), 0.01)
        parts.append((k * 0.11 + rng.uniform(0, 0.03), p))
    parts.append((0.3, creak(rng, 1.4, 25, 60) * 0.5))
    return norm(reverb(place(2.0, parts), rng, 1.0, 0.3))


# ---------------------------------------------------------------- creatures
def r_blot(kind):
    def fn(i):
        rng = R("blot" + kind, i)
        dur = 0.4 if kind != "death" else 0.9
        n = int(dur * SR)
        x = lowpass(noise(dur, rng), 700) * env_adsr(n, 0.01, 0.1, 0.5, dur * 0.5)
        x = x + resonator(noise(dur, rng) * env_exp(n, 0.02), rng.uniform(180, 300), 5)
        if kind == "death":
            x = x * np.linspace(1, 0.2, n) + fit(pitch(x, 0.7), n) * 0.4
        return norm(x)
    return fn


def r_smudge(kind):
    def fn(i):
        rng = R("smudge" + kind, i)
        if kind == "ambient":
            dur = 2.0
            x = vocal(rng, lambda ts: 140 - 25 * ts, dur, VOWEL_O, breath=1.2) * env_adsr(int(dur * SR), 0.5, 0.4, 0.5, 0.8)
            return norm(reverb(lowpass(x, 2200), rng, 1.2, 0.4)) * 0.7
        if kind == "hurt":
            dur = 0.5
            return norm(vocal(rng, lambda ts: 260 + 80 * ts, dur, VOWEL_A, breath=0.8) * env_adsr(int(dur * SR), 0.02, 0.1, 0.5, 0.3))
        dur = 2.4
        x = vocal(rng, lambda ts: 300 * np.exp(-ts * 0.8), dur, VOWEL_O, breath=0.7) * env_adsr(int(dur * SR), 0.1, 0.3, 0.6, 1.5)
        return norm(reverb(x, rng, 2.0, 0.55))
    return fn


def r_crawler(kind):
    def fn(i):
        rng = R("crawler" + kind, i)
        clicks = []
        count = {"ambient": 8, "hurt": 5, "death": 12}[kind]
        for k in range(count):
            c = bandpass(noise(0.03, rng), 2000, 7000) * env_exp(int(0.03 * SR), 0.004)
            clicks.append((k * rng.uniform(0.04, 0.09), c))
        x = place(1.0, clicks)
        if kind != "ambient":
            x = x + fit(vocal(rng, lambda ts: 900 - 300 * ts, 0.3, VOWEL_A, 0.5), len(x)) * 0.2
        return norm(x) * 0.8
    return fn


def r_crow(kind):
    def fn(i):
        rng = R("crow" + kind, i)
        if kind == "flap":
            parts = [(k * 0.12, lowpass(noise(0.08, rng), 1500) * env_exp(int(0.08 * SR), 0.02)) for k in range(4)]
            return norm(place(0.6, parts)) * 0.7
        dur = 0.45 if kind != "death" else 0.8
        f = lambda ts: 520 + 120 * np.sin(np.pi * ts / dur) - (200 * ts if kind == "death" else 0)
        x = vocal(rng, f, dur, [(1100, 1.0, 5), (1700, 0.7, 6), (2800, 0.3, 8)], breath=0.4, rough=0.8)
        x = x * env_adsr(len(x), 0.02, 0.1, 0.7, 0.15)
        parts = [(0, x)]
        if kind == "ambient":
            parts.append((0.55, x * 0.8))
        return norm(reverb(place(1.4, parts), rng, 0.8, 0.2))
    return fn


def r_stag(kind):
    def fn(i):
        rng = R("stag" + kind, i)
        dur = {"ambient": 2.2, "hurt": 0.6, "death": 2.0}[kind]
        f = lambda ts: (180 if kind != "hurt" else 300) + 90 * np.sin(np.pi * ts / dur) - 40 * ts
        x = vocal(rng, f, dur, VOWEL_O, breath=0.5, rough=0.4) * env_adsr(int(dur * SR), 0.1, 0.2, 0.7, 0.5)
        x = x + fit(pitch(x, 0.94), len(x)) * 0.5
        return norm(distance(x, rng, 0.5 if kind == "ambient" else 0.1))
    return fn


def r_hound(kind):
    def fn(i):
        rng = R("hound" + kind, i)
        if kind == "ambient":
            return growl(rng, 1.4, 65)
        if kind == "howl":
            dur = 2.8
            f = lambda ts: 330 + 220 * np.sin(np.pi * np.minimum(ts / 1.2, 1)) - 80 * np.maximum(ts - 1.4, 0)
            x = vocal(rng, f, dur, VOWEL_U, 0.3) * env_adsr(int(dur * SR), 0.3, 0.3, 0.8, 1.0)
            return norm(reverb(x, rng, 2.0, 0.45))
        if kind == "hurt":
            return norm(vocal(rng, lambda ts: 700 - 400 * ts, 0.35, VOWEL_A, 0.3) * env_adsr(int(0.35 * SR), 0.01, 0.1, 0.6, 0.2))
        return norm(growl(rng, 1.8, 50) * np.linspace(1, 0, int(1.8 * SR)))
    return fn


def r_moth(i):
    rng = R("moth", i)
    x = am(lowpass(noise(0.8, rng), 1200), 34, 0.9) * env_adsr(int(0.8 * SR), 0.1, 0.2, 0.6, 0.3)
    return norm(x) * 0.5


def r_rubricator(kind):
    def fn(i):
        rng = R("rub" + kind, i)
        if kind == "ambient":
            parts, off = [], 0.0
            for k in range(rng.integers(3, 6)):
                d = rng.uniform(0.15, 0.35)
                parts.append((off, vocal(rng, lambda ts: 110 + 10 * np.sin(ts * 9), d, VOWEL_O if k % 2 else VOWEL_U, 0.6)
                              * env_adsr(int(d * SR), 0.03, 0.05, 0.7, 0.08)))
                off += d + rng.uniform(0.05, 0.2)
            return norm(reverb(lowpass(place(off, parts), 2000), rng, 0.8, 0.25)) * 0.7
        if kind == "yes":
            return norm(vocal(rng, lambda ts: 120 + 30 * ts, 0.5, VOWEL_U, 0.4) * env_adsr(int(0.5 * SR), 0.05, 0.1, 0.7, 0.2))
        if kind == "no":
            return norm(vocal(rng, lambda ts: 140 - 40 * ts, 0.5, VOWEL_O, 0.4) * env_adsr(int(0.5 * SR), 0.05, 0.1, 0.7, 0.2))
        if kind == "hurt":
            return norm(vocal(rng, lambda ts: 200 + 60 * ts, 0.4, VOWEL_A, 0.5) * env_adsr(int(0.4 * SR), 0.01, 0.1, 0.6, 0.2))
        return norm(reverb(vocal(rng, lambda ts: 180 * np.exp(-ts), 2.0, VOWEL_O, 0.5) * env_adsr(int(2 * SR), 0.1, 0.3, 0.5, 1.2), rng, 1.8, 0.5))
    return fn


def r_knocker(kind):
    def fn(i):
        rng = R("knocker" + kind, i)
        if kind == "ambient":
            return r_breath(i + 10) * 0.8
        if kind == "lunge":
            dur = 1.2
            x = vocal(rng, lambda ts: 380 + 500 * np.sin(np.pi * ts / dur), dur, VOWEL_A, 0.6, rough=1.0)
            return norm(reverb(x * env_adsr(int(dur * SR), 0.02, 0.2, 0.7, 0.4), rng, 1.0, 0.3))
        if kind == "hurt":
            return norm(vocal(rng, lambda ts: 160 + 100 * ts, 0.4, VOWEL_U, 0.7, rough=0.5) * env_adsr(int(0.4 * SR), 0.01, 0.1, 0.6, 0.2))
        return norm(reverb(vocal(rng, lambda ts: 220 * np.exp(-ts * 0.6), 2.5, VOWEL_A, 0.8, 0.6) * env_adsr(int(2.5 * SR), 0.05, 0.3, 0.6, 1.5), rng, 2.0, 0.5))
    return fn


def r_copyist(kind):
    def fn(i):
        rng = R("copy" + kind, i)
        if kind == "reveal":
            dur = 1.6
            n = int(dur * SR)
            tear = bandpass(noise(dur, rng), 800, 6000) * (np.abs(noise(dur, rng)) ** 4) * env_adsr(n, 0.01, 0.3, 0.6, 0.6)
            crack = lowpass(noise(dur, rng), 400) * env_exp(n, 0.05)
            scream = vocal(rng, lambda ts: 250 + 500 * np.minimum(ts, 0.8), dur, VOWEL_A, 0.5, 0.8) * env_adsr(n, 0.3, 0.3, 0.7, 0.5) * 0.7
            return norm(reverb(fit(tear, n) * 2 + fit(crack, n) + fit(scream, n), rng, 1.0, 0.3))
        if kind == "ambient":
            dur = 1.5
            moo = vocal(rng, lambda ts: 95 + 10 * np.sin(ts * 3), dur, VOWEL_U, 0.2) * env_adsr(int(dur * SR), 0.2, 0.2, 0.8, 0.4)
            return norm(moo + fit(pitch(moo, 1.07), len(moo)) * 0.6)
        if kind == "hurt":
            return norm(vocal(rng, lambda ts: 300 - 100 * ts, 0.4, VOWEL_A, 0.6, 0.7) * env_adsr(int(0.4 * SR), 0.01, 0.1, 0.6, 0.2))
        return norm(reverb(vocal(rng, lambda ts: 200 * np.exp(-ts * 0.5), 2.0, VOWEL_O, 0.7, 0.6) * env_adsr(int(2 * SR), 0.05, 0.3, 0.6, 1.2), rng, 1.5, 0.4))
    return fn


def r_longhand(kind):
    def fn(i):
        rng = R("long" + kind, i)
        if kind == "ambient":
            return reverb(creak(rng, 1.8, 15, 45), rng, 1.5, 0.4) * 0.8
        if kind == "move":
            return norm(place(0.8, [(0, creak(rng, 0.35, 60, 160)), (0.05, whoosh(rng, 0.5) * 0.4)]))
        if kind == "hurt":
            return norm(creak(rng, 0.4, 100, 250))
        return norm(reverb(creak(rng, 2.5, 10, 80) + fit(pitch(creak(rng, 2.5, 10, 60), 0.7), int(2.5 * SR)) * 0.5, rng, 2.0, 0.5))
    return fn


def r_redacted(kind):
    def fn(i):
        rng = R("red" + kind, i)
        if kind == "ambient":
            dur = 1.5
            x = bandpass(noise(dur, rng), 2000, 8000) * (rng.random(int(dur * SR)) < 0.3) * env_adsr(int(dur * SR), 0.2, 0.2, 0.7, 0.4)
            return norm(x) * 0.5
        if kind == "attack":
            return norm(scrape(rng, 0.35, 1500, 8000))
        if kind == "hurt":
            return norm(bitcrush(vocal(rng, lambda ts: 250 + 50 * ts, 0.35, VOWEL_A, 0.5), 4, 8))
        return norm(reverb(bitcrush(scrape(rng, 1.5, 500, 5000), 3, 12), rng, 1.5, 0.4))
    return fn


def r_erratum(kind):
    def fn(i):
        rng = R("err" + kind, i)
        if kind == "ambient":
            dur = 1.0
            x = lowpass(noise(dur, rng), 500) * (np.abs(noise(dur, rng)) ** 2) * env_adsr(int(dur * SR), 0.1, 0.1, 0.8, 0.2)
            return norm(x) * 0.6
        dur = 0.9
        x = saw(lambda ts: 200 + 1800 * (ts * 7 % 1), dur) * env_adsr(int(dur * SR), 0.01, 0.2, 0.7, 0.3)
        return norm(bitcrush(x, 3, 20))
    return fn


def r_palehand(i):
    rng = R("palehand", i)
    dur = 7.0
    n = int(dur * SR)
    x = drone(rng, dur, 32.7, 6) * env_adsr(n, 2.5, 1.0, 0.7, 3.0)
    x = x + fit(reverb(bell(65.4, 6.0, rng) * 0.3, rng, 3.0, 0.6), n)
    return norm(lowpass(x, 800))


def r_fair_copy(i):
    rng = R("fair", i)
    x = whoosh(rng, 0.9, True)[::-1]
    return norm(reverb(x, rng, 1.0, 0.5)) * 0.7


def r_bookbinder(kind):
    def fn(i):
        rng = R("bb" + kind, i)
        if kind == "ambient":
            parts = [(k * rng.uniform(0.08, 0.14), bell(rng.uniform(2500, 4000), 0.15, rng, (1.0, 1.9)) * 0.5) for k in range(10)]
            return norm(place(1.8, parts) + fit(growl(rng, 1.8, 55), int(1.8 * SR)) * 0.4)
        if kind == "needle":
            return norm(place(0.6, [(0, whoosh(rng, 0.3) * 0.6), (0.25, bell(3200, 0.3, rng, (1.0, 2.4)))]))
        if kind == "rebind":
            dur = 3.0
            squeal = sine(lambda ts: 800 + 700 * ts / dur + 40 * np.sin(ts * 30), dur) * env_adsr(int(dur * SR), 0.3, 0.3, 0.7, 0.8)
            return norm(reverb(fit(squeal, int(dur * SR)) * 0.4 + fit(creak(rng, dur, 30, 90), int(dur * SR)) * 0.6, rng, 1.5, 0.4))
        if kind == "hurt":
            return norm(vocal(rng, lambda ts: 140 + 60 * ts, 0.5, VOWEL_U, 0.6, 0.5) * env_adsr(int(0.5 * SR), 0.01, 0.1, 0.6, 0.3) + fit(page_flip(rng, 0.5), int(0.5 * SR)) * 0.5)
        dur = 4.0
        n = int(dur * SR)
        thuds = place(dur, [(k * rng.uniform(0.15, 0.3), lowpass(noise(0.3, rng), 400) * env_exp(int(0.3 * SR), 0.05)) for k in range(10)])
        flutter = am(page_flip(rng, dur), 15, 0.7) * 0.6
        voice = vocal(rng, lambda ts: 160 * np.exp(-ts * 0.4), dur, VOWEL_O, 0.5) * env_adsr(n, 0.1, 0.5, 0.5, 2.0) * 0.5
        return norm(reverb(fit(thuds, n) + fit(flutter, n) + fit(voice, n), rng, 2.0, 0.4))
    return fn


def r_rasure(kind):
    def fn(i):
        rng = R("ras" + kind, i)
        if kind == "ambient":
            return norm(reverb(lowpass(scrape(rng, 2.0, 300, 2000), 1500) * 0.6 + fit(r_breath(i + 20), int(2.0 * SR)) * 0.4, rng, 2.0, 0.5))
        if kind == "scrape":
            return norm(reverb(scrape(rng, 2.2, 1200, 7000), rng, 2.5, 0.5))
        if kind == "phase":
            dur = 4.0
            n = int(dur * SR)
            roar = sum(saw(lambda ts, f=f: f * (1 + 0.1 * np.sin(ts * 7)), dur) for f in (55, 58.3, 82.4, 110))
            roar = lowpass(roar, 900) * env_adsr(n, 0.3, 0.5, 0.7, 1.5)
            return norm(reverb(fit(roar, n) + fit(lowpass(noise(dur, rng), 600) * env_adsr(n, 0.1, 0.4, 0.4, 1.5), n), rng, 3.0, 0.5))
        if kind == "hurt":
            return norm(place(0.8, [(0, scrape(rng, 0.4, 2000, 8000)), (0.02, vocal(rng, lambda ts: 90 + 40 * ts, 0.5, VOWEL_U, 0.8, 0.8) * 0.8)]))
        dur = 7.0
        n = int(dur * SR)
        dissolve = am(highpass(noise(dur, rng), 1000), 20, 0.8) * env_adsr(n, 0.5, 1.0, 0.6, 4.0) * 0.4
        fall = sine(lambda ts: 440 * np.exp(-ts * 0.5), dur) * env_adsr(n, 0.1, 1.0, 0.5, 4.0) * 0.4
        choir = sum(vocal(rng, lambda ts, f=f: f, dur, VOWEL_A, 0.1) for f in (220, 277, 330)) * env_adsr(n, 2.0, 1.0, 0.6, 3.0) * 0.2
        return norm(reverb(fit(dissolve, n) + fit(fall, n) + fit(choir, n), rng, 3.5, 0.55))
    return fn


# ---------------------------------------------------------------- ambience & music
def r_undertext_loop(i):
    rng = R("loop", i)
    dur = 24.0
    n = int(dur * SR)
    x = drone(rng, dur, 41.2, 5) * 0.5
    wind = lowpass(noise(dur, rng), 400) * (0.4 + 0.3 * np.sin(2 * np.pi * t(dur) / dur * 2))
    x = x + wind * 0.5
    # Make the loop seamless: crossfade the tail into the head.
    fl = int(2.0 * SR)
    x[:fl] = x[:fl] * np.linspace(0, 1, fl) + x[-fl:] * np.linspace(1, 0, fl)
    return norm(x[: n - fl]) * 0.6


def r_additions(i):
    rng = R("add", i)
    kinds = [lambda: distance(page_flip(rng, 0.5), rng, 1.0), lambda: distance(r_quill(i + 5), rng, 0.8),
             lambda: distance(whisper(rng, 1.2), rng, 0.7), lambda: distance(bell(rng.uniform(300, 500), 3.0, rng), rng, 1.0) * 0.5]
    return norm(kinds[i % 4]()) * 0.7


def r_mood(i):
    rng = R("mood", i)
    dur = 6.0
    n = int(dur * SR)
    x = drone(rng, dur, 36.7, 4) * env_adsr(n, 2.0, 1.0, 0.6, 2.5) + fit(distance(whisper(rng, 2.0), rng, 1.0), n) * 0.3
    return norm(x)


def music_bed(rng, dur, root, sr=SR):
    n = int(dur * sr)
    return drone(rng, dur, root, 5, sr) * 0.3


def note(freq, dur, rng, kind="bell"):
    if kind == "bell":
        return bell(freq, dur, rng, (1.0, 2.0, 3.01, 4.2))
    n = int(dur * SR)
    x = sum(sine(freq * h, dur) / (h * h) for h in (1, 2, 3))
    return x * env_adsr(n, 0.02, 0.2, 0.4, dur * 0.5)


def r_music_undertext(i):
    rng = R("music_under", i)
    dur = 96.0
    bed = music_bed(rng, dur, 36.71)
    scale = [146.8, 164.8, 174.6, 220.0, 233.1, 293.7, 329.6, 349.2]
    parts = [(0, bed)]
    tpos = 4.0
    while tpos < dur - 8:
        f = scale[rng.integers(len(scale))]
        parts.append((tpos, note(f, 6.0, rng) * 0.35))
        if rng.random() < 0.3:
            parts.append((tpos + 0.5, note(f * 1.5, 5.0, rng) * 0.2))
        tpos += rng.uniform(2.5, 6.0)
    x = place(dur, parts)
    x = reverb(x, rng, 4.0, 0.55)[: int(dur * SR)]
    return fade(norm(x) * 0.7, 3.0, 6.0)


def r_music_rasure(i):
    rng = R("music_ras", i)
    bpm = 84
    beat = 60 / bpm
    bars = 20
    dur = bars * 4 * beat
    parts = [(0, music_bed(rng, dur, 36.71) * 1.2)]
    tom = lambda f: lowpass(noise(0.5, rng), 300) * env_exp(int(0.5 * SR), 0.08) + sine(f, 0.5) * env_exp(int(0.5 * SR), 0.12)
    for b in range(bars * 4):
        tt = b * beat
        if b % 4 in (0, 3) or (b % 8 == 6):
            parts.append((tt, tom(55 if b % 4 == 0 else 65) * 0.9))
        if b % 2 == 1:
            parts.append((tt, highpass(noise(0.05, rng), 5000) * env_exp(int(0.05 * SR), 0.01) * 0.2))
    riff = [73.4, 73.4, 77.8, 69.3, 73.4, 110.0, 103.8, 98.0]
    for bar in range(bars):
        f = riff[bar % len(riff)]
        d = 4 * beat
        s = saw(lambda ts: f * (1 + 0.003 * np.sin(ts * 30)), d) + saw(f * 1.5 * 1.004, d) * 0.6
        s = lowpass(s, 900 + 400 * (bar % 4)) * env_adsr(int(d * SR), 0.3, 0.3, 0.7, 0.6) * 0.35
        parts.append((bar * d, s))
        if bar >= 8:
            parts.append((bar * d, vocal(rng, lambda ts: f * 4, d, VOWEL_A, 0.1) * env_adsr(int(d * SR), 1.0, 0.5, 0.6, 1.0) * 0.15))
    x = place(dur, parts)
    x = reverb(x, rng, 2.5, 0.35)[: int(dur * SR)]
    fl = int(1.0 * SR)
    x[:fl] = x[:fl] * np.linspace(0, 1, fl) + x[-fl:] * np.linspace(1, 0, fl)
    return norm(x[:-fl]) * 0.85


def r_disc(i):
    """'Lower Writing': a music-box lullaby from the First Draft, scraped thin in places."""
    rng = R("disc", i)
    dur = 114.0
    melody = [(392, 1), (440, 1), (392, 1), (330, 2), (294, 1), (330, 1), (392, 2), (0, 1),
              (392, 1), (440, 1), (494, 1), (440, 2), (392, 1), (330, 1), (294, 3), (0, 1),
              (262, 1), (294, 1), (330, 1), (392, 2), (330, 1), (294, 1), (262, 3), (0, 1)]
    beat = 0.62
    parts, tt = [], 2.0
    while tt < dur - 6:
        for f, l in melody:
            if tt > dur - 6:
                break
            if f and rng.random() > 0.08:  # a few notes are simply missing
                box = bell(f * 2, 2.5, rng, (1.0, 3.0, 5.1)) * 0.5
                parts.append((tt, box))
            tt += l * beat
        tt += beat * 2
    x = place(dur, parts)
    wow = np.sin(2 * np.pi * 0.4 * t(dur)) * 0.002
    idx = np.clip(np.arange(len(x)) * (1 + wow), 0, len(x) - 1).astype(int)
    x = x[idx]
    hiss = highpass(noise(dur, rng), 4000) * 0.02
    # Scraped passages: the tune drops into near-silence and paper noise for a moment.
    for k in range(6):
        s = int(rng.uniform(10, dur - 10) * SR)
        e = s + int(rng.uniform(1.5, 4.0) * SR)
        x[s:e] *= np.linspace(1, 0.05, e - s) ** 0.3
        x[s:e] += page_flip(rng, (e - s) / SR)[: e - s] * 0.08
    x = reverb(x + hiss, rng, 2.0, 0.35)[: int(dur * SR)]
    x = x + fit(music_bed(rng, dur, 65.4), len(x)) * 0.4
    return fade(norm(x) * 0.8, 1.0, 5.0)


# ============================================================ catalogue
# event id -> (folder, [clip functions], extra sounds.json fields)
CATALOGUE = {
    "ambient.undertext.loop": ("ambient", [r_undertext_loop], {}),
    "ambient.undertext.additions": ("ambient", [r_additions] * 4, {}),
    "ambient.undertext.mood": ("ambient", [r_mood] * 2, {}),
    "music.undertext": ("music", [r_music_undertext], {"stream": True, "stereo": True}),
    "music.rasure": ("music", [r_music_rasure], {"stream": True, "stereo": True}),
    "music_disc.lower_writing": ("records", [r_disc], {"stream": True}),
    "event.knock": ("event", [r_knock] * 3, {}),
    "event.footsteps": ("event", [r_footsteps] * 3, {}),
    "event.whisper": ("event", [r_whisper] * 4, {}),
    "event.scream": ("event", [r_scream] * 2, {}),
    "event.stinger": ("event", [r_stinger] * 3, {}),
    "event.heartbeat": ("event", [r_heartbeat], {}),
    "event.tinnitus": ("event", [r_tinnitus], {}),
    "event.breath": ("event", [r_breath] * 2, {}),
    "event.door_creak": ("event", [r_door_creak] * 2, {}),
    "event.page_turn_distant": ("event", [r_page_turn_distant] * 2, {}),
    "event.scrape_distant": ("event", [r_scrape_distant] * 2, {}),
    "event.silence_break": ("event", [r_silence_break], {}),
    "item.rasorium.scrape": ("item", [r_rasorium] * 2, {}),
    "item.page_turn": ("item", [r_page_turn] * 3, {}),
    "item.quill_scratch": ("item", [r_quill] * 3, {}),
    "item.bookmark.use": ("item", [r_bookmark], {}),
    "item.lens.focus": ("item", [r_lens], {}),
    "item.censer.swing": ("item", [r_censer] * 2, {}),
    "block.tear.ambient": ("block", [r_tear_ambient] * 2, {}),
    "block.tear.seal": ("block", [r_tear_seal], {}),
    "block.veil.ambient": ("block", [r_veil_ambient] * 2, {}),
    "block.veil.travel": ("block", [r_veil_travel], {}),
    "block.gate.open": ("block", [r_gate_open], {}),
    "block.ritual.begin": ("block", [r_ritual_begin], {}),
    "block.ritual.complete": ("block", [r_ritual_complete], {}),
    "block.ritual.fail": ("block", [r_ritual_fail], {}),
    "block.ward.hum": ("block", [r_ward_hum], {}),
    "block.blank.erase": ("block", [r_blank_erase] * 2, {}),
    "block.sealed_door.open": ("block", [r_sealed_door], {}),
    "entity.blotling.squish": ("entity", [r_blot("squish")] * 2, {}),
    "entity.blotling.hurt": ("entity", [r_blot("hurt")], {}),
    "entity.blotling.death": ("entity", [r_blot("death")], {}),
    "entity.smudge.ambient": ("entity", [r_smudge("ambient")] * 3, {}),
    "entity.smudge.hurt": ("entity", [r_smudge("hurt")], {}),
    "entity.smudge.death": ("entity", [r_smudge("death")], {}),
    "entity.margin_crawler.ambient": ("entity", [r_crawler("ambient")] * 2, {}),
    "entity.margin_crawler.hurt": ("entity", [r_crawler("hurt")], {}),
    "entity.margin_crawler.death": ("entity", [r_crawler("death")], {}),
    "entity.quillcrow.ambient": ("entity", [r_crow("ambient")] * 3, {}),
    "entity.quillcrow.hurt": ("entity", [r_crow("hurt")], {}),
    "entity.quillcrow.death": ("entity", [r_crow("death")], {}),
    "entity.quillcrow.flap": ("entity", [r_crow("flap")] * 2, {}),
    "entity.pale_stag.ambient": ("entity", [r_stag("ambient")] * 2, {}),
    "entity.pale_stag.hurt": ("entity", [r_stag("hurt")], {}),
    "entity.pale_stag.death": ("entity", [r_stag("death")], {}),
    "entity.inkhound.ambient": ("entity", [r_hound("ambient")] * 2, {}),
    "entity.inkhound.howl": ("entity", [r_hound("howl")] * 2, {}),
    "entity.inkhound.hurt": ("entity", [r_hound("hurt")], {}),
    "entity.inkhound.death": ("entity", [r_hound("death")], {}),
    "entity.foxing_moth.ambient": ("entity", [r_moth] * 2, {}),
    "entity.rubricator.ambient": ("entity", [r_rubricator("ambient")] * 3, {}),
    "entity.rubricator.yes": ("entity", [r_rubricator("yes")], {}),
    "entity.rubricator.no": ("entity", [r_rubricator("no")], {}),
    "entity.rubricator.hurt": ("entity", [r_rubricator("hurt")], {}),
    "entity.rubricator.death": ("entity", [r_rubricator("death")], {}),
    "entity.knocker.ambient": ("entity", [r_knocker("ambient")], {}),
    "entity.knocker.lunge": ("entity", [r_knocker("lunge")] * 2, {}),
    "entity.knocker.hurt": ("entity", [r_knocker("hurt")], {}),
    "entity.knocker.death": ("entity", [r_knocker("death")], {}),
    "entity.copyist.reveal": ("entity", [r_copyist("reveal")], {}),
    "entity.copyist.ambient": ("entity", [r_copyist("ambient")] * 2, {}),
    "entity.copyist.hurt": ("entity", [r_copyist("hurt")], {}),
    "entity.copyist.death": ("entity", [r_copyist("death")], {}),
    "entity.longhand.ambient": ("entity", [r_longhand("ambient")] * 2, {}),
    "entity.longhand.move": ("entity", [r_longhand("move")] * 2, {}),
    "entity.longhand.hurt": ("entity", [r_longhand("hurt")], {}),
    "entity.longhand.death": ("entity", [r_longhand("death")], {}),
    "entity.redacted.ambient": ("entity", [r_redacted("ambient")] * 2, {}),
    "entity.redacted.attack": ("entity", [r_redacted("attack")] * 2, {}),
    "entity.redacted.hurt": ("entity", [r_redacted("hurt")], {}),
    "entity.redacted.death": ("entity", [r_redacted("death")], {}),
    "entity.erratum.ambient": ("entity", [r_erratum("ambient")], {}),
    "entity.erratum.reveal": ("entity", [r_erratum("reveal")], {}),
    "entity.palehand.ambient": ("entity", [r_palehand], {}),
    "entity.fair_copy.vanish": ("entity", [r_fair_copy], {}),
    "entity.bookbinder.ambient": ("entity", [r_bookbinder("ambient")] * 2, {}),
    "entity.bookbinder.needle": ("entity", [r_bookbinder("needle")], {}),
    "entity.bookbinder.rebind": ("entity", [r_bookbinder("rebind")], {}),
    "entity.bookbinder.hurt": ("entity", [r_bookbinder("hurt")], {}),
    "entity.bookbinder.death": ("entity", [r_bookbinder("death")], {}),
    "entity.rasure.ambient": ("entity", [r_rasure("ambient")], {}),
    "entity.rasure.scrape": ("entity", [r_rasure("scrape")] * 2, {}),
    "entity.rasure.phase": ("entity", [r_rasure("phase")], {}),
    "entity.rasure.hurt": ("entity", [r_rasure("hurt")] * 2, {}),
    "entity.rasure.death": ("entity", [r_rasure("death")], {}),
}


def main():
    manifest = {}
    total = 0
    for event, (folder, fns, extra) in CATALOGUE.items():
        entries = []
        stereo = extra.get("stereo", False)
        for i, fn in enumerate(fns):
            base = event.replace(".", "_")
            name = f"{folder}/{base}_{i}" if len(fns) > 1 else f"{folder}/{base}"
            clip = fn(i)
            save(os.path.join(SND, name + ".ogg"), clip, stereo=stereo)
            total += 1
            entry = {"name": f"palimpsest:{name}"}
            if extra.get("stream"):
                entry["stream"] = True
            entries.append(entry)
        manifest[event] = {"sounds": entries, "subtitle": f"subtitles.palimpsest.{event}"}
        if event.startswith("music"):
            manifest[event].pop("subtitle")
            manifest[event]["subtitle"] = f"subtitles.palimpsest.{event}"
    with open(os.path.join(os.path.dirname(SND), "sounds.json"), "w") as fh:
        json.dump(manifest, fh, indent=2)
    print(f"{total} sound files, {len(manifest)} events")


if __name__ == "__main__":
    main()
