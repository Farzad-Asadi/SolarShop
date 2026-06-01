package com.example.solarShop.feature.product.ui.product

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
                            onClick = {
                                onEditPurchasePrice(productId)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = "قیمت خرید"
                            )
                        }
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
                    //اطلاعات کالا
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = product.product.name,
                                    style = MaterialTheme.typography.headlineSmall
                                )

                                if (product.product.model.isNotBlank()) {
                                    Text(
                                        text = "مدل: ${product.product.model}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = listOfNotNull(
                                        product.category?.name,
                                        product.brand?.name
                                    ).joinToString(" / "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    //تصویر اصلی
                    item {
                        if (coverImage != null) {
                            AsyncImage(
                                model = coverImage.uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }

                    //تصاویر کالا
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "تصاویر کالا",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (uiState.images.isEmpty()) {
                                    Text(
                                        text = "هنوز تصویری برای این کالا ثبت نشده است.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    items(uiState.images) { image ->
                                        AsyncImage(
                                            model = image.uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(88.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .combinedClickable(
                                                    onClick = {
                                                        // بعداً نمایش تمام‌صفحه
                                                    },
                                                    onLongClick = {
                                                        selectedImageId = image.id
                                                        showImageOptionsSheet = true
                                                    }
                                                )
                                        )
                                    }

                                    item {

                                        Box(
                                            modifier = Modifier
                                                .size(88.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    showPickSheet = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {

                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // کارت قیمت
                    item {

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Text(
                                    text = "آخرین قیمت خرید",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val price = uiState.activePurchasePrice

                                if (price == null) {

                                    Text("هنوز قیمتی ثبت نشده است")

                                } else {

                                    price.buyPriceDollar?.let {
                                        Text("$it $")
                                    }

                                    price.buyPriceToman?.let {
                                        Text(
                                            "${String.format("%,d", it)} تومان"
                                        )
                                    }

                                    val salePrice = uiState.salePriceResult

                                    if (salePrice != null) {
                                        HorizontalDivider()

                                        Text(
                                            text = "قیمت فروش پیشنهادی",
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Text(
                                            text = "${String.format("%,d", salePrice.finalSalePriceToman)} تومان",
                                            style = MaterialTheme.typography.headlineSmall
                                        )

                                        Text(
                                            text = "سود: ${String.format("%,d", salePrice.profitAmountToman)} تومان",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    price.dollarRateToman?.let {
                                        Text(
                                            "نرخ دلار: ${
                                                String.format("%,d", it)
                                            }"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    //موجودی
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "موجودی فعلی",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "${uiState.currentStock} عدد",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                product.product.id?.let { productId ->
                                    Button(
                                        onClick = { onAddInventoryTransaction(productId) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("ثبت ورود / خروج موجودی")
                                    }
                                }


                            }
                        }
                    }

                    //تاریخچه موجودی
                    if (uiState.inventoryTransactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "تاریخچه موجودی",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        items(uiState.inventoryTransactions.take(5)) { tx ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    val signedQty = tx.transactionType.signedQuantity(tx.quantity)

                                    Text(
                                        text = "${tx.transactionType.labelFa()}  |  $signedQty",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatPersianDateTime(tx.createdAt),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (tx.note.isNotBlank()) {
                                        Text(tx.note)
                                    }
                                }
                            }
                        }
                    }

                    //مشخصات فنی
                    item {
                        Text(
                            text = "مشخصات فنی",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (uiState.attributes.isEmpty()) {
                        item {
                            Text("مشخصاتی برای این کالا ثبت نشده است.")
                        }
                    } else {
                        items(uiState.attributes) { attr ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = attr.title,
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = buildString {
                                            append(attr.valueText.orEmpty().ifBlank { "-" })
                                            if (!attr.unit.isNullOrBlank()) {
                                                append(" ${attr.unit}")
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