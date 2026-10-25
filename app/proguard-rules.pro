# ==============================================================================
# VibraPay Official Cloud E-Wallet — Aggressive R8 / ProGuard Obfuscation Rules
# ==============================================================================

# 1. Aggressive Code Obfuscation & Package Flattening
-repackageclasses 'o.vp'
-allowaccessmodification
-overloadaggressively
-renamesourcefileattribute SourceFile

# Remove debug/verbose logging calls in production release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 2. Preserve Firebase Firestore Typed Document Models (Required for .toObject() reflection)
-keep class com.example.cloud.UserPrivateProfileDoc { *; }
-keep class com.example.cloud.CloudAccountDoc { *; }
-keep class com.example.cloud.PhoneDirectoryDoc { *; }
-keep class com.example.cloud.CloudTransferDoc { *; }
-keepnames class * implements java.io.Serializable

# 3. Preserve Room Database Entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 4. Preserve SQLCipher Native JNI Bindings
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# 5. Preserve Android Keystore & Security Crypto Classes
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**
