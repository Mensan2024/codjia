// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine.jvm

import io.github.mensan2024.itmentor.engine.Composer
import io.github.mensan2024.itmentor.engine.CreativeDirector
import io.github.mensan2024.itmentor.engine.Episodes
import io.github.mensan2024.itmentor.engine.Fidelity
import io.github.mensan2024.itmentor.engine.FrameRenderer
import io.github.mensan2024.itmentor.engine.Layout
import io.github.mensan2024.itmentor.engine.Look
import io.github.mensan2024.itmentor.engine.Pins
import io.github.mensan2024.itmentor.engine.Scene
import io.github.mensan2024.itmentor.engine.Scenario
import io.github.mensan2024.itmentor.engine.StoryPlan
import io.github.mensan2024.itmentor.engine.StoryboardPlanner
import io.github.mensan2024.itmentor.engine.Studio
import io.github.mensan2024.itmentor.engine.Timeline
import io.github.mensan2024.itmentor.engine.VideoFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File

/**
 * Phase B (27/11/2026) : texte long → épisodes sans compression, plan de production modifiable, scène remplaçable seule.
 * Les seuils vérifiés ici sont des objectifs provisoires ; les mesures réelles sont affichées par `story plan` / `story render`.
 */
class StudioTest {
    private val fonts = FontBank(File(System.getProperty("itm.fonts", "engine-android/src/main/assets/fonts")))
    private val dir = listOf(File("engine-core/src/test/resources"), File("../engine-core/src/test/resources")).first { it.isDirectory }
    private val longs = listOf("longs/pare_feu_long.txt", "longs/phishing_long.txt")
    private fun text(n: String) = File(dir, n).readText()
    private val fmt = VideoFormat(270, 480, 30)

    private fun frame(r: FrameRenderer, t: Float): IntArray {
        val img = BufferedImage(fmt.width, fmt.height, BufferedImage.TYPE_INT_RGB)
        r.render(Java2DGfx(img, fonts), t)
        return (img.raster.dataBuffer as DataBufferInt).data.clone()
    }

    // ── 100 % du texte, dans l'ordre, sans compression ──

    @Test fun lesTextesLongsSontDecoupesEnEpisodesSansRienPerdre() {
        for (f in longs) {
            val plan = Studio.plan(text(f), 7, voice = true)
            assertTrue("$f : doit faire plusieurs épisodes", plan.episodeCount >= 2)
            val fid = plan.fidelity()
            assertTrue("$f : ${fid.summary()}", fid.perfect)
            val all = plan.series.flatMap { it.scenes }.filter { !it.id.endsWith("-open") }
            assertEquals("$f : chaque scène dans un seul épisode", plan.storyboard.scenes.map { it.id }, all.map { it.id })
            for (ep in plan.series) {
                assertTrue("$f ${ep.episode} : ${Episodes.spanMs(ep.scenes, true)} ms", Episodes.spanMs(ep.scenes, true) <= 58_000)
                assertTrue(ep.episode!!.label.startsWith("ÉPISODE "))
            }
            assertTrue("$f : ${plan.qc()}", plan.qc().none { it.startsWith("BLOQUANT") })
        }
    }

    @Test fun aucuneDureeNEstComprimee() {
        for (f in longs) {
            val plan = Studio.plan(text(f), 7, voice = true)
            val natural = StoryboardPlanner.plan(text(f), 7, compress = false).scenes.associate { it.id to it.durationMs }
            for (ep in plan.prepared(null)) {
                Timeline.build(ep) // ≤ 59 s
                for (s in ep.scenes) if (!s.id.endsWith("-open")) assertTrue("$f ${s.id} : ${s.durationMs} < ${natural[s.id]}", s.durationMs >= (natural[s.id] ?: 0) - 1)
            }
        }
    }

    @Test fun ancienneVoieInchangee() {
        // La voie historique (compression) reste la valeur par défaut : une vidéo, jamais plus de 59 s.
        val p = CreativeDirector.propose(text("longs/pare_feu_long.txt"), 7, forcedLook = Look.SCENARIO)
        assertTrue(Composer.totalMs(Composer.prepare(p.storyboard, p.template, null)) <= 59_000)
        assertEquals(null, p.storyboard.episode)
    }

    @Test fun uneDemoNEstInsereeQueSiLeTexteNommeUnSysteme() {
        val plan = Studio.plan(text("longs/pare_feu_long.txt"), 7)
        assertTrue("démo hors sujet", plan.storyboard.scenes.none { it.layout == Layout.SCREEN })
        val rdp = Studio.plan("Astuce : sécuriser le bureau à distance\nSur Windows, le bureau à distance (RDP) permet de prendre la main sur un ordinateur. Pour le protéger, active l'authentification au niveau du réseau et mets à jour Windows Update régulièrement.\nPrévois aussi un mot de passe long et unique pour ce compte.", 7)
        assertTrue(rdp.storyboard.scenes.any { it.layout == Layout.SCREEN })
        assertTrue(rdp.fidelity().perfect)
    }

