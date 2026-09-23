# Vizitor — ممیزی کامل معماری فعلی (قبل از تغییر)

**موضوع:** بازطراحی اتصال `Android → HTTPS API (PHP/IIS) → SQL Server` به `Android → SQL Server (TCP 1433)`
**قاعدهٔ این ممیزی:** هیچ حدسی دربارهٔ نام جدول/ستون/SP انجام نشده است. همهٔ نام‌ها یا از سورس کد (PHP / Kotlin / C#) یا از مدل EF داخل `AtiranLocalServices.dll` استخراج شده‌اند. مواردی که در سورس موجود نبودند، صریحاً در بخش «افتراقات» علامت **VERIFY** خورده‌اند.

---

## ۱. معماری فعلی (As-Is)

```
┌────────────────────────────┐        ┌──────────────────────────────┐        ┌─────────────────────┐
│ Android (Compose, v2.17.2) │ HTTPS  │ PHP/IIS Web Service          │ TDS    │ SQL Server 2014     │
│                            │ ◄────► │ (viz/server/web/index.php)   │ ◄────► │ DB: Atiran2         │
│ Room (offline-first)       │ JSON   │ PDO sqlsrv, API-Key+Bearer   │ :1433  │ + Vizitor* tables   │
│ Retrofit + OkHttp + Gson   │        │                              │        │ + vwVizitor* views  │
│ WorkManager (sync)         │        │                              │        │                     │
└────────────────────────────┘        └──────────────────────────────┘        └─────────────────────┘
```

### ۱.۱ آنندروید — فهرست کامل اجزا

| طبقه | فایل | نقش |
|---|---|---|
| Activity | `MainActivity.kt` | تک‌Activity؛ تمام UI با Compose Navigation |
| Application | `VizitorApp.kt` | init Room + AuthStore + ChatPrefs + ChequeStore + برنامه‌ریزی SyncWorker + seed دمو (فقط debug) |
| ViewModel | `VizitorViewModel.kt` | **تک** ViewModel برای کل اپ |
| Repository | `data/repository/VizitorRepository.kt` | **مخزن اصلی — offline-first**: خواندن همیشه از Room؛ نوشتن ابتدا Room، سپس ارسال به سرور |
| Repository | `data/repository/SettingsRepository.kt` | DataStore؛ `ServerConfig` (IP، پورت HTTP، **پورت 1433**، apiPath، apiKey، https، workerUrl، geminiModel، autoSync) |
| Data Source (محلی) | `data/local/AppDatabase.kt` | Room؛ جدول‌ها: `products`, `customers`, `invoices`, `invoice_items`, `sal_mali_history`, `cart_items`, `chat_messages` |
| Data Source (محلی) | `data/local/Daos.kt` | DAOها (products/customers/invoices/cart/salMali/chat) |
| Data Source (محلی) | `data/local/Entities.kt` | Entityها + `TopProduct` |
| Data Source (محلی) | `data/local/AuthStore.kt` | توکن نشست با **AES-GCM روی Android Keystore**؛ deviceId = ANDROID_ID؛ اعتبارسنجی UTC |
| Data Source (محلی) | `data/local/ChatPrefs.kt` | پروفایل و تنظیمات چت |
| Data Source (محلی) | `data/local/ChequeStore.kt` | چک‌های پیگیری |
| Data Source (محلی) | `data/local/SeedData.kt` | دادهٔ دمو — **فقط در build debug** |
| Data Source (دور) | `data/remote/RetrofitClient.kt` | OkHttp (connect 20s / read 30s) + Gson |
| Data Source (دور) | `data/remote/ApiService.kt` | اینترفیس Retrofit (۷ روش — جدول ۳) |
| Data Source (دور) | `data/remote/ApiModels.kt` | DTOها: `ApiEnvelope<T>`, `LoginDto`, `PingDto`, `ProductDto`, `CustomerDto`, `SalMaliRowDto`, `InvoiceHeaderRequest`, `InvoiceLineRequest`, `NewCustomerRequest/Response` |
| Sync | `data/sync/SyncWorker.kt` | WorkManager دوره‌ای (فقط روی شبکه موجود)؛ ارسال فاکتورهای pending + دریافت کاتالوگ/مشتریان |
| AI | `ai/GeminiAssistant.kt` | Gemini از طریق Cloudflare Worker (آدرس از Settings) |
| Share | `data/share/InvoiceShare.kt` | به‌ اشتراک‌گذاری فاکتور |
| UI (10 صفحه) | `ui/screens/{Splash,Login,Dashboard,Customers,Catalog,Cart,Reports,Scanner,Settings,Chat}Screen.kt` | صفحات فعلی |
| UI (اجزا) | `ui/components/{Charts,Glass,Luxury,MiniRouteMap,Tilt3D,VoiceSearch,Widgets}.kt` | کامپوننت‌ها |
| ابزار | `util/{Format,Money}.kt`, `perf/VizitorPerf.kt` | قالب‌بندی/پول + شناسای قدرت دستگاه |

**Gradle/Build:** `compileSdk 35`, `minSdk 24`, `targetSdk 35`, `versionCode 21702 (2.17.2)`, Kotlin 17, Compose BOM 2024.09, Room 2.6.1 (KSP), WorkManager 2.9.1, Retrofit 2.11 + OkHttp 4.12 + Gson 2.11, DataStore 1.1.1, CameraX 1.3.4 + ML Kit barcode 17.3, desugaring 2.1.3 (java.time روی API 24/25), release: minify+shrink+proguard.

**Permissionها:** `INTERNET`, `ACCESS_NETWORK_STATE`, `CAMERA`, `ACCESS_FINE/COARSE_LOCATION`, `POST_NOTIFICATIONS`, `VIBRATE` — **هیچ permission جدیدی برای اتصال مستقیم SQL لازم نیست** (TCP روی INTERNET است).

**Connection Stringها:** امروز اپ **اتصال مستقیم DB ندارد**؛ فقط `baseUrl` (HTTP) + `apiKey` از DataStore. نکتهٔ مهم: `ServerConfig.dbPort (1433)` **از قبل در تنظیمات وجود دارد** و فقط نمایشی بوده — پس ساختار «ورود IP سرور DB + پورت» از قبل در اپ هست و ما آن را حرفه‌ای‌تر می‌کنیم.
`usesCleartextTraffic=true` + `network_security_config.xml` برای شبکهٔ داخلی HTTP.

### ۱.۲ سرور PHP/IIS — فهرست کامل

- `index.php` — روتر ۸ اکشن (جدول ۳)؛ لاگ درخواست در shutdown (جدول `VizitorApiLog`)
- `lib/db.php` — PDO `sqlsrv` (LoginTimeout=5، Encrypt/TrustServerCertificate از config)
- `lib/auth.php` — **دو لایه**: `X-Api-Key` (همهٔ endpointها) + `Authorization: Bearer <token>` (SHA-256 توکن در جدول `VizitorTokens`؛ TTL 720 دقیقه؛ قفل ۵ بار تلاش / ۱۵ دقیقه در `VizitorLoginAttempts`)
- `lib/repo.php` — تمام کوئری‌ها (بخش ۳)
- `lib/http.php` — کمکی‌های HTTP/JSON
- `config.sample.php` — تنظیمات: DB (host/port/name/user/pass/encrypt)، `api_key`، `token_ttl_minutes`، `max_login_attempts`، `strict_totals` (true)، `decrement_stock` (false)، `write_to_erp` (false)، `log_requests`

### ۱.۳ جدول‌ها و Viewها — سه گروه مجزا

**الف) Vizitor** (جدول‌های اختصاصی نصب فعلی — از `server/sql/01_schema.sql` و `repo.php`):
`VizitorUsers`(Id, Username, PasswordHash, VisitorCode, DisplayName, Role, IsActive, LastLoginAt) · `VizitorTokens` · `VizitorLoginAttempts` · `VizitorSequences` · `VizitorSalesHeader`(ClientInvoiceId, InvoiceNo, CustomerId, CustomerName, GrossAmount, Discount, FinalAmount, SignatureBase64, Note, VisitorUserId, DeviceId, CreatedAtMs) · `VizitorSalesLines` · `VizitorCustomerRequests`(…, DedupeKey, Status) · `VizitorApiLog` + جداول فیل `VizitorProducts/VizitorCustomers/VizitorSalMali` + **نماهای قراردادی `vwVizitorProducts/vwVizitorCustomers/vwVizitorSalMali`** (در ریپازیتوری فقط «قالب» هستند؛ نام جدول/ستون واقعی باید دستی جایگزین شده باشد — تعریف واقعی روی سرور شما فقط با `00_audit_atiran2.sql` مشخص می‌شود).

