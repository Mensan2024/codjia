// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.android

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Ce que contient réellement le fichier produit, lu sur le téléphone (équivalent de ffprobe). */
data class Mp4Report(
    val mime: String, val width: Int, val height: Int, val durationMs: Long, val sampleCount: Int, val measuredFps: Double,
    /** Rotation inscrite dans le fichier : certains encodeurs n'acceptent que le paysage, Media3 encode alors
     *  en 1920 × 1080 et demande au lecteur de pivoter de 90°. */
    val rotationDegrees: Int = 0,
    /** Piste audio : type, fréquence d'échantillonnage, durée. `null` = vidéo muette. */
    val audioMime: String? = null,
    val audioSampleRate: Int = 0,
    val audioDurationMs: Long = 0,
) {
    val isH264 get() = mime == MediaFormat.MIMETYPE_VIDEO_AVC
    val displayWidth get() = if (rotationDegrees % 180 != 0) height else width
    val displayHeight get() = if (rotationDegrees % 180 != 0) width else height
}

object Mp4Inspector {
    fun inspect(file: File): Mp4Report {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(file.absolutePath)
            var aMime: String? = null
            var aRate = 0
            var aDur = 0L
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val m = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (m.startsWith("audio/")) {
                    aMime = m
                    aRate = if (f.containsKey(MediaFormat.KEY_SAMPLE_RATE)) f.getInteger(MediaFormat.KEY_SAMPLE_RATE) else 0
                    aDur = if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) / 1000 else 0
                }
            }
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: continue
                if (!mime.startsWith("video/")) continue
                ex.selectTrack(i)
                var n = 0
                while (ex.sampleTime >= 0) { n++; ex.advance() }
                val durUs = if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) else 0L
                val rotation = if (f.containsKey(MediaFormat.KEY_ROTATION)) f.getInteger(MediaFormat.KEY_ROTATION) else 0
                return Mp4Report(
                    mime, f.getInteger(MediaFormat.KEY_WIDTH), f.getInteger(MediaFormat.KEY_HEIGHT),
                    durUs / 1000, n, if (durUs > 0) n / (durUs / 1e6) else 0.0, rotation, aMime, aRate, aDur,
                )
            }
            error("Aucune piste vidéo dans ${file.name}")
        } finally {
            ex.release()
        }
    }
}

/**
 * Copie la vidéo là où l'utilisateur la retrouve : Galerie > Movies/VIDEO24 (Android 10 et plus,
 * sans aucune permission), ou dossier Movies de l'application sur Android 8 et 9.
 */
object GalleryExport {
    fun publish(context: Context, video: File, title: String = video.nameWithoutExtension): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, video.name)
                put(MediaStore.Video.Media.TITLE, title)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/VIDEO24")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                ?: error("MediaStore a refusé la création du fichier")
            resolver.openOutputStream(uri).use { out -> video.inputStream().use { it.copyTo(out!!) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val dest = File(dir, video.name)
        video.copyTo(dest, overwrite = true)
        return Uri.fromFile(dest)
    }
}
