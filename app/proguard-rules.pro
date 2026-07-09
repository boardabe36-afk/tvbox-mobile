# Default ProGuard rules for TVBox Simple

# Keep model classes used in JSON (Gson)
-keep class com.simple.tvbox.model.** { *; }

# Gson - keep generic type signatures so TypeToken works after R8/shrink
# Source: https://stackoverflow.com/questions/76224936/google-gson-preserve-generic-signatures
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes AnnotationDefault
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-dontwarn sun.misc.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep WatchHistoryRepository and SourceRepository (Gson reflection on these too)
-keep class com.simple.tvbox.data.** { *; }

# OkHttp + Okio recommended
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase


# Keep Compose + Lifecycle (LocalLifecycleOwner crash fix)
-keep class androidx.lifecycle.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.navigation.compose.** { *; }
-keep class androidx.activity.compose.** { *; }

# Keep SpiderSite + VideoItem + VideoClient (reflection via factory)
-keep class com.simple.tvbox.source.** { *; }
-keep class com.simple.tvbox.model.** { *; }
-keep class com.simple.tvbox.util.** { *; }

# Keep DoubanService + DoubanItem (Gson reflection)
-keep class com.simple.tvbox.source.DoubanService** { *; }
