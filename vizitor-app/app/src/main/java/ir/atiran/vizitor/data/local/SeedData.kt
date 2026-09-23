/*
 * ═══════════════════════════════════════════════════════════════════════════
 *  Vizitor — آتیران ویزیتور | داده نمونه اولیه (Demo Seed)
 *  Developed by Milano Technical Team, Milad Yaghoobi
 *  ─────────────────────────────────────────────────────────────────────────
 *  فقط در اولین اجرا (حالت دمو) تا زمان اتصال به سرور آتیران کاشته می‌شود.
 *  دامنه کسب‌وکار: پخش آجیل و خشکبار
 * ═══════════════════════════════════════════════════════════════════════════
 */
package ir.atiran.vizitor.data.local

/** پرداخت/واریز نمونه برای گردش حساب مشتری. */
data class PaymentSeed(val customerId: Int, val amount: Long, val daysAgo: Int)

object SeedData {

    val products = listOf(
        ProductEntity(1, "6260201000011", "پسته اکبری فندقی درجه یک", "پسته", 1_850_000, 42.0, "🥜", unit = "کیلو", packSize = 10, price2 = 1_920_000, consumerPrice = 2_250_000),
        ProductEntity(2, "6260201000028", "بادام هندی ویتنامی W240", "مغز آجیل", 980_000, 118.0, "🌰", unit = "کیلو", packSize = 20, price2 = 1_020_000, consumerPrice = 1_180_000),
        ProductEntity(3, "6260201000035", "پسته احمدآقایی خندان", "پسته", 1_650_000, 96.0, "🥜", unit = "کیلو", packSize = 10, price2 = 1_710_000, consumerPrice = 2_050_000),
        ProductEntity(4, "6260201000042", "فندق تازه توخالی", "مغز آجیل", 1_150_000, 63.0, "🌰", unit = "کیلو", packSize = 15, price2 = 1_190_000, consumerPrice = 1_390_000),
        ProductEntity(5, "6260201000059", "مغز گردوی کاغذی چهارمحال", "مغز آجیل", 1_320_000, 55.0, "🧠", unit = "کیلو", packSize = 12, price2 = 1_370_000, consumerPrice = 1_620_000),
        ProductEntity(6, "6260201000066", "کشمش طلایی ملایر ممتاز", "خشکبار", 385_000, 140.0, "🍇", unit = "کیلو", packSize = 25, price2 = 399_000, consumerPrice = 479_000),
        ProductEntity(7, "6260201000073", "خرما مضافتی بم سوپر", "خشکبار", 1_250_000, 22.0, "🌴", isVip = true, unit = "کارتن ۵ کیلو", packSize = 5, price2 = 1_290_000, consumerPrice = 1_550_000),
        ProductEntity(8, "6260201000080", "آجیل شب یلدایی مخلوط لوکس", "آجیل مخلوط", 1_980_000, 48.0, "🎁", isVip = true, unit = "کیلو", packSize = 8, price2 = 2_050_000, consumerPrice = 2_460_000),
        ProductEntity(9, "6260201000097", "توت خشک درشت ملارد", "خشکبار", 460_000, 87.0, "🫐", unit = "کیلو", packSize = 20, price2 = 475_000, consumerPrice = 570_000),
        ProductEntity(10, "6260201000103", "انجیر خشک استعناب", "خشکبار", 540_000, 15.0, "🍯", isVip = true, unit = "بسته ۵۰۰ گرم", packSize = 2, price2 = 560_000, consumerPrice = 672_000),
        ProductEntity(11, "6260201000110", "تخمه کدو گرامی بو داده", "تخمه", 720_000, 74.0, "🎃", unit = "کیلو", packSize = 16, price2 = 745_000, consumerPrice = 894_000),
        ProductEntity(12, "6260201000127", "بادام درختی سنگی خام", "مغز آجیل", 890_000, 102.0, "🌰", unit = "کیلو", packSize = 20, price2 = 920_000, consumerPrice = 1_104_000),
    ).map { p ->
        // دستهٔ پیشنهادی + میانگین قیمت هر قلم (همان قواعدی که روی دادهٔ واقعی سرور اجرا می‌شود)
        p.copy(
            category = ir.atiran.vizitor.sqldirect.ProductDefaults.categoryOf(p.name),
            avgPrice = ir.atiran.vizitor.sqldirect.ProductDefaults.averageOf(
                listOf(p.price, p.price2, p.consumerPrice)
            ),
        )
    }

