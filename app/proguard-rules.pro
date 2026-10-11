# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep line numbers so Play Console / crash reports show readable stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Moshi: JSON models use generated adapters (@JsonClass(generateAdapter = true)), whose
# consumer rules ship with Moshi. GeminiClient also registers KotlinJsonAdapterFactory,
# which reads Kotlin metadata via reflection.
-keep class kotlin.Metadata { *; }

# Retrofit service interfaces are created through reflection; keep their generic signatures
# so suspend functions and response types survive shrinking.
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keep,allowobfuscation,allowshrinking interface com.gymbro.fitness.data.GeminiApiService
-keep,allowobfuscation,allowshrinking interface com.gymbro.fitness.data.OpenFoodFactsApi

# OkHttp can use these optional TLS providers when present; the app doesn't ship them.
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
