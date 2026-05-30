package com.example.solarShop.data.local.entity.pricing

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID


@Entity(
    tableName = "product_purchase_prices",
    indices = [
        Index("productId"),
        Index("createdAt"),
        Index(value = ["productId", "isActive"])
    ]
)
data class ProductPurchasePriceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    val buyPriceDollar: Double? = null,
    val buyPriceToman: Long? = null,
    val dollarRateToman: Long? = null,

    val note: String = "",
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis()
)