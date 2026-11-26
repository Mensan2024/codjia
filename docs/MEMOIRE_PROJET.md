# Mémoire du projet CODJIA

Ce document résume l'historique, les décisions et le cahier des charges en cours de CODJIA. Il sert à reprendre le travail d'une session à l'autre sans perdre le contexte.

Dernière mise à jour : 26 novembre 2026. Auteur : CODJIA (Mensan2024).

---

## 1. Origine du projet

1. **Vidéos « Astuces cybersécurité » faites à la main avec Claude**, au format vertical 1080 × 1920, de 28 à 50 s, avec sous-titres incrustés, musique et bruitages générés, sans voix off. Elles sont publiées sur LinkedIn avec les hashtags `#securiteinformatiquepourtous #BonnesPratiques #codjia #Iamnotperfect`.
   - 021 : vérifier les permissions des applications (Windows, Linux avec Flatpak, Snap et AppArmor, macOS)
   - 022 : sécuriser le Bureau à distance (RDP) : désactiver, limiter les utilisateurs, mots de passe robustes, MFA, ne pas exposer le port 3389
   - 024 : audit de sécurité : comptes, permissions, ports, services, mises à jour, rapport. Commandes Linux et PowerShell
   - 025 : certificats numériques HTTPS, avec la banque fictive « mabanque.bf »
   - 026 : force brute. Une version longue de 50 s, puis une version courte de 30 s jugée meilleure : la définition et les 4 défenses seulement
   - 027 : archives **compressées ET chiffrées** (AES-256) avec 7-Zip, `7z a -p -mhe=on` et `hdiutil`. Un ZIP ordinaire ne protège rien.
   - Conventions : IP prises dans les plages de documentation, marques et banques fictives, interfaces redessinées sans logos, commandes exactes.
2. **Kit_Video_Astuces** : un kit Python (Pillow, numpy, ffmpeg) pour fabriquer ces vidéos hors ligne sans abonnement. On ne modifie que `astuce.py`.
3. **Astuce2Video v0.1** : une application Python (Flask et ligne de commande, licence MIT). On colle le texte d'une astuce et on obtient un MP4 de 40 s au plus. Elle a 7 types de scènes, choisit les icônes selon les mots, anime les commandes dans un terminal et génère sa musique.
4. **Application mobile open source** : le projet a d'abord été nommé IT_MENTOR (dossier de conception phase 0 et 1, décisions D1 à D10, dans Claude Docs « Astuce2Video Mobile — Dossier de conception »). Il est devenu **CODJIA** (dépôt `Mensan2024/codjia`).

## 2. Décisions de conception déjà prises (phase IT_MENTOR, toujours valables sauf mention)

