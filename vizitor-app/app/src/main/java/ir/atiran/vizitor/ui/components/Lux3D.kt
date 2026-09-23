/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | کیت گرافیکی سه‌بعدی لوکس (v2.18.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  خواستهٔ کارفرما: «منوها و چارت‌ها و جداول کاملاً سه‌بعدی».
 *
 *  این فایل همان را با هندسهٔ واقعی رسم می‌کند (نه فقط سایه و گرادیان):
 *    ▸ Lux3DStage   — صحنهٔ سه‌بعدی: کفِ پرسپکتیوی + نور طلایی + محو لبه‌ها
 *    ▸ lux3dPanel   — وجه + ضخامت (لبهٔ زیرین) + خط نور بالای وجه
 *    ▸ Lux3DTile/Menu — منوی سه‌بعدی: کاشی‌های برجسته با چرخش هنگام لمس
 *    ▸ Lux3DBarChart — ستون‌های ایزومتریک: وجه روبرو + وجه بالا + وجه کنار +
 *                      سایه و بازتاب کف
 *    ▸ Lux3DDonut  — دونات ضخیم سه‌بعدی (لبهٔ تیره + وجه روشن + نگین)
 *    ▸ Lux3DTable  — جدول با سربرگ طلایی برجسته و ردیف‌های ضخامت‌دار
 *    ▸ Lux3DStatOrb — گویِ آماره (کره با نور نقطه‌ای و سایهٔ پایه)
 *
 *  همه با Canvas خودِ Compose — بدون هیچ کتابخانهٔ بیرونی و بدون سنگین‌شدن APK.
 *  اندازه‌ها همه از پالت تم می‌آیند، پس با هر تم (طلایی پیش‌فرض یا تم‌های قبلی)
 *  هماهنگ می‌مانند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaNumber
import ir.atiran.vizitor.util.toFaPrice

// ═══════════════════════════ ۱) مدل‌های داده ═══════════════════════════

/** یک قلم منوی سه‌بعدی. */
data class Lux3DItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String? = null,
    val accent: Color? = null,
)

// ═══════════════════════════ ۲) صحنهٔ سه‌بعدی ═══════════════════════════

/**
 * صحنهٔ سه‌بعدی: کفِ پرسپکتیوی با خطوط گریز + نور طلایی بالای صحنه.
 * به‌عنوان پس‌زمینهٔ بخش‌های «اطلاع‌رسانی اولیه» و «فعالیت‌ها» استفاده می‌شود.
 */
@Composable
fun Lux3DStage(
    modifier: Modifier = Modifier,
    height: Dp? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val p = vizitorPalette
    val base = modifier
        .fillMaxWidth()
        .drawBehind {
            val w = size.width
            val h = size.height
            // نور طلایی از بالا (منبع نور صحنه)
            drawRect(
                Brush.verticalGradient(
                    listOf(p.gold.copy(alpha = 0.13f), Color.Transparent),
                    startY = 0f, endY = h * 0.66f
                )
            )
            // افق
            val horizon = h * 0.60f
            // سایهٔ کف (زمین)
            drawRect(
                Brush.verticalGradient(
                    listOf(Color.Transparent, p.surfaceDeep.copy(alpha = 0.85f)),
                    startY = horizon, endY = h
                ),
                topLeft = Offset(0f, horizon),
                size = Size(w, h - horizon)
            )
            // خطوط کف با فاصلهٔ فزاینده (حس عمق)
            var y = horizon
            var step = 5f
            while (y < h) {
                val t = ((y - horizon) / (h - horizon)).coerceIn(0f, 1f)
                drawLine(
                    color = p.gold.copy(alpha = 0.06f + 0.22f * t),
                    start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f
                )
                y += step
                step *= 1.32f
            }
            // دو خط گریز به مرکز افق (کفِ سه‌بعدی)
            drawLine(
                color = p.gold.copy(alpha = 0.16f),
                start = Offset(w / 2f, horizon), end = Offset(0f, h), strokeWidth = 1f
            )
            drawLine(
                color = p.gold.copy(alpha = 0.16f),
                start = Offset(w / 2f, horizon), end = Offset(w, h), strokeWidth = 1f
            )
        }
    if (height != null) Box(base.height(height), content = content) else Box(base, content = content)
}

// ═══════════════════════════ ۳) وجه + ضخامت (پایهٔ همهٔ اجزا) ═══════════════════════════

/**
 * قاب سه‌بعدی: وجهِ گرادیانی + **ضخامت واقعی** در لبهٔ پایین + حاشیه + خط نور.
 *
 * نکتهٔ چیدمانی: این مودیفایر ضخامت را *بیرون* از مرزهای خودش رسم می‌کند؛ در
 * اجزای آمادهٔ همین فایل، فضای لبه با یک Spacer رزرو شده است.
 */
