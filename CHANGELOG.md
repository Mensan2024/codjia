# Journal des versions

Le format suit [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et la numérotation [SemVer](https://semver.org/lang/fr/).

## [2.12.0] — Phase C : situations visuelles par domaine

### Ajouté
- **Moteur de situations** : personnages 2D (`Cast`, 6 apparences, 13 expressions, poses), décors et accessoires (`Props`) et 47 plans de situation pour 5 domaines — hameçonnage, santé mentale, humanitaire, marketing, réseaux — choisis d'après les **sujets précis** du texte (`topics`/`needs`), pas seulement d'après le domaine.
- Les cartes d'ouverture d'épisode ont une affiche en situation ; un plan non répétable n'est jamais réutilisé entre épisodes d'une même série.
- Outil `story gallery` ; exemples avant/après : `docs/phaseC/`. Tests : `PhaseCTest`.

### Corrigé
- Scènes de démonstration macOS inventées pour un texte réseau sans procédure ; numérotation de liste comptée comme mots absents (fidélité 97,6 % → 100 %) ; remarque « BLOQUANT … noté 0,00 » due au contexte par épisode.

### Résultat (5 textes d'essai)
- Scènes sans image : 20/40 → 0/42 ; fidélité 100 %/100 % ; contrôle qualité 0 remarque. Voir `docs/PHASE_C.md` pour les limites (voix non validée, schémas anciens résiduels, titres de gabarit).

## [2.11.0] — Phase B : pipeline Scénario, épisodes, storyboard éditable

### Ajouté
- **Aucune compression forcée** : le nouveau mode `compress = false` conserve 100 % du texte de l'auteur, dans son ordre, aux durées naturelles. Un contenu long est découpé automatiquement en **plusieurs épisodes cohérents** (`Episodes` : budget 50 s, programmation dynamique, jamais de coupe au milieu d'une partie « (1/2)→(2/2) », carte d'ouverture « ÉPISODE k/N » de 2,6 s, pastille d'épisode dans l'en-tête). Une scène trop longue pour un épisode est refusée avec un message clair, jamais tronquée. La voix n'est jamais accélérée dans ce mode.
- **Fidélité mesurée** (`Fidelity`) : part des mots de l'auteur présents et part retrouvée dans l'ordre (plus longue sous-suite commune), mots absents et mots déplacés listés.
- **Plan de production éditable** (`StoryPlan`, `Studio.plan`) : texte source + visuels « épinglés » + modifications de l'utilisateur ; sérialisation JSON aller-retour ; fiche de scène à 13 éléments (texte exact, narration, image, personnages, objets, action, décor, animation, cadrage, caméra, durée, transition, son) — ce que le moteur ne modélise pas est affiché « — », jamais inventé.
- **Remplacement d'une scène sans toucher aux autres** : `replaceVisual`, `reroll`, `withDuration`, `withTransition`, `withText`, `resplit` ; test d'identité pixel par pixel des scènes non modifiées ; cache de rendu par segment (empreinte FNV-1a) : seuls les segments modifiés et leurs voisins de fondu sont ré-encodés.
- **Interface** : écran « Storyboard » (Web et Android) avec vignette de chaque scène, remplacement du visuel, durée ±1 s, transition, texte, épisodes, bouton « Scénario éditable » ; export un MP4 par épisode.
- Outil `story` (engine-jvm) : `plan`, `replace`, `sheet`, `render`. Tests : `StudioTest` (15). Exemple complet : `docs/phaseB/`.

### Corrigé
- Ordre du texte : sections segmentées par alternance liste/paragraphe, démo insérée à sa place sans réordonner l'auteur, démo d'écran seulement si le texte nomme un système, hashtags exclus du texte lu.
- Chemin historique (compression 59 s) conservé tel quel.

### Limites connues (entrées de la phase C)
- Sur l'exemple cyber, 5 scènes d'auteur sur 9 restent en « texte seul » (aucun plan du registre n'atteint 0,6) ; un plan non répétable n'est utilisé qu'une fois ; l'affectation globale peut placer le meilleur plan sur une autre scène que la plus proche.
- Les titres génériques des scènes (« À savoir », « Et aussi ») viennent des gabarits, pas de l'auteur ; les cartes d'ouverture d'épisode sont en texte seul.
- Durées « avec voix » estimées (2,5 mots/s) ; les MP4 de démonstration du banc JVM n'ont pas de voix. Web et APK validés par la CI seulement ; aucune mesure sur appareil Android.

## [2.10.0]

### Ajouté
- **Compréhension de la scène (étape 6.1 de la Partie 6)** : le moteur construit pour chaque scène un `SceneIntent` (domaine, concept, acteurs, objets, action, contexte, relation, rôle et objectif pédagogiques) et choisit dans un registre de ~45 plans, répartis en dix genres, celui qui illustre vraiment le contenu : hameçonnage, force brute, pare-feu, topologie et VLAN, client-serveur, gratitude, stress, tunnel de vente. Les plans sont attribués à toute la vidéo en une seule affectation (algorithme hongrois) : un plan ne sert qu'une fois, une scène sans plan assez adapté reste en texte seul.
- **Format « Scénario »** (`Look.SCENARIO`), recommandé en premier quand le texte relève d'un concept connu. Étiquettes SIMULATION / EXEMPLE FICTIF / SCHEMA PEDAGOGIQUE / ILLUSTRATION QUALITATIVE. Sujets sensibles : plans doux uniquement.
- `Storyboard.profile` : le profil compris du texte d'origine accompagne le storyboard.
- Documentation : `docs/SCENARIOS_6_1.md`. Tests : `ScenarioTest`.

- **Correction « le texte commande l'image »** (27/11) : porte de validation sémantique (`SemanticGate`) — un objet 3D (ordinateur, serveur, cadenas) ou une attaque ne sont proposés que si le passage les cite ; deux nouveaux concepts, **méthode / workflow** (escalier de la mission, chaîne d'étapes avec jeton, étapes cochées) et **accès à l'eau** (village, point d'eau, distance avant/après, effets sur la communauté) ; plan « e-mail d'hameçonnage » (exemple fictif) ; plan « soir » de la gratitude. Les étapes affichées viennent des mots de l'auteur.
- **Contrôle qualité automatique** (`QualityControl.audit`) avant le rendu : pertinence (note ≥ 0,6), objets cités par le texte, concept, durée suffisante, aucun plan répété, sobriété des sujets sensibles, texte présent ; une scène en défaut reste en texte seul.
- Direction artistique : palettes choisies d'après le contenu (cyber, réseau, méthode, eau) et non plus au hasard ; plus de demi-sphères jaune/orange ni de grandes bandes colorées dans le style « Aube » ; mise en page qui centre le schéma et le texte et le cadre sur la partie utile du plan.
- Personnage de dessin complet (`Fig`) : silhouette stylisée à bras articulés, marche, assis, sourire.
- Tests : `CorrectionTest` (textes d'essai A–E, aucun objet sans justification, contrôle qualité, conservation mot pour mot).

### Corrigé
- Un texte sur client et serveur (requête, code de statut, base de données) n'est plus classé « marketing ».
- Le prompt IA (« La méthode en 4 points ») ne reçoit plus un ordinateur portable éclaté sans rapport.
- **Texte de l'auteur tronqué par « … »** dans les formats Personnage animé, Élan, Stratégie et Éditorial pour une longue phrase unique : la taille descend maintenant jusqu'à un plancher lisible avant toute troncature (`TextLayout.fit`).
- Titre du format Scénario jamais tronqué ; légendes des plans déplacées pour rester dans la zone de sécurité.
- Tunnel de vente : une étape « Fidélise » n'est plus dessinée « Convertir ».
- Format Infographie : l'animation du cadenas qui devine des mots de passe (« Soleil2024 ») n'apparaît plus pour un texte sur le phishing ou un rançongiciel ; elle est réservée aux textes qui parlent de mots de passe, de connexion ou d'essais.
- Étiquettes des étapes : plus de mot de liaison pendant (« GIT PUSH ET » devient « GIT PUSH ») ; schémas d'étapes maintenus dans la zone de sécurité pour 4 à 6 étapes.

### Limites connues
- Personnages : silhouettes plates stylisées, sans expressions ni gestes riches (étape 6.3) ; compréhension lexicale en français ; mesures de performance faites sur JVM, non sur appareil Android ; Web validé par la CI seulement.
- Un texte d'une phrase donne deux scènes (titre + appel à l'action) : pas de scènes inventées pour le remplir.
- Les autres formats (hors Scénario) gardent leur décor d'origine, dont une grande sphère verte en bas du format Élan.

## [2.9.0]

### Ajouté
- **Type « Attaque et défense 3D »** (cybersécurité, réseaux, systèmes, code, IA ; première recommandation pour les textes de cybersécurité) : en 3D, un poste attaquant à l'écran rouge envoie des paquets vers une baie de serveurs cible. Les scènes qui décrivent l'attaque restent en alerte rouge ; dès qu'une scène parle de parade (« comment l'éviter », étapes de défense, conclusion), deux boucliers descendent, les paquets s'arrêtent dessus et éclatent en vert, la cible passe en protégé. Repères « ATTAQUANT / CIBLE / BOUCLIER » et bandeau « ATTAQUE SIMULÉE » puis « DÉFENSE ACTIVE ». Tout est étiqueté « SIMULATION 3D » : aucun trafic réel, aucune attaque réelle.
- Le type « Démonstration 3D » devient une recommandation de premier rang pour les réseaux, la cybersécurité, le code et l'IA.
- Moteur 3D : boucliers extrudés, pièces qui apparaissent avec un décalage, projection d'un point 3D à l'écran (pour les repères et les paquets).

