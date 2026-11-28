// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase C — aide humanitaire en SITUATION : un camp, une distribution, un point de santé, une classe sous une tente, une famille qui arrive, une chaîne de
 * bénévoles. Les personnes sont montrées debout, actives, avec dignité (aucune mise en scène de la misère). Aucun logo, aucun emblème protégé : le
 * pictogramme de santé est une croix verte générique. Les situations sont illustratives (non documentaires) et le disent.
 */
internal object SitAid {
    private fun dusk(c: StageCtx, horizon: Float = 400f, ground: Int = rgb(0xC9A66B)): Float {
        val g = c.g; val t = c.t
        Props.sky(g, rgb(0x5F8FD0), rgb(0xFBD9A4), horizon)
        Props.sun(g, 840f, 190f, 38f, t, rgb(0xFFE29A))
        Props.cloud(g, (SK.loop(t, 30000f) * 1300f) - 150f, 100f, 70f, a = 0.75f)
        // Collines lointaines
        val hill = PathData().moveTo(0f, horizon).cubicTo(150f, horizon - 70f, 330f, horizon - 60f, 500f, horizon - 20f).cubicTo(680f, horizon - 70f, 860f, horizon - 60f, 1000f, horizon - 10f).lineTo(1000f, horizon).close()
        g.fillPath(hill, rgb(0x6F9A7A))
        Props.ground(g, horizon, ground)
        return horizon
    }

    private fun caption(c: StageCtx) = SK.caption(c.g, "ILLUSTRATION  -  SITUATION TYPE", 72f, 26f, c.th.accent.withAlpha(0.85f))

