package com.example.solarShop.data.room.tables.product

data class ProductAttributeDisplayInfo(
    val valueId: Int?,
    val productId: Int,
    val attributeDefinitionId: Int,

    val title: String,
    val key: String,
    val valueType: String,
    val unit: String?,
    val isRequired: Boolean,
    val sortOrder: Int,

    val valueText: String?
)


