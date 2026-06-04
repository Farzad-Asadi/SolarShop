package com.example.solarShop.feature.product.viewmodel

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.repository.product.ProductRepository
import com.example.solarShop.data.repository.productImage.ProductImageRepository
import com.example.solarShop.feature.product.viewmodel.product.ProductGridItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProductByCategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    @ApplicationContext private val app: Context,
    private val productImageRepository: ProductImageRepository
) : ViewModel() {

    private val categoryId =
        checkNotNull(
            savedStateHandle.get<Int>("categoryId")
        )

    private val selectedProductIds = MutableStateFlow<Set<Int>>(emptySet())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        flow {
            val category = productRepository.getCategoryById(categoryId)
            emit(category)
        },
        productRepository.observeProductsByCategoryFullInfo(categoryId),
        selectedProductIds
    ) { category, products ,selectedIds->
        Triple(category, products, selectedIds)
    }.flatMapLatest { (category, products, selectedIds) ->

        val productIds = products.mapNotNull { it.product.id }

        productImageRepository
            .observeImagesForProducts(productIds)
            .map { images ->

                val coverByProductId = images
                    .groupBy { it.productId }
                    .mapValues { (_, list) ->
                        list.minByOrNull { it.sortOrder }
                    }

                ProductByCategoryUiState(
                    isLoading = false,
                    categoryId = categoryId,
                    categoryName = category?.name.orEmpty(),
                    products = products.map { productFullInfo ->

                        val coverImage =
                            productFullInfo.product.id?.let { id ->
                                coverByProductId[id]
                            }

                        val coverUri = coverImage?.let { image ->
                            val file = File(
                                File(app.filesDir, "images"),
                                image.fileName
                            )

                            FileProvider.getUriForFile(
                                app,
                                "${app.packageName}.fileprovider",
                                file
                            )
                        }

                        ProductGridItemUi(
                            productFullInfo = productFullInfo,
                            coverUri = coverUri
                        )
                    },
                    selectedProductIds = selectedIds
                )
            }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProductByCategoryUiState()
    )


    init {
        viewModelScope.launch {
            productRepository.cleanupOldDraftProducts()
        }
    }


    fun startSelection(productId: Int) {
        selectedProductIds.value = setOf(productId)
    }

    fun toggleSelection(productId: Int) {
        selectedProductIds.update { current ->
            if (productId in current) {
                current - productId
            } else {
                current + productId
            }
        }
    }

    fun clearSelection() {
        selectedProductIds.value = emptySet()
    }

    fun deleteSelectedProducts() {
        viewModelScope.launch {
            selectedProductIds.value.forEach { productId ->
                productRepository.archiveProduct(productId)
            }

            clearSelection()
        }
    }
}