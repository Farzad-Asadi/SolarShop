package com.example.solarShop.data.repository.inventory

import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {

    fun observeCurrentStock(productId: Int): Flow<Double>

    fun observeTransactions(
        productId: Int
    ): Flow<List<InventoryTransactionEntity>>

    suspend fun purchase(
        productId: Int,
        quantity: Double,
        note: String = ""
    )

    suspend fun sale(
        productId: Int,
        quantity: Double,
        note: String = ""
    )

    suspend fun adjust(
        productId: Int,
        quantity: Double,
        note: String = ""
    )
}