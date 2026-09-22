/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | صفحهٔ ورود لاکچری (Welcome Splash) v4.0.0
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  بازطراحی کامل صفحهٔ اصلی برای «همهٔ گوشی‌های اندروید» (اندروید ۷ تا ۱۵):
 *    ▸ ابعاد واکنشی چهاررده (ScreenFit): کوچک / معمولی / بزرگ / تبلت
 *    ▸ حاشیهٔ امن نوار وضعیت، نوار ناوبری، بریدگی دوربین و بلندگو
 *    ▸ پس‌زمینهٔ تمام‌صفحهٔ چرمیِ آجیلی در تم‌های تیره و روشن (بدون بریدگی)
 *    ▸ نشان سه‌بعدی طلایی M + مغز پسته در قاب جواهر با هاله و درخشش
 *    ▸ پنل گام‌به‌گام ورود: گام ۱ اتصال سرور ← گام ۲ انتخاب نقش
 *    ▸ کاشی نقش‌ها با گوی سه‌بعدی آواتار، رینگ طلایی و فرو رفتن هنگام لمس
 *    ▸ کلید سه‌بعدی «طلایی» برای ورود سریع (عمق واقعی + جاروب نور)
 *  همه جلوه‌ها با موتور VizitorPerf روی دستگاه ضعیف خودکار ساده می‌شوند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.atiran.vizitor.R
import ir.atiran.vizitor.sqldirect.ServerSession
import ir.atiran.vizitor.ui.components.BrandOrb
import ir.atiran.vizitor.ui.components.BtnTone
import ir.atiran.vizitor.ui.components.GlowChip
import ir.atiran.vizitor.ui.components.GoldDivider
import ir.atiran.vizitor.ui.components.GoldFlourish
import ir.atiran.vizitor.ui.components.LuxBanner
import ir.atiran.vizitor.ui.components.LuxChip
import ir.atiran.vizitor.ui.components.LuxTone
import ir.atiran.vizitor.ui.components.glassRelief
import ir.atiran.vizitor.ui.components.luxFrame
import ir.atiran.vizitor.ui.components.ornaments
import ir.atiran.vizitor.ui.components.shimmerSweep
import ir.atiran.vizitor.ui.components.GradientTitle
import ir.atiran.vizitor.ui.components.IconOrb3D
import ir.atiran.vizitor.ui.components.KeyRow
import ir.atiran.vizitor.ui.components.LuxuryTile
import ir.atiran.vizitor.ui.components.OrbIconButton
import ir.atiran.vizitor.ui.components.PremiumButton
import ir.atiran.vizitor.ui.components.PremiumPanel
import ir.atiran.vizitor.ui.components.SatinBackdrop
import ir.atiran.vizitor.ui.components.ScreenFit
import ir.atiran.vizitor.ui.components.PremiumSectionTitle
import ir.atiran.vizitor.ui.components.StatPill
import ir.atiran.vizitor.ui.components.press3D
import ir.atiran.vizitor.ui.components.rememberScreenFit
import ir.atiran.vizitor.ui.components.screenSafePadding
import ir.atiran.vizitor.ui.theme.TextSecondary
import ir.atiran.vizitor.ui.theme.vizitorPalette

/** قاب متحرک با پیشرفت یک‌باره — آلفا بین ابتدا→پایان پنجره. */
private fun segmentAlpha(progress: Float, start: Float, span: Float): Float =
    ((progress - start) / span).coerceIn(0f, 1f)

/** نقش‌های ورود با آواتار کاراکتریِ شغلی — ویزیتور فعال؛ بقیه «به‌زودی». */
private data class Role(
    val title: String,
    val subtitle: String,
    val avatarRes: Int,
    val active: Boolean
)

private val Roles = listOf(
    Role("مامور فروش", "ویزیتور سیار", R.drawable.nut_visitor, true),
    Role("مدیریت", "نظارت کل", R.drawable.nut_manager, false),
    Role("مدیر فروش", "تیم فروش", R.drawable.nut_sales, false),
    Role("حسابداری", "مالی و اسناد", R.drawable.nut_accountant, false),
    Role("انبار و پخش", "موجودی و ارسال", R.drawable.nut_warehouse, false),
    Role("کاربر فروشگاه", "فروش حضوری", R.drawable.role_shopkeeper, false)
)

