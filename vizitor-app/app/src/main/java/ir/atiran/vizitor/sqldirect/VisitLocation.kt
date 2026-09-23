/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | دریافت موقعیت GPS برای ثبت ویزیت
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  بدون وابستگی به Play Services — فقط LocationManager خودِ اندروید:
 *    • lastKnown()      : آخرین موقعیت ذخیره‌شده (سریع، بدون صبر)
 *    • requestSingle()  : یک‌بار موقعیت تازه از GPS (+ تایم‌اوت ۱۲ ثانیه)
 *  اجازهٔ موقعیت در سطح UI (Compose) درخواست می‌شود؛ این کلاس فقط با فرض
 *  داشتن اجازه اجرا می‌شود و هر خطا را نرم می‌گیرد (null برگرداندن).
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

@SuppressLint("MissingPermission")
class VisitLocationHelper(private val context: Context) {

    private val manager: LocationManager? =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** آخرین موقعیت ذخیره‌شده (هر سه منبع؛ اگر نباشد null). */
    fun lastKnown(): Location? {
        val lm = manager ?: return null
        return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * یک درخواست واحد موقعیت از GPS. نتیجه فقط یک‌بار فراخوانی می‌شود
     * (با موفقیت، یا null بعد از تایم‌اوت ۱۲ ثانیه).
     */
    fun requestSingle(onResult: (Location?) -> Unit) {
        val lm = manager
        if (lm == null) {
            onResult(null)
            return
        }
        val done = AtomicBoolean(false)
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (done.compareAndSet(false, true)) {
                    removeUpdates(lm, this)
                    onResult(location)
                }
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        try {
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, context.mainLooper)
        } catch (_: Throwable) {
            // GPS ممکن است خاموش باشد؛ نتایج دیگر را می‌گیریم
        }
        try {
            lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, context.mainLooper)
        } catch (_: Throwable) {
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (done.compareAndSet(false, true)) {
                removeUpdates(lm, listener)
                onResult(null)
            }
        }, 12_000)
    }

    private fun removeUpdates(lm: LocationManager, l: LocationListener) {
        try {
            lm.removeUpdates(l)
        } catch (_: Throwable) {
        }
    }
}
