/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | «ارسال فاکتور» به سبک گزارش طلایی (v2.19.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  این صفحه عیناً مثل عکس‌های مرجع چیده شده و **مسیر واقعی ارسال فاکتور**
 *  را دنبال می‌کند:
 *     ۱) نوار بالا (عنوان طلایی + بازگشت + تنظیمات/هشدار/جست‌وجو)
 *     ۲) قرص‌های فیلتر اقلام (همه / موجودی کافی / کم‌موجود)
 *     ۳) کارت طلایی انتخاب مشتری + یادداشت ویزیتور
 *     ۴) اقلام سبد با کنترل تعداد و مبلغ هر قلم
 *     ۵) خلاصهٔ عددی + سهمیهٔ اسناد و اعتبار (از دادهٔ واقعی سرور)
 *     ۶) پنل امضای دیجیتال مشتری
 *     ۷) کارت سند MEELANO: پیش‌فاکتور آتیران (ردیف‌های برچسب — مقدار)
 *     ۸) کاشی‌های خروجی واقعی: اشتراک، تصویر، PDF، Word، گزارش‌ها
 *     ۹) کلید «ثبت واقعی در آتیران» + دکمهٔ طلایی ارسال + کارت نتیجهٔ سند
 *
 *  مسیر ارسال: گوشی (Room) → VizitorGateway.submitCart → EXEC dbo.add_sail_pish
 *  → اقلام در dbo.subsailtemp_pish (تریگر trig_sst_pish) → بازخوانی تأییدی از
 *  dbo.subsailfact_pish ⋈ dbo.sailfact_pish. تا وقتی «پیش‌فاکتور مرجع» در ERP
 *  موجود نباشد، سند فقط پیش‌نمایش می‌شود و هیچ چیزی در سرور نوشته نمی‌شود.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.CartItemEntity
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.data.local.InvoiceEntity
import ir.atiran.vizitor.data.share.InvoiceShare
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.GlamourButton
import ir.atiran.vizitor.ui.components.GlamourTone
import ir.atiran.vizitor.ui.components.GoldBurstOverlay
import ir.atiran.vizitor.ui.components.MaAmber
import ir.atiran.vizitor.ui.components.MaDocCard
import ir.atiran.vizitor.ui.components.MaEmptyState
import ir.atiran.vizitor.ui.components.MaGoldCta
import ir.atiran.vizitor.ui.components.MaGreen
import ir.atiran.vizitor.ui.components.MaHeroTitle
import ir.atiran.vizitor.ui.components.MaNavItem
import ir.atiran.vizitor.ui.components.MaRed
import ir.atiran.vizitor.ui.components.MaSectionHeader
import ir.atiran.vizitor.ui.components.MaSegmentPills
import ir.atiran.vizitor.ui.components.MaStatStrip
import ir.atiran.vizitor.ui.components.MaThinBar
import ir.atiran.vizitor.ui.components.MaToolGrid
import ir.atiran.vizitor.ui.components.MaToolTile
import ir.atiran.vizitor.ui.components.MaTopBar
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.RoyalSurfaceBrush
import ir.atiran.vizitor.ui.components.ToggleRow
import ir.atiran.vizitor.ui.components.dashboardBackdrop
import ir.atiran.vizitor.ui.components.metalPanel
import ir.atiran.vizitor.ui.components.rememberScreenFit
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.components.screenSafePadding
import ir.atiran.vizitor.ui.theme.DarkSlateDeep
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.parseAmount
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice
import java.io.ByteArrayOutputStream

