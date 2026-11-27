// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Plans sobres : ils ne dramatisent pas et ne posent aucun diagnostic (voir [Beat.calm]). */

internal object GratitudeScenes {
    /** Écriture manuscrite : une courbe ondulante dont on dévoile la fraction `p`. */
    private fun scribble(g: Gfx, x0: Float, y: Float, len: Float, p: Float, color: Int, seed: Int) {
        if (p <= 0f) return
        val path = PathData().moveTo(x0, y)
        var x = x0
        var i = 0
        while (x < x0 + len) {
            x += 9f; i++
            path.lineTo(x, y + sin(i * 0.9f + seed) * 5f + SK.hash01(i, seed) * 4f)
        }
        g.strokePath(path.trimmed(p), 4.5f, color)
    }

    private val journal = beat("journal", "Le carnet de gratitude", setOf(Role.INTRO, Role.DEFINE), setOf(Goal.APAISER, Goal.MOTIVER),
        cueSet(Thing.JOURNAL, Act.ECRIRE, Act.REMERCIER), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val rise = SK.ease(t, 0f, 2600f, Easing.EXPO_OUT)
        val sx = 780f; val sy = lerp(520f, 190f, rise)
        SK.glow(g, sx, sy, 330f, th.accent, 0.30f * rise + 0.08f)
        g.fillCircle(sx, sy, 76f, th.accent)
        SK.rays(g, sx, sy, 100f, 140f + 14f * sin(t / 400f), 12, t / 5000f, th.accent.withAlpha(0.55f * rise), 7f)
        // La page du carnet
        val a = SK.ease(t, 200f, 700f, Easing.EXPO_OUT)
        g.withPose(Pose(alpha = a, dy = (1f - a) * 40f, scale = 0.96f + 0.04f * a), 330f, 380f) {
            g.save(); g.rotate(-3f, 330f, 380f)
            g.fillRoundRect(60f, 110f, 600f, 640f, 26f, rgb(0xF5EBDD))
            g.fillRect(60f, 110f, 100f, 640f, rgb(0xE5D5BD))
            for (k in 0 until 3) {
                val y = 270f + k * 120f
                g.line(150f, y + 16f, 570f, y + 16f, 2f, rgb(0xC9B79A))
                scribble(g, 150f, y, 380f, SK.ease(t, 900f + k * 1500f, 1300f, Easing.EASE_IN_OUT), rgb(0x3C4A63), k * 5 + 1)
                val h = SK.pop(t, 2100f + k * 1500f, 500f)
                if (h > 0f) SK.heart(g, 560f, y - 4f, 16f * h, rgb(0xFF6B81))
            }
            SK.text(g, "MERCI", 150f, 190f, 46f, rgb(0x3C4A63), 700, family = FontFamily.HAND)
            g.restore()
        }
        for (k in 0 until 5) {
            val u = SK.loop(t, 4000f, k * 800f)
            SK.spark(g, 640f + SK.hash01(k, 4) * 300f, lerp(560f, 240f, u), 9f, th.glow.withAlpha(0.7f * sin(u * PI.toFloat())))
        }
    }

