// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Textes réels (astuces 022 à 027) et un texte par domaine, dans src/test/resources/textes. */
object Fixtures {
    private val dir = listOf(File("engine-core/src/test/resources/textes"), File("src/test/resources/textes")).first { it.isDirectory }
    val ALL: Map<String, String> = dir.listFiles()!!.filter { it.extension == "txt" }.sortedBy { it.name }
        .associate { it.nameWithoutExtension to it.readText() }
}

class PlannerTest {
    @Test fun chaqueTexteDonneUnStoryboardValide() {
        for ((name, text) in Fixtures.ALL) for (seed in 0L..5L) {
            val p = CreativeDirector.propose(text, seed)
            val sb = Composer.prepare(p.storyboard, p.template, null)
            val tl = Timeline.build(sb) // lève une exception au-delà de 45 s
            assertTrue("$name/$seed : ${tl.totalMs} ms", tl.totalMs in 15_000L..59_000L)
            // Une phrase unique (moins de 8 mots) donne un titre et une fin : on n'invente pas de scènes pour la remplir.
            val tiny = text.split(Regex("\\s+")).size < 8
            assertTrue("$name/$seed : trop peu de scènes", sb.scenes.size >= if (tiny) 2 else 3)
            assertEquals(Layout.HERO, sb.scenes.first().layout)
            // Pas de scène finale inventée pour les sujets sensibles ou personnels quand l'auteur n'a écrit aucune conclusion.
            if (ContentAnalyzer.analyze(text).family.sensitive)
                assertTrue("$name/$seed : dernière scène ${sb.scenes.last().layout}", sb.scenes.last().layout in listOf(Layout.CHECKLIST, Layout.DEFINITION, Layout.STEPS))
            else assertEquals(Layout.CHECKLIST, sb.scenes.last().layout)
            for (sc in sb.scenes) {
                val texts = listOf(sc.title, sc.body, sc.subtitle ?: "", sc.kicker ?: "", sc.statValue ?: "") + sc.items
                for (t in texts) {
                    assertTrue("$name : astérisques déséquilibrés dans « $t »", t.count { it == '*' } % 2 == 0)
                    val bad = t.firstOrNull { !TextSanitizer.isAllowed(it) }
                    assertTrue("$name : caractère non affichable « $bad » dans « $t »", bad == null)
                }
                assertTrue("$name : liste vide", sc.layout != Layout.STEPS || sc.items.size >= 2)
            }
        }
    }

    /** Tout le message est montré : chaque mot (4 lettres ou plus) des phrases et des listes se retrouve dans la vidéo. */
    @Test fun toutLeTexteEstMontreSansResume() {
        fun words(t: String) = Regex("[\\p{L}]{4,}").findAll(t.lowercase().replace('’', '\'')).map { it.value }.toSet()
        for ((name, text) in Fixtures.ALL) for (seed in 0L..3L) {
            val p = CreativeDirector.propose(text, seed)
            val sb = Composer.prepare(p.storyboard, p.template, null)
            val shown = words(sb.scenes.joinToString(" ") { sc ->
                listOf(sc.title, sc.body, sc.subtitle ?: "", sc.kicker ?: "").joinToString(" ") + " " + sc.items.joinToString(" ") +
                    (sc.compare?.let { c -> " " + (c.badValue + c.goodValue).joinToString(" ") { sp -> sp.text } } ?: "")
            }.replace("*", ""))
            val source = text.lines().drop(1).map { it.trim() }
                .filter { it.isNotEmpty() && !(it.endsWith(":") && it.length <= 70) && !it.startsWith("#") }
                .map { l -> l.replace(Regex("#\\S+"), "").replace("*", "").let { x -> if (Regex("^[^:]{2,45}\\s:\\s").containsMatchIn(x)) x.substringAfter(" : ") else x } }
            val missing = source.flatMap { words(it) }.filter { it !in shown }.distinct()
            assertTrue("$name/$seed : mots absents de la vidéo : $missing", missing.isEmpty())
        }
    }

    @Test fun domainesDesTextesDeReference() {
        val expected = mapOf(
            "astuce_022_rdp" to Domain.CYBER, "astuce_024_audit" to Domain.CYBER, "astuce_025_certificats" to Domain.CYBER,
            "astuce_026_force_brute" to Domain.CYBER, "astuce_027_archive" to Domain.CYBER, "reseaux_vlan" to Domain.RESEAUX,
            "systemes_linux" to Domain.SYSTEMES, "ia_prompt" to Domain.IA, "fullstack_git" to Domain.FULLSTACK,
            "sante_longevite" to Domain.SANTE, "marketing_linkedin" to Domain.MARKETING, "anglais_present_perfect" to Domain.ANGLAIS,
        )
        for ((name, d) in expected) assertEquals(name, d, StoryboardPlanner.analyze(Fixtures.ALL.getValue(name)).domain)
    }

    @Test fun lectureDesAstuces() {
        val a = StoryboardPlanner.analyze(Fixtures.ALL.getValue("astuce_022_rdp"))
        assertEquals("22", a.number)
        assertTrue(a.title.startsWith("Sécurise le Bureau à distance"))
        assertEquals(5, a.lists.maxOf { it.second.size })
        assertTrue(a.hashtags.contains("#securiteinformatiquepourtous"))
        val b = StoryboardPlanner.analyze(Fixtures.ALL.getValue("astuce_027_archive"))
        assertTrue("Conclusion lue", b.conclusion!!.contains("mot de passe robuste"))
        val c = StoryboardPlanner.analyze(Fixtures.ALL.getValue("ia_prompt"))
        assertEquals(1, c.bad.size); assertEquals(1, c.good.size)
    }

