package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StoreSettings
import com.example.data.model.StoreThemeOption
import com.example.data.model.SubscriptionPlan
import com.example.ui.components.SubscriptionAndBrandingCard
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalStoreColors
import com.example.ui.theme.TitaniumPrimary
import com.example.ui.theme.VioletPrimary

@Composable
fun SettingsScreen(
    settings: StoreSettings,
    currentTheme: StoreThemeOption,
    onSelectTheme: (StoreThemeOption) -> Unit,
    onSaveSettings: (StoreSettings) -> Unit,
    onActivateSubscription: (SubscriptionPlan, String, String, String) -> Unit,
    onVerifySmsCode: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var storeName by remember(settings.storeName) { mutableStateOf(settings.storeName) }
    var tagline by remember(settings.tagline) { mutableStateOf(settings.tagline) }
    var customAppNameUnderBadge by remember(settings.customAppNameUnderBadge) { mutableStateOf(settings.customAppNameUnderBadge) }
    var websiteUrl by remember(settings.websiteUrl) { mutableStateOf(settings.websiteUrl) }
    var verifiedBadge by remember(settings.verifiedBadge) { mutableStateOf(settings.verifiedBadge) }
    var instagramHandle by remember(settings.instagramHandle) { mutableStateOf(settings.instagramHandle) }
    var tiktokHandle by remember(settings.tiktokHandle) { mutableStateOf(settings.tiktokHandle) }
    var appDownloadUrl by remember(settings.appDownloadUrl) { mutableStateOf(settings.appDownloadUrl) }
    var defaultShippingCompany by remember(settings.defaultShippingCompany) { mutableStateOf(settings.defaultShippingCompany) }
    var defaultDaysText by remember(settings.defaultEstimatedDays) { mutableStateOf(settings.defaultEstimatedDays.toString()) }
    val storeColors = LocalStoreColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // App Color Customization Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "تغيير لون التطبيق",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تغيير لون التطبيق",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        text = "اختر اللون الرئيسي لهوية متجرك وسيتم تطبيقه فوراً على كامل التطبيق:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StoreThemeOption.entries.forEach { option ->
                            val swatchColor = when (option) {
                                StoreThemeOption.EMERALD -> EmeraldPrimary
                                StoreThemeOption.CRIMSON -> CrimsonPrimary
                                StoreThemeOption.TITANIUM -> TitaniumPrimary
                                StoreThemeOption.AMBER -> AmberPrimary
                                StoreThemeOption.VIOLET -> VioletPrimary
                            }
                            val selected = option == currentTheme
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectTheme(option) }
                                    .padding(4.dp)
                                    .testTag("theme_swatch_${option.key}"),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = option.labelAr,
                                            tint = Color.Black,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.labelAr.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Store Identity, Verified Badge, Social Handles & Shipping Defaults
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "إعدادات المتجر والتوثيق والشحن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("اسم المتجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("الوصف تحت علامة التوثيق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = customAppNameUnderBadge,
                        onValueChange = { customAppNameUnderBadge = it },
                        label = { Text("اسم التطبيق تحت العلامة الزرقاء") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("رابط الموقع الرسمي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = storeColors.verifiedBadge,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إظهار العلامة الزرقاء الموثقة")
                        }
                        Switch(
                            checked = verifiedBadge,
                            onCheckedChange = { verifiedBadge = it },
                        )
                    }

                    OutlinedTextField(
                        value = instagramHandle,
                        onValueChange = { instagramHandle = it },
                        label = { Text("حساب إنستقرام في الشريط المتحرك") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = tiktokHandle,
                        onValueChange = { tiktokHandle = it },
                        label = { Text("حساب تيك توك في الشريط المتحرك") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = appDownloadUrl,
                        onValueChange = { appDownloadUrl = it },
                        label = { Text("رابط تحميل تطبيق المتجر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = defaultShippingCompany,
                            onValueChange = { defaultShippingCompany = it },
                            label = { Text("شركة الشحن الافتراضية") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f),
                        )
                        OutlinedTextField(
                            value = defaultDaysText,
                            onValueChange = { if (it.all { c -> c.isDigit() }) defaultDaysText = it },
                            label = { Text("أيام التوصيل") },
                            singleLine = true,
                            modifier = Modifier.weight(0.7f),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            onSaveSettings(
                                settings.copy(
                                    storeName = storeName,
                                    tagline = tagline,
                                    customAppNameUnderBadge = customAppNameUnderBadge,
                                    websiteUrl = websiteUrl,
                                    verifiedBadge = verifiedBadge,
                                    instagramHandle = instagramHandle,
                                    tiktokHandle = tiktokHandle,
                                    appDownloadUrl = appDownloadUrl,
                                    defaultShippingCompany = defaultShippingCompany,
                                    defaultEstimatedDays = (defaultDaysText.toLongOrNull() ?: 3L).coerceIn(0L, 90L),
                                    themeColorKey = currentTheme.key,
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_settings_button"),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الإعدادات")
                    }
                }
            }
        }

        // Subscription & SMS Activation Card
        item {
            SubscriptionAndBrandingCard(
                settings = settings,
                onActivateSubscription = onActivateSubscription,
                onVerifySmsCode = onVerifySmsCode,
            )
        }

        // Account & Sign Out
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "حساب المتجر الموثق",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("owner_email_hidden_badge"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = "إيميل المالك مخفي",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حساب المالك مفعل • البريد الإلكتروني مخفي للخصوصية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sign_out_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل الخروج")
                    }
                }
            }
        }
    }
}
