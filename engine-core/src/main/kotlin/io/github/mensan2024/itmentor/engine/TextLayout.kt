// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Bloc de texte mis en page : lignes déjà coupées, police finale, interligne. */
data class TextBlock(val lines: List<String>, val font: FontSpec, val lineHeight: Float, val truncated: Boolean) {
    val height: Float get() = if (lines.isEmpty()) 0f else lineHeight * lines.size
    val charCount: Int get() = lines.sumOf { it.length }
}

/**
 * Retours à la ligne automatiques et ajustement des textes trop longs.
 * - On ne coupe que sur une espace normale : les espaces insécables (U+00A0, U+202F) de la typographie
 *   française restent collées (« texte », « Vrai ou faux ? »).
 * - Un mot plus long que la ligne est coupé entre deux caractères.
 * - `fit` réduit la taille jusqu'à tenir dans `maxLines`, puis tronque avec « … » en dernier recours.
 */
object TextLayout {
    private const val ELLIPSIS = "…"

    fun wrap(g: Gfx, text: String, font: FontSpec, maxWidth: Float): List<String> {
        val out = ArrayList<String>()
        for (paragraph in text.split('\n')) {
            val words = paragraph.split(' ').filter { it.isNotEmpty() }
            if (words.isEmpty()) { out += ""; continue }
            var line = ""
            for (word in words) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (g.measureText(candidate, font) <= maxWidth) {
                    line = candidate
                    continue
                }
                if (line.isNotEmpty()) { out += line; line = "" }
                if (g.measureText(word, font) <= maxWidth) {
                    line = word
                } else {
                    // Mot trop long : coupe caractère par caractère (adresses, hashtags…).
                    var chunk = ""
                    for (ch in word) {
                        if (chunk.isNotEmpty() && g.measureText(chunk + ch, font) > maxWidth) { out += chunk; chunk = "" }
                        chunk += ch
                    }
                    line = chunk
                }
            }
            if (line.isNotEmpty()) out += line
        }
        return out
    }

    fun fit(
        g: Gfx, text: String, font: FontSpec, maxWidth: Float, maxLines: Int,
        minSize: Float = font.size * 0.62f, lineHeightFactor: Float = 1.18f,
    ): TextBlock {
        require(maxLines >= 1)
        var size = font.size
        while (true) {
            val f = font.withSize(size)
            val raw = wrap(g, text, f, maxWidth)
            val lines = if (text.contains('\n')) raw else avoidWidow(g, raw, f, maxWidth)
            if (lines.size <= maxLines) return TextBlock(lines, f, size * lineHeightFactor, truncated = false)
            val next = size * 0.94f
            if (next < minSize) {
                val kept = lines.take(maxLines).toMutableList()
                var last = kept.last()
                while (last.isNotEmpty() && g.measureText(last.trimEnd() + ELLIPSIS, f) > maxWidth) last = last.dropLast(1)
                kept[kept.lastIndex] = last.trimEnd() + ELLIPSIS
                return TextBlock(kept, f, size * lineHeightFactor, truncated = true)
            }
            size = next
        }
    }

    /**
     * Évite une « veuve » (un mot seul sur la dernière ligne) : on descend le dernier mot
     * de l'avant-dernière ligne si la dernière ligne reste dans la largeur.
     */
    fun avoidWidow(g: Gfx, lines: List<String>, font: FontSpec, maxWidth: Float): List<String> {
        if (lines.size < 2) return lines
        val last = lines.last()
        val prev = lines[lines.size - 2]
        if (last.contains(' ') || !prev.contains(' ')) return lines
        val cut = prev.lastIndexOf(' ')
        val moved = prev.substring(cut + 1) + " " + last
        if (g.measureText(moved, font) > maxWidth) return lines
        return lines.dropLast(2) + prev.substring(0, cut) + moved
    }

    /** Lignes coupées à `reveal` (machine à écrire), sans jamais déplacer le texte déjà affiché. */
    fun revealed(block: TextBlock, reveal: Float): List<String> {
        var budget = (block.charCount * clamp01(reveal)).toInt()
        val out = ArrayList<String>()
        for (l in block.lines) {
            if (budget <= 0) break
            out += if (l.length <= budget) l else l.substring(0, budget)
            budget -= l.length
        }
        return out
    }
}
