/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | «همهٔ فعالیت‌های ویزیتور» (v2.18.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  خواستهٔ کارفرما: «یک برنامه مختص تمامی فعالیت‌های ویزیتور با قابلیت‌های کامل».
 *  این صفحه، مرکز همهٔ کارهای روزمرهٔ ویزیتور است:
 *    ▸ منوی سه‌بعدی کامل (۱۲ فعالیت) با کاشی‌های برجستهٔ قابل لمس
 *    ▸ نمودار ستونی سه‌بعدی «ویزیت‌های هفته» از جدول واقعی dbo.Visit
 *    ▸ جدول سه‌بعدی آخرین ویزیت‌ها (مشتری، تاریخ، ساعت، مدت، توضیح)
 *    ▸ جدول سه‌بعدی مطالبات در خطر (مشتریان بدهکار با مبلغ بدهی)
 *    ▸ آخرین سند صادرشده: پیش‌نمایش/نتیجهٔ ثبت واقعی پیش‌فاکتور
 *    ▸ مسیرهای تعریف‌شده (dbo.masir) و همگام‌سازی کامل
 *  هر عددی که دیده می‌شود از سرور یا دیتابیس محلی آمده است؛ هیچ دادهٔ نمایشی
 *  در این صفحه ساخته نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.sqldirect.VizitorGateway
import ir.atiran.vizitor.sqldirect.VizitorSession
import ir.atiran.vizitor.ui.components.GlamourButton
import ir.atiran.vizitor.ui.components.GlamourTone
import ir.atiran.vizitor.ui.components.Lux3DBarChart
import ir.atiran.vizitor.ui.components.Lux3DItem
import ir.atiran.vizitor.ui.components.Lux3DMenu
import ir.atiran.vizitor.ui.components.Lux3DNote
import ir.atiran.vizitor.ui.components.Lux3DOrbRow
import ir.atiran.vizitor.ui.components.Lux3DSectionTitle
import ir.atiran.vizitor.ui.components.Lux3DSlab
import ir.atiran.vizitor.ui.components.Lux3DStage
import ir.atiran.vizitor.ui.components.Lux3DStatOrb
import ir.atiran.vizitor.ui.components.Lux3DTable
import ir.atiran.vizitor.ui.components.MaGoldDivider
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

@Composable
fun ActivitiesScreen(
    viewModel: VizitorViewModel,
    onBack: () -> Unit,
    onOpenBriefing: () -> Unit,
    onOpenVisits: () -> Unit,
    onOpenCustomers: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    val feed by viewModel.activities.collectAsState()
    val busy by viewModel.activitiesBusy.collectAsState()
    val syncing by viewModel.syncing.collectAsState()
    val outcome by viewModel.invoiceOutcome.collectAsState()
    val session by VizitorSession.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadActivities(true) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ═════════ ۱) کارت قهرمان ═════════
        item {
            Lux3DStage(
                height = 172.dp,
                modifier = Modifier.clip(RoundedCornerShape(22.dp))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.surfaceDeep.copy(alpha = 0.7f))
                                .border(1.dp, p.gold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickableBack(onBack),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.Icon(
                                Icons.Filled.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = p.gold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "فعالیت‌های ویزیتور",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = p.gold
                            )
                            Text(
                                "VIZITOR ACTIVITY CENTER",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                color = p.textSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(p.surfaceDeep.copy(alpha = 0.5f))
                            .border(1.dp, p.gold.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 11.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (feed?.connected == true) MaGreen else p.danger)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (session.erpName.ifBlank { session.erpUser }.ifBlank { "ویزیتور" }) +
                                " • " + (feed?.visitsToday ?: 0).toFaNumber() + " ویزیت امروز" +
                                " • " + (feed?.routes?.size ?: 0).toFaNumber() + " مسیر",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = p.textPrimary,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        // ═════════ ۲) شاخص‌های امروز (گوی‌های سه‌بعدی) ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "امروز شما در یک نگاه",
                    subtitle = "ویزیت، مطالبات، سهمیه و اسناد — همه از دادهٔ واقعی",
                    icon = Icons.Filled.Verified,
                    badge = if (busy) "به‌روزرسانی…" else null
                )
                Spacer(Modifier.height(12.dp))
                Lux3DOrbRow {
                    Lux3DStatOrb(
                        value = (feed?.visitsToday ?: 0).toFaNumber(),
                        label = "ویزیت امروز",
                        caption = "جدول dbo.Visit",
                        color = MaGreen,
                        size = 88.dp,
                        modifier = Modifier.weight(1f)
                    )
                    Lux3DStatOrb(
                        value = (feed?.limits?.quotaLeft?.toFaNumber() ?: "∞"),
                        label = "سهمیهٔ فاکتور",
                        caption = "TedadFactorMojazMande",
                        size = 88.dp,
                        modifier = Modifier.weight(1f)
                    )
                    Lux3DStatOrb(
                        value = (feed?.debtors?.size ?: 0).toFaNumber(),
                        label = "بدهکار",
                        caption = "نیازمند پیگیری",
                        color = p.danger,
                        size = 88.dp,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                MaGoldDivider()
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "اعتبار شما: " + (feed?.limits?.credit?.let { it.toFaPrice() } ?: "—"),
                        fontSize = 10.5.sp,
                        color = p.textSecondary
                    )
                    Text(
                        "نقدی " + (feed?.limits?.percentCash?.toInt()?.toFaNumber() ?: "۰") + "٪ • چکی " +
                            (feed?.limits?.percentCheque?.toInt()?.toFaNumber() ?: "۰") + "٪",
                        fontSize = 10.5.sp,
                        color = p.textSecondary
                    )
                }
            }
        }

        // ═════════ ۳) منوی سه‌بعدی همهٔ فعالیت‌ها ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "همهٔ فعالیت‌ها",
                    subtitle = "منوی سه‌بعدی — با لمس هر کاشی، همان بخش باز می‌شود",
                    icon = Icons.Filled.PlayCircle
                )
                Spacer(Modifier.height(12.dp))
                Lux3DMenu(
                    items = listOf(
                        Lux3DItem("visits", "ثبت ویزیت", "مراجعه با موقعیت", Icons.Filled.PlayCircle, badge = (feed?.visitsToday ?: 0).toFaNumber()),
                        Lux3DItem("customers", "مشتریان", "فهرست و پیگیری", Icons.Filled.Person),
                        Lux3DItem("catalog", "ویترین", "کالا و قیمت", Icons.Filled.Storefront),
                        Lux3DItem("cart", "سبد و سند", "اقلام و پیش‌فاکتور", Icons.Filled.ShoppingCart),
                        Lux3DItem("reports", "گزارش‌ها", "روند فروش", Icons.Filled.Analytics),
                        Lux3DItem("briefing", "اطلاع‌رسانی", "خلاصهٔ وضعیت", Icons.Filled.Inventory2),
                        Lux3DItem("scanner", "اسکنر بارکد", "افزودن سریع کالا", Icons.Filled.QrCodeScanner),
                        Lux3DItem("chat", "گفتگوی ویزیتورها", "هماهنگی تیمی", Icons.Filled.Message),
                        Lux3DItem("newcustomer", "مشتری جدید", "ثبت در انتظار تأیید", Icons.Filled.PersonAdd),
                        Lux3DItem("sync", "همگام‌سازی", "کالا/مشتری/فاکتور", Icons.Filled.Sync),
                        Lux3DItem("routes", "مسیرها", "dbo.masir", Icons.Filled.Route, badge = (feed?.routes?.size ?: 0).toFaNumber()),
                        Lux3DItem("settings", "تنظیم اتصال", "سرور و ورود", Icons.Filled.Settings),
                    ),
                    columns = 3,
                    dense = true,
                    onSelect = { item ->
                        when (item.key) {
                            "visits" -> onOpenVisits()
                            "customers" -> onOpenCustomers()
                            "catalog" -> onOpenCatalog()
                            "cart" -> onOpenCart()
                            "reports" -> onOpenReports()
                            "briefing" -> onOpenBriefing()
                            "scanner" -> onOpenScanner()
                            "chat" -> onOpenChat()
                            "newcustomer" -> onOpenCustomers()
                            "sync" -> viewModel.syncNow()
                            "settings" -> onOpenSettings()
                            "routes" -> viewModel.loadActivities(true)
                        }
                    }
                )
            }
        }

        // ═════════ ۴) نمودار ستونی سه‌بعدی ویزیت‌های هفته ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "ویزیت‌های هفتهٔ شما",
                    subtitle = "ستون‌های سه‌بعدی از تاریخ‌های واقعی dbo.Visit",
                    icon = Icons.Filled.Analytics,
                    badge = (feed?.visits?.size ?: 0).toFaNumber() + " ویزیت"
                )
                Spacer(Modifier.height(10.dp))
                Lux3DBarChart(
                    rows = feed?.weeklyVisits ?: emptyList(),
                    height = 200.dp,
                    valueLabel = { it.toFaNumber() }
                )
            }
        }

        // ═════════ ۵) آخرین ویزیت‌ها ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "آخرین ویزیت‌های ثبت‌شده",
                    subtitle = "مشتری، تاریخ، ساعت و مدت مراجعه",
                    icon = Icons.Filled.Receipt
                )
                Spacer(Modifier.height(12.dp))
                Lux3DTable(
                    headers = listOf("مشتری", "تاریخ", "ساعت", "مدت"),
                    rows = (feed?.visits ?: emptyList()).take(12).map { v ->
                        listOf(
                            v.customerName.ifBlank { "مشتری " + v.shmo.toFaNumber() },
                            v.dateCreated.ifBlank { "—" },
                            v.timeCreated.take(5).ifBlank { "—" },
                            v.durationMin.toFaNumber() + " د"
                        )
                    },
                    weights = listOf(1.8f, 1.1f, 0.9f, 0.8f),
                    accentColumn = 1,
                    emptyText = "هنوز ویزیتی ثبت نشده — از کاشی «ثبت ویزیت» شروع کنید"
                )
            }
        }

        // ═════════ ۶) مطالبات در خطر ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "مطالبات در خطر",
                    subtitle = "بیشترین بدهی‌ها — اول این‌ها را پیگیری کنید",
                    icon = Icons.Filled.Person,
                    badge = (feed?.debtors?.size ?: 0).toFaNumber()
                )
                Spacer(Modifier.height(12.dp))
                Lux3DTable(
                    headers = listOf("مشتری", "بدهی"),
                    rows = (feed?.debtors ?: emptyList()).take(10).map { (name, debt) ->
                        listOf(name, debt.toFaPrice())
                    },
                    weights = listOf(1.7f, 1f),
                    accentColumn = 1,
                    emptyText = "هیچ مشتری بدهکاری در فهرست همگام‌شده نیست"
                )
            }
        }

        // ═════════ ۷) مسیرهای تعریف‌شده ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "مسیرهای شما",
                    subtitle = "dbo.masir — مسیرهای رسمی همین ویزیتور در ERP",
                    icon = Icons.Filled.Route,
                    badge = (feed?.routes?.size ?: 0).toFaNumber()
                )
                Spacer(Modifier.height(12.dp))
                Lux3DTable(
                    headers = listOf("شماره", "نام مسیر", "منطقه"),
                    rows = (feed?.routes ?: emptyList()).take(12).map {
                        listOf(it.number.ifBlank { "—" }, it.name.ifBlank { "—" }, it.region?.toFaNumber() ?: "—")
                    },
                    accentColumn = 1,
                    emptyText = "مسیری برای شما تعریف نشده است"
                )
            }
        }

        // ═════════ ۸) آخرین سند (پیش‌نمایش/ثبت واقعی) ═════════
        item {
            Lux3DSlab {
                Lux3DSectionTitle(
                    title = "آخرین سند پیش‌فاکتور",
                    subtitle = "نتیجهٔ فراخوانی آخر: پیش‌نمایش یا ثبت واقعی در آتیران",
                    icon = Icons.Filled.Inventory2,
                    badge = when {
                        outcome == null -> null
                        outcome!!.live && outcome!!.ok -> "ثبت شد"
                        outcome!!.live -> "ناموفق"
                        else -> "پیش‌نمایش"
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (outcome == null) {
                    Lux3DNote(
                        text = "هنوز سندی از این گوشی صادر نشده است. پس از «صدور و ارسال» در سبد سفارش، " +
                            "نتیجه و متن کامل سند همین‌جا نمایش داده می‌شود."
                    )
                } else {
                    val o = outcome!!
                    Lux3DNote(
                        text = o.message,
                        tone = if (o.ok && o.live) MaGreen else p.gold
                    )
                    o.preview?.let { pv ->
                        Spacer(Modifier.height(12.dp))
                        Lux3DTable(
                            headers = listOf("کالا", "تعداد", "قیمت", "جمع"),
                            rows = pv.lines.take(12).map { l ->
                                listOf(
                                    l.productName,
                                    l.quantity.toFaNumber(),
                                    l.unitPrice.toFaPrice(),
                                    l.lineSum.toFaPrice()
                                )
                            },
                            weights = listOf(1.9f, 0.9f, 1.1f, 1.1f),
                            accentColumn = 3
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "مشتری: ${pv.customerName} (کد ${pv.customerShmo.toFaNumber()}) • " +
                                "تاریخ ${pv.dateText} • جمع نهایی ${pv.finalAmount.toFaPrice()}",
                            fontSize = 10.sp,
                            color = p.textSecondary,
                            lineHeight = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        GlamourButton(
                            label = "پاک‌کردن این کارت",
                            icon = Icons.Filled.CheckCircle,
                            tone = GlamourTone.GLASS,
                            height = 44.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.clearInvoiceOutcome() }
                        )
                    }
                }
            }
        }

        // ═════════ ۹) دکمه‌های پایانی ═════════
        item {
            Column {
                GlamourButton(
                    label = "همگام‌سازی کامل با سرور",
                    subtitle = "کالا • مشتری • فاکتور • ویزیت • مسیر • سهمیه",
                    icon = Icons.Filled.Sync,
                    tone = GlamourTone.GLASS,
                    loading = syncing,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.syncNow() }
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "این صفحه همهٔ کارهای روزمرهٔ ویزیتور را یک‌جا جمع می‌کند؛ هر عدد از سرور آتیران " +
                        "یا دیتابیس همین گوشی می‌آید.",
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

/** لمس‌پذیری سبک برای دکمهٔ بازگشت. */
private fun Modifier.clickableBack(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
