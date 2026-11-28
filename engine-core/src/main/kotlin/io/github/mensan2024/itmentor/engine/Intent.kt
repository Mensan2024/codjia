// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Partie 6.1 — Intention visuelle d'une scène.
 *
 * Le planificateur découpe le texte de l'auteur en scènes (texte intact, même ordre). Cette couche répond à une autre
 * question : « que faut-il MONTRER pour que ce passage soit compris ? ». Pour chaque scène elle établit, hors ligne et
 * sans modèle de langage, une [SceneIntent] : le domaine, le concept expliqué, les acteurs, les objets, l'action, le
 * contexte, la relation entre les éléments, l'objectif pédagogique et le rôle de la scène dans le récit.
 *
 * Méthode : lexiques pondérés sur le texte sans accents (comme [ContentAnalyzer]), combinés à la disposition de la
 * scène et à sa position dans la vidéo. Chaque décision garde ses indices ([SceneIntent.evidence]) pour être expliquée,
 * et une confiance : une scène que le moteur ne comprend pas reste sans concept ([Concept.NONE]) au lieu d'être devinée.
 * Rien n'est inventé : les mots affichés restent ceux de l'auteur ; l'intention ne sert qu'à choisir l'image.
 */
enum class Concept(val label: String) {
    PHISHING("Hameçonnage"), BRUTE_FORCE("Force brute"), MALWARE("Logiciel malveillant ou attaque technique"),
    FIREWALL("Pare-feu"), TOPOLOGY("Topologie réseau"), SEGMENTATION("Segmentation (VLAN)"), CLIENT_SERVER("Client et serveur"),
    GRATITUDE("Gratitude"), STRESS("Stress et apaisement"), FUNNEL("Tunnel de vente"),
    METHOD("Méthode ou enchaînement d'étapes"), WATER("Accès à l'eau potable"),
    AID("Aide humanitaire et situations humaines"), NETFLOW("Circulation des données dans un réseau"), NONE("Sans concept visuel net"),
}

/** Place de la scène dans le récit pédagogique. */
enum class Role(val label: String) {
    INTRO("ouverture"), DEFINE("définition"), RECOGNIZE("reconnaître les signes"), MECHANISM("mécanisme"),
    PROCEDURE("procédure"), ADVICE("conseil, parade"), CONTRAST("contraste"), MEASURE("mesure"), RECAP("à retenir"),
}

/** Qui agit ou subit dans la scène (cités par le texte, ou impliqués par le concept : [SceneIntent.impliedActors]). */
enum class Actor(val label: String) {
    ATTAQUANT("attaquant"), UTILISATEUR("utilisateur"), CLIENT_APP("client (application)"), SERVEUR("serveur"), ROUTEUR("routeur"),
    SWITCH("commutateur"), PARE_FEU("pare-feu"), BASE_DE_DONNEES("base de données"), PROSPECT("prospect"),
    ENTREPRENEUR("entrepreneur"), PROCHE("personne de confiance"), PROFESSIONNEL("professionnel de santé"), BANQUE("banque"), COMMUNAUTE("communauté"),
}

/** Ce que le texte manipule. */
enum class Thing(val label: String) {
    MESSAGE("message"), LIEN("lien"), MOT_DE_PASSE("mot de passe"), CODE("code à usage unique"), PAQUET("paquet"), REGLE("règle"),
    REQUETE("requête"), REPONSE("réponse"), STATUT("code de statut"), DONNEES("données"), JOURNAL("carnet"), RESPIRATION("respiration"),
    MARCHE("marche"), SOMMEIL("sommeil"), OFFRE("offre"), JOURNAUX_SYSTEME("journaux d'événements"), VLAN("VLAN"),
    ORDINATEUR("ordinateur"), COMPTE("compte"), ETAPES("étapes"), EAU("eau"), SOIR("soirée, journée"), SMS("SMS"), EMAIL("courriel"), OBJET_3D("objet physique à montrer en volume"), ATTAQUE("attaque"), MISSION("mission, objectif"), WORKFLOW("flux de travail"),
}

/** Ce qui se passe. */
enum class Act(val label: String) {
    ENVOYER("envoyer"), CLIQUER("cliquer"), VERIFIER("vérifier"), SAISIR("saisir"), DEVINER("essayer en boucle"),
    LIMITER("limiter les tentatives"), BLOQUER("bloquer"), AUTORISER("autoriser"), FILTRER("filtrer"), RELIER("relier"),
    DEMANDER("demander"), REPONDRE("répondre"), ACTIVER_MFA("activer la double authentification"), SURVEILLER("surveiller"),
    CHANGER("changer"), SIGNALER("signaler"), NAVIGUER_DIRECT("passer par le site officiel"), RESPIRER("respirer"),
    REMERCIER("remercier"), ECRIRE("écrire"), PARTAGER("partager, parler"), MARCHER("bouger"), DORMIR("dormir"),
    ATTIRER("attirer"), CONVERTIR("convertir"), FIDELISER("fidéliser"), MESURER("mesurer"), CHIFFRER("chiffrer"), ENCHAINER("enchaîner des étapes"), PUISER("puiser l'eau"),
}

enum class Context(val label: String) { COMMUNAUTE("communauté"), INTERNET("Internet"), ENTREPRISE("entreprise"), PERSONNEL("vie personnelle"), INTIME("intériorité"), COMMERCIAL("commerce"), TECHNIQUE("technique") }

/** Nature du lien entre les éléments à montrer. */
enum class Relation(val label: String) {
    ATTAQUE_CIBLE("un attaquant vise une cible"), FILTRE("un filtre trie ce qui passe"), CONNEXION("des éléments reliés"),
    DEMANDE_REPONSE("une demande et sa réponse"), TRANSFORMATION("un état qui devient un autre"), PROGRESSION("des étapes qui s'enchaînent"),
    CONTRASTE("deux situations opposées"), SOUTIEN("une présence qui soutient"), AUCUNE("aucune relation nette"),
}

/** Ce que le spectateur doit pouvoir faire après la scène. */
enum class Goal(val label: String) {
    IDENTIFIER_MENACE("identifier la menace"), COMPRENDRE_MECANISME("comprendre le mécanisme"), APPRENDRE_REFLEXE("apprendre le bon réflexe"),
    VOIR_ARCHITECTURE("voir l'architecture"), SUIVRE_PROCEDURE("suivre la procédure"), APAISER("s'apaiser"), MOTIVER("passer à l'action"),
    CONVAINCRE("être convaincu"), MEMORISER("retenir l'essentiel"),
}

