# Atiran Schema — Extracted (evidence-based)

Source: `AtiranLocalServices.dll` (SACModel SSDL) + `DataAccess/*.cs` entity classes,
extracted from `AtiranHamrah.zip` in the `Companymeelano/atiran` repository.

⚠️ This is the schema of the **sac/Atiran2** data model as known from the codebase.
Tables the Vizitor app additionally needs (`sys_users`, `Roles`, `new_cust`, `ka_act`)
are NOT part of this model — they must be verified on the real server via
`sql/00_audit_atiran2.sql` before relying on them.

## BANK
- Type: Tables | PK: RDF | Columns: 20
- FK refs: FK_BANK_BANK_NAME: BANK.BANK→BANK; FK_PishDaryaftPos_BANK: BANK.BANK→BANK

| Column | SQL Type | Nullable |
|---|---|---|
| RDF | int |  |
| START_DATE | char |  |
| OWNER | nvarchar |  |
| BANKNAME | nvarchar |  |
| SHOBE | nvarchar |  |
| ADDRE | nvarchar |  |
| TELL1 | nvarchar |  |
| TELL2 | nvarchar |  |
| SHHE | nvarchar |  |
| MAN | money |  |
| TafsilCode | nchar |  |
| AccountType | int |  |
| BranchCode | int |  |
| TafsilID | bigint |  |
| Active | bit |  |
| BankRdf | int |  |
| IsPos | bit |  |
| IsCard | bit |  |
| UserID | int |  |
| BankTransferCode | nvarchar |  |

## BANK_NAME
- Type: Tables | PK: RDF | Columns: 3
- FK refs: FK_BANK_BANK_NAME: BANK_NAME.BANK_NAME→BANK_NAME

| Column | SQL Type | Nullable |
|---|---|---|
| RDF | int |  |
| NAMES | varchar |  |
| Active | bit |  |

## CITYS
- Type: Tables | PK: RDF | Columns: 7
- FK refs: FK_CITYS_Province: CITYS.CITYS→CITYS; FK_regions_CITYS: CITYS.CITYS→CITYS

| Column | SQL Type | Nullable |
|---|---|---|
| RDF | int |  |
| name | varchar |  |
| ProvinceID | int |  |
| Lat | float |  |
| Lng | float |  |
| TemoCityID | int |  |
| CodeTTMS | nvarchar |  |

## CUSTOMERS
- Type: Tables | PK: SHMO | Columns: 59
- FK refs: FK_CUSTOMERS_custgroup: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_CUSTOMERS_masir: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_PishDaryaft_CUSTOMERS: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_Visit_CUSTOMERS: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_cust_act_CUSTOMERS: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_sailfact_CUSTOMERS: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_sailfact_pish_CUSTOMERS: CUSTOMERS.CUSTOMERS→CUSTOMERS; FK_sys_cus_CUSTOMERS: CUSTOMERS.CUSTOMERS→CUSTOMERS; TabletCustomer_CUSTOMERS_SHMO_fk: CUSTOMERS.CUSTOMERS→CUSTOMERS

| Column | SQL Type | Nullable |
|---|---|---|
| SHMO | int |  |
| MONAME | nvarchar |  |
| code | nvarchar |  |
| SHHES | nvarchar |  |
| BANKNAME | nvarchar |  |
| bankshobe | nvarchar |  |
| addre | nvarchar |  |
| tell1 | nvarchar |  |
| tell2 | nvarchar |  |
| cell | nvarchar |  |
| active | char |  |
| cred | money |  |
| man | money |  |
| peygham1 | nvarchar |  |
| special | char |  |
| rdf_city | int |  |
| rdf_region | int |  |
| group_rdf | int |  |
| date | char |  |
| sh_i_m | int |  |
| sharh | nvarchar |  |
| vis_rdf | int |  |
| user_d | nvarchar |  |
| shomare_masir | int |  |
| defi_vis | int |  |
| hesab_status | int |  |
| maxopen_time | char |  |
| check_eteb | int |  |
| just_naghdi | int |  |
| black_list | int |  |
| result_m | nvarchar |  |
| c_egh | nvarchar |  |
| c_mel | nvarchar |  |
| c_pos | nvarchar |  |
| kind | int |  |
| IsEmp | int |  |
| MaxManFactor | int |  |
| RDF_masir | int |  |
| Lat | float |  |
| Lng | float |  |
| TafsilCode | nchar |  |
| Ecode_Vis | int |  |
| PersonalityType | int |  |
| EtehadieID | int |  |
| Shenaseh_Egh | nvarchar |  |
| TafsilID | bigint |  |
| Username | nvarchar |  |
| Password | nvarchar |  |
| PriceCheck | decimal |  |
| CheckDateDay | int |  |
| ShmoMoaref | int |  |
| RoleCode | nvarchar |  |
| TransferCode | nvarchar |  |
| CustomerTypeTtmsId | bigint |  |
| CustomerBranch | nvarchar |  |
| TaxInvoiceType | int |  |
| WithTax | bit |  |
| FatherName | nvarchar |  |
| DateOfBirth | nvarchar |  |

## Company
- Type: Tables | PK: rdf | Columns: 18

| Column | SQL Type | Nullable |
|---|---|---|
| name | varchar |  |
| modir_amel | varchar |  |
| rdf | int |  |
| current_ | char |  |
| addre | text |  |
| tell1 | varchar |  |
| fax | varchar |  |
| cell | varchar |  |
| tell2 | varchar |  |
| arm | image |  |
| date__ | char |  |
| t_kind | int |  |
| C_meli | varchar |  |
| C_egh | varchar |  |
| C_pos | varchar |  |
| foroshgahi_active | int |  |
| TaxMemoryID | nvarchar |  |
| Branch | nvarchar |  |

## Device
- Type: Tables | PK: DeviceID | Columns: 10
- FK refs: FK_DeviceLocation_Device: Device.Device→Device; FK_visitors_Devices: Device.Device→Device

| Column | SQL Type | Nullable |
|---|---|---|
| DeviceID | int |  |
| ProductKey | nvarchar |  |
| CPUID | nvarchar |  |
| Status | int |  |
| ActivationDate | nchar |  |
| ExpirationDate | nchar |  |
| SysID | int |  |
| DeviceType | int |  |
| DeviceName | nvarchar |  |
| AppID | nvarchar |  |

## DeviceLocation
- Type: Tables | PK: ID | Columns: 7
- FK refs: FK_DeviceLocation_Device: DeviceLocation.DeviceLocation→DeviceLocation

| Column | SQL Type | Nullable |
|---|---|---|
| ID | bigint |  |
| DeviceID | int |  |
| DateAndTime | bigint |  |
| Latitude | float |  |
| Longitude | float |  |
| Angle | smallint |  |
| Speed | float |  |

## DeviceMessages
- Type: Tables | PK: RowID | Columns: 8
- FK refs: FK_DeviceMessages_visitors: DeviceMessages.DeviceMessages→DeviceMessages

