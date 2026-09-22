# Verified ERP schema — database `Meelano` (live server, 2026-09-18)

Source of truth: the real server output of `sql/00_audit_atiran2.sql` **v5**
(section 00..12). Everything below is observed, not guessed. Nothing here is
invented; where a value is unknown it is marked `?`.

Notation: `name type(len) [NOT NULL|NULL] [PK|IDENTITY]`
(`PK` = part of the primary key, `IDENTITY` = auto increment)

## 0. Server facts

| item | value | consequence for the app |
|---|---|---|
| server_name / machine | `MIGHTY` | |
| instance | **default instance** (no instance name) | confirmed listening on **0.0.0.0:1433** and `[::]:1433` (TSQL, ONLINE) |
| product_version | `12.0.2269.0` (SQL Server **2014**, RTM) | mssql-jdbc must stay on a JDBC-4.0/4.2 compatible build |
| edition | Enterprise Edition (64-bit) | |
| database | `Meelano` | connection string database name |
| compat_level | `120` | |
| db_collation | `SQL_Latin1_General_CP1256_CI_AS` | case-insensitive, Arabic/Persian code page |
| `dbo.CUSTOMERS` probe | found | the database is the ERP database |

## 1. Schemas in use

`dbo` (ERP core), **`Hamrah`** (mobile module: `Visit`, `TabletCustomer`,
`PishDaryaft*`, `Device*`, `backsail_pish`, `subBackSail_pish`),
`warehousing`, `security`, `EMS`, `kg`.

**The mobile/tablet module is `Hamrah`.** Atiran's own tablet app used it; all
those tables are empty (0 rows) on this server.

## 2. Answers to the five open VERIFY items

1. **`new_cust` is a stored procedure**, not a table (`dbo.new_cust`,
   50 parameters, `@id_en` + `@id_en1` OUTPUT). That is why section 07 reported
   it as missing from the table list. There is also `dbo.newcust`,
   `dbo.t_newcust`, and the table `dbo.NCustomers`.
2. **Login**: `dbo.sys_users` = `user_id` (PK, identity), `user_name`,
   `user_password varbinary(50) NOT NULL` → the password is stored as a
   **SQL Server hash** (PWDENCRYPT family), so it must be verified **inside SQL
   Server with `PWDCOMPARE`** — no hash scheme has to be re-implemented in
   Kotlin, and the plaintext never leaves the device (probe script `02`).
3. **No `vwVizitor*` views exist** (section 03/08 empty) — the Vizitor PHP layer
   was never installed on this server, as expected for a first deployment.
4. **Invoice procedures that DO exist**: `dbo.add_sail_pish` (25 params,
   `@id_en OUTPUT`), `dbo.AddInvoice` (39 params, `@id_en OUTPUT`, takes
   `@shpish bigint` = the pre-invoice id), `dbo.FixMojodi (@Shfac, @state)`,
   `dbo.new_cust` (50 params). **Missing**: `add_sailfact`, `subsailfact`,
   `subsailfact_pish`, `sp_add_sail_pish`, `svcAddSailFactPish`.
5. **Password scheme probe**: `sys_users.user_password` is varbinary; the
   `first3` sample is meaningless for binary data (expected). Row counts:
   `sys_users` = 2 rows, `visitors.Password` = 0 non-null values →
   `visitors.Username/Password` is not the login source; **login must be
   `sys_users`** (as the user requires).

## 3. Row counts on the live database (first deployment, confirmed)

| table | rows | | table | rows |
|---|---|---|---|---|
| `dbo.CUSTOMERS` | 7 | | `dbo.sailfact` | 3 |
| `dbo.inventory` | 50 | | `dbo.subsailfact` | 15 |
| `dbo.forosh_price` | 50 | | `dbo.sailfact_pish` | **0** |
| `dbo.kagroup` | 30 | | `dbo.subsailfact_pish` | **0** |
| `dbo.custgroup` | 9 | | `ddo.vis_goals` | 0 |
| `dbo.anbars` | 1 | | `dbo.prizePercent` | 0 |
| `dbo.sys_users` | 2 | | `dbo.visitors` | 1 |
| `dbo.sys_vis` | 1 | | `dbo.masir` / `dbo.MasirDay` | 1 / 0 |
| `dbo.sys_cus` | 7 | | `dbo.getchk`, `dbo.BANK` | 0 / 0 |
| `dbo.sys_kal` | 50 | | **all `Hamrah.*`** | **0** |
| `dbo.sys_anb` | 1 | | | |

→ No visitor has ever produced a pre-invoice: the tables the app will write
(`sailfact_pish`, `subsailfact_pish`) are completely empty.

## 4. Login & identity tables

```
sys_users:  user_id int IDENTITY PK | user_password varbinary(50) NOT NULL |
            user_name varchar(80) NULL | user_lname varchar(30) NOT NULL |
            user_fname varchar(30) NOT NULL | role_id int NULL | skin_id int NULL |
            user_pic image NULL | IsLocked bit NULL | IsLoggedIn bit NULL |
            TafsilCow nchar(10) NULL | TafsilID bigint NULL | phone nvarchar(11) NULL |
            email nvarchar(50) NULL | nationalCode nvarchar(11) NULL |
            address nvarchar(0) NULL | active bit NULL | shmo int NOT NULL |
            BackGroundAddress nvarchar(2000) NULL | IsNotePade bit NULL |
            SysuserTransferCode nvarchar(50) NULL
Roles:      id int IDENTITY PK | name varchar(30) NOT NULL | SubSystemId int NULL
visitors:   vis_rdf int IDENTITY PK | vis_name varchar(30) | vis_addre varchar(80) |
            vis_tell1/tell2/vis_cell varchar(25) | vis_man money | VIs_region int |
            viss_date char(10) | active char(1) | vis_city int NULL | h_sabet money NULL |
            image image NULL | is_supervisor char(1) NULL | supervisor_rdf int NULL |
            supervisor_per decimal(18,3) NULL | dar_z decimal(18,3) NULL | eteb money NULL |
            per_p_d_naghd / per_p_d_check / per_jar_bch decimal(18,3) NULL | kind int NULL |
            tedad_fmmt int NULL | mab_fmmt money NULL | rdf_device int NULL |
            TedadFactorMojazMande int | MablaghMojazMandeJahatFactorha decimal(18,0) |
            Type1 / Type2 bit NULL | Username nvarchar(100) NULL |
            Password nvarchar(100) NULL | UserID int NULL | rdf_device_distribution int NULL
```

**Scope / permission tables** (they define what a visitor may see):

```
sys_vis:  rdf int IDENTITY | SysID int PK | shvis int PK | UserID int
sys_cus:  rdf int IDENTITY | SysID int PK | Shmo int PK | UserID int
sys_kal:  (columns still to capture — script 02)
sys_anb:  (columns still to capture — script 02)
```

