/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | معماری کریستال + بکگراند ابریشمی (نسخه ۱٫۹٫۰)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  ▸ کارت‌های کریستالی: گرادیان عمودی ظریف + خط نور استودیویی — بدون هاله دایره
 *  ▸ بکگراند «ابریشم روان»: موج‌های سه‌بعدی حرکت‌کننده + غبار طلایی درخشان
 *  ▸ ShimmerBorder: حاشیه نور متحرک برای کارت‌های سلطنتی
 *  همه رنگ‌ها از LocalVizitorPalette پیروی می‌کنند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import ir.atiran.vizitor.R
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.ui.theme.DarkSlate
import ir.atiran.vizitor.ui.theme.DarkSlateDeep
import ir.atiran.vizitor.ui.theme.DarkSlateElevated
import ir.atiran.vizitor.ui.theme.GlassBorder
import ir.atiran.vizitor.ui.theme.GlassFill
import ir.atiran.vizitor.ui.theme.GlassHighlight
import ir.atiran.vizitor.perf.GfxLevel
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.theme.vizitorPalette
import kotlin.math.sin
import kotlin.random.Random

/**
 * کارت کریستالی: گرادیان عمودی خیلی ظریف روی سطح تم + خط نور استودیویی بالا
 * + حاشیه شیشه‌ای پالت. (هاله‌های دایره‌ای نسخه‌های قبل حذف شدند)
 */
@Composable
fun Modifier.glassPanel(
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp
): Modifier {
    val surface = DarkSlateElevated
    val surfaceDeep = DarkSlateDeep
    val streak = GlassHighlight
    return this
        .clip(shape)
        .drawBehind {
            // گرادیان عمق کریستالی (بالا روشن‌تر، پایین عمیق‌تر)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surface,
                        androidx.compose.ui.graphics.lerp(surface, surfaceDeep, 0.55f)
                    )
                )
            )
            // خط نور استودیویی بالای کارت
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(streak, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.30f
                )
            )
        }
        .border(borderWidth, borderColor, shape)
}

/** کارت کریستالی آماده استفاده با پدینگ داخلی استاندارد و ورود سینمایی. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    borderColor: Color = GlassBorder,
    content: @Composable BoxScope.() -> Unit
) {
    // ✨ ورود سینمایی: محو + بالاآمدن + بزرگ‌نمایی ملایم (یک‌بار، سبک و روان)
    // در حالت گرافیکی «سبک» (گوشی‌های اقتصادی) کاملاً حذف می‌شود تا لگ صفر شود.
    val enter = remember { androidx.compose.animation.core.Animatable(if (VizitorPerf.entranceFx) 0f else 1f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (enter.value < 1f) {
            enter.animateTo(
                1f,
                androidx.compose.animation.core.tween(
                    durationMillis = if (VizitorPerf.level == GfxLevel.HIGH) 450 else 300,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                val e = enter.value
                this.alpha = e
                val s = 0.955f + 0.045f * e
                scaleX = s
                scaleY = s
                translationY = (1f - e) * 30f
            }
            .glassPanel(shape = shape, borderColor = borderColor)
            .padding(16.dp),
        content = content
    )
}

/** درخشش نئونی دور کارت (برای موارد مهم). */
fun Modifier.neonGlow(color: Color, radius: Dp = 18.dp): Modifier = this
    .shadow(radius, RoundedCornerShape(22.dp), ambientColor = color, spotColor = color)

/** حاشیه طلایی مخصوص مشتریان/کالاهای VIP. */
fun Modifier.goldBorder(shape: RoundedCornerShape = RoundedCornerShape(22.dp)): Modifier =
    this.border(1.5.dp, Color(0xB3FFD166), shape)

/**
 * ✨ حاشیه نور متحرک (Shimmer Border) — باریکه نور طلایی که آرام دور کارت
 * سفر می‌کند. امضای بصری لوکس نسخه جدید؛ سبک و فقط یک لایه رسم.
 */
