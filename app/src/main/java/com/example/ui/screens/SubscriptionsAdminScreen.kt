package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomerSubStatus
import com.example.data.model.CustomerSubscription
import com.example.data.model.SmsAlertLog
import com.example.data.model.StoreSettings
import com.example.data.model.SubscriptionPlan
import com.example.ui.components.AddOrEditSubscriptionDialog
import com.example.ui.components.AdjustDurationDialog
import com.example.ui.components.BlueBadgeVerificationDialog
import com.example.ui.components.SmsComposerDialog
import com.example.ui.components.VerifiedStoreTitleBlock
import com.example.ui.components.launchSmsIntent
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TurboAmber
import com.example.ui.viewmodel.DataUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsAdminScreen(
    settings: StoreSettings,
    subscriptionsState: DataUiState<List<CustomerSubscription>>,
    smsLogsState: DataUiState<List<SmsAlertLog>>,
    globalAutoSmsEnabled: Boolean,
    pendingSmsDispatch: SmsAlertLog?,
    aiSmsDraftLoading: Boolean,
    onToggleGlobalAutoSms: (Boolean) -> Unit,
    onScanAndSendAutoAlerts: () -> Unit,
    onDismissPendingSms: () -> Unit,
    onSaveSubscription: (
        existingId: String?,
        customerName: String,
        customerPhone: String,
        plan: SubscriptionPlan,
        remainingDays: Long,
        alertThresholdDays: Long,
        autoSmsEnabled: Boolean,
    ) -> Unit,
    onAdjustDays: (CustomerSubscription, Long, Long, Boolean) -> Unit,
    onRenewSubscription: (CustomerSubscription, SubscriptionPlan) -> Unit,
    onToggleCustomerAutoSms: (CustomerSubscription, Boolean) -> Unit,
    onSendManualSms: (CustomerSubscription, String, Boolean) -> Unit,
    onDraftAiSms: (CustomerSubscription, (String) -> Unit) -> Unit,
    onDeleteSubscription: (String) -> Unit,
    onNotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDurationSub by remember { mutableStateOf<CustomerSubscription?>(null) }
    var composingSmsSub by remember { mutableStateOf<CustomerSubscription?>(null) }
    var showBadgeDialog by remember { mutableStateOf(false) }

    val subscriptions = (subscriptionsState as? DataUiState.Success)?.data ?: emptyList()
    val smsLogs = (smsLogsState as? DataUiState.Success)?.data ?: emptyList()

    val activeCount = subscriptions.count { it.status == CustomerSubStatus.ACTIVE.key }
    val expiringCount = subscriptions.count { it.status == CustomerSubStatus.EXPIRING_SOON.key }
    val expiredCount = subscriptions.count { it.status == CustomerSubStatus.EXPIRED.key }

    val filteredSubs = remember(subscriptions, searchQuery, statusFilter) {
        val q = searchQuery.trim()
        subscriptions.filter { sub ->
            val matchesFilter = statusFilter == "all" || sub.status == statusFilter
            val matchesQuery = q.isEmpty() ||
                sub.customerName.contains(q, ignoreCase = true) ||
                sub.customerPhone.contains(q, ignoreCase = true) ||
                sub.planLabelAr.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    VerifiedStoreTitleBlock(
                        storeName = settings.storeName,
                        tagline = "إدارة اشتراكات العملاء وتنبيهات SMS",
                        verified = settings.verifiedBadge,
                        customAppNameUnderBadge = settings.customAppNameUnderBadge,
                        websiteUrl = settings.websiteUrl,
                        onBadgeClick = { showBadgeDialog = true },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "إضافة مشترك") },
                text = { Text("إضافة مشترك") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_customer_sub_fab"),
            )
        },
    ) { innerPadding ->
        when (subscriptionsState) {
            is DataUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is DataUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = subscriptionsState.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            is DataUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Summary metrics row
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SubStatCard("الإجمالي", subscriptions.size, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                            SubStatCard("نشط", activeCount, OnlineGreen, Modifier.weight(1f))
                            SubStatCard("قريب الانتهاء", expiringCount, TurboAmber, Modifier.weight(1f))
                            SubStatCard("منتهي", expiredCount, BlockedRed, Modifier.weight(1f))
                        }
                    }

                    // Automatic SMS expiry alert engine card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.NotificationsActive,
                                            contentDescription = null,
                                            tint = TurboAmber,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "التنبيه التلقائي عبر SMS",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = "يرسل رسالة نصية عند وصول المدة المتبقية لحد التنبيه ($expiringCount عملاء)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = globalAutoSmsEnabled,
                                        onCheckedChange = onToggleGlobalAutoSms,
                                        modifier = Modifier.testTag("global_auto_sms_switch"),
                                    )
                                }

                                Button(
                                    onClick = onScanAndSendAutoAlerts,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("scan_auto_sms_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("فحص وإرسال تنبيهات قرب الانتهاء الآن")
                                }
                            }
                        }
                    }

                    // Quick-launch SMS app banner for pending auto/manual SMS
                    item {
                        AnimatedVisibility(visible = pendingSmsDispatch != null) {
                            pendingSmsDispatch?.let { pending ->
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = "رسالة SMS جاهزة للإرسال: ${pending.customerName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.weight(1f),
                                            )
                                            IconButton(
                                                onClick = onDismissPendingSms,
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    Icons.Filled.Close,
                                                    contentDescription = "إغلاق التنبيه",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${pending.customerPhone} • ${pending.messageBody}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Button(
                                            onClick = {
                                                launchSmsIntent(
                                                    context = context,
                                                    phoneNumber = pending.customerPhone,
                                                    messageBody = pending.messageBody,
                                                    onError = onNotify,
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("open_sms_app_banner_button"),
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فتح تطبيق الرسائل وإرسال SMS")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Search & Status Filter
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                label = { Text("ابحث باسم العميل أو رقم الجوال...") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sub_search_input"),
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf(
                                    "all" to "الكل (${subscriptions.size})",
                                    CustomerSubStatus.EXPIRING_SOON.key to "قريب الانتهاء ($expiringCount)",
                                    CustomerSubStatus.ACTIVE.key to "نشط ($activeCount)",
                                    CustomerSubStatus.EXPIRED.key to "منتهي ($expiredCount)",
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = statusFilter == key,
                                        onClick = { statusFilter = key },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                        modifier = Modifier.testTag("sub_filter_$key"),
                                    )
                                }
                            }
                        }
                    }

                    // Customer Subscription Cards
                    items(filteredSubs, key = { it.id }) { sub ->
                        CustomerSubscriptionCard(
                            sub = sub,
                            onDecrementDay = {
                                onAdjustDays(sub, (sub.remainingDays - 1L).coerceAtLeast(0L), sub.alertThresholdDays, false)
                            },
                            onIncrementDay = {
                                onAdjustDays(sub, (sub.remainingDays + 1L).coerceAtMost(366L), sub.alertThresholdDays, false)
                            },
                            onOpenDurationDialog = { editingDurationSub = sub },
                            onQuickRenewMonth = { onRenewSubscription(sub, SubscriptionPlan.ONE_MONTH) },
                            onToggleAutoSms = { enabled -> onToggleCustomerAutoSms(sub, enabled) },
                            onOpenSmsDialog = { composingSmsSub = sub },
                            onDelete = { onDeleteSubscription(sub.id) },
                        )
                    }

                    // SMS Alert History Log
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "سجل تنبيهات الرسائل النصية (SMS) • ${smsLogs.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (smsLogs.isEmpty()) {
                        item {
                            Text(
                                text = "لم يتم إرسال أي رسائل تنبيه SMS بعد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(smsLogs.take(15), key = { it.id }) { log ->
                            SmsLogItemCard(
                                log = log,
                                onLaunchSmsApp = {
                                    launchSmsIntent(context, log.customerPhone, log.messageBody, onNotify)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddOrEditSubscriptionDialog(
            onDismiss = { showAddDialog = false },
            onSave = onSaveSubscription,
        )
    }

    editingDurationSub?.let { sub ->
        AdjustDurationDialog(
            sub = sub,
            onDismiss = { editingDurationSub = null },
            onApply = { days, threshold, paused ->
                onAdjustDays(sub, days, threshold, paused)
            },
        )
    }

    composingSmsSub?.let { sub ->
        SmsComposerDialog(
            sub = sub,
            settings = settings,
            aiDraftLoading = aiSmsDraftLoading,
            onDraftAiSms = onDraftAiSms,
            onSendSms = onSendManualSms,
            onDismiss = { composingSmsSub = null },
            onNotify = onNotify,
        )
    }

    if (showBadgeDialog) {
        BlueBadgeVerificationDialog(
            settings = settings,
            onDismiss = { showBadgeDialog = false },
        )
    }
}

@Composable
private fun SubStatCard(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CustomerSubscriptionCard(
    sub: CustomerSubscription,
    onDecrementDay: () -> Unit,
    onIncrementDay: () -> Unit,
    onOpenDurationDialog: () -> Unit,
    onQuickRenewMonth: () -> Unit,
    onToggleAutoSms: (Boolean) -> Unit,
    onOpenSmsDialog: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusEnum = CustomerSubStatus.fromKey(sub.status)
    val statusColor = when (statusEnum) {
        CustomerSubStatus.ACTIVE -> OnlineGreen
        CustomerSubStatus.EXPIRING_SOON -> TurboAmber
        CustomerSubStatus.EXPIRED -> BlockedRed
        CustomerSubStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val planTotalDays = SubscriptionPlan.fromKey(sub.planKey).durationDays.coerceAtLeast(1L)
    val progress = (sub.remainingDays.toFloat() / planTotalDays.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_sub_card_${sub.id}"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sub.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${sub.customerPhone} • ${sub.planLabelAr}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.18f),
                ) {
                    Text(
                        text = statusEnum.labelAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "المدة المتبقية: ${sub.remainingDays} يوم",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                    Text(
                        text = "تنبيه عند ≤ ${sub.alertThresholdDays} يوم • (${sub.smsAlertCount} تنبيه)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
            }

            // Duration controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDecrementDay,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("-1 يوم")
                }
                OutlinedButton(
                    onClick = onIncrementDay,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Text("+1 يوم")
                }
                OutlinedButton(
                    onClick = onOpenDurationDialog,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("set_duration_btn_${sub.id}"),
                ) {
                    Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تحديد المدة")
                }
                Button(
                    onClick = onQuickRenewMonth,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تجديد +30 يوم")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "تنبيه SMS تلقائي عند قرب الانتهاء",
                    style = MaterialTheme.typography.labelMedium,
                )
                Switch(
                    checked = sub.autoSmsAlertEnabled,
                    onCheckedChange = onToggleAutoSms,
                )
            }

            if (sub.lastSmsAlertText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "آخر SMS: ${sub.lastSmsAlertText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onOpenSmsDialog,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("send_sms_btn_${sub.id}"),
                ) {
                    Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إرسال SMS / AI")
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف اشتراك ${sub.customerName}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmsLogItemCard(
    log: SmsAlertLog,
    onLaunchSmsApp: () -> Unit,
) {
    val triggerLabel = when (log.triggerType) {
        "auto_expiry" -> "تلقائي (قرب الانتهاء)"
        "ai_drafted" -> "صياغة الذكاء الاصطناعي"
        else -> "إرسال يدوي"
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = log.customerName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = triggerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "${log.customerPhone} • المتبقي وقت التنبيه: ${log.remainingDaysAtSend} يوم",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = log.messageBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = onLaunchSmsApp) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "فتح الرسائل", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SMS")
            }
        }
    }
}