    private val bloom = beat("bloom", "La gratitude fait pousser", setOf(Role.MECHANISM, Role.ADVICE, Role.DEFINE), setOf(Goal.MOTIVER, Goal.APAISER),
        cueSet(Act.REMERCIER), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 330f, 420f, th.accent, 0.13f)
        g.fillCircle(830f, 150f, 56f, th.accent); SK.rays(g, 830f, 150f, 76f, 110f, 10, t / 6000f, th.accent.withAlpha(0.5f), 6f)
        g.fillRoundRect(150f, 580f, 850f, 640f, 30f, rgb(0x5B3A29))
        val grow = SK.ease(t, 400f, 5200f, Easing.EASE_IN_OUT)
        val stem = PathData().moveTo(500f, 590f).cubicTo(480f, 500f, 530f, 420f, 500f, 330f).cubicTo(480f, 270f, 505f, 230f, 500f, 190f)
        g.strokePath(stem.trimmed(grow), 14f, rgb(0x57B86E))
        for (k in 0 until 4) {
            val p = SK.pop(t, 1500f + k * 1000f, 800f)
            if (p <= 0f) continue
            val side = if (k % 2 == 0) -1f else 1f
            val y = 520f - k * 85f
            g.withPose(Pose(alpha = clamp01(p), scale = clamp01(p)), 500f, y) {
                val leaf = PathData().moveTo(500f, y).cubicTo(500f + side * 40f, y - 70f, 500f + side * 130f, y - 70f, 500f + side * 150f, y - 20f)
                    .cubicTo(500f + side * 110f, y + 20f, 500f + side * 40f, y + 20f, 500f, y).close()
                g.fillPath(leaf, rgb(0x6FD08A))
            }
        }
        val fl = SK.pop(t, 5600f, 900f)
        if (fl > 0f) {
            for (k in 0 until 6) {
                val a = k * PI.toFloat() / 3f + t / 2500f
                g.fillCircle(500f + cos(a) * 44f * fl, 170f + sin(a) * 44f * fl, 26f * fl, th.accent)
            }
            g.fillCircle(500f, 170f, 24f * fl, rgb(0xFFF2B8))
        }
        for (k in 0 until 8) {
            val u = SK.loop(t, 5000f, k * 600f)
            SK.spark(g, 300f + SK.hash01(k, 8) * 400f, lerp(560f, 160f, u), 8f, th.glow.withAlpha(0.7f * sin(u * PI.toFloat())))
        }
    }

    private val share = beat("share", "Dire merci à quelqu'un", setOf(Role.ADVICE, Role.PROCEDURE, Role.MECHANISM), setOf(Goal.MOTIVER, Goal.APAISER),
        cueSet(Act.PARTAGER, Act.REMERCIER, Actor.PROCHE), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 180f, 340f, 260f, th.accent, 0.14f)
        val recv = SK.ease(t, 2600f, 1500f)
        SK.glow(g, 820f, 340f, 260f, th.accent, 0.10f + 0.22f * recv)
        val a = SK.pop(t, 100f, 600f)
        g.withPose(Pose(alpha = clamp01(a)), 180f, 340f) { SK.person(g, 180f, 330f, 100f, th.ink) }
        g.withPose(Pose(alpha = clamp01(a)), 820f, 340f) { SK.person(g, 820f, 330f, 100f, mix(th.ink, th.accent, 0.5f * recv)) }
        SK.dashed(g, 270f, 340f, 730f, 340f, 3f, th.accent.withAlpha(0.35f), phase = t / 30f)
        for (k in 0 until 3) {
            val u = SK.loop(t - 700f, 3600f, k * 1200f)
            if (t < 700f) continue
            val x = lerp(280f, 720f, u); val y = 340f - sin(u * PI.toFloat()) * 110f
            SK.heart(g, x, y, 22f, rgb(0xFF6B81).withAlpha(clamp01(u / 0.1f) * (1f - clamp01((u - 0.9f) / 0.1f))))
        }
        val m = SK.pop(t, 1000f, 700f)
        g.withPose(Pose(alpha = clamp01(m), dy = (1f - clamp01(m)) * 20f), 270f, 190f) {
            g.fillRoundRect(80f, 120f, 340f, 200f, 36f, th.panelHi)
            SK.text(g, "MERCI", 210f, 178f, 44f, th.ink, center = true, family = FontFamily.HAND)
        }
        val s = SK.pop(t, 3000f, 700f)
        if (s > 0f) { SK.spark(g, 900f, 200f, 24f * s, th.glow); SK.spark(g, 740f, 170f, 16f * s, th.glow) }
    }


    /** Le soir : une personne écrit dans son carnet, trois moments positifs de sa journée apparaissent un à un. */
    private val evening = beat("evening", "Le soir, trois choses positives dans le carnet", setOf(Role.INTRO, Role.DEFINE, Role.PROCEDURE, Role.ADVICE), setOf(Goal.APAISER, Goal.MOTIVER),
        cueSet(Thing.JOURNAL, Act.ECRIRE, Thing.SOIR), calm = true, minMs = 5500) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "ILLUSTRATION", 72f, 24f, th.accent.withAlpha(0.85f))
        val floor = 640f
        // pièce : mur, fenêtre sur la nuit, lampe
        g.fillRoundRect(40f, 50f, 960f, floor, 28f, th.panel)
        g.fillRoundRect(690f, 100f, 910f, 330f, 16f, rgb(0x0B1630))
        for (k in 0 until 7) g.fillCircle(710f + SK.hash01(k, 11) * 180f, 120f + SK.hash01(k, 12) * 190f, 2.5f, rgb(0xFFFFFF).withAlpha(0.35f + 0.5f * (0.5f + 0.5f * sin(t / 500f + k))))
        g.fillCircle(830f, 170f, 34f, rgb(0xFFF2C2)); g.fillCircle(844f, 160f, 30f, rgb(0x0B1630))
        g.strokeRoundRect(690f, 100f, 910f, 330f, 16f, 6f, th.line)
        g.line(800f, 100f, 800f, 330f, 4f, th.line)
        g.fillRect(40f, floor - 8f, 960f, floor, th.line.withAlpha(0.5f))
        // bureau et lampe
        g.fillRoundRect(330f, 500f, 700f, 520f, 8f, mix(rgb(0x8A5A34), th.bgBottom, 0.2f))
        g.fillRect(360f, 520f, 376f, floor - 8f, mix(rgb(0x8A5A34), th.bgBottom, 0.3f)); g.fillRect(654f, 520f, 670f, floor - 8f, mix(rgb(0x8A5A34), th.bgBottom, 0.3f))
        g.line(620f, 500f, 640f, 400f, 8f, th.muted); g.line(640f, 400f, 560f, 372f, 8f, th.muted)
        SK.glow(g, 545f, 400f, 300f, th.accent, 0.30f)
        g.fillCircle(552f, 376f, 20f, th.accent)
        // carnet ouvert
        g.fillRoundRect(400f, 484f, 540f, 502f, 4f, rgb(0xF5EBDD))
        for (k in 0 until 3) {
            val p = SK.ease(t, 1200f + k * 1700f, 1200f)
            if (p > 0f) g.line(414f, 490f + k * 0f, 414f + 110f * p, 490f, 1.5f, rgb(0x3C4A63))
        }
        // chaise et personne assise qui écrit
        g.fillRoundRect(190f, 520f, 300f, 536f, 6f, th.line)
        g.fillRect(200f, 536f, 212f, floor - 8f, th.line); g.fillRect(278f, 536f, 290f, floor - 8f, th.line)
        val sit = FigStyle.of(1, th.accent)
        val wob = sin(t / 140f)
        val smile = SK.ease(t, 1500f, 2500f)
        Fig.draw(g, 262f, floor - 8f, 250f, sit, 1, armF = (62f + 3f * wob) to (28f + 6f * wob), armB = 50f to 35f, smile = 0.2f + 0.8f * smile, seated = true, lean = 0.015f)
        // trois moments positifs : ils montent du carnet et se rangent au-dessus
        val icons = listOf(IconId.SUN, IconId.HEART, IconId.CHECK)
        val cols = listOf(th.accent, rgb(0xFF7A8C), th.good)
        for (k in 0 until 3) {
            val at = 1600f + k * 1700f
            val u = SK.ease(t, at, 1300f, Easing.EXPO_OUT)
            if (u <= 0f) continue
            val tx = 430f + k * 130f; val ty = 250f
            val x = lerp(470f, tx, u); val y = lerp(470f, ty, u)
            g.withPose(Pose(alpha = clamp01(u * 2f)), x, y) {
                g.fillCircle(x, y, 44f, th.panelHi)
                g.strokeCircle(x, y, 44f, 4f, cols[k])
                Icons.draw(g, icons[k], x, y, 30f, cols[k], 3f)
                g.fillCircle(x - 34f, y - 34f, 15f, cols[k]); SK.text(g, "${k + 1}", x - 34f, y - 27f, 20f, th.bgBottom, center = true)
            }
        }
    }

    private val recap = beat("recap", "À retenir : un regard qui change", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Act.REMERCIER), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val rise = SK.ease(t, 0f, 2200f, Easing.EXPO_OUT)
        val cy = lerp(560f, 330f, rise)
        SK.glow(g, 500f, cy, 420f, th.accent, 0.22f)
        g.fillCircle(500f, cy, 120f, th.accent)
        SK.rays(g, 500f, cy, 150f, 220f + 20f * sin(t / 500f), 16, t / 6000f, th.accent.withAlpha(0.6f), 8f)
        val pulse = 1f + 0.08f * sin(t / 350f)
        SK.heart(g, 500f, cy - 4f, 60f * pulse * SK.pop(t, 1200f, 700f).coerceAtMost(1.1f), rgb(0xFFF2B8))
    }

    val kind = SceneKind("gratitude", "Gratitude", setOf(Concept.GRATITUDE), listOf(journal, evening, bloom, share, recap))
}