@Composable
fun CartScreen(
    viewModel: VizitorViewModel,
    onBack: () -> Unit = {},
    onOpenCatalog: () -> Unit = {},
    onOpenCustomers: () -> Unit = {},
    onOpenReports: () -> Unit = {},
) {
    val p = vizitorPalette
    val fit = rememberScreenFit()
    val context = LocalContext.current

    val items by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val gross by viewModel.cartTotal.collectAsState()
    val invoices by viewModel.invoices.collectAsState()
    val outcome by viewModel.invoiceOutcome.collectAsState()
    val liveWrite by viewModel.liveInvoiceWrite.collectAsState()
    val aiSuggestion by viewModel.aiSuggestion.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val briefing by viewModel.briefing.collectAsState()
    val briefingBusy by viewModel.briefingBusy.collectAsState()

    var note by remember { mutableStateOf("") }
    var filterKey by remember { mutableStateOf("all") }
    var showPicker by remember { mutableStateOf(false) }
    var previewOpen by remember { mutableStateOf(false) }
    var goldBurst by remember { mutableStateOf(false) }
    val signaturePaths = remember { mutableStateListOf<Path>() }
    var hasSignature by remember { mutableStateOf(false) }

    // سهمیه/اعتبار و آمادگی مسیر سند از دادهٔ واقعی سرور
    LaunchedEffect(Unit) { if (briefing == null) viewModel.loadBriefing() }

    val lastInvoice: InvoiceEntity? = remember(invoices) { invoices.maxByOrNull { it.createdAt } }
    val linesCount = items.size
    val lowStock = items.count { it.stock < it.quantity }
    val avgLine = if (linesCount == 0) 0L else gross / linesCount.toLong()
    val filtered = remember(items, filterKey) {
        when (filterKey) {
            "ok" -> items.filter { it.stock >= it.quantity }
            "low" -> items.filter { it.stock < it.quantity }
            else -> items
        }
    }
    val limits = briefing?.limits

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .dashboardBackdrop()
                .screenSafePadding(),
            contentPadding = PaddingValues(
                start = fit.pad, end = fit.pad,
                top = if (fit.short) 8.dp else 12.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (fit.short) 10.dp else 13.dp)
        ) {
            val contentMod =
                if (fit.contentMax > 0.dp) Modifier.fillMaxWidth().widthIn(max = fit.contentMax)
                else Modifier.fillMaxWidth()

            // ═══ ۱) نوار بالا ═══
            item {
                MaTopBar(
                    title = "سبد سفارش",
                    eyebrow = "پیش‌فاکتور آتیران — ارسال به سرور یا پیش‌نمایش امن",
                    onBack = onBack,
                    onFilter = { filterKey = if (filterKey == "all") "low" else "all" },
                    onAlert = { previewOpen = outcome?.preview != null },
                    alertCount = if (lowStock > 0) lowStock else 0,
                    alertTint = if (lowStock > 0) MaRed else MaGreen,
                    onSearch = { showPicker = true },
                    chip = if (liveWrite) "ثبت واقعی" else "پیش‌نمایش",
                    chipColor = if (liveWrite) MaGreen else MaAmber,
                    modifier = contentMod
                )
            }

            // ═══ ۲) قرص‌های فیلتر اقلام ═══
            item {
                MaSegmentPills(
                    items = listOf(
                        MaNavItem("all", "همه (${linesCount.toFaNumber()})", Icons.Filled.ShoppingCart),
                        MaNavItem("ok", "موجودی کافی (${(linesCount - lowStock).toFaNumber()})", Icons.Filled.CheckCircle),
                        MaNavItem("low", "کم‌موجود (${lowStock.toFaNumber()})", Icons.Filled.Warning),
                    ),
                    selectedKey = filterKey,
                    onSelect = { filterKey = it },
                    tints = mapOf("ok" to MaGreen, "low" to MaRed),
                    modifier = contentMod
                )
            }

            // ═══ ۳) تیتر قهرمان ═══
            item {
                MaHeroTitle(
                    title = "ارسال فاکتور",
                    subtitle = "ثبت پیش‌فاکتور فروش در سامانهٔ آتیران",
                    modifier = contentMod
                )
            }

            // ═══ ۴) کارت طلایی انتخاب مشتری ═══
            item {
                MaGoldCta(
                    title = selectedCustomer?.name ?: "انتخاب مشتری فاکتور",
                    subtitle = selectedCustomer?.let {
                        "گروه ${it.groupName} • شهر ${it.city} • کد ${it.code}"
                    } ?: "با نام، کد یا شهر جست‌وجو کنید — یا مشتری متفرقه بزنید",
                    icon = Icons.Filled.Person,
                    badge = selectedCustomer?.let { if (it.debt > 0) "بدهکار" else "خوش‌حساب" },
                    onClick = { showPicker = true },
                    modifier = contentMod
                )
            }

            // ═══ ۵) یادداشت ویزیتور ═══
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("توضیحات ویزیتور (درج در پیش‌فاکتور آتیران)") },
                    placeholder = { Text("مثال: تحویل قبل از پنجشنبه، کارتن‌های پسته بدون مغز باز…") },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.gold,
                        unfocusedBorderColor = p.gold.copy(alpha = 0.25f),
                        focusedLabelColor = p.gold,
                        cursorColor = p.gold
                    ),
                    modifier = contentMod
                )
            }

            // ═══ ۶) اقلام سبد ═══
            item {
                MaSectionHeader(
                    title = "اقلام سبد سفارش",
                    icon = Icons.Filled.ShoppingCart,
                    count = linesCount.toFaNumber()
                )
            }

            if (items.isEmpty()) {
                item {
                    Column(
                        modifier = contentMod
                            .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MaEmptyState(
                            title = "سبد خالی است",
                            subtitle = "از «ویترین کالا» یا اسکنر بارکد، کالا اضافه کنید"
                        )
                        Spacer(Modifier.height(10.dp))
                        GlamourButton(
                            label = "رفتن به ویترین کالا",
                            icon = Icons.Filled.Storefront,
                            tone = GlamourTone.GOLD,
                            height = 52.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onOpenCatalog
                        )
                        Spacer(Modifier.height(8.dp))
                        GlamourButton(
                            label = "انتخاب مشتری",
                            icon = Icons.Filled.Person,
                            tone = GlamourTone.GLASS,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showPicker = true }
                        )
                    }
                }
            }

            items(filtered, key = { it.productId }) { item ->
                CartLine(
                    item = item,
                    onAdd = {
                        if (item.quantity < item.stock) {
                            viewModel.addToCart(
                                ir.atiran.vizitor.data.local.ProductEntity(
                                    item.productId, "", item.productName, "", item.unitPrice, item.stock
                                )
                            )
                        } else viewModel.showToast("موجودی کافی نیست ❌")
                    },
                    onRemove = { viewModel.decrement(item.productId) },
                    onDelete = { viewModel.removeFromCart(item.productId) },
                    onSetQty = { q -> viewModel.setCartQty(item.productId, q) }
                )
            }

            // ═══ ۷) خلاصهٔ عددی ═══
            item {
                MaStatStrip(
                    items = listOf(
                        Triple("تعداد اقلام", linesCount.toFaNumber(), p.gold),
                        Triple("جمع اقلام", gross.toFaPrice(), MaGreen),
                        Triple("میانگین هر قلم", avgLine.toFaPrice(), MaAmber),
                        Triple("امضا", if (hasSignature) "ثبت شده" else "ندارد", if (hasSignature) MaGreen else MaRed),
                    ),
                    modifier = contentMod
                )
            }

            // ═══ ۸) سهمیه و اعتبار واقعی ویزیتور ═══
            item {
                Column(
                    modifier = contentMod
                        .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
                        .padding(13.dp)
                ) {
                    MaSectionHeader(
                        title = "سهمیه و اعتبار ویزیتور",
                        icon = Icons.Filled.Analytics,
                        count = if (briefingBusy) "…" else null
                    )
                    Spacer(Modifier.height(9.dp))
                    MaThinBar(
                        caption = "سهمیهٔ باقی‌ماندهٔ اسناد (TedadFactorMojazMande)",
                        fraction = when {
                            limits?.quotaLeft == null -> 0f
                            limits.quotaLeft <= 0 -> 0f
                            else -> (limits.quotaLeft.toFloat() / (limits.quotaLeft + linesCount).coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        valueText = limits?.quotaLeft?.let { "${it.toFaNumber()} سند" } ?: "—"
                    )
                    Spacer(Modifier.height(10.dp))
                    MaThinBar(
                        caption = "اعتبار مجاز (visitors.eteb)",
                        fraction = when {
                            limits?.credit == null || limits.credit <= 0L -> 0f
                            gross <= 0L -> 0.4f
                            else -> (gross.toFloat() / limits.credit.toFloat()).coerceIn(0f, 1f)
                        },
                        valueText = limits?.credit?.let { toFaPriceSafe(it) } ?: "—",
                        color = MaGreen
                    )
                    Spacer(Modifier.height(10.dp))
                    MaThinBar(
                        caption = "تعداد مشتریان همگام‌شده روی گوشی",
                        fraction = if (customers.isEmpty()) 0f else 1f,
                        valueText = "${customers.size.toFaNumber()} مشتری",
                        color = p.gold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (briefing == null) "برای دیدن سهمیه، اتصال به سرور لازم است."
                        else if (briefing?.liveWriteReady == true)
                            "مسیر ثبت واقعی سند در آتیران آماده است ✅"
                        else "پیش‌نمایش امن فعال است — " + (briefing?.liveWriteReason ?: "پیش‌فاکتور مرجع در ERP یافت نشد"),
                        fontSize = 10.5.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary
                    )
                }
            }

            // ═══ ۹) امضای دیجیتال مشتری ═══
            item {
                GlassCard(modifier = contentMod.royalBorder()) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoyalHeader(text = "امضای دیجیتال مشتری", icon = Icons.Filled.Draw)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = {
                                signaturePaths.clear(); hasSignature = false
                            }) {
                                Text("پاک کردن", color = MaRed)
                            }
                        }
                        SignaturePad(
                            paths = signaturePaths,
                            onDrawn = { hasSignature = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSlateDeep)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (hasSignature) "امضا دریافت شد ✅" else "مشتری با انگشت روی کادر بالا امضا می‌کند",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasSignature) NeonGreen else TextSecondary
                        )
                    }
                }
            }

            // ═══ ۱۰) کلید ثبت واقعی + دکمهٔ ارسال ═══
            item {
                Column(
                    modifier = contentMod
                        .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
                        .padding(13.dp)
                ) {
                    ToggleRow(
                        label = "ثبت واقعی در آتیران",
                        hint = "روشن = سند در ERP نوشته می‌شود | خاموش = فقط پیش‌نمایش",
                        checked = liveWrite,
                        onCheckedChange = { viewModel.setLiveInvoiceWrite(it) },
                        icon = Icons.Filled.Send
                    )
                    Spacer(Modifier.height(10.dp))
                    MaGoldCta(
                        title = if (items.isEmpty()) "سبد خالی است" else "ارسال پیش‌فاکتور به آتیران",
                        subtitle = if (liveWrite)
                            "ثبت واقعی سند در ERP آتیران — پس از تأیید، شمارهٔ سند برگردانده می‌شود"
                        else "پیش‌نمایش امن سند — هیچ چیزی در سرور نوشته نمی‌شود",
                        icon = Icons.Filled.Send,
                        badge = if (liveWrite) "LIVE" else "SAFE",
                        enabled = items.isNotEmpty(),
                        onClick = {
                            val png = if (hasSignature) renderSignature(signaturePaths) else null
                            viewModel.issueInvoice(png, false, note) { invoice ->
                                signaturePaths.clear()
                                hasSignature = false
                                note = ""
                                goldBurst = true
                                viewModel.showToast("فاکتور ${invoice.id.toFaNumber()} صادر شد ✅")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        GlamourButton(
                            label = "ثبت محلی بدون ارسال به سرور",
                            subtitle = "ذخیره در گوشی برای ارسال بعدی",
                            icon = Icons.Filled.ShoppingCart,
                            tone = GlamourTone.GLASS,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val png = if (hasSignature) renderSignature(signaturePaths) else null
                                viewModel.issueInvoice(png, false, note) { invoice ->
                                    signaturePaths.clear()
                                    hasSignature = false
                                    goldBurst = true
                                    viewModel.showToast("پیش‌فاکتور ${invoice.id.toFaNumber()} در گوشی ذخیره شد")
                                }
                            }
                        )
                    }
                }
            }

            // ═══ ۱۱) کارت نتیجهٔ سند ═══
            outcome?.let { o ->
                item {
                    Column(
                        modifier = contentMod
                            .metalPanel(RoundedCornerShape(20.dp), strong = true, corner = 20f)
                            .padding(13.dp)
                    ) {
                        MaSectionHeader(
                            title = if (o.ok) "نتیجهٔ ثبت سند در آتیران" else "نتیجهٔ فراخوانی سند",
                            icon = if (o.ok) Icons.Filled.CheckCircle else Icons.Filled.Warning
                        )
                        Spacer(Modifier.height(9.dp))
                        OutcomeRow("حالت", if (o.live) "ثبت واقعی (LIVE)" else "پیش‌نمایش امن", if (o.live) MaGreen else MaAmber)
                        OutcomeRow("شمارهٔ سند (shfacfo)", o.shfacfo?.toFaNumber() ?: "—", p.gold)
                        OutcomeRow("اقلام نوشته‌شده", o.linesWritten.toFaNumber(), MaGreen)
                        OutcomeRow("پیام سرویس", o.message, p.textPrimary)
                        if (o.preview != null) {
                            Spacer(Modifier.height(9.dp))
                            GlamourButton(
                                label = "نمایش متن پیش‌نمایش سند",
                                icon = Icons.Filled.ReceiptLong,
                                tone = GlamourTone.GLASS,
                                height = 46.dp,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { previewOpen = true }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        GlamourButton(
                            label = "پاک کردن این کارت",
                            icon = Icons.Filled.Delete,
                            tone = GlamourTone.GLASS,
                            height = 44.dp,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.clearInvoiceOutcome() }
                        )
                    }
                }
            }

            // ═══ ۱۱/۲) دستیار هوشمند مکمل (همان دستیار پیشنهاد کالا) ═══
            item {
                Column(
                    modifier = contentMod
                        .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
                        .padding(13.dp)
                ) {
                    MaSectionHeader(
                        title = "دستیار هوشمند فروش",
                        icon = Icons.Filled.AutoAwesome
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        "با توجه به مشتری انتخاب‌شده و سابقهٔ خرید او، کالاهای مکمل پیشنهاد می‌شود. " +
                            "اگر اینترنت/پراکسی در دسترس نباشد، پیشنهاد کلاسیک داخلی نمایش داده می‌شود.",
                        fontSize = 10.5.sp,
                        lineHeight = 16.sp,
                        color = TextSecondary
                    )
                    aiSuggestion?.let { text ->
                        Spacer(Modifier.height(9.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(p.gold.copy(alpha = 0.09f))
                                .border(1.dp, p.gold.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 9.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = p.textPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    GlamourButton(
                        label = "پیشنهاد کالاهای مکمل",
                        subtitle = "برای مشتری «${selectedCustomer?.name ?: "مشتری متفرقه"}»",
                        icon = Icons.Filled.AutoAwesome,
                        tone = GlamourTone.GOLD,
                        height = 50.dp,
                        enabled = !aiLoading,
                        loading = aiLoading,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.askAiAssistant() }
                    )
                }
            }

            // ═══ ۱۲) کاشی‌های خروجی واقعی ═══
            item {
                MaToolGrid(
                    title = "خروجی سند",
                    columns = 5,
                    tools = listOf(
                        MaToolTile("اشتراک", Icons.Filled.Share, p.gold) {
                            lastInvoice?.let { inv ->
                                viewModel.invoiceItems(inv.id) { lines ->
                                    runCatching { InvoiceShare.shareText(context, inv, lines) }
                                }
                            } ?: viewModel.showToast("ابتدا یک فاکتور صادر کنید")
                        },
                        MaToolTile("تصویر", Icons.Filled.Draw, MaGreen) {
                            lastInvoice?.let { inv ->
                                viewModel.invoiceItems(inv.id) { lines ->
                                    runCatching { InvoiceShare.shareImage(context, inv, lines) }
                                }
                            } ?: viewModel.showToast("ابتدا یک فاکتور صادر کنید")
                        },
                        MaToolTile("PDF", Icons.Filled.ReceiptLong, MaRed) {
                            lastInvoice?.let { inv ->
                                viewModel.invoiceItems(inv.id) { lines ->
                                    runCatching { InvoiceShare.sharePdf(context, inv, lines) }
                                }
                            } ?: viewModel.showToast("ابتدا یک فاکتور صادر کنید")
                        },
                        MaToolTile("Word", Icons.Filled.Receipt, NeonPurple) {
                            lastInvoice?.let { inv ->
                                viewModel.invoiceItems(inv.id) { lines ->
                                    runCatching { InvoiceShare.shareWord(context, inv, lines) }
                                }
                            } ?: viewModel.showToast("ابتدا یک فاکتور صادر کنید")
                        },
                        MaToolTile("گزارش‌ها", Icons.Filled.Analytics, p.accentText) { onOpenReports() },
                    ),
                    modifier = contentMod
                )
            }

            // ═══ ۱۳) کارت سند MEELANO ═══
            item {
                MaDocCard(
                    title = "پیش‌فاکتور فروش آتیران",
                    subtitle = "دوره: همین سبد سفارش • مسیر: اتصال مستقیم SQL Server",
                    rows = listOf(
                        "مشتری" to (selectedCustomer?.name ?: "مشتری متفرقه"),
                        "کد مشتری" to (selectedCustomer?.let {
                            if (it.id == 0) "—" else it.code
                        } ?: "—"),
                        "تعداد اقلام" to linesCount.toFaNumber(),
                        "جمع اقلام" to gross.toFaPrice(),
                        "مبلغ نهایی" to gross.toFaPrice(),
                        "وضعیت مسیر سند" to if (briefing?.liveWriteReady == true) "آمادهٔ ثبت واقعی" else "پیش‌نمایش امن",
                        "امضای مشتری" to if (hasSignature) "ثبت شده" else "ثبت نشده",
                    ),
                    note = note.ifBlank {
                        if (lowStock > 0) "$lowStock قلم موجودی کمتر از سفارش دارد — قبل از ارسال بررسی کنید."
                        else "مبلغ نهایی برابر جمع اقلام است؛ کسورات در این نسخه اعمال نمی‌شود."
                    },
                    modifier = contentMod
                )
            }

            item {
                Row(
                    modifier = contentMod,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlamourButton(
                        label = "همگام‌سازی داده",
                        icon = Icons.Filled.Sync,
                        tone = GlamourTone.GLASS,
                        height = 48.dp,
                        enabled = !briefingBusy,
                        loading = briefingBusy,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.syncNow() }
                    )
                    GlamourButton(
                        label = "مشتریان",
                        icon = Icons.Filled.Person,
                        tone = GlamourTone.GLASS,
                        height = 48.dp,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenCustomers
                    )
                }
            }

            item { MilanoFooter(modifier = contentMod) }
        }

        GoldBurstOverlay(active = goldBurst) { goldBurst = false }
    }

    // ═══ دیالوگ انتخاب مشتری ═══
    if (showPicker) {
        CustomerPickerDialog(
            customers = customers.filter { !it.pendingApproval },
            onDismiss = { showPicker = false },
            onPick = {
                viewModel.selectCustomer(it)
                showPicker = false
            }
        )
    }

    // ═══ دیالوگ پیش‌نمایش متن سند ═══
    if (previewOpen) {
        val text = outcome?.preview?.asText() ?: "پیش‌نمایشی موجود نیست."
        Dialog(onDismissRequest = { previewOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(RoyalSurfaceBrush)
                    .royalBorder(RoundedCornerShape(24.dp))
                    .padding(15.dp)
            ) {
                RoyalHeader(text = "پیش‌نمایش سند آتیران", icon = Icons.Filled.ReceiptLong)
                Spacer(Modifier.height(10.dp))
                Text(
                    text,
                    fontSize = 11.sp,
                    lineHeight = 18.sp,
                    color = p.textPrimary,
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(12.dp))
                GlamourButton(
                    label = "بستن",
                    icon = Icons.Filled.Delete,
                    tone = GlamourTone.GLASS,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { previewOpen = false }
                )
            }
        }
    }
}

// ═══════════════════════════ اجزای کمکی ═══════════════════════════

/** یک ردیف نتیجه در کارت نتیجهٔ سند (برچسب — مقدار رنگ‌دار). */
@Composable
private fun OutcomeRow(label: String, value: String, tint: Color) {
    val p = vizitorPalette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            fontSize = 11.5.sp,
            color = TextSecondary,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            modifier = Modifier.weight(1f)
        )
    }
}

