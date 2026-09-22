/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نیت نقش ورود (v2.16.0)
 *  ─────────────────────────────────────────────────────────────────────────
 *  صفحهٔ اول تعیین می‌کند کاربر با کدام نقش وارد می‌شود:
 *    · VISITOR  → پنل ویزیتور (پیشخوان/ویترین/مشتری/گزارشات)
 *    · MANAGER  → پنل مدیریت (نمودارها و جدول‌های گزارش کامل)
 *  ورود هر دو نقش با «نام کاربری و کلمهٔ عبور خودِ کاربر در سامانهٔ آتیران»
 *  انجام می‌شود (جدول واقعی dbo.sys_users) — دقیقاً همان چیزی که کارفرما خواسته.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object VizitorRoleIntent {

    enum class Role(val title: String) {
        VISITOR("مامور فروش / ویزیتور"),
        MANAGER("مدیریت"),
        SALES_MANAGER("مدیر فروش"),
        ACCOUNTANT("حسابداری"),
        WAREHOUSE("انبار و پخش"),
        SHOPKEEPER("کاربر فروشگاه"),
    }

    private val _role = MutableStateFlow(Role.VISITOR)
    val role: StateFlow<Role> = _role.asStateFlow()

    val current: Role get() = _role.value

    fun set(role: Role) { _role.value = role }

    /** آیا پنل مدیریت باید باز شود؟ (مدیریت و مدیر فروش هر دو گزارش کامل می‌بینند) */
    fun wantsManagerPanel(): Boolean =
        current == Role.MANAGER || current == Role.SALES_MANAGER
}
