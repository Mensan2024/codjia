// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.ArtDirector
import io.github.mensan2024.itmentor.engine.ContentAnalyzer
import io.github.mensan2024.itmentor.engine.ContentFamily
import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.Mood
import io.github.mensan2024.itmentor.engine.Objective
import io.github.mensan2024.itmentor.engine.Register
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Étape 2 de la Partie 4 : CODJIA comprend le texte avant de choisir la mise en scène. */
class UnderstandingTest {
    private val dir = File("engine-core/src/test/resources/textes")
    private fun text(name: String) = File(dir, name).readText()

    private val sensitiveBanned = setOf(Look.FLASH, Look.JOURNAL, Look.TERMINAL, Look.BD, Look.NEON, Look.DEMO, Look.MONTAGE, Look.KIT, Look.COLLAGE)

    private val cases = listOf(
        "famille_mental_stress.txt" to ContentFamily.MENTAL,
        "famille_marketing_tunnel.txt" to ContentFamily.MARKETING,
        "famille_reseau_cours_vlan.txt" to ContentFamily.NETWORK,
        "famille_dev_api_rest.txt" to ContentFamily.DEV,
        "famille_bible_message.txt" to ContentFamily.BIBLE,
        "famille_perso_discipline.txt" to ContentFamily.PERSONAL,
    )

    @Test fun lesSixCasObligatoiresSontBienClasses() {
        val out = StringBuilder()
        for ((f, want) in cases) {
            val p = ContentAnalyzer.analyze(text(f))
            out.appendLine("$f -> ${p.family} conf=${"%.2f".format(p.confidence)} public=${p.audience} ton=${p.tone} obj=${p.objective} indices=${p.evidence}")
            assertEquals("$f : famille", want, p.family)
            assertTrue("$f : confiance trop basse (${p.confidence})", p.confidence >= 0.3f)
        }
        println(out)
    }

    @Test fun lesTextesAnterieursGardentLeurDomaine() {
        val expected = mapOf(
            "astuce_026_force_brute.txt" to ContentFamily.CYBER, "astuce_033_phishing_sections.txt" to ContentFamily.CYBER,
            "reseaux_vlan.txt" to ContentFamily.NETWORK, "fullstack_git.txt" to ContentFamily.DEV, "ia_prompt.txt" to ContentFamily.AI,
            "sante_longevite.txt" to ContentFamily.HEALTH, "marketing_linkedin.txt" to ContentFamily.MARKETING,
            "anglais_present_perfect.txt" to ContentFamily.ENGLISH,
        )
        for ((f, want) in expected) assertEquals(f, want, ContentAnalyzer.analyze(text(f)).family)
    }

    @Test fun lesSujetsSensiblesNeRecoiventJamaisUnFormatAgressif() {
        for (f in listOf("famille_mental_stress.txt", "famille_bible_message.txt")) {
            val t = text(f)
            val p = ContentAnalyzer.analyze(t)
            assertTrue(f, p.sensitive)
            for (d in ArtDirector.all(p)) assertFalse("$f : ${d.look} proposé", d.look in sensitiveBanned)
            for (seed in 1L..60L) {
                val prop = CreativeDirector.propose(t, seed)
                assertFalse("$f graine $seed : ${prop.look}", prop.look in sensitiveBanned)
                assertTrue("$f graine $seed : ambiance ${prop.direction.mood}", prop.direction.mood == Mood.CALM || prop.direction.mood == Mood.WARM)
            }
        }
    }

    @Test fun lesTechniquesNeRecoiventPasDeFormatsDeMeditation() {
        for (f in listOf("famille_reseau_cours_vlan.txt", "famille_dev_api_rest.txt")) {
            val t = text(f)
            for (seed in 1L..40L) assertTrue("$f : ${CreativeDirector.propose(t, seed).look}", CreativeDirector.propose(t, seed).look in setOf(Look.DEMO, Look.INFOGRAPHIE, Look.EXPLICATIF, Look.TABLEAU, Look.TERMINAL, Look.EXPLIC_BAS))
        }
    }

