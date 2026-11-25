#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""
Musiques douces ORIGINALES de CODJIA, entièrement synthétisées par ce script.

Aucun échantillon, aucune mélodie ou boucle tierce : chaque note est calculée ici.
Les fichiers produits sont donc couverts par la licence du projet et peuvent être
redistribués partout (Google Play, F-Droid), sans droits d'auteur à payer.

Usage :  python3 tools/music/compose.py <dossier_de_sortie>
Produit : <id>.wav (44,1 kHz stéréo) — puis encodage AAC par build_music.sh.
"""
import json
import sys
from pathlib import Path

import numpy as np
from scipy.signal import butter, sosfilt

SR = 44_100
LENGTH_S = 48.0
N = int(SR * LENGTH_S)
RNG = np.random.default_rng(20261124)  # graine fixe : la musique est identique à chaque génération


# ─────────────────────────── outils de synthèse ───────────────────────────

def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def env(n, a, d, s, r, sustain_len):
    """Enveloppe ADSR (durées en secondes), longueur totale n échantillons."""
    out = np.zeros(n)
    ia, id_, isus, ir = int(a * SR), int(d * SR), int(sustain_len * SR), int(r * SR)
    t = 0
    seg = min(ia, n); out[:seg] = np.linspace(0, 1, seg, endpoint=False); t += seg
    seg = min(id_, n - t); out[t:t + seg] = np.linspace(1, s, seg, endpoint=False); t += seg
    seg = min(max(isus - ia - id_, 0), n - t); out[t:t + seg] = s; t += seg
    seg = min(ir, n - t); out[t:t + seg] = np.linspace(s, 0, seg); t += seg
    return out


def decay(n, tau):
    return np.exp(-np.arange(n) / (tau * SR))


def lowpass(x, fc, order=2):
    return sosfilt(butter(order, min(fc, SR / 2.2), "low", fs=SR, output="sos"), x)


def highpass(x, fc, order=2):
    return sosfilt(butter(order, fc, "high", fs=SR, output="sos"), x)


def bandpass(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def saw(freq, n, harmonics=24):
    """Dent de scie à bande limitée (somme d'harmoniques) : douce, sans repliement."""
    t = np.arange(n) / SR
    out = np.zeros(n)
    for k in range(1, harmonics + 1):
        if freq * k > SR / 2.5:
            break
        out += np.sin(2 * np.pi * freq * k * t) / k
    return out * 0.55


def add(track, sig, start_s, pan=0.0):
    """Ajoute un signal mono dans la piste stéréo, avec panoramique à puissance constante."""
    i = int(start_s * SR)
    if i >= N:
        return
    sig = sig[: N - i]
    l = np.cos((pan + 1) * np.pi / 4)
    r = np.sin((pan + 1) * np.pi / 4)
    track[0, i:i + len(sig)] += sig * l
    track[1, i:i + len(sig)] += sig * r


def reverb(x, seconds=2.4, mix=0.25, damp=5000):
    """Réverbération par convolution avec une réponse impulsionnelle synthétique décorrélée L/R."""
    n = int(seconds * SR)
    out = np.empty_like(x)
    for ch in range(2):
        ir = RNG.standard_normal(n) * np.exp(-np.arange(n) / (seconds * SR / 6.9))
        ir = lowpass(ir, damp)
        ir[: int(0.012 * SR)] = 0  # pré-délai
        ir /= np.sqrt(np.sum(ir ** 2))
        wet = np.fft.irfft(np.fft.rfft(x[ch], len(x[ch]) + n) * np.fft.rfft(ir, len(x[ch]) + n))[: len(x[ch])]
        out[ch] = x[ch] * (1 - mix) + wet * mix * 1.6
    return out


def master(x, target_rms_db=-17.0):
    """Mastering doux : passe-haut, niveau RMS visé, limiteur souple, crête à −1 dBFS, fondus."""
    x = np.stack([highpass(x[0], 35), highpass(x[1], 35)])
    # Compatibilité mono (haut-parleur de téléphone) : on réduit la largeur stéréo
    # jusqu'à une corrélation gauche/droite d'au moins 0,4.
    mid, side = (x[0] + x[1]) / 2, (x[0] - x[1]) / 2
    for _ in range(12):
        if np.corrcoef(mid + side, mid - side)[0, 1] >= 0.4:
            break
        side *= 0.8
    x = np.stack([mid + side, mid - side])
    rms = np.sqrt(np.mean(x ** 2)) + 1e-9
    x *= 10 ** (target_rms_db / 20) / rms
    x = np.tanh(x * 1.2) / 1.2
    x *= 10 ** (-1 / 20) / max(np.max(np.abs(x)), 1e-9)
    final_rms = 20 * np.log10(np.sqrt(np.mean(x ** 2)) + 1e-9)
    if final_rms > target_rms_db + 1:  # musique « douce » : jamais plus forte que la cible
        x *= 10 ** ((target_rms_db + 1 - final_rms) / 20)
    fade_in = int(0.02 * SR)
    fade_out = int(3.0 * SR)
    x[:, :fade_in] *= np.linspace(0, 1, fade_in)
    x[:, -fade_out:] *= np.linspace(1, 0, fade_out) ** 2
    return x


# ─────────────────────────── instruments ───────────────────────────

def pad(notes, dur, bright=1800):
    n = int((dur + 2.0) * SR)
    out = np.zeros(n)
    for m in notes:
        f = midi(m)
        for det in (-0.07, 0.0, 0.07):  # trois oscillateurs désaccordés = largeur
            out += saw(f * 2 ** (det / 12), n, 12)
    out = lowpass(out, bright, 2) * env(n, 0.9, 0.5, 0.8, 1.8, dur) / (len(notes) * 3)
    return out


def keys(m, dur, vel=1.0):
    """Piano électrique doux : fondamentale + 2e harmonique + léger « tine » qui s'éteint vite."""
    n = int((dur + 1.2) * SR)
    t = np.arange(n) / SR
    f = midi(m)
    tone = np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * 2 * f * t) * decay(n, 0.6)
    tone += 0.12 * np.sin(2 * np.pi * 7.1 * f * t) * decay(n, 0.05)
    trem = 1 + 0.06 * np.sin(2 * np.pi * 4.5 * t)
    return tone * trem * env(n, 0.004, 0.6, 0.45, 0.9, dur) * 0.22 * vel


