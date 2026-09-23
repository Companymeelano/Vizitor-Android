/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران | پروفایل مخفی سرور و اتصال هوشمند (v2.23.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  خواستهٔ مدیر: «اطلاعات سرور جوری تنظیم شود که هوشمند تشخیص داده شود و کاربر
 *  هیچ اطلاعاتی نتواند ببیند؛ فقط نام کاربری و کلمهٔ عبور خودش را وارد کند.»
 *
 *  این فایل سه کار می‌کند:
 *    ۱) نگه‌داشتن مشخصات سرور به‌صورت **مخفی** (داخل کد، Base64 — نه در UI و نه
 *       در فایل‌های قابل خواندن؛ روی صفحه هیچ نشانی/کاربر/رمزی نمایش داده نمی‌شود).
 *    ۲) **تشخیص هوشمند شبکه**: اگر گوشی به وای‌فای وصل باشد اول نشانی داخلی
 *       (شبکهٔ اداره) و اگر روی دادهٔ همراه/اینترنت باشد اول نشانی بیرونی امتحان
 *       می‌شود؛ اگر یکی جواب نداد خودکار سراغ دیگری می‌رود.
 *    ۳) اتصال امن و فقط-خواندنی به سرور برای بخش گزارش‌ها (بدون هیچ پرسشی از کاربر).
 *
 *  ⚠️ هیچ‌جای برنامه این مقادیر را چاپ یا نمایش ندهید. برای پیام‌ها از
 *     `MaServerProfile.safe()` استفاده کنید که نشانی‌ها را «سرور آتیران» می‌کند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64

/** نوع شبکه‌ای که کاربر روی آن است (برای انتخاب هوشمند نشانی). */
enum class MaNetKind(val label: String) {
    WIFI("وای‌فای / شبکهٔ داخلی"),
    MOBILE("اینترنت همراه"),
    ETHERNET("شبکهٔ کابلی"),
    NONE("بدون شبکه"),
    OTHER("شبکهٔ نامشخص"),
}

/** نتیجهٔ اتصال هوشمند (همهٔ برچسب‌ها برای نمایش امن‌اند — بدون نشانی و رمز). */
data class MaSmartConnect(
    val ok: Boolean,
    val netLabel: String,
    val channelLabel: String,
    val modeLabel: String,
    val message: String,
)

/**
 * پروفایل مخفی سرور آتیران + اتصال هوشمند.
 *
 * همهٔ مقادیر با Base64 ذخیره شده‌اند تا در فهرست رشته‌های APK خوانا نباشند و
 * هیچ‌کدام در رابط کاربری نمایش داده نمی‌شوند.
 */
object MaServerProfile {

    // ── مشخصات سرور (مخفی) ─────────────────────────────────────────────────
    private const val H_EXT_B64 = "MzcuMTQzLjE0Ny4xOQ=="        // نشانی اینترنت (بیرون از شبکه)
    private const val H_LOC_B64 = "MTkyLjE2OC4xLjE1MA=="        // نشانی داخلی (وای‌فای اداره)
    private const val DB_B64 = "QXRpcmFuMg=="                   // نام دیتابیس
    private const val USER_B64 = "QWRtaW5Bbg=="                 // کاربر گزارش‌ها
    private const val PASS_B64 = "U3RAUjIwMjIk"                 // رمز کاربر گزارش‌ها

    private const val PORT = 1433

