/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نمودارهای سه‌بعدی لاکچری
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  دونات با عمق و سایه (حس سه‌بعدی) + چارت ستونی استوانه‌ای با بازتاب.
 *  همه‌چیز یک‌لایه و استاتیک/کم‌فریم — بدون کوچک‌ترین لگ.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import ir.atiran.vizitor.ui.theme.DonutTrack
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonGreenDark
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import ir.atiran.vizitor.ui.theme.GoldDark
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.ui.theme.TextPrimary
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import kotlin.math.min

/**
 * نمودار حلقوی سه‌بعدی تارگت فروش: لایه عمق تاریک + ریل + قوس گرادیانی
 * سبز نئونی + هایلایت استودیویی + تیک‌های طلایی دور.
 */
@Composable
fun NeonDonutChart(
    progress: Float,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    trackColor: Color = DonutTrack,
    centerColor: Color = NeonGreen,
    progressBrush: Brush = Brush.sweepGradient(
        colors = listOf(NeonGreenDark, NeonGreen, Color(0xFFB8FFD9), NeonGreen)
    )
) {
    // رنگ‌های تم — خوانده شده در کانتکست کامپوزبل پیش از ورود به Canvas
    val haloColor = NeonPurple.copy(alpha = 0.10f)
    val tickColor = Gold
    // مدار چرخان تیک‌های طلایی ✨ (حرکت ابدی، خیلی آهسته) — در حالت «سبک» ثابت
    val tickOrbit = if (VizitorPerf.screenFx) {
        val o by rememberInfiniteTransition(label = "tickOrbit").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(18000, easing = LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "tickOrbitV"
        )
        o
    } else 0f
    val animated by produceState(initialValue = 0f, key1 = progress) {
        var current = 0f
        val step = progress / 40f
        while (current < progress) {
            current = min(current + kotlin.math.abs(step), progress)
            value = current
            kotlinx.coroutines.delay(12)
        }
        value = progress
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 16.dp.toPx()
            val pad = stroke / 2f + 6f
            val arcSize = Size(this.size.width - stroke - 12f, this.size.height - stroke - 12f)
            val tl = Offset(pad, pad)
            val sweep = 360f * animated.coerceIn(0f, 1f)

            // ۰) هاله رنگ اصلی تم پشت نمودار
            drawCircle(
                color = haloColor,
                radius = min(this.size.width, this.size.height) / 2f - 1f,
                style = Stroke(42f)
            )
            // ۱) سایه عمق (حس سه‌بعدی) — حلقه تاریک کمی پایین‌تر
            drawArc(
                color = Color(0xFF04060A),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(pad, pad + 7f), size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۲) ریل خاکستری
            drawArc(
                color = trackColor,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۳) تیک‌های طلایی چرخان دور نمودار (۱۲ نشان مداری)
            repeat(12) { i ->
                val a = Math.toRadians((i * 30 + tickOrbit - 90).toDouble())
                val r1 = this.size.width / 2f - 3f
                val r2 = this.size.width / 2f - 9f
                val cx = this.size.width / 2f
                drawLine(
                    tickColor.copy(alpha = 0.55f),
                    Offset(cx + kotlin.math.cos(a).toFloat() * r1, cx + kotlin.math.sin(a).toFloat() * r1),
                    Offset(cx + kotlin.math.cos(a).toFloat() * r2, cx + kotlin.math.sin(a).toFloat() * r2),
                    strokeWidth = 2.5f
                )
            }
            // ۴) هاله نئونی پیشرفت
            drawArc(
                brush = progressBrush,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(stroke + 10f, cap = StrokeCap.Round),
                alpha = 0.16f
            )
            // ۵) قوس اصلی پیشرفت
            drawArc(
                brush = progressBrush,
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            // ۶) هایلایت استودیویی باریک روی قوس
            if (sweep > 4f) {
                drawArc(
                    color = Color.White.copy(alpha = 0.35f),
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(tl.x + stroke * 0.28f, tl.y + stroke * 0.28f),
                    size = Size(arcSize.width - stroke * 0.56f, arcSize.height - stroke * 0.56f),
                    style = Stroke(2.5f, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = centerColor
            )
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

/**
 * چارت ستونی سه‌بعدی هفتگی: ستون‌های گرادیانی طلا→بنفش با درپوش بیضوی
 * براق (حس استوانه) و بازتاب ملایم زیر خط مبنا + برچسب روزها.
 */
@Composable
fun RoyalBarChart(
    data: List<Pair<String, Long>>,
    modifier: Modifier = Modifier
) {
    // رنگ‌های تم — خوانده شده در کانتکست کامپوزبل پیش از ورود به Canvas
    val cGold = Gold
    val cPrimary = NeonPurple
    val cPrimaryDark = NeonPurpleDark
    val cBarTop = vizitorPalette.accentText
    val cLabelArgb = TextSecondary.toArgb()
    val cValueArgb = Gold.toArgb()
    // فونت فارسی روی برچسب‌های گرافیکی (Canvas بومی) — یکدست با کل برنامه
    val faTypeface = ir.atiran.vizitor.ui.components.rememberFaTypeface()
    // رشد فنری ستون‌ها هنگام ورود 🌱 (یک‌بار — در حالت «سبک» بدون انیمیشن)
    val grow = remember { androidx.compose.animation.core.Animatable(if (VizitorPerf.entranceFx) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (grow.value < 1f) {
            grow.animateTo(
                1f,
                androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 240f)
            )
        }
    }
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val base = h - 40f
        val max = data.maxOf { it.second }.coerceAtLeast(1L).toFloat()
        val bw = w / data.size
        val barW = bw * 0.44f

        // خط مبنا طلایی
        drawLine(cGold.copy(alpha = 0.7f), Offset(8f, base), Offset(w - 8f, base), 2f)

        data.forEachIndexed { i, entry ->
            val (label, v) = entry
            val hVal = (v / max) * (base - 40f) * grow.value
            val x = i * bw + (bw - barW) / 2f
            val y = base - hVal

            // بازتاب زیر مبنا
            drawRoundRect(
                color = cPrimary.copy(alpha = 0.14f),
                topLeft = Offset(x, base + 5f),
                size = Size(barW, min(hVal * 0.22f, 16f)),
                cornerRadius = CornerRadius(6f)
            )
            // ستون مکعبی سه‌بعدی سلطنتی
            if (hVal > 4f) {
                val depth = 9f
                // وجه کناری تیره (عمق)
                drawPath(
                    Path().apply {
                        moveTo(x + barW, y)
                        lineTo(x + barW + depth, y - depth * 0.6f)
                        lineTo(x + barW + depth, base - depth * 0.6f)
                        lineTo(x + barW, base)
                        close()
                    },
                    cPrimaryDark.copy(alpha = 0.55f)
                )
                // وجه اصلی — گرادیان رنگ اصلی تم
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(cBarTop, cPrimary, cPrimaryDark),
                        startY = y, endY = base
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barW, hVal),
                    cornerRadius = CornerRadius(7f)
                )
                // وجه بالایی روشن‌تر
                drawPath(
                    Path().apply {
                        moveTo(x, y)
                        lineTo(x + depth, y - depth * 0.6f)
                        lineTo(x + barW + depth, y - depth * 0.6f)
                        lineTo(x + barW, y)
                        close()
                    },
                    cBarTop
                )
                // درپوش طلایی درخشان
                drawOval(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFF3D6), cGold)),
                    topLeft = Offset(x + barW * 0.14f, y - 8f),
                    size = Size(barW * 0.72f, 12f)
                )
                // مقدار بالای ستون
                if (v > 0 && grow.value > 0.9f) {
                    drawContext.canvas.nativeCanvas.drawText(
                        v.compactFa(),
                        x + barW / 2f,
                        y - 16f,
                        android.graphics.Paint().apply {
                            color = cValueArgb
                            typeface = faTypeface
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            isFakeBoldText = true
                        }
                    )
                }
            }
            // برچسب روز
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x + barW / 2f,
                h - 12f,
                android.graphics.Paint().apply {
                    color = cLabelArgb
                    typeface = faTypeface
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

/**
 * دونات دوحلقه «نبض» — حلقه بیرونی پیشرفت تارگت (گرادیان سبز نئونی) +
 * حلقه داخلی نسبت همگام‌سازی فاکتورها (گرادیان طلایی) با عمق سه‌بعدی.
 * فوق‌فشرده برای کارت «در یک نگاه» پیشخوان.
 */
@Composable
fun TwinDonutChart(
    outerProgress: Float,
    innerProgress: Float,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    size: Dp = 118.dp
) {
    // رنگ‌های تم — کپچر در کانتکست کامپوزبل پیش از ورود به Canvas
    val haloColor = NeonPurple.copy(alpha = 0.08f)
    val trackColor = DonutTrack
    val cGreenDark = NeonGreenDark
    val cGreen = NeonGreen
    val cGreenHi = Color(0xFFB8FFD9)
    val cGoldDark = GoldDark
    val cGold = Gold
    val p = outerProgress.coerceIn(0f, 1f)
    val q = innerProgress.coerceIn(0f, 1f)
    // انیمیشن یک‌باره پرشدن دو حلقه (در حالت «سبک» آنی)
    val anim by produceState(initialValue = if (VizitorPerf.entranceFx) 0f else 1f, key1 = p, key2 = q) {
        if (!VizitorPerf.entranceFx) { value = 1f; return@produceState }
        var cur = 0f
        val t = maxOf(p, q).coerceAtLeast(0.01f)
        while (cur < 1f) {
            cur = min(cur + 0.03f, 1f)
            value = cur
            kotlinx.coroutines.delay(12)
        }
        value = 1f
    }
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val cx = w / 2f
        val outerStroke = 13.dp.toPx()
        val innerStroke = 10.dp.toPx()
        val gap = 7.dp.toPx()
        val pad = outerStroke / 2f + 4f
        val outerSize = Size(w - pad * 2, w - pad * 2)
        val outerTl = Offset(pad, pad)
        val ip = pad + outerStroke + gap + innerStroke / 2f
        val innerSize = Size(w - ip * 2, w - ip * 2)
        val innerTl = Offset(ip, ip)

        // ۰) هاله ظریف پشت
        drawCircle(
            color = haloColor,
            radius = w / 2f - 2f,
            style = Stroke(26f)
        )
        // ۱) عمق حلقه بیرونی (حس سه‌بعدی)
        drawArc(
            color = Color(0xFF04060A),
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = Offset(pad, pad + 6f), size = outerSize,
            style = Stroke(outerStroke, cap = StrokeCap.Round)
        )
        // ۲) ریل حلقه بیرونی
        drawArc(
            color = trackColor,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = outerTl, size = outerSize,
            style = Stroke(outerStroke, cap = StrokeCap.Round)
        )
        // ۳) ماژوری پیشرفت بیرونی — سبز نئونی (تارگت)
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(cGreenDark, cGreen, cGreenHi, cGreen)
            ),
            startAngle = -90f, sweepAngle = 360f * p * anim, useCenter = false,
            topLeft = outerTl, size = outerSize,
            style = Stroke(outerStroke + 8f, cap = StrokeCap.Round),
            alpha = 0.16f
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(cGreenDark, cGreen, cGreenHi, cGreen)
            ),
            startAngle = -90f, sweepAngle = 360f * p * anim, useCenter = false,
            topLeft = outerTl, size = outerSize,
            style = Stroke(outerStroke, cap = StrokeCap.Round)
        )
        // ۴) ریل حلقه داخلی
        drawArc(
            color = trackColor,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            topLeft = innerTl, size = innerSize,
            style = Stroke(innerStroke, cap = StrokeCap.Round)
        )
        // ۵) ماژور طلایی داخلی — نسبت همگام‌سازی
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(cGoldDark, cGold, Color(0xFFFFF3D6), cGold)
            ),
            startAngle = -90f, sweepAngle = 360f * q * anim, useCenter = false,
            topLeft = innerTl, size = innerSize,
            style = Stroke(innerStroke, cap = StrokeCap.Round)
        )
        // ۶) تیک‌های ظریف ثابت بین دو حلقه
        repeat(12) { i ->
            val a = Math.toRadians((i * 30 - 90).toDouble())
            val r1 = ip - gap / 2f - innerStroke / 2f
            val r2 = r1 - 4f
            drawLine(
                cGold.copy(alpha = 0.30f),
                Offset(cx + kotlin.math.cos(a).toFloat() * r1, cx + kotlin.math.sin(a).toFloat() * r1),
                Offset(cx + kotlin.math.cos(a).toFloat() * r2, cx + kotlin.math.sin(a).toFloat() * r2),
                strokeWidth = 2f
            )
        }
    }
}

