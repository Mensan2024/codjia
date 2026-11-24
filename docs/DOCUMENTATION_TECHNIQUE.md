# CODJIA — Documentation technique du moteur vidéo (version 1.1)

## 1. Vue d'ensemble

```
Texte libre ──► CreativeDirector.propose(texte, graine, templates récents)
                  ├─ TextSanitizer   : nettoyage, 5 000 caractères au plus
                  ├─ DomainDetector  : domaine + templates recommandés
                  └─ StoryboardPlanner.plan : scènes, compositions, durées ≤ 40 s
Contenu (Storyboard)  ──►  Composer.prepare(contenu, template, musique)
                              ├─ Templates.apply  : habillage (transitions, identité)
                              └─ Rhythm.align     : début des scènes calé sur le tempo
                         ──►  FrameRenderer.render(gfx, t)   ← même fonction pour l'aperçu et l'export
                                 ├─ Backgrounds   : décor animé du template
                                 ├─ SceneRenderer : 6 compositions + titres animés
                                 └─ transitions + indicateur de progression + pied de page
Android : AndroidCanvasGfx ─► Media3 Transformer (OverlayEffect) ─► MP4 H.264
Musique : AudioDecoder (MediaCodec) ─► AudioMix.musicBed (fondus, volume, durée) ─► WAV ─► AAC dans le MP4
```

Le moteur (`engine-core`) est du Kotlin pur. Il ne dépend ni d'Android, ni de l'interface, ni d'une horloge, et il est entièrement testable sur une JVM. L'interface ne fait qu'appeler `Composer` et `FrameRenderer`.

## 2. Modules

| Module | Rôle |
| --- | --- |
| `engine-core` | Modèle, chronologie, animations, texte, templates, musique (mixage et rythme), détection du domaine |
| `engine-jvm` | Banc d'essai : rendu Java2D et encodage ffmpeg du poste. Sert aux tests, aux captures et à la vidéo de démonstration. Jamais livré |
| `engine-android` | Canvas Android, export Media3, décodage audio, catalogue musical, relecture du MP4, Galerie |
| `demo-app` | Application de test du parcours de création (ce n'est pas encore l'application complète) |
| `tools/music` | Composition et encodage des musiques originales |

## 3. Templates

Un `Template` réunit toute une identité :

- une palette (`Style`) ;
- une police de titre et une police d'étiquette ;
- un mouvement de titre (`TitleMotion`) et une mise en valeur des mots (`Emphasis`) ;
- un style de carte (`CardStyle`) ;
- un décor animé (`Decor`) ;
- une liste de transitions et leur durée ;
- l'animation d'entrée des listes ;
- les effets cinéma (bandes noires, grain).

| Template | Police des titres | Mouvement des titres | Mise en valeur | Décor | Transitions |
| --- | --- | --- | --- | --- | --- |
| Cyber Neon | Inter Display | Mots qui montent | Halo néon | Grille, circuits, impulsions, balayage | Volets, zoom, glissement, fondu |
| Corporate Premium | Gloock (serif) | Lignes qui sortent d'un masque | Soulignement doré | Filets dorés, arcs | Fondu, glissement, passage au noir |
| Modern Education | Outfit | Mots qui rebondissent | Surligneur jaune | Formes géométriques flottantes | Cercle, glissement, volets |
| Green Nature | Bricolage Grotesque | Mots qui montent | Soulignement | Rayons de lumière, feuilles, pollen | Fondu, cercle, glissement |
| Medical Clean | Instrument Sans | Mots qui montent | Soulignement | Ondes, croix, tracé cardiaque | Cercle, fondu, glissement |
| Marketing Impact | Big Shoulders (majuscules) | Mots qui rebondissent | Bloc de couleur | Blocs diagonaux, étoiles | Volets, zoom, glissement |
| Bold Typography | Boldonse (majuscules) | Lignes qui sortent d'un masque | Bloc jaune | Mots géants qui défilent | Glissement, volets, zoom |
| Cinematic Story | Instrument Serif | Lettres en fondu | Soulignement | Fuite de lumière, vignette, grain, bandes cinéma | Passage au noir, zoom, fondu |
| Network Blueprint *(0.4)* | Tektur | Lignes qui sortent d'un masque | Soulignement | Plan technique : quadrillage, topologie, paquets qui circulent | Volets, glissement, cercle, fondu |
| Code Studio *(0.4)* | JetBrains Mono | Lettres en fondu | Soulignement | Éditeur de code : numéros de ligne, coloration, curseur | Glissement, volets, fondu, zoom |