`SysID` groups rows per company/OSystem (`osystems`), `UserID` joins to
`sys_users.user_id`; `visitors.UserID` joins a visitor to his `sys_users` row.
So the chain is: login → `sys_users.user_id` → `visitors` (UserID) → `vis_rdf`
→ `sys_vis/sys_cus/sys_kal/sys_anb` (filtered by `SysID`).

## 5. Customers, groups and the Atiran 5-price-tier logic

```
CUSTOMERS:  SHMO int IDENTITY PK | MONAME nvarchar(500) | code nvarchar(250) |
            SHHES nvarchar(200) | BANKNAME/BANKSHOBE nvarchar(300) | addre nvarchar(2000) |
            tell1/tell2/cell nvarchar(200) | active char(1) | cred money | man money |
            peygham1 nvarchar(100) | special char(1) | rdf_city int NULL |
            rdf_region int | group_rdf int | date char(10) NULL | sh_i_m int NULL |
            sharh nvarchar(2550) NULL | vis_rdf int | user_d nvarchar(300) NULL |
            shomare_masir int NULL | defi_vis int NULL | hesab_status int |
            maxopen_time char(10) NULL | check_eteb int NULL | just_naghdi int NULL |
            black_list int NULL | result_m nvarchar(1000) | c_egh/c_mel/c_pos nvarchar(100) NULL |
            kind int NULL | IsEmp int NULL | MaxManFactor int NULL | RDF_masir int NULL |
            Lat/Lng float NULL | TafsilCode nchar(13) NULL | Ecode_Vis int NULL |
            PersonalityType int NULL | EtehadieID int NULL | Shenaseh_Egh nvarchar(100) NULL |
            TafsilID bigint NULL | Username nvarchar(100) NULL | Password nvarchar(500) NULL |
            PriceCheck decimal(18,3) NULL | CheckDateDay int NULL | ShmoMoaref int NULL |
            RoleCode nvarchar(20) NULL | TransferCode nvarchar(50) NULL
custgroup:  group_rdf int IDENTITY PK | group_name varchar(30) | stdate char(10) NULL |
            price int NULL            <-- THE PRICE TIER (1..5) ->
            ted_rooz int NULL | AccType int NULL | MoeinID bigint NULL |
            Active bit NULL | PerGain decimal(18,2) NULL
forosh_price: shka bigint PK | forosh1..forosh5 money NOT NULL | mp1..mp5 int NOT NULL |
            pv1..pv5 decimal(18,3) NOT NULL | naka nvarchar(500) NULL |
            group_rdf int NULL | active char(1) NULL | MinPrice money | MaxPrice money
```

Price resolution (to be implemented exactly, no fallback invented):
`CUSTOMERS.group_rdf → custgroup.price (tier n) → forosh_price.forosh<n>`
with `mp<n>`/`pv<n>` as the same tier's quantity/percent modifiers.
`prizePercent` holds range/date-based promotional percentages
(`Shka`, `FromNum`, `ToNum`, `FromPrice`, `ToPrice`, `FromDate`, `ToDate`,
`Percent`, `RdfCustGroup`, `RdfProvinc/City/Region/Masir`, `SysID`) — **empty
(0 rows)** right now.

## 6. Catalogue, stock and warehouses

```
inventory:  shka bigint IDENTITY PK | naka nvarchar(500) | coka nvarchar(500) |
            group_rdf int | vahsanj nvarchar(300) | mohvah bigint | mojkavah decimal(18,3) |
            mojkajoz int | reopoint int | bastebandi nvarchar(250) | tedbastebandi decimal(18,3) |
            vahwe/vahsp decimal(18,3) | visper decimal(18,3) NULL | active char(1) |
            pure_buy_price/buy_price/inventory_price money | MODPAR int NULL |
            buyjoz money NULL | sharh nvarchar(2000) NULL | min_sef int NULL |
            ted_f_ja/ted_ja int NULL | shka_ja bigint NULL | barbari_vahed money NULL |
            ptax decimal(18,3) NULL | inventory_price_tax money NULL |
            maxtafnaghd decimal(18,3) NULL | black_list int NULL | backfine decimal(18,2) NULL |
            ExpirationDate nchar(10) NULL | FinalSalePrice money NULL |
            PAvarez decimal(18,2) NULL | ImPureBuyPrice money | MaxJozForosh decimal(18,3) |
            PerPos decimal(18,3) NULL | GoodsKindID int NULL | InventoryTypeID int NULL |
            WithProductionSerial bit | ActiveOnlineSales bit | ActiveCapillarySales bit |
            reducePrizeFromSales bit | siteId bigint NULL | nakaEN nvarchar(500) NULL |
            isCustomerClub bit NULL | MandatoryRoleCode bit NULL | TransferCode nvarchar(50) NULL
inventory_anbars: rdf_anbars int PK | shka bigint PK | mojkavah decimal(18,3) |
            mojkajoz int | name nvarchar(250) | tedbastebandi decimal(18,3)   <- per-warehouse stock
anbars:     rdf_anbar int IDENTITY PK | name nvarchar(300) | addre nvarchar(2000) |
            start_date nvarchar(50) | tell1/tell2 nvarchar(50) | anbardar nvarchar(500) NULL |
            Active bit NULL | Base bit NULL | UserID int NULL
kagroup:    group_rdf int IDENTITY PK | group_name nvarchar(500) | stdate datetime |
            CanNegative int NULL | ParentGroupRdf int NULL | GroupLevel int NULL |
            hasPic bit NULL | Active bit | siteId bigint NULL
```

Views available for stock: **`dbo.VW_InventoryAnbars`** (plus
`warehousing.InvantoryAnbar`, `dbo.Inventory_Anbars_PS`).

## 7. Pre-invoice (پیش‌فاکتور), invoice and the mobile module

```
sailfact_pish: rdf__ int PK | shfacfo bigint PK | USER__ varchar(300) | date char(10) |
            shmo int | barbari money NULL | shfacthand nvarchar(500) NULL | vis_rdf int |
            sumlineall money | all money | gainall money | tafif money | jamtakhgh money |
            done_date char(10) | panevis varchar(80) | isret char(1) | ismodify char(1) |
            active char(1) | modpar int | rdf_sarbarg int | rdf_tahbarg int | nah_par int |
            mod_darsad_vis int | nah_d_text text | man_gh money NULL | sh_f bigint NULL |
            user_f varchar(300) NULL | date_f char(10) NULL | ted_rooz int NULL |
            taeed int NULL | taeedUser varchar(300) NULL | sysid int NULL |
            TaedHesabdari bit | TaedForush bit | UserTaedHesabdari nvarchar(500) NULL |
            DateTaedHesabdari nvarchar(10) NULL | UserTaedForush nvarchar(50) NULL |
            DateTaedForush nvarchar(10) NULL | VisitID bigint NULL | Stamp nvarchar(500) NULL |
            Rejected bit NULL | RejectedUser nvarchar(500) NULL | RejectedDate nvarchar(10) NULL |
            RejectedComment nvarchar(1000) NULL | taraz_kh_pish int NULL |
            DateRecive varchar(10) NULL | TimeRecive varchar(20) NULL | tax money NULL |
            avarez money NULL | MpKol int NULL | MpIsAuto bit NULL
subsailfact_pish: rdf__ int PK | shfacfo bigint PK | SHKA bigint | rdf_anbar int |
            TEDVAH decimal(18,3) | TEDJOZ int NULL | VAHPRICE money | JOZPRICE money |
            BASTEBANDI varchar(25) NULL | TEDBASTEBANDI int NULL | LINESUM money |
            LINEGAIN money | ISRET char(1) | PERTAFIF decimal(18,2) NULL | RDF int PK |
            jozgain money | PERVIS decimal(18,2) NULL | litakhma money | active char(1) |
            amani bit NULL | Pavarez decimal(18,2) NULL | Avarez money NULL |
            Ptax decimal(18,2) NULL | Tax money NULL | Mp int NULL
```

