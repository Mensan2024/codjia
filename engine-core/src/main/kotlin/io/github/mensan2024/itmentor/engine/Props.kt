// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Phase C — accessoires et décors des situations visuelles. Tout est vectoriel, sans image importée ni reproduction de produit ou de marque :
 * ce sont des objets génériques (jerrican, caisse, sac, bulle, bureau, tente…) assemblés par les scènes pour montrer une situation
 * (une personne devant un courriel, une famille au camp, une cliente à la caisse…).
 */
internal object Props {
    // ───────────── Couleurs d'ambiance ─────────────
    val WOOD = rgb(0x8A5A3A)
    val CLOTH = rgb(0xE8D9B5)
    val CANVAS = rgb(0xD8CDB0)
    val LEAF = rgb(0x2F8F57)
    val SUN = rgb(0xFFC857)
    val SKIN_TONES = listOf(rgb(0x7A4A30), rgb(0x8A5535), rgb(0x5B3623), rgb(0xB57A55), rgb(0x6A4029), rgb(0xD9A27C))

    // ───────────── Ciel, terre, astres ─────────────

    fun sky(g: Gfx, top: Int, bottom: Int, horizon: Float, w: Float = 1000f) { g.fillVerticalGradient(0f, 0f, w, horizon, top, bottom) }

    fun ground(g: Gfx, y: Float, color: Int, w: Float = 1000f, h: Float = 700f) {
        g.fillRect(0f, y, w, h, color)
        g.fillRect(0f, y, w, y + 5f, mix(color, rgb(0xFFFFFF), 0.18f))
    }

    fun sun(g: Gfx, x: Float, y: Float, r: Float, t: Float, color: Int = SUN) {
        SK.glow(g, x, y, r * 3.2f, color, 0.35f)
        SK.rays(g, x, y, r * 1.3f, r * 1.75f, 12, t / 4000f * 2f * PI.toFloat(), color.withAlpha(0.75f), r * 0.1f)
        g.fillCircle(x, y, r, color)
    }

    fun moon(g: Gfx, x: Float, y: Float, r: Float, bg: Int) {
        SK.glow(g, x, y, r * 3f, rgb(0xDDE6FF), 0.22f)
        g.fillCircle(x, y, r, rgb(0xF4F1DC))
        g.fillCircle(x + r * 0.42f, y - r * 0.18f, r * 0.86f, bg)
    }

    fun stars(g: Gfx, l: Float, t: Float, r: Float, b: Float, n: Int, tt: Float, seed: Int = 1) {
        for (i in 0 until n) {
            val x = l + SK.hash01(i, seed) * (r - l); val y = t + SK.hash01(i, seed + 7) * (b - t)
            val tw = 0.5f + 0.5f * sin(tt / 700f + i * 1.7f)
            g.fillCircle(x, y, 1.5f + 1.6f * SK.hash01(i, seed + 3), rgb(0xFFFFFF).withAlpha(0.35f + 0.5f * tw))
        }
    }

    fun cloud(g: Gfx, x: Float, y: Float, s: Float, color: Int = rgb(0xFFFFFF), a: Float = 0.9f) {
        val c = color.withAlpha(a)
        g.fillRoundRect(x - s, y - s * 0.22f, x + s, y + s * 0.3f, s * 0.3f, c)
        g.fillCircle(x - s * 0.35f, y - s * 0.28f, s * 0.42f, c)
        g.fillCircle(x + s * 0.18f, y - s * 0.4f, s * 0.52f, c)
    }

    // ───────────── Nature et bâtiments ─────────────

