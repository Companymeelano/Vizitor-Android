/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تب ۲: ویترین کالا (3D Catalog) v2.4.0
 *  نمایش تکیه‌رویی (تک‌ستونه) با کارت پهن: صحنه بزرگ‌تر + مشخصات کریستالی
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  کارت‌های شیشه‌ای با افکت سه‌بعدی، دکمه‌های شناور + و -،
 *  موجودی زنده (Live Stock) و اسکنر بارکد دوربین
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import ir.atiran.vizitor.data.local.CustomerEntity
import ir.atiran.vizitor.ui.components.RoyalHeader
import ir.atiran.vizitor.ui.components.RoyalSurfaceBrush
import ir.atiran.vizitor.ui.components.auroraFrame
import ir.atiran.vizitor.ui.components.royalBorder
import ir.atiran.vizitor.ui.theme.NeonPurpleDark
import ir.atiran.vizitor.util.parseAmount
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Inventory2
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.VizitorViewModel
import ir.atiran.vizitor.data.local.ProductEntity
import ir.atiran.vizitor.perf.VizitorPerf
import ir.atiran.vizitor.ui.components.GlassCard
import ir.atiran.vizitor.ui.components.NeonGreenButton
import ir.atiran.vizitor.ui.components.MicButton
import ir.atiran.vizitor.ui.components.MilanoFooter
import ir.atiran.vizitor.ui.components.ShimmerGoldText
import ir.atiran.vizitor.ui.components.goldBorder
import ir.atiran.vizitor.ui.components.rememberVoiceSearch
import ir.atiran.vizitor.ui.components.tilt3D
import ir.atiran.vizitor.ui.theme.AccentText
import ir.atiran.vizitor.ui.theme.DangerRed
import ir.atiran.vizitor.ui.theme.Gold
import ir.atiran.vizitor.ui.theme.GoldDark
import ir.atiran.vizitor.ui.theme.NeonGreen
import ir.atiran.vizitor.ui.theme.NeonPurple
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

@Composable
fun CatalogScreen(
    viewModel: VizitorViewModel,
    onOpenScanner: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    var query by remember { mutableStateOf("") }
    var zoomProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var addProduct by remember { mutableStateOf<ProductEntity?>(null) }
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()

    // آخرین قیمت فروش کالای انتخابی به مشتری انتخابی
    var lastPrice by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(addProduct) {
        lastPrice = null
        val pr = addProduct
        val cu = selectedCustomer
        if (pr != null && cu != null) {
            viewModel.lastSalePrice(cu.id, pr.id) { lastPrice = it }
        }
    }
    val startVoice = rememberVoiceSearch(
        onResult = { query = it; viewModel.showToast("جستجوی صوتی: «$it»") },
        onUnavailable = { viewModel.showToast("ورودی صوتی روی این دستگاه در دسترس نیست 🎙️") }
    )
    val filtered = remember(products, query) {
        if (query.isBlank()) products
        else products.filter { it.name.contains(query) || it.code.contains(query) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    ShimmerGoldText("ویترین کالا")
                    Text(
                        "کاتالوگ زنده با موجودی لحظه‌ای — نمایش تک‌ردیفه واضح",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { SearchField(query) { query = it } }
                        Spacer(Modifier.width(8.dp))
                        MicButton(onClick = { startVoice() })
                    }
                }
            }

            items(filtered, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    qtyInCart = remember(cartItems) { cartItems.firstOrNull { it.productId == product.id }?.quantity ?: 0.0 },
                    onAdd = { addProduct = product },
                    onRemove = { viewModel.decrement(product.id) },
                    onZoom = { zoomProduct = product }
                )
            }

            item { MilanoFooter() }
        }

        // ── دیالوگ بزرگنمایی تصویر کالا ────────────────────────────────────
        zoomProduct?.let { p ->
            ProductZoomDialog(p) { zoomProduct = null }
        }

        // ── دیالوگ افزودن هوشمند به سبد (تعداد دستی + انتخاب قیمت) ─────────
        addProduct?.let { pr ->
            AddToCartDialog(
                product = pr,
                customer = selectedCustomer,
                initialLevel = viewModel.priceLevelFor(selectedCustomer),
                lastPrice = lastPrice,
                onSaveVisitorLevel = { viewModel.setDefaultPriceLevel(it) },
                onConfirm = { qty, price ->
                    viewModel.addToCart(pr, qty, price)
                    viewModel.showToast(
                        "«${pr.name}» × ${qty.toFaNumber()} با قیمت انتخابی به سبد اضافه شد ✅"
                    )
                    addProduct = null
                },
                onDismiss = { addProduct = null }
            )
        }

        // ── دکمه فعال‌سازی دوربین — اسکنر بارکد ─────────────────────────────
        androidx.compose.material3.FloatingActionButton(
            onClick = onOpenScanner,
            containerColor = NeonPurple,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = "اسکنر بارکد")
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onChange,
            placeholder = { Text("جستجوی نام یا بارکد کالا…", color = TextSecondary) },
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
 * کارت کالای v2.4.0 — چیدمان پهن تک‌ردیفه:
 * صحنه نمایش هاله‌دار ۱۱۶dp کنار بدنه مشخصات کریستالی (چیپ‌های موجودی/واحد/بسته)
 * + پنل قیمت‌های سه‌بعدی کامل + استپر زنده با نشانگر «در سبد: N».
 */