**ب) Atiran (ERP)** — ۵۵ شیء (جدول/نما) با **تمام ستون‌ها** از مدل EF استخراج شده:
→ فایل کامل: [`ATIRAN-SCHEMA-EXTRACTED.md`](ATIRAN-SCHEMA-EXTRACTED.md) (سورس: `AtiranLocalServices.dll` + `DataAccess/*.cs` در `AtiranHamrah.zip`)

کلیدی‌ترین‌ها برای Vizitor:

| شیء | PK | ستون‌های کلیدی (اثبات‌شده) |
|---|---|---|
| `CUSTOMERS` | `SHMO` | MONAME, code, addre, tell1, tell2, cell, active, cred (اعتبار), man (مانده), group_rdf, rdf_city, rdf_region, vis_rdf, defi_vis, black_list, Lat, Lng, … (59 ستون) |
| `custgroup` | `group_rdf` | group_name, price (سطح قیمت!), ted_rooz, AccType, Active, PerGain |
| `CITYS` / `Province` / `regions` | RDF / ProvinceID / rdf_region | name, Lat, Lng + FK زنجیره‌ای (city→province, region→city) |
| `inventory` | `shka` | naka, coka, group_rdf, vahsanj, mojkavah, mojkajoz, buyjoz, active, pure_buy_price, buy_price, inventory_price, black_list, ExpirationDate, WithProductionSerial, … (51 ستون) |
| `kagroup` | `group_rdf` | group_name, CanNegative, ParentGroupRdf, GroupLevel, hasPic, Active |
| `forosh_price` | `shka` | **forosh1..forosh5** (پنج سطح قیمت), mp1..mp5, pv1..pv5, group_rdf, active, MinPrice, MaxPrice — FK→inventory |
| `prizePercent` | RowId | شکاندا/ازتا تعداد و قیمت + Percent + RdfCustGroup/Province/City/Region/Masir (تخفیف پلکانی) |
| `anbars` | rdf_anbar | name, addre, anbardar, Active, Base |
| `inventory_anbars` | rdf_anbars | shka, mojkavah (کسری), mojkajoz (جز), name, tedbastebandi — FK→inventory, anbars |
| `VW_InventoryAnbars` | shka | نما موجودی زنده: shka, rdf_anbars, name, mojkavah, mojkajoz, MojodiPish_vah, MojodiPish_joz |
| `sailfact` | rdf__ | shfacfo, date, shmo, vis_rdf, sumlineall, all, tafif, done_date, active, sysid, userid, … (66) — FK→CUSTOMERS |
| `subsailfact` | rdf__ | shfacfo, SHKA, rdf_anbar, TEDVAH, TEDJOZ, VAHPRICE, JOZPRICE, LINESUM, PERTAFIF, ProductionSeriesID, … (37) |
| `sailfact_pish` | rdf__ | shfacfo, date, shmo, vis_rdf, sumlineall, all, tafif, taeid, VisitID, Rejected*, … (51) — FK→CUSTOMERS, Visit |
| `subsailfact_pish` | rdf__ | shfacfo, SHKA, TEDVAH, TEDJOZ, VAHPRICE, LINESUM, ISRET, … (25) |
| `PishDaryaft` | GhnoPishDaryaft | Naghd, Date, Shmo, Shfac, VisitorID, SysID, TaeidHesabdari*, Rejected* + جداول جانبی GetCheck/MultiFactor/Pos |
| `visitors` | vis_rdf | vis_name, vis_cell, eteb, is_supervisor, supervisor_rdf, active, **Username, Password, UserID** (3 ستون آخر = اتصال به سیستم کاربر) |
| `sys_vis` | SysID | shvis (ویزیتور), UserID — **چه ویزیتوری در چه سیستمی فعال است** |
| `sys_cus` | SysID | Shmo (مشتری), UserID — **چه مشتری‌هایی برای چه کاربری/سیستمی مجاز است (Visitor Scope!)** |
| `sys_kal` / `sys_anb` | SysID | همان الگو برای کالا/انبار |
| `masir` / `MasirDay` | rdf_masir / ID | مسیر ویزیتور + برنامهٔ روزانه (تاریخ، شهر، منطقه، مسیر) |
| `vis_goals` / `VW_GoalsVisitors` | rdf / rdf | اهداف ویزیتور (مبلغ/تعداد × گروه مشتری/کالا + جغرافیا) — نما شامل نام‌هاست |
| `cust_act` | rdf_ | سابقهٔ اسناد/تخفیف مشتری (shmo, date, act_bes/bed/dis, ghno, DocNumber, …) |
| `TabletCustomer` | id | **مکانیزم موبایل/تبلت موجود**: vis_rdf, shmo, name, cell, address, metraj*, DoneSave, IsEdit — آفلاین-first قبلی |
| `Device*` | — | Device, DeviceLocation, DeviceMessages, DeviceSettings (اتصال دستگاه‌ها) |
| `osystems` | rdf_system | سیستم‌ها (نام، آدرس، AnbarRdf, Active) |
| `getchk` / `BANK` / `BANK_NAME` | rdf | چک‌ها و بانک‌ها |
| نماهای فروش | — | `VWForushKhales`(90 ستون), `VW_AllBargashti`, `VW_FORUSH_RizAghlam_Nakhales`, `VW_Forush_DarBazeZamani`(107), `svcGetBanks`, `promotion` |