`Hamrah` module (all empty): `Visit` (VisitID, VisRdf, Shmo, Duration, Created,
Sent, Description, SentLat/SentLng/SaveLat/SaveLng, SignatureImage varbinary(max)),
`TabletCustomer` (id, vis_rdf, shmo, create_date, birth_date, name, melli_code,
tell1/tell2/cell, address, sharh, estijari, owners_count, metraj_shop,
metraj_yakhchal, yakhchal_count, tablo, sabeghe, Lat, Lng, Active),
`PishDaryaft` (cash receipts, 19 cols incl. VisitorID/SysID/Taeed*/Rejected*),
`PishDaryaftGetCheck/MultiFactor/Pos`, `Device*`, `vishfactor`, `backsail_pish`.

## 8. Procedures available for the write path (verified signatures)

```
dbo.add_sail_pish(@date char(10), @shmo bigint, @barbari money, @tozih varchar(500),
    @vis_rdf int, @sumlineall money, @all money, @gainall money, @tafif money,
    @jamtakhgh money, @done_date char(10), @user varchar(300), @panevis varchar(80),
    @rdf_sarbarg int, @rdf_tahbarg int, @modpar int, @ph_kh int, @mod int,
    @mod_darsad_vis int, @nah_par int, @sh_fac bigint, @id_en bigint OUTPUT,
    @ted_rooz int, @sysid int, @tax money, @avarez money)          -- 3118 chars
dbo.AddInvoice(@username nvarchar(100), @date char(10), @shmo bigint, @barbari money,
    @description nvarchar(500), @vis_rdf int, @sumlineall money, @all money, @tafif money,
    @SumTafifAghlam money, @done_date char(10), @panevis nvarchar(1000), @modpar int,
    @rdf_tahbarg int, @nah_par int, @nah_d_text nvarchar(200), @driver_name varchar(70),
    @rdf_driver int, @mamorp_name varchar(70), @rdf_mamorp int, @bamandeh int,
    @shpish bigint, @batarikh int, @chap_f bit, @chap_h bit, @tax money,
    @moname nvarchar(500), @nahve_namayesh_daryaft int, @vazn decimal(18,2), @avarez money,
    @sysid int, @userid int, @id_en bigint OUTPUT, @VisitorPoorsant money,
    @ShSanadFerestande nvarchar(500), @ExternalCosts decimal(18,0),
    @HajmiOverall decimal(18,0), @chapWithTasvie bit, @chapWithMande bit)   -- 5850 chars
dbo.FixMojodi(@Shfac bigint, @state int)                                    -- 7957 chars
dbo.new_cust(... 50 params ... @id_en OUTPUT, @id_en1 OUTPUT,
    @username nvarchar(100), @password nvarchar(500), @DateCheck int,
    @PriceCheck decimal(18,3), @vis_rdf int, @shomare_masir int, ...)        -- 3868 chars
```

Still to read (script 03 dumps the bodies): `add_sail_pish`, `AddInvoice`,
`new_cust`, `FixMojodi`. Also present and useful:
`dbo.Edit_sail_pish`, `dbo.back_sail`, `dbo.change_price`, `dbo.AddUser`,
`dbo.FixInventoryPrice`, `dbo.UpdateMojodiInventoryAnbars`,
`dbo.ListPishFactor`, `dbo.CustomerListToDate`, `dbo.set_vis_koli`,
`dbo.VisitorSalesCommision`, `dbo.GetVisitorPoints`,
`dbo.AddFromAtiranDetailsForVisitors`.

## 9. Helpful views / functions found (section 10)

Views: `dbo.pishfactors`, `dbo.pishfactor_body`, `dbo.SailFactPish_Details`,
`dbo.subsailFactPish`, `dbo.VW_InventoryAnbars`, `dbo.VW_GoalsVisitors`,
`dbo.VisitorInformation`, `dbo.VisitInfo`, `dbo.Vw_Visit`,
`dbo.VW_CustomerInformation`, `dbo.vw_customer`, `dbo.VW_ListCustomer`,
`dbo.VW_ListPishDaryaft`, `dbo.VwListPishfactorhayeTeadNashodeh`,
`dbo.VW_RowDetailsForosh`, `dbo.VWSailfact`, `dbo.CustactGetchk`,
`dbo.VW_UserAnbar`, `dbo.VW_UserPos`, `dbo.VW_CustomerControl`, `dbo.UsersTasks`.

Functions: `dbo.GetMainMasirID`, `dbo.Func_GetPathByMasirID`,
`dbo.Func_GetVisitorsByVisRdf`, `dbo.vis_name`, `dbo.vis_cell_supervisor`,
`dbo.cust_group_name`, `dbo.cust_vis_name`, `dbo.get_vis_rdf`,
`dbo.get_role_id`, `dbo.CalcDetailsPishfactor`, `dbo.CalcPriceDocument`,
`dbo.CalcPriceFromAtiranDocument`, `dbo.Func_TafifKhalesForosh`,
`dbo.mosh_sp_price`, `dbo.last_price_f_k_mosh`, `dbo.SetUsername`,
`dbo.SetUserpass`, `dbo.SetSystemName`, `dbo.which_panevis`,
`dbo.IsAccountingSystemStarted`, `dbo.GetMainMasirID`.

## 10b. Findings from audit part 5 (2026-09-18) — important corrections

### The char(1) boolean columns hold `'t'`, NOT `'1'`
Section G3 of the live output shows the real values:

```
G3|inventory.active|t|rows=50        G3|CUSTOMERS.active|t|rows=7
G3|forosh_price.active|t|rows=50     G3|visitors.active|t|rows=1
G3|kagroup.Active|0|rows=1           G3|kagroup.Active|1|rows=29
G3|anbars.Active|1|rows=1            G3|custgroup.Active|1|rows=9
```

