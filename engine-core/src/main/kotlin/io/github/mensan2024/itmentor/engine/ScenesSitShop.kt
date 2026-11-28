// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * Phase C — marketing et vente en SITUATION : un étal, un produit, une cliente, un parcours d'achat. Les produits sont des objets génériques
 * (sacs, boîtes, jerricans…), jamais des marques. Aucun chiffre n'est inventé : les barres de l'entonnoir montrent une forme, pas un résultat.
 */
internal object SitShop {
    private fun market(c: StageCtx): Float {
        val g = c.g; val th = c.th
        Props.sky(g, mix(th.bgTop, rgb(0x6FA6E0), 0.6f), rgb(0xFBE3B4), 420f)
        Props.sun(g, 860f, 130f, 30f, c.t, rgb(0xFFE29A))
        Props.ground(g, 420f, rgb(0xCDB27C))
        Props.tree(g, 90f, 440f, 240f, t = c.t); Props.tree(g, 940f, 440f, 200f, t = c.t, who = 3)
        g.fillRect(0f, 600f, 1000f, 700f, rgb(0xB89C74))
        return 600f
    }

    /** Étal : toile rayée, comptoir, étagère de produits génériques. */
    private fun stall(c: StageCtx, x: Float, floor: Float, w: Float) {
        val g = c.g
        for (k in 0 until 8) {
            val l = x - w / 2f + k * w / 8f
            g.fillRect(l, floor - 330f, l + w / 8f + 1f, floor - 270f, if (k % 2 == 0) rgb(0xE2675A) else rgb(0xF6EFD9))
        }
        g.fillRect(x - w / 2f + 6f, floor - 270f, x - w / 2f + 16f, floor, rgb(0x6B5A48)); g.fillRect(x + w / 2f - 16f, floor - 270f, x + w / 2f - 6f, floor, rgb(0x6B5A48))
        g.fillRect(x - w / 2f + 16f, floor - 220f, x + w / 2f - 16f, floor - 214f, rgb(0x8A5A3A))
        for (i in 0 until 4) Props.box(g, x - w / 2f + 70f + i * (w - 140f) / 3f, floor - 214f, 56f, 50f, listOf(rgb(0x3DBBD8), rgb(0xF2C14E), rgb(0xE2675A), rgb(0x7CC48A))[i])
        Props.desk(g, x, floor - 110f, floor, w - 20f, rgb(0x9A6B3F))
    }

