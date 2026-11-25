# Journal des versions

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et la numérotation [SemVer](https://semver.org/lang/fr/).

## [1.6.0] — 2026-11-25
### Musique
- **La musique ne s'arrête plus et ne redémarre plus au milieu d'une vidéo.** Les morceaux durent maintenant 64 s, plus que la plus longue vidéo (59 s) : ils ne bouclent plus, ni dans l'aperçu ni dans la vidéo exportée. L'aperçu se recale moins souvent sur la musique, donc sans à-coups.
- **Nouveau catalogue de 13 musiques originales, rythmées et avec basses** (toujours synthétisées par le projet, sans échantillon tiers, licence GPL-3.0) :
  - Afro Vibes, Amapiano Groove, Tropical Dembow ;
  - Trap Bass, Drill Night, Phonk Drift ;
  - Big Drop (EDM), Deep House Pulse, Future Bass, Outrun (synthwave) ;
  - Boom Bap Study, Lo-fi Beats ;
  - Epic Trailer.
- Construites pour les vidéos courtes :
  - énergie dès le début, le « drop » arrive vers 4 s ;
  - montée, roulement de caisse claire et impact avant chaque relance ;
  - aucun silence ;
  - basses saturées et mixage pensé pour les haut-parleurs de téléphone (moins de sous-graves inaudibles, plus de présence).
- Choix automatique par domaine revu : trap, drill ou phonk pour la cyber ; EDM ou synthwave pour les réseaux ; afro ou amapiano pour la santé et le marketing…
- La page Musique ne montre plus que les catégories qui contiennent des musiques.
- Nouveau test : chaque musique dure plus que la plus longue vidéo, et chaque musique choisie automatiquement existe.

## [1.5.2] — 2026-11-25
### Corrections
- **Parties du texte perdues.** Quand une partie (« Comment l'éviter : », « Comment se protéger : »…) contenait des phrases plutôt qu'une courte liste, elle était jetée, et la vidéo passait directement à « L'essentiel ». Désormais :
  - toutes les parties sont gardées, dans l'ordre du texte ;
  - chaque partie a ses propres scènes, avec son titre (« Comment l'éviter », « Comment se comporter si tu as cliqué ») : des explications pour les phrases, des étapes pour les listes ;
  - les en-têtes sur la même ligne que le texte (« Comment se comporter : change… ») sont reconnus.
- Autres pertes de texte corrigées :
  - les éléments de liste longs ne sont plus tronqués ;
  - les phrases très longues sont coupées aux virgules au lieu d'être raccourcies ;
  - toutes les lignes ❌/✅ sont montrées, pas seulement la première paire ;
  - toutes les lignes de la partie « À retenir » sont gardées.
- La conclusion n'est plus dite deux fois à la fin.
- Format Éditorial : un titre avec un mot en italique pouvait déborder de l'écran.
- Montage dynamique : titres jusqu'à 8 mots (« Comment se comporter si tu as cliqué »).
- Tests :
  - nouveau test qui vérifie que **chaque mot du texte de l'auteur apparaît dans la vidéo** ;
  - les scripts de test s'arrêtent désormais sur toute erreur de compilation (avant, ils pouvaient tourner sur une ancienne version des tests).

## [1.5.1] — 2026-11-25
### Corrections
- Aperçu figé au-delà de 48 s : la musique boucle désormais dans l'aperçu (comme à l'export), et l'image suit sa propre horloge.
- Plantages possibles :
  - « Autre version » (dans l'aperçu et après la génération) : calculée hors de l'écran, avec un message en cas d'échec ;
  - aperçu impossible : message clair au lieu d'une fermeture de l'appli ;
  - échec de la voix off pendant l'export : la vidéo est générée quand même, avec la musique seule.
- Vidéo refusée au-delà de 59 s : un format en coupes franches ou le calage sur le tempo pouvaient pousser un texte long à plus de 59 s. Les scènes sont maintenant resserrées automatiquement, sans rien retirer.
- Démo hors sujet : « mets à jour » dans une astuce sur la box ne déclenche plus la démo Windows Update (il faut parler d'un ordinateur).
- Titres coupés sur un petit mot (« … des », « … si tu ») et titres devenus minuscules : ils passent maintenant sur deux lignes, en grand.
- Nouveau test : un texte long (8 étapes, ~58 s) rendu dans tous les formats.
### Montage dynamique plus rythmé
- Un plan par phrase (3 à 4 s) au lieu de longs blocs.
- Les mots clés surgissent autour de l'objet au moment exact où ils sont dits.
- Effets visuels :
  - reflet lumineux sur chaque nouvelle carte, qui entre aussi avec un rebond ;
  - lente poussée de caméra pendant le plan ;
  - mot prononcé qui « pop » ;
  - mascotte qui saute à chaque coupe.
- Terminal réaliste : sortie typique de la commande (installation, lignes « Failed password » avec des adresses IP de documentation…).

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
