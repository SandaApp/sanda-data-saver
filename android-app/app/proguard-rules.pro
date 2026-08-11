# Sanda Data Saver - ProGuard Rules
# Keep all project classes - app uses reflection for VPN and services
-keep class com.sanda.datasaver.** { *; }

# Keep AndroidX and Material
-keep class androidx.** { *; }
-keep class com.google.android.material.** { *; }

# Keep VPN service
-keep class android.net.VpnService { *; }

# Keep preferences
-keep class androidx.preference.** { *; }

# Keep widget and receivers
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.app.Service { *; }

# Remove logs for release (optional)
# -assumenosideeffects class android.util.Log {
#    public static *** d(...);
#    public static *** v(...);
# }

# Dont warn
-dontwarn com.sanda.datasaver.**
-dontwarn androidx.**
-dontwarn org.conscrypt.**
-dontnote **

# Keep view binding
-keep class * implements androidx.viewbinding.ViewBinding { *; }
