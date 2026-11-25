// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Domaines de CODJIA : l'informatique, l'IA, et quelques compétences transverses. */
enum class Domain(val label: String, val kicker: String) {
    RESEAUX("Réseaux informatiques", "ASTUCE RÉSEAUX"),
    SYSTEMES("Systèmes : Windows, macOS, Linux, Android", "ASTUCE SYSTÈMES"),
    CYBER("Cybersécurité", "ASTUCE CYBERSÉCURITÉ"),
    IA("Intelligence artificielle", "ASTUCE IA"),
    FULLSTACK("Programmation fullstack", "ASTUCE DEV"),
    SANTE("Santé et longévité", "ASTUCE SANTÉ"),
    MARKETING("Marketing", "ASTUCE MARKETING"),
    ANGLAIS("Anglais", "ENGLISH TIP"),
    GENERAL("Général", "ASTUCE"),
}

data class DomainScore(val domain: Domain, val score: Int)

/**
 * Détection du domaine, entièrement hors ligne : on compte des mots-clés (racines, sans accents),
 * avec un poids double pour les termes très spécifiques. Simple, explicable et testable.
 */
object DomainDetector {
    /** (mot-clé, poids). Un mot-clé commence à une frontière de mot ; il peut être une racine. */
    private val KEYWORDS: Map<Domain, List<Pair<String, Int>>> = mapOf(
        Domain.RESEAUX to listOf("ccna" to 3, "ccnp" to 3, "vlan" to 3, "ospf" to 3, "eigrp" to 3, "bgp" to 3, "routeur" to 2,
            "router" to 2, "switch" to 2, "commutateur" to 2, "adresse ip" to 2, "ipv4" to 2, "ipv6" to 2, "sous-reseau" to 2,
            "subnet" to 2, "masque" to 1, "trunk" to 2, "spanning" to 2, "stp" to 2, "hsrp" to 3, "dhcp" to 2, "dns" to 1, "nat" to 1,
            "tcp" to 1, "udp" to 1, "osi" to 2, "reseau" to 1, "wifi" to 1, "ethernet" to 2, "cisco" to 2, "mikrotik" to 3,
            "ping" to 1, "traceroute" to 2, "acl" to 2, "routage" to 2, "lan" to 1, "wan" to 1, "paquet" to 1),
        Domain.SYSTEMES to listOf("windows" to 2, "macos" to 3, "mac os" to 3, "linux" to 2, "ubuntu" to 2, "debian" to 2,
            "android" to 2, "terminal" to 1, "powershell" to 3, "bash" to 2, "sudo" to 2, "apt" to 1, "systemd" to 3,
            "explorateur" to 1, "finder" to 2, "registre" to 1, "active directory" to 3, "windows server" to 3, "gestionnaire des taches" to 2,
            "noyau" to 2, "kernel" to 2, "partition" to 2, "disque" to 1, "mise a jour" to 1, "pilote" to 1, "raccourci" to 1, "clavier" to 1),
        Domain.CYBER to listOf("cyber" to 3, "pirat" to 2, "hameconn" to 3, "phishing" to 3, "mot de passe" to 2, "mots de passe" to 2,
            "virus" to 2, "malware" to 3, "rancon" to 3, "ransomware" to 3, "chiffr" to 2, "mfa" to 3, "authentific" to 2,
            "vpn" to 2, "pare-feu" to 2, "firewall" to 2, "vulnerab" to 2, "attaque" to 2, "force brute" to 3, "certificat" to 2,
            "https" to 1, "fuite" to 1, "arnaque" to 2, "fraude" to 2, "audit de securite" to 3, "securite" to 1, "rdp" to 2,
            "fail2ban" to 3, "intrusion" to 2, "sauvegarde" to 1, "permission" to 1),
        Domain.IA to listOf("intelligence artificielle" to 3, "ia" to 2, "chatgpt" to 3, "claude" to 2, "gemini" to 2, "prompt" to 3,
            "llm" to 3, "modele de langage" to 3, "machine learning" to 3, "apprentissage automatique" to 3, "deep learning" to 3,
            "reseau de neurones" to 3, "neurone" to 2, "donnees d'entrainement" to 3, "hallucination" to 3, "agent" to 1, "algorithme" to 1),
        Domain.FULLSTACK to listOf("javascript" to 3, "typescript" to 3, "python" to 2, "java" to 2, "kotlin" to 3, "react" to 3,
            "node" to 2, "html" to 3, "css" to 3, "api" to 2, "backend" to 3, "frontend" to 3, "fullstack" to 3, "base de donnees" to 2,
            "sql" to 2, "git" to 2, "github" to 2, "fonction" to 1, "variable" to 1, "code" to 1, "developp" to 1, "framework" to 2,
            "debug" to 2, "bug" to 1, "deploi" to 1, "docker" to 2, "json" to 2, "http" to 1, "programm" to 1),
        Domain.SANTE to listOf("sante" to 2, "longtemps" to 2, "longevite" to 3, "sommeil" to 2, "dormir" to 2, "hydrat" to 2,
            "eau" to 1, "marche" to 1, "sport" to 2, "activite physique" to 3, "alimentation" to 2, "manger" to 1, "legumes" to 2,
            "fruits" to 1, "sucre" to 1, "stress" to 2, "tension" to 1, "coeur" to 2, "medecin" to 2, "prevention" to 1,
            "bien-etre" to 2, "respiration" to 2, "posture" to 2, "ecran" to 1, "yeux" to 1, "pause" to 1),
        Domain.MARKETING to listOf("marketing" to 3, "client" to 2, "vente" to 2, "vendre" to 2, "marque" to 2, "publicite" to 2,
            "reseaux sociaux" to 2, "abonne" to 2, "publication" to 2, "linkedin" to 2, "facebook" to 2, "tiktok" to 2, "instagram" to 2,
            "contenu" to 1, "audience" to 2, "prospect" to 2, "branding" to 3, "seo" to 3, "conversion" to 2, "entrepreneur" to 2),
        Domain.ANGLAIS to listOf("anglais" to 3, "english" to 3, "vocabulaire" to 2, "vocabulary" to 3, "grammaire" to 2, "grammar" to 3,
            "prononciation" to 3, "pronunciation" to 3, "verbe" to 1, "verb" to 2, "phrase" to 1, "sentence" to 2, "traduction" to 2,
            "speaking" to 3, "listening" to 3, "present perfect" to 3, "irregular" to 3, "word" to 1, "learn" to 1, "the" to 1, "you" to 1),
    )

