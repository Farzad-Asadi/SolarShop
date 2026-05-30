package com.example.solarShop.feature.product.viewmodel.product

import com.example.solarShop.data.local.relation.product.ProductAttributeDisplayInfo

data class ProductEditUiState(

    val categoryId: Int? = null,

    val name: String = "",
    val model: String = "",

    val attributes: List<ProductAttributeDisplayInfo> = emptyList(),

    val attributeValues: Map<Int, String> = emptyMap(),

    val isSaving: Boolean = false
)