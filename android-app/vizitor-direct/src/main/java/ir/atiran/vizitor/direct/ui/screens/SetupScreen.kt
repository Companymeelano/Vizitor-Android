// ═══════════════════════════════════════════════════════════════════════════
//  صفحهٔ راه‌اندازی اتصال — همان ترتیب گام‌های نصب‌کنندهٔ ویندوز:
//    ۱) سرور و پورت (۱۴۳۳)   ۲) کاربر دیتابیس   ۳) فهرست دیتابیس‌ها و انتخاب
//    ۴) اعمال تنظیمات و تست اتصال   ۵) خواندن مشخصات و بررسی سلامت
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.direct.DbListState
import ir.atiran.vizitor.direct.SetupForm
import ir.atiran.vizitor.direct.ui.components.GoldButton
import ir.atiran.vizitor.direct.ui.components.KpiTile
import ir.atiran.vizitor.direct.ui.components.MessageBar
import ir.atiran.vizitor.direct.ui.components.SoftButton
import ir.atiran.vizitor.direct.ui.components.StatusKind
import ir.atiran.vizitor.direct.ui.components.StatusRow
import ir.atiran.vizitor.direct.ui.components.VizCard
import ir.atiran.vizitor.direct.ui.components.VizChip
import ir.atiran.vizitor.direct.ui.components.VizField
import ir.atiran.vizitor.direct.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    form: SetupForm,
    connection: ConnectionState,
    dbList: DbListState,
    dbInfo: Triple<String, String, String>?,
    health: Map<String, Boolean>,
    driverName: String,
    message: String,
    error: String,
    onForm: ((SetupForm) -> SetupForm) -> Unit,
    onFetchDatabases: () -> Unit,
    onConnect: (String) -> Unit,
    onRefreshHealth: () -> Unit,
    onScanQr: () -> Unit,
    onPickJson: () -> Unit,
    onApplyCardText: (String) -> Unit,
    onForget: () -> Unit,
) {
    var showPaste by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MessageBar(message, isError = false)
        MessageBar(error, isError = true)

        // ── ۱) سرور و پورت ───────────────────────────────────────────────────
        VizCard("گام ۱ — سرور و پورت", badge = "port 1433", icon = "🖧") {
            VizField(
                label = "آی‌پی داخلی شبکه (مثلاً 192.168.1.150)",
                value = form.hostLan,
                onValueChange = { v -> onForm { it.copy(hostLan = v) } },
                hint = "داخل فروشگاه",
            )
            Spacer(Modifier.height(10.dp))
            VizField(
                label = "آی‌پی اختصاصی / اینترنتی (اختیاری)",
                value = form.hostPublic,
                onValueChange = { v -> onForm { it.copy(hostPublic = v) } },
                hint = "برای اتصال از بیرون شبکه",
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                VizChip("اتصال از شبکهٔ داخلی", selected = !form.usePublic) {
                    onForm { it.copy(usePublic = false) }
                }
                VizChip("اتصال از بیرون (آی‌پی اختصاصی)", selected = form.usePublic) {
                    onForm { it.copy(usePublic = true) }
                }
            }
            Spacer(Modifier.height(10.dp))
            VizField(
                label = "پورت",
                value = form.port,
                onValueChange = { v -> onForm { it.copy(port = v.filter { ch -> ch.isDigit() }.take(5)) } },
                numeric = true,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "نشانی فعال: ${form.activeHost.ifBlank { "—" }}:${form.port}  (درایور: $driverName)",
                color = TextMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("اسکن کارت اتصال (QR)", onScanQr, modifier = Modifier.weight(1f))
                SoftButton("چسباندن متن کارت", { showPaste = true }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            SoftButton("خواندن فایل android-connect.json", onPickJson)
        }

        // ── ۲) کاربر دیتابیس ─────────────────────────────────────────────────
        VizCard("گام ۲ — کاربر دیتابیس", badge = "sql login", icon = "🔐") {
            VizField(
                label = "نام کاربری",
                value = form.user,
                onValueChange = { v -> onForm { it.copy(user = v) } },
                hint = "مدیر یک‌بار وارد می‌کند",
            )
            Spacer(Modifier.height(10.dp))
            VizField(
                label = "کلمهٔ عبور",
                value = form.password,
                onValueChange = { v -> onForm { it.copy(password = v) } },
                password = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "رمز فقط در همین گوشی و به‌صورت رمزنگاری‌شده (AES-GCM + Keystore) نگه داشته می‌شود؛ " +
                    "نه در سرور ذخیره می‌شود و نه در هیچ پیامی چاپ می‌شود.",
                color = TextMuted,
                fontSize = 11.5.sp,
            )
            Spacer(Modifier.height(12.dp))
            GoldButton(
                text = "تأیید و دریافت فهرست دیتابیس‌ها",
                onClick = onFetchDatabases,
                loading = dbList is DbListState.Loading,
                enabled = form.activeHost.isNotBlank() && form.user.isNotBlank() && form.password.isNotBlank(),
            )
        }

        // ── ۳) انتخاب دیتابیس ────────────────────────────────────────────────
        VizCard("گام ۳ — انتخاب دیتابیس حسابداری", badge = "sys.databases", icon = "🗃") {
            when (dbList) {
                is DbListState.Idle -> StatusRow(StatusKind.Idle, "هنوز فهرستی گرفته نشده است.")
                is DbListState.Loading -> StatusRow(StatusKind.Warn, "در حال خواندن فهرست از سرور…")
                is DbListState.Failed -> StatusRow(StatusKind.Error, dbList.message)
                is DbListState.Ready -> {
                    if (dbList.databases.isEmpty()) {
                        StatusRow(StatusKind.Warn, "هیچ دیتابیس کاربری‌ای روی این سرور پیدا نشد.")
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            dbList.databases.forEach { name ->
                                VizChip(name, selected = form.database == name) {
                                    onForm { it.copy(database = name) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            VizField(
                label = "نام دیتابیس (می‌توانید دستی تایپ کنید)",
                value = form.database,
                onValueChange = { v -> onForm { it.copy(database = v.trim()) } },
                hint = "هیچ دیتابیسی اجباری نیست",
            )
            Spacer(Modifier.height(12.dp))
            GoldButton(
                text = "اعمال تنظیمات و تست اتصال",
                onClick = { onConnect(form.database) },
                enabled = form.database.isNotBlank(),
                loading = connection is ConnectionState.Connecting,
            )
            Spacer(Modifier.height(10.dp))
            when (connection) {
                is ConnectionState.Ready -> StatusRow(
                    StatusKind.Ok,
                    "اتصال سالم است (${connection.latencyMs} میلی‌ثانیه).",
                )
                is ConnectionState.Connecting -> StatusRow(StatusKind.Warn, "در حال اتصال…")
                is ConnectionState.Disconnected -> StatusRow(StatusKind.Idle, "اتصال برقرار نیست.")
                is ConnectionState.Error -> StatusRow(StatusKind.Error, connection.message)
            }
        }

        // ── ۴) مشخصات و سلامت ───────────────────────────────────────────────
        VizCard("گام ۴ — خواندن تنظیمات و بررسی سلامت", badge = "health", icon = "🩺") {
            val info = dbInfo
            if (info == null) {
                StatusRow(StatusKind.Idle, "پس از اتصال، مشخصات سرور اینجا خوانده می‌شود.")
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiTile(info.first.ifBlank { "—" }, "دیتابیس فعال", modifier = Modifier.weight(1f))
                    KpiTile(info.third.ifBlank { "—" }, "ردیف CUSTOMERS", modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "نسخهٔ SQL Server: ${info.second.ifBlank { "—" }}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Spacer(Modifier.height(10.dp))
            if (health.isEmpty()) {
                StatusRow(StatusKind.Idle, "آمادگی مسیر پیش‌فاکتور هنوز بررسی نشده است.")
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    health.entries.take(12).forEach { (key, ok) ->
                        VizChip(if (ok) "✓ $key" else "✕ $key", selected = ok) { }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftButton("بررسی مجدد سلامت", onRefreshHealth, modifier = Modifier.weight(1f))
                SoftButton("پاک کردن تنظیمات گوشی", onForget, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "همهٔ کوئری‌ها فقط‌خواندنی‌اند (sys.databases و جدول‌های ERP). هیچ تغییری در دیتابیس سرور " +
                "انجام نمی‌شود و رمز کاربر دیتابیس هرگز نمایش داده نمی‌شود.",
            color = TextMuted,
            fontSize = 11.5.sp,
        )
        Spacer(Modifier.height(10.dp))
    }

    if (showPaste) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaste = false },
            title = { Text("چسباندن متن کارت اتصال") },
            text = {
                Column {
                    Text(
                        "متن کارت را از فایل android-connect.txt (یا کد QR) اینجا بچسبانید. " +
                            "خط «متن QR» را هم اگر بچسبانید خوانده می‌شود.",
                        color = TextMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    VizField(
                        label = "متن کارت",
                        value = text,
                        onValueChange = { text = it },
                        hint = "vizitor://c?h=…&p=1433&d=…",
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onApplyCardText(text)
                    showPaste = false
                }) { Text("خواندن") }
            },
            dismissButton = {
                TextButton(onClick = { showPaste = false }) { Text("انصراف") }
            },
        )
    }
}
