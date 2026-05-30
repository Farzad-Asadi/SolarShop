package com.example.solarShop.data.local.relation.product

data class ProductListItem(
    val productId: Int?,
    val name: String,
    val model: String,

    val categoryName: String?,
    val brandName: String?,

    val currentStock: Double?
)