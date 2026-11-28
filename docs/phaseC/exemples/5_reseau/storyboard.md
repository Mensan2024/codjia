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
épisodes : 2 · scènes : 10 · modèle network-blueprint · musique future
fidélité : mots de l'auteur : 169 · présents 100.0 % · dans l'ordre 100.0 %
  ÉPISODE 1/2 : 5 scènes, 45.425 s avec la voix, 40.775 s à l'écran
  ÉPISODE 2/2 : 6 scènes, 45.054 s avec la voix, 43.454 s à l'écran
contrôle qualité : 0 remarque(s)
```

## Épisode 1/2

### Scène 1.1 — `s-hero` (HERO, 6.2 s)

1. **Texte exact** : ASTUCE RÉSEAUX ⏎ Comment une demande traverse le réseau de l'entreprise ⏎ Quand un employé ouvre un site depuis son poste de travail, plusieurs équipements travaillent en quelques millisecondes.
2. **Narration** : Comment une demande traverse le réseau de l'entreprise. Quand un employé ouvre un site depuis son poste de travail, plusieurs équipements travaillent en quelques millisecondes.
3. **Image (intention visuelle)** : « Un bureau, ses postes et la sortie vers Internet » (Réseaux en situation) : des éléments reliés ; objets montrés : ordinateur ; acteurs : utilisateur — pour comprendre le mécanisme. Adéquation 1.07.
4. **Personnages** : utilisateur
5. **Objets** : ordinateur
6. **Action** : demander
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 6.2 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · ouverture · des éléments reliés · visuel : `sit-net/office-flow` · empreinte `061abc8fc2654119`

### Scène 1.2 — `s-def` (DEFINITION, 8.1 s)

1. **Texte exact** : D'abord, le DHCP ⏎ À son arrivée, l'ordinateur reçoit automatiquement une adresse IP du routeur.
2. **Narration** : À son arrivée, l'ordinateur reçoit automatiquement une adresse IP du routeur.
3. **Image (intention visuelle)** : « Chaque appareil reçoit son adresse » (Réseaux en situation) : des éléments reliés ; objets montrés : ordinateur, objet physique à montrer en volume ; acteurs : routeur — pour comprendre le mécanisme. Adéquation 1.07.
4. **Personnages** : routeur
5. **Objets** : ordinateur, objet physique à montrer en volume
6. **Action** : envoyer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 8.1 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `sit-net/dhcp` · empreinte `fb893207edf7ee49`

### Scène 1.3 — `s-def0-1` (DEFINITION, 8.1 s)

1. **Texte exact** : Ensuite, le DNS ⏎ Le nom du site est traduit en adresse IP, comme un annuaire.
2. **Narration** : Le nom du site est traduit en adresse IP, comme un annuaire.
3. **Image (intention visuelle)** : « Le nom devient une adresse » (Réseaux en situation) : des éléments reliés — pour comprendre le mécanisme. Adéquation 0.87.
4. **Personnages** : —
5. **Objets** : —
6. **Action** : enchaîner des étapes
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 8.1 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `sit-net/dns` · empreinte `1ae77dda62f3dcc9`

### Scène 1.4 — `s-def0-2` (DEFINITION, 9.9 s)

1. **Texte exact** : Puis le trafic passe par le switch, qui relie les postes entre eux, et atteint le routeur.
2. **Narration** : Puis le trafic passe par le switch, qui relie les postes entre eux, et atteint le routeur.
3. **Image (intention visuelle)** : « Topologie en étoile » (Topologie et VLAN) : des éléments reliés ; objets montrés : paquet, ordinateur, objet physique à montrer en volume ; acteurs : routeur, commutateur — pour comprendre le mécanisme. Adéquation 0.82.
4. **Personnages** : routeur, commutateur
5. **Objets** : paquet, ordinateur, objet physique à montrer en volume
6. **Action** : relier
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 9.9 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `topology/star` · empreinte `81d2d6510f68a45a`

### Scène 1.5 — `s-def0-3` (DEFINITION, 10.3 s)

1. **Texte exact** : Le routeur applique le NAT ⏎ Les adresses privées de l'entreprise sont traduites en une seule adresse publique pour sortir sur Internet.
2. **Narration** : Les adresses privées de l'entreprise sont traduites en une seule adresse publique pour sortir sur Internet.
3. **Image (intention visuelle)** : « Plusieurs adresses privées, une adresse publique » (Réseaux en situation) : des éléments reliés ; objets montrés : objet physique à montrer en volume ; acteurs : routeur — pour comprendre le mécanisme. Adéquation 1.07.
4. **Personnages** : routeur
5. **Objets** : objet physique à montrer en volume
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 10.3 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `sit-net/nat` · empreinte `1461d8ac643fb592`

## Épisode 2/2

### Scène 2.1 — `ep2-open` (HERO, 2.6 s)

1. **Texte exact** : ÉPISODE 2/2 ⏎ Comment une demande traverse le réseau de l'entreprise
2. **Narration** : Comment une demande traverse le réseau de l'entreprise.
3. **Image (intention visuelle)** : « Affiche : des postes reliés à Internet » (Réseaux en situation) : des éléments reliés — pour comprendre le mécanisme. Adéquation 0.78.
4. **Personnages** : —
5. **Objets** : —
6. **Action** : demander
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 2.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · ouverture · des éléments reliés · visuel : `sit-net/poster` · empreinte `6739b4fecd5ad02d`

### Scène 2.2 — `s-def0-4` (DEFINITION, 8.8 s)

1. **Texte exact** : Le pare-feu filtre ce qui entre et ce qui sort selon des règles précises.
2. **Narration** : Le pare-feu filtre ce qui entre et ce qui sort selon des règles précises.
3. **Image (intention visuelle)** : « Tri des paquets » (Pare-feu) : un filtre trie ce qui passe ; objets montrés : règle, objet physique à montrer en volume ; acteurs : pare-feu — pour comprendre le mécanisme. Adéquation 0.90.
4. **Personnages** : pare-feu
5. **Objets** : règle, objet physique à montrer en volume
6. **Action** : filtrer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 8.8 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `firewall/filter` · empreinte `51bbe7d74709318a`

### Scène 2.3 — `s-def0-5` (DEFINITION, 9.2 s)

1. **Texte exact** : Pour les employés à distance, un VPN crée un tunnel chiffré jusqu'au réseau de l'entreprise.
2. **Narration** : Pour les employés à distance, un VPN crée un tunnel chiffré jusqu'au réseau de l'entreprise.
3. **Image (intention visuelle)** : « Le tunnel chiffré du télétravail » (Réseaux en situation) : des éléments reliés ; acteurs : utilisateur — pour comprendre le mécanisme. Adéquation 1.07.
4. **Personnages** : utilisateur
5. **Objets** : —
6. **Action** : mesurer
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 9.2 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `sit-net/vpn` · empreinte `a5bfc9431f26e9d3`

### Scène 2.4 — `s-def0-6` (DEFINITION, 9.2 s)

1. **Texte exact** : En cas de panne, teste maillon par maillon ⏎ Le câble, le switch, la passerelle, puis le DNS.
2. **Narration** : Le câble, le switch, la passerelle, puis le DNS.
3. **Image (intention visuelle)** : « Diagnostiquer une panne maillon par maillon » (Réseaux en situation) : des éléments reliés ; objets montrés : message, objet physique à montrer en volume ; acteurs : routeur, commutateur — pour comprendre le mécanisme. Adéquation 1.00.
4. **Personnages** : routeur, commutateur
5. **Objets** : message, objet physique à montrer en volume
6. **Action** : essayer en boucle
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 9.2 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `sit-net/trouble` · empreinte `ed08b58c79cb5f58`

### Scène 2.5 — `s-def0-7` (DEFINITION, 7.0 s)

1. **Texte exact** : Le problème se trouve souvent au premier maillon qui échoue.
2. **Narration** : Le problème se trouve souvent au premier maillon qui échoue.
3. **Image (intention visuelle)** : « Le premier maillon qui échoue désigne le problème » (Réseaux en situation) : des éléments reliés ; objets montrés : message — pour comprendre le mécanisme. Adéquation 0.97.
4. **Personnages** : —
5. **Objets** : message
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 7.0 s
12. **Transition** : fade 500 ms
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · définition · des éléments reliés · visuel : `sit-net/first-fail` · empreinte `0a3f01ed15423703`

### Scène 2.6 — `s-end` (CHECKLIST, 9.0 s)

1. **Texte exact** : À retenir ⏎ • Un réseau fiable, c'est une chaîne dont chaque maillon est compris
2. **Narration** : À retenir. Un réseau fiable, c'est une chaîne dont chaque maillon est compris. Partage à quelqu’un qui en a besoin.
3. **Image (intention visuelle)** : « À retenir : une chaîne, chaque maillon compris » (Réseaux en situation) : des éléments reliés ; objets montrés : message — pour retenir l'essentiel. Adéquation 0.87.
4. **Personnages** : —
5. **Objets** : message
6. **Action** : partager, parler
7. **Décor** : format Scénario visuel, modèle Network Blueprint
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 9.0 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « future », voix off
- Intention comprise : Circulation des données dans un réseau · à retenir · des éléments reliés · visuel : `sit-net/recap` · empreinte `ba372a1fed9ce998`