internal object StressScenes {
    /** Une icône de geste, dessinée dans un disque centré en (cx, cy) de rayon r. `a` = intensité d'animation 0..1. */
    private fun gestureIcon(g: Gfx, th: StageTheme, motif: String, cx: Float, cy: Float, r: Float, t: Float, col: Int) {
        when (motif) {
            "breath" -> { val k = 0.55f + 0.35f * (0.5f + 0.5f * sin(t / 700f)); g.strokeCircle(cx, cy, r * k, 6f, col); g.strokeCircle(cx, cy, r * k * 0.6f, 4f, col.withAlpha(0.5f)) }
            "name" -> { g.fillRoundRect(cx - r * 0.7f, cy - r * 0.5f, cx + r * 0.7f, cy + r * 0.35f, r * 0.3f, col); g.fillRect(cx - r * 0.3f, cy + r * 0.3f, cx - r * 0.05f, cy + r * 0.6f, col); for (i in 0 until 3) g.fillCircle(cx - r * 0.35f + i * r * 0.35f, cy - r * 0.07f, r * 0.08f, th.bgBottom) }
            "walk" -> { for (i in 0 until 2) { val k = ((t / 500f) + i * 0.5f) % 1f; g.fillCircle(cx - r * 0.3f + i * r * 0.6f, cy + r * 0.5f - k * r, r * 0.18f * (1f - k * 0.5f), col.withAlpha(1f - k * 0.5f)); g.fillCircle(cx - r * 0.3f + i * r * 0.6f, cy + r * 0.78f - k * r, r * 0.1f, col.withAlpha(1f - k * 0.5f)) } }
            "talk" -> { g.fillRoundRect(cx - r * 0.75f, cy - r * 0.6f, cx + r * 0.2f, cy, r * 0.25f, col); g.fillRoundRect(cx - r * 0.2f, cy - r * 0.05f, cx + r * 0.75f, cy + r * 0.55f, r * 0.25f, col.withAlpha(0.6f)) }
            "sleep" -> { g.fillCircle(cx, cy, r * 0.55f, col); g.fillCircle(cx + r * 0.28f, cy - r * 0.18f, r * 0.5f, th.panel) }
            else -> SK.heart(g, cx, cy, r * 0.5f, col)
        }
    }

