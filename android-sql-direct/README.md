# Vizitor Android → SQL Server مستقیم — آثار کار (mirror)

این پوشه **آینهٔ کار روی ریپازیتوری هم‌خانوادهٔ `Companymeelano/viz`** است
(شاخهٔ محلی `arena/sql-direct-audit`). چون access push به آن ریپازیتوری برای
این agent وجود ندارد، نسخهٔ مطمئن از آثار در همین جایی که session روی آن
ردیابی می‌شود نگهداری می‌شود.

## فایل‌ها
| فایل | توضیح |
|---|---|
| `AUDIT-CURRENT-ARCHITECTURE.md` | ممیزی کامل معماری فعلی + معماری هدف + نقشهٔ مهاجرت + افتراقات VERIFY |
| `ATIRAN-SCHEMA-EXTRACTED.md` | ۵۵ جدول/نمای Atiran با تمام ستون‌ها/PK/FK (استخراج‌شده از مدل EF واقعی ERP) |
| `sql/00_audit_atiran2.sql` | اسکریپت **فقط‌خواندنی** ممیزی سرور واقعی — **نسخهٔ v2** (خروجی را به agent بفرستید) |
| `sql/01_setup_vizitor_user.sql` | آماده‌سازی غیرتلفیقی سرور: کاربر فقط‌خواندنی `vizitor_android` + فایروال فقط LAN |
| `SqlConnectionManager.kt` | فاز ۲: pool + validation + retry + transaction + StateFlow + پیام فارسی خطا — **اصلاح‌شده ۲۰۲۶-۰۹-۱۸:** این فایل هرگز کامپایل نشده بود و ۱۱ خطای واقعی داشت (۶ کاراکتر تک‌کوتیشن چندحرفی در `jdbcUrl()`، `@Synchronized` روی تابع `suspend`، خطای نوع برگشتی حلقهٔ بی‌پایان در `withConnection`، `.close()` روی مقدار Boolean در `ping`، و `e.message.lines()` روی مقدار nullable). همه رفع شد و فایل با کامپایلر واقعی Kotlin بدون خطا/هشدار کامپایل می‌شود |
| `SecureDbStore.kt` | فاز ۲: ذخیرهٔ امن اطلاعات اتصال (AES-GCM + Android Keystore) |

## ⚠️ چه فایلی را کجا اجرا کنیم (این اشتباه یک‌بار اتفاق افتاده است)

| فایل | کجا اجرا می‌شود | کجا هرگز |
|---|---|---|
| `sql/00` … `sql/07` (`*.sql`) | فقط در **SSMS** روی سرور — کل فایل: `Ctrl+A` بعد `F5` | — |
| `tools/run_audit.bat` | فقط در **ویندوز سرور**: در File Explorer راست‌کلیک → «Run as administrator» (فایل را **اجرا** کن) | در SSMS و در **PowerShell** اجرا **نشود**؛ متنش را کپی/پیست **نکن** (خطاهای `REM: The term 'REM' is not recognized` یعنی متن فایل به‌جای اجرای فایل، در کنسول چسبانده شده) |
| `tools/verify_tsql.py` و `tools/check_sql_columns.py` | فقط با **پایتون** (روی کامپیوتر توسعه): `python3 tools/verify_tsql.py` | در SSMS باز/اجرا **نشود** |
| `patches/*.patch`، `*.kt`، `*.md`، `*.tsv` | ویرایشگر / گیت / Android Studio | در SSMS باز **نشوند** |

اگر فایل پایتون یا batch در SSMS اجرا شود، تب Messages دقیقاً این‌ها را نشان می‌دهد
(هیچ‌کدام خطای دیتابیس نیست):
`Msg 137 Must declare the scalar variable "@echo"` /
`Msg 911 Database 'REM' does not exist` /
`Msg 102 Incorrect syntax near '!'` /
`Msg 103 The identifier that starts with ... is too long. Maximum length is 128`.
یعنی «کدِ پایتون/دستورِ ویندوز دارد به‌عنوان SQL اجرا می‌شود».
اگر متن `.bat` داخل **PowerShell** چسبانده شود، خطاها این‌هاست:
`ParserError: Unexpected token 'off'`، `REM: The term 'REM' is not recognized`،
`setlocal: The term 'setlocal' is not recognized` — یعنی «دستورهای cmd دارند در
PowerShell اجرا می‌شوند»؛ فایل باید اجرا شود، نه کپی.

**راه جایگزین بدون `sqlcmd`:** فایل `sql/03b_bodies_file.sql` همان چهار بدنهٔ
پروسیجر را به‌صورت **result set** برمی‌گرداند (نه `PRINT`)، پس در SSMS با
`Ctrl+Shift+F` (Query → Results To → Results to File) و سپس `F5` کامل داخل یک فایل
ذخیره می‌شود. راهنمای کاملِ سه روش در `tools/README-how-to-run.txt` است
(و همان فایل‌ها به‌صورت یک بستهٔ `Vizitor-audit-files.zip` هم کنار ریپو هست).

## تاریخچهٔ `sql/00_audit_atiran2.sql`
* **v1** → روی سرور خطا داد: `Msg 102, Level 15, State 1, Line 142 — Incorrect syntax near '@pwSql'.`
  علت: کل فایل یک batch واحد بود (بدون `GO`) و همان یک دستور `EXEC sp_executesql STUFF(...)`
  خطای parse می‌داد → طبق قانون SQL Server هیچ‌کدام از بخش‌های اسکریپت اجرا نشد و خروجی خالی ماند.
* **v2** → هر بخش با `GO` جدا شد (خطای یک بخش بقیه را از کار نمی‌اندازد)، بخش
  «الگوی پسورد» با حلقهٔ `WHILE` + `EXEC (@sql)` بازنویسی شد، جدول‌های ناموجود در دیتابیس
  «رد» می‌شوند نه اینکه کرش کنند، و بخش‌های جدید اضافه شد (شبکه/پورت، کشف نام اشیاء).
