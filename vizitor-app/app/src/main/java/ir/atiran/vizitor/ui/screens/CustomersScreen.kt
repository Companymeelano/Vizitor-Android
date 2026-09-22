/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۴: مشتری (CRM & Routing)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کارت‌های مشتری ارتقایافته (آواتار تو‌حلقه، پنل اطلاعات، اکشن‌های کوچک)،
 *  نشانگر وضعیت اعتباری (سبز/قرمز)، تماس، مسیریابی شهری از طریق API نقشه‌ها
 *  و بهینه‌سازی مسیر توزیع بر اساس فاصله. نقشه ابتدای صفحه حذف شد.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import kotlin.math.abs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import ir.atiran.vizitor.util.toFaPrice
import ir.atiran.vizitor.util.toFaDate
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.data.local.SeedData
import ir.atiran.vizitor.data.local.InvoiceEntity
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import ir.atiran.vizitor.ui.components.rememberVoiceSearch
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.MicButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import ir.atiran.vizitor.ui.components.NeonPurpleButton
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.RoyalSurfaceBrush
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.components.tilt3D
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.atiran.vizitor.VizitorViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ir.atiran.vizitor.R
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.StatusChip
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.theme.AccentText
import ir.atiran.vizitor.ui.components.GlamourButton
import ir.atiran.vizitor.ui.components.GlamourTone
import ir.atiran.vizitor.ui.components.MiniStat
import ir.atiran.vizitor.ui.components.TableHeader
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CustomersScreen(viewModel: VizitorViewModel) {
    val customers by viewModel.customers.collectAsState()
    val context = LocalContext.current
    var optimized by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showAddCustomer by remember { mutableStateOf(false) }
    var statementCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    val invoices by viewModel.invoices.collectAsState()
    val sync by viewModel.customerSync.collectAsState()
    val startVoice = rememberVoiceSearch(
        onResult = { query = it; viewModel.showToast("جستجوی صوتی مشتری: «$it»") },
        onUnavailable = { viewModel.showToast("ورودی صوتی روی این دستگاه در دسترس نیست 🎙️") }
    )

    // موقعیت فرضی ویزیتور (در نسخه عملیاتی از FusedLocation استفاده می‌شود)
    val myLat = 35.7219; val myLng = 51.3815

    val list = remember(customers, optimized, query) {
        val base = if (query.isBlank()) customers
        else customers.filter {
            it.name.contains(query) || it.code.contains(query) || it.city.contains(query)
        }
        if (optimized) base.sortedBy { distanceKm(myLat, myLng, it.lat, it.lng) }
        else base
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                ShimmerGoldText("مشتری")
                Text(
                    "مدیریت مشتریان، وضعیت اعتبار و مسیریابی هوشمند فروش",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                // ── کاشی‌های آماری مشتریان (کل / دارای بدهی / جمع بدهی) ──
                val debtors = customers.count { it.debt > 0 }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MiniStat(
                        label = "کل مشتریان",
                        value = customers.size.toFaNumber(),
                        tint = vizitorPalette.accent,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        label = "دارای بدهی",
                        value = debtors.toFaNumber(),
                        tint = DangerRed,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStat(
                        label = "جمع بدهی (ریال)",
                        value = customers.sumOf { it.debt }.toFaPrice(),
                        tint = vizitorPalette.gold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { CustomerSearchField(query) { query = it } }
                    Spacer(Modifier.width(8.dp))
                    MicButton(onClick = { startVoice() })
                }
                Spacer(Modifier.height(10.dp))
                // ── همگام‌سازی مشتریان از سرور (سه مسیر پله‌ای در CustomerSync) ──
                GlamourButton(
                    label = if (sync.busy) "در حال گرفتن فهرست مشتریان…" else "همگام‌سازی مشتریان از سرور",
                    subtitle = "اگر فهرست خالی یا قدیمی است، این دکمه را بزنید",
                    icon = Icons.Filled.Sync,
                    tone = GlamourTone.PURPLE,
                    height = 54.dp,
                    loading = sync.busy,
                    enabled = !sync.busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.syncCustomersNow() }
                )
                if (sync.message.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        sync.message,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sync.ok) NeonGreen else DangerRed
                    )
                }
                Spacer(Modifier.height(10.dp))
                TableHeader(
                    title = "فهرست مشتریان این ویزیتور",
                    count = list.size.toFaNumber() + " مشتری",
                    icon = Icons.Filled.People
                )
                Spacer(Modifier.height(10.dp))
                // دکمه‌های قهرمان — عنوان و زیرعنوان دقیقاً داخل قاب، بردر روی مرز
                val p0 = vizitorPalette
                HeroActionButton(
                    title = if (optimized) "مسیر بهینه شد ✅" else "بهینه‌سازی مسیر ویزیت",
                    subtitle = if (optimized) "برای بازگشت به ترتیب پیش‌فرض بزنید" else "بر اساس نزدیکی جغرافیایی مشتریان منطقه",
                    icon = Icons.Filled.Route,
                    faceTop = p0.btnAccentTop, faceBottom = p0.btnAccentBottom,
                    contentColor = p0.onAccent,
                    onClick = { optimized = !optimized },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                // ✨ درگاه ثبت مشتری جدید — ارسال برای تأیید به حسابداری آتیران
                HeroActionButton(
                    title = "افزودن مشتری جدید",
                    subtitle = "ارسال برای تأیید حسابداری آتیران",
                    icon = Icons.Filled.PersonAdd,
                    faceTop = p0.btnPrimaryTop, faceBottom = p0.btnPrimaryBottom,
                    contentColor = p0.onPrimary,
                    onClick = { showAddCustomer = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            RoyalHeader(text = "مشتریان منطقه", icon = Icons.Filled.People)
        }

        items(list, key = { it.id }) { customer ->
            CustomerCard(
                customer = customer,
                order = if (optimized) list.indexOf(customer) + 1 else null,
                onCall = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                    )
                },
                onNavigate = {
                    // اتصال به API نقشه‌ها: گوگل‌مپ/نشان/بلد با Intent استاندارد
                    val gmm = Uri.parse(
                        "google.navigation:q=${customer.lat},${customer.lng}&mode=d"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, gmm).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    } else {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("geo:${customer.lat},${customer.lng}?q=${customer.lat},${customer.lng}(${customer.name})")
                            )
                        )
                    }
                },
                onPick = {
                    viewModel.selectCustomer(customer)
                    viewModel.showToast(
                        "مشتری «${customer.name}» برای فاکتور انتخاب شد؛ از دکمه مرکزی سبد استفاده کنید 🛒"
                    )
                },
                onStatement = { statementCustomer = customer },
                onSms = { openSmsApp(context, customer.phone, "") },
                onSmsBalance = {
                    openSmsApp(context, customer.phone, balanceSmsMessage(customer))
                    viewModel.showToast("متن مانده حساب برای ارسال پیامکی آماده شد 📨")
                }
            )
        }

        item { MilanoFooter() }
    }

    statementCustomer?.let { c ->
        StatementDialog(c, invoices) { statementCustomer = null }
    }

    if (showAddCustomer) {
        NewCustomerDialog(
            onDismiss = { showAddCustomer = false },
            onSubmit = { name, group, city, address, phone ->
                viewModel.addPendingCustomer(name, group, city, address, phone)
            }
        )
    }
}

