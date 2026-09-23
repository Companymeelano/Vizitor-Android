# Vizitor Direct SQL — Final Verification 2.14.2

## Fixed
- Kotlin compile error: Regex transform returned `Char`; now returns `String`.
- Removed obsolete Retrofit ERP transport and `data/remote` package.
- Removed `android:usesCleartextTraffic="true"` from the manifest.
- Preserved OkHttp/Gson because the optional AI assistant uses OkHttp directly.
- Direct SQL remains JDBC/TDS on TCP 1433 with SQL authentication.
- Database discovery remains through `master` / `sys.databases`.
- Visitor authentication remains separate from SQL Server authentication.
- Connection manager drains the pool when server/database/user changes.
- Pre-invoice path remains Direct SQL and final invoice issuance remains blocked.

## Build limitation in this environment
A complete Gradle build could not be executed here because `services.gradle.org` is unreachable from the build environment and no Gradle 8.9 distribution is cached locally. This is an environment/network limitation, not a reported source error.

## Local build
Run `vizitor-app\\BUILD-APK.bat` on a Windows machine with JDK 17 and internet access. Android Studio is not required.
