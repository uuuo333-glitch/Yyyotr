package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.ChatConversation
import com.example.data.model.EngineAlertSound
import com.example.data.model.StoreSettings
import com.example.ui.theme.LocalStoreColors
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlinx.coroutines.delay

@Composable
fun VerifiedStoreTitleBlock(
    storeName: String,
    tagline: String,
    verified: Boolean = true,
    customAppNameUnderBadge: String = "",
    websiteUrl: String = "",
    onBadgeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val storeColors = LocalStoreColors.current
    Column(
        modifier = modifier
            .then(
                if (onBadgeClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBadgeClick)
                        .padding(vertical = 2.dp)
                        .testTag("verified_badge_trigger")
                } else {
                    Modifier
                }
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = storeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (verified) {
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "علامة التوثيق الزرقاء",
                    tint = storeColors.verifiedBadge,
                    modifier = Modifier.size(19.dp),
                )
                if (onBadgeClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = storeColors.verifiedBadge.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = "تحقق",
                            style = MaterialTheme.typography.labelSmall,
                            color = storeColors.verifiedBadge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = tagline,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (customAppNameUnderBadge.isNotBlank() || websiteUrl.isNotBlank()) {
            val subLine = buildString {
                if (customAppNameUnderBadge.isNotBlank()) append(customAppNameUnderBadge)
                if (customAppNameUnderBadge.isNotBlank() && websiteUrl.isNotBlank()) append(" • ")
                if (websiteUrl.isNotBlank()) append(websiteUrl.removePrefix("https://").removePrefix("http://"))
            }
            Text(
                text = subLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun BlueBadgeVerificationDialog(
    settings: StoreSettings,
    onOpenSubscriptionSettings: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val storeColors = LocalStoreColors.current
    val plan = com.example.data.model.SubscriptionPlan.fromKey(settings.subscriptionPlan)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = storeColors.verifiedBadge.copy(alpha = 0.16f),
                    modifier = Modifier.size(68.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "حساب موثق",
                            tint = storeColors.verifiedBadge,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = settings.storeName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = storeColors.verifiedBadge,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = storeColors.verifiedBadge.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "حساب متجر موثوق ومعتمد • العلامة الزرقاء مفعّلة",
                        style = MaterialTheme.typography.labelMedium,
                        color = storeColors.verifiedBadge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "التخصص المعتمد تحت العلامة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = settings.tagline,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (settings.customAppNameUnderBadge.isNotBlank()) {
                            Text(
                                text = "اسم التطبيق المخصص: ${settings.customAppNameUnderBadge}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (settings.websiteUrl.isNotBlank()) {
                            Text(
                                text = "الموقع الرسمي: ${settings.websiteUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = SpaceGroteskFontFamily,
                                color = storeColors.verifiedBadge,
                                modifier = Modifier.clickable {
                                    runCatching {
                                        val normalized = if (settings.websiteUrl.startsWith("http")) {
                                            settings.websiteUrl
                                        } else {
                                            "https://${settings.websiteUrl}"
                                        }
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
                                    }
                                },
                            )
                        }
                        Text(
                            text = "حالة الاشتراك: ${plan.labelAr} (${settings.subscriptionDaysLeft} يوم متبقي)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (onOpenSubscriptionSettings != null) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenSubscriptionSettings()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("تعديل الاشتراك والرابط")
                        }
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_verification_dialog"),
                    ) {
                        Text("تم التحقق")
                    }
                }
            }
        }
    }
}

data class ChatThemePalette(
    val headerContainer: Color,
    val tickerContainer: Color,
    val tickerContent: Color,
    val outgoingBubble: Color,
    val incomingBubble: Color,
    val accent: Color,
)

@Composable
fun rememberChatThemePalette(preset: com.example.data.model.ChatThemePreset): ChatThemePalette {
    val storeColors = LocalStoreColors.current
    return when (preset) {
        com.example.data.model.ChatThemePreset.CLASSIC_WA -> ChatThemePalette(
            headerContainer = Color(0xFF0B2E26),
            tickerContainer = Color(0xFF103F34),
            tickerContent = Color(0xFFD9FDD3),
            outgoingBubble = Color(0xFF005C4B),
            incomingBubble = Color(0xFF1F2C34),
            accent = Color(0xFF25D366),
        )
        com.example.data.model.ChatThemePreset.CARBON_GOLD -> ChatThemePalette(
            headerContainer = Color(0xFF1C1917),
            tickerContainer = Color(0xFF2B2314),
            tickerContent = Color(0xFFFDE68A),
            outgoingBubble = Color(0xFF854D0E),
            incomingBubble = Color(0xFF292524),
            accent = Color(0xFFF59E0B),
        )
        com.example.data.model.ChatThemePreset.NEON_NIGHT -> ChatThemePalette(
            headerContainer = Color(0xFF0F172A),
            tickerContainer = Color(0xFF1E293B),
            tickerContent = Color(0xFFBAE6FD),
            outgoingBubble = Color(0xFF1D4ED8),
            incomingBubble = Color(0xFF1E293B),
            accent = Color(0xFF38BDF8),
        )
        com.example.data.model.ChatThemePreset.TURBO_FIRE -> ChatThemePalette(
            headerContainer = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            tickerContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.80f),
            tickerContent = MaterialTheme.colorScheme.onPrimaryContainer,
            outgoingBubble = storeColors.outgoingBubble,
            incomingBubble = storeColors.incomingBubble,
            accent = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun ChatThemeSelectorStrip(
    selectedPreset: com.example.data.model.ChatThemePreset,
    onSelectPreset: (com.example.data.model.ChatThemePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "تيم المحادثة:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        com.example.data.model.ChatThemePreset.entries.forEach { preset ->
            val selected = preset == selectedPreset
            FilterChip(
                selected = selected,
                onClick = { onSelectPreset(preset) },
                label = {
                    Text(
                        text = preset.labelAr,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier
                    .height(28.dp)
                    .testTag("chat_theme_chip_${preset.key}"),
            )
        }
    }
}

@Composable
fun AnimatedSocialTickerBar(
    settings: StoreSettings,
    onOpenVideoCatalog: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        color = containerColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val url = "https://instagram.com/${settings.instagramHandle.removePrefix("@")}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = "إنستقرام",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "انستقرام",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF141414),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val url = "https://tiktok.com/@${settings.tiktokHandle.removePrefix("@")}"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "تيك توك",
                        tint = Color(0xFF25F4EE),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تيك توك",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "تابع مقاطع العوادم ودبات التلوث على إنستقرام ${settings.instagramHandle} وتيك توك ${settings.tiktokHandle} • شحن سريع لجميع المناطق • ",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE),
            )

            Spacer(modifier = Modifier.width(6.dp))

            AssistChip(
                onClick = onOpenVideoCatalog,
                label = {
                    Text(
                        text = "المقاطع",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "فيديوهات الكتالوج",
                        modifier = Modifier.size(15.dp),
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ),
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
fun AnimatedChatWallpaperBackground(
    selectedIndex: Int,
    autoCycle: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var activeWallpaper by remember(selectedIndex) { mutableIntStateOf(selectedIndex.coerceIn(0, 3)) }

    LaunchedEffect(autoCycle, selectedIndex) {
        if (autoCycle) {
            while (true) {
                delay(7000L)
                activeWallpaper = (activeWallpaper + 1) % 4
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "wallpaperGlow")
    val glowShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowShift",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = activeWallpaper,
            animationSpec = tween(durationMillis = 1200),
            label = "wallpaperCrossfade",
        ) { idx ->
            when (idx) {
                0 -> Image(
                    painter = painterResource(id = R.drawable.img_chat_wallpaper_turbo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.32f),
                )
                1 -> Image(
                    painter = painterResource(id = R.drawable.img_exhaust_sport),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.24f),
                )
                2 -> Image(
                    painter = painterResource(id = R.drawable.img_store_banner),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.25f),
                )
                else -> Image(
                    painter = painterResource(id = R.drawable.img_catalytic_converter),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.25f),
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bgColor.copy(alpha = 0.80f),
                        bgColor.copy(alpha = 0.65f),
                        bgColor.copy(alpha = 0.88f),
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * (0.2f + 0.6f * glowShift), size.height * 0.35f),
                    radius = size.minDimension * 0.65f,
                ),
                center = Offset(size.width * (0.2f + 0.6f * glowShift), size.height * 0.35f),
                radius = size.minDimension * 0.65f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryColor.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * (0.8f - 0.5f * glowShift), size.height * 0.72f),
                    radius = size.minDimension * 0.55f,
                ),
                center = Offset(size.width * (0.8f - 0.5f * glowShift), size.height * 0.72f),
                radius = size.minDimension * 0.55f,
            )
        }
    }
}

@Composable
fun EngineAlertSoundStrip(
    selectedSound: EngineAlertSound,
    playingSound: EngineAlertSound?,
    onSelectAndSendSound: (EngineAlertSound) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.NotificationsActive,
            contentDescription = "تنبيه صوتي للمحادثة",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "صوت التنبيه:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EngineAlertSound.entries.forEach { sound ->
            val isSelected = sound == selectedSound
            val isPlaying = sound == playingSound
            FilterChip(
                selected = isSelected,
                onClick = { onSelectAndSendSound(sound) },
                label = {
                    Text(
                        text = if (isPlaying) "يعمل: ${sound.labelAr}" else sound.labelAr,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.GraphicEq else Icons.Filled.VolumeUp,
                        contentDescription = sound.labelAr,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier.testTag("sound_chip_${sound.key}"),
            )
        }
    }
}

@Composable
fun ShippingWaybillCard(
    chat: ChatConversation,
    isOwnerMode: Boolean,
    onEditWaybillClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusLabel = when (chat.waybillStatus) {
        "delivered" -> "تم التوصيل"
        "shipped" -> "جاري الشحن"
        else -> "قيد التجهيز"
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = "بوليصة الشحن",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بوليصة الشحن • ${chat.waybillNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGroteskFontFamily,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEditWaybillClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_waybill_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "تعديل بوليصة الشحن",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val customerSummary = if (chat.shippingCustomerName.isBlank() && chat.shippingAddress.isBlank()) {
                "اضغط تعديل لإدخال اسم العميل والعنوان والمنطقة"
            } else {
                "العميل: ${chat.shippingCustomerName} | المنطقة: ${chat.shippingRegion}\nالعنوان: ${chat.shippingAddress}"
            }
            Text(
                text = customerSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "شركة الشحن: ${chat.shippingCompany.ifBlank { "يحددها المالك" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "الوصول خلال: ${chat.shippingDaysRemaining} أيام",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = if (isOwnerMode) "وضع المالك: يمكنك تعديل شركة الشحن والأيام المتبقية" else "وضع العميل: يمكنك إدخال الاسم والعنوان والمنطقة",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaybillEditorDialog(
    chat: ChatConversation,
    isOwnerMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        customerName: String,
        address: String,
        region: String,
        shippingCompany: String,
        daysRemaining: Long,
        waybillNumber: String,
        status: String,
    ) -> Unit,
) {
    var customerName by remember { mutableStateOf(chat.shippingCustomerName.ifBlank { chat.customerName }) }
    var address by remember { mutableStateOf(chat.shippingAddress) }
    var region by remember { mutableStateOf(chat.shippingRegion) }
    var shippingCompany by remember { mutableStateOf(chat.shippingCompany.ifBlank { "أرامكس (Aramex)" }) }
    var daysRemainingText by remember { mutableStateOf(chat.shippingDaysRemaining.toString()) }
    var waybillNumber by remember { mutableStateOf(chat.waybillNumber.ifBlank { "WB-${(10000..99999).random()}" }) }
    var status by remember { mutableStateOf(chat.waybillStatus) }
    var statusExpanded by remember { mutableStateOf(false) }

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
                    text = "بوليصة الشحن التفاعلية",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("اسم العميل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = region,
                    onValueChange = { region = it },
                    label = { Text("المنطقة / المدينة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان التفصيلي") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = shippingCompany,
                    onValueChange = { shippingCompany = it },
                    label = { Text("شركة الشحن (يضعها المالك)") },
                    enabled = isOwnerMode,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = daysRemainingText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() }) daysRemainingText = input
                        },
                        label = { Text("الأيام المتبقية") },
                        enabled = isOwnerMode,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = waybillNumber,
                        onValueChange = { waybillNumber = it },
                        label = { Text("رقم البوليصة") },
                        enabled = isOwnerMode,
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (isOwnerMode) {
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = !statusExpanded },
                    ) {
                        val displayStatus = when (status) {
                            "shipped" -> "جاري الشحن"
                            "delivered" -> "تم التوصيل"
                            else -> "قيد التجهيز"
                        }
                        OutlinedTextField(
                            value = displayStatus,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("حالة الشحنة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                        ) {
                            listOf("pending" to "قيد التجهيز", "shipped" to "جاري الشحن", "delivered" to "تم التوصيل")
                                .forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            status = key
                                            statusExpanded = false
                                        },
                                    )
                                }
                        }
                    }
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
                            onSave(
                                customerName,
                                address,
                                region,
                                shippingCompany,
                                (daysRemainingText.toLongOrNull() ?: 3L).coerceIn(0L, 90L),
                                waybillNumber,
                                status,
                            )
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_waybill_button"),
                    ) {
                        Text("حفظ بالمحادثة")
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceCallOverlayDialog(
    peerName: String,
    storeName: String,
    onEndCall: (durationSeconds: Int) -> Unit,
) {
    var seconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeaker by remember { mutableStateOf(false) }
    val storeColors = LocalStoreColors.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            seconds++
        }
    }

    Dialog(onDismissRequest = { onEndCall(seconds) }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(84.dp)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "اتصال صوتي",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = peerName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Verified,
                        contentDescription = null,
                        tint = storeColors.verifiedBadge,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = "اتصال صوتي عبر $storeName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = String.format("%02d:%02d", seconds / 60, seconds % 60),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = SpaceGroteskFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalIconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier.size(54.dp),
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = if (isMuted) "إلغاء الكتم" else "كتم الميكروفون",
                        )
                    }

                    Button(
                        onClick = { onEndCall(seconds) },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = storeColors.blockedBanner,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("end_voice_call_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "إنهاء المكالمة",
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { isSpeaker = !isSpeaker },
                        modifier = Modifier.size(54.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "مكبر الصوت",
                            tint = if (isSpeaker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubscriptionAndBrandingCard(
    settings: StoreSettings,
    onActivateSubscription: (
        plan: com.example.data.model.SubscriptionPlan,
        phoneNumber: String,
        customAppName: String,
        websiteUrl: String,
    ) -> Unit,
    onVerifySmsCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val storeColors = LocalStoreColors.current

    var selectedPlan by remember(settings.subscriptionPlan) {
        mutableStateOf(com.example.data.model.SubscriptionPlan.fromKey(settings.subscriptionPlan))
    }
    var customAppName by remember(settings.customAppNameUnderBadge) {
        mutableStateOf(settings.customAppNameUnderBadge)
    }
    var websiteUrl by remember(settings.websiteUrl) {
        mutableStateOf(settings.websiteUrl)
    }
    var phoneForSms by remember(settings.customerPhoneForSms) {
        mutableStateOf(settings.customerPhoneForSms.ifBlank { "+9665" })
    }
    var enteredSmsCode by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "باقات الاشتراك (25 ر.س/شهر + 5 أيام مجاناً)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (settings.subscriptionActive) {
                        storeColors.verifiedBadge.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                ) {
                    Text(
                        text = if (settings.subscriptionActive) {
                            "فعّال • ${settings.subscriptionDaysLeft} يوم"
                        } else {
                            "بانتظار كود SMS"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (settings.subscriptionActive) storeColors.verifiedBadge else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = "احصل على تجربة مجانية 5 أيام، أو اشترك بـ 25 ريال للشهر لتخصيص اسم التطبيق تحت العلامة الزرقاء ووضع رابط موقعك:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                com.example.data.model.SubscriptionPlan.entries.forEach { plan ->
                    val isSelected = plan == selectedPlan
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPlan = plan },
                        label = {
                            Text(
                                text = plan.labelAr,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        modifier = Modifier.testTag("subscription_plan_${plan.key}"),
                    )
                }
            }

            OutlinedTextField(
                value = customAppName,
                onValueChange = { customAppName = it },
                label = { Text("اسم التطبيق تحت العلامة الزرقاء") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_app_name_input"),
            )

            OutlinedTextField(
                value = websiteUrl,
                onValueChange = { websiteUrl = it },
                label = { Text("رابط الموقع الإلكتروني") },
                leadingIcon = {
                    Icon(Icons.Filled.Language, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("website_url_input"),
            )

            OutlinedTextField(
                value = phoneForSms,
                onValueChange = { phoneForSms = it },
                label = { Text("رقم الجوال لإرسال كود التفعيل (SMS)") },
                leadingIcon = {
                    Icon(Icons.Filled.Sms, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sms_phone_input"),
            )

            Button(
                onClick = {
                    onActivateSubscription(selectedPlan, phoneForSms, customAppName, websiteUrl)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activate_subscription_button"),
            ) {
                Icon(Icons.Filled.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (selectedPlan == com.example.data.model.SubscriptionPlan.FREE_TRIAL) {
                        "تفعيل 5 أيام مجاناً + إرسال كود SMS"
                    } else {
                        "دفع ${selectedPlan.priceSar} ر.س وإرسال كود SMS"
                    }
                )
            }

            if (settings.subscriptionSmsCode.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "تم توليد كود التفعيل النصي (SMS): ${settings.subscriptionSmsCode}",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        OutlinedButton(
                            onClick = {
                                val smsText = "كود تفعيل اشتراك ${settings.storeName} (${selectedPlan.labelAr}): ${settings.subscriptionSmsCode}"
                                val smsUri = Uri.parse("smsto:${Uri.encode(phoneForSms)}")
                                val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                    putExtra("sms_body", smsText)
                                }
                                runCatching { context.startActivity(smsIntent) }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فتح تطبيق الرسائل لإرسال الكود SMS")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = enteredSmsCode,
                                onValueChange = { enteredSmsCode = it },
                                label = { Text("أدخل كود التفعيل") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("verify_sms_code_input"),
                            )
                            Button(
                                onClick = { onVerifySmsCode(enteredSmsCode) },
                                enabled = enteredSmsCode.isNotBlank(),
                                modifier = Modifier.testTag("verify_sms_code_button"),
                            ) {
                                Text("تفعيل")
                            }
                        }
                    }
                }
            }
        }
    }
}
