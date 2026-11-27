# Storyboard — Comprends et configure ton pare-feu !

## Texte original (intégral)

> 🔥Astuce cybersécurité 030 — Comprends et configure ton pare-feu !
> Un pare-feu est un filtre placé entre ton réseau et Internet : il examine chaque paquet et décide s'il peut passer ou s'il doit être bloqué.
> 
> Chaque paquet porte une adresse source, une adresse de destination, un protocole et un port. Le pare-feu compare ces informations à une liste de règles, lues de haut en bas. La première règle qui correspond s'applique, et les règles suivantes sont ignorées.
> 
> Par exemple, un employé qui ouvre une page web envoie un paquet vers le port 443. Le pare-feu l'autorise, puis laisse revenir la réponse du site. À l'inverse, une machine inconnue qui essaie de se connecter au port 22 de ton serveur est bloquée, car aucune règle ne l'autorise.
> 
> ⚠️ Les erreurs fréquentes à éviter :
> - Laisser une règle « tout autoriser » au début de la liste.
> - Ouvrir le port 3389 ou le port 22 à tout Internet.
> - Oublier de supprimer les règles temporaires après une maintenance.
> - Ne jamais lire les journaux du pare-feu.
> 
> Pour bien configurer ton pare-feu, suis ces étapes :
> - Commence par tout bloquer par défaut.
> - Autorise uniquement les services dont tu as besoin, par exemple le web sur le port 443.
> - Limite l'accès à l'administration à quelques adresses de confiance.
> - Journalise les paquets bloqués pour repérer les tentatives suspectes.
> - Teste chaque règle avant de l'appliquer en production.
> 
> Un pare-feu à état suit les connexions déjà établies. Il laisse donc passer la réponse à une demande légitime sans ouvrir la porte à un trafic qui n'a rien demandé. C'est ce qui le distingue d'un simple filtre de ports.
> 
> À retenir : un pare-feu n'est pas un produit qu'on installe puis qu'on oublie. Une bonne politique, c'est « tout bloquer, puis autoriser au cas par cas », avec des règles relues régulièrement et des journaux consultés.
> 
> #securiteinformatiquepourtous
> #pare-feu
> #BonnesPratiques
> #codjia
> 

## Synthèse

```
épisodes : 4 · scènes : 9 · modèle cyber-neon · musique phonk
fidélité : mots de l'auteur : 318 · présents 100.0 % · dans l'ordre 100.0 %
  ÉPISODE 1/4 : 3 scènes, 35.293 s avec la voix, 32.243 s à l'écran
  ÉPISODE 2/4 : 3 scènes, 27.561 s avec la voix, 27.111 s à l'écran
  ÉPISODE 3/4 : 3 scènes, 39.1 s avec la voix, 36.2 s à l'écran
  ÉPISODE 4/4 : 3 scènes, 35.35 s avec la voix, 24.6 s à l'écran
contrôle qualité : 8 remarque(s)
  Attention scène s-hero : aucune représentation assez pertinente : texte seul
  Attention scène s-def : aucune représentation assez pertinente : texte seul
  Attention scène s-def0-1 : aucune représentation assez pertinente : texte seul
  Attention scène ep2-open : aucune représentation assez pertinente : texte seul
  Attention scène ep3-open : aucune représentation assez pertinente : texte seul
  Attention scène s-steps0 : aucune représentation assez pertinente : texte seul
  Attention scène s-steps2-0 : aucune représentation assez pertinente : texte seul
  Attention scène ep4-open : aucune représentation assez pertinente : texte seul
```

## Épisode 1/4

### Scène 1.1 — `s-hero` (HERO, 5.5 s)

1. **Texte exact** : ASTUCE CYBERSÉCURITÉ N°030 ⏎ Comprends et configure ton pare-feu !
2. **Narration** : Comprends et configure ton pare-feu !.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Pare-feu, rôle : ouverture) : texte seul. À couvrir en phase C.
4. **Personnages** : pare-feu
5. **Objets** : objet physique à montrer en volume
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 5.5 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · ouverture · un filtre trie ce qui passe · visuel : `aucun` · empreinte `e98c2e900d73235c`

### Scène 1.2 — `s-def` (DEFINITION, 14.0 s)

1. **Texte exact** : Un pare-feu est un filtre placé entre ton réseau et Internet : il examine chaque paquet et décide s'il peut passer ou s'il doit être bloqué. Chaque paquet porte une adresse source, une adresse de destination, un protocole et un port.
2. **Narration** : Un pare-feu est un filtre placé entre ton réseau et Internet : il examine chaque paquet et décide s'il peut passer ou s'il doit être bloqué. Chaque paquet porte une adresse source, une adresse de destination, un protocole et un port.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Pare-feu, rôle : mécanisme) : texte seul. À couvrir en phase C.
4. **Personnages** : pare-feu
5. **Objets** : paquet, objet physique à montrer en volume
6. **Action** : filtrer
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 14.0 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `aucun` · empreinte `e4e013a9c76ed1f0`

