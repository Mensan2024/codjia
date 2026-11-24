# IT_MENTOR — Rapport de validation du prototype de moteur vidéo

Date : 23 novembre 2026 · Version : 0.1-prototype · Licence : GPL-3.0-or-later

## 1. Verdict

Le moteur fonctionne. Il a produit une vraie vidéo MP4 animée de **31,8 s** à partir d'un storyboard de 6 scènes. Elle est en H.264 High, 1080 × 1920 et 30 i/s, et contient 954 images.

Les 26 tests automatiques passent.

**Limite à lever avant la phase 2 :** la partie Android (rendu sur Canvas et export Media3) est écrite, mais elle n'a **ni été compilée ni été exécutée**. Mon espace de travail n'a accès ni au SDK Android, ni à Google Maven, ni à un téléphone. Les mesures sur téléphone restent donc à faire (section 7).

| Exigence | Résultat | Preuve |
| --- | --- | --- |
| Format vertical 9:16, 1080 × 1920 | ✅ 1080 × 1920 | ffprobe |
| Durée ≤ 40 s, avec refus au-delà | ✅ 31,8 s, refus testé | ffprobe, `refuseUneVideoDePlusDe40Secondes` |
| MP4, H.264/AVC | ✅ H.264 High, niveau 4.0, yuv420p | ffprobe |
| 30 i/s | ✅ 30/1 constant, 954 images = 31,8 s × 30 | ffprobe `-count_frames` |
| Résolution et fréquence adaptables | ✅ 720 × 1280 produit avec la même composition | `le720pEstLaMemeCompositionQueLe1080p`, vidéo 720p |
| Au moins 5 scènes | ✅ 6 scènes, 6 compositions différentes | storyboard `DemoStoryboard.phishing()` |
| 8 animations | ✅ toutes calculées à partir du temps de rendu | `huitAnimationsCalculeesSurLeTempsDeRendu` |
| 3 transitions | ✅ fondu, glissement, zoom (+ coupe franche) | images de transition vérifiées |
| Accents français | ✅ é è ê à ù ç œ « » ’ et espaces insécables | `lesPolicesContiennentTousLesCaracteresDuStoryboard` |
| Retours à la ligne et textes trop longs | ✅ coupure, réduction de la taille, « … », pas de mot seul en dernière ligne | 7 tests `TextLayoutTest` |
| Marges de sécurité | ✅ 197 textes vérifiés, tous dans la zone sûre | `toutLeTexteResteDansLaZoneSure` |
| Rendu déterministe | ✅ même instant = mêmes pixels | `rendreDeuxFoisLeMemeInstantDonneLaMemeImage` |
| 3 styles graphiques | ✅ Cyber, Santé, Agro | `IT_MENTOR_3_styles.png` |
| Export réel sur le stockage du téléphone | ⏳ code écrit (MediaStore › Movies/IT_MENTOR), pas encore exécuté | section 7 |

## 2. Vidéo produite (mesurée avec ffprobe)

| Mesure | 1080p (livrable) | 720p (format de repli) |
| --- | --- | --- |
| Fichier | `IT_MENTOR_prototype_phishing_1080p.mp4` | `IT_MENTOR_prototype_phishing_720p.mp4` |
| Codec | H.264, profil High, niveau 4.0, yuv420p | H.264, profil High |
| Résolution | 1080 × 1920 | 720 × 1280 |
| Fréquence | 30/1 (constante) | 30/1 |
| Durée | 31,800 s | 31,800 s |
| Images décodées | 954 | 954 |
| Débit moyen | 1,71 Mb/s | 1,13 Mb/s |
| Taille | 6,8 Mo (6 796 517 octets) | 4,5 Mo |
| Décodage complet | sans erreur (`ffmpeg -f null`) | sans erreur |

La vidéo n'a pas de piste audio : c'est la décision D8, pas de musique en v1.

Le débit de 1,7 Mb/s est bas parce que l'encodeur x264 est très efficace sur des aplats et du texte (CRF 17, qualité visuelle élevée). Sur le téléphone, l'encodeur matériel aura besoin de plus pour la même netteté. L'exporteur Android vise donc environ 7,5 Mb/s en 1080p.

