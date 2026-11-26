// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Dessine le contenu d'une scène à un instant local donné (ms depuis son début), habillé par un template.
 * Six compositions (Layout) ; le template fournit polices, couleurs, cartes, mouvements des titres
 * et mise en valeur des mots. Les mises en page de texte sont calculées une fois puis mises en cache.
 */
class SceneRenderer(private val tp: Template) {

    private val style = tp.style
    private val cache = HashMap<String, TextBlock>()

    private fun block(g: Gfx, key: String, text: String, font: FontSpec, width: Float, maxLines: Int, lh: Float = 1.18f) =
        cache.getOrPut(key) { TextLayout.fit(g, text, font, width, maxLines, lineHeightFactor = lh) }

    private val fBody = FontSpec(FontFamily.TEXT, 500, 46f)
    private val fItem = FontSpec(FontFamily.TEXT, 600, 42f)
    private val fStepLabel = FontSpec(FontFamily.TEXT, 700, 48f)
    private val fItemDetail = FontSpec(FontFamily.TEXT, 500, 40f)
    private val fSub = FontSpec(FontFamily.TEXT, 500, 40f)
    private val fMono = FontSpec(FontFamily.MONO, 400, 38f)
    /** Couleur des petits textes d'accent : l'accent sur fond sombre, la couleur d'emphase (plus contrastée) sur fond clair. */
    private val accentText = if (style.dark) style.accent else style.emphasis.let { if (tp.emphasis == Emphasis.MARKER) style.accent else it }

    fun draw(g: Gfx, sc: Scene, t: Float, tGlobal: Float) {
        val dur = sc.durationMs.toFloat()
        val exit = Animator.exit(sc.exit, t, dur)
        val alt = sc.variant % 2 == 1
        when (sc.layout) {
            Layout.HERO -> if (alt) heroSplit(g, sc, t, exit, tGlobal) else hero(g, sc, t, exit, tGlobal)
            Layout.DEFINITION -> if (alt) definitionQuote(g, sc, t, dur, exit, tGlobal) else definition(g, sc, t, dur, exit, tGlobal)
            Layout.STEPS -> if (alt) stepsCards(g, sc, t, exit, tGlobal) else steps(g, sc, t, exit, tGlobal)
            Layout.COMPARE -> if (alt) compareVersus(g, sc, t, exit) else compare(g, sc, t, exit)
            Layout.STAT -> if (alt) statBar(g, sc, t, exit, tGlobal) else stat(g, sc, t, exit, tGlobal)
            Layout.CHECKLIST -> if (alt) checklistCards(g, sc, t, exit, tGlobal) else checklist(g, sc, t, exit, tGlobal)
            Layout.SCREEN -> {}
        }
    }

    /** Pose d'un élément : animation d'entrée (du template par défaut), décalée de `delay`, puis sortie de la scène. */
    private fun p(sc: Scene, t: Float, exit: Pose, delay: Int, type: AnimType = tp.enter, dur: Int = sc.enter.durationMs,
                  easing: Easing = sc.enter.easing): Pose =
        Animator.enter(sc.enter.copy(type = type, delayMs = sc.enter.delayMs + delay, durationMs = dur, easing = easing), t) + exit

    // ───────────────────────────── Éléments communs ─────────────────────────────

    private fun titleBlock(g: Gfx, sc: Scene, key: String, size: Float, width: Float, maxLines: Int): Pair<Marked, TextBlock> {
        val src = if (tp.uppercaseTitles) sc.title.uppercase() else sc.title
        val marked = Marked.parse(src)
        // Boldonse a des capitales très hautes : interligne plus large pour que les blocs de mise en valeur ne se touchent pas.
        val lh = when (tp.titleFont.family) { FontFamily.HEAVY -> 1.5f; FontFamily.CONDENSED -> 1.12f; else -> 1.08f }
        return marked to block(g, "${sc.id}/$key", marked.plain, tp.titleFont.withSize(size * tp.titleScale), width, maxLines, lh)
    }

    /** Titre animé du template. Retourne la hauteur occupée. */
    private fun title(g: Gfx, sc: Scene, key: String, size: Float, width: Float, maxLines: Int, x: Float, top: Float,
                      align: Align, t: Float, exit: Pose, delay: Float): Float {
        val (marked, b) = titleBlock(g, sc, key, size, width, maxLines)
        g.withPose(exit, x, top) {
            g.drawKinetic(b, marked, x, top, align, style.text, Kinetic(tp.motion, tp.emphasis, style, delay), t)
        }
        return b.height
    }

    private fun kicker(g: Gfx, text: String, x: Float, cy: Float, align: Align): Float {
        val f = tp.kickerFont
        val tracking = if (f.family == FontFamily.CONDENSED) 2f else 3.2f
        val w = g.measureTracked(text, f, tracking) + 48f
        val l = if (align == Align.CENTER) x - w / 2 else x
        val h = 28f
        if (tp.emphasis == Emphasis.BOX) {
            g.fillRect(l, cy - h, l + w, cy + h, style.accent)
            g.drawTracked(text, l + 24f, cy + g.metrics(f).ascent / 2f, f, style.accentInk, tracking)
        } else {
            g.fillRoundRect(l, cy - h, l + w, cy + h, h, style.accent.withAlpha(if (style.dark) 0.12f else 0.10f))
            g.strokeRoundRect(l, cy - h, l + w, cy + h, h, 2f, style.accent.withAlpha(0.55f))
            g.drawTracked(text, l + 24f, cy + g.metrics(f).ascent / 2f, f, accentText, tracking)
        }
        return w
    }

    /** Carte selon le style du template, avec ombre portée. */
    private fun card(g: Gfx, l: Float, t: Float, r: Float, b: Float, stroke: Int? = null) {
        val rad = tp.radius
        when (tp.card) {
            CardStyle.GLASS -> {
                g.fillRoundRect(l, t + 18f, r, b + 18f, rad, style.shadow)
                g.fillRoundRect(l, t, r, b, rad, style.surface.withAlpha(0.82f))
                g.strokeRoundRect(l, t, r, b, rad, 2f, stroke ?: style.surfaceStroke)
                g.fillRoundRect(l + rad, t + 1f, r - rad, t + 3f, 1f, style.text.withAlpha(0.10f)) // reflet
            }
            CardStyle.SOLID -> {
                g.fillRoundRect(l, t + 14f, r, b + 14f, rad, style.shadow)
                g.fillRoundRect(l, t, r, b, rad, style.surface)
                if (stroke != null) g.strokeRoundRect(l, t, r, b, rad, 3f, stroke)
            }
            CardStyle.OUTLINE -> {
                if (!style.dark) g.fillRoundRect(l, t, r, b, rad, style.surface.withAlpha(0.7f))
                g.strokeRoundRect(l, t, r, b, rad, 3f, stroke ?: style.surfaceStroke)
            }
        }
    }

    /** Pastille d'icône : disque + anneau tournant en pointillés (élément décoratif animé). */
    private fun iconBadge(g: Gfx, id: IconId, cx: Float, cy: Float, r: Float, tGlobal: Float, color: Int = style.accent) {
        g.fillCircle(cx, cy, r, style.surface)
        g.strokeCircle(cx, cy, r, 3f, color.withAlpha(0.85f))
        val rot = (tGlobal * 0.03f) % 360f
        for (k in 0 until 12) g.strokeArc(cx, cy, r + 20f, rot + k * 30f, 14f, 3f, color.withAlpha(0.45f))
        Icons.draw(g, id, cx, cy, r * 1.05f, color, strokeWidth = 2.1f)
    }

    private fun caretOn(t: Float, reveal: Float) = reveal < 1f || ((t / 420f).toInt() % 2 == 0)

