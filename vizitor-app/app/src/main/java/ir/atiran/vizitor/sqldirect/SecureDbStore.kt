/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | ذخیرهٔ امن اطلاعات اتصال دیتابیس
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  اطلاعات اتصال SQL Server (شامل رمز) هرگز به‌صورت متن در SharedPreferences
 *  یا DataStore نگه نمی‌دارند؛ یک بلوک JSON با AES-GCM و کلید غیرخارج‌شدنی
 *  Android Keystore رمزنگاری می‌شود (همین الگویی که AuthStore برای توکن
 *  نشست استفاده می‌کند).
 *
 *  قوانین امنیتی:
 *   • رمز در حافظه فقط هنگام اتصال مصرف می‌شود
 *   • هیچ‌گاه در log/crash report نمی‌آید (استثنا در LogCat تنظیم نشده)
 *   • خروج از حساب (logout) → clean
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/** اعتبارنامهٔ ورود ویزیتور که (در صورت درخواست خودش) ذخیره شده است. */
data class ErpCredentials(
    val username: String,
    val password: String,
    val remember: Boolean,
)

/** نگهداری امن تنظیمات اتصال SQL Server (رمزنگارش AES-GCM + Keystore). */
object SecureDbStore {

    private const val PREFS = "vizitor_db"
    private const val KEY_BLOB = "db_blob_v1"
    private const val KS = "AndroidKeyStore"
    private const val ALIAS = "VizitorDbKey"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private lateinit var appContext: Context
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (this::appContext.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /**
     * ذخیرهٔ تنظیمات (کل بلوک، از جمله رمز، رمزنگاری می‌شود).
     *
     * دو نشانی سرور هم همراه همان بلوک نگه داشته می‌شوند تا برنامه بداند کاربر
     * از «آی‌پی اختصاصی/اینترنتی» وصل شده یا از «آی‌پی داخلی شبکه».
     */
    fun save(settings: DbSettings) {
        val o = currentJson() ?: JSONObject()
        o.put("host", settings.host)
        o.put("port", settings.port)
        o.put("database", settings.database)
        o.put("username", settings.username)
        o.put("password", settings.password)
        o.put("useEncryption", settings.useEncryption)
        o.put("trustServerCert", settings.trustServerCert)
        o.put("connectTimeoutSec", settings.connectTimeoutSec)
        o.put("queryTimeoutSec", settings.queryTimeoutSec)
        writeJson(o)
    }

    /** نگه‌داشتن هر دو نشانی سرور و این‌که کدام‌یک فعال است. */
    fun saveAddresses(hostExternal: String, hostLocal: String, useExternal: Boolean) {
        val o = currentJson() ?: JSONObject()
        o.put("hostExternal", hostExternal.trim())
        o.put("hostLocal", hostLocal.trim())
        o.put("useExternal", useExternal)
        writeJson(o)
    }

    /** هر دو نشانی سرور + نشانی فعال، بدون ذخیره‌سازی دوباره. */
    fun loadAddresses(): Triple<String, String, Boolean> {
        val o = currentJson() ?: return Triple("", "", false)
        return Triple(
            o.optString("hostExternal", ""),
            o.optString("hostLocal", o.optString("host", "")),
            o.optBoolean("useExternal", false)
        )
    }

    private fun currentJson(): JSONObject? {
        if (!this::appContext.isInitialized) return null
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        return try {
            val raw = Base64.decode(blob, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, GCM_IV_LENGTH)
            val ct = raw.copyOfRange(GCM_IV_LENGTH, raw.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
            JSONObject(String(cipher.doFinal(ct), Charsets.UTF_8))
        } catch (_: Exception) {
            null
        }
    }

    private fun writeJson(o: JSONObject) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val enc = cipher.doFinal(o.toString().toByteArray(Charsets.UTF_8))
        // IV + ciphertext در یک رشته Base64
        val blob = Base64.encodeToString(iv + enc, Base64.NO_WRAP)
        prefs.edit().putString(KEY_BLOB, blob).apply()
    }

    /**
     * ذخیرهٔ اعتبارنامهٔ ورود ویزیتور (dbo.sys_users) — فقط وقتی خودِ کاربر
     * کلید «ورود سریع/به‌خاطر سپردن» را روشن کرده باشد. رمز هم داخل همان بلوک
     * AES-GCM رمزنگاری می‌شود و هرگز به‌صورت متن نمی‌ماند.
     */
    fun saveErp(username: String, password: String, remember: Boolean) {
        val o = currentJson() ?: JSONObject()
        o.put("erpUser", username)
        o.put("erpPassword", if (remember) password else "")
        o.put("erpRemember", remember)
        writeJson(o)
    }

    /** خواندن اعتبارنامهٔ ذخیره‌شدهٔ ویزیتور (یا null). */
    fun loadErp(): ErpCredentials? {
        val o = currentJson() ?: return null
        val user = o.optString("erpUser", "")
        if (user.isBlank()) return null
        val remember = o.optBoolean("erpRemember", false)
        return ErpCredentials(
            username = user,
            password = if (remember) o.optString("erpPassword", "") else "",
            remember = remember,
        )
    }

    /** فراموش‌کردن اعتبارنامهٔ ویزیتور (رمز فوراً از حافظهٔ دستگاه پاک می‌شود). */
    fun clearErp() {
        val o = currentJson() ?: return
        o.put("erpUser", "")
        o.put("erpPassword", "")
        o.put("erpRemember", false)
        writeJson(o)
    }

    /** خواندن تنظیمات؛ اگر چیزی ذخیره نشده یا خطا بود → null. */
    fun load(): DbSettings? {
        val o = currentJson() ?: return null
        return try {
            DbSettings(
                host = o.optString("host", ""),
                port = o.optInt("port", 1433),
                database = o.optString("database", ""),
                username = o.optString("username", ""),
                password = o.optString("password", ""),
                useEncryption = o.optBoolean("useEncryption", true),
                trustServerCert = o.optBoolean("trustServerCert", true),
                connectTimeoutSec = o.optInt("connectTimeoutSec", 10),
                queryTimeoutSec = o.optInt("queryTimeoutSec", 30),
            )
        } catch (_: Exception) {
            null
        }
    }

    /** فقط مشخصات بدون رمز (برای نمایش در UI: «مربوط به کاربر … در …»). */
    fun loadMasked(): Pair<String, String>? {
        return load()?.let { it.username to it.masked() }
    }

    /** پاک‌سازی کامل (در هنگام خروج از حساب). */
    fun clear() {
        if (!this::prefs.isInitialized) return
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KS).apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KS)
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return gen.generateKey()
    }
}
