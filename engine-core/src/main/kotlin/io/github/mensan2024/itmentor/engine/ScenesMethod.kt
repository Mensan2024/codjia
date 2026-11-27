// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Texte qui rentre dans une largeur : la taille diminue jusqu'à 60 % puis le texte est tronqué (jamais de débordement). */
internal fun SK.fitText(g: Gfx, s: String, cx: Float, base: Float, maxW: Float, size: Float, color: Int, weight: Int = 700) {
    var sz = size
    while (sz > size * 0.6f && g.measureText(s, SK.font(sz, weight)) > maxW) sz -= 1f
    var out = s
    while (out.length > 2 && g.measureText(out, SK.font(sz, weight)) > maxW) out = out.dropLast(1)
    SK.text(g, out, cx, base, sz, color, weight, center = true)
}

private fun StageCtx.steps(minCount: Int = 3): Pair<List<String>, Boolean> {
    val raw = intent.params["steps"] ?: intent.params["videoSteps"]
    val list = raw?.split('|')?.filter { it.isNotBlank() }.orEmpty()
    return if (list.size >= 2) list.take(6) to true else listOf("ETAPE 1", "ETAPE 2", "ETAPE 3", "ETAPE 4").take(max(minCount, 3)) to false
}

/**
 * Méthode ou enchaînement d'étapes (workflow, processus, « méthode en 4 points », « il faut comprendre, analyser, définir… »).
 * Les libellés viennent du texte de l'auteur ([StepLabels]) ; sans énumération, des repères génériques ÉTAPE 1, 2, 3 montrent la
 * relation (l'ordre, la progression) sans rien affirmer de plus.
 */
internal object MethodScenes {
    private val figure = lazy { FigStyle.of(0, rgb(0x4DA3FF)) }

