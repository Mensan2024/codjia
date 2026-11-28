// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.Gallery
import io.github.mensan2024.itmentor.engine.AudioMix
import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.Episodes
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Hash64
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.MusicTrack
import io.github.mensan2024.itmentor.engine.MusicVolume
import io.github.mensan2024.itmentor.engine.Pins
import io.github.mensan2024.itmentor.engine.StoryPlan
import io.github.mensan2024.itmentor.engine.Storyboard
import io.github.mensan2024.itmentor.engine.Studio
import io.github.mensan2024.itmentor.engine.TechLevel
import io.github.mensan2024.itmentor.engine.LessonWriter
import io.github.mensan2024.itmentor.engine.VideoFormat
import io.github.mensan2024.itmentor.engine.Wav
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import io.github.mensan2024.itmentor.engine.Narration
import io.github.mensan2024.itmentor.engine.Pcm
import io.github.mensan2024.itmentor.engine.VoiceMode
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO

/**
 * Studio en ligne de commande (phase B) : texte → plan de production (JSON + storyboard lisible) → rendu par scène avec cache.
 *
 *   story plan    <texte.txt> <graine> <dossier> [format]   écrit plan.json et storyboard.md
 *   story replace <plan.json> <scène> <visuel|none> <sortie.json>   remplace le visuel d'UNE scène
 *   story sheet   <plan.json> <image.png>                    une image par scène, épisode par épisode
 *   story render  <plan.json> <dossier> [largeur]            un MP4 par épisode ; seuls les segments modifiés sont ré-encodés
 */
