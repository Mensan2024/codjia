// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Compréhension du contenu (étape 2 de la Partie 4).
 *
 * Avant de choisir un format, CODJIA établit un [ContentProfile] : famille de contenu, public, registre,
 * niveau, objectif, ton, émotions, notions à illustrer et type de conclusion. Tout se fait sur l'appareil,
 * sans réseau ni modèle de langage : ce sont des indices pondérés (vocabulaire, références, structure,
 * ton) combinés par des règles de désambiguïsation, et chaque décision garde ses indices ([ContentProfile.evidence])
 * pour être expliquée à l'utilisateur. Ce n'est pas une compréhension « humaine » : les textes ambigus ou
 * trop courts sont signalés ([ContentProfile.confidence]) plutôt que devinés.
 */
enum class ContentFamily(val label: String, val domain: Domain, val sensitive: Boolean = false) {
    MENTAL("Santé mentale et bien-être", Domain.SANTE, sensitive = true),
    BIBLE("Parole biblique et spiritualité", Domain.GENERAL, sensitive = true),
    PERSONAL("Développement personnel", Domain.GENERAL),
    MARKETING("Marketing et entrepreneuriat", Domain.MARKETING),
    NETWORK("Réseaux et systèmes", Domain.RESEAUX),
    DEV("Développement et programmation", Domain.FULLSTACK),
    CYBER("Cybersécurité", Domain.CYBER),
    AI("Intelligence artificielle", Domain.IA),
    HEALTH("Santé et hygiène de vie", Domain.SANTE),
    ENGLISH("Anglais", Domain.ANGLAIS),
    GENERAL("Général", Domain.GENERAL),
}

enum class Objective(val label: String) {
    ENSEIGNER("Enseigner pas à pas"), ALERTER("Alerter et protéger"), MOTIVER("Motiver à agir"),
    APAISER("Apaiser et accompagner"), CONVAINCRE("Convaincre"), MEDITER("Méditer"), INFORMER("Informer"),
}

enum class Tone(val label: String) {
    CALME("calme"), CONTEMPLATIF("contemplatif"), CHALEUREUX("chaleureux"), ENERGIQUE("énergique"),
    SERIEUX("sérieux"), TECHNIQUE("technique"), URGENT("urgent"), PEDAGOGIQUE("pédagogique"),
}

enum class Register(val label: String) { TU("tutoiement"), VOUS("vouvoiement"), NEUTRE("neutre") }
enum class Level(val label: String) { DEBUTANT("débutant"), INTERMEDIAIRE("intermédiaire"), EXPERT("expert") }

/** Comment le texte devrait se terminer (la conclusion de l'auteur reste, de toute façon, entière). */
enum class ConclusionKind(val label: String) {
    ACTION("un geste concret à faire"), REFLEXION("une pensée à emporter"), ENCOURAGEMENT("un encouragement"),
    RECAP("un récapitulatif"), APPEL("une invitation à agir"), RECUEILLEMENT("un temps de recueillement"),
}

data class ContentProfile(
    val family: ContentFamily,
    /** Familles proches, de la plus probable à la moins probable (sert aux recommandations). */
    val alternatives: List<ContentFamily>,
    val audience: String,
    val register: Register,
    val level: Level,
    val objective: Objective,
    val tone: Tone,
    val emotions: List<String>,
    /** Notions qui méritent une démonstration visuelle (les mots du texte, pas des inventions). */
    val showable: List<String>,
    val conclusion: ConclusionKind,
    /** 0 = aucun indice, 1 = sans ambiguïté. */
    val confidence: Float,
    /** Indices qui ont décidé de la famille, en clair. */
    val evidence: List<String>,
    /** Sujet sensible : pas de diagnostic, pas de promesse, ambiance sobre (voir [ArtDirector]). */
    val sensitive: Boolean,
    val wordCount: Int,
) {
    val domain: Domain get() = family.domain
    val ambiguous: Boolean get() = confidence < 0.3f
}

