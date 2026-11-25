// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.FontCatalog
import io.github.mensan2024.itmentor.engine.FontFamily
import io.github.mensan2024.itmentor.engine.FontSpec
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Moment
import io.github.mensan2024.itmentor.engine.Safe
import io.github.mensan2024.itmentor.engine.Samples
import io.github.mensan2024.itmentor.engine.Templates
import io.github.mensan2024.itmentor.engine.VideoFormat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File

class RenderTest {
    private val fonts = FontBank(File(System.getProperty("itm.fonts", "engine-android/src/main/assets/fonts")))

    private fun renderer(tp: io.github.mensan2024.itmentor.engine.Template, f: VideoFormat) =
        FrameRenderer(Composer.prepare(Samples.forTemplate(tp), tp, null), f, tp)

    private fun render(r: FrameRenderer, t: Float, record: MutableList<Pair<String, Rectangle2D>>? = null): IntArray {
        val img = BufferedImage(r.format.width, r.format.height, BufferedImage.TYPE_INT_RGB)
        val g = Java2DGfx(img, fonts)
        g.recordText = record
        r.render(g, t)
        return (img.raster.dataBuffer as DataBufferInt).data.copyOf()
    }

    @Test fun rendreDeuxFoisLeMemeInstantDonneLaMemeImage() {
        for (tp in Templates.ALL) {
            val r = renderer(tp, VideoFormat.HD)
            for (t in listOf(0f, 5100f, 7777f, 16400f, 30000f)) {
                assertArrayEquals("${tp.id} : image différente à t=$t", render(r, t), render(renderer(tp, VideoFormat.HD), t))
            }
        }
    }

    @Test fun deuxInstantsDifferentsDonnentDesImagesDifferentes() {
        val r = renderer(Templates.CYBER_NEON, VideoFormat.HD)
        assertTrue(!render(r, 1000f).contentEquals(render(r, 1033f)))
    }

    @Test fun toutesLesPolicesCouvrentLeFrancais() {
        val all = buildString {
            for (sb in Samples.ALL) for (s in sb.scenes) {
                append(s.title.uppercase()); append(s.title); append(s.body); append(s.subtitle ?: ""); append(s.kicker ?: ""); append(s.statValue ?: "")
                s.items.forEach { append(it) }
            }
            append("é è ê ë à â ù û ç œ Œ É È À Ç « » ’ … — \u00A0")
        }.replace("*", "")
        for (fam in FontFamily.entries) for (w in FontCatalog.FILES.getValue(fam).keys) {
            val f = fonts.get(FontSpec(fam, w, 40f))
            val missing = f.canDisplayUpTo(all)
            assertEquals("Glyphe manquant dans $fam/$w : « ${all.getOrNull(missing)} »", -1, missing)
        }
    }

    @Test fun toutLeTexteResteDansLaZoneSurePourChaqueTemplate() {
        var checked = 0
        for (tp in Templates.ALL) for (sb in Samples.ALL) {
            val r = FrameRenderer(Composer.prepare(sb, tp, null), VideoFormat.FULL_HD, tp)
            for (slot in r.timeline.slots) {
                val t = slot.endMs - 900f
                if (r.timeline.at(t) !is Moment.Single) continue
                val rec = ArrayList<Pair<String, Rectangle2D>>()
                render(r, t, rec)
                for ((text, b) in rec) {
                    if (b.height > 300) continue // mot géant décoratif du fond (Bold Typography)
                    val ok = b.minX >= Safe.LEFT - 2 && b.maxX <= Safe.RIGHT + 2 && b.minY >= 90.0 && b.maxY <= Safe.BOTTOM
                    assertTrue("« $text » sort de la zone sûre (${tp.id}, ${sb.id}/${slot.scene.id}) : $b", ok)
                    checked++
                }
            }
        }
        println("Zone sûre : $checked textes vérifiés")
        assertTrue(checked > 3000)
    }

    @Test fun lesTextesNeSeChevauchentPas() {
        for (tp in Templates.ALL) for (sb in Samples.ALL) {
            val r = FrameRenderer(Composer.prepare(sb, tp, null), VideoFormat.FULL_HD, tp)
            for (slot in r.timeline.slots) {
                val rec = ArrayList<Pair<String, Rectangle2D>>()
                render(r, slot.endMs - 900f, rec)
                // Lignes de texte : on fusionne les mots d'une même ligne, puis on compare les lignes entre elles.
                val boxes = rec.filter { it.second.height in 1.0..300.0 }.map { it.second }
                for (i in boxes.indices) for (j in i + 1 until boxes.size) {
                    val a = boxes[i]; val b = boxes[j]
                    val ix = minOf(a.maxX, b.maxX) - maxOf(a.minX, b.minX)
                    val iy = minOf(a.maxY, b.maxY) - maxOf(a.minY, b.minY)
                    val overlap = ix > 6 && iy > minOf(a.height, b.height) * 0.45
                    assertTrue("${tp.id} ${sb.id}/${slot.scene.id} : « ${rec[i].first} » chevauche « ${rec[j].first} »", !overlap)
                }
            }
        }
    }

