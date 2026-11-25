// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.atan2
import kotlin.math.sin

/**
 * Format « Tableau blanc » : une main tenant un feutre écrit et dessine chaque scène sur une feuille blanche.
 * Écriture manuscrite (Kalam, OFL), croquis tremblés, couleurs de feutre. Tout est dessiné en vectoriel :
 * aucune image externe, la main est une illustration originale.
 */
internal class WhiteboardLook(private val sb: Storyboard) : LookRenderer {
    private val r = Rand.of(sb.seed, 0x7AB1)
    private val markers = listOf(
        rgb(0xD63A3A) to rgb(0x1F5FBF), rgb(0x1F5FBF) to rgb(0xE07B00),
        rgb(0x1E8A4C) to rgb(0xD63A3A), rgb(0x6B3FB5) to rgb(0x0F8A8A),
    )
    private val pair = markers[(r.nextInt() ushr 1) % markers.size]
    private val paper = rgb(0xFBFAF6)
    override val ink = rgb(0x1E2228)
    override val muted = rgb(0x5A616B)
    override val accent = pair.first
    override val accent2 = pair.second
    private val ok = rgb(0x1E8A4C)
    private val danger = rgb(0xD63A3A)
    private val skin = listOf(rgb(0x8D5524), rgb(0xC68642), rgb(0x6B4226), rgb(0xE0AC69))[(r.nextInt() ushr 1) % 4]
    private val sleeve = listOf(rgb(0x2B3A55), rgb(0x3C3C3C), rgb(0x7A2E2E), rgb(0x245B4A))[(r.nextInt() ushr 1) % 4]

    private val cache = TextCache()
    private val scripts = HashMap<String, Script>()
    private val home = 1300f to 2250f
    private val TOP = 230f

    private val fTitle = FontSpec(FontFamily.HAND, 700, 104f)
    private val fBody = FontSpec(FontFamily.HAND, 400, 54f)
    private val fSmall = FontSpec(FontFamily.HAND, 400, 44f)
    private val fKicker = FontSpec(FontFamily.HAND, 700, 42f)

