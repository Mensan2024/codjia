# Étape 6.1 — Compréhension de la scène et registre de scènes (version 2.10.0)

Objectif validé le 27/11/2026 : que le moteur *comprenne* ce que chaque scène explique et choisisse une
représentation adaptée au contenu, au lieu de plaquer l'animation disponible. Les sujets différents doivent
donner des scènes différentes, visuellement et dans leur déroulé.

## Chaîne de traitement

```
texte de l'auteur ──► scènes (inchangées)
                         │
              ContentAnalyzer (famille, objectif, sensibilité : sur le texte d'ORIGINE, portés par Storyboard.profile)
                         │
                IntentAnalyzer ──► SceneIntent (une par scène)
                         │
                SceneRegistry : genres de scènes → plans (Beat)
                         │
                ScenarioPlanner : affectation globale (algorithme hongrois)
                         │
                Look.SCENARIO (Demo3DLook) : dessine le plan dans la place que le texte laisse
```

### SceneIntent (Intent.kt)

| Champ | Contenu |
|---|---|
| domaine, concept (+ concepts secondaires) | hameçonnage, force brute, logiciel malveillant, pare-feu, topologie, segmentation (VLAN), client-serveur, gratitude, stress, tunnel de vente |
| rôle pédagogique | ouverture, définition, reconnaître, mécanisme, procédure, conseil/parade, contraste, mesure, à retenir |
| acteurs, objets, action | attaquant, victime, commutateur, routeur, serveur… ; paquet, lien, mot de passe, VLAN… ; filtrer, cliquer, respirer, remercier… |
| contexte, relation | personnel / professionnel / réseau ; demande-réponse, étapes qui s'enchaînent, contraste, élément central relié… |
| objectif de la scène | comprendre le mécanisme, reconnaître la menace, apprendre le réflexe, suivre la procédure, retenir… |

L'analyse se fait par lexiques pondérés sur le texte sans accents ; le score d'une scène s'additionne à la moitié
de celui de la vidéo entière, pour que « Capte / Convertis » soit compris dans un texte de marketing même sans le mot « tunnel ».
Un concept n'est admis que dans les familles où il a un sens (« stress » dans un cours réseau n'est pas un sujet d'apaisement).

### Registre (SceneRegistry.kt, Scenes*.kt)

Dix genres, ~45 plans. Chaque plan déclare : rôles et objectifs servis, indices (cues) qu'il sait illustrer, s'il est
« doux » (admis pour les sujets sensibles), s'il peut se répéter, sa durée minimale, les concepts auxquels il est réservé,
la fenêtre utile du cadre virtuel 1000 × 700 (`y0`, `vh`).

Note d'adéquation = 0,25 concept + 0,35 rôle + 0,30 recouvrement des indices + 0,10 objectif. En dessous de 0,60, la
scène reste **sans image** (texte seul) : c'est préférable à une illustration hors sujet.

### Planificateur (ScenarioPlanner)

Affectation de poids maximal (Kuhn-Munkres) de toutes les scènes de la vidéo aux plans, en une fois :
- un plan ne sert qu'une fois dans une vidéo (les rares plans répétables existent en deux exemplaires, le second moins bien noté, et jamais deux scènes de suite) ;
- une scène ne « vole » pas le plan dont la suivante avait davantage besoin (défaut du choix scène par scène) ;
- sujets sensibles : seuls les plans doux entrent en compétition ;
- ex æquo tranchés par la graine : deux versions d'un même texte peuvent différer sans devenir incohérentes.

## Ce que cela donne (graine 7, `Bench plan … scenario`)

| Sujet | Plans retenus, dans l'ordre |
|---|---|
| Hameçonnage (SMS) | bait → anatomy → verify → direct → recover → recap |
| Force brute | guess → attack3d/attack → monitor → layers |
| Pare-feu | filter → zones → attack3d/defense → rules → recap |
| Topologie réseau | star → path → recap-star |
| Client-serveur | request → status → layers → recap |
| Gratitude | journal → bloom → share → recap |
| Stress | tangle → walk → breath → talk → recap |
| Marketing (tunnel) | funnel → loop → stages → leak → recap |