def pluck(m, vel=1.0, tau=0.55):
    """Piano / harpe : partiels légèrement inharmoniques qui s'éteignent à des vitesses différentes."""
    n = int(2.6 * SR)
    t = np.arange(n) / SR
    f = midi(m)
    out = np.zeros(n)
    for k, a in ((1, 1.0), (2, 0.42), (3, 0.2), (4, 0.1), (5, 0.05)):
        fk = f * k * (1 + 0.0004 * k * k)
        out += a * np.sin(2 * np.pi * fk * t) * decay(n, tau / k ** 0.7)
    att = np.minimum(1, np.arange(n) / (0.003 * SR))
    return out * att * 0.2 * vel


def balafon(m, vel=1.0):
    """Lame de bois frappée (type balafon / marimba) : partiels 1 ; 3,93 ; 9,2."""
    n = int(1.4 * SR)
    t = np.arange(n) / SR
    f = midi(m)
    out = (np.sin(2 * np.pi * f * t) * decay(n, 0.35)
           + 0.35 * np.sin(2 * np.pi * 3.93 * f * t) * decay(n, 0.07)
           + 0.12 * np.sin(2 * np.pi * 9.2 * f * t) * decay(n, 0.02))
    buzz = 0.04 * bandpass(RNG.standard_normal(n), 300, 1200) * decay(n, 0.04)  # léger bourdonnement de calebasse
    return (out + buzz) * np.minimum(1, np.arange(n) / (0.002 * SR)) * 0.28 * vel


def bass(m, dur, vel=1.0):
    n = int((dur + 0.3) * SR)
    t = np.arange(n) / SR
    f = midi(m)
    tone = np.sin(2 * np.pi * f * t) + 0.18 * np.sin(2 * np.pi * 2 * f * t)
    return tone * env(n, 0.01, 0.2, 0.7, 0.25, dur) * 0.32 * vel


