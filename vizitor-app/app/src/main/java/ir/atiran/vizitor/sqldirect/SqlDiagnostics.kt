/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | عیب‌یابی گام‌به‌گام اتصال مستقیم SQL Server
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرا این فایل لازم است؟
 *  «پورت ۱۴۳۳ در ping.eu سبز است» فقط یعنی یک برنامه روی آن پورت گوش می‌دهد.
 *  ده‌ها چیز دیگر می‌تواند جلوی ورود برنامه را بگیرد: حالت احراز هویت ویندوزی،
 *  دسترسی‌نداشتن کاربر، درایور، TLS، آی‌پی داخلی/اختصاصی، نام دیتابیس…
 *  این فایل همان مسیر را مرحله‌به‌مرحله می‌رود و برای هر مرحله می‌گوید
 *  «شد یا نشد و چرا» — با پیام فارسی و جزئیات فنی، بدون هیچ رمزی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import java.net.InetSocketAddress
import java.net.Socket
import java.sql.Connection
import java.sql.DriverManager

/** یک سطر نتیجهٔ عیب‌یابی. */
data class DiagRow(
    val title: String,
    val ok: Boolean,
    val detail: String,
    val hint: String = "",
)

object SqlDiagnostics {

    private const val TCP_TIMEOUT_MS = 6000