| Column | SQL Type | Nullable |
|---|---|---|
| RowID | bigint |  |
| VisID | int |  |
| ReadTime | datetime |  |
| ReadDate | nvarchar |  |
| SendTime | datetime |  |
| SendDate | nvarchar |  |
| Title | nvarchar |  |
| ForcedUpdate | bit |  |

## DeviceSettings
- Type: Tables | PK: ID | Columns: 28
- FK refs: DeviceSettings_visitors_vis_rdf_fk: DeviceSettings.DeviceSettings→DeviceSettings

| Column | SQL Type | Nullable |
|---|---|---|
| ID | int |  |
| VisitorId | int |  |
| DefaultPriceGrp | int |  |
| VisitRangeLimit | int |  |
| MojoodiType | int |  |
| PrintCount | int |  |
| ActsLimit | int |  |
| TrackingTimeDiff | int |  |
| TrackingDisplacementDiff | int |  |
| NewVis4NoLoc | bit |  |
| VisitHasLocation | bit |  |
| DiscountApply | bit |  |
| JozChange | bit |  |
| SendLoc | bit |  |
| VisitorSeeIP | bit |  |
| HasAccessPishDaryaft | bit |  |
| HasAccessPishFactor | bit |  |
| HasAccessRahyab | bit |  |
| PriceGrpType | int |  |
| RoozMasir | bit |  |
| MaxAllowedSyncDays | int |  |
| SelectByMasir | bit |  |
| VisitSendTimeLimit | int |  |
| CheckingBouncedCheck | bit |  |
| InventoryCheck | int |  |
| AutoPrize | bit |  |
| IsTaxActive | bit |  |
| AccessToTheVisitorGoalsReport | bit |  |

## LimitedServices
- Type: Tables | PK: id | Columns: 3

| Column | SQL Type | Nullable |
|---|---|---|
| id | int |  |
| methodName | varchar |  |
| isLimit | bit |  |

## MasirDay
- Type: Tables | PK: ID | Columns: 6

| Column | SQL Type | Nullable |
|---|---|---|
| ID | int |  |
| Date_ | varchar |  |
| VisRdf | int |  |
| CityRdf | int |  |
| RegionRdf | int |  |
| MasirRdf | int |  |

## PishDaryaft
- Type: Tables | PK: GhnoPishDaryaft | Columns: 20
- FK refs: FK_PishDaryaftGetCheck_PishDaryaft: PishDaryaft.PishDaryaft→PishDaryaft; FK_PishDaryaftMultiFactor_PishDaryaft: PishDaryaft.PishDaryaft→PishDaryaft; FK_PishDaryaftPos_PishDaryaft: PishDaryaft.PishDaryaft→PishDaryaft; FK_PishDaryaft_CUSTOMERS: PishDaryaft.PishDaryaft→PishDaryaft; FK_PishDaryaft_osystems: PishDaryaft.PishDaryaft→PishDaryaft; FK_PishDaryaft_visitors: PishDaryaft.PishDaryaft→PishDaryaft

| Column | SQL Type | Nullable |
|---|---|---|
| GhnoPishDaryaft | int |  |
| Naghd | decimal |  |
| Date | nvarchar |  |
| Comment | nvarchar |  |
| Shmo | int |  |
| DateRecive | nvarchar |  |
| Shfac | int |  |
| VisitorID | int |  |
| SysID | int |  |
| ClockTime | nvarchar |  |
| UserTaeedHesabdari | int |  |
| DateTaeedHesabdari | nvarchar |  |
| TaeedHesabdari | bit |  |
| UserRejectedHesabdari | int |  |
| DateRejectedHesabdari | nvarchar |  |
| Rejected | bit |  |
| RejectedComment | nvarchar |  |
| GhnoInDar | int |  |
| ExtraPrice | decimal |  |
| Tafif | decimal |  |

## PishDaryaftGetCheck
- Type: Tables | PK: RowID | Columns: 10
- FK refs: FK_PishDaryaftGetCheck_PishDaryaft: PishDaryaftGetCheck.PishDaryaftGetCheck→PishDaryaftGetCheck

| Column | SQL Type | Nullable |
|---|---|---|
| RowID | int |  |
| GhnoPishDaryaft | int |  |
| sardate | nvarchar |  |
| ShomareHesab | nvarchar |  |
| BankName | nvarchar |  |
| Shobe | nvarchar |  |
| Serial | nvarchar |  |
| Price | decimal |  |
| Desc | nvarchar |  |
| ShenaseSayad | nvarchar |  |

## PishDaryaftMultiFactor
- Type: Tables | PK: RowID | Columns: 4
- FK refs: FK_PishDaryaftMultiFactor_PishDaryaft: PishDaryaftMultiFactor.PishDaryaftMultiFactor→PishDaryaftMultiFactor

| Column | SQL Type | Nullable |
|---|---|---|
| RowID | int |  |
| GhnoPishDaryaft | int |  |
| Shfacfo | int |  |
| Price | decimal |  |

## PishDaryaftPos
- Type: Tables | PK: RowID | Columns: 6
- FK refs: FK_PishDaryaftPos_BANK: PishDaryaftPos.PishDaryaftPos→PishDaryaftPos; FK_PishDaryaftPos_PishDaryaft: PishDaryaftPos.PishDaryaftPos→PishDaryaftPos

| Column | SQL Type | Nullable |
|---|---|---|
| RowID | int |  |
| GhnoPishDaryaft | int |  |
| price | decimal |  |
| PosBankRdf | int |  |
| ShomarePeygiri | nvarchar |  |
| IsPDA | bit |  |

## Province
- Type: Tables | PK: ProvinceID | Columns: 6
- FK refs: FK_CITYS_Province: Province.Province→Province

| Column | SQL Type | Nullable |
|---|---|---|
| ProvinceID | int |  |
| Name | nvarchar |  |
| Lat | float |  |
| Lng | float |  |
| TempProvinceID | int |  |
| CodeTTMS | nvarchar |  |

## SaleFactTasvieh
- Type: Tables | PK: TasviehID | Columns: 3

| Column | SQL Type | Nullable |
|---|---|---|
| TasviehID | int |  |
| Description | nvarchar |  |
| TedRooz | int |  |

## TabletCustomer
- Type: Tables | PK: id | Columns: 24
- FK refs: TabletCustomer_CUSTOMERS_SHMO_fk: TabletCustomer.TabletCustomer→TabletCustomer; TabletCustomer_visitors_vis_rdf_fk: TabletCustomer.TabletCustomer→TabletCustomer

| Column | SQL Type | Nullable |
|---|---|---|
| id | int |  |
| vis_rdf | int |  |
| shmo | int |  |
| create_date | varchar |  |
| birth_date | varchar |  |
| name | nvarchar |  |
| melli_code | nvarchar |  |
| tell1 | nvarchar |  |
| tell2 | nvarchar |  |
| cell | nvarchar |  |
| address | nvarchar |  |
| sharh | nvarchar |  |
| estijari | bit |  |
| owners_count | int |  |
| metraj_shop | decimal |  |
| metraj_yakhchal | decimal |  |
| yakhchal_count | int |  |
| tablo | nvarchar |  |
| sabeghe | int |  |
| Lat | float |  |
| Lng | float |  |
| Active | bit |  |
| IsEdit | bit |  |
| DoneSave | bit |  |

