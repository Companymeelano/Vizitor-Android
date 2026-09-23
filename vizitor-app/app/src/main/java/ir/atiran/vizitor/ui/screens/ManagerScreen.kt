/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | پنل مدیریت (v2.16.0)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  این صفحه فقط با «نام کاربری و کلمهٔ عبور شخصی مدیر در سامانهٔ آتیران»
 *  باز می‌شود (جدول واقعی dbo.sys_users) و همهٔ گزارش‌های مدیریتی را یک‌جا
 *  می‌آورد: نمودار ستونی فروش، نمودار حلقه‌ای سهم فاکتورها و دستهٔ کالاها،
 *  و جدول‌های مشتریان بدهکار/برترین مشتریان/برترین کالاها/آخرین فاکتورها.
 *  همهٔ اعداد از دادهٔ واقعی همگام‌شده از سرور محاسبه می‌شوند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.ui.components.DonutChart
import ir.atiran.vizitor.ui.components.LineTrendChart
import ir.atiran.vizitor.ui.components.MaAmber
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MaPulseCard
import ir.atiran.vizitor.ui.components.MaPulseRow
import ir.atiran.vizitor.ui.components.MaRed
import ir.atiran.vizitor.ui.components.MaStatStrip
import ir.atiran.vizitor.ui.components.MaToolGrid
import ir.atiran.vizitor.ui.components.MaToolTile
import ir.atiran.vizitor.ui.components.MetalBarChart
import ir.atiran.vizitor.ui.components.Ring3DChart
import ir.atiran.vizitor.ui.components.TrendSeries
import ir.atiran.vizitor.ui.components.maStatus
import ir.atiran.vizitor.ui.components.GlamourButton
import ir.atiran.vizitor.ui.components.GlamourCard
import ir.atiran.vizitor.ui.components.GlamourTone
import ir.atiran.vizitor.ui.components.KpiCard
import ir.atiran.vizitor.ui.components.ReportRow
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.manager.ManagerAnalytics
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

