# Partie 5 — Audit du moteur et architecture proposée (étape 0)

Ce document répond à l'exigence « auditer le moteur actuel avant toute modification » et « distinguer les fonctionnalités réellement disponibles de celles qui nécessitent un moteur externe ». Il décrit l'état réel de CODJIA 2.5.x, pas l'état souhaité.

## 1. Ce que le moteur est réellement

CODJIA est un **moteur de rendu vectoriel 2D procédural**, écrit en Kotlin commun (`engine-core`). Chaque image de la vidéo est **dessinée par du code** (rectangles, cercles, chemins, dégradés, halos, texte) à 30 images par seconde, puis encodée en H.264.

Conséquences vérifiées dans le code :

| Capacité | État | Détail |
|---|---|---|
| Dessin vectoriel 2D (formes, chemins, dégradés, halos, texte) | Disponible | Interface `Gfx` : `fillPath`, `fillRadialGlow`, `fillVerticalGradient`, `drawText`… |
| Animation (mouvement, apparition, zoom, panoramique léger, transitions) | Disponible | Chronologie, courbes d'accélération, 5 types de transition |
| Motion design, typographie animée, infographies, schémas | Disponible | 22 formats dont Infographie, Explicatif, Tableau blanc, Stratégie, Élan, Sérénité, Lumière |
| Simulations techniques (terminal, fenêtres, paquets, VLAN, pare-feu, API) | Disponible | Étiquetées « Simulation » ; aucune exécution réelle |
| Personnage animé | Partiel | Un seul personnage : le robot Codi (`MascotLook`) et une main de tableau blanc. **Aucun personnage humain.** |
| **Importer une image ou une vidéo** (photo, plan filmé, illustration externe) | **Absent** | L'interface `Gfx` du moteur commun ne contient aucune primitive d'image : `drawImage` n'existe que dans l'outil de test JVM. Le moteur **ne sait pas afficher une photo**. |
| Plans réalistes, humains photoréalistes, environnements naturels réalistes | **Impossible en interne** | Rien ne les produit. Le dessin procédural donne un rendu « illustration plate », pas du cinéma. |
| 3D réelle (modèles, éclairage, caméra 3D) | **Absent** | Aucun moteur 3D. Une pseudo-3D isométrique ou en parallaxe est faisable en 2D. |
| Génération vidéo par IA | **Absent** | L'application n'a aucune permission réseau et fonctionne hors ligne. |
| Voix de synthèse, musique, mixage | Disponible | Musiques du catalogue (droits vérifiés), voix du système, enregistrement personnel |
| Export vertical 1080×1920 | Disponible | MP4 (JVM, Android Media3, navigateur) |
| Export 16:9 et 1:1 | **Non livré** | `VideoFormat` accepte des dimensions arbitraires mais les mises en page sont composées pour le vertical ; il faut les recomposer, pas recadrer. |

**Verdict honnête.** Les modes « réalisme cinématographique », « 3D » et « semi-réaliste » de la partie 5 ne peuvent pas être produits par le moteur actuel. Promettre le contraire serait faux. Ce qui est atteignable en interne : l'**animation 2D illustrée de qualité**, le **motion design professionnel**, le **documentaire infographique** et les **démonstrations techniques**.

## 2. Les six modes de la partie 5 face à la réalité

