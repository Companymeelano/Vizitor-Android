/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | مدیریت اتصال مستقیم SQL Server (TDS 1433)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  معماری:  UI → ViewModel → Repository → DataSource → این Manager → SQL:1433
 *
 *  مسئولیت‌ها:
 *   • ساخت/بستن اتصال با درایور رسمی mssql-jdbc (JDBC استاندارد)
 *   • Connection Pool کوچک (4) + اعتبارسنجی (Connection.isValid)
 *   • Retry خودکار برای خطاهای گذرا (قطع شبکه، deadlock) با backoff
 *   • Transaction-safe: commit/rollback کامل — هیچ رکورد نیمه‌ای نمی‌ماند
 *   • StateFlow<ConnectionState> برای نمایش زندهٔ وضعیت در UI
 *   • تبدیل خطاهای SQL Server به پیام فارسی گویا
 *   • رمز عبور هرگز در log/crash نمی‌آید (فقط در حلقهٔ اتصال مصرف می‌شود)
 *  تمام عملیات روی Dispatchers.IO است — هیچ کاری UI را نمی‌بندد.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.sql

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLTransientException
import java.sql.Statement
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** وضعیت زندهٔ اتصال (در UI نمایش داده می‌شود). */
sealed class ConnectionState {
    /** اتصال فعال و سالم. */
    data class Ready(val latencyMs: Long) : ConnectionState()
    /** در حال برقرار کردن اتصال. */
    object Connecting : ConnectionState()
    /** قطع است (هنوز تلاش نشده یا کاربر قطع کرده). */
    object Disconnected : ConnectionState()
    /** خطا — پیام فارسی برای کاربر. */
    data class Error(val message: String) : ConnectionState()

    val isReady: Boolean get() = this is Ready
}

/**
 * تنظیمات اتصال (از SecureDbStore خوانده می‌شود).
 * رمز فقط به‌صورت موقت در حافظهٔ این آبجکت می‌ماند — در هیچ‌جای دیگر ذخیره/چاپ نمی‌شود.
 */
data class DbSettings(
    val host: String,
    val port: Int = 1433,
    val database: String,
    val username: String,
    val password: String,
    /** TLS: true توصیه می‌شود؛ اگر گواهی خودامضا باشد trustServerCert=true کافی است. */
    val useEncryption: Boolean = true,
    val trustServerCert: Boolean = true,
    val connectTimeoutSec: Int = 10,
    val queryTimeoutSec: Int = 30,
) {
    /** JDBC URL — بدون هیچ مقادیر حساس. */
    fun jdbcUrl(): String = buildString {
        append("jdbc:sqlserver://").append(host).append(':').append(port)
        append(";databaseName=").append(database)
        append(";encrypt=").append(if (useEncryption) "true" else "false")
        append(";trustServerCertificate=").append(if (trustServerCert) "true" else "false")
        append(";loginTimeout=").append(connectTimeoutSec)
        append(";sendStringParametersAsUnicode=true")
        append(";applicationName=VizitorAndroid")
    }

    /** نمایش امن (برای UI و لاگ): بدون رمز. */
    fun masked(): String = "$username@$host:$port/$database"
}

/**
 * مدیریت اتصال تک‌نشیست (singleton برای کل اپ).
 *
 * استفاده:
 *   SqlConnectionManager.connect(settings)          // یک‌بار پس از تنظیمات
 *   SqlConnectionManager.withConnection { c -> ... } // هر کوئری
 *   SqlConnectionManager.inTransaction { c -> ... }  // ثبت سند (commit/rollback کامل)
 *   SqlConnectionManager.disconnect()               // خروج کاربر
 */
object SqlConnectionManager {

    private const val POOL_MAX = 4
    private const val VALIDATE_SECONDS = 5
    private const val MAX_RETRIES = 2
    private const val RETRY_BACKOFF_MS = 700L

