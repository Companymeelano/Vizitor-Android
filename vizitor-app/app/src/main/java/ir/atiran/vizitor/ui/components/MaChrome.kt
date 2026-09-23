/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | «چهارچوب گزارش طلایی» (v2.19.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل، همان اسکلتِ تصویریِ نمونهٔ مرجع (M•A Report) را می‌سازد تا
 *  صفحهٔ «اتصال ویزیتور» و «ارسال فاکتور» عیناً مثل عکس‌های مرجع در بیایند:
 *
 *    ▸ نوار بالا: سه کرهٔ (فیلتر / هشدار با شمارنده / جست‌وجو) + عنوان طلایی + فلش
 *    ▸ نوار بخش‌ها: شش بخش افقی با نشانِ فعالِ طلایی و نقطهٔ زیرین
 *    ▸ کارت طلایی CTA: مانند «ساخت چارت» مرجع — متن راست، دکمهٔ مربع‌گرد چپ
 *    ▸ قرص‌های بخش (Segment): فعال = گرادیان طلایی با متن تیره، غیرفعال = شیشهٔ تیره
 *    ▸ نوار نازک روند با دستهٔ طلایی («نمایش … مورد اخیر» مرجع)
 *    ▸ کارت سند MEELANO REPORTS با ردیف‌های «برچسب — مقدار» و امضای برند
 *    ▸ برگهٔ «تنظیمات هوشمند»: گروه‌های بازشو + کاشی‌های رنگی Word/Excel/PDF
 *
 *  همه چیز فقط با Canvas/Compose خودِ برنامه رسم می‌شود؛ بدون کتابخانهٔ بیرونی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.atiran.vizitor.ui.theme.vizitorPalette

// ═══════════════════ ۱) کرهٔ بالا: فیلتر / هشدار / جست‌وجو ═══════════════════

/**
 * کرهٔ شیشه‌ای نوار بالا (مثل سه کرهٔ گوشهٔ چپ مرجع).
 * @param badge شمارندهٔ هشدار (۰ = بدون نشان).
 */
@Composable
fun MaOrbButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color? = null,
    badge: Int = 0,
    contentDescription: String? = null,
) {
    val p = vizitorPalette
    val c = tint ?: p.gold
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(c.copy(alpha = 0.22f), p.surfaceDeep.copy(alpha = 0.85f))
                    )
                )
                .border(1.2.dp, c.copy(alpha = 0.55f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = c,
                modifier = Modifier.size(size * 0.45f)
            )
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaRed)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    badge.toString(),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

// ═════════════ ۲) نوار بالا: عنوان طلایی + فلش بازگشت + سه کره ═════════════

/**
 * نوار بالای صفحه‌های مرجع:
 *   [فلش] [عنوان طلایی + زیرنویس] …………… [قرص وضعیت] [فیلتر] [هشدار] [جست‌وجو]
 */
@Composable
fun MaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    onFilter: (() -> Unit)? = null,
    onAlert: (() -> Unit)? = null,
    alertCount: Int = 0,
    alertTint: Color? = null,
    onSearch: (() -> Unit)? = null,
    chip: String? = null,
    chipColor: Color? = null,
) {
    val p = vizitorPalette
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = p.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        Column {
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.4.sp,
                color = p.gold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    eyebrow,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.weight(1f))
        if (!chip.isNullOrBlank()) {
            GlowChip(text = chip, color = chipColor ?: p.gold)
            Spacer(Modifier.width(6.dp))
        }
        if (onFilter != null) {
            MaOrbButton(
                icon = Icons.Filled.Settings,
                onClick = onFilter,
                contentDescription = "تنظیمات هوشمند"
            )
            Spacer(Modifier.width(6.dp))
        }
        if (onAlert != null) {
            MaOrbButton(
                icon = Icons.Filled.Warning,
                onClick = onAlert,
                tint = alertTint ?: if (alertCount > 0) MaRed else p.gold,
                badge = alertCount,
                contentDescription = "هشدارها"
            )
            Spacer(Modifier.width(6.dp))
        }
        if (onSearch != null) {
            MaOrbButton(
                icon = Icons.Filled.Search,
                onClick = onSearch,
                contentDescription = "جست‌وجو"
            )
        }
    }
}

// ═══════════════ ۳) نوار بخش‌ها (شش بخش افقی با نشان فعال) ═══════════════

/** یک بخش در نوار بالای صفحه (مانند «نمای کلی / مشتریان / کالاها …»). */
data class MaNavItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * نوار بخش‌های افقی مرجع: همه در یک ردیف، بخش فعال با کاشیِ طلایی و
 * نقطهٔ کوچک زیر برچسب.
 */
