/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | کیت «لاکچری پرمیوم» (Premium UI Kit 2.13.7)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  این کیت، لایهٔ تازهٔ ظرافت برای صفحهٔ اصلی و بخش ورود/تنظیمات است:
 *    ▸ ScreenFit / rememberScreenFit : اندازهٔ واکنشی چهاررده (کوچک→تبلت)
 *    ▸ Modifier.screenSafePadding    : حاشیهٔ امن نوار وضعیت، ناوبری و بریدگی
 *    ▸ PremiumPanel                  : پنل شیشه‌ای با سرصفحهٔ شماره‌دار
 *    ▸ PremiumButton                 : کلید سه‌بعدی با عمق واقعی و جاروب نور
 *    ▸ PremiumField                  : فیلد بزرگ‌تر با برچسب شناور و هالهٔ فوکوس
 *    ▸ OrbIconButton / StepBadge     : دکمهٔ گوی‌دار و نشان شمارهٔ طلایی
 *    ▸ ToggleRow / StatPill / KeyRow : اجزای تنظیمات ورود و شمارنده‌ها
 *  همه جلوه‌ها لایه‌ای (drawWithContent/drawBehind) و بدون blur/shadow سنگین
 *  هستند؛ روی گوشی ضعیف با VizitorPerf خودکار ساده می‌شوند (بدون لگ).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
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

// ═══════════════════════ اندازهٔ واکنشی صفحه (همهٔ گوشی‌ها) ═══════════════════════

/** ردهٔ اندازهٔ دستگاه — از گوشی کوچک اقتصادی تا گوشی بزرگ و تبلت. */
enum class FitTier { MINI, COMPACT, REGULAR, LARGE, TABLET }

/**
 * اندازه‌های کالیبره‌شدهٔ یک صفحه بر پایهٔ ابعاد واقعی دستگاه (dp).
 * همهٔ صفحه‌های بازطراحی‌شده از همین یک منبع اندازه می‌گیرند تا در هیچ
 * گوشی‌ای نه چیزی بریده شود و نه فضاهای خالی زشت بماند.
 */
@Immutable
class ScreenFit(val width: Dp, val height: Dp) {

    val tier: FitTier = when {
        width >= 600.dp -> FitTier.TABLET
        width >= 430.dp -> FitTier.LARGE
        width < 340.dp || height < 620.dp -> FitTier.MINI
        width < 360.dp -> FitTier.COMPACT
        else -> FitTier.REGULAR
    }

    /** صفحهٔ خوابیده (افقی) — چیدمان باید کوتاه و فشرده شود. */
    val landscape: Boolean = width > height

    /** گوشی خیلی کوچک یا صفحهٔ کوتاه — لمس‌پذیر ولی جمع‌وجور. */
    val tiny: Boolean = width < 360.dp || height < 640.dp

    /** صفحهٔ کوتاه (کمتر از ۷۰۰dp) — فاصله‌های عمودی جمع‌وجور می‌شوند. */
    val short: Boolean = height < 700.dp
    /** صفحهٔ بلند — فضا بازتر می‌شود تا وسط صفحه خالی نماند. */
    val tall: Boolean = height >= 860.dp

    /** حالت فشرده: کوتاه، کوچک یا خوابیده — همهٔ فاصله‌های عمودی جمع می‌شوند. */
    val dense: Boolean = short || tiny || landscape

    /** گوشی باریک (مثل ۳۲۰dp) — متن‌ها یک پله کوچک‌تر می‌شوند. */
    val narrow: Boolean = width < 360.dp
    /** صفحهٔ پهن (تبلت/گوشی بزرگ) — محتوا وسط‌چین و محدود می‌شود. */
    val wide: Boolean = width >= 480.dp

    /** حاشیهٔ افقی محتوا. */
    val pad: Dp = when (tier) {
        FitTier.MINI -> 12.dp
        FitTier.COMPACT -> 14.dp
        FitTier.REGULAR -> 18.dp
        FitTier.LARGE -> 22.dp
        FitTier.TABLET -> 28.dp
    }

