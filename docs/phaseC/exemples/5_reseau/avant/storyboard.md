# Storyboard — Comment une demande traverse le réseau de l'entreprise

## Texte original (intégral)

> Comment une demande traverse le réseau de l'entreprise
> Quand un employé ouvre un site depuis son poste de travail, plusieurs équipements travaillent en quelques millisecondes.
> D'abord, le DHCP : à son arrivée, l'ordinateur reçoit automatiquement une adresse IP du routeur.
> Ensuite, le DNS : le nom du site est traduit en adresse IP, comme un annuaire.
> Puis le trafic passe par le switch, qui relie les postes entre eux, et atteint le routeur.
> Le routeur applique le NAT : les adresses privées de l'entreprise sont traduites en une seule adresse publique pour sortir sur Internet.
> Le pare-feu filtre ce qui entre et ce qui sort selon des règles précises.
> Pour les employés à distance, un VPN crée un tunnel chiffré jusqu'au réseau de l'entreprise.
> En cas de panne, teste maillon par maillon : le câble, le switch, la passerelle, puis le DNS. Le problème se trouve souvent au premier maillon qui échoue.
> À retenir : un réseau fiable, c'est une chaîne dont chaque maillon est compris.
> #reseaux
> 

## Synthèse

```
épisodes : 3 · scènes : 9 · modèle network-blueprint · musique future
fidélité : mots de l'auteur : 169 · présents 100.0 % · dans l'ordre 100.0 %
  ÉPISODE 1/3 : 3 scènes, 40.5 s avec la voix, 31.8 s à l'écran
  ÉPISODE 2/3 : 3 scènes, 30.931 s avec la voix, 29.281 s à l'écran
  ÉPISODE 3/3 : 5 scènes, 28.125 s avec la voix, 27.325 s à l'écran
contrôle qualité : 11 remarque(s)
  BLOQUANT scène s-def : plan firewall/zones noté 0.00 (< 0.6)
  BLOQUANT scène s-def : plan firewall/zones (Pare-feu) pour le concept Topologie réseau
  Attention scène s-hero : aucune représentation assez pertinente : texte seul
  Attention scène ep2-open : aucune représentation assez pertinente : texte seul
  BLOQUANT scène d-mac-1 : plan firewall/filter noté 0.00 (< 0.6)
  BLOQUANT scène d-mac-1 : plan firewall/filter (Pare-feu) pour le concept Sans concept visuel net
  BLOQUANT scène s-end : plan firewall/recap noté 0.00 (< 0.6)
  BLOQUANT scène s-end : plan firewall/recap (Pare-feu) pour le concept Sans concept visuel net
  Attention scène ep3-open : aucune représentation assez pertinente : texte seul
  Attention scène d-mac-0 : aucune représentation assez pertinente : texte seul
  Attention scène d-mac-2 : aucune représentation assez pertinente : texte seul
```

## Épisode 1/3

### Scène 1.1 — `s-hero` (HERO, 4.8 s)

1. **Texte exact** : ASTUCE RÉSEAUX ⏎ Comment une demande traverse le réseau de l'entreprise ⏎ Quand un employé ouvre un site depuis son poste de travail, plusieurs équipements travaillent en quelques millisecondes.
2. **Narration** : Comment une demande traverse le réseau de l'entreprise. Quand un employé ouvre un site depuis son poste de travail, plusieurs équipements travaillent en quelques millisecondes.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Topologie réseau, rôle : ouverture) : texte seul. À couvrir en phase C.
4. **Personnages** : utilisateur, commutateur
5. **Objets** : ordinateur
6. **Action** : demander
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 4.8 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Topologie réseau · ouverture · des éléments reliés · visuel : `aucun` · empreinte `6d2dad50b7c6997d`

### Scène 1.2 — `s-def` (DEFINITION, 14.0 s)

