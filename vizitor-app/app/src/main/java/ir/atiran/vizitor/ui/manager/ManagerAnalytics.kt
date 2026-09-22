/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تحلیل داده برای پنل مدیریت (v2.16.0)
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  همهٔ محاسبات روی «دادهٔ واقعی همگام‌شده از سرور آتیران» انجام می‌شود:
 *      server_invoices ← dbo.sailfact / dbo.sailfact_pish
 *      customers       ← dbo.CUSTOMERS  (بدهی، شهر، گروه)
 *      products        ← dbo.inventory + dbo.forosh_price
 *  هیچ عدد ساختگی تولید نمی‌شود؛ اگر داده‌ای نباشد، صفر/«داده‌ای نیست» نشان
 *  داده می‌شود. جمع‌ها با Long انجام می‌شود تا از سرریز جلوگیری شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.manager

import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.data.local.ServerInvoiceEntity

object ManagerAnalytics {

    data class Snapshot(
        // ── شاخص‌های کلیدی ──
        val invoiceCount: Int,
        val totalSales: Long,
        val totalDiscount: Long,
        val averageInvoice: Long,
        val preInvoiceCount: Int,
        val officialInvoiceCount: Int,
        val customerCount: Int,
        val debtorCount: Int,
        val totalDebt: Long,
        val productCount: Int,
        val stockValue: Long,
        val outOfStockCount: Int,
        // ── نمودار/جدول ──
        val salesByDay: List<Pair<String, Long>>,
        val invoiceSplit: List<Triple<String, Long, Int>>,   // (نام، مقدار، رنگ)
        val topCustomers: List<Triple<String, String, Long>>, // (نام، کد، جمع خرید)
        val debtors: List<CustomerEntity>,
        val categoryValue: List<Triple<String, Long, Int>>,
        val topProducts: List<Triple<String, String, Long>>,  // (نام، گروه، ارزش موجودی)
        val recentInvoices: List<ServerInvoiceEntity>,
    )

    /** رنگ‌های نمودار (ثابت — تا همه‌جا یکسان باشد). */
    const val C_GOLD = 0xFFE8B23F.toInt()
    const val C_PURPLE = 0xFF8B3FF0.toInt()
    const val C_GREEN = 0xFF19D67C.toInt()
    const val C_RED = 0xFFE5484D.toInt()
    const val C_BLUE = 0xFF3B82F6.toInt()

    fun snapshot(
        invoices: List<ServerInvoiceEntity>,
        customers: List<CustomerEntity>,
        products: List<ProductEntity>,
    ): Snapshot {
        val totalSales = invoices.sumOf { it.total }
        val totalDiscount = invoices.sumOf { it.discount }
        val pre = invoices.count { it.kind.contains("پیش") }
        val official = invoices.size - pre

        val debtors = customers.filter { it.debt > 0 }.sortedByDescending { it.debt }

        // فروش به تفکیک تاریخ (همان رشتهٔ تاریخ واقعی سرور، مرتب‌شدهٔ نزولی)
        val byDay = invoices
            .groupBy { it.dateText.ifBlank { "بدون تاریخ" } }
            .mapValues { (_, rows) -> rows.sumOf { it.total } }
            .entries
            .sortedByDescending { it.key }
            .take(8)
            .map { it.key to it.value }
            .reversed()

        val split = listOf(
            Triple("فاکتور رسمی", official.toLong(), C_GREEN),
            Triple("پیش‌فاکتور", pre.toLong(), C_GOLD),
        )

        // برترین مشتریان بر اساس جمع مبلغ فاکتورها (نام از جدول مشتریان)
        val nameByKey = HashMap<String, String>()
        customers.forEach { c ->
            nameByKey[c.id.toString()] = c.name
            nameByKey[c.code] = c.name
        }
        val topCustomers = invoices
            .groupBy { it.customerCode }
            .mapValues { (_, rows) -> rows.sumOf { it.total } }
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { (code, sum) ->
                val name = nameByKey[code] ?: "کد مشتری $code"
                Triple(name, code, sum)
            }

        // سهم دسته‌های کالا از «ارزش موجودی» (قیمت × موجودی)
        val categoryValue = products
            .groupBy { it.category.ifBlank { "سایر" } }
            .mapValues { (_, rows) -> rows.sumOf { p -> (p.price.coerceAtLeast(0)) * p.stock.coerceAtLeast(0).toLong() } }
            .entries
            .sortedByDescending { it.value }
            .take(6)
            .mapIndexed { i, e ->
                Triple(e.key, e.value, ChartColors[i % ChartColors.size])
            }

        val topProducts = products
            .map { p -> Triple(p.name, p.groupName.ifBlank { p.category }, p.price * p.stock.toLong()) }
            .sortedByDescending { it.third }
            .take(8)

        val stockValue = products.sumOf { (it.price.coerceAtLeast(0)) * it.stock.coerceAtLeast(0).toLong() }

        return Snapshot(
            invoiceCount = invoices.size,
            totalSales = totalSales,
            totalDiscount = totalDiscount,
            averageInvoice = if (invoices.isEmpty()) 0L else totalSales / invoices.size,
            preInvoiceCount = pre,
            officialInvoiceCount = official,
            customerCount = customers.size,
            debtorCount = debtors.size,
            totalDebt = debtors.sumOf { it.debt },
            productCount = products.size,
            stockValue = stockValue,
            outOfStockCount = products.count { it.stock <= 0.0 },
            salesByDay = byDay,
            invoiceSplit = split,
            topCustomers = topCustomers,
            debtors = debtors.take(8),
            categoryValue = categoryValue,
            topProducts = topProducts,
            recentInvoices = invoices.take(10),
        )
    }

    private val ChartColors = listOf(C_GOLD, C_PURPLE, C_GREEN, C_BLUE, C_RED, 0xFF22D3EE.toInt())
}