    fun tree(g: Gfx, x: Float, base: Float, h: Float, leaf: Int = LEAF, t: Float = 0f, who: Int = 0) {
        val sway = sin(t / 1400f + who) * h * 0.012f
        g.line(x, base, x + sway * 0.4f, base - h * 0.5f, h * 0.075f, mix(WOOD, rgb(0x000000), 0.15f))
        g.fillCircle(x + sway, base - h * 0.68f, h * 0.30f, leaf)
        g.fillCircle(x - h * 0.17f + sway, base - h * 0.55f, h * 0.22f, mix(leaf, rgb(0x000000), 0.1f))
        g.fillCircle(x + h * 0.18f + sway, base - h * 0.58f, h * 0.22f, mix(leaf, rgb(0xFFFFFF), 0.08f))
    }

    fun hut(g: Gfx, x: Float, base: Float, w: Float, wall: Int = rgb(0xC98F5F), roof: Int = rgb(0x8C5A2B)) {
        val h = w * 0.55f
        g.fillRect(x - w / 2f, base - h, x + w / 2f, base, wall)
        g.fillRect(x - w / 2f, base - h * 0.14f, x + w / 2f, base, mix(wall, rgb(0x000000), 0.18f))
        val r = PathData().moveTo(x - w * 0.62f, base - h).lineTo(x, base - h - w * 0.42f).lineTo(x + w * 0.62f, base - h).close()
        g.fillPath(r, roof)
        g.fillRoundRect(x - w * 0.12f, base - h * 0.72f, x + w * 0.12f, base, w * 0.03f, mix(wall, rgb(0x000000), 0.45f))
    }

    fun tent(g: Gfx, x: Float, base: Float, w: Float, color: Int = CANVAS, stripe: Int = rgb(0x3A7BD5)) {
        val h = w * 0.62f
        val p = PathData().moveTo(x - w / 2f, base).lineTo(x, base - h).lineTo(x + w / 2f, base).close()
        g.fillPath(p, color)
        val door = PathData().moveTo(x - w * 0.14f, base).lineTo(x, base - h * 0.62f).lineTo(x + w * 0.14f, base).close()
        g.fillPath(door, mix(color, rgb(0x000000), 0.55f))
        g.line(x - w / 2f, base, x, base - h, max(3f, w * 0.03f), stripe)
        g.line(x + w / 2f, base, x, base - h, max(3f, w * 0.03f), stripe)
        g.line(x, base - h, x, base - h - w * 0.07f, 3f, rgb(0x55402A))
    }

    fun bush(g: Gfx, x: Float, base: Float, s: Float, color: Int = LEAF) {
        g.fillCircle(x - s * 0.5f, base - s * 0.35f, s * 0.42f, mix(color, rgb(0x000000), 0.1f))
        g.fillCircle(x + s * 0.1f, base - s * 0.5f, s * 0.55f, color)
        g.fillCircle(x + s * 0.65f, base - s * 0.3f, s * 0.38f, mix(color, rgb(0xFFFFFF), 0.08f))
    }

    // ───────────── Intérieur ─────────────

    /** Pièce de jour : mur, plinthe, sol. Retourne l'ordonnée du sol. */
    fun room(g: Gfx, wall: Int, floor: Int, floorY: Float = 585f, w: Float = 1000f, h: Float = 700f): Float {
        g.fillVerticalGradient(0f, 0f, w, floorY, mix(wall, rgb(0xFFFFFF), 0.06f), mix(wall, rgb(0x000000), 0.12f))
        g.fillRect(0f, floorY, w, h, floor)
        g.fillRect(0f, floorY - 14f, w, floorY, mix(wall, rgb(0x000000), 0.28f))
        g.fillRect(0f, floorY, w, floorY + 6f, mix(floor, rgb(0xFFFFFF), 0.15f))
        return floorY
    }

    fun window(g: Gfx, l: Float, t: Float, r: Float, b: Float, sky: Int, frame: Int, night: Boolean = false, tt: Float = 0f) {
        g.fillRoundRect(l - 10f, t - 10f, r + 10f, b + 10f, 10f, frame)
        g.fillRoundRect(l, t, r, b, 5f, sky)
        if (night) stars(g, l + 6f, t + 6f, r - 6f, b - 6f, 10, tt, 4)
        g.fillRect((l + r) / 2f - 4f, t, (l + r) / 2f + 4f, b, frame)
        g.fillRect(l, (t + b) / 2f - 4f, r, (t + b) / 2f + 4f, frame)
    }

