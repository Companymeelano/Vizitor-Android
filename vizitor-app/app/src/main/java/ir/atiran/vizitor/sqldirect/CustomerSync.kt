/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | همگام‌سازی مطمئن فهرست مشتریان (v2.16.0)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرا این فایل؟ چون «فهرست مشتریان همگام نمی‌شد» چند علت کاملاً متفاوت دارد و
 *  نمی‌شود با یک کوئری ثابت همه‌شان را پوشش داد:
 *    ۱) جدول مجوز dbo.sys_cus برای این کاربر خالی است.
 *    ۲) ستون CUSTOMERS.vis_rdf اصلاً در این نسخهٔ ERP پر نشده است.
 *    ۳) بعضی ستون‌های CUSTOMERS در این نسخه وجود ندارند/نامشان متفاوت است و
 *       کوئری ثابت با خطا برمی‌گردد (همین باعث می‌شد کل بخش مشتریان خالی بماند).
 *
 *  راه‌حل: سه مسیر پله‌ای + «سازگارسازی با ستون‌های واقعی همان سرور»
 *    · اول ستون‌های واقعی dbo.CUSTOMERS از sys.columns خوانده می‌شود (کش‌شده)،
 *      بعد کوئری فقط با همان ستون‌هایی ساخته می‌شود که روی این سرور وجود دارند.
 *    · مسیرها به‌ترتیب: sys_cus ← CUSTOMERS.vis_rdf ← همهٔ مشتریان فعال سامانه.
 *    · هیچ عدد/مشتری ساختگی ساخته نمی‌شود؛ فقط دادهٔ واقعی جداول ERP.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import ir.atiran.vizitor.data.local.CustomerEntity
import java.sql.ResultSet

object CustomerSync {

    private const val ACTIVE_CHAR = "t"
    private val db get() = SqlConnectionManager

    /** نتیجهٔ همگام‌سازی مشتریان — با «مسیر» و یادداشت‌های شفاف. */
    data class Result(
        val customers: List<CustomerEntity>,
        /** sys_cus | CUSTOMERS.vis_rdf | all_customers */
        val source: String,
        val notes: List<String> = emptyList(),
        val error: String? = null,
    ) {
        val count: Int get() = customers.size
        val sourceLabel: String
            get() = when (source) {
                "sys_cus" -> "مجوز کاربر (dbo.sys_cus)"
                "CUSTOMERS.vis_rdf" -> "مشتریان تخصیص‌یافته به ویزیتور (CUSTOMERS.vis_rdf)"
                "all_customers" -> "همهٔ مشتریان فعال سامانه (مسیر پشتیبان)"
                else -> source
            }
    }

    // ── کش ستون‌های واقعی جدول CUSTOMERS روی همین سرور ──────────────────────
    @Volatile private var columnCache: Set<String>? = null

    /**
     * ستون‌های تأییدشدهٔ dbo.CUSTOMERS (از ممیزی واقعی سرور) — فقط اگر خواندن
     * sys.columns به هر دلیلی ممکن نبود (مثلاً محدودیت دسترسی) استفاده می‌شود
     * تا نام مشتری و وضعیت فعال همیشه خوانده شود.
     */
    private val verifiedColumns = setOf(
        "shmo", "moname", "code", "group_rdf", "rdf_city", "addre", "cell", "tell1",
        "cred", "man", "black_list", "active", "lat", "lng", "vis_rdf",
    )

    suspend fun columns(force: Boolean = false): Set<String> {
        columnCache?.takeIf { !force }?.let { return it }
        val cols = runCatching {
            db.withConnection { c ->
                c.prepareStatement(
                    "SELECT name FROM sys.columns WHERE object_id = OBJECT_ID('dbo.CUSTOMERS')"
                ).use { ps ->
                    ps.queryTimeout = 15
                    ps.executeQuery().use { rs ->
                        val out = HashSet<String>()
                        while (rs.next()) { rs.getString(1)?.let { out += it.lowercase() } }
                        out
                    }
                }
            }
        }.getOrDefault(emptySet())
        if (cols.isNotEmpty()) columnCache = cols
        return cols
    }

