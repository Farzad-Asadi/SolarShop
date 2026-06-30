package com.example.solarShop.data.local.dao.sales

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.solarShop.data.local.entity.sales.ProductSaleTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductSaleTransactionDao {

    @Insert
    suspend fun insertSaleTransaction(
        transaction: ProductSaleTransactionEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSaleTransaction(
        transaction: ProductSaleTransactionEntity
    ): Long

    @Query("""
        SELECT *
        FROM product_sale_transactions
        WHERE productId = :productId
        AND deletedAt IS NULL
        ORDER BY soldAt DESC, createdAt DESC
    """)
    fun observeSaleTransactionsByProduct(
        productId: Int
    ): Flow<List<ProductSaleTransactionEntity>>

    @Query("""
        SELECT *
        FROM product_sale_transactions
        WHERE deletedAt IS NULL
        ORDER BY soldAt DESC, createdAt DESC
    """)
    fun observeAllSaleTransactions(): Flow<List<ProductSaleTransactionEntity>>

    @Query("""
        SELECT *
        FROM product_sale_transactions
        WHERE uid = :uid
        LIMIT 1
    """)
    suspend fun getSaleTransactionByUid(
        uid: String
    ): ProductSaleTransactionEntity?

    @Query("""
        SELECT *
        FROM product_sale_transactions
        WHERE isSynced = 0
    """)
    suspend fun getUnsyncedSaleTransactions(): List<ProductSaleTransactionEntity>

    @Query("""
        UPDATE product_sale_transactions
        SET isSynced = 1
        WHERE uid IN (:uids)
    """)
    suspend fun markSaleTransactionsSynced(
        uids: List<String>
    )

    @Query("""
        UPDATE product_sale_transactions
        SET
            deletedAt = :deletedAt,
            updatedAt = :deletedAt,
            isSynced = 0
        WHERE uid = :uid
        AND deletedAt IS NULL
    """)
    suspend fun softDeleteByUid(
        uid: String,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT *
        FROM product_sale_transactions
    """)
    suspend fun getAllSaleTransactionsForBackup(): List<ProductSaleTransactionEntity>
}