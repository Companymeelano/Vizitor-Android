/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران | جعبه‌ابزار دکمه‌ها و ویجت‌های سه‌بعدی (v2.24.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل «حس سه‌بعدی واقعی» را به رابط کاربری می‌آورد:
 *
 *    ▸ Lux3DButton      — دکمهٔ برجستهٔ چندلایه: وجه گرادیانی + لبهٔ ضخامت +
 *                          هایلایت شیشه‌ای + سایهٔ رنگی؛ هنگام لمس فرو می‌رود
 *                          (چرخش ملایم روی محور X) و مثل دکمهٔ فیزیکی برمی‌گردد.
 *    ▸ Lux3DIconButton  — دکمهٔ گرد سه‌بعدی با حلقهٔ نور و شمارندهٔ هشدار.
 *    ▸ Lux3DTile        — کاشی سه‌بعدی (برای دسته‌بندی بخش‌ها) با عمق و فشار.
 *    ▸ Lux3DCategoryBar — نوار «دسته‌بندی» بخش‌ها: هر دسته یک کاشی برجسته.
 *    ▸ Lux3DGauge       — گیج شعاعی سه‌بعدی برای «سلامت سرور» (۰ تا ۱۰۰).
 *    ▸ Lux3DStatusPill  — نشان وضعیت با لبهٔ برجسته (سالم/هشدار/خطا/در حال بررسی).
 *    ▸ Lux3DCheckRow    — ردیف بررسی: چراغ سه‌بعدی + عنوان + مقدار + توضیح.
 *
 *  همهٔ رنگ‌ها از پالت فعال برنامه (`vizitorPalette`) خوانده می‌شود و برای
 *  «حرکت» از انیمیشن فنری استفاده شده — بدون هیچ کتابخانهٔ تازه.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.atiran.vizitor.ui.theme.vizitorPalette
import kotlin.math.cos
import kotlin.math.sin

// ═══════════════════════════ رنگ‌ها و پوستهٔ سه‌بعدی ═══════════════════════════

/** لحن رنگی ویجت‌های سه‌بعدی. */
enum class Lux3DTone { GOLD, GREEN, BLUE, RED, NIGHT }

/** سطح وضعیت برای نشان‌ها و چراغ‌های بررسی. */
enum class Lux3DLevel { OK, WARN, BAD, IDLE, RUN }

/** پوستهٔ سه‌بعدی: وجه بالا/پایین، لبهٔ ضخامت، هاله و رنگ متن. */
data class Lux3DSkin(
    val faceTop: Color,
    val faceBottom: Color,
    val edge: Color,
    val glow: Color,
    val ink: Color,
    val ring: Color,
)

/** ساخت پوستهٔ سه‌بعدی از لحن انتخابی (همه از پالت فعال برنامه). */
@Composable
fun lux3dSkin(tone: Lux3DTone): Lux3DSkin {
    val p = vizitorPalette
    return when (tone) {
        Lux3DTone.GOLD -> Lux3DSkin(
            faceTop = p.goldHighlight,
            faceBottom = p.gold,
            edge = p.goldDark,
            glow = p.gold.copy(alpha = 0.42f),
            ink = Color(0xFF241708),
            ring = p.goldHighlight.copy(alpha = 0.75f),
        )
        Lux3DTone.GREEN -> Lux3DSkin(
            faceTop = Color(0xFF8CE39A),
            faceBottom = Color(0xFF2E9A47),
            edge = Color(0xFF14401F),
            glow = MaGreen.copy(alpha = 0.40f),
            ink = Color(0xFF07210E),
            ring = Color(0xFFB8F2C0).copy(alpha = 0.70f),
        )
        Lux3DTone.BLUE -> Lux3DSkin(
            faceTop = Color(0xFF8FB9FF),
            faceBottom = Color(0xFF2E5CC8),
            edge = Color(0xFF152B60),
            glow = Color(0xFF4D8BFF).copy(alpha = 0.40f),
            ink = Color(0xFF061229),
            ring = Color(0xFFBBD4FF).copy(alpha = 0.70f),
        )
        Lux3DTone.RED -> Lux3DSkin(
            faceTop = Color(0xFFFF9A88),
            faceBottom = Color(0xFFC03A2C),
            edge = Color(0xFF611710),
            glow = MaRed.copy(alpha = 0.40f),
            ink = Color(0xFF2A0603),
            ring = Color(0xFFFFC9BE).copy(alpha = 0.70f),
        )
        Lux3DTone.NIGHT -> Lux3DSkin(
            faceTop = p.surface,
            faceBottom = p.surfaceDeep,
            edge = Color(0xFF04070C),
            glow = p.primary.copy(alpha = 0.30f),
            ink = p.textPrimary,
            ring = p.glassBorder,
        )
    }
}

/** پوستهٔ متناسب با هر سطح وضعیت (سالم/هشدار/خطا/بررسی/بی‌خبر). */
@Composable
fun lux3dLevelSkin(level: Lux3DLevel): Lux3DSkin = when (level) {
    Lux3DLevel.OK -> lux3dSkin(Lux3DTone.GREEN)
    Lux3DLevel.WARN -> lux3dSkin(Lux3DTone.GOLD)
    Lux3DLevel.BAD -> lux3dSkin(Lux3DTone.RED)
    Lux3DLevel.RUN -> lux3dSkin(Lux3DTone.BLUE)
    Lux3DLevel.IDLE -> Lux3DSkin(
        faceTop = Color(0xFF9AA6B8),
        faceBottom = Color(0xFF5A6577),
        edge = Color(0xFF2A3140),
        glow = Color(0xFF7A8598).copy(alpha = 0.30f),
        ink = Color(0xFF10151D),
        ring = Color.White.copy(alpha = 0.25f),
    )
}

/** انیمیشن فشار با فنر (مشترک همهٔ ویجت‌ها). */
@Composable
private fun pressAnim(pressed: Boolean, enabled: Boolean = true): Float {
    val v by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
    )
    return v
}

