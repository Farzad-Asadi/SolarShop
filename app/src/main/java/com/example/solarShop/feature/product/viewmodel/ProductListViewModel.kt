package com.example.solarShop.feature.product.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.repository.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val productRepository: ProductRepository,
) : ViewModel() {

    val uiState =
        combine(
            productRepository.observeActiveCategories(),
            productRepository.observeProductCountByCategory()
        ) { categories, productCountByCategory ->
            ProductListUiState(
                isLoading = false,
                categories = categories,
                productCountByCategory = productCountByCategory
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProductListUiState(isLoading = true)
        )

    fun updateCategorySortOrders(
        categories: List<ProductCategoryEntity>
    ) {
        viewModelScope.launch {
            productRepository.updateCategorySortOrders(categories)
        }
    }

}