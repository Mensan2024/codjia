// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Gfx factice : chaque caractère mesure 0,5 × la taille. Suffit pour tester la mise en page. */
private class FakeGfx : Gfx {
    override fun save() {}
    override fun restore() {}
    override fun translate(dx: Float, dy: Float) {}
    override fun scale(s: Float, px: Float, py: Float) {}
    override fun rotate(degrees: Float, px: Float, py: Float) {}
    override fun clipRect(l: Float, t: Float, r: Float, b: Float) {}
    override fun multiplyAlpha(a: Float) {}
    override fun beginLayer(alpha: Float) {}
    override fun endLayer() {}
    override fun fillRect(l: Float, t: Float, r: Float, b: Float, color: Int) {}
    override fun fillVerticalGradient(l: Float, t: Float, r: Float, b: Float, top: Int, bottom: Int) {}
    override fun fillRadialGlow(cx: Float, cy: Float, radius: Float, color: Int) {}
    override fun fillRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, color: Int) {}
    override fun strokeRoundRect(l: Float, t: Float, r: Float, b: Float, radius: Float, width: Float, color: Int) {}
    override fun fillCircle(cx: Float, cy: Float, r: Float, color: Int) {}
    override fun strokeCircle(cx: Float, cy: Float, r: Float, width: Float, color: Int) {}
    override fun strokeArc(cx: Float, cy: Float, r: Float, startDeg: Float, sweepDeg: Float, width: Float, color: Int) {}
    override fun line(x0: Float, y0: Float, x1: Float, y1: Float, width: Float, color: Int) {}
    override fun fillPath(path: PathData, color: Int) {}
    override fun strokePath(path: PathData, width: Float, color: Int) {}
    override fun measureText(text: String, font: FontSpec) = text.length * font.size * 0.5f
    override fun metrics(font: FontSpec) = Metrics(font.size * 0.7f, font.size * 0.2f)
    override fun drawText(text: String, x: Float, baseline: Float, font: FontSpec, color: Int) {}
}

class TimelineTest {
    private fun scene(id: String, ms: Int, tr: Transition = Transition(TransitionType.FADE, 500)) =
        Scene(id = id, layout = Layout.HERO, title = id, durationMs = ms, transitionOut = tr)

    @Test fun demoDureEnviron32Secondes() {
        val tl = Timeline.build(DemoStoryboard.phishing())
        assertEquals(31_800L, tl.totalMs)
        assertEquals(954, tl.frameCount(30))
        assertTrue(DemoStoryboard.phishing().scenes.size >= 5)
    }

    @Test fun lesTransitionsSeChevauchent() {
        val sb = Storyboard("t", "t", "cyber", listOf(scene("a", 4000), scene("b", 4000, Transition.CUT), scene("c", 3000)))
        val tl = Timeline.build(sb)
        assertEquals(listOf(0L, 3500L, 7500L), tl.slots.map { it.startMs })
        assertEquals(10_500L, tl.totalMs)
        val m = tl.at(3750f)
        assertTrue(m is Moment.Blend)
        assertEquals(0.5f, (m as Moment.Blend).progress, 1e-4f)
        assertTrue(tl.at(7500f) is Moment.Single) // coupe franche : jamais de mélange
    }

    @Test fun refuseUneVideoDePlusDe40Secondes() {
        val sb = Storyboard("t", "t", "cyber", List(9) { scene("s$it", 5000) })
        try {
            Timeline.build(sb); fail("Une vidéo de plus de 40 s doit être refusée")
        } catch (e: StoryboardException) {
            assertTrue(e.message!!.contains("40"))
        }
    }

    @Test fun refuseUneTransitionTropLongue() {
        val sb = Storyboard("t", "t", "cyber", listOf(scene("a", 1000, Transition(TransitionType.SLIDE, 800)), scene("b", 3000)))
        try { Timeline.build(sb); fail() } catch (_: StoryboardException) {}
    }

    @Test fun refuseLesIdentifiantsEnDouble() {
        try { Storyboard("t", "t", "cyber", listOf(scene("a", 1000), scene("a", 1000))); fail() } catch (_: IllegalArgumentException) {}
    }

    @Test fun pasDeDeriveDeTemps() {
        val tl = Timeline.build(DemoStoryboard.phishing())
        assertEquals(31_766.666f, tl.frameTimeMs(953, 30), 0.01f)
    }
}

class EasingAndAnimationTest {
    @Test fun lesCourbesPartentDe0EtArriventA1() {
        for (e in listOf(Easing.LINEAR, Easing.EXPO_OUT, Easing.EASE_IN, Easing.EASE_IN_OUT, Easing.STANDARD, Easing.BACK_OUT)) {
            assertEquals(0f, e.ease(0f), 1e-4f)
            assertEquals(1f, e.ease(1f), 1e-4f)
        }
    }

    @Test fun expoOutEstMonotone() {
        var prev = 0f
        for (i in 1..100) { val v = Easing.EXPO_OUT.ease(i / 100f); assertTrue(v >= prev - 1e-5f); prev = v }
    }

    @Test fun backOutDepasseLegerement() {
        assertTrue((0..100).map { Easing.BACK_OUT.ease(it / 100f) }.max() > 1.0f)
    }

