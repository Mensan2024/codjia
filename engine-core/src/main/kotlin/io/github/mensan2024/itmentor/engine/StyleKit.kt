// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Styles modulables : un style de vidéo est une combinaison de pièces interchangeables
 * (palette, fond, polices, traitement des titres, en-tête, cadre des cartes, sous-titres, entrée des
 * plans, mascotte). Des règles de design écartent les combinaisons illisibles ou trop chargées :
 * chaque génération obtient un style différent, mais toujours propre.
 *
 * Les pièces sont décrites par des données (pas de code par style) : ajouter une palette ou une police
 * ajoute des dizaines de combinaisons d'un coup.
 */
data class Palette(
    val name: String,
    val light: Boolean,
    val bgTop: Int, val bgBottom: Int,
    val ink: Int, val muted: Int, val dim: Int,
    val accent: Int, val accent2: Int,
    val card: Int, val inner: Int, val chip: Int,
    /** Cadre du mot surligné et couleur du texte dedans. */
    val highlight: Int, val onHighlight: Int,
)

enum class KitBackground { GRID_FLOOR, STARS, AURORA, MESH, NEON_LINES, DOTS, PAPER, SUNSET, GLOW }
enum class KitTitleFx { HIGHLIGHT, MARKER, COLOR, TYPEWRITER }
enum class KitHeader { BADGE, CHIP, BAR }
enum class KitFrame { DOUBLE, GLASS, NEON, FLAT }
enum class KitCaptions { BOX, COLOR, POP, BAR }
enum class KitMotion { RISE, DROP, ZOOM, POP }
enum class KitMascot { BALL, BOT, NONE }

