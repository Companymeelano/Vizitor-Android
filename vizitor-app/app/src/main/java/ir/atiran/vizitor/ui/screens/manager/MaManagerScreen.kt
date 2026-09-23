/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | «گزارش مدیریت» — اتاق فرمان مدیر (v2.20.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  بخش گزارش مدیریت، هم‌شکل با برنامهٔ مرجع «M•REPORT» (نسخهٔ پیوست app-debug-40):
 *
 *    ▸ برگهٔ «نمای کلی»    — فهرست واقعی جدول‌های سرور + جست‌وجوی نام جدول +
 *                            شمار رکورد هر جدول + حجم دیتابیس.
 *    ▸ برگهٔ «مرور داده‌ها» — صفحه‌بندی واقعی جدول با ROW_NUMBER و جست‌وجوی
 *                            زنده در ستون‌های متنی + جزئیات کامل رکورد.
 *    ▸ برگهٔ «اتصال جداول» — نگاشت پنج بخش گزارش (مشتریان، کالاها، فروش و
 *                            فاکتورها، چک‌ها، بانک‌ها) به جدول‌های واقعی سرور،
 *                            با تشخیص خودکار از روی نام‌های حدسی فارسی/انگلیسی.
 *    ▸ برگهٔ «تنظیم اتصال» — همان دو نشانی مرجع (شبکهٔ داخلی/اینترنت)، پورت،
 *                            دیتابیس، کاربر و رمز + کاوش چهارحالته + عیب‌یابی گام‌به‌گام.
 *    ▸ برگهٔ «گزارش‌ها»    — مجموع ستون مبلغ، برترین‌ها بر اساس مبلغ، آخرین
 *                            رکوردها و خلاصهٔ سرور؛ همه از دادهٔ واقعی.
 *
 *  قاعدهٔ ثابت مرجع: «اتصال فقط-خواندنی است» — هیچ چیزی روی سرور تغییر نمی‌کند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens.manager

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.sqldirect.DbSettings
import ir.atiran.vizitor.sqldirect.MaSection
import ir.atiran.vizitor.sqldirect.MaSectionMap
import ir.atiran.vizitor.sqldirect.MaSectionStore
import ir.atiran.vizitor.sqldirect.MaMapping
import ir.atiran.vizitor.sqldirect.MaSqlEngine
import ir.atiran.vizitor.sqldirect.MaTable
import ir.atiran.vizitor.sqldirect.MaTableHeuristics
import ir.atiran.vizitor.sqldirect.MaTableRoles
import ir.atiran.vizitor.sqldirect.MaPage
import ir.atiran.vizitor.sqldirect.MaOverview
import ir.atiran.vizitor.sqldirect.MaDiagStep
import ir.atiran.vizitor.sqldirect.SecureDbStore
import ir.atiran.vizitor.ui.components.Lux3DBarChart
import ir.atiran.vizitor.ui.components.Lux3DNote
import ir.atiran.vizitor.ui.components.Lux3DTable
import ir.atiran.vizitor.ui.components.MaAmber
import ir.atiran.vizitor.ui.components.MaDocCard
import ir.atiran.vizitor.ui.components.MaGoldCta
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MaHeroTitle
import ir.atiran.vizitor.ui.components.MaMetricCard
import ir.atiran.vizitor.ui.components.MaNavItem
import ir.atiran.vizitor.ui.components.MaNavStrip
import ir.atiran.vizitor.ui.components.MaOrbButton
import ir.atiran.vizitor.ui.components.MaRed
import ir.atiran.vizitor.ui.components.MaSectionHeader
import ir.atiran.vizitor.ui.components.MaSegmentPills
import ir.atiran.vizitor.ui.components.MaSmartGroup
import ir.atiran.vizitor.ui.components.MaSmartSheet
import ir.atiran.vizitor.ui.components.MaSmartTile
import ir.atiran.vizitor.ui.components.MaStatStrip
import ir.atiran.vizitor.ui.components.MaThinBar
import ir.atiran.vizitor.ui.components.MaTopBar
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import kotlinx.coroutines.launch

// ═══════════════════════════ مدل وضعیت صفحه ═══════════════════════════

/** یک بلوک گزارش (عنوان + ردیف‌های متنی + نمودار میله‌ای اختیاری). */
private data class MaBlock(
    val title: String,
    val subtitle: String,
    val rows: List<Pair<String, String>>,
    val chart: List<Pair<String, Long>> = emptyList(),
    val chartCaption: String = "",
    val emptyText: String = "رکوردی یافت نشد",
)

private class MaStudioState {
    // ── ناوبری
    var tab by mutableStateOf("overview")
    var busy by mutableStateOf(false)
    var msg by mutableStateOf<String?>(null)
    var msgOk by mutableStateOf(true)

    // ── تنظیم اتصال (دو نشانی، مثل نسخهٔ مرجع: داخلی/اینترنت)
    var hostExternal by mutableStateOf("")
    var hostLocal by mutableStateOf("")
    var useExternal by mutableStateOf(false)
    var port by mutableStateOf("1433")
    var database by mutableStateOf("")
    var user by mutableStateOf("")
    var pass by mutableStateOf("")
    var remember by mutableStateOf(true)
    var showPass by mutableStateOf(false)
    var useTls by mutableStateOf(true)

    // ── وضعیت اتصال
    var connected by mutableStateOf(false)
    var modeLabel by mutableStateOf("—")
    var targetLabel by mutableStateOf("—")
    var activeDb by mutableStateOf("")
    var serverName by mutableStateOf("")
    var serverVersion by mutableStateOf("")

    // ── داده
    var overview by mutableStateOf<MaOverview?>(null)
    var map by mutableStateOf(MaSectionMap())
    var section by mutableStateOf(MaSection.CUSTOMERS)
    var page by mutableStateOf<MaPage?>(null)
    var pageIndex by mutableStateOf(1)
    var search by mutableStateOf("")
    var tableQuery by mutableStateOf("")
    var manualRef by mutableStateOf("")
    var detail by mutableStateOf<Pair<String, List<Pair<String, String>>>?>(null)
    var diag by mutableStateOf<List<MaDiagStep>?>(null)
    var diagBusy by mutableStateOf(false)
    var blocks by mutableStateOf<List<MaBlock>>(emptyList())
    var reportBusy by mutableStateOf(false)
    var roles by mutableStateOf<Map<String, MaTableRoles>>(emptyMap())
}

private val STUDIO_TABS = listOf(
    MaNavItem("overview", "نمای کلی", Icons.Filled.Dashboard),
    MaNavItem("data", "مرور داده‌ها", Icons.Filled.TableChart),
    MaNavItem("map", "اتصال جداول", Icons.Filled.Link),
    MaNavItem("conn", "تنظیم اتصال", Icons.Filled.Settings),
    MaNavItem("report", "گزارش‌ها", Icons.Filled.Insights),
)

private const val READ_ONLY_NOTE =
    "اتصال فقط-خواندنی است — M•REPORT هیچ داده‌ای را در سرور تغییر نمی‌دهد. " +
        "نقش ستون‌ها (نام، مبلغ، تاریخ، کد) به‌صورت هوشمند تشخیص داده می‌شود."

// ═══════════════════════════ صفحهٔ اصلی ═══════════════════════════

/**
 * «گزارش مدیریت» — تمام‌صفحه، مشابه بخش مدیریت برنامهٔ مرجع.
 * @param onBack بازگشت به صفحهٔ قبل.
 */
@Composable
fun MaManagerScreen(onBack: () -> Unit) {
    val p = vizitorPalette
    val st = remember { MaStudioState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    // ── کارهای شبکه‌ای: توابع سطح‌فایل (suspend) — وضعیت از st خوانده/نوشته می‌شود

    // ── نخستین بار: خواندن تنظیمات + نگاشت ذخیره‌شده روی همین گوشی
    LaunchedEffect(Unit) {
        MaSectionStore.init(ctx)
        st.map = MaSectionStore.load()
        val saved = SecureDbStore.load()
        val (he, hl, useExt) = SecureDbStore.loadAddresses()
        if (saved != null) {
            st.hostExternal = if (he.isNotBlank()) he else saved.host
            st.hostLocal = hl
            st.useExternal = useExt
            st.port = saved.port.toString()
            st.database = saved.database
            st.user = saved.username
            st.pass = saved.password
            st.useTls = saved.useEncryption
        } else {
            val (l, n, useNet) = MaSectionStore.loadMode()
            st.hostLocal = l
            st.hostExternal = n
            st.useExternal = useNet
        }
        if (MaSqlEngine.isConnected) {
            st.connected = true
            st.modeLabel = MaSqlEngine.modeLabel
            st.targetLabel = MaSqlEngine.targetLabel
            studioOverview(st)
        }
    }

    val tableList = st.overview?.tables ?: emptyList()
    val shownTables = remember(tableList, st.tableQuery) {
        val q = st.tableQuery.trim().lowercase()
        if (q.isBlank()) tableList else tableList.filter {
            it.name.lowercase().contains(q) || it.schema.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .dashboardBackdrop()
    ) {
        MaTopBar(
            title = "گزارش مدیریت",
            eyebrow = "M•REPORT — اتاق فرمان مدیر",
            onBack = onBack,
            chip = if (st.connected) "متصل — ${st.modeLabel}" else "وصل نشده — از «اتصال جداول»",
            chipColor = if (st.connected) MaGreen else MaAmber,
        )
        MaNavStrip(
            items = STUDIO_TABS,
            selectedKey = st.tab,
            onSelect = { st.tab = it },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        st.msg?.let { m ->
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                MaStatStrip(
                    items = listOf(
                        Triple(
                            if (st.msgOk) "انجام شد" else "نکته",
                            m,
                            if (st.msgOk) MaGreen else MaAmber,
                        )
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ═══════════════ برگهٔ «تنظیم اتصال» ═══════════════
            if (st.tab == "conn") {
                item {
                    MaHeroTitle(
                        title = "تنظیم اتصال سرور",
                        subtitle = "دو نشانی مرجع (شبکهٔ داخلی / اینترنت) + کاوش چهارحالته " +
                            "Microsoft و jTDS — همان موتور اتصال M•REPORT",
                    )
                }
                item {
                    Column(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (st.useExternal) "نشانی اینترنت (بیرون از شبکهٔ اداره)" else "نشانی شبکهٔ داخلی",
                                color = p.gold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Switch(checked = st.useExternal, onCheckedChange = { st.useExternal = it })
                        }
                        Text(
                            "با روشن‌بودن کلید، از نشانی اینترنتی سرور استفاده می‌شود؛ " +
                                "خاموش باشد، نشانی داخلی شبکهٔ اداره.",
                            color = p.textSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }
                item {
                    MaField(
                        label = "نشانی سرور (اینترنت)",
                        value = st.hostExternal,
                        onChange = { st.hostExternal = it },
                        hint = "مثلاً 37.143.147.19 یا SERVER\\INSTANCE"
                    )
                }
                item {
                    MaField(
                        label = "نشانی سرور (شبکهٔ داخلی)",
                        value = st.hostLocal,
                        onChange = { st.hostLocal = it },
                        hint = "مثلاً 192.168.1.10",
                        keyboard = KeyboardType.Number
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            MaField("پورت", st.port, { st.port = it }, "1433", KeyboardType.Number)
                        }
                        Box(Modifier.weight(1.6f)) {
                            MaField("نام دیتابیس", st.database, { st.database = it }, "مثلاً Atiran2")
                        }
                    }
                }
                item {
                    MaField("نام کاربری", st.user, { st.user = it }, "کاربر SQL Server (نه ویندوز)")
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            MaField(
                                label = "رمز عبور",
                                value = st.pass,
                                onChange = { st.pass = it },
                                hint = "••••••••",
                                keyboard = KeyboardType.Password,
                                hidden = !st.showPass,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        MaOrbButton(
                            icon = if (st.showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            onClick = { st.showPass = !st.showPass },
                            size = 40.dp,
                            contentDescription = "نمایش رمز",
                        )
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "ذخیرهٔ رمز روی همین گوشی (رمزنگاری‌شده با کلید سخت‌افزاری)",
                            color = p.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = st.remember, onCheckedChange = { st.remember = it })
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "تلاش اول با رمزنگاری TLS (اگر سرور پشتیبانی نکند، خودکار حالت بدون TLS و jTDS)",
                            color = p.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = st.useTls, onCheckedChange = { st.useTls = it })
                    }
                }
                item {
                    MaGoldCta(
                        title = if (st.busy) "در حال اتصال…" else "اتصال به سرور",
                        subtitle = "امتحان چهار حالت: Microsoft+TLS، Microsoft، jTDS، jTDS+TLS",
                        icon = Icons.Filled.Cloud,
                        enabled = !st.busy,
                        onClick = { scope.launch { studioConnect(st) } },
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            MaMetricCard(
                                title = "آزمایش اتصال",
                                value = if (st.connected) "سالم" else "—",
                                icon = Icons.Filled.CheckCircle,
                                subtitle = st.activeDb.ifBlank { "دیتابیس فعال" },
                                tint = MaGreen,
                                onClick = { scope.launch { studioTest(st) } }
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            MaMetricCard(
                                title = "عیب‌یابی اتصال",
                                value = "گام‌به‌گام",
                                icon = Icons.Filled.BugReport,
                                subtitle = "TCP · TDS · TLS · ورود",
                                tint = MaAmber,
                                onClick = { scope.launch { studioDiagnose(st) } }
                            )
                        }
                    }
                }
                item {
                    MaDocCard(
                        title = "خلاصهٔ اتصال",
                        subtitle = if (st.connected) "متصل — آخرین دریافت: ${st.targetLabel}" else "وصل نشده",
                        rows = listOf(
                            "حالت برنده" to st.modeLabel,
                            "هدف اتصال" to st.targetLabel,
                            "دیتابیس فعال" to st.activeDb.ifBlank { "—" },
                            "نام سرور" to st.serverName.ifBlank { "—" },
                            "نسخهٔ سرور" to st.serverVersion.ifBlank { "—" },
                        ),
                        note = READ_ONLY_NOTE,
                    )
                }
                item { Lux3DNote(READ_ONLY_NOTE) }
            }

            // ═══════════════ برگهٔ «نمای کلی» ═══════════════
            if (st.tab == "overview") {
                item {
                    MaHeroTitle(
                        title = "نمای کلی دیتابیس",
                        subtitle = "جدول‌های واقعی سرور، شمار رکورد هر جدول و حجم دیتابیس — " +
                            "همه فقط-خواندنی",
                    )
                }
                item {
                    MaStatStrip(
                        items = listOf(
                            Triple("جداول", (st.overview?.tables?.size ?: 0).toFaNumber(), p.gold),
                            Triple("مجموع رکوردها", (st.overview?.totalRows ?: 0L).toFaNumber(), MaGreen),
                            Triple(
                                "حجم دیتابیس",
                                "%.1f مگابایت".format(st.overview?.sizeMb ?: 0.0).toFaDigits(),
                                MaAmber,
                            ),
                            Triple("بخش‌های متصل", "${st.map.mappedCount.toFaNumber()} از ۵", p.primary),
                        )
                    )
                }
                item {
                    MaGoldCta(
                        title = if (st.busy) "در حال دریافت…" else "دریافت فهرست جداول سرور",
                        subtitle = if (st.connected) "متصل به ${st.targetLabel}" else "اول باید اتصال برقرار شود",
                        icon = Icons.Filled.Refresh,
                        enabled = !st.busy,
                        onClick = { scope.launch { studioOverview(st) } },
                    )
                }
                item {
                    MaSegmentPills(
                        items = MaSection.values().map { MaNavItem(it.key, it.label, icon = sectionIcon(it)) },
                        selectedKey = st.section.key,
                        onSelect = { key ->
                            MaSection.values().firstOrNull { it.key == key }?.let { st.section = it }
                        },
                    )
                }
                item {
                    val ref = st.map.refOf(st.section)
                    MaDocCard(
                        title = "بخش فعال: ${st.section.label}",
                        subtitle = ref ?: "وصل نشده — از «اتصال جداول» انتخاب کنید",
                        rows = buildList {
                            add("جدول نگاشت‌شده" to (ref ?: "—"))
                            add("تعداد رکورد" to rowCountOf(tableList, ref).toFaNumber())
                            ref?.let { st.roles[it] }?.let { r ->
                                add("ستون نام" to (r.titleCol ?: "—"))
                                add("ستون مبلغ" to (r.amountCol ?: "—"))
                                add("ستون تاریخ" to (r.dateCol ?: "—"))
                                add("ستون کد" to (r.codeCol ?: "—"))
                            }
                            add("پیشنهاد جداول" to suggestTables(st.section, tableList).size.toFaNumber() + " جدول")
                        },
                        note = if (ref == null) "جدول این بخش را انتخاب کنید تا داده واقعی همین‌جا نمایش داده شود" else null,
                    )
                }
                item {
                    MaField(
                        label = "جست‌وجوی نام جدول",
                        value = st.tableQuery,
                        onChange = { st.tableQuery = it },
                        hint = "مثلاً sailfact یا مشتری",
                    )
                }
                if (st.overview == null) {
                    item { Lux3DNote("هنوز فهرست جداول دریافت نشده — دکمهٔ بالا را بزنید.") }
                } else if (shownTables.isEmpty()) {
                    item { Lux3DNote("جدولی با این نام یافت نشد") }
                } else {
                    item {
                        MaSectionHeader(
                            title = "جدول‌های سرور",
                            count = shownTables.size.toFaNumber(),
                            icon = Icons.Filled.Storage,
                        )
                    }
                    item {
                        Lux3DTable(
                            headers = listOf("جدول", "اسکیما", "تعداد رکورد", "پیشنهاد برای بخش"),
                            rows = shownTables.take(40).map { t ->
                                listOf(t.name, t.schema, t.rows.toFaNumber(), hintFor(t, tableList))
                            },
                            weights = listOf(1.5f, 0.7f, 1f, 1.4f),
                            emptyText = "جدولی یافت نشد",
                        )
                    }
                }
                item { Lux3DNote(READ_ONLY_NOTE) }
            }

            // ═══════════════ برگهٔ «مرور داده‌ها» ═══════════════
            if (st.tab == "data") {
                item {
                    MaHeroTitle(
                        title = "مرور داده‌های سرور",
                        subtitle = "صفحه‌بندی واقعی جدول + جست‌وجو در ستون‌های متنی + جزئیات کامل رکورد",
                    )
                }
                item {
                    MaSegmentPills(
                        items = MaSection.values().map { MaNavItem(it.key, it.label, icon = sectionIcon(it)) },
                        selectedKey = st.section.key,
                        onSelect = { key ->
                            MaSection.values().firstOrNull { it.key == key }?.let {
                                st.section = it
                                st.page = null
                                st.pageIndex = 1
                                st.search = ""
                                if (st.map.refOf(it) != null) scope.launch { studioPage(st, it, 1, "") }
                            }
                        },
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            MaField(
                                label = "جست‌وجو در ستون‌های متنی این جدول…",
                                value = st.search,
                                onChange = { st.search = it },
                                hint = "متن مورد نظر",
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        MaOrbButton(
                            icon = Icons.Filled.Search,
                            onClick = { scope.launch { studioPage(st, st.section, 1, st.search) } },
                            size = 44.dp,
                            contentDescription = "جست‌وجو",
                        )
                    }
                }
                val ref = st.map.refOf(st.section)
                item {
                    MaGoldCta(
                        title = when {
                            ref == null -> "این بخش جدول ندارد"
                            st.busy -> "در حال دریافت…"
                            else -> "دریافت صفحهٔ اول «${st.section.label}»"
                        },
                        subtitle = ref ?: "از «اتصال جداول» جدول این بخش را انتخاب کنید",
                        icon = Icons.Filled.TableChart,
                        enabled = !st.busy && ref != null,
                        onClick = { scope.launch { studioPage(st, st.section, 1, st.search) } },
                    )
                }
                val pg = st.page
                if (pg != null) {
                    item {
                        MaStatStrip(
                            items = listOf(
                                Triple("متصل به", ref ?: "—", p.gold),
                                Triple("مجموع رکوردها", pg.total.toFaNumber(), MaGreen),
                                Triple("صفحه", "${st.pageIndex.toFaNumber()} از ${pageCount(pg.total, 25).toFaNumber()}", MaAmber),
                                Triple("ردیف این صفحه", pg.rows.size.toFaNumber(), p.primary),
                            )
                        )
                    }
                    item {
                        Lux3DTable(
                            headers = pg.columns,
                            rows = pg.rows.map { r -> r.map { it.ifBlank { "—" } } },
                            weights = pg.columns.map { if (pg.columns.size > 6) 0.6f else 1f },
                            emptyText = if (st.search.isBlank()) "این جدول خالی است" else "نتیجه‌ای برای جست‌وجو یافت نشد",
                        )
                    }
                    if (pg.rows.isNotEmpty()) {
                        item {
                            MaSectionHeader(
                                title = "جزئیات رکورد",
                                count = "لمس برای پرونده کامل",
                                icon = Icons.Filled.TableChart,
                            )
                        }
                        item {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                pg.rows.take(12).forEachIndexed { i, r ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(p.surface.copy(alpha = 0.55f))
                                            .clickable {
                                                st.detail = (ref ?: "—") to pg.columns.mapIndexed { ci, c ->
                                                    c to (r.getOrNull(ci) ?: "").ifBlank { "—" }
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Filled.ChevronLeft,
                                            contentDescription = null,
                                            tint = p.gold,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "رکورد ${((st.pageIndex - 1) * 25 + i + 1).toFaNumber()}",
                                            color = p.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(0.6f),
                                        )
                                        Text(
                                            r.firstOrNull { it.isNotBlank() }?.take(60) ?: "—",
                                            color = p.textSecondary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1.6f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                MaMetricCard(
                                    title = "صفحهٔ قبلی",
                                    value = "قبلی",
                                    icon = Icons.Filled.ChevronRight,
                                    subtitle = if (st.pageIndex > 1) "صفحهٔ ${(st.pageIndex - 1).toFaNumber()}" else "صفحهٔ اول",
                                    onClick = {
                                        if (st.pageIndex > 1) scope.launch { studioPage(st, st.section, st.pageIndex - 1, st.search) }
                                    },
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                MaMetricCard(
                                    title = "صفحهٔ بعدی",
                                    value = "بعدی",
                                    icon = Icons.Filled.ChevronLeft,
                                    subtitle = "صفحهٔ ${(st.pageIndex + 1).toFaNumber()} از ${pageCount(pg.total, 25).toFaNumber()}",
                                    onClick = {
                                        if (st.pageIndex < pageCount(pg.total, 25)) {
                                            scope.launch { studioPage(st, st.section, st.pageIndex + 1, st.search) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                } else if (ref == null) {
                    item { Lux3DNote("جدول «${st.section.label}» انتخاب نشده — از «اتصال جداول» جدول آن را وصل کنید.") }
                }
            }

            // ═══════════════ برگهٔ «اتصال جداول» ═══════════════
            if (st.tab == "map") {
                item {
                    MaHeroTitle(
                        title = "اتصال جداول سرور",
                        subtitle = "هر بخش M•REPORT به کدام جدول سرور وصل شود؟ — تشخیص خودکار + انتخاب دستی",
                    )
                }
                item {
                    MaGoldCta(
                        title = if (st.busy) "در حال تشخیص…" else "تشخیص خودکار جداول",
                        subtitle = "جست‌وجو با نام‌های حدسی فارسی/انگلیسی در فهرست جدول‌های سرور",
                        icon = Icons.Filled.Search,
                        enabled = !st.busy && st.overview != null,
                        onClick = { scope.launch { studioAutoDetect(st) } },
                    )
                }
                item {
                    MaStatStrip(
                        items = listOf(
                            Triple("بخش‌های متصل", "${st.map.mappedCount.toFaNumber()} از ۵", if (st.map.mappedCount == 5) MaGreen else MaAmber),
                            Triple("جداول سرور", (st.overview?.tables?.size ?: 0).toFaNumber(), p.gold),
                            Triple("وضعیت", if (st.map.mappedCount == 5) "همه بخش‌ها به سرور وصل‌اند" else "نیازمند توجه", if (st.map.mappedCount == 5) MaGreen else MaAmber),
                        )
                    )
                }
                MaSection.values().forEach { section ->
                    item {
                        val ref = st.map.refOf(section)
                        val suggested = suggestTables(section, tableList)
                        MaDocCard(
                            title = section.label,
                            subtitle = ref ?: "وصل نشده — از «اتصال جداول» انتخاب کنید",
                            badge = "بخش ${section.key.uppercase()}",
                            rows = buildList {
                                if (suggested.isNotEmpty()) {
                                    add("نمونه‌های روی سرور" to suggested.take(4).joinToString(" · ") { it.ref })
                                }
                                ref?.let { st.roles[it] }?.let { r ->
                                    add("نقش ستون‌ها" to "نام: ${r.titleCol ?: "—"} · مبلغ: ${r.amountCol ?: "—"} · تاریخ: ${r.dateCol ?: "—"} · کد: ${r.codeCol ?: "—"}")
                                }
                                add("وضعیت" to (if (ref == null) "وصل نشده" else "متصل — $ref"))
                            },
                            note = "برای انتخاب جدول این بخش، یکی از نمونه‌های بالا را لمس کنید یا نام کامل را وارد کنید.",
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            suggested.take(3).forEach { t ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(p.surface.copy(alpha = 0.6f))
                                        .border(1.dp, p.gold.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .clickable {
                                            val m = st.map.with(section, t.ref)
                                            st.map = m
                                            MaSectionStore.save(m)
                                            st.msgOk = true
                                            st.msg = "«${section.label}» به ${t.ref} وصل شد — ${t.rows.toFaNumber()} رکورد"
                                            scope.launch {
                                                runCatching { MaMapping.rolesOf(t.ref) }.getOrNull()?.let { role ->
                                                    st.roles = st.roles + (t.ref to role)
                                                }
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                ) {
                                    Column {
                                        Text(t.name, color = p.gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            "متصل — ${t.rows.toFaNumber()} رکورد",
                                            color = p.textSecondary,
                                            fontSize = 10.sp,
                                        )
                                    }
                                }
                            }
                            if (suggested.isEmpty()) {
                                Box(Modifier.weight(1f)) {
                                    Text(
                                        "لمس برای انتخاب جدول...",
                                        color = p.textSecondary,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    MaField(
                        label = "نام کامل جدول (اسکیما.جدول)",
                        value = st.manualRef,
                        onChange = { st.manualRef = it },
                        hint = "مثلاً dbo.CUSTOMERS",
                    )
                }
                item {
                    MaGoldCta(
                        title = "وصل‌کردن جدول دستی به بخش «${st.section.label}»",
                        subtitle = "نام کامل جدول را در کادر بالا بنویسید (اسکیما.جدول)",
                        icon = Icons.Filled.Link,
                        enabled = !st.busy && MaMapping.split(st.manualRef) != null,
                        onClick = {
                            val (schema, table) = MaMapping.split(st.manualRef) ?: return@MaGoldCta
                            st.manualRef = "$schema.$table"
                            scope.launch {
                                st.busy = true
                                val cols = runCatching { MaSqlEngine.columnsOf(schema, table) }.getOrNull()
                                if (cols == null) {
                                    st.msgOk = false
                                    st.msg = "جدول «$schema.$table» پیدا نشد یا ستونی ندارد — نام را بررسی کنید."
                                } else {
                                    val role = MaTableHeuristics.roles(cols)
                                    val m = st.map.with(st.section, "$schema.$table")
                                    st.map = m
                                    st.roles = st.roles + ("$schema.$table" to role)
                                    MaSectionStore.save(m)
                                    st.msgOk = true
                                    st.msg = "«${st.section.label}» به $schema.$table وصل شد — ${cols.size.toFaNumber()} ستون"
                                }
                                st.busy = false
                            }
                        },
                    )
                }
                item {
                    MaStatStrip(
                        items = listOf(
                            Triple("بخش انتخاب‌شده", st.section.label, p.primary),
                            Triple("جدول فعلی", st.map.refOf(st.section) ?: "وصل نشده", p.gold),
                        )
                    )
                }
                item { Lux3DNote(READ_ONLY_NOTE) }
            }

            // ═══════════════ برگهٔ «گزارش‌ها» ═══════════════
            if (st.tab == "report") {
                item {
                    MaHeroTitle(
                        title = "گزارش‌های مدیریت",
                        subtitle = "مجموع ستون مبلغ، برترین‌ها بر اساس مبلغ و آخرین رکوردها — از دادهٔ واقعی سرور",
                    )
                }
                item {
                    MaGoldCta(
                        title = if (st.reportBusy) "در حال ساخت گزارش…" else "ساخت گزارش از سرور",
                        subtitle = if (st.map.mappedCount == 0) "اول جدول‌ها را در «اتصال جداول» وصل کنید"
                        else "${st.map.mappedCount.toFaNumber()} بخش متصل — آمادهٔ گزارش",
                        icon = Icons.Filled.Insights,
                        enabled = !st.reportBusy && st.map.mappedCount > 0,
                        onClick = { scope.launch { studioReport(st) } },
                    )
                }
                item {
                    MaStatStrip(
                        items = listOf(
                            Triple("بخش‌های متصل", "${st.map.mappedCount.toFaNumber()} از ۵", p.gold),
                            Triple(
                                "مجموع رکوردها",
                                (st.overview?.totalRows ?: 0L).toFaNumber(),
                                MaGreen,
                            ),
                            Triple("حجم دیتابیس", "%.1f مگابایت".format(st.overview?.sizeMb ?: 0.0).toFaDigits(), MaAmber),
                            Triple("گزارش‌های ساخته‌شده", st.blocks.size.toFaNumber(), p.primary),
                        )
                    )
                }
                if (st.blocks.isEmpty()) {
                    item {
                        Lux3DNote(
                            if (st.map.mappedCount == 0)
                                "جدول هر بخش را انتخاب کنید تا داده واقعی نمایش داده شود"
                            else "دکمهٔ «ساخت گزارش از سرور» را بزنید تا گزارش‌ها از دادهٔ واقعی ساخته شود."
                        )
                    }
                }
                st.blocks.forEach { b ->
                    item {
                        MaSectionHeader(
                            title = b.title,
                            count = b.subtitle,
                            icon = Icons.Filled.Insights,
                        )
                    }
                    if (b.chart.isNotEmpty()) {
                        item {
                            Lux3DBarChart(
                                rows = b.chart,
                                maxBars = 7,
                                valueLabel = { it.toFaPrice() },
                            )
                        }
                        item { Lux3DNote(b.chartCaption) }
                    }
                    item {
                        MaDocCard(
                            title = b.title,
                            subtitle = b.subtitle,
                            rows = b.rows.ifEmpty { listOf("وضعیت" to b.emptyText) },
                        )
                    }
                }
                if (st.blocks.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) {
                                MaMetricCard(
                                    title = "اشتراک‌گذاری گزارش",
                                    value = "ارسال",
                                    icon = Icons.Filled.Share,
                                    subtitle = "متن کامل گزارش",
                                    tint = MaGreen,
                                    onClick = {
                                        val text = buildReportText(st)
                                        val i = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "گزارش مدیریت — آتیران ویزیتور")
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        runCatching {
                                            ctx.startActivity(Intent.createChooser(i, "اشتراک‌گذاری گزارش"))
                                        }
                                    },
                                )
                            }
                            Box(Modifier.weight(1f)) {
                                MaMetricCard(
                                    title = "کپی گزارش",
                                    value = "کپی",
                                    icon = Icons.Filled.ContentCopy,
                                    subtitle = "در حافظهٔ گوشی",
                                    tint = p.primary,
                                    onClick = {
                                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                            as android.content.ClipboardManager
                                        cm.setPrimaryClip(
                                            android.content.ClipData.newPlainText("گزارش مدیریت", buildReportText(st))
                                        )
                                        st.msgOk = true
                                        st.msg = "گزارش در حافظهٔ گوشی کپی شد"
                                    },
                                )
                            }
                        }
                    }
                }
                item {
                    MaDocCard(
                        title = "پروندهٔ سرور",
                        subtitle = if (st.connected) "متصل — آخرین دریافت: ${st.targetLabel}" else "وصل نشده",
                        rows = listOf(
                            "حالت اتصال" to st.modeLabel,
                            "هدف اتصال" to st.targetLabel,
                            "دیتابیس فعال" to st.activeDb.ifBlank { "—" },
                            "نسخهٔ سرور" to st.serverVersion.ifBlank { "—" },
                            "تعداد جدول‌ها" to (st.overview?.tables?.size ?: 0).toFaNumber(),
                        ),
                        note = READ_ONLY_NOTE,
                    )
                }
                item {
                    MaThinBar(
                        caption = "پوشش بخش‌های گزارش",
                        fraction = st.map.mappedCount / 5f,
                        valueText = "${st.map.mappedCount.toFaNumber()} از ۵",
                    )
                }
            }

            item {
                Text(
                    "Meelano Studio Design • Milad Yaghoobi — گزارش مدیریت آتیران ویزیتور",
                    color = p.gold,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    // ── برگهٔ جزئیات رکورد (مشابه «جزئیات رکورد —» مرجع)
    st.detail?.let { (ref, cols) ->
        MaSmartSheet(
            title = "جزئیات رکورد",
            subtitle = "پرونده رکورد از $ref — فقط خواندن",
            groups = listOf(
                MaSmartGroup(
                    key = "cols",
                    title = "همهٔ ستون‌ها",
                    subtitle = "${cols.size.toFaNumber()} ستون",
                    icon = Icons.Filled.TableChart,
                    tiles = cols.mapIndexed { i, (k, v) ->
                        MaSmartTile(
                            key = "c$i",
                            label = k,
                            subtitle = v.take(120),
                            icon = Icons.Filled.ChevronLeft,
                            tint = p.gold,
                        )
                    },
                )
            ),
            onPick = { st.detail = null },
            onClose = { st.detail = null },
        )
    }

    // ── برگهٔ عیب‌یابی اتصال (گام‌به‌گام، مثل نسخهٔ مرجع)
    st.diag?.let { steps ->
        MaSmartSheet(
            title = "عیب‌یابی اتصال",
            subtitle = if (st.diagBusy) "در حال بررسی…" else "پیدا کردن سرور → درگاه TCP → رمزنگاری/TLS → ورود → نتیجه",
            groups = listOf(
                MaSmartGroup(
                    key = "diag",
                    title = "گام‌های بررسی",
                    subtitle = "${steps.count { it.ok }.toFaNumber()} از ${steps.size.toFaNumber()} گام موفق",
                    icon = Icons.Filled.BugReport,
                    tiles = steps.mapIndexed { i, s ->
                        MaSmartTile(
                            key = "d$i",
                            label = s.title,
                            subtitle = (s.detail + if (s.hint.isNotBlank()) "\nراهنما: ${s.hint}" else "").take(400),
                            icon = if (s.ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            tint = if (s.ok) MaGreen else MaRed,
                        )
                    },
                )
            ),
            onPick = { key ->
                val i = key.removePrefix("d").toIntOrNull()
                val port = i?.let { steps.getOrNull(it)?.suggestedPort }
                if (port != null) {
                    st.port = port.toString()
                    st.msgOk = true
                    st.msg = "پورت $port تنظیم شد — دوباره «اتصال به سرور» را بزنید."
                    st.diag = null
                }
            },
            onClose = { st.diag = null },
        )
    }
}


// ═══════════════════════════ کارهای شبکه‌ای (suspend) ═══════════════════════════

/** دریافت فهرست جدول‌ها + حجم دیتابیس و به‌روزرسانی وضعیت صفحه. */
private suspend fun studioOverview(st: MaStudioState) {
    st.busy = true
    runCatching { MaSqlEngine.overview() }
        .onSuccess { ov ->
            st.overview = ov
            st.msgOk = true
            st.msg = "فهرست جداول دریافت شد — ${ov.tables.size.toFaNumber()} جدول، " +
                "مجموع ${ov.totalRows.toFaNumber()} رکورد"
        }
        .onFailure { st.msgOk = false; st.msg = MaSqlEngine.friendly(it) }
    st.busy = false
}

/** اتصال چهارحالته به سرور + خواندن مشخصات سرور و سپس فهرست جدول‌ها. */
private suspend fun studioConnect(st: MaStudioState) {
    val cfg = buildCfg(st)
    if (cfg == null) {
        st.msgOk = false
        st.msg = "آدرس سرور، نام دیتابیس و نام کاربری را کامل کنید."
        return
    }
    st.busy = true
    val r = MaSqlEngine.connect(cfg)
    st.connected = r.ok
    if (r.ok) {
        st.modeLabel = r.modeLabel
        st.targetLabel = MaSqlEngine.targetLabel
        SecureDbStore.save(if (st.remember) cfg else cfg.copy(password = ""))
        SecureDbStore.saveAddresses(st.hostExternal, st.hostLocal, st.useExternal)
        MaSectionStore.saveMode(st.hostLocal, st.hostExternal, st.useExternal)
        val t = runCatching { MaSqlEngine.test(cfg) }.getOrNull()
        st.activeDb = t?.first ?: ""
        st.serverName = t?.second ?: ""
        st.serverVersion = (t?.third ?: "").lineSequence().firstOrNull()?.trim() ?: ""
        st.msgOk = true
        st.msg = "اتصال برقرار شد — ${r.modeLabel} • ${MaSqlEngine.targetLabel}"
        st.busy = false
        studioOverview(st)
    } else {
        st.msgOk = false
        st.msg = r.message
        st.busy = false
    }
}

/** آزمایش سبک اتصال (نام دیتابیس فعال و نام/نسخهٔ سرور). */
private suspend fun studioTest(st: MaStudioState) {
    val cfg = buildCfg(st) ?: run {
        st.msgOk = false
        st.msg = "آدرس سرور، نام دیتابیس و نام کاربری را کامل کنید."
        return
    }
    st.busy = true
    runCatching { MaSqlEngine.test(cfg) }
        .onSuccess { t ->
            st.activeDb = t.first
            st.serverName = t.second
            st.serverVersion = t.third.lineSequence().firstOrNull()?.trim() ?: ""
            st.connected = true
            st.modeLabel = MaSqlEngine.modeLabel
            st.targetLabel = MaSqlEngine.targetLabel
            st.msgOk = true
            st.msg = "دیتابیس فعال: ${t.first} • سرور: ${t.second}"
        }
        .onFailure { st.msgOk = false; st.msg = MaSqlEngine.friendly(it) }
    st.busy = false
}

/** عیب‌یابی گام‌به‌گام اتصال. */
private suspend fun studioDiagnose(st: MaStudioState) {
    val cfg = buildCfg(st) ?: run {
        st.msgOk = false
        st.msg = "آدرس سرور، نام دیتابیس و نام کاربری را کامل کنید."
        return
    }
    st.diagBusy = true
    st.msg = null
    st.diag = runCatching { MaSqlEngine.diagnose(cfg) }
        .getOrElse { listOf(MaDiagStep(false, "عیب‌یابی ناموفق", MaSqlEngine.friendly(it))) }
    st.diagBusy = false
}

/** دریافت یک صفحه از جدول بخش انتخاب‌شده (با جست‌وجوی اختیاری). */
private suspend fun studioPage(st: MaStudioState, section: MaSection, index: Int, searchText: String) {
    val ref = st.map.refOf(section) ?: run { st.page = null; return }
    val (schema, table) = MaMapping.split(ref) ?: run { st.page = null; return }
    st.busy = true
    runCatching { MaSqlEngine.page(schema, table, index, 25, searchText) }
        .onSuccess { st.page = it; st.pageIndex = index }
        .onFailure { st.msgOk = false; st.msg = MaSqlEngine.friendly(it) }
    st.busy = false
}

/** تشخیص خودکار جداول همهٔ بخش‌ها + ذخیرهٔ نگاشت روی همین گوشی. */
private suspend fun studioAutoDetect(st: MaStudioState) {
    st.busy = true
    val found = runCatching { MaMapping.autoDetect() }.getOrElse { MaSectionMap() }
    var m = st.map
    MaSection.values().forEach { s ->
        val r = found.refOf(s)
        if (!r.isNullOrBlank()) m = m.with(s, r)
    }
    st.map = m
    MaSectionStore.save(m)
    MaSection.values().forEach { s ->
        val ref = m.refOf(s) ?: return@forEach
        runCatching { MaMapping.rolesOf(ref) }.getOrNull()?.let { role ->
            st.roles = st.roles + (ref to role)
        }
    }
    st.busy = false
    st.msgOk = m.mappedCount > 0
    st.msg = if (m.mappedCount > 0)
        "تشخیص خودکار انجام شد — ${m.mappedCount.toFaNumber()} بخش از ۵ وصل شد"
    else "جدولی با نام‌های حدسی پیدا نشد — نام جدول را دستی وارد کنید."
}

/** ساخت گزارش‌های مدیریت از دادهٔ واقعی: شمارش، مجموع مبلغ، برترین‌ها و آخرین رکوردها. */
private suspend fun studioReport(st: MaStudioState) {
    st.reportBusy = true
    val out = mutableListOf<MaBlock>()
    for (s in MaSection.values()) {
        val ref = st.map.refOf(s) ?: continue
        val (schema, table) = MaMapping.split(ref) ?: continue
        val cols = runCatching { MaSqlEngine.columnsOf(schema, table) }.getOrNull() ?: continue
        val role = MaTableHeuristics.roles(cols)
        st.roles = st.roles + (ref to role)
        val rows = mutableListOf<Pair<String, String>>()
        var chart = listOf<Pair<String, Long>>()
        var chartCaption = ""
        runCatching { MaSqlEngine.count(schema, table) }.getOrNull()?.let {
            rows += "تعداد رکورد" to it.toFaNumber()
        }
        val amountCol = role.amountCol
        if (amountCol != null) {
            runCatching { MaSqlEngine.sumOf(schema, table, amountCol) }.getOrNull()?.let { sum ->
                rows += "مجموع «$amountCol»" to sum.toLong().toFaPrice()
            }
            val titleCol = role.titleCol
            if (titleCol != null) {
                runCatching { MaSqlEngine.topBy(schema, table, amountCol, 7) }.getOrNull()?.let { tp ->
                    val ti = tp.columns.indexOfFirst { it.equals(titleCol, true) }
                    val ai = tp.columns.indexOfFirst { it.equals(amountCol, true) }
                    if (ti >= 0 && ai >= 0) {
                        val pairs = tp.rows.mapNotNull { r ->
                            val amount = r.getOrNull(ai)?.toAmountOrNull() ?: return@mapNotNull null
                            val title = r.getOrNull(ti)?.ifBlank { "—" } ?: "—"
                            title to amount
                        }
                        pairs.forEach { rows += it.first to it.second.toFaPrice() }
                        chart = pairs
                        chartCaption = "برترین‌ها — بر اساس ستون مبلغ «$amountCol»"
                    }
                }
            }
        }
        val latestCol = role.dateCol ?: role.codeCol
        if (latestCol != null) {
            runCatching { MaSqlEngine.latestBy(schema, table, latestCol, 5) }.getOrNull()?.let { lp ->
                rows += "—" to "آخرین رکوردها بر اساس «$latestCol»"
                lp.rows.take(5).forEach { r ->
                    val t = lp.columns.indexOfFirst { it.equals(latestCol, true) }
                    val key = if (t >= 0) r.getOrNull(t) ?: "—" else "—"
                    val first = r.getOrNull(0) ?: ""
                    rows += key to first.ifBlank { "—" }
                }
            }
        }
        out += MaBlock(
            title = "گزارش ${s.label}",
            subtitle = "$ref — ${cols.size.toFaNumber()} ستون",
            rows = rows,
            chart = chart,
            chartCaption = chartCaption,
        )
    }
    st.blocks = out
    st.reportBusy = false
    st.msgOk = out.isNotEmpty()
    st.msg = if (out.isEmpty())
        "هنوز جدولی به بخش‌های گزارش وصل نشده — از «اتصال جداول» شروع کنید."
    else
        "گزارش از ${out.size.toFaNumber()} بخش ساخته شد"
}

// ═══════════════════════════ کمکی‌ها ═══════════════════════════

/** ساخت تنظیمات اتصال از فیلدهای صفحه (نشانی فعال، پورت، دیتابیس، کاربر، رمز). */
private fun buildCfg(st: MaStudioState): DbSettings? {
    val host = if (st.useExternal) st.hostExternal.trim() else st.hostLocal.trim().ifBlank { st.hostExternal.trim() }
    if (host.isBlank() || st.database.isBlank() || st.user.isBlank()) return null
    val port = st.port.trim().toIntOrNull() ?: 1433
    return DbSettings(
        host = host,
        port = port,
        database = st.database.trim(),
        username = st.user.trim(),
        password = st.pass,
        useEncryption = st.useTls,
        trustServerCert = true,
        connectTimeoutSec = 12,
        queryTimeoutSec = 30,
    )
}

/** آیکن هر بخش گزارش. */
private fun sectionIcon(s: MaSection) = when (s) {
    MaSection.CUSTOMERS -> Icons.Filled.Person
    MaSection.PRODUCTS -> Icons.Filled.Storefront
    MaSection.INVOICES -> Icons.Filled.Receipt
    MaSection.CHECKS -> Icons.Filled.Wallet
    MaSection.BANKS -> Icons.Filled.AccountBalanceWallet
}

/** تعداد صفحه‌ها برای اندازهٔ صفحهٔ ۲۵ ردیف. */
private fun pageCount(total: Long, size: Int): Int =
    if (total <= 0) 1 else ((total + size - 1) / size).toInt().coerceAtLeast(1)

/** تعداد رکورد یک جدول از فهرست دریافت‌شده. */
private fun rowCountOf(tables: List<MaTable>, ref: String?): Long {
    if (ref == null) return 0L
    val (schema, table) = MaMapping.split(ref) ?: return 0L
    return tables.firstOrNull { it.schema.equals(schema, true) && it.name.equals(table, true) }?.rows ?: 0L
}

/** جدول‌های حدسی یک بخش (بر اساس نام‌های فارسی/انگلیسی مرجع). */
private fun suggestTables(section: MaSection, tables: List<MaTable>): List<MaTable> {
    if (tables.isEmpty()) return emptyList()
    val keys = MaTableHeuristics.candidates(section).map { it.lowercase() }
    return tables.filter { t ->
        val n = t.name.lowercase()
        keys.any { k -> n.contains(k) || k.contains(n) }
    }.sortedByDescending { it.rows }.take(4)
}

/** برچسب «پیشنهاد برای بخش» برای هر جدول در فهرست. */
private fun hintFor(t: MaTable, all: List<MaTable>): String {
    val hits = MaSection.values().filter { s ->
        suggestTables(s, all).any { it.schema == t.schema && it.name == t.name }
    }
    return if (hits.isEmpty()) "—" else hits.joinToString("، ") { it.label }
}

/** تبدیل متن ستون سرور به عدد (ارقام فارسی/لاتین، جداکننده‌ها). */
private fun String.toAmountOrNull(): Long? {
    val trimmed = trim()
    val negative = trimmed.startsWith("-") || trimmed.contains('(') && trimmed.contains(')')
    val digits = StringBuilder()
    for (c in trimmed) when {
        c in '0'..'9' -> digits.append(c)
        c in '۰'..'۹' -> digits.append('0' + (c - '۰'))
        c == '.' || c == ',' || c == '٫' || c == '٬' || c == ' ' -> {}
        else -> {}
    }
    val v = digits.toString().toLongOrNull() ?: return null
    return if (negative) -v else v
}

/** متن کامل گزارش برای اشتراک‌گذاری/کپی. */
private fun buildReportText(st: MaStudioState): String = buildString {
    appendLine("📊 گزارش مدیریت — آتیران ویزیتور (M•REPORT)")
    appendLine("سرور: ${st.targetLabel} • حالت اتصال: ${st.modeLabel}")
    appendLine("دیتابیس فعال: ${st.activeDb.ifBlank { "—" }}")
    st.overview?.let { ov ->
        appendLine("جداول: ${ov.tables.size} • حجم دیتابیس: ${"%.1f".format(ov.sizeMb)} مگابایت")
    }
    appendLine("بخش‌های متصل: ${st.map.mappedCount} از ۵")
    appendLine("──────────────────────────────")
    st.blocks.forEach { b ->
        appendLine("■ ${b.title} (${b.subtitle})")
        b.rows.take(12).forEach { (k, v) -> appendLine("  • $k: $v") }
        appendLine("")
    }
    appendLine("Meelano Studio Design • Milad Yaghoobi")
}

/** کادر ورودی هم‌سبک با دیگر صفحه‌های تازه (طلایی روی شیشهٔ تیره). */
@Composable
private fun MaField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    hidden: Boolean = false,
) {
    val p = vizitorPalette
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = if (hint.isNotBlank()) {
            { Text(hint, fontSize = 11.sp, color = p.textSecondary.copy(alpha = 0.7f)) }
        } else null,
        singleLine = true,
        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
