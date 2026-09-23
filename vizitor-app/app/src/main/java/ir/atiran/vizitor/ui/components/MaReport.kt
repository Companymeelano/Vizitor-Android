/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | زبانِ طراحی «گزارش طلایی» (v2.17.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل، همان زبانِ بصری نمونهٔ مرجع (M•A Report) را به برنامه می‌آورد:
 *    ▸ پس‌زمینهٔ مشکیِ عمیق + طلای فلزی (نه بنفش/سبز نئونی)
 *    ▸ کارت‌های فلزی با حاشیهٔ طلایی باریک و خط نور بالای کارت
 *    ▸ نمودار ستونی «استوانهٔ فلزی» با بازتاب (دقیقاً مثل مرجع)
 *    ▸ نمودار خطی دو‌سری با پرشدگی گرادیانی (روند فروش / وصول)
 *    ▸ نمودار حلقه‌ای درخشان سه‌بعدی با نگین‌های الماسی
 *    ▸ ردیف‌های «نبض کسب‌وکار» با نوار وضعیت رنگی (سبز/کهربایی/قرمز)
 *    ▸ کارت‌های شاخص (KPI) با آیکن طلایی و مقدار درشت
 *    ▸ کاشی‌های ابزار (خروجی/دسترسی) به سبک Word/Excel/PDF مرجع
 *    ▸ نشان برند «MEELANO STUDIO DESIGN» داخل قرص طلایی
 *  همهٔ اجزا فقط با Canvas خودِ Compose رسم می‌شوند: بدون کتابخانهٔ بیرونی،
 *  بدون سنگین‌شدن APK و با احترام به حالت «گرافیک سبک» (VizitorPerf).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

// ── رنگ‌های وضعیت (همان سه‌گانهٔ مرجع: پایدار / نیازمند توجه / بحران) ──
val MaGreen = Color(0xFF5BC26A)
val MaAmber = Color(0xFFE0A94A)
val MaRed = Color(0xFFE5544B)

/** رنگ و واژهٔ وضعیت بر اساس نسبت ۰..۱ (یک‌جا و یکسان در کل برنامه). */
fun maStatus(fraction: Float): Pair<Color, String> = when {
    fraction >= 0.66f -> MaGreen to "پایدار"
    fraction >= 0.33f -> MaAmber to "نیازمند توجه"
    else -> MaRed to "بحران"
}

// ═══════════════════════ ۱) نشان برند و جداکنندهٔ طلایی ═══════════════════════

/**
 * نشان «MEELANO STUDIO DESIGN» — قرصِ فلزی با حاشیهٔ طلایی و نشان M.
 * همان امضای بصری انتهای صفحه‌های مرجع.
 */
@Composable
fun MeelanoPill(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val p = vizitorPalette
    val h = if (compact) 26.dp else 30.dp
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0x1AFFFFFF), p.gold.copy(alpha = 0.16f), Color(0x1AFFFFFF))
                )
            )
            .border(1.dp, p.gold.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = if (compact) 10.dp else 13.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "MEELANO STUDIO DESIGN",
            fontSize = if (compact) 8.5.sp else 9.5.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp,
            color = p.gold
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(h - 8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.verticalGradient(listOf(p.goldHighlight, p.gold, p.goldDark))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "M",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A1204)
            )
        }
    }
}

/** خط جداکنندهٔ طلایی با نگین وسط — جایگزین خط‌های سادهٔ مرجع. */
@Composable
fun MaGoldDivider(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val p = vizitorPalette
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, p.gold.copy(alpha = 0.55f), p.goldHighlight, p.gold.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
        if (!label.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = p.gold.copy(alpha = 0.85f)
            )
        }
    }
}

// ═══════════════════════ ۲) سطح فلزی‌طلایی کارت‌ها ═══════════════════════

/**
 * قاب فلزی-طلایی: گرادیان عمودی سطوح + حاشیهٔ باریک طلایی + خط نور بالای کارت.
 * پایهٔ ریختِ نو همهٔ کارت‌های برنامه.
 */
@Composable
fun Modifier.metalPanel(
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    strong: Boolean = false,
    corner: Float = 20f,
): Modifier {
    val p = vizitorPalette
    return this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    p.surface.copy(alpha = if (strong) 0.99f else 0.93f),
                    p.surfaceDeep.copy(alpha = if (strong) 1f else 0.97f)
                )
            )
        )
        .border(borderWidth, p.gold.copy(alpha = if (strong) 0.45f else 0.30f), shape)
        .metalSheen(p.goldHighlight, corner)
}

