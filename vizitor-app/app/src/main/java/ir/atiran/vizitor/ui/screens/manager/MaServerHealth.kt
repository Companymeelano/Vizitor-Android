/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران | «سلامت سرور مورد اتصال» (v2.24.0)
 *  Developed by Meelano Studio Design — Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  خواستهٔ مدیر: «امکان صحت و سلامت سرور مورد اتصال» + «دسته‌بندی بهتر».
 *
 *  این فایل یک آزمایشگاه کوچک سلامت می‌سازد که با کوئری‌های فقط-خواندنی و
 *  کاوش‌های شبکه‌ای، وضعیت سرور را در **پنج دستهٔ روشن** می‌سنجد و به هر دسته
 *  یک چراغ رنگی می‌دهد:
 *
 *    ۱) شبکه و مسیر        — نوع شبکه، مسیر انتخابی، زمان پاسخ TCP، پایداری
 *    ۲) اتصال و احراز هویت — برقراری اتصال، حالت اتصال، تأیید ورود، حساب کاربر
 *    ۳) دیتابیس و گزارش‌ها — نسخهٔ سرور، حجم دیتابیس، تعداد جداول، مجموع رکوردها
 *    ۴) دسترسی و امنیت     — آزمون خواندن، نقش ستون‌ها، فقط-خواندنی بودن
 *
 *  سپس یک «نمرهٔ سلامت» از ۰ تا ۱۰۰ می‌سازد و همه‌چیز را در کارت‌های سه‌بعدی
 *  (`Lux3D*`) نمایش می‌دهد. هیچ نشانی/کاربر/رمزی روی صفحه نمی‌آید — همهٔ متن‌ها
 *  از `MaServerProfile.safe()` می‌گذرند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens.manager

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.sqldirect.MaMapping
import ir.atiran.vizitor.sqldirect.MaNetKind
import ir.atiran.vizitor.sqldirect.MaSection
import ir.atiran.vizitor.sqldirect.MaSectionMap
import ir.atiran.vizitor.sqldirect.MaSectionStore
import ir.atiran.vizitor.sqldirect.MaServerProfile
import ir.atiran.vizitor.sqldirect.MaSqlEngine
import ir.atiran.vizitor.sqldirect.SecureDbStore
import ir.atiran.vizitor.ui.components.Lux3DButton
import ir.atiran.vizitor.ui.components.Lux3DCheckRow
import ir.atiran.vizitor.ui.components.Lux3DDivider
import ir.atiran.vizitor.ui.components.Lux3DGauge
import ir.atiran.vizitor.ui.components.Lux3DGroupHeader
import ir.atiran.vizitor.ui.components.Lux3DLevel
import ir.atiran.vizitor.ui.components.Lux3DIconButton
import ir.atiran.vizitor.ui.components.Lux3DStatusPill
import ir.atiran.vizitor.ui.components.Lux3DTone
import ir.atiran.vizitor.ui.components.lux3dLevelSkin
import ir.atiran.vizitor.ui.components.metalPanel
import ir.atiran.vizitor.ui.theme.vizitorPalette
import ir.atiran.vizitor.util.toFaDigits
import ir.atiran.vizitor.util.toFaNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════ مدل دادهٔ سلامت ═══════════════════════════

/** یک «بررسی» سلامت: عنوان، مقدار برجسته، توضیح و چراغ وضعیت. */
data class MaHealthItem(
    val title: String,
    val value: String,
    val detail: String,
    val level: Lux3DLevel,
)

/** یک «دسته» از بررسی‌ها (شبکه، اتصال، دیتابیس، امنیت …). */
data class MaHealthGroup(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val items: List<MaHealthItem>,
) {
    /** بدترین وضعیت میان بررسی‌های این دسته. */
    val level: Lux3DLevel
        get() = when {
            items.isEmpty() -> Lux3DLevel.IDLE
            items.any { it.level == Lux3DLevel.BAD } -> Lux3DLevel.BAD
            items.any { it.level == Lux3DLevel.WARN } -> Lux3DLevel.WARN
            items.any { it.level == Lux3DLevel.RUN } -> Lux3DLevel.RUN
            items.all { it.level == Lux3DLevel.IDLE } -> Lux3DLevel.IDLE
            else -> Lux3DLevel.OK
        }

    /** «سالم» بودن کل دسته. */
    val okCount: Int get() = items.count { it.level == Lux3DLevel.OK }
}

