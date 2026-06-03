package com.example.solarShop.feature.product.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.solarShop.feature.product.viewmodel.ProductByCategoryViewModel

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun ProductByCategoryScreen(
    onBack: () -> Unit,
    onAddProduct: (Int) -> Unit,
    onProductClick: (Int) -> Unit,
    viewModel: ProductByCategoryViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.selectedProductIds.isEmpty()) {
                        Text(uiState.categoryName)
                    } else {
                        Text("${uiState.selectedProductIds.size} کالا انتخاب شده")
                    }
                },
                navigationIcon = {
                    if (uiState.selectedProductIds.isEmpty()) {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.clearSelection()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "خروج از حالت انتخاب"
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.selectedProductIds.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                showDeleteDialog = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف کالاهای انتخاب‌شده"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedProductIds.isEmpty()) {
                FloatingActionButton(
                    onClick = { uiState.categoryId?.let { onAddProduct(it) } }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                }
            }
        }
    ) { padding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("هنوز کالایی در این دسته ثبت نشده است")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.products) { item ->
                    val productId = item.productFullInfo.product.id
                    val isSelected =
                        productId != null && productId in uiState.selectedProductIds

                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (productId == null) return@combinedClickable

                                    if (uiState.selectedProductIds.isNotEmpty()) {
                                        viewModel.toggleSelection(productId)
                                    } else {
                                        onProductClick(productId)
                                    }
                                },
                                onLongClick = {
                                    if (productId != null) {
                                        viewModel.startSelection(productId)
                                    }
                                }
                            ),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.coverUri != null) {
                                        AsyncImage(
                                            model = item.coverUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Inventory2,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.45f
                                            )
                                        )
                                    }
                                }

                                Column {

                                    val brandName = item.productFullInfo.brand?.name
                                    Text(
                                        text = item.productFullInfo.product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 2
                                    )

                                    if (!brandName.isNullOrBlank()) {
                                        Text(
                                            text = brandName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }

                                    if (item.productFullInfo.product.model.isNotBlank()) {
                                        Text(
                                            text = item.productFullInfo.product.model,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (uiState.selectedProductIds.isNotEmpty()) {
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Outlined.Circle
                                    },
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(28.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("حذف کالا")
            },
            text = {
                Text("آیا از حذف ${uiState.selectedProductIds.size} کالا مطمئنی؟")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedProducts()
                        showDeleteDialog = false
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}