@Composable
fun Modifier.shimmerBorder(
    shape: Shape = RoundedCornerShape(22.dp),
    width: Dp = 1.5.dp,
    periodMs: Int = 3400
): Modifier {
    // حالت سازگار: روی میان‌رده و پایین‌تر، حاشیه نور ثابت (بدون انیمیشن دائمی)
    if (!VizitorPerf.listFx) {
        val p0 = vizitorPalette
        return this.border(
            width,
            Brush.linearGradient(listOf(p0.primary, p0.goldHighlight, p0.primary)),
            shape
        )
    }
    val transition = rememberInfiniteTransition(label = "shimmerBorder")
    val t by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "borderPhase"
    )
    val p = vizitorPalette
    return this.drawBehind {
        val w = size.width
        val band = w * 0.42f
        val x = t * (w + band * 2) - band
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    p.primaryDark.copy(alpha = 0.85f),
                    p.primary,
                    p.gold.copy(alpha = 0.95f),
                    p.goldHighlight,
                    p.gold.copy(alpha = 0.95f),
                    p.primary,
                    p.primaryDark.copy(alpha = 0.85f)
                ),
                start = Offset(x - band / 2, 0f),
                end = Offset(x + band / 2, size.height)
            ),
            style = Stroke(width.toPx()),
            cornerRadius = CornerRadius(20.dp.toPx())
        )
    }
}

// ── بکگراند ابریشمی ─────────────────────────────────────────────────────────

/** نقاط غبار طلایی (ثابت با سید؛ بدون هزینه محاسباتی در هر فریم). */
private data class Stardust(val x: Float, val y: Float, val r: Float, val speed: Float)

private val stardust: List<Stardust> = run {
    val rnd = Random(1404)
    List(26) {
        Stardust(
            x = rnd.nextFloat(),
            y = rnd.nextFloat(),
            r = 1.2f + rnd.nextFloat() * 2.3f,
            speed = 0.6f + rnd.nextFloat() * 1.4f
        )
    }
}


/** رسم بافت چرم به‌صورت مرکزی‌پوشان — مثل background-size: cover. */
private fun DrawScope.drawLeatherCover(leather: ImageBitmap) {
    val iw = leather.width.toFloat()
    val ih = leather.height.toFloat()
    val cover = maxOf(size.width / iw, size.height / ih)
    val dw = (iw * cover).toInt()
    val dh = (ih * cover).toInt()
    drawImage(
        image = leather,
        dstOffset = IntOffset(((size.width - dw) / 2f).toInt(), ((size.height - dh) / 2f).toInt()),
        dstSize = IntSize(dw, dh)
    )
}

/**
 * ✨ بکگراند «ابریشم روان» — جایگزین هاله‌های دایره‌ای قدیمی:
 * سه موج ابریشمی بزرگ و کم‌رنگ (رنگ اصلی، طلایی، لهجه) که آرام سر می‌خورند
 * + ۲۶ ذره غبار طلایی چشمک‌زن + گرادیان عمقی پس‌زمینه تم.
 * فقط ۳ انیمیشن سبک در لایه رسم — روان روی همه گوشی‌ها.
 */