/** کارنامهٔ کامل سلامت سرور. */
data class MaHealthReport(
    val score: Int,
    val headline: String,
    val pathLabel: String,
    val atLabel: String,
    val durationMs: Long,
    val groups: List<MaHealthGroup>,
) {
    val level: Lux3DLevel
        get() = when {
            score >= 85 -> Lux3DLevel.OK
            score >= 60 -> Lux3DLevel.WARN
            else -> Lux3DLevel.BAD
        }
    val itemCount: Int get() = groups.sumOf { it.items.size }
    val okCount: Int get() = groups.sumOf { it.okCount }
}

// ═══════════════════════════ موتور سنجش سلامت ═══════════════════════════

/**
 * سنجش سلامت سرور با کوئری‌های فقط-خواندنی.
 *
 * @param quick حالت سبک برای صفحهٔ خانه: شبکه + اتصال را می‌سنجد و از کوئری‌های
 *   سنگین (حجم دیتابیس، شمار جدول‌ها، آزمون خواندن) می‌گذرد.
 */
object MaServerHealth {

    private fun nowMs(): Long = System.currentTimeMillis()

    private fun tcpProbe(host: String, timeoutMs: Int): Pair<Boolean, Long> {
        val t = nowMs()
        val ok = runCatching { MaSqlEngine.rawTcpProbe(host, MaServerProfile.port, timeoutMs) }
            .getOrDefault(false)
        return ok to (nowMs() - t)
    }

    private fun levelOfTcp(ok: Boolean, ms: Long): Lux3DLevel = when {
        !ok -> Lux3DLevel.BAD
        ms <= 700 -> Lux3DLevel.OK
        ms <= 2200 -> Lux3DLevel.WARN
        else -> Lux3DLevel.BAD
    }

