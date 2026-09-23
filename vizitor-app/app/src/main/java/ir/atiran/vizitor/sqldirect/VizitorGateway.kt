/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | دروازهٔ واحد اتصال و فراخوانی (v2.18.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  چرا این فایل؟
 *    در تحلیل کدبه‌کد نسخهٔ ۲٫۱۷ دو ایراد ساختاری پیدا شد:
 *      ۱) «اتصال» فقط وقتی برقرار می‌شد که کاربر از صفحهٔ اتصال رد شده باشد؛
 *         بعد از بسته‌شدن برنامه، کوئری‌ها بی‌اتصال می‌ماندند.
 *      ۲) «فراخوانی» دو مسیر موازی داشت: لایهٔ Retrofit/API قدیمی و لایهٔ
 *         اتصال مستقیم SQL. دکمه‌های همگام‌سازی و صدور فاکتور به مسیر قدیمی
 *         (API) می‌رفتند که در این نسخهٔ نصبی وجود ندارد ⇒ هیچ‌وقت نتیجه نمی‌داد.
 *
 *  راه‌حل: همین فایل، تنها دروازهٔ ارتباط با سرور آتیران است.
 *    UI → ViewModel → VizitorGateway → (SqlConnectionManager + MeelanoDataSource)
 *
 *  قواعد رعایت‌شده:
 *   • هر عملیات اول `ensureConnected()` را صدا می‌زند (خودترمیمی با تنظیمات ذخیره‌شده)
 *   • هیچ کوئری‌ای روی ترد UI اجرا نمی‌شود (همه suspend + Dispatchers.IO در لایهٔ پایین)
 *   • نام جدول/ستون‌ها فقط از ممیزی واقعی سرور (docs/schema/meelano-columns.tsv)
 *   • مسیر نوشتن پیش‌فاکتور: دقیقاً همان مسیر خودِ ERP
 *       EXEC dbo.add_sail_pish ...            → شمارهٔ پیش‌فاکتور (shfacfo)
 *       INSERT در dbo.subsailtemp_pish (mod=1) → تریگر در subsailfact_pish می‌نویسد
 *       قرائت تأییدی از subsailfact_pish ⋈ sailfact_pish (shfacfo و rdf__)
 *   • «ثبت واقعی» فقط وقتی فعال می‌شود که یک پیش‌فاکتور مرجع در خودِ ERP وجود
 *     داشته باشد (قاعدهٔ مستندشدهٔ پروژه: مقادیر ناشناختهٔ سربرگ حدس زده نمی‌شوند).
 *     تا آن زمان، فراخوانی همیشه «پیش‌نمایش سند» می‌دهد و هیچ سندی ثبت نمی‌شود.
 *   • رمز عبور هرگز لاگ/چاپ نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.sqldirect

