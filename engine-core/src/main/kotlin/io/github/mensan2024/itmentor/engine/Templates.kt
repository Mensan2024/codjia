// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Fond animé propre à chaque template. */
enum class Decor { CIRCUIT_GRID, CORPORATE_LINES, EDU_SHAPES, ORGANIC_LEAVES, MEDICAL_PULSE, MARKETING_BLOCKS, GIANT_TYPE, CINEMA_LIGHT,
    CODE_EDITOR, BLUEPRINT }

/** Façon dont les titres apparaissent. */
enum class TitleMotion {
    WORDS_UP,      // les mots montent un par un
    WORDS_POP,     // les mots « rebondissent » en grossissant
    MASK_REVEAL,   // chaque ligne sort d'un masque, comme d'une fente
    LETTERS_FADE,  // lettres en fondu lent (cinéma)
}

/** Mise en valeur des mots entre *astérisques*. */
enum class Emphasis { GLOW, UNDERLINE, MARKER, BOX }

/** Traitement des cartes (définition, comparaison). */
enum class CardStyle { GLASS, SOLID, OUTLINE }

/**
 * Un template = une identité complète : palette, typographies, mouvements, transitions et décor.
 * Le contenu (le storyboard) ne change pas ; seul l'habillage change.
 */
data class Template(
    val id: String,
    val name: String,
    val tagline: String,
    val style: Style,
    val decor: Decor,
    val titleFont: FontSpec,
    val titleScale: Float = 1f,
    val uppercaseTitles: Boolean = false,
    val kickerFont: FontSpec = FontSpec(FontFamily.TEXT, 700, 26f),
    val motion: TitleMotion,
    val emphasis: Emphasis,
    val card: CardStyle,
    val radius: Float = 32f,
    /** Transitions utilisées à tour de rôle entre les scènes (jamais deux fois la même d'affilée si possible). */
    val transitions: List<TransitionType>,
    val transitionMs: Int = 550,
    /** Animation d'entrée des listes et des cartes. */
    val enter: AnimType = AnimType.SLIDE_UP,
    val domains: Set<Domain>,
    val letterbox: Boolean = false,
    val grain: Boolean = false,
)

object Templates {
    private fun f(family: FontFamily, weight: Int, size: Float = 88f) = FontSpec(family, weight, size)

    val CYBER_NEON = Template(
        id = "cyber-neon", name = "Cyber Neon", tagline = "Bleu nuit, néons, circuits",
        style = Style(
            dark = true, bgTop = rgb(0x070D1C), bgBottom = rgb(0x0D1730), surface = rgb(0x111D38), surfaceStroke = rgb(0x24407A),
            text = rgb(0xF4F8FF), textMuted = rgb(0x9DB0CF), accent = rgb(0x22D3EE), accent2 = rgb(0x8B5CF6),
            accentInk = rgb(0x041A22), emphasis = rgb(0x22D3EE), highlight = rgb(0xF5B301), highlightInk = rgb(0x0E1A2B),
            ok = rgb(0x34D399), danger = rgb(0xFB7185),
        ),
        decor = Decor.CIRCUIT_GRID, titleFont = f(FontFamily.DISPLAY, 800),
        kickerFont = FontSpec(FontFamily.TECH, 500, 26f),
        motion = TitleMotion.WORDS_UP, emphasis = Emphasis.GLOW, card = CardStyle.GLASS,
        transitions = listOf(TransitionType.WIPE, TransitionType.ZOOM, TransitionType.SLIDE, TransitionType.WIPE, TransitionType.FADE),
        enter = AnimType.SLIDE_LEFT, domains = setOf(Domain.CYBER, Domain.RESEAUX, Domain.IA),
    )

