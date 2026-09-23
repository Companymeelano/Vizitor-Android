// ═══════════════════════════════════════════════════════════════════════════
//  پیکربندی سرور — همان چیزی که در تنظیمات برنامه نگه داشته می‌شود.
//  (کلاس اصلی در خودِ برنامهٔ ویزیتور وجود دارد؛ این نسخه فقط برای اینکه
//   لایهٔ دادهٔ تأییدشدهٔ MeelanoDataSource بدون تغییر کامپایل شود نوشته شده و
//   همان فیلدهای مصرف‌شده را دارد: serverIp و dbPort.)
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.data.repository

import ir.atiran.vizitor.data.sql.DbSettings

/**
 * تنظیمات اتصال برنامه.
 *
 * نشانی‌ها: [hostLan] آی‌پی داخلی شبکهٔ فروشگاه و [hostPublic] آی‌پی اختصاصی/اینترنتی.
 * کاربر با [usePublic] انتخاب می‌کند از کدام‌یک وصل شود و [serverIp] همان نشانی فعال است.
 */
data class ServerConfig(
    val serverIp: String = "",
    val dbPort: Int = 1433,
    val dbName: String = "",
    val dbUser: String = "",
    val dbPassword: String = "",
    val useEncryption: Boolean = false,   // SQL Server 2014 معمولاً گواهی TLS معتبر ندارد
    val trustServerCert: Boolean = true,
    /** آی‌پی داخلی و آی‌پی اختصاصی، برای جابه‌جایی سریع بین «داخل/بیرون شبکه». */
    val hostLan: String = "",
    val hostPublic: String = "",
    val usePublic: Boolean = false,
    /** کد فعال‌سازی سامانه (همان کدی که نصب‌کننده در سرور ثبت کرده است) — اختیاری. */
    val activationCode: String = "",
    /** آدرس پنل مدیریت روی سرور (فقط برای نمایش؛ اتصال دادهٔ برنامه مستقیم است). */
    val panelUrl: String = "",
) {
    /** تبدیل به تنظیمات لایهٔ داده (رمز فقط در حافظه می‌ماند). */
    fun toDbSettings(): DbSettings = DbSettings(
        host = serverIp,
        port = dbPort,
        database = dbName,
        username = dbUser,
        password = dbPassword,
        useEncryption = useEncryption,
        trustServerCert = trustServerCert,
    )

    /** نشانی نمایشی بدون رمز. */
    fun masked(): String = "$dbUser@$serverIp:$dbPort/$dbName"

    companion object {
        /** از تنظیمات ذخیره‌شدهٔ امن ⇒ پیکربندی برنامه. */
        fun from(settings: DbSettings, addresses: Triple<String, String, Boolean>): ServerConfig =
            ServerConfig(
                serverIp = settings.host,
                dbPort = settings.port,
                dbName = settings.database,
                dbUser = settings.username,
                dbPassword = settings.password,
                useEncryption = settings.useEncryption,
                trustServerCert = settings.trustServerCert,
                hostLan = addresses.first,
                hostPublic = addresses.second,
                usePublic = addresses.third,
            )
    }
}
