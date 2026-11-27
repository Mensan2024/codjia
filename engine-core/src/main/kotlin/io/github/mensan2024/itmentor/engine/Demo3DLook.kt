// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.sin

/**
 * Format « Démonstration 3D » : un objet en volume (ordinateur portable, baie de serveurs ou cadenas, choisi d'après le
 * sujet) tourne doucement au-dessus du texte ; selon les scènes il reste assemblé ou s'éclate en pièces avec des repères.
 * 3D stylisée dessinée par le moteur ([Render3D]) : c'est une illustration, jamais une représentation exacte, et elle est
 * étiquetée comme telle. Le texte de l'auteur est intégralement affiché sous la scène, dans l'ordre.
 * Si le texte d'une scène est trop long pour tenir sous l'objet, l'objet est retiré de cette scène plutôt que de couper le texte.
 */
internal class Demo3DLook(private val sb: Storyboard) : LookRenderer {
    private class Palette(val bgTop: Int, val bgBottom: Int, val ink: Int, val muted: Int, val accent: Int, val pal: Models3D.Pal)
    private val palettes = listOf(
        Palette(rgb(0x0A1020), rgb(0x14213D), rgb(0xF2F6FF), rgb(0xA9B6CE), rgb(0x37C6F4),
            Models3D.Pal(rgb(0xC9D2E0), rgb(0x2A3347), rgb(0x37C6F4), rgb(0x0B1730), rgb(0x6BE0FF))),
        Palette(rgb(0x120B24), rgb(0x24104A), rgb(0xF6F1FF), rgb(0xB5A9D4), rgb(0xB07CFF),
            Models3D.Pal(rgb(0xD6D0EA), rgb(0x2E2547), rgb(0xB07CFF), rgb(0x140C2E), rgb(0xFF7AD9))),
        Palette(rgb(0x0E1013), rgb(0x1F2329), rgb(0xFFF8EE), rgb(0xB4B0A8), rgb(0xFF9A3D),
            Models3D.Pal(rgb(0xD9D9D6), rgb(0x30343B), rgb(0xFF9A3D), rgb(0x10151C), rgb(0xFFC46B))),
    )
    private val pal = palettes[(Rand.of(sb.seed, 0x3D0B).nextInt() ushr 1) % palettes.size]
    override val ink = pal.ink
    override val muted = pal.muted
    override val accent = pal.accent
    override val accent2 = pal.pal.glow

    private val cache = TextCache()
    private val head = FontSpec(FontFamily.CONDENSED, 800, 150f)
    private val sans = FontSpec(FontFamily.TEXT, 500, 38f)
    private val sansB = FontSpec(FontFamily.TEXT, 700, 46f)
    private val caps = FontSpec(FontFamily.TEXT, 700, 26f)
    private val tag = FontSpec(FontFamily.TEXT, 700, 22f)
    private val L = Safe.LEFT + 10f
    private val R = Safe.RIGHT - 10f
    private val W = R - L
    private val bottomLimit = Safe.BOTTOM - 100f
    private val phase = (Rand.of(sb.seed, 0x3D0C).nextInt() ushr 1) % 628 / 100f

    private val kind: Models3D.Kind = run {
        val text = TextSanitizer.clean(sb.title + " " + sb.scenes.joinToString(" ") { it.title + " " + it.body + " " + it.items.joinToString(" ") }).lowercase()
        val domain = DomainDetector.detect(text)
        val net = Regex("serveur|vlan|switch|routeur|router|datacenter|data center|cloud|\\bdns\\b|ccna|ccnp|rÃ©seau|réseau|reseau|pare-?feu|firewall").containsMatchIn(text)
        val pc = Regex("ordinateur|\\bpc\\b|laptop|portable|windows|matériel|materiel|processeur|mémoire|memoire|clavier|écran|ecran").containsMatchIn(text)
        val sec = Regex("mot de passe|chiffr|cadenas|authentif|\\bmfa\\b|phishing|hameçon|hamecon|verrou|confidentiel|protég|proteg|malware|attaque|sécurité|securite").containsMatchIn(text)
        when {
            pc && !net -> Models3D.Kind.LAPTOP
            domain == Domain.CYBER || (sec && !net) -> Models3D.Kind.LOCK
            domain == Domain.RESEAUX || net -> Models3D.Kind.SERVER
            else -> Models3D.Kind.LAPTOP
        }
    }
    private val model: Model3D = Models3D.build(kind, pal.pal)