    private val mission = beat("mission", "Une personne gravit la méthode", setOf(Role.INTRO, Role.DEFINE, Role.PROCEDURE, Role.ADVICE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.ETAPES, Act.ENCHAINER, Thing.MISSION), calm = true, minMs = 5500, y0 = 90f, vh = 590f) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val (labels, own) = c.steps()
        val n = labels.size
        SK.caption(g, if (own) "SCHEMA PEDAGOGIQUE" else "SCHEMA PEDAGOGIQUE  -  REPERES GENERIQUES", 72f, 114f, th.accent.withAlpha(0.85f))
        val floor = 650f
        val stepH = min(104f, 420f / n)
        val x0 = 120f
        val w = (880f - x0) / n
        SK.glow(g, 820f, 150f, 360f, th.glow, 0.10f)
        // personne : un segment par marche, 1000 ms de marche puis 450 ms d'arrêt
        val seg = 1450f
        val start = 500f
        val pos = FloatArray(n + 1) { if (it == 0) 92f else x0 + (it - 1) * w + w / 2f }
        val posY = FloatArray(n + 1) { if (it == 0) floor else floor - it * stepH }
        val u = ((t - start) / seg).coerceIn(0f, n.toFloat())
        val s = min(u.toInt(), n - 1)
        val within = u - s
        val move = SK.ease(within * seg, 0f, 1000f, Easing.EASE_IN_OUT)
        val px = lerp(pos[s], pos[s + 1], if (u >= n) 1f else move)
        val py = lerp(posY[s], posY[s + 1], if (u >= n) 1f else move)
        val reached = if (u >= n) n else s + (if (move > 0.98f) 1 else 0)
        for (i in 0 until n) {
            val bx0 = x0 + i * w; val bx1 = bx0 + w - 8f
            val top = floor - (i + 1) * stepH
            val a = SK.pop(t, 150f + i * 160f, 500f)
            if (a <= 0f) continue
            val lit = i < reached
            g.withPose(Pose(alpha = clamp01(a), dy = (1f - clamp01(a)) * 30f), (bx0 + bx1) / 2f, top) {
                g.fillRoundRect(bx0, top, bx1, floor + 20f, 14f, if (lit) mix(th.panel, th.accent, 0.22f) else th.panel)
                g.fillRoundRect(bx0, top, bx1, top + 8f, 4f, if (lit) th.accent else th.line)
                g.fillCircle((bx0 + bx1) / 2f, top + 40f, 21f, if (lit) th.accent else th.panelHi)
                SK.text(g, "${i + 1}", (bx0 + bx1) / 2f, top + 48f, 24f, if (lit) th.bgBottom else th.ink, center = true)
                SK.fitText(g, labels[i], (bx0 + bx1) / 2f, top + 90f, bx1 - bx0 - 12f, 22f, if (lit) th.ink else th.muted)
            }
        }
        // drapeau au sommet
        val topY = floor - n * stepH
        val fx = x0 + (n - 1) * w + w - 40f
        g.line(fx, topY, fx, topY - 120f, 5f, th.ink)
        val wave = sin(t / 260f)
        val fl = PathData().moveTo(fx, topY - 120f).lineTo(fx + 78f, topY - 100f + 6f * wave).lineTo(fx, topY - 70f).close()
        g.fillPath(fl, if (reached >= n) th.good else th.warn)
        // la personne
        val walking = u < n && move > 0.02f && move < 0.98f
        val ph = t / 150f
        val gesture = if (reached >= n) (-170f to -10f) else 8f to 12f
        Fig.draw(g, px, py, 118f, figure.value, 1, walk = if (walking) sin(ph) else 0f, armF = if (walking) (35f * sin(ph + 1.6f) to 20f) else gesture,
            armB = if (walking) (-35f * sin(ph + 1.6f) to 20f) else 8f to 12f, smile = if (reached >= n) 1f else 0.4f)
        if (reached >= n) { val k = SK.pop(t, start + n * seg, 600f); SK.check(g, fx - 20f, topY - 150f, 22f * k, th.good, 1f) }
    }

    private val flow = beat("flow", "Les étapes et leur enchaînement", setOf(Role.INTRO, Role.MECHANISM, Role.DEFINE, Role.PROCEDURE, Role.ADVICE), setOf(Goal.COMPRENDRE_MECANISME, Goal.SUIVRE_PROCEDURE),
        cueSet(Thing.ETAPES, Act.ENCHAINER, Thing.WORKFLOW), calm = true, y0 = 90f, vh = 440f) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val (labels, own) = c.steps()
        val n = labels.size
        SK.caption(g, if (own) "SCHEMA PEDAGOGIQUE" else "SCHEMA PEDAGOGIQUE  -  REPERES GENERIQUES", 72f, 124f, th.accent.withAlpha(0.85f))
        val gap = 36f
        val bw = min(220f, (820f - (n - 1) * gap) / n)
        val bh = 190f
        val total = n * bw + (n - 1) * gap
        val centers = List(n) { i -> ((1000f - total) / 2f + bw / 2f + i * (bw + gap)) to 290f }
        val cycle = 1000f + n * 900f + 1800f
        val tt = if (t < cycle) t else 1000f + n * 900f
        // liens
        for (i in 0 until n - 1) {
            val (ax, ay) = centers[i]; val (bx, by) = centers[i + 1]
            val p = SK.ease(tt, 900f + i * 900f, 500f, Easing.EXPO_OUT)
            SK.arrow(g, ax + bw / 2f + 6f, ay, lerp(ax + bw / 2f + 6f, bx - bw / 2f - 8f, p), by, 5f, th.accent.withAlpha(0.35f + 0.65f * p), 18f)
        }
        val cur = ((tt - 700f) / 900f).toInt().coerceIn(-1, n - 1)
        for (i in 0 until n) {
            val (cx, cy) = centers[i]
            val a = SK.pop(tt, 200f + i * 140f, 500f)
            if (a <= 0f) continue
            val on = i <= cur
            val now = i == cur && tt < 1000f + n * 900f
            if (now) SK.glow(g, cx, cy, 160f, th.accent, 0.30f + 0.1f * sin(t / 200f))
            g.withPose(Pose(alpha = clamp01(a), scale = 0.85f + 0.15f * clamp01(a)), cx, cy) {
                g.fillRoundRect(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f, 22f, if (on) mix(th.panel, th.accent, 0.20f) else th.panel)
                g.strokeRoundRect(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f, 22f, 3f, if (on) th.accent else th.line)
                g.fillCircle(cx - bw / 2f + 6f, cy - bh / 2f + 6f, 21f, if (on) th.accent else th.panelHi)
                SK.text(g, "${i + 1}", cx - bw / 2f + 6f, cy - bh / 2f + 15f, 24f, if (on) th.bgBottom else th.ink, center = true)
                SK.fitText(g, labels[i], cx, cy + 16f, bw - 24f, 30f, th.ink)
            }
        }
        // rail sous les étapes : le jeton parcourt le processus du début au résultat
        val railY = 450f
        val rx0 = centers.first().first; val rx1 = centers.last().first
        g.line(rx0, railY, rx1, railY, 6f, th.line)
        for (i in 0 until n) g.fillCircle(centers[i].first, railY, 11f, if (i <= cur) th.accent else th.panelHi)
        SK.caption(g, "DEBUT", rx0 - 30f, railY + 46f, th.muted)
        SK.caption(g, "RESULTAT", rx1 - 70f, railY + 46f, th.muted)
        val tokP = ((tt - 900f) / (n * 900f)).coerceIn(0f, 1f)
        if (tt > 900f && tokP < 1f) {
            SK.packet(g, lerp(rx0, rx1, tokP), railY, 14f, th.warn)
        }
        if (tt >= 1000f + n * 900f) {
            val k = SK.pop(tt, 1000f + n * 900f, 500f)
            val (lx, ly) = centers.last()
            g.fillCircle(lx + bw / 2f - 8f, ly - bh / 2f - 4f, 24f * k, th.good); SK.check(g, lx + bw / 2f - 8f, ly - bh / 2f - 4f, 14f * k, th.bgBottom)
        }
    }

    private val recap = beat("recap", "À retenir : les étapes cochées", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Thing.ETAPES, Act.ENCHAINER), calm = true, y0 = 40f, vh = 480f) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val (labels, own) = c.steps()
        val n = labels.size
        SK.caption(g, if (own) "SCHEMA PEDAGOGIQUE" else "SCHEMA PEDAGOGIQUE  -  REPERES GENERIQUES", 72f, 64f, th.accent.withAlpha(0.85f))
        val rh = min(84f, 420f / n)
        for (i in 0 until n) {
            val y = 80f + i * (rh + 16f)
            val a = SK.ease(t, 100f + i * 220f, 450f, Easing.EXPO_OUT)
            val done = SK.ease(t, 700f + i * 700f, 350f)
            g.withPose(Pose(alpha = a, dx = (1f - a) * -60f), 500f, y) {
                g.fillRoundRect(110f, y, 890f, y + rh, 20f, th.panel)
                g.fillRoundRect(110f, y, 110f + (890f - 110f) * 0.0f + 12f, y + rh, 6f, th.accent.withAlpha(0.8f))
                g.fillCircle(176f, y + rh / 2f, rh * 0.32f, if (done > 0.5f) th.good else th.panelHi)
                if (done > 0.01f) SK.check(g, 176f, y + rh / 2f, rh * 0.18f * done, th.bgBottom)
                SK.text(g, labels[i], 236f, y + rh / 2f + 10f, min(34f, rh * 0.42f), if (done > 0.5f) th.ink else th.muted)
            }
        }
        val py = 80f + n * (rh + 16f) + 18f
        g.fillRoundRect(110f, py, 890f, py + 22f, 11f, th.panel)
        val p = SK.ease(t, 700f, n * 700f, Easing.EASE_IN_OUT)
        g.fillRoundRect(110f, py, 110f + 780f * p, py + 22f, 11f, th.good)
    }

    val kind = SceneKind("method", "Méthode et workflow", setOf(Concept.METHOD), listOf(mission, flow, recap))
}

