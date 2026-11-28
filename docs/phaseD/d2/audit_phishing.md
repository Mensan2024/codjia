# Audit de pertinence

## 1.1 `s-hero` · HERO · 6.2 s
- AFFICHÉ : ASTUCE CYBERSÉCURITÉ ⏎ Le phishing : le faux courriel qui vole ton mot de passe ⏎ Lundi matin, Awa ouvre sa boîte de réception.
- DIT : Le phishing : le faux courriel qui vole ton mot de passe. Lundi matin, Awa ouvre sa boîte de réception.
- sujet/rôle : Hameçonnage · ouverture ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:MOT_DE_PASSE, thing:OBJET_3D, topic:inbox, topic:login
- PLAN : sit-phishing/inbox (Une personne reçoit un courriel urgent) note 1.07 ; dessine : inbox
- VERDICT : ok : le texte cite inbox

## 1.2 `s-def` · DEFINITION · 9.2 s
- AFFICHÉ : Un courriel de « sa banque » lui annonce que son compte sera suspendu sous 24 heures.
- DIT : Un courriel de « sa banque » lui annonce que son compte sera suspendu sous 24 heures.
- sujet/rôle : Hameçonnage · définition ; indices du texte : actor:BANQUE, thing:ATTAQUE, thing:COMPTE, thing:EMAIL, thing:MESSAGE, topic:callback, topic:inbox, topic:phish-urgent, topic:urgency
- PLAN : sit-phishing/urgent (Le message presse : compte suspendu sous 24 heures) note 1.07 ; dessine : phish-urgent
- VERDICT : ok : le texte cite phish-urgent

## 1.3 `s-def0-1` · DEFINITION · 8.1 s
- AFFICHÉ : Il lui demande de confirmer ses informations en cliquant sur un lien.
- DIT : Il lui demande de confirmer ses informations en cliquant sur un lien.
- sujet/rôle : Hameçonnage · définition ; indices du texte : act:CLIQUER, act:DEMANDER, thing:ATTAQUE, thing:EMAIL, thing:LIEN, topic:link, topic:phish-click
- PLAN : sit-phishing/clicklink (Le message demande de confirmer ses informations en cliquant sur un lien) note 1.07 ; dessine : phish-click
- VERDICT : ok : le texte cite phish-click

