# پرامپت ساخت «آتیران ویزیتور / گزارشات مدیر» — از صفر تا ۱۰۰

> **این سند خودش پرامپت است.** کل متن زیر را (از خط «◤ شروع پرامپت» تا «◢ پایان پرامپت»)
> به هوش مصنوعیِ سازنده بدهید. هر بخش، تصمیم‌های فنیِ **آزموده‌شده و کامپایل‌شدهٔ** این پروژه
> است؛ چیزی از خودتان اضافه/کم نکنید مگر در «نکات اختیاری» صریح بگویید.
>
> نسخهٔ مرجع: **۲٫۲۴٫۰** — دو بستهٔ `ir.atiran.vizitor` و `ir.atiran.mreport`، اتصال مستقیم
> SQL Server روی پورت ۱۴۳۳، بخش گزارش‌های مدیر «M•A Report» + کارت «سلامت سرور».

---
---

## ◤ شروع پرامپت

### ۰) نقش تو

تو یک مهندس ارشد اندروید (Kotlin/Jetpack Compose) + مهندس دادهٔ SQL Server هستی. باید یک
پروژهٔ اندروید **کامل و کامپایل‌شدنی** بسازی که:

1. مستقیماً (بدون IIS، بدون API میانی، بدون REST) به **SQL Server** وصل شود؛
2. اطلاعات گزارش‌های مدیریتی را با کوئری **فقط-خواندنی** از جدول‌های واقعی بخواند؛
3. رابط کاربری فارسی راست‌به‌چپ با ظاهر «M•A Report» (طلایی/فلزی/سه‌بعدی) بسازد؛
4. با **دو بستهٔ جدا** منتشر شود (نسخهٔ کامل ویزیتور + نسخهٔ انحصاری گزارشات مدیر) که کنار
   هم روی یک گوشی نصب می‌شوند؛
5. امضای ثابت داشته باشد تا روی نسخهٔ نصب‌شده **به‌روزرسانی** شود؛
6. با GitHub Actions به‌صورت خودکار APK بسازد، امضا کند و در Releases منتشر کند.

**زبان پاسخ و مستندات: فارسی.** نام‌های کد و کامنت‌های فنی هم فارسی‌اند (کد باید با
کامنت فارسی خوانا باشد). اعداد نمایشی روی صفحه **فارسی** (`۰۱۲۳۴۵۶۷۸۹`) باشند.

---

## ۱) نتیجهٔ نهایی (Definition of Done)

| # | خروجی | معیار پذیرش |
|---|---|---|
| ۱ | پروژهٔ Gradle قابل ساخت | `./gradlew assembleVizitorRelease assembleMreportRelease` موفق |
| ۲ | دو APK امضاشده | `Vizitor-2.24.0-direct.apk` و `MReport-2.24.0-direct.apk` |
| ۳ | بسته‌ها | `ir.atiran.vizitor` (versionCode 21820) و `ir.atiran.mreport` (versionCode 21820) کنار هم نصب شوند |
| ۴ | اتصال واقعی | روی شبکهٔ داخلی به `192.168.1.150` و روی اینترنت به `37.143.147.19` وصل شود |
| ۵ | ورود کاربر | با جدول `dbo.sys_users` (فقط-خواندنی) کاربر وارد شود و شناسهٔ ویزیتورش خوانده شود |
| ۶ | گزارش‌ها | شمار رکورد/مجموع مبلغ/برترین‌ها/آخرین رکوردها/روند ماهانه از جدول‌های واقعی |
| ۷ | سلامت سرور | نمرهٔ ۰ تا ۱۰۰ + چهار دستهٔ بررسی (شبکه، اتصال، دیتابیس، امنیت) |
| ۸ | پوشیدگی | در کل APK هیچ نشانی/کاربر/رمز SQL خوانا نباشد (Base64) |
| ۹ | انتشار | دو ریلیز GitHub با APK + `.sha256` (+ نسخهٔ آزمون + `SHA256SUMS.txt`) |

---

## ۲) محیط ساخت و نسخه‌های قفل‌شده

| مورد | نسخه |
|---|---|
| Gradle | 8.9 (`gradle-wrapper.properties` → `gradle-8.9-bin.zip`) |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.20 + پلاگین `org.jetbrains.kotlin.plugin.compose` 2.0.20 |
| KSP | 2.0.20-1.0.25 |
| JDK | 17 (CI: `actions/setup-java@v4` با temurin 17) |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| Compose BOM | `2024.09.03` + `material3` + `material-icons-extended` |
| Room | 2.6.1 (runtime/ktx/ksp) |
| WorkManager | 2.9.1 |
| Retrofit/OkHttp/Gson | 2.11.0 / 4.12.0 / 2.11.0 |
| DataStore | 1.1.1 |
| کارایی/رسانه | CameraX 1.3.4 + ML Kit barcode-scanning 17.3.0 |
| **درایورهای SQL** | `com.microsoft.sqlserver:mssql-jdbc:9.4.1.jre8` + `net.sourceforge.jtds:jtds:1.3.1` |
| Desugaring | `com.android.tools:desugar_jdk_libs:2.1.2` با `isCoreLibraryDesugaringEnabled = true` |
| gradle.properties | `-Xmx4096m -Dfile.encoding=UTF-8 -XX:+UseParallelGC`, `parallel=true`, `caching=true`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true` |

**نکتهٔ حیاتی درایور:** mssql-jdbc از `java.time` استفاده می‌کند که در API 24/25 نیست؛
**بدون desugaring** روی گوشی‌های قدیمی `NoClassDefFoundError` می‌دهد.

**بسته‌بندی منابع (packaging):** هر دو درایور `META-INF` مشترک دارند:
```kotlin
packaging {
    resources.excludes += setOf(
        "META-INF/LICENSE*", "META-INF/NOTICE*", "META-INF/DEPENDENCIES",
        "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
        "META-INF/versions/**",
    )
    resources.pickFirsts += setOf("META-INF/services/java.sql.Driver")
}
```

`AndroidManifest.xml` باید این مجوزها را داشته باشد:
`INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `ACCESS_FINE_LOCATION`,
`ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`, `VIBRATE` — و
`android:usesCleartextTraffic="true"`، `android:supportsRtl="true"`،
`android:name=".VizitorApp"`. (مجوز `ACCESS_NETWORK_STATE` برای تشخیص نوع شبکهٔ اتصال
هوشمند لازم است.)