    val customers = listOf(
        CustomerEntity(1, "C-1001", "آجیل و خشکبار برادران رحیمی", "عمده", "تهران", "بازار بزرگ، دالان زرگرها، پلاک ۱۲", "09121112233", 35.6710, 51.4200, true, true, 3, 5, 18_500_000),
        CustomerEntity(2, "C-1002", "خشکبار سرای الماس کرج", "نیمه‌عمده", "کرج", "بلوار طالقانی، نبش ۸", "09123445566", 35.8327, 50.9915, true, false, 12, 30, 64_200_000),
        CustomerEntity(3, "C-1003", "سوپرمارکت آفتاب اکبر", "خرده", "تهران", "خیابان شوش، کوچه لاله", "09354556677", 35.6602, 51.4230, false, false, 25, 55, 132_750_000),
        CustomerEntity(4, "C-1004", "پخش آجیل آریا قزوین", "عمده", "قزوین", "خیابان امام، مجتمع تجاری آریا", "09127778899", 36.2680, 50.0040, true, true, 6, 10, 0),
        CustomerEntity(5, "C-1005", "خشکبار سعادت", "خرده", "تهران", "سعادت‌آباد، سرو غربی", "09191234567", 35.7760, 51.3700, false, false, 40, 70, 87_400_000),
        CustomerEntity(6, "C-1006", "بازرگانی خشکبار مهرگان", "عمده", "اصفهان", "خیابان چهارباغ بالا", "09131112244", 32.6546, 51.6680, true, false, 15, 35, 41_300_000)
    )

    /** واریزها/پرداخت‌های نمونه برای گردش حساب. */
    val payments = listOf(
        PaymentSeed(1, 25_000_000, 9),
        PaymentSeed(1, 10_000_000, 2),
        PaymentSeed(2, 30_000_000, 14),
        PaymentSeed(3, 20_000_000, 30),
        PaymentSeed(3, 15_000_000, 12),
        PaymentSeed(5, 10_000_000, 21),
        PaymentSeed(6, 50_000_000, 7)
    )

    val salMali = listOf(
        SalMaliHistoryEntity(customerId = 1, productName = "پسته اکبری فندقی درجه یک", totalQty = 96.0, yearMonth = "1404-04"),
        SalMaliHistoryEntity(customerId = 1, productName = "کشمش طلایی ملایر ممتاز", totalQty = 120.0, yearMonth = "1404-04"),
        SalMaliHistoryEntity(customerId = 1, productName = "آجیل شب یلدایی مخلوط لوکس", totalQty = 64.0, yearMonth = "1404-05"),
        SalMaliHistoryEntity(customerId = 2, productName = "بادام هندی ویتنامی W240", totalQty = 30.0, yearMonth = "1404-03"),
        SalMaliHistoryEntity(customerId = 2, productName = "مغز گردوی کاغذی چهارمحال", totalQty = 18.0, yearMonth = "1404-04"),
        SalMaliHistoryEntity(customerId = 4, productName = "خرما مضافتی بم سوپر", totalQty = 12.0, yearMonth = "1404-05"),
        SalMaliHistoryEntity(customerId = 4, productName = "انجیر خشک استعناب", totalQty = 24.0, yearMonth = "1404-05"),
        SalMaliHistoryEntity(customerId = 3, productName = "تخمه کدو گرامی بو داده", totalQty = 8.0, yearMonth = "1404-02")
    )
}
