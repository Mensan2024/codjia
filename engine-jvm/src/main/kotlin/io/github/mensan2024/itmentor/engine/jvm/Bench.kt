// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.AudioMix
import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.MusicCategory
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.MusicVolume
import io.github.mensan2024.itmentor.engine.Samples
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.Templates
import io.github.mensan2024.itmentor.engine.VideoFormat
import io.github.mensan2024.itmentor.engine.Wav
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/**
 * Banc d'essai JVM. Il exécute le MÊME moteur que l'application Android, avec un rendu Java2D,
 * et encode avec l'outil ffmpeg installé sur la machine (outil de développement, jamais embarqué).
 *
 *   render    <sortie.mp4> <template> <musique|aucune> [largeur]   vidéo complète avec musique + mesures JSON
 *   templates <dossier>                                           3 images clés par template
 */
fun main(args: Array<String>) {
    val fonts = FontBank(File(System.getProperty("itm.fonts", "engine-android/src/main/assets/fonts")))
    val musicDir = File(System.getProperty("itm.music", "engine-android/src/main/assets/music"))
    when (args.firstOrNull()) {
        "render" -> {
            val out = File(args[1])
            val tp = Templates.byId(args[2])
            val music = args.getOrNull(3)?.takeIf { it != "aucune" }?.let { readCatalog(musicDir).first { t -> t.id == it } }
            val width = args.getOrNull(4)?.toInt() ?: 1080
            val format = VideoFormat(width, width * 16 / 9, 30)
            val sb = Composer.prepare(Samples.forTemplate(tp), tp, music)
            val stats = renderVideo(sb, format, fonts, out, music?.let { File(musicDir, it.file.removePrefix("music/")) })
            File(out.path.removeSuffix(".mp4") + "-mesures.json").writeText(stats.toJson())
            println(stats.toJson())
        }
        "templates" -> {
            val dir = File(args[1]).apply { mkdirs() }
            for (tp in Templates.ALL) {
                val sb = Composer.prepare(Samples.forTemplate(tp), tp, null)
                val r = FrameRenderer(sb, VideoFormat.FULL_HD, tp)
                val s = r.timeline.slots
                // Couverture, définition, comparaison : à 75 % de chaque scène, quand tout est en place.
                for ((name, slot) in listOf("1-couverture" to s[0], "2-definition" to s[1], "4-comparaison" to s[3], "3-etapes" to s[2], "5-chiffre" to s[4], "6-retenir" to s[5])) {
                    savePng(r, fonts, slot.startMs + slot.durationMs * 0.78f - 300f, File(dir, "${tp.id}_$name.png"))
                }
            }
        }
        "plan" -> {
            val text = File(args[1]).readText()
            val seed = args.getOrNull(2)?.toLong() ?: 0L
            val p = io.github.mensan2024.itmentor.engine.CreativeDirector.propose(text, seed)
            println("Domaine : ${p.domain} · format : ${p.look} · template : ${p.template.id} · musique : ${p.musicId}")
            val tl = io.github.mensan2024.itmentor.engine.Timeline.build(Composer.prepare(p.storyboard, p.template, null))
            println("Durée : ${tl.totalMs} ms")
            if (p.look == io.github.mensan2024.itmentor.engine.Look.SCENARIO || args.getOrNull(3) == "scenario") println(io.github.mensan2024.itmentor.engine.Scenario.report(p.storyboard))
            for (sc in p.storyboard.scenes) {
                println("— ${sc.layout} v${sc.variant} ${sc.durationMs} ms | ${sc.kicker ?: ""} | ${sc.title}")
                if (sc.subtitle != null) println("    sous-titre : ${sc.subtitle}")
                if (sc.body.isNotEmpty()) println("    texte : ${sc.body}")
                sc.items.forEach { println("    • $it") }
                sc.statValue?.let { println("    chiffre : $it") }
                sc.compare?.let { println("    ✗ ${it.badValue.joinToString("") { s -> s.text }}  ✓ ${it.goodValue.joinToString("") { s -> s.text }}") }
            }
        }
        "sheet" -> {
            // Planche : une image au cœur de chaque scène, pour un texte et une graine.
            val text = File(args[1]).readText()
            val seed = args[2].toLong()
            val out = File(args[3])
            val p = io.github.mensan2024.itmentor.engine.CreativeDirector.propose(text, seed, forcedLook = args.getOrNull(4)?.let { io.github.mensan2024.itmentor.engine.Look.valueOf(it) })
            val r = FrameRenderer(Composer.prepare(p.storyboard, p.template, null), VideoFormat(540, 960, 30), p.template)
            val slots = r.timeline.slots
            val sheet = BufferedImage(540 * slots.size, 960, BufferedImage.TYPE_INT_RGB)
            val g2 = sheet.createGraphics()
            for ((i, sl) in slots.withIndex()) {
                val img = BufferedImage(540, 960, BufferedImage.TYPE_INT_RGB)
                r.render(Java2DGfx(img, fonts), sl.endMs - 900f)
                g2.drawImage(img, i * 540, 0, null)
            }
            ImageIO.write(sheet, "png", out)
            println("${p.domain} · ${p.look} · ${p.template.id} · ${p.musicId} · ${r.timeline.totalMs} ms")
        }
        "frames" -> {
            // Images en pleine résolution à des instants donnés : frames <texte> <graine> <format> <out-dir> t1 t2 …
            val p = io.github.mensan2024.itmentor.engine.CreativeDirector.propose(File(args[1]).readText(), args[2].toLong(),
                forcedLook = io.github.mensan2024.itmentor.engine.Look.valueOf(args[3]))
            val r = FrameRenderer(Composer.prepare(p.storyboard, p.template, null), VideoFormat.FULL_HD, p.template)
            val dir = File(args[4]).apply { mkdirs() }
            for (t in args.drop(5)) savePng(r, fonts, t.toFloat(), File(dir, "f_$t.png"))
            println(r.timeline.slots.joinToString { "${it.scene.layout}@${it.startMs}-${it.endMs}" })
        }
        "video" -> {
            val text = File(args[1]).readText()
            val seed = args[2].toLong()
            val out = File(args[3])
            val p = io.github.mensan2024.itmentor.engine.CreativeDirector.propose(text, seed, forcedLook = args.getOrNull(4)?.let { io.github.mensan2024.itmentor.engine.Look.valueOf(it) })
            val music = p.musicId?.let { id -> readCatalog(musicDir).first { it.id == id } }
            val sb = Composer.prepare(p.storyboard, p.template, music)
            val stats = renderVideo(sb, VideoFormat.FULL_HD, fonts, out, music?.let { File(musicDir, it.file.removePrefix("music/")) })
            File(out.path.removeSuffix(".mp4") + "-mesures.json").writeText(stats.toJson())
            println(stats.toJson())
        }
        "story" -> storyMain(args.drop(1))
        else -> System.err.println("Usage : render|templates|plan|sheet|video|story …")
    }
}