Les titres s'écrivent avec des `*astérisques*` autour des mots à mettre en valeur, par exemple « Repère un *e-mail piégé* ».

Les transitions tournent selon la liste du template, sans jamais se répéter deux fois de suite (vérifié par un test).

Chaque scène reçoit une légère variation de teinte de fond. Les couleurs de texte respectent un contraste d'au moins 7:1 pour le texte principal et 4,5:1 pour le texte secondaire (vérifié par un test).

**Ajouter un template.** Il suffit de créer une valeur `Template` dans `Templates.kt` et, si besoin, un `Decor`. Le reste du moteur n'est pas modifié.

## 4. Domaines et recommandation

CODJIA se concentre sur l'informatique, l'IA et quelques domaines voisins. `DomainDetector` attribue un score pondéré à des mots-clés (sans accents, par racine) dans 8 domaines :

| Domaine | Exemples de mots-clés | 3 templates proposés (texte de test) | Musiques proposées |
| --- | --- | --- | --- |
| Réseaux (CCNA, CCNP) | ccna, vlan, ospf, bgp, hsrp, trunk, mikrotik | Network Blueprint, Cyber Neon, Corporate Premium | pulse, horizon, momentum |
| Systèmes (Windows, Linux, macOS, Android) | powershell, systemd, macos, sudo, active directory | Network Blueprint, Code Studio, Corporate Premium | pulse, lofi, horizon |
| Cybersécurité | phishing, rdp, mfa, malware, chiffrement, fail2ban | Cyber Neon, Bold Typography, Network Blueprint | pulse, momentum, elan |
| IA | prompt, llm, chatgpt, hallucination, deep learning | Bold Typography, Cyber Neon, Code Studio | elan, pulse, horizon |
| Fullstack | javascript, react, api, sql, git, docker | Code Studio, Cyber Neon, Network Blueprint | lofi, pulse, momentum |
| Santé et longévité | sommeil, longévité, activité physique, stress | Medical Clean, Green Nature, Cinematic Story | aube, canopee, sahel |
| Marketing | linkedin, audience, branding, seo, conversion | Marketing Impact, Corporate Premium, Bold Typography | momentum, horizon, elan |
| Anglais | english, present perfect, grammar, pronunciation | Modern Education, Marketing Impact, Corporate Premium | lofi, elan, sahel |

Le domaine n'est retenu qu'à partir d'un score de 3. En dessous, le texte est classé « Général ». `recommend()` classe les **10** templates sans en retirer aucun : l'utilisateur garde le libre choix. Les mots-clés du domaine servent aussi à choisir le mot mis en valeur dans les titres.

## 5. Du texte libre à la vidéo (`Planner.kt`)

