/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | اشتراک‌گذاری فاکتور (PDF / Word / تصویر / متن)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  بدون هیچ وابستگی خارجی: PdfDocument و Canvas خود اندروید.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.data.local.InvoiceItemEntity
import ir.atiran.vizitor.util.toFaDate
import ir.atiran.vizitor.util.toFaPrice
import java.io.File
import java.io.FileOutputStream

object InvoiceShare {

    private const val W = 1080
    private const val MARGIN = 90
    private val GOLD = Color.parseColor("#FFD166")
    private val GREEN = Color.parseColor("#2BFF88")
    private val BG = Color.parseColor("#0B0E13")
    private val CARD = Color.parseColor("#12161D")
    private val TEXT = Color.parseColor("#F2F4F8")
    private val MUTED = Color.parseColor("#9AA3B2")

    /** رندر فاکتور به بیت‌مپ لاکچری (تم تیره با طلایی). */
    /** فونت فارسی برنامه برای متن‌های گرافیکی فاکتور (هم‌رنگ و هم‌فونت با کل اپ). */
    private fun faFace(ctx: Context): android.graphics.Typeface? =
        androidx.core.content.res.ResourcesCompat.getFont(ctx, ir.atiran.vizitor.R.font.vazirmatn_bold)

    fun renderBitmap(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        typeface: android.graphics.Typeface? = null
    ): Bitmap {
        val rowH = 92
        val height = 620 + items.size.coerceAtLeast(1) * rowH + 640
        val bmp = Bitmap.createBitmap(W, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(BG)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TEXT
            this.typeface = typeface
        }
        val right = Paint(paint).apply { textAlign = Paint.Align.RIGHT }
        val left = Paint(paint).apply { textAlign = Paint.Align.LEFT }

        var y = 120f
        // سربرگ
        right.color = GOLD; right.textSize = 64f
        c.drawText("آتیران ویزیتور", W - MARGIN.toFloat(), y, right)
        left.color = MUTED; left.textSize = 34f
        c.drawText("سامانه پخش آجیل و خشکبار", MARGIN.toFloat(), y, left)
        y += 70f
        right.color = TEXT; right.textSize = 40f
        c.drawText("فاکتور فروش ${invoice.serverId ?: ("#" + invoice.id)}", W - MARGIN.toFloat(), y, right)
        left.textSize = 34f
        c.drawText(invoice.createdAt.toFaDate(), MARGIN.toFloat(), y, left)
        y += 80f
        right.color = MUTED
        c.drawText("مشتری: ${invoice.customerName}", W - MARGIN.toFloat(), y, right)
        y += 60f
        if (invoice.note.isNotBlank()) {
            right.color = GOLD; right.textSize = 32f
            c.drawText("توضیحات: ${invoice.note}", W - MARGIN.toFloat(), y, right)
            y += 60f
        }

        // جداکننده طلایی
        val gold = Paint().apply { color = GOLD; strokeWidth = 3f }
        c.drawLine(MARGIN.toFloat(), y, W - MARGIN.toFloat(), y, gold)
        y += 70f

        // اقلام
        for ((i, item) in items.withIndex()) {
            if (i % 2 == 0) {
                c.drawRect(
                    MARGIN.toFloat() - 20f, y - 52f, W - MARGIN.toFloat() + 20f, y + 34f,
                    Paint().apply { color = CARD }
                )
            }
            right.color = TEXT; right.textSize = 36f
            c.drawText(item.productName, W - MARGIN.toFloat(), y, right)
            left.color = GREEN; left.textSize = 34f
            c.drawText("${item.quantity} × ${item.unitPrice.toFaPrice()}", MARGIN.toFloat(), y, left)
            y += rowH.toFloat()
        }

        c.drawLine(MARGIN.toFloat(), y - 30f, W - MARGIN.toFloat(), y - 30f, gold)
        y += 50f
        right.color = MUTED; right.textSize = 36f
        c.drawText("جمع اقلام: ${invoice.grossAmount.toFaPrice()}", W - MARGIN.toFloat(), y, right)
        y += 62f
        right.color = GREEN; right.textSize = 48f
        c.drawText("مبلغ نهایی: ${invoice.finalAmount.toFaPrice()}", W - MARGIN.toFloat(), y, right)
        y += 90f

        // فوتر کپی‌رایت
        val center = Paint(paint).apply { textAlign = Paint.Align.CENTER; color = GOLD; textSize = 30f }
        c.drawText("Developed by Milano Technical Team, Milad Yaghoobi", W / 2f, y, center)

        return bmp
    }

    private fun sharedDir(ctx: Context): File =
        File(ctx.cacheDir, "shared").apply { mkdirs() }

    private fun shareFile(ctx: Context, file: File, mime: String, title: String) {
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(intent, "اشتراک فاکتور — $title"))
    }

