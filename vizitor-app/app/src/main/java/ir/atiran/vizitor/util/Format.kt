/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ابزارهای قالب‌بندی فارسی
 *  Developed by Milano Technical Team, Milad Yaghoobi
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import java.util.Locale

private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
private val latinSymbols = DecimalFormatSymbols(Locale.US)

/** تبدیل رقم‌های لاتین به فارسی و جداکننده‌ها به معادل فارسی. */
fun String.toFaDigits(): String =
    map { c ->
        when {
            c in '0'..'9' -> faDigits[c - '0']
            c == ',' -> '٬'
            else -> c
        }
    }.joinToString("")

/** قالب قیمت ریالی با جداکننده هزارگان فارسی. */
fun Long.toFaPrice(): String =
    DecimalFormat("#,###", latinSymbols).format(this).toFaDigits() + " ریال"

fun Long.toFaNumber(): String =
    DecimalFormat("#,###", latinSymbols).format(this).toFaDigits()

fun Int.toFaNumber(): String = toLong().toFaNumber()

fun Double.toFaNumber(): String =
    DecimalFormat("#,###.##", latinSymbols).format(this).toFaDigits().replace('.', '٫')

/** تاریخ شمسی ساده از روی تاریخ میلادی (الگوریتم تبدیل جلالی). */
fun Long.toFaDate(): String {
    val cal = java.util.Calendar.getInstance()
    cal.time = Date(this)
    val (jy, jm, jd) = gregorianToJalali(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    val months = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    return "$jd ${months[jm - 1]} $jy".toFaDigits()
}

fun Long.toFaTime(): String {
    val cal = java.util.Calendar.getInstance()
    cal.time = Date(this)
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)
    return "%02d:%02d".format(h, m).toFaDigits()
}

/** تبدیل میلادی به شمسی — داخلی تا VisitDate (ثبت ویزیت) هم از همین الگوریتم استفاده کند. */
internal fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + (days / 31)
        jd = 1 + (days % 31)
    } else {
        jm = 7 + ((days - 186) / 30)
        jd = 1 + ((days - 186) % 30)
    }
    return Triple(jy, jm, jd)
}

/**
 * تبدیل متن ورودی کاربر (ارقام فارسی/عربی/لاتین، ممیز ٫ یا / یا ,)
 * به عدد اعشاری — برای ورود دستی تعداد و قیمت.
 */
fun String.parseAmount(): Double? {
    val norm = map { c ->
        when {
            c in '۰'..'۹' -> ('0' + (c - '۰'))
            c in '٠'..'٩' -> ('0' + (c - '٠'))
            c == '٫' || c == '/' || c == ',' -> '.'
            else -> c
        }
    }.joinToString("").filter { it.isDigit() || it == '.' }
    if (norm.isEmpty() || norm == ".") return null
    return norm.toDoubleOrNull()
}
