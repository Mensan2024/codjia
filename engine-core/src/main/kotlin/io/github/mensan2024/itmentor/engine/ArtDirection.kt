// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

/** Ambiance générale : elle filtre les palettes et les effets des formats à style modulable. */
enum class Mood { ANY, CALM, WARM, ENERGETIC, TECH }

/**
 * Une direction artistique : un format, une ambiance, une musique, un rythme, et la raison du choix.
 * Les directions viennent de la famille de contenu ([ContentFamily]) et de son profil ([ContentProfile]) :
 * un texte biblique ou de santé mentale n'obtient jamais « Flash info » ou « Terminal », quelle que soit la graine.
 */
data class Direction(
    val id: String,
    val name: String,
    val look: Look,
    val mood: Mood,
    val music: List<String>,
    /** 1 = très posé, 3 = très dynamique. */
    val intensity: Int,
    val why: String,
)

object ArtDirector {
    /** Formats exclus pour les sujets sensibles : clignotements, alertes, jargon visuel, effets bruyants. */
    private val SENSITIVE_BANNED = setOf(Look.FLASH, Look.JOURNAL, Look.TERMINAL, Look.BD, Look.NEON, Look.DEMO, Look.MONTAGE, Look.KIT, Look.COLLAGE, Look.DEMONSTRATION, Look.DEMO3D, Look.ATTAQUE)

    private fun d(id: String, name: String, look: Look, mood: Mood, music: List<String>, intensity: Int, why: String) =
        Direction(id, name, look, mood, music, intensity, why)

