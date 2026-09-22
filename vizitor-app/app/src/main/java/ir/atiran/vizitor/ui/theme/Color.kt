/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | پالت رنگی پویا (تم‌پذیر)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تمام نام‌های رنگی قبلی حفظ شده‌اند، اما حالا به‌جای مقدار ثابت، از
 *  LocalVizitorPalette خوانده می‌شوند؛ بنابراین با تعویض تم (تیره/روشن
 *  لاکچری) رنگ کل برنامه خودکار و هماهنگ تغییر می‌کند — بدون تغییر در
 *  کد صفحه‌ها. تعریف پالت‌ها: Themes.kt
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── سطوح پایه ───────────────────────────────────────────────────────────────
val DarkSlate: Color @Composable get() = vizitorPalette.background
val DarkSlateElevated: Color @Composable get() = vizitorPalette.surface
val DarkSlateDeep: Color @Composable get() = vizitorPalette.surfaceDeep

// ── بنفش اصلی (CTA) ─────────────────────────────────────────────────────────
val NeonPurple: Color @Composable get() = vizitorPalette.primary
val NeonPurpleDark: Color @Composable get() = vizitorPalette.primaryDark
val NeonPurpleGlow: Color @Composable get() = vizitorPalette.primary.copy(alpha = 0.40f)

// ── سبز موفقیت/تارگت ────────────────────────────────────────────────────────
val NeonGreen: Color @Composable get() = vizitorPalette.accent
val NeonGreenDark: Color @Composable get() = vizitorPalette.accentDark
val NeonGreenGlow: Color @Composable get() = vizitorPalette.accent.copy(alpha = 0.35f)

// ── طلایی VIP ───────────────────────────────────────────────────────────────
val Gold: Color @Composable get() = vizitorPalette.gold
val GoldDark: Color @Composable get() = vizitorPalette.goldDark
val GoldGlow: Color @Composable get() = vizitorPalette.gold.copy(alpha = 0.25f)

// ── شیشه ────────────────────────────────────────────────────────────────────
val GlassFill: Color @Composable get() = vizitorPalette.textPrimary.copy(alpha = 0.06f)
val GlassFillStrong: Color @Composable get() = vizitorPalette.surface.copy(alpha = 0.92f)
val GlassBorder: Color @Composable get() = vizitorPalette.glassBorder
val GlassHighlight: Color @Composable get() = vizitorPalette.topStreak

// ── متن‌ها و وضعیت‌ها ───────────────────────────────────────────────────────
val DangerRed: Color @Composable get() = vizitorPalette.danger
val TextPrimary: Color @Composable get() = vizitorPalette.textPrimary
val TextSecondary: Color @Composable get() = vizitorPalette.textSecondary

/** متن برجسته روی سطوح سلطنتی (یاسی در تم تیره، بنفش عمیق در تم روشن). */
val AccentText: Color @Composable get() = vizitorPalette.accentText

/** ریل نمودار دونات و نوارهای خاکستری پیشرفت. */
val DonutTrack: Color @Composable get() = vizitorPalette.donutTrack