data class StyleKit(
    val palette: Palette,
    val background: KitBackground,
    val titleFont: FontSpec,
    val captionFont: FontSpec,
    val titleFx: KitTitleFx,
    val header: KitHeader,
    val frame: KitFrame,
    val captions: KitCaptions,
    val motion: KitMotion,
    val mascot: KitMascot,
    /** Petit éclair lumineux à chaque coupe. */
    val flash: Boolean,
) {
    /** Description courte, pour l'interface (« Minuit néon · Karaoké · Grille »). */
    val label: String get() = "${palette.name} · ${BG_NAMES.getValue(background)} · ${CAP_NAMES.getValue(captions)}"

    companion object {
        private fun rgb(hex: Long) = (0xFF000000L or hex).toInt()

        val PALETTES = listOf(
            Palette("Charbon orange", false, rgb(0x1C1C1E), rgb(0x0B0B0C), rgb(0xFFFFFF), rgb(0x8A8A8F), rgb(0x5A5A60),
                rgb(0xFF7A33), rgb(0xFFD23F), rgb(0x1E1E20), rgb(0x262628), rgb(0x2C2C30), rgb(0xFFFFFF), rgb(0x111111)),
            Palette("Minuit cyan", false, rgb(0x0B1530), rgb(0x050914), rgb(0xF2F7FF), rgb(0x8EA0BF), rgb(0x4A5878),
                rgb(0x22D3EE), rgb(0xA78BFA), rgb(0x101C3A), rgb(0x16264A), rgb(0x1B2D57), rgb(0x22D3EE), rgb(0x06121F)),
            Palette("Violet néon", false, rgb(0x1A0B2E), rgb(0x0A0414), rgb(0xFFF5FF), rgb(0xB39BCC), rgb(0x5E4A78),
                rgb(0xFF3CAC), rgb(0x7B5CFF), rgb(0x1F1036), rgb(0x2A1748), rgb(0x331C57), rgb(0xFF3CAC), rgb(0xFFFFFF)),
            Palette("Noir citron", false, rgb(0x121212), rgb(0x050505), rgb(0xFFFFFF), rgb(0x9A9A9A), rgb(0x555555),
                rgb(0xC6FF3D), rgb(0x3DD9FF), rgb(0x1A1A1A), rgb(0x232323), rgb(0x2A2A2A), rgb(0xC6FF3D), rgb(0x0A0A0A)),
            Palette("Marine soleil", false, rgb(0x0C1E3C), rgb(0x061226), rgb(0xFFFFFF), rgb(0x9DB0CF), rgb(0x4D6286),
                rgb(0xFFC93C), rgb(0xFF6B6B), rgb(0x112A52), rgb(0x173463), rgb(0x1C3D73), rgb(0xFFC93C), rgb(0x0C1E3C)),
            Palette("Forêt menthe", false, rgb(0x0E1F1A), rgb(0x06110D), rgb(0xF1FFF8), rgb(0x8FB3A5), rgb(0x4B6B5F),
                rgb(0x3DF5A7), rgb(0xFFD166), rgb(0x13291F), rgb(0x1A352A), rgb(0x1F3E31), rgb(0x3DF5A7), rgb(0x06110D)),
            Palette("Crème encre", true, rgb(0xFBF6EC), rgb(0xF1E9D8), rgb(0x16161A), rgb(0x6B6458), rgb(0xB8AE9C),
                rgb(0xFF5A1F), rgb(0x2F6BFF), rgb(0xFFFFFF), rgb(0xF4EEE2), rgb(0xEDE4D3), rgb(0x16161A), rgb(0xFFFFFF)),
            Palette("Blanc bleu", true, rgb(0xF5F8FF), rgb(0xE6ECFA), rgb(0x0F172A), rgb(0x5B6B88), rgb(0xA9B6CE),
                rgb(0x2563EB), rgb(0xF59E0B), rgb(0xFFFFFF), rgb(0xEEF2FB), rgb(0xE3E9F7), rgb(0x2563EB), rgb(0xFFFFFF)),
            Palette("Papier rouge", true, rgb(0xF7F3EE), rgb(0xECE5DC), rgb(0x1B1B1B), rgb(0x6E655C), rgb(0xBDB3A8),
                rgb(0xE63946), rgb(0x1D3557), rgb(0xFFFFFF), rgb(0xF2ECE5), rgb(0xEAE2D8), rgb(0xE63946), rgb(0xFFFFFF)),
            Palette("Nuit indigo", false, rgb(0x0B1224), rgb(0x1A1446), rgb(0xF4F6FA), rgb(0x8E98AD), rgb(0x4A5470),
                rgb(0x37C6F4), rgb(0xFFC43D), rgb(0x111A2E), rgb(0x16213A), rgb(0x1B2744), rgb(0x37C6F4), rgb(0x0B1224)),
            Palette("Ardoise corail", false, rgb(0x15181F), rgb(0x0A0C10), rgb(0xF7F7F8), rgb(0x9499A6), rgb(0x50555F),
                rgb(0xFF6F59), rgb(0x43D9AD), rgb(0x1C2029), rgb(0x232833), rgb(0x2A303C), rgb(0xFF6F59), rgb(0xFFFFFF)),
            Palette("Océan turquoise", false, rgb(0x04263A), rgb(0x021521), rgb(0xEFFCFF), rgb(0x8CB5C4), rgb(0x3F6878),
                rgb(0x2EE6D6), rgb(0xFF9F1C), rgb(0x07324B), rgb(0x0B3D5A), rgb(0x0F4766), rgb(0x2EE6D6), rgb(0x021521)),
        )

        val TITLE_FONTS = listOf(
            FontSpec(FontFamily.TEXT, 800, 66f), FontSpec(FontFamily.DISPLAY, 900, 66f), FontSpec(FontFamily.ROUNDED, 800, 66f),
            FontSpec(FontFamily.GROTESK, 800, 66f), FontSpec(FontFamily.CONDENSED, 800, 76f),
            FontSpec(FontFamily.SERIF, 400, 70f),
        )
        val CAPTION_FONTS = listOf(
            FontSpec(FontFamily.TEXT, 800, 56f), FontSpec(FontFamily.ROUNDED, 800, 56f), FontSpec(FontFamily.CONDENSED, 800, 66f),
            FontSpec(FontFamily.DISPLAY, 900, 56f),
        )

        private val BG_NAMES = mapOf(KitBackground.GRID_FLOOR to "Grille", KitBackground.STARS to "Étoiles", KitBackground.AURORA to "Aurore",
            KitBackground.MESH to "Halo", KitBackground.NEON_LINES to "Néon", KitBackground.DOTS to "Points", KitBackground.PAPER to "Papier",
            KitBackground.SUNSET to "Planète", KitBackground.GLOW to "Lueur")
        private val CAP_NAMES = mapOf(KitCaptions.BOX to "Karaoké", KitCaptions.COLOR to "Mot en couleur", KitCaptions.POP to "Mots géants",
            KitCaptions.BAR to "Bandeau")

        /** Le style de référence (format « Montage dynamique »). */
        val MONTAGE = StyleKit(PALETTES[0], KitBackground.GRID_FLOOR, TITLE_FONTS[0], CAPTION_FONTS[0], KitTitleFx.HIGHLIGHT,
            KitHeader.BADGE, KitFrame.DOUBLE, KitCaptions.BOX, KitMotion.RISE, KitMascot.BALL, false)

        private val DARK_BG = listOf(KitBackground.GRID_FLOOR, KitBackground.STARS, KitBackground.AURORA, KitBackground.MESH,
            KitBackground.NEON_LINES, KitBackground.DOTS, KitBackground.SUNSET, KitBackground.GLOW)
        private val LIGHT_BG = listOf(KitBackground.PAPER, KitBackground.DOTS, KitBackground.MESH)
        private val BUSY_BG = setOf(KitBackground.STARS, KitBackground.AURORA, KitBackground.NEON_LINES, KitBackground.SUNSET)

        /**
         * Combinaison tirée par la graine, avec les règles de design :
         * - palette claire → fond clair (papier, points, halo), jamais de cadre néon ;
         * - fond chargé → les cartes ont toujours un cadre (lisibilité) ;
         * - police condensée → pas de machine à écrire (déjà très expressive) ;
         * - « Mots géants » en sous-titres → la mascotte reste (sinon le bas paraît vide) ;
         * - deux effets forts (éclair + entrée « pop ») ne vont jamais ensemble.
         */
        /** Nombre de combinaisons possibles (pour l'interface : « des milliers de styles »). */
        val combinations: Long get() = PALETTES.size.toLong() * DARK_BG.size * TITLE_FONTS.size * CAPTION_FONTS.size *
            KitTitleFx.entries.size * KitHeader.entries.size * KitFrame.entries.size * KitCaptions.entries.size * KitMotion.entries.size

        /** Le style d'origine de l'Infographie (référence « astuce 026 »). */
        val INFOGRAPHIE = StyleKit(PALETTES.first { it.name == "Nuit indigo" }, KitBackground.GLOW, FontSpec(FontFamily.TEXT, 800, 66f),
            CAPTION_FONTS[0], KitTitleFx.COLOR, KitHeader.CHIP, KitFrame.FLAT, KitCaptions.BOX, KitMotion.RISE, KitMascot.NONE, false)

        /**
         * Style d'une infographie : fond sombre uniquement (les terminaux et graphiques y restent lisibles),
         * polices de titre sobres (le schéma porte déjà le message), une fois sur cinq le style d'origine.
         */
        fun infographic(seed: Long): StyleKit {
            val r = Rand.of(seed, 0x1F05)
            if ((r.nextInt() ushr 1) % 5 == 0) return INFOGRAPHIE
            fun <T> pick(l: List<T>): T = l[(r.nextInt() ushr 1) % l.size]
            val pal = pick(PALETTES.filter { !it.light })
            val fonts = TITLE_FONTS.filter { it.family != FontFamily.SERIF }
            return StyleKit(pal, pick(DARK_BG), pick(fonts), CAPTION_FONTS[0], pick(listOf(KitTitleFx.COLOR, KitTitleFx.HIGHLIGHT, KitTitleFx.MARKER)),
                pick(KitHeader.entries), pick(KitFrame.entries), KitCaptions.BOX, pick(KitMotion.entries), KitMascot.NONE, r.next() < 0.3f)
        }

        fun random(seed: Long): StyleKit {
            val r = Rand.of(seed, 0x57E1)
            fun <T> pick(l: List<T>): T = l[(r.nextInt() ushr 1) % l.size]
            val pal = pick(PALETTES)
            val bg = pick(if (pal.light) LIGHT_BG else DARK_BG)
            val frames = KitFrame.entries.filter { !(pal.light && it == KitFrame.NEON) && !(bg in BUSY_BG && it == KitFrame.FLAT) }
            val tf = pick(TITLE_FONTS)
            val fxs = KitTitleFx.entries.filter { !(it == KitTitleFx.TYPEWRITER && tf.family == FontFamily.CONDENSED) }
            val caps = pick(KitCaptions.entries)
            val motion = pick(KitMotion.entries)
            val flash = motion != KitMotion.POP && r.next() < 0.5f
            val mascots = if (caps == KitCaptions.POP) listOf(KitMascot.BALL, KitMascot.BOT) else KitMascot.entries
            return StyleKit(pal, bg, tf, pick(CAPTION_FONTS), pick(fxs), pick(KitHeader.entries), pick(frames), caps, motion, pick(mascots), flash)
        }
    }
}