---

## ۳) ساختار پروژه

```
vizitor-app/
├── settings.gradle.kts            (rootProject.name = "Vizitor"; include(":app"))
├── build.gradle.kts               (پلاگین‌ها: android 8.5.2 / kotlin 2.0.20 / compose / ksp)
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/ir/atiran/vizitor/
        │   │   ├── VizitorApp.kt, MainActivity.kt, VizitorViewModel.kt
        │   │   ├── sqldirect/        ← لایهٔ اتصال مستقیم SQL (قلب پروژه)
        │   │   ├── ui/components/     ← کتابخانهٔ رابط کاربری (فلزی/سه‌بعدی/نمودار)
        │   │   ├── ui/screens/        ← صفحه‌های برنامهٔ کامل
        │   │   ├── ui/screens/manager/← اتاق فرمان گزارش + خانهٔ M•A Report
        │   │   ├── ui/theme/          ← پالت‌های رنگ، فونت وزیرمتن، تم پویا
        │   │   ├── data/              ← Room/Retrofit/کارگر پس‌زمینه (اختیاری برای ویزیت)
        │   │   └── util/Format.kt     ← اعداد و مبالغ فارسی
        │   └── res/                   ← drawable-nodpi, font, mipmap-*, values, xml
        ├── vizitor/res/values/strings.xml   (app_name = «آتیران ویزیتور»)
        └── mreport/res/values/strings.xml   (app_name = «گزارشات مدیر»)
```

**حداقل فایل‌های ضروری لایهٔ داده/گزارش** (نام‌ها را عیناً همین بگذارید تا بازرسی‌های CI و
نگهداری ساده باشد):

| فایل | مسئولیت |
|---|---|
| `sqldirect/DirectSql.kt` | بارگذاری رفلکتیو درایورها (`jtds`, `mssql`) و ساخت JDBC URL پایه |
| `sqldirect/MaSqlEngine.kt` | موتور چهارحالتهٔ اتصال + همهٔ کوئری‌های گزارش |
| `sqldirect/MaServerProfile.kt` | **پروفایل مخفی سرور** + تشخیص شبکه + اتصال هوشمند + پوشاندن اطلاعات |
| `sqldirect/MaMapping.kt` | بخش‌های گزارش، هیوریستیک نام جدول‌ها و نقش ستون‌ها، ذخیرهٔ نگاشت |
| `sqldirect/SecureDbStore.kt` | ذخیرهٔ رمزنگاری‌شده (Keystore/AES-GCM) تنظیمات، حساب کاربر، هویت ویزیتور |
| `sqldirect/MeelanoDataSource.kt` | کوئری‌های واقعی کسب‌وکار (sys_users, sys_vis, visitors, sys_cus, …) |
| `sqldirect/SqlConnectionManager.kt` | مدیر اتصال مشترک برنامهٔ کامل (`super`) |
| `ui/screens/manager/MReportHome.kt` | خانهٔ مرجع «M•A Report» (۶ بخش + پوستهٔ طلایی) |
| `ui/screens/manager/MaManagerScreen.kt` | اتاق فرمان گزارش‌ها (دسته‌بندی + برگه‌ها) |
| `ui/screens/manager/MaServerHealth.kt` | **سلامت سرور** (نمرهٔ ۰..۱۰۰ + چهار دسته) |
| `ui/components/Lux3DButtons.kt` | ویجت‌های سه‌بعدی (دکمه/کاشی/گیج/چیپ/چراغ) |

---

## ۴) مشخصات سرور (این داده‌ها **مخفی** می‌مانند)

| مورد | مقدار |
|---|---|
| نشانی بیرونی (اینترنت) | `37.143.147.19` |
| نشانی داخلی (شبکهٔ اداره) | `192.168.1.150` |
| پورت | `1433` |
| دیتابیس | `Atiran2` |
| کاربر SQL | `AdminAn` |
| رمز SQL | `St@R2022$` |
| نوع دسترسی | **فقط SELECT** + کاتالوگ سیستم (`sys.*`) — هیچ INSERT/UPDATE/DELETE |

### قاعدهٔ پوشیدگی (تخطی‌ناپذیر)

* این پنج مقدار **هرگز** در رابط کاربری، پیام خطا، فهرست رشته‌های APK یا لاگ چاپ نشوند.
* در کد به‌شکل **Base64** نگه داشته شوند:

```kotlin
private const val H_EXT_B64  = "MzcuMTQzLjE0Ny4xOQ=="   // 37.143.147.19
private const val H_LOC_B64  = "MTkyLjE2OC4xLjE1MA=="   // 192.168.1.150
private const val DB_B64     = "QXRpcmFuMg=="           // Atiran2
private const val USER_B64   = "QWRtaW5Bbg=="           // AdminAn
private const val PASS_B64   = "U3RAUjIwMjIk"           // St@R2022$
private const val PORT = 1433
private fun b64(s: String) = String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
```

* همهٔ متن‌های رو-به-کاربر باید از تابع پوشاننده بگذرند:

```kotlin
fun safe(raw: String?): String            // IP:port → «سرور آتیران»، Atiran2 → «دیتابیس آتیران»،
                                          // AdminAn → «حساب گزارش‌ها»، رمز → «••••••»، «پورت 1433» → «پورت سرور»
fun safeSteps(steps: List<MaDiagStep>): List<MaDiagStep>   // پوشاندن گام‌های عیب‌یابی
```

الگوهای حذف: IPv4 (`\b\d{1,3}(?:\.\d{1,3}){3}\b`)، `host:port`، `(پورت|port)\s*[:=]?\s*\d{2,5}`،
نام دیتابیس، نام کاربر، رمز.

