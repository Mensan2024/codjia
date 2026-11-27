// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.QualityControl
import io.github.mensan2024.itmentor.engine.Scenario
import io.github.mensan2024.itmentor.engine.Storyboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Correction du 27/11/2026 : « le texte commande l'image ». Les cinq textes d'essai A–E : chaque image doit être justifiée par son
 * passage ; aucun objet (ordinateur, bouclier, cadenas…) n'apparaît sans que le texte le cite.
 */
class CorrectionTest {
    private val dir = listOf(File("engine-core/src/test/resources/textes"), File("../engine-core/src/test/resources/textes")).first { it.isDirectory }
    private fun text(name: String) = File(dir, name).readText()
    private fun sb(name: String, seed: Long = 7L): Storyboard = CreativeDirector.propose(text(name), seed, forcedLook = Look.SCENARIO).storyboard
    private fun norm(s: String) = s.replace(Regex("\\s+"), " ").trim()

    private val essais = listOf(
        "test_a_methode.txt" to "method",
        "test_b_gratitude.txt" to "gratitude",
        "test_c_phishing.txt" to "phishing",
        "test_d_workflow.txt" to "method",
        "test_e_eau.txt" to "water",
    )

    @Test fun chaqueEssaiRecoitLeGenreDeSceneDeSonPropreSujet() {
        for ((f, kind) in essais) for (seed in listOf(1L, 7L, 19L)) {
            val picks = Scenario.picks(sb(f, seed)).filterNotNull()
            println("$f/$seed -> $picks")
            assertTrue("$f (graine $seed) : aucun plan de « $kind » : $picks", picks.any { it.startsWith("$kind/") })
            assertTrue("$f (graine $seed) : un plan étranger au sujet : $picks", picks.all { it.startsWith("$kind/") })
        }
    }

    @Test fun aucunObjetSansQueLeTexteLeCite() {
        // La méthode, le workflow, l'eau et la gratitude ne parlent ni d'ordinateur, ni de bouclier, ni d'attaque.
        for ((f, _) in essais.filter { it.first != "test_c_phishing.txt" }) for (seed in listOf(1L, 7L, 19L)) {
            val picks = Scenario.picks(sb(f, seed)).filterNotNull()
            assertTrue("$f : objet 3D ou attaque sans justification : $picks", picks.none { it.startsWith("object3d/") || it.startsWith("attack3d/") })
        }
    }

    @Test fun lePromptIaNeDeclenchePasUneDemonstrationDOrdinateur() {
        // Ancien défaut (2.9.0) : « La méthode en 4 points » recevait un ordinateur portable éclaté.
        val picks = Scenario.picks(sb("ia_prompt.txt")).filterNotNull()
        println("ia_prompt -> $picks")
        assertTrue("ia_prompt : un objet 3D sans justification : $picks", picks.none { it.startsWith("object3d/") || it.startsWith("attack3d/") })
        assertTrue("ia_prompt : la méthode doit être représentée comme une méthode : $picks", picks.any { it.startsWith("method/") })
    }

    @Test fun leControleQualiteLaisseUneRepresentationJustifieePourChaqueEssai() {
        for ((f, _) in essais) for (seed in listOf(1L, 7L, 19L)) {
            val s = sb(f, seed)
            val bloquants = QualityControl.audit(s).filter { it.blocking }
            assertTrue("$f (graine $seed) :\n${QualityControl.report(s)}", bloquants.isEmpty())
        }
    }

    @Test fun leControleQualitePasseSurLesHuitSujetsDe61() {
        for (f in listOf("astuce_033_phishing_sections.txt", "astuce_026_force_brute.txt", "scenario_pare_feu.txt", "scenario_topologie.txt",
            "scenario_client_serveur.txt", "scenario_gratitude.txt", "famille_mental_stress.txt", "famille_marketing_tunnel.txt", "ia_prompt.txt")) {
            val s = sb(f)
            assertTrue("$f :\n${QualityControl.report(s)}", QualityControl.passes(s))
        }
    }

    @Test fun leTexteDeLAuteurEstConserveMotPourMot() {
        for ((f, _) in essais) {
            val voulu = norm(text(f)).lowercase()
            val s = sb(f)
            val rendu = norm(s.scenes.joinToString(" ") { sc -> sc.title + " " + (sc.subtitle ?: "") + " " + sc.body }).lowercase()
            val mots = voulu.split(' ').map { it.trim(',', '.', '«', '»', '!', '?') }.filter { it.length > 2 }
            for (m in mots) assertTrue("$f : le mot « $m » a disparu : $rendu", rendu.contains(m))
            assertFalse("$f : une ellipse a été ajoutée", rendu.contains("…"))
        }
    }

    @Test fun lesEtapesDeLaMethodeViennentDuTexteDeLAuteur() {
        val report = Scenario.report(sb("test_a_methode.txt"))
        println(report)
        assertTrue(report, report.contains("method/"))
    }

    @Test fun unTexteSansSujetReconnuResteSansImageDecorative() {
        // marketing_linkedin.txt n'a aucun passage représentable : mieux vaut du texte seul qu'une décoration sans rapport.
        val picks = Scenario.picks(sb("marketing_linkedin.txt"))
        println("marketing_linkedin -> $picks")
        assertEquals("aucun objet 3D", 0, picks.filterNotNull().count { it.startsWith("object3d/") || it.startsWith("attack3d/") })
    }
}
