// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.Scenario
import io.github.mensan2024.itmentor.engine.Storyboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Partie 6, étape 6.1 : compréhension de la scène (SceneIntent) et choix d'une représentation adaptée au contenu. */
class ScenarioTest {
    private val dir = listOf(File("engine-core/src/test/resources/textes"), File("../engine-core/src/test/resources/textes")).first { it.isDirectory }
    private fun text(name: String) = File(dir, name).readText()
    private fun storyboard(name: String, seed: Long = 7L): Storyboard = CreativeDirector.propose(text(name), seed, forcedLook = Look.SCENARIO).storyboard

    /** Les huit sujets imposés par la validation de l'étape 6.1, avec le genre de scène attendu. */
    private val subjects = listOf(
        "astuce_033_phishing_sections.txt" to "phishing",
        "astuce_026_force_brute.txt" to "bruteforce",
        "scenario_pare_feu.txt" to "firewall",
        "scenario_topologie.txt" to "topology",
        "scenario_client_serveur.txt" to "clientserver",
        "scenario_gratitude.txt" to "gratitude",
        "famille_mental_stress.txt" to "stress",
        "famille_marketing_tunnel.txt" to "funnel",
    )

    /** Phase C : une famille de situations (personnages, lieux) peut illustrer le même sujet que l'ancienne famille. */
    private fun family(kind: String) = listOf(kind) + when (kind) { "phishing" -> listOf("sit-phishing"); "stress" -> listOf("sit-mental"); "water" -> listOf("sit-aid"); "funnel" -> listOf("sit-shop"); "clientserver", "topology", "firewall" -> listOf("sit-net"); else -> emptyList() }

    @Test fun chaqueSujetRecoitSaPropreFamilleDeScenes() {
        for ((f, kind) in subjects) {
            val sb = storyboard(f)
            val picks = Scenario.picks(sb).filterNotNull()
            println("$f -> $picks")
            assertTrue("$f : au moins la moitié des scènes doit avoir une représentation (${Scenario.coverage(sb)})", Scenario.coverage(sb) >= 0.5f)
            val own = picks.count { family(kind).any { p -> it.startsWith("$p/") } }
            assertTrue("$f : la majorité des plans doit venir du genre « $kind » : $picks", own * 2 > picks.size)
        }
    }

    @Test fun lesSujetsDifferentsDonnentDesSequencesDifferentes() {
        val seqs = subjects.map { (f, _) -> Scenario.picks(storyboard(f)).filterNotNull() }
        for (i in seqs.indices) for (j in i + 1 until seqs.size)
            assertTrue("${subjects[i].first} et ${subjects[j].first} ont la même séquence de plans", seqs[i] != seqs[j])
        // Aucun plan n'est partagé par deux sujets de genres différents : les images ne se ressemblent pas.
        val kinds = seqs.map { s -> s.map { it.substringBefore('/') }.toSet() }
        assertTrue("au moins 6 genres de scènes distincts", kinds.flatten().toSet().size >= 6)
    }

    @Test fun uneSceneNEstPasRepeteeInutilementDansUneVideo() {
        for ((f, _) in subjects) for (seed in listOf(1L, 7L, 19L)) {
            val picks = Scenario.picks(storyboard(f, seed)).filterNotNull()
            assertEquals("$f (graine $seed) : plan répété : $picks", picks.size, picks.toSet().size)
        }
    }

    @Test fun lesConceptsSontComprisParScene() {
        val concepts = subjects.associate { (f, k) -> k to Scenario.concepts(storyboard(f)).map { it.name } }
        println(concepts)
        assertTrue(concepts.getValue("phishing").any { it == "PHISHING" })
        assertTrue(concepts.getValue("bruteforce").any { it == "BRUTE_FORCE" })
        assertTrue(concepts.getValue("firewall").any { it == "FIREWALL" })
        assertTrue(concepts.getValue("topology").any { it == "TOPOLOGY" })
        assertTrue(concepts.getValue("clientserver").any { it == "CLIENT_SERVER" })
        assertTrue(concepts.getValue("gratitude").any { it == "GRATITUDE" })
        assertTrue(concepts.getValue("stress").any { it == "STRESS" })
        assertTrue(concepts.getValue("funnel").any { it == "FUNNEL" })
    }

    @Test fun unTexteSensibleNeRecoitQueDesPlansDouxEtLeTexteReste() {
        val orig = text("famille_mental_stress.txt")
        val p = CreativeDirector.propose(orig, 7L, forcedLook = Look.SCENARIO)
        assertTrue(p.profile.sensitive)
        val picks = Scenario.picks(p.storyboard).filterNotNull()
        assertTrue("aucun plan pour un texte sensible", picks.isNotEmpty())
        // Aucun plan d'attaque ou d'alerte : on reste dans le registre « stress ».
        assertTrue("plan non apaisant : $picks", picks.none { it.startsWith("attack3d/") || it.startsWith("bruteforce/") || it.startsWith("phishing/") })
        // Le texte de l'auteur est conservé : chaque ligne de liste se retrouve dans le storyboard.
        val words = p.storyboard.scenes.flatMap { listOf(it.title, it.body) + it.items }.joinToString(" ").lowercase()
        for (w in listOf("respire lentement", "nomme ton émotion", "bouge un peu", "dors à heures régulières")) assertTrue("texte perdu : $w", w in words)
    }

    @Test fun leRapportExpliqueChaqueChoix() {
        val rep = Scenario.report(storyboard("astuce_033_phishing_sections.txt"))
        assertTrue(rep.lines().size >= 4)
        assertTrue(rep.contains("phishing/"))
    }
}
