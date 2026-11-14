package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.CatalogProduct
import com.example.data.model.ChatConversation
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class StoreRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureDefaultStoreSeeded_and_observeProductsAndChats_succeedsForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = StoreRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureDefaultStoreSeeded()
        }

        val settings = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStoreSettings(uid).first { it != null }
        }
        assertNotNull(settings)
        assertEquals("عوادم التيربو", settings?.storeName)

        val products = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeProducts(uid).first { it.isNotEmpty() }
        }
        assertTrue(products.any { it.seoKeywords.contains("دبة تلوث") })

        val chats = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChats(uid).first { it.isNotEmpty() }
        }
        assertTrue(chats.isNotEmpty())

        val subs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerSubscriptions(uid).first { it.isNotEmpty() }
        }
        assertTrue(subs.any { it.status == "expiring_soon" && it.remainingDays == 2L })
    }

    @Test
    fun customerSubscription_updateDays_and_dispatchSmsAlert_succeedForOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = StoreRepository(firestore, auth)

        val subId = "sub_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.upsertCustomerSubscription(
                com.example.data.model.CustomerSubscription(
                    id = subId,
                    userId = uid,
                    customerName = "ماجد الشهري",
                    customerPhone = "+966559998877",
                    planKey = "1_month",
                    planLabelAr = "شهر واحد (25 ر.س)",
                    priceSar = 25.0,
                    remainingDays = 10L,
                    alertThresholdDays = 3L,
                    autoSmsAlertEnabled = true,
                ),
                explicitId = subId,
            )
        }

        val initialSub = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerSubscriptions(uid).first { list -> list.any { it.id == subId } }
        }.first { it.id == subId }
        assertEquals("active", initialSub.status)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.updateSubscriptionRemainingDays(
                subId = subId,
                remainingDays = 2L,
                alertThresholdDays = 3L,
            )
        }

        val expiringSub = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerSubscriptions(uid).first { list ->
                list.any { it.id == subId && it.status == "expiring_soon" }
            }
        }.first { it.id == subId }
        assertEquals(2L, expiringSub.remainingDays)
        assertEquals("expiring_soon", expiringSub.status)

        val logId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.dispatchSmsAlert(
                sub = expiringSub,
                messageBody = "تنبيه: متبقي يومان على انتهاء اشتراكك في عوادم التيربو",
                triggerType = "auto_expiry",
                deliveryStatus = "queued_auto",
            )
        }

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeSmsAlertLogs(uid).first { list -> list.any { it.id == logId } }
        }
        val sentLog = logs.first { it.id == logId }
        assertEquals("auto_expiry", sentLog.triggerType)
        assertEquals(2L, sentLog.remainingDaysAtSend)

        val afterAlertSub = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCustomerSubscriptions(uid).first { list ->
                list.any { it.id == subId && it.smsAlertCount == 1L }
            }
        }.first { it.id == subId }
        assertEquals(1L, afterAlertSub.smsAlertCount)
    }

    @Test
    fun createProduct_toggleBlock_updateWaybill_and_sendMessage_succeed() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = StoreRepository(firestore, auth)

        val prodId = "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createProduct(
                CatalogProduct(
                    userId = uid,
                    name = "دبة تلوث سيراميك 400 خلية",
                    description = "مطابقة للمواصفات وتزيل كود الحساس",
                    category = "دبات التلوث",
                    priceSar = 920.0,
                    imageKey = "catalytic_converter",
                    seoKeywords = listOf("دبة تلوث", "كتاليزر", "P0420"),
                    inStock = true,
                ),
                explicitId = prodId,
            )
        }

        val chatId = "chat_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createChat(
                ChatConversation(
                    userId = uid,
                    customerName = "سلطان الحربي",
                    customerPhone = "+966501112233",
                    lastMessage = "مرحبا",
                    isInternetBlocked = false,
                    alertSound = "motorcycle",
                    wallpaperIndex = 1L,
                    shippingCompany = "أرامكس",
                    shippingDaysRemaining = 3L,
                    waybillNumber = "WB-9911",
                    waybillStatus = "pending",
                ),
                explicitId = chatId,
            )
            repo.setChatInternetBlocked(chatId, true)
            repo.updateChatWaybill(
                chatId = chatId,
                customerName = "سلطان الحربي",
                address = "حي الروضة، شارع التحلية",
                region = "جدة",
                shippingCompany = "سمسا",
                daysRemaining = 2L,
                waybillNumber = "WB-9911",
                status = "shipped",
            )
            repo.updateChatMedia(chatId, "rocket", 2L)
            repo.updateChatTheme(chatId, "carbon_gold")
            repo.sendMessage(
                chatId = chatId,
                senderRole = "owner",
                messageType = "sound_alert",
                text = "تم إرسال صوت انفجار وفرقعة",
                soundType = "pops_bangs",
            )
            repo.sendMessage(
                chatId = chatId,
                senderRole = "system",
                messageType = "sms_code",
                text = "رسالة نصية SMS: كود تفعيل الاشتراك هو TB-123456",
            )
            repo.ensureDefaultStoreSeeded()
            val currentSettings = repo.observeStoreSettings(uid).first { it != null }!!
            repo.saveStoreSettings(
                currentSettings.copy(
                    customAppNameUnderBadge = "متجر العوادم الرسمي",
                    websiteUrl = "https://exhaustpro.sa",
                    subscriptionPlan = "1_month",
                    subscriptionActive = true,
                    subscriptionDaysLeft = 30L,
                    subscriptionSmsCode = "TB-123456",
                    customerPhoneForSms = "+966501112233",
                )
            )
        }

        val chat = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeChat(uid, chatId).first { it != null && it.isInternetBlocked && it.chatThemeKey == "carbon_gold" }
        }
        assertEquals(true, chat?.isInternetBlocked)
        assertEquals("جدة", chat?.shippingRegion)
        assertEquals(2L, chat?.shippingDaysRemaining)
        assertEquals("rocket", chat?.alertSound)
        assertEquals("carbon_gold", chat?.chatThemeKey)

        val updatedSettings = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStoreSettings(uid).first { it?.subscriptionPlan == "1_month" }
        }
        assertEquals("متجر العوادم الرسمي", updatedSettings?.customAppNameUnderBadge)
        assertEquals("https://exhaustpro.sa", updatedSettings?.websiteUrl)
        assertEquals("TB-123456", updatedSettings?.subscriptionSmsCode)
    }

    @Test
    fun crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StoreRepository(firestore, auth)
        val prodId = "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        val subId = "sub_${UUID.randomUUID().toString().replace("-", "").take(10)}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createProduct(
                CatalogProduct(
                    userId = aliceUid,
                    name = "دبة تيربو",
                    description = "وصف",
                    category = "أنظمة العادم",
                    priceSar = 500.0,
                    imageKey = "exhaust_sport",
                    seoKeywords = listOf("تيربو"),
                    inStock = true,
                ),
                explicitId = prodId,
            )
            aliceRepo.upsertCustomerSubscription(
                com.example.data.model.CustomerSubscription(
                    id = subId,
                    userId = aliceUid,
                    customerName = "عميل سري",
                    customerPhone = "+966500001111",
                    remainingDays = 5L,
                    alertThresholdDays = 3L,
                ),
                explicitId = subId,
            )
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("products").document(prodId)
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED for cross-user read")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("customer_subscriptions").document(subId)
                    .get().await()
            }
            fail("Expected PERMISSION_DENIED for cross-user subscription read")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }
    }

    @Test
    fun observeProducts_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = StoreRepository(firestore, auth)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeProducts("some_owner_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated list")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_store@test.com"
        const val BOB_EMAIL = "bob_store@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