    private val storefront = beat("stall", "L'étal, le produit, la cliente", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.RECOGNIZE), setOf(Goal.CONVAINCRE, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.ENTREPRENEUR, Actor.PROSPECT, Thing.OFFRE) + setOf("topic:shop", "topic:product", "topic:customer"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = market(c)
        stall(c, 640f, floor, 520f)
        val vend = Cast.draw(g, 640f, floor - 110f + 6f, 360f, Cast.persona(1), Face.mix(Face.NEUTRAL, Face.HAPPY, SK.ease(t, 1800f, 700f)), Body.mix(Poses.idle(t), Poses.wave(t), SK.ease(t, 1800f, 500f) * 0.6f), t, 1, mirror = true)
        Props.priceTag(g, 470f, 520f, "PRIX CLAIR", th)
        // La cliente arrive, s'arrête devant les produits, regarde, sourit.
        val u = SK.ease(t, 0f, 2200f)
        val x = lerp(-40f, 240f, u)
        val face = Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 2400f, 700f))
        Cast.draw(g, x, floor + 50f, 420f, Cast.persona(3), Props.gaze(face, x, 400f, 600f, 400f), Body.mix(Poses.walk(t, 700f), Poses.idle(t), u), t, 3)
        Props.bubble(g, 90f, 80f, 380f, 170f, 220f, 260f, th.panelHi, th.line, SK.pop(t, 2600f, 500f))
        if (SK.pop(t, 2900f, 400f) > 0.5f) SK.heart(g, 235f, 125f, 26f, th.bad)
        if (vend.hr < 0f) return@beat
    }

    private val feed = beat("feed", "Un flux qui attire les clients", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.PROCEDURE, Role.ADVICE), setOf(Goal.CONVAINCRE, Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Act.ATTIRER, Actor.PROSPECT) + setOf("topic:attract", "topic:customer"), minMs = 4000, needs = setOf("topic:attract")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = market(c)
        // Un grand téléphone montre des publications ; des personnes se tournent vers l'étal.
        val px = 300f
        SK.phone(g, px, 340f, 270f, 520f, th, screen = th.panel)
        g.save(); g.clipRect(px - 123f, 340f - 218f, px + 123f, 340f + 232f)
        val sc = (t / 14f) % 260f
        for (i in 0..3) {
            val y = 150f + i * 260f - sc
            g.fillRoundRect(px - 112f, y, px + 112f, y + 236f, 14f, th.panelHi)
            g.fillRoundRect(px - 102f, y + 10f, px + 102f, y + 136f, 10f, listOf(rgb(0x3DBBD8), rgb(0xF2C14E), rgb(0xE2675A), rgb(0x7CC48A))[i % 4])
            Props.box(g, px, y + 120f, 70f, 56f, rgb(0xFFFFFF).withAlpha(0.85f))
            SK.heart(g, px - 84f, y + 172f, 16f, th.bad); g.fillRoundRect(px - 56f, y + 164f, px + 40f, y + 176f, 5f, th.muted)
            g.fillRoundRect(px - 100f, y + 196f, px + 60f, y + 206f, 5f, th.line)
        }
        g.restore()
        Props.notifBadge(g, px + 120f, 140f, 5, SK.pop(t, 1200f, 400f), th.bad)
        // Les clients arrivent un à un vers l'étal à droite.
        stall(c, 780f, floor, 360f)
        for (k in 0 until 3) {
            val u = SK.ease(t, 900f + k * 900f, 1800f); val x = lerp(470f, 640f - k * 60f, u)
            Cast.draw(g, x, floor + 20f - k * 8f, 250f - k * 10f, Cast.persona(k + 2), Face.mix(Face.NEUTRAL, Face.HAPPY, u), Body.mix(Poses.walk(t, 700f), Poses.idle(t), u), t, k + 2)
        }
    }

    private val journey = beat("journey", "Le parcours d'un client", SitKit.BODY + setOf(Role.PROCEDURE, Role.MECHANISM, Role.ADVICE, Role.DEFINE, Role.INTRO), setOf(Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME, Goal.CONVAINCRE),
        cueSet(Actor.PROSPECT, Act.CONVERTIR, Act.ATTIRER, Act.FIDELISER) + setOf("topic:customer", "topic:pay", "topic:retain", "topic:attract"), minMs = 5000, y0 = 80f, vh = 540f) { c ->
        val g = c.g; val th = c.th; val t = c.t
        Props.sky(g, mix(th.bgTop, rgb(0x6FA6E0), 0.5f), rgb(0xFBE3B4), 380f)
        Props.ground(g, 380f, rgb(0xCDB27C))
        // Chemin qui serpente, quatre étapes ; les libellés viennent du texte de l'auteur quand il énumère ses étapes.
        val fromText = c.intent.params["steps"]?.split('|')?.takeIf { it.size >= 3 }
        val labels = (fromText ?: listOf("DECOUVRE", "CHOISIT", "ACHETE", "REVIENT")).take(4)
        val n = labels.size
        val xs = List(n) { 170f + it * (660f / (n - 1)) }
        val ys = List(n) { if (it % 2 == 0) 470f else 400f }
        for (i in 0 until n - 1) SK.dashed(g, xs[i], ys[i] + 60f, xs[i + 1], ys[i + 1] + 60f, 8f, rgb(0xB89C74), 22f, 16f, t / 30f)
        val prog = clamp01(t / (c.scene.durationMs * 0.8f).coerceAtLeast(4000f)) * (n - 1)
        for (i in 0 until n) {
            val on = prog >= i - 0.01f
            val p = SK.pop(t, 300f + i * 300f, 500f)
            g.withPose(Pose(alpha = p, scale = 0.7f + 0.3f * p), xs[i], ys[i]) {
                when (i % 4) {
                    0 -> { SK.phone(g, xs[i], ys[i] - 60f, 76f, 140f, th); SK.heart(g, xs[i], ys[i] - 70f, 18f, th.bad) }
                    1 -> { stall(c, xs[i], ys[i] + 80f, 200f) }
                    2 -> { g.fillRoundRect(xs[i] - 70f, ys[i] - 40f, xs[i] + 70f, ys[i] + 80f, 10f, rgb(0x6B5A48)); g.fillRoundRect(xs[i] - 56f, ys[i] - 30f, xs[i] + 56f, ys[i] + 10f, 6f, th.panelHi); SK.check(g, xs[i], ys[i] - 10f, 20f, th.good) }
                    else -> { Props.shoppingBag(g, xs[i], ys[i] + 70f, 90f); SK.heart(g, xs[i], ys[i] - 40f, 22f, th.bad) }
                }
                SK.pill(g, labels[i], xs[i], ys[i] + 130f, if (on) th.good else th.muted, th.bgTop.withAlpha(0.85f), if (labels[i].length > 12) 20f else 24f)
            }
        }
        // Le client avance d'étape en étape.
        val seg = prog.toInt().coerceAtMost(n - 2); val f = prog - seg
        val cx = lerp(xs[seg], xs[seg + 1], Easing.EASE_IN_OUT.ease(f)); val cy = lerp(ys[seg], ys[seg + 1], Easing.EASE_IN_OUT.ease(f)) + 50f
        Cast.draw(g, cx - 40f, cy + 60f, 220f, Cast.persona(0), Face.mix(Face.NEUTRAL, Face.HAPPY, prog / (n - 1)), Poses.walk(t, 600f), t, 0)
    }

    private val checkout = beat("checkout", "L'achat à la caisse", SitKit.BODY + setOf(Role.PROCEDURE, Role.ADVICE, Role.MECHANISM, Role.DEFINE), setOf(Goal.CONVAINCRE, Goal.SUIVRE_PROCEDURE),
        cueSet(Act.CONVERTIR, Thing.OFFRE, Actor.ENTREPRENEUR) + setOf("topic:pay", "topic:product", "topic:customer"), minMs = 4000, needs = setOf("topic:pay")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = market(c)
        Props.desk(g, 500f, floor - 150f, floor + 40f, 460f, rgb(0x9A6B3F))
        Cast.draw(g, 700f, floor - 150f + 40f, 380f, Cast.persona(5), Face.mix(Face.NEUTRAL, Face.HAPPY, SK.ease(t, 2400f, 700f)), Poses.idle(t), t, 5, mirror = true)
        Props.box(g, 480f, floor - 150f, 90f, 80f, rgb(0x3DBBD8))
        Props.priceTag(g, 480f, floor - 270f, "PRIX", th)
        val tap = SK.ease(t, 1400f, 800f)
        val an = Cast.draw(g, 250f, floor + 40f, 420f, Cast.persona(0), Face.mix(Face.FOCUSED, Face.HAPPY, SK.ease(t, 2400f, 600f)), Body.mix(Poses.holdFront(t), Body(armL = 6f to 4f, armR = 60f to 20f), tap * 0.5f), t, 0)
        // Paiement par téléphone : l'écran s'affiche, la coche valide.
        g.withPose(Pose(alpha = tap), 400f, 280f) { SK.phone(g, 400f, 250f, 110f, 200f, th); if (SK.ease(t, 2100f, 400f) > 0.5f) SK.check(g, 400f, 262f, 28f, th.good) }
        if (SK.ease(t, 2500f, 400f) > 0.3f) Props.shoppingBag(g, 290f, floor + 40f, 100f)
        if (an.hr < 0f) return@beat
    }

    private val follow = beat("follow", "Le client revient avec un proche", SitKit.BODY + setOf(Role.ADVICE, Role.RECAP, Role.MECHANISM, Role.DEFINE, Role.PROCEDURE), setOf(Goal.CONVAINCRE, Goal.MEMORISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.FIDELISER, Actor.PROSPECT) + setOf("topic:retain", "topic:customer", "topic:talk"), minMs = 4000, needs = setOf("topic:retain")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = market(c)
        stall(c, 680f, floor, 460f)
        Cast.draw(g, 680f, floor - 110f + 6f, 340f, Cast.persona(1), Face.HAPPY, Poses.wave(t), t, 1, mirror = true)
        // Carte de fidélité : les tampons s'ajoutent.
        g.fillRoundRect(90f, 90f, 400f, 250f, 18f, th.panelHi); g.strokeRoundRect(90f, 90f, 400f, 250f, 18f, 3f, th.warn)
        for (i in 0 until 6) { val p = SK.pop(t, 800f + i * 500f, 400f); val x = 140f + (i % 3) * 100f; val y = 140f + (i / 3) * 70f; g.strokeCircle(x, y, 28f, 3f, th.line); if (p > 0.05f) SK.heart(g, x, y, 18f * p, th.bad) }
        // Elle revient avec une amie.
        val u = SK.ease(t, 1200f, 2200f)
        Cast.draw(g, lerp(-60f, 190f, u), floor + 50f, 400f, Cast.persona(3), Face.mix(Face.NEUTRAL, Face.HAPPY, u), Body.mix(Poses.walk(t, 700f), Poses.idle(t), u), t, 3)
        Cast.draw(g, lerp(-200f, 350f, u), floor + 50f, 380f, Cast.persona(2), Face.mix(Face.NEUTRAL, Face.SMILE, u), Body.mix(Poses.walk(t + 150f, 700f), Poses.idle(t), u), t, 2)
        Props.bubble(g, 420f, 300f, 580f, 372f, 470f, 430f, th.panelHi, th.line, SK.pop(t, 3000f, 500f)); if (SK.pop(t, 3200f, 400f) > 0.5f) SK.heart(g, 500f, 336f, 20f, th.bad)
    }

    private val analytics = beat("analytics", "Lire l'entonnoir : où partent les clients", SitKit.BODY + setOf(Role.MEASURE, Role.ADVICE, Role.DEFINE, Role.MECHANISM, Role.RECAP), setOf(Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME, Goal.MEMORISER),
        cueSet(Act.MESURER) + setOf("topic:measure", "topic:customer", "topic:pay"), minMs = 4000, needs = setOf("topic:measure")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = Props.room(g, mix(th.bgBottom, th.accent, 0.12f), mix(th.bgTop, th.bgBottom, 0.35f), 600f)
        // Tableau d'entonnoir : quatre barres décroissantes (forme, pas de chiffres), personnes qui y entrent.
        val labels = listOf("VISITENT", "HESITENT", "ACHETENT", "REVIENNENT"); val ws = listOf(1f, 0.72f, 0.46f, 0.3f)
        g.fillRoundRect(40f, 70f, 620f, 560f, 22f, th.panel); g.strokeRoundRect(40f, 70f, 620f, 560f, 22f, 3f, th.line)
        for (i in 0..3) {
            val p = SK.ease(t, 500f + i * 600f, 900f); val y = 110f + i * 110f; val w = 500f * ws[i] * p
            g.fillRoundRect(80f, y, 80f + w, y + 70f, 14f, mix(th.accent, th.good, i / 3f))
            for (k in 0 until (4 - i)) if (p > 0.6f) Cast.draw(g, 110f + k * 52f, y + 66f, 70f, Cast.persona(k + i), Face.NEUTRAL, Poses.STAND, t, k + i)
            SK.text(g, labels[i], 80f, y + 98f, 18f, th.muted)
        }
        val face = Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 3200f, 800f))
        val an = Cast.draw(g, 820f, floor, 400f, Cast.persona(5), Props.gaze(face, 820f, 300f, 400f, 300f), Body.mix(Poses.holdFront(t), Body(armL = 6f to 4f, armR = 80f to 84f), SK.ease(t, 2000f, 600f) * 0.8f), t, 5, mirror = true)
        if (an.hr < 0f) return@beat
    }

    private val offer = beat("offer", "Une offre claire", SitKit.BODY + setOf(Role.DEFINE, Role.ADVICE, Role.MECHANISM, Role.RECAP, Role.CONTRAST), setOf(Goal.CONVAINCRE, Goal.MEMORISER),
        cueSet(Thing.OFFRE, Act.CONVERTIR) + setOf("topic:product", "topic:customer", "topic:pay"), minMs = 4000, needs = setOf("topic:product")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = market(c)
        // Socle, produit, étiquette de prix ; trois bénéfices se cochent autour.
        g.fillRoundRect(380f, floor - 40f, 620f, floor + 10f, 12f, rgb(0x8A5A3A))
        Props.box(g, 500f, floor - 40f, 180f, 150f, rgb(0x3DBBD8))
        SK.glow(g, 500f, floor - 120f, 190f, th.accent, 0.2f)
        Props.priceTag(g, 500f, floor - 250f, "PRIX CLAIR", th, size = 34f)
        for (i in 0..2) {
            val p = SK.pop(t, 900f + i * 700f, 500f); val a = -0.6f + i * 0.6f; val x = 500f + sin(a) * 280f; val y = 190f + (i % 2) * 70f
            g.withPose(Pose(alpha = p, scale = 0.8f + 0.2f * p), x, y) { g.fillCircle(x, y, 38f, th.panelHi); g.strokeCircle(x, y, 38f, 3f, th.good); SK.check(g, x, y, 22f, th.good, SK.ease(t, 1100f + i * 700f, 400f)) }
            SK.dashed(g, 500f, floor - 150f, x, y + 38f, 3f, th.good.withAlpha(0.4f * p), phase = t / 40f)
        }
        Cast.draw(g, 190f, floor + 60f, 430f, Cast.persona(3), Face.mix(Face.FOCUSED, Face.HAPPY, SK.ease(t, 2800f, 700f)), Body.mix(Poses.hand2Chin(t), Poses.handsUp(t), SK.ease(t, 3200f, 500f) * 0.5f), t, 3)
        Cast.draw(g, 820f, floor + 60f, 420f, Cast.persona(1), Face.SMILE, Poses.open(t), t, 1, mirror = true)
    }


    private val poster = beat("poster", "Affiche : l'étal et sa cliente", setOf(Role.INTRO, Role.RECAP), setOf(Goal.MEMORISER), emptySet(), repeatable = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val floor = market(c); stall(c, 640f, floor, 560f)
        Cast.draw(g, 640f, floor - 104f, 380f, Cast.persona(1), Face.HAPPY, Poses.wave(t), t, 1, mirror = true)
        Cast.draw(g, 250f, floor + 50f, 430f, Cast.persona(3), Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 600f, 700f)), Poses.idle(t), t, 3)
        SK.heart(g, 250f, 160f - 8f * sin(t / 400f), 28f, th.bad)
    }

    val kind = SceneKind("sit-shop", "Marketing et vente en situation", setOf(Concept.FUNNEL), listOf(storefront, feed, journey, checkout, follow, analytics, offer, poster), fallbackFor = setOf(Concept.METHOD))
}
