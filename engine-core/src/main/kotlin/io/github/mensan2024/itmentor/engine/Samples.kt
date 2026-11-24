// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Storyboards de démonstration, un par grand domaine. Ils ne dépendent d'aucun template :
 * c'est `Templates.apply` qui les habille. Les mots entre *astérisques* sont mis en valeur.
 * Espaces insécables ( ) devant « : ? ! » et dans les guillemets, comme en typographie française.
 */
object Samples {
    private const val NB = ' '

    /** Cybersécurité : repérer un e-mail d'hameçonnage. */
    val CYBER = Storyboard(
        id = "demo-phishing", title = "Repère un e-mail piégé", templateId = Templates.CYBER_NEON.id,
        hashtag = "#securiteinformatiquepourtous",
        scenes = listOf(
            Scene(id = "s1", layout = Layout.HERO, durationMs = 5200, kicker = "ASTUCE CYBERSÉCURITÉ",
                title = "Repère un *e-mail piégé* en 5${NB}secondes",
                subtitle = "L’hameçonnage vise tout le monde, du stagiaire au directeur.",
                illustration = IllustrationId.PHISHING_MAIL, icon = IconId.MAIL),
            Scene(id = "s2", layout = Layout.DEFINITION, durationMs = 6200, kicker = "DÉFINITION", icon = IconId.WARNING, shape = ShapeKind.HEXAGON,
                title = "L’*hameçonnage*, c’est quoi${NB}?",
                body = "Un faux message qui imite ta banque, ton opérateur ou un collègue pour te faire cliquer, payer ou donner ton mot de passe.",
                subtitle = "Il arrive là où tu ne l’attends pas${NB}: e-mail, SMS, WhatsApp."),
            Scene(id = "s3", layout = Layout.STEPS, durationMs = 6400, icon = IconId.SEARCH,
                title = "Les *4 signaux* d’alerte",
                items = listOf(
                    "Expéditeur bizarre : une adresse qui imite le vrai nom",
                    "Urgence : «${NB}ton compte sera bloqué ce soir${NB}»",
                    "Lien douteux : il ne mène pas au site officiel",
                    "Demande inhabituelle : paiement, code ou pièce jointe",
                )),
            Scene(id = "s4", layout = Layout.COMPARE, durationMs = 6000, kicker = "EXERCICE",
                title = "*Vrai* ou *faux*${NB}?",
                compare = Compare("Lien piégé", listOf(Span("https://ma-banque"), Span("-verif-compte.co", true), Span("/login")),
                    "Vrai site", listOf(Span("https://www.ma-banque.bf"), Span("/connexion"))),
                subtitle = "Appuie longuement sur un lien pour voir la vraie adresse avant de cliquer."),
            Scene(id = "s5", layout = Layout.STAT, durationMs = 4800, statValue = "3${NB}s",
                title = "Le bon réflexe${NB}: *une pause*",
                body = "Trois secondes de recul avant chaque clic suffisent souvent à déjouer le piège."),
            Scene(id = "s6", layout = Layout.CHECKLIST, durationMs = 5600, illustration = IllustrationId.SHIELD_CHECK,
                title = "*À retenir*",
                items = listOf("Je vérifie l’expéditeur", "Je ne clique jamais dans l’urgence",
                    "Je passe par le site ou l’appli officiels", "Je signale le message et je le supprime"),
                subtitle = "Garde l’œil ouvert${NB}: même un message reçu d’un proche peut être piégé.",
                body = "Partage à quelqu’un qui en a besoin",
                exit = AnimSpec(AnimType.FADE_OUT, 600, easing = Easing.EASE_IN)),
        ),
    )

