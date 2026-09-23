// ═══════════════════════════════════════════════════════════════════════════
//  وضعیت برنامه: راه‌اندازی اتصال → انتخاب دیتابیس → ورود ویزیتور → کار با داده
//
//  • هیچ کوئری‌ای اینجا نوشته نمی‌شود؛ همهٔ SQL در لایهٔ تأییدشدهٔ
//    MeelanoDataSource (جدول‌ها و ستون‌های واقعی سرور) است.
//  • همهٔ کارهای سنگین روی Dispatchers.IO و با timeout انجام می‌شود
//    (SqlConnectionManager خودش این کار را می‌کند) — نخ رابط کاربری نمی‌خوابد.
//  • رمز SQL فقط در حافظه و در SecureDbStore رمزنگاری‌شده می‌ماند؛ نه لاگ
//    می‌شود و نه در پیام خطا می‌آید.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.atiran.vizitor.data.local.SecureDbStore
import ir.atiran.vizitor.data.repository.ServerConfig
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.data.sql.DbCustomer
import ir.atiran.vizitor.data.sql.DbLoginRow
import ir.atiran.vizitor.data.sql.DbProduct
import ir.atiran.vizitor.data.sql.DbSettings
import ir.atiran.vizitor.data.sql.DbVisitorIdentity
import ir.atiran.vizitor.data.sql.DirectSql
import ir.atiran.vizitor.data.sql.MeelanoDataSource
import ir.atiran.vizitor.data.sql.SqlConnectionManager
import ir.atiran.vizitor.direct.util.ConnectCard
import ir.atiran.vizitor.direct.util.ConnectCards
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** فرم راه‌اندازی اتصال (همان گام‌های نصب‌کنندهٔ ویندوز). */
data class SetupForm(
    val hostLan: String = "",
    val hostPublic: String = "",
    val usePublic: Boolean = false,
    val port: String = "1433",
    val user: String = "",
    val password: String = "",
    val database: String = "",
) {
    val activeHost: String get() = if (usePublic && hostPublic.isNotBlank()) hostPublic else hostLan
}

/** نتیجهٔ گرفتن فهرست دیتابیس‌ها. */
sealed class DbListState {
    object Idle : DbListState()
    object Loading : DbListState()
    data class Ready(val databases: List<String>) : DbListState()
    data class Failed(val message: String) : DbListState()
}

/** وضعیت ورود کاربر ویزیتور. */
sealed class SessionState {
    object LoggedOut : SessionState()
    object LoggingIn : SessionState()
    data class LoggedIn(val user: DbLoginRow, val identity: DbVisitorIdentity?) : SessionState()
}

/** فهرست‌های خوانده‌شده از سرور (بدون هیچ دادهٔ ساختگی). */
data class DataState(
    val customers: List<DbCustomer> = emptyList(),
    val products: List<DbProduct> = emptyList(),
    val loading: Boolean = false,
    val message: String = "",
    val error: String = "",
)

class DirectViewModel(app: Application) : AndroidViewModel(app) {

    private val sql = MeelanoDataSource(SqlConnectionManager)

    private val _form = MutableStateFlow(SetupForm())
    val form: StateFlow<SetupForm> = _form.asStateFlow()

    private val _dbList = MutableStateFlow<DbListState>(DbListState.Idle)
    val dbList: StateFlow<DbListState> = _dbList.asStateFlow()

    private val _session = MutableStateFlow<SessionState>(SessionState.LoggedOut)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val _data = MutableStateFlow(DataState())
    val data: StateFlow<DataState> = _data.asStateFlow()

    private val _health = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val health: StateFlow<Map<String, Boolean>> = _health.asStateFlow()

    private val _dbInfo = MutableStateFlow<Triple<String, String, String>?>(null)
    val dbInfo: StateFlow<Triple<String, String, String>?> = _dbInfo.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error.asStateFlow()

    val connection: StateFlow<ConnectionState> = SqlConnectionManager.state

    /** نام درایور فعال روی این دستگاه (نمایش در کارت وضعیت). */
    val driverName: String get() = DirectSql.displayName()

    private var currentConfig: ServerConfig? = null

    init {
        SecureDbStore.init(app)
        restore()
    }

    // ── بازیابی تنظیمات ذخیره‌شده ─────────────────────────────────────────────
    private fun restore() {
        val saved = SecureDbStore.load() ?: return
        val addresses = SecureDbStore.loadAddresses()
        currentConfig = ServerConfig.from(saved, addresses)
        _form.value = SetupForm(
            hostLan = addresses.first.ifBlank { saved.host },
            hostPublic = addresses.second,
            usePublic = addresses.third,
            port = saved.port.toString(),
            user = saved.username,
            password = saved.password,
            database = saved.database,
        )
        if (saved.host.isNotBlank() && saved.database.isNotBlank() && saved.username.isNotBlank()) {
            viewModelScope.launch { silentReconnect(saved) }
        }
    }

