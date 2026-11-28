// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import io.github.mensan2024.itmentor.engine.TechLevel.AVANCE
import io.github.mensan2024.itmentor.engine.TechLevel.EXPERT
import io.github.mensan2024.itmentor.engine.TechLevel.INTERMEDIAIRE

/** Fiches de réseaux et de systèmes (D.2) : routage OSPF, annuaire Active Directory, pare-feu. Des faits vérifiables, aucune commande inventée. */
internal object CardsNetwork {
    private val ospf = TopicCard(
        id = "ospf", kind = TopicKind.CONCEPT,
        title = "OSPF : comment les routeurs trouvent le meilleur chemin",
        names = listOf("ospf", "open shortest path first", "routage ospf", "protocole ospf", "routage dynamique", "ospf cisco"),
        hook = "Dans une entreprise de quatre sites, un câble est coupé un mardi matin. Pourtant personne ne perd sa connexion : les routeurs ont trouvé un autre chemin sans qu'un technicien intervienne.",
        definition = "OSPF, pour Open Shortest Path First, est un protocole de routage dynamique à état de liens : les routeurs d'un même réseau d'entreprise partagent la carte du réseau et calculent eux-mêmes le plus court chemin.",
        variants = listOf(Fact("OSPFv2 route l'IPv4 et OSPFv3 route l'IPv6 ; c'est un protocole ouvert, normalisé par l'IETF.", INTERMEDIAIRE)),
        steps = listOf(
            Fact("Chaque routeur envoie des paquets Hello pour découvrir ses voisins."),
            Fact("Deux voisins qui s'entendent forment une adjacence et synchronisent leurs informations."),
            Fact("Chaque routeur annonce l'état de ses liens dans des messages appelés LSA, diffusés à tout le réseau."),
            Fact("Tous les routeurs d'une même aire obtiennent ainsi la même carte du réseau."),
            Fact("Chacun calcule alors le plus court chemin vers chaque réseau, avec l'algorithme SPF de Dijkstra."),
            Fact("Le coût d'un lien dépend de sa bande passante : plus il est rapide, plus son coût est faible.", INTERMEDIAIRE),
            Fact("Les meilleurs chemins entrent dans la table de routage. Si un lien tombe, un nouveau LSA est diffusé et le calcul est refait."),
            Fact("Sur un réseau Ethernet partagé, un routeur désigné, le DR, et son remplaçant, le BDR, limitent le nombre d'adjacences.", INTERMEDIAIRE),
            Fact("Deux voisins doivent s'accorder sur l'intervalle Hello, le délai de mort, l'aire et le masque : par défaut sur Ethernet, 10 secondes et 40 secondes.", AVANCE),
            Fact("L'aire 0, le backbone, relie toutes les autres aires ; les routeurs de bordure d'aire, les ABR, résument les routes entre elles.", AVANCE),
        ),
        consequences = listOf(
            Fact("Les routes se découvrent et se mettent à jour sans configuration manuelle réseau par réseau."),
            Fact("Quand un lien tombe, les routeurs recalculent un autre chemin sans intervention humaine."),
            Fact("En découpant le réseau en aires, on garde des tables de routage raisonnables même sur un grand réseau.", INTERMEDIAIRE),
        ),
        practices = listOf(
            Fact("Garde l'aire 0 continue, résume les routes aux routeurs de bordure et authentifie les échanges OSPF.", AVANCE),
            Fact("Rends passives les interfaces vers les postes : aucun routeur n'y existe, donc aucun message OSPF n'a à y circuler.", AVANCE),
            Fact("Selon l'équipement, la bande passante de référence par défaut est de 100 Mbit/s : ajuste-la si tes liens sont plus rapides, sinon des liens différents obtiennent le même coût.", EXPERT),
        ),
        mistakes = listOf(
            Fact("Des intervalles Hello, une aire ou un masque différents entre deux voisins : l'adjacence ne se forme pas.", INTERMEDIAIRE),
            Fact("Une MTU différente des deux côtés peut bloquer l'adjacence pendant l'échange des bases.", AVANCE),
        ),
        prevention = emptyList(),
        recap = "chaque routeur connaît la carte du réseau et calcule lui-même le plus court chemin.",
        tags = "#reseaux #ccna #ccnp #ospf",
    )

