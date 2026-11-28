// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/** Topologie, segmentation (VLAN), trunk, routage : schémas pédagogiques d'un réseau d'entreprise. */
internal object TopologyScenes {
    private fun vlanColors(c: StageCtx) = listOf(c.th.accent, c.th.warn, c.th.good)

    private fun vlanLabels(c: StageCtx): List<String> =
        (c.intent.params["vlans"]?.split(",")?.map { "VLAN $it" } ?: listOf("VLAN 10", "VLAN 20", "VLAN 30"))

    private fun link(g: Gfx, x0: Float, y0: Float, x1: Float, y1: Float, th: StageTheme, color: Int = th.line, w: Float = 5f) = g.line(x0, y0, x1, y1, w, color)

    private fun travel(g: Gfx, th: StageTheme, pts: List<Pair<Float, Float>>, u: Float, color: Int, s: Float = 11f) {
        val lens = (0 until pts.size - 1).map { hypot(pts[it + 1].first - pts[it].first, pts[it + 1].second - pts[it].second) }
        var d = u * lens.sum()
        for (i in lens.indices) {
            if (d <= lens[i]) {
                val f = d / lens[i]
                SK.packet(g, lerp(pts[i].first, pts[i + 1].first, f), lerp(pts[i].second, pts[i + 1].second, f), s, color)
                return
            }
            d -= lens[i]
        }
    }

