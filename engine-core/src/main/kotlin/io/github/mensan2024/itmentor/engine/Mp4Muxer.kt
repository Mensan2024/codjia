// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Assemble un fichier MP4 (ISO BMFF) à partir d'échantillons déjà encodés : vidéo H.264 (format AVCC,
 * c'est-à-dire NAL préfixés par leur longueur) et son AAC ou Opus. Sert à la version navigateur, où
 * WebCodecs encode les images et le son mais ne fabrique pas le fichier.
 *
 * Fichier « faststart » : l'index (moov) est placé avant les données (mdat), la vidéo démarre tout de
 * suite à l'ouverture, y compris sur iPhone. Pur Kotlin, sans dépendance.
 */
class Mp4Muxer(
    private val width: Int,
    private val height: Int,
    /** Configuration du décodeur H.264 (boîte avcC), fournie par l'encodeur. */
    private val avcC: ByteArray,
    private val audio: AudioTrackConfig? = null,
) {
    enum class AudioCodec { AAC, OPUS }

    class AudioTrackConfig(
        val codec: AudioCodec, val sampleRate: Int, val channels: Int,
        /** AAC : AudioSpecificConfig ; Opus : ignoré (la boîte dOps est fabriquée ici). */
        val config: ByteArray = ByteArray(0),
        /** Opus : échantillons à ignorer au début (pré-roulement de l'encodeur). */
        val preSkip: Int = 312,
    )

    private class Sample(val data: ByteArray, val durationTicks: Long, val key: Boolean)

    private val videoTimescale = 90_000
    private val video = ArrayList<Sample>()
    private val sound = ArrayList<Sample>()

    /** Ajoute une image encodée (durée en microsecondes). */
    fun addVideo(data: ByteArray, durationUs: Long, keyFrame: Boolean) {
        video += Sample(data, durationUs * videoTimescale / 1_000_000, keyFrame)
    }

    /** Ajoute un paquet audio encodé (durée en microsecondes). */
    fun addAudio(data: ByteArray, durationUs: Long) {
        val a = requireNotNull(audio) { "Pas de piste audio déclarée" }
        sound += Sample(data, durationUs * a.sampleRate / 1_000_000, true)
    }

    fun finish(): ByteArray {
        require(video.isNotEmpty()) { "Aucune image" }
        // 1er passage pour connaître la taille de moov (les décalages stco ont une taille fixe), 2e avec les vrais décalages.
        val headSize = ftyp().size + moov(0).size // début de la boîte mdat
        val moov = moov(headSize.toLong())
        val dataSize = video.sumOf { it.data.size } + sound.sumOf { it.data.size }
        val out = W()
        out.bytes(ftyp())
        out.bytes(moov)
        out.u32(8 + dataSize); out.tag("mdat")
        for (s in video) out.bytes(s.data)
        for (s in sound) out.bytes(s.data)
        return out.toByteArray()
    }

    // ─────────────────────────── boîtes ───────────────────────────

    private class W {
        private var buf = ByteArray(1 shl 16)
        var size = 0
            private set

        private fun need(n: Int) {
            if (size + n <= buf.size) return
            var c = buf.size
            while (c < size + n) c *= 2
            buf = buf.copyOf(c)
        }

        fun u8(v: Int) { need(1); buf[size++] = v.toByte() }
        fun u16(v: Int) { u8(v ushr 8); u8(v) }
        fun u32(v: Int) { u8(v ushr 24); u8(v ushr 16); u8(v ushr 8); u8(v) }
        fun u32(v: Long) = u32(v.toInt())
        fun tag(t: String) { for (ch in t) u8(ch.code) }
        fun bytes(b: ByteArray) { need(b.size); b.copyInto(buf, size); size += b.size }
        fun zeros(n: Int) { repeat(n) { u8(0) } }
        fun toByteArray(): ByteArray = buf.copyOf(size)
    }

    private fun box(type: String, body: W.() -> Unit): ByteArray {
        val inner = W().apply(body).toByteArray()
        return W().apply { u32(8 + inner.size); tag(type); bytes(inner) }.toByteArray()
    }

    private fun fullBox(type: String, version: Int, flags: Int, body: W.() -> Unit): ByteArray =
        box(type) { u8(version); u8(flags ushr 16); u8(flags ushr 8); u8(flags); body() }

    private fun ftyp() = box("ftyp") { tag("isom"); u32(512); tag("isom"); tag("iso2"); tag("avc1"); tag("mp41") }

    private val movieTimescale = 1000
    private fun videoDurationTicks() = video.sumOf { it.durationTicks }
    private fun audioDurationTicks() = sound.sumOf { it.durationTicks }

    private fun moov(mdatStart: Long): ByteArray {
        val vDurMs = videoDurationTicks() * movieTimescale / videoTimescale
        val aDurMs = audio?.let { audioDurationTicks() * movieTimescale / it.sampleRate } ?: 0
        val videoOffset = mdatStart + 8
        val audioOffset = videoOffset + video.sumOf { it.data.size }
        return box("moov") {
            bytes(fullBox("mvhd", 0, 0) {
                u32(0); u32(0); u32(movieTimescale); u32(maxOf(vDurMs, aDurMs))
                u32(0x00010000); u16(0x0100); zeros(10); matrix(); zeros(24); u32(if (audio != null) 3 else 2)
            })
            bytes(trak(1, isVideo = true, offset = videoOffset, durMs = vDurMs))
            if (audio != null && sound.isNotEmpty()) bytes(trak(2, isVideo = false, offset = audioOffset, durMs = aDurMs))
        }
    }

    private fun W.matrix() { u32(0x00010000); u32(0); u32(0); u32(0); u32(0x00010000); u32(0); u32(0); u32(0); u32(0x40000000) }

    private fun trak(id: Int, isVideo: Boolean, offset: Long, durMs: Long): ByteArray = box("trak") {
        val samples = if (isVideo) video else sound
        val timescale = if (isVideo) videoTimescale else audio!!.sampleRate
        bytes(fullBox("tkhd", 0, 3) {
            u32(0); u32(0); u32(id); u32(0); u32(durMs); zeros(8); u16(0); u16(0); u16(if (isVideo) 0 else 0x0100); u16(0)
            matrix(); u32(if (isVideo) width shl 16 else 0); u32(if (isVideo) height shl 16 else 0)
        })
        bytes(box("mdia") {
            bytes(fullBox("mdhd", 0, 0) { u32(0); u32(0); u32(timescale); u32(samples.sumOf { it.durationTicks }); u16(0x55C4); u16(0) })
            bytes(fullBox("hdlr", 0, 0) {
                u32(0); tag(if (isVideo) "vide" else "soun"); zeros(12)
                for (ch in (if (isVideo) "CODJIA video" else "CODJIA audio")) u8(ch.code); u8(0)
            })
            bytes(box("minf") {
                if (isVideo) bytes(fullBox("vmhd", 0, 1) { zeros(8) }) else bytes(fullBox("smhd", 0, 0) { zeros(4) })
                bytes(box("dinf") { bytes(fullBox("dref", 0, 0) { u32(1); bytes(fullBox("url ", 0, 1) {}) }) })
                bytes(stbl(samples, isVideo, offset))
            })
        })
    }

    private fun stbl(samples: List<Sample>, isVideo: Boolean, offset: Long): ByteArray = box("stbl") {
        bytes(fullBox("stsd", 0, 0) { u32(1); bytes(if (isVideo) avc1() else audioEntry()) })
        // stts : durées, regroupées quand elles se suivent à l'identique
        val runs = ArrayList<Pair<Int, Long>>()
        for (s in samples) if (runs.isNotEmpty() && runs.last().second == s.durationTicks) runs[runs.lastIndex] = runs.last().first + 1 to s.durationTicks else runs += 1 to s.durationTicks
        bytes(fullBox("stts", 0, 0) { u32(runs.size); for ((n, d) in runs) { u32(n); u32(d) } })
        if (isVideo) {
            val keys = samples.withIndex().filter { it.value.key }.map { it.index + 1 }
            bytes(fullBox("stss", 0, 0) { u32(keys.size); for (k in keys) u32(k) })
        }
        // un seul bloc (chunk) contigu par piste
        bytes(fullBox("stsc", 0, 0) { u32(1); u32(1); u32(samples.size); u32(1) })
        bytes(fullBox("stsz", 0, 0) { u32(0); u32(samples.size); for (s in samples) u32(s.data.size) })
        bytes(fullBox("stco", 0, 0) { u32(1); u32(offset) })
    }

    private fun avc1(): ByteArray = box("avc1") {
        zeros(6); u16(1); zeros(16); u16(width); u16(height); u32(0x00480000); u32(0x00480000); u32(0); u16(1)
        zeros(32); u16(0x0018); u16(0xFFFF)
        bytes(box("avcC") { bytes(avcC) })
    }

    private fun audioEntry(): ByteArray {
        val a = audio!!
        val entry = if (a.codec == AudioCodec.AAC) "mp4a" else "Opus"
        return box(entry) {
            zeros(6); u16(1); zeros(8); u16(a.channels); u16(16); u16(0); u16(0)
            u32(minOf(a.sampleRate, 65535) shl 16)
            if (a.codec == AudioCodec.AAC) bytes(esds(a)) else bytes(box("dOps") {
                u8(0); u8(a.channels); u16(a.preSkip); u32(a.sampleRate); u16(0); u8(0)
            })
        }
    }

    /** Descripteur MPEG-4 (esds) pour l'AAC. */
    private fun esds(a: AudioTrackConfig): ByteArray {
        val asc = a.config.takeIf { it.isNotEmpty() } ?: aacConfig(a.sampleRate, a.channels)
        fun W.desc(tag: Int, body: ByteArray) { u8(tag); u8(0x80); u8(0x80); u8(0x80); u8(body.size); bytes(body) }
        val dsi = W().apply { bytes(asc) }.toByteArray()
        val dcd = W().apply { u8(0x40); u8(0x15); u8(0); u16(0); u32(160_000); u32(160_000); desc(0x05, dsi) }.toByteArray()
        val sl = byteArrayOf(0x02)
        val es = W().apply { u16(2); u8(0); desc(0x04, dcd); desc(0x06, sl) }.toByteArray()
        return fullBox("esds", 0, 0) { desc(0x03, es) }
    }

    companion object {
        private val AAC_RATES = intArrayOf(96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350)

        /** AudioSpecificConfig AAC-LC (2 octets) quand l'encodeur n'en fournit pas. */
        fun aacConfig(sampleRate: Int, channels: Int): ByteArray {
            val idx = AAC_RATES.indexOf(sampleRate).let { if (it < 0) 4 else it }
            val v = (2 shl 11) or (idx shl 7) or (channels shl 3)
            return byteArrayOf((v ushr 8).toByte(), v.toByte())
        }

        /**
         * Construit avcC à partir des NAL SPS et PPS (utile si l'encodeur rend du format « Annex B »).
         * Tests et secours uniquement.
         */
        fun avcCFrom(sps: ByteArray, pps: ByteArray): ByteArray {
            val w = W()
            w.u8(1); w.u8(sps[1].toInt() and 0xFF); w.u8(sps[2].toInt() and 0xFF); w.u8(sps[3].toInt() and 0xFF)
            w.u8(0xFF); w.u8(0xE1); w.u16(sps.size); w.bytes(sps); w.u8(1); w.u16(pps.size); w.bytes(pps)
            return w.toByteArray()
        }
    }
}
