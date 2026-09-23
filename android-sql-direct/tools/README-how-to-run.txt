چطور این ممیزی‌ها را اجرا کنیم (Vizitor / Meelano)
==================================================

این پوشه باید شامل این فایل‌ها باشد (اگر پوشه وجود ندارد، اول بسازش:
    New-Item -ItemType Directory -Force C:\vizitor_audit):
    run_audit.bat
    02_fill_gaps.sql
    03_dump_proc_bodies.sql
    03b_bodies_file.sql
    04_port_check.sql
    05_gaps_small.sql
    08_which_db.sql

    اگر می‌خواهی همهٔ بررسی‌ها روی دیتابیس دیگری اجرا شود (مثلاً Atiran14050603):
        run_audit.bat Atiran14050603

راه ۱ (پیشنهادی — همه‌چیز با یک بار):
    ۱) فایل‌ها را در یک پوشه بگذار، مثلا  C:\vizitor_audit\
    ۲) در File Explorer روی  run_audit.bat  راست‌کلیک کن → "Run as administrator"
    ۳) صبر کن تا تمام شود؛ در همین پوشه این فایل‌ها ساخته می‌شوند:
         out_02_gaps.txt ، out_03_bodies.txt ، out_04_port.txt ، out_05_small.txt
    ۴) همان چهار فایل .txt را بفرست.

    ⚠️ مهم: متن فایل run_audit.bat را داخل PowerShell یا SSMS کپی نکن.
       این فایل باید اجرا شود (دابل‌کلیک یا Run as administrator)، نه کپی.
       اگر متنش را در PowerShell بچسبانی، خطاهایی مثل
       «REM: The term 'REM' is not recognized» می‌گیری: بی‌خطرند ولی هیچ کاری
       هم انجام نمی‌دهند.

راه ۲ (اگر run_audit.bat گفت sqlcmd پیدا نشد — بدون نیاز به sqlcmd):
    ۱) در SSMS فایل  03b_bodies_file.sql  را باز کن (روی دیتابیس Meelano)
    ۲) *قبل از* F5 کلیدهای  Ctrl+Shift+F  را بزن
       (Query → Results To → Results to File)
    ۳) F5 را بزن؛ SSMS نام فایل می‌پرسد →  out_03_bodies.txt  بگذار
    ۴) همین کار را برای  05_gaps_small.sql  تکرار کن →  out_05_small.txt

راه ۳ (اگر با همان PowerShell راحت‌تری — این‌ها دستور واقعی‌اند و کپی‌شان اشکالی ندارد):
    Get-Command sqlcmd                 # اول ببین sqlcmd نصب است یا نه
    cd C:\vizitor_audit
    sqlcmd -S localhost -d Meelano -E -i 03_dump_proc_bodies.sql -o out_03_bodies.txt -y 0 -W
    sqlcmd -S localhost -d Meelano -E -i 05_gaps_small.sql       -o out_05_small.txt  -y 0 -W
    sqlcmd -S localhost -d Meelano -E -i 02_fill_gaps.sql        -o out_02_gaps.txt   -y 0 -W

هیچ‌کدام از این اسکریپت‌ها چیزی در دیتابیس نمی‌نویسند (فقط خواندن).
فایل‌های .sql را فقط در SSMS اجرا کن.

