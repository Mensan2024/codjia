// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.web

import io.github.mensan2024.itmentor.engine.AudioMix
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Mp4Muxer
import io.github.mensan2024.itmentor.engine.Pcm
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.Template
import io.github.mensan2024.itmentor.engine.VideoFormat
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.Promise

/**
 * Génération du MP4 dans le navigateur : le moteur dessine chaque image sur un canevas, WebCodecs
 * l'encode en H.264 (matériel quand c'est possible), le son est encodé en AAC (ou Opus si l'AAC
 * n'est pas disponible), puis Mp4Muxer assemble le fichier. Rien ne quitte l'appareil.
 */
object Exporter {
    class Result(val bytes: ByteArray, val width: Int, val height: Int, val audioCodec: String?, val codec: String, val ms: Double)

    fun supported(): Boolean = js("typeof VideoEncoder !== 'undefined' && typeof VideoFrame !== 'undefined'") as Boolean

    private val VIDEO_CODECS = listOf("avc1.640028", "avc1.4D0028", "avc1.42E028", "avc1.42002A")

    /** Choisit le premier réglage vidéo pris en charge, en 1080p puis 720p. */
    suspend fun pickVideo(): Pair<dynamic, VideoFormat>? {
        for (size in listOf(1080 to 1920, 720 to 1280)) for (codec in VIDEO_CODECS) {
            val cfg = obj("codec" to codec, "width" to size.first, "height" to size.second,
                "bitrate" to (if (size.first == 1080) 8_000_000 else 5_000_000), "framerate" to 30, "avc" to obj("format" to "avc"))
            val ok = try {
                val r: dynamic = (js("VideoEncoder").isConfigSupported(cfg) as Promise<dynamic>).await()
                r.supported == true
            } catch (e: Throwable) { false }
            if (ok) return cfg to VideoFormat(size.first, size.second, 30)
        }
        return null
    }

    private suspend fun pickAudio(rate: Int, channels: Int): dynamic {
        if (js("typeof AudioEncoder === 'undefined'") as Boolean) return null
        val candidates = listOf(obj("codec" to "mp4a.40.2", "sampleRate" to rate, "numberOfChannels" to channels, "bitrate" to 160_000),
            obj("codec" to "opus", "sampleRate" to 48000, "numberOfChannels" to channels, "bitrate" to 160_000))
        for (cfg in candidates) {
            val ok = try {
                val r: dynamic = (js("AudioEncoder").isConfigSupported(cfg) as Promise<dynamic>).await()
                r.supported == true
            } catch (e: Throwable) { false }
            if (ok) return cfg
        }
        return null
    }

