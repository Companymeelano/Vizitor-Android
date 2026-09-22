# Implementation — the pre-invoice write path (Android → SQL Server)

Status: **implemented in code, not yet enabled for live use.** The code path is
complete and parameterised; two value sets still come from the ERP itself (below).

Source of every name and rule: `docs/audit-runs/out_13_trigger_bodies.txt` (the two
trigger bodies), `docs/audit-runs/out_14_columns_and_samples.txt` (column list + types
of `subsailtemp_pish` / `subsailfact_pish` and three real invoices), and
`docs/write-path/ERP-WRITE-PROCEDURES.md` §1 (the exact `add_sail_pish` signature).

## 1. What the app does, exactly

```
Step 1  EXEC dbo.add_sail_pish ...            -> @id_en = the new shfacfo
        (the procedure owns `transaction forosh`; the app never wraps it)
Step 2  per line, one INSERT into dbo.subsailtemp_pish with mod = 1
        (the INSTEAD OF trigger trig_sst_pish copies it into dbo.subsailfact_pish,
         stamping rdf__ = max(rdf__) of the live head and active = 't')
Step 3  read the pre-invoice back through dbo.pishfactor_body
        (join on shfacfo AND rdf__ - the only correct join, see PRE-INVOICE-LINES §7.5)
```

Nothing is written into `subsailtemp_pish` itself (INSTEAD OF), so the staging table
stays empty — the same as the ERP's own behaviour.

## 2. Why the app must not wrap it all in one transaction

| object | owns |
|---|---|
| `dbo.add_sail_pish` | `begin/commit transaction forosh` |
| `dbo.trig_sst_pish` | `begin/commit transaction forosh` |
| `dbo.Edit_sail_pish` | `begin/commit transaction forosh` |
| `dbo.FixManCustomer` | `begin/commit transaction a` + `xact_abort on` |
| `dbo.new_cust` | `begin/commit transaction t1` + `xact_abort on` |

A nested `BEGIN TRAN` inside them would make the outer ROLLBACK the app's only escape
hatch and would fight `xact_abort`. So: three sequential units, with compensation by
the app if a later step fails (see §5).

## 3. The line values, as data proves them

* `LINESUM = TEDVAH * VAHPRICE + TEDJOZ * JOZPRICE` (every sampled ERP line);
* quantity and price are split the same way: `tedvah` = packages, `tedjoz` = lose
  pieces, `mohvah` = pieces per package (`inventory.mohvah`);
* `RDF` = line serial **from 0** inside the factor;
* `BASTEBANDI = '--'`, `TEDBASTEBANDI = 0`, `PERTAFIF/PERVIS/litakhma = 0`,
  `Tax/Avarez = 0` in the sample (the ERP adds the tax when the invoice is issued, via
  `dbo.Addmaliyat`);
* `ISRET`: `add_sail_pish` itself writes the literal `0` into the head's `isret`, so the
  app sends `'0'` for a normal (non-return) line. The real sample in §6 confirms it;
* `modpar` (staging) lands in `subsailfact_pish.Mp`; for a normal line the ERP's own
  factor rows leave `Mp` NULL.

## 4. Columns the app sends into `dbo.subsailtemp_pish`

```
mod, shfacfo, rdf__, rdf, shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice,
bastebandi, tedbastebandi, linesum, isret, pertafif, pervis, litakhma, active,
amani, Pavarez, Avarez, Ptax, Tax, PerPromotion, modpar
```

25 placeholders, all parameterised - no string concatenation anywhere, so a product
name or a customer note can never become SQL.

## 5. Failure handling (the rule the operator set)

* the head is created first, so a failure in step 2 leaves a head without lines;
* the app **never** rolls that back with its own `DELETE` (nothing is deleted without
  analysis). It retries the line, and if it still fails it reports to the user with the
  `shfacfo`, and calls `retirePreInvoice()` **only if** that behaviour is approved:
  `UPDATE dbo.sailfact_pish SET active = 'f', ismodify = 't' WHERE shfacfo = ? AND active = 't'`
  - which is byte-for-byte what `dbo.Edit_sail_pish` does to a version it replaces, and
  makes the half-invoice invisible to `ListPishFactor` (it filters `active = 't'`);
* idempotency stays client-side (`client_invoice_id` kept in Room, exactly as the
  current app does), because the ERP has no such column.

## 6. The one thing still missing: a real pre-invoice sample

`sailfact_pish` and `subsailtemp_pish` hold **0 rows** in both databases, so the app
would be the first writer. Five head values cannot be read from anywhere yet:

| parameter | why it matters |
|---|---|
| `@ted_rooz` | `ListPishFactor` (`@Mod = 1`) only shows a pre-invoice while `dif_date(date, @mydate) <= ted_rooz` |
| `@ph_kh` | stored in the head, meaning unverified |
| `@mod_darsad_vis` | visitor percentage, unverified |
| `@rdf_sarbarg` | stored in the head, meaning unverified |
| `@gainall` | profit total, unverified (the trigger computes gain only later, at invoicing) |

Fix (30 seconds of work in the ERP, once): create **one** pre-invoice in the ERP's own
sales screen for a visitor, save it (do not invoice it), then read it back:

```sql
SELECT TOP 5 * FROM dbo.sailfact_pish ORDER BY shfacfo DESC;

SELECT * FROM dbo.subsailfact_pish
WHERE shfacfo = (SELECT MAX(shfacfo) FROM dbo.sailfact_pish)
ORDER BY rdf__ DESC, RDF;
```

Those two rows become the reference implementation: the app fills its template from them
and the write path is enabled. Until then `createPreInvoice()` refuses to run with a
clear message instead of inventing values.
