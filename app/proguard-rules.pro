# R8 runs with the optimized Android defaults in release builds. Keep only
# metadata accessed reflectively by Retrofit and Kotlin-based serializers.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,AnnotationDefault