    private val tangle = beat("tangle", "Du nœud au calme", setOf(Role.INTRO, Role.DEFINE), setOf(Goal.APAISER),
        cueSet(), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val k = SK.ease(t, 1800f, 3600f, Easing.EASE_IN_OUT)
        val col = mix(th.muted, th.accent, k)
        SK.glow(g, 500f, 350f, 400f, th.accent, 0.06f + 0.12f * k)
        val n = 220
        val frame = (t / 85f).toInt()
        val breathe = 1f + 0.05f * sin(t / 900f) * k
        for (layer in 0 until 3) {
            val path = PathData()
            for (i in 0..n) {
                val s = i / n.toFloat()
                val a = 2f * PI.toFloat() * (4f + layer) * s
                val r = 150f + 90f * sin(2f * PI.toFloat() * 3f * s + layer)
                val jx = (SK.hash01(i + layer * 400, frame) - 0.5f) * 22f * (1f - k)
                val jy = (SK.hash01(i + layer * 400 + 9, frame) - 0.5f) * 22f * (1f - k)
                val sx = 500f + r * cos(a) * 1.5f + jx; val sy = 350f + r * sin(a * 1.3f) * 1.1f + jy
                val calmR = (130f + layer * 38f) * breathe
                val ca = 2f * PI.toFloat() * s
                val cxp = 500f + calmR * cos(ca) * 1.25f; val cyp = 350f + calmR * sin(ca)
                val x = lerp(sx, cxp, k); val y = lerp(sy, cyp, k)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            g.strokePath(path, 4.5f - layer * 0.8f, col.withAlpha(0.9f - layer * 0.2f))
        }
    }

    private val support = beat("support", "Tu n'es pas seul", setOf(Role.DEFINE, Role.INTRO, Role.RECOGNIZE), setOf(Goal.APAISER),
        cueSet(Actor.PROCHE, Act.PARTAGER), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 350f, 400f, th.accent, 0.14f)
        val cx = 500f; val cy = 350f
        val n = 9
        for (i in 0 until n) {
            val ang = i * 2f * PI.toFloat() / n - PI.toFloat() / 2f
            val rr = 235f + 14f * sin(t / 900f + i)
            val x = cx + cos(ang) * rr * 1.5f; val y = cy + sin(ang) * rr * 0.95f
            val p = SK.pop(t, 500f + i * 260f, 600f)
            if (p <= 0f) continue
            g.line(cx, cy, x, y, 2.5f, th.accent.withAlpha(0.28f * clamp01(p) * (0.7f + 0.3f * sin(t / 500f + i))))
            g.withPose(Pose(alpha = clamp01(p), scale = 0.6f + 0.4f * clamp01(p)), x, y) { SK.person(g, x, y, 34f, mix(th.muted, th.ink, 0.6f)) }
        }
        g.fillCircle(cx, cy, 64f, th.accent.withAlpha(0.25f))
        SK.person(g, cx, cy, 52f, th.ink)
        g.strokeCircle(cx, cy, 78f + 8f * sin(t / 500f), 3f, th.accent.withAlpha(0.6f))
    }

