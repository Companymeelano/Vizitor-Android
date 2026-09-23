// ═══════════════════════════════════════════════════════════════════════════
//  ورود ویزیتور — با همان جدول کاربران سامانهٔ اصلی (dbo.sys_users)
//  از این پس هر ویزیتور فقط نام کاربری و کلمهٔ عبور خودش را می‌زند.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.direct.ui.components.GoldButton
import ir.atiran.vizitor.direct.ui.components.MessageBar
import ir.atiran.vizitor.direct.ui.components.SoftButton
import ir.atiran.vizitor.direct.ui.components.StatusKind
import ir.atiran.vizitor.direct.ui.components.StatusRow
import ir.atiran.vizitor.direct.ui.components.VizCard
import ir.atiran.vizitor.direct.ui.components.VizField
import ir.atiran.vizitor.direct.ui.theme.TextMuted

@Composable
fun LoginScreen(
    maskedTarget: String,
    connection: ConnectionState,
    driverName: String,
    loggingIn: Boolean,
    message: String,
    error: String,
    onLogin: (String, String) -> Unit,
    onBackToSetup: () -> Unit,
) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MessageBar(message, isError = false)
        MessageBar(error, isError = true)

        VizCard("اتصال فعال به دیتابیس", badge = driverName, icon = "🔌") {
            Text(
                maskedTarget,
                color = TextMuted,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(8.dp))
            when (connection) {
                is ConnectionState.Ready -> StatusRow(StatusKind.Ok, "اتصال سالم است (${connection.latencyMs} میلی‌ثانیه).")
                is ConnectionState.Connecting -> StatusRow(StatusKind.Warn, "در حال اتصال…")
                is ConnectionState.Disconnected -> StatusRow(StatusKind.Idle, "اتصال برقرار نیست.")
                is ConnectionState.Error -> StatusRow(StatusKind.Error, connection.message)
            }
        }

        VizCard("ورود ویزیتور", badge = "dbo.sys_users", icon = "🔑") {
            VizField(
                label = "نام کاربری",
                value = user,
                onValueChange = { user = it },
                hint = "همان نام کاربری سامانه",
            )
            Spacer(Modifier.height(10.dp))
            VizField(
                label = "کلمهٔ عبور",
                value = pass,
                onValueChange = { pass = it },
                password = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "ورود با همان کاربر سامانهٔ اصلی انجام می‌شود؛ نقش، دسترسی به مشتریان و کالاها " +
                    "و محدودهٔ ویزیت از همان‌جا می‌آید. رمز شما ذخیره نمی‌شود.",
                color = TextMuted,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            GoldButton(
                text = "ورود",
                onClick = { onLogin(user, pass) },
                loading = loggingIn,
                enabled = user.isNotBlank() && pass.isNotBlank(),
            )
            Spacer(Modifier.height(8.dp))
            SoftButton("بازگشت به تنظیمات اتصال", onBackToSetup)
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "اگر ورود ناموفق بود: حساب در sys_users باید active باشد و (برای ویزیتور) در جدول " +
                "sys_vis به یک ردیف visitors وصل باشد. پیام دقیق خطا در بالای صفحه نشان داده می‌شود.",
            color = TextMuted,
            fontSize = 11.5.sp,
        )
    }
}