@Composable
fun Modifier.lux3dPanel(
    depth: Dp = 8.dp,
    corner: Dp = 18.dp,
    accent: Color? = null,
    strong: Boolean = true,
): Modifier {
    val p = vizitorPalette
    val edge = accent ?: p.gold
    val dd = depth
    val cc = corner
    return this
        // ضخامت (لبهٔ زیرین): کمی پایین‌تر از وجه، تیره‌تر از آن
        .drawBehind {
            val px = dd.toPx()
            val r = cc.toPx()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        edge.copy(alpha = 0.62f),
                        edge.copy(alpha = 0.20f),
                        edge.copy(alpha = 0.05f)
                    )
                ),
                topLeft = Offset(0f, px * 0.45f),
                size = Size(size.width, size.height + px * 0.55f),
                cornerRadius = CornerRadius(r, r)
            )
        }
        .clip(RoundedCornerShape(cc))
        .background(
            Brush.verticalGradient(
                listOf(
                    p.surface.copy(alpha = if (strong) 0.99f else 0.94f),
                    p.surfaceDeep.copy(alpha = if (strong) 1f else 0.97f)
                )
            )
        )
        .border(1.dp, edge.copy(alpha = if (strong) 0.50f else 0.34f), RoundedCornerShape(cc))
        // خط نور بالای وجه (شیشهٔ صیقلی)
        .drawWithContent {
            drawContent()
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(edge.copy(alpha = 0.16f), Color.Transparent),
                    startY = 0f, endY = size.height * 0.55f
                ),
                cornerRadius = CornerRadius(cc.toPx(), cc.toPx())
            )
        }
}

/** کارت سه‌بعدی ساده — وجه + ضخامت + رزرو فضای لبه. */
@Composable
fun Lux3DSlab(
    modifier: Modifier = Modifier,
    depth: Dp = 9.dp,
    corner: Dp = 18.dp,
    accent: Color? = null,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .lux3dPanel(depth = depth, corner = corner, accent = accent)
                .padding(contentPadding),
            content = content
        )
        Spacer(Modifier.height(depth - 2.dp))
    }
}

// ═══════════════════════════ ۴) منوی سه‌بعدی ═══════════════════════════

/**
 * کاشی سه‌بعدی — هنگام لمس کمی به عقب می‌چرخد و جمع می‌شود (حس فشردن دکمه).
 * @param dense نسخهٔ فشرده برای شبکه‌های چندستونه.
 */
@Composable
fun Lux3DTile(
    item: Lux3DItem,
    modifier: Modifier = Modifier,
    depth: Dp = 8.dp,
    dense: Boolean = false,
    onClick: () -> Unit,
) {
    val p = vizitorPalette
    val accent = item.accent ?: p.gold
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(120), label = "lux3dTilePress")

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = 1f - 0.035f * press
                    scaleY = 1f - 0.035f * press
                    rotationX = 7f * press
                    cameraDistance = 12f * density
                }
                .lux3dPanel(depth = depth, corner = 16.dp, accent = accent)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(if (dense) 10.dp else 13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MaIconBadge(item.icon, size = if (dense) 34.dp else 42.dp, tint = accent)
                Spacer(Modifier.width(if (dense) 8.dp else 11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title,
                        fontSize = if (dense) 12.sp else 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = p.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.subtitle,
                        fontSize = if (dense) 9.sp else 10.sp,
                        color = p.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = if (dense) 12.sp else 14.sp
                    )
                    if (!item.badge.isNullOrBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Lux3DPill(item.badge!!, accent)
                    }
                }
            }
        }
        Spacer(Modifier.height(depth - 2.dp))
    }
}

/** قرصِ سه‌بعدی کوچک (برچسب/وضعیت). */
@Composable
fun Lux3DPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.62f))))
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(50))
                .padding(horizontal = 9.dp, vertical = 2.5.dp)
        ) {
            Text(
                text,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A1204)
            )
        }
        Spacer(Modifier.height(2.dp))
    }
}

/**
 * منوی سه‌بعدی — شبکهٔ چندستونه از کاشی‌های برجسته.
 * (شبکه با Row/Column ساخته می‌شود، نه LazyGrid، تا داخل صفحات اسکرولی
 *  هیچ خطای اندازه‌گذاری رخ ندهد.)
 */
