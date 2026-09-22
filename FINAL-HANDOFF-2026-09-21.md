# Vizitor Android — Final Direct SQL Handoff

Version: **2.14.1-direct**

## مسیر اصلی پروژه
برای ساخت APK از این پروژه استفاده شود:

`vizitor-app/`

## معماری اتصال

`Android UI → ViewModel → Repository/Direct DataSource → SqlConnectionManager → SQL Server TCP 1433`

هیچ IIS/API واسطی برای مسیر ERP و ثبت پیش‌فاکتور لازم نیست.

## ورود

1. آدرس SQL Server و پورت (پیش‌فرض 1433)
2. نام کاربری و رمز SQL Server
3. نمایش فهرست دیتابیس‌های قابل دسترس از `master`
4. انتخاب دیتابیس مقصد
5. تست اتصال
6. نام کاربری و رمز ویزیتور از `dbo.sys_users`
7. همگام‌سازی دامنهٔ مجاز کالا/مشتری/انبار و داده‌های مربوط

اطلاعات حساس در `SecureDbStore` با AES-GCM و Android Keystore ذخیره می‌شوند.

## ثبت پیش‌فاکتور

دکمهٔ سبد خرید مستقیماً مسیر واقعی ERP را اجرا می‌کند:

- `dbo.add_sail_pish`
- `dbo.subsailtemp_pish`
- `dbo.trig_sst_pish`
- بازخوانی و تطبیق تعداد خطوط و مبلغ ثبت‌شده

صدور فاکتور نهایی از ویزیتور عمداً فعال نیست.

## مجوز SQL

فایل زیر را با حساب مدیریتی روی دیتابیس مقصد اجرا کنید:

`android-sql-direct/sql/01_setup_vizitor_user.sql`

این اسکریپت برای کاربر `vizitor_android` این دسترسی‌ها را فراهم می‌کند:

- `db_datareader`
- `GRANT EXECUTE ON OBJECT::dbo.add_sail_pish`
- `GRANT INSERT ON OBJECT::dbo.subsailtemp_pish`

هیچ UPDATE/DELETE/DDL برای مسیر اپ اعطا نمی‌شود.

## دادهٔ آزمایشی

Seedهای ساختگی که در مسیر داشبورد/گردش حساب استفاده می‌شدند حذف یا غیرفعال شده‌اند. اپ نباید دادهٔ جعلی را به‌عنوان دادهٔ واقعی ERP نمایش دهد.

## Build

در محیط فعلی، Gradle 8.9 به‌صورت cache محلی موجود نبود و دانلود آن به دلیل محدودیت شبکه انجام نشد. گزارش دقیق در:

`android-sql-direct/docs/BUILD-VERIFICATION-2026-09-21.md`

## فایل مهم برای کالیبراسیون پیش‌فاکتور

`android-sql-direct/sql/10_preinvoice_sample.sql`

این فایل فقط خواندنی است و برای مقایسه با یک پیش‌فاکتور واقعی ERP استفاده می‌شود. مسیر فعلی بدون وجود نمونهٔ واقعی نیز با fallback محافظه‌کارانه کار می‌کند، اما پنج مقدار سربرگ می‌توانند با یک نمونهٔ واقعی دقیق‌تر شوند.
