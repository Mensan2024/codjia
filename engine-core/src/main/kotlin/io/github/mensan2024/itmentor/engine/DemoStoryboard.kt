// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Storyboard de démonstration : 6 scènes, 6 compositions, 3 types de transitions, environ 32 s.
 * Les espaces insécables ( ) respectent la typographie française devant « : ? ! » et dans les guillemets.
 */
object DemoStoryboard {
    private const val NB = ' '

    fun phishing(styleId: String = Styles.CYBER.id): Storyboard {
        // Fonds propres à chaque scène pour le style Cyber ; les autres styles gardent leur propre teinte.
        fun bg(hex: Long): Int? = if (styleId == Styles.CYBER.id) rgb(hex) else null
        return Storyboard(
        id = "demo-phishing",
        title = "Repère un e-mail piégé",
        styleId = styleId,
        footer = "IT_MENTOR",
        hashtag = "#securiteinformatiquepourtous",
        scenes = listOf(
            Scene(
                id = "s1-couverture", narration = "Astuce cybersécurité : repère un e-mail piégé en cinq secondes.", layout = Layout.HERO, durationMs = 5500,
                kicker = "ASTUCE CYBERSÉCURITÉ",
                title = "Repère un e-mail piégé en 5${NB}secondes",
                subtitle = "L’hameçonnage vise tout le monde, du stagiaire au directeur.",
                illustration = IllustrationId.PHISHING_MAIL, icon = IconId.MAIL,
                enter = AnimSpec(AnimType.SLIDE_UP, 750),
                transitionOut = Transition(TransitionType.FADE, 500),
            ),
            Scene(
                id = "s2-definition", narration = "L’hameçonnage, c’est un faux message qui imite ta banque ou un collègue, pour voler ton argent ou ton mot de passe.", layout = Layout.DEFINITION, durationMs = 6000,
                background = bg(0x0D1526),
                kicker = "DÉFINITION", icon = IconId.WARNING, shape = ShapeKind.HEXAGON,
                title = "L’hameçonnage, c’est quoi${NB}?",
                body = "Un faux message qui imite ta banque, ton opérateur ou un collègue pour te faire cliquer, payer ou donner ton mot de passe.",
                subtitle = "Il arrive là où tu ne l’attends pas${NB}: e-mail, SMS, WhatsApp.",
                enter = AnimSpec(AnimType.SLIDE_UP, 700),
                transitionOut = Transition(TransitionType.SLIDE, 600),
            ),
            Scene(
                id = "s3-signaux", narration = "Quatre signaux d’alerte : un expéditeur bizarre, l’urgence, un lien douteux, une demande inhabituelle.", layout = Layout.STEPS, durationMs = 6500,
                background = bg(0x0A1424),
                icon = IconId.SEARCH,
                title = "Les 4 signaux d’alerte",
                items = listOf(
                    "Expéditeur bizarre : une adresse qui imite le vrai nom",
                    "Urgence : «${NB}ton compte sera bloqué ce soir${NB}»",
                    "Lien douteux : il ne mène pas au site officiel",
                    "Demande inhabituelle : paiement, code ou pièce jointe",
                ),
                enter = AnimSpec(AnimType.SLIDE_DOWN, 700, distance = 0.03f),
                transitionOut = Transition(TransitionType.ZOOM, 600),
            ),
            Scene(
                id = "s4-comparaison", narration = "Vrai ou faux ? Appuie longuement sur un lien pour voir sa vraie adresse.", layout = Layout.COMPARE, durationMs = 6000,
                background = bg(0x0C1322),
                kicker = "EXERCICE",
                title = "Vrai ou faux${NB}?",
                compare = Compare(
                    badLabel = "Lien piégé",
                    badValue = listOf(Span("https://ma-banque"), Span("-verif-compte.co", highlight = true), Span("/login")),
                    goodLabel = "Vrai site",
                    goodValue = listOf(Span("https://www.ma-banque.bf"), Span("/connexion")),
                ),
                subtitle = "Appuie longuement sur un lien pour voir la vraie adresse avant de cliquer.",
                enter = AnimSpec(AnimType.SLIDE_UP, 700),
                transitionOut = Transition(TransitionType.SLIDE, 500),
            ),
            Scene(
                id = "s5-reflexe", narration = "Le bon réflexe : une pause de trois secondes avant chaque clic.", layout = Layout.STAT, durationMs = 5000,
                background = bg(0x0B1220),
                statValue = "3${NB}s",
                title = "Le bon réflexe${NB}: une pause",
                body = "Trois secondes de recul avant chaque clic suffisent souvent à déjouer le piège.",
                enter = AnimSpec(AnimType.ZOOM_IN, 800),
                transitionOut = Transition(TransitionType.FADE, 500),
            ),
            Scene(
                id = "s6-a-retenir", narration = "À retenir : vérifie l’expéditeur, ne clique jamais dans l’urgence, et partage cette astuce.", layout = Layout.CHECKLIST, durationMs = 5500,
                background = bg(0x0E1A2B),
                illustration = IllustrationId.SHIELD_CHECK,
                title = "À retenir",
                items = listOf(
                    "Je vérifie l’expéditeur",
                    "Je ne clique jamais dans l’urgence",
                    "Je passe par le site ou l’appli officiels",
                    "Je signale le message et je le supprime",
                ),
                subtitle = "Garde l’œil ouvert${NB}: même un message reçu d’un proche peut être piégé.",
                body = "Partage à quelqu’un qui en a besoin",
                enter = AnimSpec(AnimType.SLIDE_UP, 700),
                exit = AnimSpec(AnimType.FADE_OUT, 600, easing = Easing.EASE_IN),
            ),
        ),
    )
    }
}
