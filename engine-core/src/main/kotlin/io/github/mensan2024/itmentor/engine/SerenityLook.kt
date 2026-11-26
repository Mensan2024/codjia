// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * Format « Sérénité » : conçu pour les sujets de bien-être et de santé mentale. Fond dégradé doux, un cercle
 * lumineux qui « respire » lentement (simple décor : aucune consigne de respiration n'est affichée, aucun
 * diagnostic, aucune promesse), poussières de lumière en dérive, texte serif centré, une idée à la fois.
 * Pas de flash, pas de coupure sèche, pas de mouvement brusque. Trois ambiances : crépuscule, sable, sauge.
 */
internal class SerenityLook(private val sb: Storyboard) : LookRenderer {
    private data class Palette(val top: Int, val bottom: Int, val ink: Int, val muted: Int, val accent: Int, val orb: Int, val dark: Boolean)
    private val palettes = listOf(
        Palette(rgb(0x14213D), rgb(0x1E4050), rgb(0xF4EFE6), rgb(0xA9B7C6), rgb(0x8FD3C1), rgb(0x8FD3C1), true),   // crépuscule
        Palette(rgb(0xF7F0E5), rgb(0xE9DCC9), rgb(0x2B2A28), rgb(0x6F675B), rgb(0xB5654A), rgb(0xE2A98B), false),  // sable
        Palette(rgb(0x1C2A25), rgb(0x2E4639), rgb(0xEEF2EA), rgb(0xA8B8AC), rgb(0xB9D8A5), rgb(0xB9D8A5), true),   // sauge
    )
    private val pal = palettes[(Rand.of(sb.seed, 0x5E8E).nextInt() ushr 1) % palettes.size]
    override val ink = pal.ink
    override val muted = pal.muted
    override val accent = pal.accent
    override val accent2 = pal.muted
    private val cache = TextCache()
    private val serif = FontSpec(FontFamily.CINEMA, 400, 100f)
    private val italic = FontSpec(FontFamily.CINEMA_ITALIC, 400, 100f)
    private val sans = FontSpec(FontFamily.TEXT, 500, 34f)
    private val caps = FontSpec(FontFamily.TEXT, 600, 26f)
    private val L = Safe.LEFT + 20f
    private val R = Safe.RIGHT - 20f
    private val W = R - L
    private val bottomLimit = Safe.BOTTOM - 120f

