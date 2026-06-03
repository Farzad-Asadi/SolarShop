package com.example.solarShop.feature.product.viewmodel

import com.example.solarShop.feature.product.viewmodel.product.ProductGridItemUi

data class ProductByCategoryUiState(
    val isLoading: Boolean = true,

    val categoryId: Int? = null,
    val categoryName: String = "",

    val products: List<ProductGridItemUi> = emptyList(),

    val selectedProductIds: Set<Int> = emptySet()
)