// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fonds animés des templates. Ils dépendent du temps GLOBAL : pendant une transition, les deux scènes
 * montrent le même décor au même endroit, ce qui rend le raccord invisible.
 * Chaque scène reçoit une légère variation de teinte (index de scène) : le fond évolue au fil de l'histoire.
 */
internal object Backgrounds {
    private const val W = VideoFormat.DESIGN_WIDTH
    private const val H = VideoFormat.DESIGN_HEIGHT

    private val traces: List<PathData> = run {
        val r = Rand(2024)
        val cell = 72f
        List(7) {
            var x = (r.range(1f, 14f)).toInt() * cell
            var y = (r.range(2f, 25f)).toInt() * cell
            val p = PathData().moveTo(x, y)
            repeat(5) { k ->
                if (k % 2 == 0) x += (if (r.next() > 0.5f) 1 else -1) * (r.range(2f, 5f)).toInt() * cell
                else y += (if (r.next() > 0.5f) 1 else -1) * (r.range(2f, 5f)).toInt() * cell
                p.lineTo(x.coerceIn(cell, W - cell), y.coerceIn(cell, H - cell))
            }
            p
        }
    }

    fun draw(g: Gfx, tp: Template, sceneIndex: Int, sceneBg: Int?, tGlobalMs: Float) {
        val s = tp.style
        val tint = if (sceneIndex % 2 == 1) s.accent2 else s.accent
        val top = sceneBg ?: mix(s.bgTop, tint, if (s.dark) 0.06f else 0.035f)
        g.fillVerticalGradient(0f, 0f, W, H, top, s.bgBottom)
        val t = tGlobalMs
        when (tp.decor) {
            Decor.CIRCUIT_GRID -> circuit(g, s, t)
            Decor.CORPORATE_LINES -> corporate(g, s, t)
            Decor.EDU_SHAPES -> education(g, s, t)
            Decor.ORGANIC_LEAVES -> nature(g, s, t)
            Decor.MEDICAL_PULSE -> medical(g, s, t)
            Decor.MARKETING_BLOCKS -> marketing(g, s, t)
            Decor.GIANT_TYPE -> giantType(g, s, t)
            Decor.CINEMA_LIGHT -> cinema(g, s, t)
            Decor.CODE_EDITOR -> codeEditor(g, s, t)
            Decor.BLUEPRINT -> blueprint(g, s, t)
        }
    }

    private fun aurora(g: Gfx, s: Style, t: Float, a1: Float, a2: Float) {
        g.fillRadialGlow(540f + 260f * sin(t * 0.00021f), 560f + 160f * cos(t * 0.00017f), 680f, s.accent.withAlpha(a1))
        g.fillRadialGlow(260f + 200f * cos(t * 0.00013f), 1480f + 140f * sin(t * 0.00019f), 620f, s.accent2.withAlpha(a2))
    }

    // Cyber Neon : halos néon, grille de points, pistes de circuit parcourues par des impulsions.
    private fun circuit(g: Gfx, s: Style, t: Float) {
        aurora(g, s, t, 0.17f, 0.15f)
        val cell = 72f
        val drift = (t * 0.010f) % cell
        val dot = s.text.withAlpha(0.06f)
        var y = -drift
        while (y < H + cell) {
            var x = cell / 2
            while (x < W) { g.fillCircle(x, y, 2.4f, dot); x += cell }
            y += cell
        }
        for ((i, p) in traces.withIndex()) {
            g.strokePath(p, 2.5f, s.accent.withAlpha(0.06f))
            val period = 5200f + i * 700f
            val phase = ((t + i * 900f) % period) / period
            g.strokePath(p.segment(phase - 0.10f, phase), 3.5f, (if (i % 2 == 0) s.accent else s.accent2).withAlpha(0.4f))
        }
        // Balayage lumineux horizontal (scanner) toutes les 6 s.
        val sy = ((t % 6000f) / 6000f) * (H + 400f) - 200f
        g.fillVerticalGradient(0f, sy - 120f, W, sy, s.accent.withAlpha(0f), s.accent.withAlpha(0.05f))
    }

    // Corporate Premium : fines lignes dorées qui se tracent, grand cercle, beaucoup d'air.
    private fun corporate(g: Gfx, s: Style, t: Float) {
        g.fillRadialGlow(880f, 260f, 620f, s.accent.withAlpha(0.10f))
        val k = Easing.EXPO_OUT.ease(window(t % 12000f, 0f, 2400f))
        g.line(Safe.LEFT - 30f, 220f, Safe.LEFT - 30f, 220f + 1300f * k, 1.5f, s.accent.withAlpha(0.55f))
        g.line(Safe.LEFT - 30f, 1700f, Safe.LEFT - 30f + 400f * k, 1700f, 1.5f, s.accent.withAlpha(0.55f))
        val rot = t * 0.004f
        g.strokeArc(900f, 1500f, 330f, rot, 250f, 1.5f, s.accent.withAlpha(0.35f))
        g.strokeArc(900f, 1500f, 280f, -rot * 1.3f, 120f, 1.2f, s.accent2.withAlpha(0.18f))
        for (i in 0 until 5) g.line(860f + i * 18f, 150f, 860f + i * 18f, 190f, 1.2f, s.accent2.withAlpha(0.25f))
    }