    /** Santé : le lavage des mains (durée d'au moins 20 s recommandée par les CDC). */
    val SANTE = Storyboard(
        id = "demo-mains", title = "Bien se laver les mains", templateId = Templates.MEDICAL_CLEAN.id,
        hashtag = "#prevention",
        scenes = listOf(
            Scene(id = "s1", layout = Layout.HERO, durationMs = 5200, kicker = "ASTUCE SANTÉ",
                title = "Des *mains propres*, un geste qui protège",
                subtitle = "Le lavage des mains limite la transmission de nombreux microbes.",
                illustration = IllustrationId.WATER_DROP),
            Scene(id = "s2", layout = Layout.DEFINITION, durationMs = 6000, kicker = "POURQUOI${NB}?", icon = IconId.DROP, shape = ShapeKind.RING,
                title = "Nos mains *transportent* les microbes",
                body = "Poignées, téléphones, billets : nous touchons notre visage sans y penser, et les microbes passent des mains à la bouche, au nez et aux yeux.",
                subtitle = "Le savon et le frottement les décollent de la peau."),
            Scene(id = "s3", layout = Layout.STEPS, durationMs = 6400, icon = IconId.HEART,
                title = "Les *4 gestes* clés",
                items = listOf("Mouiller : eau propre sur les deux mains",
                    "Savonner : paumes, dos des mains, entre les doigts",
                    "Frotter : au moins 20 secondes, ongles compris",
                    "Rincer et sécher : serviette propre ou à l’air libre")),
            Scene(id = "s4", layout = Layout.COMPARE, durationMs = 5800, kicker = "LE BON GESTE",
                title = "Rincer *ne suffit pas*",
                compare = Compare("Insuffisant", listOf(Span("Un passage rapide sous l’eau", true)),
                    "Efficace", listOf(Span("Savon + 20 secondes de frottement"))),
                subtitle = "Pas d’eau ni de savon${NB}? Un gel hydroalcoolique dépanne."),
            Scene(id = "s5", layout = Layout.STAT, durationMs = 4800, statValue = "20${NB}s",
                title = "Le temps *minimum*", body = "Environ deux fois « Joyeux anniversaire » chanté en entier."),
            Scene(id = "s6", layout = Layout.CHECKLIST, durationMs = 5600, illustration = IllustrationId.CHECK_CIRCLE,
                title = "*Quand* se laver les mains${NB}?",
                items = listOf("Avant de cuisiner et de manger", "Après les toilettes",
                    "Après avoir toussé ou éternué", "En rentrant à la maison"),
                body = "Partage ce rappel autour de toi",
                exit = AnimSpec(AnimType.FADE_OUT, 600, easing = Easing.EASE_IN)),
        ),
    )

    /** Agriculture : le compost maison. */
    val AGRO = Storyboard(
        id = "demo-compost", title = "Le compost maison", templateId = Templates.GREEN_NATURE.id,
        hashtag = "#agriculturedurable",
        scenes = listOf(
            Scene(id = "s1", layout = Layout.HERO, durationMs = 5200, kicker = "ASTUCE AGRICULTURE",
                title = "Transforme tes *déchets* en engrais",
                subtitle = "Le compost nourrit le sol et réduit les déchets du foyer.",
                illustration = IllustrationId.PLANT_GROWTH),
            Scene(id = "s2", layout = Layout.DEFINITION, durationMs = 6000, kicker = "DÉFINITION", icon = IconId.LEAF, shape = ShapeKind.RING,
                title = "Le *compost*, c’est quoi${NB}?",
                body = "Un engrais naturel obtenu en laissant se décomposer les restes de cuisine et les déchets verts, grâce à l’air, à l’eau et aux micro-organismes."),
            Scene(id = "s3", layout = Layout.STEPS, durationMs = 6400, icon = IconId.SUN,
                title = "*4 étapes* pour réussir",
                items = listOf("Alterner : déchets verts et déchets secs (feuilles, paille)",
                    "Humidifier : comme une éponge bien essorée",
                    "Aérer : retourner le tas régulièrement",
                    "Patienter : quelques mois, jusqu’à une odeur de sous-bois")),
            Scene(id = "s4", layout = Layout.COMPARE, durationMs = 5800, kicker = "TRI",
                title = "Oui ou *non*${NB}?",
                compare = Compare("À éviter", listOf(Span("Viande, huile, plastique", true)),
                    "Bienvenus", listOf(Span("Épluchures, feuilles, marc de café"))),
                subtitle = "Un tas équilibré ne sent pas mauvais."),
            Scene(id = "s5", layout = Layout.STAT, durationMs = 4800, statValue = "0${NB}F",
                title = "Un engrais *gratuit*", body = "Fabriqué chez toi, avec ce que tu jetais déjà."),
            Scene(id = "s6", layout = Layout.CHECKLIST, durationMs = 5600, illustration = IllustrationId.CHECK_CIRCLE,
                title = "*À retenir*",
                items = listOf("Équilibrer vert et sec", "Garder humide, sans excès", "Aérer souvent", "Utiliser quand c’est mûr"),
                body = "Partage à un jardinier",
                exit = AnimSpec(AnimType.FADE_OUT, 600, easing = Easing.EASE_IN)),
        ),
    )

