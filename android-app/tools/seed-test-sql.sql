-- ═══════════════════════════════════════════════════════════════════════════
--  اسکیمای آزمونِ اتصال — فقط برای تست خودکار در CI
--
--  ⚠ این اسکیما «شبیه‌سازی» است و از روی ستون‌هایی ساخته شده که خودِ برنامهٔ
--  اندروید می‌خواند (MeelanoDataSource.kt). هدف: اثبات این‌که
--    ۱) درایور/نشانی JDBC و کاربر db_datareader کار می‌کنند،
--    ۲) همین دستورهای T-SQL روی یک SQL Server واقعی اجرا می‌شوند،
--    ۳) هیچ مجوز اضافه‌ای جز همان‌هایی که نصب‌کننده می‌دهد لازم نیست.
--  این اسکیما جای دیتابیس واقعی Meelano را نمی‌گیرد و هیچ ادعایی دربارهٔ
--  اسکیمای واقعی ERP ندارد.
-- ═══════════════════════════════════════════════════════════════════════════

IF DB_ID(N'Meelano') IS NULL
    CREATE DATABASE [Meelano];
GO
USE [Meelano];
GO

-- ── کاربران سامانه (همان ستون‌هایی که کوئری ورود می‌خواند) ────────────────────
IF OBJECT_ID(N'dbo.sys_users') IS NULL
CREATE TABLE dbo.sys_users (
    user_id       INT           NOT NULL PRIMARY KEY,
    user_name     NVARCHAR(50)  NOT NULL,
    user_fname    NVARCHAR(50)  NULL,
    user_lname    NVARCHAR(50)  NULL,
    role_id       INT           NULL,
    active        BIT           NOT NULL DEFAULT 1,
    IsLocked      BIT           NULL,
    shmo          INT           NOT NULL DEFAULT 0,
    user_password VARBINARY(50) NULL      -- در ERP واقعی: متن ساده داخل varbinary
);
GO
-- رمزها مثل ERP واقعی: بایت‌های متن ساده (varbinary) و مقایسه با CONVERT(varchar)
DELETE FROM dbo.sys_users;
INSERT INTO dbo.sys_users (user_id, user_name, user_fname, user_lname, role_id, active, IsLocked, shmo, user_password)
VALUES (11, N'vizitor1', N'ویزیتور', N'یک', 5, 1, NULL, 1, CONVERT(VARBINARY(50), '1')),
       (12, N'vizitor2', N'ویزیتور', N'دو', 5, 0, NULL, 1, CONVERT(VARBINARY(50), '2')),
       (13, N'lockuser', N'کاربر', N'قفل', 5, 1, 1, 1, CONVERT(VARBINARY(50), '3'));
GO

-- ── دامنهٔ دسترسی ویزیتور ───────────────────────────────────────────────────
IF OBJECT_ID(N'dbo.sys_vis') IS NULL
CREATE TABLE dbo.sys_vis (UserID INT NOT NULL, shvis INT NOT NULL, SysID INT NOT NULL);
GO
IF OBJECT_ID(N'dbo.visitors') IS NULL
CREATE TABLE dbo.visitors (
    vis_rdf INT NOT NULL PRIMARY KEY, vis_name NVARCHAR(100) NULL, active BIT NOT NULL DEFAULT 1,
    VIs_region INT NULL, vis_city INT NULL);
GO
IF OBJECT_ID(N'dbo.sys_cus') IS NULL
CREATE TABLE dbo.sys_cus (UserID INT NOT NULL, Shmo INT NOT NULL, SysID INT NOT NULL);
GO
IF OBJECT_ID(N'dbo.sys_kal') IS NULL
CREATE TABLE dbo.sys_kal (UserID INT NOT NULL, Shka BIGINT NOT NULL, SysID INT NOT NULL);
GO
IF OBJECT_ID(N'dbo.sys_anb') IS NULL
CREATE TABLE dbo.sys_anb (UserID INT NOT NULL, Rdf_Anbar INT NOT NULL, SysID INT NOT NULL);
GO
DELETE FROM dbo.sys_vis; DELETE FROM dbo.visitors; DELETE FROM dbo.sys_cus;
DELETE FROM dbo.sys_kal; DELETE FROM dbo.sys_anb;
INSERT INTO dbo.visitors (vis_rdf, vis_name, active, VIs_region, vis_city)
VALUES (101, N'ویزیتور یک', 1, 7, 21);
INSERT INTO dbo.sys_vis (UserID, shvis, SysID) VALUES (11, 101, 1);
INSERT INTO dbo.sys_cus (UserID, Shmo, SysID) VALUES (11, 5001, 1), (11, 5002, 1), (11, 5003, 1);
INSERT INTO dbo.sys_kal (UserID, Shka, SysID) VALUES (11, 9001, 1), (11, 9002, 1);
INSERT INTO dbo.sys_anb (UserID, Rdf_Anbar, SysID) VALUES (11, 1, 1);
GO

