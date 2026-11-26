// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Format « Explicatif simulé » : chaque idée est un chapitre numéroté.
 * - une carte de chapitre en haut (rubrique + titre), qui se transforme en carte claire « À FAIRE » ;
 * - au centre, une simulation d'interface qui montre l'idée (liste qui se coche, note qui s'écrit, chiffre qui monte…) ;
 * - une ligne de progression « 02 / 09 » ;
 * - en bas, l'explication en grosse typographie, par groupes de mots, le mot clé sur fond clair.
 * Le texte de l'auteur est toujours entier : il est dit dans les sous-titres et montré dans la simulation.
 */
internal class ExplainerLook(private val sb: Storyboard, private val variant: Int = 0) : LookRenderer {
    private class Th(val bg1: Int, val bg2: Int, val panel: Int, val card: Int, val cream: Int, val creamInk: Int, val gold: Int, val good: Int)

    private val kit = StyleKit.random(sb.seed)
    private val th = kit.palette.let { Th(it.bgTop, it.bgBottom, it.card, it.inner, it.highlight, it.onHighlight, it.accent, rgb(0x1FA971)) }
    private val white = kit.palette.ink
    private val dim = kit.palette.muted
    private val rnd = Rand.of(sb.seed, 0x3E7)
    /** Variantes de détail, tirées de la graine : légende, cadre, police des titres. */
    private val capStyle = (rnd.nextInt() ushr 1) % 3
    private val panelStyle = (rnd.nextInt() ushr 1) % 3
    private val titleFam = listOf(FontFamily.SERIF, FontFamily.CINEMA, FontFamily.DISPLAY)[(rnd.nextInt() ushr 1) % 3]
    // Géométrie selon la variante : A carte au-dessus, B carte sous la simulation, C simulation haute et légende en bandeau.
    private val cardY = when (variant) { 1 -> 1010f; 2 -> 300f; else -> 420f }
    private val panelDy = when (variant) { 1 -> -310f; 2 -> -120f; else -> 0f }
    private val capY = if (variant == 2) 1440f else 1470f
    private val bad = rgb(0xFF6B6B)
    override val ink = white
    override val muted = dim
    override val accent = th.gold
    override val accent2 = th.cream
    private val cache = TextCache()

    private val serif get() = FontSpec(titleFam, if (titleFam == FontFamily.DISPLAY) 800 else 400, 44f)
    private fun txt(w: Int, s: Float) = FontSpec(FontFamily.TEXT, w, s)

    private enum class Kind { COVER, DEFINE, STEP, VERSUS, STAT, CHECK, CTA }
    private data class Beat(
        val kind: Kind, val tag: String, val name: String, val caption: String,
        val main: String = "", val detail: String? = null, val list: List<String> = emptyList(), val index: Int = 0, val icon: IconId = IconId.BULB,
    )

    private fun plain(s: String?) = Marked.strip(s ?: "").replace(' ', ' ').trim()
    private val domain by lazy { DomainDetector.detect(TextSanitizer.clean(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body })) }
    private val astuceNo: Int? = (listOf(sb.title) + sb.scenes.take(1).flatMap { listOf(it.kicker ?: "", it.title) })
        .firstNotNullOfOrNull { Regex("astuce\\D{0,30}?0*(\\d{1,4})", RegexOption.IGNORE_CASE).find(plain(it))?.groupValues?.get(1)?.toIntOrNull() }

    private val totalSteps by lazy { sb.scenes.filter { it.layout == Layout.STEPS }.sumOf { it.items.size } }
    private val firstStep: Map<String, Int> by lazy {
        var n = 0
        sb.scenes.filter { it.layout == Layout.STEPS }.associate { sc -> val s0 = n; n += sc.items.size; sc.id to s0 }
    }
    private val beatsCache = HashMap<String, List<Beat>>()

    /** Nom court pour une carte ou une ligne de liste : début de la phrase, 26 caractères au plus. */
    private fun short(text: String): String {
        val c = plain(text).split(Regex(" : | \\(|, | — |\\. ")).first().trim().trimEnd('.', ':', '!')
        var out = ""
        for (w in c.split(' ')) { val n = (out + " " + w).trim(); if (n.length > 26) break; out = n }
        return out.ifEmpty { c.take(26) }.replaceFirstChar { it.uppercase() }
    }

    private fun split2(text: String): Pair<String, String> {
        val cut = plain(text).split(Regex(" : | \\(|, | — ")).first().trim().trimEnd('.', '!', ':')
        val w = cut.split(' ').filter { it.isNotBlank() }.take(8)
        if (w.size <= 2) return w.joinToString(" ") to ""
        val k = (w.size + 1) / 2
        return w.take(k).joinToString(" ") to w.drop(k).joinToString(" ")
    }

    private fun beats(sc: Scene): List<Beat> = beatsCache.getOrPut(sc.id) {
        val tag = (sc.kicker ?: "").uppercase()
        when (sc.layout) {
            Layout.HERO -> listOf(Beat(Kind.COVER, astuceNo?.let { "ASTUCE ${Fmt.pad(it, 3)}" } ?: "CODJIA", plain(sc.title), plain(sc.subtitle ?: sc.title),
                list = sb.scenes.filter { it.layout == Layout.STEPS }.flatMap { it.items }.map { short(splitLabel(plain(it)).first) }.filter { it.length >= 3 }.take(6), icon = sc.iconOrDefault()))
            Layout.DEFINITION -> listOf(Beat(Kind.DEFINE, "LE PRINCIPE", short(sc.title), plain(sc.body.ifEmpty { sc.subtitle }), main = plain(sc.body.ifEmpty { sc.subtitle })))
            Layout.STEPS -> {
                val word = if (domain == Domain.CYBER) "DÉFENSE" else "ÉTAPE"
                val labels = sc.items.map { short(splitLabel(plain(it)).first) }
                val n0 = firstStep[sc.id] ?: 0
                val total = totalSteps.coerceAtLeast(sc.items.size)
                sc.items.mapIndexed { i, it ->
                    val (label, detail) = splitLabel(plain(it))
                    Beat(Kind.STEP, "$word ${n0 + i + 1}/$total", short(label), plain(it), main = label, detail = detail, list = labels, index = i)
                }
            }
            Layout.STAT -> listOf(Beat(Kind.STAT, tag.ifEmpty { "LE CHIFFRE" }, short(sc.title), plain(sc.title) + (if (sc.body.isNotEmpty()) ". " + plain(sc.body) else ""), main = sc.statValue ?: ""))
            Layout.COMPARE -> sc.compare?.let { c ->
                listOf(Beat(Kind.VERSUS, "À ÉVITER", short(c.badText()), "${plain(c.badLabel)} : ${c.badText()}. ${plain(c.goodLabel)} : ${c.goodText()}.",
                    main = c.badText(), detail = c.goodText(), list = listOf(short(c.goodText()))))
            } ?: emptyList()
            Layout.CHECKLIST -> listOfNotNull(
                if (sc.items.isEmpty()) null else Beat(Kind.CHECK, tag.ifEmpty { "À RETENIR" }, short(sc.title).let { n -> if (n.equals(tag.ifEmpty { "À RETENIR" }, ignoreCase = true)) "L'essentiel" else n }, plain(sc.subtitle).ifEmpty { "${sc.items.size} réflexes simples à appliquer dès aujourd'hui." }, list = sc.items.map(::plain)),
                Beat(Kind.CTA, "CODJIA", "La suite", astuceNo?.let { "Abonne-toi pour l'astuce ${Fmt.pad(it + 1, 3)} !" } ?: plain(sc.body).ifEmpty { "Abonne-toi pour la suite !" }, main = plain(sc.body)),
            )
            Layout.SCREEN -> emptyList()
        }
    }

    private val offsets: Map<String, Int> by lazy { var n = 0; sb.scenes.associate { it.id to n.also { _ -> n += beats(it).size } } }
    private val totalBeats: Int by lazy { sb.scenes.sumOf { beats(it).size }.coerceAtLeast(1) }

    private fun beatAt(sc: Scene, t: Float): Triple<Int, Float, Float> {
        val bs = beats(sc)
        val w = bs.map { 0.7f + 0.3f * (it.caption.length.coerceIn(30, 200) / 100f) - (if (it.kind == Kind.CTA) 0.3f else 0f) }
        val total = w.sum()
        var acc = 0f
        for ((i, x) in w.withIndex()) {
            val len = sc.durationMs * x / total
            if (t < acc + len || i == bs.lastIndex) return Triple(i, (t - acc).coerceAtLeast(0f), len)
            acc += len
        }
        return Triple(0, t, sc.durationMs.toFloat())
    }

    private fun pop(t: Float, delay: Float = 0f, dur: Float = 420f) = Easing.EXPO_OUT.ease(clamp01((t - delay) / dur))

    // ───────────────────────────── Scène ─────────────────────────────

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        KitDecor.background(g, kit, sb.seed, tGlobalMs, corners = false)
        val bs = beats(sc)
        if (bs.isEmpty()) return
        val (i, t, len) = beatAt(sc, localMs)
        val b = bs[i]
        val key = "${sc.id}/$i"
        when (b.kind) {
            Kind.COVER -> cover(g, b, t, len, key)
            Kind.DEFINE -> { card(g, b.tag, b.name, t, key); panel(g, t, "En clair") { define(g, b, t, len, key) } }
            Kind.STEP -> { card(g, b.tag, b.name, t, key, answer = pop(t, len * 0.45f)); panel(g, t, "Pas à pas") { step(g, b, t, len, key) } }
            Kind.VERSUS -> { card(g, b.tag, b.name, t, key, strike = pop(t, len * 0.3f), answer = pop(t, len * 0.5f), answerTag = "À FAIRE", answerName = b.list.firstOrNull() ?: "La bonne façon"); panel(g, t, "Comparons") { versus(g, b, t, len, key) } }
            Kind.STAT -> { card(g, b.tag, b.name, t, key); panel(g, t, "En chiffres") { stat(g, b, t, len) } }
            Kind.CHECK -> { card(g, b.tag, b.name, t, key); panel(g, t, "Ta liste") { check(g, b, t, len, key) } }
            Kind.CTA -> { card(g, b.tag, b.name, t, key); panel(g, t, "") { cta(g, t, len) } }
        }
        progress(g, offsets.getValue(sc.id) + i, t)
        caption(g, b.caption, t, len, key)
    }

    // ───────────────────────────── Éléments communs ─────────────────────────────

    private fun card(g: Gfx, tag: String, name: String, t: Float, key: String, strike: Float = 0f, answer: Float = -1f, answerTag: String = "À FAIRE", answerName: String = "Appliqué") {
        val a = pop(t)
        val y = cardY; val h = 150f
        val tf = txt(800, 22f)
        val nf = fitSize(g, name, txt(800, 46f), 400f)
        val w = (maxOf(g.measureTracked(tag, tf, 2.4f), g.measureText(name, nf)) + 64f).coerceIn(280f, 520f)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 24f, scale = 0.96f + 0.04f * a), Safe.LEFT, y) {
            g.fillRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + h, 28f, th.card)
            g.strokeRoundRect(Safe.LEFT, y, Safe.LEFT + w, y + h, 28f, 1.5f, 0x22FFFFFF)
            g.drawTracked(tag, Safe.LEFT + 32f, y + 46f, tf, th.gold, 2.4f)
            val base = y + 108f
            val col = if (strike > 0f) mix(white, dim, strike) else white
            g.drawText(name, Safe.LEFT + 32f, base, nf, col)
            if (strike > 0f) g.line(Safe.LEFT + 28f, base - nf.size * 0.32f, Safe.LEFT + 28f + (g.measureText(name, nf) + 8f) * strike, base - nf.size * 0.32f, 4f, bad)
        }
        if (answer < 0f) return
        val ax = Safe.LEFT + w + 30f
        val ex = Safe.RIGHT
        // flèche
        val arrowA = pop(t, 0f, 300f)
        g.line(ax, y + h / 2, ax + 56f, y + h / 2, 5f, white.withAlpha(arrowA))
        g.line(ax + 38f, y + h / 2 - 18f, ax + 58f, y + h / 2, 5f, white.withAlpha(arrowA))
        g.line(ax + 38f, y + h / 2 + 18f, ax + 58f, y + h / 2, 5f, white.withAlpha(arrowA))
        val bx = ax + 84f
        if (bx + 220f > ex) return
        if (answer < 0.02f) {
            g.strokeRoundRect(bx, y, minOf(ex, bx + 300f), y + h, 28f, 3f, dim.withAlpha(0.7f))
            val q = txt(800, 64f); g.drawText("?", (bx + minOf(ex, bx + 300f)) / 2 - g.measureText("?", q) / 2, y + h / 2 + g.metrics(q).ascent / 2, q, dim)
        } else {
            val af = fitSize(g, answerName, txt(800, 46f), ex - bx - 90f)
            val bw = (maxOf(g.measureTracked(answerTag, tf, 2.4f), g.measureText(answerName, af)) + 100f).coerceAtMost(ex - bx)
            g.withPose(Pose(alpha = answer, scale = 0.85f + 0.15f * Easing.BACK_OUT.ease(answer)), bx + bw / 2, y + h / 2) {
                g.fillRoundRect(bx, y, bx + bw, y + h, 28f, th.cream)
                g.drawTracked(answerTag, bx + 30f, y + 46f, tf, th.creamInk.withAlpha(0.7f), 2.4f)
                g.drawText(answerName, bx + 30f, y + 108f, af, th.creamInk)
                g.fillCircle(bx + bw - 44f, y + h - 44f, 18f, th.good)
                Icons.draw(g, IconId.CHECK, bx + bw - 44f, y + h - 44f, 22f, rgb(0xFFFFFF), 3f)
            }
        }
    }

    private fun panel(g: Gfx, t: Float, title: String, body: () -> Unit) {
        val a = pop(t, 80f, 520f)
        val top = 640f; val bottom = 1220f
        g.withPose(Pose(alpha = a, dy = (1 - a) * 34f + panelDy), 540f, top) {
            when (panelStyle) {
                1 -> { g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, bottom, 36f, th.panel.withAlpha(0.8f)); g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, bottom, 36f, 2f, 0x44FFFFFF) }
                2 -> { g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, bottom, 36f, th.panel); g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, bottom, 36f, 4f, th.gold.withAlpha(0.8f)) }
                else -> {
                    g.fillRoundRect(Safe.LEFT + 4f, top + 10f, Safe.RIGHT + 4f, bottom + 10f, 36f, 0x33000000)
                    g.fillRoundRect(Safe.LEFT, top, Safe.RIGHT, bottom, 36f, th.panel)
                    g.strokeRoundRect(Safe.LEFT, top, Safe.RIGHT, bottom, 36f, 1.5f, 0x24FFFFFF)
                }
            }
            if (title.isNotEmpty()) g.drawText(title, Safe.LEFT + 44f, top + 86f, serif, white)
            body()
        }
    }

    private fun progress(g: Gfx, idx: Int, t: Float) {
        val y = 1272f
        val a = pop(t, 200f)
        val f = txt(600, 24f)
        g.drawText("${Fmt.pad(idx + 1, 2)} / ${Fmt.pad(totalBeats, 2)}", Safe.LEFT, y + 8f, f, th.gold.withAlpha(a))
        val n = minOf(totalBeats, 12)
        val cur = if (totalBeats <= 12) idx else idx * 12 / totalBeats
        var x = Safe.LEFT + 150f
        for (k in 0 until n) {
            if (k == cur) { g.fillRoundRect(x, y - 6f, x + 30f, y + 6f, 6f, white.withAlpha(a)); x += 42f }
            else { g.fillCircle(x + 5f, y, 5f, if (k < cur) white.withAlpha(0.7f * a) else dim.withAlpha(0.45f * a)); x += 22f }
        }
        val pf = txt(700, 22f)
        val pw = g.measureText("Exemple", pf) + 40f
        g.strokeRoundRect(Safe.RIGHT - pw, y - 20f, Safe.RIGHT, y + 22f, 21f, 2f, th.gold.withAlpha(0.8f * a))
        g.drawText("Exemple", Safe.RIGHT - pw + 20f, y + 8f, pf, th.gold.withAlpha(a))
    }

    // ───────────────────────────── Sous-titres par groupes de mots ─────────────────────────────

    private fun chunks(text: String): List<List<String>> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val out = ArrayList<List<String>>()
        var cur = ArrayList<String>()
        for (w in words) {
            cur.add(w)
            val brk = w.endsWith(".") || w.endsWith("!") || w.endsWith("?") || w.endsWith(":") || w.endsWith(";") || w.endsWith(",")
            if (cur.size >= 4 || (brk && cur.size >= 2)) { out += cur; cur = ArrayList() }
        }
        if (cur.isNotEmpty()) { if (cur.size == 1 && out.isNotEmpty()) out[out.lastIndex] = out.last() + cur else out += cur }
        return out
    }

    private val chunkCache = HashMap<String, List<List<String>>>()

    private fun keyWord(c: List<String>): Int {
        if (c.size < 2) return -1
        c.indexOfFirst { w -> w.any { it.isDigit() } }.takeIf { it >= 0 }?.let { return it }
        c.indexOfFirst { w -> w.trim('.', ',', '!', '?', ':', ';', '(', ')').let { x -> x.length in 2..8 && x.count { ch -> ch.isUpperCase() } >= 2 } }.takeIf { it >= 0 }?.let { return it }
        val cand = c.withIndex().filter { (_, w) -> w.trim('.', ',', '!', '?', ':', ';', '«', '»').length >= 7 }
        return cand.maxByOrNull { it.value.length }?.index ?: -1
    }

    private fun caption(g: Gfx, text: String, t: Float, len: Float, key: String) {
        if (text.isBlank()) return
        val cs = chunkCache.getOrPut(key) { chunks(text) }
        if (cs.isEmpty()) return
        val weights = cs.map { it.sumOf { w -> w.length + 1 }.toFloat() }
        val total = weights.sum()
        val span = len * 0.94f
        var acc = 0f
        var idx = cs.lastIndex
        var local = 0f
        for ((i, w) in weights.withIndex()) {
            val l = span * w / total
            if (t < acc + l) { idx = i; local = t - acc; break }
            acc += l
            if (i == cs.lastIndex) local = t - acc + l
        }
        val c = cs[idx]
        val hl = keyWord(c)
        var size = 66f
        var f = txt(800, size)
        var lines: List<List<String>> = listOf(c)
        while (true) {
            f = txt(800, size)
            val sp = g.measureText(" ", f)
            fun ww(w: String) = g.measureText(w, f) + (if (hl >= 0 && w === c[hl]) 36f else 0f)
            fun lw(l: List<String>) = l.sumOf { ww(it).toDouble() }.toFloat() + sp * (l.size - 1)
            lines = listOf(c)
            if (lw(c) > Safe.WIDTH - 20f && c.size > 1) { val k = (c.size + 1) / 2; lines = listOf(c.take(k), c.drop(k)) }
            if (lines.all { lw(it) <= Safe.WIDTH - 20f } || size <= 30f) break
            size -= 4f
        }
        val spaceW = g.measureText(" ", f)
        fun wordW(w: String) = g.measureText(w, f) + (if (hl >= 0 && w === c[hl]) 36f else 0f)
        val a = Easing.EXPO_OUT.ease(clamp01(local / 220f))
        val lh = size * 1.45f
        if (variant == 2) g.fillRoundRect(Safe.LEFT - 20f, capY - lines.size * lh / 2f - 20f, Safe.RIGHT + 20f, capY + lines.size * lh / 2f + 20f, 30f, th.panel.withAlpha(0.88f))
        val top = capY - (lines.size * lh) / 2f
        g.withPose(Pose(alpha = a, dy = (1 - a) * 18f, scale = 0.94f + 0.06f * a), 540f, capY) {
            for ((li, ln) in lines.withIndex()) {
                val lw = ln.sumOf { wordW(it).toDouble() }.toFloat() + spaceW * (ln.size - 1)
                var x = 540f - lw / 2f
                val base = top + li * lh + size
                for (w in ln) {
                    val isHl = hl >= 0 && w === c[hl]
                    val ww = g.measureText(w, f)
                    if (isHl && capStyle == 1) {
                        g.drawText(w, x + 18f, base, f, th.gold)
                        x += ww + 36f + spaceW
                    } else if (isHl && capStyle == 2) {
                        g.drawText(w, x + 18f, base, f, white)
                        g.fillRoundRect(x + 18f, base + 8f, x + 18f + ww, base + 18f, 5f, th.gold)
                        x += ww + 36f + spaceW
                    } else if (isHl) {
                        g.fillRoundRect(x, base - size, x + ww + 36f, base + size * 0.33f, 16f, th.cream)
                        g.drawText(w, x + 18f, base, f, th.creamInk)
                        x += ww + 36f + spaceW
                    } else { g.drawText(w, x, base, f, white); x += ww + spaceW }
                }
            }
        }
    }

    // ───────────────────────────── Couverture ─────────────────────────────

    private fun cover(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val (l1, l2) = split2(b.name)
        val a = pop(t)
        g.drawTracked(b.tag, Safe.LEFT, 330f, txt(800, 26f), th.gold, 3f)
        val f1 = fitSize(g, l1, FontSpec(FontFamily.SERIF, 400, 112f), Safe.WIDTH)
        g.withPose(Pose(alpha = a, dy = (1 - a) * 30f), 540f, 420f) {
            g.drawText(l1, 540f - g.measureText(l1, f1) / 2, 450f, f1, white)
        }
        if (l2.isNotEmpty()) {
            val f2 = fitSize(g, l2, FontSpec(FontFamily.CINEMA_ITALIC, 400, 104f), Safe.WIDTH)
            val a2 = pop(t, 160f)
            g.withPose(Pose(alpha = a2, dy = (1 - a2) * 30f), 540f, 560f) { g.drawText(l2, 540f - g.measureText(l2, f2) / 2, 570f, f2, dim) }
        }
        if (b.list.size < 2) {
            val a3 = pop(t, 300f)
            g.withPose(Pose(alpha = a3, scale = 0.8f + 0.2f * a3), 540f, 930f) {
                for (r in 0..2) g.strokeCircle(540f, 930f, 120f + r * 70f, 2f, th.gold.withAlpha(0.28f - r * 0.07f))
                g.fillCircle(540f, 930f, 100f, th.gold.withAlpha(0.2f))
                Icons.draw(g, b.icon, 540f, 930f, 110f, th.gold, 4f)
            }
            return
        }
        val tiles = b.list
        val cols = 3
        val tw = (Safe.WIDTH - 2 * 22f) / cols
        val th0 = 140f
        val swap = clamp01((t - len * 0.5f) / 500f)
        for ((i, label) in tiles.withIndex()) {
            val r = i / cols; val c = i % cols
            val x = Safe.LEFT + c * (tw + 22f); val y = 700f + r * (th0 + 22f)
            val ta = pop(t, 250f + i * 90f)
            val flip = clamp01(swap * 1.6f - i * 0.06f)
            g.withPose(Pose(alpha = ta, dy = (1 - ta) * 28f, scale = 0.9f + 0.1f * ta), x + tw / 2, y + th0 / 2) {
                g.fillRoundRect(x, y, x + tw, y + th0, 24f, mix(th.card, th.cream, flip))
                val lf = fitSize(g, label, txt(800, 32f), tw - 40f)
                g.drawText(label, x + 22f, y + 62f, lf, mix(white, th.creamInk, flip))
                val bf = txt(700, 20f)
                val txtB = if (flip > 0.5f) "✓ ok" else "à voir"
                val bw = g.measureText(txtB, bf) + 28f
                g.fillRoundRect(x + 22f, y + 84f, x + 22f + bw, y + 118f, 17f, if (flip > 0.5f) th.good.withAlpha(0.9f) else 0x22FFFFFF)
                g.drawText(txtB, x + 36f, y + 108f, bf, if (flip > 0.5f) rgb(0xFFFFFF) else dim)
            }
        }
    }

    // ───────────────────────────── Simulations dans le cadre ─────────────────────────────

    private fun define(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val x0 = Safe.LEFT + 40f; val x1 = Safe.RIGHT - 40f
        val bl = cache.fit(g, "d/$key", b.main, txt(600, 42f), x1 - x0 - 60f, 7, 1.32f)
        g.fillRoundRect(x0, 760f, x1, 760f + maxOf(260f, bl.height + 60f).coerceAtMost(420f), 24f, th.card)
        val reveal = clamp01((t - 250f) / (len * 0.7f))
        g.drawBlock(bl, x0 + 30f, 790f, white, reveal = reveal, caretColor = th.gold, caretVisible = ((t / 450f).toInt() % 2 == 0))
    }

    private fun step(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val x0 = Safe.LEFT + 40f; val x1 = Safe.RIGHT - 40f
        val side = b.list.size >= 2
        var rx = x0
        if (side) {
            val sw = 300f
            g.fillRoundRect(x0, 760f, x0 + sw, 1180f, 24f, th.card)
            val rows = b.list.take(6)
            val rh = minOf(78f, 380f / rows.size)
            for ((i, l) in rows.withIndex()) {
                val y = 780f + i * rh
                val cur = i == b.index
                if (cur) g.fillRoundRect(x0 + 10f, y, x0 + sw - 10f, y + rh - 8f, 14f, 0x30FFFFFF)
                val done = i < b.index || (cur && t > len * 0.5f)
                g.strokeRoundRect(x0 + 24f, y + rh / 2 - 15f, x0 + 54f, y + rh / 2 + 15f, 6f, 2.5f, if (done) th.good else dim)
                if (done) Icons.draw(g, IconId.CHECK, x0 + 39f, y + rh / 2, 22f, th.good, 3f)
                val lf = fitSize(g, l, txt(if (cur) 800 else 600, 26f), sw - 100f)
                g.drawText(l, x0 + 68f, y + rh / 2 + g.metrics(lf).ascent / 2, lf, if (cur) white else dim)
            }
            rx = x0 + sw + 30f
        }
        g.fillRoundRect(rx, 760f, x1, 1180f, 24f, th.card)
        val head = txt(800, 22f)
        g.drawTracked(if (b.detail != null) "POURQUOI" else "À RETENIR", rx + 30f, 810f, head, th.gold, 2.4f)
        val body = b.detail ?: b.main
        val bl = cache.fit(g, "s/$key", body, txt(600, 40f), x1 - rx - 60f, 7, 1.32f)
        g.drawBlock(bl, rx + 30f, 836f, white, reveal = clamp01((t - 200f) / (len * 0.65f)), caretColor = th.gold, caretVisible = ((t / 450f).toInt() % 2 == 0))
    }

    private fun versus(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val x0 = Safe.LEFT + 40f; val x1 = Safe.RIGHT - 40f
        fun row(y: Float, col: Int, mark: IconId, text: String, a: Float, k: String) {
            g.withPose(Pose(alpha = a, dy = (1 - a) * 24f), 540f, y) {
                g.fillRoundRect(x0, y, x1, y + 180f, 24f, th.card)
                g.fillCircle(x0 + 60f, y + 90f, 28f, col.withAlpha(0.2f))
                Icons.draw(g, mark, x0 + 60f, y + 90f, 30f, col, 4f)
                val bl = cache.fit(g, k, text, txt(700, 34f), x1 - x0 - 190f, 3, 1.28f)
                g.drawBlock(bl, x0 + 118f, y + 90f - bl.height / 2f, white)
            }
        }
        row(770f, bad, IconId.CROSS, b.main, pop(t, 200f), "v1/$key")
        row(980f, th.good, IconId.CHECK, b.detail ?: "", pop(t, len * 0.45f), "v2/$key")
    }

    private fun stat(g: Gfx, b: Beat, t: Float, len: Float) {
        val (v, unit) = statParts(b.main)
        val digits = v.filter { it.isDigit() }
        val target = digits.toLongOrNull()
        val numeric = target != null && v.all { it.isDigit() || it == ' ' || it == ' ' || it == '.' || it == ',' } && digits.length <= 9
        val shown = if (numeric) {
            val k = Easing.EXPO_OUT.ease(clamp01((t - 150f) / 1100f))
            group((target!! * k).toLong())
        } else v
        val f = fitSize(g, shown, FontSpec(FontFamily.SERIF, 400, 190f), Safe.WIDTH - 100f)
        g.drawText(shown, 540f - g.measureText(shown, f) / 2, 1000f, f, white)
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit, txt(700, 42f), Safe.WIDTH - 100f)
            g.drawText(unit, 540f - g.measureText(unit, uf) / 2, 1090f, uf, th.gold)
        }
        val bar = pop(t, 200f, 900f)
        g.fillRoundRect(Safe.LEFT + 120f, 1140f, Safe.RIGHT - 120f, 1152f, 6f, 0x22FFFFFF)
        g.fillRoundRect(Safe.LEFT + 120f, 1140f, Safe.LEFT + 120f + (Safe.WIDTH - 240f) * bar, 1152f, 6f, th.gold)
    }

    private fun group(n: Long): String {
        val s = n.toString(); val sb2 = StringBuilder()
        for ((i, c) in s.withIndex()) { if (i > 0 && (s.length - i) % 3 == 0) sb2.append(' '); sb2.append(c) }
        return sb2.toString()
    }

    private fun check(g: Gfx, b: Beat, t: Float, len: Float, key: String) {
        val x0 = Safe.LEFT + 40f; val x1 = Safe.RIGHT - 40f
        val items = b.list.take(6)
        val rh = minOf(96f, 410f / maxOf(1, items.size))
        for ((i, it) in items.withIndex()) {
            val y = 770f + i * rh
            val ap = pop(t, 150f + i * 120f)
            val tick = clamp01((t - (350f + i * (len * 0.7f / items.size))) / 260f)
            g.withPose(Pose(alpha = ap, dx = (1 - ap) * 40f), 540f, y) {
                g.fillRoundRect(x0, y, x1, y + rh - 12f, 20f, th.card)
                g.strokeRoundRect(x0 + 24f, y + rh / 2 - 6f - 17f, x0 + 58f, y + rh / 2 - 6f + 17f, 8f, 3f, if (tick > 0.5f) th.good else dim)
                if (tick > 0f) Icons.draw(g, IconId.CHECK, x0 + 41f, y + rh / 2 - 6f, 26f * tick, th.good, 3.5f)
                val lf = fitSize(g, it, txt(700, 32f), x1 - x0 - 120f)
                g.drawText(it, x0 + 84f, y + rh / 2 - 6f + g.metrics(lf).ascent / 2, lf, if (tick > 0.5f) white else dim)
            }
        }
    }

    private fun cta(g: Gfx, t: Float, len: Float) {
        val a = pop(t, 100f)
        val cy = 930f
        val press = clamp01((t - len * 0.55f) / 200f) * (1f - clamp01((t - len * 0.55f - 200f) / 200f))
        val bw = 520f; val bh = 130f
        g.withPose(Pose(alpha = a, scale = (0.9f + 0.1f * a) * (1f - 0.06f * press)), 540f, cy) {
            val done = t > len * 0.6f
            g.fillRoundRect(540f - bw / 2, cy - bh / 2, 540f + bw / 2, cy + bh / 2, 40f, if (done) th.good else th.cream)
            val lab = if (done) "Abonné ✓" else "S'abonner"
            val f = txt(800, 48f)
            g.drawText(lab, 540f - g.measureText(lab, f) / 2, cy + g.metrics(f).ascent / 2, f, if (done) rgb(0xFFFFFF) else th.creamInk)
        }
        // curseur qui vient cliquer
        val k = Easing.EXPO_OUT.ease(clamp01((t - 200f) / (len * 0.4f)))
        val px = lerp(820f, 640f, k); val py = lerp(1130f, cy + 30f, k)
        val cur = PathData().moveTo(0f, 0f).lineTo(0f, 46f).lineTo(12f, 35f).lineTo(21f, 54f).lineTo(30f, 50f).lineTo(21f, 31f).lineTo(36f, 31f).close()
        g.fillPath(cur.transformed(1f, px, py), white)
        g.strokePath(cur.transformed(1f, px, py), 2.5f, th.bg1)
    }
}
