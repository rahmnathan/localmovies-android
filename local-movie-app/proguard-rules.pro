# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Nathan\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
# You can edit the include currentPath and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# OpenAPI client generated code uses Jakarta/Javax annotations that are not present at runtime
# These are compile-time only annotations and can be safely ignored
-dontwarn jakarta.annotation.**
-dontwarn javax.annotation.**

# Jakarta validation annotations (used by OpenAPI client)
-dontwarn jakarta.validation.**
-dontwarn javax.validation.**
