// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Voix off : automatique (français ou anglais selon chaque phrase), toujours en français, toujours en anglais, ou aucune. */
enum class VoiceMode(val label: String) { AUTO("Auto FR/EN"), FR("Français"), EN("Anglais"), NONE("Sans voix") }

/**
 * Voix off lue par la synthèse vocale du téléphone, par-dessus la musique.
 *
 * Règles :
 * - chaque scène a une phrase courte tirée de son contenu (titre, définition, étapes…) ;
 * - la voix commence [LEAD_MS] après le début de la scène et se termine avant la transition suivante :
 *   deux phrases ne se chevauchent jamais ; une scène n'est qu'allongée, jamais raccourcie ;
 * - en mode automatique, la langue est choisie phrase par phrase (une leçon d'anglais mêle les deux) ;
 * - la musique baisse automatiquement sous la voix (« ducking ») puis remonte.
 */
object Narration {
    const val LEAD_MS = 400L
    const val GAP_MS = 250L
    const val END_MS = 700L

    data class Line(val sceneId: String, val text: String, val lang: String)

    /** Phrase lue pour une scène (texte nettoyé pour la synthèse vocale). */
    fun textOf(s: Scene): String {
        fun p(x: String?) = Marked.strip(x ?: "").trim().trimEnd('.', ':')
        val parts: List<String> = when (s.layout) {
            Layout.HERO -> listOf(p(s.title), p(s.subtitle))
            Layout.DEFINITION -> listOf(p(s.body.ifEmpty { s.subtitle ?: s.title }))
            Layout.STEPS -> listOf(p(s.title)) + s.items.map { splitLabel(p(it)).first }
            Layout.STAT -> listOf(p(s.statValue), p(s.title))
            Layout.COMPARE -> s.compare?.let { listOf("${it.badLabel} : ${it.badText()}", "${it.goodLabel} : ${it.goodText()}") } ?: listOf(p(s.title))
            Layout.CHECKLIST -> listOf(p(s.title)) + s.items.map { p(it) } + listOf(p(s.body))
            Layout.SCREEN -> listOf(p(s.subtitle ?: s.title))
        }
        return clean(parts.filter { it.isNotBlank() }.joinToString(". ") + ".")
    }

    /** Ce qui se lit mal : hashtags, flèches, puces, symboles. */
    fun clean(t: String): String = t
        .replace(Regex("#\\w+"), "")
        .replace("→", ", ").replace("›", ", ").replace(">", ", ").replace("•", ", ")
        .replace("…", ".").replace("*", "")
        .replace(Regex("\\s*\\.\\s*\\.+"), ".")
        .replace(Regex("\\s+"), " ").trim()

    private val EN = setOf("the", "is", "are", "to", "you", "your", "a", "an", "and", "of", "in", "it", "have", "has", "for", "with",
        "this", "that", "on", "be", "i", "we", "they", "she", "he", "was", "were", "been", "since", "already", "yet", "just", "ever", "never", "how", "when", "use", "do")
    private val FR = setOf("le", "la", "les", "de", "des", "du", "un", "une", "et", "est", "en", "pour", "que", "qui", "dans", "sur", "pas",
        "tu", "ton", "ta", "tes", "vous", "votre", "à", "au", "aux", "ce", "cette", "avec", "se", "son", "sa", "il", "elle", "on", "ne", "plus", "ou")

    /** Langue d'une phrase : "en" si les mots-outils anglais dominent nettement, sinon "fr". */
    fun languageOf(text: String): String {
        val words = text.lowercase().split(Regex("[^a-zàâçéèêëîïôûùüÿœ']+")).filter { it.isNotBlank() }
        val en = words.count { it in EN }
        val fr = words.count { it in FR }
        return if (en >= 2 && en > fr * 1.3) "en" else "fr"
    }

    fun lines(sb: Storyboard, mode: VoiceMode): List<Line> {
        if (mode == VoiceMode.NONE) return emptyList()
        return sb.scenes.mapNotNull { s ->
            val text = textOf(s)
            if (text.length < 3) null
            else Line(s.id, text, when (mode) { VoiceMode.FR -> "fr"; VoiceMode.EN -> "en"; else -> languageOf(text) })
        }
    }

    /** Allonge les scènes pour que chaque phrase tienne dans sa scène. */
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
    fun starts(timeline: Timeline): Map<String, Long> = timeline.slots.associate { it.scene.id to it.startMs + LEAD_MS }

