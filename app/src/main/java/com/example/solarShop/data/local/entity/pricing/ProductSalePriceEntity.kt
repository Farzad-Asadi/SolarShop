package com.example.solarShop.data.local.entity.pricing

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_sale_prices",
    indices = [
        Index("productId"),
        Index("priceType"),
        Index("createdAt"),
        Index(value = ["productId", "priceType", "isActive"])
    ]
)
data class ProductSalePriceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    // consumer یا colleague
    val priceType: String,

    val salePriceToman: Long,

    val profitPercent: Double? = null,

// قیمت خرید/مبنای دلاری کالا در زمان محاسبه فروش
    val baseDollarPrice: Double? = null,

// نرخ دلاری که قیمت فروش با آن محاسبه شده
    val dollarRateToman: Long? = null,

// قیمت خرید تومانی مبنا، برای مراجعات بعدی
    val basePurchasePriceToman: Long? = null,

    val note: String = "",
    val isActive: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val deletedAt: Long? = null,

    val isSynced: Boolean = false
)