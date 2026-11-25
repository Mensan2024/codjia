plugins { alias(libs.plugins.android.application) }

// Clé de publication : fournie uniquement par l'environnement (secrets GitHub ou poste du mainteneur).
// Elle n'est jamais dans le dépôt. Sans elle, la version de publication est produite non signée.
val releaseKeystore: File? = System.getenv("CODJIA_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.isFile }

android {
    // Espace de noms du code Kotlin (interne, sans effet sur l'identifiant public).
    namespace = "io.github.mensan2024.itmentor.prototype"
    compileSdk = 36
    defaultConfig {
        // Identifiant public DÉFINITIF (Google Play, F-Droid) : ne plus jamais le modifier.
        applicationId = "io.github.mensan2024.codjia"
        minSdk = 26
        targetSdk = 36
        versionCode = 151
        versionName = "1.5.1"
    }
    signingConfigs {
        // Clé de développement FIXE, versionnée (ce n'est pas une clé de publication) :
        // chaque APK produit par GitHub Actions s'installe par-dessus le précédent.
        getByName("debug") {
            storeFile = file("codjia-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) create("release") {
            storeFile = releaseKeystore
            storePassword = System.getenv("CODJIA_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("CODJIA_KEY_ALIAS")
            keyPassword = System.getenv("CODJIA_KEY_PASSWORD")
            enableV1Signing = false // inutile avec minSdk 26 (Android 8+)
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        // Version de test : identifiant « .dev » et nom « CODJIA test », installable à côté de la version publiée.
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        // Version de publication : code réduit et obscurci par R8, ressources inutiles retirées.
        // Elle n'est pas signée ici : la clé de publication ne doit jamais être dans le dépôt.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
        }
    }
    // Pas de bloc de métadonnées de dépendances chiffré par Google dans l'APK (exigence F-Droid, build reproductible).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":engine-android"))
    implementation(libs.media3.common)
    implementation(libs.activity)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines.android)
}
