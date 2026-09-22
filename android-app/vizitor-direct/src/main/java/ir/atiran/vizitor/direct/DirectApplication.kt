package ir.atiran.vizitor.direct

import android.app.Application
import ir.atiran.vizitor.data.local.SecureDbStore

/**
 * نگه‌داری امن تنظیمات اتصال باید پیش از هر استفاده‌ای آماده شود
 * (کلید AES در Android Keystore ساخته/خوانده می‌شود).
 */
class DirectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SecureDbStore.init(this)
    }
}