راه ۴ (کاملاً بدون فایل — وقتی نه پوشه داری و نه فایل sql؛ فقط sqlcmd نصب است)
    اول ببین sqlcmd هست:      Get-Command sqlcmd
    بعد این یک خط را کپی کن (کل خط را یکجا؛ پوشه را می‌سازد و برای هر پروسیجر
    یک فایل جدا می‌نویسد):

    New-Item -ItemType Directory -Force C:\vizitor_audit | Out-Null; foreach ($p in 'add_sail_pish','AddInvoice','new_cust','FixMojodi') { sqlcmd -S localhost -d Meelano -E -h -1 -Q "SET NOCOUNT ON; SELECT m.definition FROM sys.sql_modules m JOIN sys.objects o ON o.object_id = m.object_id WHERE o.name = '$p'" -o "C:\vizitor_audit\body_$p.txt" -y 0 -W }

    نتیجه:
        C:\vizitor_audit\body_add_sail_pish.txt
        C:\vizitor_audit\body_AddInvoice.txt
        C:\vizitor_audit\body_new_cust.txt
        C:\vizitor_audit\body_FixMojodi.txt
    کنترل کن که خالی نباشند:
        Get-ChildItem C:\vizitor_audit | Select-Object Name, Length

    نکته: خطای  Sqlcmd: 'xxx.sql': Invalid filename  یعنی sqlcmd فایل را در
    «پوشهٔ جاری» می‌گردد (مثلاً C:\Windows\System32). یا اول  cd  کن به پوشهٔ
    فایل‌ها، یا مسیر کامل بده، یا از همین راه ۴ استفاده کن که فایل لازم ندارد.

راه ۵ (اسکریپت 08 — تعیین این‌که کدام دیتابیس «زنده» است؛ خیلی مهم)
    آن ستون‌هایی که از سه جدول فرستادی از دیتابیس  Atiran14050603  بود، ولی همهٔ
    بررسی‌های قبلی (ورود، تعداد رکوردها، بدنهٔ پروسیجرها) در دیتابیس  Meelano  انجام
    شده. اپ باید همان جایی بنویسد که خود ERP می‌نویسد؛ پس این باید با شاهد روشن شود.

    روش:
    ۱) 08_which_db.sql  را در SSMS باز کن.
    ۲) در نوار بالا («Available Databases») دیتابیس  Meelano  را انتخاب کن و F5.
    ۳) خروجی را بفرست (چند جدول کوچک است).
    ۴) بعد همان فایل را با دیتابیس  Atiran14050603  انتخاب‌شده هم اجرا کن و بفرست.
    ستون  last_write  نشان می‌دهد آخرین بار چه زمانی در آن دیتابیس نوشته شده؛
    دیتابیس زنده تاریخ امروز را دارد.

راه ۶ (مستقیم در SSMS — بدون فایل، بدون حلقه؛ فقط کپی کن و F5)
    هر دستور را در یک پنجرهٔ جدید SSMS بچسبان. دیتابیس انتخاب‌شده در نوار بالا مهم است.

    الف) فهرست و طول متن پروسیجرها/ویویی که هنوز لازم است (خروجی کوچک، اول این را بفرست):
        SELECT o.name, o.type_desc, LEN(m.definition) AS chars
        FROM sys.sql_modules AS m
        JOIN sys.objects AS o ON o.object_id = m.object_id
        WHERE o.name IN ('Edit_sail_pish','AddFromAtiranDetailsForVisitors',
                         'SelectPriceAndTedvahForushVisitorhaByDate','FixManCustomer',
                         'UpdateMojodiInventory','UpdateMojodiInventoryAnbars',
                         'UpdateMojodiInventoryAnbarsPS','VW_InventoryAnbars')
        ORDER BY o.name;

    ب) متن کامل همان پروسیجرها/ویو (همان WHERE را نگه دار، فقط ستون‌ها را عوض کن):
        SELECT o.name, m.definition
        FROM sys.sql_modules AS m
        JOIN sys.objects AS o ON o.object_id = m.object_id
        WHERE o.name IN ('Edit_sail_pish','AddFromAtiranDetailsForVisitors',
                         'SelectPriceAndTedvahForushVisitorhaByDate','FixManCustomer',
                         'UpdateMojodiInventory','UpdateMojodiInventoryAnbars',
                         'UpdateMojodiInventoryAnbarsPS','VW_InventoryAnbars')
        ORDER BY o.name;
        برای کپی یک خانه: روی خانه کلیک کن، Ctrl+A، Ctrl+C
        برای متن‌های بزرگ: Query -> Results To -> Results to File و بعد F5

    پ) کدام دیتابیس زنده است؟ (بدون فایل)
        SELECT d.name AS db, MAX(s.last_user_update) AS last_write
        FROM sys.databases AS d
        LEFT JOIN sys.dm_db_index_usage_stats AS s ON s.database_id = d.database_id
        WHERE d.database_id > 4
        GROUP BY d.name
        ORDER BY last_write DESC;

        و سال مالی جاری: یک بار با Meelano انتخاب‌شده و یک بار با Atiran14050603:
        SELECT DB_NAME() AS db, rdf, name, nam_db, [Current] FROM dbo.sal_mali;