    private const val ACCENTED = "àâäáãåçéèêëíìîïñóòôöõúùûüýÿœæÀÂÄÁÃÅÇÉÈÊËÍÌÎÏÑÓÒÔÖÕÚÙÛÜÝŸ"
    private const val PLAIN = "aaaaaaceeeeiiiinooooouuuuyy\u0000\u0000AAAAAACEEEEIIIINOOOOOUUUUYY"

    /** Retire les accents (sans java.text.Normalizer, pour tourner aussi dans le navigateur). */
    fun stripAccents(s: String): String = buildString(s.length) {
        for (c in s) {
            when (c) {
                'œ' -> append("oe"); 'æ' -> append("ae"); 'Œ' -> append("OE"); 'Æ' -> append("AE")
                else -> { val i = ACCENTED.indexOf(c); append(if (i >= 0) PLAIN[i] else c) }
            }
        }
    }

    fun normalize(text: String): String =
        stripAccents(text.lowercase())
            .replace('’', '\'').replace(Regex("\\s+"), " ")

    private val patterns: Map<Domain, List<Pair<Regex, Int>>> = KEYWORDS.mapValues { (_, keys) ->
        keys.map { (k, w) -> Regex("(?<![a-z0-9])" + Regex.escape(k)) to w }
    }

    fun scores(text: String): List<DomainScore> {
        val t = " " + normalize(text) + " "
        return patterns.map { (d, ps) -> DomainScore(d, ps.sumOf { (re, w) -> re.findAll(t).count() * w }) }
            .sortedByDescending { it.score }
    }

    fun detect(text: String): Domain = scores(text).first().let { if (it.score >= 3) it.domain else Domain.GENERAL }

    /** Le mot appartient-il au vocabulaire du domaine ? (sert à choisir le mot mis en valeur dans un titre) */
    fun isKeyword(word: String, d: Domain): Boolean {
        val w = normalize(word).trim(',', '.', '!', '?', ':', '\u00A0', '(', ')', '\'', '"')
        if (w.length < 3) return false
        return KEYWORDS[d].orEmpty().any { (k, _) -> !k.contains(' ') && k.length >= 4 && w.startsWith(k) }
    }

    /** Les 8 domaines « réels », pour les menus. */
    val ALL = Domain.entries.filter { it != Domain.GENERAL }

    /** Templates classés du plus adapté au moins adapté pour ce texte. Tous restent proposés. */
    fun recommend(text: String): List<Template> {
        val sc = scores(text).associate { it.domain to it.score }
        val main = detect(text)
        return Templates.ALL.sortedByDescending { t ->
            (if (t.domains.first() == main) 2000 else if (main in t.domains) 1000 else 0) +
                t.domains.sumOf { sc[it] ?: 0 } * 10 + (if (Domain.GENERAL in t.domains) 1 else 0)
        }
    }

    /** Texte d'un storyboard, pour l'analyse. */
    fun textOf(sb: Storyboard): String = buildString {
        append(sb.title).append(' ')
        for (s in sb.scenes) {
            append(s.title).append(' ').append(s.body).append(' ').append(s.subtitle ?: "").append(' ')
            s.items.forEach { append(it).append(' ') }
        }
    }.replace("*", "")
}
