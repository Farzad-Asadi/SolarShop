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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = combine(
        flow {
            val category = productRepository.getCategoryById(categoryId)
            emit(category)
        },
        productRepository.observeProductsByCategoryFullInfo(categoryId)
    ) { category, products ->
        category to products
    }.flatMapLatest { (category, products) ->

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
                    }
                )
            }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProductByCategoryUiState()
    )
}