راه ۷ (مستقیم در SSMS — تنها سؤال باقی‌ماندهٔ نوشتن)
    معلوم شد نه add_sail_pish و نه Edit_sail_pish سطرهای subsailfact_pish را
    نمی‌نویسند. این دستور همهٔ ماژول‌های دیتابیس (پروسیجر، ویو، فانکشن، تریگر) را
    می‌گردد و می‌گوید چه چیزی این جدول‌ها را دست می‌زند:

        SELECT o.name, o.type_desc
        FROM sys.sql_modules AS m
        JOIN sys.objects AS o ON o.object_id = m.object_id
        WHERE m.definition LIKE '%subsailfact_pish%'
           OR m.definition LIKE '%subsailtemp%'
           OR m.definition LIKE '%SubSailSefaresh%'
        ORDER BY o.name;

    اگر خروجی خالی بود، یعنی خودِ برنامهٔ ERP (کلاینت) سطرها را مستقیم در جدول
    درج می‌کند و برای الگوی درج باید به مسیر دیگری فکر کنیم (مثلاً همان ستون‌های
    subsailtemp که ERP پر می‌کند).

    و اگر نام‌های دیگر هم پیدا شد، متنشان را با «راه ۶» بگیر (فقط اسم‌ها را در
    همان WHERE بگذار).

راه ۸ (مستقیم در SSMS — بستنِ آخرین حلقهٔ نوشتن)
    نتیجهٔ راه ۷ دوازده ماژول داد؛ دو تا از آن‌ها «تریگر» هستند و تریگر خودبه‌خود
    اجرا می‌شود — یعنی کسی لازم نیست صدایش بزند. به احتمال زیاد همان‌جا سطرهای
    subsailfact_pish درج می‌شوند.

    اول این (کوچک؛ می‌گوید هر تریگر روی کدام جدول است):

        SELECT t.name, OBJECT_NAME(t.parent_id) AS on_table, t.is_disabled, t.is_instead_of_trigger
        FROM sys.triggers AS t
        WHERE t.name IN ('trig_sst_pish','InvoiceTrigger');

    بعد متن این هشت ماژول (بقیه مثل CloseTheFiscalYear/RestoreToDefault لازم نیست):

        SELECT o.name, m.definition
        FROM sys.sql_modules AS m
        JOIN sys.objects AS o ON o.object_id = m.object_id
        WHERE o.name IN ('trig_sst_pish','InvoiceTrigger','pishfactor_body','subsailFactPish',
                         'CalcDetailsPishfactor','ListPishFactor','VW_Taraz_pish','VWDeatailspishFactorForush')
        ORDER BY o.name;

    (برای کپی متن‌های بزرگ: روی خانه کلیک → Ctrl+A → Ctrl+C، یا Results to File.)

