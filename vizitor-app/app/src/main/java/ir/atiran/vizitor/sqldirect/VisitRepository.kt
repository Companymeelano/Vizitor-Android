/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ثبت ویزیت در جدول واقعی dbo.Visit
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  قرارداد ستون‌ها (از خروجی ممیزی واقعی سرور — ATIRAN-SCHEMA-EXTRACTED.md):
 *
 *    dbo.Visit :
 *      VisitID     bigint   (کلید اصلی)
 *      VisRdf      int      ← dbo.visitors.vis_rdf  (ویزیتور)
 *      Shmo        int      ← dbo.CUSTOMERS.SHMO    (مشتری)
 *      Duration    int      ← مدت ویزیت (دقیقه)
 *      Created     bigint   ← ثانیهٔ UNIX زمان ثبت
 *      Sent        bigint   ← ۰ = ثبت‌شده (ERP خودش به‌روزرسانی می‌کند)
 *      Description nvarchar ← توضیح ویزیت
 *      SentLng/SentLat float ← مختصات سمت سرور (خالی می‌ماند)
 *      SaveLat/SaveLng float ← مختصات ثبت از دستگاه
 *      DateCreated char     ← تاریخ شمسی قالب ERP (1405/06/27)
 *      DateSent    char     ← خالی می‌ماند
 *      TimeCreated char     ← ساعت قالب HH:mm:ss
 *      TimeSent    char     ← خالی می‌ماند
 *
 *  نکات:
 *   • اگر VisitID در این نسخهٔ ERP «identity» باشد، خودِ سرور شماره می‌دهد؛
 *     وگرنه خود ما MAX(VisitID)+1 را می‌گذاریم — هر دو حالت کار می‌کند.
 *   • همهٔ کوئری‌ها پارامتری‌اند؛ هیچ رشته‌ای به SQL چسبانده نمی‌شود.
 *   • کاربر محدود (vizitor_android) باید INSERT روی dbo.Visit داشته باشد —
 *     نصب‌کنندهٔ ویندوز این اجازه را می‌دهد (provision_android_sql.py).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import java.sql.Connection
import java.sql.ResultSet

/** یک ویزیت ثبت‌شده در dbo.Visit (با نام مشتری از همان دیتابیس). */
data class VisitRow(
    val visitId: Long,
    val visRdf: Int,
    val shmo: Int,
    val customerName: String,
    val durationMin: Int,
    val description: String,
    val lat: Double?,
    val lng: Double?,
    val dateCreated: String,
    val timeCreated: String,
)

object VisitRepository {

    private val db get() = SqlConnectionManager

    // دو دستور ترکیبی (INSERT + SELECT SCOPE_IDENTITY) در یک PreparedStatement
    // با پارامتر روی همهٔ درایورها یکسان کار نمی‌کند؛ درج و خواندن شمارهٔ ثبت
    // دو دستور جداگانه اجرا می‌شوند (SCOPE_IDENTITY فقط نتیجهٔ همین تراکنش/اتصال را می‌دهد).
    private const val SQL_INSERT = """
        INSERT INTO dbo.Visit
            (VisRdf, Shmo, Duration, Created, Sent, Description,
             SaveLat, SaveLng, DateCreated, TimeCreated)
        VALUES (?, ?, ?, ?, 0, ?, ?, ?, ?, ?)
    """

    private const val SQL_INSERT_EXPLICIT = """
        INSERT INTO dbo.Visit
            (VisitID, VisRdf, Shmo, Duration, Created, Sent, Description,
             SaveLat, SaveLng, DateCreated, TimeCreated)
        VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?)
    """