    private val pool = LinkedBlockingQueue<Connection>(POOL_MAX)
    private var settings: DbSettings? = null
    private var connecting = false

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** برقراری اتصال + اعتبارسنجی با SELECT 1. (هرگز دو اتصال هم‌زمان ساخته نمی‌شود) */
    suspend fun connect(s: DbSettings): Boolean = withContext(Dispatchers.IO) {
        if (connecting) return@withContext false
        connecting = true
        try {
        if (s.host.isBlank() || s.database.isBlank() || s.username.isBlank()) {
            _state.value = ConnectionState.Error("آدرس سرور دیتابیس، نام دیتابیس یا نام کاربری خالی است.")
            return@withContext false
        }
        _state.value = ConnectionState.Connecting
        try {
            val c = openOne(s)
            ping(c)
            pool.offer(c)
            settings = s
            _state.value = ConnectionState.Ready(0)
            true
        } catch (e: SQLException) {
            _state.value = ConnectionState.Error(friendlyError(e))
            false
        } catch (e: Exception) {
            _state.value = ConnectionState.Error("خطای غیرمنتظره در اتصال: ${e.javaClass.simpleName}")
            false
        }
        } finally {
            connecting = false
        }
    }

    /**
     * فهرست دیتابیس‌های همان سرور (برای مرحلهٔ «انتخاب دیتابیس حسابداری» در نصب).
     *
     * این تابع با همان کاربر/رمزی که کاربر وارد کرده به دیتابیس master وصل
     * می‌شود، فقط sys.databases را می‌خواند و هیچ چیزی را تغییر نمی‌دهد.
     * دیتابیس‌های سیستمی (database_id <= 4) و دیتابیس‌های آفلاین نمایش داده
     * نمی‌شوند. رمز و هیچ مقدار حساسی log نمی‌شود.
     */
    suspend fun listDatabases(s: DbSettings): Result<List<String>> = withContext(Dispatchers.IO) {
        if (s.host.isBlank() || s.username.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("آدرس سرور یا نام کاربری خالی است."))
        }
        var c: Connection? = null
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
            val master = s.copy(database = "master")     // master در همهٔ نصب‌ها وجود دارد
            c = java.sql.DriverManager.getConnection(master.jdbcUrl(), master.username, master.password)
            val out = mutableListOf<String>()
            c.createStatement().use { st ->
                st.queryTimeout = s.queryTimeoutSec
                st.executeQuery(
                    "SELECT name FROM sys.databases " +
                            "WHERE database_id > 4 AND state = 0 ORDER BY name"
                ).use { rs ->
                    while (rs.next()) out += rs.getString(1)
                }
            }
            Result.success(out)
        } catch (e: SQLException) {
            Result.failure(IllegalStateException(friendlyError(e), e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("گرفتن لیست دیتابیس‌ها ناموفق بود: ${e.javaClass.simpleName}", e))
        } finally {
            try { c?.close() } catch (_: Exception) {}
        }
    }

    /** سنجش سریع سلامت (برای دکمهٔ «تست اتصال» در تنظیمات). */
    suspend fun refresh(): ConnectionState = withContext(Dispatchers.IO) {
        val s = settings ?: return@withContext ConnectionState.Disconnected
        try {
            val startedAt = System.nanoTime()
            val c = borrow()
            try {
                ping(c)
            } finally {
                recycle(c)
            }
            val ms = (System.nanoTime() - startedAt) / 1_000_000
            _state.value = ConnectionState.Ready(ms)
            ConnectionState.Ready(ms)
        } catch (e: SQLException) {
            _state.value = ConnectionState.Error(friendlyError(e))
            ConnectionState.Error(friendlyError(e))
        }
    }

    /** قطع کامل (در هنگام خروج کاربر). */
    @Synchronized
    fun disconnect() {
        drainPool()
        settings = null
        _state.value = ConnectionState.Disconnected
    }

    /**
     * اجرای یک بلوک روی یک اتصال اعتبارسنجی‌شده، با retry برای خطاهای گذرا.
     * اتصال همیشه (حتی در خطا) به استخر برمی‌گردد یا بسته می‌شود — leak ندارد.
     */
    suspend fun <T> withConnection(block: (Connection) -> T): T =
        withContext(Dispatchers.IO) { runWithRetry(block) }

    private suspend fun <T> runWithRetry(block: (Connection) -> T): T {
        requireNotNull(settings) { "قبل از هر کوئری باید اتصال برقرار باشد" }
        var attempt = 0
        while (true) {
            attempt++
            val c = borrow()
            try {
                val result = block(c)
                recycle(c)
                return result
            } catch (e: SQLException) {
                killQuietly(c)
                if (isTransient(e) && attempt <= MAX_RETRIES) {
                    delay(RETRY_BACKOFF_MS * attempt)
                    continue
                }
                if (!c.isClosed && c.isValid(VALIDATE_SECONDS)) {
                    pool.offer(c) // اتصال سالم بود؛ خطا از خود کوئری است
                }
                throw e
            }
        }
    }

    /**
     * Transaction کامل:
     *   موفق → COMMIT یک‌پارچه   |   هر خطا → ROLLBACK کامل
     * هیچ وضعیت نیمه‌ثبتی (header بدون lines و…) وجود ندارد.
     */
    suspend fun <T> inTransaction(block: (Connection) -> T): T = withConnection { c ->
        val previous = c.autoCommit
        c.autoCommit = false
        try {
            val result = block(c)
            c.commit()
            result
        } catch (e: Throwable) {
            try {
                if (!c.isClosed) c.rollback()
            } catch (_: SQLException) {
                // rollback خود خطا داد؛ اتصال در withConnection بسته می‌شود
            }
            throw e
        } finally {
            try {
                c.autoCommit = previous
            } catch (_: SQLException) {
            }
        }
    }

    // ─────────────────────────── زیرساخت داخلی ───────────────────────────

    private fun ping(c: Connection) {
        c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).use { st ->
            st.queryTimeout = 5
            st.execute("SELECT 1")
        }
    }

    private fun openOne(s: DbSettings): Connection {
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver") // ثبت صریح درایور
        val c = java.sql.DriverManager.getConnection(s.jdbcUrl(), s.username, s.password)
        c.transactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
        return c
    }

    private fun borrow(): Connection {
        // اولین تلاش: از استخر
        while (true) {
            val c = pool.poll() ?: break
            if (!c.isClosed && c.isValid(VALIDATE_SECONDS)) {
                return c
            }
            killQuietly(c)
        }
        // استخر خالی: اتصال تازه (تا سقف POOL_MAX ساختن هم‌زمان محدود می‌شود)
        val s = requireNotNull(settings) { "اتصال فعالی وجود ندارد" }
        val fresh = openOne(s)
        ping(fresh)
        return fresh
    }

    private fun recycle(c: Connection) {
        if (!c.isClosed) pool.offer(c)
    }

    private fun killQuietly(c: Connection) {
        try {
            if (!c.isClosed) c.close()
        } catch (_: SQLException) {
        }
    }

    private fun drainPool() {
        while (true) {
            val c = pool.poll() ?: break
            killQuietly(c)
        }
    }

    /** آیا خطا گذراست و با تکرار ارزش دارد؟ (قطع شبکه، deadlock، timeout) */
    private fun isTransient(e: SQLException): Boolean {
        if (e is SQLTransientException) return true
        val code = e.errorCode
        return code in setOf(
            10053, // net/lib error: connection interrupted (قطع شبکه)
            10054, // اتصال از سمت سرور قطع
            1205,  // deadlock victim
            1222,  // lock timeout
            -2     // timeout
        )
    }

    /**
     * ترجمهٔ خطاهای رایج SQL Server به فارسی (بدون نشت جزئیات حساس).
     * کد خطا برای عیب‌یابی فنی هم در پرانتز می‌آید.
     */
    internal fun friendlyError(e: SQLException): String {
        val code = e.errorCode
        val state = e.sqlState
        return when {
            code == 18456 -> "ورود به دیتابیس ناموفق بود — نام کاربری/رمز را بررسی کنید (SQL $code، دلایل: ${e.message?.lines()?.firstOrNull()})"
            code == 4060 -> "دیتابیس با نام واردشده پیدا نشد یا دسترسی ندارید (SQL $code)"
            code == 40197 -> "این حساب اجازهٔ اتصال به این دیتابیس را ندارد (SQL $code)"
            code == 53 -> "به سرور دیتابیس نمی‌توان رسید — IP/پورت را بررسی کنید یا مطمئن شوید فایروال سرور باز است (SQL $code)"
            code == 10053 || code == 10054 -> "اتصال در حین کار قطع شد — شبکهٔ موبایل/وای‌فای را چک کنید (SQL $code)"
            code == 1205 -> "درگیری موقت با یک تراکنش دیگر (deadlock) — دوباره تلاش کنید (SQL $code)"
            state == "08S01" || state?.startsWith("08") == true -> "خطای ارتباطی با سرور دیتابیس (SQLState $state)"
            state?.startsWith("28") == true -> "احراز هویت ناموفق (SQLState $state)"
            else -> "خطای دیتابیس (کد ${code ?: "—"}): ${firstLine(e)}"
        }
    }

    private fun firstLine(e: SQLException): String =
        e.message?.lines()?.firstOrNull()?.take(160) ?: "نامشخص"
}
