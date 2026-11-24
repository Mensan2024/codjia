// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.net.Uri
import android.os.Debug
import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import com.google.common.collect.ImmutableList
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.VideoFormat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Mesures relevées pendant un export sur le téléphone. */
data class ExportStats(
    val width: Int, val height: Int, val fps: Int, val frames: Int, val durationMs: Long,
    val wallMs: Long, val renderOnlyMs: Long, val avgFrameMs: Double, val p95FrameMs: Double, val maxFrameMs: Double,
    val javaHeapPeakMb: Double, val nativeHeapPeakMb: Double, val fileBytes: Long, val encoderName: String?,
)

/**
 * Choisit le format selon l'encodeur H.264 du téléphone : 1080 × 1920 à 30 i/s si possible, sinon 720 × 1280.
 * On accepte aussi un encodeur qui ne gère que le paysage (1920 × 1080) : Transformer sait alors pivoter.
 */
object DeviceProfile {
    fun pickFormat(): VideoFormat {
        val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        var portrait = false
        var landscape = false
        for (info: MediaCodecInfo in list.codecInfos) {
            if (!info.isEncoder) continue
            for (type in info.supportedTypes) {
                if (!type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)) continue
                val caps = info.getCapabilitiesForType(type).videoCapabilities ?: continue
                if (caps.areSizeAndRateSupported(1080, 1920, 30.0)) portrait = true
                if (caps.areSizeAndRateSupported(1920, 1080, 30.0)) landscape = true
            }
        }
        return if (portrait || landscape) VideoFormat.FULL_HD else VideoFormat.HD
    }
}

/**
 * Le moteur dessine chaque image dans un Bitmap ; Media3 Transformer l'applique en surimpression
 * (OverlayEffect) sur un fond fixe de la durée de la vidéo, puis encode en H.264 dans un MP4.
 * Media3 gère l'horodatage exact des images, l'encodeur matériel, le repli et le multiplexage.
 */
@UnstableApi
class Media3VideoExporter(private val context: Context) {

    /** Surimpression appelée par Media3 pour chaque image, avec son temps de présentation exact. */
    private class EngineOverlay(private val renderer: FrameRenderer, fonts: AndroidFonts) : BitmapOverlay() {
        private val w = renderer.format.width
        private val h = renderer.format.height
        // Deux bitmaps en alternance : Media3 voit toujours un objet différent et téléverse la nouvelle image.
        private val buffers = arrayOf(Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888), Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888))
        private val canvases = arrayOf(Canvas(buffers[0]), Canvas(buffers[1]))
        private val gfx = AndroidCanvasGfx(fonts)
        private var flip = 0
        private var lastUs = Long.MIN_VALUE
        val frameTimesMs = ArrayList<Double>(1300)
        @Volatile var nativePeak = 0L
        @Volatile var javaPeak = 0L

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            if (presentationTimeUs == lastUs) return buffers[flip]
            lastUs = presentationTimeUs
            flip = 1 - flip
            val t0 = SystemClock.elapsedRealtimeNanos()
            val c = canvases[flip]
            c.drawColor(Color.BLACK)
            gfx.begin(c)
            renderer.render(gfx, presentationTimeUs / 1000f)
            frameTimesMs += (SystemClock.elapsedRealtimeNanos() - t0) / 1e6
            if (frameTimesMs.size % 15 == 0) {
                nativePeak = maxOf(nativePeak, Debug.getNativeHeapAllocatedSize())
                val rt = Runtime.getRuntime()
                javaPeak = maxOf(javaPeak, rt.totalMemory() - rt.freeMemory())
            }
            return buffers[flip]
        }

        fun recycle() = buffers.forEach { it.recycle() }
    }

    /**
     * Exporte le storyboard dans `output` (MP4). Doit être appelé depuis le fil principal :
     * Transformer exige un fil avec Looper. Le rendu lui-même se fait sur le fil de Media3.
     */
    @MainThread
    suspend fun export(
        storyboard: Storyboard, format: VideoFormat, output: File, onProgress: (Int) -> Unit = {},
    ): ExportStats = coroutineScope {
        val renderer = FrameRenderer(storyboard, format) // lève StoryboardException si > 40 s
        val fonts = withContext(Dispatchers.IO) { AndroidFonts(context.assets) }
        val background = withContext(Dispatchers.IO) { backgroundPng(format) }
        val overlay = EngineOverlay(renderer, fonts)

        val effects = Effects(
            /* audioProcessors = */ ImmutableList.of(),
            /* videoEffects = */ ImmutableList.of(
                Presentation.createForWidthAndHeight(format.width, format.height, Presentation.LAYOUT_SCALE_TO_FIT),
                OverlayEffect(ImmutableList.of(overlay)),
            ),
        )
        val item = MediaItem.Builder()
            .setUri(Uri.fromFile(background))
            .setImageDurationMs(renderer.timeline.totalMs)
            .build()
        val edited = EditedMediaItem.Builder(item)
            .setFrameRate(format.fps)
            .setEffects(effects)
            .build()

        // Débit visé : ~0,12 bit par pixel et par image (≈ 7,5 Mb/s en 1080p30), confortable pour du texte net.
        val bitrate = (format.width * format.height * format.fps * 0.12).toInt()
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setEnableFallback(true)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
            .build()

        output.parentFile?.mkdirs()
        val tmp = File(output.parentFile, output.nameWithoutExtension + ".part.mp4")
        tmp.delete()
        val t0 = SystemClock.elapsedRealtime()

        val result: ExportResult = suspendCancellableCoroutine { cont: CancellableContinuation<ExportResult> ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setEncoderFactory(encoderFactory)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (cont.isActive) cont.resume(exportResult)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (cont.isActive) cont.resumeWithException(exportException)
                    }
                })
                .build()
            cont.invokeOnCancellation { transformer.cancel() }
            transformer.start(edited, tmp.absolutePath)

            // Progression : interrogée sur le fil principal, comme l'exige Transformer.
            launch {
                val holder = ProgressHolder()
                while (cont.isActive) {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                    delay(250)
                }
            }
        }
        overlay.recycle()
        check(tmp.renameTo(output)) { "Impossible de finaliser ${output.name}" }
        val wall = SystemClock.elapsedRealtime() - t0
        val times = overlay.frameTimesMs.sorted()
        ExportStats(
            width = format.width, height = format.height, fps = format.fps,
            frames = result.videoFrameCount, durationMs = result.durationMs,
            wallMs = wall, renderOnlyMs = times.sum().toLong(),
            avgFrameMs = times.average(), p95FrameMs = times[(times.size * 0.95).toInt().coerceAtMost(times.lastIndex)],
            maxFrameMs = times.last(),
            javaHeapPeakMb = overlay.javaPeak / 1048576.0, nativeHeapPeakMb = overlay.nativePeak / 1048576.0,
            fileBytes = output.length(), encoderName = result.videoEncoderName,
        )
    }

    /** Fond noir de la taille de sortie, mis en cache (une image de la durée de la vidéo). */
    private fun backgroundPng(format: VideoFormat): File {
        val f = File(context.cacheDir, "itm_bg_${format.width}x${format.height}.png")
        if (!f.exists()) {
            val bmp = Bitmap.createBitmap(format.width, format.height, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.BLACK)
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bmp.recycle()
        }
        return f
    }
}