object ContentAnalyzer {
    private class Lex(val family: ContentFamily, entries: List<Pair<String, Int>>) {
        val patterns = entries.map { (k, w) -> Triple(k, Regex("(?<![a-z0-9])" + Regex.escape(k)), w) }
    }

    private val MENTAL = Lex(ContentFamily.MENTAL, listOf(
        "sante mentale" to 7, "anxiete" to 5, "anxieu" to 4, "angoisse" to 4, "depress" to 5, "burn-out" to 4, "burnout" to 4,
        "epuisement" to 3, "stress" to 3, "emotion" to 3, "estime de soi" to 5, "confiance en soi" to 2, "psycholog" to 3,
        "therap" to 3, "bien-etre" to 2, "equilibre emotionnel" to 6, "tristesse" to 3, "solitude" to 3, "ressent" to 2,
        "pensees negatives" to 5, "apais" to 3, "traumatis" to 3, "culpabilit" to 2, "bienveillan" to 2, "psychique" to 3,
        "mental" to 2, "souffrance" to 3, "tu n'es pas seul" to 4, "demander de l'aide" to 3, "ecoute" to 1, "relation" to 1,
        "respirer" to 1, "respiration" to 1, "paniqu" to 3, "inquiet" to 2, "fatigue" to 1, "sentiment" to 1,
    ))
    private val PERSONAL = Lex(ContentFamily.PERSONAL, listOf(
        "developpement personnel" to 7, "discipline" to 5, "motivation" to 4, "leadership" to 4, "productivite" to 4,
        "gestion du temps" to 5, "objectif" to 3, "habitude" to 3, "resilience" to 4, "reussite" to 3, "procrastin" to 4,
        "ambition" to 3, "routine" to 2, "perseveran" to 4, "mindset" to 4, "etat d'esprit" to 3, "priorite" to 2,
        "concentration" to 1, "excuse" to 2, "echec" to 1, "progres" to 2, "potentiel" to 3, "confiance en soi" to 2,
        "devenir meilleur" to 4, "sortir de ta zone de confort" to 5, "zone de confort" to 4, "chaque jour" to 1, "regularite" to 3,
    ))
    private val BIBLE = Lex(ContentFamily.BIBLE, listOf(
        "dieu" to 3, "seigneur" to 3, "eternel" to 2, "jesus" to 5, "christ" to 4, "evangile" to 4, "bible" to 5, "biblique" to 5,
        "verset" to 5, "psaume" to 5, "priere" to 3, "prier" to 3, "saint-esprit" to 5, "esprit saint" to 5, "amen" to 4,
        "peche" to 2, "eglise" to 2, "benir" to 2, "beni" to 2, "ecriture" to 2, "parole de dieu" to 6, "pasteur" to 2,
        "disciple" to 2, "apotre" to 3, "grace" to 1, "foi" to 1, "pardon" to 1, "royaume" to 1, "sainte" to 1,
    ))
    private val BOOKS = "genese|exode|levitique|nombres|deuteronome|josue|juges|ruth|samuel|rois|chroniques|esdras|nehemie|esther|job|psaumes?|" +
        "proverbes|ecclesiaste|cantique|esaie|jeremie|lamentations|ezechiel|daniel|osee|joel|amos|abdias|jonas|michee|nahum|habacuc|sophonie|" +
        "aggee|zacharie|malachie|matthieu|marc|luc|jean|actes|romains|corinthiens|galates|ephesiens|philippiens|colossiens|thessaloniciens|" +
        "timothee|tite|philemon|hebreux|jacques|pierre|jude|apocalypse"
    /** « Jean 3:16 », « 1 Corinthiens 13.4-7 », « Psaume 23 : 1 ». */
    private val VERSE = Regex("(?<![a-z0-9])(?:[123] ?)?(?:$BOOKS) \\d{1,3} ?[:.,] ?\\d{1,3}")