@Composable
fun MaNavStrip(
    items: List<MaNavItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    if (items.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val active = item.key == selectedKey
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelect(item.key) }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(
                            if (active)
                                Brush.verticalGradient(listOf(p.goldHighlight, p.gold, p.goldDark))
                            else
                                Brush.verticalGradient(
                                    listOf(
                                        p.gold.copy(alpha = 0.10f),
                                        p.surfaceDeep.copy(alpha = 0.55f)
                                    )
                                )
                        )
                        .border(
                            1.dp,
                            if (active) p.goldHighlight.copy(alpha = 0.85f) else p.gold.copy(alpha = 0.28f),
                            RoundedCornerShape(11.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (active) Color(0xFF1A1204) else p.gold.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                AutoFitText(
                    text = item.label,
                    color = if (active) p.gold else p.textSecondary,
                    fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
                    minimumSize = 7.sp,
                    maximumSize = 9.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(3.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (active) p.gold else Color.Transparent)
                )
            }
        }
    }
}

// ═════════════════════ ۴) کارت طلایی بزرگ (CTA مرجع) ═════════════════════

/**
 * کارت طلایی پررنگ مرجع (مثل «ساخت چارت»):
 * متن راست‌چین تیره روی گرادیان طلایی + دکمهٔ مربع‌گرد در سمت چپ.
 */
@Composable
fun MaGoldCta(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    enabled: Boolean = true,
) {
    val p = vizitorPalette
    val dim = if (enabled) 1f else 0.55f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        p.goldHighlight.copy(alpha = 0.96f * dim),
                        p.gold.copy(alpha = 0.92f * dim),
                        p.goldDark.copy(alpha = 0.88f * dim),
                    )
                )
            )
            .border(1.dp, p.goldHighlight.copy(alpha = 0.55f * dim), RoundedCornerShape(22.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF3A2A08).copy(alpha = dim),
                            Color(0xFF120C02).copy(alpha = dim)
                        )
                    )
                )
                .border(1.dp, p.goldHighlight.copy(alpha = 0.45f * dim), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = p.goldHighlight,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1204),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    textAlign = TextAlign.Start
                )
                if (!badge.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x331A1204))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2A1D04)
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(
                subtitle,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xCC1A1204),
                textAlign = TextAlign.Start
            )
        }
    }
}

// ════════════════════ ۵) قرص‌های بخش (Segment مرجع) ════════════════════

/**
 * ردیف قرص‌های انتخاب بخش (مثل «دریافت / پرداخت / روزانه / تحلیل» مرجع).
 * قرص فعال طلایی با متن تیره؛ بقیه شیشهٔ تیره با متن طلایی.
 */
@Composable
fun MaSegmentPills(
    items: List<MaNavItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    tints: Map<String, Color> = emptyMap(),
) {
    val p = vizitorPalette
    if (items.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { item ->
            val active = item.key == selectedKey
            val tint = tints[item.key] ?: p.gold
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (active)
                            Brush.verticalGradient(listOf(p.goldHighlight, p.gold, p.goldDark))
                        else
                            Brush.verticalGradient(
                                listOf(p.surface.copy(alpha = 0.92f), p.surfaceDeep.copy(alpha = 0.94f))
                            )
                    )
                    .border(
                        1.dp,
                        if (active) p.goldHighlight.copy(alpha = 0.7f) else tint.copy(alpha = 0.32f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(item.key) }
                    .padding(horizontal = 6.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (active) Color(0xFF1A1204) else tint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                AutoFitText(
                    text = item.label,
                    color = if (active) Color(0xFF1A1204) else p.textPrimary,
                    fontWeight = FontWeight.Black,
                    minimumSize = 8.sp,
                    maximumSize = 11.sp,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

// ═══════════════ ۶) نوار نازک روند با دستهٔ طلایی («نمایش …») ═══════════════

/**
 * نوار نازک پیشرفت با دستهٔ طلایی — همان نمای «نمایش ۱۰ مورد اخیر» مرجع
 * که این‌جا برای نمایش نسبت‌های واقعی (سهمیه، اعتبار، سهم مشتری) به کار می‌رود.
 */
@Composable
fun MaThinBar(
    caption: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    color: Color? = null,
) {
    val p = vizitorPalette
    val f = fraction.coerceIn(0f, 1f)
    val c = color ?: p.gold
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                caption,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!valueText.isNullOrBlank()) {
                Text(
                    valueText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = c
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // ریل تیره
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(p.surfaceDeep, c.copy(alpha = 0.18f), p.surfaceDeep)
                        )
                    )
                    .border(0.6.dp, c.copy(alpha = 0.22f), RoundedCornerShape(50))
            )
            // بخش پرشده
            Box(
                modifier = Modifier
                    .fillMaxWidth(f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(c.copy(alpha = 0.55f), c)))
            )
            // دستهٔ طلایی
            Box(
                modifier = Modifier
                    .fillMaxWidth(f)
                    .height(14.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(c, c.copy(alpha = 0.55f))))
                        .border(1.dp, p.goldHighlight.copy(alpha = 0.7f), CircleShape)
                )
            }
        }
    }
}

// ═══════════════ ۷) کارت سند MEELANO REPORTS (کارت گزارش مرجع) ═══════════════

/**
 * کارت سند مرجع: قرص «MEELANO REPORTS» + نشان M•D، عنوان، دوره،
 * ردیف‌های «برچسب — مقدار» و در پایان امضای برنامه‌نویس.
 * @param rows هر ردیف: (برچسب، مقدار) — مقدار خالی با «—» نشان داده می‌شود.
 */