@Composable
fun Modifier.dashboardBackdrop(): Modifier {
    val bg = DarkSlate
    val bgDeep = DarkSlateDeep
    val p = vizitorPalette
    val dust = GlassFill
    // 🪶 بافت چرم اختصاصی: تم تاریک ← چرم مشکی | تم روشن ← چرم عاج روشن
    val isDarkBg = p.background.luminance() < 0.5f
    val leather: ImageBitmap = ImageBitmap.imageResource(
        if (isDarkBg) R.drawable.leather_dark else R.drawable.leather_light
    )
    // پوشش نیمه‌شفاف گرادیان تا چرم نفس بکشد ولی متن‌ها خوانا بمانند
    val veil = if (isDarkBg) 0.80f else 0.84f

    // ── حالت سبک (گوشی‌های اقتصادی): بکگراند کاملاً استاتیک و فوق‌سبک ──
    // بدون هیچ انیمیشن دائمی — فقط یک‌بار رسم گرادیان و یک موج ثابت.
    if (!VizitorPerf.screenFx) {
        val sheenTop = p.goldHighlight
        val waveColor = p.primary
        return this.drawBehind {
            val w = size.width
            val h = size.height
            // ۰) بافت چرم زیبا در پایه
            drawLeatherCover(leather)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        bgDeep.copy(alpha = veil),
                        bg.copy(alpha = veil - 0.02f),
                        bg.copy(alpha = veil + 0.02f)
                    )
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(sheenTop.copy(alpha = 0.04f), Color.Transparent),
                    startY = 0f,
                    endY = h * 0.35f
                )
            )
            val baseY = h * 0.30f
            val amp = h * 0.045f
            val path = Path()
            path.moveTo(-40f, baseY)
            val segments = 6
            for (s in 1..segments) {
                val x = -40f + (w + 80f) * s / segments
                val y = baseY + sin(s * 1.05f) * amp
                val prevX = -40f + (w + 80f) * (s - 1) / segments
                val prevY = baseY + sin((s - 1) * 1.05f) * amp
                path.quadraticTo((prevX + x) / 2f, prevY + (prevY - y) * 0.35f, x, y)
            }
            path.lineTo(w + 40f, h + 40f)
            path.lineTo(-40f, h + 40f)
            path.close()
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(waveColor.copy(alpha = 0.05f), Color.Transparent),
                    startY = baseY - amp,
                    endY = baseY + h * 0.35f
                )
            )
            drawRoundRect(color = dust, topLeft = Offset.Zero, size = size, cornerRadius = CornerRadius.Zero)
        }
    }

    // سطوح «متعادل» و «کامل» — پرچمدارها پرتو نور و غبار کامل می‌گیرند
    val beamEnabledLevel = VizitorPerf.beams
    val dustCountLevel = VizitorPerf.stardustCount

    val transition = rememberInfiniteTransition(label = "silk")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "silkPhase"
    )
    val twinkle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinklePhase"
    )

    return this.drawBehind {
        val w = size.width
        val h = size.height

        // ۰) بافت چرم زیبا در پایه
        drawLeatherCover(leather)

        // ۱) پرده گرادیان نیمه‌شفاف (بالا عمیق‌تر ← پایین روشن‌تر جزئی) — چرم زیرش دیده می‌شود
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    bgDeep.copy(alpha = veil),
                    bg.copy(alpha = veil - 0.02f),
                    bg.copy(alpha = veil + 0.02f)
                )
            )
        )

        // ۲) موج‌های ابریشمی روان (بدون دایره!)
        val silkColors = listOf(
            p.primary.copy(alpha = 0.055f),
            p.gold.copy(alpha = 0.045f),
            p.accent.copy(alpha = 0.045f)
        )
        silkColors.forEachIndexed { band, color ->
            val baseY = h * (0.22f + band * 0.26f)
            val amp = h * (0.045f + band * 0.012f)
            val drift = phase * (1f - band * 0.18f) + band * 2.1f
            val path = Path()
            path.moveTo(-40f, baseY + sin(drift) * amp)
            val segments = 6
            for (s in 1..segments) {
                val x = -40f + (w + 80f) * s / segments
                val y = baseY + sin(drift + s * 1.05f) * amp
                val prevX = -40f + (w + 80f) * (s - 1) / segments
                val prevY = baseY + sin(drift + (s - 1) * 1.05f) * amp
                path.quadraticTo(
                    (prevX + x) / 2f, prevY + (prevY - y) * 0.35f,
                    x, y
                )
            }
            path.lineTo(w + 40f, h + 40f)
            path.lineTo(-40f, h + 40f)
            path.close()
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(color, Color.Transparent),
                    startY = baseY - amp,
                    endY = baseY + h * 0.35f
                )
            )
        }

        // ۳) پرتوهای نور خداگونه مورب — دو ستون نور پهن و بسیار کم‌رنگ که آرام می‌لغزند
        repeat(2) { beam ->
            val beamT = (sin(phase * 0.5f + beam * 2.6f) + 1f) / 2f
            val cx = w * (0.18f + beam * 0.5f) + (beamT - 0.5f) * w * 0.16f
            val bw = w * 0.15f
            val slope = h * 0.5f
            drawPath(
                path = Path().apply {
                    moveTo(cx - bw / 2f, -40f)
                    lineTo(cx + bw / 2f, -40f)
                    lineTo(cx + bw / 2f - slope, h + 40f)
                    lineTo(cx - bw / 2f - slope, h + 40f)
                    close()
                },
                brush = Brush.verticalGradient(
                    colors = listOf(
                        p.goldHighlight.copy(alpha = 0.026f + beamT * 0.018f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                )
            )
        }

        // ۴) غبار طلایی چشمک‌زن — تعداد سازگار با قدرت دستگاه
        if (dustCountLevel > 0) stardust.take(dustCountLevel).forEach { star ->
            val tw = (sin(twinkle * star.speed + star.x * 12f) + 1f) / 2f
            val alpha = 0.10f + tw * 0.45f
            val cx = star.x * w
            val cy = star.y * h
            drawCircle(p.gold.copy(alpha = alpha * 0.35f), radius = star.r * 2.6f, center = Offset(cx, cy))
            drawCircle(p.goldHighlight.copy(alpha = alpha), radius = star.r, center = Offset(cx, cy))
        }

        // ۵) لایه غبار ملایم یکپارچه‌کننده
        drawRoundRect(color = dust, topLeft = Offset.Zero, size = size, cornerRadius = CornerRadius.Zero)
    }
}