### Scène 1.3 — `s-def0-1` (DEFINITION, 13.6 s)

1. **Texte exact** : Le pare-feu compare ces informations à une liste de règles, lues de haut en bas. La première règle qui correspond s'applique, et les règles suivantes sont ignorées.
2. **Narration** : Le pare-feu compare ces informations à une liste de règles, lues de haut en bas. La première règle qui correspond s'applique, et les règles suivantes sont ignorées.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Pare-feu, rôle : mécanisme) : texte seul. À couvrir en phase C.
4. **Personnages** : pare-feu
5. **Objets** : règle, objet physique à montrer en volume
6. **Action** : vérifier
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 13.6 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `aucun` · empreinte `00aa37bd903e8839`

## Épisode 2/4

### Scène 2.1 — `ep2-open` (HERO, 2.6 s)

1. **Texte exact** : ÉPISODE 2/4 ⏎ Comprends et configure ton pare-feu !
2. **Narration** : Comprends et configure ton pare-feu !.
3. **Image (intention visuelle)** : Carte d'ouverture de l'épisode (titre de la série), ajoutée par l'application : texte seul.
4. **Personnages** : pare-feu
5. **Objets** : objet physique à montrer en volume
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 2.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · ouverture · un filtre trie ce qui passe · visuel : `aucun` · empreinte `a106c206b1190e89`

### Scène 2.2 — `s-def0-2` (DEFINITION, 13.3 s)

1. **Texte exact** : Par exemple, un employé qui ouvre une page web envoie un paquet vers le port 443. Le pare-feu l'autorise, puis laisse revenir la réponse du site.
2. **Narration** : Par exemple, un employé qui ouvre une page web envoie un paquet vers le port 443. Le pare-feu l'autorise, puis laisse revenir la réponse du site.
3. **Image (intention visuelle)** : « Zones et sens de circulation » (Pare-feu) : un filtre trie ce qui passe ; objets montrés : paquet, réponse, objet physique à montrer en volume ; acteurs : utilisateur, pare-feu — pour comprendre le mécanisme. Adéquation 0.90.
4. **Personnages** : utilisateur, pare-feu
5. **Objets** : paquet, réponse, objet physique à montrer en volume
6. **Action** : autoriser
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 13.3 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `firewall/zones` · empreinte `90fb6c68afcfd5e9`

### Scène 2.3 — `s-def0-3` (DEFINITION, 12.2 s)

1. **Texte exact** : À l'inverse, une machine inconnue qui essaie de se connecter au port 22 de ton serveur est bloquée, car aucune règle ne l'autorise.
2. **Narration** : À l'inverse, une machine inconnue qui essaie de se connecter au port 22 de ton serveur est bloquée, car aucune règle ne l'autorise.
3. **Image (intention visuelle)** : « Table de règles » (Pare-feu) : un filtre trie ce qui passe ; objets montrés : règle, objet physique à montrer en volume ; acteurs : serveur, pare-feu — pour comprendre le mécanisme. Adéquation 1.00.
4. **Personnages** : serveur, pare-feu
5. **Objets** : règle, objet physique à montrer en volume
6. **Action** : bloquer
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 12.2 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `firewall/rules` · empreinte `769948cd5e768564`

## Épisode 3/4

### Scène 3.1 — `ep3-open` (HERO, 2.6 s)

1. **Texte exact** : ÉPISODE 3/4 ⏎ Comprends et configure ton pare-feu !
2. **Narration** : Comprends et configure ton pare-feu !.
3. **Image (intention visuelle)** : Carte d'ouverture de l'épisode (titre de la série), ajoutée par l'application : texte seul.
4. **Personnages** : pare-feu
5. **Objets** : objet physique à montrer en volume
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 2.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · ouverture · un filtre trie ce qui passe · visuel : `aucun` · empreinte `a16fbc9a63d35b5f`

### Scène 3.2 — `s-steps0` (STEPS, 15.6 s)

1. **Texte exact** : Les erreurs fréquentes à éviter ⏎ • Laisser une règle « tout autoriser » au début de la liste ⏎ • Ouvrir le port 3389 ou le port 22 à tout Internet ⏎ • Oublier de supprimer les règles temporaires après une maintenance ⏎ • Ne jamais lire les journaux du pare-feu
2. **Narration** : Les erreurs fréquentes à éviter. Laisser une règle « tout autoriser » au début de la liste. Ouvrir le port 3389 ou le port 22 à tout Internet. Oublier de supprimer les règles temporaires après une maintenance. Ne jamais lire les journaux du pare-feu.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Pare-feu, rôle : conseil, parade) : texte seul. À couvrir en phase C.
4. **Personnages** : pare-feu
5. **Objets** : règle, journaux d'événements, objet physique à montrer en volume
6. **Action** : autoriser
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 15.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · conseil, parade · un filtre trie ce qui passe · visuel : `aucun` · empreinte `5a97acb7b4720658`

