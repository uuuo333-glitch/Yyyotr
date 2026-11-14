package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.EngineSoundPlayer
import com.example.data.ai.ExhaustAiAssistant
import com.example.data.model.StoreSettings
import com.example.data.model.StoreThemeOption
import com.example.data.prefs.ThemePreferences
import com.example.data.repository.StoreRepository
import com.example.security.configureAppCheck
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoreHubScreen
import com.example.ui.screens.SubscriptionsAdminScreen
import com.example.ui.theme.TurboExhaustTheme
import com.example.ui.viewmodel.DataUiState
import com.example.ui.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private enum class StoreTab(
    val route: String,
    val labelAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    CHATS("chats", "المحادثات", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
    CATALOG("catalog", "الكتالوج", Icons.Filled.ViewModule, Icons.Outlined.ViewModule),
    SUBSCRIPTIONS("subscriptions", "الاشتراكات", Icons.Filled.CardMembership, Icons.Outlined.CardMembership),
    STORE("store", "المتجر", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    SETTINGS("settings", "الألوان", Icons.Filled.Palette, Icons.Outlined.Palette),
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val themePrefs = ThemePreferences(applicationContext)

        setContent {
            val savedTheme by themePrefs.themeFlow.collectAsStateWithLifecycle(
                initialValue = StoreThemeOption.EMERALD
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                TurboExhaustTheme(themeOption = savedTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppRootNavigation(
                            themePrefs = themePrefs,
                            currentTheme = savedTheme,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRootNavigation(
    themePrefs: ThemePreferences,
    currentTheme: StoreThemeOption,
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedStoreShell(
                currentUserId = uid,
                themePrefs = themePrefs,
                currentTheme = currentTheme,
            )
        }
    }
}

@Composable
private fun AuthenticatedStoreShell(
    currentUserId: String,
    themePrefs: ThemePreferences,
    currentTheme: StoreThemeOption,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: StoreViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoreViewModel(
                    repository = StoreRepository(db),
                    themePreferences = themePrefs,
                    aiAssistant = ExhaustAiAssistant(),
                    soundPlayer = EngineSoundPlayer(),
                    currentUserId = currentUserId,
                )
            }
        }
    )

    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val activeChat by viewModel.activeChatState.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessagesState.collectAsStateWithLifecycle()
    val isOwnerMode by viewModel.isOwnerMode.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val playingSound by viewModel.soundPlayer.playingSound.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val seoGenerating by viewModel.seoGenerating.collectAsStateWithLifecycle()
    val customerSubscriptionsState by viewModel.customerSubscriptionsState.collectAsStateWithLifecycle()
    val smsAlertLogsState by viewModel.smsAlertLogsState.collectAsStateWithLifecycle()
    val globalAutoSmsEnabled by viewModel.globalAutoSmsEnabled.collectAsStateWithLifecycle()
    val pendingSmsDispatch by viewModel.pendingSmsDispatch.collectAsStateWithLifecycle()
    val aiSmsDraftLoading by viewModel.aiSmsDraftLoading.collectAsStateWithLifecycle()

    val settings = (settingsState as? DataUiState.Success)?.data ?: StoreSettings(userId = currentUserId)
    val products = (productsState as? DataUiState.Success)?.data ?: emptyList()
    val chats = (chatsState as? DataUiState.Success)?.data ?: emptyList()

    var currentTab by remember { mutableStateOf(StoreTab.CHATS) }
    var catalogInitialCategory by remember { mutableStateOf("الكل") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(settings.themeColorKey) {
        val remoteOption = StoreThemeOption.fromKey(settings.themeColorKey)
        if (remoteOption != currentTheme && settingsState is DataUiState.Success) {
            themePrefs.saveTheme(remoteOption)
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val openedChat = activeChat
    if (openedChat != null) {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatDetailScreen(
                chat = openedChat,
                messages = activeMessages,
                settings = settings,
                products = products,
                isOwnerMode = isOwnerMode,
                aiState = aiState,
                playingSound = playingSound,
                onBack = { viewModel.selectChat(null) },
                onOpenVideoCatalog = {
                    viewModel.selectChat(null)
                    catalogInitialCategory = "مقاطع الفيديو"
                    currentTab = StoreTab.CATALOG
                },
                onToggleInternetBlock = { viewModel.toggleChatInternetBlock(openedChat) },
                onSendMessage = { text, askAiToo -> viewModel.sendChatMessage(openedChat, text, askAiToo) },
                onAskAi = { prompt -> viewModel.askAiInChat(openedChat, prompt) },
                onSendSoundAlert = { sound -> viewModel.sendSoundAlert(openedChat, sound) },
                onPlaySoundPreview = { sound -> viewModel.soundPlayer.play(sound) },
                onCycleWallpaper = { nextIdx -> viewModel.cycleChatWallpaper(openedChat, nextIdx) },
                onSendAd = { title, sub, prodId -> viewModel.sendInChatAd(openedChat, title, sub, prodId) },
                onSaveWaybill = { cName, addr, reg, comp, days, wb, status ->
                    viewModel.saveWaybill(openedChat, cName, addr, reg, comp, days, wb, status)
                },
                onLogVoiceCall = { secs -> viewModel.logVoiceCall(openedChat, secs) },
                onUpdateChatTheme = { preset -> viewModel.updateChatTheme(openedChat, preset) },
                onAutoFillCustomerAi = { viewModel.autoFillCustomerDataWithAi(openedChat, activeMessages) },
                onSendProductToChat = { product -> viewModel.sendProductCardToChat(openedChat, product) },
                onOpenSubscriptionSettings = {
                    viewModel.selectChat(null)
                    currentTab = StoreTab.SETTINGS
                },
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        StoreTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (tab != StoreTab.CATALOG) catalogInitialCategory = "الكل"
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.labelAr,
                                    )
                                },
                                label = { Text(tab.labelAr) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail {
                        StoreTab.entries.forEach { tab ->
                            val selected = tab == currentTab
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (tab != StoreTab.CATALOG) catalogInitialCategory = "الكل"
                                    currentTab = tab
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.labelAr,
                                    )
                                },
                                label = { Text(tab.labelAr) },
                                modifier = Modifier.testTag("nav_rail_${tab.route}"),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                    ) {
                        when (currentTab) {
                            StoreTab.CHATS -> ChatsListScreen(
                                settings = settings,
                                chatsState = chatsState,
                                isOwnerMode = isOwnerMode,
                                onToggleOwnerMode = viewModel::toggleOwnerMode,
                                onSelectChat = { chatId -> viewModel.selectChat(chatId) },
                                onCreateChat = { name, phone ->
                                    viewModel.createNewChat(name, phone) { newId ->
                                        viewModel.selectChat(newId)
                                    }
                                },
                                onOpenSubscriptionSettings = { currentTab = StoreTab.SETTINGS },
                            )

                            StoreTab.CATALOG -> CatalogScreen(
                                settings = settings,
                                productsState = productsState,
                                isOwnerMode = isOwnerMode,
                                initialCategoryFilter = catalogInitialCategory,
                                seoGenerating = seoGenerating,
                                onCreateProduct = viewModel::createCatalogProduct,
                                onDeleteProduct = viewModel::deleteProduct,
                                onUpdateKeywords = viewModel::updateProductKeywords,
                                onGenerateAiSeo = viewModel::generateAiSeoKeywords,
                                onSendProductToActiveChat = { product ->
                                    val targetChat = chats.firstOrNull()
                                    if (targetChat != null) {
                                        viewModel.sendProductCardToChat(targetChat, product)
                                        viewModel.selectChat(targetChat.id)
                                    } else {
                                        viewModel.showMessage("أنشئ محادثة أولاً لمشاركة المنتج")
                                    }
                                },
                                onPlayVideoSound = { sound -> viewModel.soundPlayer.play(sound) },
                                onOpenSubscriptionSettings = { currentTab = StoreTab.SETTINGS },
                            )

                            StoreTab.SUBSCRIPTIONS -> SubscriptionsAdminScreen(
                                settings = settings,
                                subscriptionsState = customerSubscriptionsState,
                                smsLogsState = smsAlertLogsState,
                                globalAutoSmsEnabled = globalAutoSmsEnabled,
                                pendingSmsDispatch = pendingSmsDispatch,
                                aiSmsDraftLoading = aiSmsDraftLoading,
                                onToggleGlobalAutoSms = viewModel::toggleGlobalAutoSms,
                                onScanAndSendAutoAlerts = viewModel::triggerManualAutoExpiryScan,
                                onDismissPendingSms = viewModel::dismissPendingSmsBanner,
                                onSaveSubscription = viewModel::saveCustomerSubscription,
                                onAdjustDays = viewModel::adjustSubscriptionDays,
                                onRenewSubscription = viewModel::renewCustomerSubscription,
                                onToggleCustomerAutoSms = viewModel::toggleCustomerAutoSms,
                                onSendManualSms = { sub, body, usedAi ->
                                    viewModel.sendManualSmsAlert(sub, body, isAiDrafted = usedAi)
                                },
                                onDraftAiSms = viewModel::draftAiSmsForSubscriber,
                                onDeleteSubscription = viewModel::deleteCustomerSubscription,
                                onNotify = viewModel::showMessage,
                            )

                            StoreTab.STORE -> StoreHubScreen(
                                settings = settings,
                                products = products,
                                chats = chats,
                                onPlaySound = { sound -> viewModel.soundPlayer.play(sound) },
                                onNotify = viewModel::showMessage,
                                onActivateSubscription = viewModel::activateSubscriptionPlan,
                                onVerifySmsCode = viewModel::verifySubscriptionSmsCode,
                                onOpenSubscriptionSettings = { currentTab = StoreTab.SETTINGS },
                                onOpenAdminSubscriptions = { currentTab = StoreTab.SUBSCRIPTIONS },
                            )

                            StoreTab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                currentTheme = currentTheme,
                                onSelectTheme = viewModel::updateThemeColor,
                                onSaveSettings = viewModel::saveStoreSettings,
                                onActivateSubscription = viewModel::activateSubscriptionPlan,
                                onVerifySmsCode = viewModel::verifySubscriptionSmsCode,
                                onSignOut = {
                                    signOut(credentialManager = credentialManager, scope = scope)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
