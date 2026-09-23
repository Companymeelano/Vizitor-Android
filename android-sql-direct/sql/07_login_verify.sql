/* ===========================================================================
   Vizitor - audit part 7: prove the app's login condition  -  v2  -  2026-09-18
   READ ONLY, pure ASCII, no BOM, output about 25 lines.
   ---------------------------------------------------------------------------
   WHAT WE KNOW (from the ERP's own code on this server):
     * dbo.SetUserpass reads the password as:  convert(varchar(50), user_password)
     * dbo.ChangeUserPassInSalMali writes it as: user_password = CONVERT(varbinary, @PassWord)
     * the stored value is 1 byte with hex 31, i.e. the plain character "1"
   => this ERP stores the password as plain text inside a varbinary column, so
      SQL Server hashing (PWDCOMPARE) is NOT the mechanism, and the Android app
      compares with CONVERT(varchar(50), user_password) exactly like the ERP does.

   WHAT THIS SCRIPT TESTS, FOR REAL, WITHOUT PRINTING ANY PASSWORD:
     V1   the app's exact WHERE clause with the username+password you type in
          the FILL IN block below  ->  MATCH / NO MATCH
     V1b  the same user name without the password condition (control: it proves
          the spelling of user_name is right)
     V1c  PWDCOMPARE result, recorded only to document that hashing is unused
     V2   the same clause with a deliberately wrong password -> must be 0 rows
     V3   the other candidate credential stores (security.ConfirmUser and
          security.LoginDetails): columns + row counts
     V4   fiscal-year database list (dbo.sal_mali), dbo.overal_setting count and
          dbo.IsAccountingSystemStarted()

   HOW TO RUN:  SSMS on the server, database [Meelano] (or sqlcmd)
     1. edit ONLY the FILL IN block below (about 30 lines down),
     2. select the WHOLE file (Ctrl+A) and press F5,
     3. send the Messages tab back, then do NOT save this file.

   v2 FIX (2026-09-18): in v1 the three test values were DECLAREd above a GO and
   used below it, so every section failed to compile with "Msg 137 Must declare
   the scalar variable @testUser" and the only output was "*** no output ***".
   Now the three values live in temp table #cfg, and every section is its own
   self-contained batch that reads them from there - a GO can no longer orphan
   a variable. tools/verify_tsql.py now fails any script with that mistake
   (see the "variable scope per batch" line in its report).
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor login verification - v2 (2026-09-18) - run the whole file';

IF OBJECT_ID(N'tempdb..#cfg') IS NOT NULL DROP TABLE #cfg;
CREATE TABLE #cfg (testUser NVARCHAR(80), testPassword NVARCHAR(128), wrongPassword NVARCHAR(128));

IF OBJECT_ID(N'tempdb..#o') IS NOT NULL DROP TABLE #o;
CREATE TABLE #o (seq INT IDENTITY(1,1) PRIMARY KEY, line NVARCHAR(MAX));

/* ===========================================================================
   FILL IN  -  this is the only place you edit.
   Both users on this server exist with the password "1"; type the password you
   really log in with. It is never printed - only MATCH / NO MATCH comes back.
   =========================================================================== */
INSERT INTO #cfg (testUser, testPassword, wrongPassword)
VALUES (N'Admin', N'1', N'definitely-not-the-password-12345');
GO


/* ---- V1) the app's exact login predicate, with the REAL password -------- */
DECLARE @testUser NVARCHAR(80), @testPassword NVARCHAR(128), @out NVARCHAR(MAX) = N'';
SELECT @testUser = testUser, @testPassword = testPassword FROM #cfg;

SELECT @out = @out + N'V1|MATCH|user_id=' + ISNULL(CAST(u.user_id AS NVARCHAR(6)), N'<null>')
            + N'|user=' + ISNULL(u.user_name, N'<null>')
            + N'|fullname=' + ISNULL(u.user_fname, N'') + N' ' + ISNULL(u.user_lname, N'')
            + N'|role_id=' + ISNULL(CAST(u.role_id AS NVARCHAR(6)), N'<null>')
            + N'|active=' + ISNULL(CAST(u.active AS NVARCHAR(10)), N'<null>')
            + N'|IsLocked=' + ISNULL(CAST(u.IsLocked AS NVARCHAR(10)), N'<null>')
            + N'|shmo=' + ISNULL(CAST(u.shmo AS NVARCHAR(6)), N'<null>')
FROM dbo.sys_users u
WHERE u.user_name = @testUser
  AND CONVERT(varchar(50), u.user_password) = @testPassword
  AND u.active = 1;

IF @out IS NULL OR LEN(@out) = 0
    SET @out = N'V1|NO MATCH|no row came back for user [' + ISNULL(@testUser, N'?')
             + N']. Check the password typed in the FILL IN block, the spelling of'
             + N' user_name, and that active = 1 for that user.';

INSERT INTO #o (line) VALUES (@out);
GO


/* ---- V1b) same user name WITHOUT the password condition (control) ------- */
DECLARE @testUser NVARCHAR(80), @out NVARCHAR(MAX) = N'';
SELECT @testUser = testUser FROM #cfg;

