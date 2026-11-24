// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * État visuel d'un élément à un instant donné. Les animations ne lisent jamais d'horloge :
 * tout est calculé à partir du temps de rendu (ms depuis le début de la scène).
 */
data class Pose(
    val alpha: Float = 1f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val scale: Float = 1f,
    /** Fraction de texte révélée (machine à écrire) ou de trait dessiné. */
    val reveal: Float = 1f,
) {
    operator fun plus(o: Pose) = Pose(alpha * o.alpha, dx + o.dx, dy + o.dy, scale * o.scale, minOf(reveal, o.reveal))

    companion object { val IDENTITY = Pose() }
}

object Animator {
    private const val W = VideoFormat.DESIGN_WIDTH
    private const val H = VideoFormat.DESIGN_HEIGHT

    /** Pose d'entrée : à p = 0 l'élément est « hors champ », à p = 1 il est en place. */
    fun enter(spec: AnimSpec, tMs: Float): Pose {
        if (spec.type == AnimType.NONE) return Pose.IDENTITY
        val p = window(tMs, spec.delayMs.toFloat(), spec.durationMs.toFloat())
        val e = spec.easing.ease(p)
        // L'opacité monte plus vite que le déplacement : le texte est lisible avant d'être arrivé.
        val a = clamp01(p * 1.8f)
        val dx = spec.distance * W
        val dy = spec.distance * H
        return when (spec.type) {
            AnimType.NONE -> Pose.IDENTITY
            AnimType.FADE_IN -> Pose(alpha = Easing.STANDARD.ease(p))
            AnimType.FADE_OUT -> Pose(alpha = 1f - e) // utilisé en entrée : disparition (rare)
            AnimType.SLIDE_LEFT -> Pose(alpha = a, dx = (1 - e) * dx)
            AnimType.SLIDE_RIGHT -> Pose(alpha = a, dx = -(1 - e) * dx)
            AnimType.SLIDE_UP -> Pose(alpha = a, dy = (1 - e) * dy)
            AnimType.SLIDE_DOWN -> Pose(alpha = a, dy = -(1 - e) * dy)
            AnimType.ZOOM_IN -> Pose(alpha = a, scale = lerp(0.82f, 1f, e))
            AnimType.ZOOM_OUT -> Pose(alpha = a, scale = lerp(1.18f, 1f, e))
            AnimType.TYPEWRITER -> Pose(alpha = if (p > 0f || spec.delayMs == 0) 1f else 0f, reveal = e)
            AnimType.MOVE_PATH -> Pose(reveal = e) // la position est calculée par l'élément à partir de reveal
        }
    }

    /** Pose de sortie : la fenêtre se termine exactement à la fin de la scène. */
    fun exit(spec: AnimSpec, tMs: Float, sceneMs: Float): Pose {
        if (spec.type == AnimType.NONE || spec.durationMs == 0) return Pose.IDENTITY
        val start = sceneMs - spec.durationMs - spec.delayMs
        val p = window(tMs, start, spec.durationMs.toFloat())
        if (p <= 0f) return Pose.IDENTITY
        val e = spec.easing.ease(p)
        val dx = spec.distance * W
        val dy = spec.distance * H
        return when (spec.type) {
            AnimType.FADE_OUT, AnimType.FADE_IN, AnimType.TYPEWRITER, AnimType.MOVE_PATH, AnimType.NONE -> Pose(alpha = 1 - e)
            AnimType.SLIDE_LEFT -> Pose(alpha = 1 - e, dx = -e * dx)
            AnimType.SLIDE_RIGHT -> Pose(alpha = 1 - e, dx = e * dx)
            AnimType.SLIDE_UP -> Pose(alpha = 1 - e, dy = -e * dy)
            AnimType.SLIDE_DOWN -> Pose(alpha = 1 - e, dy = e * dy)
            AnimType.ZOOM_IN -> Pose(alpha = 1 - e, scale = lerp(1f, 1.12f, e))
            AnimType.ZOOM_OUT -> Pose(alpha = 1 - e, scale = lerp(1f, 0.88f, e))
        }
    }

    /** Pose complète d'un élément : entrée (avec décalage propre) puis sortie commune à la scène. */
    fun pose(enter: AnimSpec, exit: AnimSpec, tMs: Float, sceneMs: Float): Pose =
        enter(enter, tMs) + exit(exit, tMs, sceneMs)
}