    /** Bureau vu de face : plateau + panneau avant ; se dessine APRÈS le personnage assis pour cacher ses jambes. */
    fun desk(g: Gfx, cx: Float, topY: Float, floorY: Float, w: Float, color: Int = WOOD) {
        g.fillRoundRect(cx - w / 2f - 8f, topY, cx + w / 2f + 8f, topY + 16f, 6f, mix(color, rgb(0xFFFFFF), 0.18f))
        g.fillRect(cx - w / 2f, topY + 16f, cx + w / 2f, floorY, mix(color, rgb(0x000000), 0.2f))
        g.fillRect(cx - w / 2f + 14f, topY + 30f, cx + w / 2f - 14f, topY + 36f, mix(color, rgb(0x000000), 0.4f))
    }

    /** Ordinateur portable vu de DOS (face tournée vers la personne) : le couvercle masque son ventre. */
    fun laptopBack(g: Gfx, cx: Float, deskY: Float, w: Float, color: Int = rgb(0xAEB6C4)) {
        val h = w * 0.62f
        g.fillRoundRect(cx - w / 2f, deskY - h, cx + w / 2f, deskY + 2f, w * 0.03f, color)
        g.fillRoundRect(cx - w * 0.07f, deskY - h * 0.55f, cx + w * 0.07f, deskY - h * 0.55f + w * 0.07f, w * 0.03f, mix(color, rgb(0xFFFFFF), 0.5f))
    }

    fun bench(g: Gfx, cx: Float, topY: Float, floorY: Float, w: Float, color: Int = WOOD) {
        g.fillRoundRect(cx - w / 2f, topY, cx + w / 2f, topY + 20f, 8f, mix(color, rgb(0xFFFFFF), 0.12f))
        g.fillRect(cx - w / 2f + 24f, topY + 20f, cx - w / 2f + 40f, floorY, mix(color, rgb(0x000000), 0.25f))
        g.fillRect(cx + w / 2f - 40f, topY + 20f, cx + w / 2f - 24f, floorY, mix(color, rgb(0x000000), 0.25f))
    }

    fun bed(g: Gfx, cx: Float, floorY: Float, w: Float, sheet: Int, frame: Int = WOOD) {
        g.fillRoundRect(cx - w / 2f, floorY - 130f, cx + w / 2f, floorY - 40f, 10f, frame)
        g.fillRoundRect(cx - w / 2f - 6f, floorY - 120f, cx + w / 2f + 6f, floorY - 80f, 14f, sheet)
        g.fillRoundRect(cx - w / 2f + 14f, floorY - 150f, cx - w / 2f + 110f, floorY - 112f, 14f, mix(sheet, rgb(0xFFFFFF), 0.5f))
        g.fillRect(cx - w / 2f + 10f, floorY - 40f, cx - w / 2f + 28f, floorY, mix(frame, rgb(0x000000), 0.3f))
        g.fillRect(cx + w / 2f - 28f, floorY - 40f, cx + w / 2f - 10f, floorY, mix(frame, rgb(0x000000), 0.3f))
    }

    /** Chaise vue de face derrière une personne assise de hauteur h (dessinée AVANT elle). */
    fun chair(g: Gfx, x: Float, floor: Float, h: Float, color: Int = rgb(0x6B5A48), mirror: Boolean = false) {
        val seatY = floor - 0.30f * h + 0.02f * h
        g.fillRoundRect(x - 0.17f * h, seatY - 0.36f * h, x + 0.17f * h, seatY - 0.02f * h, 0.04f * h, mix(color, rgb(0x000000), 0.12f))
        g.fillRoundRect(x - 0.2f * h, seatY, x + 0.2f * h, seatY + 0.045f * h, 0.02f * h, color)
        g.fillRect(x - 0.17f * h, seatY + 0.045f * h, x - 0.14f * h, floor, mix(color, rgb(0x000000), 0.3f))
        g.fillRect(x + 0.14f * h, seatY + 0.045f * h, x + 0.17f * h, floor, mix(color, rgb(0x000000), 0.3f))
        if (mirror) return
    }