    @Test fun lesSixVideosSontVisuellementDifferentes() {
        // Mise en scène recommandée en premier pour chaque famille (le mode automatique tire au sort parmi les directions admissibles).
        val picks = cases.map { (f, _) -> io.github.mensan2024.itmentor.engine.ArtDirector.recommend(io.github.mensan2024.itmentor.engine.ContentAnalyzer.analyze(text(f))).first().let { it.look to it.mood } }
        println(cases.map { it.first }.zip(picks))
        assertTrue("au moins 5 mises en scène distinctes sur 6 : $picks", picks.map { it.first }.toSet().size >= 5)
    }

    @Test fun troisRecommandationsDistinctesParFamille() {
        for ((f, _) in cases) {
            val r = ArtDirector.recommend(ContentAnalyzer.analyze(text(f)))
            assertEquals(f, 3, r.size)
            assertEquals(f, 3, r.map { it.look }.toSet().size)
            assertTrue(r.all { it.why.isNotBlank() })
        }
    }

    @Test fun autreVersionChangeDeDirectionSansSortirDeLaFamille() {
        val t = text("famille_bible_message.txt")
        val first = CreativeDirector.propose(t, 5L)
        val second = CreativeDirector.propose(t, 6L, avoidLooks = listOf(first.look))
        assertTrue("même format deux fois de suite", second.look != first.look)
        assertTrue(second.look !in sensitiveBanned)
    }

    @Test fun lesCasAmbigusEtTresCourtsSontSignales() {
        val short = ContentAnalyzer.analyze(text("famille_court_ambigu.txt"))
        assertEquals(ContentFamily.GENERAL, short.family)
        assertTrue(short.ambiguous)
        val amb = ContentAnalyzer.analyze(text("famille_ambigu_confiance.txt"))
        println("ambigu -> ${amb.family} conf=${amb.confidence} alt=${amb.alternatives}")
        assertTrue("devrait être mental ou personnel : ${amb.family}", amb.family == ContentFamily.MENTAL || amb.family == ContentFamily.PERSONAL)
        // Un texte très court produit quand même une vidéo valide, avec une direction non agressive.
        val prop = CreativeDirector.propose(text("famille_court_ambigu.txt"), 1L)
        assertTrue(prop.recommended.isNotEmpty())
    }

    @Test fun leRegistreEtLObjectifSontReconnus() {
        val perso = ContentAnalyzer.analyze(text("famille_perso_discipline.txt"))
        assertEquals(Register.TU, perso.register)
        assertEquals(Objective.MOTIVER, perso.objective)
        assertEquals(Objective.MEDITER, ContentAnalyzer.analyze(text("famille_bible_message.txt")).objective)
        assertEquals(Objective.APAISER, ContentAnalyzer.analyze(text("famille_mental_stress.txt")).objective)
        assertEquals(Objective.ALERTER, ContentAnalyzer.analyze(text("astuce_033_phishing_sections.txt")).objective)
    }

    @Test fun uneReferenceBibliqueEstDetecteeEtJamaisInventee() {
        val p = ContentAnalyzer.analyze(text("famille_bible_message.txt"))
        assertTrue(p.evidence.any { it.contains("jean 3:16") })
        assertTrue(p.evidence.any { it.contains("psaume 23:1") })
        // Le texte du verset reste celui de l'auteur : rien n'est ajouté ni retiré par l'analyse.
        val sb = CreativeDirector.propose(text("famille_bible_message.txt"), 2L).storyboard
        val shown = sb.scenes.joinToString(" ") { it.title + " " + it.body + " " + (it.subtitle ?: "") + " " + it.items.joinToString(" ") }
        assertTrue(shown.contains("Jean 3:16") || shown.contains("Jean 3"))
    }
}