-- ── مشتریان و گروه‌های مشتری (تیر قیمت) ─────────────────────────────────────
IF OBJECT_ID(N'dbo.custgroup') IS NULL
CREATE TABLE dbo.custgroup (
    group_rdf INT NOT NULL PRIMARY KEY, group_name NVARCHAR(100) NULL,
    price INT NULL, Active BIT NOT NULL DEFAULT 1);
GO
IF OBJECT_ID(N'dbo.CUSTOMERS') IS NULL
CREATE TABLE dbo.CUSTOMERS (
    SHMO INT NOT NULL PRIMARY KEY, MONAME NVARCHAR(200) NULL, code NVARCHAR(50) NULL,
    group_rdf INT NULL, rdf_city INT NULL, addre NVARCHAR(300) NULL, cell NVARCHAR(50) NULL,
    cred BIGINT NULL, man BIGINT NULL, black_list INT NULL, active CHAR(1) NOT NULL DEFAULT 't',
    Lat FLOAT NULL, Lng FLOAT NULL, vis_rdf INT NULL);
GO
DELETE FROM dbo.CUSTOMERS; DELETE FROM dbo.custgroup;
INSERT INTO dbo.custgroup (group_rdf, group_name, price, Active)
VALUES (1, N'همکار', 2, 1), (2, N'مصرف‌کننده', 5, 1), (3, N'غیرفعال', 1, 0);
INSERT INTO dbo.CUSTOMERS (SHMO, MONAME, code, group_rdf, rdf_city, addre, cell, cred, man, black_list, active, Lat, Lng, vis_rdf)
VALUES (5001, N'فروشگاه الف', N'C001', 1, 21, N'تهران', N'09120000001', 0, 1500000, 0, 't', 35.7, 51.4, 101),
       (5002, N'فروشگاه ب',  N'C002', 2, 21, N'کرج',  N'09120000002', 0, 250000,  0, 't', 35.8, 50.9, 101),
       (5003, N'مشتری غیرفعال', N'C003', 2, 21, N'قم', N'09120000003', 0, 0, 0, 'f', NULL, NULL, 101);
GO

-- ── کالا، قیمت‌ها، موجودی انبار ─────────────────────────────────────────────
IF OBJECT_ID(N'dbo.inventory') IS NULL
CREATE TABLE dbo.inventory (
    shka BIGINT NOT NULL PRIMARY KEY, naka NVARCHAR(200) NULL, coka NVARCHAR(50) NULL,
    group_rdf INT NULL, vahsanj NVARCHAR(50) NULL, mojkavah FLOAT NULL, mojkajoz INT NULL,
    vahsp FLOAT NULL, tedbastebandi FLOAT NULL, ExpirationDate NVARCHAR(20) NULL, active CHAR(1) NOT NULL DEFAULT 't');
GO
IF OBJECT_ID(N'dbo.forosh_price') IS NULL
CREATE TABLE dbo.forosh_price (
    shka BIGINT NOT NULL PRIMARY KEY, forosh1 BIGINT NULL, forosh2 BIGINT NULL, forosh3 BIGINT NULL,
    forosh4 BIGINT NULL, forosh5 BIGINT NULL, MinPrice BIGINT NULL, MaxPrice BIGINT NULL,
    active CHAR(1) NOT NULL DEFAULT 't');
GO
DELETE FROM dbo.inventory; DELETE FROM dbo.forosh_price;
INSERT INTO dbo.inventory (shka, naka, coka, group_rdf, vahsanj, mojkavah, mojkajoz, vahsp, tedbastebandi, ExpirationDate, active)
VALUES (9001, N'روغن آفتابگردان', N'K001', 1, N'حلب', 120.5, 3, 16.0, 6.0, NULL, 't'),
       (9002, N'برنج ایرانی', N'K002', 1, N'کیلو', 300.0, 0, 1.0, 10.0, NULL, 't');