### Scène 3.3 — `s-steps2-0` (STEPS, 19.0 s)

1. **Texte exact** : Pour bien configurer ton pare-feu, suis ces étapes ⏎ • Commence par tout bloquer par défaut ⏎ • Autorise uniquement les services dont tu as besoin : par exemple le web sur le port 443 ⏎ • Limite l'accès à l'administration à quelques adresses de confiance ⏎ • Journalise les paquets bloqués : pour repérer les tentatives suspectes ⏎ • Teste chaque règle avant de l'appliquer en production
2. **Narration** : Pour bien configurer ton pare-feu, suis ces étapes. Commence par tout bloquer par défaut. Autorise uniquement les services dont tu as besoin. Limite l'accès à l'administration à quelques adresses de confiance. Journalise les paquets bloqués. Teste chaque règle avant de l'appliquer en production.
3. **Image (intention visuelle)** : Aucun visuel du registre n'atteint 0.60 pour cette scène (concept : Pare-feu, rôle : procédure) : texte seul. À couvrir en phase C.
4. **Personnages** : pare-feu
5. **Objets** : paquet, règle, étapes, objet physique à montrer en volume
6. **Action** : bloquer
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 19.0 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · procédure · un filtre trie ce qui passe · visuel : `aucun` · empreinte `9121812544c1174d`

## Épisode 4/4

### Scène 4.1 — `ep4-open` (HERO, 2.6 s)

1. **Texte exact** : ÉPISODE 4/4 ⏎ Comprends et configure ton pare-feu !
2. **Narration** : Comprends et configure ton pare-feu !.
3. **Image (intention visuelle)** : Carte d'ouverture de l'épisode (titre de la série), ajoutée par l'application : texte seul.
4. **Personnages** : pare-feu
5. **Objets** : objet physique à montrer en volume
6. **Action** : —
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : texte plein cadre
10. **Mouvement de caméra** : —
11. **Durée** : 2.6 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · ouverture · un filtre trie ce qui passe · visuel : `aucun` · empreinte `0475d9c2bae4abbd`

### Scène 4.2 — `s-def3-0` (DEFINITION, 14.0 s)

1. **Texte exact** : Un pare-feu à état suit les connexions déjà établies. Il laisse donc passer la réponse à une demande légitime sans ouvrir la porte à un trafic qui n'a rien demandé. C'est ce qui le distingue d'un simple filtre de ports.
2. **Narration** : Un pare-feu à état suit les connexions déjà établies. Il laisse donc passer la réponse à une demande légitime sans ouvrir la porte à un trafic qui n'a rien demandé. C'est ce qui le distingue d'un simple filtre de ports.
3. **Image (intention visuelle)** : « Tri des paquets » (Pare-feu) : un filtre trie ce qui passe ; objets montrés : paquet, réponse, objet physique à montrer en volume ; acteurs : pare-feu — pour comprendre le mécanisme. Adéquation 1.00.
4. **Personnages** : pare-feu
5. **Objets** : paquet, réponse, objet physique à montrer en volume
6. **Action** : filtrer
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 14.0 s
12. **Transition** : fade 500 ms
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · mécanisme · un filtre trie ce qui passe · visuel : `firewall/filter` · empreinte `7826d2f22c6bd3c2`

### Scène 4.3 — `s-end` (CHECKLIST, 9.0 s)

1. **Texte exact** : À retenir ⏎ • Un pare-feu n'est pas un produit qu'on installe puis qu'on oublie ⏎ • Une bonne politique, c'est « tout bloquer, puis autoriser au cas par cas », avec des règles relues régulièrement et des journaux consultés
2. **Narration** : À retenir. Un pare-feu n'est pas un produit qu'on installe puis qu'on oublie. Une bonne politique, c'est « tout bloquer, puis autoriser au cas par cas », avec des règles relues régulièrement et des journaux consultés. Partage cette astuce.
3. **Image (intention visuelle)** : « À retenir : un filtre à l'entrée » (Pare-feu) : un filtre trie ce qui passe ; objets montrés : règle, journaux d'événements, objet physique à montrer en volume ; acteurs : pare-feu — pour retenir l'essentiel. Adéquation 0.90.
4. **Personnages** : pare-feu
5. **Objets** : règle, journaux d'événements, objet physique à montrer en volume
6. **Action** : bloquer
7. **Décor** : format Scénario visuel, modèle Cyber Neon
8. **Animation** : entrée slide_up, sortie fade_out
9. **Cadrage** : plan plein cadre
10. **Mouvement de caméra** : fixe
11. **Durée** : 9.0 s
12. **Transition** : fin de l'épisode
13. **Son** : musique « phonk », voix off
- Intention comprise : Pare-feu · à retenir · un filtre trie ce qui passe · visuel : `firewall/recap` · empreinte `13e090395858155b`
