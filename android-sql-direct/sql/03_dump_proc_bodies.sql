/* ===========================================================================
   Vizitor - audit part 3: full bodies of the four invoice/customer procedures
   v1  -  2026-09-18  -  READ ONLY, pure ASCII, no BOM
   ---------------------------------------------------------------------------
   WHY: the app must follow the ERP's real transaction flow - which rows are
   inserted, in which order, inside which transaction, and which validation
   runs. That logic exists only inside these procedures:
       dbo.add_sail_pish   (pre-invoice header)
       dbo.AddInvoice      (final invoice, takes the pre-invoice id @shpish)
       dbo.new_cust        (customer creation, 50 parameters)
       dbo.FixMojodi       (stock correction after an invoice)
   Guessing any of it is not acceptable, so the bodies are read out verbatim.

   HOW TO RUN - preferred (writes everything into a file, no copy/paste):
        sqlcmd -S localhost -d Meelano -E -i 03_dump_proc_bodies.sql ^
               -o C:\path\proc_bodies.txt -y 0 -W
     then send that file.
   Alternative: run it in SSMS and copy the Messages tab.

   Output format: every line starts with the procedure name, then a chunk
   number, then 200 characters - so nothing can be truncated by any setting.
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor audit part 3 (procedure bodies) - script version v1 (2026-09-18)';
GO

DECLARE @out NVARCHAR(MAX) = N'';

/* header: name + length, so a missing proc is obvious immediately */
SELECT @out = @out + N'P|' + p.name + N'|LENGTH=' + CAST(LEN(ISNULL(m.definition, N'')) AS NVARCHAR(12))
            + CASE WHEN m.definition IS NULL THEN N'|<no definition available>' ELSE N'|' END + CHAR(10)
FROM sys.procedures p
LEFT JOIN sys.sql_modules m ON m.object_id = p.object_id
WHERE p.name COLLATE Latin1_General_CI_AS IN (N'add_sail_pish', N'AddInvoice', N'new_cust', N'FixMojodi')
ORDER BY p.name;

/* bodies, 200-character chunks (paste-safe and paste-truncation-proof) */
SELECT @out = @out + N'P|' + p.name + N'|' + CAST(num.n AS NVARCHAR(4)) + N'|'
            + SUBSTRING(m.definition, (num.n - 1) * 200 + 1, 200) + CHAR(10)
FROM sys.procedures p
JOIN sys.sql_modules m ON m.object_id = p.object_id
CROSS JOIN (SELECT TOP (80) ROW_NUMBER() OVER (ORDER BY object_id) AS n FROM sys.all_objects) num
WHERE p.name COLLATE Latin1_General_CI_AS IN (N'add_sail_pish', N'AddInvoice', N'new_cust', N'FixMojodi')
  AND num.n <= CEILING(LEN(ISNULL(m.definition, N'')) / 200.0)
ORDER BY p.name, num.n;

DECLARE @i INT = 1;
WHILE @i <= LEN(@out)
BEGIN
    PRINT SUBSTRING(@out, @i, 4000);
    SET @i = @i + 4000;
END
GO

/* ===========================================================================
   END OF PART 3.
   =========================================================================== */