    /** آیا VisitID در این دیتابیس identity است؟ (برای درج درست) */
    private fun isVisitIdIdentity(c: Connection): Boolean =
        c.prepareStatement(
            """
            SELECT CASE WHEN MAX(c.is_identity) = 1 THEN 1 ELSE 0 END
              FROM sys.columns AS c
             WHERE c.object_id = OBJECT_ID(N'dbo.Visit')
               AND c.name = N'VisitID'
            """.trimIndent()
        ).use { ps ->
            ps.queryTimeout = 15
            ps.executeQuery().use { rs -> rs.next() && rs.getInt(1) == 1 }
        }

    /**
     * ثبت یک ویزیت. مقدار برگشتی: شمارهٔ ثبت (VisitID) یا -1 اگر نخواند.
     * @param lat/lng مختصات دستگاه (null = بدون موقعیت)
     */
    suspend fun recordVisit(
        visRdf: Int,
        shmo: Int,
        durationMin: Int,
        lat: Double?,
        lng: Double?,
        description: String,
        dateCreated: String,
        timeCreated: String,
    ): Long = db.withConnection { c ->
        val identity = isVisitIdIdentity(c)
        val nowUnix = System.currentTimeMillis() / 1000L
        if (identity) {
            c.prepareStatement(SQL_INSERT.trimIndent()).use { ps ->
                bindVisitParams(ps, visRdf, shmo, durationMin, nowUnix, lat, lng, description, dateCreated, timeCreated)
                ps.queryTimeout = 30
                ps.executeUpdate()
            }
            // شمارهٔ ثبتِ همین اتصال/تراکنش (حداکثر چند میکروثانیه از درج فاصله دارد)
            c.createStatement().use { st ->
                st.queryTimeout = 15
                st.executeQuery("SELECT CAST(SCOPE_IDENTITY() AS BIGINT)").use { rs ->
                    if (rs.next()) rs.getLong(1) else -1L
                }
            }
        } else {
            // identity نیست: شمارهٔ بعدی را اول می‌خوانیم، بعد با همان درج می‌کنیم
            val nextId = c.prepareStatement(
                "SELECT ISNULL(MAX(VisitID), 0) + 1 FROM dbo.Visit"
            ).use { ps ->
                ps.queryTimeout = 15
                ps.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) else 1L }
            }
            c.prepareStatement(SQL_INSERT_EXPLICIT.trimIndent()).use { ps ->
                ps.setLong(1, nextId)
                bindVisitParams(ps, visRdf, shmo, durationMin, nowUnix, lat, lng, description, dateCreated, timeCreated, offset = 2)
                ps.queryTimeout = 30
                ps.executeUpdate()
                nextId
            }
        }
    }

    /**
     * پر کردن پارامترهای مشترک (VisRdf تا TimeCreated).
     * در حالت identity این پارامترها از ۱ شروع می‌شوند؛ در حالت صریح،
     * پارامتر ۱ = VisitID است و بقیه با [offset] جابه‌جا می‌شوند.
     */
    private fun bindVisitParams(
        ps: java.sql.PreparedStatement,
        visRdf: Int,
        shmo: Int,
        durationMin: Int,
        nowUnix: Long,
        lat: Double?,
        lng: Double?,
        description: String,
        dateCreated: String,
        timeCreated: String,
        offset: Int = 1,
    ) {
        ps.setInt(offset, visRdf)
        ps.setInt(offset + 1, shmo)
        ps.setInt(offset + 2, durationMin)
        ps.setLong(offset + 3, nowUnix)
        ps.setString(offset + 4, description)
        if (lat != null && lng != null) {
            ps.setDouble(offset + 5, lat)
            ps.setDouble(offset + 6, lng)
        } else {
            ps.setNull(offset + 5, java.sql.Types.FLOAT)
            ps.setNull(offset + 6, java.sql.Types.FLOAT)
        }
        ps.setString(offset + 7, dateCreated)
        ps.setString(offset + 8, timeCreated)
    }

    /** فهرست ویزیت‌های اخیر یک ویزیتور (جدیدترین اول) + نام مشتری. */
    suspend fun recentVisits(visitorRdf: Int, limit: Int = 60): List<VisitRow> =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT v.VisitID, v.VisRdf, v.Shmo, v.Duration, v.Description,
                       v.SaveLat, v.SaveLng, v.DateCreated, v.TimeCreated,
                       c.MONAME AS customer_name
                  FROM dbo.Visit AS v
                  LEFT JOIN dbo.CUSTOMERS AS c ON c.SHMO = v.Shmo
                 WHERE v.VisRdf = ?
                 ORDER BY v.VisitID DESC
                 OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 30
                ps.setInt(1, visitorRdf)
                ps.setInt(2, limit.coerceIn(1, 500))
                ps.executeQuery().use { rs -> rs.mapVisitRows() }
            }
        }

    /** تعداد ویزیت‌های امروزِ یک ویزیتور (بر اساس تاریخ شمسی ثبت). */
    suspend fun countToday(visitorRdf: Int, todayJalali: String): Int =
        db.withConnection { c ->
            c.prepareStatement(
                """
                SELECT COUNT(*) AS n
                  FROM dbo.Visit
                 WHERE VisRdf = ? AND DateCreated = ?
                """.trimIndent()
            ).use { ps ->
                ps.queryTimeout = 15
                ps.setInt(1, visitorRdf)
                ps.setString(2, todayJalali)
                ps.executeQuery().use { rs -> if (rs.next()) rs.getInt("n") else 0 }
            }
        }

    private fun ResultSet.mapVisitRows(): List<VisitRow> {
        val out = ArrayList<VisitRow>()
        while (next()) {
            val latVal = getDouble("SaveLat")
            val lat = if (wasNull()) null else latVal
            val lngVal = getDouble("SaveLng")
            val lng = if (wasNull()) null else lngVal
            out += VisitRow(
                visitId = getLong("VisitID"),
                visRdf = getInt("VisRdf"),
                shmo = getInt("Shmo"),
                customerName = getString("customer_name")?.trim().orEmpty(),
                durationMin = getInt("Duration"),
                description = getString("Description")?.trim().orEmpty(),
                lat = lat,
                lng = lng,
                dateCreated = getString("DateCreated")?.trim().orEmpty(),
                timeCreated = getString("TimeCreated")?.trim().orEmpty(),
            )
        }
        return out
    }
}