    /** Toutes les directions de chaque famille (les trois premières sont recommandées). */
    private val CATALOG: Map<ContentFamily, List<Direction>> = mapOf(
        ContentFamily.MENTAL to listOf(
            d("mental-scenario", "Scénario apaisant", Look.SCENARIO, Mood.CALM, listOf("lofi", "cinematic"), 1, "chaque geste a son image : respiration, marche, parole, calme sans à-coup"),
            d("mental-serenite", "Sérénité", Look.SERENITE, Mood.CALM, listOf("lofi", "cinematic"), 1, "cercle qui respire, texte centré, aucun mouvement brusque"),
            d("mental-editorial", "Apaisant éditorial", Look.EDITORIAL, Mood.CALM, listOf("lofi", "cinematic"), 1, "mise en page sobre, texte posé, sans effet agressif"),
            d("mental-nuit", "Récit nocturne", Look.CINEMA, Mood.CALM, listOf("cinematic", "lofi"), 1, "ambiance douce et intime, adaptée à un sujet personnel"),
            d("mental-tableau", "Tableau pédagogique doux", Look.TABLEAU, Mood.CALM, listOf("lofi", "cinematic"), 1, "une main qui explique calmement, sans surcharge"),
            d("mental-mascotte", "Compagnon bienveillant", Look.MASCOTTE, Mood.CALM, listOf("lofi"), 1, "un personnage qui accompagne, ton rassurant"),
        ),
        ContentFamily.BIBLE to listOf(
            d("bible-lumiere", "Lumière", Look.LUMIERE, Mood.WARM, listOf("cinematic", "lofi"), 1, "étoile de lumière, tons chauds, texte centré : recueillement"),
            d("bible-contemplatif", "Contemplatif", Look.CINEMA, Mood.WARM, listOf("cinematic", "lofi"), 1, "serif italique et nuit étoilée : recueillement"),
            d("bible-sobre", "Sobre et élégant", Look.EDITORIAL, Mood.WARM, listOf("cinematic", "lofi"), 1, "typographie de livre, le texte reste au centre"),
            d("bible-enseignement", "Enseignement au tableau", Look.TABLEAU, Mood.WARM, listOf("lofi", "cinematic"), 1, "idéal pour expliquer un passage point par point"),
        ),
        ContentFamily.PERSONAL to listOf(
            d("perso-scenario", "Scénario de gratitude", Look.SCENARIO, Mood.WARM, listOf("cinematic", "lofi", "afrobeat"), 2, "carnet, plante qui pousse, merci partagé : lumière chaude"),
            d("perso-elan", "Élan", Look.ELAN, Mood.ENERGETIC, listOf("cinematic", "hiphop", "afrobeat"), 2, "soleil levant et chaîne de progression : énergie posée, sans flash"),
            d("perso-montage", "Élan dynamique", Look.MONTAGE, Mood.ENERGETIC, listOf("hiphop", "future", "afrobeat"), 3, "chapitres numérotés et sous-titres mot à mot : progression visible"),
            d("perso-neon", "Affiche motivation", Look.NEON, Mood.ENERGETIC, listOf("future", "hiphop", "edm"), 3, "titres percutants, rythme soutenu"),
            d("perso-collage", "Collage inspirant", Look.COLLAGE, Mood.WARM, listOf("afrobeat", "amapiano", "cinematic"), 2, "cartes en papier, étiquettes, côté humain"),
            d("perso-kit", "Style unique", Look.KIT, Mood.ENERGETIC, listOf("afrobeat", "future", "hiphop"), 3, "une combinaison nouvelle à chaque version"),
            d("perso-cinema", "Cinéma inspirant", Look.CINEMA, Mood.WARM, listOf("cinematic"), 2, "ambiance de film pour un message fort"),
        ),
        ContentFamily.MARKETING to listOf(
            d("market-scenario", "Scénario du tunnel", Look.SCENARIO, Mood.TECH, listOf("future", "house", "afrobeat"), 2, "les visiteurs entrent, se perdent ou deviennent clients : le tunnel en action"),
            d("market-strategie", "Tableau de bord et tunnel", Look.STRATEGIE, Mood.TECH, listOf("future", "house", "afrobeat"), 2, "les étapes deviennent un tunnel de conversion, courbe de tendance décorative"),
            d("market-neon", "Pub dynamique", Look.NEON, Mood.ENERGETIC, listOf("afrobeat", "edm", "future"), 3, "accroches fortes, sous-titres mot à mot"),
            d("market-data", "Données et tunnel", Look.INFOGRAPHIE, Mood.TECH, listOf("future", "house", "afrobeat"), 2, "schémas animés pour montrer la stratégie"),
            d("market-journal", "Journal business", Look.JOURNAL, Mood.ENERGETIC, listOf("edm", "house"), 2, "bandeau d'actualité : ton professionnel"),
            d("market-montage", "Montage dynamique", Look.MONTAGE, Mood.ENERGETIC, listOf("afrobeat", "amapiano", "dembow"), 3, "rythme réseaux sociaux"),
            d("market-collage", "Collage commercial", Look.COLLAGE, Mood.WARM, listOf("amapiano", "afrobeat"), 2, "cartes et étiquettes"),
            d("market-flash", "Flash promo", Look.FLASH, Mood.ENERGETIC, listOf("edm", "trap"), 3, "texte géant, effet d'annonce"),
        ),
        ContentFamily.NETWORK to listOf(
            d("net-scenario", "Scénario réseau", Look.SCENARIO, Mood.TECH, listOf("synthwave", "future", "house"), 2, "topologie, VLAN, trunk, pare-feu : chaque idée a son schéma animé"),
            d("net-3d", "Démonstration 3D", Look.DEMO3D, Mood.TECH, listOf("synthwave", "future", "house"), 2, "baie de serveurs en 3D qui s'éclate, repères et lumière"),
            d("net-demo", "Simulation à l'écran", Look.DEMO, Mood.TECH, listOf("synthwave", "house", "edm"), 2, "terminal et fenêtres comme sur un vrai poste"),
            d("net-infographie", "Schémas animés", Look.INFOGRAPHIE, Mood.TECH, listOf("synthwave", "future", "house"), 2, "un schéma par idée : topologies, paquets, VLAN"),
            d("net-cours", "Cours explicatif", Look.EXPLICATIF, Mood.TECH, listOf("synthwave", "house"), 2, "chapitres numérotés et simulation d'interface"),
            d("net-tableau", "Tableau technique", Look.TABLEAU, Mood.TECH, listOf("lofi", "house"), 1, "dessin pas à pas"),
            d("net-terminal", "Terminal", Look.TERMINAL, Mood.TECH, listOf("synthwave", "trap"), 2, "commandes tapées une à une"),
            d("net-bas", "Explicatif carte dessous", Look.EXPLIC_BAS, Mood.TECH, listOf("synthwave", "house"), 2, "simulation en haut, explication en bas"),
        ),
        ContentFamily.DEV to listOf(
            d("dev-scenario", "Scénario client-serveur", Look.SCENARIO, Mood.TECH, listOf("lofi", "synthwave", "future"), 2, "la requête part, le serveur répond : le flux en images"),
            d("dev-terminal", "Terminal et code", Look.TERMINAL, Mood.TECH, listOf("hiphop", "lofi", "synthwave"), 2, "le code et les commandes apparaissent tapés"),
            d("dev-3d", "Démonstration 3D", Look.DEMO3D, Mood.TECH, listOf("lofi", "synthwave", "future"), 2, "ordinateur en 3D qui s'éclate en pièces"),
            d("dev-demo", "Démo d'interface", Look.DEMO, Mood.TECH, listOf("lofi", "synthwave"), 2, "fenêtres et clics comme sur un vrai poste"),
            d("dev-infographie", "Schémas animés", Look.INFOGRAPHIE, Mood.TECH, listOf("future", "house"), 2, "éditeur de code, flux client-serveur"),
            d("dev-cours", "Cours explicatif", Look.EXPLICATIF, Mood.TECH, listOf("lofi", "synthwave"), 2, "chapitres et simulation"),
            d("dev-tableau", "Tableau", Look.TABLEAU, Mood.TECH, listOf("lofi"), 1, "schémas d'architecture dessinés"),
        ),
        ContentFamily.CYBER to listOf(
            d("cyber-scenario", "Scénario d'attaque et de parade", Look.SCENARIO, Mood.TECH, listOf("trap", "phonk", "cinematic"), 2, "le leurre, le mécanisme, puis chaque réflexe de défense en image"),
            d("cyber-attaque3d", "Attaque et défense 3D", Look.ATTAQUE, Mood.TECH, listOf("trap", "phonk", "cinematic"), 2, "un attaquant, une cible et des paquets en 3D, puis les boucliers de défense descendent"),
            d("cyber-3d", "Démonstration 3D", Look.DEMO3D, Mood.TECH, listOf("trap", "cinematic", "edm"), 2, "cadenas ou serveurs en 3D, vue éclatée"),
            d("cyber-demo", "Démonstration de l'attaque", Look.DEMO, Mood.TECH, listOf("trap", "phonk", "cinematic"), 2, "montrer le piège et la parade à l'écran"),
            d("cyber-infographie", "Schémas animés", Look.INFOGRAPHIE, Mood.TECH, listOf("trap", "cinematic", "edm"), 2, "attaque, protection, étapes de défense"),
            d("cyber-alerte", "Alerte info", Look.FLASH, Mood.ENERGETIC, listOf("trap", "drill"), 3, "ton d'alerte pour un risque réel"),
            d("cyber-journal", "Journal télévisé", Look.JOURNAL, Mood.TECH, listOf("cinematic", "edm"), 2, "présentation factuelle"),
            d("cyber-terminal", "Terminal", Look.TERMINAL, Mood.TECH, listOf("trap", "phonk"), 2, "commandes de défense"),
            d("cyber-cours", "Cours explicatif", Look.EXPLICATIF, Mood.TECH, listOf("cinematic", "synthwave"), 2, "chapitres numérotés"),
        ),
        ContentFamily.AI to listOf(
            d("ai-scenario", "Scénario visuel", Look.SCENARIO, Mood.TECH, listOf("future", "lofi"), 2, "chaque idée a son schéma : méthode en étapes, requête, réponse"),
            d("ai-3d", "Démonstration 3D", Look.DEMO3D, Mood.TECH, listOf("future", "edm"), 2, "ordinateur en 3D qui s'éclate en pièces"),
            d("ai-infographie", "Schémas animés", Look.INFOGRAPHIE, Mood.TECH, listOf("future", "edm"), 2, "prompt, données, réponse : le flux en images"),
            d("ai-cours", "Cours explicatif", Look.EXPLICATIF, Mood.TECH, listOf("future", "synthwave"), 2, "chapitres numérotés"),
            d("ai-kit", "Style unique", Look.KIT, Mood.TECH, listOf("future", "synthwave"), 3, "nouvelle combinaison à chaque version"),
            d("ai-montage", "Montage dynamique", Look.MONTAGE, Mood.ENERGETIC, listOf("future", "edm"), 3, "rythme soutenu"),
        ),
        ContentFamily.HEALTH to listOf(
            d("health-scenario", "Scénario visuel", Look.SCENARIO, Mood.CALM, listOf("lofi", "afrobeat"), 1, "des personnes, des gestes et leurs conséquences, sans chiffre inventé"),
            d("health-infographie", "Habitudes en schémas", Look.INFOGRAPHIE, Mood.CALM, listOf("lofi", "afrobeat", "house"), 2, "une habitude par schéma"),
            d("health-editorial", "Éditorial chaleureux", Look.EDITORIAL, Mood.WARM, listOf("lofi", "afrobeat"), 1, "mise en page de magazine santé"),
            d("health-mascotte", "Personnage animé", Look.MASCOTTE, Mood.WARM, listOf("afrobeat", "amapiano"), 2, "ton chaleureux"),
            d("health-tableau", "Tableau", Look.TABLEAU, Mood.CALM, listOf("lofi"), 1, "explication dessinée"),
        ),
        ContentFamily.ENGLISH to listOf(
            d("en-tableau", "Tableau de classe", Look.TABLEAU, Mood.WARM, listOf("lofi", "amapiano"), 1, "exemples écrits à la main"),
            d("en-bd", "Bande dessinée", Look.BD, Mood.WARM, listOf("amapiano", "hiphop"), 2, "dialogues en bulles"),
            d("en-mascotte", "Personnage animé", Look.MASCOTTE, Mood.WARM, listOf("amapiano", "afrobeat"), 2, "un compagnon qui répète"),
            d("en-cours", "Cours explicatif", Look.EXPLICATIF, Mood.ANY, listOf("lofi", "hiphop"), 2, "chapitres numérotés"),
        ),
        ContentFamily.GENERAL to listOf(
            d("gen-scenario", "Scénario visuel", Look.SCENARIO, Mood.ANY, listOf("lofi", "cinematic", "afrobeat"), 2, "le texte commande l'image : méthode, personnes, lieux et conséquences montrés tels qu'écrits"),
            d("gen-montage", "Montage dynamique", Look.MONTAGE, Mood.ANY, listOf("afrobeat", "edm", "amapiano"), 3, "polyvalent"),
            d("gen-cours", "Cours explicatif", Look.EXPLICATIF, Mood.ANY, listOf("lofi", "afrobeat"), 2, "chapitres clairs"),
            d("gen-infographie", "Schémas animés", Look.INFOGRAPHIE, Mood.ANY, listOf("house", "future"), 2, "une idée, un schéma"),
            d("gen-editorial", "Éditorial chic", Look.EDITORIAL, Mood.ANY, listOf("lofi", "cinematic"), 1, "sobre et lisible"),
            d("gen-kit", "Style unique", Look.KIT, Mood.ANY, listOf("afrobeat", "future"), 3, "combinaison nouvelle à chaque version"),
        ),
    )