    @Test fun nettoyageEtLimites() {
        val clean = TextSanitizer.clean("🔐 **Astuce** — test\u0007 ✅ ok 👍\n\n\n\nfin")
        assertFalse(clean.contains("🔐") || clean.contains("**") || clean.contains('\u0007'))
        assertTrue(clean.startsWith("Astuce"))
        val huge = "mot ".repeat(50_000)
        val t0 = System.nanoTime()
        val sb = StoryboardPlanner.plan("Titre\n$huge", 1)
        assertTrue("Texte géant traité en moins d'1 s", (System.nanoTime() - t0) / 1e9 < 1.0)
        assertTrue(Timeline.build(sb).totalMs <= 40_000)
        try { StoryboardPlanner.plan("   ", 1); org.junit.Assert.fail() } catch (_: IllegalArgumentException) {}
        try { StoryboardPlanner.plan("🔐🔐🔐", 1); org.junit.Assert.fail() } catch (_: IllegalArgumentException) {}
    }

    @Test fun typographieFrancaise() {
        assertEquals("Vrai ou faux ?", StoryboardPlanner.frenchTypo("Vrai ou faux ?"))
        assertEquals("À retenir : oui", StoryboardPlanner.frenchTypo("À retenir : oui"))
        assertEquals("« ok »", StoryboardPlanner.frenchTypo("« ok »"))
    }

    @Test fun pasDeMajusculeSurLesCommandes() {
        val sb = StoryboardPlanner.plan(Fixtures.ALL.getValue("fullstack_git"), 0)
        val steps = sb.scenes.first { it.layout == Layout.STEPS }
        assertTrue(steps.items.first().startsWith("git pull"))
    }
}

class VariationTest {
    /** Empreinte visuelle d'une génération : template, variantes, transitions, palette, mouvement. */
    private fun signature(text: String, seed: Long): String {
        val p = CreativeDirector.propose(text, seed)
        val sb = Composer.prepare(p.storyboard, p.template, null)
        val t = Templates.vary(p.template, seed)
        return listOf(p.template.id, t.motion, t.style.accent, sb.scenes.joinToString { "${it.layout}${it.variant}:${it.transitionOut.type}" }).joinToString("|")
    }

    @Test fun chaqueGenerationEstDifferente() {
        for ((name, text) in Fixtures.ALL) {
            val sigs = (1L..8L).map { signature(text, it * 7919) }
            assertTrue("$name : seulement ${sigs.toSet().size} formats différents sur 8", sigs.toSet().size >= 6)
            for (i in 1 until sigs.size) assertTrue("$name : deux générations successives identiques", sigs[i] != sigs[i - 1])
        }
    }

    @Test fun memeGraineMemeVideo() {
        val text = Fixtures.ALL.getValue("astuce_026_force_brute")
        assertEquals(CreativeDirector.propose(text, 42), CreativeDirector.propose(text, 42))
        assertEquals(signature(text, 42), signature(text, 42))
    }

    @Test fun eviteLesTemplatesRecents() {
        val text = Fixtures.ALL.getValue("astuce_022_rdp")
        val first = CreativeDirector.propose(text, 1).template.id
        repeat(10) { k -> assertTrue(CreativeDirector.propose(text, k + 2L, avoidTemplates = listOf(first)).template.id != first) }
    }

    @Test fun leChoixDeLUtilisateurEstRespecte() {
        val text = Fixtures.ALL.getValue("sante_longevite")
        assertEquals(Templates.BOLD_TYPOGRAPHY, CreativeDirector.propose(text, 9, forcedTemplate = Templates.BOLD_TYPOGRAPHY).template)
    }

    @Test fun leFormatChangeAChaqueGeneration() {
        // Comme dans l'appli : on évite les 2 derniers formats ; 6 générations de suite = au moins 4 formats différents.
        for ((name, text) in Fixtures.ALL) {
            val recent = ArrayDeque<Look>()
            val seen = LinkedHashSet<Look>()
            var prev: Look? = null
            for (seed in 1L..6L) {
                val look = CreativeDirector.propose(text, seed * 7919, avoidLooks = recent).look
                assertTrue("$name : même format deux fois de suite ($look)", look != prev)
                seen += look; prev = look
                recent.addLast(look); while (recent.size > 2) recent.removeFirst()
            }
            // Les sujets sensibles n'ont que quelques formats admissibles (aucun format bruyant) : on exige toute la palette disponible.
            val admissible = ArtDirector.all(ContentAnalyzer.analyze(text)).size
            assertTrue("$name : formats trop peu variés $seen", seen.size >= minOf(4, admissible))
        }
    }

    @Test fun laDemoEcranEstReserveeALInformatique() {
        val text = Fixtures.ALL.getValue("sante_longevite")
        for (seed in 1L..40L) assertTrue(CreativeDirector.propose(text, seed).look != Look.DEMO)
        assertEquals(Look.DEMO, CreativeDirector.propose(text, 3, forcedLook = Look.DEMO).look)
    }

    @Test fun laDureeSuitLaLongueurDuTexte() {
        fun ms(text: String) = Timeline.build(Composer.prepare(CreativeDirector.propose(text, 4).storyboard, CreativeDirector.propose(text, 4).template, null)).totalMs
        val court = ms("Astuce 030 — Verrouille ton écran.\nQuand tu quittes ton poste, appuie sur Windows + L.")
        val long = Fixtures.ALL.getValue("long_reseau_securise").let(::ms)
        assertTrue("texte court : vidéo courte ($court ms)", court < 20_000)
        assertTrue("texte long : vidéo longue ($long ms)", long > 45_000)
        assertTrue(long <= Timeline.MAX_DURATION_MS)
        assertEquals(12_000, StoryboardPlanner.targetMs(5))
        assertTrue(StoryboardPlanner.targetMs(100) in 36_000..40_000)
    }
}
