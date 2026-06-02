package com.example.solarShop.feature.product.viewmodel.category

import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity

data class CategoryEditUiState(
    val categoryId: Int? = null,

    val name: String = "",
    val description: String = "",

    val imageFileName: String? = null,

    val isSaving: Boolean = false,

    val attributes: List<CategoryAttributeDefinitionEntity> = emptyList(),
)