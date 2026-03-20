# Règles ProGuard pour KeyBinderTv

# Conserver les classes de données sérialisées (kotlinx.serialization)
-keep class dev.cavatz.keybindertv.data.model.** { *; }

# kotlinx.serialization — évite l'obfuscation des sérialiseurs générés
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Conserver les classes Compose (nécessaire pour certains builds release)
-keep class androidx.compose.** { *; }
