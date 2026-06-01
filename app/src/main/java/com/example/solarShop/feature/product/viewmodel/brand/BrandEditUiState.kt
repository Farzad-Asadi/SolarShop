package com.example.solarShop.feature.product.viewmodel.brand

data class BrandEditUiState(
    val brandId: Int? = null,

    val name: String = "",
    val description: String = "",
    val imageFileName: String? = null,

    val isSaving: Boolean = false
)