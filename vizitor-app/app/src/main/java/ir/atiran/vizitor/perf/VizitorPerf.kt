/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | موتور گرافیک سازگار (Adaptive Graphics Engine)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  تشخیص خودکار قدرت گوشی (رم، هسته‌ها، کلاس حافظه، نسخه اندروید) در لحظه
 *  اجرای برنامه و انتخاب سطح جلوه‌ها؛ بدون ایجاد هیچ هنگی روی هیچ گوشی:
 *    HIGH     → همه جلوه‌های لوکس (گوشی‌های پرچمدار)
 *    BALANCED → جلوه‌های ثابت سبک + حرکت‌های سطح صفحه (میان‌رده)
 *    LOW      → کاملاً استاتیک و فوق‌سبک (گوشی‌های اقتصادی / رم کم)
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.perf

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/** سطح جلوه‌های بصری — با برچسب فارسی برای نمایش در تنظیمات. */
enum class GfxLevel(val faLabel: String) {
    LOW("سبک — حداکثر سرعت"),
    BALANCED("متعادل — زیبایی و روان"),
    HIGH("کامل — حداکثر جلوه")
}

/**
 * تشخیص یک‌باره در شروع برنامه و خواندن بدون هزینه توسط همه اجزای UI.
 * هیچ انیمیشن دائمی‌ای روی سطح LOW فعال نمی‌شود و هیچ جلوه لیستی
 * (قاب نور متحرک، جاروب نور دکمه‌ها، چرخش سه‌بعدی) روی BALANCED و پایین‌تر.
 */
object VizitorPerf {

    @Volatile
    var level: GfxLevel = GfxLevel.HIGH
        private set

    /** فراخوانی در Application.onCreate — کاملاً سبک و روی ترد اصلی امن. */
    fun detect(context: Context): GfxLevel {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val totalGb = mem.totalMem / (1024.0 * 1024.0 * 1024.0)
        val cores = Runtime.getRuntime().availableProcessors()
        val heapMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L)
        val api = Build.VERSION.SDK_INT

        val result = when {
            // گوشی اقتصادی: رم بسیار کم، هیپ محدود، هسته کم یا اندروید خیلی قدیمی
            am.isLowRamDevice || totalGb <= 3.5 || heapMb <= 96 || cores <= 4 || api < 26 ->
                GfxLevel.LOW
            // میان‌رده: رم متوسط یا هسته محدود
            totalGb <= 5.5 || heapMb <= 160 || cores <= 6 || api < 29 ->
                GfxLevel.BALANCED
            else -> GfxLevel.HIGH
        }
        level = result
        return result
    }

    /** جلوه‌های متحرک داخلی آیتم‌های لیست (قاب نور کارت‌ها، نفس دکمه‌ها، شاین، تیلت) — فقط HIGH. */
    val listFx: Boolean get() = level == GfxLevel.HIGH

    /** حرکت‌های تزئینی سطح صفحه (موج ابریشمی، مدار FAB، شیمر عنوان، تیک چرخان) — غیرفعال فقط در LOW. */
    val screenFx: Boolean get() = level != GfxLevel.LOW

    /** انیمیشن ورود یک‌باره کارت‌ها — در LOW کاملاً حذف (نمایش آنی). */
    val entranceFx: Boolean get() = level != GfxLevel.LOW

    /** پرتوهای نور خداگونه در بکگراند — فقط HIGH. */
    val beams: Boolean get() = level == GfxLevel.HIGH

    /** تعداد ذرات غبار طلایی بکگراند بر اساس سطح. */
    val stardustCount: Int
        get() = when (level) {
            GfxLevel.LOW -> 0
            GfxLevel.BALANCED -> 14
            GfxLevel.HIGH -> stardustMax
        }

    private const val stardustMax = 26
}