/** Accès à l'eau potable : un village, ses habitants, la pompe, puis les effets. Illustrations qualitatives : aucun chiffre. */
internal object WaterScenes {
    private fun hut(g: Gfx, x: Float, ground: Float, w: Float, h: Float, wall: Int, roof: Int) {
        g.fillRoundRect(x - w / 2f, ground - h, x + w / 2f, ground, 6f, wall)
        val r = PathData().moveTo(x - w / 2f - 14f, ground - h + 4f).lineTo(x, ground - h - w * 0.42f).lineTo(x + w / 2f + 14f, ground - h + 4f).close()
        g.fillPath(r, roof)
        g.fillRoundRect(x - w * 0.12f, ground - h * 0.55f, x + w * 0.12f, ground, 5f, mix(wall, rgb(0x000000), 0.45f))
    }

    private fun jerrycan(g: Gfx, x: Float, y: Float, s: Float, color: Int) {
        g.fillRoundRect(x - s * 0.5f, y - s * 0.6f, x + s * 0.5f, y + s * 0.5f, s * 0.12f, color)
        g.fillRoundRect(x - s * 0.18f, y - s * 0.8f, x + s * 0.18f, y - s * 0.55f, s * 0.05f, mix(color, rgb(0x000000), 0.3f))
        g.strokeRoundRect(x - s * 0.5f, y - s * 0.6f, x + s * 0.5f, y + s * 0.5f, s * 0.12f, 2f, mix(color, rgb(0x000000), 0.35f))
    }

