package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeName: String = "عوادم التيربو",
    val verifiedBadge: Boolean = true,
    val tagline: String = "أنظمة العادم دبات التلوث",
    val customAppNameUnderBadge: String = "تطبيق عوادم التيربو الخاص",
    val websiteUrl: String = "https://turboexhaust.sa",
    val themeColorKey: String = "emerald",
    val chatThemeKey: String = "turbo_fire",
    val instagramHandle: String = "@turbo_exhaust_sa",
    val tiktokHandle: String = "@turbo.exhaust",
    val appDownloadUrl: String = "https://turboexhaust.app/store",
    val defaultShippingCompany: String = "أرامكس (Aramex)",
    val defaultEstimatedDays: Long = 3L,
    val activeWallpaperIndex: Long = 0L,
    val selectedAlertSound: String = "turbo",
    val subscriptionPlan: String = "trial",
    val subscriptionActive: Boolean = true,
    val subscriptionDaysLeft: Long = 5L,
    val subscriptionSmsCode: String = "",
    val customerPhoneForSms: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CatalogProduct(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "أنظمة العادم",
    val priceSar: Double = 0.0,
    val imageKey: String = "exhaust_sport",
    val videoTitle: String = "",
    val videoDurationSec: Long = 0L,
    val seoKeywords: List<String> = emptyList(),
    val inStock: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val lastMessage: String = "",
    @get:PropertyName("isInternetBlocked")
    @set:PropertyName("isInternetBlocked")
    var isInternetBlocked: Boolean = false,
    val alertSound: String = "turbo",
    val wallpaperIndex: Long = 0L,
    val chatThemeKey: String = "turbo_fire",
    val shippingCustomerName: String = "",
    val shippingAddress: String = "",
    val shippingRegion: String = "",
    val shippingCompany: String = "أرامكس (Aramex)",
    val shippingDaysRemaining: Long = 3L,
    val waybillNumber: String = "",
    val waybillStatus: String = "pending",
    val activeAdTitle: String = "",
    val activeAdSubtitle: String = "",
    val activeAdProductId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val chatId: String = "",
    val senderRole: String = "owner",
    val messageType: String = "text",
    val text: String = "",
    val soundType: String = "",
    val productId: String = "",
    val adTitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class EngineAlertSound(
    val key: String,
    val labelAr: String,
    val subtitleAr: String,
) {
    POPS_BANGS("pops_bangs", "انفجار وفرقعة", "فرقعة عادم وتفجير رالي قوي"),
    REV_LIMITER("rev_limiter", "تقطيع المكينة", "تفتفة وتقطيع آر بي إم عالي"),
    ROCKET("rocket", "صاروخ مسرع", "انطلاقة صاروخ بسرعة فائقة"),
    SIREN_SCREAM("siren_scream", "صرخة حماسية", "صفير حاد وصرخة تيربو سريعة"),
    DRAG_LAUNCH("drag_launch", "انطلاقة سباق", "لانش كنترول وتسارع كفرات"),
    MOTORCYCLE("motorcycle", "دباب مسرع", "تسارع دباب ريس ناري"),
    SPORTBIKE("sportbike", "دراجة نارية", "صوت أكزوز دراجة نارية رياضي"),
    TURBO("turbo", "سيارة تيربو", "تيربو مع بلف تنفيس Blow-off"),
    V8("v8", "عادم رياضي V8", "هدير عادم رياضي عميق"),
    ;

    companion object {
        fun fromKey(key: String): EngineAlertSound =
            entries.firstOrNull { it.key == key } ?: TURBO
    }
}

enum class StoreThemeOption(
    val key: String,
    val labelAr: String,
) {
    EMERALD("emerald", "أخضر الأعمال"),
    CRIMSON("crimson", "أحمر التيربو"),
    TITANIUM("titanium", "أزرق التيتانيوم"),
    AMBER("amber", "ذهبي رياضي"),
    VIOLET("violet", "بنفسجي ليلي"),
    ;

    companion object {
        fun fromKey(key: String): StoreThemeOption =
            entries.firstOrNull { it.key == key } ?: EMERALD
    }
}

enum class ChatThemePreset(
    val key: String,
    val labelAr: String,
) {
    TURBO_FIRE("turbo_fire", "تيم تيربو الناري"),
    CLASSIC_WA("classic_wa", "تيم واتساب الكلاسيكي"),
    CARBON_GOLD("carbon_gold", "تيم الكربون الذهبي"),
    NEON_NIGHT("neon_night", "تيم الليل الرياضي"),
    ;

    companion object {
        fun fromKey(key: String): ChatThemePreset =
            entries.firstOrNull { it.key == key } ?: TURBO_FIRE
    }
}

enum class SubscriptionPlan(
    val key: String,
    val labelAr: String,
    val priceSar: Int,
    val durationDays: Long,
    val monthsCount: Int,
) {
    FREE_TRIAL("trial", "تجربة مجانية 5 أيام", 0, 5L, 0),
    ONE_MONTH("1_month", "شهر واحد (25 ر.س)", 25, 30L, 1),
    TWO_MONTHS("2_months", "شهرين (50 ر.س)", 50, 60L, 2),
    THREE_MONTHS("3_months", "ثلاثة أشهر (75 ر.س)", 75, 90L, 3),
    ONE_YEAR("12_months", "سنة كاملة (300 ر.س)", 300, 365L, 12),
    ;

    companion object {
        fun fromKey(key: String): SubscriptionPlan =
            entries.firstOrNull { it.key == key } ?: FREE_TRIAL
    }
}

enum class CustomerSubStatus(
    val key: String,
    val labelAr: String,
) {
    ACTIVE("active", "نشط"),
    EXPIRING_SOON("expiring_soon", "قريب الانتهاء"),
    EXPIRED("expired", "منتهي"),
    PAUSED("paused", "متوقف مؤقتاً"),
    ;

    companion object {
        fun fromKey(key: String): CustomerSubStatus =
            entries.firstOrNull { it.key == key } ?: ACTIVE
    }
}

fun computeSubscriptionStatus(
    remainingDays: Long,
    thresholdDays: Long,
    paused: Boolean = false,
): String = when {
    paused -> CustomerSubStatus.PAUSED.key
    remainingDays <= 0L -> CustomerSubStatus.EXPIRED.key
    remainingDays <= thresholdDays -> CustomerSubStatus.EXPIRING_SOON.key
    else -> CustomerSubStatus.ACTIVE.key
}

@IgnoreExtraProperties
data class CustomerSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val chatId: String = "",
    val planKey: String = SubscriptionPlan.ONE_MONTH.key,
    val planLabelAr: String = SubscriptionPlan.ONE_MONTH.labelAr,
    val priceSar: Double = 25.0,
    val remainingDays: Long = 30L,
    val alertThresholdDays: Long = 3L,
    val status: String = CustomerSubStatus.ACTIVE.key,
    val autoSmsAlertEnabled: Boolean = true,
    val lastSmsAlertText: String = "",
    val smsAlertCount: Long = 0L,
    val lastAlertSentAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SmsAlertLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subscriptionId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val messageBody: String = "",
    val triggerType: String = "auto_expiry",
    val remainingDaysAtSend: Long = 0L,
    val deliveryStatus: String = "queued_auto",
    @ServerTimestamp val createdAt: Timestamp? = null,
)
