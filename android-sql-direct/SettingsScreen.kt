/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۶: تنظیمات (Settings)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  انتخاب تم اپلیکیشن + پیکربندی سرور آتیران (IP/پورت/مسیر/کلید) +
 *  کارت وضعیت سلامت سرور (تست دقیق اتصال) + مدیریت همگام‌سازی
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.HealthUiState
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.repository.ServerConfig
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.data.sql.DbSettings
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
import ir.atiran.vizitor.ui.theme.VizitorPalette
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDate
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import ir.atiran.vizitor.util.toFaTime


@Composable
fun SettingsScreen(viewModel: VizitorViewModel) {
    val config by viewModel.config.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val testing by viewModel.testing.collectAsState()
    val health by viewModel.health.collectAsState()
    val syncReport by viewModel.syncReport.collectAsState()
    val palette = vizitorPalette
    val context = LocalContext.current
    val themeId by ThemeManager.themeId.collectAsState()
    // وضعیت اتصال مستقیم SQL Server (معماری جدید لایهٔ داده)
    val dbState by viewModel.dbState.collectAsState()
    val dbTesting by viewModel.dbTesting.collectAsState()
    val savedDb by viewModel.dbConfig.collectAsState()
    val dbOptions by viewModel.dbOptions.collectAsState()
    val dbListLoading by viewModel.dbListLoading.collectAsState()

    // فرم پیکربندی سرور
    var ip by remember(config.serverIp) { mutableStateOf(config.serverIp) }
    var httpPort by remember(config.httpPort) { mutableStateOf(config.httpPort.toString()) }
    var dbPort by remember(config.dbPort) { mutableStateOf(config.dbPort.toString()) }
    var apiPath by remember(config.apiPath) { mutableStateOf(config.apiPath) }
    var apiKey by remember(config.apiKey) { mutableStateOf(config.apiKey) }
    var useHttps by remember(config.useHttps) { mutableStateOf(config.useHttps) }
    var workerUrl by remember(config.workerUrl) { mutableStateOf(config.workerUrl) }

    // فرم اتصال مستقیم SQL Server — دو نشانی: آی‌پی اختصاصی (بیرون شبکه) و آی‌پی داخلی
    val savedExtHost by viewModel.dbHostExternal.collectAsState()
    val savedLanHost by viewModel.dbHostLocal.collectAsState()
    val useExternal by viewModel.dbUseExternal.collectAsState()

    var dbHostExternal by remember(savedExtHost) { mutableStateOf(savedExtHost) }
    var dbHostLocal by remember(savedLanHost) { mutableStateOf(savedLanHost) }
    var dbPortText by remember(savedDb) { mutableStateOf((savedDb?.port ?: 1433).toString()) }
    var dbName by remember(savedDb) { mutableStateOf(savedDb?.database ?: "") }
    var dbUser by remember(savedDb) { mutableStateOf(savedDb?.username ?: "") }
    var dbPass by remember { mutableStateOf("") }

    /** نشانی فعال بر اساس انتخاب کاربر (پیش‌فرض: آی‌پی اختصاصی اگر وارد شده باشد). */
    fun activeHost(): String {
        val ext = dbHostExternal.trim()
        val loc = dbHostLocal.trim()
        return if (useExternal) ext.ifBlank { loc } else loc.ifBlank { ext }
    }

    fun currentSettings(database: String) = DbSettings(
        host = activeHost(),
        port = dbPortText.toIntOrNull() ?: 1433,
        database = database,
        username = dbUser.trim(),
        password = dbPass
    )

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

        // ── اتصال مستقیم SQL Server (لایهٔ دادهٔ اصلی برنامه) ──────────────
        item { SectionTitle(text = "اتصال مستقیم SQL Server", icon = Icons.Filled.Storage) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                    // ── ۱) روش اتصال: آی‌پی اختصاصی (بیرون شبکه) یا آی‌پی داخلی ──
                    Text(
                        "۱) روش اتصال را انتخاب کنید:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ModeChip(
                            label = "از اینترنت (آی‌پی اختصاصی)",
                            icon = Icons.Filled.Language,
                            selected = useExternal,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.setDbAddresses(dbHostExternal, dbHostLocal, true)
                            }
                        )
                        ModeChip(
                            label = "شبکهٔ داخلی (مثلاً 192.168.x.x)",
                            icon = Icons.Filled.Router,
                            selected = !useExternal,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewModel.setDbAddresses(dbHostExternal, dbHostLocal, false)
                            }
                        )
                    }

                    OutlinedTextField(
                        value = dbHostExternal,
                        onValueChange = {
                            dbHostExternal = it
                            viewModel.setDbAddresses(it, dbHostLocal, useExternal)
                        },
                        label = { Text("آی‌پی اختصاصی سرور SQL (بیرون شبکه)") },
                        placeholder = { Text("مثلاً 37.143.147.19") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dbHostLocal,
                        onValueChange = {
                            dbHostLocal = it
                            viewModel.setDbAddresses(dbHostExternal, it, useExternal)
                        },
                        label = { Text("آی‌پی داخلی سرور SQL (داخل شبکهٔ فروشگاه)") },
                        placeholder = { Text("مثلاً 192.168.1.150") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── ۲) پورت و کاربر/رمز ورود به SQL ─────────────────────────
                    Text(
                        "۲) پورت (پیش‌فرض ۱۴۳۳) و نام کاربری و کلمهٔ عبور SQL Server:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = dbPortText,
                            onValueChange = { dbPortText = it.filter(Char::isDigit) },
                            label = { Text("پورت") },
                            singleLine = true, colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dbUser, onValueChange = { dbUser = it },
                            label = { Text("نام کاربری SQL") },
                            singleLine = true, colors = fieldColors,
                            modifier = Modifier.weight(2f)
                        )
                    }
                    OutlinedTextField(
                        value = dbPass, onValueChange = { dbPass = it },
                        label = { Text("کلمهٔ عبور SQL") },
                        singleLine = true, colors = fieldColors,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── ۳) تأیید و گرفتن لیست دیتابیس‌ها ────────────────────────
                    Text(
                        "۳) دکمهٔ زیر را بزنید؛ اگر سرور/کاربر/رمز درست باشد، لیست دیتابیس‌های همان سرور می‌آید:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textPrimary
                    )
                    NeonPurpleButton(
                        text = if (dbListLoading) "در حال گرفتن لیست…" else "تأیید و دریافت لیست دیتابیس‌ها",
                        icon = Icons.Filled.Storage,
                        enabled = !dbListLoading && activeHost().isNotBlank() && dbUser.isNotBlank(),
                        onClick = { viewModel.loadDatabases(currentSettings("master")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (dbOptions.isNotEmpty()) {
                        Text(
                            "۴) دیتابیس برنامه را انتخاب کنید:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textPrimary
                        )
                        dbOptions.chunked(2).forEach { rowItems ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { name ->
                                    val selected = name == dbName
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (selected) NeonPurple.copy(alpha = 0.25f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { dbName = name }
                                    ) {
                                        Text(
                                            name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (selected) NeonPurple else palette.textSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                                repeat(2 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = dbName, onValueChange = { dbName = it },
                        label = { Text("نام دیتابیس انتخاب‌شده") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── ۵) اعمال تنظیمات و تست اتصال ────────────────────────────
                    Row {
                        NeonGreenButton(
                            text = if (dbTesting) "در حال تست…" else "اعمال تنظیمات و تست اتصال",
                            icon = Icons.Filled.CloudDone,
                            enabled = !dbTesting && activeHost().isNotBlank() &&
                                    dbName.isNotBlank() && dbUser.isNotBlank() && dbPass.isNotEmpty(),
                            onClick = {
                                viewModel.setDbAddresses(dbHostExternal, dbHostLocal, useExternal)
                                viewModel.testDbConnection(currentSettings(dbName.trim()))
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        NeonPurpleButton(
                            text = "قطع اتصال",
                            enabled = dbState is ConnectionState.Ready,
                            onClick = { viewModel.disconnectDb() }
                        )
                    }

                    // ── وضعیت زندهٔ اتصال ───────────────────────────────────────
                    val statusText = when (val st = dbState) {
                        is ConnectionState.Ready -> "متصل به ${dbName} ✅ (${st.latencyMs}ms)"
                        ConnectionState.Connecting -> "در حال اتصال…"
                        ConnectionState.Disconnected ->
                            if (savedDb != null) "پیکربندی ذخیره شده — اتصال برقرار نیست"
                            else "پیکربندی نشده"
                        is ConnectionState.Error -> st.message
                    }
                    val statusColor = when (dbState) {
                        is ConnectionState.Ready -> NeonGreen
                        is ConnectionState.Error -> DangerRed
                        else -> palette.textSecondary
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (dbState) {
                                is ConnectionState.Ready -> Icons.Filled.CheckCircle
                                is ConnectionState.Error -> Icons.Filled.ErrorOutline
                                else -> Icons.Filled.Info
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }

                    Text(
                        "آی‌پی اختصاصی برای زمانی است که گوشی بیرون از شبکهٔ فروشگاه باشد (پورت ۱۴۳۳ باید " +
                                "از بیرون باز/فوروارد شده باشد). داخل فروشگاه همان آی‌پی داخلی کافی است. " +
                                "رمز فقط رمزنگاری‌شده (Android Keystore) روی همین دستگاه ذخیره می‌شود؛ " +
                                "نه در سرور، نه در فایل پیکربندی و نه در هیچ Log.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // ── شناسنامهٔ سامانه و سازنده (کارت برجسته با نور و لبه) ───────────
        item { SectionTitle(text = "دربارهٔ سامانه", icon = Icons.Filled.MilitaryTech) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                NeonPurple.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.dp, NeonPurple.copy(alpha = 0.45f)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(NeonPurple, NeonPurple.copy(alpha = 0.25f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MilitaryTech,
                                contentDescription = null,
                                tint = palette.textPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "سامانهٔ ویزیتور — نسخهٔ ۱.۳",
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                "اتصال مستقیم و امن به SQL Server (پورت ۱۴۳۳)",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Brush.horizontalGradient(listOf(NeonPurple, Color.Transparent)))
                    )
                    Text(
                        "طراحی و برنامه‌نویسی:  میلاد یقوبی  (Milad Yaghoobi)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonPurple
                    )
                    Text(
                        "گروه نرم‌افزاری:  Meelano Studio Design",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonPurple
                    )
                    Text(
                        "بدون IIS و بدون API میانی؛ رمزها فقط رمزنگاری‌شده (Android Keystore) " +
                                "روی همین دستگاه می‌مانند و هیچ‌وقت در لاگ یا گزارش چاپ نمی‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }
        }

        // ── پیکربندی سرور ───────────────────────────────────────────────────
        item { SectionTitle(text = "پیکربندی سرور آتیران", icon = Icons.Filled.Dns) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = ip, onValueChange = { ip = it },
                        label = { Text("آدرس IP سرور") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = httpPort, onValueChange = { httpPort = it.filter(Char::isDigit) },
                            label = { Text("پورت وب‌سرویس") },
                            singleLine = true, colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dbPort, onValueChange = { dbPort = it.filter(Char::isDigit) },
                            label = { Text("پورت SQL Server") },
                            singleLine = true, colors = fieldColors,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = apiPath, onValueChange = { apiPath = it },
                        label = { Text("مسیر API (مثال: vizitor)") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = apiKey, onValueChange = { apiKey = it },
                        label = { Text("کلید API") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // ── کلید اتصال امن: خاموش برای شبکهٔ داخلی (سرور HTTP) ────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "اتصال امن (HTTPS)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                if (useHttps) "برای دامنه با گواهی معتبر؛ سرور باید روی HTTPS باشد"
                                else "برای شبکهٔ داخلی شرکت؛ نصب پیش‌فرض سرور روی HTTP است",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Switch(
                            checked = useHttps,
                            onCheckedChange = { useHttps = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonGreen)
                        )
                    }

                    OutlinedTextField(
                        value = workerUrl, onValueChange = { workerUrl = it },
                        label = { Text("آدرس پراکسی هوش مصنوعی (Cloudflare Worker)") },
                        singleLine = true, colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // ── پیش‌نمایش زندهٔ آدرس کامل وب‌سرویس ──────────────────────
                    val previewUrl = remember(ip, httpPort, apiPath, useHttps) {
                        val path = apiPath.trim().trim('/')
                        (if (useHttps) "https" else "http") +
                                "://${ip.trim().ifBlank { "…" }}:${httpPort.ifBlank { "…" }}/" +
                                (if (path.isEmpty()) "" else "$path/") + "index.php"
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Storage, contentDescription = null,
                            tint = palette.gold, modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            previewUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row {
                        NeonPurpleButton(
                            text = "ذخیره پیکربندی",
                            onClick = {
                                viewModel.saveConfig(
                                    config.copy(
                                        serverIp = ip.trim(),
                                        httpPort = httpPort.toIntOrNull() ?: 8731,
                                        dbPort = dbPort.toIntOrNull() ?: 1433,
                                        apiPath = apiPath.trim(),
                                        apiKey = apiKey.trim(),
                                        useHttps = useHttps,
                                        workerUrl = workerUrl.trim()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        NeonGreenButton(
                            text = if (testing) "در حال تست…" else "تست سلامت اتصال",
                            icon = Icons.Filled.MonitorHeart,
                            enabled = !testing,
                            onClick = { viewModel.testConnection() }
                        )
                    }

                    // ── بازگشت یک‌مرحله‌ای به تنظیمات پیش‌فرض سرور میلانو ─────
                    TextButton(
                        onClick = {
                            ip = ServerConfig().serverIp
                            httpPort = ServerConfig().httpPort.toString()
                            dbPort = ServerConfig().dbPort.toString()
                            apiPath = ServerConfig().apiPath
                            apiKey = ServerConfig().apiKey
                            useHttps = ServerConfig().useHttps
                            viewModel.saveConfig(
                                config.copy(
                                    serverIp = ip, httpPort = httpPort.toInt(),
                                    dbPort = dbPort.toInt(), apiPath = apiPath, apiKey = apiKey,
                                    useHttps = useHttps
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.RestartAlt, contentDescription = null,
                            tint = palette.gold, modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "بازگشت به پیش‌فرض امن HTTPS (api.vizitor.local:443)",
                            color = palette.gold
                        )
                    }
                }
            }
        }

        // ── کارت وضعیت سلامت سرور ───────────────────────────────────────────
        item { HealthStatusCard(health = health, testing = testing) }

        // ── مدیریت همگام‌سازی ───────────────────────────────────────────────
        item { SectionTitle(text = "مدیریت همگام‌سازی", icon = Icons.Filled.CloudSync) }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("همگام‌سازی خودکار", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "سینک خودکار فاکتورها و کاتالوگ پس از اتصال به شبکه",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = config.autoSync,
                            onCheckedChange = { viewModel.saveConfig(config.copy(autoSync = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = NeonGreen)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (config.lastSyncAt > 0)
                            "آخرین همگام‌سازی: ${config.lastSyncAt.toFaDate()} — ${config.lastSyncAt.toFaTime()}"
                        else "هنوز همگام‌سازی انجام نشده است",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    NeonGreenButton(
                        text = if (syncing) "در حال همگام‌سازی…" else "همگام‌سازی اکنون",
                        icon = Icons.Filled.Sync,
                        enabled = !syncing,
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (syncing) {
                        Spacer(Modifier.height(10.dp))
                        CircularProgressIndicator(
                            color = NeonGreen,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                    }

                    // ── نتیجهٔ ماندگار آخرین سینک ──────────────────────────────
                    val lastReport = syncReport
                    if (lastReport != null && !syncing) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (lastReport.errors.isEmpty()) Icons.Filled.CloudDone
                                else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = if (lastReport.errors.isEmpty()) palette.accent else palette.danger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                lastReport.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lastReport.errors.isEmpty()) palette.accent else palette.danger
                            )
                        }
                        lastReport.errors.take(3).forEach { err ->
                            Text(
                                "• $err",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.danger
                            )
                        }
                        if (lastReport.errors.size > 3) {
                            Text(
                                "و ${(lastReport.errors.size - 3).toFaNumber()} خطای دیگر…",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }
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

// ═══════════════════ چیپ انتخاب روش اتصال (اینترنتی / داخلی) ═══════════════════
@Composable
private fun ModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) NeonPurple.copy(alpha = 0.22f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        border = if (selected) BorderStroke(1.dp, NeonPurple) else null,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) NeonPurple else vizitorPalette.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = if (selected) NeonPurple else vizitorPalette.textSecondary
            )
        }
    }
}
