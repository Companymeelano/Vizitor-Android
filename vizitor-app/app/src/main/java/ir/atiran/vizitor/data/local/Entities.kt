/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | موجودیت‌های دیتابیس محلی (Room Entities)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  ذخیره امن کاتالوگ، مشتریان و فاکتورها برای کارکرد کامل آفلاین
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** کالای کاتالوگ — آینه جدول کالا در دیتابیس آتیران. */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val code: String,              // کد کالا / بارکد
    val name: String,
    val groupName: String,         // گروه کالا
    val price: Long,               // قیمت فروش ۱ (ریال)
    val stock: Double,             // موجودی زنده (Live Stock)
    val imageEmoji: String = "📦",
    val isVip: Boolean = false,
    val unit: String = "کیلو",     // واحد شمارش کالا
    val packSize: Int = 1,         // تعداد/وزن داخل هر بسته
    val price2: Long = 0,          // قیمت فروش ۲ (۰ = مشابه فروش ۱)
    val consumerPrice: Long = 0,   // قیمت مصرف‌کننده (۰ = مشابه فروش ۱)
    val updatedAt: Long = System.currentTimeMillis()
)

/** مشتری — آینه جداول CUSTOMERS و CustGroup. */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: Int,
    val code: String,
    val name: String,
    val groupName: String,         // CustGroup
    val city: String,
    val address: String,
    val phone: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val creditOk: Boolean = true,  // نشانگر وضعیت اعتباری (سبز/قرمز)
    val isVip: Boolean = false,
    val lastPurchaseDaysAgo: Int = 0,
    val purchaseDropPercent: Int = 0, // درصد افت خرید — مبنای لیست پیگیری
    val debt: Long = 0,                // مانده بدهی مشتری (ریال)
    val pendingApproval: Boolean = false // true = مشتری جدید در انتظار تأیید حسابداری آتیران
)

/** هدر فاکتور — آینه جدول SalesHeader. */
@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverId: String? = null,  // شماره فاکتور صادرشده در سرور
    val customerId: Int,
    val customerName: String,
    val grossAmount: Long,         // جمع اقلام
    val discount: Long,            // کسورات
    val finalAmount: Long,         // مبلغ نهایی
    val status: InvoiceStatus = InvoiceStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val signatureBase64: String? = null, // امضای دیجیتال مشتری (PNG)
    val note: String = ""                // توضیحات ویزیتور برای پیش‌فاکتور
)

enum class InvoiceStatus { PENDING, SYNCED, FAILED }

/** اقلام فاکتور — آینه جدول SalesLines. */
@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId"), Index("productId")]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Int,
    val productName: String,
    val quantity: Double,
    val unitPrice: Long,
    val lineTotal: Long
)

/** خلاصه تاریخچه فروش سال مالی (sal_mali) برای دستیار هوش مصنوعی. */
@Entity(tableName = "sal_mali_history")
data class SalMaliHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Int,
    val productName: String,
    val totalQty: Double,
    val yearMonth: String // مثال: 1404-05
)

/** سطر سبد خرید فعلی (قبل از صدور فاکتور). */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val productName: String,
    val unitPrice: Long,
    val quantity: Double,
    val stock: Double
)

/** پیام گفتگوی گروهی ویزیتورها — اتاق محلی گفتگو (Offline-First، آماده سینک با آتیران). */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderName: String,
    val senderUsername: String,   // انگلیسی — بدون @
    val senderPhone: String = "",
    val text: String,             // متن پیام | برای VOICE/VIDEO مدت، برای STICKER ایموجی
    val type: ChatMessageType = ChatMessageType.TEXT,
    val timeLong: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,  // سنجاق‌شده توسط مدیر
    val mine: Boolean = false
)

enum class ChatMessageType { TEXT, VOICE, STICKER, VIDEO }

/**
 * فاکتور/پیش‌فاکتور **واقعی سامانهٔ آتیران** که با اتصال مستقیم از SQL Server
 * خوانده شده است (جدول‌های dbo.sailfact و dbo.sailfact_pish).
 *
 * چرا جدول جدا از InvoiceEntity؟ چون InvoiceEntity کارهای **صادرشده از خود گوشی**
 * است (وضعیت PENDING/SYNCED و اقلام سبد). اگر فاکتورهای سرور در همان جدول ریخته
 * می‌شد، شمارش «فاکتورهای در انتظار ارسال» و جمع فروش امروز اشتباه می‌شد.
 */
@Entity(tableName = "server_invoices")
data class ServerInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,            // shfacfo — شمارهٔ فاکتور/پیش‌فاکتور
    val kind: String,              // «پیش‌فاکتور» یا «فاکتور»
    val dateText: String,          // date — همان‌طور که در ERP ذخیره است
    val customerCode: String,      // shmo
    val customerName: String = "", // از مشتریان همگام‌شده تکمیل می‌شود
    val total: Long = 0,           // all
    val discount: Long = 0,        // tafif
    val fetchedAt: Long = System.currentTimeMillis()
)

/** خروجی غیرموجودیتی: پرفروش‌ترین‌ها (پروجکشن کوئری). */
data class TopProduct(
    val productName: String,
    val total: Double
)
