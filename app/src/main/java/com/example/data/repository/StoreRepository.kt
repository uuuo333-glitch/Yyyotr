package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CatalogProduct
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.CustomerSubStatus
import com.example.data.model.CustomerSubscription
import com.example.data.model.SmsAlertLog
import com.example.data.model.StoreSettings
import com.example.data.model.SubscriptionPlan
import com.example.data.model.computeSubscriptionStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب جوجل قبل الوصول إلى قاعدة البيانات.")

    fun observeStoreSettings(userId: String): Flow<StoreSettings?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("store_settings").document(DEFAULT_SETTINGS_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            StoreSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun ensureDefaultStoreSeeded() {
        val uid = requireUserId()
        val settingsRef = db.collection("users").document(uid)
            .collection("store_settings").document(DEFAULT_SETTINGS_ID)
        val settingsSnap = try {
            settingsRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, settingsRef.path, auth)
            throw e
        }
        if (!settingsSnap.exists()) {
            saveStoreSettings(
                StoreSettings(
                    id = DEFAULT_SETTINGS_ID,
                    userId = uid,
                    storeName = "عوادم التيربو",
                    verifiedBadge = true,
                    tagline = "أنظمة العادم دبات التلوث",
                    customAppNameUnderBadge = "تطبيق عوادم التيربو الخاص",
                    websiteUrl = "https://turboexhaust.sa",
                    themeColorKey = "emerald",
                    chatThemeKey = "turbo_fire",
                    instagramHandle = "@turbo_exhaust_sa",
                    tiktokHandle = "@turbo.exhaust",
                    appDownloadUrl = "https://turboexhaust.app/download",
                    defaultShippingCompany = "أرامكس (Aramex)",
                    defaultEstimatedDays = 3L,
                    activeWallpaperIndex = 0L,
                    selectedAlertSound = "turbo",
                    subscriptionPlan = "trial",
                    subscriptionActive = true,
                    subscriptionDaysLeft = 5L,
                ),
                isCreate = true,
            )
        }

        val productsCol = db.collection("users").document(uid).collection("products")
        val existingProducts = try {
            productsCol.limit(1).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, productsCol.path, auth)
            throw e
        }
        if (existingProducts.isEmpty) {
            val starterProducts = listOf(
                CatalogProduct(
                    id = "prod_cat_304",
                    userId = uid,
                    name = "دبة تلوث رياضية فولاذ 304",
                    description = "دبة تلوث عالية التدفق 200 خلية تقلل الكتمة وتعالج لمبة المكينة P0420 وتمنح صوتاً متزناً.",
                    category = "دبات التلوث",
                    priceSar = 850.0,
                    imageKey = "catalytic_converter",
                    videoTitle = "تجربة تدفق دبة التلوث قبل وبعد التركيب",
                    videoDurationSec = 42L,
                    seoKeywords = listOf("دبة تلوث", "كتاليزر رياضي", "علاج لمبة المكينة", "P0420", "عوادم السعودية"),
                    inStock = true,
                ),
                CatalogProduct(
                    id = "prod_ti_dual",
                    userId = uid,
                    name = "كتباك تيتانيوم مخرجين Valvetronic",
                    description = "نظام عادم كامل من التيتانيوم مع صمام تحكم بالريموت لفتح وإغلاق الصوت الرياضي.",
                    category = "أنظمة العادم",
                    priceSar = 3450.0,
                    imageKey = "exhaust_sport",
                    videoTitle = "صوت التيتانيوم مع فتح الصمام الرياضي",
                    videoDurationSec = 58L,
                    seoKeywords = listOf("كتباك تيتانيوم", "دبة رياضية", "اقزوز", "صوت رياضي", "تعديل سيارات"),
                    inStock = true,
                ),
                CatalogProduct(
                    id = "prod_downpipe_turbo",
                    userId = uid,
                    name = "داون بايب تيربو ستانلس ستيل",
                    description = "داون بايب مصمم لسيارات التيربو لزيادة استجابة التيربو وتقليل حرارة العادم.",
                    category = "تيربو وداون بايب",
                    priceSar = 1490.0,
                    imageKey = "store_banner",
                    videoTitle = "تسارع سيارة تيربو بعد تركيب الداون بايب",
                    videoDurationSec = 35L,
                    seoKeywords = listOf("داون بايب", "تيربو", "سيارة تيربو", "عزم", "عوادم تيربو"),
                    inStock = true,
                ),
                CatalogProduct(
                    id = "prod_video_showcase",
                    userId = uid,
                    name = "فيديو تجربة دبة البيئة والأصوات الرياضية",
                    description = "مقطع استعراضي يوضح صوت الدباب المسرع والسيارة التيربو وجودة اللحام.",
                    category = "مقاطع الفيديو",
                    priceSar = 690.0,
                    imageKey = "exhaust_sport",
                    videoTitle = "استعراض أصوات العوادم الحقيقية بالورشة",
                    videoDurationSec = 75L,
                    seoKeywords = listOf("فيديو دبة تلوث", "صوت اقزوز", "تيك توك عوادم", "انستقرام تعديل"),
                    inStock = true,
                ),
            )
            starterProducts.forEach { createProduct(it, explicitId = it.id) }
        }

        val chatsCol = db.collection("users").document(uid).collection("chats")
        val existingChats = try {
            chatsCol.limit(1).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, chatsCol.path, auth)
            throw e
        }
        if (existingChats.isEmpty) {
            val chatId = "chat_demo_1"
            createChat(
                ChatConversation(
                    id = chatId,
                    userId = uid,
                    customerName = "فهد القحطاني",
                    customerPhone = "+966 55 412 8890",
                    lastMessage = "تم تجهيز بوليصة الشحن الخاصة بطلبك",
                    isInternetBlocked = false,
                    alertSound = "pops_bangs",
                    wallpaperIndex = 0L,
                    chatThemeKey = "turbo_fire",
                    shippingCustomerName = "فهد القحطاني",
                    shippingAddress = "حي الياسمين، طريق أنس بن مالك",
                    shippingRegion = "الرياض",
                    shippingCompany = "أرامكس (Aramex)",
                    shippingDaysRemaining = 2L,
                    waybillNumber = "WB-40921",
                    waybillStatus = "shipped",
                    activeAdTitle = "عرض دبات التلوث الأصلية والرياضية",
                    activeAdSubtitle = "خصم 15% على دبات التلوث مع شحن سريع لجميع المناطق",
                    activeAdProductId = "prod_cat_304",
                ),
                explicitId = chatId,
            )
            sendMessage(
                chatId = chatId,
                senderRole = "customer",
                messageType = "text",
                text = "السلام عليكم، اسمي فهد القحطاني من الرياض حي الياسمين طريق أنس بن مالك، أبغى دبة تلوث أصلية، كم السعر؟",
            )
            sendMessage(
                chatId = chatId,
                senderRole = "owner",
                messageType = "ad",
                text = "خصم 15% على دبات التلوث فولاذ 304 عالية التدفق – ضمان 3 سنوات",
                productId = "prod_cat_304",
                adTitle = "إعلان المتجر الخاص • دبات التلوث",
            )
        }

        val subsCol = db.collection("users").document(uid).collection("customer_subscriptions")
        val existingSubs = try {
            subsCol.limit(1).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, subsCol.path, auth)
            throw e
        }
        if (existingSubs.isEmpty) {
            val starterSubs = listOf(
                CustomerSubscription(
                    id = "sub_fahad_1",
                    userId = uid,
                    customerName = "فهد القحطاني",
                    customerPhone = "+966554128890",
                    chatId = "chat_demo_1",
                    planKey = SubscriptionPlan.ONE_MONTH.key,
                    planLabelAr = SubscriptionPlan.ONE_MONTH.labelAr,
                    priceSar = 25.0,
                    remainingDays = 2L,
                    alertThresholdDays = 3L,
                    status = CustomerSubStatus.EXPIRING_SOON.key,
                    autoSmsAlertEnabled = true,
                    lastSmsAlertText = "",
                    smsAlertCount = 0L,
                ),
                CustomerSubscription(
                    id = "sub_khalid_2",
                    userId = uid,
                    customerName = "خالد العتيبي",
                    customerPhone = "+966501112233",
                    chatId = "",
                    planKey = SubscriptionPlan.THREE_MONTHS.key,
                    planLabelAr = SubscriptionPlan.THREE_MONTHS.labelAr,
                    priceSar = 75.0,
                    remainingDays = 24L,
                    alertThresholdDays = 5L,
                    status = CustomerSubStatus.ACTIVE.key,
                    autoSmsAlertEnabled = true,
                    lastSmsAlertText = "",
                    smsAlertCount = 0L,
                ),
                CustomerSubscription(
                    id = "sub_saad_3",
                    userId = uid,
                    customerName = "سعد الدوسري",
                    customerPhone = "+966549988776",
                    chatId = "",
                    planKey = SubscriptionPlan.FREE_TRIAL.key,
                    planLabelAr = SubscriptionPlan.FREE_TRIAL.labelAr,
                    priceSar = 0.0,
                    remainingDays = 0L,
                    alertThresholdDays = 3L,
                    status = CustomerSubStatus.EXPIRED.key,
                    autoSmsAlertEnabled = true,
                    lastSmsAlertText = "انتهت فترة التجربة المجانية في عوادم التيربو — جدد باقة 25 ر.س شهرياً.",
                    smsAlertCount = 1L,
                ),
            )
            starterSubs.forEach { upsertCustomerSubscription(it, explicitId = it.id) }
        }
    }

    suspend fun saveStoreSettings(settings: StoreSettings, isCreate: Boolean = false) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("store_settings").document(DEFAULT_SETTINGS_ID)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "storeName" to settings.storeName.trim().ifEmpty { "عوادم التيربو" },
            "verifiedBadge" to settings.verifiedBadge,
            "tagline" to settings.tagline.trim().ifEmpty { "أنظمة العادم دبات التلوث" },
            "customAppNameUnderBadge" to settings.customAppNameUnderBadge.trim().ifEmpty { "تطبيق عوادم التيربو الخاص" },
            "websiteUrl" to settings.websiteUrl.trim().ifEmpty { "https://turboexhaust.sa" },
            "themeColorKey" to settings.themeColorKey,
            "chatThemeKey" to settings.chatThemeKey,
            "instagramHandle" to settings.instagramHandle.trim(),
            "tiktokHandle" to settings.tiktokHandle.trim(),
            "appDownloadUrl" to settings.appDownloadUrl.trim(),
            "defaultShippingCompany" to settings.defaultShippingCompany.trim(),
            "defaultEstimatedDays" to settings.defaultEstimatedDays.coerceIn(0L, 90L),
            "activeWallpaperIndex" to settings.activeWallpaperIndex.coerceIn(0L, 10L),
            "selectedAlertSound" to settings.selectedAlertSound,
            "subscriptionPlan" to settings.subscriptionPlan,
            "subscriptionActive" to settings.subscriptionActive,
            "subscriptionDaysLeft" to settings.subscriptionDaysLeft.coerceIn(0L, 366L),
            "subscriptionSmsCode" to settings.subscriptionSmsCode.trim(),
            "customerPhoneForSms" to settings.customerPhoneForSms.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            val existingSnap = docRef.get().await()
            if (isCreate || !existingSnap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                payload.remove("userId")
                docRef.update(payload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    fun observeProducts(userId: String): Flow<List<CatalogProduct>> = flow {
        val query = db.collection("users").document(userId).collection("products")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CatalogProduct::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, "users/$userId/products", userId)
                    throw error
                }
        )
    }

    suspend fun createProduct(product: CatalogProduct, explicitId: String? = null): String {
        val uid = requireUserId()
        val id = explicitId ?: "prod_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("products").document(id)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "name" to product.name.trim(),
            "description" to product.description.trim(),
            "category" to product.category.trim(),
            "priceSar" to product.priceSar.coerceAtLeast(0.0),
            "imageKey" to product.imageKey.ifBlank { "exhaust_sport" },
            "seoKeywords" to product.seoKeywords.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
            "inStock" to product.inStock,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (product.videoTitle.isNotBlank()) {
            payload["videoTitle"] = product.videoTitle.trim()
            payload["videoDurationSec"] = product.videoDurationSec.coerceIn(0L, 3600L)
        }
        try {
            docRef.set(payload).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateProductKeywords(productId: String, keywords: List<String>) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        val clean = keywords.map { it.trim() }.filter { it.isNotEmpty() }.take(15)
        try {
            docRef.update(
                mapOf(
                    "seoKeywords" to clean,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteProduct(productId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("products").document(productId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    fun observeChats(userId: String): Flow<List<ChatConversation>> = flow {
        val query = db.collection("users").document(userId).collection("chats")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatConversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, "users/$userId/chats", userId)
                    throw error
                }
        )
    }

    fun observeChat(userId: String, chatId: String): Flow<ChatConversation?> = flow {
        val docRef = db.collection("users").document(userId).collection("chats").document(chatId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            ChatConversation::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, docRef.path, userId)
                    throw error
                }
        )
    }

    suspend fun createChat(chat: ChatConversation, explicitId: String? = null): String {
        val uid = requireUserId()
        val id = explicitId ?: "chat_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("chats").document(id)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "customerName" to chat.customerName.trim().ifEmpty { "عميل جديد" },
            "customerPhone" to chat.customerPhone.trim(),
            "lastMessage" to chat.lastMessage.trim().ifEmpty { "بدأت محادثة جديدة" },
            "isInternetBlocked" to chat.isInternetBlocked,
            "alertSound" to chat.alertSound,
            "wallpaperIndex" to chat.wallpaperIndex.coerceIn(0L, 10L),
            "chatThemeKey" to chat.chatThemeKey,
            "shippingCustomerName" to chat.shippingCustomerName.trim(),
            "shippingAddress" to chat.shippingAddress.trim(),
            "shippingRegion" to chat.shippingRegion.trim(),
            "shippingCompany" to chat.shippingCompany.trim(),
            "shippingDaysRemaining" to chat.shippingDaysRemaining.coerceIn(0L, 90L),
            "waybillNumber" to chat.waybillNumber.ifBlank { "WB-${(10000..99999).random()}" },
            "waybillStatus" to chat.waybillStatus,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (chat.activeAdTitle.isNotBlank()) {
            payload["activeAdTitle"] = chat.activeAdTitle.trim()
            payload["activeAdSubtitle"] = chat.activeAdSubtitle.trim()
            payload["activeAdProductId"] = chat.activeAdProductId
        }
        try {
            docRef.set(payload).await()
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateChatTheme(chatId: String, chatThemeKey: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "chatThemeKey" to chatThemeKey,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun setChatInternetBlocked(chatId: String, blocked: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "isInternetBlocked" to blocked,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateChatMedia(chatId: String, alertSound: String, wallpaperIndex: Long) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "alertSound" to alertSound,
                    "wallpaperIndex" to wallpaperIndex.coerceIn(0L, 10L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateChatWaybill(
        chatId: String,
        customerName: String,
        address: String,
        region: String,
        shippingCompany: String,
        daysRemaining: Long,
        waybillNumber: String,
        status: String,
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "shippingCustomerName" to customerName.trim(),
                    "shippingAddress" to address.trim(),
                    "shippingRegion" to region.trim(),
                    "shippingCompany" to shippingCompany.trim(),
                    "shippingDaysRemaining" to daysRemaining.coerceIn(0L, 90L),
                    "waybillNumber" to waybillNumber.trim().ifEmpty { "WB-1000" },
                    "waybillStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateChatAd(
        chatId: String,
        adTitle: String,
        adSubtitle: String,
        productId: String,
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("chats").document(chatId)
        try {
            docRef.update(
                mapOf(
                    "activeAdTitle" to adTitle.trim(),
                    "activeAdSubtitle" to adSubtitle.trim(),
                    "activeAdProductId" to productId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    fun observeMessages(userId: String, chatId: String): Flow<List<ChatMessage>> = flow {
        val query = db.collection("users").document(userId)
            .collection("chats").document(chatId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, "users/$userId/chats/$chatId/messages", userId)
                    throw error
                }
        )
    }

    suspend fun sendMessage(
        chatId: String,
        senderRole: String,
        messageType: String,
        text: String,
        soundType: String = "",
        productId: String = "",
        adTitle: String = "",
    ): String {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val chatRef = db.collection("users").document(uid).collection("chats").document(chatId)
        val msgRef = chatRef.collection("messages").document(msgId)
        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "chatId" to chatId,
            "senderRole" to senderRole,
            "messageType" to messageType,
            "text" to text.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
        )
        if (soundType.isNotBlank()) payload["soundType"] = soundType
        if (productId.isNotBlank()) payload["productId"] = productId
        if (adTitle.isNotBlank()) payload["adTitle"] = adTitle

        try {
            msgRef.set(payload).await()
            chatRef.update(
                mapOf(
                    "lastMessage" to text.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            return msgId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path, auth)
            throw e
        }
    }

    fun observeCustomerSubscriptions(userId: String): Flow<List<CustomerSubscription>> = flow {
        val query = db.collection("users").document(userId).collection("customer_subscriptions")
            .orderBy("remainingDays", Query.Direction.ASCENDING)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomerSubscription::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, "users/$userId/customer_subscriptions", userId)
                    throw error
                }
        )
    }

    fun observeSmsAlertLogs(userId: String): Flow<List<SmsAlertLog>> = flow {
        val query = db.collection("users").document(userId).collection("sms_alert_logs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SmsAlertLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, "users/$userId/sms_alert_logs", userId)
                    throw error
                }
        )
    }

    suspend fun upsertCustomerSubscription(
        sub: CustomerSubscription,
        explicitId: String? = null,
    ): String {
        val uid = requireUserId()
        val id = explicitId ?: sub.id.ifBlank {
            "sub_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        }
        val docRef = db.collection("users").document(uid)
            .collection("customer_subscriptions").document(id)
        val cleanDays = sub.remainingDays.coerceIn(0L, 366L)
        val cleanThreshold = sub.alertThresholdDays.coerceIn(1L, 30L)
        val paused = sub.status == CustomerSubStatus.PAUSED.key
        val computedStatus = computeSubscriptionStatus(cleanDays, cleanThreshold, paused)

        val payload = hashMapOf<String, Any>(
            "userId" to uid,
            "customerName" to sub.customerName.trim().ifEmpty { "عميل المتجر" },
            "customerPhone" to sub.customerPhone.trim().ifEmpty { "+966500000000" },
            "chatId" to sub.chatId.trim(),
            "planKey" to sub.planKey,
            "planLabelAr" to sub.planLabelAr.trim().ifEmpty { SubscriptionPlan.fromKey(sub.planKey).labelAr },
            "priceSar" to sub.priceSar.coerceIn(0.0, 100000.0),
            "remainingDays" to cleanDays,
            "alertThresholdDays" to cleanThreshold,
            "status" to computedStatus,
            "autoSmsAlertEnabled" to sub.autoSmsAlertEnabled,
            "lastSmsAlertText" to sub.lastSmsAlertText.take(500),
            "smsAlertCount" to sub.smsAlertCount.coerceIn(0L, 10000L),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
                docRef.set(payload).await()
            } else {
                payload.remove("userId")
                docRef.update(payload).await()
            }
            return id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            throw e
        }
    }

    suspend fun updateSubscriptionRemainingDays(
        subId: String,
        remainingDays: Long,
        alertThresholdDays: Long,
        plan: SubscriptionPlan? = null,
        paused: Boolean = false,
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("customer_subscriptions").document(subId)
        val cleanDays = remainingDays.coerceIn(0L, 366L)
        val cleanThreshold = alertThresholdDays.coerceIn(1L, 30L)
        val nextStatus = computeSubscriptionStatus(cleanDays, cleanThreshold, paused)
        val updates = hashMapOf<String, Any>(
            "remainingDays" to cleanDays,
            "alertThresholdDays" to cleanThreshold,
            "status" to nextStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (cleanDays > cleanThreshold) {
            updates["lastSmsAlertText"] = ""
        }
        if (plan != null) {
            updates["planKey"] = plan.key
            updates["planLabelAr"] = plan.labelAr
            updates["priceSar"] = plan.priceSar.toDouble()
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun setSubscriptionAutoSms(subId: String, enabled: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("customer_subscriptions").document(subId)
        try {
            docRef.update(
                mapOf(
                    "autoSmsAlertEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            throw e
        }
    }

    suspend fun deleteCustomerSubscription(subId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("customer_subscriptions").document(subId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            throw e
        }
    }

    suspend fun dispatchSmsAlert(
        sub: CustomerSubscription,
        messageBody: String,
        triggerType: String,
        deliveryStatus: String,
    ): String {
        val uid = requireUserId()
        val cleanBody = messageBody.trim().ifEmpty {
            "تنبيه اشتراك: متبقي ${sub.remainingDays} أيام على انتهاء باقة ${sub.planLabelAr}"
        }.take(500)
        val logId = "sms_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val logRef = db.collection("users").document(uid)
            .collection("sms_alert_logs").document(logId)
        val logPayload = hashMapOf<String, Any>(
            "userId" to uid,
            "subscriptionId" to sub.id,
            "customerName" to sub.customerName.trim().ifEmpty { "عميل المتجر" },
            "customerPhone" to sub.customerPhone.trim().ifEmpty { "+966500000000" },
            "messageBody" to cleanBody,
            "triggerType" to triggerType,
            "remainingDaysAtSend" to sub.remainingDays.coerceIn(0L, 366L),
            "deliveryStatus" to deliveryStatus,
            "createdAt" to FieldValue.serverTimestamp(),
        )

        val subRef = db.collection("users").document(uid)
            .collection("customer_subscriptions").document(sub.id)

        try {
            logRef.set(logPayload).await()
            subRef.update(
                mapOf(
                    "lastSmsAlertText" to cleanBody,
                    "smsAlertCount" to (sub.smsAlertCount + 1L).coerceIn(0L, 10000L),
                    "lastAlertSentAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            if (sub.chatId.isNotBlank()) {
                val chatRef = db.collection("users").document(uid).collection("chats").document(sub.chatId)
                val chatSnap = chatRef.get().await()
                if (chatSnap.exists()) {
                    val blocked = chatSnap.getBoolean("isInternetBlocked") == true
                    if (!blocked) {
                        sendMessage(
                            chatId = sub.chatId,
                            senderRole = "system",
                            messageType = "sms_code",
                            text = "تنبيه SMS تلقائي (${sub.customerPhone}): $cleanBody",
                        )
                    }
                }
            }
            return logId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, logRef.path, auth)
            throw e
        }
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String,
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path, auth)
        }
    }

    companion object {
        const val DEFAULT_SETTINGS_ID = "default"
    }
}
