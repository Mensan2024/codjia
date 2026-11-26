# CODJIA — Diagnostic technique (version 2.2.0)

Date : 26 novembre 2026. Complète `AUDIT.md` (phase A, 24 novembre). Tout ce qui est écrit ici a été mesuré ou lu dans le code ; ce qui n'est pas vérifié est marqué « non vérifié ».

## 1. Architecture

| Module | Rôle | Taille |
| --- | --- | --- |
| `engine-core` | Moteur Kotlin commun (analyse du texte, scénario, rendu image par image, montage, musique, narration) | 43 fichiers, ≈ 11 200 lignes |
| `engine-jvm` | Rendu Java2D, encodeur MP4, tests | 5 fichiers, ≈ 800 lignes |
| `engine-android` / `demo-app` | Application Android (aucune permission) | ≈ 1 000 lignes |
| `web` | PWA Kotlin/JS : interface, rendu Canvas, export WebCodecs, voix Piper, musique perso | 7 fichiers, ≈ 1 350 lignes |

Chaîne de traitement : texte → `Planner` (domaine, sections, étapes, conclusion) → `Storyboard` de scènes → `Composer` (gabarit, format, rythme, ≤ 57,5 s) → `Look` (rendu du format) → `FrameRenderer` (superposition : barre de progression + logo CODJIA) → MP4.

Le même moteur sert Android et le web : un format ajouté apparaît partout, sans second développement.

## 2. Formats actuels (16 « Look »)

Motion design, Tableau blanc, Démo à l'écran, Personnage animé, Éditorial chic, Explicatif néon, Collage, Cinéma, Infographie, Explicatif simulé (3 mises en page), Journal télévisé, Flash information, Terminal hacker, Bande dessinée. Chacun a des variantes (palettes, fonds, polices, mises en page) tirées du moteur de styles.

Aucun format historique n'a été supprimé.

## 3. Conservation du texte (exigence non négociable)

- **Avant 2.2.0** : des mots de l'auteur disparaissaient selon les formats (titres de section remplacés par un titre générique, texte tapé qui n'a pas le temps de finir, titres longs coupés, légende limitée à 9 mots).
- **Corrigé en 2.2.0** et verrouillé par un test (`chaqueFormatMontreToutLeTexteDeLAuteur`) : tous les formats × tous les textes d'exemple, chaque mot de l'auteur doit être dessiné à l'écran. Résultat : **réussi**.
- **Limites connues** : (a) le test vérifie que le texte est dessiné à un moment de la vidéo, pas qu'il reste lisible assez longtemps ; (b) il porte sur nos textes d'exemple (≈ 20), pas sur des textes quelconques ; (c) au-delà de ≈ 59 s de lecture, le texte est condensé par le format (durée plafonnée) : le traitement des textes longs en plusieurs épisodes n'est **pas encore fait**.

## 4. Tests

70 tests JUnit, tous réussis (≈ 6 min). Couverture réelle : zone sûre (≈ 6 000 + 29 000 + 39 000 textes vérifiés selon les suites), absence de chevauchement, conservation du texte, catalogue musical, muxeur MP4, formats modulables, textes libres (≈ 89 000 textes vérifiés).

**Non couverts aujourd'hui** (à ne pas considérer comme testés) : synchronisation audio/vidéo bout en bout, qualité d'export mesurée sur plusieurs formats d'image, stabilité sur textes très longs, rendu sur Safari/iPhone, appareils réels autres que le Galaxy A16.

## 5. Domaines et démonstrations visuelles

Domaines reconnus : Réseaux, Systèmes, Cybersécurité, IA, Fullstack, Santé, Marketing, Anglais, Général.

Démonstrations réelles existantes : simulations d'interface macOS / Windows / Ubuntu / Cisco (`Procedures`), schémas d'infographie (cible, attaque par force brute, tentatives, bouclier, chaîne, graphe, terminal, versus, cases à cocher).

**Écarts constatés** : pas encore de schéma propre à l'IA (flux de données, effet du prompt), au développement (éditeur de code simulé : écrire, exécuter, erreur, correction), à la santé (anatomie, gestes), ni aux réseaux (paquets, VLAN, DHCP/DNS/NAT) ; pour SSH, pare-feu, MFA, antivirus, hameçonnage, la démonstration repose sur les schémas génériques. À traiter ensuite, toujours étiquetés « simulation ».

## 6. Écarts avec la Partie 2

Faits : 16 formats, palettes/fonds/variantes modulables, voix Piper hors ligne, import de musique perso, PWA installable, un seul logo CODJIA, export vertical 1080×1920.

Pas encore faits : familles de styles supplémentaires (cinématique documentaire, 3D/isométrique, typographie cinétique dédiée, interface futuriste, etc.), favoris / historique / comparaison / aperçu des styles, exports 16:9 et 1:1, enregistrement de la voix de l'utilisateur avec prompteur, historique de projets en local, bibliothèque musicale par genres (voir limite ci-dessous), vidéos de démonstration, tests sur 3 ratios.

**Limite honnête sur la musique** : je ne peux pas composer ni obtenir ici des morceaux professionnels par genre (afrobeat, amapiano, etc.) ; le moteur sait lire des pistes fournies. La solution réaliste est une petite bibliothèque de pistes libres de droits choisies et validées par toi, plus l'import perso déjà présent.

## 7. Risques identifiés

1. Rendu web non testé sur Safari/iPhone (WebCodecs, polices) : non vérifié.
2. Performance sur téléphones modestes : mesurée seulement sur Galaxy A16.
3. Le test de conservation ne couvre pas la lisibilité temporelle ; à compléter (durée minimale d'affichage par phrase).
4. Dépendances : mises à jour Dependabot en attente de ta décision.

## 8. Ordre de travail proposé

1. Démonstrations par domaine (réseaux, IA, dev, santé, cyber) + tests.
2. Textes longs : durée étendue ou épisodes, sans perte de texte.
3. Exports 16:9 et 1:1 + tests de ratio.
4. Voix personnelle avec prompteur (web), puis historique de projets.
5. Favoris / comparaison / aperçu des styles ; nouvelles familles de formats.
6. Musique : bibliothèque réduite validée.
7. Vidéos de démonstration et documentation.
