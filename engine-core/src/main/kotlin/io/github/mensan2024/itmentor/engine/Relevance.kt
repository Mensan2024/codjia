// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/**
 * Porte de validation sémantique (correction du 27/11/2026) : « le texte commande l'image ». Un format n'est proposé que si le texte
 * contient de quoi le justifier ; sinon il est écarté, même s'il est disponible et joli.
 *
 * Constat à l'origine : la famille « IA » recommandait en premier une démonstration 3D d'ordinateur portable éclaté pour une
 * « méthode en 4 points » (rôle, contexte, tâche, format), parce que le choix de l'objet suivait le domaine et non le passage.
 */
internal object SemanticGate {
    /** Mots qui désignent un objet que le volume 3D sait montrer : ordinateur et ses pièces, serveur et réseau physique, cadenas. */
    private val OBJECT = Regex("(?<![a-z0-9])(?:ordinateur|laptop|pc portable|\\bpc\\b|processeur|carte mere|clavier|batterie|materiel|serveurs?|datacenter|data center|baie|vlan|switch|routeur|commutateur|pare-?feu|firewall|cadenas|chiffrement|chiffrer|verrou|mots? de passe)")
    private val ATTACK = Regex("(?<![a-z0-9])(?:attaque|attaquant|pirate|hacker|intrusion|force brute|ddos|ransomware|rancongiciel|malware|phishing|hameconn|usurp|cybercrimin)")

    fun hasObject3D(normalized: String) = OBJECT.containsMatchIn(normalized)
    fun hasAttack(normalized: String) = ATTACK.containsMatchIn(normalized)

    /** Ce format est-il justifié par ce texte ? */
    fun allows(look: Look, p: ContentProfile): Boolean = when (look) {
        Look.SCENARIO -> p.concept != Concept.NONE
        Look.DEMO3D -> p.object3d
        Look.ATTAQUE -> p.attack3d && p.object3d
        else -> true
    }
}

/** Un défaut relevé par le contrôle qualité : la scène concernée (0 = première), la vérification et ce qui ne va pas. */
data class QcFinding(val scene: Int, val check: String, val message: String, val blocking: Boolean)

/**
 * Contrôle qualité automatique AVANT le rendu final (correction du 27/11/2026). Il ne juge pas le « beau » : il vérifie ce qui se
 * mesure — chaque image est-elle justifiée par son passage, un objet 3D est-il demandé par le texte, un plan est-il répété sans
 * raison, la durée suffit-elle à l'animation, les mots de l'auteur sont-ils tous conservés. Une scène en défaut bloquant n'est pas
 * corrigée à l'aveugle : le planificateur la laisse sans image (texte seul) plutôt que de la décorer.
 */
object QualityControl {
    fun audit(sb: Storyboard): List<QcFinding> {
        val profile = sb.profile ?: ContentAnalyzer.analyze(IntentAnalyzer.videoText(sb))
        val plan = ScenarioPlanner.plan(sb, profile)
        val out = ArrayList<QcFinding>()
        val seen = HashMap<String, Int>()
        for ((i, p) in plan.picks.withIndex()) {
            if (p == null) continue
            val it = plan.intents[i]
            val dur = sb.scenes[i].durationMs
            // 1. pertinence : la note du plan doit dépasser le seuil
            if (p.fit < SceneRegistry.MIN_FIT) out += QcFinding(i, "pertinence", "plan ${p.key} noté ${"%.2f".format(p.fit)} (< ${SceneRegistry.MIN_FIT})", true)
            // 2. objets : tout indice obligatoire du plan figure dans le passage
            val manque = p.beat.needs.filter { it !in plan.intents[i].cues }
            if (manque.isNotEmpty()) out += QcFinding(i, "objet", "plan ${p.key} montre des objets que le passage ne cite pas : $manque", true)
            // 3. concept : un plan ne sert que les concepts qu'il déclare
            val ok = it.concept in p.kind.concepts || p.kind.concepts.any { c -> c in it.secondary } || it.concept in p.kind.fallbackFor
            if (!ok) out += QcFinding(i, "concept", "plan ${p.key} (${p.kind.label}) pour le concept ${it.concept.label}", true)
            // 4. durée : l'animation a le temps de se dérouler
            if (dur < p.beat.minMs) out += QcFinding(i, "durée", "plan ${p.key} demande ${p.beat.minMs} ms, la scène en dure $dur", true)
            // 5. répétition : un plan non répétable ne sert qu'une fois
            val prev = seen[p.key]
            if (prev != null && !p.beat.repeatable) out += QcFinding(i, "répétition", "plan ${p.key} déjà utilisé à la scène ${prev + 1}", true)
            seen.putIfAbsent(p.key, i)
            // 6. sujet sensible : seulement des plans sobres
            if (profile.sensitive && !p.beat.calm) out += QcFinding(i, "sensibilité", "plan ${p.key} non sobre pour un sujet sensible", true)
        }
        // 7. conservation du texte : aucune scène vide, aucune ellipse ajoutée par un découpage
        for ((i, s) in sb.scenes.withIndex()) {
            if (s.title.isBlank() && s.body.isBlank()) out += QcFinding(i, "texte", "scène sans texte", true)
        }
        // 8. une scène sans image est signalée (information, non bloquante) : le texte seul est préférable à une décoration sans rapport
        for ((i, p) in plan.picks.withIndex()) if (p == null) out += QcFinding(i, "couverture", "aucune représentation assez pertinente : texte seul", false)
        return out
    }

    /** Vrai quand aucun défaut bloquant n'est relevé. */
    fun passes(sb: Storyboard): Boolean = audit(sb).none { it.blocking }

    fun report(sb: Storyboard): String {
        val f = audit(sb)
        if (f.isEmpty()) return "Contrôle qualité : aucun défaut."
        return f.joinToString("\n") { "Scène ${it.scene + 1} [${it.check}${if (it.blocking) ", BLOQUANT" else ""}] ${it.message}" }
    }
}
