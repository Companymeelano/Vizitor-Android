# ERP write path — the four procedures, read verbatim from the live server

Source: `sql/03_dump_proc_bodies.sql` run by the operator on 2026-09-18 against
`Meelano` (SSMS page headers `MIGHTY.Meelano (MIGHTY\MeeLano-Pc)`). Everything below
is copied from those bodies — parameter lists, INSERT column lists and value lists.
Long lines were re-joined from the chunked output; nothing was invented.

`overal_setting` flags the bodies read (id → meaning, taken from the code itself):

| id | used by | meaning in the code |
|---|---|---|
| 67 | `IsAccountingSystemStarted()` | 1 = accounting started |
| 77 | `add_sail_pish` | =1 → pre-invoice is auto-confirmed for accounting (`TaedHesabdari=1`, user `اتوماتيك`) |
| 78 | `add_sail_pish` | =1 → pre-invoice is auto-confirmed for sales (`TaedForush=1`, user `اتوماتيك`) |
| 97 | `AddInvoice` | days the invoice date is pushed forward when it comes from a pre-invoice |
| 135 | `FixMojodi` | `'Ex'` → ledger description gets extra text (not needed by the app) |
| 168 | `AddInvoice` | =1 → the customer ledger line includes the customer name |

## 1. `dbo.add_sail_pish` — creates a PRE-INVOICE (`sailfact_pish`)

26 parameters, exact order:

```
@date char(10), @shmo bigint, @barbari money, @tozih varchar(500), @vis_rdf int,
@sumlineall money, @all money, @gainall money, @tafif money, @jamtakhgh money,
@done_date char(10), @user varchar(300), @panevis varchar(80) = 'ذکر نشده',
@rdf_sarbarg int, @rdf_tahbarg int, @modpar int, @ph_kh int, @mod int,
@mod_darsad_vis int, @nah_par int, @sh_fac bigint, @id_en bigint OUTPUT,
@ted_rooz int, @sysid int = 1, @tax money, @avarez money
```

Flow:

1. The whole insert is guarded by `if @mod = 1` (with any other `@mod` the procedure
   silently does nothing but still runs the confirmation blocks below).
2. `begin transaction forosh` … `commit transaction forosh`.
3. New pre-invoice number: `@sh_f = 1` when `count(*)` of `sailfact_pish` is 0,
   otherwise `@sh_f = max(shfacfo) + 1`. `@sh_fac` (a parameter) is **not used** for this.
4. `@man_m = (select man from customers where shmo = @shmo)` — the customer's balance
   is stored into the pre-invoice as `man_gh`.
5. One row is inserted into `sailfact_pish` — twice in the body, once for
   `@vis_rdf > 0` and once for `@vis_rdf = -1` (both inserts are byte-identical):

```
insert into sailfact_pish
 (rdf__,shfacfo,user__,[date],shmo,barbari,shfacthand,vis_rdf,sumlineall,
  [all],gainall,tafif,jamtakhgh,done_date,panevis,isret,ismodify,
  active,modpar,rdf_sarbarg,rdf_tahbarg,nah_par,mod_darsad_vis,man_gh,sh_f,
  user_f,date_f,ted_rooz,taeed,taeedUser,sysid,rejected,tax,avarez)
values
 (1,@sh_f,@user,@date,@shmo,@barbari,@tozih,@vis_rdf,@sumlineall,
  @all,@gainall,@tafif,@jamtakhgh,@done_date,@panevis,0,0,
  't',@modpar,@rdf_sarbarg,@rdf_tahbarg,@nah_par,@mod_darsad_vis,@man_m,0,
  '--','--',@ted_rooz,0,'--',@sysid,0,@tax,@avarez)
```

   then `set @id_en = @sh_f` → **the OUTPUT parameter returns the new `shfacfo`**.
   Note `sh_f = 0` (the pre-invoice is not yet invoiced) and `rejected = 0`.