So `char(1)` flags are `'t'`/`'f'` and `bit` flags are `1`/`0`. **A filter written
as `active = '1'` silently returns 0 rows** — this was a real bug in the first
version of the data source and is now fixed. The guard tool compares the declared
Kotlin constants against `docs/schema/meelano-values.tsv`, so it cannot recur.

### The user → visitor link is `sys_vis`, not `visitors.UserID`
`visitors.UserID` is **NULL** in the live row; the scope table carries the link:

```
G5|visitor|vis_rdf=1|name=ويزيتور سيستم|UserID=<null>|region=1|city=1
G5|sys_vis|SysID=1|shvis=1|UserID=1
G5|sys_cus|SysID=1|Shmo=1..6|UserID=1        (user 1 may serve customers 1..6)
G5|sys_cus|SysID=1|Shmo=7|UserID=2           (user 2 may serve customer 7)
```

Chain: `sys_users.user_id → sys_vis.UserID → sys_vis.shvis → visitors.vis_rdf`.

### Columns of the remaining scope/config tables (G1)

```
sys_kal: rdf, sysid, shka, UserID          sys_anb: rdf, SysID, shanb, UserID
sys_vis: rdf, SysID, shvis, UserID         sys_cus: rdf, SysID, Shmo, UserID
sys_use: rdf, SysID, shuse, UserID         sys_wor: rdf, SysID, shwor, UserID
systems: rdf, name, active                 AnbarDifferent: RowID, Shka, SanadNo, Kind,
                                           AnbarID, JozPrice, TedVahOld, TedJozOld,
                                           TedVahCounted, TedJozCounted, Active
NCustomers: RowID, NShmo, NMan, NDate, BlackList
```

### Live configuration data (small, first deployment)

* company/OSystem: `rdf=1`, name `مديريت`, warehouse 1, active
* visitor: `vis_rdf=1` «ويزيتور سيستم», active, region 1, city 1, no supervisor,
  commission percentages 0
* customer groups: 9 rows, **all with `price = 1`** → tier 1 for everybody today
  (`مشتريان`, `تامين کنندگان`, `ويزيتورها`, `مامورين پخش`, `مامورين مطالبات`,
  `راننده ها`, `پرسنل دفتري`, `كارگران`, `بنکداران`)
* customers: 7 rows — `ويزيتور سيستم` (group 3), `مشتری آنلاین`, `ایلیا پخش`,
  `بازرگانی موسوی مقدم`, `پخش درخشان`, `پخش بلدی`, `امین لیاقت`; balances (`man`)
  0 / 0 / 5.8M / 0 / 80M / 0 / 25M, `black_list = 0` for all
* route: 1 row (`مسير سيستم`)

### LOGIN IS NOT SOLVED YET — and here is exactly why

```
G4|sys_users|id=1|name=Admin|hash_bytes=1|active=1|pwdcompare_result=0
G4|sys_users|id=2|name=مدير |hash_bytes=1|active=1|pwdcompare_result=0
```

`user_password` is **1 byte long**, so it is not a SQL Server password hash and
`PWDCOMPARE` cannot be the mechanism. (The 0 result is also expected because the
probe placeholder was sent instead of a real password.) The app must therefore
not guess; `sql/06_login_probe.sql` collects the evidence: the classification of
that byte, the other candidate credential stores (`security.ConfirmUser`,
`EMS.user`, `dbo.sys_use`), and the real bodies of the ERP helpers
`SetUserpass` / `SetUsername` / `GetUser` / `getEmsUsername`.


## 13. LOGIN — SOLVED (evidence from the ERP's own code, audit part 6)

### What the ERP does

The stored value is **plain text inside a `varbinary` column**, not a hash:

```sql
-- dbo.SetUserpass   (function used by the ERP itself)
set @result = (select convert(varchar(50), user_password) from sys_users where user_id = @UserID)

-- dbo.ChangeUserPassInSalMali   (how the ERP WRITES a password)
set @sql = 'update ' + @NameDB + '.dbo.sys_users set user_password = CONVERT(varbinary, @PassWord) where user_name = @UserName'

-- dbo.AddUser: reads it the same way
set @UserPass = (select convert(varchar(50), user_password) from sys_users where user_id = @UserID)
```

Live data confirms it: `DATALENGTH(user_password) = 1` and its hex is `31`, i.e.
the single character `1` — for **both** users (`Admin`, `مدير`). A SQL Server
password hash would be 20/44/60 bytes and `PWDCOMPARE` returns 0, so hashing was
never involved.

### The app's login predicate (now implemented)

```sql
SELECT TOP (1) user_id, user_name, user_fname, user_lname, role_id, active, IsLocked, shmo
  FROM dbo.sys_users
 WHERE user_name = ?
   AND CONVERT(varchar(50), user_password) = ?
   AND active = 1
```

* `user_name` is compared with the database collation `..._CI_AS`
  (case-insensitive), exactly like the ERP's own `dbo.get_role_id(@user__)`.
* The password comparison mirrors how the ERP *reads* the value. If the ERP later
  turns out to be case-sensitive, the one-line switch is
  `sys_users.user_password = CONVERT(varbinary(50), ?)`.
* The password is only ever a JDBC parameter: never concatenated into SQL, never
  stored by the app, never logged, never echoed in an error message.
* `IsLocked` is returned so the UI can say "account locked" instead of silently
  refusing the login.

### Security consequences (stated plainly)

This ERP keeps application passwords in recoverable form, so **anyone with read
rights on the database can read them** — that is a property of the ERP, not of
this app, and it cannot be fixed from the Android side. Mitigations in the app: a
dedicated least-privilege SQL login (`vizitor_android`, read-only), passwords sent
only over the shop LAN, a TLS-encrypted JDBC connection preferred
(`useEncryption=true, trustServerCertificate=true` handles the self-signed
certificate), credentials encrypted with the Android Keystore on the device
(`SecureDbStore`), and a "clear configuration" action that wipes them.

### Other candidate stores, checked and excluded

`L6` scanned the whole database for password-like columns — there are only three:
`dbo.sys_users.user_password`, `dbo.CUSTOMERS.Password` (customer portal, not the
staff login) and `dbo.visitors.Password` (0 non-null rows). `L7` listed the other
auth-shaped objects: `dbo.Create_Login`, `dbo.ChangePassword`,
`dbo.dt_validateloginparams(_u)`, `security.ConfirmUser`, `security.LoginDetails`,
`dbo.SettingForPopUpWhenLogin` — none is used by the app's login path;
`sql/07_login_verify.sql` re-checks `security.ConfirmUser`/`LoginDetails` and the
ERP helpers for completeness.

### Fiscal-year databases

`dbo.ChangeUserPassInSalMali` iterates `select nam_db from dbo.sal_mali` and
updates `sys_users` in **each** fiscal-year database, so this ERP can span several
databases. The app targets the current one (`Meelano`);
`sql/07_login_verify.sql` lists what `dbo.sal_mali` contains, so we know whether a
fiscal-year switch has to be handled later.

