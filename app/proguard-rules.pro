# ProGuard / R8 Configuration for VibePitch Studio (com.aistudio.vibepitch.kxpqmr)
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep Firebase Firestore POJO / Data Models & @DocumentId / @ServerTimestamp fields
-keep class com.example.data.model.** { *; }
-keepnames class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.model.** {
    public <init>();
    public <fields>;
    public <methods>;
}

# Keep Firebase & Google Play Services Credential Manager classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Kotlin Coroutines & Serialization
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