    /** Poussières de lumière : positions de départ fixes (graine du storyboard), dérive lente vers le haut. */
    private val motes: List<FloatArray> = Rand.of(sb.seed, 0x77A1).let { r ->
        List(16) { floatArrayOf(r.range(60f, 1020f), r.range(0f, 1920f), r.range(3f, 8f), r.range(8f, 18f)) }
    }

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        val t = localMs
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, pal.top, pal.bottom)
        for (m in motes) {
            val y = ((m[1] - tGlobalMs * m[3] / 1000f) % 1920f + 1920f) % 1920f
            g.fillCircle(m[0] + sin((tGlobalMs / 4000f + m[1]) * 1f) * 14f, y, m[2], pal.accent.withAlpha(if (pal.dark) 0.16f else 0.20f))
        }
        when (sc.layout) {
            Layout.HERO -> hero(g, sc, t, tGlobalMs)
            Layout.DEFINITION -> definition(g, sc, t, tGlobalMs)
            Layout.STEPS -> steps(g, sc, t, tGlobalMs)
            Layout.STAT -> stat(g, sc, t, tGlobalMs)
            Layout.COMPARE -> compare(g, sc, t, tGlobalMs)
            Layout.CHECKLIST -> checklist(g, sc, t, tGlobalMs)
            Layout.SCREEN -> {}
        }
    }

    // ───────────────────────────── Éléments communs ─────────────────────────────

    private fun fade(t: Float, delay: Float, dur: Float = 1100f) = Easing.EXPO_OUT.ease(window(t, delay, dur))

    /** Le cercle qui respire : cycle de 10 s, jamais d'à-coup. */
    private fun orb(g: Gfx, cx: Float, cy: Float, r: Float, t: Float, tGlobal: Float) {
        val a = fade(t, 0f, 1600f)
        val breath = 1f + 0.10f * sin(2f * PI.toFloat() * tGlobal / 10_000f)
        val rr = r * breath
        g.fillRadialGlow(cx, cy, rr * 2.6f, pal.orb.withAlpha((if (pal.dark) 0.20f else 0.30f) * a))
        g.fillRadialGlow(cx, cy, rr * 1.6f, pal.orb.withAlpha((if (pal.dark) 0.30f else 0.40f) * a))
        g.fillCircle(cx, cy, rr, pal.orb.withAlpha((if (pal.dark) 0.55f else 0.70f) * a))
        g.strokeCircle(cx, cy, rr * 1.18f, 2f, pal.orb.withAlpha(0.45f * a))
    }

    private fun kicker(g: Gfx, sc: Scene, t: Float) {
        val txt = (sc.kicker ?: return).uppercase()
        val a = fade(t, 200f)
        val x = 540f - g.measureTracked(txt, caps, 5f) / 2
        g.withPose(Pose(alpha = a), x, 214f) { g.drawTracked(txt, x, 214f, caps, pal.accent, 5f) }
    }

    /** Titre serif centré, mots mis en valeur en italique accentué, apparition douce ligne par ligne. */
    private fun headline(g: Gfx, sc: Scene, key: String, size: Float, top: Float, lines: Int, t: Float, delay: Float): Float {
        val marked = Marked.parse(sc.title)
        val f = serif.withSize(size)
        var fitW = W
        var b = cache.fit(g, key, marked.plain, f, fitW, lines, 1.08f)
        repeat(4) {
            val rr = MarkedText.runs(b, marked)
            val fiw = italic.withSize(b.font.size)
            if (b.lines.indices.all { MarkedText.lineWidth(g, rr[it], b.font, fiw) <= W }) return@repeat
            fitW *= 0.93f
            b = cache.fit(g, "$key/$fitW", marked.plain, f, fitW, lines, 1.08f)
        }
        val runs = MarkedText.runs(b, marked)
        val fi = italic.withSize(b.font.size)
        for (i in b.lines.indices) {
            val a = fade(t, delay + i * 220f, 1400f)
            val base = g.baselineOf(b, top, i)
            val lw = MarkedText.lineWidth(g, runs[i], b.font, fi)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 22f), 540f, base) {
                MarkedText.drawLine(g, runs[i], 540f - lw / 2, base, b.font, pal.ink, pal.accent, fi)
            }
        }
        return top + b.height
    }

    private fun para(g: Gfx, key: String, text: String, font: FontSpec, top: Float, w: Float, lines: Int, color: Int, t: Float, delay: Float, lh: Float = 1.4f): TextBlock {
        val b = cache.fit(g, key, Marked.strip(text), font, w, lines, lh)
        val a = fade(t, delay, 1300f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 18f), 540f, top) { g.drawBlock(b, 540f, top, color, Align.CENTER) }
        return b
    }

    /** Décalage vertical qui centre un groupe de contenu dans la page (au plus 260 px, jamais vers le haut). */
    private fun centerShift(top: Float, end: Float): Float = ((bottomLimit - top) - (end - top)).coerceAtLeast(0f).let { (it * 0.5f).coerceAtMost(260f) }

    // ───────────────────────────── Scènes ─────────────────────────────

    private fun hero(g: Gfx, sc: Scene, t: Float, tg: Float) {
        // L'échelle baisse jusqu'à ce que le titre et le chapeau tiennent, sans jamais couper le texte.
        var k = 1f
        var title: TextBlock; var sub: TextBlock?
        while (true) {
            title = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title).plain, serif.withSize(112f * k), W, 6, 1.08f)
            sub = sc.subtitle?.let { cache.fit(g, "${sc.id}/s@$k", Marked.strip(it), italic.withSize(46f * k), W - 40f, 7, 1.34f) }
            val total = 2 * 150f * k + 90f + title.height + (sub?.height?.plus(70f) ?: 0f)
            if (total <= bottomLimit - 240f || k <= 0.62f) break
            k -= 0.06f
        }
        val orbR = 150f * k
        val endY = 330f + 2 * orbR + 90f + title.height + (sub?.height?.plus(70f) ?: 0f)
        g.save(); g.translate(0f, centerShift(330f, endY) * 0.6f)
        val orbY = 330f + orbR
        orb(g, 540f, orbY, orbR, t, tg)
        var y = orbY + orbR * 1.35f + 40f
        y = headline(g, sc, "${sc.id}/h@$k", 112f * k, y, 6, t, 500f) + 50f
        sub?.let { para(g, "${sc.id}/s@$k", sc.subtitle!!, italic.withSize(46f * k), y, W - 40f, 7, pal.muted, t, 1700f, 1.34f) }
        g.restore()
    }

    private fun definition(g: Gfx, sc: Scene, t: Float, tg: Float) {
        kicker(g, sc, t)
        var k = 1f
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        var endY: Float
        while (true) {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title).plain, serif.withSize(86f * k), W, 3, 1.08f)
            val bb = cache.fit(g, "${sc.id}/b@$k", body, serif.withSize(54f * k), W - 30f, 14, 1.42f)
            endY = 500f + tb.height + 76f + bb.height
            if (endY <= bottomLimit || k <= 0.62f) break
            k -= 0.06f
        }
        g.save(); g.translate(0f, centerShift(300f, endY) * 0.6f)
        orb(g, 540f, 360f, 62f, t, tg)
        var y = headline(g, sc, "${sc.id}/h@$k", 86f * k, 500f, 3, t, 300f) + 36f
        val a = Easing.EASE_IN_OUT.ease(window(t, 1000f, 1100f))
        g.fillRect(540f - 60f * a, y, 540f + 60f * a, y + 3f, pal.accent)
        y += 40f
        if (body.isNotEmpty()) para(g, "${sc.id}/b@$k", body, serif.withSize(54f * k), y, W - 30f, 14, pal.ink, t, 1300f, 1.42f)
        g.restore()
    }

    private fun steps(g: Gfx, sc: Scene, t: Float, tg: Float) {
        kicker(g, sc, t)
        val dotR = 34f
        val textX = L + dotR * 2 + 30f
        val textW = R - textX
        class Row(val top: Float, val label: TextBlock, val detail: TextBlock?, val h: Float)
        var titleH = 0f
        fun layout(k: Float): List<Row> {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title).plain, serif.withSize(80f * k), W, 3, 1.08f)
            titleH = tb.height
            val out = ArrayList<Row>(); var y = 440f + tb.height + 56f
            for ((i, item) in sc.items.withIndex()) {
                val (label, detail) = splitLabel(Marked.strip(item))
                val lb = cache.fit(g, "${sc.id}/l$i@$k", label, serif.withSize(52f * k), textW, 4, 1.12f)
                val db = detail?.let { cache.fit(g, "${sc.id}/d$i@$k", it, sans.withSize(34f * k), textW, 5, 1.32f) }
                val h = maxOf(dotR * 2, lb.height + (db?.height?.plus(8f) ?: 0f))
                out += Row(y, lb, db, h)
                y += h + 44f * k
            }
            return out
        }
        val ks = listOf(1f, 0.9f, 0.8f, 0.72f, 0.65f, 0.6f)
        val chosen = ks.firstOrNull { k -> layout(k).let { r -> r.isEmpty() || r.last().top + r.last().h <= bottomLimit } } ?: ks.last()
        val rows = layout(chosen)
        g.save(); g.translate(0f, centerShift(300f, rows.lastOrNull()?.let { it.top + it.h } ?: 600f) * 0.6f)
        orb(g, 540f, 330f, 46f, t, tg)
        headline(g, sc, "${sc.id}/h@$chosen", 80f * chosen, 440f, 3, t, 300f)
        for ((i, r) in rows.withIndex()) {
            val d = 900f + i * 700f
            val a = fade(t, d, 1200f)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 22f), L, r.top) {
                val cy = r.top + dotR
                g.fillCircle(L + dotR, cy, dotR, pal.accent.withAlpha(if (pal.dark) 0.22f else 0.25f))
                g.strokeCircle(L + dotR, cy, dotR, 2f, pal.accent.withAlpha(0.75f))
                val nf = serif.withSize(40f * chosen.coerceAtLeast(0.8f))
                val num = (i + 1).toString()
                g.drawText(num, L + dotR - g.measureText(num, nf) / 2, cy + g.metrics(nf).ascent / 2.2f, nf, pal.accent)
                val labelTop = if (r.detail == null) r.top + (dotR * 2 - r.label.height).coerceAtLeast(0f) / 2 else r.top - 4f
                g.drawBlock(r.label, textX, labelTop, pal.ink)
                r.detail?.let { g.drawBlock(it, textX, labelTop + r.label.height + 8f, pal.muted) }
            }
        }
        g.restore()
    }

    private fun stat(g: Gfx, sc: Scene, t: Float, tg: Float) {
        orb(g, 540f, 400f, 90f, t, tg)
        val (num, unit) = statParts(sc.statValue ?: "")
        val f = fitSize(g, num, serif.withSize(300f), W)
        val a = fade(t, 500f, 1500f)
        val base = 900f
        g.withPose(Pose(alpha = a, scale = lerp(1.05f, 1f, a)), 540f, base - 100f) { g.drawText(num, 540f - g.measureText(num, f) / 2, base, f, pal.ink) }
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit, italic.withSize(80f), W)
            val ua = fade(t, 1100f)
            g.withPose(Pose(alpha = ua), 540f, base + 110f) { g.drawText(unit, 540f - g.measureText(unit, uf) / 2, base + 120f, uf, pal.accent) }
        }
        headline(g, sc, "${sc.id}/h", 58f, base + 230f, 4, t, 1500f)
    }

    private fun compare(g: Gfx, sc: Scene, t: Float, tg: Float) {
        val cmp = sc.compare ?: return hero(g, sc, t, tg)
        orb(g, 540f, 330f, 50f, t, tg)
        var y = headline(g, sc, "${sc.id}/h", 76f, 440f, 3, t, 300f) + 60f
        fun block(label: String, value: String, top: Float, d: Float, color: Int, key: String): Float {
            val a = fade(t, d)
            g.withPose(Pose(alpha = a), 540f, top) {
                g.drawTracked(label.uppercase(), 540f - g.measureTracked(label.uppercase(), caps, 4f) / 2, top + 28f, caps, color, 4f)
            }
            val b = para(g, key, value, serif.withSize(54f), top + 64f, W - 30f, 6, pal.ink, t, d + 200f, 1.3f)
            return top + 64f + b.height
        }
        y = block(cmp.badLabel, cmp.badText(), y, 900f, pal.muted, "${sc.id}/b") + 60f
        y = block(cmp.goodLabel, cmp.goodText(), y, 2400f, pal.accent, "${sc.id}/g")
    }

    private fun checklist(g: Gfx, sc: Scene, t: Float, tg: Float) {
        val tb = cache.fit(g, "${sc.id}/hh", Marked.parse(sc.title).plain, serif.withSize(96f), W, 2, 1.08f)
        val blocks = sc.items.mapIndexed { i, it -> cache.fit(g, "${sc.id}/i$i", Marked.strip(it), serif.withSize(58f), W - 30f, 5, 1.25f) }
        val bodyB = if (sc.body.isNotEmpty()) cache.fit(g, "${sc.id}/body", Marked.strip(sc.body), italic.withSize(48f), W - 30f, 3, 1.3f) else null
        val endY = 500f + tb.height + 60f + blocks.sumOf { (it.height + 80f).toDouble() }.toFloat() + (bodyB?.height?.plus(20f) ?: 0f)
        g.save(); g.translate(0f, centerShift(300f, endY) * 0.6f)
        orb(g, 540f, 360f, 62f, t, tg)
        var y = headline(g, sc, "${sc.id}/h", 96f, 500f, 2, t, 300f) + 60f
        for ((i, item) in sc.items.withIndex()) {
            val d = 900f + i * 700f
            val a = Easing.EASE_IN_OUT.ease(window(t, d - 200f, 900f))
            g.fillRect(540f - 40f * a, y, 540f + 40f * a, y + 2f, pal.accent.withAlpha(0.7f))
            y += 40f
            val b = para(g, "${sc.id}/i$i", item, serif.withSize(58f), y, W - 30f, 5, pal.ink, t, d, 1.25f)
            y += b.height + 40f
        }
        if (bodyB != null) {
            y += 20f
            para(g, "${sc.id}/body", sc.body, italic.withSize(48f), y, W - 30f, 3, pal.accent, t, 900f + sc.items.size * 700f + 300f, 1.3f)
        }
        g.restore()
    }
}
