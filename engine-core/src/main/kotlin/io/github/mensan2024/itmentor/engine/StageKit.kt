// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Boîte à outils des scènes du registre (6.1) : tout est dessiné en vectoriel par le moteur, dans un cadre virtuel de
 * 1000 × 700 que le format met à l'échelle de la place laissée par le texte. Aucune image importée : les droits sont ceux
 * du code. Les objets sont des symboles stylisés (téléphone, cadenas, serveur…), jamais des reproductions de produits ou de marques.
 * Les textes dessinés ici sont des légendes courtes en caractères latins (la police du moteur ne contient pas ✓ ni →).
 */
internal class StageTheme(
    val bgTop: Int, val bgBottom: Int, val ink: Int, val muted: Int, val accent: Int, val glow: Int,
    val good: Int = rgb(0x3DDC84), val bad: Int = rgb(0xFF4D5E), val warn: Int = rgb(0xFFB84D),
) {
    /** Surface un peu plus claire que le fond, pour les cartes et fenêtres. */
    val panel: Int get() = mix(bgBottom, ink, 0.10f)
    val panelHi: Int get() = mix(bgBottom, ink, 0.18f)
    val line: Int get() = mix(bgBottom, ink, 0.35f)
}

internal class StageCtx(
    val g: Gfx,
    /** Temps depuis le début de la scène (ms). */
    val t: Float,
    /** Temps global de la vidéo (ms) : pour les mouvements qui ne doivent pas redémarrer à chaque scène. */
    val tg: Float,
    val th: StageTheme,
    val intent: SceneIntent,
    val scene: Scene,
    val seed: Long,
    val w: Float = 1000f,
    val h: Float = 700f,
)

internal object SK {
    const val W = 1000f
    const val H = 700f

    fun font(size: Float, weight: Int = 700, family: FontFamily = FontFamily.TEXT) = FontSpec(family, weight, size)

    /** Progression 0..1 d'une boucle de `period` ms. */
    fun loop(t: Float, period: Float, offset: Float = 0f): Float { val x = ((t + offset) % period) / period; return if (x < 0f) x + 1f else x }
    fun ease(t: Float, start: Float, dur: Float, e: Easing = Easing.EASE_IN_OUT): Float = e.ease(window(t, start, dur))
    fun pop(t: Float, start: Float, dur: Float = 500f): Float = Easing.BACK_OUT.ease(window(t, start, dur))

    /** Nombre pseudo-aléatoire stable dans [0, 1[ : le même index donne toujours la même valeur (pas d'état). */
    fun hash01(i: Int, salt: Int): Float {
        var x = i * 374761393 + salt * 668265263
        x = (x xor (x ushr 13)) * 1274126177.toInt()
        x = x xor (x ushr 16)
        return (x and 0xFFFFFF) / 16777216f
    }

    // ───────────────────────────── Texte ─────────────────────────────

    fun text(g: Gfx, s: String, x: Float, base: Float, size: Float, color: Int, weight: Int = 700, center: Boolean = false, family: FontFamily = FontFamily.TEXT) {
        val f = font(size, weight, family)
        g.drawText(s, if (center) x - g.measureText(s, f) / 2f else x, base, f, color)
    }