/** قالب‌بندی مبلغ به تومان با امنیت نوع (Long همیشه). */
private fun toFaPriceSafe(value: Long): String = value.toFaPrice()

@Composable
private fun CartLine(
    item: CartItemEntity,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
    onSetQty: (Double) -> Unit
) {
    val p = vizitorPalette
    val enough = item.stock >= item.quantity
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(20.dp), corner = 20f)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(p.gold.copy(alpha = 0.22f), p.surfaceDeep.copy(alpha = 0.8f))
                        )
                    )
                    .border(1.dp, p.gold.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = p.gold,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1.25f)) {
                Text(
                    item.productName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = p.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "قیمت واحد: ${item.unitPrice.toFaPrice()} • موجودی: ${item.stock.toFaNumber()}",
                    fontSize = 10.sp,
                    color = if (enough) TextSecondary else MaRed
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(p.gold.copy(alpha = 0.18f)),
                colors = IconButtonDefaults.iconButtonColors(contentColor = p.gold)
            ) { Icon(Icons.Filled.Remove, contentDescription = "کمتر", modifier = Modifier.size(16.dp)) }
            var qtyText by remember(item.quantity) { mutableStateOf(item.quantity.toFaNumber()) }
            OutlinedTextField(
                value = qtyText,
                onValueChange = { v ->
                    qtyText = v
                    v.parseAmount()?.let { q -> onSetQty(q) }
                },
                modifier = Modifier
                    .width(74.dp)
                    .padding(horizontal = 5.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleSmall.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.ExtraBold
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.gold,
                    unfocusedBorderColor = p.gold.copy(alpha = 0.25f),
                    cursorColor = p.gold
                )
            )
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (enough) MaGreen.copy(alpha = 0.85f) else MaRed.copy(alpha = 0.5f)),
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Black)
            ) { Icon(Icons.Filled.Add, contentDescription = "بیشتر", modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "مبلغ قلم",
                    fontSize = 9.5.sp,
                    color = TextSecondary
                )
                Text(
                    (item.quantity.toLong() * item.unitPrice).toFaPrice(),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = MaGreen
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaRed.copy(alpha = 0.15f),
                    contentColor = MaRed
                )
            ) { Icon(Icons.Filled.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp)) }
        }
    }
}