data class SceneIntent(
    val domain: Domain,
    val family: ContentFamily,
    val concept: Concept,
    /** Concepts voisins présents dans la scène (ordre décroissant). */
    val secondary: List<Concept>,
    val role: Role,
    val actors: Set<Actor>,
    /** Acteurs que le concept suppose sans que le texte les nomme (l'attaquant d'un hameçonnage). */
    val impliedActors: Set<Actor>,
    val things: Set<Thing>,
    val acts: Set<Act>,
    val action: Act?,
    val context: Context,
    val relation: Relation,
    val goal: Goal,
    /** Nombres et motifs tirés du texte de l'auteur (« VLAN 10 », « GET /api/users »…), pour que l'image reprenne ses mots. */
    val params: Map<String, String>,
    /** Motif visuel de chaque élément de liste, dans l'ordre du texte (« password », « mfa », « breath »…) ; vide si inconnu. */
    val itemMotifs: List<String>,
    /** 0 = aucun indice, 1 = sans ambiguïté. */
    val confidence: Float,
    val evidence: List<String>,
    /** Sujets précis cités par la scène (« dns », « camp », « inbox »…) : ils départagent les plans d'un même domaine. */
    val topics: Set<String> = emptySet(),
) {
    /** Indices comparables à ceux d'une représentation. */
    val cues: Set<String> by lazy { (acts.map { "act:" + it.name } + things.map { "thing:" + it.name } + (actors + impliedActors).map { "actor:" + it.name } + topics.map { "topic:$it" }).toSet() }
}

object IntentAnalyzer {
    private class Lex(val concept: Concept, entries: List<Pair<String, Int>>) {
        val patterns = entries.map { (k, w) -> Triple(k, Regex("(?<![a-z0-9])$k"), w) }
    }

    // Les expressions sont des regex sur le texte sans accents et en minuscules.
    private val LEX = listOf(
        Lex(Concept.PHISHING, listOf("phishing" to 7, "hameconn" to 7, "sms piege" to 7, "smishing" to 7, "faux (?:mail|message|site|sms)" to 5, "courriel" to 2,
            "e-?mail" to 1, "sms" to 3, "lien" to 2, "cliqu" to 2, "expediteur" to 3, "usurp" to 3, "imite" to 3, "arnaque" to 2, "piege" to 3,
            "33700" to 3, "tromp" to 2, "informations? confidentielles?" to 2, "voler|vol de" to 1, "urgen" to 2, "site officiel" to 3, "application officielle" to 3, "piece jointe" to 2, "qr code" to 2, "pression" to 1)),
        Lex(Concept.BRUTE_FORCE, listOf("force brute" to 9, "combinaison" to 3, "tentative" to 3, "ssh" to 2, "fail2ban" to 6, "failed password" to 5,
            "echecs? d'authentification" to 5, "dictionnaire" to 3, "credential stuffing" to 5, "limitation des tentatives" to 5, "verrouill" to 2,
            "auth\\.log" to 4, "mots? de passe" to 1, "essayer de nombreuses" to 5, "essaie" to 1)),
        Lex(Concept.MALWARE, listOf("ransomware" to 8, "rancongiciel" to 8, "rancon" to 5, "malware" to 7, "logiciel malveillant" to 7, "virus" to 5, "trojan" to 6,
            "cheval de troie" to 6, "spyware" to 6, "ddos" to 7, "injection sql" to 7, "xss" to 6, "man-in-the-middle" to 7, "mitm" to 6, "exploit" to 3,
            "faille" to 3, "vulnerab" to 3, "intrusion" to 3, "porte derobee|backdoor" to 5, "mise a jour" to 1, "patch" to 2)),
        Lex(Concept.FIREWALL, listOf("pare-feu" to 8, "firewall" to 8, "filtr" to 3, "regles?" to 2, "acl" to 4, "trafic" to 2, "entrant" to 2, "sortant" to 2, "ufw" to 5,
            "iptables" to 5, "dmz" to 4, "bloque" to 1, "autorise" to 1, "ports?\\b" to 1)),
        Lex(Concept.TOPOLOGY, listOf("topologie" to 8, "en etoile" to 5, "etoile" to 3, "maille" to 4, "architecture reseau" to 6, "reseau local" to 3, "cablage" to 3,
            "switch" to 2, "commutateur" to 2, "routeur" to 2, "postes? de travail" to 2, "ordinateurs? du reseau" to 3, "reseau d'entreprise" to 3, "passerelle" to 1)),
        Lex(Concept.SEGMENTATION, listOf("vlan" to 7, "trunk" to 6, "domaine de diffusion" to 6, "inter-?vlan" to 6, "802\\.1q" to 5, "segment" to 3,
            "sous-reseau" to 3, "subnet" to 3, "diffusion" to 3, "broadcast" to 3)),
        Lex(Concept.CLIENT_SERVER, listOf("client-serveur" to 9, "requete" to 4, "endpoint" to 5, "\\bapi\\b" to 3, "frontend" to 4, "backend" to 4, "serveur" to 2,
            "reponse" to 3, "http" to 3, "(?:get|post|put|delete) /" to 5, "json" to 3, "navigateur" to 2, "base de donnees" to 2, "code de statut" to 4, "fetch" to 3)),
        Lex(Concept.GRATITUDE, listOf("gratitude" to 9, "reconnaissan" to 6, "merci" to 5, "remerci" to 6, "apprecier" to 3, "journal de gratitude" to 9, "carnet" to 2,
            "benedictions?" to 2, "positi[fv]" to 1, "cadeau" to 1, "chance" to 1,
            "choses? positives?" to 4, "trois choses" to 2, "experiences? positives?" to 3, "chaque soir" to 2)),
        Lex(Concept.STRESS, listOf("stress" to 6, "anxi" to 5, "respire" to 4, "respiration" to 4, "inspire" to 3, "expire" to 3, "calme" to 2, "apais" to 3, "emotion" to 2,
            "tension" to 1, "burn-?out" to 5, "epuis" to 3, "detente" to 2, "relax" to 3, "charge mentale" to 5, "tu n'es pas seul" to 4, "souffrance" to 2, "pensees" to 1)),
        Lex(Concept.FUNNEL, listOf("tunnel de vente" to 9, "tunnel" to 4, "conversion" to 4, "convertis|convertir" to 3, "prospect" to 3, "audience" to 2, "capte" to 2,
            "fidelis" to 3, "attire" to 2, "taux de conversion" to 6, "lead" to 3, "publicite" to 1,
            "clients?" to 2, "clientele" to 3, "produits?" to 2, "boutique|magasin|stand\\b" to 3, "parcours (?:client|d'achat)" to 6, "panier" to 3, "achat" to 2, "vente" to 2, "marque" to 1,
            "commande" to 2, "paiement" to 2, "avis" to 1, "reseaux sociaux" to 2, "visibilite" to 2, "promotion" to 2, "recommand" to 1)),
        Lex(Concept.METHOD, listOf("workflow" to 9, "methode" to 5, "processus" to 4, "enchainement" to 6, "etapes?" to 3, "mission" to 3, "role" to 2, "contexte" to 2,
            "tache" to 2, "format attendu" to 4, "pas a pas" to 3, "en \\d points?" to 4, "procedure" to 2, "pipeline" to 5)),
        Lex(Concept.AID, listOf("humanitaire" to 5, "deplace" to 6, "refugie" to 6, "\\bcamps?\\b" to 5, "aide alimentaire" to 7, "distribution" to 2, "\\bong\\b" to 4, "benevole" to 4,
            "crise" to 2, "urgence" to 2, "solidarite" to 4, "abri" to 3, "tentes?" to 3, "kit" to 1, "vaccin" to 3, "famille" to 1, "orphelin" to 4, "sinistr" to 4, "inondation" to 4,
            "secours" to 4, "vulnerable" to 3, "dignite" to 3, "donateur" to 3, "dons?\\b" to 2, "mobilis" to 2, "conflit" to 3, "famine|malnutrition" to 5, "enfants?" to 1, "ecole" to 1, "communaute" to 1, "terrain" to 1)),
        Lex(Concept.NETFLOW, listOf("\\bdns\\b" to 6, "dhcp" to 6, "\\bnat\\b" to 5, "wi-?fi|wifi" to 5, "\\bvpn\\b" to 5, "adresse ip" to 5, "passerelle" to 3, "\\bip\\b" to 2, "\\blan\\b" to 3, "\\bwan\\b" to 3,
            "flux" to 3, "paquets?" to 2, "protocole" to 3, "ethernet" to 3, "modele osi|couche" to 2, "bande passante" to 3, "latence" to 3, "tcp" to 3, "udp" to 3, "access point|point d'acces" to 4,
            "reseau d'entreprise|reseau informatique|reseaux informatiques" to 5, "fournisseur d'acces" to 3, "internet" to 1, "serveur" to 1, "postes?" to 1, "imprimante" to 2, "ping" to 3, "cable" to 2)),
        Lex(Concept.WATER, listOf("eau potable" to 9, "points? d'eau" to 7, "puits" to 6, "forage" to 6, "pompe" to 3, "\\beau\\b" to 2, "assainissement" to 5, "communaute" to 2,
            "village" to 3, "corvee d'eau" to 6, "maladies? hydriques?" to 6, "hygiene" to 2, "humanitaire" to 3)),
    )