    fun pill(g: Gfx, s: String, cx: Float, cy: Float, fg: Int, bg: Int, size: Float = 26f, alpha: Float = 1f) {
        if (alpha <= 0.01f) return
        val f = font(size)
        val w = g.measureText(s, f) + size * 1.1f
        val h = size * 1.75f
        g.save(); if (alpha < 0.999f) g.multiplyAlpha(alpha)
        g.fillRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h / 2f, bg)
        g.drawText(s, cx - w / 2f + size * 0.55f, cy + size * 0.36f, f, fg)
        g.restore()
    }

    /** Étiquette « EXEMPLE FICTIF » : toute simulation ou illustration est annoncée comme telle. */
    fun caption(g: Gfx, s: String, x: Float, y: Float, color: Int) {
        g.drawTracked(s, x, y, font(22f), color, 3f)
    }

    // ───────────────────────────── Traits et formes de base ─────────────────────────────

    fun arrow(g: Gfx, x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int, head: Float = 20f) {
        val dx = x1 - x0; val dy = y1 - y0
        val len = kotlin.math.hypot(dx, dy)
        if (len < 1f) return
        val ux = dx / len; val uy = dy / len
        g.line(x0, y0, x1 - ux * head * 0.7f, y1 - uy * head * 0.7f, width, color)
        val p = PathData().moveTo(x1, y1).lineTo(x1 - ux * head + uy * head * 0.55f, y1 - uy * head - ux * head * 0.55f)
            .lineTo(x1 - ux * head - uy * head * 0.55f, y1 - uy * head + ux * head * 0.55f).close()
        g.fillPath(p, color)
    }

    fun dashed(g: Gfx, x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int, dash: Float = 16f, gap: Float = 12f, phase: Float = 0f) {
        val len = kotlin.math.hypot(x1 - x0, y1 - y0)
        if (len < 1f) return
        val ux = (x1 - x0) / len; val uy = (y1 - y0) / len
        var s = -((phase % (dash + gap)) + (dash + gap)) % (dash + gap)
        while (s < len) {
            val a = max(0f, s); val b = min(len, s + dash)
            if (b > a) g.line(x0 + ux * a, y0 + uy * a, x0 + ux * b, y0 + uy * b, width, color)
            s += dash + gap
        }
    }

    fun glow(g: Gfx, x: Float, y: Float, r: Float, color: Int, a: Float) { if (a > 0.01f) g.fillRadialGlow(x, y, r, color.withAlpha(a)) }

    fun check(g: Gfx, cx: Float, cy: Float, s: Float, color: Int, p: Float = 1f, width: Float = s * 0.28f) {
        if (p <= 0f) return
        val ax = cx - s * 0.55f; val ay = cy + s * 0.02f
        val bx = cx - s * 0.12f; val by = cy + s * 0.45f
        val ex = cx + s * 0.6f; val ey = cy - s * 0.42f
        val first = clamp01(p / 0.4f); val second = clamp01((p - 0.4f) / 0.6f)
        g.line(ax, ay, lerp(ax, bx, first), lerp(ay, by, first), width, color)
        if (second > 0f) g.line(bx, by, lerp(bx, ex, second), lerp(by, ey, second), width, color)
    }

    fun cross(g: Gfx, cx: Float, cy: Float, s: Float, color: Int, p: Float = 1f, width: Float = s * 0.28f) {
        if (p <= 0f) return
        val a = clamp01(p / 0.5f); val b = clamp01((p - 0.5f) / 0.5f)
        g.line(cx - s * 0.5f, cy - s * 0.5f, cx - s * 0.5f + s * a, cy - s * 0.5f + s * a, width, color)
        if (b > 0f) g.line(cx + s * 0.5f, cy - s * 0.5f, cx + s * 0.5f - s * b, cy - s * 0.5f + s * b, width, color)
    }

    /** Éclatement quand un paquet est arrêté : anneau qui grandit + éclats. */
    fun burst(g: Gfx, x: Float, y: Float, r: Float, color: Int, p: Float) {
        if (p <= 0f || p >= 1f) return
        val a = 1f - p
        g.strokeCircle(x, y, r * (0.3f + 0.9f * p), 4f * a + 1f, color.withAlpha(0.9f * a))
        for (i in 0 until 8) {
            val ang = i * PI.toFloat() / 4f + 0.3f
            val r0 = r * (0.5f + 0.6f * p); val r1 = r0 + r * 0.35f * a
            g.line(x + cos(ang) * r0, y + sin(ang) * r0, x + cos(ang) * r1, y + sin(ang) * r1, 3f, color.withAlpha(0.8f * a))
        }
    }

    // ───────────────────────────── Objets ─────────────────────────────

    fun packet(g: Gfx, x: Float, y: Float, s: Float, color: Int, a: Float = 1f) {
        if (a <= 0.01f) return
        g.fillCircle(x, y, s * 1.6f, color.withAlpha(0.18f * a))
        g.fillRoundRect(x - s, y - s, x + s, y + s, s * 0.3f, color.withAlpha(a))
        g.fillRoundRect(x - s * 0.45f, y - s * 0.45f, x + s * 0.45f, y + s * 0.45f, s * 0.15f, Color.white(0.55f * a))
    }

    fun envelope(g: Gfx, cx: Float, cy: Float, w: Float, fill: Int, line: Int, a: Float = 1f) {
        if (a <= 0.01f) return
        val h = w * 0.68f
        g.save(); if (a < 0.999f) g.multiplyAlpha(a)
        g.fillRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, w * 0.08f, fill)
        g.strokeRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, w * 0.08f, max(2f, w * 0.04f), line)
        g.line(cx - w / 2f + 3f, cy - h / 2f + 4f, cx, cy + h * 0.08f, max(2f, w * 0.035f), line)
        g.line(cx + w / 2f - 3f, cy - h / 2f + 4f, cx, cy + h * 0.08f, max(2f, w * 0.035f), line)
        g.restore()
    }

    /** Téléphone vu de face. L'écran est (cx − w/2 + 12, cy − h/2 + 44) → (cx + w/2 − 12, cy + h/2 − 28). */
    fun phone(g: Gfx, cx: Float, cy: Float, w: Float, h: Float, th: StageTheme, screen: Int = th.panelHi) {
        g.fillRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, w * 0.13f, th.line)
        g.fillRoundRect(cx - w / 2f + 5f, cy - h / 2f + 5f, cx + w / 2f - 5f, cy + h / 2f - 5f, w * 0.115f, th.bgBottom)
        g.fillRoundRect(cx - w / 2f + 12f, cy - h / 2f + 44f, cx + w / 2f - 12f, cy + h / 2f - 28f, w * 0.06f, screen)
        g.fillRoundRect(cx - w * 0.14f, cy - h / 2f + 16f, cx + w * 0.14f, cy - h / 2f + 26f, 5f, th.line)
    }

    fun laptop(g: Gfx, cx: Float, cy: Float, w: Float, th: StageTheme, screen: Int = th.panelHi) {
        val h = w * 0.62f
        g.fillRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, w * 0.03f, th.line)
        g.fillRoundRect(cx - w / 2f + 8f, cy - h / 2f + 8f, cx + w / 2f - 8f, cy + h / 2f - 8f, w * 0.02f, screen)
        g.fillRoundRect(cx - w * 0.58f, cy + h / 2f, cx + w * 0.58f, cy + h / 2f + w * 0.035f, w * 0.02f, th.line)
    }

    fun pc(g: Gfx, cx: Float, cy: Float, s: Float, th: StageTheme, color: Int = th.line, screen: Int = th.panelHi) {
        g.fillRoundRect(cx - s * 0.7f, cy - s * 0.5f, cx + s * 0.7f, cy + s * 0.4f, s * 0.1f, color)
        g.fillRoundRect(cx - s * 0.6f, cy - s * 0.4f, cx + s * 0.6f, cy + s * 0.3f, s * 0.06f, screen)
        g.fillRect(cx - s * 0.08f, cy + s * 0.4f, cx + s * 0.08f, cy + s * 0.62f, color)
        g.fillRoundRect(cx - s * 0.35f, cy + s * 0.6f, cx + s * 0.35f, cy + s * 0.72f, s * 0.04f, color)
    }

    fun server(g: Gfx, cx: Float, cy: Float, w: Float, h: Float, th: StageTheme, tg: Float, accent: Int = th.accent) {
        val rows = 3
        val rh = h / rows
        for (i in 0 until rows) {
            val top = cy - h / 2f + i * rh + 3f
            g.fillRoundRect(cx - w / 2f, top, cx + w / 2f, top + rh - 6f, 8f, th.line)
            g.fillRoundRect(cx - w / 2f + 6f, top + 6f, cx + w / 2f - 6f, top + rh - 12f, 5f, th.bgBottom)
            for (k in 0 until 3) {
                val on = hash01(i * 7 + k, 11) + 0.5f * sin(tg / (300f + 90f * k) + i + k) > 0.55f
                g.fillCircle(cx - w / 2f + 22f + k * 18f, top + rh / 2f - 3f, 4.5f, if (on) accent else th.line)
            }
            g.fillRoundRect(cx + w / 2f - 70f, top + rh / 2f - 9f, cx + w / 2f - 18f, top + rh / 2f + 3f, 3f, th.line)
        }
    }

    fun cloud(g: Gfx, cx: Float, cy: Float, s: Float, color: Int) {
        g.fillCircle(cx - s * 0.55f, cy + s * 0.1f, s * 0.42f, color)
        g.fillCircle(cx - s * 0.1f, cy - s * 0.18f, s * 0.55f, color)
        g.fillCircle(cx + s * 0.5f, cy + s * 0.05f, s * 0.45f, color)
        g.fillRoundRect(cx - s * 0.95f, cy + s * 0.05f, cx + s * 0.95f, cy + s * 0.52f, s * 0.24f, color)
    }

    fun lock(g: Gfx, cx: Float, cy: Float, s: Float, body: Int, shackle: Int, open: Float = 0f, shake: Float = 0f) {
        val ox = shake
        val lift = open * s * 0.32f
        g.strokeArc(cx + ox, cy - s * 0.1f - lift, s * 0.38f, 180f, 180f, s * 0.15f, shackle)
        g.line(cx + ox - s * 0.38f, cy - s * 0.1f - lift, cx + ox - s * 0.38f, cy - s * 0.1f + (if (open > 0.5f) -s * 0.04f else s * 0.05f), s * 0.15f, shackle)
        g.line(cx + ox + s * 0.38f, cy - s * 0.1f - lift, cx + ox + s * 0.38f, cy - s * 0.1f + s * 0.05f, s * 0.15f, shackle)
        g.fillRoundRect(cx + ox - s * 0.62f, cy - s * 0.12f, cx + ox + s * 0.62f, cy + s * 0.78f, s * 0.14f, body)
        g.fillCircle(cx + ox, cy + s * 0.28f, s * 0.11f, Color.black(0.45f))
        g.fillRoundRect(cx + ox - s * 0.04f, cy + s * 0.3f, cx + ox + s * 0.04f, cy + s * 0.52f, s * 0.03f, Color.black(0.45f))
    }

    fun shield(g: Gfx, cx: Float, cy: Float, s: Float, fill: Int, stroke: Int, check: Float = 0f) {
        val p = PathData().moveTo(cx, cy - s).lineTo(cx + s * 0.8f, cy - s * 0.65f).lineTo(cx + s * 0.8f, cy + s * 0.05f)
            .cubicTo(cx + s * 0.8f, cy + s * 0.55f, cx + s * 0.35f, cy + s * 0.8f, cx, cy + s)
            .cubicTo(cx - s * 0.35f, cy + s * 0.8f, cx - s * 0.8f, cy + s * 0.55f, cx - s * 0.8f, cy + s * 0.05f)
            .lineTo(cx - s * 0.8f, cy - s * 0.65f).close()
        g.fillPath(p, fill)
        g.strokePath(p, max(3f, s * 0.07f), stroke)
        if (check > 0f) check(g, cx, cy + s * 0.02f, s * 0.5f, stroke, check)
    }

    /** Silhouette à capuche : l'attaquant. Stylisée, sans visage reconnaissable. */
    fun hood(g: Gfx, cx: Float, cy: Float, s: Float, color: Int, eye: Int) {
        val body = PathData().moveTo(cx - s * 1.0f, cy + s * 1.5f).cubicTo(cx - s * 1.0f, cy + s * 0.5f, cx - s * 0.9f, cy + s * 0.25f, cx - s * 0.5f, cy + s * 0.1f)
            .lineTo(cx + s * 0.5f, cy + s * 0.1f).cubicTo(cx + s * 0.9f, cy + s * 0.25f, cx + s * 1.0f, cy + s * 0.5f, cx + s * 1.0f, cy + s * 1.5f).close()
        g.fillPath(body, color)
        val head = PathData().moveTo(cx - s * 0.62f, cy + s * 0.35f).cubicTo(cx - s * 0.75f, cy - s * 0.6f, cx - s * 0.35f, cy - s * 1.15f, cx, cy - s * 1.15f)
            .cubicTo(cx + s * 0.35f, cy - s * 1.15f, cx + s * 0.75f, cy - s * 0.6f, cx + s * 0.62f, cy + s * 0.35f)
            .cubicTo(cx + s * 0.3f, cy + s * 0.5f, cx - s * 0.3f, cy + s * 0.5f, cx - s * 0.62f, cy + s * 0.35f).close()
        g.fillPath(head, color)
        g.fillRoundRect(cx - s * 0.42f, cy - s * 0.55f, cx + s * 0.42f, cy + s * 0.25f, s * 0.3f, Color.black(0.55f))
        g.fillRoundRect(cx - s * 0.3f, cy - s * 0.18f, cx - s * 0.1f, cy - s * 0.08f, s * 0.05f, eye)
        g.fillRoundRect(cx + s * 0.1f, cy - s * 0.18f, cx + s * 0.3f, cy - s * 0.08f, s * 0.05f, eye)
    }

    /** Silhouette d'utilisateur (tête + épaules). */
    fun person(g: Gfx, cx: Float, cy: Float, s: Float, color: Int) {
        g.fillCircle(cx, cy - s * 0.55f, s * 0.48f, color)
        val sh = PathData().moveTo(cx - s * 0.95f, cy + s * 1.2f).cubicTo(cx - s * 0.95f, cy + s * 0.3f, cx - s * 0.5f, cy + s * 0.05f, cx, cy + s * 0.05f)
            .cubicTo(cx + s * 0.5f, cy + s * 0.05f, cx + s * 0.95f, cy + s * 0.3f, cx + s * 0.95f, cy + s * 1.2f).close()
        g.fillPath(sh, color)
    }

    /** Pointeur de souris / doigt. */
    fun cursor(g: Gfx, x: Float, y: Float, s: Float, fill: Int, edge: Int) {
        val p = PathData().moveTo(x, y).lineTo(x, y + s * 1.5f).lineTo(x + s * 0.38f, y + s * 1.14f).lineTo(x + s * 0.68f, y + s * 1.8f)
            .lineTo(x + s * 0.92f, y + s * 1.68f).lineTo(x + s * 0.62f, y + s * 1.04f).lineTo(x + s * 1.08f, y + s * 1.0f).close()
        g.fillPath(p, fill)
        g.strokePath(p, 2.5f, edge)
    }

    /** Fenêtre de navigateur ; `url` est tapé jusqu'à `urlP` (0..1). Retourne le haut du contenu. */
    fun browser(g: Gfx, l: Float, t: Float, r: Float, b: Float, th: StageTheme, url: String, urlP: Float = 1f, urlColor: Int = th.ink, secure: Boolean? = null): Float {
        g.fillRoundRect(l, t, r, b, 20f, th.panel)
        g.strokeRoundRect(l, t, r, b, 20f, 3f, th.line)
        g.fillRoundRect(l + 14f, t + 12f, r - 14f, t + 62f, 25f, th.bgBottom)
        for (i in 0 until 3) g.fillCircle(l + 40f + i * 22f, t + 87f, 6f, th.line)
        val shown = url.take((url.length * clamp01(urlP)).toInt())
        val f = font(26f, 400, FontFamily.MONO)
        val tx = l + 66f
        g.drawText(shown, tx, t + 46f, f, urlColor)
        if (urlP in 0.01f..0.99f) g.fillRect(tx + g.measureText(shown, f) + 2f, t + 25f, tx + g.measureText(shown, f) + 5f, t + 52f, th.ink)
        when (secure) {
            true -> lock(g, l + 40f, t + 33f, 20f, th.good, th.good)
            false -> g.fillCircle(l + 40f, t + 37f, 9f, th.warn)
            null -> {}
        }
        return t + 100f
    }

    /** Fenêtre de terminal avec des lignes (déjà tronquées par l'appelant). */
    fun terminal(g: Gfx, l: Float, t: Float, r: Float, b: Float, th: StageTheme) {
        g.fillRoundRect(l, t, r, b, 18f, rgb(0x0A0F18))
        g.strokeRoundRect(l, t, r, b, 18f, 3f, th.line)
        g.fillRoundRect(l, t, r, t + 44f, 18f, th.panelHi)
        g.fillRect(l, t + 30f, r, t + 44f, th.panelHi)
        for (i in 0 until 3) g.fillCircle(l + 28f + i * 22f, t + 22f, 6f, if (i == 0) th.bad else if (i == 1) th.warn else th.good)
    }

    /** Commutateur : boîtier plat avec diodes qui clignotent. */
    fun switchBox(g: Gfx, cx: Float, cy: Float, w: Float, th: StageTheme, tg: Float, body: Int = th.line, accent: Int = th.accent) {
        val h = w * 0.3f
        g.fillRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h * 0.22f, body)
        g.fillRoundRect(cx - w / 2f + 8f, cy - h / 2f + 8f, cx + w / 2f - 8f, cy + h / 2f - 8f, h * 0.15f, th.bgBottom)
        val n = 6
        for (i in 0 until n) {
            val px = cx - w / 2f + 26f + i * (w - 52f) / (n - 1)
            g.fillRoundRect(px - 9f, cy - 8f, px + 9f, cy + 11f, 3f, th.line)
            val on = (sin(tg / 260f + i * 1.7f) > 0.1f)
            g.fillCircle(px, cy - h / 2f + 17f, 3.5f, if (on) accent else th.line)
        }
    }

    fun router(g: Gfx, cx: Float, cy: Float, w: Float, th: StageTheme, tg: Float, accent: Int = th.accent) {
        val h = w * 0.34f
        g.fillRoundRect(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f, h * 0.3f, th.line)
        g.fillRoundRect(cx - w / 2f + 8f, cy - h / 2f + 8f, cx + w / 2f - 8f, cy + h / 2f - 8f, h * 0.24f, th.bgBottom)
        for (k in 0..1) {
            val ax = cx + (if (k == 0) -1 else 1) * w * 0.3f
            g.line(ax, cy - h / 2f, ax + (if (k == 0) -1 else 1) * 12f, cy - h / 2f - h * 0.8f, 6f, th.line)
        }
        for (i in 0 until 4) g.fillCircle(cx - w * 0.28f + i * w * 0.19f, cy, 5f, if (sin(tg / 300f + i * 2.1f) > 0f) accent else th.line)
    }

    /** Rayons de soleil ou de lumière derrière un point. */
    fun rays(g: Gfx, cx: Float, cy: Float, r0: Float, r1: Float, n: Int, rot: Float, color: Int, width: Float = 6f) {
        for (i in 0 until n) {
            val a = rot + i * 2f * PI.toFloat() / n
            g.line(cx + cos(a) * r0, cy + sin(a) * r0, cx + cos(a) * r1, cy + sin(a) * r1, width, color)
        }
    }

    /** Étoile à quatre branches (étincelle). */
    fun spark(g: Gfx, cx: Float, cy: Float, s: Float, color: Int) {
        val p = PathData().moveTo(cx, cy - s).cubicTo(cx + s * 0.1f, cy - s * 0.1f, cx + s * 0.1f, cy - s * 0.1f, cx + s, cy)
            .cubicTo(cx + s * 0.1f, cy + s * 0.1f, cx + s * 0.1f, cy + s * 0.1f, cx, cy + s)
            .cubicTo(cx - s * 0.1f, cy + s * 0.1f, cx - s * 0.1f, cy + s * 0.1f, cx - s, cy)
            .cubicTo(cx - s * 0.1f, cy - s * 0.1f, cx - s * 0.1f, cy - s * 0.1f, cx, cy - s).close()
        g.fillPath(p, color)
    }

    fun heart(g: Gfx, cx: Float, cy: Float, s: Float, color: Int) {
        val p = PathData().moveTo(cx, cy + s * 0.9f)
            .cubicTo(cx - s * 1.5f, cy - s * 0.1f, cx - s * 0.7f, cy - s * 1.1f, cx, cy - s * 0.35f)
            .cubicTo(cx + s * 0.7f, cy - s * 1.1f, cx + s * 1.5f, cy - s * 0.1f, cx, cy + s * 0.9f).close()
        g.fillPath(p, color)
    }
}

internal object Color {
    fun white(a: Float): Int = rgb(0xFFFFFF).withAlpha(a)
    fun black(a: Float): Int = rgb(0x000000).withAlpha(a)
}
