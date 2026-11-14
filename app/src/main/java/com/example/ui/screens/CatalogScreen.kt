package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.CatalogProduct
import com.example.data.model.EngineAlertSound
import com.example.data.model.StoreSettings
import com.example.ui.components.BlueBadgeVerificationDialog
import com.example.ui.components.VerifiedStoreTitleBlock
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.viewmodel.DataUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CatalogScreen(
    settings: StoreSettings,
    productsState: DataUiState<List<CatalogProduct>>,
    isOwnerMode: Boolean,
    initialCategoryFilter: String = "الكل",
    seoGenerating: Boolean,
    onCreateProduct: (CatalogProduct) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onUpdateKeywords: (String, List<String>) -> Unit,
    onGenerateAiSeo: (String, String, String, (List<String>, String) -> Unit) -> Unit,
    onSendProductToActiveChat: (CatalogProduct) -> Unit,
    onPlayVideoSound: (EngineAlertSound) -> Unit,
    onOpenSubscriptionSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categories = listOf("الكل", "دبات التلوث", "أنظمة العادم", "تيربو وداون بايب", "مقاطع الفيديو")
    var selectedCategory by remember(initialCategoryFilter) { mutableStateOf(initialCategoryFilter) }
    var keywordQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showBadgeDialog by remember { mutableStateOf(false) }
    var activeVideoProduct by remember { mutableStateOf<CatalogProduct?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (isOwnerMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_product_fab"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة منتج للكتالوج")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    VerifiedStoreTitleBlock(
                        storeName = "كتالوج ${settings.storeName}",
                        tagline = "عرض المنتجات على جوجل • الكلمات المفتاحية • مقاطع الفيديو",
                        verified = settings.verifiedBadge,
                        customAppNameUnderBadge = settings.customAppNameUnderBadge,
                        websiteUrl = settings.websiteUrl,
                        onBadgeClick = { showBadgeDialog = true },
                    )

                    OutlinedTextField(
                        value = keywordQuery,
                        onValueChange = { keywordQuery = it },
                        placeholder = { Text("ابحث بكلمات جوجل المفتاحية (مثل: دبة تلوث، تيربو، كتباك)...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalog_seo_search_input"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                leadingIcon = if (cat == "مقاطع الفيديو") {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                } else null,
                                modifier = Modifier.testTag("category_chip_$cat"),
                            )
                        }
                    }
                }
            }

            when (productsState) {
                is DataUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("جاري تحميل الكتالوج...")
                    }
                }
                is DataUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(productsState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is DataUiState.Success -> {
                    val filtered = remember(productsState.data, selectedCategory, keywordQuery) {
                        productsState.data.filter { prod ->
                            val matchesCat = selectedCategory == "الكل" ||
                                prod.category == selectedCategory ||
                                (selectedCategory == "مقاطع الفيديو" && prod.videoTitle.isNotBlank())
                            val q = keywordQuery.trim()
                            val matchesSearch = q.isEmpty() ||
                                prod.name.contains(q, ignoreCase = true) ||
                                prod.description.contains(q, ignoreCase = true) ||
                                prod.seoKeywords.any { it.contains(q, ignoreCase = true) }
                            matchesCat && matchesSearch
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            ProductCatalogCard(
                                product = product,
                                storeName = settings.storeName,
                                isOwnerMode = isOwnerMode,
                                onKeywordTap = { kw -> keywordQuery = kw },
                                onPlayVideo = { activeVideoProduct = product },
                                onSendToChat = { onSendProductToActiveChat(product) },
                                onDelete = { onDeleteProduct(product.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            seoGenerating = seoGenerating,
            onGenerateAiSeo = onGenerateAiSeo,
            onDismiss = { showAddDialog = false },
            onSave = { product ->
                onCreateProduct(product)
                showAddDialog = false
            },
        )
    }

    activeVideoProduct?.let { videoProduct ->
        VideoPreviewDialog(
            product = videoProduct,
            onPlayAudio = {
                val sound = when {
                    videoProduct.name.contains("تيربو") || videoProduct.category.contains("تيربو") -> EngineAlertSound.TURBO
                    videoProduct.category.contains("العادم") -> EngineAlertSound.V8
                    else -> EngineAlertSound.SPORTBIKE
                }
                onPlayVideoSound(sound)
            },
            onSendToChat = {
                onSendProductToActiveChat(videoProduct)
                activeVideoProduct = null
            },
            onDismiss = { activeVideoProduct = null },
        )
    }

    if (showBadgeDialog) {
        BlueBadgeVerificationDialog(
            settings = settings,
            onOpenSubscriptionSettings = onOpenSubscriptionSettings,
            onDismiss = { showBadgeDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductCatalogCard(
    product: CatalogProduct,
    storeName: String,
    isOwnerMode: Boolean,
    onKeywordTap: (String) -> Unit,
    onPlayVideo: () -> Unit,
    onSendToChat: () -> Unit,
    onDelete: () -> Unit,
) {
    val imageRes = when (product.imageKey) {
        "catalytic_converter" -> R.drawable.img_catalytic_converter
        "store_banner" -> R.drawable.img_store_banner
        else -> R.drawable.img_exhaust_sport
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                ) {
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                if (product.videoTitle.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onPlayVideo),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = "تشغيل مقطع الفيديو",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "فيديو (${product.videoDurationSec}ث)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${product.priceSar.toInt()} ر.س",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = SpaceGroteskFontFamily,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Google Search SERP Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = "معاينة بحث جوجل",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Google • turboexhaust.app/p/${product.id.take(8)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = SpaceGroteskFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${product.name} | $storeName",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (product.seoKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                product.seoKeywords.forEach { kw ->
                                    AssistChip(
                                        onClick = { onKeywordTap(kw) },
                                        label = { Text(kw, style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Tag,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                            )
                                        },
                                        modifier = Modifier.height(26.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onSendToChat,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة بالمحادثة")
                    }

                    if (isOwnerMode) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "حذف المنتج",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewDialog(
    product: CatalogProduct,
    onPlayAudio: () -> Unit,
    onSendToChat: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "videoPulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = product.videoTitle.ifBlank { product.name },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_exhaust_sport),
                        contentDescription = product.videoTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                            modifier = Modifier
                                .size(64.dp)
                                .clickable(onClick = onPlayAudio),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "تشغيل صوت الفيديو",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "اضغط على أيقونة الصوت لسماع نغمة العادم والتيربو الخاصة بهذا المنتج.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("إغلاق") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onSendToChat) { Text("إرسال بالمحادثة") }
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    seoGenerating: Boolean,
    onGenerateAiSeo: (String, String, String, (List<String>, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CatalogProduct) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("دبات التلوث") }
    var priceText by remember { mutableStateOf("750") }
    var description by remember { mutableStateOf("") }
    var keywordsText by remember { mutableStateOf("دبة تلوث, كتاليزر, عوادم") }
    var videoTitle by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "إضافة منتج + كلمات بحث جوجل",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المنتج") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("الفئة") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) priceText = it },
                        label = { Text("السعر (ر.س)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f),
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = videoTitle,
                    onValueChange = { videoTitle = it },
                    label = { Text("عنوان مقطع الفيديو (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text("كلمات بحث جوجل (مفصولة بفاصلة)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = {
                        onGenerateAiSeo(name, category, description) { keywords, snippet ->
                            keywordsText = keywords.joinToString(", ")
                            if (description.isBlank() && snippet.isNotBlank()) {
                                description = snippet
                            }
                        }
                    },
                    enabled = name.isNotBlank() && !seoGenerating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (seoGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("جاري التوليد بالذكاء الاصطناعي...")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("توليد كلمات جوجل بالذكاء الاصطناعي")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val imageKey = if (category.contains("تلوث")) "catalytic_converter" else "exhaust_sport"
                            onSave(
                                CatalogProduct(
                                    name = name,
                                    category = category,
                                    priceSar = priceText.toDoubleOrNull() ?: 0.0,
                                    description = description.ifBlank { "منتج أصلي عالي الجودة من المتجر" },
                                    imageKey = imageKey,
                                    videoTitle = videoTitle,
                                    videoDurationSec = if (videoTitle.isNotBlank()) 45L else 0L,
                                    seoKeywords = keywordsText.split(",", "،").map { it.trim() }.filter { it.isNotEmpty() },
                                    inStock = true,
                                )
                            )
                        },
                        enabled = name.isNotBlank(),
                    ) {
                        Text("حفظ بالكتالوج")
                    }
                }
            }
        }
    }
}