    /** ستون‌های انتخابی + شرط‌ها بر اساس ستون‌های واقعی همین سرور ساخته می‌شوند. */
    private fun buildQuery(cols: Set<String>, filter: Filter): Prepared {
        val has = { name: String -> cols.contains(name.lowercase()) }
        val select = ArrayList<String>()
        select += "CUSTOMERS.SHMO AS SHMO"
        if (has("MONAME")) select += "CUSTOMERS.MONAME AS MONAME"
        if (has("code")) select += "CUSTOMERS.code AS code"
        if (has("group_rdf")) select += "CUSTOMERS.group_rdf AS group_rdf"
        if (has("group_name")) select += "CUSTOMERS.group_name AS group_name"
        if (has("rdf_city")) select += "CUSTOMERS.rdf_city AS rdf_city"
        if (has("city_name")) select += "CUSTOMERS.city_name AS city_name"
        if (has("addre")) select += "CUSTOMERS.addre AS addre"
        if (has("cell")) select += "CUSTOMERS.cell AS cell"
        if (has("tell1")) select += "CUSTOMERS.tell1 AS tell1"
        if (has("cred")) select += "CUSTOMERS.cred AS cred"
        if (has("man")) select += "CUSTOMERS.man AS man"
        if (has("black_list")) select += "CUSTOMERS.black_list AS black_list"
        if (has("active")) select += "CUSTOMERS.active AS active"
        if (has("Lat")) select += "CUSTOMERS.Lat AS Lat"
        if (has("Lng")) select += "CUSTOMERS.Lng AS Lng"
        if (has("vis_rdf")) select += "CUSTOMERS.vis_rdf AS vis_rdf"

        val where = ArrayList<String>()
        if (has("active")) where += "CUSTOMERS.active = ?"

        val from: String
        when (filter) {
            is Filter.SysCus -> {
                from = "dbo.sys_cus JOIN dbo.CUSTOMERS ON CUSTOMERS.SHMO = sys_cus.Shmo"
                where += "sys_cus.UserID = ?"
                where += "(? = 0 OR sys_cus.SysID = ?)"
            }
            is Filter.Visitor -> {
                from = "dbo.CUSTOMERS"
                if (has("vis_rdf")) where += "CUSTOMERS.vis_rdf = ?"
            }
            is Filter.All -> from = "dbo.CUSTOMERS"
        }

        val group = if (has("group_name")) "" else
            " LEFT JOIN dbo.custgroup ON custgroup.group_rdf = CUSTOMERS.group_rdf"
        val groupCol = if (has("group_name")) "" else ", custgroup.group_name AS group_name"
        val orderBy = if (has("MONAME")) " ORDER BY CUSTOMERS.MONAME" else " ORDER BY CUSTOMERS.SHMO"
        val sql = "SELECT TOP (?) ${select.joinToString(", ")}" +
            (if (groupCol.isNotEmpty()) groupCol else "") +
            " FROM $from$group" +
            (if (where.isEmpty()) "" else " WHERE " + where.joinToString(" AND ")) +
            orderBy
        return Prepared(sql, filter)
    }

    private sealed interface Filter {
        data class SysCus(val userId: Int, val companyId: Int?) : Filter
        data class Visitor(val visitorRdf: Int) : Filter
        data object All : Filter
    }

    private data class Prepared(val sql: String, val filter: Filter)

    private fun bind(ps: java.sql.PreparedStatement, p: Prepared, limit: Int) {
        var i = 1
        ps.setInt(i++, limit.coerceIn(1, 5000))
        // «فعال بودن» فقط اگر ستون active روی این سرور وجود داشته باشد
        if (p.sql.contains("CUSTOMERS.active = ?")) ps.setString(i++, ACTIVE_CHAR)
        when (val f = p.filter) {
            is Filter.SysCus -> {
                ps.setInt(i++, f.userId)
                ps.setInt(i++, f.companyId ?: 0)
                ps.setInt(i++, f.companyId ?: 0)
            }
            is Filter.Visitor -> if (p.sql.contains("CUSTOMERS.vis_rdf = ?")) ps.setInt(i++, f.visitorRdf)
            Filter.All -> Unit
        }
    }

    private suspend fun run(p: Prepared, limit: Int): List<CustomerEntity> =
        db.withConnection { c ->
            c.prepareStatement(p.sql).use { ps ->
                ps.queryTimeout = 40
                bind(ps, p, limit)
                ps.executeQuery().use { rs -> rs.readCustomers() }
            }
        }

