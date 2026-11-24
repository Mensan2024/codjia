// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.DemoStoryboard
import io.github.mensan2024.itmentor.engine.FontFamily
import io.github.mensan2024.itmentor.engine.FontSpec
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Moment
import io.github.mensan2024.itmentor.engine.Safe
import io.github.mensan2024.itmentor.engine.Styles
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

    private fun render(r: FrameRenderer, t: Float, record: MutableList<Pair<String, Rectangle2D>>? = null): IntArray {
        val img = BufferedImage(r.format.width, r.format.height, BufferedImage.TYPE_INT_RGB)
        val g = Java2DGfx(img, fonts)
        g.recordText = record
        r.render(g, t)
        return (img.raster.dataBuffer as DataBufferInt).data.copyOf()
    }

    @Test fun rendreDeuxFoisLeMemeInstantDonneLaMemeImage() {
        val r = FrameRenderer(DemoStoryboard.phishing(), VideoFormat.HD)
        for (t in listOf(0f, 5250f, 7777f, 16600f, 31000f)) {
            assertArrayEquals("Image différente à t=$t", render(r, t), render(FrameRenderer(DemoStoryboard.phishing(), VideoFormat.HD), t))
        }
    }

    @Test fun deuxInstantsDifferentsDonnentDesImagesDifferentes() {
        val r = FrameRenderer(DemoStoryboard.phishing(), VideoFormat.HD)
        assertTrue(!render(r, 1000f).contentEquals(render(r, 1033f)))
    }

    @Test fun lesPolicesContiennentTousLesCaracteresDuStoryboard() {
        val sb = DemoStoryboard.phishing()
        val all = buildString {
            for (s in sb.scenes) {
                append(s.title); append(s.body); append(s.subtitle ?: ""); append(s.kicker ?: ""); append(s.statValue ?: "")
                s.items.forEach { append(it) }
            }
            append("é è ê ë à â ù û ç œ Œ É È À Ç « » ’ … —  ")
        }
        for (fam in FontFamily.entries) for (w in listOf(400, 500, 600, 700, 800)) {
            val f = fonts.get(FontSpec(fam, w, 40f))
            val missing = f.canDisplayUpTo(all)
            assertEquals("Glyphe manquant dans $fam/$w : « ${all.getOrNull(missing)} »", -1, missing)
        }
    }

    @Test fun toutLeTexteResteDansLaZoneSure() {
        val r = FrameRenderer(DemoStoryboard.phishing(), VideoFormat.FULL_HD)
        var checked = 0
        // Milieu et fin de chaque scène, hors transitions : tout est à sa place finale.
        for (slot in r.timeline.slots) {
            for (t in listOf(slot.startMs + slot.durationMs * 0.6f, slot.endMs - 700f)) {
                if (r.timeline.at(t) !is Moment.Single) continue
                val rec = ArrayList<Pair<String, Rectangle2D>>()
                render(r, t, rec)
                assertTrue("Aucun texte enregistré (${slot.scene.id})", rec.size >= 4)
                checked += rec.size
                for ((text, b) in rec) {
                    val ok = b.minX >= Safe.LEFT - 2 && b.maxX <= Safe.RIGHT + 2 && b.minY >= 90.0 && b.maxY <= Safe.BOTTOM
                    assertTrue("« $text » sort de la zone sûre (${slot.scene.id}, t=$t) : $b", ok)
                }
            }
        }
        println("Zone sûre : $checked textes vérifiés")
    }

    @Test fun lesTroisStylesSeRendent() {
        for (s in Styles.ALL) {
            val r = FrameRenderer(DemoStoryboard.phishing(s.id), VideoFormat.HD)
            val px = render(r, 3000f)
            assertTrue(px.toSet().size > 500) // une vraie image, pas un aplat
        }
    }

    @Test fun le720pEstLaMemeCompositionQueLe1080p() {
        val hd = FrameRenderer(DemoStoryboard.phishing(), VideoFormat.HD)
        val fhd = FrameRenderer(DemoStoryboard.phishing(), VideoFormat.FULL_HD)
        val a = ArrayList<Pair<String, Rectangle2D>>(); render(hd, 9000f, a)
        val b = ArrayList<Pair<String, Rectangle2D>>(); render(fhd, 9000f, b)
        assertEquals(b.map { it.first }, a.map { it.first })
        for (i in a.indices) assertEquals(b[i].second.minX * 720 / 1080, a[i].second.minX, 2.0)
    }
}
