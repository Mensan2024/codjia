// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

class SecurityTest {
    @Test fun acceptsNormalAudio() {
        assertNull(ImportPolicy.checkAudio(5_000_000, 180_000, 44_100, 2, "audio/mpeg"))
        assertNull(ImportPolicy.checkAudio(null, 180_000, 48_000, 1, "audio/mp4a-latm"))
    }

    @Test fun rejectsHostileAudio() {
        assertNotNull(ImportPolicy.checkAudio(1_000, 10_000, 44_100, 2, "video/mp4"))
        assertNotNull(ImportPolicy.checkAudio(1_000, 10_000, 44_100, 2, null))
        assertNotNull(ImportPolicy.checkAudio(ImportPolicy.MAX_AUDIO_BYTES + 1, 10_000, 44_100, 2, "audio/mpeg"))
        assertNotNull(ImportPolicy.checkAudio(1_000, 0, 44_100, 2, "audio/mpeg"))
        assertNotNull(ImportPolicy.checkAudio(1_000, 4L * 3600_000, 44_100, 2, "audio/mpeg"))
        assertNotNull(ImportPolicy.checkAudio(1_000, 10_000, 1_000_000, 2, "audio/mpeg"))
        assertNotNull(ImportPolicy.checkAudio(1_000, 10_000, 44_100, 64, "audio/mpeg"))
    }

    @Test fun pcmBudgetIsBounded() {
        // 40 s + 3 s de marge, 48 kHz stéréo 16 bits ≈ 8,3 Mo, quelle que soit la longueur du fichier source.
        assertEquals(43_000L * 48_000 / 1000 * 2 * 2, ImportPolicy.pcmByteBudget(40_000, 48_000, 2))
        assertTrue(ImportPolicy.pcmByteBudget(Long.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE) < 40L * 1024 * 1024)
    }

    @Test fun displayNameIsSanitized() {
        assertEquals("Ma chanson", ImportPolicy.displayName("Ma chanson.mp3"))
        assertEquals("a b", ImportPolicy.displayName("a\u0000\nb.ogg"))
        assertEquals("evil3pm.exe", ImportPolicy.displayName("evil‮3pm.exe.mp3"))
        assertEquals("etc passwd", ImportPolicy.displayName("../etc/passwd.wav").removePrefix(".. ").trim())
        assertEquals("Ma musique", ImportPolicy.displayName(null))
        assertTrue(ImportPolicy.displayName("x".repeat(500)).length <= ImportPolicy.MAX_NAME_CHARS)
    }

    @Test fun fileStemIsSafe() {
        assertEquals("etc_passwd", ImportPolicy.safeFileStem("../etc/passwd"))
        assertEquals("Astuce_022_RDP", ImportPolicy.safeFileStem("Astuce 022 — RDP"))
        assertEquals("video", ImportPolicy.safeFileStem("¤¤"))
    }
}
