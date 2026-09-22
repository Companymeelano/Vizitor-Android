/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | بخش «تنظیمات ورود / اتصال به سرور آتیران»
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  بازطراحی لاکچری (نسخهٔ ۲٫۱۳٫۷) روی همان منطق کارآمد:
 *    ▸ گام ۱ کارت اتصال نصب‌کننده (کارت متنی یا اسکن QR)
 *    ▸ گام ۲ سرور، پورت ۱۴۳۳، دیتابیس و کاربر محدود دیتابیس
 *    ▸ گام ۳ ورود ویزیتور با جدول واقعی dbo.sys_users + «به‌خاطر سپردن»
 *    ▸ کلیدهای سه‌بعدی با عمق واقعی، فیلدهای بزرگ و خوش‌لمس، نشان شمارهٔ طلایی
 *    ▸ چیدمان واکنشی برای همهٔ گوشی‌ها (کوچک/بزرگ/تبلت) + حاشیهٔ امن نوار سیستم
 *  امنیت: رمزها فقط رمزنگاری‌شده (AES-GCM + Android Keystore) ذخیره می‌شوند و
 *  هرگز در متن، لاگ یا گزارش چاپ نمی‌شوند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsEthernet
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.sqldirect.DirectSqlViewModel
import ir.atiran.vizitor.sqldirect.DirectUiState
import ir.atiran.vizitor.sqldirect.ServerSession
import ir.atiran.vizitor.sqldirect.VizitorSession
import ir.atiran.vizitor.ui.components.BtnTone
import ir.atiran.vizitor.ui.components.GlowChip
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.components.GoldDivider
import ir.atiran.vizitor.ui.components.GoldFlourish
import ir.atiran.vizitor.ui.components.LuxBanner
import ir.atiran.vizitor.ui.components.LuxChip
import ir.atiran.vizitor.ui.components.LuxTone
import ir.atiran.vizitor.ui.components.StepRail
import ir.atiran.vizitor.ui.components.glassRelief
import ir.atiran.vizitor.ui.components.luxFrame
import ir.atiran.vizitor.ui.components.ornaments
import ir.atiran.vizitor.ui.components.shimmerSweep
import ir.atiran.vizitor.ui.components.GradientTitle
import ir.atiran.vizitor.ui.components.IconOrb3D
import ir.atiran.vizitor.ui.components.KeyRow
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.OrbIconButton
import ir.atiran.vizitor.ui.components.PremiumButton
import ir.atiran.vizitor.ui.components.PremiumField
import ir.atiran.vizitor.ui.components.PremiumPanel
import ir.atiran.vizitor.ui.components.ScreenFit
import ir.atiran.vizitor.ui.components.StatPill
import ir.atiran.vizitor.ui.components.ToggleRow
import ir.atiran.vizitor.ui.components.rememberScreenFit
import ir.atiran.vizitor.ui.components.screenSafePadding
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber

