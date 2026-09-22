/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | فونت اختصاصی فارسی (Vazirmatn) + تایپوگرافی کالیبره
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  ▸ شش وزن واقعی فونت وزیرمتن (Thin…Black) به‌صورت TTF در res/font قرار دارد
 *    تا روی همهٔ گوشی‌ها (اندروید ۷ به بالا) بدون اتکا به فونت سیستمی،
 *    یکسان و زیبا رندر شود.
 *  ▸ هیچ letterSpacing روی متن فارسی گذاشته نمی‌شود (اتصال حروف فارسی
 *    باید دست‌نخورده بماند).
 *  ▸ مقیاس تایپوگرافی برای فارسی: ارتفاع خط باصرفه‌تر (۱٫۵ برابر) تا
 *    اعراب/کشیدگی‌ها بریده نشوند و متن در همه جای برنامه خوانا و شیک باشد.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.R

/** فونت اختصاصی برنامه — وزیرمتن با شش وزن واقعی. */
val VazirFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black)
)

/** فونت درشت‌نمای برند (تیترهای اسپلش و سرصفحهٔ پنل). */
val VazirBrand = FontFamily(Font(R.font.vazirmatn_black, FontWeight.Black))

/**
 * تایپوگرافی کالیبره‌شدهٔ فارسی — تمام ۱۵ اسلات متریال تعریف شده‌اند تا
 * هیچ متنی در هیچ صفحه‌ای به فونت پیش‌فرض سیستم برنگردد.
 * (تعداد خط فارسی بیشتر از لاتین است؛ ارتفاع‌ها ۱٫۵۲ تا ۱٫۶ برابر انتخاب شده)
 */
val VizitorTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Black,
        fontSize = 40.sp, lineHeight = 54.sp, letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp, lineHeight = 48.sp, letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 38.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Bold,
        fontSize = 23.sp, lineHeight = 34.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 30.sp, letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 25.sp, letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp, lineHeight = 21.sp, letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp, lineHeight = 19.sp, letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 22.sp, letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp, lineHeight = 19.sp, letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = VazirFamily, fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp, lineHeight = 17.sp, letterSpacing = 0.sp
    )
)

/** اندازهٔ فونت فیلدهای ورودی (متن فارسی خوانا و دوستانه). */
val InputTextSize = 14.5.sp

/** ضریب ارتفاع خط استاندارد فارسی — برای سبک‌های دست‌ساز. */
val FaLineHeight = 1.55.em