def arp_synth(m, dur, cutoff=2600):
    n = int((dur + 0.4) * SR)
    s = saw(midi(m), n, 18) * env(n, 0.003, 0.18, 0.25, 0.25, dur)
    return lowpass(s, cutoff, 2) * 0.22


def kick(vel=1.0):
    n = int(0.45 * SR)
    t = np.arange(n) / SR
    f = 48 + 85 * np.exp(-t / 0.035)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * decay(n, 0.16) * 0.55 * vel


def snare(vel=1.0):
    n = int(0.3 * SR)
    noise = bandpass(RNG.standard_normal(n), 900, 6000) * decay(n, 0.07)
    body = np.sin(2 * np.pi * 185 * np.arange(n) / SR) * decay(n, 0.05)
    return (noise * 0.35 + body * 0.2) * vel


def hat(vel=1.0, tau=0.025):
    n = int(0.15 * SR)
    return highpass(RNG.standard_normal(n), 7000, 2) * decay(n, tau) * 0.12 * vel


def shaker(vel=1.0):
    n = int(0.12 * SR)
    e = np.minimum(1, np.arange(n) / (0.02 * SR)) * decay(n, 0.03)
    return bandpass(RNG.standard_normal(n), 3000, 9000) * e * 0.1 * vel


def tom(m, vel=1.0):
    n = int(0.5 * SR)
    t = np.arange(n) / SR
    f = midi(m) * (1 + 0.6 * np.exp(-t / 0.03))
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * decay(n, 0.18) * 0.35 * vel


# ─────────────────────────── morceaux ───────────────────────────

def track_aube():
    """Calme, ambiant, 70 BPM. Nappes larges et cloches clairsemées."""
    bpm = 70; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[48, 55, 59, 64], [45, 52, 55, 60, 64], [41, 48, 52, 57], [43, 50, 53, 59, 62]]  # Cmaj7 Am9 Fmaj7 G6
    bells = [76, 79, 74, 72, 71, 74, 76, 67]
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, pad(ch, bar), t, 0.0)
        add(tr, bass(ch[0] - 12, bar * 0.95, 0.8), t)
        for k, off in enumerate((0, 1.5, 2.5)):
            if (i + k) % 2 == 0:
                add(tr, pluck(bells[(i * 3 + k) % len(bells)], 0.55, 1.2), t + off * beat, (-0.5, 0.4, 0.1)[k])
        t += bar; i += 1
    return master(reverb(tr, 3.2, 0.42, 4500), -18), bpm


def track_lofi():
    """Lo-fi concentré, 80 BPM. Accords jazz au piano électrique, batterie feutrée, craquements de vinyle."""
    bpm = 80; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[50, 53, 57, 60, 64], [43, 53, 57, 59, 64], [48, 52, 55, 59, 62], [45, 49, 55, 58, 61]]  # Dm9 G13 Cmaj9 A7b9
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        for k, m in enumerate(ch[1:]):
            add(tr, keys(m, bar * 0.9, 0.85), t + k * 0.018, (-0.3 + 0.15 * k))  # léger « strum »
        add(tr, keys(ch[1] + 12, beat * 0.6, 0.35), t + 2.5 * beat, 0.4)
        add(tr, bass(ch[0] - 12, beat * 1.5), t)
        add(tr, bass(ch[0] - 12, beat * 0.8, 0.8), t + 2.5 * beat)
        if i >= 1:
            for b in range(4):
                tb = t + b * beat
                if b in (0, 2): add(tr, kick(0.8), tb)
                if b == 2 and i % 2: add(tr, kick(0.5), tb + 0.75 * beat)
                if b in (1, 3): add(tr, snare(0.55), tb + 0.02)
                for h in (0, 0.5):
                    swing = 0.08 * beat if h else 0
                    add(tr, hat(0.7 if h == 0 else 0.45), tb + h * beat + swing, 0.3)
        t += bar; i += 1
    crackle = np.zeros(N)
    idx = RNG.integers(0, N, 900)
    crackle[idx] = RNG.standard_normal(900) * 0.25
    crackle = highpass(crackle, 2500)
    tr += crackle * 0.6
    tr = np.stack([lowpass(tr[0], 7500), lowpass(tr[1], 7500)])  # douceur « cassette »
    return master(reverb(tr, 1.6, 0.18, 6000), -17), bpm


