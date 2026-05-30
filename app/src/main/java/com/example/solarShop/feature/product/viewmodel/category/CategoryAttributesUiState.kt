package com.example.solarShop.feature.product.viewmodel.category

import com.example.solarShop.data.local.entity.attribute.CategoryAttributeDefinitionEntity

data class CategoryAttributesUiState(
    val isLoading: Boolean = true,

    val categoryId: Int? = null,
    val attributes: List<CategoryAttributeDefinitionEntity> = emptyList()
)