    val CORPORATE_PREMIUM = Template(
        id = "corporate-premium", name = "Corporate Premium", tagline = "Ivoire, or, sobriété",
        style = Style(
            dark = false, bgTop = rgb(0xFBF8F2), bgBottom = rgb(0xF1EADB), surface = rgb(0xFFFFFF), surfaceStroke = rgb(0xE2D6BC),
            text = rgb(0x14213D), textMuted = rgb(0x5B6478), accent = rgb(0xB8892B), accent2 = rgb(0x14213D),
            accentInk = rgb(0xFFFFFF), emphasis = rgb(0x8A6416), highlight = rgb(0x14213D), highlightInk = rgb(0xFBF8F2),
            ok = rgb(0x1F7A4D), danger = rgb(0xB42318),
        ),
        decor = Decor.CORPORATE_LINES, titleFont = f(FontFamily.SERIF, 400), titleScale = 1.02f,
        kickerFont = FontSpec(FontFamily.TEXT, 600, 24f),
        motion = TitleMotion.MASK_REVEAL, emphasis = Emphasis.UNDERLINE, card = CardStyle.SOLID, radius = 14f,
        transitions = listOf(TransitionType.FADE, TransitionType.SLIDE, TransitionType.DIP, TransitionType.SLIDE),
        transitionMs = 650, enter = AnimType.FADE_IN, domains = setOf(Domain.GENERAL, Domain.MARKETING, Domain.IA),
    )

    val MODERN_EDUCATION = Template(
        id = "modern-education", name = "Modern Education", tagline = "Clair, coloré, pédagogique",
        style = Style(
            dark = false, bgTop = rgb(0xFFFBF2), bgBottom = rgb(0xFFF1D6), surface = rgb(0xFFFFFF), surfaceStroke = rgb(0xF2DDB0),
            text = rgb(0x1E1B4B), textMuted = rgb(0x4B4F6B), accent = rgb(0x2563EB), accent2 = rgb(0xF97316),
            accentInk = rgb(0xFFFFFF), emphasis = rgb(0x1E1B4B), highlight = rgb(0xFACC15), highlightInk = rgb(0x1E1B4B),
            ok = rgb(0x16A34A), danger = rgb(0xDC2626),
        ),
        decor = Decor.EDU_SHAPES, titleFont = f(FontFamily.ROUNDED, 700),
        motion = TitleMotion.WORDS_POP, emphasis = Emphasis.MARKER, card = CardStyle.SOLID, radius = 36f,
        transitions = listOf(TransitionType.CIRCLE, TransitionType.SLIDE, TransitionType.WIPE, TransitionType.CIRCLE),
        enter = AnimType.ZOOM_IN, domains = setOf(Domain.ANGLAIS, Domain.GENERAL),
    )

    val GREEN_NATURE = Template(
        id = "green-nature", name = "Green Nature", tagline = "Vert profond, lumière, végétal",
        style = Style(
            dark = true, bgTop = rgb(0x0B2A1A), bgBottom = rgb(0x143D22), surface = rgb(0x184A2A), surfaceStroke = rgb(0x2E6B3F),
            text = rgb(0xF7FBEF), textMuted = rgb(0xBFD3B0), accent = rgb(0xA3E635), accent2 = rgb(0xF59E0B),
            accentInk = rgb(0x132406), emphasis = rgb(0xD9F99D), highlight = rgb(0xF59E0B), highlightInk = rgb(0x1A1205),
            ok = rgb(0xA3E635), danger = rgb(0xFCA5A5),
        ),
        decor = Decor.ORGANIC_LEAVES, titleFont = f(FontFamily.GROTESK, 700),
        motion = TitleMotion.WORDS_UP, emphasis = Emphasis.UNDERLINE, card = CardStyle.GLASS, radius = 40f,
        transitions = listOf(TransitionType.FADE, TransitionType.CIRCLE, TransitionType.SLIDE, TransitionType.FADE),
        transitionMs = 650, enter = AnimType.SLIDE_UP, domains = setOf(Domain.SANTE),
    )

