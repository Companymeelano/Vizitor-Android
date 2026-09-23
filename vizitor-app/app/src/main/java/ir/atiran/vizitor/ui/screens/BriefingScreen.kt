/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحهٔ «اطلاع‌رسانی اولیه به ویزیتور» (v2.18.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  خواستهٔ کارفرما: «... جهت اطلاع‌رسانی اولیه به ویزیتور».
 *  این صفحه، پیش از شروع کار، همه‌چیز را یک‌جا به ویزیتور می‌گوید:
 *    ▸ وضعیت واقعی اتصال (درایور، تأخیر، دیتابیس، نسخهٔ سرور)
 *    ▸ اختیارات خودش در ERP: مشتری/کالا/انبار مجاز، سهمیهٔ فاکتور، اعتبار،
 *      درصد نقدی و چکی (همه از جدول‌های واقعی، بدون مقدار ساختگی)
 *    ▸ دادهٔ آمادهٔ روی گوشی (کالا/مشتری/فاکتور همگام‌شده)
 *    ▸ ترکیب مشتریان (خوش‌حساب/بدهکار) به‌صورت دونات سه‌بعدی
 *    ▸ مسیرهای تعریف‌شدهٔ خودش (dbo.masir)
 *    ▸ وضعیت «ثبت واقعی پیش‌فاکتور» و دلیل روشن/خاموش بودنش
 *    ▸ منوی سه‌بعدی شروع سریع فعالیت‌ها
 *  همهٔ اعداد از سرور/دیتابیس محلی می‌آیند؛ هیچ دادهٔ نمایشی ساخته نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.sqldirect.VizitorGateway
import ir.atiran.vizitor.sqldirect.VizitorSession
import ir.atiran.vizitor.ui.components.GlamourButton
import ir.atiran.vizitor.ui.components.GlamourTone
import ir.atiran.vizitor.ui.components.Lux3DDonut
import ir.atiran.vizitor.ui.components.Lux3DItem
import ir.atiran.vizitor.ui.components.Lux3DMenu
import ir.atiran.vizitor.ui.components.Lux3DNote
import ir.atiran.vizitor.ui.components.Lux3DOrbRow
import ir.atiran.vizitor.ui.components.Lux3DProgress
import ir.atiran.vizitor.ui.components.Lux3DSectionTitle
import ir.atiran.vizitor.ui.components.Lux3DSlab
import ir.atiran.vizitor.ui.components.Lux3DStage
import ir.atiran.vizitor.ui.components.Lux3DStatOrb
import ir.atiran.vizitor.ui.components.Lux3DTable
import ir.atiran.vizitor.ui.components.MaGoldDivider
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MaRed
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.maStatus
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

@Composable
fun BriefingScreen(
    viewModel: VizitorViewModel,
    onStart: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenVisits: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    val briefing by viewModel.briefing.collectAsState()
    val busy by viewModel.briefingBusy.collectAsState()
    val liveWrite by viewModel.liveInvoiceWrite.collectAsState()
    val session by VizitorSession.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadBriefing(true) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ═════════ ۱) کارت قهرمان: هویت ویزیتور + وضعیت اتصال ═════════
        item {
            Lux3DStage(
                height = 232.dp,
                modifier = Modifier.clip(RoundedCornerShape(22.dp))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "اطلاع‌رسانی اولیه",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = p.gold
                    )
                    Text(
                        "ATIRAN • VIZITOR QUICK BRIEF",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = p.textSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, Brush.linearGradient(listOf(p.goldHighlight, p.gold)), CircleShape)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(ir.atiran.vizitor.R.drawable.nut_visitor),
                                contentDescription = "ویزیتور",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(CircleShape)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                session.erpName.ifBlank { session.erpUser }.ifBlank { "ویزیتور" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = p.textPrimary,
                                maxLines = 1
                            )
                            Text(
                                "کد ویزیتور: " + (session.visitorRdf?.toFaNumber() ?: "—") +
                                    " • شرکت: " + (session.companyId?.toFaNumber() ?: "—"),
                                fontSize = 10.sp,
                                color = p.textSecondary
                            )
                            Text(
                                "دیتابیس: " + (session.serverLabel.ifBlank { "—" }),
                                fontSize = 10.sp,
                                color = p.textSecondary,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(7.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (briefing?.probe?.ok == true) MaGreen else MaRed)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    if (briefing?.probe?.ok == true) "متصل و آماده" else "اتصال برقرار نیست",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (briefing?.probe?.ok == true) MaGreen else MaRed
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(p.surfaceDeep.copy(alpha = 0.55f))
                            .border(1.dp, p.gold.copy(alpha = 0.28f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MiniFact(Icons.Filled.DateRange, VizitorGateway.todayJalali())
                        MiniFact(
                            Icons.Filled.Storage,
                            briefing?.probe?.driver ?: "—"
                        )
                        MiniFact(
                            Icons.Filled.AccessTime,
                            (briefing?.probe?.latencyMs ?: 0L).toFaNumber() + " م.ث"
                        )
                    }
                }
            }
        }

        // ═════════ ۲) وضعیت اتصال و خدمات ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "وضعیت اتصال و خدمات",
                    subtitle = "سنجش زندهٔ همین لحظه از سرور آتیران (اتصال مستقیم پورت ۱۴۳۳)",
                    icon = Icons.Filled.Verified,
                    badge = if (busy) "در حال سنجش…" else null
                )
                Spacer(Modifier.height(12.dp))
                Lux3DOrbRow {
                    Lux3DStatOrb(
                        value = if (briefing?.probe?.ok == true) "✓" else "✕",
                        label = "اتصال",
                        caption = briefing?.probe?.message.orEmpty().take(40),
                        color = if (briefing?.probe?.ok == true) MaGreen else MaRed,
                        size = 92.dp,
                        modifier = Modifier.weight(1f)
                    )
                    Lux3DStatOrb(
                        value = (briefing?.productsLocal ?: 0).toFaNumber(),
                        label = "کالا",
                        caption = "همگام‌شده روی گوشی",
                        size = 92.dp,
                        modifier = Modifier.weight(1f)
                    )
                    Lux3DStatOrb(
                        value = (briefing?.customersLocal ?: 0).toFaNumber(),
                        label = "مشتری",
                        caption = "فهرست مجاز شما",
                        color = p.goldHighlight,
                        size = 92.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                MaGoldDivider()
                Spacer(Modifier.height(10.dp))
                BriefingStatusRow(
                    label = "دیتابیس و سرور",
                    detail = (briefing?.probe?.database ?: session.database) +
                        " • SQL " + (briefing?.probe?.serverVersion ?: "—"),
                    fraction = if (briefing?.probe?.ok == true) 1f else 0f
                )
                BriefingStatusRow(
                    label = "فاکتورهای سرور",
                    detail = (briefing?.invoicesLocal ?: 0).toFaNumber() + " سند خوانده شده",
                    fraction = if (briefing?.invoicesLocal ?: 0 > 0) 1f else 0.4f
                )
                BriefingStatusRow(
                    label = "ویزیت‌های امروز",
                    detail = (briefing?.visitsToday ?: 0).toFaNumber() + " ثبت در dbo.Visit",
                    fraction = ((briefing?.visitsToday ?: 0) / 5f).coerceIn(0f, 1f)
                )
                BriefingStatusRow(
                    label = "مشتریان بدهکار",
                    detail = (briefing?.debtorsLocal ?: 0).toFaNumber() + " نفر • " +
                        (briefing?.debtTotalLocal ?: 0L).toFaPrice(),
                    fraction = briefing?.healthyShare ?: 0f
                )
            }
        }

        // ═════════ ۳) اختیارات و اهداف ویزیتور ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "اختیارات و اهداف شما در سامانه",
                    subtitle = "همهٔ مقادیر از رکورد خودِ شما درERP خوانده شده‌اند",
                    icon = Icons.Filled.Verified
                )
                Spacer(Modifier.height(12.dp))
                Lux3DTable(
                    headers = listOf("شرح", "مقدار", "وضعیت"),
                    rows = listOf(
                        listOf(
                            "مجوز مشتری",
                            (briefing?.identity?.allowedCustomers ?: 0).toFaNumber() + " مشتری",
                            if ((briefing?.identity?.allowedCustomers ?: 0) > 0) "فعال" else "بررسی شود"
                        ),
                        listOf(
                            "مجوز کالا",
                            (briefing?.identity?.allowedProducts ?: 0).toFaNumber() + " کالا",
                            if ((briefing?.identity?.allowedProducts ?: 0) > 0) "فعال" else "بررسی شود"
                        ),
                        listOf(
                            "مجوز انبار",
                            (briefing?.identity?.allowedWarehouses ?: 0).toFaNumber() + " انبار",
                            if ((briefing?.identity?.allowedWarehouses ?: 0) > 0) "فعال" else "بررسی شود"
                        ),
                        listOf(
                            "سهمیهٔ فاکتور باقی‌مانده",
                            briefing?.limits?.quotaLeft?.let { it.toFaNumber() + " فاکتور" } ?: "نامحدود",
                            "TedadFactorMojazMande"
                        ),
                        listOf(
                            "اعتبار (eteb)",
                            briefing?.limits?.credit?.let { it.toFaPrice() } ?: "—",
                            if ((briefing?.limits?.credit ?: 0L) > 0L) "مثبت" else "صفر"
                        ),
                        listOf(
                            "درصد نقدی / چکی",
                            ((briefing?.limits?.percentCash?.toInt() ?: 0).toFaNumber() + "٪ / " +
                                (briefing?.limits?.percentCheque?.toInt() ?: 0).toFaNumber() + "٪"),
                            "per_p_d_naghd / per_p_d_check"
                        ),
                    ),
                    weights = listOf(1.5f, 1.2f, 1.1f),
                    accentColumn = 1,
                    emptyText = "برای نمایش اختیارات، اتصال باید برقرار باشد"
                )
                Spacer(Modifier.height(6.dp))
                Lux3DProgress(
                    fraction = briefing?.healthyShare ?: 0f,
                    label = "سهم مشتریان خوش‌حساب",
                    valueText = (((briefing?.healthyShare ?: 0f) * 100f).toInt().toFaNumber()) + "٪",
                    color = MaGreen
                )
            }
        }

        // ═════════ ۴) ترکیب مشتریان (دونات سه‌بعدی) ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "ترکیب مشتریان شما",
                    subtitle = "دونات سه‌بعدی بر اساس مانده حسابِ واقعی هر مشتری",
                    icon = Icons.Filled.People,
                    badge = (briefing?.customersLocal ?: 0).toFaNumber() + " مشتری"
                )
                Spacer(Modifier.height(14.dp))
                val total = (briefing?.customersLocal ?: 0).coerceAtLeast(0)
                val debtors = (briefing?.debtorsLocal ?: 0).coerceAtLeast(0)
                val healthy = (total - debtors).coerceAtLeast(0)
                Lux3DDonut(
                    slices = listOf(
                        "خوش‌حساب" to healthy.toFloat(),
                        "بدهکار" to debtors.toFloat(),
                    ),
                    centerValue = (total.toFaNumber()),
                    centerLabel = "کل مشتریان",
                    colors = listOf(MaGreen, MaRed),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ═════════ ۵) مسیرهای تعریف‌شده ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "مسیرهای تعریف‌شدهٔ شما",
                    subtitle = "از جدول واقعی dbo.masir — مبنای برنامهٔ روزانهٔ ویزیت",
                    icon = Icons.Filled.Route,
                    badge = (briefing?.routes?.size ?: 0).toFaNumber()
                )
                Spacer(Modifier.height(12.dp))
                Lux3DTable(
                    headers = listOf("شمارهٔ مسیر", "نام مسیر", "منطقه"),
                    rows = (briefing?.routes ?: emptyList()).take(12).map {
                        listOf(
                            it.number.ifBlank { "—" },
                            it.name.ifBlank { "—" },
                            it.region?.toFaNumber() ?: "—"
                        )
                    },
                    accentColumn = 1,
                    emptyText = "هنوز مسیری برای شما در ERP تعریف نشده است"
                )
            }
        }

        // ═════════ ۶) مسیر نوشتن سند (پیش‌نمایش / ثبت واقعی) ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "مسیر ثبت سند در آتیران",
                    subtitle = "EXEC dbo.add_sail_pish + تریگر trig_sst_pish — همان مسیر خودِ ERP",
                    icon = Icons.Filled.Receipt,
                    badge = if (briefing?.liveWriteReady == true) "آماده" else "پیش‌نمایش"
                )
                Spacer(Modifier.height(12.dp))
                Lux3DNote(
                    text = briefing?.liveWriteReason ?: "در حال بررسی مسیر ثبت…",
                    tone = if (briefing?.liveWriteReady == true) MaGreen else p.gold
                )
                Spacer(Modifier.height(12.dp))
                Lux3DTable(
                    headers = listOf("وضعیت", "شرح"),
                    rows = listOf(
                        listOf(
                            if (liveWrite) "روشن" else "خاموش",
                            if (liveWrite) "سند پس از تأیید، واقعاً در آتیران ثبت می‌شود"
                            else "فقط پیش‌نمایش سند ساخته می‌شود و چیزی در سرور نوشته نمی‌شود"
                        ),
                        listOf(
                            if (briefing?.liveWriteReady == true) "موجود" else "ناموجود",
                            "پیش‌فاکتور مرجع در dbo.sailfact_pish (مبنای مقادیر سربرگ)"
                        ),
                    ),
                    weights = listOf(0.8f, 2.4f),
                    accentColumn = 0
                )
                Spacer(Modifier.height(12.dp))
                GlamourButton(
                    label = if (liveWrite) "خاموش‌کردن ثبت واقعی" else "فعال‌کردن ثبت واقعی در آتیران",
                    subtitle = if (briefing?.liveWriteReady == true)
                        "با مسئولیت خودتان — پس از تأیید حسابدار"
                    else "تا وقتی پیش‌فاکتور مرجع در ERP نباشد، فعال نمی‌شود",
                    icon = Icons.Filled.Warning,
                    tone = if (briefing?.liveWriteReady == true) GlamourTone.GREEN else GlamourTone.GLASS,
                    enabled = briefing?.liveWriteReady == true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.setLiveInvoiceWrite(!liveWrite) }
                )
            }
        }

        // ═════════ ۷) منوی سه‌بعدی شروع سریع ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "شروع سریع فعالیت‌ها",
                    subtitle = "منوی سه‌بعدی — هر کاشی یک کار روزمرهٔ ویزیتور",
                    icon = Icons.Filled.Dashboard
                )
                Spacer(Modifier.height(12.dp))
                Lux3DMenu(
                    items = listOf(
                        Lux3DItem("visits", "ثبت ویزیت", "ثبت مراجعه با موقعیت و توضیح", Icons.Filled.PlayCircle),
                        Lux3DItem("customers", "مشتریان", "فهرست و پیگیری مطالبات", Icons.Filled.Person),
                        Lux3DItem("catalog", "ویترین", "کالا، قیمت و موجودی زنده", Icons.Filled.Storefront),
                        Lux3DItem("cart", "سبد و سند", "اقلام و صدور پیش‌فاکتور", Icons.Filled.ShoppingCart),
                        Lux3DItem("reports", "گزارش‌ها", "روند فروش و اسناد سرور", Icons.Filled.Analytics),
                        Lux3DItem("settings", "تنظیم اتصال", "سرور، دیتابیس و ورود", Icons.Filled.Settings),
                    ),
                    columns = 2,
                    onSelect = { item ->
                        when (item.key) {
                            "visits" -> onOpenVisits()
                            "customers" -> onOpenCustomers()
                            "catalog" -> onOpenCatalog()
                            "cart" -> onOpenActivities()
                            "reports" -> onOpenReports()
                            "settings" -> onOpenSettings()
                        }
                    }
                )
            }
        }

        // ═════════ ۸) دکمه‌های پایانی ═════════
        item {
            Column {
                GlamourButton(
                    label = "همگام‌سازی کامل با سرور",
                    subtitle = "کالا • مشتری • فاکتور • ویزیت • مسیر",
                    icon = Icons.Filled.Sync,
                    tone = GlamourTone.GLASS,
                    loading = busy,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.syncNow() }
                )
                Spacer(Modifier.height(10.dp))
                GlamourButton(
                    label = "شروع کار",
                    subtitle = "ورود به پیشخوان ویزیتور",
                    icon = Icons.Filled.CheckCircle,
                    tone = GlamourTone.GOLD,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        viewModel.markBriefingSeen()
                        onStart()
                    }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "این اطلاعات در هر اجرا از نو از سرور خوانده می‌شود؛ هیچ عدد نمایشی در برنامه ساخته نمی‌شود.",
                    fontSize = 9.5.sp,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 13.sp
                )
                Spacer(Modifier.height(14.dp))
                MilanoFooter()
            }
        }
    }
}

/** یک قلم کوچک در نوار اطلاعات کارت قهرمان. */
@Composable
private fun MiniFact(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val p = vizitorPalette
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription = null,
            tint = p.gold,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(text, fontSize = 10.sp, color = p.textPrimary, maxLines = 1)
    }
}

/** ردیف وضعیت با نوار رنگی (پایدار / نیازمند توجه / بحران) — با کارت سه‌بعدی. */
@Composable
private fun BriefingStatusRow(label: String, detail: String, fraction: Float) {
    val (color, word) = maStatus(fraction)
    val p = vizitorPalette
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(p.surfaceDeep.copy(alpha = 0.55f))
            .border(1.dp, color.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = p.textPrimary,
                maxLines = 1
            )
            Text(detail, fontSize = 9.5.sp, color = p.textSecondary, maxLines = 1)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(word, fontSize = 9.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}
