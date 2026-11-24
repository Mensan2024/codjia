// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.android

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import io.github.mensan2024.itmentor.engine.AudioMix
import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicChoice
import io.github.mensan2024.itmentor.engine.MusicSource
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.Pcm
import io.github.mensan2024.itmentor.engine.Wav
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Catalogue musical décrit par assets/music/music.json. Ajouter une piste = déposer le fichier et
 * ajouter une entrée (titre, catégorie, tempo, licence, origine) : aucun changement de code.
 */
object MusicCatalog {
    fun load(context: Context): List<MusicTrack> {
        val json = context.assets.open("music/music.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(json)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val peaks = o.optJSONArray("peaks")
            MusicTrack(
                id = o.getString("id"), title = o.getString("title"),
                category = MusicCategory.valueOf(o.getString("category")),
                bpm = o.getInt("bpm"), file = o.getString("file"), durationMs = o.getLong("durationMs"),
                licence = o.getString("licence"), origin = o.getString("origin"),
                peaks = if (peaks == null) emptyList() else List(peaks.length()) { peaks.getDouble(it).toFloat() },
            )
        }
    }

    /**
     * Musique importée par l'utilisateur (sous sa responsabilité). Tempo inconnu : pas de calage au rythme.
     * L'URI est gardé dans `file` avec le préfixe « content: ».
     */
    fun fromUserFile(context: Context, uri: Uri): MusicTrack {
        var name = "Ma musique"
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) name = c.getString(0).substringBeforeLast('.')
        }
        val ex = MediaExtractor()
        var duration = 0L
        try {
            ex.setDataSource(context, uri, null)
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true && f.containsKey(MediaFormat.KEY_DURATION)) {
                    duration = f.getLong(MediaFormat.KEY_DURATION) / 1000
                }
            }
        } finally { ex.release() }
        require(duration > 0) { "Ce fichier ne contient pas de piste audio lisible" }
        return MusicTrack("import:${uri.hashCode()}", name, MusicCategory.DOUCE, 0, uri.toString(), duration,
            "Fichier de l'utilisateur", "Importé par l'utilisateur, sous sa responsabilité", source = MusicSource.USER_IMPORT)
    }
}

/** Décodage d'une piste (AAC, MP3, OGG… selon le téléphone) en PCM 16 bits, avec les codecs d'Android. */
object AudioDecoder {
    fun decode(context: Context, track: MusicTrack): Pcm {
        val ex = MediaExtractor()
        try {
            if (track.source == MusicSource.USER_IMPORT) ex.setDataSource(context, Uri.parse(track.file), null)
            else context.assets.openFd(track.file).use { fd -> ex.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            return decode(ex)
        } finally {
            ex.release()
        }
    }

    private fun decode(ex: MediaExtractor): Pcm {
        val trackIndex = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            ?: error("Aucune piste audio")
        ex.selectTrack(trackIndex)
        val inFormat = ex.getTrackFormat(trackIndex)
        val codec = MediaCodec.createDecoderByType(inFormat.getString(MediaFormat.KEY_MIME)!!)
        var rate = inFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = inFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val out = ByteArrayOutputStream()
        try {
            codec.configure(inFormat, null, null, 0)
            codec.start()
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val ii = codec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val buf = codec.getInputBuffer(ii)!!
                        val n = ex.readSampleData(buf, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    if (info.size > 0) {
                        val ob = codec.getOutputBuffer(oi)!!
                        ob.position(info.offset); ob.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        ob.get(chunk)
                        out.write(chunk)
                    }
                    codec.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val f = codec.outputFormat
                    rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    if (f.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        check(f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_16BIT) { "Format PCM non pris en charge" }
                    }
                }
            }
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
        val bytes = out.toByteArray()
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        // Au-delà de la stéréo : on garde les deux premiers canaux.
        if (channels > 2) {
            val frames = shorts.size / channels
            return Pcm(rate, 2, ShortArray(frames * 2) { shorts[(it / 2) * channels + it % 2] })
        }
        return Pcm(rate, channels, ShortArray((shorts.size / channels) * channels) { shorts[it] })
    }

    /** Prépare la piste finale (fondus, volume, durée exacte de la vidéo) dans un WAV. `null` si la vidéo doit être muette. */
    fun prepareBed(context: Context, choice: MusicChoice, totalMs: Long, workDir: File): File? {
        val track = choice.track ?: return null
        if (!choice.hasAudio) return null
        val bed = AudioMix.musicBed(decode(context, track), totalMs, choice.volume.gain)
        workDir.mkdirs()
        return File(workDir, "musique_video.wav").apply { writeBytes(Wav.write(bed)) }
    }
}