## [2.8.0]

### Ajouté
- **Type « Démonstration 3D »** (informatique : réseaux, systèmes, cybersécurité, code, IA) : un objet en volume tourne doucement au-dessus du texte, avec ombre au sol et anneaux lumineux. Trois objets, choisis d'après le sujet : **ordinateur portable** (qui s'éclate en écran, clavier, carte mère avec processeur et mémoire, batterie, châssis, avec repères nommés), **baie de serveurs** (les serveurs coulissent hors de la baie) et **cadenas** (l'anse s'ouvre). L'objet reste assemblé en ouverture, s'éclate pendant les étapes et se réassemble en conclusion.
- **Moteur 3D logiciel** (`Engine3D.kt`) en Kotlin commun : perspective, faces cachées écartées, tri du plus loin au plus proche, éclairage par face. Même image sur Android, navigateur et tests ; aucune permission, aucune bibliothèque.
- Étiquetage : « ILLUSTRATION 3D » sur chaque scène. C'est de la 3D stylisée à facettes : ni texture, ni reflet, ni ombre précise, ni personnage. L'objet prend toute la hauteur que le texte laisse libre ; si le texte est trop long, l'objet disparaît de la scène plutôt que de couper le texte.
- Non proposé pour les sujets sensibles ni hors informatique.

