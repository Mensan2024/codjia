# Audit de pertinence

## 1.1 `s-hero` · HERO · 6.2 s
- AFFICHÉ : ASTUCE SYSTÈMES ⏎ Active Directory : l'annuaire qui gère les comptes de l'entreprise ⏎ Dans une entreprise de deux cents personnes, un nouvel employé arrive lundi.
- DIT : Active Directory : l'annuaire qui gère les comptes de l'entreprise. Dans une entreprise de deux cents personnes, un nouvel employé arrive lundi.
- sujet/rôle : Annuaire et domaine Windows · ouverture ; indices du texte : actor:UTILISATEUR, thing:COMPTE, topic:ad, topic:cabling, topic:dns, topic:office
- PLAN : net3d-ad/poster (Affiche : un annuaire et ses contrôleurs de domaine) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Annuaire et domaine Windows »

## 1.2 `s-def` · DEFINITION · 11.8 s
- AFFICHÉ : En quelques clics, l'informatique lui crée un compte ⏎ Il peut ouvrir une session sur n'importe quel poste et accéder aux dossiers de son équipe.
- DIT : Il peut ouvrir une session sur n'importe quel poste et accéder aux dossiers de son équipe.
- sujet/rôle : Annuaire et domaine Windows · définition ; indices du texte : thing:COMPTE, thing:ORDINATEUR, topic:ad-auth, topic:attract, topic:office, topic:volunteer
- PLAN : net3d-ad/ad-auth (L'ouverture de session : identifiants, ticket Kerberos, accès) note 0.90 ; dessine : ad-auth
- VERDICT : ok : le texte cite ad-auth

## 1.3 `s-def0-1` · DEFINITION · 14.0 s
- AFFICHÉ : Active Directory est l'annuaire de Microsoft pour les réseaux Windows : il centralise les comptes, les groupes et les ordinateurs d'un domaine, et décide qui peut se connecter et accéder à quoi.
- DIT : Active Directory est l'annuaire de Microsoft pour les réseaux Windows : il centralise les comptes, les groupes et les ordinateurs d'un domaine, et décide qui peut se connecter et accéder à quoi.
- sujet/rôle : Annuaire et domaine Windows · définition ; indices du texte : act:RELIER, thing:COMPTE, thing:OBJET_3D, thing:ORDINATEUR, topic:ad, topic:ad-domain, topic:dns, topic:sender
- PLAN : net3d-ad/ad-domain (Le domaine : contrôleurs, unités d'organisation, groupes et règles) note 0.85 ; dessine : ad-domain
- VERDICT : ok : le texte cite ad-domain

## 2.1 `ep2-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 2/4 ⏎ Active Directory : l'annuaire qui gère les comptes de l'entreprise
- DIT : Active Directory : l'annuaire qui gère les comptes de l'entreprise.
- sujet/rôle : Annuaire et domaine Windows · ouverture ; indices du texte : thing:COMPTE, topic:ad, topic:cabling, topic:dns, topic:office
- PLAN : net3d-ad/poster (Affiche : un annuaire et ses contrôleurs de domaine) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Annuaire et domaine Windows » ; déjà montré en 1.1

## 2.2 `s-steps0` · STEPS · 12.2 s
- AFFICHÉ : À quoi ça sert ⏎ • Une seule identité : pour se connecter à tous les postes et services de l'entreprise ⏎ • Les droits sont donnés à des groupes plutôt qu'à chaque personne ⏎ • Les règles de sécurité et de configuration s'appliquent à tous les postes d'un coup, grâce aux stratégies de groupe
- DIT : À quoi ça sert. Une seule identité : pour se connecter à tous les postes et services de l'entreprise. Les droits sont donnés à des groupes plutôt qu'à chaque personne. Les règles de sécurité et de configuration s'appliquent à tous les postes d'un coup, grâce aux stratégies de groupe.
- sujet/rôle : Annuaire et domaine Windows · procédure ; indices du texte : act:RELIER, thing:ORDINATEUR, thing:REGLE, topic:ad, topic:ad-central, topic:ad-gpo, topic:ad-group, topic:alone, topic:attract, topic:cabling, topic:food, topic:office, topic:product, topic:purpose
- PLAN : net3d-ad/ad-benefits (À quoi sert l'annuaire : une identité, des groupes, des règles pour tout le parc) note 1.00 ; dessine : ad-central
- VERDICT : ok : le texte cite ad-central

## 2.3 `s-steps2-0` · STEPS · 12.2 s
- AFFICHÉ : Comment ça fonctionne (1/3) ⏎ • Le domaine regroupe les comptes d'utilisateurs, d'ordinateurs et de groupes dans une base commune, l'annuaire ⏎ • Cette base est hébergée par des contrôleurs de domaine, qui se la copient entre eux : c'est la réplication ⏎ • Les objets sont rangés dans des unités d'organisation, qui servent à déléguer l'administration et à appliquer les stratégies de groupe
- DIT : Comment ça fonctionne (1/3). Le domaine regroupe les comptes d'utilisateurs, d'ordinateurs et de groupes dans une base commune, l'annuaire. Cette base est hébergée par des contrôleurs de domaine, qui se la copient entre eux : c'est la réplication. Les objets sont rangés dans des unités d'organisation, qui servent à déléguer l'administration et à appliquer les stratégies de groupe.
- sujet/rôle : Annuaire et domaine Windows · procédure ; indices du texte : act:VERIFIER, actor:UTILISATEUR, thing:COMPTE, thing:OBJET_3D, thing:ORDINATEUR, topic:ad, topic:ad-domain, topic:ad-gpo, topic:dns, topic:food, topic:sender
- PLAN : net3d-ad/ad-domain (Le domaine : contrôleurs, unités d'organisation, groupes et règles) note 0.85 ; dessine : ad-domain
- VERDICT : ok : le texte cite ad-domain ; déjà montré en 1.3

## 3.1 `ep3-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 3/4 ⏎ Active Directory : l'annuaire qui gère les comptes de l'entreprise
- DIT : Active Directory : l'annuaire qui gère les comptes de l'entreprise.
- sujet/rôle : Annuaire et domaine Windows · ouverture ; indices du texte : thing:COMPTE, topic:ad, topic:cabling, topic:dns, topic:office
- PLAN : net3d-ad/poster (Affiche : un annuaire et ses contrôleurs de domaine) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Annuaire et domaine Windows » ; déjà montré en 1.1

## 3.2 `s-steps2-1` · STEPS · 9.0 s
- AFFICHÉ : Comment ça fonctionne (2/3) ⏎ • À l'ouverture de session : le poste prouve l'identité de l'utilisateur au contrôleur de domaine, qui répond par un ticket Kerberos ⏎ • Avec ce ticket : l'utilisateur obtient des tickets de service pour accéder aux dossiers partagés ou aux applications, sans retaper son mot de passe
- DIT : Comment ça fonctionne (2/3). À l'ouverture de session : le poste prouve l'identité de l'utilisateur au contrôleur de domaine, qui répond par un ticket Kerberos. Avec ce ticket : l'utilisateur obtient des tickets de service pour accéder aux dossiers partagés ou aux applications, sans retaper son mot de passe.
- sujet/rôle : Annuaire et domaine Windows · procédure ; indices du texte : act:ENCHAINER, act:PARTAGER, act:REPONDRE, act:VERIFIER, actor:UTILISATEUR, thing:ETAPES, thing:MOT_DE_PASSE, thing:OBJET_3D, thing:ORDINATEUR, topic:ad, topic:ad-auth, topic:attract, topic:login, topic:office, topic:product, topic:sender, topic:talk
- PLAN : net3d-ad/ad-auth (L'ouverture de session : identifiants, ticket Kerberos, accès) note 1.00 ; dessine : ad-auth
- VERDICT : ok : le texte cite ad-auth ; déjà montré en 1.2

## 3.3 `s-steps2-2` · DEFINITION · 11.5 s
- AFFICHÉ : Comment ça fonctionne (3/3) ⏎ Les utilisateurs sont placés dans des groupes, et ce sont les groupes qui reçoivent les permissions sur les dossiers
- DIT : Les utilisateurs sont placés dans des groupes, et ce sont les groupes qui reçoivent les permissions sur les dossiers.
- sujet/rôle : Annuaire et domaine Windows · définition ; indices du texte : actor:UTILISATEUR, thing:ETAPES, thing:MISSION, topic:ad-group
- PLAN : net3d-ad/ad-groups (Les utilisateurs sont dans des groupes, les groupes ont les droits) note 1.00 ; dessine : ad-group
- VERDICT : ok : le texte cite ad-group

## 4.1 `ep4-open` · HERO · 2.6 s
- AFFICHÉ : ÉPISODE 4/4 ⏎ Active Directory : l'annuaire qui gère les comptes de l'entreprise
- DIT : Active Directory : l'annuaire qui gère les comptes de l'entreprise.
- sujet/rôle : Annuaire et domaine Windows · ouverture ; indices du texte : thing:COMPTE, topic:ad, topic:cabling, topic:dns, topic:office
- PLAN : net3d-ad/poster (Affiche : un annuaire et ses contrôleurs de domaine) note 0.85 ; dessine : (générique)
- VERDICT : plan générique (ouverture) : sujet « Annuaire et domaine Windows » ; déjà montré en 1.1

## 4.2 `s-steps3-0` · DEFINITION · 8.5 s
- AFFICHÉ : Erreurs fréquentes (1/2) ⏎ Donner les droits d'administrateur du domaine à des comptes utilisés au quotidien
- DIT : Donner les droits d'administrateur du domaine à des comptes utilisés au quotidien.
- sujet/rôle : Annuaire et domaine Windows · définition ; indices du texte : thing:COMPTE, thing:SOIR, topic:ad, topic:ad-admin, topic:sender
- PLAN : net3d-ad/ad-admin (Un compte du quotidien administrateur du domaine : un seul poste compromis expose tout) note 0.85 ; dessine : ad-admin
- VERDICT : ok : le texte cite ad-admin

## 4.3 `s-steps3-1` · DEFINITION · 10.0 s
- AFFICHÉ : Erreurs fréquentes (2/2) ⏎ N'avoir qu'un seul contrôleur de domaine : s'il tombe, plus personne ne peut s'authentifier sur le domaine
- DIT : N'avoir qu'un seul contrôleur de domaine : s'il tombe, plus personne ne peut s'authentifier sur le domaine.
- sujet/rôle : Annuaire et domaine Windows · définition ; indices du texte : act:VERIFIER, topic:ad, topic:ad-auth, topic:ad-dcfail, topic:alone, topic:sender
- PLAN : net3d-ad/ad-dcfail (Un seul contrôleur de domaine : s'il tombe, plus personne ne s'authentifie) note 0.85 ; dessine : ad-dcfail
- VERDICT : ok : le texte cite ad-dcfail

## 4.4 `s-end` · CHECKLIST · 9.0 s
- AFFICHÉ : À retenir ⏎ • Active Directory centralise les identités : un compte ⏎ • Des groupes ⏎ • Des règles appliquées à tout le parc
- DIT : À retenir. Active Directory centralise les identités : un compte. Des groupes. Des règles appliquées à tout le parc. Partage cette astuce.
- sujet/rôle : Annuaire et domaine Windows · à retenir ; indices du texte : act:PARTAGER, thing:COMPTE, thing:REGLE, topic:ad, topic:ad-central, topic:talk
- PLAN : net3d-ad/ad-benefits (À quoi sert l'annuaire : une identité, des groupes, des règles pour tout le parc) note 1.00 ; dessine : ad-central
- VERDICT : ok : le texte cite ad-central ; déjà montré en 2.2