    /** Gilet de couleur vive (bénévole, agent de terrain) posé sur le torse d'un personnage déjà dessiné. */
    fun vest(g: Gfx, x: Float, an: Anchors, h: Float, color: Int = rgb(0xFF8A3D)) {
        val top = an.shoulderY + 0.03f * h; val bot = an.hipY + 0.005f * h
        for (side in listOf(-1f, 1f)) {
            val l = if (side < 0) x - 0.125f * h else x + 0.012f * h
            g.fillRoundRect(l, top, l + 0.113f * h, bot, 0.012f * h, color)
            g.fillRect(l, top + (bot - top) * 0.55f, l + 0.113f * h, top + (bot - top) * 0.55f + 0.016f * h, rgb(0xF4F4D8))
        }
    }

    fun truck(g: Gfx, x: Float, base: Float, w: Float, body: Int = rgb(0xE9E4D6), cab: Int = rgb(0x2F6FB5), t: Float = 0f) {
        val h = w * 0.36f
        g.fillRoundRect(x - w / 2f, base - h - w * 0.07f, x + w * 0.22f, base - w * 0.07f, w * 0.015f, body)
        g.fillRoundRect(x + w * 0.22f, base - h * 0.78f - w * 0.07f, x + w / 2f, base - w * 0.07f, w * 0.03f, cab)
        g.fillRoundRect(x + w * 0.3f, base - h * 0.7f - w * 0.07f, x + w * 0.46f, base - h * 0.4f - w * 0.07f, w * 0.01f, rgb(0xBFE3F5))
        g.fillRect(x - w / 2f, base - w * 0.13f, x + w / 2f, base - w * 0.07f, rgb(0x3A3F50))
        for (wx in listOf(-0.3f, 0.35f)) { g.fillCircle(x + wx * w, base - w * 0.06f, w * 0.065f, rgb(0x20242E)); g.fillCircle(x + wx * w, base - w * 0.06f, w * 0.028f, rgb(0xC7CBD6)) }
        g.fillRoundRect(x - w * 0.42f, base - h * 0.78f - w * 0.07f, x + w * 0.12f, base - h * 0.5f - w * 0.07f, w * 0.01f, rgb(0x2FA27A).withAlpha(0.9f))
        if (t < 0f) return
    }

    fun lamp(g: Gfx, x: Float, baseY: Float, h: Float, on: Boolean, glow: Int = rgb(0xFFD58A)) {
        g.fillRoundRect(x - h * 0.2f, baseY - 8f, x + h * 0.2f, baseY, 4f, rgb(0x55607A))
        g.line(x, baseY - 8f, x, baseY - h * 0.75f, 5f, rgb(0x55607A))
        val sh = PathData().moveTo(x - h * 0.22f, baseY - h * 0.55f).lineTo(x - h * 0.12f, baseY - h * 0.9f).lineTo(x + h * 0.12f, baseY - h * 0.9f).lineTo(x + h * 0.22f, baseY - h * 0.55f).close()
        g.fillPath(sh, if (on) glow else rgb(0x7A849C))
        if (on) SK.glow(g, x, baseY - h * 0.55f, h * 1.3f, glow, 0.35f)
    }

