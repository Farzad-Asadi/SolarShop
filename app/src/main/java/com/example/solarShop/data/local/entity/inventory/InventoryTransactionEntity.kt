package com.example.solarShop.data.local.entity.inventory

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.solarShop.InventoryTransactionType
import java.util.UUID


@Entity(
    tableName = "inventory_transactions",
    indices = [
        Index("productId"),
        Index("transactionType"),
        Index("createdAt")
    ]
)
data class InventoryTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,

    val uid: String = UUID.randomUUID().toString(),

    val productId: Int,

    val quantity: Double,

    val transactionType: InventoryTransactionType,

    val note: String = "",

    val createdAt: Long = System.currentTimeMillis()
)