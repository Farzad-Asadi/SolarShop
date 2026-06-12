package com.example.solarShop.data.repository.product

import com.example.solarShop.data.local.entity.product.ProductBrandEntity
import com.example.solarShop.data.local.entity.product.ProductCategoryEntity
import com.example.solarShop.data.local.entity.product.ProductEntity
import com.example.solarShop.data.local.entity.product.ProductImageEntity
import com.example.solarShop.data.local.entity.product.ProductUnitEntity
import com.example.solarShop.data.local.relation.product.ProductFullInfo
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    fun observeActiveCategories(): Flow<List<ProductCategoryEntity>>
    fun observeActiveBrands(): Flow<List<ProductBrandEntity>>
    fun observeActiveUnits(): Flow<List<ProductUnitEntity>>

    fun observeActiveProducts(): Flow<List<ProductEntity>>
    fun observeProductsByCategory(categoryId: Int): Flow<List<ProductEntity>>
    fun observeActiveProductsFullInfo(): Flow<List<ProductFullInfo>>

    suspend fun getProductFullInfo(productId: Int): ProductFullInfo?

    suspend fun upsertCategory(category: ProductCategoryEntity): Long
    suspend fun upsertBrand(brand: ProductBrandEntity): Long
    suspend fun upsertUnit(unit: ProductUnitEntity): Long
    suspend fun upsertProduct(product: ProductEntity): Long

    suspend fun archiveProduct(productId: Int)

    fun observeProductImages(productId: Int): Flow<List<ProductImageEntity>>
    suspend fun upsertProductImage(image: ProductImageEntity): Long
    suspend fun deleteProductImageById(imageId: Int)

    suspend fun getCategoryById(
        categoryId: Int
    ): ProductCategoryEntity?

    fun observeProductsByCategoryFullInfo(
        categoryId: Int
    ): Flow<List<ProductFullInfo>>

    fun observeProductFullInfo(
        productId: Int
    ): Flow<ProductFullInfo?>


    suspend fun getBrandById(brandId: Int): ProductBrandEntity?

    suspend fun updateProductBasicInfo(
        id: Int,
        categoryId: Int,
        name: String,
        model: String,
        brandId: Int?
    )

    suspend fun finalizeDraftProduct(
        id: Int,
        name: String,
        model: String,
        brandId: Int?
    )

    suspend fun cleanupOldDraftProducts(
        olderThanMillis: Long = 24 * 60 * 60 * 1000L
    )

    fun observeProductCountByCategory(): Flow<Map<Int, Int>>

    suspend fun updateCategorySortOrders(
        categories: List<ProductCategoryEntity>
    )

    suspend fun upsertCategoryByUid(category: ProductCategoryEntity): Long

    suspend fun getUnsyncedCategories(): List<ProductCategoryEntity>

    suspend fun markCategoriesSynced(uids: List<String>)

    suspend fun upsertBrandByUid(brand: ProductBrandEntity): Long

    suspend fun getUnsyncedBrands(): List<ProductBrandEntity>

    suspend fun markBrandsSynced(uids: List<String>)

}