6. After the commit, if `overal_setting.id = 77` is 1 →
   `update sailfact_pish set TaedHesabdari=1, UserTaedHesabdari='اتوماتيك', DateTaedHesabdari=@date where shfacfo=@sh_f`;
   if id = 78 is 1 → the same for `TaedForush` / `UserTaedForush` / `DateTaedForush`.
7. **The procedure does not insert the line items.** The ERP's UI writes
   `subsailfact_pish` itself; the app must do the same. The exact pattern is expected
   from `dbo.Edit_sail_pish` (requested; see "still missing" below).

## 2. `dbo.AddInvoice` — turns a pre-invoice into a real invoice (`sailfact`)

41 parameters, exact order:

```
@username nvarchar(100), @date char(10), @shmo bigint, @barbari money,
@description nvarchar(500), @vis_rdf int, @sumlineall money, @all money, @tafif money,
@SumTafifAghlam money, @done_date char(10), @panevis nvarchar(1000) = 'ذکر نشده',
@modpar int, @rdf_tahbarg int, @nah_par int, @nah_d_text nvarchar(200),
@driver_name varchar(70), @rdf_driver int, @mamorp_name varchar(70), @rdf_mamorp int,
@bamandeh int, @shpish bigint, @batarikh int, @chap_f bit, @chap_h bit, @tax money,
@moname nvarchar(500), @nahve_namayesh_daryaft int, @vazn decimal(18,2),
@avarez money = 0, @sysid int = 1, @userid int, @id_en bigint OUTPUT,
@VisitorPoorsant money, @ShSanadFerestande nvarchar(500), @ExternalCosts decimal(18,0),
@HajmiOverall decimal(18,0), @chapWithTasvie bit, @chapWithMande bit
```

Its own comments name the steps; the body implements them like this:

1. **Factor number**: `@shf_temp = 1` when `sailfact` is empty, else `max(shfacfo)+1`.
   The ledger text is built as
   `'فروش فاكتور شماره <n> ' + @description` (with the customer name prepended when
   `overal_setting.id = 168` is 1). `@man_m = customers.man`.
2. **Date push**: `@Setting97 = overal_setting.value where id = 97`; if `@shpish > 0`
   and `@Setting97 > 0` then `@date = dbo.what_date(@date, @Setting97)`.
3. **INSERT INTO `sailfact`** with `rdf__ = 1`, `active = 't'`, `ismodify = 'f'`,
   `tasvieh = 'f'`, `man_gh = @man_m`, `t_date = dbo.what_date(@date, @modpar)`,
   `time_ = substring(convert(varchar(50),getdate()),13,7)`, `Status = 0`,
   `sh_taraz_kh = 0`, `bamandeh = @bamandeh`, `shpish = @shpish`, `batarikh = @batarikh`,
   `description/panevis/driver/mamorp/nah_par/nah_d_text/vazn/sysid/avarez/tax` from the
   parameters, and every `*_fel` column = `round(<same value>, 0, 1)`.
   Then `set @id_en = @shf_temp`.
4. **Mark the pre-invoice as invoiced** (only when `@shpish <> 0`):
   `update sailfact_pish set sh_f = @shf_temp, user_f = @username, date_f = @date where shfacfo = @shpish`.
5. If `isnull(@tax,0) + isnull(@avarez,0) <> 0` →
   `exec Addmaliyat @date, 'فروش فاکتور شماره<n>', 0, @TaxVaAvarez, @userid, 2, @id_en`.
8. Ledger line:
   `insert into cust_act (shmo,date,act_bes,act_bed,act_dis,act_id,UserID,ghno,done_date,t_time,ShowInReport,sysid)
    values (@shmo,@date,0,@all,@descriptionCustAct,20,@userid,@shf_temp,@done_date,GETDATE(),1,@sysid)`.
9. `exec dbo.FixManCustomer @shmo`.

