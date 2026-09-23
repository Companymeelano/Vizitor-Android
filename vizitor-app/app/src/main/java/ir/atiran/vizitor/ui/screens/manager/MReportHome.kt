/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  گزارشات مدیر — «M•A Report» | خانهٔ نسخهٔ انحصاری (v2.22.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل، خانهٔ برنامهٔ انحصاری را **کاملاً مشابه** برنامهٔ مرجع
 *  (`app-debug-40.apk`) می‌سازد:
 *
 *    ▸ نوار بالا: «M•A Report» + دکمهٔ بازگشت + سه دکمهٔ گرد
 *      (تنظیمات هوشمند، هشدارها، جست‌وجو) — همان چیدمان مرجع.
 *    ▸ نوار بخش‌ها: نمای کلی · مشتریان · کالاها · خزانه · گزارش‌ها · اعلان‌ها.
 *    ▸ «نبض کسب‌وکار»، «M•D Intelligence»، چهار کارت شاخص، «چارت‌های من»،
 *      «روند مطالبات و وصول طلب» و «Top Products Report».
 *    ▸ «تنظیمات هوشمند» به‌صورت برگهٔ بازشو با همان گروه‌ها و کاشی‌های مرجع.
 *    ▸ پانویس مرجع: «★ داده‌های این بخش صرفاً برای نمایش قابلیت‌های
 *      M•A Report است» + «MEELANO STUDIO DESIGN» + «برنامه‌نویس • میلاد یعقوبی».
 *
 *  ── تنها بخش‌هایی که واقعاً «راه افتاده‌اند» (خواستهٔ کاربر) ────────────────
 *    • اتصال به دیتابیس: از راه «تنظیم اتصال» (همان موتور چهارحالته).
 *    • فراخوانی اطلاعات: شمار رکوردها، مجموع مبلغ‌ها، برترین‌ها، آخرین
 *      رکوردها و روند ماهانه — همه با کوئری فقط-خواندنی از جدول‌های نگاشت‌شده.
 *
 *  هرجا دادهٔ واقعی در دسترس نباشد، متنِ دقیقِ خودِ مرجع نمایش داده می‌شود
 *  (مثل «دادهٔ فاکتور در دسترس نیست» یا «خوانده نشد») — نه عدد ساختگی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.sqldirect.MaMapping
import ir.atiran.vizitor.sqldirect.MaNetKind
import ir.atiran.vizitor.sqldirect.MaServerProfile
import ir.atiran.vizitor.sqldirect.MeelanoDataSource
import ir.atiran.vizitor.sqldirect.SqlConnectionManager
import ir.atiran.vizitor.sqldirect.MaPage
import ir.atiran.vizitor.sqldirect.MaSectionMap
import ir.atiran.vizitor.sqldirect.MaSectionStore
import ir.atiran.vizitor.sqldirect.MaSqlEngine
import ir.atiran.vizitor.sqldirect.MaTableRoles
import ir.atiran.vizitor.ui.components.LineTrendChart
import ir.atiran.vizitor.ui.components.Lux3DBarChart
import ir.atiran.vizitor.ui.components.Lux3DDonut
import ir.atiran.vizitor.ui.components.Lux3DNote
import ir.atiran.vizitor.ui.components.Lux3DTable
import ir.atiran.vizitor.ui.components.MaAmber
import ir.atiran.vizitor.ui.components.MaDocCard
import ir.atiran.vizitor.ui.components.MaEmptyState
import ir.atiran.vizitor.ui.components.MaGoldCta
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MaHeroTitle
import ir.atiran.vizitor.ui.components.MaMetricCard
import ir.atiran.vizitor.ui.components.MaNavItem
import ir.atiran.vizitor.ui.components.MaNavStrip
import ir.atiran.vizitor.ui.components.MaOrbButton
import ir.atiran.vizitor.ui.components.MaPulseCard
import ir.atiran.vizitor.ui.components.MaPulseRow
import ir.atiran.vizitor.ui.components.MaRed
import ir.atiran.vizitor.ui.components.MaSectionHeader
import ir.atiran.vizitor.ui.components.MaSegmentPills
import ir.atiran.vizitor.ui.components.MaSmartGroup
import ir.atiran.vizitor.ui.components.MaSmartSheet
import ir.atiran.vizitor.ui.components.MaSmartTile
import ir.atiran.vizitor.ui.components.MaStatStrip
import ir.atiran.vizitor.ui.components.MaThinBar
import ir.atiran.vizitor.ui.components.MaToolGrid
import ir.atiran.vizitor.ui.components.MaToolTile
import ir.atiran.vizitor.ui.components.MaTopBar
import ir.atiran.vizitor.ui.components.MetalBarChart
import ir.atiran.vizitor.ui.components.TrendSeries
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.components.metalPanel
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.parseAmount
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

// ═══════════════════════════ مدل وضعیت خانهٔ گزارش ═══════════════════════════

/** وضعیت زندهٔ خانهٔ «M•A Report»: نگاشت، اتصال و داده‌های خوانده‌شده از سرور. */
private class MaHomeState {
    var tab by mutableStateOf("overview")
    var busy by mutableStateOf(false)
    var msg by mutableStateOf<String?>(null)
    var msgOk by mutableStateOf(true)
    var sheet by mutableStateOf(false)
    var tick by mutableStateOf(0)

    // ── اتصال هوشمند (مشخصات سرور هرگز نمایش داده نمی‌شود)
    var connected by mutableStateOf(false)
    var netLabel by mutableStateOf("در حال بررسی شبکه…")
    var channelLabel by mutableStateOf("—")
    var modeLabel by mutableStateOf("—")
    var targetLabel by mutableStateOf("سرور آتیران")

    // ── ورود کاربر (همان حساب برنامهٔ آتیران)
    var erpUser by mutableStateOf("")
    var erpPass by mutableStateOf("")
    var rememberErp by mutableStateOf(true)
    var showErpPass by mutableStateOf(false)
    var loginBusy by mutableStateOf(false)
    var welcome by mutableStateOf("")

    // ── نگاشت بخش‌ها
    var map by mutableStateOf(MaSectionMap())

    // ── شاخص‌های واقعی (null = خوانده نشد)
    var custCount by mutableStateOf<Long?>(null)
    var prodCount by mutableStateOf<Long?>(null)
    var checkCount by mutableStateOf<Long?>(null)
    var bankCount by mutableStateOf<Long?>(null)
    var tableCount by mutableStateOf<Long?>(null)
    var salesSum by mutableStateOf<Double?>(null)
    var checksSum by mutableStateOf<Double?>(null)
    var banksSum by mutableStateOf<Double?>(null)

    // ── ردیف‌ها و برترین‌ها
    var custRows by mutableStateOf<MaPage?>(null)
    var prodRows by mutableStateOf<MaPage?>(null)
    var checkRows by mutableStateOf<MaPage?>(null)
    var bankRows by mutableStateOf<MaPage?>(null)
    var custRoles by mutableStateOf<MaTableRoles?>(null)
    var prodRoles by mutableStateOf<MaTableRoles?>(null)
    var salesRoles by mutableStateOf<MaTableRoles?>(null)
    var topCustomers by mutableStateOf<List<Pair<String, Double>>>(emptyList())
    var topProducts by mutableStateOf<List<Pair<String, Double>>>(emptyList())

    // ── روند ماهانه (برچسب‌ها + دو سری)
    var trendLabels by mutableStateOf<List<String>>(emptyList())
    var trendSales by mutableStateOf<List<Long>>(emptyList())
    var trendCollect by mutableStateOf<List<Long>>(emptyList())

    // ── فیلترها
    var custQuery by mutableStateOf("")
    var prodQuery by mutableStateOf("")
    var treasuryTab by mutableStateOf("recv")
    var treasuryQuery by mutableStateOf("")
    var alertTab by mutableStateOf("all")
}

/** شش بخش نوار بالای خانه — همان ترتیب و نام‌های مرجع. */
private val HOME_TABS = listOf(
    MaNavItem("overview", "نمای کلی", Icons.Filled.Dashboard),
    MaNavItem("customers", "مشتریان", Icons.Filled.Person),
    MaNavItem("products", "کالاها", Icons.Filled.Storefront),
    MaNavItem("treasury", "خزانه", Icons.Filled.AccountBalanceWallet),
    MaNavItem("reports", "گزارش‌ها", Icons.Filled.Description),
    MaNavItem("alerts", "اعلان‌ها", Icons.Filled.Notifications),
)

/** متن‌های ثابت مرجع (برای جاهایی که دادهٔ واقعی در دسترس نیست). */
private const val DEMO_NOTE =
    "★ داده‌های این بخش صرفاً برای نمایش قابلیت‌های M•A Report است"
private const val NO_INVOICE_DATA = "دادهٔ فاکتور در دسترس نیست"
private const val NO_AMOUNT_COL = "ستون مبلغ این جدول تشخیص داده نشد"
private const val TREASURY_DEMO_NOTE =
    "★ داده نمایشی — با اتصال سرور چک‌های واقعی افزوده می‌شود"

// ═══════════════════════════ خانهٔ «M•A Report» ═══════════════════════════

/**
 * خانهٔ نسخهٔ انحصاری «گزارشات مدیر» — مشابه برنامهٔ مرجع `app-debug-40`.
 *
 * @param onExit بازگشت (در نسخهٔ انحصاری: بستن برنامه).
 * @param onOpen باز کردن صفحهٔ کاری موتور گزارش: `conn` (تنظیم اتصال)،
 *   `map` (اتصال جداول)، `data` (مرور داده‌ها)، `overview` (فهرست جدول‌ها).
 * @param refreshSignal با هر بار بازگشت از صفحه‌های کاری یک واحد زیاد می‌شود
 *   تا داده‌های تازه از سرور خوانده شوند.
 */
@Composable
fun MReportHome(
    onExit: () -> Unit,
    onOpen: (String) -> Unit,
    refreshSignal: Int = 0,
) {
    val p = vizitorPalette
    val ctx = LocalContext.current
    val st = remember { MaHomeState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(refreshSignal, st.tick) { maHomeLoad(st, ctx) }

    val alerts = maAlertRows(st)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .dashboardBackdrop()
    ) {
        MaTopBar(
            title = "M•A Report",
            eyebrow = "Intelligent Reporting Experience",
            onBack = onExit,
            onFilter = { st.sheet = true },
            onAlert = { st.tab = "alerts" },
            alertCount = alerts.size,
            onSearch = { onOpen("data") },
            chip = if (st.connected) "متصل ✓" else "در حال اتصال…",
            chipColor = if (st.connected) MaGreen else MaAmber,
        )
        MaNavStrip(
            items = HOME_TABS,
            selectedKey = st.tab,
            onSelect = { st.tab = it },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
        if (st.busy || st.msg != null) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                MaStatStrip(
                    items = listOf(
                        Triple(
                            if (st.busy) "در حال دریافت اطلاعات" else if (st.msgOk) "انجام شد" else "نکته",
                            st.msg ?: st.targetLabel,
                            if (st.busy) MaAmber else if (st.msgOk) MaGreen else MaAmber,
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
            when (st.tab) {
                "customers" -> homeCustomers(st, onOpen)
                "products" -> homeProducts(st, onOpen)
                "treasury" -> homeTreasury(st, onOpen)
                "reports" -> homeReports(st, ctx)
                "alerts" -> homeAlerts(st, alerts, onOpen)
                else -> homeOverview(
                    st = st,
                    onOpen = onOpen,
                    onConnect = { scope.launch { maConnectNow(st, ctx) } },
                    onLogin = { scope.launch { maLogin(st, ctx) } },
                )
            }
            item { MaHomeFooter() }
        }
    }

    if (st.sheet) {
        MaSmartSheet(
            title = "تنظیمات هوشمند",
            subtitle = "دستهٔ اصلی با زیرمجموعه هوشمند — تم هماهنگ",
            groups = maSmartGroups(st),
            onPick = { key ->
                when (key) {
                    "conn", "map", "data", "overview" -> {
                        st.sheet = false
                        onOpen(key)
                    }
                    "reload" -> {
                        st.tick += 1
                        st.msgOk = true
                        st.msg = "اطلاعات دوباره از سرور خوانده می‌شود…"
                    }
                    "share" -> {
                        maShare(ctx, maReportText(st))
                        st.sheet = false
                    }
                    "copy" -> {
                        maCopy(ctx, maReportText(st))
                        st.msgOk = true
                        st.msg = "متن گزارش در حافظهٔ گوشی کپی شد"
                        st.sheet = false
                    }
                    "customers" -> st.tab = "customers"
                    "reports" -> st.tab = "reports"
                    "alerts" -> st.tab = "alerts"
                    else -> {
                        st.msgOk = false
                        st.msg = "این کاشی در نسخهٔ فعلی فقط نمایشی است — " +
                            "اتصال به دیتابیس و فراخوانی اطلاعات فعال است."
                    }
                }
            },
            onClose = { st.sheet = false },
        )
    }
}

// ═══════════════════════════ برگهٔ «نمای کلی» ═══════════════════════════

private fun LazyListScope.homeOverview(
    st: MaHomeState,
    onOpen: (String) -> Unit,
    onConnect: () -> Unit,
    onLogin: () -> Unit,
) {
    item {
        MaHeroTitle(
            title = "M•A Report",
            subtitle = "Intelligent Reporting Experience",
        )
    }
    item {
        val p = vizitorPalette
        if (!st.connected) {
            MaGoldCta(
                title = if (st.busy) "در حال اتصال…" else "اتصال خودکار به سرور",
                subtitle = "سرور گزارش‌ها خودکار پیدا می‌شود — چیزی برای وارد کردن نیست",
                icon = Icons.Filled.Cloud,
                onClick = onConnect,
                badge = "خودکار",
                enabled = !st.busy,
            )
        } else {
            MaStatStrip(
                items = listOf(
                    Triple("وصل", st.targetLabel, MaGreen),
                    Triple("مسیر", st.channelLabel, p.gold),
                    Triple("جدول‌ها", (st.tableCount ?: 0L).toFaNumber(), p.gold),
                )
            )
        }
    }
    // ── ورود کاربر آتیران: تنها چیزی که کاربر وارد می‌کند (نام کاربری و رمز خودش)
    if (st.erpUser.isBlank()) {
        item {
            val p = vizitorPalette
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .metalPanel()
                    .padding(14.dp),
            ) {
                Text(
                    "ورود کاربر آتیران",
                    color = p.gold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "با همان نام کاربری و کلمهٔ عبور برنامهٔ آتیران وارد شوید تا گزارش‌ها به نام شما به‌روز شود.",
                    color = p.textSecondary,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = st.erpUser,
                    onValueChange = { st.erpUser = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("نام کاربری", color = p.textSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = st.erpPass,
                        onValueChange = { st.erpPass = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("کلمهٔ عبور", color = p.textSecondary, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (st.showErpPass) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    MaOrbButton(
                        icon = if (st.showErpPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        onClick = { st.showErpPass = !st.showErpPass },
                        size = 40.dp,
                        contentDescription = "نمایش رمز",
                    )
                }
                Spacer(Modifier.height(10.dp))
                MaGoldCta(
                    title = if (st.loginBusy) "در حال ورود…" else "ورود و همگام‌سازی",
                    subtitle = "ورود با حساب خودتان و به‌روزرسانی گزارش‌ها از سرور",
                    icon = Icons.Filled.Person,
                    enabled = !st.loginBusy && !st.busy,
                    onClick = onLogin,
                )
            }
        }
    }
    item { MaPulseCard(rows = maPulseRows(st)) }
    item { mdIntelligenceCard(st) }

    // چهار کارت شاخص — گردش مالی، مشتریان، کالاها، چک‌ها
    item {
        val p = vizitorPalette
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MaMetricCard(
                title = "گردش مالی",
                value = st.salesSum?.toLong()?.toFaPrice() ?: "—",
                icon = Icons.Filled.TrendingUp,
                modifier = Modifier.weight(1f),
                subtitle = "تومان",
                caption = maCaption(st.map.invoices, st.salesSum != null),
                tint = p.gold,
                onClick = { onOpen("report") },
            )
            MaMetricCard(
                title = "مشتریان",
                value = st.custCount?.toFaNumber() ?: "—",
                icon = Icons.Filled.Person,
                modifier = Modifier.weight(1f),
                subtitle = "رکورد",
                caption = maCaption(st.map.customers, st.custCount != null),
                tint = MaGreen,
                onClick = { st.tab = "customers" },
            )
        }
    }
    item {
        val p = vizitorPalette
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MaMetricCard(
                title = "کالاها",
                value = st.prodCount?.toFaNumber() ?: "—",
                icon = Icons.Filled.Storefront,
                modifier = Modifier.weight(1f),
                subtitle = "قلم",
                caption = maCaption(st.map.products, st.prodCount != null),
                tint = p.gold,
                onClick = { st.tab = "products" },
            )
            MaMetricCard(
                title = "چک‌ها",
                value = st.checkCount?.toFaNumber() ?: "—",
                icon = Icons.Filled.Receipt,
                modifier = Modifier.weight(1f),
                subtitle = "رکورد",
                caption = maCaption(st.map.checks, st.checkCount != null),
                tint = MaAmber,
                onClick = { st.tab = "treasury" },
            )
        }
    }

    item {
        MaGoldCta(
            title = "چارت‌های من",
            subtitle = "شخصی‌سازی داشبورد و نمودارها — انتخاب و چینش با تمِ تو",
            icon = Icons.Filled.Add,
            onClick = { onOpen("report") },
        )
    }

    // روند مطالبات و وصول طلب — ۸ ماه اخیر
    item { MaSectionHeader(title = "روند مطالبات و وصول طلب — ۸ ماه اخیر") }
    item {
        val p = vizitorPalette
        if (st.trendLabels.size >= 2) {
            LineTrendChart(
                labels = st.trendLabels,
                series = buildList {
                    add(TrendSeries("مطالبات", MaAmber, st.trendSales))
                    if (st.trendCollect.any { it > 0L }) {
                        add(TrendSeries("وصول طلب", MaGreen, st.trendCollect))
                    }
                },
            )
        } else {
            maPanel {
                Text(NO_INVOICE_DATA, color = p.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "برای رسم روند، جدول «فروش و فاکتورها» باید نگاشت شده و ستون‌های تاریخ و مبلغ داشته باشد.",
                    color = p.textSecondary,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(8.dp))
                MaThinBar(caption = "روند — خوانده نشد", fraction = 0f, valueText = "—")
            }
        }
    }

    // وضعیت مطالبات
    item { MaSectionHeader(title = "وضعیت مطالبات") }
    item {
        val p = vizitorPalette
        val sales = st.salesSum
        val checks = st.checksSum
        if (sales != null && sales > 0.0) {
            val ratio = ((checks ?: 0.0) / sales).coerceIn(0.0, 1.0).toFloat()
            maPanel {
                Text(
                    "نسبت وصول‌شده به فروش کل",
                    color = p.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                MaThinBar(
                    caption = "وصول‌شده از فروش",
                    fraction = ratio,
                    valueText = "${(ratio * 100).toInt().toFaNumber()}٪",
                    color = if (ratio >= 0.5f) MaGreen else MaAmber,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "مجموع فروش: ${sales.toLong().toFaPrice()} تومان • " +
                        "مجموع چک‌ها: ${(checks ?: 0.0).toLong().toFaPrice()} تومان",
                    color = p.textSecondary,
                    fontSize = 11.sp,
                )
            }
        } else {
            maPanel {
                Text(
                    "تفکیک وصول/باز از دیتابیس خوانده نمی‌شود — درصدی ساخته نمی‌شود.",
                    color = p.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }

    // Top Products Report — امضای بصری
    item { MaSectionHeader(title = "Top Products Report — امضای بصری M•A") }
    item {
        val p = vizitorPalette
        if (st.topProducts.isNotEmpty()) {
            MetalBarChart(rows = st.topProducts.map { it.first to it.second.toLong() })
        } else {
            maPanel {
                Text(
                    "کالایی از دیتابیس خوانده نشد — ستونی برای رسم نیست.",
                    color = p.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
    if (!st.connected || st.topProducts.isEmpty() || st.trendLabels.size < 2) {
        item { Lux3DNote(DEMO_NOTE) }
    }
}

/** کارت «M•D Intelligence» — با دادهٔ واقعی فاکتور یا متن دقیق مرجع. */
@Composable
private fun mdIntelligenceCard(st: MaHomeState) {
    val p = vizitorPalette
    MaSectionHeader(title = "M•D Intelligence")
    Spacer(Modifier.height(6.dp))
    maPanel {
        if (st.topProducts.isNotEmpty()) {
            Text(
                "برترین کالاها — بر اساس ستون مبلغ",
                color = p.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Lux3DBarChart(
                rows = st.topProducts.take(6).map { it.first to it.second.toLong() },
                height = 170.dp,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        NO_INVOICE_DATA,
                        color = p.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(NO_AMOUNT_COL, color = p.textSecondary, fontSize = 11.sp)
                }
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaAmber,
                    modifier = Modifier.width(26.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            MetalBarChart(
                rows = listOf("—" to 1L, "—" to 2L, "—" to 3L),
                height = 90.dp,
                maxBars = 3,
                valueLabel = { "" },
            )
        }
    }
}

// ═══════════════════════════ برگهٔ «مشتریان» ═══════════════════════════

private fun LazyListScope.homeCustomers(
    st: MaHomeState,
    onOpen: (String) -> Unit,
) {
    item {
        MaHeroTitle(
            title = "پرونده مشتریان",
            subtitle = "جست‌وجو، سطح‌بندی و مانده حساب — از جدول مشتریان سرور",
        )
    }
    item {
        maSearch(
            value = st.custQuery,
            hint = "جست‌وجوی مشتری…",
            onChange = { st.custQuery = it },
        )
    }
    item {
        val rows = maFilterRows(st.custRows, st.custQuery)
        if (rows.isEmpty()) {
            Column {
                MaEmptyState(
                    title = "مشتری‌ای یافت نشد",
                    subtitle = "نام یا سطح دیگری را جست‌وجو کنید",
                )
                Spacer(Modifier.height(8.dp))
                Lux3DNote(
                    if (st.map.customers.isNullOrBlank())
                        "مشتری‌ای از دیتابیس خوانده نشد — اتصال به سرور و نگاشت جدول مشتریان را بررسی کنید."
                    else
                        "مشتری‌ای از دیتابیس خوانده نشد — نگاشت جدول مشتریان را بررسی کنید.",
                    tone = MaAmber,
                )
            }
        } else {
            Lux3DTable(
                headers = maHeaders(st.custRows, st.custRoles),
                rows = rows,
                emptyText = "این جدول خالی است",
            )
        }
    }
    item { MaSectionHeader(title = "توزیع سطح مشتریان — ۳بعدی خلاقانه") }
    item {
        val p = vizitorPalette
        val tiers = maTierRows(st.topCustomers)
        if (tiers.isNotEmpty()) {
            MetalBarChart(rows = tiers, height = 200.dp, maxBars = 4)
        } else {
            maPanel {
                Text(
                    "سطح‌بندی مشتریان ساخته نشد — ستون مبلغ جدول مشتریان تشخیص داده نشد.",
                    color = p.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
    item { MaSectionHeader(title = "مانده حساب — سهم هر مشتری (دونات ۳بعدی)") }
    item {
        val p = vizitorPalette
        val shares = st.topCustomers.take(5).mapNotNull { pair ->
            pair.first.take(18) to pair.second.toFloat()
        }
        if (shares.isNotEmpty()) {
            Lux3DDonut(
                slices = shares,
                centerValue = st.topCustomers.sumOf { it.second }.toLong().toFaPrice(),
                centerLabel = "تومان",
            )
        } else {
            maPanel {
                Text(
                    "مانده‌ای خوانده نشد — با اتصال سرور، سهم هر مشتری ساخته می‌شود.",
                    color = p.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
    item {
        MaGoldCta(
            title = "مرور کامل دادهٔ مشتریان",
            subtitle = "صفحه‌بندی، جست‌وجوی زنده و جزئیات هر رکورد",
            icon = Icons.Filled.TableChart,
            onClick = { onOpen("data") },
        )
    }
    item { Lux3DNote(DEMO_NOTE) }
}

// ═══════════════════════════ برگهٔ «کالاها» ═══════════════════════════

private fun LazyListScope.homeProducts(
    st: MaHomeState,
    onOpen: (String) -> Unit,
) {
    item {
        MaHeroTitle(
            title = "کالاها",
            subtitle = "فهرست کالا، برترین‌ها و سهم دسته‌ها — از جدول کالای سرور",
        )
    }
    item {
        maSearch(value = st.prodQuery, hint = "جست‌وجوی کالا…", onChange = { st.prodQuery = it })
    }
    item {
        val rows = maFilterRows(st.prodRows, st.prodQuery)
        if (rows.isEmpty()) {
            Column {
                MaEmptyState(
                    title = "کالایی یافت نشد",
                    subtitle = "عبارت دیگری را جست‌وجو کنید",
                )
                Spacer(Modifier.height(8.dp))
                Lux3DNote(
                    "کالایی از دیتابیس خوانده نشد — اتصال و نگاشت جدول کالاها را بررسی کنید.",
                    tone = MaAmber,
                )
            }
        } else {
            Lux3DTable(
                headers = maHeaders(st.prodRows, st.prodRoles),
                rows = rows,
                emptyText = "این جدول خالی است",
            )
        }
    }
    item { MaSectionHeader(title = "برترین کالاها — بر اساس ستون مبلغ") }
    item {
        val p = vizitorPalette
        if (st.topProducts.isNotEmpty()) {
            MetalBarChart(
                rows = st.topProducts.take(7).map { it.first to it.second.toLong() },
                height = 200.dp,
            )
        } else {
            maPanel {
                Text(
                    "ستون مبلغ جدول کالاها تشخیص داده نشد — نمودار ساخته نشد.",
                    color = p.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
    item { MaSectionHeader(title = "سهم دسته‌های کالا (دونات ۳بعدی)") }
    item {
        val p = vizitorPalette
        val shares = st.topProducts.take(5).mapNotNull { it.first.take(18) to it.second.toFloat() }
        if (shares.isNotEmpty()) {
            Lux3DDonut(
                slices = shares,
                centerValue = st.topProducts.sumOf { it.second }.toLong().toFaPrice(),
                centerLabel = "تومان",
            )
        } else {
            maPanel {
                Text(
                    "دستهٔ کالایی خوانده نشد — پس از نگاشت جدول کالاها ساخته می‌شود.",
                    color = p.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
    item { Lux3DNote(DEMO_NOTE) }
}

// ═══════════════════════════ برگهٔ «خزانه» ═══════════════════════════

private fun LazyListScope.homeTreasury(
    st: MaHomeState,
    onOpen: (String) -> Unit,
) {
    item {
        MaHeroTitle(
            title = "خزانه",
            subtitle = "بانک‌ها، چک‌ها، دریافت و پرداخت — حساب‌داری هوشمند",
        )
    }
    item {
        MaSegmentPills(
            items = listOf(
                MaNavItem("recv", "دریافت", Icons.Filled.CheckCircle),
                MaNavItem("pay", "پرداخت", Icons.Filled.Receipt),
                MaNavItem("daily", "روزانه", Icons.Filled.Insights),
                MaNavItem("earn", "تحلیل", Icons.Filled.TrendingUp),
            ),
            selectedKey = st.treasuryTab,
            onSelect = { st.treasuryTab = it },
        )
    }
    item {
        maSearch(
            value = st.treasuryQuery,
            hint = "جست‌وجوی سند، مشتری یا پوز…",
            onChange = { st.treasuryQuery = it },
        )
    }
    item {
        val p = vizitorPalette
        val sum = when (st.treasuryTab) {
            "pay" -> st.banksSum
            else -> st.checksSum
        }
        Column {
            MaMetricCard(
                title = if (st.treasuryTab == "pay") "سرجمع پرداختی‌ها" else "سرجمع دریافتی‌ها",
                value = sum?.toLong()?.toFaPrice() ?: "—",
                icon = Icons.Filled.AccountBalanceWallet,
                subtitle = "تومان",
                caption = maCaption(
                    if (st.treasuryTab == "pay") st.map.banks else st.map.checks,
                    sum != null,
                ),
                tint = p.gold,
                onClick = { onOpen("data") },
            )
            Spacer(Modifier.height(8.dp))
            MaToolGrid(
                tools = listOf(
                    MaToolTile("نقد", Icons.Filled.AccountBalanceWallet, MaGreen) { onOpen("data") },
                    MaToolTile("پوز", Icons.Filled.Receipt, p.gold) { onOpen("data") },
                    MaToolTile("چک", Icons.Filled.Description, MaAmber) { onOpen("data") },
                ),
                columns = 3,
            )
            Spacer(Modifier.height(8.dp))
            Lux3DNote(TREASURY_DEMO_NOTE)
        }
    }
    item {
        val rows = maFilterRows(st.checkRows ?: st.bankRows, st.treasuryQuery)
        if (rows.isEmpty()) {
            MaEmptyState(
                title = "سند دریافتی یافت نشد",
                subtitle = "فیلتر یا جست‌وجو را تغییر دهید",
            )
        } else {
            Lux3DTable(
                headers = maHeaders(st.checkRows ?: st.bankRows, null),
                rows = rows,
                emptyText = "هنوز رکوردی در این جدول ثبت نشده است",
            )
        }
    }
    item {
        MaGoldCta(
            title = "نگاشت جدول‌های خزانه",
            subtitle = "چک‌ها، بانک‌ها و حساب‌ها را به جدول واقعی سرور وصل کنید",
            icon = Icons.Filled.Link,
            onClick = { onOpen("map") },
        )
    }
    item { Lux3DNote(DEMO_NOTE) }
}

// ═══════════════════════════ برگهٔ «گزارش‌ها» ═══════════════════════════

private fun LazyListScope.homeReports(
    st: MaHomeState,
    ctx: Context,
) {
    val text = maReportText(st)
    item {
        MaDocCard(
            title = "گزارش وضعیت مطالبات مشتریان",
            subtitle = "دوره: کل دادهٔ موجود در دیتابیس • مشتریان",
            rows = listOf(
                "تعداد مشتریان" to (st.custCount?.toFaNumber() ?: "—"),
                "جمع فروش" to (st.salesSum?.toLong()?.toFaPrice() ?: "—"),
                "چک‌های ثبت‌شده" to (st.checkCount?.toFaNumber() ?: "—"),
                "مانده" to (st.banksSum?.toLong()?.toFaPrice() ?: "—"),
            ),
            note = if (st.connected) "داده‌ها از سرور ${st.targetLabel} خوانده شده است"
            else "برای دیدن اعداد واقعی، از «اتصال به دیتابیس» شروع کنید",
        )
    }
    item {
        val p = vizitorPalette
        MaToolGrid(
            tools = listOf(
                MaToolTile("اشتراک", Icons.Filled.Share, p.gold) { maShare(ctx, text) },
                MaToolTile("کپی", Icons.Filled.ContentCopy, MaGreen) { maCopy(ctx, text) },
                MaToolTile("چاپ", Icons.Filled.Print, MaAmber) { maShare(ctx, text) },
                MaToolTile("Word", Icons.Filled.Description, p.gold) {},
                MaToolTile("PDF", Icons.Filled.Description, MaRed) {},
            ),
            columns = 5,
        )
    }
    item {
        Column {
            MaGoldCta(
                title = "به‌روزرسانی اطلاعات",
                subtitle = "خواندن دوبارهٔ همهٔ شاخص‌ها از سرور (فقط-خواندنی)",
                icon = Icons.Filled.Refresh,
                onClick = { st.tick += 1 },
            )
            Spacer(Modifier.height(8.dp))
            Lux3DNote(
                "خروجی Word و PDF در این نسخه فعال نیست — «اشتراک» و «کپی» متنِ همین گزارش را می‌دهند.",
                tone = MaAmber,
            )
        }
    }
    item { Lux3DNote(DEMO_NOTE) }
}

// ═══════════════════════════ برگهٔ «اعلان‌ها» ═══════════════════════════

private fun LazyListScope.homeAlerts(
    st: MaHomeState,
    alerts: List<Pair<String, String>>,
    onOpen: (String) -> Unit,
) {
    item {
        MaHeroTitle(
            title = "اعلان‌ها",
            subtitle = "در انتظار • انجام‌شده",
        )
    }
    item {
        MaGoldCta(
            title = "بررسی اتصال و نگاشت",
            subtitle = "اگر گزارش‌ها خالی است، از اینجا اتصال و جدول‌ها را بررسی کنید",
            icon = Icons.Filled.Link,
            onClick = { onOpen("map") },
        )
    }
    item {
        MaSegmentPills(
            items = listOf(
                MaNavItem("all", "همه", Icons.Filled.Notifications),
                MaNavItem("server", "سرور", Icons.Filled.Cloud),
                MaNavItem("map", "نگاشت", Icons.Filled.Link),
                MaNavItem("ok", "انجام‌شده", Icons.Filled.CheckCircle),
            ),
            selectedKey = st.alertTab,
            onSelect = { st.alertTab = it },
        )
    }
    item {
        val shown = when (st.alertTab) {
            "server" -> alerts.filter { it.first.startsWith("سرور") }
            "map" -> alerts.filter { it.first.startsWith("نگاشت") }
            "ok" -> alerts.filter { it.first.startsWith("انجام") }
            else -> alerts
        }
        if (shown.isEmpty()) {
            MaEmptyState(title = "همه‌چیز مرتب است", subtitle = "در این دسته اعلان جدیدی وجود ندارد")
        } else {
            Column {
                shown.forEach { (kind, body) ->
                    MaStatStrip(
                        items = listOf(
                            Triple(
                                kind,
                                body,
                                if (kind.startsWith("انجام")) MaGreen else MaAmber,
                            )
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
    item {
        Lux3DNote(
            "اعلان واقعی‌ای نیست — ردیف‌های سرور و یادآورهای خودتان اینجا می‌آیند.",
            tone = MaAmber,
        )
    }
    item { Lux3DNote(DEMO_NOTE) }
}

// ═══════════════════════════ اجزای کوچک خانه ═══════════════════════════

/** پانویس مرجع: خط ستاره‌دار + لوگوی استودیو + نام برنامه‌نویس. */
@Composable
private fun MaHomeFooter() {
    val p = vizitorPalette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            DEMO_NOTE,
            color = p.textSecondary,
            fontSize = 10.5.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "MEELANO STUDIO DESIGN",
            color = p.gold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.gold.copy(alpha = 0.18f))
                    .border(1.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = p.gold, modifier = Modifier.size(13.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "برنامه‌نویس • میلاد یعقوبی",
                color = p.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** کادر شیشه‌ای-فلزی که همهٔ کارت‌های خانه از آن استفاده می‌کنند. */
@Composable
private fun maPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .metalPanel()
            .padding(14.dp),
    ) { content() }
}

/** کادر جست‌وجوی خانه (همان حس فیلدهای مرجع). */
@Composable
private fun maSearch(value: String, hint: String, onChange: (String) -> Unit) {
    val p = vizitorPalette
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(hint, color = p.textSecondary, fontSize = 12.sp) },
        trailingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "جست‌وجو", tint = p.gold)
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
}

/** کاشی‌های برگهٔ «تنظیمات هوشمند» — همان گروه‌ها و چیدمان مرجع. */
@Composable
private fun maSmartGroups(st: MaHomeState): List<MaSmartGroup> {
    val p = vizitorPalette
    return listOf(
        MaSmartGroup(
            key = "smart",
            title = "مدیریت هوشمند و دسترسی سریع",
            subtitle = "جست‌وجو، همگام‌سازی، دستیار — Command Center",
            icon = Icons.Filled.Insights,
            tiles = listOf(
                MaSmartTile(
                    "data",
                    "جست‌وجوی هوشمند",
                    "Command Center — مرور و جست‌وجوی همهٔ بخش‌ها",
                    Icons.Filled.Search,
                    p.gold,
                ),
                MaSmartTile(
                    "conn",
                    "اتصال و همگام‌سازی آتیران",
                    "به‌روزرسانی خودکار گزارش‌ها",
                    Icons.Filled.Cloud,
                    MaGreen,
                ),
                MaSmartTile(
                    "reload",
                    "دریافت مجدد اطلاعات",
                    "خواندن دوبارهٔ شاخص‌ها و برترین‌ها از سرور",
                    Icons.Filled.Refresh,
                    MaAmber,
                ),
            ),
        ),
        MaSmartGroup(
            key = "data",
            title = "داده، اتصال و خروجی‌های کاربردی",
            subtitle = "سرور، جدول‌ها و خروجی گزارش",
            icon = Icons.Filled.AccountBalanceWallet,
            tiles = listOf(
                MaSmartTile(
                    "overview",
                    "فهرست جدول‌های سرور",
                    "شمار رکورد، اسکیما و حجم دیتابیس",
                    Icons.Filled.TableChart,
                    p.gold,
                ),
                MaSmartTile(
                    "map",
                    "اتصال جداول سرور",
                    "نگاشت پنج بخش گزارش به جدول‌های واقعی",
                    Icons.Filled.Link,
                    MaGreen,
                ),
                MaSmartTile(
                    "reports",
                    "گزارش‌های من",
                    "Executive Summary و ذخیرهٔ الگو",
                    Icons.Filled.Description,
                    p.gold,
                ),
                MaSmartTile(
                    "share",
                    "اشتراک‌گذاری گزارش",
                    "فرستادن متن گزارش با برنامه‌های گوشی",
                    Icons.Filled.Share,
                    MaAmber,
                ),
                MaSmartTile(
                    "copy",
                    "کپی متن گزارش",
                    "بردن متن کامل گزارش در حافظهٔ گوشی",
                    Icons.Filled.ContentCopy,
                    MaGreen,
                ),
                MaSmartTile(
                    "alerts",
                    "اعلان‌ها و هشدارها",
                    "وضعیت اتصال، نگاشت و سرور",
                    Icons.Filled.Notifications,
                    MaRed,
                ),
            ),
        ),
    )
}

// ═══════════════════════════ منطق خواندن داده (فقط-خواندنی) ═══════════════════════════

/** یک کوئری را با اطمینان اجرا می‌کند: هر خطا → null (بدون بستن صفحه). */
private suspend fun <T> maTry(block: suspend () -> T?): T? = runCatching { block() }.getOrNull()

/**
 * خواندن همهٔ شاخص‌های خانه از سرور — فقط SELECT/کاتالوگ سیستم.
 * هر بخش جداگانه اجرا می‌شود تا خطای یک جدول، بقیهٔ گزارش را نخواباند.
 */
private suspend fun maHomeLoad(st: MaHomeState, ctx: Context) {
    st.busy = true
    st.map = MaSectionStore.load()
    st.netLabel = MaServerProfile.netKind(ctx).label
    st.targetLabel = "سرور آتیران"
    val erp = SecureDbStore.loadErp()
    if (erp != null && erp.username.isNotBlank()) {
        st.erpUser = erp.username
        if (st.erpPass.isBlank()) st.erpPass = erp.password
        st.rememberErp = erp.remember
    }
    SecureDbStore.loadVisitor()?.let { v -> if (v.name.isNotBlank()) st.welcome = v.name }

    // ── اتصال خودکار: کاربر هیچ چیزی وارد نمی‌کند
    st.connected = MaSqlEngine.isConnected
    if (!st.connected && MaServerProfile.netKind(ctx) != MaNetKind.NONE) {
        val r = MaServerProfile.connectSmart(ctx)
        st.connected = r.ok
        st.channelLabel = r.channelLabel
        st.modeLabel = if (r.ok) "اتصال خودکار" else "—"
        st.msgOk = r.ok
        st.msg = MaServerProfile.safe(r.message)
    }

    if (!st.connected) {
        st.custCount = null
        st.prodCount = null
        st.checkCount = null
        st.bankCount = null
        st.tableCount = null
        st.salesSum = null
        st.checksSum = null
        st.banksSum = null
        st.custRows = null
        st.prodRows = null
        st.checkRows = null
        st.bankRows = null
        st.topCustomers = emptyList()
        st.topProducts = emptyList()
        st.trendLabels = emptyList()
        st.trendSales = emptyList()
        st.trendCollect = emptyList()
        st.busy = false
        return
    }

    st.tableCount = maTry { MaSqlEngine.overview().tables.size.toLong() }

    // ── مشتریان (شمار رکورد + برترین‌ها/سهم مانده)
    val custRef = st.map.customers
    if (!custRef.isNullOrBlank()) {
        st.custCount = maTry { maSplit(custRef)?.let { (s, t) -> MaSqlEngine.count(s, t) } }
        st.custRoles = maTry { MaMapping.rolesOf(custRef) }
        st.custRows = maTry { maReadPage(custRef, st.custRoles, 60) }
        st.topCustomers = maRowsToPairs(st.custRows, st.custRoles)
    }

    // ── کالاها
    val prodRef = st.map.products
    if (!prodRef.isNullOrBlank()) {
        st.prodCount = maTry { maSplit(prodRef)?.let { (s, t) -> MaSqlEngine.count(s, t) } }
        st.prodRoles = maTry { MaMapping.rolesOf(prodRef) }
        st.prodRows = maTry { maReadPage(prodRef, st.prodRoles, 60) }
        st.topProducts = maRowsToPairs(st.prodRows, st.prodRoles)
    }

    // ── فروش و فاکتورها (گردش مالی + روند ماهانه)
    val invRef = st.map.invoices
    if (!invRef.isNullOrBlank()) {
        st.salesRoles = maTry { MaMapping.rolesOf(invRef) }
        st.salesSum = maTry {
            val col = st.salesRoles?.amountCol ?: return@maTry null
            maSplit(invRef)?.let { (s, t) -> MaSqlEngine.sumOf(s, t, col) }
        }
        val page = maTry { maSplit(invRef)?.let { (s, t) -> MaSqlEngine.page(s, t, 1, 200) } }
        val trend = maMonthly(page, st.salesRoles)
        st.trendLabels = trend.first
        st.trendSales = trend.second
    } else {
        st.trendLabels = emptyList()
        st.trendSales = emptyList()
    }

    // ── چک‌ها
    val checkRef = st.map.checks
    if (!checkRef.isNullOrBlank()) {
        st.checkCount = maTry { maSplit(checkRef)?.let { (s, t) -> MaSqlEngine.count(s, t) } }
        st.checkRows = maTry { maReadPage(checkRef, null, 60) }
        st.checksSum = maTry {
            val roles = MaMapping.rolesOf(checkRef)
            val col = roles?.amountCol ?: return@maTry null
            maSplit(checkRef)?.let { (s, t) -> MaSqlEngine.sumOf(s, t, col) }
        }
        val cRoles = maTry { MaMapping.rolesOf(checkRef) }
        val cPage = maTry { maSplit(checkRef)?.let { (s, t) -> MaSqlEngine.page(s, t, 1, 200) } }
        val cTrend = maMonthly(cPage, cRoles)
        st.trendCollect = cTrend.second
    }

    // ── بانک‌ها و حساب‌ها
    val bankRef = st.map.banks
    if (!bankRef.isNullOrBlank()) {
        st.bankCount = maTry { maSplit(bankRef)?.let { (s, t) -> MaSqlEngine.count(s, t) } }
        st.bankRows = maTry { maReadPage(bankRef, null, 40) }
        st.banksSum = maTry {
            val roles = MaMapping.rolesOf(bankRef)
            val col = roles?.amountCol ?: return@maTry null
            maSplit(bankRef)?.let { (s, t) -> MaSqlEngine.sumOf(s, t, col) }
        }
    }

    st.msgOk = st.custCount != null || st.prodCount != null || st.salesSum != null
    st.msg = "اطلاعات از سرور خوانده شد — ${st.targetLabel}"
    st.busy = false
}

/** اتصال خودکار به سرور گزارش‌ها (دکمهٔ خانه) — بدون نمایش هیچ مشخصاتی. */
private suspend fun maConnectNow(st: MaHomeState, ctx: Context) {
    st.busy = true
    st.netLabel = MaServerProfile.netKind(ctx).label
    val r = MaServerProfile.connectSmart(ctx)
    st.connected = r.ok
    st.channelLabel = r.channelLabel
    st.modeLabel = if (r.ok) "اتصال خودکار" else "—"
    st.msgOk = r.ok
    st.msg = MaServerProfile.safe(r.message)
    st.busy = false
    if (r.ok) maHomeLoad(st, ctx)
}

/**
 * ورود با حساب خودِ کاربر (همان حساب برنامهٔ آتیران) و سپس همگام‌سازی گزارش‌ها.
 * هیچ اطلاعاتی از سرور روی صفحه نمی‌آید.
 */
private suspend fun maLogin(st: MaHomeState, ctx: Context) {
    if (st.erpUser.isBlank() || st.erpPass.isBlank()) {
        st.msgOk = false
        st.msg = "نام کاربری و کلمهٔ عبور خود را وارد کنید."
        return
    }
    st.loginBusy = true
    st.msgOk = true
    st.msg = "در حال ورود به سامانه…"

    if (!MaSqlEngine.isConnected) {
        val r = MaServerProfile.connectSmart(ctx)
        st.connected = r.ok
        st.channelLabel = r.channelLabel
        if (!r.ok) {
            st.loginBusy = false
            st.msgOk = false
            st.msg = MaServerProfile.safe(r.message)
            return
        }
    }
    if (!MaServerProfile.ensureSqlConnection(ctx)) {
        st.loginBusy = false
        st.msgOk = false
        st.msg = "اتصال به سرور برقرار نشد — شبکه را بررسی کنید."
        return
    }

    val data = MeelanoDataSource(SqlConnectionManager)
    val row = runCatching { data.login(st.erpUser.trim(), st.erpPass) }.getOrNull()
    if (row == null) {
        st.loginBusy = false
        st.msgOk = false
        st.msg = "نام کاربری یا کلمهٔ عبور درست نیست ❌ — همان حساب برنامهٔ آتیران را وارد کنید."
        return
    }
    if (!row.active || row.locked) {
        st.loginBusy = false
        st.msgOk = false
        st.msg = if (!row.active) "حساب «${row.username}» غیرفعال است."
        else "حساب «${row.username}» قفل است 🔒 — از سامانه بازش کنید."
        return
    }

    val identity = runCatching { data.visitorIdentity(row.userId, row.companyId) }.getOrNull()
    SecureDbStore.saveErp(row.username, st.erpPass, st.rememberErp)
    st.erpUser = row.username
    st.welcome = row.fullName.ifBlank { row.username }
    if (identity != null && identity.visitorRdf != null) {
        SecureDbStore.saveVisitor(
            userId = row.userId,
            companyId = row.companyId,
            visitorRdf = identity.visitorRdf,
            name = identity.displayName.ifBlank { st.welcome },
        )
    }
    st.loginBusy = false
    st.msgOk = true
    st.msg = "ورود موفق ✓ ${st.welcome} — در حال همگام‌سازی گزارش‌ها…"
    st.tick += 1   // بارگذاری دوبارهٔ اطلاعات با حساب کاربر
}

/** جداکردن «schema.table» به دو تکه — یا null اگر قالب درست نباشد. */
private fun maSplit(ref: String): Pair<String, String>? = MaMapping.split(ref)

/** خواندن یک صفحه: اگر ستون مبلغ باشد، برترین‌ها؛ وگرنه صفحهٔ اول جدول. */
private suspend fun maReadPage(ref: String, roles: MaTableRoles?, limit: Int): MaPage? {
    val (schema, table) = maSplit(ref) ?: return null
    val amount = roles?.amountCol
    if (amount.isNullOrBlank()) return MaSqlEngine.page(schema, table, 1, limit)
    return runCatching { MaSqlEngine.topBy(schema, table, amount, limit) }.getOrNull()
        ?: MaSqlEngine.page(schema, table, 1, limit)
}

/** تبدیل ردیف‌های یک صفحه به زوج «عنوان → مبلغ» برای نمودار و سهم‌ها. */
private fun maRowsToPairs(page: MaPage?, roles: MaTableRoles?): List<Pair<String, Double>> {
    if (page == null || page.rows.isEmpty()) return emptyList()
    val cols = page.columns
    fun idx(name: String?): Int? =
        name?.takeIf { it.isNotBlank() }?.let { n -> cols.indexOfFirst { it.equals(n, true) } }
            ?.takeIf { it >= 0 }

    val titleIdx = idx(roles?.titleCol) ?: idx(roles?.codeCol) ?: 0
    val amountIdx = idx(roles?.amountCol)
    return page.rows.mapNotNull { row ->
        val label = row.getOrNull(titleIdx)?.trim().orEmpty().ifBlank { "—" }
        val raw = amountIdx?.let { row.getOrNull(it) }
            ?: row.firstOrNull { it.parseAmount() != null }
        val amount = raw?.parseAmount() ?: 0.0
        if (label == "—" && amount == 0.0) null else label to amount
    }
}

/** توزیع سطح مشتریان (پلاتینی/طلایی/نقره‌ای/برنزی) از روی مبلغ‌های خوانده‌شده. */
private fun maTierRows(pairs: List<Pair<String, Double>>): List<Pair<String, Long>> {
    val values = pairs.map { it.second }.filter { it > 0.0 }.sortedDescending()
    if (values.size < 4) return emptyList()
    val step = (values.size / 4).coerceAtLeast(1)
    val plat = values.take(step).sum()
    val gold = values.drop(step).take(step).sum()
    val silver = values.drop(step * 2).take(step).sum()
    val bronze = values.drop(step * 3).sum()
    return listOf(
        "پلاتینی" to plat.toLong(),
        "طلایی" to gold.toLong(),
        "نقره‌ای" to silver.toLong(),
        "برنزی" to bronze.toLong(),
    ).filter { it.second > 0L }
}

/** سرفصل‌های جدول بر اساس نقش ستون‌ها (نام/مبلغ/تاریخ/کد). */
private fun maHeaders(page: MaPage?, roles: MaTableRoles?): List<String> {
    val cols = page?.columns ?: return emptyList()
    val picked = listOfNotNull(roles?.titleCol, roles?.amountCol, roles?.codeCol)
        .distinct()
        .filter { name -> cols.any { it.equals(name, true) } }
    return if (picked.isNotEmpty()) picked else cols.take(4)
}

/** فیلتر کردن ردیف‌های یک صفحه با جست‌وجوی متنی زنده. */
private fun maFilterRows(page: MaPage?, query: String): List<List<String>> {
    val rows = page?.rows ?: return emptyList()
    val q = query.trim()
    if (q.isBlank()) return rows
    return rows.filter { row -> row.any { it.contains(q, ignoreCase = true) } }
}

/** کلید ماه از یک مقدار تاریخ («1404/05/12» یا «2026-09-12» → «1404/05»). */
private fun maMonthKey(raw: String): String {
    val v = raw.trim().replace('T', ' ')
    val sep = v.firstOrNull { it == '/' || it == '-' || it == '.' } ?: return ""
    val parts = v.split(sep)
    if (parts.size < 2) return ""
    val y = parts[0].trim()
    val m = parts[1].trim()
    if (y.length !in 2..4 || m.isBlank() || m.length > 2) return ""
    return "$y/$m"
}

/** جمع مبلغ‌ها به تفکیک ماه — برای نمودار «روند مطالبات و وصول طلب». */
private fun maMonthly(page: MaPage?, roles: MaTableRoles?): Pair<List<String>, List<Long>> {
    val empty = emptyList<String>() to emptyList<Long>()
    val p = page ?: return empty
    val dateCol = roles?.dateCol ?: return empty
    val amountCol = roles?.amountCol ?: return empty
    val di = p.columns.indexOfFirst { it.equals(dateCol, true) }
    val ai = p.columns.indexOfFirst { it.equals(amountCol, true) }
    if (di < 0 || ai < 0) return empty
    val buckets = linkedMapOf<String, Long>()
    p.rows.forEach { row ->
        val key = maMonthKey(row.getOrNull(di).orEmpty())
        if (key.isNotBlank()) {
            val amount = row.getOrNull(ai).orEmpty().parseAmount() ?: 0.0
            buckets[key] = (buckets[key] ?: 0L) + amount.toLong()
        }
    }
    val keys = buckets.keys.toList().takeLast(8)
    if (keys.size < 2) return empty
    return keys to keys.map { buckets[it] ?: 0L }
}

/** برچسب کوچک زیر هر کارت شاخص: نام جدول نگاشت‌شده یا دلیل خوانده‌نشدن. */
private fun maCaption(ref: String?, loaded: Boolean): String = when {
    ref.isNullOrBlank() -> "جدول نگاشت نشده — «اتصال جداول»"
    !loaded -> "خوانده نشد — $ref"
    else -> ref
}

/** شش ردیف «نبض کسب‌وکار» — همه بر پایهٔ دادهٔ واقعی خوانده‌شده. */
private fun maPulseRows(st: MaHomeState): List<MaPulseRow> {
    fun row(label: String, ok: Boolean, fraction: Float, good: String, bad: String): MaPulseRow {
        val f = if (ok) fraction.coerceIn(0.2f, 1f) else 0.08f
        val color = when {
            !ok -> MaRed
            f >= 0.6f -> MaGreen
            f >= 0.3f -> MaAmber
            else -> MaRed
        }
        return MaPulseRow(label = label, fraction = f, status = if (ok) good else bad, color = color)
    }

    val sales = st.salesSum
    val collected = ((st.checksSum ?: 0.0) / (if ((sales ?: 0.0) > 0.0) sales!! else 1.0))
        .coerceIn(0.0, 1.0).toFloat()

    return listOf(
        row(
            "مشتریان",
            st.custCount != null && st.custCount!! > 0,
            ((st.custCount ?: 0L).toFloat() / 500f),
            "پایدار",
            "خوانده نشد",
        ),
        row(
            "مطالبات",
            sales != null,
            collected,
            if (collected >= 0.5f) "پایدار" else "نیازمند توجه",
            "بدون ستون مبلغ",
        ),
        row(
            "چک‌ها",
            st.checkCount != null && st.checkCount!! > 0,
            ((st.checkCount ?: 0L).toFloat() / 200f),
            if (st.checkCount == 0L) "نیازمند توجه" else "پایدار",
            "خوانده نشد",
        ),
        row(
            "موجودی",
            st.prodCount != null && st.prodCount!! > 0,
            ((st.prodCount ?: 0L).toFloat() / 300f),
            "پایدار",
            "خوانده نشد",
        ),
        row(
            "سیستم",
            st.connected,
            (((st.tableCount ?: 0L).toFloat() / 200f) + 0.35f),
            "پایدار",
            "آفلاین",
        ),
        row(
            "اتصال آتیران",
            st.connected,
            1f,
            "متصل",
            "قطع",
        ),
    )
}

/** اعلان‌های واقعی: وضعیت اتصال و نگاشت بخش‌ها. */
private fun maAlertRows(st: MaHomeState): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    if (st.connected) {
        out += "انجام‌شده" to "اتصال برقرار است — ${st.modeLabel} • ${st.targetLabel}"
    } else {
        out += "سرور" to "وصل نشده — از «اتصال به دیتابیس» شروع کنید"
    }
    val missing = buildList {
        if (st.map.customers.isNullOrBlank()) add("مشتریان")
        if (st.map.products.isNullOrBlank()) add("کالاها")
        if (st.map.invoices.isNullOrBlank()) add("فروش و فاکتورها")
        if (st.map.checks.isNullOrBlank()) add("چک‌ها")
        if (st.map.banks.isNullOrBlank()) add("بانک‌ها و حساب‌ها")
    }
    if (missing.isEmpty()) {
        out += "انجام‌شده" to "هر پنج بخش گزارش به جدول سرور وصل است"
    } else {
        out += "نگاشت" to "این بخش‌ها جدول ندارند: ${missing.joinToString("، ")}"
    }
    if (st.connected && (st.custRows?.rows?.isEmpty() == true)) {
        out += "نگاشت" to "جدول مشتریان خالی است — جدول دیگری را وصل کنید"
    }
    return out
}

/** متن گزارش (برای اشتراک و کپی) — ساخته‌شده از همان اعداد صفحه. */
private fun maReportText(st: MaHomeState): String = buildString {
    appendLine("گزارش وضعیت مطالبات مشتریان — M•A Report")
    appendLine("دوره: کل دادهٔ موجود در دیتابیس • مشتریان")
    appendLine("وضعیت اتصال: " + if (st.connected) "متصل ✓" else "وصل نشده")
    if (st.welcome.isNotBlank()) appendLine("کاربر: ${st.welcome}")
    appendLine("──────────────")
    appendLine("تعداد مشتریان: ${st.custCount?.toFaNumber() ?: "—"}")
    appendLine("تعداد کالاها: ${st.prodCount?.toFaNumber() ?: "—"}")
    appendLine("جمع فروش: ${st.salesSum?.toLong()?.toFaPrice() ?: "—"} تومان")
    appendLine("چک‌های ثبت‌شده: ${st.checkCount?.toFaNumber() ?: "—"}")
    appendLine("مانده بانک‌ها: ${st.banksSum?.toLong()?.toFaPrice() ?: "—"} تومان")
    if (st.topCustomers.isNotEmpty()) {
        appendLine("──────────────")
        appendLine("برترین مشتریان:")
        st.topCustomers.take(5).forEach {
            appendLine("• ${it.first} — ${it.second.toLong().toFaPrice()}")
        }
    }
    if (st.topProducts.isNotEmpty()) {
        appendLine("──────────────")
        appendLine("برترین کالاها:")
        st.topProducts.take(5).forEach {
            appendLine("• ${it.first} — ${it.second.toLong().toFaPrice()}")
        }
    }
    appendLine("──────────────")
    appendLine("M•A Report — Meelano Reports")
    appendLine("Meelano Studio Design · Milad Yaghoobi")
}

/** اشتراک‌گذاری متن گزارش با برنامه‌های گوشی. */
private fun maShare(ctx: Context, text: String) {
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "M•A Report")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { ctx.startActivity(Intent.createChooser(i, "اشتراک گزارش M•A Report")) }
}

/** کپی متن گزارش در حافظهٔ گوشی. */
private fun maCopy(ctx: Context, text: String) {
    runCatching {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("M•A Report", text))
    }
}
