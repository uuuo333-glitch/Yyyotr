package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CatalogProduct
import com.example.data.model.ChatConversation
import com.example.data.model.EngineAlertSound
import com.example.data.model.StoreSettings
import com.example.data.model.SubscriptionPlan
import com.example.ui.components.BlueBadgeVerificationDialog
import com.example.ui.components.SubscriptionAndBrandingCard
import com.example.ui.theme.LocalStoreColors
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun StoreHubScreen(
    settings: StoreSettings,
    products: List<CatalogProduct>,
    chats: List<ChatConversation>,
    onPlaySound: (EngineAlertSound) -> Unit,
    onNotify: (String) -> Unit,
    onActivateSubscription: (SubscriptionPlan, String, String, String) -> Unit,
    onVerifySmsCode: (String) -> Unit,
    onOpenSubscriptionSettings: () -> Unit,
    onOpenAdminSubscriptions: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val storeColors = LocalStoreColors.current
    var showBadgeDialog by remember { mutableStateOf(false) }

    val blockedCount = chats.count { it.isInternetBlocked }
    val activeShipments = chats.count { it.waybillNumber.isNotBlank() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Store Brand Hero Banner with Verified Blue Badge
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_store_banner),
                            contentDescription = settings.storeName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showBadgeDialog = true }
                                .testTag("hub_verified_badge_trigger")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = settings.storeName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (settings.verifiedBadge) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = "علامة التوثيق الزرقاء",
                                        tint = storeColors.verifiedBadge,
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = storeColors.verifiedBadge.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            text = "تحقق",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = storeColors.verifiedBadge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = settings.tagline,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (settings.customAppNameUnderBadge.isNotBlank() || settings.websiteUrl.isNotBlank()) {
                                Text(
                                    text = "${settings.customAppNameUnderBadge} • ${settings.websiteUrl}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable {
                                        if (settings.websiteUrl.isNotBlank()) {
                                            runCatching {
                                                val normalized = if (settings.websiteUrl.startsWith("http")) {
                                                    settings.websiteUrl
                                                } else {
                                                    "https://${settings.websiteUrl}"
                                                }
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StoreStatPill(
                            icon = Icons.Filled.Inventory2,
                            label = "المنتجات",
                            value = products.size.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StoreStatPill(
                            icon = Icons.Filled.LocalShipping,
                            label = "البوليصات",
                            value = activeShipments.toString(),
                            modifier = Modifier.weight(1f),
                        )
                        StoreStatPill(
                            icon = Icons.Filled.WifiOff,
                            label = "محظور",
                            value = blockedCount.toString(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Subscription & Branding (25 SAR/month, 5-day free trial, SMS code, custom name & website)
        item {
            SubscriptionAndBrandingCard(
                settings = settings,
                onActivateSubscription = onActivateSubscription,
                onVerifySmsCode = onVerifySmsCode,
            )
        }

        // Quick Shortcut to Customer Subscriptions & Automated SMS Admin
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardMembership,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "لوحة إدارة اشتراكات العملاء",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "متابعة الحالات، تعديل المدة المتبقية، وتنبيهات SMS التلقائية",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onOpenAdminSubscriptions,
                        modifier = Modifier.testTag("open_admin_subs_from_hub"),
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الإدارة")
                    }
                }
            }
        }

        // Download / Share Dedicated Store App Card
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
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحميل ومشاركة تطبيق المتجر الخاص بك",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        StoreQrCodeBadge(
                            seed = settings.appDownloadUrl,
                            modifier = Modifier.size(88.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "رابط تثبيت تطبيق ${settings.storeName}:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = settings.appDownloadUrl,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = SpaceGroteskFontFamily,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "حمّل تطبيق ${settings.storeName} (${settings.tagline}): ${settings.appDownloadUrl}",
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "مشاركة رابط التطبيق"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_store_app_button"),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاركة التحميل")
                        }

                        OutlinedButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.appDownloadUrl))
                                onNotify("تم نسخ رابط تحميل تطبيق المتجر")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copy_app_link_button"),
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ الرابط")
                        }
                    }
                }
            }
        }

        // Social Channels & Engine Sounds Preview
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
                        text = "قنوات المتجر وأصوات المحركات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("انستقرام", style = MaterialTheme.typography.labelSmall)
                                    Text(settings.instagramHandle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("تيك توك", style = MaterialTheme.typography.labelSmall)
                                    Text(settings.tiktokHandle, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "تجربة أصوات تنبيه المحادثة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineAlertSound.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                pair.forEach { sound ->
                                    OutlinedButton(
                                        onClick = { onPlaySound(sound) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                    ) {
                                        Icon(Icons.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(sound.labelAr, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBadgeDialog) {
        BlueBadgeVerificationDialog(
            settings = settings,
            onOpenSubscriptionSettings = onOpenSubscriptionSettings,
            onDismiss = { showBadgeDialog = false },
        )
    }
}

@Composable
private fun StoreStatPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
            )
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StoreQrCodeBadge(
    seed: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val grid = 9
            val cell = size.minDimension / grid
            val hash = seed.hashCode()
            for (r in 0 until grid) {
                for (c in 0 until grid) {
                    val isCorner = (r < 3 && c < 3) || (r < 3 && c >= grid - 3) || (r >= grid - 3 && c < 3)
                    val filled = isCorner || (((hash xor (r * 31 + c * 17)) and 1) == 0)
                    if (filled) {
                        drawRect(
                            color = if (isCorner) primary else Color(0xFF0F172A),
                            topLeft = Offset(c * cell, r * cell),
                            size = Size(cell * 0.85f, cell * 0.85f),
                        )
                    }
                }
            }
        }
    }
}