---

## ۵) تشخیص هوشمند شبکه و انتخاب مسیر

```kotlin
enum class MaNetKind(val label: String) {
    WIFI("وای‌فای / شبکهٔ داخلی"), MOBILE("اینترنت همراه"),
    ETHERNET("شبکهٔ کابلی"), NONE("بدون شبکه"), OTHER("شبکهٔ نامشخص")
}
```

* با `ConnectivityManager` + `NetworkCapabilities` نوع شبکهٔ فعال خوانده شود
  (WIFI ⇒ داخلی اول، ETHERNET ⇒ داخلی اول، CELLULAR/OTHER/NONE ⇒ بیرونی اول).
  بی‌نیاز از مجوز تازه (فقط `ACCESS_NETWORK_STATE`).
* `orderedHosts(ctx)` ترتیب امتحان را می‌دهد؛ `connectSmart(ctx)`:
  1. **کاوش سریع TCP** روی هر نشانی (`rawTcpProbe(host, 1433, 3000ms)`) و اولویت‌دادن به
     نشانی‌هایی که پورتشان باز است؛
  2. سپس **اتصال واقعی چهارحالته** به ترتیب؛ اولین موفق برنده است؛
  3. مسیر موفق (داخلی/بیرونی) در حافظهٔ رمزنگاری‌شده **ذخیره** شود تا اجرای بعدی سریع باشد؛
  4. اگر هیچ‌کدام نشد، پیام روشن و بدون لو دادن نشانی: «گوشی به شبکه وصل نیست…» یا
     «اتصال به سرور برقرار نشد — شبکه را بررسی کنید.»
* خروجی `connectSmart` یک `MaSmartConnect(ok, netLabel, channelLabel /*داخلی|اینترنت*/,
  modeLabel /*خودکار|جایگزین*/, message)` باشد.

---

## ۶) موتور اتصال چهارحالته (دقیقاً همین ترتیب و همین URLها)

```kotlin
enum class Mode(val label: String, val usingMssql: Boolean, val tls: Boolean) {
    MSSQL_TLS("Microsoft + TLS", true, true),
    MSSQL_PLAIN("Microsoft بدون TLS", true, false),
    JTDS_PLAIN("jTDS (جایگزین ویندوز)", false, false),
    JTDS_TLS("jTDS + TLS", false, true),
}
```

* اگر `useEncryption == true` → ترتیب: `MSSQL_TLS → MSSQL_PLAIN → JTDS_PLAIN → JTDS_TLS`
* وگرنه → `MSSQL_PLAIN → JTDS_PLAIN → MSSQL_TLS → JTDS_TLS`
* هر تلاش، **اعتبارسنجی با `SELECT 1`** دارد؛ در موفقیت `live`/`activeMode` ثبت می‌شود.

ساخت URL (عیناً):

```kotlin
// مایکروسافت
"jdbc:sqlserver://$host:$port;databaseName=$db" +
  (if (tls) ";encrypt=true;trustServerCertificate=true;sslProtocol=TLS"
   else     ";encrypt=false;trustServerCertificate=true") +
  ";connectRetryCount=1;connectRetryDelay=1" +
  ";applicationName=AtiranVizitor;loginTimeout=$login" +
  (if (instance != null) ";instanceName=$instance" else "")

// jTDS
"jdbc:jtds:sqlserver://$host:$port/$db;loginTimeout=$login;socketTimeout=$queryTimeout" +
  (if (tls) ";ssl=request" else "") + (if (instance != null) ";instance=$instance" else "")
```

* کاربر/رمز با `Properties("user" → …, "password" → …)` داده شود (نه داخل URL).
* پارس هدف: `IP | SERVER\INSTANCE | IP:PORT | IP\INSTANCE:PORT` (پشتیبانی از نمونهٔ نام‌بریده
  و پرس‌وجوی پورت از SQL Browser روی UDP 1434).
* اگر همهٔ حالت‌ها شکست خورد: اول `rawTcpProbe` تا مشخص شود مشکل «شبکه/فایروال» است یا
  «رمز/دیتابیس»، و پیام متناسب بده (مثلاً فیلتر شدن پورت ۱۴۳۳ توسط اپراتور همراه).

**عیب‌یابی گام‌به‌گام** (`diagnose`) این گام‌ها را برگرداند: هدف اتصال → پورت واقعی نمونه
(UDP 1434) → کاوش خام TCP → پاسخ PRELOGIN (TDS) → کاوش TLS → اتصال واقعی چهارحالته →
پیام خطای دوستانه. هر مرحله `MaDiagStep(ok, title, detail, hint, suggestedPort?)` و
**قبل از نمایش از `safeSteps` بگذرد**.

---

## ۷) ذخیرهٔ امن روی گوشی

`SecureDbStore` (SharedPreferences `vizitor_db` + blob رمزنگاری‌شده با AES-GCM و کلید
Keystore — روی API پایین‌تر fallback نرم‌افزاری) این کلیدها را نگه دارد:

| کلید | کاربرد |
|---|---|
| `host, port, database, username, password, useEncryption, trustServerCert, connectTimeoutSec, queryTimeoutSec` | تنظیمات اتصال (پیش‌فرض‌های مخفی) |
| `hostExternal, hostLocal, useExternal` | مسیر شناخته‌شدهٔ اتصال (برای اجرای بعدی) |
| `erpUser, erpPassword, erpRemember` | حساب کاربر آتیران (رمز فقط اگر «به‌خاطر بسپار» روشن باشد) |
| `visUserId, visCompanyId, visVisitorRdf, visName` | هویت ویزیتور وارد‌شده |

`DbSettings(host, port = 1433, database, username, password, useEncryption = true,
trustServerCert = true, connectTimeoutSec = 8, queryTimeoutSec = 30)`.

`MaSectionStore` (SharedPreferences `vizitor_manager`): نگاشت پنج بخش (`section_map_v1`) +
`host_local/host_net/use_net`؛ اگر خالی بود از **پروفایل مخفی** پر شود (کاربر چیزی وارد نمی‌کند).