* **v3 (فعلی)** → دو اصلاح بعد از اجرای واقعی روی سرور:
  - `Msg 208 Invalid object name 'wanted'` در بخش ۰۷: در T-SQL یک CTE فقط تا پایان
    **همان یک دستور** بعدی زنده است؛ دستور دوم که به `wanted` ارجاع می‌داد آن را نمی‌دید.
    الان از table variable (`@wanted`) استفاده می‌شود.
  - **نام دیتابیس واقعی روی سرور: `Meelano`** (نه Atiran2). چک نام سخت‌گیرانه حذف شد:
    اسکریپت هر دیتابیسی که به آن وصل باشید را ممیزی می‌کند، نامش را چاپ می‌کند و
    وجود `dbo.CUSTOMERS` را probe می‌کند تا مطمئن شویم دیتابیس درست است.
  - همهٔ مقایسه‌های نام اشیاء با `COLLATE Latin1_General_CI_AS` → روی دیتابیس با
    collation حساس به بزرگی/کوچکی حروف هم درست کار می‌کند.
* دو باگ پنهان v1 هم با مستندات رسمی مایکروسافت (نه حدس) پیدا و رفع شد:
  `sys.index_columns` ستون `is_primary_key` ندارد (باید به `sys.indexes` join شود) و
  `sys.parameters` ستون `PARAMETER_NAME` ندارد (نام ستون `name` است).
* اعتبارسنجی: هر ۱۵ batch با پارسر T-SQL (sqlglot) parse شد + SQL داینامیک تولیدشده
  شبیه‌سازی و parse شد. (`sqlfluff` در این sandbox حتی روی `SELECT 1 AS c;` خطا می‌دهد → استفاده نشد.)
* محتوای اجرایی فایل کاملاً ASCII است؛ متن فارسی فقط داخل کامنت‌هاست (بی‌اثر روی اجرا).

## تاریخچهٔ نسخه‌های ۰۰ (v4 → v5) — مهم
* **v4** روی سرور این خطا را داد: `Msg 102 ... Incorrect syntax near '<invisible>'`
  علت: v3 فایل را **UTF-8 با BOM** ذخیره کرده بود و SSMS آن بایت نامرئی را داخل اولین
  batch می‌خواند. اکنون هر دو فایل **ASCII خالص و بدون BOM** هستند و ابزار
  `tools/verify_tsql.py` وجود BOM و هر کاراکتر غیر-ASCII (حتی در کامنت) را رد می‌کند.
* **v5 (فعلی)**: تمام خروجی ممیزی حالا با `PRINT` در تب **Messages** چاپ می‌شود
  (به‌صورت خط‌های pipe-separated با برچسب بخش، مثل `02|CUSTOMERS|SHMO|nvarchar|30|NOT NULL|PK`).
  دیگر لازم نیست از grid کپی کنید: کل audit در Messages است.
  (`PRINT` حداکثر ۴۰۰۰ کاراکتر یونیکد در هر فراخوانی می‌پذیرد → متن‌ها در تکه‌های
  ۴۰۰۰ کاراکتری چاپ می‌شوند.)
* **یک باگ واقعی دیگر قبل از اجرای شما گرفته شد**: در §04 یک `)` جا افتاده بود
  (final depth = 1) که روی سرور دقیقاً `Msg 102` می‌داد؛ به ابزار یک چک
  «توازن پرانتزها» و «توازن BEGIN/END و TRY/CATCH» اضافه شد و اسکریپت اصلاح شد.
* نتیجهٔ آخرین اجرای ابزار: `15/15` و `2/2` batch سالم، هر سه دستور داینامیک
  بازسازی و parse شدند، بدون BOM، ASCII خالص → **ALL CHECKS PASSED**.

## فاز ۲ — اتصال UI به لایهٔ SQL (انجام‌شده، 2026-09-18)
یک patch کامل و آماده برای apply روی ریپازیتوری `Companymeelano/viz`:
`patches/phase2-sql-ui-wiring.patch` (۶ فایل، ۶۲۸ خط افزودن)

| فایل | تغییر |
|---|---|
| `app/build.gradle.kts` | افزودن `com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre8` |
| `app/proguard-rules.pro` | keep برای `com.microsoft.sqlserver.jdbc.**` + dontwarn |
| `data/sql/SqlConnectionManager.kt` | **جدید** — pool، retry، transaction، StateFlow، پیام فارسی خطا |
| `data/local/SecureDbStore.kt` | **جدید** — ذخیرهٔ رمزنگاری‌شدهٔ پیکربندی (AES-GCM + Keystore) |
| `VizitorViewModel.kt` | `dbState`/`dbTesting`/`dbConfig` + `testDbConnection()`/`refreshDbState()`/`disconnectDb()`/`clearDbConfig()` |
| `ui/screens/SettingsScreen.kt` | بخش جدید «اتصال مستقیم SQL Server»: فیلدهای سرور/پورت/دیتابیس/کاربر/رمز + دکمهٔ «تست اتصال SQL» + نمایش زندهٔ وضعیت |

نکات پیاده‌سازی:
* مقادیر پیش‌فرض فرم از **ممیزی واقعی** پر شده‌اند: `192.168.1.150`، پورت `1433`،
  دیتابیس `Meelano`، کاربر `vizitor_android`.
* پیکربندی **فقط پس از اتصال موفق** ذخیره می‌شود (نه صرفاً با زدن دکمه).
* رمز عبور با `PasswordVisualTransformation` نمایش داده می‌شود، هرگز Log نمی‌شود و
  فقط رمزنگاری‌شده در Keystore دستگاه می‌ماند.
* هیچ عنصر UI قبلی حذف/بازطراحی نشد؛ فقط یک بخش به صفحهٔ تنظیمات اضافه شد.

