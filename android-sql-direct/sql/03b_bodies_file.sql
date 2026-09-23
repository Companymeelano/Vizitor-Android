/* ===========================================================================
   Vizitor - audit part 3b: the four procedure bodies as a RESULT SET  -  v1
   2026-09-18  -  READ ONLY, pure ASCII, no BOM.
   ---------------------------------------------------------------------------
   WHY THIS EXISTS (in addition to part 3):
     part 3 prints the bodies with PRINT, which lands in the SSMS "Messages"
     tab - and this SSMS installation silently drops long Messages output.
     This file returns the same bodies as ordinary rows instead, so you can
     save them straight to a file:

        in SSMS:  press  Ctrl+Shift+F  (Query -> Results To -> Results to File)
                  BEFORE pressing F5, choose a file name, then press F5.
        or:       sqlcmd -S localhost -d Meelano -E -i 03b_bodies_file.sql ^
                         -o out_03_bodies.txt -y 0 -W

   The rows are chunked (400 characters per row) so that even a manual copy
   from the grid stays complete: read the columns  proc_name | chunk_no | chunk
   and concatenate the chunks of each procedure in chunk_no order.

   Nothing is written to the database.
   =========================================================================== */

SET NOCOUNT ON;
PRINT N'Vizitor part 3b - bodies as a result set (v1, 2026-09-18). Results to File: Ctrl+Shift+F, then F5.';

/* 1) what exists and how long it is - a missing procedure is obvious here */
SELECT p.name                              AS proc_name,
       LEN(ISNULL(m.definition, N''))      AS body_chars,
       CASE WHEN m.definition IS NULL THEN N'<no definition - check VIEW DEFINITION>'
            ELSE N'ok' END                 AS state
  FROM sys.procedures AS p
  LEFT JOIN sys.sql_modules AS m ON m.object_id = p.object_id
 WHERE p.name IN (N'add_sail_pish', N'AddInvoice', N'new_cust', N'FixMojodi')
 ORDER BY p.name;

/* 2) the bodies, 400 characters per row, in order */
;WITH nums AS (
    SELECT TOP (200) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
      FROM sys.all_objects
)
SELECT p.name AS proc_name,
       n.n    AS chunk_no,
       SUBSTRING(m.definition, (n.n - 1) * 400 + 1, 400) AS chunk
  FROM sys.procedures AS p
  JOIN sys.sql_modules AS m ON m.object_id = p.object_id
 CROSS JOIN nums AS n
 WHERE p.name IN (N'add_sail_pish', N'AddInvoice', N'new_cust', N'FixMojodi')
   AND m.definition IS NOT NULL
   AND n.n <= CEILING(LEN(m.definition) / 400.0)
 ORDER BY p.name, n.n;

/* 3) lengths of the procedures these four call - so we know what else exists */
SELECT o.name                       AS helper_name,
       LEN(ISNULL(m.definition, N'')) AS body_chars
  FROM sys.objects AS o
  LEFT JOIN sys.sql_modules AS m ON m.object_id = o.object_id
 WHERE o.name IN (N'FixManCustomer', N'get_vis_rdf', N'SetUserpass', N'GetUser',
                  N'get_role_id', N'IsAccountingSystemStarted')
 ORDER BY o.name;