    /** Sujets précis (sur le texte de la scène sans accents) : ils ne portent aucun concept à eux seuls, ils orientent le choix du plan. */
    private val TOPICS = listOf(
        "inbox" to Regex("courriel|e-?mail|\\bmail\\b|boite de reception|message"),
        "urgency" to Regex("urgen|24 ?h|immediat|suspendu|expire|sous \\d+|menace|pression|vite"),
        "sender" to Regex("expediteur|adresse|domaine|usurp|imite|ressemble"),
        "link" to Regex("\\blien|url|clique"),
        "chain" to Regex("se deroule|etape par etape|marque connue|fausse page de connexion|revend tes identifiants"),
        "damage" to Regex("consequence|virements? |achats? frauduleux|compte est vole|piege(?:r)? tes contacts|reseau de ton entreprise"),
        "login" to Regex("mots? de passe|identifiant|connexion|saisi|formulaire|code"),
        "callback" to Regex("appel|telephon|contacte|numero|banque|officiel"),
        "report" to Regex("signal|supprim|transmet|alerte|previens|declare"),
        "attachment" to Regex("piece jointe|fichier|document|telecharg"),
        "lying" to Regex("allonge|au lit|dans le lit|couche|il est (?:vingt|minuit|\\w+ heures)"),
        "sleepgood" to Regex("dormir|sommeil|coucher|bonne nuit|horaire de coucher"),
        "night" to Regex("nuit|soir|dormir|insomnie|sommeil|pensees|ruminer|coucher"),
        "overload" to Regex("charge|trop de|surcharge|taches|deborde|epuis|pression|echeance|travail|burn"),
        "breath" to Regex("respir|inspire|expire|souffle"),
        "talk" to Regex("parle|parler|confie|partag|ecoute|ami|proche|confiance|entourage"),
        "walk" to Regex("march|bouge|sport|promenade|dehors|exterieur|activite physique|nature"),
        "journal" to Regex("carnet|journal|cahier|ecri|noter|gratitude"),
        "alone" to Regex("seul|isole|isolement|honte|silence"),
        "hope" to Regex("mieux|apaise|lumiere|progres|espoir|souffle|reprendre|aller mieux"),
        "pro" to Regex("professionnel|medecin|psycholog|consult|therapeute|aide specialisee"),
        "camp" to Regex("\\bcamps?\\b|deplace|refugie|abri|tente|site d'accueil"),
        "food" to Regex("aliment|vivres|nourriture|faim|riz|repas|ration|malnutrition"),
        "health" to Regex("sante|soin|vaccin|medic|clinique|maladie|infirmi|hygiene"),
        "school" to Regex("ecole|enfants|education|apprendre|classe|etude"),
        "volunteer" to Regex("benevole|\\bong\\b|equipe|solidarite|donateur|partenaire|mobilis|engage|agir"),
        "family" to Regex("famille|mere|parents?|enfants?|orphelin|village|habitants|population"),
        "logistics" to Regex("camion|transport|stock|livraison|acheminer|distribution|route|convoi"),
        "water" to Regex("\\beau\\b|puits|forage|pompe|jerrican|assainissement"),
        "attract" to Regex("publicite|reseaux sociaux|audience|visibilite|attire|contenu|post|abonne|campagne"),
        "product" to Regex("produit|offre|prix|promotion|catalogue|article|service"),
        "pay" to Regex("paiement|achat|commande|panier|mobile money|caisse|payer|facture"),
        "retain" to Regex("fidel|rachat|avis|recommand|suivi|apres-vente|revient|reviens|deuxieme"),
        "measure" to Regex("mesure|taux|chiffre|indicateur|analyse|statistique|resultat|donnees"),
        "customer" to Regex("\\bclients?\\b|acheteur|prospect|consommateur"),
        "shop" to Regex("boutique|magasin|commerce|vendeur|stand\\b|marche\\b|entrepreneur"),
        "dns" to Regex("\\bdns\\b|nom de domaine|resolution de nom|annuaire"),
        "dhcp" to Regex("dhcp|adresse automatique|attribu\\w+ (?:une )?adresse|bail"),
        "nat" to Regex("\\bnat\\b|adresse publique|adresses? privees?|traduction d'adresse"),
        "wifi" to Regex("wi-?fi|point d'acces|sans fil|borne"),
        "vpn" to Regex("\\bvpn\\b|tunnel chiffre|teletravail|a distance"),
        "gateway" to Regex("passerelle|routeur|sortie vers internet|route par defaut"),
        "cabling" to Regex("cable|port\\b|prise|rj45|fibre|brass"),
        "troubleshoot" to Regex("maillon|panne|diagnos|ping|depann|probleme|ne fonctionne pas|coupure|incident|traceroute|verifie"),
        "firstfail" to Regex("premier maillon|premiere etape qui echoue|qui echoue|se trouve souvent"),
        "office" to Regex("entreprise|bureau|employes?|postes?|reseau local|\\blan\\b|salaries|site"),
        "internet" to Regex("internet|fournisseur|\\bfai\\b|cloud|externe|web"),
        "serverroom" to Regex("serveur|datacenter|baie|salle technique|rack"),
        "flow" to Regex("flux|trafic|paquet|donnees|circule|transit|achemin"),
    )

