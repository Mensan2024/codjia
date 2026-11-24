# CODJIA — règles R8 de la version de publication.
# Le moteur n'utilise ni réflexion ni sérialisation : rien de spécifique à conserver.
# Media3 et AndroidX fournissent leurs propres règles (consumer rules).

# Retire les journaux de débogage de la version publiée.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