/**
 * ✨ فرم ثبت مشتری جدید — مخصوص مشتریانی که هنوز در سیستم آتیران تعریف نشده‌اند.
 * پس از ثبت، اطلاعات برای «تأیید» به حسابداری آتیران ارسال می‌شود؛ کاربر
 * حسابداری در بخش «مشتریان در انتظار» آن را تأیید می‌کند و آنگاه مشتری
 * برای ویزیتور قابل فاکتورکردن خواهد بود.
 */
@Composable
private fun NewCustomerDialog(
    onDismiss: () -> Unit,
    onSubmit: (name: String, group: String, city: String, address: String, phone: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && phone.trim().length >= 7

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(RoyalSurfaceBrush)
                .royalBorder(RoundedCornerShape(26.dp))
        ) {
            Column(
                Modifier
                    .padding(18.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                RoyalHeader(text = "افزودن مشتری جدید", icon = Icons.Filled.PersonAdd)
                Spacer(Modifier.height(10.dp))
                // توضیح جریان تأیید
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Gold.copy(alpha = 0.10f))
                        .border(1.dp, Gold.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.HourglassEmpty,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "پس از ثبت، مشتری برای «تأیید» به حسابداری آتیران ارسال می‌شود و پس از تأیید، قابل فاکتور خواهد بود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentText
                    )
                }
                Spacer(Modifier.height(12.dp))

                NewCustomerField(name, { name = it }, "نام و نام‌خانوادگی / عنوان فروشگاه *", Icons.Filled.Home)
                Spacer(Modifier.height(8.dp))
                NewCustomerField(phone, { phone = it }, "شماره تماس مشتری *", Icons.Filled.Call, KeyboardType.Phone)
                Spacer(Modifier.height(8.dp))
                NewCustomerField(city, { city = it }, "شهر", Icons.Filled.LocationOn)
                Spacer(Modifier.height(8.dp))
                NewCustomerField(group, { group = it }, "گروه مشتری (مثلاً خرده‌فروشی / عمده)", Icons.Filled.People)
                Spacer(Modifier.height(8.dp))
                NewCustomerField(address, { address = it }, "آدرس فروشگاه / محله", Icons.Filled.Route, singleLine = false)
                Spacer(Modifier.height(14.dp))

                NeonGreenButton(
                    text = "ثبت و ارسال برای تأیید حسابداری 📨",
                    enabled = valid,
                    onClick = {
                        onSubmit(name, group, city, address, phone)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        }
    }
}

/** فیلد ورودی یکدست فرم مشتری جدید. */
@Composable
private fun NewCustomerField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(18.dp)) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonPurple,
            unfocusedBorderColor = Color(0x33FFFFFF),
            focusedLabelColor = NeonPurple,
            cursorColor = NeonPurple
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