## VWForushKhales
- Type: Unknown | PK: coka | Columns: 90

| Column | SQL Type | Nullable |
|---|---|---|
| coka | nvarchar |  |
| vazn | decimal |  |
| sysid | int |  |
| vis_name | varchar |  |
| SHKA | bigint |  |
| naka | nvarchar |  |
| group_rdf | int |  |
| group_name | nvarchar |  |
| shfac | bigint |  |
| act_dis | nvarchar |  |
| act_date | char |  |
| user | varchar |  |
| UserID | int |  |
| vahsanj | nvarchar |  |
| mohvah | bigint |  |
| mohvah2 | int |  |
| tedvah_fel | decimal |  |
| tedjoz_fel | int |  |
| tedbastebandi_fel | decimal |  |
| JOZPRICE | money |  |
| VAHPRICE | money |  |
| rdf_kh | int |  |
| pervis | decimal |  |
| vis_rdf | int |  |
| moname | nvarchar |  |
| shmo | int |  |
| kol_price | decimal |  |
| Tafif_khales | money |  |
| TafifHajmi | decimal |  |
| kol_price_tafif | decimal |  |
| CountKala | int |  |
| Vis_sahm | decimal |  |
| Status | int |  |
| sh_taraz_kh | int |  |
| pertaf | decimal |  |
| tafAghlamValue | decimal |  |
| rdf_anbar | int |  |
| name | nvarchar |  |
| rdf_driver | int |  |
| driver_name | varchar |  |
| rdf_mamorp | int |  |
| mamorp_name | varchar |  |
| code | nvarchar |  |
| RoleCode | nvarchar |  |
| c_mel | nvarchar |  |
| c_egh | nvarchar |  |
| Shenaseh_Egh | nvarchar |  |
| barbari | money |  |
| ptax | decimal |  |
| pavarez | decimal |  |
| EnterdNaka | nvarchar |  |
| rdf__ | int |  |
| custGroupRdf | int |  |
| vahwe | decimal |  |
| bastebandi | nvarchar |  |
| CusAddre | nvarchar |  |
| CusTell1 | nvarchar |  |
| CusTell2 | nvarchar |  |
| CusMan | money |  |
| SailMabDaryaftFactor | money |  |
| CustomerCell | nvarchar |  |
| RDFMasir | int |  |
| MasirName | varchar |  |
| RDFRegion | int |  |
| RegionName | varchar |  |
| RDFCity | int |  |
| CityName | varchar |  |
| RDFProvince | int |  |
| ProvinceName | nvarchar |  |
| CusGroupName | varchar |  |
| KHKol | decimal |  |
| DateTasvieh | char |  |
| TasviehStatus | char |  |
| kol_price_WithTAT | decimal |  |
| description | nvarchar |  |
| Gift | bit |  |
| kolpricejayeze | decimal |  |
| PerPromotion | decimal |  |
| PromotionValue | decimal |  |
| ShfacTaeedErsal | bigint |  |
| name_System | nvarchar |  |
| time_ | varchar |  |
| WithProductionSerial | bit |  |
| ProductionSeriesID | bigint |  |
| SubmittedTax | int |  |
| TaxUniqueID | nvarchar |  |
| DateSendToTaxSystem | nchar |  |
| TimeSendToTaxSystem | nchar |  |
| TransferCode | nvarchar |  |
| CustomerTransferCode | nvarchar |  |

## VW_AllBargashti
- Type: Unknown | PK: coka | Columns: 85

| Column | SQL Type | Nullable |
|---|---|---|
| coka | nvarchar |  |
| rdf_anbar | int |  |
| rdf_kh | int |  |
| sysid | int |  |
| vahprice | money |  |
| litakhma | money |  |
| rdf | int |  |
| group_rdf | int |  |
| kol_price | decimal |  |
| kol_price_tafif | decimal |  |
| act_id | int |  |
| vis_sahm | decimal |  |
| shfac | bigint |  |
| sh_back_sanad | int |  |
| act_date | char |  |
| user | varchar |  |
| shka | bigint |  |
| naka | nvarchar |  |
| barbari | money |  |
| mohvah | bigint |  |
| tedvah | decimal |  |
| tedjoz | int |  |
| jozprice | money |  |
| ptax | decimal |  |
| vis_rdf | int |  |
| pertaf | decimal |  |
| pervis | decimal |  |
| pavarez | decimal |  |
| HAct_id | int |  |
| Hour | char |  |
| sh_sanad | bigint |  |
| sh_factor | bigint |  |
| shmo | int |  |
| Moname | nvarchar |  |
| CustomerMan | money |  |
| addre | nvarchar |  |
| tell1 | nvarchar |  |
| tell2 | nvarchar |  |
| cell | nvarchar |  |
| c_mel | nvarchar |  |
| code | nvarchar |  |
| c_egh | nvarchar |  |
| Shenaseh_Egh | nvarchar |  |
| CustGroupRDF | int |  |
| CustGroupName | varchar |  |
| CityID | int |  |
| CityName | varchar |  |
| RegionID | int |  |
| RegionName | varchar |  |
| ProvinceID | int |  |
| ProvinceName | nvarchar |  |
| RDF_masir | int |  |
| RoleCode | nvarchar |  |
| Name_masir | varchar |  |
| sh_taraz | int |  |
| Type_bargashti | varchar |  |
| date_forosh | char |  |
| act_dis | varchar |  |
| PerFine | decimal |  |
| group_name | nvarchar |  |
| PerPromotionSF | decimal |  |
| rdf_driver | int |  |
| rdf_mamorp | int |  |
| active | char |  |
| VisName | varchar |  |
| BASTEBANDI | nvarchar |  |
| NameAnbar | nvarchar |  |
| done_date | char |  |
| TafifHajmi1 | decimal |  |
| TedBasteBandi | decimal |  |
| CanNegativeKaGroup | int |  |
| VisNamePerfine | varchar |  |
| VisIDPerfine | int |  |
| RevocationID | int |  |
| RevocationName | nvarchar |  |
| visitorGarima | decimal |  |
| WithProductionSerial | bit |  |
| ProductionSeriesID | bigint |  |
| mohvah2 | int |  |
| CodePs | nvarchar |  |
| ExpDate | nvarchar |  |
| ExpDateGregorian | nvarchar |  |
| PsName | nvarchar |  |
| PriceCustomer | money |  |
| ProductionPrice | money |  |

## VW_FORUSH_RizAghlam_Nakhales
- Type: Unknown | PK: coka | Columns: 90