    private val ad = TopicCard(
        id = "active-directory", kind = TopicKind.CONCEPT,
        title = "Active Directory : l'annuaire qui gère les comptes de l'entreprise",
        names = listOf("active directory", "annuaire active directory", "ad ds", "controleur de domaine", "domaine windows", "domaine active directory"),
        hook = "Dans une entreprise de deux cents personnes, un nouvel employé arrive lundi. En quelques clics, l'informatique lui crée un compte : il peut ouvrir une session sur n'importe quel poste et accéder aux dossiers de son équipe.",
        definition = "Active Directory est l'annuaire de Microsoft pour les réseaux Windows : il centralise les comptes, les groupes et les ordinateurs d'un domaine, et décide qui peut se connecter et accéder à quoi.",
        steps = listOf(
            Fact("Le domaine regroupe les comptes d'utilisateurs, d'ordinateurs et de groupes dans une base commune, l'annuaire."),
            Fact("Cette base est hébergée par des contrôleurs de domaine, qui se la copient entre eux : c'est la réplication."),
            Fact("Les objets sont rangés dans des unités d'organisation, qui servent à déléguer l'administration et à appliquer les stratégies de groupe."),
            Fact("À l'ouverture de session, le poste prouve l'identité de l'utilisateur au contrôleur de domaine, qui répond par un ticket Kerberos."),
            Fact("Avec ce ticket, l'utilisateur obtient des tickets de service pour accéder aux dossiers partagés ou aux applications, sans retaper son mot de passe."),
            Fact("Les utilisateurs sont placés dans des groupes, et ce sont les groupes qui reçoivent les permissions sur les dossiers.", INTERMEDIAIRE),
            Fact("L'annuaire s'appuie sur le DNS : les postes retrouvent les contrôleurs de domaine grâce aux enregistrements SRV.", AVANCE),
            Fact("Une forêt peut contenir plusieurs domaines liés par des relations d'approbation, et cinq rôles FSMO se répartissent des tâches uniques dans la forêt et le domaine.", EXPERT),
        ),
        consequences = listOf(
            Fact("Une seule identité pour se connecter à tous les postes et services de l'entreprise."),
            Fact("Les droits sont donnés à des groupes plutôt qu'à chaque personne."),
            Fact("Les règles de sécurité et de configuration s'appliquent à tous les postes d'un coup, grâce aux stratégies de groupe."),
        ),
        practices = listOf(
            Fact("Applique le moindre privilège et sépare les comptes d'administration des comptes de tous les jours.", AVANCE),
            Fact("Sauvegarde l'état du système des contrôleurs de domaine et protège-les, physiquement et par le réseau.", AVANCE),
            Fact("Surveille les connexions des comptes privilégiés et les modifications des groupes sensibles.", EXPERT),
        ),
        mistakes = listOf(
            Fact("Donner les droits d'administrateur du domaine à des comptes utilisés au quotidien.", INTERMEDIAIRE),
            Fact("N'avoir qu'un seul contrôleur de domaine : s'il tombe, plus personne ne peut s'authentifier sur le domaine.", INTERMEDIAIRE),
            Fact("Un DNS mal configuré : un poste qui ne trouve pas son contrôleur de domaine ne peut ni rejoindre le domaine ni ouvrir de session.", AVANCE),
        ),
        prevention = emptyList(),
        recap = "Active Directory centralise les identités : un compte, des groupes et des règles appliquées à tout le parc.",
        tags = "#windowsserver #activedirectory",
    )

    private val firewall = TopicCard(
        id = "firewall", kind = TopicKind.CONCEPT,
        title = "Le pare-feu : le contrôleur qui filtre chaque paquet",
        names = listOf("pare-feu", "pare feu", "parefeu", "firewall"),
        hook = "À l'entrée d'une entreprise, un agent vérifie chaque visiteur : qui es-tu, où vas-tu, as-tu le droit d'entrer ? Dans un réseau, le pare-feu joue ce rôle pour chaque paquet de données.",
        definition = "Un pare-feu est un équipement ou un logiciel qui filtre le trafic entre deux réseaux selon des règles : il autorise certains paquets et bloque les autres.",
        steps = listOf(
            Fact("Le pare-feu relie des zones : le réseau interne, Internet et souvent une zone intermédiaire pour les serveurs publics, la DMZ."),
            Fact("Il lit chaque paquet : adresses source et destination, protocole et port."),
            Fact("Il compare le paquet aux règles, dans l'ordre, de haut en bas."),
            Fact("La première règle qui correspond s'applique : autoriser ou bloquer."),
            Fact("Si aucune règle ne correspond, la règle par défaut s'applique, et elle devrait refuser."),
            Fact("Un pare-feu à états suit les connexions : la réponse à une requête sortante est autorisée automatiquement, pas une connexion entrante que personne n'a demandée.", INTERMEDIAIRE),
            Fact("Les pare-feu de nouvelle génération ajoutent l'identification des applications, la prévention d'intrusion et l'inspection du trafic chiffré.", AVANCE),
        ),
        consequences = listOf(
            Fact("Il protège le réseau interne contre les connexions venues d'Internet que personne n'a sollicitées."),
            Fact("Il limite ce que chaque zone peut atteindre : les invités ne voient pas les serveurs."),
            Fact("Il garde une trace des connexions autorisées et refusées."),
        ),
        practices = listOf(
            Fact("Applique le moindre privilège : n'ouvre que ce qui est nécessaire et documente chaque règle.", AVANCE),
            Fact("Révise régulièrement les règles et supprime celles qui ne servent plus.", AVANCE),
            Fact("Journalise les refus et surveille-les : ils révèlent les scans et les tentatives d'intrusion.", EXPERT),
        ),
        mistakes = listOf(
            Fact("Une règle trop large, comme tout autoriser, rend les autres inutiles."),
            Fact("L'ordre des règles : une règle générale placée trop haut masque les règles précises.", INTERMEDIAIRE),
            Fact("Ouvrir un port « temporairement » et l'oublier.", AVANCE),
        ),
        prevention = emptyList(),
        recap = "un pare-feu applique des règles claires : on autorise le nécessaire et on bloque le reste.",
        tags = "#parefeu #reseaux #cybersecurite",
    )

    val cards: List<TopicCard> = listOf(ospf, ad, firewall)
}
