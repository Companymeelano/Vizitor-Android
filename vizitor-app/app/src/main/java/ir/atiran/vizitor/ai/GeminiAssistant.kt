/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | دستیار هوشمند فروش (AI Sales Assistant)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  مبتنی بر Google Gemini API — کلید API هرگز در اپ ذخیره نمی‌شود؛
 *  تمامی درخواست‌ها از Reverse Proxy روی Cloudflare Workers عبور می‌کنند.
 *  ورودی تحلیل: تاریخچه جداول sal_mali + سبد فعلی → پیشنهاد کالای مکمل
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import ir.atiran.vizitor.data.local.CartItemEntity
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.data.local.SalMaliHistoryEntity
import ir.atiran.vizitor.data.repository.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiAssistant(private val config: ServerConfig) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    /**
     * تحلیل تاریخچه خرید و پیشنهاد کالای مکمل برای بیش‌فروشی.
     * @return پاسخ متنی مدل یا پیام خطای کوتاه.
     */
    suspend fun suggestComplementary(
        customerName: String,
        salMali: List<SalMaliHistoryEntity>,
        cart: List<CartItemEntity>,
        catalog: List<ProductEntity>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(customerName, salMali, cart, catalog)
            val url = config.workerUrl.trimEnd('/') +
                    "/v1beta/models/${config.geminiModel}:generateContent"

            val body = JsonObject().apply {
                add("contents", gson.toJsonTree(listOf(mapOf("parts" to listOf(mapOf("text" to prompt))))))
                add(
                    "generationConfig",
                    gson.toJsonTree(
                        mapOf("temperature" to 0.4, "maxOutputTokens" to 512)
                    )
                )
            }.toString()

            val request = Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                // کلید Gemini توسط خودِ ورکر تزریق می‌شود؛ اپ هیچ کلیدی نمی‌فرستد.
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(RuntimeException("پاسخ پراکسی: ${response.code}"))
                }
                val json = gson.fromJson(response.body!!.string(), JsonObject::class.java)
                val text = json.getAsJsonArray("candidates")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")
                    ?.get(0)?.asJsonObject
                    ?.get("text")?.asString
                if (text.isNullOrBlank()) Result.failure(RuntimeException("پاسخ خالی مدل"))
                else Result.success(text.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        customerName: String,
        salMali: List<SalMaliHistoryEntity>,
        cart: List<CartItemEntity>,
        catalog: List<ProductEntity>
    ): String = buildString {
        appendLine("تو دستیار هوشمند فروش اپ «آتیران ویزیتور» هستی. فارسی، کوتاه و عملی پاسخ بده.")
        appendLine("وظیفه: تحلیل تاریخچه سال مالی مشتری و سبد فعلی و پیشنهاد کالای مکمل برای بیش‌فروشی.")
        appendLine()
        appendLine("مشتری: $customerName")
        appendLine("تاریخچه خرید (جدول sal_mali):")
        salMali.forEach { appendLine("- ${it.yearMonth}: ${it.productName} مقدار ${it.totalQty}") }
        appendLine("اقلام فعلی سبد:")
        if (cart.isEmpty()) appendLine("- (خالی)")
        else cart.forEach { appendLine("- ${it.productName} × ${it.quantity}") }
        appendLine("کاتالوگ موجود:")
        catalog.forEach { appendLine("- ${it.name} (گروه: ${it.groupName}, قیمت: ${it.price} ریال, موجودی: ${it.stock})") }
        appendLine()
        appendLine("خروجی: حداکثر ۳ پیشنهاد. هر پیشنهاد شامل: نام کالا، دلیل مکمل بودن، و جمله کوتاه برای متقاعدسازی مشتری.")
    }
}
