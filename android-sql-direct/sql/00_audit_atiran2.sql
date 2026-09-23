/* ===========================================================================
   Vizitor - ERP database audit  -  v5  -  2026-09-18  -  READ ONLY
   ===========================================================================
   This script NEVER writes, NEVER drops, NEVER alters anything.
   It only reads system catalogs (sys.*) plus COUNT / MIN / MAX checks.

   ---------------------------------------------------------------------------
   NEW IN v5 (why this version exists)
     * v4 was saved as UTF-8 WITH BOM and SSMS reported
         "Msg 102 ... Incorrect syntax near '<invisible>'"  on the first batch,
       because the BOM byte became part of the code. This file is now pure
       ASCII - comments included - and has no BOM, so no encoding issue can
       ever affect execution again.
     * EVERY result is now PRINTed as text into the MESSAGES tab instead of
       appearing in a results grid, so you can collect the complete audit with
       one copy-paste (or one sqlcmd command) - no grid export needed.

   OUTPUT FORMAT
     One line per item, pipe separated, each line starts with its section:
       00|server info ...
       01|table            |exists/missing
       02|table            |column |type |nullable |PK/IDENTITY
       03|view name        |definition chunk no |chunk
       04|procedure        |exists |parameter signature
       05|procedure        |body chunk no |chunk
       06|password column  |type |length          (no values)
       06b|password column |rows |min/max length |first 3 characters only
       07|table            |row count        (+ 07|MISSING|table lines)
       08|object containing "Vizitor" |type
       09|Vizitor table    |approx rows
       10|discovered object|type             (ground truth for object names)
       11|SQL network / LoginMode registry values
       12|database on this instance |state |size

   HOW TO RUN (SSMS on the server)
     1. Open this file, select the ERP database (on this server: Meelano)
        in the database dropdown. This script does not care about the name,
        it audits whatever database is selected.
     2. Check the box: Query menu -> "Include Messages in Results" is not
        needed; just press F5.
     3. The Messages tab now contains the WHOLE audit. Send it back by:
          a) clicking inside the Messages tab, Ctrl+A, Ctrl+C, paste into a
             .txt file and attach it, OR
          b) running this in CMD on the server (writes the file for you):
               sqlcmd -S localhost -d Meelano -E -i 00_audit_atiran2.sql ^
                      -o C:\path\audit_output.txt -y 0 -W
             (-E = log in with the current Windows account, no password)
     4. The first line printed must read: "script version v5".

   SECTIONS
     00_SERVER            version / edition / instance / database context
     01_TABLES            every table the app plans to use: exists? schema?
     02_COLUMNS           full column list (type / length / nullable / PK)
     03_VIEWS             real definition of vwVizitor* / *Vizitor* views
     04_PROCS             stored procedures: exists? + parameter signature
     05_PROC_BODIES       procedure bodies, 200-character chunks
     06_PASSWORD_COLUMNS  login-password columns: name / type / length only
     06b_PW_SAMPLE        hash-scheme evidence (first 3 characters only)
     07_ROW_COUNTS        row count of every relevant table that exists
     08_VIZITOR_OBJECTS   all objects whose name contains "Vizitor"
     09_VIZITOR_COUNTS    approximate row counts of those tables
     10_NAME_SEARCH       all objects matching Atiran keywords

        (ground truth for the "no guessing" rule)
     11_SQL_NETWORK_CONFIG real TCP port / LoginMode (needs VIEW SERVER STATE)
     12_DATABASES         every non-system database on this instance
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor ERP audit - script version v5 (2026-09-18) - read-only, sections 00..12';
GO


/* ==== 00) Server identity ================================================= */
DECLARE @out NVARCHAR(MAX) = N'';
SET @out = @out + N'00|server_name      |' + CAST(SERVERPROPERTY('ServerName')   AS NVARCHAR(128)) + CHAR(10);
SET @out = @out + N'00|instance_name    |' + ISNULL(CAST(SERVERPROPERTY('InstanceName') AS NVARCHAR(128)), N'(default instance)') + CHAR(10);
SET @out = @out + N'00|machine_name     |' + CAST(SERVERPROPERTY('MachineName')  AS NVARCHAR(128)) + CHAR(10);
SET @out = @out + N'00|product_version  |' + CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(64)) + CHAR(10);
SET @out = @out + N'00|product_level    |' + CAST(SERVERPROPERTY('ProductLevel')   AS NVARCHAR(32)) + CHAR(10);
SET @out = @out + N'00|edition          |' + CAST(SERVERPROPERTY('Edition')        AS NVARCHAR(128)) + CHAR(10);
SET @out = @out + N'00|database         |' + DB_NAME() + CHAR(10);
SET @out = @out + N'00|compat_level     |' + CAST((SELECT d.compatibility_level FROM sys.databases d WHERE d.database_id = DB_ID()) AS NVARCHAR(4)) + CHAR(10);
SET @out = @out + N'00|db_collation     |' + (SELECT d.collation_name FROM sys.databases d WHERE d.database_id = DB_ID()) + CHAR(10);
SET @out = @out + N'00|CUSTOMERS_probe  |' + CASE WHEN OBJECT_ID(N'dbo.CUSTOMERS') IS NOT NULL THEN N'found'
                                              WHEN OBJECT_ID(N'dbo.customers') IS NOT NULL THEN N'found with different casing'
                                              ELSE N'NOT FOUND - wrong database?' END + CHAR(10);

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 01) Tables the app will use - existence ============================= */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'01|' + w.name + N'|'
            + CASE WHEN o.object_id IS NULL THEN N'MISSING'
                   ELSE o.type_desc + N' in schema ' + SCHEMA_NAME(o.schema_id) END + CHAR(10)