def track_elan():
    """Inspirant, 100 BPM. Arpèges de piano en croches, cordes qui montent, claps à partir de la mesure 9."""
    bpm = 100; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[48, 55, 60, 64], [43, 55, 59, 62], [45, 52, 57, 60], [41, 53, 57, 60]]  # C G Am F
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        lift = min(1.0, 0.55 + i * 0.04)
        add(tr, pad([m + 12 for m in ch[1:]], bar, 1400 + i * 60) * lift * 0.8, t)
        add(tr, bass(ch[0] - 12, bar * 0.9, 0.9), t)
        pattern = [ch[1], ch[2], ch[3], ch[2] + 12, ch[3] + 12, ch[2] + 12, ch[3], ch[2]]
        for k, m in enumerate(pattern):
            add(tr, pluck(m + 12, 0.75 if k % 2 else 0.95), t + k * beat / 2, -0.35 + 0.1 * k)
        if i >= 8:
            for b in (1, 3):
                add(tr, snare(0.35), t + b * beat, 0.1)
            add(tr, kick(0.55), t)
            add(tr, kick(0.45), t + 2 * beat)
        t += bar; i += 1
    return master(reverb(tr, 2.2, 0.3, 6000), -17), bpm


def track_pulse():
    """Tech minimal, 110 BPM. Arpège filtré en doubles croches, sous-basse, pied doux."""
    bpm = 110; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[45, 57, 60, 64], [41, 57, 60, 65], [48, 55, 60, 64], [43, 55, 59, 62]]  # Am F C G
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, pad(ch[1:], bar, 1100) * 0.7, t)
        seq = [ch[1], ch[2], ch[3], ch[2] + 12]
        for k in range(16):
            cutoff = 1200 + 2600 * (0.5 + 0.5 * np.sin(2 * np.pi * (i * 16 + k) / 64))  # filtre qui respire
            add(tr, arp_synth(seq[k % 4] + 12, beat / 4 * 0.8, cutoff) * (1.0 if k % 4 == 0 else 0.7),
                t + k * beat / 4, 0.35 if k % 2 else -0.35)
        for b in range(4):
            add(tr, bass(ch[0] - 12, beat * 0.45, 0.9), t + b * beat + beat / 2)
            if i >= 2:
                add(tr, kick(0.6), t + b * beat)
                add(tr, hat(0.5, 0.02), t + b * beat + beat / 2, 0.2)
        t += bar; i += 1
    return master(reverb(tr, 1.8, 0.22, 7000), -17), bpm


def track_sahel():
    """Chaleureux, 96 BPM. Balafon en gamme pentatonique, shaker, tambour grave. Compositions originales."""
    bpm = 96; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    scale = [55, 57, 59, 62, 64, 67, 69, 71, 74, 76]  # sol majeur pentatonique
    roots = [43, 40, 36, 38]  # G Em C D
    motif = [0, 2, 4, 3, 2, 4, 5, 4, 2, 1, 2, 0, 3, 2, 1, 0]
    t = 0.0; i = 0
    while t < LENGTH_S:
        r = roots[i % 4]
        add(tr, pad([r + 12, r + 19, r + 24], bar, 1200) * 0.45, t)
        add(tr, bass(r, beat * 1.6, 0.9), t)
        add(tr, bass(r + 7, beat * 0.9, 0.7), t + 2.5 * beat)
        shift = (i % 4) in (1, 2)
        for k in range(8):
            note = scale[(motif[(i * 8 + k) % 16] + (1 if shift else 0)) % len(scale)]
            vel = 1.0 if k % 2 == 0 else 0.7
            add(tr, balafon(note + 12, vel), t + k * beat / 2, -0.4 + 0.1 * k)
            if k in (3, 7) and i % 2:
                add(tr, balafon(note + 24, 0.4), t + k * beat / 2 + beat / 4, 0.5)
        for k in range(16):
            add(tr, shaker(1.0 if k % 4 == 2 else 0.55), t + k * beat / 4, 0.45)
        if i >= 1:
            add(tr, tom(40, 0.9), t)
            add(tr, tom(47, 0.55), t + 1.5 * beat, -0.3)
            add(tr, tom(40, 0.7), t + 2.5 * beat)
            add(tr, tom(52, 0.45), t + 3.5 * beat, 0.3)
        t += bar; i += 1
    return master(reverb(tr, 1.9, 0.22, 6500), -17), bpm


