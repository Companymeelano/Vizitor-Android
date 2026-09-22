/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ویجت‌های مشترک رابط کاربری (نسخه لاکچری)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  دکمه‌های لاکچری با حلقه طلایی، براق استودیویی، جاروب نور و مقیاس لمسی.
 *  همه جلوه‌ها لایه‌ای و سبک هستند (بدون blur/shadow) — روان روی هر گوشی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.GoldDark
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

/**
 * ✨ دکمه اکشن اصلی سه‌بعدی (نسخه ۱٫۹٫۰ — مجسمه‌ای):
 * لبه عمق فیزیکی زیر سطح + هاله نور پرتابی + گرادیان تم + جاروب نور +
 * آیکن داخل گوی شیشه‌ای. رنگ‌ها از پالت فعال.
 */
@Composable
fun NeonPurpleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(26.dp)
    val p = vizitorPalette
    val face = 58.dp
    // نور تنفسی — تپش ملایم هاله زیر دکمه ✨ (سطح «کامل»؛ در غیر این‌صورت ثابت و بدون هنگ)
    val breathe = if (VizitorPerf.listFx) {
        val b by rememberInfiniteTransition(label = "glowPulse").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowPulseV"
        )
        b
    } else 0.55f
    // ✨ حس مغناطیسی: هاله طلایی که هنگام فشار دکمه شعله می‌کشد + هستیک ظریف
    val pressSrc = remember { MutableInteractionSource() }
    val isPressed by pressSrc.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val glowColor = p.primary.copy(alpha = (if (isPressed) 0.30f else 0.24f) + breathe * 0.16f)
    val pressGold = p.gold.copy(alpha = if (isPressed) 0.35f else 0f)
    Box(
        modifier = modifier
            .height(face + 6.dp)
            .press3D(depth = 4.dp)
            .drawBehind {
                // باریکه طلایی هنگام فشار — ریپل لاکچری
                if (pressGold.alpha > 0f) {
                    drawRoundRect(
                        color = pressGold,
                        cornerRadius = CornerRadius(30.dp.toPx())
                    )
                }
                // هاله نور تنفسی زیر دکمه (حس شناور و زنده بودن)
                if (enabled) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.minDimension * (2.0f + breathe * 0.5f)
                        ),
                        cornerRadius = CornerRadius(30.dp.toPx())
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = pressSrc,
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // لبه عمق فیزیکی (۳بعد واقعی)
        Box(
            Modifier
                .fillMaxWidth()
                .height(face)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(
                    if (enabled) p.btnPrimaryBottom.copy(alpha = 0.9f)
                    else p.textPrimary.copy(alpha = 0.05f)
                )
        )
        // سطح اصلی دکمه
        LuxurySurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(face),
            shape = shape,
            fillBrush = Brush.verticalGradient(
                colors = if (enabled)
                    listOf(p.btnPrimaryTop, p.primary, p.btnPrimaryBottom)
                else
                    listOf(p.textPrimary.copy(alpha = 0.12f), p.textPrimary.copy(alpha = 0.06f))
            ),
            enabled = enabled
        ) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    // گوی شیشه‌ای آیکن
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (enabled) 0.18f else 0.06f))
                            .border(1.dp, Color.White.copy(alpha = if (enabled) 0.35f else 0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, contentDescription = null,
                            tint = if (enabled) p.onPrimary else p.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    text,
                    color = if (enabled) p.onPrimary else p.textSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )
                )
            }
        }
    }
}

/**
 * ✨ دکمه ثانویه سه‌بعدی — لبه عمق + هاله نور + آیکن در گوی شیشه‌ای.
 */
@Composable
fun NeonGreenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(22.dp)
    val p = vizitorPalette
    val face = 50.dp
    val breathe2 = if (VizitorPerf.listFx) {
        val b by rememberInfiniteTransition(label = "glowPulse2").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowPulse2V"
        )
        b
    } else 0.55f
    // ✨ حس مغناطیسی: هاله طلایی که هنگام فشار دکمه شعله می‌کشد + هستیک ظریف
    val pressSrc2 = remember { MutableInteractionSource() }
    val isPressed2 by pressSrc2.collectIsPressedAsState()
    val haptic2 = LocalHapticFeedback.current
    val glowColor = p.accent.copy(alpha = (if (isPressed2) 0.28f else 0.22f) + breathe2 * 0.14f)
    val pressGold2 = p.gold.copy(alpha = if (isPressed2) 0.32f else 0f)
    Box(
        modifier = modifier
            .height(face + 6.dp)
            .press3D(depth = 4.dp)
            .drawBehind {
                // باریکه طلایی هنگام فشار — ریپل لاکچری
                if (pressGold2.alpha > 0f) {
                    drawRoundRect(
                        color = pressGold2,
                        cornerRadius = CornerRadius(26.dp.toPx())
                    )
                }
                if (enabled) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = Offset(size.width / 2f, size.height),
                            radius = size.minDimension * 2.2f
                        ),
                        cornerRadius = CornerRadius(26.dp.toPx())
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = pressSrc2,
                indication = null
            ) {
                haptic2.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // لبه عمق فیزیکی
        Box(
            Modifier
                .fillMaxWidth()
                .height(face)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(
                    if (enabled) p.accentDark.copy(alpha = 0.9f)
                    else p.textPrimary.copy(alpha = 0.05f)
                )
        )
        // سطح اصلی دکمه
        LuxurySurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(face),
            shape = shape,
            fillBrush = Brush.verticalGradient(
                colors = if (enabled)
                    listOf(p.btnAccentTop, p.accent, p.btnAccentBottom)
                else
                    listOf(p.textPrimary.copy(alpha = 0.12f), p.textPrimary.copy(alpha = 0.06f))
            ),
            enabled = enabled
        ) {
            Row(
                Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (p.isDark) 0.18f else 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, contentDescription = null,
                            tint = if (enabled) p.onAccent else p.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text,
                    color = if (enabled) p.onAccent else p.textSecondary,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
    }
}

/** عنوان هر بخش از صفحه با نشان طلایی. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** برچسب وضعیت کوچک رنگی با حلقه ظریف هم‌رنگ. */
@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
    }
}

/**
 * ═ فوتر کپی‌رایت — الزام هویتی پروژه ═
 * متن با گرادیان طلایی و جداکننده درخشان.
 */
@Composable
fun MilanoFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color(0xFFFFF6CC), Gold, Color.Transparent))
                )
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Developed by Milano Technical Team, Milad Yaghoobi",
            color = Gold,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
    }
}