@Composable
private fun ProductCard(
    product: ProductEntity,
    qtyInCart: Double,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onZoom: () -> Unit
) {
    val inStock = product.stock > 0
    val lowStock = product.stock in 0.0..10.0
    val stockColor = if (inStock) if (lowStock) Gold else NeonGreen else DangerRed
    // رنگ‌های تم — صحنه کالا با رنگ اصلی پالت فعال کاربر رنگ می‌گیرد
    val p = vizitorPalette

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .tilt3D(maxTilt = 7f, enabled = VizitorPerf.listFx)
            .then(if (product.isVip) Modifier.goldBorder() else Modifier),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // ═══ صحنه نمایش کالا — مربع بزرگ، گرادیان تم + هاله طلایی ═══
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(p.halo1.copy(alpha = 0.9f), Color(0x09FFFFFF))
                        )
                    )
                    .clickable(onClick = onZoom)
                    .auroraFrame(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(p.gold.copy(alpha = 0.24f), Color.Transparent))
                        )
                )
                Text(product.imageEmoji, fontSize = 52.sp, textAlign = TextAlign.Center)
                // نشان موجودی (داخل صحنه، بالا-انتها)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(stockColor)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            !inStock -> "ناموجود"
                            lowStock -> "کم: ${product.stock.toFaNumber()}"
                            else -> "${product.stock.toFaNumber()}"
                        },
                        color = Color(0xFF0B1220),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                // نشان VIP (داخل صحنه، بالا-ابتدا)
                if (product.isVip) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFFFD166))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFF4A3400),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "VIP",
                            color = Color(0xFF4A3400),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                // نشان بزرگنمایی (داخل صحنه، پایین-انتها)
                Icon(
                    Icons.Filled.ZoomIn,
                    contentDescription = "بزرگنمایی",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier
                        .size(15.dp)
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            // ═══ بدنه مشخصات کریستالی ═══
            Column(modifier = Modifier.weight(1f)) {
                // نام کالا — بزرگ و خوانا
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.2.sp,
                        lineHeight = 21.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${product.groupName} | کد: ${product.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(5.dp))

                // چیپ‌های مشخصات واضح: موجودی/واحد/بسته
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    SpecChip(
                        text = if (inStock) "موجودی: ${product.stock.toFaNumber()}" else "ناموجود",
                        tint = stockColor
                    )
                    SpecChip(text = "واحد: ${product.unit}", tint = NeonPurple)
                    SpecChip(text = "بسته: ${product.packSize.toFaNumber()}", tint = TextSecondary)
                }

                Spacer(Modifier.height(6.dp))

                // ═══ پنل قیمت‌های سه‌بعدی ═══
                PricePanel(product)

                Spacer(Modifier.height(6.dp))

                // ═══ استپر زنده با نشانگر در سبد ═══
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRemove,
                        enabled = inStock,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, NeonPurple.copy(alpha = 0.45f), CircleShape)
                            .background(NeonPurple.copy(alpha = 0.22f)),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = NeonPurple)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "کاهش", modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (qtyInCart > 0) Gold.copy(alpha = 0.14f) else Color(0x0FFFFFFF))
                            .border(
                                1.dp,
                                if (qtyInCart > 0) Gold.copy(alpha = 0.45f) else Color(0x22FFFFFF),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (qtyInCart > 0) "در سبد: ${qtyInCart.toFaNumber()}" else "·",
                            fontSize = if (qtyInCart > 0) 10.5.sp else 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (qtyInCart > 0) Gold else TextSecondary,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onAdd,
                        enabled = inStock,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF8CFFCB), NeonGreen))
                            )
                            .border(1.dp, Color(0x8CFFFFFF), CircleShape),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF0B3520))
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "افزودن به سبد", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/** چیپ مشخصات کوچک واضح — رنگ طبق معنا (تکذیب/کیفی/خنثی). */
@Composable
private fun SpecChip(text: String, tint: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.10f))
            .border(1.dp, tint.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * پنل قیمت‌های کالا — فروش ۱ و فروش ۲ در دو سکّه سه‌بعدی کنار هم
 * و قیمت مصرف‌کننده به‌صورت نوار قهرمان با قاب طلایی در انتهای پنل.
 */
@Composable
private fun PricePanel(product: ProductEntity) {
    val sale1 = product.price
    val sale2 = if (product.price2 > 0) product.price2 else product.price
    val consumer = if (product.consumerPrice > 0) product.consumerPrice else product.price
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x0DFFFFFF))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Sell,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "قیمت‌های کالا (ریال)",
                style = MaterialTheme.typography.labelSmall,
                color = Gold,
                modifier = Modifier.weight(1f)
            )
            // چیپ هوشمند «سود مشتری» — حاشیه قیمت مصرف‌کننده تا فروش ۱
            if (sale1 > 0 && consumer > sale1) {
                val margin = ((consumer - sale1) * 100 / sale1).toInt()
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(NeonGreen.copy(alpha = 0.13f))
                        .border(1.dp, NeonGreen.copy(alpha = 0.38f), RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "سود مشتری: ${margin.toFaNumber()}٪+",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonGreen,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PriceCoin(
                label = "فروش ۱",
                price = sale1,
                face = listOf(Color(0xFF8CFFCB), Color(0xFF17D877)),
                edge = Color(0xFF0B7A44),
                textColor = Color(0xFF0A3521),
                modifier = Modifier.weight(1f)
            )
            PriceCoin(
                label = "فروش ۲",
                price = sale2,
                face = listOf(Color(0xFFFFE29A), Color(0xFFF0B23C)),
                edge = Color(0xFF8F6414),
                textColor = Color(0xFF4A3400),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(7.dp))
        // قیمت مصرف‌کننده — قهرمان پنل با قاب طلایی
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(y = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2C0B4E))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(NeonPurple, NeonPurpleDark)))
                    .border(1.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "قیمت مصرف‌کننده",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    consumer.toFaPrice(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFE29A)
                )
            }
        }
    }
}