---

## ۸) خواندن اطلاعات (کوئری‌های واقعی — همه فقط-خواندنی)

### ۸٫۱ ورود کاربر با حساب خودش (تنها ورودی کاربر)

```sql
SELECT TOP (1)
       sys_users.user_id, sys_users.user_name, sys_users.user_fname, sys_users.user_lname,
       sys_users.role_id, sys_users.active,
       CASE WHEN sys_users.IsLocked = 1 THEN 1 ELSE 0 END AS is_locked,
       sys_users.shmo
  FROM dbo.sys_users
 WHERE sys_users.user_name = ?
   AND CONVERT(varchar(50), sys_users.user_password) = ?
   AND sys_users.active = 1
```
سپس گاردها: اگر ردیفی نبود ⇒ «نام کاربری یا کلمهٔ عبور نادرست است»؛ اگر `is_locked = 1` ⇒
«حساب قفل است»؛ اگر `active = 0` ⇒ «حساب غیرفعال است».

### ۸٫۲ هویت ویزیتور (تأییدشده روی دادهٔ واقعی: `visitors.UserID` تهی است)

```sql
SELECT TOP (1)
       visitors.vis_rdf, visitors.vis_name, visitors.active,
       visitors.VIs_region, visitors.vis_city,
       (SELECT COUNT(*) FROM dbo.sys_cus WHERE sys_cus.UserID = sys_vis.UserID) AS allowed_customers,
       (SELECT COUNT(*) FROM dbo.sys_kal WHERE sys_kal.UserID = sys_vis.UserID) AS allowed_products,
       (SELECT COUNT(*) FROM dbo.sys_anb WHERE sys_anb.UserID = sys_vis.UserID) AS allowed_warehouses
  FROM dbo.sys_vis
  JOIN dbo.visitors ON visitors.vis_rdf = sys_vis.shvis
 WHERE sys_vis.UserID = ?
   AND (? = 0 OR sys_vis.SysID = ?)
 ORDER BY visitors.vis_rdf
```
زنجیرهٔ کلیدی که باید رعایت شود: `sys_users.user_id → sys_vis.UserID → sys_vis.shvis →
visitors.vis_rdf`.

### ۸٫۳ سایر کوئری‌های کسب‌وکار (الگو)

| موضوع | جدول‌ها |
|---|---|
| کالاهای مجاز | `dbo.inventory` (شکا/ناکا/کوکا/گروه، فیلتر `active`) |
| قیمت فروش | `dbo.forosh_price` |
| موجودی انبارها | `dbo.VW_InventoryAnbars` |
| مشتریان مجاز | `dbo.sys_cus JOIN dbo.CUSTOMERS` (`sys_cus.UserID`, `CUSTOMERS.vis_rdf`) |
| گروه کالا / گروه مشتری | `dbo.kagroup`, `dbo.custgroup` |
| پیش‌فاکتور و فاکتور | `dbo.sailfact_pish`, `dbo.sailfact` (`vis_rdf, sysid, shfacfo, date, shmo, all, tafif`) |
| ویزیت‌ها | `dbo.Visit` |
| مسیرها | `dbo.masir` |

### ۸٫۴ کوئری‌های گزارش مدیریتی (لایهٔ M•REPORT)

| تابع | کار |
|---|---|
| `test(cfg)` | `SELECT DB_NAME(), @@SERVERNAME, @@VERSION` |
| `overview()` | `@@VERSION` + حجم دیتابیس: `SELECT SUM(CAST(size AS BIGINT))*8 FROM sys.master_files WHERE database_id = DB_ID()` + فهرست جدول‌ها از `sys.tables/sys.schemas/sys.partitions` با شمار رکورد |
| `columnsOf(schema, table)` | ستون‌ها از `INFORMATION_SCHEMA.COLUMNS` / `sys.columns` |
| `count`, `sumOf` | شمار و مجموع یک ستون (با `QUOTENAME` برای شناسه‌ها) |
| `topBy`, `latestBy` | برترین/آخرین رکوردها بر اساس ستون مبلغ/تاریخ |
| `page(schema, table, page, size, search)` | صفحه‌بندی + جست‌وجوی زنده (همه فقط `SELECT`) |

**قاعدهٔ سخت:** هیچ عدد ساختگی نمایش داده نشود. اگر دادهٔ واقعی نیست، همان متن دقیق مرجع:
«دادهٔ فاکتور در دسترس نیست» · «ستون مبلغ این جدول تشخیص داده نشد» · «مشتری‌ای یافت نشد» ·
«همه‌چیز مرتب است» · «★ داده‌های این بخش صرفاً برای نمایش قابلیت‌های M•A Report است».

---

## ۹) نگاشت بخش‌ها و تشخیص هوشمند ستون‌ها

پنج بخش گزارش با کلید/برچسب فارسی: `customers مشتریان`، `products کالاها`،
`invoices فروش و فاکتورها`، `checks چک‌ها`، `banks بانک‌ها و حساب‌ها`.

**نام‌های حدسی جدول هر بخش** (به همین ترتیب اولویت):

| بخش | نامزدها |
|---|---|
| مشتریان | `customer, moshtar, مشتری, CUSTOMERS, ashkh, اشخاص, person, client, partner, طرف, customers` |
| کالاها | `kala, KALA, کالا, product, item, goods, جنس, merch, inventory, products` |
| فاکتور/فروش | `factor, faktor, فاکتور, invoice, sale, forosh, فروش, order, sailfact, invoices` |
| چک‌ها | `check, cheque, چک, checks, cheques, check_detail` |
| بانک/حساب | `bank, بانک, banks, account, hesab, حساب` |

**تشخیص نقش ستون‌ها** (خودکار، روی نام + نوع):

* عنوان: `fullname, customername, kalaname, name, title, onvan, نام, شرح` و نوع غیرعددی
* مبلغ: نوع در `{decimal, numeric, money, smallmoney, float, real}` و نام شامل
  `mablagh, amount, مبلغ, price, ghimat, قیمت, bedehkar, bestankar, mandeh, mande, مانده, balance, total, jam, جمع, allfel, mabdaryaft, tdf`