    /**
     * همگام‌سازی مشتریان با سه مسیر پله‌ای.
     * @return لیست آمادهٔ درج در Room + مسیر و یادداشت‌ها.
     */
    suspend fun fetch(
        userId: Int,
        companyId: Int?,
        visitorRdf: Int?,
        limit: Int = 3000,
    ): Result {
        val cols = try {
            columns().takeIf { it.isNotEmpty() } ?: verifiedColumns
        } catch (e: Throwable) {
            verifiedColumns
        }
        val notes = ArrayList<String>()
        var lastError: String? = null

        // مسیر ۱ — مجوز واقعی کاربر
        runCatching {
            val p = buildQuery(cols, Filter.SysCus(userId, companyId))
            run(p, limit)
        }.onSuccess { rows ->
            if (rows.isNotEmpty()) return Result(rows, "sys_cus")
            notes += "جدول مجوز sys_cus برای این کاربر ردیفی برنگرداند"
        }.onFailure { lastError = it.message ?: it.javaClass.simpleName }

        // مسیر ۲ — مشتریان تخصیص‌یافته به همان ویزیتور
        if (visitorRdf != null && visitorRdf > 0) {
            runCatching {
                val p = buildQuery(cols, Filter.Visitor(visitorRdf))
                if (!p.sql.contains("vis_rdf = ?")) emptyList() else run(p, limit)
            }.onSuccess { rows ->
                if (rows.isNotEmpty()) return Result(rows, "CUSTOMERS.vis_rdf", notes)
                notes += "برای این ویزیتور مشتری‌ای در CUSTOMERS.vis_rdf ثبت نشده"
            }.onFailure { lastError = lastError ?: (it.message ?: it.javaClass.simpleName) }
        }

        // مسیر ۳ — مسیر پشتیبان نهایی: همهٔ مشتریان فعال (تا فهرست هرگز خالی نماند)
        runCatching {
            val p = buildQuery(cols, Filter.All)
            run(p, limit)
        }.onSuccess { rows ->
            if (rows.isNotEmpty()) {
                notes += "فهرست از مسیر پشتیبان «همهٔ مشتریان فعال» خوانده شد"
                return Result(rows, "all_customers", notes)
            }
            notes += "هیچ مشتری فعالی در جدول dbo.CUSTOMERS پیدا نشد"
        }.onFailure { lastError = lastError ?: (it.message ?: it.javaClass.simpleName) }

        return Result(emptyList(), "none", notes, lastError)
    }

    /** ساخت ردیف Room از ردیف واقعی SQL. */
    private fun ResultSet.readCustomers(): List<CustomerEntity> {
        val out = ArrayList<CustomerEntity>()
        while (next()) {
            val shmo = optInt("SHMO") ?: 0
            val name = optString("MONAME").orEmpty()
            val codeRaw = optString("code")
            val active = optString("active")
            val black = optInt("black_list") ?: 0
            val cityText = optString("city_name")
            val cityRdf = optInt("rdf_city")
            out += CustomerEntity(
                id = shmo,
                code = codeRaw?.takeIf { it.isNotBlank() } ?: shmo.toString(),
                name = name.ifBlank { "مشتری $shmo" },
                groupName = optString("group_name").orEmpty(),
                city = cityText?.takeIf { it.isNotBlank() } ?: cityRdf?.toString().orEmpty(),
                address = optString("addre").orEmpty(),
                phone = optString("cell")?.takeIf { it.isNotBlank() } ?: optString("tell1").orEmpty(),
                lat = optDouble("Lat") ?: 0.0,
                lng = optDouble("Lng") ?: 0.0,
                creditOk = black != 1 && !active.equals("f", true),
                debt = optLong("man") ?: 0L,
            )
        }
        return out
    }

    // ── خواندن امن (ستون اگر در SELECT نبود ⇒ null، بدون هیچ استثنا) ────────
    private fun ResultSet.optString(col: String): String? = try {
        val v = getString(col); if (wasNull()) null else v?.trim()
    } catch (_: Throwable) { null }

    private fun ResultSet.optInt(col: String): Int? = try {
        val v = getInt(col); if (wasNull()) null else v
    } catch (_: Throwable) {
        try {
            val s = getString(col)?.trim(); if (s.isNullOrEmpty()) null else s.toInt()
        } catch (_: Throwable) { null }
    }

    private fun ResultSet.optLong(col: String): Long? = try {
        val v = getLong(col); if (wasNull()) null else v
    } catch (_: Throwable) {
        try {
            val s = getString(col)?.trim(); if (s.isNullOrEmpty()) null else s.toDouble().toLong()
        } catch (_: Throwable) { null }
    }

    private fun ResultSet.optDouble(col: String): Double? = try {
        val v = getDouble(col); if (wasNull()) null else v
    } catch (_: Throwable) {
        try {
            val s = getString(col)?.trim(); if (s.isNullOrEmpty()) null else s.toDouble()
        } catch (_: Throwable) { null }
    }
}
