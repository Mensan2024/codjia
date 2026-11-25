# Journal des versions

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et la numérotation [SemVer](https://semver.org/lang/fr/).

## [1.5.0] — 2026-11-25
- Nouveau format **Montage dynamique** (10 formats ; il revient deux fois plus souvent que les autres). C'est le style des tutoriels animés des réseaux sociaux :
  - un chapitre numéroté en haut, avec une petite étiquette et le mot clé surligné ;
  - au centre, une carte qui montre l'action : page qui s'écrit, prompt qui se tape, terminal, téléphone qui coche les étapes, objet en relief avec étiquette, chiffre, « à éviter / à faire », guide final ;
  - les sous-titres mot à mot, avec le mot prononcé surligné ;
  - la mascotte CODJIA qui parle, sur un sol en grille.
- **Tout le message est déroulé**, sans résumé inventé :
  - toutes les phrases d'explication (une scène par groupe de phrases) ;
  - toutes les listes et tous leurs éléments ;
  - la fin ne reprend que la conclusion écrite par l'auteur, sinon « Abonne-toi ».
  - Avec une démo à l'écran, les étapes de l'auteur sont gardées en plus de la démo.
- Durée selon le texte : **30 à 59 s**. Un texte très long est resserré plutôt que coupé.

## [1.4.0] — 2026-11-25
- Nouveau format **Infographie** (9 formats au total) : chaque idée est expliquée par un schéma animé qui montre le mécanisme. Exemples : cibles d'une attaque, cadenas assailli d'essais avec compteur, barres de robustesse de mots de passe, chaîne « mot de passe deviné › code MFA demandé › accès refusé », tentatives bloquées avec la configuration fail2ban, graphique d'échecs avec alerte et commande `grep`, parcours d'étapes. L'explication s'affiche dans un cadre en bas, et la vidéo se termine par « Abonne-toi » et « Prochaine : Astuce N+1 ».
- Voix off **au choix** directement sur l'aperçu (bouton « Voix : oui / non ») ; la langue (Auto FR/EN, Français, Anglais) reste réglable dans « Musique ».
- 6 nouvelles musiques originales, mixées pour les haut-parleurs de téléphone : afrobeats, amapiano, synthwave, future bass, trap, cinématique. Nouvelles catégories « Afro » et « Cinématique » ; 14 musiques au total.
- Durée adaptée au contenu : 35 à 45 s.

## [1.3.0] — 2026-11-24
- Voix off de retour, en français et en anglais, **avec la musique** : la musique baisse automatiquement sous la voix puis remonte.
- Mode « Auto FR/EN » : la langue est choisie phrase par phrase (une leçon d'anglais lit les exemples en anglais et les explications en français). Modes Français, Anglais ou Sans voix.
- Voix de synthèse du téléphone, hors ligne uniquement (aucun accès réseau). Les scènes s'allongent pour la voix ; lecture accélérée jusqu'à 1,3× si la vidéo dépasse 38 s.
- Aperçu avec la voix et la musique mélangées.

## [1.2.0] — 2026-11-24
- 3 nouveaux formats au rythme des vidéos explicatives des réseaux sociaux : **Explicatif néon** (titres condensés, fenêtres, terminal), **Collage** (fond bleu, cartes en papier épinglées, étiquettes, lettres découpées), **Cinéma** (ambiance nocturne, serif italique, téléphone réaliste).
- Grammaire commune : temps forts de 2 à 4 s, titre court et percutant, visuel concret (téléphone avec écran de réglages, de verrouillage ou de code, fenêtre, terminal, chiffre, verdict), sous-titres mot à mot avec mot clé coloré, icônes choisies selon le sens.
- 8 formats au total, différents à chaque génération.

## [1.1.0] — 2026-11-24
- 5 formats de vidéo qui changent à chaque génération : motion design, tableau blanc (main qui écrit), démo à l'écran, personnage animé (Codi), éditorial chic.
- Démonstrations pas à pas sur Windows, macOS, Ubuntu et console Cisco : Bureau à distance, pare-feu, mises à jour, VLAN.
- Durée de 35 à 45 s selon l'astuce.
- Parcours plus fluide : texte → aperçu immédiat → style et musique facultatifs ; transitions d'écran, retour haptique, aperçu en pause au toucher, écran de génération clair.
- Import audio : mono ou stéréo, 48 kHz au plus.

## [1.0.0] — 2026-11-24
Première version publique. Identifiant définitif : `io.github.mensan2024.codjia`.
- Texte libre → storyboard → vidéo, 8 domaines, 10 templates, variation à chaque génération.
- Musique originale, import de musique, export MP4 avec son.
- Aucune permission, signature de publication, attestation de provenance.
- Détails : [docs/release-notes/v1.0.0.md](docs/release-notes/v1.0.0.md).

## Prototypes (non publiés)
- **0.4** — Texte libre, planificateur, Network Blueprint et Code Studio, variation, durcissement de sécurité.
- **0.3** — Nom CODJIA, bibliothèque musicale, 8 templates.
- **0.2** — Voix off (retirée en 0.3).
- **0.1** — Moteur de rendu validé sur Galaxy A16.
