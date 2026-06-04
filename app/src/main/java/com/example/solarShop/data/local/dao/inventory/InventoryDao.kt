package com.example.solarShop.data.local.dao.inventory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.solarShop.InventoryTransactionType
import com.example.solarShop.data.local.entity.inventory.InventoryTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // ---------- Inventory Transaction ----------
    @Insert
    suspend fun insertInventoryTransaction(
        transaction: InventoryTransactionEntity
    ): Long


    // ---------- Inventory ----------
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


    // ---------- Backup ----------
    @Query("SELECT * FROM inventory_transactions")
    suspend fun getAllInventoryTransactionsForBackup(): List<InventoryTransactionEntity>

    // ---------- Restore ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryTransactionsForRestore(items: List<InventoryTransactionEntity>)



    // ---------- Update Transaction ----------
    @Query("""
    UPDATE inventory_transactions
    SET
        transactionType = :type,
        quantity = :quantity,
        note = :note,
        createdAt = :createdAt
    WHERE id = :id
""")
    suspend fun updateTransaction(
        id: Int,
        type: InventoryTransactionType,
        quantity: Double,
        note: String,
        createdAt: Long
    )

    @Query("""
    DELETE FROM inventory_transactions
    WHERE id = :id
""")
    suspend fun deleteTransactionById(
        id: Int
    )





}