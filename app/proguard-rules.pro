# NEXUS ProGuard Rules

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Room entities
-keep class com.nexus.intelligence.data.local.entity.** { *; }

# Keep Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**

# Keep PdfBox
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# Keep Tesseract
-keep class cz.adaptech.tesseract4android.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep data classes for serialization
-keep class com.nexus.intelligence.domain.model.** { *; }

# General Android
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
