/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۱: پیشخوان من (Smart Dashboard) v2.2.0
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  بازطراحی فشرده و هوشمند «در یک نگاه»:
 *    ▸ کارت «نبض امروز»: دونات دوحلقه (تارگت بیرونی + سینک داخلی) + ۶ چیپ آمار
 *    ▸ کارت «ریسک و فرصت»: بدهی‌های بزرگ و پرفروش‌ترین‌ها کنار هم در یک کارت
 *    ▸ کارت هفتگی فشرده: چارت سه‌بعدی خلوت‌تر + ۳ چیپ خلاق (جمع/بهترین/میانگین)
 *    ▸ سوییچر تم زنده در گوشه بالای صفحه (نقاط رنگی ۵ پالت، با چک‌مارک طلایی)
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import ir.atiran.vizitor.data.local.ChequeStore
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.data.local.InvoiceStatus
import ir.atiran.vizitor.data.local.TopProduct
import ir.atiran.vizitor.ui.components.DepthBar
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.RankBadge3D
import ir.atiran.vizitor.ui.components.RoyalBarChart
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.StatusDot
import ir.atiran.vizitor.ui.components.TwinDonutChart
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.theme.AllPalettes
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextPrimary
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.ThemeManager
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaTime
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

/** گرادیان هشدار بدهی — سرخ تم با هایلایت ملایم. */
private val DebtBarColors = listOf(Color(0xFFFF4D6D), Color(0xFFFF8FA3))

@Composable
fun DashboardScreen(
    viewModel: VizitorViewModel,
    onOpenChat: () -> Unit = {},
    onOpenVisits: () -> Unit = {}
) {
    val todaySales by viewModel.todaySales.collectAsState()
    val followUp by viewModel.followUpCustomers.collectAsState()
    val pending by viewModel.pendingCount.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cheques by ChequeStore.items.collectAsState()
    val appContext = LocalContext.current
    LaunchedEffect(customers) {
        if (customers.isNotEmpty()) ChequeStore.seedIfEmpty(appContext, customers)
    }

    val weekly = remember(invoices, todaySales) { buildWeekly(invoices, todaySales) }
    val debtors = remember(customers) {
        customers.filter { it.debt > 0 }.sortedByDescending { it.debt }.take(3)
    }
    val sentToday = remember(invoices) { countSentToday(invoices) }
    // آخرین زمان همگام‌سازی با سرور آتیران (جدیدترین فاکتور SYNCED)
    val lastSyncAt = remember(invoices) {
        invoices.filter { it.status == InvoiceStatus.SYNCED }.maxOfOrNull { it.createdAt }
    }

    val target = viewModel.dailyTarget
    val progress = if (target > 0) (todaySales.toFloat() / target).coerceIn(0f, 1f) else 0f
    val syncRatio =
        if (pending + sentToday > 0) sentToday.toFloat() / (pending + sentToday).toFloat() else 1f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── سرصفحه + سوییچر تم (گوشه بالا، کنار عنوان) ─────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // پنل کاربری ویزیتور — آواتار بادام در قاب مدور گرادیانی تم 🧔🏻🥜
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    VisitorAvatarHero()
                }
                Spacer(Modifier.width(10.dp))
                ThemeDotsSwitch()
            }
        }

        // ── دسترسی سریع (v2.14.0) — ثبت ویزیت + گفتگوی ویزیتورها ───────────
        item {
            QuickAccessCard(
                onVisit = onOpenVisits,
                onChat = onOpenChat
            )
        }

        // ── ۱) نبض امروز — دونات دوحلقه (تارگت بیرونی + سینک داخلی) ────────
        item { PulseCard(target, todaySales, progress, pending, sentToday, syncRatio, lastSyncAt) }

        // ── ۲) ریسک بدهی — فقط مشتریان بدهکار (تک‌ستونه و واضح) ────────────
        item { RiskAndWinCard(debtors) }

        // ── ۳) چارت پیگیری چک‌های مشتریان (ثبت‌نشده) ────────────────────────
        item {
            ChequeChartCard(
                cheques = cheques,
                onCall = { phone ->
                    appContext.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    )
                },
                onResolve = { ch ->
                    ChequeStore.resolve(appContext, ch.serial)
                    viewModel.showToast("چک ${ch.serial.toFaDigits()} به‌عنوان «ثبت‌شده» برداشته شد ✅")
                }
            )
        }

        // ── ۴) چارت «مشتریان خرید نکرده» — دقیقاً زیر آن ────────────────────
        item {
            FollowUpChartCard(
                followUp = followUp,
                onPick = { viewModel.selectCustomer(it) }
            )
        }

        // ── ۴) فروش هفتگی — نمودار سه‌بعدی فشرده + چیپ‌های هوشمند ──────────
        item { WeeklyCard(weekly) }

        item { MilanoFooter() }
    }
}

