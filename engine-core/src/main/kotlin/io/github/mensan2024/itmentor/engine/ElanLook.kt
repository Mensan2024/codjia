// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Format « Élan » : conçu pour le développement personnel. Un soleil qui se lève lentement derrière le texte,
 * titres condensés en majuscules alignés à gauche, grands numéros, et une chaîne de points en haut qui se
 * remplit de scène en scène (la progression se voit). Énergique mais posé : pas de flash, pas de coupure sèche,
 * pas de promesse chiffrée. Trois ambiances : aube, or, vert.
 */
internal class ElanLook(private val sb: Storyboard) : LookRenderer {
    private data class Palette(val bg: Int, val glowA: Int, val glowB: Int, val ink: Int, val muted: Int, val accent: Int)
    private val palettes = listOf(
        Palette(rgb(0x16101F), rgb(0xFF7A3D), rgb(0xFF3D81), rgb(0xFFF4EA), rgb(0xB9AAC4), rgb(0xFF9A5C)),   // aube
        Palette(rgb(0x101418), rgb(0xFFC857), rgb(0xFF8A3D), rgb(0xFFF8E6), rgb(0xAEB6BE), rgb(0xFFC857)),    // or
        Palette(rgb(0x0E1812), rgb(0x6EE7A0), rgb(0x2BB5A0), rgb(0xEFFAF3), rgb(0xA6BDB0), rgb(0x6EE7A0)),    // vert
    )
    private val pal = palettes[(Rand.of(sb.seed, 0x3E1A).nextInt() ushr 1) % palettes.size]
    override val ink = pal.ink
    override val muted = pal.muted
    override val accent = pal.accent
    override val accent2 = pal.glowB
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
        // Soleil levant : le halo monte lentement sur toute la vidéo.
        val total = sb.scenes.sumOf { it.durationMs }.coerceAtLeast(1)
        val rise = clamp01(tGlobalMs / total)
        val sy = lerp(1860f, 1500f, rise)
        g.fillRadialGlow(540f, sy, 900f, pal.glowB.withAlpha(0.22f))
        g.fillRadialGlow(540f, sy, 560f, pal.glowA.withAlpha(0.30f))
        g.fillCircle(540f, sy + 120f, 190f, pal.glowA.withAlpha(0.35f))
        chain(g, slot.index, sb.scenes.size, t)
        when (sc.layout) {
            Layout.HERO -> hero(g, sc, t)
            Layout.DEFINITION -> definition(g, sc, t)
            Layout.STEPS -> steps(g, sc, t)
            Layout.STAT -> stat(g, sc, t)
            Layout.COMPARE -> compare(g, sc, t)
            Layout.CHECKLIST -> checklist(g, sc, t)
            Layout.SCREEN -> {}
        }
    }

    private fun fade(t: Float, delay: Float, dur: Float = 700f) = Easing.EXPO_OUT.ease(window(t, delay, dur))

    /** La chaîne : un point par scène, rempli jusqu'à la scène en cours. */
    private fun chain(g: Gfx, index: Int, n: Int, t: Float) {
        val gap = 46f
        val x0 = L
        for (i in 0 until n) {
            val on = i < index || (i == index)
            val fill = if (i < index) 1f else if (i == index) fade(t, 200f, 900f) else 0f
            g.strokeCircle(x0 + i * gap + 9f, 196f, 9f, 2.5f, pal.accent.withAlpha(if (on) 0.9f else 0.35f))
            if (fill > 0f) g.fillCircle(x0 + i * gap + 9f, 196f, 9f * fill, pal.accent)
        }
    }

    private fun kicker(g: Gfx, sc: Scene, t: Float, y: Float) {
        val txt = (sc.kicker ?: return).uppercase()
        val a = fade(t, 100f)
        g.withPose(Pose(alpha = a, dx = (1 - a) * -30f), L, y) { g.drawTracked(txt, L, y, caps, pal.accent, 5f) }
    }

    /** Titre condensé en majuscules, aligné à gauche, mots mis en valeur en couleur d'accent, montée ligne par ligne. */
    private fun headline(g: Gfx, text: String, key: String, size: Float, top: Float, lines: Int, t: Float, delay: Float): Float {
        val marked = Marked.parse(text.uppercase())
        val f = head.withSize(size)
        val b = cache.fit(g, key, marked.plain, f, W, lines, 1.0f)
        val runs = MarkedText.runs(b, marked)
        for (i in b.lines.indices) {
            val a = fade(t, delay + i * 140f)
            val base = g.baselineOf(b, top, i)
            g.withPose(Pose(alpha = a, dx = (1 - a) * -60f), L, base) {
                MarkedText.drawLine(g, runs[i], L, base, b.font, pal.ink, pal.accent, b.font)
            }
        }
        return top + b.height
    }

    private fun para(g: Gfx, key: String, text: String, font: FontSpec, top: Float, w: Float, lines: Int, color: Int, t: Float, delay: Float, lh: Float = 1.38f): TextBlock {
        val b = cache.fit(g, key, Marked.strip(text), font, w, lines, lh)
        val a = fade(t, delay, 900f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 22f), L, top) { g.drawBlock(b, L, top, color) }
        return b
    }

    private fun bar(g: Gfx, y: Float, t: Float, delay: Float) {
        val a = Easing.EASE_IN_OUT.ease(window(t, delay, 800f))
        g.fillRect(L, y, L + 140f * a, y + 8f, pal.accent)
    }

    // ───────────────────────────── Scènes ─────────────────────────────

    private fun hero(g: Gfx, sc: Scene, t: Float) {
        var k = 1f
        var endY: Float
        while (true) {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(190f * k), W, 6, 1.0f)
            val sb2 = sc.subtitle?.let { cache.fit(g, "${sc.id}/s@$k", Marked.strip(it), sans.withSize(42f), W - 40f, 6, 1.38f) }
            endY = 470f + tb.height + 70f + (sb2?.height?.plus(30f) ?: 0f)
            if (endY <= bottomLimit - 60f || k <= 0.55f) break
            k -= 0.06f
        }
        val dy = ((bottomLimit - endY).coerceAtLeast(0f) * 0.4f).coerceAtMost(200f)
        g.save(); g.translate(0f, dy)
        kicker(g, sc, t, 330f)
        var y = headline(g, sc.title, "${sc.id}/h@$k", 190f * k, 470f, 6, t, 300f) + 36f
        bar(g, y, t, 900f)
        y += 50f
        sc.subtitle?.let { para(g, "${sc.id}/s@$k", it, sans.withSize(42f), y, W - 40f, 6, pal.muted, t, 1200f) }
        g.restore()
    }

    private fun definition(g: Gfx, sc: Scene, t: Float) {
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        var k = 1f
        var endY: Float
        while (true) {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(130f * k), W, 3, 1.0f)
            val bb = cache.fit(g, "${sc.id}/b@$k", body, sans.withSize(54f * k), W - 20f, 14, 1.4f)
            endY = 470f + tb.height + 80f + bb.height
            if (endY <= bottomLimit || k <= 0.6f) break
            k -= 0.06f
        }
        g.save(); g.translate(0f, ((bottomLimit - endY).coerceAtLeast(0f) * 0.4f).coerceAtMost(200f))
        kicker(g, sc, t, 330f)
        var y = headline(g, sc.title, "${sc.id}/h@$k", 130f * k, 400f, 3, t, 250f) + 34f
        bar(g, y, t, 800f)
        y += 54f
        if (body.isNotEmpty()) para(g, "${sc.id}/b@$k", body, sans.withSize(54f * k), y, W - 20f, 14, pal.ink, t, 1000f, 1.4f)
        g.restore()
    }

    private fun steps(g: Gfx, sc: Scene, t: Float) {
        val numW = 150f
        val textX = L + numW + 20f
        val textW = R - textX
        class Row(val top: Float, val label: TextBlock, val detail: TextBlock?, val h: Float)
        fun layout(k: Float): Pair<Float, List<Row>> {
            val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(120f * k), W, 4, 1.0f)
            val out = ArrayList<Row>(); var y = 340f + tb.height + 56f
            for ((i, item) in sc.items.withIndex()) {
                val (label, detail) = splitLabel(Marked.strip(item))
                val lb = cache.fit(g, "${sc.id}/l$i@$k", label, sansB.withSize(48f * k), textW, 4, 1.18f)
                val db = detail?.let { cache.fit(g, "${sc.id}/d$i@$k", it, sans.withSize(36f * k), textW, 5, 1.32f) }
                val h = maxOf(100f * k, lb.height + (db?.height?.plus(8f) ?: 0f))
                out += Row(y, lb, db, h)
                y += h + 50f * k
            }
            return tb.height to out
        }
        val ks = listOf(1f, 0.9f, 0.8f, 0.72f, 0.65f, 0.6f)
        val ch = ks.firstOrNull { k -> layout(k).second.let { r -> r.isEmpty() || r.last().top + r.last().h <= bottomLimit } } ?: ks.last()
        val rows = layout(ch).second
        kicker(g, sc, t, 290f)
        headline(g, sc.title, "${sc.id}/h@$ch", 120f * ch, 340f, 4, t, 200f)
        for ((i, r) in rows.withIndex()) {
            val d = 700f + i * 600f
            val a = fade(t, d, 800f)
            g.withPose(Pose(alpha = a, dx = (1 - a) * -80f), L, r.top) {
                val nf = head.withSize(130f * ch)
                val num = (i + 1).toString().padStart(2, '0')
                g.drawText(num, L, r.top + g.metrics(nf).ascent, nf, pal.accent)
                g.fillRect(textX - 14f, r.top + 6f, textX - 10f, r.top + r.h, pal.accent.withAlpha(0.5f))
                g.drawBlock(r.label, textX, r.top, pal.ink)
                r.detail?.let { g.drawBlock(it, textX, r.top + r.label.height + 8f, pal.muted) }
            }
        }
    }

    private fun stat(g: Gfx, sc: Scene, t: Float) {
        val (num, unit) = statParts(sc.statValue ?: "")
        val f = fitSize(g, num, head.withSize(520f), W)
        val a = fade(t, 300f, 900f)
        val base = 900f
        g.withPose(Pose(alpha = a, scale = lerp(0.9f, 1f, a)), L, base - 150f) { g.drawText(num, L, base, f, pal.accent) }
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit.uppercase(), head.withSize(120f), W)
            g.withPose(Pose(alpha = fade(t, 800f)), L, base + 120f) { g.drawText(unit.uppercase(), L, base + 140f, uf, pal.ink) }
        }
        headline(g, sc.title, "${sc.id}/h", 70f, base + 230f, 4, t, 1000f)
    }

    private fun compare(g: Gfx, sc: Scene, t: Float) {
        val cmp = sc.compare ?: return hero(g, sc, t)
        var y = headline(g, sc.title, "${sc.id}/h", 110f, 340f, 4, t, 200f) + 60f
        fun block(label: String, value: String, top: Float, d: Float, color: Int, key: String): Float {
            g.withPose(Pose(alpha = fade(t, d)), L, top) { g.drawTracked(label.uppercase(), L, top + 28f, caps, color, 5f) }
            val b = para(g, key, value, sansB.withSize(50f), top + 64f, W, 6, pal.ink, t, d + 150f, 1.25f)
            return top + 64f + b.height
        }
        y = block(cmp.badLabel, cmp.badText(), y, 700f, pal.muted, "${sc.id}/b") + 60f
        block(cmp.goodLabel, cmp.goodText(), y, 1900f, pal.accent, "${sc.id}/g")
    }

    private fun checklist(g: Gfx, sc: Scene, t: Float) {
        val blocks = sc.items.mapIndexed { i, it -> cache.fit(g, "${sc.id}/i$i", Marked.strip(it), sansB.withSize(52f), W - 90f, 5, 1.22f) }
        var y = headline(g, sc.title, "${sc.id}/h", 130f, 340f, 4, t, 200f) + 56f
        for ((i, b) in blocks.withIndex()) {
            val d = 700f + i * 600f
            val a = fade(t, d, 800f)
            g.withPose(Pose(alpha = a, dx = (1 - a) * -60f), L, y) {
                g.fillRoundRect(L, y + 6f, L + 56f, y + 62f, 14f, pal.accent)
                val s = Easing.EASE_IN_OUT.ease(window(t, d + 250f, 500f))
                if (s > 0f) {
                    g.line(L + 14f, y + 36f, L + 25f, y + 48f, 7f, pal.bg)
                    g.line(L + 25f, y + 48f, L + 14f + 28f * s, y + 48f - 26f * s, 7f, pal.bg)
                }
                g.drawBlock(b, L + 90f, y, pal.ink)
            }
            y += maxOf(b.height, 68f) + 36f
        }
        if (sc.body.isNotEmpty()) para(g, "${sc.id}/body", sc.body, sans.withSize(44f), y + 10f, W, 3, pal.muted, t, 700f + sc.items.size * 600f + 200f)
    }
}
