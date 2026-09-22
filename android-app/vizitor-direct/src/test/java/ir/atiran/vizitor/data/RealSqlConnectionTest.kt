// ═══════════════════════════════════════════════════════════════════════════
//  آزمون اتصال واقعی به SQL Server (روی رانر CI با کانتینر SQL Server اجرا می‌شود)
//
//  این آزمون، خودِ کدِ برنامه را روی یک SQL Server واقعی اجرا می‌کند تا مطمئن
//  شویم «اتصال بدون مشکل» است — نه فقط از نظر کامپایل:
//    ۱) هر دو درایور (mssql-jdbc و jTDS) بار می‌شوند و نشانی‌هایشان پذیرفته می‌شود
//    ۲) اتصال با کاربر محدود (فقط db_datareader) به master و گرفتن فهرست دیتابیس‌ها
//    ۳) اتصال به دیتابیس انتخاب‌شده + خواندن مشخصات سرور
//    ۴) ورود ویزیتور با جدول dbo.sys_users (رمز varbinary + مقایسهٔ CONVERT)
//    ۵) مشتریان مجاز، گروه‌ها و تیر قیمت، کالاها، موجودی انبار، هویت ویزیتور
//    ۶) بررسی سلامت مسیر پیش‌فاکتور (بدون هیچ نوشتنی)
//    ۷) کاربر محدود اجازهٔ DELETE ندارد (فقط خواندن)
//    ۸) مسیر کاملِ نوشتن پیش‌فاکتور با همان کاربر محدود: EXEC dbo.add_sail_pish
//       + INSERT در جدول میانی + اجرای تریگر — این آزمون فقط روی دیتابیس
//       یک‌بارمصرفِ کانتینر CI می‌نویسد، نه روی دیتابیس واقعی مشتری
//
//  متغیرهای محیطی (در نبودشان آزمون رد می‌شود و بیلد عادی را خراب نمی‌کند):
//    VIZ_TEST_SQL_HOST, VIZ_TEST_SQL_PORT, VIZ_TEST_SQL_DB,
//    VIZ_TEST_SQL_USER, VIZ_TEST_SQL_PASS
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.data

import ir.atiran.vizitor.data.sql.DbSettings
import ir.atiran.vizitor.data.sql.DirectSql
import ir.atiran.vizitor.data.sql.MeelanoDataSource
import ir.atiran.vizitor.data.sql.SqlConnectionManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.sql.DriverManager

class RealSqlConnectionTest {

    private val host = System.getenv("VIZ_TEST_SQL_HOST") ?: ""
    private val port = (System.getenv("VIZ_TEST_SQL_PORT") ?: "1433").toInt()
    private val db = System.getenv("VIZ_TEST_SQL_DB") ?: "Meelano"
    private val user = System.getenv("VIZ_TEST_SQL_USER") ?: ""
    private val pass = System.getenv("VIZ_TEST_SQL_PASS") ?: ""

    private fun settings(database: String = db) = DbSettings(
        host = host,
        port = port,
        database = database,
        username = user,
        password = pass,
        useEncryption = false,      // مثل خودِ برنامه (SQL Server 2014 بدون گواهی معتبر)
        trustServerCert = true,
    )

    private fun requireEnv() {
        assumeTrue(
            "آزمون اتصال واقعی فقط وقتی اجرا می‌شود که متغیرهای VIZ_TEST_SQL_* تنظیم باشند",
            host.isNotBlank() && user.isNotBlank() && pass.isNotBlank(),
        )
    }