    private fun hits(t: String, lex: Lex): Pair<Int, List<String>> {
        var total = 0
        val ev = ArrayList<String>()
        for ((k, re, w) in lex.patterns) {
            val n = re.findAll(t).count()
            if (n > 0) { total += minOf(n, 3) * w; ev += k }
        }
        return total to ev
    }

    private class Cue<T>(val value: T, regex: String) { val re = Regex(regex) }

    private val ACTORS = listOf(
        Cue(Actor.ATTAQUANT, "attaquant|pirate|hacker|cybercriminel|escroc|fraudeur|arnaqueur|malveillant"),
        Cue(Actor.UTILISATEUR, "utilisateur|employe|victime|collaborateur"),
        Cue(Actor.SERVEUR, "serveur|backend|datacenter"),
        Cue(Actor.CLIENT_APP, "frontend|navigateur|application web|\\bclient\\b.{0,40}(?:serveur|requete)|(?:serveur|requete).{0,40}\\bclient\\b"),
        Cue(Actor.ROUTEUR, "routeur|router|passerelle"),
        Cue(Actor.SWITCH, "switch|commutateur"),
        Cue(Actor.PARE_FEU, "pare-feu|firewall|ufw|iptables"),
        Cue(Actor.BASE_DE_DONNEES, "base de donnees|\\bbdd\\b|\\bsql\\b"),
        Cue(Actor.PROSPECT, "prospect|audience|abonne|\\blead"),
        Cue(Actor.ENTREPRENEUR, "entrepreneur|commercant|vendeur"),
        Cue(Actor.PROCHE, "personne de confiance|proche|ami\\b|famille|collegue"),
        Cue(Actor.PROFESSIONNEL, "medecin|psycholog|professionnel de sante"),
        Cue(Actor.BANQUE, "banque|bancaire"),
        Cue(Actor.COMMUNAUTE, "communaute|village|habitants?|population|familles"),
    )
    private val THINGS = listOf(
        Cue(Thing.MESSAGE, "sms|mail|courriel|message|notification"),
        Cue(Thing.LIEN, "\\blien|url|adresse du site|raccourci"),
        Cue(Thing.MOT_DE_PASSE, "mots? de passe|password|identifiant"),
        Cue(Thing.CODE, "\\bcode\\b|\\bmfa\\b|\\botp\\b|double authentification|deux facteurs"),
        Cue(Thing.PAQUET, "paquet|trafic|connexion entrante|flux"),
        Cue(Thing.REGLE, "\\bregles?\\b|\\bacl\\b|politique de securite"),
        Cue(Thing.REQUETE, "requete|fetch|get /|post /|appel api"),
        Cue(Thing.REPONSE, "reponse|renvoie|retourne|json"),
        Cue(Thing.STATUT, "statut|status|\\b(?:200|404|500)\\b"),
        Cue(Thing.DONNEES, "donnees"),
        Cue(Thing.JOURNAL, "carnet|journal de|cahier|ecris|noter"),
        Cue(Thing.RESPIRATION, "respir|inspire|expire"),
        Cue(Thing.MARCHE, "march(?:e|er|ant)|bouge|sport|promenade"),
        Cue(Thing.SOMMEIL, "sommeil|dors|dormir|repos"),
        Cue(Thing.OFFRE, "offre|prix|promotion|achat"),
        Cue(Thing.JOURNAUX_SYSTEME, "auth\\.log|failed password|journaux|logs?\\b|echecs? d'authentification"),
        Cue(Thing.VLAN, "vlan"),
        Cue(Thing.ORDINATEUR, "ordinateur|\\bpc\\b|poste"),
        Cue(Thing.COMPTE, "compte"),
        Cue(Thing.ETAPES, "etapes?|methode|workflow|processus|enchainement|mission|pipeline|\\b\\d points?"),
        Cue(Thing.EAU, "eau potable|point d'eau|points d'eau|puits|forage|pompe|\\beau\\b"),
        Cue(Thing.MISSION, "mission|reussir|objectif a atteindre|but a atteindre"),
        Cue(Thing.WORKFLOW, "workflow|processus|pipeline|enchainement|circuit"),
        Cue(Thing.SOIR, "soir|nuit|journee|chaque jour|quotidien"),
    )
    private val ACTS = listOf(
        Cue(Act.ENVOYER, "envoie|recoit|recu|expedi|imite"),
        Cue(Act.CLIQUER, "cliqu|appuie|ouvre le lien"),
        Cue(Act.VERIFIER, "verifi|controle|examine|survole|compare"),
        Cue(Act.SAISIR, "saisi|tape |renseign|communique|entre ton|demande de code"),
        Cue(Act.DEVINER, "essay|tente|combinaison|devin|teste"),
        Cue(Act.LIMITER, "limit|verrouill|fail2ban|bannit|apres \\d+ ech"),
        Cue(Act.BLOQUER, "bloqu|refus|rejet|interdi|deni|drop"),
        Cue(Act.AUTORISER, "autoris|laisse passer|permet|accepte|allow"),
        Cue(Act.FILTRER, "filtr"),
        Cue(Act.RELIER, "relie|connect|branche|raccord|cable"),
        Cue(Act.DEMANDER, "demande|requete|appel|fetch|envoie la requete"),
        Cue(Act.REPONDRE, "repond|renvoie|retourne|reponse"),
        Cue(Act.ACTIVER_MFA, "\\bmfa\\b|double authentification|deux facteurs|2fa"),
        Cue(Act.SURVEILLER, "surveill|grep|journal|logs?\\b|supervis"),
        Cue(Act.CHANGER, "change |changer|modifie|renouvel|reinitialis"),
        Cue(Act.SIGNALER, "signal|previens|prevenir|alerte|declare"),
        Cue(Act.NAVIGUER_DIRECT, "toi-meme|application officielle|site officiel|tape l'adresse|favori|ouvre plutot|plutot l'application"),
        Cue(Act.RESPIRER, "respir|inspire|expire"),
        Cue(Act.REMERCIER, "merci|remerci|reconnaissan|gratitude|apprecie"),
        Cue(Act.ECRIRE, "ecri|notes? |carnet|journal de|cahier"),
        Cue(Act.PARTAGER, "partag|parle a|confie|echang|exprime|en parler"),
        Cue(Act.MARCHER, "march(?:e|er)\\b|bouge|sport|promenade|activite physique"),
        Cue(Act.DORMIR, "dors|dormir|sommeil|repos"),
        Cue(Act.ATTIRER, "attire|capte|publicite|trafic|audience"),
        Cue(Act.CONVERTIR, "convertis|convertir|conversion|vend|offre claire|prix"),
        Cue(Act.FIDELISER, "fidelis|suivi apres|deuxieme achat|rachat"),
        Cue(Act.MESURER, "mesure|taux|indicateur|analyse|chiffre"),
        Cue(Act.CHIFFRER, "chiffr|vpn|https"),
        Cue(Act.ENCHAINER, "enchain|etape|ensuite|puis |d'abord|methode|workflow|processus|analys|definir|determiner|comprendre son"),
        Cue(Act.PUISER, "puise|pompe|acces a|approvision|collecter l'eau|ameliore"),
    )

