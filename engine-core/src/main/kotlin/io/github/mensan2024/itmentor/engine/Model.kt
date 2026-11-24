// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Données d'entrée du moteur. Tout est immuable et indépendant d'Android :
 * le même storyboard produit la même vidéo sur le téléphone et sur le banc d'essai JVM.
 */

/** Format de sortie. Le moteur dessine toujours dans un espace de conception 1080 × 1920,
 *  puis le met à l'échelle : 720 × 1280 donne exactement la même composition. */
data class VideoFormat(val width: Int = 1080, val height: Int = 1920, val fps: Int = 30) {
    init {
        require(width > 0 && height > 0) { "Dimensions invalides : ${width}x$height" }
        require(width % 2 == 0 && height % 2 == 0) { "H.264 4:2:0 exige des dimensions paires" }
        require(fps in 15..60) { "Fréquence hors limites : $fps" }
    }

    /** Facteur entre l'espace de conception (1080 de large) et la sortie réelle. */
    val scale: Float get() = width / DESIGN_WIDTH

    companion object {
        const val DESIGN_WIDTH = 1080f
        const val DESIGN_HEIGHT = 1920f
        val FULL_HD = VideoFormat(1080, 1920, 30)
        val HD = VideoFormat(720, 1280, 30)
    }
}

/** Les compositions graphiques disponibles. Chacune est une mise en page différente. */
enum class Layout { HERO, DEFINITION, STEPS, COMPARE, STAT, CHECKLIST }

enum class IconId { SHIELD, LOCK, MAIL, WARNING, CHECK, CROSS, KEY, EYE, USER, HOOK, SEARCH, CLOCK }

enum class IllustrationId { NONE, PHISHING_MAIL, SHIELD_CHECK, TIMER_RING }

enum class ShapeKind { RING, HEXAGON, DIAMOND }

/** Les 8 animations exigées. Les entrées et les sorties partagent le même vocabulaire. */
enum class AnimType {
    NONE,
    FADE_IN,
    FADE_OUT,
    SLIDE_LEFT,   // glissement horizontal (vers la gauche)
    SLIDE_RIGHT,  // glissement horizontal (vers la droite)
    SLIDE_UP,     // glissement vertical (vers le haut)
    SLIDE_DOWN,   // glissement vertical (vers le bas)
    ZOOM_IN,      // zoom avant
    ZOOM_OUT,     // zoom arrière
    TYPEWRITER,   // texte progressif
    MOVE_PATH,    // déplacement d'un élément graphique le long d'un chemin
}

data class AnimSpec(
    val type: AnimType,
    val durationMs: Int = 600,
    val delayMs: Int = 0,
    val easing: Easing = Easing.EXPO_OUT,
    /** Amplitude d'un glissement, en fraction de la largeur (horizontal) ou de la hauteur (vertical). */
    val distance: Float = 0.06f,
) {
    init {
        require(durationMs >= 0 && delayMs >= 0) { "Durée ou délai négatif" }
    }
}

enum class TransitionType { CUT, FADE, SLIDE, ZOOM }

data class Transition(val type: TransitionType, val durationMs: Int = 500) {
    init { require(durationMs >= 0) }
    companion object { val CUT = Transition(TransitionType.CUT, 0) }
}

/** Morceau de texte coloré (pour surligner une partie suspecte d'une adresse, par exemple). */
data class Span(val text: String, val highlight: Boolean = false)

data class Compare(
    val badLabel: String, val badValue: List<Span>,
    val goodLabel: String, val goodValue: List<Span>,
)

data class Scene(
    val id: String,
    val layout: Layout,
    val title: String,
    val body: String = "",
    val subtitle: String? = null,
    val kicker: String? = null,
    val durationMs: Int,
    /** Couleur de fond propre à la scène (ARGB). `null` = couleur du style. */
    val background: Int? = null,
    val icon: IconId? = null,
    val illustration: IllustrationId = IllustrationId.NONE,
    val shape: ShapeKind = ShapeKind.RING,
    val items: List<String> = emptyList(),
    val compare: Compare? = null,
    val statValue: String? = null,
    /** Phrase lue par la voix off (texte oral, distinct du texte affiché). `null` = scène muette. */
    val narration: String? = null,
    val enter: AnimSpec = AnimSpec(AnimType.SLIDE_UP),
    val exit: AnimSpec = AnimSpec(AnimType.FADE_OUT, durationMs = 350, easing = Easing.EASE_IN),
    /** Transition vers la scène suivante (ignorée pour la dernière scène). */
    val transitionOut: Transition = Transition(TransitionType.FADE, 500),
) {
    init {
        require(id.isNotBlank()) { "Une scène doit avoir un identifiant" }
        require(durationMs > 0) { "La scène $id doit avoir une durée positive" }
    }
}

data class Storyboard(
    val id: String,
    val title: String,
    val styleId: String,
    val scenes: List<Scene>,
    val footer: String = "IT_MENTOR",
    val hashtag: String? = null,
) {
    init {
        require(scenes.isNotEmpty()) { "Le storyboard est vide" }
        val dup = scenes.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(dup.isEmpty()) { "Identifiants de scène en double : $dup" }
    }
}
