/* ===========================================================================
   Vizitor - audit part 13: is there a real pre-invoice to learn from?
   READ ONLY, pure ASCII, no BOM. Run in SSMS (F5) - any database, this script
   prints its own DB_NAME() first, so the answer always says where it came from.
   ---------------------------------------------------------------------------
   Why: the app's write path needs five head values that exist nowhere else
   (ted_rooz, ph_kh, mod_darsad_vis, rdf_sarbarg, gainall). The ERP writes them
   when a pre-invoice is saved from its own sales screen, so we need ONE real
   pre-invoice in this database. These queries say whether there is one yet.
   =========================================================================== */
SET NOCOUNT ON;

SELECT DB_NAME() AS db;

/* 1) how many pre-invoices exist here? (0 = none saved yet, nothing to learn) */
SELECT DB_NAME() AS db,
       (SELECT COUNT(*) FROM dbo.sailfact_pish)    AS head_rows,
       (SELECT COUNT(*) FROM dbo.subsailfact_pish) AS line_rows;
GO

/* 2) the newest pre-invoice: the head (this is the reference for the 5 values) */
SELECT TOP 5 * FROM dbo.sailfact_pish ORDER BY shfacfo DESC;
GO

/* 3) and its lines (this is the reference for tedvah / tedjoz / vahprice / linesum) */
SELECT * FROM dbo.subsailfact_pish
 WHERE shfacfo = (SELECT MAX(shfacfo) FROM dbo.sailfact_pish)
 ORDER BY rdf__ DESC, RDF;
GO

/* 4) how many columns each table has here - useful when the two databases differ */
SELECT 'sailfact_pish' AS tbl, COUNT(*) AS columns_
  FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'sailfact_pish'
UNION ALL
SELECT 'subsailtemp_pish', COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'subsailtemp_pish'
UNION ALL
SELECT 'subsailfact_pish', COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'subsailfact_pish';
GO