    private suspend fun silentReconnect(saved: DbSettings) {
        val ok = SqlConnectionManager.connect(saved)
        if (ok) {
            readServerInfo()
        } else {
            _error.value = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                ?: "اتصال به دیتابیس برقرار نشد."
        }
    }

    // ── ویرایش فرم ───────────────────────────────────────────────────────────
    fun updateForm(block: (SetupForm) -> SetupForm) {
        _form.value = block(_form.value)
        _error.value = ""
        _message.value = ""
    }

    /** پر کردن فرم از کارت اتصال نصب‌کننده. */
    fun applyCard(card: ConnectCard) {
        _form.value = _form.value.copy(
            hostLan = card.hostLan.ifBlank { _form.value.hostLan },
            hostPublic = card.hostPublic.ifBlank { _form.value.hostPublic },
            port = card.port.toString(),
            user = card.login.ifBlank { _form.value.user },
            database = card.database.ifBlank { _form.value.database },
            usePublic = card.hostLan.isBlank() && card.hostPublic.isNotBlank(),
        )
        _message.value = "کارت اتصال خوانده شد (منبع: ${card.source}). حالا رمز کاربر دیتابیس را وارد کنید."
    }

    /** خواندن هر متن کارتی (URI کد QR، JSON، یا متن کارت) و پر کردن فرم. */
    fun applyCardText(text: String) {
        val card = ConnectCards.parse(text)
        if (card == null) {
            _error.value = "متن کارت اتصال خوانده نشد. باید با vizitor:// شروع شود یا فایل android-connect.json باشد."
            return
        }
        applyCard(card)
    }

    /** نمایش پیام خطای آماده (مثلاً خطای خواندن فایل). */
    fun setError(message: String) {
        _error.value = message
    }

    // ── گام ۲ نصب‌کننده: تست اتصال و گرفتن فهرست دیتابیس‌ها ─────────────────
    fun fetchDatabases() {
        val f = _form.value
        if (f.activeHost.isBlank() || f.user.isBlank() || f.password.isBlank()) {
            _error.value = "آدرس سرور، نام کاربری و رمز لازم است."
            return
        }
        _dbList.value = DbListState.Loading
        _error.value = ""
        viewModelScope.launch {
            val probe = DbSettings(
                host = f.activeHost.trim(),
                port = f.port.toIntOrNull() ?: 1433,
                database = "master",
                username = f.user.trim(),
                password = f.password,
                useEncryption = false,
                trustServerCert = true,
            )
            val result = SqlConnectionManager.listDatabases(probe)
            _dbList.value = result.fold(
                onSuccess = { names -> DbListState.Ready(names) },
                onFailure = { e -> DbListState.Failed(e.message ?: "گرفتن فهرست دیتابیس‌ها ناموفق بود.") },
            )
        }
    }

    // ── گام ۳ و ۴ نصب‌کننده: انتخاب دیتابیس + اعمال تنظیمات و تست سلامت ────
    fun connectToDatabase(database: String = _form.value.database) {
        val f = _form.value
        val db = database.trim()
        if (db.isEmpty()) {
            _error.value = "نام دیتابیس را انتخاب یا تایپ کنید."
            return
        }
        if (f.activeHost.isBlank() || f.user.isBlank()) {
            _error.value = "آدرس سرور و نام کاربری لازم است."
            return
        }
        _error.value = ""
        viewModelScope.launch {
            val settings = DbSettings(
                host = f.activeHost.trim(),
                port = f.port.toIntOrNull() ?: 1433,
                database = db,
                username = f.user.trim(),
                password = f.password,
                useEncryption = false,
                trustServerCert = true,
            )
            val ok = SqlConnectionManager.connect(settings)
            if (!ok) {
                _error.value = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                    ?: "اتصال برقرار نشد."
                return@launch
            }
            // ذخیرهٔ رمزنگاری‌شده + نشانی‌ها (برای اتصال دفعهٔ بعد)
            SecureDbStore.saveAddresses(f.hostPublic.trim(), f.hostLan.trim(), f.usePublic)
            SecureDbStore.save(settings)
            currentConfig = ServerConfig(
                serverIp = settings.host, dbPort = settings.port, dbName = db,
                dbUser = settings.username, dbPassword = settings.password,
                useEncryption = false, trustServerCert = true,
                hostLan = f.hostLan.trim(), hostPublic = f.hostPublic.trim(), usePublic = f.usePublic,
            )
            _form.value = f.copy(database = db)
            readServerInfo()
            _message.value = "اتصال برقرار شد و تنظیمات ذخیره شد."
        }
    }

