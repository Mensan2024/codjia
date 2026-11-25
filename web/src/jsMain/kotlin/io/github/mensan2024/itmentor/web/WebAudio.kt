// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.Pcm
import kotlinx.browser.window
import kotlin.js.Promise

/**
 * Musique dans le navigateur : catalogue (music.json), décodage (Web Audio), lecture de l'aperçu
 * et conversion en PCM pour le mixage du moteur (le même que sur Android).
 */
class WebAudio(private val base: String) {
    private var ctx: dynamic = null
    private val buffers = HashMap<String, dynamic>()
    private var source: dynamic = null
    private var gain: dynamic = null

    /** Le contexte audio doit être créé après un geste de l'utilisateur (règle des navigateurs, iPhone compris). */
    fun context(): dynamic {
        if (ctx == null) {
            ctx = js("new (window.AudioContext || window.webkitAudioContext)()")
        }
        if (ctx.state == "suspended") ctx.resume()
        return ctx
    }

    suspend fun catalog(): List<MusicTrack> {
        val text = (window.fetch(base + "assets/music/music.json").await().text()).await()
        val arr: dynamic = JSON.parse<dynamic>(text)
        val out = ArrayList<MusicTrack>()
        val n = arr.length as Int
        for (i in 0 until n) {
            val o = arr[i]
            val peaks = ArrayList<Float>()
            val pk = o.peaks
            if (pk != undefined) for (k in 0 until (pk.length as Int)) peaks += (pk[k] as Number).toFloat()
            out += MusicTrack(o.id as String, o.title as String, MusicCategory.valueOf(o.category as String), (o.bpm as Number).toInt(),
                o.file as String, (o.durationMs as Number).toLong(), o.licence as String, o.origin as String, peaks)
        }
        return out
    }

    suspend fun buffer(track: MusicTrack): dynamic {
        val cached: dynamic = buffers[track.id]
        if (cached != null) return cached
        if (track.category == io.github.mensan2024.itmentor.engine.MusicCategory.PERSO) {
            val bytes = MyMusic.bytes(track.file) ?: throw IllegalStateException("Ta musique « ${track.title} » n'est plus disponible : importe-la à nouveau.")
            return decode(track.id, bytes)
        }
        val ab = (window.fetch(base + "assets/" + track.file).await().arrayBuffer()).await()
        val c = context()
        val buf: dynamic = Promise<dynamic> { res, rej -> c.decodeAudioData(ab, { b: dynamic -> res(b) }, { e: dynamic -> rej(Error("Décodage audio impossible : $e")) }) }.await()
        buffers[track.id] = buf
        return buf
    }

    /** Musique importée : décodée une fois, gardée sous son identifiant. */
    suspend fun decode(id: String, bytes: dynamic): dynamic {
        val c = context()
        val buf: dynamic = Promise<dynamic> { res, rej -> c.decodeAudioData(bytes, { b: dynamic -> res(b) }, { e: dynamic -> rej(Error("Ce fichier audio n'est pas lisible par ton navigateur ($e)")) }) }.await()
        buffers[id] = buf
        return buf
    }

    fun forget(id: String) { buffers.remove(id) }

    /** PCM 16 bits → AudioBuffer (pour écouter la voix off mixée dans l'aperçu). */
    fun fromPcm(p: Pcm): dynamic {
        val c = context()
        val frames = p.frames
        val buf: dynamic = c.createBuffer(p.channels, frames, p.sampleRate)
        for (ch in 0 until p.channels) {
            val d: dynamic = buf.getChannelData(ch)
            var i = 0
            while (i < frames) { d[i] = p.samples[i * p.channels + ch] / 32768.0; i++ }
        }
        return buf
    }

    /** Joue la musique à partir de `offsetMs`, au volume donné. */
    fun play(buf: dynamic, offsetMs: Double, volume: Float) {
        stop()
        val c = context()
        val g: dynamic = c.createGain()
        g.gain.value = volume
        g.connect(c.destination)
        val s: dynamic = c.createBufferSource()
        s.buffer = buf
        s.connect(g)
        s.start(0, offsetMs / 1000.0)
        source = s; gain = g
    }

    fun stop() {
        try { source?.stop() } catch (e: Throwable) {}
        try { source?.disconnect(); gain?.disconnect() } catch (e: Throwable) {}
        source = null; gain = null
    }

    /** Horloge audio en millisecondes (pour caler l'image sur le son). */
    fun nowMs(): Double = if (ctx == null) 0.0 else (ctx.currentTime as Double) * 1000.0

    fun suspendAudio() { ctx?.suspend() }
    fun resumeAudio() { ctx?.resume() }

    /** AudioBuffer → PCM 16 bits entrelacé (stéréo au plus). */
    fun toPcm(buf: dynamic): Pcm {
        val channels = minOf(2, buf.numberOfChannels as Int)
        val frames = buf.length as Int
        val rate = buf.sampleRate as Int
        val data = (0 until channels).map { buf.getChannelData(it) }
        val out = ShortArray(frames * channels)
        for (ch in 0 until channels) {
            val d: dynamic = data[ch]
            var i = 0
            while (i < frames) {
                val v = (d[i] as Double).coerceIn(-1.0, 1.0)
                out[i * channels + ch] = (v * 32767).toInt().toShort()
                i++
            }
        }
        return Pcm(rate, channels, out)
    }
}
