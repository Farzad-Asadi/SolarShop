package com.example.solarShop.feature.product.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.feature.product.viewmodel.product.ProductEditViewModel
import com.example.solarShop.utils.MyCurrencyField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditScreen(
    onClose: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                            text = "تصویر کالا",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            if (uiState.coverImageFileName == null) {

                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp)
                                )

                            } else {

                                // فعلاً بعداً AsyncImage وصل می‌کنیم
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                // مرحله بعد
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("انتخاب تصویر")
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
                    OutlinedTextField(
                        value = uiState.attributeValues[attr.attributeDefinitionId].orEmpty(),
                        onValueChange = {
                            viewModel.onAttributeValueChange(
                                attributeDefinitionId = attr.attributeDefinitionId,
                                value = it
                            )
                        },
                        label = {
                            Text(
                                if (attr.unit.isNullOrBlank())
                                    attr.title
                                else
                                    "${attr.title} (${attr.unit})"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
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
}