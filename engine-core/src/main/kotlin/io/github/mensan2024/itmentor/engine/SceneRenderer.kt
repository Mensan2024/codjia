// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Dessine le contenu d'une scène à un instant local donné (ms depuis son début).
 * Chaque composition (Layout) place ses éléments puis les anime avec l'Animator.
 * Les mises en page de texte sont mises en cache : on ne recalcule les retours à la ligne qu'une fois.
 */
class SceneRenderer(private val style: Style) {

    private val cache = HashMap<String, TextBlock>()

    private fun block(g: Gfx, key: String, text: String, font: FontSpec, width: Float, maxLines: Int, lh: Float = 1.18f) =
        cache.getOrPut(key) { TextLayout.fit(g, text, font, width, maxLines, lineHeightFactor = lh) }

    // Polices de la charte : Inter Display pour les titres, Inter pour le texte, JetBrains Mono pour le code.
    private val fTitle = FontSpec(FontFamily.DISPLAY, 800, 88f)
    private val fBody = FontSpec(FontFamily.TEXT, 500, 46f)
    private val fItem = FontSpec(FontFamily.TEXT, 700, 42f)
    private val fStepLabel = FontSpec(FontFamily.TEXT, 700, 48f)
    private val fItemDetail = FontSpec(FontFamily.TEXT, 500, 40f)
    private val fKicker = FontSpec(FontFamily.TEXT, 700, 26f)
    private val fSub = FontSpec(FontFamily.TEXT, 500, 40f)
    private val fMono = FontSpec(FontFamily.MONO, 400, 38f)

    fun draw(g: Gfx, sc: Scene, t: Float, tGlobal: Float) {
        val dur = sc.durationMs.toFloat()
        val exit = Animator.exit(sc.exit, t, dur)
        when (sc.layout) {
            Layout.HERO -> hero(g, sc, t, dur, exit, tGlobal)
            Layout.DEFINITION -> definition(g, sc, t, dur, exit, tGlobal)
            Layout.STEPS -> steps(g, sc, t, exit)
            Layout.COMPARE -> compare(g, sc, t, exit)
            Layout.STAT -> stat(g, sc, t, exit, tGlobal)
            Layout.CHECKLIST -> checklist(g, sc, t, exit, tGlobal)
        }
    }

    /** Pose d'un élément : type d'animation de la scène, décalé de `delay`. */
    private fun p(sc: Scene, t: Float, exit: Pose, delay: Int, type: AnimType = sc.enter.type, dur: Int = sc.enter.durationMs,
                  easing: Easing = sc.enter.easing): Pose =
        Animator.enter(sc.enter.copy(type = type, delayMs = sc.enter.delayMs + delay, durationMs = dur, easing = easing), t) + exit

    // ───────────────────────────── Éléments communs ─────────────────────────────

    private fun kicker(g: Gfx, text: String, x: Float, cy: Float, align: Align): Float {
        val tracking = 3.2f
        val w = g.measureTracked(text, fKicker, tracking) + 48f
        val l = if (align == Align.CENTER) x - w / 2 else x
        g.fillRoundRect(l, cy - 28f, l + w, cy + 28f, 28f, style.accent.withAlpha(0.12f))
        g.strokeRoundRect(l, cy - 28f, l + w, cy + 28f, 28f, 2f, style.accent.withAlpha(0.55f))
        val m = g.metrics(fKicker)
        g.drawTracked(text, l + 24f, cy + m.ascent / 2f, fKicker, style.accent, tracking)
        return w
    }

    /** Pastille d'icône : disque + anneau tournant en pointillés (élément décoratif animé). */
    private fun iconBadge(g: Gfx, id: IconId, cx: Float, cy: Float, r: Float, tGlobal: Float, color: Int = style.accent) {
        g.fillCircle(cx, cy, r, style.surface)
        g.strokeCircle(cx, cy, r, 3f, color.withAlpha(0.8f))
        val rot = (tGlobal * 0.03f) % 360f
        for (k in 0 until 12) g.strokeArc(cx, cy, r + 20f, rot + k * 30f, 14f, 3f, color.withAlpha(0.45f))
        Icons.draw(g, id, cx, cy, r * 1.05f, color, strokeWidth = 2.1f)
    }

    private fun caretOn(t: Float, reveal: Float) = reveal < 1f || ((t / 420f).toInt() % 2 == 0)

