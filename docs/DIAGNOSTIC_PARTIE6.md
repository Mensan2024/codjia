# Partie 6 — Diagnostic du moteur avant refonte (état au 27/11/2026, version 2.9.0)

Ce document répond à l'audit exigé avant toute modification d'architecture. Il décrit l'état réel du code, ses limites, la cause de la répétition visuelle, et propose une architecture et un ordre de travail. Rien n'est promis ici qui ne soit réalisable avec les technologies actuelles du projet.

## 1. Réponses à l'audit

| Question | Réponse vérifiée dans le code |
|---|---|
| Comment les scènes sont-elles générées ? | `StoryboardPlanner.plan(texte, graine, modèle)` (Planner.kt, 666 lignes) découpe le texte de l'auteur en scènes : couverture, définition, étapes, chiffre, comparaison, liste de conclusion. **Il ne reçoit ni la famille de contenu, ni le format choisi.** Le storyboard est donc le même pour un texte de phishing, de santé mentale ou de marketing. |
| Combien de types de scènes ? | **Sept** : `Layout { HERO, DEFINITION, STEPS, COMPARE, STAT, CHECKLIST, SCREEN }`. Toute vidéo est une suite de ces sept types. |
| Comment les styles sont-ils choisis ? | `ContentAnalyzer` (lexique pondéré, hors ligne) → `ContentProfile` (famille, ton, objectif) → `ArtDirector.auto` choisit un format (`Look`) dans un catalogue par famille. La compréhension agit **sur le choix du format, de l'ambiance et de la musique, pas sur le contenu des scènes**. |
| Comment les animations sont-elles produites ? | Courbes d'accélération (`Easing`), apparitions par `Pose` (alpha, décalage, échelle), fenêtres temporelles (`window`). Chaque format les code à sa façon, en dur, scène par scène. |
| Comment les images sont-elles associées au texte ? | **Elles ne le sont pas.** Le moteur n'a aucune primitive d'image (ni photo ni vidéo). Le lien texte → visuel se limite à 31 pictogrammes vectoriels (`IconId`), 14 illustrations (`IllustrationId`), les schémas d'`InfographicLook` choisis par expressions régulières, et les démonstrations `Procedures` (fenêtres Windows, macOS, Ubuntu, console réseau). |
| Comment les transitions sont-elles réalisées ? | 7 types (`CUT, FADE, SLIDE, ZOOM, WIPE, CIRCLE, DIP`), appliqués dans `FrameRenderer.transition` ; chaque format n'en autorise que 2 ou 3 (`Look.transitions`). |
| Comment la vidéo est-elle exportée ? | `Composer.prepare` (habillage, calage sur le tempo) → `FrameRenderer.render(g, t)` dessine chaque image via l'interface `Gfx`. Android : image dessinée dans un `Bitmap`, appliquée par Media3 Transformer. Web : canevas + WebCodecs `VideoEncoder` + `Mp4Muxer`. Tests : JVM + ffmpeg. |
| Les thèmes utilisent-ils des moteurs distincts ? | **Partiellement.** Il y a 14 classes de rendu pour 26 types. Mais **chacune contient le même `when (sc.layout)` sur les 7 mêmes types de scènes**. Ce sont des habillages différents d'un même scénario (cartes, listes, titres), pas des moteurs de mise en scène. Le seul rendu à part est `ScreenSceneRenderer` (simulations de fenêtres). |
| Le moteur a-t-il des capacités 3D ? | **Oui depuis la 2.8.0** : `Engine3D.kt`, un moteur 3D logiciel (perspective, faces cachées, éclairage par face, ombre au sol), avec trois objets et une scène d'attaque/défense. Avant cela, aucune. |
| Pourquoi des sujets différents donnent-ils des animations similaires ? | Voir section 2. |

## 2. Cause racine de la répétition

1. **Le planificateur est aveugle au domaine.** Le même découpage en 7 types de scènes sort pour tous les sujets. Seules quelques retouches de titres existent (famille biblique, absence d'appel à l'action pour les sujets sensibles).
2. **Les formats ne choisissent pas ce qu'ils montrent.** Un format reçoit une scène « ÉTAPES » et dessine une liste ; il ne sait pas que l'étape décrit un filtrage, un tunnel ou une émotion.
3. **Le lien sens → image repose sur des expressions régulières locales** (`diagramFor` dans InfographicLook, `Procedures.match`), seulement dans deux formats, et sur une trentaine de sujets prévus.
4. **La variation est cosmétique** : palette, graine, variante 0/1 d'une même composition (`Scene.variant`), jamais un autre scénario visuel.
5. **Aucune notion de personnage, de lieu, d'objet ou d'action** n'existe dans le modèle de données : une scène est du texte plus un pictogramme.