    private fun pump(g: Gfx, x: Float, ground: Float, th: StageTheme, t: Float, flowing: Float) {
        g.fillRoundRect(x - 46f, ground - 30f, x + 46f, ground, 6f, th.line)
        g.fillRoundRect(x - 14f, ground - 190f, x + 14f, ground - 30f, 5f, mix(th.line, th.ink, 0.25f))
        g.fillRoundRect(x - 28f, ground - 214f, x + 78f, ground - 186f, 12f, mix(th.line, th.ink, 0.25f))
        val lever = sin(t / 380f) * 8f * flowing
        g.line(x - 20f, ground - 200f, x - 90f, ground - 232f + lever, 9f, th.muted)
        g.fillCircle(x - 90f, ground - 232f + lever, 10f, th.muted)
        if (flowing > 0.05f) {
            val sx = x + 72f; val sy = ground - 186f
            g.line(sx, sy, sx, ground - 56f, 7f * flowing, th.glow.withAlpha(0.85f))
            for (k in 0 until 6) {
                val u = SK.loop(t, 700f, k * 117f)
                g.fillCircle(sx + (SK.hash01(k, 5) - 0.5f) * 14f, sy + u * (ground - 56f - sy), 4f * flowing, th.glow.withAlpha(0.8f * (1f - u * 0.4f)))
            }
        }
    }

