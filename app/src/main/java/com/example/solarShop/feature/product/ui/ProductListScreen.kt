package com.example.solarShop.feature.product.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.feature.product.ui.component.CategoryCard
import com.example.solarShop.feature.product.viewmodel.ProductListViewModel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel = hiltViewModel(),
    onAddCategoryClick: () -> Unit = {},
    onCategoryClick: (Int) -> Unit = {},
    onEditCategoryClick: (Int) -> Unit = {},
    onOpenBrands: () -> Unit,
    onHomeClick: () -> Unit = {},

) {
    val uiState by viewModel.uiState.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    var isReorderMode by rememberSaveable {
        mutableStateOf(false)
    }



    var reorderCategories by remember {
        mutableStateOf(uiState.categories)
    }

    LaunchedEffect(uiState.categories, isReorderMode) {
        if (!isReorderMode) {
            reorderCategories = uiState.categories
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isReorderMode) {
                            "چینش دسته‌ها"
                        } else {
                            "دسته‌بندی کالاها"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "صفحه اصلی"
                        )
                    }
                },
                actions = {

                    if (isReorderMode) {
                        IconButton(
                            onClick = {
                                viewModel.updateCategorySortOrders(reorderCategories)
                                isReorderMode = false
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "پایان چینش"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                showMenu = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            }
                        ) {

                            DropdownMenuItem(
                                text = {
                                    Text("مدیریت برندها")
                                },
                                onClick = {
                                    showMenu = false
                                    onOpenBrands()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تنظیم چینش دسته‌ها") },
                                onClick = {
                                    showMenu = false
                                    reorderCategories = uiState.categories
                                    isReorderMode = true
                                }
                            )
                        }
                    }


                }
            )
        },
        floatingActionButton = {
            var expanded by remember { mutableStateOf(false) }

            Box {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("ایجاد دسته‌بندی جدید") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            expanded = false
                            onAddCategoryClick()
                        }
                    )
                }

                FloatingActionButton(
                    onClick = { expanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "افزودن"
                    )
                }
            }
        }
    ) { paddingValues ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {

            if (isReorderMode) {
                val context = LocalContext.current

                CategoryReorderList(
                    categories = reorderCategories,
                    onMove = { fromIndex, toIndex ->
                        val list = reorderCategories.toMutableList()
                        val item = list.removeAt(fromIndex)
                        list.add(toIndex, item)
                        reorderCategories = list
                    },

                    imageUriForCategory = { category ->


                        category.imageFileName?.let { fileName ->
                            val file = File(File(context.filesDir, "images"), fileName)
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    scaffoldPadding = paddingValues
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.categories) { category ->

                        val context = LocalContext.current

                        val imageUri = category.imageFileName?.let { fileName ->
                            val file = File(File(context.filesDir, "images"), fileName)
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        }

                        CategoryCard(
                            name = category.name,
                            imageUri = imageUri,
                            productCount = uiState.productCountByCategory[category.id] ?: 0,
                            onClick = {
                                category.id?.let { onCategoryClick(it) }
                            },
                            onLongClick = {
                                category.id?.let { onEditCategoryClick(it) }
                            }
                        )
                    }
                }
            }



        }
    }
}

@Composable
private fun CategoryReorderList(
    categories: List<ProductCategoryEntity>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    imageUriForCategory: (ProductCategoryEntity) -> Uri?,
    modifier: Modifier = Modifier,
    scaffoldPadding: PaddingValues
) {
    val reorderState =
        rememberReorderableLazyListState(
            onMove = { from, to ->
                onMove(
                    from.index,
                    to.index.coerceIn(0, categories.lastIndex)
                )
            }
        )

    LazyColumn(
        state = reorderState.listState,
        modifier = modifier
            .fillMaxSize()
            .reorderable(reorderState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = scaffoldPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom = scaffoldPadding.calculateBottomPadding() + 16.dp
        )
    ) {
        items(
            items = categories,
            key = { it.id ?: it.uid }
        ) { category ->

            ReorderableItem(
                state = reorderState,
                key = category.id ?: category.uid
            ) { isDragging ->

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .detectReorderAfterLongPress(reorderState),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = if (isDragging) 10.dp else 3.dp
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val imageUri = imageUriForCategory(category)

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "جابجایی",
                            modifier = Modifier
                                .size(28.dp)
                                .detectReorderAfterLongPress(reorderState)
                        )
                    }
                }
            }
        }
    }
}