* تاریخ: `date, tarikh, تاریخ`
* کد: `code, shomare, کد, id, shmo, shka, shfacfo`

«تشخیص خودکار جداول» (`autoDetect`) برای هر بخش، اولین نامزد موجود در `sys.tables` را
انتخاب کند و نگاشت را ذخیره کند؛ دکمهٔ «اتصال جداول» هم اجازهٔ نگاشت دستی `schema.table` بدهد.

---

## ۱۰) رابط کاربری

### ۱۰٫۱ هویت بصری

* پالت پیش‌فرض: **«گزارش طلایی»** — پس‌زمینهٔ مشکی عمیق، سطوح گرافیتی، طلای فلزی،
  خط نور استودیویی بالای کارت‌ها، هاله‌های نوری. رنگ‌های وضعیت: `MaGreen #5BC26A`،
  `MaAmber #E0A94A`، `MaRed #E5544B`.
* همهٔ رنگ‌ها از یک `VizitorPalette` واحد خوانده شوند (تم‌های چنده‌گانه پشتیبانی شوند).
* فونت **وزیرمتن** در `res/font` + پشتیبانی RTL و اعداد فارسی (`toFaNumber`, `toFaPrice`,
  `toFaDigits`).
* ویجت‌های مشترک (نام‌ها را رعایت کن): `MaTopBar`, `MaNavStrip`, `MaHeroTitle`,
  `MaGoldCta`, `MaMetricCard`, `MaField`, `MaOrbButton`, `MaSegmentPills`, `MaStatStrip`,
  `MaDocCard`, `MaSmartSheet/Group/Tile`, `MaPulseCard/Row`, `MaSectionHeader`,
  `MaEmptyState`, `Lux3DTable`, `Lux3DNote`, نمودارها: `MetalBarChart`, `Lux3DBarChart`,
  `Lux3DDonut`, `LineTrendChart`.
* `Modifier.metalPanel(...)` برای سطوح فلزی و `Modifier.dashboardBackdrop()` برای پس‌زمینه.

### ۱۰٫۲ اتاق فرمان (MaManagerScreen)

* نوار بالا: عنوان «گزارش مدیریت» (یا «گزارشات مدیر» در نسخهٔ انحصاری) + چیپ وضعیت
  («متصل ✓» / «وصل نشده — «اتصال و ورود»»).
* **دسته‌بندی سه‌گانه** با کاشی‌های سه‌بعدی؛ زیر هر دسته فقط برگه‌های همان دسته:

| دسته | زیربخش‌ها |
|---|---|
| **اتصال و سلامت** | «اتصال و ورود» · «سلامت سرور» |
| **داده و جداول** | «نمای کلی» · «مرور داده‌ها» · «اتصال جداول» |
| **گزارش و تحلیل** | «گزارش‌ها» |

* برگهٔ «اتصال و ورود»: کارت «اتصال هوشمند» (شبکهٔ فعلی/وضعیت/مسیر/کاربر)، دکمهٔ
  «اتصال خودکار به سرور»، نوار سلامت، کاشی‌های «سلامت کامل/آزمایش اتصال/عیب‌یابی»، و
  **کارت ورود** با دو فیلد (نام کاربری، کلمهٔ عبور + نمایش/پنهان) + سوییچ «به‌خاطر سپردن»
  + دکمهٔ «ورود و همگام‌سازی». **هیچ فیلد نشانی/پورت/دیتابیس/کاربر SQL وجود نداشته باشد.**
* اتصال هنگام باز شدن صفحه **خودکار** انجام شود (skip اگر شبکه نیست) و پس از موفقیت
  یک **سنجش سبک سلامت** اجرا شود.

### ۱۰٫۳ خانهٔ «M•A Report» (نسخهٔ انحصاری — کلون مرجع)

ساختار دقیق از بالا به پایین (رعایت شود):

1. نوار بالا: دکمهٔ بازگشت + عنوان طلایی «M•A Report» + زیرنویس
   «Intelligent Reporting Experience» + سه دکمهٔ گرد (تنظیمات هوشمند، هشدار با شمارنده، جست‌وجو)
2. نوار شش بخش: **نمای کلی · مشتریان · کالاها · خزانه · گزارش‌ها · اعلان‌ها**
3. اگر وصل نیست: دکمهٔ «اتصال خودکار به سرور» (نشان «خودکار»)، وگرنه نوار
   «وصل / مسیر / جدول‌ها»
4. **نوار سلامت سرور** (نمره + سرصفحه + دکمهٔ بررسی)
5. اگر حساب کاربری ذخیره نشده: کارت «ورود کاربر آتیران» (دو فیلد + دکمهٔ «ورود و همگام‌سازی»)
6. «نبض کسب‌وکار» (۶ ردیف با نوار رنگی، مقادیر واقعی)
7. «M•D Intelligence»
8. چهار کارت شاخص: گردش مالی · مشتریان · کالاها · چک‌ها (شمار/مجموع واقعی + نام جدول)
9. «چارت‌های من» (کارت طلایی)
10. «روند مطالبات و وصول طلب — ۸ ماه اخیر» (نمودار خطی دومسیره)
11. «توزیع سطح مشتریان — ۳بعدی خلاقانه» + «مانده حساب — سهم هر مشتری — دونات ۳بعدی»
12. «وضعیت مطالبات»، «Top Products Report»، «Executive Summary» و فریم امضا
13. برگهٔ «تنظیمات هوشمند» به‌صورت Sheet با گروه‌ها/کاشی‌های مرجع
14. پانویس: «★ داده‌های این بخش صرفاً برای نمایش قابلیت‌های M•A Report است» +
    «MEELANO STUDIO DESIGN» + «برنامه‌نویس • میلاد یعقوبی»

### ۱۰٫۴ کارت «سلامت سرور مورد اتصال» (MaServerHealth.kt)