Parties du code responsables : `Planner.kt` (plan sans domaine), `Model.kt` (`Layout` à 7 valeurs, `Scene` sans intention visuelle), le `when (sc.layout)` répété dans chaque `*Look.kt`, et `ArtDirection.kt` (choix d'un format seulement).

## 3. Limites techniques à ne pas masquer

| Demande de la partie 6 | Réalité |
|---|---|
| Three.js, WebGL, WebGPU | Réalisables **dans la version web seulement** (Kotlin/JS). L'application Android est en Kotlin natif avec Canvas et Media3 : il n'y existe pas de Three.js. Deux moteurs 3D différents donneraient des vidéos différentes, non testables par la suite automatique actuelle. Le moteur 3D logiciel déjà écrit tourne identiquement partout. **Recommandation : le garder et l'enrichir ; envisager plus tard un accélérateur GPU facultatif pour le web.** |
| Modèles GLB/GLTF | Il faudrait (a) un lecteur glTF en Kotlin, (b) des modèles sous licence libre tracée, (c) un rendu avec tampon de profondeur : l'algorithme actuel (tri des solides) est faux pour les formes creuses non convexes. Faisable, coûteux, et lourd pour le téléphone. Non recommandé en première étape. |
| Personnages 3D réalistes | **Non faisable** sans modèles animés externes. Faisable : personnages 3D **stylisés à facettes** construits par le moteur (corps, tête, membres hiérarchiques ; marche, geste de pointer, bouche, expressions) et personnages **2D vectoriels**. Continuité garantie par une fiche de personnage figée par série. |
| Documentaire réaliste, médias importés | Contredit ta réponse du 26/11 (« non » à l'import d'images et de vidéos). Sans médias réels, le « documentaire » sera infographique et illustré. **À reconfirmer.** |
| Anatomie, organes, molécules | Molécules et structures simples : faisables (sphères et liaisons). Organes : seulement des représentations très simplifiées et étiquetées « schéma », jamais une source médicale. |
| Environnements (villages, eau potable, paysages) | Faisables en 3D stylisée procédurale (maisons, arbres, puits, pompes, terrain) et en 2D en couches. Pas de réalisme photo. |
| Systèmes de particules | Faisables (poussières, étincelles, paquets, pluie, eau). |
| Narration synchronisée scène par scène | Les durées de scène suivent déjà le texte ; une synchronisation fine au mot près dépend de la voix de l'appareil (non mesurable à l'avance). |
| Tout cela sur téléphone | Le coût de rendu doit être mesuré sur appareil réel : 30 ms par image sur ordinateur pour la scène d'attaque ; non vérifié sur Android. |

## 4. Architecture proposée : un « scénario visuel » entre le texte et le rendu

```
Texte de l'auteur ──► Planner (inchangé : texte intact, même ordre)
                         │
                         ▼
            Analyse sémantique par scène  ──►  SceneIntent
            (concept, relation, acteurs, lieu, émotion)
                         │
                         ▼
            Registre de types de scènes (SceneKind)
            chacun : « sait-il montrer cette intention ? » + son rendu
                         │      ex. FILTRAGE_RESEAU, ATTAQUE_DEFENSE, TOPOLOGIE,
                         ▼          FLUX_CODE, VUE_ECLATEE, TUNNEL, PERSONNAGE_SITUATION,
            Réalisation (RealisationPlan)    PAYSAGE, PRODUIT_TOURNANT, PROGRESSION...
            caméra, mouvement, transition, durée
                         │
                         ▼
            Rendu (Gfx 2D + Engine3D)  →  montage, audio, export (inchangés)
```

Principes :
- **Le texte de l'auteur reste intact et dans l'ordre** : `SceneIntent` s'ajoute à `Scene`, ne remplace rien.
- **Une famille de production (A à H) = un jeu de types de scènes autorisés + un langage de caméra, de rythme et de lumière**, pas un habillage d'un même `when`.
- **Choix par le sens du passage**, avec une règle de diversité : jamais deux scènes consécutives du même type quand une alternative adaptée existe ; variantes d'angle, de décor et de séquence tirées de la graine, **parmi les seules variantes admissibles pour le sens** (aucun mouvement sans rapport avec le sujet).
- **Repli** : si aucune scène spécialisée ne sait montrer une intention, on retombe sur les types actuels. Rien ne casse.
- **Étiquetage** : toute scène simulée ou illustrée est marquée ; aucun témoignage, chiffre ou victime inventés.

## 5. Ordre de réalisation proposé (pas tout en même temps)

| Étape | Contenu | Résultat vérifiable |
|---|---|---|
| **6.1** | `SceneIntent` + registre de types de scènes + sélection par sens, branchés sur le planificateur sans changer le texte. Premiers types : filtrage réseau (ton exemple du pare-feu : paquets autorisés passent, interdits bloqués, règle visible), attaque/défense, topologie réseau, flux client-serveur. | Textes de cybersécurité, réseau et code : scénarios visuellement différents ; tests de conservation du texte inchangés. |
| **6.2** | Bibliothèque 3D élargie (routeur, commutateur, pare-feu, nuage, carte mère, molécules, produit en rotation) ; réalisateur de caméra (orbite, travelling, plongée, plan serré) ; particules. | Images de contrôle par type ; mesure de coût de rendu. |
| **6.3** | Personnages stylisés 3D et 2D, fiche de personnage figée, marche/parole/pointage/émotions ; série avec personnage récurrent. | Même personnage reconnaissable sur plusieurs scènes et plusieurs vidéos. |
| **6.4** | Environnements : village, point d'eau, paysage, salle informatique ; familles humanitaire, biblique, développement personnel. | Vidéos de test des huit sujets de la partie 5. |
| **6.5** | Familles A à H complètes, sélecteur et prévisualisation. | Tableau de diversité : même texte, deux familles, deux scénarios différents. |
| **6.6** | Synchronisation fine, ambiances sonores libres de droits, mesures sur appareils réels. | Rapport de performance. |

## 6. Décisions que toi seul peux prendre

1. **Moteur 3D** : on garde et on enrichit le moteur logiciel commun (recommandé), ou on ajoute un moteur Three.js/WebGL pour le web seulement ?
2. **Documentaire réaliste** : tu maintiens « non » à l'import de photos et de vidéos (alors le documentaire sera infographique et illustré), ou tu autorises l'import par l'utilisateur ?
3. **Personnages réalistes** : acceptes-tu des personnages stylisés (3D à facettes et 2D vectoriels) à la place du réalisme ?
4. **Anatomie** : acceptes-tu des schémas simplifiés étiquetés, sans prétention médicale ?
