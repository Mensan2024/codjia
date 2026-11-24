// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Règles de sécurité appliquées à tout ce qui vient de l'extérieur de l'application (fichier audio
 * importé, nom de fichier). Code pur, testé sur la JVM ; l'adaptation Android ne fait que les appeler.
 *
 * Menace visée : un fichier piégé ou démesuré (taille, durée, nombre de canaux, fréquence) qui ferait
 * planter le décodeur ou saturer la mémoire, et un nom de fichier qui injecterait des caractères de
 * contrôle dans l'interface ou dans les métadonnées MP4.
 */
object ImportPolicy {
    /** 60 Mo : un MP3 de 30 min à 256 kb/s fait ~55 Mo. */
    const val MAX_AUDIO_BYTES: Long = 60L * 1024 * 1024
    /** 30 min : au-delà, ce n'est plus une musique de fond. */
    const val MAX_AUDIO_DURATION_MS: Long = 30L * 60 * 1000
    const val MIN_AUDIO_DURATION_MS: Long = 1_000
    val SAMPLE_RATES = 8_000..96_000
    val CHANNELS = 1..8
    /** Le décodage s'arrête dès qu'on a de quoi couvrir la vidéo (+ marge) : mémoire bornée, quelle que soit la source. */
    const val DECODE_MARGIN_MS: Long = 3_000
    /** Garde-fou contre un décodeur bloqué. */
    const val DECODE_TIMEOUT_MS: Long = 60_000
    const val MAX_NAME_CHARS = 60

    /** Types MIME acceptés à l'import (le sélecteur de fichiers filtre déjà sur les types audio). */
    fun isAcceptedMime(mime: String?): Boolean = mime != null && mime.startsWith("audio/") && mime.length <= 64

    /** Retourne `null` si l'import est acceptable, sinon le message à afficher à l'utilisateur. */
    fun checkAudio(sizeBytes: Long?, durationMs: Long, sampleRate: Int, channels: Int, mime: String?): String? = when {
        !isAcceptedMime(mime) -> "Ce fichier n'est pas un fichier audio"
        sizeBytes != null && sizeBytes > MAX_AUDIO_BYTES -> "Fichier trop lourd (max ${MAX_AUDIO_BYTES / 1048576} Mo)"
        durationMs < MIN_AUDIO_DURATION_MS -> "Ce fichier ne contient pas de piste audio lisible"
        durationMs > MAX_AUDIO_DURATION_MS -> "Musique trop longue (max ${MAX_AUDIO_DURATION_MS / 60000} min)"
        sampleRate !in SAMPLE_RATES -> "Fréquence d'échantillonnage non prise en charge ($sampleRate Hz)"
        channels !in CHANNELS -> "Nombre de canaux non pris en charge ($channels)"
        else -> null
    }

    /** Nombre maximal d'octets PCM 16 bits à conserver pour une vidéo de `videoMs`. */
    fun pcmByteBudget(videoMs: Long, sampleRate: Int, channels: Int): Long {
        val ms = (videoMs.coerceIn(0, 60_000) + DECODE_MARGIN_MS)
        return ms * sampleRate.coerceIn(SAMPLE_RATES) / 1000 * channels.coerceIn(CHANNELS) * 2
    }

    /** Nom affichable : sans extension, sans caractères de contrôle ni séparateurs de chemin, longueur bornée. */
    fun displayName(raw: String?): String {
        val base = (raw ?: "").substringBeforeLast('.')
        val clean = buildString {
            for (ch in base) {
                when {
                    ch.isISOControl() || ch == '/' || ch == '\\' -> append(' ')
                    Character.getType(ch) == Character.FORMAT.toInt() -> {} // ex. U+202E (inversion de sens)
                    else -> append(ch)
                }
            }
        }.replace(Regex("\\s+"), " ").trim()
        val cut = if (clean.length > MAX_NAME_CHARS) clean.take(MAX_NAME_CHARS - 1).trimEnd() + "…" else clean
        return cut.ifEmpty { "Ma musique" }
    }

    /** Nom de fichier sûr pour l'export (ASCII, sans traversée de répertoire). */
    fun safeFileStem(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9_-]"), "_").replace(Regex("_+"), "_").trim('_').take(48).ifEmpty { "video" }
}
