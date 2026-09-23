# تحلیل کد‌به‌کد پروژهٔ «آتیران ویزیتور» — دور ۲٫۱۹٫۰
**تاریخ:** ۲۰۲۶‑۰۹‑۲۳ · **شاخه:** `arena/01a0c76f-vizitor-android` · **نسخهٔ خروجی:** `2.19.0-direct` (شمارهٔ ساخت ۲۱۹۱۵)

> این سند، نتیجهٔ **بازخوانی خط‌به‌خط** کل پروژهٔ اندروید است، نه خلاصهٔ ظاهری.
> هدف دو چیز بود: (۱) پیدا کردن هر جایِ نیمه‌وصلِ «اتصال» و «فراخوانی» تا هیچ دکمه‌ای
> بی‌نتیجه نماند، و (۲) بازسازی صفحهٔ نخست (اتصال ویزیتور) و صفحهٔ ارسال فاکتور
> **دقیقاً به سبک عکس‌های مرجع** M•A Report.

---

## ۱) روش کار
۱. فهرست‌برداری کامل: هر فایل `.kt` با شمار خط و نقش آن (جدول بخش ۲).
۲. استخراج همهٔ اعلان‌های سطح‌بالای `fun` (۵۹۶ اعلان) و شمارش ارجاع‌ها در کل پروژه
   برای یافتن کد بی‌فراخوان (بخش ۴).
۳. ردگیری زنجیرهٔ فراخوانی دو مسیر کلیدی «اتصال ویزیتور» و «ارسال فاکتور»
   از لایهٔ رابط کاربری تا خودِ SQL (بخش‌های ۳‑الف و ۳‑ب).
۴. بررسی ساختاری هر فایل تازه: توازن بلوک‌ها (پرانتز/آکولاد) و قواعد چیدمان Compose
   («هر `item` فقط یک فرزند» — اشکالی که کارت‌ها را روی هم می‌اندازد).
۵. تطبیق تصویری با یازده عکس مرجع (نوار بالا، نوار شش بخش، نبض اتصال، کارت طلایی،
   قرص‌ها، کارت سند، کاشی‌های خروجی، برگهٔ تنظیمات هوشمند).

**اندازهٔ پروژه:** ۷۵ فایل Kotlin · ۲۹٬۹۷۳ خط · دو پشتهٔ موازی
(`sqldirect/` = مسیر زندهٔ SQL Server، `data/` = آفلاین Room + لایهٔ قدیمی PHP).

---

## ۲) فهرست خط‌به‌خط فایل‌ها