FROM (VALUES
        (N'sys_users'), (N'Roles'), (N'new_cust'), (N'ka_act'),
        (N'CUSTOMERS'), (N'custgroup'), (N'CITYS'), (N'Province'), (N'regions'),
        (N'inventory'), (N'kagroup'), (N'forosh_price'), (N'prizePercent'),
        (N'anbars'), (N'inventory_anbars'),
        (N'sailfact'), (N'subsailfact'), (N'sailfact_pish'), (N'subsailfact_pish'),
        (N'PishDaryaft'), (N'PishDaryaftGetCheck'), (N'PishDaryaftMultiFactor'), (N'PishDaryaftPos'),
        (N'visitors'), (N'sys_vis'), (N'sys_cus'), (N'sys_kal'), (N'sys_anb'),
        (N'masir'), (N'MasirDay'), (N'Visit'), (N'vis_goals'),
        (N'cust_act'), (N'TabletCustomer'), (N'Device'), (N'DeviceLocation'),
        (N'DeviceMessages'), (N'DeviceSettings'), (N'osystems'), (N'Company'),
        (N'getchk'), (N'BANK'), (N'BANK_NAME'), (N'overal_setting')
    ) AS w(name)
LEFT JOIN sys.objects o
       ON o.name COLLATE Latin1_General_CI_AS = w.name
      AND o.type IN (N'U', N'V', N'FN', N'IF', N'TF', N'P')
ORDER BY w.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 02) Full column detail of the relevant tables ========================
   02|table|column|type|nullable|PK / IDENTITY markers                       */
DECLARE @out NVARCHAR(MAX) = N'';

/* how many columns each table actually has (quick sanity line) */
SELECT @out = @out + N'02|' + t.name + N'|COLUMN_COUNT|' + CAST(COUNT(*) AS NVARCHAR(8)) + CHAR(10)
FROM sys.tables t
JOIN sys.columns c ON c.object_id = t.object_id
WHERE t.name COLLATE Latin1_General_CI_AS IN (N'sys_users', N'Roles', N'new_cust', N'ka_act',
        N'CUSTOMERS', N'custgroup', N'CITYS', N'Province', N'regions',
        N'inventory', N'kagroup', N'forosh_price', N'prizePercent',
        N'anbars', N'inventory_anbars',
        N'sailfact', N'subsailfact', N'sailfact_pish', N'subsailfact_pish',
        N'PishDaryaft', N'visitors', N'sys_vis', N'sys_cus',
        N'masir', N'MasirDay', N'Visit', N'vis_goals',
        N'cust_act', N'TabletCustomer', N'osystems', N'Company', N'getchk')