    private val camp = beat("camp", "Un camp et ses familles", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.RECOGNIZE), setOf(Goal.MOTIVER, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.COMMUNAUTE, Thing.EAU) + setOf("topic:camp", "topic:family", "topic:water"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val hz = dusk(c)
        // Rangées de tentes à la perspective : les plus lointaines sont les plus petites.
        for (row in 0 until 3) for (i in 0 until 6 - row) {
            val sc = 0.55f + row * 0.28f; val base = hz + 22f + row * 62f
            val x = 70f + i * (190f + row * 22f) + (row % 2) * 70f
            Props.tent(g, x, base, 120f * sc, if ((i + row) % 3 == 0) rgb(0xC9D8E8) else Props.CANVAS, if ((i + row) % 2 == 0) rgb(0x3A7BD5) else rgb(0xD0803A))
            if (row == 1 && i % 2 == 0) for (k in 0..1) { val u = SK.loop(t, 3000f, k * 1500f + i * 400f); g.fillCircle(x + 30f + u * 20f, base - 80f - u * 90f, 6f + u * 14f, Color.white(0.25f * (1f - u))) }
        }
        // Au premier plan : un adulte et un enfant avec un jerrican, d'autres familles qui passent au loin.
        val walkers = listOf(0.0f, 0.5f)
        for ((k, w) in walkers.withIndex()) {
            val u = SK.loop(t, 16000f, w * 16000f); val x = -80f + u * 1160f
            Cast.draw(g, x, hz + 70f, 120f, Cast.persona(k + 2), Face.NEUTRAL, Poses.walk(t, 800f), t, k + 2)
        }
        val a = Cast.draw(g, 330f, 665f, 330f, Cast.persona(1), Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 1500f, 900f)), Poses.idle(t), t, 1)
        Cast.draw(g, 440f, 665f, 200f, Cast.persona(3), Face.SMILE, Poses.wave(t), t, 3)
        Props.jerrycan(g, 560f, 665f, 70f)
        if (a.hr < 0f) return@beat
        caption(c)
    }

    private val distribution = beat("distribution", "Une distribution de vivres", SitKit.BODY + setOf(Role.PROCEDURE, Role.MECHANISM, Role.ADVICE, Role.DEFINE, Role.INTRO), setOf(Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME, Goal.MOTIVER),
        cueSet(Act.PARTAGER, Actor.COMMUNAUTE) + setOf("topic:food", "topic:logistics", "topic:volunteer"), minMs = 4000, needs = setOf("topic:food")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val hz = dusk(c, 360f, rgb(0xCDB27C))
        caption(c)
        Props.truck(g, 800f, 560f, 400f, t = t)
        for (k in 0..2) Props.sack(g, 560f + k * 46f, 560f, 62f)
        Props.crate(g, 700f, 560f, 120f, 80f, label = "VIVRES")
        // Un bénévole en gilet tend un sac ; trois personnes attendent leur tour en file.
        val hand = SK.ease(t, 1500f, 900f)
        val vx = 470f
        val an = Cast.draw(g, vx, 640f, 350f, Cast.persona(5), Face.SMILE, Body.mix(Poses.idle(t), Body(armL = 6f to 4f, armR = 80f to 84f), hand), t, 5, mirror = true)
        Props.vest(g, vx, an, 350f)
        val xs = listOf(230f, 120f, 30f)
        for ((i, x) in xs.withIndex()) {
            val recv = if (i == 0) hand else 0f
            Cast.draw(g, x, 640f - i * 12f, 340f - i * 28f, Cast.persona(i), if (i == 0) Face.mix(Face.NEUTRAL, Face.HAPPY, hand) else Face.NEUTRAL, if (i == 0) Body.mix(Poses.idle(t), Body(armL = 6f to 4f, armR = 80f to 84f), recv) else Poses.idle(t + i * 500f), t, i)
        }
        Props.sack(g, lerp(an.rx - 24f, 300f, hand), lerp(580f, 600f, hand), 62f)
    }

    private val clinic = beat("clinic", "Un point de santé sous la tente", SitKit.BODY + setOf(Role.ADVICE, Role.DEFINE, Role.MECHANISM, Role.RECOGNIZE, Role.PROCEDURE), setOf(Goal.MOTIVER, Goal.APPRENDRE_REFLEXE),
        cueSet(Actor.PROFESSIONNEL, Actor.COMMUNAUTE) + setOf("topic:health", "topic:family"), minMs = 4000, needs = setOf("topic:health")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val hz = dusk(c, 330f, rgb(0xCDB27C))
        caption(c)
        // Grande tente ouverte vue de face, panneau de santé, banc de consultation.
        g.fillRoundRect(250f, 150f, 950f, 590f, 18f, rgb(0xE9E4D6))
        g.fillRect(250f, 150f, 950f, 200f, rgb(0x2FA27A))
        Props.medicalCross(g, 360f, 175f, 30f, bg = rgb(0x1F7C5C))
        SK.text(g, "POINT DE SANTE", 420f, 192f, 30f, rgb(0xFFFFFF))
        g.fillRect(250f, 560f, 950f, 590f, rgb(0xB9AE90))
        Props.bench(g, 700f, 450f, 590f, 220f, rgb(0x9A7B55))
        val fa = Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 2400f, 800f))
        val an = Cast.draw(g, 520f, 590f, 380f, Cast.persona(5), fa, Body.mix(Poses.idle(t), Poses.point(t).copy(armL = 70f to 80f, armR = 6f to 4f), 0.5f), t, 5, mirror = true)
        Props.vest(g, 520f, an, 380f, rgb(0xFFFFFF))
        Cast.draw(g, 700f, 590f, 220f, Cast.persona(3), Face.mix(Face.WORRIED, Face.SMILE, SK.ease(t, 2000f, 1000f)), Poses.idle(t).copy(seated = true), t, 3)
        Cast.draw(g, 850f, 590f, 380f, Cast.persona(1), Face.mix(Face.WORRIED, Face.RELIEVED, SK.ease(t, 2400f, 900f)), Poses.holdFront(t), t, 1, mirror = true)
        // Un thermomètre puis une coche : le suivi se fait étape par étape.
        val p = SK.ease(t, 1800f, 600f)
        g.withPose(Pose(alpha = p), 640f, 340f) {
            g.fillRoundRect(630f, 300f, 650f, 390f, 10f, rgb(0xF4F4F4)); g.fillCircle(640f, 396f, 14f, th.bad); g.fillRect(636f, 340f + (1f - p) * 40f, 644f, 390f, th.bad)
        }
        if (SK.ease(t, 3200f, 400f) > 0.5f) SK.check(g, 640f, 280f, 30f, th.good)
    }

    private val school = beat("school", "Une classe sous la tente", SitKit.BODY + setOf(Role.ADVICE, Role.DEFINE, Role.MECHANISM, Role.PROCEDURE, Role.INTRO), setOf(Goal.MOTIVER, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.COMMUNAUTE) + setOf("topic:school", "topic:family", "topic:volunteer"), minMs = 4000, needs = setOf("topic:school")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        dusk(c, 300f, rgb(0xCDB27C))
        caption(c)
        g.fillRoundRect(100f, 120f, 900f, 600f, 20f, rgb(0xE9E4D6))
        g.fillRect(100f, 560f, 900f, 600f, rgb(0xB9AE90))
        // Tableau avec trois signes tracés peu à peu
        g.fillRoundRect(140f, 170f, 560f, 380f, 10f, rgb(0x2C4A3C)); g.strokeRoundRect(140f, 170f, 560f, 380f, 10f, 8f, rgb(0x8A5A3A))
        val letters = listOf("A", "B", "C", "1", "2")
        for ((i, s) in letters.withIndex()) { val p = SK.ease(t, 800f + i * 700f, 500f); if (p > 0.01f) SK.text(g, s, 190f + i * 72f, 300f, 64f * p, Color.white(0.9f), 700) }
        val an = Cast.draw(g, 650f, 600f, 390f, Cast.persona(0), Face.SMILE, Body.mix(Poses.idle(t), Body(armL = 100f to 120f, armR = 6f to 4f), 0.7f + 0.3f * sin(t / 700f)), t, 0, mirror = true)
        for (i in 0 until 4) {
            val x = 190f + i * 105f
            g.fillRoundRect(x - 40f, 590f, x + 40f, 604f, 6f, rgb(0xB5543A).withAlpha(0.8f))
            Cast.draw(g, x, 598f, 200f, Cast.persona(i + 2), Face.mix(Face.NEUTRAL, Face.HAPPY, SK.ease(t, 1500f + i * 500f, 600f)), Body.mix(Poses.idle(t + i * 300f), Poses.handsUp(t), if (i == 1) 0.6f * (0.5f + 0.5f * sin(t / 600f)) else 0f).copy(seated = true), t, i + 2)
        }
        if (an.hr < 0f) return@beat
    }

    private val arrival = beat("arrival", "Une famille arrive au camp", SitKit.BODY + setOf(Role.DEFINE, Role.CONTRAST, Role.INTRO, Role.MECHANISM, Role.RECOGNIZE), setOf(Goal.MOTIVER, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.COMMUNAUTE) + setOf("topic:family", "topic:camp"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val hz = dusk(c, 340f)
        caption(c)
        // La route défile ; au loin, les tentes du camp se rapprochent.
        val sp = t * 0.12f
        for (i in 0 until 5) { val x = 760f + i * 85f - sp * 0.08f; if (x > 620f) Props.tent(g, x, hz + 12f + (i % 2) * 14f, 70f + (i % 3) * 8f, Props.CANVAS, rgb(0x3A7BD5)) }
        Props.tree(g, 120f - (sp * 0.3f) % 1200f + 1200f, hz + 40f, 220f, t = t)
        g.fillRect(0f, 520f, 1000f, 700f, rgb(0xB5976A))
        for (i in 0 until 8) { val x = ((i * 170f - sp) % 1360f + 1360f) % 1360f - 150f; g.fillRoundRect(x, 640f, x + 90f, 650f, 5f, rgb(0x9A7F58)) }
        // Quatre personnes marchent vers le camp avec des baluchons ; la plus petite tient la main d'un parent.
        val face = Face.mix(Face.TIRED, Face.RELIEVED, SK.ease(t, 2400f, 1500f))
        Cast.draw(g, 250f, 650f, 400f, Cast.persona(0), face, Poses.carry(t, 800f), t, 0)
        Cast.draw(g, 400f, 650f, 360f, Cast.persona(1), face, Poses.carry(t + 200f, 800f), t, 1)
        Cast.draw(g, 520f, 650f, 220f, Cast.persona(3), Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 2800f, 800f)), Poses.walk(t, 600f), t, 3)
        g.fillRoundRect(225f, 410f, 285f, 470f, 20f, rgb(0xA6824F))
    }

    private val chain = beat("chain", "Une chaîne de bénévoles", SitKit.BODY + setOf(Role.RECAP, Role.ADVICE, Role.MECHANISM, Role.PROCEDURE, Role.DEFINE), setOf(Goal.MEMORISER, Goal.MOTIVER, Goal.SUIVRE_PROCEDURE),
        cueSet(Act.PARTAGER, Actor.COMMUNAUTE) + setOf("topic:volunteer", "topic:logistics", "topic:food"), minMs = 4000, needs = setOf("topic:volunteer")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val hz = dusk(c, 360f, rgb(0xCDB27C))
        caption(c)
        Props.truck(g, 120f, 590f, 330f, t = t)
        Props.crate(g, 890f, 600f, 140f, 100f, label = "STOCK")
        val xs = listOf(300f, 440f, 580f, 720f)
        val per = 1100f
        for ((i, x) in xs.withIndex()) {
            val an = Cast.draw(g, x, 640f, 360f, Cast.persona(i), Face.SMILE, Body(armL = 90f to 100f, armR = 90f to 100f), t, i, mirror = false)
            Props.vest(g, x, an, 360f, if (i % 2 == 0) rgb(0xFF8A3D) else rgb(0x3DBBD8))
        }
        // Les colis passent de main en main, du camion vers le stock.
        for (k in 0..2) {
            val u = SK.loop(t, 4400f, k * 1470f)
            val px = lerp(190f, 880f, u); val py = 470f - sin(u * PI.toFloat() * 4f) * 16f
            Props.box(g, px, py + 30f, 56f, 46f, listOf(rgb(0xE2675A), rgb(0x3DBBD8), rgb(0xF2C14E))[k])
        }
        if (per < 0f) return@beat
    }

    private val waterPoint = beat("water-point", "Le point d'eau du camp", SitKit.BODY + setOf(Role.PROCEDURE, Role.ADVICE, Role.MECHANISM, Role.DEFINE, Role.INTRO), setOf(Goal.MOTIVER, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.EAU, Act.PUISER) + setOf("topic:water", "topic:camp", "topic:family"), minMs = 4000, needs = setOf("topic:water")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val hz = dusk(c, 360f)
        caption(c)
        // Réservoir sur pilotis, robinet, filet d'eau, jerricans qui se remplissent à tour de rôle.
        g.fillRoundRect(640f, 190f, 860f, 310f, 24f, rgb(0x3A7BD5)); g.fillRect(660f, 310f, 676f, 560f, rgb(0x6B5A48)); g.fillRect(824f, 310f, 840f, 560f, rgb(0x6B5A48))
        g.fillRect(740f, 310f, 760f, 480f, rgb(0x8E98AA)); g.fillRoundRect(720f, 470f, 800f, 490f, 6f, rgb(0x8E98AA))
        val flow = clamp01(SK.loop(t, 6000f) * 1.25f)
        g.fillRect(756f, 490f, 764f, 590f, rgb(0x6EC6EA).withAlpha(0.85f))
        Props.jerrycan(g, 760f, 590f, 80f, fill = flow)
        for (k in 0..1) Props.jerrycan(g, 560f - k * 70f, 640f, 70f, fill = 0.2f + 0.8f * k)
        Cast.draw(g, 430f, 650f, 360f, Cast.persona(1), Face.SMILE, Poses.carry(t, 900f), t, 1)
        Cast.draw(g, 230f, 650f, 320f, Cast.persona(0), Face.NEUTRAL, Poses.idle(t), t, 0)
        Cast.draw(g, 110f, 650f, 220f, Cast.persona(3), Face.HAPPY, Poses.wave(t), t, 3)
        if (th.good < 0) return@beat
    }

    private val needs = beat("needs", "Les besoins essentiels", SitKit.BODY + setOf(Role.MEASURE, Role.DEFINE, Role.RECAP, Role.MECHANISM, Role.PROCEDURE), setOf(Goal.MEMORISER, Goal.COMPRENDRE_MECANISME, Goal.MOTIVER),
        cueSet(Actor.COMMUNAUTE) + setOf("topic:water", "topic:food", "topic:health", "topic:camp"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        Props.sky(g, mix(th.bgTop, th.bgBottom, 0.4f), th.bgBottom, 700f)
        caption(c)
        val items = listOf("EAU", "VIVRES", "ABRI", "SOINS")
        for ((i, s) in items.withIndex()) {
            val x = 130f + i * 245f; val p = SK.pop(t, 400f + i * 500f, 600f)
            g.withPose(Pose(alpha = p, scale = 0.8f + 0.2f * p), x, 330f) {
                g.fillRoundRect(x - 105f, 120f, x + 105f, 540f, 24f, th.panel); g.strokeRoundRect(x - 105f, 120f, x + 105f, 540f, 24f, 3f, th.line)
                when (i) {
                    0 -> Props.jerrycan(g, x, 300f, 120f, fill = SK.ease(t, 1200f, 2000f))
                    1 -> { Props.sack(g, x - 30f, 300f, 100f); Props.crate(g, x + 40f, 300f, 76f, 60f) }
                    2 -> Props.tent(g, x, 300f, 150f)
                    else -> Props.medicalCross(g, x, 230f, 62f)
                }
                Cast.draw(g, x, 520f, 150f, Cast.persona(i), Face.mix(Face.NEUTRAL, Face.HAPPY, SK.ease(t, 1500f + i * 500f, 700f)), Poses.idle(t + i * 300f), t, i)
                SK.pill(g, s, x, 580f, th.ink, th.bgTop.withAlpha(0.85f), 24f)
            }
        }
    }


    private val poster = beat("poster", "Affiche : une famille au camp", setOf(Role.INTRO), setOf(Goal.MEMORISER), emptySet(), repeatable = true) { c ->
        val g = c.g; val t = c.t
        val hz = dusk(c, 380f); caption(c)
        for (i in 0 until 5) Props.tent(g, 90f + i * 210f, hz + 30f + (i % 2) * 16f, 130f, if (i % 2 == 0) Props.CANVAS else rgb(0xC9D8E8), rgb(0x3A7BD5))
        Cast.draw(g, 400f, 680f, 400f, Cast.persona(1), Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 700f, 800f)), Poses.idle(t), t, 1)
        Cast.draw(g, 560f, 680f, 240f, Cast.persona(3), Face.SMILE, Poses.wave(t), t, 3)
        Cast.draw(g, 250f, 680f, 420f, Cast.persona(0), Face.NEUTRAL, Poses.idle(t + 700f), t, 0)
        Props.jerrycan(g, 680f, 680f, 80f)
    }

    val kind = SceneKind("sit-aid", "Aide humanitaire en situation", setOf(Concept.AID, Concept.WATER), listOf(camp, distribution, clinic, school, arrival, chain, waterPoint, needs, poster), fallbackFor = setOf(Concept.GRATITUDE))
}
