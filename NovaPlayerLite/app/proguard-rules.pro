# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /usr/local/google/home/juliemartin/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project-specific keep rules here:

# Keep Compose-related classes
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }
