package com.example.solarShop.feature.product.viewmodel.category

data class CategoryEditUiState(
    val categoryId: Int? = null,

    val name: String = "",
    val description: String = "",

    val imageFileName: String? = null,

    val isSaving: Boolean = false
)