GROUP BY t.name;

/* one line per column */
SELECT @out = @out + N'02|' + t.name + N'|' + c.name + N'|' + ty.name + N'|'
            + ISNULL(CASE WHEN ty.name IN (N'nvarchar', N'nchar')
                               THEN CAST(c.max_length / 2 AS NVARCHAR(8))
                          WHEN ty.name IN (N'varchar', N'char', N'varbinary', N'binary')
                               THEN CASE WHEN c.max_length = -1 THEN N'MAX'
                                         ELSE CAST(c.max_length AS NVARCHAR(8)) END
                          WHEN ty.name IN (N'decimal', N'numeric')
                               THEN CAST(c.[precision] AS NVARCHAR(3)) + N',' + CAST(c.[scale] AS NVARCHAR(3))
                          ELSE N'' END, N'-') + N'|'
            + CASE WHEN c.is_nullable = 1 THEN N'NULL' ELSE N'NOT NULL' END
            + CASE WHEN c.is_identity = 1 THEN N'|IDENTITY' ELSE N'' END
            + CASE WHEN EXISTS (SELECT 1
                                  FROM sys.index_columns ic
                                  JOIN sys.indexes i ON i.object_id = ic.object_id
                                                    AND i.index_id  = ic.index_id
                                 WHERE ic.object_id = c.object_id
                                   AND ic.column_id  = c.column_id
                                   AND i.is_primary_key = 1) THEN N'|PK' ELSE N'' END
            + CHAR(10)
FROM sys.tables t
JOIN sys.columns c  ON c.object_id = t.object_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
WHERE t.name COLLATE Latin1_General_CI_AS IN (N'sys_users', N'Roles', N'new_cust', N'ka_act',
        N'CUSTOMERS', N'custgroup', N'CITYS', N'Province', N'regions',
        N'inventory', N'kagroup', N'forosh_price', N'prizePercent',
        N'anbars', N'inventory_anbars',
        N'sailfact', N'subsailfact', N'sailfact_pish', N'subsailfact_pish',
        N'PishDaryaft', N'visitors', N'sys_vis', N'sys_cus',
        N'masir', N'MasirDay', N'Visit', N'vis_goals',
        N'cust_act', N'TabletCustomer', N'osystems', N'Company', N'getchk')
ORDER BY t.name, c.column_id;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 03) Real definition of the Vizitor views ============================
   200-character chunks so nothing is lost by any output setting.            */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'03|' + v.name + N'|' + CAST(num.n AS NVARCHAR(4)) + N'|'
            + SUBSTRING(m.definition, (num.n - 1) * 200 + 1, 200) + CHAR(10)
FROM sys.views v
JOIN sys.sql_modules m ON m.object_id = v.object_id
CROSS JOIN (SELECT TOP (60) ROW_NUMBER() OVER (ORDER BY object_id) AS n FROM sys.all_objects) num
WHERE (v.name COLLATE Latin1_General_CI_AS LIKE N'vwVizitor%'
    OR v.name COLLATE Latin1_General_CI_AS LIKE N'%Vizitor%')
  AND num.n <= CEILING(LEN(ISNULL(m.definition, N'')) / 200.0)
ORDER BY v.name, num.n;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
IF LEN(@out) = 0 PRINT N'03|(no view with Vizitor in its name exists in this database)';
GO