private data class Txn(val date: Long, val title: String, val amount: Long, val credit: Boolean)

/**
 * دیالوگ گردش حساب مشتری — فاکتورها (بدهکار) + واریزها (بستانکار) + مانده.
 */
@Composable
private fun StatementDialog(
    customer: CustomerEntity,
    invoices: List<InvoiceEntity>,
    onDismiss: () -> Unit
) {
    val txns = remember(customer, invoices) {
        val debits = invoices
            .filter { it.customerId == customer.id }
            .map { Txn(it.createdAt, "فاکتور ${it.serverId ?: ("#" + it.id)}", it.finalAmount, false) }
        val credits = SeedData.payments
            .filter { it.customerId == customer.id }
            .map { Txn(System.currentTimeMillis() - it.daysAgo * 86_400_000L, "واریز / پرداخت", it.amount, true) }
        (debits + credits).sortedByDescending { it.date }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("گردش حساب: ${customer.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                Text(
                    "مانده فعلی: ${customer.debt.toFaPrice()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (customer.debt > 0) DangerRed else NeonGreen
                )
                Spacer(Modifier.height(10.dp))
                if (txns.isEmpty()) {
                    Text("گردشی ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                txns.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                t.date.toFaDate(),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            (if (t.credit) "+" else "-") + t.amount.toFaPrice(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (t.credit) NeonGreen else DangerRed
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

/** فاصله هاورساین (کیلومتر) برای بهینه‌سازی مسیر توزیع. */
private fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}


/** آواتار ثابت خلاقانهٔ هر مشتری — ۵ چهرهٔ آجیل سه‌بعدی، تخصیص پایدار بر اساس نام. */
private fun avatarResFor(name: String): Int = when (abs(name.hashCode()) % 5) {
    0 -> R.drawable.avatar_pistachio
    1 -> R.drawable.avatar_almond
    2 -> R.drawable.avatar_cashew
    3 -> R.drawable.avatar_walnut
    else -> R.drawable.avatar_fig
}

/**
 * کارت مشتری نسل جدید: آواتار تو‌حلقه با نشان اعتبار و ترتیب ویزیت،
 * پنل اطلاعات یکدست (نشانی / تماس / آخرین خرید / مانده حساب)،
 * اکشن اصلی برجسته و اکشن‌های کوچک سه‌تایی (پیامک، مانده، گردش حساب).
 */
@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    order: Int?,
    onCall: () -> Unit,
    onNavigate: () -> Unit,
    onPick: () -> Unit,
    onStatement: () -> Unit,
    onSms: () -> Unit,
    onSmsBalance: () -> Unit
) {
    val statusColor = if (customer.creditOk) NeonGreen else DangerRed
    val p = vizitorPalette
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            // پارالاکس لمسی — کارت هنگام لمس به‌ظاهر می‌چرخد (گیت با VizitorPerf)
            .tilt3D(maxTilt = 6f, enabled = VizitorPerf.listFx)
            // تک‌قاب دقیق: VIP فقط قاب طلایی، عادی فقط قاب سلطنتی — بدون تداخل/بیرون‌زدگی
            .then(if (customer.isVip) Modifier.goldBorder() else Modifier.royalBorder())
    ) {
        Column {
            // ═══ بنر لوکس «پخش عمده آجیل و خشکبار» — تنتِ زنده از پالت تم ═══
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.nuts_premium),
                    contentDescription = "پخش عمده آجیل و خشکبار",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(78.dp)
                )
                // تنتِ ملایم رنگ تم فعال — بنر خلوت جدید (ابریشم تیره) با تنت سبک‌تر دیده می‌شود
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(78.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    p.primary.copy(alpha = 0.34f),
                                    Color(0x29000000),
                                    p.gold.copy(alpha = 0.22f)
                                )
                            )
                        )
                )
                // خط هیرلاین طلایی نازک در لبه پایین بنر (امضای لوکس)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.2.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, p.gold.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                )
                Text(
                    "پخش عمده آجیل و خشکبار ✨",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // در RTL ← گوشه چپِ خلوت بنر
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0x8C0B1220))
                        .padding(horizontal = 9.dp, vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // ═══ ردیف هویت: آواتار تو‌حلقه + نام + چیپ وضعیت ═══
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    // آواتار سلطنتی با حلقه خارجی ظریف — تصویر ثابت سه‌بعدی از خانواده آجیل ✨
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.14f))
                            .border(1.dp, Gold.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // حلقه داخلی از رنگ‌های تم فعال + چهره سه‌بعدی آجیل (پایدار به ازای مشتری)
                        Image(
                            painter = painterResource(avatarResFor(customer.name)),
                            contentDescription = "آواتار مشتری",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    Brush.linearGradient(listOf(p.gold, p.primary)),
                                    CircleShape
                                )
                        )
                    }
                    // نقطه وضعیت اعتبار روی حلقه آواتار
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B1220))
                            .border(1.dp, Color(0xFF0B1220), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                                .shadow(4.dp, CircleShape, ambientColor = statusColor, spotColor = statusColor)
                        )
                    }
                    // نشان طلایی ترتیب ویزیت (پس از بهینه‌سازی مسیر) — داخل آواتار، بدون بیرون‌زدگی
                    if (order != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFFFFF3D6), Gold)))
                                .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                order.toFaNumber(),
                                color = Color(0xFF4A3400),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            customer.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (customer.isVip) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Filled.MilitaryTech,
                                contentDescription = "VIP",
                                tint = Gold,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "گروه ${customer.groupName} • کد ${customer.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusChip(
                    text = if (customer.pendingApproval) "در انتظار تأیید"
                    else if (customer.creditOk) "مجاز" else "مسدود",
                    color = if (customer.pendingApproval) Gold else statusColor
                )
            }

            Spacer(Modifier.height(10.dp))

            // ═══ پنل اطلاعات مشتری ═══
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x0DFFFFFF))
                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                InfoLine(
                    icon = Icons.Filled.LocationOn,
                    tint = Gold,
                    text = "${customer.city} — ${customer.address}"
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        InfoLine(
                            icon = Icons.Filled.Call,
                            tint = NeonPurple,
                            text = customer.phone
                        )
                    }
                    Box(Modifier.weight(1f)) {
                        InfoLine(
                            icon = Icons.Filled.DateRange,
                            tint = AccentText,
                            text = "آخرین خرید: ${customer.lastPurchaseDaysAgo.toFaNumber()} روز پیش"
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (customer.pendingApproval) {
                    // بنر در انتظار تأیید حسابداری
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Gold.copy(alpha = 0.11f))
                            .border(1.dp, Gold.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.HourglassEmpty,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            "در انتظار تأیید حسابداری آتیران — پس از تأیید، برای فاکتور فعال می‌شود",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                // کپسول مانده حساب — نمای فوری سلامت مالی مشتری
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusColor.copy(alpha = 0.11f))
                        .border(1.dp, statusColor.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.AccountBalanceWallet,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (customer.debt > 0) "مانده حساب: ${customer.debt.toFaPrice()}"
                        else "حساب تسویه است ✅",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ═══ اکشن اصلی: صدور فاکتور + تماس + مسیریابی — همه دکمه‌ها دقیقاً ۵۶dp هم‌تراز ═══
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (customer.pendingApproval) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Gold.copy(alpha = 0.10f))
                            .border(1.dp, Gold.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "پس از تأیید حسابداری، صدور فاکتور فعال می‌شود ⏳",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    NeonGreenButton(
                        text = "صدور فاکتور برای این مشتری",
                        onClick = onPick,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.dp, NeonPurple.copy(alpha = 0.5f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = NeonPurple.copy(alpha = 0.2f),
                        contentColor = NeonPurple
                    )
                ) { Icon(Icons.Filled.Call, contentDescription = "تماس", modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onNavigate,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(1.dp, Gold.copy(alpha = 0.5f), CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Gold.copy(alpha = 0.2f),
                        contentColor = Gold
                    )
                ) { Icon(Icons.Filled.NearMe, contentDescription = "مسیریابی", modifier = Modifier.size(22.dp)) }
            }

            Spacer(Modifier.height(8.dp))

            // ═══ اکشن‌های کوچک: پیامک | مانده | گردش حساب ═══
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniActionChip("پیامک", Icons.Filled.Sms, NeonGreen, Modifier.weight(1f), onSms)
                MiniActionChip("مانده", Icons.Filled.Message, NeonPurple, Modifier.weight(1f), onSmsBalance)
                MiniActionChip("گردش حساب", Icons.Filled.ReceiptLong, Gold, Modifier.weight(1f), onStatement)
            }
        }
    }
}