/**
 * پنل لمسی اخذ امضای دیجیتال — رسم مسیر با انگشت روی Canvas.
 */
@Composable
private fun SignaturePad(
    paths: androidx.compose.runtime.snapshots.SnapshotStateList<Path>,
    onDrawn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val inkColor = NeonGreen
    val guideColor = vizitorPalette.glassBorder

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val path = Path().apply { moveTo(offset.x, offset.y) }
                    currentPath = path
                },
                onDrag = { change, _ ->
                    change.consume()
                    currentPath?.lineTo(change.position.x, change.position.y)
                },
                onDragEnd = {
                    currentPath?.let {
                        paths.add(it)
                        onDrawn()
                    }
                    currentPath = null
                }
            )
        }
    ) {
        drawLine(
            color = guideColor,
            start = Offset(size.width * 0.1f, size.height * 0.75f),
            end = Offset(size.width * 0.9f, size.height * 0.75f),
            strokeWidth = 1.dp.toPx()
        )
        paths.forEach { path ->
            drawPath(path = path, color = inkColor, style = Stroke(width = 3.dp.toPx()))
        }
        currentPath?.let {
            drawPath(path = it, color = inkColor, style = Stroke(width = 3.dp.toPx()))
        }
    }
}

/** رستر کردن امضا به PNG برای ذخیره در دیتابیس و ارسال به سرور. */
private fun renderSignature(
    paths: androidx.compose.runtime.snapshots.SnapshotStateList<Path>,
    width: Int = 800,
    height: Int = 300
): ByteArray? {
    if (paths.isEmpty()) return null
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.argb(255, 43, 255, 136)
        strokeWidth = 6f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    paths.forEach { uiPath ->
        canvas.drawPath(uiPath.asAndroidPath(), paint)
    }
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
    bitmap.recycle()
    return out.toByteArray()
}

