# ویزیتور مستقیم — نسخهٔ اندروید با اتصال مستقیم به SQL Server

این یک پروژهٔ **کامل و آمادهٔ ساخت** اندروید است (Android Studio / Gradle) که همان کاری را می‌کند
که نصب‌کنندهٔ ویندوز `release\Vizitor-Setup-1.0.0.exe` سرور را برایش آماده می‌کند:

* اتصال **مستقیم** به SQL Server روی **پورت ۱۴۳۳** — بدون IIS، بدون API میانی، بدون وب‌سرور.
* خواندن **جدول‌های واقعی** سامانهٔ حسابداری (همان شناسه‌های تأییدشده: `dbo.sys_users`,
  `dbo.sys_cus`, `dbo.CUSTOMERS`, `dbo.custgroup`, `dbo.sys_vis`, `dbo.visitors`, کالا و موجودی،
  و آمادگی مسیر پیش‌فاکتور `dbo.add_sail_pish`).
* همان **طرح بصری** نصب‌کننده و پنل مدیریت: سرمه‌ای یکدست، فونت وزیرمتن، فارسی و راست‌به‌چپ،
  با امضای «میلاد یقوبی — Meelano Studio Design».
* همان **کارت اتصال** نصب‌کننده را می‌خواند: کد QR (`vizitor://c?h=…&p=1433&d=…`)، فایل
  `android-connect.json`، یا متن کارت.

---

## ۰) فایل APK آمادهٔ نصب (دانلود مستقیم)
APK ساخته شد و در صفحهٔ ریلیز همین مخزن قابل دانلود است:

```
https://github.com/Companymeelano/Vizitor-Android/releases/tag/vizitor-direct-v1.0.0
```

> 🆕 **آخرین بیلد — ۲۰۲۶-۰۹-۲۲** (کامیت `6aae9e0`): با **کلید امضای پایدار پروژه**
> (`android-app/vizitor-ci.jks`، همان کلید برای بیلدهای بعدی) امضا شده و خودِ فایل هم داخل
> مخزن هست: [`dist/VizitorDirect-1.0.0.apk`](dist/VizitorDirect-1.0.0.apk).
>
> | فایل | حجم | sha256 |
> |---|---|---|
> | `VizitorDirect-1.0.0.apk` | ۷٬۶۲۳٬۴۱۵ بایت | `8a17ea650ab1883de359db18fc7ea04681623d1f87f62468ea5e3a9cdcf1051d` |
> | `VizitorDirect-1.0.0-debug.apk` | ۱۰٬۷۱۴٬۳۵۰ بایت | `c0c2b82b6e1c7ee6dfef5ac2c79b1df3ed6c317d5cdba6dc7c41b2459f83bfd9` |
>
> (جدول قدیمی زیر مربوط به بیلد ۲۰۲۶-۰۹-۲۰ است؛ چون کلید آن بیلد با این یکی یکی نیست،
> برای نصب تازه همان فایل تازه را بگذارید.)

| فایل | حجم | sha256 | امضا |
|---|---|---|---|
| `VizitorDirect-1.0.0.apk` | ۷٫۶ مگابایت (۷٬۶۲۳٬۴۱۵ بایت) | `49b8b94fc33a7631f06aee151c018a77902022efaccf24d395751c57488bcf7d` | کلید «Vizitor Direct / Meelano Studio Design» — طرح امضای v2 |
| `VizitorDirect-1.0.0-debug.apk` | ۱۰ مگابایت | `3a89d78393f28bcff7d237e23f0a705f15aec15cc941fad9d3f43b27f09fe7ce` | کلید آزمون اندروید (در کنار نسخهٔ اصلی نصب می‌شود) |

نصب روی گوشی: فایل `VizitorDirect-1.0.0.apk` را انتقال دهید → روی گوشی باز کنید →
اگر پیام «نصب از منابع ناشناس» آمد اجازه دهید → نصب.