def clap(vel=1.0):
    n = int(0.25 * SR)
    e = np.zeros(n)
    for k, off in enumerate((0, 0.011, 0.022)):  # trois mains légèrement décalées
        i = int(off * SR); e[i:] += decay(n - i, 0.012 if k < 2 else 0.06) * (0.6 if k < 2 else 1.0)
    return bandpass(RNG.standard_normal(n), 900, 5000) * e * 0.18 * vel


def bird(f0, vel=1.0):
    """Gazouillis synthétique : courte glissade de fréquence modulée."""
    n = int(0.18 * SR)
    t = np.arange(n) / SR
    f = f0 * (1 + 0.35 * np.sin(2 * np.pi * 22 * t)) * (1 + 0.5 * t / 0.18)
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.sin(np.pi * t / 0.18) ** 2 * 0.05 * vel


def track_momentum():
    """Dynamique et motivant, 118 BPM. Pied régulier, claps, accords pincés à contretemps, basse ronde."""
    bpm = 118; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[45, 57, 60, 64], [41, 57, 60, 65], [43, 55, 59, 62], [48, 55, 60, 64]]  # Am F G C
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, pad([m + 12 for m in ch[1:]], bar, 2200) * 0.35, t)
        for b in range(4):
            tb = t + b * beat
            add(tr, kick(0.75), tb)
            if b in (1, 3) and i >= 1: add(tr, clap(0.9), tb, 0.05)
            add(tr, hat(0.55, 0.018), tb + beat / 2, 0.25)
            for k, m in enumerate(ch[1:]):  # accord pincé à contretemps
                add(tr, pluck(m + 12, 0.55, 0.22), tb + beat / 2 + k * 0.006, -0.3 + 0.3 * k)
            add(tr, bass(ch[0] - 12 + (12 if b == 3 else 0), beat * 0.42, 0.9), tb + beat / 2)
        if i >= 4:
            lead = [ch[3] + 12, ch[2] + 12, ch[3] + 12, ch[1] + 24]
            for k, m in enumerate(lead):
                add(tr, keys(m, beat * 0.8, 0.5), t + k * beat, 0.2)
        t += bar; i += 1
    return master(reverb(tr, 1.4, 0.16, 7500), -17), bpm


def track_horizon():
    """Professionnel et corporate, 92 BPM. Piano clair, nappe discrète, percussions légères."""
    bpm = 92; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[50, 57, 62, 66], [47, 55, 59, 62], [43, 55, 59, 62, 66], [45, 57, 61, 64]]  # D Bm Gmaj7 A
    melody = [74, 73, 71, 69, 71, 73, 74, 76]
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, pad(ch[1:], bar, 1500) * 0.4, t)
        add(tr, bass(ch[0] - 12, bar * 0.9, 0.8), t)
        for b in range(4):
            for k, m in enumerate(ch[1:]):
                add(tr, pluck(m, 0.45 if b % 2 else 0.6, 0.7), t + b * beat + k * 0.012, -0.2 + 0.15 * k)
            if i >= 2:
                add(tr, hat(0.35, 0.03), t + b * beat + beat / 2, 0.3)
                if b == 0: add(tr, kick(0.45), t)
                if b == 2: add(tr, snare(0.25), t + b * beat)
        if i >= 1:
            for k in range(2):
                add(tr, pluck(melody[(i * 2 + k) % len(melody)], 0.7, 0.9), t + k * 2 * beat, 0.15)
        t += bar; i += 1
    return master(reverb(tr, 2.0, 0.24, 6500), -17), bpm


