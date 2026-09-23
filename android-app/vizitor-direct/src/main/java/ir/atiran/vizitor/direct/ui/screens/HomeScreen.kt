// ═══════════════════════════════════════════════════════════════════════════
//  خانه — پس از ورود: هویت ویزیتور، دامنهٔ دسترسی و دادهٔ زندهٔ جدول‌های واقعی
//  (مشتریان مجاز، کالاها) — همه فقط خواندن، بدون هیچ دادهٔ ساختگی.
// ═══════════════════════════════════════════════════════════════════════════
package ir.atiran.vizitor.direct.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.data.sql.ConnectionState
import ir.atiran.vizitor.data.sql.DbLoginRow
import ir.atiran.vizitor.data.sql.DbVisitorIdentity
import ir.atiran.vizitor.direct.DataState
import ir.atiran.vizitor.direct.ui.components.KpiTile
import ir.atiran.vizitor.direct.ui.components.MessageBar
import ir.atiran.vizitor.direct.ui.components.SoftButton
import ir.atiran.vizitor.direct.ui.components.StatusKind
import ir.atiran.vizitor.direct.ui.components.StatusRow
import ir.atiran.vizitor.direct.ui.components.ThinDivider
import ir.atiran.vizitor.direct.ui.components.VizCard
import ir.atiran.vizitor.direct.ui.components.VizChip
import ir.atiran.vizitor.direct.ui.components.VizField
import ir.atiran.vizitor.direct.ui.theme.Gold
import ir.atiran.vizitor.direct.ui.theme.Raised
import ir.atiran.vizitor.direct.ui.theme.Line
import ir.atiran.vizitor.direct.ui.theme.TextMain
import ir.atiran.vizitor.direct.ui.theme.TextMuted

@Composable
fun HomeScreen(
    user: DbLoginRow,
    identity: DbVisitorIdentity?,
    connection: ConnectionState,
    data: DataState,
    health: Map<String, Boolean>,
    message: String,
    error: String,
    onLoadCustomers: () -> Unit,
    onLoadProducts: (String?) -> Unit,
    onRefreshHealth: () -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) }          // 0 = مشتریان، 1 = کالاها
    var customerFilter by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MessageBar(message, isError = false)
        MessageBar(error, isError = true)

        // ── کارت هویت ویزیتور ────────────────────────────────────────────────
        VizCard("کارت ویزیتور", badge = "sys_users + sys_vis", icon = "🪪") {
            Text(
                identity?.displayName?.ifBlank { user.fullName } ?: user.fullName,
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "کاربر: ${user.username}  •  شناسهٔ کاربر: ${user.userId}" +
                    (identity?.visitorRdf?.let { "  •  کد ویزیتور: $it" } ?: ""),
                color = TextMuted,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                KpiTile(
                    (identity?.allowedCustomers?.toString() ?: "—"),
                    "مشتری مجاز",
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    (identity?.allowedProducts?.toString() ?: "—"),
                    "کالای مجاز",
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    (identity?.allowedWarehouses?.toString() ?: "—"),
                    "انبار مجاز",
                    modifier = Modifier.weight(1f),
                )
            }
            if (identity == null) {
                Spacer(Modifier.height(10.dp))
                StatusRow(
                    StatusKind.Warn,
                    "این کاربر در جدول sys_vis به یک ردیف visitors وصل نیست؛ دامنهٔ دسترسی ویزیتوری ندارد.",
                )
            }
            Spacer(Modifier.height(10.dp))
            when (connection) {
                is ConnectionState.Ready -> StatusRow(StatusKind.Ok, "اتصال سالم (${connection.latencyMs} میلی‌ثانیه).")
                is ConnectionState.Connecting -> StatusRow(StatusKind.Warn, "در حال اتصال…")
                is ConnectionState.Disconnected -> StatusRow(StatusKind.Idle, "اتصال برقرار نیست.")
                is ConnectionState.Error -> StatusRow(StatusKind.Error, connection.message)
            }
        }

        // ── آمادگی مسیر پیش‌فاکتور (بدون نوشتن چیزی) ─────────────────────────
        VizCard("آمادگی ثبت پیش‌فاکتور", badge = "add_sail_pish", icon = "🧾") {
            if (health.isEmpty()) {
                StatusRow(StatusKind.Idle, "هنوز بررسی نشده است.")
            } else {
                val missing = health.filterValues { !it }.keys.toList()
                if (missing.isEmpty()) {
                    StatusRow(StatusKind.Ok, "همهٔ اشیای لازم روی این دیتابیس موجود است (${health.size} مورد).")
                } else {
                    StatusRow(StatusKind.Warn, "این موارد روی دیتابیس نیست: ${missing.joinToString("، ")}")
                }
            }
            Spacer(Modifier.height(10.dp))
            SoftButton("بررسی مجدد", onRefreshHealth)
        }

        // ── دادهٔ زنده ───────────────────────────────────────────────────────
        VizCard("دادهٔ زندهٔ سامانه", badge = "CUSTOMERS / کالا", icon = "📊") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VizChip("مشتریان مجاز من", selected = tab == 0) { tab = 0 }
                VizChip("کالاها و قیمت‌ها", selected = tab == 1) { tab = 1 }
            }
            Spacer(Modifier.height(12.dp))
            if (tab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SoftButton("خواندن مشتریان", onLoadCustomers, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                VizField(
                    label = "جست‌وجو در فهرست خوانده‌شده",
                    value = customerFilter,
                    onValueChange = { customerFilter = it },
                    hint = "بخشی از نام مشتری",
                )
                Spacer(Modifier.height(10.dp))
                val filtered = data.customers.filter {
                    customerFilter.isBlank() || it.name.contains(customerFilter, ignoreCase = true)
                }
                Text(
                    "نمایش ${filtered.size} از ${data.customers.size} مشتری مجاز",
                    color = TextMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                filtered.take(60).forEach { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Raised, RoundedCornerShape(12.dp))
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = TextMain, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "کد ${c.code}  •  ${c.groupName ?: "بدون گروه"}  •  ${c.phone}",
                                color = TextMuted,
                                fontSize = 11.5.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("بدهی", color = TextMuted, fontSize = 10.5.sp)
                            Text(c.debt.toString(), color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SoftButton("خواندن کالاها", { onLoadProducts(null) }, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                VizField(
                    label = "جست‌وجوی کالا در سرور",
                    value = productSearch,
                    onValueChange = { productSearch = it },
                    hint = "بخشی از نام یا کد کالا",
                )
                Spacer(Modifier.height(8.dp))
                SoftButton("جست‌وجو", { onLoadProducts(productSearch) })
                Spacer(Modifier.height(10.dp))
                Text("${data.products.size} کالا", color = TextMuted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                data.products.take(60).forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Raised, RoundedCornerShape(12.dp))
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = TextMain, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "کد ${p.code}  •  ${p.unit}  •  موجودی ${p.stockVah}",
                                color = TextMuted,
                                fontSize = 11.5.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("قیمت‌ها (۵ تیر)", color = TextMuted, fontSize = 10.5.sp)
                            Text(
                                "${p.priceTier1} / ${p.priceTier2} / ${p.priceTier3} / ${p.priceTier4} / ${p.priceTier5}",
                                color = Gold,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (data.loading) {
                Spacer(Modifier.height(8.dp))
                StatusRow(StatusKind.Warn, "در حال خواندن از سرور…")
            }
        }

        ThinDivider()
        SoftButton("خروج از حساب", onLogout)
        Spacer(Modifier.height(6.dp))
    }
}
