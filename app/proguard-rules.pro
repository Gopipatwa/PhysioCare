# ML Kit pose detection uses JNI; keep its classes intact.
-keep class com.google.mlkit.vision.pose.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_vision_**
-dontwarn javax.annotation.**
