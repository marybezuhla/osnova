# Readable crash traces, without the original file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# The voice model's runtime creates its Java objects from native code: its classes keep their names
-keep class ai.onnxruntime.** { *; }
