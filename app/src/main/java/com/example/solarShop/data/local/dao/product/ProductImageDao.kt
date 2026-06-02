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
        ORDER BY sortOrder
    """)
    fun observeImagesForProduct(
        productId: Int
    ): Flow<List<ProductImageEntity>>

    @Query("""
        SELECT *
        FROM product_images
        WHERE productId = :productId
        ORDER BY sortOrder
    """)
    suspend fun getImagesForProduct(
        productId: Int
    ): List<ProductImageEntity>

    @Query("""
        SELECT MAX(sortOrder)
        FROM product_images
        WHERE productId = :productId
    """)
    suspend fun getMaxSortOrder(
        productId: Int
    ): Int?

    @Query("""
        UPDATE product_images
        SET sortOrder = :sortOrder
        WHERE id = :imageId
    """)
    suspend fun updateOrder(
        imageId: Int,
        sortOrder: Int
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
    ORDER BY sortOrder ASC
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

}