## 3. Performances et mémoire (banc d'essai JVM)

Machine : conteneur cloud, 2 vCPU Intel Xeon 2,3 GHz, OpenJDK 21. Le rendu et l'encodage x264 « medium » tournent en même temps sur les 2 cœurs.

| Mesure | 1080p | 720p |
| --- | --- | --- |
| Temps total (rendu + encodage) | 30,4 s, soit 0,96 × la durée de la vidéo | 16,3 s, soit 0,51 × |
| Temps de rendu seul | 24,2 s | 13,7 s |
| Rendu d'une image : moyenne | 25,3 ms | 14,3 ms |
| Rendu d'une image : 95ᵉ centile | 45,7 ms | 25,9 ms |
| Rendu d'une image : maximum | 153,9 ms (premières images, préchauffage de la JVM) | 105,0 ms |
| Pic du tas Java | 81 Mo | 69 Mo |
| Pic de mémoire résidente, Java | 175 Mo | 156 Mo |
| Pic de mémoire résidente, encodeur ffmpeg | 307 Mo | 182 Mo |

**Ce que ces chiffres ne disent pas :** ils ne prédisent pas les temps sur un téléphone. Le rendu Android passe par android.graphics.Canvas (Skia) et l'encodage par l'encodeur matériel, et les deux se comportent différemment. Les mêmes mesures sont prévues sur l'appareil (section 7).

## 4. Tests automatiques : 26 sur 26 réussis

Ils ont été lancés avec `./test.sh` : compilateur Kotlin 2.0.21 et JUnit 4.13.2, sans réseau.

| Suite | Tests | Ce qui est vérifié |
| --- | --- | --- |
| `TimelineTest` | 6 | Durée 31,8 s = 954 images, chevauchement des transitions, refus au-delà de 40 s, refus d'une transition trop longue, identifiants uniques, pas de dérive du temps |
| `EasingAndAnimationTest` | 5 | Courbes bornées et monotones, les 8 animations, délais |
| `TextLayoutTest` | 7 | Largeur respectée, espaces insécables, mot trop long, accents, réduction puis « … », pas de mot seul en dernière ligne, machine à écrire stable |
| `PathTest` | 1 | Tracé progressif à la bonne longueur |
| `FormatTest` | 1 | Formats adaptables, dimensions impaires refusées |
| `RenderTest` | 6 | Déterminisme, images différentes à des instants différents, glyphes présents dans toutes les polices, zone sûre (197 textes), 3 styles, même composition en 720p et en 1080p |

**Vérification visuelle :** j'ai regardé 15 images clés en taille réelle, dont les 3 transitions. J'ai aussi regardé 6 images décodées depuis le MP4 final. Corrections faites à la suite de cette relecture : le fil de l'hameçon n'était pas aligné sur son œillet, les scènes 2 et 3 étaient trop tassées en haut, et le texte de la définition se terminait par un mot seul.

## 5. Choix techniques et justification

| Besoin | Choix | Pourquoi | Écarté |
| --- | --- | --- | --- |
| Moteur de motion design | Kotlin pur (`engine-core`), sans Android | On le teste sur n'importe quelle JVM, il est déterministe, et le même code sert à l'aperçu et à l'export | Moteurs de jeu : trop lourds, licences variables |
| Dessin | Interface `Gfx`, avec deux implémentations : Canvas Android et Java2D | Canvas (Skia) fait partie d'Android, ne demande aucune dépendance et gère bien le texte. OpenGL ES n'apporterait rien d'indispensable à ce stade | OpenGL ES : beaucoup plus de code pour le texte, à réévaluer si le Canvas logiciel est trop lent |
| Encodage sur Android | **Media3 Transformer** : image de fond + `OverlayEffect` + `BitmapOverlay`, export H.264 | Bibliothèque officielle de Google, activement maintenue (1.10 en mars 2026, 1.11 en août 2026), licence Apache 2.0. Elle gère l'horodatage exact, l'encodeur matériel, le repli de résolution et le MP4 | **MediaCodec + `lockHardwareCanvas`** : l'horodatage des images y suit l'horloge murale et non le temps de la vidéo, il faudrait donc passer par EGL. Ce chemin reste le plan B |
| Encodage dans le banc d'essai | ffmpeg (x264) du poste, appelé comme programme externe | Outil de développement uniquement, jamais livré | **FFmpegKit** : abandonné, exclu comme demandé |
| Polices | Inter et JetBrains Mono, embarquées | Licence OFL, couverture complète du français, rendu identique sur tous les téléphones | Polices du système : différentes d'un téléphone à l'autre |
| Fichiers de sortie | MediaStore › Movies/IT_MENTOR (Android 10+), dossier de l'appli (Android 8–9) | Aucune permission demandée | Permission de stockage : refusée par Google Play pour ce cas d'usage |