// ═══════════════════════════════ دکمهٔ برجسته ═══════════════════════════════

/**
 * دکمهٔ سه‌بعدی برجسته — قلب ظاهر تازهٔ برنامه.
 *
 * هنگام لمس: وجه دکمه به‌اندازهٔ ضخامت پایین می‌رود (لبهٔ زیرش پنهان می‌شود)،
 * کمی روی محور X می‌چرخد و سایه جمع می‌شود؛ مثل فشار دادن یک کلید فیزیکی.
 *
 * @param tone لحن رنگی ([Lux3DTone]).
 * @param depth ضخامت بدنهٔ دکمه.
 * @param badge متن کوچک گوشهٔ دکمه (مثلاً «خودکار»).
 */
@Composable
fun Lux3DButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tone: Lux3DTone = Lux3DTone.GOLD,
    badge: String? = null,
    enabled: Boolean = true,
    busy: Boolean = false,
    depth: Dp = 8.dp,
    tilt: Boolean = true,
) {
    val skin = lux3dSkin(tone)
    val shape = RoundedCornerShape(20.dp)
    val src = remember { MutableInteractionSource() }
    val pressedState = src.collectIsPressedAsState()
    val pressed = pressAnim(pressedState.value, enabled)

    Column(modifier = modifier.fillMaxWidth()) {
        // ── وجه دکمه (روی لبهٔ ضخامت کشیده می‌شود) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .then(if (tilt && enabled) Modifier.tilt3D(maxTilt = 6f) else Modifier)
                .graphicsLayer {
                    translationY = depth.toPx() * pressed
                    rotationX = 9f * pressed
                    cameraDistance = 16f * density
                    alpha = if (enabled) 1f else 0.5f
                }
                .then(
                    if (enabled) Modifier
                        .shadow(elevation = 16.dp - 9.dp * pressed, shape = shape, clip = false)
                        .clickable(interactionSource = src, indication = null, onClick = onClick)
                    else Modifier
                )
                .clip(shape)
                .background(Brush.verticalGradient(listOf(skin.faceTop, skin.faceBottom)))
                .border(1.dp, skin.ring.copy(alpha = 0.55f), shape)
        ) {
            // هایلایت شیشه‌ای بالای دکمه
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.34f), Color.Transparent)
                        )
                    )
            )
            // هالهٔ رنگی گوشهٔ راست
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(58.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(skin.faceTop.copy(alpha = 0.30f), Color.Transparent)
                        ),
                        CircleShape,
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(6.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.55f),
                                        Color.White.copy(alpha = 0.12f),
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(icon, null, tint = skin.ink, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = skin.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        if (!badge.isNullOrBlank()) {
                            Spacer(Modifier.width(7.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(skin.ink.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    badge,
                                    color = skin.ink.copy(alpha = 0.90f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = skin.ink.copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (busy) "…" else "‹",
                    color = skin.ink.copy(alpha = 0.75f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            // رگهٔ نوری پایین (عمق بیشتر)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.20f))
                        )
                    )
            )
        }
        // ── لبهٔ ضخامت (زیر وجه؛ با فشار دادن پنهان می‌شود) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(depth)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(skin.edge, skin.edge.copy(alpha = 0.86f))))
        )
    }
}

