# Keep osmdroid classes
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Keep Room entities
-keep class com.mockgps.data.** { *; }

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }

# Keep OkHttp
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep navigation
-keep class androidx.navigation.** { *; }

# Keep permissions
-keep class com.google.accompanist.permissions.** { *; }

# Keep Shizuku
-keep class moe.shizuku.** { *; }
-dontwarn moe.shizuku.**