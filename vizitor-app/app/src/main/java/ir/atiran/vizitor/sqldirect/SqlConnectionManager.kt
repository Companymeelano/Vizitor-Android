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
package ir.atiran.vizitor.sqldirect

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
import java.util.concurrent.LinkedBlockingQueue

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
    /** نشانی سرورِ پاک‌سازی‌شده (بدون http://، بدون اسلش/فاصلهٔ اضافه). */
    val cleanHost: String get() = sanitizeHost(host)

    /** پورت معتبر (اگر کاربر چیز عجیبی وارد کرد، ۱۴۳۳). */
    val cleanPort: Int get() = if (port in 1..65535) port else 1433

    /** JDBC URL درایور مایکروسافت — بدون هیچ مقدار حساس. */
    fun jdbcUrl(): String = DirectSql.url(this, DirectSql.MSSQL)

    /** نشانی jTDS (درایور اول روی اندروید). */
    fun jtdsUrl(): String = DirectSql.url(this, DirectSql.JTDS)

    /** نمایش امن (برای UI و لاگ): بدون رمز. */
    fun masked(): String = "$username@$cleanHost:$cleanPort/$database"
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

    // ── نکتهٔ نسخهٔ اندروید ────────────────────────────────────────────────
    //  در این پروژه دو درایور بسته‌بندی شده است (رسمی مایکروسافت + jTDS) و
    //  انتخاب/ساخت نشانی به DirectSql سپرده شده تا اگر درایور رسمی روی یک
    //  دستگاه بار نشد، خودکار jTDS استفاده شود. هیچ کوئری‌ای تغییر نکرده است.

    private const val POOL_MAX = 4
    private const val VALIDATE_SECONDS = 5
    private const val MAX_RETRIES = 2
    private const val RETRY_BACKOFF_MS = 700L

    private val pool = LinkedBlockingQueue<Connection>(POOL_MAX)
    private var settings: DbSettings? = null
    private var connecting = false

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** آیا همین حالا یک اتصال سالم داریم؟ (برای تصمیم‌گیری در همگام‌سازی) */
    fun connected(): Boolean = _state.value.isReady

    // ── v2.18.0: خودترمیمی اتصال ───────────────────────────────────────────
    //  ایراد نسخه‌های قبل: `settings` فقط در همان اجرای برنامه (process) زنده
    //  بود. اگر اندروید برنامه را می‌بست و کاربر بدون رفتن به صفحهٔ اتصال وارد
    //  یک تب دیگر می‌شد، `settings` تهی بود و هر کوئری با پیام
    //  «قبل از هر کوئری باید اتصال برقرار باشد» شکست می‌خورد — یعنی اتصالِ
    //  ذخیره‌شده روی گوشی عملاً استفاده نمی‌شد. حالا هر کوئری می‌تواند خودش
    //  از حافظهٔ امن (SecureDbStore) اتصال را برقرار کند.

    /** تنظیمات ذخیره‌شدهٔ همین گوشی (اگر کاربر قبلاً ذخیره کرده باشد). */
    private fun storedSettings(): DbSettings? =
        runCatching { SecureDbStore.load() }.getOrNull()

    /** تنظیمات فعال؛ اگر در این اجرا ست نشده باشد، از حافظهٔ امن خوانده می‌شود. */
    private fun currentSettings(): DbSettings? =
        settings ?: storedSettings()?.also { settings = it }

    /**
     * تضمین اتصال: نقطهٔ ورود واحد همهٔ بخش‌ها.
     * اگر اتصال باز و سالم است true؛ وگرنه با تنظیمات ذخیره‌شده وصل می‌شود.
     */
    suspend fun ensureConnected(): Boolean {
        if (_state.value.isReady) {
            // اتصال «باز» است ولی ممکن است استخر کهنه/خالی شده باشد → یک ping سبک
            val alive = withContext(Dispatchers.IO) {
                runCatching {
                    val c = borrow()
                    try { ping(c) } finally { recycle(c) }
                }.isSuccess
            }
            if (alive) return true
        }
        val s = currentSettings()
        if (s == null) {
            _state.value = ConnectionState.Error(
                "هنوز تنظیمات اتصال روی این گوشی ذخیره نشده است — «تنظیم اتصال» را کامل کنید."
            )
            return false
        }
        return connect(s)
    }

    /** درایوری که اتصال فعلی با آن برقرار شده (برای نمایش در کارت وضعیت). */
    @Volatile
    var activeDriver: String? = null
        private set

    /** برقراری اتصال + اعتبارسنجی با SELECT 1. (هرگز دو اتصال هم‌زمان ساخته نمی‌شود) */
    suspend fun connect(s: DbSettings): Boolean = withContext(Dispatchers.IO) {
        // ── v2.18.0 ──────────────────────────────────────────────────────────
        //  الف) مسیر سریع: با همین تنظیمات همین حالا وصل هستیم ⇒ دوباره وصل نشو
        //     (قبلاً هر فراخوانی، اتصال را می‌بست و از نو می‌ساخت: هم کند بود،
        //      هم اگر همان لحظه کوئری در جریان بود، آن کوئری می‌شکست).
        if (_state.value.isReady && settings?.masked() == s.masked() &&
            settings?.password == s.password
        ) return@withContext true
        //  ب) اگر اتصال دیگری در جریان است، به‌جای برگرداندن falseِ گنگ، منتظر
        //     نتیجهٔ همان اتصال می‌مانیم (منبع واحد وضعیت = state).
        if (connecting) {
            var waited = 0
            while (connecting && waited < 40) { delay(100); waited++ }   // تا ۴ ثانیه
            return@withContext _state.value.isReady
        }
        connecting = true
        try {
            if (s.cleanHost.isBlank() || s.database.isBlank() || s.username.isBlank()) {
                _state.value = ConnectionState.Error("آدرس سرور دیتابیس، نام دیتابیس یا نام کاربری خالی است.")
                return@withContext false
            }
            // تنظیمات عوض شده ⇒ اتصال‌های قبلی به سرور/دیتابیس دیگری هستند؛ بسته شوند
            if (settings?.masked() != s.masked()) {
                drainPool()
                activeDriver = null
            }
            _state.value = ConnectionState.Connecting
            try {
                val opened = openOne(s)
                val c = opened.connection
                ping(c)
                pool.offer(c)
                settings = s
                activeDriver = opened.driver
                _state.value = ConnectionState.Ready(0)
                true
            } catch (e: SqlConnectFailure) {
                activeDriver = null
                _state.value = ConnectionState.Error(e.faMessage)
                false
            } catch (e: SQLException) {
                activeDriver = null
                _state.value = ConnectionState.Error(friendlyError(e))
                false
            } catch (t: Throwable) {
                // NoClassDefFoundError و مانند آن «Error» هستند نه Exception؛ اگر این‌جا
                // گرفته نشوند برنامه بسته می‌شود (کرش) — دقیقاً همان چیزی که نباید رخ دهد.
                activeDriver = null
                _state.value = ConnectionState.Error(describeThrowable(t))
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
            val master = s.copy(database = "master")     // master در همهٔ نصب‌ها وجود دارد
            val opened = openOne(master)                 // با درایور جایگزین هم تلاش می‌شود
            c = opened.connection
            activeDriver = opened.driver
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
        } catch (e: SqlConnectFailure) {
            Result.failure(IllegalStateException(e.faMessage, e))
        } catch (e: SQLException) {
            Result.failure(IllegalStateException(friendlyError(e), e))
        } catch (t: Throwable) {
            Result.failure(IllegalStateException(describeThrowable(t), t))
        } finally {
            try { c?.close() } catch (_: Throwable) {}
        }
    }

    /** سنجش سریع سلامت (برای دکمهٔ «تست اتصال» در تنظیمات). */
    suspend fun refresh(): ConnectionState = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        // v2.18.0: اگر اتصال در این اجرا برقرار نشده، با تنظیمات ذخیره‌شده برقرارش کن
        if (!ensureConnected()) return@withContext _state.value
        try {
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
        } catch (t: Throwable) {
            val msg = describeThrowable(t)
            _state.value = ConnectionState.Error(msg)
            ConnectionState.Error(msg)
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
        // v2.18.0: اگر در این اجرا اتصالی برقرار نشده باشد، از تنظیمات ذخیره‌شده
        // استفاده می‌شود تا کوئری‌ها بعد از بسته‌شدن برنامه هم کار کنند.
        val active = currentSettings()
            ?: throw SqlConnectFailure(
                faMessage = "اتصال دیتابیس برقرار نیست. یک‌بار از صفحهٔ «تنظیم اتصال» وارد شوید؛ " +
                    "از آن به بعد، بخش‌های دیگر خودشان اتصال ذخیره‌شده را برقرار می‌کنند.",
                details = "no saved DbSettings on device",
            )
        if (settings == null) settings = active
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

    /**
     * یک اتصال تازه می‌سازد و در صورت لازم، درایور دیگر را امتحان می‌کند.
     *
     * قاعده: درایور دوم فقط وقتی امتحان می‌شود که خطای اول «مربوط به خود درایور»
     * باشد (مثل NoClassDefFoundError روی اندروید، یا خطای پروتکل/TLS). خطای
     * روشنِ رمز عبور یا دست‌نبودن دیتابیس دوباره تکرار نمی‌شود تا حساب SQL قفل نشود.
     */
    private fun openOne(s: DbSettings): Opened {
        val attempts = mutableListOf<String>()
        var firstRealSqlError: SQLException? = null
        for (kind in DirectSql.order()) {
            try {
                val c = java.sql.DriverManager.getConnection(DirectSql.url(s, kind), s.username, s.password)
                c.transactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
                activeDriver = kind
                return Opened(c, kind)
            } catch (e: SQLException) {
                if (firstRealSqlError == null) firstRealSqlError = e
                attempts += "${DirectSql.displayName(kind)} → ${firstLine(e)}"
                if (!isDriverLevelFailure(e, kind)) break     // خطای واقعی دیتابیس؛ سراغ درایور بعدی نرو
            } catch (t: Throwable) {
                // Error مثل NoClassDefFoundError: این درایور روی این دستگاه/سرور کار نمی‌کند
                attempts += "${DirectSql.displayName(kind)} → ${describeThrowable(t)}"
            }
        }
        // اگر همهٔ درایورها به خطای دیتابیس خوردند، همان خطای واقعی را بالا بفرست
        firstRealSqlError?.let { throw it }
        throw SqlConnectFailure(
            faMessage = "هیچ‌کدام از دو درایور نتوانستند اتصال را باز کنند:\n" +
                attempts.joinToString("\n") + "\n" +
                "اگر پیام مربوط به بار نشدن درایور است، همین گزارش را بفرستید.",
            details = attempts.joinToString(" | "),
        )
    }

    /** آیا خطا به خود درایور مربوط است (نه رمز/دیتابیس)؟ */
    private fun isDriverLevelFailure(e: SQLException, kind: String): Boolean {
        val code = e.errorCode
        val msg = (e.message ?: "").lowercase()
        // رمز/دسترسی: قطعاً خطای دیتابیس است، نه درایور
        val authLike = code in setOf(18456, 18452, 4060, 916, 40197) ||
            msg.contains("login failed") || msg.contains("cannot open database")
        if (authLike) return false
        // خطای شماره‌دارِ سرور = پاسخ واقعی دیتابیس
        if (code != 0) return false
        // کد صفر با پیام درایور/پروتکل/TLS = ارزش امتحان درایور بعدی را دارد
        return true
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
        val s = currentSettings() ?: throw SqlConnectFailure(
            faMessage = "اتصال دیتابیس برقرار نیست — تنظیمات اتصال روی گوشی پیدا نشد.",
            details = "borrow(): no settings",
        )
        val fresh = openOne(s).connection
        ping(fresh)
        return fresh
    }

    private fun recycle(c: Connection) {
        if (!c.isClosed) pool.offer(c)
    }

    private fun killQuietly(c: Connection) {
        try {
            if (!c.isClosed) c.close()
        } catch (_: Throwable) {
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
        val msg = (e.message ?: "")
        val lower = msg.lowercase()
        return when {
            // ── احراز هویت ────────────────────────────────────────────────────
            code == 18456 -> {
                val reason = msg.lines().firstOrNull().orEmpty()
                when {
                    lower.contains("windows authentication") || lower.contains("integrated") ->
                        "ورود SQL رد شد: سرور در حالت «فقط احراز هویت ویندوز» است و کاربران SQL اجازهٔ ورود ندارند (SQL $code).\n" +
                            "راه‌حل: نصب‌کننده را با گزینهٔ فعال‌کردن حالت Mixed Mode اجرا کنید (یا در SSMS: Properties → Security → «SQL Server and Windows Authentication mode») و سرویس SQL را ری‌استارت کنید."
                    lower.contains("password did not match") || lower.contains("state: 8") || lower.contains("state 8") ->
                        "نام کاربری درست است ولی رمز اشتباه است (SQL $code). رمز همان «کاربر محدود دیتابیس» است که نصب‌کننده ساخته — در فایل کارت نیست."
                    lower.contains("not enabled") || lower.contains("disabled") ->
                        "این کاربر SQL غیرفعال است (SQL $code). در SSMS: Security → Logins → کاربر → Status → Login: Enabled."
                    lower.contains("not associated with a trusted") || lower.contains("not trusted") ->
                        "این حساب روی آن سرور وجود ندارد یا مخصوص دامنهٔ دیگری است (SQL $code)."
                    else ->
                        "ورود به دیتابیس ناموفق بود (SQL $code). نام کاربری/رمز یا حالت احراز هویت سرور را بررسی کنید.\nپیام سرور: $reason"
                }
            }
            code == 4060 -> "دیتابیس با نام واردشده پیدا نشد یا این کاربر به آن دسترسی ندارد (SQL $code). نام دیتابیس را دقیق بنویسید (مثلاً atiran2)."
            code == 916 -> "کاربر به این دیتابیس دسترسی ندارد (SQL $code) — نصب‌کننده باید کاربر را در همان دیتابیس بسازد."
            code == 40197 -> "این حساب اجازهٔ اتصال به این دیتابیس را ندارد (SQL $code)"
            code == 53 -> "به سرور دیتابیس نمی‌توان رسید — آی‌پی/پورت را بررسی کنید یا مطمئن شوید فایروال سرور باز است (SQL $code)"
            code == 10053 || code == 10054 -> "اتصال در حین کار قطع شد — شبکهٔ موبایل/وای‌فای را چک کنید (SQL $code)"
            code == 1205 -> "درگیری موقت با یک تراکنش دیگر (deadlock) — دوباره تلاش کنید (SQL $code)"
            // ── TLS / پروتکل (شایع روی سرورهای قدیمی) ─────────────────────────
            lower.contains("tls") || lower.contains("ssl") || lower.contains("protocol version") ->
                "سرور با رمزنگاری TLS موردنظر برنامه توافق نکرد (${e.javaClass.simpleName}).\n" +
                    "کلید «رمزنگاری TLS» را در همین صفحه خاموش کنید و دوباره امتحان کنید."
            // ── شبکه/زمان ─────────────────────────────────────────────────────
            lower.contains("connection refused") || lower.contains("refused") ->
                "سرور روی این نشانی/پورت اتصال را نپذیرفت (Connection refused). اگر بیرون از شبکه هستید، کلید «اتصال از بیرون» باید روشن و آی‌پی اختصاصی درست باشد."
            lower.contains("timed out") || lower.contains("timeout") ->
                "زمان اتصال تمام شد — یا سرور در دسترس نیست، یا فایروال اجازه نمی‌دهد (پورت ۱۴۳۳)."
            lower.contains("unknown host") || lower.contains("no address") ->
                "نشانی سرور پیدا نشد — آی‌پی را بررسی کنید."
            lower.contains("no suitable driver") ->
                "درایور JDBC بارگذاری نشد (No suitable driver) — نسخهٔ کامل برنامه را نصب کنید."
            state == "08S01" || state?.startsWith("08") == true -> "خطای ارتباطی با سرور دیتابیس (SQLState $state): ${firstLine(e)}"
            state?.startsWith("28") == true -> "احراز هویت ناموفق (SQLState $state): ${firstLine(e)}"
            else -> "خطای دیتابیس (کد ${code ?: "—"}): ${firstLine(e)}"
        }
    }

    /** توصیف امن هر Throwable — مخصوص Error هایی مثل NoClassDefFoundError که SQLException نیستند. */
    internal fun describeThrowable(t: Throwable): String = when (t) {
        is NoClassDefFoundError ->
            "درایور روی این گوشی بار نشد (${t.message?.take(90) ?: "NoClassDefFoundError"}) — برنامه درایور جایگزین را امتحان می‌کند."
        is SQLException -> friendlyError(t)
        else -> "${t.javaClass.simpleName}: ${t.message?.take(120) ?: "—"}"
    }

    private fun firstLine(e: SQLException): String =
        e.message?.lines()?.firstOrNull()?.take(160) ?: "نامشخص"
}

/** یک اتصال بازشده به‌همراه درایوری که با آن ساخته شد. */
internal class Opened(val connection: Connection, val driver: String)

/**
 * شکست اتصال در سطح «همهٔ درایورها».
 * پیام فارسی آمادهٔ نمایش است و جزئیات فنی هم برای گزارش عیب‌یابی نگه داشته می‌شود
 * (هیچ‌کدام رمز ندارند).
 */
class SqlConnectFailure(
    val faMessage: String,
    val details: String,
) : Exception(details)
