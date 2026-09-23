/* ===========================================================================
   Vizitor - audit part 2: fill the remaining gaps  -  v1  -  2026-09-18
   READ ONLY. Every line is printed to the Messages tab, nothing else.
   Pure ASCII, no BOM.
   ---------------------------------------------------------------------------
   This script asks only for what part 1 could not deliver because its output
   was too large to paste:

     A) the real TCP port + LoginMode registry values  (part 1 was cut off)
     B) how the ERP verifies a password (sys_users.user_password is varbinary,
        so the app must verify server-side with PWDCOMPARE or a GetUser-like
        procedure) - plus the short bodies of the login/config functions
     C) contents + column names of the tiny configuration tables
        (osystems, Roles, visitors, sys_vis, sys_cus, sys_kal, sys_anb, anbars,
         masir, MasirDay, custgroup) and small samples of the data tables
     D) the definitions of the views the app will read
        (VW_InventoryAnbars, pishfactors, SailFactPish_Details, ...)

   Run it exactly like part 1: select the database [Meelano], press F5 and send
   the Messages tab (or use sqlcmd -o file, see part 1's header).

   The OUTPUT is deliberately small (roughly 200-300 lines).
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor audit part 2 (gaps) - script version v1 (2026-09-18)';
GO


/* ==== A) network / authentication registry ============================== */
DECLARE @out NVARCHAR(MAX) = N'';
BEGIN TRY
    SELECT @out = @out + N'A|' + registry_key + N'|' + ISNULL(value_name, N'-') + N'|'
                + ISNULL(CAST(value_data AS NVARCHAR(256)), N'<null>') + CHAR(10)
    FROM sys.dm_server_registry
    WHERE value_name IN (N'TcpPort', N'TcpDynamicPorts', N'TcpPortValue',
                         N'LoginMode', N'LoginAuditLevel')
       OR registry_key LIKE N'%SuperSocketNetLib\Tcp%'
    ORDER BY registry_key, value_name;
END TRY
BEGIN CATCH
    SET @out = @out + N'A|SKIPPED|' + ERROR_MESSAGE()
             + N' -> run as sa/sysadmin, or read the port with NETSTAT -ANO | FINDSTR LISTENING | FINDSTR 143' + CHAR(10);
END CATCH
IF LEN(@out) = 0 SET @out = N'A|(no registry rows visible)' + CHAR(10);

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== B) login: how is the password verified? ============================
   sys_users.user_password is varbinary, so the Android app must NOT try to
   re-implement the hash: SQL Server verifies it (PWDCOMPARE) or the ERP does
   (GetUser-like procedure). Here we check both possibilities.              */
DECLARE @out NVARCHAR(MAX) = N'';

BEGIN TRY
    SELECT @out = @out + N'B|sys_users|id=' + CAST(user_id AS NVARCHAR(10))
                + N'|name=' + ISNULL(user_name, N'<null>')
                + N'|active=' + ISNULL(CAST(active AS NVARCHAR(10)), N'<null>')
                + N'|role_id=' + ISNULL(CAST(role_id AS NVARCHAR(6)), N'<null>')
                + N'|shmo=' + ISNULL(CAST(shmo AS NVARCHAR(6)), N'<null>')
                + N'|pw_bytes=' + ISNULL(CAST(DATALENGTH(user_password) AS NVARCHAR(6)), N'<null>')
                + N'|pwdcompare(''x'')=' + CAST(PWDCOMPARE(N'x', user_password) AS NVARCHAR(4))
                + N'|locked=' + ISNULL(CAST(IsLocked AS NVARCHAR(10)), N'<null>') + CHAR(10)
    FROM dbo.sys_users
    ORDER BY user_id;
END TRY
BEGIN CATCH
    SET @out = @out + N'B|ERROR|' + ERROR_MESSAGE() + CHAR(10);
END CATCH