    private val breath = beat("breath", "Respirer lentement", setOf(Role.ADVICE, Role.PROCEDURE), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.RESPIRER, Thing.RESPIRATION), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        // Inspire 4 s, expire 6 s : le cycle de la phrase de l'auteur si elle donne ces durées, sinon 4 et 6 par défaut.
        val inh = 4000f; val exh = 6000f
        val m = t % (inh + exh)
        val inhale = m < inh
        val k = if (inhale) SK.ease(m, 0f, inh, Easing.EASE_IN_OUT) else 1f - SK.ease(m - inh, 0f, exh, Easing.EASE_IN_OUT)
        SK.glow(g, 500f, 340f, 420f, th.accent, 0.10f + 0.14f * k)
        for (i in 3 downTo 0) g.strokeCircle(500f, 340f, (120f + 120f * k) + i * 34f, 3f, th.accent.withAlpha(0.14f + 0.10f * (3 - i) / 3f))
        g.fillCircle(500f, 340f, 120f + 120f * k, mix(th.accent, th.bgBottom, 0.5f).withAlpha(0.65f))
        g.strokeCircle(500f, 340f, 120f + 120f * k, 6f, th.accent)
        SK.text(g, if (inhale) "INSPIRE" else "EXPIRE", 500f, 352f, 44f, th.ink, center = true)
        val remaining = if (inhale) ((inh - m) / 1000f).toInt() + 1 else ((inh + exh - m) / 1000f).toInt() + 1
        SK.text(g, remaining.toString(), 500f, 640f, 64f, th.accent, center = true, family = FontFamily.MONO)
    }

    private val walk = beat("walk", "Marcher pour détendre le corps", setOf(Role.ADVICE), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.MARCHER, Thing.MARCHE), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        g.fillCircle(780f, 170f, 70f, th.accent.withAlpha(0.9f)); SK.glow(g, 780f, 170f, 300f, th.accent, 0.20f)
        val hills = PathData().moveTo(0f, 420f).cubicTo(200f, 340f, 380f, 380f, 560f, 400f).cubicTo(740f, 420f, 860f, 340f, 1000f, 380f).lineTo(1000f, 700f).lineTo(0f, 700f).close()
        g.fillPath(hills, mix(th.bgBottom, th.accent, 0.12f))
        val road = PathData().moveTo(250f, 700f).cubicTo(400f, 560f, 700f, 520f, 520f, 400f)
        g.strokePath(road, 54f, mix(th.bgBottom, th.ink, 0.22f))
        val u = SK.loop(t, 9000f)
        val pts = road.flatten(24).first()
        val idx = (u * (pts.size - 1)).toInt().coerceIn(0, pts.size - 1)
        val (x, y) = pts[idx]
        val sc = 0.35f + 0.65f * ((y - 400f) / 300f)
        val swing = sin(t / 190f) * 0.5f
        val bodyH = 120f * sc
        g.line(x, y - bodyH, x, y - bodyH * 0.35f, 9f * sc, th.ink)
        g.fillCircle(x, y - bodyH - 16f * sc, 17f * sc, th.ink)
        g.line(x, y - bodyH * 0.35f, x + swing * 34f * sc, y, 8f * sc, th.ink)
        g.line(x, y - bodyH * 0.35f, x - swing * 34f * sc, y, 8f * sc, th.ink)
        g.line(x, y - bodyH * 0.9f, x - swing * 30f * sc, y - bodyH * 0.5f, 7f * sc, th.ink)
        g.line(x, y - bodyH * 0.9f, x + swing * 30f * sc, y - bodyH * 0.5f, 7f * sc, th.ink)
    }

    private val talk = beat("talk", "En parler à quelqu'un de confiance", setOf(Role.ADVICE), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.PARTAGER, Actor.PROCHE), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 350f, 400f, th.accent, 0.12f)
        SK.person(g, 200f, 400f, 105f, mix(th.muted, th.ink, 0.5f)); SK.person(g, 800f, 400f, 105f, th.ink)
        val phase = (t / 2200f).toInt() % 2
        val u = (t % 2200f) / 2200f
        val bx = if (phase == 0) 240f else 560f; val by = 150f
        val pop = SK.pop(t % 2200f, 0f, 500f)
        g.withPose(Pose(alpha = clamp01(pop), scale = 0.8f + 0.2f * clamp01(pop)), bx + 100f, by) {
            g.fillRoundRect(bx, by - 60f, bx + 200f, by + 50f, 30f, th.panelHi)
            for (i in 0 until 3) g.fillCircle(bx + 55f + i * 45f, by - 5f, 9f + 3f * sin(t / 160f + i), th.ink)
        }
        // Le poids se partage : le disque gris à gauche se divise et glisse vers l'autre personne.
        val share = SK.ease(t, 1500f, 4200f)
        g.fillCircle(200f, 215f, 40f * (1f - 0.5f * share), th.muted.withAlpha(0.8f))
        g.fillCircle(800f, 215f, 40f * 0.5f * share, th.muted.withAlpha(0.8f * share))
        SK.heart(g, 500f, 440f - 6f * sin(t / 400f), 28f, rgb(0xFF6B81).withAlpha(0.5f + 0.5f * share))
    }

    private val gestures = beat("gestures", "Chaque geste, son image", setOf(Role.ADVICE, Role.PROCEDURE), setOf(Goal.APAISER, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.RESPIRER, Act.MARCHER, Act.PARTAGER, Act.DORMIR), calm = true, minMs = 5000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val motifs = c.intent.itemMotifs.ifEmpty { listOf("breath", "name", "walk", "talk", "sleep") }
        val n = motifs.size.coerceIn(1, 5)
        val words = mapOf("breath" to "RESPIRER", "name" to "NOMMER", "walk" to "BOUGER", "talk" to "PARLER", "sleep" to "DORMIR")
        val step = 880f / n
        g.line(60f + step / 2f, 340f, 940f - step / 2f, 340f, 4f, th.line)
        for (i in 0 until n) {
            val cx = 60f + step * (i + 0.5f); val cy = 340f
            val at = 700f + i * 600f
            val p = SK.pop(t, at, 600f)
            val on = clamp01(p)
            val r = minOf(step * 0.42f, 95f)
            g.withPose(Pose(alpha = 0.35f + 0.65f * on, scale = 0.8f + 0.2f * on), cx, cy) {
                g.fillCircle(cx, cy, r, th.panel)
                g.strokeCircle(cx, cy, r, 4f, if (on > 0.5f) th.accent else th.line)
                gestureIcon(g, th, motifs[i], cx, cy, r, t, if (on > 0.5f) th.accent else th.muted)
            }
            SK.pill(g, words[motifs[i]] ?: "GESTE", cx, cy + r + 52f, th.ink, th.bgTop.withAlpha(0.85f), 18f, on)
            SK.text(g, (i + 1).toString(), cx, cy - r - 24f, 30f, th.accent.withAlpha(on), center = true, family = FontFamily.MONO)
        }
    }

    private val recap = beat("recap", "À retenir : un geste à la fois", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(), calm = true) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val k = 0.5f + 0.5f * sin(t / 1100f)
        SK.glow(g, 500f, 340f, 440f, th.accent, 0.12f + 0.08f * k)
        for (i in 0 until 3) g.strokeCircle(500f, 340f, 130f + i * 55f + 18f * k, 3f, th.accent.withAlpha(0.35f - i * 0.09f))
        g.fillCircle(500f, 340f, 112f + 14f * k, th.accent.withAlpha(0.28f))
        SK.heart(g, 500f, 340f, 64f + 6f * k, th.accent)
    }

    val kind = SceneKind("stress", "Stress et apaisement", setOf(Concept.STRESS), listOf(tangle, support, breath, walk, talk, gestures, recap))
}

