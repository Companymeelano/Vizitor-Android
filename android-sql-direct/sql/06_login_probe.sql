/* ===========================================================================
   Vizitor - audit part 6: how does the ERP really verify a login?  - v3
   2026-09-18  -  READ ONLY, pure ASCII, no BOM
   ---------------------------------------------------------------------------
   v3 FIXES "Msg 8155 No column name was specified for column 1 of 'x'" that
   broke L3a/L3b in v2: the helper pattern "FROM (SELECT 1) x" was invalid, so
   those statements now run without a FROM clause. The login question itself is
   already ANSWERED (see sql/07_login_verify.sql); this file is kept for the
   remaining columns of security.ConfirmUser/LoginDetails and helper bodies.

   v2 FIXED TWO MISTAKES OF v1 (which produced only the header line):
     1. "Msg 156 ... Incorrect syntax near the keyword 'user'": v1 wrote
        FROM EMS.user.  USER is a reserved T-SQL keyword, so the table must be
        written [EMS].[user]. (Same for any other reserved-name object.)
     2. v1 put every section into ONE batch, so that syntax error killed the
        whole body of the script. v2 gives every section its own GO, and each
        section writes into a temp table that is printed at the very end - so a
        failure in one section can no longer hide the others.

   WHY THIS SCRIPT EXISTS (facts from the live database):
     * dbo.sys_users.user_password is varbinary(50) but only 1 byte long, so it
       is NOT a SQL Server password hash and PWDCOMPARE cannot be the mechanism.
     * visitors.UserID is NULL, so the user -> visitor link is sys_vis.
   Rather than guess how the ERP authenticates, this collects the evidence.

   HOW TO RUN: SSMS on [Meelano], F5, then send the Messages tab (about 40-60
   lines). Everything is read-only.

   PRIVACY: no password value is ever printed. The only value-related line is
   section L2, which prints the single stored byte in hex (a 1-byte value) so we
   can tell a placeholder from real data. Delete that section if you prefer.
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor login probe - v2 (2026-09-18) - sections L1..L7';

IF OBJECT_ID(N'tempdb..#o') IS NOT NULL DROP TABLE #o;
CREATE TABLE #o (seq INT IDENTITY(1,1) PRIMARY KEY, line NVARCHAR(MAX));
GO


/* ---- L1) shape of the stored password column --------------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'L1|user_id=' + CAST(user_id AS NVARCHAR(6))
            + N'|user=' + ISNULL(user_name, N'<null>')
            + N'|bytes=' + ISNULL(CAST(DATALENGTH(user_password) AS NVARCHAR(6)), N'null')
            + N'|shape=' + CASE
                 WHEN user_password IS NULL THEN N'null'
                 WHEN DATALENGTH(user_password) IN (20, 44, 60) THEN N'looks like a password hash'
                 WHEN DATALENGTH(user_password) = 0 THEN N'empty'
                 WHEN DATALENGTH(user_password) = 1 THEN N'single byte - not a hash'
                 ELSE N'other length - not a standard hash' END
            + N'|active=' + ISNULL(CAST(active AS NVARCHAR(10)), N'<null>')
            + N'|locked=' + ISNULL(CAST(IsLocked AS NVARCHAR(10)), N'<null>')
            + N'|role_id=' + ISNULL(CAST(role_id AS NVARCHAR(6)), N'<null>')
            + N'|shmo=' + ISNULL(CAST(shmo AS NVARCHAR(6)), N'<null>')
            + CHAR(10)
FROM dbo.sys_users
ORDER BY user_id;
IF LEN(@out) = 0 SET @out = N'L1|(sys_users is empty)' + CHAR(10);
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L2) [optional] that single byte in hex ---------------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'L2|user_id=' + CAST(user_id AS NVARCHAR(6))
            + N'|pw_hex=' + ISNULL(CONVERT(NVARCHAR(20), user_password, 2), N'null')
            + N'|as_char=' + ISNULL(CONVERT(NVARCHAR(4), CONVERT(VARCHAR(2), user_password)), N'<null>')
            + CHAR(10)
FROM dbo.sys_users
ORDER BY user_id;
IF LEN(@out) = 0 SET @out = N'L2|(no rows)' + CHAR(10);
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L3a) security.ConfirmUser ----------------------------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
BEGIN TRY
    SELECT @out = @out + N'L3|security.ConfirmUser|COLS|'
                + STUFF((SELECT N',' + c.name + N' ' + ty.name
                           FROM sys.columns c
                           JOIN sys.types ty ON ty.user_type_id = c.user_type_id
                          WHERE c.object_id = OBJECT_ID(N'security.ConfirmUser')
                          ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'')
                + CHAR(10);
    SELECT @out = @out + N'L3|security.ConfirmUser|rows=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
    FROM security.ConfirmUser;
END TRY
BEGIN CATCH
    SET @out = @out + N'L3|security.ConfirmUser|SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L3b) [EMS].[user]   (brackets: USER is a reserved keyword) --------- */