| فایل | خط | نقش |
|---|---:|---|
| `ui/screens/MaConnectScreen.kt` | 1273 | صفحهٔ نخست «اتصال ویزیتور» به سبک مرجع (۲٫۱۹) |
| `ui/screens/CatalogScreen.kt` | 1220 | ویترین کالا (تصویر، سطوح قیمت، افزودن به سبد) |
| `ui/screens/CartScreen.kt` | 1115 | ارسال فاکتور به سبک مرجع + خروجی واقعی سند (۲٫۱۹) |
| `ui/screens/DirectSqlScreen.kt` | 1087 | اتصال پیشرفتهٔ گام‌به‌گام + انتخاب ویزیتور |
| `ui/components/MaReport.kt` | 1073 | زبان طراحی «گزارش طلایی»: پنل فلزی، نبض، نمودار، کاشی ابزار |
| `ui/components/Premium.kt` | 1032 | پنل/دکمه/فیلد/سوئیچ پریمیوم + ScreenFit (پاسخ‌گویی) |
| `ui/screens/ChatScreen.kt` | 1031 | گفتگوی ویزیتورها |
| `ui/screens/CustomersScreen.kt` | 1026 | پروندهٔ مشتریان + سینک |
| `ui/screens/DashboardScreen.kt` | 996 | پیشخوان (شاخص‌ها، نبض، دسترسی سریع) |
| `ui/components/MaChrome.kt` | 964 | چهارچوب مرجع: نوار بالا/بخش‌ها، کارت طلایی، قرص‌ها، کارت سند، تنظیمات هوشمند (۲٫۱۹) |
| `sqldirect/MeelanoDataSource.kt` | 948 | کوئری‌های واقعی ERP (ورود، ویزیتور، مشتری، کالا، انبار، پیش‌فاکتور) |
| `ui/components/Lux3D.kt` | 943 | کیت سه‌بعدی (صحنه، کاشی/منو، ستون، دونات، گوی، جدول) |
| `ui/components/Charts.kt` | 855 | نمودارهای خطی/دونات/ستونی |
| `sqldirect/DirectSqlViewModel.kt` | 773 | وضعیت و فرمان‌های صفحهٔ اتصال (اتصال، ورود، سینک، خروج) |
| `ui/navigation/VizitorNavigation.kt` | 760 | ناوبری: مسیرها، نوار پایین، FAB سبد، مسیر اتصال/اطلاع‌رسانی |
| `ui/screens/VisitScreen.kt` | 747 | ثبت ویزیت (موقعیت، مشتری، ذخیره) |
| `ui/screens/SplashScreen.kt` | 719 | صفحهٔ آغاز و انتخاب نقش |
| `ui/components/SplashKit.kt` | 681 | اجزای صفحهٔ آغاز و قرص‌های درخشان |
| `ui/screens/SettingsScreen.kt` | 586 | تنظیمات برنامه (تم، سرور، سینک) |
| `ui/screens/ManagerScreen.kt` | 582 | پنل مدیریت (گزارش کامل، نمودار و جدول) |
| `ui/screens/BriefingScreen.kt` | 576 | اطلاع‌رسانی اولیه به ویزیتور (۲٫۱۸) |
| `sqldirect/VizitorGateway.kt` | 564 | دروازهٔ واحد: probe، سینک، اطلاع‌رسانی، ثبت پیش‌فاکتور و کنار گذاشتن سند |
| `ui/components/Luxury2.kt` | 559 | کارت‌های لوکس نسل دوم |
| `ui/screens/ReportsScreen.kt` | 537 | گزارش‌ها و اشتراک فاکتور |
| `sqldirect/SqlConnectionManager.kt` | 535 | گردانندهٔ اتصال (connect / ensureConnected / borrow / withConnection) |
| `VizitorViewModel.kt` | 523 | پل رابط کاربری ↔ دادهٔ محلی و دروازهٔ سرور (اتصال، سینک، سند، اطلاع‌رسانی) |
| `ui/components/Luxury.kt` | 504 | شیمر طلایی، افکت یخ طلایی، سرصفحهٔ سلطنتی |
| `ui/screens/ActivitiesScreen.kt` | 476 | همهٔ فعالیت‌های ویزیتور (۲٫۱۸) |
| `ui/components/Glass.kt` | 421 | کارت شیشه‌ای، پس‌زمینهٔ پیشخوان |
| `data/repository/VizitorRepository.kt` | 417 | دادهٔ محلی + صدور فاکتور محلی + سینک قدیمی PHP |
| `ui/components/Widgets.kt` | 388 | دکمه‌های نئون، فوتر برند، عنوان بخش |
| `ui/components/AutoFit.kt` | 324 | متن پاسخ‌گو و کمکی‌های اندازه |
| `sqldirect/VisitViewModel.kt` | 283 | وضعیت و فرمان‌های صفحهٔ ویزیت |
| `sqldirect/CustomerSync.kt` | 274 | همگام‌سازی مشتریان مجاز ویزیتور |
| `ui/components/Glamour.kt` | 272 | دکمهٔ طلایی/بنفش (GlamourButton) و کارت شاخص |
| `sqldirect/SecureDbStore.kt` | 254 | ذخیرهٔ رمزنگاری‌شدهٔ تنظیمات/کاربر/ویزیتور روی گوشی |
| `sqldirect/VisitRepository.kt` | 249 | ویزیت‌ها روی dbo.Visit |
| `sqldirect/SqlDiagnostics.kt` | 241 | تشخیص گام‌به‌گام اتصال و گزارش عیب‌یابی بدون رمز |
| `ui/theme/Themes.kt` | 227 | هفت پالت رنگ برنامه |
| `sqldirect/SqldirectSync.kt` | 226 | همگام‌سازی کالا/مشتری/فاکتور/ویزیت/مسیر/سهمیه + گزارش فارسی |
| `data/share/InvoiceShare.kt` | 213 | ساخت و اشتراک‌گذاری PDF/Word/تصویر/متن فاکتور |
| `ui/screens/ScannerScreen.kt` | 186 | اسکنر بارکد کالا |
| `data/local/Daos.kt` | 185 | DAOهای Room (کالا، مشتری، فاکتور، سبد، چت، چک) |
| `sqldirect/VisitorRepository.kt` | 178 | ویزیتورها، مجوزها و سهمیه/اعتبار |
| `ui/components/MiniRouteMap.kt` | 169 | نقشهٔ کوچک مسیر |
| `data/local/Entities.kt` | 164 | موجودیت‌های Room (کالا، مشتری، فاکتور، اقلام، سبد، فاکتور سرور) |
| `ui/manager/ManagerAnalytics.kt` | 143 | محاسبهٔ شاخص‌های مدیریتی از دادهٔ محلی |
| `sqldirect/VisitorLogin.kt` | 140 | فهرست ویزیتورهای dbo.sys_vis (مدل، جست‌وجو، برچسب) |
| `data/local/ChatPrefs.kt` | 132 | ذخیرهٔ محلی پیام‌های گفتگو و پاک‌سازی |
| `sqldirect/VizitorSession.kt` | 127 | نشست جاری (کد ویزیتور، کاربر، شرکت، شمارنده‌ها) |
| `sqldirect/DirectSql.kt` | 119 | ساخت نشانی JDBC و بارگذاری دو درایور |
| `data/local/AppDatabase.kt` | 115 | پایگاه آفلاین Room (نسخه، جداول، ساخت) |
| `sqldirect/ConnectCards.kt` | 112 | تجزیهٔ «کارت اتصال» نصب‌کنندهٔ ویندوز |
| `ui/theme/VizitorFonts.kt` | 111 | فونت‌ها و اندازه‌ها |
| `sqldirect/ProductDefaults.kt` | 108 | پیش‌فرض قیمت مصرف‌کننده و دستهٔ کالا از نام کالا |
| `data/remote/ApiModels.kt` | 107 | مدل‌های JSON لایهٔ قدیمی PHP |
| `ai/GeminiAssistant.kt` | 106 | دستیار هوشمند پیشنهاد کالای مکمل (با بازگشت آفلاین) |
| `util/Format.kt` | 100 | اعداد/مبلغ/تاریخ فارسی و تجزیهٔ عدد |
| `data/sync/SyncWorker.kt` | 96 | کار پس‌زمینهٔ سینک (لایهٔ قدیمی) |
| `data/local/ChequeStore.kt` | 93 | ذخیرهٔ محلی چک‌ها |
| `sqldirect/VisitLocation.kt` | 92 | موقعیت مکانی ویزیت |
| `ui/catalog/ProductVisuals.kt` | 87 | تصویر/ایموجی کالا بر اساس نام کالا |
| `ui/components/Tilt3D.kt` | 86 | تیلت سه‌بعدی کارت‌ها |
| `ui/components/VoiceSearch.kt` | 84 | جست‌وجوی صوتی |
| `data/repository/SettingsRepository.kt` | 82 | تنظیمات سرور (DataStore) |
| `perf/VizitorPerf.kt` | 81 | تشخیص قدرت گوشی و انتخاب سطح جلوه (LOW/BALANCED/HIGH) |
| `ui/theme/Theme.kt` | 76 | چیدمان تم Material |
| `data/local/SeedData.kt` | 70 | دادهٔ نمونهٔ اولیه برای حالت آفلاین |
| `MainActivity.kt` | 69 | ورود برنامه، RTL و تم |
| `data/remote/ApiService.kt` | 63 | Retrofit: فراخوانی‌های لایهٔ قدیمی PHP |
| `ui/theme/Color.kt` | 52 | رنگ‌های معنایی و سازگاری |
| `sqldirect/VisitDate.kt` | 48 | تاریخ شمسی ویزیت |
| `data/remote/RetrofitClient.kt` | 40 | ساخت کلاینت Retrofit با مهلت‌ها |
| `VizitorApp.kt` | 39 | Application: تشخیص سطح گرافیک (VizitorPerf.detect) |
| `sqldirect/VizitorRoleIntent.kt` | 39 | نقش انتخاب‌شده (ویزیتور / مدیر) |