// ═════════════════════════ سوییچر تم زنده ═════════════════════════

/**
 * نقاط رنگی ۵ پالت — هر نقطه از رنگ جادویی خودِ آن تم ساخته شده
 * (گرادیان primary → gold آن تم) و با انتخاب، قاب طلایی و چک‌مارک می‌گیرد.
 * نشسته در گوشه بالای پیشخوان تا با یک لمس، کل برنامه بازرنگ شود.
 */
@Composable
private fun ThemeDotsSwitch() {
    val ctx = LocalContext.current
    val themeId by ThemeManager.themeId.collectAsState()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Filled.Palette,
            contentDescription = "انتخاب تم",
            tint = Gold,
            modifier = Modifier.size(14.dp)
        )
        // ۷) پیش‌نمایش نام تم انتخابی — برچسب زنده کنار نقاط رنگی
        AllPalettes.firstOrNull { it.id == themeId }?.let { cur ->
            Text(
                cur.displayName,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Gold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AllPalettes.forEach { pl ->
            val selected = pl.id == themeId
            Box(
                modifier = Modifier
                    .size(if (selected) 26.dp else 21.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(pl.primary, pl.gold)))
                    .then(
                        if (selected) Modifier.border(2.dp, vizitorPalette.goldHighlight, CircleShape)
                        else Modifier.border(1.dp, Color(0x3DFFFFFF), CircleShape)
                    )
                    .clickable { ThemeManager.setTheme(ctx, pl.id) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = pl.displayName,
                        tint = pl.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

/**
 * کارت «دسترسی سریع» (v2.14.0) — دو کاشی بزرگ:
 *   «ثبت ویزیت» (تب ویزیت — جدول dbo.Visit) و «گفتگوی ویزیتورها».
 */
@Composable
private fun QuickAccessCard(
    onVisit: () -> Unit,
    onChat: () -> Unit
) {
    val p = vizitorPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(p.surface.copy(alpha = 0.85f))
            .border(1.dp, p.glassBorder, RoundedCornerShape(18.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickTile(
            title = "ثبت ویزیت",
            subtitle = "مراجعه به مشتری + GPS",
            icon = Icons.Filled.Flag,
            modifier = Modifier.weight(1f),
            onClick = onVisit
        )
        QuickTile(
            title = "گفتگو",
            subtitle = "اتاق ویزیتورها",
            icon = Icons.Filled.Message,
            modifier = Modifier.weight(1f),
            onClick = onChat
        )
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = vizitorPalette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(p.btnPrimaryTop.copy(alpha = 0.55f), p.surfaceDeep.copy(alpha = 0.8f))
                )
            )
            .border(1.dp, p.gold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(p.btnPrimaryTop, p.btnPrimaryBottom))),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(
            title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            fontSize = 9.5.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** پنل هویت ویزیتور — بادام با کت‌وشلوار طلایی در قاب مدور گرادیانی تم. */
@Composable
private fun VisitorAvatarHero() {
    val p = vizitorPalette
    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .border(
                2.dp,
                Brush.linearGradient(listOf(p.gold, p.primary)),
                CircleShape
            )
            .background(p.primary.copy(alpha = 0.10f), CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(ir.atiran.vizitor.R.drawable.nut_visitor),
            contentDescription = "آواتار ویزیتور",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
        )
    }
}

// ═════════════════════════ کارت نبض امروز ═════════════════════════

/** دونات دوحلقه (تارگت بیرونی + سینک داخلی) + ۶ چیپ آماری فشرده. */
@Composable
private fun PulseCard(
    target: Long,
    todaySales: Long,
    progress: Float,
    pending: Int,
    sentToday: Int,
    syncRatio: Float,
    lastSyncAt: Long?
) {
    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "نبض امروز در یک نگاه", icon = Icons.Filled.Flag)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TwinDonutChart(
                    outerProgress = progress,
                    innerProgress = syncRatio,
                    centerValue = "${(progress * 100).toInt()}٪".toFaDigits(),
                    centerLabel = "تارگت",
                    size = 122.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    // دونات بیرونی: تارگت — دونات داخلی: سینک
                    LegendLine(NeonGreen, "حلقه بیرونی: پیشرفت تارگت")
                    Spacer(Modifier.height(4.dp))
                    LegendLine(Gold, "حلقه داخلی: همگام‌سازی فاکتورها")
                    Spacer(Modifier.height(8.dp))
                    // آخرین همگام‌سازی — هوشمند و فوق‌فشرده (بدون تغییر آرايش کارت)
                    Text(
                        if (lastSyncAt != null) "آخرین سینک: ${lastSyncAt.toFaTime()}"
                        else "هنوز سینکی انجام نشده",
                        fontSize = 9.sp,
                        color = NeonPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        if (progress >= 1f) "هدف امروز محقق شد 🏆"
                        else "تا هدف: ${(target - todaySales).coerceAtLeast(0).toFaPrice()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (progress >= 1f) NeonGreen else Gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // ۶ چیپ هوشمند — همه آمار کلیدی در ۳ ردیف فشرده
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("فروش امروز", todaySales.toFaPrice(), NeonGreen, Modifier.weight(1f))
                StatChip("هدف روزانه", target.toFaPrice(), NeonPurple, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(
                    "مانده تا هدف",
                    (target - todaySales).coerceAtLeast(0).toFaPrice(),
                    Gold, Modifier.weight(1f)
                )
                StatChip(
                    "پیشرفت",
                    "${(progress * 100).toInt()}٪".toFaDigits(),
                    if (progress >= 1f) NeonGreen else NeonPurple, Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("در صف ارسال", "${pending.toFaNumber()} فاکتور", NeonPurple, Modifier.weight(1f))
                StatChip(
                    "ارسال‌شده امروز",
                    "${sentToday.toFaNumber()} فاکتور",
                    if (pending == 0) NeonGreen else Gold, Modifier.weight(1f)
                )
            }
        }
    }
}

/** خط راهنمای رنگ: نقطه رنگی + متن خیلی کوچک. */
@Composable
private fun LegendLine(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
}

/** چیپ آماری فشرده — برچسب خاکستری + مقدار رنگی؛ پایه داشبورد «در یک نگاه». */
@Composable
private fun StatChip(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═════════════════════════ کارت ریسک و فرصت ═════════════════════════

/** کارت ریسک بدهی — فقط بدهکاران بزرگ (تک‌ستونه، فشرده و واضح). */
@Composable
private fun RiskAndWinCard(debtors: List<CustomerEntity>) {
    val maxDebt = debtors.maxOfOrNull { it.debt }?.coerceAtLeast(1L) ?: 1L
    val sumDebt = debtors.sumOf { it.debt }

    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "ریسک بدهی — مشتریان بدهکار", icon = Icons.Filled.WarningAmber)
            Spacer(Modifier.height(10.dp))
            if (debtors.isEmpty()) {
                Text(
                    "مشتری بدهکاری نیست 🎉",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                debtors.forEachIndexed { i, c ->
                    MiniRankRow(
                        rank = i + 1,
                        name = c.name,
                        value = c.debt.toFaPrice(),
                        valueColor = DangerRed,
                        fraction = c.debt.toFloat() / maxDebt,
                        barColors = DebtBarColors
                    )
                    if (i < debtors.lastIndex) Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "جمع بدهی باز: ${sumDebt.toFaPrice()}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Gold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * چارت پیگیری چک‌های مشتریان — چک‌هایی که هنوز در سامانه ثبت نشده‌اند:
 * نام مشتری + سریال + بانک + مبلغ + سررسید نزدیک + نوار مهلت و تماس مستقیم.
 */
@Composable
private fun ChequeChartCard(
    cheques: List<ir.atiran.vizitor.data.local.ChequeFollowUp>,
    onCall: (String) -> Unit,
    onResolve: (ir.atiran.vizitor.data.local.ChequeFollowUp) -> Unit
) {
    // نوار سررسید: محکم با یکی از چرخه‌های رنگی پالت (ها در تیره/روشن خواناست)
    val dueColors = listOf(Gold, DangerRed)

    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "پیگیری چک‌های مشتریان (ثبت‌نشده)", icon = Icons.Filled.AccountBalanceWallet)
            Spacer(Modifier.height(10.dp))
            if (cheques.isEmpty()) {
                Text(
                    "چک پیگیری‌نشده‌ای نیست — همه ثبت شده‌اند ✅",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                cheques.forEachIndexed { i, ch ->
                    val frac = (1f - ch.dueDays / 30f).coerceIn(0.08f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RankBadge3D(rank = i + 1, size = 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                ch.customerName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "سریال ${ch.serial.toFaDigits()} • بانک ${ch.bank} • ${ch.amount.toFaPrice()}",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            DepthBar(fraction = frac, fillColors = dueColors, height = 4.dp)
                        }
                        Spacer(Modifier.width(6.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${ch.dueDays.toFaNumber()} روز",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (ch.dueDays <= 7) DangerRed else Gold,
                                maxLines = 1
                            )
                            Row {
                                // تماس مستقیم با مشتری
                                Box(
                                    Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen.copy(alpha = 0.16f))
                                        .border(1.dp, NeonGreen.copy(alpha = 0.5f), CircleShape)
                                        .clickable { onCall(ch.phone) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Call, contentDescription = "تماس", tint = NeonGreen, modifier = Modifier.size(13.dp))
                                }
                                Spacer(Modifier.width(6.dp))
                                // علامت «ثبت شد» — حذف از فهرست پیگیری
                                Box(
                                    Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Gold.copy(alpha = 0.14f))
                                        .border(1.dp, Gold.copy(alpha = 0.5f), CircleShape)
                                        .clickable { onResolve(ch) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "ثبت شد", tint = Gold, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                    if (i < cheques.lastIndex) Spacer(Modifier.height(7.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "جمع مبالغ چک‌ها: ${cheques.sumOf { it.amount }.toFaPrice()} • ${cheques.size.toFaNumber()} چک",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Gold,
                    maxLines = 1
                )
            }
        }
    }
}

/** سربرگ کوچک ستون — آیکن + متن محکم رنگی. */
@Composable
private fun MiniPanelTitle(text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ردیف فوق‌فشرده: نشان رتبه ۲۰dp + نام + نوار ٪ + مقدار. */
@Composable
private fun MiniRankRow(
    rank: Int,
    name: String,
    value: String,
    valueColor: Color,
    fraction: Float,
    barColors: List<Color>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RankBadge3D(rank = rank, size = 20.dp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            DepthBar(
                fraction = fraction,
                fillColors = barColors,
                height = 4.dp
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1
        )
    }
}

// ═════════════════════════ کارت هفتگی فشرده ═════════════════════════

/** چارت ستونی سه‌بعدی خلوت‌تر + ۳ چیپ هوشمند (جمع / بهترین روز / میانگین). */
@Composable
private fun WeeklyCard(weekly: List<Pair<String, Long>>) {
    val sum = weekly.sumOf { it.second }
    val best = weekly.maxByOrNull { it.second }
    val avg = if (weekly.isNotEmpty()) sum / weekly.size else 0L

    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "فروش هفتگی", icon = Icons.Filled.BarChart)
            Spacer(Modifier.height(8.dp))
            RoyalBarChart(
                data = weekly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("جمع هفته", sum.toFaPrice(), Gold, Modifier.weight(1f))
                StatChip(
                    "بهترین روز",
                    best?.let { "${it.first}: ${it.second.toFaPrice()}" } ?: "—",
                    NeonGreen, Modifier.weight(1f)
                )
                StatChip("میانگین روزانه", avg.toFaPrice(), NeonPurple, Modifier.weight(1f))
            }
        }
    }
}

/**
 * چارت «مشتریان خرید نکرده» (افت خرید) — منطبق بر سبک کارت بدهکاران:
 * نشان رتبه، نام با رنگ متن تم (تیره/روشن هر دو خوانا)، نوار قدرت افت و مقدار ٪.
 */
@Composable
private fun FollowUpChartCard(
    followUp: List<CustomerEntity>,
    onPick: (CustomerEntity) -> Unit
) {
    val rows = followUp.take(4)
    val avg = if (followUp.isNotEmpty()) followUp.sumOf { it.purchaseDropPercent } / followUp.size else 0
    // رنگ‌های پالت فعال — در تم‌های تیره و روشن هر دو به‌خوبی دیده می‌شوند
    val barColors = listOf(NeonPurple, Gold)

    GlassCard(modifier = Modifier.fillMaxWidth().royalBorder()) {
        Column {
            RoyalHeader(text = "مشتریان خرید نکرده (افت خرید)", icon = Icons.Filled.TrendingUp)
            Spacer(Modifier.height(10.dp))
            if (rows.isEmpty()) {
                Text(
                    "همه مشتریان در وضعیت پایدار هستند 🎉",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                rows.forEachIndexed { i, c ->
                    val drop = c.purchaseDropPercent.coerceIn(0, 100)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(c) }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        RankBadge3D(rank = i + 1, size = 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                c.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            DepthBar(fraction = (drop / 100f).coerceIn(0.06f, 1f), fillColors = barColors, height = 4.dp)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "افت ${drop.toFaNumber()}٪",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed,
                            maxLines = 1
                        )
                    }
                    if (i < rows.lastIndex) Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(8.dp))
                // جمع‌بندی چارت — خوانا در هر دو خانواده تم
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "مشتری متوقف: ${followUp.size.toFaNumber()} • میانگین افت: ${avg.toFaNumber()}٪",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Gold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═════════════════════════ توابع داده ═════════════════════════

/** ساخت داده هفتگی چارت ستونی (۷ روز اخیر؛ در نبود داده، دمو). */
private fun buildWeekly(
    invoices: List<InvoiceEntity>,
    todaySales: Long
): List<Pair<String, Long>> {
    val labels = arrayOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val out = mutableListOf<Pair<String, Long>>()
    for (back in 6 downTo 0) {
        val c = java.util.Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(java.util.Calendar.DAY_OF_YEAR, -back)
        }
        val start = (c.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = start + 86_400_000L
        var sum = invoices.filter { it.createdAt in start until end }.sumOf { it.finalAmount }
        if (sum == 0L && back == 0) sum = todaySales
        out += labels[c.get(java.util.Calendar.DAY_OF_WEEK) % 7] to sum
    }
    return if (out.all { it.second == 0L }) {
        // داده نمایشی برای پیش‌نمایش جذاب چارت
        listOf(32, 45, 28, 61, 52, 74, 40).mapIndexed { i, v -> out[i].first to v * 1_000_000L }
    } else out
}

/** شمارش فاکتورهای ارسال‌شده امروز (سینک‌شده با سرور آتیران). */
private fun countSentToday(invoices: List<InvoiceEntity>): Int {
    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    return invoices.count { it.createdAt >= todayStart && it.status == InvoiceStatus.SYNCED }
}