**چه چیزی داخل APK بررسی شده است** (گزارش کامل: `apk-report.txt`):
* `apksigner verify` → `Verifies` (طرح امضای v2 + v3، گواهی سازنده).
* ۲ فایل dex در نسخهٔ ریلیز؛ هر دو درایور داخل بسته‌اند
  (`com/microsoft/sqlserver/jdbc/SQLServerDriver` و `net/sourceforge/jtds/jdbc/Driver`).
* کلاس‌های خود برنامه (`ir/atiran/vizitor/direct/MainActivity`,
  `ir/atiran/vizitor/data/sql/MeelanoDataSource`) و اسکنر QR (`com/journeyapps/barcodescanner`) داخل dex.
* مجوز فونت وزیرمتن: `assets/licenses/Vazirmatn-OFL.txt`.

> 🔑 **نگه‌داری کلید امضا:** نسخهٔ ریلیز با کلیدی امضا شده که خودِ CI ساخته است
> (`vizitor-ci.jks` در بخش artifactهای همان اجرا، فقط برای اعضای مخزن قابل دانلود).
> برای نسخه‌های بعدی، همان فایل کلید را در `android-app/keystore.properties` بدهید تا
> به‌روزرسانی روی همان نصب قبلی انجام شود؛ وگرنه باید برنامه را حذف و دوباره نصب کنید.

### ساخت خودکار در گیت‌هاب
ورک‌فلوی `.github/workflows/build-android-direct.yml` با هر تغییر در `android-app/`
اجرا می‌شود: JDK 17 → ساخت کلید → `assembleDebug assembleRelease` → بررسی امضا و محتوای dex →
ثبت گزارش در `android-app/apk-report.txt` → انتشار ریلیز با APKها.

## ۰-۱) تأیید واقعی اتصال: SQL Server زنده + خودِ کد برنامه

اتصال «روی کاغذ» تأیید نمی‌شود. ورک‌فلوی `.github/workflows/verify-sql-connection.yml`
یک **SQL Server واقعی** (کانتینر ۲۰۱۹) بالا می‌آورد، اسکیمای آزمون
(`tools/seed-test-sql.sql`) و **همان کاربر محدودی که نصب‌کننده می‌سازد**
(`vizitor_android` با `db_datareader` + `EXECUTE dbo.add_sail_pish` + `INSERT` روی جدول میانی)
را می‌سازد و بعد آزمون‌های `RealSqlConnectionTest` را با **خودِ کدِ لایهٔ دادهٔ برنامه**
اجرا می‌کند:

| # | آزمون | چه چیزی ثابت می‌شود |
|---|---|---|
| ۱ | هر دو درایور (mssql-jdbc و jTDS) | بار می‌شوند و نشانی‌های JDBC پذیرفته می‌شوند |
| ۲ | اتصال با کاربر محدود به `master` | فهرست `sys.databases` با `db_datareader` گرفته می‌شود |
| ۳ | دیتابیس انتخاب‌شده | اتصال + نسخهٔ سرور + تعداد مشتری |
| ۴ | ورود ویزیتور | `sys_users` با رمز `varbinary` و مقایسهٔ `CONVERT`؛ رمز غلط/حساب غیرفعال/قفل رد می‌شوند |
| ۵ | دادهٔ کاری | مشتریان مجاز، گروه و تیر قیمت، کالا، قیمت، موجودی انبار، هویت ویزیتور |
| ۶ | سلامت پیش‌فاکتور | وجود پروسیجر/سربرگ/سطر/جدول میانی/تریگر + ۲۵ ستون |
| ۷ | امنیت | کاربر محدود `DELETE` نمی‌تواند |
| ۸ | **مسیر نوشتن** | با همان کاربر محدود: `EXEC dbo.add_sail_pish` + `INSERT` در جدول میانی + اجرای تریگر → خواندن سطر واقعی پیش‌فاکتور |

خروجی در [`sql-connection-report.txt`](sql-connection-report.txt) ثبت می‌شود (تاریخ، کامیت،
نتیجهٔ تک‌تک آزمون‌ها از فایل XML گِریدل و خطوط خود آزمون). آزمون ۸ فقط روی دیتابیس
یک‌بارمصرفِ کانتینر CI می‌نویسد، نه روی دیتابیس مشتری.

