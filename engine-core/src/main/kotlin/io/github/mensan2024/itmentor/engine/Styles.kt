// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

enum class Decor { CIRCUIT_GRID, PULSE, FIELD_ROWS }

/**
 * Style graphique d'un domaine. Le moteur ne contient aucune règle propre à un domaine :
 * changer de style change les couleurs, le décor animé et le pictogramme de marque, rien d'autre.
 * Les couleurs reprennent la charte IT_MENTOR (version « vidéo », sur fond sombre).
 */
data class Style(
    val id: String,
    val name: String,
    val bgTop: Int,
    val bgBottom: Int,
    val surface: Int,
    val surfaceStroke: Int,
    val text: Int,
    val textMuted: Int,
    val accent: Int,
    val accentInk: Int,
    val highlight: Int,
    val highlightInk: Int,
    val ok: Int,
    val danger: Int,
    val decor: Decor,
)

object Styles {
    val CYBER = Style(
        id = "cyber", name = "Cybersécurité",
        bgTop = rgb(0x0B1220), bgBottom = rgb(0x0E1A2B),
        surface = rgb(0x14223A), surfaceStroke = rgb(0x263754),
        text = rgb(0xF8FAFC), textMuted = rgb(0x9FB0C8),
        accent = rgb(0x22D3EE), accentInk = rgb(0x06202A),
        highlight = rgb(0xF5B301), highlightInk = rgb(0x0E1A2B),
        ok = rgb(0x34D399), danger = rgb(0xFB7185),
        decor = Decor.CIRCUIT_GRID,
    )
    val SANTE = Style(
        id = "sante", name = "Santé et prévention",
        bgTop = rgb(0x05201B), bgBottom = rgb(0x0A2D26),
        surface = rgb(0x0E3A31), surfaceStroke = rgb(0x1B5446),
        text = rgb(0xF0FDF8), textMuted = rgb(0xA3C9BD),
        accent = rgb(0x34D399), accentInk = rgb(0x03241B),
        highlight = rgb(0xF5B301), highlightInk = rgb(0x0E1A2B),
        ok = rgb(0x6EE7B7), danger = rgb(0xFB7185),
        decor = Decor.PULSE,
    )
    val AGRO = Style(
        id = "agro", name = "Agriculture et agronomie",
        bgTop = rgb(0x121C08), bgBottom = rgb(0x1B2A0D),
        surface = rgb(0x233517), surfaceStroke = rgb(0x3A5226),
        text = rgb(0xFBFAF4), textMuted = rgb(0xBCC8A6),
        accent = rgb(0xA3E635), accentInk = rgb(0x1A2606),
        highlight = rgb(0xF59E0B), highlightInk = rgb(0x1A1205),
        ok = rgb(0xA3E635), danger = rgb(0xFB7185),
        decor = Decor.FIELD_ROWS,
    )

    val ALL = listOf(CYBER, SANTE, AGRO)

    fun byId(id: String): Style = ALL.firstOrNull { it.id == id } ?: throw StoryboardException("Style inconnu : $id")
}
