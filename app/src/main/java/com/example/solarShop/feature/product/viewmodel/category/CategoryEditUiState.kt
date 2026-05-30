package com.example.solarShop.feature.product.viewmodel.category

data class CategoryEditUiState(
    val categoryId: Int? = null,

    val name: String = "",
    val description: String = "",

    val isSaving: Boolean = false
)