**ج) API محلی Atiran (LocalServices)** — ۲۰+ اندپوینت .NET (از `atiran/docs/api-reference.json`): `Customers`, `FilteredCustomers(WithMode)`, `CustGroups/FilteredCustGroups`, `FilteredKaGroups`, `InventoryAnbars`, `CustomerFactors`, `CustomerEtebar`, `GetChecks/GetAllChecks/CustomerChecks`, `VisitorMasir/AllVisitorMasir`, `VisitorMessages/AllVisitorMessages`, `ReadMessage`, `RegisterDeviceAppId`, `CheckSetInfo`, `CustomerByShMo` — این‌ها قرارداد داده‌ای ERP را تأیید می‌کنند (شکل پاسخ‌ها در `dto-reference.json`).

---

## ۲. Map کامل: قابلیت → Endpoint فعلی → شیء SQL

| # | قابلیت در اپ (صفحه) | Endpoint فعلی (PHP) | منطق فعلی سرور (repo.php) | شیء SQL فعلی | شیء SQL هدف (مستقیم) |
|---|---|---|---|---|---|
| 1 | سلامت/تست اتصال (Settings) | `action=ping` | `vizitor_table_counts`, `SERVERPROPERTY` | Vizitor* + DB meta | `SELECT` مستقیم از sys.tables + `DB_NAME()` |
| 2 | نسخه سرویس | `action=version` | config | — | حذف (نسخه در اپ) |
| 3 | **Login** (LoginScreen) | `action=login` | `password_verify` روی `VizitorUsers.PasswordHash` (bcrypt PHP!) + lockout + token SHA-256 | VizitorUsers, VizitorTokens, VizitorLoginAttempts | **سؤال طرحی‌شده** — بخش ۵ |
| 4 | کاتالوگ کالا (CatalogScreen/Scanner) | `action=catalog&since` | `SELECT … FROM vwVizitorProducts` (sort Persian_100) | vwVizitorProducts → (قالب: Kala/KalaGroup) | `inventory` + `kagroup` + `VW_InventoryAnbars` (اثبات‌شده) |
| 5 | مشتریان (CustomersScreen) | `action=customers&since` | `SELECT … FROM vwVizitorCustomers` | vwVizitorCustomers → (قالب: Customer/CustGroup) | `CUSTOMERS` + `custgroup` + `CITYS` + scope با `sys_cus` (اثبات‌شده) |
| 6 | سال مالی مشتری (DAS/Gemini) | `action=sal_mali` | `vwVizitorSalMali` | vwVizitorSalMali | `sailfact`+`subsailfact` (یا `VWForushKhales`) — **VERIFY** روی سرور |
| 7 | **ثبت فاکتور** (Cart→signature) | `action=submit_invoice` | idempotency با `ClientInvoiceId` + strict_totals + شماره `VZ-سال‌جلالی-سکانس` + transaction | VizitorSalesHeader/Lines (+ قلاب `write_to_erp` **پیاده‌نشده**) | `sailfact_pish`/`subsailfact_pish` یا SP (بخش ۵) — **VERIFY** |
| 8 | مشتری جدید (Customers→new) | `action=submit_customer` | dedupe sha256(name|phone|city) 30روز + درخواست PENDING | VizitorCustomerRequests | `new_cust` یا همون جدول درخواست — **VERIFY** |
| 9 | قیمت/موجودی در سبد | — (مطالعه از کش Room) | — | — | `forosh_price` (تایر قیمت) + `VW_InventoryAnbars` (موجودی زنده) |
| 10 | مسیریابی/مسیر (MiniRouteMap) | — | — | — | `masir` + `MasirDay` + `regions` (موجود در مدل) |
| 11 | اهداف فروش (Reports) | — (hardcode در اپ) | — | — | `vis_goals` / `VW_GoalsVisitors` |
| 12 | چت ویزیتورها (ChatScreen) | — (فقط Room) | — | — | در اسکریم فعلی **چیزی به سرور نمی‌رود**؛ همان‌طور می‌ماند |
| 13 | چک‌ها (ChequeStore) | — | — | — | `getchk` (در فازهای بعد) |

