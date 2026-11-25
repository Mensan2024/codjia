# SPDX-License-Identifier: GPL-3.0-or-later
"""
Musiques « pro » ORIGINALES de CODJIA : production plus riche (structure intro / montée / refrain /
sortie, compression latérale, basses 808 et « log drum », accords « supersaw », montées de bruit).

Tout est synthétisé ici, sans aucun échantillon : même licence que le projet (GPL-3.0-or-later).
Ce module est importé par compose.py, qui l'ajoute à la fin de la liste des morceaux.
"""
import numpy as np

from compose import (SR, N, LENGTH_S, RNG, midi, env, decay, lowpass, highpass, bandpass, saw, add, reverb, master,
                     keys, pluck, kick, snare, hat, shaker)


# ─────────────────────────── outils de production ───────────────────────────

def sidechain(x, times, depth=0.65, release=0.22):
    """Compression latérale : le son « pompe » sur chaque coup de grosse caisse."""
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


def supersaw(notes, dur, cutoff=3200, voices=5, detune=0.16):
    n = int((dur + 0.6) * SR)
    out = np.zeros(n)
    for m in notes:
        for v in range(voices):
            d = (v - (voices - 1) / 2) * detune / ((voices - 1) / 2)
            out += saw(midi(m) * 2 ** (d / 12), n, 14)
    out = lowpass(out, cutoff, 2) * env(n, 0.01, 0.15, 0.85, 0.5, dur)
    return out / (len(notes) * voices) * 0.9


def sub808(m, dur, glide_from=None, vel=1.0):
    """Basse 808 : sinus grave à longue tenue, attaque claquée, glissando optionnel, légère saturation."""
    n = int((dur + 0.2) * SR)
    t = np.arange(n) / SR
    f1 = midi(m)
    f = np.full(n, f1)
    if glide_from is not None:
        f0 = midi(glide_from)
        k = np.minimum(1, t / 0.09)
        f = f0 + (f1 - f0) * k
    f = f * (1 + 0.5 * np.exp(-t / 0.015))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR)
    s = np.tanh(s * 1.6) / np.tanh(1.6)
    return s * env(n, 0.002, 0.1, 0.85, 0.12, dur) * 0.42 * vel


def log_drum(m, vel=1.0):
    """« Log drum » (amapiano) : note grave percussive dont la hauteur chute, saturée, très présente."""
    n = int(0.7 * SR)
    t = np.arange(n) / SR
    f = midi(m) * (1 + 0.9 * np.exp(-t / 0.02))
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.3 * np.sin(4 * np.pi * np.cumsum(f) / SR) * decay(n, 0.06)
    s = np.tanh(s * 2.2) * decay(n, 0.28)
    return lowpass(s, 2200) * 0.5 * vel


def rim(vel=1.0):
    n = int(0.08 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * 1700 * t) * decay(n, 0.008) + bandpass(RNG.standard_normal(n), 2000, 6000) * decay(n, 0.006) * 0.5
    return s * 0.22 * vel


def clap2(vel=1.0):
    """Claquement de mains : trois éclats de bruit rapprochés + queue."""
    n = int(0.35 * SR)
    out = np.zeros(n)
    for k, d in enumerate((0.0, 0.011, 0.022)):
        i = int(d * SR)
        m = n - i
        out[i:] += bandpass(RNG.standard_normal(m), 900, 5000) * decay(m, 0.012 if k < 2 else 0.09)
    return out * 0.32 * vel


def hat_roll(start, beat, kind, tr, vel=0.5, pan=0.25):
    """Charleston en triolets ou en triples croches (style trap)."""
    div = {"t": 3, "s": 4, "f": 8}[kind]
    for k in range(div):
        add(tr, 2.0 * hat(vel * (0.7 + 0.3 * (k % 2 == 0)), 0.015), start + k * beat / div, pan)


def riser(dur, start_f=300, end_f=9000, vel=1.0):
    """Montée de bruit filtré (avant un refrain)."""
    n = int(dur * SR)
    noise = RNG.standard_normal(n)
    out = np.zeros(n)
    blocks = 32
    for b in range(blocks):
        i0, i1 = b * n // blocks, (b + 1) * n // blocks
        f = start_f * (end_f / start_f) ** (b / blocks)
        out[i0:i1] = bandpass(noise[i0:i1], max(80, f * 0.6), min(f * 1.4, SR / 2.3))
    return out * np.linspace(0.0, 1.0, n) ** 2 * 0.25 * vel


def impact(vel=1.0):
    """Impact grave au début d'une section."""
    n = int(1.8 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * np.cumsum(45 * (1 + 2 * np.exp(-t / 0.05))) / SR) * decay(n, 0.6)
    s += lowpass(RNG.standard_normal(n), 900) * decay(n, 0.25) * 0.4
    return s * 0.5 * vel


