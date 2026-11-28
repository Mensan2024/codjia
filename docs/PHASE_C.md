# Phase C — des idées du texte aux situations visuelles (2.12.0)

Objectif de l'utilisateur : « concentre-toi exclusivement sur la pertinence et la richesse des scènes » — un texte de phishing doit montrer un courriel frauduleux, un lien suspect et une personne confrontée ; un texte de santé mentale, un personnage dans une situation crédible ; un texte humanitaire, des personnes, des lieux ; un texte marketing, un produit, un client, un parcours ; un texte réseau, des topologies, des équipements, des flux.

## Ce qui a été ajouté

| Élément | Rôle |
|---|---|
| `Cast.kt` | Personnages 2D : 6 apparences, 13 expressions (+ mélange), corps (bras, inclinaison, pas, assis, accroupi, mains qui visent une cible), poses prêtes à l'emploi. Rendu déterministe (même graine → mêmes pixels). |
| `Props.kt` | Décors et accessoires : pièces, fenêtres, lit, lampe, horloge, arbres, tentes, étal, caisse, camion, bulles… |
| `ScenesSitPhishing/Mental/Aid/Shop/Net.kt` | 5 familles de **situations** (47 plans) : chaque plan est une petite scène jouée (personnages + objets + lieu) liée à des sujets précis du texte. |
| `Intent.kt` | Détection de **sujets précis** (`topics` : dns, dhcp, nat, vpn, inbox, clues, breath, water, stall, pay, firstfail…) ; un plan peut exiger un sujet (`needs`) — c'est ce qui empêche un schéma Wi-Fi d'illustrer une phrase sans rapport. Les cartes d'ouverture sont rôle « introduction ». |
| `SceneRegistry.kt` | Les situations passent à égalité devant un schéma abstrait (+0,07) ; la carte d'ouverture d'épisode préfère l'affiche en situation du sujet ; un plan non répétable n'est jamais réutilisé dans un autre épisode de la même série (`Storyboard.reserved`). |
| `Planner.kt` / `Fidelity.kt` / `Episodes.kt` / `StoryPlan.kt` | Groupes de 150 caractères en mode studio ; pas de scène de démonstration pour un système d'exploitation sans procédure ; marqueurs de liste (« 1 », « • ») non comptés comme des mots de l'auteur ; ouverture d'épisode illustrée. |
| `Gallery.kt` + `story gallery` | Planche de tous les plans, pour les relire. Exemples : `docs/phaseC/galerie/`. |

Aucun format n'a été retiré (les 26 formats restent). Identifiant de paquet et permissions inchangés (aucune permission).

## Résultats mesurés sur cinq textes (graine 7)

Textes : `docs/phaseC/textes/`. Avant = VIDEO24 2.11.0 (tag 364b39d), après = 2.12.0. Storyboards, plans JSON, planches et comparaisons : `docs/phaseC/exemples/<texte>/`.

| Texte | Scènes sans image avant → après | Plans différents avant → après | Plans « situation » après |
|---|---|---|---|
| Hameçonnage | 4/7 → 0/8 | 4 → 8 | 7 |
| Santé mentale | 3/7 → 0/7 | 5 → 7 | 7 |
| Humanitaire | 4/8 → 0/8 | 5 → 8 | 8 |
| Marketing | 4/7 → 0/8 | 4 → 8 | 7 |
| Réseaux | 5/11 → 0/11 | 7 → 11 | 9 |

Fidélité du texte : 100 % présents, 100 % dans l'ordre sur les cinq textes (avant : 97,6 % sur le texte marketing, à cause de la numérotation de liste). Contrôle qualité : 0 remarque (avant : 3 à 11). Réseaux : plus de scène de démonstration macOS inventée.

## Limites connues (honnêtes)

- Les durées « avec voix » sont des estimations (2,5 mots/s) ; les MP4 de démonstration du banc JVM n'ont **pas de voix** : la synchronisation voix/image n'est **pas validée** par ces vidéos.
- Quelques scènes restent des schémas plus anciens (`phishing/mail`, `phishing/recap`, `stress/recap` selon le texte, `topology/star`, `firewall/filter`, `funnel/leak`) : illustrés, mais plus abstraits que les situations.
- Les titres de scène génériques (« À savoir », « Et aussi », « En bref ») et les lignes « Partage cette astuce » viennent des gabarits de l'application, pas de l'auteur.
- Les personnages sont stylisés et peu variés (6 apparences) ; certaines poses (tête dans les mains, personne couchée) restent simples.
- Les plans de situation sont écrits pour 5 domaines ; un texte hors de ces domaines retombe sur le registre précédent, ou sur le texte seul si aucun plan n'atteint le seuil de pertinence (0,6).
- Web et APK ne sont validés que par compilation en CI ; le storyboard n'a pas été éprouvé sur appareil Android réel.
- Seuils (45 ms/image, 150 ms voix/image, 10 dB voix/musique, 8 % décoratif) : objectifs provisoires, non mesurés par cette phase.

## Hors périmètre

La phase D n'est pas commencée (validation de la phase C requise).
