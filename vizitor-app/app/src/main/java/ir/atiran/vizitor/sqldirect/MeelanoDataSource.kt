/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | منبع دادهٔ مستقیم SQL Server (دیتابیس Meelano)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  معماری:  UI (Compose) → ViewModel → Repository → DataSource (اینجا) →
 *           SqlConnectionManager → SQL Server 192.168.1.150:1433 → Meelano
 *
 *  ⚠️ قانون قطعی این فایل: هر نام جدول/ستون در کوئری‌ها از خروجی ممیزی
 *  واقعی سرور گرفته شده و در docs/schema/meelano-columns.tsv ثبت است.
 *  هیچ نامی حدس زده نشده؛ ابزار tools/check_sql_columns.py همین را چک می‌کند.
 *
 *  همهٔ توابع suspend هستند و از withConnection استفاده می‌کنند → هیچ کوئری‌ای
 *  روی ترد UI اجرا نمی‌شود و اتصال همیشه به pool برمی‌گردد (بدون leak).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import ir.atiran.vizitor.data.repository.ServerConfig
import java.sql.ResultSet

// ─────────────────────────────────────────────────────────────────────────────
//  مدل‌های خروجی (سطر خام دیتابیس؛ مپ به Room در Repository انجام می‌شود)
// ─────────────────────────────────────────────────────────────────────────────

/** یک ردیف کالا با هر ۵ سطح قیمت Atiran و موجودی. */
data class DbProduct(
    val shka: Long,
    val name: String,
    val code: String,
    val groupRdf: Int,
    val unit: String,
    val stockVah: Double,
    val stockJoz: Int,
    val pieceWeight: Double,
    val packSize: Double,
    val expirationDate: String?,
    val priceTier1: Long,
    val priceTier2: Long,
    val priceTier3: Long,
    val priceTier4: Long,
    val priceTier5: Long,
    val minPrice: Long,
    val maxPrice: Long,
)

/** یک ردیف مشتری (بر اساس فیلترهای verified: sys_cus و CUSTOMERS). */
data class DbCustomer(
    val shmo: Int,
    val name: String,
    val code: String,
    val groupRdf: Int,
    val groupName: String?,
    val cityRdf: Int?,
    val address: String,
    val phone: String,
    val credit: Long,
    val debt: Long,
    val blackList: Int?,
    val active: String,
    val lat: Double?,
    val lng: Double?,
    val visitorRdf: Int,
)

/** گروه کالا — از dbo.kagroup (ستون‌های ممیزی‌شدهٔ group_rdf و group_name). */
data class DbProductGroup(
    val groupRdf: Int,
    val name: String,
)

/**
 * یک فاکتور/پیش‌فاکتور واقعیِ ویزیتور، از جداول خودِ ERP.
 * همهٔ ستون‌های استفاده‌شده در ممیزی سرور تأیید شده‌اند:
 *   dbo.sailfact      → shfacfo, date, shmo, all, tafif, vis_rdf, sysid
 *   dbo.sailfact_pish → shfacfo, date, shmo, all, tafif, vis_rdf, sysid
 * هیچ ستونی حدس زده نشده و هیچ مقداری ساخته نمی‌شود.
 */
data class DbInvoiceRow(
    val number: String,
    val dateText: String,
    val customerCode: String,
    val total: Long,
    val discount: Long,
    val isPreInvoice: Boolean,
)

/** گروه مشتری + سطح قیمت واقعی (custgroup.price = ستون تیر قیمت). */
data class DbCustomerGroup(
    val groupRdf: Int,
    val name: String,
    val priceTier: Int?,
)

/** موجودی یک کالا در یک انبار — از ویو خودِ ERP (VW_InventoryAnbars).
 *  quantity = موجودی روی کاغذ؛ available* = قابل فروش، یعنی موجودی منهای
 *  مقداری که پیش‌فاکتورهای بازِ دیگر روی همان کالا/انبار گرفته‌اند. */
data class DbStockRow(
    val shka: Long,
    val warehouseRdf: Int,
    val warehouseName: String,
    val quantity: Double,
    val quantityPiece: Int,
    val availableQuantity: Double = 0.0,
    val availablePiece: Int = 0,
)

/** هویت ویزیتور: از visitors (کلید کسب‌وکار) و sys_users (کلید ورود). */
data class DbVisitorIdentity(
    val userId: Int,
    val username: String,
    val displayName: String,
    val visitorRdf: Int?,
    val allowedCustomers: Int,
    val allowedProducts: Int,
    val allowedWarehouses: Int,
)

/** ردیف خام جدول sys_users برای ورود (بدون هیچ ستون رمزی). */
data class DbLoginRow(
    val userId: Int,
    val username: String,
    val fullName: String,
    val roleId: Int?,
    val active: Boolean,
    val locked: Boolean,
    val companyId: Int,
)

/**
 * منبع دادهٔ فقط‌خواندنی روی دیتابیس واقعی Meelano.
 * لایهٔ نوشتن (پیش‌فاکتور) بعد از تأیید بدنهٔ stored procedureها اضافه می‌شود.
 */
class MeelanoDataSource(private val db: SqlConnectionManager) {

