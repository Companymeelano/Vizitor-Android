/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | سیستم چندتم لاکچری (نسخه ۱٫۸٫۰)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  پنج پالت کاملاً هماهنگ — ۳ تم تیره لاکچری + ۲ تم روشن لاکچری:
 *    ۱. شبِ سلطنتی (تیره — بنفش نئونی)      [پیش‌فرض]
 *    ۲. شبِ طلایی (تیره — طلایی کازینویی)
 *    ۳. زمردِ شب (تیره — سبز زمردی)
 *    ۴. مروارید سلطنتی (روشن — کرم/بنفش سلطنتی)
 *    ۵. رزِ لاکچری (روشن — صورتی رزگلد)
 *  همه اجزای UI از طریق LocalVizitorPalette رنگ می‌گیرند تا تمامی بخش‌ها
 *  از نظر رنگی کاملاً هماهنگ باشند. انتخاب تم با ThemeManager ذخیره می‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** پالت معنایی کامل یک تم — هر رنگ نقش مشخصی در سراسر اپ دارد. */
class VizitorPalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    // سطوح
    val background: Color,
    val surface: Color,
    val surfaceDeep: Color,
    val royalSurfaceTop: Color,     // گرادیان جدول‌ها/کارت‌های سلطنتی (شروع)
    val royalSurfaceBottom: Color,  // گرادیان جدول‌ها/کارت‌های سلطنتی (پایان)
    val glassBorder: Color,
    val topStreak: Color,           // خط نور استودیویی بالای کارت‌ها
    // هاله‌های نور کارت‌ها
    val halo1: Color,
    val halo2: Color,
    // دکمه اصلی (CTA)
    val primary: Color,
    val primaryDark: Color,
    val btnPrimaryTop: Color,
    val btnPrimaryBottom: Color,
    val onPrimary: Color,
    // دکمه ثانویه (موفقیت/تایید)
    val accent: Color,
    val accentDark: Color,
    val btnAccentTop: Color,
    val btnAccentBottom: Color,
    val onAccent: Color,
    // طلایی لوکس
    val gold: Color,
    val goldDark: Color,
    val goldHighlight: Color,       // درخشان‌ترین نقطه شیمر طلایی
    // وضعیت‌ها
    val danger: Color,
    // متن‌ها
    val textPrimary: Color,
    val textSecondary: Color,
    val accentText: Color,          // متن برجسته روی سطوح (یاسی تیره/روشن)
    // اجزای نموداری
    val donutTrack: Color
)

// ════════════════════════ پالت‌های آماده ════════════════════════

/** ۱) شبِ سلطنتی — تیره لوکس فعلی: اسلیت + بنفش نئونی + سبز نئونی + طلا. */
val RoyalDarkPalette = VizitorPalette(
    id = "royal_dark", displayName = "شبِ سلطنتی", isDark = true,
    background = Color(0xFF0B0E13), surface = Color(0xFF12161D), surfaceDeep = Color(0xFF07090C),
    royalSurfaceTop = Color(0xFF1E1133), royalSurfaceBottom = Color(0xFF130B20),
    glassBorder = Color(0x33FFFFFF), topStreak = Color(0x14FFFFFF),
    halo1 = Color(0x26B04BF8), halo2 = Color(0x1A2BFF88),
    primary = Color(0xFFB04BF8), primaryDark = Color(0xFF7A2FB8),
    btnPrimaryTop = Color(0xFFB06CFF), btnPrimaryBottom = Color(0xFF4A1799), onPrimary = Color.White,
    accent = Color(0xFF2BFF88), accentDark = Color(0xFF0FBF62),
    btnAccentTop = Color(0xFF8CFFCB), btnAccentBottom = Color(0xFF0CB35C), onAccent = Color(0xFF04150C),
    gold = Color(0xFFFFD166), goldDark = Color(0xFFC99B2E), goldHighlight = Color(0xFFFFF7CF),
    danger = Color(0xFFFF4D6D),
    textPrimary = Color(0xFFF2F4F8), textSecondary = Color(0xFF9AA3B2), accentText = Color(0xFFE3BFFF),
    donutTrack = Color(0xFF1C2330)
)

/** ۲) شبِ طلایی — تیره لوکس: کربن گرم + گرادیان طلایی + زمرد. */
val MidnightGoldPalette = VizitorPalette(
    id = "midnight_gold", displayName = "شبِ طلایی", isDark = true,
    background = Color(0xFF0A0805), surface = Color(0xFF171208), surfaceDeep = Color(0xFF060402),
    royalSurfaceTop = Color(0xFF211807), royalSurfaceBottom = Color(0xFF120C04),
    glassBorder = Color(0x33F5D98A), topStreak = Color(0x17FFFFFF),
    halo1 = Color(0x2EFFC85C), halo2 = Color(0x1A35E08B),
    primary = Color(0xFFFFC85C), primaryDark = Color(0xFFB07F16),
    btnPrimaryTop = Color(0xFFFFE8A8), btnPrimaryBottom = Color(0xFFB07F16), onPrimary = Color(0xFF241A02),
    accent = Color(0xFF35E08B), accentDark = Color(0xFF14A861),
    btnAccentTop = Color(0xFF9BFFD0), btnAccentBottom = Color(0xFF12A061), onAccent = Color(0xFF04150C),
    gold = Color(0xFFFFDF8E), goldDark = Color(0xFFC39A3C), goldHighlight = Color(0xFFFFF9DE),
    danger = Color(0xFFFF5D7A),
    textPrimary = Color(0xFFF9F1DE), textSecondary = Color(0xFFC4B493), accentText = Color(0xFFFFE2A0),
    donutTrack = Color(0xFF241C0D)
)