    fun plant(g: Gfx, x: Float, baseY: Float, h: Float) {
        g.fillRoundRect(x - h * 0.16f, baseY - h * 0.3f, x + h * 0.16f, baseY, h * 0.04f, rgb(0xB9694A))
        for (k in -2..2) {
            val a = k * 0.42f
            val p = PathData().moveTo(x, baseY - h * 0.3f).cubicTo(x + k * h * 0.12f, baseY - h * 0.6f, x + k * h * 0.18f, baseY - h * 0.85f, x + sin(a) * h * 0.4f, baseY - h * (0.7f + 0.15f * cos(a)))
                .cubicTo(x + k * h * 0.1f, baseY - h * 0.8f, x + k * h * 0.05f, baseY - h * 0.55f, x, baseY - h * 0.3f).close()
            g.fillPath(p, if (k % 2 == 0) LEAF else mix(LEAF, rgb(0x000000), 0.15f))
        }
    }

    // ───────────── Objets ─────────────

    fun jerrycan(g: Gfx, x: Float, base: Float, s: Float, color: Int = rgb(0xF2C14E), fill: Float = 1f) {
        g.fillRoundRect(x - s * 0.5f, base - s * 1.05f, x + s * 0.5f, base, s * 0.1f, color)
        g.fillRoundRect(x - s * 0.5f, base - s * 1.05f + s * 1.05f * (1f - fill), x + s * 0.5f, base, s * 0.1f, mix(color, rgb(0x5FD0E8), 0.45f))
        g.fillRoundRect(x - s * 0.45f, base - s * 1.05f, x + s * 0.05f, base - s * 0.8f, s * 0.08f, mix(color, rgb(0xFFFFFF), 0.25f))
        g.fillRoundRect(x + s * 0.12f, base - s * 1.22f, x + s * 0.42f, base - s * 1.04f, s * 0.05f, mix(color, rgb(0x000000), 0.3f))
        g.strokeRoundRect(x - s * 0.5f, base - s * 1.05f, x + s * 0.5f, base, s * 0.1f, max(2f, s * 0.04f), mix(color, rgb(0x000000), 0.35f))
        g.line(x - s * 0.18f, base - s * 0.4f, x + s * 0.18f, base - s * 0.4f, s * 0.05f, mix(color, rgb(0x000000), 0.35f))
    }

    fun crate(g: Gfx, x: Float, base: Float, w: Float, h: Float, color: Int = rgb(0xC48A4A), label: String? = null, labelColor: Int = rgb(0xFFFFFF)) {
        g.fillRoundRect(x - w / 2f, base - h, x + w / 2f, base, w * 0.05f, color)
        g.strokeRoundRect(x - w / 2f, base - h, x + w / 2f, base, w * 0.05f, max(2f, w * 0.03f), mix(color, rgb(0x000000), 0.35f))
        g.line(x - w / 2f, base - h * 0.5f, x + w / 2f, base - h * 0.5f, max(2f, w * 0.03f), mix(color, rgb(0x000000), 0.25f))
        g.line(x - w / 2f, base - h, x + w / 2f, base, max(2f, w * 0.025f), mix(color, rgb(0x000000), 0.18f))
        if (label != null) {
            val fs = min(h * 0.28f, w * 0.2f)
            g.fillRoundRect(x - w * 0.36f, base - h * 0.74f, x + w * 0.36f, base - h * 0.4f, w * 0.03f, rgb(0x1B2233).withAlpha(0.82f))
            SK.text(g, label, x, base - h * 0.4f - (h * 0.34f - fs) / 2f - fs * 0.15f, fs, labelColor, center = true)
        }
    }

    fun sack(g: Gfx, x: Float, base: Float, s: Float, color: Int = rgb(0xD9C38D)) {
        g.fillRoundRect(x - s * 0.45f, base - s * 0.9f, x + s * 0.45f, base, s * 0.28f, color)
        g.fillRoundRect(x - s * 0.3f, base - s * 1.02f, x + s * 0.3f, base - s * 0.8f, s * 0.08f, mix(color, rgb(0x000000), 0.12f))
        g.line(x - s * 0.3f, base - s * 0.55f, x + s * 0.3f, base - s * 0.55f, max(2f, s * 0.03f), mix(color, rgb(0x000000), 0.3f))
        g.fillCircle(x, base - s * 0.38f, s * 0.12f, rgb(0xC0392B))
    }