    /** Toutes les directions admissibles pour ce profil, la meilleure en premier. */
    fun all(profile: ContentProfile): List<Direction> {
        // Le « Scénario visuel » n'est proposé que si le moteur reconnaît un concept à montrer : sinon il laisserait des scènes sans image.
        val base = CATALOG.getValue(profile.family).filter { SemanticGate.allows(it.look, profile) }
        val own = if (profile.sensitive) base.filter { it.look !in SENSITIVE_BANNED } else base
        // Contenu ambigu : on complète avec les directions de la famille voisine, jamais avec des formats exclus.
        val extra = if (profile.ambiguous) profile.alternatives.flatMap { CATALOG.getValue(it) }.filter { (!profile.sensitive || it.look !in SENSITIVE_BANNED) && SemanticGate.allows(it.look, profile) } else emptyList()
        // Un ton énergique ou urgent monte le rythme, un ton calme le baisse : on classe par proximité.
        val want = when (profile.tone) { Tone.CALME, Tone.CONTEMPLATIF -> 1; Tone.ENERGIQUE, Tone.URGENT -> 3; else -> 2 }
        return (own + extra).distinctBy { it.look }
            // Le scénario visuel, quand le moteur sait quoi montrer, passe devant : c'est la mise en scène qui suit le sens du texte.
            .sortedBy { if (it.look == Look.SCENARIO) -1 else kotlin.math.abs(it.intensity - want) * 10 + (own + extra).indexOf(it) }
    }

