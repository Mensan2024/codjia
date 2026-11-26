// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.FontCatalog
import io.github.mensan2024.itmentor.engine.FontFamily
import io.github.mensan2024.itmentor.engine.FontSpec
import io.github.mensan2024.itmentor.engine.Gfx
import io.github.mensan2024.itmentor.engine.Metrics
import io.github.mensan2024.itmentor.engine.PathData
import kotlinx.browser.document
import kotlin.math.PI

/** Polices du moteur (les mêmes fichiers que l'application Android), chargées avec l'API FontFace. */
object WebFonts {
    fun family(spec: FontSpec): String = "itm-" + FontCatalog.file(spec.family, spec.weight).substringBeforeLast('.')

    private var all: kotlin.js.Promise<dynamic>? = null

    /** Lance le chargement de toutes les polices en parallèle (sans bloquer l'interface). */
    fun start(base: String) {
        if (all != null) return
        val files = FontFamily.entries.flatMap { f -> FontCatalog.FILES.getValue(f).values }.distinct()
        val loads = files.map { file ->
            val name = "itm-" + file.substringBeforeLast('.')
            val url = "url(${base}assets/fonts/$file)"
            val face: dynamic = js("new FontFace(name, url)")
            (face.load() as kotlin.js.Promise<dynamic>).then { loaded ->
                document.asDynamic().fonts.add(loaded); Unit
            }
        }
        all = kotlin.js.Promise.all(loads.toTypedArray()).then { Unit } as kotlin.js.Promise<dynamic>
    }

    /** Attend que toutes les polices soient prêtes (aperçu et export). */
    suspend fun ready() { all?.await() }

}

/**
 * Implémentation navigateur de Gfx sur un canevas 2D. Même convention que la version Android :
 * opacité propre (pile synchronisée avec save/restore), calques hors écran pour les fondus.
 */
class CanvasGfx : Gfx {
    private var ctx: dynamic = null
    private var width = 0
    private var height = 0
    private var alpha = 1.0
    private val alphaStack = ArrayList<Double>()

    private class Layer(val ctx: dynamic, val canvas: dynamic, val alpha: Double, val prevAlpha: Double)
    private val layers = ArrayList<Layer>()
    private val pool = ArrayList<dynamic>()
    private val metricsCache = HashMap<String, Metrics>()
    private val colorCache = HashMap<Long, String>()

    /** À appeler avant chaque image. `scale` = taille réelle / 1080 (le moteur dessine en 1080 × 1920). */
    fun begin(canvas: dynamic) {
        ctx = canvas.getContext("2d")
        width = canvas.width as Int
        height = canvas.height as Int
        alpha = 1.0
        alphaStack.clear()
        layers.clear()
        ctx.setTransform(1, 0, 0, 1, 0, 0)
        ctx.globalAlpha = 1
        ctx.fillStyle = "#000"
        ctx.fillRect(0, 0, width, height)
        ctx.lineCap = "round"
        ctx.lineJoin = "round"
        ctx.textBaseline = "alphabetic"
    }

    private fun css(c: Int, extra: Double = 1.0): String {
        val a = ((c ushr 24) and 0xFF) / 255.0 * alpha * extra
        val key = ((c.toLong() and 0xFFFFFFFFL) shl 8) or (kotlin.math.round(a * 255).toLong() and 0xFF)
        return colorCache.getOrPut(key) {
            val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
            "rgba($r,$g,$b,${kotlin.math.round(a * 1000) / 1000.0})"
        }
    }

    override fun save() { ctx.save(); alphaStack.add(alpha) }
    override fun restore() { ctx.restore(); alpha = alphaStack.removeAt(alphaStack.lastIndex) }
    override fun translate(dx: Float, dy: Float) { ctx.translate(dx, dy) }
    override fun scale(s: Float, px: Float, py: Float) { ctx.translate(px, py); ctx.scale(s, s); ctx.translate(-px, -py) }
    override fun rotate(degrees: Float, px: Float, py: Float) { ctx.translate(px, py); ctx.rotate(degrees * PI / 180); ctx.translate(-px, -py) }
    override fun clipRect(l: Float, t: Float, r: Float, b: Float) { ctx.beginPath(); ctx.rect(l, t, r - l, b - t); ctx.clip() }
    override fun clipPath(path: PathData) { ctx.clip(path2d(path)) }
    override fun multiplyAlpha(a: Float) { alpha *= a }

    override fun beginLayer(alpha: Float) {
        val canvas: dynamic = if (pool.size > layers.size) pool[layers.size] else {
            val c: dynamic = document.createElement("canvas"); pool.add(c); c
        }
        if (canvas.width != width) canvas.width = width
        if (canvas.height != height) canvas.height = height
        val lc: dynamic = canvas.getContext("2d")
        lc.setTransform(1, 0, 0, 1, 0, 0)
        lc.clearRect(0, 0, width, height)
        lc.setTransform(ctx.getTransform())
        lc.lineCap = "round"; lc.lineJoin = "round"; lc.textBaseline = "alphabetic"
        layers.add(Layer(ctx, canvas, alpha.toDouble() * this.alpha, this.alpha))
        ctx = lc
        this.alpha = 1.0
    }