### 13.1 Live proof — `sql/07_login_verify.sql` v2 run on the server (2026-09-18)

The operator ran the whole file (8 of 8 output lines) and this is the raw result:

```
V1|MATCH|user_id=1|user=Admin|fullname=آتيران آتيران نوين|role_id=1|active=1|IsLocked=nu|shmo=1
V1b|control|rows_with_that_user_name=1|active_1_rows=1
V1c|PWDCOMPARE_result=0 (expected 0 - this ERP does not use SQL Server password hashing)
V2|wrong_password_rows=0 (must be 0, otherwise the comparison is broken)
V3|security.ConfirmUser|COLS|RowID,RealUserID,RealUserName,FakeUserID,FakeUserName,P,SumMab,
   SumPos,SumCheck,Shmo,Moname,RealPlusFake,Ghno|rows=1
V3|security.LoginDetails|COLS|RowID,UserID,DateClient,DateServer,ComputerIP,ComputerName|rows=16
V4|sal_mali|COLS|sal_maliID,rdf,name,nam_db,StartDate,EndDate,Current
V4|sal_mali|nam_db=Meelano|rows=1|overal_setting_rows=352|IsAccountingSystemStarted=0
```

Conclusions:

* **The app's login predicate is proven against the real database**:
  `user_name = ? AND CONVERT(varchar(50), user_password) = ? AND active = 1` on
  `dbo.sys_users` returns the Admin row (V1) and rejects a wrong password (V2 = 0).
  Live proof supersedes the earlier `PWDCOMPARE` idea; V1c confirms that hashing
  is not in use (result 0 for the *correct* password).
* `IsLocked=nu` was **our own display bug, not a schema surprise**: the expression
  was `ISNULL(CAST(u.IsLocked AS NVARCHAR(2)), N'null')` and `ISNULL` returns the
  type of its *first* argument, so the replacement literal `null` was truncated to
  `nu`. `sys_users.IsLocked` is `bit NULL` and its value is **NULL** for both users.
  Consequence for the app: locking is only signalled by `IsLocked = 1`; 0 and NULL
  both mean "not locked", and the ERP's own login does not filter on it either.
  `MeelanoDataSource.login()` now returns a non-NULL `is_locked` flag
  (`CASE WHEN IsLocked = 1 THEN 1 ELSE 0 END`) so the app never has to interpret a
  NULL. `tools/verify_tsql.py` gained a check that fails this truncation pattern
  (`02`, `05`, `06`, `07` were fixed by widening the CAST).
* `security.ConfirmUser` is the ERP's **impersonation table** ("login as another
  user": RealUserID/FakeUserID + a `P` column of unknown semantics). 1 row. The app
  does **not** read it, does not use it for login, and never reads/stores `P`.
* `security.LoginDetails` is the ERP's **login history** (16 rows: UserID, client
  and server timestamps, ComputerIP, ComputerName). It holds no secrets; the app
  does not write to it (that would need write access the read-only SQL user must
  not have). Recording app logins there is an option for a later phase, to be
  decided by the ERP owner.
* `dbo.sal_mali` has exactly **1 row** (`nam_db = Meelano`) → a single fiscal-year
  database today, so no fiscal-year switch is needed for the app. The columns are
  `sal_maliID, rdf, name, nam_db, StartDate, EndDate, Current`; `name` (the fiscal
  year label) was not printed and is only needed if the app ever shows/reports it.
* `dbo.IsAccountingSystemStarted()` returns **0** (`overal_setting` has 352 rows;
  the flag is row id 67). The accounting system is therefore not "started" on this
  server yet. Whether the write path (`add_sail_pish` / `AddInvoice`) refuses to
  post while it is 0 must be checked against those procedure bodies — this is one
  of the acceptance tests for the pre-invoice phase.

## 10. Still open (filled by scripts 02 and 03)

* ~~real TCP port~~ → **RESOLVED: 1433** (`sys.dm_tcp_listener_states` shows
  `0.0.0.0:1433` + `[::]:1433`, state ONLINE, type TSQL). The `1434` seen in the
  registry belongs to the DAC/admin connection (`AdminConnection\Tcp`), not to the
  application endpoint. The app therefore connects to `192.168.1.150:1433`.
* columns of `sys_kal` / `sys_anb`
* whether `PWDCOMPARE` accepts the stored `sys_users.user_password` hashes
* content of the tiny configuration tables (`osystems`, `Roles`, `visitors`,
  `sys_vis`, `sys_cus`, `sys_kal`, `sys_anb`, `custgroup`, `anbars`)
* full bodies of `add_sail_pish`, `AddInvoice`, `new_cust`, `FixMojodi`
  (they define the exact pre-invoice/invoice transaction flow the app must
  follow), and the short bodies of `SetUserpass` / `GetUser` / `get_vis_rdf`


## 11. Evidence about the write path (from the partial part-3 output)

Only the 4 `LENGTH=` header lines and the last chunk of `dbo.new_cust` came back
(the SSMS Messages tab drops long output), but that last chunk is informative:

```
P|new_cust|20|,@a
   end
   exec FixManCustomer @a
commit transaction t1
end
```

So `dbo.new_cust` **opens its own transaction (`t1`), calls `dbo.FixManCustomer`
inside it and commits** — the app must not wrap it in another transaction, and
must treat the returned `@id_en` as the new customer number. Full bodies are
still needed; `tools/run_audit.bat` writes them to a file without truncation.

## 12. Environments observed while auditing (useful for troubleshooting)

* Messages-tab output is silently dropped past a few thousand characters on this
  SSMS installation → long dumps must go to a file (`sqlcmd -o`, or
  `tools/run_audit.bat`, or SSMS Ctrl+Shift+F "Results to File").

## 14. Findings from audit part 2 — `02_fill_gaps.sql` (run 2026-09-18)

The operator ran part 2 through `sqlcmd` (13 output pages; the page header lines
identify the login as `MIGHTY\MeeLano-Pc`, i.e. Windows authentication works).

### 14.1 Network / TCP — registry section A

* `…\SuperSocketNetLib\Tcp\IPAll`: `TcpPort = 1433`, `TcpDynamicPorts` empty,
  `ListenOnAllIPs = 1` → SQL Server listens on **every** local address at 1433,
  which matches `sys.dm_tcp_listener_states` (`0.0.0.0:1433`, `[::]:1433`).
* `…\Tcp\IP1…IP5` are all `Enabled = 0` (so IPAll governs) and carry the machine
  addresses that were configured the last time TCP/IP properties were edited:
  `IP1 = 192.168.1.110` (the only IPv4), `IP2 = fe80::6a5d:7287:a5ec:3132%26`,
  `IP3 = 169.254.151.74` (APIPA), `IP4 = ::1`, `IP5 = 127.0.0.1`.