    fun shoppingBag(g: Gfx, x: Float, base: Float, s: Float, color: Int = rgb(0xE2675A)) {
        g.fillRoundRect(x - s * 0.42f, base - s * 0.85f, x + s * 0.42f, base, s * 0.06f, color)
        g.strokeCircle(x - s * 0.18f, base - s * 0.85f, s * 0.14f, max(2f, s * 0.035f), mix(color, rgb(0x000000), 0.35f))
        g.strokeCircle(x + s * 0.18f, base - s * 0.85f, s * 0.14f, max(2f, s * 0.035f), mix(color, rgb(0x000000), 0.35f))
        g.fillRoundRect(x - s * 0.2f, base - s * 0.5f, x + s * 0.2f, base - s * 0.3f, s * 0.05f, Color.white(0.55f))
    }

    fun box(g: Gfx, x: Float, base: Float, w: Float, h: Float, color: Int, ribbon: Int = rgb(0xFFFFFF)) {
        g.fillRoundRect(x - w / 2f, base - h, x + w / 2f, base, w * 0.05f, color)
        g.fillRect(x - w * 0.07f, base - h, x + w * 0.07f, base, ribbon.withAlpha(0.85f))
        g.fillRoundRect(x - w / 2f, base - h, x + w / 2f, base - h * 0.78f, w * 0.05f, mix(color, rgb(0xFFFFFF), 0.18f))
    }

    fun priceTag(g: Gfx, x: Float, y: Float, label: String, th: StageTheme, tilt: Float = -8f, size: Float = 30f) {
        g.save(); g.rotate(tilt, x, y)
        val f = SK.font(size)
        val w = g.measureText(label, f) + size * 1.4f
        g.fillRoundRect(x - w / 2f, y - size * 0.95f, x + w / 2f, y + size * 0.95f, size * 0.35f, rgb(0xFFF1B8))
        g.fillCircle(x - w / 2f + size * 0.45f, y, size * 0.17f, th.bgBottom)
        g.drawText(label, x - w / 2f + size * 0.85f, y + size * 0.36f, f, rgb(0x2A2210))
        g.restore()
    }

    /** Pictogramme de santé générique, vert : la croix rouge est un emblème protégé, jamais utilisé ici. */
    fun medicalCross(g: Gfx, x: Float, y: Float, s: Float, fg: Int = rgb(0xFFFFFF), bg: Int = rgb(0x2FA27A)) {
        g.fillRoundRect(x - s, y - s, x + s, y + s, s * 0.3f, bg)
        g.fillRoundRect(x - s * 0.22f, y - s * 0.68f, x + s * 0.22f, y + s * 0.68f, s * 0.05f, fg)
        g.fillRoundRect(x - s * 0.68f, y - s * 0.22f, x + s * 0.68f, y + s * 0.22f, s * 0.05f, fg)
    }

    fun clock(g: Gfx, x: Float, y: Float, r: Float, th: StageTheme, t: Float, color: Int = th.warn) {
        g.fillCircle(x, y, r, th.panelHi)
        g.strokeCircle(x, y, r, max(3f, r * 0.12f), color)
        val a = t / 1200f * 2f * PI.toFloat() - PI.toFloat() / 2f
        g.line(x, y, x + cos(a) * r * 0.72f, y + sin(a) * r * 0.72f, max(2f, r * 0.08f), th.ink)
        g.line(x, y, x + cos(a / 12f - 1.2f) * r * 0.45f, y + sin(a / 12f - 1.2f) * r * 0.45f, max(3f, r * 0.12f), th.ink)
    }

    fun magnifier(g: Gfx, x: Float, y: Float, r: Float, color: Int, lens: Int = Color.white(0.12f)) {
        g.line(x + r * 0.7f, y + r * 0.7f, x + r * 1.7f, y + r * 1.7f, r * 0.28f, color)
        g.fillCircle(x, y, r, lens)
        g.strokeCircle(x, y, r, r * 0.16f, color)
    }

