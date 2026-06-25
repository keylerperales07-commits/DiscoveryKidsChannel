# proguard-debug.pro
# Reglas adicionales para el build type debug con R8 habilitado.
# Mantiene los llamados a android.util.Log para que los logs sean
# visibles en Logcat durante el desarrollo de la Beta.

# Conserva todos los métodos de android.util.Log (d, i, w, e, v, wtf).
-keep class android.util.Log { *; }

# Evita que R8 elimine las llamadas a Log.d, Log.i, Log.w, Log.e.
-keepclassmembers class * {
    void log(...);
}

# Conserva nombres de clases en stack traces para que sean legibles en Logcat.
-keepattributes SourceFile,LineNumberTable

# No ofuscar los nombres de Activity/Fragment propios del proyecto
# para que los logs y stack traces sean legibles sin mapping.
-keep class com.keyler.discoverykidschannel.** { *; }
