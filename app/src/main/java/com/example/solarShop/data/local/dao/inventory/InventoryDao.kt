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
        AND deletedAt IS NULL
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
        AND deletedAt IS NULL
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
        createdAt = :createdAt,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE id = :id
""")
    suspend fun updateTransaction(
        id: Int,
        type: InventoryTransactionType,
        quantity: Double,
        note: String,
        createdAt: Long,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    UPDATE inventory_transactions
    SET
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND deletedAt IS NULL
""")
    suspend fun deleteTransactionById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM inventory_transactions WHERE uid = :uid LIMIT 1")
    suspend fun getTransactionByUid(uid: String): InventoryTransactionEntity?

    @Query("""
    SELECT * FROM inventory_transactions
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedInventoryTransactions(): List<InventoryTransactionEntity>

    @Query("""
    UPDATE inventory_transactions
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markInventoryTransactionsSynced(uids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryTransaction(
        transaction: InventoryTransactionEntity
    ): Long


    @Query("""
    SELECT * FROM inventory_transactions
    WHERE deletedAt IS NULL
""")
    fun observeAllTransactions(): Flow<List<InventoryTransactionEntity>>



}