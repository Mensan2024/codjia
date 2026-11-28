# Audit de pertinence

## 1.1 `s-hero` · HERO · 6.2 s
- AFFICHÉ : ASTUCE CYBERSÉCURITÉ ⏎ Le rançongiciel : l'attaque qui chiffre tes fichiers ⏎ Un mardi matin, Moussa, comptable dans une petite entreprise, ouvre une facture reçue par courriel.
- DIT : Le rançongiciel : l'attaque qui chiffre tes fichiers. Un mardi matin, Moussa, comptable dans une petite entreprise, ouvre une facture reçue par courriel.
- sujet/rôle : Logiciel malveillant ou attaque technique · ouverture ; indices du texte : act:CHIFFRER, act:ENVOYER, act:MESURER, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, topic:attachment, topic:cabling, topic:inbox, topic:measure, topic:office, topic:pay, topic:ransom
- PLAN : net3d-ransom/poster (Affiche : un poste dont les fichiers se verrouillent) note 1.00 ; dessine : ransom
- VERDICT : ok : le texte cite ransom

## 1.2 `s-def` · DEFINITION · 9.2 s
- AFFICHÉ : Quelques minutes plus tard, ses fichiers ne s'ouvrent plus et un message réclame de l'argent.
- DIT : Quelques minutes plus tard, ses fichiers ne s'ouvrent plus et un message réclame de l'argent.
- sujet/rôle : Logiciel malveillant ou attaque technique · définition ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, topic:attachment, topic:ransom-note
- PLAN : net3d-ransom/ransom-note (Le message de rançon et son compte à rebours) note 1.00 ; dessine : ransom-note
- VERDICT : ok : le texte cite ransom-note

## 1.3 `s-def0-1` · DEFINITION · 10.7 s
- AFFICHÉ : Un rançongiciel, ou ransomware, est un logiciel malveillant qui chiffre tes fichiers et exige une rançon pour les déchiffrer.
- DIT : Un rançongiciel, ou ransomware, est un logiciel malveillant qui chiffre tes fichiers et exige une rançon pour les déchiffrer.
- sujet/rôle : Logiciel malveillant ou attaque technique · définition ; indices du texte : act:CHIFFRER, act:MESURER, actor:ATTAQUANT, thing:ATTAQUE, thing:EMAIL, topic:attachment, topic:measure, topic:ransom
- PLAN : net3d-ransom/ransom-overview (Un poste infecté, des fichiers qui se verrouillent) note 1.00 ; dessine : ransom
- VERDICT : ok : le texte cite ransom

## 1.4 `s-def0-2` · DEFINITION · 12.2 s
- AFFICHÉ : La double extorsion ajoute le vol des données avant le chiffrement : l'attaquant menace de les publier, même si la victime restaure ses sauvegardes.
- DIT : La double extorsion ajoute le vol des données avant le chiffrement : l'attaquant menace de les publier, même si la victime restaure ses sauvegardes.
- sujet/rôle : Logiciel malveillant ou attaque technique · définition ; indices du texte : act:CHIFFRER, act:MESURER, actor:ATTAQUANT, actor:UTILISATEUR, thing:ATTAQUE, thing:DONNEES, thing:EMAIL, thing:OBJET_3D, topic:flow, topic:measure, topic:ransom, topic:ransom-leak, topic:urgency
- PLAN : net3d-ransom/ransom-leak (Double extorsion : les données partent avant le chiffrement) note 1.00 ; dessine : ransom-leak
- VERDICT : ok : le texte cite ransom-leak

## 2.1 `ep2-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 2/4 ⏎ Le rançongiciel : l'attaque qui chiffre tes fichiers
- DIT : Le rançongiciel : l'attaque qui chiffre tes fichiers.
- sujet/rôle : Logiciel malveillant ou attaque technique · ouverture ; indices du texte : act:CHIFFRER, act:MESURER, thing:ATTAQUE, thing:EMAIL, topic:attachment, topic:measure, topic:ransom
- PLAN : net3d-ransom/poster (Affiche : un poste dont les fichiers se verrouillent) note 1.00 ; dessine : ransom
- VERDICT : ok : le texte cite ransom ; déjà montré en 1.1