/** Lecture minimale de music.json (format connu, généré par tools/music/compose.py). */
fun readCatalog(dir: File): List<MusicTrack> {
    val json = File(dir, "music.json").readText()
    return Regex("\\{[^{}]*\"id\"[^{}]*\\}").findAll(json).map { m ->
        val o = m.value
        fun str(k: String) = Regex("\"$k\":\\s*\"([^\"]*)\"").find(o)!!.groupValues[1]
        fun num(k: String) = Regex("\"$k\":\\s*([0-9.]+)").find(o)!!.groupValues[1]
        val peaks = Regex("\"peaks\":\\s*\\[([^\\]]*)\\]").find(o)?.groupValues?.get(1)?.split(",")?.map { it.trim().toFloat() } ?: emptyList()
        MusicTrack(str("id"), str("title"), MusicCategory.valueOf(str("category")), num("bpm").toInt(), str("file"),
            num("durationMs").toLong(), str("licence"), str("origin"), peaks)
    }.toList()
}

fun savePng(r: FrameRenderer, fonts: FontBank, t: Float, file: File) {
    val img = BufferedImage(r.format.width, r.format.height, BufferedImage.TYPE_INT_RGB)
    r.render(Java2DGfx(img, fonts), t)
    ImageIO.write(img, "png", file)
}