راه ۹ (مستقیم در SSMS — سه پرسش کوچک، آخرین ابهام‌ها)
    نکته: هر سه کوئری خودشان DB_NAME() را چاپ می‌کنند؛ پس اگر نوار ابزار SSMS روی دیتابیس
    دیگری باشد، خودِ جواب لو می‌دهد.

    ۱) ستون‌های جدول میانی پیش‌فاکتور + جدول اقلام (نوع، نال‌پذیری، پیش‌فرض) — اپ باید
       همهٔ ستون‌های اجباری را بفرستد:

        SELECT DB_NAME() AS db, TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, DATA_TYPE,
               CHARACTER_MAXIMUM_LENGTH AS len, NUMERIC_PRECISION AS pr, NUMERIC_SCALE AS sc,
               IS_NULLABLE, COLUMN_DEFAULT
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME IN ('subsailtemp_pish','subsailfact_pish')
        ORDER BY TABLE_NAME, ORDINAL_POSITION;

    ۲) یک نمونهٔ واقعی از داده‌ی خودِ ERP (سه فاکتور آخر) — تا قرارداد
       tedvah/tedjoz/vahprice/jozprice/linesum را از داده یاد بگیریم نه از حدس:

        SELECT TOP 3 DB_NAME() AS db, * FROM sailfact ORDER BY shfacfo DESC;

        SELECT DB_NAME() AS db, * FROM subsailfact
        WHERE shfacfo IN (SELECT TOP 3 shfacfo FROM sailfact ORDER BY shfacfo DESC)
        ORDER BY shfacfo, rdf__, RDF;

    ۳) چه ماژولی «فاکتور» را از «پیش‌فاکتور» می‌سازد (کسی که sailfact را درج می‌کند یا
       shpish را پر می‌کند) — فقط نام و طول، متن بعداً:

        SELECT DB_NAME() AS db, o.name, o.type_desc, LEN(m.definition) AS len
        FROM sys.sql_modules AS m JOIN sys.objects AS o ON o.object_id = m.object_id
        WHERE m.definition LIKE '%shpish%'
           OR (m.definition LIKE '%insert into sailfact%' AND m.definition NOT LIKE '%sailfact_pish%')
        ORDER BY o.name;

راه ۱۰ (مستقیم در SSMS — همین چهار کوئری، دو بار: یک‌بار با Meelano، یک‌بار با Atiran14050603)
    چرا: جواب‌های راه ۹ خودشان ستون db را Atiran14050603 چاپ کردند؛ پس روشن نیست کدام
    دیتابیس دیتابیسِ اپ است. این چهار کوئری همان ابهام را می‌بندد.
    روش: در نوار ابزار SSMS (کادر بالا-چپ) دیتابیس را انتخاب کن، همین چهار کوئری را F5 کن،
    بعد دیتابیس را عوض کن و دوباره F5 کن. هر جواب خودش نام دیتابیس را چاپ می‌کند.

    ۱) فهرست دیتابیس‌های همین سرور:

        SELECT name, state_desc, create_date, compatibility_level
        FROM sys.databases WHERE database_id > 4 ORDER BY name;

    ۲) آیا اشیای کلیدی در «همین دیتابیسِ انتخاب‌شده» وجود دارند؟

        SELECT DB_NAME() AS db,
               OBJECT_ID('dbo.subsailtemp_pish') AS staging_pish,
               OBJECT_ID('dbo.subsailfact_pish') AS lines_pish,
               OBJECT_ID('dbo.trig_sst_pish')    AS trg_pish,
               OBJECT_ID('dbo.subsailtemp')      AS staging_invoice,
               OBJECT_ID('dbo.InvoiceTrigger')   AS trg_invoice,
               OBJECT_ID('dbo.add_sail_pish')    AS add_sail_pish,
               OBJECT_ID('dbo.AddInvoice')       AS add_invoice,
               OBJECT_ID('dbo.Edit_sail_pish')   AS edit_sail_pish;

    ۳) اندازهٔ واقعی داده در همین دیتابیس (کدام یکی دادهٔ زنده دارد؟ اگر جدولی نباشد،
       به‌جای خطا فقط در لیست نمی‌آید):

        SELECT DB_NAME() AS db, t.name, SUM(p.rows) AS rows_count
        FROM sys.tables AS t
        JOIN sys.partitions AS p ON p.object_id = t.object_id AND p.index_id IN (0,1)
        WHERE t.name IN ('sailfact','sailfact_pish','subsailfact','subsailfact_pish',
                         'subsailtemp','subsailtemp_pish','CUSTOMERS','inventory',
                         'sys_users','sal_mali','visitors')
        GROUP BY t.name ORDER BY t.name;

    ۴) ستون‌های دو جدول اقلام (همان کوئری راه ۹ بخش ۱) - برای مقایسهٔ دو دیتابیس:

        SELECT DB_NAME() AS db, TABLE_NAME, ORDINAL_POSITION, COLUMN_NAME, DATA_TYPE,
               CHARACTER_MAXIMUM_LENGTH AS len, NUMERIC_PRECISION AS pr, NUMERIC_SCALE AS sc,
               IS_NULLABLE, COLUMN_DEFAULT
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_NAME IN ('subsailtemp_pish','subsailfact_pish')
        ORDER BY TABLE_NAME, ORDINAL_POSITION;