@Composable
fun SplashScreen(
    onEnter: () -> Unit,
    onSoon: (String) -> Unit,
    serverSession: ServerSession = ServerSession(),
    onOpenServerConfig: () -> Unit = {},
    onQuickEnter: () -> Unit = {},
) {
    val p = vizitorPalette
    // وضعیت اتصال/ورود از والد (منبع واحد: VizitorSession) می‌آید
    val session = serverSession
    // اندازهٔ واکنشی صفحه — همهٔ گوشی‌ها و تبلت‌ها
    val fit = rememberScreenFit()
    val isDark = p.isDark

    // انیمیشن ورود یک‌باره (بدون حلقه دائمی — ضد لگ استارتاپ)
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(1600)) }
    val t = enter.value

    val headA = segmentAlpha(t, 0.00f, 0.16f)
    val heroA = segmentAlpha(t, 0.06f, 0.18f)
    val panelA = segmentAlpha(t, 0.18f, 0.20f)
    val rolesH = segmentAlpha(t, 0.34f, 0.16f)
    val rolesA = segmentAlpha(t, 0.42f, 0.26f)
    val footA = segmentAlpha(t, 0.68f, 0.26f)

    Box(Modifier.fillMaxSize()) {
        // ══ پس‌زمینهٔ لاکچری: بافت چرمی آجیلی تمام‌صفحه + هالهٔ نور طلایی ══
        ScreenBackdrop(dark = isDark)
        SatinBackdrop()

        // ستون محتوا — با حاشیهٔ امن نوار سیستم روی هر گوشی
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenSafePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = fit.pad, vertical = if (fit.short) 8.dp else 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // روی صفحهٔ پهن (تبلت/گوشی بزرگ) محتوا وسط‌چین و محدود می‌شود
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fit.contentMax > 0.dp) Modifier.widthIn(max = fit.contentMax) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (fit.short) 4.dp else 10.dp))

                // ══════════ سرصفحهٔ برند — نشان سه‌بعدی M + نام سامانه ══════════
                BrandHeader(session = session, fit = fit, alpha = headA)

                Spacer(Modifier.height(fit.gap))

                // ══════════ کارت معرفی (هیرو) — تیتر گرادیانی + مدیریت ══════════
                HeroCard(fit = fit, alpha = heroA)

                Spacer(Modifier.height(fit.gap))

                // ══════════════════════════════════════════════════════════════
                //  گام ۱ — اتصال به سرور آتیران (تنظیم با ذخیره، سپس نقش)
                // ══════════════════════════════════════════════════════════════
                Box(Modifier.alpha(panelA)) {
                    ServerConnectPanel(
                        session = session,
                        onConfigure = onOpenServerConfig,
                        onQuickEnter = onQuickEnter,
                        fit = fit
                    )
                }

                Spacer(Modifier.height(fit.gap))

                // ══════════ گام ۲ — انتخاب نقش ورود ══════════
                Box(Modifier.alpha(rolesH)) {
                    PremiumSectionTitle(
                        text = "گام ۲ — انتخاب نقش ورود",
                        icon = Icons.Filled.WorkspacePremium,
                        hint = "نقش «مامور فروش» فعال است؛ بقیهٔ نقش‌ها به‌زودی فعال می‌شوند"
                    )
                }
                Spacer(Modifier.height(if (fit.short) 10.dp else 14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(rolesA),
                    verticalArrangement = Arrangement.spacedBy(if (fit.narrow) 8.dp else 10.dp)
                ) {
                    Roles.chunked(fit.roleColumns).forEach { rowRoles ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (fit.narrow) 8.dp else 10.dp)
                        ) {
                            rowRoles.forEach { role ->
                                RoleTile(
                                    role = role,
                                    avatar = fit.roleAvatar,
                                    fit = fit,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (role.active) onEnter()
                                        else onSoon("بخش «${role.title}» به‌زودی فعال می‌شود 🚀")
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(if (fit.short) 16.dp else 24.dp))

                // ══════════ فوتر امضای میلانو ══════════
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(footA)
                ) {
                    GoldFlourish(height = if (fit.dense) 14.dp else 18.dp)
                    Spacer(Modifier.height(if (fit.dense) 8.dp else 11.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LuxChip(
                            "گروه فنی و مهندسی میلانو",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.Verified,
                            textSize = fit.microText
                        )
                        LuxChip(
                            "Milad Yaghoobi",
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.WorkspacePremium,
                            tint = p.primary,
                            textSize = fit.microText
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LuxChip(
                        "Meelano Studio Design • نسخهٔ ۲٫۱۳٫۸ — اتصال مستقیم SQL (پورت ۱۴۳۳)",
                        icon = Icons.Filled.Shield,
                        tint = p.gold,
                        textSize = fit.microText
                    )
                }
            }
        }
    }
}

// ═══════════════════ پس‌زمینهٔ تمام‌صفحهٔ آجیلی (چرم) ═══════════════════

/**
 * پس‌زمینهٔ ثابت تمام‌صفحه: بافت چرمی/آجیلی در تم تیره و لایهٔ کرم روشن در تم
 * روشن، با پوشش گرادیانی که متن‌ها را خوانا نگه می‌دارد. تصویر با Crop پر می‌شود
 * تا روی هر نسبت‌تصویر (۱۸:۹، ۱۹٫۵:۹، ۴:۳ تبلت) بدون بریدگی زشت بنشیند.
 */
@Composable
private fun ScreenBackdrop(dark: Boolean) {
    val p = vizitorPalette
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(if (dark) R.drawable.leather_dark else R.drawable.leather_light),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // پوشش تم‌رنگ: تیره‌تر در تم شب، روشن‌تر در تم روز (خوانایی + حس لاکچری)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (dark) listOf(
                            p.background.copy(alpha = 0.86f),
                            p.background.copy(alpha = 0.72f),
                            p.surfaceDeep.copy(alpha = 0.90f)
                        ) else listOf(
                            p.background.copy(alpha = 0.90f),
                            p.background.copy(alpha = 0.80f),
                            p.background.copy(alpha = 0.94f)
                        )
                    )
                )
        )
    }
}