Transaction: `begin transaction Invoice` … `commit transaction Invoice`.
Steps 6 and 7 (visitor commission, and commission from the tablet) are **not
implemented in this body** — only the comments remain, so the app is not expected to
do anything about them.

**Like `add_sail_pish`, this procedure does not insert the invoice lines
(`subsailfact`).** The final invoice's lines are written by the ERP UI.

## 3. `dbo.new_cust` — creates a customer

50 parameters; the first ones in order:

```
@moname varchar(50), @code varchar(25), @shhes varchar(20), @bankname varchar(20),
@bankshobe varchar(30), @addre varchar(70), @tell1 varchar(20), @tell2 varchar(20),
@cell varchar(20), @active char(1), @credit money, @man money, @peygham varchar(100),
@special char(1), @rdf_city int, @sh_region int, @group_rdf int, @act_bed money,
@act_bes money, @act_bedbes char(2), @user varchar(30), @date char(10),
@done_date char(10), @id_en bigint OUTPUT, @pic image = null, @sh_i_m int = 0,
@sharh text, @id_en1 bigint OUTPUT, @vis_rdf int, @shomare_masir int,
@check_eteb int, @just_naghdi int, @c_egh varchar(100), @c_mel varchar(100),
@c_pos varchar(100), @kind int = 1, @IsEmp int, @MaxManFactor int, @Ecode_Vis int = null,
@ethadie int, @shenase_egh varchar(100) = null, @pertype int, @sysid int = 1,
@username nvarchar(100) = null, @password nvarchar(500) = null, @DateCheck int = null,
@PriceCheck decimal(18,3) = null
```

Flow:

1. `set xact_abort on` + `begin transaction t1` → **the procedure owns its transaction;
   never call it inside another one**, and never re-use the name `t1` elsewhere.
2. `@kind = (select AccType from dbo.Custgroup where group_rdf = @group_rdf)`.
3. Duplicate guard: `select shmo from customers where moname = @moname`; if any row →
   `raiserror ('نام تكراري است',16,1)` and `rollback transaction t1`.
4. A cursor over **all** customers bumps `sh_i_m` for the rows whose `shomare_masir`
   equals `@shomare_masir` and whose `sh_i_m >= @sh_i_m`.
5. `insert into customers (...)` with the columns
   `[MONAME],[code],[SHHES],[BANKNAME],[bankshobe],[addre],[tell1],[tell2],[cell],
   [active],[cred],[man],[peygham1],[special],[rdf_city],[rdf_region],[group_rdf],[date],
   [sh_i_m],[sharh],[vis_rdf],[user_d],[shomare_masir],[defi_vis],[hesab_status],
   [maxopen_time],[check_eteb],[just_naghdi],[black_list],[result_m],[c_egh],[c_mel],
   [c_pos],[kind],[IsEmp],[MaxManFactor],[RDF_masir],[Lat],[Lng],[TafsilCode],[Ecode_Vis],
   EtehadieID,PersonalityType,Shenaseh_Egh,Username,[Password],CheckDateDay,PriceCheck`
   and fixed literals worth remembering: `hesab_status = 1`, `maxopen_time = '99/12/29'`,
   `black_list = 0`, `result_m = 'ذکرنشده'`, `TafsilCode = '-1'`, `PersonalityType = 1`,
   `Lat = 0`, `Lng = 0`, `defi_vis = @vis_rdf` (the same parameter as `vis_rdf` again).
6. `set @a = @@identity; set @id_en = @a` → **the new SHMO** (the second OUTPUT
   parameter `@id_en1` stays unused).
7. `insert into cus_image (shmo,[image]) values(@id_en,@pic)` (even when `@pic` is null).
8. `insert into cust_act (shmo,[date],act_bes,act_bed,act_dis,act_id,ghno,done_date,t_time,ShowInReport,sysid,isActive,UserID)
    values (@a,@date,@act_bes,@act_bed,'حساب قبلي',0,0,@done_date,getdate(),1,1,1,1)`.