    /** 📷 اشتراک به‌صورت تصویر PNG. */
    fun shareImage(ctx: Context, invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        val bmp = renderBitmap(invoice, items, faFace(ctx))
        val file = File(sharedDir(ctx), "invoice-${invoice.id}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        shareFile(ctx, file, "image/png", "فاکتور تصویری")
    }

    /** 📄 اشتراک به‌صورت PDF (با PdfDocument خود اندروید). */
    fun sharePdf(ctx: Context, invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        val bmp = renderBitmap(invoice, items, faFace(ctx))
        val doc = PdfDocument()
        val pageW = 595; val pageH = 842
        var pageNo = 1
        var srcTop = 0
        // مقیاس بیت‌مپ به عرض صفحه A4
        val scale = pageW.toFloat() / W
        val pageBmpH = (pageH / scale).toInt()
        while (srcTop < bmp.height) {
            val info = PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create()
            val page = doc.startPage(info)
            val dstH = minOf(pageH, (bmp.height * scale).toInt() - (srcTop * scale).toInt() + 1)
            page.canvas.drawColor(Color.WHITE)
            page.canvas.drawBitmap(
                bmp,
                Rect(0, srcTop, W, minOf(bmp.height, srcTop + pageBmpH)),
                Rect(0, 0, pageW, dstH),
                null
            )
            doc.finishPage(page)
            srcTop += pageBmpH
            pageNo++
        }
        val file = File(sharedDir(ctx), "invoice-${invoice.id}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        shareFile(ctx, file, "application/pdf", "فاکتور PDF")
    }

    /** 📝 اشتراک به‌صورت فایل Word (سند .doc سازگار). */
    fun shareWord(ctx: Context, invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        val html = buildString {
            append("<html dir=\"rtl\"><head><meta charset=\"utf-8\"><title>فاکتور</title></head><body>")
            append("<h1 style=\"color:#B8860B\">آتیران ویزیتور — فاکتور فروش</h1>")
            append("<p>شماره: ${invoice.serverId ?: invoice.id} | تاریخ: ${invoice.createdAt.toFaDate()}</p>")
            append("<p>مشتری: ${invoice.customerName}</p>")
            if (invoice.note.isNotBlank()) append("<p><b>توضیحات:</b> ${invoice.note}</p>")
            append("<table border=\"1\" cellpadding=\"6\" style=\"border-collapse:collapse;width:100%\">")
            append("<tr style=\"background:#f0e6c8\"><th>کالا</th><th>تعداد</th><th>فی</th><th>جمع</th></tr>")
            items.forEach {
                append("<tr><td>${it.productName}</td><td>${it.quantity}</td><td>${it.unitPrice}</td><td>${it.lineTotal}</td></tr>")
            }
            append("</table>")
            append("<p>جمع اقلام: ${invoice.grossAmount} ریال | " +
                    "<b>نهایی: ${invoice.finalAmount} ریال</b></p>")
            append("<p style=\"color:#B8860B\">Developed by Milano Technical Team, Milad Yaghoobi</p>")
            append("</body></html>")
        }
        val file = File(sharedDir(ctx), "invoice-${invoice.id}.doc")
        file.writeText(html, Charsets.UTF_8)
        shareFile(ctx, file, "application/msword", "فاکتور Word")
    }

    /** 📨 اشتراک متن ساده. */
    fun shareText(ctx: Context, invoice: InvoiceEntity, items: List<InvoiceItemEntity>) {
        val text = buildString {
            appendLine("🧾 فاکتور فروش آتیران ویزیتور")
            appendLine("مشتری: ${invoice.customerName}")
            if (invoice.note.isNotBlank()) appendLine("توضیحات: ${invoice.note}")
            appendLine("تاریخ: ${invoice.createdAt.toFaDate()}")
            items.forEach { appendLine("• ${it.productName} × ${it.quantity} = ${it.lineTotal.toFaPrice()}") }
            appendLine("💰 مبلغ نهایی: ${invoice.finalAmount.toFaPrice()}")
            appendLine("Developed by Milano Technical Team, Milad Yaghoobi")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "فاکتور ${invoice.serverId ?: invoice.id}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        ctx.startActivity(Intent.createChooser(intent, "اشتراک فاکتور"))
    }
}
