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


    override suspend fun addTransaction(
        transaction: InventoryTransactionEntity
    ): Long {
        return inventoryDao.insertInventoryTransaction(transaction)
    }

}