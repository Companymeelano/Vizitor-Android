/* ===========================================================================
   Vizitor - step 1: server preparation  -  v2  -  2026-09-18
   ===========================================================================
   NON-DESTRUCTIVE and IDEMPOTENT: it only CREATES what is missing and never
   deletes, renames or alters any existing object. Safe to run more than once.

   What it does
     1. creates a dedicated SQL login for the Android app (vizitor_android)
        - do NOT let the app use the AdminAn account (least privilege)
     2. creates that login as a database user inside the ERP database and adds
        it to db_datareader, then grants ONLY the two write capabilities required
        by the verified pre-invoice path: EXECUTE on dbo.add_sail_pish and
        INSERT on dbo.subsailtemp_pish. No UPDATE/DELETE/DDL permission is granted.
     3. prints whether the server allows SQL logins at all (mixed mode)
     4. does NOT touch the Windows firewall - the two NETSH commands are
        printed at the end for you to run in CMD on the server

   How to run
     * SSMS, logged in as sa or another sysadmin account, on the server.
     * Database dropdown does not matter: the script targets @dbName directly.
     * F5, then read the Messages tab (every line starts with a section label).

   This file is pure ASCII and has no BOM on purpose: an invisible BOM at the
   start of a .sql file makes SSMS report
       "Msg 102 ... Incorrect syntax near '<invisible>'".
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor server preparation - script version v2 (2026-09-18)';
GO


/* ###########################################################################
   ###  EDIT THE TWO LINES BELOW, THEN RUN THE WHOLE SCRIPT                 ###
   ###########################################################################
   @dbName          : the ERP database. On this server it is Meelano.
   @appLoginPassword: the password for the app login vizitor_android.
                      Replace the placeholder with a long random password,
                      save it in your password manager (you will type the same
                      password once inside the Android app Settings screen),
                      then delete this .sql file or clear the line again.

   NOTE: the password of the ERP login (AdminAn) is never used, printed or
   changed by this script.
   ########################################################################### */

DECLARE @dbName            SYSNAME       = N'Meelano';
DECLARE @appLoginPassword  NVARCHAR(128) = N'REPLACE_THIS_with_a_long_random_password_2026';
DECLARE @loginName         SYSNAME       = N'vizitor_android';

/* ---- 1) does the database exist? ---------------------------------------- */
IF DB_ID(@dbName) IS NULL
BEGIN
    PRINT N'01|FAILED|database [' + @dbName + N'] was not found on this server.'
        + N' Set the correct name in the @dbName line and run the script again.'
        + N' NOTHING was changed on the server.';
    RETURN;                      -- stop here: nothing else makes sense
END
PRINT N'01|OK|database [' + @dbName + N'] found.';

/* ---- 2) server login (created only if missing) -------------------------- */
IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name = @loginName)
    PRINT N'02|login [' + @loginName + N'] already exists - left untouched (password NOT changed).';
ELSE
BEGIN
    /* built dynamically only to keep every name in one place; the names come
       from the declared variables above, never from user input */
    DECLARE @sql NVARCHAR(MAX) =
        N'CREATE LOGIN ' + QUOTENAME(@loginName)
      + N' WITH PASSWORD = N''' + REPLACE(@appLoginPassword, N'''', N'''''') + N''','
      + N' DEFAULT_DATABASE = ' + QUOTENAME(@dbName) + N','
      + N' CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
    BEGIN TRY
        EXEC (@sql);
        PRINT N'02|OK|login [' + @loginName + N'] created (CHECK_POLICY = ON, CHECK_EXPIRATION = OFF).';
    END TRY
    BEGIN CATCH
        PRINT N'02|FAILED|could not create the login: ' + ERROR_MESSAGE();
    END CATCH
END

/* ---- 3) database user + read-only role, inside the ERP database --------- */
DECLARE @sqlUser NVARCHAR(MAX) =
    N'USE ' + QUOTENAME(@dbName) + N';
      IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name = N''' + REPLACE(@loginName, N'''', N'''''') + N''')
          CREATE USER ' + QUOTENAME(@loginName) + N' FOR LOGIN ' + QUOTENAME(@loginName) + N';
      IF NOT EXISTS (SELECT 1
                       FROM sys.database_role_members rm
                       JOIN sys.database_principals rp ON rp.principal_id = rm.role_principal_id
                       JOIN sys.database_principals mp ON mp.principal_id = rm.member_principal_id
                      WHERE rp.name = N''db_datareader''
                        AND mp.name = N''' + REPLACE(@loginName, N'''', N'''''') + N''')
          ALTER ROLE db_datareader ADD MEMBER ' + QUOTENAME(@loginName) + N';';
BEGIN TRY
    EXEC (@sqlUser);
    PRINT N'03|OK|user [' + @loginName + N'] is in place inside [' + @dbName + N'] and is a member of db_datareader.';
END TRY
BEGIN CATCH
    PRINT N'03|FAILED|could not create the user/role: ' + ERROR_MESSAGE();
END CATCH