راه ۱۱ (مستقیم در SSMS — «کدام دیتابیس؟» را خودش جواب می‌دهد)
    چرا: راه ۹ و راه ۱۰ هر دو با db = Atiran14050603 برگشتند؛ یعنی نوار ابزار SSMS عوض
    نشده. این کوئری لازم نیست نوار ابزار را عوض کنی: خودش همهٔ دیتابیس‌های همین سرور را
    می‌گردد و برای هرکدام می‌گوید چه اشیایی دارد، چه جدول‌هایی چند سطر دارند، و خودِ آن
    دیتابیس چه می‌گوید «دیتابیس سال مالی» است (sal_mali.nam_db).
    (همین متن در مخزن هم هست: sql/09_compare_databases.sql — ولی لازم نیست فایل بسازی؛
    همین را کپی کن.)

راه ۱۲ (دو نکتهٔ کوتاه، بدون کوئری جدید)
    * اسکریپت 08 حالا «بخش ۶» دارد: sal_mali همهٔ دیتابیس‌های همین سرور را می‌آورد تا
      معلوم شود کدام دیتابیس سطر [Current]=1 دارد - یعنی خودِ ERP کدام را «سال مالی جاری»
      می‌داند. (بدون تغییر دیتابیس نوار ابزار هم جواب می‌دهد.)
    * اگر خواستی run_audit.bat روی دیتابیس دیگری کار کند، اسمش را به‌عنوان آرگومان بده:

          run_audit.bat Atiran14050603

      بدون آرگومان، پیش‌فرض Meelano است.

راه ۱۳ (مستقیم در SSMS — «یک پیش‌فاکتور واقعی از خودِ ERP»)
    چرا: پنج مقدار سربرگ پیش‌فاکتور (ted_rooz, ph_kh, mod_darsad_vis, rdf_sarbarg, gainall)
    را هیچ‌جا نمی‌شود خواند، چون sailfact_pish در هر دو دیتابیس ۰ سطر است. این‌ها را فقط
    خودِ ERP وقتی پیش‌فاکتور را ذخیره می‌کند می‌نویسد.
    کار تو: در ماژول فروش خودِ ERP، برای یک ویزیتور یک پیش‌فاکتور بساز (یک مشتری + یک یا
    دو کالا) و ذخیره کن — «فاکتور» نکن، فقط پیش‌فاکتور بماند.
    بعد این فایل را در SSMS باز کن و F5:  sql/10_preinvoice_sample.sql
    (یا همین چهار کوئری را دستی بزن؛ خط اول خودش می‌گوید روی کدام دیتابیس اجرا شده.)

        SELECT DB_NAME() AS db,
               (SELECT COUNT(*) FROM dbo.sailfact_pish)    AS head_rows,
               (SELECT COUNT(*) FROM dbo.subsailfact_pish) AS line_rows;

        SELECT TOP 5 * FROM dbo.sailfact_pish ORDER BY shfacfo DESC;

        SELECT * FROM dbo.subsailfact_pish
         WHERE shfacfo = (SELECT MAX(shfacfo) FROM dbo.sailfact_pish)
         ORDER BY rdf__ DESC, RDF;

    اگر head_rows صفر برگشت یعنی پیش‌فاکتور ذخیره نشده (یا روی دیتابیس دیگری ذخیره شده)؛
    همان عدد را برایم بفرست تا مسیر را دقیق کنم.
