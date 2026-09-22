/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | جعبه‌ابزار لاکچری نسل ۲ (نسخه ۲٫۱۳٫۸)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  اجزای مشترک صفحهٔ اول و «تنظیمات ورود»:
 *    ▸ Modifier.luxFrame      : قاب گرادیانی طلایی با بازتاب شیشه
 *    ▸ Modifier.ornaments     : تزئین چهار گوشه (نشان نجیب‌زادگی)
 *    ▸ Modifier.shimmerSweep  : جاروب نور مورب روی سطوح براق
 *    ▸ LuxOrb                 : گوی سه‌بعدی آیکن با هاله و براقیت
 *    ▸ GoldFlourish           : جداکنندهٔ طلایی با الماس مرکزی و پیچک
 *    ▸ LuxChip                : چیپ هویتی کوچک با هاله
 *    ▸ StepRail               : ریل گام‌ها (اتصال ← سرور ← ورود)
 *    ▸ LuxBanner              : نوار وضعیت لوکس (موفق/هشدار/خطا/اطلاع)
 *  همهٔ جلوه‌ها با VizitorPerf دروازه‌بانی می‌شوند تا روی گوشی ضعیف هم روان بماند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber

// ═══════════════════════ قاب‌ها و تزئین‌های شیشه‌ای ═══════════════════════

/** قاب گرادیانی طلایی دور هر سطح (به‌جای حاشیهٔ تخت). */
@Composable
fun Modifier.luxFrame(
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    accent: Color = vizitorPalette.gold,
    alpha: Float = 0.55f,
    stroke: Dp = 1.dp
): Modifier = border(
    stroke,
    Brush.linearGradient(
        listOf(
            accent.copy(alpha = alpha),
            Color.White.copy(alpha = alpha * 0.30f),
            accent.copy(alpha = alpha * 0.62f)
        )
    ),
    shape
)

/** خط نور استودیویی و بازتاب پایین — عمق واقعی روی هر سطح. */
fun Modifier.glassRelief(
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    top: Color = Color.White.copy(alpha = 0.16f),
    bottom: Color = Color.Black.copy(alpha = 0.22f)
): Modifier = drawBehind {
    val sw = 1.dp.toPx()
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, top, Color.Transparent)
        ),
        start = Offset(0f, sw * 1.2f),
        end = Offset(size.width, sw * 1.2f),
        strokeWidth = sw
    )
    drawLine(
        brush = Brush.horizontalGradient(
            listOf(Color.Transparent, bottom, Color.Transparent)
        ),
        start = Offset(0f, size.height - sw * 1.4f),
        end = Offset(size.width, size.height - sw * 1.4f),
        strokeWidth = sw
    )
}

/**
 * تزئین چهار گوشه — دو خط نازک با یک نگین کوچک در نوک هر گوشه.
 * (همان زبان تصویری «نجیب‌زادگی» که در سرصفحه و کارت هیرو دیده می‌شود.)
 */
@Composable
fun Modifier.ornaments(
    color: Color = vizitorPalette.gold,
    alpha: Float = 0.45f,
    inset: Dp = 9.dp,
    len: Dp = 15.dp,
    stroke: Dp = 1.4.dp
): Modifier = drawBehind {
    val i = inset.toPx()
    val l = len.toPx()
    val sw = stroke.toPx()
    val c = color.copy(alpha = alpha)
    val gem = color.copy(alpha = (alpha + 0.35f).coerceAtMost(1f))

    fun bracket(cx: Float, cy: Float, dx: Float, dy: Float) {
        drawLine(c, Offset(cx, cy), Offset(cx + dx * l, cy), sw, StrokeCap.Round)
        drawLine(c, Offset(cx, cy), Offset(cx, cy + dy * l), sw, StrokeCap.Round)
        drawCircle(gem, radius = sw * 1.15f, center = Offset(cx + dx * l, cy + dy * l))
    }
    if (size.width < l * 3.2f || size.height < l * 3.2f) return@drawBehind
    bracket(i, i, 1f, 1f)
    bracket(size.width - i, i, -1f, 1f)
    bracket(i, size.height - i, 1f, -1f)
    bracket(size.width - i, size.height - i, -1f, -1f)
}

/** جاروب نور مورب روی سطح براق (روی گوشی ضعیف کاملاً خاموش). */
@Composable
fun Modifier.shimmerSweep(
    color: Color = Color.White.copy(alpha = 0.10f),
    bandWidth: Dp = 80.dp,
    periodMillis: Int = 2800
): Modifier {
    val enabled = VizitorPerf.screenFx
    if (!enabled) return this
    val transition = rememberInfiniteTransition(label = "sweep")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepProgress"
    )
    return this.drawWithContent {
        drawContent()
        val w = bandWidth.toPx()
        val cx = -w + (size.width + 2f * w) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, color, Color.Transparent),
                start = Offset(cx - w, size.height),
                end = Offset(cx + w, 0f)
            ),
            topLeft = Offset(cx - w, 0f),
            size = Size(2f * w, size.height)
        )
    }
}