/* visitors: is a visitor linked to a sys_users row? (visitors.UserID) */
SELECT @out = @out + N'B|visitor|vis_rdf=' + CAST(vis_rdf AS NVARCHAR(10))
            + N'|name=' + ISNULL(vis_name, N'<null>')
            + N'|active=' + ISNULL(active, N'<null>')
            + N'|UserID=' + ISNULL(CAST(UserID AS NVARCHAR(10)), N'<null>')
            + N'|Username=' + ISNULL(Username, N'<null>')
            + N'|has_password=' + CASE WHEN Password IS NULL OR Password = N'' THEN N'no' ELSE N'yes' END
            + N'|device=' + ISNULL(CAST(rdf_device AS NVARCHAR(10)), N'<null>')
            + N'|city=' + ISNULL(CAST(vis_city AS NVARCHAR(10)), N'<null>')
            + N'|region=' + ISNULL(CAST(VIs_region AS NVARCHAR(10)), N'<null>')
            + N'|max_factor=' + ISNULL(CAST(TedadFactorMojazMande AS NVARCHAR(10)), N'<null>')
            + N'|max_amount=' + ISNULL(CAST(MablaghMojazMandeJahatFactorha AS NVARCHAR(20)), N'<null>')
            + CHAR(10)
FROM dbo.visitors
ORDER BY vis_rdf;

/* short bodies of the login / config helpers, so we know the real rule */
SELECT @out = @out + N'B|def|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'.' + o.name
            + N'|' + ISNULL(LEFT(m.definition, 1200), N'<no definition>') + CHAR(10)
FROM sys.objects o
JOIN sys.sql_modules m ON m.object_id = o.object_id
WHERE o.name COLLATE Latin1_General_CI_AS IN (N'SetUserpass', N'SetUsername', N'SetSystemName',
        N'GetUser', N'get_role_id', N'get_vis_rdf', N'which_panevis',
        N'IsAccountingSystemStarted', N'getEmsUsername', N'vis_name', N'cust_group_name')
ORDER BY o.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== C) configuration tables + small samples ============================
   Generic dumper: for every table below it prints one COLS line with the
   column names (needed for sys_kal / sys_anb whose columns we do not know
   yet) and then up to @maxRows data lines (all text-like columns, joined with
   '|'; binary/image/text columns are skipped).                            */
DECLARE @maxRows INT = 25;

DECLARE @tables TABLE (rn INT IDENTITY(1,1) PRIMARY KEY, name SYSNAME, max_rows INT);
INSERT INTO @tables (name, max_rows) VALUES
    (N'dbo.osystems', 5), (N'dbo.Roles', 10), (N'dbo.CITYS', 5), (N'dbo.Province', 5),
    (N'dbo.regions', 5), (N'dbo.anbars', 5), (N'dbo.masir', 5), (N'dbo.MasirDay', 5),
    (N'dbo.sys_vis', 20), (N'dbo.sys_cus', 20), (N'dbo.sys_kal', 20), (N'dbo.sys_anb', 20),
    (N'dbo.custgroup', 15), (N'dbo.CUSTOMERS', 10), (N'dbo.forosh_price', 5),
    (N'dbo.inventory', 5), (N'dbo.inventory_anbars', 15), (N'dbo.kagroup', 10),
    (N'dbo.sailfact', 3), (N'dbo.subsailfact', 5), (N'dbo.cust_act', 3);

CREATE TABLE #dump (seq INT IDENTITY, line NVARCHAR(MAX));

DECLARE @i INT = 1, @n INT, @tn SYSNAME, @lim INT, @cols NVARCHAR(MAX), @q NVARCHAR(MAX);
SELECT @n = COUNT(*) FROM @tables;