SELECT @out = N'V1b|control|rows_with_that_user_name=' + CAST(COUNT(*) AS NVARCHAR(6))
            + N'|active_1_rows=' + CAST(SUM(CASE WHEN u.active = 1 THEN 1 ELSE 0 END) AS NVARCHAR(6))
            + N' (proves the name is spelled right)'
FROM dbo.sys_users u WHERE u.user_name = @testUser;

INSERT INTO #o (line) VALUES (@out);
GO


/* ---- V1c) hash-style comparison, for the record ------------------------- */
DECLARE @testUser NVARCHAR(80), @testPassword NVARCHAR(128), @out NVARCHAR(MAX) = N'';
SELECT @testUser = testUser, @testPassword = testPassword FROM #cfg;

SELECT @out = N'V1c|PWDCOMPARE_result=' + ISNULL(CAST(PWDCOMPARE(@testPassword, u.user_password) AS NVARCHAR(4)), N'null')
            + N' (expected 0 - this ERP does not use SQL Server password hashing)'
FROM dbo.sys_users u WHERE u.user_name = @testUser;

IF @out IS NULL OR LEN(@out) = 0
    SET @out = N'V1c|skipped|no row for that user_name';

INSERT INTO #o (line) VALUES (@out);
GO


/* ---- V2) the predicate must REJECT a wrong password --------------------- */
DECLARE @testUser NVARCHAR(80), @wrongPassword NVARCHAR(128), @out NVARCHAR(MAX) = N'';
SELECT @testUser = testUser, @wrongPassword = wrongPassword FROM #cfg;

SELECT @out = N'V2|wrong_password_rows=' + CAST(COUNT(*) AS NVARCHAR(6))
            + N' (must be 0, otherwise the comparison is broken)'
FROM dbo.sys_users u
WHERE u.user_name = @testUser
  AND CONVERT(varchar(50), u.user_password) = @wrongPassword
  AND u.active = 1;

INSERT INTO #o (line) VALUES (@out);
GO


/* ---- V3) the other candidate credential stores -------------------------- */
DECLARE @out NVARCHAR(MAX) = N'';

SELECT @out = @out + N'V3|security.ConfirmUser|COLS|'
            + STUFF((SELECT N',' + c.name
                       FROM sys.columns c
                      WHERE c.object_id = OBJECT_ID(N'security.ConfirmUser')
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'')
            + N'|rows=' + CAST((SELECT COUNT(*) FROM security.ConfirmUser) AS NVARCHAR(10));

INSERT INTO #o (line) VALUES (@out);

SET @out = N'';
SELECT @out = @out + N'V3|security.LoginDetails|COLS|'
            + STUFF((SELECT N',' + c.name
                       FROM sys.columns c
                      WHERE c.object_id = OBJECT_ID(N'security.LoginDetails')
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'')
            + N'|rows=' + CAST((SELECT COUNT(*) FROM security.LoginDetails) AS NVARCHAR(10));

INSERT INTO #o (line) VALUES (@out);
GO


/* ---- V4) fiscal-year databases + accounting flag ------------------------ */
DECLARE @out NVARCHAR(MAX) = N'';

SELECT @out = @out + N'V4|sal_mali|COLS|'
            + STUFF((SELECT N',' + c.name
                       FROM sys.columns c
                      WHERE c.object_id = OBJECT_ID(N'dbo.sal_mali')
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'');

INSERT INTO #o (line) VALUES (@out);

SET @out = N'V4|sal_mali|nam_db=' + ISNULL((SELECT TOP 1 nam_db FROM dbo.sal_mali), N'<null>')
         + N'|rows=' + CAST((SELECT COUNT(*) FROM dbo.sal_mali) AS NVARCHAR(6))
         + N'|overal_setting_rows=' + CAST((SELECT COUNT(*) FROM dbo.overal_setting) AS NVARCHAR(6))
         + N'|IsAccountingSystemStarted=' + ISNULL(CAST(dbo.IsAccountingSystemStarted() AS NVARCHAR(10)), N'null');

INSERT INTO #o (line) VALUES (@out);
GO


/* ---- print everything that was collected -------------------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + ISNULL(line, N'<null line>') + CHAR(10) FROM #o ORDER BY seq;

IF @out IS NULL OR LEN(@out) = 0
    SET @out = N'*** no output - please report this: run the WHOLE file (Ctrl+A, then F5) ***' + CHAR(10);

SET @out = N'=== output lines received: ' + CAST((SELECT COUNT(*) FROM #o) AS NVARCHAR(3))
         + N' of 8 expected (V1, V1b, V1c, V2, V3 x2, V4 x2) ======' + CHAR(10) + @out
         + N'=== what we need back: this output. V1 must say MATCH and V2 must say 0. ===';

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
DROP TABLE #o;
DROP TABLE #cfg;
GO

/* ===========================================================================
   END OF PART 7 (v2).
   If V1 says MATCH and V2 says wrong_password_rows=0, the app's login is proven
   against the real database: user_name + CONVERT(varchar(50), user_password) +
   active = 1, on dbo.sys_users. Send the Messages tab back.
   =========================================================================== */
