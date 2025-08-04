-dontobfuscate

# Keep line number info for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin generics, annotations, and metadata
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class ** { @kotlin.Metadata *; }

# Gson support for generics
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.browntowndev.liftlab.core.domain.enums.MovementPattern { *; }

# Ktor plugin reflection support
-keep class io.ktor.client.plugins.** { *; }
-keep class io.ktor.client.plugins.contentnegotiation.ContentNegotiation$* { *; }
-keep class io.ktor.client.plugins.HttpTimeout$* { *; }

# Suppress warnings for ktor/kotlinx
-dontwarn io.ktor.**
-dontwarn kotlinx.serialization.**