    /** خواندن مشخصات سرور/دیتابیس + آمادگی مسیر نوشتن (تنها خواندن). */
    private suspend fun readServerInfo() {
        runCatching { _dbInfo.value = sql.databaseInfo() }
        runCatching { _health.value = sql.preInvoiceHealth() }
    }

    fun refreshHealth() {
        viewModelScope.launch {
            runCatching { SqlConnectionManager.refresh() }
            readServerInfo()
        }
    }

    // ── گام ۷ نصب‌کننده: ورود ویزیتور با جدول واقعی dbo.sys_users ──────────
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _error.value = "نام کاربری و کلمهٔ عبور خود را وارد کنید."
            return
        }
        _session.value = SessionState.LoggingIn
        _error.value = ""
        viewModelScope.launch {
            try {
                val row = sql.login(username.trim(), password)
                when {
                    row == null -> {
                        _session.value = SessionState.LoggedOut
                        _error.value = "نام کاربری یا کلمهٔ عبور درست نیست."
                    }
                    !row.active -> {
                        _session.value = SessionState.LoggedOut
                        _error.value = "این حساب در سامانه غیرفعال است (sys_users.active = 0)."
                    }
                    row.locked -> {
                        _session.value = SessionState.LoggedOut
                        _error.value = "این حساب قفل است؛ با مدیر سامانه تماس بگیرید."
                    }
                    else -> {
                        val identity = runCatching { sql.visitorIdentity(row.userId, row.companyId) }.getOrNull()
                        _session.value = SessionState.LoggedIn(row, identity)
                        _message.value = "خوش آمدید ${identity?.displayName ?: row.fullName}"
                    }
                }
            } catch (e: Exception) {
                _session.value = SessionState.LoggedOut
                _error.value = e.message ?: "ورود ناموفق بود."
            }
        }
    }

    fun logout() {
        SqlConnectionManager.disconnect()
        _session.value = SessionState.LoggedOut
        _data.value = DataState()
        _message.value = "از حساب خارج شدید."
    }

    /** قطع کامل + پاک کردن تنظیمات ذخیره‌شده (برای تحویل گوشی به فرد دیگر). */
    fun forgetEverything() {
        SqlConnectionManager.disconnect()
        SecureDbStore.clear()
        _session.value = SessionState.LoggedOut
        _data.value = DataState()
        _dbList.value = DbListState.Idle
        _dbInfo.value = null
        _health.value = emptyMap()
        _form.value = SetupForm()
        currentConfig = null
        _message.value = "تنظیمات اتصال از این گوشی پاک شد."
    }

    // ── داده‌های ویزیتور (فقط خواندن از جدول‌های واقعی) ─────────────────────
    /**
     * مشتریان مجاز همین ویزیتور (جدول‌های واقعی sys_cus + CUSTOMERS).
     * جست‌وجو در همین فهرستِ خوانده‌شده، سمت برنامه انجام می‌شود (بدون کوئری تازه).
     */
    fun loadCustomers() {
        val session = _session.value as? SessionState.LoggedIn ?: return
        viewModelScope.launch {
            _data.value = _data.value.copy(loading = true, error = "", message = "")
            try {
                val list = sql.customersFor(session.user.userId, session.user.companyId, limit = 300)
                _data.value = _data.value.copy(
                    customers = list,
                    loading = false,
                    message = "مشتریان مجاز شما: ${list.size}",
                )
            } catch (e: Exception) {
                _data.value = _data.value.copy(loading = false, error = e.message ?: "خواندن مشتریان ناموفق بود.")
            }
        }
    }

    fun loadProducts(search: String? = null) {
        val session = _session.value as? SessionState.LoggedIn ?: return
        viewModelScope.launch {
            _data.value = _data.value.copy(loading = true, error = "", message = "")
            try {
                val list = sql.products(limit = 300, search = search?.takeIf { it.isNotBlank() })
                _data.value = _data.value.copy(
                    products = list,
                    loading = false,
                    message = "کالاها: ${list.size}",
                )
            } catch (e: Exception) {
                _data.value = _data.value.copy(loading = false, error = e.message ?: "خواندن کالاها ناموفق بود.")
            }
        }
    }

    fun clearMessage() {
        _message.value = ""
    }

    fun clearError() {
        _error.value = ""
    }
}