def track_canopee():
    """Nature et environnement, 84 BPM. Nappe aérienne, souffle du vent, lames de bois, chants d'oiseaux."""
    bpm = 84; beat = 60 / bpm; bar = 4 * beat
    tr = np.zeros((2, N))
    prog = [[52, 59, 64, 68], [49, 56, 61, 64], [45, 57, 61, 64], [47, 54, 59, 63]]  # E C#m A B
    t = 0.0; i = 0
    while t < LENGTH_S:
        ch = prog[i % 4]
        add(tr, pad(ch[1:], bar, 1300) * 0.6, t)
        add(tr, bass(ch[0] - 12, bar * 0.9, 0.6), t)
        notes = [ch[1] + 12, ch[2] + 12, ch[3] + 12, ch[2] + 12, ch[1] + 24, ch[3] + 12]
        for k, off in enumerate((0, 0.75, 1.5, 2, 2.75, 3.5)):
            if (i + k) % 3 != 2:
                add(tr, balafon(notes[k], 0.55), t + off * beat, -0.5 + 0.2 * k)
        for _ in range(3):  # oiseaux, placés au hasard (graine fixe)
            add(tr, bird(RNG.uniform(2600, 4200), RNG.uniform(0.5, 1.0)), t + RNG.uniform(0, bar), RNG.uniform(-0.8, 0.8))
        t += bar; i += 1
    wind = lowpass(RNG.standard_normal(N), 700) * (0.5 + 0.5 * np.sin(2 * np.pi * np.arange(N) / SR / 9.0))
    tr += np.stack([wind, np.roll(wind, 2000)]) * 0.05
    return master(reverb(tr, 2.8, 0.35, 5000), -18), bpm


# (identifiant, titre, catégorie) — les catégories reprennent celles de l'application.
TRACKS = [
    ("aube", "Aube", "DOUCE", track_aube),
    ("momentum", "Momentum", "DYNAMIQUE", track_momentum),
    ("horizon", "Horizon", "CORPORATE", track_horizon),
    ("elan", "Élan", "INSPIRANTE", track_elan),
    ("pulse", "Pulse", "TECHNO", track_pulse),
    ("lofi", "Lo-fi Focus", "EDUCATIVE", track_lofi),
    ("canopee", "Canopée", "NATURE", track_canopee),
    ("sahel", "Sahel", "NATURE", track_sahel),
]


def write_wav(path, x):
    from scipy.io import wavfile
    wavfile.write(path, SR, (np.clip(x.T, -1, 1) * 32767).astype(np.int16))


if __name__ == "__main__":
    import compose_pro  # morceaux à la production plus riche (ajoutés après les 8 premiers : ceux-ci restent identiques)
    TRACKS = TRACKS + compose_pro.PRO_TRACKS
    out = Path(sys.argv[1] if len(sys.argv) > 1 else "build/music")
    out.mkdir(parents=True, exist_ok=True)
    meta = []
    for tid, title, category, fn in TRACKS:
        x, bpm = fn()
        mono = x.mean(axis=0)
        bins = np.array_split(mono, 48)
        peaks = np.array([np.sqrt(np.mean(b ** 2)) for b in bins])
        peaks = np.round(peaks / max(peaks.max(), 1e-9), 3).tolist()  # forme d'onde affichée dans l'appli
        write_wav(out / f"{tid}.wav", x)
        rms = 20 * np.log10(np.sqrt(np.mean(x ** 2)))
        print(f"{tid:6s} {bpm} BPM  RMS {rms:.1f} dBFS  crête {20*np.log10(np.max(np.abs(x))):.1f} dBFS")
        meta.append({"id": tid, "title": title, "category": category, "bpm": bpm, "file": f"music/{tid}.m4a",
                     "durationMs": int(LENGTH_S * 1000),
                     "licence": "GPL-3.0-or-later",
                     "origin": "Composition originale CODJIA, synthétisée par tools/music/compose.py (aucun échantillon tiers)",
                     "peaks": peaks})
    (out / "music.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
