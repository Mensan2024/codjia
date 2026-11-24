// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Courbe d'accélération : f(0) = 0, f(1) = 1. Calcul pur, sans état ni horloge. */
fun interface Easing {
    fun ease(p: Float): Float

    companion object {
        val LINEAR = Easing { it }
        /** Départ vif, arrivée très douce : le mouvement « premium » des interfaces modernes. */
        val EXPO_OUT = CubicBezier(0.16f, 1f, 0.3f, 1f)
        val EASE_IN = CubicBezier(0.4f, 0f, 1f, 1f)
        val EASE_IN_OUT = CubicBezier(0.65f, 0f, 0.35f, 1f)
        val STANDARD = CubicBezier(0.2f, 0f, 0f, 1f)
        /** Léger dépassement puis retour (apparition « pop » des pastilles). */
        val BACK_OUT = CubicBezier(0.34f, 1.56f, 0.64f, 1f)
    }
}

/** Bézier cubique CSS (P0 = 0,0 ; P3 = 1,1). Résolution de x(t) = p par Newton puis dichotomie. */
class CubicBezier(private val x1: Float, private val y1: Float, private val x2: Float, private val y2: Float) : Easing {
    init { require(x1 in 0f..1f && x2 in 0f..1f) { "x1 et x2 doivent être dans [0, 1]" } }

    private fun bx(t: Float) = 3 * (1 - t) * (1 - t) * t * x1 + 3 * (1 - t) * t * t * x2 + t * t * t
    private fun by(t: Float) = 3 * (1 - t) * (1 - t) * t * y1 + 3 * (1 - t) * t * t * y2 + t * t * t
    private fun dbx(t: Float) = 3 * (1 - t) * (1 - t) * x1 + 6 * (1 - t) * t * (x2 - x1) + 3 * t * t * (1 - x2)

    override fun ease(p: Float): Float {
        if (p <= 0f) return 0f
        if (p >= 1f) return 1f
        var t = p
        repeat(8) {
            val err = bx(t) - p
            if (abs(err) < 1e-5f) return by(t)
            val d = dbx(t)
            if (abs(d) < 1e-6f) return@repeat
            t -= err / d
        }
        // Repli robuste si Newton n'a pas convergé.
        var lo = 0f
        var hi = 1f
        t = p
        repeat(30) {
            val x = bx(t)
            if (abs(x - p) < 1e-5f) return by(t)
            if (x < p) lo = t else hi = t
            t = (lo + hi) / 2
        }
        return by(t)
    }
}

fun clamp01(v: Float) = max(0f, min(1f, v))
fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Progression d'une fenêtre [start, start + dur] au temps t, bornée à [0, 1]. */
fun window(t: Float, start: Float, dur: Float): Float = if (dur <= 0f) (if (t >= start) 1f else 0f) else clamp01((t - start) / dur)