| Column | SQL Type | Nullable |
|---|---|---|
| coka | nvarchar |  |
| StuffCode | nvarchar |  |
| vazn | decimal |  |
| tasvieh | char |  |
| active | char |  |
| tdf | money |  |
| MabDaryaftFactor | money |  |
| RdfGroupCust | int |  |
| NameGroupCust | varchar |  |
| shka | bigint |  |
| naka | nvarchar |  |
| group_rdf | int |  |
| group_name | nvarchar |  |
| shfac | bigint |  |
| act_date | char |  |
| user | varchar |  |
| UserID | int |  |
| vahsanj | nvarchar |  |
| mohvah | bigint |  |
| tedvah | decimal |  |
| tedjoz | int |  |
| tedbastebandi | decimal |  |
| price | money |  |
| rdf_kh | int |  |
| ptax | decimal |  |
| pertaf | decimal |  |
| tax | money |  |
| litakhma | money |  |
| pavarez | decimal |  |
| avarez | money |  |
| pervis | decimal |  |
| vis_rdf | int |  |
| vis_name | varchar |  |
| moname | nvarchar |  |
| shmo | int |  |
| JOZPRICE | money |  |
| VAHPRICE | money |  |
| TafAghlam | money |  |
| TafifHajmi | decimal |  |
| act_dis | nvarchar |  |
| priceKol_tafif | decimal |  |
| price_kol | decimal |  |
| CountKala | int |  |
| vis_sahm | decimal |  |
| Status | int |  |
| sh_taraz_kh | int |  |
| rdf_anbar | int |  |
| name | nvarchar |  |
| rdf_driver | int |  |
| driver_name | varchar |  |
| rdf_mamorp | int |  |
| mamorp_name | varchar |  |
| rdf_city | int |  |
| man | money |  |
| rdf_region | int |  |
| RDF_masir | int |  |
| code | nvarchar |  |
| sysid | int |  |
| barbari | money |  |
| t_date | char |  |
| rdf__ | int |  |
| tell1 | nvarchar |  |
| tell2 | nvarchar |  |
| cell | nvarchar |  |
| addre | nvarchar |  |
| c_mel | nvarchar |  |
| c_egh | nvarchar |  |
| Shenaseh_Egh | nvarchar |  |
| RoleCode | nvarchar |  |
| description | nvarchar |  |
| bastebandi | nvarchar |  |
| EnterdNaka | nvarchar |  |
| NKol | decimal |  |
| ShSanadFerestande | nvarchar |  |
| PriceKolWithTAT | decimal |  |
| LineName | nvarchar |  |
| PerPromotion | decimal |  |
| PromotionValue | decimal |  |
| ShfacTaeedErsal | bigint |  |
| time_ | varchar |  |
| WithProductionSerial | bit |  |
| ProductionSeriesID | bigint |  |
| done_date | char |  |
| mohvah2 | int |  |
| TaxUniqueID | nvarchar |  |
| DateSendToTaxSystem | nchar |  |
| TimeSendToTaxSystem | nchar |  |
| SubmittedTax | int |  |
| TransferCode | nvarchar |  |
| CustomerTransferCode | nvarchar |  |

## VW_Forush_DarBazeZamani
- Type: Unknown | PK: coka | Columns: 107

| Column | SQL Type | Nullable |
|---|---|---|
| coka | nvarchar |  |
| sh_taraz_kh | int |  |
| Pcustomer | numeric |  |
| Pzayeat | numeric |  |
| Psalem | numeric |  |
| pricesalem | numeric |  |
| pricezayeat | numeric |  |
| Pbargashti | numeric |  |
| CountJayeze | int |  |
| KindKala | int |  |
| mohvah | bigint |  |
| pathname | nvarchar |  |
| ProvinceID | int |  |
| ProvinceName | nvarchar |  |
| CityRDF | int |  |
| RegionID | int |  |
| RegionName | varchar |  |
| Cityname | varchar |  |
| vazn | decimal |  |
| tell1 | nvarchar |  |
| tell2 | nvarchar |  |
| cell | nvarchar |  |
| addre | nvarchar |  |
| code | nvarchar |  |
| active | char |  |
| t_date | char |  |
| c_mel | nvarchar |  |
| c_egh | nvarchar |  |
| Shenaseh_Egh | nvarchar |  |
| MabDaryaftFactor | money |  |
| tdf | money |  |
| tasvieh | char |  |
| CustomerMan | money |  |
| CustomerManDetail | varchar |  |
| man | money |  |
| act_date | char |  |
| vis_rdf | int |  |
| naka | nvarchar |  |
| CountKala | int |  |
| countcustomer | int |  |
| vis_name | varchar |  |
| user | varchar |  |
| group_rdf | int |  |
| group_name | nvarchar |  |
| rdf_mamorp | int |  |
| shka | bigint |  |
| rdf_anbar | int |  |
| RDF_masir | int |  |
| rdf_driver | int |  |
| Status | int |  |
| sysid | int |  |
| rdf_kh | int |  |
| facfo | bigint |  |
| moname | nvarchar |  |
| RoleCode | nvarchar |  |
| shmo | int |  |
| RdfGroupCust | int |  |
| NameGroupCust | varchar |  |
| NPricekol | decimal |  |
| Ntax | money |  |
| Navarez | money |  |
| Ntafif | money |  |
| NTafifHajmi | decimal |  |
| Nbarbari | money |  |
| Ntedjoz | int |  |
| Ntedvah | decimal |  |
| Nvis_Sahm | decimal |  |
| Nvazn | numeric |  |
| NCountFactor | int |  |
| JOZPRICE | money |  |
| pertaf | decimal |  |
| NPricekol_Taf | decimal |  |
| KHPricekol | decimal |  |
| KHvis_sahm | decimal |  |
| KHbarbari | money |  |
| KHtax | decimal |  |
| KHavarez | decimal |  |
| KHshfacfo | bigint |  |
| KHvazn | numeric |  |
| KHCountFactor | int |  |
| KHPricekol_Taf | decimal |  |
| KHtafif | money |  |
| KHtedjoz | int |  |
| KHtedvah | decimal |  |
| BShfacfo | bigint |  |
| Brdf_anbar | int |  |
| Btedvah | decimal |  |
| Btedjoz | int |  |
| BPricekol | decimal |  |
| Bvazn | numeric |  |
| Bvaznzayeat | numeric |  |
| Bvaznsalem | numeric |  |
| BCountFactor | int |  |
| Bbarbari | money |  |
| Bvis_sahm | decimal |  |
| Btax | decimal |  |
| Bavarez | decimal |  |
| BPricekol_Taf | decimal |  |
| Btafif | money |  |
| ShSanadFerestande | nvarchar |  |
| LineName | nvarchar |  |
| ShfacTaeedErsal | bigint |  |
| WithProductionSerial | bit |  |
| ProductionSeriesID | bigint |  |
| done_date | char |  |
| mohvah2 | int |  |
| time_ | varchar |  |

