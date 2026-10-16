# Add project specific ProGuard rules here.
# 保留影图主包（debug 包不混淆，方便调试）
-keep class com.yingtu.plus.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn androidx.media3.**