## 1.4 `s-def0-2` · DEFINITION · 13.3 s
- AFFICHÉ : C'est du hameçonnage, ou phishing ⏎ Un fraudeur imite un organisme de confiance pour te pousser à révéler tes identifiants ou tes données bancaires, ou à ouvrir un fichier piégé.
- DIT : Un fraudeur imite un organisme de confiance pour te pousser à révéler tes identifiants ou tes données bancaires, ou à ouvrir un fichier piégé.
- sujet/rôle : Hameçonnage · définition ; indices du texte : act:ENVOYER, actor:ATTAQUANT, actor:BANQUE, thing:ATTAQUE, thing:DONNEES, thing:EMAIL, thing:MOT_DE_PASSE, topic:attachment, topic:flow, topic:login, topic:measure, topic:sender, topic:talk
- PLAN : sit-phishing/victim (Le mot de passe saisi part chez l'attaquant) note 1.07 ; dessine : login
- VERDICT : ok : le texte cite login

## 2.1 `ep2-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 2/5 ⏎ Le phishing : le faux courriel qui vole ton mot de passe
- DIT : Le phishing : le faux courriel qui vole ton mot de passe.
- sujet/rôle : Hameçonnage · ouverture ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:MOT_DE_PASSE, thing:OBJET_3D, thing:SMS, topic:inbox, topic:login
- PLAN : sit-phishing/poster (Affiche : un message qui fait douter) note 0.78 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Hameçonnage »

## 2.2 `s-def0-3` · DEFINITION · 12.9 s
- AFFICHÉ : Le spear phishing vise une personne précise avec un message personnalisé, le whaling vise les dirigeants, le smishing passe par SMS et le vishing par téléphone.
- DIT : Le spear phishing vise une personne précise avec un message personnalisé, le whaling vise les dirigeants, le smishing passe par SMS et le vishing par téléphone.
- sujet/rôle : Hameçonnage · définition ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:SMS, topic:callback, topic:phish-variants
- PLAN : sit-phishing/variants (Les variantes : spear phishing, whaling, smishing, vishing) note 1.07 ; dessine : phish-variants
- VERDICT : ok : le texte cite phish-variants

## 2.3 `s-steps0` · STEPS · 19.0 s
- AFFICHÉ : Comment l'attaque se déroule ⏎ • Le fraudeur choisit une marque connue et copie son logo et son style ⏎ • Il envoie un message urgent : compte suspendu, colis bloqué ou facture impayée ⏎ • Le lien mène à une fausse page de connexion : hébergée sur une adresse qui ressemble à la vraie ⏎ • Tu saisis ton identifiant et ton mot de passe : ils sont envoyés directement au fraudeur ⏎ • Il se connecte à ton vrai compte, ou il revend tes identifiants
- DIT : Comment l'attaque se déroule. Le fraudeur choisit une marque connue et copie son logo et son style. Il envoie un message urgent : compte suspendu, colis bloqué ou facture impayée. Le lien mène à une fausse page de connexion : hébergée sur une adresse qui ressemble à la vraie. Tu saisis ton identifiant et ton mot de passe : ils sont envoyés directement au fraudeur. Il se connecte à ton vrai compte, ou il revend tes identifiants.
- sujet/rôle : Hameçonnage · procédure ; indices du texte : act:BLOQUER, act:CONVERTIR, act:ENVOYER, act:RELIER, act:SAISIR, actor:ATTAQUANT, thing:ATTAQUE, thing:COMPTE, thing:EMAIL, thing:LIEN, thing:MESSAGE, thing:MOT_DE_PASSE, thing:OBJET_3D, thing:SMS, topic:chain, topic:inbox, topic:link, topic:login, topic:pay, topic:sender, topic:urgency
- PLAN : sit-phishing/chain (L'attaque de bout en bout : du fraudeur au compte volé) note 1.07 ; dessine : chain
- VERDICT : ok : le texte cite chain

## 3.1 `ep3-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 3/5 ⏎ Le phishing : le faux courriel qui vole ton mot de passe
- DIT : Le phishing : le faux courriel qui vole ton mot de passe.
- sujet/rôle : Hameçonnage · ouverture ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:MOT_DE_PASSE, thing:OBJET_3D, topic:inbox, topic:login
- PLAN : sit-phishing/poster (Affiche : un message qui fait douter) note 0.78 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Hameçonnage » ; déjà montré en 2.1

## 3.2 `s-steps2-0` · STEPS · 12.2 s
- AFFICHÉ : Les conséquences ⏎ • Ton compte est volé : avec toutes les données qu'il contient ⏎ • Des virements ou des achats frauduleux sont faits en ton nom ⏎ • Le fraudeur utilise ton compte : pour piéger tes contacts ou entrer dans le réseau de ton entreprise
- DIT : Les conséquences. Ton compte est volé : avec toutes les données qu'il contient. Des virements ou des achats frauduleux sont faits en ton nom. Le fraudeur utilise ton compte : pour piéger tes contacts ou entrer dans le réseau de ton entreprise.
- sujet/rôle : Hameçonnage · conseil, parade ; indices du texte : actor:ATTAQUANT, thing:ATTAQUE, thing:COMPTE, thing:DONNEES, thing:EMAIL, thing:OFFRE, topic:cabling, topic:consequences, topic:damage, topic:flow, topic:measure, topic:office, topic:pay
- PLAN : sit-phishing/damage (Ce que coûte un compte volé) note 1.00 ; dessine : damage
- VERDICT : ok : le texte cite damage

## 3.3 `s-steps3-0` · STEPS · 15.6 s
- AFFICHÉ : Comment le reconnaître ⏎ • Un message urgent qui menace de suspendre ton compte ⏎ • Une adresse d'expéditeur qui ressemble à celle de la banque sans être la sienne ⏎ • Un lien dont l'adresse réelle ne correspond pas au texte affiché ⏎ • Une formule générique à la place de ton nom, ou une pièce jointe que tu n'attendais pas
- DIT : Comment le reconnaître. Un message urgent qui menace de suspendre ton compte. Une adresse d'expéditeur qui ressemble à celle de la banque sans être la sienne. Un lien dont l'adresse réelle ne correspond pas au texte affiché. Une formule générique à la place de ton nom, ou une pièce jointe que tu n'attendais pas.
- sujet/rôle : Hameçonnage · reconnaître les signes ; indices du texte : act:ENVOYER, actor:BANQUE, thing:ATTAQUE, thing:COMPTE, thing:EMAIL, thing:LIEN, thing:MESSAGE, topic:attachment, topic:callback, topic:inbox, topic:link, topic:sender, topic:signs, topic:urgency
- PLAN : sit-phishing/clues (Les indices qui trahissent le message) note 1.07 ; dessine : EMAIL, LIEN, VERIFIER, inbox, link, sender, urgency
- VERDICT : ok : le texte cite EMAIL, LIEN, sender, urgency, link, inbox

## 4.1 `ep4-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 4/5 ⏎ Le phishing : le faux courriel qui vole ton mot de passe
- DIT : Le phishing : le faux courriel qui vole ton mot de passe.
- sujet/rôle : Hameçonnage · ouverture ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:MOT_DE_PASSE, thing:OBJET_3D, topic:inbox, topic:login
- PLAN : sit-phishing/poster (Affiche : un message qui fait douter) note 0.78 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Hameçonnage » ; déjà montré en 2.1

## 4.2 `s-steps4-0` · DEFINITION · 9.2 s
- AFFICHÉ : Comment le détecter (1/2) ⏎ Survole le lien avec ta souris : l'adresse réelle s'affiche, compare-la au site officiel
- DIT : Survole le lien avec ta souris : l'adresse réelle s'affiche, compare-la au site officiel.
- sujet/rôle : Hameçonnage · reconnaître les signes ; indices du texte : act:NAVIGUER_DIRECT, act:VERIFIER, thing:ATTAQUE, thing:EMAIL, thing:LIEN, topic:callback, topic:link, topic:office, topic:phish-hover, topic:sender
- PLAN : sit-phishing/hover (Survoler le lien avant de cliquer) note 1.00 ; dessine : phish-hover
- VERDICT : ok : le texte cite phish-hover

## 4.3 `s-steps4-1` · DEFINITION · 11.1 s
- AFFICHÉ : Comment le détecter (2/2) ⏎ Dans l'en-tête du message : un échec des contrôles SPF, DKIM ou DMARC signale une usurpation possible de l'expéditeur
- DIT : Dans l'en-tête du message : un échec des contrôles SPF, DKIM ou DMARC signale une usurpation possible de l'expéditeur.
- sujet/rôle : Hameçonnage · reconnaître les signes ; indices du texte : act:ENVOYER, act:SIGNALER, act:VERIFIER, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, topic:phish-headers, topic:report, topic:sender
- PLAN : sit-phishing/headers (En-tête du message : SPF, DKIM et DMARC en échec) note 1.07 ; dessine : phish-headers
- VERDICT : ok : le texte cite phish-headers

## 4.4 `s-steps5-0` · DEFINITION · 9.6 s
- AFFICHÉ : Comment s'en protéger (1/5) ⏎ Ne clique pas sur le lien : ouvre le site en tapant toi-même son adresse
- DIT : Ne clique pas sur le lien : ouvre le site en tapant toi-même son adresse.
- sujet/rôle : Hameçonnage · conseil, parade ; indices du texte : act:CLIQUER, act:NAVIGUER_DIRECT, thing:ATTAQUE, thing:EMAIL, thing:LIEN, topic:link, topic:office, topic:phish-typed, topic:protect, topic:sender
- PLAN : sit-phishing/typed (Taper soi-même l'adresse officielle plutôt que cliquer) note 1.07 ; dessine : phish-typed
- VERDICT : ok : le texte cite phish-typed

## 4.5 `s-steps5-1` · DEFINITION · 8.5 s
- AFFICHÉ : Comment s'en protéger (2/5) ⏎ Appelle l'organisme avec le numéro officiel : jamais avec celui du message
- DIT : Appelle l'organisme avec le numéro officiel : jamais avec celui du message.
- sujet/rôle : Hameçonnage · conseil, parade ; indices du texte : act:DEMANDER, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, topic:callback, topic:phish-callback, topic:protect
- PLAN : sit-phishing/callback (Appeler par le numéro officiel) note 0.87 ; dessine : phish-callback
- VERDICT : ok : le texte cite phish-callback

## 4.6 `s-steps5-2` · DEFINITION · 9.2 s
- AFFICHÉ : Comment s'en protéger (3/5) ⏎ Active la double authentification : un second contrôle s'ajoute à ton mot de passe
- DIT : Active la double authentification : un second contrôle s'ajoute à ton mot de passe.
- sujet/rôle : Hameçonnage · conseil, parade ; indices du texte : act:ACTIVER_MFA, act:VERIFIER, thing:ATTAQUE, thing:CODE, thing:EMAIL, thing:MOT_DE_PASSE, thing:OBJET_3D, topic:login, topic:phish-mfa, topic:protect
- PLAN : sit-phishing/mfa (Double authentification : le mot de passe seul ne suffit plus) note 1.07 ; dessine : phish-mfa
- VERDICT : ok : le texte cite phish-mfa

## 5.1 `ep5-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 5/5 ⏎ Le phishing : le faux courriel qui vole ton mot de passe
- DIT : Le phishing : le faux courriel qui vole ton mot de passe.
- sujet/rôle : Hameçonnage · ouverture ; indices du texte : thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:MOT_DE_PASSE, thing:OBJET_3D, topic:inbox, topic:login
- PLAN : sit-phishing/poster (Affiche : un message qui fait douter) note 0.78 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Hameçonnage » ; déjà montré en 2.1

## 5.2 `s-steps5-3` · DEFINITION · 10.3 s
- AFFICHÉ : Comment s'en protéger (4/5) ⏎ Préfère une clé de sécurité ou une passkey : elle refuse de s'authentifier sur un faux site
- DIT : Préfère une clé de sécurité ou une passkey : elle refuse de s'authentifier sur un faux site.
- sujet/rôle : Hameçonnage · conseil, parade ; indices du texte : act:BLOQUER, thing:ATTAQUE, thing:EMAIL, topic:office, topic:phish-passkey, topic:protect
- PLAN : sit-phishing/passkey (Passkey ou clé de sécurité : elle refuse un faux site) note 1.07 ; dessine : phish-passkey
- VERDICT : ok : le texte cite phish-passkey

## 5.3 `s-steps5-4` · DEFINITION · 7.8 s
- AFFICHÉ : Comment s'en protéger (5/5) ⏎ Signale le courriel à ton service informatique, puis supprime-le
- DIT : Signale le courriel à ton service informatique, puis supprime-le.
- sujet/rôle : Hameçonnage · conseil, parade ; indices du texte : act:ENCHAINER, act:SIGNALER, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, topic:inbox, topic:product, topic:protect, topic:report
- PLAN : sit-phishing/report (Signaler puis supprimer) note 1.07 ; dessine : report
- VERDICT : ok : le texte cite report

## 5.4 `s-note5-0` · DEFINITION · 12.2 s
- AFFICHÉ : Si tu as déjà saisi ton mot de passe : change-le immédiatement sur le site officiel et préviens ta banque ou ton service informatique
- DIT : Si tu as déjà saisi ton mot de passe : change-le immédiatement sur le site officiel et préviens ta banque ou ton service informatique.
- sujet/rôle : Hameçonnage · définition ; indices du texte : act:NAVIGUER_DIRECT, act:SAISIR, act:SIGNALER, actor:BANQUE, thing:ATTAQUE, thing:EMAIL, thing:MOT_DE_PASSE, thing:OBJET_3D, topic:callback, topic:login, topic:office, topic:phish-reset, topic:product, topic:report, topic:retain, topic:urgency
- PLAN : sit-phishing/reset (Déjà saisi son mot de passe : le changer sur le site officiel et prévenir) note 1.07 ; dessine : phish-reset
- VERDICT : ok : le texte cite phish-reset

## 5.5 `s-end` · CHECKLIST · 9.0 s
- AFFICHÉ : À retenir ⏎ • Face à un message urgent qui demande tes identifiants ⏎ • Ne clique pas : vérifie par un canal officiel
- DIT : À retenir. Face à un message urgent qui demande tes identifiants. Ne clique pas : vérifie par un canal officiel. Enregistre cette astuce.
- sujet/rôle : Hameçonnage · à retenir ; indices du texte : act:CLIQUER, act:DEMANDER, act:VERIFIER, thing:ATTAQUE, thing:EMAIL, thing:MESSAGE, thing:MOT_DE_PASSE, topic:callback, topic:inbox, topic:link, topic:login, topic:troubleshoot, topic:urgency
- PLAN : phishing/recap (À retenir : vérifier avant de cliquer) note 0.90 ; dessine : CLIQUER, LIEN, VERIFIER
- VERDICT : ok : le texte cite VERIFIER, CLIQUER