**Point de vigilance (D4) :** ton dossier prévoyait « Media3 Transformer + moteur maison ». C'est exactement ce que fait le prototype. La seule différence avec le dossier : MediaCodec seul n'est plus le chemin principal, il devient le plan B, pour la raison d'horodatage donnée dans le tableau.

## 6. Ce que contient le moteur

- **Chronologie** (`Timeline`) : calcule le début de chaque scène, fait se chevaucher les scènes pendant les transitions, et refuse toute vidéo de plus de 40 s.
- **Animations** (`Animator`) : apparition, disparition, glissement horizontal, glissement vertical, zoom avant, zoom arrière, machine à écrire et déplacement le long d'un chemin. Elles sont calculées uniquement à partir du temps de rendu, avec des courbes de Bézier (`Easing`).
- **Transitions** : fondu, glissement, zoom et coupe franche.
- **6 compositions** : couverture, définition, étapes, comparaison, chiffre clé et « à retenir ».
- **Éléments graphiques** : titre, texte, sous-titre, icônes vectorielles originales, illustrations, formes géométriques, décor animé, indicateur de progression par scène et pied de page de marque.
- **Texte** : retours à la ligne, espaces insécables, mots trop longs coupés, réduction automatique de la taille puis « … », pas de mot seul en dernière ligne.
- **3 styles** : Cyber (grille de circuit), Santé (ondes et électrocardiogramme), Agro (sillons et soleil). Changer de style ne demande aucune modification du moteur.

## 7. Reste à valider sur un téléphone (première tâche de la phase 2)

1. Ouvrir le projet dans Android Studio. Accepter l'assistant de mise à jour si des versions plus récentes que celles de `gradle/libs.versions.toml` sont proposées.
2. Compiler `demo-app`, puis l'installer sur au moins deux téléphones : un récent, et un d'entrée de gamme sous Android 8 ou 9.
3. Appuyer sur « Générer la vidéo de démonstration ». L'appli affiche elle-même le rapport :
   - format choisi ;
   - codec, résolution, durée et images, relus dans le fichier avec `MediaExtractor` ;
   - temps d'export ;
   - temps de rendu par image ;
   - pics de mémoire ;
   - état thermique.
4. M'envoyer ce texte, les éventuelles erreurs de compilation et la vidéo produite.

Si le rendu d'une image dépasse environ 60 ms sur l'entrée de gamme, l'export prendra environ 1 minute pour 32 s de vidéo. Ce serait acceptable avec une notification, mais à améliorer. Il y a trois pistes, dans cet ordre :

1. mettre en cache le fond de chaque scène ;
2. passer en 720p automatiquement ;
3. utiliser OpenGL ES.

## 8. Reproduire ces résultats

```
./test.sh                 # 26 tests, sans Gradle ni réseau (compilateur Kotlin embarqué dans Gradle)
./gradlew :engine-core:test :engine-jvm:test      # même chose avec Gradle (réseau nécessaire)
./gradlew :engine-jvm:run --args="render out/demo.mp4 1080 30"
./gradlew :demo-app:installDebug                  # sur un téléphone branché
```