    /** فاصلهٔ بین بلوک‌های اصلی صفحه. */
    val gap: Dp = when {
        dense -> 10.dp
        tall -> 20.dp
        else -> 16.dp
    }

    /** فاصلهٔ داخلی کارت‌ها. */
    val inner: Dp = when (tier) {
        FitTier.MINI -> 11.dp
        FitTier.COMPACT -> 12.dp
        else -> 15.dp
    }

    /** بیشینهٔ عرض محتوا — روی صفحهٔ پهن، محتوا کشیده و زشت نمی‌شود. */
    val contentMax: Dp = if (wide) 520.dp else 0.dp

    /** گوی نشان برند در سرصفحه. */
    val brandOrb: Dp = when (tier) {
        FitTier.MINI -> 46.dp
        FitTier.COMPACT -> 52.dp
        FitTier.REGULAR -> 62.dp
        FitTier.LARGE -> 70.dp
        FitTier.TABLET -> 78.dp
    }

    /** قطر گوی آواتار نقش‌ها. */
    val roleAvatar: Dp = when (tier) {
        FitTier.MINI -> 46.dp
        FitTier.COMPACT -> 54.dp
        FitTier.REGULAR -> 64.dp
        FitTier.LARGE -> 72.dp
        FitTier.TABLET -> 80.dp
    }

    /** ردیف‌های شبکهٔ نقش‌ها — دو ستون تا عرض ۶۰۰dp، سپس سه ستون. */
    val roleColumns: Int = if (width >= 600.dp || (landscape && width >= 480.dp)) 3 else 2

    /** ارتفاع کلید اصلی. */
    val buttonHeight: Dp = when {
        dense -> 52.dp
        short -> 54.dp
        tier == FitTier.TABLET || tier == FitTier.LARGE -> 64.dp
        else -> 60.dp
    }

    /** کمینهٔ ارتفاع فیلد ورودی (برای لمس راحت روی همهٔ گوشی‌ها). */
    val fieldHeight: Dp = if (dense) 52.dp else 58.dp

    /** اندازهٔ تیتر نام سامانه. */
    val titleSize: TextUnit = when (tier) {
        FitTier.MINI -> 17.5.sp
        FitTier.COMPACT -> 19.sp
        FitTier.REGULAR -> 22.sp
        FitTier.LARGE -> 24.sp
        FitTier.TABLET -> 26.sp
    }

    /** اندازهٔ تیتر کارت هیرو. */
    val heroSize: TextUnit = when (tier) {
        FitTier.MINI -> 15.5.sp
        FitTier.COMPACT -> 17.sp
        FitTier.REGULAR -> 20.sp
        FitTier.LARGE -> 22.sp
        FitTier.TABLET -> 24.sp
    }

    /** اندازهٔ متن دکمه‌های اصلی. */
    val buttonText: TextUnit = if (narrow) 13.5.sp else 15.5.sp

    /** اندازهٔ عنوان کارت‌ها. */
    val cardTitle: TextUnit = if (narrow) 13.5.sp else 15.5.sp

    /** متن ریز (چیپ هویت، زیرنویس، امضای فوتر). */
    val microText: TextUnit = if (narrow) 8.5.sp else 9.5.sp

    /** بلندی سرصفحهٔ برند — روی صفحهٔ کوتاه جمع می‌شود. */
    val heroInner: Dp = if (dense) 13.dp else 16.dp

    /** جاروب نور و حرکت‌های تکرارشونده — روی دستگاه ضعیف خاموش. */
    val animated: Boolean = VizitorPerf.screenFx
}

/** اندازهٔ صفحه را در لحظهٔ ترکیب می‌خواند (بدون هیچ هزینهٔ اضافی). */
@Composable
fun rememberScreenFit(): ScreenFit {
    val cfg = LocalConfiguration.current
    val w = cfg.screenWidthDp
    val h = cfg.screenHeightDp
    return remember(w, h) { ScreenFit(w.dp, h.dp) }
}

