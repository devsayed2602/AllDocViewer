# Add project specific ProGuard rules here.
-keep class org.apache.poi.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn org.apache.**
-dontwarn com.tom_roush.**
-dontwarn javax.xml.**
