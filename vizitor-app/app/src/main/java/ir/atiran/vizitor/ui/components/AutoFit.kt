/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | اجزای تازهٔ رابط کاربری (اندازه‌گیری خودکار متن)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  · AutoFitText : متن دکمه/چیپ/سرستون را همان‌قدر بزرگ می‌گیرد که «جا شود»؛
 *                  اگر جا نشد، پله‌پله کوچک می‌شود (تا کف تعیین‌شده) — پس هیچ
 *                  برچسبی در دکمه‌ها نمی‌بُرد و هیچ‌جا متن سرریز نمی‌کند.
 *  · IdealButton : دکمهٔ سه‌بعدی با لبهٔ عمق، درخشش، فشرده‌شدن هنگام لمس و
 *                  برچسب خودتنظیم + آیکن اختیاری.
 *  · PriceRow    : سطر «جدول قیمت» با برچسب، مقدار و رنگ معنا (فروش/مصرف‌کننده/
 *                  میانگین/پیش‌فرض) برای ویترین و گزارش‌ها.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints

/**
 * متنِ خودتنظیم: اندازهٔ قلم از [maximumSize] شروع می‌شود و تا وقتی متن در
 * عرض موجود جا نشود یا به [minimumSize] برسد، کوچک‌تر می‌شود.
 * بر پایهٔ onTextLayout (بدون API آزمایشی) — نتیجه روی همهٔ نسخه‌های Compose پایدار است.
 */
@Composable
fun AutoFitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight = FontWeight.Bold,
    minimumSize: TextUnit = 9.sp,
    maximumSize: TextUnit = 16.sp,
    maxLines: Int = 1,
    letterSpacing: TextUnit = 0.sp,
    textAlign: TextAlign = TextAlign.Center,
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val available = maxWidth
        var fontSize by remember(text, available) { mutableStateOf(maximumSize) }
        var settled by remember(text, available) { mutableStateOf(false) }
        val floor = minimumSize.value

        Text(
            text = text,
            color = color,
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // حلقهٔ متوقف‌شدنی: تا وقتی متن سرریز می‌کند کوچک می‌شویم و
                // به‌محض جا شدن، اندازه «تثبیت» می‌شود تا هیچ نوسانی رخ ندهد.
                if (!settled && (result.lineCount > maxLines || result.didOverflowWidth)) {
                    val next = fontSize.value - 0.5f
                    if (next <= floor) {
                        fontSize = floor.sp
                        settled = true
                    } else {
                        fontSize = next.sp
                    }
                } else if (!settled) {
                    settled = true
                }
            }
        )
    }
}

/**
 * دکمهٔ ایده‌آل: عمق سه‌بعدی + گرادیان + فشرده‌شدن هنگام لمس + برچسب خودتنظیم.
 */
@Composable
fun IdealButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    face: List<Color> = listOf(Color(0xFFFFE29A), Color(0xFFF0B23C)),
    edge: Color = Color(0xFF8F6414),
    textColor: Color = Color(0xFF3B2A00),
    minFont: TextUnit = 9.sp,
    maxFont: TextUnit = 14.sp,
    corner: androidx.compose.ui.unit.Dp = 16.dp,
    height: androidx.compose.ui.unit.Dp = 44.dp,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = tween(90),
        label = "idealButtonPress"
    )
    val alpha = if (enabled) 1f else 0.45f
    Box(modifier = modifier.scale(scale).height(height)) {
        // لبهٔ عمق (زیر)
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 3.dp)
                .clip(RoundedCornerShape(corner))
                .background(edge.copy(alpha = alpha))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height - 3.dp)
                .clip(RoundedCornerShape(corner))
                .background(Brush.linearGradient(face))
                .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(corner))
                .clickable(enabled = enabled) {
                    pressed = true
                    onClick()
                }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            AutoFitText(
                text = label,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                minimumSize = minFont,
                maximumSize = maxFont,
                modifier = Modifier.weight(1f, fill = true)
            )
        }
    }
    // بازگرداندن حالت فشرده پس از یک لحظه
    if (pressed) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(110)
            pressed = false
        }
    }
}

/**
 * سطر جدول قیمت — برچسب در ابتدا، مقدار در انتها، نوار رنگی معنا در کنار.
 */
@Composable
fun PriceRow(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    badge: String? = null,
    emphasized: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (emphasized) tint.copy(alpha = 0.14f) else Color(0x0DFFFFFF))
            .border(
                1.dp,
                if (emphasized) tint.copy(alpha = 0.45f) else Color(0x1FFFFFFF),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .clip(RoundedCornerShape(50))
                .background(tint)
        )
        Spacer(Modifier.width(7.dp))
        AutoFitText(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
            fontWeight = FontWeight.Bold,
            minimumSize = 8.sp,
            maximumSize = 11.5.sp,
            modifier = Modifier.weight(1f, fill = true),
            textAlign = TextAlign.Start
        )
        if (badge != null) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.20f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    badge,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = tint,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(6.dp))
        }
        Text(
            value,
            fontSize = if (emphasized) 12.5.sp else 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (emphasized) tint else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** سرستون جدول ویترین — عنوان سمت راست، شمارش سمت چپ. */
@Composable
fun TableHeader(
    title: String,
    count: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    // v2.17.0 — سربرگ بخش‌ها با نگین طلایی و شمارشِ قرص‌مانند (سبک مرجع)
    MaSectionHeader(title = title, count = count, icon = icon, modifier = modifier)
}

/** کاشی کوچک آماری برای سربرگ بخش‌ها (تعداد کالا/مشتری/فاکتور). */
@Composable
fun MiniStat(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    // v2.17.0 — کاشی کوچک آماری با قاب فلزی طلایی هم‌رنگ مقدار
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.16f), Color(0x0A000000))
                )
            )
            .border(1.dp, tint.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AutoFitText(
            text = value,
            color = tint,
            fontWeight = FontWeight.ExtraBold,
            minimumSize = 11.sp,
            maximumSize = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(2.dp))
        AutoFitText(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontWeight = FontWeight.Medium,
            minimumSize = 7.5.sp,
            maximumSize = 9.5.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** تصویر کالا با قاب کریستالی و گوشه‌های نرم (برای ویترین). */
@Composable
fun GoodsImage(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = resId),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
