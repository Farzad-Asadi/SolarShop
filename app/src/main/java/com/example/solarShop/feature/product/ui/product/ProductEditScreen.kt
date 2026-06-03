package com.example.solarShop.feature.product.ui.product

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo
import com.example.solarShop.feature.product.viewmodel.product.ProductEditViewModel
import com.example.solarShop.utils.MyCurrencyField
import com.example.solarShop.utils.rememberCameraCaptureLauncher
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onClose: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    var showPickSheet by remember { mutableStateOf(false) }
    var pendingTempFile by remember { mutableStateOf<File?>(null) }

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

    var brandMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("کالای جدید") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تصاویر کالا",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val coverImage = uiState.images.firstOrNull()

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

                        if (uiState.images.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.images.forEach { image ->
                                    val file = File(File(context.filesDir, "images"), image.fileName)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )

                                    ElevatedCard(
                                        modifier = Modifier.size(72.dp)
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                showPickSheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("افزودن تصویر")
                        }
                    }
                }
            }

            //کارت اطلاعات اصلی
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.priceNote,
                            onValueChange = viewModel::onPriceNoteChange,
                            label = { Text("یادداشت قیمت") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            //کارت موجودی اولیه
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "موجودی اولیه",
                            style = MaterialTheme.typography.titleMedium
                        )

                        OutlinedTextField(
                            value = uiState.initialQuantity,
                            onValueChange = viewModel::onInitialQuantityChange,
                            label = { Text("تعداد موجودی") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.inventoryNote,
                            onValueChange = viewModel::onInventoryNoteChange,
                            label = { Text("یادداشت موجودی") },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                        viewModel.save(
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductAttributeInputField(
    attr: ProductAttributeDisplayInfo,
    value: String,
    onValueChange: (String) -> Unit
) {
    val labelText =
        if (attr.unit.isNullOrBlank()) attr.title else "${attr.title} (${attr.unit})"

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

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }
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
        }

        else -> {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(labelText) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

}