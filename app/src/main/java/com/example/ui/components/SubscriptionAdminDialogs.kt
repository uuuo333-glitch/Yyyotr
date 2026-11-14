package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CustomerSubStatus
import com.example.data.model.CustomerSubscription
import com.example.data.model.StoreSettings
import com.example.data.model.SubscriptionPlan

@Composable
fun AddOrEditSubscriptionDialog(
    initialSub: CustomerSubscription? = null,
    onDismiss: () -> Unit,
    onSave: (
        existingId: String?,
        customerName: String,
        customerPhone: String,
        plan: SubscriptionPlan,
        remainingDays: Long,
        alertThresholdDays: Long,
        autoSmsEnabled: Boolean,
    ) -> Unit,
) {
    var customerName by remember { mutableStateOf(initialSub?.customerName.orEmpty()) }
    var customerPhone by remember { mutableStateOf(initialSub?.customerPhone ?: "+9665") }
    var selectedPlan by remember {
        mutableStateOf(
            SubscriptionPlan.fromKey(initialSub?.planKey ?: SubscriptionPlan.ONE_MONTH.key)
        )
    }
    var remainingDaysText by remember {
        mutableStateOf((initialSub?.remainingDays ?: selectedPlan.durationDays).toString())
    }
    var thresholdDaysText by remember {
        mutableStateOf((initialSub?.alertThresholdDays ?: 3L).toString())
    }
    var autoSmsEnabled by remember { mutableStateOf(initialSub?.autoSmsAlertEnabled ?: true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = if (initialSub == null) "إضافة اشتراك عميل جديد" else "تعديل اشتراك العميل",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("اسم العميل") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_dialog_name_input"),
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("رقم الجوال لإرسال SMS") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sub_dialog_phone_input"),
                )

                Text(
                    text = "اختر الباقة (25 ر.س / شهر):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SubscriptionPlan.entries.forEach { plan ->
                        FilterChip(
                            selected = selectedPlan == plan,
                            onClick = {
                                selectedPlan = plan
                                if (initialSub == null) {
                                    remainingDaysText = plan.durationDays.toString()
                                }
                            },
                            label = { Text(plan.labelAr, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = remainingDaysText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) remainingDaysText = input
                        },
                        label = { Text("الأيام المتبقية") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_dialog_days_input"),
                    )
                    OutlinedTextField(
                        value = thresholdDaysText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) thresholdDaysText = input
                        },
                        label = { Text("تنبيه قبل (أيام)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_dialog_threshold_input"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "إرسال تنبيه SMS تلقائي عند قرب الانتهاء",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = autoSmsEnabled,
                        onCheckedChange = { autoSmsEnabled = it },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customerName.isNotBlank() && customerPhone.isNotBlank()) {
                                val days = (remainingDaysText.toLongOrNull() ?: selectedPlan.durationDays).coerceIn(0L, 366L)
                                val threshold = (thresholdDaysText.toLongOrNull() ?: 3L).coerceIn(1L, 30L)
                                onSave(
                                    initialSub?.id,
                                    customerName,
                                    customerPhone,
                                    selectedPlan,
                                    days,
                                    threshold,
                                    autoSmsEnabled,
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("sub_dialog_save_button"),
                    ) {
                        Text("حفظ الاشتراك")
                    }
                }
            }
        }
    }
}

@Composable
fun AdjustDurationDialog(
    sub: CustomerSubscription,
    onDismiss: () -> Unit,
    onApply: (remainingDays: Long, thresholdDays: Long, paused: Boolean) -> Unit,
) {
    var daysText by remember { mutableStateOf(sub.remainingDays.toString()) }
    var thresholdText by remember { mutableStateOf(sub.alertThresholdDays.toString()) }
    var isPaused by remember { mutableStateOf(sub.status == CustomerSubStatus.PAUSED.key) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "تحديد المدة المتبقية — ${sub.customerName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(2L to "يومان (قريب الانتهاء)", 5L to "5 أيام", 30L to "شهر (30 يوم)", 90L to "3 أشهر", 0L to "منتهي (0)")
                        .forEach { (presetDays, label) ->
                            FilterChip(
                                selected = daysText.toLongOrNull() == presetDays,
                                onClick = { daysText = presetDays.toString() },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = daysText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) daysText = it },
                        label = { Text("الأيام المتبقية") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adjust_days_field"),
                    )
                    OutlinedTextField(
                        value = thresholdText,
                        onValueChange = { if (it.all { ch -> ch.isDigit() }) thresholdText = it },
                        label = { Text("عتبة التنبيه (أيام)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adjust_threshold_field"),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("إيقاف الاشتراك مؤقتاً", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = isPaused, onCheckedChange = { isPaused = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val d = (daysText.toLongOrNull() ?: sub.remainingDays).coerceIn(0L, 366L)
                            val t = (thresholdText.toLongOrNull() ?: sub.alertThresholdDays).coerceIn(1L, 30L)
                            onApply(d, t, isPaused)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("confirm_adjust_days_button"),
                    ) {
                        Text("تطبيق المدة")
                    }
                }
            }
        }
    }
}

@Composable
fun SmsComposerDialog(
    sub: CustomerSubscription,
    settings: StoreSettings,
    aiDraftLoading: Boolean,
    onDraftAiSms: (CustomerSubscription, (String) -> Unit) -> Unit,
    onSendSms: (CustomerSubscription, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val defaultText = remember(sub.id, settings.storeName) {
        val daysPhrase = if (sub.remainingDays <= 0L) "انتهى اليوم" else "متبقي ${sub.remainingDays} أيام على انتهائه"
        "مرحباً ${sub.customerName}، نذكركم من ${settings.storeName} أن اشتراك (${sub.planLabelAr}) $daysPhrase. للتجديد السريع بـ 25 ر.س/شهر: ${settings.websiteUrl}"
    }
    var messageBody by remember { mutableStateOf(defaultText) }
    var usedAi by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sms, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إرسال رسالة نصية SMS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = "المستلم: ${sub.customerName} (${sub.customerPhone})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                OutlinedButton(
                    onClick = {
                        onDraftAiSms(sub) { drafted ->
                            messageBody = drafted
                            usedAi = true
                        }
                    },
                    enabled = !aiDraftLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_draft_sms_button"),
                ) {
                    if (aiDraftLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري صياغة الرسالة بالذكاء الاصطناعي...")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صياغة رسالة تنبيه احترافية بالذكاء الاصطناعي")
                    }
                }

                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    label = { Text("نص الرسالة النصية القصيرة") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sms_body_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            if (messageBody.isNotBlank()) {
                                onSendSms(sub, messageBody, usedAi)
                                launchSmsIntent(context, sub.customerPhone, messageBody, onNotify)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_send_sms_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل وإرسال SMS")
                    }
                }
            }
        }
    }
}

fun launchSmsIntent(
    context: android.content.Context,
    phoneNumber: String,
    messageBody: String,
    onError: (String) -> Unit,
) {
    val cleanPhone = phoneNumber.trim()
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:${Uri.encode(cleanPhone)}")
        putExtra("sms_body", messageBody)
    }
    runCatching {
        context.startActivity(intent)
    }.onFailure {
        onError("تم حفظ سجل الرسالة النصية (لم يتم العثور على تطبيق رسائل SMS على هذا الجهاز)")
    }
}
