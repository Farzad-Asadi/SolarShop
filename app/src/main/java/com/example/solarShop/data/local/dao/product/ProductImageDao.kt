package com.example.solarShop.data.local.dao.product

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductImageDao {

    @Insert
    suspend fun insert(
        entity: ProductImageEntity
    )

    @Delete
    suspend fun delete(
        entity: ProductImageEntity
    )

    @Query("""
        DELETE FROM product_images
        WHERE id = :imageId
    """)
    suspend fun deleteById(
        imageId: Int
    )

    @Query("""
        SELECT *
        FROM product_images
        WHERE productId = :productId
        AND deletedAt IS NULL
        ORDER BY sortOrder
    """)
    fun observeImagesForProduct(
        productId: Int
    ): Flow<List<ProductImageEntity>>

    @Query("""
        SELECT *
        FROM product_images
        WHERE productId = :productId
        AND deletedAt IS NULL
        ORDER BY sortOrder
    """)
    suspend fun getImagesForProduct(
        productId: Int
    ): List<ProductImageEntity>

    @Query("""
        SELECT MAX(sortOrder)
        FROM product_images
        WHERE productId = :productId
        AND deletedAt IS NULL
    """)
    suspend fun getMaxSortOrder(
        productId: Int
    ): Int?

    @Query("""
    UPDATE product_images
    SET 
        sortOrder = :sortOrder,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE id = :imageId
""")
    suspend fun updateOrder(
        imageId: Int,
        sortOrder: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        SELECT *
        FROM product_images
        WHERE id = :imageId
    """)
    suspend fun getById(
        imageId: Int
    ): ProductImageEntity?

    @Query("""
    SELECT *
    FROM product_images
    WHERE productId IN (:productIds)
      AND deletedAt IS NULL
    ORDER BY sortOrder ASC, createdAt ASC
""")
    fun observeImagesForProducts(
        productIds: List<Int>
    ): Flow<List<ProductImageEntity>>



    // ---------- Backup ----------
    @Query("SELECT * FROM product_images")
    suspend fun getAllImagesForBackup(): List<ProductImageEntity>

    // ---------- Restore ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImagesForRestore(items: List<ProductImageEntity>)






    @Query("SELECT * FROM product_images WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): ProductImageEntity?

    @Query("""
    SELECT * FROM product_images
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedProductImages(): List<ProductImageEntity>

    @Query("""
    UPDATE product_images
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markProductImagesSynced(uids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductImage(image: ProductImageEntity): Long



    @Query("""
    UPDATE product_images
    SET 
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :imageId
      AND deletedAt IS NULL
""")
    suspend fun softDeleteById(
        imageId: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

}