    val MEDICAL_CLEAN = Template(
        id = "medical-clean", name = "Medical Clean", tagline = "Blanc, menthe, confiance",
        style = Style(
            dark = false, bgTop = rgb(0xF6FCFB), bgBottom = rgb(0xE3F4F1), surface = rgb(0xFFFFFF), surfaceStroke = rgb(0xC9E8E2),
            text = rgb(0x0B2E2A), textMuted = rgb(0x3F5F5A), accent = rgb(0x0F766E), accent2 = rgb(0x38BDF8),
            accentInk = rgb(0xFFFFFF), emphasis = rgb(0x0F766E), highlight = rgb(0x0F766E), highlightInk = rgb(0xFFFFFF),
            ok = rgb(0x15803D), danger = rgb(0xC2410C),
        ),
        decor = Decor.MEDICAL_PULSE, titleFont = f(FontFamily.CLEAN, 700),
        motion = TitleMotion.WORDS_UP, emphasis = Emphasis.UNDERLINE, card = CardStyle.OUTLINE, radius = 28f,
        transitions = listOf(TransitionType.CIRCLE, TransitionType.FADE, TransitionType.SLIDE, TransitionType.FADE),
        enter = AnimType.FADE_IN, domains = setOf(Domain.SANTE, Domain.GENERAL),
    )

    val MARKETING_IMPACT = Template(
        id = "marketing-impact", name = "Marketing Impact", tagline = "Couleurs vives, blocs, énergie",
        style = Style(
            dark = true, bgTop = rgb(0x2A0A3D), bgBottom = rgb(0x4A0E4E), surface = rgb(0x3B1250), surfaceStroke = rgb(0x7A2A8C),
            text = rgb(0xFFFFFF), textMuted = rgb(0xF3D2F2), accent = rgb(0xFF3D7F), accent2 = rgb(0xFFB020),
            accentInk = rgb(0xFFFFFF), emphasis = rgb(0x1A0626), highlight = rgb(0xFFB020), highlightInk = rgb(0x1A0626),
            ok = rgb(0x4ADE80), danger = rgb(0xFF8FA3),
        ),
        decor = Decor.MARKETING_BLOCKS, titleFont = f(FontFamily.CONDENSED, 700), titleScale = 1.25f, uppercaseTitles = true,
        kickerFont = FontSpec(FontFamily.CONDENSED, 700, 34f),
        motion = TitleMotion.WORDS_POP, emphasis = Emphasis.BOX, card = CardStyle.SOLID, radius = 18f,
        transitions = listOf(TransitionType.WIPE, TransitionType.ZOOM, TransitionType.SLIDE, TransitionType.WIPE),
        transitionMs = 500, enter = AnimType.SLIDE_RIGHT, domains = setOf(Domain.MARKETING, Domain.ANGLAIS),
    )

    val BOLD_TYPOGRAPHY = Template(
        id = "bold-typography", name = "Bold Typography", tagline = "Noir, jaune, titres géants",
        style = Style(
            dark = true, bgTop = rgb(0x0A0A0A), bgBottom = rgb(0x141414), surface = rgb(0x1C1C1C), surfaceStroke = rgb(0x3A3A3A),
            text = rgb(0xFAFAFA), textMuted = rgb(0xB5B5B5), accent = rgb(0xFFE600), accent2 = rgb(0xFF4D2E),
            accentInk = rgb(0x0A0A0A), emphasis = rgb(0x0A0A0A), highlight = rgb(0xFFE600), highlightInk = rgb(0x0A0A0A),
            ok = rgb(0xFFE600), danger = rgb(0xFF4D2E),
        ),
        decor = Decor.GIANT_TYPE, titleFont = f(FontFamily.HEAVY, 400), titleScale = 0.74f, uppercaseTitles = true,
        kickerFont = FontSpec(FontFamily.TEXT, 800, 26f),
        motion = TitleMotion.MASK_REVEAL, emphasis = Emphasis.BOX, card = CardStyle.OUTLINE, radius = 6f,
        transitions = listOf(TransitionType.SLIDE, TransitionType.WIPE, TransitionType.ZOOM, TransitionType.SLIDE),
        transitionMs = 450, enter = AnimType.SLIDE_LEFT, domains = setOf(Domain.IA, Domain.CYBER, Domain.MARKETING, Domain.GENERAL),
    )