fun storyMain(a: List<String>) {
    val fonts = FontBank(File(System.getProperty("itm.fonts", "engine-android/src/main/assets/fonts")))
    val musicDir = File(System.getProperty("itm.music", "engine-android/src/main/assets/music"))
    when (a.firstOrNull()) {
        "plan" -> {
            val text = File(a[1]).readText()
            val look = a.getOrNull(4)?.let { Look.valueOf(it) } ?: Look.SCENARIO
            val plan = Studio.plan(text, a[2].toLong(), voice = true, look = look)
            val dir = File(a[3]).apply { mkdirs() }
            File(dir, "plan.json").writeText(plan.toJson())
            File(dir, "storyboard.md").writeText(storyboardMarkdown(plan))
            println(summary(plan))
        }
        "ask" -> {
            // ask "<demande courte>" <niveau> <graine> <dossier> : la demande devient une leçon (D.1), puis un plan de production.
            val level = TechLevel.valueOf(a[2].uppercase())
            val (ask, plan) = Studio.ask(a[1], level, a[3].toLong())
            println(ask.note)
            if (plan == null) return
            val dir = File(a[4]).apply { mkdirs() }
            File(dir, "lecon.txt").writeText(LessonWriter.write(ask.card!!, level))
            File(dir, "plan.json").writeText(plan.toJson())
            File(dir, "storyboard.md").writeText(storyboardMarkdown(plan))
            println(summary(plan))
        }
        "timing" -> {
            // timing <plan.json> <sortie.tsv> : une ligne par scène (épisode, id, début ms, fin ms, narration) pour poser une voix off et en mesurer le calage.
            val plan = StoryPlan.fromJson(File(a[1]).readText())
            val music = catalogTrack(plan, musicDir)
            val cards = plan.cards().associateBy { it.id }
            val out = StringBuilder()
            for ((k, sb) in plan.prepared(music).withIndex()) {
                val r = FrameRenderer(sb, VideoFormat(270, 480, 30), plan.template)
                for (sl in r.timeline.slots) out.appendLine("${k + 1}\t${sl.scene.id}\t${sl.startMs}\t${sl.endMs}\t${(cards[sl.scene.id]?.narration ?: "").replace('\t', ' ').replace('\n', ' ')}")
            }
            File(a[2]).writeText(out.toString())
            println("calage : ${out.lines().count { it.isNotBlank() }} scène(s)")
        }
        "why" -> {
            // why <plan.json> <épisode> <scène> : pourquoi chaque plan est admis, refusé ou écarté pour cette scène.
            val plan = StoryPlan.fromJson(File(a[1]).readText())
            plan.why(a[2].toInt(), a[3].toInt()).forEach { println(it) }
        }
        "audit" -> {
            // audit <plan.json> <sortie.md> [dossier-images] : scène par scène, texte / narration / plan et ce qu'il dessine / verdict ; avec un dossier, 3 images par scène.
            val plan = StoryPlan.fromJson(File(a[1]).readText())
            val rows = plan.audit()
            val sb = StringBuilder("# Audit de pertinence\n\n")
            for (r in rows) {
                sb.appendLine("## ${r.episode}.${r.index} `${r.id}` · ${r.layout} · ${"%.1f".format(r.durationMs / 1000.0)} s")
                sb.appendLine("- AFFICHÉ : " + r.shownText.replace("\n", " ⏎ "))
                sb.appendLine("- DIT : " + r.narration)
                sb.appendLine("- sujet/rôle : ${r.concept} · ${r.role} ; indices du texte : ${r.textCues.joinToString(", ")}")
                sb.appendLine("- PLAN : ${r.visualKey ?: "—"} (${r.visualLabel ?: "texte seul"}) note ${r.fit?.let { "%.2f".format(it) } ?: "—"} ; dessine : ${r.depicts.joinToString(", ").ifEmpty { "(générique)" }}")
                sb.appendLine("- VERDICT : ${r.verdict}" + (r.repeat?.let { " ; $it" } ?: ""))
                sb.appendLine()
            }
            File(a[2]).writeText(sb.toString())
            println("audit : ${rows.size} scène(s), ${rows.count { it.verdict.startsWith("REFUSÉ") }} refusée(s), ${rows.count { it.visualKey == null }} sans image")
            a.getOrNull(3)?.let { d ->
                val dir = File(d).apply { mkdirs() }
                val music = catalogTrack(plan, musicDir)
                val tw = 360; val th = 640
                for ((e, sbd) in plan.prepared(music).withIndex()) {
                    val r = FrameRenderer(sbd, VideoFormat(tw, th, 30), plan.template)
                    for ((i, sl) in r.timeline.slots.withIndex()) {
                        val strip = BufferedImage(tw * 3, th, BufferedImage.TYPE_INT_RGB)
                        val g2 = strip.createGraphics()
                        for ((k, f) in listOf(0.25f, 0.6f, 0.95f).withIndex()) {
                            val img = BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB)
                            r.render(Java2DGfx(img, fonts), sl.startMs + (sl.endMs - sl.startMs) * f)
                            g2.drawImage(img, k * tw, 0, null)
                        }
                        ImageIO.write(strip, "png", File(dir, "${e + 1}_${i + 1}_${sl.scene.id}.png"))
                    }
                }
            }
        }
        "replace" -> {
            val plan = StoryPlan.fromJson(File(a[1]).readText())
            val next = plan.replaceVisual(a[2], if (a[3] == "none") Pins.TEXT_ONLY else a[3])
            File(a[4]).writeText(next.toJson())
            println("scène ${a[2]} : ${plan.pins[a[2]]} → ${next.pins[a[2]]} ; ${next.pins.count { (k, v) -> plan.pins[k] != v }} épingle(s) modifiée(s) sur ${next.pins.size}")
            next.qc().filter { it.contains(a[2]) }.forEach { println("  $it") }
        }
        "gallery" -> {
            // gallery <image.png> [filtre] [t_ms] : une vignette 500×350 par plan 2D du registre (cadre virtuel 1000×700).
            val filter = a.getOrNull(2)?.takeIf { it != "-" }
            val tMs = a.getOrNull(3)?.toFloat() ?: 4200f
            val keys = Gallery.keys().filter { filter == null || it.contains(filter) }
            val big = keys.size <= 2
            val tw = if (big) 1000 else 500; val th = if (big) 700 else 350; val cols = if (big) 1 else 3; val rows = (keys.size + cols - 1) / cols
            val sheet = BufferedImage(tw * cols, (th + 22) * rows, BufferedImage.TYPE_INT_RGB)
            val g2 = sheet.createGraphics()
            g2.color = Color(18, 18, 22); g2.fillRect(0, 0, sheet.width, sheet.height)
            g2.font = Font("SansSerif", Font.BOLD, 14)
            for ((i, key) in keys.withIndex()) {
                val img = BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB)
                Gallery.draw(Java2DGfx(img, fonts), key, tMs)
                val x = (i % cols) * tw; val y = (i / cols) * (th + 22)
                g2.drawImage(img.getScaledInstance(tw, th, java.awt.Image.SCALE_SMOOTH), x, y + 22, null)
                g2.color = Color.WHITE; g2.drawString("$key", x + 6, y + 16)
            }
            ImageIO.write(sheet, "png", File(a[1]))
            println("galerie : ${keys.size} plan(s)")
        }
        "sheet" -> {
            val plan = StoryPlan.fromJson(File(a[1]).readText())
            val music = catalogTrack(plan, musicDir)
            val eps = plan.prepared(music)
            val tw = 360; val th = 640; val label = 26
            val cols = eps.maxOf { it.scenes.size }
            val sheet = BufferedImage(tw * cols, (th + label) * eps.size, BufferedImage.TYPE_INT_RGB)
            val g2 = sheet.createGraphics()
            g2.color = Color(18, 18, 22); g2.fillRect(0, 0, sheet.width, sheet.height)
            g2.font = Font("SansSerif", Font.BOLD, 15)
            for ((e, sb) in eps.withIndex()) {
                val r = FrameRenderer(sb, VideoFormat(tw, th, 30), plan.template)
                for ((i, sl) in r.timeline.slots.withIndex()) {
                    val img = BufferedImage(tw, th, BufferedImage.TYPE_INT_RGB)
                    r.render(Java2DGfx(img, fonts), sl.endMs - 900f)
                    g2.drawImage(img, i * tw, e * (th + label) + label, null)
                    g2.color = Color.WHITE
                    g2.drawString("${sb.episode?.index ?: 1}.${i + 1}  ${sl.scene.id}  ${plan.pins[sl.scene.id] ?: ""}".take(46), i * tw + 6, e * (th + label) + 18)
                }
            }
            ImageIO.write(sheet, "png", File(a[2]))
            println("planche : ${eps.size} épisode(s), ${eps.sumOf { it.scenes.size }} scène(s)")
        }
        "render" -> {
            val plan = StoryPlan.fromJson(File(a[1]).readText())
            val dir = File(a[2]).apply { mkdirs() }
            val width = a.getOrNull(3)?.toInt() ?: 1080
            val format = VideoFormat(width, width * 16 / 9, 30)
            val music = catalogTrack(plan, musicDir)
            val cache = File(dir, "cache").apply { mkdirs() }
            val report = StringBuilder()
            val voiceModel = a.getOrNull(4)?.takeIf { it != "-" }
            val musicPcm = music?.let { m ->
                val raw = File(dir, "music.src.wav")
                run("ffmpeg", "-v", "error", "-y", "-i", File(musicDir, m.file.removePrefix("music/")).path, "-ac", "2", "-ar", "44100", "-c:a", "pcm_s16le", raw.path)
                Wav.read(raw.readBytes()).also { raw.delete() }
            }
            for ((k, sb0) in plan.prepared(music).withIndex()) {
                val out = File(dir, "episode_${k + 1}.mp4")
                val t0 = System.nanoTime()
                var sb = sb0
                var voiced: Pcm? = null
                if (voiceModel != null) {
                    val vd = File(dir, "voix").apply { mkdirs() }
                    val v = blocking {
                        Narration.prepare(sb0, VoiceMode.FR, setOf("fr"),
                            synth = { text, _, _ -> piperClip(voiceModel, text, vd) },
                            music = { total -> musicPcm?.let { AudioMix.musicBed(it, total, MusicVolume.NORMAL.gain) } },
                            allowSpeedUp = false)
                    }
                    sb = v.storyboard; voiced = v.audio
                    // Calage mesuré : pour chaque scène, marge entre la fin de la phrase et le début de la scène suivante (≥ 0 = pas de chevauchement).
                    val tl = io.github.mensan2024.itmentor.engine.Timeline.build(sb)
                    val st0 = Narration.starts(tl)
                    val margins = Narration.lines(sb, VoiceMode.FR).map { l ->
                        val slot = tl.slots.first { it.scene.id == l.sceneId }
                        val end = st0.getValue(l.sceneId) + piperClip(voiceModel, l.text, vd).durationMs
                        val nextStart = tl.slots.getOrNull(tl.slots.indexOf(slot) + 1)?.startMs ?: tl.totalMs
                        l.sceneId to (nextStart - end)
                    }
                    println("  calage : marge minimale ${margins.minOf { it.second }} ms (${margins.minByOrNull { it.second }!!.first}) ; " + margins.joinToString(" ") { "${it.first}:${it.second}" })
                    val grown = sb.scenes.zip(sb0.scenes).filter { (x, y) -> x.durationMs != y.durationMs }
                    println("  voix : ${v.voiceMs / 1000.0} s de parole ; ${grown.size} scène(s) allongée(s) " +
                        grown.joinToString(", ") { (x, y) -> "${x.id} +${x.durationMs - y.durationMs} ms" })
                }
                val st = renderSegments(plan, sb, format, fonts, cache, out, music?.let { File(musicDir, it.file.removePrefix("music/")) }, voiced)
                val line = "épisode ${k + 1}/${plan.episodeCount} : ${st.total} segments, ${st.encoded} encodé(s) [${st.encodedIds.joinToString(", ")}], ${st.reused} réutilisé(s), " +
                    "${st.frames} images, ${st.durationMs / 1000.0} s, ${(System.nanoTime() - t0) / 1_000_000_000} s de calcul"
                println(line); report.appendLine(line)
            }
            File(dir, "rendu.txt").writeText(report.toString())
        }
        else -> System.err.println("Usage : story plan|replace|sheet|render …")
    }
}