---

## ۳) ردگیری دو مسیر کلیدی (وضعیت تأیید‌شده)

### الف) مسیر «اتصال ویزیتور» — کامل و یک‌مسیره
| گام | فایل:خط | کار |
|---|---|---|
| ۱ | `ui/screens/SplashScreen.kt:127` | صفحهٔ آغاز؛ دکمهٔ «تنظیم اتصال» → `onOpenServerConfig` |
| ۲ | `ui/navigation/VizitorNavigation.kt:344` | `navController.navigate(Routes.WELCOME)` |
| ۳ | `ui/navigation/VizitorNavigation.kt:468` | **تازه:** مسیر WELCOME → `MaConnectScreen` (صفحهٔ مرجع‌گونه) |
| ۴ | `ui/screens/MaConnectScreen.kt:52` | `viewModel.restoreSaved()` — خواندن تنظیمات ذخیره‌شدهٔ گوشی |
| ۵ | `sqldirect/DirectSqlViewModel.kt:284` | `connectDatabase(autoLoginAfter)` |
| ۶ | `sqldirect/SqlConnectionManager.kt:157` | `connect(DbSettings)` — امتحان jTDS سپس Microsoft |
| ۷ | `sqldirect/SqlConnectionManager.kt:130` | `ensureConnected()` — خودترمیمی قبل از هر کوئری |
| ۸ | `sqldirect/DirectSqlViewModel.kt:567` | `loginAndLoad()` → `MeelanoDataSource.login:159` |
| ۹ | `sqldirect/DirectSqlViewModel.kt:484` | `enterAsVisitor(option)` — ورود از فهرست `dbo.sys_vis` |
| ۱۰ | `sqldirect/SqlConnectionManager.kt:254` | `refresh()` — زمان پاسخ سرور برای «نبض اتصال» |
| ۱۱ | `sqldirect/VizitorGateway.kt:64` | `probe()` — درایور، تأخیر، دیتابیس، نسخه |
| ۱۲ | `sqldirect/VizitorSession.kt` | نشست: `visitorRdf`، کاربر، شرکت، شمارنده‌ها |