    /**
     * Mélange voix + musique en stéréo : la musique baisse à `duck` (×) quand la voix parle, avec une
     * attaque de 80 ms et un relâchement de 450 ms ; un limiteur doux évite toute saturation.
     */
    fun mix(voice: Pcm, music: Pcm?, totalMs: Long, duck: Float = 0.28f): Pcm {
        val rate = music?.sampleRate ?: 44_100
        val frames = (totalMs * rate / 1000).toInt()
        val v = AudioMix.resample(AudioMix.toMono(voice), rate).samples
        val m = music?.let { if (it.channels == 2) it else Pcm(it.sampleRate, 2, ShortArray(it.frames * 2) { k -> it.samples[k / 2] }) }
        val out = ShortArray(frames * 2)
        val att = 1.0 - exp(-1.0 / (0.080 * rate))
        val rel = 1.0 - exp(-1.0 / (0.450 * rate))
        var env = 0.0
        // Détection de la voix sur une fenêtre glissante : niveau > ~-38 dBFS = la voix parle.
        val win = (rate / 50).coerceAtLeast(1)
        var acc = 0L
        for (i in 0 until frames) {
            val vs = if (i < v.size) v[i].toInt() else 0
            acc += abs(vs)
            if (i >= win) acc -= abs(if (i - win < v.size) v[i - win].toInt() else 0)
            val speaking = (acc / win) > 400
            val target = if (speaking) 1.0 else 0.0
            env += (target - env) * (if (target > env) att else rel)
            val g = 1.0 - (1.0 - duck) * env
            for (ch in 0 until 2) {
                val mv = if (m != null && i < m.frames) m.samples[i * 2 + ch] * g else 0.0
                val s = mv + vs * 0.95
                // Limiteur doux (tanh) au-delà de -3 dBFS
                val x = s / 32768.0
                val y = if (abs(x) < 0.7) x else kotlin.math.sign(x) * (0.7 + 0.3 * kotlin.math.tanh((abs(x) - 0.7) / 0.3))
                out[i * 2 + ch] = (y * 32767).roundToInt().coerceIn(-32768, 32767).toShort()
            }
        }
        return Pcm(rate, 2, out)
    }

    /** Storyboard aux durées adaptées à la voix + piste finale (voix + musique) ; commun Android / navigateur. */
    class Voiced(val storyboard: Storyboard, val audio: Pcm, val speechRate: Float, val languages: Set<String>, val voiceMs: Long)

    private val RATES = floatArrayOf(1.0f, 1.1f, 1.2f, 1.3f)

    /**
     * Synthétise chaque phrase avec [synth], allonge les scènes si besoin, puis mélange voix et musique
     * (la musique baisse sous la voix). Si la vidéo dépasse 52 s, la lecture est un peu accélérée
     * (jusqu'à 1,3×) ; au-delà de 59 s, c'est refusé. [available] : langues que le moteur sait lire.
     */
    suspend fun prepare(
        storyboard: Storyboard, mode: VoiceMode, available: Set<String>,
        synth: suspend (text: String, lang: String, rate: Float) -> Pcm,
        music: suspend (totalMs: Long) -> Pcm?,
        progress: (done: Int, total: Int) -> Unit = { _, _ -> },
        /** `false` (épisodes) : la voix n'est jamais accélérée ; si elle ne tient pas, c'est l'épisode qui est trop long. */
        allowSpeedUp: Boolean = true,
    ): Voiced {
        require(available.isNotEmpty()) { "Aucune voix disponible" }
        val lines = lines(storyboard, mode).map { if (it.lang in available) it else it.copy(lang = available.first()) }
        require(lines.isNotEmpty()) { "Aucune phrase à lire" }
        var best: Pair<Storyboard, Map<String, Pcm>>? = null
        var bestRate = 1f
        var done = 0
        for (rate in if (allowSpeedUp) RATES.toList() else listOf(1.0f)) {
            val clips = LinkedHashMap<String, Pcm>()
            for (l in lines) {
                clips[l.sceneId] = synth(l.text, l.lang, rate)
                progress(++done, lines.size)
            }
            val fitted = fit(storyboard, clips.mapValues { it.value.durationMs })
            val total = runCatching { Timeline.build(fitted).totalMs }.getOrNull() ?: continue
            best = fitted to clips; bestRate = rate
            if (total <= 52_000 || !allowSpeedUp) break
        }
        val (fitted, clips) = best ?: throw StoryboardException(
            if (allowSpeedUp) "Le texte lu dépasse 59 s, même accéléré à 1,3× : raccourcis ton texte."
            else "La voix de cet épisode dépasse 59 s : le texte est découpé en un épisode de plus.")
        val timeline = Timeline.build(fitted)
        val st = starts(timeline)
        val voiceTrack = AudioMix.place(timeline.totalMs, clips.map { (id, pcm) -> st.getValue(id) to pcm })
        val mixed = mix(voiceTrack, music(timeline.totalMs), timeline.totalMs)
        return Voiced(fitted, mixed, bestRate, lines.map { it.lang }.toSet(), clips.values.sumOf { it.durationMs })
    }

    @Suppress("unused") private fun clampMs(x: Long) = max(0L, min(x, Timeline.MAX_DURATION_MS))
}