وضعیت: **کد نوشته شده ولی کامپایل نشده** (در sandbox نه Android SDK هست و نه JDK) —
اعتبارسنجی انجام‌شده: تعادل ساختاری هر ۴ فایل Kotlin، بررسی وجود همهٔ symbolهای
ارجاع‌داده‌شده، و تطابق امضاهای `SqlConnectionManager`/`SecureDbStore` با مصرف‌کننده‌ها.

## تاریخچهٔ `sql/06_login_probe.sql`
* **v1** → سرور این خطا را داد: `Msg 156, Level 15, State 1, Line 82 — Incorrect syntax
  near the keyword 'user'`. علت: `FROM EMS.user` — کلمهٔ `USER` **کلیدواژهٔ رزرو T-SQL** است
  و باید `[EMS].[user]` نوشته شود. چون کل بدنهٔ اسکریپت در **یک batch** بود، این خطای
  سینتکس همهٔ بخش‌های L1..L6 را از بین برد (فقط سرصفحه چاپ شد).
* **v2 (فعلی)** → هر بخش `GO` جداگانه دارد و همه در یک **جدول موقت** (`#o`) جمع شده و
  در انتها یک‌جا چاپ می‌شوند؛ پس خطای یک بخش، بقیه را از بین نمی‌برد. نام `[EMS].[user]`
  براکت‌گذاری شد، بخش `L6` (همهٔ ستون‌های شبه‌رمز در کل دیتابیس) و `L7` (اشیاء با نام
  login/pass/confirm/auth) اضافه شدند.
* ابزار `tools/verify_tsql.py` حالا **چک کلیدواژه‌های رزرو** دارد: هر نام شیئی که
  براکت‌گذاری نشده باشد و کلیدواژهٔ رزرو باشد → FAIL (خودآزما: همان `EMS.user` قدیمی تشخیص داده شد).

## تاریخچهٔ `sql/01_setup_vizitor_user.sql`
* نام دیتابیس به یک متغیر در بالای فایل منتقل شد (`DECLARE @dbName = N'Meelano'`) و
  ساخت کاربر/نقش با dynamic SQL انجام می‌شود تا مستقل از دیتابیسِ بازِ پنجرهٔ SSMS باشد.
* اگر نام دیتابیس اشتباه باشد، فقط یک پیام واضح چاپ می‌شود و **هیچ تغییری روی سرور انجام نمی‌شود**.
* یک بخش جدید `01_LOGINMODE_CHECK` نشان می‌دهد سرور در حالت احراز هویت ترکیبی
  (LoginMode = 2) هست یا نه — چون ورود کاربر SQL بدون آن ممکن نیست.
* عضویت نقش idempotent است و ردیف `02_ROLE_CHECK` صحت آن را نشان می‌دهد.

## فایل‌های جدید پس از اجرای v5 روی سرور (خروجی واقعی رسید)
| فایل | توضیح |
|---|---|
| `docs/VERIFIED-SCHEMA-Meelano.md` | **اسکیمای تأییدشدهٔ واقعی**: سرور، schemaها، ستون‌های جدول‌های کلیدی، پاسخ ۵ مورد VERIFY، تعداد ردیف‌ها، فلؤ نوشتن با SPها |
| `sql/02_fill_gaps.sql` | ممیزی دوم با **خروجی کوچک**: پورت واقعی TCP + LoginMode، روش تأیید رمز (`PWDCOMPARE`)، بدنهٔ توابع کوتاه لاگین، محتوای جدول‌های تنظیمات و نمونه‌داده‌ها، تعریف viewهای مورد نیاز |
| `sql/03_dump_proc_bodies.sql` | بدنهٔ کامل ۴ پروسیجر کلیدی (`add_sail_pish`, `AddInvoice`, `new_cust`, `FixMojodi`) — ترجیحاً با `sqlcmd -o file` تا فایل ضمیمه شود |
| `tools/verify_tsql.py` | ابزار چک استاتیک (parse هر batch، بازسازی SQL داینامیک، توازن پرانتز/BEGIN-END، رد BOM و غیر-ASCII) |

### یافته‌های کلیدی ممیزی واقعی (2026-09-18)
* سرور SQL Server **2014 Enterprise**، instance پیش‌فرض، دیتابیس **Meelano**،
  collation `SQL_Latin1_General_CP1256_CI_AS` (case-insensitive).
* ماژول موبایل در schema جداگانهٔ **`Hamrah`** است (`Visit`, `TabletCustomer`,
  `PishDaryaft*`, `Device*`) و **همه خالی‌اند (0 ردیف)** → تأیید قطعی «اولین استقرار».
* `new_cust` یک **STORED PROCEDURE** است نه جدول (به همین دلیل در 07 به‌عنوان MISSING آمد).
* **لاگین**: `dbo.sys_users.user_password` از نوع `varbinary(50)` است → رمز در اپ
  بازسازی نمی‌شود؛ تأیید سمت SQL با `PWDCOMPARE` انجام می‌شود (اسکریپت 02 این را probe می‌کند).
* هیچ `vwVizitor*` روی سرور وجود ندارد (لایهٔ PHP نصب نشده).
* پروسیجرهای موجود برای مسیر نوشتن: `dbo.add_sail_pish` (۲۵ پارامتر)،
  `dbo.AddInvoice` (۳۹ پارامتر + `@shpish`)، `dbo.FixMojodi`، `dbo.new_cust` (۵۰ پارامتر).
* قیمت‌گذاری تأیید شد: `CUSTOMERS.group_rdf` → `custgroup.price` (تیر ۱..۵) →
  `forosh_price.forosh1..forosh5` (+ `mp*, pv*`).
* جدول‌های `sailfact_pish` / `subsailfact_pish` **صفر ردیف** → هیچ پیش‌فاکتوری ثبت نشده است.

