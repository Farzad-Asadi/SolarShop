package com.example.solarShop.feature.product.ui.product

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import com.example.solarShop.data.local.entity.pricing.ProductPurchasePriceEntity
import com.example.solarShop.data.local.entity.pricing.ProductSalePriceEntity
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.feature.product.model.ProductEditImageItem
import com.example.solarShop.feature.product.viewmodel.product.ProductEditViewModel
import com.example.solarShop.utils.DateUi
import com.example.solarShop.utils.MyCurrencyField
import com.example.solarShop.utils.PersianDateUiAdapter
import com.example.solarShop.utils.currency.toPriceString
import com.example.solarShop.utils.formatPersianDateTime
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onClose: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()




    val context = LocalContext.current




    var draggingImages by remember { mutableStateOf(false) }
    var showPickSheet by remember { mutableStateOf(false) }
    var pendingTempFile by remember { mutableStateOf<File?>(null) }
    var imageToDelete by remember {
        mutableStateOf<ProductEditImageItem?>(null)
    }
    var showDeleteDialog by remember {
        mutableStateOf(false)
    }
    var brandMenuExpanded by remember { mutableStateOf(false) }
    var showAddBrandDialog by remember { mutableStateOf(false) }
    var newBrandName by remember { mutableStateOf("") }
    var purchaseToDelete by remember {
        mutableStateOf<ProductPurchasePriceEntity?>(null)
    }
    var saleGroupToDelete by remember {
        mutableStateOf<List<ProductSalePriceEntity>>(emptyList())
    }
    var showDeleteSaleDialog by remember {
        mutableStateOf(false)
    }
    var showDeletePurchaseDialog by remember {
        mutableStateOf(false)
    }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importProductImageFromGallery(uri)
        }
    }
    val cameraController = rememberCameraCaptureLauncher(
        requiredPermissions = { emptyArray() },
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
        onMessage = {}
    )
    val sourceImages = remember(
        uiState.images,
        uiState.pendingImageFiles
    ) {
        buildList {
            uiState.images.forEach { image ->
                add(
                    ProductEditImageItem(
                        id = image.id,
                        fileName = image.fileName,
                        isPending = false
                    )
                )
            }

            uiState.pendingImageFiles.forEach { file ->
                add(
                    ProductEditImageItem(
                        id = null,
                        fileName = file.name,
                        isPending = true
                    )
                )
            }
        }
    }
    var imageItems by remember { mutableStateOf(sourceImages) }
    var inventoryToDelete by remember {
        mutableStateOf<InventoryTransactionEntity?>(null)
    }
    var showDeleteInventoryDialog by remember {
        mutableStateOf(false)
    }





    LaunchedEffect(sourceImages) {
        if (!draggingImages) {
            imageItems = sourceImages
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (viewModel.startedAsNewProduct) {
                            "کالای جدید"
                        } else {
                            "ویرایش کالا"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //کارت تصویر
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تصاویر کالا",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "${imageItems.size} تصویر",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val coverImage = imageItems.firstOrNull()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clickable {
                                    if (uiState.productId == null) {
                                        viewModel.save(
                                            onSuccess = {
                                                showPickSheet = true
                                            }
                                        )
                                    } else {
                                        showPickSheet = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverImage == null) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp)
                                )
                            } else {
                                val file = File(File(context.filesDir, "images"), coverImage.fileName)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )

                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        val reorderState = rememberReorderableLazyListState(
                            onMove = { from, to ->
                                draggingImages = true

                                val list = imageItems.toMutableList()
                                if (list.isEmpty()) return@rememberReorderableLazyListState

                                val fromIndex = from.index
                                if (fromIndex !in list.indices) return@rememberReorderableLazyListState

                                val item = list.removeAt(fromIndex)
                                val toIndex = to.index.coerceIn(0, list.size)

                                list.add(toIndex, item)
                                imageItems = list
                            },
                            onDragEnd = { _, _ ->
                                draggingImages = false


                            }
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            LazyRow(
                                state = reorderState.listState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .reorderable(reorderState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = imageItems,
                                    key = { item ->
                                        item.id?.let { "saved_$it" } ?: "pending_${item.fileName}"
                                    }
                                ) { image ->

                                    ReorderableItem(
                                        state = reorderState,
                                        key = image.id?.let { "saved_$it" }
                                            ?: "pending_${image.fileName}"
                                    ) { isDragging ->

                                        val file =
                                            File(File(context.filesDir, "images"), image.fileName)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(if (isDragging) 96.dp else 88.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    imageToDelete = image
                                                    showDeleteDialog = true
                                                }
                                                .detectReorderAfterLongPress(reorderState)
                                        ) {
                                            AsyncImage(
                                                model = uri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "جابجایی",
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(20.dp),
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
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
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "افزودن تصویر",
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }


                        }
                    }
                }
            }

            //کارت اطلاعات اصلی
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "اطلاعات اصلی",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = viewModel::onNameChange,
                            label = { Text("نام کالا") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.model,
                            onValueChange = viewModel::onModelChange,
                            label = { Text("مدل") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        val selectedBrandName =
                            uiState.brands
                                .firstOrNull { it.id == uiState.brandId }
                                ?.name
                                ?: "انتخاب نشده"

                        ExposedDropdownMenuBox(
                            expanded = brandMenuExpanded,
                            onExpandedChange = {
                                brandMenuExpanded = !brandMenuExpanded
                            }
                        ) {
                            OutlinedTextField(
                                value = selectedBrandName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("برند") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = brandMenuExpanded
                                    )
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = brandMenuExpanded,
                                onDismissRequest = {
                                    brandMenuExpanded = false
                                }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون برند") },
                                    onClick = {
                                        viewModel.onBrandChange(null)
                                        brandMenuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("+ افزودن برند جدید") },
                                    onClick = {
                                        brandMenuExpanded = false
                                        newBrandName = ""
                                        showAddBrandDialog = true
                                    }
                                )

                                uiState.brands.forEach { brand ->
                                    DropdownMenuItem(
                                        text = { Text(brand.name) },
                                        onClick = {
                                            viewModel.onBrandChange(brand.id)
                                            brandMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                    }
                }
            }

            //کارت قیمت خرید
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "قیمت خرید",
                            style = MaterialTheme.typography.titleMedium
                        )

                        MyCurrencyField(
                            value = uiState.buyPriceToman,
                            onValueChange = viewModel::onBuyPriceTomanChange,
                            label = "قیمت خرید تومان",
                            modifier = Modifier.fillMaxWidth()
                        )

                        MyCurrencyField(
                            value = uiState.dollarRateToman,
                            onValueChange = viewModel::onDollarRateChange,
                            label = "نرخ دلار خرید",
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.buyPriceDollar,
                            onValueChange = viewModel::onBuyPriceDollarChange,
                            label = { Text("قیمت دلاری") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.initialQuantity,
                            onValueChange = viewModel::onInitialQuantityChange,
                            label = { Text("تعداد خرید") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        DatePickerField(
                            label = "تاریخ خرید",
                            epochMs = uiState.purchaseDate,
                            onPick = viewModel::onPurchaseDateChange
                        )

                        OutlinedTextField(
                            value = uiState.priceNote,
                            onValueChange = viewModel::onPriceNoteChange,
                            label = { Text("یادداشت قیمت") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.addCurrentPurchaseToHistory()
                            },
                            enabled = uiState.productId != null && uiState.buyPriceToman != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (uiState.selectedPurchasePriceId != null) {
                                    "ویرایش این خرید"
                                } else {
                                    "ثبت این خرید در تاریخچه"
                                }
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = "تاریخچه خرید",
                            style = MaterialTheme.typography.titleMedium
                        )


                        if (uiState.purchasePrices.isEmpty()) {
                            Text("هنوز رکورد خریدی ثبت نشده است.")
                        } else {
                            uiState.purchasePrices.forEach { price ->
                                val isSelected =
                                    price.id != null && price.id == uiState.selectedPurchasePriceId

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.selectPurchasePrice(price)
                                        },
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("${price.buyPriceToman.toPriceString()} تومان | دلار: ${price.buyPriceDollar ?: "-"}")

                                            Row(
                                                modifier = Modifier
                                                    .padding(10.dp),
                                            ) {
                                                Text(
                                                    text = formatPersianDateTime(
                                                        price.purchasedAt,
                                                        true
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                VerticalDivider(thickness = 2.dp)
                                                Text(
                                                    text = "   نرخ دلار خرید :${ price.dollarRateToman.toPriceString() } ",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (price.note.isNotBlank()) {
                                                Text(
                                                    text = price.note,
                                                    maxLines = 1,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                purchaseToDelete = price
                                                showDeletePurchaseDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف رکورد خرید"
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            //کارت فروش
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "قیمت فروش",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = uiState.saleBaseDollarPrice,
                            onValueChange = viewModel::onSaleBaseDollarPriceChange,
                            label = { Text("قیمت دلاری مبنا") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        MyCurrencyField(
                            value = uiState.saleDollarRateToman,
                            onValueChange = viewModel::onSaleDollarRateChange,
                            label = "نرخ دلار فروش",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.consumerProfitPercent,
                                onValueChange = viewModel::onConsumerProfitPercentChange,
                                label = { Text("درصد مصرف‌کننده") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                modifier = Modifier.weight(0.45f)
                            )

                            Text(
                                text = uiState.consumerSalePriceToman?.toPriceString().let {
                                    "$it تومان"
                                } ?: "-",
                                modifier = Modifier.weight(0.55f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.colleagueProfitPercent,
                                onValueChange = viewModel::onColleagueProfitPercentChange,
                                label = { Text("درصد همکار") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                modifier = Modifier.weight(0.45f)
                            )

                            Text(
                                text = uiState.colleagueSalePriceToman?.toPriceString().let {
                                    "$it تومان"
                                } ?: "-",
                                modifier = Modifier.weight(0.55f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        DatePickerField(
                            label = "تاریخ قیمت فروش",
                            epochMs = uiState.saleDate,
                            onPick = viewModel::onSaleDateChange
                        )

                        OutlinedTextField(
                            value = uiState.saleNote,
                            onValueChange = viewModel::onSaleNoteChange,
                            label = { Text("یادداشت فروش") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val isEditingSale =
                            uiState.selectedConsumerSalePriceId != null ||
                                    uiState.selectedColleagueSalePriceId != null

                        Button(
                            onClick = {
                                viewModel.addCurrentSalePricesToHistory()
                            },
                            enabled =
                            uiState.productId != null &&
                                    uiState.saleBaseDollarPrice.isNotBlank() &&
                                    uiState.saleDollarRateToman != null &&
                                    (
                                            uiState.consumerSalePriceToman != null ||
                                                    uiState.colleagueSalePriceToman != null
                                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isEditingSale) {
                                    "ویرایش این فروش"
                                } else {
                                    "ثبت این فروش در تاریخچه"
                                }
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = "تاریخچه قیمت فروش",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (uiState.salePrices.isEmpty()) {
                            Text("هنوز قیمت فروشی ثبت نشده است.")
                        } else {
                            val groupedSales =
                                uiState.salePrices
                                    .groupBy { it.createdAt }
                                    .toSortedMap(compareByDescending { it })

                            groupedSales.forEach { (createdAt, prices) ->

                                val consumer =
                                    prices.firstOrNull { it.priceType == "consumer" }

                                val colleague =
                                    prices.firstOrNull { it.priceType == "colleague" }

                                val base =
                                    consumer ?: colleague

                                val isSelected =
                                    consumer?.id == uiState.selectedConsumerSalePriceId ||
                                            colleague?.id == uiState.selectedColleagueSalePriceId

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.selectSaleGroup(
                                                consumer = consumer,
                                                colleague = colleague
                                            )
                                        },
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {


                                        consumer?.let { price ->
                                            Text(
                                                text = "مصرف‌کننده: ${price.salePriceToman.toPriceString()} تومان | سود: ${price.profitPercent ?: "-"}٪",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        colleague?.let { price ->
                                            Text(
                                                text = "همکار:          ${price.salePriceToman.toPriceString()} تومان | سود: ${price.profitPercent ?: "-"}٪",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = formatPersianDateTime(createdAt),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )

                                                Text(
                                                    text = "دلار: ${base?.dollarRateToman ?: "-"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    saleGroupToDelete = prices
                                                    showDeleteSaleDialog = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف رکورد فروش"
                                                )
                                            }
                                        }

                                        val note =
                                            base?.note.orEmpty()

                                        if (note.isNotBlank()) {
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

            //کارت موجودی اولیه
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "موجودی",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = "موجودی فعلی: ${uiState.currentStock}",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedTextField(
                            value = uiState.inventoryQuantity,
                            onValueChange = viewModel::onInventoryQuantityChange,
                            label = { Text("تعداد") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        InventoryTypeSelector(
                            selectedType = uiState.inventoryTransactionType,
                            onTypeChange = viewModel::onInventoryTransactionTypeChange
                        )

                        DatePickerField(
                            label = "تاریخ تراکنش",
                            epochMs = uiState.inventoryTransactionDate,
                            onPick = viewModel::onInventoryTransactionDateChange
                        )

                        OutlinedTextField(
                            value = uiState.inventoryNote,
                            onValueChange = viewModel::onInventoryNoteChange,
                            label = { Text("یادداشت موجودی") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.addOrUpdateInventoryTransaction()
                            },
                            enabled = uiState.productId != null &&
                                    uiState.inventoryQuantity.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (uiState.selectedInventoryTransactionId != null) {
                                    "ویرایش این تراکنش موجودی"
                                } else {
                                    "ثبت این تراکنش در تاریخچه"
                                }
                            )
                        }

                        HorizontalDivider()

                        Text(
                            text = "تاریخچه موجودی",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (uiState.inventoryTransactions.isEmpty()) {
                            Text("هنوز تراکنش موجودی ثبت نشده است.")
                        } else {
                            uiState.inventoryTransactions.forEach { item ->

                                val isSelected =
                                    item.id == uiState.selectedInventoryTransactionId

                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.selectInventoryTransaction(item)
                                        },
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${inventoryTypeTitle(item.transactionType)} | تعداد: ${item.quantity}"
                                            )

                                            Text(
                                                text = formatPersianDateTime(item.createdAt),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            if (item.note.isNotBlank()) {
                                                Text(
                                                    text = item.note,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                inventoryToDelete = item
                                                showDeleteInventoryDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف تراکنش موجودی"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //کارت مشخصات فنی
            item {
                HorizontalDivider()
                Text(
                    text = "مشخصات فنی",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (uiState.attributes.isEmpty()) {
                item {
                    Text("برای این دسته هنوز مشخصه‌ای تعریف نشده است.")
                }
            } else {
                items(uiState.attributes) { attr ->
                    ProductAttributeInputField(
                        attr = attr,
                        value = uiState.attributeValues[attr.attributeDefinitionId].orEmpty(),
                        onValueChange = { value ->
                            viewModel.onAttributeValueChange(
                                attributeDefinitionId = attr.attributeDefinitionId,
                                value = value
                            )
                        }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        viewModel.saveWithImageOrder(
                            imageItems = imageItems,
                            onSuccess = onClose
                        )
                    },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (uiState.isSaving) "در حال ذخیره..." else "ذخیره کالا"
                    )
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
                Text(
                    text = "افزودن تصویر کالا",
                    style = MaterialTheme.typography.titleMedium
                )

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
            }
        }
    }
    if (showAddBrandDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddBrandDialog = false
            },
            title = {
                Text("افزودن برند جدید")
            },
            text = {
                OutlinedTextField(
                    value = newBrandName,
                    onValueChange = {
                        newBrandName = it
                    },
                    label = {
                        Text("نام برند")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createBrandAndSelect(
                            name = newBrandName,
                            onDone = {
                                showAddBrandDialog = false
                                newBrandName = ""
                            }
                        )
                    }
                ) {
                    Text("افزودن")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddBrandDialog = false
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
    if (showDeleteDialog) {

        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text("حذف تصویر")
            },

            text = {
                Text("این تصویر حذف شود؟")
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        imageToDelete?.let {
                            viewModel.deleteImage(it)
                        }

                        showDeleteDialog = false
                        imageToDelete = null
                    }
                ) {
                    Text("حذف")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        imageToDelete = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
    if (showDeletePurchaseDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeletePurchaseDialog = false
                purchaseToDelete = null
            },
            title = {
                Text("حذف رکورد خرید")
            },
            text = {
                Text("آیا از حذف این رکورد خرید مطمئنی؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        purchaseToDelete?.id?.let { id ->
                            viewModel.deletePurchasePrice(id)
                        }

                        showDeletePurchaseDialog = false
                        purchaseToDelete = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeletePurchaseDialog = false
                        purchaseToDelete = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
    if (showDeleteSaleDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteSaleDialog = false
                saleGroupToDelete = emptyList()
            },
            title = {
                Text("حذف رکورد فروش")
            },
            text = {
                Text("آیا از حذف این رکورد فروش مطمئنی؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        saleGroupToDelete.forEach { item ->
                            item.id?.let { id ->
                                viewModel.deleteSalePrice(id)
                            }
                        }

                        showDeleteSaleDialog = false
                        saleGroupToDelete = emptyList()
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteSaleDialog = false
                        saleGroupToDelete = emptyList()
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
    if (showDeleteInventoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteInventoryDialog = false
                inventoryToDelete = null
            },
            title = {
                Text("حذف تراکنش موجودی")
            },
            text = {
                Text("آیا از حذف این تراکنش موجودی مطمئنی؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        inventoryToDelete?.id?.let { id ->
                            viewModel.deleteInventoryTransaction(id)
                        }

                        showDeleteInventoryDialog = false
                        inventoryToDelete = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteInventoryDialog = false
                        inventoryToDelete = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductAttributeInputField(
    attr: ProductAttributeDisplayInfo,
    value: String,
    onValueChange: (String) -> Unit
) {
    val labelText = buildString {
        append(attr.title)

        if (attr.key.isNotBlank()) {
            append(" _ ")
            append(attr.key)
        }

        if (!attr.unit.isNullOrBlank()) {
            append(" _ (${attr.unit})")
        }
    }


    when (attr.valueType.lowercase()) {

        "number" -> {
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(
                        newValue.filter { it.isDigit() || it == '.' }
                    )
                },
                label = { Text(labelText) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal
                ),
                trailingIcon = {
                    AttributeDescriptionIcon(
                        description = attr.description,
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        "boolean" -> {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labelText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Switch(
                        checked = value == "true",
                        onCheckedChange = {
                            onValueChange(it.toString())
                        }
                    )
                    AttributeDescriptionIcon(
                        description = attr.description,
                    )
                }
            }
        }

        "enum" -> {
            var expanded by remember { mutableStateOf(false) }

            val options =
                attr.enumOptions
                    .orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = value.ifBlank { "انتخاب نشده" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(labelText) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("پاک کردن انتخاب") },
                            onClick = {
                                onValueChange("")
                                expanded = false
                            }
                        )

                        options.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                AttributeDescriptionIcon(
                    description = attr.description,
                )
            }
        }

        else -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(labelText) },
                textStyle = LocalTextStyle.current.copy(
                    textDirection = TextDirection.Ltr
                ),
                trailingIcon = {
                    AttributeDescriptionIcon(
                        description = attr.description,
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttributeDescriptionIcon(
    description: String
) {
    if (description.isBlank()) return

    val tooltipState = rememberTooltipState(
        isPersistent = true
    )
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above
        ),
        tooltip = {
            PlainTooltip {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(description)

                    TextButton(
                        onClick = {
                            scope.launch {
                                tooltipState.dismiss()
                            }
                        }
                    ) {
                        Text("فهمیدم")
                    }
                }
            }
        },
        state = tooltipState
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    tooltipState.show()
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "توضیح مشخصه"
            )
        }
    }
}

@Composable
private fun DatePickerField(
    label: String,
    epochMs: Long,
    onPick: (Long) -> Unit,
    noClock: Boolean = true,
    dateUi: DateUi = PersianDateUiAdapter
) {
    var open by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = formatPersianDateTime(epochMs, noClock = noClock),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            TextButton(
                onClick = {
                    open = true
                }
            ) {
                Text("انتخاب")
            }
        }
    )

    if (open) {
        dateUi.Picker(
            currentEpochMs = epochMs,
            onPick = { picked ->
                onPick(picked)
                open = false
            },
            onDismiss = {
                open = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryTypeSelector(
    selectedType: InventoryTransactionType,
    onTypeChange: (InventoryTransactionType) -> Unit
) {
    val options = listOf(
        InventoryTransactionType.PURCHASE,
        InventoryTransactionType.SALE,
        InventoryTransactionType.SALE_RETURN,
        InventoryTransactionType.PURCHASE_RETURN,
        InventoryTransactionType.ADJUSTMENT
    )

    var expanded by remember {
        mutableStateOf(false)
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        OutlinedTextField(
            value = inventoryTypeTitle(selectedType),
            onValueChange = {},
            readOnly = true,
            label = { Text("نوع تراکنش") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { type ->
                DropdownMenuItem(
                    text = { Text(inventoryTypeTitle(type)) },
                    onClick = {
                        onTypeChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun inventoryTypeTitle(
    type: InventoryTransactionType
): String {
    return when (type) {
        InventoryTransactionType.PURCHASE -> "خرید / ورود کالا"
        InventoryTransactionType.SALE -> "فروش / خروج کالا"
        InventoryTransactionType.SALE_RETURN -> "برگشت از مشتری"
        InventoryTransactionType.PURCHASE_RETURN -> "برگشت به تأمین‌کننده"
        InventoryTransactionType.ADJUSTMENT -> "اصلاح دستی"
    }
}