WHILE @i <= @n
BEGIN
    SELECT @tn = name, @lim = max_rows FROM @tables WHERE rn = @i;

    SELECT @cols = STUFF((SELECT N' + N''|'' + ISNULL(CAST(' + QUOTENAME(c.name) + N' AS NVARCHAR(400)), N''<null>'')'
                            FROM sys.columns c
                            JOIN sys.types ty ON ty.user_type_id = c.user_type_id
                           WHERE c.object_id = OBJECT_ID(@tn)
                             AND ty.name NOT IN (N'image', N'varbinary', N'text', N'ntext',
                                                 N'xml', N'geography', N'geometry', N'timestamp')
                           ORDER BY c.column_id
                             FOR XML PATH('')), 1, 3, N'');

    IF @cols IS NULL
        INSERT INTO #dump (line) VALUES (N'C|' + @tn + N'|COLS|<table not found>');
    ELSE
    BEGIN
        INSERT INTO #dump (line)
        SELECT N'C|' + @tn + N'|COLS|'
             + STUFF((SELECT N',' + c2.name
                        FROM sys.columns c2
                       WHERE c2.object_id = OBJECT_ID(@tn)
                       ORDER BY c2.column_id FOR XML PATH('')), 1, 1, N'');

        IF @lim > 0
        BEGIN
            SET @q = N'INSERT INTO #dump (line) SELECT N''C|' + REPLACE(@tn, N'''', N'''''')
                   + N'|'' + ' + @cols + N' FROM (SELECT TOP (' + CAST(@lim AS NVARCHAR(6)) + N') * FROM ' + @tn + N') x';
            BEGIN TRY
                EXEC (@q);
            END TRY
            BEGIN CATCH
                INSERT INTO #dump (line) VALUES (N'C|' + @tn + N'|ERROR|' + ERROR_MESSAGE());
            END CATCH
        END
    END
    SET @i = @i + 1;
END

DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + line + CHAR(10) FROM #dump ORDER BY seq;

DECLARE @i2 INT = 1;
WHILE @i2 <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i2, 4000);
    SET @i2 = @i2 + 4000;
END
DROP TABLE #dump;
GO


/* ==== D) definitions of the views the app will read =====================
   Only the first 1200 characters of each, enough to see which tables and
   filters they use. Full text can be dumped later for the ones we pick.   */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'D|' + v.name + N'|len=' + CAST(LEN(ISNULL(m.definition, N'')) AS NVARCHAR(10))
            + N'|' + ISNULL(LEFT(m.definition, 1200), N'<none>') + CHAR(10)
FROM sys.views v
JOIN sys.sql_modules m ON m.object_id = v.object_id
WHERE v.name COLLATE Latin1_General_CI_AS IN (N'VW_InventoryAnbars', N'pishfactors',
        N'pishfactor_body', N'SailFactPish_Details', N'subsailFactPish',
        N'VW_GoalsVisitors', N'VisitorInformation', N'VisitInfo', N'Vw_Visit',
        N'VwListPishfactorhayeTeadNashodeh', N'VW_RowDetailsForosh',
        N'VW_CustomerInformation', N'vw_customer', N'VW_ListCustomer')
ORDER BY v.name;

IF LEN(@out) = 0 SET @out = N'D|(none of the listed views exist)' + CHAR(10);

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ==== E) what is really missing for the write path ====================== */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'E|' + o.name + N'|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'|'
            + ISNULL(CAST(LEN(m.definition) AS NVARCHAR(12)) + N' characters', N'<no definition>')
            + CHAR(10)
FROM sys.objects o
LEFT JOIN sys.sql_modules m ON m.object_id = o.object_id
WHERE o.name COLLATE Latin1_General_CI_AS IN (N'add_sail_pish', N'AddInvoice', N'FixMojodi',
        N'new_cust', N'newcust', N't_newcust', N'Edit_sail_pish', N'back_sail', N'change_price',
        N'AddUser', N'FixInventoryPrice', N'UpdateMojodiInventoryAnbars', N'ListPishFactor',
        N'CustomerListToDate', N'set_vis_koli', N'VisitorSalesCommision', N'GetVisitorPoints',
        N'AddFromAtiranDetailsForVisitors', N'SelectPriceAndTedvahForushVisitorhaByDate',
        N'ProcInsertIntoSysKal', N'ted_moghayerat_anbar', N'raf_moghayerat_anbar',
        N'fill_cust_more_info', N'close_open_cust')
ORDER BY o.name;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ===========================================================================
   END OF PART 2.
   Next: 03_dump_proc_bodies.sql writes the full bodies of add_sail_pish,
   AddInvoice, new_cust and FixMojodi into a file (best via sqlcmd -o).
   =========================================================================== */