def strings(notes, dur, cutoff=2400):
    """Cordes : scies douces avec vibrato lent et attaque progressive."""
    n = int((dur + 1.0) * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    for m in notes:
        vib = 1 + 0.004 * np.sin(2 * np.pi * 5.2 * t)
        ph = 2 * np.pi * np.cumsum(midi(m) * vib) / SR
        for k in range(1, 12):
            out += np.sin(ph * k) / k * (0.98 ** k)
    return lowpass(out, cutoff, 2) * env(n, 0.35, 0.3, 0.8, 0.8, dur) / len(notes) * 0.35


def taiko(vel=1.0):
    n = int(1.1 * SR)
    t = np.arange(n) / SR
    s = np.sin(2 * np.pi * np.cumsum(62 * (1 + 1.2 * np.exp(-t / 0.02))) / SR) * decay(n, 0.35)
    s += bandpass(RNG.standard_normal(n), 120, 900) * decay(n, 0.05) * 0.6
    return s * 0.55 * vel


def guitar_pluck(m, vel=1.0):
    """Guitare « highlife » claire (Karplus-Strong simplifié)."""
    f = midi(m)
    period = int(SR / f)
    n = int(1.2 * SR)
    buf = RNG.uniform(-1, 1, period)
    out = np.zeros(n)
    for i in range(n):
        out[i] = buf[i % period]
        buf[i % period] = 0.5 * (buf[i % period] + buf[(i + 1) % period]) * 0.996
    return highpass(out, 180) * 0.25 * vel


def eqp(x):
    """Égalisation pour haut-parleur de téléphone : moins de sous-graves, plus de présence et d'air."""
    out = np.empty_like(x)
    for ch in range(2):
        low = lowpass(x[ch], 110, 2)
        pres = bandpass(x[ch], 1800, 6000, 2)
        air = highpass(x[ch], 7000, 2)
        out[ch] = x[ch] - 0.5 * low + 1.1 * pres + 1.4 * air
    return out


def pro_master(x, target):
    return master(eqp(x), target)


# ─────────────────────────── morceaux ───────────────────────────

def track_afro():
    """Afro Groove, 104 BPM : grosse caisse afrobeats syncopée, rimshots, shakers, guitare highlife, basse ronde."""
    bpm = 104; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N)); drums = np.zeros((2, N))
    prog = [[50, 62, 65, 69], [55, 62, 67, 71], [52, 64, 67, 71], [57, 61, 64, 69]]  # Dm G Em A
    kicks = []
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        section = 0 if i < 2 else (1 if i < 12 else 2)
        # Pad chaud
        add(tr, supersaw(ch[1:], bar, 1400, 3, 0.08) * 0.55, t)
        # Guitare : motif syncopé (croches pointées)
        if section >= 1:
            for k, step in enumerate((0, 3, 6, 8, 11, 14)):
                add(tr, guitar_pluck(ch[1 + k % 3] + 12, 0.9 if k % 2 == 0 else 0.7), t + step * beat / 4, 0.35 if k % 2 else -0.3)
        # Batterie afrobeats : grosse caisse 1, 2+, 3 ; rimshot sur les contretemps
        for b in range(4):
            if section >= 1 or b >= 2:
                for st in ((0,), (0,), (0, 3), (2,))[b]:
                    tt = t + b * beat + st * beat / 4
                    add(drums, kick(0.95), tt); kicks.append(tt)
                add(drums, rim(0.9), t + b * beat + 3 * beat / 4, 0.2)
                for s in range(4):
                    add(drums, 2.2 * shaker(0.7 if s % 2 else 1.0), t + b * beat + s * beat / 4, -0.25)
            if section >= 1 and b in (1, 3):
                add(drums, clap2(0.8), t + b * beat)
        # Basse : motif rebondi
        if section >= 1:
            for st, dn in ((0, 0), (3, 0), (6, 7), (8, 0), (11, 5), (14, 7)):
                add(tr, sub808(ch[0] - 12 + dn, beat / 4 * 1.6, vel=0.8), t + st * beat / 4)
        if i == 11:
            add(tr, riser(bar), t)
        t += bar; i += 1
    tr = sidechain(tr, kicks, 0.45, 0.18)
    return pro_master(reverb(tr + drums * 1.1, 1.4, 0.16, 6000), -14.5), bpm