تابع `MaServerHealth.probe(ctx, quick: Boolean): MaHealthReport` با چهار دسته:

| دسته (key) | بررسی‌ها |
|---|---|
| **شبکه و مسیر** (`net`) | شبکهٔ فعلی گوشی · مسیر انتخابی (داخلی/اینترنت) · زمان پاسخ TCP به میلی‌ثانیه · پایداری مسیر (۳ کاوش پی‌درپی) |
| **اتصال و احراز هویت** (`conn`) | برقراری اتصال + زمان آن · حالت اتصال موتور · تأیید دیتابیس با `SELECT` آزمایشی + نسخهٔ سرور · وضعیت حساب کاربر (welcome) |
| **دیتابیس و گزارش‌ها** (`db`) | نسخهٔ سرور · حجم دیتابیس · شمار جدول‌ها · مجموع رکوردها · بخش‌های نگاشت‌شده (از ۵) |
| **دسترسی و امنیت** (`sec`) | آزمون خواندن گزارش (شمار رکورد در چند ms) · نقش ستون‌ها (از ۴) · فقط‑SELECT بودن · پوشیدگی مشخصات |

* هر بررسی: `MaHealthItem(title, value, detail, level)` با `Lux3DLevel ∈ {OK, WARN, BAD, IDLE, RUN}`.
* سطح هر دسته = بدترین سطح اعضایش؛ **نمرهٔ سلامت** = میانگین وزن‌دار
  (OK=۱۰۰، WARN=۶۰، IDLE=۷۰، RUN=۵۰، BAD=۰) و سرصفحهٔ متنی: ≥۹۰ «سرور سالم و آماده است ✓»،
  ≥۷۰ «چند نکتهٔ کوچک»، ≥۴۰ «نیازمند بررسی»، وگرنه «اتصال سرور درست نیست».
* `quick = true` (خانه) فقط دو دستهٔ شبکه و اتصال را می‌سنجد؛ حالت کامل چهار دسته.
* نمایش: `MaServerHealthCard` (گیج شعاعی + چک‌لیست)، `MaServerHealthStrip` (نوار فشرده)،
  `MaServerHealthChip`. همهٔ متن‌ها پس از `safe()`.

### ۱۰٫۵ ویجت‌های سه‌بعدی (`ui/components/Lux3DButtons.kt`)

| ویجت | مشخصات |
|---|---|
| `Lux3DButton(title, onClick, modifier, subtitle, icon, tone, badge, enabled, busy, depth = 8.dp, tilt = true)` | **لایهٔ ضخامت** زیر + وجه گرادیانی + هایلایت شیشه‌ای + هالهٔ شعاعی + سایه؛ هنگام لمس `translationY = depth`، `rotationX = 9°`، سایه جمع می‌شود، برگشت با فنر (Spring, MediumBouncy, High) |
| `Lux3DIconButton(icon, onClick, size = 46.dp, tone, badge)` | گرد سه‌بعدی با حلقهٔ نور و شمارندهٔ هشدار |
| `Lux3DTile(title, icon, onClick, caption, value, selected, tone, badge)` | کاشی دسته‌بندی با عمق ۷dp و چرخش ۸° هنگام لمس |
| `Lux3DCategoryBar(items, selectedKey, onSelect, captions)` | نوار دسته‌ها (Row با `spacedBy(8.dp)`، هر کاشی `weight(1f)`) |
| `Lux3DGauge(percent, centerText, centerSub, size, tone)` | گیج شعاعی: کمان ۲۷۰° از زاویهٔ ۱۳۵°، حلقهٔ عمق مشکی، قوس گرادیانی، هایلایت شیشه‌ای، نشانگر نقطه‌ای |
| `Lux3DStatusPill(text, level)` · `Lux3DCheckRow(title, level, value, detail)` | نشان وضعیت و ردیف بررسی با چراغ سه‌بعدی |
| `Lux3DDivider` · `Lux3DGroupHeader(title, icon, trailing, status)` | جداکنندهٔ نوری و سرتیتر گروه |
| `enum Lux3DTone { GOLD, GREEN, BLUE, RED, NIGHT }` + `lux3dSkin(tone)` + `lux3dLevelSkin(level)` | پوستهٔ رنگی از پالت فعال |

> انیمیشن‌ها فقط با `animateFloatAsState` + `Spring` و گرافیک فقط با `Canvas` خود Compose —
> **بدون هیچ کتابخانهٔ بیرونی** (APK سنگین نشود).

---

## ۱۱) دو طعم (flavor) و امضا

```kotlin
flavorDimensions += "edition"
productFlavors {
    create("vizitor") { dimension = "edition"; applicationId = "ir.atiran.vizitor"
                        buildConfigField("boolean", "MR_EDITION", "false") }
    create("mreport") { dimension = "edition"; applicationId = "ir.atiran.mreport"
                        versionName = "2.24.0-report"
                        buildConfigField("boolean", "MR_EDITION", "true") }
}
```
* `defaultConfig`: `versionCode = 21820`, `versionName = "2.24.0-direct"`.
* **دام:** `versionName` در بلوک flavor سخت‌نویسی شده؛ با هر نسخه **هر دو** مقدار
  (`defaultConfig` و `mreport`) به‌روز شود، وگرنه APK نسخهٔ انحصاری نام نسخهٔ قدیمی می‌گیرد.
* نسخهٔ debug: `applicationIdSuffix = ".debug"` و `versionNameSuffix = "-debug"`.
* امضا: `keystore.properties` در ریشهٔ پروژه (در گیت **نیست**):
  `storeFile`, `storePassword`, `keyAlias`, `keyPassword`؛ اگر فایل نبود، release بدون امضا
  ساخته شود ولی در CI از Secretها کلید بازسازی می‌شود.
* **کلید ثابت** (alias: `vizitorapp`, CN=Vizitor App Direct, RSA 2048, معتبر تا ۲۰۵۶) —
  تغییر آن باعث می‌شود به‌روزرسانی روی نسخهٔ نصب‌شده ممکن نباشد.

---

