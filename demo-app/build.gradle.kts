plugins { alias(libs.plugins.android.application) }
android {
    namespace = "io.github.mensan2024.itmentor.prototype"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.mensan2024.itmentor.prototype"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1-prototype"
    }
    buildTypes {
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
