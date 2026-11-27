# Phase B — pipeline Scénario, épisodes et storyboard éditable (version 2.11.0)

## Ce qui a été demandé
Le pipeline Scénario est le chemin principal ; aucun texte supprimé ni résumé (plusieurs épisodes plutôt qu'une compression à 59 s) ; storyboard visible et modifiable avant le rendu final ; chaque scène remplaçable sans régénérer les autres ; exemple complet de cybersécurité ; les 94 tests et les 26 formats préservés.

## Exemple : « Comprends et configure ton pare-feu » (`docs/phaseB/exemple_cyber.txt`, graine 7)

| | 2.10.0 (mesuré, même texte, même graine) | Phase B |
|---|---|---|
| Sortie | 1 vidéo de 56,8 s | 4 épisodes (32,3 / 26,8 / 36,0 / 24,5 s à l'écran) |
| Mots de l'auteur présents | 99,4 % (« À retenir » absent) | 100 % |
| Mots dans l'ordre | **85,5 %** (41 mots déplacés : le paragraphe « pare-feu à état » est remonté) | **100 %** |
| Écrans de démo non demandés | 2 (macOS, le texte ne nomme aucun système) | 0 |
| Scènes avec un plan du registre | 4 sur 11 | 4 sur 9 scènes d'auteur (+3 cartes d'ouverture en texte seul) |

Mesures : `Fidelity.measure` (code dans `engine-core`), vidéos rendues par `story render` en 1080×1920 / 30 i/s (H.264 + AAC musique ; **sans voix** : la voix off dépend du moteur de synthèse de l'appareil, non disponible sur le banc JVM).

## Fichiers
- `docs/phaseB/exemple_cyber.txt` — texte original.
- `docs/phaseB/apres/storyboard.md` — texte intégral, synthèse, puis pour chaque scène les 13 éléments et l'intention visuelle ; `plan.json` — plan éditable ; `planche_apres.png` — une image par scène.
- `docs/phaseB/avant_2.10.0/planche_2.10.0.png` — la même planche avec le code 2.10.0.
- `docs/phaseB/remplacement/` — deux scènes remplacées (`s-def0-1` : texte seul → règles ; `s-def0-2` : zones → filtre) : `plan_modifie.json`, planche, `rendu.txt` : 4 segments ré-encodés sur 12 (les 2 scènes + leurs voisins portant le fondu), 8 réutilisés ; épisodes 3 et 4 : 0 segment ré-encodé, 1 s.
- Les MP4 sont fournis hors dépôt (trop lourds).

## Utilisation en ligne de commande
```
story plan    <texte.txt> <graine> <dossier> [format]
story replace <plan.json> <scène> <visuel|none> <sortie.json>
story sheet   <plan.json> <image.png>
story render  <plan.json> <dossier> [largeur]
```

## Tests
109 tests au total, tous OK (94 d'origine + 15 `StudioTest`) : découpage sans coupe de partie, fidélité 100 %/100 % sur deux textes longs, identité pixel des scènes non modifiées, JSON aller-retour, cache de segments, pastille d'épisode confinée à l'en-tête.

## Seuils : souhaités ou mesurés
Aucun seuil de performance nouveau n'est revendiqué ici. Les temps de rendu (≈ 25 s de calcul par épisode de 30 s, JVM de bureau) sont des mesures de ce banc, pas de l'appareil Android.

## Entrées de la phase C (non commencée)
Couverture visuelle (5 scènes d'auteur sur 9 en texte seul ici), plans non répétables épuisés avec seulement 4 plans pare-feu, affectation globale parfois mal placée, titres génériques de gabarit, cartes d'ouverture sans visuel.