internal object FunnelScenes {
    private fun funnelPath(top: Float, bottom: Float, wTop: Float, wBottom: Float, cx: Float = 500f) =
        PathData().moveTo(cx - wTop / 2f, top).lineTo(cx + wTop / 2f, top).lineTo(cx + wBottom / 2f, bottom).lineTo(cx - wBottom / 2f, bottom).close()

    private val funnel = beat("funnel", "Le tunnel", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM), setOf(Goal.CONVAINCRE, Goal.COMPRENDRE_MECANISME),
        cueSet(Act.ATTIRER, Act.CONVERTIR, Actor.PROSPECT)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "SCHEMA PEDAGOGIQUE", 72f, 24f, th.accent.withAlpha(0.85f))
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        val path = funnelPath(150f, 560f, 760f, 120f)
        g.fillPath(path, th.panel.withAlpha(a))
        g.strokePath(path, 4f, th.accent.withAlpha(0.8f * a))
        for (i in 1..2) { val y = 150f + i * 137f; val w = lerp(760f, 120f, i / 3f); g.line(500f - w / 2f, y, 500f + w / 2f, y, 3f, th.line.withAlpha(a)) }
        val cols = listOf(th.accent, th.warn, th.glow, th.good)
        // Beaucoup de visiteurs entrent ; à chaque étage une partie sort sur le côté ; quelques-uns deviennent clients.
        for (k in 0 until 26) {
            val period = 5200f
            val u = SK.loop(t - 500f, period, k * period / 26f)
            if (t < 500f) continue
            val survive = SK.hash01(k, 21) < 0.2f
            val drop = SK.hash01(k, 22)
            val stop = if (survive) 1f else if (drop < 0.5f) 0.34f else if (drop < 0.8f) 0.67f else 0.9f
            val yy = 120f + clamp01(u / stop) * (if (survive) 530f else 530f * stop)
            val fy = clamp01((yy - 150f) / 410f)
            val w = lerp(760f, 120f, fy)
            val x = 500f + (SK.hash01(k, 23) - 0.5f) * (w - 40f) * (if (survive) 0.4f else 1f)
            var px = x; var alpha = clamp01(u / 0.05f)
            if (!survive && u >= stop) {
                val e = clamp01((u - stop) / 0.12f)
                px = x + (if (SK.hash01(k, 24) > 0.5f) 1f else -1f) * e * 200f; alpha *= 1f - e
            }
            val col = if (survive && fy > 0.7f) th.good else cols[k % 3]
            g.fillCircle(px, yy, 10f, col.withAlpha(alpha))
        }
        SK.pill(g, "AUDIENCE", 500f, 100f, th.ink, th.bgTop.withAlpha(0.85f), 22f, a)
        SK.pill(g, "CLIENTS", 500f, 620f, th.bgBottom, th.good, 24f, a)
    }

    private val stages = beat("stages", "Les étapes du tunnel", setOf(Role.PROCEDURE, Role.ADVICE), setOf(Goal.SUIVRE_PROCEDURE, Goal.CONVAINCRE),
        cueSet(Act.ATTIRER, Act.CONVERTIR, Act.FIDELISER), minMs = 5000) { c ->
        val g = c.g; val th = c.th; val t = c.t
        val names = mapOf("attract" to "ATTIRER", "capture" to "CAPTER", "convert" to "CONVERTIR", "retain" to "FIDELISER")
        val motifs = c.intent.itemMotifs.ifEmpty { listOf("attract", "capture", "convert", "retain") }
        val n = motifs.size.coerceIn(1, 5)
        val cols = listOf(th.accent, th.glow, th.warn, th.good, th.accent)
        val rowH = 590f / n
        for (i in 0 until n) {
            val at = 700f + i * 600f
            val p = SK.ease(t, at, 600f, Easing.EXPO_OUT)
            val w = lerp(900f, 360f, i / maxOf(1f, n - 1f))
            val y = 40f + i * rowH + rowH / 2f
            g.withPose(Pose(alpha = p, dx = (1f - p) * -80f), 500f, y) {
                g.fillRoundRect(500f - w / 2f, y - rowH * 0.42f, 500f + w / 2f, y + rowH * 0.42f, 26f, th.panel)
                g.strokeRoundRect(500f - w / 2f, y - rowH * 0.42f, 500f + w / 2f, y + rowH * 0.42f, 26f, 3f, cols[i].withAlpha(0.9f))
                val ix = 500f - w / 2f + 70f
                g.fillCircle(ix, y, rowH * 0.3f, cols[i].withAlpha(0.25f))
                when (motifs[i]) {
                    "attract" -> Icons.draw(g, IconId.MEGAPHONE, ix, y, rowH * 0.42f, cols[i], 2.2f)
                    "capture" -> SK.envelope(g, ix, y, rowH * 0.5f, th.panelHi, cols[i])
                    "convert" -> { Icons.draw(g, IconId.TARGET, ix, y, rowH * 0.42f, cols[i], 2.2f) }
                    "retain" -> Icons.draw(g, IconId.HEART, ix, y, rowH * 0.42f, cols[i], 2.2f)
                    else -> Icons.draw(g, IconId.CHART, ix, y, rowH * 0.42f, cols[i], 2.2f)
                }
                SK.text(g, names[motifs[i]] ?: "ETAPE ${i + 1}", ix + rowH * 0.5f, y + 11f, minOf(36f, rowH * 0.3f), th.ink)
            }
        }
    }

    private val leak = beat("leak", "Mesurer étape par étape", setOf(Role.MEASURE, Role.ADVICE, Role.DEFINE), setOf(Goal.SUIVRE_PROCEDURE, Goal.COMPRENDRE_MECANISME),
        cueSet(Act.MESURER, Act.CONVERTIR)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.caption(g, "ILLUSTRATION QUALITATIVE", 72f, 24f, th.accent.withAlpha(0.85f))
        val widths = listOf(1f, 0.62f, 0.18f, 0.13f)
        val names = listOf("ATTIRER", "CAPTER", "CONVERTIR", "FIDELISER")
        for (i in widths.indices) {
            val y = 70f + i * 150f
            val p = SK.ease(t, 200f + i * 500f, 800f, Easing.EXPO_OUT)
            val w = 700f * widths[i] * p
            g.fillRoundRect(240f, y, 240f + 700f, y + 104f, 24f, th.panel)
            g.fillRoundRect(240f, y, 240f + w, y + 104f, 24f, (if (i == 2) th.bad else th.accent).withAlpha(0.85f))
            SK.text(g, names[i], 30f, y + 66f, 26f, th.ink)
        }
        // La loupe se pose là où la perte est la plus forte.
        val u = SK.ease(t, 3200f, 1600f, Easing.EASE_IN_OUT)
        val mx = lerp(150f, 560f, u); val my = lerp(120f, 220f + 150f, u)
        if (t > 3200f) {
            g.strokeCircle(mx, my, 54f, 9f, th.warn); g.line(mx + 38f, my + 38f, mx + 84f, my + 84f, 12f, th.warn)
            SK.pill(g, "PERTE", 760f, 220f + 150f - 66f, rgb(0xFFFFFF), th.bad, 24f, SK.ease(t, 4600f, 400f))
        }
    }

    private val loop = beat("loop", "Faire revenir le client", setOf(Role.ADVICE, Role.DEFINE, Role.RECAP), setOf(Goal.CONVAINCRE, Goal.APPRENDRE_REFLEXE),
        cueSet(Act.FIDELISER, Act.CONVERTIR)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 350f, 400f, th.good, 0.12f)
        val a = SK.ease(t, 100f, 600f)
        g.strokeCircle(500f, 350f, 210f, 10f, th.accent.withAlpha(0.35f * a))
        val ang = t / 1800f
        for (k in 0 until 3) {
            val aa = ang + k * 2f * PI.toFloat() / 3f
            val x = 500f + cos(aa) * 210f; val y = 350f + sin(aa) * 210f
            g.fillCircle(x, y, 40f, th.accent.withAlpha(0.2f))
            SK.person(g, x, y + 6f, 26f, th.ink)
            if (k == 0) g.fillCircle(x + 34f, y - 38f, 16f, th.good)
        }
        Icons.draw(g, IconId.HEART, 500f, 350f, 120f * (1f + 0.06f * sin(t / 300f)), th.good, 2.5f)
        SK.pill(g, "ACHAT", 500f, 120f, th.bgBottom, th.good, 22f, a)
    }

    private val recap = beat("recap", "À retenir", setOf(Role.RECAP), setOf(Goal.MEMORISER),
        cueSet(Act.CONVERTIR, Act.ATTIRER)) { c ->
        val g = c.g; val th = c.th; val t = c.t
        SK.glow(g, 500f, 350f, 420f, th.good, 0.14f)
        val a = SK.ease(t, 100f, 700f, Easing.EXPO_OUT)
        val path = funnelPath(130f, 520f, 640f, 130f)
        g.fillPath(path, th.panel.withAlpha(a)); g.strokePath(path, 4f, th.accent.withAlpha(a))
        val s = SK.pop(t, 900f, 700f).coerceAtMost(1.15f)
        g.save(); g.scale(s, 500f, 600f)
        g.fillCircle(500f, 600f, 64f, th.good); SK.check(g, 500f, 600f, 36f, th.bgBottom)
        g.restore()
        for (k in 0 until 5) {
            val u = SK.loop(t, 3000f, k * 600f)
            SK.spark(g, 330f + SK.hash01(k, 31) * 340f, lerp(120f, 520f, u), 9f, th.glow.withAlpha(0.8f * sin(u * PI.toFloat())))
        }
    }

    val kind = SceneKind("funnel", "Tunnel de vente", setOf(Concept.FUNNEL), listOf(funnel, stages, leak, loop, recap))
}

