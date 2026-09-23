/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تاریخ/ساعت شمسی برای جدول dbo.Visit
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  ستون‌های DateCreated/TimeCreated جدول Visit در خودِ ERP به‌صورت رشتهٔ
 *  شمسی نگه داشته می‌شوند (قالب هم‌سان تاریخ‌های فاکتور: 1405/06/27).
 *  تبدیل میلادی←شمسی با همان الگوریتمی است که بقیهٔ برنامه (toFaDate)
 *  استفاده می‌کند — یک منبع واحد برای همهٔ تاریخ‌های شمسی اپ.
 *  ساعت به قالب ۲۴ ساعتهٔ HH:mm:ss است.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import ir.atiran.vizitor.util.gregorianToJalali
import java.util.Calendar
import java.util.Locale

/**
 * تاریخ و ساعت جاری به دو رشتهٔ قابل درج در dbo.Visit:
 *  • [dateText] : yyyy/MM/dd شمسی (مثلاً 1405/06/27)
 *  • [timeText] : HH:mm:ss (مثلاً 13:05:42)
 */
object VisitDate {

    data class Now(
        val dateText: String,
        val timeText: String,
    )

    fun now(): Now {
        val cal = Calendar.getInstance()
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        val hh = cal.get(Calendar.HOUR_OF_DAY)
        val mi = cal.get(Calendar.MINUTE)
        val ss = cal.get(Calendar.SECOND)
        val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
        return Now(
            dateText = String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd),
            timeText = String.format(Locale.US, "%02d:%02d:%02d", hh, mi, ss),
        )
    }

    /** فقط تاریخ شمسی امروز (برای شمارش «تعداد ویزیت امروز»). */
    fun todayJalali(): String = now().dateText
}
