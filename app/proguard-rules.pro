# R8 runs with the optimized Android defaults in release builds. Keep only
# metadata accessed reflectively by Retrofit and Kotlin-based serializers.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault

# Retrofit reads HTTP annotations on service interfaces at runtime. This rule
# retains the metadata while allowing R8 to optimize and obfuscate the code.
-keep,allowoptimization,allowobfuscation,allowshrinking class retrofit2.** { *; }
