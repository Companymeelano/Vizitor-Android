/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | خواندن جدول و ستون‌های ویزیتورها از SQL Server
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل «قرارداد ستون‌ها» را همان‌طور که در سرور واقعی است می‌خواند:
 *
 *    dbo.sys_vis  : rdf, SysID, shvis, UserID          ← اتصال کاربر به ویزیتور
 *    dbo.visitors : vis_rdf, vis_name, vis_addre, vis_tell1, vis_tell2,
 *                   vis_cell, VIs_region, vis_city, active, eteb,
 *                   is_supervisor, per_p_d_naghd, per_p_d_check,
 *                   TedadFactorMojazMande, ...
 *
 *  و برای هر ویزیتور، دامنهٔ دسترسی او را از جدول‌های مجوز می‌شمارد:
 *    dbo.sys_cus (مشتریان) · dbo.sys_kal (کالاها) · dbo.sys_anb (انبارها)
 *
 *  نکات امنیتی:
 *   • هیچ‌گاه ستون Password جدول visitors خوانده نمی‌شود.
 *   • همهٔ کوئری‌ها پارامتری‌اند و فقط SELECT روی جدول‌های مجازِ کاربر
 *     محدود (db_datareader) اجرا می‌شوند — همان دسترسی‌ای که نصب‌کننده می‌دهد.
 *   • ساختار ستون‌ها از sys.columns خوانده می‌شود، نه حدس‌زده‌شده.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

/** یک ستون واقعی از جدول دیتابیس (نام، نوع، طول، تهی‌پذیر بودن). */
data class DirectColumn(
    val name: String,
    val typeName: String,
    val maxLength: Int,
    val nullable: Boolean,
)

/** یک ویزیتور با همان ستون‌هایی که اپ نمایش می‌دهد. */
data class DirectVisitorRow(
    val rdf: Int,
    val name: String,
    val cell: String,
    val tell: String,
    val address: String,
    val region: Int?,
    val city: Int?,
    val active: String,
    val supervisor: Boolean,
    val credit: Long?,
    val percentCash: Double?,
    val percentCheque: Double?,
    val allowedInvoicesLeft: Int?,
    val allowedCustomers: Int,
    val allowedProducts: Int,
    val allowedWarehouses: Int,
)

object VisitorRepository {

    private val db get() = SqlConnectionManager

    /**
     * ویزیتورهای مجاز یک کاربر ERP (نقشهٔ sys_users.user_id → sys_vis.UserID → visitors).
     * وقتی [companyId] داده شود، فقط رکوردهای همان شرکت برمی‌گردند.
     */
    suspend fun visitorsForUser(
        userId: Int,
        companyId: Int? = null,
        limit: Int = 200,
    ): List<DirectVisitorRow> = db.withConnection { c ->
        c.prepareStatement(
            """
            SELECT v.vis_rdf, v.vis_name, v.vis_cell, v.vis_tell1, v.vis_addre,
                   v.VIs_region, v.vis_city, v.active, v.is_supervisor, v.eteb,
                   v.per_p_d_naghd, v.per_p_d_check, v.TedadFactorMojazMande,
                   (SELECT COUNT(*) FROM dbo.sys_cus sc
                     WHERE sc.UserID = sv.UserID AND (? = 0 OR sc.SysID = ?)) AS allowed_customers,
                   (SELECT COUNT(*) FROM dbo.sys_kal sk
                     WHERE sk.UserID = sv.UserID AND (? = 0 OR sk.SysID = ?)) AS allowed_products,
                   (SELECT COUNT(*) FROM dbo.sys_anb sa
                     WHERE sa.UserID = sv.UserID AND (? = 0 OR sa.SysID = ?)) AS allowed_warehouses
              FROM dbo.sys_vis AS sv
              JOIN dbo.visitors AS v ON v.vis_rdf = sv.shvis
             WHERE sv.UserID = ?
               AND (? = 0 OR sv.SysID = ?)
             ORDER BY v.vis_name
             OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
            """.trimIndent()
        ).use { ps ->
            ps.queryTimeout = 30
            val company = companyId ?: 0
            ps.setInt(1, company); ps.setInt(2, company)
            ps.setInt(3, company); ps.setInt(4, company)
            ps.setInt(5, company); ps.setInt(6, company)
            ps.setInt(7, userId)
            ps.setInt(8, company); ps.setInt(9, company)
            ps.setInt(10, limit.coerceIn(1, 2000))
            ps.executeQuery().use { rs ->
                val out = ArrayList<DirectVisitorRow>()
                while (rs.next()) {
                    out += DirectVisitorRow(
                        rdf = rs.getInt("vis_rdf"),
                        name = rs.getString("vis_name")?.trim().orEmpty(),
                        cell = rs.getString("vis_cell")?.trim().orEmpty(),
                        tell = rs.getString("vis_tell1")?.trim().orEmpty(),
                        address = rs.getString("vis_addre")?.trim().orEmpty(),
                        region = rs.nullableIntValue("VIs_region"),
                        city = rs.nullableIntValue("vis_city"),
                        active = rs.getString("active")?.trim().orEmpty(),
                        supervisor = runCatching { rs.getBoolean("is_supervisor") }.getOrDefault(false),
                        credit = rs.nullableLongValue("eteb"),
                        percentCash = rs.nullableDoubleValue("per_p_d_naghd"),
                        percentCheque = rs.nullableDoubleValue("per_p_d_check"),
                        allowedInvoicesLeft = rs.nullableIntValue("TedadFactorMojazMande"),
                        allowedCustomers = rs.getInt("allowed_customers"),
                        allowedProducts = rs.getInt("allowed_products"),
                        allowedWarehouses = rs.getInt("allowed_warehouses"),
                    )
                }
                out
            }
        }
    }