DECLARE @out NVARCHAR(MAX) = N'';
BEGIN TRY
    SELECT @out = @out + N'L3|EMS.user|COLS|'
                + STUFF((SELECT N',' + c.name + N' ' + ty.name
                           FROM sys.columns c
                           JOIN sys.types ty ON ty.user_type_id = c.user_type_id
                          WHERE c.object_id = OBJECT_ID(N'[EMS].[user]')
                          ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'')
                + CHAR(10);
    SELECT @out = @out + N'L3|EMS.user|rows=' + CAST(COUNT(*) AS NVARCHAR(10)) + CHAR(10)
    FROM [EMS].[user];
END TRY
BEGIN CATCH
    SET @out = @out + N'L3|EMS.user|SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L4) permission-scope rows of the real users ----------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'L4|sys_vis|SysID=' + CAST(SysID AS NVARCHAR(6))
            + N'|shvis=' + CAST(shvis AS NVARCHAR(6))
            + N'|UserID=' + CAST(UserID AS NVARCHAR(6)) + CHAR(10)
FROM dbo.sys_vis;

SELECT @out = @out + N'L4|sys_use|SysID=' + CAST(SysID AS NVARCHAR(6))
            + N'|shuse=' + CAST(shuse AS NVARCHAR(6))
            + N'|UserID=' + CAST(UserID AS NVARCHAR(6)) + CHAR(10)
FROM dbo.sys_use;

SELECT @out = @out + N'L4|sys_users_list|' + CAST(user_id AS NVARCHAR(6))
            + N'|' + ISNULL(user_name, N'<null>')
            + N'|' + ISNULL(user_fname, N'') + N' ' + ISNULL(user_lname, N'')
            + N'|active=' + ISNULL(CAST(active AS NVARCHAR(10)), N'<null>') + CHAR(10)
FROM dbo.sys_users;
IF LEN(@out) = 0 SET @out = N'L4|(no scope rows)' + CHAR(10);
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L5) the ERP's own login helpers (short bodies) ------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'L5|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'.' + o.name
            + N'|' + ISNULL(LEFT(m.definition, 1500), N'<no definition>') + CHAR(10)
FROM sys.objects o
JOIN sys.sql_modules m ON m.object_id = o.object_id
WHERE o.name COLLATE Latin1_General_CI_AS IN (N'SetUserpass', N'SetUsername', N'GetUser',
        N'getEmsUsername', N'SetSystemName', N'AddUser', N'ChangeUserPassInSalMali',
        N'get_role_id', N'IsAccountingSystemStarted')
ORDER BY o.name;
IF LEN(@out) = 0 SET @out = N'L5|(none of the expected helper procedures/functions exist)' + CHAR(10);
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L6) every password-like column in the whole database ------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'L6|' + ISNULL(SCHEMA_NAME(t.schema_id), N'?') + N'.' + t.name
            + N'|' + c.name + N'|' + ty.name + N'|len=' + CAST(c.max_length AS NVARCHAR(8)) + CHAR(10)
FROM sys.columns c
JOIN sys.tables  t  ON t.object_id = c.object_id
JOIN sys.types   ty ON ty.user_type_id = c.user_type_id
WHERE c.name COLLATE Latin1_General_CI_AS LIKE N'%pass%'
   OR c.name COLLATE Latin1_General_CI_AS LIKE N'%pwd%'
   OR c.name COLLATE Latin1_General_CI_AS LIKE N'%hash%'
   OR c.name COLLATE Latin1_General_CI_AS LIKE N'%secret%'
   OR c.name COLLATE Latin1_General_CI_AS LIKE N'%credential%'
ORDER BY t.name, c.column_id;
IF LEN(@out) = 0 SET @out = N'L6|(no password-like column anywhere)' + CHAR(10);
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- L7) objects whose name hints at login/authentication ------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'L7|' + ISNULL(SCHEMA_NAME(o.schema_id), N'?') + N'.' + o.name
            + N'|' + o.type_desc + CHAR(10)
FROM sys.objects o
WHERE o.type IN (N'U', N'V', N'P', N'FN', N'IF', N'TF')
  AND (o.name COLLATE Latin1_General_CI_AS LIKE N'%pass%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%login%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%confirm%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%auth%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%session%'
    OR o.name COLLATE Latin1_General_CI_AS LIKE N'%isvaliduser%')
ORDER BY o.type_desc, o.name;
IF LEN(@out) = 0 SET @out = N'L7|(nothing found)' + CHAR(10);
INSERT INTO #o (line) SELECT line FROM (SELECT line = @out) x;
GO


/* ---- print everything collected so far, in order ---------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + line + CHAR(10) FROM #o ORDER BY seq;
IF LEN(@out) = 0 SET @out = N'*** no section produced output - please report this ***' + CHAR(10);

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
DROP TABLE #o;
GO

/* ===========================================================================
   END OF PART 6 (v2). Send the Messages tab.
   If a section printed a SKIPPED line, that object does not exist or is not
   readable - the remaining sections are still valid.
   =========================================================================== */
