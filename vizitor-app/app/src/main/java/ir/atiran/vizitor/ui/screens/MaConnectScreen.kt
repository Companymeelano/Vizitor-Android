/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | «اتصال ویزیتور» به سبک گزارش طلایی (v2.19.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این صفحه، همان صفحهٔ اول برنامه است و عیناً مثل عکس‌های مرجع چیده شده:
 *     ۱) نوار بالا: عنوان طلایی + فلش + سه کرهٔ (تنظیمات / هشدار / جست‌وجو)
 *     ۲) نوار شش بخش (نمای کلی، مشتریان، کالاها، خزانه/سبد، گزارش‌ها، فعالیت‌ها)
 *     ۳) تیتر قهرمان + زیرنویس لوکس
 *     ۴) «نبض اتصال» — فهرست وضعیت سه‌رنگ مرجع برای سرور/دیتابیس/ورود/اختیارات
 *     ۵) کارت طلایی بزرگ «اتصال به سرور آتیران» (و همگام‌سازی پس از اتصال)
 *     ۶) فرم ورود ویزیتور (نام کاربری + رمز) و فهرست واقعی dbo.sys_vis
 *     ۷) تنظیمات کامل سرور/دیتابیس (بازشو) + تست، ذخیرهٔ امن و پاک‌سازی
 *     ۸) کارت سند MEELANO با گزارش وضعیت اتصال (ردیف‌های برچسب — مقدار)
 *     ۹) برگهٔ «تنظیمات هوشمند» با گروه‌ها و کاشی‌های رنگی
 *
 *  نکتهٔ مهم: این صفحه هیچ فراخوانی نمایشی ندارد؛ همهٔ دکمه‌ها به متدهای واقعی
 *  DirectSqlViewModel وصل‌اند و هیچ رمزی در متن/لاگ/گزارش چاپ نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.atiran.vizitor.sqldirect.ConnectionState