    @Test fun lesVideosGenereesDepuisDuTexteLibreRestentPropres() {
        val dir = listOf(File("engine-core/src/test/resources/textes"), File("../engine-core/src/test/resources/textes")).first { it.isDirectory }
        var checked = 0
        for (f in dir.listFiles()!!.filter { it.extension == "txt" }.sortedBy { it.name }) for (seed in listOf(3L, 4L, 17L)) for (look in io.github.mensan2024.itmentor.engine.Look.entries) {
            val p = io.github.mensan2024.itmentor.engine.CreativeDirector.propose(f.readText(), seed, forcedLook = look)
            val r = FrameRenderer(Composer.prepare(p.storyboard, p.template, null), VideoFormat.FULL_HD, p.template)
            for (slot in r.timeline.slots) {
                val rec = ArrayList<Pair<String, Rectangle2D>>()
                render(r, slot.endMs - 900f, rec)
                val boxes = rec.filter { it.second.height in 1.0..300.0 }
                for ((text, b) in boxes) {
                    val ok = b.minX >= Safe.LEFT - 2 && b.maxX <= Safe.RIGHT + 2 && b.minY >= 90.0 && b.maxY <= Safe.BOTTOM
                    assertTrue("« $text » sort de la zone sûre (${f.name}, graine $seed, $look, ${p.template.id}, ${slot.scene.id} v${slot.scene.variant}) : $b", ok)
                    checked++
                }
                for (i in boxes.indices) for (j in i + 1 until boxes.size) {
                    val a = boxes[i].second; val b = boxes[j].second
                    val ix = minOf(a.maxX, b.maxX) - maxOf(a.minX, b.minX)
                    val iy = minOf(a.maxY, b.maxY) - maxOf(a.minY, b.minY)
                    assertTrue("${f.name}/$seed $look ${p.template.id} ${slot.scene.id} v${slot.scene.variant} : « ${boxes[i].first} » chevauche « ${boxes[j].first} »",
                        !(ix > 6 && iy > minOf(a.height, b.height) * 0.45))
                }
            }
        }
        println("Textes libres : $checked textes vérifiés")
    }

    /** Styles modulables : 24 combinaisons tirées au hasard, vérifiées toutes les 1,2 s sur toute la vidéo. */
    @Test fun lesStylesModulablesRestentPropres() {
        val dir = listOf(File("engine-core/src/test/resources/textes"), File("../engine-core/src/test/resources/textes")).first { it.isDirectory }
        val texts = listOf("astuce_033_phishing_sections.txt", "long_reseau_securise.txt", "anglais_present_perfect.txt", "systemes_linux.txt")
        var checked = 0
        for (seed in 101L..124L) {
            val name = texts[(seed % texts.size).toInt()]
            val p = io.github.mensan2024.itmentor.engine.CreativeDirector.propose(File(dir, name).readText(), seed, forcedLook = io.github.mensan2024.itmentor.engine.Look.KIT)
            val r = FrameRenderer(Composer.prepare(p.storyboard, p.template, null), VideoFormat.FULL_HD, p.template)
            val kit = io.github.mensan2024.itmentor.engine.StyleKit.random(p.storyboard.seed)
            var t = 600f
            while (t < r.timeline.totalMs) {
                if (r.timeline.at(t) is Moment.Single) {
                    val rec = ArrayList<Pair<String, Rectangle2D>>()
                    render(r, t, rec)
                    val boxes = rec.filter { it.second.height in 1.0..300.0 }
                    for ((text, b) in boxes) {
                        val ok = b.minX >= Safe.LEFT - 2 && b.maxX <= Safe.RIGHT + 2 && b.minY >= 90.0 && b.maxY <= Safe.BOTTOM
                        assertTrue("« $text » sort de la zone sûre ($name, graine $seed, ${kit.label}, t=$t) : $b", ok)
                        checked++
                    }
                    for (i in boxes.indices) for (j in i + 1 until boxes.size) {
                        val a = boxes[i].second; val b = boxes[j].second
                        val ix = minOf(a.maxX, b.maxX) - maxOf(a.minX, b.minX)
                        val iy = minOf(a.maxY, b.maxY) - maxOf(a.minY, b.minY)
                        assertTrue("$name graine $seed (${kit.label}, ${kit.header}, ${kit.captions}, ${kit.titleFx}) t=$t : « ${boxes[i].first} » chevauche « ${boxes[j].first} »",
                            !(ix > 6 && iy > minOf(a.height, b.height) * 0.45))
                    }
                }
                t += 1200f
            }
        }
        println("Styles modulables : $checked textes vérifiés")
    }

    @Test fun le720pEstLaMemeCompositionQueLe1080p() {
        val hd = renderer(Templates.MODERN_EDUCATION, VideoFormat.HD)
        val fhd = renderer(Templates.MODERN_EDUCATION, VideoFormat.FULL_HD)
        val a = ArrayList<Pair<String, Rectangle2D>>(); render(hd, 9000f, a)
        val b = ArrayList<Pair<String, Rectangle2D>>(); render(fhd, 9000f, b)
        assertEquals(b.map { it.first }, a.map { it.first })
        for (i in a.indices) assertEquals(b[i].second.minX * 720 / 1080, a[i].second.minX, 2.0)
    }
}