1. **Nettoyage** (`TextSanitizer`). 5 000 caractères au plus. On retire les emojis et les caractères de contrôle et on garde une liste blanche (latin jusqu'à U+017F, ponctuation). Les marqueurs ❌ et ✅ sont lus **avant** le nettoyage pour repérer les comparaisons.
2. **Analyse** (`TextAnalysis`). Titre « Astuce NNN — … », accroche, définition, listes (puces et en-têtes terminés par « : »), conclusion (« à retenir », « en résumé », « remember »), hashtags.
3. **Planification** (`StoryboardPlanner.plan`). Couverture, puis définition, chiffre clé (nombre avec unité, ports entre parenthèses), étapes, comparaison et « à retenir ». La durée de chaque scène vient du temps de lecture (2,7 mots/s), bornée selon la composition. Si le total dépasse 38,5 s, on retire d'abord les scènes facultatives, puis on resserre.
4. **Typographie française.** Espace insécable avant `: ; ! ?` et dans les guillemets. Les commandes (`git`, `df`…) ne prennent pas de majuscule. Les éléments sont découpés en « libellé : détail ».

## 6. Une vidéo différente à chaque génération

Chaque génération reçoit une **graine** de 64 bits tirée par `SecureRandom`. Le bouton « 🔀 Une autre version » en tire une nouvelle. Les tirages dérivent de la graine par SplitMix64 (`Rand.of(graine, usage)`), si bien que deux graines voisines donnent des résultats indépendants. La graine fait varier :

| Niveau | Ce qui change |
| --- | --- |
| Template | Choisi parmi les 3 meilleurs du domaine, en **évitant les 2 derniers utilisés**. Si l'utilisateur choisit un template, il est conservé |
| Compositions | Chacune des 6 compositions a 2 variantes : couverture classique ou « titre d'abord », définition en carte ou en citation, étapes en frise ou en cartes, chiffre en anneau ou en jauge, comparaison empilée ou « versus » barrée, « à retenir » centré ou en cartes |
| Ordre | L'ordre des scènes intermédiaires peut changer (étapes avant ou après le chiffre, par exemple) |
| Habillage | Couleurs d'accent permutées, variante de mouvement des titres, rotation des transitions décalée |
| Textes | Formulation des intertitres (« À retenir », « L'essentiel », « En résumé ») et de l'appel à l'action |
| Musique | Tirée parmi les 3 pistes du domaine, sauf si l'utilisateur en a choisi une ou en a importé une |

Le test `VariationTest` exige au moins 6 rendus distincts sur 8 graines, et jamais deux fois de suite le même template. Avec la même graine, on obtient exactement la même vidéo, ce qui rend les rendus reproductibles. La graine 0 donne le rendu de référence sans variation, utilisé pour les captures.

## 6 bis. Cinq formats de réalisation (`Looks.kt`)

Le même storyboard peut être mis en scène de cinq façons très différentes. `CreativeDirector` choisit le format à chaque génération en évitant les deux derniers. L'utilisateur peut aussi l'imposer.

| Format | Fichier | Mise en scène |
| --- | --- | --- |
| Motion design | `SceneRenderer.kt` | Cartes animées, typographie cinétique, 10 modèles de couleurs |
| Tableau blanc | `WhiteboardLook.kt` | Une main (illustration originale, 4 teintes de peau) tient un feutre. Elle écrit le texte (police Kalam, OFL) et dessine des croquis tracés trait par trait : icônes, cercles, flèches, cases cochées, ratures. Le contenu est centré verticalement en deux passes |
| Démo à l'écran | `DemoLook.kt` | Fenêtres de terminal ou de réglages, frappe au clavier, curseur et clics, interrupteurs. Réservé aux domaines informatiques |
| Personnage animé | `MascotLook.kt` | Codi, robot original : clignement des yeux, regard, bouche synchronisée sur le texte, bras qui pointe ou salue, humeurs (neutre, inquiet, content), bulles |
| Éditorial chic | `EditorialLook.kt` | Mise en page de magazine : serif, italique d'accent, lettrine, filets animés, zoom lent, grain. Palettes ivoire, noir ou marine |

Toutes les mises en page de tous les formats sont vérifiées par `RenderTest` (15 textes × 3 graines × 5 formats) : aucun texte hors de la zone sûre, aucun chevauchement.

## 6 ter. Démonstrations sur écran simulé (`Simulator.kt`, `Procedures.kt`)

Quand le texte parle d'un sujet de la bibliothèque, la vidéo contient une **vraie démonstration pas à pas**. Les sujets actuels sont le Bureau à distance (RDP), le pare-feu, les mises à jour et les VLAN.

- **Systèmes simulés.** Bureaux Windows 11, macOS et Ubuntu 24.04, plus une console Cisco IOS, dessinés en vectoriel dans un espace de 1600 × 1000, sans logo de marque. On y trouve :
  - le menu Démarrer, Spotlight ou Activités, avec la recherche tapée ;
  - les fenêtres Paramètres, Sécurité Windows, Réglages Système et Paramètres GNOME ;
  - le terminal.
- **Caméra.** Elle zoome sur la zone utile et reste continue d'une étape à l'autre : coupes franches, sans fondu entre les étapes. Le curseur se déplace et clique, avec une onde de clic, et les interrupteurs basculent.
- **Libellés.** Les noms des menus et des réglages reprennent ceux des interfaces françaises.
- **Commandes.** Les sorties sont celles des outils réels (ufw, apt, IOS). Les compteurs, comme le nombre de paquets, sont des exemples réalistes. Aucune sortie n'est fabriquée à partir du texte de l'utilisateur.
- **Choix du système.** C'est celui que nomme le texte. Sinon, la graine en choisit un, ce qui fait qu'une autre version peut montrer Linux au lieu de Windows.
- **Durée.** Couverture, puis (définition), étapes de la démo, (comparaison) et « à retenir », entre 35 et 45 s. Les démos des textes courts sont un peu ralenties pour atteindre 35 s.

Ajouter un sujet revient à écrire un `Topic` dans `Procedures.kt` : mots-clés, puis, pour chaque système, des étapes (titre, légende, écran, action).

## 7. Musique

| Étape | Où | Détail |
| --- | --- | --- |
| Catalogue | `assets/music/music.json` | Titre, catégorie, tempo, durée, licence, origine, forme d'onde (48 valeurs) |
| Écoute avant choix | `MediaPlayer` | Lecture et pause, progression affichée sur la forme d'onde |
| Montage au rythme | `Rhythm.align` | Chaque scène commence sur un temps (écart ≤ 1 ms). La vidéo ne dépasse jamais 40 s |
| Décodage | `AudioDecoder` | MediaExtractor + MediaCodec vers du PCM 16 bits |
| Mixage | `AudioMix.musicBed` | Coupe à la durée exacte, fondu d'entrée de 0,25 s, fondu de sortie de 1,8 s en courbe douce. Une piste plus courte que la vidéo est rebouclée avec un fondu enchaîné d'1 s |
| Volume | `MusicVolume` | Normal (×1), Réduit (×0,45), Coupé (pas de piste) |
| Encodage | Media3 Transformer | AAC dans le MP4, deux séquences parallèles (vidéo + audio) qui démarrent à 0 |
| Contrôle | `Mp4Inspector` | Si une musique a été choisie et que le MP4 n'a pas de piste audio, l'export est refusé avec un message |

## 8. Signature et installation

- **Clé de développement fixe :** `demo-app/codjia-debug.keystore`, mot de passe `android`. Elle est volontairement publique et ne sert qu'aux tests. Elle permet d'installer chaque nouvel APK par-dessus le précédent.
- **Clé de publication :** celle de Google Play et de F-Droid sera différente et ne sera jamais versionnée.
- **Version de publication :** `assembleRelease` produit un APK non signé, réduit et obscurci par R8. La CI le compile et vérifie ses permissions à chaque envoi.

La sécurité est décrite dans [SECURITE.md](SECURITE.md) : modèle de menaces, limites d'import, manifeste sans réseau, CodeQL et Dependabot.

## 9. Compiler et tester

```
./test.sh                                    # 56 tests, sans réseau (compilateur Kotlin de Gradle)
./gradlew :engine-core:test :engine-jvm:test :demo-app:assembleDebug
./gradlew :engine-jvm:run --args="templates out/captures"
./gradlew :engine-jvm:run --args="render out/demo.mp4 cyber-neon pulse 1080"
./gradlew :engine-jvm:run --args="plan astuce.txt 42"          # storyboard proposé pour la graine 42
./gradlew :engine-jvm:run --args="video astuce.txt 42 out.mp4" # vidéo complète avec musique
tools/music/build_music.sh                   # régénère les 8 musiques
```

À chaque envoi sur GitHub, l'intégration continue lance les tests et produit l'APK, disponible dans l'onglet *Actions*.