    /** Bulle de dialogue. Retourne (gauche, haut, droite, bas) de l'intérieur ; `tailX/tailY` = point visé. */
    fun bubble(g: Gfx, l: Float, t: Float, r: Float, b: Float, tailX: Float, tailY: Float, fill: Int, line: Int, a: Float = 1f) {
        if (a <= 0.01f) return
        g.save(); if (a < 0.999f) g.multiplyAlpha(a)
        val mx = (l + r) / 2f
        val tx = tailX.coerceIn(l + 24f, r - 24f)
        val base = if (tailY > b) b else t
        val dir = if (tailY > b) 1f else -1f
        val tail = PathData().moveTo(tx - 16f, base).lineTo(tailX, tailY).lineTo(tx + 16f, base).close()
        g.fillRoundRect(l, t, r, b, 26f, fill)
        g.strokeRoundRect(l, t, r, b, 26f, 3f, line)
        g.fillPath(tail, fill)
        g.line(tx - 16f, base, tailX, tailY, 3f, line); g.line(tx + 16f, base, tailX, tailY, 3f, line)
        g.line(tx - 14f, base - dir * 0.5f, tx + 14f, base - dir * 0.5f, 5f, fill)
        g.restore()
        if (mx < 0f) return
    }

    /** Bulle de pensée : trois cercles qui montent de la tête. */
    fun thought(g: Gfx, x: Float, y: Float, w: Float, h: Float, fromX: Float, fromY: Float, fill: Int, line: Int, a: Float = 1f) {
        if (a <= 0.01f) return
        g.save(); if (a < 0.999f) g.multiplyAlpha(a)
        for (k in 0..2) {
            val u = (k + 1) / 4f
            val r = 6f + k * 5f
            g.fillCircle(lerp(fromX, x, u), lerp(fromY, y + h / 2f, u), r, fill); g.strokeCircle(lerp(fromX, x, u), lerp(fromY, y + h / 2f, u), r, 2f, line)
        }
        g.fillRoundRect(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f, min(w, h) / 2f.coerceAtLeast(1f) * 0.8f, fill)
        g.strokeRoundRect(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f, min(w, h) / 2f.coerceAtLeast(1f) * 0.8f, 3f, line)
        g.restore()
    }

