// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import kotlin.math.hypot

/**
 * Format de réalisation d'une vidéo : bien plus qu'un changement de couleurs, c'est une autre manière
 * de raconter le même contenu. Le storyboard (scènes, textes, durées) est commun à tous les formats ;
 * chaque format le met en scène à sa façon.
 */
enum class Look(val label: String, val tagline: String, val transitions: List<TransitionType>) {
    MOTION("Motion design", "Cartes animées et typographie cinétique", emptyList()),
    TABLEAU("Tableau blanc", "Une main écrit et dessine l'astuce au feutre",
        listOf(TransitionType.SLIDE, TransitionType.SLIDE, TransitionType.FADE)),
    DEMO("Démo à l'écran", "Terminal, fenêtres et clics, comme sur un vrai poste",
        listOf(TransitionType.ZOOM, TransitionType.FADE, TransitionType.SLIDE)),
    MASCOTTE("Personnage animé", "Codi, le petit robot, explique en bulles",
        listOf(TransitionType.CIRCLE, TransitionType.SLIDE, TransitionType.ZOOM)),
    EDITORIAL("Éditorial chic", "Mise en page magazine, sobre et élégante",
        listOf(TransitionType.DIP, TransitionType.FADE, TransitionType.WIPE)),
    NEON("Explicatif néon", "Titres condensés, fenêtres et terminal, sous-titres mot à mot",
        listOf(TransitionType.CUT, TransitionType.ZOOM, TransitionType.CUT)),
    COLLAGE("Collage", "Cartes en papier épinglées, étiquettes et lettres découpées",
        listOf(TransitionType.SLIDE, TransitionType.CUT, TransitionType.ZOOM)),
    CINEMA("Cinéma", "Ambiance nocturne, serif italique, téléphone réaliste",
        listOf(TransitionType.FADE, TransitionType.DIP, TransitionType.FADE)),
    INFOGRAPHIE("Infographie", "Chaque idée expliquée par un schéma animé, explication en bas",
        listOf(TransitionType.FADE, TransitionType.CUT, TransitionType.FADE)),
    EXPLICATIF("Explicatif simulé", "Chapitres numérotés, simulation d'interface et explication mot clé par mot clé",
        listOf(TransitionType.FADE, TransitionType.CUT)),
    EXPLIC_BAS("Explicatif · carte dessous", "Simulation en haut, carte de chapitre dessous, mots clés en bas",
        listOf(TransitionType.CUT, TransitionType.FADE)),
    EXPLIC_BANDEAU("Explicatif · bandeau", "Simulation haute et légende dans un bandeau",
        listOf(TransitionType.FADE, TransitionType.CUT)),
    JOURNAL("Journal télévisé", "Incrustation, bandeau de titre et bandeau défilant",
        listOf(TransitionType.WIPE, TransitionType.CUT)),
    FLASH("Flash information", "Alerte rouge, titre qui claque, texte en gros",
        listOf(TransitionType.CUT, TransitionType.ZOOM)),
    TERMINAL("Terminal hacker", "Le texte est tapé commande par commande dans un terminal",
        listOf(TransitionType.CUT)),
    BD("Bande dessinée", "Cases, éclats, bulles et légendes",
        listOf(TransitionType.SLIDE, TransitionType.CUT)),
    MONTAGE("Montage dynamique", "Chapitres numérotés, action animée, sous-titres mot à mot, mascotte",
        listOf(TransitionType.CUT)),
    KIT("Style unique", "Une nouvelle combinaison à chaque version : couleurs, fond, titres, sous-titres, mascotte",
        listOf(TransitionType.CUT)),
    SERENITE("Sérénité", "Dégradé doux, cercle qui respire, serif centré : calme et sans à-coup",
        listOf(TransitionType.FADE, TransitionType.DIP, TransitionType.FADE)),
    LUMIERE("Lumière", "Étoile de lumière, tons chauds, serif centré : méditation et parole",
        listOf(TransitionType.FADE, TransitionType.DIP, TransitionType.FADE)),
    ELAN("Élan", "Soleil levant, titres condensés, grands numéros, chaîne de progression",
        listOf(TransitionType.SLIDE, TransitionType.FADE, TransitionType.SLIDE)),
    STRATEGIE("Stratégie", "Tableau de bord, courbe de tendance et tunnel de conversion pour les étapes",
        listOf(TransitionType.SLIDE, TransitionType.FADE, TransitionType.ZOOM));

    companion object {
        fun byName(name: String?): Look = entries.firstOrNull { it.name == name } ?: MOTION
    }
}