1. **Texte exact** : D'abord, le DHCP : à son arrivée, l'ordinateur reçoit automatiquement une adresse IP du routeur. Ensuite, le DNS : le nom du site est traduit en adresse IP, comme un annuaire.
2. **Narration** : D'abord, le DHCP : à son arrivée, l'ordinateur reçoit automatiquement une adresse IP du routeur. Ensuite, le DNS : le nom du site est traduit en adresse IP, comme un annuaire.
3. **Image (intention visuelle)** : « Zones et sens de circulation » (Pare-feu) : des éléments reliés ; objets montrés : ordinateur, objet physique à montrer en volume ; acteurs : routeur, commutateur — pour comprendre le mécanisme. Adéquation 0.00 (sous le seuil de 0.60 : choix de l'utilisateur).
4. **Personnages** : routeur, commutateur
5. **Objets** : ordinateur, objet physique à montrer en volume
6. **Action** : envoyer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 14.0 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Topologie réseau · mécanisme · des éléments reliés · visuel : `firewall/zones` · empreinte `add46ef18a592c6d`

### Scène 1.3 — `s-def0-1` (DEFINITION, 14.0 s)

1. **Texte exact** : Puis le trafic passe par le switch, qui relie les postes entre eux, et atteint le routeur. Le routeur applique le NAT : les adresses privées de l'entreprise sont traduites en une seule adresse publique pour sortir sur Internet.
2. **Narration** : Puis le trafic passe par le switch, qui relie les postes entre eux, et atteint le routeur. Le routeur applique le NAT : les adresses privées de l'entreprise sont traduites en une seule adresse publique pour sortir sur Internet.
3. **Image (intention visuelle)** : « Le chemin d'un paquet » (Topologie et VLAN) : des éléments reliés ; objets montrés : paquet, ordinateur, objet physique à montrer en volume ; acteurs : routeur, commutateur — pour comprendre le mécanisme. Adéquation 1.00.
4. **Personnages** : routeur, commutateur
5. **Objets** : paquet, ordinateur, objet physique à montrer en volume
6. **Action** : relier
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : bandeau large (400 sur 700)
10. **Mouvement de caméra** : fixe
11. **Durée** : 14.0 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « future », voix off
- Intention comprise : Topologie réseau · mécanisme · des éléments reliés · visuel : `topology/path` · empreinte `2d5cdc7843cb04fe`

## Épisode 2/3

### Scène 2.1 — `ep2-open` (HERO, 2.6 s)

1. **Texte exact** : ÉPISODE 2/3 ⏎ Comment une demande traverse le réseau de l'entreprise
2. **Narration** : Comment une demande traverse le réseau de l'entreprise.
3. **Image (intention visuelle)** : Carte d'ouverture de l'épisode (titre de la série), ajoutée par l'application : texte seul.
4. **Personnages** : pare-feu
5. **Objets** : —
6. **Action** : demander
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 2.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Pare-feu · ouverture · un filtre trie ce qui passe · visuel : `aucun` · empreinte `5c09eb6e69272ad1`

### Scène 2.2 — `s-def0-2` (DEFINITION, 14.0 s)

1. **Texte exact** : Le pare-feu filtre ce qui entre et ce qui sort selon des règles précises. Pour les employés à distance, un VPN crée un tunnel chiffré jusqu'au réseau de l'entreprise.
2. **Narration** : Le pare-feu filtre ce qui entre et ce qui sort selon des règles précises. Pour les employés à distance, un VPN crée un tunnel chiffré jusqu'au réseau de l'entreprise.
3. **Image (intention visuelle)** : « Table de règles » (Pare-feu) : un filtre trie ce qui passe ; objets montrés : règle, objet physique à montrer en volume ; acteurs : utilisateur, pare-feu — pour comprendre le mécanisme. Adéquation 0.80.
4. **Personnages** : utilisateur, pare-feu
5. **Objets** : règle, objet physique à montrer en volume
6. **Action** : filtrer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 14.0 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `firewall/rules` · empreinte `00b4dcf8ca3a3ae3`

### Scène 2.3 — `s-def0-3` (DEFINITION, 13.6 s)

1. **Texte exact** : En cas de panne, teste maillon par maillon : le câble, le switch, la passerelle, puis le DNS. Le problème se trouve souvent au premier maillon qui échoue.
2. **Narration** : En cas de panne, teste maillon par maillon : le câble, le switch, la passerelle, puis le DNS. Le problème se trouve souvent au premier maillon qui échoue.
3. **Image (intention visuelle)** : « Topologie en étoile » (Topologie et VLAN) : un filtre trie ce qui passe ; objets montrés : message, objet physique à montrer en volume ; acteurs : routeur, commutateur, pare-feu — pour comprendre le mécanisme. Adéquation 0.82.
4. **Personnages** : routeur, commutateur, pare-feu
5. **Objets** : message, objet physique à montrer en volume
6. **Action** : essayer en boucle
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 13.6 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « future », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `topology/star` · empreinte `ef507856d21a7e37`

## Épisode 3/3

### Scène 3.1 — `ep3-open` (HERO, 2.6 s)

1. **Texte exact** : ÉPISODE 3/3 ⏎ Comment une demande traverse le réseau de l'entreprise
2. **Narration** : Comment une demande traverse le réseau de l'entreprise.
3. **Image (intention visuelle)** : Carte d'ouverture de l'épisode (titre de la série), ajoutée par l'application : texte seul.
4. **Personnages** : —
5. **Objets** : —
6. **Action** : demander
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 2.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Sans concept visuel net · ouverture · aucune relation nette · visuel : `aucun` · empreinte `e32e3db8449fded9`

### Scène 3.2 — `d-mac-0` (SCREEN, 6.3 s)

1. **Texte exact** : Appuie sur Cmd + Espace (Spotlight), tape « Réglages Système » et valide.
2. **Narration** : Appuie sur Cmd + Espace (Spotlight), tape « Réglages Système » et valide.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Sans concept visuel net, rôle : définition) : texte seul. À couvrir en phase C.
4. **Personnages** : —
5. **Objets** : —
6. **Action** : cliquer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 6.3 s
12. **Transition** : cut 0 ms
13. **Son** : musique « future », voix off
- Intention comprise : Sans concept visuel net · définition · aucune relation nette · visuel : `aucun` · empreinte `e6bd396e3aad212f`