    // Modern Education : formes géométriques colorées qui flottent doucement, grille pointillée.
    private fun education(g: Gfx, s: Style, t: Float) {
        val dot = s.text.withAlpha(0.07f)
        var y = 60f
        while (y < H) { var x = 60f; while (x < W) { g.fillCircle(x, y, 2.2f, dot); x += 60f }; y += 60f }
        fun bob(seed: Int, amp: Float) = amp * sin(t * 0.0012f + seed)
        // Formes en bordure de cadre, hors des zones de texte (haut ≤ 200 px, bas ≥ 1680 px).
        g.fillCircle(1035f, 175f + bob(1, 6f), 44f, s.accent2.withAlpha(0.85f))
        g.strokeCircle(70f, 1790f + bob(2, 10f), 58f, 10f, s.accent.withAlpha(0.8f))
        val tri = PathData().polygon(990f, 1790f + bob(3, 10f), 54f, 3, -90f + t * 0.01f)
        g.fillPath(tri, s.highlight)
        g.fillRoundRect(40f, 160f + bob(4, 8f), 140f, 196f + bob(4, 8f), 18f, s.accent.withAlpha(0.85f))
        // Gribouillis ondulé.
        val w = PathData().moveTo(420f, 1790f)
        for (i in 1..8) w.lineTo(420f + i * 34f, 1790f + (if (i % 2 == 0) -18f else 18f) + bob(i, 4f))
        g.strokePath(w, 8f, s.accent2.withAlpha(0.7f))
    }

    // Green Nature : rayons de lumière, feuilles qui se balancent, particules de pollen.
    private fun nature(g: Gfx, s: Style, t: Float) {
        g.fillRadialGlow(860f, 220f, 720f, s.accent2.withAlpha(0.20f))
        g.fillRadialGlow(200f, 1600f, 620f, s.accent.withAlpha(0.12f))
        for (i in 0 until 4) {
            val a = 0.035f + 0.02f * sin(t * 0.0008f + i)
            val p = PathData().moveTo(760f + i * 60f, 0f).lineTo(860f + i * 70f, 0f).lineTo(260f + i * 160f, H).lineTo(120f + i * 150f, H).close()
            g.fillPath(p, s.accent2.withAlpha(a))
        }
        fun leaf(cx: Float, cy: Float, size: Float, rot: Float, color: Int) {
            g.save(); g.rotate(rot, cx, cy)
            val p = PathData().moveTo(cx, cy - size).cubicTo(cx + size * 0.7f, cy - size * 0.5f, cx + size * 0.6f, cy + size * 0.5f, cx, cy + size)
                .cubicTo(cx - size * 0.6f, cy + size * 0.5f, cx - size * 0.7f, cy - size * 0.5f, cx, cy - size).close()
            g.fillPath(p, color)
            g.line(cx, cy - size * 0.9f, cx, cy + size * 0.95f, 3f, s.bgBottom.withAlpha(0.5f))
            g.restore()
        }
        leaf(980f, 1500f, 150f, -35f + 6f * sin(t * 0.0011f), s.accent.withAlpha(0.22f))
        leaf(1040f, 1690f, 110f, 20f + 7f * sin(t * 0.0013f + 1f), s.accent.withAlpha(0.16f))
        leaf(60f, 380f, 120f, 40f + 6f * sin(t * 0.0012f + 2f), s.accent.withAlpha(0.14f))
        val r = Rand(7)
        for (i in 0 until 26) {
            val x0 = r.range(0f, W); val y0 = r.range(0f, H); val sp = r.range(0.006f, 0.02f)
            val y = (y0 - t * sp) % H
            g.fillCircle(x0 + 20f * sin(t * 0.001f + i), if (y < 0) y + H else y, r.range(2f, 4.5f), s.accent2.withAlpha(0.35f))
        }
    }

