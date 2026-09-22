# گزارش نهایی — مهاجرت اپ ویزیتور به SQL Server مستقیم

نسخهٔ این سند: 2026-09-21 · وضعیت کلی: **مسیر ثبت پیش‌فاکتور از سبد خرید به SQL Server واقعی متصل شده است؛ Build نهایی فقط به دلیل عدم دسترسی این محیط به دانلود Gradle روی خود دستگاه قابل اجرا نیست.**

علامت‌ها: ✅ کامل · 🟡 انجام شده و منتظر تأیید داده است · ⛔ تا رسیدن یک مقدار واقعی متوقف

---

## ۱) فایل‌های جدید / تغییرکرده

**نکتهٔ داده:** مسیرهای عملیاتی برنامه دیگر از `SeedData` ساختگی برای داشبورد/گردش حساب/چک‌ها استفاده نمی‌کنند؛ دادهٔ آزمایشی از مسیر نمایش واقعی حذف شده است.

| فایل | نقش |
|---|---|
| `app/src/main/java/.../data/sql/SqlConnectionManager.kt` | استخر اتصال + وضعیت اتصال + تراکنش |
| `app/src/main/java/.../data/sql/MeelanoDataSource.kt` | همهٔ کوئری‌ها (خواندن + نوشتن) |
| `app/src/main/java/.../data/local/SecureDbStore.kt` | ذخیرهٔ امن تنظیمات اتصال |
| `app/src/main/java/.../ui/screens/SettingsScreen.kt` | صفحهٔ تنظیمات SQL (بدون تغییر ظاهر) |
| `app/src/main/java/.../VizitorViewModel.kt` | اتصال به لایهٔ جدید |
| `app/build.gradle.kts`, `app/proguard-rules.pro` | درایور JDBC + قواعد نگه‌داشتن کلاس‌ها |
| `patches/phase2-sql-ui-wiring.patch` | همین مجموعه به‌صورت یک patch قابل اعمال روی شاخهٔ `2331c90` |

## ۲) معماری

```
UI (Compose) → ViewModel → Repository → MeelanoDataSource → SqlConnectionManager → TDS 1433 → Meelano
```

* هیچ منطق تجاری در UI نیست؛ صفحه‌ها همان صفحه‌های فعلی‌اند (بدون بازطراحی).
* هر تابع `suspend` و روی `Dispatchers.IO` — هیچ کوئری‌ای روی ترد UI اجرا نمی‌شود.
* استخر اتصال با سقف ۴ و اعتبارسنجی ۵ ثانیه؛ بازگشت اتصال در `finally` (بدون leak).
* همهٔ مقادیر پارامتری؛ هیچ رشته‌ای به SQL چسبانده نمی‌شود.

## ۳) جدول‌ها (خواندن)

`dbo.sys_users`, `dbo.sys_vis`, `dbo.visitors`, `dbo.sys_cus`, `dbo.CUSTOMERS`,
`dbo.custgroup`, `dbo.sys_anb`, `dbo.inventory`, `dbo.inventory_anbars`, `dbo.anbars`,
`dbo.forosh_price`, `dbo.kagroup`, `dbo.VW_InventoryAnbars`, `sailfact_pish`,
`subsailfact_pish`, `cust_act`, `sal_mali`.

## ۴) Stored Procedure‌ها

| SP | وضعیت |
|---|---|
| `dbo.add_sail_pish` | ✅ فراخوانی پیاده شده (۲۶ پارامتر، موضعی، `@id_en` خروجی) |
| `dbo.Edit_sail_pish` | 🟡 خوانده و مستند شده؛ در کد فقط برای «کنار گذاشتن پیش‌فاکتور نیمه‌کاره» از الگویش استفاده شده |
| `dbo.new_cust` | ⛔ فراخوانی نوشته نشده — چند مقدار امضایش (مثل `@act_bedbes`, `@sh_i_m`) باید از یک ثبت واقعی گرفته شود |
| `dbo.FixManCustomer`, `dbo.AddInvoice`, `dbo.FixMojodi`, `dbo.UpdateMojodiInventory*` | 🟡 خوانده و مستند؛ در مسیر پیش‌فاکتور لازم نیستند، در فاکتورشدن لازم‌اند |

