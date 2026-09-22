/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نمودارهای سبک و بی‌وابستگی (v2.16.0)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرا دست‌ساز؟ چون هیچ کتابخانهٔ نموداری به پروژه اضافه نشده و همه‌چیز با
 *  Canvas خودِ Compose رسم می‌شود؛ پس حجم APK بالا نمی‌رود و روی گوشی ضعیف هم
 *  روان است (بدون انیمیشن سنگین). دو نمودار لازم پنل مدیریت:
 *    · BarChart  : فروش روزانه/ماهانه (ستون‌های گرادیانی + برچسب مقدار)
 *    · DonutChart: سهم بخش‌ها با وسط‌چین عددی + راهنمای رنگ (legend)
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

/**
 * نمودار ستونی افقی‌نامحدود (عمودی، ۸ ستون بیشینه) با برچسب مقدار و نام.
 * @param data جفت‌های (برچسب، مقدار) — ترتیب نمایش از راست به چپ.
 */
@Composable
fun BarChart(
    data: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 170.dp,
    start: Color = Color(0xFF8B3FF0),
    end: Color = Color(0xFFE8B23F),
    valueLabel: (Long) -> String = { it.toFaPrice() },
) {
    val p = vizitorPalette
    val shown = remember(data) { data.take(8) }
    val maxValue = (shown.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val grow by animateFloatAsState(1f, tween(650), label = "barGrow")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shown.forEach { (label, value) ->
                val fraction = (value.toDouble() / maxValue.toDouble()).toFloat().coerceIn(0f, 1f)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AutoFitText(
                        text = valueLabel(value),
                        color = p.gold,
                        fontWeight = FontWeight.ExtraBold,
                        minimumSize = 6.5.sp,
                        maximumSize = 9.5.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((height.value * 0.68f).dp * (fraction * grow).coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Brush.verticalGradient(listOf(start, end)))
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shown.forEach { (label, _) ->
                AutoFitText(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    minimumSize = 6.5.sp,
                    maximumSize = 9.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * نمودار دونات (حلقه‌ای) با عدد مرکزی و راهنمای رنگ.
 * @param slices سه‌گانه‌های (نام، مقدار، رنگ)
 */
@Composable
fun DonutChart(
    slices: List<Triple<String, Long, Color>>,
    modifier: Modifier = Modifier,
    centerTitle: String = "",
    centerValue: String = "",
    size: androidx.compose.ui.unit.Dp = 150.dp,
) {
    val p = vizitorPalette
    val live = remember(slices) { slices.filter { it.second > 0L } }
    val total = live.sumOf { it.second }.coerceAtLeast(1L)
    val sweep by animateFloatAsState(1f, tween(700), label = "donutGrow")

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(size)) {
                val stroke = 22.dp.toPx()
                val d = kotlin.math.min(this.size.width, this.size.height) - stroke
                val topLeft = Offset((this.size.width - d) / 2f, (this.size.height - d) / 2f)
                // حلقهٔ زمینه
                drawArc(
                    color = p.gold.copy(alpha = 0.14f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = Size(d, d),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                var startAngle = -90f
                live.forEach { (_, value, color) ->
                    val full = value.toDouble() / total.toDouble() * 360.0
                    val arc = (full * sweep).toFloat()
                    drawArc(
                        color = color,
                        startAngle = startAngle, sweepAngle = (arc - 2f).coerceAtLeast(1f),
                        useCenter = false, topLeft = topLeft, size = Size(d, d),
                        style = Stroke(stroke, cap = StrokeCap.Butt)
                    )
                    startAngle += full.toFloat()
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (centerTitle.isNotBlank()) {
                    Text(
                        centerTitle,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                if (centerValue.isNotBlank()) {
                    Text(
                        centerValue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = p.gold
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            live.take(6).forEach { (name, value, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                    Spacer(Modifier.width(7.dp))
                    AutoFitText(
                        text = name,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                        fontWeight = FontWeight.Bold,
                        minimumSize = 8.sp,
                        maximumSize = 11.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.weight(1f, fill = true)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${(value * 100 / total).toFaNumber()}٪",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
            }
            if (live.isEmpty()) {
                Text(
                    "داده‌ای برای نمایش نیست",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** سطر جدول ساده (برچسب + مقدار) — برای جدول‌های گزارش مدیریت. */
@Composable
fun ReportRow(
    index: String,
    title: String,
    subtitle: String?,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0DFFFFFF))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(index, fontSize = 10.sp, fontWeight = FontWeight.Black, color = tint)
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            AutoFitText(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                minimumSize = 9.sp,
                maximumSize = 12.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(1.dp))
                AutoFitText(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    minimumSize = 7.5.sp,
                    maximumSize = 9.5.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(value, fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = tint)
    }
}
