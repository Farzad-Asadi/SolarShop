package com.example.solarShop.data.repository.inventory

import com.example.solarShop.InventoryTransactionType
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

    suspend fun updateTransaction(
        id: Int,
        transactionType: InventoryTransactionType,
        quantity: Double,
        note: String,
        createdAt: Long
    )

    suspend fun deleteTransactionById(
        id: Int
    )

    suspend fun addTransaction(
        transaction: InventoryTransactionEntity
    ): Long


    suspend fun getUnsyncedInventoryTransactions(): List<InventoryTransactionEntity>

    suspend fun markInventoryTransactionsSynced(uids: List<String>)

    suspend fun upsertInventoryTransactionByUid(
        transaction: InventoryTransactionEntity
    ): Long


    suspend fun getAllInventoryTransactionsForBackup(): List<InventoryTransactionEntity>

}