    suspend fun export(
        sb: Storyboard, template: Template, music: Pcm?, musicGain: Float,
        progress: (done: Int, total: Int) -> Unit, cancelled: () -> Boolean,
    ): Result {
        val started = window.performance.now()
        val (vcfg, format) = pickVideo() ?: throw IllegalStateException("Ton navigateur ne sait pas encoder la vidéo H.264. Mets-le à jour (Chrome, Edge, Safari 16.4+, Firefox 130+).")
        val renderer = FrameRenderer(sb, format, template)
        val totalMs = renderer.timeline.totalMs
        val frames = ((totalMs * format.fps + 999) / 1000).toInt()
        val frameUs = 1_000_000.0 / format.fps

        // ── Son : même mixage que l'application Android (boucle de sécurité, fondu final, volume).
        var pcm: Pcm? = music?.let { AudioMix.musicBed(it, totalMs, musicGain) }
        var acfg: dynamic = null
        if (pcm != null) {
            acfg = pickAudio(pcm.sampleRate, pcm.channels)
            if (acfg != null && (acfg.sampleRate as Int) != pcm.sampleRate) pcm = AudioMix.resample(pcm, acfg.sampleRate as Int)
        }

        // ── Vidéo
        var avcC: ByteArray? = null
        val videoChunks = ArrayList<Triple<ByteArray, Long, Boolean>>()
        var encError: Throwable? = null
        val vOut = { chunk: dynamic, meta: dynamic ->
            val data = ByteArray(chunk.byteLength as Int)
            chunk.copyTo(uint8(data))
            if (avcC == null && meta != undefined && meta.decoderConfig != undefined && meta.decoderConfig.description != undefined) {
                avcC = bytesOf(meta.decoderConfig.description).copyOf()
            }
            val dur = (chunk.duration as? Number)?.toLong() ?: frameUs.toLong()
            videoChunks += Triple(data, dur, chunk.type == "key")
        }
        val vErr = { e: dynamic -> encError = Error("Encodage vidéo : ${e.message}") }
        val venc: dynamic = js("new VideoEncoder({output: vOut, error: vErr})")
        venc.configure(vcfg)

        val canvas: dynamic = document.createElement("canvas")
        canvas.width = format.width
        canvas.height = format.height
        val gfx = CanvasGfx()
        for (i in 0 until frames) {
            if (cancelled()) { try { venc.close() } catch (e: Throwable) {}; throw IllegalStateException("Génération annulée") }
            encError?.let { throw it }
            gfx.begin(canvas)
            renderer.render(gfx, (i * 1000.0 / format.fps).toFloat())
            val ts = (i * frameUs).toLong().toDouble()
            val init = obj("timestamp" to ts, "duration" to frameUs.toLong().toDouble())
            val frame: dynamic = js("new VideoFrame(canvas, init)")
            venc.encode(frame, obj("keyFrame" to (i % (format.fps * 2) == 0)))
            frame.close()
            // Ne pas engorger l'encodeur ; rendre la main à la page pour la barre de progression.
            while ((venc.encodeQueueSize as Int) > 3) sleep(2)
            if (i % 3 == 0) { progress(i, frames); sleep(0) }
        }
        (venc.flush() as Promise<dynamic>).await()
        venc.close()
        encError?.let { throw it }
        val config = avcC ?: throw IllegalStateException("L'encodeur n'a pas fourni la configuration H.264")

        // ── Son
        var audioCodec: String? = null
        val audioChunks = ArrayList<Pair<ByteArray, Long>>()
        var asc: ByteArray? = null
        if (pcm != null && acfg != null) {
            val p = pcm
            val aOut = { chunk: dynamic, meta: dynamic ->
                val data = ByteArray(chunk.byteLength as Int)
                chunk.copyTo(uint8(data))
                if (asc == null && meta != undefined && meta.decoderConfig != undefined && meta.decoderConfig.description != undefined) {
                    asc = bytesOf(meta.decoderConfig.description).copyOf()
                }
                audioChunks += data to ((chunk.duration as? Number)?.toLong() ?: 0L)
            }
            val aErr = { e: dynamic -> encError = Error("Encodage audio : ${e.message}") }
            val aenc: dynamic = js("new AudioEncoder({output: aOut, error: aErr})")
            aenc.configure(acfg)
            val block = 4800
            var f = 0
            val totalFrames = p.frames
            while (f < totalFrames) {
                val n = minOf(block, totalFrames - f)
                val slice = p.samples.copyOfRange(f * p.channels, (f + n) * p.channels)
                val i16: dynamic = slice
                val buffer = i16.buffer; val off = i16.byteOffset; val len = i16.length
                val view: dynamic = js("new Int16Array(buffer, off, len)")
                val init = obj("format" to "s16", "sampleRate" to p.sampleRate, "numberOfFrames" to n, "numberOfChannels" to p.channels,
                    "timestamp" to (f * 1_000_000.0 / p.sampleRate), "data" to view)
                val ad: dynamic = js("new AudioData(init)")
                aenc.encode(ad)
                ad.close()
                f += n
                while ((aenc.encodeQueueSize as Int) > 8) sleep(2)
            }
            (aenc.flush() as Promise<dynamic>).await()
            aenc.close()
            encError?.let { throw it }
            audioCodec = if ((acfg.codec as String).startsWith("mp4a")) "AAC" else "Opus"
        }

        // ── Assemblage MP4
        val audioTrack = if (audioCodec != null && audioChunks.isNotEmpty()) {
            val p = pcm!!
            if (audioCodec == "AAC") Mp4Muxer.AudioTrackConfig(Mp4Muxer.AudioCodec.AAC, p.sampleRate, p.channels, asc ?: ByteArray(0))
            else Mp4Muxer.AudioTrackConfig(Mp4Muxer.AudioCodec.OPUS, 48000, p.channels)
        } else null
        val mux = Mp4Muxer(format.width, format.height, config, audioTrack)
        for ((d, dur, key) in videoChunks) mux.addVideo(d, dur, key)
        if (audioTrack != null) {
            val defaultDur = if (audioTrack.codec == Mp4Muxer.AudioCodec.AAC) 1024L * 1_000_000 / audioTrack.sampleRate else 20_000L
            for ((d, dur) in audioChunks) mux.addAudio(d, if (dur > 0) dur else defaultDur)
        }
        progress(frames, frames)
        return Result(mux.finish(), format.width, format.height, audioCodec.takeIf { audioTrack != null }, vcfg.codec as String,
            window.performance.now() - started)
    }
}
