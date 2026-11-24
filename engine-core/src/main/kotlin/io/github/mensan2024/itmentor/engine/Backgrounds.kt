// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fonds animés. Ils dépendent du temps GLOBAL : pendant une transition, les deux scènes
 * montrent le même décor au même endroit, ce qui rend le raccord invisible.
 */
internal object Backgrounds {
    private const val W = VideoFormat.DESIGN_WIDTH
    private const val H = VideoFormat.DESIGN_HEIGHT

    /** Pistes de « circuit » (coordonnées en cases de grille), calculées une fois. */
    private val traces: List<PathData> = run {
        val r = Rand(2024)
        val cell = 72f
        List(6) {
            var x = (r.range(1f, 14f)).toInt() * cell
            var y = (r.range(2f, 25f)).toInt() * cell
            val p = PathData().moveTo(x, y)
            repeat(5) { k ->
                if (k % 2 == 0) x += (if (r.next() > 0.5f) 1 else -1) * (r.range(2f, 5f)).toInt() * cell
                else y += (if (r.next() > 0.5f) 1 else -1) * (r.range(2f, 5f)).toInt() * cell
                p.lineTo(x.coerceIn(cell, W - cell), y.coerceIn(cell, H - cell))
            }
            p
        }
    }

    fun draw(g: Gfx, style: Style, sceneBg: Int?, tGlobalMs: Float) {
        val top = sceneBg ?: style.bgTop
        g.fillVerticalGradient(0f, 0f, W, H, top, style.bgBottom)

        // Deux halos qui dérivent lentement (trajectoires de Lissajous).
        val t = tGlobalMs
        g.fillRadialGlow(540f + 230f * sin(t * 0.00021f), 560f + 150f * cos(t * 0.00017f), 640f, style.accent.withAlpha(0.15f))
        g.fillRadialGlow(320f + 180f * cos(t * 0.00013f), 1520f + 120f * sin(t * 0.00019f), 560f, style.highlight.withAlpha(0.07f))

        when (style.decor) {
            Decor.CIRCUIT_GRID -> circuit(g, style, t)
            Decor.PULSE -> pulse(g, style, t)
            Decor.FIELD_ROWS -> fields(g, style, t)
        }
    }

    private fun circuit(g: Gfx, style: Style, t: Float) {
        val cell = 72f
        val drift = (t * 0.010f) % cell
        val dot = style.text.withAlpha(0.06f)
        var y = -drift
        while (y < H + cell) {
            var x = cell / 2
            while (x < W) { g.fillCircle(x, y, 2.4f, dot); x += cell }
            y += cell
        }
        // Pistes fixes + une impulsion lumineuse qui les parcourt.
        for ((i, p) in traces.withIndex()) {
            g.strokePath(p, 2.5f, style.accent.withAlpha(0.05f))
            val period = 5200f + i * 700f
            val phase = ((t + i * 900f) % period) / period
            g.strokePath(p.segment(phase - 0.10f, phase), 3.5f, style.accent.withAlpha(0.32f))
        }
    }

    private fun pulse(g: Gfx, style: Style, t: Float) {
        // Ondes concentriques qui s'élargissent.
        for (k in 0 until 4) {
            val ph = ((t / 4200f) + k / 4f) % 1f
            g.strokeCircle(540f, 640f, 140f + ph * 820f, 3f, style.accent.withAlpha(0.13f * (1f - ph)))
        }
        // Tracé d'électrocardiogramme qui défile.
        val p = PathData()
        val baseY = 1520f
        val period = 360f
        val off = (t * 0.12f) % period
        var x = -period - off
        p.moveTo(x, baseY)
        while (x < W + period) {
            p.lineTo(x + 120f, baseY); p.lineTo(x + 150f, baseY - 26f); p.lineTo(x + 175f, baseY + 34f)
            p.lineTo(x + 200f, baseY - 120f); p.lineTo(x + 228f, baseY + 60f); p.lineTo(x + 252f, baseY); p.lineTo(x + period, baseY)
            x += period
        }
        g.strokePath(p, 3f, style.accent.withAlpha(0.16f))
    }

    private fun fields(g: Gfx, style: Style, t: Float) {
        // Sillons en perspective qui avancent vers le spectateur.
        val vx = 540f
        val vy = 1080f
        for (i in -9..9) {
            g.line(vx, vy, vx + i * 190f, H + 40f, 2.5f, style.accent.withAlpha(0.07f))
        }
        val phase = (t / 2600f) % 1f
        for (k in 0 until 7) {
            val f = ((k + phase) / 7f)
            val y = vy + (H - vy) * f * f
            g.line(0f, y, W, y, 2f, style.accent.withAlpha(0.05f + 0.06f * f))
        }
        // Soleil qui monte très lentement.
        val sy = 330f - 40f * sin((t / 40000f) * PI.toFloat())
        g.fillRadialGlow(820f, sy, 260f, style.highlight.withAlpha(0.18f))
        g.fillCircle(820f, sy, 62f, style.highlight.withAlpha(0.35f))
    }
}
