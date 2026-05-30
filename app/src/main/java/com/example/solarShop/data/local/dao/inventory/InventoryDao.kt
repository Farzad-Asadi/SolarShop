package com.example.solarShop.data.local.dao.inventory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Insert
    suspend fun insertInventoryTransaction(
        transaction: InventoryTransactionEntity
    ): Long

    @Query("""
        SELECT * FROM inventory_transactions
        WHERE productId = :productId
        ORDER BY createdAt DESC
    """)
    fun observeInventoryTransactions(
        productId: Int
    ): Flow<List<InventoryTransactionEntity>>

    @Query("""
        SELECT COALESCE(
            SUM(
                CASE
                    WHEN transactionType IN ('PURCHASE','SALE_RETURN')
                        THEN quantity

                    WHEN transactionType IN ('SALE','PURCHASE_RETURN')
                        THEN -quantity

                    ELSE quantity
                END
            ),
        0)
        FROM inventory_transactions
        WHERE productId = :productId
    """)
    fun observeCurrentStock(
        productId: Int
    ): Flow<Double>
}