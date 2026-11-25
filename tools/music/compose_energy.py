#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""
Catalogue musical CODJIA v2 : morceaux ORIGINAUX, énergiques, entièrement synthétisés ici
(aucun échantillon ni boucle tierce : même licence que le projet, GPL-3.0-or-later).

Pensé pour les vidéos courtes :
- 64 s par morceau, plus long que la plus longue vidéo (59 s) : la musique ne boucle jamais,
  donc elle ne s'arrête et ne redémarre jamais au milieu d'une vidéo ;
- énergie dès le début : 2 mesures d'intro filtrée avec montée, puis le « drop » vers 4 s ;
- aucun silence : les respirations gardent la basse et les nappes, seule la batterie s'allège ;
- basses saturées (harmoniques) pour qu'on les entende aussi sur un haut-parleur de téléphone.

Usage : python3 tools/music/compose_energy.py <dossier_de_sortie>
"""
import json
import sys
from pathlib import Path

import numpy as np

from compose import SR, midi, env, decay, lowpass, highpass, bandpass, saw, reverb

LENGTH_S = 64.0
N = int(SR * LENGTH_S)
RNG = np.random.default_rng(20261125)


# ─────────────────────────── outils ───────────────────────────

def add(tr, sig, t, pan=0.0, gain=1.0):
    i = int(t * SR)
    if i >= N or i < 0:
        return
    sig = sig[: N - i] * gain
    l, r = np.cos((pan + 1) * np.pi / 4), np.sin((pan + 1) * np.pi / 4)
    tr[0, i:i + len(sig)] += sig * l
    tr[1, i:i + len(sig)] += sig * r


def mixs(*sigs):
    """Somme de signaux de longueurs différentes."""
    n = max(len(x) for x in sigs)
    out = np.zeros(n)
    for x in sigs:
        out[:len(x)] += x
    return out


def sat(x, drive):
    return np.tanh(x * drive) / np.tanh(drive)


def sidechain(x, times, depth=0.7, release=0.2):
    g = np.ones(x.shape[-1])
    r = int(release * SR)
    curve = 1 - depth * (1 - np.linspace(0, 1, r)) ** 2
    for t in times:
        i = int(t * SR)
        if i >= len(g):
            continue
        seg = min(r, len(g) - i)
        g[i:i + seg] = np.minimum(g[i:i + seg], curve[:seg])
    return x * g


def delay_fx(x, time, fb=0.35, mix=0.3):
    """Écho stéréo en ping-pong."""
    d = int(time * SR)
    out = x.copy()
    l, r = np.zeros(x.shape[1]), np.zeros(x.shape[1])
    src = x.mean(axis=0)
    g = mix
    for k in range(1, 6):
        if d * k >= x.shape[1]:
            break
        tgt = l if k % 2 else r
        tgt[d * k:] += src[: x.shape[1] - d * k] * g
        g *= fb
    out[0] += lowpass(l, 6000)
    out[1] += lowpass(r, 6000)
    return out


def lp_sweep(x, f0, f1):
    """Filtre passe-bas qui s'ouvre progressivement (intro filtrée)."""
    n = x.shape[-1]
    out = np.zeros_like(x)
    blocks = 24
    for b in range(blocks):
        i0, i1 = b * n // blocks, (b + 1) * n // blocks
        f = f0 * (f1 / f0) ** (b / (blocks - 1))
        for ch in range(2):
            out[ch, i0:i1] = lowpass(x[ch, max(0, i0 - 2000):i1], f)[-(i1 - i0):]
    return out


# ─────────────────────────── batterie ───────────────────────────

def kick(vel=1.0, punch=1.0, tail=0.32):
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    f = 46 + 120 * np.exp(-t / 0.03) + 40 * np.exp(-t / 0.004)
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * decay(n, tail)
    click = highpass(RNG.standard_normal(n), 3000) * decay(n, 0.002) * 0.5 * punch
    return sat(s * 1.3 + click, 1.6) * 0.62 * vel


def snare(vel=1.0, body=190, tone=0.5):
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    b = np.sin(2 * np.pi * np.cumsum(body * (1 + 0.6 * np.exp(-t / 0.01))) / SR) * decay(n, 0.06) * tone
    nz = bandpass(RNG.standard_normal(n), 1500, 9000) * decay(n, 0.13)
    return sat(b + nz, 1.4) * 0.36 * vel


def clap(vel=1.0):
    n = int(0.4 * SR)
    out = np.zeros(n)
    for k, d in enumerate((0.0, 0.009, 0.019, 0.027)):
        i = int(d * SR)
        out[i:] += bandpass(RNG.standard_normal(n - i), 1000, 6000) * decay(n - i, 0.01 if k < 3 else 0.11)
    return out * 0.38 * vel


def hat(vel=1.0, tau=0.03, open_=False):
    n = int((0.35 if open_ else 0.12) * SR)
    s = highpass(RNG.standard_normal(n), 7500) * decay(n, 0.12 if open_ else tau)
    return s * 0.2 * vel


