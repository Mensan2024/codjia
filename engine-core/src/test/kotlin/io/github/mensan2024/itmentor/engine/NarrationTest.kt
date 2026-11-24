// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class NarrationTest {
    private fun tone(ms: Long, rate: Int = 22_050, amp: Int = 8000) =
        Pcm(rate, ShortArray((ms * rate / 1000).toInt()) { (amp * sin(2 * PI * 440 * it / rate)).toInt().toShort() })

    @Test fun chaqueScenePossedeUneVoixOff() {
        val sb = DemoStoryboard.phishing()
        assertEquals(sb.scenes.size, Narration.texts(sb).size)
    }

    @Test fun lesScenesSAllongentPourLaVoixSansJamaisRaccourcir() {
        val sb = DemoStoryboard.phishing()
        val voice = mapOf("s1-couverture" to 4200L, "s2-definition" to 7800L)
        val fitted = Narration.fit(sb, voice)
        assertEquals(5500, fitted.scenes[0].durationMs) // 450 + 4200 + 500 + 300 = 5450 < 5500 : inchangé
        assertEquals(450 + 7800 + 600 + 300, fitted.scenes[1].durationMs)
        assertEquals(sb.scenes[2].durationMs, fitted.scenes[2].durationMs)
    }

    @Test fun deuxVoixNeSeChevauchentJamais() {
        val sb = DemoStoryboard.phishing()
        val voice = sb.scenes.associate { it.id to 5000L }
        val tl = Timeline.build(Narration.fit(sb, voice))
        val starts = Narration.starts(tl)
        val ids = sb.scenes.map { it.id }
        for (i in 0 until ids.lastIndex) {
            val endVoice = starts.getValue(ids[i]) + 5000L
            assertTrue("Voix de ${ids[i]} chevauche la suivante", endVoice + Narration.GAP_MS <= starts.getValue(ids[i + 1]))
        }
        assertTrue(starts.getValue(ids.last()) + 5000L + Narration.END_MS - Narration.LEAD_MS <= tl.totalMs + 1)
    }

    @Test fun uneVoixTropLongueFaitRefuserLaVideo() {
        val sb = DemoStoryboard.phishing()
        try {
            Timeline.build(Narration.fit(sb, sb.scenes.associate { it.id to 9000L })); fail()
        } catch (e: StoryboardException) { assertTrue(e.message!!.contains("40")) }
    }

    @Test fun wavAllerRetour() {
        val p = tone(250)
        val back = Wav.read(Wav.write(p))
        assertEquals(p.sampleRate, back.sampleRate)
        assertTrue(p.samples.contentEquals(back.samples))
    }

    @Test fun wavStereoEtTailleProvisoire() {
        val bytes = Wav.write(tone(100)).also { java.nio.ByteBuffer.wrap(it).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(40, 0) }
        assertEquals(tone(100).samples.size, Wav.read(bytes).samples.size)
    }

    @Test fun lesExtraitsSontPlacesAuBonInstantEtNormalises() {
        val mix = AudioMix.place(3000, listOf(500L to tone(1000), 2000L to tone(500)))
        assertEquals(3000L, mix.durationMs)
        val r = mix.sampleRate
        assertEquals(0, mix.samples.take(r * 450 / 1000).maxOf { kotlin.math.abs(it.toInt()) })
        val peak = mix.samples.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue("Crête $peak", peak in 19_000..29_100)
    }

    @Test fun reechantillonnage() {
        val p = AudioMix.resample(tone(1000, 16_000), 24_000)
        assertEquals(24_000, p.samples.size)
    }
}
