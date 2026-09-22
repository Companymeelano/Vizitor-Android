/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | دکمه‌های جذاب و کارت‌های آماری (v2.16.0)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  · GlamourButton : دکمهٔ بزرگ سه‌بعدی با گرادیان، درخشش متحرک، سایهٔ رنگی،
 *                    آیکن و زیرنویس — برچسب همیشه با اندازهٔ خودتنظیم جا می‌شود.
 *  · KpiCard       : کاشی شاخص کلیدی با آیکن، عدد بزرگ و برچسب (برای پنل مدیریت).
 *  · GlamourCard   : قاب شیشه‌ای طلایی دور هر بخش (جدول/نمودار).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.vizitorPalette

/**
 * دکمهٔ «جذاب» — سه‌بعدی، گرادیانی، با درخششِ عبوری و فشرده‌شدن هنگام لمس.
 * متن برچسب/زیرنویس خودتنظیم است، پس هیچ نوشته‌ای درون دکمه نمی‌بُرد.
 */
@Composable
fun GlamourButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: androidx.compose.ui.unit.Dp = 54.dp,
    tone: GlamourTone = GlamourTone.GOLD,
) {
    val p = vizitorPalette
    val face: List<Color> = when (tone) {
        GlamourTone.GOLD -> listOf(Color(0xFFFFE9AE), Color(0xFFE8B23F), Color(0xFFC98A18))
        GlamourTone.PURPLE -> listOf(Color(0xFFB478FF), Color(0xFF8B3FF0), Color(0xFF5B21A8))
        GlamourTone.GREEN -> listOf(Color(0xFF9CFFD2), Color(0xFF19D67C), Color(0xFF0B8F4E))
        GlamourTone.GLASS -> listOf(p.primary.copy(alpha = 0.35f), p.surfaceDeep.copy(alpha = 0.75f))
    }
    val textColor = when (tone) {
        GlamourTone.GOLD -> Color(0xFF3A2700)
        GlamourTone.GLASS -> p.textPrimary
        else -> Color.White
    }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed && enabled) 0.975f else 1f, tween(90), label = "glamPress")
    val alpha = if (enabled) 1f else 0.5f

    // درخششِ عبوری روی رویهٔ دکمه
    val shimmer = if (enabled) {
        val t = rememberInfiniteTransition(label = "glamShine")
        val v by t.animateFloat(
            initialValue = -0.4f, targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
            label = "shine"
        )
        v
    } else 0.5f

    Box(modifier = modifier.scale(scale).height(height)) {
        // سایهٔ رنگی زیر دکمه (حس عمق)
        Box(
            Modifier
                .matchParentSize()
                .padding(top = 5.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(face.last().copy(alpha = 0.45f * alpha))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height - 5.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(face.map { it.copy(alpha = it.alpha * alpha) }))
                .border(1.dp, Color(0x77FFFFFF).copy(alpha = 0.6f * alpha), RoundedCornerShape(20.dp))
                .drawBehind {
                    // نوار درخشش مایل
                    val w = size.width
                    val x = shimmer * (w + 220f) - 110f
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color(0x59FFFFFF), Color.Transparent),
                            start = Offset(x, 0f),
                            end = Offset(x + 110f, size.height)
                        )
                    )
                }
                .clickable(enabled = enabled && !loading) {
                    pressed = true
                    onClick()
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(9.dp))
            }
            Column(
                modifier = Modifier.weight(1f, fill = true),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AutoFitText(
                    text = if (loading) "لطفاً صبر کنید…" else label,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    minimumSize = 10.sp,
                    maximumSize = 15.5.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                if (subtitle != null && !loading) {
                    Spacer(Modifier.height(1.dp))
                    AutoFitText(
                        text = subtitle,
                        color = textColor.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium,
                        minimumSize = 7.5.sp,
                        maximumSize = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (loading) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
    if (pressed) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(110)
            pressed = false
        }
    }
}

enum class GlamourTone { GOLD, PURPLE, GREEN, GLASS }

/**
 * کاشی شاخص کلیدی (KPI) — به سبک کارت‌های شاخص مرجع:
 * آیکن طلایی گوشه‌گرد، عنوان، خط کوتاه طلایی و مقدار درشت رنگی.
 */
@Composable
fun KpiCard(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    Column(
        modifier = modifier
            .metalPanel(RoundedCornerShape(18.dp), corner = 18f)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MaIconBadge(icon, size = 28.dp, tint = tint)
            Spacer(Modifier.width(6.dp))
            AutoFitText(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                minimumSize = 8.sp,
                maximumSize = 11.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f, fill = true)
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(30.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(tint, tint.copy(alpha = 0.25f))))
        )
        Spacer(Modifier.height(5.dp))
        AutoFitText(
            text = value,
            color = tint,
            fontWeight = FontWeight.Black,
            minimumSize = 11.sp,
            maximumSize = 17.sp,
            modifier = Modifier.fillMaxWidth()
        )
        if (footnote != null) {
            Spacer(Modifier.height(1.dp))
            AutoFitText(
                text = footnote,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontWeight = FontWeight.Medium,
                minimumSize = 7.sp,
                maximumSize = 9.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** قاب فلزی-طلایی — دور جدول‌ها و نمودارها (به سبک کارت‌های مرجع). */
@Composable
fun GlamourCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(22.dp), corner = 22f)
            .padding(12.dp)
    ) {
        if (title != null) {
            MaSectionHeader(title = title, icon = icon)
            trailing?.let {
                Spacer(Modifier.height(6.dp))
                it()
            }
            Spacer(Modifier.height(9.dp))
        }
        content()
    }
}
