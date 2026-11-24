// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Tracés « à main levée » : légers tremblements, dépassements, comme au feutre.
 * Déterministes : la même graine donne toujours le même trait.
 */
internal class Sketch(seed: Int) {
    private val r = Rand(seed or 1)
    private fun j(a: Float) = r.range(-a, a)

    /** Trait légèrement courbé, dans un chemin existant (sans moveTo si `continueFrom`). */
    fun line(p: PathData, x0: Float, y0: Float, x1: Float, y1: Float, wobble: Float = 4f, move: Boolean = true): PathData {
        val len = hypot(x1 - x0, y1 - y0)
        val w = wobble * (0.6f + len / 600f).coerceAtMost(2.2f)
        if (move) p.moveTo(x0 + j(1.5f), y0 + j(1.5f))
        p.cubicTo(lerp(x0, x1, 0.33f) + j(w), lerp(y0, y1, 0.33f) + j(w), lerp(x0, x1, 0.66f) + j(w), lerp(y0, y1, 0.66f) + j(w), x1 + j(1.5f), y1 + j(1.5f))
        return p
    }

    fun line(x0: Float, y0: Float, x1: Float, y1: Float, wobble: Float = 4f) = line(PathData(), x0, y0, x1, y1, wobble)

    /** Rectangle dessiné d'un seul trait, avec un léger dépassement à la fin. */
    fun rect(l: Float, t: Float, rr: Float, b: Float, wobble: Float = 4f): PathData {
        val p = PathData().moveTo(l + j(3f), t + j(3f))
        line(p, l, t, rr, t, wobble, move = false)
        line(p, rr, t, rr, b, wobble, move = false)
        line(p, rr, b, l, b, wobble, move = false)
        line(p, l, b, l + j(3f), t + 18f, wobble, move = false)
        return p
    }

    /** Ellipse tracée en un peu plus d'un tour (le trait se chevauche, comme à la main). */
    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, turns: Float = 1.12f): PathData {
        val p = PathData()
        val start = r.range(-2.4f, -1.6f)
        val n = 28
        val ampl = 0.035f
        for (i in 0..n) {
            val a = start + (2 * PI.toFloat() * turns) * i / n
            val k = 1f + j(ampl) + 0.04f * i / n
            val x = cx + rx * k * cos(a)
            val y = cy + ry * k * sin(a)
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        return p
    }

    /** Soulignement légèrement ondulé. */
    fun underline(x0: Float, x1: Float, y: Float): PathData {
        val p = PathData().moveTo(x0 - 6f, y + j(2f))
        p.cubicTo(lerp(x0, x1, 0.3f), y + j(5f), lerp(x0, x1, 0.7f), y + j(5f), x1 + 10f, y - 4f + j(2f))
        return p
    }

    /** Flèche courbe de (x0, y0) vers (x1, y1). */
    fun arrow(x0: Float, y0: Float, x1: Float, y1: Float, bend: Float = 40f): PathData {
        val mx = (x0 + x1) / 2 + bend
        val my = (y0 + y1) / 2
        val p = PathData().moveTo(x0, y0).cubicTo(mx, my - 10f, mx, my + 10f, x1, y1)
        val ang = kotlin.math.atan2(y1 - (my + 10f), x1 - mx)
        val h = 26f
        p.moveTo(x1 - h * cos(ang - 0.5f), y1 - h * sin(ang - 0.5f)).lineTo(x1, y1).lineTo(x1 - h * cos(ang + 0.5f), y1 - h * sin(ang + 0.5f))
        return p
    }

    fun check(cx: Float, cy: Float, s: Float): PathData =
        PathData().moveTo(cx - s * 0.5f, cy + j(2f)).lineTo(cx - s * 0.12f, cy + s * 0.38f).cubicTo(cx + s * 0.05f, cy + s * 0.05f, cx + s * 0.3f, cy - s * 0.3f, cx + s * 0.62f, cy - s * 0.58f)

    fun cross(cx: Float, cy: Float, s: Float): PathData {
        val p = line(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2, 2f)
        return line(p, cx + s / 2, cy - s / 2, cx - s / 2, cy + s / 2, 2f)
    }

    /** Hachures en zigzag (pour barrer ou remplir). */
    fun scribble(l: Float, t: Float, rr: Float, b: Float, rows: Int = 3): PathData {
        val p = PathData().moveTo(l, t)
        val h = (b - t) / rows
        for (i in 0 until rows) {
            p.lineTo(rr + j(6f), t + h * (i + 0.5f) + j(4f))
            p.lineTo(l + j(6f), t + h * (i + 1) + j(4f))
        }
        return p
    }

    /** Étoile/éclat dessiné (trois petits traits) pour attirer l'œil. */
    fun spark(cx: Float, cy: Float, s: Float): PathData {
        val p = PathData()
        for (a in listOf(-2.2f, -1.57f, -0.9f)) {
            p.moveTo(cx + cos(a) * s * 0.45f, cy + sin(a) * s * 0.45f).lineTo(cx + cos(a) * s, cy + sin(a) * s)
        }
        return p
    }
}

/**
 * Suite d'actions dessinées l'une après l'autre (écrire un texte, tracer un trait…), avec la position
 * du stylo à chaque instant : c'est ce qui permet de faire suivre la main.
 */
internal class Script {
    abstract class Act {
        var start = 0f
        var dur = 0f
        abstract val natural: Float
        abstract fun draw(g: Gfx, p: Float)
        abstract fun pen(g: Gfx, p: Float): Pair<Float, Float>
        open val usesPen: Boolean = true
    }

