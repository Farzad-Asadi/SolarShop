package com.example.solarShop.feature.product.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.repository.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProductByCategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val categoryId =
        checkNotNull(
            savedStateHandle.get<Int>("categoryId")
        )

    val uiState = combine(
        flow {
            val category = productRepository.getCategoryById(categoryId)
            emit(category)
        },
        productRepository.observeProductsByCategoryFullInfo(categoryId)
    ) { category, products ->
        ProductByCategoryUiState(
            isLoading = false,
            categoryId = categoryId,
            categoryName = category?.name.orEmpty(),
            products = products
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProductByCategoryUiState()
    )
}