## [2.7.0]

### Modifié
- **L'application s'appelle VIDEO24** (nom affiché, écrans, page web, installation sur l'écran d'accueil). L'identifiant technique (`io.github.mensan2024.codjia`), le dépôt et les données déjà enregistrées sur l'appareil ne changent pas : les mises à jour continuent de fonctionner. Les vidéos de la galerie sont désormais rangées dans « Movies › VIDEO24 ».
- **Plus aucun logo ni nom d'application dans les vidéos produites** : pied de page supprimé, mentions « CODJIA » des formats remplacées par des libellés neutres, nom retiré des noms de fichiers exportés et des métadonnées du titre.
- Descriptions des types corrigées : « Démo à l'écran » (fenêtres et clics *simulés*) et « Cinéma » (téléphone *illustré*, plus « réaliste »).

### Ajouté
- **Type « Démonstration intelligente »** : chaque explication devient une démonstration visuelle animée. Simulations étiquetées « SIMULATION » ou « ILLUSTRATION » : paquets et VLAN (réseau), échange client / serveur et éditeur (code), entonnoir de conversion (marketing), progression d'habitude. Une étape qui n'a pas de schéma propre reçoit la simulation de son domaine (réseau, code, marketing). Non proposé pour les sujets sensibles (santé mentale, biblique). Anatomie et situations humaines : pas encore disponibles.

## [2.6.1]

