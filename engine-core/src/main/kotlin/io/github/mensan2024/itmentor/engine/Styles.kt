// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Palette d'un template. Les couleurs de texte respectent un contraste d'au moins 4,5:1 sur le fond.
 * `accent` et `accent2` servent aux formes et au décor ; `emphasis` aux mots mis en valeur ;
 * `highlight` / `highlightInk` aux pastilles d'appel à l'action.
 */
data class Style(
    val dark: Boolean,
    val bgTop: Int,
    val bgBottom: Int,
    val surface: Int,
    val surfaceStroke: Int,
    val text: Int,
    val textMuted: Int,
    val accent: Int,
    val accent2: Int,
    val accentInk: Int,
    val emphasis: Int,
    val highlight: Int,
    val highlightInk: Int,
    val ok: Int,
    val danger: Int,
) {
    /** Ombre portée des cartes. */
    val shadow: Int get() = if (dark) 0x66000000 else (text and 0x00FFFFFF) or 0x1F000000
}
