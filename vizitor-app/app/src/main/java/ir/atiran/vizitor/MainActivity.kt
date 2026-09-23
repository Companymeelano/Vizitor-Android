/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | اکتیویتی اصلی
 *  Developed by Milano Technical Team, Milad Yaghoobi
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import ir.atiran.vizitor.ui.navigation.VizitorRoot
import ir.atiran.vizitor.ui.screens.manager.MReportRoot
import ir.atiran.vizitor.ui.theme.ThemeManager
import ir.atiran.vizitor.ui.theme.VizitorTheme
import ir.atiran.vizitor.ui.theme.paletteById

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // اسپلش‌اسکرین لاکچری پیش از محتوای اصلی
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // بارگذاری تم ذخیره‌شده کاربر (۵ تم لاکچری تیره/روشن)
        ThemeManager.init(this)
        enableEdgeToEdge()
        setContent {
            val themeId by ThemeManager.themeId.collectAsState()
            val isDark = paletteById(themeId).isDark
            // هماهنگی رنگ آیکن‌های نوار وضعیت با روشن/تیره بودن تم
            LaunchedEffect(isDark) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
            VizitorTheme(themeId) {
                // راست‌چین کامل رابط کاربری فارسی + مهار مقیاس فونت سیستم
                // (فونت‌های خیلی بزرگ دسترس‌پذیری چیدمان را روی گوشی کوچک نمی‌شکند)
                val baseDensity = LocalDensity.current
                val safeFontScale = baseDensity.fontScale.coerceIn(0.90f, 1.20f)
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides Density(baseDensity.density, safeFontScale)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // v2.20.0: نسخهٔ انحصاری «گزارشات مدیر» مستقیم به اتاق فرمان
                        // گزارش‌ها می‌رود؛ نسخهٔ کامل همان ناوبری همیشگی را دارد.
                        if (BuildConfig.MR_EDITION) MReportRoot() else VizitorRoot()
                    }
                }
            }
        }
    }
}