---

## ۳. منطق‌های کسب‌وکاری که **باید** حفظ شوند (از سورس استخراج شده)

1. **Idempotency فاکتور**: `ClientInvoiceId` یکتا (UUID در اپ) → ثبت تکراری همان `InvoiceNo` را برمی‌گرداند؛ حتی در race شرطی دوباره بررسی می‌شود.
2. **strict_totals**: `sum(lines) == gross` و `gross - discount == final` (خطای 400 در غیر این صورت).
3. **قفل موقت Login**: ۵ شکست در ۱۵ دقیقه → 429.
4. **توکن نشست**: SHA-256 در DB، TTL 720 دقیقه UTC، `LastUsedAt` quiet-update، پاک‌سازی توکن‌های منقضی.
5. **شمارهٔ فاکتور**: `VZ-<سال جلالی>-<سکانس 6 رقم>` با سکانس اتمیک (`UPDATE…OUTPUT`).
6. **Dedupe مشتری جدید**: sha256(نام|تلفن|شهر) در پنجرهٔ 30 روزه PENDING.
7. **تخفیف‌ها**: در v1.6.0 طبق کامنت سورس «به درخواست کارفرما حذف شد» (discount=0) — `computeDiscount` هنوز در کد هست ولی استفاده نمی‌شود.
8. **سطح قیمت فعلی اپ**: `defaultPriceLevel` — گروه «عمده» → `price2`، بقیه → `price1` (هیووریستیک سمت کلاینت؛ در معماری جدید با `forosh_price.forosh1..5` + `custgroup.price` جایگزین صحیح می‌شود — **منطق ساده‌سازی نمی‌شود**).
9. **آفلاین-first**: فاکتور اول در Room (pending)، ارسال توسط SyncWorker با retry/backoff؛ موجودی محلی optimistic-kسر می‌شود.
10. **decrement_stock=false / write_to_erp=false** (config): فاکتورها در `VizitorSalesHeader` می‌مانند تا حسابداری ببیند. → در معماری مستقیم، این «قلاب» بالاخره می‌تواند واقعی شود (بخش ۵-ج).

