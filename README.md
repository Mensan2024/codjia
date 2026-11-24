# CODJIA — prototype du moteur vidéo

![CODJIA](docs/brand/codjia-logo.png)

CODJIA transforme **ton propre texte** en vidéo éducative verticale de 40 s au plus (MP4, H.264 et AAC), entièrement hors ligne.

- **Domaines :** réseaux (CCNA, CCNP), systèmes (Windows, macOS, Linux, Android), cybersécurité, IA, développement fullstack, santé et longévité, marketing, anglais.
- **Une vidéo différente à chaque génération :** template, mise en page, ordre, couleurs, transitions et musique varient.
- **Aucune permission :** pas de réseau, pas de collecte.

La licence est GPL-3.0-or-later.

![Les templates](docs/templates/apercu-8-templates.jpg)

![Même texte, 3 générations](docs/templates/variation-astuce-022.jpg)

| Module | Rôle |
| --- | --- |
| `engine-core` | Moteur Kotlin pur : chronologie, animations, transitions, texte, 10 templates, planificateur texte → storyboard, variation, musique, détection du domaine, règles de sécurité |
| `engine-jvm` | Banc d'essai (tests, captures, vidéo de démonstration) |
| `engine-android` | Rendu Canvas, export Media3 Transformer, décodage audio, catalogue musical |
| `demo-app` | Application de test : contenu → modèle → musique → aperçu → génération |
| `tools/music` | Compositions musicales originales, reproductibles |

## Installer l'APK

Ouvre l'onglet **Actions** du dépôt, puis la dernière exécution verte. Télécharge l'artefact **CODJIA-prototype-apk** et installe le fichier `.apk` sur le téléphone.

## Documentation

- [Audit et versions conservées](docs/AUDIT.md)
- [Documentation technique](docs/DOCUMENTATION_TECHNIQUE.md)
- [Bibliothèque musicale et licences](docs/MUSIQUE.md)
- [Rapport de validation](docs/RAPPORT_VALIDATION.md)
- [Plan de développement complet](docs/PLAN_DEVELOPPEMENT.md)
- [Sécurité : modèle de menaces](docs/SECURITE.md) · [Signaler une vulnérabilité](SECURITY.md)

## Licences

- **Code et musiques :** GPL-3.0-or-later.
- **Polices :** SIL Open Font License 1.1. Les licences sont dans `engine-android/src/main/assets/fonts/`.
