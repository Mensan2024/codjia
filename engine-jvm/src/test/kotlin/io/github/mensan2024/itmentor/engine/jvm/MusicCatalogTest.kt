// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.Timeline
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MusicCatalogTest {
    private val dir = listOf(File("engine-android/src/main/assets/music"), File("../engine-android/src/main/assets/music")).first { it.isDirectory }
    private val catalog = readCatalog(dir)

    /** La musique ne doit jamais boucler (s'arrêter puis redémarrer) pendant une vidéo. */
    @Test fun chaqueMusiqueEstPlusLongueQueLaPlusLongueVideo() {
        assertTrue(catalog.size >= 10)
        for (t in catalog) {
            assertTrue("${t.id} : ${t.durationMs} ms", t.durationMs >= Timeline.MAX_DURATION_MS + 3_000)
            assertTrue("${t.id} : fichier absent", File(dir, t.file.removePrefix("music/")).isFile)
        }
    }

    /** Toute musique choisie automatiquement existe dans le catalogue. */
    @Test fun lesMusiquesChoisiesExistent() {
        val ids = catalog.map { it.id }.toSet()
        val texts = listOf("Astuce cybersécurité : active la MFA", "Astuce réseau : crée un VLAN sur ton switch Cisco",
            "Astuce santé : bois de l'eau", "English tip: use the present perfect", "Astuce IA : écris un bon prompt",
            "Astuce marketing : publie sur LinkedIn", "Astuce Linux : la commande df -h", "Astuce dev : git status")
        for (t in texts) for (seed in 1L..12L) {
            val id = CreativeDirector.propose(t + "\n- un\n- deux", seed).musicId ?: continue
            assertTrue("$id absent du catalogue", id in ids)
        }
    }
}
