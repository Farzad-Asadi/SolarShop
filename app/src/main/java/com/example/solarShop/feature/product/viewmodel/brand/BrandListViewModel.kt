package com.example.solarShop.feature.product.viewmodel.brand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.solarShop.data.repository.product.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BrandListViewModel @Inject constructor(
    productRepository: ProductRepository
) : ViewModel() {

    val uiState = productRepository.observeActiveBrands()
        .map { brands ->
            BrandListUiState(
                isLoading = false,
                brands = brands
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BrandListUiState()
        )
}