data class RenderStats(
    val width: Int, val height: Int, val fps: Int, val frames: Int, val durationMs: Long,
    val renderOnlyMs: Long, val wallMs: Long, val avgFrameMs: Double, val p95FrameMs: Double, val maxFrameMs: Double,
    val heapPeakMb: Double, val outputBytes: Long, val template: String, val music: String?,
) {
    fun toJson() = """{"template":"$template","music":${music?.let { "\"$it\"" } ?: "null"},"width":$width,"height":$height,"fps":$fps,""" +
        """"frames":$frames,"durationMs":$durationMs,"renderOnlyMs":$renderOnlyMs,"wallMs":$wallMs,""" +
        """"avgFrameMs":${"%.2f".format(java.util.Locale.ROOT, avgFrameMs)},"p95FrameMs":${"%.2f".format(java.util.Locale.ROOT, p95FrameMs)},""" +
        """"maxFrameMs":${"%.2f".format(java.util.Locale.ROOT, maxFrameMs)},"heapPeakMb":${"%.1f".format(java.util.Locale.ROOT, heapPeakMb)},"outputBytes":$outputBytes}"""
}

/** Décode un fichier audio avec ffmpeg en WAV 44,1 kHz stéréo (banc d'essai uniquement). */
private fun decodeWithFfmpeg(src: File, dst: File) {
    val p = ProcessBuilder("ffmpeg", "-v", "error", "-y", "-i", src.path, "-ac", "2", "-ar", "44100", "-c:a", "pcm_s16le", dst.path)
        .redirectErrorStream(true).start()
    check(p.waitFor() == 0) { "Décodage de ${src.name} impossible" }
}

fun renderVideo(sb: Storyboard, format: VideoFormat, fonts: FontBank, out: File, musicFile: File?): RenderStats {
    val tp = Templates.byId(sb.templateId)
    val renderer = FrameRenderer(sb, format, tp)
    // Piste audio : même mixage que sur le téléphone (AudioMix.musicBed), puis encodage AAC.
    val bed = musicFile?.let { src ->
        val raw = File(out.parentFile, out.nameWithoutExtension + ".src.wav")
        decodeWithFfmpeg(src, raw)
        val mixed = AudioMix.musicBed(Wav.read(raw.readBytes()), renderer.timeline.totalMs, MusicVolume.NORMAL.gain)
        raw.delete()
        File(out.parentFile, out.nameWithoutExtension + ".bed.wav").apply { writeBytes(Wav.write(mixed)) }
    }
    val img = BufferedImage(format.width, format.height, BufferedImage.TYPE_INT_RGB)
    val pixels = (img.raster.dataBuffer as DataBufferInt).data
    val bytes = ByteBuffer.allocate(pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    val tmp = File(out.parentFile, out.nameWithoutExtension + ".tmp.mp4")
    val cmd = mutableListOf("ffmpeg", "-y", "-loglevel", "error",
        "-f", "rawvideo", "-pix_fmt", "bgr0", "-s", "${format.width}x${format.height}", "-r", "${format.fps}", "-i", "-")
    if (bed != null) cmd += listOf("-i", bed.path)
    cmd += listOf("-c:v", "libx264", "-preset", "medium", "-crf", "17", "-profile:v", "high", "-pix_fmt", "yuv420p", "-r", "${format.fps}")
    if (bed != null) cmd += listOf("-c:a", "aac", "-b:a", "160k", "-shortest")
    cmd += listOf("-metadata", "title=${sb.title}", "-metadata", "comment=Créé avec VIDEO24", "-movflags", "+faststart", tmp.path)
    val proc = ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
    val sink = proc.outputStream.buffered(1 shl 20)
    val mem = ManagementFactory.getMemoryMXBean()
    var heapPeak = 0L
    val times = DoubleArray(renderer.frameCount)
    val g = Java2DGfx(img, fonts)
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
    check(proc.waitFor() == 0) { "ffmpeg a échoué" }
    bed?.delete()
    check(tmp.renameTo(out)) { "Impossible de renommer ${tmp.path}" }
    val wall = (System.nanoTime() - wall0) / 1_000_000
    val sorted = times.sorted()
    return RenderStats(format.width, format.height, format.fps, renderer.frameCount, renderer.timeline.totalMs,
        renderNs / 1_000_000, wall, times.average(), sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.lastIndex)], sorted.last(),
        heapPeak / 1048576.0, out.length(), tp.id, musicFile?.nameWithoutExtension)
}