## ۵) توابع

`dbo.IsAccountingSystemStarted()`, `dbo.what_date()`, `dbo.cal_gain()`, `dbo.dif_date()`,
`dbo.CalcDetailsPishfactor()` — همه فقط مستند شده‌اند (سرور خودش صدایشان می‌زند؛ اپ
نیازی به فراخوانی مستقیم ندارد).

## ۶) نگاشت API → SQL

| اندپوینت فعلی | معادل SQL | وضعیت |
|---|---|---|
| `POST action=login` | `SELECT ... FROM dbo.sys_users WHERE user_name=? AND CONVERT(varchar(50),user_password)=? AND active=1` | ✅ |
| `GET action=ping` | `SELECT DB_NAME(), SERVERPROPERTY('ProductVersion'), COUNT(*) FROM dbo.CUSTOMERS` | ✅ |
| `GET action=catalog` | `inventory ⋈ forosh_price` + `dbo.VW_InventoryAnbars` | ✅ |
| `GET action=customers` | `sys_cus ⋈ CUSTOMERS ⋈ custgroup` | ✅ |
| `GET action=sal_mali` | `dbo.sal_mali` | 🟡 ستون‌های گزارش قدیمی باید با همین جدول تطبیق داده شود |
| `POST action=submit_invoice` | مسیر UI اکنون مستقیم `add_sail_pish` + `INSERT dbo.subsailtemp_pish (mod=1)` را اجرا می‌کند | ✅ مسیر عملیاتی پیاده‌سازی و بازخوانی/کنترل شده؛ مقادیر بدون شاهد با fallback خنثی محافظه‌کارانه پر می‌شوند |
| `POST action=submit_customer` | `dbo.new_cust` | ⛔ منتظر مقادیر واقعی |

## ۷) تنظیمات SQL Server

* SQL 2014، کامپتیبیلیتی ۱۲۰، کالِیشن فارسی؛ TDS روی `0.0.0.0:1433` (تأییدشده از رجیستری سرور).
* توصیه: TLS روی 1433 (یا `encrypt=true;trustServerCertificate=false` بعد از نصب گواهی) — در حال حاضر اتصال داخلی LAN است.
* `max degree of parallelism` و سایز حافظه دست‌نخورده می‌ماند (اپ فقط SELECT/EXEC سبک می‌زند).

## ۸) فایروال

* فقط پورت **1433 TCP** برای رنج LAN (مثلاً `192.168.1.0/24`) باز شود — نه اینترنت.
* دستور NETSH در انتهای `sql/01_setup_vizitor_user.sql` چاپ می‌شود.
* دسترسی از بیرون فروشگاه فقط با VPN (توصیه) یا port-forward موقت؛ هر دو خارج از v1.

## ۹) تنظیمات اندروید

* صفحهٔ تنظیمات: IP/DB/کاربر/رمز SQL (ذخیرهٔ امن، بدون هیچ رمزی در کد).
* مقدار پیش‌فرض: `192.168.1.150` / `1433` / دیتابیس `Meelano`.
* کاربر SQL اختصاصی `vizitor_android` (بدون استفاده از `AdminAn`).
* نمایش وضعیت اتصال + دکمهٔ تست اتصال + پیام خطای روشن در قطعی شبکه.

## ۱۰) دسترسی‌های SQL

اسکریپت `sql/01_setup_vizitor_user.sql` اکنون به‌صورت idempotent کاربر `vizitor_android` را در `db_datareader` قرار می‌دهد و فقط این دو مجوز نوشتن را می‌دهد:
`GRANT EXECUTE ON dbo.add_sail_pish TO vizitor_android` و
`GRANT INSERT ON dbo.subsailtemp_pish TO vizitor_android`.
هیچ UPDATE/DELETE/DDL ای به کاربر اپ داده نمی‌شود و خود اسکریپت نیز نتیجهٔ هر دو مجوز را با `HAS_PERMS_BY_NAME` گزارش می‌کند.