/* ==== 04) Stored procedures - existence + parameter signature ============ */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'04|' + o.name + N'|'
            + CASE WHEN p.object_id IS NULL THEN N'MISSING'
                   ELSE N'exists in schema ' + ISNULL(SCHEMA_NAME(p.schema_id), N'?') END + N'|'
            + ISNULL((SELECT STUFF((SELECT N', ' + PP.name + N' ' + TYP.name
                                         + CASE WHEN TYP.is_user_defined <> 0 THEN N''
                                                WHEN PP.max_length = -1 THEN N'(MAX)'
                                                WHEN TYP.name IN (N'nvarchar', N'nchar')
                                                     THEN N'(' + CAST(PP.max_length / 2 AS NVARCHAR(8)) + N')'
                                                WHEN TYP.name IN (N'varchar', N'char', N'varbinary', N'binary')
                                                     THEN CASE WHEN PP.max_length IN (0, -1) THEN N''
                                                               ELSE N'(' + CAST(PP.max_length AS NVARCHAR(8)) + N')' END
                                                WHEN TYP.name IN (N'decimal', N'numeric')
                                                     THEN N'(' + CAST(PP.precision AS NVARCHAR(3)) + N','
                                                              + CAST(PP.scale     AS NVARCHAR(3)) + N')'
                                                ELSE N'' END
                                         + CASE WHEN PP.is_output = 1 THEN N' OUTPUT' ELSE N'' END
                                      FROM sys.parameters PP
                                      JOIN sys.types TYP ON TYP.user_type_id = PP.user_type_id
                                     WHERE PP.object_id = p.object_id
                                       AND PP.parameter_id > 0
                                     ORDER BY PP.parameter_id
                                       FOR XML PATH('')), 1, 2, N'')), N'(no parameters)') + CHAR(10)
FROM (VALUES (N'add_sail_pish'), (N'subsailfact_pish'), (N'AddInvoice'),
             (N'FixMojodi'), (N'sp_add_sail_pish'), (N'svcAddSailFactPish'),
             (N'add_sailfact'), (N'subsailfact'), (N'new_cust'), (N'add_new_cust'),
             (N'VizitorLogin'), (N'sp_VizitorLogin')) o(name)
LEFT JOIN sys.procedures p ON p.name COLLATE Latin1_General_CI_AS = o.name
ORDER BY o.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 05) Procedure bodies, 200-character chunks ========================= */
DECLARE @out NVARCHAR(MAX) = N'';

SELECT @out = @out + N'05|' + p.name + N'|0|'
            + CASE WHEN m.definition IS NULL
                   THEN N'<no definition available (encrypted or native)>'
                   ELSE CAST(LEN(m.definition) AS NVARCHAR(16)) + N' characters' END + CHAR(10)
FROM sys.procedures p
LEFT JOIN sys.sql_modules m ON m.object_id = p.object_id
WHERE p.name COLLATE Latin1_General_CI_AS IN (N'add_sail_pish', N'subsailfact_pish', N'AddInvoice',
        N'FixMojodi', N'sp_add_sail_pish', N'svcAddSailFactPish', N'add_sailfact',
        N'subsailfact', N'new_cust', N'add_new_cust')
ORDER BY p.name;

SELECT @out = @out + N'05|' + p.name + N'|' + CAST(num.n AS NVARCHAR(4)) + N'|'
            + SUBSTRING(m.definition, (num.n - 1) * 200 + 1, 200) + CHAR(10)
FROM sys.procedures p
JOIN sys.sql_modules m ON m.object_id = p.object_id
CROSS JOIN (SELECT TOP (40) ROW_NUMBER() OVER (ORDER BY object_id) AS n FROM sys.all_objects) num
WHERE p.name COLLATE Latin1_General_CI_AS IN (N'add_sail_pish', N'subsailfact_pish', N'AddInvoice',
        N'FixMojodi', N'sp_add_sail_pish', N'svcAddSailFactPish', N'add_sailfact',
        N'subsailfact', N'new_cust', N'add_new_cust')
  AND num.n <= CEILING(LEN(ISNULL(m.definition, N'')) / 200.0)
ORDER BY p.name, num.n;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 06) Login password columns: names / types only, no values ========== */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'06|' + t.name + N'|' + c.name + N'|' + ty.name + N'|'
            + CASE WHEN ty.name IN (N'nvarchar', N'nchar') THEN CAST(c.max_length / 2 AS NVARCHAR(8))
                   WHEN ty.name IN (N'varchar', N'char')   THEN CASE WHEN c.max_length = -1 THEN N'MAX'
                                                                     ELSE CAST(c.max_length AS NVARCHAR(8)) END
                   ELSE N'' END
            + N'|' + CASE WHEN c.is_nullable = 1 THEN N'NULL' ELSE N'NOT NULL' END + CHAR(10)