* `…\AdminConnection\Tcp\TcpDynamicPorts = 1434` → the DAC listener, as established.
* ⚠️ **Conflict with the project brief**: the brief says the server's LAN address is
  `192.168.1.150`, the server's own registry says `192.168.1.110`. One of the two is
  wrong, and the Android app cannot connect until we know which. The authoritative
  answer is what the server reports for itself over TCP:

```sql
SELECT local_net_address, local_tcp_port
  FROM sys.dm_exec_connections WHERE session_id = @@SPID;
```

  *Pending:* operator to run this (it is part of the `out_00_quick.txt` quick check
  of `tools/run_audit.bat`). Until it is answered, the address stays editable in the
  app's Settings screen and no default is treated as verified.

### 14.2 Roles, authentication helpers, visitor limits — section B

* `dbo.Roles` = 6 rows, all real: `1 مدير`, `2 مدير فروش`, `3 مدير حسابداري`,
  `4 حسابدار`, **`5 ويزيتور`**, `6 كاربر`. So the visitor role id in this ERP is 5.
* `sys_users` again: `id=1 Admin`, `id=2 مدير`, both `active=1`, `role_id=1`,
  `shmo=1`, `pw_bytes=1` (plain text in `varbinary`), `pwdcompare('x')=0`.
* `visitors` row 1: `active='t'`, `UserID=<null>`, `has_password=no`
  → confirms the app must resolve the visitor through `sys_vis`
  (`sys_users.user_id → sys_vis.UserID → shvis → visitors.vis_rdf`); the ERP's own
  helper `get_vis_rdf` (below) would return -1 for this installation.
* **Visitor sales limits are 0**: `TedadFactorMojazMande = 0` and
  `MablaghMojazMandeJahatFactorha = 0`. What 0 means for the app (unlimited vs.
  nothing allowed) can only be decided from the ERP's own write logic, so this is
  now an explicit acceptance question for the pre-invoice phase.
* Function bodies read verbatim (all small): `get_role_id` (`select role_id from
  sys_users where user_name = @user__`), `get_vis_rdf` (`select top 1 vis_rdf from
  visitors where UserID = @user__`), `SetUsername`, `SetUserpass`
  (`convert(varchar(50), user_password)`), `SetSystemName` (`osystems.name_System`),
  `vis_name` (`visitors.vis_name where vis_rdf = @shmo and active = 't'`),
  `cust_group_name` → `custgroup.group_name` via
  `customers.group_rdf`, `which_panevis`, `getEmsUsername`, `EMS.GetUser`,
  and `IsAccountingSystemStarted` = `select value from dbo.overal_setting where id = 67`.

### 14.3 Views that exist on the live server — section D (definitions verbatim)

These matter because the app must reuse the ERP's own formulas instead of inventing
its own:

| View | Chars | Why it matters to Vizitor |
|---|---|---|
| `VW_InventoryAnbars` | 3055 | **Authoritative sellable stock**: `(inventory_anbars.mojkavah × inventory.mohvah + mojkajoz) − Σ(TEDVAH × mohvah + TEDJOZ)` over `subsailfact_pish ⋈ sailfact_pish` where `Rejected = 0 and active = 't' and sh_f = 0`. Use it for availability. |
| `VW_CustomerInformation` | 1516 | Per-customer: overdue counts (`Moavagh1/2`), `TedFactorGhabli`, `RemainCredit`, `ForoshType` (= `custgroup.price`, the price tier!), returned cheques. |
| `VisitorInformation` | 1706 | Visitor KPIs: `ManCustomers`, `MabCheck`, `TedFactorErsali`, `TedFactor`, `MabFactorErsali` → the app's Profile/visitor screen. |
| `VW_GoalsVisitors` | 1117 | Visit goals (`vis_goals` is empty) with group/route names. |
| `VW_ListCustomer`, `vw_customer` | 853 / 3246 | Customer lists incl. last invoice (`sailfact_3`), last receipt (`dar_3`), route/city/region names. |
| `subsailFactPish`, `pishfactor_body`, `pishfactors`, `SailFactPish_Details`, `VwListPishfactorhayeTeadNashodeh` | 364 … 1531 | The pre-invoice tables as the ERP sees them: header (`sailfact_pish`: `shfacfo, USER__, date, sh_f, vis_rdf, [all], ted_rooz, shmo, shfacthand, Rejected, TaedHesabdari, TaedForush, sysid, active`) and lines (`subsailfact_pish`: `shfacfo, SHK, rdf_anbar, TEDVAH, TEDJOZ, VAHPRICE, JOZPRICE, LINESUM, litakhma, PERTAFIF, PERVIS, Tax, Avarez, Ptax, Pavarez, RDF`). |
| `VisitInfo`, `Vw_Visit` | 1594 / 3121 | Visit tracking (`Hamrah.Visit` ↔ `sailfact_pish.VisitID`), duration and order counts — the app's visit feature. |
| `VW_RowDetailsForosh` | 308 | Sold lines of final invoices (`active='t'`). |

### 14.4 Procedure inventory — section E (name + size)

`add_sail_pish 3118`, `AddInvoice 5850`, `new_cust 3868`, `FixMojodi 7957`,
`Edit_sail_pish 3091` (**edit pre-invoice**), `newcust 3927` + `t_newcust 5875`,
`ListPishFactor 8555`, `back_sail 5046` (invoice reversal), `set_vis_koli 2099`,
`VisitorSalesCommision 984`, `GetVisitorPoints 5160`,
`SelectPriceAndTedvahForushVisitorhaByDate 1066` (**price + quantity per visitor per
date** — likely the ERP's own pricing entry point), `FixInventoryPrice 5473`,
`UpdateMojodiInventoryAnbars 2718`, `ted_moghayerat_anbar 720`,
`raf_moghayerat_anbar 1500`, `ProcInsertIntoSysKal 903`, `change_price 762`,
`close_open_cust 607`, `fill_cust_more_info 896`, `CustomerListToDate 242`,
`AddFromAtiranDetailsForVisitors 843`, `AddUser 591`.

The four bodies the app's write path needs (`add_sail_pish`, `AddInvoice`,
`new_cust`, `FixMojodi`) are **still the missing piece** — parts 3/3b did not run
in the operator's folder; `tools/run_audit.bat` now dumps them by itself even when
no `.sql` file is present.

### 14.5 Sample data seen (section C)

* `forosh_price`: 5 rows; `forosh1` = 50000000, 100000000, 0, 0, 0 → the higher
  tiers are zero, so a customer whose group tier is > 1 would get 0.00 prices.
  Correct tier resolution + a defined fallback is therefore mandatory (as the brief says).
* `inventory_anbars`: 15 rows for warehouse 1 (`mojkavah = 20`, `mojkajoz = 0`).
* `kagroup`: 10 groups (سختافزاری قطعات، نرمافزار، آموزش، خدمات، قطعات، شبكه، آتیران، هارد، كیس) with `ParentGroupRdf`/`GroupLevel`.
* `sailfact`: 3 invoices (1402/02/05، 1405/06/09، 1405/06/15) — note the **1405**
  dates, i.e. the ERP is in use in the current Jalali year.
