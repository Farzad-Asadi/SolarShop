package com.example.solarShop.feature.product.viewmodel

import com.example.solarShop.data.local.entity.product.ProductCategoryEntity

data class ProductListUiState(
    val isLoading: Boolean = false,
    val categories: List<ProductCategoryEntity> = emptyList(),
    val productCountByCategory: Map<Int, Int> = emptyMap()

)


data class CategoryProductCount(
    val categoryId: Int,
    val count: Int
)