9. `insert into sys_cus values (1,@a,1)` → the new customer is granted to system 1 and
   user 1 (hard-coded).
10. `if dbo.IsAccountingSystemStarted() = 1 → exec dbo.AssignTafsilCodeToEntity 1,@a`.
11. `exec FixManCustomer @a` → recalculates the customer's balance.
12. `commit transaction t1`.

## 4. `dbo.FixMojodi (@Shfac bigint, @state int)`

`set xact_abort on` + `begin transaction a` … `commit transaction a`.
`@state` selects the operation (the body's own comments):

| state | meaning (comment in the body) | what the app cares about |
|---|---|---|
| 1 | sales invoice (`فاكتور فروش`) | loops `subsailfact` of `@Shfac` → `exec UpdateMojodiInventory @shka`, `exec UpdateMojodiInventoryAnbars @shka`, and `UpdateMojodiInventoryAnbarsPS` when `ProductionSeriesID` is not null |
| 2 | new purchase | not used by the app |
| 3 | purchase reversal | not used |
| 4 | purchase edit | not used |
| 5 | store sale (`فاكتور فروشگاهي`) | writes the stock formula inline (below) |
| 6 | store sale edit | same formula with the delta of the last two `rdf__` rows |

The stock formula, written out in state 5 and 6 (this is the ERP's own conversion —
`sellable = mojkavah × mohvah + mojkajoz`, in **whole boxes and loose pieces**):

```
if inventory.mohvah = 1:
    update inventory        set mojkavah = mojkavah - (@TedVah_f + @TedJoh_f), mojkajoz = 0
    update inventory_anbars set mojkavah = mojkavah - (@TedVah_f + @TedJoh_f), mojkajoz = 0
else:
    @Kol = @TedVah_f * @mohvah + @TedJoh_f
    mojkavah = floor((((mojkavah * @mohvah) + mojkajoz) - @Kol) / @mohvah)
    mojkajoz = (((mojkavah * @mohvah) + mojkajoz) - @Kol) % @mohvah
```

Consequence for the app: stock must never be adjusted by hand. After a final invoice
is created, call `dbo.FixMojodi @Shfac = <new sailfact.shfacfo>, @state = 1` and let the
ERP do exactly what its own screens do.

## Still missing for a complete write path (requested from the operator)

1. `dbo.Edit_sail_pish` (3091 chars) — the pre-invoice **edit** path; it is the
   expected place where the ERP inserts/updates `subsailfact_pish` lines.
2. `dbo.AddFromAtiranDetailsForVisitors` (843) — how the tablet-originated lines are added.
3. `dbo.SelectPriceAndTedvahForushVisitorhaByDate` (1066) — the ERP's own
   price + available-quantity lookup for a visitor (the fallback rule for price tiers).
4. The column names of `dbo.VW_InventoryAnbars` (the computed sellable-stock column),
   because its definition arrived truncated.

## 5. `dbo.Edit_sail_pish` — what "editing" a pre-invoice really does (versioning!)

26 parameters: the same head fields as `add_sail_pish` plus `@Shfacfo` and `@Promotion`.

1. `if @mod = 1` → `begin transaction forosh`
2. `@VisIDOld = (select VisitID from sailfact_pish where shfacfo = @Shfacfo and active = 't')`
3. `update sailfact_pish set active = 'f', ismodify = 't' where shfacfo = @Shfacfo`
4. `update subsailfact_pish set active = 'f' where shfacfo = @Shfacfo` — **every old line is retired**
5. `@RDF__ = (select MAX(rdf__) from sailfact_pish where shfacfo = @Shfacfo) + 1`
6. insert a NEW head row with the same `shfacfo` and the new `rdf__`, `active = 't'`,
   `ismodify = 0`, `sh_f = 0`, `user_f = '--'`, `date_f = '--'`, and `VisitID = @VisIDOld`,
   `Promotion`
7. commit, then the same `overal_setting` 77 / 78 auto-confirm blocks (with `and active = 't'`)

Three consequences the app cannot ignore:

* `sailfact_pish.rdf__` is a **version counter**, not a row id. Every edit inserts
  `(shfacfo, rdf__ + 1)` and retires the previous rows; the live pre-invoice is the row
  with `active = 't'`.
* A `subsailfact_pish` line belongs to a head row **by `(shfacfo, rdf__)`** — the ERP's
  own view joins exactly that pair. New lines must carry the `rdf__` of the *current*
  head version, and after an edit they must be written again with the new version.
* Like `add_sail_pish`, this procedure **does not insert lines** — it only retires the
  old ones. The insert itself is still the piece we are hunting.

## 6. `dbo.FixManCustomer (@shmo)` — the customer balance

```
set xact_abort on; begin transaction a
@Man = ISNULL(SUM(act_bed),0) - ISNULL(SUM(act_bes),0)
       from cust_act where shmo = @Shmo and (isActive <> 0 or isActive is null)
update CUSTOMERS set man = @man where SHMO = @shmo
if overal_setting.id = 117 → exec FixTasvie @shmo
exec Fix_Sys_Mandeh_Customer @shmo
commit transaction a
```

Owns transaction `a` and re-enables `xact_abort` → never call it inside another
transaction. `cust_act.isActive` decides which ledger rows count.

## 7. Stock is recomputed from the `ka_act` ledger — never adjusted by hand

`ka_act` is the goods ledger (`shka`, `tedvah`, `tedjoz`, `tedbastebandi`, `act_id`,
`active`, `RdfAnbar`, `ProductionSeriesID`). All three procedures rebuild a quantity
from scratch:

```
@kol = SUM((tedvah * mohvah + tedjoz) * CASE WHEN act_id IN
        (20,22,5,19,18,48,26,85,133) THEN -1 ELSE 1 END)
       FROM ka_act WHERE shka = @Shka AND active = 't' [AND RdfAnbar = @anbar] [AND ProductionSeriesID = @Ps]
```

then `overal_setting.id = 8` (1 = one-unit mode) also rebuilds `tedbastebandi` the same
way, and the value is split into boxes/pieces:

```
mohvah = 1        → mojkavah = @kol,                 mojkajoz = 0
@kol / mohvah >= 0 → mojkavah = floor(@kol/mohvah),   mojkajoz = @kol % mohvah
otherwise          → mojkavah = -floor(abs(@kol/mohvah)), mojkajoz = @kol % mohvah
```

* `dbo.UpdateMojodiInventory @shka` → the whole-product row in `inventory`
* `dbo.UpdateMojodiInventoryAnbars @shka` → cursor over every `inventory_anbars.rdf_anbars`
  of that product, filtered by `ka_act.RdfAnbar`
* `dbo.UpdateMojodiInventoryAnbarsPS @shka, @anbarID, @PsID` → only when
  `inventory.WithProductionSerial = 1`, writes `Inventory_Anbars_PS` (`rdfAnbar`, `PsId`)

`FixMojodi(@Shfac, @state = 1)` calls the first two for every line of a sales invoice
(and the PS variant when `ProductionSeriesID` is not null).

## 8. `dbo.VW_InventoryAnbars` — the availability the ERP itself shows

Columns: `shka, rdf_anbars, name, tedbastebandi, mojkavah, mojkajoz, MojodiPish_vah,
MojodiPish_joz`, over `inventory_anbars ⋈ inventory` where `inventory.active = 't'`.

`MojodiPish_*` = what is left after **open pre-invoices** are taken out:

```
(mojkavah * mohvah + mojkajoz)
  - ISNULL(SUM(TEDVAH) * mohvah + SUM(TEDJOZ), 0)
```

where the sum comes from `subsailfact_pish ⋈ sailfact_pish` on
`shfacfo` **and** `rdf__`, filtered by `Rejected = 0 AND active = 't' AND sh_f = 0`
and matched on `(shka, rdf_anbar)`. The value is then split into boxes/pieces exactly
like §7 (`floor` / `%`, with the negative branch). So this is the number to show a
visitor: on-hand stock minus what other open pre-invoices already hold.

## 9. The two remaining helpers that came back

* `dbo.AddFromAtiranDetailsForVisitors` (955) — writes an Atiran "details" row through
  `AddToFromAtiranDetails`, then, when the visitor has a supervisor
  (`visitors.supervisor_rdf <> 0`), multiplies `@bed` / `@bes` by
  `visitors.supervisor_per`, resolves the supervisor's own customer ledger
  (`CUSTOMERS.TafsilID where Ecode_Vis = @super and kind <> 8`) and calls itself again
  for that supervisor. This is the *ledger* split between visitor and supervisor — not
  a pre-invoice line writer.
* `dbo.SelectPriceAndTedvahForushVisitorhaByDate` (1066) — the ERP's own "sales by
  visitor between two dates" report over `VWForushKhales ⋈ inventory ⋈ kagroup ⋈
  visitors ⋈ masir ⋈ CUSTOMERS ⋈ custgroup`, filtered by `CUSTOMERS.RDF_masir`,
  `CUSTOMERS.group_rdf` and `kagroup.group_rdf` (including child groups through
  `kagroup.ParentGroupRdf`), grouping per visitor + path. It returns
  `price = SUM(kol_price_tafif)` and `tedvah = SUM(tedvah_fel * mohvah + tedjoz_fel)`.
  It is a *report*, so it is not the price fallback rule either.

## 10. Still missing

1. **Who writes `subsailfact_pish` lines at all.** Neither `add_sail_pish` nor
   `Edit_sail_pish` inserts them, so it is either a procedure we have not found yet or
   the ERP client writes the table directly. One direct query settles it (search every
   module for the table name).
2. `dbo.AddToFromAtiranDetails`, `dbo.FixTasvie`, `dbo.Fix_Sys_Mandeh_Customer`,
   `dbo.Addmaliyat`, `dbo.what_date`, `dbo.AssignTafsilCodeToEntity`,
   `dbo.IsAccountingSystemStarted` — referenced by the bodies we now have.
3. `dbo.VWForushKhales` (used by the price report) and the column list of `dbo.ka_act`.
4. Where the `Atiran14050603` database lives: the last-write query returned **only
   `Meelano`** as a non-system database on this instance, so that older column-list
   paste came from another instance or another server.

## 11. The line writer: candidates located (module search, 2026-09-18)

The "rah 7" search (verbatim paste in `docs/audit-runs/out_12_module_search.txt`) came
back with 12 modules that mention the line tables:

| module | type | why it matters |
|---|---|---|
| `trig_sst_pish` | trigger | "sst" reads like sub-sail-temp: most likely the bridge **`subsailtemp` -> `subsailfact_pish`**. A trigger needs no caller, which is exactly why none of the procedures we read wrote the lines. |
| `InvoiceTrigger` | trigger | fires around invoicing (`sailfact` / `sailfact_pish`) |
| `pishfactor_body` | view | the ERP's own "pre-invoice with lines" reader - the reference shape for a correct write |
| `subsailFactPish` | view | same family, the lines of a pre-invoice |
| `CalcDetailsPishfactor` | scalar function | computes the detail numbers of a pre-invoice |
| `ListPishFactor` | procedure | the ERP's list screen; shows which columns it expects |
| `VW_Taraz_pish`, `VWDeatailspishFactorForush` | views | balance / detail views over pre-invoices |
| `CloseTheFiscalYear`, `RestoreToDefault` | procedures | maintenance; never called by the app |

Next: the two triggers (with their parent tables) and these views/functions. Until the
trigger body is read, the app still does not insert lines.

## 12. The staging tables and their two triggers (part 9, 2026-09-18)

Verbatim bodies: `docs/audit-runs/out_13_trigger_bodies.txt`.

| trigger | table | kind | writes |
|---|---|---|---|
| `dbo.trig_sst_pish` | `dbo.subsailtemp_pish` | INSTEAD OF INSERT, enabled | `dbo.subsailfact_pish` (pre-invoice lines) |
| `dbo.InvoiceTrigger` | `dbo.subsailtemp` | INSTEAD OF INSERT, enabled | `dbo.subsailfact` (invoice lines) **and** `dbo.ka_act` (inventory ledger) |

That closes the hole of §1.7 and §2.11: the ERP never inserts the line tables directly,
it stages the line and the trigger copies it.

### 12.1 `trig_sst_pish` - the pre-invoice line writer

```
begin transaction forosh
if @mod = 0 -> copy the staged row as-is, rdf__ and active come from the row
if @mod = 1 -> rdf__ = (select max(rdf__) from sailfact_pish where shfacfo = @shfacfo),
               active = 't'
commit transaction forosh
```

Target column list (this IS the line shape the app must produce):

```
rdf__, shfacfo, shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice, bastebandi,
tedbastebandi, linesum, linegain(0), isret, pertafif, rdf, jozgain(0), pervis,
litakhma, active, amani, Pavarez, Avarez, Ptax, Tax, PerPromotion, Mp(= modpar)
```

`mod` is a column of `subsailtemp_pish` and is the branch selector. The view
`pishfactor_body` joins the result back to the head on `shfacfo AND rdf__`, so
`mod = 1` is the mode that keeps a line on the live version.

### 12.2 `InvoiceTrigger` - the real-invoice line writer (and the only ledger writer we have seen)

Steps, exactly as the body's own comments name them:

1. read every field of the staged row into variables (`rdf__`, `shfacfo`, `shka`,
   `rdf_anbar`, `tedvah`, `tedjoz`, `vahprice`, `jozprice`, `bastebandi`,
   `tedbastebandi`, `linesum`, `pertafif`, `rdf`, `pervis`, `litakhma`, `active`, `naka`,
   `ptax`, `tax`, `avarez`, `pavarez`, `gift`, `date`, `done_date`, `UserID`, `vis_rdf`,
   `sysid`, `tafifAghlam`, `TafifLine`, `ProductionSeriesID`, `TEDVAHMain`,
   `TEDJOZMain`, `PerPromotion`, `PromotionValue`, `MultiPishFactor`, `TafifPos`,
   `TafifNaghd`, `VarietyID`) - the full column set of `dbo.subsailtemp`;
2. `@tedad = (@tedvah * inventory.mohvah) + @tedjoz`; `@mohvah` from `inventory`;
3. **date push**: if `MultiPishFactor = 0` and `sailfact.shpish > 0` (the invoice came
   from a pre-invoice) and there is exactly one `sailfact` row for that number, then if
   `overal_setting.id = 97 > 0` → `@date = dbo.what_date(@date, @Setting97)`. This is the
   trigger-side twin of the same shift inside `AddInvoice`;
4. Step 1: `insert into subsailfact` (40 named columns);
5. Step 2: `@invepgh = produce.ProductionSeries.PriceEnd` when
   `ProductionSeriesID is not null`, else `inventory.inventory_price`;
   `@Gain = dbo.cal_gain(1, @tedad, @JOZPRICE, @invepgh, @litakhma, @pervis)`;
   `insert into ka_act (...)` with `act_id = 20`, `HAct_id = 20`, `ghno = @shfacfo`,
   `act_dis = 'فروش فاكتور شماره <n> - <customer name>'`, `rdf_kh = @RDF`,
   `TamamJoz = FLOOR(@JOZPRICE - (@litakhma / @tedad))`;
6. `print('')`.

⚠️ The app **does not** write invoices in v1 (it writes pre-invoices). This section is
recorded because `ka_act` is where stock comes from - if the app ever finalises an
invoice itself, skipping these rows would silently break `UpdateMojodi*`.

### 12.3 `ListPishFactor` - the ERP's own list screen

`@Mod` selects the set, all branches share the same projection:

| `@Mod` | meaning | filter on top of the shared one |
|---|---|---|
| 1 | pending, still inside `ted_rooz` | `sh_f = 0` and `dbo.dif_date(date, @mydate) <= ted_rooz` |
| 2 | pending, overdue | `sh_f = 0` and `dbo.dif_date(date, @mydate) > ted_rooz` |
| 3 | invoiced | `sh_f <> 0` |
| 4 | all confirmed ones | (no `sh_f` filter) |

Shared filter: `TaedForush = 1 and TaedHesabdari = 1 and Rejected = 0 and
sailfact_pish.active = 't' and <line>.active = 't'`. Joins: `CUSTOMERS`, `masir`,
`[Quarter]`, `regions`, `CITYS`, `visitors` (left), `subsailfact_pish`, `inventory`.
Weight = `SUM((inventory.vahwe / inventory.mohvah) * (TEDVAH * inventory.mohvah + TEDJOZ))`.

⚠️ It joins `subsailfact_pish` on `shfacfo` **only** (no `rdf__`), so an edited
pre-invoice can repeat its lines in this list. Do not copy that join into the app.

### 12.4 The other four objects

* `CalcDetailsPishfactor(@shfacfo)` → `count(rdf__)` of `active = 't'` lines of the head.
* `pishfactor_body` → the ERP's pre-invoice-with-lines read shape (join on `shfacfo AND
  rdf__` + `anbars`); `LineSum_Takhfif_Tax = (LINESUM - litakhma) + (Avarez + Tax)`.
* `subsailFactPish` → thin view (`naka`, qty, `LINESUM`, `litakhma`, `kol = LINESUM - litakhma`);
  joins `inventory` only, so it carries every version's lines.
* `VW_Taraz_pish` → customer/route/inventory projection of pending pre-invoices, also
  joining the lines by `shfacfo` only; `sh_f = 0 and active = 't'`.
* `VWDeatailspishFactorForush` → thinnest line view (naka, vahsanj, qty, prices, `LINESUM`).

### 12.5 Tables and columns this part added to the picture

`dbo.subsailtemp_pish` (staging, ≥25 columns: `mod`, plus the 24 that the trigger copies),
`dbo.subsailtemp` (staging, the columns the InvoiceTrigger reads), `dbo.ka_act`
(column list from the trigger's insert), `dbo.masir(RDF_masir, QuarterID)`,
`dbo.[Quarter](ID, RegionId, Name)`, `dbo.regions(rdf_region, rdf_city, name_region)`,
`dbo.CITYS(RDF, name)`, `dbo.anbars(rdf_anbar, name)`, `dbo.sailfact.shpish`,
`inventory.vahwe / vahsanj / mohvah / inventory_price`,
`produce.ProductionSeries(ID, PriceEnd)`, `dbo.what_date`, `dbo.cal_gain`,
`dbo.dif_date`, `CUSTOMERS.c_mel / c_egh / c_pos / tell1 / tell2 / MANAME`.

### 12.6 What is still missing (script "راه ۹")

1. `INFORMATION_SCHEMA.COLUMNS` of `dbo.subsailtemp_pish` / `dbo.subsailfact_pish`
   (types + nullability + defaults - the app's INSERT must supply every required column);
2. one real head row + its lines from the ERP's own data (`sailfact` / `subsailfact`,
   the two tables that do have rows) to learn the numbers convention;
3. confirmation of `PerPromotion` on `subsailfact_pish` in `Meelano` itself.
