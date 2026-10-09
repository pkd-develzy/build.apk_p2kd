# Proguard rules for P2KD Coklit Native App
-keep class id.p2kd.kalisalak.coklit.data.models.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
