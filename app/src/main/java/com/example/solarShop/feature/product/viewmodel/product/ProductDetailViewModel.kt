package com.example.solarShop.feature.product.viewmodel.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.repository.attribute.AttributeRepository
import com.example.solarShop.data.repository.inventory.InventoryRepository
import com.example.solarShop.data.repository.pricing.PricingRepository
import com.example.solarShop.data.repository.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    productRepository: ProductRepository,
    attributeRepository: AttributeRepository,
    private val pricingRepository: PricingRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val productId =
        checkNotNull(
            savedStateHandle.get<Int>("productId")
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState =
        productRepository
            .observeProductFullInfo(productId)
            .flatMapLatest { product ->

                if (product == null) {
                    flowOf(
                        ProductDetailUiState(
                            isLoading = false
                        )
                    )
                } else {

                    combine(
                        attributeRepository.observeProductAttributeDisplayInfo(
                            productId = productId,
                            categoryId = product.product.categoryId
                        ),
                        inventoryRepository.observeCurrentStock(productId)
                    ) { attributes, currentStock ->

                        val activePrice =
                            pricingRepository.getActivePurchasePrice(productId)

                        val salePriceResult =
                            pricingRepository.calculateSalePrice(productId)

                        ProductDetailUiState(
                            isLoading = false,
                            product = product,
                            attributes = attributes,
                            activePurchasePrice = activePrice,
                            salePriceResult = salePriceResult,
                            currentStock = currentStock
                        )
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                ProductDetailUiState()
            )
}