    /**
     * فهرست واقعی ستون‌های dbo.visitors (نام/نوع/طول/تهی‌پذیری) —
     * از sys.columns خوانده می‌شود تا هیچ ستونی حدس زده نشود.
     * ستون‌های حساس (Password) نمایش داده نمی‌شوند.
     */
    suspend fun visitorsColumns(): List<DirectColumn> = db.withConnection { c ->
        c.prepareStatement(
            """
            SELECT col.name AS name, typ.name AS type_name,
                   col.max_length AS max_length, col.is_nullable AS is_nullable
              FROM sys.columns AS col
              JOIN sys.types AS typ ON typ.user_type_id = col.user_type_id
             WHERE col.object_id = OBJECT_ID(N'dbo.visitors')
             ORDER BY col.column_id
            """.trimIndent()
        ).use { ps ->
            ps.queryTimeout = 15
            ps.executeQuery().use { rs ->
                val out = ArrayList<DirectColumn>()
                while (rs.next()) {
                    val name = rs.getString("name")
                    if (name.equals("Password", ignoreCase = true)) continue   // هرگز نمایش داده نمی‌شود
                    out += DirectColumn(
                        name = name,
                        typeName = rs.getString("type_name") ?: "",
                        maxLength = rs.getInt("max_length"),
                        nullable = rs.getBoolean("is_nullable"),
                    )
                }
                out
            }
        }
    }

    /** شمارش کل ویزیتورهای دیتابیس (برای نشان دادن «چند ویزیتور در ERP هست»). */
    suspend fun visitorCount(): Int = db.withConnection { c ->
        c.prepareStatement("SELECT COUNT(*) AS n FROM dbo.visitors").use { ps ->
            ps.queryTimeout = 15
            ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("n") else 0 }
        }
    }
}

// ── کمک‌تابع‌های ResultSet (بدون هیچ ORM) ────────────────────────────────────
private fun java.sql.ResultSet.nullableIntValue(column: String): Int? {
    val v = getInt(column)
    return if (wasNull()) null else v
}

private fun java.sql.ResultSet.nullableLongValue(column: String): Long? {
    val v = getLong(column)
    return if (wasNull()) null else v
}

private fun java.sql.ResultSet.nullableDoubleValue(column: String): Double? {
    val v = getDouble(column)
    return if (wasNull()) null else v
}
