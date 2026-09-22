/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | مغزِ اتصال مستقیم به SQL Server (پورت ۱۴۳۳)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  سه کار را با کمترین کلیک انجام می‌دهد:
 *    ۱) «اتصال سریع»: با تنظیمات ذخیره‌شده یک‌ضربه‌ای وصل می‌شود (کارت/تایپ لازم نیست)
 *    ۲) «ورود و همگام‌سازی»: ورود با dbo.sys_users + پر شدن کالا/مشتری/فاکتور
 *    ۳) «به‌خاطر سپردن»: نام کاربری و رمز ویزیتور با AES-GCM + Keystore ذخیره می‌شود
 *       تا اجرای بعدی فقط با یک ضربه روی نقش، به پنل برسد.
 *  همهٔ عملیات روی Dispatchers.IO است و رمزی هرگز چاپ/لاگ/نمایش داده نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.atiran.vizitor.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** وضعیت فرم صفحهٔ «اتصال به سرور». */
data class DirectUiState(
    // کارت اتصال نصب‌کننده
    val cardText: String = "",
    val cardExpanded: Boolean = false,
    // سرور و دیتابیس
    val host: String = "",
    val publicHost: String = "",
    val port: String = "1433",
    val database: String = "",
    val usePublicHost: Boolean = false,
    val useEncryption: Boolean = true,
    // اعتبارنامهٔ کاربر محدود دیتابیس
    val dbUser: String = "",
    val dbPassword: String = "",
    // ورود ویزیتور (dbo.sys_users)
    val erpUser: String = "",
    val erpPassword: String = "",
    val rememberMe: Boolean = false,
    // وضعیت
    val busy: Boolean = false,
    val connected: Boolean = false,
    val loggedIn: Boolean = false,
    val settingsExpanded: Boolean = false,
    val loginExpanded: Boolean = false,
    val detailsExpanded: Boolean = false,
    val status: String = "",
    val statusKind: Int = 0,          // ۰=اطلاع، ۱=موفق، ۲=خطا
    val databases: List<String> = emptyList(),
    val serverInfo: String = "",
    val health: Map<String, Boolean> = emptyMap(),
    val visitors: List<DirectVisitorRow> = emptyList(),
    val columns: List<DirectColumn> = emptyList(),
    val visitorCount: Int = 0,
    val loggedInUser: String = "",
    val loggedInName: String = "",
    val loggedInUserId: Int? = null,
    val loggedInCompanyId: Int? = null,
    val allowedCustomers: Int = 0,
    val allowedProducts: Int = 0,
    val allowedWarehouses: Int = 0,
    val visitorsOfUser: Int = 0,
    // ── ورود از جدول sys_vis (بدون پرسیدن نام کاربری/رمز کاربر آتیران) ──────
    val visitorOptions: List<VisitorOption> = emptyList(),
    val visitorPickerOpen: Boolean = false,
    val visitorsLoading: Boolean = false,
    val selectedVisitorRdf: Int? = null,
    val selectedVisitorName: String = "",
    val visitorFilter: String = "",
)

class DirectSqlViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(DirectUiState())
    val state: StateFlow<DirectUiState> = _state.asStateFlow()

    private val data get() = MeelanoDataSource(SqlConnectionManager)
    private val db: AppDatabase get() = AppDatabase.getInstance(getApplication())

    init {
        SecureDbStore.init(app)
        VizitorSession.refreshFromStore()
        restoreSaved()
    }

    /** خواندن تنظیمات ذخیره‌شده (اتصال + اعتبارنامهٔ ویزیتور) و پر کردن فرم. */
    fun restoreSaved() {
        val saved = SecureDbStore.load()
        val erp = SecureDbStore.loadErp()
        if (saved == null && erp == null) return
        val (external, local, useExternal) = SecureDbStore.loadAddresses()
        _state.update {
            it.copy(
                host = local.ifBlank { saved?.host.orEmpty() }.ifBlank { it.host },
                publicHost = external.ifBlank { it.publicHost },
                usePublicHost = if (external.isNotBlank()) useExternal else it.usePublicHost,
                port = (saved?.port ?: 1433).toString(),
                database = saved?.database.orEmpty().ifBlank { it.database },
                dbUser = saved?.username.orEmpty().ifBlank { it.dbUser },
                dbPassword = saved?.password.orEmpty(),
                useEncryption = saved?.useEncryption ?: it.useEncryption,
                erpUser = erp?.username.orEmpty().ifBlank { it.erpUser },
                erpPassword = erp?.password.orEmpty(),
                rememberMe = erp?.remember == true,
                // اگر تنظیمات از قبل هست، بخش‌های فنی بسته می‌مانند (صفحه شلوغ نمی‌شود)
                settingsExpanded = saved == null,
                loginExpanded = erp?.remember != true,
                status = if (it.status.isBlank() && saved != null)
                    "اتصال روی این گوشی ذخیره شده است — «اتصال سریع» را بزنید یا نام کاربری/رمز خود را وارد کنید."
                else it.status,
                statusKind = if (it.status.isBlank() && saved != null) 1 else it.statusKind,
            )
        }
        // سرور و دیتابیس را به آیکن‌ها/وضعیت مشترک هم بدهیم
        publishSession()
    }

    // ── فرم ────────────────────────────────────────────────────────────────
    fun onCardText(v: String) = _state.update { it.copy(cardText = v) }
    fun onHost(v: String) = _state.update { it.copy(host = v.trim()) }
    fun onPublicHost(v: String) = _state.update { it.copy(publicHost = v.trim()) }
    fun onPort(v: String) = _state.update { it.copy(port = v.filter { ch -> ch.isDigit() }) }
    fun onDatabase(v: String) = _state.update { it.copy(database = v.trim()) }
    fun onDbUser(v: String) = _state.update { it.copy(dbUser = v.trim()) }
    fun onDbPassword(v: String) = _state.update { it.copy(dbPassword = v) }
    fun onErpUser(v: String) = _state.update { it.copy(erpUser = v.trim()) }
    fun onErpPassword(v: String) = _state.update { it.copy(erpPassword = v) }
    fun onRememberMe(v: Boolean) = _state.update { it.copy(rememberMe = v) }
    fun onUsePublicHost(v: Boolean) = _state.update { it.copy(usePublicHost = v) }
    fun onUseEncryption(v: Boolean) = _state.update { it.copy(useEncryption = v) }
    fun toggleCard() = _state.update { it.copy(cardExpanded = !it.cardExpanded) }
    fun toggleSettings() = _state.update { it.copy(settingsExpanded = !it.settingsExpanded) }
    fun toggleLogin() = _state.update { it.copy(loginExpanded = !it.loginExpanded) }
    fun toggleDetails() = _state.update { it.copy(detailsExpanded = !it.detailsExpanded) }

    /** خواندن کارت اتصال نصب‌کننده (متن، QR یا android-connect.json). */
    fun applyCard() {
        val card = ConnectCards.parse(_state.value.cardText)
        if (card == null) {
            fail("کارت خوانده نشد. متن کارت را کامل بچسبانید (باید با vizitor:// شروع شود) " +
                "یا محتوای فایل android-connect.json را بچسبانید.")
            return
        }
        _state.update {
            it.copy(
                host = card.hostLan.ifBlank { it.host },
                publicHost = card.hostPublic.ifBlank { it.publicHost },
                usePublicHost = card.hostLan.isBlank() && card.hostPublic.isNotBlank(),
                port = card.port.toString(),
                database = card.database.ifBlank { it.database },
                dbUser = card.login.ifBlank { it.dbUser },
                cardExpanded = false,
                settingsExpanded = true,
                status = "کارت اتصال خوانده شد (${card.source}) ✅ — " +
                    "اگر بیرون از شبکه هستید کلید «اتصال از بیرون» را روشن کنید، سپس رمز کاربر دیتابیس را بزنید.",
                statusKind = 1,
            )
        }
    }

    // ── اتصال ──────────────────────────────────────────────────────────────
    private fun activeHost(): String =
        if (_state.value.usePublicHost && _state.value.publicHost.isNotBlank()) _state.value.publicHost
        else _state.value.host

    private fun settings(database: String? = null): DbSettings {
        val s = _state.value
        return DbSettings(
            host = activeHost(),
            port = s.port.toIntOrNull() ?: 1433,
            database = database ?: s.database,
            username = s.dbUser,
            password = s.dbPassword,
            useEncryption = s.useEncryption,
            trustServerCert = true,
        )
    }

    private fun fail(message: String) {
        _state.update { it.copy(busy = false, status = message, statusKind = 2) }
        VizitorSession.endBusy()
        VizitorSession.setStatus(message, 2)
    }

    /** آیا ویزیتور (از sys_vis) روی گوشی ذخیره شده است؟ (برای ورود سریع بدون پرسیدن رمز) */
    private var visitorSavedCache: Boolean? = null

    private fun hasSavedVisitor(): Boolean {
        visitorSavedCache?.let { return it }
        val saved = runCatching { SecureDbStore.loadVisitor() != null }.getOrDefault(false)
        visitorSavedCache = saved
        return saved
    }

    private fun publishSession() {
        val s = _state.value
        VizitorSession.update {
            it.copy(
                configured = s.host.isNotBlank() && s.database.isNotBlank() && s.dbUser.isNotBlank(),
                connected = s.connected,
                loggedIn = s.loggedIn,
                // ورود سریع: یا اعتبارنامهٔ آتیران ذخیره شده، یا ویزیتور sys_vis انتخاب‌شده داریم
                credentialsSaved = (s.rememberMe && s.erpUser.isNotBlank()) || hasSavedVisitor(),
                host = s.host, publicHost = s.publicHost, usePublicHost = s.usePublicHost,
                port = s.port.toIntOrNull() ?: 1433, database = s.database,
                erpUser = s.loggedInUser.ifBlank { s.erpUser },
                erpName = s.loggedInName,
                erpUserId = s.loggedInUserId, companyId = s.loggedInCompanyId,
                serverInfo = s.serverInfo,
                visitorsCount = s.visitorsOfUser,
            )
        }
    }

    /** گام نصب‌کننده: تست اتصال + فهرست دیتابیس‌ها (انتخاب یا تایپ دستی). */
    fun fetchDatabases() {
        val s = _state.value
        if (activeHost().isBlank() || s.dbUser.isBlank() || s.dbPassword.isBlank()) {
            fail("آدرس سرور، نام کاربری و رمز کاربر دیتابیس لازم است (کارت اتصال نام کاربری را پر می‌کند).")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال اتصال به ${activeHost()}:${s.port} …", statusKind = 0) }
            VizitorSession.beginBusy("در حال اتصال به سرور…")
            SqlConnectionManager.listDatabases(settings(database = "master")).fold(
                onSuccess = { list ->
                    _state.update {
                        it.copy(
                            busy = false, databases = list,
                            status = "اتصال برقرار است ✅ — ${list.size} دیتابیس پیدا شد. " +
                                "دیتابیس حسابداری را از فهرست انتخاب یا دستی تایپ کنید.",
                            statusKind = 1,
                        )
                    }
                    VizitorSession.endBusy()
                    VizitorSession.setStatus("سرور پاسخ داد ✅ — ${list.size} دیتابیس", 1)
                },
                onFailure = { e ->
                    fail(
                        "اتصال ناموفق ❌ — ${e.message ?: e.javaClass.simpleName}\n" +
                            "بررسی کنید SQL Server روشن است، سرویس روی پورت ${s.port} گوش می‌دهد و فایروال اجازه می‌دهد " +
                            "(نصب‌کننده این‌ها را آماده می‌کند). اگر سرور قدیمی است و پیام مربوط به TLS بود، " +
                            "کلید «رمزنگاری TLS» را خاموش کنید."
                    )
                },
            )
        }
    }

    /** ذخیرهٔ رمزنگاری‌شدهٔ تنظیمات اتصال (بدون نیاز به اتصال). */
    fun saveSettings() {
        val s = _state.value
        if (activeHost().isBlank() || s.database.isBlank() || s.dbUser.isBlank()) {
            fail("برای ذخیره، آدرس سرور، نام دیتابیس و نام کاربر دیتابیس لازم است.")
            return
        }
        SecureDbStore.save(settings())
        SecureDbStore.saveAddresses(
            hostExternal = s.publicHost,
            hostLocal = s.host,
            useExternal = s.usePublicHost && s.publicHost.isNotBlank(),
        )
        SecureDbStore.saveErp(s.erpUser, s.erpPassword, s.rememberMe && s.erpUser.isNotBlank())
        publishSession()
        _state.update {
            it.copy(
                status = "تنظیمات با AES-GCM + Android Keystore ذخیره شد ✅ " +
                    "(" + it.dbUser + "@" + it.host + ":" + it.port + "/" + it.database + ")" +
                    if (it.rememberMe) " — ورود سریع برای اجرای بعدی فعال شد." else "",
                statusKind = 1,
            )
        }
    }

    /**
     * اتصال به دیتابیس. اگر موفق بود: تنظیمات ذخیره می‌شود، سلامت مسیر پیش‌فاکتور
     * خوانده می‌شود و در صورت وجود اعتبارنامهٔ ذخیره‌شده، ورود هم خودکار انجام می‌گیرد.
     */
    fun connectDatabase(autoLoginAfter: Boolean = false) {
        val s = _state.value
        if (activeHost().isBlank() || s.database.isBlank() || s.dbUser.isBlank() || s.dbPassword.isBlank()) {
            fail("آدرس سرور، نام دیتابیس، نام کاربری و رمز کاربر دیتابیس لازم است.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال اتصال به دیتابیس ${s.database} …", statusKind = 0) }
            VizitorSession.beginBusy("در حال اتصال به دیتابیس ${s.database}…")
            val cfg = settings()
            if (!SqlConnectionManager.connect(cfg)) {
                val message = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                fail(
                    "اتصال برقرار نشد ❌ — ${message ?: "خطای نامشخص"}\n" +
                        "نام دیتابیس را دقیق بنویسید (قابل تایپ دستی است) و دسترسی کاربر را بررسی کنید."
                )
                return@launch
            }
            SecureDbStore.save(settings())
            SecureDbStore.saveAddresses(
                hostExternal = s.publicHost, hostLocal = s.host,
                useExternal = s.usePublicHost && s.publicHost.isNotBlank(),
            )
            val info = runCatching { data.databaseInfo() }.getOrNull()
            val health = runCatching { data.preInvoiceHealth() }.getOrElse { emptyMap() }
            _state.update {
                it.copy(
                    busy = false, connected = true,
                    serverInfo = info?.let { t -> "${t.first} • SQL Server ${t.second} • ${t.third} مشتری" }.orEmpty(),
                    health = health,
                    status = "اتصال برقرار و تنظیمات ذخیره شد ✅ — حالا ورود ویزیتور را کامل کنید.",
                    statusKind = 1,
                )
            }
            publishSession()
            VizitorSession.endBusy()
            VizitorSession.setStatus("اتصال برقرار شد ✅ — ${cfg.database}", 1)

            val savedErp = SecureDbStore.loadErp()
            if (autoLoginAfter && savedErp != null && savedErp.remember && savedErp.password.isNotBlank()) {
                loginAndSync(savedErp.username, savedErp.password)
            } else if (!_state.value.loggedIn) {
                // اتصال برقرار شد ولی ورود انجام نشده ⇒ فهرست ویزیتورهای واقعی
                // (dbo.sys_vis) خودکار خوانده می‌شود تا کاربر فقط انتخاب کند.
                loadVisitorsForLogin(force = true)
            }
        }
    }

    /** ورود سریع: اتصال با تنظیمات ذخیره‌شده + ورود با اعتبارنامهٔ ذخیره‌شده + همگام‌سازی. */
    fun quickEnter() {
        val saved = SecureDbStore.load()
        val erp = SecureDbStore.loadErp()
        if (saved == null || saved.host.isBlank() || saved.database.isBlank()) {
            fail("هنوز اتصالی ذخیره نشده است — «تنظیم اتصال» را کامل کنید.")
            return
        }
        // مسیر تازهٔ پیش‌فرض: ویزیتور ذخیره‌شده از sys_vis (بدون نام کاربری/رمز آتیران)
        val savedVisitor = SecureDbStore.loadVisitor()
        if (savedVisitor != null && savedVisitor.visitorRdf > 0 &&
            (erp == null || !erp.remember || erp.password.isBlank())
        ) {
            val (external, local, useExternal) = SecureDbStore.loadAddresses()
            _state.update {
                it.copy(
                    host = local.ifBlank { saved.host }, publicHost = external,
                    usePublicHost = useExternal && external.isNotBlank(),
                    port = saved.port.toString(), database = saved.database,
                    dbUser = saved.username, dbPassword = saved.password,
                    useEncryption = saved.useEncryption,
                )
            }
            enterAsVisitor(
                VisitorOption(
                    userId = savedVisitor.userId,
                    companyId = savedVisitor.companyId,
                    visitorRdf = savedVisitor.visitorRdf,
                    name = savedVisitor.name,
                    cell = "", region = null, city = null, active = "t",
                    allowedCustomers = 0, allowedProducts = 0, allowedWarehouses = 0,
                )
            )
            return
        }
        if (erp == null || !erp.remember || erp.password.isBlank() || erp.username.isBlank()) {
            // تنظیمات هست ولی نه ویزیتور ذخیره‌شده و نه اعتبارنامهٔ آتیران → انتخاب ویزیتور
            val (external, local, useExternal) = SecureDbStore.loadAddresses()
            _state.update {
                it.copy(
                    host = local.ifBlank { saved.host }, publicHost = external,
                    usePublicHost = useExternal && external.isNotBlank(),
                    port = saved.port.toString(), database = saved.database,
                    dbUser = saved.username, dbPassword = saved.password,
                    useEncryption = saved.useEncryption,
                    settingsExpanded = false, loginExpanded = true,
                    status = "اتصال آماده است — نام کاربری و رمز خود را بزنید و «ورود و همگام‌سازی» را انتخاب کنید.",
                    statusKind = 0,
                )
            }
            return
        }
        val (external, local, useExternal) = SecureDbStore.loadAddresses()
        _state.update {
            it.copy(
                host = local.ifBlank { saved.host }, publicHost = external,
                usePublicHost = useExternal && external.isNotBlank(),
                port = saved.port.toString(), database = saved.database,
                dbUser = saved.username, dbPassword = saved.password,
                useEncryption = saved.useEncryption,
                erpUser = erp.username, erpPassword = erp.password, rememberMe = true,
                loginExpanded = false, settingsExpanded = false,
            )
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "اتصال سریع به ${saved.database} …", statusKind = 0) }
            VizitorSession.beginBusy("ورود سریع به سامانه…")
            if (!SqlConnectionManager.connect(settings())) {
                val message = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                fail("اتصال سریع ناموفق بود ❌ — ${message ?: "خطای نامشخص"}")
                return@launch
            }
            _state.update { it.copy(connected = true) }
            val info = runCatching { data.databaseInfo() }.getOrNull()
            val health = runCatching { data.preInvoiceHealth() }.getOrElse { emptyMap() }
            _state.update {
                it.copy(
                    serverInfo = info?.let { t -> "${t.first} • SQL Server ${t.second} • ${t.third} مشتری" }.orEmpty(),
                    health = health,
                )
            }
            loginAndSync(erp.username, erp.password)
        }
    }

    // ══════════════════ ورود از جدول واقعی dbo.sys_vis ═══════════════════════
    //  جریان تازه (خواستهٔ کارفرما): بدون هیچ پرس‌وجوی «نام کاربری/رمز کاربر آتیران»،
    //  ابتدا فهرست ویزیتورها از sys_vis خوانده می‌شود و کاربر فقط ویزیتورش را
    //  انتخاب می‌کند؛ بعد همهٔ داده‌ها با همان UserID/SysID/shvis همگام می‌شوند.

    fun toggleVisitorPicker() = _state.update { it.copy(visitorPickerOpen = !it.visitorPickerOpen) }

    fun onVisitorFilter(v: String) = _state.update { it.copy(visitorFilter = v) }

    /** خواندن فهرست ویزیتورهای قابل انتخاب از sys_vis (نیازمند اتصال برقرار). */
    fun loadVisitorsForLogin(force: Boolean = false) {
        if (!force && _state.value.visitorOptions.isNotEmpty()) {
            _state.update { it.copy(visitorPickerOpen = true) }
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(visitorsLoading = true, visitorPickerOpen = true,
                    status = "در حال خواندن فهرست ویزیتورها از dbo.sys_vis …", statusKind = 0)
            }
            // اگر هنوز وصل نیستیم، با تنظیمات ذخیره‌شده وصل شو (هیچ رمزی پرسیده نمی‌شود
            // چون رمز کاربر محدود دیتابیس از قبل روی گوشی ذخیره شده است).
            if (!_state.value.connected) {
                val saved = SecureDbStore.load()
                if (saved != null) {
                    _state.update {
                        it.copy(
                            host = it.host.ifBlank { saved.host },
                            database = it.database.ifBlank { saved.database },
                            dbUser = it.dbUser.ifBlank { saved.username },
                            dbPassword = it.dbPassword.ifBlank { saved.password },
                            useEncryption = saved.useEncryption,
                        )
                    }
                }
                if (!SqlConnectionManager.connect(settings())) {
                    val msg = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                    _state.update {
                        it.copy(visitorsLoading = false, status = "اتصال به سرور برقرار نشد ❌ — ${msg ?: "خطای نامشخص"}", statusKind = 2)
                    }
                    return@launch
                }
                SecureDbStore.save(settings())
                _state.update { it.copy(connected = true) }
                publishSession()
            }
            val options = runCatching { VisitorLoginRepository.options(companyId = 0) }
                .getOrElse { emptyList() }
            val savedVisitor = SecureDbStore.loadVisitor()
            _state.update {
                it.copy(
                    visitorOptions = options,
                    visitorsLoading = false,
                    selectedVisitorRdf = savedVisitor?.visitorRdf ?: it.selectedVisitorRdf,
                    selectedVisitorName = savedVisitor?.name ?: it.selectedVisitorName,
                    status = if (options.isEmpty())
                        "در جدول dbo.sys_vis ویزیتوری برای انتخاب پیدا نشد — دسترسی کاربر دیتابیس و دادهٔ sys_vis را بررسی کنید."
                    else
                        "فهرست ویزیتورها آماده است ✅ — ${options.size} ویزیتور؛ یکی را انتخاب کنید.",
                    statusKind = if (options.isEmpty()) 2 else 1,
                )
            }
        }
    }

    /** ورود به‌عنوان ویزیتور انتخاب‌شده از sys_vis — بدون نام کاربری/رمز آتیران. */
    fun enterAsVisitor(option: VisitorOption) {
        if (!option.isActive) {
            fail("ویزیتور «${option.label}» در سامانه غیرفعال است (visitors.active = 'f').")
            return
        }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    busy = true, visitorPickerOpen = false,
                    selectedVisitorRdf = option.visitorRdf, selectedVisitorName = option.label,
                    status = "در حال اتصال به‌عنوان ${option.label} …", statusKind = 0,
                )
            }
            VizitorSession.beginBusy("در حال ورود ویزیتور ${option.label}…")
            if (!_state.value.connected && !SqlConnectionManager.connect(settings())) {
                val msg = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                fail("اتصال به سرور برقرار نشد ❌ — ${msg ?: "خطای نامشخص"}")
                return@launch
            }
            SecureDbStore.save(settings())
            SecureDbStore.saveVisitor(
                userId = option.userId, companyId = option.companyId,
                visitorRdf = option.visitorRdf, name = option.label,
            )
            visitorSavedCache = true
            val identity = runCatching { data.visitorIdentity(option.userId, option.companyId) }.getOrNull()
            val visitors = runCatching { VisitorRepository.visitorsForUser(option.userId, option.companyId) }
                .getOrElse { emptyList() }
            val columns = runCatching { VisitorRepository.visitorsColumns() }.getOrElse { emptyList() }
            val total = runCatching { VisitorRepository.visitorCount() }.getOrDefault(0)
            _state.update { st ->
                st.copy(
                    connected = true, loggedIn = true,
                    loggedInUser = option.label, loggedInName = option.label,
                    loggedInUserId = option.userId, loggedInCompanyId = option.companyId,
                    visitors = visitors, visitorsOfUser = visitors.size,
                    columns = columns, visitorCount = total,
                    allowedCustomers = option.allowedCustomers,
                    allowedProducts = option.allowedProducts,
                    allowedWarehouses = option.allowedWarehouses,
                    rememberMe = true,
                    status = "ورود ویزیتور «${option.label}» انجام شد ✅ — در حال همگام‌سازی…",
                    statusKind = 1,
                )
            }
            publishSession()
            VizitorSession.beginSync()
            val report = runCatching {
                SqldirectSync.run(
                    db = db,
                    userId = option.userId,
                    companyId = option.companyId,
                    visitorRdf = identity?.visitorRdf ?: option.visitorRdf,
                )
            }
            report.fold(
                onSuccess = { r ->
                    _state.update { it.copy(busy = false, status = "ویزیتور ${option.label} ✅ — " + r.summary, statusKind = 1) }
                    VizitorSession.update {
                        it.copy(
                            loggedIn = true, connected = true,
                            erpUser = option.label, erpName = option.label,
                            erpUserId = option.userId, companyId = option.companyId,
                            visitorRdf = identity?.visitorRdf ?: option.visitorRdf,
                            productsCount = r.products, customersCount = r.customers,
                            invoicesCount = r.invoices, visitorsCount = visitors.size,
                            credentialsSaved = true,
                        )
                    }
                    VizitorSession.endSync(r.summary)
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(busy = false, statusKind = 2,
                            status = "ورود انجام شد ✅ ولی همگام‌سازی ناتمام ماند — " + (e.message ?: "") + "\nاز «همگام‌سازی» دوباره امتحان کنید.")
                    }
                    VizitorSession.endSync("همگام‌سازی ناتمام: " + (e.message ?: "خطای نامشخص"), 2)
                },
            )
        }
    }

    /** ورود با فرم (دکمهٔ صفحه). */
    fun loginAndLoad() = loginAndSync(_state.value.erpUser, _state.value.erpPassword)

    /**
     * ورود با جدول واقعی dbo.sys_users + همگام‌سازی همهٔ سرویس‌های داده.
     * اگر اعتبارنامه از حافظهٔ امن آمده باشد (اتصال سریع) هم همین مسیر اجرا می‌شود.
     */
    private fun loginAndSync(user: String, password: String) {
        if (user.isBlank() || password.isBlank()) {
            fail("نام کاربری و کلمهٔ عبور خود را (حساب ویزیتور در سامانه) وارد کنید.")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال ورود به سامانه …", statusKind = 0) }
            VizitorSession.beginBusy("در حال ورود به سامانه…")

            if (!_state.value.connected) {
                if (!SqlConnectionManager.connect(settings())) {
                    val message = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                    fail("اتصال به دیتابیس برقرار نشد ❌ — ${message ?: "خطای نامشخص"}")
                    return@launch
                }
                SecureDbStore.save(settings())
                _state.update { it.copy(connected = true) }
            }

            val row = runCatching { data.login(user, password) }.getOrNull()
            if (row == null) {
                fail("نام کاربری یا کلمهٔ عبور درست نیست ❌ — ورود فقط با جدول واقعی کاربران سامانه (dbo.sys_users) انجام می‌شود.")
                return@launch
            }
            if (!row.active) {
                fail("حساب «${row.username}» در سامانه غیرفعال است (sys_users.active = 0).")
                return@launch
            }
            if (row.locked) {
                fail("حساب «${row.username}» قفل است 🔒 — از سامانه بازش کنید.")
                return@launch
            }

            val identity = runCatching { data.visitorIdentity(row.userId, row.companyId) }.getOrNull()
            val visitors = runCatching { VisitorRepository.visitorsForUser(row.userId, row.companyId) }
                .getOrElse { emptyList() }
            val columns = runCatching { VisitorRepository.visitorsColumns() }.getOrElse { emptyList() }
            val total = runCatching { VisitorRepository.visitorCount() }.getOrDefault(0)

            _state.update { st ->
                st.copy(
                    loggedIn = true,
                    loggedInUser = row.username, loggedInName = row.fullName,
                    loggedInUserId = row.userId, loggedInCompanyId = row.companyId,
                    erpUser = row.username, erpPassword = password,
                    visitors = visitors, visitorsOfUser = visitors.size,
                    columns = columns, visitorCount = total,
                    allowedCustomers = identity?.allowedCustomers ?: 0,
                    allowedProducts = identity?.allowedProducts ?: 0,
                    allowedWarehouses = identity?.allowedWarehouses ?: 0,
                    status = "ورود موفق ✅ ${row.fullName.ifBlank { row.username }} — در حال همگام‌سازی داده‌ها…",
                    statusKind = 1,
                )
            }
            // «به‌خاطر سپردن» → ذخیرهٔ رمزنگاری‌شدهٔ اعتبارنامه برای ورود سریع بعدی
            SecureDbStore.saveErp(row.username, password, _state.value.rememberMe)
            publishSession()

            // ── همگام‌سازی: کالا، مشتری، فاکتور (سرویس‌های داده فعال می‌شوند) ──
            VizitorSession.beginSync()
            val report = runCatching {
                SqldirectSync.run(
                    db = db,
                    userId = row.userId,
                    companyId = row.companyId,
                    visitorRdf = identity?.visitorRdf ?: visitors.firstOrNull()?.rdf,
                )
            }
            report.fold(
                onSuccess = { r ->
                    _state.update {
                        it.copy(
                            busy = false,
                            status = "ورود موفق ✅ ${row.fullName.ifBlank { row.username }} — " + r.summary,
                            statusKind = 1,
                        )
                    }
                    VizitorSession.update {
                        it.copy(
                            loggedIn = true, connected = true,
                            erpUser = row.username, erpName = row.fullName,
                            erpUserId = row.userId, companyId = row.companyId,
                            visitorRdf = identity?.visitorRdf ?: visitors.firstOrNull()?.rdf,
                            productsCount = r.products, customersCount = r.customers,
                            invoicesCount = r.invoices, visitorsCount = visitors.size,
                            credentialsSaved = _state.value.rememberMe,
                        )
                    }
                    VizitorSession.endSync(r.summary)
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            busy = false,
                            status = "ورود انجام شد ✅ ولی همگام‌سازی ناتمام ماند — " +
                                (e.message ?: e.javaClass.simpleName) + "\nاز دکمهٔ «همگام‌سازی» دوباره امتحان کنید.",
                            statusKind = 2,
                        )
                    }
                    VizitorSession.endSync("همگام‌سازی ناتمام: " + (e.message ?: "خطای نامشخص"), 2)
                },
            )
        }
    }

    /** همگام‌سازی دستی (از صفحهٔ اتصال، تنظیمات یا صفحهٔ گزارش‌ها). */
    fun syncNow() {
        val s = _state.value
        val uid = s.loggedInUserId ?: VizitorSession.current.erpUserId
        if (uid == null) {
            fail("برای همگام‌سازی اول وارد شوید.")
            return
        }
        val vRdf = VizitorSession.current.visitorRdf ?: s.visitors.firstOrNull()?.rdf
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال همگام‌سازی با سرور …", statusKind = 0) }
            VizitorSession.beginSync()
            if (!_state.value.connected && !SqlConnectionManager.connect(settings())) {
                val message = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                fail("اتصال به سرور برقرار نشد ❌ — ${message ?: "خطای نامشخص"}")
                return@launch
            }
            val report = runCatching {
                SqldirectSync.run(db, uid, s.loggedInCompanyId ?: VizitorSession.current.companyId, vRdf)
            }
            report.fold(
                onSuccess = { r ->
                    _state.update { it.copy(busy = false, status = r.summary, statusKind = 1) }
                    VizitorSession.update {
                        it.copy(productsCount = r.products, customersCount = r.customers, invoicesCount = r.invoices)
                    }
                    VizitorSession.endSync(r.summary)
                },
                onFailure = { e -> fail("همگام‌سازی ناموفق ❌ — ${e.message ?: e.javaClass.simpleName}") },
            )
        }
    }

    /** خواندن دوبارهٔ ویزیتورها + مشخصات سرور (بدون ورود مجدد). */
    fun refreshVisitors() {
        val uid = _state.value.loggedInUserId ?: return
        val company = _state.value.loggedInCompanyId
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val visitors = runCatching { VisitorRepository.visitorsForUser(uid, company) }.getOrElse { emptyList() }
            val columns = runCatching { VisitorRepository.visitorsColumns() }.getOrElse { emptyList() }
            val total = runCatching { VisitorRepository.visitorCount() }.getOrDefault(0)
            _state.update {
                it.copy(
                    busy = false, visitors = visitors, visitorsOfUser = visitors.size,
                    columns = columns, visitorCount = total,
                    status = "اطلاعات ویزیتورها به‌روز شد (${visitors.size} رکورد)", statusKind = 1,
                )
            }
            publishSession()
        }
    }

    /** خروج از حساب (تنظیمات اتصال می‌ماند، وضعیت ورود پاک می‌شود). */
    fun logout() {
        SqlConnectionManager.disconnect()
        SecureDbStore.clearErp()
        SecureDbStore.clearVisitor()   // ویزیتور انتخاب‌شده هم پاک می‌شود (ورود دوباره با انتخاب تازه)
        visitorSavedCache = false
        _state.update {
            it.copy(
                connected = false, loggedIn = false, rememberMe = false,
                erpPassword = "", erpUser = "", visitors = emptyList(), columns = emptyList(),
                visitorOptions = emptyList(), selectedVisitorRdf = null,
                selectedVisitorName = "", visitorFilter = "", visitorPickerOpen = false,
                loggedInUser = "", loggedInName = "", loggedInUserId = null, loggedInCompanyId = null,
                status = "از حساب خارج شدید. (اطلاعات اتصال سرور محفوظ ماند)", statusKind = 0,
            )
        }
        VizitorSession.reset()
        publishSession()
    }

    fun disconnect() {
        SqlConnectionManager.disconnect()
        _state.update {
            it.copy(
                connected = false, loggedIn = false, status = "اتصال قطع شد. (تنظیمات ذخیره‌شده دست‌نخورده مانده است)",
                statusKind = 0, visitors = emptyList(), columns = emptyList(),
                loggedInUser = "", loggedInName = "", loggedInUserId = null, loggedInCompanyId = null,
                serverInfo = "", health = emptyMap(),
            )
        }
        publishSession()
    }

    /** پاک کردن کامل تنظیمات و رمزهای ذخیره‌شده روی همین گوشی. */
    fun clearStored() {
        SecureDbStore.clear()
        SqlConnectionManager.disconnect()
        _state.value = DirectUiState(status = "همهٔ تنظیمات و رمزهای ذخیره‌شده پاک شد.", statusKind = 0)
        VizitorSession.update {
            ServerSession(message = "تنظیمات اتصال پاک شد.", messageKind = 0)
        }
    }
}