/**
 * حاشیهٔ امن نوار وضعیت / نوار ناوبری / بریدگی دوربین و بلندگو.
 * بدون این، روی بسیاری از گوشی‌ها سرصفحه زیر ساعت و فوتر زیر نوار سیستم
 * می‌رفت؛ با آن، چیدمان روی همهٔ گوشی‌ها و تبلت‌ها درست می‌نشیند.
 */
@Composable
fun Modifier.screenSafePadding(
    edges: WindowInsetsSides = WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
): Modifier = this.windowInsetsPadding(WindowInsets.safeDrawing.only(edges))

// ═══════════════════════════ پنل لاکچری ═══════════════════════════

/**
 * پنل شیشه‌ای لاکچری با سرصفحهٔ شماره‌دار.
 * شمارهٔ طلایی، گوی آیکن، تیتر و زیرنویس دارد و بدنه‌اش هر محتوایی می‌گیرد.
 */
@Composable
fun PremiumPanel(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    step: Int? = null,
    hint: String? = null,
    accent: Color = vizitorPalette.primary,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    inner: Dp = 15.dp,
    trailing: (@Composable () -> Unit)? = null,
    /** اگر مقدار بگیرد، سرصفحهٔ پنل لمس‌پذیر می‌شود (کارت بازشو). */
    onHeaderClick: (() -> Unit)? = null,
    /** وضعیت باز/بسته — فقط برای چرخش نرم فلش سرصفحه. */
    expanded: Boolean = false,
    content: @Composable () -> Unit
) {
    val p = vizitorPalette
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "panelChevron"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.13f),
                        p.surface.copy(alpha = 0.52f),
                        p.surfaceDeep.copy(alpha = 0.38f)
                    )
                )
            )
            .drawWithContent {
                // براقیت بالا (زیر محتوا) — حس شیشهٔ صیقلی بدون کاهش خوانایی متن
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.30f
                    )
                )
                drawContent()
            }
            .border(
                1.3.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.75f),
                        p.gold.copy(alpha = 0.35f),
                        p.goldDark.copy(alpha = 0.28f)
                    )
                ),
                shape
            )
            .padding(inner)
    ) {
        // ── سرصفحهٔ پنل (لمس‌پذیر برای کارت‌های بازشو) ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onHeaderClick != null) Modifier.clickable { onHeaderClick() }
                    else Modifier
                )
        ) {
            if (step != null) {
                StepBadge(step)
                Spacer(Modifier.width(9.dp))
            }
            if (icon != null) {
                IconOrb3D(icon = icon, size = 38.dp, glowColor = accent)
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    ),
                    color = p.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hint != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        hint,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
            if (onHeaderClick != null) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = p.gold,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = chevron }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        content()
    }
}

/** نشان شمارهٔ طلایی سه‌بعدی (گام ۱، ۲، ۳ …) با هالهٔ نور. */
@Composable
fun StepBadge(number: Int, size: Dp = 28.dp) {
    val p = vizitorPalette
    val sizePx = with(LocalDensity.current) { size.toPx() }
    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(p.gold.copy(alpha = 0.30f), Color.Transparent),
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
                        lerp(p.goldHighlight, Color.White, 0.25f),
                        p.gold,
                        p.goldDark
                    )
                )
            )
            .border(1.dp, p.goldHighlight.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            // عدد فارسی با قلم درشت — خوانا حتی روی نشان کوچک
            number.toFaNumber(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.48f).sp,
                lineHeight = (size.value * 0.62f).sp
            ),
            color = Color(0xFF20160A)
        )
    }
}

// ═══════════════════════════ کلید سه‌بعدی ═══════════════════════════