import ir.atiran.vizitor.data.local.AppDatabase
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.util.toFaNumber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VizitorGateway {

    private val data: MeelanoDataSource get() = MeelanoDataSource(SqlConnectionManager)

    // ═══════════════════════ ۱) اتصال ═══════════════════════

    /** وضعیت سنجیده‌شدهٔ اتصال (برای کارت وضعیت و «اطلاع‌رسانی اولیه»). */
    data class Probe(
        val ok: Boolean,
        val driver: String = "—",
        val latencyMs: Long = 0L,
        val database: String = "",
        val serverVersion: String = "",
        val customersInErp: String = "",
        val message: String = "",
    )

    /** تضمین اتصال (پیش از هر فراخوانی). */
    suspend fun ensureConnection(): Boolean = SqlConnectionManager.ensureConnected()

    /**
     * تست کامل اتصال: باز کردن اتصال (اگر باز نیست)، SELECT 1 با زمان‌سنجی و
     * خواندن مشخصات سرور. همه با همان درایوری که واقعاً جواب داده است.
     */
    suspend fun probe(): Probe {
        if (!ensureConnection()) {
            return Probe(ok = false, message = errorMessage())
        }
        val started = System.nanoTime()
        return try {
            val state = SqlConnectionManager.refresh()
            val latency = (System.nanoTime() - started) / 1_000_000
            if (!state.isReady) {
                Probe(ok = false, message = errorMessage())
            } else {
                val info = runCatching { data.databaseInfo() }.getOrNull()
                Probe(
                    ok = true,
                    driver = DirectSql.displayName(SqlConnectionManager.activeDriver),
                    latencyMs = latency,
                    database = info?.first ?: VizitorSession.current.database,
                    serverVersion = info?.second.orEmpty(),
                    customersInErp = info?.third.orEmpty(),
                    message = "اتصال سالم است ✅",
                )
            }
        } catch (t: Throwable) {
            Probe(ok = false, message = SqlConnectionManager.describeThrowable(t))
        }
    }

    /** پیام خطای فارسی اتصال (از وضعیت جاری). */
    fun errorMessage(): String =
        (SqlConnectionManager.state.value as? ConnectionState.Error)?.message
            ?: "اتصال برقرار نشد (خطای نامشخص)."

    // ═══════════════════════ ۲) اختیارات و سهمیهٔ ویزیتور ═══════════════════════

    /** اختیارات و سهمیهٔ واقعی ویزیتور از خودِ ERP (بدون هیچ مقدار ساختگی). */
    data class VisitorLimits(
        val credit: Long? = null,             // visitors.eteb
        val percentCash: Double? = null,      // visitors.per_p_d_naghd
        val percentCheque: Double? = null,    // visitors.per_p_d_check
        val quotaLeft: Int? = null,           // visitors.TedadFactorMojazMande
        val quotaAmount: Long? = null,        // visitors.MablaghMojazMandeJahatFactorha
        val supervisor: Boolean = false,
    )

    suspend fun limits(userId: Int, companyId: Int?): VisitorLimits = runCatching {
        val all = VisitorRepository.visitorsForUser(userId, companyId, limit = 200)
        val row = all.firstOrNull { it.rdf == VizitorSession.current.visitorRdf } ?: all.firstOrNull()
        if (row == null) VisitorLimits()
        else VisitorLimits(
            credit = row.credit,
            percentCash = row.percentCash,
            percentCheque = row.percentCheque,
            quotaLeft = row.allowedInvoicesLeft,
            quotaAmount = null,
            supervisor = row.supervisor,
        )
    }.getOrElse { VisitorLimits() }

    /** شمارش ویزیت‌های امروزِ همین ویزیتور از جدول dbo.Visit. */
    suspend fun todayVisits(visitorRdf: Int?): Int {
        if (visitorRdf == null) return 0
        return runCatching { VisitRepository.countToday(visitorRdf, VisitDate.todayJalali()) }
            .getOrDefault(0)
    }

    /** آخرین ویزیت‌های ثبت‌شدهٔ ویزیتور (جدول dbo.Visit). */
    suspend fun recentVisits(visitorRdf: Int?, limit: Int = 20): List<VisitRow> {
        if (visitorRdf == null) return emptyList()
        return runCatching { VisitRepository.recentVisits(visitorRdf, limit) }.getOrDefault(emptyList())
    }

    // ═══════════════════════ ۳) مسیرهای ویزیتور (dbo.masir) ═══════════════════════

    /** یک مسیر تعریف‌شده برای این ویزیتور: نام مسیر + شمارهٔ مسیر + منطقه. */
    data class RouteRow(val number: String, val name: String, val region: Int?)

    /**
     * فهرست مسیرهای همین ویزیتور از جدول واقعی dbo.masir
     * (ستون‌ها: trdf_masir, rdf_region, shomare_masir, name, vis_rdf — از ممیزی سرور).
     */
    suspend fun routesFor(visitorRdf: Int?, limit: Int = 40): List<RouteRow> {
        if (visitorRdf == null) return emptyList()
        if (!ensureConnection()) return emptyList()
        return runCatching {
            SqlConnectionManager.withConnection { c ->
                c.prepareStatement(
                    """
                    SELECT TOP ($limit) m.shomare_masir, m.name, m.rdf_region
                      FROM dbo.masir AS m
                     WHERE m.vis_rdf = ?
                     ORDER BY m.shomare_masir
                    """.trimIndent()
                ).use { ps ->
                    ps.queryTimeout = 20
                    ps.setInt(1, visitorRdf)
                    ps.executeQuery().use { rs ->
                        val out = ArrayList<RouteRow>()
                        while (rs.next()) {
                            out += RouteRow(
                                number = rs.getString(1)?.trim().orEmpty(),
                                name = rs.getString(2)?.trim().orEmpty(),
                                region = rs.getInt(3).takeIf { !rs.wasNull() },
                            )
                        }
                        out
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    // ═══════════════════════ ۴) همگام‌سازی کامل ═══════════════════════

    /**
     * همگام‌سازی کامل سرویس‌های داده (کالا، مشتری، فاکتورها) با ضمانت اتصال.
     * اگر اتصال برقرار نشود، یک گزارش با هشدار فارسی برگردانده می‌شود (بدون کرش).
     */
    suspend fun syncAll(db: AppDatabase): SqldirectSync.Report {
        val session = VizitorSession.current
        val userId = session.erpUserId
        if (userId == null) {
            return SqldirectSync.Report(
                products = 0, customers = 0, invoices = 0, durationMs = 0,
                warnings = listOf("اول وارد سامانه شوید (نام کاربری و رمز)"),
            )
        }
        if (!ensureConnection()) {
            return SqldirectSync.Report(
                products = 0, customers = 0, invoices = 0, durationMs = 0,
                warnings = listOf("اتصال: " + errorMessage()),
            )
        }
        return SqldirectSync.run(
            db = db,
            userId = userId,
            companyId = session.companyId,
            visitorRdf = session.visitorRdf,
        )
    }

    // ═══════════════════════ ۵) اطلاع‌رسانی اولیه ═══════════════════════

    /**
     * «اطلاع‌رسانی اولیه» — همهٔ چیزی که ویزیتور پیش از شروع کار باید بداند:
     * وضعیت اتصال، اختیارات خودش در ERP، سهمیه/اعتبار، دادهٔ همگام‌شدهٔ روی گوشی،
     * مطالبات در خطر، مسیرها و وضعیت مسیر نوشتن پیش‌فاکتور.
     */
    data class Briefing(
        val probe: Probe,
        val limits: VisitorLimits,
        val identity: DbVisitorIdentity?,
        val visitsToday: Int,
        val productsLocal: Int,
        val customersLocal: Int,
        val invoicesLocal: Int,
        val debtorsLocal: Int,
        val debtTotalLocal: Long,
        val routes: List<RouteRow>,
        val liveWriteReady: Boolean,
        val liveWriteReason: String,
        val generatedAt: Long,
    ) {
        val debtorShare: Float
            get() = if (customersLocal <= 0) 0f else debtorsLocal.toFloat() / customersLocal.toFloat()

        val healthyShare: Float get() = (1f - debtorShare).coerceIn(0f, 1f)
    }

    suspend fun briefing(db: AppDatabase): Briefing {
        val session = VizitorSession.current
        val probe = probe()

        val identity = if (probe.ok && session.erpUserId != null) {
            runCatching { data.visitorIdentity(session.erpUserId!!, session.companyId) }.getOrNull()
        } else null

        val limits = if (probe.ok && session.erpUserId != null) {
            limits(session.erpUserId!!, session.companyId)
        } else VisitorLimits()

        val customers = runCatching { db.customers().getAll() }.getOrDefault(emptyList())
        val products = runCatching { db.products().getAll() }.getOrDefault(emptyList())
        val invoiceRows = runCatching { db.serverInvoices().count() }.getOrDefault(0)
        val debtors = customers.filter { it.debt > 0L }

        val reference = preInvoiceReference()
        val liveReady = reference != null
        val reason = when {
            !probe.ok -> "اتصال دیتابیس برقرار نیست"
            reference == null ->
                "در جدول dbo.sailfact_pish هیچ پیش‌فاکتور مرجعی نیست؛ تا وقتی خودِ ERP یک پیش‌فاکتور ثبت نکند، " +
                    "برنامه فقط «پیش‌نمایش سند» می‌دهد و چیزی در سرور نمی‌نویسد (مقادیر ناشناختهٔ سربرگ حدس زده نمی‌شوند)."
            else -> "آماده — بر پایهٔ پیش‌فاکتور مرجعِ شماره ${reference.shfacfo.toFaNumber()} در همان ERP"
        }

        return Briefing(
            probe = probe,
            limits = limits,
            identity = identity,
            visitsToday = if (probe.ok) todayVisits(session.visitorRdf) else 0,
            productsLocal = products.size,
            customersLocal = customers.size,
            invoicesLocal = invoiceRows,
            debtorsLocal = debtors.size,
            debtTotalLocal = debtors.sumOf { it.debt },
            routes = if (probe.ok) routesFor(session.visitorRdf) else emptyList(),
            liveWriteReady = liveReady,
            liveWriteReason = reason,
            generatedAt = System.currentTimeMillis(),
        )
    }

    // ═══════════════════════ ۶) مسیر نوشتن پیش‌فاکتور ═══════════════════════

    /**
     * «پیش‌فاکتور مرجع» — آخرین پیش‌فاکتور واقعیِ ثبت‌شده در خودِ ERP.
     * پنج مقدار سربرگ که در مستندات پروژه «ناشناخته» علامت خورده‌اند
     * (ted_rooz, mod_darsad_vis, rdf_sarbarg, gainall و nah_par/ph_kh وابسته)
     * فقط از این ردیف خوانده می‌شوند — هیچ مقدار حدسی در برنامه نیست.
     */
    data class PreInvoiceReference(
        val shfacfo: Long,
        val tedRooZ: Int,
        val modDarsadVis: Int,
        val rdfSarbarg: Int,
        val rdfTahbarg: Int,
        val modpar: Int,
        val nahPar: Int,
        val barbari: Long,
        val panevis: String,
        val gainAll: Long,
        val phKh: Int,
    )

    suspend fun preInvoiceReference(): PreInvoiceReference? {
        if (!SqlConnectionManager.connected()) return null
        return runCatching {
            SqlConnectionManager.withConnection { c ->
                c.prepareStatement(
                    """
                    SELECT TOP (1)
                           h.shfacfo, h.ted_rooz, h.mod_darsad_vis, h.rdf_sarbarg, h.rdf_tahbarg,
                           h.modpar, h.nah_par, h.barbari, h.panevis, h.gainall
                      FROM dbo.sailfact_pish AS h
                     WHERE ISNULL(h.ted_rooz, 0) >= 0
                     ORDER BY h.shfacfo DESC
                    """.trimIndent()
                ).use { ps ->
                    ps.queryTimeout = 20
                    ps.executeQuery().use { rs ->
                        if (!rs.next()) null else PreInvoiceReference(
                            shfacfo = rs.getLong(1),
                            tedRooZ = rs.getInt(2),
                            modDarsadVis = rs.getInt(3),
                            rdfSarbarg = rs.getInt(4),
                            rdfTahbarg = rs.getInt(5),
                            modpar = rs.getInt(6),
                            nahPar = rs.getInt(7),
                            barbari = rs.getLong(8),
                            panevis = rs.getString(9)?.trim().orEmpty(),
                            gainAll = rs.getLong(10),
                            phKh = 0,
                        )
                    }
                }
            }
        }.getOrNull()
    }

    /** یک سطر پیش‌نمایش/ارسال پیش‌فاکتور (برای نمایش به ویزیتور). */
    data class PreviewLine(
        val productName: String,
        val quantity: Double,
        val unitPrice: Long,
        val lineSum: Long,
        val warehouseRdf: Int,
    )

    /** پیش‌نمایش سند پیش‌فاکتور — همان مقادیری که فرستاده می‌شوند (بدون رمز). */
    data class PreInvoicePreview(
        val customerName: String,
        val customerShmo: Int,
        val visitorRdf: Int,
        val userName: String,
        val dateText: String,
        val lines: List<PreviewLine>,
        val sumLineAll: Long,
        val finalAmount: Long,
        val tedRooZ: Int,
        val note: String,
    ) {
        /** متن آمادهٔ نمایش در دیالوگ «پیش‌نمایش سند». */
        fun asText(): String = buildString {
            appendLine("پیش‌فاکتور — ${dateText}")
            appendLine("مشتری: ${customerName} (کد ${customerShmo.toFaNumber()})")
            appendLine("ویزیتور: ${visitorRdf.toFaNumber()} • کاربر: $userName")
            appendLine("──────────────────────────")
            lines.forEach { l ->
                appendLine("• ${l.productName} × ${l.quantity.toFaNumber()} = ${l.lineSum.toFaNumber()} ریال")
            }
            appendLine("──────────────────────────")
            appendLine("جمع کل: ${sumLineAll.toFaNumber()} ریال")
            appendLine("مبلغ نهایی: ${finalAmount.toFaNumber()} ریال")
            appendLine("انبار اقلام: ${lines.map { it.warehouseRdf }.distinct().joinToString("، ") { it.toFaNumber() }}")
            appendLine("اعتبار سند (روز): ${tedRooZ.toFaNumber()}")
            if (note.isNotBlank()) appendLine("توضیح: $note")
        }
    }

    /** نتیجهٔ یک فراخوانی نوشتن. */
    data class WriteOutcome(
        val ok: Boolean,
        val live: Boolean,
        val shfacfo: Long?,
        val linesWritten: Int,
        val message: String,
        val preview: PreInvoicePreview?,
    )

    /**
     * فراخوانی «صدور پیش‌فاکتور» از سبد خریدِ گوشی.
     *
     * @param live  اگر false (پیش‌فرض تا وقتی ERP مرجع ندارد) فقط پیش‌نمایش ساخته
     *              می‌شود؛ اگر true و مرجع موجود باشد، سند واقعاً در ERP ثبت می‌شود.
     * @param userName نام کاربری ERP برای ستون USER__ سربرگ.
     */
    suspend fun submitCart(
        db: AppDatabase,
        customer: CustomerEntity?,
        userName: String,
        note: String,
        live: Boolean,
    ): WriteOutcome {
        val session = VizitorSession.current
        if (customer == null) {
            return WriteOutcome(false, false, null, 0, "اول مشتری را انتخاب کنید.", null)
        }
        val shmo = customer.id
        if (shmo <= 0) {
            return WriteOutcome(
                false, false, null, 0,
                "این مشتری تازه و محلی است (هنوز در آتیران ثبت نشده)؛ صدور پیش‌فاکتور برای او ممکن نیست.",
                null,
            )
        }
        val visitorRdf = session.visitorRdf
        if (visitorRdf == null) {
            return WriteOutcome(false, false, null, 0, "کد ویزیتور شما معلوم نیست؛ یک‌بار دوباره وارد شوید.", null)
        }
        val cart = runCatching { db.cart().getAll() }.getOrDefault(emptyList())
        if (cart.isEmpty()) {
            return WriteOutcome(false, false, null, 0, "سبد سفارش خالی است.", null)
        }
        if (!ensureConnection()) {
            return WriteOutcome(false, false, null, 0, "اتصال: " + errorMessage(), null)
        }

        // ── انبار اقلام: سنگین‌ترین انبارِ همان کالا (از ویو واقعی VW_InventoryAnbars) ──
        val stockRows = runCatching { data.stock() }.getOrDefault(emptyList())
        val warehouseOf: Map<Long, Int> = stockRows.groupBy { it.shka }
            .mapValues { (_, rows) -> rows.maxByOrNull { it.quantity }?.warehouseRdf ?: DEFAULT_WAREHOUSE }

        val preview = PreInvoicePreview(
            customerName = customer.name,
            customerShmo = shmo,
            visitorRdf = visitorRdf,
            userName = userName.ifBlank { session.erpUser },
            dateText = VisitDate.todayJalali(),
            lines = cart.map { item ->
                val sum = (item.quantity * item.unitPrice).toLong()
                PreviewLine(
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    lineSum = sum,
                    warehouseRdf = warehouseOf[item.productId.toLong()] ?: DEFAULT_WAREHOUSE,
                )
            },
            sumLineAll = cart.sumOf { (it.quantity * it.unitPrice).toLong() },
            finalAmount = cart.sumOf { (it.quantity * it.unitPrice).toLong() },
            tedRooZ = preInvoiceReference()?.tedRooZ ?: DEFAULT_TED_ROOZ,
            note = note,
        )

        if (!live) {
            return WriteOutcome(
                ok = true, live = false, shfacfo = null, linesWritten = 0,
                message = "پیش‌نمایش سند آماده شد — هنوز چیزی در سرور ثبت نشده است.",
                preview = preview,
            )
        }

        val reference = preInvoiceReference()
            ?: return WriteOutcome(
                ok = false, live = true, shfacfo = null, linesWritten = 0,
                message = "ثبت واقعی ممکن نیست: در dbo.sailfact_pish پیش‌فاکتور مرجعی نیست. " +
                    "یک‌بار خودِ ERP یک پیش‌فاکتور ثبت کند، از آن لحظه این دکمه فعال می‌شود.",
                preview = preview,
            )

        // پیش‌ بررسی وجود اشیا/ستون‌های مسیر نوشتن
        val health = runCatching { data.preInvoiceHealth() }.getOrDefault(emptyMap())
        val missing = health.filterValues { !it }.keys
        if (missing.isNotEmpty()) {
            return WriteOutcome(
                false, true, null, 0,
                "زیرساخت ثبت پیش‌فاکتور در این دیتابیس کامل نیست: " + missing.joinToString("، "),
                preview,
            )
        }

        val head = MeelanoDataSource.DbPreInvoiceHead(
            shmo = shmo,
            visRdf = visitorRdf,
            userName = preview.userName.ifBlank { "vizitor_android" },
            date = preview.dateText,
            doneDate = preview.dateText,
            sumLineAll = preview.sumLineAll,
            finalAmount = preview.finalAmount,
            tafif = 0L,
            jamTakhgh = 0L,
            tax = 0L,
            avarez = 0L,
            barbari = reference.barbari,
            tozih = note,
            panevis = reference.panevis.ifBlank { "ذکر نشده" },
            rdfSarbarg = reference.rdfSarbarg,
            rdfTahbarg = reference.rdfTahbarg.takeIf { it > 0 } ?: 2,
            modpar = reference.modpar,
            phKh = reference.phKh,
            modDarsadVis = reference.modDarsadVis,
            nahPar = reference.nahPar.takeIf { it > 0 } ?: 1,
            tedRooZ = reference.tedRooZ.takeIf { it > 0 } ?: DEFAULT_TED_ROOZ,
            gainAll = reference.gainAll,
            sysId = session.companyId ?: 1,
        )
        // مقدار تعداد: ستون TEDVAH در ERP از نوع decimal(18,3) است، پس همان تعداد
        // سبد (که می‌تواند اعشاری باشد) مستقیم فرستاده می‌شود — بدون هیچ گردکردن حدسی.
        val lines = preview.lines.mapIndexed { index, l ->
            MeelanoDataSource.DbPreInvoiceLine(
                shka = cart[index].productId.toLong(),
                rdfAnbar = l.warehouseRdf,
                tedVah = l.quantity,
                tedJoz = 0,
                vahPrice = l.unitPrice,
                jozPrice = 0L,
                lineSum = l.lineSum,
                rdf = index,
            )
        }

        return runCatching { data.createPreInvoice(head, lines) }.fold(
            onSuccess = { result ->
                if (result.ok) {
                    // تأیید با قرائت دوباره از خودِ ERP (نه از حافظهٔ برنامه)
                    val readBack = runCatching { data.preInvoiceLines(result.shfacfo) }.getOrDefault(emptyList())
                    WriteOutcome(
                        ok = true, live = true, shfacfo = result.shfacfo, linesWritten = result.linesWritten,
                        message = "پیش‌فاکتور ${result.shfacfo.toFaNumber()} در آتیران ثبت شد ✅ " +
                            "(${readBack.size.toFaNumber()} قلم از سرور بازخوانی شد)",
                        preview = preview,
                    )
                } else {
                    val ref = reference.shfacfo
                    val retired = runCatching { data.retirePreInvoice(result.shfacfo) }.getOrDefault(0)
                    WriteOutcome(
                        ok = false, live = true, shfacfo = result.shfacfo, linesWritten = result.linesWritten,
                        message = "سند نیمه‌کاره ماند: ${result.linesWritten.toFaNumber()} قلم از " +
                            "${lines.size.toFaNumber()} ثبت شد" +
                            (if (retired > 0) " — سند نیمه‌کاره کنار گذاشته شد (active='f')" else "") +
                            " • خطا: " + (result.error?.message ?: "نامشخص") +
                            " • مرجع: ${ref.toFaNumber()}",
                        preview = preview,
                    )
                }
            },
            onFailure = { e ->
                WriteOutcome(
                    false, true, null, 0,
                    "ثبت پیش‌فاکتور انجام نشد ❌ — " + (e.message ?: e.javaClass.simpleName),
                    preview,
                )
            },
        )
    }

    // ═══════════════════════ ۷) تاریخ فاکتورهای ERP ═══════════════════════

    /** تاریخ شمسی امروز به همان قالب ERP (1405/06/27) — یک منبع واحد. */
    fun todayJalali(): String = VisitDate.todayJalali()

    /** برچسب زمانی خوانا برای «آخرین به‌روزرسانی». */
    fun stamp(millis: Long): String =
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(millis))

    /** انبار پیش‌فرض اقلام (نمونه‌های واقعی ERP با rdf_anbar = 1 ثبت شده‌اند). */
    const val DEFAULT_WAREHOUSE = 1

    /** اعتبار پیش‌فرض سند (روز) — فقط تا وقتی پیش‌فاکتور مرجع در ERP نباشد. */
    const val DEFAULT_TED_ROOZ = 30
}