### Ajouté
- **Profondeur et mouvement** (formats Sérénité et Lumière) : trois plans de collines en bas de l'image, qui dérivent à des vitesses différentes (parallaxe lente). Le mouvement suit l'horloge globale : il continue sans à-coup d'une scène à l'autre, et remplit le tiers inférieur auparavant vide.

### Corrigé
- Format Démo : une liste d'actions n'apparaît plus dans une fausse fenêtre « Paramètres » avec des interrupteurs. Les interrupteurs restent réservés aux étapes qui parlent vraiment de réglages ; sinon, fenêtre « Liste d'actions » avec cases cochées une à une par le curseur.

## [2.6.0]

### Ajouté
- **Démonstrations intelligentes** : quatre simulations animées déclenchées automatiquement par le contenu du texte, toutes étiquetées « SIMULATION » ou « ILLUSTRATION », sans aucun chiffre inventé : pare-feu (réseaux, cybersécurité, systèmes), entonnoir de conversion (marketing), échange client / serveur API avec codes de statut (développement), progression d'habitude jour après jour (développement personnel, santé).
- `docs/AUDIT_PARTIE5.md` : audit honnête du moteur (rendu vectoriel 2D procédural, pas d'import d'images ni de vidéos, pas de 3D), architecture proposée de « fournisseurs visuels » avec repli procédural, niveaux de production, décisions à valider.

## [2.5.4]

### Ajouté
- **Format « Stratégie »** (famille marketing, première direction recommandée) : tableau de bord sombre, courbe de tendance décorative (aucune valeur inventée), carte publicitaire générique sans marque en ouverture, et tunnel de conversion : chaque étape d'une liste devient un étage plus étroit que le précédent. Trois ambiances (marine, anthracite, violet).

### Modifié
- Choix automatique : la première génération prend la meilleure direction de la famille détectée ; « Autre version » varie ensuite parmi les sept premières directions admissibles, en évitant les deux derniers formats.

## [2.5.3]

### Ajouté
- **Format « Élan »** (famille développement personnel, première direction recommandée) : soleil qui se lève lentement, titres condensés en majuscules, grands numéros, chaîne de points qui se remplit de scène en scène, cases cochées en conclusion. Trois ambiances (aube, or, vert), énergique mais sans flash.

