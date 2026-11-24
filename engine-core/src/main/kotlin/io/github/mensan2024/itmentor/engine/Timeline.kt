// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.roundToLong

class StoryboardException(message: String) : IllegalArgumentException(message)

/** Position d'une scène sur la ligne de temps globale (en ms). */
data class Slot(val index: Int, val scene: Scene, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

/** Ce qui est visible à un instant : une scène seule, ou deux scènes pendant une transition. */
sealed interface Moment {
    data class Single(val slot: Slot, val localMs: Float) : Moment
    data class Blend(
        val from: Slot, val fromLocalMs: Float,
        val to: Slot, val toLocalMs: Float,
        val transition: Transition,
        /** Avancement de la transition, dans [0, 1]. */
        val progress: Float,
    ) : Moment
}

/**
 * Chronologie calculée une seule fois. Les transitions sont des recouvrements :
 * la scène i+1 commence `transition.durationMs` avant la fin de la scène i.
 * Durée totale = somme des durées − somme des recouvrements.
 */
class Timeline private constructor(val slots: List<Slot>, val totalMs: Long) {

    fun frameCount(fps: Int): Int = ((totalMs * fps) / 1000.0).roundToLong().toInt()

    /** Temps (ms) de l'image n. Calcul entier : aucune dérive, quelle que soit la longueur. */
    fun frameTimeMs(frame: Int, fps: Int): Float = (frame * 1000.0 / fps).toFloat()

    fun at(tMs: Float): Moment {
        val t = tMs.coerceIn(0f, (totalMs - 1).coerceAtLeast(0).toFloat())
        for (i in slots.indices) {
            val s = slots[i]
            val next = slots.getOrNull(i + 1)
            if (next != null && t >= next.startMs && t < s.endMs) {
                val tr = s.scene.transitionOut
                val p = (t - next.startMs) / (s.endMs - next.startMs).toFloat()
                return Moment.Blend(s, t - s.startMs, next, t - next.startMs, tr, clamp01(p))
            }
            if (t >= s.startMs && t < s.endMs) return Moment.Single(s, t - s.startMs)
        }
        val last = slots.last()
        return Moment.Single(last, (last.durationMs - 1).toFloat())
    }

    companion object {
        /** Une astuce dure de 35 à 45 s selon son contenu ; jamais plus de 45 s. */
        const val MAX_DURATION_MS = 45_000L
        const val MAX_DEMO_DURATION_MS = 45_000L

        fun limitFor(sb: Storyboard): Long = if (sb.scenes.any { it.layout == Layout.SCREEN }) MAX_DEMO_DURATION_MS else MAX_DURATION_MS

        fun build(storyboard: Storyboard, maxDurationMs: Long = limitFor(storyboard)): Timeline {
            val scenes = storyboard.scenes
            val slots = ArrayList<Slot>(scenes.size)
            var cursor = 0L
            for ((i, sc) in scenes.withIndex()) {
                val start = cursor
                val end = start + sc.durationMs
                slots += Slot(i, sc, start, end)
                val isLast = i == scenes.lastIndex
                val overlap = if (isLast || sc.transitionOut.type == TransitionType.CUT) 0 else sc.transitionOut.durationMs
                if (!isLast) {
                    val nextDur = scenes[i + 1].durationMs
                    if (overlap * 2 > sc.durationMs || overlap * 2 > nextDur) {
                        throw StoryboardException(
                            "La transition après « ${sc.id} » ($overlap ms) dépasse la moitié d'une des deux scènes"
                        )
                    }
                }
                cursor = end - overlap
            }
            val total = slots.last().endMs
            if (total > maxDurationMs) {
                throw StoryboardException(
                    "La vidéo durerait ${"%.1f".format(total / 1000.0)} s : le maximum est ${maxDurationMs / 1000} s"
                )
            }
            return Timeline(slots, total)
        }
    }
}
