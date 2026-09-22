/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | کیت بصری «لاکچری سه‌بعدی» (Splash Kit)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  اجزای اختصاصی صفحهٔ اصلی و صفحهٔ اتصال:
 *    ▸ SatinBackdrop   : بک‌گراند عمیق + هالهٔ نور + کمان‌های طلایی + غبار
 *    ▸ GradientTitle   : تیتر گرادیانی با عمق (سایهٔ نرم زیر متن)
 *    ▸ IconOrb3D       : گوی سه‌بعدی آیکن (رویه گرادیانی + رینگ + درخشش بالا)
 *    ▸ BrandOrb        : گوی نشان برند با مارک سه‌بعدی برنامه
 *    ▸ VizitorField    : فیلد ورودی شیشه‌ای هم‌رنگ تم
 *    ▸ LuxuryTile      : کاشی لمس‌پذیر لاکچری (نقش‌ها و بخش‌های اصلی)
 *    ▸ GoldDivider     : جداکنندهٔ نورانی
 *    ▸ SegmentedChip   : چیپ وضعیت با چراغ نورانی
 *  همه جلوه‌ها لایه‌ای و سبک هستند (بدون blur/shadow سنگین) و روی گوشی‌های
 *  ضعیف به‌طور خودکار ساده می‌شوند (VizitorPerf) — بدون لگ.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.R
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

// ═══════════════════════════ بک‌گراند لاکچری ═══════════════════════════

/**
 * بک‌گراند «ساتن شبانه»: گرادیان عمیق + دو هالهٔ نور نرم + کمان‌های طلایی
 * نازک (حس ساعت لوکس) + پرتوهای مورب + غبار طلایی.
 * حرکت‌ها فقط روی گوشی‌های توانمند (VizitorPerf) و همگی سبک هستند.
 */
@Composable
fun SatinBackdrop(modifier: Modifier = Modifier) {
    val p = vizitorPalette
    val moving = VizitorPerf.screenFx
    val drift = if (moving) {
        val tr = rememberInfiniteTransition(label = "backdropDrift")
        val d by tr.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "backdropDriftV"
        )
        d
    } else 0f

    val base = p.background
    val deep = p.surfaceDeep
    val haloA = p.primary
    val haloB = p.gold
    val arcColor = p.gold
    val dust = p.goldHighlight

    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height

                // ۱) گرادیان پایه (عمق ساتن)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(deep, base, base.copy(alpha = 0.97f))
                    )
                )

                // ۲) هالهٔ نور بالا (بنفش تم) و هالهٔ طلایی پایین — آرام نفس می‌کشند
                val pulse = if (moving) (kotlin.math.sin(drift) + 1f) / 2f else 0.5f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(haloA.copy(alpha = 0.30f + pulse * 0.10f), Color.Transparent),
                        center = Offset(w * 0.18f, -h * 0.02f),
                        radius = w * 1.05f
                    ),
                    radius = w * 1.05f,
                    center = Offset(w * 0.18f, -h * 0.02f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(haloB.copy(alpha = 0.16f - pulse * 0.05f), Color.Transparent),
                        center = Offset(w * 0.92f, h * 0.30f),
                        radius = w * 0.85f
                    ),
                    radius = w * 0.85f,
                    center = Offset(w * 0.92f, h * 0.30f)
                )

                // ۳) کمان‌های نازک طلایی (حس ساعتِ لوکس) — سه حلقهٔ هم‌مرکز
                val cx = w * 0.5f
                val cy = h * 0.16f
                listOf(0.62f, 0.80f, 1.00f).forEachIndexed { i, f ->
                    val r = w * f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                arcColor.copy(alpha = 0.22f - i * 0.05f),
                                arcColor.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy)
                        ),
                        startAngle = -20f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = Offset(cx - r, cy - r),
                        size = Size(r * 2f, r * 2f),
                        style = Stroke(width = 1.1f.dp.toPx()),
                        alpha = 0.75f
                    )
                }

                // ۴) پرتو نور مورب نرم (یک لایهٔ پهن و کم‌رنگ)
                val beamX = w * (0.55f + (if (moving) kotlin.math.sin(drift * 0.5f) * 0.06f else 0f))
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(beamX - w * 0.16f, -20f)
                        lineTo(beamX + w * 0.16f, -20f)
                        lineTo(beamX + w * 0.16f - h * 0.55f, h + 20f)
                        lineTo(beamX - w * 0.16f - h * 0.55f, h + 20f)
                        close()
                    },
                    brush = Brush.verticalGradient(
                        colors = listOf(p.textPrimary.copy(alpha = 0.030f), Color.Transparent)
                    )
                )

                // ۵) غبار طلایی (ثابت — بدون هزینهٔ فریم)
                if (VizitorPerf.stardustCount > 0) {
                    val spots = listOf(
                        0.08f to 0.18f, 0.22f to 0.42f, 0.36f to 0.10f, 0.62f to 0.26f,
                        0.78f to 0.12f, 0.90f to 0.34f, 0.48f to 0.58f, 0.14f to 0.72f,
                        0.70f to 0.66f, 0.86f to 0.82f, 0.30f to 0.88f, 0.56f to 0.94f
                    )
                    spots.take(VizitorPerf.stardustCount.coerceAtMost(12)).forEachIndexed { i, (fx, fy) ->
                        val r = (1.1f + (i % 3) * 0.7f).dp.toPx()
                        drawCircle(
                            dust.copy(alpha = 0.10f + (i % 4) * 0.05f),
                            radius = r * 3.2f,
                            center = Offset(fx * w, fy * h)
                        )
                        drawCircle(dust.copy(alpha = 0.42f), radius = r, center = Offset(fx * w, fy * h))
                    }
                }
            }
    )
}