    suspend fun probe(ctx: Context, quick: Boolean = false): MaHealthReport =
        withContext(Dispatchers.IO) {
            val started = nowMs()

            // ── ۰) پیش‌نیاز: شبکه و ترتیب نشانی‌های مخفی ──────────────────
            val kind = MaServerProfile.netKind(ctx)
            val hosts = MaServerProfile.orderedHosts(ctx)
            val primaryLocal = hosts.firstOrNull() == MaServerProfile.hostLocal
            val pathLabel = if (primaryLocal) "شبکهٔ داخلی" else "اینترنت"

            var reachedLocal = false
            var tcpOk = false
            var tcpMs = -1L
            for (h in hosts) {
                val (ok, ms) = tcpProbe(h, 2500)
                if (ok) {
                    tcpOk = true
                    tcpMs = ms
                    reachedLocal = h == MaServerProfile.hostLocal
                    break
                }
            }

            // پایداری مسیر: سه کاوش پی‌درپی روی همان نشانی موفق (یا اولی)
            val probeHost = if (tcpOk) {
                if (reachedLocal) MaServerProfile.hostLocal else MaServerProfile.hostExternal
            } else hosts.firstOrNull().orEmpty()
            val stability = if (probeHost.isBlank()) 0 else
                (1..3).count { tcpProbe(probeHost, 1500).first }

            // ── ۱) دستهٔ شبکه و مسیر ──────────────────────────────────────
            val netItems = mutableListOf(
                MaHealthItem(
                    title = "شبکهٔ فعلی گوشی",
                    value = kind.label,
                    detail = "انتخاب مسیر سرور بر اساس همین شبکه انجام می‌شود.",
                    level = if (kind == MaNetKind.NONE) Lux3DLevel.BAD else Lux3DLevel.OK,
                ),
                MaHealthItem(
                    title = "مسیر انتخابی برنامه",
                    value = pathLabel,
                    detail = if (primaryLocal)
                        "ابتدا سرور داخلی اداره امتحان می‌شود و اگر نبود، مسیر اینترنت."
                    else
                        "ابتدا مسیر اینترنت امتحان می‌شود و اگر نبود، سرور داخلی اداره.",
                    level = Lux3DLevel.OK,
                ),
                MaHealthItem(
                    title = "زمان پاسخ سرور (کاوش TCP)",
                    value = if (tcpOk) "${tcpMs.toFaNumber()} میلی‌ثانیه" else "بی‌پاسخ",
                    detail = if (tcpOk)
                        "سرور از مسیر «${if (reachedLocal) "داخلی" else "اینترنت"}» در دسترس است."
                    else
                        "هیچ‌کدام از دو مسیر پاسخ ندادند — شبکه را بررسی کنید.",
                    level = levelOfTcp(tcpOk, tcpMs),
                ),
                MaHealthItem(
                    title = "پایداری مسیر",
                    value = "${stability.toFaNumber()} از ۳",
                    detail = "سه کاوش پی‌درپی برای اطمینان از ثابت بودن مسیر.",
                    level = when {
                        stability >= 3 -> Lux3DLevel.OK
                        stability >= 1 -> Lux3DLevel.WARN
                        else -> Lux3DLevel.BAD
                    },
                ),
            )
            val netGroup = MaHealthGroup("net", "شبکه و مسیر", Icons.Filled.Wifi, netItems)

            // ── ۲) دستهٔ اتصال و احراز هویت ───────────────────────────────
            val connStart = nowMs()
            val connected = runCatching { MaServerProfile.ensureSqlConnection(ctx) }
                .getOrDefault(false)
            val connMs = nowMs() - connStart
            val modeLabel = MaServerProfile.safe(MaSqlEngine.modeLabel).ifBlank { "—" }

            var serverVersion = ""
            var dbOk = false
            if (connected) {
                dbOk = runCatching {
                    val (db, _, ver) = MaSqlEngine.test(MaServerProfile.activeSettings(ctx))
                    serverVersion = MaServerProfile.safe(ver).lineSequence().firstOrNull().orEmpty()
                    db.isNotBlank()
                }.getOrDefault(false)
            }

            val erp = runCatching { SecureDbStore.loadErp() }.getOrNull()
            val visitor = runCatching { SecureDbStore.loadVisitor() }.getOrNull()
            val connItems = mutableListOf(
                MaHealthItem(
                    title = "برقراری اتصال",
                    value = if (connected) "برقرار ✓" else "برقرار نشد",
                    detail = if (connected)
                        "اتصال در ${connMs.toFaNumber()} میلی‌ثانیه آماده شد."
                    else
                        "دکمهٔ «اتصال خودکار به سرور» را بزنید یا عیب‌یابی را ببینید.",
                    level = if (connected) Lux3DLevel.OK else Lux3DLevel.BAD,
                ),
                MaHealthItem(
                    title = "حالت اتصال",
                    value = modeLabel,
                    detail = "موتور اتصال برنامه (چهارحالته) و مسیر فعال.",
                    level = if (connected) Lux3DLevel.OK else Lux3DLevel.IDLE,
                ),
                MaHealthItem(
                    title = "تأیید دیتابیس (SELECT)",
                    value = if (dbOk) "تأیید شد ✓" else "تأیید نشد",
                    detail = if (serverVersion.isNotBlank())
                        "نسخهٔ سرور: $serverVersion"
                    else
                        "پاسخ سرور برای پرس‌وجوی آزمایشی خوانده نشد.",
                    level = if (dbOk) Lux3DLevel.OK else if (connected) Lux3DLevel.WARN else Lux3DLevel.BAD,
                ),
                MaHealthItem(
                    title = "حساب کاربر آتیران",
                    value = visitor?.name?.takeIf { it.isNotBlank() } ?: "وارد نشده",
                    detail = if (!erp?.username.isNullOrBlank())
                        "نام کاربری به‌خاطر سپرده شده است (رمزنگاری‌شده روی گوشی)."
                    else
                        "برای گزارش‌های شخصی‌سازی‌شده وارد شوید.",
                    level = if (!visitor?.name.isNullOrBlank()) Lux3DLevel.OK else Lux3DLevel.WARN,
                ),
            )
            val connGroup = MaHealthGroup("conn", "اتصال و احراز هویت", Icons.Filled.Shield, connItems)

            val groups = mutableListOf(netGroup, connGroup)

            // ── ۳) دستهٔ دیتابیس و گزارش‌ها (در حالت سبک انجام نمی‌شود) ────
            if (!quick) {
                val overview = if (connected) runCatching { MaSqlEngine.overview() }.getOrNull() else null
                val map = runCatching { MaSectionStore.load() }.getOrElse { MaSectionMap() }
                val dbItems = listOf(
                    MaHealthItem(
                        title = "نسخهٔ سرور SQL",
                        value = if (serverVersion.isNotBlank()) "خوانده شد ✓" else "خوانده نشد",
                        detail = serverVersion.ifBlank { "برای خواندن نسخه به اتصال سالم نیاز است." },
                        level = if (serverVersion.isNotBlank()) Lux3DLevel.OK else Lux3DLevel.WARN,
                    ),
                    MaHealthItem(
                        title = "حجم دیتابیس",
                        value = if (overview != null) "%.1f مگابایت".format(overview.sizeMb).toFaDigits() else "—",
                        detail = "اندازهٔ فایل‌های دیتابیس آتیران روی سرور.",
                        level = if (overview != null && overview.sizeMb > 0.0) Lux3DLevel.OK else Lux3DLevel.WARN,
                    ),
                    MaHealthItem(
                        title = "جداول سرور",
                        value = if (overview != null) "${overview.tables.size.toFaNumber()} جدول" else "—",
                        detail = "فهرست جدول‌های دیتابیس (کاتالوگ سیستم).",
                        level = if (!overview?.tables.isNullOrEmpty()) Lux3DLevel.OK else Lux3DLevel.WARN,
                    ),
                    MaHealthItem(
                        title = "مجموع رکوردها",
                        value = if (overview != null) overview.totalRows.toFaNumber() else "—",
                        detail = "شمار تقریبی همهٔ رکوردهای جدول‌های سرور.",
                        level = if ((overview?.totalRows ?: 0L) > 0L) Lux3DLevel.OK else Lux3DLevel.WARN,
                    ),
                    MaHealthItem(
                        title = "بخش‌های نگاشت‌شده",
                        value = "${map.mappedCount.toFaNumber()} از ۵",
                        detail = "بخش‌هایی که جدول واقعی سرور به آن‌ها وصل است.",
                        level = if (map.mappedCount >= 3) Lux3DLevel.OK
                        else if (map.mappedCount >= 1) Lux3DLevel.WARN
                        else Lux3DLevel.WARN,
                    ),
                )
                groups += MaHealthGroup("db", "دیتابیس و گزارش‌ها", Icons.Filled.Storage, dbItems)

                // ── ۴) دستهٔ دسترسی و امنیت ───────────────────────────────
                val ref = MaSection.values().mapNotNull { sec -> map.refOf(sec) }.firstOrNull()
                var readMs = -1L
                var readRows = -1L
                if (connected && ref != null) {
                    val (schema, table) = MaMapping.split(ref) ?: ("" to "")
                    if (schema.isNotBlank() && table.isNotBlank()) {
                        val t = nowMs()
                        readRows = runCatching { MaSqlEngine.count(schema, table) }.getOrDefault(-1L)
                        readMs = nowMs() - t
                    }
                }
                val roles = if (ref != null) runCatching { MaMapping.rolesOf(ref) }.getOrNull() else null
                val roleCount = listOfNotNull(
                    roles?.titleCol, roles?.amountCol, roles?.dateCol, roles?.codeCol,
                ).size
                val secItems = listOf(
                    MaHealthItem(
                        title = "آزمون خواندن گزارش",
                        value = when {
                            readRows >= 0 -> "موفق ✓"
                            ref == null -> "بدون جدول"
                            else -> "ناموفق"
                        },
                        detail = when {
                            readRows >= 0 -> "خواندن ${readRows.toFaNumber()} رکورد در ${readMs.toFaNumber()} میلی‌ثانیه."
                            ref == null -> "جدولی نگاشت نشده — از «اتصال جداول» استفاده کنید."
                            else -> "خواندن آزمایشی انجام نشد."
                        },
                        level = if (readRows >= 0) Lux3DLevel.OK else Lux3DLevel.WARN,
                    ),
                    MaHealthItem(
                        title = "نقش ستون‌ها",
                        value = "${roleCount.toFaNumber()} از ۴",
                        detail = "نام، مبلغ، تاریخ و کد — تشخیص هوشمند خودکار.",
                        level = if (roleCount >= 3) Lux3DLevel.OK
                        else if (roleCount >= 1) Lux3DLevel.WARN
                        else Lux3DLevel.WARN,
                    ),
                    MaHealthItem(
                        title = "نوع دسترسی برنامه",
                        value = "فقط SELECT",
                        detail = "هیچ داده‌ای روی سرور نوشته یا تغییر داده نمی‌شود.",
                        level = Lux3DLevel.OK,
                    ),
                    MaHealthItem(
                        title = "پوشیدگی مشخصات سرور",
                        value = "پنهان کامل",
                        detail = "نشانی، پورت، دیتابیس و کاربر SQL هیچ‌جا نمایش داده نمی‌شود.",
                        level = Lux3DLevel.OK,
                    ),
                )
                groups += MaHealthGroup("sec", "دسترسی و امنیت", Icons.Filled.Lock, secItems)
            }

            // ── ۵) نمرهٔ سلامت ────────────────────────────────────────────
            val weights = groups.flatMap { g -> g.items.map { it.level } }
            val maxScore = weights.size * 100
            val gotScore: Int = weights.map { lv ->
                when (lv) {
                    Lux3DLevel.OK -> 100
                    Lux3DLevel.WARN -> 60
                    Lux3DLevel.IDLE -> 70
                    Lux3DLevel.RUN -> 50
                    Lux3DLevel.BAD -> 0
                }
            }.sum()
            val score = if (maxScore == 0) 0 else (gotScore * 100 / maxScore)
            val headline = when {
                score >= 90 -> "سرور سالم و آماده است ✓"
                score >= 70 -> "سرور سالم است — چند نکتهٔ کوچک"
                score >= 40 -> "سرور پاسخ می‌دهد اما نیازمند بررسی"
                else -> "اتصال سرور درست نیست"
            }
            val at = SimpleDateFormat("HH:mm", Locale.US).format(Date()).toFaDigits()

            MaHealthReport(
                score = score.coerceIn(0, 100),
                headline = headline,
                pathLabel = if (reachedLocal) "داخلی" else pathLabel,
                atLabel = at,
                durationMs = nowMs() - started,
                groups = groups,
            )
        }
}

