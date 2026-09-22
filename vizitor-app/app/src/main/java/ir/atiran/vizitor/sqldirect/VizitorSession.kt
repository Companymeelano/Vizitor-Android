/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نشست سراسری اتصال مستقیم (اتصال به SQL Server)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرا این فایل لازم است؟
 *    دو نقطهٔ ورود به اتصال داریم — صفحهٔ ابتدایی (انتخاب نقش) و صفحهٔ تنظیمات —
 *    و هر دو باید «یک وضعیت واحد» را ببینند: آیا اتصال تنظیم شده؟ متصل است؟
 *    چه کسی وارد شده؟ آخرین همگام‌سازی چه زمانی و با چه نتیجه‌ای بود؟
 *    این شیء همان منبع واحد وضعیت است (StateFlow) تا هیچ‌جای برنامه دو روایت
 *    متناقض از وضعیت اتصال نشان ندهد.
 *  امنیت: هیچ رمزی در این وضعیت نگه داشته نمی‌شود — فقط نام کاربری/سرور/دیتابیس.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** وضعیت یکپارچهٔ اتصال برنامه به سرور آتیران. */
data class ServerSession(
    /** تنظیمات اتصال ذخیره شده است (سرور/دیتابیس/کاربر دیتابیس). */
    val configured: Boolean = false,
    /** کانکشن SQL باز است. */
    val connected: Boolean = false,
    /** ورود ویزیتور با جدول dbo.sys_users انجام شده است. */
    val loggedIn: Boolean = false,
    /** نام کاربری و رمز ویزیتور ذخیره شده‌اند (ورود سریع در اجرای بعدی). */
    val credentialsSaved: Boolean = false,
    val host: String = "",
    val publicHost: String = "",
    val usePublicHost: Boolean = false,
    val port: Int = 1433,
    val database: String = "",
    val erpUser: String = "",
    val erpName: String = "",
    val erpUserId: Int? = null,
    val companyId: Int? = null,
    val visitorRdf: Int? = null,
    val serverInfo: String = "",
    val busy: Boolean = false,
    val syncing: Boolean = false,
    val lastSyncAt: Long = 0L,
    val lastSyncSummary: String = "",
    /** خلاصهٔ دادهٔ همگام‌شده — برای نمایش «سرویس‌ها فعال شد» در صفحهٔ اول. */
    val productsCount: Int = 0,
    val customersCount: Int = 0,
    val visitorsCount: Int = 0,
    val invoicesCount: Int = 0,
    /** پیام آخرین عملیات (فارسی) و نوع آن: ۰=اطلاع، ۱=موفق، ۲=خطا. */
    val message: String = "",
    val messageKind: Int = 0,
) {
    val activeHost: String get() = if (usePublicHost && publicHost.isNotBlank()) publicHost else host

    /** برچسب کوتاه سرور برای نشان دادن در نوار وضعیت: user@host:port/db */
    val serverLabel: String
        get() = when {
            host.isBlank() && database.isBlank() -> ""
            database.isBlank() -> "$host:$port"
            else -> "$host:$port/$database"
        }

    val readyForQuickEnter: Boolean
        get() = configured && credentialsSaved && !busy
}

/** منبع واحد وضعیت اتصال در سراسر برنامه (تک‌نمونه/Thread-safe). */
object VizitorSession {

    private val _state = MutableStateFlow(ServerSession())
    val state: StateFlow<ServerSession> = _state.asStateFlow()

    val current: ServerSession get() = _state.value

    fun update(block: (ServerSession) -> ServerSession) = _state.update(block)

    fun setStatus(message: String, kind: Int) = _state.update { it.copy(message = message, messageKind = kind) }

    fun beginBusy(message: String = "") = _state.update {
        it.copy(busy = true, message = message.ifBlank { it.message }, messageKind = if (message.isBlank()) it.messageKind else 0)
    }

    fun endBusy() = _state.update { it.copy(busy = false) }

    fun beginSync() = _state.update { it.copy(syncing = true, busy = true) }

    fun endSync(summary: String, kind: Int = 1) = _state.update {
        it.copy(
            syncing = false, busy = false,
            lastSyncAt = System.currentTimeMillis(),
            lastSyncSummary = summary,
            message = summary, messageKind = kind,
        )
    }

    /** پاک‌سازی کامل وضعیت (خروج). */
    fun reset() = _state.update {
        ServerSession(
            configured = it.configured, credentialsSaved = it.credentialsSaved,
            host = it.host, publicHost = it.publicHost, usePublicHost = it.usePublicHost,
            port = it.port, database = it.database,
            message = "از حساب خارج شدید.", messageKind = 0,
        )
    }

    /** بازخوانی وضعیت از محل ذخیره‌سازی امن (هنگام باز شدن صفحه‌ها). */
    fun refreshFromStore() {
        val saved = SecureDbStore.load()
        val (external, local, useExternal) = SecureDbStore.loadAddresses()
        val erp = SecureDbStore.loadErp()
        _state.update {
            it.copy(
                configured = saved != null && saved.host.isNotBlank() && saved.database.isNotBlank(),
                credentialsSaved = erp?.remember == true && erp.username.isNotBlank(),
                host = local.ifBlank { saved?.host.orEmpty() },
                publicHost = external,
                usePublicHost = useExternal && external.isNotBlank(),
                port = saved?.port ?: it.port,
                database = saved?.database.orEmpty(),
                erpUser = erp?.username.orEmpty().ifBlank { it.erpUser },
            )
        }
    }
}