**نتیجه:** هیچ گام بی‌فراخوانی نمانده است؛ هر دکمهٔ صفحهٔ اتصال به یکی از
خط‌های بالا می‌رسد و صفحهٔ «تنظیمات پیشرفته» (`DirectSqlScreen.kt:138`) هم به‌عنوان
مسیر کامل تشخیصی در دسترس است.

### ب) مسیر «ارسال فاکتور» — از انگشت ویزیتور تا ردیف ERP
| گام | فایل:خط | کار |
|---|---|---|
| ۱ | `ui/screens/CartScreen.kt` | انتخاب مشتری، اقلام، یادداشت، امضا، کلید «ثبت واقعی» |
| ۲ | `VizitorViewModel.kt:270` | `issueInvoice(signaturePng, cashSettlement, note, onDone)` |
| ۳ | `data/repository/VizitorRepository.kt:190` | ثبت محلی در Room (هدر + اقلام + کسر موجودی + خالی‌کردن سبد) |
| ۴ | `VizitorViewModel.kt:283` | `VizitorGateway.submitCart(db, customer, userName, note, live)` |
| ۵ | `sqldirect/VizitorGateway.kt:390` | ساخت پیش‌نمایش سند (مبلغ‌ها، انبار هر قلم از `VW_InventoryAnbars`) |
| ۶ | `sqldirect/VizitorGateway.kt:298` | `preInvoiceReference()` — دروازهٔ ایمنی: پیش‌فاکتور مرجع در ERP؟ |
| ۷ | `sqldirect/MeelanoDataSource.kt:664` | `createPreInvoice(head, lines)` — `EXEC dbo.add_sail_pish` (head) |
| ۸ | `sqldirect/MeelanoDataSource.kt:691` | درج اقلام در `dbo.subsailtemp_pish` (تریگر `trig_sst_pish`) |
| ۹ | `sqldirect/MeelanoDataSource.kt:691` | `preInvoiceLines(shfacfo)` — بازخوانی تأییدی از `subsailfact_pish ⋈ sailfact_pish` |
| ۱۰ | `sqldirect/MeelanoDataSource.kt:734` | `retirePreInvoice(shfacfo)` — کنار گذاشتن سند نیمه‌کاره (`active='f'`, `ismodify='t'`) |
| ۱۱ | `VizitorViewModel.kt:305` | `invoiceOutcome` — نتیجه برای کارت «نتیجهٔ سند» |
| ۱۲ | `data/share/InvoiceShare.kt:133/141/172/196` | خروجی واقعی: تصویر، PDF، Word، متن |