/** Ce qu'un format doit savoir dessiner. Le [FrameRenderer] s'occupe du temps, des transitions et du pied de page. */
interface LookRenderer {
    /** Couleurs du pied de page et de l'indicateur de progression. */
    val ink: Int
    val muted: Int
    /** Couleurs des volets de transition. */
    val accent: Int
    val accent2: Int
    fun scene(g: Gfx, slot: Slot, localMs: Float, tGlobalMs: Float)
    /** Effet appliqué sur toute l'image (grain…). */
    fun overlayFx(g: Gfx, frame: Int) {}
}

object Looks {
    /** Formats adaptés à un domaine (la démo à l'écran n'a de sens que pour l'informatique). */
    fun forDomain(d: Domain): List<Look> = when (d) {
        Domain.RESEAUX, Domain.SYSTEMES, Domain.CYBER, Domain.FULLSTACK, Domain.IA -> Look.entries
        else -> Look.entries - Look.DEMO
    }

    /** Remplace les transitions du template par celles du format (sauf Motion design). */
    fun apply(sb: Storyboard, look: Look): Storyboard {
        if (look == Look.MOTION || look.transitions.isEmpty()) return sb.copy(look = look)
        val r = Rand.of(sb.seed, 0x10C4)
        var k = (r.nextInt() ushr 1) % look.transitions.size
        // Infographie : chaque étape déroule un schéma détaillé (graphique, terminal…) : 20 % de temps en plus
        // (Composer ramène ensuite la vidéo sous 59 s si besoin).
        val base = if (look == Look.INFOGRAPHIE) sb.scenes.map { if (it.layout == Layout.STEPS) it.copy(durationMs = (it.durationMs * 1.2f).toInt()) else it } else sb.scenes
        val scenes = base.mapIndexed { i, s ->
            if (i == base.lastIndex || (s.layout == Layout.SCREEN && base[i + 1].layout == Layout.SCREEN)) s else {
                val type = look.transitions[k % look.transitions.size]; k++
                val maxMs = (minOf(s.durationMs, base[i + 1].durationMs) * 0.4f).toInt()
                val want = if (type == TransitionType.SLIDE) 650 else 550
                s.copy(transitionOut = if (type == TransitionType.CUT) Transition.CUT else Transition(type, minOf(want, maxMs)))
            }
        }
        return sb.copy(scenes = scenes, look = look)
    }

    fun renderer(look: Look, template: Template, storyboard: Storyboard): LookRenderer? = when (look) {
        Look.MOTION -> null
        Look.TABLEAU -> WhiteboardLook(storyboard)
        Look.DEMO -> DemoLook(storyboard, template)
        Look.MASCOTTE -> MascotLook(storyboard, template)
        Look.EDITORIAL -> EditorialLook(storyboard)
        Look.NEON -> ReelLook(storyboard, template, ReelStyle.NEON)
        Look.COLLAGE -> ReelLook(storyboard, template, ReelStyle.COLLAGE)
        Look.CINEMA -> ReelLook(storyboard, template, ReelStyle.CINEMA)
        Look.INFOGRAPHIE -> InfographicLook(storyboard, StyleKit.infographic(storyboard.seed))
        Look.EXPLICATIF -> ExplainerLook(storyboard, 0)
        Look.EXPLIC_BAS -> ExplainerLook(storyboard, 1)
        Look.EXPLIC_BANDEAU -> ExplainerLook(storyboard, 2)
        Look.JOURNAL -> StageLook(storyboard, StageLook.Style.JOURNAL)
        Look.FLASH -> StageLook(storyboard, StageLook.Style.FLASH)
        Look.TERMINAL -> StageLook(storyboard, StageLook.Style.TERMINAL)
        Look.BD -> StageLook(storyboard, StageLook.Style.BD)
        Look.MONTAGE -> MontageLook(storyboard)
        Look.SERENITE -> SerenityLook(storyboard)
        Look.ELAN -> ElanLook(storyboard)
        Look.STRATEGIE -> StrategyLook(storyboard)
        Look.LUMIERE -> SerenityLook(storyboard, SerenityLook.Theme.LUMIERE)
        Look.KIT -> MontageLook(storyboard, StyleKit.random(storyboard.seed, storyboard.mood))
    }
}

// ───────────────────────────── Outils communs aux formats ─────────────────────────────

/** Polyligne avec abscisse curviligne : position d'un stylo à une fraction du tracé. */
internal class Polyline(path: PathData) {
    private val pts: List<Pair<Float, Float>> = path.flatten().flatten()
    private val cum = FloatArray(pts.size)
    val length: Float

    init {
        var acc = 0f
        for (i in 1 until pts.size) { acc += hypot(pts[i].first - pts[i - 1].first, pts[i].second - pts[i - 1].second); cum[i] = acc }
        length = acc
    }