اصطلاح‌نامهٔ دسترسی‌ها و کارت اتصال هم به‌صورت خودکار مقابله می‌شود
(`api/check_connection_contract.py`) و نتیجه‌اش در
[`server-setup-report.txt`](server-setup-report.txt) می‌آید.

> ⚠️ **چیزی که CI نمی‌تواند ثابت کند:** اجرای واقعی روی گوشی و روی ویندوز مشتری.
> این دو آزمون دستی باقی می‌مانند (نصب APK روی یک گوشی و اجرای `Vizitor-Setup-1.0.0.exe`).

## ۱) ساخت فایل APK روی کامپیوتر خودتان

### راه اول — Android Studio (ساده‌ترین)
1. پوشهٔ `android-app` را در Android Studio باز کنید (Open → همین پوشه).
2. صبر کنید Gradle و کتابخانه‌ها دانلود شوند (اینترنت لازم است؛ فقط برای ساخت، نه برای اجرا).
3. `Build ▸ Build Bundle(s)/APK(s) ▸ Build APK(s)` — فایل خروجی:
   `vizitor-direct/build/outputs/apk/debug/vizitor-direct-debug.apk`

### راه دوم — خط فرمان (ویندوز)
```bat
cd android-app
gradlew.bat assembleDebug
```
خروجی: `vizitor-direct\build\outputs\apk\debug\vizitor-direct-debug.apk`

### نسخهٔ امضاشدهٔ نهایی (برای نصب روی گوشی‌های واقعی)
```bat
keytool -genkeypair -v -keystore vizitor-release.jks -alias vizitor -keyalg RSA -keysize 2048 -validity 10000
```
سپس فایل `android-app\keystore.properties` را بسازید:
```properties
storeFile=../vizitor-release.jks
storePassword=…
keyAlias=vizitor
keyPassword=…
```
و:
```bat
gradlew.bat assembleRelease
```

**نیازمندی‌ها:** JDK 17 (همراه Android Studio می‌آید) + اتصال اینترنت در زمان ساخت + Android SDK 34.

> APK رسمی همین حالا در بخش ۰ موجود است (ساختهٔ رانر گیت‌هاب با همان کد این پوشه).
> ساخت محلی فقط وقتی لازم است که بخواهید کد را تغییر دهید.

---

## ۲) گام‌های کار با برنامه (دقیقاً به ترتیب نصب‌کننده)

| گام | در برنامه | همان چیزی که در نصب‌کننده/سرور انجام شده |
|---|---|---|
| ۱ | **سرور و پورت**: آی‌پی داخلی (و اگر لازم شد آی‌پی اختصاصی) و پورت `1433` | نصب‌کننده همین‌ها را در کارت اتصال نوشته است |
| ۲ | **کاربر دیتابیس**: نام کاربری و رمز → دکمهٔ «تأیید و دریافت فهرست دیتابیس‌ها» | کاربر محدود `vizitor_android` که اسکریپت نصب ساخته |
| ۳ | **انتخاب دیتابیس**: چیپ‌های فهرست `sys.databases` + کادر **تایپ دستی** + «اعمال تنظیمات و تست اتصال» | هیچ دیتابیسی اجباری نیست؛ حتی Meelano |
| ۴ | **خواندن تنظیمات و بررسی سلامت**: نام دیتابیس، نسخهٔ SQL Server، ردیف‌های `CUSTOMERS` و آمادگی اشیای پیش‌فاکتور | همان بازرسی نصب‌کننده، این‌بار از داخل گوشی |
| ۵ | **ورود ویزیتور**: با همان `dbo.sys_users` سامانهٔ اصلی | حساب باید `active` باشد و در `sys_vis` به `visitors` وصل باشد |
| ۶ | **خانه**: کارت ویزیتور، شمار مشتری/کالا/انبار مجاز، مشتریان و کالاها با **پنج تیر قیمت Atiran** | همان کوئری‌های تأییدشدهٔ ERP |

### سه راه خواندن کارت اتصال
1. **اسکن QR** — فایل `C:\Vizitor\setup\android-connect.png` را از روی مانیتور سرور اسکن کنید.
2. **فایل** — دکمهٔ «خواندن فایل android-connect.json» (همان پوشه روی سرور).
3. **متن** — «چسباندن متن کارت» و گذاشتن خط `vizitor://c?...` یا کل متن `android-connect.txt`.

