package com.example.solarShop.feature.product.viewmodel

import com.example.solarShop.data.local.relation.product.ProductFullInfo

data class ProductByCategoryUiState(
    val isLoading: Boolean = true,

    val categoryId: Int? = null,
    val categoryName: String = "",

    val products: List<ProductFullInfo> = emptyList()
)