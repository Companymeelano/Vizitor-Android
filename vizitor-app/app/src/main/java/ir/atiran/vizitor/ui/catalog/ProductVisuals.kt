/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | تصاویر پیش‌فرض کالاها بر اساس «نام کالا»
 *  Developed by Milad Yaghoobi — Meelano Studio Design
 *  ─────────────────────────────────────────────────────────────────────────
 *  ویترین باید برای هر کالا یک تصویر واقعی نشان بدهد، حتی وقتی سرور تصویر
 *  کالا ندارد. این فایل یک «فهرست پیش‌فرض تصاویر» است: بر اساس کلیدواژه‌های
 *  نام کالا، مناسب‌ترین تصویر بستهٔ برنامه انتخاب می‌شود (تصاویر در
 *  res/drawable-nodpi هستند و حجم APK را هم زیاد نمی‌کنند).
 *
 *  قاعده: هر کلیدواژه‌ای که اول بیاید، اول برنده است — پس ترکیب‌های خاص
 *  («بادام هندی») قبل از عام‌ها («بادام») آمده‌اند.
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.ui.catalog

import androidx.annotation.DrawableRes
import ir.atiran.vizitor.R

object ProductImages {

    /** ریشهٔ تصاویر در res/drawable-nodpi (عکس‌های استودیویی سبک‌سازی‌شده، ~۶۵ کیلوبایت هر کدام) */
    @DrawableRes private val PISTACHIO = R.drawable.goods_pistachio
    @DrawableRes private val ALMOND = R.drawable.goods_almond
    @DrawableRes private val WALNUT = R.drawable.goods_walnut
    @DrawableRes private val CASHEW = R.drawable.goods_cashew
    @DrawableRes private val RAISIN = R.drawable.goods_raisin
    @DrawableRes private val DATE = R.drawable.goods_date
    @DrawableRes private val FIG = R.drawable.goods_fig
    @DrawableRes private val SEEDS = R.drawable.goods_seeds
    @DrawableRes private val SAFFRON = R.drawable.goods_saffron
    @DrawableRes private val HONEY = R.drawable.goods_honey
    @DrawableRes private val MIXED = R.drawable.goods_mixed

    /**
     * قواعد نام → تصویر پیش‌فرض (ترتیب مهم است: ترکیب‌های خاص قبل از واژه‌های عام).
     * دو گروه «تنقلات/شیرینی» و «نوشیدنی» به نزدیک‌ترین عکس محصول نگاشت شده‌اند
     * (سینی میوهٔ خشک و شیشهٔ مایع طلایی) و در صورت افزودن عکس اختصاصی، فقط
     * یک ردیف از همین جدول عوض می‌شود.
     */
    private val rules: List<Pair<List<String>, Int>> = listOf(
        listOf("بادام هندی", "کاشو", "بادام‌هندی") to CASHEW,
        listOf("فندق", "نخودچی", "چلغوز") to CASHEW,
        listOf("پسته") to PISTACHIO,
        listOf("بادام") to ALMOND,
        listOf("گردو") to WALNUT,
        listOf("کشمش", "مویز", "توت خشک") to RAISIN,
        listOf("خرما", "رطب") to DATE,
        listOf("انجیر", "برگه", "قیسی", "آلو", "زردآلو", "خشکبار") to FIG,
        listOf("شکلات", "آبنبات", "پاستیل", "بیسکویت", "کیک", "تنقلات", "قند", "شکر", "شیرینی") to FIG,
        listOf("تخمه", "آفتابگردان", "کدو", "شاهدانه", "هندوانه", "ژاپنی") to SEEDS,
        listOf("زعفران", "هل", "دارچین", "زردچوبه", "فلفل", "ادویه", "سماق", "زنجبیل") to SAFFRON,
        listOf("عسل", "ارده", "حلوا", "کره", "شیره", "نوشابه", "آبمیوه", "شربت", "چای", "قهوه", "دمنوش") to HONEY,
        listOf("آجیل", "مغز", "مخلوط") to MIXED,
    )

    /** تصویر پیش‌فرض برای یک نام کالا (اگر چیزی پیدا نشد: آجیل مخلوط). */
    @DrawableRes
    fun forName(name: String): Int {
        val n = name.trim()
        if (n.isNotEmpty()) {
            rules.forEach { (keys, res) ->
                if (keys.any { n.contains(it, ignoreCase = true) }) return res
            }
        }
        return MIXED
    }

    /** ایموجی جایگزین، برای وقتی که تصویر رسمی کالا از سرور بیاید (مسیر آینده). */
    fun emojiFor(name: String): String {
        val n = name.trim()
        return when {
            n.contains("پسته") -> "🌰"
            n.contains("بادام") -> "🥜"
            n.contains("گردو") -> "🌰"
            n.contains("کشمش") || n.contains("مویز") -> "🍇"
            n.contains("خرما") -> "🌴"
            n.contains("انجیر") -> "🍈"
            n.contains("تخمه") || n.contains("کدو") -> "🌻"
            n.contains("زعفران") || n.contains("ادویه") -> "🌸"
            n.contains("عسل") -> "🍯"
            n.contains("چای") || n.contains("قهوه") -> "🍵"
            n.contains("شکلات") || n.contains("شیرینی") -> "🍫"
            else -> "📦"
        }
    }
}
