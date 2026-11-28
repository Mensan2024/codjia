// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Ce que le moteur a compris d'une demande courte. */
class Ask(val prompt: String, val card: TopicCard?, val level: TechLevel, val note: String)

/**
 * Phase D.1 — moteur de compréhension et de scénarisation : une demande courte (« Phishing », « Explique-moi les VLAN ») devient une leçon complète
 * écrite dans l'ordre pédagogique du type de sujet. Le texte produit est ensuite traité par le même pipeline que n'importe quel texte d'auteur
 * (découpage en scènes, choix des visuels, épisodes), donc la fidélité et l'édition du storyboard restent identiques.
 *
 * Hors bibliothèque : AUCUNE leçon n'est inventée. [Ask.card] est null et [Ask.note] l'explique ; la voie « LLM optionnel » (D.3) le complétera.
 */
object LessonWriter {
    private val FILLER = listOf(
        "explique-moi", "explique moi", "expliquez-moi", "peux-tu m'expliquer", "fais-moi une video sur", "fais une video sur", "video sur", "video de", "c'est quoi", "qu'est-ce que", "qu'est ce que",
        "comment fonctionne", "comment marche", "comment ca marche", "fonctionnement de", "fonctionnement d'", "fonctionnement du", "le fonctionnement", "principe de", "principe du", "attaque par", "attaque de", "une attaque", "l'attaque", "attaque",
        "la ", "le ", "les ", "un ", "une ", "des ", "du ", "de ", "d'", "l'",
    )

    fun normalize(s: String): String = DomainDetector.normalize(s).replace(Regex("[?!.,;:«»\"]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Compare le sujet demandé aux noms des fiches : égalité d'abord, puis inclusion d'un nom entier dans la demande. */
    fun understand(prompt: String, level: TechLevel = TechLevel.DEBUTANT): Ask {
        val q = normalize(prompt)
        var core = " $q "
        for (f in FILLER) core = core.replace(" $f", " ")
        core = core.replace(Regex("\\s+"), " ").trim()
        var best: TopicCard? = null; var bestScore = 0
        for (c in Knowledge.cards) for (n in c.names) {
            val nn = normalize(n)
            val s = when {
                core == nn || q == nn -> 100 + nn.length
                Regex("(?<![a-z0-9])" + Regex.escape(nn) + "(?![a-z0-9])").containsMatchIn(q) -> 50 + nn.length
                else -> 0
            }
            if (s > bestScore) { bestScore = s; best = c }
        }
        return if (best != null) Ask(prompt, best, level, "Sujet reconnu : ${best.title} (niveau ${level.label}).")
        else Ask(prompt, null, level, "Sujet absent de la bibliothèque (${Knowledge.cards.joinToString { it.id }}) : aucune leçon n'est inventée.")
    }

    /** La leçon, sous forme de texte d'auteur (titre, paragraphes, listes à tirets, mention finale). */
    fun write(card: TopicCard, level: TechLevel = TechLevel.DEBUTANT): String {
        fun keep(l: List<Fact>) = l.filter { it.from.ordinal <= level.ordinal }.map { it.text }
        val sb = StringBuilder()
        fun section(head: String, items: List<String>) {
            if (items.isEmpty()) return
            sb.appendLine(head)
            items.forEach { sb.appendLine("- $it") }
        }
        sb.appendLine(card.title)
        sb.appendLine(card.hook)
        sb.appendLine((listOf(card.definition) + keep(card.variants)).joinToString(" "))
        if (card.kind == TopicKind.ATTACK) {
            section("Comment l'attaque se déroule :", keep(card.steps))
            section("Les conséquences :", keep(card.consequences))
        } else {
            // Notion : à quoi ça sert avant le fonctionnement.
            section("À quoi ça sert :", keep(card.consequences))
            section("Comment ça fonctionne :", keep(card.steps))
        }
        section("Comment le reconnaître :", keep(card.signs))
        section("Comment le détecter :", keep(card.detection))
        section("Comment s'en protéger :", keep(card.prevention))
        if (card.reaction != null) sb.appendLine(card.reaction)
        section("Bonnes pratiques :", keep(card.practices))
        section("Erreurs fréquentes :", keep(card.mistakes))
        sb.appendLine("À retenir : ${card.recap}")
        sb.append(card.tags)
        return sb.toString()
    }
}