    // ── fidélité : l'outil de mesure détecte les pertes et les inversions ──

    @Test fun laFideliteDetecteUnMotPerduEtUnOrdreInverse() {
        val raw = "Titre\nAlpha beta gamma. Delta epsilon zêta."
        val ok = StoryboardPlanner.plan(raw, 1, compress = false).scenes
        assertTrue(Fidelity.measure(raw, ok).perfect)
        val cut = ok.map { it.copy(body = it.body.replace("gamma", ""), subtitle = it.subtitle?.replace("gamma", ""), items = it.items.map { i -> i.replace("gamma", "") }) }
        assertFalse(Fidelity.measure(raw, cut).perfect)
        val swapped = listOf(Scene("a", Layout.DEFINITION, "x", body = "Delta epsilon zêta", durationMs = 3000), Scene("b", Layout.DEFINITION, "y", body = "Alpha beta gamma", durationMs = 3000))
        val r = Fidelity.measure("Alpha beta gamma. Delta epsilon zêta.", swapped)
        assertEquals(1.0, r.presence, 1e-9)
        assertTrue("l'inversion doit se voir : ${r.ordered}", r.ordered < 0.7)
    }

    @Test fun unHashtagAvecTraitDUnionNEstPasCoupe() {
        val sb = StoryboardPlanner.plan("Astuce 001 — Titre de test\nUne phrase.\nÀ retenir : un point. Un autre.\n#pare-feu #codjia", 1, compress = false)
        assertTrue(sb.scenes.flatMap { it.items + it.body + it.title }.none { it.contains("-feu") })
    }

    @Test fun unParagrapheEcritApresUneListeResteApresLaListe() {
        val raw = "Astuce 002 — Ordre\nIntroduction courte du sujet.\nÉtapes à suivre :\n- Premier geste à faire\n- Deuxième geste à faire\n- Troisième geste à faire\nCe paragraphe vient après la liste et il est assez long pour ne pas être pris pour une puce : il développe une précision importante que l'auteur a voulu placer en dernier.\n#x"
        val sb = StoryboardPlanner.plan(raw, 1, compress = false)
        val iList = sb.scenes.indexOfFirst { it.layout == Layout.STEPS }
        val iPara = sb.scenes.indexOfFirst { it.body.contains("après la liste") }
        assertTrue("liste=$iList paragraphe=$iPara", iList in 0 until iPara)
        assertTrue(Fidelity.measure(raw, sb.scenes).perfect)
    }

    // ── découpage en épisodes ──

    @Test fun leDecoupageEvitedeCouperUnePartieEnDeux() {
        fun sc(i: Int, ms: Int, title: String = "T$i", layout: Layout = Layout.DEFINITION) = Scene("s$i", layout, title, body = "mot", durationMs = ms)
        val scenes = listOf(sc(0, 12_000), sc(1, 12_000), sc(2, 12_000, "Partie (1/2)"), sc(3, 12_000, "Partie (2/2)"), sc(4, 12_000), sc(5, 12_000))
        val a = Episodes.assign(scenes, voice = false, budgetMs = 50_000)
        assertEquals("deux épisodes suffisent", 2, a.max())
        assertTrue("jamais entre (1/2) et (2/2) : $a", a[2] == a[3])
        assertTrue(a.zipWithNext().all { (x, y) -> y >= x })
    }

    @Test fun uneSceneTropLongueEstRefuseeSansCompression() {
        val big = listOf(Scene("a", Layout.DEFINITION, "t", body = "x", durationMs = 49_000), Scene("b", Layout.DEFINITION, "t", body = "x", durationMs = 3_000))
        try { Episodes.assign(big, false, 50_000); throw AssertionError("aurait dû refuser") } catch (e: io.github.mensan2024.itmentor.engine.StoryboardException) { /* attendu */ }
    }

    // ── plan JSON ──

    @Test fun leJsonFaitLAllerRetourExact() {
        val plan = Studio.plan(text("longs/phishing_long.txt"), 7).replaceVisual("s-def", Pins.TEXT_ONLY).withDuration("s-def", 9_000)
        val j = plan.toJson()
        val back = StoryPlan.fromJson(j)
        assertEquals(j, back.toJson())
        assertEquals(plan.series.map { it.scenes }, back.series.map { it.scenes })
        assertEquals(plan.series.map { it.pins }, back.series.map { it.pins })
    }