## فاز ۳ — لایهٔ دادهٔ SQL (شروع) + محافظ «بدون نام حدسی»
| فایل | توضیح |
|---|---|
| `MeelanoDataSource.kt` | **جدید** — منبع دادهٔ فقط‌خواندنی روی Meelano: لاگین با `PWDCOMPARE`، کالا + ۵ سطح قیمت، قیمت به‌ازای تیر، موجودی انبارها، مشتریان مجاز (`sys_cus`)، گروه‌های مشتری با تیر قیمت، هویت ویزیتور، اطلاعات دیتابیس — همه با SQL پارامتری و `queryTimeout` |
| `docs/schema/meelano-columns.tsv` | ۲۰ جدول و ۴۴۱ ستون **تأییدشدهٔ** استخراج‌شده از خروجی واقعی ممیزی (ماشین‌خوان) |
| `tools/check_sql_columns.py` | محافظ: هر SQL در فایل‌های Kotlin/‌SQL را با اسکیمای تأییدشده مقایسه می‌کند و هر ستون ناشناخته را FAIL می‌دهد (با تست خودآزما: تزریق ستون جعلی → شناسایی شد) |
| `sql/05_gaps_small.sql` | ممیزی کوچک (خروجی ~۴۰ خط / ۲.۵KB): ستون‌های `sys_kal`/`sys_anb`، LoginMode، معنای واقعی ستون‌های `active`، تست `PWDCOMPARE`، نمونه‌های کوچک |
| `tools/run_audit.bat` | روی سرور با یک دوبار-کلیک همهٔ ممیزی‌ها را در فایل‌های `out_*.txt` می‌نویسد (دور زدنِ بریدگی تب Messages) |

**یافتهٔ قطعی پورت:** `sys.dm_tcp_listener_states` → `0.0.0.0:1433` و `[::]:1433` (ONLINE).
آن `1434` که در رجیستری دیده شد مربوط به **DAC** (`AdminConnection\Tcp`) است، نه اپ.
پس اتصال اپ: **`192.168.1.150:1433` → دیتابیس `Meelano`**.

## یافته‌های مهم ممیزی بخش ۵ (۲۰۲۶-۰۹-۱۸)
* **باگ واقعی در کد:** ستون‌های `char(1)` این ERP مقدار **`'t'`** دارند نه `'1'`.
  فیلتر `active = '1'` بی‌صدا صفر ردیف برمی‌گرداند. اصلاح شد و محافظ
  `tools/check_sql_columns.py` حالا مقادیر ثابت را هم با
  `docs/schema/meelano-values.tsv` تطبیق می‌دهد (`ACTIVE_CHAR = "t"`).
* **اتصال کاربر به ویزیتور از `sys_vis` است، نه `visitors.UserID`** (که NULL است):
  `sys_users.user_id → sys_vis.UserID → sys_vis.shvis → visitors.vis_rdf`.
* ستون‌های `sys_kal`/`sys_anb`/`sys_use`/`sys_wor`/`systems` تأیید و به فایل اسکیما اضافه شدند.
* دادهٔ واقعی: ۹ گروه مشتری (همه با تیر قیمت ۱)، ۷ مشتری، ۱ ویزیتور، ۱ انبار، ۱ مسیر.
* **ورود هنوز حل نشده:** `DATALENGTH(user_password) = 1` بایت → هش SQL Server نیست و
  `PWDCOMPARE` منطقی نیست. اسکریپت `sql/06_login_probe.sql` شواهد لازم را جمع می‌کند
  (طبقه‌بندی آن بایت، جدول‌های کاندید رمز، و بدنهٔ توابع `SetUserpass`/`GetUser`).

## ورود (Login) — حل شد ✅ (۲۰۲۶-۰۹-۱۸)
از بدنهٔ توابع خودِ ERP روی سرور:
```sql
-- dbo.SetUserpass
set @result=(select convert(varchar(50),user_password) from sys_users where user_id=@UserID)
-- dbo.ChangeUserPassInSalMali  (نحوهٔ نوشتن رمز توسط ERP)
set user_password = CONVERT(varbinary, @PassWord)
```
و دادهٔ واقعی: `DATALENGTH(user_password)=1` با hex `31` (= کاراکتر «1»).
⇒ رمز در این ERP **متن ساده داخل varbinary** است؛ `PWDCOMPARE` هیچ‌وقت جواب نمی‌دهد.
شرط ورود اپ دقیقاً مطابق ERP پیاده شد:
```sql
WHERE user_name = ? AND CONVERT(varchar(50), user_password) = ? AND active = 1
```
* رمز فقط پارامتر است: در SQL الحاق نمی‌شود، در اپ ذخیره/لاگ نمی‌شود، در پیام خطا چاپ نمی‌شود.
* `IsLocked` برگردانده می‌شود تا UI پیام «حساب قفل است» بدهد (نه ردِ بی‌دلیل).
* ⚠️ این ERP رمزها را قابل‌بازیابی نگه می‌دارد (مشکل خود ERP است، نه اپ)؛ تدابیر اپ:
  کاربر SQL مجازِ کم‌دسترسی، فقط LAN، ترجیح TLS، رمزنگاری روی دستگاه با Keystore، و
  عملیات «پاک‌کردن پیکربندی».

### نتیجهٔ اجرای واقعی `07` نسخهٔ v2 روی سرور (۲۰۲۶-۰۹-۱۸) ✅
هر ۸ خط خروجی رسید: `V1|MATCH|user_id=1|user=Admin|role_id=1|active=1|shmo=1`،
`V1b` (نام کاربری درست است)، `V1c|PWDCOMPARE_result=0`، `V2|wrong_password_rows=0`.
⇒ **ورود اپ روی دیتابیس واقعی اثبات شد.** چند یافتهٔ جانبی از همان خروجی:
* `IsLocked=nu` **باگ نمایشی خودِ ما بود، نه چیز عجیب در دیتابیس**:
  `ISNULL(CAST(x AS NVARCHAR(2)), N'null')` نوعش را از آرگومان اول می‌گیرد و `null`
  را به `nu` می‌بُرد. نوع واقعی `bit NULL` است و مقدارش برای هر دو کاربر NULL؛
  یعنی «قفل» فقط با `IsLocked = 1` معنا دارد (خود ERP هم روی آن فیلتر نمی‌کند).
  اپ حالا `CASE WHEN IsLocked = 1 THEN 1 ELSE 0 END AS is_locked` می‌گیرد تا هیچ‌وقت
  NULL را «قفل» تعبیر نکند. ابزار `verify_tsql.py` هم چکِ این الگو را گرفت
  (در `02`, `05`, `06`, `07` رفع شد).
