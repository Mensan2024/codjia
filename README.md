# CODJIA — prototype du moteur vidéo

Transforme un storyboard (scènes, textes, animations, transitions) en vidéo MP4 verticale H.264, 40 s au plus.
Licence : GPL-3.0-or-later. Polices : Inter et JetBrains Mono (OFL, licences dans `engine-android/src/main/assets/fonts/`).

| Module | Rôle | État |
| --- | --- | --- |
| `engine-core` | Moteur Kotlin pur : chronologie, animations, transitions, texte, compositions, styles | Compilé et testé |
| `engine-jvm` | Banc d'essai : rendu Java2D + encodage ffmpeg du poste, mesures | Compilé et testé |
| `engine-android` | Rendu sur Canvas Android + export Media3 Transformer + relecture du MP4 + Galerie | Écrit, **non compilé ici** |
| `demo-app` | Écran de test minimal : génère la vidéo et affiche le rapport mesuré sur le téléphone | Écrit, **non compilé ici** |

Le rapport complet est dans `docs/RAPPORT_VALIDATION.md`.

## Démarrage rapide
- Tests sans réseau : `./test.sh`
- Android : ouvrir le dossier dans Android Studio, puis lancer `demo-app`.