/** سطر اطلاعات کوچک: آیکن رنگی در گوی ملایم + متن. */
@Composable
private fun InfoLine(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** چیپ اکشن کوچک زیر کارت (پیامک / مانده / گردش حساب). */
@Composable
private fun MiniActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    // چیپ اکشن — ارتفاع ثابت و بردر یکنواخت دورتا‌دور برای هر سه دکمه (تراز دقیق)
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (tint == NeonPurple) AccentText else tint)
    }
}

/** فیلد جستجوی مشتری (نام/کد/شهر) با سبک شیشه‌ای. */
@Composable
private fun CustomerSearchField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Search, contentDescription = null,
            tint = TextSecondary, modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("جستجوی مشتری (نام/کد/شهر)…", color = TextSecondary) },
            singleLine = true,
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = NeonPurple
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}


/**
 * ساخت متن پیامک مانده حساب با قالب مشخص، تاریخ روز و اطلاعات ضروری —
 * هوشمندانه حداکثر در حد ۳ بخش پیامکی (≤ ۱۸۰ نویسه) کوتاه می‌شود.
 */
private fun balanceSmsMessage(c: CustomerEntity): String {
    val date = System.currentTimeMillis().toFaDate()
    val status = if (c.debt > 0)
        "خواهشمند است نسبت به تسویه حساب اقدام فرمایید."
    else
        "حساب شما تسویه است؛ از همراهی شما سپاسگزاریم."
    val msg = "سلام ${c.name} عزیز؛ مانده حساب شما نزد آجیل و خشکبار آتیران در تاریخ $date مبلغ ${c.debt.toFaPrice()} ریال می‌باشد. $status — آتیران"
    return msg.take(180)
}

/** باز کردن اپ پیامک گوشی با شماره و متن آماده (بدون نیاز به مجوز). */
private fun openSmsApp(context: android.content.Context, phone: String, body: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:$phone")
        putExtra("sms_body", body)
    }
    runCatching { context.startActivity(intent) }
}

// ═══════════════ دکمه قهرمان دولاینه — عنوان/زیرعنوان دقیقاً داخل قاب ═══════════════

/**
 * جایگزین NeonGreen/Purple Button برای اکشن‌های بالای صفحه:
 * متن هرگز به دو خط نمی‌شکند، کپسول آیکن با فاصله ثابت داخل قاب است و
 * بردر دقیقاً روی مرز خود دکمه رسم می‌شود (هیچ لایه‌ای بیرون نمی‌زند).
 */
@Composable
private fun HeroActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    faceTop: Color,
    faceBottom: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = vizitorPalette
    Row(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(faceTop, faceBottom)))
            .border(1.dp, p.gold.copy(alpha = 0.38f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // کپسول شیشه‌ای آیکن — فاصله ثابت از لبه‌ها، کاملاً داخل قاب
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = contentColor, modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
