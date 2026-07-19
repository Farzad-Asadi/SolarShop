package com.example.solarShop.data.repository.inventory

import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.local.dao.inventory.InventoryDao
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao
) : InventoryRepository {

    override fun observeCurrentStock(productId: Int) =
        inventoryDao.observeCurrentStock(productId)

    override fun observeTransactions(productId: Int) =
        inventoryDao.observeInventoryTransactions(productId)

    override suspend fun purchase(
        productId: Int,
        quantity: Double,
        note: String
    ) {
        inventoryDao.insertInventoryTransaction(
            InventoryTransactionEntity(
                productId = productId,
                quantity = quantity,
                transactionType = InventoryTransactionType.PURCHASE,
                note = note
            )
        )
    }

    override suspend fun sale(
        productId: Int,
        quantity: Double,
        note: String
    ) {
        inventoryDao.insertInventoryTransaction(
            InventoryTransactionEntity(
                productId = productId,
                quantity = quantity,
                transactionType = InventoryTransactionType.SALE,
                note = note
            )
        )
    }

    override suspend fun adjust(
        productId: Int,
        quantity: Double,
        note: String
    ) {
        inventoryDao.insertInventoryTransaction(
            InventoryTransactionEntity(
                productId = productId,
                quantity = quantity,
                transactionType = InventoryTransactionType.ADJUSTMENT,
                note = note
            )
        )
    }

    override suspend fun updateTransaction(
        id: Int,
        transactionType: InventoryTransactionType,
        quantity: Double,
        note: String,
        createdAt: Long
    ) {
        inventoryDao.updateTransaction(
            id = id,
            type = transactionType,
            quantity = quantity,
            note = note,
            createdAt = createdAt
        )
    }

    override suspend fun deleteTransactionById(
        id: Int
    ) {
        inventoryDao.deleteTransactionById(id)
    }

    override suspend fun softDeleteByUid(
        uid: String
    ) {
        inventoryDao.softDeleteByUid(uid)
    }


    override suspend fun addTransaction(
        transaction: InventoryTransactionEntity
    ): Long {
        return inventoryDao.insertInventoryTransaction(transaction)
    }


    override suspend fun getUnsyncedInventoryTransactions(): List<InventoryTransactionEntity> {
        return inventoryDao.getUnsyncedInventoryTransactions()
    }

    override suspend fun markInventoryTransactionsSynced(
        uids: List<String>
    ) {
        inventoryDao.markInventoryTransactionsSynced(uids)
    }

    override suspend fun upsertInventoryTransactionByUid(
        transaction: InventoryTransactionEntity
    ): Long {
        val existing = inventoryDao.getTransactionByUid(transaction.uid)

        if (existing == null) {
            return inventoryDao.upsertInventoryTransaction(transaction)
        }

        if (existing.deletedAt != null && transaction.deletedAt == null) {
            return existing.id?.toLong() ?: 0L
        }

        if (!existing.isSynced && existing.updatedAt > transaction.updatedAt) {
            return existing.id?.toLong() ?: 0L
        }

        return inventoryDao.upsertInventoryTransaction(
            transaction.copy(
                id = existing.id,
                createdAt = existing.createdAt
            )
        )
    }

    override suspend fun getAllInventoryTransactionsForBackup(): List<InventoryTransactionEntity> {
        return inventoryDao.getAllInventoryTransactionsForBackup()
    }

    override fun observeAllTransactions() =
        inventoryDao.observeAllTransactions()

}
