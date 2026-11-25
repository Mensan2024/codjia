# CODJIA — Bibliothèque musicale : origine et licences

## Règle

CODJIA n'intègre que trois sortes de musique :

- des **compositions originales** ;
- des musiques **libres de droits** dont la licence autorise l'intégration et la redistribution dans une application ;
- des **fichiers importés par l'utilisateur**, sous sa responsabilité. Ces fichiers ne sont jamais redistribués.

Aucune musique commerciale protégée n'est intégrée.

## Pistes intégrées (version 1.6)

Les 13 pistes sont des **compositions originales**, entièrement calculées par le script `tools/music/compose_energy.py`, note par note. Il n'y a aucun échantillon, aucune boucle et aucune mélodie tierce. Elles sont donc sous la licence du projet (**GPL-3.0-or-later**) et peuvent être redistribuées sur Google Play comme sur F-Droid.

Le script est fourni avec le code : n'importe qui peut régénérer les fichiers à l'identique (graine aléatoire fixe) avec `tools/music/build_music.sh`.

| Titre | Catégorie | Tempo | Ce qu'on entend |
| --- | --- | --- | --- |
| Afro Vibes | Afro et tropical | 106 BPM | Grosse caisse syncopée, rimshots, shakers, percussions, basse ronde, mélodie |
| Amapiano Groove | Afro et tropical | 113 BPM | « Log drum » grave, shakers, piano jazzy |
| Tropical Dembow | Afro et tropical | 96 BPM | Rythme dembow, 808, accords lumineux |
| Trap Bass | Dynamique, grosses basses | 140 BPM | 808 qui glisse, charleston en rafales, mélodie sombre avec écho |
| Drill Night | Dynamique, grosses basses | 142 BPM | 808 glissées très saturées, charleston en triolets, cordes |
| Phonk Drift | Dynamique, grosses basses | 130 BPM | Cloche mélodique, 808 saturée, ambiance course de nuit |
| Big Drop | Électro | 128 BPM | Accords « supersaw » qui pompent, basse octave, mélodie de drop |
| Deep House Pulse | Électro | 124 BPM | 4 temps, basse en contretemps, accords « stab » |
| Future Bass | Électro | 150 BPM | Accords larges hachés, 808, plucks |
| Outrun | Électro | 100 BPM | Arpège rétro, basse en croches, nappe synthwave |
| Boom Bap Study | Hip-hop et lo-fi | 92 BPM | Batterie qui claque, basse ronde, piano électrique |
| Lo-fi Beats | Hip-hop et lo-fi | 86 BPM | Batterie feutrée, basse, piano — posé, pour apprendre |
| Epic Trailer | Cinématique et épique | 92 BPM | Taikos, cordes en ostinato, « braams », impacts |

Construction commune, pensée pour les vidéos courtes :
- **64 s par piste**, plus que la plus longue vidéo (59 s) : la musique ne boucle jamais, donc ne s'arrête et ne redémarre jamais pendant une vidéo (vérifié par `MusicCatalogTest`).
- **Énergie immédiate** : 2 mesures d'intro filtrée avec une montée, puis le « drop » vers 4 s. Toutes les 8 mesures, une respiration (montée, roulement, impact) relance l'énergie, **sans silence**.
- **Pensé pour le téléphone** :
  - basses saturées, pour que leurs harmoniques s'entendent sur un petit haut-parleur ;
  - sous-graves atténués ;
  - présence et aigus renforcés.

Le choix « Sans musique » est proposé en tête de liste.

## Format et niveau

- Les fichiers sont en AAC 192 kb/s, stéréo, 44,1 kHz : environ 1,7 Mo par piste.
- Le niveau moyen est d'environ −10 à −12 dBFS RMS (musiques rythmées), avec une crête à −1 dBFS. Le volume est homogène d'une piste à l'autre ; sous la voix off, la musique baisse automatiquement.
- La stéréo est limitée pour rester correcte sur le haut-parleur mono d'un téléphone : la corrélation gauche/droite est d'au moins 0,45.

## Ajouter des musiques plus tard

Le catalogue est décrit par `engine-android/src/main/assets/music/music.json`. Pour ajouter une piste, il suffit de déposer le fichier et d'ajouter une entrée avec les champs suivants : `id`, `title`, `category`, `bpm`, `file`, `durationMs`, `licence`, `origin`, `peaks`.

Une piste tierce n'est acceptée qu'avec une licence qui autorise explicitement la redistribution dans une application, par exemple CC0 ou CC BY. Pour une piste CC BY, le crédit va dans `origin`. Pour un catalogue plus grand, téléchargé à la demande, le même format JSON servira d'index distant.