/**
 * فونت فارسی برای متن‌هایی که با Canvas بومی (نمودارها، نقشهٔ مسیر، تصویر
 * فاکتور) رسم می‌شوند — تا در همهٔ بخش‌ها یکدست و زیبا باشد.
 */
@Composable
fun rememberFaTypeface(
    weight: FaWeight = FaWeight.Medium
): android.graphics.Typeface? {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val res = when (weight) {
        FaWeight.Regular -> R.font.vazirmatn_regular
        FaWeight.Medium -> R.font.vazirmatn_medium
        FaWeight.Bold -> R.font.vazirmatn_bold
        FaWeight.ExtraBold -> R.font.vazirmatn_extrabold
    }
    return remember(res) {
        androidx.core.content.res.ResourcesCompat.getFont(ctx, res)
    }
}

/** وزن‌های فونت فارسی برای متن‌های بومی. */
enum class FaWeight { Regular, Medium, Bold, ExtraBold }

// ═══════════════════════════ متن‌های لاکچری ═══════════════════════════

/**
 * تیتر گرادیانی با حس عمق: یک لایهٔ تیرهٔ نرم زیر متن (سایهٔ شیک) +
 * لایهٔ اصلی با گرادیان طلایی/تم.
 */
@Composable
fun GradientTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    colors: List<Color> = listOf(vizitorPalette.goldDark, vizitorPalette.gold, vizitorPalette.goldHighlight, vizitorPalette.gold),
    textAlign: TextAlign = TextAlign.Center
) {
    Box(modifier) {
        // سایهٔ نرم زیر متن — عمق سه‌بعدی بدون shadow سنگین
        Text(
            text = text,
            style = style,
            textAlign = textAlign,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier.graphicsLayer { translationY = 1.6f; alpha = 0.6f }
        )
        Text(
            text = text,
            style = style.copy(brush = Brush.horizontalGradient(colors), letterSpacing = 0.sp),
            textAlign = textAlign
        )
    }
}

// ═══════════════════════════ گوی‌های سه‌بعدی ═══════════════════════════

/**
 * گوی سه‌بعدی آیکن: دیسک گرادیانی تم + رینگ طلایی + درخشش شیشه‌ای بالاچپ +
 * هالهٔ نور بیرونی (و تپش نرم روی گوشی‌های توانمند).
 */
@Composable
fun IconOrb3D(
    icon: ImageVector? = null,
    size: Dp = 40.dp,
    tint: Color = vizitorPalette.gold,
    glowColor: Color = vizitorPalette.primary,
    cornerRadius: Dp = size / 2,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
) {
    val p = vizitorPalette
    val shape = RoundedCornerShape(cornerRadius)
    // اندازهٔ پیکسلی پیش از ورود به لایهٔ رسم (در DrawScope نام size به Size اشاره می‌کند)
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx() }
    val breathe = if (VizitorPerf.listFx) {
        val tr = rememberInfiniteTransition(label = "orbBreathe")
        val b by tr.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "orbBreatheV"
        )
        b
    } else 0.5f

    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // هالهٔ نور پشت گوی
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = 0.30f + breathe * 0.16f), Color.Transparent),
                        center = center,
                        radius = sizePx * 1.15f
                    ),
                    radius = sizePx * 1.15f,
                    center = center
                )
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.lerp(p.primary, Color.White, 0.30f),
                        p.primary,
                        p.primaryDark
                    )
                )
            )
            .drawWithContent {
                drawContent()
                // درخشش شیشه‌ای بالا (حس برجستگی)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(sizePx * 0.32f, sizePx * 0.24f),
                        radius = sizePx * 0.55f
                    ),
                    radius = sizePx * 0.55f,
                    center = Offset(sizePx * 0.32f, sizePx * 0.24f)
                )
                // لبهٔ پایین تیره (حس عمق)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.28f)),
                        startY = sizePx * 0.55f,
                        endY = sizePx
                    )
                )
            }
            .border(
                1.2.dp,
                Brush.linearGradient(
                    listOf(p.goldHighlight, p.gold.copy(alpha = 0.75f), p.goldDark.copy(alpha = 0.6f))
                ),
                shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) content()
        else if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.50f))
        }
    }
}

