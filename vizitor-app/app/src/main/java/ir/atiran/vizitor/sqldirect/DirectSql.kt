// ═══════════════════════════════════════════════════════════════════════════
//  انتخاب و ساخت نشانی درایور JDBC برای اتصال مستقیم به SQL Server (پورت ۱۴۳۳)
//
//  دو درایور همراه برنامه بسته‌بندی شده و ترتیب امتحان‌شدنش این است:
//    ۱) jTDS  (net.sourceforge.jtds) — نشانی: jdbc:jtds:sqlserver://…
//       دلیل اول بودن: روی اندروید آزمایش‌شده است؛ به javax.naming و java.time
//       نیاز ندارد و TDS 8 را مستقیم حرف می‌زند. (درایور رسمی مایکروسافت روی
//       اندروید در بعضی مسیرها javax.naming/java.time می‌خواهد و روی اندروید
//       قدیمی (API 24/25) بار نمی‌شود.)
//    ۲) درایور رسمی مایکروسافت (mssql-jdbc) — نشانی: jdbc:sqlserver://…
//
//  اگر درایور اول روی یک دستگاه/سرور جواب نداد، دومی خودکار امتحان می‌شود؛
//  اما فقط وقتی خطا «مربوط به خود درایور» باشد — خطای رمز عبور دوباره تکرار
//  نمی‌شود تا حساب SQL قفل نشود.
//  هیچ‌کدام از این‌ها رمز را جایی چاپ نمی‌کنند.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.sqldirect

object DirectSql {

    const val JTDS = "jtds"
    const val MSSQL = "mssql"

    const val JTDS_CLASS = "net.sourceforge.jtds.jdbc.Driver"
    const val MSSQL_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

    /** ترتیب امتحان درایورها — jTDS اول (اندروید-دوست)، مایکروسافت دوم. */
    val ORDER = listOf(JTDS, MSSQL)

    @Volatile
    private var loaded: MutableMap<String, String?> = LinkedHashMap()   // kind -> پیام خطای بارگذاری

    /** آیا کلاس این درایور روی این دستگاه بار می‌شود؟ (نتیجه کش می‌شود) */
    fun tryLoad(kind: String): Boolean {
        val className = className(kind)
        loaded[kind]?.let { return it == null }        // قبلاً امتحان شده
        return try {
            Class.forName(className)
            loaded[kind] = null
            true
        } catch (t: Throwable) {
            // NoClassDefFoundError و هم‌خانواده‌هایش (Error) هم همین‌جا گرفته می‌شوند
            loaded[kind] = "${t.javaClass.simpleName}: ${t.message?.take(120) ?: "—"}"
            false
        }
    }

    /**
     * درایورهای قابل استفاده روی این دستگاه، به ترتیب اولویت.
     * اگر هیچ درایوری بار نشد، هر دو برمی‌گردند تا خطای واقعی به کاربر نشان داده شود
     * (به‌جای پیام گنگ «درایور پیدا نشد»).
     */
    fun order(): List<String> {
        val ok = ORDER.filter { tryLoad(it) }
        return ok.ifEmpty { ORDER }
    }

    /** پیام خطای بارگذاری یک درایور (برای گزارش عیب‌یابی) یا null اگر سالم بار شده. */
    fun loadError(kind: String): String? {
        tryLoad(kind)
        return loaded[kind]
    }

    fun className(kind: String): String = when (kind) {
        JTDS -> JTDS_CLASS
        else -> MSSQL_CLASS
    }

    /** نام فارسی/نمایشی درایور برای کارت وضعیت. */
    fun displayName(kind: String?): String = when (kind) {
        JTDS -> "jTDS"
        MSSQL -> "Microsoft JDBC"
        else -> "—"
    }

    /** نشانی JDBC بر پایهٔ درایور — بدون رمز (رمز جدا به DriverManager داده می‌شود). */
    fun url(s: DbSettings, kind: String): String = when (kind) {
        JTDS -> buildString {
            append("jdbc:jtds:sqlserver://").append(s.cleanHost).append(':').append(s.cleanPort)
            append('/').append(s.database.ifBlank { "master" })
            append(";useUnicode=true;characterEncoding=UTF-8")
            append(";loginTimeout=").append(s.connectTimeoutSec)
            append(";socketTimeout=").append(s.queryTimeoutSec)
            // jTDS: ssl=off | request | require | authenticate
            append(";ssl=").append(if (s.useEncryption) "require" else "off")
            append(";appName=VizitorAndroid")
        }
        else -> buildString {
            append("jdbc:sqlserver://").append(s.cleanHost).append(':').append(s.cleanPort)
            append(";databaseName=").append(s.database.ifBlank { "master" })
            append(";encrypt=").append(if (s.useEncryption) "true" else "false")
            append(";trustServerCertificate=").append(if (s.trustServerCert) "true" else "false")
            append(";loginTimeout=").append(s.connectTimeoutSec)
            append(";sendStringParametersAsUnicode=true")
            append(";applicationName=VizitorAndroid")
        }
    }
}

/**
 * پاک‌سازی نشانی سرور از چیزهایی که کاربران معمولاً ناخواسته وارد می‌کنند:
 * `http://`، `tcp:`، فاصله، اسلش انتهایی، `host,1433`، `host\instance`.
 * خروجی برای متن راهنما امن است (بدون رمز).
 */
fun sanitizeHost(raw: String): String {
    var h = raw.trim()
    h = h.removePrefix("jdbc:sqlserver://").removePrefix("jdbc:jtds:sqlserver://")
    h = h.removePrefix("tcp:").removePrefix("http://").removePrefix("https://")
    h = h.substringBefore('/').substringBefore('\\').trim()
    // host,port → host  (پورت جدا وارد می‌شود)
    if (h.contains(',') && h.substringAfter(',').trim().all { it.isDigit() }) {
        h = h.substringBefore(',').trim()
    }
    // host:port → host
    if (h.count { it == ':' } == 1 && h.substringAfter(':').all { it.isDigit() } && h.substringAfter(':').isNotEmpty()) {
        h = h.substringBefore(':').trim()
    }
    return h.trim().trimEnd('.').trim()
}