## Correction « le texte commande l'image » (27/11)

Constat : une « méthode en 4 points » recevait un ordinateur portable éclaté, un workflow un bouclier : l'objet suivait le domaine, pas le passage.

- **SemanticGate** : `DEMO3D` n'est proposé que si le texte cite un objet que le volume sait montrer ; `ATTAQUE` que s'il cite une attaque ET un objet ; `SCENARIO` que si un concept est reconnu. Les plans 3D du registre ont des indices obligatoires (`needs`).
- **Concepts METHOD et WATER** avec leurs plans : `method/mission` (escalier : rôle, contexte, tâche, format attendu, tirés des mots de l'auteur), `method/flow` (chaîne d'étapes, jeton sur un rail), `method/recap` ; `water/village`, `distance`, `effects`, `recap`.
- **Contrôle qualité** (`QualityControl.audit`, `report`, `passes`) : pertinence, objets, concept, durée, répétition, sobriété, texte. Un défaut bloquant laisse la scène en texte seul.
- **Direction artistique** : palettes par contenu ; décor « Aube » sans demi-sphère ; schéma centré avec le texte.

Textes d'essai (`engine-core/src/test/resources/textes/test_a…e`) : A méthode → `method/mission, recap` ; B gratitude → `gratitude/evening, recap` ; C hameçonnage → `phishing/mail, recap` ; D workflow → `method/flow, recap` ; E eau → `water/village, recap`.

## Honnêteté et limites

- Tout est vectoriel / 3D logicielle à facettes, dessiné par le moteur : **aucun personnage réaliste, aucune photo**. Les personnages sont des silhouettes stylisées (étape 6.3 à venir).
- Chaque scène simulée porte son étiquette : SIMULATION, EXEMPLE FICTIF, SCHEMA PEDAGOGIQUE ou ILLUSTRATION QUALITATIVE. Aucun chiffre ni témoignage inventé.
- Un texte de marketing sans vocabulaire de tunnel (ex. `marketing_linkedin.txt`) ne reçoit **pas** de scène du registre : texte seul. Les genres couvrent dix concepts, pas tous les sujets.
- La compréhension est lexicale (français) : elle ne remplace pas un modèle de langage. Un sujet hors des dix concepts retombe sur les formats existants.
- Mesures sur JVM (1080 × 1920, 30 fps) : 21–27 ms par image en moyenne, p95 41–51 ms, tas ≤ 81 Mo, comparable à « Démonstration 3D » (24,5 ms) et inférieur à « Attaque et défense 3D » (35,8 ms) sur le même texte. **Aucune mesure sur appareil Android** à ce stade ; la compatibilité Web est validée par la CI (compilation Kotlin/JS), pas par un rendu navigateur mesuré.
- Les cinq textes d'essai tiennent en une ou deux phrases : deux scènes (titre + appel à l'action), pas de scènes inventées.
- Le plan 3D « attack » (hérité de 2.9.0) déborde légèrement à gauche sur certains cadrages : non corrigé ici.

## Extensibilité (décision 2 du 27/11)

Un plan est une fonction `StageCtx → dessin` sur l'abstraction `Gfx` : l'ajout ultérieur d'images personnelles importées, de
bibliothèques de ressources locales ou de modèles sous licence compatible se fera par de nouveaux plans ou par un nouveau
primitif de `Gfx`, sans toucher à l'analyse ni à l'affectation. Rien n'est importé dans cette étape.

## Tests

`ScenarioTest` : genre dominant correct pour chacun des 8 sujets, séquences deux à deux différentes, ≥ 6 genres distincts,
aucun plan répété (3 graines), concepts reconnus, sujet sensible limité aux plans doux avec texte conservé, rapport lisible.
`UnderstandingTest`, `RenderTest` : conservation du texte, zone de sécurité et chevauchements pour tous les formats, dont SCENARIO.