/** گوی نشان برند — مارک سه‌بعدی برنامه (آیکن نسخهٔ جدید) روی گوی جواهر. */
@Composable
fun BrandOrb(size: Dp = 58.dp, modifier: Modifier = Modifier) {
    val p = vizitorPalette
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx() }
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(p.gold.copy(alpha = 0.26f), Color.Transparent),
                        center = center,
                        radius = sizePx * 0.95f
                    ),
                    radius = sizePx * 0.95f,
                    center = center
                )
            }
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(p.primary, Color.White, 0.34f),
                        p.primary,
                        p.primaryDark
                    )
                )
            )
            .border(
                1.4.dp,
                Brush.linearGradient(listOf(p.goldHighlight, p.gold, p.goldDark)),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.brand_mark),
            contentDescription = "نشان آتیران ویزیتور",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size * 0.80f)
                .graphicsLayer { translationY = -0.5f }
        )
    }
}

// ═══════════════════════════ کاشی و چیپ ═══════════════════════════

/**
 * کاشی لاکچری لمس‌پذیر: سطح شیشه‌ای + رینگ گرادیانی + براقیت بالا +
 * فرو رفتن سه‌بعدی هنگام لمس + هالهٔ نور هنگام فشار.
 */
@Composable
fun LuxuryTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    enabled: Boolean = true,
    accent: Color = vizitorPalette.gold,
    content: @Composable () -> Unit
) {
    val p = vizitorPalette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Box(
        modifier = modifier
            .press3D(depth = 3.dp)
            .drawBehind {
                // هالهٔ طلایی هنگام لمس
                if (pressed) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.maxDimension * 0.75f
                        ),
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        p.textPrimary.copy(alpha = if (pressed) 0.16f else 0.115f),
                        p.surface.copy(alpha = 0.55f),
                        p.surfaceDeep.copy(alpha = 0.35f)
                    )
                )
            )
            .drawWithContent {
                drawContent()
                // براقیت بالای کاشی (رفلکس نور)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.13f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.42f
                    )
                )
                // درخشش هنگام لمس
                if (pressed) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.16f), Color.Transparent),
                            startY = 0f,
                            endY = size.height
                        )
                    )
                }
            }
            .border(
                if (pressed) 1.6.dp else 1.1.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = if (pressed) 0.95f else 0.55f),
                        p.primary.copy(alpha = 0.35f),
                        p.goldDark.copy(alpha = 0.30f)
                    )
                ),
                shape
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** جداکنندهٔ نورانی طلایی با نگین وسط. */
@Composable
fun GoldDivider(modifier: Modifier = Modifier, label: String? = null) {
    val p = vizitorPalette
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, p.gold.copy(alpha = 0.75f))))
        )
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(p.goldHighlight)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = p.gold
            )
            Spacer(Modifier.width(7.dp))
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(p.goldHighlight)
            )
            Spacer(Modifier.width(8.dp))
        } else Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(p.gold.copy(alpha = 0.75f), Color.Transparent)))
        )
    }
}

/** چیپ وضعیت با چراغ نورانی — هم‌رنگ تم. */
@Composable
fun GlowChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    pulse: Boolean = false
) {
    val alpha = if (pulse && VizitorPerf.screenFx) {
        val tr = rememberInfiniteTransition(label = "chipPulse")
        val a by tr.animateFloat(
            initialValue = 0.45f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chipPulseV"
        )
        a
    } else 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f * alpha), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
        if (icon != null) {
            Spacer(Modifier.width(6.dp))
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = color
        )
    }
}

// ═══════════════════════════ فیلد ورودی شیشه‌ای ═══════════════════════════

/**
 * فیلد ورودی هم‌رنگ تم: قاب گرد، آیکن در گوی کوچک، برچسب طلایی در فوکوس.
 * (به‌جای فیلد پیش‌فرض متریال که با تم برنامه ناهماهنگ بود)
 */
@Composable
fun VizitorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp)
) {
    val p = vizitorPalette
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = shape,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = p.textPrimary, lineHeight = 20.sp),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        leadingIcon = if (icon != null) {
            {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(p.primary.copy(alpha = 0.16f))
                        .border(1.dp, p.primary.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = p.gold, modifier = Modifier.size(16.dp))
                }
            }
        } else null,
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.gold,
            unfocusedBorderColor = p.glassBorder,
            errorBorderColor = p.danger,
            focusedLabelColor = p.gold,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = p.textPrimary,
            unfocusedTextColor = p.textPrimary,
            cursorColor = p.gold,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedLeadingIconColor = p.gold,
            unfocusedLeadingIconColor = p.gold.copy(alpha = 0.75f)
        )
    )
}

/** ردیف برچسب+مقدار شیک (برای پنل‌های اطلاعاتی). */
@Composable
fun InfoPill(
    label: String,
    value: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(p.textPrimary.copy(alpha = 0.055f))
            .border(1.dp, p.glassBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = p.gold, modifier = Modifier.size(13.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = p.textPrimary
        )
    }
}