* `cust_act`: 3 opening-balance rows.
* ⚠️ Reading note: in section C the column list and the row values are both built by
  string concatenation over `sys.columns`, and SQL Server does **not** guarantee the
  `ORDER BY` of a concatenation subquery — the values of a row can therefore be
  printed in a different order than the `COLS|` list. Only explicitly labelled
  queries (sections A, B, E and the `COLS|` lists themselves) are authoritative for
  column↔value mapping; the row samples are evidence of *content*, not of order.

## 15. The write path — bodies received (audit part 3, 2026-09-18)

`add_sail_pish` (3118), `AddInvoice` (5850), `new_cust` (3868) and `FixMojodi` (7957)
came back complete, together with the small helpers. They are written out, step by
step, in **`docs/write-path/ERP-WRITE-PROCEDURES.md`** — the parameter lists, INSERT
column lists and value lists are verbatim.

The conclusions the app must live by:

* `add_sail_pish` inserts exactly one `sailfact_pish` row, numbers it as
  `max(shfacfo)+1`, sets `rdf__=1`, `active='t'`, `sh_f=0`, `rejected=0`,
  `man_gh = customers.man`, and returns the number in `@id_en`. It only runs when
  `@mod = 1`. It never touches `subsailfact_pish`.
* `AddInvoice` creates the `sailfact` row (`rdf__=1`, `active='t'`, `ismodify='f'`,
  `tasvieh='f'`, `t_date = what_date(@date,@modpar)`, all `*_fel` columns =
  `round(x,0,1)`), then `update sailfact_pish set sh_f, user_f, date_f` (the
  pre-invoice is "invoiced" exactly by `sh_f <> 0`), then optionally `Addmaliyat`,
  then `cust_act` with `act_id = 20`, then `FixManCustomer @shmo`. It never touches
  `subsailfact`.
* `new_cust` owns `transaction t1` (`xact_abort on`), rejects duplicate names with
  `raiserror('نام تكراري است',16,1)`, writes `cus_image`, a `cust_act` opening row,
  `sys_cus (1, @a, 1)`, optionally `AssignTafsilCodeToEntity`, then `FixManCustomer`,
  and returns the new SHMO in `@id_en`. **Never call it inside another transaction.**
* `FixMojodi @Shfac, @state` must be used for stock: state 1 for a real sales invoice
  (cursor over `subsailfact` → `UpdateMojodiInventory*`), state 5/6 for store sales.
  Its inline formula (`mojkavah/mojkajoz` with `mohvah`, `floor` + `%`) is the ERP's
  own; the app must not invent a different one.
* `overal_setting` ids seen in the code: 67 accounting-started, 77/78 auto-confirm the
  pre-invoice after insert, 97 days to shift an invoice date forward when it comes
  from a pre-invoice, 135 `'Ex'`, 168 include the customer name in the ledger text.

Still needed (requested): `Edit_sail_pish` (the place where the ERP writes the
`subsailfact_pish` lines), `AddFromAtiranDetailsForVisitors`,
`SelectPriceAndTedvahForushVisitorhaByDate`, and the column names of
`VW_InventoryAnbars` (its definition arrived truncated).

### 15.1 Other facts fixed by parts 5 and 6

* `security.ConfirmUser.P` is **bit** (audit part 6, L3) — not a password, so nothing
  secret lives in that table. `EMS.user` exists with 9 columns and 1 row.
* Column **types** for `AnbarDifferent`, `NCustomers`, `systems`, `sys_vis`,
  `sys_cus`, `sys_kal`, `sys_anb`, `sys_use`, `sys_wor`, `security.ConfirmUser`,
  `security.LoginDetails` and `EMS.user` are now recorded in
  `docs/schema/meelano-types.tsv`, and the tables themselves were added to
  `docs/schema/meelano-columns.tsv` (30 tables / 504 columns).
* `kagroup.Active`: 29 rows with 1, 1 row with 0 (bit) — the guard already knew this.
* **All nine `custgroup` rows have `price` (tier) = 1**, and customers 2…7 sit in
  group 1 while customer 1 sits in group 3 → in today's data every customer gets
  `forosh_price.forosh1`. The app still resolves the tier dynamically (the brief
  forbids hard-coded prices) and needs the ERP's fallback rule for a tier whose
  `forosh<n>` is 0 — that is what `SelectPriceAndTedvahForushVisitorhaByDate` should
  settle.
* `visitors` row 1: `supervisor = 'f'`, `per_p_d_naghd = 0.000`, `per_p_d_check = 0.000`;
  its sales limits are both 0.

## 16. The remaining objects read (audit part 7, 2026-09-18)

Bodies received verbatim (raw paste: `docs/audit-runs/out_11_helper_bodies.txt`,
walk-through in `docs/write-path/ERP-WRITE-PROCEDURES.md` §5–§10):

| object | chars | what it proves |
|---|---|---|
| `Edit_sail_pish` | 3134 | **`sailfact_pish.rdf__` is a version counter.** The "edit" retires the old head (`active='f'`, `ismodify='t'`), retires **all** its lines (`update subsailfact_pish set active='f'`), takes `MAX(rdf__)+1` and inserts a new head with that version, `active='t'`, `sh_f=0`, `VisitID` carried over, plus `Promotion`. It inserts **no lines** — the line-writing statement is still unfound. |
| `FixManCustomer` | 599 | `CUSTOMERS.man = SUM(cust_act.act_bed) - SUM(cust_act.act_bes)` where `isActive <> 0 or isActive is null`; then `FixTasvie` when `overal_setting.id = 117`; then `Fix_Sys_Mandeh_Customer`. Owns `transaction a` + `xact_abort on`. |
| `UpdateMojodiInventory` | 1320 | Stock is rebuilt from the goods ledger: `SUM((tedvah*mohvah + tedjoz) * CASE WHEN act_id IN (20,22,5,19,18,48,26,85,133) THEN -1 ELSE 1 END) FROM ka_act WHERE active='t'`, `tedbastebandi` rebuilt when `overal_setting.id = 8 = 1`, then split into boxes/pieces with `floor` / `%` incl. the negative branch. |
| `UpdateMojodiInventoryAnbars` | 1792 | The same, per warehouse: cursor over `inventory_anbars.rdf_anbars` of that product, `ka_act.RdfAnbar` filter. |
| `UpdateMojodiInventoryAnbarsPS` | 1773 | The same for production series, only when `inventory.WithProductionSerial = 1`, writing `Inventory_Anbars_PS` on `(shka, rdfAnbar, PSId)`. |
| `VW_InventoryAnbars` | 3055 | Columns `shka, rdf_anbars, name, tedbastebandi, mojkavah, mojkajoz, MojodiPish_vah, MojodiPish_joz`; base `inventory_anbars ⋈ inventory where inventory.active='t'`. `MojodiPish_*` = `(mojkavah*mohvah + mojkajoz) − ISNULL(SUM(TEDVAH)*mohvah + SUM(TEDJOZ),0)` taken from `subsailfact_pish ⋈ sailfact_pish` on **`shfacfo` and `rdf__`**, `Rejected=0 and active='t' and sh_f=0`, matched on `(shka, rdf_anbar)`, then split with the same `floor`/`%` rule. |
| `AddFromAtiranDetailsForVisitors` | 955 | Ledger split: calls `AddToFromAtiranDetails`, then when `visitors.supervisor_rdf <> 0` scales `@bed/@bes` by `visitors.supervisor_per`, resolves the supervisor's ledger via `CUSTOMERS.TafsilID where Ecode_Vis = @super and kind <> 8`, and recurses. Not a line writer. |
| `SelectPriceAndTedvahForushVisitorhaByDate` | 1066 | The ERP's own per-visitor sales report over `VWForushKhales ⋈ inventory ⋈ kagroup ⋈ visitors ⋈ masir ⋈ CUSTOMERS ⋈ custgroup` (child groups via `kagroup.ParentGroupRdf`), filtered by `CUSTOMERS.RDF_masir/group_rdf`, grouped per visitor + path. A report, not the price fallback. |

