// ═══════════════════════════════════════════════════════════════════════════
//  انتخاب و ساخت نشانی درایور JDBC برای اتصال مستقیم به SQL Server (پورت ۱۴۳۳)
//
//  دو درایور همراه برنامه بسته‌بندی شده‌اند تا روی هر گوشی‌ای کار کند:
//    ۱) درایور رسمی مایکروسافت (mssql-jdbc) — نشانی: jdbc:sqlserver://…
//    ۲) درایور پشتیبان jTDS                — نشانی: jdbc:jtds:sqlserver://…
//
//  برنامه اول درایور رسمی را امتحان می‌کند و اگر روی آن دستگاه بار نشد، خودکار
//  به jTDS برمی‌گردد. هیچ‌کدام از این‌ها رمز را جایی چاپ نمی‌کنند.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.data.sql

object DirectSql {

    const val MSSQL = "mssql"
    const val JTDS = "jtds"

    private const val MSSQL_CLASS = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    private const val JTDS_CLASS = "net.sourceforge.jtds.jdbc.Driver"

    @Volatile
    private var cached: String? = null

    /** درایور قابل استفاده روی این دستگاه (بار اول تشخیص داده و کش می‌شود). */
    fun detect(): String {
        cached?.let { return it }
        val found = when {
            tryLoad(MSSQL_CLASS) -> MSSQL
            tryLoad(JTDS_CLASS) -> JTDS
            else -> MSSQL     // هیچ‌کدام نبود ⇒ خطای واضح در زمان اتصال داده می‌شود
        }
        cached = found
        return found
    }

    /** برای سازگاری با کد تأییدشدهٔ قبلی که فقط Class.forName صدا می‌زد. */
    fun ensureDriver(): String = detect()

    /** نام فارسی درایور برای نمایش در کارت وضعیت. */
    fun displayName(kind: String = detect()): String = when (kind) {
        JTDS -> "jTDS"
        else -> "Microsoft JDBC"
    }

    /** نشانی JDBC بر پایهٔ درایور انتخابی — بدون رمز (رمز جدا به DriverManager داده می‌شود). */
    fun url(s: DbSettings, kind: String = detect()): String = when (kind) {
        JTDS -> buildString {
            append("jdbc:jtds:sqlserver://").append(s.host).append(':').append(s.port)
            append('/').append(s.database.ifBlank { "master" })
            append(";useUnicode=true;characterEncoding=UTF-8")
            append(";loginTimeout=").append(s.connectTimeoutSec)
            append(";appName=VizitorAndroid")
        }
        else -> s.jdbcUrl()
    }

    private fun tryLoad(className: String): Boolean = try {
        Class.forName(className)
        true
    } catch (_: Throwable) {
        false    // درایور نبود یا روی این دستگاه قابل بارگذاری نبود
    }
}
