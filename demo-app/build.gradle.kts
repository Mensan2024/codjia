plugins { alias(libs.plugins.android.application) }
android {
    // Identifiant technique conservé : la nouvelle version s'installe par-dessus l'ancienne.
    namespace = "io.github.mensan2024.itmentor.prototype"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.mensan2024.itmentor.prototype"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3-codjia"
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
    }
    buildTypes {
        getByName("debug") { signingConfig = signingConfigs.getByName("debug") }
        release { isMinifyEnabled = false }
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
