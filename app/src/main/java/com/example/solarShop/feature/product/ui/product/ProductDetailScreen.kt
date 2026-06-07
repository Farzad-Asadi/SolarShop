package com.example.solarShop.feature.product.ui.product

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.solarShop.domain.inventory.labelFa
import com.example.solarShop.domain.inventory.signedQuantity
import com.example.solarShop.feature.product.viewmodel.product.ProductDetailViewModel
import com.example.solarShop.feature.product.viewmodel.product.ProductImageUi
import com.example.solarShop.utils.FullscreenImageViewer
import com.example.solarShop.utils.currency.toPriceString
import com.example.solarShop.utils.formatPersianDateTime
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onEditProduct: (Int) -> Unit,
    onEditPurchasePrice: (Int) -> Unit,
    onAddInventoryTransaction: (Int) -> Unit,
    onAddImageClick: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.product
    val coverImage = uiState.images.firstOrNull()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importProductImage(uri)
        }
    }

    var showPickSheet by remember { mutableStateOf(false) }
    var pendingTempFile by remember { mutableStateOf<File?>(null) }
    var selectedImageId by remember { mutableStateOf<Int?>(null) }
    var showImageOptionsSheet by remember { mutableStateOf(false) }

    var nameExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var priceExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var inventoryExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var attributesExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedImageIndex by remember {
        mutableIntStateOf(0)
    }

    var fullscreenImageStartIndex by remember {
        mutableStateOf<Int?>(null)
    }

    //لانچر دوربین
    val cameraController = rememberCameraCaptureLauncher(
        requiredPermissions = {
            emptyArray()
        },
        createOutputUri = {
            val (tmpFile, tmpUri) = viewModel.createProductCameraTempUri()
            pendingTempFile = tmpFile
            tmpUri
        },
        onResult = { _, success ->
            val tmp = pendingTempFile

            if (success && tmp != null) {
                viewModel.importProductImageFromCameraTemp(tmp)
            } else {
                tmp?.let { runCatching { it.delete() } }
            }

            pendingTempFile = null
        },
        onMessage = {
            // فعلاً خالی یا Log
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات کالا") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    product?.product?.id?.let { productId ->

                        IconButton(
                            onClick = { onEditProduct(productId) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش کالا"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            product == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("کالا پیدا نشد")
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    //تصویر اصلی
                    item {
                        ProductDetailImageCard(
                            images = uiState.images,
                            selectedIndex = selectedImageIndex,
                            onSelectedIndexChange = {
                                selectedImageIndex = it
                            },
                            onOpenFullscreen = {
                                fullscreenImageStartIndex = selectedImageIndex
                            }
                        )
                    }

                    //اطلاعات کالا
                    item {
                        DetailExpandableCard(
                            title = "نام کالا",
                            summary = product?.product?.name.orEmpty(),
                            expanded = nameExpanded,
                            onExpandedChange = {
                                nameExpanded = it
                            }
                        ) {

                            Text(
                                text = "مدل: ${product?.product?.model.orEmpty()}"
                            )

                            Text(
                                text = "برند: ${product?.brand?.name ?: "بدون برند"}"
                            )
                        }
                    }

                    // کارت قیمت
                    item {
                        val latestConsumerSale =
                            uiState.salePrices.firstOrNull {
                                it.priceType == "consumer"
                            }

                        val latestColleagueSale =
                            uiState.salePrices.firstOrNull {
                                it.priceType == "colleague"
                            }

                        val liveConsumerPrice =
                            calculateLiveSalePrice(
                                baseDollarPrice = latestConsumerSale?.baseDollarPrice,
                                dailyDollarRateToman = uiState.dailyDollarRateToman,
                                profitPercent = latestConsumerSale?.profitPercent
                            )

                        val liveColleaguePrice =
                            calculateLiveSalePrice(
                                baseDollarPrice = latestColleagueSale?.baseDollarPrice,
                                dailyDollarRateToman = uiState.dailyDollarRateToman,
                                profitPercent = latestColleagueSale?.profitPercent
                            )

                        val priceSummary =
                            liveConsumerPrice?.let {
                                "${it.toPriceString()} تومان"
                            } ?: "قیمت فروش ثبت نشده"

                        DetailExpandableCard(
                            title = "قیمت‌ها",
                            summary = priceSummary,
                            expanded = priceExpanded,
                            onExpandedChange = {
                                priceExpanded = it
                            }
                        ) {
                            Text(
                                text = "نرخ دلار روز: ${
                                    uiState.dailyDollarRateToman?.toPriceString() ?: "-"
                                } تومان",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            HorizontalDivider()

                            Text(
                                text = "قیمت فروش روز",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                text = "مصرف‌کننده: ${
                                    liveConsumerPrice?.toPriceString() ?: "-"
                                } تومان"
                            )

                            Text(
                                text = "همکار: ${
                                    liveColleaguePrice?.toPriceString() ?: "-"
                                } تومان"
                            )

                            HorizontalDivider()

                            Text(
                                text = "آخرین رکورد خرید",
                                style = MaterialTheme.typography.titleSmall
                            )

                            val activePurchase =
                                uiState.activePurchasePrice

                            if (activePurchase == null) {
                                Text("قیمت خریدی ثبت نشده است.")
                            } else {
                                Text(
                                    text = "خرید: ${activePurchase.buyPriceToman.toPriceString()} تومان"
                                )

                                Text(
                                    text = "قیمت دلاری خرید: ${activePurchase.buyPriceDollar ?: "-"}"
                                )

                                Text(
                                    text = "دلار زمان خرید: ${
                                        activePurchase.dollarRateToman?.toPriceString() ?: "-"
                                    }"
                                )

                                Text(
                                    text = "تاریخ خرید: ${
                                        formatPersianDateTime(activePurchase.purchasedAt, true)
                                    }"
                                )
                            }

                            HorizontalDivider()

                            Text(
                                text = "آخرین رکورد فروش",
                                style = MaterialTheme.typography.titleSmall
                            )

                            latestConsumerSale?.let { sale ->
                                Text(
                                    text = "مصرف‌کننده: سود ${sale.profitPercent ?: "-"}٪، مبنا ${sale.baseDollarPrice ?: "-"} دلار"
                                )
                            }

                            latestColleagueSale?.let { sale ->
                                Text(
                                    text = "همکار: سود ${sale.profitPercent ?: "-"}٪، مبنا ${sale.baseDollarPrice ?: "-"} دلار"
                                )
                            }

                            if (latestConsumerSale == null && latestColleagueSale == null) {
                                Text("رکورد فروش ثبت نشده است.")
                            }
                        }
                    }

                    //موجودی
                    item {

                        val stockSummary =
                            "${uiState.currentStock} عدد"

                        DetailExpandableCard(
                            title = "موجودی",
                            summary = stockSummary,
                            expanded = inventoryExpanded,
                            onExpandedChange = {
                                inventoryExpanded = it
                            }
                        ) {

                            Text(
                                text = "موجودی فعلی",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(
                                text = stockSummary,
                                style = MaterialTheme.typography.headlineSmall
                            )


                            HorizontalDivider()

                            Text(
                                text = "تاریخچه موجودی",
                                style = MaterialTheme.typography.titleSmall
                            )

                            if (uiState.inventoryTransactions.isEmpty()) {

                                Text(
                                    text = "تراکنشی ثبت نشده است."
                                )

                            } else {

                                uiState.inventoryTransactions.forEach { tx ->

                                    val signedQty =
                                        tx.transactionType
                                            .signedQuantity(tx.quantity)

                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.elevatedCardColors(
                                            containerColor =
                                            MaterialTheme.colorScheme.surface
                                        )
                                    ) {

                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {

                                            Text(
                                                text = tx.transactionType.labelFa(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Text(
                                                text = signedQty.toString(),
                                                style = MaterialTheme.typography.titleMedium
                                            )

                                            Text(
                                                text = formatPersianDateTime(
                                                    tx.createdAt,
                                                    true
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color =
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (tx.note.isNotBlank()) {
                                                Text(
                                                    text = tx.note,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //مشخصات فنی
                    if (uiState.attributes.isEmpty()) {
                        item {
                            Text("مشخصاتی برای این کالا ثبت نشده است.")
                        }
                    } else {
                        item {
                            DetailExpandableCard(
                                title = "مشخصات",
                                summary = if (uiState.attributes.isEmpty()) {
                                    "مشخصاتی ثبت نشده"
                                } else {
                                    "${uiState.attributes.size} مشخصه"
                                },
                                expanded = attributesExpanded,
                                onExpandedChange = {
                                    attributesExpanded = it
                                }
                            ) {
                                if (uiState.attributes.isEmpty()) {
                                    Text("مشخصاتی برای این کالا ثبت نشده است.")
                                } else {
                                    uiState.attributes.forEach { attr ->
                                        ElevatedCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.elevatedCardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = attr.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Text(
                                                    text = buildString {
                                                        append(
                                                            attr.valueText
                                                                .orEmpty()
                                                                .ifBlank { "-" }
                                                        )

                                                        if (!attr.unit.isNullOrBlank()) {
                                                            append(" ")
                                                            append(attr.unit)
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }
    }
    fullscreenImageStartIndex?.let { startIndex ->
        FullscreenImageViewer(
            title = product?.product?.name.orEmpty(),
            imageUris = uiState.images.map { it.uri },
            initialPage = startIndex,
            onClose = {
                fullscreenImageStartIndex = null
            }
        )
    }
    if (showPickSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("افزودن تصویر", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = {
                        showPickSheet = false
                        pickImage.launch("image/*")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("از گالری")
                }

                Button(
                    onClick = {
                        showPickSheet = false
                        cameraController.launch()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("از دوربین")
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
    if (showImageOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImageOptionsSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("گزینه‌های تصویر", style = MaterialTheme.typography.titleMedium)

                Button(
                    onClick = {
                        selectedImageId?.let { imageId ->
                            viewModel.setImageAsCover(imageId)
                        }

                        selectedImageId = null
                        showImageOptionsSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("انتخاب به عنوان عکس اصلی")
                }

                Button(
                    onClick = {
                        selectedImageId?.let { imageId ->
                            viewModel.deleteProductImage(imageId)
                        }

                        selectedImageId = null
                        showImageOptionsSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حذف تصویر")
                }
            }
        }
    }
}

@Composable
private fun ProductDetailImageCard(
    images: List<ProductImageUi>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onOpenFullscreen: () -> Unit
) {
    val safeIndex =
        selectedIndex.coerceIn(
            0,
            (images.size - 1).coerceAtLeast(0)
        )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    enabled = images.isNotEmpty()
                ) {
                    onOpenFullscreen()
                },
            contentAlignment = Alignment.Center
        ) {
            if (images.isEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            } else {
                AsyncImage(
                    model = images[safeIndex].uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                if (images.size > 1) {
                    IconButton(
                        onClick = {
                            val next =
                                if (safeIndex == 0) images.lastIndex else safeIndex - 1

                            onSelectedIndexChange(next)
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "عکس قبلی"
                        )
                    }

                    IconButton(
                        onClick = {
                            val next =
                                if (safeIndex == images.lastIndex) 0 else safeIndex + 1

                            onSelectedIndexChange(next)
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "عکس بعدی"
                        )
                    }

                    Text(
                        text = "${safeIndex + 1}/${images.size}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailExpandableCard(
    title: String,
    summary: String? = null,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExpandedChange(!expanded)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    if (!summary.isNullOrBlank()) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

private fun calculateLiveSalePrice(
    baseDollarPrice: Double?,
    dailyDollarRateToman: Long?,
    profitPercent: Double?
): Long? {
    if (
        baseDollarPrice == null ||
        dailyDollarRateToman == null ||
        profitPercent == null
    ) return null

    val basePrice =
        baseDollarPrice * dailyDollarRateToman

    val profitAmount =
        basePrice * profitPercent / 100.0

    return (basePrice + profitAmount).toLong()
}