    /** Marketing : une publication qui attire. */
    val MARKETING = Storyboard(
        id = "demo-publication", title = "Une publication qui attire", templateId = Templates.MARKETING_IMPACT.id,
        hashtag = "#entrepreneuriat",
        scenes = listOf(
            Scene(id = "s1", layout = Layout.HERO, durationMs = 5200, kicker = "ASTUCE MARKETING",
                title = "Une publication qui *attire*", subtitle = "Quatre règles simples pour être lu, retenu et partagé.",
                illustration = IllustrationId.CHART_GROWTH),
            Scene(id = "s2", layout = Layout.DEFINITION, durationMs = 6000, kicker = "LE PRINCIPE", icon = IconId.MEGAPHONE, shape = ShapeKind.DIAMOND,
                title = "Capter, *apporter*, faire agir",
                body = "Une bonne publication capte l’attention dès la première ligne, apporte une vraie valeur, puis invite clairement à agir."),
            Scene(id = "s3", layout = Layout.STEPS, durationMs = 6400, icon = IconId.TARGET,
                title = "Les *4 règles*",
                items = listOf("Accroche : une question ou un chiffre dès la 1re ligne",
                    "Valeur : une astuce concrète, pas une publicité",
                    "Appel à l’action : commenter, partager, s’abonner",
                    "Régularité : publier au même rythme chaque semaine")),
            Scene(id = "s4", layout = Layout.COMPARE, durationMs = 5800, kicker = "AVANT / APRÈS",
                title = "Sois *précis*",
                compare = Compare("Trop vague", listOf(Span("« Découvrez nos produits${NB}! »", true)),
                    "Qui attire", listOf(Span("« 3 erreurs qui font fuir vos clients »"))),
                subtitle = "Une promesse précise donne envie de lire la suite."),
            Scene(id = "s5", layout = Layout.STAT, durationMs = 4800, statValue = "1",
                title = "*Une seule* idée par post", body = "Un message clair se retient mieux qu’une liste de promesses."),
            Scene(id = "s6", layout = Layout.CHECKLIST, durationMs = 5600, illustration = IllustrationId.CHECK_CIRCLE,
                title = "*Checklist* avant de publier",
                items = listOf("Une accroche forte", "Une valeur concrète", "Un appel à l’action", "Un visuel lisible"),
                body = "Enregistre cette checklist",
                exit = AnimSpec(AnimType.FADE_OUT, 600, easing = Easing.EASE_IN)),
        ),
    )

    val ALL = listOf(CYBER, SANTE, AGRO, MARKETING)

    /** Contenu de démonstration le plus naturel pour un template (captures et vignettes). */
    fun forTemplate(t: Template): Storyboard = when (t.id) {
        Templates.CYBER_NEON.id, Templates.BOLD_TYPOGRAPHY.id -> CYBER
        Templates.MEDICAL_CLEAN.id, Templates.MODERN_EDUCATION.id -> SANTE
        Templates.GREEN_NATURE.id, Templates.CINEMATIC_STORY.id -> AGRO
        Templates.CODE_STUDIO.id, Templates.NETWORK_BLUEPRINT.id -> CYBER
        else -> MARKETING
    }
}