**قاعدهٔ ایمنی (دست‌نخورده):** تا وقتی `dbo.sailfact_pish` هیچ پیش‌فاکتور مرجعی
نداشته باشد، برنامه **فقط پیش‌نمایش** می‌سازد و هیچ ردیفی در سرور نمی‌نویسد؛
پنج مقدار ناشناختهٔ سربرگ حدس زده نمی‌شوند.

---

## ۴) یافته‌های تحلیل: کد بی‌فراخوان و اتصال‌های نیمه‌کاره

در کل پروژه، ۳۵ تابع یا مقدار پیدا شد که **هیچ‌جا فراخوانی نمی‌شدند**. دسته‌بندی:

| # | مورد | فایل | تصمیم در ۲٫۱۹٫۰ |
|---|---|---|---|
| ۱ | `askAiAssistant` + `aiSuggestion` + `aiLoading` | `VizitorViewModel.kt` | **وصل شد** — بخش «دستیار هوشمند فروش» در صفحهٔ ارسال فاکتور |
| ۲ | `MaMetricCard` | `ui/components/MaReport.kt` | **وصل شد** — چهار کارت شاخص صفحهٔ اتصال |
| ۳ | `SqlDiagnostics.run` + `buildReport` | `sqldirect/SqlDiagnostics.kt` | **وصل شد** — دکمهٔ «اجرای تشخیص کامل» در دیالوگ هشدار |
| ۴ | `refreshVisitors` | `sqldirect/DirectSqlViewModel.kt` | **وصل شد** — کاشی «به‌روزرسانی ویزیتورها» در تنظیمات هوشمند |
| ۵ | `toggleVisitorPicker` | `sqldirect/DirectSqlViewModel.kt` | بی‌نیاز (صفحهٔ تازه خودش باز/بسته می‌کند) — گزارش شد |
| ۶ | `HealthStatusCard`, `MiniPanelTitle` | `SettingsScreen.kt`, `DashboardScreen.kt` | خصوصی/بی‌استفاده — دست‌نخورده |
| ۷ | `RoyalTable`, `NeonDonutChart` | `ui/components/Charts.kt` | بازماندهٔ زبان بصری قدیمی — دست‌نخورده |
| ۸ | `IdealButton`, `VizitorField`, `InfoPill`, `PriceTag3D` | `AutoFit/SplashKit/Luxury` | بازماندهٔ نسخه‌های پیشین — دست‌نخورده |
| ۹ | `MiniRouteMap` | `ui/components/MiniRouteMap.kt` | جایگزین شد با جدول مسیرها — دست‌نخورده |
| ۱۰ | `salesSince`, `observeCount`, `observeItemCount` | `data/local/Daos.kt` | کمکی‌های DAO بدون مصرف |
| ۱۱ | `customerGroups`, `customersForVisitor`, `priceFor`, `toServerConfig` | `sqldirect/MeelanoDataSource.kt` | خواندن‌های جایگزین‌شده |
| ۱۲ | `fetchSalMaliFor`, `verifyPassword`, `loadMasked` | `VizitorRepository/ChatPrefs/SecureDbStore` | بدون مصرف |
| ۱۳ | `jdbcUrl`, `jtdsUrl` | `sqldirect/SqlConnectionManager.kt` | نشانی‌ها مستقیم ساخته می‌شوند |
| ۱۴ | `tcpReachable`, `buildReport` (پیش‌تر) | `sqldirect/SqlDiagnostics.kt` | `tcpCheck` جای آن را گرفته |
| ۱۵ | `stamp`, `firstLine`, `emojiFor`, `markupPercentFor` | Gateway/SqlConnectionManager/ProductVisuals/ProductDefaults | کمکی‌های بی‌مصرف (بدون ریسک) |
| ۱۶ | `onLocationChanged/onProviderDisabled/onProviderEnabled/onStatusChanged` | `sqldirect/VisitLocation.kt` | پیاده‌سازی رابط موقعیت — **لازم** است (هشدار کاذب ابزار) |

