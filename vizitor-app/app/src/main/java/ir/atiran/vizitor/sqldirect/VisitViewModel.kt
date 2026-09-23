/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ویومدیل «ثبت ویزیت» (جدول dbo.Visit)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  بعد از ورود ویزیتور (dbo.sys_users) این ویومدیل بخش‌های ثبت ویزیت را
 *  فعال می‌کند:
 *    • مشتریان مجاز از dbo.sys_cus (همان فیلتر خودِ ERP)
 *    • ثبت ویزیت در dbo.Visit با تاریخ شمسی + ساعت + موقعیت GPS
 *    • فهرست ویزیت‌های اخیر + شمارش امروز
 *  همهٔ عملیات SQL روی Dispatchers.IO است (از طریق SqlConnectionManager).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ir.atiran.vizitor.util.toFaDigits

/** یک مشتری مجاز برای انتخاب در فرم ثبت ویزیت. */
data class VisitCustomerOption(
    val shmo: Int,
    val name: String,
    val phone: String,
    val address: String,
)

/** وضعیت صفحهٔ «ثبت ویزیت». */
data class VisitUiState(
    /** ورود ویزیتور انجام شده و کد ویزیتور مشخص است (بخش‌ها فعال‌اند). */
    val ready: Boolean = false,
    val connected: Boolean = false,
    val visitorName: String = "",
    val visitorRdf: Int? = null,
    val customers: List<VisitCustomerOption> = emptyList(),
    val search: String = "",
    val selectedCustomer: VisitCustomerOption? = null,
    val listExpanded: Boolean = false,
    val duration: Int = 30,
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyM: Double? = null,
    val locating: Boolean = false,
    val busy: Boolean = false,
    val status: String = "",
    val statusKind: Int = 0,
    val visits: List<VisitRow> = emptyList(),
    val todayCount: Int = 0,
)

class VisitViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(VisitUiState())
    val state: StateFlow<VisitUiState> = _state.asStateFlow()

    private val data get() = MeelanoDataSource(SqlConnectionManager)

    init {
        // وقتی ویزیتور وارد شود، بخش‌ها فعال و داده‌ها از سامانه خوانده می‌شوند
        viewModelScope.launch {
            VizitorSession.state.collect { session ->
                val ready = session.loggedIn && session.visitorRdf != null && session.erpUserId != null
                _state.update {
                    it.copy(
                        ready = ready,
                        connected = session.connected,
                        visitorName = session.erpName.ifBlank { session.erpUser },
                        visitorRdf = session.visitorRdf,
                    )
                }
                if (ready && itCustomersEmpty()) {
                    loadCustomers(session)
                    loadHistory()
                }
                if (!ready) {
                    _state.update {
                        it.copy(
                            customers = emptyList(),
                            selectedCustomer = null,
                            visits = emptyList(),
                            todayCount = 0,
                            latitude = null,
                            longitude = null,
                            accuracyM = null,
                        )
                    }
                }
            }
        }
    }

    private fun itCustomersEmpty(): Boolean = _state.value.customers.isEmpty()

    // ── فرم ────────────────────────────────────────────────────────────────
    fun onSearch(v: String) = _state.update { it.copy(search = v, listExpanded = true) }

    fun onDescription(v: String) = _state.update { it.copy(description = v) }

    fun selectCustomer(c: VisitCustomerOption?) = _state.update {
        it.copy(selectedCustomer = c, listExpanded = false, search = "")
    }

    fun changeDuration(delta: Int) = _state.update {
        it.copy(duration = (it.duration + delta).coerceIn(1, 720))
    }

    fun clearCustomer() = _state.update { it.copy(selectedCustomer = null) }

    // ── موقعیت GPS ─────────────────────────────────────────────────────────
    fun startLocating() = _state.update { it.copy(locating = true) }

    /** نتیجهٔ دریافت موقعیت (null = دریافت نشد). */
    fun onLocation(location: Location?) {
        if (location != null) {
            _state.update {
                it.copy(
                    locating = false,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyM = location.accuracy.toDouble(),
                )
            }
        } else {
            // اگر قبلاً موقعیت داشتیم نگهش می‌داریم؛ وگرنه پیام راهنما
            _state.update {
                if (it.latitude != null) {
                    it.copy(locating = false)
                } else {
                    it.copy(
                        locating = false,
                        status = "موقعیت دریافت نشد — می‌توانید ویزیت را بدون مختصات ثبت کنید.",
                        statusKind = 0,
                    )
                }
            }
        }
    }

    /** آخرین موقعیت ذخیره‌شدهٔ دستگاه (بدون درخواست تازه). */
    fun onLastKnown(location: Location?) {
        if (location == null) return
        if (_state.value.latitude == null) {
            _state.update {
                it.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracyM = location.accuracy.toDouble(),
                )
            }
        }
    }

    fun clearStatus() = _state.update { it.copy(status = "") }

    /** پاک‌کردن موقعیت (کاربر می‌خواهد بدون مختصات یا با موقعیت تازه ثبت کند). */
    fun onLocationCleared() = _state.update {
        it.copy(latitude = null, longitude = null, accuracyM = null)
    }

    // ── داده از سامانه ────────────────────────────────────────────────────
    private fun loadCustomers(session: ServerSession) {
        val uid = session.erpUserId ?: return
        viewModelScope.launch {
            val rows = runCatching {
                data.customersFor(userId = uid, companyId = session.companyId, limit = 500)
            }.getOrElse { e ->
                _state.update {
                    it.copy(
                        status = "خواندن مشتریان مجاز ناموفق بود: ${e.message ?: e.javaClass.simpleName}",
                        statusKind = 2,
                    )
                }
                emptyList()
            }
            _state.update {
                it.copy(
                    customers = rows.map { c ->
                        VisitCustomerOption(
                            shmo = c.shmo,
                            name = c.name,
                            phone = c.phone,
                            address = c.address,
                        )
                    },
                )
            }
        }
    }

    /** خواندن ویزیت‌های اخیر + شمارش امروز (از دکمهٔ تازه‌سازی یا بعد از ثبت). */
    fun loadHistory() {
        val rdf = _state.value.visitorRdf ?: return
        viewModelScope.launch {
            val (list, today) = runCatching {
                VisitRepository.recentVisits(rdf, 60) to
                    VisitRepository.countToday(rdf, VisitDate.todayJalali())
            }.getOrElse { e ->
                if (_state.value.visits.isEmpty()) {
                    _state.update {
                        it.copy(
                            status = "خواندن ویزیت‌های اخیر ناموفق بود: ${e.message ?: e.javaClass.simpleName}",
                            statusKind = 2,
                        )
                    }
                }
                Pair(emptyList<VisitRow>(), 0)
            }
            _state.update { it.copy(visits = list, todayCount = today) }
        }
    }

    /** بارگذاری کامل (تازه‌سازی): مشتریان + ویزیت‌های اخیر. */
    fun refreshAll() {
        val session = VizitorSession.current
        if (session.loggedIn && session.erpUserId != null) {
            loadCustomers(session)
        }
        loadHistory()
    }

    // ── ثبت ویزیت ──────────────────────────────────────────────────────────
    fun record() {
        val s = _state.value
        if (!s.ready) {
            _state.update { it.copy(status = "برای ثبت ویزیت باید وارد سامانه باشید.", statusKind = 2) }
            return
        }
        val cust = s.selectedCustomer
        if (cust == null) {
            _state.update { it.copy(status = "اول مشتری ویزیت را انتخاب کنید.", statusKind = 2) }
            return
        }
        if (!s.connected) {
            _state.update { it.copy(status = "اتصال به سرور قطع است — دوباره وصل شوید.", statusKind = 2) }
            return
        }
        val rdf = s.visitorRdf ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, status = "در حال ثبت ویزیت در سامانه…", statusKind = 0) }
            val now = VisitDate.now()
            val result = runCatching {
                VisitRepository.recordVisit(
                    visRdf = rdf,
                    shmo = cust.shmo,
                    durationMin = s.duration,
                    lat = s.latitude,
                    lng = s.longitude,
                    description = s.description.trim(),
                    dateCreated = now.dateText,
                    timeCreated = now.timeText,
                )
            }
            result.fold(
                onSuccess = { id ->
                    _state.update { st ->
                        st.copy(
                            busy = false,
                            description = "",
                            duration = 30,
                            status = "ویزیت «${cust.name}» در سامانه ثبت شد ✅ " +
                                "(شمارهٔ ثبت: ${id.toString().toFaDigits()}, تاریخ ${now.dateText})",
                            statusKind = 1,
                        )
                    }
                    loadHistory()
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(busy = false, status = friendlyVisitError(e), statusKind = 2)
                    }
                },
            )
        }
    }
}