    val CINEMATIC_STORY = Template(
        id = "cinematic-story", name = "Cinematic Story", tagline = "Lumière chaude, cadre cinéma",
        style = Style(
            dark = true, bgTop = rgb(0x120D0A), bgBottom = rgb(0x231812), surface = rgb(0x2A1E16), surfaceStroke = rgb(0x5A4232),
            text = rgb(0xFFF6EA), textMuted = rgb(0xD8C3A8), accent = rgb(0xF2A65A), accent2 = rgb(0xE85D3B),
            accentInk = rgb(0x1A0F08), emphasis = rgb(0xF6C48D), highlight = rgb(0xF2A65A), highlightInk = rgb(0x1A0F08),
            ok = rgb(0xB5D99C), danger = rgb(0xF28B82),
        ),
        decor = Decor.CINEMA_LIGHT, titleFont = f(FontFamily.CINEMA, 400), titleScale = 1.18f,
        kickerFont = FontSpec(FontFamily.TEXT, 600, 24f),
        motion = TitleMotion.LETTERS_FADE, emphasis = Emphasis.UNDERLINE, card = CardStyle.GLASS, radius = 20f,
        transitions = listOf(TransitionType.DIP, TransitionType.ZOOM, TransitionType.FADE, TransitionType.DIP),
        transitionMs = 800, enter = AnimType.FADE_IN, domains = setOf(Domain.GENERAL, Domain.SANTE, Domain.IA), letterbox = true, grain = true,
    )

    val CODE_STUDIO = Template(
        id = "code-studio", name = "Code Studio", tagline = "Éditeur de code, coloration, terminal",
        style = Style(
            dark = true, bgTop = rgb(0x0D1117), bgBottom = rgb(0x161B22), surface = rgb(0x1C2230), surfaceStroke = rgb(0x30363D),
            text = rgb(0xF0F6FC), textMuted = rgb(0xA6B3C2), accent = rgb(0x7EE787), accent2 = rgb(0xD2A8FF),
            accentInk = rgb(0x0D1117), emphasis = rgb(0x79C0FF), highlight = rgb(0xFFA657), highlightInk = rgb(0x0D1117),
            ok = rgb(0x7EE787), danger = rgb(0xFF7B72),
        ),
        decor = Decor.CODE_EDITOR, titleFont = f(FontFamily.MONO, 700), titleScale = 0.86f,
        kickerFont = FontSpec(FontFamily.MONO, 700, 26f),
        motion = TitleMotion.LETTERS_FADE, emphasis = Emphasis.UNDERLINE, card = CardStyle.GLASS, radius = 14f,
        transitions = listOf(TransitionType.SLIDE, TransitionType.WIPE, TransitionType.FADE, TransitionType.ZOOM),
        transitionMs = 450, enter = AnimType.SLIDE_LEFT, domains = setOf(Domain.FULLSTACK, Domain.SYSTEMES, Domain.IA),
    )

    val NETWORK_BLUEPRINT = Template(
        id = "network-blueprint", name = "Network Blueprint", tagline = "Plan technique, topologie, paquets",
        style = Style(
            dark = true, bgTop = rgb(0x0A2540), bgBottom = rgb(0x0C3157), surface = rgb(0x103A66), surfaceStroke = rgb(0x2F6DA8),
            text = rgb(0xF2F8FF), textMuted = rgb(0xA9C6E6), accent = rgb(0x5CC8FF), accent2 = rgb(0xFFD166),
            accentInk = rgb(0x06223D), emphasis = rgb(0xFFD166), highlight = rgb(0xFFD166), highlightInk = rgb(0x0A2540),
            ok = rgb(0x6EE7B7), danger = rgb(0xFF8FA3),
        ),
        decor = Decor.BLUEPRINT, titleFont = f(FontFamily.TECH, 500), titleScale = 0.95f,
        kickerFont = FontSpec(FontFamily.TECH, 500, 26f),
        motion = TitleMotion.MASK_REVEAL, emphasis = Emphasis.UNDERLINE, card = CardStyle.OUTLINE, radius = 10f,
        transitions = listOf(TransitionType.WIPE, TransitionType.SLIDE, TransitionType.CIRCLE, TransitionType.FADE),
        transitionMs = 550, enter = AnimType.SLIDE_LEFT, domains = setOf(Domain.RESEAUX, Domain.SYSTEMES, Domain.CYBER),
    )