/** Plans 3D déjà existants (moteur logiciel) : ils sont dessinés par [Demo3DLook] lui-même, d'où `draw = null`. */
internal object Attack3DScenes {
    private val IT = setOf(Domain.CYBER, Domain.RESEAUX, Domain.SYSTEMES, Domain.FULLSTACK, Domain.IA)

    private val attack = beat("attack", "Simulation 3D d'une attaque", setOf(Role.INTRO, Role.DEFINE, Role.MECHANISM, Role.RECOGNIZE), setOf(Goal.IDENTIFIER_MENACE, Goal.COMPRENDRE_MECANISME),
        cueSet(Actor.ATTAQUANT), needs = cueSet(Thing.ATTAQUE, Thing.OBJET_3D), draw = null)
    private val defense = beat("defense", "Simulation 3D de la parade", setOf(Role.ADVICE, Role.RECAP, Role.PROCEDURE), setOf(Goal.APPRENDRE_REFLEXE, Goal.MEMORISER),
        cueSet(Act.BLOQUER, Act.LIMITER, Act.VERIFIER, Act.ACTIVER_MFA, Act.CHANGER), needs = cueSet(Thing.ATTAQUE, Thing.OBJET_3D), draw = null)
    private val obj = beat("object", "Objet en volume", Role.entries.toSet(), Goal.entries.toSet(), cueSet(Thing.OBJET_3D), repeatable = true, needs = cueSet(Thing.OBJET_3D), draw = null)

    val attackKind = SceneKind("attack3d", "Attaque et défense 3D", setOf(Concept.MALWARE), listOf(attack, defense),
        fallbackFor = setOf(Concept.PHISHING, Concept.BRUTE_FORCE, Concept.FIREWALL), domains = IT)
    val objectKind = SceneKind("object3d", "Objet 3D", setOf(Concept.NONE), listOf(obj), domains = IT)
}
