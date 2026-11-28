// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Phase C — réseaux informatiques en SITUATION : un bureau et ses postes, la résolution d'un nom, l'attribution d'une adresse, la traduction d'adresses,
 * le Wi-Fi, le tunnel chiffré, la salle technique, le diagnostic d'une panne. Les adresses affichées sont des adresses de DOCUMENTATION
 * (192.0.2.0/24, 198.51.100.0/24, 203.0.113.0/24 : RFC 5737) ou privées (192.168.x.x) ; les noms de domaine sont en « .example » (RFC 2606).
 */
internal object SitNet {
    private fun bg(c: StageCtx) {
        val g = c.g; val th = c.th
        g.fillVerticalGradient(0f, 0f, 1000f, 700f, mix(th.bgTop, th.bgBottom, 0.3f), th.bgBottom)
        // quadrillage « plan d'architecte » très discret
        for (i in 0..10) g.line(i * 100f, 0f, i * 100f, 700f, 1f, th.line.withAlpha(0.12f))
        for (i in 0..7) g.line(0f, i * 100f, 1000f, i * 100f, 1f, th.line.withAlpha(0.12f))
    }

    private fun label(c: StageCtx, s: String, x: Float, y: Float, col: Int = c.th.ink, a: Float = 1f, size: Float = 22f) = SK.pill(c.g, s, x, y, col, c.th.bgTop.withAlpha(0.85f), size, a)
    private fun chip(c: StageCtx, s: String, x: Float, y: Float, col: Int, a: Float = 1f) {
        if (a <= 0.01f) return
        val g = c.g; val f = SK.font(17f, 600, FontFamily.MONO); val w = g.measureText(s, f) + 20f
        g.save(); if (a < 0.999f) g.multiplyAlpha(a)
        g.fillRoundRect(x - w / 2f, y - 18f, x + w / 2f, y + 18f, 10f, c.th.bgTop.withAlpha(0.92f)); g.strokeRoundRect(x - w / 2f, y - 18f, x + w / 2f, y + 18f, 10f, 2.5f, col)
        g.drawText(s, x - w / 2f + 10f, y + 6f, f, col); g.restore()
    }

    private fun along(pts: List<Pair<Float, Float>>, u: Float): Pair<Float, Float> {
        val seg = pts.size - 1; val p = (u.coerceIn(0f, 0.9999f)) * seg; val i = p.toInt(); val f = p - i
        return lerp(pts[i].first, pts[i + 1].first, f) to lerp(pts[i].second, pts[i + 1].second, f)
    }

