/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۵: گزارشات و تنظیمات (Reports & Sync)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تاریخچه فاکتورها + پیکربندی سرور (IP و پورت 1433) +
 *  مدیریت همگام‌سازی + درباره ما (لایسنس و تیم توسعه)
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.HealthUiState
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.repository.ServerConfig
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.SectionTitle
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.theme.AllPalettes
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.DonutTrack
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.ThemeManager
import ir.atiran.vizitor.ui.components.MiniStat
import ir.atiran.vizitor.ui.components.ReportRow
import ir.atiran.vizitor.ui.components.TableHeader
import ir.atiran.vizitor.ui.theme.VizitorPalette
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDate
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import ir.atiran.vizitor.util.toFaTime

@Composable
fun ReportsScreen(viewModel: VizitorViewModel) {
    val invoices by viewModel.invoices.collectAsState()
    val serverInvoices by viewModel.serverInvoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val palette = vizitorPalette
    val context = LocalContext.current
    var shareTarget by remember { mutableStateOf<InvoiceEntity?>(null) }


    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("گزارشات")
                Text(
                    "تاریخچه فروش و فاکتورهای صادرشده",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                // ── کاشی‌های آماری گزارشات: سریع‌ترین نگاه به عملکرد ──
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MiniStat(
                        label = "فاکتور صادرشده",
                        value = invoices.size.toFaNumber(),
                        tint = palette.accent,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        label = "فاکتور سرور",
                        value = serverInvoices.size.toFaNumber(),
                        tint = palette.gold,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        label = "جمع مبلغ سرور",
                        value = serverInvoices.sumOf { it.total }.toFaPrice(),
                        tint = palette.accentText,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (serverInvoices.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MiniStat(
                            label = "جمع تخفیف سرور",
                            value = serverInvoices.sumOf { it.discount }.toFaPrice(),
                            tint = palette.gold,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStat(
                            label = "میانگین هر فاکتور",
                            value = (serverInvoices.sumOf { it.total } / serverInvoices.size).toFaPrice(),
                            tint = palette.accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                TableHeader(
                    title = "تاریخچهٔ فاکتورها",
                    count = invoices.size.toFaNumber() + " رکورد",
                    icon = Icons.Filled.History
                )
            }
        }

        if (invoices.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "هنوز فاکتوری صادر نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        items(invoices.take(30), key = { it.id }) { invoice ->
            InvoiceHistoryRow(invoice) { shareTarget = invoice }
        }


        // ── فاکتورهای واقعی سامانه (خوانده‌شده با اتصال مستقیم به SQL Server) ──
        item {
            TableHeader(
                title = "فاکتورهای سامانه (از سرور آتیران)",
                count = serverInvoices.size.toFaNumber() + " فاکتور",
                icon = Icons.Filled.Dns
            )
        }

        if (serverInvoices.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "هنوز فاکتوری از سرور خوانده نشده است. در «تنظیمات → اتصال به سرور آتیران» " +
                            "یا در صفحهٔ اول، دکمهٔ «همگام‌سازی» را بزنید تا فاکتورهای واقعی شما " +
                            "(dbo.sailfact و dbo.sailfact_pish) همین‌جا نمایش داده شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        items(serverInvoices.take(40), key = { "srv-${it.id}" }) { row ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "شماره ${row.number.toFaDigits()}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    (if (row.kind.contains("پیش")) palette.gold else palette.accent).copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                row.kind,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (row.kind.contains("پیش")) palette.gold else palette.accent
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        (if (row.customerName.isNotBlank()) row.customerName else "کد مشتری ${row.customerCode}") +
                            " • تاریخ ${row.dateText.toFaDigits()}",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text(
                            "مبلغ: ${row.total.toFaPrice()}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.accentText
                        )
                        if (row.discount > 0) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "تخفیف: ${row.discount.toFaPrice()}",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── گزارش مشتریان (بدهی و خرید) ────────────────────────────────────
        item {
            TableHeader(
                title = "گزارش مشتریان (بدهی و خرید)",
                count = customers.size.toFaNumber() + " مشتری",
                icon = Icons.Filled.People
            )
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    if (customers.isEmpty()) {
                        Text(
                            "فهرست مشتریان خالی است — در تب «مشتری» دکمهٔ «همگام‌سازی مشتریان از سرور» را بزنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        val debtors = customers.filter { it.debt > 0 }.sortedByDescending { it.debt }
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MiniStat(
                                label = "کل مشتریان",
                                value = customers.size.toFaNumber(),
                                tint = palette.accent,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStat(
                                label = "بدهکار",
                                value = debtors.size.toFaNumber(),
                                tint = DangerRed,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStat(
                                label = "جمع بدهی",
                                value = debtors.sumOf { it.debt }.toFaPrice(),
                                tint = palette.gold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (debtors.isEmpty()) {
                            Text("همهٔ مشتریان این ویزیتور تسویه هستند ✅", style = MaterialTheme.typography.bodySmall, color = NeonGreen)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                debtors.take(10).forEachIndexed { i, c ->
                                    ReportRow(
                                        index = (i + 1).toFaNumber(),
                                        title = c.name,
                                        subtitle = "کد ${c.code.toFaDigits()}" +
                                            (if (c.phone.isNotBlank()) " • ${c.phone.toFaDigits()}" else ""),
                                        value = c.debt.toFaPrice(),
                                        tint = DangerRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── خلاصهٔ ویترین (ارزش موجودی) ─────────────────────────────────────
        item {
            TableHeader(
                title = "خلاصهٔ انبار و کالا",
                count = products.size.toFaNumber() + " کالا",
                icon = Icons.Filled.Inventory2
            )
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    val stockValue = products.sumOf { it.price.coerceAtLeast(0) * it.stock.coerceAtLeast(0).toLong() }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        MiniStat(
                            label = "ارزش موجودی (ریال)",
                            value = stockValue.toFaPrice(),
                            tint = palette.gold,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStat(
                            label = "موجود",
                            value = products.count { it.stock > 0 }.toFaNumber(),
                            tint = NeonGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MiniStat(
                            label = "ناموجود",
                            value = products.count { it.stock <= 0.0 }.toFaNumber(),
                            tint = DangerRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        products.sortedByDescending { it.price * it.stock.toLong() }.take(8).forEachIndexed { i, pr ->
                            ReportRow(
                                index = (i + 1).toFaNumber(),
                                title = pr.name,
                                subtitle = "${pr.category.ifBlank { pr.groupName }} • موجودی ${pr.stock.toFaNumber()} ${pr.unit}",
                                value = (pr.price * pr.stock.toLong()).toFaPrice(),
                                tint = palette.gold
                            )
                        }
                    }
                }
            }
        }

        // ── درباره ما ───────────────────────────────────────────────────────
        item { SectionTitle(text = "درباره ما", icon = Icons.Filled.Info) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "آتیران ویزیتور — نسخه ۲٫۱۵٫۰",
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "سامانه فروش و ویزیت هوشمند ویژه مجموعه پخش آجیل و خشکبار آتیران، " +
                                "متصل به دیتابیس حسابداری (SQL Server) با معماری آفلاین-اول، " +
                                "همگام‌سازی خودکار و دستیار فروش مبتنی بر هوش مصنوعی.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Smartphone,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "موتور گرافیک سازگار: «${VizitorPerf.level.faLabel}» — تنظیم خودکار بر اساس قدرت گوشی شما (بدون هنگ)",
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonGreen
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "لایسنس: تجاری — تمامی حقوق برای مجموعه آتیران محفوظ است. © ۱۴۰۴",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        item { MilanoFooter() }
    }

    shareTarget?.let { inv ->
        ShareInvoiceDialog(inv, viewModel) { shareTarget = null }
    }
}

@Composable
private fun InvoiceHistoryRow(invoice: InvoiceEntity, onShare: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "فاکتور ${if (invoice.serverId != null) invoice.serverId else "#" + invoice.id}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    "${invoice.customerName} | ${invoice.createdAt.toFaDate()} — ${invoice.createdAt.toFaTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    invoice.finalAmount.toFaPrice(),
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonGreen
                )
                if (invoice.discount > 0) {
                    Text(
                        "کسورات: ${invoice.discount.toFaPrice()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gold
                    )
                }
            }
            StatusChip(
                text = when (invoice.status) {
                    InvoiceStatus.SYNCED -> "سینک شده"
                    InvoiceStatus.PENDING -> "در صف"
                    InvoiceStatus.FAILED -> "خطا"
                },
                color = when (invoice.status) {
                    InvoiceStatus.SYNCED -> NeonGreen
                    InvoiceStatus.PENDING -> Gold
                    InvoiceStatus.FAILED -> DangerRed
                }
            )
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = "اشتراک فاکتور",
                    tint = NeonGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * کارنامه عملکرد ماهانه — رتبه‌بندی مدال برنزی/نقره‌ای/طلایی.
 */


/** دیالوگ اشتراک فاکتور: PDF / Word / تصویر / متن. */
@Composable
private fun ShareInvoiceDialog(
    invoice: InvoiceEntity,
    viewModel: ir.atiran.vizitor.VizitorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اشتراک فاکتور ${invoice.serverId ?: ("#" + invoice.id)}") },
        text = {
            Column {
                ShareOption("📄 فایل PDF") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.sharePdf(context, invoice, items)
                    }
                }
                ShareOption("📝 فایل Word") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.shareWord(context, invoice, items)
                    }
                }
                ShareOption("🖼️ تصویر PNG") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.shareImage(context, invoice, items)
                    }
                }
                ShareOption("📨 متن پیام") {
                    viewModel.invoiceItems(invoice.id) { items ->
                        ir.atiran.vizitor.data.share.InvoiceShare.shareText(context, invoice, items)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun ShareOption(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = NeonGreen, modifier = Modifier.fillMaxWidth())
    }
}
