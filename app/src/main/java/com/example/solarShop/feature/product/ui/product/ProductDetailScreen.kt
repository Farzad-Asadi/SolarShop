package com.example.solarShop.feature.product.ui.product

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.solarShop.feature.product.viewmodel.product.ProductDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    onBack: () -> Unit,
    onEditProduct: (Int) -> Unit,
    onEditPurchasePrice: (Int) -> Unit,
    onAddInventoryTransaction: (Int) -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.product

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
}