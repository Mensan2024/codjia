// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.android

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import io.github.mensan2024.itmentor.engine.AudioMix
import io.github.mensan2024.itmentor.engine.Narration
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.StoryboardException
import io.github.mensan2024.itmentor.engine.Timeline
import io.github.mensan2024.itmentor.engine.Wav
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Voix de synthèse du téléphone, utilisée uniquement HORS LIGNE (décision D6) :
 * on ne garde que les voix qui ne demandent pas de réseau et dont les données sont installées.
 */
class OfflineVoice private constructor(private val tts: TextToSpeech, val voice: Voice, val engine: String) {

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
        tts.voice = voice
    }

    /** Lit `text` dans un fichier WAV. `rate` = 1.0 pour la vitesse normale. */
    suspend fun synthesize(text: String, out: File, rate: Float, id: String) {
        tts.setSpeechRate(rate)
        out.delete()
        withTimeout(30_000) {
            suspendCancellableCoroutine { cont ->
                pending[id] = cont
                cont.invokeOnCancellation { pending.remove(id) }
                val r = tts.synthesizeToFile(text, null, out, id)
                if (r != TextToSpeech.SUCCESS) {
                    pending.remove(id)
                    cont.resumeWithException(IllegalStateException("La synthèse a été refusée par le moteur vocal"))
                }
            }
        }
    }

    fun shutdown() = tts.shutdown()

    companion object {
        /** Ouvre le moteur vocal et choisit la meilleure voix française hors ligne. `null` s'il n'y en a aucune. */
        suspend fun open(context: Context, language: String = "fr"): OfflineVoice? {
            val holder = arrayOfNulls<TextToSpeech>(1)
            val ok = suspendCancellableCoroutine { cont ->
                holder[0] = TextToSpeech(context.applicationContext) { status -> if (cont.isActive) cont.resume(status == TextToSpeech.SUCCESS) }
            }
            val tts = holder[0] ?: return null
            if (!ok) { tts.shutdown(); return null }
            val best = tts.voices.orEmpty()
                .filter { v ->
                    v.locale.language == language && !v.isNetworkConnectionRequired &&
                        TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features.orEmpty()
                }
                .sortedWith(compareByDescending<Voice> { it.locale.country == "FR" }.thenByDescending { it.quality })
                .firstOrNull()
            if (best == null) { tts.shutdown(); return null }
            return OfflineVoice(tts, best, tts.defaultEngine ?: "inconnu")
        }

        /** Ouvre l'écran système d'installation des données vocales. */
        fun installIntent(): Intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/** Résultat de la préparation de la voix off. */
data class NarratedStoryboard(val storyboard: Storyboard, val audio: File, val speechRate: Float, val voiceName: String, val voiceMs: Long)

/**
 * Synthétise la phrase de chaque scène, adapte les durées des scènes, puis assemble une piste WAV unique.
 * Si la vidéo dépasse 40 s, la synthèse est relancée un peu plus vite (au plus 1,15×), puis abandonnée.
 */
object Narrator {
    private val RATES = floatArrayOf(1.0f, 1.08f, 1.15f)

    suspend fun prepare(voice: OfflineVoice, storyboard: Storyboard, workDir: File): NarratedStoryboard {
        val texts = Narration.texts(storyboard)
        require(texts.isNotEmpty()) { "Aucune scène n'a de voix off" }
        workDir.mkdirs()
        var lastError: StoryboardException? = null
        for (rate in RATES) {
            val clips = LinkedHashMap<String, io.github.mensan2024.itmentor.engine.Pcm>()
            for ((id, text) in texts) {
                val f = File(workDir, "voix_$id.wav")
                voice.synthesize(text, f, rate, "$id@$rate")
                clips[id] = withContext(Dispatchers.IO) { Wav.read(f.readBytes()) }
            }
            val fitted = Narration.fit(storyboard, clips.mapValues { it.value.durationMs })
            val timeline = try { Timeline.build(fitted) } catch (e: StoryboardException) { lastError = e; continue }
            val starts = Narration.starts(timeline)
            val mix = AudioMix.place(timeline.totalMs, clips.map { (id, pcm) -> starts.getValue(id) to pcm })
            val out = File(workDir, "voix_off.wav")
            withContext(Dispatchers.IO) { out.writeBytes(Wav.write(mix)) }
            return NarratedStoryboard(fitted, out, rate, voice.voice.name, clips.values.sumOf { it.durationMs })
        }
        throw StoryboardException("Le texte lu est trop long pour 40 s, même accéléré à 1,15× : ${lastError?.message}")
    }
}