    /** Les trois directions proposées à l'utilisateur (mode « Recommandé »). Jamais deux fois le même format. */
    fun recommend(profile: ContentProfile): List<Direction> = all(profile).take(3)

    /**
     * Mode « Automatique » : la meilleure direction, mais « Autre version » change de direction
     * (on évite les formats des dernières générations) sans jamais sortir des directions admissibles.
     */
    fun auto(profile: ContentProfile, seed: Long, avoidLooks: Collection<Look> = emptyList()): Direction {
        // Tirage parmi les cinq premières directions admissibles (la recommandation reste sur trois) : assez de variété d'une génération à l'autre.
        val top = all(profile).take(7)
        // Première génération (aucun format récent à éviter) : la meilleure direction pour ce contenu. « Autre version » varie ensuite.
        if (avoidLooks.isEmpty()) return top.first()
        val fresh = top.filter { it.look !in avoidLooks }.ifEmpty { top }
        return fresh[(Rand.of(seed, 0x1B5E).nextInt() ushr 1) % fresh.size]
    }

    /** Direction pour un format imposé par l'utilisateur (mode « Manuel ») : ambiance de la famille, format libre. */
    fun manual(profile: ContentProfile, look: Look): Direction =
        all(profile).firstOrNull { it.look == look }
            ?: CATALOG.getValue(profile.family).first().copy(id = "manuel-${look.name.lowercase()}", name = look.label, look = look, why = "format choisi par toi")
}
