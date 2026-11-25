// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Format « Éditorial chic » : mise en page de magazine. Grande serif (Instrument Serif), italique pour
 * les mots clés, filets fins qui se tracent, chiffres de chapitre, lettrine. Mouvements lents et
 * amples, zoom très léger (effet « Ken Burns »), grain discret. Trois palettes : ivoire, noir, marine.
 */
internal class EditorialLook(private val sb: Storyboard) : LookRenderer {
    private data class Palette(val bg: Int, val ink: Int, val muted: Int, val accent: Int, val rule: Int)
    private val palettes = listOf(
        Palette(rgb(0xF3EEE5), rgb(0x161412), rgb(0x6B645B), rgb(0xA8432C), rgb(0x161412)),
        Palette(rgb(0x0E0E0E), rgb(0xF2EDE4), rgb(0xA39C91), rgb(0xC9A45C), rgb(0xF2EDE4)),
        Palette(rgb(0x0F1B2D), rgb(0xEDE6DA), rgb(0x9FA9B8), rgb(0xE3B23C), rgb(0xEDE6DA)),
    )
    private val pal = palettes[(Rand.of(sb.seed, 0x4ED1).nextInt() ushr 1) % palettes.size]
    override val ink = pal.ink
    override val muted = pal.muted
    override val accent = pal.accent
    override val accent2 = pal.muted
    private val cache = TextCache()
    private val serif = FontSpec(FontFamily.CINEMA, 400, 120f)
    private val italic = FontSpec(FontFamily.CINEMA_ITALIC, 400, 120f)
    private val sans = FontSpec(FontFamily.TEXT, 400, 40f)
    private val caps = FontSpec(FontFamily.TEXT, 600, 26f)
    private val L = Safe.LEFT + 34f
    private val R = Safe.RIGHT - 20f

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        val t = localMs
        g.fillRect(0f, 0f, 1080f, 1920f, pal.bg)
        // Zoom très lent sur toute la page
        val k = 1f + 0.03f * clamp01(t / sc.durationMs.coerceAtLeast(1))
        g.save(); g.scale(k, 540f, 900f)
        folio(g, sc, slot.index, t)
        when (sc.layout) {
            Layout.HERO -> hero(g, sc, t)
            Layout.DEFINITION -> definition(g, sc, t)
            Layout.STEPS -> steps(g, sc, t)
            Layout.STAT -> stat(g, sc, t)
            Layout.COMPARE -> compare(g, sc, t)
            Layout.CHECKLIST -> checklist(g, sc, t)
            Layout.SCREEN -> {}
        }
        g.restore()
    }

    override fun overlayFx(g: Gfx, frame: Int) {
        val r = Rand(0x9E37 + frame % 6)
        val c = if (pal.bg == palettes[0].bg) 0x0D000000 else 0x0DFFFFFF
        repeat(160) { val x = r.range(0f, 1080f); val y = r.range(0f, 1920f); g.fillRect(x, y, x + 2f, y + 2f, c) }
    }

    // ───────────────────────────── Éléments communs ─────────────────────────────

    private fun fade(t: Float, delay: Float, dur: Float = 900f) = Easing.EXPO_OUT.ease(window(t, delay, dur))

    /** Filet qui se trace de gauche à droite. */
    private fun rule(g: Gfx, x0: Float, x1: Float, y: Float, t: Float, delay: Float, w: Float = 2f, color: Int = pal.rule) {
        val p = Easing.EASE_IN_OUT.ease(window(t, delay, 900f))
        if (p > 0f) g.fillRect(x0, y, x0 + (x1 - x0) * p, y + w, color)
    }

    /** En-tête de page : rubrique et numéro, comme un folio de magazine. */
    private fun folio(g: Gfx, sc: Scene, index: Int, t: Float) {
        val a = fade(t, 0f)
        val y = 196f
        g.withPose(Pose(alpha = a), L, y) {
            val left = (sc.kicker ?: "CODJIA").uppercase()
            g.drawTracked(left, L, y, caps, pal.ink, 4f)
            val right = "N° %02d".format(index + 1)
            g.drawTracked(right, R - g.measureTracked(right, caps, 4f), y, caps, pal.muted, 4f)
        }
        rule(g, L, R, y + 26f, t, 100f, 1.5f)
    }

    /** Titre en serif avec mots mis en valeur en italique couleur accent. Révélation ligne par ligne, montée lente. */
    private fun headline(g: Gfx, sc: Scene, key: String, size: Float, x: Float, top: Float, w: Float, lines: Int, t: Float, delay: Float, align: Align = Align.LEFT): Float {
        val marked = Marked.parse(sc.title)
        val f = serif.withSize(size)
        // Les mots mis en valeur sont en italique, un peu plus larges : on resserre jusqu'à ce que chaque ligne tienne.
        var fitW = w
        var b = cache.fit(g, key, marked.plain, f, fitW, lines, 1.02f)
        repeat(4) {
            val rr = MarkedText.runs(b, marked)
            val fiw = italic.withSize(b.font.size)
            if (b.lines.indices.all { MarkedText.lineWidth(g, rr[it], b.font, fiw) <= w }) return@repeat
            fitW *= 0.93f
            b = cache.fit(g, "$key/$fitW", marked.plain, f, fitW, lines, 1.02f)
        }
        val runs = MarkedText.runs(b, marked)
        val fi = italic.withSize(b.font.size)
        for (i in b.lines.indices) {
            val a = fade(t, delay + i * 160f, 1100f)
            val base = g.baselineOf(b, top, i)
            val lw = MarkedText.lineWidth(g, runs[i], b.font, fi)
            val lx = if (align == Align.CENTER) x - lw / 2 else x
            g.save()
            g.clipRect(0f, base - b.font.size * 1.1f, 1080f, base + b.font.size * 0.35f)
            g.translate(0f, (1 - a) * b.font.size * 0.9f)
            MarkedText.drawLine(g, runs[i], lx, base, b.font, pal.ink, pal.accent, fi)
            g.restore()
        }
        return top + b.height
    }

    private fun para(g: Gfx, key: String, text: String, font: FontSpec, x: Float, top: Float, w: Float, lines: Int, color: Int, t: Float, delay: Float, lh: Float = 1.42f, align: Align = Align.LEFT): Float {
        val b = cache.fit(g, key, Marked.strip(text), font, w, lines, lh)
        val a = fade(t, delay, 1000f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 24f), x, top) { g.drawBlock(b, x, top, color, align) }
        return top + b.height
    }

    // ───────────────────────────── Scènes ─────────────────────────────

    private fun hero(g: Gfx, sc: Scene, t: Float) {
        // Grand titre centré au milieu de la page, chapeau en italique, signature en bas.
        val title = cache.fit(g, "${sc.id}/h", Marked.parse(sc.title).plain, serif.withSize(140f), R - L, 5, 1.02f)
        val sub = sc.subtitle?.let { cache.fit(g, "${sc.id}/s", Marked.strip(it), italic.withSize(50f), R - L - 80f, 4, 1.3f) }
        val total = title.height + 70f + (sub?.height?.plus(60f) ?: 0f)
        var y = ((1560f + 260f) / 2 - total / 2).coerceAtLeast(300f)
        y = headline(g, sc, "${sc.id}/h", 140f, 540f, y, R - L, 5, t, 300f, Align.CENTER) + 40f
        rule(g, 540f - 60f, 540f + 60f, y, t, 900f, 3f, pal.accent)
        y += 40f
        sub?.let { para(g, "${sc.id}/s", sc.subtitle!!, italic.withSize(50f), 540f, y, R - L - 80f, 4, pal.muted, t, 1200f, 1.3f, Align.CENTER) }
        val a = fade(t, 1600f)
        g.withPose(Pose(alpha = a), 540f, 1500f) {
            val by = "Une astuce CODJIA"
            g.drawTracked(by.uppercase(), 540f - g.measureTracked(by.uppercase(), caps, 3f) / 2, 1500f, caps, pal.muted, 3f)
        }
    }

    private fun definition(g: Gfx, sc: Scene, t: Float) {
        var y = 300f
        y = headline(g, sc, "${sc.id}/h", 96f, L, y, R - L, 3, t, 200f) + 60f
        rule(g, L, L + 140f, y, t, 600f, 3f, pal.accent)
        y += 60f
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        if (body.isEmpty()) return
        // Lettrine : la première lettre en très grand, le texte s'enroule à côté sur trois lignes.
        val first = body.take(1)
        val rest = body.drop(1)
        val df = serif.withSize(250f)
        val dw = g.measureText(first, df) + 26f
        val a = fade(t, 800f, 1200f)
        g.withPose(Pose(alpha = a), L, y) { g.drawText(first, L, y + g.metrics(df).ascent, df, pal.accent) }
        val bf = serif.withSize(54f)
        val wrapped = TextLayout.wrap(g, rest, bf, R - L - dw)
        val firstLines = wrapped.take(3)
        val consumed = firstLines.joinToString(" ").length
        val remaining = rest.drop(consumed).trim()
        val b1 = TextBlock(firstLines, bf, 54f * 1.36f, false)
        val pa = fade(t, 1000f, 1100f)
        g.withPose(Pose(alpha = pa, dy = (1 - pa) * 20f), L + dw, y) { g.drawBlock(b1, L + dw, y - 6f, pal.ink) }
        y += maxOf(b1.height, g.metrics(df).ascent + 20f) + 6f
        if (remaining.isNotEmpty()) para(g, "${sc.id}/r", remaining, bf, L, y, R - L, 7, pal.ink, t, 1300f, 1.36f)
    }

    private fun steps(g: Gfx, sc: Scene, t: Float) {
        var y = 290f
        y = headline(g, sc, "${sc.id}/h", 92f, L, y, R - L, 2, t, 200f) + 50f
        val n = sc.items.size.coerceAtLeast(1)
        val row = ((1540f - y) / n).coerceIn(150f, 270f)
        val nf = serif.withSize(96f)
        for ((i, item) in sc.items.withIndex()) {
            val top = y + i * row
            val d = 700f + i * 380f
            rule(g, L, R, top, t, d - 200f, 1.2f, pal.rule.withAlpha(0.35f))
            val a = fade(t, d, 1000f)
            val (label, detail) = splitLabel(Marked.strip(item))
            g.withPose(Pose(alpha = a, dy = (1 - a) * 26f), L, top) {
                val num = "%02d".format(i + 1)
                g.drawText(num, L, top + 26f + g.metrics(nf).ascent, nf, pal.accent)
                val tx = L + 170f
                val lb = cache.fit(g, "${sc.id}/l$i", label, serif.withSize(54f), R - tx, 2, 1.1f)
                g.drawBlock(lb, tx, top + 28f, pal.ink)
                detail?.let {
                    val db = cache.fit(g, "${sc.id}/d$i", it, sans.withSize(34f), R - tx, if (row > 200f) 2 else 1, 1.3f)
                    g.drawBlock(db, tx, top + 34f + lb.height, pal.muted)
                }
            }
        }
    }

    private fun stat(g: Gfx, sc: Scene, t: Float) {
        val (num, unit) = statParts(sc.statValue ?: "")
        val f = fitSize(g, num, serif.withSize(420f), R - L)
        val a = fade(t, 300f, 1400f)
        val base = 920f
        g.withPose(Pose(alpha = a, scale = lerp(1.08f, 1f, a)), 540f, base - 150f) {
            g.drawText(num, 540f - g.measureText(num, f) / 2, base, f, pal.ink)
        }
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit, italic.withSize(84f), R - L)
            val ua = fade(t, 900f)
            g.withPose(Pose(alpha = ua), 540f, base + 110f) { g.drawText(unit, 540f - g.measureText(unit, uf) / 2, base + 120f, uf, pal.accent) }
        }
        rule(g, 540f - 200f, 540f + 200f, base + 190f, t, 1100f, 2f)
        headline(g, sc, "${sc.id}/h", 64f, 540f, base + 250f, R - L - 40f, 3, t, 1300f, Align.CENTER)
    }

    private fun compare(g: Gfx, sc: Scene, t: Float) {
        val cmp = sc.compare ?: return hero(g, sc, t)
        var y = 290f
        y = headline(g, sc, "${sc.id}/h", 88f, L, y, R - L, 3, t, 200f) + 70f
        fun block(label: String, value: String, top: Float, d: Float, strike: Boolean, color: Int, key: String): Float {
            val a = fade(t, d, 1000f)
            var yy = top
            g.withPose(Pose(alpha = a), L, top) {
                g.drawTracked(label.uppercase(), L, yy + 30f, caps, color, 4f)
            }
            yy += 60f
            val vb = cache.fit(g, key, value, serif.withSize(64f), R - L, 3, 1.15f)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 20f), L, yy) { g.drawBlock(vb, L, yy, if (strike) pal.muted else pal.ink) }
            if (strike) {
                val s = Easing.EASE_IN_OUT.ease(window(t, d + 900f, 700f))
                for (i in vb.lines.indices) {
                    val base = g.baselineOf(vb, yy, i)
                    val lw = g.measureText(vb.lines[i], vb.font)
                    if (s > 0f) g.fillRect(L, base - vb.font.size * 0.28f, L + lw * s, base - vb.font.size * 0.28f + 3f, pal.accent)
                }
            }
            return yy + vb.height
        }
        y = block(cmp.badLabel, cmp.badText(), y, 600f, true, pal.muted, "${sc.id}/b") + 50f
        rule(g, L, R, y, t, 1900f, 1.2f, pal.rule.withAlpha(0.4f))
        y += 50f
        y = block(cmp.goodLabel, cmp.goodText(), y, 2200f, false, pal.accent, "${sc.id}/g") + 50f
        sc.subtitle?.let { if (y < 1380f) para(g, "${sc.id}/s", it, italic.withSize(44f), L, y, R - L, 3, pal.muted, t, 2900f, 1.3f) }
    }

    private fun checklist(g: Gfx, sc: Scene, t: Float) {
        var y = 300f
        y = headline(g, sc, "${sc.id}/h", 110f, L, y, R - L, 2, t, 200f) + 70f
        for ((i, item) in sc.items.withIndex()) {
            val d = 700f + i * 380f
            val a = fade(t, d)
            val b = cache.fit(g, "${sc.id}/i$i", Marked.strip(item), serif.withSize(56f), R - L - 70f, 2, 1.12f)
            g.withPose(Pose(alpha = a, dx = (1 - a) * -20f), L, y) {
                g.fillRect(L, g.baselineOf(b, y, 0) - 20f, L + 36f, g.baselineOf(b, y, 0) - 17f, pal.accent)
                g.drawBlock(b, L + 70f, y, pal.ink)
            }
            y += b.height + 40f
        }
        if (sc.body.isNotEmpty()) {
            y += 30f
            val d = 700f + sc.items.size * 380f + 200f
            val text = Marked.strip(sc.body)
            val f = italic.withSize(58f)
            val a = fade(t, d)
            g.withPose(Pose(alpha = a), L, y) {
                val w = g.measureText(text, f).coerceAtMost(R - L)
                g.drawText(text, L, y + g.metrics(f).ascent + 10f, f, pal.accent)
                val u = Easing.EASE_IN_OUT.ease(window(t, d + 500f, 800f))
                g.fillRect(L, y + g.metrics(f).ascent + 28f, L + w * u, y + g.metrics(f).ascent + 31f, pal.accent)
            }
        }
    }
}