/** ۳) زمردِ شب — تیره لوکس: سبز تیره مخملی + زمرد نئونی + طلای ملایم. */
val EmeraldNoirPalette = VizitorPalette(
    id = "emerald_noir", displayName = "زمردِ شب", isDark = true,
    background = Color(0xFF050B08), surface = Color(0xFF0C1912), surfaceDeep = Color(0xFF030805),
    royalSurfaceTop = Color(0xFF0F2A1C), royalSurfaceBottom = Color(0xFF081A10),
    glassBorder = Color(0x339BEFC9), topStreak = Color(0x14FFFFFF),
    halo1 = Color(0x2A17CE8B), halo2 = Color(0x1AFFC85C),
    primary = Color(0xFF2BE0A0), primaryDark = Color(0xFF0FA96F),
    btnPrimaryTop = Color(0xFF8CFFD1), btnPrimaryBottom = Color(0xFF0C8E5C), onPrimary = Color(0xFF02170E),
    accent = Color(0xFF17CE8B), accentDark = Color(0xFF0C8A5A),
    btnAccentTop = Color(0xFFAFFFE0), btnAccentBottom = Color(0xFF0C8A5A), onAccent = Color(0xFF02170E),
    gold = Color(0xFFF2C76A), goldDark = Color(0xFFB99545), goldHighlight = Color(0xFFFFF3D0),
    danger = Color(0xFFFF5D7A),
    textPrimary = Color(0xFFEAF7F0), textSecondary = Color(0xFFA2C2B1), accentText = Color(0xFFAFFFE0),
    donutTrack = Color(0xFF142A1E)
)

/** ۴) مروارید سلطنتی — روشن لوکس: کرم عاج + بنفش سلطنتی + طلای عمیق. */
val RoyalPearlPalette = VizitorPalette(
    id = "royal_pearl", displayName = "مروارید سلطنتی", isDark = false,
    background = Color(0xFFF5EFE4), surface = Color(0xFFFFFDF8), surfaceDeep = Color(0xFFEBE2CF),
    royalSurfaceTop = Color(0xFFF3EBFF), royalSurfaceBottom = Color(0xFFFCF8FF),
    glassBorder = Color(0x407A3FD0), topStreak = Color(0x59FFFFFF),
    halo1 = Color(0x1F7A3FD0), halo2 = Color(0x1412A567),
    primary = Color(0xFF7A3FD0), primaryDark = Color(0xFF5621A6),
    btnPrimaryTop = Color(0xFFB48CF2), btnPrimaryBottom = Color(0xFF5C22AE), onPrimary = Color.White,
    accent = Color(0xFF0E9E64), accentDark = Color(0xFF0A7A4B),
    btnAccentTop = Color(0xFF4ED9A0), btnAccentBottom = Color(0xFF0A7A4B), onAccent = Color.White,
    gold = Color(0xFFAF8009), goldDark = Color(0xFF8A6207), goldHighlight = Color(0xFFE9CE8F),
    danger = Color(0xFFD23C5E),
    textPrimary = Color(0xFF241D2E), textSecondary = Color(0xFF6E6377), accentText = Color(0xFF5C22AE),
    donutTrack = Color(0xFFE3D9C4)
)

/** ۵) رزِ لاکچری — روشن لوکس: صورتی پودری + رزگلد + نعنایی. */
val RoseLuxePalette = VizitorPalette(
    id = "rose_luxe", displayName = "رزِ لاکچری", isDark = false,
    background = Color(0xFFFBF0F3), surface = Color(0xFFFFFAFB), surfaceDeep = Color(0xFFF3E1E7),
    royalSurfaceTop = Color(0xFFFCE4EC), royalSurfaceBottom = Color(0xFFFFF6F9),
    glassBorder = Color(0x40B75C84), topStreak = Color(0x59FFFFFF),
    halo1 = Color(0x1FB75C84), halo2 = Color(0x142FA37C),
    primary = Color(0xFFB75C84), primaryDark = Color(0xFF883457),
    btnPrimaryTop = Color(0xFFE39AB8), btnPrimaryBottom = Color(0xFF883457), onPrimary = Color.White,
    accent = Color(0xFF2FA37C), accentDark = Color(0xFF1F7A5C),
    btnAccentTop = Color(0xFF6FD9B5), btnAccentBottom = Color(0xFF1F7A5C), onAccent = Color.White,
    gold = Color(0xFFB07E2A), goldDark = Color(0xFF8A5F1A), goldHighlight = Color(0xFFEAC77D),
    danger = Color(0xFFD8406B),
    textPrimary = Color(0xFF332027), textSecondary = Color(0xFF7C606B), accentText = Color(0xFF883457),
    donutTrack = Color(0xFFEDD9E0)
)