- Android natif en **Kotlin**. Rendu dans le téléphone : Canvas, puis **Media3 Transformer** (`OverlayEffect` et `BitmapOverlay`). Pas de FFmpeg, car FFmpegKit est abandonné depuis avril 2025. Repli possible : MediaCodec avec `lockHardwareCanvas`, puis MediaMuxer.
- **Principe du moteur :** une image est une fonction du temps. L'aperçu et l'export appellent la même fonction. Le rendu est déterministe (graine tirée du projet, polices embarquées, ressorts précalculés), donc testable avec des images de référence.
- **Domaines sous forme de packs de données** (JSON, icônes, règles). Ajouter un domaine ne demande aucun changement du moteur. Le pack Santé réinjecte d'office sa mention de prudence.
- **IA facultative et indépendante du fournisseur** (contrat `AiProvider`, un adaptateur par famille d'API : Gemini, Anthropic, compatible OpenAI). L'utilisateur fournit **sa propre clé** (option A). La clé est chiffrée par le Keystore en AES-256-GCM. Tout storyboard issu de l'IA porte le drapeau `aiGenerated` et peut être signalé. Mode texte hors ligne en repli.
- **Voix off** : la synthèse vocale Android, gardée seulement si une voix est réellement disponible hors ligne. Sous-titres toujours présents.
- **Licence** : GPL-3.0-or-later pour le code, CC BY-SA 4.0 pour les packs, OFL pour les polices.
- **Distribution** : Google Play (compte personnel, donc 12 testeurs pendant 14 jours, signalement du contenu généré par IA, aucune donnée collectée) et F-Droid (aucune bibliothèque propriétaire, compilation reproductible, anti-fonctionnalité NonFreeNet pour le mode IA).
- **Langues** : français en v1, puis anglais. Mooré et dioula d'abord en sous-titres, faute de voix de synthèse.
- **Encore ouvert :** l'identifiant d'application définitif. Proposition faite pour IT_MENTOR : `io.github.mensan2024.itmentor`, jamais validée. Il faut en fixer un pour CODJIA avant la première publication sur les boutiques.
- **Contrainte d'environnement :** dans les sessions cloud, `dl.google.com` (SDK Android) est bloqué par défaut. L'utilisateur a refusé le 24/11/2026 la création d'un environnement qui l'autorise. La compilation Android passe donc par GitHub Actions (CI du dépôt).

## 3. État actuel de CODJIA (v2.4.3, 26/11/2026)

- Modules : `engine-core` (Kotlin pur : chronologie, templates, planificateur, détection du domaine, musique), `engine-jvm` (banc d'essai), `engine-android` (Canvas et Media3), `web` (PWA : moteur compilé en JS, export WebCodecs, en ligne sur https://mensan2024.github.io/codjia/), `demo-app`, `tools/music`, `tools/voice`.
- 11 formats, plus de 500 000 combinaisons de styles, voix Piper dans le navigateur, musiques importées, conservation intégrale du texte (2.2.0), démonstrations par domaine (2.3.0), refonte de l'interface web (2.4.x).
- **Durée** : le README actuel dit de 30 à 59 s (environ 5 s plus 1/3 de seconde par mot). Ça diffère de la règle d'origine « 40 s au plus ». Il faut confirmer laquelle est voulue.
- Restait à faire après la 2.4.3 : la Partie 1 (textes longs), puis les exports 16:9 et 1:1 (Partie 2). L'écran de 360 × 640 oblige à faire défiler d'un cran. L'application Android n'a pas été modifiée par la refonte web.
- Principe de communication : ne pas écrire « grâce à l'intelligence artificielle », car le moteur tourne sur l'appareil, sans IA.

## 4. Cahier des charges en cours : PARTIE 4 — Studio intelligent de création vidéo

**Vision :** CODJIA comprend le contenu, choisit une direction artistique adaptée au **sens** du texte (pas à quelques mots-clés), construit une narration captivante et produit une vidéo professionnelle. Ce ne sont pas des diapositives de texte sur un fond animé.

### 4.1 Analyse du contenu (priorité absolue)
Le moteur doit identifier le sujet, le domaine, le public, l'objectif, le ton, les émotions, les notions à illustrer, les passages à démontrer visuellement et le type de conclusion. Il en déduit une direction artistique.

### 4.2 Six familles spécialisées
Chaque famille a ses propres compositions, animations, transitions, illustrations, typographies, musiques et structures narratives.
- **A. Santé mentale et bien-être** : ambiance apaisante et humaine, animations douces, textes courts. Pas de diagnostic, pas de promesse thérapeutique, pas de formulation culpabilisante.
- **B. Marketing et entrepreneuriat** : motion design dynamique, publicités simulées (marques fictives), graphiques, tableaux de bord, tunnels de conversion.
- **C. Réseaux et systèmes** (CCNA, VLAN, EtherChannel, routage, Linux, Windows Server, Wi-Fi, pare-feu, cloud) : topologies animées, flux de paquets, terminaux, configurations. Commandes exactes.
- **D. Full-stack et programmation** : éditeur de code animé, échanges client-serveur, bases de données, erreur puis correction. Ne jamais présenter une simulation comme une vraie exécution, ni inventer du code présenté comme fonctionnel.
- **E. Parole biblique et spiritualité** : ambiance sobre et contemplative, lumière naturelle, typographies élégantes. Références exactes. Ne jamais inventer ni mal attribuer un verset.
- **F. Développement personnel** : scènes humaines, progression visuelle, typographie cinétique, musique motivante.

### 4.3 Bibliothèque de styles
Cinématique, documentaire, JT, motion design, minimaliste, 3D, illustré, futuriste, éducatif interactif, storytelling, publicité premium, tableau blanc, interface logicielle, simulation technique, nature, inspiration.
Chaque style définit sa palette, ses fonds, ses transitions, ses animations, ses typographies, ses éléments, ses sons, ses règles de composition et ses scènes préférées.
Trois modes : **Automatique**, **Recommandé** (3 thèmes proposés) et **Manuel**, avec un aperçu avant validation.

### 4.4 Narration qui retient l'attention
- Accroche forte dès les premières secondes (question, situation, problème reconnu).
- Développement progressif, en alternant les plans.
- Conclusion adaptée : synthèse, action, invitation.
- La structure varie selon le domaine. À éviter : intros longues, répétitions, écrans chargés, transitions gratuites.

### 4.5 Richesse visuelle
Chaque scène apporte une information. Les visuels sont synchronisés avec le propos (exemple d'un pare-feu : trafic, paquets autorisés et bloqués, puis règles). Aucun fait inventé.

### 4.6 Accroches
Le moteur propose plusieurs accroches. L'utilisateur peut en choisir une, en demander d'autres, la modifier ou garder son intro. **Jamais de modification silencieuse.**

### 4.7 Personnalisation
Domaine, public, style, ambiance, niveau de langage, durée, format, narration, musique, voix, intensité des animations. Réglages automatiques pour les débutants, options avancées pour les autres.

### 4.8 Interface
Garder l'identité CODJIA (sombre, orange et rose), le grand aperçu, la barre fixe en bas (Voix · Format · Musique · Autre version) et le bouton « Générer la vidéo » bien visible. Ajouter une bibliothèque visuelle des thèmes et des recommandations claires.

### 4.9 Audio
Cohérence entre la voix, la musique, les scènes et les sous-titres. La musique baisse sous la voix (ducking). Voix synthétique, enregistrement de sa propre voix, import d'un fichier audio, mode sans voix. Scènes et sous-titres synchronisés sur la narration.

### 4.10 Conservation intégrale (impératif)
Ne jamais supprimer ni résumer le texte. Toute amélioration rédactionnelle demande l'accord de l'utilisateur. Un texte long donne une vidéo plus longue ou plusieurs épisodes.

### 4.11 Plateformes et confort
Android, iPhone, tablettes, Windows, Linux, macOS. Sauvegarde, historique, favoris, duplication, export, reprise d'une création interrompue, erreurs claires, connexion instable, mémoire et performances.

### 4.12 Architecture modulaire
Analyse sémantique → classification → direction artistique → scénario → découpage → visuels → narration → musique → montage et rendu → export. Ajouter un domaine ou un thème ne doit pas demander de tout réécrire. **Aucune fausse fonctionnalité** (données fictives présentées comme réelles, boutons sans action).

### 4.13 Tests obligatoires
Santé mentale, stratégie marketing, cours sur les VLAN, tutoriel API REST, message biblique, discipline personnelle. Pour chacun : classification, thème, pertinence des scènes, narration, musique, lisibilité, conservation du contenu, rendu final. Plus des textes ambigus, très courts et très longs. Le même texte doit produire des vidéos réellement différentes selon le domaine.

### 4.14 Méthode, dans cet ordre, une étape à la fois
1. **Auditer le projet existant et présenter son architecture** ← prochaine étape
2. Concevoir le moteur d'analyse et de sélection automatique des thèmes
3. Construire les 6 familles
4. Améliorer les scènes, les animations et la narration
5. Bibliothèque de styles et recommandations
6. Interface et performances
7. Tester de vraies vidéos et corriger

À chaque étape : fichiers concernés, explications, aucune régression, tests exécutés, bilan honnête. Ne jamais dire « terminé » sans l'avoir testé.

### 4.15 Recommandation stratégique de l'utilisateur
Viser d'abord **6 familles × 3 styles vraiment différents = 18 combinaisons excellentes**, plutôt que 500 variantes superficielles. Mesurer sur de vraies vidéos : temps de génération, fluidité, cohérence, lisibilité, fidélité au texte.

### 4.16 Idées pour la suite
Storyboard modifiable avant le rendu, générateur d'accroches par plateforme (TikTok, Reels, Shorts, LinkedIn), démonstrations intelligentes, adaptation multiplateforme qui réorganise les scènes au lieu de recadrer, analyse de qualité avant l'export, bibliothèque communautaire de modèles.

## 5. Préférences de travail de l'utilisateur
- Tout en **français**, avec tutoiement.
- Se mettre d'accord **avant** de développer. Avancer par étapes validées.
- Honnêteté stricte sur ce qui est testé et ce qui ne l'est pas. Pas de promesse inexacte.
- Vidéos courtes. L'utilisateur a préféré la version de 30 s à celle de 50 s.
- Rester gratuit et open source, sans dépendre d'un abonnement payant.
