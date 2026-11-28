// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Phase D.1 — bibliothèque de connaissances informatiques et de cybersécurité.
 *
 * Chaque fiche est une DONNÉE (pas du code de moteur) : on enrichit la bibliothèque en ajoutant une fiche à [Knowledge.cards], sans toucher au
 * moteur vidéo. Les fiches contiennent uniquement des faits techniques vérifiables ; une adresse ou un nom d'exemple est toujours fictif
 * (domaines « .example » de la RFC 2606, adresses de documentation de la RFC 5737).
 */
enum class TechLevel(val label: String) { DEBUTANT("débutant"), INTERMEDIAIRE("intermédiaire"), AVANCE("avancé"), EXPERT("expert") }

enum class TopicKind(val label: String) { ATTACK("attaque"), CONCEPT("notion") }

/** Un fait de la fiche, montré à partir du niveau [from]. */
class Fact(val text: String, val from: TechLevel = TechLevel.DEBUTANT)

class TopicCard(
    val id: String,
    val kind: TopicKind,
    val title: String,
    /** Noms sous lesquels l'utilisateur peut désigner le sujet (sans accents, minuscules). */
    val names: List<String>,
    /** Mise en situation réaliste (personnage fictif) : montrée au niveau débutant et au-dessus. */
    val hook: String,
    val definition: String,
    val variants: List<Fact> = emptyList(),
    /** Déroulement étape par étape (attaque) ou fonctionnement (notion). */
    val steps: List<Fact>,
    val consequences: List<Fact> = emptyList(),
    val signs: List<Fact> = emptyList(),
    val detection: List<Fact> = emptyList(),
    val prevention: List<Fact>,
    val reaction: String? = null,
    val practices: List<Fact> = emptyList(),
    val recap: String,
    /** Étiquette de la rubrique « utilisation professionnelle » (notion) ou « erreurs fréquentes ». */
    val mistakes: List<Fact> = emptyList(),
    val tags: String = "#securiteinformatiquepourtous",
)

object Knowledge {
    val cards: List<TopicCard> by lazy { CardsSecurity.cards + CardsNetwork.cards }

    fun byId(id: String): TopicCard? = cards.firstOrNull { it.id == id }
}