---

## ۴. ممیزی امنیتی فعلی → الزامات جدید

| مورد | وضعیت فعلی | الزام در معماری مستقیم |
|---|---|---|
| رازها | رمز DB فقط در `config.php` سرور؛ اپ فقط apiKey | رمز SQL **توسط کاربر در Settings** وارد می‌شود؛ در **EncryptedSharedPreferences** (Android Keystore) نگه می‌دارد — هرگز در Source/Log/Crash |
| تزریق SQL | همهٔ کوئری‌های PHP parameterized | همان قانون: **فقط parameterized** در کل DataSource |
| کاربر SQL | `vizitor_app` محدود (توصیه در config.sample) | کاربر جدید **limit-privilege** (دستور دقیق: بخش ۸) |
| TLS | `encrypt`/`trust_server_certificate` از config | بررسی `encrypt=true`؛ اگر گواهی خودامضا باشد `trustServerCertificate=true` با هشدار در UI |
| اتصال | TCP 1433 از اپ → اینترنت | فایروال (بخش ۹) + توصیهٔ VPN |

---

## ۵. افتراقات اثبات‌شده (مطابق قانون #19) — این‌ها **حدس نیستند**

| # | مورد | شواهد | اقدام |
|---|---|---|---|
| A | ستون‌های `dbo.sys_users` و `dbo.Roles` | **در هیچ یک از آرته‌فاکت‌های موجود نیست** (نه در مدل SAC، نه در DataAccess، نه در api-reference). `visitors.Username/Password/UserID` و `sys_vis.UserID` نشان می‌دهند جدول کاربرها جداست و `UserID` به آن اشاره می‌کند | `00_audit_atiran2.sql` (بخش 01/02/06) روی سرور واقعی → ستون‌ها و **نمونهٔ 3 کاراکتر اول هش** → الگوریتم hash مشخص می‌شود |
| B | امضای SPهای `add_sail_pish` و هم‌خانواده | در سورس موجود نیستند (کد PHP فعلی اصلاً SP نمی‌زند) | همان اسکریت (بخش 04/05) → نام دقیق + پارامترها + متن SP |
| C | تعریف واقعی `vwVizitor*` روی سرور شما | در ریپازیتوری فقط قالب با `← جایگزین کنید` | همان اسکریت (بخش 03) → `sys.sql_modules` |
| D | `new_cust` / `ka_act` | در لیست شما هست ولی در مدل SAC نیست | بخش 01/02 اسکریت |
| E | الگوریتم پاسورد Atiran | نامعلوم (PHP Vizitor از bcrypt خود PHP استفاده می‌کند که **از Android قابل verify نیست** → حتی اگر VizitorUsers بماند، مستقیم‌خوانی عملی نیست) | بخش 06b اسکریت (نمونهٔ هش) |

