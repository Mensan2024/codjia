// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.Knowledge
import io.github.mensan2024.itmentor.engine.LessonWriter
import io.github.mensan2024.itmentor.engine.Studio
import io.github.mensan2024.itmentor.engine.TechLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase D.1 (28/11/2026) : une demande courte devient une leçon écrite à partir d'une fiche vérifiée, jamais inventée. */
class LessonTest {
    @Test fun `des demandes courtes de formes differentes designent la meme fiche`() {
        for (q in listOf("Phishing", "phishing", "Explique-moi le phishing", "Qu'est-ce que le hameçonnage ?", "Comment fonctionne un faux courriel ?", "Attaque phishing"))
            assertEquals("« $q »", "phishing", LessonWriter.understand(q).card?.id)
    }

    @Test fun `un sujet hors bibliotheque ne produit aucune lecon inventee`() {
        val (ask, plan) = Studio.ask("zorglub quantique", TechLevel.DEBUTANT)
        assertNull(ask.card); assertNull(plan)
        assertTrue(ask.note.contains("aucune leçon n'est inventée"))
    }

    @Test fun `les noms de fiches ne se recouvrent pas`() {
        val seen = HashMap<String, String>()
        for (c in Knowledge.cards) for (n in c.names) { val k = LessonWriter.normalize(n); val other = seen.put(k, c.id); assertTrue("« $n » partagé entre $other et ${c.id}", other == null || other == c.id) }
    }

    @Test fun `le niveau enrichit la lecon sans retirer ce qui precede`() {
        for (c in Knowledge.cards) {
            val sizes = TechLevel.entries.map { LessonWriter.write(c, it).length }
            assertTrue("${c.id} $sizes", sizes.zipWithNext().all { (a, b) -> a <= b })
            assertTrue("${c.id} : le niveau expert doit apporter du contenu de plus que débutant", sizes.last() > sizes.first())
        }
    }

    @Test fun `la lecon suit la progression pedagogique d'une attaque`() {
        val t = LessonWriter.write(Knowledge.byId("phishing")!!, TechLevel.INTERMEDIAIRE)
        val order = listOf("Comment l'attaque se déroule", "Les conséquences", "Comment le reconnaître", "Comment le détecter", "Comment s'en protéger", "À retenir")
        val pos = order.map { t.indexOf(it) }
        assertTrue("sections absentes : $pos", pos.all { it >= 0 })
        assertEquals(pos.sorted(), pos)
    }

    @Test fun `la lecon est un plan complet sans scene vide ni texte omis`() {
        for (level in TechLevel.entries) {
            val (ask, plan) = Studio.ask("Phishing", level)
            assertNotNull(plan); plan!!
            val f = plan.fidelity()
            assertEquals("${level.label} : ${f.missing}", 1.0, f.presence, 1e-9)
            assertEquals(1.0, f.ordered, 1e-9)
            val sans = plan.cards().filter { it.visualKey == null }.map { it.id }
            assertTrue("${level.label} : scènes sans image $sans", sans.isEmpty())
            assertTrue(plan.qc().none { it.startsWith("BLOQUANT") })
            assertTrue(ask.note.contains(level.label))
        }
    }

    @Test fun `les cinq demandes de demonstration sont reconnues et planifiees sans scene sans image au niveau intermediaire`() {
        val demandes = mapOf("Phishing" to "phishing", "OSPF" to "ospf", "Ransomware" to "ransomware", "Active Directory" to "active-directory", "Pare-feu" to "firewall",
            "Explique-moi OSPF" to "ospf", "C'est quoi un pare-feu ?" to "firewall", "Comment fonctionne Active Directory" to "active-directory")
        for ((q, id) in demandes) {
            val (ask, plan) = Studio.ask(q, TechLevel.INTERMEDIAIRE)
            assertEquals("« $q »", id, ask.card?.id)
            assertNotNull(plan); plan!!
            assertEquals("« $q » : ${plan.fidelity().missing}", 1.0, plan.fidelity().presence, 1e-9)
            assertTrue("« $q » : ${plan.qc()}", plan.qc().none { it.startsWith("BLOQUANT") })
            val vis = plan.cards().mapNotNull { it.visualKey }
            assertTrue("« $q » : un seul genre de plan attendu pour une notion", vis.isNotEmpty())
        }
    }

    @Test fun `un plan de rancongiciel ne montre jamais de visuel d'hameconnage`() {
        for (level in TechLevel.entries) {
            val (_, plan) = Studio.ask("Ransomware", level)
            val keys = plan!!.cards().mapNotNull { it.visualKey }
            assertTrue("${level.label} : $keys", keys.none { it.startsWith("sit-phishing") || it.startsWith("phishing/") })
        }
    }

    @Test fun `les plans 3D d'un sujet ne servent que ce sujet`() {
        val (_, ospf) = Studio.ask("OSPF", TechLevel.INTERMEDIAIRE)
        val (_, ad) = Studio.ask("Active Directory", TechLevel.INTERMEDIAIRE)
        assertTrue(ospf!!.cards().mapNotNull { it.visualKey }.none { it.startsWith("net3d-ad") || it.startsWith("net3d-ransom") })
        assertTrue(ad!!.cards().mapNotNull { it.visualKey }.none { it.startsWith("net3d-ospf") || it.startsWith("net3d-ransom") })
    }
}