    private val office = beat("office-flow", "Un bureau, ses postes et la sortie vers Internet", SitKit.BODY + setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.RECOGNIZE), setOf(Goal.VOIR_ARCHITECTURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.PAQUET, Thing.ORDINATEUR, Actor.SWITCH, Actor.ROUTEUR) + setOf("topic:office", "topic:flow", "topic:internet", "topic:gateway", "topic:cabling"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        g.fillRoundRect(30f, 130f, 520f, 600f, 22f, th.panel.withAlpha(0.85f)); g.strokeRoundRect(30f, 130f, 520f, 600f, 22f, 3f, th.line)
        g.fillRect(30f, 560f, 520f, 580f, th.line.withAlpha(0.4f))
        val pcX = listOf(120f, 275f, 430f)
        for ((i, x) in pcX.withIndex()) {
            Cast.draw(g, x, 575f, 185f, Cast.persona(i + 1), if (i == 1) Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 2500f, 600f)) else Face.FOCUSED, Poses.typing(t + i * 190f).copy(seated = true), t, i + 1)
            val deskTop = 575f - 0.30f * 185f + 6f
            Props.laptopBack(g, x, deskTop, 62f); Props.desk(g, x, deskTop, 575f, 130f)
            g.line(x, deskTop - 40f, x, 235f, 4f, th.accent.withAlpha(0.5f))
        }
        SK.switchBox(g, 275f, 215f, 300f, th, t)
        g.line(425f, 215f, 560f, 215f, 5f, th.line)
        SK.router(g, 620f, 230f, 130f, th, t); g.line(685f, 215f, 740f, 215f, 5f, th.line)
        SK.shield(g, 770f, 215f, 46f, th.good.withAlpha(0.25f), th.good, 1f); g.line(810f, 215f, 870f, 215f, 5f, th.line)
        SK.cloud(g, 925f, 200f, 62f, th.accent.withAlpha(0.45f))
        label(c, "POSTES", 275f, 640f, th.muted); label(c, "SWITCH", 275f, 285f, th.muted); label(c, "ROUTEUR", 620f, 300f, th.muted); label(c, "PARE-FEU", 765f, 290f, th.muted, 1f, 18f); label(c, "INTERNET", 925f, 285f, th.muted, 1f, 18f)
        // Les paquets partent des postes, traversent la chaîne vers Internet ; les réponses reviennent.
        for (k in 0 until 3) {
            val x = pcX[k]
            val up = listOf(x to 400f, x to 235f, 275f to 215f, 620f to 215f, 770f to 215f, 925f to 205f)
            val u = SK.loop(t, 4200f, k * 1400f)
            val (px, py) = along(up, u)
            SK.packet(g, px, py, 12f, th.accent, clamp01(u / 0.06f))
            val ud = SK.loop(t + 2100f, 4200f, k * 1400f)
            val (qx, qy) = along(up.reversed(), ud)
            SK.packet(g, qx, qy + 14f, 9f, th.good, clamp01(ud / 0.06f) * 0.9f)
        }
    }

    private val dns = beat("dns", "Le nom devient une adresse", SitKit.BODY + setOf(Role.MECHANISM, Role.PROCEDURE, Role.DEFINE, Role.INTRO), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Act.DEMANDER, Act.REPONDRE, Thing.REQUETE, Thing.REPONSE) + setOf("topic:dns", "topic:internet"), minMs = 5000, needs = setOf("topic:dns")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        Cast.draw(g, 140f, 640f, 330f, Cast.persona(0), Face.mix(Face.FOCUSED, Face.HAPPY, SK.ease(t, 5600f, 500f)), Poses.typing(t).copy(seated = false, armL = 34f to -70f, armR = 34f to -70f), t, 0)
        SK.laptop(g, 230f, 470f, 150f, th)
        SK.text(g, "monsite.example", 190f, 462f, 15f, th.ink, 600, family = FontFamily.MONO)
        SK.server(g, 560f, 190f, 190f, 130f, th, t); label(c, "DNS", 560f, 290f, th.accent)
        SK.server(g, 860f, 480f, 190f, 130f, th, t); label(c, "SERVEUR WEB", 860f, 580f, th.muted)
        // 1) la question, 2) la réponse, 3) la requête vers l'adresse reçue, 4) la page.
        val q = SK.ease(t, 700f, 1400f); val r = SK.ease(t, 2400f, 1400f); val s = SK.ease(t, 4000f, 1500f)
        if (q > 0f && r < 1f) { SK.arrow(g, 290f, 440f, lerp(290f, 480f, q), lerp(440f, 250f, q), 5f, th.accent, 16f); chip(c, "monsite.example ?", 380f, 300f, th.accent, q * (1f - r)) }
        if (r > 0f && s < 1f) { SK.arrow(g, 470f, 270f, lerp(470f, 290f, r), lerp(270f, 440f, r), 5f, th.good, 16f); chip(c, "203.0.113.10", 330f, 400f, th.good, r * (1f - s)) }
        if (s > 0f) { SK.arrow(g, 300f, 500f, lerp(300f, 760f, s), lerp(500f, 490f, s), 5f, th.warn, 16f); chip(c, "vers 203.0.113.10", 540f, 560f, th.warn, s) }
        if (SK.ease(t, 5600f, 400f) > 0.5f) SK.check(g, 230f, 470f, 34f, th.good)
    }

    private val dhcp = beat("dhcp", "Chaque appareil reçoit son adresse", SitKit.BODY + setOf(Role.MECHANISM, Role.PROCEDURE, Role.DEFINE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Act.RELIER, Thing.ORDINATEUR) + setOf("topic:dhcp", "topic:office", "topic:gateway"), minMs = 5000, needs = setOf("topic:dhcp")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        SK.router(g, 500f, 150f, 230f, th, t); label(c, "ROUTEUR  -  DHCP", 500f, 235f, th.accent)
        val devs = listOf(Triple("PC", 760f, "192.168.1.10"), Triple("TEL", 920f, "192.168.1.11"), Triple("IMPR", 590f, "192.168.1.12"))
        for ((n, x, ip) in devs) {
            val y = 450f
            when (n) { "PC" -> SK.laptop(g, x, y, 110f, th); "TEL" -> SK.phone(g, x, y, 56f, 100f, th); else -> { g.fillRoundRect(x - 50f, y - 30f, x + 50f, y + 30f, 8f, th.line); g.fillRect(x - 30f, y - 52f, x + 30f, y - 30f, th.panelHi) } }
            g.line(x, y - 56f, 500f, 190f, 3f, th.line.withAlpha(0.7f)); chip(c, ip, x, y + 80f, th.muted)
        }
        // Le nouvel arrivant branche son portable ; le routeur lui attribue la première adresse libre.
        val u = SK.ease(t, 0f, 1800f)
        val x = lerp(-60f, 260f, u)
        Cast.draw(g, x, 640f, 320f, Cast.persona(3), Face.mix(Face.NEUTRAL, Face.HAPPY, SK.ease(t, 4200f, 600f)), Body.mix(Poses.walk(t, 700f), Poses.holdFront(t), u), t, 3)
        SK.laptop(g, 330f, 520f, 120f, th)
        val a = SK.ease(t, 2200f, 1600f)
        if (a > 0f && a < 1f) chip(c, "192.168.1.23", lerp(500f, 330f, a), lerp(190f, 440f, a), th.good)
        if (a >= 1f) chip(c, "192.168.1.23", 330f, 590f, th.good)
        if (SK.ease(t, 3800f, 400f) > 0.5f) SK.check(g, 330f, 520f, 30f, th.good)
        if (u < 0.3f) return@beat
        SK.dashed(g, 330f, 500f, 500f, 190f, 3f, th.good.withAlpha(0.5f * a), phase = t / 30f)
    }

    private val nat = beat("nat", "Plusieurs adresses privées, une adresse publique", SitKit.BODY + setOf(Role.MECHANISM, Role.DEFINE, Role.PROCEDURE), setOf(Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.ROUTEUR, Thing.PAQUET) + setOf("topic:nat", "topic:gateway", "topic:internet"), minMs = 5000, needs = setOf("topic:nat")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        val ys = listOf(150f, 330f, 510f); val ips = listOf("192.168.1.10", "192.168.1.11", "192.168.1.12")
        for (i in 0..2) {
            SK.laptop(g, 100f, ys[i], 120f, th); chip(c, ips[i], 100f, ys[i] + 80f, th.muted)
            g.line(165f, ys[i], 470f, 330f, 3f, th.line)
        }
        SK.router(g, 540f, 330f, 150f, th, t); label(c, "ROUTEUR  -  NAT", 540f, 410f, th.accent)
        g.line(615f, 330f, 790f, 300f, 5f, th.line)
        SK.cloud(g, 860f, 280f, 90f, th.accent.withAlpha(0.45f)); label(c, "INTERNET", 860f, 370f, th.muted)
        chip(c, "203.0.113.5", 700f, 250f, th.warn)
        for (i in 0..2) {
            val u = SK.loop(t, 4500f, i * 1500f)
            val pts = listOf(165f to ys[i], 470f to 330f, 540f to 330f, 790f to 300f)
            val (px, py) = along(pts, u)
            SK.packet(g, px, py, 11f, if (u < 0.5f) th.accent else th.warn, clamp01(u / 0.05f))
            val ud = SK.loop(t + 2250f, 4500f, i * 1500f); val (qx, qy) = along(pts.reversed(), ud)
            SK.packet(g, qx, qy + 14f, 9f, th.good, clamp01(ud / 0.05f) * 0.9f)
        }
        Cast.draw(g, 330f, 660f, 220f, Cast.persona(5), Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 3000f, 600f)), Poses.thinking(t), t, 5)
    }

    private val wifi = beat("wifi", "Le Wi-Fi : qui peut se connecter", SitKit.BODY + setOf(Role.PROCEDURE, Role.DEFINE, Role.MECHANISM, Role.ADVICE), setOf(Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME, Goal.APPRENDRE_REFLEXE),
        cueSet(Thing.MOT_DE_PASSE, Act.AUTORISER, Act.BLOQUER) + setOf("topic:wifi", "topic:office"), minMs = 4500, needs = setOf("topic:wifi")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        SK.router(g, 500f, 200f, 200f, th, t)
        for (k in 0..3) { val u = SK.loop(t, 2400f, k * 600f); g.strokeArc(500f, 150f, 70f + u * 260f, 210f, 120f, 5f, th.accent.withAlpha(0.7f * (1f - u))) }
        // Invitée connectée (coche), collègue connecté, intrus bloqué par le mot de passe.
        Cast.draw(g, 220f, 650f, 360f, Cast.persona(1), Face.mix(Face.FOCUSED, Face.HAPPY, SK.ease(t, 1800f, 600f)), Poses.holdFront(t), t, 1)
        SK.phone(g, 215f, 500f, 60f, 110f, th); if (SK.ease(t, 1800f, 400f) > 0.5f) SK.check(g, 215f, 505f, 20f, th.good)
        SK.laptop(g, 500f, 520f, 150f, th); Cast.draw(g, 500f, 665f, 190f, Cast.persona(2), Face.FOCUSED, Poses.idle(t), t, 2); if (SK.ease(t, 2600f, 400f) > 0.5f) SK.check(g, 500f, 520f, 26f, th.good)
        SK.hood(g, 840f, 470f, 62f, rgb(0x1B2233), th.bad); SK.laptop(g, 840f, 600f, 130f, th, screen = th.bad.withAlpha(0.3f))
        val blocked = SK.ease(t, 2800f, 500f)
        SK.lock(g, 720f, 320f, 44f, th.warn, th.ink, 0f, if (blocked > 0.2f && blocked < 0.9f) sin(t / 40f) * 4f else 0f)
        if (blocked > 0.5f) SK.cross(g, 720f, 420f, 26f, th.bad)
        label(c, "INTRUS", 840f, 665f, th.bad); label(c, "MOT DE PASSE", 720f, 270f, th.warn, 1f, 20f)
    }

    private val vpn = beat("vpn", "Le tunnel chiffré du télétravail", SitKit.BODY + setOf(Role.PROCEDURE, Role.ADVICE, Role.MECHANISM, Role.DEFINE), setOf(Goal.SUIVRE_PROCEDURE, Goal.APPRENDRE_REFLEXE, Goal.COMPRENDRE_MECANISME),
        cueSet(Act.CHIFFRER, Actor.UTILISATEUR) + setOf("topic:vpn", "topic:internet", "topic:office"), minMs = 5000, needs = setOf("topic:vpn")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        // Maison à gauche, siège de l'entreprise à droite, Internet au milieu ; le tunnel traverse Internet.
        val hx = 130f
        g.fillRoundRect(hx - 100f, 330f, hx + 100f, 560f, 10f, rgb(0xC98F5F)); val roof = PathData().moveTo(hx - 125f, 335f).lineTo(hx, 230f).lineTo(hx + 125f, 335f).close(); g.fillPath(roof, rgb(0x8C5A2B))
        Cast.draw(g, hx - 20f, 560f, 220f, Cast.persona(0), Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 3000f, 700f)), Poses.typing(t).copy(seated = false, armL = 34f to -70f, armR = 34f to -70f), t, 0)
        SK.laptop(g, hx + 40f, 460f, 90f, th)
        g.fillRoundRect(760f, 220f, 960f, 560f, 10f, th.panelHi); for (r in 0..4) for (q in 0..2) g.fillRoundRect(780f + q * 58f, 245f + r * 56f, 820f + q * 58f, 280f + r * 56f, 4f, if ((r + q) % 3 == 0) th.warn.withAlpha(0.8f) else th.line)
        SK.server(g, 860f, 600f, 130f, 80f, th, t)
        SK.cloud(g, 500f, 250f, 160f, th.line.withAlpha(0.5f)); label(c, "INTERNET", 500f, 190f, th.muted)
        // Le tunnel : un tube opaque qui traverse le nuage, avec des paquets verrouillés à l'intérieur.
        val y = 420f
        g.fillRoundRect(240f, y - 36f, 760f, y + 36f, 36f, th.accent.withAlpha(0.28f)); g.strokeRoundRect(240f, y - 36f, 760f, y + 36f, 36f, 4f, th.accent)
        for (k in 0 until 4) { val u = SK.loop(t, 4000f, k * 1000f); val px = lerp(260f, 740f, u); SK.packet(g, px, y, 14f, th.accent); SK.lock(g, px, y - 3f, 9f, th.ink, th.ink) }
        SK.lock(g, 400f, y + 90f, 30f, th.good, th.ink); label(c, "TUNNEL CHIFFRE", 400f, y + 170f, th.good)
        // Un curieux voit passer le trafic mais ne peut rien lire.
        SK.hood(g, 660f, 570f, 40f, rgb(0x1B2233), th.bad); label(c, "ILLISIBLE", 660f, 660f, th.bad, 1f, 18f)
        if (SK.ease(t, 2400f, 500f) > 0.5f) SK.cross(g, 740f, 560f, 20f, th.bad)
    }

    private val rack = beat("rack", "La salle technique et ses baies", SitKit.BODY + setOf(Role.DEFINE, Role.PROCEDURE, Role.MECHANISM, Role.INTRO, Role.ADVICE), setOf(Goal.VOIR_ARCHITECTURE, Goal.SUIVRE_PROCEDURE),
        cueSet(Actor.SERVEUR, Actor.BASE_DE_DONNEES, Thing.JOURNAUX_SYSTEME) + setOf("topic:serverroom", "topic:cabling", "topic:office"), minMs = 4000, needs = setOf("topic:serverroom")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        g.fillVerticalGradient(0f, 0f, 1000f, 600f, mix(th.bgTop, th.bgBottom, 0.5f), th.bgBottom); g.fillRect(0f, 600f, 1000f, 700f, th.line.withAlpha(0.5f))
        for (r in 0..2) {
            val x = 190f + r * 230f
            g.fillRoundRect(x - 100f, 70f, x + 100f, 600f, 12f, rgb(0x20242E)); g.strokeRoundRect(x - 100f, 70f, x + 100f, 600f, 12f, 3f, th.line)
            for (k in 0..3) SK.server(g, x, 160f + k * 120f, 170f, 90f, th, t + r * 300f)
            for (k in 0..2) { val y0 = 130f + k * 160f; val col = listOf(th.accent, th.warn, th.good)[(k + r) % 3]; val p = PathData().moveTo(x + 85f, y0).cubicTo(x + 130f, y0 + 20f, x + 130f, y0 + 90f, x + 115f + (r % 2) * 20f, y0 + 150f); g.strokePath(p, 5f, col) }
        }
        val an = Cast.draw(g, 880f, 640f, 400f, Cast.persona(2), Face.mix(Face.NEUTRAL, Face.SMILE, SK.ease(t, 2200f, 800f)), Body.mix(Poses.holdFront(t), Body(armL = 6f to 4f, armR = 80f to 100f), SK.ease(t, 1200f, 600f) * 0.7f), t, 2, mirror = true)
        g.fillRoundRect(an.lx - 36f, an.ly - 52f, an.lx + 36f, an.ly + 38f, 8f, th.panelHi); SK.text(g, "OK", an.lx, an.ly + 6f, 22f, th.good, center = true)
    }

    private val trouble = beat("trouble", "Diagnostiquer une panne maillon par maillon", SitKit.BODY + setOf(Role.ADVICE, Role.PROCEDURE, Role.RECOGNIZE, Role.MECHANISM), setOf(Goal.SUIVRE_PROCEDURE, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.VERIFIER, Act.SURVEILLER, Thing.JOURNAUX_SYSTEME) + setOf("topic:troubleshoot", "topic:cabling", "topic:dns", "topic:gateway"), minMs = 5000, needs = setOf("topic:troubleshoot")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        Cast.draw(g, 160f, 640f, 360f, Cast.persona(5), Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 5400f, 600f)), Poses.thinking(t), t, 5)
        val names = listOf("CABLE", "SWITCH", "PASSERELLE", "DNS", "INTERNET"); val xs = List(5) { 340f + it * 138f }
        // La sonde avance : chaque maillon est testé ; le DNS échoue, on le corrige, tout passe au vert.
        for (i in 0..4) {
            val x = xs[i]; val ts = 800f + i * 900f; val p = SK.ease(t, ts, 400f)
            val failing = i == 3
            val fixed = SK.ease(t, 5200f, 400f) > 0.5f
            val col = if (p < 0.5f) th.line else if (failing && !fixed) th.bad else th.good
            g.fillRoundRect(x - 54f, 260f, x + 54f, 380f, 16f, th.panel); g.strokeRoundRect(x - 54f, 260f, x + 54f, 380f, 16f, 4f, col)
            when (i) { 0 -> g.line(x - 28f, 340f, x + 28f, 300f, 6f, th.muted); 1 -> SK.switchBox(g, x, 320f, 80f, th, t); 2 -> SK.router(g, x, 330f, 80f, th, t); 3 -> SK.server(g, x, 320f, 70f, 70f, th, t); else -> SK.cloud(g, x, 320f, 38f, th.muted) }
            if (i < 4) g.line(x + 54f, 320f, xs[i + 1] - 54f, 320f, 4f, if (p > 0.5f && !(failing && !fixed)) th.good else th.line)
            if (p > 0.5f) { if (failing && !fixed) SK.cross(g, x, 430f, 24f, th.bad) else SK.check(g, x, 430f, 24f, th.good) }
            label(c, names[i], x, 485f, th.muted, 1f, 18f)
        }
        val u = SK.loop(t, 1800f); val head = clamp01((t - 700f) / 4300f) * 600f + 380f
        chip(c, "ping 192.0.2.1", 640f, 160f, th.accent, SK.ease(t, 300f, 400f))
        if (head < 0f) return@beat
    }




    private val firstFail = beat("first-fail", "Le premier maillon qui échoue désigne le problème", SitKit.BODY + setOf(Role.RECAP), setOf(Goal.APPRENDRE_REFLEXE, Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Act.VERIFIER) + setOf("topic:firstfail", "topic:troubleshoot"), minMs = 4000, needs = setOf("topic:firstfail")) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        val names = listOf("POSTE", "SWITCH", "PASSERELLE", "DNS", "INTERNET"); val xs = List(5) { 120f + it * 190f }
        val bad = 2
        for (i in 0..4) {
            val x = xs[i]; val ts = 500f + i * 700f; val p = SK.ease(t, ts, 350f)
            val state = if (p < 0.5f) 0 else if (i < bad) 1 else if (i == bad) 2 else 3
            val col = when (state) { 1 -> th.good; 2 -> th.bad; 3 -> th.line.withAlpha(0.6f); else -> th.line }
            if (i < 4) g.line(x + 54f, 290f, xs[i + 1] - 54f, 290f, 5f, if (state == 1) th.good else th.line)
            g.fillRoundRect(x - 56f, 230f, x + 56f, 350f, 18f, th.panel); g.strokeRoundRect(x - 56f, 230f, x + 56f, 350f, 18f, 4f, col)
            when (i) { 0 -> SK.laptop(g, x, 290f, 70f, th); 1 -> SK.switchBox(g, x, 290f, 80f, th, t); 2 -> SK.router(g, x, 300f, 80f, th, t); 3 -> SK.server(g, x, 290f, 70f, 70f, th, t); else -> SK.cloud(g, x, 290f, 38f, th.muted) }
            when (state) { 1 -> SK.check(g, x, 410f, 24f, th.good); 2 -> SK.cross(g, x, 410f, 26f + 3f * sin(t / 150f), th.bad) }
            label(c, names[i], x, 465f, th.muted, 1f, 18f)
        }
        val a = SK.ease(t, 500f + bad * 700f + 300f, 500f)
        if (a > 0.02f) { SK.arrow(g, xs[bad], 560f, xs[bad], 500f, 5f, th.bad); label(c, "PREMIER ÉCHEC", xs[bad], 590f, th.bad, a, 22f) }
        Cast.draw(g, 880f, 690f, 230f, Cast.persona(5), Face.mix(Face.FOCUSED, Face.SURPRISED, a), Poses.idle(t), t, 5, mirror = true)
    }

    private val recap = beat("recap", "À retenir : une chaîne, chaque maillon compris", setOf(Role.RECAP), setOf(Goal.MEMORISER), cueSet(Thing.PAQUET) + setOf("topic:flow", "topic:troubleshoot", "topic:gateway", "topic:office"), minMs = 4000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        val names = listOf("POSTE", "SWITCH", "ROUTEUR", "PARE-FEU", "INTERNET"); val xs = List(5) { 110f + it * 195f }
        for (i in 0..4) {
            val x = xs[i]; val p = SK.pop(t, 300f + i * 600f, 500f); val ok = SK.ease(t, 900f + i * 600f, 300f) > 0.5f
            if (i < 4) g.line(x + 54f, 330f, xs[i + 1] - 54f, 330f, 5f, if (ok) th.good else th.line)
            g.withPose(Pose(alpha = p, scale = 0.7f + 0.3f * p), x, 330f) {
                g.fillRoundRect(x - 56f, 270f, x + 56f, 390f, 18f, th.panel); g.strokeRoundRect(x - 56f, 270f, x + 56f, 390f, 18f, 4f, if (ok) th.good else th.line)
                when (i) { 0 -> SK.laptop(g, x, 330f, 70f, th); 1 -> SK.switchBox(g, x, 330f, 80f, th, t); 2 -> SK.router(g, x, 340f, 80f, th, t); 3 -> SK.shield(g, x, 330f, 36f, th.good.withAlpha(0.25f), th.good, 1f); else -> SK.cloud(g, x, 330f, 38f, th.muted) }
            }
            if (ok) SK.check(g, x, 440f, 24f, th.good)
            label(c, names[i], x, 500f, th.muted, p, 18f)
        }
        val u = SK.loop(t, 3600f); val (px, py) = along(xs.map { it to 330f }, u); SK.packet(g, px, py, 12f, th.accent)
        Cast.draw(g, 500f, 690f, 200f, Cast.persona(5), Face.mix(Face.FOCUSED, Face.SMILE, SK.ease(t, 3500f, 600f)), Poses.idle(t), t, 5)
    }

    private val poster = beat("poster", "Affiche : des postes reliés à Internet", setOf(Role.INTRO), setOf(Goal.MEMORISER), emptySet(), repeatable = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        bg(c)
        for (k in 0..2) { val x = 160f + k * 140f; SK.laptop(g, x, 520f, 110f, th); g.line(x, 470f, 330f, 330f, 4f, th.accent.withAlpha(0.6f)) }
        SK.switchBox(g, 330f, 310f, 220f, th, t); g.line(440f, 310f, 560f, 310f, 5f, th.line); SK.router(g, 620f, 320f, 130f, th, t); g.line(685f, 310f, 780f, 280f, 5f, th.line)
        SK.cloud(g, 860f, 260f, 80f, th.accent.withAlpha(0.5f))
        for (k in 0..2) { val u = SK.loop(t, 3000f, k * 1000f); val (px, py) = along(listOf(160f + k * 140f to 480f, 330f to 330f, 620f to 320f, 860f to 270f), u); SK.packet(g, px, py, 11f, th.accent, clamp01(u / 0.05f)) }
        Cast.draw(g, 830f, 680f, 340f, Cast.persona(2), Face.FOCUSED, Poses.idle(t), t, 2, mirror = true)
    }

    val kind = SceneKind("sit-net", "Réseaux en situation", setOf(Concept.NETFLOW), listOf(office, dns, dhcp, nat, wifi, vpn, rack, trouble, firstFail, recap, poster),
        fallbackFor = setOf(Concept.TOPOLOGY, Concept.CLIENT_SERVER, Concept.SEGMENTATION, Concept.FIREWALL))
}