    private val ADVICE_RE = Regex("comment l'eviter|comment l'eviter|eviter|conseil|reflexe|protege|proteger|solution|bonnes? pratiques?|que faire|si tu as clique|comporter|parade|prevenir|gestes?|a essayer|essayer|mets en place|active\\b|utilise\\b|surveille|ne clique")
    private val RECOGNIZE_RE = Regex("reconnai|indices?|signes?|detecter|repere|comment savoir|alerte|symptome")
    private val ATTACK_WORDS = Regex("attaque|pirate|hacker|piege|arnaque|usurp|imite|essayer de nombreuses|intrusion|rancon|malveillant|vol de")

    /** Concepts qui supposent un attaquant sans que le texte le nomme. */
    private val IMPLIES_ATTACKER = setOf(Concept.PHISHING, Concept.BRUTE_FORCE, Concept.MALWARE)

    private fun sceneText(sc: Scene): String {
        val parts = ArrayList<String>()
        parts += sc.title; parts += sc.body; sc.subtitle?.let { parts += it }
        parts += sc.items
        sc.compare?.let { parts += it.badLabel; parts += it.badText(); parts += it.goodLabel; parts += it.goodText() }
        sc.statValue?.let { parts += it }
        return Marked.strip(parts.joinToString(" "))
    }

    /** Concept dominant d'un texte entier (déjà normalisé, sans accents) pour une famille : [Concept.NONE] si rien n'est assez net. */
    fun dominantConcept(normalized: String, family: ContentFamily): Concept {
        val allowed = allowedConcepts(family)
        val best = LEX.filter { it.concept in allowed }.map { it.concept to hits(normalized, it).first }.maxByOrNull { it.second }
        return if (best != null && best.second >= 8) best.first else Concept.NONE
    }

    /** Texte entier d'une vidéo (titre + scènes), pour le concept dominant. */
    fun videoText(sb: Storyboard): String = Marked.strip(sb.title + " " + sb.scenes.joinToString(" ") { sceneText(it) })

    private fun conceptScores(t: String): Map<Concept, Pair<Int, List<String>>> = LEX.associate { it.concept to hits(t, it) }