    val ALL = listOf(CYBER_NEON, NETWORK_BLUEPRINT, CODE_STUDIO, CORPORATE_PREMIUM, MODERN_EDUCATION, GREEN_NATURE,
        MEDICAL_CLEAN, MARKETING_IMPACT, BOLD_TYPOGRAPHY, CINEMATIC_STORY)

    fun byId(id: String): Template = ALL.firstOrNull { it.id == id } ?: throw StoryboardException("Template inconnu : $id")

    /** Template de référence d'un domaine (celui dont c'est le domaine principal). */
    fun recommendFor(d: Domain): Template = ALL.firstOrNull { it.domains.first() == d } ?: MODERN_EDUCATION

    /**
     * Habille un storyboard avec un template : transitions du template (en rotation) et durée de transition.
     * Le contenu n'est pas modifié : on peut changer de template à tout moment avant l'export.
     */
    fun apply(sb: Storyboard, t: Template): Storyboard {
        var previous: TransitionType? = null
        // La graine décale la rotation : deux générations n'enchaînent pas les mêmes transitions.
        val offset = (Rand.of(sb.seed, 0x51F3).nextInt() ushr 1) % t.transitions.size
        val scenes = sb.scenes.mapIndexed { i, s ->
            var k = i + offset
            var type = t.transitions[k % t.transitions.size]
            // Jamais deux fois la même transition d'affilée : on prend la suivante de la liste.
            while (type == previous && t.transitions.toSet().size > 1) type = t.transitions[++k % t.transitions.size]
            previous = type
            // Une transition ne peut pas dépasser 40 % de la plus courte des deux scènes concernées.
            val next = sb.scenes.getOrNull(i + 1)
            // Entre deux étapes d'une même démonstration : coupe franche, la caméra assure la continuité.
            if (s.layout == Layout.SCREEN && next?.layout == Layout.SCREEN) return@mapIndexed s.copy(transitionOut = Transition.CUT)
            val maxMs = ((minOf(s.durationMs, next?.durationMs ?: s.durationMs)) * 0.4).toInt()
            s.copy(transitionOut = Transition(type, minOf(t.transitionMs, maxMs)))
        }
        return sb.copy(templateId = t.id, scenes = scenes)
    }

    /**
     * Variation visuelle d'un template pour une graine donnée : accents inversés, autre mouvement de titre
     * compatible avec l'identité. La graine 0 rend le template tel quel (captures de référence, tests).
     */
    fun vary(t: Template, seed: Long): Template {
        if (seed == 0L) return t
        val r = Rand.of(seed, 0x2B7E)
        val s = t.style
        val swap = r.next() < 0.5f
        val style = if (swap) s.copy(accent = s.accent2, accent2 = s.accent) else s
        // Mouvements « cousins » : on reste dans l'esprit du template.
        val motions = when (t.motion) {
            TitleMotion.WORDS_UP, TitleMotion.WORDS_POP -> listOf(TitleMotion.WORDS_UP, TitleMotion.WORDS_POP, TitleMotion.MASK_REVEAL)
            TitleMotion.MASK_REVEAL -> listOf(TitleMotion.MASK_REVEAL, TitleMotion.WORDS_UP)
            TitleMotion.LETTERS_FADE -> listOf(TitleMotion.LETTERS_FADE, TitleMotion.MASK_REVEAL)
        }
        val motion = motions[(r.nextInt() ushr 1) % motions.size]
        return t.copy(style = style, motion = motion)
    }
}