### Corrigé
- Les lignes sans puce écrites après une liste ne sont plus numérotées comme une étape de plus (« Quatre habitudes » n'affiche plus 5 étapes) : elles passent dans leur propre scène, dans l'ordre de l'auteur.
- Une conclusion de type définition (« la discipline, c'est choisir… ») n'est plus coupée en deux puces.

## [2.5.2]

### Ajouté
- **Format « Lumière »** (famille parole biblique, première direction recommandée) : étoile de lumière, trois ambiances chaudes (nuit étoilée, parchemin, aube ambrée), serif centré. Une référence biblique collée à une citation (« Jean 3:16 ») est isolée sur sa propre ligne, telle que l'auteur l'a écrite, sans rien ajouter ni modifier. Intitulés neutres « Pour méditer » / « MÉDITATION » au lieu de « Le saviez-vous ? ».

## [2.5.1]

### Ajouté
- **Format « Sérénité »** (famille santé mentale, première direction recommandée) : dégradé doux (crépuscule, sable ou sauge), cercle lumineux qui respire (simple décor, aucune consigne), poussières de lumière, serif centré, apparition douce, transitions en fondu. Tout le texte de l'auteur reste affiché (tests de conservation, de zone sûre et de chevauchement passés sur les 24 textes de référence).

### Corrigé
- Le format Cinéma n'affiche plus de faux téléphone « Réglages / Sécurité » sur les sujets apaisants ; reconnaissance des mots entiers (plus de faux positif sur « code », etc.).
- Éditorial : plus de lettrine qui coupe un mot court (« Tu », « Il »…).

## [2.5.0]

### Ajouté (moteur)
- **Compréhension du contenu** (`ContentAnalyzer`) : famille (santé mentale, parole biblique, développement personnel, marketing, réseaux, développement, cybersécurité, IA, santé, anglais), public, objectif, ton, registre, niveau, type de conclusion, indice de confiance et indices relevés. Analyse hors ligne par lexiques pondérés et règles (pas d'IA générative) ; un texte ambigu ou très court est signalé par une confiance basse.
- **Direction artistique** (`ArtDirector`) : trois propositions par famille (mode « Recommandé »), choix automatique parmi les directions admissibles, mode manuel. Chaque direction porte son ambiance, sa musique et la raison du choix.
- Sujets sensibles (santé mentale, parole biblique) : formats bruyants exclus (flash, journal, terminal, néon, collage, montage), palettes et animations apaisées, aucun appel à l'action inventé.
- 8 textes de référence et 10 tests dédiés (six cas types, ambigus, très court).

### Corrigé
- Aucun mot de l'auteur n'est plus coupé : titres Infographie sur plusieurs lignes, étapes et tableau blanc qui réduisent la taille avant de tronquer, paragraphe d'avertissement en fin de liste affiché dans sa propre scène « À savoir » (ordre conservé).
- Textes très courts : durée plancher de 16 s.

### Limites connues
- Les directions réutilisent les 18 formats existants ; les illustrations propres à chaque famille et la bibliothèque de styles étendue restent à construire. L'interface web n'expose pas encore le profil ni les trois recommandations.

## [2.4.3]

### Corrigé (version web)
- Aperçu : la taille de la vidéo laisse toujours la place à la barre de lecture au-dessus de la barre d'outils ; les quatre boutons tiennent sur une ligne même sur un écran de 360 px.

## [2.4.2]

### Corrigé (version web)
- Aperçu sur petit écran : la barre de lecture n'est plus recouverte par la barre du bas (marge basse suffisante).

## [2.4.1]

### Corrigé (version web)
- Barre du bas : fond opaque (le texte de confidentialité ne passe plus derrière les boutons), message de confirmation placé au-dessus de la barre, libellés jamais coupés, astuce de première utilisation compacte.

## [2.4.0]

### Ajouté (version web)
- Présentation courte sous le logo (réduite dès qu'on écrit, fermable).
- Barre d'outils fixe sur une seule ligne : Voix · Format · Musique · Autre version, avec icône, libellé court et état allumé ; défilement horizontal sur écran très étroit.
- Aperçu : état de chargement, lecture / pause / reprise, barre de progression cliquable, temps écoulé ; taille adaptée à l'espace disponible.
- Sauvegarde automatique (sur l'appareil) du texte, de la voix off et du volume de musique.
- Aide facultative (bouton « ? ») et astuce de première utilisation.

### Corrigé
- « Essayer un exemple » charge maintenant un exemple différent à chaque touche (il ne faisait rien, car l'exemple était déjà dans le champ). Le champ démarre vide ; « Créer ma vidéo » demande un texte s'il est vide.

## [2.3.0]

### Ajouté
- Démonstrations visuelles par domaine dans le format Infographie, toutes étiquetées « Simulation » ou « Illustration » : trajet d'un paquet avec TTL (réseaux), VLAN isolés par un commutateur (réseaux), prompt vague contre prompt précis (IA), éditeur de code avec erreur puis correction (développement), rythme cardiaque et quatre habitudes (santé), SMS piégé vers faux site (hameçonnage).

### Corrigé
- Un texte sur l'hameçonnage n'affiche plus le schéma des mots de passe, hors sujet.

## [2.2.0]

### Ajouté
- Quatre formats : Journal télévisé, Flash info, Terminal, BD.
- Test « chaque format montre tout le texte de l'auteur » (tous formats × textes d'exemple).

### Corrigé
- Conservation intégrale du texte : les titres de section de l'auteur (« Comment se comporter si tu as cliqué : », « Sur tes propres machines ou avec autorisation : »…) sont conservés tels quels, dans leur ordre, sans remplacement par un titre générique.
- Frappe du texte terminée avant la fin des scènes courtes ; titres longs et étapes longues non coupés.
- Logo : seul « CODJIA » reste affiché pendant la lecture.

## [2.1.1]

### Ajouté
- Deux nouvelles mises en page du format explicatif (« carte dessous », « bandeau ») ; le format explicatif prend désormais palette, fond et variantes (légende, cadre, police) du moteur de styles : des centaines de combinaisons.
- Web : le bouton « Format » revient dans l'aperçu (tous les formats, anciens et nouveaux).

### Modifié
- Tous les formats ont la même chance d'être proposés (plus de format favorisé).
- Pied de page : seulement le logo CODJIA (plus de hashtag en bas à droite).
- Format explicatif : plus de logo ni de texte en haut.
- Web : choix de musique réduit (une par ambiance), sans filtres.

## [2.1.0]

### Ajouté
- Nouveau format « Explicatif simulé » (le plus fréquent par défaut) : un chapitre par idée, avec carte de chapitre qui se transforme en carte « À FAIRE », simulation d'interface au centre (liste qui se coche, note qui s'écrit, comparaison, chiffre qui monte, bouton S'abonner), progression « 02 / 09 » et explication en grosse typographie, par groupes de mots, avec le mot clé sur fond clair. Quatre ambiances de couleurs, tirées au hasard.

## [2.0.2]

### Modifié
- Web : interface épurée. Accueil : un seul exemple (« Essayer un exemple »), texte d'introduction court. Aperçu : plus de pastilles de détails, plus de choix de langue, seulement Voix off, Musique, Autre version et Générer.

## [2.0.1]

### Corrigé
- Web : l'interface s'affiche immédiatement ; les polices (≈ 6 Mo) se chargent en parallèle et en arrière-plan au lieu d'être téléchargées l'une après l'autre avant tout affichage.
- Web : si l'application ne démarre pas (erreur ou chargement > 15 s), un message clair et un bouton « Réinitialiser et recharger » remplacent le logo infini.

## [2.0.0] — 2026-11-25
### Voix off dans la version web
- **Voix off : oui / non** sur l'aperçu, comme dans l'application Android, avec choix de la langue (auto, français, anglais).
- **Voix neuronales Piper** (français « siwis », anglais « alba », CC BY 4.0) exécutées dans le navigateur avec ONNX Runtime Web : rien n'est envoyé. La voix (≈ 63 Mo) se télécharge une seule fois et reste dans le cache du navigateur. Versions et empreintes figées dans `tools/voice/prepare_web_voice.sh`.
- La musique baisse sous la voix ; le scénario s'allonge si la phrase le demande (plafond 59 s, lecture un peu accélérée si besoin). Même logique pour Android et le web (`Narration.prepare`, testée).

### Ta propre musique
- **Importer ma musique** (web et Android) : MP3, M4A, WAV, OGG… Le fichier reste sur l'appareil et réapparaît dans « Mes musiques » à la visite suivante ; une corbeille permet de le retirer.

### Durée selon le texte
- La durée suit la longueur du texte (≈ 5 s + 0,33 s par mot affiché, 12 à 59 s) au lieu de tendre vers la même valeur. Le message n'est jamais coupé.

### Styles
- **Infographie stylable** : palettes, fonds, polices, cadres, en-têtes, effets de titre et animations d'entrée viennent du moteur de styles (3 palettes et un fond de plus). La mention « Sécurité informatique pour tous » n'apparaît plus dans l'en-tête : CODJIA convient à tous les sujets.
- Les décors partagés sont dans `KitDecor` (même rendu pour « Style unique », « Montage dynamique » et « Infographie »).

### Interface web
- Refonte : étapes Texte → Aperçu → Vidéo, estimation de durée pendant la saisie, aperçu avec pause, anneau de progression, boutons et cartes plus lisibles, mêmes polices que les vidéos.
- **Mises à jour automatiques** : la page vérifie la version au lancement, au retour sur l'appli et toutes les 20 minutes, puis se recharge d'elle-même (jamais pendant la fabrication d'une vidéo).

## [1.9.0] — 2026-11-25
### Format « Infographie » enrichi (style des astuces cybersécurité)
- **Schémas animés par étape**, choisis selon le contenu : attaque en direct (essais de mots de passe, cadenas forcé), barres de combinaisons (26⁶ → 309 millions, phrase de passe → milliards de milliards), chaîne MFA (code à 6 chiffres bloqué), compteur de tentatives et bannissement d'IP avec un terminal fail2ban, graphique des échecs d'authentification avec seuil d'alerte et commande `grep … /var/log/auth.log`.
- **Chapitres** (« N réflexes », « Comment se défendre ? »), compteur « DÉFENSE n/total », en-tête « ASTUCE NNN · série », fond rouge pour l'attaque et bleu pour la défense, carte de fin « Abonne-toi · Prochaine : Astuce NNN+1 ».
- **Timing** : les étapes riches reçoivent plus de temps et leurs animations sont resserrées, pour que chaque schéma se termine avant la scène suivante (durée toujours ≤ 59 s).

## [1.8.0] — 2026-11-25
### Application web (iPhone, Android, ordinateur)
- **CODJIA fonctionne dans le navigateur**, sans rien installer, et s'installe sur l'écran d'accueil comme une application (PWA). Adresse : `https://mensan2024.github.io/codjia/`.
- **Même moteur que l'application Android**, compilé en JavaScript : mêmes formats, styles modulables, musiques et règles (tout le texte montré, 30 à 59 s).
- Même parcours : texte → aperçu animé avec musique → style / musique → génération → enregistrer ou partager.
  - Sur iPhone : « Partager » puis « Enregistrer la vidéo » la met dans Photos.
- **Tout se passe sur l'appareil** : aucun texte ni vidéo envoyé sur Internet. Fonctionne hors ligne une fois ouverte.
- Export MP4 1080 × 1920 :
  - vidéo H.264 encodée par le navigateur (WebCodecs) ;
  - son AAC, ou Opus si l'AAC n'est pas disponible ;
  - fichier assemblé par un muxer MP4 écrit pour CODJIA (« faststart », lecture immédiate sur iPhone).
- Navigateurs : Chrome, Edge, Samsung Internet, Firefox 130+, Safari 16.4+ (iOS 16.4+). Un navigateur trop ancien affiche l'aperçu et explique comment mettre à jour.
- Tests :
  - le MP4 assemblé est décodé sans erreur par ffmpeg, avec AAC et avec Opus ;
  - l'application web a été testée dans Chrome (format téléphone et ordinateur) : une vidéo de 57 s est générée en 40 s, avec image et son.
### Moteur
- Moteur 100 % Kotlin commun (sans API Java) : mise en forme des nombres, accents, WAV, angles.
- Icônes : « bureau » ne déclenche plus la goutte d'eau, « technologie » ne déclenche plus l'œil (mots entiers).

## [1.7.0] — 2026-11-25
### Styles modulables
- Nouveau format **« Style unique »** : chaque vidéo reçoit une **combinaison nouvelle** de pièces interchangeables, ce qui fait des milliers de styles possibles. Chaque pièce a plusieurs variantes :

  | Pièce | Variantes |
  | --- | --- |
  | Palettes | 9, dont 3 claires (Charbon orange, Minuit cyan, Violet néon, Noir citron, Marine soleil, Forêt menthe, Crème encre, Blanc bleu, Papier rouge) |
  | Fonds animés | 8 : grille en perspective, étoiles, aurore, halos, lignes néon, points, papier, planète |
  | Polices de titre | 6 ; 4 polices pour les sous-titres |
  | Mot clé du titre | surligné, trait de marqueur, en couleur, machine à écrire |
  | En-têtes | badge numéroté, pastille centrée, barre de couleur |
  | Cadres de carte | double, verre, néon, sans cadre |
  | Sous-titres | karaoké encadré, mot en couleur, mots géants, bandeau |
  | Entrées de plan | montée, chute, zoom, « pop », avec ou sans éclair à la coupe |
  | Mascotte | boule à antenne, petit robot, sans mascotte |
- Des **règles de design** écartent les combinaisons illisibles :
  - palette claire → fond clair, jamais de néon ;
  - fond chargé → toujours un cadre ;
  - pas deux effets forts ensemble…
- Ce format revient le plus souvent dans la rotation ; « Autre version » donne une nouvelle combinaison. L'aperçu affiche le nom du style tiré (par exemple « Marine soleil · Aurore · Mot en couleur »).
- Le « Montage dynamique » reste disponible tel quel (c'est l'une des combinaisons, figée).
- Nouveau test : 24 combinaisons tirées au hasard, vérifiées toutes les 1,2 s sur toute la vidéo (texte dans l'écran, aucun chevauchement) : 32 000 textes contrôlés.
- Titres placés d'après les mesures réelles de chaque police.

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