    /**
     * Établit l'intention de chaque scène. Le contexte de la vidéo entière pèse à moitié : une scène « Comment l'éviter »
     * qui ne répète pas le mot « phishing » reste une scène sur le phishing.
     */
    fun analyze(sb: Storyboard, profile: ContentProfile = sb.profile ?: ContentAnalyzer.analyze(videoText(sb))): List<SceneIntent> {
        val videoNorm = " " + DomainDetector.normalize(TextSanitizer.clean(videoText(sb))) + " "
        val video = conceptScores(videoNorm)
        val last = sb.scenes.lastIndex
        return sb.scenes.mapIndexed { i, sc ->
            val clean = TextSanitizer.clean(sceneText(sc))
            val t = " " + DomainDetector.normalize(clean) + " "
            val local = conceptScores(t)
            // Concept : local + moitié du contexte vidéo.
            val total = Concept.entries.filter { it != Concept.NONE }.associateWith { c -> local.getValue(c).first + video.getValue(c).first / 2 }
            val ranked = total.entries.sortedByDescending { it.value }
            val top = ranked.first()
            // Un domaine non technique n'accepte pas un concept technique par simple voisinage de mots.
            val allowed = allowedConcepts(profile.family)
            val filtered = ranked.filter { it.key in allowed }
            val best = filtered.firstOrNull()
            // Une scène qui n'a pas de mot du lexique (« À retenir… », une ouverture) garde le sujet de la vidéo entière quand celui-ci est net.
            val ownConcept = best != null && best.value >= 5
            val concept = if (ownConcept) best!!.key else if (profile.concept != Concept.NONE && profile.concept in allowed) profile.concept else Concept.NONE
            val second = if (concept == Concept.NONE || !ownConcept) emptyList()
            else filtered.drop(1).filter { it.value >= 4 && it.value * 5 >= best!!.value * 2 }.map { it.key }.take(2)
            val confidence = if (concept == Concept.NONE) 0f else if (!ownConcept) 0.35f else {
                val rival = filtered.getOrNull(1)?.value ?: 0
                clamp01((best!!.value - rival).toFloat() / (best.value + 6f) + 0.25f).coerceAtMost(1f)
            }

            val actors = ACTORS.filter { it.re.containsMatchIn(t) }.map { it.value }.toMutableSet()
            val things = THINGS.filter { it.re.containsMatchIn(t) }.map { it.value }.toMutableSet()
            // Un support (SMS, courriel) cité n'importe où dans la vidéo vaut pour toutes ses scènes : « Comment le reconnaître » parle du même SMS.
            if (Regex("(?<![a-z])(?:sms|texto|smishing)").containsMatchIn(videoNorm)) things += Thing.SMS
            if (Regex("e-?mail|courriel|mel\\b|boite de reception").containsMatchIn(videoNorm)) things += Thing.EMAIL
            // Garde sémantique : un volume 3D ou une scène d'attaque ne sont admis que si le texte parle d'un objet ou d'une attaque.
            if (SemanticGate.hasObject3D(t)) things += Thing.OBJET_3D
            if (SemanticGate.hasAttack(videoNorm)) things += Thing.ATTAQUE
            val acts = ACTS.filter { it.re.containsMatchIn(t) }.map { it.value }.toMutableSet()
            val topics = TOPICS.filter { it.second.containsMatchIn(t) }.map { it.first }.toSet()
            // Un lexique de concept ne fait pas un acte : on retire ce que le concept n'autorise pas à illustrer seul.
            val implied = HashSet<Actor>()
            if (concept in IMPLIES_ATTACKER && Actor.ATTAQUANT !in actors) implied += Actor.ATTAQUANT
            if (concept == Concept.FIREWALL) implied += Actor.PARE_FEU
            if (concept == Concept.CLIENT_SERVER) { implied += Actor.SERVEUR; implied += Actor.CLIENT_APP }
            if (concept == Concept.TOPOLOGY || concept == Concept.SEGMENTATION) implied += Actor.SWITCH

            val adviceWords = ADVICE_RE.containsMatchIn(t)
            val recognizeWords = RECOGNIZE_RE.containsMatchIn(" " + DomainDetector.normalize(sc.title) + " ")
            val attackWords = ATTACK_WORDS.containsMatchIn(t)
            val role = when {
                (i == 0 && sc.layout == Layout.HERO) || sc.isEpisodeOpener -> Role.INTRO
                sc.layout == Layout.CHECKLIST && i == last -> Role.RECAP
                sc.layout == Layout.CHECKLIST -> Role.RECAP
                sc.layout == Layout.COMPARE -> Role.CONTRAST
                sc.layout == Layout.STAT -> Role.MEASURE
                recognizeWords -> Role.RECOGNIZE
                adviceWords && !(attackWords && !DomainDetector.normalize(sc.title).contains("evit")) -> Role.ADVICE
                adviceWords && sc.layout == Layout.STEPS -> Role.ADVICE
                sc.layout == Layout.STEPS -> if (concept in IMPLIES_ATTACKER || concept == Concept.FIREWALL || concept == Concept.CLIENT_SERVER || concept == Concept.SEGMENTATION) Role.PROCEDURE else Role.PROCEDURE
                sc.layout == Layout.DEFINITION && attackWords && concept in IMPLIES_ATTACKER -> Role.DEFINE
                sc.layout == Layout.DEFINITION -> if (concept == Concept.CLIENT_SERVER || concept == Concept.TOPOLOGY || concept == Concept.SEGMENTATION || concept == Concept.FIREWALL) Role.MECHANISM else Role.DEFINE
                else -> Role.DEFINE
            }

            val params = LinkedHashMap<String, String>()
            Regex("vlan ?(\\d{1,4})", RegexOption.IGNORE_CASE).findAll(sc.title + " " + sc.body + " " + sc.items.joinToString(" ") + " " + sb.title)
                .map { it.groupValues[1] }.distinct().take(3).toList().takeIf { it.size >= 2 }?.let { params["vlans"] = it.joinToString(",") }
            Regex("\\b(GET|POST|PUT|DELETE) (/[\\w/.-]*)").find(clean + " " + videoText(sb))?.let { params["request"] = it.groupValues[1] + " " + it.groupValues[2] }
            Regex("(?:inspire|inhale)\\D{0,18}(\\w+) ?(?:seconde|s\\b)", RegexOption.IGNORE_CASE).find(clean)?.let { params["inhale"] = it.groupValues[1].lowercase() }

            StepLabels.of(sc).takeIf { it.size >= 2 }?.let { params["steps"] = it.joinToString("|") }
            StepLabels.of(sb.scenes.firstOrNull { StepLabels.of(it).size >= 3 } ?: sc).takeIf { it.size >= 2 }?.let { params["videoSteps"] = it.joinToString("|") }

            val relation = when {
                concept in IMPLIES_ATTACKER && (Actor.ATTAQUANT in implied || Actor.ATTAQUANT in actors) && role != Role.ADVICE && role != Role.RECAP -> Relation.ATTAQUE_CIBLE
                concept == Concept.FIREWALL || ((Act.BLOQUER in acts || Act.FILTRER in acts) && Act.AUTORISER in acts) -> Relation.FILTRE
                concept == Concept.CLIENT_SERVER || (Act.DEMANDER in acts && Act.REPONDRE in acts) -> Relation.DEMANDE_REPONSE
                concept == Concept.TOPOLOGY || concept == Concept.SEGMENTATION -> Relation.CONNEXION
                concept == Concept.STRESS && (role == Role.INTRO || role == Role.DEFINE) -> Relation.TRANSFORMATION
                concept == Concept.STRESS && Act.PARTAGER in acts -> Relation.SOUTIEN
                concept == Concept.FUNNEL || concept == Concept.METHOD || sc.layout == Layout.STEPS && concept != Concept.NONE -> Relation.PROGRESSION
                concept == Concept.WATER || concept == Concept.AID -> Relation.TRANSFORMATION
                concept == Concept.NETFLOW -> Relation.CONNEXION
                sc.layout == Layout.COMPARE -> Relation.CONTRASTE
                else -> Relation.AUCUNE
            }

            val context = when {
                concept == Concept.GRATITUDE || concept == Concept.STRESS -> Context.INTIME
                concept == Concept.FUNNEL -> Context.COMMERCIAL
                concept == Concept.WATER || concept == Concept.AID -> Context.COMMUNAUTE
                Regex("entreprise|societe|collaborateur|equipe|bureau|reseau local|datacenter|dmz").containsMatchIn(t) -> Context.ENTREPRISE
                concept == Concept.CLIENT_SERVER || Regex("\\bcode\\b|commande|`|console").containsMatchIn(t) || clean.contains('`') -> Context.TECHNIQUE
                concept in IMPLIES_ATTACKER || concept == Concept.FIREWALL -> Context.INTERNET
                profile.family == ContentFamily.PERSONAL || profile.family == ContentFamily.MENTAL -> Context.PERSONNEL
                else -> Context.INTERNET
            }

            val goal = when {
                role == Role.RECAP -> Goal.MEMORISER
                concept == Concept.STRESS -> Goal.APAISER
                concept == Concept.GRATITUDE -> if (profile.objective == Objective.MOTIVER) Goal.MOTIVER else Goal.APAISER
                concept == Concept.FUNNEL -> if (role == Role.INTRO) Goal.CONVAINCRE else Goal.SUIVRE_PROCEDURE
                concept == Concept.METHOD -> if (role == Role.DEFINE || role == Role.INTRO || role == Role.MECHANISM) Goal.COMPRENDRE_MECANISME else Goal.SUIVRE_PROCEDURE
                concept == Concept.WATER || concept == Concept.AID -> Goal.MOTIVER
                concept == Concept.NETFLOW -> if (role == Role.PROCEDURE || role == Role.ADVICE) Goal.SUIVRE_PROCEDURE else Goal.COMPRENDRE_MECANISME
                role == Role.ADVICE -> Goal.APPRENDRE_REFLEXE
                role == Role.RECOGNIZE -> Goal.IDENTIFIER_MENACE
                role == Role.MECHANISM -> Goal.COMPRENDRE_MECANISME
                concept == Concept.TOPOLOGY || concept == Concept.SEGMENTATION -> Goal.VOIR_ARCHITECTURE
                role == Role.PROCEDURE -> Goal.SUIVRE_PROCEDURE
                concept in IMPLIES_ATTACKER && (role == Role.INTRO || role == Role.DEFINE) -> Goal.IDENTIFIER_MENACE
                else -> Goal.COMPRENDRE_MECANISME
            }

            val action = pickAction(acts, concept, role)
            val ev = ArrayList<String>()
            if (concept != Concept.NONE) ev += local.getValue(concept).second.take(4).ifEmpty { listOf("sujet de la vidéo entière") }
            ev += "rôle : ${role.label}"
            val motifs = sc.items.map { itemMotif(it) }
            SceneIntent(profile.domain, profile.family, concept, second, role, actors, implied, things, acts, action, context, relation, goal, params, motifs, confidence, ev, topics)
        }
    }

