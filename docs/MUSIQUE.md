# CODJIA — Bibliothèque musicale : origine et licences

## Règle

CODJIA n'intègre que trois sortes de musique :

- des **compositions originales** ;
- des musiques **libres de droits** dont la licence autorise l'intégration et la redistribution dans une application ;
- des **fichiers importés par l'utilisateur**, sous sa responsabilité. Ces fichiers ne sont jamais redistribués.

Aucune musique commerciale protégée n'est intégrée.

## Pistes intégrées (version 0.3)

Les 8 pistes sont des **compositions originales**, entièrement calculées par le script `tools/music/compose.py`, note par note. Il n'y a aucun échantillon, aucune boucle et aucune mélodie tierce. Elles sont donc sous la licence du projet (**GPL-3.0-or-later**) et peuvent être redistribuées sur Google Play comme sur F-Droid.

Le script est fourni avec le code : n'importe qui peut régénérer les fichiers à l'identique (graine aléatoire fixe) avec `tools/music/build_music.sh`.

| Titre | Catégorie | Tempo | Durée | Instruments |
| --- | --- | --- | --- | --- |
| Aube | Douce et apaisante | 70 BPM | 0:48 | Nappes larges, cloches clairsemées |
| Momentum | Dynamique et motivante | 118 BPM | 0:48 | Pied régulier, claps, accords pincés à contretemps |
| Horizon | Professionnelle et corporate | 92 BPM | 0:48 | Piano clair, nappe discrète, percussions légères |
| Élan | Inspirante et émotionnelle | 100 BPM | 0:48 | Arpèges de piano, cordes qui montent |
| Pulse | Technologique et futuriste | 110 BPM | 0:48 | Arpège filtré, sous-basse |
| Lo-fi Focus | Éducative et pédagogique | 80 BPM | 0:48 | Piano électrique, batterie feutrée |
| Canopée | Nature et environnement | 84 BPM | 0:48 | Lames de bois, vent, oiseaux synthétiques |
| Sahel | Nature et environnement | 96 BPM | 0:48 | Balafon (gamme pentatonique), shaker, tambour grave |

Le choix « Sans musique » est proposé en tête de liste.

## Format et niveau

- Les fichiers sont en AAC 160 kb/s, stéréo, 44,1 kHz : environ 1 Mo par piste, 7,7 Mo pour les huit.
- Le niveau moyen est de −16 à −17 dBFS RMS, avec une crête toujours sous −2 dBFS. Le volume est donc homogène d'une piste à l'autre.
- La stéréo est limitée pour rester correcte sur le haut-parleur mono d'un téléphone : la corrélation gauche/droite est d'au moins 0,4.

## Ajouter des musiques plus tard

Le catalogue est décrit par `engine-android/src/main/assets/music/music.json`. Pour ajouter une piste, il suffit de déposer le fichier et d'ajouter une entrée avec les champs suivants : `id`, `title`, `category`, `bpm`, `file`, `durationMs`, `licence`, `origin`, `peaks`.

Une piste tierce n'est acceptée qu'avec une licence qui autorise explicitement la redistribution dans une application, par exemple CC0 ou CC BY. Pour une piste CC BY, le crédit va dans `origin`. Pour un catalogue plus grand, téléchargé à la demande, le même format JSON servira d'index distant.