## VW_GoalsVisitors
- Type: Unknown | PK: rdf | Columns: 25

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| baze_rdf | int |  |
| vis_rdf | int |  |
| mab | money |  |
| ted | decimal |  |
| Active | bit |  |
| CustomerGroupRdf | int |  |
| KalaGroupRdf | int |  |
| InventoryID | bigint |  |
| ProvinceID | int |  |
| CityID | int |  |
| RegionID | int |  |
| PathID | int |  |
| RdfBaze | int |  |
| BazeName | varchar |  |
| StartDate | char |  |
| EndDate | char |  |
| CusGroupName | varchar |  |
| KaGroupName | nvarchar |  |
| Naka | nvarchar |  |
| ProvinceName | nvarchar |  |
| CityName | varchar |  |
| RegionName | varchar |  |
| PathName | varchar |  |
| VisitorName | varchar |  |

## VW_InventoryAnbars
- Type: Unknown | PK: shka | Columns: 8

| Column | SQL Type | Nullable |
|---|---|---|
| shka | bigint |  |
| rdf_anbars | int |  |
| name | nvarchar |  |
| tedbastebandi | decimal |  |
| mojkavah | decimal |  |
| mojkajoz | int |  |
| MojodiPish_vah | decimal |  |
| MojodiPish_joz | decimal |  |

## Visit
- Type: Tables | PK: VisitID | Columns: 15
- FK refs: FK_Visit_CUSTOMERS: Visit.Visit→Visit; FK_Visit_visitors: Visit.Visit→Visit; FK_sailfact_pish_visit: Visit.Visit→Visit

| Column | SQL Type | Nullable |
|---|---|---|
| VisitID | bigint |  |
| VisRdf | int |  |
| Shmo | int |  |
| Duration | int |  |
| Created | bigint |  |
| Sent | bigint |  |
| Description | nvarchar |  |
| SentLng | float |  |
| SentLat | float |  |
| SaveLat | float |  |
| SaveLng | float |  |
| DateCreated | char |  |
| DateSent | char |  |
| TimeCreated | char |  |
| TimeSent | char |  |

## anbars
- Type: Tables | PK: rdf_anbar | Columns: 10
- FK refs: FK_inventory_anbars_anbars: anbars.anbars→anbars; FK_osystems_anbars: anbars.anbars→anbars; FK_subsailfact_anbars: anbars.anbars→anbars; FK_sys_anb_anbars: anbars.anbars→anbars

| Column | SQL Type | Nullable |
|---|---|---|
| rdf_anbar | int |  |
| name | nvarchar |  |
| addre | nvarchar |  |
| start_date | nvarchar |  |
| tell1 | nvarchar |  |
| tell2 | nvarchar |  |
| anbardar | nvarchar |  |
| Active | bit |  |
| Base | bit |  |
| UserID | int |  |

## baze
- Type: Tables | PK: rdf | Columns: 7
- FK refs: FK_vis_goals_baze: baze.baze→baze

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| name | varchar |  |
| sta | char |  |
| end_ | char |  |
| Active | bit |  |
| CopyFrom | int |  |
| UserID | int |  |

## cust_act
- Type: Tables | PK: rdf_ | Columns: 20
- FK refs: FK_cust_act_CUSTOMERS: cust_act.cust_act→cust_act

| Column | SQL Type | Nullable |
|---|---|---|
| rdf_ | int |  |
| shmo | int |  |
| date | char |  |
| act_bes | money |  |
| act_bed | money |  |
| act_dis | nvarchar |  |
| act_id | int |  |
| ghno | bigint |  |
| done_date | char |  |
| t_time | datetime |  |
| ShowInReport | bit |  |
| DocNumber | int |  |
| MoghayeratDiscription | nvarchar |  |
| sysid | int |  |
| isActive | bit |  |
| fk_id | bigint |  |
| Rdf_ForDar | int |  |
| GhestPriceTemp | decimal |  |
| UserID | int |  |
| AccDocNumber | int |  |

## custgroup
- Type: Tables | PK: group_rdf | Columns: 9
- FK refs: FK_CUSTOMERS_custgroup: custgroup.custgroup→custgroup; FK_custgroup_custgroup: custgroup.custgroup1→custgroup; FK_custgroup_custgroup: custgroup.custgroup→custgroup; FK_vis_goals_custgroup: custgroup.custgroup→custgroup

| Column | SQL Type | Nullable |
|---|---|---|
| group_name | varchar |  |
| group_rdf | int |  |
| stdate | char |  |
| price | int |  |
| ted_rooz | int |  |
| AccType | int |  |
| MoeinID | bigint |  |
| Active | bit |  |
| PerGain | decimal |  |

## forosh_price
- Type: Tables | PK: shka | Columns: 21
- FK refs: FK_forosh_price_inventory: forosh_price.forosh_price→forosh_price

| Column | SQL Type | Nullable |
|---|---|---|
| shka | bigint |  |
| forosh1 | money |  |
| mp1 | int |  |
| pv1 | decimal |  |
| forosh2 | money |  |
| mp2 | int |  |
| pv2 | decimal |  |
| forosh3 | money |  |
| mp3 | int |  |
| pv3 | decimal |  |
| forosh4 | money |  |
| mp4 | int |  |
| pv4 | decimal |  |
| forosh5 | money |  |
| mp5 | int |  |
| pv5 | decimal |  |
| naka | nvarchar |  |
| group_rdf | int |  |
| active | char |  |
| MinPrice | money |  |
| MaxPrice | money |  |

## getchk
- Type: Tables | PK: rdf | Columns: 49

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | bigint |  |
| getdate | char |  |
| sardate | char |  |
| getchkshhes | nvarchar |  |
| getchbank | nvarchar |  |
| getchkshobe | nvarchar |  |
| shgetchk | nvarchar |  |
| getchkmab | money |  |
| shmo | bigint |  |
| VIRTUALNAME | varchar |  |
| shahrestan | int |  |
| chk_satus | int |  |
| our_bankrdf | int |  |
| our_bankrdf_date | char |  |
| naghddate | char |  |
| back | char |  |
| kharj_date | char |  |
| kharj_shmo | bigint |  |
| karj_virtualname | varchar |  |
| ghno | int |  |
| rdf_in_ghno | int |  |
| done_date | char |  |
| vagozarande | varchar |  |
| our_bankrdf_donedate | char |  |
| naghddonedate | char |  |
| kharj_done_date | char |  |
| mod | int |  |
| vis_rdf | int |  |
| kharj_mod | int |  |
| shfac | int |  |
| rdf_mamorm | int |  |
| rdf_mamorp | int |  |
| rdf_driver | int |  |
| soo | int |  |
| sysid | int |  |
| DocID | bigint |  |
| FromAtiranDocID | bigint |  |
| Description | nvarchar |  |
| p | int |  |
| GhnoPardakht | int |  |
| GhnoKharj | int |  |
| Rdf_ | int |  |
| TafsilID | bigint |  |
| Rdf_P | int |  |
| UserID | int |  |
| ShenaseSayad | nvarchar |  |
| RegistrationInquiry | bit |  |
| IdDocumentZirSarfasl | int |  |
| ToNewYear | bit |  |