    private val star = beat("star", "Topologie en étoile", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM), setOf(Goal.VOIR_ARCHITECTURE),
        cueSet(Actor.SWITCH, Act.RELIER, Thing.ORDINATEUR, Actor.ROUTEUR)) { c ->
        val g = c.g; val th = c.th; val t = c.t; val tg = c.tg
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val cx = 500f; val sy = 330f
        val nodes = listOf(150f to 520f, 330f to 580f, 670f to 580f, 850f to 520f, 120f to 340f, 880f to 340f)
        val app = SK.ease(t, 100f, 800f, Easing.EXPO_OUT)
        SK.cloud(g, cx, 52f, 52f, th.panelHi.withAlpha(app))
        link(g, cx, 80f, cx, 170f, th, th.line.withAlpha(app))
        SK.router(g, cx, 195f, 150f, th, tg)
        link(g, cx, 225f, cx, sy - 30f, th, th.line.withAlpha(app))
        SK.switchBox(g, cx, sy, 200f, th, tg)
        for ((i, n) in nodes.withIndex()) {
            val p = SK.pop(t, 500f + i * 220f, 500f)
            if (p <= 0f) continue
            link(g, cx + (n.first - cx) * 0.18f, sy + 40f, n.first, n.second - 34f, th, th.line.withAlpha(clamp01(p)))
            g.withPose(Pose(alpha = clamp01(p), scale = 0.7f + 0.3f * clamp01(p)), n.first, n.second) {
                if (i == 1) SK.server(g, n.first, n.second, 80f, 90f, th, tg) else SK.pc(g, n.first, n.second, 62f, th)
            }
        }
        val a = SK.loop(t, 3000f)
        val src = nodes[0]; val dst = nodes[3]
        val path = listOf(src.first to src.second - 34f, cx - 40f to sy + 40f, cx + 40f to sy + 40f, dst.first to dst.second - 34f)
        if (t > 3000f) travel(g, th, path, a, th.good)
        SK.pill(g, "COMMUTATEUR", cx + 190f, sy + 8f, th.ink, th.bgTop.withAlpha(0.85f), 20f, app)
    }

    private val path = beat("path", "Le chemin d'un paquet", setOf(Role.MECHANISM, Role.PROCEDURE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.PAQUET, Actor.ROUTEUR, Act.RELIER), y0 = 120f, vh = 400f) { c ->
        val g = c.g; val th = c.th; val t = c.t; val tg = c.tg
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 144f, th.accent.withAlpha(0.85f))
        val y = 350f
        val xs = listOf(90f, 280f, 470f, 660f, 850f)
        val labels = listOf("PC A", "COMMUTATEUR", "ROUTEUR", "COMMUTATEUR", "PC B")
        for (i in 0 until xs.size - 1) link(g, xs[i], y, xs[i + 1], y, th, th.line, 6f)
        for ((i, x) in xs.withIndex()) {
            val p = SK.pop(t, 100f + i * 250f, 500f)
            g.withPose(Pose(alpha = clamp01(p), scale = 0.75f + 0.25f * clamp01(p)), x, y) {
                when (i) { 0, 4 -> SK.pc(g, x, y, 76f, th); 2 -> SK.router(g, x, y, 130f, th, tg); else -> SK.switchBox(g, x, y, 120f, th, tg) }
            }
            SK.pill(g, labels[i], x, y + 100f, th.muted, th.bgTop.withAlpha(0.8f), 17f, clamp01(p))
        }
        val dur = 5200f
        val u = ((t - 1400f) % (dur + 800f)) / dur
        if (t > 1400f && u in 0f..1f) {
            travel(g, th, xs.map { it to y }, u, th.good, 13f)
            val hop = (u * (xs.size - 1)).toInt().coerceIn(0, xs.size - 1)
            val near = ((u * (xs.size - 1)) - hop)
            val hx = xs[hop]
            SK.glow(g, hx, y, 110f, th.good, 0.28f * (1f - near))
            SK.text(g, "SAUT ${hop + 1}", 500f, 190f, 40f, th.good, center = true, family = FontFamily.MONO)
        }
        if (t > 1400f && u > 1f) SK.pill(g, "ARRIVE", 850f, 235f, th.bgBottom, th.good, 24f)
    }

    private fun pcGroup(g: Gfx, th: StageTheme, x: Float, ys: List<Float>, color: Int, lit: Float) {
        for (y in ys) {
            SK.glow(g, x, y, 60f, color, 0.30f * lit)
            SK.pc(g, x, y, 56f, th, color = mix(th.line, color, 0.7f), screen = mix(th.panelHi, color, 0.35f + 0.4f * lit))
        }
    }

    private val vlan = beat("vlan", "Un réseau, des VLAN", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM), setOf(Goal.VOIR_ARCHITECTURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.VLAN, Thing.PAQUET, Actor.SWITCH), onlyFor = setOf(Concept.SEGMENTATION)) { c ->
        val g = c.g; val th = c.th; val t = c.t; val tg = c.tg
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val cols = vlanColors(c); val names = vlanLabels(c)
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        g.fillRoundRect(30f, 90f, 470f, 640f, 28f, cols[0].withAlpha(0.10f * a))
        g.strokeRoundRect(30f, 90f, 470f, 640f, 28f, 3f, cols[0].withAlpha(0.6f * a))
        g.fillRoundRect(530f, 90f, 970f, 640f, 28f, cols[1].withAlpha(0.10f * a))
        g.strokeRoundRect(530f, 90f, 970f, 640f, 28f, 3f, cols[1].withAlpha(0.6f * a))
        SK.pill(g, names[0], 250f, 126f, th.bgBottom, cols[0], 24f, a); SK.pill(g, names[1], 750f, 126f, th.bgBottom, cols[1], 24f, a)
        SK.switchBox(g, 500f, 380f, 150f, th, tg)
        val ysL = listOf(220f, 380f, 540f)
        // Diffusion : un message envoyé à tous, qui s'arrête à la frontière du VLAN.
        val bc = SK.loop(t - 900f, 4200f)
        val wave = if (t > 900f) clamp01(bc / 0.55f) else 0f
        val lit = clamp01(wave * 2f) * (1f - clamp01((bc - 0.7f) / 0.3f))
        pcGroup(g, th, 110f, ysL, cols[0], if (wave > 0.3f) lit else 0f)
        pcGroup(g, th, 890f, ysL, cols[1], 0f)
        for (y in ysL) { link(g, 150f, y, 440f, 380f, th, cols[0].withAlpha(0.6f), 4f); link(g, 850f, y, 560f, 380f, th, cols[1].withAlpha(0.6f), 4f) }
        if (t > 900f && wave > 0f && bc < 0.8f) {
            val r = 20f + 380f * wave
            g.save(); g.clipRect(30f, 90f, 470f, 640f)
            g.strokeCircle(110f, 380f, r, 6f, cols[0].withAlpha(0.7f * (1f - wave * 0.6f)))
            g.restore()
            if (wave > 0.82f) {
                SK.cross(g, 500f, 300f, 44f, th.bad, 1f, 10f)
                SK.pill(g, "S'ARRETE ICI", 500f, 240f, rgb(0xFFFFFF), th.bad, 22f)
            }
        }
        g.line(500f, 150f, 500f, 330f, 4f, th.bad.withAlpha(0.5f * a))
        g.line(500f, 430f, 500f, 620f, 4f, th.bad.withAlpha(0.5f * a))
    }

    private val trunk = beat("trunk", "Le trunk relie les commutateurs", setOf(Role.PROCEDURE, Role.MECHANISM), setOf(Goal.SUIVRE_PROCEDURE, Goal.VOIR_ARCHITECTURE),
        cueSet(Thing.VLAN, Act.RELIER, Actor.SWITCH), onlyFor = setOf(Concept.SEGMENTATION)) { c ->
        val g = c.g; val th = c.th; val t = c.t; val tg = c.tg
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val cols = vlanColors(c); val names = vlanLabels(c)
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        val lx = 190f; val rx = 810f; val sy = 300f
        SK.switchBox(g, lx, sy, 170f, th, tg); SK.switchBox(g, rx, sy, 170f, th, tg)
        // Le tube du trunk, deux couloirs de couleurs différentes.
        g.fillRoundRect(lx + 85f, sy - 34f, rx - 85f, sy + 34f, 30f, th.panelHi.withAlpha(a))
        g.strokeRoundRect(lx + 85f, sy - 34f, rx - 85f, sy + 34f, 30f, 3f, th.ink.withAlpha(0.5f * a))
        for (k in 0 until 6) {
            val u = SK.loop(t, 3200f, k * 530f)
            val lane = k % 2
            val dir = if (k % 3 == 0) -1 else 1
            val x = if (dir == 1) lerp(lx + 100f, rx - 100f, u) else lerp(rx - 100f, lx + 100f, u)
            g.fillRoundRect(x - 22f, sy - 22f + lane * 24f, x + 22f, sy - 4f + lane * 24f, 8f, cols[lane])
            g.fillRoundRect(x - 22f, sy - 22f + lane * 24f, x - 8f, sy - 4f + lane * 24f, 8f, Color.white(0.5f))
        }
        SK.pill(g, "TRUNK", 500f, 190f, th.bgBottom, th.ink, 28f, a)
        SK.pill(g, "LES DEUX VLAN CIRCULENT SUR UN SEUL LIEN", 500f, 245f, th.ink, th.bgTop.withAlpha(0.85f), 18f, a)
        for ((side, x) in listOf(lx to 0, rx to 1).map { it.second to it.first }) {
            for (k in 0 until 2) {
                val y = 540f; val px = x - 70f + k * 140f
                link(g, x, sy + 26f, px, y - 36f, th, cols[k].withAlpha(0.8f), 4f)
                val p = SK.pop(t, 700f + k * 300f + side * 200f, 500f)
                g.withPose(Pose(alpha = clamp01(p)), px, y) { SK.pc(g, px, y, 62f, th, color = mix(th.line, cols[k], 0.7f), screen = mix(th.panelHi, cols[k], 0.5f)) }
                SK.pill(g, names[k], px, y + 70f, th.bgBottom, cols[k], 18f, clamp01(p))
            }
        }
    }

    private val inter = beat("inter", "Le routeur fait parler deux VLAN", setOf(Role.MECHANISM, Role.DEFINE, Role.PROCEDURE), setOf(Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.ROUTEUR, Thing.VLAN, Act.RELIER), onlyFor = setOf(Concept.SEGMENTATION)) { c ->
        val g = c.g; val th = c.th; val t = c.t; val tg = c.tg
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val cols = vlanColors(c); val names = vlanLabels(c)
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        val ay = 430f
        g.fillRoundRect(30f, 360f, 330f, 640f, 26f, cols[0].withAlpha(0.10f * a)); g.strokeRoundRect(30f, 360f, 330f, 640f, 26f, 3f, cols[0].withAlpha(0.6f * a))
        g.fillRoundRect(670f, 360f, 970f, 640f, 26f, cols[1].withAlpha(0.10f * a)); g.strokeRoundRect(670f, 360f, 970f, 640f, 26f, 3f, cols[1].withAlpha(0.6f * a))
        SK.pc(g, 110f, 520f, 66f, th, color = mix(th.line, cols[0], 0.7f), screen = mix(th.panelHi, cols[0], 0.5f))
        SK.pc(g, 890f, 520f, 66f, th, color = mix(th.line, cols[1], 0.7f), screen = mix(th.panelHi, cols[1], 0.5f))
        SK.pill(g, names[0], 180f, 600f, th.bgBottom, cols[0], 20f, a); SK.pill(g, names[1], 820f, 600f, th.bgBottom, cols[1], 20f, a)
        SK.switchBox(g, 230f, ay + 10f, 130f, th, tg); SK.switchBox(g, 770f, ay + 10f, 130f, th, tg)
        val rp = SK.pop(t, 500f, 600f)
        g.withPose(Pose(alpha = clamp01(rp), scale = 0.7f + 0.3f * clamp01(rp)), 500f, 190f) { SK.router(g, 500f, 190f, 190f, th, tg) }
        SK.pill(g, "ROUTEUR", 500f, 90f, th.bgBottom, th.ink, 24f, clamp01(rp))
        val pts = listOf(110f to 495f, 230f to ay - 10f, 400f to 215f, 500f to 195f, 600f to 215f, 770f to ay - 10f, 890f to 495f)
        for (i in 0 until pts.size - 1) link(g, pts[i].first, pts[i].second, pts[i + 1].first, pts[i + 1].second, th, th.line.withAlpha(0.8f), 5f)
        val u = ((t - 1500f) % 6400f) / 5600f
        if (t > 1500f && u in 0f..1f) {
            val color = if (u < 0.5f) cols[0] else cols[1]
            travel(g, th, pts, u, color, 13f)
            SK.text(g, if (u < 0.5f) names[0] else names[1], 500f, 330f, 30f, color, center = true, family = FontFamily.MONO)
        }
    }

    private val recap = beat("recap", "À retenir : segmenter, relier, router", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Thing.VLAN, Act.RELIER, Actor.ROUTEUR), onlyFor = setOf(Concept.SEGMENTATION)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val cols = vlanColors(c)
        val labels = listOf("UN VLAN PAR SERVICE", "UN TRUNK ENTRE LES SWITCHES", "UN ROUTEUR POUR LES RELIER")
        for (k in 0 until 3) {
            val cy = 130f + k * 190f
            val p = SK.pop(t, 500f + k * 900f, 600f)
            if (p <= 0f) continue
            g.withPose(Pose(alpha = clamp01(p), dx = (1f - clamp01(p)) * -80f), 500f, cy) {
                g.fillRoundRect(60f, cy - 70f, 940f, cy + 70f, 28f, th.panel)
                g.strokeRoundRect(60f, cy - 70f, 940f, cy + 70f, 28f, 3f, cols[k % 3].withAlpha(0.8f))
                when (k) {
                    0 -> { SK.pc(g, 140f, cy, 50f, th, mix(th.line, cols[0], 0.7f)); SK.pc(g, 220f, cy, 50f, th, mix(th.line, cols[1], 0.7f)) }
                    1 -> { g.fillRoundRect(110f, cy - 14f, 250f, cy + 14f, 14f, th.panelHi); g.fillRoundRect(110f, cy - 14f, 180f, cy, 7f, cols[0]); g.fillRoundRect(180f, cy, 250f, cy + 14f, 7f, cols[1]) }
                    else -> SK.router(g, 180f, cy + 4f, 120f, th, c.tg)
                }
                SK.text(g, labels[k], 320f, cy + 12f, 32f, th.ink)
                val ck = SK.ease(t, 900f + k * 900f, 400f)
                g.fillCircle(890f, cy, 26f * ck, th.good); SK.check(g, 890f, cy, 15f * ck, th.bgBottom)
            }
        }
    }

    private val recapStar = beat("recap-star", "À retenir : un commutateur au centre", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Actor.SWITCH, Actor.ROUTEUR, Act.RELIER), onlyFor = setOf(Concept.TOPOLOGY)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val labels = listOf("UN COMMUTATEUR AU CENTRE", "UN ROUTEUR VERS L'EXTERIEUR")
        for (k in 0 until 2) {
            val cy = 190f + k * 250f
            val p = SK.pop(t, 500f + k * 1100f, 600f)
            if (p <= 0f) continue
            g.withPose(Pose(alpha = clamp01(p), dx = (1f - clamp01(p)) * -80f), 500f, cy) {
                g.fillRoundRect(60f, cy - 90f, 940f, cy + 90f, 30f, th.panel)
                g.strokeRoundRect(60f, cy - 90f, 940f, cy + 90f, 30f, 3f, th.accent.withAlpha(0.8f))
                if (k == 0) SK.switchBox(g, 200f, cy, 190f, th, c.tg) else SK.router(g, 200f, cy + 8f, 170f, th, c.tg)
                SK.text(g, labels[k], 330f, cy + 11f, 27f, th.ink)
                val ck = SK.ease(t, 900f + k * 1100f, 400f)
                g.fillCircle(890f, cy, 28f * ck, th.good); SK.check(g, 890f, cy, 16f * ck, th.bgBottom)
            }
        }
    }

    val kind = SceneKind("topology", "Topologie et VLAN", setOf(Concept.TOPOLOGY, Concept.SEGMENTATION), listOf(star, path, vlan, trunk, inter, recap, recapStar), fallbackFor = setOf(Concept.NETFLOW))
}