// ═══════════════════════ گوی سه‌بعدی آیکن ═══════════════════════

/**
 * گوی سه‌بعدی: هالهٔ نور + کرهٔ گرادیانی + بازتاب بالا و سایهٔ پایین + حلقهٔ طلایی.
 * آیکن روی سطح کره می‌نشیند و حس برجستگی می‌دهد.
 */
@Composable
fun LuxOrb(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    tint: Color = vizitorPalette.gold,
    contentDescription: String? = null
) {
    val p = vizitorPalette
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // هالهٔ نور پشت گوی
        Canvas(Modifier.fillMaxWidth().height(size)) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(tint.copy(alpha = 0.30f), Color.Transparent),
                    center = center, radius = this.size.minDimension * 0.5f
                ),
                radius = this.size.minDimension * 0.5f
            )
        }
        Box(
            modifier = Modifier
                .size(size * 0.86f)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            lerp(p.surface, tint, 0.45f),
                            p.surfaceDeep,
                            lerp(p.background, Color.Black, 0.35f)
                        )
                    )
                )
                .border(1.2.dp, tint.copy(alpha = 0.75f), CircleShape)
                .glassRelief(CircleShape)
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = lerp(tint, Color.White, 0.35f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(size * 0.46f)
            )
        }
    }
}

// ═══════════════════════ جداکنندهٔ طلایی و چیپ ═══════════════════════

/** جداکنندهٔ طلایی: دو خط با الماس مرکزی و پیچک‌های کوچک دو طرف. */
@Composable
fun GoldFlourish(
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    color: Color = vizitorPalette.gold
) {
    // رنگ‌های پالت باید «قبل» از بلوک رسم (DrawScope) خوانده شوند؛
    // خواندن CompositionLocal داخل لامبای Canvas مجاز نیست.
    val goldDark = vizitorPalette.goldDark
    Canvas(modifier.fillMaxWidth().height(height)) {
        val midY = size.height / 2f
        val cx = size.width / 2f
        val sw = 1.3.dp.toPx()
        val gemR = 3.6.dp.toPx()
        val gap = gemR * 3.4f

        val line = Brush.horizontalGradient(
            listOf(Color.Transparent, color.copy(alpha = 0.85f), color)
        )
        drawLine(
            brush = line, start = Offset(0f, midY), end = Offset(cx - gap, midY),
            strokeWidth = sw, cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(color, color.copy(alpha = 0.85f), Color.Transparent)
            ),
            start = Offset(cx + gap, midY), end = Offset(size.width, midY),
            strokeWidth = sw, cap = StrokeCap.Round
        )
        // پیچک‌های کوچک دو طرف الماس
        val curl = Path().apply {
            moveTo(cx - gap * 0.55f, midY)
            cubicTo(cx - gap * 0.95f, midY - gemR * 1.7f, cx - gap * 1.35f, midY + gemR * 1.7f, cx - gap * 1.7f, midY)
        }
        drawPath(curl, color.copy(alpha = 0.55f), style = Stroke(sw * 0.9f, cap = StrokeCap.Round))
        val curl2 = Path().apply {
            moveTo(cx + gap * 0.55f, midY)
            cubicTo(cx + gap * 0.95f, midY - gemR * 1.7f, cx + gap * 1.35f, midY + gemR * 1.7f, cx + gap * 1.7f, midY)
        }
        drawPath(curl2, color.copy(alpha = 0.55f), style = Stroke(sw * 0.9f, cap = StrokeCap.Round))

        // الماس مرکزی
        val diamond = Path().apply {
            moveTo(cx, midY - gemR)
            lineTo(cx + gemR, midY)
            lineTo(cx, midY + gemR)
            lineTo(cx - gemR, midY)
            close()
        }
        drawPath(
            diamond,
            brush = Brush.linearGradient(
                listOf(Color.White, color, goldDark),
                start = Offset(cx - gemR, midY - gemR),
                end = Offset(cx + gemR, midY + gemR)
            )
        )
    }
}