    // ── ورود: مطابق دقیق رفتار خودِ ERP (بدون هیچ حدسی) ──────────────────────
    //  شواهد از بدنهٔ توابع واقعی ERP روی همین سرور:
    //    · dbo.SetUserpass:   select convert(varchar(50), user_password) from sys_users ...
    //    · dbo.ChangeUserPassInSalMali:
    //          update ... set user_password = CONVERT(varbinary, @PassWord) ...
    //    · ممیزی روی دادهٔ واقعی: DATALENGTH(user_password)=1 و hex آن '31' (= کاراکتر '1')
    //  ⇒ رمز در این ERP «متن ساده» داخل varbinary است و SQL Server هش نمی‌کند؛
    //    پس PWDCOMPARE هرگز جواب نمی‌دهد و مقایسه باید با CONVERT(varchar(50), …) باشد،
    //    دقیقاً همان‌طور که خودِ ERP رمز را می‌خواند.
    //
    //  نکته: collation دیتابیس SQL_Latin1_General_CP1256_CI_AS است، پس مقایسه
    //  حساس به بزرگی/کوچکی حروف نیست. اگر بعداً معلوم شد ERP حساس است، فقط همین
    //  عبارت به  sys_users.user_password = CONVERT(varbinary(50), ?)  تغییر می‌کند.
    //
    //  امنیت: رمز فقط پارامتر همین کوئری است — در اپ ذخیره نمی‌شود، لاگ نمی‌شود و
    //  در هیچ پیام خطایی چاپ نمی‌شود. (بستهٔ ورود SQL Server نیز در همان handshake
    //  رمزنگاری می‌شود، ولی توصیهٔ ما فعال بودن TLS روی اتصال است.)
    //
    //  IsLocked: نوع واقعی ستون bit NULL است و مقدار هر دو کاربر NULL (تست زندهٔ
    //  sql/07_login_verify.sql v2 روی سرور، 2026-09-18: V1|MATCH و V2=0).
    //  خودِ ERP هم در شرط ورود روی IsLocked فیلتر نمی‌کند (همان تست با همان شرط
    //  ERP ردیف را برگرداند). پس اینجا با CASE به صفر/یک تبدیل می‌شود تا اپ هرگز
    //  NULL را «قفل‌بودن» تفسیر نکند: تنها IsLocked = 1 یعنی قفل. صفر و NULL = باز.
    suspend fun login(username: String, password: String): DbLoginRow? =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT TOP (1)
                       sys_users.user_id,
                       sys_users.user_name,
                       sys_users.user_fname,
                       sys_users.user_lname,
                       sys_users.role_id,
                       sys_users.active,
                       CASE WHEN sys_users.IsLocked = 1 THEN 1 ELSE 0 END AS is_locked,
                       sys_users.shmo
                  FROM dbo.sys_users
                 WHERE sys_users.user_name = ?
                   AND CONVERT(varchar(50), sys_users.user_password) = ?
                   AND sys_users.active = 1
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.setString(1, username)
                ps.setString(2, password)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else DbLoginRow(
                        userId = rs.getInt("user_id"),
                        username = rs.getString("user_name") ?: username,
                        fullName = listOfNotNull(
                            rs.getString("user_fname"), rs.getString("user_lname")
                        ).joinToString(" "),
                        roleId = rs.nullableInt("role_id"),
                        active = rs.getBoolean("active"),
                        locked = rs.getBoolean("is_locked"),
                        companyId = rs.getInt("shmo"),
                    )
                }
            }
        }

    // ── کاتالوگ کالا + ۵ سطح قیمت + موجودی کل ────────────────────────────────
    //  برای ویزیتور غیرحرفه‌ای اپ فقط به قیمت‌های نقدی (forosh*) نیاز دارد؛
    //  ستون‌های mp*/pv* (تعداد/درصد) در فاز بعد در صورت نیاز استفاده می‌شوند.
    suspend fun products(limit: Int = 500, offset: Int = 0, search: String? = null): List<DbProduct> =
        db.withConnection { c ->
            val like = search?.let { "%$it%" }
            val sql = buildString {
                append(
                    """
                    SELECT inventory.shka, inventory.naka, inventory.coka, inventory.group_rdf,
                           inventory.vahsanj, inventory.mojkavah, inventory.mojkajoz,
                           inventory.vahsp, inventory.tedbastebandi, inventory.ExpirationDate,
                           forosh_price.forosh1, forosh_price.forosh2, forosh_price.forosh3,
                           forosh_price.forosh4, forosh_price.forosh5,
                           forosh_price.MinPrice, forosh_price.MaxPrice
                      FROM dbo.inventory
                      LEFT JOIN dbo.forosh_price ON forosh_price.shka = inventory.shka
                     WHERE inventory.active = ?
                    """.trimIndent()
                )
                if (like != null) append("\n   AND (inventory.naka LIKE ? OR inventory.coka LIKE ?)")
                append("\n     ORDER BY inventory.naka\n     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")
            }
            c.prepareStatement(sql).use { ps ->
                ps.queryTimeout = 30
                ps.setString(1, ACTIVE_CHAR)
                var i = 2
                if (like != null) { ps.setString(i++, like); ps.setString(i++, like) }
                ps.setInt(i++, offset)
                ps.setInt(i, limit)
                ps.executeQuery().use { rs -> rs.mapRows(::readProduct) }
            }
        }

    /**
     * قیمت یک کالا برای یک سطح (۱..۵) + بررسی بازهٔ مجاز Atiran.
     * `custgroup.price` تیر مشتری است؛ قیمت از همان ستون forosh<n> خوانده می‌شود.
     * هیچ قیمت پیش‌فرض/ساختگی‌ای اینجا وجود ندارد: اگر ردیف قیمت نباشد → null.
     */
    suspend fun priceFor(shka: Long, tier: Int): Long? {
        val column = when (tier.coerceIn(1, 5)) {
            1 -> "forosh1"; 2 -> "forosh2"; 3 -> "forosh3"; 4 -> "forosh4"; else -> "forosh5"
        }
        return db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT forosh_price.$column
                  FROM dbo.forosh_price
                 WHERE forosh_price.shka = ?
                   AND forosh_price.active = ?
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.setLong(1, shka)
                ps.setString(2, ACTIVE_CHAR)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else null }
            }
        }
    }

    // ── موجودی انبارها (از ویو خودِ ERP: موجودی منهای پیش‌فاکتورهای باز) ─────
    //  عدد mojkavah/mojkajoz روی کاغذ است؛ MojodiPish_vah/MojodiPish_joz همان
    //  چیزی است که ERP به عنوان موجودی قابل فروش نشان می‌دهد (سطرهای
    //  subsailfact_pish با sh_f = 0 و active = 't' و Rejected = 0 کم شده‌اند).
    suspend fun stock(shka: Long? = null): List<DbStockRow> =
        db.withConnection { c ->
            val sql = buildString {
                append(
                    """
                    SELECT vz.shka, vz.rdf_anbars, vz.name,
                           vz.mojkavah, vz.mojkajoz,
                           vz.MojodiPish_vah, vz.MojodiPish_joz
                      FROM dbo.VW_InventoryAnbars vz
                     WHERE (? = 0 OR vz.shka = ?)
                     ORDER BY vz.name
                    """.trimIndent()
                )
            }
            c.prepareStatement(sql).use { ps ->
                ps.queryTimeout = 30
                ps.setLong(1, shka ?: 0L)
                ps.setLong(2, shka ?: 0L)
                ps.executeQuery().use { rs ->
                    rs.mapRows {
                        DbStockRow(
                            shka = it.getLong("shka"),
                            warehouseRdf = it.getInt("rdf_anbars"),
                            warehouseName = it.getString("name") ?: "",
                            quantity = it.getDouble("mojkavah"),
                            quantityPiece = it.getInt("mojkajoz"),
                            availableQuantity = it.getDouble("MojodiPish_vah"),
                            availablePiece = it.getInt("MojodiPish_joz"),
                        )
                    }
                }
            }
        }

    // ── مشتریان مجاز یک ویزیتور (فیلتر واقعی جدول sys_cus) ───────────────────
    //  sys_cus: SysID + Shmo + UserID  → شرکت، مشتری مجاز، کاربر
    suspend fun customersFor(userId: Int, companyId: Int?, limit: Int = 500, offset: Int = 0): List<DbCustomer> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT CUSTOMERS.SHMO, CUSTOMERS.MONAME, CUSTOMERS.code, CUSTOMERS.group_rdf,
                       custgroup.group_name, CUSTOMERS.rdf_city, CUSTOMERS.addre,
                       CUSTOMERS.cell, CUSTOMERS.cred, CUSTOMERS.man, CUSTOMERS.black_list,
                       CUSTOMERS.active, CUSTOMERS.Lat, CUSTOMERS.Lng, CUSTOMERS.vis_rdf
                  FROM dbo.sys_cus
                  JOIN dbo.CUSTOMERS ON CUSTOMERS.SHMO = sys_cus.Shmo
                  LEFT JOIN dbo.custgroup ON custgroup.group_rdf = CUSTOMERS.group_rdf
                 WHERE sys_cus.UserID = ?
                   AND (? = 0 OR sys_cus.SysID = ?)
                   AND CUSTOMERS.active = ?
                 ORDER BY CUSTOMERS.MONAME
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                ps.setInt(1, userId)
                ps.setInt(2, companyId ?: 0)
                ps.setInt(3, companyId ?: 0)
                ps.setString(4, ACTIVE_CHAR)
                ps.setInt(5, offset)
                ps.setInt(6, limit)
                ps.executeQuery().use { rs -> rs.mapRows(::readCustomer) }
            }
        }

    /** گروه‌های مشتری + تیر قیمت (پایهٔ کل منطق قیمت Atiran). */
    /** گروه‌های کالا (dbo.kagroup: group_rdf, group_name) — برای نام گروه هر کالا. */
    suspend fun productGroups(): List<DbProductGroup> =
        db.withConnection { c ->
            c.prepareStatement(
                "SELECT group_rdf, group_name FROM dbo.kagroup ORDER BY group_name"
            ).use { ps ->
                ps.queryTimeout = 20
                ps.executeQuery().use { rs ->
                    val out = ArrayList<DbProductGroup>()
                    while (rs.next()) {
                        out += DbProductGroup(
                            groupRdf = rs.getInt("group_rdf"),
                            name = rs.getString("group_name")?.trim().orEmpty(),
                        )
                    }
                    out
                }
            }
        }

    /**
     * فاکتورها و پیش‌فاکتورهای یک ویزیتور (برای بخش گزارش‌ها).
     * فیلترها: vis_rdf = کد ویزیتور، و در صورت مشخص بودن شرکت: sysid.
     * مرتب‌سازی و محدودسازی در همین تابع (Kotlin) انجام می‌شود تا هیچ تبدیل نوعی
     * روی ستون‌های تاریخ/مبلغ (که نوع دقیقشان نامعلوم است) صورت نگیرد.
     */
    suspend fun invoicesForVisitor(
        visitorRdf: Int,
        companyId: Int? = null,
        limit: Int = 200,
    ): List<DbInvoiceRow> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT p.shfacfo, p.date, p.shmo, p.all, p.tafif, 1 AS is_pre
                  FROM dbo.sailfact_pish AS p
                 WHERE p.vis_rdf = ? AND (? = 0 OR p.sysid = ?)
                UNION ALL
                SELECT f.shfacfo, f.date, f.shmo, f.all, f.tafif, 0 AS is_pre
                  FROM dbo.sailfact AS f
                 WHERE f.vis_rdf = ? AND (? = 0 OR f.sysid = ?)
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                val company = companyId ?: 0
                ps.setInt(1, visitorRdf); ps.setInt(2, company); ps.setInt(3, company)
                ps.setInt(4, visitorRdf); ps.setInt(5, company); ps.setInt(6, company)
                ps.executeQuery().use { rs ->
                    val out = ArrayList<DbInvoiceRow>()
                    while (rs.next()) {
                        out += DbInvoiceRow(
                            number = rs.stringLoose("shfacfo"),
                            dateText = rs.stringLoose("date"),
                            customerCode = rs.stringLoose("shmo"),
                            total = rs.longLoose("all"),
                            discount = rs.longLoose("tafif"),
                            isPreInvoice = rs.intLoose("is_pre") == 1,
                        )
                    }
                    out.sortedByDescending { it.dateText }
                        .take(limit.coerceIn(1, 1000))
                }
            }
        }

    suspend fun customerGroups(): List<DbCustomerGroup> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT custgroup.group_rdf, custgroup.group_name, custgroup.price
                  FROM dbo.custgroup
                 WHERE custgroup.Active = 1        -- bit column (verified)
                 ORDER BY custgroup.group_name
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 20
                ps.executeQuery().use { rs ->
                    rs.mapRows {
                        DbCustomerGroup(
                            groupRdf = it.getInt("group_rdf"),
                            name = it.getString("group_name") ?: "",
                            priceTier = it.nullableInt("price"),
                        )
                    }
                }
            }
        }

    // ── هویت ویزیتور + دامنهٔ دسترسی (sys_vis / sys_cus / sys_kal / sys_anb) ──
    suspend fun visitorIdentity(userId: Int, companyId: Int? = null): DbVisitorIdentity? =
        db.withConnection { c ->
            //  نکتهٔ مهم (تأییدشده با دادهٔ واقعی): visitors.UserID روی این سرور NULL است،
            //  پس اتصال کاربر به ویزیتور از طریق sys_vis است:
            //      sys_users.user_id -> sys_vis.UserID -> sys_vis.shvis -> visitors.vis_rdf
            c.prepareStatement(
                """
                SELECT TOP (1)
                       visitors.vis_rdf, visitors.vis_name, visitors.active,
                       visitors.VIs_region, visitors.vis_city,
                       (SELECT COUNT(*) FROM dbo.sys_cus WHERE sys_cus.UserID = sys_vis.UserID) AS allowed_customers,
                       (SELECT COUNT(*) FROM dbo.sys_kal WHERE sys_kal.UserID = sys_vis.UserID) AS allowed_products,
                       (SELECT COUNT(*) FROM dbo.sys_anb WHERE sys_anb.UserID = sys_vis.UserID) AS allowed_warehouses
                  FROM dbo.sys_vis
                  JOIN dbo.visitors ON visitors.vis_rdf = sys_vis.shvis
                 WHERE sys_vis.UserID = ?
                   AND (? = 0 OR sys_vis.SysID = ?)
                 ORDER BY visitors.vis_rdf
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 20
                ps.setInt(1, userId)
                ps.setInt(2, companyId ?: 0)
                ps.setInt(3, companyId ?: 0)
                ps.executeQuery().use { rs ->
                    if (!rs.next()) null else DbVisitorIdentity(
                        userId = userId,
                        username = "",
                        displayName = rs.getString("vis_name") ?: "",
                        visitorRdf = rs.nullableInt("vis_rdf"),
                        allowedCustomers = rs.getInt("allowed_customers"),
                        allowedProducts = rs.getInt("allowed_products"),
                        allowedWarehouses = rs.getInt("allowed_warehouses"),
                    )
                }
            }
        }

    // ── سلامت/متادیتا: نسخهٔ دیتابیس برای نمایش در تنظیمات ───────────────────
    suspend fun databaseInfo(): Triple<String, String, String> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT DB_NAME() AS db_name,
                       CAST(SERVERPROPERTY('ProductVersion') AS NVARCHAR(64)) AS version,
                       (SELECT COUNT(*) FROM dbo.CUSTOMERS) AS customers
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.executeQuery().use { rs ->
                    if (!rs.next()) Triple("", "", "") else Triple(
                        rs.getString("db_name") ?: "",
                        rs.getString("version") ?: "",
                        rs.getString("customers") ?: "0",
                    )
                }
            }
        }

    // ══════════════════════════════════════════════════════════════════════════
    //  مسیر نوشتن پیش‌فاکتور — عیناً همان مسیری که خودِ ERP می‌رود
    //  شواهد (verbatim روی سرور):
    //    out_13_trigger_bodies.txt  → متن دو تریگر و ListPishFactor
    //    out_14_columns_and_samples.txt → ۳۴ ستون جدول میانی + دادهٔ واقعی فاکتورها
    //    docs/write-path/ERP-WRITE-PROCEDURES.md §1 → امضای دقیق add_sail_pish
    //    docs/write-path/IMPLEMENTATION-pre-invoice.md → تحلیل کامل
    //  • سربرگ : EXEC dbo.add_sail_pish ... @id_en = شمارهٔ جدید پیش‌فاکتور
    //  • اقلام  : INSERT در dbo.subsailtemp_pish با mod = 1؛ تریگر trig_sst_pish
    //            (INSTEAD OF INSERT) آن را در dbo.subsailfact_pish می‌نویسد و
    //            rdf__ سربرگِ زنده را روی سطر می‌گذارد.
    //  • هیچ تراکنش بیرونی: هم پروسیجر و هم تریگر «transaction forosh» خودشان را دارند.
    //  • هیچ رشته‌ای به SQL چسبانده نمی‌شود؛ همه پارامترند.
    // ══════════════════════════════════════════════════════════════════════════

    /** ستون‌هایی که اپ در جدول میانیِ پیش‌فاکتور می‌نویسد (همان‌ها که تریگر می‌خواند). */
    private val stagingColumns = listOf(
        "mod", "shfacfo", "rdf__", "rdf", "shka", "rdf_anbar", "tedvah", "tedjoz",
        "vahprice", "jozprice", "bastebandi", "tedbastebandi", "linesum", "isret",
        "pertafif", "pervis", "litakhma", "active", "amani", "Pavarez", "Avarez",
        "Ptax", "Tax", "PerPromotion", "modpar",
    )

    /** سربرگ پیش‌فاکتور — به‌ترتیبِ همان پارامترهای واقعی dbo.add_sail_pish. */
    data class DbPreInvoiceHead(
        val shmo: Int,            // مشتری
        val visRdf: Int,          // ویزیتور (dbo.visitors.vis_rdf)
        val userName: String,     // user__ سربرگ (نام کاربری ERP)
        val date: String,         // تاریخ شمسی به قالب ERP: 1405/06/27
        val doneDate: String,     // در دادهٔ واقعی ERP برابر همان date است
        val sumLineAll: Long,     // جمع اقلام پیش از تخفیف
        val finalAmount: Long,    // مبلغ نهایی (@all)
        val tafif: Long,          // تخفیف کل
        val jamTakhgh: Long,      // جمع تخفیف‌ها
        val tax: Long,            // مالیات (ERP در زمان فاکتور با Addmaliyat اعمال می‌کند)
        val avarez: Long,         // عوارض
        val barbari: Long,        // کرایهٔ حمل — در فاکتورهای واقعی صفر است
        val tozih: String,        // توضیح (shfacthand)
        val panevis: String,      // پی‌نویس — پیش‌فرض خودِ پروسیجر «ذکر نشده»
        val rdfSarbarg: Int,
        val rdfTahbarg: Int,      // در فاکتورهای واقعی 2
        val modpar: Int,          // در فاکتورهای واقعی 0
        val phKh: Int,
        val modDarsadVis: Int,
        val nahPar: Int,          // در فاکتورهای واقعی 1
        val tedRooZ: Int,         // مهلت اعتبار (روز) — مقدار درست از پیش‌فاکتور واقعی ERP
        val gainAll: Long,
        val sysId: Int = 1,
    )

    /** یک قلم پیش‌فاکتور — فقط ستون‌هایی که تریگر از سطر میانی می‌خواند. */
    data class DbPreInvoiceLine(
        val shka: Long,               // کالا
        val rdfAnbar: Int,            // انبار
        val tedVah: Double,           // تعداد بسته (decimal(18,3))
        val tedJoz: Int,              // تعداد جزیی
        val vahPrice: Long,           // قیمت بسته
        val jozPrice: Long,           // قیمت واحد
        val lineSum: Long,            // جمع سطر = tedvah*vahprice + tedjoz*jozprice
        val rdf: Int,                 // شمارهٔ سطر — در فاکتورهای واقعی از صفر
        val perTafif: Double = 0.0,
        val perVis: Double = 0.0,
        val litaKhma: Long = 0,
        val pTax: Double = 0.0,
        val pAvarez: Double = 0.0,
        val tax: Long = 0,
        val avarez: Long = 0,
        val perPromotion: Double = 0.0,
        val modpar: Int = 0,
        val bastebandi: String = "--",  // value: در دادهٔ واقعی ERP همین است
        val tedBastebandi: Int = 0,
        val amani: Int = 0,
        val isRet: String = "0",        // value: add_sail_pish خودش در سربرگ 0 می‌گذارد
        val active: String = "t",       // value: dbo.subsailfact_pish.active
    )

    /** نتیجهٔ ثبت: شمارهٔ پیش‌فاکتور + تعداد اقلام نوشته‌شده (+ خطا، اگر بود). */
    data class DbPreInvoiceWriteResult(
        val shfacfo: Long,
        val linesWritten: Int,
        val error: Exception? = null,
    ) {
        val ok: Boolean get() = error == null && linesWritten > 0
    }

    /** یک سطر از اقلامِ نوشته‌شده (برای تأیید بعد از ثبت). */
    data class DbPreInvoiceLineRow(
        val rdf: Int,
        val shka: Long,
        val tedVah: Double,
        val tedJoz: Int,
        val vahPrice: Long,
        val jozPrice: Long,
        val lineSum: Long,
        val litaKhma: Long,
        val perTafif: Double,
        val tax: Long,
        val avarez: Long,
        val active: String,
    )

    /**
     * پیش‌بررسی: هم وجود اشیا و هم وجود همان ستون‌هایی که اپ می‌نویسد.
     * روی دیتابیسی که نسخهٔ دیگری از ERP است، این تابع پیش از هر نوشتنی
     * «چه چیزی کم است» را دقیق می‌گوید - به‌جای خطای مبهم وسط کار.
     */
    suspend fun preInvoiceHealth(): Map<String, Boolean> =
        db.withConnection { c ->
            val out = LinkedHashMap<String, Boolean>()
            c.prepareStatement(
                """
                SELECT OBJECT_ID(N'dbo.add_sail_pish')    AS add_sail_pish,
                       OBJECT_ID(N'dbo.sailfact_pish')    AS head,
                       OBJECT_ID(N'dbo.subsailfact_pish') AS lines,
                       OBJECT_ID(N'dbo.subsailtemp_pish') AS staging,
                       OBJECT_ID(N'dbo.trig_sst_pish')    AS trig_pish
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.executeQuery().use { rs ->
                    if (rs.next()) {
                        out["add_sail_pish"] = rs.getInt("add_sail_pish") != 0
                        out["sailfact_pish"] = rs.getInt("head") != 0
                        out["subsailfact_pish"] = rs.getInt("lines") != 0
                        out["subsailtemp_pish"] = rs.getInt("staging") != 0
                        out["trig_sst_pish"] = rs.getInt("trig_pish") != 0
                    }
                }
            }
            if (out["subsailtemp_pish"] == true) {
                val present = HashSet<String>()
                c.prepareStatement(
                    """
                    SELECT c.name AS column_name
                      FROM sys.columns AS c
                     WHERE c.object_id = OBJECT_ID(N'dbo.subsailtemp_pish')
                    """.trimIndent()
                ).use { ps ->
                    ps.queryTimeout = 15
                    ps.executeQuery().use { rs ->
                        while (rs.next()) present.add(rs.getString("column_name").lowercase())
                    }
                }
                for (col in stagingColumns) out["column:$col"] = present.contains(col.lowercase())
            }
            out
        }

    /**
     * ثبت پیش‌فاکتور: سربرگ و بعد اقلام.
     * این تابع تراکنش بیرونی باز نمی‌کند (پروسیجر و تریگر تراکنش خودشان را دارند).
     * اگر سطر i خطا بدهد، متوقف می‌شود و شمارهٔ پیش‌فاکتورِ ساخته‌شده را برمی‌گرداند
     * تا لایهٔ بالاتر تصمیم بگیرد (retry یا retirePreInvoice).
     */
    suspend fun createPreInvoice(
        head: DbPreInvoiceHead,
        lines: List<DbPreInvoiceLine>,
    ): DbPreInvoiceWriteResult {
        require(lines.isNotEmpty()) { "pre-invoice without lines" }

        val missing = preInvoiceHealth().filterValues { !it }.keys
        if (missing.isNotEmpty()) {
            throw IllegalStateException("ERP objects/columns missing on this database: $missing")
        }

        val shfacfo = db.withConnection { c -> insertPreInvoiceHead(c, head) }
        val version = db.withConnection { c -> liveHeadVersion(c, shfacfo) }

        var written = 0
        for (line in lines) {
            try {
                db.withConnection { c -> insertPreInvoiceLine(c, shfacfo, version, line) }
                written++
            } catch (e: Exception) {
                return DbPreInvoiceWriteResult(shfacfo, written, e)
            }
        }
        return DbPreInvoiceWriteResult(shfacfo, written, null)
    }

    /** قرائت اقلامِ یک پیش‌فاکتور با جوین درست (shfacfo + rdf__) — برای تأیید پس از ثبت. */
    suspend fun preInvoiceLines(shfacfo: Long): List<DbPreInvoiceLineRow> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT p.RDF AS rdf, p.SHKA AS shka, p.TEDVAH AS tedvah, p.TEDJOZ AS tedjoz,
                       p.VAHPRICE AS vahprice, p.JOZPRICE AS jozprice, p.LINESUM AS linesum,
                       p.litakhma AS litakhma, p.PERTAFIF AS pertafif,
                       p.Tax AS tax, p.Avarez AS avarez, p.active AS active
                  FROM dbo.subsailfact_pish AS p
                  JOIN dbo.sailfact_pish AS h ON h.shfacfo = p.shfacfo AND h.rdf__ = p.rdf__
                 WHERE p.shfacfo = ? AND h.active = 't' AND p.active = 't'
                 ORDER BY p.RDF
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                ps.setLong(1, shfacfo)
                ps.executeQuery().use { rs ->
                    rs.mapRows {
                        DbPreInvoiceLineRow(
                            rdf = it.getInt("rdf"),
                            shka = it.getLong("shka"),
                            tedVah = it.getDouble("tedvah"),
                            tedJoz = it.getInt("tedjoz"),
                            vahPrice = it.getLong("vahprice"),
                            jozPrice = it.getLong("jozprice"),
                            lineSum = it.getLong("linesum"),
                            litaKhma = it.getLong("litakhma"),
                            perTafif = it.getDouble("pertafif"),
                            tax = it.getLong("tax"),
                            avarez = it.getLong("avarez"),
                            active = it.getString("active") ?: "",
                        )
                    }
                }
            }
        }

    /**
     * کنار گذاشتن یک پیش‌فاکتور نیمه‌کاره — دقیقاً همان کاری که Edit_sail_pish با
     * نسخهٔ قبلی می‌کند: active='f' و ismodify='t' (سند حذف نمی‌شود، فقط از دیدِ
     * لیست‌های ERP - که active='t' فیلتر می‌کنند - خارج می‌شود).
     * ⚠️ استفاده از این تابع باید توسط کارفرما تأیید شود (سؤال باز در گزارش نهایی).
     */
    suspend fun retirePreInvoice(shfacfo: Long): Int =
        db.withConnection { c ->
            c.prepareStatement(
                """
                UPDATE dbo.sailfact_pish
                   SET active = 'f', ismodify = 't'
                 WHERE shfacfo = ? AND active = 't'
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                ps.setLong(1, shfacfo)
                ps.executeUpdate()
            }
        }

    private fun insertPreInvoiceHead(c: java.sql.Connection, h: DbPreInvoiceHead): Long {
        //  فراخوانی با prepareCall (سینتکس استاندارد JDBC) و پارامترهای «موضعی»:
        //  ترتیب دقیقاً همان ترتیبِ اعلانِ پارامترهای dbo.add_sail_pish است، پس
        //  هیچ پارامتری جابه‌جا نمی‌شود و نامی هم داخل SQL نوشته نمی‌شود.
        //    1 @date          10 @jamtakhgh     19 @mod_darsad_vis
        //    2 @shmo          11 @done_date     20 @nah_par
        //    3 @barbari       12 @user          21 @sh_fac  = 0  (پروسیجر استفاده نمی‌کند)
        //    4 @tozih         13 @panevis       22 @id_en   OUTPUT = شمارهٔ پیش‌فاکتور
        //    5 @vis_rdf       14 @rdf_sarbarg   23 @ted_rooz
        //    6 @sumlineall    15 @rdf_tahbarg   24 @sysid
        //    7 @all           16 @modpar        25 @tax
        //    8 @gainall       17 @ph_kh         26 @avarez
        //    9 @tafif         18 @mod = 1
        c.prepareCall(
            "{call dbo.add_sail_pish(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}"
        ).use { ps ->
            ps.queryTimeout = 60
            ps.setString(1, h.date)
            ps.setInt(2, h.shmo)
            ps.setLong(3, h.barbari)
            ps.setString(4, h.tozih)
            ps.setInt(5, h.visRdf)
            ps.setLong(6, h.sumLineAll)
            ps.setLong(7, h.finalAmount)
            ps.setLong(8, h.gainAll)
            ps.setLong(9, h.tafif)
            ps.setLong(10, h.jamTakhgh)
            ps.setString(11, h.doneDate)
            ps.setString(12, h.userName)
            ps.setString(13, h.panevis)
            ps.setInt(14, h.rdfSarbarg)
            ps.setInt(15, h.rdfTahbarg)
            ps.setInt(16, h.modpar)
            ps.setInt(17, h.phKh)
            ps.setInt(18, 1)                 // @mod = 1 → پروسیجر واقعاً درج می‌کند
            ps.setInt(19, h.modDarsadVis)
            ps.setInt(20, h.nahPar)
            ps.setLong(21, 0)                // @sh_fac (بدون استفاده در بدنه)
            ps.registerOutParameter(22, java.sql.Types.BIGINT)
            ps.setInt(23, h.tedRooZ)
            ps.setInt(24, h.sysId)
            ps.setLong(25, h.tax)
            ps.setLong(26, h.avarez)
            ps.execute()
            return ps.getLong(22)
        }
    }

    /** نسخهٔ سربرگِ زنده (rdf__) — تریگر با mod=1 همین را روی سطر می‌گذارد. */
    private fun liveHeadVersion(c: java.sql.Connection, shfacfo: Long): Int =
        c.prepareStatement(
            """
            SELECT MAX(h.rdf__) AS version
              FROM dbo.sailfact_pish AS h
             WHERE h.shfacfo = ? AND h.active = 't'
            """.trimIndent()
        ).use { ps ->
            ps.queryTimeout = 15
            ps.setLong(1, shfacfo)
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("version") else 0 }
        }

    private fun insertPreInvoiceLine(
        c: java.sql.Connection,
        shfacfo: Long,
        version: Int,
        l: DbPreInvoiceLine,
    ) {
        c.prepareStatement(
            """
            INSERT INTO dbo.subsailtemp_pish
                (mod, shfacfo, rdf__, rdf, shka, rdf_anbar, tedvah, tedjoz, vahprice, jozprice,
                 bastebandi, tedbastebandi, linesum, isret, pertafif, pervis, litakhma, active,
                 amani, Pavarez, Avarez, Ptax, Tax, PerPromotion, modpar)
            VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { ps ->
            ps.queryTimeout = 60
            ps.setLong(1, shfacfo)
            ps.setInt(2, version)
            ps.setInt(3, l.rdf)
            ps.setLong(4, l.shka)
            ps.setInt(5, l.rdfAnbar)
            ps.setDouble(6, l.tedVah)
            ps.setInt(7, l.tedJoz)
            ps.setLong(8, l.vahPrice)
            ps.setLong(9, l.jozPrice)
            ps.setString(10, l.bastebandi)
            ps.setInt(11, l.tedBastebandi)
            ps.setLong(12, l.lineSum)
            ps.setString(13, l.isRet)
            ps.setDouble(14, l.perTafif)
            ps.setDouble(15, l.perVis)
            ps.setLong(16, l.litaKhma)
            ps.setString(17, l.active)
            ps.setInt(18, l.amani)
            ps.setDouble(19, l.pAvarez)
            ps.setLong(20, l.avarez)
            ps.setDouble(21, l.pTax)
            ps.setLong(22, l.tax)
            ps.setDouble(23, l.perPromotion)
            ps.setInt(24, l.modpar)
            ps.executeUpdate()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private companion object {
        /**
         * مقادیر «فعال» — با خروجی واقعی سرور تأیید شده‌اند (بخش G3 ممیزی):
         *   ستون‌های char(1) این ERP مقدار 't' (فعال) و 'f' (غیرفعال) دارند،
         *   نه '1'. ستون‌های bit با 1/0 مقایسه می‌شوند.
         * ابزار tools/check_sql_columns.py این دو مقدار را با فایل
         * docs/schema/meelano-values.tsv تطبیق می‌دهد.
         */
        const val ACTIVE_CHAR = "t"   // value: dbo.inventory.active
        const val ACTIVE_BIT = 1      // value: dbo.kagroup.Active
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  کمک‌تابع‌های ResultSet (مستقل از هر ORM)
// ─────────────────────────────────────────────────────────────────────────────
private inline fun <T> ResultSet.mapRows(read: (ResultSet) -> T): List<T> {
    val out = ArrayList<T>()
    while (next()) out.add(read(this))
    return out
}

private fun ResultSet.nullableInt(column: String): Int? {
    val v = getInt(column)
    return if (wasNull()) null else v
}

/** خواندن مقدار به‌صورت متن، بدون توجه به نوع واقعی ستون (varchar/int/date/money). */
private fun ResultSet.stringLoose(column: String): String = try {
    (getObject(column) ?: "").toString().trim()
} catch (_: Exception) {
    ""
}

/** خواندن مقدار عددی با تحمل خطا: ستون متنی، اعشاری یا پولی همه پذیرفته می‌شوند. */
private fun ResultSet.longLoose(column: String): Long = try {
    getLong(column)
} catch (_: Exception) {
    val digits = stringLoose(column).filter { it.isDigit() || it == '-' || it == '.' }
    val asDouble = digits.toDoubleOrNull() ?: 0.0
    asDouble.toLong()
}

private fun ResultSet.intLoose(column: String): Int = longLoose(column).toInt()

private fun readProduct(rs: ResultSet) = DbProduct(
    shka = rs.getLong("shka"),
    name = rs.getString("naka") ?: "",
    code = rs.getString("coka") ?: "",
    groupRdf = rs.getInt("group_rdf"),
    unit = rs.getString("vahsanj") ?: "",
    stockVah = rs.getDouble("mojkavah"),
    stockJoz = rs.getInt("mojkajoz"),
    pieceWeight = rs.getDouble("vahsp"),
    packSize = rs.getDouble("tedbastebandi"),
    expirationDate = rs.getString("ExpirationDate"),
    priceTier1 = rs.getLong("forosh1"),
    priceTier2 = rs.getLong("forosh2"),
    priceTier3 = rs.getLong("forosh3"),
    priceTier4 = rs.getLong("forosh4"),
    priceTier5 = rs.getLong("forosh5"),
    minPrice = rs.getLong("MinPrice"),
    maxPrice = rs.getLong("MaxPrice"),
)

private fun readCustomer(rs: ResultSet) = DbCustomer(
    shmo = rs.getInt("SHMO"),
    name = rs.getString("MONAME") ?: "",
    code = rs.getString("code") ?: "",
    groupRdf = rs.getInt("group_rdf"),
    groupName = rs.getString("group_name"),
    cityRdf = rs.nullableInt("rdf_city"),
    address = rs.getString("addre") ?: "",
    phone = rs.getString("cell") ?: "",
    credit = rs.getLong("cred"),
    debt = rs.getLong("man"),
    blackList = rs.nullableInt("black_list"),
    active = rs.getString("active") ?: "",
    lat = rs.getDouble("Lat").takeIf { !rs.wasNull() },
    lng = rs.getDouble("Lng").takeIf { !rs.wasNull() },
    visitorRdf = rs.getInt("vis_rdf"),
)

/** مقادیر پیش‌فرض پیکربندی که با ممیزی سرور تأیید شده‌اند. */
object MeelanoDefaults {
    const val HOST = "192.168.1.150"      // LAN IP واقعی سرور (تأیید کاربر)
    const val PORT = 1433                  // sys.dm_tcp_listener_states: 0.0.0.0:1433
    const val DATABASE = "Meelano"         // DB_NAME() روی سرور
    const val APP_USER = "vizitor_android" // کاربر فقط‌خواندنی که اسکریپت 01 می‌سازد

    fun toServerConfig(cfg: ServerConfig): ServerConfig =
        cfg.copy(serverIp = HOST, dbPort = PORT)
}
