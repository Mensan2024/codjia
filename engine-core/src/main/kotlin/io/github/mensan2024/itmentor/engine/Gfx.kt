// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Familles de polices embarquées (toutes sous licence OFL, fichiers dans assets/fonts). */
enum class FontFamily {
    TEXT, DISPLAY, MONO,
    SERIF,      // Gloock : élégance éditoriale (Corporate Premium)
    ROUNDED,    // Outfit : géométrique et amicale (Modern Education)
    GROTESK,    // Bricolage Grotesque : organique (Green Nature)
    CLEAN,      // Instrument Sans : nette et rassurante (Medical Clean)
    CONDENSED,  // Big Shoulders : condensée et percutante (Marketing Impact)
    HEAVY,      // Boldonse : très grasse, expressive (Bold Typography)
    CINEMA,     // Instrument Serif : titres de film (Cinematic Story)
    CINEMA_ITALIC,
    TECH,       // Tektur : technique (étiquettes Cyber Neon)
    HAND,       // Kalam : écriture au feutre (format Tableau blanc)
}

/** Fichier à utiliser pour chaque famille et graisse. Partagé par les rendus Android et JVM. */
object FontCatalog {
    val FILES: Map<FontFamily, Map<Int, String>> = mapOf(
        FontFamily.TEXT to mapOf(400 to "Inter-Regular.otf", 500 to "Inter-Medium.otf", 600 to "Inter-SemiBold.otf",
            700 to "Inter-Bold.otf", 800 to "Inter-ExtraBold.otf"),
        FontFamily.DISPLAY to mapOf(800 to "InterDisplay-ExtraBold.otf", 900 to "InterDisplay-Black.otf"),
        FontFamily.MONO to mapOf(400 to "JetBrainsMono-Regular.ttf", 700 to "JetBrainsMono-Bold.ttf"),
        FontFamily.SERIF to mapOf(400 to "Gloock-Regular.ttf"),
        FontFamily.ROUNDED to mapOf(700 to "Outfit-Bold.ttf"),
        FontFamily.GROTESK to mapOf(700 to "BricolageGrotesque-Bold.ttf"),
        FontFamily.CLEAN to mapOf(700 to "InstrumentSans-Bold.ttf"),
        FontFamily.CONDENSED to mapOf(700 to "BigShoulders-Bold.ttf"),
        FontFamily.HEAVY to mapOf(400 to "Boldonse-Regular.ttf"),
        FontFamily.CINEMA to mapOf(400 to "InstrumentSerif-Regular.ttf"),
        FontFamily.CINEMA_ITALIC to mapOf(400 to "InstrumentSerif-Italic.ttf"),
        FontFamily.TECH to mapOf(500 to "Tektur-Medium.ttf"),
        FontFamily.HAND to mapOf(400 to "Kalam-Regular.ttf", 700 to "Kalam-Bold.ttf"),
    )

    fun file(family: FontFamily, weight: Int): String {
        val byWeight = FILES.getValue(family)
        return byWeight.getValue(byWeight.keys.minByOrNull { kotlin.math.abs(it - weight) }!!)
    }
}

/** Police demandée par le moteur. `weight` suit l'échelle CSS (400 = normal, 800 = extra-gras). */
data class FontSpec(val family: FontFamily, val weight: Int, val size: Float) {
    fun withSize(s: Float) = copy(size = s)
}

/** `ascent` = hauteur des capitales (centrage optique), `descent` = profondeur des jambages. */
data class Metrics(val ascent: Float, val descent: Float)

/**
 * Surface de dessin minimale dont le moteur a besoin.
 * Deux implémentations : Android (android.graphics.Canvas) et JVM (java.awt.Graphics2D, banc d'essai).
 * Les couleurs sont en ARGB 32 bits. Les coordonnées sont celles de l'espace de conception 1080 × 1920.
 */
interface Gfx {
    fun save()
    fun restore()
    fun translate(dx: Float, dy: Float)
    fun scale(s: Float, px: Float, py: Float)
    fun rotate(degrees: Float, px: Float, py: Float)
    fun clipRect(l: Float, t: Float, r: Float, b: Float)

    /** Opacité globale multipliée, sauvegardée par save/restore. */
    fun multiplyAlpha(a: Float)

    /** Calque hors écran composé avec une opacité (fondus entre scènes). */
    fun beginLayer(alpha: Float)
    fun endLayer()

    /** Découpe selon un chemin (masques circulaires des transitions). */
    fun clipPath(path: PathData)

    fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int)
    fun fillVerticalGradient(l: Float, t: Float, r: Float, b: Float, top: Int, bottom: Int)
    /** Halo radial : `color` au centre, transparent au bord. */
    fun fillRadialGlow(cx: Float, cy: Float, radius: Float, color: Int)
    fun fillRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int)
    fun strokeRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, width: Float, color: Int)
    fun fillCircle(cx: Float, cy: Float, r: Float, color: Int)
    fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float, color: Int)
    /** Arc ouvert à extrémités arrondies. 0° = 3 h, sens horaire. */
    fun strokeArc(cx: Float, cy: Float, r: Float, startDeg: Float, sweepDeg: Float, width: Float, color: Int)
    fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int)
    fun fillPath(path: PathData, color: Int)
    fun strokePath(path: PathData, width: Float, color: Int)

    fun measureText(text: String, font: FontSpec): Float
    fun metrics(font: FontSpec): Metrics
    fun drawText(text: String, x: Float, baseline: Float, font: FontSpec, color: Int)
}

