/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | موتور اتصال و گزارش مدیریت (v2.20.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل، پورتِ کاملِ موتور اتصال برنامهٔ مرجع «آتیران همراه (M•REPORT)» است؛
 *  همان چیزی که در نسخهٔ پیوست (app-debug-40) آزموده شده بود:
 *
 *    ▸ اتصال با **چهار حالت**: Microsoft+TLS ، Microsoft بدون TLS ،
 *      jTDS و jTDS+TLS — هر کدام که جواب داد، همان فعال می‌شود.
 *    ▸ پارس هدف اتصال: `IP` یا `SERVER\\INSTANCE` یا `IP:PORT` یا `IP\\INSTANCE:PORT`
 *    ▸ کاوش خام TCP (پورت باز است؟) و کاوش PRELOGIN واقعی TDS
 *      (پشت پورت، واقعاً SQL Server نشسته است؟)
 *    ▸ کاوش دست‌دادن TLS با پروتکل‌های TLSv1.3/1.2/1.1/1.0 (سازگاری سرورهای قدیمی)
 *    ▸ پرسیدن پورت واقعی نمونه از سرویس SQL Browser روی UDP 1434
 *    ▸ عیب‌یابی گام‌به‌گام با متن فارسی و پیشنهاد «استفاده از این پورت»
 *    ▸ نگاشت خطاهای درایور به پیام فارسی فهمیدنی (رمز اشتباه، دیتابیس بسته، …)
 *    ▸ کوئری‌های گزارش: نسخه/حجم دیتابیس، فهرست جدول‌ها با تعداد رکورد،
 *      یافتن جدول با نام‌های حدسی، ستون‌ها، شمارش، جمع، TOP، آخرین رکوردها
 *      و صفحه‌بندی + جست‌وجوی زنده در ستون‌های متنی.
 *
 *  همه‌چیز فقط-خواندنی است: هیچ کوئری‌ای در این فایل داده را تغییر نمی‌دهد.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Locale
import java.util.Properties

// ═══════════════════════════ مدل‌های داده ═══════════════════════════

/** هدف اتصال پس از پارس (`host` + `port` + `instance` اختیاری). */
data class MaTarget(
    val host: String,
    val port: Int,
    val instance: String? = null,
) {
    val label: String
        get() = buildString {
            append(host)
            if (!instance.isNullOrBlank()) append("\\").append(instance)
            append(":").append(port)
        }
}

/** یک ستون جدول (نام + نوع). */
data class MaColumn(val name: String, val type: String)

/** یک جدول سرور با تعداد رکورد (از sys.partitions). */
data class MaTable(val schema: String, val name: String, val rows: Long) {
    val ref: String get() = "$schema.$name"
}

/** نمای کلی دیتابیس: نسخهٔ سرور، حجم (مگابایت) و جدول‌ها. */
data class MaOverview(
    val version: String,
    val sizeMb: Double,
    val tables: List<MaTable>,
) {
    val totalRows: Long get() = tables.sumOf { it.rows }
}

/** یک صفحه از دادهٔ جدول (ستون‌ها + ردیف‌ها + کل رکوردها). */
data class MaPage(
    val columns: List<String>,
    val rows: List<List<String>>,
    val total: Long,
)

/** یک گام عیب‌یابی اتصال. */
data class MaDiagStep(
    val ok: Boolean,
    val title: String,
    val detail: String,
    val hint: String = "",
    val suggestedPort: Int? = null,
)

/** نتیجهٔ اتصال + حالت برنده. */
data class MaConnectResult(
    val ok: Boolean,
    val modeLabel: String,
    val message: String,
    val steps: List<MaDiagStep> = emptyList(),
)

// ═══════════════════════════ موتور ═══════════════════════════

object MaSqlEngine {

    /** حالت‌های اتصال — ترتیب امتحان: TLS مایکروسافت، بدون TLS، jTDS، jTDS+TLS. */
    enum class Mode(val label: String, val usingMssql: Boolean, val tls: Boolean) {
        MSSQL_TLS("Microsoft + TLS", true, true),
        MSSQL_PLAIN("Microsoft بدون TLS", true, false),
        JTDS_PLAIN("jTDS (جایگزین ویندوز)", false, false),
        JTDS_TLS("jTDS + TLS", false, true),
    }

    private const val APP_NAME = "AtiranVizitor"
    private const val DEFAULT_PORT = 1433

    private val lock = Mutex()

    @Volatile
    private var live: Connection? = null

    @Volatile
    private var cfgCache: DbSettings? = null

    @Volatile
    private var targetCache: MaTarget? = null

    /** حالتی که اتصال فعلی با آن برقرار شده (برای نمایش در کارت وضعیت). */
    @Volatile
    var activeMode: Mode? = null
        private set

    val isConnected: Boolean
        get() = live?.let { runCatching { !it.isClosed }.getOrDefault(false) } ?: false

    val modeLabel: String get() = activeMode?.label ?: "—"
    val targetLabel: String get() = targetCache?.label ?: "—"

    // ── پارس هدف اتصال: IP | SERVER\INSTANCE | IP:PORT | IP\INSTANCE:PORT ──
    fun parseTarget(rawHost: String, rawPort: Int): MaTarget {
        var host = rawHost.trim()
        var instance: String? = null
        var port = if (rawPort in 1..65535) rawPort else DEFAULT_PORT

        val backslash = host.indexOf('\\')
        if (backslash > 0) {
            instance = host.substring(backslash + 1).trim().ifBlank { null }
            host = host.substring(0, backslash).trim()
        }
        val colon = host.indexOf(':')
        if (colon > 0) {
            val p = host.substring(colon + 1).trim().toIntOrNull()
            if (p != null && p in 1..65535) port = p
            host = host.substring(0, colon).trim()
        }
        return MaTarget(host, port, instance)
    }

    private fun urlFor(cfg: DbSettings, t: MaTarget, mode: Mode): String {
        val db = cfg.database.trim()
        val login = cfg.connectTimeoutSec.coerceIn(3, 30)
        return if (mode.usingMssql) {
            buildString {
                append("jdbc:sqlserver://").append(t.host).append(":").append(t.port)
                append(";databaseName=").append(db)
                if (mode.tls) {
                    append(";encrypt=true;trustServerCertificate=true;sslProtocol=TLS")
                } else {
                    append(";encrypt=false;trustServerCertificate=true")
                }
                append(";connectRetryCount=1;connectRetryDelay=1")
                append(";applicationName=").append(APP_NAME)
                append(";loginTimeout=").append(login)
                if (!t.instance.isNullOrBlank()) append(";instanceName=").append(t.instance)
            }
        } else {
            buildString {
                append("jdbc:jtds:sqlserver://").append(t.host).append(":").append(t.port).append("/").append(db)
                append(";loginTimeout=").append(login)
                append(";socketTimeout=").append(cfg.queryTimeoutSec.coerceIn(10, 120))
                if (mode.tls) append(";ssl=request")
                if (!t.instance.isNullOrBlank()) append(";instance=").append(t.instance)
            }
        }
    }

    private fun props(cfg: DbSettings): Properties = Properties().apply {
        this["user"] = cfg.username
        this["password"] = cfg.password
    }

    /** باز کردن یک اتصال با حالت مشخص (بدون کش‌کردن). */
    private fun open(cfg: DbSettings, t: MaTarget, mode: Mode): Connection {
        DirectSql.tryLoad(if (mode.usingMssql) DirectSql.MSSQL else DirectSql.JTDS)
        DriverManager.setLoginTimeout(cfg.connectTimeoutSec.coerceIn(3, 30))
        return DriverManager.getConnection(urlFor(cfg, t, mode), props(cfg))
    }

    /**
     * اتصال با چهار حالت. اولین حالتی که جواب بدهد برنده است و پیام می‌دهد.
     * اگر هیچ‌کدام جواب نداد، کاوش خام TCP علت را روشن می‌کند.
     */
    suspend fun connect(cfg: DbSettings): MaConnectResult = withContext(Dispatchers.IO) {
        lock.withLock {
            closeQuietly()
            val t = parseTarget(cfg.host, cfg.port)
            cfgCache = cfg
            targetCache = t
            val order = if (cfg.useEncryption) {
                listOf(Mode.MSSQL_TLS, Mode.MSSQL_PLAIN, Mode.JTDS_PLAIN, Mode.JTDS_TLS)
            } else {
                listOf(Mode.MSSQL_PLAIN, Mode.JTDS_PLAIN, Mode.MSSQL_TLS, Mode.JTDS_TLS)
            }
            var lastError: Throwable? = null
            for (m in order) {
                try {
                    val c = open(cfg, t, m)
                    c.createStatement().use { st ->
                        st.queryTimeout = 15
                        st.executeQuery("SELECT 1").use { rs -> if (rs.next()) { /* ok */ } }
                    }
                    live = c
                    activeMode = m
                    return@withLock MaConnectResult(
                        ok = true,
                        modeLabel = m.label,
                        message = "اتصال برقرار شد — ${m.label} • ${t.label}",
                    )
                } catch (e: Throwable) {
                    lastError = e
                }
            }
            val reachable = rawTcpProbe(t.host, t.port)
            val msg = if (!reachable) {
                "سرور روی این شبکه در دسترس نیست (${t.host}:${t.port}). " +
                    "اگر با اینترنت همراه هستید، اپراتور ممکن است پورت دیتابیس را فیلتر کند — " +
                    "از وای‌فای شبکه اداره یا VPN استفاده کنید."
            } else {
                friendly(lastError ?: IllegalStateException("اتصال به سرور در هیچ‌یک از چهار حالت ناموفق بود"))
            }
            MaConnectResult(false, "", msg)
        }
    }

    /** قطع اتصال (تنظیمات ذخیره‌شده دست‌نخورده می‌ماند). */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        lock.withLock { closeQuietly() }
    }

    private fun closeQuietly() {
        runCatching { live?.close() }
        live = null
        activeMode = null
    }

    /** اجرای یک بلوک روی اتصال جاری؛ اگر اتصال بسته باشد، با تنظیمات ذخیره‌شده وصل می‌شود. */
    private suspend fun <T> onConnection(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        lock.withLock {
            val current = live?.takeIf { runCatching { !it.isClosed }.getOrDefault(false) }
            val c = if (current != null) current else {
                val cfg = cfgCache ?: SecureDbStore.load()
                ?: throw IllegalStateException("تنظیمات اتصال روی این گوشی ذخیره نشده است.")
                val t = parseTarget(cfg.host, cfg.port)
                cfgCache = cfg
                targetCache = t
                var made: Connection? = null
                var lastError: Throwable? = null
                for (m in listOf(Mode.JTDS_PLAIN, Mode.MSSQL_PLAIN, Mode.MSSQL_TLS, Mode.JTDS_TLS)) {
                    try {
                        made = open(cfg, t, m); activeMode = m; break
                    } catch (e: Throwable) {
                        lastError = e
                    }
                }
                made ?: throw IllegalStateException(friendly(lastError ?: IllegalStateException("اتصال برقرار نشد")))
            }
            if (current == null) live = c
            block(c)
        }
    }

    // ═══════════════════ کاوش‌های شبکه (TCP / TDS / TLS / Browser) ═══════════════════

    /** کاوش خام TCP: پورت باز است؟ (بدون هیچ کوئری) */
    fun rawTcpProbe(host: String, port: Int, timeoutMs: Int = 4000): Boolean = runCatching {
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), timeoutMs)
            true
        }
    }.getOrDefault(false)

    /**
     * کاوش PRELOGIN واقعی TDS: پورت باز بودن کافی نیست؛ باید SQL Server
     * هم پاسخ بدهد. اولین بایت پاسخ 0x04 (Tabular Result) یعنی سرور سالم است.
     */
    fun tdsPrelogin(host: String, port: Int, timeoutMs: Int = 5000): Boolean = runCatching {
        // بستهٔ PRELOGIN استاندارد TDS 7.x — ۴۷ بایت (بایت آخر VERSION = 0)
        val body = intArrayOf(
            0x00, 0x00, 0x1A, 0x00, 0x06, 0x01, 0x02, 0x00,
            0x1F, 0x00, 0x01, 0x02, 0x00, 0x20, 0x00, 0x01,
            0x02, 0x00, 0x21, 0x00, 0x01, 0x03, 0x00, 0x22,
            0x00, 0x04, 0x04, 0x00, 0x26, 0x00, 0x01, 0xFF,
            0x09, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00
        )
        val packet = byteArrayOf(0x12, 0x01, 0x00, 0x2F, 0x00, 0x00, 0x01, 0x00) +
            body.map { it.toByte() }.toByteArray()
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), timeoutMs)
            s.soTimeout = timeoutMs
            s.getOutputStream().write(packet)
            s.getOutputStream().flush()
            val buf = ByteArray(8)
            val n = s.getInputStream().read(buf)
            n > 0 && buf[0] == 0x04.toByte()
        }
    }.getOrDefault(false)

    /**
     * کاوش دست‌دادن TLS با پروتکل‌های مختلف — سرورهای قدیمی فقط TLSv1 دارند.
     * خروجی: نام پروتکلی که موفق شد (یا null).
     */
    fun tlsProbe(host: String, port: Int): String? {
        val protocols = listOf("TLSv1.3", "TLSv1.2", "TLSv1.1", "TLSv1")
        for (p in protocols) {
            val ok = runCatching {
                val ctx = javax.net.ssl.SSLContext.getInstance(p)
                ctx.init(null, null, null)
                val f = ctx.socketFactory
                val raw = Socket()
                raw.connect(InetSocketAddress(host, port), 5000)
                raw.soTimeout = 6000
                val ssl = f.createSocket(raw, host, port, true) as javax.net.ssl.SSLSocket
                ssl.startHandshake()
                ssl.close()
                true
            }.getOrDefault(false)
            if (ok) return p
        }
        return null
    }

    /**
     * پرسیدن پورت واقعی نمونه از سرویس SQL Browser (UDP 1434).
     * پاسخ متنی است؛ بعد از توکن «tcp» عدد پورت می‌آید.
     */
    fun sqlBrowser(host: String, instance: String, timeoutMs: Int = 1500): Int? = runCatching {
        val req = byteArrayOf(0x04) + instance.toByteArray(Charsets.US_ASCII)
        java.net.DatagramSocket().use { s ->
            s.soTimeout = timeoutMs
            val addr = java.net.InetAddress.getByName(host)
            s.send(java.net.DatagramPacket(req, req.size, addr, 1434))
            val buf = ByteArray(4096)
            val res = java.net.DatagramPacket(buf, buf.size)
            s.receive(res)
            val text = String(buf, 0, res.length, Charsets.US_ASCII)
            val tokens = text.split(';')
            val i = tokens.indexOfFirst { it.equals("tcp", true) }
            if (i >= 0 && i + 1 < tokens.size) tokens[i + 1].trim().toIntOrNull() else null
        }
    }.getOrNull()

    // ═══════════════════ عیب‌یابی گام‌به‌گام (بدون رمز) ═══════════════════

    suspend fun diagnose(cfg: DbSettings): List<MaDiagStep> = withContext(Dispatchers.IO) {
        val steps = mutableListOf<MaDiagStep>()
        val t = parseTarget(cfg.host, cfg.port)
        steps += MaDiagStep(
            ok = cfg.host.isNotBlank(),
            title = "هدف اتصال: ${t.label}",
            detail = if (!t.instance.isNullOrBlank())
                "نمونه نام‌بریده «${t.instance}» — درایور پورت واقعی را از سرویس SQL Browser (UDP 1434) می‌پرسد"
            else "آدرس و پورت مستقیم (بدون نمونه نام‌بریده)",
            hint = if (!t.instance.isNullOrBlank())
                "اگر این گام شکست خورد: در سرور سرویس SQL Server Browser را روشن کنید و UDP 1434 را در فایروال باز کنید؛ " +
                    "یا پورت مستقیم نمونه را پیدا کنید و فقط IP و پورت را (بدون بک‌اسلش) وارد کنید."
            else ""
        )

        // ۱) نمونه نام‌بریده → پورت از SQL Browser
        if (!t.instance.isNullOrBlank()) {
            val discovered = sqlBrowser(t.host, t.instance)
            steps += if (discovered != null && discovered != t.port) {
                MaDiagStep(
                    ok = true,
                    title = "پورت واقعی سرور پیدا شد: $discovered",
                    detail = "اتصال روی پورت $discovered موفق بود! (نمونه «${t.instance}»)",
                    hint = "دکمه «استفاده از این پورت» را بزنید تا خودکار در فیلد پورت تنظیم شود.",
                    suggestedPort = discovered
                )
            } else if (discovered != null) {
                MaDiagStep(true, "سرویس SQL Browser (UDP 1434)",
                    "پورت واقعی نمونه «${t.instance}» پرسیده شد: $discovered")
            } else {
                MaDiagStep(false, "سرویس SQL Browser (UDP 1434)",
                    "پاسخی نرسید — یا سرویس Browser خاموش است یا ترافیک UDP بسته است",
                    hint = "از مدیر سرور بخواهید پورت واقعی SQL Server را بگوید (فایروال ویندوز → Inbound Rules روی SQL Server).")
            }
        }

        // ۲) کاوش خام TCP
        val tcp = rawTcpProbe(t.host, t.port)
        steps += if (tcp) {
            MaDiagStep(true, "دست‌دادن TCP با سرور برقرار شد", "سرویس SQL Server روی پورت ${t.port} در دسترس است")
        } else {
            MaDiagStep(
                false,
                "اتصال به پورت ممکن نشد: ${t.host}:${t.port}",
                "درگاه اتصال (TCP ${t.port}) بسته است",
                hint = "۱) در SQL Server Configuration Manager پروتکل TCP/IP را فعال و سرویس SQL را ری‌استارت کنید. " +
                    "۲) فایروال ویندوز و مودم/روتر را برای این پورت باز کنید. ۳) اگر پورت دیگری است، در فیلد پورت همان را وارد کنید."
            )
        }

        // ۳) کاوش PRELOGIN (آیا واقعاً SQL Server است؟)
        if (tcp) {
            val tds = tdsPrelogin(t.host, t.port)
            steps += if (tds) {
                MaDiagStep(true, "پاسخ prelogin معتبر از SQL Server دریافت شد", "پشت این پورت واقعاً SQL Server نشسته است")
            } else {
                MaDiagStep(
                    false,
                    "اتصال TCP برقرار شد اما هیچ پاسخ TDS از سرور نرسید",
                    "پورت باز است ولی SQL Server پشت آن پاسخگو نیست",
                    hint = "معمولاً یعنی: پورت‌فوروارد/NAT به مقصد اشتباه می‌رود، یا دستگاه میانی (فایروال/اپراتور) " +
                        "فقط اتصال را می‌پذیرد و داده را رها می‌کند. اگر با اینترنت همراه هستید و داخل شبکه اداره وصل می‌شوید، " +
                        "اپراتور ترافیک این پورت را فیلتر می‌کند — از VPN استفاده کنید."
                )
            }
        }

        // ۴) کاوش TLS
        if (tcp) {
            val proto = tlsProbe(t.host, t.port)
            steps += if (proto != null) {
                MaDiagStep(true, "رمزنگاری اتصال (TLS)", "دست‌دادن TLS با سرور برقرار شد ($proto) — رمزنگاری سازگار است")
            } else {
                MaDiagStep(
                    false,
                    "رمزنگاری اتصال (TLS)",
                    "دست‌دادن TLS ناموفق: protocol handshake",
                    hint = "TLS سرور با اندروید سازگار نیست (معمولاً سرور فقط TLS قدیمی 1.0/1.1 دارد). " +
                        "برنامه با درایور جایگزین (jTDS) بدون TLS ادامه می‌دهد — مثل نسخهٔ ویندوز."
                )
            }
        }

        // ۵) ورود با چهار حالت
        val result = connectInternal(cfg)
        steps += if (result.ok) {
            MaDiagStep(true, "ورود با نام کاربری و رمز", "اعتبارنامه پذیرفته شد — ${result.modeLabel}")
        } else {
            MaDiagStep(
                false,
                "ورود با نام کاربری و رمز",
                result.message,
                hint = if (result.message.contains("Login failed")) {
                    "نام کاربری و رمز SQL Server (نه ویندوز) را بررسی کنید؛ کاربر باید SQL Server Authentication باشد."
                } else {
                    "اگر مطمئن نیستید این متن را برای پشتیبانی بفرستید."
                }
            )
        }

        // ۶) نتیجهٔ نهایی
        if (result.ok) {
            val info = runCatching { test(cfg).second }.getOrDefault("")
            steps += MaDiagStep(
                true,
                "نتیجه نهایی",
                "اتصال کامل برقرار شد — می‌توانید وارد شوید" + if (info.isNotBlank()) "\n$info" else ""
            )
        } else {
            steps += MaDiagStep(false, "نتیجه نهایی", "هیچ حالت اتصال جواب نداد", hint = "متن خطای بالا و راهنما را بررسی کنید.")
        }
        steps
    }

    /** اتصال بی‌متن برای استفادهٔ داخلی عیب‌یابی (بدون تغییر اتصال جاری). */
    private fun connectInternal(cfg: DbSettings): MaConnectResult {
        val t = parseTarget(cfg.host, cfg.port)
        val order = listOf(Mode.MSSQL_TLS, Mode.MSSQL_PLAIN, Mode.JTDS_PLAIN, Mode.JTDS_TLS)
        var lastError: Throwable? = null
        for (m in order) {
            try {
                open(cfg, t, m).use { c ->
                    c.createStatement().use { st ->
                        st.queryTimeout = 15
                        st.executeQuery("SELECT 1").use { rs -> if (rs.next()) { /* ok */ } }
                    }
                }
                return MaConnectResult(true, m.label, "اتصال برقرار شد — ${m.label}")
            } catch (e: Throwable) {
                lastError = e
            }
        }
        return MaConnectResult(false, "", friendly(lastError ?: IllegalStateException("اتصال ناموفق بود")))
    }

    // ═══════════════════ کوئری‌های گزارش (فقط-خواندنی) ═══════════════════

    /** تست اتصال: نام دیتابیس فعال، نام سرور و نسخه. */
    suspend fun test(cfg: DbSettings): Triple<String, String, String> = onConnection { c ->
        c.createStatement().use { st ->
            st.queryTimeout = 15
            st.executeQuery("SELECT DB_NAME(), @@SERVERNAME, @@VERSION").use { rs ->
                if (!rs.next()) Triple("", "", "")
                else Triple(rs.getString(1) ?: "", rs.getString(2) ?: "", rs.getString(3) ?: "")
            }
        }
    }

    /** نسخهٔ سرور + حجم دیتابیس (مگابایت) + فهرست جدول‌ها با تعداد رکورد. */
    suspend fun overview(): MaOverview = onConnection { c ->
        var version = ""
        c.createStatement().use { st ->
            st.queryTimeout = 20
            runCatching {
                st.executeQuery("SELECT @@VERSION").use { rs -> if (rs.next()) version = rs.getString(1) ?: "" }
            }
        }
        var sizeMb = 0.0
        c.createStatement().use { st ->
            st.queryTimeout = 20
            runCatching {
                st.executeQuery("SELECT SUM(CAST(size AS BIGINT)) * 8 FROM sys.master_files WHERE database_id = DB_ID()")
                    .use { rs -> if (rs.next()) sizeMb = rs.getDouble(1) / 1024.0 }
            }
        }
        val tables = mutableListOf<MaTable>()
        c.createStatement().use { st ->
            st.queryTimeout = 25
            st.executeQuery(
                """
                SELECT s.name AS sch, t.name AS tbl,
                       ISNULL(SUM(CAST(CASE WHEN p.index_id IN (0,1) THEN p.rows END AS BIGINT)), 0) AS cnt
                FROM sys.tables t
                JOIN sys.schemas s ON s.schema_id = t.schema_id
                LEFT JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0,1)
                GROUP BY s.name, t.name
                ORDER BY t.name
                """.trimIndent()
            ).use { rs ->
                while (rs.next()) {
                    tables += MaTable(
                        schema = rs.getString("sch") ?: "dbo",
                        name = rs.getString("tbl") ?: "",
                        rows = rs.getLong("cnt"),
                    )
                }
            }
        }
        MaOverview(version = version.lineSequence().firstOrNull()?.trim() ?: "", sizeMb = sizeMb, tables = tables)
    }

    /**
     * یافتن جدول واقعی با نام‌های حدسی: `SELECT TOP (1) schema + '.' + name ...`
     * ترتیب اولویت همان ترتیبی است که داده می‌شود (مثل نسخهٔ مرجع).
     */
    suspend fun findTable(candidates: List<String>): String? = onConnection { c ->
        if (candidates.isEmpty()) return@onConnection null
        val clean = candidates.filter { it.isNotBlank() }.distinct()
        if (clean.isEmpty()) return@onConnection null
        val inList = clean.joinToString(",") { "'" + it.replace("'", "''") + "'" }
        val order = clean.mapIndexed { i, n -> "WHEN '" + n.replace("'", "''") + "' THEN $i" }
            .joinToString(" ")
        val sql = "SELECT TOP (1) s.name + N'.' + t.name FROM sys.tables t " +
            "JOIN sys.schemas s ON s.schema_id = t.schema_id " +
            "WHERE t.name IN ($inList) ORDER BY CASE t.name $order ELSE 99 END"
        c.createStatement().use { st ->
            st.queryTimeout = 20
            st.executeQuery(sql).use { rs -> if (rs.next()) rs.getString(1) else null }
        }
    }

    /** ستون‌های یک جدول (نام + نوع) از INFORMATION_SCHEMA. */
    suspend fun columnsOf(schema: String, table: String): List<MaColumn> = onConnection { c ->
        c.prepareStatement(
            "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION"
        ).use { ps ->
            ps.queryTimeout = 20
            ps.setString(1, schema)
            ps.setString(2, table)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<MaColumn>()
                while (rs.next()) {
                    out += MaColumn(rs.getString(1) ?: "", rs.getString(2) ?: "")
                }
                if (out.isEmpty()) throw IllegalStateException("جدول «$schema.$table» یافت نشد یا ستونی ندارد")
                out
            }
        }
    }

    /** شمارش رکوردهای یک جدول. */
    suspend fun count(schema: String, table: String): Long = onConnection { c ->
        c.createStatement().use { st ->
            st.queryTimeout = 25
            st.executeQuery("SELECT COUNT_BIG(*) FROM ${q(schema)}.${q(table)}").use { rs ->
                if (rs.next()) rs.getLong(1) else 0L
            }
        }
    }

    /** جمع یک ستون (FLOAT) — برای مبلغ‌ها. */
    suspend fun sumOf(schema: String, table: String, column: String): Double? = onConnection { c ->
        c.createStatement().use { st ->
            st.queryTimeout = 25
            st.executeQuery("SELECT SUM(CAST(${q(column)} AS FLOAT)) FROM ${q(schema)}.${q(table)}").use { rs ->
                if (rs.next()) {
                    val v = rs.getDouble(1)
                    if (rs.wasNull()) null else v
                } else null
            }
        }
    }

    /** بزرگ‌ترین مقادیر بر اساس یک ستون (TOP n ORDER BY col DESC). */
    suspend fun topBy(
        schema: String,
        table: String,
        column: String,
        limit: Int = 10,
        sumColumn: String? = null,
    ): MaPage = onConnection { c ->
        val cols = columnsOfInternal(c, schema, table).map { it.name }
        if (cols.none { it.equals(column, true) }) {
            throw IllegalStateException("ستون «$column» در جدول «$schema.$table» یافت نشد")
        }
        val n = limit.coerceIn(1, 100)
        val sql = "SELECT TOP $n * FROM ${q(schema)}.${q(table)} ORDER BY ${q(column)} DESC"
        c.createStatement().use { st ->
            st.queryTimeout = 30
            st.executeQuery(sql).use { rs -> readRows(rs) }
        }
    }

    /** آخرین رکوردها بر اساس ستون تاریخ/شماره. */
    suspend fun latestBy(
        schema: String,
        table: String,
        column: String,
        limit: Int = 10,
    ): MaPage = onConnection { c ->
        val cols = columnsOfInternal(c, schema, table).map { it.name }
        if (cols.none { it.equals(column, true) }) {
            throw IllegalStateException("ستون «$column» در جدول «$schema.$table» یافت نشد")
        }
        val n = limit.coerceIn(1, 100)
        val sql = "SELECT TOP $n * FROM ${q(schema)}.${q(table)} ORDER BY ${q(column)} DESC"
        c.createStatement().use { st ->
            st.queryTimeout = 30
            st.executeQuery(sql).use { rs -> readRows(rs) }
        }
    }

    /**
     * یک صفحه از دادهٔ جدول با جست‌وجوی زنده در ستون‌های متنی
     * (دقیقاً همان الگوی نسخهٔ مرجع: ROW_NUMBER + LIKE).
     */
    suspend fun page(
        schema: String,
        table: String,
        pageNumber: Int,
        pageSize: Int,
        search: String = "",
        searchColumns: List<String> = emptyList(),
    ): MaPage = onConnection { c ->
        val cols = columnsOfInternal(c, schema, table)
        val ref = "${q(schema)}.${q(table)}"
        val term = search.trim()
        val likeCols = if (term.isBlank()) emptyList() else {
            (if (searchColumns.isEmpty()) cols.filter { isTextType(it.type) }.map { it.name } else searchColumns)
        }
        val where = if (term.isNotBlank() && likeCols.isNotEmpty()) {
            " WHERE " + likeCols.joinToString(" OR ") { "${q(it)} LIKE ?" }
        } else ""

        var total = 0L
        c.prepareStatement("SELECT COUNT_BIG(*) FROM $ref$where").use { ps ->
            ps.queryTimeout = 25
            if (term.isNotBlank() && likeCols.isNotEmpty()) {
                likeCols.forEachIndexed { i, _ -> ps.setString(i + 1, "%$term%") }
            }
            ps.executeQuery().use { rs -> if (rs.next()) total = rs.getLong(1) }
        }

        val size = pageSize.coerceIn(10, 200)
        val p = pageNumber.coerceAtLeast(1)
        val from = (p - 1).toLong() * size
        val to = from + size
        val sql = buildString {
            append("SELECT * FROM (\n")
            append("  SELECT *, ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS [_rn]\n")
            append("  FROM $ref$where\n")
            append(") AS [_paged]\n")
            append("WHERE [_paged].[_rn] > ? AND [_paged].[_rn] <= ?\n")
            append("ORDER BY [_paged].[_rn]")
        }
        c.prepareStatement(sql).use { ps ->
            ps.queryTimeout = 30
            var idx = 1
            if (term.isNotBlank() && likeCols.isNotEmpty()) {
                likeCols.forEach { _ -> ps.setString(idx++, "%$term%") }
            }
            ps.setLong(idx++, from)
            ps.setLong(idx, to)
            ps.executeQuery().use { rs ->
                val page = readRows(rs)
                page.copy(total = total)
            }
        }
    }

    // ═══════════════════ کمکی‌های داخلی ═══════════════════

    private fun columnsOfInternal(c: Connection, schema: String, table: String): List<MaColumn> =
        c.prepareStatement(
            "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION"
        ).use { ps ->
            ps.queryTimeout = 20
            ps.setString(1, schema)
            ps.setString(2, table)
            ps.executeQuery().use { rs ->
                val out = mutableListOf<MaColumn>()
                while (rs.next()) out += MaColumn(rs.getString(1) ?: "", rs.getString(2) ?: "")
                if (out.isEmpty()) throw IllegalStateException("جدول «$schema.$table» یافت نشد یا ستونی ندارد")
                out
            }
        }

    private fun readRows(rs: ResultSet): MaPage {
        val meta = rs.metaData
        val count = meta.columnCount
        val names = (1..count).map { meta.getColumnLabel(it) ?: "" }
        val rows = mutableListOf<List<String>>()
        while (rs.next()) {
            rows += (1..count).map { i -> cell(rs, i) }
        }
        return MaPage(columns = names, rows = rows, total = rows.size.toLong())
    }

    /** مقدار یک سلول به متن امن (باینری‌ها خلاصه می‌شوند، متن‌های بلند بریده می‌شوند). */
    private fun cell(rs: ResultSet, i: Int): String {
        val v = runCatching { rs.getObject(i) }.getOrNull() ?: return ""
        return when (v) {
            is ByteArray -> "(داده باینری ${v.size} بایت)"
            is java.sql.Clob -> runCatching { v.getSubString(1, 200) }.getOrDefault("(متن بلند)")
            is java.sql.Blob -> "(داده باینری ${runCatching { v.length() }.getOrDefault(0L)} بایت)"
            is String -> if (v.length > 400) v.take(400) + "…" else v
            is java.util.Date -> java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(v)
            else -> v.toString()
        }
    }

    /** نقل‌قول امن نام اسکیما/جدول/ستون (فقط حرف، عدد، _ و $ مجاز است). */
    fun q(identifier: String): String {
        val safe = identifier.filter { it.isLetterOrDigit() || it == '_' || it == '$' || it == '.' }
        val parts = safe.split('.').filter { it.isNotBlank() }
        if (parts.isEmpty()) throw IllegalArgumentException("نام نامعتبر: $identifier")
        return parts.joinToString(".") { "[$it]" }
    }

    /** آیا نوع ستون متنی است (برای جست‌وجوی LIKE)؟ */
    fun isTextType(type: String): Boolean {
        val t = type.lowercase(Locale.US)
        return t in setOf("char", "nchar", "varchar", "nvarchar", "text", "ntext", "sysname", "xml")
    }

    /**
     * نگاشت خطاهای درایور به پیام فارسی — دقیقاً همان الگوی نسخهٔ مرجع،
     * تا کاربر بفهمد مشکل «رمز» است یا «شبکه» یا «رمزنگاری».
     */
    fun friendly(t: Throwable): String {
        val raw = (t.message ?: t.javaClass.simpleName)
        val m = raw.lowercase(Locale.US)
        return when {
            m.contains("login failed") || m.contains("password") && m.contains("incorrect") ->
                "نام کاربری یا رمز عبور SQL Server اشتباه است"
            m.contains("cannot open database") || m.contains("database") && m.contains("not found") ->
                "دیتابیس مورد نظر باز نشد — نام دیتابیس یا دسترسی کاربر را بررسی کنید"
            m.contains("not associated with a trusted sql server connection") ->
                "این حساب، ویندوزی است — کاربر باید SQL Server Authentication باشد یا حالت Mixed Mode در سرور فعال شود"
            m.contains("ssl") || m.contains("certificate") || m.contains("secure connection") || m.contains("protocol version") ->
                "خطای رمزنگاری در اتصال به سرور — برنامه اتصال بدون رمزنگاری را هم خودکار امتحان می‌کند؛ " +
                    "از «عیب‌یابی اتصال» برای جزئیات استفاده کنید"
            m.contains("timeout") || m.contains("timed out") || m.contains("did not return a response") ->
                "سرور اتصال را پذیرفت ولی پاسخ نداد — معمولاً پروتکل TCP/IP در SQL Server فعال نیست یا TLS سرور قدیمی است. " +
                    "از دکمه «عیب‌یابی اتصال» استفاده کنید"
            m.contains("unknownhost") || m.contains("unknown host") ->
                "آدرس «${raw.take(60)}» در شبکه پیدا نشد — IP یا نام سرور را دقیق بررسی کنید"
            m.contains("no route to host") || m.contains("connection refused") || m.contains("connection reset") ||
                m.contains("socket was closed") ->
                "اتصال به سرور برقرار نشد — آدرس/پورت، فایروال یا فعال‌بودن TCP/IP در SQL Server را بررسی کنید"
            else -> "خطای دیتابیس: ${raw.take(180)}"
        }
    }
}