// ═══════════════════════════ دکمهٔ گرد سه‌بعدی ═══════════════════════════

/** دکمهٔ گرد سه‌بعدی با حلقهٔ نور، عمق و فشار برجسته. */
@Composable
fun Lux3DIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    tone: Lux3DTone = Lux3DTone.GOLD,
    badge: Int = 0,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val skin = lux3dSkin(tone)
    val src = remember { MutableInteractionSource() }
    val pressedState = src.collectIsPressedAsState()
    val pressed = pressAnim(pressedState.value, enabled)
    val depth = (size.value * 0.14f).dp

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = depth.toPx() * pressed
                alpha = if (enabled) 1f else 0.5f
            },
        contentAlignment = Alignment.Center,
    ) {
        // بدنهٔ ضخامت
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { translationY = depth.toPx() * (1f - pressed * 0.70f) }
                .clip(CircleShape)
                .background(skin.edge)
        )
        // وجه دکمه
        Box(
            modifier = Modifier
                .size(size)
                .shadow(9.dp - 5.dp * pressed, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(skin.faceTop, skin.faceBottom)))
                .border(1.dp, skin.ring.copy(alpha = 0.60f), CircleShape)
                .then(
                    if (enabled) Modifier.clickable(
                        interactionSource = src,
                        indication = null,
                        onClick = onClick,
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.62f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.42f), Color.Transparent)
                        ),
                        CircleShape,
                    )
            )
            Icon(icon, contentDescription, tint = skin.ink, modifier = Modifier.size(size * 0.46f))
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .background(MaRed, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.50f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("$badge", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════ کاشی سه‌بعدی بخش‌ها ═══════════════════════════

/**
 * کاشی سه‌بعدی برای دسته‌بندی بخش‌ها: آیکون در گویِ برجسته، عنوان، زیرنویس و
 * مقدار. هنگام لمس فرو می‌رود و حالت انتخاب‌شده را با هالهٔ نوری نشان می‌دهد.
 */
@Composable
fun Lux3DTile(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    value: String? = null,
    selected: Boolean = false,
    tone: Lux3DTone = Lux3DTone.NIGHT,
    badge: String? = null,
) {
    val p = vizitorPalette
    val skin = lux3dSkin(tone)
    val shape = RoundedCornerShape(18.dp)
    val src = remember { MutableInteractionSource() }
    val pressedState = src.collectIsPressedAsState()
    val pressed = pressAnim(pressedState.value)
    val depth = 7.dp
    val ringColor = if (selected) skin.faceBottom else p.glassBorder

    Column(modifier = modifier) {
        // ── وجه کاشی ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .graphicsLayer {
                    translationY = depth.toPx() * pressed
                    rotationX = 8f * pressed
                    cameraDistance = 16f * density
                }
                .shadow(if (selected) 14.dp else 8.dp, shape, clip = false)
                .clickable(interactionSource = src, indication = null, onClick = onClick)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (selected) skin.faceTop.copy(alpha = 0.35f) else p.royalSurfaceTop,
                            if (selected) skin.faceBottom.copy(alpha = 0.26f) else p.royalSurfaceBottom,
                        )
                    )
                )
                .border(
                    if (selected) 1.6.dp else 1.dp,
                    ringColor.copy(alpha = if (selected) 0.95f else 0.60f),
                    shape,
                )
                .padding(horizontal = 11.dp, vertical = 11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(5.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(skin.faceTop, skin.faceBottom)))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = skin.ink, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        color = p.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    if (!caption.isNullOrBlank()) {
                        Text(
                            caption,
                            color = p.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                        )
                    }
                }
                if (!value.isNullOrBlank()) {
                    Text(
                        value,
                        color = skin.faceBottom,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
            if (!badge.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(skin.faceBottom.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        badge,
                        color = skin.faceBottom,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        // ── لبهٔ ضخامت کاشی ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(depth)
                .clip(shape)
                .background(skin.edge)
        )
    }
}

// ═══════════════════════════ نوار دسته‌بندی بخش‌ها ═══════════════════════════

/**
 * نوار «دسته‌بندی» — هر دسته یک کاشی برجستهٔ سه‌بعدی با آیکون و زیرنویس.
 * دستهٔ فعال با هالهٔ طلایی مشخص می‌شود.
 */
@Composable
fun Lux3DCategoryBar(
    items: List<MaNavItem>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    captions: Map<String, String> = emptyMap(),
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Lux3DTile(
                title = item.label,
                caption = captions[item.key],
                icon = item.icon,
                selected = item.key == selectedKey,
                tone = if (item.key == selectedKey) Lux3DTone.GOLD else Lux3DTone.NIGHT,
                onClick = { onSelect(item.key) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ═══════════════════════════ گیج شعاعی سلامت ═══════════════════════════

/**
 * گیج سه‌بعدی «سلامت سرور»: حلقهٔ ضخامت تیره + قوس رنگی گرادیانی + نشانگر
 * نقطه‌ای + عدد وسط. مقدار [percent] بین ۰ و ۱۰۰ است.
 */
@Composable
fun Lux3DGauge(
    percent: Float,
    centerText: String,
    modifier: Modifier = Modifier,
    centerSub: String? = null,
    size: Dp = 132.dp,
    tone: Lux3DTone = Lux3DTone.GREEN,
) {
    val p = vizitorPalette
    val skin = lux3dSkin(tone)
    val value by animateFloatAsState(
        targetValue = percent.coerceIn(0f, 100f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow,
        ),
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = this.size.width
            val height = this.size.height
            val stroke = minOf(width, height) * 0.13f
            val inset = stroke / 2f + 2f
            val arcSize = Size(width - inset * 2f, height - inset * 2f)
            val topLeft = Offset(inset, inset)
            // حلقهٔ زیرین (عمق)
            drawArc(
                color = Color.Black.copy(alpha = 0.45f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = p.donutTrack,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 0.82f, cap = StrokeCap.Round),
            )
            // قوس مقدار
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(skin.faceTop.copy(alpha = 0.65f), skin.faceBottom, skin.faceTop)
                ),
                startAngle = 135f,
                sweepAngle = 270f * (value / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 0.82f, cap = StrokeCap.Round),
            )
            // هایلایت شیشه‌ای روی قوس
            drawArc(
                color = Color.White.copy(alpha = 0.28f),
                startAngle = 150f,
                sweepAngle = 250f * (value / 100f),
                useCenter = false,
                topLeft = Offset(inset + stroke * 0.16f, inset + stroke * 0.10f),
                size = Size(arcSize.width - stroke * 0.32f, arcSize.height - stroke * 0.32f),
                style = Stroke(width = stroke * 0.20f, cap = StrokeCap.Round),
            )
            // نشانگر نقطه‌ای سر قوس
            val angle = Math.toRadians((135f + 270f * (value / 100f)).toDouble())
            val cx = width / 2f
            val cy = height / 2f
            val r = arcSize.width / 2f
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = stroke * 0.22f,
                center = Offset(
                    cx + (r * cos(angle)).toFloat(),
                    cy + (r * sin(angle)).toFloat(),
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerText,
                color = skin.faceTop,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
            )
            if (!centerSub.isNullOrBlank()) {
                Text(
                    centerSub,
                    color = p.textSecondary,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ═══════════════════════════ نشان وضعیت برجسته ═══════════════════════════

/** نشان وضعیت با لبهٔ برجسته و چراغ کوچک. */
@Composable
fun Lux3DStatusPill(
    text: String,
    level: Lux3DLevel,
    modifier: Modifier = Modifier,
) {
    val skin = lux3dLevelSkin(level)
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(skin.faceTop.copy(alpha = 0.32f), skin.faceBottom.copy(alpha = 0.20f))
                )
            )
            .border(1.dp, skin.faceBottom.copy(alpha = 0.65f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(skin.faceBottom, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = skin.faceTop, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ═══════════════════════════ ردیف بررسی سلامت ═══════════════════════════

/**
 * ردیف یک «بررسی» در کارت سلامت سرور: چراغ سه‌بعدی، عنوان، مقدار برجسته و
 * توضیح کوتاه.
 */
@Composable
fun Lux3DCheckRow(
    title: String,
    level: Lux3DLevel,
    modifier: Modifier = Modifier,
    value: String? = null,
    detail: String? = null,
) {
    val p = vizitorPalette
    val skin = lux3dLevelSkin(level)
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        // چراغ سه‌بعدی
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(skin.edge),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer { translationY = -1.5f }
                    .shadow(4.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(skin.faceTop, skin.faceBottom)))
                    .border(1.dp, Color.White.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color.White.copy(alpha = 0.85f), CircleShape)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = p.textPrimary,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (!value.isNullOrBlank()) {
                    Text(
                        value,
                        color = skin.faceBottom,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.End,
                    )
                }
            }
            if (!detail.isNullOrBlank()) {
                Text(
                    detail,
                    color = p.textSecondary,
                    fontSize = 10.5.sp,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

/** خط جداکنندهٔ نازک بین ردیف‌های یک گروه. */
@Composable
fun Lux3DDivider(modifier: Modifier = Modifier) {
    val p = vizitorPalette
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, p.glassBorder, Color.Transparent)
                )
            )
    )
}

/** سرتیتر گروه با آیکون سه‌بعدی کوچک و مقدار اختیاری سمت چپ. */
@Composable
fun Lux3DGroupHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    status: String? = null,
) {
    val p = vizitorPalette
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .shadow(4.dp, RoundedCornerShape(9.dp), clip = false)
                .clip(RoundedCornerShape(9.dp))
                .background(Brush.verticalGradient(listOf(p.royalSurfaceTop, p.royalSurfaceBottom)))
                .border(1.dp, p.glassBorder, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = p.gold, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = p.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
        )
        if (!status.isNullOrBlank()) {
            Lux3DStatusPill(status, Lux3DLevel.IDLE)
        }
        if (!trailing.isNullOrBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(trailing, color = p.textSecondary, fontSize = 11.sp)
        }
    }
}