/** ۰) گزارش طلایی — مشکیِ عمیق + طلای فلزی (زبانِ طراحی مرجع M•A Report). */
val MaReportGoldPalette = VizitorPalette(
    id = "ma_report_gold", displayName = "گزارش طلایی", isDark = true,
    background = Color(0xFF0B0906), surface = Color(0xFF17130C), surfaceDeep = Color(0xFF050403),
    royalSurfaceTop = Color(0xFF241C0E), royalSurfaceBottom = Color(0xFF120D06),
    glassBorder = Color(0x33E8B94F), topStreak = Color(0x1AFFFFFF),
    halo1 = Color(0x2AF0B23C), halo2 = Color(0x14FFFFFF),
    primary = Color(0xFFF0B23C), primaryDark = Color(0xFFA87A1C),
    btnPrimaryTop = Color(0xFFFFE49A), btnPrimaryBottom = Color(0xFFC8912A), onPrimary = Color(0xFF201604),
    accent = Color(0xFFD9B45C), accentDark = Color(0xFFA8853A),
    btnAccentTop = Color(0xFFF2DCA2), btnAccentBottom = Color(0xFFB8933F), onAccent = Color(0xFF1A1305),
    gold = Color(0xFFF0B23C), goldDark = Color(0xFFB9862A), goldHighlight = Color(0xFFFFF0C0),
    danger = Color(0xFFE5544B),
    textPrimary = Color(0xFFF4ECDC), textSecondary = Color(0xFFA3906F), accentText = Color(0xFFF3C869),
    donutTrack = Color(0xFF241C10)
)

/** ۰-روشن) شامپاین روشن — کرم/عاج + طلای عمیق (هم‌خانوادهٔ همان زبان طراحی). */
val ChampagneLightPalette = VizitorPalette(
    id = "champagne_light", displayName = "شامپاین روشن", isDark = false,
    background = Color(0xFFFBF6EC), surface = Color(0xFFFFFDF8), surfaceDeep = Color(0xFFF1E8D7),
    royalSurfaceTop = Color(0xFFFFF6E2), royalSurfaceBottom = Color(0xFFFDF9F0),
    glassBorder = Color(0x40B08A2A), topStreak = Color(0x66FFFFFF),
    halo1 = Color(0x1FB08A2A), halo2 = Color(0x12108A5A),
    primary = Color(0xFFA87E1E), primaryDark = Color(0xFF7C5B10),
    btnPrimaryTop = Color(0xFFE8C46E), btnPrimaryBottom = Color(0xFFA87E1E), onPrimary = Color.White,
    accent = Color(0xFF0E8F62), accentDark = Color(0xFF0A6B49),
    btnAccentTop = Color(0xFF4ED9A0), btnAccentBottom = Color(0xFF0A6B49), onAccent = Color.White,
    gold = Color(0xFF9A7524), goldDark = Color(0xFF7A5A16), goldHighlight = Color(0xFFE9CE8F),
    danger = Color(0xFFC94A3F),
    textPrimary = Color(0xFF241C10), textSecondary = Color(0xFF6F6250), accentText = Color(0xFF8A6A1A),
    donutTrack = Color(0xFFE7DCC6)
)

/** همه تم‌ها به ترتیب نمایش در انتخابگر. */
val AllPalettes: List<VizitorPalette> = listOf(
    MaReportGoldPalette, ChampagneLightPalette,
    RoyalDarkPalette, MidnightGoldPalette, EmeraldNoirPalette, RoyalPearlPalette, RoseLuxePalette
)

fun paletteById(id: String): VizitorPalette = AllPalettes.firstOrNull { it.id == id } ?: MaReportGoldPalette

/** CompositionLocal سراسری پالت فعال — تنها منبع رنگی تمام اجزای UI. */
val LocalVizitorPalette = compositionLocalOf { MaReportGoldPalette }

/** دسترسی کوتاه به پالت فعال داخل کامپوزبل‌ها. */
val vizitorPalette: VizitorPalette
    @Composable get() = LocalVizitorPalette.current

// ════════════════════════ مدیر انتخاب و ذخیره تم ════════════════════════

/**
 * نگهدارنده وضعیت تم انتخابی کاربر — ذخیره ماندگار در SharedPreferences.
 * با set() تمام رابط کاربری به‌صورت زنده و بدون ری‌استارت بازرنگ می‌شود.
 */
object ThemeManager {
    private const val PREFS = "vizitor_theme_prefs"
    private const val KEY_THEME = "theme_id"

    private val _themeId = MutableStateFlow(MaReportGoldPalette.id)
    val themeId: StateFlow<String> = _themeId.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _themeId.value = prefs.getString(KEY_THEME, MaReportGoldPalette.id) ?: MaReportGoldPalette.id
    }

    fun setTheme(context: Context, id: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, id).apply()
        _themeId.value = id
    }
}