### اشکال‌های واقعی که در همین دور رفع شدند
| # | اشکال | فایل | رفع |
|---|---|---|---|
| ۱ | کارت‌های شاخص تازه: سه فرزند در یک `item` (روی‌هم‌افتادن) | `ui/screens/MaConnectScreen.kt:381` | همه در یک `Column` قرار گرفتند |
| ۲ | «راهنمای برند» پنل مدیریت: `Spacer` و `Text` در یک `item` | `ui/screens/ManagerScreen.kt:569` | داخل `Column` رفتند |
| ۳ | دکمهٔ ارسال فاکتور روی همان مسیر قدیمی بود (نامشخص بودن نتیجه) | `ui/screens/CartScreen.kt:315` (نسخهٔ پیشین) | صفحهٔ تازه، خروجی سند را **صریح** نشان می‌دهد (شمارهٔ سند/تعداد اقلام/پیام) |
| ۴ | دستیار هوشمند در هیچ صفحه‌ای دیده نمی‌شد | `VizitorViewModel.kt:311` | بخش «دستیار هوشمند فروش» + دکمهٔ واقعی |
| ۵ | تشخیص عیب‌یابی فقط در کد بود، نه در دسترس کاربر | `sqldirect/SqlDiagnostics.kt:37` | دکمهٔ تشخیص در دیالوگ هشدار |
| ۶ | نکتهٔ خطای نوع در `VizitorGateway` (کشف در بیلد ۲٫۱۸٫۰) | `sqldirect/VizitorGateway.kt:423` | نوع کلید نقشهٔ انبار `Map<Long, Int>` شد (در `c3fe97f` رفع و منتشر شد) |

> نکتهٔ صادقانه: موارد ۶ و بخشی از ۳ مربوط به دور پیشین‌اند و در همین سند فقط
> برای کامل‌بودن زنجیرهٔ تحلیل ثبت شده‌اند.

---

## ۵) دقیقاً چه چیزی به سبک عکس‌های مرجع بازسازی شد

| عنصر در عکس مرجع | معادل در برنامه (۲٫۱۹٫۰) |
|---|---|
| نوار بالا: عنوان طلایی + فلش + سه کره (فیلتر/هشدار/جست‌وجو) | `ui/components/MaChrome.kt` → `MaTopBar` + `MaOrbButton` (با شمارندهٔ هشدار) |
| نوار شش بخش افقی با کاشی فعال طلایی | `MaNavStrip` (در `MaConnectScreen` به شش مسیر واقعی برنامه وصل است) |
| تیتر قهرمان + زیرنویس حروف‌فاصله‌دار | `MaHeroTitle` (نسخهٔ ۲٫۱۷، بازاستفاده شد) |
| «نبض کسب‌وکار» با نوار سه‌رنگ | `MaPulseCard` + ردیف‌های `MaPulseRow` — این‌بار با دادهٔ واقعی اتصال |
| کارت طلایی «ساخت چارت» | `MaGoldCta` (متن راست، دکمهٔ مربع‌گرد چپ) |
| کارت‌های شاخص (مشتریان/گردش مالی/چک‌ها/کالاها) | گرید ۲×۲ از `MaMetricCard` (مشتری/کالا/انبار/ویزیتور مجاز) |
| قرص‌های بخش (دریافت/پرداخت/روزانه/تحلیل) | `MaSegmentPills` (فیلتر اقلام: همه / موجودی کافی / کم‌موجود) |
| نوار نازک «نمایش ۱۰ مورد اخیر» | `MaThinBar` (سهمیهٔ اسناد، اعتبار، مشتریان همگام‌شده) |
| کارت سند «MEELANO REPORTS» با ردیف برچسب—مقدار | `MaDocCard` (گزارش اتصال + کارت پیش‌فاکتور) |
| کاشی‌های رنگی Word/Excel/PDF/چاپ/اشتراک | `MaToolGrid` + `MaToolTile` — با کارهای واقعی (اشتراک، تصویر، PDF، Word، گزارش‌ها) |
| برگهٔ «تنظیمات هوشمند» با گروه‌های بازشو | `MaSmartSheet` + `MaSmartGroup` + `MaSmartTile` (۱۰ کار واقعی) |
| «دستیار هوشمند پسته» | بخش «دستیار هوشمند فروش» در صفحهٔ ارسال فاکتور (وصل‌شده) |
| امضای «Meelano Studio Design · Milad Yaghoobi» | `MeelanoPill`/`MilanoFooter` (هر دو صفحه) |