// ════════════════════════ سرصفحهٔ برند ════════════════════════

/** نشان سه‌بعدی M + مغز پسته در قاب جواهر + نام سامانه با تیتر گرادیانی. */
@Composable
private fun BrandHeader(session: ServerSession, fit: ScreenFit, alpha: Float) {
    val p = vizitorPalette
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = ((1f - alpha) * -16).dp)
    ) {
        BrandOrb(size = fit.brandOrb)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            GradientTitle(
                text = "آتیران ویزیتور",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = fit.titleSize
                ),
                textAlign = TextAlign.Start
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "سامانهٔ هوشمند ویزیت، ویترین و سفارش‌گیری",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (fit.narrow) 10.sp else 11.sp),
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // نشان وضعیت امنیت — همیشه سبز طلایی، پس از ورود «سپر تیک‌دار»
        IconOrb3D(
            icon = if (session.loggedIn) Icons.Filled.Verified else Icons.Filled.Shield,
            size = if (fit.narrow) 34.dp else 40.dp,
            tint = p.gold,
            glowColor = if (session.loggedIn) p.accent else p.primary
        )
    }
}

// ════════════════════════ کارت هیرو ════════════════════════

@Composable
private fun HeroCard(fit: ScreenFit, alpha: Float) {
    val p = vizitorPalette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .offset(y = ((1f - alpha) * -12).dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        p.primary.copy(alpha = 0.22f),
                        p.surface.copy(alpha = 0.36f),
                        Color.Transparent
                    )
                )
            )
            .shimmerSweep(color = p.goldHighlight.copy(alpha = 0.10f))
            .luxFrame(RoundedCornerShape(24.dp), p.gold, 0.62f, 1.2.dp)
            .ornaments(color = p.gold, alpha = 0.42f)
            .glassRelief(RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = if (fit.dense) 10.dp else 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconOrb3D(icon = Icons.Filled.ShoppingCart, size = 26.dp, cornerRadius = 9.dp)
            Spacer(Modifier.width(7.dp))
            Text(
                "فروش عمدهٔ آجیل و خشکبار",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = if (fit.narrow) 10.5.sp else 11.5.sp
                ),
                color = p.accentText
            )
            Spacer(Modifier.width(7.dp))
            IconOrb3D(icon = Icons.Filled.WorkspacePremium, size = 26.dp, cornerRadius = 9.dp)
        }
        Spacer(Modifier.height(8.dp))
        GradientTitle(
            text = "پخش عمدهٔ آجیل و خشکبار درخشان",
            colors = listOf(p.goldDark, p.gold, p.goldHighlight, p.gold),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = fit.heroSize,
                lineHeight = (fit.heroSize.value * 1.5f).sp
            )
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconOrb3D(icon = Icons.Filled.Verified, size = 24.dp, cornerRadius = 8.dp)
            Spacer(Modifier.width(7.dp))
            Text(
                "با مدیریت سرکار خانم حمدانی",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = if (fit.narrow) 11.sp else 12.sp
                ),
                color = p.accentText
            )
        }
        Spacer(Modifier.height(if (fit.dense) 7.dp else 10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LuxChip(
                "اتصال مستقیم SQL",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Dns,
                textSize = fit.microText
            )
            LuxChip(
                "پورت ۱۴۳۳",
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Sync,
                textSize = fit.microText
            )
            if (!fit.narrow) {
                LuxChip(
                    "دیتابیس آتیران",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Verified,
                    tint = p.primary,
                    textSize = fit.microText
                )
            }
        }
    }
}