    /**
     * همهٔ مراحل عیب‌یابی. هیچ استثنایی بیرون نمی‌زند — هر شکست به یک سطر تبدیل می‌شود.
     * هیچ رمزی در خروجی نیست.
     */
    fun run(cfg: DbSettings): List<DiagRow> {
        val rows = ArrayList<DiagRow>()
        val host = cfg.cleanHost

        // ── ۱) پاک‌سازی نشانی ────────────────────────────────────────────────
        rows += if (host.isBlank()) {
            DiagRow("۱) نشانی سرور", false, "خالی است", "آی‌پی سرور را وارد کنید")
        } else {
            val changed = host != cfg.host.trim()
            DiagRow(
                "۱) نشانی سرور", true,
                "$host  —  پورت ${cfg.cleanPort}" + if (changed) "  (نشانی واردشده پاک‌سازی شد)" else "",
                if (cfg.cleanPort != 1433) "پورت پیش‌فرض SQL Server روی این سامانه ۱۴۳۳ است" else "",
            )
        }

        // ── ۲) DNS (اگر نام دامنه است) ───────────────────────────────────────
        val isIp = host.matches(Regex("^\\\\d{1,3}(\\\\.\\\\d{1,3}){3}$"))
        if (host.isNotBlank() && !isIp) {
            rows += try {
                val ip = java.net.InetAddress.getByName(host).hostAddress
                DiagRow("۲) تبدیل نام دامنه به آی‌پی", true, "$host → $ip")
            } catch (t: Throwable) {
                DiagRow("۲) تبدیل نام دامنه به آی‌پی", false, t.javaClass.simpleName,
                    "نام دامنه را درست وارد کنید یا مستقیم آی‌پی بزنید")
            }
        } else {
            rows += DiagRow("۲) تبدیل نام دامنه به آی‌پی", true, "لازم نیست (آی‌پی وارد شده)")
        }

        // ── ۳) دسترسی شبکه به پورت (همان کاری که ping.eu می‌کند) ─────────────
        rows += tcpCheck("۳) دسترسی شبکه به پورت ${cfg.cleanPort}", host, cfg.cleanPort)

        // ── ۴) اگر نشانی دومی هم دارید، آن را هم امتحان کن ──────────────────
        // (کاربر معمولاً هم آی‌پی داخلی دارد هم اختصاصی؛ معلوم می‌شود کدام از
        //  جای فعلی او در دسترس است.)
        // این بررسی در ViewModel با هر دو نشانی انجام می‌شود.

        // ── ۵) بارگذاری درایورها روی این گوشی ───────────────────────────────
        val driverRows = DirectSql.ORDER.map { kind ->
            val err = DirectSql.loadError(kind)
            if (err == null) {
                DiagRow("درایور ${DirectSql.displayName(kind)}", true, "بار شد (${DirectSql.className(kind)})")
            } else {
                DiagRow("درایور ${DirectSql.displayName(kind)}", false, err,
                    "درایور دیگر خودکار امتحان می‌شود")
            }
        }
        rows += driverRows
        if (driverRows.all { !it.ok }) {
            rows += DiagRow("درایورهای JDBC", false, "هیچ درایوری روی این گوشی بار نشد",
                "نسخهٔ کامل برنامه را نصب کنید (نسخهٔ debug یا release همین ریلیز)")
        }

        // ── ۶) ورود واقعی به سرور + حالت احراز هویت سرور ────────────────────
        if (host.isBlank() || cfg.username.isBlank() || cfg.password.isBlank()) {
            rows += DiagRow("۶) ورود به SQL Server", false, "نام کاربری یا رمز خالی است",
                "کاربر محدود دیتابیس و رمزش را وارد کنید")
            return rows
        }

        val master = cfg.copy(database = "master")
        val attempts = ArrayList<String>()
        var logged = false
        for (kind in DirectSql.order()) {
            var opened: Connection? = null
            try {
                val c = DriverManager.getConnection(DirectSql.url(master, kind), master.username, master.password)
                opened = c
                logged = true
                val version = c.createStatement().use { st ->
                    st.queryTimeout = 10
                    st.executeQuery("SELECT @@VERSION AS v").use { rs -> if (rs.next()) rs.getString(1) else "" }
                }.lines().firstOrNull().orEmpty().trim()
                val windowsOnly = c.createStatement().use { st ->
                    st.executeQuery("SELECT CAST(SERVERPROPERTY('IsIntegratedSecurityOnly') AS int) AS v")
                        .use { rs -> if (rs.next()) rs.getInt(1) else 0 }
                }
                val me = c.createStatement().use { st ->
                    st.executeQuery("SELECT SUSER_SNAME() AS v").use { rs -> if (rs.next()) rs.getString(1) else "" }
                }
                rows += DiagRow(
                    "۶) ورود به SQL Server (با ${DirectSql.displayName(kind)})", true,
                    "موفق — سرور شما را «$me» می‌شناسد\\nنسخه: $version" +
                        if (windowsOnly == 1) "\\n⚠️ سرور در حالت «فقط احراز هویت ویندوز» است؛ ورود با کاربر SQL فقط با فعال‌کردن حالت Mixed Mode ممکن می‌شود." else "",
                    if (windowsOnly == 1) "نصب‌کننده → گزینهٔ Mixed Mode → ری‌استارت سرویس SQL" else "",
                )
                break
            } catch (e: java.sql.SQLException) {
                attempts += "${DirectSql.displayName(kind)}: ${SqlConnectionManager.friendlyError(e)}"
                // اگر خطا یک پاسخ واقعی از سرور بود (مثل رمز اشتباه) دیگر سراغ درایور بعدی نرو
                if (e.errorCode != 0) break
            } catch (t: Throwable) {
                attempts += "${DirectSql.displayName(kind)}: ${SqlConnectionManager.describeThrowable(t)}"
            } finally {
                try { opened?.close() } catch (_: Throwable) {}
            }
        }
        if (!logged) {
            rows += DiagRow(
                "۶) ورود به SQL Server", false,
                attempts.joinToString("\\n").ifBlank { "اتصال برقرار نشد" },
                "پورت باز است ولی ورود انجام نشد — پیام بالا دقیقاً می‌گوید چرا",
            )
            return rows
        }

        // ── ۷) فهرست دیتابیس‌ها و دسترسی به دیتابیس حسابداری ───────────────
        if (cfg.database.isNotBlank()) {
            var opened: Connection? = null
            try {
                val c = DriverManager.getConnection(DirectSql.url(cfg, DirectSql.order().first()), cfg.username, cfg.password)
                opened = c
                val accessible = c.prepareStatement("SELECT HAS_DBACCESS(?) AS v, DB_NAME(DB_ID(?)) AS n").use { ps ->
                    ps.setString(1, cfg.database); ps.setString(2, cfg.database)
                    ps.queryTimeout = 10
                    ps.executeQuery().use { rs ->
                        if (rs.next()) (rs.getInt("v") to (rs.getString("n") ?: "")) else (0 to "")
                    }
                }
                rows += when {
                    accessible.second.isBlank() -> DiagRow(
                        "۷) دسترسی به دیتابیس «${cfg.database}»", false,
                        "این نام روی سرور پیدا نشد",
                        "نام دیتابیس را از فهرست انتخاب کنید یا دقیق بنویسید",
                    )
                    accessible.first == 1 -> DiagRow("۷) دسترسی به دیتابیس «${cfg.database}»", true, "دسترسی دارد ✓")
                    else -> DiagRow(
                        "۷) دسترسی به دیتابیس «${cfg.database}»", false,
                        "دیتابیس هست ولی این کاربر دسترسی ندارد",
                        "نصب‌کننده باید کاربر را در همین دیتابیس بسازد (db_datareader)",
                    )
                }
            } catch (e: java.sql.SQLException) {
                rows += DiagRow("۷) دسترسی به دیتابیس «${cfg.database}»", false,
                    SqlConnectionManager.friendlyError(e))
            } catch (t: Throwable) {
                rows += DiagRow("۷) دسترسی به دیتابیس «${cfg.database}»", false,
                    SqlConnectionManager.describeThrowable(t))
            } finally {
                try { opened?.close() } catch (_: Throwable) {}
            }
        } else {
            rows += DiagRow("۷) نام دیتابیس", false, "خالی است",
                "نام دیتابیس حسابداری را وارد کنید (قابل تایپ دستی)")
        }

        return rows
    }

