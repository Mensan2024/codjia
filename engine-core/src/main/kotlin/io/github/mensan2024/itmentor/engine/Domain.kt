// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import java.text.Normalizer

/** Domaines reconnus dans le texte de l'utilisateur. */
enum class Domain(val label: String) {
    CYBER("Cybersécurité et informatique"),
    SANTE("Santé et prévention"),
    AGRO("Agriculture et environnement"),
    MARKETING("Marketing et entrepreneuriat"),
    BUSINESS("Entreprise et management"),
    EDUCATION("Éducation et formation"),
    GENERAL("Général"),
}

data class DomainScore(val domain: Domain, val score: Int)

/**
 * Détection du domaine, entièrement hors ligne : on compte les mots-clés (racines, sans accents).
 * Simple, explicable et testable ; le mode IA pourra l'affiner plus tard.
 */
object DomainDetector {
    private val KEYWORDS: Map<Domain, List<String>> = mapOf(
        Domain.CYBER to listOf("cyber", "pirat", "hameconn", "phishing", "mot de passe", "motdepasse", "virus", "malware", "rancon",
            "securite informatique", "reseau", "wifi", "pare-feu", "firewall", "chiffr", "donnees", "compte", "lien", "mail",
            "authentific", "vpn", "logiciel", "informatique", "serveur", "hacker", "arnaque", "fraude"),
        Domain.SANTE to listOf("sante", "maladie", "medec", "soin", "hopital", "vaccin", "paludisme", "moustique", "hygien",
            "mains", "savon", "symptom", "prevention", "nutrition", "eau potable", "infirm", "patient", "diabet", "tension",
            "virus de", "fievre", "epidem", "medicament", "grossesse", "allait"),
        Domain.AGRO to listOf("agricul", "agronom", "culture", "recolte", "semence", "sol", "engrais", "compost", "irrigation",
            "eleva", "betail", "plante", "arrosage", "champ", "paysan", "maraich", "climat", "environnement", "arbre", "foret",
            "ecolog", "durable", "pluie", "secheresse", "biodiversite"),
        Domain.MARKETING to listOf("marketing", "client", "vente", "vendre", "marque", "publicite", "pub ", "reseaux sociaux",
            "abonne", "publication", "communication", "prospect", "promo", "entrepreneur", "startup", "boutique", "commerce",
            "instagram", "facebook", "tiktok", "contenu", "audience", "branding"),
        Domain.BUSINESS to listOf("entreprise", "manag", "equipe", "reunion", "strategie", "budget", "finance", "projet",
            "leadership", "productivite", "organisation", "rh ", "recrut", "gestion", "investis", "objectif", "performance"),
        Domain.EDUCATION to listOf("apprendre", "eleve", "etudiant", "cours", "lecon", "ecole", "enseign", "formation",
            "examen", "revision", "pedagog", "memoriser", "methode", "exercice", "universite", "professeur"),
    )

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .replace('’', '\'').replace(Regex("\\s+"), " ")

    fun scores(text: String): List<DomainScore> {
        val t = " " + normalize(text) + " "
        return KEYWORDS.map { (d, keys) ->
            DomainScore(d, keys.sumOf { k -> Regex("(?<![a-z])" + Regex.escape(k)).findAll(t).count() })
        }.sortedByDescending { it.score }
    }

    fun detect(text: String): Domain = scores(text).first().let { if (it.score >= 2) it.domain else Domain.GENERAL }

    /**
     * Templates classés du plus adapté au moins adapté pour ce texte. Tous restent proposés :
     * l'utilisateur garde le libre choix.
     */
    fun recommend(text: String): List<Template> {
        val sc = scores(text).associate { it.domain to it.score }
        val main = detect(text)
        return Templates.ALL.sortedByDescending { t ->
            // Domaine principal du template (le premier déclaré) > domaine secondaire > score des mots-clés.
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