/**
 * دیالوگ جست‌وجوی مشتری برای صدور فاکتور — جست‌وجوی زنده بر اساس نام/کد/شهر،
 * آواتار مینیاتوری، گروه و شهر مشتری + گزینه «مشتری متفرقه» در بالای فهرست.
 */
@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onPick: (CustomerEntity?) -> Unit
) {
    var q by remember { mutableStateOf("") }
    val filtered = remember(customers, q) {
        if (q.isBlank()) customers
        else customers.filter {
            it.name.contains(q) || it.code.contains(q) || it.city.contains(q)
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(RoyalSurfaceBrush)
                .royalBorder(RoundedCornerShape(24.dp))
        ) {
            Column(Modifier.padding(16.dp)) {
                RoyalHeader(text = "انتخاب مشتری فاکتور", icon = Icons.Filled.Person)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x14FFFFFF))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Search, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    androidx.compose.material3.TextField(
                        value = q,
                        onValueChange = { q = it },
                        placeholder = { Text("جست‌وجوی مشتری (نام/کد/شهر)…", color = TextSecondary) },
                        singleLine = true,
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = vizitorPalette.gold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.height(300.dp)) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            PickerRow(
                                title = "مشتری متفرقه (بدون ثبت در آتیران)",
                                subtitle = "فاکتور بدون کد مشتری ثبت می‌شود",
                                initial = "●",
                                tint = vizitorPalette.gold,
                                onClick = { onPick(null) }
                            )
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    "مشتری‌ای مطابق جست‌وجو یافت نشد ❌",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        items(filtered, key = { it.id }) { c ->
                            PickerRow(
                                title = c.name,
                                subtitle = "گروه ${c.groupName} • ${c.city} • کد ${c.code}" +
                                    if (c.debt > 0) " • بدهی ${c.debt.toFaPrice()}" else "",
                                initial = c.name.firstOrNull()?.toString() ?: "؟",
                                tint = vizitorPalette.gold,
                                onClick = { onPick(c) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                GlamourButton(
                    label = "بستن",
                    icon = Icons.Filled.Delete,
                    tone = GlamourTone.GLASS,
                    height = 46.dp,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                )
            }
        }
    }
}

/** سطر مشتری در دیالوگ انتخابگر — آواتار کوچک + عنوان و زیرعنوان + کلیک انتخاب. */
@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    initial: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x0DFFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.18f))
                .border(1.dp, tint.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initial,
                color = tint,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = vizitorPalette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