def track_amapiano():
    """Amapiano, 112 BPM : log drum, piano en accords de septième, shakers fins, claps espacés."""
    bpm = 112; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N)); drums = np.zeros((2, N))
    prog = [[45, 60, 64, 67, 71], [50, 60, 62, 65, 69], [43, 59, 62, 65, 69], [48, 60, 64, 67, 71]]  # Am9 Dm9 G9 Cmaj7
    kicks = []
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        sec = 0 if i < 2 else 1
        # Piano : accords plaqués sur les temps 1 et 2+
        for st in (0, 6, 10):
            for m in ch[1:]:
                add(tr, keys(m, beat * 0.8, 0.75), t + st * beat / 4, -0.1)
        for b in range(4):
            tt = t + b * beat
            add(drums, kick(0.7 if sec else 0.4), tt); kicks.append(tt)
            for s in range(4):
                add(drums, 2.2 * shaker(0.55 + 0.35 * (s == 2)), tt + s * beat / 4, 0.3)
            if b == 3:
                add(drums, clap2(0.6), tt + beat / 2)
        # Log drum : motif caractéristique (syncopes, glissements d'octave)
        if sec:
            for st, dn in ((2, 0), (5, 0), (7, 12), (10, 0), (13, 7), (15, 0)):
                add(tr, log_drum(ch[0] - 12 + dn, 0.85), t + st * beat / 4)
        if i % 4 == 3:
            add(tr, pluck(ch[-1] + 12, 0.6), t + 3.5 * beat, 0.4)
        t += bar; i += 1
    tr = sidechain(tr, kicks, 0.3, 0.15)
    return pro_master(reverb(tr + drums, 1.6, 0.2, 7000), -14.5), bpm


def track_synthwave():
    """Synthwave, 100 BPM : arpège en croches, basse pulsée, caisse claire « gated », nappes rétro."""
    bpm = 100; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N)); drums = np.zeros((2, N))
    prog = [[45, 57, 60, 64], [41, 57, 60, 65], [43, 55, 59, 62], [40, 55, 59, 64]]  # Am F G Em
    kicks = []
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, supersaw(ch[1:], bar, 1600, 5, 0.18) * 0.6, t)
        arp = [ch[1] + 12, ch[2] + 12, ch[3] + 12, ch[2] + 24]
        for k in range(8):
            n = int((beat / 2 + 0.3) * SR)
            s = lowpass(saw(midi(arp[k % 4]), n, 16), 2400 + 1600 * (i % 2)) * env(n, 0.002, 0.12, 0.3, 0.2, beat / 2) * 0.18
            add(tr, s, t + k * beat / 2, 0.4 if k % 2 else -0.4)
        for k in range(8):
            add(tr, sub808(ch[0] - 12, beat / 2 * 0.7, vel=0.6), t + k * beat / 2)
        if i >= 2:
            for b in range(4):
                tt = t + b * beat
                add(drums, kick(0.9), tt); kicks.append(tt)
                add(drums, 2.0 * hat(0.45, 0.03), tt + beat / 2, 0.2)
                if b in (1, 3):
                    sn = snare(1.1)
                    add(drums, sn, tt)
        if i == 1:
            add(tr, riser(bar, 400, 8000, 0.8), t)
        t += bar; i += 1
    tr = sidechain(tr, kicks, 0.4, 0.2)
    drums = reverb(drums, 0.9, 0.35, 5000)  # réverbération courte et dense : effet « gated » des années 80
    return pro_master(reverb(tr, 2.2, 0.25, 6000) + drums, -14.5), bpm


def track_future():
    """Future Bass, 140 BPM (demi-temps) : accords supersaw qui pompent, notes hachées, montée puis refrain."""
    bpm = 140; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N)); drums = np.zeros((2, N))
    prog = [[50, 62, 65, 69, 72], [46, 62, 65, 70, 74], [53, 60, 65, 69, 72], [48, 60, 64, 67, 72]]  # Dm7 Bb F C
    kicks = []
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        section = 0 if i < 4 else (1 if i < 7 else (2 if i < 20 else 3))
        if section in (0, 3):
            for k, m in enumerate(ch[1:]):
                add(tr, pluck(m + 12, 0.7, 0.4), t + k * beat / 2, 0.3 if k % 2 else -0.3)
        if section == 1:
            for k in range(8 if i < 6 else 16):
                add(drums, snare(0.4 + 0.6 * k / 16), t + k * bar / (8 if i < 6 else 16))
            if i == 6:
                add(tr, riser(bar, 300, 10000, 1.2), t)
        if section == 2:
            if i == 7:
                add(drums, impact(0.9), t)
            # Accords supersaw hachés en croches (le « pompage » vient de la compression latérale)
            for k in range(8):
                if k in (3, 6):
                    continue
                add(tr, supersaw(ch[1:], beat / 2 * 0.9, 4200, 7, 0.22) * 1.2, t + k * beat / 2)
            for b in range(4):
                tt = t + b * beat
                if b in (0, 2):
                    add(drums, kick(1.0), tt); kicks.append(tt)
                if b == 2:
                    add(drums, clap2(1.0), tt); add(drums, snare(0.6), tt)
                add(drums, 2.0 * hat(0.4, 0.02), tt + beat / 2, 0.25)
            add(tr, sub808(ch[0] - 12, bar * 0.9, vel=0.7), t)
            # Petites notes « voix » (formants) en contre-chant
            for k, st in enumerate((2, 5, 9, 12)):
                n = int(0.25 * SR)
                f = midi(ch[1 + k % 4] + 12)
                tt = np.arange(n) / SR
                v = np.sin(2 * np.pi * f * tt) * (1 + 0.5 * np.sin(2 * np.pi * 2 * f * tt))
                add(tr, bandpass(v, 600, 2600) * env(n, 0.005, 0.08, 0.3, 0.1, 0.15) * 0.25, t + st * beat / 4, 0.2)
        t += bar; i += 1
    tr = sidechain(tr, kicks, 0.75, 0.3)
    return pro_master(reverb(tr + drums, 1.8, 0.22, 7000), -14.0), bpm


