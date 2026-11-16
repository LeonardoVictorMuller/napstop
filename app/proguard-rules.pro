# NapStop ProGuard Rules

# Keep application entry points
-keep class com.leonardovictormuller.napstop.MainActivity { *; }
-keep class com.leonardovictormuller.napstop.NapStopApplication { *; }

# Keep domain models (needed for reflection-safe serialization)
-keep class com.leonardovictormuller.napstop.domain.model.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }

# Suppress warnings for optional deps
-dontwarn com.google.android.gms.**

# Remove all logging in release (privacy: no coordinates ever printed)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