/** رنگ‌های رویه/لبه/رینگ هر تن دکمه. */
private data class BtnSkin(
    val top: Color,
    val bottom: Color,
    val edge: Color,
    val edgeDark: Color,
    val ringA: Color,
    val ringB: Color,
    val text: Color,
    val glow: Color
)

/** تُن دکمه‌های پرمیوم: اصلی (بنفش تم)، طلایی، موفق (سبز) و شیشه‌ای. */
enum class BtnTone { PRIMARY, GOLD, SUCCESS, GLASS }

@Composable
private fun skinOf(tone: BtnTone): BtnSkin {
    val p = vizitorPalette
    return when (tone) {
        BtnTone.PRIMARY -> BtnSkin(
            top = p.btnPrimaryTop, bottom = p.btnPrimaryBottom,
            edge = p.primaryDark, edgeDark = p.surfaceDeep,
            ringA = p.goldHighlight, ringB = p.gold,
            text = p.onPrimary, glow = p.primary
        )
        BtnTone.GOLD -> BtnSkin(
            top = p.goldHighlight, bottom = p.goldDark,
            edge = p.goldDark, edgeDark = Color(0xFF2A1D06),
            ringA = Color.White.copy(alpha = 0.9f), ringB = p.goldHighlight,
            text = Color(0xFF241A02), glow = p.gold
        )
        BtnTone.SUCCESS -> BtnSkin(
            top = p.btnAccentTop, bottom = p.btnAccentBottom,
            edge = p.accentDark, edgeDark = Color(0xFF03251A),
            ringA = Color.White.copy(alpha = 0.85f), ringB = p.accent,
            text = p.onAccent, glow = p.accent
        )
        BtnTone.GLASS -> BtnSkin(
            top = p.textPrimary.copy(alpha = 0.16f), bottom = p.surface.copy(alpha = 0.75f),
            edge = p.surfaceDeep, edgeDark = Color.Black,
            ringA = p.gold.copy(alpha = 0.75f), ringB = p.primary.copy(alpha = 0.55f),
            text = p.textPrimary, glow = p.primary
        )
    }
}

/**
 * کلید سه‌بعدی پرمیوم: رویهٔ گرادیانی برّاق + لبهٔ ضخیم پایین (عمق واقعی) که
 * هنگام لمس کلید داخل لبه فرو می‌رود، رینگ طلایی گرادیانی، هالهٔ نور پشت،
 * آیکن در گوی شیشه‌ای و جاروب نور ملایم روی گوشی‌های توانمند.
 */
