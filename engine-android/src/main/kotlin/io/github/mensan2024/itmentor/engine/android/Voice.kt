// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.android

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import io.github.mensan2024.itmentor.engine.Narration
import io.github.mensan2024.itmentor.engine.Pcm
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.StoryboardException
import io.github.mensan2024.itmentor.engine.Timeline
import io.github.mensan2024.itmentor.engine.VoiceMode
import io.github.mensan2024.itmentor.engine.Wav
import io.github.mensan2024.itmentor.engine.AudioMix
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Voix de synthèse du téléphone, utilisée HORS LIGNE uniquement : on ne garde que les voix installées
 * qui ne demandent pas de réseau. Une voix française et une voix anglaise si elles existent.
 */
class OfflineVoices private constructor(private val tts: TextToSpeech, val byLang: Map<String, Voice>) {
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) { pending.remove(utteranceId)?.resume(Unit) }
            @Deprecated("Remplacée par onError(String, Int)", ReplaceWith("onError(utteranceId, -1)"))
            override fun onError(utteranceId: String) = onError(utteranceId, -1)
            override fun onError(utteranceId: String, errorCode: Int) {
                pending.remove(utteranceId)?.resumeWithException(IllegalStateException("Synthèse vocale en échec (code $errorCode)"))
            }
        })
    }

    fun has(lang: String) = lang in byLang

    /** Lit `text` dans un fichier WAV avec la voix de `lang`. */
    suspend fun synthesize(text: String, lang: String, out: File, rate: Float, id: String) {
        tts.voice = byLang[lang] ?: byLang.values.first()
        tts.setSpeechRate(rate)
        out.delete()
        withTimeout(30_000) {
            suspendCancellableCoroutine { cont ->
                pending[id] = cont
                cont.invokeOnCancellation { pending.remove(id) }
                if (tts.synthesizeToFile(text, null, out, id) != TextToSpeech.SUCCESS) {
                    pending.remove(id)
                    cont.resumeWithException(IllegalStateException("La synthèse a été refusée par le moteur vocal"))
                }
            }
        }
    }

    fun shutdown() = tts.shutdown()

    companion object {
        /** Ouvre le moteur vocal ; `null` s'il n'y a aucune voix française ni anglaise hors ligne. */
        suspend fun open(context: Context): OfflineVoices? {
            val holder = arrayOfNulls<TextToSpeech>(1)
            val ok = suspendCancellableCoroutine { cont ->
                holder[0] = TextToSpeech(context.applicationContext) { status -> if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS) }
            }
            val tts = holder[0] ?: return null
            if (!ok) { tts.shutdown(); return null }
            val offline = tts.voices.orEmpty().filter { v ->
                !v.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features.orEmpty()
            }
            fun best(lang: String, country: String) = offline.filter { it.locale.language == lang }
                .sortedWith(compareByDescending<Voice> { it.locale.country == country }.thenByDescending { it.quality })
                .firstOrNull()
            val map = listOfNotNull(best("fr", "FR")?.let { "fr" to it }, best("en", "US")?.let { "en" to it }).toMap()
            if (map.isEmpty()) { tts.shutdown(); return null }
            return OfflineVoices(tts, map)
        }

        /** Écran système d'installation des voix (si une langue manque). */
        fun installIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/** Résultat : storyboard aux durées adaptées à la voix + piste audio finale (voix + musique). */
data class VoicedResult(val storyboard: Storyboard, val audio: File, val speechRate: Float, val languages: Set<String>, val voiceMs: Long)

/**
 * Synthétise chaque phrase, allonge les scènes si besoin, puis mélange voix et musique (la musique baisse
 * sous la voix). Si la vidéo dépasse 38 s, la lecture est un peu accélérée (jusqu'à 1,3×) ; au-delà de 45 s,
 * c'est refusé.
 */
object Narrator {
    private val RATES = floatArrayOf(1.0f, 1.1f, 1.2f, 1.3f)

    suspend fun prepare(
        voices: OfflineVoices, storyboard: Storyboard, mode: VoiceMode, workDir: File,
        music: suspend (totalMs: Long) -> Pcm?,
    ): VoicedResult {
        val lines = Narration.lines(storyboard, mode).map { if (voices.has(it.lang)) it else it.copy(lang = voices.byLang.keys.first()) }
        require(lines.isNotEmpty()) { "Aucune phrase à lire" }
        workDir.mkdirs()
        var best: Pair<Storyboard, Map<String, Pcm>>? = null
        var bestRate = 1f
        for (rate in RATES) {
            val clips = LinkedHashMap<String, Pcm>()
            for (l in lines) {
                val f = File(workDir, "voix_${l.sceneId}.wav")
                voices.synthesize(l.text, l.lang, f, rate, "${l.sceneId}@$rate")
                clips[l.sceneId] = withContext(Dispatchers.IO) { Wav.read(f.readBytes()).also { f.delete() } }
            }
            val fitted = Narration.fit(storyboard, clips.mapValues { it.value.durationMs })
            val total = runCatching { Timeline.build(fitted).totalMs }.getOrNull() ?: continue
            best = fitted to clips; bestRate = rate
            if (total <= 38_000) break
        }
        val (fitted, clips) = best ?: throw StoryboardException("Le texte lu dépasse 45 s, même accéléré à 1,3× : raccourcis ton texte.")
        val timeline = Timeline.build(fitted)
        val starts = Narration.starts(timeline)
        val voiceTrack = AudioMix.place(timeline.totalMs, clips.map { (id, pcm) -> starts.getValue(id) to pcm })
        val mixed = withContext(Dispatchers.Default) { Narration.mix(voiceTrack, music(timeline.totalMs), timeline.totalMs) }
        val out = File(workDir, "voix_et_musique.wav")
        withContext(Dispatchers.IO) { out.writeBytes(Wav.write(mixed)) }
        return VoicedResult(fitted, out, bestRate, lines.map { it.lang }.toSet(), clips.values.sumOf { it.durationMs })
    }
}
