// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase C — santé mentale et bien-être en SITUATION. Des personnages dans des moments crédibles (une nuit de pensées qui tournent, une journée
 * qui déborde, une marche, une conversation, un carnet) : sobres, sans scène de détresse, sans diagnostic ni mise en scène de la souffrance.
 * Toutes les scènes sont `calm` : elles sont les seules admises pour un sujet sensible.
 */
internal object SitMental {
    private fun roomNight(c: StageCtx): Float {
        val th = c.th
        val f = Props.room(c.g, mix(th.bgBottom, rgb(0x3A3F7A), 0.35f), mix(th.bgTop, th.bgBottom, 0.3f), 590f)
        Props.window(c.g, 90f, 70f, 300f, 300f, rgb(0x0E1633), rgb(0x5A6190), night = true, tt = c.t)
        Props.moon(c.g, 215f, 150f, 36f, rgb(0x0E1633))
        return f
    }

    private fun roomDay(c: StageCtx, sky: Int = rgb(0xBFE3F5)): Float {
        val th = c.th
        val f = Props.room(c.g, mix(th.bgBottom, rgb(0xE9D8B4), 0.38f), mix(th.bgTop, rgb(0x8A5A3A), 0.45f), 590f)
        Props.window(c.g, 90f, 70f, 330f, 320f, sky, mix(th.bgBottom, rgb(0xFFFFFF), 0.5f))
        return f
    }

