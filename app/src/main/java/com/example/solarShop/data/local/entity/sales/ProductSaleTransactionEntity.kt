package com.example.solarShop.data.local.entity.sales

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "product_sale_transactions",
    indices = [
        Index("uid", unique = true),
        Index("productId"),
        Index("inventoryTransactionUid"),
        Index("soldAt"),
        Index("createdAt"),
        Index("updatedAt")
    ]
)
data class ProductSaleTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    // لینک اختیاری به تراکنش موجودی از نوع SALE
    val inventoryTransactionUid: String? = null,

    val quantity: Double,

    // consumer / colleague / manual
    val priceType: String = "consumer",

    // قیمت فروش واقعی در لحظه فروش
    val unitSalePriceToman: Long,
    val totalSalePriceToman: Long,

    // نرخ دلار لحظه فروش
    val saleDollarRateToman: Long? = null,

    // لینک اختیاری به رکورد خرید/قیمت فروش مبنا
    val purchasePriceUid: String? = null,
    val salePriceUid: String? = null,

    // snapshot قیمت خرید در لحظه فروش
    val buyPriceDollar: Double? = null,
    val buyPriceToman: Long? = null,
    val purchaseDollarRateToman: Long? = null,

    // معادل دلاری فروش
    val unitSalePriceDollar: Double? = null,

    // سود واقعی snapshot شده
    val unitProfitToman: Long? = null,
    val totalProfitToman: Long? = null,

    val unitProfitDollar: Double? = null,
    val totalProfitDollar: Double? = null,

    val profitPercentByToman: Double? = null,
    val profitPercentByDollar: Double? = null,

    val soldAt: Long = System.currentTimeMillis(),

    val note: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val deletedAt: Long? = null,

    val isSynced: Boolean = false
)