/** خط نور بالای کارت (حس فلز صیقلی) — یک لایهٔ سبک، بدون انیمیشن. */
private fun Modifier.metalSheen(strike: Color, corner: Float): Modifier = this.drawBehind {
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(strike.copy(alpha = 0.11f), Color.Transparent),
            startY = 0f, endY = size.height * 0.5f
        ),
        cornerRadius = CornerRadius(corner.dp.toPx())
    )
}

/** نشان آیکنی طلایی (مربع گوشه‌گرد) — همان کاشیِ آیکن مرجع در کارت‌ها. */
@Composable
fun MaIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    tint: Color? = null,
    dark: Boolean = true,
) {
    val p = vizitorPalette
    val c = tint ?: p.gold
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(
                Brush.verticalGradient(
                    listOf(c.copy(alpha = 0.95f), c.copy(alpha = 0.55f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (dark) Color(0xFF1A1204) else Color.White,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

// ═══════════════════════ ۳) سرتیترها ═══════════════════════

/** نگین الماسی طلایی (مربعِ چرخیده) — بولت عنوان‌ها. */
@Composable
private fun DiamondDot(size: Dp = 7.dp, color: Color? = null) {
    val p = vizitorPalette
    val c = color ?: p.gold
    Box(
        Modifier
            .size(size)
            .rotate(45f)
            .background(Brush.linearGradient(listOf(p.goldHighlight, c)), RoundedCornerShape(2.dp))
    )
}

/** سرتیتر داخلی کارت‌ها: نگین + عنوان طلایی + شمارش + خط جداکننده. */
@Composable
fun MaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    icon: ImageVector? = null,
) {
    val p = vizitorPalette
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                MaIconBadge(icon, size = 28.dp)
                Spacer(Modifier.width(8.dp))
            } else {
                DiamondDot(size = 8.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.2.sp,
                    brush = Brush.horizontalGradient(listOf(p.goldHighlight, p.gold, p.goldDark))
                ),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            if (!count.isNullOrBlank()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(p.gold.copy(alpha = 0.14f))
                        .border(1.dp, p.gold.copy(alpha = 0.40f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(count, fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = p.gold)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        MaGoldDivider()
    }
}

/**
 * تیتر قهرمان صفحه (مثل «M•A Report» مرجع): طلایی درشت + زیرنویس لوکس.
 * @param fx در حالت «گرافیک سبک» انیمیشن شیمر اجرا نمی‌شود.
 */
@Composable
fun MaHeroTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    fx: Boolean = true,
) {
    val p = vizitorPalette
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (fx) {
            ShimmerGoldText(title)
        } else {
            Text(
                title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    brush = Brush.horizontalGradient(listOf(Gold, p.goldHighlight))
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(34.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, p.gold.copy(alpha = 0.7f))
                            )
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = p.gold.copy(alpha = 0.9f)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(34.dp)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(p.gold.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

// ═══════════════════════ ۴) کارت شاخص (KPI) و نبض کسب‌وکار ═══════════════════════

/**
 * کارت شاخص مرجع: عنوان طلایی + آیکن، ارزش درشت، زیرنویس، خط کوتاه طلایی.
 * @param caption متن پایین کارت (مثل «نمایش ۱۰ مورد اخیر»).
 */
@Composable
fun MaMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    caption: String? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val p = vizitorPalette
    val c = tint ?: p.gold
    Column(
        modifier = modifier
            .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
            .then(onClick?.let { cb -> Modifier.clickable { cb() } } ?: Modifier)
            .padding(horizontal = 11.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                AutoFitText(
                    text = title,
                    color = p.textPrimary,
                    fontWeight = FontWeight.Black,
                    minimumSize = 10.sp,
                    maximumSize = 14.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    AutoFitText(
                        text = subtitle,
                        color = p.textSecondary,
                        fontWeight = FontWeight.Medium,
                        minimumSize = 7.5.sp,
                        maximumSize = 10.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            MaIconBadge(icon, size = 36.dp, tint = c)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(p.goldHighlight, p.goldDark)))
        )
        Spacer(Modifier.height(7.dp))
        AutoFitText(
            text = value,
            color = c,
            fontWeight = FontWeight.Black,
            minimumSize = 14.sp,
            maximumSize = 22.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        if (!caption.isNullOrBlank()) {
            Spacer(Modifier.height(5.dp))
            MaGoldDivider()
            Spacer(Modifier.height(5.dp))
            AutoFitText(
                text = caption,
                color = p.textSecondary,
                fontWeight = FontWeight.Medium,
                minimumSize = 8.sp,
                maximumSize = 10.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** یک ردیف «نبض»: برچسب + نوار وضعیت رنگی + قرص وضعیت (سبز/کهربایی/قرمز). */
data class MaPulseRow(
    val label: String,
    val fraction: Float,
    val status: String,
    val color: Color,
    val hint: String? = null,
)

/** کارت «نبض کسب‌وکار» — همان جدولِ سلامتی مرجع با نوار و وضعیت رنگی. */
@Composable
fun MaPulseCard(
    rows: List<MaPulseRow>,
    modifier: Modifier = Modifier,
    title: String = "نبض کسب‌وکار",
) {
    val p = vizitorPalette
    Column(
        modifier = modifier
            .metalPanel(RoundedCornerShape(22.dp), corner = 22f)
            .padding(13.dp)
    ) {
        MaSectionHeader(title)
        Spacer(Modifier.height(9.dp))
        rows.forEach { r ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, r.color.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(r.color, r.color.copy(alpha = 0.25f))))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(p.donutTrack)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(r.fraction.coerceIn(0.03f, 1f))
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(r.color.copy(alpha = 0.55f), r.color)
                                )
                            )
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    AutoFitText(
                        text = r.label,
                        color = p.textPrimary,
                        fontWeight = FontWeight.Bold,
                        minimumSize = 8.5.sp,
                        maximumSize = 11.5.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                    if (!r.hint.isNullOrBlank()) {
                        AutoFitText(
                            text = r.hint,
                            color = p.textSecondary,
                            fontWeight = FontWeight.Medium,
                            minimumSize = 6.5.sp,
                            maximumSize = 8.5.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(96.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════ ۵) نمودار ستونی فلزی (استوانه) ═══════════════════════

/**
 * نمودار ستونی «استوانهٔ فلزی» با بازتاب و برچسب مقدار — همان نمودار مرجع،
 * با گرادیان افقی برای حس استوانه و سایهٔ عمودی برای عمق.
 */
@Composable
fun MetalBarChart(
    rows: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    maxBars: Int = 7,
    valueLabel: (Long) -> String = { it.toFaPrice() },
) {
    val p = vizitorPalette
    val data = remember(rows) { rows.take(maxBars) }
    val maxValue = (data.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val grow by animateFloatAsState(1f, tween(700), label = "metalBarGrow")

    if (data.isEmpty()) {
        MaEmptyState(
            title = "داده‌ای برای نمودار نیست",
            subtitle = "پس از همگام‌سازی با سرور، نمودار اینجا رسم می‌شود",
            modifier = modifier
        )
        return
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { (label, value) ->
                val frac = (value.toDouble() / maxValue.toDouble()).toFloat() * grow
                val barFrac = frac.coerceIn(0.05f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoFitText(
                        text = valueLabel(value),
                        color = p.gold,
                        fontWeight = FontWeight.ExtraBold,
                        minimumSize = 6.sp,
                        maximumSize = 9.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    // ستون استوانه‌ای
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((height.value * 0.62f).dp * barFrac)
                            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(p.goldDark, p.gold, p.goldHighlight, p.gold, p.goldDark)
                                )
                            )
                    ) {
                        // سایهٔ عمقی پایین ستون (حس حجم)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0x33000000))
                                    )
                                )
                        )
                        // درخشش نوک ستون
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(p.goldHighlight.copy(alpha = 0.85f))
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    // بازتاب کف (Reflection)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((height.value * 0.10f).dp * barFrac)
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(p.gold.copy(alpha = 0.22f), Color.Transparent)
                                )
                            )
                    )
                    Spacer(Modifier.height(5.dp))
                    AutoFitText(
                        text = label,
                        color = p.textSecondary,
                        fontWeight = FontWeight.Medium,
                        minimumSize = 6.5.sp,
                        maximumSize = 9.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        MaGoldDivider()
    }
}

// ═══════════════════════ ۶) نمودار خطی روند (دو‌سری) ═══════════════════════

/** یک سری داده برای نمودار روند. */
data class TrendSeries(
    val name: String,
    val color: Color,
    val values: List<Long>,
)

/**
 * نمودار خطی نرم با پرشدگی گرادیانی و راهنمای رنگ — مثل «روند مطالبات
 * و وصول طلب» مرجع. همهٔ محاسبات روی دادهٔ واقعی و داخل Canvas است.
 */
@Composable
fun LineTrendChart(
    labels: List<String>,
    series: List<TrendSeries>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    valueLabel: (Long) -> String = { it.toFaPrice() },
) {
    val p = vizitorPalette
    val allValues = series.flatMap { it.values }
    val maxV = (allValues.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val grow by animateFloatAsState(1f, tween(850), label = "trendGrow")

    Column(modifier.fillMaxWidth()) {
        // راهنمای رنگ
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            series.forEach { s ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(s.color)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        s.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.textSecondary
                    )
                    Spacer(Modifier.width(10.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val n = labels.size
            if (n < 2) return@Canvas
            val stepX = size.width / (n - 1).toFloat()

            // خطوط راهنمای افقی
            for (i in 0..3) {
                val y = size.height * (i / 3f)
                drawLine(
                    color = p.gold.copy(alpha = 0.09f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
            }

            series.forEach { s ->
                if (s.values.size < 2) return@forEach
                val pts = s.values.mapIndexed { i, v ->
                    val x = stepX * i
                    val ratio = v.toDouble() / maxV.toDouble()
                    val y = size.height - (ratio * size.height * 0.80 * grow).toFloat() -
                        (size.height * 0.10f)
                    Offset(x, y.coerceIn(2f, size.height - 2f))
                }
                val line = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val cur = pts[i]
                        val midX = (prev.x + cur.x) / 2f
                        cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                    }
                }
                val fill = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, size.height)
                    lineTo(pts.first().x, size.height)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(s.color.copy(alpha = 0.30f), Color.Transparent),
                        startY = 0f,
                        endY = size.height
                    )
                )
                drawPath(
                    path = line,
                    color = s.color,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )
                // نقاط درخشان روی گره‌های روند
                pts.forEach { pt ->
                    drawCircle(color = p.surfaceDeep, radius = 3.4.dp.toPx(), center = pt)
                    drawCircle(color = s.color, radius = 2.3.dp.toPx(), center = pt)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val shown = when {
                labels.size <= 4 -> labels
                else -> listOf(labels.first(), labels[labels.size / 2], labels.last())
            }
            shown.forEach { l ->
                Text(
                    l,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = p.textSecondary,
                    maxLines = 1
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "بیشترین مقدار در این بازه: ${valueLabel(maxV)}",
            fontSize = 8.5.sp,
            color = p.textSecondary.copy(alpha = 0.75f)
        )
    }
}

// ═══════════════════════ ۷) نمودار حلقه‌ای سه‌بعدی درخشان ═══════════════════════

/**
 * حلقهٔ درخشان با عمق و نگین‌های الماسی — همان حلقهٔ سه‌بعدی مرجع.
 * @param percent نسبت پرشدگی ۰ تا ۱
 */
@Composable
fun Ring3DChart(
    percent: Float,
    centerTitle: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 190.dp,
    accent: Color? = null,
) {
    val p = vizitorPalette
    val c = accent ?: p.gold
    val sweep by animateFloatAsState(percent.coerceIn(0f, 1f), tween(900), label = "ringGrow")

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = diameter.toPx() * 0.115f
            val d = kotlin.math.min(this.size.width, this.size.height) - stroke * 1.9f
            val left = (this.size.width - d) / 2f
            val top = (this.size.height - d) / 2f + stroke * 0.35f
            val box = Size(d, d)

            // ۰) سایهٔ عمق زیر حلقه
            drawArc(
                color = Color.Black.copy(alpha = 0.55f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(left, top + stroke * 0.22f), size = box,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۱) ریل زمینه
            drawArc(
                color = c.copy(alpha = 0.12f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(left, top), size = box,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۲) کمان درخشان
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(p.goldDark, c, p.goldHighlight, c, p.goldDark)
                ),
                startAngle = -90f,
                sweepAngle = (360f * sweep).coerceAtLeast(4f),
                useCenter = false,
                topLeft = Offset(left, top), size = box,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۳) باریکهٔ نور روی کمان (حس برجستگی)
            drawArc(
                color = Color.White.copy(alpha = 0.28f),
                startAngle = -90f,
                sweepAngle = (360f * sweep).coerceAtLeast(4f),
                useCenter = false,
                topLeft = Offset(left, top), size = box,
                style = Stroke(stroke * 0.28f, cap = StrokeCap.Round)
            )
            // ۴) نگین‌های الماسی چهار جهت
            val cx = this.size.width / 2f
            val cy = top + d / 2f
            val r = d / 2f
            listOf(-90f, 0f, 90f, 180f).forEach { ang ->
                val a = Math.toRadians(ang.toDouble())
                val x = cx + (r * kotlin.math.cos(a)).toFloat()
                val y = cy + (r * kotlin.math.sin(a)).toFloat()
                val s = stroke * 0.30f
                val diamond = Path().apply {
                    moveTo(x, y - s)
                    lineTo(x + s, y)
                    lineTo(x, y + s)
                    lineTo(x - s, y)
                    close()
                }
                drawPath(diamond, p.goldHighlight)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerTitle.isNotBlank()) {
                AutoFitText(
                    text = centerTitle,
                    color = p.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    minimumSize = 7.5.sp,
                    maximumSize = 10.sp,
                    modifier = Modifier.width(diameter * 0.52f)
                )
            }
            Spacer(Modifier.height(2.dp))
            AutoFitText(
                text = centerValue,
                color = c,
                fontWeight = FontWeight.Black,
                minimumSize = 13.sp,
                maximumSize = 24.sp,
                modifier = Modifier.width(diameter * 0.62f)
            )
        }
    }
}

// ═══════════════════════ ۸) کاشی‌های ابزار و حالت خالی ═══════════════════════

/** یک کاشی ابزار (به سبک Word/Excel/PDF/چاپ مرجع). */
data class MaToolTile(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit = {},
)

/** شبکهٔ کاشی‌های ابزار با آیکن رنگی و برچسب زیر آن. */
@Composable
fun MaToolGrid(
    tools: List<MaToolTile>,
    modifier: Modifier = Modifier,
    columns: Int = 5,
    title: String? = null,
) {
    val p = vizitorPalette
    Column(
        modifier = modifier
            .metalPanel(RoundedCornerShape(22.dp), corner = 22f)
            .padding(13.dp)
    ) {
        if (!title.isNullOrBlank()) {
            MaSectionHeader(title)
            Spacer(Modifier.height(10.dp))
        }
        tools.chunked(columns).forEach { rowTools ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTools.forEach { t ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { t.onClick() }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(t.tint.copy(alpha = 0.85f), t.tint.copy(alpha = 0.42f))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                t.icon,
                                contentDescription = t.label,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        AutoFitText(
                            text = t.label,
                            color = p.textPrimary,
                            fontWeight = FontWeight.Bold,
                            minimumSize = 7.5.sp,
                            maximumSize = 10.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                repeat(columns - rowTools.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** حالت خالی مرجع: حلقهٔ طلایی با تیک + عنوان + توضیح. */
@Composable
fun MaEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(p.gold.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
                .border(1.dp, p.gold.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = p.gold,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(9.dp))
        AutoFitText(
            text = title,
            color = p.textPrimary,
            fontWeight = FontWeight.ExtraBold,
            minimumSize = 11.sp,
            maximumSize = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(3.dp))
        AutoFitText(
            text = subtitle,
            color = p.textSecondary,
            fontWeight = FontWeight.Medium,
            minimumSize = 8.5.sp,
            maximumSize = 11.sp,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** نوار خلاصهٔ عددی: چند شاخص کوچک در یک ردیف (زیر تیتر قهرمان صفحه). */
@Composable
fun MaStatStrip(
    items: List<Triple<String, String, Color>>,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(18.dp), corner = 18f)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items.forEach { (label, value, color) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AutoFitText(
                    text = value,
                    color = color,
                    fontWeight = FontWeight.Black,
                    minimumSize = 10.sp,
                    maximumSize = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(2.dp))
                AutoFitText(
                    text = label,
                    color = p.textSecondary,
                    fontWeight = FontWeight.Medium,
                    minimumSize = 7.sp,
                    maximumSize = 9.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** عدد فارسی کوتاه برای برچسب‌های نمودار (بدون واحد پول). */
fun Long.faCount(): String = toFaNumber()