    private val night = beat("night", "Une nuit où les pensées tournent", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.RECOGNIZE, Role.MECHANISM), setOf(Goal.APAISER),
        cueSet(Thing.SOIR, Thing.SOMMEIL) + setOf("topic:night", "topic:overload", "topic:alone"), calm = true, minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomNight(c)
        Props.bed(g, 640f, floor, 520f, rgb(0x5A6BA8))
        Props.lamp(g, 920f, floor - 100f, 120f, true)
        val face = Face.mix(Face.TIRED, Face.WORRIED, SK.ease(t, 800f, 900f)).copy(gazeY = 0.35f)
        val an = Cast.draw(g, 520f, floor, 400f, Cast.persona(3), face, Body.mix(Poses.slump(t), Poses.hand2Chin(t).copy(seated = true, crouch = 0.4f), SK.ease(t, 1500f, 600f)).copy(seated = true), t, 3)
        // Les pensées tournent au-dessus de la tête, puis s'éclaircissent doucement.
        val a = SK.ease(t, 600f, 900f)
        Props.swirl(g, an.headX + 20f, an.headY - 150f, 90f, t, th.muted, a * 0.9f)
        Props.swirl(g, an.headX + 120f, an.headY - 190f, 55f, -t * 1.3f, th.accent, a * 0.6f)
        Props.clock(g, 430f, 160f, 34f, th, t * 0.2f, th.muted)
    }

    private val overload = beat("overload", "Une journée qui déborde", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.RECOGNIZE), setOf(Goal.APAISER, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.SOIR) + setOf("topic:overload", "topic:night", "topic:alone"), calm = true, minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomDay(c, rgb(0x9BB6CF))
        val k = SK.ease(t, 600f, 2800f)
        val face = Face.mix(Face.FOCUSED, Face.STRESSED, SK.ease(t, 1600f, 1400f))
        val an = SitKit.worker(c, 500f, floor, 440f, 5, Props.gaze(face, 500f, 300f, 300f, 200f), Poses.typing(t), 400f)
        // Des cartes de tâches arrivent de tous côtés et s'empilent : agenda, courriel, téléphone, horloge.
        val kinds = listOf("agenda", "mail", "phone", "clock", "agenda", "mail", "phone", "clock")
        for ((i, kd) in kinds.withIndex()) {
            val start = 300f + i * 330f
            val u = SK.ease(t, start, 900f)
            if (u <= 0f) continue
            val side = if (i % 2 == 0) -1f else 1f
            val tx = 500f + side * (130f + (i / 2) * 22f) ; val ty = 110f + (i / 2) * 58f
            val sx = 500f + side * 520f; val sy = 40f + (i * 83f) % 300f
            val x = lerp(sx, tx, u); val y = lerp(sy, ty, u) + sin(t / 500f + i) * 4f
            g.withPose(Pose(alpha = u), x, y) {
                g.fillRoundRect(x - 56f, y - 40f, x + 56f, y + 40f, 12f, th.panelHi)
                g.strokeRoundRect(x - 56f, y - 40f, x + 56f, y + 40f, 12f, 3f, if (i > 4) th.warn else th.line)
                when (kd) {
                    "agenda" -> { g.fillRect(x - 40f, y - 28f, x + 40f, y - 14f, th.bad.withAlpha(0.8f)); for (r in 0..1) for (q in 0..3) g.fillRoundRect(x - 38f + q * 20f, y - 4f + r * 20f, x - 26f + q * 20f, y + 8f + r * 20f, 3f, th.muted) }
                    "mail" -> SK.envelope(g, x, y, 64f, th.panelHi, th.accent)
                    "phone" -> { g.fillRoundRect(x - 16f, y - 30f, x + 16f, y + 30f, 7f, th.muted); Props.notifBadge(g, x + 20f, y - 26f, 3, 1f, th.bad) }
                    else -> Props.clock(g, x, y, 26f, th, t + i * 200f)
                }
            }
        }
        Props.swirl(g, 500f, 70f, 70f, t, th.warn, k * 0.5f)
        if (an.hr < 0f) return@beat
    }

    private val breatheWindow = beat("breathe-window", "Respirer près d'une fenêtre", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.DEFINE, Role.INTRO), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.RESPIRER, Thing.RESPIRATION) + setOf("topic:breath", "topic:hope"), calm = true, minMs = 4000, needs = setOf("topic:breath")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomDay(c, rgb(0xCDE9F7))
        Props.sun(g, 210f, 150f, 30f, t)
        Props.plant(g, 930f, floor, 190f)
        // Cycle de 8 s : inspirer (4 s) puis expirer (4 s).
        val u = SK.loop(t, 8000f); val inh = if (u < 0.5f) Easing.EASE_IN_OUT.ease(u * 2f) else 1f - Easing.EASE_IN_OUT.ease((u - 0.5f) * 2f)
        val an = Cast.draw(g, 600f, floor, 420f, Cast.persona(2), Face.mix(Face.FOCUSED, Face.RELIEVED, inh), Body.mix(Poses.idle(t), Poses.open(t), inh * 0.35f), t, 2)
        val cx = 600f; val cy = an.shoulderY - 20f
        g.strokeCircle(cx, cy, 120f + inh * 90f, 5f, th.accent.withAlpha(0.35f + 0.4f * inh))
        g.strokeCircle(cx, cy, 160f + inh * 90f, 3f, th.accent.withAlpha(0.2f))
        SK.pill(g, if (u < 0.5f) "INSPIRE" else "EXPIRE", 830f, 330f, th.accent, th.bgTop.withAlpha(0.8f), 28f)
    }

    private val bench = beat("talk-bench", "En parler à quelqu'un de confiance", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.DEFINE, Role.MECHANISM), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.PARTAGER, Actor.PROCHE) + setOf("topic:talk", "topic:alone", "topic:hope"), calm = true, minMs = 4000, needs = setOf("topic:talk")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val horizon = 430f
        Props.sky(g, rgb(0x6C8FD0), rgb(0xF3C9A0), horizon)
        Props.sun(g, 800f, 200f, 36f, t, rgb(0xFFE29A))
        Props.ground(g, horizon, rgb(0x3E7C4F))
        Props.tree(g, 120f, horizon + 40f, 330f, t = t); Props.tree(g, 900f, horizon + 30f, 280f, t = t, who = 2)
        g.fillRect(0f, 590f, 1000f, 700f, rgb(0xB89C74))
        Props.bench(g, 500f, 470f, 590f, 520f)
        val say = SK.loop(t, 4200f)
        val talking = if (say < 0.7f) 1f else 0f
        val fa = Face.mix(Face.WORRIED, Face.SMILE, SK.ease(t, 2600f, 1200f))
        // Deux personnes assises : l'une confie, l'autre écoute.
        val a = Cast.draw(g, 400f, 590f, 350f, Cast.persona(0), fa.copy(mouthOpen = 0.25f * talking * (0.5f + 0.5f * sin(t / 90f))), Poses.holdFront(t).copy(seated = true), t, 0)
        val b = Cast.draw(g, 600f, 590f, 350f, Cast.persona(1), Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 1800f, 800f)), Body.mix(Poses.idle(t).copy(seated = true), Poses.hug(t).copy(seated = true, armL = 40f to -40f), SK.ease(t, 3000f, 600f) * 0.4f), t, 1, mirror = true)
        val p1 = SK.pop(t, 500f, 500f)
        Props.bubble(g, 140f, 70f, 380f, 170f, 395f, 300f, th.panelHi, th.line, p1)
        if (p1 > 0.6f) for (k in 0..2) g.fillCircle(210f + k * 50f, 120f, 8f + 3f * sin(t / 200f + k), th.muted)
        val p2 = SK.pop(t, 2400f, 500f)
        if (p2 > 0.01f) SK.heart(g, b.headX, b.headY - 140f - 20f * p2, 34f * p2, th.bad)
        if (a.hr < 0f) return@beat
    }

    private val walk = beat("walk-path", "Marcher dehors", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.DEFINE, Role.INTRO), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.MARCHER, Thing.MARCHE) + setOf("topic:walk", "topic:hope"), calm = true, minMs = 4000, needs = setOf("topic:walk")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val horizon = 400f
        Props.sky(g, rgb(0x7FB6E6), rgb(0xFBE3B4), horizon)
        Props.sun(g, 780f, 150f, 34f, t, rgb(0xFFE29A))
        Props.cloud(g, (SK.loop(t, 24000f) * 1300f) - 150f, 110f, 80f)
        Props.ground(g, horizon, rgb(0x4C9A5E))
        // Défilement : arbres et buissons passent derrière la personne qui marche.
        val sp = t / 9f
        for (i in 0 until 5) { val x = ((i * 300f - sp * 0.5f) % 1500f + 1500f) % 1500f - 250f; Props.tree(g, x, horizon + 30f, 300f, t = t, who = i) }
        g.fillRect(0f, 540f, 1000f, 700f, rgb(0xCFB486))
        g.fillRect(0f, 540f, 1000f, 548f, rgb(0xB19A6C))
        for (i in 0 until 7) { val x = ((i * 190f - sp) % 1330f + 1330f) % 1330f - 150f; g.fillRoundRect(x, 640f, x + 100f, 650f, 5f, rgb(0xB19A6C)) }
        Cast.draw(g, 480f, 640f, 400f, Cast.persona(0), Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 1200f, 1500f)), Poses.walk(t, 700f), t, 0)
        for (i in 0 until 3) { val x = ((i * 400f - sp * 1.4f + t / 6f) % 1200f + 1200f) % 1200f - 100f; val y = 120f + i * 38f + sin(t / 300f + i) * 10f; g.line(x - 12f, y, x, y - 6f, 3f, th.ink.withAlpha(0.6f)); g.line(x, y - 6f, x + 12f, y, 3f, th.ink.withAlpha(0.6f)) }
    }

    private val notebook = beat("notebook", "Écrire ce qu'on ressent", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.DEFINE, Role.RECAP, Role.MECHANISM), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE, Goal.MEMORISER),
        cueSet(Act.ECRIRE, Thing.JOURNAL, Thing.SOIR) + setOf("topic:journal", "topic:hope", "topic:night"), calm = true, minMs = 4000, needs = setOf("topic:journal")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomNight(c)
        Props.lamp(g, 840f, floor - 140f, 170f, true)
        val an = SitKit.worker(c, 480f, floor, 440f, 1, Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 2800f, 900f)).copy(gazeY = 0.5f), Poses.typing(t).copy(), 330f)
        // Le carnet est sur le bureau, de face : on y voit les lignes se remplir et trois petites images apparaître.
        g.fillRoundRect(330f, 400f, 630f, 456f, 8f, rgb(0xF6EFD9))
        for (k in 0..1) g.fillRoundRect(350f, 414f + k * 18f, 350f + clamp01((window(t, 600f + k * 900f, 1200f))) * 250f, 420f + k * 18f, 3f, rgb(0x9A8F6E))
        for (k in 0..2) {
            val p = SK.pop(t, 1600f + k * 900f, 500f)
            if (p <= 0.01f) continue
            val x = 680f + k * 90f; val y = 250f + sin(t / 600f + k) * 8f
            g.fillCircle(x, y, 38f * p, th.panelHi); g.strokeCircle(x, y, 38f * p, 3f, th.warn)
            when (k) { 0 -> Props.sun(g, x, y, 12f * p, t); 1 -> SK.heart(g, x, y, 18f * p, th.bad); else -> SK.spark(g, x, y, 18f * p, th.accent) }
        }
        if (an.hr < 0f) return@beat
    }


    private val awake = beat("awake", "Allongé, les yeux ouverts, les pensées tournent", SitKit.BODY + setOf(Role.DEFINE), setOf(Goal.APAISER, Goal.COMPRENDRE_MECANISME, Goal.APPRENDRE_REFLEXE),
        cueSet(Thing.SOMMEIL) + setOf("topic:night", "topic:lying"), calm = true, minMs = 4000, needs = setOf("topic:lying")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomNight(c)
        Props.bed(g, 520f, floor, 640f, rgb(0x5A6BA8))
        val hx = 300f; val hy = floor - 150f
        g.fillCircle(hx, hy, 46f, Props.SKIN_TONES[3]); g.fillCircle(hx - 8f, hy - 14f, 38f, rgb(0x24160F))
        g.fillCircle(hx + 14f, hy - 4f, 5f, rgb(0xFFFFFF)); g.fillCircle(hx + 16f, hy - 4f, 2.5f, rgb(0x14100C))
        g.fillRoundRect(hx + 40f, floor - 180f, 780f, floor - 70f, 40f, rgb(0x8C98D0))
        Props.clock(g, 880f, 150f, 52f, th, t)
        // Les pensées tournent au-dessus du lit : le travail, des messages, des tâches inachevées (icônes, aucun texte inventé).
        for (i in 0..2) {
            val u = SK.loop(t, 5200f, i * 1300f); val a = SK.ease(t, 300f + i * 700f, 500f)
            val x = hx + 110f + 150f * i + 24f * sin(u * 6.28f + i); val y = hy - 150f - 60f * (i % 2) - 24f * sin(u * 6.28f)
            g.withPose(Pose(alpha = a, scale = 0.7f + 0.3f * a), x, y) {
                g.fillCircle(x, y, 46f, th.panel.withAlpha(0.92f)); g.strokeCircle(x, y, 46f, 3f, th.line)
                when (i) { 0 -> SK.laptop(g, x, y + 8f, 54f, th); 1 -> SK.envelope(g, x, y, 44f, th.accent, th.bgTop); else -> { for (k in 0..2) { g.strokeRoundRect(x - 22f, y - 24f + k * 18f, x - 10f, y - 12f + k * 18f, 2f, 2f, th.muted); g.line(x - 2f, y - 18f + k * 18f, x + 22f, y - 18f + k * 18f, 3f, th.muted) } } }
            }
        }
    }

    private val sleep = beat("sleep", "Dormir, téléphone posé", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.DEFINE), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.DORMIR, Thing.SOMMEIL) + setOf("topic:night", "topic:hope"), calm = true, minMs = 4000, needs = setOf("topic:sleepgood")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomNight(c)
        Props.bed(g, 560f, floor, 600f, rgb(0x5A6BA8))
        // Le téléphone est posé sur la table de nuit, écran éteint.
        g.fillRoundRect(840f, floor - 150f, 960f, floor, 8f, rgb(0x6B5A48))
        g.fillRoundRect(870f, floor - 166f, 930f, floor - 150f, 6f, rgb(0x20242E))
        val fade = SK.ease(t, 1200f, 900f)
        g.fillRoundRect(878f, floor - 163f, 922f, floor - 152f, 3f, mix(th.accent, rgb(0x20242E), fade))
        // La personne dort : tête sur l'oreiller, couverture remontée.
        g.save(); g.translate(0f, 0f)
        val hx = 335f; val hy = floor - 150f
        g.fillCircle(hx, hy, 46f, Props.SKIN_TONES[3]); g.fillCircle(hx - 8f, hy - 14f, 38f, rgb(0x24160F))
        g.line(hx + 10f, hy + 2f, hx + 30f, hy + 2f, 3f, rgb(0x2A1A12))
        g.fillRoundRect(hx + 40f, floor - 180f, 800f, floor - 70f, 40f, rgb(0x8C98D0))
        g.restore()
        for (k in 0..2) { val u = SK.loop(t, 3600f, k * 1200f); val x = 360f + u * 80f; val y = floor - 220f - u * 130f; SK.text(g, "Z", x, y, 26f + k * 8f, th.ink.withAlpha(1f - u), 800) }
    }

    private val morning = beat("morning", "Le matin, la lumière revient", SitKit.BODY + setOf(Role.RECAP, Role.ADVICE, Role.CONTRAST, Role.DEFINE, Role.MEASURE), setOf(Goal.APAISER, Goal.MEMORISER, Goal.MOTIVER),
        cueSet(Thing.RESPIRATION) + setOf("topic:hope", "topic:breath", "topic:walk"), calm = true, minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val rise = SK.ease(t, 300f, 2800f)
        val floor = Props.room(g, mix(rgb(0x3A3F7A), rgb(0xF2DDB8), rise), mix(th.bgBottom, rgb(0x8A5A3A), rise * 0.6f), 590f)
        Props.window(g, 380f, 60f, 700f, 360f, mix(rgb(0x182349), rgb(0xBFE3F5), rise), mix(rgb(0x5A6190), rgb(0xFFFFFF), rise))
        Props.sun(g, 540f, lerp(360f, 200f, rise), 42f, t)
        g.save(); g.clipRect(380f, 60f, 700f, 360f); Props.ground(g, 330f, rgb(0x4C9A5E), 1000f, 360f); g.restore()
        val fa = Face.mix(Face.TIRED, Face.HAPPY, rise)
        Cast.draw(g, 220f, floor, 400f, Cast.persona(1), fa, Body.mix(Poses.slump(t), Poses.open(t), rise), t, 1)
        Cast.draw(g, 840f, floor, 380f, Cast.persona(3), Face.mix(Face.NEUTRAL, Face.SMILE, rise), Body.mix(Poses.idle(t), Poses.wave(t), SK.ease(t, 2600f, 400f)), t, 3, mirror = true)
    }

    private val consult = beat("consult", "Consulter un professionnel", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.DEFINE, Role.RECOGNIZE), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Actor.PROFESSIONNEL, Act.PARTAGER) + setOf("topic:pro", "topic:talk", "topic:alone"), calm = true, minMs = 4000, needs = setOf("topic:pro")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = roomDay(c)
        Props.plant(g, 930f, floor, 200f)
        g.fillRoundRect(420f, floor - 60f, 590f, floor - 8f, 10f, rgb(0x6B5A48))
        val talk = 0.5f + 0.5f * sin(t / 100f)
        Props.chair(g, 250f, floor, 380f); Props.chair(g, 760f, floor, 380f)
        Cast.draw(g, 250f, floor, 380f, Cast.persona(0), Face.mix(Face.WORRIED, Face.RELIEVED, SK.ease(t, 2800f, 1200f)).copy(mouthOpen = if (SK.loop(t, 4000f) < 0.6f) 0.2f * talk else 0f), Poses.holdFront(t).copy(seated = true), t, 0)
        Cast.draw(g, 760f, floor, 380f, Cast.persona(5), Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 2000f, 1000f)), Body.mix(Poses.idle(t).copy(seated = true), Poses.hand2Chin(t).copy(seated = true), 0.7f), t, 5, mirror = true)
        // Un carnet de notes et une bulle d'écoute (oreille) : on est écouté, sans jugement.
        g.fillRoundRect(470f, floor - 126f, 540f, floor - 70f, 6f, rgb(0xF6EFD9))
        for (k in 0..1) g.fillRoundRect(480f, floor - 114f + k * 18f, 480f + 40f * clamp01(window(t, 800f + k * 1000f, 1000f)) + 10f, floor - 108f + k * 18f, 3f, rgb(0x9A8F6E))
        Props.bubble(g, 620f, 90f, 900f, 190f, 780f, 260f, th.panelHi, th.line, SK.pop(t, 1800f, 500f))
        for (k in 0..2) g.fillCircle(700f + k * 50f, 140f, 8f, th.muted.withAlpha(0.5f + 0.5f * sin(t / 250f + k)))
    }



    /** Une carte par geste de la liste de l'auteur, dans son ordre ; chaque carte montre une personne qui fait ce geste, et s'allume à son tour. */
    private val gestures = beat("gestures", "Chaque geste, une personne qui le fait", SitKit.BODY + setOf(Role.RECAP), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE, Goal.SUIVRE_PROCEDURE),
        cueSet(Act.RESPIRER, Act.ECRIRE, Act.MARCHER, Act.PARTAGER, Act.DORMIR) + setOf("topic:breath", "topic:journal", "topic:walk", "topic:talk", "topic:night"), calm = true, minMs = 6000, needs = setOf("topic:breath")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        g.fillVerticalGradient(0f, 0f, 1000f, 700f, mix(th.bgTop, th.bgBottom, 0.4f), th.bgBottom)
        val motifs = c.intent.itemMotifs.map { it.ifBlank { "talk" } }.ifEmpty { listOf("breath", "journal", "walk", "talk", "sleep") }.take(5)
        val n = motifs.size; val cw = 920f / n; val per = (c.scene.durationMs.toFloat() / n).coerceIn(1200f, 3600f)
        for ((i, m) in motifs.withIndex()) {
            val x0 = 40f + i * cw; val cx = x0 + cw / 2f
            val act = clamp01(1f - kotlin.math.abs((t - (400f + i * per)) / per).coerceAtMost(1f))
            val p = SK.pop(t, 200f + i * 250f, 500f)
            g.withPose(Pose(alpha = p, scale = 0.9f + 0.1f * p), cx, 350f) {
                g.fillRoundRect(x0 + 6f, 90f, x0 + cw - 6f, 610f, 22f, th.panel); g.strokeRoundRect(x0 + 6f, 90f, x0 + cw - 6f, 610f, 22f, 4f, mix(th.line, th.accent, act))
                val h = 250f; val fl = 520f
                val word = when (m) { "breath" -> "RESPIRER"; "name", "journal" -> "ÉCRIRE"; "walk" -> "MARCHER"; "sleep" -> "DORMIR"; "talk" -> "PARLER"; else -> "AGIR" }
                when (m) {
                    "breath" -> { val u = SK.loop(t, 4000f); val inh = if (u < 0.5f) u * 2f else 2f - u * 2f
                        g.strokeCircle(cx, fl - h * 0.62f, h * (0.20f + 0.10f * inh), 4f, th.accent.withAlpha(0.8f))
                        Cast.draw(g, cx, fl, h, Cast.persona(2), Face.mix(Face.NEUTRAL, Face.RELIEVED, inh), Poses.idle(t), t, 2) }
                    "name", "journal" -> { val an = Cast.draw(g, cx, fl, h, Cast.persona(1), Face.mix(Face.FOCUSED, Face.SMILE, 0.5f), Poses.holdFront(t), t, 1)
                        val nx = (an.lx + an.rx) / 2f; val ny = (an.ly + an.ry) / 2f - 6f
                        g.fillRoundRect(nx - 30f, ny - 24f, nx + 30f, ny + 24f, 5f, rgb(0xF6EFD9)); g.strokeRoundRect(nx - 30f, ny - 24f, nx + 30f, ny + 24f, 5f, 2f, rgb(0x8A6B3A))
                        for (k in 0 until 3) g.line(nx - 20f, ny - 12f + k * 12f, nx + 20f - 6f * k, ny - 12f + k * 12f, 2f, rgb(0x6B7280)) }
                    "walk" -> { Props.tree(g, x0 + cw * 0.24f, fl, h * 0.62f, t = t); Cast.draw(g, cx + 14f, fl, h, Cast.persona(0), Face.SMILE, Poses.walk(t, 700f), t, 0) }
                    "sleep" -> { Props.moon(g, cx + cw * 0.2f, 190f, 24f, th.panel); Props.bed(g, cx - 10f, fl, cw * 0.72f, rgb(0x5A6BA8))
                        SK.text(g, "Z", cx - 6f, 400f - 10f * sin(t / 400f), 26f, th.ink.withAlpha(0.85f), 800, center = true)
                        SK.phone(g, x0 + cw - 30f, fl - 52f, 22f, 38f, th); SK.cross(g, x0 + cw - 30f, fl - 120f, 12f, th.bad) }
                    else -> { Cast.draw(g, cx - cw * 0.2f, fl, h * 0.8f, Cast.persona(0), Face.WORRIED, Poses.idle(t), t, 0)
                        Cast.draw(g, cx + cw * 0.2f, fl, h * 0.8f, Cast.persona(5), Face.SMILE, Poses.idle(t), t, 5, mirror = true)
                        Props.bubble(g, cx - 56f, fl - h * 0.8f - 74f, cx + 20f, fl - h * 0.8f - 24f, cx - 14f, fl - h * 0.8f - 6f, th.panelHi, th.line, 0.95f)
                        SK.heart(g, cx + 36f, fl - h * 0.8f - 48f, 12f + 3f * act, th.bad) }
                }
                SK.pill(g, word, cx, 585f, th.ink, th.bgTop.withAlpha(0.85f), 18f)
                SK.pill(g, (i + 1).toString(), x0 + 40f, 130f, th.ink, if (act > 0.5f) th.accent.withAlpha(0.8f) else th.bgTop.withAlpha(0.8f), 24f)
            }
        }
    }

    private val poster = beat("poster", "Affiche : un souffle à la fenêtre", setOf(Role.INTRO), setOf(Goal.MEMORISER), emptySet(), calm = true, repeatable = true) { c ->
        val g = c.g; val t = c.t
        val floor = roomDay(c, rgb(0xCDE9F7)); Props.sun(g, 210f, 150f, 30f, t)
        val u = SK.loop(t, 8000f); val inh = if (u < 0.5f) Easing.EASE_IN_OUT.ease(u * 2f) else 1f - Easing.EASE_IN_OUT.ease((u - 0.5f) * 2f)
        val an = Cast.draw(g, 620f, floor, 440f, Cast.persona(1), Face.mix(Face.NEUTRAL, Face.RELIEVED, inh), Poses.idle(t), t, 1)
        g.strokeCircle(620f, an.shoulderY - 20f, 150f + inh * 80f, 5f, c.th.accent.withAlpha(0.3f + 0.4f * inh))
    }

    val kind = SceneKind("sit-mental", "Bien-être en situation", setOf(Concept.STRESS), listOf(night, awake, overload, breatheWindow, bench, walk, notebook, sleep, morning, consult, gestures, poster))
}
