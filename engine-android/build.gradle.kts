// Adaptation Android : rendu sur android.graphics.Canvas et export Media3 Transformer.
plugins { alias(libs.plugins.android.library) }
android {
    namespace = "io.github.mensan2024.itmentor.engine.android"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    api(project(":engine-core"))
    implementation(libs.media3.transformer)
    implementation(libs.media3.effect)
    implementation(libs.media3.common)
    implementation(libs.coroutines.android)
    implementation(libs.annotation)
}