INSERT INTO dbo.forosh_price (shka, forosh1, forosh2, forosh3, forosh4, forosh5, MinPrice, MaxPrice, active)
VALUES (9001, 1500000, 1450000, 1400000, 1350000, 1300000, 1200000, 1600000, 't'),
       (9002, 2500000, 2450000, 2400000, 2350000, 2300000, 2200000, 2600000, 't');
GO

-- ویو موجودی انبارها (همان ستون‌هایی که برنامه می‌خواند)
IF OBJECT_ID(N'dbo.VW_InventoryAnbars') IS NULL
EXEC('CREATE VIEW dbo.VW_InventoryAnbars AS
      SELECT i.shka, a.rdf_anbars, a.name, i.mojkavah, i.mojkajoz,
             CAST(i.mojkavah - 5 AS FLOAT) AS MojodiPish_vah,
             CAST(i.mojkajoz AS INT)       AS MojodiPish_joz
        FROM dbo.inventory i
        CROSS JOIN (SELECT 1 AS rdf_anbars, N''انبار مرکزی'' AS name) a');
GO

-- ── مسیر پیش‌فاکتور (فقط برای بررسی سلامت — بدون درج) ────────────────────────
IF OBJECT_ID(N'dbo.sailfact_pish') IS NULL
CREATE TABLE dbo.sailfact_pish (
    shfacfo BIGINT NOT NULL, rdf__ INT NOT NULL, active CHAR(1) NOT NULL DEFAULT 't',
    ismodify CHAR(1) NOT NULL DEFAULT 'f');
GO
IF OBJECT_ID(N'dbo.subsailfact_pish') IS NULL
CREATE TABLE dbo.subsailfact_pish (
    RDF INT NOT NULL, shfacfo BIGINT NOT NULL, rdf__ INT NOT NULL, SHKA BIGINT NULL,
    TEDVAH FLOAT NULL, TEDJOZ INT NULL, VAHPRICE BIGINT NULL, JOZPRICE BIGINT NULL,
    LINESUM BIGINT NULL, litakhma BIGINT NULL, PERTAFIF FLOAT NULL, Tax BIGINT NULL,
    Avarez BIGINT NULL, active CHAR(1) NOT NULL DEFAULT 't');
GO
IF OBJECT_ID(N'dbo.subsailtemp_pish') IS NULL
CREATE TABLE dbo.subsailtemp_pish (
    mod INT NULL, shfacfo BIGINT NULL, rdf__ INT NULL, rdf INT NULL, shka BIGINT NULL,
    rdf_anbar INT NULL, tedvah FLOAT NULL, tedjoz INT NULL, vahprice BIGINT NULL,
    jozprice BIGINT NULL, bastebandi NVARCHAR(30) NULL, tedbastebandi FLOAT NULL, linesum BIGINT NULL,
    isret BIT NULL, pertafif FLOAT NULL, pervis FLOAT NULL, litakhma BIGINT NULL,
    active CHAR(1) NULL, amani BIT NULL, Pavarez FLOAT NULL, Avarez BIGINT NULL,
    Ptax FLOAT NULL, Tax BIGINT NULL, PerPromotion FLOAT NULL, modpar INT NULL);
GO
-- پروسیجر سربرگ: امضای ۲۶ پارامتری، دقیقاً به همان ترتیبی که اپ صدا می‌زند
-- (doc: docs/write-path/ERP-WRITE-PROCEDURES.md §1). بدنه فقط نقش «ساخت سربرگ و
--  بازگرداندن شماره» را بازی می‌کند؛ منطق واقعی ERP این‌جا شبیه‌سازی شده است.
IF OBJECT_ID(N'dbo.add_sail_pish') IS NULL
EXEC(N'CREATE PROCEDURE dbo.add_sail_pish
    @date NVARCHAR(20), @shmo INT, @barbari BIGINT, @tozih NVARCHAR(300), @vis_rdf INT,
    @sumlineall BIGINT, @all BIGINT, @gainall BIGINT, @tafif BIGINT, @jamtakhgh BIGINT,
    @done_date NVARCHAR(20), @user NVARCHAR(50), @panevis NVARCHAR(200), @rdf_sarbarg INT,
    @rdf_tahbarg INT, @modpar INT, @ph_kh INT, @mod INT, @mod_darsad_vis INT, @nah_par INT,
    @sh_fac BIGINT, @id_en BIGINT OUTPUT, @ted_rooz INT, @sysid INT, @tax BIGINT, @avarez BIGINT
AS
BEGIN
    SET NOCOUNT ON;
    IF @mod <> 1 RETURN;                      -- مثل پروسیجر واقعی: mod=1 یعنی درج
    DECLARE @new BIGINT = (SELECT ISNULL(MAX(shfacfo), 0) + 1 FROM dbo.sailfact_pish);
    INSERT INTO dbo.sailfact_pish (shfacfo, rdf__) VALUES (@new, 1);
    SET @id_en = @new;
END');
GO
-- تریگر سطر میانی: در ERP واقعی همین تریگر سطر «واقعی» پیش‌فاکتور را می‌سازد.
--  این‌جا همان کار انجام می‌شود تا آزمون، کل مسیر نوشتن را بسنجد: اپ یک سطر در
--  جدول میانی درج می‌کند و تریگر آن را به dbo.subsailfact_pish می‌برد.
IF OBJECT_ID(N'dbo.trig_sst_pish') IS NULL
EXEC(N'CREATE TRIGGER dbo.trig_sst_pish ON dbo.subsailtemp_pish INSTEAD OF INSERT AS
BEGIN
    SET NOCOUNT ON;
    INSERT INTO dbo.subsailtemp_pish
        (mod, shfacfo, rdf__, rdf, shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice,
         bastebandi, tedbastebandi, linesum, isret, pertafif, pervis, litakhma, active,
         amani, Pavarez, Avarez, Ptax, Tax, PerPromotion, modpar)
    SELECT mod, shfacfo, rdf__, rdf, shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice,
           bastebandi, tedbastebandi, linesum, isret, pertafif, pervis, litakhma, active,
           amani, Pavarez, Avarez, Ptax, Tax, PerPromotion, modpar
      FROM inserted;
    INSERT INTO dbo.subsailfact_pish
        (RDF, shfacfo, rdf__, SHKA, TEDVAH, TEDJOZ, VAHPRICE, JOZPRICE, LINESUM, litakhma,
         PERTAFIF, Tax, Avarez, active)
    SELECT RDF, shfacfo, rdf__, SHKA, TEDVAH, TEDJOZ, VAHPRICE, JOZPRICE, LINESUM, litakhma,
           PERTAFIF, Tax, Avarez, active
      FROM inserted;
END');
GO

-- ── کاربر محدود، دقیقاً با همان دسترسی‌هایی که نصب‌کننده می‌دهد ─────────────
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'vizitor_android')
    CREATE LOGIN [vizitor_android] WITH PASSWORD = N'Viz!tor-Android-2026', CHECK_POLICY = OFF;
GO
USE [Meelano];
GO
IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N'vizitor_android')
    CREATE USER [vizitor_android] FOR LOGIN [vizitor_android];
IF NOT EXISTS (SELECT 1 FROM sys.database_role_members rm
               JOIN sys.database_principals r ON r.principal_id = rm.role_principal_id
               JOIN sys.database_principals u ON u.principal_id = rm.member_principal_id
               WHERE r.name = 'db_datareader' AND u.name = 'vizitor_android')
    ALTER ROLE [db_datareader] ADD MEMBER [vizitor_android];
GRANT EXECUTE ON dbo.add_sail_pish TO [vizitor_android];
GRANT INSERT ON dbo.subsailtemp_pish TO [vizitor_android];
GO

-- گزارشی از آنچه ساخته شد (برای لاگ CI)
USE [Meelano];
SELECT N'databases' AS what, (SELECT COUNT(*) FROM sys.databases WHERE database_id > 4) AS n
UNION ALL SELECT N'sys_users', COUNT(*) FROM dbo.sys_users
UNION ALL SELECT N'CUSTOMERS', COUNT(*) FROM dbo.CUSTOMERS
UNION ALL SELECT N'inventory', COUNT(*) FROM dbo.inventory
UNION ALL SELECT N'customer of vizitor1', COUNT(*) FROM dbo.sys_cus WHERE UserID = 11;
-- نسخهٔ SQL Server (برای ثبت در گزارش)
SELECT SERVERPROPERTY('ProductVersion') AS product_version, SERVERPROPERTY('ProductLevel') AS level;
-- شنونده‌های TCP (همان چیزی که اتصال اندروید به آن وابسته است)
SELECT port, type_desc, state_desc, ip_address FROM sys.dm_tcp_listener_states ORDER BY port, ip_address;
GO