| Mode | Faisable en interne ? | Ce qu'il faut pour aller plus loin |
|---|---|---|
| 1. Réalisme cinématographique | Non | Médias réels ou générés : photos/vidéos de l'utilisateur, banque libre de droits, ou génération externe |
| 2. Animation 2D | **Oui**, avec un vrai travail d'illustration | Personnages humains vectoriels paramétrables, décors par scène, animations de pose |
| 3. Animation 3D | Non (vraie 3D) ; isométrique 2D oui | Moteur 3D ou génération externe |
| 4. Semi-réaliste | Non en interne | Fond photographique importé + personnages 2D par-dessus (nécessite d'abord l'import d'images) |
| 5. Documentaire | **Partiel** : infographies, cartes simplifiées, citations, mention « simulation » | Images de terrain : médias importés |
| 6. Motion design | **Oui**, déjà le cœur du moteur | Plus de variété et de finition |

## 3. Architecture proposée : des « fournisseurs visuels » interchangeables

Pour ne jamais bloquer l'application quand un service externe manque, chaque scène est dessinée par un **fournisseur visuel** choisi selon la disponibilité. Le texte, les durées et la narration ne changent pas ; seul l'habillage de la scène change.

```
Storyboard (texte conservé, scènes, durées)
        │
        ▼
 SceneIntent  « que doit montrer cette scène ? »  (sujet, action, lieu, émotion, personnage)
        │
        ▼
 VisualProvider (interface)  ──►  choisi par ordre de préférence, repli automatique
   1. UserMedia        photos/vidéos fournies par l'utilisateur (droits à sa charge)    [à créer]
   2. LicensedLibrary  illustrations/photos libres de droits intégrées, licences tracées [à créer]
   3. ExternalGen      génération par un service tiers (clé de l'utilisateur)             [optionnel, hors ligne désactivé]
   4. Procedural       dessin vectoriel interne : toujours disponible                      [existant, repli final]
        │
        ▼
 Rendu, montage, audio, export (existant)
```

Règles de conception :

- **Le repli procédural est toujours présent.** Si un fournisseur échoue (réseau, quota, erreur), la scène est redessinée en vectoriel, et l'utilisateur en est informé.
- **Aucun fournisseur externe sans accord explicite.** Aujourd'hui l'application n'a aucune permission et n'envoie rien. Un fournisseur externe enverrait le texte de l'utilisateur à un tiers : ce changement de principe doit être validé par toi, avec indication claire dans l'interface, clé API fournie par l'utilisateur et stockée localement, jamais dans le dépôt.
- **Étiquetage éthique intégré** : toute scène générée ou illustrée est marquée comme fictive/simulée ; aucun témoignage, chiffre, victime ou situation historique n'est ajouté par le moteur.
- **Droits** : seuls les médias dont la licence est tracée (fichier `licenses.json` par ressource) sont intégrés ; les médias importés restent sous la responsabilité de l'utilisateur, comme pour la musique importée.

## 4. Trois niveaux de production (ta recommandation)

| Niveau | Contenu réel | Dépend d'un service externe |
|---|---|---|
| **Essentiel** | Rendu actuel, 30 i/s, scènes simples, durée de rendu courte | Non |
| **Professionnel** | Illustrations de scène, personnages 2D récurrents, caméra et transitions soignées, 1080p | Non |
| **Cinématique** | Plans réalistes (médias importés ou générés), profondeur, étalonnage | **Oui** : médias importés ou service externe. Sans cela, ce niveau est désactivé et affiché comme tel. |

## 5. Plan par priorité (conforme à ton ordre), avec ce que chaque étape livre vraiment

1. **Qualité de rendu des scènes** : lisibilité, finition, éclairage et profondeur des illustrations existantes ; jeu de transitions par famille. *Livrable : captures avant/après et tests.*
2. **Import d'images et de vidéos par l'utilisateur** (prérequis des modes réaliste, semi-réaliste et documentaire) : ajout de `drawImage` au moteur commun, décodage par plateforme, mouvements de caméra (Ken Burns, parallaxe), superposition de texte lisible. *C'est la seule voie réaliste vers du « semi-réaliste » sans service externe.*
3. **Personnages humains 2D cohérents** : `CharacterSpec` (visage, teint, coiffure, vêtement, proportions, style) gelé par vidéo et réutilisable dans une série ; poses et expressions de base ; diversité de représentation dès le départ.
4. **Famille humanitaire** : bibliothèque de scènes illustrées (village, point d'eau, école, centre de santé, marché…), narration sans sensationnalisme, étiquette « illustration fictive », aucun chiffre ni témoignage inventé.
5. **Caméra, mouvement, transitions** adaptés à chaque famille.
6. **Storyboard modifiable** : liste des scènes avec passage du texte, style, durée, et relance d'une seule scène (le moteur est déjà déterministe par scène et par graine : la relance partielle est réalisable).
7. **Audio et synchronisation** : durcir la synchronisation scène/narration, effets sonores sous licence, réduction automatique de la musique sous la voix.
8. **Tests de performance et de qualité** sur appareils réels et sur les huit vidéos de test de la partie 5.

## 6. Décisions que toi seul peux prendre

1. **Import d'images et de vidéos par l'utilisateur** (étape 2) : oui / non ? C'est ce qui débloque tout le réalisme possible hors ligne.
2. **Service externe de génération** : veux-tu en prévoir un (clé API de l'utilisateur, coût et vie privée à sa charge) ou rester 100 % hors ligne et sans permission ?
3. **Bibliothèque d'illustrations libres de droits** : acceptes-tu que je ne l'intègre qu'après vérification de chaque licence, sinon je dessine tout en vectoriel (plus lent, plus cohérent, droits garantis) ?

Tant que ces trois points ne sont pas tranchés, je me limite à ce que le moteur sait faire honnêtement : étapes 1, 3, 4 (illustrations vectorielles), 5, 6 et 7.

## 7. Décisions validées (26/11/2026) et constats visuels de la priorité 1

Décisions : **aucun import d'images ou de vidéos**, **aucun service externe**, **tout en vectoriel dessiné par le moteur** (droits garantis, cohérence). Le niveau « Cinématique » reste donc désactivé ; les modes réaliste, semi-réaliste et 3D ne seront pas proposés comme tels. Les modes réellement offerts : animation 2D illustrée, motion design, documentaire infographique, démonstration technique.

Constats sur images de contrôle (Sérénité, Élan, Démo ; texte de 38 à 50 s) :

1. **Espace vide** : le tiers inférieur de nombreuses scènes courtes est vide (contenu concentré en haut, pied de page « CODJIA » seul en bas).
2. **Aucun sujet illustré** : les scènes sont typographiques (orbe, soleil, fenêtre). Rien ne représente le propos (personne, lieu, objet). C'est le vrai écart avec la partie 5 ; il se comble par la priorité 3 (personnages) et la priorité 4 (scènes humanitaires), pas par un simple polissage.
3. **Habillage trompeur** : en Démo, un audit de sécurité est montré dans une fausse fenêtre « Paramètres » avec des interrupteurs, ce qui laisse croire à un réglage réel.
4. **Titres génériques** du planificateur (« Action — Sur tes propres machines… ») qui reprennent une phrase du texte en guise de titre.
5. **Profondeur** : aplats et halos seulement ; pas d'ombres portées cohérentes ni de plans superposés.

Incrément proposé pour la priorité 1 : (a) équilibrage vertical des scènes courtes, (b) plans de profondeur (ombres douces, couches de parallaxe lente) communs aux formats, (c) retrait des habillages trompeurs et des titres génériques. Il précède la bibliothèque de personnages.
