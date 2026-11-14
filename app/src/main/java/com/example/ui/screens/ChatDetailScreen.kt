package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CatalogProduct
import com.example.data.model.ChatConversation
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThemePreset
import com.example.data.model.EngineAlertSound
import com.example.data.model.StoreSettings
import com.example.ui.components.AnimatedChatWallpaperBackground
import com.example.ui.components.AnimatedSocialTickerBar
import com.example.ui.components.BlueBadgeVerificationDialog
import com.example.ui.components.ChatThemeSelectorStrip
import com.example.ui.components.EngineAlertSoundStrip
import com.example.ui.components.ShippingWaybillCard
import com.example.ui.components.VerifiedStoreTitleBlock
import com.example.ui.components.VoiceCallOverlayDialog
import com.example.ui.components.WaybillEditorDialog
import com.example.ui.components.rememberChatThemePalette
import com.example.ui.theme.LocalStoreColors
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.AiUiState

@Composable
fun ChatDetailScreen(
    chat: ChatConversation,
    messages: List<ChatMessage>,
    settings: StoreSettings,
    products: List<CatalogProduct>,
    isOwnerMode: Boolean,
    aiState: AiUiState,
    playingSound: EngineAlertSound?,
    onBack: () -> Unit,
    onOpenVideoCatalog: () -> Unit,
    onToggleInternetBlock: () -> Unit,
    onSendMessage: (String, Boolean) -> Unit,
    onAskAi: (String) -> Unit,
    onSendSoundAlert: (EngineAlertSound) -> Unit,
    onPlaySoundPreview: (EngineAlertSound) -> Unit,
    onCycleWallpaper: (Long) -> Unit,
    onSendAd: (String, String, String) -> Unit,
    onSaveWaybill: (String, String, String, String, Long, String, String) -> Unit,
    onLogVoiceCall: (Int) -> Unit,
    onUpdateChatTheme: (ChatThemePreset) -> Unit,
    onAutoFillCustomerAi: () -> Unit,
    onSendProductToChat: (CatalogProduct) -> Unit,
    onOpenSubscriptionSettings: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var messageText by remember { mutableStateOf("") }
    var showWaybillDialog by remember { mutableStateOf(false) }
    var showWaybillCard by remember { mutableStateOf(true) }
    var showVoiceCall by remember { mutableStateOf(false) }
    var showAdDialog by remember { mutableStateOf(false) }
    var showBadgeDialog by remember { mutableStateOf(false) }
    var showProductPickerDialog by remember { mutableStateOf(false) }
    var autoCycleWallpaper by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()
    val storeColors = LocalStoreColors.current
    val activeChatTheme = ChatThemePreset.fromKey(chat.chatThemeKey)
    val chatPalette = rememberChatThemePalette(activeChatTheme)

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated changing chat wallpapers
        AnimatedChatWallpaperBackground(
            selectedIndex = chat.wallpaperIndex.toInt(),
            autoCycle = autoCycleWallpaper,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Chat Header with Clickable Verified Badge, Voice Call, Wallpaper Switch, Internet-Cutoff Block
            Surface(
                color = chatPalette.headerContainer,
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                        )
                    }

                    VerifiedStoreTitleBlock(
                        storeName = if (isOwnerMode) "${chat.customerName} • ${settings.storeName}" else settings.storeName,
                        tagline = settings.tagline,
                        verified = settings.verifiedBadge,
                        customAppNameUnderBadge = settings.customAppNameUnderBadge,
                        websiteUrl = settings.websiteUrl,
                        onBadgeClick = { showBadgeDialog = true },
                        modifier = Modifier.weight(1f),
                    )

                    // Voice Call Button
                    IconButton(
                        onClick = { showVoiceCall = true },
                        modifier = Modifier.testTag("voice_call_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "اتصال صوتي",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Change / Cycle Animated Wallpaper Button
                    IconButton(
                        onClick = {
                            autoCycleWallpaper = !autoCycleWallpaper
                            onCycleWallpaper((chat.wallpaperIndex + 1) % 4)
                        },
                        modifier = Modifier.testTag("wallpaper_toggle_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Wallpaper,
                            contentDescription = "تبديل الخلفيات المتحركة",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    // Internet-Cutoff Block Button
                    IconButton(
                        onClick = onToggleInternetBlock,
                        modifier = Modifier.testTag("internet_block_button"),
                    ) {
                        Icon(
                            imageVector = if (chat.isInternetBlocked) Icons.Filled.WifiOff else Icons.Filled.Block,
                            contentDescription = if (chat.isInternetBlocked) "إعادة الإنترنت للمحادثة" else "حظر بقطع الإنترنت",
                            tint = if (chat.isInternetBlocked) storeColors.blockedBanner else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Animated Top Marquee Ticker (Instagram & TikTok + Video Catalog link)
            AnimatedSocialTickerBar(
                settings = settings,
                onOpenVideoCatalog = onOpenVideoCatalog,
                containerColor = chatPalette.tickerContainer,
                contentColor = chatPalette.tickerContent,
            )

            // In-Chat Theme Selector Strip (تيم المحادثة)
            Surface(color = chatPalette.headerContainer.copy(alpha = 0.86f)) {
                ChatThemeSelectorStrip(
                    selectedPreset = activeChatTheme,
                    onSelectPreset = onUpdateChatTheme,
                )
            }

            // Internet Blocked Banner
            AnimatedVisibility(visible = chat.isInternetBlocked) {
                Surface(
                    color = storeColors.blockedBanner,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WifiOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "محادثة محظورة: تم قطع الإنترنت عن هذه المحادثة",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        TextButton(onClick = onToggleInternetBlock) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة الاتصال", color = Color.White)
                        }
                    }
                }
            }

            // Pinned In-Chat Advertisement Banner
            if (chat.activeAdTitle.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = "إعلان داخل المحادثة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.activeAdTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (chat.activeAdSubtitle.isNotBlank()) {
                                Text(
                                    text = chat.activeAdSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.88f),
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Shipping Waybill Card inside Chat
            AnimatedVisibility(visible = showWaybillCard) {
                ShippingWaybillCard(
                    chat = chat,
                    isOwnerMode = isOwnerMode,
                    onEditWaybillClick = { showWaybillDialog = true },
                )
            }

            // Quick Action Row inside Chat (Waybill, AI Auto-Fill, Product+Price, Ad, AI assistant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showWaybillCard = !showWaybillCard },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showWaybillCard) "إخفاء البوليصة" else "بوليصة الشحن",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                OutlinedButton(
                    onClick = onAutoFillCustomerAi,
                    enabled = aiState !is AiUiState.Loading && !chat.isInternetBlocked,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("ai_autofill_waybill_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعبئة بيانات العميل AI", style = MaterialTheme.typography.labelSmall)
                }

                OutlinedButton(
                    onClick = { showProductPickerDialog = true },
                    enabled = !chat.isInternetBlocked && products.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("send_product_in_chat_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("منتج وسعر", style = MaterialTheme.typography.labelSmall)
                }

                if (isOwnerMode) {
                    OutlinedButton(
                        onClick = { showAdDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("post_ad_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نشر إعلان", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val query = messageText.ifBlank { "رشح لي أفضل دبة تلوث وعادم رياضي من الكتالوج مع توضيح الشحن" }
                        onAskAi(query)
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("ask_ai_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("رد الذكاء الاصطناعي", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        outgoingBubbleColor = chatPalette.outgoingBubble,
                        incomingBubbleColor = chatPalette.incomingBubble,
                        onPlaySound = { key -> onPlaySoundPreview(EngineAlertSound.fromKey(key)) },
                    )
                }

                if (aiState is AiUiState.Loading || aiState is AiUiState.Streaming) {
                    item(key = "ai_streaming_bubble") {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                            modifier = Modifier.widthIn(max = 320.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (aiState is AiUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text("جاري تفكير الذكاء الاصطناعي...", style = MaterialTheme.typography.bodySmall)
                                } else if (aiState is AiUiState.Streaming) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(aiState.text, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            // Engine / Exhaust Sound Alert Selector Strip
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                EngineAlertSoundStrip(
                    selectedSound = EngineAlertSound.fromKey(chat.alertSound),
                    playingSound = playingSound,
                    onSelectAndSendSound = onSendSoundAlert,
                )
            }

            // Message Composer
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        enabled = !chat.isInternetBlocked,
                        placeholder = {
                            Text(
                                if (chat.isInternetBlocked) {
                                    "المحادثة مقطوعة الإنترنت..."
                                } else if (isOwnerMode) {
                                    "اكتب رد المتجر أو سؤالاً للذكاء الاصطناعي..."
                                } else {
                                    "اكتب استفسارك عن العادم أو دبة التلوث..."
                                }
                            )
                        },
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input"),
                    )

                    FilledIconButton(
                        onClick = {
                            val txt = messageText
                            messageText = ""
                            onSendMessage(txt, !isOwnerMode)
                        },
                        enabled = !chat.isInternetBlocked && messageText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_message_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                        )
                    }
                }
            }
        }
    }

    if (showWaybillDialog) {
        WaybillEditorDialog(
            chat = chat,
            isOwnerMode = isOwnerMode,
            onDismiss = { showWaybillDialog = false },
            onSave = onSaveWaybill,
        )
    }

    if (showVoiceCall) {
        VoiceCallOverlayDialog(
            peerName = if (isOwnerMode) chat.customerName else settings.storeName,
            storeName = settings.storeName,
            onEndCall = { secs ->
                showVoiceCall = false
                onLogVoiceCall(secs)
            },
        )
    }

    if (showAdDialog) {
        PostInChatAdDialog(
            products = products,
            onDismiss = { showAdDialog = false },
            onPost = { title, subtitle, prodId ->
                onSendAd(title, subtitle, prodId)
                showAdDialog = false
            },
        )
    }

    if (showBadgeDialog) {
        BlueBadgeVerificationDialog(
            settings = settings,
            onOpenSubscriptionSettings = onOpenSubscriptionSettings,
            onDismiss = { showBadgeDialog = false },
        )
    }

    if (showProductPickerDialog) {
        PickProductForChatDialog(
            products = products,
            onDismiss = { showProductPickerDialog = false },
            onSelectProduct = { prod ->
                onSendProductToChat(prod)
                showProductPickerDialog = false
            },
        )
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    outgoingBubbleColor: Color,
    incomingBubbleColor: Color,
    onPlaySound: (String) -> Unit,
) {
    val isOutgoing = message.senderRole == "owner"
    val isSystem = message.senderRole == "system"
    val isAi = message.senderRole == "ai"

    if (isSystem) {
        val isSmsCode = message.messageType == "sms_code"
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSmsCode) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.90f)
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (isSmsCode) {
                        Icon(
                            imageVector = Icons.Filled.Sms,
                            contentDescription = "رسالة كود SMS",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSmsCode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSmsCode) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isOutgoing) 18.dp else 4.dp,
                bottomEnd = if (isOutgoing) 4.dp else 18.dp,
            ),
            color = when {
                isAi -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                isOutgoing -> outgoingBubbleColor.copy(alpha = 0.95f)
                else -> incomingBubbleColor.copy(alpha = 0.95f)
            },
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 310.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (isAi) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "مساعد المتجر الذكي",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else if (message.messageType == "ad") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = message.adTitle.ifBlank { "إعلان المتجر" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else if (message.messageType == "product") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "منتج • ${message.adTitle.ifBlank { "الكتالوج" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAi) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                )

                if (message.messageType == "sound_alert" && message.soundType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onPlaySound(message.soundType) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "تشغيل الصوت",
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تشغيل صوت ${EngineAlertSound.fromKey(message.soundType).labelAr}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickProductForChatDialog(
    products: List<CatalogProduct>,
    onDismiss: () -> Unit,
    onSelectProduct: (CatalogProduct) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "إرسال منتج وسعر للمحادثة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(products, key = { it.id }) { prod ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProduct(prod) },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = prod.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${prod.priceSar.toInt()} ر.س",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontFamily = SpaceGroteskFontFamily,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("إغلاق") }
                }
            }
        }
    }
}

@Composable
private fun PostInChatAdDialog(
    products: List<CatalogProduct>,
    onDismiss: () -> Unit,
    onPost: (String, String, String) -> Unit,
) {
    var title by remember { mutableStateOf("عرض خاص: دبات التلوث وأنظمة العادم") }
    var subtitle by remember { mutableStateOf("ضمان 3 سنوات وشحن سريع لجميع المناطق") }
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id.orEmpty()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "إرسال إعلان داخل المحادثة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الإعلان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("تفاصيل العرض") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (products.isNotEmpty()) {
                    Text(
                        text = "ربط بمنتج من الكتالوج:",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    products.take(3).forEach { prod ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedProductId == prod.id) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            onClick = {
                                selectedProductId = prod.id
                                title = "عرض ${prod.name}"
                                subtitle = "${prod.description.take(80)} • السعر ${prod.priceSar.toInt()} ر.س"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "${prod.name} (${prod.priceSar.toInt()} ر.س)",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onPost(title, subtitle, selectedProductId) },
                        enabled = title.isNotBlank(),
                    ) {
                        Text("نشر بالمحادثة")
                    }
                }
            }
        }
    }
}