def rim(vel=1.0):
    n = int(0.08 * SR)
    t = np.arange(n) / SR
    return (np.sin(2 * np.pi * 1750 * t) * decay(n, 0.009) + bandpass(RNG.standard_normal(n), 2000, 6000) * decay(n, 0.006) * 0.5) * 0.24 * vel


def shaker(vel=1.0):
    n = int(0.09 * SR)
    s = bandpass(RNG.standard_normal(n), 5000, 12000) * np.sin(np.linspace(0, np.pi, n)) ** 2
    return s * 0.12 * vel


def perc(f, vel=1.0):
    """Percussion accordée (conga, tom)."""
    n = int(0.3 * SR)
    t = np.arange(n) / SR
    return np.sin(2 * np.pi * np.cumsum(f * (1 + 0.4 * np.exp(-t / 0.01))) / SR) * decay(n, 0.08) * 0.3 * vel


def cowbell(m, vel=1.0):
    """Cloche « phonk » : deux carrés désaccordés, filtrés, à la hauteur voulue."""
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = midi(m)
    s = np.sign(np.sin(2 * np.pi * f * t)) + np.sign(np.sin(2 * np.pi * f * 1.48 * t))
    return bandpass(s, 500, 5000) * decay(n, 0.12) * 0.17 * vel


def log_drum(m, vel=1.0):
    n = int(0.6 * SR)
    t = np.arange(n) / SR
    f = midi(m) * (1 + 0.9 * np.exp(-t / 0.02))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.35 * np.sin(4 * np.pi * np.cumsum(f) / SR) * decay(n, 0.06)
    return lowpass(sat(s, 2.4) * decay(n, 0.26), 2600) * 0.55 * vel


# ─────────────────────────── basses et synthés ───────────────────────────

def b808(m, dur, glide_from=None, vel=1.0, drive=2.2):
    """808 saturée : audible sur téléphone grâce aux harmoniques."""
    n = int((dur + 0.15) * SR)
    t = np.arange(n) / SR
    f1 = midi(m)
    f = np.full(n, f1)
    if glide_from is not None:
        f = midi(glide_from) + (f1 - midi(glide_from)) * np.minimum(1, t / 0.08)
    f = f * (1 + 0.6 * np.exp(-t / 0.012))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR)
    s = sat(s, drive) * env(n, 0.002, 0.15, 0.85, 0.1, dur)
    return s * 0.45 * vel


def bassline(m, dur, vel=1.0, cutoff=900, drive=1.8):
    """Basse synthé (scie filtrée + sinus), pour house, EDM, synthwave."""
    n = int((dur + 0.05) * SR)
    t = np.arange(n) / SR
    f = midi(m)
    s = 0.7 * np.sin(2 * np.pi * f * t) + 0.5 * lowpass(saw(f, n, 20), cutoff)
    return sat(s, drive) * env(n, 0.004, 0.08, 0.8, 0.04, dur) * 0.38 * vel


def supersaw(notes, dur, cutoff=4200, voices=6, detune=0.18, vel=1.0):
    n = int((dur + 0.3) * SR)
    out = np.zeros(n)
    for m in notes:
        for v in range(voices):
            d = (v - (voices - 1) / 2) * detune / ((voices - 1) / 2)
            out += saw(midi(m) * 2 ** (d / 12), n, 16)
    return lowpass(out, cutoff) * env(n, 0.008, 0.2, 0.8, 0.25, dur) / (len(notes) * voices) * 1.1 * vel


def stab(notes, dur=0.18, cutoff=3000, vel=1.0):
    n = int((dur + 0.2) * SR)
    out = np.zeros(n)
    for m in notes:
        for det in (-0.08, 0.0, 0.08):
            out += saw(midi(m) * 2 ** (det / 12), n, 16)
    return lowpass(out, cutoff) * env(n, 0.003, 0.08, 0.5, 0.12, dur) / (len(notes) * 3) * 1.2 * vel


def lead(m, dur, vel=1.0, cutoff=5000, square=False):
    n = int((dur + 0.15) * SR)
    t = np.arange(n) / SR
    f = midi(m) * (1 + 0.003 * np.sin(2 * np.pi * 5.5 * t) * np.minimum(1, t / 0.25))
    ph = 2 * np.pi * np.cumsum(f) / SR
    s = np.sign(np.sin(ph)) * 0.5 if square else sum(np.sin(ph * k) / k for k in range(1, 14))
    return lowpass(s, cutoff) * env(n, 0.005, 0.1, 0.7, 0.12, dur) * 0.2 * vel


def pluck(m, vel=1.0, bright=4500):
    n = int(0.6 * SR)
    t = np.arange(n) / SR
    f = midi(m)
    s = sum(np.sin(2 * np.pi * f * k * t) * decay(n, 0.25 / k ** 0.6) / k for k in range(1, 10))
    return lowpass(s, bright) * np.minimum(1, t / 0.002) * 0.22 * vel