@Composable
fun DirectSqlScreen(
    viewModel: DirectSqlViewModel,
    onBack: () -> Unit,
    onEnterPanel: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val session by VizitorSession.state.collectAsState()
    val p = vizitorPalette
    val fit = rememberScreenFit()

    Box(Modifier.fillMaxSize().dashboardBackdrop()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .screenSafePadding(),
            contentPadding = PaddingValues(
                start = fit.pad, end = fit.pad,
                top = if (fit.short) 8.dp else 14.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (fit.short) 10.dp else 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // روی گوشی بزرگ/تبلت، محتوا وسط‌چین و عرض‌محدود می‌شود
            val contentMod = if (fit.contentMax > 0.dp) Modifier.fillMaxWidth().widthIn(max = fit.contentMax)
            else Modifier.fillMaxWidth()

            // ── نوار بالا: بازگشت سه‌بعدی + تیتر گرادیانی ──────────────────
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = contentMod
                ) {
                    OrbIconButton(
                        icon = Icons.Filled.ArrowBack,
                        onClick = onBack,
                        size = 44.dp,
                        contentDescription = "بازگشت"
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        GradientTitle(
                            text = "اتصال به سرور آتیران",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = if (fit.narrow) 16.sp else 18.sp
                            ),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            "پورت ۱۴۳۳ — اتصال مستقیم، بدون IIS و بدون سرویس میانی",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (fit.narrow) 9.5.sp else 10.5.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.busy || session.syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.4.dp,
                            color = p.gold
                        )
                    } else {
                        IconOrb3D(
                            icon = Icons.Filled.Shield,
                            size = 40.dp,
                            tint = p.gold,
                            glowColor = p.accent
                        )
                    }
                }
            }

            // ── ریل گام‌ها: کارت اتصال ← سرور/دیتابیس ← ورود ویزیتور ────────
            item {
                val step = when {
                    state.loggedIn -> 2
                    session.configured -> 1
                    else -> 0
                }
                Column(
                    modifier = contentMod
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(p.surface.copy(alpha = 0.55f), p.background.copy(alpha = 0.30f))
                            )
                        )
                        .shimmerSweep(color = p.goldHighlight.copy(alpha = 0.07f), periodMillis = 3600)
                        .luxFrame(RoundedCornerShape(20.dp), p.gold, 0.48f)
                        .ornaments(color = p.gold, alpha = 0.34f, inset = 7.dp, len = 11.dp)
                        .glassRelief(RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = if (fit.dense) 9.dp else 12.dp)
                ) {
                    StepRail(
                        current = step,
                        steps = listOf("کارت اتصال", "سرور و دیتابیس", "ورود ویزیتور"),
                        labelSize = fit.microText,
                        nodeSize = if (fit.dense) 27.dp else 30.dp
                    )
                }
            }

            // ── کارت خلاصهٔ وضعیت (نمای اصلی و کوتاه) ───────────────────────
            item { StatusSummary(state, session, viewModel, onEnterPanel, fit, contentMod) }

            // ── گام‌های بازشو (پیش‌فرض بسته — صفحه شلوغ نمی‌شود) ─────────────
            item { ConnectionCard(state, viewModel, fit, contentMod) }
            item { ServerCard(state, viewModel, fit, contentMod) }
            item { LoginCard(state, viewModel, onEnterPanel, fit, contentMod) }

            // ── سلامت مسیر پیش‌فاکتور (اگر خوانده شده باشد) ─────────────────
            if (state.health.isNotEmpty()) {
                item { HealthPanel(state, fit, contentMod) }
            }

            // ── جزئیات ویزیتورها و ستون‌ها (بازشوی اختیاری) ──────────────────
            if (state.visitors.isNotEmpty() || state.columns.isNotEmpty()) {
                item { DetailsCard(state, viewModel, fit, contentMod) }
            }

            // ── یادآوری امنیت + امضای برند ──────────────────────────────────
            item {
                Column(contentMod) {
                    GoldFlourish(height = if (fit.dense) 14.dp else 18.dp)
                    Spacer(Modifier.height(if (fit.dense) 8.dp else 11.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LuxChip(
                            "اتصال مستقیم SQL — پورت ۱۴۳۳",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Dns,
                            textSize = fit.microText
                        )
                        LuxChip(
                            "ذخیرهٔ امن رمزها",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Lock,
                            tint = p.accent,
                            textSize = fit.microText
                        )
                    }
                    Spacer(Modifier.height(if (fit.dense) 8.dp else 10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconOrb3D(icon = Icons.Filled.Lock, size = 32.dp, cornerRadius = 11.dp)
                        Spacer(Modifier.width(9.dp))
                        Text(
                            "رمزها فقط رمزنگاری‌شده (AES-GCM + Android Keystore) روی همین گوشی می‌مانند " +
                                "و هیچ‌گاه در متن یا گزارش چاپ نمی‌شوند.",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (fit.narrow) 10.sp else 10.5.sp,
                                lineHeight = 17.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    MilanoFooter()
                }
            }
        }
    }
}

// ═══════════════════════ کارت خلاصهٔ وضعیت ═══════════════════════

@Composable
private fun StatusSummary(
    state: DirectUiState,
    session: ServerSession,
    viewModel: DirectSqlViewModel,
    onEnterPanel: () -> Unit,
    fit: ScreenFit,
    modifier: Modifier
) {
    val p = vizitorPalette
    val statusColor = when {
        state.loggedIn -> p.accent
        state.connected -> p.gold
        session.configured -> p.primary
        else -> p.danger
    }
    val statusText = when {
        state.loggedIn -> "وارد شده"
        state.connected -> "وصل به دیتابیس"
        session.configured -> "تنظیم‌شده"
        else -> "تنظیم نشده"
    }

    PremiumPanel(
        title = if (state.loggedIn) state.loggedInName.ifBlank { state.loggedInUser } else "وضعیت اتصال",
        hint = if (state.loggedIn) "کاربر سامانهٔ آتیران — همهٔ سرویس‌ها فعال" else "برای کار با سرویس‌ها ابتدا وصل شوید",
        icon = if (state.loggedIn) Icons.Filled.Verified else Icons.Filled.Dns,
        accent = statusColor,
        inner = fit.inner,
        modifier = modifier
            .shimmerSweep(color = p.goldHighlight.copy(alpha = 0.07f), periodMillis = 3200)
            .ornaments(color = p.gold, alpha = 0.30f, inset = 8.dp, len = 12.dp),
        trailing = { GlowChip(text = statusText, color = statusColor, pulse = state.busy || session.syncing) }
    ) {
        KeyRow(label = "سرور", value = session.serverLabel, icon = Icons.Filled.Dns)
        Spacer(Modifier.height(5.dp))
        KeyRow(label = "دیتابیس", value = state.database, icon = Icons.Filled.Storage)
        Spacer(Modifier.height(5.dp))
        KeyRow(label = "کاربر سامانه", value = state.loggedInUser.ifBlank { "وارد نشده" }, icon = Icons.Filled.Person)
        Spacer(Modifier.height(5.dp))
        KeyRow(
            label = "آخرین همگام‌سازی",
            value = if (session.lastSyncAt == 0L) "انجام نشده" else session.lastSyncSummary,
            icon = Icons.Filled.Sync
        )

        if (state.loggedIn) {
            Spacer(Modifier.height(11.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatPill(session.productsCount, "کالا", Modifier.weight(1f), Icons.Filled.Storage, p.gold)
                StatPill(session.customersCount, "مشتری", Modifier.weight(1f), Icons.Filled.Person, p.primary)
                StatPill(session.invoicesCount, "فاکتور", Modifier.weight(1f), Icons.Filled.Verified, p.accent)
            }
        }

        Spacer(Modifier.height(14.dp))

        // کلید اصلی هوشمند + کلید همگام‌سازی
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!state.loggedIn) {
                PremiumButton(
                    text = if (session.configured) "ورود سریع" else "تنظیم اتصال",
                    icon = Icons.Filled.Login,
                    tone = if (session.configured) BtnTone.GOLD else BtnTone.PRIMARY,
                    height = fit.buttonHeight,
                    textSize = fit.buttonText,
                    enabled = !state.busy,
                    loading = state.busy,
                    onClick = { if (session.configured) viewModel.quickEnter() else viewModel.toggleSettings() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                PremiumButton(
                    text = "ورود به پنل",
                    icon = Icons.Filled.ExitToApp,
                    tone = BtnTone.PRIMARY,
                    height = fit.buttonHeight,
                    textSize = fit.buttonText,
                    onClick = onEnterPanel,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(9.dp))
            PremiumButton(
                text = if (fit.narrow) "همگام" else "همگام‌سازی",
                icon = Icons.Filled.Sync,
                tone = BtnTone.SUCCESS,
                height = fit.buttonHeight,
                textSize = fit.buttonText,
                enabled = state.loggedIn && !state.busy,
                loading = session.syncing,
                onClick = { viewModel.syncNow() },
                modifier = Modifier.weight(1f)
            )
        }

        if (state.status.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            LuxBanner(
                tone = when (state.statusKind) {
                    1 -> LuxTone.SUCCESS
                    2 -> LuxTone.DANGER
                    else -> LuxTone.INFO
                },
                title = when (state.statusKind) {
                    1 -> "انجام شد"
                    2 -> "نیازمند رسیدگی"
                    else -> "اطلاع"
                },
                message = state.status,
                compact = fit.dense
            )
        }
    }
}

// ═══════════════════════ گام ۱: کارت اتصال نصب‌کننده ═══════════════════════

@Composable
private fun ConnectionCard(
    state: DirectUiState,
    viewModel: DirectSqlViewModel,
    fit: ScreenFit,
    modifier: Modifier
) {
    val p = vizitorPalette
    PremiumPanel(
        title = "کارت اتصال نصب‌کننده",
        hint = "از فایل setup\\android-connect.txt یا اسکن QR",
        icon = Icons.Filled.QrCodeScanner,
        step = 1,
        accent = p.primary,
        inner = fit.inner,
        modifier = modifier,
        onHeaderClick = viewModel::toggleCard,
        expanded = state.cardExpanded
    ) {
        AnimatedVisibility(visible = state.cardExpanded) {
            Column {
                Text(
                    "متن کارت را بچسبانید یا محتوای android-connect.json را وارد کنید؛ نشانی سرور، " +
                        "پورت ۱۴۳۳، نام دیتابیس و نام کاربر محدود خودکار پر می‌شود (رمز در کارت نیست).",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = p.textSecondary
                )
                Spacer(Modifier.height(10.dp))
                PremiumField(
                    value = state.cardText,
                    onValueChange = viewModel::onCardText,
                    label = "متن کارت اتصال",
                    hint = "vizitor://c?h=…&p=1433&d=…&u=…",
                    icon = Icons.Filled.QrCodeScanner,
                    singleLine = false,
                    minHeight = 96.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                )
                Spacer(Modifier.height(11.dp))
                PremiumButton(
                    text = "خواندن کارت اتصال",
                    icon = Icons.Filled.QrCodeScanner,
                    tone = BtnTone.SUCCESS,
                    height = fit.buttonHeight,
                    textSize = fit.buttonText,
                    onClick = { viewModel.applyCard() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ═══════════════════════ گام ۲: سرور، دیتابیس و کاربر محدود ═══════════════════════

@Composable
private fun ServerCard(
    state: DirectUiState,
    viewModel: DirectSqlViewModel,
    fit: ScreenFit,
    modifier: Modifier
) {
    val p = vizitorPalette
    var showDbPass by remember { mutableStateOf(false) }
    var dbMenu by remember { mutableStateOf(false) }

    PremiumPanel(
        title = "سرور، دیتابیس و کاربر محدود",
        hint = if (state.database.isBlank()) "پورت ۱۴۳۳ و کاربر محدود دیتابیس"
        else "${state.database} — ${state.dbUser.ifBlank { "بدون کاربر" }}",
        icon = Icons.Filled.Dns,
        step = 2,
        accent = p.gold,
        inner = fit.inner,
        modifier = modifier,
        onHeaderClick = viewModel::toggleSettings,
        expanded = state.settingsExpanded
    ) {
        AnimatedVisibility(visible = state.settingsExpanded) {
            Column {
                // ── آدرس سرور + پورت ──
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

                // ── نام دیتابیس (تایپ دستی یا انتخاب از فهرست) ──
                Row(verticalAlignment = Alignment.Bottom) {
                    PremiumField(
                        value = state.database,
                        onValueChange = viewModel::onDatabase,
                        label = "نام دیتابیس (قابل تایپ دستی)",
                        hint = "ATIRAN",
                        icon = Icons.Filled.Storage,
                        minHeight = fit.fieldHeight,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.databases.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        Box {
                            OrbIconButton(
                                icon = Icons.Filled.ArrowDropDown,
                                onClick = { dbMenu = true },
                                size = 46.dp,
                                contentDescription = "فهرست دیتابیس‌ها"
                            )
                            DropdownMenu(expanded = dbMenu, onDismissRequest = { dbMenu = false }) {
                                state.databases.forEach { db ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                db,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = p.textPrimary
                                            )
                                        },
                                        onClick = { viewModel.onDatabase(db); dbMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── کاربر محدود دیتابیس ──
                Text(
                    "کاربر محدود دیتابیس — همان که نصب‌کننده ساخت (نام کاربری از کارت پر می‌شود)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 17.sp),
                    color = p.textSecondary
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
                    hint = "رمز در C:\\ProgramData\\Vizitor\\android_app_password.txt",
                    icon = Icons.Filled.Key,
                    minHeight = fit.fieldHeight,
                    visualTransformation = if (showDbPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { showDbPass = !showDbPass }) {
                            Icon(
                                if (showDbPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
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

// ═══════════════════════ گام ۳: ورود ویزیتور ═══════════════════════

@Composable
private fun LoginCard(
    state: DirectUiState,
    viewModel: DirectSqlViewModel,
    onEnterPanel: () -> Unit,
    fit: ScreenFit,
    modifier: Modifier
) {
    val p = vizitorPalette
    var showPass by remember { mutableStateOf(false) }

    PremiumPanel(
        title = "ورود ویزیتور",
        hint = if (state.loggedIn) "وارد شده: ${state.loggedInUser}" else "با حساب خودتان در سامانهٔ آتیران",
        icon = Icons.Filled.Person,
        step = 3,
        accent = if (state.loggedIn) p.accent else p.primary,
        inner = fit.inner,
        modifier = modifier,
        onHeaderClick = viewModel::toggleLogin,
        expanded = state.loginExpanded || !state.loggedIn,
        trailing = {
            if (state.loggedIn) {
                GlowChip(text = "فعال", color = p.accent)
            }
        }
    ) {
        AnimatedVisibility(visible = state.loginExpanded || !state.loggedIn) {
            Column {
                Text(
                    "همان نام کاربری و کلمهٔ عبوری که با آن به سامانه وارد می‌شوید (جدول واقعی dbo.sys_users).",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = p.textSecondary
                )
                Spacer(Modifier.height(10.dp))
                PremiumField(
                    value = state.erpUser,
                    onValueChange = viewModel::onErpUser,
                    label = "نام کاربری شما در سامانه",
                    hint = "مثال: m.yaghoobi",
                    icon = Icons.Filled.Person,
                    minHeight = fit.fieldHeight,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                PremiumField(
                    value = state.erpPassword,
                    onValueChange = viewModel::onErpPassword,
                    label = "کلمهٔ عبور شما",
                    hint = "رمز شما در سامانهٔ آتیران",
                    icon = Icons.Filled.Key,
                    minHeight = fit.fieldHeight,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "نمایش/پنهان رمز",
                                tint = p.gold
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                ToggleRow(
                    label = "به‌خاطر سپردن",
                    hint = "ورود سریع در اجرای بعدی — بدون تایپ مجدد",
                    checked = state.rememberMe,
                    onCheckedChange = viewModel::onRememberMe,
                    icon = Icons.Filled.Key
                )

                Spacer(Modifier.height(14.dp))
                PremiumButton(
                    text = if (state.loggedIn) "ورود به پنل" else "ورود و همگام‌سازی",
                    subtitle = if (state.loggedIn) null else "اتصال + ورود + دریافت کالا، مشتری و فاکتور",
                    icon = Icons.Filled.Login,
                    tone = BtnTone.PRIMARY,
                    height = fit.buttonHeight,
                    textSize = fit.buttonText,
                    enabled = !state.busy,
                    loading = state.busy,
                    onClick = { if (state.loggedIn) onEnterPanel() else viewModel.loginAndLoad() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (state.loggedIn) {
                        PremiumButton(
                            text = "به‌روزرسانی ویزیتورها و ستون‌ها",
                            icon = Icons.Filled.Refresh,
                            tone = BtnTone.GLASS,
                            height = 48.dp,
                            textSize = 12.5.sp,
                            onClick = { viewModel.refreshVisitors() },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(9.dp))
                        PremiumButton(
                            text = "خروج",
                            icon = Icons.Filled.ExitToApp,
                            tone = BtnTone.GLASS,
                            height = 48.dp,
                            textSize = 12.5.sp,
                            onClick = { viewModel.logout() },
                            modifier = Modifier.width(118.dp)
                        )
                    } else {
                        PremiumButton(
                            text = "اتصال دیتابیس",
                            icon = Icons.Filled.Storage,
                            tone = BtnTone.GLASS,
                            height = 48.dp,
                            textSize = 12.5.sp,
                            enabled = !state.busy,
                            onClick = { viewModel.connectDatabase() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════ سلامت مسیر پیش‌فاکتور ═══════════════════════

@Composable
private fun HealthPanel(state: DirectUiState, fit: ScreenFit, modifier: Modifier) {
    val p = vizitorPalette
    PremiumPanel(
        title = "بررسی سلامت مسیر پیش‌فاکتور",
        hint = "کنترل آماده بودن جدول‌ها و روال‌های سامانه",
        icon = Icons.Filled.CheckCircle,
        accent = p.accent,
        inner = fit.inner,
        modifier = modifier
    ) {
        state.health.forEach { (key, ok) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (ok) p.accent else p.danger,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    key,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = p.textSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (ok) "سالم" else "بررسی شود",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                    color = if (ok) p.accent else p.danger
                )
            }
        }
    }
}

// ═══════════════════════ جزئیات (ویزیتورها و ستون‌ها) ═══════════════════════

@Composable
private fun DetailsCard(
    state: DirectUiState,
    viewModel: DirectSqlViewModel,
    fit: ScreenFit,
    modifier: Modifier
) {
    val p = vizitorPalette
    PremiumPanel(
        title = "جزئیات دادهٔ ویزیتورها",
        hint = "${state.visitorsOfUser.toFaNumber()} ویزیتور • ${state.columns.size.toFaNumber()} ستون از dbo.visitors",
        icon = Icons.Filled.Storage,
        accent = p.primary,
        inner = fit.inner,
        modifier = modifier,
        onHeaderClick = viewModel::toggleDetails,
        expanded = state.detailsExpanded
    ) {
        AnimatedVisibility(visible = state.detailsExpanded) {
            Column {
                state.visitors.take(50).forEach { v ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                v.name.ifBlank { "ویزیتور #${v.rdf}" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = p.textPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            GlowChip(
                                text = if (v.active.equals("t", true)) "فعال" else "غیرفعال",
                                color = if (v.active.equals("t", true)) p.accent else p.danger
                            )
                        }
                        val line = listOfNotNull(
                            "کد ${v.rdf}",
                            v.cell.takeIf { it.isNotBlank() }?.let { "موبایل $it" },
                            v.credit?.let { "اعتبار ${it.toFaNumber()}" },
                            v.allowedInvoicesLeft?.let { "فاکتور مجاز $it" }
                        ).joinToString(" • ")
                        if (line.isNotBlank()) {
                            Text(
                                line,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = p.textSecondary
                            )
                        }
                        Text(
                            "دسترسی: ${v.allowedCustomers} مشتری • ${v.allowedProducts} کالا • ${v.allowedWarehouses} انبار",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = p.textSecondary
                        )
                    }
                }
                if (state.columns.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    GoldDivider(label = "ستون‌های dbo.visitors")
                    Spacer(Modifier.height(8.dp))
                    state.columns.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 1.5.dp)) {
                            Text(
                                c.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = p.textSecondary
                            )
                            Text(
                                c.typeName + if (c.maxLength > 0) "(${c.maxLength})" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = p.textSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

