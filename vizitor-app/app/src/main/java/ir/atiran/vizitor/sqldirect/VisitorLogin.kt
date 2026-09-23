/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ورود ویزیتور از جدول واقعی dbo.sys_vis
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرا این فایل؟
 *    تا پیش از این، ورود نیازمند «نام کاربری و کلمهٔ عبور کاربر آتیران»
 *    (dbo.sys_users) بود. حالا فهرست ویزیتورهای واقعی از جدول dbo.sys_vis
 *    خوانده می‌شود و کاربر فقط ویزیتور خودش را انتخاب می‌کند — بدون هیچ
 *    پرس‌وجوی نام کاربری/رمز اولیه.
 *
 *  قرارداد ستون‌ها (از ممیزی واقعی سرور، بدون حدس):
 *      dbo.sys_vis  : rdf, SysID, shvis, UserID
 *                     · UserID → کلید جدول‌های مجوز (sys_cus / sys_kal / sys_anb)
 *                     · SysID  → شرکت (هم‌معنی sys_users.shmo)
 *                     · shvis  → dbo.visitors.vis_rdf (کلید فاکتورها/پیش‌فاکتورها)
 *      dbo.visitors : vis_rdf, vis_name, vis_cell, vis_addre, VIs_region,
 *                     vis_city, active, is_supervisor, eteb, ...
 *
 *  امنیت: هیچ ستون رمزی خوانده نمی‌شود (Password جدول visitors هرگز خوانده
 *  نمی‌شود) و همهٔ کوئری‌ها پارامتری و فقط SELECT هستند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

/** یک ویزیتور قابل انتخاب برای ورود (یک ردیف واقعی sys_vis ⋈ visitors). */
data class VisitorOption(
    /** dbo.sys_vis.UserID — کلید جدول‌های مجوز (sys_cus/sys_kal/sys_anb). */
    val userId: Int,
    /** dbo.sys_vis.SysID — شرکت (برای فیلتر SysID در کوئری‌ها). */
    val companyId: Int,
    /** dbo.sys_vis.shvis = dbo.visitors.vis_rdf — کلید فاکتورها. */
    val visitorRdf: Int,
    val name: String,
    val cell: String,
    val region: Int?,
    val city: Int?,
    /** char(1): 't' فعال، 'f' غیرفعال. */
    val active: String,
    val allowedCustomers: Int,
    val allowedProducts: Int,
    val allowedWarehouses: Int,
) {
    val isActive: Boolean get() = !active.equals("f", true)
    val label: String get() = name.ifBlank { "ویزیتور #$visitorRdf" }

    /** خط دوم کارت انتخاب ویزیتور. */
    val subtitle: String
        get() = buildList {
            if (cell.isNotBlank()) add(cell)
            add("کد ویزیتور: $visitorRdf")
            if (allowedCustomers > 0) add("$allowedCustomers مشتری مجاز")
            if (!isActive) add("غیرفعال")
        }.joinToString(" • ")
}

/** خواندن اختیاری عدد صحیح از یک ستون (اگر NULL یا نوع ناجور بود ⇒ null). */
private fun java.sql.ResultSet.nullableIntValue(column: String): Int? = try {
    val v = getInt(column)
    if (wasNull()) null else v
} catch (_: Throwable) {
    try {
        val t = getString(column)?.trim()
        if (t.isNullOrEmpty()) null else t.toInt()
    } catch (_: Throwable) {
        null
    }
}

object VisitorLoginRepository {

    private val db get() = SqlConnectionManager

    /**
     * فهرست ویزیتورهایی که می‌توان با آن‌ها وارد شد.
     *
     * @param companyId اگر > ۰ باشد فقط ویزیتورهای همان شرکت (sys_vis.SysID) می‌آیند.
     *                  صفر یعنی «همهٔ شرکت‌ها» (وقتی هنوز شرکت انتخاب نشده).
     * @param limit     سقف تعداد رکورد (TOP با پارامتر — امن).
     */
    suspend fun options(companyId: Int = 0, limit: Int = 500): List<VisitorOption> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT TOP (?)
                       sv.UserID, sv.SysID, sv.shvis,
                       v.vis_name, v.vis_cell, v.VIs_region, v.vis_city, v.active,
                       (SELECT COUNT(*) FROM dbo.sys_cus sc
                         WHERE sc.UserID = sv.UserID AND (? = 0 OR sc.SysID = ?)) AS allowed_customers,
                       (SELECT COUNT(*) FROM dbo.sys_kal sk
                         WHERE sk.UserID = sv.UserID AND (? = 0 OR sk.SysID = ?)) AS allowed_products,
                       (SELECT COUNT(*) FROM dbo.sys_anb sa
                         WHERE sa.UserID = sv.UserID AND (? = 0 OR sa.SysID = ?)) AS allowed_warehouses
                  FROM dbo.sys_vis AS sv
                  JOIN dbo.visitors AS v ON v.vis_rdf = sv.shvis
                 WHERE (? = 0 OR sv.SysID = ?)
                 ORDER BY CASE WHEN v.active = 'f' THEN 1 ELSE 0 END, v.vis_name
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                val company = companyId
                ps.setInt(1, limit.coerceIn(1, 2000))
                ps.setInt(2, company); ps.setInt(3, company)
                ps.setInt(4, company); ps.setInt(5, company)
                ps.setInt(6, company); ps.setInt(7, company)
                ps.setInt(8, company); ps.setInt(9, company)
                ps.executeQuery().use { rs ->
                    val out = ArrayList<VisitorOption>()
                    while (rs.next()) {
                        out += VisitorOption(
                            userId = rs.getInt("UserID"),
                            companyId = rs.getInt("SysID"),
                            visitorRdf = rs.getInt("shvis"),
                            name = rs.getString("vis_name")?.trim().orEmpty(),
                            cell = rs.getString("vis_cell")?.trim().orEmpty(),
                            region = rs.nullableIntValue("VIs_region"),
                            city = rs.nullableIntValue("vis_city"),
                            active = rs.getString("active")?.trim().orEmpty(),
                            allowedCustomers = rs.getInt("allowed_customers"),
                            allowedProducts = rs.getInt("allowed_products"),
                            allowedWarehouses = rs.getInt("allowed_warehouses"),
                        )
                    }
                    out
                }
            }
        }

    /** جست‌وجوی محلی در فهرست (نام یا موبایل یا کد) — بدون کوئری تازه به سرور. */
    fun search(options: List<VisitorOption>, query: String): List<VisitorOption> {
        val q = query.trim()
        if (q.isBlank()) return options
        return options.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.cell.contains(q) ||
                it.visitorRdf.toString() == q ||
                it.userId.toString() == q
        }
    }
}
