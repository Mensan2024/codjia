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

/** Texte dont certains mots sont mis en valeur : « Repère un *e-mail piégé* ». */
class Marked(val plain: String, private val marks: BooleanArray) {
    /** Le i-ème caractère non blanc est-il mis en valeur ? */
    fun isMarked(nonSpaceIndex: Int) = marks.getOrElse(nonSpaceIndex) { false }

    companion object {
        fun parse(src: String): Marked {
            val text = src.replace(' ', ' ') // espace fine absente de certaines polices
            val sb = StringBuilder()
            val marks = ArrayList<Boolean>()
            var on = false
            for (ch in text) {
                if (ch == '*') { on = !on; continue }
                sb.append(ch)
                if (ch != ' ') marks += on
            }
            return Marked(sb.toString(), marks.toBooleanArray())
        }

        fun strip(src: String) = src.replace("*", "").replace(' ', ' ')
    }
}

/** Paramètres d'un titre animé. */
class Kinetic(
    val motion: TitleMotion, val emphasis: Emphasis, val style: Style,
    val delayMs: Float, val staggerMs: Float = 70f,
)

/**
 * Titre animé mot à mot (ou lettre à lettre), avec mise en valeur des mots marqués.
 * `t` = temps local de la scène (ms). Les positions sont calculées sur la ligne complète :
 * le texte ne bouge jamais horizontalement pendant l'animation.
 */
fun Gfx.drawKinetic(block: TextBlock, marked: Marked, x: Float, top: Float, align: Align, color: Int, k: Kinetic, t: Float) {
    val f = block.font
    val m = metrics(f)
    var nonSpace = 0
    var word = 0
    var char = 0
    for ((li, line) in block.lines.withIndex()) {
        val base = baselineOf(block, top, li)
        val lineW = measureText(line, f)
        val lx = if (align == Align.CENTER) x - lineW / 2 else x
        // Masque de ligne (MASK_REVEAL) : la ligne sort d'une fente, de bas en haut.
        val lineP = Easing.EXPO_OUT.ease(window(t, k.delayMs + li * 130f, 700f))
        if (k.motion == TitleMotion.MASK_REVEAL) {
            if (lineP <= 0f) { nonSpace += line.count { it != ' ' }; word += line.split(' ').count { it.isNotEmpty() }; char += line.length; continue }
            save()
            clipRect(lx - 40f, base - m.ascent - f.size * 0.35f, lx + lineW + 60f, base + f.size * 0.32f)
            translate(0f, (1f - lineP) * block.lineHeight)
        }
        var i = 0
        while (i < line.length) {
            if (line[i] == ' ') { i++; continue }
            var j = i
            while (j < line.length && line[j] != ' ') j++
            val token = line.substring(i, j)
            val wx = lx + measureText(line.substring(0, i), f)
            val ww = measureText(token, f)
            var marked0 = false
            for (c in token.indices) if (marked.isMarked(nonSpace + c)) marked0 = true
            val start = k.delayMs + word * k.staggerMs
            drawWord(token, wx, base, ww, f, m, color, marked0, k, t, start, char)
            nonSpace += token.length
            char += token.length + 1
            word++
            i = j
        }
        if (k.motion == TitleMotion.MASK_REVEAL) restore()
    }
}

private fun Gfx.drawWord(
    token: String, wx: Float, base: Float, ww: Float, f: FontSpec, m: Metrics, color: Int, marked: Boolean,
    k: Kinetic, t: Float, start: Float, charIndex: Int,
) {
    val s = k.style
    // Mise en valeur : elle arrive juste après le mot.
    val q = if (marked) Easing.EXPO_OUT.ease(window(t, start + 260f, 520f)) else 0f
    var textColor = color
    if (marked) {
        when (k.emphasis) {
            Emphasis.GLOW -> {
                if (q > 0f) fillRadialGlow(wx + ww / 2, base - m.ascent / 2, ww * 0.75f + f.size * 0.4f, s.accent.withAlpha(0.32f * q))
                textColor = mix(color, s.emphasis, q)
            }
            Emphasis.UNDERLINE -> {
                textColor = mix(color, s.emphasis, q)
                if (q > 0f) fillRoundRect(wx, base + f.size * 0.11f, wx + ww * q, base + f.size * 0.19f, f.size * 0.04f, s.accent)
            }
            Emphasis.MARKER -> {
                val pad = f.size * 0.1f
                if (q > 0f) fillRoundRect(wx - pad, base - m.ascent * 0.52f, wx - pad + (ww + pad * 2) * q, base + f.size * 0.14f, f.size * 0.08f, s.highlight.withAlpha(0.92f))
                textColor = mix(color, s.emphasis, q)
            }
            Emphasis.BOX -> {
                val pad = f.size * 0.1f
                if (q > 0f) fillRect(wx - pad, base - m.ascent - pad, wx - pad + (ww + pad * 2) * q, base + pad, s.highlight)
                textColor = mix(color, s.emphasis, q)
            }
        }
    }
    when (k.motion) {
        TitleMotion.WORDS_UP -> {
            val p = window(t, start, 560f)
            if (p <= 0f) return
            val e = Easing.EXPO_OUT.ease(p)
            save(); multiplyAlpha(clamp01(p * 2.2f)); translate(0f, (1f - e) * f.size * 0.55f)
            drawText(token, wx, base, f, textColor); restore()
        }
        TitleMotion.WORDS_POP -> {
            val p = window(t, start, 520f)
            if (p <= 0f) return
            val e = Easing.BACK_OUT.ease(p)
            save(); multiplyAlpha(clamp01(p * 3f)); scale(lerp(0.35f, 1f, e), wx + ww / 2, base - m.ascent / 2)
            drawText(token, wx, base, f, textColor); restore()
        }
        TitleMotion.MASK_REVEAL -> drawText(token, wx, base, f, textColor)
        TitleMotion.LETTERS_FADE -> {
            var cx = wx
            for ((ci, ch) in token.withIndex()) {
                val st = ch.toString()
                val p = Easing.EASE_IN_OUT.ease(window(t, k.delayMs + (charIndex + ci) * 26f, 520f))
                if (p > 0f) {
                    save(); multiplyAlpha(p); translate(0f, (1f - p) * f.size * 0.12f)
                    drawText(st, cx, base, f, textColor); restore()
                }
                cx += measureText(st, f)
            }
        }
    }
}