## inventory
- Type: Tables | PK: shka | Columns: 51
- FK refs: FK_forosh_price_inventory: inventory.inventory→inventory; FK_inventory_anbars_inventory: inventory.inventory→inventory; FK_inventory_kagroup: inventory.inventory→inventory; FK_pricePercent_inventory: inventory.inventory→inventory; FK_subsailfact_inventory: inventory.inventory→inventory; FK_sys_kal_sys_kal: inventory.inventory→inventory; FK_vis_goals_inventory: inventory.inventory→inventory

| Column | SQL Type | Nullable |
|---|---|---|
| shka | bigint |  |
| naka | nvarchar |  |
| coka | nvarchar |  |
| group_rdf | int |  |
| vahsanj | nvarchar |  |
| mohvah | bigint |  |
| mojkavah | decimal |  |
| mojkajoz | int |  |
| reopoint | int |  |
| bastebandi | nvarchar |  |
| tedbastebandi | decimal |  |
| vahwe | decimal |  |
| vahsp | decimal |  |
| visper | decimal |  |
| active | char |  |
| pure_buy_price | money |  |
| buy_price | money |  |
| inventory_price | money |  |
| MODPAR | int |  |
| buyjoz | money |  |
| sharh | nvarchar |  |
| min_sef | int |  |
| ted_f_ja | int |  |
| ted_ja | int |  |
| shka_ja | bigint |  |
| barbari_vahed | money |  |
| ptax | decimal |  |
| inventory_price_tax | money |  |
| maxtafnaghd | decimal |  |
| black_list | int |  |
| backfine | decimal |  |
| ExpirationDate | nchar |  |
| FinalSalePrice | money |  |
| PAvarez | decimal |  |
| ImPureBuyPrice | money |  |
| MaxJozForosh | decimal |  |
| PerPos | decimal |  |
| GoodsKindID | int |  |
| InventoryTypeID | int |  |
| WithProductionSerial | bit |  |
| ActiveOnlineSales | bit |  |
| ActiveCapillarySales | bit |  |
| reducePrizeFromSales | bit |  |
| siteId | bigint |  |
| nakaEN | nvarchar |  |
| isCustomerClub | bit |  |
| MandatoryRoleCode | bit |  |
| TransferCode | nvarchar |  |
| mohvah2 | int |  |
| ProductionPrice | money |  |
| StuffCode | nvarchar |  |

## inventory_anbars
- Type: Tables | PK: rdf_anbars | Columns: 6
- FK refs: FK_inventory_anbars_anbars: inventory_anbars.inventory_anbars→inventory_anbars; FK_inventory_anbars_inventory: inventory_anbars.inventory_anbars→inventory_anbars

| Column | SQL Type | Nullable |
|---|---|---|
| rdf_anbars | int |  |
| shka | bigint |  |
| mojkavah | decimal |  |
| mojkajoz | int |  |
| name | nvarchar |  |
| tedbastebandi | decimal |  |

## ka_image
- Type: Tables | PK: rdf | Columns: 3

| Column | SQL Type | Nullable |
|---|---|---|
| shka | int |  |
| pic | image |  |
| rdf | int |  |

## kagroup
- Type: Tables | PK: group_rdf | Columns: 12
- FK refs: FK_inventory_kagroup: kagroup.kagroup→kagroup; FK_kagroup_kagroup: kagroup.kagroup1→kagroup; FK_kagroup_kagroup: kagroup.kagroup→kagroup; FK_pricePercentGroup_kagroup: kagroup.kagroup→kagroup; FK_vis_goals_kagroup: kagroup.kagroup→kagroup

| Column | SQL Type | Nullable |
|---|---|---|
| group_rdf | int |  |
| group_name | nvarchar |  |
| stdate | datetime |  |
| CanNegative | int |  |
| ParentGroupRdf | int |  |
| GroupLevel | int |  |
| hasPic | bit |  |
| Active | bit |  |
| siteId | bigint |  |
| GroupCode | nvarchar |  |
| KalaTypeTtmsId | bigint |  |
| group_code | nvarchar |  |

## masir
- Type: Tables | PK: rdf_masir | Columns: 5
- FK refs: FK_CUSTOMERS_masir: masir.masir→masir; FK_masir_regions: masir.masir→masir; FK_masir_visitor: masir.masir→masir

| Column | SQL Type | Nullable |
|---|---|---|
| rdf_masir | int |  |
| rdf_region | int |  |
| shomare_masir | int |  |
| name | varchar |  |
| vis_rdf | int |  |

## osystems
- Type: Tables | PK: rdf_system | Columns: 17
- FK refs: FK_PishDaryaft_osystems: osystems.osystems→osystems; FK_osystems_anbars: osystems.osystems→osystems; FK_sys_anb_osystems: osystems.osystems→osystems; FK_sys_cus_osystems: osystems.osystems→osystems; FK_sys_kal_osystems: osystems.osystems→osystems; FK_sys_vis_osystems: osystems.osystems→osystems

| Column | SQL Type | Nullable |
|---|---|---|
| rdf_system | int |  |
| name_System | nvarchar |  |
| Address | nvarchar |  |
| tel1 | nvarchar |  |
| tel2 | nvarchar |  |
| fax | nvarchar |  |
| sh_sabt | nvarchar |  |
| c_egh | nvarchar |  |
| c_pos | nvarchar |  |
| kind_f | int |  |
| arm_ | image |  |
| eteb | money |  |
| Active | bit |  |
| AnbarRdf | int |  |
| LevelIdFive | int |  |
| LevelIdSix | int |  |
| LineTransferCode | nvarchar |  |

## overal_setting
- Type: Tables | PK: id | Columns: 6

| Column | SQL Type | Nullable |
|---|---|---|
| id | int |  |
| dis | nvarchar |  |
| value | bigint |  |
| Address | nvarchar |  |
| TabOrder | int |  |
| ExternalExplain | nvarchar |  |

## prizePercent
- Type: Tables | PK: Shka | Columns: 17
- FK refs: FK_pricePercent_inventory: prizePercent.prizePercent→prizePercent

| Column | SQL Type | Nullable |
|---|---|---|
| RowId | bigint |  |
| Shka | bigint |  |
| FromNum | int |  |
| ToNum | int |  |
| FromPrice | money |  |
| ToPrice | money |  |
| FromDate | char |  |
| ToDate | char |  |
| Percent | decimal |  |
| RdfCustGroup | int |  |
| RdfProvinc | int |  |
| RdfCity | int |  |
| RdfRegion | int |  |
| RdfMasir | int |  |
| SysID | int |  |
| DoneDate | char |  |
| UserID | int |  |

## prizePercentGroup
- Type: Tables | PK: RdfKalaGroup | Columns: 18
- FK refs: FK_pricePercentGroup_kagroup: prizePercentGroup.prizePercentGroup→prizePercentGroup