    // Medical Clean : ondes douces, croix discrètes, tracé cardiaque.
    private fun medical(g: Gfx, s: Style, t: Float) {
        g.fillRadialGlow(540f, 520f, 760f, s.accent2.withAlpha(0.12f))
        for (k in 0 until 3) {
            val ph = ((t / 4800f) + k / 3f) % 1f
            g.strokeCircle(540f, 600f, 160f + ph * 760f, 3f, s.accent.withAlpha(0.12f * (1f - ph)))
        }
        fun cross(cx: Float, cy: Float, a: Float, c: Int) {
            g.fillRoundRect(cx - a, cy - a / 3, cx + a, cy + a / 3, a / 4, c)
            g.fillRoundRect(cx - a / 3, cy - a, cx + a / 3, cy + a, a / 4, c)
        }
        cross(930f, 230f + 10f * sin(t * 0.001f), 34f, s.accent.withAlpha(0.18f))
        cross(140f, 1800f + 10f * sin(t * 0.0012f), 26f, s.accent2.withAlpha(0.3f))
        val p = PathData()
        val baseY = 1760f
        val period = 360f
        val off = (t * 0.10f) % period
        var x = -period - off
        p.moveTo(x, baseY)
        while (x < W + period) {
            p.lineTo(x + 120f, baseY); p.lineTo(x + 150f, baseY - 24f); p.lineTo(x + 175f, baseY + 30f)
            p.lineTo(x + 200f, baseY - 100f); p.lineTo(x + 228f, baseY + 50f); p.lineTo(x + 252f, baseY); p.lineTo(x + period, baseY)
            x += period
        }
        g.strokePath(p, 3f, s.accent.withAlpha(0.22f))
    }

    // Marketing Impact : grands blocs diagonaux colorés qui glissent, étoiles qui tournent.
    private fun marketing(g: Gfx, s: Style, t: Float) {
        aurora(g, s, t, 0.25f, 0.18f)
        val shift = 80f * sin(t * 0.0006f)
        g.fillPath(PathData().moveTo(W * 0.62f + shift, 0f).lineTo(W + 40f, 0f).lineTo(W + 40f, H * 0.42f).lineTo(W * 0.2f + shift, H * 0.18f).close(), s.accent.withAlpha(0.22f))
        g.fillPath(PathData().moveTo(-40f, H * 0.78f - shift).lineTo(W * 0.7f, H * 0.95f - shift).lineTo(W * 0.5f, H + 40f).lineTo(-40f, H + 40f).close(), s.accent2.withAlpha(0.20f))
        fun star(cx: Float, cy: Float, r: Float, rot: Float, c: Int) {
            val p = PathData()
            for (i in 0 until 10) {
                val a = Math.toRadians((rot + i * 36f).toDouble())
                val rr = if (i % 2 == 0) r else r * 0.42f
                val x = cx + (rr * cos(a)).toFloat(); val y = cy + (rr * sin(a)).toFloat()
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            g.fillPath(p.close(), c)
        }
        star(980f, 1800f, 58f, t * 0.03f, s.accent2.withAlpha(0.9f))
        star(1010f, 175f, 30f, -t * 0.04f, s.accent.withAlpha(0.9f))
    }

    // Bold Typography : mot géant qui défile en fond, bande jaune.
    private fun giantType(g: Gfx, s: Style, t: Float) {
        val f = FontSpec(FontFamily.HEAVY, 400, 360f)
        val word = "SAVOIR  APPRENDRE  PARTAGER  "
        val w = g.measureText(word, f)
        val x = -((t * 0.08f) % w)
        g.drawText(word + word, x, 1180f, f, s.text.withAlpha(0.03f))
        g.drawText(word + word, x * 1.4f - 300f, 1640f, f, s.text.withAlpha(0.02f))
        g.fillRect(0f, 1720f, W, 1740f, s.accent.withAlpha(0.9f))
        g.fillRect(0f, 1760f, W * (0.5f + 0.5f * sin(t * 0.0008f)), 1766f, s.accent2.withAlpha(0.8f))
    }

    // Cinematic Story : lumière chaude qui traverse le cadre (fuite de lumière), vignette.
    private fun cinema(g: Gfx, s: Style, t: Float) {
        val lx = -200f + ((t * 0.035f) % (W + 600f))
        g.fillRadialGlow(lx, 520f, 760f, s.accent.withAlpha(0.20f))
        g.fillRadialGlow(W - lx * 0.6f, 1480f, 680f, s.accent2.withAlpha(0.14f))
        g.fillVerticalGradient(0f, 0f, W, 520f, 0x8C000000.toInt(), 0x00000000)
        g.fillVerticalGradient(0f, H - 620f, W, H, 0x00000000, 0xA6000000.toInt())
    }

    // Code Studio : gouttière de numéros de ligne, lignes de code stylisées qui défilent, curseur.
    private fun codeEditor(g: Gfx, s: Style, t: Float) {
        g.fillRadialGlow(900f, 300f, 600f, s.accent2.withAlpha(0.10f))
        g.fillRadialGlow(150f, 1600f, 560f, s.accent.withAlpha(0.08f))
        g.fillRect(0f, 0f, 58f, H, s.surface.withAlpha(0.55f))
        val lineH = 46f
        val scroll = (t * 0.012f) % lineH
        val mono = FontSpec(FontFamily.MONO, 400, 20f)
        val r = Rand(31)
        val widths = FloatArray(48) { r.range(120f, 560f) }
        val indents = IntArray(48) { (r.nextInt() ushr 1) % 4 }
        val colors = intArrayOf(s.accent, s.accent2, s.emphasis, s.highlight, s.textMuted)
        var i = 0
        var y = 40f - scroll
        while (y < H) {
            val n = ((t * 0.012f) / lineH).toInt() + i
            g.drawText((n % 99 + 1).toString().padStart(2, ' '), 14f, y + 7f, mono, s.textMuted.withAlpha(0.35f))
            val k = n % widths.size
            var x = 84f + indents[k] * 32f
            val w = widths[k]
            // Chaque ligne = 2 à 3 « jetons » colorés, très discrets.
            val c1 = colors[k % colors.size].withAlpha(0.10f)
            val c2 = colors[(k + 2) % colors.size].withAlpha(0.08f)
            g.fillRoundRect(x, y - 8f, x + w * 0.35f, y + 6f, 7f, c1); x += w * 0.35f + 14f
            g.fillRoundRect(x, y - 8f, x + w * 0.45f, y + 6f, 7f, c2); x += w * 0.45f + 14f
            if (k % 3 == 0) g.fillRoundRect(x, y - 8f, x + w * 0.2f, y + 6f, 7f, c1)
            y += lineH; i++
        }
        if (((t / 530f).toInt() % 2) == 0) g.fillRect(84f, 1772f, 98f, 1802f, s.accent.withAlpha(0.6f))
    }

    // Network Blueprint : grille de plan, topologie de nœuds reliés, paquets qui circulent.
    // Nœuds placés hors des zones de texte : bande du bas (sous le pied de page) et coins du haut.
    private val nodes = listOf(110f to 1700f, 330f to 1810f, 560f to 1700f, 800f to 1815f, 990f to 1700f, 1030f to 175f, 50f to 175f)
    private val links = listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4, 2 to 4, 0 to 2, 4 to 5, 6 to 0)