    /** Spirale de pensées qui tournent (ruminations). */
    fun swirl(g: Gfx, cx: Float, cy: Float, r: Float, t: Float, color: Int, a: Float = 1f) {
        if (a <= 0.01f) return
        val p = PathData()
        val n = 60
        for (i in 0..n) {
            val u = i / n.toFloat()
            val ang = u * 5.2f * PI.toFloat() + t / 500f
            val rr = r * (0.12f + 0.88f * u)
            val x = cx + cos(ang) * rr; val y = cy + sin(ang) * rr * 0.8f
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        g.strokePath(p, 5f, color.withAlpha(a))
    }

    fun notifBadge(g: Gfx, x: Float, y: Float, n: Int, p: Float, color: Int) {
        if (p <= 0.01f) return
        g.fillCircle(x, y, 24f * p, color)
        if (p > 0.8f) SK.text(g, n.toString(), x, y + 9f, 26f, rgb(0xFFFFFF), center = true)
    }

    /** Plusieurs personnages en petit (foule) : chacun a son allure et son rythme. Retourne rien. */
    fun crowd(g: Gfx, xs: List<Float>, floor: Float, h: Float, t: Float, face: Face = Face.NEUTRAL, who0: Int = 0, mirrorFrom: Float = 1e9f, body: (Int, Float) -> Body = { i, tt -> Poses.idle(tt + i * 400f) }) {
        for ((i, x) in xs.withIndex()) Cast.draw(g, x, floor, h * (0.92f + 0.12f * SK.hash01(i, 11)), Cast.persona(who0 + i), face, body(i, t), t, who0 + i, mirror = x > mirrorFrom)
    }

    /** Petit personnage d'enfant : mêmes proportions que l'adulte, à taille réduite et visage rond. */
    fun child(g: Gfx, x: Float, floor: Float, h: Float, who: Int, face: Face, body: Body, t: Float) { Cast.draw(g, x, floor, h, Cast.persona(who), face, body, t, who) }

    // ───────────── Écrans et fenêtres ─────────────

    /** Fenêtre de courriel (fictive) : en-tête, objet, corps ; retourne (gauche, haut, droite, bas) de la zone du corps. */
    fun mailWindow(g: Gfx, l: Float, t: Float, r: Float, b: Float, th: StageTheme, from: String, subject: String, fromColor: Int = th.ink): FloatArray {
        g.fillRoundRect(l, t, r, b, 20f, th.panel)
        g.strokeRoundRect(l, t, r, b, 20f, 3f, th.line)
        g.fillRoundRect(l, t, r, t + 56f, 20f, th.panelHi)
        g.fillRect(l, t + 36f, r, t + 56f, th.panelHi)
        for (k in 0..2) g.fillCircle(l + 28f + k * 26f, t + 28f, 8f, listOf(th.bad, th.warn, th.good)[k].withAlpha(0.9f))
        SK.text(g, "De : $from", l + 28f, t + 98f, 20f, fromColor, 600, family = FontFamily.MONO)
        SK.text(g, subject, l + 28f, t + 142f, 30f, th.ink)
        g.fillRect(l + 20f, t + 160f, r - 20f, t + 163f, th.line.withAlpha(0.7f))
        return floatArrayOf(l + 28f, t + 190f, r - 28f, b - 20f)
    }

    fun textLines(g: Gfx, l: Float, t: Float, widths: FloatArray, color: Int, gap: Float = 30f, h: Float = 12f, p: Float = 1f) {
        for ((i, w) in widths.withIndex()) {
            val pp = clamp01(p * widths.size - i)
            if (pp > 0f) g.fillRoundRect(l, t + i * gap, l + w * pp, t + i * gap + h, h / 2f, color)
        }
    }

    fun avatar(g: Gfx, x: Float, y: Float, r: Float, color: Int, th: StageTheme) {
        g.fillCircle(x, y, r, color)
        g.fillCircle(x, y - r * 0.2f, r * 0.34f, Color.white(0.85f))
        val p = PathData().moveTo(x - r * 0.62f, y + r * 0.78f).cubicTo(x - r * 0.6f, y + r * 0.25f, x + r * 0.6f, y + r * 0.25f, x + r * 0.62f, y + r * 0.78f).close()
        g.save(); g.clipPath(PathData().circle(x, y, r)); g.fillPath(p, Color.white(0.85f)); g.restore()
        if (r < 0f) g.fillRect(0f, 0f, 0f, 0f, th.bgTop)
    }

    // ───────────── Gestes de scène ─────────────

    /** Regard : décale la cible d'un point (x,y) vers (tx,ty), pour que le visage suive un objet. */
    fun gaze(face: Face, fx: Float, fy: Float, tx: Float, ty: Float): Face {
        val dx = ((tx - fx) / 400f).coerceIn(-1f, 1f); val dy = ((ty - fy) / 300f).coerceIn(-1f, 1f)
        return face.copy(gazeX = dx, gazeY = dy)
    }

    fun lerpFace(a: Face, b: Face, t: Float, start: Float, dur: Float): Face = Face.mix(a, b, SK.ease(t, start, dur))

    /** Étiquette posée sous un personnage ou un objet (légende courte). */
    fun tag(g: Gfx, s: String, cx: Float, cy: Float, th: StageTheme, color: Int = th.ink, a: Float = 1f, size: Float = 24f) { SK.pill(g, s, cx, cy, color, th.bgTop.withAlpha(0.8f), size, a) }

    fun absf(x: Float) = abs(x)
}
