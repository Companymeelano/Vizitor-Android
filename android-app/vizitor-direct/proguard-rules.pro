# درایورهای JDBC با رفلکشن بار می‌شوند — دست‌نخورده بمانند
-keep class com.microsoft.sqlserver.jdbc.** { *; }
-keep class net.sourceforge.jtds.** { *; }
-dontwarn com.microsoft.sqlserver.jdbc.**
-dontwarn net.sourceforge.jtds.**
-dontwarn javax.naming.**
-dontwarn javax.transaction.**
-dontwarn java.awt.**
-dontwarn javax.security.auth.callback.**
# zxing
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
