"""Tiny synthesis toolkit (numpy/scipy) for Palimpsest's original sounds."""
import numpy as np
from scipy import signal

SR = 32000


def t(dur, sr=SR):
    return np.arange(int(dur * sr)) / sr


def noise(dur, rng, sr=SR):
    return rng.uniform(-1, 1, int(dur * sr))


def sine(freq, dur, sr=SR, phase=0.0):
    ts = t(dur, sr)
    if callable(freq):
        f = freq(ts)
        return np.sin(2 * np.pi * np.cumsum(f) / sr + phase)
    return np.sin(2 * np.pi * freq * ts + phase)


def saw(freq, dur, sr=SR):
    ts = t(dur, sr)
    f = freq(ts) if callable(freq) else np.full_like(ts, freq)
    ph = np.cumsum(f) / sr
    return 2 * (ph - np.floor(ph + 0.5))


def env_adsr(n, a, d, s, r, sr=SR):
    a, d, r = int(a * sr), int(d * sr), int(r * sr)
    sus = max(0, n - a - d - r)
    e = np.concatenate([np.linspace(0, 1, max(a, 1)), np.linspace(1, s, max(d, 1)), np.full(sus, s), np.linspace(s, 0, max(r, 1))])
    return np.pad(e, (0, max(0, n - len(e))))[:n]


def env_exp(n, decay, sr=SR):
    return np.exp(-np.arange(n) / (decay * sr))


def fade(x, fin=0.01, fout=0.05, sr=SR):
    x = x.copy()
    a, b = int(fin * sr), int(fout * sr)
    if a:
        x[:a] *= np.linspace(0, 1, a)
    if b:
        x[-b:] *= np.linspace(1, 0, b)
    return x


def lowpass(x, cutoff, order=2, sr=SR):
    b, a = signal.butter(order, min(cutoff / (sr / 2), 0.99), "low")
    return signal.lfilter(b, a, x)


def highpass(x, cutoff, order=2, sr=SR):
    b, a = signal.butter(order, min(cutoff / (sr / 2), 0.99), "high")
    return signal.lfilter(b, a, x)


def bandpass(x, lo, hi, order=2, sr=SR):
    b, a = signal.butter(order, [max(lo / (sr / 2), 0.001), min(hi / (sr / 2), 0.99)], "band")
    return signal.lfilter(b, a, x)


def resonator(x, freq, q=20, sr=SR):
    w0 = freq / (sr / 2)
    b, a = signal.iirpeak(min(w0, 0.99), q)
    return signal.lfilter(b, a, x)


def formant(x, formants, sr=SR):
    out = np.zeros_like(x)
    for f, gain, q in formants:
        out += resonator(x, f, q, sr) * gain
    return out


def reverb(x, rng, seconds=1.5, wet=0.35, damp=3000, sr=SR):
    n = int(seconds * sr)
    ir = rng.uniform(-1, 1, n) * np.exp(-np.arange(n) / (seconds * sr / 5))
    ir = lowpass(ir, damp, sr=sr)
    ir /= np.sqrt(np.sum(ir ** 2)) + 1e-9
    dry = np.pad(x, (0, n))
    tail = signal.fftconvolve(x, ir)[: len(dry)]
    tail = np.pad(tail, (0, len(dry) - len(tail)))
    return dry * (1 - wet) + tail * wet * 1.5


def distance(x, rng, amount=1.0, sr=SR):
    """Push a sound far away: less high end, more room."""
    y = lowpass(x, 5000 - 3800 * amount, sr=sr)
    return reverb(y, rng, 1.0 + amount * 1.8, 0.35 + 0.35 * amount, sr=sr)


def place(total, parts, sr=SR):
    """Mix (offset_seconds, clip) pairs into one buffer."""
    out = np.zeros(int(total * sr))
    for off, clip in parts:
        i = int(off * sr)
        j = min(len(out), i + len(clip))
        if i < len(out):
            out[i:j] += clip[: j - i]
    return out


def norm(x, peak=0.9):
    m = np.max(np.abs(x)) + 1e-9
    return x / m * peak


def pitch(x, factor):
    """Resample to shift pitch (and duration) by factor."""
    n = int(len(x) / factor)
    return signal.resample(x, max(n, 1))


def am(x, rate, depth, sr=SR, phase=0.0):
    ts = np.arange(len(x)) / sr
    return x * (1 - depth + depth * (0.5 + 0.5 * np.sin(2 * np.pi * rate * ts + phase)))


def bitcrush(x, bits=5, down=6):
    y = np.round(x * (2 ** bits)) / (2 ** bits)
    return np.repeat(y[::down], down)[: len(x)]


def fit(x, n):
    """Pad with silence or truncate to exactly n samples."""
    if len(x) >= n:
        return x[:n]
    return np.pad(x, (0, n - len(x)))
