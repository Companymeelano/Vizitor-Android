/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ویومدیل مرکزی اپلیکیشن
 *  Developed by Milano Technical Team, Milad Yaghoobi
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
import ir.atiran.vizitor.data.local.SeedData
import ir.atiran.vizitor.data.local.TopProduct
import ir.atiran.vizitor.data.local.InvoiceItemEntity
import ir.atiran.vizitor.data.repository.HealthReport
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

    /** فاکتورهای واقعی سامانه که با اتصال مستقیم از SQL Server خوانده شده‌اند. */
    val serverInvoices = repo.serverInvoices.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