    val acts = ArrayList<Act>()
    fun add(a: Act): Act { acts += a; return a }

    /** Place les actions dans [startMs, startMs + budgetMs] en conservant leurs proportions. */
    fun schedule(startMs: Float, budgetMs: Float, gapMs: Float = 160f) {
        val gaps = gapMs * (acts.size - 1).coerceAtLeast(0)
        val sum = acts.sumOf { it.natural.toDouble() }.toFloat().coerceAtLeast(1f)
        val k = ((budgetMs - gaps) / sum).coerceIn(0.25f, 1.6f)
        var t = startMs
        for (a in acts) { a.start = t; a.dur = a.natural * k; t += a.dur + gapMs }
    }

    val endMs: Float get() = acts.lastOrNull()?.let { it.start + it.dur } ?: 0f

    fun draw(g: Gfx, t: Float) {
        for (a in acts) {
            if (t < a.start) break
            a.draw(g, if (a.dur <= 0f) 1f else clamp01((t - a.start) / a.dur))
        }
    }

    /** Position du stylo, ou `null` s'il n'y a rien à dessiner (main hors champ). Inclut les déplacements entre actions. */
    fun pen(g: Gfx, t: Float, home: Pair<Float, Float>): Pair<Float, Float> {
        val pens = acts.filter { it.usesPen }
        if (pens.isEmpty()) return home
        val first = pens.first()
        if (t < first.start) {
            val to = first.pen(g, 0f)
            val k = Easing.EXPO_OUT.ease(clamp01((t - (first.start - 450f)) / 450f))
            return lerp(home.first, to.first, k) to lerp(home.second, to.second, k)
        }
        for ((i, a) in pens.withIndex()) {
            if (t <= a.start + a.dur) return a.pen(g, clamp01((t - a.start) / a.dur.coerceAtLeast(1f)))
            val next = pens.getOrNull(i + 1)
            if (next != null && t < next.start) {
                val from = a.pen(g, 1f)
                val to = next.pen(g, 0f)
                val k = Easing.EASE_IN_OUT.ease(clamp01((t - a.start - a.dur) / (next.start - a.start - a.dur).coerceAtLeast(1f)))
                return lerp(from.first, to.first, k) to lerp(from.second, to.second, k)
            }
        }
        val last = pens.last()
        val from = last.pen(g, 1f)
        val k = Easing.EASE_IN.ease(clamp01((t - last.start - last.dur - 250f) / 650f))
        return lerp(from.first, home.first, k) to lerp(from.second, home.second, k)
    }

    /** Écriture progressive d'un bloc (révélation de gauche à droite, ligne par ligne). */
    class Write(
        private val block: TextBlock, private val runs: List<List<Pair<String, Boolean>>>,
        private val x: Float, private val top: Float, private val color: Int, private val mark: Int,
        private val align: Align = Align.LEFT, msPerChar: Float = 42f,
    ) : Act() {
        override val natural = (block.charCount * msPerChar).coerceIn(300f, 3200f)
        private var widths: FloatArray? = null
        private fun widths(g: Gfx): FloatArray = widths ?: FloatArray(runs.size) { MarkedText.lineWidth(g, runs[it], block.font) }.also { widths = it }
        private fun lx(w: Float) = if (align == Align.CENTER) x - w / 2 else x

        override fun draw(g: Gfx, p: Float) {
            val w = widths(g)
            val total = w.sum()
            var remain = total * p
            for (i in runs.indices) {
                if (remain <= 0f) break
                val base = g.baselineOf(block, top, i)
                val l = lx(w[i])
                if (remain >= w[i]) MarkedText.drawLine(g, runs[i], l, base, block.font, color, mark)
                else {
                    g.save(); g.clipRect(l - 10f, base - block.font.size * 1.3f, l + remain, base + block.font.size * 0.6f)
                    MarkedText.drawLine(g, runs[i], l, base, block.font, color, mark); g.restore()
                }
                remain -= w[i]
            }
        }

        override fun pen(g: Gfx, p: Float): Pair<Float, Float> {
            val w = widths(g)
            var remain = w.sum() * p
            for (i in runs.indices) {
                val base = g.baselineOf(block, top, i)
                if (remain <= w[i] || i == runs.lastIndex) {
                    val wob = sin(remain * 0.09f) * block.font.size * 0.12f
                    return lx(w[i]) + remain.coerceAtMost(w[i]) to base - block.font.size * 0.25f + wob
                }
                remain -= w[i]
            }
            return x to top
        }
    }

    /** Trait tracé progressivement. */
    class Stroke(private val path: PathData, private val width: Float, private val color: Int, pxPerMs: Float = 1.5f) : Act() {
        private val poly = Polyline(path)
        override val natural = (poly.length / pxPerMs).coerceIn(220f, 1400f)
        override fun draw(g: Gfx, p: Float) = g.strokePath(if (p >= 1f) path else path.trimmed(p), width, color)
        override fun pen(g: Gfx, p: Float) = poly.pointAt(p)
    }

    /** Élément qui apparaît sans le stylo (aplat, fond de carte…). */
    class Appear(private val ms: Float, private val block: (Gfx, Float) -> Unit) : Act() {
        override val natural = ms
        override val usesPen = false
        override fun draw(g: Gfx, p: Float) = block(g, p)
        override fun pen(g: Gfx, p: Float) = 0f to 0f
    }
}