    fun pointAt(f: Float): Pair<Float, Float> {
        if (pts.isEmpty()) return 0f to 0f
        val target = clamp01(f) * length
        for (i in 1 until pts.size) if (cum[i] >= target) {
            val seg = cum[i] - cum[i - 1]
            val k = if (seg <= 0f) 0f else (target - cum[i - 1]) / seg
            return lerp(pts[i - 1].first, pts[i].first, k) to lerp(pts[i - 1].second, pts[i].second, k)
        }
        return pts.last()
    }
}

/** Texte avec mots mis en valeur (`*mot*`) : découpe une ligne en segments normaux / mis en valeur. */
internal object MarkedText {
    /** Pour chaque ligne du bloc (texte nu), les segments (texte, mis en valeur ?). */
    fun runs(block: TextBlock, marked: Marked): List<List<Pair<String, Boolean>>> {
        var idx = 0
        return block.lines.map { line ->
            val out = ArrayList<Pair<String, Boolean>>()
            val cur = StringBuilder()
            var curMark: Boolean? = null
            for (ch in line) {
                val m = if (ch == ' ') (curMark ?: false) else marked.isMarked(idx++)
                if (curMark != null && m != curMark) { out += cur.toString() to curMark; cur.clear() }
                curMark = m; cur.append(ch)
            }
            if (cur.isNotEmpty()) out += cur.toString() to (curMark ?: false)
            out
        }
    }

    /** Dessine une ligne segmentée ; renvoie la position x de fin. */
    fun drawLine(g: Gfx, runs: List<Pair<String, Boolean>>, x: Float, base: Float, font: FontSpec, color: Int, markColor: Int, markFont: FontSpec = font): Float {
        var cx = x
        for ((t, m) in runs) {
            val f = if (m) markFont else font
            g.drawText(t, cx, base, f, if (m) markColor else color)
            cx += g.measureText(t, f)
        }
        return cx
    }

    fun lineWidth(g: Gfx, runs: List<Pair<String, Boolean>>, font: FontSpec, markFont: FontSpec = font): Float =
        runs.sumOf { (t, m) -> g.measureText(t, if (m) markFont else font).toDouble() }.toFloat()
}

/** Cache de mises en page de texte (une par clé), pour ne pas recalculer à chaque image. */
internal class TextCache {
    private val map = HashMap<String, TextBlock>()
    fun fit(g: Gfx, key: String, text: String, font: FontSpec, maxWidth: Float, maxLines: Int, lh: Float = 1.18f): TextBlock =
        map.getOrPut(key) { TextLayout.fit(g, text, font, maxWidth, maxLines, lineHeightFactor = lh) }
}

/** Icône d'une scène : l'icône donnée, sinon une icône déduite de l'illustration. */
internal fun Scene.iconOrDefault(): IconId = icon ?: when (illustration) {
    IllustrationId.PHISHING_MAIL -> IconId.MAIL
    IllustrationId.SHIELD_CHECK -> IconId.SHIELD
    IllustrationId.WATER_DROP -> IconId.DROP
    IllustrationId.PLANT_GROWTH -> IconId.LEAF
    IllustrationId.CHART_GROWTH -> IconId.CHART
    IllustrationId.CHECK_CIRCLE -> IconId.CHECK
    IllustrationId.NETWORK_TOPOLOGY -> IconId.ROUTER
    IllustrationId.CODE_WINDOW -> IconId.CODE
    IllustrationId.OS_TERMINAL -> IconId.TERMINAL
    IllustrationId.NEURAL_NET -> IconId.BRAIN
    IllustrationId.HEART_BEAT -> IconId.HEART
    IllustrationId.SPEECH_BUBBLES -> IconId.CHAT
    IllustrationId.NONE -> IconId.BULB
}

/** Découpe « libellé : détail » utilisée par les étapes. */
internal fun splitLabel(item: String): Pair<String, String?> {
    val m = LABEL_SEP.find(item) ?: return item to null
    return item.substring(0, m.range.first).trim() to item.substring(m.range.last + 1).trim().ifEmpty { null }
}

/** « 30 minutes » → ("30", "minutes"). */
internal fun statParts(v: String): Pair<String, String> {
    val i = v.indexOfFirst { it == ' ' || it == ' ' }
    return if (i <= 0) v to "" else v.substring(0, i) to v.substring(i + 1).trim()
}

/** Réduit la police jusqu'à ce que le texte tienne dans `maxW`. */
internal fun fitSize(g: Gfx, text: String, font: FontSpec, maxW: Float): FontSpec {
    var f = font
    while (text.isNotEmpty() && g.measureText(text, f) > maxW && f.size > 20f) f = f.withSize(f.size * 0.93f)
    return f
}

internal fun Compare.badText() = badValue.joinToString("") { it.text }
internal fun Compare.goodText() = goodValue.joinToString("") { it.text }
