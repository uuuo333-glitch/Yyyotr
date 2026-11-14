package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.EngineSoundPlayer
import com.example.data.ai.ExhaustAiAssistant
import com.example.data.model.CatalogProduct
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThemePreset
import com.example.data.model.CustomerSubStatus
import com.example.data.model.CustomerSubscription
import com.example.data.model.EngineAlertSound
import com.example.data.model.SmsAlertLog
import com.example.data.model.StoreSettings
import com.example.data.model.StoreThemeOption
import com.example.data.model.SubscriptionPlan
import com.example.data.model.computeSubscriptionStatus
import com.example.data.prefs.ThemePreferences
import com.example.data.repository.StoreRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DataUiState<out T> {
    data object Loading : DataUiState<Nothing>
    data class Success<T>(val data: T) : DataUiState<T>
    data class Error(val message: String) : DataUiState<Nothing>
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class StoreViewModel(
    private val repository: StoreRepository,
    private val themePreferences: ThemePreferences,
    private val aiAssistant: ExhaustAiAssistant,
    val soundPlayer: EngineSoundPlayer,
    private val currentUserId: String,
) : ViewModel() {

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    private val _isOwnerMode = MutableStateFlow(true)
    val isOwnerMode: StateFlow<Boolean> = _isOwnerMode.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _seoGenerating = MutableStateFlow(false)
    val seoGenerating: StateFlow<Boolean> = _seoGenerating.asStateFlow()

    private val _globalAutoSmsEnabled = MutableStateFlow(true)
    val globalAutoSmsEnabled: StateFlow<Boolean> = _globalAutoSmsEnabled.asStateFlow()

    private val _pendingSmsDispatch = MutableStateFlow<SmsAlertLog?>(null)
    val pendingSmsDispatch: StateFlow<SmsAlertLog?> = _pendingSmsDispatch.asStateFlow()

    private val _aiSmsDraftLoading = MutableStateFlow(false)
    val aiSmsDraftLoading: StateFlow<Boolean> = _aiSmsDraftLoading.asStateFlow()

    private val autoAlertedInSession = mutableSetOf<String>()

    val settingsState: StateFlow<DataUiState<StoreSettings>> = repository
        .observeStoreSettings(currentUserId)
        .map<StoreSettings?, DataUiState<StoreSettings>> { settings ->
            DataUiState.Success(settings ?: StoreSettings(userId = currentUserId))
        }
        .catch { error ->
            val msg = unwrapError(error, "تعذر تحميل إعدادات المتجر")
            emit(DataUiState.Error(msg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val productsState: StateFlow<DataUiState<List<CatalogProduct>>> = repository
        .observeProducts(currentUserId)
        .map<List<CatalogProduct>, DataUiState<List<CatalogProduct>>> { DataUiState.Success(it) }
        .catch { error ->
            val msg = unwrapError(error, "تعذر تحميل كتالوج المنتجات")
            emit(DataUiState.Error(msg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val chatsState: StateFlow<DataUiState<List<ChatConversation>>> = repository
        .observeChats(currentUserId)
        .map<List<ChatConversation>, DataUiState<List<ChatConversation>>> { DataUiState.Success(it) }
        .catch { error ->
            val msg = unwrapError(error, "تعذر تحميل المحادثات")
            emit(DataUiState.Error(msg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val customerSubscriptionsState: StateFlow<DataUiState<List<CustomerSubscription>>> = repository
        .observeCustomerSubscriptions(currentUserId)
        .map<List<CustomerSubscription>, DataUiState<List<CustomerSubscription>>> { DataUiState.Success(it) }
        .catch { error ->
            val msg = unwrapError(error, "تعذر تحميل اشتراكات العملاء")
            emit(DataUiState.Error(msg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val smsAlertLogsState: StateFlow<DataUiState<List<SmsAlertLog>>> = repository
        .observeSmsAlertLogs(currentUserId)
        .map<List<SmsAlertLog>, DataUiState<List<SmsAlertLog>>> { DataUiState.Success(it) }
        .catch { error ->
            val msg = unwrapError(error, "تعذر تحميل سجل تنبيهات SMS")
            emit(DataUiState.Error(msg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataUiState.Loading)

    val activeChatState: StateFlow<ChatConversation?> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(null)
            else repository.observeChat(currentUserId, chatId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeMessagesState: StateFlow<List<ChatMessage>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else repository.observeMessages(currentUserId, chatId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            try {
                repository.ensureDefaultStoreSeeded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to seed store defaults", e)
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تهيئة بيانات المتجر الافتراضية"
            }
        }
        viewModelScope.launch {
            customerSubscriptionsState.collect { state ->
                if (state is DataUiState.Success && _globalAutoSmsEnabled.value) {
                    runAutomaticExpiryAlerts(state.data, userTriggered = false)
                }
            }
        }
    }

    fun selectChat(chatId: String?) {
        _selectedChatId.value = chatId
        _aiState.value = AiUiState.Idle
    }

    fun toggleOwnerMode(owner: Boolean) {
        _isOwnerMode.value = owner
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun updateThemeColor(option: StoreThemeOption) {
        viewModelScope.launch {
            themePreferences.saveTheme(option)
            val current = (settingsState.value as? DataUiState.Success)?.data
                ?: StoreSettings(userId = currentUserId)
            try {
                repository.saveStoreSettings(current.copy(themeColorKey = option.key))
                _snackbarMessage.value = "تم تغيير لون التطبيق إلى ${option.labelAr}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر حفظ اللون"
            }
        }
    }

    fun saveStoreSettings(updated: StoreSettings) {
        viewModelScope.launch {
            try {
                themePreferences.saveTheme(StoreThemeOption.fromKey(updated.themeColorKey))
                repository.saveStoreSettings(updated)
                _snackbarMessage.value = "تم حفظ إعدادات المتجر بنجاح"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر حفظ الإعدادات"
            }
        }
    }

    fun createNewChat(customerName: String, customerPhone: String, onCreated: (String) -> Unit) {
        val settings = (settingsState.value as? DataUiState.Success)?.data ?: StoreSettings()
        viewModelScope.launch {
            try {
                val newId = repository.createChat(
                    ChatConversation(
                        userId = currentUserId,
                        customerName = customerName,
                        customerPhone = customerPhone,
                        lastMessage = "مرحباً بك في ${settings.storeName}",
                        isInternetBlocked = false,
                        alertSound = settings.selectedAlertSound,
                        wallpaperIndex = settings.activeWallpaperIndex,
                        shippingCompany = settings.defaultShippingCompany,
                        shippingDaysRemaining = settings.defaultEstimatedDays,
                        waybillNumber = "WB-${(10000..99999).random()}",
                        waybillStatus = "pending",
                    )
                )
                repository.sendMessage(
                    chatId = newId,
                    senderRole = "owner",
                    messageType = "text",
                    text = "أهلاً بك في ${settings.storeName} (${settings.tagline}). كيف نقدر نخدمك اليوم؟",
                )
                onCreated(newId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر إنشاء المحادثة"
            }
        }
    }

    fun toggleChatInternetBlock(chat: ChatConversation) {
        val nextBlocked = !chat.isInternetBlocked
        viewModelScope.launch {
            try {
                repository.setChatInternetBlocked(chat.id, nextBlocked)
                val notice = if (nextBlocked) {
                    "تم حظر المحادثة عن طريق قطع الإنترنت عنها — لن يتم إرسال أو استقبال رسائل جديدة."
                } else {
                    "تمت إعادة توصيل الإنترنت للمحادثة بنجاح."
                }
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = "system",
                    messageType = "text",
                    text = notice,
                )
                _snackbarMessage.value = notice
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تحديث حالة الحظر"
            }
        }
    }

    fun sendChatMessage(chat: ChatConversation, text: String, askAiToo: Boolean = false) {
        if (chat.isInternetBlocked) {
            _snackbarMessage.value = "المحادثة محظورة (الإنترنت مقطوع عنها). أعد الاتصال للإرسال."
            return
        }
        val clean = text.trim()
        if (clean.isEmpty()) return

        val role = if (_isOwnerMode.value) "owner" else "customer"
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = role,
                    messageType = "text",
                    text = clean,
                )
                if (askAiToo) {
                    askAiInChat(chat, clean)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر إرسال الرسالة"
            }
        }
    }

    fun askAiInChat(chat: ChatConversation, prompt: String) {
        if (chat.isInternetBlocked) {
            _snackbarMessage.value = "الذكاء الاصطناعي متوقف لأن الإنترنت مقطوع عن هذه المحادثة."
            return
        }
        val products = (productsState.value as? DataUiState.Success)?.data ?: emptyList()
        val settings = (settingsState.value as? DataUiState.Success)?.data ?: StoreSettings()
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            var finalReply = ""
            try {
                aiAssistant.streamStoreReply(
                    userPrompt = prompt,
                    catalog = products,
                    storeName = settings.storeName,
                    history = activeMessagesState.value,
                ).collect { partial ->
                    finalReply = partial
                    _aiState.value = AiUiState.Streaming(partial)
                }
                _aiState.value = AiUiState.Success(finalReply)
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = "ai",
                    messageType = "text",
                    text = finalReply,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "تعذر الحصول على رد الذكاء الاصطناعي"
                _aiState.value = AiUiState.Error(err)
                _snackbarMessage.value = err
            }
        }
    }

    fun sendSoundAlert(chat: ChatConversation, sound: EngineAlertSound) {
        soundPlayer.play(sound)
        if (chat.isInternetBlocked) {
            _snackbarMessage.value = "تم تشغيل الصوت محلياً فقط — الإنترنت مقطوع عن المحادثة."
            return
        }
        viewModelScope.launch {
            try {
                repository.updateChatMedia(chat.id, sound.key, chat.wallpaperIndex)
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = if (_isOwnerMode.value) "owner" else "customer",
                    messageType = "sound_alert",
                    text = "تنبيه صوتي للمحادثة: ${sound.labelAr} (${sound.subtitleAr})",
                    soundType = sound.key,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر إرسال التنبيه الصوتي"
            }
        }
    }

    fun cycleChatWallpaper(chat: ChatConversation, nextIndex: Long) {
        viewModelScope.launch {
            try {
                repository.updateChatMedia(chat.id, chat.alertSound, nextIndex)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تغيير الخلفية"
            }
        }
    }

    fun sendInChatAd(chat: ChatConversation, adTitle: String, adSubtitle: String, productId: String) {
        if (chat.isInternetBlocked) {
            _snackbarMessage.value = "الإنترنت مقطوع عن المحادثة — أعد الاتصال لإرسال الإعلان."
            return
        }
        viewModelScope.launch {
            try {
                repository.updateChatAd(chat.id, adTitle, adSubtitle, productId)
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = "owner",
                    messageType = "ad",
                    text = adSubtitle,
                    productId = productId,
                    adTitle = adTitle,
                )
                _snackbarMessage.value = "تم نشر الإعلان داخل المحادثة"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر نشر الإعلان"
            }
        }
    }

    fun saveWaybill(
        chat: ChatConversation,
        customerName: String,
        address: String,
        region: String,
        shippingCompany: String,
        daysRemaining: Long,
        waybillNumber: String,
        status: String,
    ) {
        viewModelScope.launch {
            try {
                repository.updateChatWaybill(
                    chatId = chat.id,
                    customerName = customerName,
                    address = address,
                    region = region,
                    shippingCompany = shippingCompany,
                    daysRemaining = daysRemaining,
                    waybillNumber = waybillNumber,
                    status = status,
                )
                if (!chat.isInternetBlocked) {
                    repository.sendMessage(
                        chatId = chat.id,
                        senderRole = "system",
                        messageType = "waybill",
                        text = "بوليصة الشحن ($waybillNumber): العميل $customerName - $region | شركة الشحن: $shippingCompany | الوصول خلال $daysRemaining أيام",
                    )
                }
                _snackbarMessage.value = "تم تحديث بوليصة الشحن في المحادثة"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر حفظ بوليصة الشحن"
            }
        }
    }

    fun logVoiceCall(chat: ChatConversation, durationSeconds: Int) {
        if (chat.isInternetBlocked) return
        viewModelScope.launch {
            try {
                val mins = durationSeconds / 60
                val secs = durationSeconds % 60
                val formatted = String.format("%02d:%02d", mins, secs)
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = if (_isOwnerMode.value) "owner" else "customer",
                    messageType = "call_log",
                    text = "مكالمة صوتية عبر المتجر ($formatted)",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun sendProductCardToChat(chat: ChatConversation, product: CatalogProduct) {
        if (chat.isInternetBlocked) {
            _snackbarMessage.value = "الإنترنت مقطوع عن المحادثة."
            return
        }
        viewModelScope.launch {
            try {
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = if (_isOwnerMode.value) "owner" else "customer",
                    messageType = "product",
                    text = "${product.name} — ${product.priceSar.toInt()} ر.س\n${product.description}",
                    productId = product.id,
                    adTitle = product.category,
                )
                _snackbarMessage.value = "تم إرسال المنتج إلى المحادثة"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر إرسال المنتج"
            }
        }
    }

    fun createCatalogProduct(product: CatalogProduct) {
        viewModelScope.launch {
            try {
                repository.createProduct(product.copy(userId = currentUserId))
                _snackbarMessage.value = "تمت إضافة المنتج للكتالوج وتضمين كلمات بحث جوجل"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر إضافة المنتج"
            }
        }
    }

    fun updateProductKeywords(productId: String, keywords: List<String>) {
        viewModelScope.launch {
            try {
                repository.updateProductKeywords(productId, keywords)
                _snackbarMessage.value = "تم تحديث كلمات بحث جوجل للمنتج"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تحديث الكلمات المفتاحية"
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId)
                _snackbarMessage.value = "تم حذف المنتج من الكتالوج"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر حذف المنتج"
            }
        }
    }

    fun updateChatTheme(chat: ChatConversation, preset: ChatThemePreset) {
        viewModelScope.launch {
            try {
                repository.updateChatTheme(chat.id, preset.key)
                _snackbarMessage.value = "تم تطبيق ${preset.labelAr} على المحادثة"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تغيير تيم المحادثة"
            }
        }
    }

    fun autoFillCustomerDataWithAi(chat: ChatConversation, messages: List<ChatMessage>) {
        if (chat.isInternetBlocked) {
            _snackbarMessage.value = "المحادثة مقطوعة الإنترنت — أعد الاتصال لتشغيل الذكاء الاصطناعي."
            return
        }
        val products = (productsState.value as? DataUiState.Success)?.data ?: emptyList()
        viewModelScope.launch {
            _aiState.value = AiUiState.Loading
            try {
                val extracted = aiAssistant.extractCustomerOrderData(
                    messages = messages,
                    fallbackCustomerName = chat.customerName,
                    fallbackPhone = chat.customerPhone,
                    catalog = products,
                )
                repository.updateChatWaybill(
                    chatId = chat.id,
                    customerName = extracted.customerName.ifBlank { chat.customerName },
                    address = extracted.address.ifBlank { chat.shippingAddress },
                    region = extracted.region.ifBlank { chat.shippingRegion },
                    shippingCompany = chat.shippingCompany.ifBlank { "أرامكس (Aramex)" },
                    daysRemaining = chat.shippingDaysRemaining,
                    waybillNumber = chat.waybillNumber.ifBlank { "WB-${(10000..99999).random()}" },
                    status = chat.waybillStatus,
                )
                val replyText = buildString {
                    appendLine("تم التقاط وتعبئة بيانات العميل تلقائياً بالذكاء الاصطناعي:")
                    appendLine("• الاسم: ${extracted.customerName}")
                    if (extracted.region.isNotBlank()) appendLine("• المنطقة: ${extracted.region}")
                    if (extracted.address.isNotBlank()) appendLine("• العنوان: ${extracted.address}")
                    if (extracted.phone.isNotBlank()) appendLine("• الجوال: ${extracted.phone}")
                    if (extracted.requestedProduct.isNotBlank()) appendLine("• المنتج: ${extracted.requestedProduct}")
                    append(extracted.summaryNote)
                }
                _aiState.value = AiUiState.Success(replyText)
                repository.sendMessage(
                    chatId = chat.id,
                    senderRole = "ai",
                    messageType = "waybill",
                    text = replyText,
                )
                _snackbarMessage.value = "تمت تعبئة بيانات العميل في البوليصة بالذكاء الاصطناعي"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "تعذر تعبئة البيانات بالذكاء الاصطناعي"
                _aiState.value = AiUiState.Error(err)
                _snackbarMessage.value = err
            }
        }
    }

    fun activateSubscriptionPlan(
        plan: SubscriptionPlan,
        phoneNumber: String,
        customAppName: String,
        websiteUrl: String,
    ) {
        val current = (settingsState.value as? DataUiState.Success)?.data
            ?: StoreSettings(userId = currentUserId)
        val generatedCode = "TB-${(100000..999999).random()}"
        viewModelScope.launch {
            try {
                val updated = current.copy(
                    customAppNameUnderBadge = customAppName.trim().ifBlank { current.customAppNameUnderBadge },
                    websiteUrl = websiteUrl.trim().ifBlank { current.websiteUrl },
                    subscriptionPlan = plan.key,
                    subscriptionActive = plan == SubscriptionPlan.FREE_TRIAL,
                    subscriptionDaysLeft = plan.durationDays,
                    subscriptionSmsCode = generatedCode,
                    customerPhoneForSms = phoneNumber.trim(),
                )
                repository.saveStoreSettings(updated)
                val firstChat = (chatsState.value as? DataUiState.Success)?.data?.firstOrNull()
                if (firstChat != null && !firstChat.isInternetBlocked) {
                    repository.sendMessage(
                        chatId = firstChat.id,
                        senderRole = "system",
                        messageType = "sms_code",
                        text = "رسالة نصية SMS (${phoneNumber.ifBlank { "العميل" }}): كود تفعيل اشتراك ${plan.labelAr} هو $generatedCode",
                    )
                }
                _snackbarMessage.value = if (plan == SubscriptionPlan.FREE_TRIAL) {
                    "تم تفعيل التجربة المجانية 5 أيام! كود SMS: $generatedCode"
                } else {
                    "تم إرسال كود التفعيل ($generatedCode) في رسالة نصية — أدخله لتأكيد الدفع (${plan.priceSar} ر.س)"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تحديث الاشتراك"
            }
        }
    }

    fun verifySubscriptionSmsCode(enteredCode: String) {
        val current = (settingsState.value as? DataUiState.Success)?.data
            ?: StoreSettings(userId = currentUserId)
        if (current.subscriptionSmsCode.isBlank() || enteredCode.trim().equals(current.subscriptionSmsCode, ignoreCase = true)) {
            viewModelScope.launch {
                try {
                    val plan = SubscriptionPlan.fromKey(current.subscriptionPlan)
                    repository.saveStoreSettings(
                        current.copy(
                            subscriptionActive = true,
                            subscriptionDaysLeft = plan.durationDays,
                        )
                    )
                    _snackbarMessage.value = "تم تفعيل الاشتراك (${plan.labelAr}) وفتح تعديل الاسم والرابط بنجاح!"
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _snackbarMessage.value = e.localizedMessage ?: "تعذر تأكيد الكود"
                }
            }
        } else {
            _snackbarMessage.value = "الكود غير صحيح. تأكد من كود الرسالة النصية (${current.subscriptionSmsCode})"
        }
    }

    fun generateAiSeoKeywords(
        productName: String,
        category: String,
        description: String,
        onResult: (List<String>, String) -> Unit,
    ) {
        viewModelScope.launch {
            _seoGenerating.value = true
            try {
                val (keywords, snippet) = aiAssistant.generateGoogleSeoKeywords(
                    productName = productName,
                    category = category,
                    description = description,
                )
                onResult(keywords, snippet)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر توليد الكلمات المفتاحية بالذكاء الاصطناعي"
            } finally {
                _seoGenerating.value = false
            }
        }
    }

    fun toggleGlobalAutoSms(enabled: Boolean) {
        _globalAutoSmsEnabled.value = enabled
        _snackbarMessage.value = if (enabled) {
            "تم تفعيل نظام التنبيه التلقائي لقرب انتهاء الاشتراكات عبر SMS"
        } else {
            "تم إيقاف التنبيه التلقائي المؤقت"
        }
        if (enabled) {
            triggerManualAutoExpiryScan()
        }
    }

    fun dismissPendingSmsBanner() {
        _pendingSmsDispatch.value = null
    }

    fun triggerManualAutoExpiryScan() {
        val subs = (customerSubscriptionsState.value as? DataUiState.Success)?.data ?: return
        viewModelScope.launch {
            runAutomaticExpiryAlerts(subs, userTriggered = true)
        }
    }

    private suspend fun runAutomaticExpiryAlerts(
        subscriptions: List<CustomerSubscription>,
        userTriggered: Boolean,
    ) {
        val settings = (settingsState.value as? DataUiState.Success)?.data ?: StoreSettings()
        val storeName = settings.storeName.ifBlank { "عوادم التيربو" }
        val candidates = subscriptions.filter { sub ->
            sub.autoSmsAlertEnabled &&
                sub.status != CustomerSubStatus.PAUSED.key &&
                sub.remainingDays in 1L..sub.alertThresholdDays &&
                (userTriggered || (sub.lastSmsAlertText.isBlank() && sub.id !in autoAlertedInSession))
        }
        if (candidates.isEmpty()) {
            if (userTriggered) {
                _snackbarMessage.value = "لا توجد اشتراكات قريبة من الانتهاء بحاجة لتنبيه SMS الآن"
            }
            return
        }
        var sentCount = 0
        for (sub in candidates) {
            if (!userTriggered) autoAlertedInSession.add(sub.id)
            val daysText = when (sub.remainingDays) {
                1L -> "يوم واحد"
                2L -> "يومين"
                else -> "${sub.remainingDays} أيام"
            }
            val smsBody =
                "تنبيه من $storeName: مرحباً ${sub.customerName}، متبقي $daysText على انتهاء اشتراك (${sub.planLabelAr}). للتجديد بـ 25 ر.س/شهر تواصل معنا أو افتح ${settings.websiteUrl}"
            try {
                val logId = repository.dispatchSmsAlert(
                    sub = sub,
                    messageBody = smsBody,
                    triggerType = "auto_expiry",
                    deliveryStatus = "queued_auto",
                )
                sentCount++
                _pendingSmsDispatch.value = SmsAlertLog(
                    id = logId,
                    userId = currentUserId,
                    subscriptionId = sub.id,
                    customerName = sub.customerName,
                    customerPhone = sub.customerPhone,
                    messageBody = smsBody,
                    triggerType = "auto_expiry",
                    remainingDaysAtSend = sub.remainingDays,
                    deliveryStatus = "queued_auto",
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed auto SMS alert for ${sub.id}", e)
            }
        }
        if (sentCount > 0) {
            _snackbarMessage.value = "تم إرسال وتجهيز $sentCount تنبيه SMS تلقائي لقرب انتهاء الاشتراك"
        }
    }

    fun saveCustomerSubscription(
        existingId: String?,
        customerName: String,
        customerPhone: String,
        plan: SubscriptionPlan,
        remainingDays: Long,
        alertThresholdDays: Long,
        autoSmsEnabled: Boolean,
        chatId: String = "",
    ) {
        viewModelScope.launch {
            try {
                val cleanDays = remainingDays.coerceIn(0L, 366L)
                val cleanThreshold = alertThresholdDays.coerceIn(1L, 30L)
                if (cleanDays > cleanThreshold && existingId != null) {
                    autoAlertedInSession.remove(existingId)
                }
                val newSub = CustomerSubscription(
                    id = existingId.orEmpty(),
                    userId = currentUserId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    chatId = chatId,
                    planKey = plan.key,
                    planLabelAr = plan.labelAr,
                    priceSar = plan.priceSar.toDouble(),
                    remainingDays = cleanDays,
                    alertThresholdDays = cleanThreshold,
                    status = computeSubscriptionStatus(cleanDays, cleanThreshold),
                    autoSmsAlertEnabled = autoSmsEnabled,
                )
                repository.upsertCustomerSubscription(newSub, explicitId = existingId)
                _snackbarMessage.value = if (existingId == null) {
                    "تمت إضافة اشتراك العميل $customerName"
                } else {
                    "تم تحديث اشتراك $customerName"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر حفظ اشتراك العميل"
            }
        }
    }

    fun adjustSubscriptionDays(
        sub: CustomerSubscription,
        newDays: Long,
        newThreshold: Long = sub.alertThresholdDays,
        paused: Boolean = false,
    ) {
        viewModelScope.launch {
            try {
                val cleanDays = newDays.coerceIn(0L, 366L)
                val cleanThreshold = newThreshold.coerceIn(1L, 30L)
                if (cleanDays > cleanThreshold) {
                    autoAlertedInSession.remove(sub.id)
                }
                repository.updateSubscriptionRemainingDays(
                    subId = sub.id,
                    remainingDays = cleanDays,
                    alertThresholdDays = cleanThreshold,
                    paused = paused,
                )
                val nextStatus = CustomerSubStatus.fromKey(
                    computeSubscriptionStatus(cleanDays, cleanThreshold, paused)
                )
                _snackbarMessage.value = "المدة المتبقية لـ ${sub.customerName}: $cleanDays يوم (${nextStatus.labelAr})"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تعديل المدة المتبقية"
            }
        }
    }

    fun renewCustomerSubscription(
        sub: CustomerSubscription,
        plan: SubscriptionPlan,
    ) {
        viewModelScope.launch {
            try {
                autoAlertedInSession.remove(sub.id)
                val nextDays = (sub.remainingDays + plan.durationDays).coerceIn(1L, 366L)
                repository.updateSubscriptionRemainingDays(
                    subId = sub.id,
                    remainingDays = nextDays,
                    alertThresholdDays = sub.alertThresholdDays,
                    plan = plan,
                    paused = false,
                )
                _snackbarMessage.value = "تم تجديد اشتراك ${sub.customerName} (${plan.labelAr}) — المتبقي $nextDays يوم"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تجديد الاشتراك"
            }
        }
    }

    fun toggleCustomerAutoSms(sub: CustomerSubscription, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setSubscriptionAutoSms(sub.id, enabled)
                _snackbarMessage.value = if (enabled) {
                    "تم تفعيل التنبيه التلقائي عبر SMS لـ ${sub.customerName}"
                } else {
                    "تم إيقاف التنبيه التلقائي لـ ${sub.customerName}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تحديث إعداد التنبيه التلقائي"
            }
        }
    }

    fun sendManualSmsAlert(
        sub: CustomerSubscription,
        messageBody: String,
        isAiDrafted: Boolean = false,
        deliveryStatus: String = "dispatched_sms_intent",
    ) {
        viewModelScope.launch {
            try {
                val trigger = if (isAiDrafted) "ai_drafted" else "manual_owner"
                val logId = repository.dispatchSmsAlert(
                    sub = sub,
                    messageBody = messageBody,
                    triggerType = trigger,
                    deliveryStatus = deliveryStatus,
                )
                _pendingSmsDispatch.value = SmsAlertLog(
                    id = logId,
                    userId = currentUserId,
                    subscriptionId = sub.id,
                    customerName = sub.customerName,
                    customerPhone = sub.customerPhone,
                    messageBody = messageBody,
                    triggerType = trigger,
                    remainingDaysAtSend = sub.remainingDays,
                    deliveryStatus = deliveryStatus,
                )
                _snackbarMessage.value = "تم تسجيل وإرسال التنبيه النصي إلى ${sub.customerName}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر تسجيل الرسالة النصية"
            }
        }
    }

    fun draftAiSmsForSubscriber(
        sub: CustomerSubscription,
        onResult: (String) -> Unit,
    ) {
        val settings = (settingsState.value as? DataUiState.Success)?.data ?: StoreSettings()
        viewModelScope.launch {
            _aiSmsDraftLoading.value = true
            try {
                val drafted = aiAssistant.draftSubscriptionExpirySms(
                    customerName = sub.customerName,
                    planLabel = sub.planLabelAr,
                    remainingDays = sub.remainingDays,
                    storeName = settings.storeName,
                )
                onResult(drafted)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر صياغة الرسالة بالذكاء الاصطناعي"
            } finally {
                _aiSmsDraftLoading.value = false
            }
        }
    }

    fun deleteCustomerSubscription(subId: String) {
        viewModelScope.launch {
            try {
                repository.deleteCustomerSubscription(subId)
                _snackbarMessage.value = "تم حذف اشتراك العميل"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _snackbarMessage.value = e.localizedMessage ?: "تعذر حذف الاشتراك"
            }
        }
    }

    private fun unwrapError(error: Throwable, fallback: String): String {
        val ffe = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (ffe == null && error is CancellationException) throw error
        val root = ffe ?: error
        Log.w(TAG, "Stream error: ${root.message}", root)
        return root.message ?: fallback
    }

    override fun onCleared() {
        super.onCleared()
        soundPlayer.stop()
    }

    private companion object {
        const val TAG = "StoreViewModel"
    }
}
