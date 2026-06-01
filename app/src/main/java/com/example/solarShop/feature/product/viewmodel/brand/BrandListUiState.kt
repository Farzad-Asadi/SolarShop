package com.example.solarShop.feature.product.viewmodel.brand

import com.example.solarShop.data.local.entity.product.ProductBrandEntity

data class BrandListUiState(
    val isLoading: Boolean = true,
    val brands: List<ProductBrandEntity> = emptyList()
)