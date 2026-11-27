// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.mensan2024.itmentor.prototype

/** Textes d'exemple, un par domaine (les mêmes que ceux des tests du moteur). */
object Examples {
    val ALL: List<Pair<String, String>> = listOf(
        "Cyber · RDP" to """🔐Astuce cybersécurité 022 — Sécurise le Bureau à distance (RDP) !
Saviez-vous que votre ordinateur peut devenir une cible simplement parce que le Bureau à distance est mal configuré ?

Le protocole RDP (Remote Desktop Protocol) permet de prendre le contrôle d'un ordinateur à distance. 
⚠️ Les bonnes pratiques à adopter :
- Désactivez le RDP lorsqu'il n'est pas nécessaire.
- Limitez les utilisateurs autorisés à se connecter.
- Utilisez des mots de passe robustes.
- Activez l'authentification multifacteur (MFA).
- Évitez d'exposer directement le port RDP (3389) sur Internet.
#securiteinformatiquepourtous
#BonnesPratiques
#video24
#Iamnotperfect""".trimIndent(),
        "Réseaux · VLAN" to """Astuce réseau 031 — Sépare ton réseau avec des VLAN
Un VLAN (Virtual LAN) découpe un même switch en plusieurs réseaux logiques isolés : le trafic d'un VLAN ne voit pas celui des autres sans passer par un routeur.
Sur un switch Cisco :
- Crée le VLAN : vlan 10 puis name COMPTA
- Affecte le port : interface fa0/1, switchport mode access, switchport access vlan 10
- Configure le lien vers l'autre switch en trunk : switchport mode trunk
- Vérifie avec show vlan brief
À retenir : un VLAN par service, un trunk entre les switches, et un routeur ou un switch de niveau 3 pour faire communiquer les VLAN.
#CCNA #reseau""".trimIndent(),
        "Linux" to """5 commandes Linux à connaître pour diagnostiquer ton système
Quand une machine Linux ralentit, le terminal donne la réponse en quelques secondes.
Les commandes essentielles :
- top : voir en direct les processus qui consomment le processeur et la mémoire
- df -h : vérifier l'espace disque restant
- free -h : contrôler la mémoire vive disponible
- journalctl -xe : lire les derniers messages d'erreur du système
- systemctl status nom_du_service : savoir si un service tourne
À retenir : observe d'abord, puis agis. Ne tue jamais un processus sans savoir ce qu'il fait.""".trimIndent(),
        "IA · prompts" to """Écris de meilleurs prompts pour l'IA
Un modèle de langage comme ChatGPT, Claude ou Gemini répond mieux quand ta demande est précise.
La méthode en 4 points :
- Rôle : dis à l'IA qui elle doit être (formateur, développeur, recruteur)
- Contexte : explique ta situation et ton public
- Tâche : décris exactement le résultat attendu
- Format : précise la forme (liste, tableau, 100 mots)
❌ Écris-moi un texte sur la cybersécurité.
✅ Tu es formateur : écris 5 conseils de cybersécurité pour des étudiants, en liste courte.
À retenir : vérifie toujours les faits, l'IA peut se tromper avec assurance.""".trimIndent(),
        "Dev · Git" to """Astuce dev 012 — Le bon réflexe Git avant chaque modification
Travailler directement sur la branche main est la façon la plus rapide de casser un projet.
Le workflow simple :
- git pull pour récupérer la dernière version
- git switch -c ma-fonctionnalite pour créer une branche
- git add puis git commit -m avec un message clair
- git push et ouvre une pull request pour faire relire ton code
À retenir : une branche par fonctionnalité, des commits courts, une relecture avant de fusionner.
#dev #fullstack""".trimIndent(),
        "Santé" to """Astuce santé — 4 habitudes simples pour vivre en bonne santé plus longtemps
Pas besoin de changer toute ta vie : de petites habitudes répétées chaque jour font une vraie différence.
Les habitudes qui comptent :
- Bouge chaque jour : au moins 30 minutes de marche
- Dors suffisamment : 7 heures ou plus par nuit pour un adulte
- Mange plus de légumes et de fruits, moins de sucre et de produits très transformés
- Fais une pause écran toutes les heures et bois de l'eau régulièrement
À retenir : la régularité compte plus que l'intensité. Commence par une seule habitude cette semaine.
#santé""".trimIndent(),
        "Marketing" to """Rends tes publications LinkedIn plus visibles
Sur LinkedIn, les premières lignes décident si on clique sur « voir plus ».
Ce qui marche :
- Une accroche courte qui pose une question ou un problème
- Des paragraphes de 1 à 2 lignes, faciles à lire sur mobile
- Une seule idée par publication
- Un appel à l'action à la fin : commenter, partager, s'abonner
À retenir : publie régulièrement et réponds aux commentaires dans la première heure.
#marketing #LinkedIn""".trimIndent(),
        "English" to """English tip — When to use the present perfect
Use the present perfect when an action in the past is connected to the present.
How to use it:
- Experience : I have visited London twice.
- Result now : She has lost her keys, so she can't get in.
- Unfinished time : We have worked here since 2020.
- With just, already, yet : Have you finished yet?
❌ I have seen him yesterday.
✅ I saw him yesterday.
Remember: with a finished time like yesterday or last year, use the simple past.
#English""".trimIndent(),
    )
}