/** Client et serveur : requête, couches, codes de statut. */
internal object ClientServerScenes {
    private fun reqText(c: StageCtx) = c.intent.params["request"] ?: "GET /"

    private val request = beat("request", "La requête et sa réponse", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.PROCEDURE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.REQUETE, Thing.REPONSE, Actor.CLIENT_APP, Actor.SERVEUR, Act.DEMANDER)) { c ->
        val g = c.g; val th = c.th; val t = c.t; val tg = c.tg
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        g.withPose(Pose(alpha = a, dx = (1f - a) * -60f), 190f, 350f) {
            SK.browser(g, 40f, 200f, 340f, 520f, th, "mon-site.example", 1f)
            g.fillRoundRect(70f, 320f, 300f, 360f, 10f, th.panelHi); g.fillRoundRect(70f, 380f, 260f, 410f, 10f, th.panelHi)
            g.fillRoundRect(70f, 430f, 200f, 480f, 12f, th.accent.withAlpha(0.8f))
        }
        SK.pill(g, "CLIENT", 190f, 570f, th.ink, th.bgTop.withAlpha(0.8f), 24f, a)
        g.withPose(Pose(alpha = a, dx = (1f - a) * 60f), 810f, 350f) { SK.server(g, 810f, 360f, 190f, 250f, th, tg) }
        SK.pill(g, "SERVEUR", 810f, 570f, th.ink, th.bgTop.withAlpha(0.8f), 24f, a)
        val cyc = 6800f
        val u = ((t - 900f) % cyc) / cyc
        if (t > 900f) {
            // 0–0.32 : la requête part ; 0.32–0.52 : le serveur travaille ; 0.52–0.9 : la réponse revient
            val go = clamp01(u / 0.32f); val back = clamp01((u - 0.52f) / 0.38f)
            if (u < 0.34f) {
                val x = lerp(360f, 700f, go)
                SK.envelope(g, x, 330f - sin(go * PI.toFloat()) * 40f, 76f, th.panelHi, th.accent)
                SK.pill(g, reqText(c), x, 262f - sin(go * PI.toFloat()) * 40f, th.bgBottom, th.accent, 22f)
            }
            if (u in 0.32f..0.54f) { val w = (u - 0.32f) / 0.22f; g.strokeArc(810f, 215f, 34f, w * 360f, 250f, 7f, th.good); SK.text(g, "TRAITE", 810f, 150f, 22f, th.good, center = true) }
            if (u in 0.52f..0.92f) {
                val x = lerp(700f, 360f, back)
                SK.packet(g, x, 400f - sin(back * PI.toFloat()) * 30f, 16f, th.good)
                SK.pill(g, "200  { }", x, 340f - sin(back * PI.toFloat()) * 30f, th.bgBottom, th.good, 22f)
            }
        }
        SK.dashed(g, 360f, 400f, 700f, 400f, 3f, th.line, phase = t / 40f)
    }

    private val layers = beat("layers", "Trois couches", setOf(Role.DEFINE, Role.INTRO, Role.MECHANISM), setOf(Goal.VOIR_ARCHITECTURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.CLIENT_APP, Actor.SERVEUR, Actor.BASE_DE_DONNEES)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val names = listOf("FRONTEND", "BACKEND  (API)", "BASE DE DONNEES")
        val cols = listOf(th.accent, th.warn, th.good)
        for (k in 0 until 3) {
            val y = 150f + k * 205f
            val p = SK.ease(t, 150f + k * 450f, 600f, Easing.EXPO_OUT)
            g.withPose(Pose(alpha = p, dy = (1f - p) * 40f), 500f, y) {
                g.fillRoundRect(160f, y - 70f, 840f, y + 70f, 26f, th.panel)
                g.strokeRoundRect(160f, y - 70f, 840f, y + 70f, 26f, 3f, cols[k].withAlpha(0.85f))
                SK.text(g, names[k], 320f, y + 11f, 34f, th.ink)
                when (k) {
                    0 -> { g.fillRoundRect(50f, y - 52f, 130f, y + 52f, 14f, th.line); g.fillRoundRect(58f, y - 40f, 122f, y + 34f, 8f, cols[0].withAlpha(0.6f)) }
                    1 -> SK.server(g, 90f, y, 80f, 100f, th, c.tg)
                    else -> { g.fillRoundRect(55f, y - 38f, 125f, y + 38f, 12f, th.line); g.fillRoundRect(55f, y - 52f, 125f, y - 26f, 13f, cols[2]); g.fillRoundRect(55f, y + 26f, 125f, y + 52f, 13f, cols[2]) }
                }
            }
        }
        // Les données circulent de haut en bas et reviennent.
        for (k in 0 until 2) {
            val x = 880f + k * 40f - 20f
            for (i in 0 until 3) {
                val u = SK.loop(t, 2400f, i * 800f + k * 400f)
                val y = if (k == 0) lerp(150f, 560f, u) else lerp(560f, 150f, u)
                if (t > 1700f) SK.packet(g, x - 10f + k * 40f, y, 8f, if (k == 0) th.accent else th.good, clamp01(u / 0.1f) * (1f - clamp01((u - 0.9f) / 0.1f)))
            }
        }
    }

    private val status = beat("status", "Les codes de statut", setOf(Role.PROCEDURE, Role.MEASURE, Role.MECHANISM, Role.DEFINE), setOf(Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.STATUT, Thing.REPONSE)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val items = listOf(Triple("200", "TOUT VA BIEN", th.good), Triple("404", "RESSOURCE INTROUVABLE", th.warn), Triple("500", "ERREUR DU SERVEUR", th.bad))
        for ((i, it) in items.withIndex()) {
            val cy = 130f + i * 195f
            val p = SK.pop(t, 200f + i * 900f, 600f)
            if (p <= 0f) continue
            g.withPose(Pose(alpha = clamp01(p), dx = (1f - clamp01(p)) * 90f), 500f, cy) {
                g.fillRoundRect(60f, cy - 78f, 940f, cy + 78f, 30f, th.panel)
                g.strokeRoundRect(60f, cy - 78f, 940f, cy + 78f, 30f, 3f, it.third)
                g.fillRoundRect(90f, cy - 54f, 290f, cy + 54f, 22f, it.third)
                SK.text(g, it.first, 190f, cy + 26f, 76f, th.bgBottom, center = true, family = FontFamily.MONO)
                SK.text(g, it.second, 330f, cy + 12f, 34f, th.ink)
                val m = SK.ease(t, 500f + i * 900f, 500f)
                when (i) {
                    0 -> SK.check(g, 880f, cy, 28f * m, it.third)
                    1 -> { g.strokeCircle(880f, cy - 6f, 22f * m, 6f, it.third); SK.text(g, "?", 880f, cy + 12f, 38f, it.third.withAlpha(m), center = true) }
                    else -> SK.cross(g, 880f, cy, 30f * m, it.third)
                }
            }
        }
    }

    private val recap = beat("recap", "À retenir : la demande et le code de retour", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Thing.REQUETE, Thing.STATUT, Act.DEMANDER)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 350f, 420f, th.good, 0.12f)
        val a = SK.ease(t, 100f, 600f, Easing.EXPO_OUT)
        SK.browser(g, 40f, 190f, 330f, 500f, th, "mon-site.example", 1f)
        SK.server(g, 840f, 350f, 170f, 220f, th, c.tg)
        SK.arrow(g, 340f, 330f, 690f, 330f, 7f, th.accent.withAlpha(a), 24f)
        SK.arrow(g, 690f, 400f, 340f, 400f, 7f, th.good.withAlpha(a), 24f)
        val s = SK.pop(t, 900f, 700f).coerceAtMost(1.15f)
        g.save(); g.scale(s, 515f, 365f)
        g.fillRoundRect(405f, 300f, 625f, 430f, 30f, th.good)
        SK.text(g, "200", 515f, 395f, 84f, th.bgBottom, center = true, family = FontFamily.MONO)
        g.restore()
    }

    val kind = SceneKind("clientserver", "Client et serveur", setOf(Concept.CLIENT_SERVER), listOf(request, layers, status, recap), fallbackFor = setOf(Concept.NETFLOW))
}
