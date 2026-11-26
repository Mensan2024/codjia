// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.sin

/**
 * Format « Stratégie » : conçu pour le marketing et l'entrepreneuriat. Tableau de bord sombre, courbe qui monte
 * en haut de l'écran (décor : aucune valeur chiffrée inventée), carte publicitaire générique en ouverture,
 * et surtout un TUNNEL : chaque étape d'une liste devient un étage qui se rétrécit, comme un tunnel de conversion.
 * Le texte de l'auteur reste intégral ; aucun nom de marque ni statistique n'est ajouté.
 */
internal class StrategyLook(private val sb: Storyboard) : LookRenderer {
    private data class Palette(val bg: Int, val card: Int, val a: Int, val b: Int, val ink: Int, val muted: Int)
    private val palettes = listOf(
        Palette(rgb(0x0B1220), rgb(0x16213A), rgb(0x38BDF8), rgb(0xFF8A3D), rgb(0xF3F7FF), rgb(0x9FB0CB)),   // marine
        Palette(rgb(0x101214), rgb(0x1D2125), rgb(0xA3E635), rgb(0xFACC15), rgb(0xF7F8F2), rgb(0xA4ACA0)),    // anthracite
        Palette(rgb(0x140E24), rgb(0x241A3E), rgb(0xA78BFA), rgb(0xF472B6), rgb(0xF8F4FF), rgb(0xB5A9D1)),    // violet
    )
    private val pal = palettes[(Rand.of(sb.seed, 0x51A7).nextInt() ushr 1) % palettes.size]
    override val ink = pal.ink
    override val muted = pal.muted
    override val accent = pal.a
    override val accent2 = pal.b
    private val cache = TextCache()
    private val head = FontSpec(FontFamily.CONDENSED, 800, 150f)
    private val sans = FontSpec(FontFamily.TEXT, 500, 38f)
    private val sansB = FontSpec(FontFamily.TEXT, 700, 46f)
    private val caps = FontSpec(FontFamily.TEXT, 700, 26f)
    private val L = Safe.LEFT + 10f
    private val R = Safe.RIGHT - 10f
    private val W = R - L
    private val bottomLimit = Safe.BOTTOM - 120f

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        val t = localMs
        g.fillRect(0f, 0f, 1080f, 1920f, pal.bg)
        grid(g)
        trend(g, slot.index, sb.scenes.size, tGlobalMs)
        when (sc.layout) {
            Layout.HERO -> hero(g, sc, t)
            Layout.DEFINITION -> definition(g, sc, t)
            Layout.STEPS -> funnel(g, sc, t)
            Layout.STAT -> stat(g, sc, t)
            Layout.COMPARE -> compare(g, sc, t)
            Layout.CHECKLIST -> checklist(g, sc, t)
            Layout.SCREEN -> {}
        }
    }

    private fun fade(t: Float, delay: Float, dur: Float = 700f) = Easing.EXPO_OUT.ease(window(t, delay, dur))

    private fun grid(g: Gfx) {
        for (i in 1..9) g.fillRect(0f, i * 200f, 1080f, i * 200f + 1.5f, pal.ink.withAlpha(0.04f))
        for (i in 1..5) g.fillRect(i * 180f, 0f, i * 180f + 1.5f, 1920f, pal.ink.withAlpha(0.03f))
    }

    /** Courbe de tendance décorative en haut à droite : monte au fil de la vidéo, sans axe ni valeur. */
    private fun trend(g: Gfx, index: Int, n: Int, tg: Float) {
        val total = sb.scenes.sumOf { it.durationMs }.coerceAtLeast(1)
        val prog = clamp01(tg / total)
        val x0 = R - 300f; val y0 = 240f; val w = 300f; val h = 70f
        val pts = 24
        val path = PathData()
        for (i in 0..pts) {
            val u = i / pts.toFloat()
            if (u > prog + 0.04f) break
            val y = y0 - h * (u * 0.85f + 0.08f * sin(u * 14f + index))
            if (i == 0) path.moveTo(x0 + w * u, y) else path.lineTo(x0 + w * u, y)
        }
        if (prog > 0.05f) g.strokePath(path, 5f, pal.a)
        g.fillCircle(x0 + w * clamp01(prog + 0.04f), y0 - h * (clamp01(prog + 0.04f) * 0.85f + 0.08f * sin(clamp01(prog + 0.04f) * 14f + index)), 8f, pal.b)
    }

    private fun kicker(g: Gfx, sc: Scene, t: Float, y: Float) {
        val txt = (sc.kicker ?: return).uppercase()
        val a = fade(t, 100f)
        g.withPose(Pose(alpha = a, dx = (1 - a) * -30f), L, y) { g.drawTracked(txt, L, y, caps, pal.b, 5f) }
    }

    private fun headline(g: Gfx, text: String, key: String, size: Float, top: Float, lines: Int, t: Float, delay: Float): Float {
        val marked = Marked.parse(text.uppercase())
        val b = cache.fit(g, key, marked.plain, head.withSize(size), W, lines, 1.0f)
        val runs = MarkedText.runs(b, marked)
        for (i in b.lines.indices) {
            val a = fade(t, delay + i * 140f)
            val base = g.baselineOf(b, top, i)
            g.withPose(Pose(alpha = a, dx = (1 - a) * -60f), L, base) { MarkedText.drawLine(g, runs[i], L, base, b.font, pal.ink, pal.a, b.font) }
        }
        return top + b.height
    }

    private fun para(g: Gfx, key: String, text: String, font: FontSpec, x: Float, top: Float, w: Float, lines: Int, color: Int, t: Float, delay: Float, lh: Float = 1.36f): TextBlock {
        val b = cache.fit(g, key, Marked.strip(text), font, w, lines, lh)
        val a = fade(t, delay, 900f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 20f), x, top) { g.drawBlock(b, x, top, color) }
        return b
    }

    // ───────────────────────────── Scènes ─────────────────────────────

    /** Carte publicitaire générique (aucune marque) : visuel, ligne de titre factice, bouton sans texte. */
    private fun adCard(g: Gfx, top: Float, t: Float) {
        val a = fade(t, 100f, 900f)
        val h = 330f
        g.withPose(Pose(alpha = a, dy = (1 - a) * 40f, scale = lerp(0.96f, 1f, a)), 540f, top + h / 2) {
            g.fillRoundRect(L, top, R, top + h, 28f, pal.card)
            g.fillRoundRect(L + 20f, top + 20f, R - 20f, top + 200f, 20f, pal.a.withAlpha(0.28f))
            g.fillCircle(540f, top + 110f, 46f, pal.ink.withAlpha(0.9f))
            val tri = PathData().moveTo(528f, top + 88f).lineTo(528f, top + 132f).lineTo(560f, top + 110f).close()
            g.fillPath(tri, pal.bg)
            g.fillRoundRect(L + 20f, top + 222f, L + 380f, top + 240f, 9f, pal.ink.withAlpha(0.55f))
            g.fillRoundRect(L + 20f, top + 254f, L + 260f, top + 268f, 7f, pal.muted.withAlpha(0.5f))
            g.fillRoundRect(R - 220f, top + 232f, R - 20f, top + 296f, 32f, pal.b)
        }
    }

    private fun hero(g: Gfx, sc: Scene, t: Float) {
        var k = 1f
        var endY: Float
        while (true) {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(150f * k), W, 5, 1.0f)
            val sb2 = sc.subtitle?.let { cache.fit(g, "${sc.id}/s@$k", Marked.strip(it), sans.withSize(42f), W - 40f, 6, 1.36f) }
            endY = 800f + tb.height + 60f + (sb2?.height?.plus(30f) ?: 0f)
            if (endY <= bottomLimit || k <= 0.55f) break
            k -= 0.06f
        }
        kicker(g, sc, t, 300f)
        adCard(g, 380f, t)
        var y = headline(g, sc.title, "${sc.id}/h@$k", 150f * k, 800f, 5, t, 400f) + 30f
        sc.subtitle?.let { para(g, "${sc.id}/s@$k", it, sans.withSize(42f), L, y, W - 40f, 6, pal.muted, t, 1300f) }
    }

    private fun definition(g: Gfx, sc: Scene, t: Float) {
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        var k = 1f
        var endY: Float
        while (true) {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(124f * k), W, 3, 1.0f)
            val bb = cache.fit(g, "${sc.id}/b@$k", body, sans.withSize(50f * k), W - 90f, 12, 1.38f)
            endY = 440f + tb.height + 70f + bb.height + 80f
            if (endY <= bottomLimit || k <= 0.6f) break
            k -= 0.06f
        }
        kicker(g, sc, t, 330f)
        val y = headline(g, sc.title, "${sc.id}/h@$k", 124f * k, 380f, 3, t, 200f) + 50f
        val bb = cache.fit(g, "${sc.id}/b@$k", body, sans.withSize(50f * k), W - 90f, 12, 1.38f)
        val a = fade(t, 700f, 900f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 30f), L, y) {
            g.fillRoundRect(L, y, R, y + bb.height + 70f, 26f, pal.card)
            g.fillRect(L, y + 18f, L + 8f, y + bb.height + 52f, pal.a)
            g.drawBlock(bb, L + 44f, y + 35f, pal.ink)
        }
    }

    /** Tunnel de conversion : chaque étape est un étage plus étroit que le précédent. */
    private fun funnel(g: Gfx, sc: Scene, t: Float) {
        val n = sc.items.size.coerceAtLeast(1)
        class Row(val top: Float, val inset: Float, val h: Float, val label: TextBlock, val detail: TextBlock?)
        fun layout(k: Float): List<Row> {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(110f * k), W, 4, 1.0f)
            val out = ArrayList<Row>(); var y = 340f + tb.height + 50f
            for ((i, item) in sc.items.withIndex()) {
                val inset = if (n <= 1) 0f else 90f * i / (n - 1).toFloat()
                val tw = W - 2 * inset - 150f
                val (label, detail) = splitLabel(Marked.strip(item))
                val lb = cache.fit(g, "${sc.id}/l$i@$k", label, sansB.withSize(44f * k), tw, 4, 1.16f)
                val db = detail?.let { cache.fit(g, "${sc.id}/d$i@$k", it, sans.withSize(34f * k), tw, 5, 1.3f) }
                val h = maxOf(110f * k, lb.height + (db?.height?.plus(8f) ?: 0f) + 40f)
                out += Row(y, inset, h, lb, db)
                y += h + 18f * k
            }
            return out
        }
        val ks = listOf(1f, 0.9f, 0.8f, 0.72f, 0.65f, 0.6f)
        val ck = ks.firstOrNull { k -> layout(k).let { r -> r.isEmpty() || r.last().top + r.last().h <= bottomLimit } } ?: ks.last()
        val rows = layout(ck)
        kicker(g, sc, t, 290f)
        headline(g, sc.title, "${sc.id}/h@$ck", 110f * ck, 340f, 4, t, 200f)
        for ((i, r) in rows.withIndex()) {
            val d = 700f + i * 650f
            val a = fade(t, d, 800f)
            val l = L + r.inset; val rr = R - r.inset
            g.withPose(Pose(alpha = a, dy = (1 - a) * -40f), 540f, r.top) {
                g.fillRoundRect(l, r.top, rr, r.top + r.h, 22f, pal.card)
                g.fillRoundRect(l, r.top, l + 120f * ck.coerceAtLeast(0.7f), r.top + r.h, 22f, (if (i % 2 == 0) pal.a else pal.b).withAlpha(0.9f))
                val nf = head.withSize(80f * ck.coerceAtLeast(0.7f))
                val num = (i + 1).toString()
                g.drawText(num, l + 60f * ck.coerceAtLeast(0.7f) - g.measureText(num, nf) / 2, r.top + r.h / 2 + g.metrics(nf).ascent / 2.2f, nf, pal.bg)
                val tx = l + 120f * ck.coerceAtLeast(0.7f) + 28f
                val ty = r.top + (r.h - (r.label.height + (r.detail?.height?.plus(8f) ?: 0f))) / 2
                g.drawBlock(r.label, tx, ty, pal.ink)
                r.detail?.let { g.drawBlock(it, tx, ty + r.label.height + 8f, pal.muted) }
            }
        }
    }

    private fun stat(g: Gfx, sc: Scene, t: Float) {
        val (num, unit) = statParts(sc.statValue ?: "")
        val f = fitSize(g, num, head.withSize(480f), W)
        val a = fade(t, 300f, 900f)
        val base = 900f
        g.withPose(Pose(alpha = a, scale = lerp(0.92f, 1f, a)), L, base - 150f) { g.drawText(num, L, base, f, pal.a) }
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit.uppercase(), head.withSize(110f), W)
            g.withPose(Pose(alpha = fade(t, 800f)), L, base + 120f) { g.drawText(unit.uppercase(), L, base + 130f, uf, pal.ink) }
        }
        val bw = Easing.EASE_IN_OUT.ease(window(t, 900f, 1200f))
        g.fillRoundRect(L, base + 170f, R, base + 190f, 10f, pal.card)
        g.fillRoundRect(L, base + 170f, L + W * 0.72f * bw, base + 190f, 10f, pal.b)
        headline(g, sc.title, "${sc.id}/h", 64f, base + 250f, 4, t, 1200f)
    }

    private fun compare(g: Gfx, sc: Scene, t: Float) {
        val cmp = sc.compare ?: return hero(g, sc, t)
        var y = headline(g, sc.title, "${sc.id}/h", 100f, 340f, 3, t, 200f) + 50f
        fun card(label: String, value: String, top: Float, d: Float, color: Int, key: String): Float {
            val b = cache.fit(g, key, Marked.strip(value), sansB.withSize(46f), W - 70f, 6, 1.24f)
            val a = fade(t, d, 800f)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 30f), L, top) {
                g.fillRoundRect(L, top, R, top + b.height + 100f, 24f, pal.card)
                g.fillRect(L, top + 18f, L + 8f, top + b.height + 82f, color)
                g.drawTracked(label.uppercase(), L + 36f, top + 48f, caps, color, 5f)
                g.drawBlock(b, L + 36f, top + 70f, pal.ink)
            }
            return top + b.height + 100f
        }
        y = card(cmp.badLabel, cmp.badText(), y, 700f, pal.muted, "${sc.id}/b") + 36f
        card(cmp.goodLabel, cmp.goodText(), y, 1900f, pal.a, "${sc.id}/g")
    }

    private fun checklist(g: Gfx, sc: Scene, t: Float) {
        val blocks = sc.items.mapIndexed { i, it -> cache.fit(g, "${sc.id}/i$i", Marked.strip(it), sansB.withSize(48f), W - 130f, 5, 1.22f) }
        var y = headline(g, sc.title, "${sc.id}/h", 124f, 340f, 3, t, 200f) + 50f
        for ((i, b) in blocks.withIndex()) {
            val d = 700f + i * 600f
            val a = fade(t, d, 800f)
            val h = maxOf(b.height + 50f, 100f)
            g.withPose(Pose(alpha = a, dx = (1 - a) * 60f), L, y) {
                g.fillRoundRect(L, y, R, y + h, 22f, pal.card)
                g.fillCircle(L + 52f, y + h / 2, 26f, pal.a)
                val s = Easing.EASE_IN_OUT.ease(window(t, d + 250f, 500f))
                if (s > 0f) {
                    g.line(L + 40f, y + h / 2, L + 49f, y + h / 2 + 10f, 6f, pal.bg)
                    g.line(L + 49f, y + h / 2 + 10f, L + 40f + 24f * s, y + h / 2 + 10f - 22f * s, 6f, pal.bg)
                }
                g.drawBlock(b, L + 100f, y + (h - b.height) / 2, pal.ink)
            }
            y += h + 22f
        }
        if (sc.body.isNotEmpty()) para(g, "${sc.id}/body", sc.body, sans.withSize(44f), L, y + 14f, W, 3, pal.muted, t, 700f + sc.items.size * 600f + 200f)
    }
}
