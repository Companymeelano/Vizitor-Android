/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | اپ‌ماژول (app/build.gradle.kts)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  Jetpack Compose + Room + Retrofit + WorkManager + CameraX + ML Kit
 * ═══════════════════════════════════════════════════════════════════════════
 */
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ir.atiran.vizitor"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.atiran.vizitor"
        minSdk = 24
        targetSdk = 35
        // نسخهٔ «اتصال مستقیم» روی سورس ۲٫۱۳٫۵ ساخته شده؛ شمارهٔ ساخت بالاتر از
        // APK آپلودی (۲۱۷۰۱) گرفته شده تا نصب/به‌روزرسانی روی گوشی‌ها بدون خطای
        // «نسخهٔ قدیمی‌تر» انجام شود.
        //  ۲٫۱۳٫۶ : بازطراحی صفحهٔ اول، صفحهٔ اتصال، آیکن سه‌بعدی و فونت وزیرمتن
        //  ۲٫۱۳٫۷ : لاکچری‌سازی نهایی — اندازه‌های واکنشی برای همهٔ گوشی‌ها،
        //           حاشیهٔ امن نوار سیستم/بریدگی، کلیدهای سه‌بعدی با عمق واقعی،
        //           گام‌بندی ۱/۲/۳ تنظیمات ورود و آیکن جواهری تازه (M + پسته)
        //  ۲٫۱۴٫۰ : بخش «ثبت ویزیت» — ثبت مراجعه در جدول واقعی dbo.Visit
        //           (مشتری مجاز + مدت + توضیح + GPS + تاریخ شمسی)، تب تازه
        //           در نوار پایین، کارت دسترسی سریع در پیشخوان، و رفع
        //           خطاهای کلمپایل «لاکچری نسل ۲» (Luxury2/Premium)
        //  ۲٫۱۷٫۰ : بازطراحی کامل گرافیکی به سبک «گزارش طلایی» (مشکیِ عمیق + طلای فلزی):
        //           پالت تازه (گزارش طلایی/شامپاین روشن)، کارت‌های فلزی، کاشی‌های شاخص،
        //           نمودار ستونی استوانه‌ای با بازتاب، نمودار روند دوسری، حلقهٔ سه‌بعدی
        //           درخشان، جدول «نبض کسب‌وکار»، کاشی‌های ابزار و فوتر برند MEELANO
        //  ۲٫۲۴٫۰ : «سلامت سرور مورد اتصال» — آزمایشگاه پنج‌دستهٔ سلامت (شبکه و مسیر،
        //           اتصال و احراز هویت، دیتابیس و گزارش‌ها، دسترسی و امنیت) با نمرهٔ
        //           ۰ تا ۱۰۰، گیج شعاعی سه‌بعدی و چک‌لیست زنده؛ به‌علاوهٔ دسته‌بندی
        //           تازهٔ اتاق فرمان (اتصال و سلامت / داده و جداول / گزارش و تحلیل)
        //           و نسل تازهٔ دکمه‌های سه‌بعدی با عمق، هایلایت و فشار فیزیکی
        versionCode = 21820
        versionName = "2.24.0-direct"
        vectorDrawables { useSupportLibrary = true }
    }

    // ───────────────────────────────────────────────────────────────────────
    //  دو طعم ساخت از یک سورس (v2.20.0):
    //    • vizitor  → برنامهٔ کامل «آتیران ویزیتور»  (ir.atiran.vizitor)
    //    • mreport  → نسخهٔ انحصاری «گزارشات مدیر»  (ir.atiran.mreport)
    //      فقط اتاق فرمان گزارش‌ها: اتصال دقیق چهارحالته + جدول‌ها + گزارش‌ها.
    //      بستهٔ جداست، پس هر دو برنامه کنار هم روی گوشی نصب می‌شوند.
    // ───────────────────────────────────────────────────────────────────────
    flavorDimensions += "edition"
    productFlavors {
        create("vizitor") {
            dimension = "edition"
            applicationId = "ir.atiran.vizitor"
            buildConfigField("boolean", "MR_EDITION", "false")
        }
        create("mreport") {
            dimension = "edition"
            applicationId = "ir.atiran.mreport"
            versionName = "2.23.0-report"
            buildConfigField("boolean", "MR_EDITION", "true")
        }
    }

    // امضای نسخهٔ نهایی: فایل keystore.properties کنار ریشهٔ پروژه (در گیت نیست)
    val keystoreProps = rootProject.file("keystore.properties")
    if (keystoreProps.exists()) {
        val props = Properties().apply { keystoreProps.inputStream().use { load(it) } }
        signingConfigs {
            create("release") {
                val keyPath = props.getProperty("storeFile")
                storeFile = rootProject.file(keyPath).takeIf { it.exists() } ?: file(keyPath)
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // درایور JDBC (mssql-jdbc) با رفلکشن بار می‌شود؛ برای اطمینان، نسخهٔ
            // ریلیز بدون کوچک‌سازی ساخته می‌شود (همان تصمیم آزموده‌شدهٔ Vizitor Direct).
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.exists()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // درایور رسمی مایکروسافت (mssql-jdbc) از java.time استفاده می‌کند که در
        // اندروید ۷/۸ (API 24/25) وجود ندارد. desugaring آن را برای همهٔ نسخه‌ها می‌آورد
        // تا روی گوشی قدیمی هم اتصال مستقیم بدون NoClassDefFoundError کار کند.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        // درایورهای JDBC مایکروسافت و jTDS فایل‌های META-INF مشترک دارند
        resources.excludes += setOf(
            "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/DEPENDENCIES",
            "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
            "META-INF/versions/**",
        )
        resources.pickFirsts += setOf("META-INF/services/java.sql.Driver")
    }
}

dependencies {
    // ── Compose BOM ────────────────────────────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Core / Navigation / Lifecycle ─────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // ── Room (Offline-First Database) ─────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── WorkManager (Background Sync Service) ─────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ── Retrofit / OkHttp / Gson (Network Layer) ──────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // ── Coroutines ────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ── DataStore (Server Config / Settings) ──────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── اتصال مستقیم به SQL Server روی پورت ۱۴۳۳ (بدون IIS و بدون API میانی) ──
    //    دو درایور بسته‌بندی می‌شود تا روی هر گوشی‌ای یکی‌شان کار کند
    //    (همان کاری که Vizitor Direct انجام می‌دهد و در CI روی SQL Server واقعی آزموده شد)
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.4.1.jre8")
    implementation("net.sourceforge.jtds:jtds:1.3.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // ── CameraX + ML Kit (Barcode Scanner) ────────────────────────────────
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}