@Composable
fun PremiumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: BtnTone = BtnTone.PRIMARY,
    subtitle: String? = null,
    height: Dp = 60.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
    textSize: TextUnit = 15.5.sp
) {
    val p = vizitorPalette
    val skin = skinOf(tone)
    val shape = RoundedCornerShape(height / 2)
    val depth = height * 0.11f
    val depthPx = with(LocalDensity.current) { depth.toPx() }
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateFloatAsState(
        targetValue = if (pressed && enabled && !loading) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 640f),
        label = "premiumSink"
    )
    // جاروب نور ملایم روی کلیدهای فعال (فقط دستگاه توانمند)
    val sweep = if (VizitorPerf.listFx && enabled && !loading) {
        val tr = rememberInfiniteTransition(label = "btnSweep")
        val v by tr.animateFloat(
            initialValue = -0.35f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "btnSweepV"
        )
        v
    } else -1f
    val glowAlpha = if (pressed) 0.30f else 0.18f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height + depth)
            .alphaIf(!enabled)
            .drawBehind {
                // هالهٔ نور زیر کلید
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(skin.glow.copy(alpha = glowAlpha), Color.Transparent)
                    ),
                    topLeft = Offset(size.width * 0.10f, depthPx),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.80f, size.height * 0.85f),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                // لبهٔ ضخیم پایین — همان چیزی که کلید را «فیزیکی» می‌کند
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(skin.edge, skin.edgeDark)),
                    topLeft = Offset(0f, depthPx),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height - depthPx),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
    ) {
        // رویهٔ کلید — با لمس به داخل لبه فرو می‌رود
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer { translationY = sink * depthPx }
                .clip(shape)
                .background(Brush.verticalGradient(listOf(skin.top, skin.bottom)))
                .drawWithContent {
                    // براقیت رو (زیر متن، روی گرادیان)
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.55f
                        )
                    )
                    // جاروب نور مورب
                    if (sweep >= 0f) {
                        val x = size.width * sweep
                        drawPath(
                            path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(x - size.width * 0.10f, 0f)
                                lineTo(x + size.width * 0.05f, 0f)
                                lineTo(x + size.width * 0.05f - size.height * 0.5f, size.height)
                                lineTo(x - size.width * 0.10f - size.height * 0.5f, size.height)
                                close()
                            },
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                    }
                    if (pressed && enabled) {
                        drawRect(color = Color.Black.copy(alpha = 0.10f))
                    }
                    drawContent()
                }
                .border(
                    1.5.dp,
                    Brush.linearGradient(listOf(skin.ringA, skin.ringB, skin.ringA)),
                    shape
                )
                .clickable(
                    enabled = enabled && !loading,
                    interactionSource = interaction,
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = skin.text,
                    strokeWidth = 2.4.dp,
                    modifier = Modifier.size(height * 0.34f)
                )
                Spacer(Modifier.width(10.dp))
            } else if (icon != null) {
                Box(
                    Modifier
                        .size(height * 0.62f)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = skin.text,
                        modifier = Modifier.size(height * 0.34f)
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = textSize,
                        lineHeight = (textSize.value * 1.35f).sp
                    ),
                    color = skin.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = (textSize.value * 0.72f).sp,
                            lineHeight = (textSize.value * 1.05f).sp
                        ),
                        color = skin.text.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** کلید شیشه‌ای کوچک با گوی آیکن (کنار کلید اصلی). */
@Composable
fun OrbIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    tint: Color = vizitorPalette.gold,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    val p = vizitorPalette
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sink by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "orbSink"
    )
    val depthPx = with(LocalDensity.current) { (size * 0.09f).toPx() }
    // جابه‌جایی کلید = بلندی لبه (پیکسل) + فرورفتن هنگام لمس (پیکسل)
    val liftPx = depthPx
    Box(
        modifier = modifier
            .size(size + size * 0.09f)
            .alphaIf(!enabled),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { translationY = liftPx + sink * depthPx }
                .clip(RoundedCornerShape(size * 0.34f))
                .background(
                    Brush.verticalGradient(
                        listOf(p.surfaceDeep, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { translationY = sink * depthPx }
                .clip(RoundedCornerShape(size * 0.34f))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            lerp(p.primary, Color.White, 0.22f),
                            p.primary,
                            p.primaryDark
                        )
                    )
                )
                .drawWithContent {
                    drawContent()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                            center = Offset(this.size.width * 0.32f, this.size.height * 0.24f),
                            radius = this.size.minDimension * 0.6f
                        ),
                        radius = this.size.minDimension * 0.6f,
                        center = Offset(this.size.width * 0.32f, this.size.height * 0.24f)
                    )
                }
                .border(
                    1.2.dp,
                    Brush.linearGradient(listOf(p.goldHighlight, p.gold, p.goldDark)),
                    RoundedCornerShape(size * 0.34f)
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f))
        }
    }
}

// ═══════════════════════════ فیلد ورودی پرمیوم ═══════════════════════════

/**
 * فیلد ورودی صفحهٔ ورود: برچسب طلایی بالای فیلد (همیشه دیده می‌شود)، فیلد
 * بلندتر و راحت‌تر برای لمس، گوی آیکن، هالهٔ فوکوس طلایی و راهنما.
 * همهٔ متن‌ها با فونت فارسی وزیرمتن رندر می‌شوند (تایپوگرافی تم).
 */