در هر سه حالت، **رمز در کارت نیست**؛ رمز را یک‌بار (کاربر کامل) وارد می‌کنید تا فهرست دیتابیس‌ها
بیاید و بعد دیتابیس حسابداری را انتخاب می‌کنید.

---

## ۳) امنیت

| مورد | پیاده‌سازی |
|---|---|
| رمز SQL | فقط رمزنگاری‌شده روی گوشی (`AES/GCM/NoPadding` با کلید در **Android Keystore**)، در حافظهٔ برنامه هم فقط موقت |
| نمایش رمز | هیچ‌جا نمایش داده نمی‌شود؛ در کارت‌های UI فقط `user@host:1433/db` دیده می‌شود |
| لاگ | هیچ رمز/رشتهٔ حساسی لاگ نمی‌شود؛ پیام خطاها فارسی و بدون رمزند |
| کوئری‌ها | همه `PreparedStatement` و پارامتری؛ هیچ رشته‌ای داخل SQL چسبانده نمی‌شود |
| تغییر داده | همهٔ مسیرهای این نسخه **فقط‌خواندنی**‌اند (`sys.databases`، `CUSTOMERS`، کالا، هویت ویزیتور) |
| ترافیک | `cleartextTrafficPermitted=false` در `network_security_config`؛ اتصال، پروتکل بومی TDS روی ۱۴۳۳ است |
| اتصال از بیرون | توصیه: پورت ۱۴۳۳ را محدود به آی‌پی خودتان کنید یا از VPN استفاده کنید |

---

## ۴) هماهنگی با نصب‌کنندهٔ سرور

| چیز | نصب‌کنندهٔ ویندوز | این برنامه |
|---|---|---|
| پورت SQL | ۱۴۳۳ (فایروال + TCP/IP) | همان ۱۴۳۳ |
| کارت اتصال | `vizitor://c?h=…&p=1433&d=…&u=…&H=…` | همان قالب را می‌خواند (`ConnectCards.kt`) |
| فایل کارت | `setup\android-connect.json` | همان کلیدها: `host_lan`, `host_public`, `port`, `database`, `login`, `panel_url` |
| کاربر محدود | `vizitor_android` (ساخته/بررسی‌شده توسط `api/provision_android_sql.py`) | همان کاربر را در گام ۲ می‌گیرد |
| طرح و امضا | سرمه‌ای یکدست + وزیرمتن + «میلاد یقوبی — Meelano Studio Design» | همان پالت و همان فونت و همان امضا |

---

## ۵) چه چیزی واقعاً بررسی شده است؟

* **کامپایل واقعی** لایهٔ منطق و داده با کامپایلر Kotlin (نسخه ۲٫۴٫۲۰ روی JVM):
  `MeelanoDataSource.kt`، `SqlConnectionManager.kt`، `SecureDbStore.kt`، `DirectSql.kt`،
  `ServerConfig.kt`، `ConnectCards.kt` و `DirectViewModel.kt` — **بدون هیچ خطا و هشدار**.
* **یازده خطای واقعی** در `SqlConnectionManager.kt` (فایل «تأییدشدهٔ» قبلی که هرگز کامپایل نشده بود)
  پیدا و رفع شد:

  | خطا | جای خطا | رفع |
  |---|---|---|
  | ۶ کاراکتر تک‌کوتیشن چندحرفی (`';databaseName='`) | `jdbcUrl()` | تبدیل به رشتهٔ درست `";databaseName="` |
  | `@Synchronized` روی تابع `suspend` (در Kotlin مجاز نیست) | `connect()` | قفل `connecting` (همان متغیری که بی‌استفاده مانده بود) |
  | «return type mismatch» در حلقهٔ بی‌پایان داخل لامبدا | `withConnection()` | انتقال بدنه به تابع خصوصی `runWithRetry()` |
  | `st.execute("SELECT 1").close()` — `execute` مقدار Boolean برمی‌گرداند | `ping()` | حذف `close()` |
  | `e.message.lines()` روی مقدار nullable | `friendlyError()` | `e.message?.lines()?.firstOrNull()` |

