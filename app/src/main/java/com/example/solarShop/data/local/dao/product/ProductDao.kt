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
import com.example.solarShop.feature.product.viewmodel.CategoryProductCount
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao{


    // ---------- Category ----------

    @Query("SELECT * FROM product_categories WHERE isActive = 1 AND deletedAt IS NULL ORDER BY sortOrder ASC, name ASC")
    fun observeActiveCategories(): Flow<List<ProductCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: ProductCategoryEntity): Long

    @Query("""
    UPDATE product_categories
    SET 
        isActive = 0,
        deletedAt = :updatedAt,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE id = :id
""")
    suspend fun deactivateCategory(
        id: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    // ---------- Brand ----------

    @Query("SELECT * FROM product_brands WHERE isActive = 1 AND deletedAt IS NULL ORDER BY name ASC")
    fun observeActiveBrands(): Flow<List<ProductBrandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBrand(brand: ProductBrandEntity): Long

    // ---------- Unit ----------

    @Query("""
    SELECT * FROM product_units
    WHERE isActive = 1
    AND deletedAt IS NULL
    ORDER BY name ASC
""")
    fun observeActiveUnits(): Flow<List<ProductUnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUnit(unit: ProductUnitEntity): Long


    @Query("""
    SELECT * FROM product_units
    WHERE uid = :uid
    LIMIT 1
""")
    suspend fun getUnitByUid(
        uid: String
    ): ProductUnitEntity?

    @Query("""
    SELECT * FROM product_units
    WHERE id = :unitId
    LIMIT 1
""")
    suspend fun getUnitById(
        unitId: Int
    ): ProductUnitEntity?

    @Query("""
    SELECT * FROM product_units
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedUnits(): List<ProductUnitEntity>

    @Query("""
    UPDATE product_units
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markUnitsSynced(
        uids: List<String>
    )

    @Query("""
    UPDATE product_units
    SET isActive = 0,
        deletedAt = :updatedAt,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE id = :id
""")
    suspend fun deactivateUnit(
        id: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    // ---------- Product ----------

    @Query("""
        SELECT * FROM products
        WHERE isArchived = 0 AND isDraft = 0 AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun observeActiveProducts(): Flow<List<ProductEntity>>

    @Query("""
        SELECT * FROM products
        WHERE categoryId = :categoryId 
    AND isArchived = 0 
    AND isDraft = 0
    AND deletedAt IS NULL
        ORDER BY name ASC
    """)
    fun observeProductsByCategory(categoryId: Int): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity): Long

    @Query("""
    UPDATE products
    SET 
        isArchived = 1,
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE id = :id
      AND isDraft = 0
      AND deletedAt IS NULL
""")
    suspend fun softDeleteProductById(
        id: Int,
        deletedAt: Long = System.currentTimeMillis()
    )

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE isArchived = 0 AND isDraft = 0 AND deletedAt IS NULL
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

    @Query("""
    SELECT * FROM product_categories
    WHERE id = :categoryId
    LIMIT 1
""")
    suspend fun getCategoryById(
        categoryId: Int
    ): ProductCategoryEntity?

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE categoryId = :categoryId
    AND isArchived = 0 AND isDraft = 0
    ORDER BY name ASC
""")
    fun observeProductsByCategoryFullInfo(
        categoryId: Int
    ): Flow<List<ProductFullInfo>>

    @Transaction
    @Query("""
    SELECT * FROM products
    WHERE id = :productId
    LIMIT 1
""")
    fun observeProductFullInfo(
        productId: Int
    ): Flow<ProductFullInfo?>



    @Query("""
    SELECT * FROM product_brands
    WHERE id = :brandId
    LIMIT 1
""")
    suspend fun getBrandById(
        brandId: Int
    ): ProductBrandEntity?


    // ---------- Backup ----------
    @Query("SELECT * FROM product_categories")
    suspend fun getAllCategoriesForBackup(): List<ProductCategoryEntity>

    @Query("SELECT * FROM product_brands")
    suspend fun getAllBrandsForBackup(): List<ProductBrandEntity>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsForBackup(): List<ProductEntity>

    @Query("SELECT * FROM product_units")
    suspend fun getAllUnitsForBackup(): List<ProductUnitEntity>

    // ---------- Restore ----------
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoriesForRestore(items: List<ProductCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBrandsForRestore(items: List<ProductBrandEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductsForRestore(items: List<ProductEntity>)



    // ---------- Update ----------
    @Query("""
    UPDATE products
    SET 
        categoryId = :categoryId,
        name = :name,
        model = :model,
        brandId = :brandId,
        isSynced = 0,
        updatedAt = :updatedAt
    WHERE id = :id
""")
    suspend fun updateProductBasicInfo(
        id: Int,
        categoryId: Int,
        name: String,
        model: String,
        brandId: Int?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    UPDATE products
    SET 
        name = :name,
        model = :model,
        brandId = :brandId,
        isDraft = 0,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE id = :id
""")
    suspend fun finalizeDraftProduct(
        id: Int,
        name: String,
        model: String,
        brandId: Int?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    DELETE FROM products
    WHERE isDraft = 1
    AND createdAt < :threshold
""")
    suspend fun deleteOldDraftProducts(
        threshold: Long
    )

    @Query("""
    SELECT
        categoryId,
        COUNT(*) AS count
    FROM products
    WHERE isArchived = 0
    AND isDraft = 0
    AND deletedAt IS NULL
    GROUP BY categoryId
""")
    fun observeProductCountByCategory():
            Flow<List<CategoryProductCount>>

    @Query("""
    UPDATE product_categories
    SET sortOrder = :sortOrder,
        updatedAt = :updatedAt
    WHERE id = :categoryId
""")
    suspend fun updateCategorySortOrder(
        categoryId: Int,
        sortOrder: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM product_categories WHERE uid = :uid LIMIT 1")
    suspend fun getCategoryByUid(uid: String): ProductCategoryEntity?



    @Query("""
    SELECT * FROM product_categories
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedCategories(): List<ProductCategoryEntity>


    @Query("""
    UPDATE product_categories
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markCategoriesSynced(uids: List<String>)

    @Query("SELECT * FROM product_brands WHERE uid = :uid LIMIT 1")
    suspend fun getBrandByUid(uid: String): ProductBrandEntity?

    @Query("""
    SELECT * FROM product_brands
    WHERE isSynced = 0
""")
    suspend fun getUnsyncedBrands(): List<ProductBrandEntity>

    @Query("""
    UPDATE product_brands
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markBrandsSynced(uids: List<String>)

    @Query("SELECT * FROM products WHERE uid = :uid LIMIT 1")
    suspend fun getProductByUid(uid: String): ProductEntity?

    @Query("""
    SELECT * FROM products
    WHERE isSynced = 0
      AND isDraft = 0
""")
    suspend fun getUnsyncedProducts(): List<ProductEntity>

    @Query("""
    UPDATE products
    SET isSynced = 1
    WHERE uid IN (:uids)
""")
    suspend fun markProductsSynced(uids: List<String>)


    @Query("""
    SELECT COUNT(*) FROM products
    WHERE categoryId = :categoryId
      AND isDraft = 0
      AND deletedAt IS NULL
""")
    suspend fun countProductsInCategory(categoryId: Int): Int

    @Query("""
    UPDATE products
    SET 
        isArchived = 1,
        deletedAt = :deletedAt,
        updatedAt = :deletedAt,
        isSynced = 0
    WHERE categoryId = :categoryId
      AND isDraft = 0
      AND deletedAt IS NULL
""")
    suspend fun softDeleteProductsByCategory(
        categoryId: Int,
        deletedAt: Long = System.currentTimeMillis()
    )


    @Query("""
    SELECT COUNT(*) FROM products
    WHERE brandId = :brandId
      AND isDraft = 0
      AND deletedAt IS NULL
""")
    suspend fun countProductsWithBrand(brandId: Int): Int

    @Query("""
    UPDATE products
    SET 
        brandId = NULL,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE brandId = :brandId
      AND deletedAt IS NULL
""")
    suspend fun clearBrandFromProducts(
        brandId: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    UPDATE product_brands
    SET 
        isActive = 0,
        deletedAt = :updatedAt,
        updatedAt = :updatedAt,
        isSynced = 0
    WHERE id = :id
""")
    suspend fun deactivateBrand(
        id: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
    SELECT * FROM products
    WHERE id = :productId
    LIMIT 1
""")
    suspend fun getProductById(productId: Int): ProductEntity?





}