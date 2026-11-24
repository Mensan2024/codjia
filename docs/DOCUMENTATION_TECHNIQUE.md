# CODJIA — Documentation technique du moteur vidéo (version 0.3)

## 1. Vue d'ensemble

```
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

Les titres s'écrivent avec des `*astérisques*` autour des mots à mettre en valeur, par exemple « Repère un *e-mail piégé* ».

Les transitions tournent selon la liste du template, sans jamais se répéter deux fois de suite (vérifié par un test).

Chaque scène reçoit une légère variation de teinte de fond. Les couleurs de texte respectent un contraste d'au moins 7:1 pour le texte principal et 4,5:1 pour le texte secondaire (vérifié par un test).

**Ajouter un template.** Il suffit de créer une valeur `Template` dans `Templates.kt` et, si besoin, un `Decor`. Le reste du moteur n'est pas modifié.

## 4. Recommandation de templates

`DomainDetector` compte des mots-clés français (sans accents, par racine) dans 6 domaines : cyber, santé, agro, marketing, entreprise et éducation. Le domaine le plus fort est retenu s'il a au moins 2 occurrences ; sinon, le texte est classé « Général ».

`recommend()` classe les **8** templates du plus adapté au moins adapté. Aucun n'est retiré : l'utilisateur garde le libre choix.

Cette détection fonctionne hors ligne et s'explique simplement. Le mode IA pourra l'affiner en phase D.

## 5. Musique

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

## 6. Signature et installation

- **Clé de développement fixe :** `demo-app/codjia-debug.keystore`, mot de passe `android`. Elle est volontairement publique et ne sert qu'aux tests. Elle permet d'installer chaque nouvel APK par-dessus le précédent.
- **Clé de publication :** celle de Google Play et de F-Droid sera différente et ne sera jamais versionnée.

## 7. Compiler et tester

```
./test.sh                                    # 38 tests, sans réseau (compilateur Kotlin de Gradle)
./gradlew :engine-core:test :engine-jvm:test :demo-app:assembleDebug
./gradlew :engine-jvm:run --args="templates out/captures"
./gradlew :engine-jvm:run --args="render out/demo.mp4 cyber-neon pulse 1080"
tools/music/build_music.sh                   # régénère les 8 musiques
```

À chaque envoi sur GitHub, l'intégration continue lance les tests et produit l'APK, disponible dans l'onglet *Actions*.
