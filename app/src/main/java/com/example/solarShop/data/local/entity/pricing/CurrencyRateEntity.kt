package com.example.solarShop.data.local.entity.pricing

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "currency_rates",
    indices = [
        Index("currencyCode"),
        Index("createdAt"),
        Index("uid", unique = true)
    ]
)
data class CurrencyRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val uid: String = UUID.randomUUID().toString(),

    val currencyCode: String = "USD",
    val rateToman: Long,

    val source: String = "",
    val note: String = "",

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val deletedAt: Long? = null,
    val isSynced: Boolean = false
)