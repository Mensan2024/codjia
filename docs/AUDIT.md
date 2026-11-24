# CODJIA — Audit du dépôt avant évolution (phase A)

Date : 24 novembre 2026. Point de départ : la branche `main`, commit `a21b5d0`.

## Versions validées sur le téléphone, conservées

Les étiquettes Git ne passent pas par le proxy de cette session. Les versions validées sont donc gardées sous forme de **branches** (on peut les comparer et les recompiler à tout moment) :

| Branche | Contenu | Validée sur Galaxy A16 |
| --- | --- | --- |
| `version/v0.1-moteur` | Moteur vidéo seul, export MP4 muet | Oui : 31,8 s, 954 images, 30 i/s, export en 45 s |
| `version/v0.2-voix-off` | Moteur + voix off hors ligne | Oui : « c'est bon » |
| `main` | Version CODJIA décrite ci-dessous | À valider |

## Composants opérationnels, conservés tels quels

| Composant | Fichier | État |
| --- | --- | --- |
| Chronologie, limite de 40 s, chevauchement des transitions | `Timeline.kt` | Inchangé |
| Courbes d'accélération et 8 animations | `Easing.kt`, `Animation.kt` | Inchangés |
| Mise en page du texte (retours à la ligne, espaces insécables, réduction de taille, pas de mot seul en dernière ligne) | `TextLayout.kt` | Inchangé |
| Rendu Android sur Canvas | `AndroidCanvasGfx.kt` | Ajout de `clipPath` et du catalogue de polices partagé |
| Export Media3 Transformer (H.264 + piste audio AAC) | `Media3VideoExporter.kt` | Inchangé (la piste audio transporte maintenant la musique) |
| Relecture du MP4 sur le téléphone | `Mp4Inspector.kt` | Ajout du titre affiché dans la Galerie |
| Choix du format selon l'encodeur | `DeviceProfile` | Inchangé |
| Compilation de l'APK par GitHub Actions | `.github/workflows/apk.yml` | Inchangée |

## Retiré, comme demandé

- La voix off : `OfflineVoice.kt`, `Narration.kt`, le champ `narration`, la déclaration du service de synthèse vocale dans le manifeste. Ce code reste consultable dans la branche `version/v0.2-voix-off`.

## Points de vigilance relevés

1. **Signature des APK.** Jusqu'ici, chaque compilation sur GitHub signait l'APK avec une clé de développement **nouvelle et aléatoire**. Une clé fixe est maintenant versionnée dans le dépôt (`demo-app/codjia-debug.keystore`), et toutes les versions suivantes s'installeront par-dessus la précédente. En revanche, l'APK déjà installé a été signé avec une clé perdue. Si Android refuse la mise à jour (« conflit avec un package existant »), il faut désinstaller **une seule fois**. Le prototype ne contient aucune donnée à perdre.
2. **Identifiant technique.** `io.github.mensan2024.itmentor.prototype` est conservé, comme demandé. Le nom visible devient CODJIA. Pour l'application complète publiée sur Google Play, l'identifiant définitif sera à décider en phase D (`io.github.mensan2024.codjia` est proposé).
