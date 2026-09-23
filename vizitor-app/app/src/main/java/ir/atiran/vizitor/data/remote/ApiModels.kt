/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | مدل‌های تبادل داده با سرور (DTO)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  قرارداد JSON با سرور PHP (api.php) روی دیتابیس SQL Server آتیران
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.remote

import com.google.gson.annotations.SerializedName

/** پاسخ عمومی سرور. */
data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: T?
)

/** پاسخ تست سلامت سرور (action=ping). */
data class PingDto(
    @SerializedName("db") val db: String? = null,
    @SerializedName("db_host") val dbHost: String? = null,
    @SerializedName("version") val dbVersion: String? = null,
    @SerializedName("php") val php: String? = null,
    @SerializedName("api_version") val apiVersion: String? = null,
    @SerializedName("server_time") val serverTime: Long? = null,
    /** شمارش رکورد جداول کلیدی؛ مقدار null یعنی جدول در دیتابیس یافت نشده است. */
    @SerializedName("tables") val tables: Map<String, Int?>? = null
)

data class ProductDto(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("price") val price: Long,
    @SerializedName("stock") val stock: Double,
    @SerializedName("is_vip") val isVip: Boolean,
    @SerializedName("unit") val unit: String = "کیلو",
    @SerializedName("pack_size") val packSize: Int = 1,
    @SerializedName("price2") val price2: Long = 0,
    @SerializedName("consumer_price") val consumerPrice: Long = 0
)

data class CustomerDto(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("city") val city: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("credit_ok") val creditOk: Boolean,
    @SerializedName("is_vip") val isVip: Boolean,
    @SerializedName("last_purchase_days") val lastPurchaseDays: Int,
    @SerializedName("drop_percent") val dropPercent: Int,
    @SerializedName("debt") val debt: Long = 0
)

data class SalMaliRowDto(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("product_name") val productName: String,
    @SerializedName("total_qty") val totalQty: Double,
    @SerializedName("year_month") val yearMonth: String
)

/** هدر فاکتور ارسالی به سرور (معادل جدول SalesHeader). */
data class InvoiceHeaderRequest(
    @SerializedName("customer_id") val customerId: Int,
    @SerializedName("gross_amount") val grossAmount: Long,
    @SerializedName("discount") val discount: Long,
    @SerializedName("final_amount") val finalAmount: Long,
    @SerializedName("signature") val signatureBase64: String?,
    @SerializedName("note") val note: String = "",
    @SerializedName("items") val items: List<InvoiceLineRequest>
)

/** اقلام فاکتور ارسالی به سرور (معادل جدول SalesLines). */
data class InvoiceLineRequest(
    @SerializedName("product_id") val productId: Int,
    @SerializedName("quantity") val quantity: Double,
    @SerializedName("unit_price") val unitPrice: Long,
    @SerializedName("line_total") val lineTotal: Long
)

data class SubmitInvoiceResponse(
    @SerializedName("invoice_no") val invoiceNo: String,
    @SerializedName("server_time") val serverTime: Long
)

/** ثبت مشتری جدید (در انتظار تأیید حسابداری آتیران) — ارسال از سوی ویزیتور. */
data class NewCustomerRequest(
    @SerializedName("name") val name: String,
    @SerializedName("group_name") val groupName: String,
    @SerializedName("city") val city: String,
    @SerializedName("address") val address: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("visitor_note") val visitorNote: String = ""
)

data class NewCustomerResponse(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("server_time") val serverTime: Long
)