## ۱۱) فهرست خطاها و رفتار اپ

| حالت | رفتار |
|---|---|
| قطعی شبکه / خاموشی SQL | پیام فارسی «اتصال به سرور برقرار نیست» + دکمهٔ تلاش مجدد؛ بدون crash |
| رمز/کاربر غلط | «نام کاربری یا رمز نادرست است» (بدون افشای جزئیات) |
| کمی موجودی | پیام روی همان قلم، اجازهٔ ادامه با هشدار (مطابق رفتار فعلی اپ) |
| نبود یک شیء/ستون ERP | `preInvoiceHealth()` پیش از نوشتن، نام دقیق شیء/ستونِ غایب را می‌گوید |
| خطا در میان اقلام | توقف، برگرداندن شمارهٔ پیش‌فاکتور، تلاش مجدد؛ «کنار گذاشتن» فقط با تأیید کارفرما |
| قطعی وسط ثبت | idempotency سمت اپ (`client_invoice_id` در Room) تا ثبت تکراری رخ ندهد |

## ۱۲) تست‌های انجام‌شده

* ✅ `tools/verify_tsql.py` — همهٔ اسکریپت‌های SQL: ALL CHECKS PASSED (شامل تست دامنهٔ متغیر بین GOها).
* ✅ `tools/check_sql_columns.py` — همهٔ کوئری‌های Kotlin با شِمای واقعی مقایسه شد؛ selftest خودش هم INSERT/UPDATE را می‌گیرد. نتیجه: NO UNKNOWN IDENTIFIERS.
* ✅ تست زندهٔ ورود روی سرور (`07_login_verify.sql` → `V1|MATCH`).
* ✅ صحت `add_sail_pish` و تریگرها: از متن واقعی روی سرور (نه حدس).
* ⛔ تست نوشتن واقعی: نیاز به بند ۱۳.

## ۱۳) مواردی که فقط روی سرور واقعی قابل تست‌اند

1. **کالیبراسیون اختیاری پنج مقدار سربرگ** (`ted_rooz`, `ph_kh`, `mod_darsad_vis`, `rdf_sarbarg`, `gainall`) از یک پیش‌فاکتور واقعی. اگر برای همان ویزیتور پیش‌فاکتور فعال وجود داشته باشد، کد همین مقادیر را از آن می‌خواند؛ در غیر این صورت `ted_rooz` از `custgroup` گرفته می‌شود و فیلدهای بدون شاهد با مقادیر خنثی/محافظه‌کارانه (`0`، `1` یا `30`) پر می‌شوند. این fallback برای جلوگیری از حدس‌زدن قرارداد تجاری ERP است و بعداً با یک نمونهٔ واقعی قابل دقیق‌تر شدن است.
2. **تأیید دیتابیس مقصد** با `sql/09_compare_databases.sql` تا مشخص شود دیتابیس انتخاب‌شده همان دیتابیس زندهٔ ERP است.
3. تأیید اینکه `subsailtemp_pish` و `trig_sst_pish` روی دیتابیس مقصد همان نسخهٔ مستند هستند.
4. تست login و کاتالوگ با کاربر `vizitor_android` روی شبکهٔ واقعی.
5. یک ثبت آزمایشی کامل و کنترل آن در لیست پیش‌فاکتورهای ERP.
6. Build روی Windows با اینترنت یا Gradle cache موجود و نصب APK روی حداقل یک گوشی کوچک و یک گوشی بزرگ.

## ۱۴) نصب و تحویل

1. `sql/01_setup_vizitor_user.sql` روی سرور (ساخت کاربر اپ + دسترسی‌ها).
2. اجرای دستور NETSH چاپ‌شده در CMD سرور (باز کردن 1433 فقط برای LAN).
3. نصب اپ و پر کردن صفحهٔ تنظیمات (IP/DB/کاربر/رمز).
4. «تست اتصال» → سبز شدن چراغ وضعیت.
5. ثبت یک پیش‌فاکتور آزمایشی و کنترل آن در ERP.
6. تحویل: همین سند + `docs/write-path/*` + `patches/phase2-sql-ui-wiring.patch`.
