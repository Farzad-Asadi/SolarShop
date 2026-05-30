package com.example.solarShop.data.local.dao.product

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
import com.example.solarShop.data.local.relation.product.ProductFullInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao{


    // ---------- Category ----------

    @Query("SELECT * FROM product_categories WHERE isActive = 1 ORDER BY sortOrder ASC, name ASC")
    fun observeActiveCategories(): Flow<List<ProductCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: ProductCategoryEntity): Long

    @Query("UPDATE product_categories SET isActive = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun deactivateCategory(id: Int, updatedAt: Long = System.currentTimeMillis())

    // ---------- Brand ----------

    @Query("SELECT * FROM product_brands WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveBrands(): Flow<List<ProductBrandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBrand(brand: ProductBrandEntity): Long

    // ---------- Unit ----------

    @Query("SELECT * FROM product_units WHERE isActive = 1 ORDER BY name ASC")
    fun observeActiveUnits(): Flow<List<ProductUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnit(unit: ProductUnitEntity): Long

    // ---------- Product ----------

    @Query("""
        SELECT * FROM products
        WHERE isArchived = 0
        ORDER BY name ASC
    """)
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE categoryId = :categoryId AND isArchived = 0
        ORDER BY name ASC
    """)
    fun observeProductsByCategory(categoryId: Int): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity): Long

    @Query("UPDATE products SET isArchived = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun archiveProduct(id: Int, updatedAt: Long = System.currentTimeMillis())

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE isArchived = 0
    ORDER BY name ASC
""")
    fun observeActiveProductsFullInfo(): Flow<List<ProductFullInfo>>

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE id = :productId
    LIMIT 1
""")
    suspend fun getProductFullInfo(productId: Int): ProductFullInfo?


    // ---------- Product Images ----------

    @Query("""
    SELECT * FROM product_images
    WHERE productId = :productId
    ORDER BY sortOrder ASC, createdAt ASC
""")
    fun observeProductImages(productId: Int): Flow<List<ProductImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProductImage(image: ProductImageEntity): Long

    @Delete
    suspend fun deleteProductImage(image: ProductImageEntity)

    @Query("""
    DELETE FROM product_images
    WHERE id = :imageId
""")
    suspend fun deleteProductImageById(imageId: Int)

    @Query("""
    UPDATE product_images
    SET sortOrder = :sortOrder
    WHERE id = :imageId
""")
    suspend fun updateProductImageSortOrder(
        imageId: Int,
        sortOrder: Int
    )


}