* نسخهٔ اصلاح‌شده هم در همین پروژه (`android-app/.../SqlConnectionManager.kt`) و هم در فایل
  جایگزینِ برنامهٔ خودتان (`android-sql-direct/SqlConnectionManager.kt`) به‌روزرسانی شد.
* توازن ساختاری و کامل بودن ایمپورت‌های همهٔ فایل‌های رابط کاربری (Compose) با بررسی خودکار.
* **ساخت واقعی APK** روی رانر گیت‌هاب (JDK 17 + Android SDK 34) سه بار پشت‌سرهم: `assembleDebug`
  و `assembleRelease` هر دو موفق، `apksigner verify` → `Verifies`، گزارش در `apk-report.txt`.
* ⛔ **آنچه بررسی نشده:** اجرای برنامه روی گوشی/شبیه‌ساز و اتصال واقعی به SQL Server از داخل
  برنامه (این را باید روی گوشی خودتان با کارت اتصال نصب‌کننده امتحان کنید).

---

## ۶) ساختار پروژه

```
android-app/
├─ gradlew / gradlew.bat / gradle/wrapper/     ← واسط Gradle (نسخهٔ ۸٫۷)
├─ settings.gradle.kts  build.gradle.kts        ← نسخهٔ AGP 8.5.2 / Kotlin 1.9.24
└─ vizitor-direct/
   ├─ build.gradle.kts                          ← وابستگی‌ها: Compose، mssql-jdbc، jTDS، zxing
   └─ src/main/
      ├─ AndroidManifest.xml                    ← مجوزها (INTERNET، CAMERA) + فیلتر vizitor://
      ├─ assets/licenses/Vazirmatn-OFL.txt      ← مجوز فونت
      ├─ res/font/vazirmatn_*.ttf               ← فونت وزیرمتن
      ├─ res/mipmap-*/ic_launcher*.png          ← آیکن برنامه (از همان آیکن نصب‌کننده)
      └─ java/ir/atiran/vizitor/
         ├─ data/sql/MeelanoDataSource.kt       ← تمام کوئری‌های تأییدشدهٔ ERP (بدون تغییر)
         ├─ data/sql/SqlConnectionManager.kt    ← اتصال/استخر/تلاش مجدد (اصلاح‌شده)
         ├─ data/sql/DirectSql.kt               ← انتخاب درایور (رسمی مایکروسافت یا jTDS)
         ├─ data/local/SecureDbStore.kt         ← نگهداری رمزنگاری‌شدهٔ تنظیمات
         ├─ data/repository/ServerConfig.kt     ← مدل تنظیمات (serverIp/dbPort/…)
         ├─ direct/DirectViewModel.kt           ← منطق صفحه‌ها و وضعیت‌ها
         ├─ direct/util/ConnectCards.kt         ← خواندن کارت اتصال نصب‌کننده
         └─ direct/ui/…                         ← چهار صفحه + اجزای مشترک + تم
```

## ۷) عیب‌یابی سریع

| نشانه | راه‌حل |
|---|---|
| «به سرور دیتابیس نمی‌توان رسید» (SQL 53) | آی‌پی/پورت را چک کنید؛ روی سرور TCP/IP و فایروال ۱۴۳۳ باز باشد؛ از داخل شبکه با آی‌پی داخلی وصل شوید |
| «ورود به دیتابیس ناموفق بود» (SQL 18456) | نام کاربری/رمز SQL را چک کنید (حالت SQL Authentication روی سرور فعال باشد) |
| «دیتابیس پیدا نشد» (SQL 4060) | نام دیتابیس را از لیست انتخاب کنید (گام ۳) |
| ورود ویزیتور کار نمی‌کند | در `sys_users` حساب `active` باشد و در `sys_vis` به ردیف `visitors` وصل باشد |
| خطای درایور روی گوشی‌های قدیمی | برنامه خودکار به درایور پشتیبان jTDS برمی‌گردد؛ برای اطلاعات بیشتر «شناسنامه» را ببینید |