// ═══════════════════════════ کارت سلامت سرور ═══════════════════════════

/**
 * کارت «سلامت سرور مورد اتصال» — گیج سه‌بعدی نمرهٔ سلامت + پنج دستهٔ بررسی.
 *
 * @param compact حالت جمع‌وجور برای صفحهٔ خانه (فقط نمره، سرصفحه و یک دکمه).
 */
@Composable
fun MaServerHealthCard(
    report: MaHealthReport?,
    busy: Boolean,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val p = vizitorPalette
    val scoreLevel = report?.level ?: Lux3DLevel.RUN
    val skin = lux3dLevelSkin(scoreLevel)
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .metalPanel(shape, strong = true, corner = 22f)
            .padding(14.dp),
    ) {
        Lux3DGroupHeader(
            title = "سلامت سرور مورد اتصال",
            icon = Icons.Filled.Speed,
            status = when {
                busy -> "در حال بررسی"
                report == null -> "بررسی نشده"
                else -> "نمرهٔ ${report.score.toFaNumber()}"
            },
        )
        Spacer(Modifier.size(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Lux3DGauge(
                percent = (report?.score ?: 0).toFloat(),
                centerText = if (busy) "…" else "${(report?.score ?: 0).toFaNumber()}",
                centerSub = "از ۱۰۰",
                size = if (compact) 92.dp else 118.dp,
                tone = when (scoreLevel) {
                    Lux3DLevel.OK -> Lux3DTone.GREEN
                    Lux3DLevel.WARN -> Lux3DTone.GOLD
                    Lux3DLevel.BAD -> Lux3DTone.RED
                    Lux3DLevel.RUN -> Lux3DTone.BLUE
                    Lux3DLevel.IDLE -> Lux3DTone.NIGHT
                },
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        busy -> "در حال سنجش سرور…"
                        report == null -> "هنوز بررسی نشده — دکمهٔ زیر را بزنید"
                        else -> report.headline
                    },
                    color = if (report == null) p.textPrimary else skin.faceTop,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = when {
                        report == null -> "شبکه، اتصال، دیتابیس، خواندن داده و امنیت — همه در یک نگاه."
                        else -> "${report.okCount.toFaNumber()} از ${report.itemCount.toFaNumber()} بررسی سالم · " +
                            "مسیر ${report.pathLabel} · ساعت ${report.atLabel} · " +
                            "${report.durationMs.toFaNumber()} میلی‌ثانیه"
                    },
                    color = p.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Lux3DStatusPill(
                        text = when {
                            busy -> "در حال بررسی"
                            report == null -> "بررسی‌نشده"
                            scoreLevel == Lux3DLevel.OK -> "سالم"
                            scoreLevel == Lux3DLevel.WARN -> "هشدار"
                            else -> "ناسالم"
                        },
                        level = if (busy) Lux3DLevel.RUN else scoreLevel,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "دستهٔ فعال: " + (report?.groups?.firstOrNull()?.title ?: "—"),
                        color = p.textSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
        }
        Spacer(Modifier.size(12.dp))
        Lux3DButton(
            title = if (busy) "در حال سنجش سرور…" else "بررسی سلامت سرور",
            subtitle = "کاوش شبکه، تأیید اتصال، آزمون خواندن و بررسی امنیت",
            icon = Icons.Filled.Speed,
            tone = Lux3DTone.BLUE,
            enabled = !busy,
            busy = busy,
            depth = 7.dp,
            onClick = onRun,
        )

        if (!compact && report != null) {
            report.groups.forEach { g ->
                Spacer(Modifier.size(12.dp))
                Lux3DDivider()
                Spacer(Modifier.size(10.dp))
                Lux3DGroupHeader(
                    title = g.title,
                    icon = g.icon,
                    trailing = "${g.okCount.toFaNumber()}/${g.items.size.toFaNumber()}",
                )
                Spacer(Modifier.size(4.dp))
                g.items.forEachIndexed { i, item ->
                    Lux3DCheckRow(
                        title = item.title,
                        value = item.value,
                        detail = item.detail,
                        level = item.level,
                    )
                    if (i != g.items.lastIndex) Lux3DDivider()
                }
            }
            Spacer(Modifier.size(10.dp))
            Text(
                text = "این سنجش فقط-خواندنی است و هیچ تغییری روی سرور ایجاد نمی‌کند؛ " +
                    "مشخصات سرور هم برای امنیت نمایش داده نمی‌شود.",
                color = p.textSecondary,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

/**
 * نوار فشردهٔ سلامت برای صفحهٔ خانه: نمره + سرصفحه + دکمهٔ برجستهٔ بررسی.
 * (حالت سبک — بدون کوئری‌های سنگین.)
 */
@Composable
fun MaServerHealthStrip(
    report: MaHealthReport?,
    busy: Boolean,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = vizitorPalette
    val level = if (busy) Lux3DLevel.RUN else report?.level ?: Lux3DLevel.IDLE
    val skin = lux3dLevelSkin(level)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .metalPanel(RoundedCornerShape(18.dp), corner = 18f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(listOf(skin.faceTop, skin.faceBottom))
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (busy) "…" else "${(report?.score ?: 0).toFaNumber()}",
                color = skin.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (busy) "در حال سنجش سلامت سرور…" else report?.headline ?: "سلامت سرور بررسی نشده",
                color = p.textPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                text = if (report == null)
                    "برای سنجش شبکه، اتصال، دیتابیس و امنیت، دکمه را بزنید."
                else
                    "${report.okCount.toFaNumber()}/${report.itemCount.toFaNumber()} بررسی سالم · " +
                        "مسیر ${report.pathLabel} · ${report.atLabel}",
                color = p.textSecondary,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Lux3DIconButton(
            icon = if (busy) Icons.Filled.Timeline else Icons.Filled.Refresh,
            onClick = onRun,
            size = 42.dp,
            tone = when (level) {
                Lux3DLevel.OK -> Lux3DTone.GREEN
                Lux3DLevel.WARN -> Lux3DTone.GOLD
                Lux3DLevel.BAD -> Lux3DTone.RED
                else -> Lux3DTone.BLUE
            },
            contentDescription = "سنجش سلامت سرور",
            enabled = !busy,
        )
    }
}

/** نشان کوچک «سالم / هشدار / ناسالم» برای نوار بالای صفحه. */
@Composable
fun MaServerHealthChip(report: MaHealthReport?, modifier: Modifier = Modifier) {
    if (report == null) return
    Lux3DStatusPill(
        text = "سلامت ${report.score.toFaNumber()}",
        level = report.level,
        modifier = modifier,
    )
}
