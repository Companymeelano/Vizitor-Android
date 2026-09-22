/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | موتور همگام‌سازی مستقیم SQL → دیتابیس محلی
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  بعد از ورود موفق (dbo.sys_users) این موتور «سرویس‌های دادهٔ» برنامه را از
 *  خودِ سرور آتیران پر می‌کند، تا همهٔ بخش‌ها (پیشخوان، ویترین، مشتریان،
 *  گزارشات، اسکنر) با دادهٔ واقعی کار کنند:
 *     ۱) کالاها + ۵ سطح قیمت + موجودی  → جدول products
 *     ۲) مشتریان مجاز ویزیتور + گروه  → جدول customers
 *     ۳) فاکتور/پیش‌فاکتورهای واقعی   → جدول server_invoices (برای گزارش‌ها)
 *
 *  قواعد رعایت‌شده:
 *   • هیچ نام جدول/ستونی حدس زده نشده (همه از ممیزی سرور)
 *   • همهٔ کوئری‌ها پارامتری و فقط SELECT
 *   • هیچ کوئری‌ای روی ترد UI اجرا نمی‌شود (باConnection روی Dispatchers.IO)
 *   • اگر بعد از یک همگام‌سازی، سرور خالی برگرداند، دادهٔ محلی قبلی پاک نمی‌شود
 *     (تا برنامه در قطعی/خلأ داده، بی‌داده نماند) — فقط وقتی دادهٔ معتبر آمده
 *     جایگزینی انجام می‌شود و جمع‌ها در همان گزارش نشان داده می‌شوند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import ir.atiran.vizitor.data.local.AppDatabase
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.data.local.ServerInvoiceEntity

object SqldirectSync {

    /** نتیجهٔ یک همگام‌سازی کامل — برای نمایش «چه چیزی فعال شد». */
    data class Report(
        val products: Int,
        val customers: Int,
        val invoices: Int,
        val durationMs: Long,
        val warnings: List<String>,
    ) {
        val summary: String
            get() {
                val base = "همگام‌سازی انجام شد ✅ — $products کالا، $customers مشتری، " +
                    "$invoices فاکتور از سرور آتیران (${durationMs / 1000} ثانیه)"
                return if (warnings.isEmpty()) base else base + " • هشدار: " + warnings.joinToString(" • ")
            }
    }

    private val data get() = MeelanoDataSource(SqlConnectionManager)

    /**
     * اجرای کامل همگام‌سازی برای کاربر واردشده.
     * @param userId شناسهٔ کاربر در dbo.sys_users
     * @param companyId شرکت (sys_users.shmo) — برای فیلتر SysID
     * @param visitorRdf کد ویزیتور (visitors.vis_rdf) برای خواندن فاکتورها
     */
    suspend fun run(
        db: AppDatabase,
        userId: Int,
        companyId: Int?,
        visitorRdf: Int?,
    ): Report {
        val started = System.currentTimeMillis()
        val warnings = ArrayList<String>()

        // ── ۱) کالاها + موجودی + ۵ سطح قیمت ─────────────────────────────────
        var productCount = 0
        runCatching {
            val groups = runCatching { data.productGroups() }.getOrElse { emptyList() }
                .associate { it.groupRdf to it.name }
            val stock = runCatching { data.stock() }.getOrElse { emptyList() }
                .groupBy { it.shka.toInt() }
                .mapValues { (_, rows) -> rows.sumOf { it.quantity } }
            val products = data.products(limit = 2000)
            if (products.isNotEmpty()) {
                val rows = products.map { p ->
                    ProductEntity(
                        id = p.shka.toInt(),
                        code = p.code.ifBlank { p.shka.toString() },
                        name = p.name.ifBlank { "کالای ${p.shka}" },
                        groupName = groups[p.groupRdf].orEmpty(),
                        price = p.priceTier1,
                        stock = stock[p.shka.toInt()] ?: p.stockVah,
                        unit = p.unit.ifBlank { "کیلو" },
                        packSize = p.packSize.toInt().coerceAtLeast(1),
                        price2 = p.priceTier2,
                        consumerPrice = p.priceTier3,
                        updatedAt = System.currentTimeMillis(),
                    )
                }
                db.products().clear()
                db.products().upsertAll(rows)
                productCount = rows.size
            } else {
                warnings += "فهرست کالا خالی برگشت"
            }
        }.onFailure { warnings += "کالا: ${it.message ?: it.javaClass.simpleName}" }

        // ── ۲) مشتریان مجاز همین ویزیتور ────────────────────────────────────
        var customerCount = 0
        val customerNames = HashMap<String, String>()
        runCatching {
            val customers = data.customersFor(userId = userId, companyId = companyId, limit = 2000)
            if (customers.isNotEmpty()) {
                val rows = customers.map { c ->
                    customerNames[c.code.ifBlank { c.shmo.toString() }] = c.name
                    CustomerEntity(
                        id = c.shmo,
                        code = c.code.ifBlank { c.shmo.toString() },
                        name = c.name,
                        groupName = c.groupName.orEmpty(),
                        city = c.cityRdf?.toString().orEmpty(),
                        address = c.address,
                        phone = c.phone,
                        lat = c.lat ?: 0.0,
                        lng = c.lng ?: 0.0,
                        // وضعیت اعتباری از دادهٔ واقعی: لیست سیاه/غیرفعال بودن
                        creditOk = (c.blackList ?: 0) != 1 && !c.active.equals("f", true),
                        debt = c.debt,
                    )
                }
                db.customers().clear()
                db.customers().upsertAll(rows)
                customerCount = rows.size
            } else {
                warnings += "فهرست مشتریان مجاز این کاربر خالی است"
            }
        }.onFailure { warnings += "مشتریان: ${it.message ?: it.javaClass.simpleName}" }

        // ── ۳) فاکتور/پیش‌فاکتورهای واقعی ویزیتور (بخش گزارش‌ها) ─────────────
        var invoiceCount = 0
        if (visitorRdf != null) {
            runCatching {
                val invoices = data.invoicesForVisitor(visitorRdf = visitorRdf, companyId = companyId)
                if (invoices.isNotEmpty()) {
                    db.serverInvoices().clear()
                    db.serverInvoices().upsertAll(
                        invoices.map { v ->
                            ServerInvoiceEntity(
                                number = v.number,
                                kind = if (v.isPreInvoice) "پیش‌فاکتور" else "فاکتور",
                                dateText = v.dateText,
                                customerCode = v.customerCode,
                                customerName = customerNames[v.customerCode].orEmpty(),
                                total = v.total,
                                discount = v.discount,
                            )
                        }
                    )
                    invoiceCount = invoices.size
                }
            }.onFailure { warnings += "فاکتورها: ${it.message ?: it.javaClass.simpleName}" }
        } else {
            warnings += "کد ویزیتور این کاربر پیدا نشد؛ فاکتورها خوانده نشد"
        }

        return Report(
            products = productCount,
            customers = customerCount,
            invoices = invoiceCount,
            durationMs = System.currentTimeMillis() - started,
            warnings = warnings,
        )
    }
}