## 2.2 `s-steps0` · STEPS · 9.0 s
- AFFICHÉ : Comment l'attaque se déroule (1/3) ⏎ • L'entrée se fait souvent par un courriel piégé : un accès à distance mal protégé ou une faille non corrigée ⏎ • Le programme malveillant s'installe et se cache sur le poste
- DIT : Comment l'attaque se déroule (1/3). L'entrée se fait souvent par un courriel piégé : un accès à distance mal protégé ou une faille non corrigée. Le programme malveillant s'installe et se cache sur le poste.
- sujet/rôle : Logiciel malveillant ou attaque technique · conseil, parade ; indices du texte : act:PUISER, actor:ATTAQUANT, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:ORDINATEUR, topic:attract, topic:chain, topic:inbox, topic:office, topic:ransom-entry, topic:vpn
- PLAN : net3d-ransom/ransom-entry (Trois portes d'entrée : courriel, accès à distance, faille) note 0.93 ; dessine : ransom-entry
- VERDICT : ok : le texte cite ransom-entry

## 2.3 `s-steps1-1` · STEPS · 9.0 s
- AFFICHÉ : Comment l'attaque se déroule (2/3) ⏎ • Il explore le réseau et cherche les dossiers partagés et les sauvegardes accessibles ⏎ • Il chiffre les fichiers un à un : avec une clé que seul l'attaquant possède
- DIT : Comment l'attaque se déroule (2/3). Il explore le réseau et cherche les dossiers partagés et les sauvegardes accessibles. Il chiffre les fichiers un à un : avec une clé que seul l'attaquant possède.
- sujet/rôle : Logiciel malveillant ou attaque technique · procédure ; indices du texte : act:CHIFFRER, act:MESURER, act:PARTAGER, actor:ATTAQUANT, thing:ATTAQUE, thing:EMAIL, topic:alone, topic:attachment, topic:chain, topic:measure, topic:ransom, topic:ransom-spread, topic:talk
- PLAN : net3d-ransom/ransom-spread (Le rançongiciel se propage et chiffre les fichiers) note 1.00 ; dessine : ransom-spread
- VERDICT : ok : le texte cite ransom-spread

## 2.4 `s-steps1-2` · DEFINITION · 9.6 s
- AFFICHÉ : Comment l'attaque se déroule (3/3) ⏎ Un message de rançon s'affiche et demande un paiement, souvent avec un délai
- DIT : Un message de rançon s'affiche et demande un paiement, souvent avec un délai.
- sujet/rôle : Logiciel malveillant ou attaque technique · définition ; indices du texte : act:DEMANDER, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, topic:chain, topic:pay, topic:ransom, topic:ransom-note
- PLAN : net3d-ransom/ransom-note (Le message de rançon et son compte à rebours) note 1.00 ; dessine : ransom-note
- VERDICT : ok : le texte cite ransom-note ; déjà montré en 1.2

## 3.1 `ep3-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 3/4 ⏎ Le rançongiciel : l'attaque qui chiffre tes fichiers
- DIT : Le rançongiciel : l'attaque qui chiffre tes fichiers.
- sujet/rôle : Logiciel malveillant ou attaque technique · ouverture ; indices du texte : act:CHIFFRER, act:MESURER, thing:ATTAQUE, topic:attachment, topic:measure, topic:ransom
- PLAN : net3d-ransom/poster (Affiche : un poste dont les fichiers se verrouillent) note 1.00 ; dessine : ransom
- VERDICT : ok : le texte cite ransom ; déjà montré en 1.1

## 3.2 `s-steps2-0` · STEPS · 12.2 s
- AFFICHÉ : Les conséquences ⏎ • L'activité s'arrête : fichiers, applications et serveurs deviennent inutilisables ⏎ • Des données peuvent être volées, puis publiées ⏎ • Payer ne garantit pas de récupérer les fichiers et finance les criminels
- DIT : Les conséquences. L'activité s'arrête : fichiers, applications et serveurs deviennent inutilisables. Des données peuvent être volées, puis publiées. Payer ne garantit pas de récupérer les fichiers et finance les criminels.
- sujet/rôle : Logiciel malveillant ou attaque technique · procédure ; indices du texte : act:ENCHAINER, act:ENVOYER, actor:SERVEUR, thing:ATTAQUE, thing:DONNEES, thing:OBJET_3D, topic:attachment, topic:consequences, topic:damage, topic:flow, topic:measure, topic:pay, topic:ransom-impact, topic:ransom-leak, topic:serverroom, topic:urgency
- PLAN : net3d-ransom/ransom-consequences (Les conséquences : arrêt d'activité, fuite de données, aucune garantie en payant) note 1.00 ; dessine : consequences
- VERDICT : ok : le texte cite consequences

## 3.3 `s-steps3-0` · STEPS · 15.0 s
- AFFICHÉ : Comment le reconnaître ⏎ • Des fichiers qui changent d'extension ou ne s'ouvrent plus ⏎ • Un message qui réclame une rançon ⏎ • Un poste très lent : avec une activité disque inhabituelle ⏎ • Des sauvegardes ou des partages soudainement inaccessibles
- DIT : Comment le reconnaître. Des fichiers qui changent d'extension ou ne s'ouvrent plus. Un message qui réclame une rançon. Un poste très lent : avec une activité disque inhabituelle. Des sauvegardes ou des partages soudainement inaccessibles.
- sujet/rôle : Logiciel malveillant ou attaque technique · reconnaître les signes ; indices du texte : act:PARTAGER, thing:ATTAQUE, thing:MESSAGE, thing:ORDINATEUR, topic:attachment, topic:attract, topic:office, topic:ransom, topic:ransom-note, topic:signs, topic:talk, topic:urgency
- PLAN : net3d-ransom/ransom-signs (Les signes : fichiers illisibles, message, poste lent, accès perdus) note 1.00 ; dessine : signs
- VERDICT : ok : le texte cite signs

## 3.4 `s-def4-0` · DEFINITION · 11.8 s
- AFFICHÉ : Comment le détecter ⏎ Les outils de détection sur les postes, les EDR, repèrent le chiffrement massif de fichiers et peuvent isoler le poste.
- DIT : Les outils de détection sur les postes, les EDR, repèrent le chiffrement massif de fichiers et peuvent isoler le poste.
- sujet/rôle : Logiciel malveillant ou attaque technique · reconnaître les signes ; indices du texte : act:CHIFFRER, act:MESURER, thing:ATTAQUE, thing:OBJET_3D, thing:ORDINATEUR, topic:alone, topic:attachment, topic:attract, topic:measure, topic:office, topic:ransom, topic:ransom-detect
- PLAN : net3d-ransom/ransom-detect (Détecter le chiffrement massif et isoler le poste) note 0.93 ; dessine : ransom-detect
- VERDICT : ok : le texte cite ransom-detect

## 4.1 `ep4-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 4/4 ⏎ Le rançongiciel : l'attaque qui chiffre tes fichiers
- DIT : Le rançongiciel : l'attaque qui chiffre tes fichiers.
- sujet/rôle : Logiciel malveillant ou attaque technique · ouverture ; indices du texte : act:CHIFFRER, act:MESURER, thing:ATTAQUE, topic:attachment, topic:measure, topic:ransom
- PLAN : net3d-ransom/poster (Affiche : un poste dont les fichiers se verrouillent) note 1.00 ; dessine : ransom
- VERDICT : ok : le texte cite ransom ; déjà montré en 1.1

## 4.2 `s-steps5-0` · STEPS · 19.0 s
- AFFICHÉ : Comment s'en protéger ⏎ • Garde des sauvegardes hors ligne ou immuables, et teste leur restauration ⏎ • Mets à jour le système et les logiciels dès que les correctifs sortent ⏎ • Méfie-toi des pièces jointes et des liens inattendus ⏎ • Limite les droits : un compte ordinaire ne doit pas pouvoir modifier tous les partages ⏎ • Active la double authentification : sur les accès à distance
- DIT : Comment s'en protéger. Garde des sauvegardes hors ligne ou immuables, et teste leur restauration. Mets à jour le système et les logiciels dès que les correctifs sortent. Méfie-toi des pièces jointes et des liens inattendus. Limite les droits : un compte ordinaire ne doit pas pouvoir modifier tous les partages. Active la double authentification : sur les accès à distance.
- sujet/rôle : Logiciel malveillant ou attaque technique · conseil, parade ; indices du texte : act:ACTIVER_MFA, act:CHANGER, act:DEVINER, act:ENVOYER, act:LIMITER, act:PARTAGER, act:PUISER, thing:ATTAQUE, thing:CODE, thing:COMPTE, thing:LIEN, topic:backup, topic:food, topic:link, topic:protect, topic:ransom-entry, topic:ransom-segment, topic:sender, topic:talk, topic:vpn
- PLAN : net3d-ransom/ransom-protect (Se protéger : des couches successives autour du poste) note 1.00 ; dessine : protect
- VERDICT : ok : le texte cite protect

## 4.3 `s-def6-0` · DEFINITION · 12.9 s
- AFFICHÉ : Si tu vois le message de rançon ⏎ Débranche le poste du réseau, préviens tout de suite ton service informatique, et ne paie pas avant d'avoir l'avis d'experts.
- DIT : Débranche le poste du réseau, préviens tout de suite ton service informatique, et ne paie pas avant d'avoir l'avis d'experts.
- sujet/rôle : Logiciel malveillant ou attaque technique · définition ; indices du texte : act:RELIER, act:SIGNALER, thing:ATTAQUE, thing:MESSAGE, thing:ORDINATEUR, topic:attract, topic:office, topic:product, topic:ransom, topic:ransom-note, topic:ransom-react, topic:report, topic:retain
- PLAN : net3d-ransom/ransom-react (Réagir : débrancher, prévenir, ne pas payer) note 0.93 ; dessine : ransom-react
- VERDICT : ok : le texte cite ransom-react

## 4.4 `s-end` · CHECKLIST · 8.7 s
- AFFICHÉ : À retenir ⏎ • Des sauvegardes hors ligne ⏎ • Testées ⏎ • Sont la meilleure réponse à un rançongiciel
- DIT : À retenir. Des sauvegardes hors ligne. Testées. Sont la meilleure réponse à un rançongiciel. Partage cette astuce.
- sujet/rôle : Logiciel malveillant ou attaque technique · à retenir ; indices du texte : act:DEVINER, act:PARTAGER, act:REPONDRE, thing:ATTAQUE, thing:REPONSE, topic:backup, topic:ransom, topic:talk
- PLAN : net3d-ransom/ransom-lock (À retenir : verrouillé par l'attaquant, restauré par la sauvegarde) note 1.00 ; dessine : ransom
- VERDICT : ok : le texte cite ransom

