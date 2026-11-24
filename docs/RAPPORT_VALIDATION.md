# CODJIA — Rapport de validation, version 0.3 (phases B et C)

Date : 24 novembre 2026.

## 1. Verdict

| Exigence | Résultat | Preuve |
| --- | --- | --- |
| Nom visible CODJIA (libellé, icône, écran de démarrage, dossier d'export, titres) | ✅ | Manifeste, icône adaptative, `Movies/CODJIA`, titre MediaStore |
| Identifiant technique et installation par-dessus l'ancienne version | ✅ identifiant conservé, clé fixe désormais. ⚠️ Désinstallation possible **une fois** (voir AUDIT) | `demo-app/build.gradle.kts` |
| Voix off retirée | ✅ | Code supprimé, toujours consultable dans `version/v0.2-voix-off` |
| Bibliothèque musicale par catégories, avec de vrais fichiers | ✅ 8 pistes originales, 7 catégories et « Sans musique » | `assets/music`, `docs/MUSIQUE.md` |
| Écoute, forme d'onde, durée, lecture et pause, import d'un fichier | ✅ (code compilé, à essayer sur le téléphone) | `MainActivity`, `WaveformView` |
| Musique dans le MP4 : AAC, fondus, volume, pistes longues et courtes | ✅ | Tests `MusicTest` + vidéo de démonstration |
| Synchronisation | ✅ L'audio et la vidéo démarrent à 0 ; écart de fin de 8 ms | ffprobe |
| 8 templates motion design distincts | ✅ | `docs/templates/`, `TemplateTest` |
| Recommandation selon le domaine, sans rien imposer | ✅ | `DomainTest` |
| Parcours : contenu → modèle → musique → aperçu → génération, avec retour possible | ✅ (code compilé, à essayer sur le téléphone) | `MainActivity` |
| APK compilé | ✅ GitHub Actions, exécution n° 45315639391 | Onglet *Actions* |

## 2. Vidéo de démonstration avec musique (relue avec ffprobe)

Fichier : `CODJIA_demo_cyber-neon_pulse.mp4`. Template **Cyber Neon**, musique **Pulse** (110 BPM), contenu « Repère un e-mail piégé ».

| Mesure | Valeur |
| --- | --- |
| Vidéo | H.264 High, 1080 × 1920, 30/1 i/s, 949 images, 31,633 s |
| Audio | AAC LC, 44,1 kHz, stéréo, 165 kb/s, 31,625 s |
| Début audio et vidéo | 0,000 s et 0,000 s |
| Écart de fin | 8 ms (inférieur à une image) |
| Niveau audio | −16,4 dBFS RMS, crête à −4,5 dBFS |
| Fondu d'entrée | −44 dBFS sur les 50 premières ms, puis −18,6 dBFS entre 0,5 et 1 s |
| Fondu de sortie | −80 dBFS sur les 50 dernières ms (silence propre) |
| Scènes calées sur le tempo | Chaque scène commence sur un temps de 545,5 ms |
| Taille | 9,7 Mo |
| Décodage complet | Sans erreur |

## 3. Tests automatiques : 38 sur 38 réussis

Ils passent sur la machine de développement (`./test.sh`) **et** dans l'intégration continue de GitHub (Gradle).

| Suite | Tests | Ce qui est vérifié |
| --- | --- | --- |
| `TimelineTest` | 6 | Démonstrations entre 25 et 40 s **pour chaque contenu × chaque template**, refus au-delà de 40 s, transitions, pas de dérive du temps |
| `EasingAndAnimationTest` | 5 | Courbes et 8 animations |
| `TextLayoutTest` | 7 | Retours à la ligne, espaces insécables, accents, mot trop long, réduction de taille, pas de mot seul en dernière ligne |
| `PathTest`, `FormatTest` | 2 | Tracés progressifs, formats adaptables |
| `TemplateTest` | 4 | 8 identités distinctes (8 décors, au moins 6 polices de titre, thèmes clairs et sombres), transitions jamais répétées deux fois de suite, contenu inchangé quand on change de template, **contraste** (texte ≥ 7:1, texte secondaire ≥ 4,5:1, bouton d'action ≥ 4,5:1) |
| `DomainTest` | 2 | Détection de 4 domaines et du cas « Général » ; recommandation de Cyber Neon, Medical Clean et Green Nature selon le contenu ; les 8 templates restent proposés |
| `MusicTest` | 5 | Scènes calées sur le tempo pour 6 tempos × 8 templates, volumes, piste plus longue et plus courte que la vidéo, fondus jusqu'au silence, volume réduit à 0,45, WAV stéréo |
| `RenderTest` | 6 | Images identiques à chaque rendu (8 templates), couverture du français par les 12 polices, **4 742 textes dans la zone sûre** (8 templates × 4 contenus), **aucun texte qui en chevauche un autre**, même composition en 720p et en 1080p |

Le test de chevauchement a trouvé un vrai défaut, corrigé depuis : un titre long en Marketing Impact poussait le sous-titre sur le pied de page. La couverture remonte maintenant son bloc de texte quand c'est nécessaire.

## 4. Performances (banc d'essai JVM, 2 vCPU)

| Mesure | Cyber Neon, 1080p, avec musique |
| --- | --- |
| Rendu seul | 26,4 s pour 949 images, soit 27,8 ms par image en moyenne (95ᵉ centile : 45,5 ms) |
| Total avec l'encodage | 32,8 s |
| Mémoire | Tas Java : 80 Mo au maximum. Mémoire résidente : 226 Mo pour Java, 322 Mo pour ffmpeg |

Repère sur le Galaxy A16 (version 0.1) : 41 ms par image et un export de 45 s. Les templates ajoutent des effets (halos, volets, grain), donc il faut **mesurer à nouveau sur le téléphone** : l'application affiche ces chiffres à la fin de chaque génération.

## 5. Ce qui reste à vérifier sur le téléphone

1. L'installation par-dessus l'ancienne version, sinon une désinstallation unique.
2. L'écoute des musiques, la forme d'onde, l'import d'un fichier audio.
3. L'aperçu animé avec la musique (fluidité sur le Galaxy A16).
4. La génération avec musique. Le rapport affiché doit indiquer : `Audio : audio/mp4a-latm ✓` et un écart audio/vidéo de quelques millisecondes.
5. Les temps de rendu des 8 templates.
