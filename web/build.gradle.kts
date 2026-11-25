plugins {
    kotlin("multiplatform") version "2.2.21"
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig { outputFileName = "codjia.js" }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            // Le moteur vidéo partagé avec Android et les exemples de textes.
            kotlin.srcDir("../engine-core/src/main/kotlin")
            kotlin.srcDir(layout.buildDirectory.dir("generated/examples"))
        }
    }
}

val copyExamples by tasks.registering(Copy::class) {
    from("../demo-app/src/main/kotlin/io/github/mensan2024/itmentor/prototype/Examples.kt")
    into(layout.buildDirectory.dir("generated/examples"))
}
tasks.matching { it.name == "compileKotlinJs" }.configureEach { dependsOn(copyExamples) }

// Polices et musiques : les mêmes fichiers que l'application Android (aucune copie dans le dépôt).
tasks.named<Copy>("jsProcessResources") {
    from("../engine-android/src/main/assets") { into("assets") }
}