    private val MOTIFS = listOf(
        "mfa" to Regex("\\bmfa\\b|double authentification|deux facteurs|2fa"),
        "limit" to Regex("limit|fail2ban|verrouill|bannit"),
        "monitor" to Regex("surveill|grep|journal|echecs? d'authentification|logs?\\b"),
        "password" to Regex("mots? de passe|robuste"),
        "breath" to Regex("respir|inspire|expire"),
        "name" to Regex("nomme|identifie ton|dire .{0,12}je me sens|exprime"),
        "walk" to Regex("march|bouge|sport|promenade|activite physique"),
        "talk" to Regex("parle|partag|confiance|confie|en parler"),
        "sleep" to Regex("dors|dormir|sommeil|repos"),
        "attract" to Regex("attire|publicite|audience|contenu qui parle"),
        "capture" to Regex("capte|offre gratuite|contact|e-?mail|whatsapp|inscri"),
        "convert" to Regex("convertis|offre claire|prix|preuve sociale|achat"),
        "retain" to Regex("fidelis|suivi apres|deuxieme achat|rachat"),
        "measure" to Regex("mesure|taux|indicateur"),
    )

    /** Motif visuel d'un élément de liste (premier lexique qui correspond) : permet d'illustrer chaque ligne par son propre sens. */
    fun itemMotif(item: String): String {
        val full = DomainDetector.normalize(TextSanitizer.clean(Marked.strip(item)))
        // Le terme qui ouvre la ligne (« Fidélise : un suivi après l'achat… ») dit de quoi parle la ligne ; le reste ne sert qu'à défaut.
        val lead = " " + full.substringBefore(':').split(' ').take(3).joinToString(" ") + " "
        return MOTIFS.firstOrNull { it.second.containsMatchIn(lead) }?.first
            ?: MOTIFS.firstOrNull { it.second.containsMatchIn(" $full ") }?.first ?: ""
    }