---

## ۶. معماری هدف (To-Be)

```
UI (Compose — بدون تغییر)
  ↓
VizitorViewModel (تک ViewModel فعلی)
  ↓
VizitorRepository (همان — امضای عمومی حفظ می‌شود)
  ├──► Room (کش آفلاین — حفظ می‌شود؛ وضعیت تازه‌بودن داده مشخص می‌شود)
  └──► SqlServerDataSource (جدید)
         ├── CustomersSource, ProductsSource, PriceSource, StockSource,
         │ InvoiceSource (پیش‌فاکتور/فاکتور — transactional),
         │ ScopeSource (sys_cus/sys_vis/masir), GoalsSource, AuditSource
         └── SqlConnectionManager (جدید)
                • اتصال TDS مستقیم: mssql-jdbc (درایور رسمی مایکروسافت، pure-Java)
                • Connection Pool کوچیک (2-4) + validation (SELECT 1) + timeout (10s)
                • Retry با backoff برای خطاهای گذرا (SQL 10053/10054/timeout)
                • وضعیت اتصال: StateFlow<ConnectionStatus> (READY/CONNECTING/DEGRADED/OFFLINE)
                • همهٔ کوئری‌ها parameterized؛ transaction با try/commit/rollBack
                • Dispatchers.IO — هیچ کاری روی UI thread
                • رازها از EncryptedSharedPreferences — هرگز در log
```

