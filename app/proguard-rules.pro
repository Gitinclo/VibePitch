# ProGuard / R8 Configuration for VibePitch Studio (com.gitinclo.vibepitch)
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep Firebase Firestore POJO / Data Models & @DocumentId / @ServerTimestamp fields
-keep class com.gitinclo.vibepitch.data.model.** { *; }
-keepnames class com.gitinclo.vibepitch.data.model.** { *; }
-keepclassmembers class com.gitinclo.vibepitch.data.model.** {
    public <init>();
    public <fields>;
    public <methods>;
}

# Keep AuthGuardState & domain models
-keep class com.gitinclo.vibepitch.auth.** { *; }
-keep class com.gitinclo.vibepitch.domain.** { *; }

# Keep Firebase & Google Play Services Credential Manager classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Kotlin Coroutines & Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
