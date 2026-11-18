# Keep All Application Code (Domain, UI, Data, DI, Workers)
-keep class ru.dev.gitnews.** { *; }
-dontwarn ru.dev.gitnews.**

# Keep Room Database, DAOs & Entities
-keep class * extends androidx.room.RoomDatabase {
    <init>();
    *;
}
-keep class ru.dev.gitnews.data.local.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep WorkManager Workers
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class ru.dev.gitnews.worker.** { *; }

# Keep AndroidX Crypto & KeyStore
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep Retrofit & OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Google ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Yandex Mobile Ads & AppMetrica
-keep class com.yandex.mobile.ads.** { *; }
-dontwarn com.yandex.mobile.ads.**
-keep class com.monetization.ads.** { *; }
-dontwarn com.monetization.ads.**
-keep class io.appmetrica.** { *; }
-dontwarn io.appmetrica.**
-dontwarn com.yandex.metrica.*
-dontwarn io.appmetrica.analytics.*
-keep class * extends com.monetization.ads.mediation.appopenad.MediatedAppOpenAdAdapter { *; }
-keep class * extends com.monetization.ads.mediation.banner.MediatedBannerAdapter { *; }
-keep class * extends com.monetization.ads.mediation.interstitial.MediatedInterstitialAdapter { *; }
-keep class * extends com.monetization.ads.mediation.rewarded.MediatedRewardedAdapter { *; }
-keep class * extends com.monetization.ads.mediation.nativeads.MediatedNativeAdapter { *; }

# Keep Attributes
-keepattributes EnclosingMethod,InnerClasses,Signature,SourceFile,LineNumberTable