### Scène 3.3 — `d-mac-1` (SCREEN, 5.7 s)

1. **Texte exact** : Dans Réseau, le coupe-feu apparaît sous tes connexions. Ici, il est inactif.
2. **Narration** : Dans Réseau, le coupe-feu apparaît sous tes connexions. Ici, il est inactif.
3. **Image (intention visuelle)** : « Tri des paquets » (Pare-feu) : aucune relation nette — pour comprendre le mécanisme. Adéquation 0.00 (sous le seuil de 0.60 : choix de l'utilisateur).
4. **Personnages** : —
5. **Objets** : —
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 5.7 s
12. **Transition** : cut 0 ms
13. **Son** : musique « future », voix off
- Intention comprise : Sans concept visuel net · définition · aucune relation nette · visuel : `firewall/filter` · empreinte `b0941243815020a1`

### Scène 3.4 — `d-mac-2` (SCREEN, 5.4 s)

1. **Texte exact** : Active le coupe-feu : macOS bloque les connexions entrantes non autorisées.
2. **Narration** : Active le coupe-feu : macOS bloque les connexions entrantes non autorisées.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Pare-feu, rôle : conseil, parade) : texte seul. À couvrir en phase C.
4. **Personnages** : pare-feu
5. **Objets** : —
6. **Action** : bloquer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 5.4 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Pare-feu · conseil, parade · un filtre trie ce qui passe · visuel : `aucun` · empreinte `4fece896316fc648`

### Scène 3.5 — `s-end` (CHECKLIST, 8.3 s)

1. **Texte exact** : À retenir ⏎ • Un réseau fiable, c'est une chaîne dont chaque maillon est compris
2. **Narration** : À retenir. Un réseau fiable, c'est une chaîne dont chaque maillon est compris. Enregistre cette astuce.
3. **Image (intention visuelle)** : « À retenir : un filtre à l'entrée » (Pare-feu) : aucune relation nette ; objets montrés : message — pour retenir l'essentiel. Adéquation 0.00 (sous le seuil de 0.60 : choix de l'utilisateur).
4. **Personnages** : —
5. **Objets** : message
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 8.3 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « future », voix off
- Intention comprise : Sans concept visuel net · à retenir · aucune relation nette · visuel : `firewall/recap` · empreinte `d7ab398c20802f97`