    private val MARKETING = Lex(ContentFamily.MARKETING, listOf(
        "marketing" to 4, "client" to 2, "vente" to 2, "vendre" to 2, "marque" to 2, "publicite" to 3, "reseaux sociaux" to 2,
        "abonne" to 1, "linkedin" to 1, "facebook" to 1, "tiktok" to 1, "instagram" to 1, "audience" to 2, "prospect" to 3,
        "branding" to 4, "seo" to 4, "conversion" to 3, "entrepreneur" to 3, "business" to 2, "chiffre d'affaires" to 4,
        "tunnel de vente" to 6, "prospection" to 4, "fidelisation" to 4, "strategie commerciale" to 5, "campagne" to 2,
        "startup" to 3, "rentabilite" to 3, "marge" to 2, "concurren" to 2, "offre" to 1, "persona" to 2, "commerce" to 2,
        "gestion d'entreprise" to 5, "slogan" to 3, "logo" to 1,
    ))

    private val SHOWABLE: Map<ContentFamily, List<String>> = mapOf(
        ContentFamily.NETWORK to listOf("vlan", "trunk", "etherchannel", "routage", "routeur", "commutateur", "paquet", "dhcp", "dns", "nat", "wifi", "pare-feu", "virtualisation", "cloud", "linux", "windows server"),
        ContentFamily.DEV to listOf("api", "base de donnees", "frontend", "backend", "requete", "git", "erreur", "bug", "html", "css", "javascript", "python", "json", "docker"),
        ContentFamily.CYBER to listOf("phishing", "hameconnage", "mot de passe", "mfa", "pare-feu", "force brute", "lien", "sms", "ransomware", "vpn", "certificat"),
        ContentFamily.AI to listOf("prompt", "donnees", "modele", "hallucination", "deepfake", "reseau de neurones"),
        ContentFamily.MARKETING to listOf("tunnel", "conversion", "publicite", "audience", "marque", "client", "seo", "prix", "offre", "reseaux sociaux"),
        ContentFamily.MENTAL to listOf("respiration", "pensees", "emotions", "sommeil", "relation", "stress"),
        ContentFamily.PERSONAL to listOf("objectif", "routine", "habitude", "planning", "priorite", "progres"),
        ContentFamily.BIBLE to listOf("verset", "psaume", "priere", "parabole"),
        ContentFamily.HEALTH to listOf("sommeil", "eau", "marche", "alimentation", "ecran"),
        ContentFamily.ENGLISH to listOf("verbe", "phrase", "prononciation", "present perfect"),
    )

    private fun hits(t: String, lex: Lex): Pair<Int, List<String>> {
        var total = 0
        val ev = ArrayList<String>()
        for ((k, re, w) in lex.patterns) {
            val n = re.findAll(t).count()
            if (n > 0) { total += minOf(n, 4) * w; ev += k }
        }
        return total to ev
    }

    private fun count(t: String, vararg words: String): Int = words.sumOf { w -> Regex("(?<![a-z0-9])" + Regex.escape(w)).findAll(t).count() }

