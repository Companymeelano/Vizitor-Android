/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | نقشه داخلی گشت‌زنی (بدون وابستگی خارجی)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  ترسیم سبک و استاتیک: شبکه ظریف، مسیر طلایی بهینه، نشانگرهای مشتریان
 *  (سبز/قرمز اعتباری + حلقه طلایی VIP) و موقعیت ویزیتور. بدون انیمیشن دائمی.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.DarkSlateDeep
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

/**
 * نقشه شماتیک منطقه با مسیر توزیع.
 * @param route ترتیب بازدید (بهینه یا پیش‌فرض)
 */
@Composable
fun MiniRouteMap(
    customers: List<CustomerEntity>,
    route: List<CustomerEntity>,
    myLat: Double,
    myLng: Double,
    modifier: Modifier = Modifier
) {
    // فونت فارسی روی شماره‌های ترتیب بازدید (Canvas بومی)
    val faTypeface = rememberFaTypeface()

    // رنگ‌های تم — خوانده شده در کانتکست کامپوزبل پیش از ورود به Canvas
    val mapBg = DarkSlateDeep
    val gridColor = vizitorPalette.textPrimary.copy(alpha = 0.07f)
    val cGold = Gold
    val cGreen = NeonGreen
    val cDanger = DangerRed
    val cLabelArgb = Gold.toArgb()

    Column {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(mapBg)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                val w = size.width; val h = size.height
                val all = customers + CustomerEntity(
                    0, "", "من", "", "", "", "", myLat, myLng, true, false, 0, 0
                )
                var minLat = all.minOf { it.lat }; var maxLat = all.maxOf { it.lat }
                var minLng = all.minOf { it.lng }; var maxLng = all.maxOf { it.lng }
                if (maxLat - minLat < 1e-6) { minLat -= 0.05; maxLat += 0.05 }
                if (maxLng - minLng < 1e-6) { minLng -= 0.05; maxLng += 0.05 }
                val padX = w * 0.10f; val padY = h * 0.14f
                fun proj(lat: Double, lng: Double): Offset {
                    val x = padX + ((lng - minLng) / (maxLng - minLng) * (w - padX * 2)).toFloat()
                    val y = h - padY - ((lat - minLat) / (maxLat - minLat) * (h - padY * 2)).toFloat()
                    return Offset(x, y)
                }

                // شبکه ظریف شهری
                for (i in 1 until 6) {
                    val gx = w * i / 6f
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
                    val gy = h * i / 6f
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
                }

                // مسیر طلایی بهینه
                val me = proj(myLat, myLng)
                val routePath = Path().apply {
                    moveTo(me.x, me.y)
                    route.forEach { c ->
                        val p = proj(c.lat, c.lng)
                        lineTo(p.x, p.y)
                    }
                }
                // هاله ملایم مسیر
                drawPath(routePath, cGold.copy(alpha = 0.25f), style = Stroke(9f))
                drawPath(
                    routePath, cGold,
                    style = Stroke(3.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f))
                )

                // نشانگر مشتریان
                customers.forEachIndexed { idx, c ->
                    val p = proj(c.lat, c.lng)
                    val credit = if (c.creditOk) cGreen else cDanger
                    if (c.isVip) {
                        drawCircle(cGold.copy(alpha = 0.35f), radius = 16f, center = p)
                    }
                    drawCircle(mapBg, radius = 11f, center = p)
                    drawCircle(credit, radius = 8f, center = p)
                    drawCircle(cGold, radius = 8f, center = p, style = Stroke(2f))
                    // شماره ترتیب بازدید
                    val label = Paint_label(cLabelArgb, faTypeface)
                    drawContext.canvas.nativeCanvas.drawText(
                        (route.indexOf(c) + 1).toString(),
                        p.x + 14f, p.y - 12f, label
                    )
                }

                // موقعیت ویزیتور
                drawCircle(cGreen.copy(alpha = 0.3f), radius = 20f, center = me)
                drawCircle(cGreen, radius = 9f, center = me)
                drawCircle(Color.White, radius = 9f, center = me, style = Stroke(2.5f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            LegendDot(NeonGreen, "اعتبار مجاز / موقعیت من")
            Spacer(Modifier.width(14.dp))
            LegendDot(DangerRed, "مسدود اعتباری")
            Spacer(Modifier.width(14.dp))
            LegendDot(Gold, "مسیر بهینه / VIP")
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        StatusDot(color)
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

private fun Paint_label(
    argb: Int,
    typeface: android.graphics.Typeface? = null
) = android.graphics.Paint().apply {
    color = argb
    this.typeface = typeface
    textSize = 26f
    isAntiAlias = true
    textAlign = android.graphics.Paint.Align.LEFT
}
