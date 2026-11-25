# CODJIA — prototype du moteur vidéo

![CODJIA](docs/brand/codjia-logo.png)

CODJIA transforme **ton propre texte** en vidéo éducative verticale de 30 à 59 s (MP4, H.264 et AAC), entièrement hors ligne.

- **Domaines :** réseaux (CCNA, CCNP), systèmes (Windows, macOS, Linux, Android), cybersécurité, IA, développement fullstack, santé et longévité, marketing, anglais.
- **Style unique à chaque vidéo :** un moteur de styles modulables combine palettes, fonds animés, polices, titres, cadres, sous-titres, transitions et mascotte (des milliers de combinaisons, filtrées par des règles de design).
- **11 formats :** style unique, montage dynamique (tutoriel animé avec sous-titres mot à mot et mascotte), motion design, tableau blanc (main qui écrit), démo à l'écran, personnage animé, éditorial chic, explicatif néon, collage, cinéma, infographie (schémas animés).
- **Tout ton texte est montré**, sans résumé, en 30 à 59 s selon sa longueur.
- **Voix off au choix** (oui/non, français, anglais ou automatique) et 13 musiques originales rythmées, avec basses (afro, trap, phonk, EDM, house…), plus longues que la vidéo : jamais de coupure.
- **Démonstrations pas à pas** sur Windows, macOS, Ubuntu et console Cisco, pour les sujets de la bibliothèque.
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


## Télécharger

La dernière version signée est sur la page [**Releases**](https://github.com/Mensan2024/itmentor-video-prototype/releases/latest) (fichier `CODJIA-x.y.z.apk`, Android 8 ou plus). Chaque APK est accompagné de son empreinte SHA-256 et d'une attestation de provenance. La vérification est expliquée dans [docs/PUBLIER.md](docs/PUBLIER.md#5-vérifier-un-apk-téléchargé-pour-toi-et-tes-utilisateurs).

Confidentialité : [PRIVACY.md](PRIVACY.md) (aucune donnée collectée). Historique : [CHANGELOG.md](CHANGELOG.md).

## Version de test (développeurs)

Chaque envoi sur GitHub produit une version de test, **CODJIA test**, installable à côté de la version publique. Pour la récupérer : onglet **Actions**, dernière exécution verte, artefact **CODJIA-prototype-apk**.

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
