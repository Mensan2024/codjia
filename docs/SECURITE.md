# CODJIA — Modèle de menaces et mesures de sécurité (version 0.4)

## 1. Périmètre

Ce qui est protégé :
- le téléphone de l'utilisateur : stabilité, mémoire, stockage ;
- ses fichiers privés : textes, vidéos, musiques importées ;
- la chaîne de production des APK.

**Surface d'attaque.** Elle est volontairement réduite :
- l'application ne fait **aucun accès réseau** ;
- elle n'a qu'une activité exportée, le lanceur, qui ne lit aucune donnée d'Intent.

Elle ne reçoit que deux types d'entrées :
1. le **texte** saisi ou collé ;
2. un **fichier audio** choisi dans le sélecteur du système.

Le mode IA prévu (étape D8) ouvrira une troisième entrée, le réseau. Ses mesures sont prévues au §5.

## 2. Menaces et mesures (méthode STRIDE, ramenée à l'application)

| # | Menace | Vecteur | Mesure en place | Où |
| --- | --- | --- | --- | --- |
| M1 | Déni de service par un texte énorme ou piégé | Collage de plusieurs Mo, caractères de contrôle, emojis, inversion bidirectionnelle (U+202E) | Texte tronqué à 5 000 caractères à la saisie **et** dans le moteur. Liste blanche de caractères (latin, ponctuation), contrôles retirés, durée bornée à 40 s | `TextSanitizer`, `InputFilter.LengthFilter`, `PlannerTest` |
| M2 | Déni de service par un fichier audio | Fichier de plusieurs Go, durée de plusieurs heures, 64 canaux, fréquence aberrante, conteneur malformé | Refus au-delà de 60 Mo ou 30 min, fréquence entre 8 et 48 kHz, mono ou stéréo, type `audio/*` uniquement. Décodage **borné** à la durée de la vidéo + 3 s (≈ 12 Mo de PCM au maximum pour 59 s, quel que soit le fichier). Délai maximal de 60 s. Analyse hors du fil d'interface | `ImportPolicy`, `MusicCatalog.fromUserFile`, `AudioDecoder.decode`, `SecurityTest` |
| M3 | Injection par le nom de fichier | Nom avec `../`, caractères de contrôle, U+202E (faux `.mp3`) | Nom affiché nettoyé et limité à 60 caractères. Nom de fichier d'export en ASCII `[A-Za-z0-9_-]` | `ImportPolicy.displayName`, `safeFileStem` |
| M4 | Accès à des fichiers hors de ceux qu'a choisis l'utilisateur | URI `file://` ou chemin arbitraire | Seuls les URI `content://` du sélecteur sont acceptés. Aucune permission de stockage | `fromUserFile`, manifeste |
| M5 | Fuite des créations | Sauvegarde cloud, transfert d'appareil, fichiers temporaires lisibles | `allowBackup=false`, `fullBackupContent=false`, `dataExtractionRules` qui exclut tout. Fichiers de travail dans l'espace **privé**, supprimés après l'export, même en cas d'échec. La vidéo finale ne va que dans la Galerie, à la demande de l'utilisateur | manifeste, `MainActivity.generate` |
| M6 | Exfiltration de données | Bibliothèque tierce qui ajouterait un accès réseau | `INTERNET` et `ACCESS_NETWORK_STATE` retirées (`tools:node="remove"`). Texte en clair interdit (`networkSecurityConfig`). **La CI échoue** si une permission réseau ou de stockage apparaît dans l'APK | manifeste, `apk.yml` |
| M7 | Détournement d'Intent | Intent externe vers une activité | Une seule activité exportée (le lanceur). Partage par `ACTION_SEND` avec un droit de lecture temporaire sur l'URI MediaStore | manifeste |
| M8 | Rétro-ingénierie, code mort | APK de publication | R8 : réduction, obscurcissement, ressources inutiles retirées, `Log.v/d/i` supprimés | `build.gradle.kts`, `proguard-rules.pro` |
| M9 | Chaîne d'approvisionnement | Dépendance compromise ou abandonnée, Gradle Wrapper modifié, action GitHub détournée | Dépendances maintenues uniquement (pas de FFmpegKit). Dependabot hebdomadaire. `validate-wrappers`. Jeton CI en lecture seule (`permissions: contents: read`) | `.github/` |
| M10 | Vulnérabilités dans notre code | Erreur de programmation | CodeQL `security-extended` (Kotlin) à chaque modification de `main`, chaque PR et chaque semaine | `codeql.yml` |
| M11 | Usurpation de mise à jour | APK modifié par un tiers | Version de test (`…codjia.dev`) : clé de développement publique, **à ne pas diffuser hors tests**. Version publique : clé de publication détenue par le seul mainteneur, injectée par secrets GitHub dans un environnement protégé, effacée après usage. Empreinte SHA-256, empreinte du certificat et attestation de provenance Sigstore publiées avec chaque version | `release.yml`, `docs/PUBLIER.md` |
| M12 | Contenu protégé | Musique commerciale intégrée | Le catalogue ne contient que des compositions originales (GPL), documentées. La musique importée reste sous la responsabilité de l'utilisateur, ce qui est indiqué dans l'interface | `docs/MUSIQUE.md` |

## 3. Ce qui est vérifié automatiquement

| Vérification | Outil |
| --- | --- |
| Limites d'import, budget PCM, nettoyage des noms | `SecurityTest` (5 tests) |
| Texte nettoyé, limité, sans caractères interdits | `PlannerTest` |
| Aucune permission réseau ou de stockage dans les APK debug et release | Étape « Contrôle de l'APK » (`aapt2 dump permissions`) |
| Compilation de la version R8 | `assembleRelease` dans la CI |
| Analyse statique | CodeQL |
| Mises à jour de sécurité | Dependabot |

## 4. Risques résiduels connus

- **Décodeurs du système.** Le décodage audio passe par `MediaExtractor` et `MediaCodec`, qui tournent dans le processus média d'Android, isolé de l'application. Une faille de codec relève des correctifs Android. Les limites ci-dessus réduisent l'exposition sans la supprimer.
- **APK de test.** Sa clé est publique, par conception, pour permettre l'installation par-dessus la version précédente. Il ne doit pas circuler hors du groupe de test.
- **Android 8 et 9 (API 26 à 28).** La vidéo est copiée dans le dossier propre de l'application, faute de MediaStore relatif. Le bouton Partager est alors désactivé.

## 5. Mesures prévues pour le mode IA (D8)

- Clé d'API saisie par l'utilisateur, chiffrée avec Android Keystore (AES-GCM), jamais journalisée ni sauvegardée.
- Seul le module `core:ai` aura la permission INTERNET, en HTTPS uniquement. On pourra épingler les certificats du fournisseur.
- La réponse du modèle sera traitée comme une **entrée non fiable** : validation par un schéma, puis passage par `TextSanitizer` comme un texte collé. Elle ne pourra jamais déclencher d'action, seulement produire un storyboard.
- Mention « contenu généré par IA » et bouton de signalement (règles Google Play sur l'IA générative).