// ════════════════════════ کاشی نقش ════════════════════════

/** کاشی نقش — گوی سه‌بعدی آواتار + رینگ طلایی + عنوان و وضعیت. */
@Composable
private fun RoleTile(
    role: Role,
    avatar: Dp,
    fit: ScreenFit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = vizitorPalette
    val accent = if (role.active) p.gold else Color(0x55FFFFFF)

    LuxuryTile(
        onClick = onClick,
        modifier = if (role.active) {
            modifier
                .clip(RoundedCornerShape(22.dp))
                .shimmerSweep(color = p.goldHighlight.copy(alpha = 0.09f), periodMillis = 3400)
        } else modifier,
        accent = accent,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.BottomEnd) {
                IconOrb3D(
                    size = avatar,
                    glowColor = if (role.active) p.primary else p.textSecondary,
                    cornerRadius = avatar / 2
                ) {
                    Image(
                        painter = painterResource(role.avatarRes),
                        contentDescription = role.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatar - 8.dp)
                            .clip(CircleShape)
                    )
                }
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (role.active) p.accentDark else p.surfaceDeep.copy(alpha = 0.92f))
                        .border(1.dp, Color.White.copy(alpha = if (role.active) 0.6f else 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (role.active) Icons.Filled.Login else Icons.Filled.Lock,
                        contentDescription = if (role.active) null else "به‌زودی",
                        tint = Color.White.copy(alpha = if (role.active) 1f else 0.8f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                role.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (fit.narrow) 11.5.sp else 12.5.sp
                ),
                color = if (role.active) p.textPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            Text(
                role.subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = if (fit.narrow) 9.sp else 10.sp),
                color = TextSecondary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(7.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (role.active) p.gold.copy(alpha = 0.16f)
                        else p.textSecondary.copy(alpha = 0.10f)
                    )
                    .border(
                        1.dp,
                        if (role.active) p.gold.copy(alpha = 0.45f) else Color.Transparent,
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 9.dp, vertical = 2.5.dp)
            ) {
                Text(
                    if (role.active) "ورود" else "به‌زودی",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (fit.narrow) 9.sp else 9.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (role.active) p.goldHighlight else TextSecondary
                )
            }
        }
    }
}

// ════════════════════════ پنل اتصال سرور (گام ۱) ════════════════════════

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  پنل «گام ۱ — اتصال به سرور آتیران» در صفحهٔ اول (پیش از انتخاب نقش)
 *  ─────────────────────────────────────────────────────────────────────────
 *  هوشمندی این پنل:
 *    • تنظیم‌نشده → کلید طلایی «تنظیم اتصال سرور» (کارت نصب‌کننده/دستی + ذخیرهٔ امن)
 *    • تنظیم‌شده  → کلید «ورود سریع» (نام کاربری/رمز ذخیره‌شده، بدون تایپ مجدد)
 *    • متصل       → نام کاربر و دیتابیس + شمارندهٔ سرویس‌ها + «ورود به پنل»
 *    • وضعیت همیشه با چیپ نورانی سبز/طلایی/بنفش دیده می‌شود
 * ═══════════════════════════════════════════════════════════════════════════
 */