    private val village = beat("village", "Un village et sa pompe", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.ADVICE), setOf(Goal.MOTIVER, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.EAU, Actor.COMMUNAUTE, Act.PUISER), calm = true, y0 = 40f, vh = 560f) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "ILLUSTRATION", 72f, 64f, th.accent.withAlpha(0.85f))
        val ground = 560f
        val rise = SK.ease(t, 0f, 2400f, Easing.EXPO_OUT)
        val sunY = lerp(300f, 150f, rise)
        SK.glow(g, 800f, sunY, 420f, th.accent, 0.20f)
        g.fillCircle(800f, sunY, 52f, th.accent)
        SK.rays(g, 800f, sunY, 70f, 96f + 8f * sin(t / 500f), 14, t / 7000f, th.accent.withAlpha(0.5f), 6f)
        g.fillRect(-20f, ground, 1020f, 720f, mix(th.bgBottom, rgb(0xA4673A), 0.78f))
        g.fillRect(-20f, ground, 1020f, ground + 6f, mix(th.bgBottom, rgb(0xD9A066), 0.8f))
        for ((i, hx) in listOf(110f, 250f, 880f).withIndex()) {
            val a = SK.pop(t, 200f + i * 180f, 500f)
            if (a > 0f) g.withPose(Pose(alpha = clamp01(a), dy = (1f - clamp01(a)) * 24f), hx, ground) { hut(g, hx, ground, 120f - i * 8f, 90f + i * 6f, mix(rgb(0xC9A27A), th.bgBottom, 0.15f * i), mix(rgb(0x8A4B2F), th.bgBottom, 0.1f)) }
        }
        val pa = SK.pop(t, 700f, 600f)
        if (pa > 0f) g.withPose(Pose(alpha = clamp01(pa), scale = 0.8f + 0.2f * clamp01(pa)), 560f, ground) { pump(g, 560f, ground, th, t, SK.ease(t, 1800f, 700f)) }
        // seau sous le bec
        jerrycan(g, 632f, ground - 22f, 44f, rgb(0xF2C14E))
        // trois personnes qui viennent chercher de l'eau, chacune avec son jerrican
        for (k in 0 until 3) {
            val st = FigStyle.of(k, listOf(th.accent, rgb(0x5B8DEF), rgb(0xE8B04A))[k])
            val period = 6200f
            val u = SK.loop(t, period, k * 2000f)
            val fromX = 330f - k * 40f
            val atPump = 470f - k * 20f
            val px = if (u < 0.55f) lerp(fromX, atPump, u / 0.55f) else lerp(atPump, fromX, (u - 0.55f) / 0.45f)
            val going = u < 0.55f || u > 0.62f
            val face = if (u < 0.55f) 1 else -1
            val ph = t / 150f + k
            val h = 118f - k * 6f
            val a = SK.ease(t, 600f + k * 300f, 500f)
            g.withPose(Pose(alpha = a), px, ground) {
                Fig.draw(g, px, ground, h, st, face, walk = if (going && u != 0f) sin(ph) else 0f,
                    armF = if (u > 0.55f) (40f to 30f) else (30f * sin(ph + 1.6f) to 20f), armB = -30f * sin(ph + 1.6f) to 20f, smile = if (u > 0.58f) 1f else 0.4f)
                if (u > 0.55f) jerrycan(g, px + face * 30f, ground - 36f, 36f, rgb(0x4DA3FF))
            }
        }
    }

    private val distance = beat("distance", "Avant et après : l'eau près de soi", setOf(Role.CONTRAST, Role.MECHANISM, Role.ADVICE, Role.DEFINE), setOf(Goal.MOTIVER, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.EAU, Actor.COMMUNAUTE, Act.PUISER), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "ILLUSTRATION QUALITATIVE  -  SANS CHIFFRES", 72f, 24f, th.accent.withAlpha(0.85f))
        for (row in 0..1) {
            val y = 90f + row * 300f
            val ground = y + 190f
            val a = SK.ease(t, row * 1500f, 600f)
            if (a <= 0f) continue
            g.withPose(Pose(alpha = a), 500f, y) {
                g.fillRoundRect(40f, y, 960f, y + 250f, 26f, th.panel)
                SK.pill(g, if (row == 0) "AVANT" else "APRES", 130f, y + 30f, if (row == 0) th.bgBottom else th.bgBottom, if (row == 0) th.warn else th.good, 22f)
                g.fillRect(60f, ground, 940f, ground + 4f, th.line)
                hut(g, 140f, ground, 90f, 70f, mix(rgb(0xC9A27A), th.bgBottom, 0.1f), mix(rgb(0x8A4B2F), th.bgBottom, 0.1f))
                val st = FigStyle.of(1, th.accent)
                if (row == 0) {
                    // la source est loin : trajet long, en pointillés
                    SK.dashed(g, 200f, ground - 6f, 790f, ground - 6f, 4f, th.muted, 18f, 14f, phase = t * 0.05f)
                    g.fillRoundRect(800f, ground - 38f, 930f, ground + 2f, 14f, th.glow.withAlpha(0.7f))
                    SK.text(g, "SOURCE", 865f, ground - 46f, 20f, th.muted, center = true)
                    val u = SK.loop(t, 7000f, 0f)
                    val px = if (u < 0.5f) lerp(210f, 780f, u / 0.5f) else lerp(780f, 210f, (u - 0.5f) / 0.5f)
                    val face = if (u < 0.5f) 1 else -1
                    val ph = t / 210f
                    Fig.draw(g, px, ground, 104f, st, face, walk = sin(ph) * 0.8f, armF = 20f * sin(ph + 1.6f) to 20f, armB = -20f * sin(ph + 1.6f) to 20f, smile = -0.3f, lean = 0.02f * face)
                    if (u > 0.5f) jerrycan(g, px + face * 28f, ground - 32f, 34f, rgb(0x4DA3FF))
                } else {
                    // la pompe est tout près
                    pump(g, 360f, ground, th, t, SK.ease(t, 1700f, 500f))
                    SK.dashed(g, 200f, ground - 6f, 300f, ground - 6f, 4f, th.good, 14f, 10f, phase = t * 0.05f)
                    val u = SK.loop(t, 3200f, 0f)
                    val px = if (u < 0.5f) lerp(210f, 290f, u / 0.5f) else lerp(290f, 210f, (u - 0.5f) / 0.5f)
                    val face = if (u < 0.5f) 1 else -1
                    val ph = t / 150f
                    Fig.draw(g, px, ground, 104f, st, face, walk = sin(ph) * 0.8f, armF = 20f * sin(ph + 1.6f) to 20f, armB = -20f * sin(ph + 1.6f) to 20f, smile = 1f)
                    if (u > 0.5f) jerrycan(g, px + face * 28f, ground - 32f, 34f, rgb(0x4DA3FF))
                    SK.text(g, "PLUS PRES", 700f, ground - 80f, 36f, th.good, center = true)
                    SK.heart(g, 700f, ground - 150f, 22f * SK.pop(t, 2400f, 500f), th.good)
                }
            }
        }
    }

    private val effects = beat("effects", "Ce que l'eau change", setOf(Role.MECHANISM, Role.ADVICE, Role.DEFINE, Role.PROCEDURE), setOf(Goal.MOTIVER, Goal.COMPRENDRE_MECANISME),
        cueSet(Thing.EAU, Actor.COMMUNAUTE, Act.PUISER), calm = true, minMs = 5000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "ILLUSTRATION QUALITATIVE  -  SANS CHIFFRES", 72f, 24f, th.accent.withAlpha(0.85f))
        val items = listOf(Triple(IconId.DROP, "EAU PROPRE", th.glow), Triple(IconId.HEART, "SANTE", rgb(0xFF7A8C)), Triple(IconId.BOOK, "ECOLE", th.warn), Triple(IconId.CLOCK, "TEMPS GAGNE", th.good))
        val cx = 500f
        val ys = listOf(110f, 260f, 410f, 560f)
        for (i in items.indices) {
            val a = SK.pop(t, 300f + i * 900f, 600f)
            if (i > 0) {
                val p = SK.ease(t, 100f + i * 900f, 500f)
                SK.arrow(g, cx, ys[i - 1] + 62f, cx, lerp(ys[i - 1] + 62f, ys[i] - 62f, p), 6f, th.accent.withAlpha(0.8f), 20f)
            }
            if (a <= 0f) continue
            val (icon, label, col) = items[i]
            g.withPose(Pose(alpha = clamp01(a), scale = 0.8f + 0.2f * clamp01(a)), cx, ys[i]) {
                g.fillRoundRect(190f, ys[i] - 58f, 810f, ys[i] + 58f, 30f, th.panel)
                g.strokeRoundRect(190f, ys[i] - 58f, 810f, ys[i] + 58f, 30f, 3f, col.withAlpha(0.85f))
                g.fillCircle(280f, ys[i], 40f, col.withAlpha(0.22f))
                Icons.draw(g, icon, 280f, ys[i], 50f, col, 3f)
                SK.text(g, label, 350f, ys[i] + 14f, 40f, th.ink)
            }
        }
    }

    private val recap = beat("recap", "À retenir : l'eau qui monte", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Thing.EAU), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val cx = 500f; val cy = 330f
        val fill = SK.ease(t, 300f, 3200f, Easing.EASE_IN_OUT)
        SK.glow(g, cx, cy, 380f, th.glow, 0.18f)
        val drop = PathData().moveTo(cx, cy - 210f).cubicTo(cx + 40f, cy - 120f, cx + 170f, cy - 40f, cx + 170f, cy + 60f)
            .cubicTo(cx + 170f, cy + 160f, cx + 95f, cy + 230f, cx, cy + 230f).cubicTo(cx - 95f, cy + 230f, cx - 170f, cy + 160f, cx - 170f, cy + 60f)
            .cubicTo(cx - 170f, cy - 40f, cx - 40f, cy - 120f, cx, cy - 210f).close()
        g.fillPath(drop, th.panel)
        g.save(); g.clipRect(cx - 200f, cy + 230f - 440f * fill, cx + 200f, cy + 240f)
        g.fillPath(drop, th.glow.withAlpha(0.85f))
        g.restore()
        g.strokePath(drop, 6f, th.glow)
        val k = SK.pop(t, 3300f, 500f)
        if (k > 0f) { g.fillCircle(cx, cy + 40f, 52f * k, th.good); SK.check(g, cx, cy + 40f, 30f * k, th.bgBottom) }
    }

    val kind = SceneKind("water", "Accès à l'eau potable", setOf(Concept.WATER), listOf(village, distance, effects, recap))
}