New objects/columns this exposed: table **`ka_act`** (`shka, act_id, tedvah, tedjoz,
tedbastebandi, active, RdfAnbar, ProductionSeriesID`) and table
**`Inventory_Anbars_PS`**, view **`VWForushKhales`**, and the columns
`visitors.supervisor_rdf`, `visitors.supervisor_per`, `kagroup.ParentGroupRdf`,
`masir.rdf_masir`, `cust_act.isActive`, `sailfact_pish.Promotion` (now in
`docs/schema/meelano-columns.tsv`, together with `dbo.VW_InventoryAnbars`).

### 16.1 Which database? (answered for this instance)

`SELECT … MAX(last_user_update) FROM sys.dm_db_index_usage_stats` grouped by database
returned **one row: `Meelano`, last_write = NULL** — i.e. on the instance the operator
is connected to, `Meelano` is the **only** non-system database, and nothing has been
written since the last SQL Server restart (the stats reset on restart, hence NULL;
the ERP screens have not saved anything in this session). The `Atiran14050603` lists
of part 6 therefore came from **another instance or another server** — still to be
identified, but not a database of the server the app will talk to.

### 16.2 Consequence for the app

`stock()` now reads `dbo.VW_InventoryAnbars` instead of the raw `inventory_anbars`, so
the app shows what the ERP shows — and it gets both numbers: on-hand
(`mojkavah`/`mojkajoz`) and sellable after open pre-invoices
(`MojodiPish_vah`/`MojodiPish_joz`). The ERP's own view also drops inactive products
(`inventory.active = 't'`), which the raw query did not.

## 17. The write path is now closed (part 9, 2026-09-18)

Full detail: `docs/write-path/PRE-INVOICE-LINES.md` §7-§8 and
`docs/write-path/ERP-WRITE-PROCEDURES.md` §12. Raw paste:
`docs/audit-runs/out_13_trigger_bodies.txt`.

* The ERP stages invoice lines in **`dbo.subsailtemp`** and pre-invoice lines in
  **`dbo.subsailtemp_pish`**; both staging tables carry an **enabled INSTEAD OF INSERT**
  trigger (`InvoiceTrigger`, `trig_sst_pish`). Nothing is ever stored in the staging
  tables - the triggers copy the row into `dbo.subsailfact` / `dbo.subsailfact_pish`.
* `trig_sst_pish` is therefore the answer to "who writes `subsailfact_pish`". A `mod`
  column selects the branch: `mod = 1` stamps the line with the **live head's** `rdf__`
  (`max(rdf__) from sailfact_pish where shfacfo = @shfacfo`) and forces `active = 't'`;
  `mod = 0` copies `rdf__` and `active` from the staged row.
* `InvoiceTrigger` also writes the **inventory ledger** `dbo.ka_act` (`act_id = 20`,
  `ghno = shfacfo`, `gain = dbo.cal_gain(...)`, `invepgh` = `ProductionSeries.PriceEnd`
  or `inventory.inventory_price`) - the only place we have ever seen a sale reach
  `ka_act`, i.e. the only path that would move stock.
* `ListPishFactor` (the ERP's pre-invoice list) and three views
  (`pishfactor_body`, `subsailFactPish`, `VW_Taraz_pish`) confirm the line-to-head link
  `(shfacfo, rdf__)` - `pishfactor_body` uses it, the other two join on `shfacfo` alone
  and would repeat the lines of an edited pre-invoice.
* Both writer procedures (`add_sail_pish`, `Edit_sail_pish`) and the trigger own their
  own transactions (`forosh` / `Invoice`), so the app's save is three sequential units,
  never one outer transaction.
* New objects seen: `subsailtemp_pish`, `ka_act`, `masir`, `Quarter`, `regions`, `CITYS`,
  `anbars`, `produce.ProductionSeries`, `sailfact.shpish`, `dbo.what_date`,
  `dbo.cal_gain`, `dbo.dif_date`, `inventory.vahwe`.
* Open: types/columns of `subsailtemp_pish`, one real example of the ERP's numbers,
  and whether `Meelano` itself has `PerPromotion` on `subsailfact_pish`.

## 18. Which database the app talks to - the open decision (2026-09-18)

`راه ۱۰` was run twice by the operator, but both runs came back with `db =
Atiran14050603` (the SSMS toolbar was never changed). What that did prove:

* on `Atiran14050603` **every** object the write path needs exists -
  `subsailtemp_pish`, `subsailfact_pish`, `trig_sst_pish`, `InvoiceTrigger`,
  `subsailtemp`, `add_sail_pish`, `AddInvoice`, `Edit_sail_pish` (all OBJECT_IDs
  non-NULL);
* its trade is live: 1219 customers, 2197 inventory rows, 243 invoices (`sailfact`),
  1696 invoice lines (`subsailfact`), 4 users, 1 visitor;
* `sailfact_pish` and `subsailtemp_pish` hold **0 rows** - the ERP has never written a
  pre-invoice in this database, so the app will be its first writer;
* `Meelano` remains the database the parts 1-6 audit walked (7 customers, 50 inventory
  rows, 3 invoices) and where the part 7 procedure bodies came from - its line tables
  and triggers are still unverified.

The comparison script that settles it without touching the toolbar is
`sql/09_compare_databases.sql` ("راه ۱۱"): it walks `sys.databases` by itself and prints
per database the objects found, the row counts of the key tables and each database's own
`dbo.sal_mali` (`name`, `nam_db`, `Current`).

Until the operator decides, the Android layer keeps the database name in Settings and
the connection layer warns when `dbo.sal_mali.nam_db` differs from the configured name
instead of silently reading a stale database.