    /** Forme géométrique décorative qui tourne lentement. */
    private fun shape(g: Gfx, kind: ShapeKind, cx: Float, cy: Float, r: Float, tGlobal: Float, color: Int) {
        val rot = tGlobal * 0.006f
        val path = when (kind) {
            ShapeKind.RING -> PathData().circle(cx, cy, r)
            ShapeKind.HEXAGON -> PathData().polygon(cx, cy, r, 6, rot)
            ShapeKind.DIAMOND -> PathData().polygon(cx, cy, r, 4, rot)
        }
        g.strokePath(path, 4f, color)
        g.strokePath(when (kind) {
            ShapeKind.RING -> PathData().circle(cx, cy, r * 0.72f)
            ShapeKind.HEXAGON -> PathData().polygon(cx, cy, r * 0.72f, 6, -rot * 1.5f)
            ShapeKind.DIAMOND -> PathData().polygon(cx, cy, r * 0.72f, 4, -rot * 1.5f)
        }, 2f, color.withAlpha(0.6f))
    }

    // ───────────────────────────── 1. Couverture ─────────────────────────────

    private fun hero(g: Gfx, sc: Scene, t: Float, dur: Float, exit: Pose, tg: Float) {
        val cx = Safe.CENTER_X
        // Illustration : zoom arrière à l'entrée.
        g.withPose(p(sc, t, exit, 0, AnimType.ZOOM_OUT, 1000), cx, 600f) {
            illustration(g, sc.illustration, cx, 600f, t, tg)
        }
        var y = 990f
        sc.kicker?.let { k ->
            g.withPose(p(sc, t, exit, 250), cx, y) { kicker(g, k, cx, y, Align.CENTER) }
            y += 64f
        }
        val title = block(g, "${sc.id}/title", sc.title, fTitle.withSize(92f), Safe.WIDTH, 3, 1.1f)
        g.withPose(p(sc, t, exit, 380), cx, y) { g.drawBlock(title, cx, y, style.text, Align.CENTER) }
        y += title.height + 28f
        // Soulignement safran qui s'étire.
        val bar = Easing.EXPO_OUT.ease(window(t, 900f, 700f))
        if (bar > 0f) g.withPose(exit, cx, y) { g.fillRoundRect(cx - 120f * bar, y, cx + 120f * bar, y + 10f, 5f, style.highlight) }
        y += 48f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/sub", s, fSub.withSize(42f), Safe.WIDTH - 40f, 3, 1.35f)
            g.withPose(p(sc, t, exit, 1000, AnimType.FADE_IN, 700), cx, y) { g.drawBlock(sub, cx, y, style.textMuted, Align.CENTER) }
        }
    }

    // ───────────────────────────── 2. Définition ─────────────────────────────

    private fun definition(g: Gfx, sc: Scene, t: Float, dur: Float, exit: Pose, tg: Float) {
        val x = Safe.LEFT
        val badgeCy = 330f
        sc.icon?.let { id ->
            g.withPose(p(sc, t, exit, 0, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), x + 64f, badgeCy) {
                iconBadge(g, id, x + 64f, badgeCy, 62f, tg)
            }
        }
        sc.kicker?.let { k -> g.withPose(p(sc, t, exit, 150, AnimType.SLIDE_LEFT), x + 170f, badgeCy) { kicker(g, k, x + 170f, badgeCy, Align.LEFT) } }

        var y = 470f
        val title = block(g, "${sc.id}/title", sc.title, fTitle.withSize(90f), Safe.WIDTH, 3, 1.1f)
        g.withPose(p(sc, t, exit, 250), x, y) { g.drawBlock(title, x, y, style.text) }
        y += title.height + 64f

        // Carte de définition, texte en machine à écrire.
        val pad = 48f
        val body = block(g, "${sc.id}/body", sc.body, fBody.withSize(52f), Safe.WIDTH - pad * 2 - 12f, 7, 1.42f)
        val cardTop = y
        val cardBottom = y + body.height + pad * 2
        g.withPose(p(sc, t, exit, 450, AnimType.SLIDE_UP), x, cardTop) {
            g.fillRoundRect(x, cardTop, Safe.RIGHT, cardBottom, 32f, style.surface.withAlpha(0.92f))
            g.strokeRoundRect(x, cardTop, Safe.RIGHT, cardBottom, 32f, 2f, style.surfaceStroke)
            g.fillRoundRect(x, cardTop + 28f, x + 10f, cardBottom - 28f, 5f, style.accent)
        }
        val typeMs = (body.charCount * 26f).coerceIn(1200f, dur - 2200f)
        val typing = Animator.enter(AnimSpec(AnimType.TYPEWRITER, typeMs.toInt(), 900, Easing.LINEAR), t)
        g.withPose(exit, x, cardTop) {
            g.drawBlock(body, x + pad + 12f, cardTop + pad, style.text, reveal = typing.reveal,
                caretColor = style.highlight, caretVisible = caretOn(t, typing.reveal))
        }
        y = cardBottom + 56f
        sc.subtitle?.let { s ->
            val sub = block(g, "${sc.id}/sub", s, fSub.withSize(42f), Safe.WIDTH, 3, 1.35f)
            g.withPose(p(sc, t, exit, (900 + typeMs).toInt(), AnimType.FADE_IN, 600), x, y) { g.drawBlock(sub, x, y, style.textMuted) }
        }
        // Forme géométrique en bas à droite, à moitié hors champ.
        g.withPose(p(sc, t, exit, 300, AnimType.FADE_IN, 1200), 940f, 1480f) {
            shape(g, sc.shape, 940f, 1480f, 230f, tg, style.accent.withAlpha(0.13f))
        }
    }

    // ───────────────────────────── 3. Étapes ─────────────────────────────

    private fun steps(g: Gfx, sc: Scene, t: Float, exit: Pose) {
        val x = Safe.LEFT
        var y = 250f
        val titleW = if (sc.icon != null) Safe.WIDTH - 170f else Safe.WIDTH
        sc.icon?.let { id -> g.withPose(p(sc, t, exit, 100, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), Safe.RIGHT - 70f, 300f) {
            iconBadge(g, id, Safe.RIGHT - 70f, 300f, 58f, t, style.highlight)
        } }
        val title = block(g, "${sc.id}/title", sc.title, fTitle.withSize(90f), titleW, 2, 1.1f)
        g.withPose(p(sc, t, exit, 0), x, y) { g.drawBlock(title, x, y, style.text) }
        y = max(y + title.height, 420f) + 96f

        val bubbleR = 42f
        val textX = x + bubbleR * 2 + 36f
        val textW = Safe.RIGHT - textX
        data class Row(val top: Float, val label: TextBlock, val detail: TextBlock?, val h: Float)
        val rows = ArrayList<Row>()
        for ((i, item) in sc.items.withIndex()) {
            val parts = item.split(" : ", limit = 2)
            val label = block(g, "${sc.id}/i$i", parts[0], fStepLabel, textW, 2, 1.22f)
            val detail = parts.getOrNull(1)?.let { block(g, "${sc.id}/d$i", it, fItemDetail, textW, 2, 1.3f) }
            val h = max(bubbleR * 2, label.height + (detail?.height?.plus(6f) ?: 0f))
            rows += Row(y, label, detail, h)
            y += h + 72f
        }
        // Ligne qui relie les pastilles, tracée au rythme des étapes.
        if (rows.size > 1) {
            val first = rows.first().top + bubbleR
            val last = rows.last().top + bubbleR
            val stepMs = 520f
            val prog = clamp01((t - 500f) / (stepMs * (rows.size - 1) + 400f))
            g.withPose(exit, 0f, 0f) {
                g.line(x + bubbleR, first, x + bubbleR, first + (last - first) * Easing.EASE_IN_OUT.ease(prog), 4f, style.surfaceStroke)
            }
        }
        for ((i, r) in rows.withIndex()) {
            val delay = 500 + i * 520
            g.withPose(p(sc, t, exit, delay, AnimType.ZOOM_IN, 600, Easing.BACK_OUT), x + bubbleR, r.top + bubbleR) {
                g.fillCircle(x + bubbleR, r.top + bubbleR, bubbleR, style.accent)
                val num = (i + 1).toString()
                val f = FontSpec(FontFamily.DISPLAY, 800, 42f)
                val m = g.metrics(f)
                g.drawText(num, x + bubbleR - g.measureText(num, f) / 2, r.top + bubbleR + m.ascent / 2, f, style.accentInk)
            }
            g.withPose(p(sc, t, exit, delay + 80, AnimType.SLIDE_LEFT, 650), textX, r.top) {
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
        val title = block(g, "${sc.id}/title", sc.title, fTitle.withSize(84f), Safe.WIDTH, 2, 1.1f)
        g.withPose(p(sc, t, exit, 100), x, y) { g.drawBlock(title, x, y, style.text) }
        y += title.height + 64f

        val cardH = 300f
        val gap = 44f
        val aTop = y
        val bTop = y + cardH + gap
        val urlY = aTop + 200f

        // Loupe qui balaye l'adresse piégée (déplacement animé le long d'un chemin).
        val move = Easing.EASE_IN_OUT.ease(window(t, 1700f, 1500f))
        val magX = lerp(x + 90f, Safe.RIGHT - 110f, move)
        val magY = urlY - 10f - 26f * sin(move * PI.toFloat())
        val magAlpha = window(t, 1500f, 250f) * (1f - window(t, 3400f, 350f))

        card(g, sc, t, exit, aTop, cardH, cmp.badLabel, cmp.badValue, style.danger, IconId.CROSS, AnimType.SLIDE_RIGHT, 350,
            underlineToX = if (t > 1700f) magX + 30f else -1f)
        card(g, sc, t, exit, bTop, cardH, cmp.goodLabel, cmp.goodValue, style.ok, IconId.CHECK, AnimType.SLIDE_LEFT, 850,
            underlineToX = -1f)

        if (magAlpha > 0f) g.withPose(exit + Pose(alpha = magAlpha), magX, magY) {
            g.fillCircle(magX - 8f, magY - 8f, 44f, style.highlight.withAlpha(0.16f))
            Icons.draw(g, IconId.SEARCH, magX, magY, 120f, style.highlight, strokeWidth = 2.4f)
        }
        sc.subtitle?.let { s ->
            val top = bTop + cardH + 56f
            val sub = block(g, "${sc.id}/sub", s, fSub.withSize(40f), Safe.WIDTH - 70f, 3, 1.35f)
            g.withPose(p(sc, t, exit, 2600, AnimType.SLIDE_UP, 700), x, top) {
                Icons.draw(g, IconId.EYE, x + 24f, top + sub.lineHeight / 2, 46f, style.highlight, 2.2f)
                g.drawBlock(sub, x + 70f, top, style.textMuted)
            }
        }
    }

    private fun card(
        g: Gfx, sc: Scene, t: Float, exit: Pose, top: Float, h: Float, label: String, value: List<Span>,
        color: Int, icon: IconId, anim: AnimType, delay: Int, underlineToX: Float,
    ) {
        val l = Safe.LEFT
        val r = Safe.RIGHT
        g.withPose(p(sc, t, exit, delay, anim, 750) + Pose(), l, top) {
            g.fillRoundRect(l, top, r, top + h, 32f, style.surface.withAlpha(0.95f))
            g.strokeRoundRect(l, top, r, top + h, 32f, 3f, color.withAlpha(0.75f))
            g.fillCircle(l + 74f, top + 78f, 36f, color.withAlpha(0.18f))
            Icons.draw(g, icon, l + 74f, top + 78f, 44f, color, 2.6f)
            val lf = FontSpec(FontFamily.TEXT, 700, 40f)
            g.drawText(label, l + 132f, top + 78f + g.metrics(lf).ascent / 2, lf, color)
            // Adresse en police mono, réduite si elle ne tient pas.
            g.fillRoundRect(l + 36f, top + 140f, r - 36f, top + 250f, 22f, style.bgTop.withAlpha(0.85f))
            val avail = (r - 36f) - (l + 36f) - 56f
            val full = value.joinToString("") { it.text }
            var font = fMono
            while (g.measureText(full, font) > avail && font.size > 22f) font = font.withSize(font.size - 1f)
            val m = g.metrics(font)
            val base = top + 195f + m.ascent / 2
            var cx = l + 64f
            for (s in value) {
                val w = g.measureText(s.text, font)
                g.drawText(s.text, cx, base, font, if (s.highlight) color else style.text)
                if (s.highlight && underlineToX > cx) {
                    g.fillRoundRect(cx, base + 14f, min(cx + w, underlineToX), base + 20f, 3f, color)
                }
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
            // Graduations qui tournent lentement.
            val rot = tg * 0.008f
            for (k in 0 until 60) {
                val a = Math.toRadians((rot + k * 6f).toDouble())
                val r0 = if (k % 5 == 0) 272f else 282f
                g.line(cx + (r0 * kotlin.math.cos(a)).toFloat(), cy + (r0 * kotlin.math.sin(a)).toFloat(),
                    cx + (296f * kotlin.math.cos(a)).toFloat(), cy + (296f * kotlin.math.sin(a)).toFloat(), 3f,
                    style.text.withAlpha(if (k % 5 == 0) 0.28f else 0.12f))
            }
            g.strokeCircle(cx, cy, r, 26f, style.surface)
        }
        // Anneau de progression : un vrai minuteur de 3 s, calé sur le temps de rendu.
        val prog = window(t, 600f, 3000f)
        if (prog > 0f) g.withPose(exit, cx, cy) {
            g.strokeArc(cx, cy, r, -90f, 360f * prog, 26f, style.highlight)
        }
        sc.statValue?.let { v ->
            val f = FontSpec(FontFamily.DISPLAY, 800, 190f)
            g.withPose(p(sc, t, exit, 150, AnimType.ZOOM_IN, 900, Easing.BACK_OUT), cx, cy) {
                val m = g.metrics(f)
                g.drawText(v, cx - g.measureText(v, f) / 2, cy + m.ascent / 2 - 10f, f, style.text)
            }
        }
        var y = cy + 400f
        val title = block(g, "${sc.id}/title", sc.title, fTitle.withSize(82f), Safe.WIDTH, 2, 1.1f)
        g.withPose(p(sc, t, exit, 500), cx, y) { g.drawBlock(title, cx, y, style.text, Align.CENTER) }
        y += title.height + 36f
        if (sc.body.isNotEmpty()) {
            val body = block(g, "${sc.id}/body", sc.body, fSub.withSize(44f), Safe.WIDTH - 40f, 4, 1.38f)
            g.withPose(p(sc, t, exit, 800), cx, y) { g.drawBlock(body, cx, y, style.textMuted, Align.CENTER) }
        }
    }

    // ───────────────────────────── 6. À retenir ─────────────────────────────

    private fun checklist(g: Gfx, sc: Scene, t: Float, exit: Pose, tg: Float) {
        val cx = Safe.CENTER_X
        g.withPose(p(sc, t, exit, 0, AnimType.ZOOM_IN, 800, Easing.BACK_OUT), cx, 350f) {
            illustration(g, sc.illustration, cx, 350f, t, tg)
        }
        var y = 530f
        val title = block(g, "${sc.id}/title", sc.title, fTitle.withSize(86f), Safe.WIDTH, 2, 1.1f)
        g.withPose(p(sc, t, exit, 200), cx, y) { g.drawBlock(title, cx, y, style.text, Align.CENTER) }
        y += title.height + 54f

        val box = 60f
        val x = Safe.LEFT + 20f
        val textX = x + box + 30f
        for ((i, item) in sc.items.withIndex()) {
            val b = block(g, "${sc.id}/c$i", item, fItem.copy(weight = 600), Safe.RIGHT - textX, 2, 1.25f)
            val h = max(box, b.height)
            val top = y
            val delay = 500 + i * 430
            g.withPose(p(sc, t, exit, delay, AnimType.SLIDE_LEFT, 600), x, top) {
                val bt = top + (h - box) / 2
                val checked = window(t, delay + 300f, 420f)
                g.fillRoundRect(x, bt, x + box, bt + box, 16f, style.ok.withAlpha(0.16f * checked))
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
            val sub = block(g, "${sc.id}/sub", s, fSub.withSize(38f), Safe.WIDTH - 30f, 3, 1.35f)
            g.withPose(p(sc, t, exit, 500 + sc.items.size * 430 + 200, AnimType.FADE_IN, 700), cx, y) {
                g.drawBlock(sub, cx, y, style.textMuted, Align.CENTER)
            }
            y += sub.height + 44f
        }
        if (sc.body.isNotEmpty()) {
            // Appel à l'action en pastille safran.
            val f = FontSpec(FontFamily.TEXT, 700, 38f)
            val w = min(g.measureText(sc.body, f) + 80f, Safe.WIDTH)
            g.withPose(p(sc, t, exit, 500 + sc.items.size * 430 + 600, AnimType.ZOOM_IN, 700, Easing.BACK_OUT), cx, y + 46f) {
                g.fillRoundRect(cx - w / 2, y, cx + w / 2, y + 92f, 46f, style.highlight)
                val m = g.metrics(f)
                g.drawText(sc.body, cx - g.measureText(sc.body, f) / 2, y + 46f + m.ascent / 2, f, style.highlightInk)
            }
        }
    }

    // ───────────────────────────── Illustrations ─────────────────────────────

    private fun illustration(g: Gfx, id: IllustrationId, cx: Float, cy: Float, t: Float, tg: Float) {
        when (id) {
            IllustrationId.NONE -> Unit
            IllustrationId.PHISHING_MAIL -> phishingMail(g, cx, cy, t, tg)
            IllustrationId.SHIELD_CHECK -> shieldCheck(g, cx, cy, t, tg)
            IllustrationId.TIMER_RING -> Unit
        }
    }

    private fun phishingMail(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy + 20f, 420f, style.accent.withAlpha(0.22f))
        // Orbite + trois points qui tournent (décor animé).
        g.strokeCircle(cx, cy + 20f, 330f, 2f, style.text.withAlpha(0.08f))
        for (k in 0 until 3) {
            val a = Math.toRadians((tg * 0.025f + k * 120f).toDouble())
            g.fillCircle(cx + (330f * kotlin.math.cos(a)).toFloat(), cy + 20f + (330f * kotlin.math.sin(a)).toFloat(), 9f,
                if (k == 0) style.highlight else style.accent.withAlpha(0.8f))
        }
        // Enveloppe.
        val w = 460f
        val h = 310f
        val l = cx - w / 2
        val top = cy - h / 2 + 70f
        g.fillRoundRect(l, top, l + w, top + h, 34f, style.surface)
        g.strokeRoundRect(l, top, l + w, top + h, 34f, 4f, style.surfaceStroke)
        g.strokePath(PathData().moveTo(l + 34f, top + 40f).lineTo(cx, top + 175f).lineTo(l + w - 34f, top + 40f), 9f, style.accent)
        g.strokePath(PathData().moveTo(l + 40f, top + h - 40f).lineTo(cx - 70f, top + 170f), 4f, style.surfaceStroke)
        g.strokePath(PathData().moveTo(l + w - 40f, top + h - 40f).lineTo(cx + 70f, top + 170f), 4f, style.surfaceStroke)
        // Hameçon qui descend puis se balance (déplacement animé).
        val hookX = cx + 120f
        val drop = Easing.EXPO_OUT.ease(window(t, 400f, 1300f))
        val bob = if (drop >= 1f) 12f * sin((t - 1700f) * 0.004f) else 0f
        val hookY = lerp(-120f, top - 40f, drop) + bob
        if (hookY > 0f) {
            // L'œillet de l'hameçon est en (15 ; 3,6) sur la grille 24 : le fil s'y accroche exactement.
            val size = 130f
            val k = size / 24f
            val eyeX = hookX - 18f - size / 2 + 15f * k
            val eyeTop = hookY - size / 2 + (3.6f - 1.6f) * k
            g.line(eyeX, 0f, eyeX, eyeTop, 3f, style.text.withAlpha(0.55f))
            Icons.draw(g, IconId.HOOK, hookX - 18f, hookY, size, style.highlight, 2.6f)
        }
        // Pastille d'alerte qui « pop » une fois l'hameçon arrivé.
        val pop = Easing.BACK_OUT.ease(window(t, 1500f, 600f))
        if (pop > 0f) {
            val bx = l + 36f
            val by = top - 6f
            g.save(); g.scale(pop, bx, by)
            g.fillCircle(bx, by, 52f, style.danger)
            Icons.draw(g, IconId.WARNING, bx, by - 2f, 58f, style.highlightInk.withAlpha(0.9f), 2.4f)
            g.restore()
        }
    }

    private fun shieldCheck(g: Gfx, cx: Float, cy: Float, t: Float, tg: Float) {
        g.fillRadialGlow(cx, cy, 260f, style.ok.withAlpha(0.22f))
        val rot = (tg * 0.02f) % 360f
        for (k in 0 until 16) g.strokeArc(cx, cy, 160f, rot + k * 22.5f, 11f, 4f, style.ok.withAlpha(0.5f))
        val size = 220f
        val s = size / 24f
        val shield = Icons.parts(IconId.SHIELD)[0].path.transformed(s, cx - size / 2, cy - size / 2)
        g.fillPath(shield, style.ok.withAlpha(0.14f))
        g.strokePath(shield, 9f, style.ok)
        val check = PathData().moveTo(cx - 46f, cy + 4f).lineTo(cx - 12f, cy + 38f).lineTo(cx + 50f, cy - 34f)
        val k = Easing.EASE_IN_OUT.ease(window(t, 500f, 700f))
        if (k > 0f) g.strokePath(check.trimmed(k), 16f, style.highlight)
    }
}