FROM sys.tables t
JOIN sys.columns c  ON c.object_id = t.object_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
WHERE t.name COLLATE Latin1_General_CI_AS IN (N'sys_users', N'visitors', N'VizitorUsers')
  AND (c.name COLLATE Latin1_General_CI_AS LIKE N'%pass%'
    OR c.name COLLATE Latin1_General_CI_AS LIKE N'%pwd%'
    OR c.name COLLATE Latin1_General_CI_AS LIKE N'%hash%')
ORDER BY t.name, c.column_id;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
IF LEN(@out) = 0 PRINT N'06|(no password-like column found in sys_users / visitors / VizitorUsers)';
GO


/* ==== 06b) Hash-scheme evidence: lengths + FIRST 3 CHARACTERS only =======
   Needed so the Android login can verify credentials the same way Atiran
   does (bcrypt $2y$ = 60 chars, md5 = 32 hex, sha1 = 40, sha256 = 64 ...).
   Only row counts, MIN/MAX lengths and the first 3 characters are read.
   The statement is built from catalog data, so tables that do not exist here
   are simply skipped.                                                       */
CREATE TABLE #pwsample (
    section       NVARCHAR(20),
    table_schema  SYSNAME,
    table_name    SYSNAME,
    column_name   SYSNAME,
    non_null_rows BIGINT,
    min_len       INT,
    max_len       INT,
    first3        NVARCHAR(303)
);

DECLARE @pw TABLE (rn INT IDENTITY(1,1) PRIMARY KEY, sch SYSNAME, tbl SYSNAME, col SYSNAME);

INSERT INTO @pw (sch, tbl, col)
SELECT s.name, t.name, c.name
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN sys.columns c ON c.object_id = t.object_id
WHERE t.name COLLATE Latin1_General_CI_AS IN (N'sys_users', N'visitors', N'VizitorUsers')
  AND (c.name COLLATE Latin1_General_CI_AS LIKE N'%pass%'
    OR c.name COLLATE Latin1_General_CI_AS LIKE N'%pwd%'
    OR c.name COLLATE Latin1_General_CI_AS LIKE N'%hash%');

DECLARE @i INT = 1, @n INT, @sch SYSNAME, @tbl SYSNAME, @col SYSNAME;
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @n = COUNT(*) FROM @pw;

WHILE @i <= @n
BEGIN
    SELECT @sch = sch, @tbl = tbl, @col = col FROM @pw WHERE rn = @i;

    IF @i > 1 SET @sql = @sql + N' UNION ALL ';

    SET @sql = @sql
        + N'SELECT N''06b'' AS section, N'''
        + REPLACE(@sch, N'''', N'''''') + N''' AS table_schema, N'''
        + REPLACE(@tbl, N'''', N'''''') + N''' AS table_name, N'''
        + REPLACE(@col, N'''', N'''''') + N''' AS column_name, '
        + N'(SELECT COUNT(*) FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL) AS non_null_rows, '
        + N'(SELECT MIN(LEN(CAST([' + @col + N'] AS NVARCHAR(300)))) FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL) AS min_len, '
        + N'(SELECT MAX(LEN(CAST([' + @col + N'] AS NVARCHAR(300)))) FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL) AS max_len, '
        + N'(SELECT TOP 1 LEFT(CAST([' + @col + N'] AS NVARCHAR(300)), 3) + N''...'''
        + N' FROM ' + QUOTENAME(@sch) + N'.' + QUOTENAME(@tbl)
        + N' WHERE [' + @col + N'] IS NOT NULL'
        + N' AND LEN(CAST([' + @col + N'] AS NVARCHAR(300))) > 0) AS first3';

    SET @i = @i + 1;
END

IF LEN(@sql) > 0
BEGIN
    BEGIN TRY
        INSERT INTO #pwsample (section, table_schema, table_name, column_name,
                               non_null_rows, min_len, max_len, first3)
        EXEC (@sql);
    END TRY
    BEGIN CATCH
        PRINT N'06b|reading the password columns failed: ' + ERROR_MESSAGE();
    END CATCH
