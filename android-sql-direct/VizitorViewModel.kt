/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ویومدیل مرکزی اپلیکیشن
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.atiran.vizitor.ai.GeminiAssistant
import ir.atiran.vizitor.data.local.CartItemEntity
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.ChatMessageType
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.data.local.ChatPrefs
import ir.atiran.vizitor.data.local.AuthStore
import ir.atiran.vizitor.data.local.SeedData
import ir.atiran.vizitor.data.local.TopProduct
import ir.atiran.vizitor.data.local.InvoiceItemEntity
import ir.atiran.vizitor.data.repository.HealthReport
import ir.atiran.vizitor.data.local.SecureDbStore
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.data.sql.DbSettings
import ir.atiran.vizitor.data.sql.SqlConnectionManager
import ir.atiran.vizitor.data.repository.ServerConfig
import ir.atiran.vizitor.data.repository.SyncReport
import ir.atiran.vizitor.data.repository.VizitorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VizitorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VizitorRepository(app)

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()
    val loggedIn: StateFlow<Boolean> = AuthStore.loggedIn

    fun hasSession(): Boolean = AuthStore.isSessionValid()

    fun login(username: String, password: String, onSuccess: () -> Unit) = viewModelScope.launch {
        if (_loginLoading.value) return@launch
        _loginLoading.value = true
        _loginError.value = null
        repo.login(username, password).fold(
            onSuccess = { onSuccess() },
            onFailure = { _loginError.value = it.message ?: "ورود ناموفق بود" }
        )
        _loginLoading.value = false
    }

    fun logout() {
        repo.logout()
        _loginError.value = null
    }

    // ── تنظیمات ویزیتور (سطح قیمت پیش‌فرض) — ذخیره ماندگار ───────────────
    private val prefs = app.getSharedPreferences("vizitor_prefs", android.content.Context.MODE_PRIVATE)

    private val _priceLevel = MutableStateFlow(prefs.getInt("default_price_level", 1))
    /** سطح قیمت پیش‌فرض ویزیتور: ۱ = فروش ۱ | ۲ = فروش ۲ */
    val defaultPriceLevel: StateFlow<Int> = _priceLevel.asStateFlow()

    fun setDefaultPriceLevel(level: Int) {
        prefs.edit().putInt("default_price_level", level).apply()
        _priceLevel.value = level
    }

    /** سطح قیمت پیش‌فرض برای یک مشتری (بر اساس گروه؛ در نبود مشتری، تنظیم ویزیتور). */
    fun priceLevelFor(customer: CustomerEntity?): Int =
        customer?.let { repo.defaultPriceLevel(it.groupName) } ?: _priceLevel.value

    // ── فلوهای عمومی ─────────────────────────────────────────────────────────
    val products = repo.products.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val customers = repo.customers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val followUpCustomers = repo.followUpCustomers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val cartItems = repo.cartItems.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val invoices = repo.invoices.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val pendingCount = repo.pendingCount.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val cartTotal = repo.cartTotal.stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    val todaySales = repo.todaySales.stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    val commission = repo.todayCommission.stateIn(viewModelScope, SharingStarted.Lazily, 0L)
    val config = repo.config.stateIn(viewModelScope, SharingStarted.Lazily, ServerConfig())

    val dailyTarget: Long get() = repo.dailyTarget

    // ── پرفروش‌ترین‌ها (ترکیب فاکتورهای محلی + سال مالی) ─────────────────────
    private val _topProducts = MutableStateFlow<List<TopProduct>>(emptyList())
    val topProducts: StateFlow<List<TopProduct>> = _topProducts.asStateFlow()

    init {
        viewModelScope.launch {
            invoices.collect {
                _topProducts.value = repo.topProducts().ifEmpty { seedTopProducts() }
            }
        }
    }

    private fun seedTopProducts(): List<TopProduct> =
        SeedData.salMali
            .groupBy { it.productName }
            .map { (name, rows) -> TopProduct(name, rows.sumOf { it.totalQty }) }
            .sortedByDescending { it.total }
            .take(3)

    /** اقلام یک فاکتور (برای اشتراک‌گذاری PDF/Word/تصویر). */
    fun invoiceItems(invoiceId: Long, onItems: (List<InvoiceItemEntity>) -> Unit) =
        viewModelScope.launch { onItems(repo.itemsFor(invoiceId)) }

    // ── وضعیت صفحه ──────────────────────────────────────────────────────────
    private val _selectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val selectedCustomer: StateFlow<CustomerEntity?> = _selectedCustomer.asStateFlow()

    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion: StateFlow<String?> = _aiSuggestion.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** آخرین گزارش همگام‌سازی — برای نمایش ماندگار در کارت مدیریت سینک. */
    private val _syncReport = MutableStateFlow<SyncReport?>(null)
    val syncReport: StateFlow<SyncReport?> = _syncReport.asStateFlow()

    /** وضعیت در حال اجرای تست سلامت سرور. */
    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    /** آخرین نتیجهٔ تست سلامت سرور (null تا اولین اجرا). */
    private val _health = MutableStateFlow<HealthUiState?>(null)
    val health: StateFlow<HealthUiState?> = _health.asStateFlow()

    fun consumeToast() { _toast.value = null }
    fun showToast(msg: String) { _toast.value = msg }

    // ── اکشن‌های سبد خرید ───────────────────────────────────────────────────
    fun addToCart(product: ProductEntity, qty: Double = 1.0, unitPrice: Long = product.price) =
        viewModelScope.launch {
            repo.addToCart(product, qty, unitPrice)
            _toast.value = "«${product.name}» به سبد سفارش اضافه شد ✅"
        }

    /** ورود دستی تعداد در سبد. */
    fun setCartQty(productId: Int, qty: Double) = viewModelScope.launch {
        repo.setCartQty(productId, qty)
    }

    /** آخرین قیمت فروش کالا به مشتری انتخاب‌شده. */
    fun lastSalePrice(customerId: Int, productId: Int, onResult: (Long?) -> Unit) =
        viewModelScope.launch { onResult(repo.lastSalePrice(customerId, productId)) }

    fun decrement(productId: Int) = viewModelScope.launch { repo.decrementCart(productId) }
    fun removeFromCart(productId: Int) = viewModelScope.launch { repo.removeFromCart(productId) }

    fun selectCustomer(customer: CustomerEntity?) { _selectedCustomer.value = customer }

    // ── اتاق گفتگوی ویزیتورها ────────────────────────────────────────────────
    val chatMessages = repo.chatMessages.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch { repo.seedChatIfEmpty() }
    }

    fun sendChatMessage(text: String, type: ChatMessageType) = viewModelScope.launch {
        if (!ChatPrefs.isRegistered) return@launch
        repo.sendChatMessage(
            senderName = ChatPrefs.fullName.value,
            senderUsername = ChatPrefs.username.value,
            senderPhone = ChatPrefs.phone.value,
            text = text,
            type = type
        )
    }

    fun pinChatMessage(id: Long, pinned: Boolean) = viewModelScope.launch {
        repo.pinChatMessage(id, pinned)
        _toast.value = if (pinned) "پیام در بالای گفتگو سنجاق شد 📌" else "پیام از سنجاق برداشته شد"
    }

    fun deleteChatMessage(id: Long) = viewModelScope.launch {
        repo.deleteChatMessage(id)
        _toast.value = "پیام حذف شد 🗑️"
    }

    /** ثبت مشتری جدید — محلی (در انتظار تأیید حسابداری) + تلاش ارسال به آتیران. */
    fun addPendingCustomer(
        name: String, group: String, city: String, address: String, phone: String
    ) = viewModelScope.launch {
        val (customer, pushed) = repo.addPendingCustomer(name, group, city, address, phone)
        _toast.value = if (pushed)
            "مشتری «${customer.name}» ثبت و برای تأیید به حسابداری آتیران ارسال شد 📨"
        else
            "مشتری «${customer.name}» محلی ثبت شد و در اولین سینک به آتیران ارسال می‌شود ⏳"
    }

    fun findProductByBarcode(code: String, onResult: (ProductEntity?) -> Unit) =
        viewModelScope.launch {
            val p = repo.findByBarcode(code)
            if (p != null) repo.addToCart(p, 1.0, p.price)
            onResult(p)
        }

    /** صدور فاکتور: ذخیره محلی + تلاش ارسال فوری به سرور آتیران. */
    fun issueInvoice(
        signaturePng: ByteArray?,
        cashSettlement: Boolean,
        note: String = "",
        onDone: (InvoiceEntity) -> Unit
    ) = viewModelScope.launch {
        try {
            val invoice = repo.issueInvoice(_selectedCustomer.value, signaturePng, cashSettlement, note)
            _selectedCustomer.value = null
            onDone(invoice)
            // تلاش ارسال آنی؛ در صورت شکست، فاکتور در صف سینک می‌ماند
            launch {
                val report = repo.runCatching { syncAll() }.getOrNull()
                if (report != null && report.pushedInvoices > 0) {
                    _toast.value = "فاکتور به سرور آتیران ارسال شد 🚀"
                }
            }
        } catch (e: Exception) {
            _toast.value = e.message ?: "خطا در صدور فاکتور"
        }
    }

    // ── دستیار هوش مصنوعی ──────────────────────────────────────────────────
    fun askAiAssistant() = viewModelScope.launch {
        _aiLoading.value = true
        _aiSuggestion.value = null
        try {
            val cfg = config.value
            val customer = _selectedCustomer.value ?: customers.value.firstOrNull()
            val salMali = customer?.let { repo.salMaliFor(it.id) } ?: emptyList()
            val assistant = GeminiAssistant(cfg)
            val result = assistant.suggestComplementary(
                customerName = customer?.name ?: "مشتری عمومی",
                salMali = salMali,
                cart = cartItems.value,
                catalog = products.value
            )
            _aiSuggestion.value = result.getOrElse {
                "دستیار هوشمند در دسترس نیست (آفلاین یا پراکسی پیکربندی نشده). " +
                        "پیشنهاد کلاسیک: فیلتر روغن و فیلتر هوا مکمل هر خرید روغن موتور هستند. 🧰"
            }
        } finally {
            _aiLoading.value = false
        }
    }

    // ── اتصال مستقیم به SQL Server (معماری جدیدِ لایهٔ داده) ──────────────────
    //  رمز عبور فقط داخل SecureDbStore (AES-GCM + Android Keystore) می‌ماند و
    //  هرگز در Log/crash/خروجی چاپ نمی‌شود؛ در حافظه هم فقط تا زمان اتصال است.
    private val dbManager = SqlConnectionManager()

    /** وضعیت زندهٔ اتصال SQL — در صفحهٔ تنظیمات نمایش داده می‌شود. */
    val dbState: StateFlow<ConnectionState> = dbManager.state

    private val _dbTesting = MutableStateFlow(false)
    val dbTesting: StateFlow<Boolean> = _dbTesting.asStateFlow()

    private val _dbConfig = MutableStateFlow<DbSettings?>(SecureDbStore.load())
    /** آخرین پیکربندی SQL (رمز درون آن است ولی هیچ‌جا نمایش داده نمی‌شود). */
    val dbConfig: StateFlow<DbSettings?> = _dbConfig.asStateFlow()

    init {
        SecureDbStore.init(app)
        _dbConfig.value = SecureDbStore.load()
    }

    /**
     * تست و در صورت موفقیت ذخیرهٔ پیکربندی SQL.
     * فقط پیکربندی‌ای که واقعاً وصل شده روی دستگاه ذخیره می‌شود.
     */
    fun testDbConnection(settings: DbSettings) = viewModelScope.launch {
        if (_dbTesting.value) return@launch
        _dbTesting.value = true
        try {
            val ok = dbManager.connect(settings)
            if (ok) {
                SecureDbStore.save(settings)      // فقط پیکربندی‌ای که واقعاً وصل شد
                _dbConfig.value = settings
                dbManager.refresh()               // اندازه‌گیری تأخیر واقعی
                val ms = (dbManager.state.value as? ConnectionState.Ready)?.latencyMs
                _toast.value = if (ms != null) "اتصال به SQL برقرار شد ✅ (${ms}ms)" else "اتصال به SQL برقرار شد ✅"
            } else {
                _toast.value = (dbManager.state.value as? ConnectionState.Error)?.message
                    ?: "اتصال به SQL برقرار نشد"
            }
        } finally {
            _dbTesting.value = false
        }
    }

    // ── دو نشانی سرور: آی‌پی اختصاصی (بیرون شبکه) و آی‌پی داخلی شبکه ──────────
    private val savedAddresses = SecureDbStore.loadAddresses()

    private val _dbHostExternal = MutableStateFlow(savedAddresses.first)
    val dbHostExternal: StateFlow<String> = _dbHostExternal.asStateFlow()

    private val _dbHostLocal = MutableStateFlow(savedAddresses.second)
    val dbHostLocal: StateFlow<String> = _dbHostLocal.asStateFlow()

    /** true = اتصال از بیرون شبکه با آی‌پی اختصاصی، false = آی‌پی داخلی شبکه. */
    private val _dbUseExternal = MutableStateFlow(savedAddresses.third)
    val dbUseExternal: StateFlow<Boolean> = _dbUseExternal.asStateFlow()

    /** نشانی فعال بر اساس انتخاب کاربر (پیش‌فرض: بیرون شبکه اگر آی‌پی اختصاصی پر باشد). */
    fun activeDbHost(): String {
        val ext = _dbHostExternal.value.trim()
        val loc = _dbHostLocal.value.trim()
        return if (_dbUseExternal.value) ext.ifBlank { loc } else loc.ifBlank { ext }
    }

    fun setDbAddresses(hostExternal: String, hostLocal: String, useExternal: Boolean) {
        _dbHostExternal.value = hostExternal.trim()
        _dbHostLocal.value = hostLocal.trim()
        _dbUseExternal.value = useExternal
        SecureDbStore.saveAddresses(hostExternal, hostLocal, useExternal)
    }

    fun setDbUseExternal(useExternal: Boolean) {
        _dbUseExternal.value = useExternal
        SecureDbStore.saveAddresses(_dbHostExternal.value, _dbHostLocal.value, useExternal)
    }

    // ── لیست دیتابیس‌های سرور (مرحلهٔ انتخاب دیتابیس حسابداری) ───────────────
    private val _dbOptions = MutableStateFlow<List<String>>(emptyList())
    val dbOptions: StateFlow<List<String>> = _dbOptions.asStateFlow()

    private val _dbListLoading = MutableStateFlow(false)
    val dbListLoading: StateFlow<Boolean> = _dbListLoading.asStateFlow()

    /** لیست دیتابیس‌ها را با همان کاربر/رمزِ واردشده می‌گیرد (فقط خواندن). */
    fun loadDatabases(settings: DbSettings) = viewModelScope.launch {
        // لیست با همان کاربر/رمزِ واردشده گرفته می‌شود؛ رمز جایی log نمی‌شود.
        if (_dbListLoading.value) return@launch
        _dbListLoading.value = true
        try {
            val res = dbManager.listDatabases(settings)
            res.onSuccess { list ->
                _dbOptions.value = list
                _toast.value = if (list.isEmpty()) "دیتابیس کاربری روی این سرور پیدا نشد"
                else "لیست دیتابیس‌ها گرفته شد (${list.size} مورد) — دیتابیس حسابداری را انتخاب کنید"
            }.onFailure { e ->
                _dbOptions.value = emptyList()
                _toast.value = e.message ?: "گرفتن لیست دیتابیس‌ها ناموفق بود"
            }
        } finally {
            _dbListLoading.value = false
        }
    }

    /** خواندن دوبارهٔ وضعیت اتصال (برای دکمهٔ «بررسی مجدد»). */
    fun refreshDbState() = viewModelScope.launch { dbManager.refresh() }

    fun disconnectDb() {
        dbManager.disconnect()
        _toast.value = "اتصال SQL قطع شد"
    }

    fun clearDbConfig() {
        dbManager.disconnect()
        SecureDbStore.clear()
        _dbConfig.value = null
        _toast.value = "پیکربندی SQL از این دستگاه پاک شد"
    }

    // ── تنظیمات و همگام‌سازی ────────────────────────────────────────────────
    fun saveConfig(newConfig: ServerConfig) = viewModelScope.launch {
        repo.saveConfig(newConfig)
        _toast.value = "پیکربندی سرور ذخیره شد ✅"
    }

    /** تست سلامت کامل سرور — نتیجه در کارت وضعیت نمایش داده می‌شود. */
    fun testConnection() = viewModelScope.launch {
        if (_testing.value) return@launch
        _testing.value = true
        try {
            repo.testConnection().fold(
                onSuccess = {
                    _health.value = HealthUiState(ok = true, report = it, error = null)
                    _toast.value = "اتصال سالم است ✅ (تأخیر: ${it.latencyMs}ms)"
                },
                onFailure = {
                    _health.value = HealthUiState(
                        ok = false, report = null,
                        error = it.message ?: "خطای نامشخص شبکه"
                    )
                    _toast.value = "تست سلامت ناموفق ❌"
                }
            )
        } finally {
            _testing.value = false
        }
    }

    fun syncNow() = viewModelScope.launch {
        if (_syncing.value) return@launch
        _syncing.value = true
        try {
            val report: SyncReport = repo.syncAll()
            _syncReport.value = report
            _toast.value = report.summary
        } catch (e: Exception) {
            val failed = SyncReport(0, 0, listOf(e.message ?: "خطای شبکه"))
            _syncReport.value = failed
            _toast.value = "همگام‌سازی ناموفق: ${e.message}"
        } finally {
            _syncing.value = false
        }
    }

    fun computeDiscount(gross: Long, cash: Boolean): Long = repo.computeDiscount(gross, cash)
}

/** وضعیت نمایشی تست سلامت سرور در صفحه گزارشات. */
data class HealthUiState(
    val ok: Boolean,
    val report: HealthReport?,
    val error: String?
)
