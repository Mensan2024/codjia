// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.engine

import io.github.mensan2024.itmentor.engine.TechLevel.AVANCE
import io.github.mensan2024.itmentor.engine.TechLevel.INTERMEDIAIRE

/** Fiches de cybersécurité (D.1 : hameçonnage ; les autres sujets s'ajoutent ici, en données). */
internal object CardsSecurity {
    private val phishing = TopicCard(
        id = "phishing", kind = TopicKind.ATTACK,
        title = "Le phishing : le faux courriel qui vole ton mot de passe",
        names = listOf("phishing", "hameconnage", "hameconnage par courriel", "faux courriel", "faux mail", "mail frauduleux", "courriel frauduleux", "spear phishing", "whaling", "smishing"),
        hook = "Lundi matin, Awa ouvre sa boîte de réception. Un courriel de « sa banque » lui annonce que son compte sera suspendu sous 24 heures. Il lui demande de confirmer ses informations en cliquant sur un lien.",
        definition = "C'est du hameçonnage, ou phishing : un fraudeur imite un organisme de confiance pour te pousser à révéler tes identifiants ou tes données bancaires, ou à ouvrir un fichier piégé.",
        variants = listOf(
            Fact("Le spear phishing vise une personne précise avec un message personnalisé, le whaling vise les dirigeants, le smishing passe par SMS et le vishing par téléphone.", INTERMEDIAIRE),
        ),
        steps = listOf(
            Fact("Le fraudeur choisit une marque connue et copie son logo et son style."),
            Fact("Il envoie un message urgent : compte suspendu, colis bloqué ou facture impayée."),
            Fact("Le lien mène à une fausse page de connexion, hébergée sur une adresse qui ressemble à la vraie."),
            Fact("Tu saisis ton identifiant et ton mot de passe : ils sont envoyés directement au fraudeur."),
            Fact("Il se connecte à ton vrai compte, ou il revend tes identifiants."),
            Fact("Il peut aussi relayer en temps réel ton code de confirmation à la vraie page, et contourner ainsi une double authentification par code.", AVANCE),
        ),
        consequences = listOf(
            Fact("Ton compte est volé, avec toutes les données qu'il contient."),
            Fact("Des virements ou des achats frauduleux sont faits en ton nom."),
            Fact("Le fraudeur utilise ton compte pour piéger tes contacts ou entrer dans le réseau de ton entreprise."),
        ),
        signs = listOf(
            Fact("Un message urgent qui menace de suspendre ton compte"),
            Fact("Une adresse d'expéditeur qui ressemble à celle de la banque sans être la sienne"),
            Fact("Un lien dont l'adresse réelle ne correspond pas au texte affiché"),
            Fact("Une formule générique à la place de ton nom, ou une pièce jointe que tu n'attendais pas"),
        ),
        detection = listOf(
            Fact("Survole le lien avec ta souris : l'adresse réelle s'affiche, compare-la au site officiel."),
            Fact("Dans l'en-tête du message, un échec des contrôles SPF, DKIM ou DMARC signale une usurpation possible de l'expéditeur.", INTERMEDIAIRE),
            Fact("En entreprise, la passerelle de messagerie filtre les liens et les pièces jointes suspects, et un bouton de signalement prévient l'équipe sécurité.", AVANCE),
        ),
        prevention = listOf(
            Fact("Ne clique pas sur le lien : ouvre le site en tapant toi-même son adresse."),
            Fact("Appelle l'organisme avec le numéro officiel, jamais avec celui du message."),
            Fact("Active la double authentification : un second contrôle s'ajoute à ton mot de passe."),
            Fact("Préfère une clé de sécurité ou une passkey : elle refuse de s'authentifier sur un faux site.", INTERMEDIAIRE),
            Fact("Signale le courriel à ton service informatique, puis supprime-le."),
        ),
        reaction = "Si tu as déjà saisi ton mot de passe : change-le immédiatement sur le site officiel et préviens ta banque ou ton service informatique.",
        practices = listOf(
            Fact("Publie des enregistrements SPF, DKIM et DMARC pour ton domaine, et entraîne les équipes avec des simulations de phishing.", AVANCE),
        ),
        recap = "face à un message urgent qui demande tes identifiants, ne clique pas : vérifie par un canal officiel.",
    )

    val cards: List<TopicCard> = listOf(phishing)
}
