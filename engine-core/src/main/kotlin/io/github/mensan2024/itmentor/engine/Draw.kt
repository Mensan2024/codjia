// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Briques de dessin partagées par toutes les compositions. */

/** Zone sûre : rien d'important hors de ce cadre (barres d'interface de LinkedIn, TikTok, Reels…). */
object Safe {
    const val LEFT = 84f
    const val RIGHT = 996f
    const val WIDTH = RIGHT - LEFT
    const val TOP = 150f
    const val BOTTOM = 1650f
    const val CENTER_X = VideoFormat.DESIGN_WIDTH / 2
}

enum class Align { LEFT, CENTER }

/** Applique une pose (opacité, déplacement, échelle autour d'un pivot) le temps d'un bloc. */
inline fun Gfx.withPose(pose: Pose, pivotX: Float, pivotY: Float, block: () -> Unit) {
    if (pose.alpha <= 0.002f) return
    save()
    if (pose.alpha < 0.999f) multiplyAlpha(pose.alpha)
    if (pose.dx != 0f || pose.dy != 0f) translate(pose.dx, pose.dy)
    if (pose.scale != 1f) scale(pose.scale, pivotX, pivotY)
    block()
    restore()
}

/** Ligne de base de la ligne i d'un bloc dont le haut est `top`. */
fun Gfx.baselineOf(block: TextBlock, top: Float, i: Int): Float {
    val m = metrics(block.font)
    val glyphH = m.ascent
    return top + (block.lineHeight - glyphH) / 2f + m.ascent + i * block.lineHeight
}

/** Dessine un bloc de texte. `reveal` < 1 = machine à écrire (le texte ne bouge jamais pendant la frappe). */
fun Gfx.drawBlock(
    block: TextBlock, x: Float, top: Float, color: Int, align: Align = Align.LEFT,
    reveal: Float = 1f, caretColor: Int? = null, caretVisible: Boolean = false,
) {
    val lines = if (reveal >= 1f) block.lines else TextLayout.revealed(block, reveal)
    var lastX = x
    var lastBaseline = baselineOf(block, top, 0)
    for ((i, full) in block.lines.withIndex()) {
        val shown = lines.getOrNull(i) ?: break
        val base = baselineOf(block, top, i)
        val lx = when (align) {
            Align.LEFT -> x
            Align.CENTER -> x - measureText(full, block.font) / 2f
        }
        if (shown.isNotEmpty()) drawText(shown, lx, base, block.font, color)
        lastX = lx + measureText(shown, block.font)
        lastBaseline = base
    }
    if (caretColor != null && caretVisible && reveal < 1.0001f) {
        val m = metrics(block.font)
        fillRect(lastX + 6f, lastBaseline - m.ascent * 0.9f, lastX + 6f + block.font.size * 0.09f, lastBaseline + m.descent * 0.4f, caretColor)
    }
}

/** Texte espacé (petites capitales des étiquettes). Retourne la largeur dessinée. */
fun Gfx.drawTracked(text: String, x: Float, baseline: Float, font: FontSpec, color: Int, tracking: Float): Float {
    var cx = x
    for (ch in text) {
        val s = ch.toString()
        drawText(s, cx, baseline, font, color)
        cx += measureText(s, font) + tracking
    }
    return cx - x - tracking
}

fun Gfx.measureTracked(text: String, font: FontSpec, tracking: Float): Float =
    text.sumOf { measureText(it.toString(), font).toDouble() }.toFloat() + tracking * (text.length - 1).coerceAtLeast(0)

/** Générateur pseudo-aléatoire déterministe (xorshift32) : même graine, même décor, à chaque rendu. */
class Rand(seed: Int) {
    private var s = if (seed == 0) 0x9E3779B9.toInt() else seed
    fun nextInt(): Int { s = s xor (s shl 13); s = s xor (s ushr 17); s = s xor (s shl 5); return s }
    fun next(): Float = (nextInt() ushr 8) / 16777216f
    fun range(a: Float, b: Float) = a + (b - a) * next()
}
