package com.example.solarShop.data.room.tables.orderAll.priceEstimate

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.data.room.tables.orderAll.order.OrderEntity
import com.example.solarShop.utils.EstimateApproach
import com.example.solarShop.utils.EstimateCategory


@Entity(
    tableName = "price_estimates",
    indices = [Index("orderId")],
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class PriceEstimateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val category: EstimateCategory = EstimateCategory.CABINET,
    val title: String = "کابینت",
    val approach: EstimateApproach = EstimateApproach.TOP_DOWN,


    val pricePerMeterOverride: Long? = null,
    val platePriceOverride: Long? = null,
    val installationFeePerMeterOverride: Long? = null,
    val transportationPrice: Long? = null,


    val inputsJson: String = "{}", // داده‌های فرم تب (JSON)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val priceEstimateResult: Long? = null,

)