/** Chemin vectoriel indépendant de la plateforme. */
class PathData {
    sealed interface Cmd
    data class Move(val x: Float, val y: Float) : Cmd
    data class Line(val x: Float, val y: Float) : Cmd
    data class Cubic(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x: Float, val y: Float) : Cmd
    data object Close : Cmd

    val cmds = ArrayList<Cmd>()

    fun moveTo(x: Float, y: Float) = apply { cmds += Move(x, y) }
    fun lineTo(x: Float, y: Float) = apply { cmds += Line(x, y) }
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) = apply { cmds += Cubic(x1, y1, x2, y2, x, y) }
    fun close() = apply { cmds += Close }

    /** Cercle approché par 4 cubiques (erreur < 0,03 %). */
    fun circle(cx: Float, cy: Float, r: Float) = apply {
        val k = 0.5522848f * r
        moveTo(cx + r, cy)
        cubicTo(cx + r, cy + k, cx + k, cy + r, cx, cy + r)
        cubicTo(cx - k, cy + r, cx - r, cy + k, cx - r, cy)
        cubicTo(cx - r, cy - k, cx - k, cy - r, cx, cy - r)
        cubicTo(cx + k, cy - r, cx + r, cy - k, cx + r, cy)
        close()
    }

    fun polygon(cx: Float, cy: Float, r: Float, sides: Int, rotationDeg: Float = -90f) = apply {
        for (i in 0 until sides) {
            val a = rad((rotationDeg + i * 360.0 / sides)).toFloat()
            val x = cx + r * cos(a)
            val y = cy + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    /** Copie mise à l'échelle et déplacée (les icônes sont dessinées sur une grille 24 × 24). */
    fun transformed(s: Float, tx: Float, ty: Float): PathData {
        val out = PathData()
        for (c in cmds) out.cmds += when (c) {
            is Move -> Move(c.x * s + tx, c.y * s + ty)
            is Line -> Line(c.x * s + tx, c.y * s + ty)
            is Cubic -> Cubic(c.x1 * s + tx, c.y1 * s + ty, c.x2 * s + tx, c.y2 * s + ty, c.x * s + tx, c.y * s + ty)
            Close -> Close
        }
        return out
    }

    /** Aplatit le chemin en polylignes (pour tracer un trait progressivement). */
    fun flatten(stepsPerCurve: Int = 16): List<List<Pair<Float, Float>>> {
        val polys = ArrayList<MutableList<Pair<Float, Float>>>()
        var cur: MutableList<Pair<Float, Float>>? = null
        var sx = 0f; var sy = 0f; var px = 0f; var py = 0f
        for (c in cmds) when (c) {
            is Move -> { cur = mutableListOf(c.x to c.y); polys += cur; sx = c.x; sy = c.y; px = c.x; py = c.y }
            is Line -> { cur!!.add(c.x to c.y); px = c.x; py = c.y }
            is Cubic -> {
                for (i in 1..stepsPerCurve) {
                    val t = i / stepsPerCurve.toFloat()
                    val u = 1 - t
                    val x = u * u * u * px + 3 * u * u * t * c.x1 + 3 * u * t * t * c.x2 + t * t * t * c.x
                    val y = u * u * u * py + 3 * u * u * t * c.y1 + 3 * u * t * t * c.y2 + t * t * t * c.y
                    cur!!.add(x to y)
                }
                px = c.x; py = c.y
            }
            Close -> { cur!!.add(sx to sy); px = sx; py = sy }
        }
        return polys
    }

    /** Partie du chemin correspondant à la fraction [0, 1] de sa longueur totale. */
    fun trimmed(fraction: Float): PathData = segment(0f, fraction)

    /** Portion du chemin entre les fractions `from` et `to` de sa longueur (impulsions, tracés progressifs). */
    fun segment(from: Float, to: Float): PathData {
        val a = clamp01(from)
        val b = clamp01(to)
        val out = PathData()
        if (b <= a) return out
        val polys = flatten()
        var total = 0f
        for (p in polys) for (i in 1 until p.size) total += hypot(p[i].first - p[i - 1].first, p[i].second - p[i - 1].second)
        val la = total * a
        val lb = total * b
        var acc = 0f
        for (p in polys) {
            var open = false
            for (i in 1 until p.size) {
                val (x0, y0) = p[i - 1]
                val (x1, y1) = p[i]
                val seg = hypot(x1 - x0, y1 - y0)
                val s0 = acc
                val s1 = acc + seg
                acc = s1
                if (s1 <= la || s0 >= lb || seg == 0f) { open = false; continue }
                val k0 = ((la - s0) / seg).coerceIn(0f, 1f)
                val k1 = ((lb - s0) / seg).coerceIn(0f, 1f)
                if (!open) { out.moveTo(x0 + (x1 - x0) * k0, y0 + (y1 - y0) * k0); open = true }
                out.lineTo(x0 + (x1 - x0) * k1, y0 + (y1 - y0) * k1)
                if (s1 >= lb) return out
            }
        }
        return out
    }
}

/** Couleur ARGB avec une opacité supplémentaire. */
fun Int.withAlpha(a: Float): Int {
    val base = (this ushr 24) and 0xFF
    val na = (base * clamp01(a)).toInt().coerceIn(0, 255)
    return (na shl 24) or (this and 0x00FFFFFF)
}

/** Mélange linéaire de deux couleurs ARGB. */
fun mix(c0: Int, c1: Int, t: Float): Int {
    fun ch(c: Int, s: Int) = (c ushr s) and 0xFF
    fun m(s: Int) = (ch(c0, s) + (ch(c1, s) - ch(c0, s)) * clamp01(t)).toInt().coerceIn(0, 255)
    return (m(24) shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

fun rgb(hex: Long): Int = (0xFF000000L or hex).toInt()
