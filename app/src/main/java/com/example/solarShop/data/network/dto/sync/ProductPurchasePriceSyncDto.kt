package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class ProductPurchasePriceSyncDto(
    val uid: String,
    val productUid: String,
    val buyPriceDollar: Double?,
    val buyPriceToman: Long?,
    val dollarRateToman: Long?,
    val quantity: Double?,
    val purchasedAt: Long,
    val note: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)