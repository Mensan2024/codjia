# Phase D — Studio d'IA pédagogique (informatique, réseaux, cybersécurité)

## D.1 — Bibliothèque de fiches et écriture de leçon (livré)

Une demande courte (« Phishing ») devient une leçon complète :

1. `LessonWriter.understand(demande, niveau)` reconnaît le sujet par alias (texte normalisé). **Sujet absent de la bibliothèque : aucune leçon n'est inventée** (la note le dit).
2. `LessonWriter.write(fiche, niveau)` compose le texte : accroche, définition, déroulé, conséquences, signes, détection, protection, réaction, bonnes pratiques, à retenir. Chaque fait porte le niveau minimal à partir duquel il apparaît (débutant, intermédiaire, avancé, expert) : la leçon grandit avec le niveau.
3. Ce texte suit le pipeline existant (analyse d'intention, attribution des visuels, découpage en épisodes) : fidélité 100 % au texte écrit, aucune scène sans image, 0 remarque de contrôle qualité aux 4 niveaux.

Fiches disponibles : `phishing`. Les contenus ne contiennent que des faits techniques vérifiables ; adresses de documentation uniquement (domaines `.example`, RFC 5737).

Nouveaux plans : `chain` (fraudeur → faux message → victime → fausse page → compte volé, avec paquets animés) et `damage` (compte verrouillé, trois conséquences).

### Voix off et calage
Le banc de rendu (`story render <plan> <dossier> 1080 <modèle piper>`) synthétise chaque phrase, **allonge les scènes à la durée mesurée** (`Narration.prepare`), mélange la voix et la musique (ducking) puis encode. Il affiche la marge entre la fin de chaque phrase et la scène suivante : sur « Phishing », 4 épisodes, marge minimale 250 ms, aucun chevauchement.

Limite honnête : la voix Piper n'est qu'une **voix de démonstration de l'atelier** ; l'application lit avec la synthèse vocale de l'appareil (durées différentes, déjà absorbées par `Narration.fit`).

### Correctif
Un plan attribué sur toute la vidéo pouvait tomber sous le seuil de pertinence dans le contexte de son épisode (ex. `phishing/bait` noté 0 pour la scène d'ouverture aux niveaux ≥ intermédiaire). `Studio.plan` remplace maintenant ces plans par le meilleur plan libre, sinon la scène reste sans image.

### Exemples
`docs/phaseD/phishing/` : planche de contrôle, storyboard, texte de la leçon.

## À venir
D.2 : fiches et rendus pour OSPF, ransomware, Active Directory, pare-feu (puis VLAN, EtherChannel, adressage, durcissement SSH, Wi-Fi, cloud). D.3 : extensions 3D du moteur logiciel `Engine3D`, connecteur LLM optionnel (sortie toujours validée contre les fiches), réglages d'interface.


## D.2 — validation par la pertinence texte ↔ image (28/11/2026)

Critère de réussite : pour chaque scène, la voix, le texte affiché, les objets, l'animation et les personnages se correspondent ; aucun plan n'est choisi parce qu'un mot de son nom ressemble à un mot du texte. Un plan manquant est créé, pas remplacé par une animation voisine.
Résultat sur les 5 sujets (Phishing, OSPF, Ransomware, Active Directory, Pare-feu ; niveau intermédiaire) : 19 épisodes, 76 scènes, 62 illustrées par un plan dédié au mécanisme cité, 14 cartes d'ouverture (affiche de série), 0 refusée, 0 sans image. Rendu avec voix Piper (voix d'atelier), marge voix/animation ≥ 250 ms, chaque épisode < 50 s. Comparaison et limites : `docs/phaseD/comparaison_avant_apres.md`.
Limites : pas de sous-titres incrustés ; 7 scènes reprennent le plan d'un épisode précédent pour le même mécanisme ; la pièce jointe citée dans le 4ᵉ signe d'hameçonnage n'est pas dessinée.
