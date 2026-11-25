// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicTrack
import kotlinx.browser.window
import kotlin.js.Promise

/**
 * Musiques importées par l'utilisateur. Le fichier reste sur l'appareil : il est gardé dans le stockage
 * du navigateur (Cache Storage) pour être retrouvé à la prochaine visite, et n'est jamais envoyé.
 */
object MyMusic {
    private const val CACHE = "codjia-ma-musique-v1"
    private const val PREFIX = "ma-musique/"

    /** Adresse interne (jamais demandée au réseau) : durée en ms puis nom du fichier. */
    private fun key(name: String, ms: Long) = window.location.origin + "/" + PREFIX + ms + "/" + (js("encodeURIComponent")(name) as String)
    private val urls = HashMap<String, String>()

    private fun track(name: String, durationMs: Long) = MusicTrack(
        id = "perso:$name", title = name.substringBeforeLast('.').replace('_', ' ').take(60), category = MusicCategory.PERSO,
        bpm = 0, file = name, durationMs = durationMs, licence = "Ta musique (tu es responsable de ses droits)",
        origin = "Importée depuis ton appareil", peaks = emptyList(),
    )

    /** Musiques déjà importées (durée inconnue tant qu'elles ne sont pas décodées). */
    suspend fun list(): List<MusicTrack> = try {
        val cache: dynamic = (window.asDynamic().caches.open(CACHE) as Promise<dynamic>).await()
        val keys: dynamic = (cache.keys() as Promise<dynamic>).await()
        (0 until (keys.length as Int)).mapNotNull { i ->
            val url = keys[i].url as String
            val rest = url.substringAfter(PREFIX)
            val ms = rest.substringBefore('/').toLongOrNull() ?: 0L
            val name = js("decodeURIComponent")(rest.substringAfter('/')) as String
            if (name.isBlank()) null else { urls[name] = url; track(name, ms) }
        }
    } catch (e: Throwable) { emptyList() }

    /** Garde le fichier choisi et renvoie la piste correspondante, déjà décodée par [audio]. */
    suspend fun import(file: dynamic, audio: WebAudio): MusicTrack {
        val name = (file.name as String).ifBlank { "ma-musique" }
        val size = (file.size as Number).toDouble()
        if (size > 40e6) throw IllegalArgumentException("Fichier trop lourd (${(size / 1e6).toInt()} Mo) : choisis une musique de moins de 40 Mo.")
        val bytes: dynamic = (file.arrayBuffer() as Promise<dynamic>).await()
        val t0 = track(name, 0)
        val buf = audio.decode(t0.id, bytes.slice(0))
        val ms = ((buf.duration as Number).toDouble() * 1000).toLong()
        try {
            val cache: dynamic = (window.asDynamic().caches.open(CACHE) as Promise<dynamic>).await()
            val type = (file.type as? String)?.ifBlank { null } ?: "audio/mpeg"
            val init = obj("headers" to obj("content-type" to type))
            remove(name)
            val url = key(name, ms)
            val request: dynamic = newRequest(url)
            val response: dynamic = newResponse(bytes, init)
            (cache.put(request, response) as Promise<dynamic>).await()
            urls[name] = url
        } catch (e: Throwable) { console.warn("Musique non gardée pour la prochaine visite", e) }
        return t0.copy(durationMs = ms)
    }

    suspend fun bytes(name: String): dynamic {
        return try {
            val cache: dynamic = (window.asDynamic().caches.open(CACHE) as Promise<dynamic>).await()
            val url = urls[name] ?: run { list(); urls[name] } ?: return null
            val res: dynamic = (cache.match(url) as Promise<dynamic>).await()
            if (res == null || res == undefined) null else (res.arrayBuffer() as Promise<dynamic>).await()
        } catch (e: Throwable) { null }
    }

    suspend fun remove(name: String) {
        try {
            val cache: dynamic = (window.asDynamic().caches.open(CACHE) as Promise<dynamic>).await()
            if (name !in urls) list()
            val url = urls.remove(name) ?: return
            (cache.delete(url) as Promise<dynamic>).await()
        } catch (e: Throwable) { /* rien à retirer */ }
    }

    private fun newRequest(url: String): dynamic = js("new Request(url)")
    private fun newResponse(body: dynamic, init: dynamic): dynamic = js("new Response(body, init)")
}
