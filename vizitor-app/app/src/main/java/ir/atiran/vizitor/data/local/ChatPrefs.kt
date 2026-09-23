/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تنظیمات اتاق گفتگو (پروفایل، مدیر، قفل، مسدودی)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  نگهداری محلی: پروفایل ویزیتور (نام/تماس/نام‌کاربری/رمز)، وضعیت مدیر،
 *  قابلیت‌های مدیریت: قفل موقت گروه، عدم نمایش شماره/آیدی، مسدودسازی اعضا.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ChatPrefs {

    private const val PREFS = "vizitor_chat_prefs"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PHONE = "phone"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_ADMIN = "is_admin"
    private const val KEY_HIDE_CONTACT = "hide_contact"
    private const val KEY_LOCKED = "group_locked"
    private const val KEY_BLOCKED = "blocked_users"
    private const val KEY_ADMIN_USER = "admin_user"
    private const val KEY_ADMIN_PASS = "admin_pass"
    private const val KEY_AVATAR = "avatar_index"

    private val _fullName = MutableStateFlow("")
    val fullName: StateFlow<String> = _fullName.asStateFlow()
    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()
    private val _hideContact = MutableStateFlow(false)
    val hideContact: StateFlow<Boolean> = _hideContact.asStateFlow()
    private val _groupLocked = MutableStateFlow(false)
    val groupLocked: StateFlow<Boolean> = _groupLocked.asStateFlow()
    private val _blocked = MutableStateFlow<Set<String>>(emptySet())
    val blocked: StateFlow<Set<String>> = _blocked.asStateFlow()
    private val _avatarIndex = MutableStateFlow(0)
    val avatarIndex: StateFlow<Int> = _avatarIndex.asStateFlow()

    val isRegistered: Boolean get() = _username.value.isNotBlank()

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // مقداردهی اولیه حساب مدیریت در اولین اجرا
        if (!p.contains(KEY_ADMIN_USER)) {
            p.edit()
                .putString(KEY_ADMIN_USER, "admin")
                .putString(KEY_ADMIN_PASS, "admin")
                .apply()
        }
        _fullName.value = p.getString(KEY_FULL_NAME, "") ?: ""
        _phone.value = p.getString(KEY_PHONE, "") ?: ""
        _username.value = p.getString(KEY_USERNAME, "") ?: ""
        _isAdmin.value = p.getBoolean(KEY_ADMIN, false)
        _hideContact.value = p.getBoolean(KEY_HIDE_CONTACT, false)
        _groupLocked.value = p.getBoolean(KEY_LOCKED, false)
        _blocked.value = (p.getString(KEY_BLOCKED, "") ?: "")
            .split(',').filter { it.isNotBlank() }.toSet()
        _avatarIndex.value = p.getInt(KEY_AVATAR, 0)
    }

    fun saveProfile(context: Context, fullName: String, phone: String, username: String, password: String, avatarIndex: Int = 0) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_AVATAR, avatarIndex).apply()
        _avatarIndex.value = avatarIndex
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_FULL_NAME, fullName)
            .putString(KEY_PHONE, phone)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
        _fullName.value = fullName
        _phone.value = phone
        _username.value = username
    }

    fun verifyPassword(context: Context, password: String): Boolean {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PASSWORD, "") ?: ""
        return saved.isNotEmpty() && saved == password
    }

    /** بررسی اعتبار مدیر گفتگو (نام‌کاربری/رمز مدیر). */
    fun verifyAdmin(context: Context, user: String, pass: String): Boolean {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val u = p.getString(KEY_ADMIN_USER, "admin") ?: "admin"
        val w = p.getString(KEY_ADMIN_PASS, "admin") ?: "admin"
        return user.trim() == u && pass == w
    }

    /** تغییر رمز مدیر پس از ورود. */
    fun changeAdminPassword(context: Context, newPass: String): Boolean {
        if (newPass.length < 4) return false
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ADMIN_PASS, newPass).apply()
        return true
    }

    fun setAdmin(context: Context, v: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ADMIN, v).apply()
        _isAdmin.value = v
    }

    fun setHideContact(context: Context, v: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_HIDE_CONTACT, v).apply()
        _hideContact.value = v
    }

    fun setGroupLocked(context: Context, v: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LOCKED, v).apply()
        _groupLocked.value = v
    }

    /** مسدود/رفع‌مسدود — نتیجه جدید وضعیت (true = مسدود شد). */
    fun toggleBlocked(context: Context, username: String): Boolean {
        val cur = _blocked.value
        val next = if (username in cur) cur - username else cur + username
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BLOCKED, next.joinToString(",")).apply()
        _blocked.value = next
        return username in next
    }
}