**نتیجهٔ کلیدی برای UI:** امضای متدهای `VizitorRepository` (login/syncAll/issueInvoice/… ) حفظ می‌شود → هیچ صفحه‌ای تغییر نمی‌کند؛ فقط منبع داده از Retrofit به DataSource SQL تغییر می‌کند. `RetrofitClient/ApiService/ApiModels` پس از مهاجرت کامل **فقط** برای Gemini-AI (Worker) و هر سرویس غیر-DB باقی می‌مانند و بقیه حذف می‌شود.

**سازگاری درایور با Android:** `com.microsoft.sqlserver:mssql-jdbc:12.4.x` (pure Java، ARM-friendly، pooling، TLS 1.2). SQL Server 2014 (compat 120 — از `eeee.sql`) پشتیبانی می‌شود؛ `OFFSET…FETCH` برای pagination در دسترس است.

**سندیت و Transaction (بخش ۱۱ شما):** ثبت پیش‌فاکتور = یک transaction: incheck موجودی/اعتبار (با قفل سطر) → INSERT هدر → INSERT سطرها → commit؛ در هر خطا rollBack کامل؛ Idempotency با کلید کلاینت (UUID) در ستون مناسب هدر (معادل `ClientInvoiceId`)؛ حالت «نیمه‌ثبت‌شده» وجود ندارد.

---

## ۷. نقشهٔ مهاجرت endpoint → DataSource (پس از VERIFY)

| Endpoint فعلی | DataSource جدید | شیء SQL (اثبات‌شده یا VERIFY) |
|---|---|---|
| ping | `AuditSource.health()` | sys.tables/COUNT + DB_NAME() |
| login | `AuthSource.login()` | `sys_users`+`Roles` (VERIFY) یا `visitors`+`sys_vis` (اثبات‌شده) |
| catalog | `ProductsSource.list(page,search,updatedAfter)` | `inventory`+`kagroup`+`forosh_price`+`VW_InventoryAnbars` |
| customers | `CustomersSource.list(scope,page,search)` | `CUSTOMERS`+`custgroup`+`CITYS` ∩ `sys_cus` (scope) |
| sal_mali | `HistorySource.customerSales(shmo)` | `sailfact`+`subsailfact` (VERIFY: یا نما `VWForushKhales`) |
| submit_invoice | `InvoiceSource.savePish(header,lines,clientKey)` | `sailfact_pish`+`subsailfact_pish` (VERIFY: یا SP) — transaction |
| submit_customer | `CustomersSource.requestNew(...)` | `new_cust` (VERIFY) یا VizitorCustomerRequests (حفظ) |
| — (جدید) قیمت | `PriceSource.tieredPrice(shka, custGroupRdf)` | `forosh_price` (forosh1..5) + `custgroup.price` + fallback `inventory.inventory_price` — **بدون قیمت fake** |
| — (جدید) موجودی | `StockSource.stock(shka, anbarRdf)` | `VW_InventoryAnbars` |
| — (جدید) scope | `ScopeSource.visitorScope(user)` | `sys_cus`+`sys_vis`+`masir`+`MasirDay`+`vis_goals` |

---

## ۸. تنظیمات SQL Server موردنیاز (برای اجرا روی سرور — بدون آسیب)

1. **کاربر کم‌دسترسی** (اسکریپت غیرتلفیقی جدا: فقط CREATE در صورت نبود):
   - `CREATE LOGIN vizitor_android WITH PASSWORD=…` (فقط اگر نباشد)
   - یوزر در `Atiran2` فقط با: `db_datareader` + دسترسیهای مشخص:
     - SELECT روی: CUSTOMERS, custgroup, CITYS, Province, regions, inventory, kagroup, forosh_price, prizePercent, anbars, inventory_anbars, VW_InventoryAnbars, sailfact, subsailfact, sailfact_pish, subsailfact_pish, visitors, sys_vis, sys_cus, sys_kal, masir, MasirDay, Visit, vis_goals, cust_act, getchk, osystems, Company, TabletCustomer
     - INSERT/UPDATE روی: `sailfact_pish`, `subsailfact_pish` (و یا اجرای SP معتمد) + `new_cust`
     - **هیچ دسترسی DROP/ALTER DDL و هیچ دسترسی روی جداول حسابداری واقعی (sailfact/subsailfact) ندارد** — فاکتور «قطعی» توسط حسابداری در خود آتیران تصویب می‌شود (همان فرایند فعلی)
