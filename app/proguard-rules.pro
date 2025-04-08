# Keep model classes
-keep class com.eazydelivery.app.domain.model.** { *; }
-keep class com.eazydelivery.app.data.model.** { *; }

# Keep Retrofit service interfaces
-keep,allowobfuscation interface com.eazydelivery.app.data.remote.ApiService

# Retrofit rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore JSR 305 annotations for embedding nullability information
-dontwarn javax.annotation.**

# With R8 full mode generic signatures are stripped for classes that are not
# kept. Suspend functions are wrapped in continuations where the type argument
# is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# R8 full mode strips generic signatures from return types if not kept
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>

# Kotlinx Serialization rules
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.eazydelivery.app.**$serializer { *; }
-keepclassmembers class com.eazydelivery.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.eazydelivery.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Razorpay rules
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}

# Firebase rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt rules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel

# Keep accessibility service and notification listener service
-keep class com.eazydelivery.app.service.DeliveryAccessibilityService { *; }
-keep class com.eazydelivery.app.service.DeliveryNotificationListenerService { *; }

# Keep boot receiver
-keep class com.eazydelivery.app.receiver.BootReceiver { *; }

# Keep WorkManager classes
-keep class androidx.work.** { *; }

# Keep Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# Keep Biometric classes
-keep class androidx.biometric.** { *; }

# TensorFlow Lite
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# ML models
-keep class com.eazydelivery.app.ml.** { *; }

# Keep admin credentials
-keepclassmembers class com.eazydelivery.app.util.Constants {
    public static final java.lang.String ADMIN_PHONE;
    public static final java.lang.String ADMIN_EMAIL;
}