    private fun b64(s: String): String =
        String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)

    /** نشانی اینترنت (برای «سینک از بیرون شبکه»). */
    val hostExternal: String get() = b64(H_EXT_B64)

    /** نشانی داخلی (برای وقتی که به وای‌فای اداره وصل هستیم). */
    val hostLocal: String get() = b64(H_LOC_B64)

    val port: Int get() = PORT
    val database: String get() = b64(DB_B64)
    val username: String get() = b64(USER_B64)
    private val password: String get() = b64(PASS_B64)

    // ── تشخیص هوشمند شبکه ──────────────────────────────────────────────────

    /** نوع شبکهٔ فعلی گوشی (بدون هیچ درخواست مجوز تازه‌ای). */
    fun netKind(ctx: Context): MaNetKind = runCatching {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return MaNetKind.OTHER
        val an = cm.activeNetwork ?: return MaNetKind.NONE
        val caps = cm.getNetworkCapabilities(an) ?: return MaNetKind.NONE
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> MaNetKind.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> MaNetKind.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> MaNetKind.MOBILE
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> MaNetKind.OTHER
            else -> MaNetKind.NONE
        }
    }.getOrElse { MaNetKind.OTHER }

    /**
     * ترتیب هوشمند نشانی‌ها:
     *   • وای‌فای یا کابل  → اول نشانی داخلی (سریع‌تر و بدون اینترنت)، بعد بیرونی
     *   • دادهٔ همراه/سایر → اول نشانی بیرونی، بعد داخلی (شاید روی VPN/شبکهٔ محلی باشیم)
     */
    fun orderedHosts(ctx: Context): List<String> = when (netKind(ctx)) {
        MaNetKind.WIFI, MaNetKind.ETHERNET -> listOf(hostLocal, hostExternal)
        MaNetKind.MOBILE, MaNetKind.OTHER, MaNetKind.NONE -> listOf(hostExternal, hostLocal)
    }

    /** تنظیمات کامل اتصال برای گزارش‌ها (فقط داخل کد استفاده می‌شود). */
    fun settingsFor(host: String): DbSettings = DbSettings(
        host = host,
        port = PORT,
        database = database,
        username = username,
        password = password,
        useEncryption = true,
        trustServerCert = true,
        connectTimeoutSec = 8,
        queryTimeoutSec = 30,
    )

    /** تنظیمات نشانی فعال (بر اساس آخرین اتصال موفق یا نوع شبکه). */
    fun activeSettings(ctx: Context): DbSettings {
        val (_,_, useExternal) = SecureDbStore.loadAddresses()
        val saved = SecureDbStore.load()
        val host = when {
            saved != null && saved.host.isNotBlank() -> saved.host
            useExternal -> hostExternal
            else -> orderedHosts(ctx).first()
        }
        return settingsFor(host).copy(
            // مشخصات مخفی همیشه غالب است (کاربر چیزی وارد نمی‌کند)
            database = database, username = username, password = password, port = PORT,
        )
    }

    /**
     * اتصال هوشمند: اول نشانی مناسب شبکه را با یک کاوش سریع TCP می‌سنجد و بعد
     * اتصال واقعی چهارحالته را برقرار می‌کند؛ اگر جواب نداد خودکار سراغ نشانی
     * دیگر می‌رود. در پایان، تنظیمات موفق را برای اجرای بعدی ذخیره می‌کند.
     */
    suspend fun connectSmart(ctx: Context): MaSmartConnect {
        val kind = netKind(ctx)
        val hosts = orderedHosts(ctx)
        var lastMessage = ""

        // ۱) کاوش سریع — هر نشانی که پورتش باز باشد اول امتحان می‌شود
        val reachable = hosts.filter { h ->
            runCatching { MaSqlEngine.rawTcpProbe(h, PORT, 3000) }.getOrDefault(false)
        }
        val order = (reachable + hosts).distinct()

        // ۲) اتصال واقعی به ترتیب
        order.forEachIndexed { index, host ->
            val cfg = settingsFor(host)
            val r = runCatching { MaSqlEngine.connect(cfg) }.getOrNull()
            if (r != null && r.ok) {
                val internal = host == hostLocal
                // ذخیرهٔ بی‌صدای تنظیمات (رمزنگاری‌شده روی همین گوشی)
                runCatching {
                    SecureDbStore.save(cfg)
                    SecureDbStore.saveAddresses(
                        hostExternal = hostExternal,
                        hostLocal = hostLocal,
                        useExternal = !internal,
                    )
                    MaSectionStore.saveMode(hostLocal, hostExternal, !internal)
                }
                return MaSmartConnect(
                    ok = true,
                    netLabel = kind.label,
                    channelLabel = if (internal) "شبکهٔ داخلی" else "اینترنت",
                    modeLabel = if (index == 0) "خودکار" else "جایگزین",
                    message = "اتصال برقرار شد ✓" +
                        (if (index == 0) "" else " (با مسیر جایگزین)"),
                )
            }
            lastMessage = r?.message ?: lastMessage
        }

        return MaSmartConnect(
            ok = false,
            netLabel = kind.label,
            channelLabel = "—",
            modeLabel = "—",
            message = if (kind == MaNetKind.NONE)
                "گوشی به هیچ شبکه‌ای وصل نیست — اینترنت یا وای‌فای را روشن کنید."
            else
                safe(lastMessage).ifBlank { "اتصال به سرور برقرار نشد — شبکه را بررسی کنید." },
        )
    }

    /** خواندن اطلاعات سرور با نشانی فعال (برای ورود کاربر و همگام‌سازی گزارش‌ها). */
    suspend fun ensureSqlConnection(ctx: Context): Boolean {
        if (SqlConnectionManager.connected()) return true
        val cfg = activeSettings(ctx)
        return runCatching { SqlConnectionManager.connect(cfg) }.getOrDefault(false)
    }

    // ── پوشاندن اطلاعات (برای همهٔ پیام‌های روی صفحه) ──────────────────────

    /** الگوی نشانی IPv4 و نام میزبان‌های مخفی. */
    private val IPV4 = Regex("""\b\d{1,3}(?:\.\d{1,3}){3}\b""")
    private val HOST_PORT = Regex("""\b\d{1,3}(?:\.\d{1,3}){3}\s*:\s*\d{2,5}\b""")

    /**
     * هر متنی که ممکن است نشانی/پورت سرور را لو بدهد، به «سرور آتیران» تبدیل
     * می‌شود. روی همهٔ پیام‌های خطا و گام‌های عیب‌یابی اعمال می‌شود.
     */
    fun safe(raw: String?): String {
        val t = raw.orEmpty()
        if (t.isBlank()) return ""
        var out = t.replace(hostLocal, "سرور آتیران").replace(hostExternal, "سرور آتیران")
        out = out.replace(HOST_PORT, "سرور آتیران")
        out = out.replace(IPV4, "سرور آتیران")
        out = out.replace(database, "دیتابیس آتیران")
        out = out.replace(username, "حساب گزارش‌ها")
        out = out.replace(password, "••••••")
        out = out.replace(Regex("""\b(پورت|port)\s*[:=]?\s*\d{2,5}\b""", RegexOption.IGNORE_CASE), "پورت سرور")
        return out
    }

    /** نسخهٔ پوشاندهٔ گام‌های عیب‌یابی (هیچ نشانی و کاربری دیده نمی‌شود). */
    fun safeSteps(steps: List<MaDiagStep>): List<MaDiagStep> = steps.map {
        it.copy(title = safe(it.title), detail = safe(it.detail), hint = safe(it.hint))
    }
}
