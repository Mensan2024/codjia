// Moteur pur Kotlin : aucune dépendance Android, testable sur n'importe quelle JVM.
plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(17) }
dependencies { testImplementation(libs.junit) }
