# Vosk & JNA - critical!
-keep class org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-keep class com.sun.jna.platform.** { *; }
-dontwarn com.sun.jna.**
-dontwarn org.vosk.**
-dontwarn java.awt.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# App
-keep class com.antimoshennik.app.** { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.Metadata { *; }

# Android
-dontwarn android.**
-keep class android.** { *; }
