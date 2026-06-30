package com.example.solarShop.data.repository.sales

import com.example.solarShop.data.local.dao.sales.ProductSaleTransactionDao
import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import javax.inject.Inject

class ProductSaleTransactionRepositoryImpl @Inject constructor(
    private val dao: ProductSaleTransactionDao
) : ProductSaleTransactionRepository {

    override fun observeSaleTransactionsByProduct(
        productId: Int
    ) = dao.observeSaleTransactionsByProduct(productId)

    override fun observeAllSaleTransactions() =
        dao.observeAllSaleTransactions()

    override suspend fun addSaleTransaction(
        transaction: ProductSaleTransactionEntity
    ): Long {
        return dao.insertSaleTransaction(transaction)
    }

    override suspend fun getUnsyncedSaleTransactions(): List<ProductSaleTransactionEntity> {
        return dao.getUnsyncedSaleTransactions()
    }

    override suspend fun markSaleTransactionsSynced(
        uids: List<String>
    ) {
        dao.markSaleTransactionsSynced(uids)
    }

    override suspend fun upsertSaleTransactionByUid(
        transaction: ProductSaleTransactionEntity
    ): Long {
        val existing =
            dao.getSaleTransactionByUid(transaction.uid)

        if (existing == null) {
            return dao.upsertSaleTransaction(transaction)
        }

        if (existing.deletedAt != null && transaction.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > transaction.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return dao.upsertSaleTransaction(
            transaction.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun softDeleteByUid(
        uid: String
    ) {
        dao.softDeleteByUid(uid)
    }

    override suspend fun getAllSaleTransactionsForBackup(): List<ProductSaleTransactionEntity> {
        return dao.getAllSaleTransactionsForBackup()
    }
}