@Composable
private fun ServerConnectPanel(
    session: ServerSession,
    onConfigure: () -> Unit,
    onQuickEnter: () -> Unit,
    fit: ScreenFit
) {
    val p = vizitorPalette
    val stateColor = when {
        session.loggedIn -> p.accent
        session.connected -> p.gold
        session.configured -> p.primary
        else -> p.danger
    }
    val stateText = when {
        session.loggedIn -> "متصل و آماده"
        session.connected -> "وصل به دیتابیس"
        session.configured -> "آمادهٔ ورود"
        else -> "تنظیم نشده"
    }
    val actionText = when {
        session.loggedIn -> "ورود به پنل"
        session.configured -> if (session.credentialsSaved) "ورود سریع به پنل" else "ورود و همگام‌سازی"
        else -> "تنظیم اتصال سرور"
    }
    val actionHint = when {
        session.loggedIn -> "همهٔ سرویس‌ها فعال است — وارد پنل شوید"
        session.configured -> "نام کاربری و رمز ذخیره‌شده، بدون تایپ مجدد"
        else -> "کارت اتصال نصب‌کننده یا ورود دستی — یک‌بار برای همیشه"
    }

    PremiumPanel(
        title = "گام ۱ — اتصال به سرور آتیران",
        hint = actionHint,
        icon = Icons.Filled.Dns,
        step = 1,
        accent = stateColor,
        inner = fit.inner,
        trailing = { GlowChip(text = stateText, color = stateColor, pulse = session.busy || session.syncing) }
    ) {
        // ── خلاصهٔ وضعیت: سرور / دیتابیس / کاربر / آخرین همگام‌سازی ──
        KeyRow(
            label = "سرور",
            value = session.serverLabel,
            icon = Icons.Filled.Dns
        )
        if (session.erpName.isNotBlank() || session.erpUser.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            KeyRow(
                label = "کاربر سامانه",
                value = session.erpName.ifBlank { session.erpUser },
                icon = Icons.Filled.Verified
            )
        }
        if (session.loggedIn) {
            Spacer(Modifier.height(5.dp))
            KeyRow(
                label = "آخرین همگام‌سازی",
                value = if (session.lastSyncAt == 0L) "در حال آماده‌سازی" else session.lastSyncSummary,
                icon = Icons.Filled.Sync
            )
        }

        // ── شمارندهٔ سرویس‌های فعال (پس از ورود) ──
        if (session.loggedIn || session.productsCount > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatPill(session.productsCount, "کالا", modifier = Modifier.weight(1f), icon = Icons.Filled.ShoppingCart, tint = p.gold)
                StatPill(session.customersCount, "مشتری", modifier = Modifier.weight(1f), icon = Icons.Filled.Verified, tint = p.primary)
                StatPill(session.invoicesCount, "فاکتور", modifier = Modifier.weight(1f), icon = Icons.Filled.WorkspacePremium, tint = p.accent)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── کلید اصلی (سه‌بعدی) + کلید تنظیمات ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            PremiumButton(
                text = actionText,
                subtitle = null,
                icon = if (session.loggedIn) Icons.Filled.Login else Icons.Filled.Dns,
                tone = if (session.loggedIn || session.configured) BtnTone.GOLD else BtnTone.PRIMARY,
                height = fit.buttonHeight,
                textSize = fit.buttonText,
                enabled = !session.busy && !session.syncing,
                loading = session.busy || session.syncing,
                onClick = {
                    when {
                        session.loggedIn -> onQuickEnter()
                        session.configured -> onQuickEnter()
                        else -> onConfigure()
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            OrbIconButton(
                icon = Icons.Filled.Settings,
                onClick = onConfigure,
                size = fit.buttonHeight,
                contentDescription = "تنظیمات اتصال و همگام‌سازی"
            )
        }

        // ── پیام آخرین عملیات (فارسی و رنگی) ──
        if (session.message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            LuxBanner(
                tone = when (session.messageKind) {
                    1 -> LuxTone.SUCCESS
                    2 -> LuxTone.DANGER
                    else -> LuxTone.INFO
                },
                title = when (session.messageKind) {
                    1 -> "انجام شد"
                    2 -> "نیازمند رسیدگی"
                    else -> "اطلاع"
                },
                message = session.message,
                compact = fit.dense
            )
        }

        // ── پیوند به نقش فعال (اگر همه‌چیز آماده است) ──
        if (session.loggedIn) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconOrb3D(icon = Icons.Filled.Verified, size = 24.dp, cornerRadius = 8.dp)
                Spacer(Modifier.width(7.dp))
                Text(
                    "آمادهٔ ورود — روی نقش «مامور فروش» بزنید",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
                    color = p.accentText
                )
            }
        }
    }
}