    private data class Plan(val stage: Boolean, val top: Float, val k: Float, val stageBottom: Float = 0f)
    private val plans = HashMap<String, Plan>()

    // ───────────────────────────── Cadre ─────────────────────────────

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        val sc = slot.scene
        val t = localMs
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, pal.bgTop, pal.bgBottom)
        g.fillRadialGlow(540f, 520f, 780f, pal.accent.withAlpha(0.10f))
        val plan = plans.getOrPut(sc.id) { choose(g, sc) }
        if (plan.stage) stage(g, sc, t, tGlobalMs, plan.stageBottom)
        when (sc.layout) {
            Layout.HERO -> hero(g, sc, t, plan, true)
            Layout.DEFINITION -> definition(g, sc, t, plan, true)
            Layout.STEPS -> steps(g, sc, t, plan, true)
            Layout.STAT -> stat(g, sc, t, plan, true)
            Layout.COMPARE -> compare(g, sc, t, plan, true)
            Layout.CHECKLIST -> checklist(g, sc, t, plan, true)
            Layout.SCREEN -> {}
        }
    }

    private fun fade(t: Float, delay: Float, dur: Float = 700f) = Easing.EXPO_OUT.ease(window(t, delay, dur))

    private fun measure(g: Gfx, sc: Scene, top: Float, k: Float): Float {
        val p = Plan(true, top, k)
        return when (sc.layout) {
            Layout.HERO -> hero(g, sc, 0f, p, false)
            Layout.DEFINITION -> definition(g, sc, 0f, p, false)
            Layout.STEPS -> steps(g, sc, 0f, p, false)
            Layout.STAT -> stat(g, sc, 0f, p, false)
            Layout.COMPARE -> compare(g, sc, 0f, p, false)
            Layout.CHECKLIST -> checklist(g, sc, 0f, p, false)
            Layout.SCREEN -> 0f
        }
    }

    /**
     * Choisit la mise en page : l'objet prend toute la hauteur que le texte laisse libre (entre 560 et 1000 px de bas de scène) ;
     * si le texte est trop long pour laisser une scène d'au moins 560 px, texte seul. Jamais de texte coupé.
     */
    private fun choose(g: Gfx, sc: Scene): Plan {
        for (k in listOf(1f, 0.9f, 0.8f, 0.72f, 0.65f, 0.58f)) {
            val h = measure(g, sc, 0f, k)
            val sb = (bottomLimit - h - 50f).coerceAtMost(1000f)
            if (sb >= 560f) return Plan(true, sb + 50f, k, sb)
        }
        for (k in listOf(1f, 0.9f, 0.8f, 0.7f, 0.6f, 0.52f, 0.45f, 0.4f)) if (measure(g, sc, 340f, k) <= bottomLimit) return Plan(false, 340f, k)
        return Plan(false, 340f, 0.4f)
    }

    // ───────────────────────────── L'objet 3D ─────────────────────────────

    private fun explodeAt(sc: Scene, t: Float): Float = when (sc.layout) {
        Layout.HERO -> 0f
        Layout.DEFINITION -> 0.6f * Easing.EASE_IN_OUT.ease(window(t, 500f, 2400f))
        Layout.STEPS -> 0.6f + 0.4f * Easing.EASE_IN_OUT.ease(window(t, 300f, 2200f))
        Layout.CHECKLIST -> 1f - Easing.EASE_IN_OUT.ease(window(t, 300f, 2000f))
        else -> 0.45f
    }

    private fun stage(g: Gfx, sc: Scene, t: Float, tg: Float, bottom: Float) {
        val top = 230f
        val h = bottom - top
        val ex = explodeAt(sc, t)
        val appear = fade(t, 0f, 900f)
        val fit = lerp(model.fitAssembled, model.fitExploded, ex)
        val cam = Cam3D(
            yaw = -0.55f + 0.85f * sin(tg / 4300f + phase) + 0.20f * (1f - appear),
            pitch = 0.40f + 0.09f * sin(tg / 6100f + phase),
            dist = 15f,
            s = h * 0.92f / fit,
            cx = 540f - (if (labelsOn(sc, ex)) 120f else 0f), cy = (top + bottom) / 2f + 6f,
            targetY = lerp(model.centerAssembled, model.centerExploded, ex),
        )
        g.save()
        g.clipRect(0f, top - 30f, 1080f, bottom + 10f)
        // Anneaux lumineux qui tournent derrière l'objet
        for (i in 0 until 3) {
            val r = h * (0.46f + 0.07f * i)
            val a = (tg / (26f - i * 5f)) * (if (i % 2 == 0) 1f else -1f)
            g.strokeArc(cam.cx, cam.cy, r, a, 70f + i * 25f, 3f, pal.accent.withAlpha(0.22f - 0.04f * i))
            g.strokeArc(cam.cx, cam.cy, r, a + 180f, 40f, 3f, pal.pal.glow.withAlpha(0.18f))
        }
        g.withPose(Pose(alpha = appear, scale = lerp(0.92f, 1f, appear)), cam.cx, cam.cy) {
            Render3D.floor(g, model, cam, ex, pal.accent)
            val labels = Render3D.draw(g, model, cam, ex, tg)
            if (labelsOn(sc, ex)) callouts(g, labels, ex, top, bottom)
        }
        g.restore()
        val tf = tag
        g.drawTracked("ILLUSTRATION 3D", L, 196f, tf, pal.accent.withAlpha(0.9f), 3f)
    }

    private fun labelsOn(sc: Scene, ex: Float): Boolean =
        kind == Models3D.Kind.LAPTOP && ex > 0.55f && (sc.layout == Layout.STEPS || sc.layout == Layout.DEFINITION || sc.layout == Layout.CHECKLIST)

    /** Repères de la vue éclatée : nom de chaque pièce, relié à la pièce. */
    private fun callouts(g: Gfx, labels: Map<String, Pair<Float, Float>>, ex: Float, top: Float, bottom: Float) {
        val a = Easing.EASE_IN_OUT.ease(clamp01((ex - 0.55f) / 0.3f))
        val f = FontSpec(FontFamily.TEXT, 600, 26f)
        val items = labels.entries.sortedBy { it.value.second }
        val ys = FloatArray(items.size)
        var lastY = top + 10f
        for ((i, e) in items.withIndex()) { ys[i] = maxOf(e.value.second, lastY + 56f); lastY = ys[i] }
        val over = ys.lastOrNull()?.let { it - (bottom - 24f) } ?: 0f
        if (over > 0f) for (i in ys.indices) ys[i] -= over
        val lx = 792f
        for ((i, e) in items.withIndex()) {
            val (x, y) = e.value
            g.line(x, y, lx - 12f, ys[i], 2f, pal.ink.withAlpha(0.45f * a))
            g.fillCircle(x, y, 5f, pal.accent.withAlpha(a))
            g.withPose(Pose(alpha = a), lx, ys[i]) { g.drawText(e.key, lx, ys[i] + 9f, f, pal.ink) }
        }
    }

    // ───────────────────────────── Texte ─────────────────────────────

    private fun kicker(g: Gfx, sc: Scene, t: Float, y: Float, draw: Boolean) {
        val txt = (sc.kicker ?: return).uppercase()
        if (!draw) return
        val a = fade(t, 100f)
        g.withPose(Pose(alpha = a, dx = (1 - a) * -30f), L, y) { g.drawTracked(txt, L, y, caps, pal.accent, 5f) }
    }

    private fun headline(g: Gfx, text: String, key: String, size: Float, top: Float, lines: Int, t: Float, delay: Float, draw: Boolean): Float {
        val marked = Marked.parse(text.uppercase())
        val f = head.withSize(size)
        val b = cache.fit(g, key, marked.plain, f, W, lines, 1.0f)
        if (draw) {
            val runs = MarkedText.runs(b, marked)
            for (i in b.lines.indices) {
                val a = fade(t, delay + i * 140f)
                val base = g.baselineOf(b, top, i)
                g.withPose(Pose(alpha = a, dx = (1 - a) * -60f), L, base) {
                    MarkedText.drawLine(g, runs[i], L, base, b.font, pal.ink, pal.accent, b.font)
                }
            }
        }
        return top + b.height
    }

    private fun para(g: Gfx, key: String, text: String, font: FontSpec, top: Float, w: Float, lines: Int, color: Int, t: Float, delay: Float, draw: Boolean, lh: Float = 1.38f): TextBlock {
        val b = cache.fit(g, key, Marked.strip(text), font, w, lines, lh)
        if (draw) {
            val a = fade(t, delay, 900f)
            g.withPose(Pose(alpha = a, dy = (1 - a) * 22f), L, top) { g.drawBlock(b, L, top, color) }
        }
        return b
    }

    private fun bar(g: Gfx, y: Float, t: Float, delay: Float, draw: Boolean) {
        if (!draw) return
        val a = Easing.EASE_IN_OUT.ease(window(t, delay, 800f))
        g.fillRect(L, y, L + 140f * a, y + 8f, pal.accent)
    }

    private fun hero(g: Gfx, sc: Scene, t: Float, p: Plan, draw: Boolean): Float {
        val k = p.k
        kicker(g, sc, t, p.top, draw)
        val ky = if (sc.kicker != null) 44f else 0f
        var y = headline(g, sc.title, "${sc.id}/h@$k", 130f * k, p.top + ky, 6, t, 300f, draw) + 30f
        bar(g, y, t, 900f, draw)
        y += 40f
        sc.subtitle?.let { y += para(g, "${sc.id}/s@$k", it, sans.withSize(42f * k), y, W - 40f, 6, pal.muted, t, 1200f, draw).height }
        return y
    }

    private fun definition(g: Gfx, sc: Scene, t: Float, p: Plan, draw: Boolean): Float {
        val k = p.k
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        kicker(g, sc, t, p.top, draw)
        val ky = if (sc.kicker != null) 44f else 0f
        var y = headline(g, sc.title, "${sc.id}/h@$k", 104f * k, p.top + ky, 3, t, 250f, draw) + 26f
        bar(g, y, t, 800f, draw)
        y += 44f
        if (body.isNotEmpty()) y += para(g, "${sc.id}/b@$k", body, sans.withSize(50f * k), y, W - 20f, 14, pal.ink, t, 1000f, draw, 1.4f).height
        return y
    }

    private fun steps(g: Gfx, sc: Scene, t: Float, p: Plan, draw: Boolean): Float {
        val k = p.k
        val numW = 150f * k.coerceAtLeast(0.6f)
        val textX = L + numW + 20f
        val textW = R - textX
        val tb = cache.fit(g, "${sc.id}/h@$k", Marked.parse(sc.title.uppercase()).plain, head.withSize(100f * k), W, 4, 1.0f)
        kicker(g, sc, t, p.top, draw)
        val ky = if (sc.kicker != null) 44f else 0f
        headline(g, sc.title, "${sc.id}/h@$k", 100f * k, p.top + ky, 4, t, 200f, draw)
        var y = p.top + ky + tb.height + 44f * k
        var end = y
        for ((i, item) in sc.items.withIndex()) {
            val (label, detail) = splitLabel(Marked.strip(item))
            val lb = cache.fit(g, "${sc.id}/l$i@$k", label, sansB.withSize(46f * k), textW, 4, 1.18f)
            val db = detail?.let { cache.fit(g, "${sc.id}/d$i@$k", it, sans.withSize(34f * k), textW, 5, 1.32f) }
            val h = maxOf(96f * k, lb.height + (db?.height?.plus(8f) ?: 0f))
            if (draw) {
                val a = fade(t, 700f + i * 600f, 800f)
                val top = y
                g.withPose(Pose(alpha = a, dx = (1 - a) * -80f), L, top) {
                    val nf = head.withSize(120f * k)
                    g.drawText((i + 1).toString().padStart(2, '0'), L, top + g.metrics(nf).ascent, nf, pal.accent)
                    g.fillRect(textX - 14f, top + 6f, textX - 10f, top + h, pal.accent.withAlpha(0.5f))
                    g.drawBlock(lb, textX, top, pal.ink)
                    db?.let { g.drawBlock(it, textX, top + lb.height + 8f, pal.muted) }
                }
            }
            end = y + h
            y += h + 42f * k
        }
        return end
    }

    private fun stat(g: Gfx, sc: Scene, t: Float, p: Plan, draw: Boolean): Float {
        val k = p.k
        val (num, unit) = statParts(sc.statValue ?: "")
        val f = fitSize(g, num, head.withSize(300f * k), W)
        val a = fade(t, 300f, 900f)
        val base = p.top + g.metrics(f).ascent.let { -it } * 0.0f + f.size * 0.82f
        if (draw) g.withPose(Pose(alpha = a, scale = lerp(0.9f, 1f, a)), L, base - 100f) { g.drawText(num, L, base, f, pal.accent) }
        var y = base + 30f
        if (unit.isNotEmpty()) {
            val uf = fitSize(g, unit.uppercase(), head.withSize(80f * k), W)
            if (draw) g.withPose(Pose(alpha = fade(t, 800f)), L, y) { g.drawText(unit.uppercase(), L, y + uf.size * 0.8f, uf, pal.ink) }
            y += uf.size + 30f
        }
        return headline(g, sc.title, "${sc.id}/h@$k", 60f * k, y + 10f, 4, t, 1000f, draw)
    }

    private fun compare(g: Gfx, sc: Scene, t: Float, p: Plan, draw: Boolean): Float {
        val cmp = sc.compare ?: return hero(g, sc, t, p, draw)
        val k = p.k
        var y = headline(g, sc.title, "${sc.id}/h@$k", 90f * k, p.top, 4, t, 200f, draw) + 40f
        fun block(label: String, value: String, top: Float, d: Float, color: Int, key: String): Float {
            if (draw) g.withPose(Pose(alpha = fade(t, d)), L, top) { g.drawTracked(label.uppercase(), L, top + 28f, caps, color, 5f) }
            val b = para(g, key, value, sansB.withSize(46f * k), top + 56f, W, 6, pal.ink, t, d + 150f, draw, 1.25f)
            return top + 56f + b.height
        }
        y = block(cmp.badLabel, cmp.badText(), y, 700f, pal.muted, "${sc.id}/b@$k") + 44f
        return block(cmp.goodLabel, cmp.goodText(), y, 1900f, pal.accent, "${sc.id}/g@$k")
    }

    private fun checklist(g: Gfx, sc: Scene, t: Float, p: Plan, draw: Boolean): Float {
        val k = p.k
        val blocks = sc.items.mapIndexed { i, it -> cache.fit(g, "${sc.id}/i$i@$k", Marked.strip(it), sansB.withSize(48f * k), W - 90f, 5, 1.22f) }
        var y = headline(g, sc.title, "${sc.id}/h@$k", 110f * k, p.top, 4, t, 200f, draw) + 44f
        for ((i, b) in blocks.withIndex()) {
            if (draw) {
                val d = 700f + i * 600f
                val a = fade(t, d, 800f)
                val top = y
                g.withPose(Pose(alpha = a, dx = (1 - a) * -60f), L, top) {
                    g.fillRoundRect(L, top + 6f, L + 56f, top + 62f, 14f, pal.accent)
                    val s = Easing.EASE_IN_OUT.ease(window(t, d + 250f, 500f))
                    if (s > 0f) {
                        g.line(L + 14f, top + 36f, L + 25f, top + 48f, 7f, pal.bgBottom)
                        g.line(L + 25f, top + 48f, L + 14f + 28f * s, top + 48f - 26f * s, 7f, pal.bgBottom)
                    }
                    g.drawBlock(b, L + 90f, top, pal.ink)
                }
            }
            y += maxOf(b.height, 68f) + 30f * k
        }
        if (sc.body.isNotEmpty()) y += para(g, "${sc.id}/body@$k", sc.body, sans.withSize(42f * k), y + 10f, W, 3, pal.muted, t, 700f + sc.items.size * 600f + 200f, draw).height + 10f
        return y
    }
}