def track_trap():
    """Trap Tech, 140 BPM (demi-temps) : 808 qui glissent, charleston en triolets, cloche sombre."""
    bpm = 140; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N)); drums = np.zeros((2, N))
    roots = [45, 45, 41, 43]  # A A F G
    bell = [69, 72, 76, 74, 72, 69, 67, 64]
    t = 0.0; i = 0
    while t < LENGTH_S:
        r = roots[i % 4]
        # Cloche sombre (mélodie en noires)
        for k, m in enumerate(bell[(i % 2) * 4:(i % 2) * 4 + 4]):
            n = int(1.0 * SR)
            tt = np.arange(n) / SR
            f = midi(m)
            b = (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * 2.76 * f * tt) * decay(n, 0.15)) * decay(n, 0.45) * 0.14
            add(tr, b, t + k * beat, -0.2)
        add(tr, strings([r + 12, r + 15, r + 19], bar, 1600) * 0.6, t)
        if i >= 2:
            add(tr, sub808(r - 12, beat * 1.4, glide_from=(r - 7 if i % 2 else None)), t)
            add(tr, sub808(r - 12, beat * 0.9), t + 2.5 * beat)
            add(drums, kick(1.0), t); add(drums, kick(0.9), t + 2.5 * beat)
            add(drums, clap2(1.0), t + 2 * beat); add(drums, snare(0.5), t + 2 * beat)
            for b in range(4):
                kind = "t" if (b == 3 and i % 2) else ("f" if (b == 1 and i % 4 == 3) else "s")
                hat_roll(t + b * beat, beat, kind if kind != "s" else "s", drums, 0.45)
        t += bar; i += 1
    return pro_master(reverb(tr, 2.0, 0.2, 6000) + drums, -14.0), bpm


def track_cinematic():
    """Cinematic Rise, 90 BPM : ostinato de cordes, taikos, montées et impacts, thème héroïque."""
    bpm = 90; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N)); drums = np.zeros((2, N))
    prog = [[38, 50, 57, 62], [34, 50, 53, 58], [41, 53, 57, 60], [36, 48, 55, 60]]  # Dm Bb F C
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, strings(ch[1:], bar, 1800 + 300 * min(i, 6)) * 1.1, t)
        # Ostinato en doubles croches
        for k in range(16):
            n = int((beat / 4 + 0.2) * SR)
            m = ch[1 + (k % 3)] + 12
            s = lowpass(saw(midi(m), n, 12), 2600) * env(n, 0.004, 0.08, 0.2, 0.1, beat / 4) * (0.12 if k % 4 else 0.18)
            add(tr, s, t + k * beat / 4, 0.3 if k % 2 else -0.3)
        if i >= 2:
            for st in (0, 3, 6, 8, 10, 12, 14):
                add(drums, taiko(0.9 if st in (0, 8) else 0.6), t + st * beat / 4, -0.1 if st % 4 else 0.1)
        if i >= 6:
            # Thème héroïque aux cuivres (scies filtrées, attaque lente)
            for k, m in enumerate((ch[2] + 12, ch[3] + 12, ch[2] + 12, ch[1] + 12)):
                add(tr, supersaw([m], beat * 0.95, 2200, 3, 0.06) * 0.9, t + k * beat)
        if i in (3, 9):
            add(tr, riser(bar, 200, 7000, 1.0), t)
        if i in (4, 10):
            add(drums, impact(1.0), t)
        t += bar; i += 1
    return pro_master(reverb(tr + drums, 2.8, 0.3, 5500), -14.5), bpm


PRO_TRACKS = [
    ("afro", "Afro Groove", "AFRO", track_afro),
    ("amapiano", "Amapiano Sunset", "AFRO", track_amapiano),
    ("synthwave", "Synthwave Drive", "TECHNO", track_synthwave),
    ("future", "Future Lights", "DYNAMIQUE", track_future),
    ("trap", "Trap Tech", "DYNAMIQUE", track_trap),
    ("cinematic", "Cinematic Rise", "CINEMATIQUE", track_cinematic),
]