    /** Domaines où un concept peut s'appliquer : « stress » dans un texte de réseau n'est pas un sujet d'apaisement. */
    private fun allowedConcepts(f: ContentFamily): Set<Concept> = when (f) {
        ContentFamily.CYBER -> setOf(Concept.PHISHING, Concept.BRUTE_FORCE, Concept.MALWARE, Concept.FIREWALL, Concept.CLIENT_SERVER, Concept.TOPOLOGY, Concept.SEGMENTATION, Concept.NETFLOW)
        ContentFamily.NETWORK -> setOf(Concept.TOPOLOGY, Concept.SEGMENTATION, Concept.FIREWALL, Concept.CLIENT_SERVER, Concept.MALWARE, Concept.BRUTE_FORCE, Concept.PHISHING, Concept.NETFLOW)
        ContentFamily.DEV -> setOf(Concept.CLIENT_SERVER, Concept.FIREWALL, Concept.MALWARE, Concept.BRUTE_FORCE, Concept.METHOD, Concept.NETFLOW)
        ContentFamily.AI -> setOf(Concept.CLIENT_SERVER, Concept.MALWARE, Concept.PHISHING, Concept.METHOD)
        ContentFamily.MENTAL, ContentFamily.HEALTH -> setOf(Concept.STRESS, Concept.GRATITUDE)
        ContentFamily.PERSONAL -> setOf(Concept.GRATITUDE, Concept.STRESS, Concept.METHOD, Concept.WATER, Concept.AID)
        ContentFamily.MARKETING -> setOf(Concept.FUNNEL, Concept.METHOD)
        ContentFamily.BIBLE -> setOf(Concept.GRATITUDE)
        // Famille indéterminée (phrase seule, texte court) : aucun domaine ne restreint, c'est le lexique du concept qui décide.
        ContentFamily.GENERAL -> Concept.entries.filter { it != Concept.NONE }.toSet()
        ContentFamily.HEALTH -> setOf(Concept.STRESS, Concept.GRATITUDE, Concept.WATER, Concept.AID)
        else -> setOf(Concept.METHOD, Concept.WATER, Concept.GRATITUDE, Concept.AID)
    }

    /** Action principale : la plus spécifique au concept, sinon la première dans l'ordre du texte. */
    private fun pickAction(acts: Set<Act>, concept: Concept, role: Role): Act? {
        if (acts.isEmpty()) return null
        val order: List<Act> = when (concept) {
            Concept.PHISHING -> if (role == Role.ADVICE) listOf(Act.NAVIGUER_DIRECT, Act.CHANGER, Act.SIGNALER, Act.VERIFIER) else listOf(Act.SAISIR, Act.CLIQUER, Act.ENVOYER)
            Concept.BRUTE_FORCE -> listOf(Act.LIMITER, Act.ACTIVER_MFA, Act.SURVEILLER, Act.DEVINER)
            Concept.FIREWALL -> listOf(Act.FILTRER, Act.BLOQUER, Act.AUTORISER)
            Concept.TOPOLOGY, Concept.SEGMENTATION -> listOf(Act.RELIER, Act.FILTRER)
            Concept.CLIENT_SERVER -> listOf(Act.DEMANDER, Act.REPONDRE)
            Concept.GRATITUDE -> listOf(Act.ECRIRE, Act.REMERCIER, Act.PARTAGER)
            Concept.STRESS -> listOf(Act.RESPIRER, Act.PARTAGER, Act.MARCHER, Act.DORMIR)
            Concept.FUNNEL -> listOf(Act.ATTIRER, Act.CONVERTIR, Act.FIDELISER, Act.MESURER)
            else -> emptyList()
        }
        return order.firstOrNull { it in acts } ?: acts.first()
    }
}


/**
 * Libellés d'étapes tirés du texte de l'AUTEUR : la tête de chaque ligne d'une liste (« Rôle : dis à l'IA… » donne RÔLE) ou, pour une phrase
 * qui énumère (« il faut comprendre son rôle, analyser le contexte, définir la tâche… »), le nom de chaque proposition. Rien n'est inventé :
 * un texte qui n'énumère pas ses étapes n'en reçoit aucune, et le plan affiche alors des repères génériques étiquetés comme tels.
 */
internal object StepLabels {
    private val DET = setOf("le", "la", "les", "l", "un", "une", "des", "du", "de", "d", "son", "sa", "ses", "ton", "ta", "tes", "ce", "cette", "ces", "leur", "leurs", "notre", "votre", "mon", "ma", "mes")

    private val TRAIL = setOf("et", "ou", "puis", "avec", "pour", "sur", "dans", "en", "par", "a", "\u00e0", "au", "aux", "que", "qui", "si", "ni", "mais") + DET

    fun of(sc: Scene): List<String> {
        if (sc.items.size >= 2) {
            val leads = sc.items.map { lead(Marked.strip(it)) }.filter { it.isNotBlank() }
            if (leads.size >= 2) return leads
        }
        return clauses(Marked.strip(sc.title + ". " + sc.body + " " + (sc.subtitle ?: "")))
    }

    private fun lead(item: String): String {
        val head = item.substringBefore(':').substringBefore(" - ").trim()
        val words = head.split(Regex("\\s+")).filter { it.isNotBlank() }
        return clean(words.take(if (head.length == item.trim().length) 2 else 3))
    }

    private fun clean(words: List<String>): String {
        val w = words.map { it.trim(',', '.', ';', ':', '!', '?', '(', ')', '\u00ab', '\u00bb') }.filter { it.isNotBlank() }
        val kept = w.dropWhile { it.lowercase().substringBefore('\'') in DET || it.lowercase().endsWith("\'") }
        // Pas de mot de liaison pendant en fin d'étiquette (« git push et » -> « git push »).
        val trimmed = kept.dropLastWhile { it.lowercase() in TRAIL }.ifEmpty { kept }
        return trimmed.joinToString(" ").uppercase().take(18).trim()
    }

    private fun clauses(text: String): List<String> {
        val t = TextSanitizer.clean(text)
        val low = t.lowercase()
        val at = listOf("il faut ", "il suffit de ", "pour cela, ", "pour cela : ").map { low.lastIndexOf(it) to it.length }.filter { it.first >= 0 }.maxByOrNull { it.first }
        val tail = if (at != null) t.substring(at.first + at.second) else return emptyList()
        val parts = tail.split(Regex(",\\s*|;\\s*|\\s+et\\s+|\\s+puis\\s+")).map { it.trim().trimEnd('.', '!', '?') }.filter { it.isNotBlank() }
        if (parts.size < 3 || parts.size > 6) return emptyList()
        // « analyser le contexte » -> CONTEXTE ; « définir précisément la tâche » -> TÂCHE : on garde le groupe nominal final.
        val out = parts.map { p -> val words = p.split(Regex("\\s+")); clean(words.takeLast(minOf(2, words.size))) }
        return if (out.all { it.length in 2..18 }) out else emptyList()
    }
}