    override fun endLayer() {
        val l = layers.removeAt(layers.lastIndex)
        ctx = l.ctx
        ctx.save()
        ctx.setTransform(1, 0, 0, 1, 0, 0)
        ctx.globalAlpha = l.alpha
        ctx.drawImage(l.canvas, 0, 0)
        ctx.restore()
        alpha = l.prevAlpha
    }

    override fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int) {
        ctx.fillStyle = css(color); ctx.fillRect(l, t, r - l, b - t)
    }

    override fun fillVerticalGradient(l: Float, t: Float, r: Float, b: Float, top: Int, bottom: Int) {
        val g: dynamic = ctx.createLinearGradient(0, t, 0, b)
        g.addColorStop(0, css(top)); g.addColorStop(1, css(bottom))
        ctx.fillStyle = g; ctx.fillRect(l, t, r - l, b - t)
    }

    override fun fillRadialGlow(cx: Float, cy: Float, radius: Float, color: Int) {
        if (radius <= 0f) return
        val g: dynamic = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius)
        g.addColorStop(0, css(color)); g.addColorStop(0.45, css(color, 0.35)); g.addColorStop(1, css(color and 0x00FFFFFF))
        ctx.fillStyle = g
        ctx.beginPath(); ctx.arc(cx, cy, radius, 0, 2 * PI); ctx.fill()
    }

    private fun roundPath(l: Float, t: Float, r: Float, b: Float, radius: Float) {
        val rr = minOf(radius, (r - l) / 2, (b - t) / 2).coerceAtLeast(0f)
        ctx.beginPath()
        ctx.moveTo(l + rr, t)
        ctx.arcTo(r, t, r, b, rr); ctx.arcTo(r, b, l, b, rr); ctx.arcTo(l, b, l, t, rr); ctx.arcTo(l, t, r, t, rr)
        ctx.closePath()
    }

    override fun fillRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        if (r <= l || b <= t) return
        roundPath(l, t, r, b, radius); ctx.fillStyle = css(color); ctx.fill()
    }

    override fun strokeRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, width: Float, color: Int) {
        if (r <= l || b <= t) return
        roundPath(l, t, r, b, radius); ctx.lineWidth = width; ctx.strokeStyle = css(color); ctx.stroke()
    }

    override fun fillCircle(cx: Float, cy: Float, r: Float, color: Int) {
        if (r <= 0f) return
        ctx.beginPath(); ctx.arc(cx, cy, r, 0, 2 * PI); ctx.fillStyle = css(color); ctx.fill()
    }

    override fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float, color: Int) {
        if (r <= 0f) return
        ctx.beginPath(); ctx.arc(cx, cy, r, 0, 2 * PI); ctx.lineWidth = width; ctx.strokeStyle = css(color); ctx.stroke()
    }

    override fun strokeArc(cx: Float, cy: Float, r: Float, startDeg: Float, sweepDeg: Float, width: Float, color: Int) {
        if (r <= 0f || sweepDeg == 0f) return
        ctx.beginPath()
        ctx.arc(cx, cy, r, startDeg * PI / 180, (startDeg + sweepDeg) * PI / 180, sweepDeg < 0)
        ctx.lineWidth = width; ctx.strokeStyle = css(color); ctx.stroke()
    }

    override fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {
        ctx.beginPath(); ctx.moveTo(x0, y0); ctx.lineTo(x1, y1); ctx.lineWidth = width; ctx.strokeStyle = css(color); ctx.stroke()
    }

    private fun path2d(p: PathData): dynamic {
        val path: dynamic = js("new Path2D()")
        for (c in p.cmds) when (c) {
            is PathData.Move -> path.moveTo(c.x, c.y)
            is PathData.Line -> path.lineTo(c.x, c.y)
            is PathData.Cubic -> path.bezierCurveTo(c.x1, c.y1, c.x2, c.y2, c.x, c.y)
            PathData.Close -> path.closePath()
        }
        return path
    }

    override fun fillPath(path: PathData, color: Int) { ctx.fillStyle = css(color); ctx.fill(path2d(path)) }

    override fun strokePath(path: PathData, width: Float, color: Int) {
        if (path.cmds.isEmpty()) return
        ctx.lineWidth = width; ctx.strokeStyle = css(color); ctx.stroke(path2d(path))
    }

    private fun fontCss(f: FontSpec) = "${f.size}px \"${WebFonts.family(f)}\""

    override fun measureText(text: String, font: FontSpec): Float {
        ctx.font = fontCss(font)
        return (ctx.measureText(text).width as Double).toFloat()
    }

    override fun metrics(font: FontSpec): Metrics = metricsCache.getOrPut(fontCss(font)) {
        ctx.font = fontCss(font)
        val h: dynamic = ctx.measureText("H")
        val ascent = (h.actualBoundingBoxAscent as? Double)?.toFloat() ?: (font.size * 0.72f)
        val g: dynamic = ctx.measureText("gjpqy")
        val descent = (g.fontBoundingBoxDescent as? Double)?.toFloat()
            ?: (g.actualBoundingBoxDescent as? Double)?.toFloat() ?: (font.size * 0.24f)
        Metrics(ascent = ascent, descent = descent)
    }

    override fun drawText(text: String, x: Float, baseline: Float, font: FontSpec, color: Int) {
        ctx.font = fontCss(font); ctx.fillStyle = css(color); ctx.fillText(text, x, baseline)
    }
}
