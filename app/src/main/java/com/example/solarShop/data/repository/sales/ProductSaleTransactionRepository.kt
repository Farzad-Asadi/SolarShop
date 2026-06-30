package com.example.solarShop.data.repository.sales

import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import kotlinx.coroutines.flow.Flow

interface ProductSaleTransactionRepository {

    fun observeSaleTransactionsByProduct(
        productId: Int
    ): Flow<List<ProductSaleTransactionEntity>>

    fun observeAllSaleTransactions(): Flow<List<ProductSaleTransactionEntity>>

    suspend fun addSaleTransaction(
        transaction: ProductSaleTransactionEntity
    ): Long

    suspend fun getUnsyncedSaleTransactions(): List<ProductSaleTransactionEntity>

    suspend fun markSaleTransactionsSynced(
        uids: List<String>
    )

    suspend fun upsertSaleTransactionByUid(
        transaction: ProductSaleTransactionEntity
    ): Long

    suspend fun softDeleteByUid(
        uid: String
    )

    suspend fun getAllSaleTransactionsForBackup(): List<ProductSaleTransactionEntity>
}