    override fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float) {
        background(g, slot.index)
        val s = scripts.getOrPut(slot.scene.id) { build(g, slot.scene).also { it.schedule(300f, slot.scene.durationMs * 0.8f - 300f) } }
        s.draw(g, localMs)
        val (px, py) = s.pen(g, localMs, home)
        drawHand(g, px, py)
    }

    private fun background(g: Gfx, index: Int) {
        g.fillRect(0f, 0f, 1080f, 1920f, paper)
        // Grain léger et coins un peu plus sombres : une vraie feuille, pas un écran blanc.
        val rr = Rand(0x51A7 + index)
        repeat(70) { g.fillCircle(rr.range(0f, 1080f), rr.range(0f, 1920f), rr.range(0.8f, 2.2f), ink.withAlpha(0.045f)) }
        g.fillVerticalGradient(0f, 1700f, 1080f, 1920f, 0x00000000, 0x0F000000)
    }

    // ───────────────────────────── Construction des scènes ─────────────────────────────

    private fun build(g: Gfx, sc: Scene): Script {
        fun pass(y0: Float): Pair<Script, Float> {
            val s = Script()
            val sk = Sketch(sc.id.hashCode() xor sb.seed.toInt())
            val end = when (sc.layout) {
                Layout.HERO -> hero(g, sc, s, sk, y0)
                Layout.DEFINITION -> definition(g, sc, s, sk, y0)
                Layout.STEPS -> steps(g, sc, s, sk, y0)
                Layout.STAT -> stat(g, sc, s, sk, y0)
                Layout.COMPARE -> compare(g, sc, s, sk, y0)
                Layout.CHECKLIST -> checklist(g, sc, s, sk, y0)
                Layout.SCREEN -> y0
            }
            return s to end
        }
        // Premier passage pour mesurer, second passage centré verticalement (centre optique, un peu haut).
        val end = pass(TOP).second
        val shift = ((1500f - end) * 0.42f).coerceIn(0f, 360f)
        return pass(TOP + shift).first
    }

    private fun write(g: Gfx, s: Script, key: String, text: String, font: FontSpec, w: Float, lines: Int, x: Float, top: Float,
                      color: Int = ink, align: Align = Align.LEFT, lh: Float = 1.14f, msPerChar: Float = 42f): TextBlock {
        val marked = Marked.parse(text)
        val b = cache.fit(g, key, marked.plain, font, w, lines, lh)
        s.add(Script.Write(b, MarkedText.runs(b, marked), x, top, color, accent, align, msPerChar))
        return b
    }

    private fun kicker(g: Gfx, sc: Scene, s: Script, sk: Sketch, y: Float): Float {
        val k = sc.kicker ?: return y
        val b = write(g, s, "${sc.id}/k", k.uppercase(), fKicker, Safe.WIDTH, 1, Safe.LEFT, y, accent, msPerChar = 30f)
        val w = g.measureText(b.lines.firstOrNull() ?: "", b.font)
        s.add(Script.Stroke(sk.underline(Safe.LEFT, Safe.LEFT + w, y + b.height + 6f), 6f, accent, 2.5f))
        return y + b.height + 44f
    }

    /** Dessine une icône trait par trait (feutre épais), centrée en (cx, cy). */
    private fun doodle(s: Script, id: IconId, cx: Float, cy: Float, size: Float, color: Int) {
        val k = size / 24f
        for (p in Icons.parts(id)) {
            val path = p.path.transformed(k, cx - size / 2, cy - size / 2)
            if (p.filled) s.add(Script.Appear(120f) { g, a -> g.fillPath(path, color.withAlpha(a)) })
            else s.add(Script.Stroke(path, size / 24f * 1.5f, color, 1.4f))
        }
    }

    private fun hero(g: Gfx, sc: Scene, s: Script, sk: Sketch, y0: Float): Float {
        var y = kicker(g, sc, s, sk, y0)
        val b = write(g, s, "${sc.id}/t", sc.title, fTitle, Safe.WIDTH, 4, Safe.LEFT, y, lh = 1.06f)
        y += b.height + 40f
        val icY = y + 190f
        doodle(s, sc.iconOrDefault(), Safe.CENTER_X, icY, 240f, ink)
        s.add(Script.Stroke(sk.ellipse(Safe.CENTER_X, icY, 190f, 165f), 7f, accent, 2.6f))
        s.add(Script.Stroke(sk.spark(Safe.CENTER_X + 205f, icY - 150f, 60f), 6f, accent2, 1.2f))
        var end = icY + 200f
        sc.subtitle?.let { sub ->
            end = icY + 225f + write(g, s, "${sc.id}/sub", sub, fSmall, Safe.WIDTH, 3, Safe.CENTER_X, icY + 225f, muted, Align.CENTER, 1.25f, 30f).height
        }
        return end
    }

    private fun definition(g: Gfx, sc: Scene, s: Script, sk: Sketch, y0: Float): Float {
        var y = kicker(g, sc, s, sk, y0)
        val t = write(g, s, "${sc.id}/t", sc.title, fTitle.withSize(84f), Safe.WIDTH, 2, Safe.LEFT, y)
        y += t.height + 80f
        val body = Marked.strip(sc.body.ifEmpty { sc.subtitle ?: "" })
        val bb = cache.fit(g, "${sc.id}/b", body, fBody, Safe.WIDTH - 90f, 7, 1.25f)
        s.add(Script.Stroke(sk.rect(Safe.LEFT, y - 40f, Safe.RIGHT, y + bb.height + 40f), 6f, ink, 3.2f))
        s.add(Script.Write(bb, MarkedText.runs(bb, Marked.parse(body)), Safe.LEFT + 45f, y, ink, accent, msPerChar = 30f))
        y += bb.height + 90f
        if (y < 1330f) {
            doodle(s, sc.iconOrDefault(), Safe.RIGHT - 110f, y + 90f, 150f, accent)
            s.add(Script.Stroke(sk.arrow(Safe.RIGHT - 330f, y + 130f, Safe.RIGHT - 210f, y + 95f, -30f), 6f, accent2, 1.5f))
            y += 170f
        }
        return y
    }

    private fun steps(g: Gfx, sc: Scene, s: Script, sk: Sketch, y0: Float): Float {
        var y = kicker(g, sc, s, sk, y0)
        val t = write(g, s, "${sc.id}/t", sc.title, fTitle.withSize(80f), Safe.WIDTH, 2, Safe.LEFT, y)
        y += t.height + 60f
        val n = sc.items.size.coerceAtLeast(1)
        val slot = ((1520f - y) / n).coerceAtMost(260f)
        val textX = Safe.LEFT + 120f
        for ((i, item) in sc.items.withIndex()) {
            val (label, detail) = splitLabel(Marked.strip(item))
            val cy = y + 44f
            s.add(Script.Stroke(sk.ellipse(Safe.LEFT + 42f, cy, 40f, 38f), 6f, accent, 2.4f))
            val nb = cache.fit(g, "${sc.id}/n$i", "${i + 1}", fKicker.withSize(50f), 60f, 1)
            s.add(Script.Write(nb, listOf(listOf("${i + 1}" to false)), Safe.LEFT + 42f, cy - nb.height / 2, accent, accent, Align.CENTER))
            val lb = cache.fit(g, "${sc.id}/l$i", label, fBody.copy(weight = 700).withSize(52f), Safe.RIGHT - textX, if (detail == null && slot > 150f) 2 else 1, 1.12f)
            s.add(Script.Write(lb, MarkedText.runs(lb, Marked.parse(label)), textX, cy - 38f, ink, accent, msPerChar = 34f))
            var bottom = cy - 38f + lb.height
            if (detail != null && slot > 130f) {
                val db = cache.fit(g, "${sc.id}/d$i", detail, fSmall.withSize(40f), Safe.RIGHT - textX, if (slot > 200f) 2 else 1, 1.15f)
                s.add(Script.Write(db, MarkedText.runs(db, Marked.parse(detail)), textX, bottom + 4f, muted, accent, msPerChar = 26f))
                bottom += db.height + 4f
            }
            val adv = minOf(slot, maxOf(bottom - y + 56f, 130f))
            if (i < n - 1) s.add(Script.Stroke(sk.line(Safe.LEFT + 42f, cy + 48f, Safe.LEFT + 42f, y + adv - 6f, 3f), 4f, muted, 2f))
            y += if (i < n - 1) adv else bottom - y + 20f
        }
        return y
    }

    private fun stat(g: Gfx, sc: Scene, s: Script, sk: Sketch, y0: Float): Float {
        var y = kicker(g, sc, s, sk, y0)
        val (num, unit) = statParts(sc.statValue ?: "")
        val cy = y + 330f
        val f = fitSize(g, num, FontSpec(FontFamily.HAND, 700, 260f), 640f)
        val nb = TextBlock(listOf(num), f, f.size * 1.05f, false)
        s.add(Script.Write(nb, listOf(listOf(num to false)), Safe.CENTER_X, cy - nb.height / 2 - 20f, accent, accent, Align.CENTER, 120f))
        val rx = (g.measureText(num, f) / 2 + 90f).coerceIn(220f, 470f)
        s.add(Script.Stroke(sk.ellipse(Safe.CENTER_X, cy, rx, 210f), 8f, ink, 2.8f))
        if (unit.isNotEmpty()) write(g, s, "${sc.id}/u", unit, fBody.copy(weight = 700), Safe.WIDTH, 1, Safe.CENTER_X, cy + 230f, ink, Align.CENTER)
        s.add(Script.Stroke(sk.spark(Safe.CENTER_X - rx - 20f, cy - 150f, 70f), 6f, accent2, 1.2f))
        y = cy + 330f
        val t = write(g, s, "${sc.id}/t", sc.title, fTitle.withSize(68f), Safe.WIDTH, 3, Safe.CENTER_X, y, align = Align.CENTER, lh = 1.12f)
        y += t.height + 30f
        if (sc.body.isNotEmpty() && y < 1400f) y += write(g, s, "${sc.id}/b", sc.body, fSmall, Safe.WIDTH, 2, Safe.CENTER_X, y, muted, Align.CENTER).height
        return y
    }

    private fun compare(g: Gfx, sc: Scene, s: Script, sk: Sketch, y0: Float): Float {
        val cmp = sc.compare ?: return hero(g, sc, s, sk, y0)
        var y = kicker(g, sc, s, sk, y0)
        val t = write(g, s, "${sc.id}/t", sc.title, fTitle.withSize(78f), Safe.WIDTH, 2, Safe.LEFT, y)
        y += t.height + 70f
        // À éviter : croix, libellé, valeur… puis on barre.
        s.add(Script.Stroke(sk.cross(Safe.LEFT + 34f, y + 30f, 52f), 8f, danger, 1.6f))
        val bl = write(g, s, "${sc.id}/bl", cmp.badLabel, fKicker.withSize(50f), Safe.WIDTH - 100f, 1, Safe.LEFT + 90f, y, danger)
        y += bl.height + 18f
        val bvTop = y
        val bv = write(g, s, "${sc.id}/bv", cmp.badText(), fBody, Safe.WIDTH - 40f, 3, Safe.LEFT + 20f, y, ink, msPerChar = 32f)
        y += bv.height
        s.add(Script.Stroke(sk.scribble(Safe.LEFT + 10f, bvTop + 18f, Safe.LEFT + 30f + MarkedText.lineWidth(g, listOf((bv.lines.maxByOrNull { it.length } ?: "") to false), bv.font).coerceAtMost(Safe.WIDTH - 40f), y - 10f, bv.lines.size.coerceAtLeast(1)), 5f, danger.withAlpha(0.85f), 3f))
        y += 110f
        s.add(Script.Stroke(sk.check(Safe.LEFT + 34f, y + 28f, 60f), 8f, ok, 1.4f))
        val gl = write(g, s, "${sc.id}/gl", cmp.goodLabel, fKicker.withSize(50f), Safe.WIDTH - 100f, 1, Safe.LEFT + 90f, y, ok)
        y += gl.height + 18f
        val gv = write(g, s, "${sc.id}/gv", cmp.goodText(), fBody, Safe.WIDTH - 40f, 3, Safe.LEFT + 20f, y, ink, msPerChar = 32f)
        y += gv.height
        val lw = gv.lines.maxOfOrNull { g.measureText(it, gv.font) } ?: 200f
        s.add(Script.Stroke(sk.underline(Safe.LEFT + 20f, Safe.LEFT + 20f + lw, y + 4f), 7f, ok, 2.5f))
        y += 70f
        sc.subtitle?.let { if (y < 1380f) y += write(g, s, "${sc.id}/sub", it, fSmall, Safe.WIDTH, 3, Safe.LEFT, y, muted, msPerChar = 26f).height }
        return y
    }

    private fun checklist(g: Gfx, sc: Scene, s: Script, sk: Sketch, y0: Float): Float {
        var y = kicker(g, sc, s, sk, y0)
        val t = write(g, s, "${sc.id}/t", sc.title, fTitle.withSize(86f), Safe.WIDTH, 2, Safe.LEFT, y)
        y += t.height + 60f
        val n = sc.items.size.coerceAtLeast(1)
        val tail = (if (sc.body.isNotEmpty()) 170f else 0f) + (if (sc.subtitle != null) 140f else 0f)
        val slot = ((1520f - tail - y) / n).coerceIn(90f, 200f)
        for ((i, item) in sc.items.withIndex()) {
            val box = 56f
            val by = y + 8f
            s.add(Script.Stroke(sk.rect(Safe.LEFT, by, Safe.LEFT + box, by + box, 2f), 5f, ink, 2.5f))
            val ib = write(g, s, "${sc.id}/i$i", item, fBody, Safe.RIGHT - Safe.LEFT - 100f, if (slot >= 150f) 2 else 1, Safe.LEFT + 90f, y, ink, msPerChar = 30f)
            s.add(Script.Stroke(sk.check(Safe.LEFT + box / 2 + 4f, by + box / 2 - 4f, 62f), 8f, ok, 1.4f))
            y += minOf(slot, maxOf(ib.height + 46f, 100f))
        }
        sc.subtitle?.let { y += 10f; val b = write(g, s, "${sc.id}/sub", it, fSmall, Safe.WIDTH, 2, Safe.LEFT, y, muted, msPerChar = 26f); y += b.height + 40f }
        if (sc.body.isNotEmpty()) {
            val b = cache.fit(g, "${sc.id}/cta", Marked.strip(sc.body), fKicker.withSize(48f), Safe.WIDTH - 80f, 1)
            val w = g.measureText(b.lines.first(), b.font)
            s.add(Script.Stroke(sk.rect(Safe.LEFT, y, Safe.LEFT + w + 70f, y + b.height + 50f, 3f), 6f, accent, 3f))
            s.add(Script.Write(b, listOf(listOf(b.lines.first() to false)), Safe.LEFT + 35f, y + 25f, accent, accent, msPerChar = 30f))
            y += b.height + 50f
        }
        return y
    }

    // ───────────────────────────── La main ─────────────────────────────

    /**
     * Main qui tient le feutre ; la pointe du feutre est en (x, y). Illustration originale en formes simples.
     * Repère local : le feutre suit l'axe +x depuis la pointe, la main et le bras partent vers le bas à droite.
     */
    private fun drawHand(g: Gfx, x: Float, y: Float) {
        if (x > 1500f || y > 2400f) return
        val angle = deg(atan2(1.2, 1.0)).toFloat()
        val skinDark = mix(skin, 0xFF000000.toInt(), 0.30f)
        val skinShade = mix(skin, 0xFF000000.toInt(), 0.12f)
        val skinLight = mix(skin, 0xFFFFFFFF.toInt(), 0.16f)
        val bob = sin(x * 0.021f + y * 0.013f) * 2.5f
        fun layer(shadow: Boolean) {
            val sh = 0x24000000
            fun c(col: Int) = if (shadow) sh else col
            g.save()
            g.translate(x + (if (shadow) 22f else 0f), y + bob + (if (shadow) 30f else 0f))
            g.rotate(angle, 0f, 0f)
            g.scale(1.5f, 0f, 0f)
            // Bras et manche (sortent du cadre en bas à droite)
            g.fillRoundRect(262f, -86f, 1200f, 92f, 44f, c(sleeve))
            if (!shadow) {
                g.fillRoundRect(250f, -92f, 306f, 98f, 20f, mix(sleeve, 0xFFFFFFFF.toInt(), 0.14f))
                g.fillRect(320f, -86f, 1200f, -60f, 0x14FFFFFF)
            }
            // Dos de la main
            val palm = PathData().moveTo(112f, -58f).cubicTo(170f, -104f, 262f, -100f, 286f, -30f)
                .cubicTo(304f, 30f, 268f, 92f, 196f, 96f).cubicTo(138f, 100f, 100f, 62f, 96f, 18f).close()
            g.fillPath(palm, c(skin))
            // Feutre : pointe colorée, corps, bague de couleur, reflet
            if (!shadow) {
                g.fillPath(PathData().moveTo(-2f, 0f).lineTo(30f, -11f).lineTo(30f, 11f).close(), accent)
                g.fillRoundRect(26f, -19f, 236f, 19f, 8f, rgb(0x2B2F36))
                g.fillRect(30f, -19f, 58f, 19f, accent)
                g.fillRoundRect(64f, -15f, 232f, -9f, 3f, 0x30FFFFFF)
            }
            // Doigts repliés sous le feutre
            for (k in 0 until 3) {
                val cx = 214f + k * 22f; val cy = 70f + k * 8f
                g.fillCircle(cx, cy, 25f, c(skinShade))
                if (!shadow) g.strokeArc(cx, cy, 25f, 15f, 150f, 2.6f, skinDark)
            }
            // Pouce, le long du feutre côté intérieur
            g.fillPath(PathData().moveTo(196f, 30f).cubicTo(160f, 30f, 112f, 22f, 86f, 22f)
                .cubicTo(64f, 22f, 62f, 50f, 86f, 52f).cubicTo(130f, 56f, 172f, 62f, 206f, 64f).close(), c(skinLight))
            // Index posé sur le feutre, avec l'ongle
            g.fillPath(PathData().moveTo(206f, -52f).cubicTo(160f, -50f, 100f, -44f, 66f, -36f)
                .cubicTo(44f, -30f, 46f, -8f, 68f, -10f).cubicTo(112f, -12f, 170f, -16f, 214f, -16f).close(), c(skin))
            if (!shadow) {
                g.fillRoundRect(60f, -34f, 92f, -16f, 9f, mix(skin, 0xFFFFFFFF.toInt(), 0.38f))
                g.strokePath(PathData().moveTo(206f, -52f).cubicTo(160f, -50f, 100f, -44f, 66f, -36f).cubicTo(44f, -30f, 46f, -8f, 68f, -10f), 2.6f, skinDark)
                g.strokePath(PathData().moveTo(86f, 22f).cubicTo(64f, 22f, 62f, 50f, 86f, 52f).cubicTo(130f, 56f, 172f, 62f, 206f, 64f), 2.6f, skinDark)
                // Articulations
                g.strokeArc(232f, -40f, 18f, 200f, 80f, 2.2f, skinDark.withAlpha(0.6f))
            }
            g.restore()
        }
        layer(true)
        layer(false)
    }
}