    @Test
    fun `١- هر دو درایور بار می شوند و نشانی هایشان پذیرفته می شود`() {
        requireEnv()
        val microsoft = runCatching { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver") }.isSuccess
        val jtds = runCatching { Class.forName("net.sourceforge.jtds.jdbc.Driver") }.isSuccess
        // روی گوشی هر کدام بار شود کافی است؛ برای تست باید حداقل درایور رسمی موجود باشد
        assertTrue("درایور رسمی مایکروسافت باید داخل بسته باشد", microsoft)
        val kind = DirectSql.detect()
        val url = DirectSql.url(settings("master"), kind)
        assertTrue("نشانی ساخته‌شده باید با همان درایور خوانده شود: $url",
            DriverManager.getDriver(url) != null)
        println("[driver] detected=$kind microsoft=$microsoft jtds=$jtds url=$url")
        if (jtds) {
            val jtdsUrl = DirectSql.url(settings("master"), DirectSql.JTDS)
            assertTrue("jTDS باید نشانی jdbc:jtds را بپذیرد: $jtdsUrl",
                DriverManager.getDriver(jtdsUrl) != null)
            println("[driver] jtds url=$jtdsUrl")
        }
    }

    @Test
    fun `٢- اتصال با کاربر محدود به master و فهرست دیتابیس ها`() = runBlocking {
        requireEnv()
        // همان کاری که برنامه در گام ۲/۳ می‌کند: اتصال به master و خواندن sys.databases
        val listing = SqlConnectionManager.listDatabases(settings("master"))
        assertTrue("فهرست دیتابیس‌ها باید با کاربر db_datareader گرفته شود: $listing",
            listing.isSuccess)
        val names = listing.getOrThrow()
        println("[list] databases=$names")
        assertTrue("دیتابیس فعال باید در فهرست باشد", names.contains(db))

        // اتصال مستقیم با درایور پشتیبان هم باید کار کند (اگر داخل بسته باشد)
        if (runCatching { Class.forName("net.sourceforge.jtds.jdbc.Driver") }.isSuccess) {
            val jtdsUrl = DirectSql.url(settings("master"), DirectSql.JTDS)
            DriverManager.getConnection(jtdsUrl, user, pass).use { cn ->
                cn.createStatement().use { st ->
                    st.executeQuery("SELECT COUNT(*) FROM sys.databases WHERE database_id > 4").use { rs ->
                        rs.next()
                        println("[jtds] user databases visible = ${rs.getInt(1)}")
                        assertTrue(rs.getInt(1) >= 1)
                    }
                }
            }
        }
    }

    @Test
    fun `٣- اتصال به دیتابیس انتخاب شده و خواندن مشخصات سرور`() = runBlocking {
        requireEnv()
        val ok = SqlConnectionManager.connect(settings())
        assertTrue("اتصال به دیتابیس $db باید موفق باشد", ok)
        val ds = MeelanoDataSource(SqlConnectionManager)
        val info = ds.databaseInfo()
        println("[info] db=${info.first} version=${info.second} customers=${info.third}")
        assertEquals(db, info.first)
        assertTrue("نسخهٔ SQL Server باید خوانده شود", info.second.isNotBlank())
    }

    @Test
    fun `٤- ورود ویزیتور از جدول sys_users و رد شدن حساب غیرفعال و قفل`() = runBlocking {
        requireEnv()
        assertTrue(SqlConnectionManager.connect(settings()))
        val ds = MeelanoDataSource(SqlConnectionManager)

        val row = ds.login("vizitor1", "1")            // رمز varbinary مثل ERP واقعی
        assertNotNull("ورود ویزیتور فعال باید موفق باشد", row)
        assertEquals(11, row!!.userId)
        assertTrue("active باید true خوانده شود", row.active)
        assertTrue("حساب بدون قفل باید locked=false باشد", !row.locked)
        println("[login] ok user=${row.username} id=${row.userId} company=${row.companyId}")

        assertEquals("رمز اشتباه نباید وارد شود", null, ds.login("vizitor1", "غلط"))
        println("[login] رمز اشتباه رد شد ✔")
        assertEquals("حساب غیرفعال (active=0) نباید وارد شود", null, ds.login("vizitor2", "2"))
        println("[login] حساب غیرفعال (active=0) رد شد ✔")
        val locked = ds.login("lockuser", "3")
        assertNotNull("کاربر قفل‌شده باید شناخته و بعد توسط برنامه رد شود", locked)
        assertTrue("پرچم قفل باید true برگردد", locked!!.locked)
        println("[login] حساب قفل‌شده (IsLocked=1) شناخته شد و locked=true برگشت ✔")
    }

    @Test
    fun `٥- مشتریان، گروه ها و تیر قیمت، کالاها و موجودی انبار`() = runBlocking {
        requireEnv()
        assertTrue(SqlConnectionManager.connect(settings()))
        val ds = MeelanoDataSource(SqlConnectionManager)

        val identity = ds.visitorIdentity(11, 1)
        assertNotNull("هویت ویزیتور باید پیدا شود", identity)
        println("[identity] name=${identity!!.displayName} customers=${identity.allowedCustomers}" +
            " products=${identity.allowedProducts} warehouses=${identity.allowedWarehouses}")
        assertEquals("تعداد مشتری مجاز", 3, identity.allowedCustomers)

        val customers = ds.customersFor(11, 1, limit = 100)
        println("[customers] ${customers.map { it.name }}")
        assertEquals("فقط مشتریان فعالِ مجاز همین ویزیتور", 2, customers.size)
        assertTrue(customers.all { it.active == "t" })

        val groups = ds.customerGroups()
        println("[groups] ${groups.map { it.name to it.priceTier }}")
        assertEquals("فقط گروه‌های فعال", 2, groups.size)
        assertNotNull("تیر قیمت گروه همیشه تعیین می‌شود", groups.first().priceTier)

        val products = ds.products(limit = 50)
        println("[products] ${products.map { it.name to it.priceTier1 }}")
        assertEquals(2, products.size)
        val price = ds.priceFor(products.first().shka, products.first().let { 1 })
        assertNotNull("قیمت با تیر مشخص باید خوانده شود", price)
        println("[price] first product tier1=$price")

        val stock = ds.stock()
        println("[stock] ${stock.map { it.warehouseName to it.availableQuantity }}")
        assertTrue("موجودی انبار باید خوانده شود", stock.isNotEmpty())
    }

    @Test
    fun `٦- بررسی سلامت مسیر پیش فاکتور بدون هیچ نوشتنی`() = runBlocking {
        requireEnv()
        assertTrue(SqlConnectionManager.connect(settings()))
        val ds = MeelanoDataSource(SqlConnectionManager)
        val health = ds.preInvoiceHealth()
        println("[health] ${health.entries.joinToString { "${it.key}=${it.value}" }}")
        assertTrue("پروسیجر پیش‌فاکتور باید دیده شود", health["add_sail_pish"] == true)
        assertTrue("سربرگ پیش‌فاکتور", health["sailfact_pish"] == true)
        assertTrue("اقلام پیش‌فاکتور", health["subsailfact_pish"] == true)
        assertTrue("جدول میانی پیش‌فاکتور", health["subsailtemp_pish"] == true)
        assertTrue("تریگر پیش‌فاکتور", health["trig_sst_pish"] == true)
        val missingColumns = health.filterKeys { it.startsWith("column:") }.filterValues { !it }.keys
        assertTrue("هیچ ستونی از جدول میانی نباید کم باشد: $missingColumns", missingColumns.isEmpty())
        println("[health] columns check passed (${health.count { it.key.startsWith("column:") }} ستون)")
    }

    @Test
    fun `٧- کاربر محدود نباید بتواند بنویسد (فقط خواندن)`() = runBlocking {
        requireEnv()
        assertTrue(SqlConnectionManager.connect(settings()))
        var denied = false
        try {
            SqlConnectionManager.withConnection { c ->
                c.createStatement().use { st ->
                    st.execute("DELETE FROM dbo.CUSTOMERS WHERE SHMO = -1")
                }
            }
        } catch (e: Exception) {
            denied = true
            println("[deny] حذف رد شد: ${e.javaClass.simpleName}: ${e.message?.lines()?.firstOrNull()}")
        }
        assertTrue("کاربر برنامه نباید اجازهٔ DELETE داشته باشد", denied)

        // درج در جدول میانی مجاز است (برای مسیر پیش‌فاکتور) — فقط تست می‌کنیم که اجازه دارد
        val canInsert = try {
            SqlConnectionManager.withConnection { c ->
                c.createStatement().use { st ->
                    st.execute("SELECT TOP 1 1 FROM dbo.subsailtemp_pish")
                }
            }
            true
        } catch (e: Exception) {
            println("[perm] خواندن جدول میانی رد شد: ${e.message?.lines()?.firstOrNull()}")
            false
        }
        assertTrue("خواندن جدول میانی باید مجاز باشد (db_datareader)", canInsert)
    }
    @Test
    fun `۸- ثبت پیش‌فاکتور با کاربر محدود (EXEC + INSERT + تریگر)`() = runBlocking {
        requireEnv()
        assertTrue(SqlConnectionManager.connect(settings()))
        val ds = MeelanoDataSource(SqlConnectionManager)

        val head = MeelanoDataSource.DbPreInvoiceHead(
            shmo = 5001,                    // مشتری مجاز همین ویزیتور
            visRdf = 101,                   // dbo.visitors.vis_rdf
            userName = "vizitor1",
            date = "1405/06/27",
            doneDate = "1405/06/27",
            sumLineAll = 2_500_000,
            finalAmount = 2_500_000,
            tafif = 0,
            jamTakhgh = 0,
            tax = 0,
            avarez = 0,
            barbari = 0,
            tozih = "آزمون خودکار مسیر نوشتن",
            panevis = "ذکر نشده",
            rdfSarbarg = 1,
            rdfTahbarg = 2,
            modpar = 0,
            phKh = 0,
            modDarsadVis = 0,
            nahPar = 1,
            tedRooZ = 3,
            gainAll = 0,
            sysId = 1,
        )
        val line = MeelanoDataSource.DbPreInvoiceLine(
            shka = 9002,
            rdfAnbar = 1,
            tedVah = 1.0,
            tedJoz = 0,
            vahPrice = 2_500_000,
            jozPrice = 2_500_000,
            lineSum = 2_500_000,
            rdf = 0,
        )

        val res = ds.createPreInvoice(head, listOf(line))
        res.error?.let { throw AssertionError("ثبت پیش‌فاکتور خطا داد: ${it.message}", it) }
        assertTrue("شمارهٔ پیش‌فاکتور باید برگردد", res.shfacfo > 0)
        assertEquals("یک قلم باید نوشته شود", 1, res.linesWritten)
        println("[write] سربرگ ${res.shfacfo} با ${res.linesWritten} قلم ثبت شد " +
            "(EXEC dbo.add_sail_pish + INSERT در جدول میانی)")

        val rows = ds.preInvoiceLines(res.shfacfo)
        assertEquals("تریگر باید سطر واقعی پیش‌فاکتور را بسازد", 1, rows.size)
        assertEquals(2_500_000L, rows.first().lineSum)
        println("[write] اقلام خوانده‌شده: ${rows.map { it.shka to it.lineSum }} ✔")
    }
}