private fun catalogTrack(plan: StoryPlan, musicDir: File): MusicTrack? =
    plan.musicId?.let { id -> readCatalog(musicDir).firstOrNull { it.id == id } }

private fun summary(plan: StoryPlan): String = buildString {
    appendLine("épisodes : ${plan.episodeCount} · scènes : ${plan.storyboard.scenes.size} · modèle ${plan.templateId} · musique ${plan.musicId}")
    appendLine("fidélité : ${plan.fidelity().summary()}")
    for (ep in plan.series) appendLine("  ${ep.episode?.label ?: "vidéo seule"} : ${ep.scenes.size} scènes, ${Episodes.spanMs(ep.scenes, true) / 1000.0} s avec la voix, ${Composer.totalMs(ep) / 1000.0} s à l'écran")
    val qc = plan.qc()
    appendLine("contrôle qualité : ${qc.size} remarque(s)")
    qc.forEach { appendLine("  $it") }
}

private fun ms(n: Int) = "${n / 1000}.${((n % 1000) / 100)} s"

fun storyboardMarkdown(plan: StoryPlan): String = buildString {
    appendLine("# Storyboard — ${plan.storyboard.title}")
    appendLine()
    appendLine("## Texte original (intégral)")
    appendLine()
    plan.text.lines().forEach { appendLine("> $it") }
    appendLine()
    appendLine("## Synthèse")
    appendLine()
    appendLine("```")
    append(summary(plan))
    appendLine("```")
    val cards = plan.cards()
    for (ep in 1..plan.episodeCount) {
        appendLine()
        appendLine("## Épisode $ep/${plan.episodeCount}")
        for (c in cards.filter { it.episode == ep }) {
            appendLine()
            appendLine("### Scène $ep.${c.index} — `${c.id}` (${c.layout}, ${ms(c.durationMs)})" + if (c.userEdits.isNotEmpty()) "  ✎ modifié : ${c.userEdits.joinToString(", ")}" else "")
            appendLine()
            appendLine("1. **Texte exact** : " + c.text.replace("\n", " ⏎ "))
            appendLine("2. **Narration** : " + c.narration.ifBlank { "—" })
            appendLine("3. **Image (intention visuelle)** : ${c.image}")
            appendLine("4. **Personnages** : " + c.characters.joinToString(", ").ifBlank { "—" })
            appendLine("5. **Objets** : " + c.objects.joinToString(", ").ifBlank { "—" })
            appendLine("6. **Action** : ${c.action ?: "—"}")
            appendLine("7. **Décor** : ${c.setting}")
            appendLine("8. **Animation** : ${c.animation}")
            appendLine("9. **Cadrage** : ${c.framing ?: "—"}")
            appendLine("10. **Mouvement de caméra** : ${c.camera ?: "—"}")
            appendLine("11. **Durée** : ${ms(c.durationMs)}")
            appendLine("12. **Transition** : ${c.transition}")
            appendLine("13. **Son** : ${c.audio}")
            appendLine("- Intention comprise : ${c.intent} · visuel : `${c.visualKey ?: "aucun"}` · empreinte `${c.hash}`")
        }
    }
}