* `security.ConfirmUser` = جدول «ورود به‌جای کاربر دیگر» ERP (۱ ردیف، ستون `P`) —
  اپ به آن دست نمی‌زند. `security.LoginDetails` = تاریخچهٔ ۱۶ ورود (بدون رمز).
* `dbo.sal_mali` فقط ۱ ردیف دارد (`nam_db=Meelano`) ⇒ الان تک‌سال‌مالی است و سوییچ
  سال مالی لازم نیست. `IsAccountingSystemStarted()=0` ⇒ وقتی مسیر نوشتن را وصل
  کردیم باید بررسی شود که پروسیجرها با این وضعیت سند می‌زنند یا نه.

## بخش ۷ — پنج بدنهٔ باقی‌مانده رسید؛ و راز «ویرایش» پیش‌فاکتور (۲۰۲۶-۰۹-۱۸)
بدنهٔ `Edit_sail_pish` (۳۱۳۴)، `FixManCustomer` (۵۹۹)، `UpdateMojodiInventory` (۱۳۲۰)،
`UpdateMojodiInventoryAnbars` (۱۷۹۲)، `UpdateMojodiInventoryAnbarsPS` (۱۷۷۳)،
`VW_InventoryAnbars` (۳۰۵۵)، `AddFromAtiranDetailsForVisitors` (۹۵۵) و
`SelectPriceAndTedvahForushVisitorhaByDate` (۱۰۶۶) رسید. مهم‌ترین یافته:

* **`sailfact_pish.rdf__` شمارهٔ «نسخه» است، نه شمارهٔ سطر.** `Edit_sail_pish` ابتدا
  سطر قدیمی را بازنشسته می‌کند (`active='f'`, `ismodify='t'`)، **همهٔ سطرهای اقلام**
  آن پیش‌فاکتور را هم بازنشسته می‌کند (`update subsailfact_pish set active='f'`)، بعد
  `MAX(rdf__)+1` می‌گیرد و یک سطر سرِ جدید با همان `shfacfo` و نسخهٔ جدید درج می‌کند.
  پس ویوی خودِ ERP روی `(shfacfo, rdf__)` جوین می‌زند و سطر اقلام باید همان `rdf__`
  سرِ فعال را داشته باشد.
* **هیچ‌کدام از این پروسیجرها سطر اقلام درج نمی‌کنند** — نه `add_sail_pish`، نه
  `Edit_sail_pish`. باید پیدا کنیم چه چیزی (پروسیجر یا خودِ کلاینت ERP) سطرهای
  `subsailfact_pish` را می‌نویسد؛ یک دستور مستقیم این را روشن می‌کند (پایین).
* **موجودی هرگز دستی اصلاح نمی‌شود:** سه پروسیجر `UpdateMojodiInventory*` موجودی را
  از دفتر کالا (`ka_act`) بازمی‌سازند:
  `SUM((tedvah*mohvah+tedjoz) × (act_id در (20,22,5,19,18,48,26,85,133) ? −۱ : +۱))`
  و بعد با `floor` و `%` به وِاح/جوز تقسیم می‌کنند (`overal_setting` id=8 برای
  بسته‌بندی، `Inventory_Anbars_PS` فقط وقتی `WithProductionSerial=1`).
* **`FixManCustomer`** ماندهٔ مشتری را از `cust_act` بازمی‌سازد
  (`SUM(act_bed) − SUM(act_bes)` با شرط `isActive`), و اگر `overal_setting` id=117 = ۱
  بود `FixTasvie` را هم صدا می‌زند (تراکنش `a` مخصوص خودش).
* **`VW_InventoryAnbars`** همان چیزی است که ERP به‌عنوان موجودی نشان می‌دهد:
  `MojodiPish_vah/MojodiPish_joz` = موجودی منهای مقداری که پیش‌فاکتورهای **باز**
  (`sh_f=0`, `active='t'`, `Rejected=0`) گرفته‌اند؛ و فقط کالاهای `inventory.active='t'`.
  ⇒ **اپ از همین ویو استفاده می‌کند** (`stock()` عوض شد؛ هم موجودی روی کاغذ را
  می‌دهد هم قابل‌فروش را).

## سؤال دیتابیس — پاسخ این نمونه (۲۰۲۶-۰۹-۱۸)
خروجی `last_write` فقط **یک سطر** داد: `Meelano` (با مقدار NULL، یعنی از آخرین ری‌استارت
سرویس SQL چیزی در آن نوشته نشده). یعنی روی همان instance که SSMS به آن وصل است،
**`Meelano` تنها دیتابیس کاربری است** و `Atiran14050603` روی این سرور وجود ندارد — آن
لیست ستون‌ها از یک **instance/سرور دیگر** آمده. (این را باید روشن کنی: در SSMS آن موقع
به کدام سرور وصل بودی؟)

آن سه لیست ستونی که فرستادی از دیتابیس **`Atiran14050603`** بود، در حالی که همهٔ
بررسی‌های قبلی (ورود، تعداد رکوردها، بدنهٔ پروسیجرها، `sal_mali`) در **`Meelano`**
انجام شده. جدول‌های اقلام پیش‌فاکتور حالا کاملاً معلوم‌اند:

* `dbo.subsailfact_pish` — **۲۶ ستون** (همان جدول در `Meelano` ۲۵ ستون دارد؛ تفاوت
  فقط `PerPromotion` است).
