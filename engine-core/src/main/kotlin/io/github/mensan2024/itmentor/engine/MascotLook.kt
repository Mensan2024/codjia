// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Format « Personnage animé » : Codi, petit robot original de CODJIA, présente l'astuce.
 * Il cligne des yeux, regarde le contenu, parle (bouche synchronisée sur le texte qui s'écrit),
 * pointe du bras, s'inquiète devant la mauvaise pratique et se réjouit devant la bonne.
 */
internal class MascotLook(private val sb: Storyboard, private val template: Template) : LookRenderer {
    private val st = template.style
    private val r = Rand.of(sb.seed, 0x3A5C)
    private val bodies = listOf(rgb(0xFFB703), rgb(0x4CC9F0), rgb(0x80ED99), rgb(0xFF7A59), rgb(0xB794F6))
    private val body = bodies[(r.nextInt() ushr 1) % bodies.size]
    override val ink = st.text
    override val muted = st.textMuted
    override val accent = st.accent
    override val accent2 = st.accent2
    private val paper = rgb(0xFFFFFF)
    private val paperInk = rgb(0x1B1F27)
    private val paperMuted = rgb(0x5B6472)
    private val ok = rgb(0x16A34A)
    private val danger = rgb(0xDC2626)
    private val cache = TextCache()
    private val fTitle = FontSpec(FontFamily.ROUNDED, 700, 88f)
    private val fText = FontSpec(FontFamily.TEXT, 600, 44f)
    private val fSmall = FontSpec(FontFamily.TEXT, 500, 36f)

    enum class Mood { NEUTRAL, HAPPY, WORRIED }

    /** Pose de Codi à un instant donné. */
    private data class Pose2(
        val x: Float, val y: Float, val scale: Float, val mood: Mood, val talk: Float,
        val lookX: Float, val lookY: Float, val armAngle: Float, val wave: Boolean, val hop: Float,
    )

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        val t = localMs
        background(g, slot.index, tGlobalMs)
        val left = slot.index % 2 == 0
        val many = sc.items.size >= 4 || sc.layout == Layout.COMPARE
        val scale = when {
            sc.items.size >= 4 -> 0.62f
            many -> 0.74f
            else -> 0.92f
        }
        val cx = if (left) 270f else 810f
        val cy = 1555f - 235f * scale
        var talk = 0f
        var mood = Mood.NEUTRAL
        var wave = false
        var hop = 0f
        val bottom = cy - 250f * scale
        when (sc.layout) {
            Layout.HERO -> { wave = true; mood = Mood.HAPPY; talk = hero(g, sc, t, bottom, cx) }
            Layout.DEFINITION -> talk = definition(g, sc, t, bottom, cx)
            Layout.STEPS -> talk = steps(g, sc, t, bottom, cx)
            Layout.STAT -> { mood = Mood.HAPPY; hop = abs(sin(window(t, 900f, 900f) * Math.PI.toFloat() * 2f)); talk = stat(g, sc, t, bottom, cx) }
            Layout.COMPARE -> { val (tk, m) = compare(g, sc, t, bottom, cx); talk = tk; mood = m }
            Layout.CHECKLIST -> { mood = Mood.HAPPY; wave = t > sc.durationMs * 0.65f; talk = checklist(g, sc, t, bottom, cx) }
            Layout.SCREEN -> {}
        }
        // Codi regarde vers le haut du contenu et pointe dans sa direction.
        val lookX = if (left) 0.6f else -0.6f
        val arm = if (left) -40f else 220f
        val enter = Easing.BACK_OUT.ease(window(t, 0f, 700f))
        val y = cy + (1 - enter) * 500f - hop * 60f
        codi(g, Pose2(cx, y, scale, mood, talk, lookX, -0.5f, arm, wave, hop), t, tGlobalMs)
    }

    private fun background(g: Gfx, index: Int, t: Float) {
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, st.bgTop, st.bgBottom)
        // Confettis géométriques qui flottent doucement
        val rr = Rand(0x6E11 + index)
        repeat(16) {
            val x = rr.range(40f, 1040f); val y0 = rr.range(160f, 1700f); val s = rr.range(10f, 24f)
            val y = y0 + sin(t * 0.0012f + x) * 18f
            val c = (if (it % 2 == 0) st.accent else st.accent2).withAlpha(0.22f)
            when (it % 3) {
                0 -> g.fillCircle(x, y, s / 2, c)
                1 -> g.fillRoundRect(x, y, x + s, y + s, 4f, c)
                else -> g.strokeCircle(x, y, s / 2, 3f, c)
            }
        }
        g.fillRadialGlow(540f, 1560f, 700f, st.accent.withAlpha(0.10f))
    }

    // ───────────────────────────── Bulle et textes ─────────────────────────────

    /** Bulle blanche avec une pointe vers Codi. */
    private fun bubble(g: Gfx, l: Float, top: Float, rr: Float, b: Float, tailX: Float, a: Float) {
        if (a <= 0f) return
        g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.85f, 1f, Easing.BACK_OUT.ease(a))), tailX, b) {
            g.fillRoundRect(l + 8f, top + 14f, rr + 8f, b + 14f, 44f, 0x33000000)
            g.fillRoundRect(l, top, rr, b, 44f, paper)
            val tx = tailX.coerceIn(l + 90f, rr - 90f)
            g.fillPath(PathData().moveTo(tx - 40f, b - 2f).lineTo(tx + 40f, b - 2f).lineTo(tx + (if (tailX < 540f) -30f else 30f), b + 70f).close(), paper)
        }
    }

    private fun title(g: Gfx, sc: Scene, t: Float, y0: Float, size: Float = 88f): Float {
        var y = y0
        sc.kicker?.let { k ->
            val f = FontSpec(FontFamily.TEXT, 800, 30f)
            val w = g.measureTracked(k.uppercase(), f, 2f) + 44f
            val a = Easing.BACK_OUT.ease(window(t, 100f, 500f))
            g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.7f, 1f, a)), Safe.LEFT + w / 2, y + 28f) {
                g.fillRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + 56f, 28f, st.accent)
                g.drawTracked(k.uppercase(), Safe.LEFT + 22f, y + 28f + g.metrics(f).ascent / 2, f, st.accentInk, 2f)
            }
            y += 84f
        }
        val marked = Marked.parse(sc.title)
        val b = cache.fit(g, "${sc.id}/t", marked.plain, fTitle.withSize(size), Safe.WIDTH, 3, 1.08f)
        val runs = MarkedText.runs(b, marked)
        for (i in b.lines.indices) {
            val a = Easing.BACK_OUT.ease(window(t, 200f + i * 120f, 600f))
            val base = g.baselineOf(b, y, i)
            g.withPose(Pose(alpha = clamp01(a), dy = (1 - a) * 50f), Safe.LEFT, base) {
                MarkedText.drawLine(g, runs[i], Safe.LEFT, base, b.font, st.text, st.emphasis)
            }
        }
        return y + b.height
    }

    /** Texte qui s'écrit dans la bulle ; renvoie (bas, parle-t-il ?). */
    private fun spoken(g: Gfx, key: String, text: String, font: FontSpec, x: Float, top: Float, w: Float, lines: Int, t: Float, start: Float, color: Int = paperInk): Pair<Float, Float> {
        val b = cache.fit(g, key, text, font, w, lines, 1.28f)
        val dur = (b.charCount * 30f).coerceIn(400f, 3600f)
        val p = window(t, start, dur)
        if (p > 0f) g.drawBlock(b, x, top, color, reveal = p)
        return top + b.height to (if (p > 0f && p < 1f) 1f else 0f)
    }

    private fun hero(g: Gfx, sc: Scene, t: Float, limit: Float, cx: Float): Float {
        var y = title(g, sc, t, 220f, 96f) + 60f
        val sub = Marked.strip(sc.subtitle ?: sc.body)
        if (sub.isEmpty()) return 0f
        val b = cache.fit(g, "${sc.id}/s", sub, fText, Safe.WIDTH - 100f, 5, 1.28f)
        val bottom = (y + b.height + 90f).coerceAtMost(limit - 80f)
        bubble(g, Safe.LEFT, y, Safe.RIGHT, bottom, cx, window(t, 700f, 400f))
        return spoken(g, "${sc.id}/s", sub, fText, Safe.LEFT + 50f, y + 45f, Safe.WIDTH - 100f, 5, t, 1000f).second
    }

    private fun definition(g: Gfx, sc: Scene, t: Float, limit: Float, cx: Float): Float {
        val y = title(g, sc, t, 220f, 80f) + 50f
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        val b = cache.fit(g, "${sc.id}/b", body, fText, Safe.WIDTH - 100f, 8, 1.28f)
        val bottom = (y + b.height + 90f).coerceAtMost(limit - 80f)
        bubble(g, Safe.LEFT, y, Safe.RIGHT, bottom, cx, window(t, 500f, 400f))
        return spoken(g, "${sc.id}/b", body, fText, Safe.LEFT + 50f, y + 45f, Safe.WIDTH - 100f, 8, t, 800f).second
    }

    private fun steps(g: Gfx, sc: Scene, t: Float, limit: Float, cx: Float): Float {
        val y = title(g, sc, t, 200f, 74f) + 40f
        val n = sc.items.size.coerceAtLeast(1)
        val avail = limit - 80f - y - 60f
        val row = (avail / n).coerceIn(90f, 190f)
        val lines = if (row >= 108f) 2 else 1
        val bottom = y + 60f + row * n
        bubble(g, Safe.LEFT, y, Safe.RIGHT, bottom, cx, window(t, 400f, 400f))
        val step = ((sc.durationMs * 0.78f - 800f) / n).coerceAtLeast(400f)
        var talking = 0f
        for ((i, item) in sc.items.withIndex()) {
            val top = y + 30f + i * row
            val start = 800f + i * step
            val a = Easing.BACK_OUT.ease(window(t, start, 450f))
            if (a <= 0f) continue
            val ncx = Safe.LEFT + 74f
            g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.5f, 1f, a)), ncx, top + row / 2) {
                g.fillCircle(ncx, top + row / 2, 32f, st.accent)
                val nf = FontSpec(FontFamily.ROUNDED, 700, 36f)
                val s = "${i + 1}"
                g.drawText(s, ncx - g.measureText(s, nf) / 2, top + row / 2 + g.metrics(nf).ascent / 2, nf, st.accentInk)
            }
            val (label, detail) = splitLabel(Marked.strip(item))
            val text = if (detail != null && lines >= 2) "$label : $detail" else label
            val b = cache.fit(g, "${sc.id}/i$i", text, fSmall.copy(weight = 600), Safe.WIDTH - 190f, lines, 1.22f)
            val p = window(t, start + 150f, (b.charCount * 22f).coerceIn(300f, step * 0.8f))
            g.drawBlock(b, Safe.LEFT + 130f, top + (row - b.height) / 2, paperInk, reveal = p)
            if (p > 0f && p < 1f) talking = 1f
        }
        return talking
    }

    private fun stat(g: Gfx, sc: Scene, t: Float, limit: Float, cx: Float): Float {
        var y = title(g, sc, t, 220f, 70f) + 50f
        val (num, unit) = statParts(sc.statValue ?: "")
        val bottom = (y + 520f).coerceAtMost(limit - 80f)
        bubble(g, Safe.LEFT, y, Safe.RIGHT, bottom, cx, window(t, 400f, 400f))
        val cy = (y + bottom) / 2 - 20f
        val f = fitSize(g, num, FontSpec(FontFamily.ROUNDED, 700, 230f), Safe.WIDTH - 140f)
        val a = Easing.BACK_OUT.ease(window(t, 800f, 700f))
        g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.3f, 1f, a)), 540f, cy) {
            g.drawText(num, 540f - g.measureText(num, f) / 2, cy + g.metrics(f).ascent / 2 - (if (unit.isEmpty()) 0f else 30f), f, st.accent.let { if (luma(it) > 0.75f) paperInk else it })
            if (unit.isNotEmpty()) {
                val uf = fitSize(g, unit, FontSpec(FontFamily.TEXT, 700, 46f), Safe.WIDTH - 140f)
                g.drawText(unit, 540f - g.measureText(unit, uf) / 2, cy + g.metrics(f).ascent / 2 + 60f, uf, paperMuted)
            }
        }
        // Éclats autour du chiffre
        val burst = window(t, 900f, 800f)
        if (burst > 0f && burst < 1f) for (k in 0 until 12) {
            val ang = k * 30f * Math.PI.toFloat() / 180f
            val r0 = 180f + 160f * burst
            g.fillCircle(540f + cos(ang) * r0, cy + sin(ang) * r0 * 0.6f, 9f * (1 - burst), if (k % 2 == 0) st.accent else st.accent2)
        }
        y = bottom
        return if (t in 800f..2000f) 1f else 0f
    }

    private fun compare(g: Gfx, sc: Scene, t: Float, limit: Float, cx: Float): Pair<Float, Mood> {
        val cmp = sc.compare ?: return hero(g, sc, t, limit, cx) to Mood.NEUTRAL
        var y = title(g, sc, t, 200f, 70f) + 44f
        val cardH = ((limit - 60f - y - 30f) / 2).coerceIn(200f, 330f)
        fun card(top: Float, label: String, value: String, color: Int, icon: IconId, start: Float, key: String) {
            val a = Easing.BACK_OUT.ease(window(t, start, 600f))
            if (a <= 0f) return
            g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.8f, 1f, a)), 540f, top + cardH / 2) {
                g.fillRoundRect(Safe.LEFT + 6f, top + 12f, Safe.RIGHT + 6f, top + cardH + 12f, 36f, 0x26000000)
                g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, top + cardH, 36f, paper)
                g.fillRoundRect(Safe.LEFT, top, Safe.LEFT + 18f, top + cardH, 9f, color)
                g.fillCircle(Safe.LEFT + 86f, top + 72f, 34f, color)
                Icons.draw(g, icon, Safe.LEFT + 86f, top + 72f, 38f, paper, 3f)
                val lf = FontSpec(FontFamily.ROUNDED, 700, 46f)
                g.drawText(label, Safe.LEFT + 140f, top + 72f + g.metrics(lf).ascent / 2, lf, color)
                val vb = cache.fit(g, key, value, FontSpec(FontFamily.TEXT, 600, 40f), Safe.WIDTH - 100f, 3, 1.25f)
                g.drawBlock(vb, Safe.LEFT + 50f, top + 130f, paperInk)
            }
        }
        card(y, cmp.badLabel, cmp.badText(), danger, IconId.CROSS, 500f, "${sc.id}/b")
        val goodAt = sc.durationMs * 0.45f
        card(y + cardH + 30f, cmp.goodLabel, cmp.goodText(), ok, IconId.CHECK, goodAt, "${sc.id}/g")
        val mood = if (t < goodAt) (if (t > 700f) Mood.WORRIED else Mood.NEUTRAL) else Mood.HAPPY
        val talk = if (t in 600f..1800f || t in goodAt..goodAt + 1400f) 1f else 0f
        return talk to mood
    }

    private fun checklist(g: Gfx, sc: Scene, t: Float, limit: Float, cx: Float): Float {
        val y = title(g, sc, t, 220f, 80f) + 44f
        val n = sc.items.size.coerceAtLeast(1)
        val ctaH = if (sc.body.isNotEmpty()) 120f else 0f
        val row = ((limit - 80f - y - 70f - ctaH) / n).coerceIn(84f, 160f)
        val bottom = y + 50f + row * n + ctaH
        bubble(g, Safe.LEFT, y, Safe.RIGHT, bottom, cx, window(t, 400f, 400f))
        val step = ((sc.durationMs * 0.6f - 700f) / n).coerceAtLeast(350f)
        for ((i, item) in sc.items.withIndex()) {
            val top = y + 25f + i * row
            val a = Easing.BACK_OUT.ease(window(t, 700f + i * step, 450f))
            if (a <= 0f) continue
            g.withPose(Pose(alpha = clamp01(a), dx = (1 - a) * 40f), Safe.LEFT, top) {
                g.fillCircle(Safe.LEFT + 74f, top + row / 2, 26f, ok)
                Icons.draw(g, IconId.CHECK, Safe.LEFT + 74f, top + row / 2, 32f, paper, 3.4f, reveal = clamp01(a))
                val b = cache.fit(g, "${sc.id}/i$i", Marked.strip(item), fSmall.copy(weight = 600), Safe.WIDTH - 180f, if (row >= 130f) 2 else 1, 1.2f)
                g.drawBlock(b, Safe.LEFT + 124f, top + (row - b.height) / 2, paperInk)
            }
        }
        if (sc.body.isNotEmpty()) {
            val a = Easing.BACK_OUT.ease(window(t, 700f + n * step, 500f))
            val f = FontSpec(FontFamily.TEXT, 800, 36f)
            val text = Marked.strip(sc.body)
            val w = (g.measureText(text, f) + 70f).coerceAtMost(Safe.WIDTH - 100f)
            val top = y + 40f + row * n
            if (a > 0f) g.withPose(Pose(alpha = clamp01(a), scale = lerp(0.6f, 1f, a)), 540f, top + 40f) {
                g.fillRoundRect(540f - w / 2, top, 540f + w / 2, top + 80f, 40f, st.accent)
                g.drawText(text, 540f - w / 2 + 35f, top + 40f + g.metrics(f).ascent / 2, f, st.accentInk)
            }
        }
        return 0f
    }

    // ───────────────────────────── Codi ─────────────────────────────

    private fun codi(g: Gfx, p: Pose2, t: Float, tg: Float) {
        val dark = rgb(0x12151C)
        val bodyDark = mix(body, 0xFF000000.toInt(), 0.22f)
        val bodyLight = mix(body, 0xFFFFFFFF.toInt(), 0.35f)
        val bob = sin(tg * 0.006f) * 6f
        g.save()
        g.translate(p.x, p.y + bob)
        g.scale(p.scale, 0f, 0f)
        // Ombre au sol (ne monte pas avec les sauts)
        g.fillRoundRect(-120f, 222f + p.hop * 60f, 120f, 248f + p.hop * 60f, 13f, 0x30000000)
        // Jambes
        g.fillRoundRect(-62f, 120f, -24f, 228f, 19f, bodyDark)
        g.fillRoundRect(24f, 120f, 62f, 228f, 19f, bodyDark)
        g.fillRoundRect(-74f, 212f, -14f, 236f, 12f, dark)
        g.fillRoundRect(14f, 212f, 74f, 236f, 12f, dark)
        // Bras gauche (au repos ou qui salue)
        val waveA = if (p.wave) sin(t * 0.012f) * 28f else 0f
        arm(g, -148f, 20f, if (p.wave && p.armAngle > 90f) 200f + waveA else 115f + sin(tg * 0.003f) * 6f, bodyDark, dark)
        // Antenne
        g.line(0f, -126f, 0f, -186f, 7f, dark)
        val glow = 0.5f + 0.5f * sin(tg * 0.008f)
        g.fillCircle(0f, -196f, 30f, accent2.withAlpha(0.25f * glow))
        g.fillCircle(0f, -196f, 17f, accent2)
        // Corps
        g.fillRoundRect(-152f, -128f, 152f, 150f, 74f, body)
        g.fillRoundRect(-124f, -116f, 60f, -96f, 10f, bodyLight.withAlpha(0.7f))
        g.fillRoundRect(-60f, 104f, 60f, 128f, 12f, bodyDark.withAlpha(0.5f))
        // Écran du visage
        g.fillRoundRect(-114f, -90f, 114f, 72f, 48f, dark)
        // Yeux
        val blink = blinkAt(tg)
        val ex = p.lookX * 10f
        val ey = p.lookY * 8f
        val eye = rgb(0x7CF3FF)
        for (side in listOf(-1f, 1f)) {
            val cx = side * 46f + ex
            val cy = -22f + ey
            when (p.mood) {
                Mood.HAPPY -> g.strokeArc(cx, cy + 10f, 20f, 200f, 140f, 9f, eye)
                Mood.WORRIED -> {
                    g.fillRoundRect(cx - 14f, cy - 16f * blink, cx + 14f, cy + 16f * blink, 12f, eye)
                    g.line(cx - 22f, cy - 34f + side * 8f, cx + 22f, cy - 34f - side * 8f, 6f, eye)
                }
                Mood.NEUTRAL -> g.fillRoundRect(cx - 17f, cy - 26f * blink, cx + 17f, cy + 26f * blink, 15f, eye)
            }
        }
        // Bouche
        val open = if (p.talk > 0f) (0.5f + 0.5f * sin(t * 0.045f)) * p.talk else 0f
        when {
            open > 0.05f -> g.fillRoundRect(-22f + ex, 30f, 22f + ex, 36f + 22f * open, 10f, eye)
            p.mood == Mood.HAPPY -> g.strokeArc(ex, 20f, 22f, 25f, 130f, 6f, eye)
            p.mood == Mood.WORRIED -> g.strokePath(PathData().moveTo(-20f + ex, 40f).cubicTo(-8f + ex, 32f, 8f + ex, 48f, 20f + ex, 40f), 6f, eye)
            else -> g.fillRoundRect(-16f + ex, 34f, 16f + ex, 40f, 3f, eye)
        }
        if (p.mood == Mood.HAPPY) {
            g.fillCircle(-82f, 40f, 12f, rgb(0xFF8FA3).withAlpha(0.6f))
            g.fillCircle(82f, 40f, 12f, rgb(0xFF8FA3).withAlpha(0.6f))
        }
        // Bras droit : pointe vers le contenu (ou salue)
        val pointA = if (p.armAngle > 90f) 220f else -40f
        arm(g, 148f, 20f, if (p.wave && p.armAngle <= 90f) -60f + waveA else pointA + sin(tg * 0.004f) * 5f, bodyDark, dark)
        g.restore()
    }

    private fun arm(g: Gfx, sx: Float, sy: Float, deg: Float, c: Int, hand: Int) {
        val a = Math.toRadians(deg.toDouble())
        val len = 110f
        val ex = sx + (cos(a) * len).toFloat()
        val ey = sy + (sin(a) * len).toFloat()
        g.line(sx, sy, ex, ey, 30f, c)
        g.fillCircle(sx, sy, 15f, c)
        g.fillCircle(ex, ey, 24f, hand)
    }

    /** Clignement : fermé pendant ~120 ms toutes les ~3 s. */
    private fun blinkAt(t: Float): Float {
        val period = 3100f
        val ph = t % period
        return if (ph < 120f) 0.12f + 0.88f * abs(ph - 60f) / 60f else 1f
    }

    private fun luma(c: Int): Float = (0.2126f * ((c shr 16) and 0xFF) + 0.7152f * ((c shr 8) and 0xFF) + 0.0722f * (c and 0xFF)) / 255f

    @Suppress("unused") private fun angleTo(x0: Float, y0: Float, x1: Float, y1: Float) = Math.toDegrees(atan2((y1 - y0).toDouble(), (x1 - x0).toDouble())).toFloat()
}
