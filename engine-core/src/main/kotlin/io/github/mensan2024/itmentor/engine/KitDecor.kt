// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.sin

/** Décors de fond des styles modulables, partagés par le « Montage dynamique » et l'« Infographie ». */
internal object KitDecor {
    fun background(g: Gfx, kit: StyleKit, seed: Long, tg: Float, floor: Float = 1380f, corners: Boolean = true) {
        val pal = kit.palette; val light = pal.light; val accent = pal.accent; val accent2 = pal.accent2
        fun line(alpha: Float) = (if (light) pal.ink else rgb(0xFFFFFF)).withAlpha(alpha)
        g.fillVerticalGradient(0f, 0f, 1080f, 1920f, pal.bgTop, pal.bgBottom)
        val r = Rand.of(seed, 0x77D1)
        when (kit.background) {
            KitBackground.GRID_FLOOR -> {
                for (k in 0 until 26) {
                    val x = r.next() * 1080f; val y = 200f + r.next() * 1600f; val ph = r.next() * 6.28f
                    g.fillCircle(x, y + 10f * sin(tg * 0.0007f + ph), 2.6f, line(0.12f + 0.12f * sin(tg * 0.002f + ph)))
                }
                g.save(); g.clipRect(0f, floor, 1080f, 1920f)
                g.fillRect(0f, floor, 1080f, 1920f, pal.bgBottom)
                g.fillRect(0f, floor, 1080f, floor + 2f, line(0.19f))
                for (k in -9..9) g.line(540f + k * 70f, floor, 540f + k * 330f, 1920f, 1.6f, line(0.13f))
                var d = 0f
                while (true) {
                    val y = floor + 16f * (1 + d) * (1 + d)
                    if (y > 1920f) break
                    g.fillRect(0f, y, 1080f, y + 1.6f, line(0.12f)); d += 1f
                }
                g.restore()
                g.fillRadialGlow(540f, floor + 40f, 520f, line(0.13f))
            }
            KitBackground.STARS -> {
                for (k in 0 until 120) {
                    val x = r.next() * 1080f; val y0 = r.next() * 1920f; val sz = 1f + r.next() * 2.4f; val ph = r.next() * 6.28f
                    val y = (y0 - tg * 0.01f * sz).mod(1920f)
                    g.fillCircle(x, y, sz, line(0.2f + 0.5f * (0.5f + 0.5f * sin(tg * 0.002f + ph))))
                }
                g.fillRadialGlow(540f, 1700f, 900f, accent.withAlpha(0.12f))
            }
            KitBackground.AURORA -> {
                val w = sin(tg * 0.0006f)
                g.fillRadialGlow(540f + 220f * w, 1550f, 950f, accent.withAlpha(0.32f))
                g.fillRadialGlow(220f - 140f * w, 1150f, 700f, accent2.withAlpha(0.26f))
                g.fillRadialGlow(880f, 260f, 620f, accent.withAlpha(0.12f))
            }
            KitBackground.MESH -> {
                for (k in 0 until 4) {
                    val ph = k * 1.7f
                    val x = 540f + 380f * sin(tg * 0.00035f + ph); val y = 960f + 700f * sin(tg * 0.00027f + ph * 1.3f)
                    g.fillRadialGlow(x, y, 700f, (if (k % 2 == 0) accent else accent2).withAlpha(if (light) 0.16f else 0.22f))
                }
            }
            KitBackground.NEON_LINES -> {
                val off = (tg * 0.05f) % 160f
                g.save(); g.rotate(-24f, 540f, 960f)
                var x = -900f + off
                while (x < 2000f) { g.fillRect(x, -400f, x + 3f, 2400f, accent.withAlpha(0.14f)); x += 160f }
                g.restore()
                g.fillRadialGlow(540f, 300f, 700f, accent2.withAlpha(0.16f))
                g.fillRadialGlow(540f, 1700f, 800f, accent.withAlpha(0.18f))
            }
            KitBackground.DOTS -> {
                val off = (tg * 0.012f) % 48f
                var y = 24f - off
                while (y < 1920f) {
                    var x = 24f
                    while (x < 1080f) { g.fillCircle(x, y, 2.2f, line(0.14f)); x += 48f }
                    y += 48f
                }
                g.fillRadialGlow(540f, 900f, 800f, accent.withAlpha(if (light) 0.08f else 0.12f))
            }
            KitBackground.PAPER -> {
                var y = 120f
                while (y < 1920f) { g.fillRect(0f, y, 1080f, y + 1.5f, line(0.06f)); y += 64f }
                g.fillRect(150f, 0f, 152f, 1920f, accent.withAlpha(0.18f))
            }
            KitBackground.GLOW -> {
                g.fillRadialGlow(540f, 1650f, 900f, accent2.withAlpha(0.16f))
                g.fillRadialGlow(540f + 60f * sin(tg * 0.0004f), 300f, 760f, accent.withAlpha(0.14f))
            }
            KitBackground.SUNSET -> {
                // Lueur d'aube : uniquement des dégradés radiaux, sans forme à contour net. L'ancien disque plein de 620 px
                // faisait une demi-sphère qui mangeait le haut de la vidéo et le texte (retour utilisateur du 27/11/2026).
                val w = sin(tg * 0.0005f)
                g.fillRadialGlow(540f + 60f * w, -40f, 900f, accent.withAlpha(if (light) 0.14f else 0.20f))
                g.fillRadialGlow(180f, 1880f, 800f, accent2.withAlpha(0.14f))
            }
        }
        // repères « + » dans les coins
        if (corners) for (x in listOf(90f, 990f)) { val y = 168f; g.fillRect(x - 14f, y - 1.5f, x + 14f, y + 1.5f, line(0.25f)); g.fillRect(x - 1.5f, y - 14f, x + 1.5f, y + 14f, line(0.25f)) }
    }

    fun darken(c: Int, f: Float): Int {
        val r = ((c shr 16) and 0xFF) * (1 - f); val gg = ((c shr 8) and 0xFF) * (1 - f); val b = (c and 0xFF) * (1 - f)
        return (0xFF shl 24) or (r.toInt() shl 16) or (gg.toInt() shl 8) or b.toInt()
    }
}
