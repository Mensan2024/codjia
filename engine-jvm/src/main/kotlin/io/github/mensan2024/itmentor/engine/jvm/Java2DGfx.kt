// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.FontCatalog
import io.github.mensan2024.itmentor.engine.FontSpec
import io.github.mensan2024.itmentor.engine.Gfx
import io.github.mensan2024.itmentor.engine.Metrics
import io.github.mensan2024.itmentor.engine.PathData
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File

/** Polices chargées une fois depuis le dossier `fonts/` du projet (licence OFL). */
class FontBank(private val dir: File) {
    private val base = HashMap<String, Font>()
    private val sized = HashMap<FontSpec, Font>()

    fun get(spec: FontSpec): Font = sized.getOrPut(spec) {
        val name = FontCatalog.file(spec.family, spec.weight)
        base.getOrPut(name) { Font.createFont(Font.TRUETYPE_FONT, File(dir, name)) }.deriveFont(spec.size)
    }
}

/**
 * Implémentation Java2D de Gfx, pour le banc d'essai et les tests sur JVM.
 * Sur le téléphone, c'est AndroidCanvasGfx qui est utilisé ; le moteur, lui, est identique.
 */
class Java2DGfx(private val image: BufferedImage, private val fonts: FontBank) : Gfx {

    private class State(val tx: AffineTransform, val clip: Shape?, val alpha: Float)
    private class Layer(val g: Graphics2D, val img: BufferedImage, val alpha: Float, val parent: Graphics2D, val stack: ArrayDeque<State>, val galpha: Float)

    private var g: Graphics2D = newGraphics(image)
    private var alpha = 1f
    private var stack = ArrayDeque<State>()
    private val layers = ArrayDeque<Layer>()
    private val layerPool = ArrayList<BufferedImage>()
    private val frc = FontRenderContext(null, true, true)
    private val metricsCache = HashMap<FontSpec, Metrics>()

    /** Boîtes englobantes (en pixels de sortie) de chaque texte dessiné, pour les tests de marges. */
    var recordText: MutableList<Pair<String, Rectangle2D>>? = null

    private fun newGraphics(img: BufferedImage): Graphics2D = img.createGraphics().apply {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
        setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    }

    fun reset() {
        g.dispose()
        g = newGraphics(image)
        alpha = 1f
        stack.clear()
    }