## ۱۲) ساخت خودکار (GitHub Actions) و انتشار

ورک‌فلو `build-vizitor-app.yml` روی `push` به `main` و `arena/**` (فقط با تغییر
`vizitor-app/**`) و `workflow_dispatch` اجرا شود. گام‌ها:

1. `actions/checkout@v4` (fetch-depth کامل)
2. `actions/setup-java@v4` (temurin 17) + `gradle/actions/setup-gradle@v4`
3. نمایش نسخهٔ ابزارها
4. **آماده‌سازی کلید امضای پایدار** از Secretها:
   `VIZITOR_APP_KEY_B64` (base64 کلید p12) و `VIZITOR_APP_KEY_PASS` → ساخت
   `keystore.properties` + فایل کلید
5. `./gradlew assembleVizitorDebug assembleVizitorRelease assembleMreportRelease`
   (روی خطا: ذخیرهٔ ۱۲۰ خط آخر لاگ در `vizitor-app/build-failure.txt` + ثبت annotation + کامیت)
6. آماده‌سازی خروجی‌ها در `vizitor-app/dist/` + ساخت `SHA256SUMS.txt` + `BUILD-INFO.txt`
7. گزارش بازرسی APK در `vizitor-app/apk-report.txt`: `aapt2 dump badging`،
   `apksigner verify --print-certs`، شمار dex، **بازرسی رشته‌ای DEX** برای کلاس‌ها و
   رشته‌های کلیدی (نمونهٔ سنجاق‌ها: `.../sqldirect/MaServerProfile`,
   `.../ui/screens/manager/MaServerHealth`, `.../ui/components/Lux3DButtons`,
   `.../ui/screens/manager/MReportHome`, `.../ui/screens/manager/MaManagerScreen` و رشته‌های
   «سلامت سرور»، «بررسی سلامت سرور»، «اتصال و سلامت»، «داده و جداول»)
8. ثبت APK امضاشده در مخزن (`dist/`) مستقل از artifact
9. `actions/upload-artifact@v4`
10. **آماده‌سازی متن ریلیزها در فایل** `release-notes/vizitor.md` و `release-notes/mreport.md`
11. انتشار ریلیز `vizitor-app-direct-<ver>` با `body_path: release-notes/vizitor.md`
    (+ APK، `.sha256`، `SHA256SUMS.txt`، نسخهٔ debug)
12. انتشار ریلیز `manager-reports-<ver>` با `body_path: release-notes/mreport.md`
    (`make_latest: false`)
13. ضمیمهٔ قطعی APKها به هر دو ریلیز + ثبت `vizitor-app/release-assets.txt`

### دام بزرگ CI (حتماً رعایت شود)

> GitHub برای هر «عبارت» (`${{ }}`) در ورک‌فلو سقف **۲۱٬۰۰۰ کاراکتر** دارد و **کل متن
> ریلیزها را هم در همین شمارش می‌آورد**. اگر متنِ توضیحات نسخه‌ها را داخل `body: |` بنویسید،
> با رشد نسخه‌ها بیلد با خطای
> `Invalid workflow file … Exceeded max expression length 21000` رد می‌شود.
> **راه‌حل:** متن را در گامی با heredoc در `release-notes/*.md` بسازید و از
> `body_path:` استفاده کنید. بعد از هر ویرایش، YAML را با یک پارسر تست کنید.

---

## ۱۳) معیارهای پذیرش نهایی (خودت این‌ها را چک کن و گزارش بده)

1. **کامپایل:** هر سه تسک Gradle موفق؛ هیچ خطای `e: file` نباشد.
2. **بسته‌ها:** `ir.atiran.vizitor` و `ir.atiran.mreport` (هر دو `versionCode 21820`)
   کنار هم نصب شوند (نصب دومی، اولی را حذف نکند).
3. **امضا:** `apksigner verify` برای هر دو APK → Verifies (v2؛ کلید یکسان).
4. **یکسانی هش:** `sha256sum -c --ignore-missing vizitor-app/dist/SHA256SUMS.txt` → OK.
5. **پوشیدگی:** در فهرست رشته‌های APK اثری از `37.143.147.19`، `192.168.1.150`، `Atiran2`،
   `AdminAn` نباشد (چون Base64 است، `strings` آن‌ها را پیدا نمی‌کند — این را در گزارش
   بازرسی بنویس).
6. **اتصال واقعی:** روی وای‌فای اداره مسیر داخلی و روی اینترنت همراه مسیر بیرونی به‌طور
   خودکار انتخاب شود؛ با قطع یکی، خودکار سراغ دیگری برود.
7. **ورود کاربر:** با حساب واقعی، ورود موفق + به‌روزرسانی گزارش‌ها + پیام «خوش آمدید <نام>»؛
   با رمز اشتباه پیام روشن و بدون لو دادن اطلاعات.
8. **سلامت سرور:** نمره بین ۰ تا ۱۰۰؛ در شبکهٔ سالم ≥ ۸۵؛ با قطع شبکه نمره پایین و ردیف
   «اتصال» سرخ شود.
9. **انتشار:** دو ریلیز ساخته شود و URLهای دانلود پاسخ دهند
   (`releases/download/<tag>/<file>` → HTTP 302).

---

## ۱۴) دام‌ها و درس‌های آزموده‌شده (اینها را تکرار نکن)