* `dbo.SubSailSefaresh` — ۷ ستون (سطر فاکتور را به سطر سفارش وصل می‌کند).
* `dbo.subsailtemp` — ۵۲ ستون (جدول میانی ERP با `UserID` و فیلدهای سرِ فاکتور).

جزئیات در **`docs/write-path/PRE-INVOICE-LINES.md`**، لیست خام در
`docs/audit-runs/out_10_line_tables.txt` و ستون‌ها در
`docs/schema/atiran14050603-columns.tsv` (عمداً با `meelano-columns.tsv` قاطی نشد تا
وقتی دیتابیس هدف روشن شد، یک‌جا ادغام شود).

⚠️ **کد نوشتن سطرهای اقلام همچنان فعال نیست** — نه به‌خاطر دیتابیس (که این نمونه
روشن شد)، بلکه چون هنوز نمی‌دانیم درج سطرها را چه می‌کند. برای همین یک دستور مستقیم
باقی مانده: جست‌وجوی نام جدول در همهٔ ماژول‌های دیتابیس (پایین در راهنما، راه ۷).

## مسیر نوشتن ERP — بدنهٔ پروسیجرها رسید ✅ (۲۰۲۶-۰۹-۱۸)
هر چهار بدنه کامل رسید (`add_sail_pish` 3118، `AddInvoice` 5850، `new_cust` 3868،
`FixMojodi` 7957) و مرحله‌به‌مرحله در **`docs/write-path/ERP-WRITE-PROCEDURES.md`**
نوشته شده‌اند (لیست پارامترها، لیست ستون‌های `insert` و مقادیر، عیناً از خود بدنه‌ها).
نکات قطعی برای اپ:
* `add_sail_pish`: فقط با `@mod = 1` کار می‌کند، شماره را `max(shfacfo)+1` می‌گیرد،
  `rdf__=1`, `active='t'`, `sh_f=0`, `rejected=0`, `man_gh = customers.man`، و شمارهٔ
  جدید را در `@id_en` (OUTPUT) برمی‌گرداند. **سطرهای `subsailfact_pish` را نمی‌نویسد.**
* `AddInvoice`: سطر `sailfact` را می‌سازد، سپس
  `update sailfact_pish set sh_f,user_f,date_f` (یعنی «شده/نشده» بودن فاکتور دقیقاً با
  `sh_f` مشخص می‌شود)، سپس `Addmaliyat` (اگر مالیات/عوارض ≠ ۰)، سپس `cust_act` با
  `act_id = 20`، و در پایان `FixManCustomer @shmo`.
* `new_cust`: خودش `transaction t1` دارد (`xact_abort on`)، نام تکراری را با
  `raiserror('نام تكراري است',16,1)` رد می‌کند، `cus_image` + `cust_act` + `sys_cus(1,@a,1)`
  می‌نویسد و SHMO جدید را در `@id_en` می‌دهد ⇒ **هرگز داخل تراکنش دیگری صدا زده نشود**.
* `FixMojodi @Shfac,@state`: برای اصلاح موجودی؛ state=1 فاکتور فروش واقعی، state=5/6
  فروش فروشگاهی. فرمول خودش (`mojkavah/mojkajoz` با `mohvah`، `floor` و `%`) همان چیزی
  است که اپ باید به‌جای هر محاسبهٔ دستی به کار ببرد.
* فلگ‌های `overal_setting` که در کد دیدیم: 67 شروع حسابداری، 77/78 تأیید خودکار
  پیش‌فاکتور بعد از درج، 97 روز جابه‌جایی تاریخ فاکتورِ آمده از پیش‌فاکتور، 135 `'Ex'`،
  168 درج نام مشتری در شرح حساب.
* `security.ConfirmUser.P` **bit** است (نه رمز) → در آن جدول چیز محرمانه‌ای نیست.

**باقی‌ماندهٔ کوچک:** بدنهٔ `Edit_sail_pish` (جایی که ERP سطرهای `subsailfact_pish` را
می‌نویسد)، `AddFromAtiranDetailsForVisitors`، `SelectPriceAndTedvahForushVisitorhaByDate`
(قاعدهٔ fallback قیمت) و نام ستون محاسبه‌شدهٔ `VW_InventoryAnbars`.

## یافته‌های اجرای واقعی بخش ۲ (`02_fill_gaps`) — ۲۰۲۶-۰۹-۱۸
* **نقش‌ها:** `Roles` ۶ ردیف دارد: ۱ مدير، ۲ مدير فروش، ۳ مدير حسابداري، ۴ حسابدار،
  **۵ ويزيتور**، ۶ كاربر ⇒ شناسهٔ نقش ویزیتور در این ERP «۵» است.
* **محدودیت‌های ویزیتور صفر است:** `TedadFactorMojazMande = 0` و
  `MablaghMojazMandeJahatFactorha = 0`. معنی صفر (بی‌نهایت یا «هیچ») فقط از منطق
  نوشتن خود ERP قابل تصمیم است ⇒ یک **سؤال پذیرش** برای فاز پیش‌فاکتور.
* ⚠️ **تضاد آدرس سرور:** رجیستری خود سرور `IP1 = 192.168.1.110` را نشان می‌دهد،
  در حالی که در بریف پروژه `192.168.1.150` گفته شده بود. تا وقتی خود سرور جواب
  قطعی ندهد، هیچ‌کدام «تأییدشده» نیست:
  `SELECT local_net_address FROM sys.dm_exec_connections WHERE session_id = @@SPID`
  (این کوئری به چک سریع `run_audit.bat` اضافه شد و در `out_00_quick.txt` می‌آید).
* **تنظیمات شبکه:** `IPAll.TcpPort = 1433` + `ListenOnAllIPs = 1` ⇒ سرور روی همهٔ
  آدرس‌ها پورت ۱۴۳۳ گوش می‌دهد (هم‌خوان با `sys.dm_tcp_listener_states`). `1434` همان DAC است.