END
ELSE
    PRINT N'06b|(no password-like column to sample)';

DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'06b|' + table_name + N'.' + column_name + N'|rows=' + CAST(non_null_rows AS NVARCHAR(20))
            + N'|min_len=' + ISNULL(CAST(min_len AS NVARCHAR(8)), N'-')
            + N'|max_len=' + ISNULL(CAST(max_len AS NVARCHAR(8)), N'-')
            + N'|first3=' + ISNULL(first3, N'<null>') + CHAR(10)
FROM #pwsample
ORDER BY table_name, column_name;

DECLARE @i2 INT = 1;
WHILE @i2 <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i2, 4000);
    SET @i2 = @i2 + 4000;
END
DROP TABLE #pwsample;
GO


/* ==== 07) Row count of every relevant table that exists ==================
   Built dynamically, so a table that does not exist is reported instead of
   aborting the section.                                                     */
DECLARE @wanted TABLE (name SYSNAME PRIMARY KEY);
INSERT INTO @wanted (name)
SELECT DISTINCT v.name FROM (VALUES
        (N'visitors'), (N'sys_vis'), (N'sys_cus'), (N'sys_kal'), (N'sys_anb'),
        (N'masir'), (N'MasirDay'), (N'Visit'), (N'vis_goals'), (N'osystems'),
        (N'CUSTOMERS'), (N'custgroup'), (N'ka_act'), (N'kagroup'),
        (N'inventory'), (N'anbars'), (N'inventory_anbars'), (N'forosh_price'), (N'prizePercent'),
        (N'sailfact'), (N'subsailfact'), (N'sailfact_pish'), (N'subsailfact_pish'),
        (N'PishDaryaft'), (N'PishDaryaftGetCheck'), (N'getchk'), (N'BANK'),
        (N'sys_users'), (N'Roles'), (N'new_cust'), (N'cust_act'),
        (N'TabletCustomer'), (N'Device'), (N'DeviceLocation'), (N'DeviceMessages'), (N'DeviceSettings')
    ) AS v(name);

CREATE TABLE #counts (table_name NVARCHAR(300), row_count BIGINT);

