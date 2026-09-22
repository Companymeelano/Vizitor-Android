/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | کلاس Application
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  مقداردهی اولیه دیتابیس محلی + زمان‌بندی سینک پس‌زمینه + کاشت داده دمو
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor

import android.app.Application
import ir.atiran.vizitor.data.repository.VizitorRepository
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.data.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VizitorApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // موتور گرافیک سازگار: تشخیص قدرت دستگاه و تنظیم خودکار سطح جلوه‌ها
        // تا اجرای برنامه روی هیچ گوشی‌ای (اقتصادی تا پرچمدار) هنگ نداشته باشد
        VizitorPerf.detect(this)
        // مقداردهی پروفایل و تنظیمات اتاق گفتگوی ویزیتورها
        ir.atiran.vizitor.data.local.ChatPrefs.init(this)
        // مقداردهی فهرست چک‌های پیگیری‌شونده ویزیتور
        ir.atiran.vizitor.data.local.ChequeStore.init(this)
        val repository = VizitorRepository(this)
        // کاشت داده نمونه برای اولین اجرا (حالت دمو تا اتصال سرور واقعی)
        appScope.launch { repository.ensureSeeded() }
        // فعال‌سازی سرویس همگام‌سازی خودکار پس‌زمینه
        SyncWorker.schedule(this)
    }
}
