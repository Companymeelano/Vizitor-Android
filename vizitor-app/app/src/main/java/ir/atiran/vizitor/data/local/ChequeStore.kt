/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | دوش و چک‌های پیگیری‌شده (Cheque Follow-Up)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  لیست چک‌های مشتریان که هنوز در سامانه ثبت نشده‌اند؛ با نام مشتری،
 *  سریال، بانک، مبلغ، سررسید نزدیک و امکان تماس مستقیم از پیشخوان.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** یک چک در انتظار ثبت — با مشخصات کامل جهت پیگیری و تماس. */
data class ChequeFollowUp(
    val customerName: String,
    val phone: String,
    val serial: String,
    val bank: String,
    val amount: Long,
    val dueDays: Int      // روزهای مانده تا سررسید
)

object ChequeStore {

    private const val PREFS = "vizitor_cheques"
    private const val KEY_ITEMS = "items"
    private const val SEP_ITEM = ";;"
    private const val SEP_FIELD = "|"

    private val _items = MutableStateFlow<List<ChequeFollowUp>>(emptyList())
    val items: StateFlow<List<ChequeFollowUp>> = _items.asStateFlow()

    fun init(context: Context) {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ITEMS, "") ?: ""
        _items.value = raw.split(SEP_ITEM)
            .filter { it.count { c -> c == '|' } == 5 }
            .mapNotNull { row ->
                val f = row.split(SEP_FIELD)
                if (f.size != 6) return@mapNotNull null
                ChequeFollowUp(
                    customerName = f[0],
                    phone = f[1],
                    serial = f[2],
                    bank = f[3],
                    amount = f[4].toLongOrNull() ?: 0L,
                    dueDays = f[5].toIntOrNull() ?: 0
                )
            }
    }

    private fun persist(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(
                KEY_ITEMS,
                _items.value.joinToString(SEP_ITEM) {
                    listOf(it.customerName, it.phone, it.serial, it.bank, it.amount, it.dueDays)
                        .joinToString(SEP_FIELD)
                }
            )
            .apply()
    }

    /** کاشت نمونه اولیه از مشتریان واقعی — فقط یک‌بار، وقتی هنوز چکی ثبت نشده. */
    fun seedIfEmpty(context: Context, customers: List<CustomerEntity>) {
        if (_items.value.isNotEmpty()) return
        val phones = customers.filter { it.phone.isNotBlank() }.take(3)
        if (phones.isEmpty()) return
        val banks = listOf("ملی", "صادرات", "تجارت")
        val dueBy = listOf(6, 13, 24)
        _items.value = phones.mapIndexed { i, c ->
            ChequeFollowUp(
                customerName = c.name,
                phone = c.phone,
                serial = "7562${c.code}${(i + 1)}",
                bank = banks[i % banks.size],
                amount = if (c.debt > 0) c.debt else (48_000_000L + i * 17_000_000L),
                dueDays = dueBy[i % dueBy.size]
            )
        }
        persist(context)
    }

    /** علامت‌زدن «ثبت شد» — حذف از فهرست پیگیری. */
    fun resolve(context: Context, serial: String) {
        _items.value = _items.value.filterNot { it.serial == serial }
        persist(context)
    }
}
