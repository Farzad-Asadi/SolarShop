package com.example.solarShop.feature.product.viewmodel.category

import com.example.solarShop.domain.product.AttributeValueType

data class AttributeEditUiState(
    val categoryId: Int? = null,

    val title: String = "",
    val key: String = "",

    val valueType: AttributeValueType = AttributeValueType.NUMBER,

    val unit: String = "",
    val isRequired: Boolean = false,

    val isSaving: Boolean = false
)