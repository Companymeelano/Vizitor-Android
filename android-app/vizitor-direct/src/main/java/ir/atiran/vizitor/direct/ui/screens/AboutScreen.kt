// ═══════════════════════════════════════════════════════════════════════════
//  شناسنامهٔ سامانه — همان اطلاعات و امضای نصب‌کننده و پنل مدیریت
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.direct.ui.components.CreditBar
import ir.atiran.vizitor.direct.ui.components.StatusKind
import ir.atiran.vizitor.direct.ui.components.StatusRow
import ir.atiran.vizitor.direct.ui.components.ThinDivider
import ir.atiran.vizitor.direct.ui.components.VizCard
import ir.atiran.vizitor.direct.ui.theme.TextMuted

@Composable
fun AboutScreen(
    driverName: String,
    appVersion: String,
    maskedTarget: String,
    panelUrl: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VizCard("شناسنامهٔ سامانه", badge = "about", icon = "🪪") {
            InfoRow("نام سامانه", "ویزیتور — نسخهٔ اندروید با اتصال مستقیم")
            InfoRow("نسخهٔ برنامه", appVersion)
            InfoRow("حالت اتصال", "مستقیم به SQL Server روی پورت ۱۴۳۳ (بدون IIS و بدون API میانی)")
            InfoRow("درایور فعال", driverName)
            InfoRow("دیتابیس فعال", if (maskedTarget.isBlank()) "—" else maskedTarget)
            if (panelUrl.isNotBlank()) InfoRow("پنل مدیریت سرور", panelUrl)
        }

        VizCard("امنیت و داده", badge = "security", icon = "🔒") {
            StatusRow(StatusKind.Ok, "رمز SQL فقط رمزنگاری‌شده (AES-GCM + Keystore) روی همین گوشی می‌ماند.")
            Spacer(Modifier.height(8.dp))
            StatusRow(StatusKind.Ok, "رمز کاربر دیتابیس هیچ‌گاه نمایش داده یا لاگ نمی‌شود.")
            Spacer(Modifier.height(8.dp))
            StatusRow(StatusKind.Ok, "همهٔ کوئری‌ها پارامتری‌اند؛ هیچ رشتهٔ کاربری داخل SQL چسبانده نمی‌شود.")
            Spacer(Modifier.height(8.dp))
            StatusRow(StatusKind.Warn, "برای اتصال از بیرون شبکه، پورت ۱۴۳۳ را محدود به آی‌پی خودتان کنید.")
        }

        VizCard("مجوزها", badge = "licenses", icon = "📜") {
            Text(
                "فونت وزیرمتن (Vazirmatn) — مجوز SIL OFL 1.1؛ فایل مجوز در assets/licenses قرار دارد.",
                color = TextMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "درایور SQL Server: mssql-jdbc (مجوز MIT) و jTDS (مجوز LGPL) — همراه برنامه توزیع می‌شوند.",
                color = TextMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "اسکنر QR: zxing (مجوز Apache 2.0).",
                color = TextMuted,
                fontSize = 12.sp,
            )
        }

        ThinDivider()
        CreditBar()
        Spacer(Modifier.height(8.dp))
        Text(
            "این برنامه با نصب‌کنندهٔ ویندوز Vizitor-Setup-1.0.0.exe هماهنگ است: کارت اتصال، " +
                "کاربر محدود دیتابیس، پورت ۱۴۳۳ و همان طرح سرمه‌ای/فونت وزیرمتن.",
            color = TextMuted,
            fontSize = 11.5.sp,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = TextMuted, fontSize = 12.5.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}
