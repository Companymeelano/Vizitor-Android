// ===========================================================================
//  ویزیتور — نسخهٔ اندروید با اتصال مستقیم به SQL Server (پورت ۱۴۳۳)
//  خروجی APK: vizitor-direct-debug.apk  /  vizitor-direct-release.apk
// ===========================================================================
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.atiran.vizitor.direct"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.atiran.vizitor.direct"
        minSdk = 24                       // Android 7.0 و بالاتر (درایور JDBC نیاز به API 24 دارد)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // امضای نسخهٔ نهایی: فایل keystore.properties کنار همین ماژول (در گیت نیست)
    val keystoreProps = rootProject.file("keystore.properties")
    if (keystoreProps.exists()) {
        val props = Properties().apply { keystoreProps.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                // فایل کلید ممکن است کنار ریشهٔ پروژه باشد یا کنار همین ماژول
                val keyPath = props.getProperty("storeFile")
                storeFile = rootProject.file(keyPath).takeIf { it.exists() } ?: file(keyPath)
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false        // درایور JDBC و R8: با minify هوک‌های رفلکشن لازم است؛
            isShrinkResources = false      // برای اطمینان، نسخهٔ ریلیز بدون کوچک‌سازی ساخته می‌شود.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreProps.exists()) signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    tasks.withType<Test>().configureEach {
        // نتیجهٔ تک‌تک آزمون‌ها در لاگ CI چاپ شود (شواهد قابل‌ثبت در مخزن)
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }
    testOptions {
        // آزمون‌های JVM به APIهای اندروید دست نمی‌زنند؛ این گزینه فقط جلوی
        // «Method not mocked» را در صورت لمس تصادفی می‌گیرد.
        unitTests.isReturnDefaultValues = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            // درایورهای JDBC فایل‌های META-INF مشترک دارند
            excludes += setOf(
                "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
                "META-INF/versions/**",
            )
            pickFirsts += setOf("META-INF/services/java.sql.Driver")
        }
    }
}

dependencies {
    // ── رابط کاربری (Compose) ────────────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ── اتصال مستقیم به SQL Server ──────────────────────────────────────────
    // ۱) درایور رسمی مایکروسافت (jre8 ⇒ سازگار با اندروید؛ از DriverManager استفاده می‌کنیم)
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.4.1.jre8")
    // ۲) درایور پشتیبان jTDS (اگر درایور رسمی روی گوشی بار نشد، خودکار استفاده می‌شود)
    implementation("net.sourceforge.jtds:jtds:1.3.1")

    // ── اسکن کارت اتصال (همان QR نصب‌کننده) ─────────────────────────────────
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── آزمون‌ها (روی JVM): آزمون اتصال واقعی به SQL Server در CI ───────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