def keys(m, dur, vel=1.0):
    n = int((dur + 0.8) * SR)
    t = np.arange(n) / SR
    f = midi(m)
    tone = np.sin(2 * np.pi * f * t) + 0.3 * np.sin(4 * np.pi * f * t) * decay(n, 0.5) + 0.1 * np.sin(2 * np.pi * 7 * f * t) * decay(n, 0.04)
    return tone * env(n, 0.004, 0.5, 0.5, 0.6, dur) * 0.2 * vel


def pad(notes, dur, bright=2200):
    n = int((dur + 1.0) * SR)
    out = np.zeros(n)
    for m in notes:
        for det in (-0.1, 0.0, 0.1):
            out += saw(midi(m) * 2 ** (det / 12), n, 12)
    return lowpass(out, bright) * env(n, 0.3, 0.4, 0.85, 0.8, dur) / (len(notes) * 3) * 0.8


def strings(notes, dur, cutoff=2600):
    n = int((dur + 1.0) * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for m in notes:
        ph = 2 * np.pi * np.cumsum(midi(m) * (1 + 0.004 * np.sin(2 * np.pi * 5.2 * t))) / SR
        out += sum(np.sin(ph * k) / k for k in range(1, 12))
    return lowpass(out, cutoff) * env(n, 0.25, 0.3, 0.85, 0.7, dur) / len(notes) * 0.3


def braam(m, dur):
    """« Braam » de bande-annonce : cuivres graves saturés."""
    n = int((dur + 0.8) * SR)
    s = sum(saw(midi(m + o) * 2 ** (d / 12), n, 20) for o in (0, 12, 7) for d in (-0.12, 0.12))
    return lowpass(sat(s, 2.5), 1400) * env(n, 0.05, 0.4, 0.7, 0.8, dur) * 0.16


# ─────────────────────────── effets ───────────────────────────

def riser(dur, vel=1.0):
    n = int(dur * SR)
    noise = RNG.standard_normal(n)
    out = np.zeros(n)
    for b in range(32):
        i0, i1 = b * n // 32, (b + 1) * n // 32
        f = 400 * (12000 / 400) ** (b / 32)
        out[i0:i1] = bandpass(noise[i0:i1], max(100, f * 0.5), min(f * 1.5, SR / 2.3))
    return out * np.linspace(0, 1, n) ** 2 * 0.22 * vel


def downlifter(dur=1.5, vel=1.0):
    return riser(dur, vel)[::-1] * 0.8


def impact(vel=1.0):
    n = int(1.6 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * np.cumsum(42 * (1 + 2.5 * np.exp(-t / 0.04))) / SR) * decay(n, 0.5)
    s += lowpass(RNG.standard_normal(n), 1200) * decay(n, 0.2) * 0.4
    return sat(s, 1.5) * 0.5 * vel


def snare_roll(tr, start, dur, vel=0.8):
    """Roulement qui accélère avant le drop."""
    t = 0.0
    k = 0
    while t < dur:
        step = dur / 4 if t < dur / 2 else (dur / 8 if t < dur * 0.8 else dur / 16)
        add(tr, snare(vel * (0.4 + 0.6 * t / dur)), start + t)
        t += step
        k += 1


# ─────────────────────────── mixage ───────────────────────────

def phone_eq(x):
    """Présence et air pour les haut-parleurs de téléphone, sans perdre les basses (déjà saturées)."""
    out = np.empty_like(x)
    for ch in range(2):
        sub = lowpass(x[ch], 70, 2)                    # sous-graves : inaudibles sur téléphone, ils mangent le niveau
        low = bandpass(x[ch], 90, 260, 2)              # harmoniques de basse : audibles sur téléphone
        mud = bandpass(x[ch], 280, 500, 2)
        mid = bandpass(x[ch], 700, 2000, 2)
        pres = bandpass(x[ch], 2000, 6000, 2)
        air = highpass(x[ch], 8500, 2)
        out[ch] = x[ch] - 0.72 * sub + 0.5 * low - 0.3 * mud + 0.9 * mid + 1.6 * pres + 1.6 * air
    return out


def finish(parts, target_db=-11.5):
    """Somme des bus, égalisation, compression douce, limiteur, crête −1 dBFS, fondu final court."""
    x = sum(parts)
    x = phone_eq(np.stack([highpass(x[0], 30), highpass(x[1], 30)]))
    mid, side = (x[0] + x[1]) / 2, (x[0] - x[1]) / 2
    for _ in range(12):
        if np.corrcoef(mid + side, mid - side)[0, 1] >= 0.45:
            break
        side *= 0.85
    x = np.stack([mid + side, mid - side])
    x *= 10 ** (target_db / 20) / (np.sqrt(np.mean(x ** 2)) + 1e-9)
    x = np.tanh(x * 1.35) / 1.35
    x *= 10 ** (-1 / 20) / max(np.max(np.abs(x)), 1e-9)
    fi, fo = int(0.01 * SR), int(2.5 * SR)
    x[:, :fi] *= np.linspace(0, 1, fi)
    x[:, -fo:] *= np.linspace(1, 0, fo) ** 2
    return x


class Song:
    """Squelette commun : grille de temps, sections (intro, drop, respirations) et bus de mixage."""

    def __init__(self, bpm, prog, intro_bars=2):
        self.bpm = bpm
        self.beat = 60 / bpm
        self.bar = 4 * self.beat
        self.step = self.beat / 4
        self.prog = prog
        self.bars = int(LENGTH_S / self.bar) + 1
        self.intro = intro_bars
        self.drums = np.zeros((2, N))
        self.bass = np.zeros((2, N))
        self.music = np.zeros((2, N))
        self.fx = np.zeros((2, N))
        self.kicks = []

    def t(self, bar, step=0):
        return bar * self.bar + step * self.step

    def chord(self, bar):
        return self.prog[bar % len(self.prog)]

    def section(self, bar):
        """intro | drop | lift (respiration : batterie allégée, montée vers le drop suivant)."""
        if bar < self.intro:
            return "intro"
        k = (bar - self.intro) % 8
        return "lift" if k == 7 else "drop"

    def pattern(self, bar, pat, fn, pan=0.0, swing=0.0, bus=None):
        for i, c in enumerate(pat):
            if c == ".":
                continue
            v = {"x": 1.0, "X": 1.2, "o": 0.55, "g": 0.3}.get(c, 1.0)
            sw = swing * self.step if i % 2 == 1 else 0
            add(bus if bus is not None else self.drums, fn(v), self.t(bar, i) + sw, pan)

    def kick_at(self, bar, pat, k=kick):
        for i, c in enumerate(pat):
            if c != ".":
                add(self.drums, k(1.0 if c == "x" else 0.6), self.t(bar, i))
                self.kicks.append(self.t(bar, i))

    def transitions(self):
        """Montée + roulement avant chaque drop, impact au drop."""
        for bar in range(self.bars):
            if self.section(bar) == "drop" and (bar == self.intro or self.section(bar - 1) == "lift"):
                start = self.t(bar)
                rd = self.bar * (2 if bar == self.intro else 1)
                add(self.fx, riser(rd), start - rd, 0.0, 0.9)
                add(self.fx, impact(), start)
                snare_roll(self.fx, start - self.bar / 2, self.bar / 2, 0.55)

    def mix(self, chords_sc=0.65, target=-11.5, verb=0.18, intro_filter=True):
        music = sidechain(self.music, self.kicks, chords_sc, 0.18)
        bass = sidechain(self.bass, self.kicks, 0.45, 0.12)
        music = reverb(music, 1.6, verb, 6500)
        x = [self.drums * 1.1, bass * 0.8, music * 1.9, reverb(self.fx, 2.2, 0.3)]
        full = sum(x)
        if intro_filter:
            i1 = int(self.t(self.intro) * SR)
            full[:, :i1] = lp_sweep(full[:, :i1], 500, 9000)
        return finish([full], target)


# ─────────────────────────── morceaux ───────────────────────────

def track_afrobeat():
    """Afro Vibes, 106 BPM : grosse caisse syncopée, rimshots, shakers, guitare et basse ronde."""
    s = Song(106, [[50, 62, 65, 69], [55, 62, 67, 71], [52, 64, 67, 71], [57, 61, 64, 69]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x..x..x...x.x..." if sec != "intro" else "x.......x.......")
        s.pattern(bar, "....x.......x...", lambda v: clap(v * 0.8))
        s.pattern(bar, "..x..x...x..x..x", rim, pan=-0.3)
        s.pattern(bar, "xoxoxoxoxoxoxoxo", shaker, pan=0.4, swing=0.12)
        if sec == "drop":
            s.pattern(bar, "..o...o...o...o.", lambda v: perc(220, v), pan=0.5)
            s.pattern(bar, "o.....o.....o...", lambda v: perc(330, v), pan=-0.5)
        for i, d in ((0, 3), (3, 3), (6, 2), (10, 2), (12, 4)):
            add(s.bass, b808(root - 12, d * s.step, vel=0.9, drive=1.6), s.t(bar, i))
        for i in (2, 6, 10, 14):
            for m in s.chord(bar)[1:]:
                add(s.music, pluck(m + 12, 0.5), s.t(bar, i) + 0.01, 0.25)
        if sec == "drop":
            mel = [74, 72, 69, 72, 74, 76, 74, 72]
            for k in range(4):
                add(s.music, lead(mel[(bar * 2 + k) % 8], s.step * 3, 0.55), s.t(bar, 4 * k), -0.2)
        add(s.music, pad(s.chord(bar)[1:], s.bar), s.t(bar), 0.0, 0.5)
    s.transitions()
    return s.mix(0.4, -11.5), s.bpm


def track_amapiano():
    """Amapiano Groove, 113 BPM : log drum, shakers, piano jazzy, nappe chaude."""
    s = Song(113, [[53, 60, 64, 69], [50, 57, 60, 65], [55, 62, 65, 70], [48, 55, 60, 64]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x...x...x...x...")
        s.pattern(bar, "....x.......x...", lambda v: clap(v * 0.7))
        s.pattern(bar, "xoxoxoxoxoxoxoxo", shaker, pan=0.35, swing=0.1)
        s.pattern(bar, "..x...x...x...x.", lambda v: hat(v, open_=True), pan=-0.2)
        if sec != "intro":
            for i, m in ((0, 0), (3, 0), (6, 7), (10, 12), (13, 10)):
                add(s.bass, log_drum(root - 12 + m, 0.95), s.t(bar, i))
        for i in (0, 3, 7, 10):
            add(s.music, sum(keys(m + 12, s.step * 2, 0.6) for m in s.chord(bar)[1:]), s.t(bar, i), 0.2)
        add(s.music, pad(s.chord(bar), s.bar, 1800), s.t(bar), 0.0, 0.6)
    s.transitions()
    return s.mix(0.35, -11.5), s.bpm


def track_trap():
    """Trap Bass, 140 BPM (demi-tempo) : 808 qui glisse, charleston en rafales, cloche sombre."""
    s = Song(140, [[45, 57, 60, 64], [41, 57, 60, 65], [43, 55, 59, 62], [40, 55, 59, 64]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x......x..x....." if sec != "intro" else "x...............")
        s.pattern(bar, "........x.......", lambda v: mixs(clap(v), snare(v * 0.6)))
        hats = "x.x.x.x.xxx.x.x." if bar % 2 else "x.x.x.x.x.x.xxxx"
        s.pattern(bar, hats, lambda v: hat(v * 0.8, 0.02), pan=0.3)
        if sec != "intro":
            add(s.bass, b808(root - 12, s.beat * 1.6, glide_from=root - 7 if bar % 2 else None), s.t(bar, 0))
            add(s.bass, b808(root - 12 + (3 if bar % 4 == 3 else 0), s.beat * 1.2), s.t(bar, 10))
        mel = [76, 72, 69, 71, 72, 69, 67, 64]
        for k in range(8):
            add(s.music, pluck(mel[(k + bar) % 8], 0.7, 3500), s.t(bar, 2 * k), 0.15)
        add(s.music, pad(s.chord(bar)[1:], s.bar, 1500), s.t(bar), 0.0, 0.7)
    s.music = delay_fx(s.music, s.beat * 0.75, 0.35, 0.3)
    s.transitions()
    return s.mix(0.5, -11.0), s.bpm


def track_drill():
    """Drill Night, 142 BPM : 808 glissées, charleston en triolets, cordes sombres."""
    s = Song(142, [[42, 54, 57, 61], [42, 54, 57, 61], [45, 57, 61, 64], [40, 52, 55, 59]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x.....x...x....." if sec != "intro" else "x...............")
        s.pattern(bar, "....x.......x..o", lambda v: mixs(snare(v, 220, 0.4), clap(v * 0.5)))
        for k in range(12):  # triolets
            add(s.drums, hat(0.6 if k % 3 else 0.9, 0.018), bar * s.bar + k * s.bar / 12, 0.3)
        if sec != "intro":
            add(s.bass, b808(root - 12, s.beat * 1.4, glide_from=root - 5, drive=3.0), s.t(bar, 0))
            add(s.bass, b808(root - 9, s.beat * 0.8, glide_from=root - 12, drive=3.0), s.t(bar, 6))
            add(s.bass, b808(root - 12, s.beat * 1.2, drive=3.0), s.t(bar, 10))
        add(s.music, strings(s.chord(bar)[1:], s.bar, 2200), s.t(bar), -0.1, 0.9)
        for k, m in enumerate([73, 69, 66, 69]):
            add(s.music, pluck(m, 0.55, 3000), s.t(bar, 4 * k + 2), 0.3)
    s.transitions()
    return s.mix(0.45, -11.0), s.bpm


def track_phonk():
    """Phonk Drift, 130 BPM : cloche mélodique, 808 très saturée, ambiance de course de nuit."""
    s = Song(130, [[41, 53, 56, 60], [44, 56, 60, 63], [39, 51, 55, 58], [41, 53, 56, 60]])
    bell = [77, 77, 80, 77, 75, 72, 75, 77, 77, 80, 82, 80, 77, 75, 72, 70]
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x.....x.x.....x." if sec != "intro" else "x.......x.......")
        s.pattern(bar, "....x.......x...", lambda v: clap(v * 1.1))
        s.pattern(bar, "x.xxx.x.x.xxx.x.", lambda v: hat(v * 0.7, 0.015), pan=0.3)
        if sec != "intro":
            add(s.bass, b808(root - 12, s.beat * 1.5, drive=4.0), s.t(bar, 0))
            add(s.bass, b808(root - 12, s.beat * 0.9, glide_from=root, drive=4.0), s.t(bar, 8))
            add(s.bass, b808(root - 9, s.beat * 0.6, drive=4.0), s.t(bar, 14))
        for k in range(16):
            if k % 2 == 0 or sec == "drop":
                add(s.music, cowbell(bell[k] - (0 if bar % 2 == 0 else 2), 0.8 if k % 2 == 0 else 0.5), s.t(bar, k), -0.15)
        add(s.music, pad(s.chord(bar)[1:], s.bar, 1200), s.t(bar), 0.0, 0.5)
    s.transitions()
    return s.mix(0.4, -10.5), s.bpm


def track_house():
    """Deep House Pulse, 124 BPM : 4 temps, basse en contretemps, accords « stab », charleston ouverte."""
    s = Song(124, [[45, 60, 64, 67, 71], [43, 59, 62, 66, 69], [41, 57, 60, 64, 67], [43, 59, 62, 65, 69]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x...x...x...x..." if sec != "lift" else "x...x...x.......")
        s.pattern(bar, "....x.......x...", lambda v: clap(v * 0.8))
        s.pattern(bar, "..x...x...x...x.", lambda v: hat(v, open_=True), pan=0.25)
        s.pattern(bar, "x.xxx.xxx.xxx.xx", lambda v: hat(v * 0.5, 0.02), pan=-0.3)
        for i in (2, 6, 10, 14):
            add(s.bass, bassline(root - 12 + (12 if i == 14 and bar % 2 else 0), s.step * 1.7, 1.0, 700), s.t(bar, i))
        for i in (3, 6, 11) if sec == "drop" else (3,):
            add(s.music, stab(s.chord(bar)[1:], 0.16, 2600), s.t(bar, i), 0.1)
        add(s.music, pad(s.chord(bar)[1:], s.bar, 1600), s.t(bar), 0.0, 0.6)
    s.music = delay_fx(s.music, s.beat * 0.75, 0.3, 0.25)
    s.transitions()
    return s.mix(0.6, -11.0), s.bpm


def track_edm():
    """Big Drop, 128 BPM : accords « supersaw » qui pompent, basse octave, mélodie en drop."""
    s = Song(128, [[45, 57, 60, 64], [41, 57, 60, 65], [48, 55, 60, 64], [43, 55, 59, 62]])
    mel = [76, 76, 79, 76, 74, 72, 74, 76, 72, 72, 74, 72, 71, 67, 69, 71]
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x...x...x...x...")
        s.pattern(bar, "....x.......x...", lambda v: mixs(clap(v), snare(v * 0.5)))
        s.pattern(bar, "..x...x...x...x.", lambda v: hat(v * 0.9, open_=True), pan=0.2)
        for i in range(0, 16, 2):
            add(s.bass, bassline(root - 12 + (12 if i % 4 == 2 else 0), s.step * 1.6, 0.9, 1400), s.t(bar, i))
        add(s.music, supersaw(s.chord(bar)[1:], s.bar * 0.98, 5200 if sec == "drop" else 2500, vel=0.9), s.t(bar), 0.0)
        if sec == "drop":
            for k in range(8):
                add(s.music, lead(mel[(k + 8 * (bar % 2)) % 16], s.step * 1.8, 0.75, 7000), s.t(bar, 2 * k), 0.15)
    s.transitions()
    return s.mix(0.75, -10.5), s.bpm


def track_future():
    """Future Bass, 150 BPM (demi-tempo) : accords larges hachés, voix synthétiques, grosses basses."""
    s = Song(150, [[46, 58, 62, 65, 69], [43, 58, 62, 65, 67], [39, 55, 58, 62, 65], [41, 57, 60, 63, 65]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x.........x.....")
        s.pattern(bar, "........x.......", lambda v: mixs(clap(v), snare(v * 0.7)))
        s.pattern(bar, "x.x.x.x.x.x.x.xx", lambda v: hat(v * 0.7, 0.02), pan=0.3)
        add(s.bass, b808(root - 12, s.bar * 0.9, drive=2.4), s.t(bar))
        rh = (0, 3, 6, 8, 11, 14) if sec == "drop" else (0, 8)
        for i in rh:
            add(s.music, supersaw(s.chord(bar)[1:], s.step * 2, 6000, 5, 0.25, 0.85), s.t(bar, i), 0.0)
        for k, m in enumerate([77, 74, 72, 74]):
            add(s.music, pluck(m, 0.6, 6000), s.t(bar, 4 * k + 2), -0.25)
    s.music = delay_fx(s.music, s.beat * 0.5, 0.3, 0.2)
    s.transitions()
    return s.mix(0.7, -11.0), s.bpm


def track_synthwave():
    """Outrun, 100 BPM : arpège 16e, caisse claire « gated », basse en croches, nappe rétro."""
    s = Song(100, [[45, 57, 60, 64], [41, 57, 60, 65], [43, 55, 59, 62], [40, 55, 59, 64]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x...x...x...x...")
        s.pattern(bar, "....x.......x...", lambda v: snare(v * 1.1, 170, 0.6))
        s.pattern(bar, "x.x.x.x.x.x.x.x.", lambda v: hat(v * 0.7, 0.025), pan=0.2)
        for i in range(0, 16, 2):
            add(s.bass, bassline(root - 12 + (12 if i % 4 == 2 else 0), s.step * 1.8, 0.9, 1100), s.t(bar, i))
        arp = s.chord(bar)[1:] + [s.chord(bar)[1] + 12]
        for i in range(16):
            add(s.music, pluck(arp[i % len(arp)] + 12, 0.45, 5000), s.t(bar, i), 0.3 if i % 2 else -0.3)
        add(s.music, pad(s.chord(bar)[1:], s.bar, 2400), s.t(bar), 0.0, 0.8)
        if sec == "drop" and bar % 2 == 0:
            add(s.music, lead(s.chord(bar)[-1] + 12, s.bar * 0.9, 0.5, 4000, square=True), s.t(bar), -0.1)
    s.music = delay_fx(s.music, s.beat * 0.75, 0.35, 0.25)
    s.transitions()
    return s.mix(0.45, -11.5, 0.25), s.bpm


def track_hiphop():
    """Boom Bap Study, 92 BPM : batterie qui claque, basse ronde, piano électrique, scratch léger."""
    s = Song(92, [[50, 60, 65, 69], [55, 59, 65, 69], [48, 59, 64, 67], [45, 60, 64, 67]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x......x.xx....." if bar % 2 else "x.......x.x.....")
        s.pattern(bar, "....X.......X...", lambda v: snare(v, 200, 0.7))
        s.pattern(bar, "x.x.x.x.x.x.x.x.", lambda v: hat(v * 0.7, 0.03), pan=0.25, swing=0.18)
        for i, d in ((0, 6), (7, 2), (10, 4)):
            add(s.bass, bassline(root - 12, d * s.step, 1.0, 500, 1.3), s.t(bar, i))
        for i in (0, 6, 10) if sec == "drop" else (0,):
            add(s.music, sum(keys(m + 12, s.step * 4, 0.7) for m in s.chord(bar)[1:]), s.t(bar, i), 0.15)
    s.transitions()
    return s.mix(0.3, -11.5, 0.15), s.bpm


def track_lofi():
    """Lo-fi Beats, 86 BPM : posé mais avec du groove (basse, batterie feutrée) pour apprendre."""
    s = Song(86, [[50, 60, 65, 69, 72], [43, 59, 62, 65, 69], [48, 59, 64, 67, 71], [45, 59, 61, 64, 67]])
    for bar in range(s.bars):
        root = s.chord(bar)[0]
        s.kick_at(bar, "x.......x.x.....", k=lambda v: kick(v * 0.85, 0.4, 0.25))
        s.pattern(bar, "....x.......x...", lambda v: snare(v * 0.8, 180, 0.5))
        s.pattern(bar, "x.x.x.x.x.x.x.x.", lambda v: hat(v * 0.55, 0.03), pan=0.25, swing=0.2)
        add(s.bass, bassline(root - 12, s.beat * 1.8, 0.9, 400, 1.2), s.t(bar, 0))
        add(s.bass, bassline(root - 12 + 7, s.beat * 1.5, 0.8, 400, 1.2), s.t(bar, 10))
        add(s.music, sum(keys(m + 12, s.bar * 0.9, 0.55) for m in s.chord(bar)[1:]), s.t(bar), 0.1)
        for k, m in enumerate([81, 79, 76, 79]):
            add(s.music, pluck(m, 0.35, 3000), s.t(bar, 4 * k + 3), -0.3)
    s.transitions()
    return s.mix(0.25, -12.5, 0.25), s.bpm


def track_cinematic():
    """Epic Trailer, 92 BPM : taikos, cordes en ostinato, « braams », impacts."""
    s = Song(92, [[38, 50, 53, 57], [34, 50, 53, 58], [36, 48, 52, 55], [33, 49, 52, 57]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x..x..x...x.x..." if sec != "intro" else "x.......x.......", k=lambda v: kick(v * 1.1, 0.6, 0.5))
        s.pattern(bar, "....x.......x...", lambda v: snare(v * 1.2, 160, 0.8))
        s.pattern(bar, "x.xxx.xxx.xxx.xx" if sec == "drop" else "x...x...x...x...", lambda v: perc(110, v), pan=0.4)
        s.pattern(bar, "..x...x...x...x.", lambda v: perc(75, v * 1.2), pan=-0.4)
        for i in range(16):
            add(s.music, pluck(s.chord(bar)[1 + (i % 3)] + 12, 0.45, 3500), s.t(bar, i), 0.2 if i % 2 else -0.2)
        add(s.music, strings(s.chord(bar)[1:], s.bar, 3000), s.t(bar), 0.0, 1.1)
        add(s.bass, b808(root, s.bar * 0.95, drive=1.6, vel=0.9), s.t(bar))
        if sec == "drop" and bar % 2 == 0:
            add(s.music, braam(root, s.bar * 0.9), s.t(bar), 0.0, 0.9)
    s.transitions()
    return s.mix(0.3, -11.5, 0.3), s.bpm


def track_dembow():
    """Tropical Dembow, 96 BPM : rythme dembow, basse ronde, plucks et accords lumineux."""
    s = Song(96, [[48, 60, 64, 67], [43, 59, 62, 67], [45, 60, 64, 69], [41, 60, 65, 69]])
    for bar in range(s.bars):
        sec = s.section(bar)
        root = s.chord(bar)[0]
        s.kick_at(bar, "x...x...x...x...")
        s.pattern(bar, "...x..x....x..x.", lambda v: mixs(snare(v * 0.9, 240, 0.5), rim(v * 0.6)))
        s.pattern(bar, "x.x.x.x.x.x.x.x.", lambda v: hat(v * 0.6, 0.02), pan=0.3)
        if sec != "intro":
            for i in (0, 3, 6, 8, 11, 14):
                add(s.bass, b808(root - 12 + (7 if i in (6, 14) else 0), s.step * 2, vel=0.85, drive=1.8), s.t(bar, i))
        for i in (2, 5, 10, 13):
            add(s.music, sum(pluck(m + 12, 0.45, 5000) for m in s.chord(bar)[1:]), s.t(bar, i), 0.2)
        if sec == "drop":
            for k, m in enumerate([79, 76, 74, 76, 79, 81, 79, 76]):
                add(s.music, lead(m, s.step * 1.6, 0.5, 6000), s.t(bar, 2 * k), -0.2)
    s.music = delay_fx(s.music, s.beat * 0.75, 0.3, 0.2)
    s.transitions()
    return s.mix(0.4, -11.0), s.bpm


TRACKS = [
    ("afrobeat", "Afro Vibes", "AFRO", track_afrobeat),
    ("amapiano", "Amapiano Groove", "AFRO", track_amapiano),
    ("dembow", "Tropical Dembow", "AFRO", track_dembow),
    ("trap", "Trap Bass", "DYNAMIQUE", track_trap),
    ("drill", "Drill Night", "DYNAMIQUE", track_drill),
    ("phonk", "Phonk Drift", "DYNAMIQUE", track_phonk),
    ("edm", "Big Drop", "TECHNO", track_edm),
    ("house", "Deep House Pulse", "TECHNO", track_house),
    ("future", "Future Bass", "TECHNO", track_future),
    ("synthwave", "Outrun", "TECHNO", track_synthwave),
    ("hiphop", "Boom Bap Study", "EDUCATIVE", track_hiphop),
    ("lofi", "Lo-fi Beats", "EDUCATIVE", track_lofi),
    ("cinematic", "Epic Trailer", "CINEMATIQUE", track_cinematic),
]


def write_wav(path, x):
    from scipy.io import wavfile
    wavfile.write(path, SR, (np.clip(x.T, -1, 1) * 32767).astype(np.int16))


if __name__ == "__main__":
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "build/music")
    only = set(sys.argv[2:])
    out.mkdir(parents=True, exist_ok=True)
    meta = []
    for tid, title, category, fn in TRACKS:
        if only and tid not in only:
            continue
        x, bpm = fn()
        mono = x.mean(axis=0)
        peaks = np.array([np.sqrt(np.mean(b ** 2)) for b in np.array_split(mono, 48)])
        peaks = np.round(peaks / max(peaks.max(), 1e-9), 3).tolist()
        write_wav(out / f"{tid}.wav", x)
        rms = 20 * np.log10(np.sqrt(np.mean(x ** 2)))
        print(f"{tid:10s} {bpm} BPM  RMS {rms:.1f} dBFS  crête {20 * np.log10(np.max(np.abs(x))):.1f} dBFS  min RMS/s {min(20*np.log10(np.sqrt(np.mean(c**2))+1e-9) for c in np.array_split(mono[:int(59*SR)], 59)):.1f}")
        meta.append({"id": tid, "title": title, "category": category, "bpm": bpm, "file": f"music/{tid}.m4a",
                     "durationMs": int(LENGTH_S * 1000), "licence": "GPL-3.0-or-later",
                     "origin": "Composition originale CODJIA, synthétisée par tools/music/compose_energy.py (aucun échantillon tiers)",
                     "peaks": peaks})
    (out / "music.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
