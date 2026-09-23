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
import ir.atiran.vizitor.sqldirect.SqldirectSync
import ir.atiran.vizitor.sqldirect.VisitRow
import ir.atiran.vizitor.sqldirect.VizitorGateway
import ir.atiran.vizitor.sqldirect.VizitorSession
import ir.atiran.vizitor.sqldirect.SqlConnectionManager
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

    // ── همگام‌سازی دستی مشتریان (دکمهٔ «همگام‌سازی مشتریان» در تب مشتری) ────
    private val _customerSync = MutableStateFlow(CustomerSyncState())
    val customerSync: StateFlow<CustomerSyncState> = _customerSync.asStateFlow()

    /**
     * خواندن تازهٔ فهرست مشتریان از سرور و نوشتن در پایگاه‌دادهٔ محلی.
     * مسیرها: مجوز کاربر (sys_cus) ← مشتریان خودِ ویزیتور (CUSTOMERS.vis_rdf) ←
     * همهٔ مشتریان فعال. نتیجه با تعداد/مسیر/علت خطا به کاربر گفته می‌شود.
     */
    fun syncCustomersNow() {
        if (_customerSync.value.busy) return
        viewModelScope.launch {
            val session = ir.atiran.vizitor.sqldirect.VizitorSession.current
            val dbUser = session.erpUserId
            if (dbUser == null) {
                _customerSync.value = CustomerSyncState(
                    busy = false, message = "اول وارد سامانه شوید، بعد فهرست مشتریان را بگیرید.", ok = false
                )
                showToast("برای همگام‌سازی مشتریان باید وارد سامانه شده باشید ⚠️")
                return@launch
            }
            _customerSync.value = CustomerSyncState(busy = true, message = "در حال گرفتن فهرست مشتریان از سرور…")
            // v2.18.0: تضمین اتصال از دروازهٔ واحد — با تنظیمات ذخیره‌شدهٔ گوشی
            if (!ir.atiran.vizitor.sqldirect.VizitorGateway.ensureConnection()) {
                val msg = ir.atiran.vizitor.sqldirect.VizitorGateway.errorMessage()
                _customerSync.value = CustomerSyncState(busy = false, message = "اتصال به سرور برقرار نشد: $msg", ok = false)
                showToast("اتصال به سرور برقرار نشد ❌")
                return@launch
            }
            val r = runCatching {
                ir.atiran.vizitor.sqldirect.CustomerSync.fetch(
                    userId = dbUser,
                    companyId = session.companyId,
                    visitorRdf = session.visitorRdf,
                    limit = 3000,
                )
            }.getOrElse { e ->
                ir.atiran.vizitor.sqldirect.CustomerSync.Result(
                    emptyList(), "none", emptyList(), e.message ?: e.javaClass.simpleName
                )
            }
            if (r.customers.isNotEmpty()) {
                repo.replaceCustomers(r.customers)
            }
            val msg = when {
                r.customers.isNotEmpty() -> "✅ ${r.customers.size} مشتری همگام شد — مسیر: ${r.sourceLabel}"
                r.error != null -> "❌ مشتریان خوانده نشد: ${r.error}"
                else -> "مشتری‌ای پیدا نشد. " + r.notes.joinToString(" • ")
            }
            _customerSync.value = CustomerSyncState(
                busy = false, message = msg, ok = r.customers.isNotEmpty(), count = r.customers.size
            )
            showToast(if (r.customers.isNotEmpty()) "فهرست مشتریان به‌روز شد ✅ (${r.customers.size} مشتری)" else "مشتری‌ای برای این کاربر پیدا نشد ⚠️")
        }
    }

    /** وضعیت همگام‌سازی مشتریان برای نمایش در تب مشتری. */
    data class CustomerSyncState(
        val busy: Boolean = false,
        val message: String = "",
        val ok: Boolean = true,
        val count: Int = 0,
    )

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
        val (customer, _) = repo.addPendingCustomer(name, group, city, address, phone)
        // v2.18.0 — شفاف و بدون ادعای نادرست: مسیر نوشتن مشتری در ERP (dbo.new_cust)
        // در این نسخه فعال نیست (نیازمند تأیید ستون‌ها روی سرور است)، پس فقط
        // «فهرست مشتریان در انتظار» در گوشی ثبت می‌شود و به حسابداری گزارش می‌شود.
        _toast.value = "مشتری «${customer.name}» در فهرست «در انتظار تأیید حسابداری» ثبت شد ✅ " +
            "— ارسال خودکار به آتیران در این نسخه انجام نمی‌شود"
    }

    fun findProductByBarcode(code: String, onResult: (ProductEntity?) -> Unit) =
        viewModelScope.launch {
            val p = repo.findByBarcode(code)
            if (p != null) repo.addToCart(p, 1.0, p.price)
            onResult(p)
        }

    /**
     * صدور فاکتور (v2.18.0):
     *   ۱) ثبت محلی در گوشی (آفلاین‌اول — مثل قبل)
     *   ۲) فراخوانی **مسیر واقعی پیش‌فاکتور ERP** از دروازهٔ واحد:
     *      • اگر «ثبت واقعی» روشن و پیش‌فاکتور مرجع در ERP موجود باشد → سند در آتیران ثبت می‌شود
     *      • وگرنه «پیش‌نمایش سند» ساخته می‌شود و هیچ چیز در سرور نوشته نمی‌شود
     *      (قبلاً این مرحله به API قدیمی می‌رفت و نتیجه‌ای نداشت)
     */
    fun issueInvoice(
        signaturePng: ByteArray?,
        cashSettlement: Boolean,
        note: String = "",
        onDone: (InvoiceEntity) -> Unit
    ) = viewModelScope.launch {
        try {
            val customer = _selectedCustomer.value
            val invoice = repo.issueInvoice(customer, signaturePng, cashSettlement, note)
            _selectedCustomer.value = null
            onDone(invoice)

            val live = _liveInvoiceWrite.value
            val outcome = runCatching {
                VizitorGateway.submitCart(
                    db = db(),
                    customer = customer,
                    userName = VizitorSession.current.erpUser,
                    note = note,
                    live = live,
                )
            }.getOrNull()
            _invoiceOutcome.value = outcome
            _toast.value = when {
                outcome == null -> "فاکتور در گوشی ثبت شد ✅ — ارتباط با سرور برقرار نشد ⏳"
                !outcome.live -> "فاکتور در گوشی ثبت شد ✅ • " + outcome.message
                outcome.ok -> outcome.message
                else -> outcome.message
            }
        } catch (e: Exception) {
            _toast.value = e.message ?: "خطا در صدور فاکتور"
        }
    }

    /** نتیجهٔ آخرین فراخوانی سند (پیش‌نمایش/ثبت واقعی) — برای نمایش در صفحهٔ فعالیت‌ها. */
    private val _invoiceOutcome = MutableStateFlow<VizitorGateway.WriteOutcome?>(null)
    val invoiceOutcome: StateFlow<VizitorGateway.WriteOutcome?> = _invoiceOutcome.asStateFlow()

    fun clearInvoiceOutcome() { _invoiceOutcome.value = null }

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

    /**
     * تست سلامت سرور — v2.18.0: از **مسیر مستقیم SQL** انجام می‌شود (نه API میانی).
     * خروجی همان کارت وضعیت صفحهٔ تنظیمات است.
     */
    fun testConnection() = viewModelScope.launch {
        if (_testing.value) return@launch
        _testing.value = true
        try {
            val probe = VizitorGateway.probe()
            val session = VizitorSession.current
            if (probe.ok) {
                _health.value = HealthUiState(
                    ok = true,
                    report = HealthReport(
                        db = probe.database.ifBlank { session.database },
                        dbHost = "${session.activeHost}:${session.port}",
                        dbVersion = probe.serverVersion.ifBlank { null },
                        php = null,
                        apiVersion = null,
                        serverTime = System.currentTimeMillis(),
                        tables = null,
                        latencyMs = probe.latencyMs,
                        baseUrl = "direct-sql://${session.activeHost}:${session.port}/${probe.database}",
                    ),
                    error = null,
                )
                _toast.value = "اتصال سالم است ✅ (درایور ${probe.driver} • تأخیر ${probe.latencyMs} م.ث)"
            } else {
                _health.value = HealthUiState(ok = false, report = null, error = probe.message)
                _toast.value = "تست سلامت ناموفق ❌"
            }
        } finally {
            _testing.value = false
        }
    }

    /**
     * همگام‌سازی کامل — v2.18.0: از دروازهٔ واحد روی اتصال مستقیم SQL.
     * (قبلاً به مسیر Retrofit/API می‌رفت که در این نسخهٔ نصبی وجود ندارد.)
     */
    fun syncNow() = viewModelScope.launch {
        if (_syncing.value) return@launch
        _syncing.value = true
        try {
            val report: SqldirectSync.Report = VizitorGateway.syncAll(db())
            _syncReport.value = SyncReport(
                pushedInvoices = 0,
                pulledRecords = report.products + report.customers + report.invoices,
                errors = report.warnings,
            )
            _toast.value = report.summary
            // پس از همگام‌سازی، «اطلاع‌رسانی اولیه» و «فعالیت‌ها» تازه می‌شوند
            loadBriefing(force = true)
            loadActivities(force = true)
        } catch (e: Exception) {
            _syncReport.value = SyncReport(0, 0, listOf(e.message ?: "خطای اتصال"))
            _toast.value = "همگام‌سازی ناموفق: ${e.message}"
        } finally {
            _syncing.value = false
        }
    }

    fun computeDiscount(gross: Long, cash: Boolean): Long = repo.computeDiscount(gross, cash)

    // ══════════════════════════════════════════════════════════════════════════
    //  v2.18.0 — «اطلاع‌رسانی اولیه» و «فعالیت‌های ویزیتور»
    //  همهٔ فراخوانی‌های سرور از دروازهٔ واحد (VizitorGateway) عبور می‌کنند:
    //  اتصال تضمین می‌شود، مسیر قدیمی API (که در این نسخه وجود ندارد) کنار گذاشته
    //  شده و هیچ دکمه‌ای بی‌نتیجه نمی‌ماند.
    // ══════════════════════════════════════════════════════════════════════════

    /** وضعیت «اطلاع‌رسانی اولیه». */
    private val _briefing = MutableStateFlow<VizitorGateway.Briefing?>(null)
    val briefing: StateFlow<VizitorGateway.Briefing?> = _briefing.asStateFlow()

    private val _briefingBusy = MutableStateFlow(false)
    val briefingBusy: StateFlow<Boolean> = _briefingBusy.asStateFlow()

    /** آیا ویزیتور «اطلاع‌رسانی اولیه» این نسخه را دیده است؟ */
    private val _briefingSeen = MutableStateFlow(prefs.getBoolean(KEY_BRIEFING_SEEN, false))
    val briefingSeen: StateFlow<Boolean> = _briefingSeen.asStateFlow()

    fun markBriefingSeen() {
        prefs.edit().putBoolean(KEY_BRIEFING_SEEN, true).apply()
        _briefingSeen.value = true
    }

    /** سوئیچ «ثبت واقعی پیش‌فاکتور در آتیران» (پیش‌فرض: پیش‌نمایش). */
    private val _liveInvoiceWrite = MutableStateFlow(prefs.getBoolean(KEY_LIVE_WRITE, false))
    val liveInvoiceWrite: StateFlow<Boolean> = _liveInvoiceWrite.asStateFlow()

    fun setLiveInvoiceWrite(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LIVE_WRITE, enabled).apply()
        _liveInvoiceWrite.value = enabled
        showToast(
            if (enabled) "ثبت واقعی پیش‌فاکتور فعال شد ⚠️ (فقط پس از تأیید حسابدار)"
            else "ثبت واقعی خاموش شد — سند فقط پیش‌نمایش می‌شود ✅"
        )
    }

    /** بارگذاری داده‌های «اطلاع‌رسانی اولیه» (اتصال + اختیارات + دادهٔ گوشی). */
    fun loadBriefing(force: Boolean = false) {
        if (_briefingBusy.value) return
        if (!force && _briefing.value != null) return
        viewModelScope.launch {
            _briefingBusy.value = true
            _briefing.value = runCatching { VizitorGateway.briefing(db()) }.getOrElse { null }
            _briefingBusy.value = false
        }
    }

    /** «فعالیت‌های ویزیتور»: ویزیت‌های امروز/اخیر، مسیرها، سهمیه و بدهکاران. */
    private val _activities = MutableStateFlow<ActivityFeed?>(null)
    val activities: StateFlow<ActivityFeed?> = _activities.asStateFlow()

    private val _activitiesBusy = MutableStateFlow(false)
    val activitiesBusy: StateFlow<Boolean> = _activitiesBusy.asStateFlow()

    fun loadActivities(force: Boolean = false) {
        if (_activitiesBusy.value) return
        if (!force && _activities.value != null) return
        viewModelScope.launch {
            _activitiesBusy.value = true
            val session = VizitorSession.current
            val visits = runCatching { VizitorGateway.recentVisits(session.visitorRdf, 40) }
                .getOrDefault(emptyList())
            val routes = runCatching { VizitorGateway.routesFor(session.visitorRdf) }
                .getOrDefault(emptyList())
            val limits = runCatching {
                val uid = session.erpUserId
                if (uid == null) VizitorGateway.VisitorLimits()
                else VizitorGateway.limits(uid, session.companyId)
            }.getOrDefault(VizitorGateway.VisitorLimits())
            val customers = runCatching { repo.customers.first() }.getOrDefault(emptyList())
            val debtors = customers.filter { it.debt > 0L }
                .sortedByDescending { it.debt }
                .take(20)
                .map { it.name to it.debt }
            val byDay = visits.groupBy { it.dateCreated }
                .map { (day, rows) -> day to rows.size.toLong() }
                .sortedBy { it.first }
                .takeLast(7)
            _activities.value = ActivityFeed(
                visitsToday = runCatching { VizitorGateway.todayVisits(session.visitorRdf) }.getOrDefault(0),
                visits = visits,
                routes = routes,
                limits = limits,
                debtors = debtors,
                weeklyVisits = byDay,
                connected = SqlConnectionManager.connected(),
            )
            _activitiesBusy.value = false
        }
    }

    private fun db(): ir.atiran.vizitor.data.local.AppDatabase =
        ir.atiran.vizitor.data.local.AppDatabase.getInstance(getApplication())

    private companion object {
        const val KEY_BRIEFING_SEEN = "briefing_seen_2180"
        const val KEY_LIVE_WRITE = "live_invoice_write"
    }
}

/**
 * داده‌های صفحهٔ «فعالیت‌های ویزیتور» (v2.18.0) — همه از سرور واقعی/دیتابیس
 * محلی، بدون هیچ مقدار ساختگی.
 */
data class ActivityFeed(
    val visitsToday: Int = 0,
    val visits: List<VisitRow> = emptyList(),
    val routes: List<VizitorGateway.RouteRow> = emptyList(),
    val limits: VizitorGateway.VisitorLimits = VizitorGateway.VisitorLimits(),
    val debtors: List<Pair<String, Long>> = emptyList(),
    val weeklyVisits: List<Pair<String, Long>> = emptyList(),
    val connected: Boolean = false,
)

/** وضعیت نمایشی تست سلامت سرور در صفحه گزارشات. */
data class HealthUiState(
    val ok: Boolean,
    val report: HealthReport?,
    val error: String?
)
