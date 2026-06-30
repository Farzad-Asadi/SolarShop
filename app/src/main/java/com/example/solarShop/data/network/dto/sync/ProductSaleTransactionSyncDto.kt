package com.example.solarShop.data.network.dto.sync

import kotlinx.serialization.Serializable

@Serializable
data class ProductSaleTransactionSyncDto(
    val uid: String,

    val productUid: String,

    val inventoryTransactionUid: String? = null,

    val quantity: Double,

    val priceType: String,

    val unitSalePriceToman: Long,
    val totalSalePriceToman: Long,

    val saleDollarRateToman: Long? = null,

    val purchasePriceUid: String? = null,
    val salePriceUid: String? = null,

    val buyPriceDollar: Double? = null,
    val buyPriceToman: Long? = null,
    val purchaseDollarRateToman: Long? = null,

    val unitSalePriceDollar: Double? = null,

    val unitProfitToman: Long? = null,
    val totalProfitToman: Long? = null,

    val unitProfitDollar: Double? = null,
    val totalProfitDollar: Double? = null,

    val profitPercentByToman: Double? = null,
    val profitPercentByDollar: Double? = null,

    val soldAt: Long,

    val note: String,

    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)