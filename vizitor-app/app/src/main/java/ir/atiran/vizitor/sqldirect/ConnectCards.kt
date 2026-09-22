// ═══════════════════════════════════════════════════════════════════════════
//  کارت اتصالِ نصب‌کنندهٔ ویندوز
//
//  نصب‌کننده سه چیز می‌سازد (روی سرور، پوشهٔ C:\Vizitor\setup):
//     android-connect.png   کد QR
//     android-connect.txt   کارت متنی فارسی
//     android-connect.json  همان اطلاعات به شکل JSON (بدون رمز)
//
//  متن QR:   vizitor://c?h=<آی‌پی داخلی>&p=1433&d=<دیتابیس>&u=<کاربر محدود>&H=<آی‌پی ثابت>
//            کلیدها: h=آی‌پی داخلی، p=پورت، d=دیتابیس، u=کاربر محدود، H=آی‌پی اختصاصی
//
//  این فایل هر دو شکل (URI و JSON) را می‌خواند. **رمز در کارت نیست** — کاربر
//  کامل (مدیر) رمز را یک‌بار وارد می‌کند تا لیست دیتابیس‌ها بیاید و دیتابیس
//  حسابداری انتخاب شود.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.sqldirect

import android.net.Uri
import org.json.JSONObject

data class ConnectCard(
    val hostLan: String = "",
    val hostPublic: String = "",
    val port: Int = 1433,
    val database: String = "",
    val login: String = "",
    val panelUrl: String = "",
    val source: String = "",
) {
    /** نشانی داخلی اگر بود، وگرنه اختصاصی. */
    val preferredHost: String get() = hostLan.ifBlank { hostPublic }

    val isEmpty: Boolean get() = hostLan.isBlank() && hostPublic.isBlank() && database.isBlank()
}

object ConnectCards {

    /** نشانی کارت در متن آزاد (کارت متنی نصب‌کننده، پیام، یا یادداشت). */
    private val URI_PATTERN = Regex("vizitor://\\S+")

    /** تلاش برای خواندن هر متن ورودی: URI کارت، JSON کارت، یا متن کارت. */
    fun parse(text: String): ConnectCard? {
        val raw = text.trim()
        if (raw.isEmpty()) return null
        return when {
            raw.startsWith("{") -> fromJson(raw)
            raw.startsWith("vizitor://", ignoreCase = true) -> fromUri(raw)
            else -> {
                // متن کارت: نشانی را هرجای متن پیدا کن (خط کارت با «متن QR:» شروع می‌شود)
                val found = URI_PATTERN.find(raw)?.value
                found?.let { fromUri(it.trimEnd('.', '،', '»', ')')) }
            }
        }
    }

    /** vizitor://c?h=…&p=…&d=…&u=…&H=…   و   vizitor://connect?sql=host,port&db=…&user=…&public=… */
    fun fromUri(uriText: String): ConnectCard? {
        val uri = runCatching { Uri.parse(uriText) }.getOrNull() ?: return null
        if (!"vizitor".equals(uri.scheme, ignoreCase = true)) return null

        fun q(vararg keys: String): String {
            for (k in keys) {
                val v = runCatching { uri.getQueryParameter(k) }.getOrNull()
                if (!v.isNullOrBlank()) return v.trim()
            }
            return ""
        }

        var hostLan = q("h", "host", "host_lan")
        var hostPublic = q("H", "host_public", "public")
        var port = q("p", "port")
        var database = q("d", "db", "database")
        var login = q("u", "user", "login")
        val panel = q("a", "api", "panel")

        // شکل بلند: sql=host,port
        val sql = q("sql")
        if (sql.isNotBlank()) {
            val parts = sql.split(',', ':').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) hostLan = parts[0]
            if (parts.size > 1 && port.isBlank()) port = parts[1]
        }

        val card = ConnectCard(
            hostLan = hostLan,
            hostPublic = hostPublic,
            port = port.toIntOrNull()?.takeIf { it in 1..65535 } ?: 1433,
            database = database,
            login = login,
            panelUrl = panel,
            source = "QR / URI",
        )
        return card.takeUnless { it.isEmpty }
    }

    /** android-connect.json (کارت امن نصب‌کننده — بدون رمز). */
    fun fromJson(jsonText: String): ConnectCard? = try {
        val o = JSONObject(jsonText)
        val card = ConnectCard(
            hostLan = o.optString("host_lan").ifBlank { o.optString("host") },
            hostPublic = o.optString("host_public"),
            port = o.optInt("port", 1433).let { if (it in 1..65535) it else 1433 },
            database = o.optString("database"),
            login = o.optString("login"),
            panelUrl = o.optString("panel_url"),
            source = "android-connect.json",
        )
        card.takeUnless { it.isEmpty }
    } catch (_: Exception) {
        null
    }
}
