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

    private val ransomware = TopicCard(
        id = "ransomware", kind = TopicKind.ATTACK,
        title = "Le rançongiciel : l'attaque qui chiffre tes fichiers",
        names = listOf("ransomware", "rancongiciel", "rancon informatique", "logiciel de rancon", "rancon"),
        hook = "Un mardi matin, Moussa, comptable dans une petite entreprise, ouvre une facture reçue par courriel. Quelques minutes plus tard, ses fichiers ne s'ouvrent plus et un message réclame de l'argent.",
        definition = "Un rançongiciel, ou ransomware, est un logiciel malveillant qui chiffre tes fichiers et exige une rançon pour les déchiffrer.",
        variants = listOf(Fact("La double extorsion ajoute le vol des données avant le chiffrement : l'attaquant menace de les publier, même si la victime restaure ses sauvegardes.", INTERMEDIAIRE)),
        steps = listOf(
            Fact("L'entrée se fait souvent par un courriel piégé, un accès à distance mal protégé ou une faille non corrigée."),
            Fact("Le programme malveillant s'installe et se cache sur le poste."),
            Fact("Il explore le réseau et cherche les dossiers partagés et les sauvegardes accessibles."),
            Fact("Il chiffre les fichiers un à un avec une clé que seul l'attaquant possède."),
            Fact("Un message de rançon s'affiche et demande un paiement, souvent avec un délai."),
            Fact("Avant de chiffrer, les attaquants cherchent souvent à obtenir des droits d'administrateur, à désactiver l'antivirus et à supprimer les sauvegardes accessibles.", AVANCE),
        ),
        consequences = listOf(
            Fact("L'activité s'arrête : fichiers, applications et serveurs deviennent inutilisables."),
            Fact("Des données peuvent être volées, puis publiées."),
            Fact("Payer ne garantit pas de récupérer les fichiers et finance les criminels."),
        ),
        signs = listOf(
            Fact("Des fichiers qui changent d'extension ou ne s'ouvrent plus"),
            Fact("Un message qui réclame une rançon"),
            Fact("Un poste très lent, avec une activité disque inhabituelle"),
            Fact("Des sauvegardes ou des partages soudainement inaccessibles"),
        ),
        detection = listOf(
            Fact("Les outils de détection sur les postes, les EDR, repèrent le chiffrement massif de fichiers et peuvent isoler le poste.", INTERMEDIAIRE),
            Fact("Les journaux montrent un grand nombre de fichiers renommés ou modifiés en peu de temps, et des connexions inhabituelles vers les serveurs de fichiers.", AVANCE),
        ),
        prevention = listOf(
            Fact("Garde des sauvegardes hors ligne ou immuables, et teste leur restauration."),
            Fact("Mets à jour le système et les logiciels dès que les correctifs sortent."),
            Fact("Méfie-toi des pièces jointes et des liens inattendus."),
            Fact("Limite les droits : un compte ordinaire ne doit pas pouvoir modifier tous les partages."),
            Fact("Active la double authentification sur les accès à distance.", INTERMEDIAIRE),
            Fact("Segmente le réseau pour que l'infection ne se propage pas d'un poste à tout le parc.", AVANCE),
        ),
        reaction = "Si tu vois le message de rançon : débranche le poste du réseau, préviens tout de suite ton service informatique, et ne paie pas avant d'avoir l'avis d'experts.",
        practices = listOf(
            Fact("Applique la règle 3-2-1 : trois copies, sur deux supports, dont une hors ligne.", AVANCE),
            Fact("Prépare et entraîne un plan de réponse aux incidents, avec les contacts à prévenir.", AVANCE),
        ),
        recap = "des sauvegardes hors ligne, testées, sont la meilleure réponse à un rançongiciel.",
    )

    val cards: List<TopicCard> = listOf(phishing, ransomware)
}
