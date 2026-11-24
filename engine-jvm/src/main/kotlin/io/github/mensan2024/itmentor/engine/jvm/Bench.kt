// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.DemoStoryboard
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.Styles
import io.github.mensan2024.itmentor.engine.VideoFormat
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/**
 * Banc d'essai JVM. Il exécute le MÊME moteur que l'application Android, avec un rendu Java2D,
 * et encode en H.264 avec l'outil ffmpeg installé sur la machine (outil de développement,
 * jamais embarqué dans l'application).
 *
 *   render  <sortie.mp4> [largeur] [fps]   vidéo complète + mesures JSON
 *   frames  <dossier> <t_ms>...            images PNG à des instants choisis
 *   styles  <dossier> <t_ms>               même instant dans les 3 styles
 */
fun main(args: Array<String>) {
    val fonts = FontBank(File(System.getProperty("itm.fonts", "engine-android/src/main/assets/fonts")))
    when (args.firstOrNull()) {
        "render" -> {
            val out = File(args[1])
            val width = args.getOrNull(2)?.toInt() ?: 1080
            val fps = args.getOrNull(3)?.toInt() ?: 30
            val format = VideoFormat(width, width * 16 / 9, fps)
            val stats = renderVideo(DemoStoryboard.phishing(), format, fonts, out)
            File(out.path.removeSuffix(".mp4") + "-mesures.json").writeText(stats.toJson())
            println(stats.toJson())
        }
        "frames" -> {
            val dir = File(args[1]).apply { mkdirs() }
            val r = FrameRenderer(DemoStoryboard.phishing(), VideoFormat.FULL_HD)
            for (t in args.drop(2)) savePng(r, fonts, t.toFloat(), File(dir, "frame_${t}ms.png"))
        }
        "styles" -> {
            val dir = File(args[1]).apply { mkdirs() }
            for (s in Styles.ALL) {
                val r = FrameRenderer(DemoStoryboard.phishing(s.id), VideoFormat.FULL_HD)
                for (t in args.drop(2)) savePng(r, fonts, t.toFloat(), File(dir, "style_${s.id}_${t}ms.png"))
            }
        }
        else -> System.err.println("Usage : render|frames|styles …")
    }
}

fun savePng(r: FrameRenderer, fonts: FontBank, t: Float, file: File) {
    val img = BufferedImage(r.format.width, r.format.height, BufferedImage.TYPE_INT_RGB)
    val g = Java2DGfx(img, fonts)
    r.render(g, t)
    ImageIO.write(img, "png", file)
}

data class RenderStats(
    val width: Int, val height: Int, val fps: Int, val frames: Int, val durationMs: Long,
    val renderOnlyMs: Long, val wallMs: Long, val avgFrameMs: Double, val p95FrameMs: Double, val maxFrameMs: Double,
    val heapPeakMb: Double, val outputBytes: Long,
) {
    fun toJson() = """{"width":$width,"height":$height,"fps":$fps,"frames":$frames,"durationMs":$durationMs,""" +
        """"renderOnlyMs":$renderOnlyMs,"wallMs":$wallMs,"avgFrameMs":${"%.2f".format(java.util.Locale.ROOT, avgFrameMs)},""" +
        """"p95FrameMs":${"%.2f".format(java.util.Locale.ROOT, p95FrameMs)},"maxFrameMs":${"%.2f".format(java.util.Locale.ROOT, maxFrameMs)},""" +
        """"heapPeakMb":${"%.1f".format(java.util.Locale.ROOT, heapPeakMb)},"outputBytes":$outputBytes}"""
}

fun renderVideo(sb: Storyboard, format: VideoFormat, fonts: FontBank, out: File): RenderStats {
    val renderer = FrameRenderer(sb, format)
    val img = BufferedImage(format.width, format.height, BufferedImage.TYPE_INT_RGB)
    val pixels = (img.raster.dataBuffer as DataBufferInt).data
    val g = Java2DGfx(img, fonts)
    val bytes = ByteBuffer.allocate(pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    val tmp = File(out.parentFile, out.nameWithoutExtension + ".tmp.mp4")
    val cmd = listOf(
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "rawvideo", "-pix_fmt", "bgr0", "-s", "${format.width}x${format.height}", "-r", "${format.fps}", "-i", "-",
        "-c:v", "libx264", "-preset", "medium", "-crf", "17", "-profile:v", "high", "-pix_fmt", "yuv420p",
        "-r", "${format.fps}", "-movflags", "+faststart", tmp.path,
    )
    val proc = ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
    val sink = proc.outputStream.buffered(1 shl 20)
    val mem = ManagementFactory.getMemoryMXBean()
    var heapPeak = 0L
    val times = DoubleArray(renderer.frameCount)
    val wall0 = System.nanoTime()
    var renderNs = 0L
    for (i in 0 until renderer.frameCount) {
        val t0 = System.nanoTime()
        g.reset()
        renderer.renderFrame(g, i)
        val dt = System.nanoTime() - t0
        renderNs += dt
        times[i] = dt / 1e6
        bytes.clear(); bytes.asIntBuffer().put(pixels)
        sink.write(bytes.array(), 0, pixels.size * 4)
        if (i % 15 == 0) heapPeak = maxOf(heapPeak, mem.heapMemoryUsage.used)
    }
    sink.close()
    val code = proc.waitFor()
    check(code == 0) { "ffmpeg a échoué (code $code)" }
    check(tmp.renameTo(out)) { "Impossible de renommer ${tmp.path}" }
    val wall = (System.nanoTime() - wall0) / 1_000_000
    val sorted = times.sorted()
    return RenderStats(
        format.width, format.height, format.fps, renderer.frameCount, renderer.timeline.totalMs,
        renderNs / 1_000_000, wall, times.average(), sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)], sorted.last(),
        heapPeak / 1048576.0, out.length(),
    )
}