@Composable
fun ManagerScreen(
    viewModel: VizitorViewModel,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val invoices by viewModel.invoices.collectAsState()
    val serverInvoices by viewModel.serverInvoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val session by ir.atiran.vizitor.sqldirect.VizitorSession.state.collectAsState()
    val p = vizitorPalette

    val snap = remember(serverInvoices, customers, products) {
        ManagerAnalytics.snapshot(serverInvoices, customers, products)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── اگر با نقش مدیریت وارد نشده‌اید، یادآوری شفاف ──
        if (!ir.atiran.vizitor.sqldirect.VizitorRoleIntent.wantsManagerPanel()) {
            item {
                GlamourCard(title = "برای گزارش کامل مدیریت", icon = Icons.Filled.WorkspacePremium) {
                    Text(
                        "این صفحه با ورود به‌عنوان مدیر (نام کاربری و کلمهٔ عبور شخصی مدیر در سامانهٔ آتیران) " +
                            "کامل‌ترین گزارش‌ها را نشان می‌دهد. الان با نقش ویزیتور وارد شده‌اید؛ " +
                            "داده‌های زیر همان دادهٔ واقعی سرور است، ولی برای گزارش‌های مدیریتی " +
                            "از دکمهٔ زیر با حساب مدیر وارد شوید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    GlamourButton(
                        label = "ورود با حساب مدیر",
                        icon = Icons.Filled.Login,
                        tone = GlamourTone.GOLD,
                        height = 48.dp,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBack
                    )
                }
            }
        }

        // ═════════════════ سربرگ مدیر ═════════════════
        item {
            GlamourCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPurple, Gold))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Analytics,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        ShimmerGoldText("پنل مدیریت آتیران")
                        Text(
                            "گزارش کامل فروش، مشتریان و انبار — با حساب شخصی مدیر",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlamourButton(
                        label = "همگام‌سازی تازه از سرور",
                        icon = Icons.Filled.Sync,
                        tone = GlamourTone.PURPLE,
                        height = 48.dp,
                        modifier = Modifier.weight(1.6f),
                        onClick = { viewModel.syncNow() }
                    )
                    GlamourButton(
                        label = "تنظیمات",
                        icon = Icons.Filled.Settings,
                        tone = GlamourTone.GLASS,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettings
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlamourButton(
                        label = "بازگشت به صفحهٔ ورود",
                        icon = Icons.Filled.ArrowBack,
                        tone = GlamourTone.GLASS,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onBack
                    )
                    GlamourButton(
                        label = "خروج از حساب",
                        icon = Icons.Filled.ExitToApp,
                        tone = GlamourTone.GOLD,
                        height = 46.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onLogout
                    )
                }
            }
        }

        // ═════════════════ نوار خلاصهٔ عددی (سبک مرجع) ═════════════════
        item {
            MaStatStrip(
                items = listOf(
                    Triple("فروش کل", snap.totalSales.toFaPrice(), Gold),
                    Triple("فاکتور", snap.invoiceCount.toFaNumber(), NeonGreen),
                    Triple("مشتری", snap.customerCount.toFaNumber(), p.gold),
                    Triple("بدهی", snap.totalDebt.toFaPrice(), DangerRed),
                )
            )
        }

        // ═════════════════ نبض کسب‌وکار (نوار وضعیت رنگی) ═════════════════
        item {
            val salesPulse = if (snap.totalSales > 0L)
                ((snap.totalSales - snap.totalDebt).coerceAtLeast(0L).toFloat() / snap.totalSales.toFloat())
                    .coerceIn(0f, 1f) else 0f
            val officialShare = if (snap.invoiceCount > 0)
                snap.officialInvoiceCount.toFloat() / snap.invoiceCount.toFloat() else 0f
            val stockHealth = if (snap.productCount > 0)
                1f - (snap.outOfStockCount.toFloat() / snap.productCount.toFloat()) else 0f
            val cleanCustomers = if (snap.customerCount > 0)
                1f - (snap.debtorCount.toFloat() / snap.customerCount.toFloat()) else 0f
            val (c1, t1) = maStatus(salesPulse)
            val (c2, t2) = maStatus(officialShare)
            val (c3, t3) = maStatus(stockHealth)
            val (c4, t4) = maStatus(cleanCustomers)
            val (c5, t5) = maStatus(if (session.connected) 1f else 0f)
            MaPulseCard(
                rows = listOf(
                    MaPulseRow(
                        label = "وصول مطالبات",
                        fraction = salesPulse,
                        status = t1,
                        color = c1,
                        hint = "فروش منهای بدهی مشتریان"
                    ),
                    MaPulseRow(
                        label = "فاکتور رسمی",
                        fraction = officialShare,
                        status = t2,
                        color = c2,
                        hint = "نسبت به کل فاکتورها"
                    ),
                    MaPulseRow(
                        label = "سلامت انبار",
                        fraction = stockHealth,
                        status = t3,
                        color = c3,
                        hint = "${snap.outOfStockCount.toFaNumber()} کالای ناموجود"
                    ),
                    MaPulseRow(
                        label = "مشتریان خوش‌حساب",
                        fraction = cleanCustomers,
                        status = t4,
                        color = c4,
                        hint = "${snap.debtorCount.toFaNumber()} مشتری بدهکار"
                    ),
                    MaPulseRow(
                        label = "اتصال سرور آتیران",
                        fraction = if (session.connected) 1f else 0f,
                        status = t5,
                        color = c5,
                        hint = if (session.connected) "متصل — ${session.serverLabel}" else "وصل نیست"
                    ),
                )
            )
        }

        // ═════════════════ شاخص‌های کلیدی ═════════════════
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(
                        label = "فروش کل (ریال)",
                        value = snap.totalSales.toFaPrice(),
                        icon = Icons.Filled.TrendingUp,
                        tint = Gold,
                        modifier = Modifier.weight(1.3f)
                    )
                    KpiCard(
                        label = "تعداد فاکتور",
                        value = snap.invoiceCount.toFaNumber(),
                        icon = Icons.Filled.Receipt,
                        tint = NeonGreen,
                        modifier = Modifier.weight(1f),
                        footnote = "رسمی ${snap.officialInvoiceCount.toFaNumber()} • پیش ${snap.preInvoiceCount.toFaNumber()}"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(
                        label = "میانگین هر فاکتور",
                        value = snap.averageInvoice.toFaPrice(),
                        icon = Icons.Filled.BarChart,
                        tint = NeonPurple,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "جمع تخفیف‌ها",
                        value = snap.totalDiscount.toFaPrice(),
                        icon = Icons.Filled.AccountBalanceWallet,
                        tint = Color(ManagerAnalytics.C_GOLD),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiCard(
                        label = "مشتریان",
                        value = snap.customerCount.toFaNumber(),
                        icon = Icons.Filled.Group,
                        tint = NeonGreen,
                        modifier = Modifier.weight(1f),
                        footnote = "بدهکار: ${snap.debtorCount.toFaNumber()}"
                    )
                    KpiCard(
                        label = "جمع بدهی مشتریان",
                        value = snap.totalDebt.toFaPrice(),
                        icon = Icons.Filled.People,
                        tint = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "ارزش انبار",
                        value = snap.stockValue.toFaPrice(),
                        icon = Icons.Filled.Inventory2,
                        tint = Gold,
                        modifier = Modifier.weight(1f),
                        footnote = "ناموجود: ${snap.outOfStockCount.toFaNumber()}"
                    )
                }
            }
        }

        // ═════════════════ نمودار فروش ═════════════════
        item {
            GlamourCard(title = "نمودار فروش به تفکیک تاریخ", icon = Icons.Filled.BarChart) {
                if (snap.salesByDay.isEmpty()) {
                    Text(
                        "برای نمایش نمودار، ابتدا از دکمهٔ «همگام‌سازی تازه از سرور» استفاده کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    MetalBarChart(rows = snap.salesByDay)
                }
            }
        }

        // ═════════════════ روند فروش و تخفیف (نمودار خطی دوسری) ═════════════════
        item {
            GlamourCard(title = "روند فروش و تخفیف — تاریخ‌های اخیر", icon = Icons.Filled.TrendingUp) {
                if (snap.salesTrend.size < 2) {
                    Text(
                        "برای رسم روند، دست‌کم دو تاریخ با فاکتور لازم است — «همگام‌سازی تازه از سرور» را بزنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    LineTrendChart(
                        labels = snap.salesTrend.map { it.first },
                        series = listOf(
                            TrendSeries(
                                name = "فروش",
                                color = Gold,
                                values = snap.salesTrend.map { it.second }
                            ),
                            TrendSeries(
                                name = "تخفیف",
                                color = NeonGreen,
                                values = snap.salesTrend.map { it.third }
                            ),
                        )
                    )
                }
            }
        }

        // ═════════════════ نمودار حلقه‌ای فاکتورها ═════════════════
        item {
            GlamourCard(title = "سهم فاکتور رسمی و پیش‌فاکتور", icon = Icons.Filled.Receipt) {
                DonutChart(
                    slices = snap.invoiceSplit.map { Triple(it.first, it.second, Color(it.third)) },
                    centerTitle = "کل فاکتور",
                    centerValue = snap.invoiceCount.toFaNumber()
                )
            }
        }

        // ═════════════════ حلقهٔ سه‌بعدی درخشان (سبک مرجع) ═════════════════
        item {
            GlamourCard(title = "نسبت وصول مطالبات — حلقهٔ سه‌بعدی", icon = Icons.Filled.AccountBalanceWallet) {
                val collect = if (snap.totalSales > 0L)
                    ((snap.totalSales - snap.totalDebt).coerceAtLeast(0L).toFloat() / snap.totalSales.toFloat())
                        .coerceIn(0f, 1f) else 0f
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Ring3DChart(
                        percent = collect,
                        centerTitle = "وصول از فروش",
                        centerValue = "${(collect * 100).toInt().toFaNumber()}٪"
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "جمع فروش ${snap.totalSales.toFaPrice()} ریال • جمع بدهی مشتریان ${snap.totalDebt.toFaPrice()} ریال",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // ═════════════════ سهم دسته‌های کالا ═════════════════
        item {
            GlamourCard(title = "سهم دسته‌های کالا از ارزش انبار", icon = Icons.Filled.Category) {
                DonutChart(
                    slices = snap.categoryValue.map { Triple(it.first, it.second, Color(it.third)) },
                    centerTitle = "دستهٔ کالا",
                    centerValue = snap.categoryValue.size.toFaNumber()
                )
            }
        }

        // ═════════════════ جدول مشتریان بدهکار ═════════════════
        item {
            GlamourCard(title = "گزارش مشتریان بدهکار", icon = Icons.Filled.People) {
                if (snap.debtors.isEmpty()) {
                    Text("مشتری بدهکاری ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        snap.debtors.forEachIndexed { i, c ->
                            ReportRow(
                                index = (i + 1).toFaNumber(),
                                title = c.name,
                                subtitle = listOf(
                                    "کد ${c.code.toFaDigits()}",
                                    c.city.takeIf { it.isNotBlank() }?.let { "شهر $it" }.orEmpty(),
                                    c.phone.takeIf { it.isNotBlank() }.orEmpty(),
                                ).filter { it.isNotBlank() }.joinToString(" • "),
                                value = c.debt.toFaPrice(),
                                tint = DangerRed
                            )
                        }
                    }
                }
            }
        }

        // ═════════════════ جدول برترین مشتریان ═════════════════
        item {
            GlamourCard(title = "برترین مشتریان (بر اساس خرید)", icon = Icons.Filled.WorkspacePremium) {
                if (snap.topCustomers.isEmpty()) {
                    Text("فاکتوری برای تحلیل مشتریان یافت نشد.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        snap.topCustomers.forEachIndexed { i, t ->
                            ReportRow(
                                index = (i + 1).toFaNumber(),
                                title = t.first,
                                subtitle = "کد مشتری ${t.second.toFaDigits()}",
                                value = t.third.toFaPrice(),
                                tint = NeonGreen
                            )
                        }
                    }
                }
            }
        }

        // ═════════════════ جدول برترین کالاها ═════════════════
        item {
            GlamourCard(title = "برترین کالاها (به ارزش موجودی)", icon = Icons.Filled.Inventory2) {
                if (snap.topProducts.isEmpty()) {
                    Text("فهرست کالا خالی است.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        snap.topProducts.forEachIndexed { i, t ->
                            ReportRow(
                                index = (i + 1).toFaNumber(),
                                title = t.first,
                                subtitle = t.second,
                                value = t.third.toFaPrice(),
                                tint = Gold
                            )
                        }
                    }
                }
            }
        }

        // ═════════════════ جدول آخرین فاکتورها ═════════════════
        item {
            GlamourCard(title = "آخرین فاکتورهای سامانه", icon = Icons.Filled.Receipt) {
                if (snap.recentInvoices.isEmpty()) {
                    Text("فاکتوری خوانده نشده است.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        snap.recentInvoices.forEach { inv ->
                            ReportRow(
                                index = inv.kind.take(1),
                                title = "شماره ${inv.number.toFaDigits()}",
                                subtitle = listOf(
                                    inv.customerName.ifBlank { "کد مشتری ${inv.customerCode.toFaDigits()}" },
                                    "تاریخ ${inv.dateText.toFaDigits()}",
                                ).joinToString(" • "),
                                value = inv.total.toFaPrice(),
                                tint = if (inv.kind.contains("پیش")) Color(ManagerAnalytics.C_GOLD) else NeonGreen
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "همهٔ اعداد این صفحه از دادهٔ واقعی همگام‌شده از سرور آتیران " +
                        "(dbo.sailfact، dbo.sailfact_pish، dbo.CUSTOMERS، dbo.inventory) محاسبه شده است.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        // ═════════════════ فاکتورهای محلی (صادرشده از همین برنامه) ═════════════════
        item {
            GlamourCard(title = "پیش‌فاکتورهای صادرشده از برنامه", icon = Icons.Filled.Receipt) {
                if (invoices.isEmpty()) {
                    Text("هنوز پیش‌فاکتوری از این برنامه صادر نشده است.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        invoices.take(8).forEachIndexed { i, inv ->
                            ReportRow(
                                index = (i + 1).toFaNumber(),
                                title = inv.note.ifBlank { "پیش‌فاکتور #${inv.id}" },
                                subtitle = "مبلغ نهایی: ${inv.finalAmount.toFaPrice()}",
                                value = inv.grossAmount.toFaPrice(),
                                tint = NeonPurple
                            )
                        }
                    }
                }
            }
        }

        // ═════════════════ دسترسی سریع مدیر (کاشی‌های ابزار) ═════════════════
        item {
            MaToolGrid(
                title = "دسترسی سریع مدیریت",
                columns = 5,
                tools = listOf(
                    MaToolTile("همگام‌سازی سرور", Icons.Filled.Sync, NeonPurple) { viewModel.syncNow() },
                    MaToolTile("همگام‌سازی مشتریان", Icons.Filled.People, MaGreen) { viewModel.syncCustomersNow() },
                    MaToolTile("تنظیمات اتصال", Icons.Filled.Settings, MaAmber) { onOpenSettings() },
                    MaToolTile("صفحهٔ ورود", Icons.Filled.ArrowBack, Gold) { onBack() },
                    MaToolTile("خروج از حساب", Icons.Filled.ExitToApp, MaRed) { onLogout() },
                )
            )
        }

        item {
            Column {
              Spacer(Modifier.height(4.dp))
              Text(
                  "Meelano Studio Design • Milad Yaghoobi — پنل مدیریت آتیران ویزیتور",
                  style = MaterialTheme.typography.labelSmall,
                  color = p.gold,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
        }
    }
}
