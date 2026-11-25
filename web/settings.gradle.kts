// Version navigateur de CODJIA : build Gradle séparé (l'application Android n'en dépend pas).
// Le moteur (engine-core) est compilé tel quel en JavaScript : mêmes formats, styles et règles.
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
rootProject.name = "codjia-web"
