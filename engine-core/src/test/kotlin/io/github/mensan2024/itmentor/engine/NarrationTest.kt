// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationTest {
    @Test fun detecteLaLangueDeChaquePhrase() {
        assertEquals("en", Narration.languageOf("She has lost her keys, so she can't get in."))
        assertEquals("en", Narration.languageOf("We have worked here since 2020."))
        assertEquals("fr", Narration.languageOf("Utilise des mots de passe robustes et uniques."))
        assertEquals("fr", Narration.languageOf("Active la MFA dès que possible."))
    }

    @Test fun chaqueSceneAUnePhraseLisible() {
        for ((name, text) in Fixtures.ALL) {
            val sb = CreativeDirector.propose(text, 7).storyboard
            val lines = Narration.lines(sb, VoiceMode.AUTO)
            assertEquals("$name : une phrase par scène", sb.scenes.size, lines.size)
            for (l in lines) {
                assertTrue("$name : « ${l.text} »", l.text.length in 3..600)
                assertTrue("$name : symbole illisible dans « ${l.text} »", l.text.none { it in "#*→›•" })
            }
        }
        assertTrue(Narration.lines(CreativeDirector.propose(Fixtures.ALL.values.first(), 7).storyboard, VoiceMode.NONE).isEmpty())
    }

    @Test fun lesScenesSAllongentPourLaVoixSansChevauchement() {
        val sb = CreativeDirector.propose(Fixtures.ALL.values.first(), 3).storyboard
        val voice = sb.scenes.associate { it.id to 3_000L }
        val fitted = Narration.fit(sb, voice)
        val tl = Timeline.build(fitted)
        val starts = Narration.starts(tl)
        for ((i, slot) in tl.slots.withIndex()) {
            val end = starts.getValue(slot.scene.id) + 3_000L
            val limit = tl.slots.getOrNull(i + 1)?.startMs ?: tl.totalMs
            assertTrue("voix de ${slot.scene.id} déborde", end <= limit)
        }
    }

    @Test fun laMusiqueBaisseSousLaVoix() {
        val rate = 8_000
        val music = Pcm(rate, 2, ShortArray(rate * 2 * 2) { 10_000 })            // 2 s de musique constante
        val voice = Pcm(rate, 1, ShortArray(rate) { if (it % 2 == 0) 8_000 else -8_000 }) // 1 s de « voix »
        val out = Narration.mix(voice, music, 2_000)
        val during = out.samples[(rate * 0.8).toInt() * 2].toInt() - voice.samples[(rate * 0.8).toInt()] * 95 / 100
        val after = out.samples[(rate * 1.9).toInt() * 2].toInt()
        assertTrue("musique sous la voix : $during", abs(during) < 5_000)
        assertTrue("musique après la voix : $after", after > 8_000)
    }

    private fun abs(x: Int) = kotlin.math.abs(x)
}
