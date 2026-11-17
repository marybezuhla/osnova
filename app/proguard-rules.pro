# Readable crash traces, without the original file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# The voice model's runtime creates its Java objects from native code: its classes keep their names
-keep class ai.onnxruntime.** { *; }

# No log lines in the release build, ever (none are written today; this keeps it so)
-assumenosideeffects class android.util.Log { *; }
