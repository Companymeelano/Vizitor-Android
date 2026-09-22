/* ===========================================================================
   Vizitor - audit part 11: compare the ERP databases of THIS instance
   READ ONLY. Pure ASCII, no BOM. Nothing is written anywhere.
   ---------------------------------------------------------------------------
   Run it in SSMS on the server (F5). It does NOT care which database the
   toolbar shows: it walks sys.databases itself and reports, for every user
   database, which of the objects the app's write path needs exist, how many
   rows the key tables hold, and what each database says its own fiscal
   database is (dbo.sal_mali.nam_db / .Current).
   A database that lacks a table simply has no row for it - never an error.
   Output: one grid  db | what | value   plus one grid  name | state | created.
   =========================================================================== */
SET NOCOUNT ON;

IF OBJECT_ID('tempdb..#r') IS NOT NULL DROP TABLE #r;
CREATE TABLE #r (seq INT IDENTITY(1,1), db sysname, what nvarchar(200), value nvarchar(400));

DECLARE @d sysname, @sql nvarchar(max);

/* every online user database of this instance - except the system ones */
DECLARE cur CURSOR LOCAL FAST_FORWARD FOR
    SELECT name FROM sys.databases WHERE database_id > 4 AND state_desc = 'ONLINE' ORDER BY name;

OPEN cur;
FETCH NEXT FROM cur INTO @d;
WHILE @@FETCH_STATUS = 0
BEGIN
    /* 1) the objects the write path needs + how many rows the key tables hold */
    SET @sql =
        N'INSERT INTO #r(db, what, value) ' +
        N'SELECT ' + QUOTENAME(@d, '''') + N', N''obj: '' + o.name, o.type_desc ' +
        N'FROM ' + QUOTENAME(@d) + N'.sys.objects AS o ' +
        N'WHERE o.name IN (N''add_sail_pish'',N''AddInvoice'',N''Edit_sail_pish'',N''trig_sst_pish'',' +
        N'                 N''InvoiceTrigger'',N''ListPishFactor'',N''pishfactor_body'') ' +
        N'UNION ALL ' +
        N'SELECT ' + QUOTENAME(@d, '''') + N', N''rows: '' + t.name, CONVERT(nvarchar(400), SUM(p.rows)) ' +
        N'FROM ' + QUOTENAME(@d) + N'.sys.tables AS t ' +
        N'JOIN ' + QUOTENAME(@d) + N'.sys.partitions AS p ' +
        N'     ON p.object_id = t.object_id AND p.index_id IN (0,1) ' +
        N'WHERE t.name IN (N''CUSTOMERS'',N''inventory'',N''sailfact'',N''sailfact_pish'',' +
        N'                 N''subsailfact'',N''subsailfact_pish'',N''subsailtemp'',' +
        N'                 N''subsailtemp_pish'',N''sys_users'',N''visitors'',' +
        N'                 N''custgroup'',N''forosh_price'',N''kagroup'') ' +
        N'GROUP BY t.name;';
    BEGIN TRY
        EXEC sp_executesql @sql;
    END TRY
    BEGIN CATCH
        INSERT INTO #r(db, what, value) SELECT @d, N'*** objects/rows', ERROR_MESSAGE();
    END CATCH

    /* 2) what the database itself says the current fiscal database is */
    SET @sql =
        N'INSERT INTO #r(db, what, value) ' +
        N'SELECT ' + QUOTENAME(@d, '''') + N', N''sal_mali'', ' +
        N'       N''name='' + ISNULL(CONVERT(nvarchar(200), sm.name), N''?'') + ' +
        N'       N'' nam_db='' + ISNULL(CONVERT(nvarchar(200), sm.nam_db), N''?'') + ' +
        N'       N'' Current='' + ISNULL(CONVERT(nvarchar(50), sm.[Current]), N''?'') ' +
        N'FROM ' + QUOTENAME(@d) + N'.dbo.sal_mali AS sm;';
    BEGIN TRY
        EXEC sp_executesql @sql;
    END TRY
    BEGIN CATCH
        INSERT INTO #r(db, what, value) SELECT @d, N'*** sal_mali', ERROR_MESSAGE();
    END CATCH

    FETCH NEXT FROM cur INTO @d;
END
CLOSE cur;
DEALLOCATE cur;

/* the whole answer in one grid */
SELECT db, what, value FROM #r ORDER BY db, what;

/* and who these databases are */
SELECT name, state_desc, create_date, compatibility_level
FROM sys.databases WHERE database_id > 4 ORDER BY name;