| دام | راه درست |
|---|---|
| `@Composable` روی توابع `LazyListScope` (مثل `homeOverview`) | این توابع **غیر‑Composable** باشند و پالت را داخل `item { }` بخوانند |
| `return@Label` قدیمی پس از تغییر نام کامپوزبل | برچسب را همراه تغییر نام به‌روز کن (مثلاً `return@MaGoldCta` → `return@Lux3DButton`) |
| `weights.sumOf { when(it){...} }` با enum ⇒ `Overload resolution ambiguity` | `weights.map { … }.sum()` با نوع صریح |
| `byteArrayOf(0xFF…)` غیرمجاز؛ `0xFF` در byte | از `intArrayOf` یا `Color(0xFF…)` استفاده کن |
| دست‌کاری پیش‌فرض کل کتابخانهٔ ویجت‌های مشترک | فقط ویجت‌های تازه اضافه کن؛ ویجت‌های موجود را بازنویسی نکن |
| `versionName` فراموش‌شده در flavor | هر بار **هر دو** جای نسخه را به‌روز کن (بند ۱۱) |
| متن ریلیز داخل `body: |` ورک‌فلو | `release-notes/*.md` + `body_path` (بند ۱۲) |
| `git status` گمراه‌کننده در مخزن کم‌عمق/گرافت | قبل از push دوباره `git status --porcelain` بگیر و بعد از rebase محتوا را تأیید کن |
| نبود `import kotlinx.coroutines.launch` | هنگام استفاده از `scope.launch` ایمپورت را چک کن (خطای «Unresolved reference 'launch'») |
| اتصال روی گوشی‌های API 24/25 | `coreLibraryDesugaring` فعال باشد |
| رمز داخل APK | رمز SQL فقط-`SELECT` باشد و در سرور برای همان کاربر دسترسی محدود تعریف شود |

---

## ۱۵) آزمایش دستی گام‌به‌گام (بعد از ساخت)

1. نصب `MReport-2.24.0-direct.apk` → باز کردن «گزارشات مدیر» → خانهٔ M•A Report.
2. صبر کن اتصال خودکار انجام شود؛ چیپ بالا باید «متصل ✓» شود.
3. کارت «ورود کاربر آتیران» → نام کاربری و کلمهٔ عبور واقعی → «ورود و همگام‌سازی».
4. برگهٔ «سلامت سرور» → «بررسی سلامت سرور» → نمره و ردیف‌های چهار دسته را ببین.
5. «داده و جداول» → «نمای کلی» → فهرست جداول واقعی سرور؛ «اتصال جداول» → «تشخیص خودکار جداول».
6. رفتن روی دادهٔ همراه (خاموش‌کردن وای‌فای) → اجرای مجدد → مسیر باید خودکار «اینترنت» شود.
7. نصب `Vizitor-2.24.0-direct.apk` روی همان گوشی → هر دو برنامه باید کنار هم بمانند.

---

## ۱۶) قالب گزارش تحویل (خروجی نهایی تو)

```
✔ پروژه ساخته شد — <نام نسخه> (شمارهٔ ساخت <…>)
• نسخهٔ انحصاری: ir.atiran.mreport — MReport-<ver>-direct.apk — sha256 <…>
• نسخهٔ کامل  : ir.atiran.vizitor — Vizitor-<ver>-direct.apk — sha256 <…>
• لینک دانلود هر دو ریلیز
• چه چیزی ساخته شد (فهرست کوتاه بخش‌ها و امکانات)
• راستی‌آزمایی: نتیجهٔ کامپایل، apksigner، sha256sum، بازرسی DEX، پوشیدگی
• گام‌های استفاده برای کاربر نهایی
• نکات امنیتی و محدودیت‌های باقی‌مانده
```

## ◢ پایان پرامپت

---
---

# پیوست الف — داده‌های قطعی نسخهٔ مرجع (برای مقایسه/بازرسی)

| مورد | نسخهٔ انحصاری | نسخهٔ کامل |
|---|---|---|
| بسته | `ir.atiran.mreport` | `ir.atiran.vizitor` |
| versionName / Code | `2.24.0-report` / ۲۱۹۲۰ | `2.24.0-direct` / ۲۱۹۲۰ |
| فایل | `MReport-2.24.0-direct.apk` | `Vizitor-2.24.0-direct.apk` |
| حجم | ۴۲٬۱۹۰٬۵۰۲ بایت | ۴۲٬۱۹۰٬۴۹۸ بایت |
| SHA-256 | `1aafbaa9212c4b7a9f1e1a5743e39c578d549a2eddf2efb14be2b2bae5917931` | `5d9b610d4a67ac8887dc6a7b9f8a6b87700def9738be4aea5d0a4b21b37f4718` |

```
https://github.com/Companymeelano/Vizitor-Android/releases/download/manager-reports-2.24.0/MReport-2.24.0-direct.apk
https://github.com/Companymeelano/Vizitor-Android/releases/download/vizitor-app-direct-2.24.0/Vizitor-2.24.0-direct.apk
```

شاهدهای بازرسی در مخزن: `vizitor-app/apk-report.txt` · `vizitor-app/release-assets.txt` ·
`vizitor-app/dist/SHA256SUMS.txt` · `vizitor-app/HANDOVER-VIZITOR-APP-2.24.0.md`.

# پیوست ب — پیام کوتاه همراه (برای فرستادن به هوش مصنوعی)

> این سند کامل را بخوان و پروژه را **از صفر** با همین مشخصات بساز. همهٔ نسخه‌های Gradle/Kotlin،
> ساختار فایل‌ها، موتور اتصال چهارحالته، پروفایل مخفی سرور، کوئری‌های فقط-خواندنی،
> کارت «سلامت سرور»، ویجت‌های سه‌بعدی، دو طعم (flavor)، امضا و ورک‌فلو CI در آن آمده است.
> خروجی نهایی: دو APK امضاشدهٔ قابل نصب کنار هم + گزارش تحویل فارسی طبق بند ۱۶.
> کد باید کامپایل‌شدنی باشد؛ هر جا دادهٔ واقعی نیست، **عدد نساز** و متن‌های بند ۸٫۴ را نشان بده.

# پیوست ج — نکتهٔ امنیتی مهم

در این سند رمز SQL داخل خود برنامه است (خواستهٔ پروژه). اگر این سند را جایی منتشر می‌کنید،
**مقدارها را پاک کنید**؛ و روی سرور برای کاربر `AdminAn` فقط دسترسی `SELECT` (و در حالت
ایده‌آل، محدود به IPهای اداره/VPN) تعریف کنید. برنامه به‌هرحال فقط `SELECT` و کاتالوگ
سیستم می‌خواند و هیچ نوشتنی روی سرور انجام نمی‌دهد.