---

## ۶) گام‌های تأیید (چیزی که می‌توانید خودتان بیازمایید)
1. **اتصال:** برنامه را باز کنید → «تنظیمات هوشمند» (کرهٔ چپ) یا کارت طلایی →
   آدرس `192.168.1.150`، پورت `1433`، دیتابیس `Meelano`، کاربر `vizitor_android` →
   «تست اتصال» → «ذخیرهٔ امن» → «ورود سریع». انتظار: کرهٔ وضعیت «وصل» و
   ردیف‌های «نبض اتصال» سبز می‌شوند و «زمان پاسخ سرور» عدد نشان می‌دهد.
2. **ورود ویزیتور:** دکمهٔ «فهرست ویزیتورها (sys_vis)» → انتخاب ویزیتور →
   ورود خودکار به پنل؛ ردیف «ورود ویزیتور» سبز می‌شود و «اختیارات ویزیتور»
   تعداد مشتری/کالا/انبار را نشان می‌دهد.
3. **ارسال فاکتور:** ویترین → افزودن کالا → سبد → انتخاب مشتری → امضا →
   **با کلید «ثبت واقعی» خاموش** → دکمهٔ طلایی. انتظار: کارت «نتیجهٔ سند» با
   برچسب «پیش‌نمایش امن» و دکمهٔ «نمایش متن پیش‌نمایش».
4. **فعال‌کردن ثبت واقعی:** ابتدا در ERP یک پیش‌فاکتور ثبت شود، سپس کوئری
   `SELECT TOP 5 * FROM dbo.sailfact_pish ORDER BY shfacfo DESC;` را اجرا کنید؛
   اگر ردیف داشت، کلید «ثبت واقعی» را روشن کنید و سند را بفرستید — انتظار:
   «شمارهٔ سند (shfacfo)» و «اقلام نوشته‌شده» پر می‌شود.
5. **خروجی‌ها:** روی کاشی‌های «اشتراک / تصویر / PDF / Word» بزنید — فایل یا متن
   سند از طریق اندروید به اشتراک گذاشته می‌شود.
6. **تشخیص عیب:** کرهٔ هشدار (مثلث) → «اجرای تشخیص کامل» → فهرست درایور/پورت/
   دیتابیس/جدول‌ها با رنگ سبز یا قرمز.

---

## ۷) محدودیت‌های صادقانه
* آزمون روی گوشی و شبکهٔ داخلی شما انجام نشده است؛ بیلد در CI انجام و محتوای APK
  بازرسی می‌شود، اما اجرای واقعی روی دستگاه با شماست.
* «ثبت واقعی» تا وقتی پیش‌فاکتور مرجع در ERP نباشد، عمداً خاموش می‌ماند.
* کسورات (تخفیف) در این نسخه صفر است — همان تصمیم قبلی کارفرما.
* ثبت مشتری جدید همچنان محلی و «در انتظار تأیید حسابداری» است.
* تصاویر کالا (شیرینی/نوشیدنی) به‌صورت نگاشت نام انجام می‌شود.

طراحی و برنامه‌نویسی: **میلاد یقوبی** — **Meelano Studio Design**