DECLARE @sql NVARCHAR(MAX) = N'SELECT N''x'' AS table_name, CAST(NULL AS BIGINT) AS row_count WHERE 1 = 0';
SELECT @sql = @sql
            + N' UNION ALL SELECT N'''
            + REPLACE(s.name + N'.' + t.name, N'''', N'''''') + N''', COUNT(*) FROM '
            + QUOTENAME(s.name) + N'.' + QUOTENAME(t.name)
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
JOIN @wanted w     ON t.name COLLATE Latin1_General_CI_AS = w.name;

BEGIN TRY
    INSERT INTO #counts (table_name, row_count) EXEC (@sql);
END TRY
BEGIN CATCH
    PRINT N'07|row counts failed: ' + ERROR_MESSAGE();
END CATCH

DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'07|' + table_name + N'|' + ISNULL(CAST(row_count AS NVARCHAR(20)), N'?') + CHAR(10)
FROM #counts
ORDER BY table_name;

SELECT @out = @out + N'07|MISSING|' + w.name + CHAR(10)
FROM @wanted w
WHERE NOT EXISTS (SELECT 1 FROM sys.tables t
                   WHERE t.name COLLATE Latin1_General_CI_AS = w.name)
ORDER BY w.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
DROP TABLE #counts;
GO


/* ==== 08) Everything whose name contains "Vizitor" ======================= */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'08|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'.' + o.name + N'|' + o.type_desc + CHAR(10)
FROM sys.objects o
WHERE o.name COLLATE Latin1_General_CI_AS LIKE N'%Vizitor%'
  AND o.type IN (N'U', N'V', N'P', N'FN', N'IF', N'TF')
ORDER BY o.type_desc, o.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
IF LEN(@out) = 0 PRINT N'08|(no object with Vizitor in its name)';
GO


/* ==== 09) Approximate row counts of the Vizitor tables =================== */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'09|' + t.name + N'|' + CAST(SUM(p.rows) AS NVARCHAR(20)) + CHAR(10)
FROM sys.tables t
JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
WHERE t.name COLLATE Latin1_General_CI_AS LIKE N'%Vizitor%'
GROUP BY t.name
ORDER BY t.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
IF LEN(@out) = 0 PRINT N'09|(no Vizitor table)';
GO


/* ==== 10) Name discovery - ground truth for the "no guessing" rule ======= */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'10|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'.' + o.name + N'|' + o.type_desc + CHAR(10)
FROM sys.objects o
WHERE o.type IN (N'U', N'V', N'P', N'FN', N'IF', N'TF')
  AND (o.name COLLATE Latin1_General_CI_AS LIKE N'%vis%'    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%masir%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%pish%'   OR o.name COLLATE Latin1_General_CI_AS LIKE N'%sail%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%anbar%'  OR o.name COLLATE Latin1_General_CI_AS LIKE N'%cust%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%price%'  OR o.name COLLATE Latin1_General_CI_AS LIKE N'%forosh%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%user%'   OR o.name COLLATE Latin1_General_CI_AS LIKE N'%role%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%goal%'   OR o.name COLLATE Latin1_General_CI_AS LIKE N'%sys_%')
ORDER BY o.type_desc, o.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 11) SQL Server network / authentication config =====================
   Shows the real TCP port of this instance (answers the named-instance
   question) and LoginMode (2 = mixed SQL + Windows authentication, which is
   required for SQL logins such as vizitor_android).
   Needs VIEW SERVER STATE; if it is skipped, read the port on the server with
       NETSTAT -ANO | FINDSTR LISTENING | FINDSTR 143                      */
DECLARE @out NVARCHAR(MAX) = N'';
BEGIN TRY
    SELECT @out = @out + N'11|' + registry_key + N'|' + ISNULL(value_name, N'-') + N'|'
                + ISNULL(CAST(value_data AS NVARCHAR(256)), N'<null>') + CHAR(10)
    FROM sys.dm_server_registry
    WHERE registry_key LIKE N'%SuperSocketNetLib%'
       OR (registry_key LIKE N'%MSSQLServer%' AND value_name IN (N'LoginMode', N'LoginAuditLevel'))
    ORDER BY registry_key, value_name;
END TRY
BEGIN CATCH
    SET @out = @out + N'11|SKIPPED|' + ERROR_MESSAGE()
             + N'  -> needs VIEW SERVER STATE; run as sa/sysadmin, or use NETSTAT on the server.' + CHAR(10);
END CATCH

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== 12) All non-system databases on this instance =====================
   Confirms which database is the real ERP database when several exist.     */
DECLARE @out NVARCHAR(MAX) = N'';
BEGIN TRY
    SELECT @out = @out + N'12|' + d.name + N'|' + d.state_desc + N'|'
                + CAST(SUM(mf.size) * 8.0 / 1024 AS DECIMAL(12,1)) + N' MB|created '
                + CONVERT(NVARCHAR(30), d.create_date, 120) + CHAR(10)
    FROM sys.databases d
    JOIN sys.master_files mf ON mf.database_id = d.database_id
    WHERE d.database_id > 4
    GROUP BY d.name, d.state_desc, d.create_date
    ORDER BY d.name;
END TRY
BEGIN CATCH
    SELECT @out = @out + N'12|' + d.name + N'|' + d.state_desc
                + N'|size unavailable (' + ERROR_MESSAGE() + N')' + CHAR(10)
    FROM sys.databases d
    WHERE d.database_id > 4
    ORDER BY d.name;
END CATCH

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ===========================================================================
   END OF AUDIT - the last line printed is from section 12.

   Send back everything from the Messages tab (Ctrl+A, Ctrl+C) or the file
   written by the sqlcmd command in the header.

   Next steps after the output is received:
     - confirm every table/column name against 01/02
     - decide the login verification from 06/06b
     - decide which procedures can be called from 04/05
     - then: SettingsScreen -> SecureDbStore + SqlConnectionManager, and the
       SQL data sources (products / prices / stock / customers).

   Nothing in this script writes, so it is safe to run as often as needed.
   =========================================================================== */