/**
 * ترجمهٔ خطای ثبت ویزیت به فارسی — بخصوص وقتی اجازهٔ INSERT به کاربر
 * محدود داده نشده باشد (نصب‌کنندهٔ قدیمی اجرا نشده باشد):
 */
fun friendlyVisitError(e: Throwable): String {
    val msg = (e.message ?: "").lowercase()
    return when {
        msg.contains("permission denied") || msg.contains("permission was denied") ||
            msg.contains("229") || msg.contains("230") ->
            "کاربر محدود دیتابیس اجازهٔ نوشتن در جدول Visit را ندارد (SQL permission denied).\n" +
                "راه‌حل: نصب‌کنندهٔ ویندوز را دوباره اجرا کنید (بخش «آماده‌سازی اتصال مستقیم اندروید» " +
                "اجازهٔ INSERT ON dbo.Visit را می‌دهد) — یا دستی در SSMS: " +
                "GRANT INSERT ON dbo.Visit TO [vizitor_android]."
        msg.contains("does not exist") || msg.contains("913") || msg.contains("object name") ->
            "جدول Visit در این دیتابیس پیدا نشد — نسخهٔ آتیران روی این سرور را بررسی کنید."
        msg.contains("timed out") || msg.contains("timeout") || msg.contains("10053") || msg.contains("10054") ->
            "اتصال به سرور در حین ثبت قطع شد — شبکه را چک کنید و دوباره تلاش کنید."
        else ->
            "ثبت ویزیت انجام نشد: ${(e.message ?: e.javaClass.simpleName).lines().firstOrNull().orEmpty().take(160)}"
    }
}