    private fun shape(g: Gfx, kind: ShapeKind, cx: Float, cy: Float, r: Float, tGlobal: Float, color: Int) {
        val rot = tGlobal * 0.006f
        fun of(rr: Float, rt: Float) = when (kind) {
            ShapeKind.RING -> PathData().circle(cx, cy, rr)
            ShapeKind.HEXAGON -> PathData().polygon(cx, cy, rr, 6, rt)
            ShapeKind.DIAMOND -> PathData().polygon(cx, cy, rr, 4, rt)
        }
        g.strokePath(of(r, rot), 4f, color)
        g.strokePath(of(r * 0.72f, -rot * 1.5f), 2f, color.withAlpha(0.6f))
    }

    // ───────────────────────────── 1. Couverture ─────────────────────────────

    private fun hero(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val cx = Safe.CENTER_X
        g.withPose(p(sc, t, exit, 0, AnimType.ZOOM_OUT, 1000), cx, 600f) { illustration(g, sc.illustration, cx, 600f, t, tg) }
        // Le bloc texte remonte si un titre long risque de toucher le pied de page.
        val titleH = titleBlock(g, sc, "title", 92f, Safe.WIDTH, 3).second.height
        val subH = sc.subtitle?.let { block(g, "${sc.id}/sub", Marked.strip(it), fSub.withSize(42f), Safe.WIDTH - 40f, 3, 1.35f).height } ?: 0f
        val total = (if (sc.kicker != null) 66f else 0f) + titleH + 30f + 50f + subH
        var y = min(990f, 1560f - total)
        sc.kicker?.let { k ->
            g.withPose(p(sc, t, exit, 200, AnimType.SLIDE_UP), cx, y) { kicker(g, k, cx, y, Align.CENTER) }
            y += 66f
        }
        y += title(g, sc, "title", 92f, Safe.WIDTH, 3, cx, y, Align.CENTER, t, exit, 380f) + 30f
        val bar = Easing.EXPO_OUT.ease(window(t, 1000f, 700f))
        if (bar > 0f) g.withPose(exit, cx, y) { g.fillRoundRect(cx - 120f * bar, y, cx + 120f * bar, y + 10f, 5f, style.accent) }
        y += 50f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/sub", Marked.strip(s), fSub.withSize(42f), Safe.WIDTH - 40f, 3, 1.35f)
            g.withPose(p(sc, t, exit, 1100, AnimType.FADE_IN, 700), cx, y) { g.drawBlock(sub, cx, y, style.textMuted, Align.CENTER) }
        }
    }

    /** Couverture, variante « titre d'abord » : grand titre en haut, illustration en bas. */
    private fun heroSplit(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        var y = 250f
        sc.kicker?.let { k -> g.withPose(p(sc, t, exit, 0, AnimType.SLIDE_LEFT), x, y) { kicker(g, k, x, y, Align.LEFT) }; y += 70f }
        y += title(g, sc, "titleB", 104f, Safe.WIDTH, 3, x, y, Align.LEFT, t, exit, 200f) + 26f
        val bar = Easing.EXPO_OUT.ease(window(t, 800f, 700f))
        if (bar > 0f) g.withPose(exit, x, y) { g.fillRoundRect(x, y, x + 240f * bar, y + 12f, 6f, style.accent) }
        y += 44f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/subB", Marked.strip(s), fSub.withSize(42f), Safe.WIDTH, 2, 1.35f)
            g.withPose(p(sc, t, exit, 900, AnimType.FADE_IN, 700), x, y) { g.drawBlock(sub, x, y, style.textMuted) }
        }
        g.withPose(p(sc, t, exit, 300, AnimType.ZOOM_IN, 1000), Safe.CENTER_X, 1250f) {
            g.save(); g.scale(0.78f, Safe.CENTER_X, 1250f)
            illustration(g, sc.illustration, Safe.CENTER_X, 1250f, t, tg)
            g.restore()
        }
    }

    /** Définition, variante « citation » : pas de carte, grand guillemet, texte large. */
    private fun definitionQuote(g: Gfx, sc: Scene, t: Float, dur: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        var y = 240f
        sc.kicker?.let { k -> g.withPose(p(sc, t, exit, 0, AnimType.SLIDE_LEFT), x, y) { kicker(g, k, x, y, Align.LEFT) }; y += 74f }
        y += title(g, sc, "titleQ", 76f, Safe.WIDTH, 2, x, y, Align.LEFT, t, exit, 150f) + 70f
        val qf = FontSpec(FontFamily.CINEMA, 400, 260f)
        g.withPose(p(sc, t, exit, 300, AnimType.ZOOM_IN, 800, Easing.BACK_OUT), x + 60f, y + 60f) {
            g.drawText("«", x - 10f, y + 150f, qf, style.accent.withAlpha(0.9f))
        }
        val body = block(g, "${sc.id}/bodyQ", Marked.strip(sc.body), FontSpec(FontFamily.TEXT, 600, 58f), Safe.WIDTH - 40f, 8, 1.36f)
        val top = y + 190f
        g.withPose(p(sc, t, exit, 500, AnimType.FADE_IN, 400), x, top) {
            g.fillRoundRect(x, top, x + 8f, top + body.height, 4f, style.accent2)
        }
        val typeMs = (body.charCount * 22f).coerceIn(1200f, max(1200f, dur - 2400f))
        val typing = Animator.enter(AnimSpec(AnimType.TYPEWRITER, typeMs.toInt(), 800, Easing.LINEAR), t)
        g.withPose(exit, x, top) {
            g.drawBlock(body, x + 40f, top, style.text, reveal = typing.reveal, caretColor = style.accent, caretVisible = caretOn(t, typing.reveal))
        }
        sc.subtitle?.let { s ->
            val sy = top + body.height + 50f
            val sub = block(g, "${sc.id}/subQ", Marked.strip(s), fSub.withSize(40f), Safe.WIDTH - 40f, 2, 1.35f)
            if (sy + sub.height < 1560f) g.withPose(p(sc, t, exit, (800 + typeMs).toInt(), AnimType.FADE_IN, 600), x, sy) {
                g.drawBlock(sub, x + 40f, sy, style.textMuted)
            }
        }
        sc.icon?.let { id -> g.withPose(p(sc, t, exit, 200, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), Safe.RIGHT - 70f, 250f) {
            iconBadge(g, id, Safe.RIGHT - 70f, 250f, 52f, tg, style.accent2)
        } }
    }

    /** Étapes, variante « cartes » : chaque étape dans une carte avec un grand numéro. */
    private fun stepsCards(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        var y = 250f
        y += title(g, sc, "titleC", 86f, Safe.WIDTH, 2, x, y, Align.LEFT, t, exit, 0f) + 60f
        val numW = 110f
        val textX = x + numW + 40f
        val textW = Safe.RIGHT - 36f - textX
        data class Card(val top: Float, val h: Float, val label: TextBlock, val detail: TextBlock?)
        // Aucune coupure du texte de l'auteur : l'échelle baisse (jusqu'à 60 %) tant que les cartes ne tiennent pas.
        fun layoutCards(k: Float): List<Card> {
            val out = ArrayList<Card>(); var yy = y
            for ((i, item) in sc.items.withIndex()) {
                val parts = Marked.strip(item).split(LABEL_SEP, limit = 2)
                val label = block(g, "${sc.id}/ci$i@$k", parts[0], fStepLabel.withSize(44f * k), textW, 4, 1.2f)
                val detail = parts.getOrNull(1)?.let { block(g, "${sc.id}/cd$i@$k", it, fItemDetail.withSize(36f * k), textW, 5, 1.28f) }
                val h = (label.height + (detail?.height?.plus(8f) ?: 0f) + 56f * k).coerceAtLeast(120f * k)
                out += Card(yy, h, label, detail)
                yy += h + 24f * k
            }
            return out
        }
        val cards = listOf(1f, 0.88f, 0.78f, 0.7f, 0.62f).map { layoutCards(it) }.firstOrNull { c -> c.last().let { it.top + it.h } <= Safe.BOTTOM - 120f }
            ?: layoutCards(0.6f)
        for ((i, c) in cards.withIndex()) {
            val top = c.top; val h = c.h; val label = c.label; val detail = c.detail
            val delay = 450 + i * 480
            g.withPose(p(sc, t, exit, delay, dur = 650), x, top) {
                card(g, x, top, Safe.RIGHT, top + h, if (i % 2 == 0) style.accent.withAlpha(0.6f) else style.accent2.withAlpha(0.6f))
                val nf = tp.titleFont.withSize(if (tp.titleFont.family == FontFamily.HEAVY) 56f else 78f)
                val num = (i + 1).toString().padStart(2, '0')
                g.drawText(num, x + 28f, top + h / 2 + g.metrics(nf).ascent / 2, nf, if (i % 2 == 0) style.accent else style.accent2)
                val contentH = label.height + (detail?.height?.plus(8f) ?: 0f)
                val ty = top + (h - contentH) / 2
                g.drawBlock(label, textX, ty, style.text)
                detail?.let { d -> g.drawBlock(d, textX, ty + label.height + 8f, style.textMuted) }
            }
        }
    }

    /** Chiffre clé, variante « barre » : grand nombre à gauche et jauge qui se remplit. */
    private fun statBar(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        val v = sc.statValue ?: return stat(g, sc, t, exit, tg)
        val (num, unit) = splitStat(v)
        val f = fitFont(g, num, tp.titleFont.withSize(if (tp.titleFont.family == FontFamily.HEAVY) 170f else 250f), Safe.WIDTH * 0.7f)
        val uf = FontSpec(FontFamily.TEXT, 700, 52f)
        val base = 640f
        g.withPose(p(sc, t, exit, 100, AnimType.SLIDE_UP, 900), x, base) {
            g.fillRadialGlow(x + 200f, base - 100f, 420f, style.accent.withAlpha(if (style.dark) 0.18f else 0.10f))
            g.drawText(num, x, base, f, style.text)
            if (unit.isNotEmpty()) {
                val uf2 = fitFont(g, unit, uf, Safe.RIGHT - (x + g.measureText(num, f) + 24f))
                g.drawText(unit, x + g.measureText(num, f) + 24f, base, uf2, style.accent)
            }
        }
        val barTop = base + 70f
        val k = Easing.EXPO_OUT.ease(window(t, 500f, 2200f))
        g.withPose(exit, x, barTop) {
            g.fillRoundRect(x, barTop, Safe.RIGHT, barTop + 26f, 13f, style.text.withAlpha(0.12f))
            g.fillRoundRect(x, barTop, x + Safe.WIDTH * k, barTop + 26f, 13f, style.accent)
            for (i in 1 until 10) g.fillRect(x + Safe.WIDTH * i / 10f - 1f, barTop + 40f, x + Safe.WIDTH * i / 10f + 1f, barTop + 56f, style.text.withAlpha(0.2f))
        }
        var y = barTop + 120f
        y += title(g, sc, "titleS", 84f, Safe.WIDTH, 3, x, y, Align.LEFT, t, exit, 600f) + 40f
        if (sc.body.isNotEmpty()) {
            val body = block(g, "${sc.id}/bodyS", Marked.strip(sc.body), fSub.withSize(44f), Safe.WIDTH, 4, 1.38f)
            g.withPose(p(sc, t, exit, 1000, AnimType.FADE_IN), x, y) { g.drawBlock(body, x, y, style.textMuted) }
        }
    }

    // ───────────────────────────── 2. Définition ─────────────────────────────

    private fun definition(g: Gfx, sc: Scene, t: Float, dur: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        val badgeCy = 330f
        sc.icon?.let { id ->
            g.withPose(p(sc, t, exit, 0, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), x + 64f, badgeCy) { iconBadge(g, id, x + 64f, badgeCy, 62f, tg) }
        }
        sc.kicker?.let { k -> g.withPose(p(sc, t, exit, 150, AnimType.SLIDE_LEFT), x + 170f, badgeCy) { kicker(g, k, x + 170f, badgeCy, Align.LEFT) } }

        var y = 470f
        y += title(g, sc, "title", 90f, Safe.WIDTH, 3, x, y, Align.LEFT, t, exit, 250f) + 64f

        val pad = 48f
        val body = block(g, "${sc.id}/body", Marked.strip(sc.body), fBody.withSize(52f), Safe.WIDTH - pad * 2 - 12f, 7, 1.42f)
        val cardTop = y
        val cardBottom = y + body.height + pad * 2
        g.withPose(p(sc, t, exit, 450, AnimType.SLIDE_UP), x, cardTop) {
            card(g, x, cardTop, Safe.RIGHT, cardBottom)
            g.fillRoundRect(x, cardTop + 28f, x + 10f, cardBottom - 28f, 5f, style.accent)
        }
        val typeMs = (body.charCount * 24f).coerceIn(1200f, max(1200f, dur - 2400f))
        val typing = Animator.enter(AnimSpec(AnimType.TYPEWRITER, typeMs.toInt(), 900, Easing.LINEAR), t)
        g.withPose(exit, x, cardTop) {
            g.drawBlock(body, x + pad + 12f, cardTop + pad, style.text, reveal = typing.reveal,
                caretColor = style.accent2, caretVisible = caretOn(t, typing.reveal))
        }
        y = cardBottom + 56f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/sub", Marked.strip(s), fSub.withSize(42f), Safe.WIDTH, 3, 1.35f)
            g.withPose(p(sc, t, exit, (900 + typeMs).toInt(), AnimType.FADE_IN, 600), x, y) { g.drawBlock(sub, x, y, style.textMuted) }
        }
        g.withPose(p(sc, t, exit, 300, AnimType.FADE_IN, 1200), 940f, 1480f) {
            shape(g, sc.shape, 940f, 1480f, 230f, tg, style.accent.withAlpha(0.14f))
        }
    }

    // ───────────────────────────── 3. Étapes ─────────────────────────────

    private fun steps(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        var y = 250f
        val titleW = if (sc.icon != null) Safe.WIDTH - 170f else Safe.WIDTH
        sc.icon?.let { id ->
            g.withPose(p(sc, t, exit, 100, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), Safe.RIGHT - 70f, 300f) {
                iconBadge(g, id, Safe.RIGHT - 70f, 300f, 58f, tg, style.accent2)
            }
        }
        y = max(y + title(g, sc, "title", 90f, titleW, 3, x, y, Align.LEFT, t, exit, 0f), 420f) + 96f

        val bubbleR = 42f
        val textX = x + bubbleR * 2 + 36f
        val textW = Safe.RIGHT - textX
        data class Row(val top: Float, val label: TextBlock, val detail: TextBlock?, val h: Float)
        // Tout le texte de l'auteur reste visible : on réduit l'échelle (jusqu'à 62 %) avant de couper quoi que ce soit.
        fun layoutRows(k: Float): List<Row> {
            val out = ArrayList<Row>(); var yy = y
            for ((i, item) in sc.items.withIndex()) {
                val parts = Marked.strip(item).split(LABEL_SEP, limit = 2)
                val label = block(g, "${sc.id}/i$i@$k", parts[0], fStepLabel.withSize(48f * k), textW, 4, 1.22f)
                val detail = parts.getOrNull(1)?.let { block(g, "${sc.id}/d$i@$k", it, fItemDetail.withSize(40f * k), textW, 5, 1.3f) }
                val h = max(bubbleR * 2, label.height + (detail?.height?.plus(6f) ?: 0f))
                out += Row(yy, label, detail, h)
                yy += h + 72f * k
            }
            return out
        }
        val rows = listOf(1f, 0.9f, 0.8f, 0.72f, 0.65f).map { layoutRows(it) }.firstOrNull { r -> r.last().let { it.top + it.h } <= Safe.BOTTOM - 120f }
            ?: layoutRows(0.62f)
        if (rows.size > 1) {
            val first = rows.first().top + bubbleR
            val last = rows.last().top + bubbleR
            val prog = clamp01((t - 500f) / (520f * (rows.size - 1) + 400f))
            g.withPose(exit, 0f, 0f) {
                g.line(x + bubbleR, first, x + bubbleR, first + (last - first) * Easing.EASE_IN_OUT.ease(prog), 4f, style.surfaceStroke)
            }
        }
        for ((i, r) in rows.withIndex()) {
            val delay = 500 + i * 520
            g.withPose(p(sc, t, exit, delay, AnimType.ZOOM_IN, 600, Easing.BACK_OUT), x + bubbleR, r.top + bubbleR) {
                g.fillCircle(x + bubbleR, r.top + bubbleR, bubbleR, if (i % 2 == 0) style.accent else style.accent2)
                val num = (i + 1).toString()
                val f = tp.titleFont.withSize(if (tp.titleFont.family == FontFamily.HEAVY) 32f else 42f)
                g.drawText(num, x + bubbleR - g.measureText(num, f) / 2, r.top + bubbleR + g.metrics(f).ascent / 2,
                    f, if (i % 2 == 0) style.accentInk else style.highlightInk)
            }
            g.withPose(p(sc, t, exit, delay + 80, dur = 650), textX, r.top) {
                val labelTop = r.top + if (r.detail == null) (bubbleR * 2 - r.label.height) / 2 else -4f
                g.drawBlock(r.label, textX, labelTop, style.text)
                r.detail?.let { d -> g.drawBlock(d, textX, labelTop + r.label.height + 6f, style.textMuted) }
            }
        }
    }

    // ───────────────────────────── 4. Comparaison ─────────────────────────────

    private fun compare(g: Gfx, sc: Scene, t: Float, exit: Pose) {
        val cmp = sc.compare ?: return
        val x = Safe.LEFT
        var y = 230f
        sc.kicker?.let { k -> g.withPose(p(sc, t, exit, 0, AnimType.SLIDE_LEFT), x, y + 28f) { kicker(g, k, x, y + 28f, Align.LEFT) }; y += 96f }
        y += title(g, sc, "title", 84f, Safe.WIDTH, 2, x, y, Align.LEFT, t, exit, 100f) + 64f

        val cardH = 300f
        val gap = 44f
        val aTop = y
        val bTop = y + cardH + gap
        val valueY = aTop + 200f
        val move = Easing.EASE_IN_OUT.ease(window(t, 1700f, 1500f))
        val magX = lerp(x + 90f, Safe.RIGHT - 110f, move)
        val magY = valueY - 10f - 26f * sin(move * PI.toFloat())
        val magAlpha = window(t, 1500f, 250f) * (1f - window(t, 3400f, 350f))

        cmpCard(g, sc, t, exit, aTop, cardH, cmp.badLabel, cmp.badValue, style.danger, IconId.CROSS, AnimType.SLIDE_RIGHT, 350,
            underlineToX = if (t > 1700f) magX + 30f else -1f)
        cmpCard(g, sc, t, exit, bTop, cardH, cmp.goodLabel, cmp.goodValue, style.ok, IconId.CHECK, AnimType.SLIDE_LEFT, 850, -1f)

        if (magAlpha > 0f) g.withPose(exit + Pose(alpha = magAlpha), magX, magY) {
            g.fillCircle(magX - 8f, magY - 8f, 44f, style.accent2.withAlpha(0.18f))
            Icons.draw(g, IconId.SEARCH, magX, magY, 120f, style.accent2, strokeWidth = 2.4f)
        }
        sc.subtitle?.let { s ->
            val top = bTop + cardH + 56f
            val sub = block(g, "${sc.id}/sub", Marked.strip(s), fSub.withSize(40f), Safe.WIDTH - 70f, 3, 1.35f)
            g.withPose(p(sc, t, exit, 2600, AnimType.SLIDE_UP, 700), x, top) {
                Icons.draw(g, IconId.EYE, x + 24f, top + sub.lineHeight / 2, 46f, style.accent, 2.2f)
                g.drawBlock(sub, x + 70f, top, style.textMuted)
            }
        }
    }

    private fun cmpCard(
        g: Gfx, sc: Scene, t: Float, exit: Pose, top: Float, h: Float, label: String, value: List<Span>,
        color: Int, icon: IconId, anim: AnimType, delay: Int, underlineToX: Float,
    ) {
        val l = Safe.LEFT
        val r = Safe.RIGHT
        g.withPose(p(sc, t, exit, delay, anim, 750), l, top) {
            card(g, l, top, r, top + h, color.withAlpha(0.75f))
            g.fillCircle(l + 74f, top + 78f, 36f, color.withAlpha(0.18f))
            Icons.draw(g, icon, l + 74f, top + 78f, 44f, color, 2.6f)
            val lf = FontSpec(FontFamily.TEXT, 700, 40f)
            g.drawText(label, l + 132f, top + 78f + g.metrics(lf).ascent / 2, lf, color)
            g.fillRoundRect(l + 36f, top + 140f, r - 36f, top + 250f, minOf(22f, tp.radius), if (style.dark) style.bgTop.withAlpha(0.85f) else style.bgBottom)
            val avail = (r - 36f) - (l + 36f) - 56f
            val full = value.joinToString("") { it.text }
            // Une adresse (avec « / » ou « . » sans espace) en police mono ; une phrase dans la police du texte.
            val isCode = !full.contains(' ') && (full.contains('/') || full.contains('.'))
            if (!isCode && g.measureText(full, FontSpec(FontFamily.TEXT, 600, 36f)) > avail) {
                // Phrase longue : deux lignes plutôt qu'une police minuscule.
                val b = block(g, "${sc.id}/cmp/$label", full, FontSpec(FontFamily.TEXT, 600, 36f), avail, 2, 1.18f)
                val color0 = if (value.all { it.highlight }) color else style.text
                g.drawBlock(b, l + 64f, top + 195f - b.height / 2, color0)
                return@withPose
            }
            var font = if (isCode) fMono else FontSpec(FontFamily.TEXT, 600, 38f)
            while (g.measureText(full, font) > avail && font.size > 22f) font = font.withSize(font.size - 1f)
            val base = top + 195f + g.metrics(font).ascent / 2
            var cx = l + 64f
            for (s in value) {
                val w = g.measureText(s.text, font)
                g.drawText(s.text, cx, base, font, if (s.highlight) color else style.text)
                if (s.highlight && underlineToX > cx) g.fillRoundRect(cx, base + 14f, min(cx + w, underlineToX), base + 20f, 3f, color)
                cx += w
            }
        }
    }

    // ───────────────────────────── 5. Chiffre clé ─────────────────────────────

    private fun stat(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val cx = Safe.CENTER_X
        val cy = 640f
        val r = 230f
        g.withPose(p(sc, t, exit, 0, AnimType.FADE_IN, 600), cx, cy) {
            val rot = tg * 0.008f
            for (k in 0 until 60) {
                val a = rad((rot + k * 6f).toDouble())
                val r0 = if (k % 5 == 0) 272f else 282f
                g.line(cx + (r0 * cos(a)).toFloat(), cy + (r0 * sin(a)).toFloat(), cx + (296f * cos(a)).toFloat(), cy + (296f * sin(a)).toFloat(),
                    3f, style.text.withAlpha(if (k % 5 == 0) 0.28f else 0.12f))
            }
            g.fillRadialGlow(cx, cy, r * 1.1f, style.accent.withAlpha(if (style.dark) 0.18f else 0.10f))
            g.strokeCircle(cx, cy, r, 26f, if (style.dark) style.surface else style.surfaceStroke)
        }
        val prog = window(t, 600f, 3000f)
        if (prog > 0f) g.withPose(exit, cx, cy) { g.strokeArc(cx, cy, r, -90f, 360f * prog, 26f, style.accent2) }
        sc.statValue?.let { v ->
            // « 30 minutes » : le nombre en grand, l'unité en dessous, le tout dans l'anneau.
            val (num, unit) = splitStat(v)
            val f = fitFont(g, num, tp.titleFont.withSize(if (tp.titleFont.family == FontFamily.HEAVY) 130f else 190f), 380f)
            val uf = fitFont(g, unit, FontSpec(FontFamily.TEXT, 700, 44f), 340f)
            g.withPose(p(sc, t, exit, 150, AnimType.ZOOM_IN, 900, Easing.BACK_OUT), cx, cy) {
                val base = cy + g.metrics(f).ascent / 2 - (if (unit.isEmpty()) 6f else 48f)
                g.drawText(num, cx - g.measureText(num, f) / 2, base, f, style.text)
                if (unit.isNotEmpty()) g.drawText(unit, cx - g.measureText(unit, uf) / 2, base + 100f, uf, style.textMuted)
            }
        }
        var y = cy + 400f
        y += title(g, sc, "title", 82f, Safe.WIDTH, 2, cx, y, Align.CENTER, t, exit, 500f) + 36f
        if (sc.body.isNotEmpty()) {
            val body = block(g, "${sc.id}/body", Marked.strip(sc.body), fSub.withSize(44f), Safe.WIDTH - 40f, 4, 1.38f)
            g.withPose(p(sc, t, exit, 900, AnimType.FADE_IN), cx, y) { g.drawBlock(body, cx, y, style.textMuted, Align.CENTER) }
        }
    }

    /** « 30 minutes » → (« 30 », « minutes ») ; « 3389 » → (« 3389 », « »). */
    private fun splitStat(v: String): Pair<String, String> {
        val i = v.indexOfFirst { it == ' ' || it == '\u00A0' }
        return if (i <= 0) v to "" else v.substring(0, i) to v.substring(i + 1).trim()
    }

    /** Réduit la police jusqu'à ce que le texte tienne dans `maxW`. */
    private fun fitFont(g: Gfx, text: String, font: FontSpec, maxW: Float): FontSpec {
        var f = font
        while (text.isNotEmpty() && g.measureText(text, f) > maxW && f.size > 20f) f = f.withSize(f.size * 0.92f)
        return f
    }

    // ───────────────────────────── 6. À retenir ─────────────────────────────

    private fun checklist(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val cx = Safe.CENTER_X
        g.withPose(p(sc, t, exit, 0, AnimType.ZOOM_IN, 800, Easing.BACK_OUT), cx, 350f) { illustration(g, sc.illustration, cx, 350f, t, tg) }
        var y = 530f
        y += title(g, sc, "title", 86f, Safe.WIDTH, 2, cx, y, Align.CENTER, t, exit, 200f) + 54f

        val box = 60f
        val x = Safe.LEFT + 20f
        val textX = x + box + 30f
        for ((i, item) in sc.items.withIndex()) {
            val b = block(g, "${sc.id}/c$i", Marked.strip(item), fItem, Safe.RIGHT - textX, 2, 1.25f)
            val h = max(box, b.height)
            val top = y
            val delay = 500 + i * 430
            g.withPose(p(sc, t, exit, delay, AnimType.SLIDE_LEFT, 600), x, top) {
                val bt = top + (h - box) / 2
                val checked = window(t, delay + 300f, 420f)
                g.fillRoundRect(x, bt, x + box, bt + box, 16f, style.ok.withAlpha(0.16f * checked + if (style.dark) 0f else 0.04f))
                g.strokeRoundRect(x, bt, x + box, bt + box, 16f, 4f, style.ok)
                if (checked > 0f) {
                    val check = PathData().moveTo(x + 15f, bt + 31f).lineTo(x + 26f, bt + 43f).lineTo(x + 46f, bt + 18f)
                    g.strokePath(check.trimmed(Easing.EASE_IN_OUT.ease(checked)), 7f, style.ok)
                }
                g.drawBlock(b, textX, top + (h - b.height) / 2, style.text)
            }
            y += h + 34f
        }
        y += 26f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/sub", Marked.strip(s), fSub.withSize(38f), Safe.WIDTH - 30f, 3, 1.35f)
            g.withPose(p(sc, t, exit, 500 + sc.items.size * 430 + 200, AnimType.FADE_IN, 700), cx, y) {
                g.drawBlock(sub, cx, y, style.textMuted, Align.CENTER)
            }
            y += sub.height + 44f
        }
        if (sc.body.isNotEmpty()) {
            val f = FontSpec(FontFamily.TEXT, 700, 38f)
            val text = Marked.strip(sc.body)
            val w = min(g.measureText(text, f) + 80f, Safe.WIDTH)
            g.withPose(p(sc, t, exit, 500 + sc.items.size * 430 + 600, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), cx, y + 46f) {
                g.fillRoundRect(cx - w / 2, y + 10f, cx + w / 2, y + 102f, if (tp.emphasis == Emphasis.BOX) 4f else 46f, style.shadow)
                g.fillRoundRect(cx - w / 2, y, cx + w / 2, y + 92f, if (tp.emphasis == Emphasis.BOX) 4f else 46f, style.highlight)
                g.drawText(text, cx - g.measureText(text, f) / 2, y + 46f + g.metrics(f).ascent / 2, f, style.highlightInk)
            }
        }
    }

    /** Comparaison, variante « versus » : pastille VS entre les cartes, la mauvaise pratique est barrée. */
    private fun compareVersus(g: Gfx, sc: Scene, t: Float, exit: Pose) {
        val cmp = sc.compare ?: return
        val x = Safe.LEFT
        var y = 230f
        sc.kicker?.let { k -> g.withPose(p(sc, t, exit, 0, AnimType.SLIDE_DOWN), x, y + 28f) { kicker(g, k, x, y + 28f, Align.LEFT) }; y += 96f }
        y += title(g, sc, "titleV", 84f, Safe.WIDTH, 2, x, y, Align.LEFT, t, exit, 100f) + 56f
        val cardH = 300f
        val gap = 120f
        val aTop = y
        val bTop = y + cardH + gap
        cmpCard(g, sc, t, exit, aTop, cardH, cmp.badLabel, cmp.badValue, style.danger, IconId.CROSS, AnimType.SLIDE_LEFT, 300, -1f)
        // Trait qui barre la mauvaise pratique.
        val strike = Easing.EASE_IN_OUT.ease(window(t, 1500f, 600f))
        if (strike > 0f) g.withPose(exit, Safe.CENTER_X, aTop + 195f) {
            val l = Safe.LEFT + 50f
            g.fillRoundRect(l, aTop + 191f, l + (Safe.RIGHT - 50f - l) * strike, aTop + 199f, 4f, style.danger)
        }
        val vsY = aTop + cardH + gap / 2
        g.withPose(p(sc, t, exit, 1100, AnimType.ZOOM_IN, 600, Easing.BACK_OUT), Safe.CENTER_X, vsY) {
            g.fillCircle(Safe.CENTER_X, vsY, 50f, style.accent)
            val vf = FontSpec(FontFamily.DISPLAY, 800, 40f)
            g.drawText("VS", Safe.CENTER_X - g.measureText("VS", vf) / 2, vsY + g.metrics(vf).ascent / 2, vf, style.highlightInk)
        }
        cmpCard(g, sc, t, exit, bTop, cardH, cmp.goodLabel, cmp.goodValue, style.ok, IconId.CHECK, AnimType.ZOOM_IN, 1900, -1f)
        sc.subtitle?.let { s ->
            val top = bTop + cardH + 50f
            val sub = block(g, "${sc.id}/subV", Marked.strip(s), fSub.withSize(40f), Safe.WIDTH, 3, 1.35f)
            g.withPose(p(sc, t, exit, 2700, AnimType.FADE_IN, 700), x, top) { g.drawBlock(sub, x, top, style.textMuted) }
        }
    }

    /** Liste à retenir, variante « cartes » : titre en haut, chaque point dans une carte qui monte. */
    private fun checklistCards(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        val n = sc.items.size.coerceAtLeast(1)
        // Peu de points : le bloc descend vers le centre optique au lieu de laisser un grand vide en bas.
        var y = 240f + (4 - n).coerceAtLeast(0) * 110f
        y += title(g, sc, "titleK", 90f, Safe.WIDTH, 2, x, y, Align.LEFT, t, exit, 0f) + 52f
        val textX = x + 118f
        val textW = Safe.RIGHT - 36f - textX
        val tail = (if (sc.subtitle != null) 170f else 0f) + (if (sc.body.isNotEmpty()) 130f else 0f)
        val slot = ((Safe.BOTTOM - 20f - tail - y) / n).coerceAtMost(190f)
        val maxLines = if (slot >= 150f) 2 else 1
        for ((i, item) in sc.items.withIndex()) {
            val b = block(g, "${sc.id}/k$i/$maxLines", Marked.strip(item), fItem, textW, maxLines, 1.22f)
            val h = (b.height + 52f).coerceAtLeast(if (n <= 3) 150f else 108f).coerceAtMost(slot - 18f)
            val top = y
            val delay = 400 + i * 380
            g.withPose(p(sc, t, exit, delay, AnimType.SLIDE_UP, 650), x, top) {
                card(g, x, top, Safe.RIGHT, top + h, style.ok.withAlpha(0.45f))
                val cy = top + h / 2
                val checked = window(t, delay + 280f, 380f)
                g.fillCircle(x + 58f, cy, 32f, style.ok.withAlpha(0.18f + 0.6f * checked))
                if (checked > 0f) Icons.draw(g, IconId.CHECK, x + 58f, cy, 40f, if (checked > 0.6f) style.highlightInk else style.ok, 3f)
                g.drawBlock(b, textX, cy - b.height / 2, style.text)
            }
            y += h + 18f
        }
        y += 22f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/subK", Marked.strip(s), fSub.withSize(38f), Safe.WIDTH, 3, 1.35f)
            g.withPose(p(sc, t, exit, 400 + n * 380 + 200, AnimType.FADE_IN, 700), x, y) { g.drawBlock(sub, x, y, style.textMuted) }
            y += sub.height + 36f
        }
        if (sc.body.isNotEmpty()) {
            val f = FontSpec(FontFamily.TEXT, 700, 38f)
            val text = Marked.strip(sc.body)
            val w = min(g.measureText(text, f) + 80f, Safe.WIDTH)
            g.withPose(p(sc, t, exit, 400 + n * 380 + 600, AnimType.SLIDE_LEFT, 700), x, y + 46f) {
                g.fillRoundRect(x, y, x + w, y + 92f, if (tp.emphasis == Emphasis.BOX) 4f else 46f, style.highlight)
                g.drawText(text, x + 40f, y + 46f + g.metrics(f).ascent / 2, f, style.highlightInk)
            }
        }
    }

    // ───────────────────────────── Illustrations ─────────────────────────────

    private fun illustration(g: Gfx, id: IllustrationId, cx: Float, cy: Float, t: Float, tg: Float) {
        when (id) {
            IllustrationId.NONE -> Unit
            IllustrationId.PHISHING_MAIL -> phishingMail(g, cx, cy, t, tg)
            IllustrationId.SHIELD_CHECK -> shieldCheck(g, cx, cy, t, tg)
            IllustrationId.WATER_DROP -> waterDrop(g, cx, cy, t, tg)
            IllustrationId.PLANT_GROWTH -> plantGrowth(g, cx, cy, t, tg)
            IllustrationId.CHART_GROWTH -> chartGrowth(g, cx, cy, t, tg)
            IllustrationId.CHECK_CIRCLE -> checkCircle(g, cx, cy, t, tg)
            IllustrationId.NETWORK_TOPOLOGY -> networkTopology(g, cx, cy, t, tg)
            IllustrationId.CODE_WINDOW -> codeWindow(g, cx, cy, t, tg)
            IllustrationId.OS_TERMINAL -> osTerminal(g, cx, cy, t, tg)
            IllustrationId.NEURAL_NET -> neuralNet(g, cx, cy, t, tg)
            IllustrationId.HEART_BEAT -> heartBeat(g, cx, cy, t, tg)
            IllustrationId.SPEECH_BUBBLES -> speechBubbles(g, cx, cy, t, tg)
        }
    }

    private fun orbit(g: Gfx, cx: Float, cy: Float, r: Float, tg: Float) {
        g.strokeCircle(cx, cy, r, 2f, style.text.withAlpha(0.08f))
        for (k in 0 until 3) {
            val a = rad((tg * 0.025f + k * 120f).toDouble())
            g.fillCircle(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), 9f, if (k == 0) style.accent2 else style.accent.withAlpha(0.8f))
        }
    }

    private fun phishingMail(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy + 20f, 420f, style.accent.withAlpha(0.22f))
        orbit(g, cx, cy + 20f, 330f, tg)
        val w = 460f
        val h = 310f
        val l = cx - w / 2
        val top = cy - h / 2 + 70f
        g.fillRoundRect(l, top + 16f, l + w, top + h + 16f, 34f, style.shadow)
        g.fillRoundRect(l, top, l + w, top + h, 34f, style.surface)
        g.strokeRoundRect(l, top, l + w, top + h, 34f, 4f, style.surfaceStroke)
        g.strokePath(PathData().moveTo(l + 34f, top + 40f).lineTo(cx, top + 175f).lineTo(l + w - 34f, top + 40f), 9f, style.accent)
        g.strokePath(PathData().moveTo(l + 40f, top + h - 40f).lineTo(cx - 70f, top + 170f), 4f, style.surfaceStroke)
        g.strokePath(PathData().moveTo(l + w - 40f, top + h - 40f).lineTo(cx + 70f, top + 170f), 4f, style.surfaceStroke)
        val hookX = cx + 120f
        val drop = Easing.EXPO_OUT.ease(window(t, 400f, 1300f))
        val bob = if (drop >= 1f) 12f * sin((t - 1700f) * 0.004f) else 0f
        val hookY = lerp(-120f, top - 40f, drop) + bob
        if (hookY > 0f) {
            val size = 130f
            val k = size / 24f
            val eyeX = hookX - 18f - size / 2 + 15f * k
            val eyeTop = hookY - size / 2 + (3.6f - 1.6f) * k
            g.line(eyeX, 0f, eyeX, eyeTop, 3f, style.text.withAlpha(0.55f))
            Icons.draw(g, IconId.HOOK, hookX - 18f, hookY, size, style.accent2, 2.6f)
        }
        val pop = Easing.BACK_OUT.ease(window(t, 1500f, 600f))
        if (pop > 0f) {
            val bx = l + 36f
            val by = top - 6f
            g.save(); g.scale(pop, bx, by)
            g.fillCircle(bx, by, 52f, style.danger)
            Icons.draw(g, IconId.WARNING, bx, by - 2f, 58f, rgb(0x1A1A1A), 2.4f)
            g.restore()
        }
    }

    private fun shieldCheck(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 260f, style.ok.withAlpha(0.22f))
        val rot = (tg * 0.02f) % 360f
        for (k in 0 until 16) g.strokeArc(cx, cy, 160f, rot + k * 22.5f, 11f, 4f, style.ok.withAlpha(0.5f))
        val size = 220f
        val shield = Icons.parts(IconId.SHIELD)[0].path.transformed(size / 24f, cx - size / 2, cy - size / 2)
        g.fillPath(shield, style.ok.withAlpha(0.14f))
        g.strokePath(shield, 9f, style.ok)
        val check = PathData().moveTo(cx - 46f, cy + 4f).lineTo(cx - 12f, cy + 38f).lineTo(cx + 50f, cy - 34f)
        val k = Easing.EASE_IN_OUT.ease(window(t, 500f, 700f))
        if (k > 0f) g.strokePath(check.trimmed(k), 16f, style.accent2)
    }

    private fun checkCircle(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 250f, style.accent.withAlpha(if (style.dark) 0.24f else 0.14f))
        val rot = (tg * 0.02f) % 360f
        for (k in 0 until 16) g.strokeArc(cx, cy, 165f, rot + k * 22.5f, 11f, 4f, style.accent.withAlpha(0.5f))
        g.fillCircle(cx, cy, 120f, style.accent)
        val check = PathData().moveTo(cx - 52f, cy + 2f).lineTo(cx - 14f, cy + 40f).lineTo(cx + 56f, cy - 34f)
        val k = Easing.EASE_IN_OUT.ease(window(t, 500f, 700f))
        if (k > 0f) g.strokePath(check.trimmed(k), 18f, style.accentInk)
    }

    private fun waterDrop(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy + 40f, 420f, style.accent2.withAlpha(0.22f))
        orbit(g, cx, cy + 30f, 320f, tg)
        val size = 400f
        val drop = Icons.parts(IconId.DROP)[0].path.transformed(size / 24f, cx - size / 2, cy - size / 2 + 40f)
        g.fillPath(drop, style.accent.withAlpha(0.92f))
        g.strokePath(Icons.parts(IconId.DROP)[1].path.transformed(size / 24f, cx - size / 2, cy - size / 2 + 40f), 14f, rgb(0xFFFFFF).withAlpha(0.7f))
        // Bulles de savon qui montent en boucle.
        val r = Rand(5)
        for (i in 0 until 9) {
            val bx = cx + r.range(-260f, 260f)
            val period = r.range(2600f, 4200f)
            val ph = ((t + r.range(0f, 3000f)) % period) / period
            val by = cy + 260f - ph * 520f
            val br = r.range(14f, 34f)
            g.strokeCircle(bx + 14f * sin(ph * 6f + i), by, br, 3f, style.accent2.withAlpha(0.7f * (1f - ph)))
            g.fillCircle(bx + 14f * sin(ph * 6f + i) - br * 0.35f, by - br * 0.35f, br * 0.18f, rgb(0xFFFFFF).withAlpha(0.8f * (1f - ph)))
        }
    }

    private fun plantGrowth(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 420f, style.accent.withAlpha(0.22f))
        orbit(g, cx, cy + 20f, 320f, tg)
        // Sol.
        g.fillRoundRect(cx - 230f, cy + 170f, cx + 230f, cy + 230f, 30f, style.accent2.withAlpha(0.85f))
        // Tige qui pousse, feuilles qui s'ouvrent, puis léger balancement.
        val grow = Easing.EXPO_OUT.ease(window(t, 300f, 1500f))
        val sway = 4f * sin(t * 0.0018f)
        val stem = PathData().moveTo(cx, cy + 172f).cubicTo(cx - 20f, cy + 60f, cx + 30f, cy - 40f, cx + sway, cy - 160f)
        g.strokePath(stem.trimmed(grow), 16f, style.accent)
        fun leaf(px: Float, py: Float, size: Float, rot: Float, k: Float) {
            if (k <= 0f) return
            g.save(); g.rotate(rot + sway, px, py); g.scale(k, px, py)
            val p = PathData().moveTo(px, py).cubicTo(px + size * 0.4f, py - size * 0.55f, px + size, py - size * 0.4f, px + size * 1.15f, py)
                .cubicTo(px + size, py + size * 0.35f, px + size * 0.4f, py + size * 0.4f, px, py).close()
            g.fillPath(p, style.accent)
            g.line(px, py, px + size * 1.05f, py, 4f, style.bgBottom.withAlpha(0.45f))
            g.restore()
        }
        leaf(cx + 6f, cy + 40f, 150f, -25f, Easing.BACK_OUT.ease(window(t, 900f, 700f)))
        leaf(cx - 2f, cy - 30f, 130f, 205f, Easing.BACK_OUT.ease(window(t, 1200f, 700f)))
        leaf(cx + sway, cy - 158f, 110f, -60f, Easing.BACK_OUT.ease(window(t, 1500f, 700f)))
        // Soleil.
        val sun = Easing.EXPO_OUT.ease(window(t, 200f, 1200f))
        g.fillCircle(cx + 210f, cy - 190f, 58f * sun, style.accent2)
    }

    private fun chartGrowth(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 440f, style.accent.withAlpha(0.25f))
        orbit(g, cx, cy, 330f, tg)
        val base = cy + 200f
        val heights = floatArrayOf(120f, 190f, 260f, 360f)
        for ((i, h) in heights.withIndex()) {
            val k = Easing.BACK_OUT.ease(window(t, 300f + i * 180f, 700f))
            val l = cx - 230f + i * 125f
            g.fillRoundRect(l, base - h * k, l + 90f, base, 14f, if (i == heights.lastIndex) style.accent else style.accent2.withAlpha(0.85f))
        }
        val arrow = PathData().moveTo(cx - 240f, base - 140f).lineTo(cx - 80f, base - 230f).lineTo(cx + 40f, base - 190f).lineTo(cx + 230f, base - 400f)
        val k = Easing.EASE_IN_OUT.ease(window(t, 1100f, 900f))
        g.strokePath(arrow.trimmed(k), 14f, style.text)
        if (k >= 1f) {
            val head = PathData().moveTo(cx + 250f, base - 425f).lineTo(cx + 236f, base - 350f).lineTo(cx + 175f, base - 395f).close()
            g.fillPath(head, style.text)
        }
    }

    private fun tile(g: Gfx, cx: Float, cy: Float, half: Float, icon: IconId, color: Int, k: Float) {
        if (k <= 0f) return
        g.save(); g.scale(k, cx, cy)
        g.fillRoundRect(cx - half, cy - half + 10f, cx + half, cy + half + 10f, half * 0.35f, style.shadow)
        g.fillRoundRect(cx - half, cy - half, cx + half, cy + half, half * 0.35f, style.surface)
        g.strokeRoundRect(cx - half, cy - half, cx + half, cy + half, half * 0.35f, 3f, color)
        Icons.draw(g, icon, cx, cy, half * 1.15f, color, 2.2f)
        g.restore()
    }

    private fun networkTopology(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 440f, style.accent.withAlpha(0.20f))
        val pts = listOf(cx - 270f to cy - 170f, cx + 270f to cy - 170f, cx - 270f to cy + 190f, cx + 270f to cy + 190f, cx to cy - 300f)
        val icons = listOf(IconId.CPU, IconId.TERMINAL, IconId.WIFI, IconId.GLOBE, IconId.CPU)
        for ((i, p) in pts.withIndex()) {
            val k = Easing.EASE_IN_OUT.ease(window(t, 500f + i * 140f, 500f))
            if (k > 0f) g.line(cx, cy, lerp(cx, p.first, k), lerp(cy, p.second, k), 4f, style.accent.withAlpha(0.6f))
            if (k >= 1f) {
                val ph = ((tg + i * 430f) % 1800f) / 1800f
                g.fillCircle(lerp(cx, p.first, ph), lerp(cy, p.second, ph), 9f, style.accent2)
            }
        }
        for ((i, p) in pts.withIndex()) tile(g, p.first, p.second, 62f, icons[i], if (i % 2 == 0) style.accent else style.accent2,
            Easing.BACK_OUT.ease(window(t, 700f + i * 140f, 600f)))
        tile(g, cx, cy, 92f, IconId.ROUTER, style.accent, Easing.BACK_OUT.ease(window(t, 200f, 700f)))
    }

    private fun windowFrame(g: Gfx, l: Float, top: Float, r: Float, b: Float) {
        g.fillRoundRect(l, top + 18f, r, b + 18f, 26f, style.shadow)
        g.fillRoundRect(l, top, r, b, 26f, style.surface)
        g.strokeRoundRect(l, top, r, b, 26f, 2f, style.surfaceStroke)
        g.fillRoundRect(l, top, r, top + 56f, 26f, style.surfaceStroke.withAlpha(0.6f))
        g.fillRect(l, top + 30f, r, top + 56f, style.surfaceStroke.withAlpha(0.6f))
        for ((k, c) in listOf(rgb(0xFF5F57), rgb(0xFEBC2E), rgb(0x28C840)).withIndex()) g.fillCircle(l + 34f + k * 30f, top + 28f, 9f, c)
    }

    private fun codeWindow(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 440f, style.accent2.withAlpha(0.18f))
        val l = cx - 300f; val r = cx + 300f; val top = cy - 230f; val b = cy + 250f
        windowFrame(g, l, top, r, b)
        val widths = floatArrayOf(0.55f, 0.75f, 0.42f, 0.68f, 0.3f, 0.6f, 0.5f)
        val indents = intArrayOf(0, 1, 2, 2, 1, 1, 0)
        val colors = intArrayOf(style.accent2, style.emphasis, style.accent, style.highlight, style.accent2, style.accent, style.emphasis)
        val typed = window(t, 400f, 2200f) * widths.size
        for (i in widths.indices) {
            val y = top + 96f + i * 52f
            g.fillRoundRect(l + 26f, y - 8f, l + 46f, y + 8f, 4f, style.textMuted.withAlpha(0.3f))
            val frac = clamp01(typed - i)
            if (frac <= 0f) continue
            val x0 = l + 70f + indents[i] * 40f
            val w = (r - 40f - x0) * widths[i] * frac
            g.fillRoundRect(x0, y - 11f, x0 + w * 0.4f, y + 11f, 8f, colors[i])
            if (w > 60f) g.fillRoundRect(x0 + w * 0.4f + 14f, y - 11f, x0 + w, y + 11f, 8f, style.text.withAlpha(0.55f))
            if (frac < 1f || (i == widths.lastIndex && ((tg / 500f).toInt() % 2 == 0))) g.fillRect(x0 + w + 10f, y - 16f, x0 + w + 16f, y + 16f, style.accent)
        }
        tile(g, r - 10f, b - 10f, 70f, IconId.CODE, style.accent, Easing.BACK_OUT.ease(window(t, 1500f, 600f)))
    }

    private fun osTerminal(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 440f, style.accent.withAlpha(0.18f))
        // Deux fenêtres décalées : une application et un terminal.
        val back = Easing.EXPO_OUT.ease(window(t, 200f, 800f))
        g.save(); g.translate(-60f * back, -50f * back)
        windowFrame(g, cx - 280f, cy - 250f, cx + 220f, cy + 150f)
        for (i in 0 until 3) g.fillRoundRect(cx - 250f, cy - 160f + i * 70f, cx - 250f + 330f - i * 60f, cy - 130f + i * 70f, 10f, style.surfaceStroke)
        g.restore()
        val l = cx - 200f; val top = cy - 120f; val r = cx + 320f; val b = cy + 270f
        windowFrame(g, l, top, r, b)
        g.fillRoundRect(l + 2f, top + 56f, r - 2f, b - 2f, 24f, if (style.dark) rgb(0x05080F) else rgb(0x1E2430))
        val mono = FontSpec(FontFamily.MONO, 700, 34f)
        for (i in 0 until 4) {
            val y = top + 120f + i * 52f
            val k = window(t, 600f + i * 450f, 400f)
            if (k <= 0f) continue
            g.drawText(">", l + 30f, y + 12f, mono, style.accent)
            g.fillRoundRect(l + 70f, y - 8f, l + 70f + (r - l - 140f) * (0.35f + 0.15f * (i % 3)) * k, y + 8f, 8f, rgb(0xE6EDF3).withAlpha(0.75f))
        }
        if (((tg / 500f).toInt() % 2) == 0) g.fillRect(l + 30f, b - 60f, l + 50f, b - 30f, style.accent)
    }

    private fun neuralNet(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 460f, style.accent2.withAlpha(0.20f))
        val layers = listOf(3, 4, 4, 2)
        val xs = listOf(cx - 300f, cx - 100f, cx + 100f, cx + 300f)
        fun ys(n: Int) = List(n) { cy + (it - (n - 1) / 2f) * 130f }
        for (li in 0 until layers.size - 1) {
            val a = ys(layers[li]); val b = ys(layers[li + 1])
            val k = Easing.EASE_IN_OUT.ease(window(t, 300f + li * 300f, 500f))
            for (y0 in a) for (y1 in b) if (k > 0f) g.line(xs[li], y0, lerp(xs[li], xs[li + 1], k), lerp(y0, y1, k), 2f, style.text.withAlpha(0.18f))
        }
        // Signal qui traverse le réseau en boucle.
        val ph = (tg % 2400f) / 2400f * (layers.size - 1)
        val li = ph.toInt().coerceAtMost(layers.size - 2)
        val f = ph - li
        if (t > 1400f) for (y0 in ys(layers[li])) for (y1 in ys(layers[li + 1]).take(2)) {
            g.fillCircle(lerp(xs[li], xs[li + 1], f), lerp(y0, y1, f), 6f, style.accent2.withAlpha(0.8f))
        }
        for ((i, n) in layers.withIndex()) for ((j, y) in ys(n).withIndex()) {
            val k = Easing.BACK_OUT.ease(window(t, 200f + i * 250f + j * 60f, 500f))
            if (k <= 0f) continue
            val lit = li == i && t > 1400f
            g.fillCircle(xs[i], y, 34f * k, if (lit) style.accent2 else style.surface)
            g.strokeCircle(xs[i], y, 34f * k, 4f, style.accent)
        }
    }

    private fun heartBeat(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy - 20f, 440f, style.accent.withAlpha(0.20f))
        orbit(g, cx, cy, 320f, tg)
        val beat = (tg % 1000f) / 1000f
        val pulse = 1f + 0.08f * (if (beat < 0.15f) sin(beat / 0.15f * PI.toFloat()) else 0f) +
            0.05f * (if (beat in 0.2f..0.35f) sin((beat - 0.2f) / 0.15f * PI.toFloat()) else 0f)
        val size = 320f * Easing.BACK_OUT.ease(window(t, 100f, 800f)) * pulse
        if (size > 1f) {
            val heart = Icons.parts(IconId.HEART)[0].path.transformed(size / 24f, cx - size / 2, cy - size / 2 - 40f)
            g.fillPath(heart, style.accent)
        }
        val ecg = PathData().moveTo(cx - 300f, cy + 210f).lineTo(cx - 120f, cy + 210f).lineTo(cx - 80f, cy + 170f).lineTo(cx - 40f, cy + 260f)
            .lineTo(cx, cy + 120f).lineTo(cx + 40f, cy + 240f).lineTo(cx + 80f, cy + 210f).lineTo(cx + 300f, cy + 210f)
        val k = window(t, 700f, 1200f)
        if (k > 0f) g.strokePath(ecg.trimmed(k), 9f, style.accent2)
    }

    private fun bubble(g: Gfx, l: Float, top: Float, r: Float, b: Float, tailLeft: Boolean, fill: Int, text: String, color: Int, k: Float) {
        if (k <= 0f) return
        val px = if (tailLeft) l + 40f else r - 40f
        g.save(); g.scale(k, px, b)
        g.fillRoundRect(l, top + 14f, r, b + 14f, 44f, style.shadow)
        g.fillRoundRect(l, top, r, b, 44f, fill)
        val tail = if (tailLeft) PathData().moveTo(l + 40f, b - 10f).lineTo(l + 20f, b + 50f).lineTo(l + 100f, b - 10f).close()
        else PathData().moveTo(r - 40f, b - 10f).lineTo(r - 20f, b + 50f).lineTo(r - 100f, b - 10f).close()
        g.fillPath(tail, fill)
        val f = FontSpec(FontFamily.DISPLAY, 800, 64f)
        g.drawText(text, (l + r) / 2 - g.measureText(text, f) / 2, (top + b) / 2 + g.metrics(f).ascent / 2, f, color)
        g.restore()
    }

    private fun speechBubbles(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 440f, style.accent.withAlpha(0.18f))
        val bob = 8f * sin(tg * 0.002f)
        bubble(g, cx - 320f, cy - 260f + bob, cx + 120f, cy - 80f + bob, true, style.accent, "Hello!", style.accentInk,
            Easing.BACK_OUT.ease(window(t, 200f, 600f)))
        bubble(g, cx - 120f, cy + 40f - bob, cx + 320f, cy + 220f - bob, false, style.accent2, "Bonjour\u00A0!", style.highlightInk,
            Easing.BACK_OUT.ease(window(t, 900f, 600f)))
    }
}

/** Séparateur « libellé : détail » (espace normale ou insécable avant les deux-points). */
internal val LABEL_SEP = Regex("[  ]:\\s")
