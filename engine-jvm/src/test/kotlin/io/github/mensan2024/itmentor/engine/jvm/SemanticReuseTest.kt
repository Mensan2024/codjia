// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.AuditRow
import io.github.mensan2024.itmentor.engine.Studio
import io.github.mensan2024.itmentor.engine.TechLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase D.2 (28/11/2026) : critère de validation sémantique. Un plan n'est pas « réussi » parce qu'il a une image par scène :
 * chaque image doit montrer le mécanisme que le texte cite, et aucun plan propre à un sujet ne sert un autre sujet.
 */
class SemanticReuseTest {
    private val demandes = mapOf("OSPF" to "net3d-ospf", "Ransomware" to "net3d-ransom", "Active Directory" to "net3d-ad", "Pare-feu" to "net3d-fw", "Phishing" to "sit-phishing")
    private val genericRoles = setOf("ouverture", "définition", "à retenir")

    private fun audit(q: String, level: TechLevel = TechLevel.INTERMEDIAIRE): List<AuditRow> {
        val (_, plan) = Studio.ask(q, level)
        return plan!!.audit()
    }

    @Test fun `aucune image n'est choisie sans que le texte cite ce qu'elle montre`() {
        for (q in demandes.keys) for (level in TechLevel.entries) for (r in audit(q, level)) {
            if (r.visualKey == null) continue
            if (r.depicts.isEmpty()) continue // plan générique : couvert par le test des rôles
            assertTrue("« $q » ${level.label} ${r.episode}.${r.index} : ${r.visualKey} montre ${r.depicts} mais le texte cite ${r.textCues}",
                r.verdict.startsWith("ok"))
        }
    }

    @Test fun `un plan generique n'illustre que l'ouverture, la definition ou le recapitulatif`() {
        for (q in demandes.keys) for (r in audit(q)) {
            if (r.visualKey == null || r.depicts.isNotEmpty()) continue
            assertTrue("« $q » ${r.episode}.${r.index} (${r.role}) : plan générique ${r.visualKey} pour une scène de mécanisme",
                r.role in genericRoles)
        }
    }

    @Test fun `aucune scene n'est refusee par le garde semantique`() {
        for (q in demandes.keys) for (r in audit(q)) assertTrue("« $q » ${r.episode}.${r.index} : ${r.verdict}", !r.verdict.startsWith("REFUSÉ"))
    }

    @Test fun `les plans propres a un sujet ne servent jamais un autre sujet`() {
        val tous = demandes.values
        for ((q, mine) in demandes) {
            val others = tous.filter { it != mine }
            for (r in audit(q)) {
                val k = r.visualKey ?: continue
                assertTrue("« $q » ${r.episode}.${r.index} : $k appartient à un autre sujet", others.none { k.startsWith(it) })
            }
        }
    }

    @Test fun `les plans propres a un sujet ne montrent aucun visuel d'un autre domaine que le sien`() {
        for ((q, mine) in demandes) for (r in audit(q)) {
            val k = r.visualKey ?: continue
            if (k.startsWith("net3d-") || k.startsWith("sit-phishing")) assertTrue("« $q » : $k", k.startsWith(mine))
        }
    }

    @Test fun `une scene qui cite un mecanisme dedie n'a pas droit a une affiche ni a un plan d'un autre mecanisme`() {
        for ((q, _) in demandes) for (r in audit(q)) {
            val k = r.visualKey ?: continue
            val dedicated = r.textCues.filter { '-' in it.substringAfter(':') && it.startsWith("topic:") }
            if (dedicated.isEmpty() || r.role == "ouverture") continue
            assertTrue("« $q » ${r.episode}.${r.index} cite $dedicated mais montre $k", !k.endsWith("/poster"))
        }
    }

    @Test fun `aucun plan n'est reutilise a l'identique pour deux scenes consecutives`() {
        for (q in demandes.keys) for (ep in audit(q).groupBy { it.episode }.values) for ((a, b) in ep.zipWithNext())
            if (a.visualKey != null) assertTrue("« $q » ${a.episode}.${a.index}-${b.index} : ${a.visualKey} deux fois de suite", a.visualKey != b.visualKey)
    }

    @Test fun `une demande de la bibliotheque n'a aucune scene sans image`() {
        for (q in demandes.keys) {
            val rows = audit(q)
            val sans = rows.filter { it.visualKey == null }
            assertEquals("« $q » : ${sans.map { "${it.episode}.${it.index}" }}", 0, sans.size)
        }
    }
}