    /** یک بررسی خالص شبکه‌ای: باز شدن TCP روی نشانی/پورت (بدون هیچ اعتبارنامه‌ای). */
    fun tcpCheck(title: String, host: String, port: Int): DiagRow {
        if (host.isBlank()) return DiagRow(title, false, "نشانی خالی است")
        val started = System.nanoTime()
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress(host, port), TCP_TIMEOUT_MS)
            }
            val ms = (System.nanoTime() - started) / 1_000_000
            DiagRow(title, true, "پورت باز است (${ms} میلی‌ثانیه) — سرور روی $host:$port اتصال را می‌پذیرد")
        } catch (t: Throwable) {
            DiagRow(
                title, false,
                "${t.javaClass.simpleName}: ${t.message?.take(90) ?: "بدون پیام"}",
                when {
                    t is java.net.SocketTimeoutException ->
                        "پورت از این شبکه بسته است — اگر بیرون از شبکه هستید، آی‌پی اختصاصی و کلید «اتصال از بیرون» را بررسی کنید"
                    t is java.net.ConnectException ->
                        "سرور روی این نشانی گوش نمی‌دهد: یا آی‌پی داخلی/اختصاصی جابه‌جا وارد شده یا SQL Server روی ۱۴۳۳ فعال نیست"
                    t is java.net.UnknownHostException -> "نام/آی‌پی سرور درست نیست"
                    else -> ""
                },
            )
        }
    }

    /** فقط یک «بله/خیر» سریع: آیا این نشانی/پورت از همین گوشی باز می‌شود؟ */
    fun tcpReachable(host: String, port: Int, timeoutMs: Int = 4000): Boolean {
        val h = sanitizeHost(host)
        if (h.isBlank()) return false
        return try {
            Socket().use { it.connect(InetSocketAddress(h, port), timeoutMs) }
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** گزارش متنی (برای دکمهٔ کپی) — بدون هیچ رمزی. */
    fun buildReport(cfg: DbSettings, rows: List<DiagRow>, extra: List<String> = emptyList()): String =
        buildString {
            appendLine("گزارش عیب‌یابی اتصال مستقیم آتیران ویزیتور")
            appendLine("سرور: ${cfg.cleanHost}:${cfg.cleanPort}  |  دیتابیس: ${cfg.database}  |  کاربر: ${cfg.username}")
            appendLine("درایور فعال: ${DirectSql.displayName(SqlConnectionManager.activeDriver)}")
            appendLine("—".repeat(50))
            rows.forEach { r ->
                appendLine("[${if (r.ok) "OK" else "FAIL"}] ${r.title}")
                r.detail.lines().forEach { appendLine("      $it") }
                if (r.hint.isNotBlank()) appendLine("      ↳ ${r.hint}")
            }
            extra.forEach { appendLine(it) }
            appendLine("—".repeat(50))
            appendLine("(این گزارش هیچ رمزی ندارد و می‌توانید آن را برای پشتیبانی بفرستید)")
        }
}
