// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Icônes vectorielles originales, dessinées sur une grille 24 × 24, style « trait » 2 px.
 * Aucune ressource tierce : elles sont sous la licence du projet.
 */
object Icons {
    data class Part(val path: PathData, val filled: Boolean = false)

    fun parts(id: IconId): List<Part> = when (id) {
        IconId.SHIELD -> listOf(Part(PathData().moveTo(12f, 2.5f).lineTo(20f, 5.5f).lineTo(20f, 11f)
            .cubicTo(20f, 16.2f, 16.6f, 20f, 12f, 21.5f).cubicTo(7.4f, 20f, 4f, 16.2f, 4f, 11f).lineTo(4f, 5.5f).close()))
        IconId.LOCK -> listOf(
            Part(roundRect(5f, 11f, 19f, 21f, 2.2f)),
            Part(PathData().moveTo(8f, 11f).lineTo(8f, 7.5f).cubicTo(8f, 5.1f, 9.8f, 3f, 12f, 3f).cubicTo(14.2f, 3f, 16f, 5.1f, 16f, 7.5f).lineTo(16f, 11f)),
            Part(PathData().circle(12f, 16f, 1.3f), filled = true),
        )
        IconId.MAIL -> listOf(
            Part(roundRect(3f, 5f, 21f, 19f, 2.2f)),
            Part(PathData().moveTo(3.8f, 6.5f).lineTo(12f, 12.8f).lineTo(20.2f, 6.5f)),
        )
        IconId.WARNING -> listOf(
            Part(PathData().moveTo(12f, 3.2f).lineTo(21.5f, 20f).lineTo(2.5f, 20f).close()),
            Part(PathData().moveTo(12f, 9.5f).lineTo(12f, 14f)),
            Part(PathData().circle(12f, 17f, 1.1f), filled = true),
        )
        IconId.CHECK -> listOf(Part(PathData().moveTo(5f, 12.5f).lineTo(10f, 17.5f).lineTo(19.5f, 7f)))
        IconId.CROSS -> listOf(Part(PathData().moveTo(6.5f, 6.5f).lineTo(17.5f, 17.5f).moveTo(17.5f, 6.5f).lineTo(6.5f, 17.5f)))
        IconId.KEY -> listOf(
            Part(PathData().circle(8f, 15.5f, 4f)),
            Part(PathData().moveTo(10.9f, 12.6f).lineTo(20f, 3.5f).moveTo(16.8f, 6.7f).lineTo(19.3f, 9.2f).moveTo(14.6f, 8.9f).lineTo(16.6f, 10.9f)),
        )
        IconId.EYE -> listOf(
            Part(PathData().moveTo(2.5f, 12f).cubicTo(5f, 7f, 8.3f, 5f, 12f, 5f).cubicTo(15.7f, 5f, 19f, 7f, 21.5f, 12f)
                .cubicTo(19f, 17f, 15.7f, 19f, 12f, 19f).cubicTo(8.3f, 19f, 5f, 17f, 2.5f, 12f).close()),
            Part(PathData().circle(12f, 12f, 3f)),
        )
        IconId.USER -> listOf(
            Part(PathData().circle(12f, 8f, 4f)),
            Part(PathData().moveTo(4.5f, 20.5f).cubicTo(4.5f, 16.4f, 7.9f, 14f, 12f, 14f).cubicTo(16.1f, 14f, 19.5f, 16.4f, 19.5f, 20.5f)),
        )
        IconId.HOOK -> listOf(
            Part(PathData().circle(15f, 3.6f, 1.6f)),
            Part(PathData().moveTo(15f, 5.2f).lineTo(15f, 14.5f).cubicTo(15f, 18f, 12.6f, 20.5f, 9.3f, 20.5f)
                .cubicTo(6.3f, 20.5f, 4.3f, 18.3f, 4.3f, 15.6f).lineTo(7.4f, 17.6f)),
        )
        IconId.SEARCH -> listOf(Part(PathData().circle(10.5f, 10.5f, 6.5f)), Part(PathData().moveTo(15.3f, 15.3f).lineTo(20.5f, 20.5f)))
        IconId.CLOCK -> listOf(Part(PathData().circle(12f, 12f, 9f)), Part(PathData().moveTo(12f, 7f).lineTo(12f, 12f).lineTo(15.5f, 14f)))
        IconId.DROP -> listOf(Part(PathData().moveTo(12f, 2.8f).cubicTo(15.5f, 7.5f, 18.5f, 10.8f, 18.5f, 14.5f)
            .cubicTo(18.5f, 18.2f, 15.6f, 21f, 12f, 21f).cubicTo(8.4f, 21f, 5.5f, 18.2f, 5.5f, 14.5f).cubicTo(5.5f, 10.8f, 8.5f, 7.5f, 12f, 2.8f).close()),
            Part(PathData().moveTo(9f, 14.5f).cubicTo(9f, 16.3f, 10.2f, 17.6f, 12f, 17.8f)))
        IconId.HEART -> listOf(Part(PathData().moveTo(12f, 20.5f).cubicTo(5f, 15.8f, 3f, 12.2f, 3f, 9f).cubicTo(3f, 6.2f, 5.1f, 4f, 7.7f, 4f)
            .cubicTo(9.6f, 4f, 11.1f, 5.1f, 12f, 6.6f).cubicTo(12.9f, 5.1f, 14.4f, 4f, 16.3f, 4f).cubicTo(18.9f, 4f, 21f, 6.2f, 21f, 9f)
            .cubicTo(21f, 12.2f, 19f, 15.8f, 12f, 20.5f).close()))
        IconId.LEAF -> listOf(Part(PathData().moveTo(5f, 19f).cubicTo(5f, 10f, 10f, 4.5f, 20f, 4f).cubicTo(19.5f, 14f, 14f, 19f, 5f, 19f).close()),
            Part(PathData().moveTo(4f, 20f).lineTo(13f, 11f)))
        IconId.SUN -> listOf(Part(PathData().circle(12f, 12f, 4.2f)),
            Part(PathData().moveTo(12f, 2.5f).lineTo(12f, 4.5f).moveTo(12f, 19.5f).lineTo(12f, 21.5f).moveTo(2.5f, 12f).lineTo(4.5f, 12f)
                .moveTo(19.5f, 12f).lineTo(21.5f, 12f).moveTo(5.3f, 5.3f).lineTo(6.7f, 6.7f).moveTo(17.3f, 17.3f).lineTo(18.7f, 18.7f)
                .moveTo(5.3f, 18.7f).lineTo(6.7f, 17.3f).moveTo(17.3f, 6.7f).lineTo(18.7f, 5.3f)))
        IconId.MEGAPHONE -> listOf(Part(PathData().moveTo(3.5f, 10f).lineTo(3.5f, 14f).lineTo(7f, 14f).lineTo(17f, 19f).lineTo(17f, 5f).lineTo(7f, 10f).close()),
            Part(PathData().moveTo(7f, 14f).lineTo(8.5f, 19.5f).lineTo(11f, 19.5f).lineTo(10.2f, 15.6f)),
            Part(PathData().moveTo(20f, 9.5f).cubicTo(21f, 10.5f, 21f, 13.5f, 20f, 14.5f)))
        IconId.TARGET -> listOf(Part(PathData().circle(12f, 12f, 9f)), Part(PathData().circle(12f, 12f, 5.2f)), Part(PathData().circle(12f, 12f, 1.6f), filled = true))
        IconId.BULB -> listOf(Part(PathData().moveTo(9f, 17f).cubicTo(9f, 14.5f, 6f, 13f, 6f, 9.5f).cubicTo(6f, 6.2f, 8.7f, 3.5f, 12f, 3.5f)
            .cubicTo(15.3f, 3.5f, 18f, 6.2f, 18f, 9.5f).cubicTo(18f, 13f, 15f, 14.5f, 15f, 17f).close()),
            Part(PathData().moveTo(9.5f, 20.5f).lineTo(14.5f, 20.5f)))
        IconId.CHART -> listOf(Part(PathData().moveTo(4f, 20f).lineTo(20f, 20f)),
            Part(PathData().moveTo(7f, 17f).lineTo(7f, 13f).moveTo(12f, 17f).lineTo(12f, 9f).moveTo(17f, 17f).lineTo(17f, 5f)))
    }

    fun roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float): PathData {
        val k = 0.5522848f * rad
        return PathData().moveTo(l + rad, t).lineTo(r - rad, t).cubicTo(r - rad + k, t, r, t + rad - k, r, t + rad)
            .lineTo(r, b - rad).cubicTo(r, b - rad + k, r - rad + k, b, r - rad, b)
            .lineTo(l + rad, b).cubicTo(l + rad - k, b, l, b - rad + k, l, b - rad)
            .lineTo(l, t + rad).cubicTo(l, t + rad - k, l + rad - k, t, l + rad, t).close()
    }

    /** Dessine l'icône centrée en (cx, cy) avec une taille de `size` px. `reveal` trace le trait progressivement. */
    fun draw(g: Gfx, id: IconId, cx: Float, cy: Float, size: Float, color: Int, strokeWidth: Float = 2f, reveal: Float = 1f) {
        val s = size / 24f
        val tx = cx - size / 2
        val ty = cy - size / 2
        for (p in parts(id)) {
            val path = p.path.transformed(s, tx, ty)
            if (p.filled) { if (reveal >= 0.999f) g.fillPath(path, color) }
            else g.strokePath(if (reveal >= 0.999f) path else path.trimmed(reveal), strokeWidth * s, color)
        }
    }
}