| Column | SQL Type | Nullable |
|---|---|---|
| RowId | bigint |  |
| RdfKalaGroup | int |  |
| FromNum | int |  |
| ToNum | int |  |
| FromPrice | money |  |
| ToPrice | money |  |
| FromDate | char |  |
| ToDate | char |  |
| Percent | decimal |  |
| RdfCustGroup | int |  |
| RdfProvinc | int |  |
| RdfCity | int |  |
| RdfRegion | int |  |
| RdfMasir | int |  |
| SysID | int |  |
| DoneDate | char |  |
| UserID | int |  |
| SubEffect | bit |  |

## prize_table
- Type: Tables | PK: shka | Columns: 15

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| shka | int |  |
| ted_st | int |  |
| ted_end | int |  |
| shkaja | int |  |
| nakaja | varchar |  |
| tedja | int |  |
| CusGroup | int |  |
| SysID | int |  |
| ProvinceID | int |  |
| CityID | int |  |
| RegionID | int |  |
| PathID | int |  |
| Date | nvarchar |  |
| UserID | int |  |

## prize_table_group
- Type: Tables | PK: rdf_group | Columns: 16

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| rdf_group | int |  |
| ted_st | int |  |
| ted_end | int |  |
| shkaja | int |  |
| nakaja | nvarchar |  |
| tedja | int |  |
| CusGroup | int |  |
| SysID | int |  |
| ProvinceID | int |  |
| CityID | int |  |
| RegionID | int |  |
| PathID | int |  |
| Date | nvarchar |  |
| SubEffect | bit |  |
| UserID | int |  |

## promotion
- Type: Unknown | PK: Shkala | Columns: 2

| Column | SQL Type | Nullable |
|---|---|---|
| Shkala | int |  |
| PromotionText | nvarchar |  |

## regions
- Type: Tables | PK: rdf_region | Columns: 5
- FK refs: FK_masir_regions: regions.regions→regions; FK_regions_CITYS: regions.regions→regions

| Column | SQL Type | Nullable |
|---|---|---|
| rdf_region | int |  |
| rdf_city | int |  |
| sh_region | int |  |
| name_region | varchar |  |
| Color | int |  |

## sailfact
- Type: Tables | PK: rdf__ | Columns: 66
- FK refs: FK_sailfact_CUSTOMERS: sailfact.sailfact→sailfact; FK_subsailfact_sailfact: sailfact.sailfact→sailfact

| Column | SQL Type | Nullable |
|---|---|---|
| rdf__ | int |  |
| shfacfo | bigint |  |
| date | char |  |
| shmo | int |  |
| barbari | money |  |
| description | nvarchar |  |
| vis_rdf | int |  |
| sumlineall | money |  |
| all | money |  |
| tafif | money |  |
| SumTafifAghlam | money |  |
| done_date | char |  |
| panevis | nvarchar |  |
| ismodify | char |  |
| active | char |  |
| modpar | int |  |
| rdf_tahbarg | int |  |
| nah_par | int |  |
| nah_d_text | nvarchar |  |
| man_gh | money |  |
| t_date | char |  |
| tasvieh | char |  |
| driver_name | varchar |  |
| rdf_driver | int |  |
| mamorp_name | varchar |  |
| rdf_mamorp | int |  |
| bamandeh | int |  |
| sh_taraz_kh | int |  |
| time_ | varchar |  |
| shpish | int |  |
| ba_tarikh | int |  |
| chap_f | bit |  |
| chap_h | bit |  |
| tax | money |  |
| moname | nvarchar |  |
| nahve_namayesh_daryaft | int |  |
| vazn | decimal |  |
| sysid | int |  |
| avarez | money |  |
| Status | int |  |
| TaeedDate | nvarchar |  |
| TaeedUser | nvarchar |  |
| sumlineall_fel | money |  |
| all_fel | money |  |
| SumTafifAghlam_fel | money |  |
| tax_fel | money |  |
| avarez_fel | money |  |
| userid | int |  |
| taffif_fel | money |  |
| MabDaryaftFactor | money |  |
| tdf | money |  |
| ShSanadFerestande | nvarchar |  |
| ExternalCosts | decimal |  |
| HajmiOverall | decimal |  |
| ChapWithTasvieh | bit |  |
| ChapWhitMande | bit |  |
| TaeedWarehous | bit |  |
| TaeedWarehousUserId | int |  |
| TaeedWarehousDate | char |  |
| TaxUniqueID | nvarchar |  |
| SubmittedTax | int |  |
| DateSendToTaxSystem | nchar |  |
| TimeSendToTaxSystem | nchar |  |
| UidTax | nvarchar |  |
| InvoiceSerialTax | nvarchar |  |
| SubmitTaxText | nvarchar |  |

## sailfact_pish
- Type: Tables | PK: rdf__ | Columns: 51
- FK refs: FK_sailfact_pish_CUSTOMERS: sailfact_pish.sailfact_pish→sailfact_pish; FK_sailfact_pish_visit: sailfact_pish.sailfact_pish→sailfact_pish; FK_subsailfact_pish_sailfact_pish: sailfact_pish.sailfact_pish→sailfact_pish

| Column | SQL Type | Nullable |
|---|---|---|
| rdf__ | int |  |
| shfacfo | bigint |  |
| USER__ | varchar |  |
| date | char |  |
| shmo | int |  |
| barbari | money |  |
| shfacthand | nvarchar |  |
| vis_rdf | int |  |
| sumlineall | money |  |
| all | money |  |
| gainall | money |  |
| tafif | money |  |
| jamtakhgh | money |  |
| done_date | char |  |
| panevis | varchar |  |
| isret | char |  |
| ismodify | char |  |
| active | char |  |
| modpar | int |  |
| rdf_sarbarg | int |  |
| rdf_tahbarg | int |  |
| nah_par | int |  |
| mod_darsad_vis | int |  |
| nah_d_text | text |  |
| man_gh | money |  |
| sh_f | bigint |  |
| user_f | varchar |  |
| date_f | char |  |
| ted_rooz | int |  |
| taeed | int |  |
| taeedUser | varchar |  |
| sysid | int |  |
| TaedHesabdari | bit |  |
| TaedForush | bit |  |
| UserTaedHesabdari | nvarchar |  |
| DateTaedHesabdari | nvarchar |  |
| UserTaedForush | nvarchar |  |
| DateTaedForush | nvarchar |  |
| VisitID | bigint |  |
| Stamp | nvarchar |  |
| Rejected | bit |  |
| RejectedUser | nvarchar |  |
| RejectedDate | nvarchar |  |
| RejectedComment | nvarchar |  |
| taraz_kh_pish | int |  |
| DateRecive | varchar |  |
| TimeRecive | varchar |  |
| tax | money |  |
| avarez | money |  |
| MpKol | int |  |
| MpIsAuto | bit |  |