import ir.atiran.vizitor.sqldirect.DbSettings
import ir.atiran.vizitor.sqldirect.DiagRow
import ir.atiran.vizitor.sqldirect.DirectSqlViewModel
import ir.atiran.vizitor.sqldirect.DirectUiState
import ir.atiran.vizitor.sqldirect.SqlConnectionManager
import ir.atiran.vizitor.sqldirect.SqlDiagnostics
import ir.atiran.vizitor.sqldirect.VisitorLoginRepository
import ir.atiran.vizitor.sqldirect.VisitorOption
import ir.atiran.vizitor.sqldirect.VizitorRoleIntent
import ir.atiran.vizitor.ui.components.BtnTone
import ir.atiran.vizitor.ui.components.GlowChip
import ir.atiran.vizitor.ui.components.GlamourButton
import ir.atiran.vizitor.ui.components.GlamourTone
import ir.atiran.vizitor.ui.components.MaAmber
import ir.atiran.vizitor.ui.components.MaDocCard
import ir.atiran.vizitor.ui.components.MaGoldCta
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MaHeroTitle
import ir.atiran.vizitor.ui.components.MaNavItem
import ir.atiran.vizitor.ui.components.MaMetricCard
import ir.atiran.vizitor.ui.components.MaNavStrip
import ir.atiran.vizitor.ui.components.MaPulseCard
import ir.atiran.vizitor.ui.components.MaPulseRow
import ir.atiran.vizitor.ui.components.MaRed
import ir.atiran.vizitor.ui.components.MaSectionHeader
import ir.atiran.vizitor.ui.components.MaSmartGroup
import ir.atiran.vizitor.ui.components.MaSmartSheet
import ir.atiran.vizitor.ui.components.MaSmartTile
import ir.atiran.vizitor.ui.components.MaStatStrip
import ir.atiran.vizitor.ui.components.MaThinBar
import ir.atiran.vizitor.ui.components.MaTopBar
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.PremiumButton
import ir.atiran.vizitor.ui.components.PremiumField
import ir.atiran.vizitor.ui.components.PremiumPanel
import ir.atiran.vizitor.ui.components.ToggleRow
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.components.maStatus
import ir.atiran.vizitor.ui.components.metalPanel
import ir.atiran.vizitor.ui.components.rememberScreenFit
import ir.atiran.vizitor.ui.components.screenSafePadding
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MaConnectScreen(
    viewModel: DirectSqlViewModel,
    onBack: () -> Unit,
    onEnterPanel: () -> Unit,
    onOpenAdvanced: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenActivities: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val conn by SqlConnectionManager.state.collectAsState()
    val p = vizitorPalette
    val fit = rememberScreenFit()

    var smartSheet by remember { mutableStateOf(false) }
    var alertsOpen by remember { mutableStateOf(false) }
    var visitorListOpen by remember { mutableStateOf(false) }
    var showDbPass by remember { mutableStateOf(false) }
    var showErpPass by remember { mutableStateOf(false) }
    var diagRows by remember { mutableStateOf<List<DiagRow>?>(null) }
    var diagBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val isManager = VizitorRoleIntent.wantsManagerPanel()

    // ── پس از ورود موفق ویزیتور، خودکار به پنل می‌رویم (فقط اگر همین‌جا وارد شده باشد) ──
    var wasLoggedIn by remember { mutableStateOf(state.loggedIn) }
    LaunchedEffect(state.loggedIn, state.busy) {
        if (state.loggedIn && !state.busy && !wasLoggedIn) onEnterPanel()
        wasLoggedIn = state.loggedIn
    }
    // نخستین بار: تنظیمات ذخیره‌شدهٔ گوشی خوانده می‌شود تا «اتصال» یک‌کلیکی شود
    LaunchedEffect(Unit) { viewModel.restoreSaved() }

    val latency = (conn as? ConnectionState.Ready)?.latencyMs
    val connError = (conn as? ConnectionState.Error)?.message

    // ── محاسبهٔ نسبت‌های «نبض اتصال» از وضعیت واقعی ──
    val serverFraction = when {
        state.connected && latency != null -> 1f
        state.connected -> 0.82f
        conn is ConnectionState.Connecting -> 0.45f
        state.host.isNotBlank() -> 0.28f
        else -> 0.1f
    }
    val dbFraction = when {
        state.connected && state.database.isNotBlank() -> 1f
        state.database.isNotBlank() -> 0.35f
        else -> 0.1f
    }
    val loginFraction = when {
        state.loggedIn -> 1f
        state.visitorOptions.isNotEmpty() -> 0.35f
        else -> 0.1f
    }
    val rightsFraction = when {
        state.allowedCustomers > 0 || state.allowedProducts > 0 -> 1f
        state.loggedIn -> 0.5f
        else -> 0.1f
    }
    val healthTotal = state.health.size
    val healthOk = state.health.values.count { it }
    val healthFraction = if (healthTotal == 0) 0.1f else healthOk.toFloat() / healthTotal.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .dashboardBackdrop()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .screenSafePadding(),
            contentPadding = PaddingValues(
                start = fit.pad, end = fit.pad,
                top = if (fit.short) 8.dp else 12.dp,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (fit.short) 10.dp else 13.dp)
        ) {
            val contentMod =
                if (fit.contentMax > 0.dp) Modifier.fillMaxWidth().widthIn(max = fit.contentMax)
                else Modifier.fillMaxWidth()

            // ═══ ۱) نوار بالا ═══
            item {
                MaTopBar(
                    title = "آتیران ویزیتور",
                    eyebrow = "اتصال مستقیم به SQL Server — پورت ۱۴۳۳",
                    onBack = onBack,
                    onFilter = { smartSheet = true },
                    onAlert = { alertsOpen = true },
                    alertCount = if (conn is ConnectionState.Error) 1 else 0,
                    alertTint = if (conn is ConnectionState.Error) MaRed else MaAmber,
                    onSearch = {
                        visitorListOpen = !visitorListOpen
                        if (visitorListOpen) viewModel.loadVisitorsForLogin(force = state.visitorOptions.isEmpty())
                    },
                    chip = when {
                        state.loggedIn -> "وارد شده"
                        state.connected -> "وصل"
                        conn is ConnectionState.Connecting -> "در حال اتصال"
                        else -> "قطع"
                    },
                    chipColor = when {
                        state.loggedIn -> MaGreen
                        state.connected -> p.gold
                        conn is ConnectionState.Connecting -> MaAmber
                        else -> MaRed
                    },
                    modifier = contentMod
                )
            }

            // ═══ ۲) نوار شش بخش (مرجع) ═══
            item {
                MaNavStrip(
                    items = listOf(
                        MaNavItem("activities", "فعالیت‌ها", Icons.Filled.Analytics),
                        MaNavItem("reports", "گزارش‌ها", Icons.Filled.Receipt),
                        MaNavItem("cart", "خزانه", Icons.Filled.AccountBalanceWallet),
                        MaNavItem("catalog", "کالاها", Icons.Filled.Storefront),
                        MaNavItem("customers", "مشتریان", Icons.Filled.Person),
                        MaNavItem("home", "نمای کلی", Icons.Filled.Home),
                    ),
                    selectedKey = "home",
                    onSelect = { key ->
                        when (key) {
                            "activities" -> onOpenActivities()
                            "reports" -> onOpenReports()
                            "cart" -> onOpenCart()
                            "catalog" -> onOpenCatalog()
                            "customers" -> onOpenCustomers()
                            else -> onOpenHome()
                        }
                    },
                    modifier = contentMod
                )
            }

            // ═══ ۳) تیتر قهرمان ═══
            item {
                MaHeroTitle(
                    title = "آتیران ویزیتور",
                    subtitle = "تجربهٔ هوشمند ویزیت و فروش آتیران",
                    modifier = contentMod
                )
            }

            // ═══ ۴) نبض اتصال ═══
            item {
                val (sc, ss) = maStatus(serverFraction)
                val (dc, ds) = maStatus(dbFraction)
                val (lc, ls) = maStatus(loginFraction)
                val (rc, rs) = maStatus(rightsFraction)
                val (hc, hs) = maStatus(healthFraction)
                MaPulseCard(
                    title = "نبض اتصال ویزیتور",
                    rows = listOf(
                        MaPulseRow(
                            label = "اتصال آتیران",
                            fraction = serverFraction,
                            status = if (serverFraction >= 0.99f) "متصل" else ss,
                            color = sc,
                            hint = serverHint(state, latency, connError)
                        ),
                        MaPulseRow(
                            label = "دیتابیس",
                            fraction = dbFraction,
                            status = if (state.connected) "در دسترس" else ds,
                            color = dc,
                            hint = state.database.ifBlank { "نام دیتابیس وارد نشده" }
                        ),
                        MaPulseRow(
                            label = "ورود ویزیتور",
                            fraction = loginFraction,
                            status = if (state.loggedIn) "وارد شده" else ls,
                            color = lc,
                            hint = if (state.loggedIn)
                                "${state.loggedInUser.ifBlank { "—" }} • ${state.loggedInName.ifBlank { "—" }}"
                            else "${state.visitorOptions.size.toFaNumber()} ویزیتور در sys_vis"
                        ),
                        MaPulseRow(
                            label = "اختیارات ویزیتور",
                            fraction = rightsFraction,
                            status = if (rightsFraction >= 0.99f) "فعال" else rs,
                            color = rc,
                            hint = "مشتری ${state.allowedCustomers.toFaNumber()} • " +
                                "کالا ${state.allowedProducts.toFaNumber()} • " +
                                "انبار ${state.allowedWarehouses.toFaNumber()}"
                        ),
                        MaPulseRow(
                            label = "سلامت مسیر سند",
                            fraction = healthFraction,
                            status = if (healthTotal == 0) hs else "${healthOk.toFaNumber()}/${healthTotal.toFaNumber()}",
                            color = hc,
                            hint = if (healthTotal == 0)
                                "برای بررسی، به سرور وصل شوید"
                            else "رویه، جدول‌ها و تریگر پیش‌فاکتور آتیران"
                        ),
                    ),
                    modifier = contentMod
                )
            }

            // ═══ ۵) کارت طلایی اتصال / همگام‌سازی ═══
            item {
                MaGoldCta(
                    title = when {
                        state.busy -> "در حال اجرا…"
                        state.connected -> "همگام‌سازی داده‌های ویزیتور"
                        else -> "اتصال به سرور آتیران"
                    },
                    subtitle = when {
                        state.busy -> "لطفاً منتظر بمانید — نتیجه همین‌جا نوشته می‌شود"
                        state.connected -> "کالاها، مشتریان، فاکتورها و ویزیت‌های امروز از سرور خوانده می‌شود"
                        else -> "اتصال مستقیم به SQL Server با پورت ۱۴۳۳ — بدون IIS و بدون سرویس میانی"
                    },
                    icon = if (state.connected) Icons.Filled.Sync else Icons.Filled.Dns,
                    badge = if (latency != null) "${latency.toFaNumber()}ms" else null,
                    enabled = !state.busy,
                    onClick = {
                        if (state.connected) viewModel.syncNow() else viewModel.connectDatabase(true)
                    },
                    modifier = contentMod
                )
            }

            // ═══ ۵/۲) چهار کارت شاخص (مرجع: مشتریان / گردش مالی / چک‌ها / کالاها) ═══
            item {
                Column(modifier = contentMod) {
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                      MaMetricCard(
                          title = "مشتریان مجاز",
                          value = state.allowedCustomers.toFaNumber(),
                          icon = Icons.Filled.Person,
                          subtitle = "مشتری",
                          caption = "این ویزیتور — از sys_cus",
                          tint = p.gold,
                          modifier = Modifier.weight(1f),
                          onClick = onOpenCustomers
                      )
                      MaMetricCard(
                          title = "کالاها",
                          value = state.allowedProducts.toFaNumber(),
                          icon = Icons.Filled.Storefront,
                          subtitle = "قلم",
                          caption = "این ویزیتور — از sys_kal",
                          tint = MaGreen,
                          modifier = Modifier.weight(1f),
                          onClick = onOpenCatalog
                      )
                  }
                  Spacer(Modifier.height(8.dp))
                  Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                      MaMetricCard(
                          title = "انبارها",
                          value = state.allowedWarehouses.toFaNumber(),
                          icon = Icons.Filled.AccountBalanceWallet,
                          subtitle = "انبار",
                          caption = "این ویزیتور — از sys_anb",
                          tint = MaAmber,
                          modifier = Modifier.weight(1f)
                      )
                      MaMetricCard(
                          title = "ویزیتورها",
                          value = state.visitorCount.toFaNumber(),
                          icon = Icons.Filled.People,
                          subtitle = "رکورد",
                          caption = "کل dbo.sys_vis",
                          tint = p.accentText,
                          modifier = Modifier.weight(1f),
                          onClick = {
                              visitorListOpen = true
                              viewModel.loadVisitorsForLogin(force = state.visitorOptions.isEmpty())
                          }
                      )
                  }
                }
            }

            // ═══ ۵/۳) پیام وضعیت (اگر باشد) ═══
            if (state.status.isNotBlank()) {
                item {
                    val tone = when (state.statusKind) {
                        1 -> MaGreen
                        2 -> MaRed
                        else -> MaAmber
                    }
                    Row(
                        modifier = contentMod
                            .metalPanel(RoundedCornerShape(16.dp), corner = 16f)
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (state.statusKind) {
                                1 -> Icons.Filled.CheckCircle
                                2 -> Icons.Filled.Cancel
                                else -> Icons.Filled.Warning
                            },
                            contentDescription = null,
                            tint = tone,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            state.status,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = p.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ═══ ۶) ورود ویزیتور ═══
            item {
                PremiumPanel(
                    title = if (state.loggedIn) "ورود انجام شد" else "ورود ویزیتور به آتیران",
                    hint = if (state.loggedIn)
                        "وارد شده: ${state.loggedInUser.ifBlank { "—" }} — ${state.loggedInName.ifBlank { "—" }}"
                    else if (isManager) "با نام کاربری و کلمهٔ عبور شخصی مدیر در آتیران"
                    else "با نام کاربری و کلمهٔ عبور خودتان در آتیران",
                    icon = if (isManager) Icons.Filled.WorkspacePremium else Icons.Filled.Login,
                    accent = if (state.loggedIn) MaGreen else p.gold,
                    inner = fit.inner,
                    modifier = contentMod,
                    onHeaderClick = viewModel::toggleLogin,
                    expanded = state.loginExpanded || !state.loggedIn,
                    trailing = {
                        when {
                            state.loggedIn -> GlowChip(text = "فعال", color = MaGreen)
                            state.busy -> GlowChip(text = "در حال اجرا", color = MaAmber)
                        }
                    }
                ) {
                    AnimatedVisibility(visible = state.loginExpanded || !state.loggedIn) {
                        Column {
                            if (state.loggedIn) {
                                MaThinBar(
                                    caption = "سهمیهٔ مشتریان مجاز این ویزیتور",
                                    fraction = if (state.allowedCustomers <= 0) 0f
                                    else (state.visitorsOfUser.toFloat() / state.allowedCustomers.toFloat()),
                                    valueText = "${state.visitorsOfUser.toFaNumber()} از ${state.allowedCustomers.toFaNumber()}"
                                )
                                Spacer(Modifier.height(12.dp))
                                GlamourButton(
                                    label = if (isManager) "ورود به پنل مدیریت" else "ورود به پنل ویزیتور",
                                    subtitle = "همهٔ بخش‌ها با دادهٔ واقعی سرور فعال است",
                                    icon = Icons.Filled.Login,
                                    tone = GlamourTone.GOLD,
                                    height = 56.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = onEnterPanel
                                )
                                Spacer(Modifier.height(8.dp))
                                GlamourButton(
                                    label = "همگام‌سازی دوباره",
                                    subtitle = "کالا، مشتری، فاکتور و ویزیت امروز",
                                    icon = Icons.Filled.Sync,
                                    tone = GlamourTone.GLASS,
                                    height = 48.dp,
                                    enabled = !state.busy,
                                    loading = state.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { viewModel.syncNow() }
                                )
                            } else {
                                Text(
                                    "نام کاربری و کلمهٔ عبور خود را در سامانهٔ آتیران وارد کنید؛ " +
                                        "کالاها، مشتریان و فاکتورها خودکار خوانده می‌شوند.",
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(10.dp))
                                PremiumField(
                                    value = state.erpUser,
                                    onValueChange = viewModel::onErpUser,
                                    label = "نام کاربری",
                                    hint = "مثال: m.yaghoobi",
                                    icon = Icons.Filled.Person,
                                    minHeight = fit.fieldHeight,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(9.dp))
                                PremiumField(
                                    value = state.erpPassword,
                                    onValueChange = viewModel::onErpPassword,
                                    label = "کلمهٔ عبور",
                                    hint = "رمز شما در سامانهٔ آتیران",
                                    icon = Icons.Filled.Key,
                                    minHeight = fit.fieldHeight,
                                    visualTransformation = if (showErpPass)
                                        VisualTransformation.None else PasswordVisualTransformation(),
                                    trailing = {
                                        IconButton(onClick = { showErpPass = !showErpPass }) {
                                            Icon(
                                                if (showErpPass) Icons.Filled.VisibilityOff
                                                else Icons.Filled.Visibility,
                                                contentDescription = "نمایش/پنهان رمز",
                                                tint = p.gold
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(9.dp))
                                ToggleRow(
                                    label = "به‌خاطر سپردن",
                                    hint = "ورود سریع در اجرای بعدی — بدون تایپ مجدد",
                                    checked = state.rememberMe,
                                    onCheckedChange = viewModel::onRememberMe,
                                    icon = Icons.Filled.Key
                                )
                                Spacer(Modifier.height(12.dp))
                                GlamourButton(
                                    label = "ورود به سامانه",
                                    subtitle = "اتصال + ورود + دریافت کالا، مشتری و فاکتور",
                                    icon = Icons.Filled.Login,
                                    tone = GlamourTone.GOLD,
                                    height = 58.dp,
                                    enabled = !state.busy,
                                    loading = state.busy,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { viewModel.loginAndLoad() }
                                )
                            }

                            // ── مسیر جایگزین: انتخاب ویزیتور از dbo.sys_vis ──
                            Spacer(Modifier.height(10.dp))
                            GlamourButton(
                                label = if (visitorListOpen) "بستن فهرست ویزیتورها" else "فهرست ویزیتورها (sys_vis)",
                                subtitle = "ورود بدون تایپ رمز آتیران — فقط انتخاب ویزیتور",
                                icon = Icons.Filled.People,
                                tone = GlamourTone.GLASS,
                                height = 48.dp,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    visitorListOpen = !visitorListOpen
                                    if (visitorListOpen)
                                        viewModel.loadVisitorsForLogin(force = state.visitorOptions.isEmpty())
                                }
                            )

                            if (visitorListOpen) {
                                Spacer(Modifier.height(10.dp))
                                if (state.visitorsLoading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = p.gold,
                                        trackColor = p.gold.copy(alpha = 0.18f)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                PremiumField(
                                    value = state.visitorFilter,
                                    onValueChange = viewModel::onVisitorFilter,
                                    label = "جست‌وجو در ویزیتورها",
                                    hint = "نام، موبایل یا کد ویزیتور",
                                    icon = Icons.Filled.Search,
                                    minHeight = fit.fieldHeight,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                VisitorList(
                                    state = state,
                                    onPick = { option -> viewModel.enterAsVisitor(option) },
                                    onConnectFirst = { viewModel.connectDatabase() }
                                )
                            }
                        }
                    }
                }
            }

            // ═══ ۷) تنظیمات سرور و دیتابیس (بازشو) ═══
            item {
                PremiumPanel(
                    title = "سرور، دیتابیس و کاربر محدود",
                    hint = if (state.database.isBlank()) "پورت ۱۴۳۳ و کاربر محدود دیتابیس"
                    else "${state.database} — ${state.dbUser.ifBlank { "بدون کاربر" }}",
                    icon = Icons.Filled.Dns,
                    accent = p.gold,
                    inner = fit.inner,
                    modifier = contentMod,
                    onHeaderClick = viewModel::toggleSettings,
                    expanded = state.settingsExpanded
                ) {
                    AnimatedVisibility(visible = state.settingsExpanded) {
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                PremiumField(
                                    value = state.host,
                                    onValueChange = viewModel::onHost,
                                    label = "آدرس سرور (شبکهٔ داخلی)",
                                    hint = "192.168.1.150",
                                    icon = Icons.Filled.Dns,
                                    minHeight = fit.fieldHeight,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(9.dp))
                                PremiumField(
                                    value = state.port,
                                    onValueChange = viewModel::onPort,
                                    label = "پورت",
                                    hint = "1433",
                                    icon = Icons.Filled.SettingsEthernet,
                                    minHeight = fit.fieldHeight,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(112.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            PremiumField(
                                value = state.publicHost,
                                onValueChange = viewModel::onPublicHost,
                                label = "آی‌پی اختصاصی/اینترنتی (اختیاری)",
                                hint = "برای اتصال از بیرون شبکه",
                                icon = Icons.Filled.Public,
                                minHeight = fit.fieldHeight,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            ToggleRow(
                                label = "اتصال از بیرون شبکه",
                                hint = "استفاده از آی‌پی عمومی به‌جای آی‌پی داخلی",
                                checked = state.usePublicHost,
                                onCheckedChange = viewModel::onUsePublicHost,
                                icon = Icons.Filled.Public
                            )
                            Spacer(Modifier.height(8.dp))
                            ToggleRow(
                                label = "رمزنگاری TLS",
                                hint = "برای سرورهای قدیمی خاموش بماند",
                                checked = state.useEncryption,
                                onCheckedChange = viewModel::onUseEncryption,
                                icon = Icons.Filled.Shield
                            )
                            Spacer(Modifier.height(12.dp))
                            PremiumField(
                                value = state.database,
                                onValueChange = viewModel::onDatabase,
                                label = "نام دیتابیس (قابل تایپ دستی)",
                                hint = "Meelano",
                                icon = Icons.Filled.Storage,
                                minHeight = fit.fieldHeight,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.databases.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.databases.take(4).forEach { db ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(p.gold.copy(alpha = 0.12f))
                                                .border(1.dp, p.gold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                                .clickable { viewModel.onDatabase(db) }
                                                .padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                db,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = p.gold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "کاربر محدود دیتابیس — همان که نصب‌کننده ساخت. رمزها فقط رمزنگاری‌شده روی همین گوشی می‌مانند و هرگز چاپ نمی‌شوند.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 17.sp),
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            PremiumField(
                                value = state.dbUser,
                                onValueChange = viewModel::onDbUser,
                                label = "نام کاربر دیتابیس",
                                hint = "vizitor_android",
                                icon = Icons.Filled.Person,
                                minHeight = fit.fieldHeight,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            PremiumField(
                                value = state.dbPassword,
                                onValueChange = viewModel::onDbPassword,
                                label = "رمز کاربر دیتابیس",
                                hint = "رمز ذخیره‌شده روی این گوشی",
                                icon = Icons.Filled.Key,
                                minHeight = fit.fieldHeight,
                                visualTransformation = if (showDbPass)
                                    VisualTransformation.None else PasswordVisualTransformation(),
                                trailing = {
                                    IconButton(onClick = { showDbPass = !showDbPass }) {
                                        Icon(
                                            if (showDbPass) Icons.Filled.VisibilityOff
                                            else Icons.Filled.Visibility,
                                            contentDescription = "نمایش/پنهان رمز",
                                            tint = p.gold
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PremiumButton(
                                    text = "تست اتصال",
                                    icon = Icons.Filled.Dns,
                                    tone = BtnTone.SUCCESS,
                                    height = fit.buttonHeight,
                                    textSize = fit.buttonText,
                                    enabled = !state.busy,
                                    loading = state.busy,
                                    onClick = { viewModel.fetchDatabases() },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(9.dp))
                                PremiumButton(
                                    text = "ذخیرهٔ امن",
                                    icon = Icons.Filled.Key,
                                    tone = BtnTone.GOLD,
                                    height = fit.buttonHeight,
                                    textSize = fit.buttonText,
                                    onClick = { viewModel.saveSettings() },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            PremiumButton(
                                text = "ورود سریع با تنظیمات ذخیره‌شده",
                                icon = Icons.Filled.PlayCircle,
                                tone = BtnTone.GLASS,
                                height = 48.dp,
                                textSize = 12.5.sp,
                                enabled = !state.busy,
                                onClick = { viewModel.quickEnter() },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            PremiumButton(
                                text = "پاک کردن تنظیمات و رمزهای ذخیره‌شده",
                                icon = Icons.Filled.Warning,
                                tone = BtnTone.GLASS,
                                height = 48.dp,
                                textSize = 12.5.sp,
                                onClick = { viewModel.clearStored() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // ═══ ۸) سلامت مسیر سند آتیران ═══
            if (state.health.isNotEmpty() || state.serverInfo.isNotBlank()) {
                item {
                    Column(
                        modifier = contentMod
                            .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
                            .padding(13.dp)
                    ) {
                        MaSectionHeader(
                            title = "سلامت مسیر سند در آتیران",
                            icon = Icons.Filled.Verified,
                            count = if (healthTotal == 0) null else
                                "${healthOk.toFaNumber()}/${healthTotal.toFaNumber()}"
                        )
                        Spacer(Modifier.height(9.dp))
                        if (state.serverInfo.isNotBlank()) {
                            Text(
                                state.serverInfo,
                                fontSize = 10.5.sp,
                                lineHeight = 16.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        state.health.forEach { (key, ok) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (ok) MaGreen else MaRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    healthLabel(key),
                                    fontSize = 11.5.sp,
                                    color = p.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (ok) "آماده" else "نیست",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (ok) MaGreen else MaRed
                                )
                            }
                        }
                    }
                }
            }

            // ═══ ۹) کارت سند: گزارش وضعیت اتصال (سبک MEELANO REPORTS) ═══
            item {
                MaDocCard(
                    title = "گزارش وضعیت اتصال ویزیتور",
                    subtitle = "دوره: همین لحظه • دادهٔ موجود در دیتابیس آتیران",
                    rows = listOf(
                        "سرور و پورت" to serverLabel(state),
                        "دیتابیس" to state.database.ifBlank { "—" },
                        "کاربر دیتابیس" to state.dbUser.ifBlank { "—" },
                        "وارد‌شده به‌عنوان" to if (state.loggedIn)
                            state.loggedInUser.ifBlank { "—" } else "وارد نشده",
                        "مشتریان مجاز" to state.allowedCustomers.toFaNumber(),
                        "کالاهای مجاز" to state.allowedProducts.toFaNumber(),
                        "ویزیتورهای sys_vis" to state.visitorCount.toFaNumber(),
                        "زمان پاسخ سرور" to if (latency != null) "${latency.toFaNumber()} میلی‌ثانیه" else "—",
                    ),
                    note = connError?.let { "آخرین خطای اتصال: $it" }
                        ?: if (state.status.isNotBlank()) state.status else null,
                    modifier = contentMod
                )
            }

            item { MilanoFooter(modifier = contentMod) }
        }
    }

    // ═══ برگهٔ «تنظیمات هوشمند» ═══
    if (smartSheet) {
        MaSmartSheet(
            title = "تنظیمات هوشمند",
            subtitle = "شش دسته اصلی با زیرمجموعه‌های هوشمند — هر دسته در دسترس است",
            groups = listOf(
                MaSmartGroup(
                    key = "access",
                    title = "مدیریت هوشمند و دسترسی سریع",
                    subtitle = "جست‌وجو، همگام‌سازی، دستیار — همه در یک جا",
                    icon = Icons.Filled.Settings,
                    tiles = listOf(
                        MaSmartTile("quick", "ورود سریع", "با تنظیمات ذخیره‌شدهٔ گوشی وصل شو", Icons.Filled.PlayCircle, MaGreen),
                        MaSmartTile("test", "تست و اطلاعات سرور", "خواندن فهرست دیتابیس‌ها و نسخهٔ سرور", Icons.Filled.Dns, p.gold),
                        MaSmartTile("sync", "همگام‌سازی کامل", "کالا، مشتری، فاکتور، ویزیت امروز", Icons.Filled.Sync, MaGreen),
                        MaSmartTile("advanced", "تنظیمات پیشرفته", "صفحهٔ کامل اتصال و تشخیص عیب", Icons.Filled.Storage, p.accentText),
                    )
                ),
                MaSmartGroup(
                    key = "data",
                    title = "داده، اتصال و خروجی‌های کاربردی",
                    subtitle = "سرور، ذخیرهٔ امن رمز، خروج حساب",
                    icon = Icons.Filled.AccountBalanceWallet,
                    tiles = listOf(
                        MaSmartTile("login", "ورود به سامانه", "نام کاربری و رمز آتیران", Icons.Filled.Login, p.gold),
                        MaSmartTile("visitors", "فهرست ویزیتورها", "انتخاب از dbo.sys_vis", Icons.Filled.People, MaGreen),
                        MaSmartTile("refresh", "به‌روزرسانی ویزیتورها", "خواندن دوبارهٔ مجوزها و ویزیتورهای کاربر", Icons.Filled.Refresh, p.gold),
                        MaSmartTile("save", "ذخیرهٔ امن تنظیمات", "رمزها رمزنگاری‌شده روی گوشی", Icons.Filled.Key, MaAmber),
                        MaSmartTile("disconnect", "قطع اتصال", "بدون پاک کردن رمزهای ذخیره‌شده", Icons.Filled.Block, MaRed),
                        MaSmartTile("logout", "خروج از حساب", "پایان نشست ویزیتور", Icons.Filled.ExitToApp, MaRed),
                        MaSmartTile("clear", "پاک کردن همهٔ رمزها", "تنظیمات و رمزها از این گوشی حذف شود", Icons.Filled.Warning, MaRed),
                    )
                ),
            ),
            onPick = { key ->
                when (key) {
                    "quick" -> viewModel.quickEnter()
                    "test" -> viewModel.fetchDatabases()
                    "sync" -> viewModel.syncNow()
                    "advanced" -> { smartSheet = false; onOpenAdvanced() }
                    "login" -> { smartSheet = false; viewModel.toggleLogin() }
                    "visitors" -> {
                        smartSheet = false
                        visitorListOpen = true
                        viewModel.loadVisitorsForLogin(force = true)
                    }
                    "refresh" -> viewModel.refreshVisitors()
                    "save" -> viewModel.saveSettings()
                    "disconnect" -> viewModel.disconnect()
                    "logout" -> viewModel.logout()
                    "clear" -> viewModel.clearStored()
                }
                if (key != "advanced" && key != "visitors") smartSheet = false
            },
            onClose = { smartSheet = false }
        )
    }

    // ═══ دیالوگ هشدار/تشخیص ═══
    if (alertsOpen) {
        Dialog(onDismissRequest = { alertsOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(p.surface.copy(alpha = 0.99f), p.background.copy(alpha = 1f))
                        )
                    )
                    .border(1.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(14.dp)
            ) {
                MaSectionHeader(title = "تشخیص سریع اتصال", icon = Icons.Filled.Warning)
                Spacer(Modifier.height(9.dp))
                listOf(
                    Triple(
                        "درایور فعال",
                        if (state.connected) "بار شده" else "در انتظار اتصال",
                        state.connected
                    ),
                    Triple("آدرس سرور", serverLabel(state), state.host.isNotBlank()),
                    Triple("نام دیتابیس", state.database.ifBlank { "وارد نشده" }, state.database.isNotBlank()),
                    Triple("کاربر دیتابیس", state.dbUser.ifBlank { "وارد نشده" }, state.dbUser.isNotBlank()),
                    Triple("نشست ویزیتور", if (state.loggedIn) "فعال" else "بسته", state.loggedIn),
                ).forEach { (label, value, ok) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = if (ok) MaGreen else MaRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            label,
                            fontSize = 11.5.sp,
                            color = p.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            value,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = if (ok) MaGreen else MaRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (connError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        connError,
                        fontSize = 10.5.sp,
                        lineHeight = 16.sp,
                        color = MaAmber
                    )
                }
                Spacer(Modifier.height(10.dp))
                diagRows?.let { rows ->
                    Column {
                        rows.forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    if (r.ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (r.ok) MaGreen else MaRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        r.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = p.textPrimary
                                    )
                                    Text(
                                        r.detail,
                                        fontSize = 9.5.sp,
                                        lineHeight = 14.sp,
                                        color = TextSecondary
                                    )
                                    if (r.hint.isNotBlank()) {
                                        Text(
                                            "↳ " + r.hint,
                                            fontSize = 9.5.sp,
                                            lineHeight = 14.sp,
                                            color = MaAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                GlamourButton(
                    label = if (diagBusy) "در حال تشخیص…" else "اجرای تشخیص کامل (درایور، پورت، دیتابیس، جدول‌ها)",
                    icon = Icons.Filled.Search,
                    tone = GlamourTone.GLASS,
                    height = 48.dp,
                    enabled = !diagBusy,
                    loading = diagBusy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        diagBusy = true
                        scope.launch {
                            val cfg = DbSettings(
                                host = if (state.usePublicHost && state.publicHost.isNotBlank())
                                    state.publicHost else state.host,
                                port = state.port.toIntOrNull() ?: 1433,
                                database = state.database,
                                username = state.dbUser,
                                password = state.dbPassword,
                                useEncryption = state.useEncryption,
                            )
                            diagRows = withContext(Dispatchers.IO) {
                                runCatching { SqlDiagnostics.run(cfg) }.getOrDefault(emptyList())
                            }
                            diagBusy = false
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                GlamourButton(
                    label = "اتصال دوباره",
                    icon = Icons.Filled.Dns,
                    tone = GlamourTone.GOLD,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { alertsOpen = false; viewModel.connectDatabase(true) }
                )
                Spacer(Modifier.height(8.dp))
                GlamourButton(
                    label = "بستن",
                    icon = Icons.Filled.Cancel,
                    tone = GlamourTone.GLASS,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { alertsOpen = false }
                )
            }
        }
    }
}

// ═══════════════════════════ اجزای کمکی این صفحه ═══════════════════════════

/** فهرست ویزیتورهای واقعی dbo.sys_vis با جست‌وجو و انتخاب؛ خالی = راهنمای اتصال. */
@Composable
private fun VisitorList(
    state: DirectUiState,
    onPick: (VisitorOption) -> Unit,
    onConnectFirst: () -> Unit,
) {
    val p = vizitorPalette
    val filtered = remember(state.visitorOptions, state.visitorFilter) {
        VisitorLoginRepository.search(state.visitorOptions, state.visitorFilter)
    }
    if (filtered.isEmpty()) {
        Text(
            if (state.visitorOptions.isEmpty())
                "فهرست ویزیتورها خالی است — ابتدا «اتصال به سرور آتیران» را بزنید."
            else "ویزیتوری با این مشخصات پیدا نشد.",
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
            color = TextSecondary
        )
        if (state.visitorOptions.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            GlamourButton(
                label = "اتصال و خواندن فهرست",
                icon = Icons.Filled.Dns,
                tone = GlamourTone.GLASS,
                height = 46.dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = onConnectFirst
            )
        }
        return
    }
    Column {
        filtered.take(60).forEach { option ->
            val active = option.isActive
            val selected = state.selectedVisitorRdf == option.visitorRdf
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) p.gold.copy(alpha = 0.16f) else Color(0x0DFFFFFF)
                    )
                    .border(
                        1.dp,
                        if (selected) p.gold.copy(alpha = 0.6f) else p.gold.copy(alpha = 0.2f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = !state.busy && active) { onPick(option) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) p.gold.copy(alpha = 0.18f) else MaRed.copy(alpha = 0.14f)
                        )
                        .border(
                            1.dp,
                            if (active) p.gold.copy(alpha = 0.45f) else MaRed.copy(alpha = 0.45f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option.label.firstOrNull()?.toString() ?: "و",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (active) p.gold else MaRed
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        option.label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = p.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "کد ویزیتور ${option.visitorRdf.toFaNumber()} • کاربر ${option.userId.toFaNumber()}" +
                            if (active) "" else " • غیرفعال",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!active) {
                    GlowChip(text = "غیرفعال", color = MaRed)
                } else {
                    Icon(
                        Icons.Filled.Login,
                        contentDescription = "ورود",
                        tint = p.gold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (state.selectedVisitorName.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "آخرین انتخاب: ${state.selectedVisitorName}",
                fontSize = 10.sp,
                color = TextSecondary,
                textAlign = TextAlign.Start
            )
        }
    }
}

/** آدرس سرور نمایشی (هرگز رمز را نشان نمی‌دهد). */
private fun serverLabel(state: DirectUiState): String {
    val host = if (state.usePublicHost && state.publicHost.isNotBlank()) state.publicHost else state.host
    if (host.isBlank()) return "تعیین نشده"
    return "$host:${state.port.ifBlank { "1433" }}"
}

/** زیرنویس ردیف «اتصال آتیران» در نبض اتصال. */
private fun serverHint(state: DirectUiState, latency: Long?, error: String?): String = when {
    error != null -> "خطا: $error"
    latency != null -> "${serverLabel(state)} • پاسخ ${latency.toFaNumber()} میلی‌ثانیه"
    state.connected -> "${serverLabel(state)} • متصل"
    else -> serverLabel(state)
}

/** برگردان نام فنی جدول‌های مسیر سند به فارسی (سبک گزارش مرجع). */
private fun healthLabel(key: String): String = when (key) {
    "add_sail_pish" -> "رویهٔ ثبت پیش‌فاکتور (add_sail_pish)"
    "sailfact_pish" -> "سربرگ پیش‌فاکتور (sailfact_pish)"
    "subsailfact_pish" -> "اقلام پیش‌فاکتور (subsailfact_pish)"
    "subsailtemp_pish" -> "جدول موقت اقلام (subsailtemp_pish)"
    "trig_sst_pish" -> "تریگر انتقال اقلام (trig_sst_pish)"
    "visitors" -> "جدول ویزیتورها (sys_vis)"
    "customers" -> "جدول مشتریان"
    "products" -> "جدول کالاها"
    else -> key
}
