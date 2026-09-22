/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۶: تنظیمات (Settings)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  انتخاب تم اپلیکیشن + پیکربندی سرور آتیران (IP/پورت/مسیر/کلید) +
 *  کارت وضعیت سلامت سرور (تست دقیق اتصال) + مدیریت همگام‌سازی
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
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.HealthUiState
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.repository.ServerConfig
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.GlowChip
import ir.atiran.vizitor.ui.components.IconOrb3D
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
import ir.atiran.vizitor.ui.theme.VizitorPalette
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDate
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import ir.atiran.vizitor.util.toFaTime


@Composable
fun SettingsScreen(
    viewModel: VizitorViewModel,
    onOpenDirectSql: () -> Unit = {},
    serverSession: ir.atiran.vizitor.sqldirect.ServerSession = ir.atiran.vizitor.sqldirect.ServerSession(),
    onSyncNow: () -> Unit = {},
) {
    val config by viewModel.config.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val testing by viewModel.testing.collectAsState()
    val health by viewModel.health.collectAsState()
    val syncReport by viewModel.syncReport.collectAsState()
    val palette = vizitorPalette
    val context = LocalContext.current
    val themeId by ThemeManager.themeId.collectAsState()

    // فرم پیکربندی سرور
    var ip by remember(config.serverIp) { mutableStateOf(config.serverIp) }
    var httpPort by remember(config.httpPort) { mutableStateOf(config.httpPort.toString()) }
    var dbPort by remember(config.dbPort) { mutableStateOf(config.dbPort.toString()) }
    var apiPath by remember(config.apiPath) { mutableStateOf(config.apiPath) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var workerUrl by remember(config.workerUrl) { mutableStateOf(config.workerUrl) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonPurple,
        unfocusedBorderColor = Color(0x33FFFFFF),
        focusedLabelColor = NeonPurple,
        cursorColor = NeonPurple
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("تنظیمات")
                Text(
                    "تم اپلیکیشن، پیکربندی سرور و همگام‌سازی",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // ── اتصال به سرور آتیران (SQL Server / پورت ۱۴۳۳) — یک مسیر واحد ──
        //  همان صفحه‌ای که از «صفحهٔ اول» هم باز می‌شود؛ این‌جا فقط وضعیت و
        //  دکمهٔ همگام‌سازی است تا دو مسیر جدا و گیج‌کننده نداشته باشیم.
        item { SectionTitle(text = "اتصال به سرور آتیران", icon = Icons.Filled.Storage) }
        item {
            val okColor = when {
                serverSession.loggedIn -> palette.accent
                serverSession.connected -> palette.gold
                serverSession.configured -> palette.primary
                else -> palette.danger
            }
            val okText = when {
                serverSession.loggedIn -> "وارد شده"
                serverSession.connected -> "وصل به دیتابیس"
                serverSession.configured -> "آمادهٔ اتصال"
                else -> "تنظیم نشده"
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconOrb3D(
                            icon = Icons.Filled.Dns,
                            size = 38.dp,
                            tint = androidx.compose.ui.graphics.Color.White,
                            glowColor = okColor
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "اتصال سرور آتیران (پورت ۱۴۳۳)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp
                                ),
                                color = palette.textPrimary
                            )
                            Text(
                                "یک مسیر واحد — همان صفحهٔ اتصال روی صفحهٔ اول",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = TextSecondary
                            )
                        }
                        if (serverSession.syncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = palette.gold
                            )
                        } else {
                            GlowChip(text = okText, color = okColor)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (serverSession.serverLabel.isBlank())
                            "هنوز اتصالی تنظیم نشده — با یک ضربه تنظیمش کنید (کارت اتصال نصب‌کننده یا تایپ دستی)."
                        else serverSession.serverLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    if (serverSession.erpUser.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "کاربر سامانه: ${serverSession.erpName.ifBlank { serverSession.erpUser }}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                    }
                    if (serverSession.lastSyncSummary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            serverSession.lastSyncSummary,
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeonPurpleButton(
                            text = "مدیریت اتصال",
                            icon = Icons.Filled.Storage,
                            onClick = onOpenDirectSql,
                            modifier = Modifier.weight(1f)
                        )
                        NeonGreenButton(
                            text = "همگام‌سازی",
                            icon = Icons.Filled.Sync,
                            onClick = onSyncNow,
                            modifier = Modifier.weight(1f),
                            enabled = serverSession.loggedIn && !serverSession.syncing
                        )
                    }
                }
            }
        }

        // ── انتخاب تم اپلیکیشن ──────────────────────────────────────────────
        item { SectionTitle(text = "تم اپلیکیشن", icon = Icons.Filled.Palette) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AllPalettes.chunked(3).forEach { rowPalettes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPalettes.forEach { tp ->
                                ThemeChip(
                                    palette = tp,
                                    selected = tp.id == themeId,
                                    modifier = Modifier.weight(1f)
                                ) { ThemeManager.setTheme(context, tp.id) }
                            }
                            repeat(3 - rowPalettes.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // ── پیکربندی سرور ───────────────────────────────────────────────────
        // ── همگام‌سازی و سرویس‌های داده (اتصال مستقیم SQL Server) ────────────
        //  این برنامه داده را مستقیم از SQL Server می‌خواند (بدون وب‌سرویس/IIS)،
        //  بنابراین تنظیمات «آدرس API/کلید» حذف شد و فقط همین یک بخش باقی است.
        item { SectionTitle(text = "همگام‌سازی و سرویس‌های داده", icon = Icons.Filled.Sync) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "کالاها، مشتریان و فاکتورهای شما مستقیم از SQL Server خوانده و روی گوشی " +
                            "ذخیره می‌شوند تا همهٔ بخش‌ها (پیشخوان، ویترین، مشتریان، گزارشات) فعال باشند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))

                    if (serverSession.lastSyncSummary.isNotBlank()) {
                        Text(
                            serverSession.lastSyncSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.accent
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SyncChip("کالا", serverSession.productsCount)
                        SyncChip("مشتری", serverSession.customersCount)
                        SyncChip("فاکتور", serverSession.invoicesCount)
                    }

                    Spacer(Modifier.height(12.dp))
                    NeonGreenButton(
                        text = if (serverSession.syncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون",
                        icon = Icons.Filled.Sync,
                        enabled = serverSession.loggedIn && !serverSession.syncing,
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!serverSession.loggedIn) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "برای همگام‌سازی، اول در بخش «اتصال به سرور آتیران» وارد شوید.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item { ServerInvoiceShortcut(serverSession) }

    }
}

/** میان‌بر شمارش داده‌های همگام‌شده. */
@Composable
private fun SyncChip(label: String, count: Int) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(p.accent.copy(alpha = 0.12f))
            .border(1.dp, p.accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            "$label ${count.toFaNumber()}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = p.accentText
        )
    }
}

/** یادآوری محل دیدن فاکتورهای سامانه (تب گزارشات). */
@Composable
private fun ServerInvoiceShortcut(session: ir.atiran.vizitor.sqldirect.ServerSession) {
    val p = vizitorPalette
    if (!session.loggedIn) return
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Receipt, contentDescription = null, tint = p.gold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "گزارش فاکتورهای سامانه",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = p.textPrimary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${session.invoicesCount.toFaNumber()} فاکتور/پیش‌فاکتور از سرور خوانده شده و در " +
                    "تب «گزارشات» نمایش داده می‌شود.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/** چیپ انتخاب تم — نمونه رنگ زنده هر پالت با حلقه طلایی در حالت انتخاب. */
@Composable
private fun ThemeChip(
    palette: VizitorPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = vizitorPalette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) p.primary.copy(alpha = 0.16f) else Color(0x0FFFFFFF))
            .border(
                1.dp,
                if (selected) p.gold.copy(alpha = 0.75f) else Color(0x1FFFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(palette.primary, palette.accent)))
                    .border(2.dp, if (selected) palette.gold else Color(0x33FFFFFF), CircleShape)
            )
            if (selected) {
                Icon(
                    Icons.Filled.Check, contentDescription = "انتخاب‌شده",
                    tint = palette.onPrimary, modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            palette.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) p.gold else p.textSecondary,
            maxLines = 1
        )
    }
}

// ═══════════════════ کارت وضعیت سلامت سرور (تست دقیق اتصال) ═══════════════════

private val TABLE_LABELS = mapOf(
    "products" to "کالاها (Products)",
    "customers" to "مشتریان (CUSTOMERS)",
    "invoices" to "فاکتورها (SalesHeader)",
    "sal_mali" to "سال مالی (sal_mali)"
)

@Composable
private fun HealthStatusCard(health: HealthUiState?, testing: Boolean) {
    val palette = vizitorPalette
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // سربرگ کارت + چراغ وضعیت
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MonitorHeart, contentDescription = null,
                    tint = palette.gold, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "وضعیت سلامت سرور",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                val dotColor = when {
                    testing -> palette.gold
                    health == null -> palette.textSecondary
                    health.ok -> palette.accent
                    else -> palette.danger
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }

            when {
                testing -> {
                    Text(
                        "در حال بررسی دسترسی به سرور، اعتبار کلید API و اتصال دیتابیس…",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    CircularProgressIndicator(
                        color = palette.gold,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    )
                }

                health == null -> Text(
                    "برای بررسی دقیق بخش‌های اتصال، «تست سلامت اتصال» را بزنید. " +
                            "هر بخش سلامت به‌صورت جداگانه تیک می‌خورد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )

                !health.ok -> {
                    HealthRow(ok = false, label = "اتصال برقرار نشد", value = null)
                    Text(
                        health.error ?: "خطای نامشخص",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger
                    )
                    Text(
                        "راهنما: IP/پورت/مسیر API/کلید را با مقادیر سرور تطبیق دهید. " +
                                "اگر سرور هنوز راه‌اندازی نشده، اسکریپت Setup-VizitorServer.ps1 " +
                                "را روی سرور ویندوزی اجرا کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }

                else -> {
                    val report = health.report!!
                    HealthRow(ok = true, label = "سرور پاسخ می‌دهد", value = report.baseUrl.removeSuffix("api.php"))
                    HealthRow(ok = true, label = "اعتبار کلید API", value = "معتبر")
                    HealthRow(
                        ok = report.latencyMs < 5000,
                        label = "تأخیر شبکه",
                        value = "${report.latencyMs.toFaNumber()} م.ث"
                    )
                    HealthRow(ok = true, label = "اتصال دیتابیس SQL Server", value = report.db)
                    report.dbHost?.let { host ->
                        HealthRow(ok = true, label = "میزبان دیتابیس", value = host)
                    }
                    report.php?.let { php ->
                        HealthRow(ok = true, label = "نسخه PHP سرور", value = php)
                    }
                    report.apiVersion?.let { ver ->
                        HealthRow(ok = true, label = "نسخه وب‌سرویس", value = ver)
                    }

                    // پروب جداول کلیدی — هر جدول جداگانه تیک/ضربدر می‌خورد
                    val tables = report.tables
                    if (!tables.isNullOrEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "بررسی جداول دیتابیس:",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.gold
                        )
                        tables.forEach { (key, count) ->
                            HealthRow(
                                ok = count != null,
                                label = TABLE_LABELS[key] ?: key,
                                value = count?.toFaNumber()?.plus(" رکورد") ?: "یافت نشد!"
                            )
                        }
                        if (tables.values.any { it == null }) {
                            Text(
                                "جدول‌های «یافت نشد» باید در config.php سمت سرور " +
                                        "با نام واقعی جداول آتیران تطبیق داده شوند.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.danger
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthRow(ok: Boolean, label: String, value: String?) {
    val palette = vizitorPalette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = if (ok) "سالم" else "خطا",
            tint = if (ok) palette.accent else palette.danger,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = palette.textPrimary,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

/**
 * کارت انتخاب تم — پنج تم لاکچری (۳ تیره + ۲ روشن) با سواچ رنگی زنده.
 * انتخاب بلافاصله کل برنامه را بازرنگ می‌کند و ماندگار ذخیره می‌شود.
 */


/** یک ردیف انتخاب تم: سواچ سه‌رنگ (پس‌زمینه/اصلی/طلایی) + نام + نشان انتخاب. */