/** چیپ هویتی کوچک (مثل «اتصال مستقیم SQL» یا امضای میلانو). */
@Composable
fun LuxChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = vizitorPalette.gold,
    textSize: TextUnit = 9.5.sp
) {
    val p = vizitorPalette
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.20f), p.surface.copy(alpha = 0.55f))
                )
            )
            .border(0.8.dp, tint.copy(alpha = 0.42f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = textSize,
                fontWeight = FontWeight.Bold,
                lineHeight = textSize * 1.5f
            ),
            color = lerp(p.textPrimary, tint, 0.35f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═══════════════════════ ریل گام‌ها ═══════════════════════

/**
 * ریل گام‌ها: هر گام یک گرهٔ شماره‌دار با برچسب؛ گام‌های سپری‌شده طلایی و
 * گام جاری با هالهٔ نور و نبض ملایم. (اتصال ← سرور ← ورود)
 */
@Composable
fun StepRail(
    current: Int,
    steps: List<String>,
    modifier: Modifier = Modifier,
    nodeSize: Dp = 30.dp,
    labelSize: TextUnit = 9.5.sp
) {
    val p = vizitorPalette
    val pulse = if (VizitorPerf.screenFx) {
        val tr = rememberInfiniteTransition(label = "stepPulse")
        val v by tr.animateFloat(
            initialValue = 0.55f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "stepPulseV"
        )
        v
    } else 1f

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            steps.forEachIndexed { index, _ ->
                val done = index < current
                val active = index == current
                val lineColor = if (done) p.gold.copy(alpha = 0.85f) else p.glassBorder.copy(alpha = 0.7f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(nodeSize + 6.dp)
                        .drawBehind {
                            val midY = size.height / 2f
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, midY),
                                end = Offset(size.width, midY),
                                strokeWidth = 1.6.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (active) {
                        Canvas(Modifier.size(nodeSize + 14.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(p.gold.copy(alpha = 0.35f * pulse), Color.Transparent)
                                ),
                                radius = size.minDimension / 2f
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(nodeSize)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> Brush.linearGradient(listOf(p.goldHighlight, p.gold, p.goldDark))
                                    active -> Brush.linearGradient(listOf(p.btnPrimaryTop, p.btnPrimaryBottom))
                                    else -> Brush.linearGradient(listOf(p.surfaceDeep, p.background))
                                }
                            )
                            .border(
                                if (active || done) 1.4.dp else 1.dp,
                                if (done || active) p.goldHighlight.copy(alpha = 0.85f) else p.glassBorder,
                                CircleShape
                            )
                            .glassRelief(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color(0xFF241A02),
                                modifier = Modifier.size(nodeSize * 0.52f)
                            )
                        } else {
                            Text(
                                (index + 1).toFaNumber(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = (nodeSize.value * 0.40f).sp,
                                    fontWeight = FontWeight.Black
                                ),
                                color = if (active) p.onPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            steps.forEachIndexed { index, label ->
                val active = index == current
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = labelSize,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (active) p.goldHighlight else TextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════ نوار وضعیت لوکس ═══════════════════════

/** تُن نوار وضعیت. */
enum class LuxTone { SUCCESS, WARN, DANGER, INFO }

/**
 * نوار وضعیت: نوار رنگی نازک کنار، گوی آیکن درخشان، تیتر و توضیح فارسی.
 * روی صفحهٔ کوتاه خودش را جمع می‌کند ولی هرگز بریده نمی‌شود.
 */
@Composable
fun LuxBanner(
    tone: LuxTone,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    compact: Boolean = false
) {
    val p = vizitorPalette
    val accent = when (tone) {
        LuxTone.SUCCESS -> p.accent
        LuxTone.WARN -> p.gold
        LuxTone.DANGER -> p.danger
        LuxTone.INFO -> p.primary
    }
    val glyph = icon ?: when (tone) {
        LuxTone.SUCCESS -> Icons.Filled.Check
        LuxTone.WARN -> Icons.Filled.Warning
        LuxTone.DANGER -> Icons.Filled.Warning
        LuxTone.INFO -> Icons.Filled.Info
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(accent.copy(alpha = 0.20f), p.surface.copy(alpha = 0.82f))
                )
            )
            .luxFrame(shape, accent, 0.6f)
            .glassRelief(shape)
            .padding(
                horizontal = if (compact) 10.dp else 12.dp,
                vertical = if (compact) 9.dp else 11.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // نوار رنگی کنار (نشان وضعیت)
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (compact) 30.dp else 38.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f))))
        )
        Spacer(Modifier.width(10.dp))
        LuxOrb(glyph, size = if (compact) 30.dp else 36.dp, tint = accent)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.5.sp else 13.5.sp,
                    lineHeight = if (compact) 18.sp else 20.sp
                ),
                color = lerp(p.textPrimary, accent, 0.25f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (message.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (compact) 10.sp else 11.sp,
                        lineHeight = if (compact) 16.sp else 17.sp
                    ),
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