    @Test fun huitAnimationsCalculeesSurLeTempsDeRendu() {
        fun at(type: AnimType, t: Float) = Animator.enter(AnimSpec(type, 1000, 0), t)
        assertEquals(0f, at(AnimType.FADE_IN, 0f).alpha, 1e-4f); assertEquals(1f, at(AnimType.FADE_IN, 1000f).alpha, 1e-4f)
        assertEquals(1f, Animator.exit(AnimSpec(AnimType.FADE_OUT, 400), 0f, 5000f).alpha, 1e-4f)
        assertEquals(0f, Animator.exit(AnimSpec(AnimType.FADE_OUT, 400), 5000f, 5000f).alpha, 1e-4f)
        assertTrue(at(AnimType.SLIDE_LEFT, 0f).dx > 0f); assertEquals(0f, at(AnimType.SLIDE_LEFT, 1000f).dx, 1e-3f)
        assertTrue(at(AnimType.SLIDE_RIGHT, 0f).dx < 0f)
        assertTrue(at(AnimType.SLIDE_UP, 0f).dy > 0f); assertTrue(at(AnimType.SLIDE_DOWN, 0f).dy < 0f)
        assertTrue(at(AnimType.ZOOM_IN, 0f).scale < 1f); assertTrue(at(AnimType.ZOOM_OUT, 0f).scale > 1f)
        assertEquals(1f, at(AnimType.ZOOM_IN, 1000f).scale, 1e-4f)
        assertEquals(0f, Animator.enter(AnimSpec(AnimType.TYPEWRITER, 1000, 0, Easing.LINEAR), 0f).reveal, 1e-4f)
        assertEquals(0.5f, Animator.enter(AnimSpec(AnimType.TYPEWRITER, 1000, 0, Easing.LINEAR), 500f).reveal, 1e-4f)
        assertEquals(0.25f, Animator.enter(AnimSpec(AnimType.MOVE_PATH, 1000, 0, Easing.LINEAR), 250f).reveal, 1e-4f)
    }

    @Test fun leDelaiRetardeLAnimation() {
        val p = Animator.enter(AnimSpec(AnimType.FADE_IN, 500, delayMs = 1000), 900f)
        assertEquals(0f, p.alpha, 1e-4f)
    }
}

class TextLayoutTest {
    private val g = FakeGfx()
    private val f = FontSpec(FontFamily.TEXT, 500, 20f) // 10 px par caractère

    @Test fun aucuneLigneNeDepasseLaLargeur() {
        val text = "Un faux message qui imite ta banque, ton opérateur ou un collègue pour te faire cliquer."
        val lines = TextLayout.wrap(g, text, f, 200f)
        assertTrue(lines.size > 1)
        lines.forEach { assertTrue("« $it » trop longue", g.measureText(it, f) <= 200f) }
        assertEquals(text, lines.joinToString(" "))
    }

    @Test fun lesEspacesInsecablesNeSontJamaisCoupees() {
        val text = "Vrai ou faux ? Réponds vite !"
        val lines = TextLayout.wrap(g, text, f, 130f)
        lines.forEach { assertFalse(it.startsWith("?") || it.startsWith("!")) }
    }

    @Test fun unMotTropLongEstCoupe() {
        val lines = TextLayout.wrap(g, "#securiteinformatiquepourtous", f, 100f)
        assertTrue(lines.size >= 3)
        lines.forEach { assertTrue(g.measureText(it, f) <= 100f) }
        assertEquals("#securiteinformatiquepourtous", lines.joinToString(""))
    }

    @Test fun lesAccentsFrancaisSontPreserves() {
        val text = "é è ê à ù ç œ Œ « À retenir » — l’œil ouvert, reçu, déjà, où"
        assertEquals(text, TextLayout.wrap(g, text, f, 150f).joinToString(" "))
    }

    @Test fun fitReduitLaTaillePuisTronque() {
        val text = "mot ".repeat(30).trim()
        val b = TextLayout.fit(g, text, f, 200f, maxLines = 3)
        assertTrue(b.lines.size <= 3)
        val tiny = TextLayout.fit(g, "mot ".repeat(300).trim(), f, 200f, maxLines = 2)
        assertTrue(tiny.truncated)
        assertTrue(tiny.lines.last().endsWith("…"))
    }

    @Test fun pasDeMotSeulEnDerniereLigne() {
        val lines = TextLayout.avoidWidow(g, listOf("aaaa bbbb cccc", "dd"), f, 200f)
        assertEquals(listOf("aaaa bbbb", "cccc dd"), lines)
    }

    @Test fun laMachineAEcrireNeDeplacePasLeTexte() {
        val b = TextLayout.fit(g, "abc def ghi jkl", f, 80f, 4)
        val half = TextLayout.revealed(b, 0.5f)
        for ((i, l) in half.withIndex()) assertTrue(b.lines[i].startsWith(l))
    }
}

class PathTest {
    @Test fun leTraceProgressifSuitLaLongueur() {
        val p = PathData().moveTo(0f, 0f).lineTo(100f, 0f).lineTo(100f, 100f)
        val half = p.trimmed(0.5f).flatten().flatten()
        assertEquals(100f, half.last().first, 1e-3f)
        assertEquals(0f, half.last().second, 1e-3f)
        val seg = p.segment(0.25f, 0.75f).flatten().flatten()
        assertEquals(50f, seg.first().first, 1e-3f)
        assertEquals(50f, seg.last().second, 1e-3f)
    }
}

class FormatTest {
    @Test fun formatsAdaptables() {
        assertEquals(1f, VideoFormat.FULL_HD.scale, 0f)
        assertEquals(720f / 1080f, VideoFormat.HD.scale, 1e-6f)
        try { VideoFormat(1081, 1920); fail() } catch (_: IllegalArgumentException) {}
    }
}
