// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Voix off : la durée de chaque scène s'adapte à la durée de sa phrase lue.
 *
 * Règles :
 * - la voix commence [LEAD_MS] après le début de la scène (le titre apparaît d'abord) ;
 * - elle se termine avant le début de la transition suivante, plus [GAP_MS] de respiration :
 *   deux voix ne se chevauchent jamais ;
 * - une scène n'est jamais raccourcie, seulement allongée ;
 * - si le total dépasse 40 s, `Timeline.build` refuse : l'appelant relance alors la synthèse plus vite.
 */
object Narration {
    const val LEAD_MS = 450L
    const val GAP_MS = 300L
    const val END_MS = 900L

    fun texts(sb: Storyboard): Map<String, String> =
        sb.scenes.mapNotNull { s -> s.narration?.takeIf { it.isNotBlank() }?.let { s.id to it } }.toMap()

    fun fit(sb: Storyboard, voiceMs: Map<String, Long>): Storyboard {
        val scenes = sb.scenes.mapIndexed { i, s ->
            val v = voiceMs[s.id] ?: return@mapIndexed s
            val isLast = i == sb.scenes.lastIndex
            val overlap = if (isLast || s.transitionOut.type == TransitionType.CUT) 0L else s.transitionOut.durationMs.toLong()
            val need = LEAD_MS + v + if (isLast) END_MS else overlap + GAP_MS
            if (need > s.durationMs) s.copy(durationMs = need.toInt()) else s
        }
        return sb.copy(scenes = scenes)
    }

    /** Instant (ms) où commence la voix de chaque scène, sur la chronologie finale. */
    fun starts(timeline: Timeline): Map<String, Long> =
        timeline.slots.associate { it.scene.id to it.startMs + LEAD_MS }
}