## subsailfact
- Type: Tables | PK: rdf__ | Columns: 37
- FK refs: FK_subsailfact_anbars: subsailfact.subsailfact→subsailfact; FK_subsailfact_inventory: subsailfact.subsailfact→subsailfact; FK_subsailfact_sailfact: subsailfact.subsailfact→subsailfact

| Column | SQL Type | Nullable |
|---|---|---|
| rdf__ | int |  |
| shfacfo | bigint |  |
| SHKA | bigint |  |
| rdf_anbar | int |  |
| TEDVAH | decimal |  |
| TEDJOZ | int |  |
| VAHPRICE | money |  |
| JOZPRICE | money |  |
| BASTEBANDI | varchar |  |
| TEDBASTEBANDI | decimal |  |
| LINESUM | money |  |
| PERTAFIF | decimal |  |
| TafifAghlam | money |  |
| RDF | int |  |
| PERVIS | decimal |  |
| litakhma | money |  |
| active | char |  |
| naka | nvarchar |  |
| ptax | decimal |  |
| tax | money |  |
| avarez | money |  |
| pavarez | decimal |  |
| tedjoz_fel | int |  |
| tedvah_fel | decimal |  |
| tedbastebandi_fel | decimal |  |
| linesum_fel | money |  |
| litakhma_fel | money |  |
| tax_fel | money |  |
| avarez_fel | money |  |
| Gift | bit |  |
| TafifLine | money |  |
| PerPromotion | decimal |  |
| PromotionValue | decimal |  |
| TafifLineFel | decimal |  |
| ProductionSeriesID | bigint |  |
| TEDVAHMain | decimal |  |
| TEDJOZMain | int |  |

## subsailfact_pish
- Type: Tables | PK: rdf__ | Columns: 25
- FK refs: FK_subsailfact_pish_sailfact_pish: subsailfact_pish.subsailfact_pish→subsailfact_pish

| Column | SQL Type | Nullable |
|---|---|---|
| rdf__ | int |  |
| shfacfo | bigint |  |
| SHKA | bigint |  |
| rdf_anbar | int |  |
| TEDVAH | decimal |  |
| TEDJOZ | int |  |
| VAHPRICE | money |  |
| JOZPRICE | money |  |
| BASTEBANDI | varchar |  |
| TEDBASTEBANDI | int |  |
| LINESUM | money |  |
| LINEGAIN | money |  |
| ISRET | char |  |
| PERTAFIF | decimal |  |
| RDF | int |  |
| jozgain | money |  |
| PERVIS | decimal |  |
| litakhma | money |  |
| active | char |  |
| amani | bit |  |
| Pavarez | decimal |  |
| Avarez | money |  |
| Ptax | decimal |  |
| Tax | money |  |
| Mp | int |  |

## svcGetBanks
- Type: Unknown | PK: RDF | Columns: 3

| Column | SQL Type | Nullable |
|---|---|---|
| RDF | int |  |
| BankName | nvarchar |  |
| SysID | int |  |

## sys_anb
- Type: Tables | PK: SysID | Columns: 4
- FK refs: FK_sys_anb_anbars: sys_anb.sys_anb→sys_anb; FK_sys_anb_osystems: sys_anb.sys_anb→sys_anb

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| SysID | int |  |
| shanb | int |  |
| UserID | int |  |

## sys_cus
- Type: Tables | PK: SysID | Columns: 4
- FK refs: FK_sys_cus_CUSTOMERS: sys_cus.sys_cus→sys_cus; FK_sys_cus_osystems: sys_cus.sys_cus→sys_cus

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| SysID | int |  |
| Shmo | int |  |
| UserID | int |  |

## sys_kal
- Type: Tables | PK: rdf | Columns: 4
- FK refs: FK_sys_kal_osystems: sys_kal.sys_kal→sys_kal; FK_sys_kal_sys_kal: sys_kal.sys_kal→sys_kal

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| sysid | int |  |
| shka | bigint |  |
| UserID | int |  |

## sys_vis
- Type: Tables | PK: SysID | Columns: 4
- FK refs: FK_sys_vis_osystems: sys_vis.sys_vis→sys_vis; FK_sys_vis_visitors: sys_vis.sys_vis→sys_vis

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| SysID | int |  |
| shvis | int |  |
| UserID | int |  |

## vis_goals
- Type: Tables | PK: rdf | Columns: 13
- FK refs: FK_vis_goals_baze: vis_goals.vis_goals→vis_goals; FK_vis_goals_custgroup: vis_goals.vis_goals→vis_goals; FK_vis_goals_inventory: vis_goals.vis_goals→vis_goals; FK_vis_goals_kagroup: vis_goals.vis_goals→vis_goals; FK_vis_goals_visitors: vis_goals.vis_goals→vis_goals

| Column | SQL Type | Nullable |
|---|---|---|
| rdf | int |  |
| baze_rdf | int |  |
| vis_rdf | int |  |
| CustomerGroupRdf | int |  |
| mab | money |  |
| ted | decimal |  |
| Active | bit |  |
| KalaGroupRdf | int |  |
| InventoryID | bigint |  |
| ProvinceID | int |  |
| CityID | int |  |
| RegionID | int |  |
| PathID | int |  |

## visitors
- Type: Tables | PK: vis_rdf | Columns: 33
- FK refs: DeviceSettings_visitors_vis_rdf_fk: visitors.visitors→visitors; FK_DeviceMessages_visitors: visitors.visitors→visitors; FK_PishDaryaft_visitors: visitors.visitors→visitors; FK_Visit_visitors: visitors.visitors→visitors; FK_masir_visitor: visitors.visitors→visitors; FK_sys_vis_visitors: visitors.visitors→visitors; FK_vis_goals_visitors: visitors.visitors→visitors; FK_visitors_Devices: visitors.visitors→visitors; TabletCustomer_visitors_vis_rdf_fk: visitors.visitors→visitors

| Column | SQL Type | Nullable |
|---|---|---|
| vis_rdf | int |  |
| vis_name | varchar |  |
| vis_addre | varchar |  |
| vis_tell1 | varchar |  |
| vis_tell2 | varchar |  |
| vis_cell | varchar |  |
| vis_man | money |  |
| VIs_region | int |  |
| viss_date | char |  |
| active | char |  |
| vis_city | int |  |
| h_sabet | money |  |
| image | image |  |
| is_supervisor | char |  |
| supervisor_rdf | int |  |
| supervisor_per | decimal |  |
| dar_z | decimal |  |
| eteb | money |  |
| per_p_d_naghd | decimal |  |
| per_p_d_check | decimal |  |
| per_jar_bch | decimal |  |
| kind | int |  |
| tedad_fmmt | int |  |
| mab_fmmt | money |  |
| rdf_device | int |  |
| TedadFactorMojazMande | int |  |
| MablaghMojazMandeJahatFactorha | decimal |  |
| Type1 | bit |  |
| Type2 | bit |  |
| Username | nvarchar |  |
| Password | nvarchar |  |
| UserID | int |  |
| rdf_device_distribution | int |  |
