// Banc d'essai de développement (jamais livré dans l'application) : rendu Java2D + ffmpeg du poste.
plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}
kotlin { jvmToolchain(17) }
application { mainClass.set("io.github.mensan2024.itmentor.engine.jvm.BenchKt") }
dependencies {
    implementation(project(":engine-core"))
    testImplementation(libs.junit)
}
val fontsDir = rootProject.file("engine-android/src/main/assets/fonts").absolutePath
tasks.test { systemProperty("itm.fonts", fontsDir); systemProperty("java.awt.headless", "true") }
tasks.named<JavaExec>("run") { systemProperty("itm.fonts", fontsDir); workingDir = rootProject.projectDir }
