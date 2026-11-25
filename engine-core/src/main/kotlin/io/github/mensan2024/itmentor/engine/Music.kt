// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.max
import kotlin.math.roundToLong

/** Catégories de la bibliothèque musicale, dans l'ordre d'affichage. « Sans musique » est un choix à part. */
enum class MusicCategory(val label: String) {
    DOUCE("Douce et apaisante"),
    DYNAMIQUE("Dynamique et motivante"),
    CORPORATE("Professionnelle et corporate"),
    INSPIRANTE("Inspirante et émotionnelle"),
    TECHNO("Technologique et futuriste"),
    EDUCATIVE("Éducative et pédagogique"),
    NATURE("Nature et environnement"),
    AFRO("Afro (afrobeats, amapiano)"),
    CINEMATIQUE("Cinématique et épique"),
}

/** Origine d'une piste : intégrée à l'appli (licence connue) ou importée par l'utilisateur (sous sa responsabilité). */
enum class MusicSource { BUILT_IN, USER_IMPORT }

/**
 * Une piste du catalogue. Le catalogue est décrit par `assets/music/music.json` :
 * en ajouter une revient à déposer un fichier et une entrée, sans toucher au code.
 */
data class MusicTrack(
    val id: String,
    val title: String,
    val category: MusicCategory,
    val bpm: Int,
    val file: String,
    val durationMs: Long,
    val licence: String,
    val origin: String,
    /** 48 valeurs de 0 à 1 : forme d'onde affichée dans l'interface. */
    val peaks: List<Float> = emptyList(),
    val source: MusicSource = MusicSource.BUILT_IN,
) {
    val beatMs: Double get() = 60_000.0 / bpm
}

/** Volume de la musique dans la vidéo exportée. */
enum class MusicVolume(val label: String, val gain: Float) {
    NORMAL("Normal", 1.0f),
    REDUIT("Réduit", 0.45f),
    COUPE("Coupé", 0f),
}

/** Choix musical de l'utilisateur. `track == null` = « Sans musique ». */
data class MusicChoice(val track: MusicTrack?, val volume: MusicVolume = MusicVolume.NORMAL) {
    /** Faut-il une piste audio dans le MP4 ? */
    val hasAudio: Boolean get() = track != null && volume != MusicVolume.COUPE
}

/**
 * Montage « au rythme » : chaque nouvelle scène commence exactement sur un temps de la musique.
 * Les durées restent proches de celles prévues (arrondi au temps le plus proche, au moins 2 temps),
 * et la vidéo ne dépasse jamais 40 s : si besoin, on retire un temps aux scènes les plus longues.
 */
object Rhythm {
    fun align(sb: Storyboard, beatMs: Double, maxMs: Long = Timeline.limitFor(sb)): Storyboard {
        val last = sb.scenes.lastIndex
        fun overlap(i: Int, s: Scene) =
            if (i == last || s.transitionOut.type == TransitionType.CUT) 0L else s.transitionOut.durationMs.toLong()

        // Nombre de temps entre le début d'une scène et le début de la suivante.
        val beats = sb.scenes.mapIndexed { i, s ->
            max(2L, ((s.durationMs - overlap(i, s)) / beatMs).roundToLong())
        }.toMutableList()

        fun total(): Long = (beats.sum() * beatMs).roundToLong()
        while (total() > maxMs) {
            val i = beats.indices.maxByOrNull { beats[it] }!!
            if (beats[i] <= 2) break
            beats[i] = beats[i] - 1
        }
        // Positions absolues arrondies une seule fois : aucune dérive, même après 40 s.
        var acc = 0L
        val scenes = sb.scenes.mapIndexed { i, s ->
            val start = (acc * beatMs).roundToLong()
            acc += beats[i]
            val nextStart = (acc * beatMs).roundToLong()
            // La transition doit tenir dans la scène (règle de la Timeline) : on la raccourcit si besoin.
            val dur = nextStart - start + overlap(i, s)
            s.copy(durationMs = dur.toInt())
        }
        return sb.copy(scenes = scenes)
    }
}