@Composable
fun PremiumField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    minHeight: Dp = 58.dp,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp)
) {
    val p = vizitorPalette
    var focused by remember { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (focused) p.gold else p.accentText.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                ),
                color = if (focused) p.gold else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (focused) {
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(p.gold)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .onFocusChanged { focused = it.isFocused },
            enabled = enabled,
            singleLine = singleLine,
            shape = shape,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = p.textPrimary,
                lineHeight = 21.sp,
                fontSize = 14.5.sp
            ),
            placeholder = if (hint != null) {
                {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = TextSecondary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else null,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            leadingIcon = if (icon != null) {
                { IconOrb3D(icon = icon, size = 32.dp, cornerRadius = 11.dp, tint = p.gold) }
            } else null,
            trailingIcon = trailing,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = p.gold,
                unfocusedBorderColor = p.glassBorder,
                errorBorderColor = p.danger,
                focusedContainerColor = p.primary.copy(alpha = 0.07f),
                unfocusedContainerColor = Color.Transparent,
                focusedTextColor = p.textPrimary,
                unfocusedTextColor = p.textPrimary,
                cursorColor = p.gold,
                focusedLeadingIconColor = p.gold,
                unfocusedLeadingIconColor = p.gold.copy(alpha = 0.8f)
            )
        )
    }
}

// ═══════════════════════════ اجزای ورود و تنظیمات ═══════════════════════════

/** ردیف تنظیم با کلید روشن/خاموش — شیشه‌ای و هم‌رنگ تم. */
@Composable
fun ToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.textPrimary.copy(alpha = 0.05f))
            .border(1.dp, p.glassBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        if (icon != null) {
            IconOrb3D(icon = icon, size = 32.dp, cornerRadius = 11.dp, tint = p.gold)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
                color = p.textPrimary
            )
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 15.sp),
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = p.accentDark,
                uncheckedThumbColor = p.textSecondary,
                uncheckedTrackColor = p.surfaceDeep
            )
        )
    }
}

/** شمارندهٔ کوچک شیشه‌ای (کالا / مشتری / فاکتور) با گوی آیکن. */
@Composable
fun StatPill(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = vizitorPalette.gold
) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            value.toFaNumber(),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = 14.sp),
            color = p.textPrimary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextSecondary,
            maxLines = 1
        )
    }
}

/**
 * ردیف «کلید = مقدار» برای نمایش خلاصهٔ اطلاعات اتصال (سرور، دیتابیس، کاربر).
 * مقدارهای حساس هرگز اینجا نمایش داده نمی‌شوند (رمز عبور).
 */
@Composable
fun KeyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    valueColor: Color = vizitorPalette.goldHighlight
) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.textPrimary.copy(alpha = 0.035f))
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = p.accentText, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondary
        )
        Spacer(Modifier.weight(1f))
        Text(
            value.ifBlank { "—" },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** عنوان بخش با خط طلایی و آیکن — جداکنندهٔ بصری صفحه‌های ورود. */
@Composable
fun PremiumSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hint: String? = null,
    align: TextAlign = TextAlign.Center
) {
    val p = vizitorPalette
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, p.gold.copy(alpha = 0.55f), p.gold)
                        )
                    )
            )
            Spacer(Modifier.width(8.dp))
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = p.gold, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, fontSize = 13.sp),
                color = p.goldHighlight,
                textAlign = align
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(p.gold, p.gold.copy(alpha = 0.55f), Color.Transparent)
                        )
                    )
            )
        }
        if (hint != null) {
            Spacer(Modifier.height(5.dp))
            Text(
                hint,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, lineHeight = 16.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** محو شدن نرم هنگام غیرفعال بودن (بدون تغییر چیدمان). */
private fun Modifier.alphaIf(cond: Boolean): Modifier =
    this.then(if (cond) Modifier.graphicsLayer { alpha = 0.45f } else Modifier)
