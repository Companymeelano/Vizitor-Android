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
        // ── v2.18.0: دامنهٔ همگام‌سازی به «همهٔ فعالیت‌های ویزیتور» گسترده شد ──
        /** ویزیت‌های ثبت‌شدهٔ امروزِ همین ویزیتور (dbo.Visit). */
        val visitsToday: Int = 0,
        /** تعداد مسیرهای تعریف‌شدهٔ همین ویزیتور (dbo.masir). */
        val routeCount: Int = 0,
        /** سهمیهٔ باقی‌ماندهٔ فاکتور (visitors.TedadFactorMojazMande). */
        val quotaLeft: Int? = null,
        /** اعتبار ویزیتور در ERP (visitors.eteb). */
        val credit: Long? = null,
    ) {
        val summary: String
            get() {
                val base = "همگام‌سازی انجام شد ✅ — $products کالا، $customers مشتری، " +
                    "$invoices فاکتور از سرور آتیران (${durationMs / 1000} ثانیه)"
                val extra = buildList {
                    if (visitsToday > 0) add("$visitsToday ویزیت امروز")
                    if (routeCount > 0) add("$routeCount مسیر تعریف‌شده")
                }
                val withExtra = if (extra.isEmpty()) base else base + " • " + extra.joinToString(" • ")
                return if (warnings.isEmpty()) withExtra else withExtra + " • هشدار: " + warnings.joinToString(" • ")
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

        // ── v2.18.0: پیش از هر کوئری، اتصال تضمین می‌شود ─────────────────────
        //  اگر برنامه بسته و باز شده باشد، به‌جای خطای «اتصال برقرار نیست»،
        //  همان تنظیمات ذخیره‌شدهٔ گوشی دوباره وصل می‌شود.
        if (!SqlConnectionManager.ensureConnected()) {
            val why = (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
                ?: "اتصال برقرار نشد"
            return Report(
                products = 0, customers = 0, invoices = 0, durationMs = 0,
                warnings = listOf("اتصال: $why"),
            )
        }

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
                    val name = p.name.ifBlank { "کالای ${p.shka}" }
                    val tiers = listOf(
                        p.priceTier1, p.priceTier2, p.priceTier3, p.priceTier4, p.priceTier5
                    )
                    // میانگین قیمت = میانگین سطح‌های غیرصفرِ همان کالا (دادهٔ واقعی سرور)
                    var avg = ProductDefaults.averageOf(tiers)
                    if (avg <= 0L) avg = ProductDefaults.midOf(p.minPrice, p.maxPrice)
                    // قیمت مصرف‌کننده: اول forosh3 سرور؛ اگر صفر بود ⇒ پیش‌فرض بر اساس نام کالا
                    val consumerFromServer = p.priceTier3
                    val consumer = if (consumerFromServer > 0L) consumerFromServer
                    else ProductDefaults.consumerFromName(
                        base = if (p.priceTier1 > 0L) p.priceTier1 else avg,
                        name = name,
                    )
                    ProductEntity(
                        id = p.shka.toInt(),
                        code = p.code.ifBlank { p.shka.toString() },
                        name = name,
                        groupName = groups[p.groupRdf].orEmpty(),
                        price = p.priceTier1,
                        stock = stock[p.shka.toInt()] ?: p.stockVah,
                        unit = p.unit.ifBlank { "کیلو" },
                        packSize = p.packSize.toInt().coerceAtLeast(1),
                        price2 = p.priceTier2,
                        consumerPrice = consumer,
                        price4 = p.priceTier4,
                        price5 = p.priceTier5,
                        minPrice = p.minPrice,
                        maxPrice = p.maxPrice,
                        avgPrice = avg,
                        consumerIsDefault = consumerFromServer <= 0L && consumer > 0L,
                        category = ProductDefaults.categoryOf(name),
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

        // ── ۲) مشتریان (سه مسیر پله‌ای + سازگار با ستون‌های واقعی همین سرور) ──
        var customerCount = 0
        val customerNames = HashMap<String, String>()
        runCatching {
            val r = CustomerSync.fetch(
                userId = userId,
                companyId = companyId,
                visitorRdf = visitorRdf,
                limit = 3000,
            )
            r.notes.forEach { warnings += it }
            if (r.error != null && r.customers.isEmpty()) {
                warnings += "مشتریان: ${r.error}"
            }
            if (r.customers.isNotEmpty()) {
                // کلید نام: هم شمارهٔ مشتری (SHMO) و هم کد مشتری — چون فاکتورها SHMO را
                // در ستون shmo نگه می‌دارند و قبلاً فقط «کد» نگاشت می‌شد و نام‌ها خالی می‌ماند.
                r.customers.forEach { c ->
                    customerNames[c.id.toString()] = c.name
                    if (c.code.isNotBlank()) customerNames[c.code] = c.name
                }
                db.customers().clear()
                db.customers().upsertAll(r.customers)
                customerCount = r.customers.size
                if (r.source != "sys_cus") warnings += "مشتریان از مسیر «${r.sourceLabel}» خوانده شد"
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
                    val named = invoices.count { customerNames[it.customerCode]?.isNotBlank() == true }
                    if (named == 0) warnings += "نام مشتری روی فاکتورها پیدا نشد (کلید shmo)"
                } else {
                    warnings += "برای این ویزیتور فاکتور یا پیش‌فاکتوری در سرور ثبت نشده"
                }
            }.onFailure { warnings += "فاکتورها: ${it.message ?: it.javaClass.simpleName}" }
        } else {
            warnings += "کد ویزیتور این کاربر پیدا نشد؛ فاکتورها خوانده نشد"
        }

        // ── ۴) فعالیت‌های امروز: ویزیت‌های ثبت‌شده + مسیرهای تعریف‌شده ────────
        val visitsToday = runCatching { VizitorGateway.todayVisits(visitorRdf) }.getOrDefault(0)
        val routes = runCatching { VizitorGateway.routesFor(visitorRdf) }.getOrDefault(emptyList())
        val limits = runCatching { VizitorGateway.limits(userId, companyId) }.getOrDefault(
            VizitorGateway.VisitorLimits()
        )
        if (routes.isEmpty() && visitorRdf != null) {
            // نبود مسیر برای همهٔ ویزیتورها عادی است؛ فقط یک یادداشت کوتاه می‌آید
            warnings += "مسیری برای این ویزیتور در dbo.masir تعریف نشده"
        }

        return Report(
            products = productCount,
            customers = customerCount,
            invoices = invoiceCount,
            durationMs = System.currentTimeMillis() - started,
            warnings = warnings,
            visitsToday = visitsToday,
            routeCount = routes.size,
            quotaLeft = limits.quotaLeft,
            credit = limits.credit,
        )
    }
}
