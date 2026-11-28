// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.Gallery
import io.github.mensan2024.itmentor.engine.Layout
import io.github.mensan2024.itmentor.engine.StoryPlan
import io.github.mensan2024.itmentor.engine.Studio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File

/**
 * Phase C (28/11/2026) : des situations visuelles par domaine (personnages, objets, lieux) au lieu de scènes en texte seul.
 * Les cinq textes d'essai sont dans docs/phaseC/textes. Les seuils sont des objectifs vérifiés ici ; les mesures réelles sont dans les storyboards.
 */
class PhaseCTest {
    private val fonts = FontBank(File(System.getProperty("itm.fonts", "engine-android/src/main/assets/fonts")))
    private val dir = listOf(File("docs/phaseC/textes"), File("../docs/phaseC/textes")).first { it.isDirectory }
    private val names = listOf("1_phishing", "2_sante_mentale", "3_humanitaire", "4_marketing", "5_reseau")
    private val plans: Map<String, StoryPlan> by lazy { names.associateWith { Studio.plan(File(dir, "$it.txt").readText(), 7) } }
    private fun keys(p: StoryPlan) = p.cards().map { it.visualKey }

    @Test fun `aucune scene en texte seul sur les cinq textes`() {
        for ((n, p) in plans) {
            val vides = p.cards().filter { it.visualKey == null }.map { it.id }
            assertTrue("$n : scènes sans image $vides", vides.isEmpty())
        }
    }

    @Test fun `le texte de l'auteur reste integral`() {
        for ((n, p) in plans) {
            val f = p.fidelity()
            assertEquals("$n : mots absents ${f.missing}", 1.0, f.presence, 1e-9)
            assertEquals("$n : mots déplacés ${f.reordered}", 1.0, f.ordered, 1e-9)
        }
    }

    @Test fun `chaque domaine a ses propres situations`() {
        val prefixes = mapOf("1_phishing" to "sit-phishing/", "2_sante_mentale" to "sit-mental/", "3_humanitaire" to "sit-aid/", "4_marketing" to "sit-shop/", "5_reseau" to "sit-net/")
        for ((n, pre) in prefixes) {
            val own = keys(plans.getValue(n)).count { it?.startsWith(pre) == true }
            assertTrue("$n : seulement $own scènes en situation", own >= 6)
            for ((m, p2) in plans) if (m != n) assertFalse("$m utilise les situations de $n", keys(p2).any { it?.startsWith(pre) == true && !it.endsWith("/poster") })
        }
    }

    @Test fun `un plan non repetable ne sert qu'une fois par video`() {
        for ((n, p) in plans) {
            val ks = keys(p).filterNotNull().filterNot { it.endsWith("/poster") }
            assertEquals("$n : plan réutilisé ${ks.groupBy { it }.filter { it.value.size > 1 }.keys}", ks.size, ks.toSet().size)
        }
    }

    @Test fun `pas de demonstration d'un systeme que le texte ne decrit pas`() {
        for ((n, p) in plans) assertTrue("$n : scène de démonstration inventée", p.storyboard.scenes.none { it.layout == Layout.SCREEN })
    }

    @Test fun `les cartes d'ouverture ont un visuel`() {
        for ((n, p) in plans) for (c in p.cards().filter { it.id.endsWith("-open") }) assertTrue("$n ${c.id}", c.visualKey != null)
    }

    @Test fun `tous les plans de situation se dessinent sans erreur et ne sont pas vides`() {
        for (key in Gallery.keys().filter { it.startsWith("sit-") }) {
            val img = BufferedImage(1000, 700, BufferedImage.TYPE_INT_RGB)
            Gallery.draw(Java2DGfx(img, fonts), key, 4200f)
            val first = img.getRGB(0, 0)
            var diff = 0
            for (y in 0 until 700 step 10) for (x in 0 until 1000 step 10) if (img.getRGB(x, y) != first) diff++
            assertTrue("$key : image quasi vide", diff > 400)
        }
    }

    @Test fun `la liste des plans de situation couvre les cinq domaines`() {
        val k = Gallery.keys().filter { it.startsWith("sit-") }
        for (pre in listOf("sit-phishing/", "sit-mental/", "sit-aid/", "sit-shop/", "sit-net/")) assertTrue(pre, k.count { it.startsWith(pre) } >= 7)
    }
}
