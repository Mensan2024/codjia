# Publier une version de CODJIA sur GitHub Releases

Ce guide se fait **une seule fois** pour la clé (étapes 1 à 3). Ensuite, chaque version se publie en un clic (étape 4).

## 1. Créer la clé de publication (sur ton ordinateur, jamais ailleurs)

La clé de publication prouve que chaque mise à jour vient de toi. **Si tu la perds, plus aucune mise à jour ne pourra s'installer par-dessus** : les utilisateurs devraient désinstaller l'appli. **Si quelqu'un la vole**, il peut diffuser une fausse mise à jour. Elle reste donc chez toi : ni dans le dépôt, ni par e-mail, ni dans une conversation.

Il faut un JDK (17 ou plus) pour avoir `keytool`. Sur Debian ou Ubuntu : `sudo apt install openjdk-17-jdk-headless`.

```bash
mkdir -p ~/codjia-cle && cd ~/codjia-cle
keytool -genkeypair -v \
  -keystore codjia-release.jks -storetype PKCS12 \
  -alias codjia -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Mensan Moreno CODJIA, O=CODJIA, L=Ouagadougou, C=BF"
```

`keytool` demande un mot de passe. Choisis-en un long, d'au moins 20 caractères, et range-le dans ton gestionnaire de mots de passe. Au format PKCS12, ce mot de passe protège à la fois le fichier et la clé.

**Sauvegarde immédiatement** `codjia-release.jks` et son mot de passe à deux endroits distincts hors ligne, par exemple une clé USB chiffrée et ton gestionnaire de mots de passe. La clé reste valable 27 ans.

Plus tard, cette même clé servira de **clé d'importation** pour Google Play (Play App Signing).

## 2. Encoder la clé pour GitHub

Linux :
```bash
base64 -w0 codjia-release.jks > codjia-release.b64
```
Windows (PowerShell) :
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("codjia-release.jks")) | Set-Content -NoNewline codjia-release.b64
```

## 3. Déclarer les 4 secrets dans GitHub

Dépôt › **Settings › Secrets and variables › Actions › New repository secret** :

| Nom | Valeur |
| --- | --- |
| `CODJIA_KEYSTORE_BASE64` | tout le contenu de `codjia-release.b64` |
| `CODJIA_KEYSTORE_PASSWORD` | le mot de passe choisi |
| `CODJIA_KEY_ALIAS` | `codjia` |
| `CODJIA_KEY_PASSWORD` | le même mot de passe |

Supprime ensuite `codjia-release.b64` (`shred -u codjia-release.b64` sous Linux). Le `.jks` original reste dans ta sauvegarde.

GitHub chiffre les secrets et les masque dans les journaux. Seul le workflow « Publier une version », lancé depuis `main`, s'en sert.

**Protection supplémentaire (recommandée).** Dans **Settings › Environments › publication**, ajoute-toi comme *Required reviewer*. Chaque publication attendra alors ton approbation, même si ton compte GitHub était compromis par un jeton volé.

## 4. Publier une version

1. Vérifie que `versionName` dans `demo-app/build.gradle.kts` vaut la version voulue (ex. `1.0.0`). Augmente `versionCode` à chaque nouvelle version.
2. Vérifie que `docs/release-notes/v1.0.0.md` existe.
3. Onglet **Actions › Publier une version › Run workflow**, puis saisis `1.0.0`.

Le workflow refuse de publier dans les cas suivants :
- il ne part pas de `main` ;
- la version ne correspond pas à `versionName`, ou existe déjà ;
- les secrets sont absents ;
- un test échoue ;
- l'APK n'est pas signé (schéma v2) ;
- l'identifiant n'est pas `io.github.mensan2024.codjia` ;
- une permission réseau ou de stockage apparaît.

Il publie ensuite :
- `CODJIA-1.0.0.apk`, signé et optimisé par R8 ;
- `SHA256SUMS.txt` ;
- l'empreinte du certificat ;
- une **attestation de provenance** (Sigstore), qui prouve que l'APK a été produit par ce workflow, à partir de ce commit.

## 5. Vérifier un APK téléchargé (pour toi et tes utilisateurs)

```bash
sha256sum -c SHA256SUMS.txt
gh attestation verify CODJIA-1.0.0.apk -R Mensan2024/itmentor-video-prototype
apksigner verify --print-certs CODJIA-1.0.0.apk   # l'empreinte doit être celle des notes de version
```

## 6. Installer sur Android

1. Télécharge `CODJIA-x.y.z.apk` depuis la page *Releases*, sur le téléphone.
2. Autorise l'installation depuis le navigateur quand Android le demande (*Sources inconnues*).
3. La version publique (**CODJIA**, `io.github.mensan2024.codjia`) s'installe **à côté** des versions de test (**CODJIA test**, `…codjia.dev`), et de l'ancien prototype, que tu peux désinstaller.

Pour des mises à jour automatiques depuis GitHub, on peut utiliser [Obtainium](https://github.com/ImranR98/Obtainium) en lui donnant l'adresse du dépôt.
