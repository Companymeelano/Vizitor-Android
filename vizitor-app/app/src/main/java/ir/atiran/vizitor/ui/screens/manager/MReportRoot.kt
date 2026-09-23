/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  گزارشات مدیر — نسخهٔ انحصاری (v2.20.0 · edition = mreport)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این فایل فقط در نسخهٔ انحصاری «گزارشات مدیر» (بستهٔ `ir.atiran.mreport`)
 *  استفاده می‌شود: ریشهٔ برنامه مستقیماً «اتاق فرمان مدیر» را باز می‌کند —
 *  بدون صفحهٔ ورود ویزیتور، بدون ویترین/سبد/ویزیت/چت.
 *
 *  • نخستین اجرا → برگهٔ «تنظیم اتصال» (همان حس صفحهٔ ورود مرجع)
 *  • پس از اتصال → فهرست جدول‌ها، نگاشت بخش‌ها و گزارش‌های واقعی سرور
 *  • دکمهٔ بازگشت نوار بالا → بستن برنامه
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens.manager

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * ریشهٔ نسخهٔ انحصاری «گزارشات مدیر».
 *
 * همان صفحهٔ کامل «گزارش مدیریت» (M•REPORT) با پنج برگه؛ فقط ورودی برنامه
 * متفاوت است. بازگشت = خروج از برنامه (نسخهٔ انحصاری صفحهٔ دیگری ندارد).
 */
@Composable
fun MReportRoot() {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    MaManagerScreen(
        onBack = { activity?.finish() },
        exclusive = true,
    )
}
