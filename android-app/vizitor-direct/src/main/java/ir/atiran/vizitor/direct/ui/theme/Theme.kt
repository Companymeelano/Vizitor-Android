// ═══════════════════════════════════════════════════════════════════════════
//  رنگ و تایپوگرافی — دقیقاً همان زبان بصری پنل مدیریت و نصب‌کنندهٔ ویندوز:
//  سرمه‌ای یکدست، بدون گرادیان، با خط طلایی و فونت وزیرمتن.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.direct.R

// ── پالت (هم‌رنگ پنل مدیریت روی سرور) ───────────────────────────────────────
val Navy = Color(0xFF0B2138)        // پس‌زمینهٔ صفحه
val NavyDark = Color(0xFF07182A)    // نوار بالا / پایین
val Band = Color(0xFF102A45)        // کارت‌ها
val Raised = Color(0xFF143254)      // کادر ورودی / کارت برجسته
val Line = Color(0xFF1E4066)        // خط جداکننده
val Gold = Color(0xFFE3B967)        // تأکید
val TextMain = Color(0xFFEAF1F8)
val TextMuted = Color(0xFFA9BED4)
val Ok = Color(0xFF7EE0A8)
val Err = Color(0xFFF08C7A)
val Warn = Color(0xFFF3D79A)

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val vizitorTypography = Typography(
    displaySmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Vazirmatn, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Vazirmatn, fontSize = 13.5.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Vazirmatn, fontSize = 12.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Vazirmatn, fontSize = 12.sp),
)

private val vizitorColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF20160A),
    secondary = Color(0xFF7FB4E6),
    background = Navy,
    onBackground = TextMain,
    surface = Band,
    onSurface = TextMain,
    surfaceVariant = Raised,
    onSurfaceVariant = TextMuted,
    outline = Line,
    error = Err,
)

@Composable
fun VizitorTheme(content: @Composable () -> Unit) {
    // همیشه سرمه‌ای (طرح تأییدشده) — فارغ از تم سیستم
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = vizitorColors,
        typography = vizitorTypography,
        content = content,
    )
}
