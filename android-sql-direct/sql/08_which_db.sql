/* ============================================================================
   08_which_db.sql  -  WHICH database is the live one?
   READ ONLY. It writes nothing, changes nothing.

   Why: the four procedure bodies, the login probe and the row counts all came
   from the database Meelano, but the column-list paste of subsailfact_pish /
   SubSailSefaresh / subsailtemp came from a database called Atiran14050603
   (and its subsailfact_pish has one column more). The app has to write where
   the ERP writes, so this has to be settled by evidence.

   Sections 1 and 2 look at EVERY database on the server.
   Sections 3, 4 and 5 look at the database selected in the SSMS toolbar
   ("Available Databases" dropdown) - run the file once with Meelano selected
   and once with Atiran14050603 selected.
   ========================================================================== */
SET NOCOUNT ON;
GO

/* 0) where am I? ----------------------------------------------------------- */
SELECT SVR = @@SERVERNAME, DB_ = DB_NAME(), LOGIN_ = SUSER_SNAME();
GO

/* 1) every database, newest write first ------------------------------------
   last_write = the last time a user inserted / updated / deleted in it.
   The database the ERP really works in carries the recent date.            */
SELECT DB_ = d.name,
       DB_ID_ = d.database_id,
       created = CONVERT(varchar(19), d.create_date, 120),
       last_write = CONVERT(varchar(19),
            (SELECT MAX(s.last_user_update)
               FROM sys.dm_db_index_usage_stats s
              WHERE s.database_id = d.database_id), 120),
       state_ = d.state_desc
  FROM sys.databases d
 WHERE d.database_id > 4
 ORDER BY last_write DESC, d.name;
GO

/* 2) row counts of the key tables in every database ------------------------ */
SELECT DB_ = d.name,
       TBL = OBJECT_NAME(s.object_id, s.database_id),
       rows_ = SUM(s.record_count)
  FROM sys.dm_db_index_physical_stats(NULL, NULL, NULL, NULL, 'LIMITED') s
  JOIN sys.databases d ON d.database_id = s.database_id
 WHERE d.database_id > 4
   AND s.index_id IN (0, 1)
   AND OBJECT_NAME(s.object_id, s.database_id) IN
       ('sailfact', 'subsailfact', 'sailfact_pish', 'subsailfact_pish',
        'subsailtemp', 'SubSailSefaresh', 'CUSTOMERS', 'inventory', 'sys_users')
 GROUP BY d.name, OBJECT_NAME(s.object_id, s.database_id)
 ORDER BY DB_, TBL;
GO

/* 2b) every database that carries a sal_mali table, and its fiscal year rows -
   the ERP calls the fiscal year "current" with sal_mali.[Current] = 1, so this
   is the strongest single answer to "which database does the ERP work in".   */
DECLARE @dbs TABLE (rn INT IDENTITY(1,1) PRIMARY KEY, name SYSNAME);
INSERT INTO @dbs (name)
SELECT name FROM sys.databases WHERE database_id > 4 AND state_desc = 'ONLINE';

DECLARE @i INT = 1, @n INT, @db SYSNAME, @q NVARCHAR(MAX);
SELECT @n = COUNT(*) FROM @dbs;

WHILE @i <= @n
BEGIN
    SELECT @db = name FROM @dbs WHERE rn = @i;
    SET @q = N'IF EXISTS (SELECT 1 FROM ' + QUOTENAME(@db) + N'.sys.tables WHERE name = N''sal_mali'') '
           + N'SELECT ''SALMALI|'' + ' + QUOTENAME(@db, '''') + N' AS DB_, rdf, name, nam_db, [Current] '
           + N'FROM ' + QUOTENAME(@db) + N'.dbo.sal_mali ORDER BY rdf;';
    BEGIN TRY
        EXEC sp_executesql @q;
    END TRY
    BEGIN CATCH
        SELECT ERR = 'SALMALI|' + @db + '|' + ERROR_MESSAGE();
    END CATCH
    SET @i = @i + 1;
END
GO

/* 3) fiscal years of the SELECTED database ----------------------------------
   (this is the list ChangeUserPassInSalMali walks: one row per fiscal year)  */
SELECT SALMALI = 'sal_mali|' + DB_NAME(),
       sal_maliID, rdf, name, nam_db, StartDate, EndDate, [Current]
  FROM dbo.sal_mali;
GO

/* 4) the pre-invoice tables of the SELECTED database ------------------------ */
SELECT HERE = DB_NAME();
SELECT TBL = t.name,
       rows_ = SUM(p.rows),
       cols_ = (SELECT COUNT(*) FROM sys.columns c WHERE c.object_id = t.object_id)
  FROM sys.tables t
  JOIN sys.partitions p
    ON p.object_id = t.object_id AND p.index_id IN (0, 1)
 WHERE t.name IN ('sailfact_pish', 'subsailfact_pish',
                  'subsailtemp', 'SubSailSefaresh')
 GROUP BY t.name, t.object_id;
GO

/* 5) columns of subsailfact_pish in the SELECTED database ------------------- */
SELECT ORD = c.column_id,
       COL = c.name,
       TYPE = ty.name,
       LEN = c.max_length,
       NULLABLE = CASE WHEN c.is_nullable = 1 THEN 'NULL' ELSE 'NOT NULL' END
  FROM sys.columns c
  JOIN sys.types ty ON ty.user_type_id = c.user_type_id
 WHERE c.object_id = OBJECT_ID('dbo.subsailfact_pish')
 ORDER BY c.column_id;
GO

/* 6) every database of THIS instance that keeps a sal_mali table ------------
   the real question is answered here: which database does the ERP itself treat
   as the fiscal database - the one whose sal_mali row has [Current] = 1.
   Read-only. A database without sal_mali is skipped, never an error.         */
DECLARE @dbs TABLE (rn INT IDENTITY(1,1) PRIMARY KEY, dbname SYSNAME);
INSERT INTO @dbs (dbname)
SELECT name FROM sys.databases WHERE database_id > 4 AND state_desc = 'ONLINE' ORDER BY name;

DECLARE @i INT = 1, @n INT, @db SYSNAME, @q NVARCHAR(MAX);
SELECT @n = COUNT(*) FROM @dbs;
WHILE @i <= @n
BEGIN
    SELECT @db = dbname FROM @dbs WHERE rn = @i;
    SET @q = N'IF EXISTS (SELECT 1 FROM ' + QUOTENAME(@db) + N'.sys.tables WHERE name = N''sal_mali'') '
           + N'SELECT ''SALMALI|'' + ' + QUOTENAME(@db, '''') + N' AS DB_, rdf, name, nam_db, '
           + N'       StartDate, EndDate, [Current] '
           + N'FROM ' + QUOTENAME(@db) + N'.dbo.sal_mali ORDER BY rdf;';
    BEGIN TRY
        EXEC sp_executesql @q;
    END TRY
    BEGIN CATCH
        SELECT DB_ = N'SALMALI|' + @db, ERR = ERROR_MESSAGE();
    END CATCH
    SET @i = @i + 1;
END
GO