@Composable
fun MaDocCard(
    title: String,
    subtitle: String,
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    badge: String = "MEELANO REPORTS",
    note: String? = null,
    footer: String = "M•A Report — Meelano Reports",
    brand: String = "Meelano Studio Design · Milad Yaghoobi",
) {
    val p = vizitorPalette
    Column(
        modifier = modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(22.dp), strong = true, corner = 22f)
            .padding(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(
                            listOf(p.goldHighlight.copy(alpha = 0.92f), p.gold.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    badge,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF1A1204)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "M•D",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = p.gold
            )
        }
        Spacer(Modifier.height(9.dp))
        Text(
            title,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Black,
            color = p.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            fontSize = 11.sp,
            color = p.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        rows.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(p.gold)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = p.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    value.ifBlank { "—" },
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = p.gold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(p.gold.copy(alpha = 0.09f))
                    .border(1.dp, p.gold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 9.dp, vertical = 7.dp)
            ) {
                Text(
                    note,
                    fontSize = 10.5.sp,
                    lineHeight = 16.sp,
                    color = p.textSecondary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        MaGoldDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            footer,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Black,
            color = p.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            brand,
            fontSize = 10.5.sp,
            color = p.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ═══════════════ ۸) برگهٔ «تنظیمات هوشمند» (گروه‌های بازشو) ═══════════════

/** یک کاشی در برگهٔ تنظیمات هوشمند (Word/Excel/PDF/چاپ/اشتراک …). */
data class MaSmartTile(
    val key: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
)

/** یک گروه بازشو در برگهٔ تنظیمات هوشمند. */
data class MaSmartGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tiles: List<MaSmartTile>,
)

/**
 * برگهٔ «تنظیمات هوشمند» مرجع — روی صفحه باز می‌شود، هر گروه با لمس
 * باز/بسته می‌شود و کاشی‌های هر گروه کارهای واقعی برنامه را اجرا می‌کنند.
 */
@Composable
fun MaSmartSheet(
    title: String,
    subtitle: String,
    groups: List<MaSmartGroup>,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(p.surface.copy(alpha = 0.99f), p.background.copy(alpha = 1f))
                    )
                )
                .border(1.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(26.dp))
                .padding(13.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── سرصفحهٔ برگه ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                MaOrbButton(
                    icon = Icons.Filled.Settings,
                    onClick = onClose,
                    size = 44.dp,
                    contentDescription = "بستن"
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = p.gold
                    )
                    Text(
                        subtitle,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        color = p.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.gold.copy(alpha = 0.12f))
                        .border(1.dp, p.gold.copy(alpha = 0.45f), RoundedCornerShape(50))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        "بستن ✕",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = p.gold
                    )
                }
            }
            Spacer(Modifier.height(11.dp))

            groups.forEach { group ->
                MaSmartGroupCard(group = group, onPick = onPick)
                Spacer(Modifier.height(10.dp))
            }

            Text(
                "همهٔ این کارها روی اتصال مستقیم SQL Server اجرا می‌شوند؛ " +
                    "هیچ رمزی در متن یا گزارش چاپ نمی‌شود.",
                fontSize = 9.5.sp,
                lineHeight = 14.sp,
                color = p.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** یک گروه بازشو با کاشی‌های دو ستونه. */
@Composable
private fun MaSmartGroupCard(
    group: MaSmartGroup,
    onPick: (String) -> Unit,
) {
    val p = vizitorPalette
    var open by remember(group.key) { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
            .padding(11.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { open = !open }
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(p.gold.copy(alpha = 0.22f), p.surfaceDeep.copy(alpha = 0.8f))
                        )
                    )
                    .border(1.dp, p.gold.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    group.icon,
                    contentDescription = null,
                    tint = p.gold,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    group.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = p.gold
                )
                Text(
                    group.subtitle,
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    color = p.textSecondary
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(p.gold.copy(alpha = 0.12f))
                    .border(1.dp, p.gold.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = if (open) "بستن گروه" else "باز کردن گروه",
                    tint = p.gold,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (open) 180f else 0f)
                )
            }
        }
        if (open) {
            Spacer(Modifier.height(9.dp))
            group.tiles.chunked(2).forEach { rowTiles ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTiles.forEach { tile ->
                        MaSmartTileCard(
                            tile = tile,
                            onClick = { onPick(tile.key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** کاشی رنگی برگهٔ تنظیمات (به سبک Word/Excel/PDF مرجع). */
@Composable
private fun MaSmartTileCard(
    tile: MaSmartTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(tile.tint.copy(alpha = 0.20f), p.surfaceDeep.copy(alpha = 0.92f))
                )
            )
            .border(1.dp, tile.tint.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tile.tint.copy(alpha = 0.22f))
                .border(1.dp, tile.tint.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                tile.icon,
                contentDescription = null,
                tint = tile.tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tile.label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = p.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                tile.subtitle,
                fontSize = 8.5.sp,
                lineHeight = 12.sp,
                color = p.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