* **ویوهای مهم روی سرور** (تعریف واقعی‌شان خوانده شد): `VW_InventoryAnbars` (فرمول
  رسمی موجودی قابل‌فروش: `mojkavah×mohvah + mojkajoz − Σ(TEDVAH×mohvah + TEDJOZ)` با
  شرط `Rejected=0 and active='t' and sh_f=0`)، `VW_CustomerInformation` (معوق‌ها،
  اعتبار باقی‌مانده، `ForoshType = custgroup.price`)، `VisitorInformation` (KPI ویزیتور)،
  `VW_GoalsVisitors`، `vw_customer`/`VW_ListCustomer`، پنج ویوی پیش‌فاکتور
  (`pishfactor_body`, `pishfactors`, `SailFactPish_Details`, `subsailFactPish`,
  `VwListPishfactorhayeTeadNashodeh`)، `VisitInfo`/`Vw_Visit`، `VW_RowDetailsForosh`.
* **فهرست ۲۴ پروسیجر با اندازه‌شان** رسید؛ تازه‌کشف‌شده‌ها: `Edit_sail_pish` (ویرایش
  پیش‌فاکتور)، `SelectPriceAndTedvahForushVisitorhaByDate` (قیمت+تعداد به‌ازای ویزیتور
  و تاریخ)، `ListPishFactor`, `back_sail`, `set_vis_koli`, `GetVisitorPoints`,
  `UpdateMojodiInventoryAnbars`, `FixInventoryPrice`, `t_newcust`/`newcust`.
* **بدنهٔ چهار پروسیجر کلیدی هنوز نرسیده** (بخش ۳/۳ب در پوشهٔ اپراتور اجرا نشد)؛
  `run_audit.bat` حالا حتی بدون فایل `.sql` هم خودش `body_*.txt` را می‌سازد.

## تاریخچهٔ `sql/06_login_probe.sql` (v3)
* **v1** → `Msg 156 ... near the keyword 'user'`: `FROM EMS.user` بدون براکت (کل batch مرد).
* **v2** → هر بخش `GO` جدا + جدول موقت `#o`؛ اما دو بخش با `Msg 8155 No column name was
  specified for column 1 of 'x'` شکست خوردند (الگوی `FROM (SELECT 1) x`).
* **v3 (فعلی)** → آن الگو حذف شد (بدون `FROM`)؛ خلاصهٔ v3 در سرصفحه فایل.
* `sql/07_login_verify.sql` **جدید**: شرط ورود اپ را با رمز واقعی تست می‌کند (فقط
  MATCH/NO MATCH چاپ می‌شود؛ هیچ رمزی چاپ نمی‌شود)، رد شدن رمز غلط را تأیید می‌کند،
  و ستون‌های `security.ConfirmUser`/`LoginDetails` و `dbo.sal_mali` را می‌آورد.

## تاریخچهٔ `sql/07_login_verify.sql` (v1 → v2)
* **v1** → روی سرور شکست خورد: `Msg 137 Must declare the scalar variable "@testUser"`
  در خطوط ۵۵/۶۱/۶۷/۷۰/۷۸ و در پایان فقط `*** no output - please report this ***`.
  علت **اشتباه خود ما بود**: سه متغیر تست بالای یک `GO` `DECLARE` شده بودند و پایینِ همان
  `GO` استفاده می‌شدند؛ در T-SQL متغیر از `GO` عبور نمی‌کند. بنر چاپ شد ولی آن batch
  هیچ خروجی نداد (یعنی بار دیگر «یک batch = همه‌یا‌هیچ» ما را زمین زد).
* **v2 (فعلی)** → ساختار عوض شد: سه مقدار تست داخل جدول موقت `#cfg` هستند و **هر بخش یک
  batch مستقل** است که آن‌ها را از `#cfg` می‌خواند. پس نه یک `GO` می‌تواند متغیر را یتیم
  کند و نه خطای یک بخش، بقیهٔ بخش‌ها را از بین می‌برد. خط اول خروجی می‌گوید چند بخش
  گزارش داده‌اند: `=== sections reported: N of 6 ======`.
* `tools/verify_tsql.py` یک چک جدید گرفت: **دامنهٔ متغیر در هر batch** (`variable scope
  per batch`) — اگر متغیری در batch خودش `DECLARE` نشده باشد → FAIL. خودآزما: همان الگوی
  v1 («DECLARE بالای `GO`، استفاده پایین آن») تشخیص داده می‌شود.
* همان‌جا یک باگ خودِ ابزار هم پیدا و رفع شد: حذف کامنت با regex ساده، `--` داخل رشتهٔ
  متنی را کامنت می‌گرفت و رشته را می‌شکست (خط خروجی با جداکنندهٔ `---`). حالا هر دو ابزار
  کامنت‌شکنِ **رشته‌آگاه** دارند و `check_sql_columns.py` پیشوند `N` رشته‌ها را هم درست
  حذف می‌کند (قبلاً ستون جعلی «N» گزارش می‌کرد).

## تغییرات لازم در `viz` (برای re-apply روی ریپازیتوری اصلی)
`app/build.gradle.kts` → افزودن:
```kotlin
implementation("com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre8")
```
`app/proguard-rules.pro` → افزودن:
```
-keep class com.microsoft.sqlserver.jdbc.** { *; }
-keepclassmembers class com.microsoft.sqlserver.jdbc.** { *; }
```

## بخش ۸ — مسیر نوشتن پیش‌فاکتور پیاده شد (2026-09-18)

* `MeelanoDataSource.kt` حالا **نوشتن** هم دارد، دقیقاً با همان مسیری که خودِ ERP می‌رود:
  1. `{call dbo.add_sail_pish(...)}` با ۲۶ پارامتر موضعی → `@id_en` = شمارهٔ پیش‌فاکتور؛
  2. برای هر قلم یک `INSERT INTO dbo.subsailtemp_pish (...)` با `mod = 1` → تریگر
     `trig_sst_pish` خودش سطر را در `dbo.subsailfact_pish` می‌نویسد و `rdf__` سربرگِ زنده
     را روی آن می‌گذارد؛
  3. تأیید با `preInvoiceLines()` که با جوین درست `(shfacfo, rdf__)` می‌خواند.
