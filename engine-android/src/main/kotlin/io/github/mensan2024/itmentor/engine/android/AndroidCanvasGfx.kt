// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.android

import android.content.res.AssetManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import io.github.mensan2024.itmentor.engine.FontFamily
import io.github.mensan2024.itmentor.engine.FontSpec
import io.github.mensan2024.itmentor.engine.Gfx
import io.github.mensan2024.itmentor.engine.Metrics
import io.github.mensan2024.itmentor.engine.PathData
import kotlin.math.abs

/** Polices embarquées dans les assets du module (licence OFL) : même rendu sur tous les téléphones. */
class AndroidFonts(assets: AssetManager) {
    private val files = mapOf(
        FontFamily.TEXT to mapOf(400 to "Inter-Regular.otf", 500 to "Inter-Medium.otf", 600 to "Inter-SemiBold.otf",
            700 to "Inter-Bold.otf", 800 to "Inter-ExtraBold.otf"),
        FontFamily.DISPLAY to mapOf(800 to "InterDisplay-ExtraBold.otf", 900 to "InterDisplay-Black.otf"),
        FontFamily.MONO to mapOf(400 to "JetBrainsMono-Regular.ttf", 700 to "JetBrainsMono-Bold.ttf"),
    )
    private val faces: Map<Pair<FontFamily, Int>, Typeface> = files.flatMap { (fam, byW) ->
        byW.map { (w, name) -> (fam to w) to Typeface.createFromAsset(assets, "fonts/$name") }
    }.toMap()

    fun get(spec: FontSpec): Typeface {
        val w = files.getValue(spec.family).keys.minByOrNull { abs(it - spec.weight) }!!
        return faces.getValue(spec.family to w)
    }
}

/**
 * Implémentation Android de Gfx sur android.graphics.Canvas.
 * Le moteur (engine-core) ne connaît que l'interface Gfx : il est identique à celui testé sur JVM.
 * Non thread-safe : une instance par fil de rendu.
 */
class AndroidCanvasGfx(private val fonts: AndroidFonts) : Gfx {

    lateinit var canvas: Canvas

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply { isLinearText = true }
    private val path = Path()
    private val bounds = Rect()
    private val metricsCache = HashMap<FontSpec, Metrics>()

    private var alpha = 1f
    private val alphaStack = ArrayDeque<Float>()
    private val layerStack = ArrayDeque<Float>()

    /** À appeler avant chaque image. */
    fun begin(target: Canvas) {
        canvas = target
        alpha = 1f
        alphaStack.clear()
        layerStack.clear()
    }

    private fun Paint.argb(c: Int): Paint {
        shader = null
        color = c
        this.alpha = (Color.alpha(c) * this@AndroidCanvasGfx.alpha).toInt().coerceIn(0, 255)
        return this
    }

    override fun save() { canvas.save(); alphaStack.addLast(alpha) }
    override fun restore() { canvas.restore(); alpha = alphaStack.removeLast() }
    override fun translate(dx: Float, dy: Float) = canvas.translate(dx, dy)
    override fun scale(s: Float, px: Float, py: Float) = canvas.scale(s, s, px, py)
    override fun rotate(degrees: Float, px: Float, py: Float) = canvas.rotate(degrees, px, py)
    override fun clipRect(l: Float, t: Float, r: Float, b: Float) { canvas.clipRect(l, t, r, b) }
    override fun multiplyAlpha(a: Float) { alpha *= a }

    override fun beginLayer(alpha: Float) {
        layerStack.addLast(this.alpha)
        canvas.saveLayerAlpha(null, (alpha * this.alpha * 255f).toInt().coerceIn(0, 255))
        this.alpha = 1f
    }

    override fun endLayer() {
        canvas.restore()
        alpha = layerStack.removeLast()
    }

    override fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int) = canvas.drawRect(l, t, r, b, fill.argb(color))

    override fun fillVerticalGradient(l: Float, t: Float, r: Float, b: Float, top: Int, bottom: Int) {
        fill.argb(Color.WHITE)
        fill.shader = LinearGradient(0f, t, 0f, b, top, bottom, Shader.TileMode.CLAMP)
        canvas.drawRect(l, t, r, b, fill)
        fill.shader = null
    }

    override fun fillRadialGlow(cx: Float, cy: Float, radius: Float, color: Int) {
        if (radius <= 0f) return
        val rgb = color and 0x00FFFFFF
        val a = Color.alpha(color)
        val mid = ((a * 0.35f).toInt() shl 24) or rgb
        fill.argb(Color.WHITE)
        fill.shader = RadialGradient(cx, cy, radius, intArrayOf(color, mid, rgb), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius, fill)
        fill.shader = null
    }

    override fun fillRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        if (r <= l || b <= t) return
        canvas.drawRoundRect(l, t, r, b, radius, radius, fill.argb(color))
    }

    override fun strokeRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, width: Float, color: Int) {
        stroke.strokeWidth = width
        canvas.drawRoundRect(l, t, r, b, radius, radius, stroke.argb(color))
    }

    override fun fillCircle(cx: Float, cy: Float, r: Float, color: Int) = canvas.drawCircle(cx, cy, r, fill.argb(color))

    override fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float, color: Int) {
        stroke.strokeWidth = width
        canvas.drawCircle(cx, cy, r, stroke.argb(color))
    }

    override fun strokeArc(cx: Float, cy: Float, r: Float, startDeg: Float, sweepDeg: Float, width: Float, color: Int) {
        stroke.strokeWidth = width
        // Android : 0° à 3 h, sens horaire — même convention que le moteur.
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startDeg, sweepDeg, false, stroke.argb(color))
    }

    override fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {
        stroke.strokeWidth = width
        canvas.drawLine(x0, y0, x1, y1, stroke.argb(color))
    }

    override fun fillPath(path: PathData, color: Int) = canvas.drawPath(toAndroid(path), fill.argb(color))

    override fun strokePath(path: PathData, width: Float, color: Int) {
        if (path.cmds.isEmpty()) return
        stroke.strokeWidth = width
        canvas.drawPath(toAndroid(path), stroke.argb(color))
    }

    private fun font(spec: FontSpec): Paint = text.apply { typeface = fonts.get(spec); textSize = spec.size }

    override fun measureText(text: String, font: FontSpec): Float = font(font).measureText(text)

    override fun metrics(font: FontSpec): Metrics = metricsCache.getOrPut(font) {
        val p = font(font)
        p.getTextBounds("H", 0, 1, bounds)
        Metrics(ascent = -bounds.top.toFloat(), descent = p.descent())
    }

    override fun drawText(text: String, x: Float, baseline: Float, font: FontSpec, color: Int) {
        val p = font(font)
        p.color = color
        p.alpha = (Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        canvas.drawText(text, x, baseline, p)
    }

    private fun toAndroid(p: PathData): Path {
        path.rewind()
        for (c in p.cmds) when (c) {
            is PathData.Move -> path.moveTo(c.x, c.y)
            is PathData.Line -> path.lineTo(c.x, c.y)
            is PathData.Cubic -> path.cubicTo(c.x1, c.y1, c.x2, c.y2, c.x, c.y)
            PathData.Close -> path.close()
        }
        return path
    }
}