    fun analyze(raw: String): ContentProfile {
        val clean = TextSanitizer.clean(raw)
        val t = " " + DomainDetector.normalize(clean) + " "
        val words = clean.split(Regex("\\s+")).count { w -> w.any { it.isLetterOrDigit() } }
        val legacy = DomainDetector.scores(clean).associate { it.domain to it.score }

        val evidence = HashMap<ContentFamily, List<String>>()
        val score = HashMap<ContentFamily, Int>()
        fun put(f: ContentFamily, s: Int, ev: List<String>) { score[f] = s; evidence[f] = ev }

        val (m, me) = hits(t, MENTAL); put(ContentFamily.MENTAL, m, me)
        val (p, pe) = hits(t, PERSONAL); put(ContentFamily.PERSONAL, p, pe)
        val verses = VERSE.findAll(t).map { it.value.trim() }.toList()
        val (b0, be) = hits(t, BIBLE)
        put(ContentFamily.BIBLE, b0 + minOf(verses.size, 3) * 8, verses.take(3) + be)
        val (mk, mke) = hits(t, MARKETING)
        put(ContentFamily.MARKETING, maxOf(mk, (legacy[Domain.MARKETING] ?: 0)), mke)
        // Les familles techniques reprennent les mots-clés du détecteur de domaines.
        val net = (legacy[Domain.RESEAUX] ?: 0) + (legacy[Domain.SYSTEMES] ?: 0)
        put(ContentFamily.NETWORK, net, emptyList())
        val codeCues = count(t, "api rest", "endpoint", "get /", "post /", "status 200", "requete http", "frontend", "backend", "function", "=>", "console.log")
        val backticks = clean.count { it == '`' } / 2
        put(ContentFamily.DEV, (legacy[Domain.FULLSTACK] ?: 0) + codeCues * 3 + minOf(backticks, 4) * 2, emptyList())
        put(ContentFamily.CYBER, legacy[Domain.CYBER] ?: 0, emptyList())
        put(ContentFamily.AI, legacy[Domain.IA] ?: 0, emptyList())
        put(ContentFamily.HEALTH, legacy[Domain.SANTE] ?: 0, emptyList())
        put(ContentFamily.ENGLISH, legacy[Domain.ANGLAIS] ?: 0, emptyList())

        // Désambiguïsation : le sens prime sur le nombre de mots.
        val s = score.toMutableMap()
        // 1. Une référence biblique ou un vocabulaire de foi net l'emporte (« foi », « grâce » seuls ne suffisent pas).
        val bible = s.getValue(ContentFamily.BIBLE)
        val strongestTech = listOf(ContentFamily.NETWORK, ContentFamily.DEV, ContentFamily.CYBER, ContentFamily.AI).maxOf { s.getValue(it) }
        if (bible >= 9 && bible * 2 >= strongestTech) s[ContentFamily.BIBLE] = bible + 20
        // 2. Santé mentale ou santé physique : le vocabulaire psychique décide.
        val mental = s.getValue(ContentFamily.MENTAL)
        if (mental >= 5 && mental * 10 >= s.getValue(ContentFamily.HEALTH) * 5) s[ContentFamily.MENTAL] = mental + s.getValue(ContentFamily.HEALTH) / 2 + 6
        // 3. Mental ou développement personnel : on regarde si le texte accompagne (émotions, « tu n'es pas seul »)
        //    ou pousse à agir (objectifs, discipline) ; « confiance en soi » apparaît dans les deux.
        val care = count(t, "emotion", "ressent", "souffr", "tristesse", "anxi", "apais", "bienveillan", "accepte", "ecoute", "tu n'es pas seul", "respire", "douceur")
        val drive = count(t, "objectif", "discipline", "action", "reussir", "reussite", "habitude", "chaque jour", "planifi", "priorite", "avance", "progres", "performance")
        if (s.getValue(ContentFamily.MENTAL) >= 4 && s.getValue(ContentFamily.PERSONAL) >= 4) {
            if (care >= drive) s[ContentFamily.PERSONAL] = s.getValue(ContentFamily.PERSONAL) / 2 else s[ContentFamily.MENTAL] = s.getValue(ContentFamily.MENTAL) / 2
        }
        // 3 bis. « habitude », « chaque jour » : dans un texte de santé physique, ce n'est pas du développement personnel.
        val strongPersonal = count(t, "discipline", "motivation", "leadership", "objectif", "developpement personnel", "productivite", "gestion du temps", "resilience", "ambition", "reussite")
        if (s.getValue(ContentFamily.HEALTH) >= 8 && strongPersonal == 0) s[ContentFamily.PERSONAL] = s.getValue(ContentFamily.PERSONAL) / 3
        // 4. « stress », « sommeil »… dans un texte technique ne font pas une santé mentale.
        if (strongestTech >= 10 && strongestTech > s.getValue(ContentFamily.MENTAL) * 2) s[ContentFamily.MENTAL] = s.getValue(ContentFamily.MENTAL) / 3
        // 5. Réseau ou développement : une API, du JSON, du code → développement, même si « réseau » apparaît.
        if (s.getValue(ContentFamily.DEV) >= 8 && codeCues >= 2 && s.getValue(ContentFamily.DEV) >= s.getValue(ContentFamily.NETWORK) / 2) s[ContentFamily.DEV] = s.getValue(ContentFamily.DEV) + 8
        // 6. Marketing : un contenu qui parle de clients et de ventes sans vocabulaire technique.
        // 7. L'anglais ne gagne que s'il y a vraiment de l'anglais (le détecteur compte « the » et « you »).
        val english = s.getValue(ContentFamily.ENGLISH)
        if (english < 6 || count(t, "anglais", "english", "grammaire", "vocabulaire", "present perfect") == 0) s[ContentFamily.ENGLISH] = english / 3

        val ranked = s.entries.sortedByDescending { it.value }
        val top = ranked.first()
        val second = ranked.getOrNull(1)
        val enough = top.value >= 5 && words >= 12
        val family = if (enough) top.key else ContentFamily.GENERAL
        val confidence = when {
            !enough -> if (top.value >= 3) 0.2f else 0.1f
            second == null || second.value == 0 -> 1f
            else -> ((top.value - second.value).toFloat() / top.value).coerceIn(0f, 1f).let { if (top.value >= 12) minOf(1f, it + 0.15f) else it }
        }
        val alternatives = ranked.filter { it.key != family && it.value >= 3 }.take(2).map { it.key }
        val ev = (evidence[family] ?: emptyList()).take(6).ifEmpty {
            DomainDetector.scores(clean).first().let { if (it.score > 0) listOf(it.domain.label.lowercase()) else emptyList() }
        }

        // ── public, registre, niveau ──
        val tu = count(t, "tu", "ton", "ta", "tes", "toi", "te")
        val vous = count(t, "vous", "votre", "vos")
        val register = when { tu >= 3 && tu > vous * 2 -> Register.TU; vous >= 3 && vous > tu * 2 -> Register.VOUS; else -> Register.NEUTRE }
        val level = when {
            count(t, "debutant", "premiers pas", "c'est quoi", "pour commencer", "simple") > 0 -> Level.DEBUTANT
            count(t, "avance", "expert", "optimis", "architecture", "protocole", "configuration", "latence") >= 2 -> Level.EXPERT
            else -> Level.INTERMEDIAIRE
        }
        val audience = when {
            count(t, "etudiant") > 0 -> "étudiants"
            count(t, "entrepreneur", "commercant", "pme") > 0 -> "entrepreneurs et commerçants"
            count(t, "parent") > 0 -> "parents"
            count(t, "developpeur", "dev ") > 0 -> "développeurs"
            count(t, "administrateur", "technicien", "ingenieur") > 0 -> "professionnels de l'informatique"
            count(t, "freres et soeurs", "fideles", "chretien", "croyant") > 0 -> "croyants"
            count(t, "jeune") > 0 -> "jeunes"
            else -> when (family) {
                ContentFamily.BIBLE -> "croyants et lecteurs de la Bible"
                ContentFamily.MENTAL -> "toute personne concernée par son équilibre émotionnel"
                ContentFamily.PERSONAL -> "personnes qui veulent progresser"
                ContentFamily.MARKETING -> "entrepreneurs et professionnels du marketing"
                ContentFamily.NETWORK, ContentFamily.DEV, ContentFamily.AI -> if (level == Level.DEBUTANT) "débutants en informatique" else "apprenants en informatique"
                ContentFamily.CYBER -> "grand public"
                else -> "grand public"
            }
        }

        // ── objectif, ton, émotions, conclusion ──
        val alert = count(t, "attention", "danger", "risque", "piratage", "arnaque", "fraude", "vol de", "menace", "ne clique", "jamais") + (if (clean.contains("⚠")) 2 else 0)
        val steps = count(t, "etape", "d'abord", "ensuite", "puis ", "premierement", "deuxiemement", "commence par") + clean.lines().count { Regex("^\\s*(\\d+[.)]|[-•*])\\s").containsMatchIn(it) }
        val motivate = count(t, "tu peux", "vous pouvez", "ose ", "ne lache", "persevere", "reussir", "decide", "passe a l'action", "des aujourd'hui", "commence maintenant")
        val soothe = count(t, "respire", "doucement", "tu n'es pas seul", "prends soin", "accepte", "ecoute-toi", "pas grave", "autorise-toi", "bienveillan")
        val persuade = count(t, "decouvre", "achete", "gratuit", "resultats", "augmente", "multiplie", "offre", "profite", "n'attends plus", "garantie")
        val objective = when {
            family == ContentFamily.BIBLE -> Objective.MEDITER
            family == ContentFamily.MENTAL -> if (soothe + care >= 2) Objective.APAISER else Objective.INFORMER
            family == ContentFamily.MARKETING && persuade >= 2 -> Objective.CONVAINCRE
            family == ContentFamily.PERSONAL -> Objective.MOTIVER
            family == ContentFamily.CYBER && alert >= 1 -> Objective.ALERTER
            alert >= 3 -> Objective.ALERTER
            steps >= 3 -> Objective.ENSEIGNER
            motivate >= 2 -> Objective.MOTIVER
            persuade >= 2 -> Objective.CONVAINCRE
            else -> Objective.INFORMER
        }
        val tone = when {
            family == ContentFamily.BIBLE -> Tone.CONTEMPLATIF
            family == ContentFamily.MENTAL -> Tone.CALME
            objective == Objective.ALERTER -> if (alert >= 5) Tone.URGENT else Tone.SERIEUX
            family == ContentFamily.PERSONAL || objective == Objective.CONVAINCRE -> Tone.ENERGIQUE
            family == ContentFamily.NETWORK || family == ContentFamily.DEV -> if (level == Level.DEBUTANT) Tone.PEDAGOGIQUE else Tone.TECHNIQUE
            family == ContentFamily.HEALTH -> Tone.CHALEUREUX
            else -> Tone.PEDAGOGIQUE
        }
        val emotions = when (family) {
            ContentFamily.BIBLE -> listOf("recueillement", "espérance", "paix")
            ContentFamily.MENTAL -> listOf("apaisement", "réconfort", "confiance")
            ContentFamily.PERSONAL -> listOf("élan", "détermination", "fierté")
            ContentFamily.MARKETING -> listOf("envie", "ambition", "clarté")
            ContentFamily.CYBER -> listOf("vigilance", "maîtrise")
            ContentFamily.NETWORK, ContentFamily.DEV, ContentFamily.AI -> listOf("curiosité", "maîtrise")
            ContentFamily.HEALTH -> listOf("bienveillance", "énergie")
            else -> listOf("curiosité")
        }
        val conclusion = when (objective) {
            Objective.MEDITER -> ConclusionKind.RECUEILLEMENT
            Objective.APAISER -> ConclusionKind.ENCOURAGEMENT
            Objective.MOTIVER -> ConclusionKind.ACTION
            Objective.CONVAINCRE -> ConclusionKind.APPEL
            Objective.ALERTER -> ConclusionKind.ACTION
            Objective.ENSEIGNER -> ConclusionKind.RECAP
            Objective.INFORMER -> ConclusionKind.REFLEXION
        }
        val showable = (SHOWABLE[family] ?: emptyList()).filter { Regex("(?<![a-z0-9])" + Regex.escape(it)).containsMatchIn(t) }.take(6)
        return ContentProfile(family, alternatives, audience, register, level, objective, tone, emotions, showable, conclusion,
            confidence, ev, family.sensitive, words)
    }
}
