/* ===========================================================================
   Vizitor - audit part 4: the TCP port of this instance  -  v1  -  2026-09-18
   READ ONLY, pure ASCII, no BOM, output is a handful of lines.
   ---------------------------------------------------------------------------
   WHY: the Android app must know on which TCP port SQL Server listens. The
   default instance usually listens on 1433, but "usually" is not good enough -
   this prints the real listening port(s) of the instance.

   Run it in SSMS on [Meelano] and send the Messages tab. If everything below
   is skipped for permissions, use the CMD alternative printed at the end.
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor port check - v1 (2026-09-18)';
GO

/* ---- 1) the actual listening TCP ports (SQL Server 2012+) --------------- */
DECLARE @out NVARCHAR(MAX) = N'';
BEGIN TRY
    SELECT @out = @out + N'PORT|listener|' + ISNULL(ip_address, N'-') + N'|port=' + CAST(port AS NVARCHAR(8))
                + N'|' + ISNULL(type_desc, N'-') + N'|' + ISNULL(state_desc, N'-') + CHAR(10)
    FROM sys.dm_tcp_listener_states
    ORDER BY port, ip_address;
END TRY
BEGIN CATCH
    SET @out = @out + N'PORT|dm_tcp_listener_states SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH

/* ---- 2) registry values (SuperSocketNetLib + authentication mode) ------- */
BEGIN TRY
    SELECT @out = @out + N'REG|' + registry_key + N'|' + ISNULL(value_name, N'-') + N'|'
                + ISNULL(CAST(value_data AS NVARCHAR(256)), N'<null>') + CHAR(10)
    FROM sys.dm_server_registry
    WHERE registry_key LIKE N'%SuperSocketNetLib%'
       OR value_name IN (N'LoginMode', N'LoginAuditLevel')
    ORDER BY registry_key, value_name;
END TRY
BEGIN CATCH
    SET @out = @out + N'REG|registry SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH

/* ---- 3) what the app needs to be told, in plain words ------------------ */
SET @out = @out + N'INFO|instance_name=' + ISNULL(CAST(SERVERPROPERTY('InstanceName') AS NVARCHAR(128)), N'(default instance)')
          + N'|servername=' + CAST(SERVERPROPERTY('ServerName') AS NVARCHAR(128)) + CHAR(10)
          + N'INFO|if PORT above shows 1433, enter 192.168.1.150 and port 1433 in the app.'
          + N' If it shows another number, enter that number.' + CHAR(10);

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO

/* ---- 4) alternative that never needs extra permissions -----------------
   On the SERVER (CMD, as administrator):
       NETSTAT -ANO | FINDSTR LISTENING | FINDSTR 143
   From a PHONE or PC connected to the SHOP Wi-Fi (PowerShell):
       Test-NetConnection 192.168.1.150 -Port 1433
   The first shows which port the instance listens on, the second proves that
   a device inside the shop LAN can actually reach it - which is exactly what
   the Android app will do.
   ------------------------------------------------------------------------- */