2. **SQL Server Configuration Manager**: TCP/IP روی 1433 (در صورت خاموش بودن) — فقط اگر قبلاً فعال نباشد.
3. **فایروال ویندوز**: inbound rule برای 1433 (یا VPN — ترجیح امنیتی).
4. **TLS**: اگر SQL 2014 روی Win Server 2012+ باشد TLS 1.2 ممکن است (registry `EnabledSSLProtocols`) — در غیر این‌صورت برای شبکهٔ داخلی `encrypt=false` + فایروال (با هشدار در UI).

## ۹. فایروال

```
netsh advfirewall firewall add rule name="SQL Vizitor" dir=in action=allow protocol=TCP localport=1433
```
(بهتر: محدود به subnet موبایل اگر ثابت است، یا VPN.)

## ۱۰. تنظیمات Android موردنیاز

- وابستگی جدید: `com.microsoft.sqlserver:mssql-jdbc:12.4.2.jre8` (+ proguard keep-rule)
- Settings: افزودن `dbUser` (+ حفظ امن `dbPass`) کنار `serverIp/dbPort` موجود
- permission جدید: **نیازی نیست** (INTERNET کافی است)
- `network_security_config` بدون تغییر (DB = TCP، نه HTTPS)

---

## ۱۱. برنامهٔ اجرا (فازبندی)

| فاز | خروجی | وضعیت |
|---|---|---|
| 0 | این ممیزی + استخراج schema + اسکریت audit | ✅ انجام شد |
| 1 | اجرای `00_audit_atiran2.sql` روی Atiran2 واقعی → `docs/audit-output/` | ⏳ نیاز به اجرای شما |
| 2 | `SqlConnectionManager` + `SecureDbSettings` + صفحات Settings (تست اتصال) | ✅ کد manager و ذخیرهٔ امن؛ اتصال به UI در گام بعد |
| 3 | DataSourceها: Products/Catalog + Prices + Stock | بعد از فاز 1 |
| 4 | Customers + Scope (sys_cus) + Search/Pagination | |
| 5 | Auth (طبق نتیجهٔ فاز 1) + Session | |
| 6 | Invoice/PishDaryaft transactional + Idempotency + rollback tests | |
| 7 | اتصال مجدد همهٔ صفحات + حذف Retrofit (غیر-AI) + حفظ Room کش + وضعیت تازه‌بودن | |
| 8 | آزمون ۲۷ موردی (هرکدام در حد امکان اینجا/روی دستگاه شما) | |

## ۱۲. خُرده‌فنی‌ها (Risks) — شفاف

1. **آسیب‌پذیری امنیتی:** باز کردن 1433 به اینترنت خطر بالاتر از PHP-API است. حتی با کاربر کم‌دسترسی، توصیهٔ اصلی: VPN موبایل یا حداقل محدودسازی IP. این یک trade-off خودِ درخواست شماست.
2. **تغییرهای آتی آتیران:** اگر ERP آپدیت شود و ستونی تغییر کند، DataSource مستقیم حساس‌تر است — اسکریت audit هر زمان قابل اجراست (دایجست schema).
3. **SQL 2014:** برخی توابع جدید T-SQL نیست (STRING_AGG و…) — کوئری‌ها فقط با توابع ≤2014 نوشته می‌شوند.
4. **mssql-jdbc روی Android:** نیاز به تست واقعی روی دستگاه (اینجا Android SDK/اینترنت برای build نیست) — کد با APIهای پایدار و محافظت‌شده نوشته می‌شود.
5. **تاریخ/زمان:** دیتابیس Atiran از `char` date (فرمت محلی، احتمالا `yy/mm/dd`) و `datetime` استفاده می‌کند — پارس فقط با فرمت‌های اثبات‌شده در audit (بدون حدس) انجام می‌شود.