class SegStats(val total: Int, val encoded: Int, val encodedIds: List<String>, val reused: Int, val frames: Int, val durationMs: Long)

/**
 * Rend un épisode scène par scène : chaque segment (de la scène k au début de la scène k+1, fondu de sortie compris) est un MP4
 * mis en cache sous l'empreinte de ce qui le détermine. Modifier une scène ne ré-encode que son segment et celui d'avant (qui contient
 * le fondu vers elle) ; les autres sont réutilisés tels quels, puis tout est assemblé sans ré-encodage.
 */
fun renderSegments(plan: StoryPlan, sb: Storyboard, format: VideoFormat, fonts: FontBank, cache: File, out: File, musicFile: File?, voiced: Pcm? = null): SegStats {
    val r = FrameRenderer(sb, format, plan.template)
    val slots = r.timeline.slots
    val starts = slots.map { it.startMs.toFloat() } + listOf(Float.MAX_VALUE)
    val ranges = ArrayList<IntRange>()
    var f = 0
    for (k in slots.indices) {
        val first = f
        while (f < r.frameCount && r.timeOfFrame(f) < starts[k + 1]) f++
        ranges += first until f
    }
    check(f == r.frameCount) { "images non réparties : $f / ${r.frameCount}" }
    val files = ArrayList<File>()
    val encodedIds = ArrayList<String>()
    for ((k, sl) in slots.withIndex()) {
        val next = slots.getOrNull(k + 1)
        val parts = (listOf(sl.scene.toString(), next?.scene?.toString() ?: "", sb.pins[sl.scene.id], next?.let { sb.pins[it.scene.id] },
            sl.startMs, sl.durationMs, r.timeline.totalMs, k, ranges[k].first, ranges[k].last, plan.look, plan.templateId, plan.seed, sb.mood, sb.profile?.family,
            format.width, format.height, format.fps, sb.episode?.label).joinToString("|"))
        if (System.getProperty("itm.debug") != null && k == 0) File(cache.parentFile, "key0.txt").writeText(parts)
        val key = Hash64.of(parts)
        val file = File(cache, "$key.mp4")
        if (!file.exists() && !ranges[k].isEmpty()) { encodeFrames(r, fonts, format, ranges[k], file); encodedIds += sl.scene.id }
        if (!ranges[k].isEmpty()) files += file
    }
    val list = File(out.parentFile, out.nameWithoutExtension + ".list.txt")
    list.writeText(files.joinToString("\n") { "file '${it.absolutePath}'" })
    val silent = File(out.parentFile, out.nameWithoutExtension + ".video.mp4")
    run("ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", list.path, "-c", "copy", silent.path)
    val tmp = File(out.parentFile, out.nameWithoutExtension + ".tmp.mp4")
    if (voiced != null) {
        val bed = File(out.parentFile, out.nameWithoutExtension + ".bed.wav")
        bed.writeBytes(Wav.write(voiced))
        run("ffmpeg", "-y", "-loglevel", "error", "-i", silent.path, "-i", bed.path, "-map", "0:v", "-map", "1:a", "-c:v", "copy", "-c:a", "aac", "-b:a", "160k", "-shortest",
            "-metadata", "title=${sb.title}${sb.episode?.let { " — " + it.label } ?: ""}", "-metadata", "comment=Créé avec VIDEO24", "-movflags", "+faststart", tmp.path)
        bed.delete()
    } else if (musicFile != null) {
        val raw = File(out.parentFile, out.nameWithoutExtension + ".src.wav")
        run("ffmpeg", "-v", "error", "-y", "-i", musicFile.path, "-ac", "2", "-ar", "44100", "-c:a", "pcm_s16le", raw.path)
        val bed = File(out.parentFile, out.nameWithoutExtension + ".bed.wav")
        bed.writeBytes(Wav.write(AudioMix.musicBed(Wav.read(raw.readBytes()), r.timeline.totalMs, MusicVolume.NORMAL.gain)))
        run("ffmpeg", "-y", "-loglevel", "error", "-i", silent.path, "-i", bed.path, "-map", "0:v", "-map", "1:a", "-c:v", "copy", "-c:a", "aac", "-b:a", "160k", "-shortest",
            "-metadata", "title=${sb.title}${sb.episode?.let { " — " + it.label } ?: ""}", "-metadata", "comment=Créé avec VIDEO24", "-movflags", "+faststart", tmp.path)
        raw.delete(); bed.delete()
    } else run("ffmpeg", "-y", "-loglevel", "error", "-i", silent.path, "-c", "copy", "-movflags", "+faststart", tmp.path)
    check(tmp.renameTo(out) || (out.delete() && tmp.renameTo(out))) { "Impossible de renommer ${tmp.path}" }
    silent.delete(); list.delete()
    return SegStats(slots.size, encodedIds.size, encodedIds, slots.size - encodedIds.size, r.frameCount, r.timeline.totalMs)
}

