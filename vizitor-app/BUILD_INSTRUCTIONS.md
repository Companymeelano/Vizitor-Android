# Developed by Milano Technical Team, Milad Yaghoobi

# دستورالعمل کامل بیلد و کامپایل نسخه نهایی APK — آتیران ویزیتور

## پیش‌نیازها
| ابزار | نسخه |
|---|---|
| JDK | **17** (`java -version`) |
| Android SDK | Platform 35 + Build-Tools 35 (نصب خودکار توسط Android Studio) |
| Gradle | Wrapper داخلی پروژه (نسخه 8.9) — نیازی به نصب جداگانه نیست |

---

## روش اول: ترمینال اندروید استودیو یا ترمینال سیستم

```bash
# ۱) ورود به پوشه پروژه
cd Vizitor

# ۲) پاکسازی کامل پروژه از بیلدهای قبلی
./gradlew clean

# ۳) کامپایل و بیلد نسخه خروجی
./gradlew assembleDebug        # نسخه دیباگ (بدون نیاز به امضا)
# یا
./gradlew assembleRelease      # نسخه ریلیز (نیازمند تنظیم امضا در ادامه)
# یا برای انتشار در کافه‌بازار/گوگل‌پلی:
./gradlew bundleRelease        # خروجی AAB
```

> در ویندوز به جای `./gradlew` از `gradlew.bat` استفاده کنید.

### مسیر دقیق فایل‌های خروجی
| خروجی | مسیر فایل |
|---|---|
| **APK دیباگ** | `app/build/outputs/apk/debug/app-debug.apk` |
| APK ریلیز | `app/build/outputs/apk/release/app-release.apk` |
| باندل ریلیز (AAB) | `app/build/outputs/bundle/release/app-release.aab` |

---

## روش دوم: محیط Termux (بدون روت)

```bash
pkg update -y && pkg install -y openjdk-17 git
git clone <REPO_URL> && cd Vizitor

# تعریف JDK و پوشه SDK
export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk
export ANDROID_HOME=$HOME/android-sdk
mkdir -p $ANDROID_HOME

# پذیرش لایسنس‌ها و دانلود حداقل‌های SDK
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
    "platforms;android-35" "build-tools;35.0.0" "platform-tools"

# بیلد
./gradlew clean
./gradlew assembleDebug

# فایل نهایی:
ls -la app/build/outputs/apk/debug/app-debug.apk
```

> نکته: برای تجربه پایدارتر در Termux از `proot-distro` با اوبونتو استفاده کنید.

---

## امضای نسخه ریلیز (برای توزیع)

۱) ساخت کی‌استور:
```bash
keytool -genkeypair -v -keystore vizitor-release.jks \
  -alias vizitor -keyalg RSA -keysize 2048 -validity 10000
```

۲) افزودن به `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../vizitor-release.jks")
            storePassword = System.getenv("VIZITOR_KEYSTORE_PASS")
            keyAlias = "vizitor"
            keyPassword = System.getenv("VIZITOR_KEY_PASS")
        }
    }
    buildTypes {
        release { signingConfig = signingConfigs.getByName("release") }
    }
}
```

۳) بیلد نهایی:
```bash
./gradlew clean
./gradlew bundleRelease
```

---

## رفع مشکلات رایج
| مشکل | راه‌حل |
|---|---|
| `SDK location not found` | فایل `local.properties` با محتوای `sdk.dir=/path/to/android-sdk` بسازید |
| خطای حافظه هنگام بیلد | `org.gradle.jvmargs` را در `gradle.properties` به `-Xmx6g` افزایش دهید |
| خطای دانلود وابستگی‌ها | اتصال به `google()`/`mavenCentral()` یا استفاده از پروکسی/میرور |
| نصب نشدن روی دستگاه | نسخه دیباگ دارای `applicationIdSuffix = .debug` است؛ نسخه قبلی هم‌نام را حذف کنید |
