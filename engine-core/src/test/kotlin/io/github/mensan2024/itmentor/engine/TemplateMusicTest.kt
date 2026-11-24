// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class TemplateTest {
    @Test fun huitTemplatesDistincts() {
        val all = Templates.ALL
        assertEquals(8, all.size)
        assertEquals(8, all.map { it.id }.toSet().size)
        assertEquals("Chaque template a son décor", 8, all.map { it.decor }.toSet().size)
        assertTrue("Au moins 6 typographies de titre différentes", all.map { it.titleFont.family }.toSet().size >= 6)
        assertTrue("Thèmes clairs et sombres", all.any { it.style.dark } && all.any { !it.style.dark })
    }

    @Test fun lesTransitionsVarientEtNeSeRepetentPasDAffilee() {
        for (tp in Templates.ALL) {
            val sb = Templates.apply(Samples.CYBER, tp)
            val types = sb.scenes.dropLast(1).map { it.transitionOut.type }
            assertTrue("${tp.id} : au moins 2 transitions différentes", types.toSet().size >= 2)
            for (i in 1 until types.size) assertNotEquals("${tp.id} : même transition deux fois de suite", types[i - 1], types[i])
            assertEquals(tp.id, sb.templateId)
        }
    }

    @Test fun leContenuNeChangePasAvecLeTemplate() {
        val a = Templates.apply(Samples.SANTE, Templates.MEDICAL_CLEAN)
        val b = Templates.apply(Samples.SANTE, Templates.BOLD_TYPOGRAPHY)
        assertEquals(a.scenes.map { it.title }, b.scenes.map { it.title })
        assertEquals(a.scenes.map { it.items }, b.scenes.map { it.items })
    }

    @Test fun contrasteDuTexteSuffisant() {
        fun lum(c: Int): Double {
            fun ch(v: Int): Double { val s = v / 255.0; return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4) }
            return 0.2126 * ch((c shr 16) and 255) + 0.7152 * ch((c shr 8) and 255) + 0.0722 * ch(c and 255)
        }
        fun ratio(a: Int, b: Int): Double { val x = lum(a); val y = lum(b); return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05) }
        for (tp in Templates.ALL) {
            val s = tp.style
            for (bg in listOf(s.bgTop, s.bgBottom)) {
                assertTrue("${tp.id} texte ${ratio(s.text, bg)}", ratio(s.text, bg) >= 7.0)
                assertTrue("${tp.id} texte secondaire ${ratio(s.textMuted, bg)}", ratio(s.textMuted, bg) >= 4.5)
            }
            assertTrue("${tp.id} pastille d'action ${ratio(s.highlightInk, s.highlight)}", ratio(s.highlightInk, s.highlight) >= 4.5)
        }
    }
}

class DomainTest {
    @Test fun detecteLesDomaines() {
        assertEquals(Domain.CYBER, DomainDetector.detect("Comment repérer un e-mail de phishing qui vole ton mot de passe ?"))
        assertEquals(Domain.SANTE, DomainDetector.detect("Le paludisme : dormir sous moustiquaire et consulter vite en cas de fièvre."))
        assertEquals(Domain.AGRO, DomainDetector.detect("Réussir son compost et améliorer le sol de son champ avant la récolte."))
        assertEquals(Domain.MARKETING, DomainDetector.detect("Attirer des clients sur Facebook : soigner chaque publication de ta marque."))
        assertEquals(Domain.GENERAL, DomainDetector.detect("Bonjour à tous"))
    }

    @Test fun recommandeSansImposer() {
        val rec = DomainDetector.recommend("Protéger son mot de passe et son compte contre les pirates informatiques")
        assertEquals(Templates.CYBER_NEON, rec.first())
        assertEquals(8, rec.size) // tous les templates restent proposés
        assertEquals(Templates.MEDICAL_CLEAN, DomainDetector.recommend(DomainDetector.textOf(Samples.SANTE)).first())
        assertEquals(Templates.GREEN_NATURE, DomainDetector.recommend(DomainDetector.textOf(Samples.AGRO)).first())
    }
}

class MusicTest {
    private fun track(bpm: Int) = MusicTrack("t", "T", MusicCategory.DOUCE, bpm, "music/t.m4a", 48_000, "GPL", "test")

    @Test fun chaqueSceneCommenceSurUnTemps() {
        for (bpm in listOf(70, 80, 96, 100, 110, 118)) for (tp in Templates.ALL) {
            val sb = Composer.prepare(Samples.MARKETING, tp, track(bpm))
            val tl = Timeline.build(sb)
            val beat = 60_000.0 / bpm
            for (s in tl.slots) {
                val beats = s.startMs / beat
                assertTrue("$bpm BPM ${tp.id} : début ${s.startMs} hors temps", abs(beats - Math.round(beats)) * beat <= 1.0)
            }
            assertTrue(tl.totalMs <= 40_000)
        }
    }

    @Test fun volumes() {
        assertTrue(MusicChoice(track(90), MusicVolume.NORMAL).hasAudio)
        assertTrue(MusicChoice(track(90), MusicVolume.REDUIT).hasAudio)
        assertTrue(!MusicChoice(track(90), MusicVolume.COUPE).hasAudio)
        assertTrue(!MusicChoice(null).hasAudio)
    }

    private fun tone(ms: Long, rate: Int = 44_100, ch: Int = 2): Pcm {
        val frames = (ms * rate / 1000).toInt()
        return Pcm(rate, ch, ShortArray(frames * ch) { (10_000 * sin(2 * PI * 220 * (it / ch) / rate)).toInt().toShort() })
    }

    @Test fun fondDeMusiquePlusLongQueLaVideo() {
        val bed = AudioMix.musicBed(tone(48_000), 30_000)
        assertEquals(30_000L, bed.durationMs)
        assertEquals(2, bed.channels)
        assertEquals(0, bed.samples[0].toInt()) // fondu d'entrée
        assertEquals(0, bed.samples[bed.samples.size - 1].toInt()) // fondu de sortie jusqu'au silence
        val mid = bed.samples.copyOfRange(bed.samples.size / 2, bed.samples.size / 2 + 4000).maxOf { abs(it.toInt()) }
        assertTrue(mid > 9_000)
    }

    @Test fun fondDeMusiquePlusCourtQueLaVideoEstReboucle() {
        val bed = AudioMix.musicBed(tone(10_000), 25_000)
        assertEquals(25_000L, bed.durationMs)
        val at20s = 20 * 44_100 * 2
        assertTrue("La musique continue après la fin du fichier", bed.samples.copyOfRange(at20s, at20s + 4000).maxOf { abs(it.toInt()) } > 9_000)
    }

    @Test fun volumeReduit() {
        val n = AudioMix.musicBed(tone(20_000), 10_000, MusicVolume.NORMAL.gain)
        val r = AudioMix.musicBed(tone(20_000), 10_000, MusicVolume.REDUIT.gain)
        val i = n.samples.size / 2
        val pn = n.samples.copyOfRange(i, i + 4000).maxOf { abs(it.toInt()) }
        val pr = r.samples.copyOfRange(i, i + 4000).maxOf { abs(it.toInt()) }
        assertEquals(0.45, pr.toDouble() / pn, 0.02)
    }

    @Test fun wavStereoAllerRetour() {
        val p = tone(300)
        val back = Wav.read(Wav.write(p))
        assertEquals(2, back.channels)
        assertTrue(p.samples.contentEquals(back.samples))
    }
}