private fun run(vararg cmd: String) {
    val p = ProcessBuilder(*cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
    check(p.waitFor() == 0) { "${cmd.first()} a échoué" }
}

private fun encodeFrames(r: FrameRenderer, fonts: FontBank, format: VideoFormat, frames: IntRange, out: File) {
    val img = BufferedImage(format.width, format.height, BufferedImage.TYPE_INT_RGB)
    val pixels = (img.raster.dataBuffer as DataBufferInt).data
    val bytes = ByteBuffer.allocate(pixels.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    val tmp = File(out.parentFile, out.nameWithoutExtension + ".part.mp4")
    val cmd = listOf("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "bgr0", "-s", "${format.width}x${format.height}", "-r", "${format.fps}", "-i", "-",
        "-c:v", "libx264", "-preset", "veryfast", "-crf", "17", "-profile:v", "high", "-pix_fmt", "yuv420p", "-r", "${format.fps}", "-an", tmp.path)
    val proc = ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
    val sink = proc.outputStream.buffered(1 shl 20)
    val g = Java2DGfx(img, fonts)
    for (i in frames) {
        g.reset()
        r.renderFrame(g, i)
        bytes.clear(); bytes.asIntBuffer().put(pixels)
        sink.write(bytes.array(), 0, pixels.size * 4)
    }
    sink.close()
    check(proc.waitFor() == 0) { "ffmpeg a échoué" }
    check(tmp.renameTo(out)) { "Impossible de renommer ${tmp.path}" }
}

/** Synthèse locale par le programme `piper` (voix de démonstration de l'atelier ; l'application, elle, lit avec la voix du téléphone). Un clip par phrase, mis en cache. */
private fun piperClip(model: String, text: String, dir: File): Pcm {
    val wav = File(dir, "${Hash64.of(model + "|" + text)}.wav")
    if (!wav.exists()) {
        val tmp = File(dir, wav.name + ".part")
        val p = ProcessBuilder("piper", "-m", model, "-f", tmp.path).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start()
        p.outputStream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        check(p.waitFor() == 0 && tmp.renameTo(wav)) { "piper a échoué sur : $text" }
    }
    return Wav.read(wav.readBytes())
}

/** Exécute une fonction suspendante sans bibliothèque de coroutines (toutes nos suspensions s'achèvent sans changer de fil). */
private fun <T> blocking(block: suspend () -> T): T {
    var out: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { out = it })
    return out!!.getOrThrow()
}