/* ---- 3b) exact least-privilege write permissions for pre-invoice --------- */
DECLARE @sqlWrite NVARCHAR(MAX) =
    N'USE ' + QUOTENAME(@dbName) + N';
      GRANT EXECUTE ON OBJECT::dbo.add_sail_pish TO ' + QUOTENAME(@loginName) + N';
      GRANT INSERT ON OBJECT::dbo.subsailtemp_pish TO ' + QUOTENAME(@loginName) + N';';
BEGIN TRY
    EXEC (@sqlWrite);
    PRINT N'03W|OK|pre-invoice write permissions granted: EXECUTE add_sail_pish + INSERT subsailtemp_pish.';
END TRY
BEGIN CATCH
    PRINT N'03W|FAILED|pre-invoice write permissions could not be granted: ' + ERROR_MESSAGE();
END CATCH

/* ---- 4) verify the membership (the important line to check) -------------- */
DECLARE @sqlCheck NVARCHAR(MAX) =
    N'SELECT N''04|role_membership'' AS section, DB_NAME() AS database_name,'
  + N'       mp.name AS member_name, rp.name AS role_name'
  + N'  FROM sys.database_role_members rm'
  + N'  JOIN sys.database_principals rp ON rp.principal_id = rm.role_principal_id'
  + N'  JOIN sys.database_principals mp ON mp.principal_id = rm.member_principal_id'
  + N' WHERE mp.name = N''' + REPLACE(@loginName, N'''', N'''''') + N''';';
BEGIN TRY
    EXEC (@sqlCheck);
END TRY
BEGIN CATCH
    PRINT N'04|verification failed: ' + ERROR_MESSAGE();
END CATCH

DECLARE @sqlPermCheck NVARCHAR(MAX) =
    N'USE ' + QUOTENAME(@dbName) + N';
      SELECT N''04W|permissions'' AS section, DB_NAME() AS database_name,
             HAS_PERMS_BY_NAME(N''dbo.add_sail_pish'', N''OBJECT'', N''EXECUTE'') AS can_exec_add_sail_pish,
             HAS_PERMS_BY_NAME(N''dbo.subsailtemp_pish'', N''OBJECT'', N''INSERT'') AS can_insert_subsailtemp_pish;';
BEGIN TRY
    EXEC (@sqlPermCheck);
END TRY
BEGIN CATCH
    PRINT N'04W|verification failed: ' + ERROR_MESSAGE();
END CATCH

/* ---- 5) is the server accepting SQL logins? (mixed mode) ---------------- */
DECLARE @loginMode NVARCHAR(16) = NULL;
BEGIN TRY
    SELECT @loginMode = CAST(value_data AS NVARCHAR(16))
    FROM sys.dm_server_registry
    WHERE value_name = N'LoginMode';
END TRY
BEGIN CATCH
    SET @loginMode = NULL;             -- needs VIEW SERVER STATE; not critical
END CATCH

IF @loginMode = N'2'
    PRINT N'05|OK|LoginMode = 2 (mixed authentication): SQL logins such as ['
        + @loginName + N'] can connect.';
ELSE IF @loginMode IS NULL
    PRINT N'05|UNKNOWN|could not read LoginMode (needs VIEW SERVER STATE). If the app'
        + N' cannot log in, check: SSMS > right-click server > Properties > Security >'
        + N' "SQL Server and Windows Authentication mode", then restart the SQL service.';
ELSE
    PRINT N'05|ATTENTION|LoginMode = ' + @loginMode + N' (Windows authentication only).'
        + N' The app login cannot connect until mixed mode is enabled in SSMS >'
        + N' Server Properties > Security, followed by a restart of the SQL Server service.';

/* ---- 6) what to do next ------------------------------------------------- */
PRINT N'06|NEXT|1) run the read-only audit script 00_audit_atiran2.sql in SSMS with'
    + N' RESULTS TO TEXT and send the whole Messages tab back.'
    + N' 2) then run these two CMD commands ON THE SERVER as administrator to let only'
    + N' the shop LAN reach SQL Server (port 1433 stays closed to the internet):';
PRINT N'   NETSH ADVFIREWALL FIREWALL DELETE RULE NAME="Vizitor SQL (LAN)"';
PRINT N'   NETSH ADVFIREWALL FIREWALL ADD RULE NAME="Vizitor SQL (LAN)" DIR=IN ACTION=ALLOW PROTOCOL=TCP LOCALPORT=1433 REMOTEIP=192.168.1.0/24';
PRINT N'   and check the real port of the instance with:  NETSTAT -ANO | FINDSTR LISTENING | FINDSTR 143';
PRINT N'06|DONE|server preparation finished.';
GO


/* ===========================================================================
   NOTE ON A "NAMED INSTANCE"
   If NETSTAT shows no line on port 1433, the instance is not listening on the
   default port. Open SQL Server Configuration Manager on the server:
     SQL Server Network Configuration > Protocols for <instance> > TCP/IP
       > Enabled = Yes, then Properties > IP Addresses > IPAll > TCP Port = 1433
   and restart the SQL Server service. This only fixes which port the instance
   listens on; it does not change or delete any data.
   =========================================================================== */
