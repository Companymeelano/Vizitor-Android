/* ===========================================================================
   Vizitor - audit part 5: only the small missing pieces  -  v1  -  2026-09-18
   READ ONLY, pure ASCII, no BOM. Output is about 40 short lines (~2.5 KB), so
   it can be copied from the Messages tab without being cut off.
   ---------------------------------------------------------------------------
   What it answers (everything else is already verified):
     G1 columns of sysobjects we still need   (sys_kal, sys_anb)
     G2 LoginMode of the server (SQL logins allowed?)
     G3 the real meaning of the flag columns (active = '1' or 'Y'? ...)
     G4 whether PWDCOMPARE can verify a sys_users password   [OPTIONAL]
     G5 tiny samples: osystems, visitors, sys_vis, sys_cus, custgroup, masir
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor audit part 5 (small gaps) - v1 (2026-09-18)';
GO

/* ---- OPTIONAL password probe -------------------------------------------
   The Android app must verify the ERP login. dbo.sys_users.user_password is
   varbinary, so the plan is to let SQL Server itself compare:
       WHERE user_name = @u AND PWDCOMPARE(@p, user_password) = 1
   To confirm this works before writing the app code, type the password of the
   ERP user you will log in with BETWEEN THE QUOTES below (it is sent to the
   server only; the output NEVER shows it - only 0/1 match results).
   If you prefer not to put a password here, leave it as it is: the script then
   just reports the stored hash lengths.
   After running, do NOT save this file and do NOT send it back.
   ------------------------------------------------------------------------ */
DECLARE @testPassword NVARCHAR(128) = N'PUT_THE_ERP_PASSWORD_HERE';
GO


/* ---- G1) columns of the tables we still do not know --------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'G1|' + t.name + N'|'
            + STUFF((SELECT N',' + c.name + N' ' + ty.name
                       FROM sys.columns c
                       JOIN sys.types ty ON ty.user_type_id = c.user_type_id
                      WHERE c.object_id = t.object_id
                      ORDER BY c.column_id FOR XML PATH('')), 1, 1, N'')
            + CHAR(10)
FROM sys.tables t
WHERE t.name COLLATE Latin1_General_CI_AS IN (N'sys_kal', N'sys_anb', N'sys_vis', N'sys_cus',
                                              N'sys_use', N'sys_wor', N'systems',
                                              N'AnbarDifferent', N'NCustomers')
ORDER BY t.name;

/* ---- G2) authentication mode of the server ------------------------------ */
BEGIN TRY
    SELECT @out = @out + N'G2|LoginMode|' + CAST(value_data AS NVARCHAR(16))
                + CASE WHEN CAST(value_data AS NVARCHAR(16)) = N'2'
                       THEN N'|OK: mixed mode (SQL logins allowed)'
                       ELSE N'|ATTENTION: Windows-only authentication' END + CHAR(10)
    FROM sys.dm_server_registry
    WHERE value_name = N'LoginMode';
END TRY
BEGIN CATCH
    SET @out = @out + N'G2|LoginMode|SKIPPED|' + ERROR_MESSAGE() + CHAR(10);
END CATCH

/* ---- G3) what the flag columns really contain --------------------------- */
SELECT @out = @out + N'G3|' + v.tbl + N'.' + v.col + N'|'
            + ISNULL(v.meaning, N'<null>') + N'|rows=' + CAST(v.n AS NVARCHAR(10)) + CHAR(10)
FROM (
    SELECT N'inventory' AS tbl, N'active' AS col, CAST(active AS NVARCHAR(10)) AS meaning, COUNT(*) AS n
      FROM dbo.inventory GROUP BY active
    UNION ALL
    SELECT N'CUSTOMERS', N'active', CAST(active AS NVARCHAR(10)), COUNT(*)
      FROM dbo.CUSTOMERS GROUP BY active
    UNION ALL
    SELECT N'forosh_price', N'active', CAST(active AS NVARCHAR(10)), COUNT(*)
      FROM dbo.forosh_price GROUP BY active
    UNION ALL
    SELECT N'kagroup', N'Active', CAST(Active AS NVARCHAR(10)), COUNT(*)
      FROM dbo.kagroup GROUP BY Active
    UNION ALL
    SELECT N'anbars', N'Active', CAST(Active AS NVARCHAR(10)), COUNT(*)
      FROM dbo.anbars GROUP BY Active
    UNION ALL
    SELECT N'custgroup', N'Active', CAST(Active AS NVARCHAR(10)), COUNT(*)
      FROM dbo.custgroup GROUP BY Active
    UNION ALL
    SELECT N'visitors', N'active', CAST(active AS NVARCHAR(10)), COUNT(*)
      FROM dbo.visitors GROUP BY active
) v;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO


/* ---- G4) can SQL Server verify the stored password? -------------------- */
DECLARE @out NVARCHAR(MAX) = N'';
SELECT @out = @out + N'G4|sys_users|id=' + CAST(user_id AS NVARCHAR(10))
            + N'|name=' + ISNULL(user_name, N'<null>')
            + N'|hash_bytes=' + ISNULL(CAST(DATALENGTH(user_password) AS NVARCHAR(6)), N'<null>')
            + N'|active=' + ISNULL(CAST(active AS NVARCHAR(10)), N'<null>')
            + N'|pwdcompare_result=' + CAST(PWDCOMPARE(N'PUT_THE_ERP_PASSWORD_HERE', user_password) AS NVARCHAR(4))
            + CHAR(10)
FROM dbo.sys_users
ORDER BY user_id;
IF LEN(@out) = 0 SET @out = N'G4|(no row in sys_users)' + CHAR(10);

/* ---- G5) tiny samples -------------------------------------------------- */
SELECT @out = @out + N'G5|osystems|rdf=' + CAST(rdf_system AS NVARCHAR(10))
            + N'|name=' + ISNULL(name_System, N'<null>')
            + N'|AnbarRdf=' + CAST(AnbarRdf AS NVARCHAR(10))
            + N'|Active=' + CAST(Active AS NVARCHAR(2)) + CHAR(10)
FROM dbo.osystems;

SELECT @out = @out + N'G5|visitor|vis_rdf=' + CAST(vis_rdf AS NVARCHAR(10))
            + N'|name=' + ISNULL(vis_name, N'<null>')
            + N'|active=' + ISNULL(active, N'<null>')
            + N'|UserID=' + ISNULL(CAST(UserID AS NVARCHAR(10)), N'<null>')
            + N'|region=' + CAST(VIs_region AS NVARCHAR(10))
            + N'|city=' + ISNULL(CAST(vis_city AS NVARCHAR(10)), N'<null>')
            + N'|supervisor=' + ISNULL(is_supervisor, N'<null>')
            + N'|per_p_d_naghd=' + ISNULL(CAST(per_p_d_naghd AS NVARCHAR(12)), N'<null>')
            + N'|per_p_d_check=' + ISNULL(CAST(per_p_d_check AS NVARCHAR(12)), N'<null>')
            + CHAR(10)
FROM dbo.visitors;

SELECT @out = @out + N'G5|sys_vis|SysID=' + CAST(SysID AS NVARCHAR(10))
            + N'|shvis=' + CAST(shvis AS NVARCHAR(10))
            + N'|UserID=' + CAST(UserID AS NVARCHAR(10)) + CHAR(10)
FROM dbo.sys_vis;

SELECT @out = @out + N'G5|sys_cus|SysID=' + CAST(SysID AS NVARCHAR(10))
            + N'|Shmo=' + CAST(Shmo AS NVARCHAR(10))
            + N'|UserID=' + CAST(UserID AS NVARCHAR(10)) + CHAR(10)
FROM dbo.sys_cus;

SELECT @out = @out + N'G5|custgroup|group_rdf=' + CAST(group_rdf AS NVARCHAR(10))
            + N'|name=' + ISNULL(group_name, N'<null>')
            + N'|price_tier=' + ISNULL(CAST(price AS NVARCHAR(6)), N'<null>')
            + N'|Active=' + ISNULL(CAST(Active AS NVARCHAR(10)), N'<null>')
            + N'|PerGain=' + ISNULL(CAST(PerGain AS NVARCHAR(12)), N'<null>') + CHAR(10)
FROM dbo.custgroup;

SELECT @out = @out + N'G5|CUSTOMERS|SHMO=' + CAST(SHMO AS NVARCHAR(10))
            + N'|name=' + ISNULL(MONAME, N'<null>')
            + N'|group_rdf=' + CAST(group_rdf AS NVARCHAR(10))
            + N'|vis_rdf=' + CAST(vis_rdf AS NVARCHAR(10))
            + N'|active=' + ISNULL(active, N'<null>')
            + N'|man=' + CAST(man AS NVARCHAR(20))
            + N'|cred=' + CAST(cred AS NVARCHAR(20))
            + N'|black_list=' + ISNULL(CAST(black_list AS NVARCHAR(10)), N'<null>')
            + CHAR(10)
FROM dbo.CUSTOMERS;

SELECT @out = @out + N'G5|masir|rdf_masir=' + CAST(rdf_masir AS NVARCHAR(10))
            + N'|region=' + CAST(rdf_region AS NVARCHAR(10))
            + N'|shomare=' + CAST(shomare_masir AS NVARCHAR(10))
            + N'|name=' + ISNULL(name, N'<null>')
            + N'|vis_rdf=' + ISNULL(CAST(vis_rdf AS NVARCHAR(10)), N'<null>') + CHAR(10)
FROM dbo.masir;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO

/* ===========================================================================
   END OF PART 5. Send the whole Messages tab (about 40 lines).
   =========================================================================== */