/** سکّه قیمت سه‌بعدی — لبه عمق زیرین + رویه گرادیانی + برچسب و مبلغ. */
@Composable
private fun PriceCoin(
    label: String,
    price: Long,
    face: List<Color>,
    edge: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(edge.copy(alpha = 0.8f))
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(face))
                .border(1.dp, Color(0x38FFFFFF), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Text(
                label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.8f)
            )
            Text(
                price.toFaPrice(),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * دیالوگ بزرگنمایی کالا — تصویر بزرگ و مشخصات کامل (واحد/بسته/قیمت‌ها).
 */
@Composable
private fun ProductZoomDialog(product: ProductEntity, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(ir.atiran.vizitor.ui.theme.DarkSlateElevated)
                .goldBorder(RoundedCornerShape(28.dp))
        ) {
            Column(Modifier.padding(18.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x33B04BF8), Color(0x0AFFFFFF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(product.imageEmoji, fontSize = 110.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(product.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${product.groupName} | بارکد: ${product.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("واحد شمارش: ${product.unit}", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text("تعداد در بسته: ${product.packSize.toFaNumber()}",
                        style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("قیمت فروش ۱: ${product.price.toFaPrice()}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall.copy(color = NeonGreen, fontWeight = FontWeight.ExtraBold))
                    Text("قیمت فروش ۲: ${(if (product.price2 > 0) product.price2 else product.price).toFaPrice()}",
                        style = MaterialTheme.typography.titleSmall.copy(color = Gold, fontWeight = FontWeight.ExtraBold))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "قیمت مصرف‌کننده: ${(if (product.consumerPrice > 0) product.consumerPrice else product.price).toFaPrice()}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = AccentText,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (product.stock > 0) "موجودی زنده: ${product.stock.toFaNumber()} ${product.unit}" else "ناموجود",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stock > 0) NeonGreen else DangerRed
                )
                Spacer(Modifier.height(12.dp))
                NeonGreenButton(text = "بستن", onClick = onDismiss, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** چیپ انتخاب سطح قیمت (فروش ۱ / فروش ۲) با ظاهر سلطنتی بنفش. */
@Composable
private fun PriceChip(
    label: String,
    price: Long,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) Modifier.background(Brush.linearGradient(listOf(NeonPurple, NeonPurpleDark)))
                else Modifier.background(Color(0x14FFFFFF))
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) Gold else Color(0x33FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = if (!enabled) Color(0xFF5A6270) else if (selected) Color.White else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                price.toFaPrice(),
                color = if (!enabled) Color(0xFF5A6270) else if (selected) Gold else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * دیالوگ افزودن هوشمند به سبد:
 *  — ورود دستی تعداد (تایپ عدد) + دکمه‌های ±
 *  — انتخاب قیمت فروش ۱ یا ۲ با پیش‌فرض هوشمند (گروه مشتری / تنظیم ویزیتور)
 *  — نمایش آخرین قیمت فروش به مشتری انتخاب‌شده و امکان اعمال آن
 *  — ویرایش دستی قیمت نهایی
 */
@Composable
private fun AddToCartDialog(
    product: ProductEntity,
    customer: CustomerEntity?,
    initialLevel: Int,
    lastPrice: Long?,
    onSaveVisitorLevel: (Int) -> Unit,
    onConfirm: (Double, Long) -> Unit,
    onDismiss: () -> Unit
) {
    var qtyText by remember { mutableStateOf("0") }
    var lockedQty by remember { mutableStateOf(0.0) }
    var level by remember { mutableStateOf(initialLevel) }
    var manual by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf("") }

    val hasPrice2 = product.price2 > 0
    val levelPrice = if (level == 2 && hasPrice2) product.price2 else product.price
    val qty = qtyText.parseAmount() ?: 0.0
    val effectiveQty = if (lockedQty > 0) lockedQty else qty
    val finalPrice: Long = if (manual) (manualText.parseAmount()?.toLong() ?: levelPrice) else levelPrice

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(RoyalSurfaceBrush)
                .royalBorder(RoundedCornerShape(26.dp))
        ) {
            Column(Modifier.padding(18.dp)) {
                RoyalHeader(text = "افزودن به سبد فروش", icon = Icons.Filled.Add)
                Spacer(Modifier.height(10.dp))
                Text(
                    "${product.imageEmoji} ${product.name}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "واحد: ${product.unit} • هر بسته: ${product.packSize.toFaNumber()} عدد • موجودی: ${product.stock.toFaNumber()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                Spacer(Modifier.height(12.dp))
                Text("تعداد / مقدار:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            qtyText = ((qty + 1).coerceAtMost(product.stock)).toLong().toString()
                            lockedQty = 0.0
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = NeonPurple.copy(alpha = 0.25f),
                            contentColor = NeonPurple
                        )
                    ) { Icon(Icons.Filled.Add, contentDescription = "بیشتر") }
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = {
                            qtyText = it
                            lockedQty = 0.0
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            cursorColor = NeonPurple
                        )
                    )
                    IconButton(
                        onClick = {
                            qtyText = (qty - 1).coerceAtLeast(0.0).toLong().toString()
                            lockedQty = 0.0
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color(0x1AFFFFFF),
                            contentColor = TextSecondary
                        )
                    ) { Icon(Icons.Filled.Remove, contentDescription = "کمتر") }
                }
                Spacer(Modifier.height(6.dp))
                // درج سریع بر اساس بسته‌بندی + دکمه درج تعداد
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PackChip("۱ بسته (${product.packSize.toFaNumber()})") {
                        qtyText = product.packSize.toString()
                        lockedQty = product.packSize.toDouble()
                    }
                    PackChip("۲ بسته") {
                        val v = product.packSize * 2
                        qtyText = v.toString()
                        lockedQty = v.toDouble()
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (qty > 0) Modifier.background(Brush.linearGradient(listOf(Gold, GoldDark)))
                                else Modifier.background(Color(0x14FFFFFF))
                            )
                            .clickable(enabled = qty > 0) {
                                lockedQty = qty.coerceAtMost(product.stock)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            if (lockedQty > 0) "تعداد درج شد ✓" else "درج تعداد",
                            color = if (qty > 0) Color.Black else TextSecondary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("سطح قیمت:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Row {
                    PriceChip(
                        label = "قیمت فروش ۱",
                        price = product.price,
                        selected = level == 1,
                        enabled = true
                    ) { level = 1; manual = false }
                    PriceChip(
                        label = "قیمت فروش ۲",
                        price = if (hasPrice2) product.price2 else product.price,
                        selected = level == 2,
                        enabled = hasPrice2
                    ) { level = 2; manual = false }
                }
                if (!hasPrice2) {
                    Text(
                        "این کالا قیمت فروش ۲ ندارد؛ همان فروش ۱ محاسبه می‌شود.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                TextButton(onClick = { onSaveVisitorLevel(level) }) {
                    Text(
                        "ذخیره «فروش $level» به‌عنوان پیش‌فرض همیشگی من",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple
                    )
                }

                if (lastPrice != null && customer != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Gold.copy(alpha = 0.10f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "آخرین قیمت فروش به ${customer.name}:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                lastPrice.toFaPrice(),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Gold
                                )
                            )
                        }
                        TextButton(onClick = { manual = true; manualText = lastPrice.toString() }) {
                            Text("اعمال همین قیمت", color = Gold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ویرایش دستی قیمت واحد", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (manual) "قیمت دلخواه فعال است" else "بر اساس سطح انتخابی بالا",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = manual,
                        onCheckedChange = {
                            manual = it
                            if (it && manualText.isBlank()) manualText = levelPrice.toString()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeonPurple
                        )
                    )
                }
                if (manual) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = manualText,
                        onValueChange = { manualText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("قیمت دلخواه هر واحد (ریال)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold,
                            cursorColor = Gold,
                            focusedLabelColor = Gold
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "قیمت نهایی هر واحد:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        finalPrice.toFaPrice(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonGreen
                        )
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "جمع این قلم:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        (finalPrice.toBigDecimal() * effectiveQty.toBigDecimal()).toLong().toFaPrice(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold
                        )
                    )
                }

                Spacer(Modifier.height(14.dp))
                NeonGreenButton(
                    text = if (lockedQty > 0) "افزودن ${lockedQty.toFaNumber()} عدد به سبد 🛒" else "افزودن به سبد 🛒",
                    onClick = { if (lockedQty > 0) onConfirm(lockedQty.coerceAtMost(product.stock), finalPrice) },
                    enabled = lockedQty > 0,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("انصراف", color = TextSecondary)
                }
            }
        }
    }
}

/** چیپ درج سریع تعداد بر اساس بسته‌بندی کالا. */
@Composable
private fun PackChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NeonPurple.copy(alpha = 0.18f))
            .border(1.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, color = AccentText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