/** نشانگر کوچک رنگی وضعیت (سبز/قرمز) برای اعتبار مشتری. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(12.dp)) {
        drawCircle(color = color.copy(alpha = 0.25f), radius = this.size.minDimension / 2f)
        drawCircle(color = color, radius = this.size.minDimension / 4f)
    }
}

/** نمایش فشرده اعداد بزرگ برای برچسب ستون‌ها (م = میلیون ریال). */
private fun Long.compactFa(): String =
    if (this >= 1_000_000L) "${(this / 1_000_000L).toFaNumber()} م"
    else if (this >= 1000L) "${(this / 1000L).toFaNumber()} هـ"
    else this.toFaNumber()

/**
 * جدول سلطنتی فروش هفتگی — سربرگ گرادیانی بنفش، ردیف‌های زبرا با نوار سهم،
 * و ردیف جمع طلایی.
 */
@Composable
fun RoyalTable(data: List<Pair<String, Long>>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.second }.coerceAtLeast(1L)
    val max = data.maxOf { it.second }.coerceAtLeast(1L)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(RoyalSurfaceBrush)
            .border(1.dp, Color(0x33B04BF8), RoundedCornerShape(16.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(NeonPurple, NeonPurpleDark)))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("روز", Modifier.weight(0.7f), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text("فروش روز", Modifier.weight(1.9f), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Text("سهم", Modifier.weight(0.9f), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.End)
        }
        data.forEachIndexed { i, (day, v) ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (i % 2 == 0) Color(0x10FFFFFF) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(day, Modifier.weight(0.7f), color = Color(0xFFE9D5FF), fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1.9f)) {
                    Text(
                        v.toFaPrice(),
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0x1AFFFFFF))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((v.toFloat() / max).coerceIn(0.04f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(NeonPurple, Gold)))
                        )
                    }
                }
                Text(
                    "${(v * 100 / total).toFaNumber()}٪",
                    Modifier.weight(0.9f),
                    color = Gold,
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Gold.copy(alpha = 0.10f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text("جمع هفته", Modifier.weight(0.7f), color = Gold, fontWeight = FontWeight.ExtraBold)
            Text(total.toFaPrice(), Modifier.weight(1.9f), color = Gold, fontWeight = FontWeight.ExtraBold)
            Text("۱۰۰٪", Modifier.weight(0.9f), color = Gold, textAlign = TextAlign.End, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ════════════════ اجزای سه‌بعدی مشترک (نسخه ۱٫۷٫۰ — تم یکدست پیشخوان) ════════════════

/**
 * نشان رتبه سه‌بعدی — گوی گرادیانی براق با لایه عمق تیره زیرین:
 * رتبه ۱ طلایی، ۲ نقره‌ای، ۳ برنزی و رتبه‌های بعد بنفش سلطنتی.
 * استایل یکسان برای جدول بدهی مشتریان و پرفروش‌ترین‌ها.
 */
@Composable
fun RankBadge3D(rank: Int, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val face = when (rank) {
        1 -> listOf(Color(0xFFFFF3D6), Gold, GoldDark)
        2 -> listOf(Color(0xFFF6F9FC), Color(0xFFC9D4E0), Color(0xFF93A1B3))
        3 -> listOf(Color(0xFFF0C9A8), Color(0xFFC98A5B), Color(0xFF8A5327))
        else -> listOf(vizitorPalette.accentText, NeonPurple, NeonPurpleDark)
    }
    val textColor = when (rank) {
        1 -> Color(0xFF5C4200)
        2 -> Color(0xFF2F3A47)
        3 -> Color(0xFF4A2E14)
        else -> Color.White
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        // لایه عمق (سایه سه‌بعدی)
        Box(
            Modifier
                .offset(y = 2.dp)
                .size(size - 3.dp)
                .clip(CircleShape)
                .background(Color(0xFF04060A))
        )
        // رویه گرادیانی براق
        Box(
            Modifier
                .size(size - 3.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(face))
                .border(1.dp, Color(0x66FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                rank.toFaNumber(),
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
    }
}

/**
 * نوار پیشرفت سه‌بعدی (عمق‌دار) — ریل تیره یکدست + پرشدگی گرادیانی +
 * هایلایت نیمه‌بالایی برای حس برجستگی.
 * در جهت چیدمان فارسی (راست‌به‌چپ) از سمت راست پر می‌شود.
 */
@Composable
fun DepthBar(
    fraction: Float,
    fillColors: List<Color>,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(Color(0x1AFFFFFF))
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(Brush.horizontalGradient(fillColors))
            ) {
                // هایلایت نیمه‌بالا — برجستگی سه‌بعدی
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(height / 2)
                        .background(Color.White.copy(alpha = 0.22f))
                )
            }
        }
    }
}


/* ─────────────────────────────────────────────────────────────────────────
 *  بخش ۲٫۱۶٫۰ — نمودارهای سبک پنل مدیریت و گزارش‌ها (بدون کتابخانهٔ بیرونی)
 * ───────────────────────────────────────────────────────────────────────── */

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