    private fun applyAlpha() { g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha.coerceIn(0f, 1f)) }

    private fun color(argb: Int) = Color(argb, true)

    override fun save() { stack.addLast(State(g.transform, g.clip, alpha)) }
    override fun restore() {
        val s = stack.removeLast()
        g.transform = s.tx; g.clip = s.clip; alpha = s.alpha; applyAlpha()
    }
    override fun translate(dx: Float, dy: Float) = g.translate(dx.toDouble(), dy.toDouble())
    override fun scale(s: Float, px: Float, py: Float) {
        g.translate(px.toDouble(), py.toDouble()); g.scale(s.toDouble(), s.toDouble()); g.translate(-px.toDouble(), -py.toDouble())
    }
    override fun rotate(degrees: Float, px: Float, py: Float) = g.rotate(Math.toRadians(degrees.toDouble()), px.toDouble(), py.toDouble())
    override fun clipRect(l: Float, t: Float, r: Float, b: Float) = g.clip(Rectangle2D.Float(l, t, r - l, b - t))
    override fun clipPath(path: PathData) = g.clip(toAwt(path))
    override fun multiplyAlpha(a: Float) { alpha *= a; applyAlpha() }

    override fun beginLayer(alpha: Float) {
        val img = layerPool.removeLastOrNull() ?: BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB_PRE)
        val lg = newGraphics(img)
        lg.composite = AlphaComposite.Clear
        lg.fillRect(0, 0, img.width, img.height)
        lg.composite = AlphaComposite.SrcOver
        lg.transform = g.transform
        lg.clip = g.clip
        layers.addLast(Layer(lg, img, alpha, g, stack, this.alpha))
        g = lg
        stack = ArrayDeque()
        this.alpha = 1f
    }

    override fun endLayer() {
        val l = layers.removeLast()
        l.g.dispose()
        g = l.parent
        stack = l.stack
        alpha = l.galpha
        val saved = g.transform
        g.transform = AffineTransform()
        g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (alpha * l.alpha).coerceIn(0f, 1f))
        g.drawImage(l.img, 0, 0, null)
        g.transform = saved
        applyAlpha()
        layerPool += l.img
    }

    override fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int) {
        g.color = color(color); g.fill(Rectangle2D.Float(l, t, r - l, b - t))
    }

    override fun fillVerticalGradient(l: Float, t: Float, r: Float, b: Float, top: Int, bottom: Int) {
        val old = g.paint
        g.paint = GradientPaint(0f, t, color(top), 0f, b, color(bottom))
        g.fill(Rectangle2D.Float(l, t, r - l, b - t))
        g.paint = old
    }

    override fun fillRadialGlow(cx: Float, cy: Float, radius: Float, color: Int) {
        if (radius <= 0f) return
        val old = g.paint
        val c0 = color(color)
        val c1 = Color(c0.red, c0.green, c0.blue, 0)
        val mid = Color(c0.red, c0.green, c0.blue, (c0.alpha * 0.35f).toInt())
        g.paint = RadialGradientPaint(cx, cy, radius, floatArrayOf(0f, 0.45f, 1f), arrayOf(c0, mid, c1))
        g.fill(Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2))
        g.paint = old
    }

    override fun fillRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {
        if (r <= l || b <= t) return
        g.color = color(color); g.fill(RoundRectangle2D.Float(l, t, r - l, b - t, radius * 2, radius * 2))
    }

    override fun strokeRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, width: Float, color: Int) {
        g.color = color(color); g.stroke = stroke(width)
        g.draw(RoundRectangle2D.Float(l, t, r - l, b - t, radius * 2, radius * 2))
    }

    override fun fillCircle(cx: Float, cy: Float, r: Float, color: Int) {
        g.color = color(color); g.fill(Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2))
    }

    override fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float, color: Int) {
        g.color = color(color); g.stroke = stroke(width); g.draw(Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2))
    }

    override fun strokeArc(cx: Float, cy: Float, r: Float, startDeg: Float, sweepDeg: Float, width: Float, color: Int) {
        g.color = color(color); g.stroke = stroke(width)
        // Java2D : angles en degrés, sens antihoraire, 0° à 3 h. On inverse pour le sens horaire.
        g.draw(Arc2D.Float(cx - r, cy - r, r * 2, r * 2, -startDeg, -sweepDeg, Arc2D.OPEN))
    }

    override fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {
        g.color = color(color); g.stroke = stroke(width); g.draw(java.awt.geom.Line2D.Float(x0, y0, x1, y1))
    }

    override fun fillPath(path: PathData, color: Int) { g.color = color(color); g.fill(toAwt(path)) }

    override fun strokePath(path: PathData, width: Float, color: Int) {
        if (path.cmds.isEmpty()) return
        g.color = color(color); g.stroke = stroke(width); g.draw(toAwt(path))
    }

    override fun measureText(text: String, font: FontSpec): Float =
        fonts.get(font).getStringBounds(text, frc).width.toFloat()

    override fun metrics(font: FontSpec): Metrics = metricsCache.getOrPut(font) {
        val lm = fonts.get(font).getLineMetrics("Hgé", frc)
        // Hauteur de capitale réelle plutôt que l'ascendante de la police (centrage optique).
        val gv = fonts.get(font).createGlyphVector(frc, "H")
        val capH = -gv.visualBounds.y.toFloat()
        Metrics(ascent = capH, descent = lm.descent)
    }

    override fun drawText(text: String, x: Float, baseline: Float, font: FontSpec, color: Int) {
        val f = fonts.get(font)
        g.font = f; g.color = color(color)
        g.drawString(text, x, baseline)
        // Seul le texte destiné à être lu est enregistré (pas les textes décoratifs très transparents du décor).
        val visible = ((color ushr 24) and 0xFF) / 255f * alpha >= 0.4f
        if (visible) recordText?.let { rec ->
            val b = f.getStringBounds(text, frc)
            val local = Rectangle2D.Double(x.toDouble(), baseline + b.y, b.width, b.height)
            var dev = g.transform.createTransformedShape(local).bounds2D
            // Texte partiellement masqué (zone de découpe, zoom de caméra) : seule la partie visible compte.
            g.clip?.let { c ->
                val clipDev = g.transform.createTransformedShape(c).bounds2D
                if (!clipDev.intersects(dev)) return
                dev = dev.createIntersection(clipDev)
            }
            rec += text to dev
        }
    }

    private fun stroke(w: Float) = BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)

    private fun toAwt(p: PathData): Path2D.Float {
        val out = Path2D.Float()
        for (c in p.cmds) when (c) {
            is PathData.Move -> out.moveTo(c.x, c.y)
            is PathData.Line -> out.lineTo(c.x, c.y)
            is PathData.Cubic -> out.curveTo(c.x1, c.y1, c.x2, c.y2, c.x, c.y)
            PathData.Close -> out.closePath()
        }
        return out
    }
}
