/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نگاشت بخش‌ها به جدول‌های سرور (v2.20.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  پورتِ کاملِ بخش «اتصال جداول سرور» برنامهٔ مرجع (app-debug-40):
 *
 *    ▸ هر بخش گزارش (مشتریان / کالاها / فروش و فاکتورها / چک‌ها /
 *      بانک‌ها و حساب‌ها) به یک جدول واقعی سرور وصل می‌شود.
 *    ▸ «تشخیص خودکار جداول»: با نام‌های حدسی فارسی/انگلیسی جدول واقعی
 *      پیدا می‌شود (مثل جدول‌های نام‌برده در TableHeuristics مرجع).
 *    ▸ نقش ستون‌ها (نام، مبلغ، تاریخ، کد) هوشمند تشخیص داده می‌شود.
 *    ▸ نگاشت روی همین گوشی ذخیره می‌شود و اتصال فقط-خواندنی است.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

/** بخش‌های گزارش مدیریت (همان پنج بخش مرجع). */
enum class MaSection(val key: String, val label: String, val icon: String) {
    CUSTOMERS("customers", "مشتریان", "person"),
    PRODUCTS("products", "کالاها", "store"),
    INVOICES("invoices", "فروش و فاکتورها", "receipt"),
    CHECKS("checks", "چک‌ها", "sell"),
    BANKS("banks", "بانک‌ها و حساب‌ها", "wallet"),
}

/** نقش ستون‌های یک جدول: عنوان، مبلغ، تاریخ، کد. */
data class MaTableRoles(
    val titleCol: String? = null,
    val amountCol: String? = null,
    val dateCol: String? = null,
    val codeCol: String? = null,
)

/** نگاشت بخش‌ها به جدول‌های واقعی سرور (مثل `dbo.CUSTOMERS`). */
data class MaSectionMap(
    val customers: String? = null,
    val products: String? = null,
    val invoices: String? = null,
    val checks: String? = null,
    val banks: String? = null,
) {
    fun refOf(section: MaSection): String? = when (section) {
        MaSection.CUSTOMERS -> customers
        MaSection.PRODUCTS -> products
        MaSection.INVOICES -> invoices
        MaSection.CHECKS -> checks
        MaSection.BANKS -> banks
    }

    fun with(section: MaSection, ref: String?): MaSectionMap = when (section) {
        MaSection.CUSTOMERS -> copy(customers = ref)
        MaSection.PRODUCTS -> copy(products = ref)
        MaSection.INVOICES -> copy(invoices = ref)
        MaSection.CHECKS -> copy(checks = ref)
        MaSection.BANKS -> copy(banks = ref)
    }

    val mappedCount: Int
        get() = listOf(customers, products, invoices, checks, banks).count { !it.isNullOrBlank() }
}

/** نام‌های حدسی جدول‌ها و تشخیص نقش ستون‌ها. */
object MaTableHeuristics {

    /** نام‌های حدسی جدول هر بخش — ترتیب اهمیت رعایت شده (مثل نسخهٔ مرجع). */
    fun candidates(section: MaSection): List<String> = when (section) {
        MaSection.CUSTOMERS -> listOf(
            "customer", "moshtar", "مشتری", "CUSTOMERS", "ashkh", "اشخاص", "person", "client",
            "partner", "طرف", "customers"
        )
        MaSection.PRODUCTS -> listOf(
            "kala", "KALA", "کالا", "product", "item", "goods", "جنس", "merch", "inventory", "products"
        )
        MaSection.INVOICES -> listOf(
            "factor", "faktor", "فاکتور", "invoice", "sale", "forosh", "فروش", "order",
            "sailfact", "invoices"
        )
        MaSection.CHECKS -> listOf(
            "check", "cheque", "چک", "checks", "cheques", "check_detail"
        )
        MaSection.BANKS -> listOf(
            "bank", "بانک", "banks", "account", "hesab", "حساب"
        )
    }

    private val NUMERIC = setOf(
        "int", "bigint", "smallint", "tinyint", "bit", "decimal", "numeric", "float", "real",
        "money", "smallmoney"
    )
    private val MONEYISH = setOf("decimal", "numeric", "money", "smallmoney", "float", "real")
    private val TITLE_NAMES = listOf(
        "fullname", "customername", "kalaname", "name", "title", "onvan", "نام", "شرح"
    )
    private val AMOUNT_NAMES = listOf(
        "mablagh", "amount", "مبلغ", "price", "ghimat", "قیمت", "bedehkar", "bestankar",
        "mandeh", "mande", "مانده", "balance", "total", "jam", "جمع", "allfel", "mabdaryaft", "tdf"
    )
    private val DATE_NAMES = listOf("date", "tarikh", "تاریخ")
    private val CODE_NAMES = listOf("code", "shomare", "کد", "id", "shmo", "shka", "shfacfo")

    fun isNumeric(type: String): Boolean = NUMERIC.contains(type.lowercase(Locale.US))
    fun isMoneyish(type: String): Boolean = MONEYISH.contains(type.lowercase(Locale.US))

    private fun hits(name: String, needles: List<String>): Boolean {
        val n = name.lowercase(Locale.US).replace("_", "").replace(" ", "")
        return needles.any { k -> n.contains(k.lowercase(Locale.US).replace("_", "")) }
    }

    fun isTitleish(name: String): Boolean = hits(name, TITLE_NAMES)
    fun isAmountish(name: String): Boolean = hits(name, AMOUNT_NAMES)
    fun isDateish(name: String): Boolean = hits(name, DATE_NAMES)
    fun isCodeish(name: String): Boolean = hits(name, CODE_NAMES)

    /** تشخیص نقش چهار ستون اصلی یک جدول از روی نام و نوع. */
    fun roles(columns: List<MaColumn>): MaTableRoles {
        val title = columns.firstOrNull { it.type.lowercase(Locale.US) !in NUMERIC && isTitleish(it.name) }?.name
        val amount = columns.firstOrNull { isMoneyish(it.type) && isAmountish(it.name) }?.name
            ?: columns.firstOrNull { isMoneyish(it.type) }?.name
        val date = columns.firstOrNull { isDateish(it.name) }?.name
        val code = columns.firstOrNull { isCodeish(it.name) }?.name
        return MaTableRoles(titleCol = title, amountCol = amount, dateCol = date, codeCol = code)
    }
}

/** ذخیره‌سازی نگاشت بخش‌ها روی همین گوشی (بدون هیچ داده‌ای روی سرور). */
object MaSectionStore {

    private const val PREFS = "vizitor_manager"
    private const val KEY_MAP = "section_map_v1"
    private const val KEY_HOST_LOCAL = "host_local"
    private const val KEY_HOST_NET = "host_net"
    private const val KEY_USE_NET = "use_net"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    private fun p(): SharedPreferences? = prefs

    fun save(map: MaSectionMap) {
        val text = listOf(
            MaSection.CUSTOMERS.key to map.customers,
            MaSection.PRODUCTS.key to map.products,
            MaSection.INVOICES.key to map.invoices,
            MaSection.CHECKS.key to map.checks,
            MaSection.BANKS.key to map.banks,
        ).joinToString(";") { (k, v) -> "$k=${v ?: ""}" }
        p()?.edit()?.putString(KEY_MAP, text)?.apply()
    }

    fun load(): MaSectionMap {
        val text = p()?.getString(KEY_MAP, null) ?: return MaSectionMap()
        val out = MaSectionMap()
        var map = out
        text.split(";").forEach { part ->
            val i = part.indexOf('=')
            if (i <= 0) return@forEach
            val key = part.substring(0, i).trim()
            val value = part.substring(i + 1).trim().ifBlank { null }
            val section = MaSection.values().firstOrNull { it.key == key } ?: return@forEach
            map = map.with(section, value)
        }
        return map
    }

    /** حالت «محلی/اینترنت» (همان دو حالت صفحهٔ ورود مرجع). */
    fun saveMode(hostLocal: String, hostNet: String, useNet: Boolean) {
        p()?.edit()
            ?.putString(KEY_HOST_LOCAL, hostLocal)
            ?.putString(KEY_HOST_NET, hostNet)
            ?.putBoolean(KEY_USE_NET, useNet)
            ?.apply()
    }

    fun loadMode(): Triple<String, String, Boolean> {
        val pr = p() ?: return Triple("", "", false)
        return Triple(
            pr.getString(KEY_HOST_LOCAL, "") ?: "",
            pr.getString(KEY_HOST_NET, "") ?: "",
            pr.getBoolean(KEY_USE_NET, false),
        )
    }
}

/** ابزارهای نگاشت: تفکیک `schema.table` و تشخیص خودکار همهٔ بخش‌ها. */
object MaMapping {

    fun split(ref: String): Pair<String, String>? {
        val clean = ref.trim()
        val dot = clean.indexOf('.')
        if (dot <= 0 || dot >= clean.length - 1) return null
        val schema = clean.substring(0, dot).trim()
        val table = clean.substring(dot + 1).trim()
        if (schema.isBlank() || table.isBlank()) return null
        return schema to table
    }

    /**
     * تشخیض خودکار: برای هر بخش، جدول واقعی با نام‌های حدسی پیدا می‌شود.
     * نتیجه فقط شامل بخش‌هایی است که واقعاً پیدا شده‌اند.
     */
    suspend fun autoDetect(): MaSectionMap {
        var map = MaSectionMap()
        MaSection.values().forEach { section ->
            val found = runCatching { MaSqlEngine.findTable(MaTableHeuristics.candidates(section)) }.getOrNull()
            if (!found.isNullOrBlank()) map = map.with(section, found)
        }
        return map
    }

    /** نقش ستون‌های یک جدول نگاشت‌شده (برای گزارش‌های مبلغ/تاریخ). */
    suspend fun rolesOf(ref: String): MaTableRoles? {
        val (schema, table) = split(ref) ?: return null
        val cols = runCatching { MaSqlEngine.columnsOf(schema, table) }.getOrNull() ?: return null
        return MaTableHeuristics.roles(cols)
    }
}