    private fun blueprint(g: Gfx, s: Style, t: Float) {
        val minor = 40f
        var x = 0f
        while (x <= W) { g.line(x, 0f, x, H, if ((x / minor).toInt() % 5 == 0) 1.4f else 0.8f, s.text.withAlpha(if ((x / minor).toInt() % 5 == 0) 0.07f else 0.035f)); x += minor }
        var y = 0f
        while (y <= H) { g.line(0f, y, W, y, if ((y / minor).toInt() % 5 == 0) 1.4f else 0.8f, s.text.withAlpha(if ((y / minor).toInt() % 5 == 0) 0.07f else 0.035f)); y += minor }
        g.fillRadialGlow(540f, 700f, 760f, s.accent.withAlpha(0.10f))
        for ((k, l) in links.withIndex()) {
            val (a, b) = l
            val (x0, y0) = nodes[a]; val (x1, y1) = nodes[b]
            g.line(x0, y0, x1, y1, 2f, s.accent.withAlpha(0.22f))
            val ph = ((t + k * 700f) % 2600f) / 2600f
            g.fillCircle(lerp(x0, x1, ph), lerp(y0, y1, ph), 5f, s.accent2.withAlpha(0.8f))
        }
        for ((i, n) in nodes.withIndex()) {
            val (nx, ny) = n
            val pulse = 0.5f + 0.5f * sin(t * 0.003f + i)
            g.fillRoundRect(nx - 18f, ny - 18f, nx + 18f, ny + 18f, 6f, s.bgTop)
            g.strokeRoundRect(nx - 18f, ny - 18f, nx + 18f, ny + 18f, 6f, 2f, s.accent.withAlpha(0.5f + 0.3f * pulse))
            g.fillCircle(nx, ny, 4f, s.accent2.withAlpha(0.7f))
        }
    }

    /** Effets posés au-dessus de tout (grain de pellicule, bandes cinéma). */
    fun overlayFx(g: Gfx, tp: Template, frameIndexSeed: Int) {
        if (tp.grain) {
            val r = Rand(frameIndexSeed * 7919 + 13)
            for (i in 0 until 260) g.fillCircle(r.range(0f, W), r.range(0f, H), r.range(0.8f, 1.8f), (if (i % 2 == 0) 0x14FFFFFF else 0x18000000))
        }
        if (tp.letterbox) {
            g.fillRect(0f, 0f, W, 72f, 0xFF000000.toInt())
            g.fillRect(0f, H - 140f, W, H, 0xFF000000.toInt())
        }
    }

    @Suppress("unused") private val TAU = (2 * PI).toFloat()
}