@Composable
fun Lux3DMenu(
    items: List<Lux3DItem>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    dense: Boolean = false,
    onSelect: (Lux3DItem) -> Unit,
) {
    val cols = columns.coerceIn(1, 4)
    Column(modifier.fillMaxWidth()) {
        items.chunked(cols).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                row.forEach { item ->
                    Lux3DTile(
                        item = item,
                        modifier = Modifier.weight(1f),
                        dense = dense,
                        onClick = { onSelect(item) }
                    )
                }
                // ستون‌های خالی ردیف آخر (تا کاشی‌ها کشیده نشوند)
                repeat(cols - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

// ═══════════════════════════ ۵) جدول سه‌بعدی ═══════════════════════════

/**
 * جدول سه‌بعدی: سربرگ طلایی برجسته + ردیف‌های ضخامت‌دار با سایه.
 * @param weights وزن ستون‌ها (پیش‌فرض مساوی)
 * @param accentColumn شمارهٔ ستونی که رنگی (طلایی) نشان داده می‌شود (از صفر)
 */
@Composable
fun Lux3DTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    weights: List<Float>? = null,
    accentColumn: Int? = null,
    emptyText: String = "داده‌ای برای نمایش نیست",
) {
    val p = vizitorPalette
    val w = weights ?: List(headers.size) { 1f }
    Column(modifier.fillMaxWidth()) {
        // ── سربرگ: وجه طلایی + ضخامت ──
        Box(
            Modifier
                .fillMaxWidth()
                .lux3dPanel(depth = 7.dp, corner = 14.dp, accent = p.goldHighlight)
                .padding(horizontal = 10.dp, vertical = 9.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                headers.forEachIndexed { i, h ->
                    Text(
                        h,
                        modifier = Modifier.weight(w.getOrElse(i) { 1f }),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1A1204),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))

        if (rows.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .lux3dPanel(depth = 6.dp, corner = 14.dp)
                    .padding(vertical = 18.dp)
            ) {
                Text(
                    emptyText,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            rows.forEachIndexed { index, row ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .lux3dPanel(
                            depth = 6.dp,
                            corner = 13.dp,
                            accent = if (index % 2 == 0) p.gold.copy(alpha = 0.75f) else null,
                            strong = false
                        )
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        row.forEachIndexed { i, cell ->
                            Text(
                                cell,
                                modifier = Modifier.weight(w.getOrElse(i) { 1f }),
                                fontSize = 10.5.sp,
                                fontWeight = if (accentColumn == i) FontWeight.Black else FontWeight.Medium,
                                color = if (accentColumn == i) p.gold else p.textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

// ═══════════════════════════ ۶) نمودار ستونی ایزومتریک ═══════════════════════════

/**
 * نمودار ستونی سه‌بعدی (ایزومتریک): هر ستون سه وجه دارد —
 * وجه روبرو (گرادیان طلایی)، وجه بالا (روشن‌تر)، وجه کنار (تیره) —
 * به‌همراه سایه و بازتاب روی کف و درخشش نوک.
 */
@Composable
fun Lux3DBarChart(
    rows: List<Pair<String, Long>>,
    modifier: Modifier = Modifier,
    height: Dp = 215.dp,
    maxBars: Int = 7,
    accent: Color? = null,
    valueLabel: (Long) -> String = { it.toFaPrice() },
) {
    val p = vizitorPalette
    val gold = accent ?: p.gold
    val data = remember(rows) { rows.take(maxBars) }
    val grow by animateFloatAsState(1f, tween(760), label = "lux3dBarGrow")

    if (data.isEmpty()) {
        MaEmptyState(
            title = "داده‌ای برای نمودار نیست",
            subtitle = "پس از همگام‌سازی با سرور، ستون‌های سه‌بعدی اینجا رسم می‌شوند",
            modifier = modifier
        )
        return
    }
    val maxValue = (data.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val w = size.width
            val h = size.height
            val floorY = h * 0.88f
            val n = data.size
            val slot = w / n
            val bw = slot * 0.48f
            val dx = 9.dp.toPx()        // جابه‌جایی ایزومتریک افقی
            val dy = 6.dp.toPx()        // جابه‌جایی ایزومتریک عمودی
            val maxH = h * 0.66f

            // کف: خط افق و خطوط عمق
            drawLine(
                color = p.gold.copy(alpha = 0.30f),
                start = Offset(0f, floorY), end = Offset(w, floorY), strokeWidth = 1.4f
            )
            var y = floorY
            var step = 4f
            while (y < h) {
                drawLine(p.gold.copy(alpha = 0.10f), Offset(0f, y), Offset(w, y), 1f)
                y += step; step *= 1.35f
            }

            data.forEachIndexed { i, (_, value) ->
                val frac = ((value.toDouble() / maxValue.toDouble()).toFloat() * grow).coerceIn(0.04f, 1f)
                val bh = maxH * frac
                val x = i * slot + (slot - bw) / 2f
                val topY = floorY - bh

                // بازتاب روی کف (کم‌رنگ، رو به پایین)
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(gold.copy(alpha = 0.22f), Color.Transparent),
                        startY = floorY, endY = floorY + bh * 0.35f
                    ),
                    topLeft = Offset(x, floorY),
                    size = Size(bw, (bh * 0.35f).coerceAtLeast(4f))
                )
                // سایهٔ بیضی زیر ستون
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(x - dx * 0.4f, floorY - dy * 0.5f),
                    size = Size(bw + dx * 0.8f, dy * 1.6f)
                )
                // وجه روبرو
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(gold.copy(alpha = 0.55f), gold, p.goldHighlight.copy(alpha = 0.95f), gold)
                    ),
                    topLeft = Offset(x, topY),
                    size = Size(bw, bh)
                )
                // وجه بالا (موازی‌الاضلاع روشن)
                val top = Path().apply {
                    moveTo(x, topY)
                    lineTo(x + dx, topY - dy)
                    lineTo(x + bw + dx, topY - dy)
                    lineTo(x + bw, topY)
                    close()
                }
                drawPath(top, color = p.goldHighlight)

                // وجه کنار (تیره)
                val side = Path().apply {
                    moveTo(x + bw, topY)
                    lineTo(x + bw + dx, topY - dy)
                    lineTo(x + bw + dx, floorY - dy)
                    lineTo(x + bw, floorY)
                    close()
                }
                drawPath(side, color = gold.copy(alpha = 0.45f))

                // درخشش نوک ستون
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = 2.4.dp.toPx(),
                    center = Offset(x + bw / 2f + dx * 0.4f, topY - dy * 0.6f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // برچسب مقدار + نام دسته (زیر نمودار، خوانا و فارسی)
        Row(Modifier.fillMaxWidth()) {
            data.forEach { (label, value) ->
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        valueLabel(value),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = p.gold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        label,
                        fontSize = 8.5.sp,
                        color = p.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═══════════════════════════ ۷) دونات ضخیم سه‌بعدی ═══════════════════════════

/**
 * دونات سه‌بعدی: حلقهٔ ضخیم با لبهٔ تیرهٔ زیرین (عمق)، وجه رنگی روی آن،
 * نگین‌های الماسی چهار جهت و عدد فارسی در مرکز.
 */
@Composable
fun Lux3DDonut(
    slices: List<Pair<String, Float>>,
    centerValue: String,
    centerLabel: String,
    modifier: Modifier = Modifier,
    diameter: Dp = 168.dp,
    colors: List<Color>? = null,
    legend: Boolean = true,
) {
    val p = vizitorPalette
    val palette = colors ?: listOf(p.gold, p.goldHighlight, MaGreen, MaAmber, MaRed)
    val total = slices.sumOf { it.second.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    val ring = diameter.value * 0.13f
    val grow by animateFloatAsState(1f, tween(900), label = "lux3dDonutGrow")

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(diameter)) {
                val stroke = ring.dp.toPx()
                val inset = stroke / 2f + 2.dp.toPx()
                val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                val topLeft = Offset(inset, inset)
                // ۱) لبهٔ تیرهٔ زیرین (عمق سه‌بعدی)
                drawArc(
                    color = Color.Black.copy(alpha = 0.55f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(topLeft.x, topLeft.y + 6.dp.toPx()),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // ۲) ریل پشت
                drawArc(
                    color = p.donutTrack.copy(alpha = 0.35f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )
                // ۳) برش‌ها روی وجه
                var start = -90f
                slices.forEachIndexed { i, (_, v) ->
                    val sweep = (v / total) * 360f * grow
                    if (sweep > 0.5f) {
                        val c = palette[i % palette.size]
                        drawArc(
                            brush = Brush.sweepGradient(listOf(c, c.copy(alpha = 0.62f), c)),
                            startAngle = start, sweepAngle = sweep - 2f, useCenter = false,
                            topLeft = topLeft, size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        // درخشش روی لبهٔ بالای برش (حس فلز)
                        drawArc(
                            color = Color.White.copy(alpha = 0.22f),
                            startAngle = start + 1f, sweepAngle = (sweep - 4f).coerceAtLeast(0f),
                            useCenter = false,
                            topLeft = Offset(topLeft.x, topLeft.y - stroke * 0.22f),
                            size = arcSize,
                            style = Stroke(width = stroke * 0.28f, cap = StrokeCap.Round)
                        )
                    }
                    start += (v / total) * 360f
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    centerValue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = p.gold
                )
                Text(
                    centerLabel,
                    fontSize = 9.5.sp,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (legend && slices.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                slices.forEachIndexed { i, (name, v) ->
                    val c = palette[i % palette.size]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(c)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            name,
                            modifier = Modifier.weight(1f),
                            fontSize = 10.sp,
                            color = p.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${((v / total) * 100f).toInt().toFaNumber()}٪",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = c
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════ ۸) گویِ آماره (کره) ═══════════════════════════

/**
 * گوی سه‌بعدی یک عدد: کره با گرادیان شعاعی، نور نقطه‌ای، سایهٔ پایه و حلقهٔ طلایی.
 * مناسب ردیف شاخص‌های «اطلاع‌رسانی اولیه».
 */
@Composable
fun Lux3DStatOrb(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    caption: String = "",
    color: Color? = null,
    size: Dp = 96.dp,
) {
    val p = vizitorPalette
    val c = color ?: p.gold
    val appear by animateFloatAsState(1f, tween(700), label = "lux3dOrbAppear")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val r = this.size.minDimension / 2f
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                // سایهٔ پایه (نشان می‌دهد کره روی سطح ایستاده)
                drawOval(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(cx - r * 0.82f, cy + r * 0.72f),
                    size = Size(r * 1.64f, r * 0.42f)
                )
                // بدنهٔ کره — گرادیان شعاعی با منبع نور بالا-چپ
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            c,
                            c.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.65f)
                        ),
                        center = Offset(cx - r * 0.35f, cy - r * 0.42f),
                        radius = r * 1.55f * (0.85f + 0.15f * appear)
                    ),
                    radius = r * 0.92f,
                    center = Offset(cx, cy)
                )
                // نور نقطه‌ای (بازتاب منبع نور)
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = r * 0.16f,
                    center = Offset(cx - r * 0.34f, cy - r * 0.40f)
                )
                // حلقهٔ طلایی دور کره
                drawCircle(
                    color = p.goldHighlight.copy(alpha = 0.75f),
                    radius = r * 0.95f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value,
                    fontSize = (size.value * 0.20f).sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1A1204)
                )
                Text(
                    label,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1204).copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
        if (caption.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                caption,
                fontSize = 9.5.sp,
                color = p.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// ═══════════════════════════ ۹) نوار پیشرفت سه‌بعدی ═══════════════════════════

/** نوار پیشرفت سه‌بعدی: شیار فرورفته + ستون برآمده + درخشش. */
@Composable
fun Lux3DProgress(
    fraction: Float,
    label: String,
    valueText: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val p = vizitorPalette
    val c = color ?: p.gold
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(650), label = "lux3dProgress")

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = p.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(valueText, fontSize = 11.sp, fontWeight = FontWeight.Black, color = c)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .lux3dPanel(depth = 4.dp, corner = 9.dp, accent = c.copy(alpha = 0.5f), strong = false)
        ) {
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                val w = size.width * f
                if (w <= 0f) return@Canvas
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(c.copy(alpha = 0.95f), c.copy(alpha = 0.55f))
                    ),
                    size = Size(w, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.45f),
                    start = Offset(0f, size.height * 0.28f),
                    end = Offset(w, size.height * 0.28f),
                    strokeWidth = 1.6f
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

// ═══════════════════════════ ۱۰) سربرگ بخش سه‌بعدی ═══════════════════════════

/** سربرگ بخش با نشان طلایی و زیرنویس — بالای هر بلوک سه‌بعدی. */
@Composable
fun Lux3DSectionTitle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val p = vizitorPalette
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        MaIconBadge(icon, size = 34.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = p.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                fontSize = 9.5.sp,
                color = p.textSecondary,
                maxLines = 2,
                lineHeight = 12.sp
            )
        }
        if (!badge.isNullOrBlank()) Lux3DPill(badge, p.gold)
    }
}

/** ردیف‌چین افقی گوی‌های آماره (چند گوی در یک ردیف). */
@Composable
fun Lux3DOrbRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
        content = content
    )
}

/** متن کوچک راهنما (زیر هر بخش سه‌بعدی). */
@Composable
fun Lux3DNote(text: String, modifier: Modifier = Modifier, tone: Color? = null) {
    val p = vizitorPalette
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.surfaceDeep.copy(alpha = 0.6f))
            .border(1.dp, (tone ?: p.gold).copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tone ?: p.gold)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 10.sp,
            color = p.textSecondary,
            lineHeight = 15.sp,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