* `preInvoiceHealth()` **پیش از هر نوشتن** وجود اشیا و همان ۲۵ ستونِ جدول میانی را چک
  می‌کند؛ اگر دیتابیس نسخهٔ دیگری از ERP باشد، نام دقیقِ چیزِ غایب را می‌گوید (بدون حدس).
* `retirePreInvoice()` هم هست (همان `active='f'`, `ismodify='t'` که `Edit_sail_pish` با
  نسخهٔ قبلی می‌کند) ولی **تا تأیید کارفرما در مسیر خودکار استفاده نمی‌شود**.
* تحلیل کامل: `docs/write-path/IMPLEMENTATION-pre-invoice.md` · گزارش نهایی ۱۴بندی:
  `docs/FINAL-REPORT.md`.
* ابزار `check_sql_columns.py` دو باگ واقعی گرفت و رفع کرد: (۱) `INSERT`/`UPDATE` هیچ‌وقت
  اعتبارسنجی نمی‌شدند (فقط `FROM/JOIN` دیده می‌شد) — حالا هدفِ DML هم نگاشت می‌شود؛
  (۲) الگوی «نام‌های بعد از کاما = alias» روی `UPDATE ... SET a=1, b=2` هم اعمال می‌شد و
  ستونِ غلط را از چشم ابزار پنهان می‌کرد — حالا فقط برای `SELECT` فعال است. هر دو در
  `--selftest` تست دارند.

## ۹) جریان اتصال در برنامه (نسخهٔ ۱.۳ — پیاده‌شده)

هدف: کاربر خودش دیتابیس را انتخاب کند و هیچ دیتابیسی اجباری نباشد.

| گام | در برنامه |
|---|---|
| ۱ | انتخاب روش اتصال: **از اینترنت (آی‌پی اختصاصی)** یا **شبکهٔ داخلی** — پورت پیش‌فرض **۱۴۳۳** |
| ۲ | وارد کردن نام کاربری و کلمهٔ عبور ورود به SQL Server |
| ۳ | دکمهٔ **«تأیید و دریافت لیست دیتابیس‌ها»** → `listDatabases()` با همان کاربر/رمز به `master` وصل می‌شود و فقط `sys.databases` را می‌خواند |
| ۴ | انتخاب دیتابیس برنامه از میان چیپ‌ها |
| ۵ | **«اعمال تنظیمات و تست اتصال»** → اتصال تست و پیکربندی رمزنگاری‌شده ذخیره می‌شود |
| ۶ | ورود ویزیتور با نام کاربری/کلمهٔ عبور خودش در `dbo.sys_users` و ثبت پیش‌فاکتور |

### کد افزوده‌شده

| فایل | افزوده |
|---|---|
| `SecureDbStore.kt` | نگه‌داری هر دو نشانی (اختصاصی و داخلی) + این‌که کدام فعال است، داخل همان بلوک رمزنگاری‌شدهٔ AES-GCM؛ بازنویسی با توابع `currentJson()/writeJson()` |
| `SqlConnectionManager.kt` | `listDatabases(DbSettings)` — فقط‌خواندنی، به `master`، پایان اتصال در `finally` |
| `VizitorViewModel.kt` | `dbHostExternal` / `dbHostLocal` / `dbUseExternal`، `setDbAddresses()`، `activeDbHost()`، `dbOptions`، `dbListLoading`، `loadDatabases()` |
| `SettingsScreen.kt` | چیپ انتخاب روش اتصال، دو فیلد نشانی، دکمهٔ دریافت لیست، چیپ‌های دیتابیس، دکمهٔ اعمال تنظیمات و تست اتصال |

پچ آماده: `patches/phase3-database-picker.patch` — تفاوت کامل کد اندروید از کامیت `5346510` تا آخرین وضعیت
(جریان «بیرون شبکه اول»، نگه‌داری هر دو نشانی، کادر تایپ دستی دیتابیس، دکمه‌های تأیید/تست و کارت «دربارهٔ سامانه»).
کد کامل و نهایی همیشه در همین پوشه (`android-sql-direct/*.kt`) است.

### شناسنامهٔ سازنده در برنامه (نسخهٔ ۱.۳)
* سرصفحهٔ همهٔ فایل‌های `android-sql-direct/*.kt`: «Developed by Milad Yaghoobi — Meelano Studio Design».
* در تب **تنظیمات**، بخش تازهٔ **«دربارهٔ سامانه»** اضافه شد:
  «سامانهٔ ویزیتور — اتصال مستقیم و امن به SQL Server (پورت ۱۴۳۳)» +
  «طراحی و برنامه‌نویسی: میلاد یقوبی (Milad Yaghoobi)» +
  «گروه نرم‌افزاری: Meelano Studio Design».
* کادر «نام دیتابیس انتخاب‌شده» همیشه باز است: حتی اگر فهرست سرور گرفته نشود
  (سرور خاموش، رمز اشتباه، بیرون از شبکه) می‌توانید نام دیتابیس را **دستی تایپ** کنید و
  با «اعمال تنظیمات و تست اتصال» همان نام آزمایش شود — هیچ دیتابیسی اجباری نیست.

### کارت «دربارهٔ سامانه» — نسخهٔ برجسته (سه‌بعدی)
کارت شناسنامه در تب تنظیمات با **گرادیان بنفش‌ـ‌شیشه‌ای**، **لبهٔ نورانی**، **تمبر گرد با درخشش**
و خط جداکنندهٔ طلایی‌رنگ بازنویسی شد: نام سازنده «میلاد یقوبی (Milad Yaghoobi)» و گروه
«Meelano Studio Design» با رنگ تأکید (NeonPurple) و نسخهٔ سامانه در تیتر کارت.
