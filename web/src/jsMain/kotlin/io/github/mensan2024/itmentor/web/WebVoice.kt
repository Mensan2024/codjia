// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.Pcm
import kotlinx.browser.document
import kotlin.js.Promise

/**
 * Voix off dans le navigateur : voix neuronales Piper (français « siwis », anglais « alba »), exécutées sur
 * l'appareil par voix/codjia-voice.js. Le script n'est chargé que si l'utilisateur active la voix.
 */
class WebVoice(private val base: String) {
    private var loaded: Promise<dynamic>? = null

    private fun api(): Promise<dynamic> = loaded ?: Promise<dynamic> { res, rej ->
        val s = document.createElement("script").asDynamic()
        s.src = base + "voix/codjia-voice.js"
        s.onload = { res(js("window.CodjiaVoice")) }
        s.onerror = { loaded = null; rej(Error("La voix n'a pas pu être chargée : vérifie ta connexion et réessaie.")) }
        document.head!!.appendChild(s)
    }.also { loaded = it }

    /** Langues proposées par le moteur. */
    val languages = setOf("fr", "en")

    /** true si la voix est déjà sur l'appareil (pas de téléchargement à prévoir). */
    suspend fun ready(lang: String): Boolean = try { (api().await().isCached(lang) as Promise<Boolean>).await() } catch (e: Throwable) { false }

    /** Lit `text` ; [download] reçoit l'avancement du premier téléchargement (octets reçus, total). */
    suspend fun synth(text: String, lang: String, rate: Float, download: (Double, Double) -> Unit): Pcm {
        val v = api().await()
        val cb = { got: Double, total: Double -> download(got, total) }
        val r: dynamic = (v.synthesize(text, lang, rate, cb) as Promise<dynamic>).await()
        val f: dynamic = r.samples
        val n = f.length as Int
        val out = ShortArray(n)
        var i = 0
        while (i < n) {
            val x = (f[i] as Double).coerceIn(-1.0, 1.0)
            out[i] = (x * 32767).toInt().toShort()
            i++
        }
        return Pcm(r.sampleRate as Int, 1, out)
    }
}