    @Test fun lesVisuelsFigesCorrespondentAuChoixAutomatique() {
        val plan = Studio.plan(text("longs/pare_feu_long.txt"), 7)
        val free = Scenario.picks(plan.storyboard.copy(pins = emptyMap()))
        for ((i, s) in plan.storyboard.scenes.withIndex()) assertEquals(s.id, free[i] ?: Pins.TEXT_ONLY, plan.pins[s.id])
    }

    @Test fun lesEditionsInvalidesSontRefusees() {
        val plan = Studio.plan(text("longs/pare_feu_long.txt"), 7)
        for (bad in listOf<() -> Unit>({ plan.replaceVisual("nope", Pins.TEXT_ONLY) }, { plan.replaceVisual("s-def", "x/y") }, { plan.withDuration("s-def", 100) }))
            try { bad(); throw AssertionError("aurait dû refuser") } catch (e: IllegalArgumentException) { /* attendu */ }
    }

    @Test fun lesFichesDonnentLes13ChampsSansRienInventer() {
        val cards = Studio.plan(text("longs/pare_feu_long.txt"), 7).cards()
        assertEquals(16, cards.size) // phase C : groupes de 150 caractères → davantage de scènes plus courtes (12 en phase B)
        for (c in cards) {
            assertTrue(c.hash.length == 16 && c.durationMs > 0 && c.setting.isNotBlank() && c.animation.isNotBlank() && c.audio.isNotBlank())
            if (c.visualKey == null && !c.id.endsWith("-open")) assertTrue("${c.id} : ${c.image}", c.image.contains("Aucun visuel"))
            if (c.visualKey != null) assertTrue(c.image.contains(c.visualLabel!!))
            assertEquals("pas de personnage inventé", c.characters.isEmpty(), c.characters.isEmpty())
        }
    }

    // ── remplacer UNE scène ne change que cette scène, au pixel près ──

    @Test fun remplacerUneSceneNeChangeQueCetteScene() {
        val a = Studio.plan(text("longs/pare_feu_long.txt"), 7)
        val id = "s-def0-3"
        val b = a.replaceVisual(id, "firewall/filter")
        assertNotEquals(a.pins[id], b.pins[id])
        assertEquals("seule épingle modifiée", listOf(id), a.pins.keys.filter { a.pins[it] != b.pins[it] })
        for (s in a.storyboard.scenes) if (s.id != id) assertEquals(s.id, a.sceneHash(s.id), b.sceneHash(s.id)) else assertNotEquals(a.sceneHash(id), b.sceneHash(id))
        var compared = 0
        for ((ea, eb) in a.prepared(null).zip(b.prepared(null))) {
            val ra = FrameRenderer(ea, fmt, a.template); val rb = FrameRenderer(eb, fmt, b.template)
            assertEquals("durées inchangées", ra.timeline.totalMs, rb.timeline.totalMs)
            val slots = ra.timeline.slots
            for ((k, sl) in slots.withIndex()) {
                val prevOverlap = if (k > 0) slots[k - 1].endMs - sl.startMs else 0L
                val nextOverlap = if (k < slots.lastIndex) sl.endMs - slots[k + 1].startMs else 0L
                val lo = sl.startMs + prevOverlap + 100f; val hi = sl.endMs - nextOverlap - 100f
                for (q in listOf(0.0f, 0.35f, 0.7f, 1.0f)) {
                    val t = lo + (hi - lo) * q
                    val same = frame(ra, t).contentEquals(frame(rb, t))
                    if (sl.scene.id == id) { if (q == 1.0f) assertFalse("la scène remplacée doit changer", same) } else { assertTrue("${sl.scene.id} t=$t doit rester identique", same); compared++ }
                }
            }
        }
        assertTrue("images comparées : $compared", compared >= 30)
    }

    @Test fun leBadgeEpisodeSeDessineSousLaBarreEtSeulement() {
        val plan = Studio.plan(text("longs/pare_feu_long.txt"), 7)
        val ep = plan.prepared(null)[1]
        val with = FrameRenderer(ep, fmt, plan.template)
        val without = FrameRenderer(ep.copy(episode = null), fmt, plan.template)
        val t = with.timeline.slots[1].startMs + 3000f
        val x = frame(with, t); val y = frame(without, t)
        val diff = x.indices.filter { x[it] != y[it] }
        assertTrue("le badge doit apparaître", diff.isNotEmpty())
        assertTrue("badge confiné à la bande 105–160 px (échelle ${fmt.scale})", diff.all { (it / fmt.width) in (105 * fmt.scale).toInt()..(160 * fmt.scale).toInt() + 1 })
    }
}
