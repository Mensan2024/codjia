// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.Mp4Muxer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Le MP4 fabriqué par Mp4Muxer (version navigateur) doit être lu sans erreur par un vrai lecteur :
 * on encode avec ffmpeg (H.264 Annex B, AAC en ADTS, Opus en Ogg), on assemble avec Mp4Muxer, puis
 * ffprobe/ffmpeg vérifient et décodent tout le fichier.
 */
class Mp4MuxerTest {
    private val dir = File(System.getProperty("java.io.tmpdir"), "codjia-mux").apply { mkdirs() }

    private fun run(vararg cmd: String): String {
        val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText()
        assertEquals("Échec : ${cmd.joinToString(" ")}\n$out", 0, p.waitFor())
        return out
    }

    /** Découpe un flux H.264 Annex B en unités NAL. */
    private fun nals(b: ByteArray): List<ByteArray> {
        val starts = ArrayList<Int>()
        var i = 0
        while (i + 3 < b.size) {
            if (b[i].toInt() == 0 && b[i + 1].toInt() == 0 && b[i + 2].toInt() == 1) { starts += i + 3; i += 3 } else i++
        }
        return starts.mapIndexed { k, s ->
            var e = if (k + 1 < starts.size) starts[k + 1] - 3 else b.size
            while (e > s && b[e - 1].toInt() == 0) e--
            b.copyOfRange(s, e)
        }
    }

    private fun video(muxerFor: (ByteArray) -> Mp4Muxer): Mp4Muxer {
        val h264 = File(dir, "v.h264")
        run("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "testsrc=size=1080x1920:rate=30", "-t", "2", "-c:v", "libx264",
            "-profile:v", "high", "-bf", "0", "-g", "30", "-pix_fmt", "yuv420p", "-x264-params", "aud=1", "-f", "h264", h264.path)
        val all = nals(h264.readBytes())
        val sps = all.first { (it[0].toInt() and 0x1F) == 7 }
        val pps = all.first { (it[0].toInt() and 0x1F) == 8 }
        val mux = muxerFor(Mp4Muxer.avcCFrom(sps, pps))
        var au = ArrayList<ByteArray>()
        fun flush() {
            if (au.isEmpty()) return
            val key = au.any { (it[0].toInt() and 0x1F) == 5 }
            val data = au.filter { (it[0].toInt() and 0x1F) !in setOf(7, 8, 9) }
            val size = data.sumOf { 4 + it.size }
            val out = java.io.ByteArrayOutputStream(size)
            for (n in data) { out.write(byteArrayOf((n.size ushr 24).toByte(), (n.size ushr 16).toByte(), (n.size ushr 8).toByte(), n.size.toByte())); out.write(n) }
            mux.addVideo(out.toByteArray(), 1_000_000L / 30, key)
            au = ArrayList()
        }
        for (n in all) { if ((n[0].toInt() and 0x1F) == 9) flush(); au += n }
        flush()
        return mux
    }

    private fun check(f: File, audioCodec: String) {
        val probe = run("ffprobe", "-v", "error", "-show_entries", "stream=codec_name,width,height,duration", "-of", "csv=p=0", f.path)
        assertTrue(probe, probe.contains("h264,1080,1920"))
        assertTrue(probe, probe.contains(audioCodec))
        val decode = run("ffmpeg", "-v", "error", "-i", f.path, "-f", "null", "-")
        assertTrue("Erreurs de décodage : $decode", decode.isBlank())
        val dur = run("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", f.path).trim().toDouble()
        assertEquals(2.0, dur, 0.15)
        // faststart : l'index « moov » est avant les données « mdat »
        val head = String(f.readBytes().copyOfRange(0, 64), Charsets.ISO_8859_1)
        assertTrue(head.indexOf("moov") in 1 until 64)
    }

    @Test fun videoH264EtSonAac() {
        val adts = File(dir, "a.aac")
        run("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100", "-t", "2", "-ac", "2", "-c:a", "aac", "-b:a", "128k", "-f", "adts", adts.path)
        val mux = video { avcC -> Mp4Muxer(1080, 1920, avcC, Mp4Muxer.AudioTrackConfig(Mp4Muxer.AudioCodec.AAC, 44100, 2)) }
        val b = adts.readBytes()
        var i = 0
        while (i + 7 <= b.size) {
            val len = ((b[i + 3].toInt() and 3) shl 11) or ((b[i + 4].toInt() and 0xFF) shl 3) or ((b[i + 5].toInt() and 0xFF) ushr 5)
            val header = if ((b[i + 1].toInt() and 1) == 1) 7 else 9
            mux.addAudio(b.copyOfRange(i + header, i + len), 1024L * 1_000_000 / 44100)
            i += len
        }
        val f = File(dir, "aac.mp4").apply { writeBytes(mux.finish()) }
        check(f, "aac")
    }

    @Test fun videoH264EtSonOpus() {
        val ogg = File(dir, "a.ogg")
        run("ffmpeg", "-v", "error", "-y", "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=48000", "-t", "2", "-ac", "2", "-c:a", "libopus", "-b:a", "128k", "-f", "ogg", ogg.path)
        val mux = video { avcC -> Mp4Muxer(1080, 1920, avcC, Mp4Muxer.AudioTrackConfig(Mp4Muxer.AudioCodec.OPUS, 48000, 2)) }
        // Pages Ogg → paquets Opus (on saute OpusHead et OpusTags)
        val b = ogg.readBytes()
        var i = 0
        var cur = java.io.ByteArrayOutputStream()
        val packets = ArrayList<ByteArray>()
        while (i + 27 <= b.size && String(b, i, 4, Charsets.ISO_8859_1) == "OggS") {
            val segs = b[i + 26].toInt() and 0xFF
            val table = (0 until segs).map { b[i + 27 + it].toInt() and 0xFF }
            var p = i + 27 + segs
            for (l in table) { cur.write(b, p, l); p += l; if (l < 255) { packets += cur.toByteArray(); cur = java.io.ByteArrayOutputStream() } }
            i = p
        }
        for (pk in packets.drop(2)) mux.addAudio(pk, 20_000)
        val f = File(dir, "opus.mp4").apply { writeBytes(mux.finish()) }
        check(f, "opus")
    }
}
